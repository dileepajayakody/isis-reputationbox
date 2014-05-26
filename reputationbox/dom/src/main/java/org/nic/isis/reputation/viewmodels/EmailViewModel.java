package org.nic.isis.reputation.viewmodels;

import java.util.ArrayList;
import java.util.List;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Programmatic;
import org.json.JSONArray;
import org.json.JSONObject;

public class EmailViewModel implements ViewModel {

	private JSONObject json;
	@Override
	@Hidden
	public void viewModelInit(String momento) {
		this.json = new JSONObject(momento);
	}

	@Override
	public String viewModelMemento() {
		// TODO Auto-generated method stub
		return json.toString();
	}
	
	public String getMessageId(){
		return json.getString("messageId");
	}
	
	public String getDate(){
		return json.getString("date");
	}
	
	public List<String> getFolders(){
		List<String> folderList = new ArrayList<String>();
		JSONArray folders = (JSONArray)json.get("folders");
		for(int i=0; i < folders.length(); i++){
			folderList.add(folders.getString(i));
		}
		return folderList;
	}
	
	@Programmatic
	public JSONObject getAddressObject(){
		return (JSONObject)json.get("addresses");
	}
	
	public String getFromAddress(){
		JSONObject fromAddressObject = (JSONObject)getAddressObject().get("from");
		String email = fromAddressObject.getString("email");
		return email;
	}
	
	public List<String> getToAddresses(){
		List<String> toAddressEmails = new ArrayList<String>();
		JSONArray toAddressArray = (JSONArray)getAddressObject().get("to");
		
		for(int i=0; i < toAddressArray.length(); i++){
			JSONObject toAddressObj = (JSONObject)toAddressArray.get(i);
			String email = toAddressObj.getString("email");
			toAddressEmails.add(email);
		}
		return toAddressEmails;
	}
	
	public List<String> getCCAddresses(){
		List<String> ccAddressEmails = new ArrayList<String>();
		JSONArray ccAddressArray = (JSONArray)getAddressObject().get("cc");
		
		for(int i=0; i < ccAddressArray.length(); i++){
			JSONObject ccAddressObj = (JSONObject)ccAddressArray.get(i);
			String email = ccAddressObj.getString("email");
			ccAddressEmails.add(email);
		}
		return ccAddressEmails;
	}
	
	public String getSubject(){
		return json.getString("subject");
	}
	
	public String getGmailThreadId(){
		return json.getString("gmailThreadId");
	}
	

}
