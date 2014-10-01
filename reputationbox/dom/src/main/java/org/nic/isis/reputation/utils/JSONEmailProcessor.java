package org.nic.isis.reputation.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nic.isis.reputation.dom.EmailAttachment;
import org.nic.isis.reputation.dom.EmailFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dileepa Performs JSON processing to extract email info from
 *         ContextIOResponse
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
	
	public int getDateReceived(){
		int dateReceived = json.getInt("date_received");
		return dateReceived;
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
		if (!getAddressObject().isNull("from")){
			JSONObject fromAddressObject = (JSONObject) getAddressObject().get(
					"from");
			
			String email = fromAddressObject.getString("email");
			return email;
		} else {
			return null;
		}
		
	}

	public List<String> getToAddresses() {
		List<String> toAddressEmails = new ArrayList<String>();
		try {
			
			if (!getAddressObject().isNull("to")) {
				JSONArray toAddressArray = (JSONArray) getAddressObject().get("to");
				for (int i = 0; i < toAddressArray.length(); i++) {
					JSONObject toAddressObj = (JSONObject) toAddressArray
							.get(i);
					String email = toAddressObj.getString("email");
					toAddressEmails.add(email);
				}
			}

		} catch (JSONException jex) {
			logger.error("JSON Exception occured while retrieving TO addresses", jex);
		}
		return toAddressEmails;
	}

	public List<String> getCCAddresses() {
		List<String> ccAddressEmails = new ArrayList<String>();
		try {
			
			if (!getAddressObject().isNull("cc")) {
				JSONArray ccAddressArray = (JSONArray) getAddressObject().get(
						"cc");
				for (int i = 0; i < ccAddressArray.length(); i++) {
					JSONObject toAddressObj = (JSONObject) ccAddressArray
							.get(i);
					String email = toAddressObj.getString("email");
					ccAddressEmails.add(email);
				}
			}

		} catch (JSONException jex) {
			logger.error(
					"JSON Exception occured while retrieving CC addresses", jex);
		}
		return ccAddressEmails;
	}

	public EmailFlag getFlags(){
		if(json.get("flags") != null){
			JSONArray flagsArray = (JSONArray)json.get("flags");
			EmailFlag flagObject = new EmailFlag();
			for(int i = 0; i < flagsArray.length(); i++){
				String flag = flagsArray.getString(i);
				if(flag.equalsIgnoreCase("\\Seen")){
					flagObject.setSeen(true);
				} else if(flag.equalsIgnoreCase("\\Answered")){
					flagObject.setAnswered(true);
				} else if(flag.equalsIgnoreCase("\\Flagged")){
					flagObject.setFlagged(true);
				} else if(flag.equalsIgnoreCase("\\Deleted")){
					flagObject.setDeleted(true);
				} else if(flag.equalsIgnoreCase("\\Draft")){
					flagObject.setDraft(true);
				} else if(flag.equalsIgnoreCase("\\Nonjunk")){
					flagObject.setNonJunk(true);
				}  
			}
			return flagObject;
		}
		return null;
	}

	public List<EmailAttachment> getAttachments() {
		List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();
		if (json.get("files") != null) {
			JSONArray filesArray = (JSONArray) json.get("files");
			EmailAttachment attachment = new EmailAttachment();
			for (int i = 0; i < filesArray.length(); i++) {
				JSONObject file = (JSONObject) filesArray.get(i);
				String fileId = (String) file.get("file_id");
				String fileName = (String) file.get("file_name");
				Integer fileSize = (Integer) file.get("size");
				String fileType = (String) file.get("type");

				attachment.setAttachmentId(fileId);
				attachment.setFileName(fileName);
				attachment.setSize(fileSize);
				attachment.setType(fileType);
				
				attachments.add(attachment);
			}
		}
		return attachments;
	}
	
	public String getBodyContent(){
		JSONArray bodyArray = (JSONArray)json.get("body");
		//return only the first body item
		JSONObject bodyObject = (JSONObject)bodyArray.get(0);
		String content = bodyObject.getString("content");
		
		//List<String> bodyContents = new ArrayList<String>();
		/*for(int i = 0; i < bodyArray.length(); i++){
			JSONObject bodyObject = (JSONObject)bodyArray.get(i);
			String content = bodyObject.getString("content");
			bodyContents.add(content);
		}*/
		return content;
	}

}
