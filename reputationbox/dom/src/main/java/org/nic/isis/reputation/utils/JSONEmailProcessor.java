package org.nic.isis.reputation.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dileepa Performs JSON processing to extract email info from
 *         allmessages 2.0 API
 */
public class JSONEmailProcessor {

	private JSONObject json;
	private final static Logger logger = LoggerFactory
			.getLogger(JSONEmailProcessor.class);

	public JSONEmailProcessor(JSONObject js) {
		this.json = js;
	}

	public String getEmailMessageId() {
		return json.getString("email_message_id");
	}

	/**
	 * @return Unique and persistent id assigned by Context.IO to the message,
	 */
	public String getPersistentMessageId() {
		return json.getString("message_id");
	}

	public String getGmailMessageId() {
		return json.getString("gmail_message_id");
	}

	public int getDateIndexed() {
		return json.getInt("date_indexed");
	}

	public int getMessageDate() {
		int date = json.getInt("date");
		return date;
	}

	public String getSubject() {
		return json.getString("subject");
	}

	public String getGmailThreadId() {
		return json.getString("gmail_thread_id");
	}

	public List<String> getFolders() {
		List<String> folderList = new ArrayList<String>();
		JSONArray folders = (JSONArray) json.get("folders");
		if (folders != null) {
			for (int i = 0; i < folders.length(); i++) {
				folderList.add(folders.getString(i));
			}
		}

		return folderList;
	}

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

			if (null != toAddressArray) {
				for (int i = 0; i < toAddressArray.length(); i++) {
					JSONObject toAddressObj = (JSONObject) toAddressArray
							.get(i);
					String email = toAddressObj.getString("email");
					toAddressEmails.add(email);
				}
			}

		} catch (JSONException jex) {
			logger.error("JSON Exception occured while retrieving TO addresses");
		}
		return toAddressEmails;
	}

	public List<String> getCCAddresses() {
		List<String> ccAddressEmails = new ArrayList<String>();
		try {
			JSONArray ccAddressArray = (JSONArray) getAddressObject().get("cc");

			if (ccAddressArray != null) {
				for (int i = 0; i < ccAddressArray.length(); i++) {
					JSONObject toAddressObj = (JSONObject) ccAddressArray
							.get(i);
					String email = toAddressObj.getString("email");
					ccAddressEmails.add(email);
				}
			}

		} catch (JSONException jex) {
			logger.error("JSON Exception occured while retrieving CC addresses");
		}
		return ccAddressEmails;
	}
	
	public JSONArray getFiles(){
		return (JSONArray) json.get("files");
	}
	
	

}
