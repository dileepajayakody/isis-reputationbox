package org.nic.isis.reputation.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.scribe.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.tomtasche.contextio.ContextIO;
import at.tomtasche.contextio.ContextIOResponse;

public class ContextIOService {

	ContextIO contextIO;
	@Inject
	DomainObjectContainer container;
	
	private final static Logger logger = LoggerFactory
			.getLogger(ContextIOService.class);
	

	public ContextIOService(){
		contextIO = new ContextIO("65kd0b3k", "CetIiO0Ke0Klb2u8");
	}
	
	public UserMailBox synMailBox(UserMailBox mailbox, int limit) {
		int since = mailbox.getLastIndexTimestamp();
		Map<String, String> params = new HashMap<String, String>();
		params.put("since", String.valueOf(since));
		params.put("limit", String.valueOf(limit));
		ContextIOResponse cio = contextIO.allMessages(mailbox.getEmailId(), params);
		Response cioResponse = cio.getRawResponse();
		String responseString = cioResponse.getBody();

		JSONObject json = new JSONObject(responseString);
		JSONArray data = json.getJSONArray("data");
		int lastIndexedTimestamp= json.getInt("timestamp");
		
		
		for (int i = 0; i < data.length(); i++) {
			//iterating over email objects
			JSONObject jsonObj = (JSONObject) data.get(i);

			try {		
				JSONEmailProcessor jsonProcessor = new JSONEmailProcessor(jsonObj);
				String emailMessageID = jsonProcessor.getEmailMessageId();
				String gmailThreadID =jsonProcessor.getGmailThreadId();
				String subject = jsonProcessor.getSubject();
				Date emailDate = jsonProcessor.getDate();
				String fromAddress = jsonProcessor.getFromAddress();
				List<String> toAddresses = jsonProcessor.getToAddresses();
				List<String> ccAddresses = jsonProcessor.getCCAddresses();
				List<String> folders = jsonProcessor.getFolders();
				
				Email email = new Email();
				email.setSubject(subject);
				email.setMessageId(emailMessageID);
				email.setGmailThreadId(gmailThreadID);
				email.setDate(emailDate);
				email.setFromAddress(fromAddress);
				email.setCcAddresses(ccAddresses);
				email.setToAddresses(toAddresses);
				email.setFolders(folders);
				
				mailbox.addEmail(email);
				
			} catch (Exception e) {
				logger.error("Error while encoding Json message", e);
			}
		}
		mailbox.setLastIndexTimestamp(lastIndexedTimestamp);
		return mailbox;
		
	}
	
	public Email getEmail(String emailAccount, String msgId, Email email){
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("emailmessageid", msgId);
		ContextIOResponse cioMessageText = contextIO.messageText(
				emailAccount, emailParams);
		JSONObject messageJson = new JSONObject(cioMessageText
				.getRawResponse().getBody());
		JSONArray messageData = messageJson.getJSONArray("data");
		JSONObject messageObj = (JSONObject) messageData.get(0);

		String contentType = messageObj.getString("type");
		String charSet = messageObj.getString("charset");
		String content = messageObj.getString("content");
		email.setBody(content);
		email.setContentType(contentType);
		email.setCharSet(charSet);
		return email;
	}
	
}
