package org.nic.isis.reputation.viewmodels;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Title;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailViewModel implements ViewModel {

	private JSONObject json;
	private final static Logger logger = LoggerFactory
			.getLogger(EmailViewModel.class);

	@Override
	@Hidden
	public void viewModelInit(String momento) {
		String decodedJsonString;
		try {
			decodedJsonString = URLDecoder.decode(momento, "UTF-8");
			this.json = new JSONObject(decodedJsonString);
		} catch (UnsupportedEncodingException e) {
			logger.error("Error while decoding momento string", e);
		}

	}

	@Hidden
	public String viewModelMemento() {
		String vmMomento = "";
		try {
			vmMomento = URLEncoder.encode(json.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("Error while returning view model momento string", e);
		}
		return vmMomento;
	}

	@Title
	public String getEmailMessageId() {
		return json.getString("emailMessageId");
	}

	public Date getDate() {
		Date date = new Date(json.getInt("date"));
		return date;
	}

	public List<String> getFolders() {
		List<String> folderList = new ArrayList<String>();
		JSONArray folders = (JSONArray) json.get("folders");
		for (int i = 0; i < folders.length(); i++) {
			folderList.add(folders.getString(i));
		}
		return folderList;
	}

	@Programmatic
	public JSONObject getAddressObject() {
		return (JSONObject) json.get("addresses");
	}

	public String getFromAddress() {
		JSONObject fromAddressObject = (JSONObject) getAddressObject().get(
				"from");
		String email = fromAddressObject.getString("email");
		return email;
	}

	public List<String> getToAddresses() {
		List<String> toAddressEmails = new ArrayList<String>();
		try {
			JSONArray toAddressArray = (JSONArray) getAddressObject().get("to");

			for (int i = 0; i < toAddressArray.length(); i++) {
				JSONObject toAddressObj = (JSONObject) toAddressArray.get(i);
				String email = toAddressObj.getString("email");
				toAddressEmails.add(email);
			}
		}catch (JSONException jex){
			logger.error(
					"JSON Exception occured while retrieving TO addresses", jex);
		}
		return toAddressEmails;
	}

	public List<String> getCCAddresses() {
		List<String> ccAddressEmails = new ArrayList<String>();
		try {		
				JSONArray ccAddressArray = (JSONArray) getAddressObject().get(
						"cc");
				for (int i = 0; i < ccAddressArray.length(); i++) {
					JSONObject ccAddressObj = (JSONObject) ccAddressArray
							.get(i);
					String email = ccAddressObj.getString("email");
					ccAddressEmails.add(email);
				}
		} catch (JSONException jex) {
			logger.error(
					"JSON Exception occured while retrieving CC addresses", jex);
		}
		return ccAddressEmails;
	}

	public String getSubject() {
		return json.getString("subject");
	}

	public String getGmailThreadId() {
		return json.getString("gmailThreadId");
	}

}
