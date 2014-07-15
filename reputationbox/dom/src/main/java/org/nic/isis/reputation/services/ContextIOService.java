package org.nic.isis.reputation.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.settings.ApplicationSetting;
import org.apache.isis.applib.services.settings.ApplicationSettingsService;
import org.apache.isis.applib.services.settings.ApplicationSettingsServiceRW;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.reputation.utils.JSONEmailProcessor;
import org.nic.isis.reputation.utils.URLUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.tomtasche.contextio.ContextIOApi;
import at.tomtasche.contextio.ContextIOResponse;
import at.tomtasche.contextio.ContextIO_V11;
import at.tomtasche.contextio.ContextIO_V20;

/**
 * Class to manage Context.IO API access
 * 
 */
public class ContextIOService {

	
	private final static Logger logger = LoggerFactory
			.getLogger(ContextIOService.class);

    //region > init
    @PostConstruct
    public void init(){

        //this.key = applicationSettingsService.find("contextIOApiKey").valueAsString();
        //this.secret = applicationSettingsService.find("contextIOApiSecret").valueAsString();
       
        contextio_v11 = new ContextIO_V11();
        contextio_v20 = new ContextIO_V20();
       
        String key = "65kd0b3k";
        contextio_v11.setKey(key);
        contextio_v20.setKey(key);
    
        String secret = "CetIiO0Ke0Klb2u8";
        contextio_v11.setSecret(secret);
        contextio_v20.setSecret(secret);
        
        String accountId = "53214991facaddd22d812863";
        contextio_v11.setAccountId(accountId);
        contextio_v20.setAccountId(accountId);
   
        contextio_v11.setSsl(true);
        contextio_v20.setSsl(true);
        
        contextio_v11.setSaveHeaders(true);
        contextio_v20.setSaveHeaders(true);
        
    }
    //endregion


    //service methods
	public void listAllMailBoxes(){
		ContextIOResponse accountsResponse = contextio_v20.getAccounts();
		String responseBody = accountsResponse.getRawResponse().getBody();
		logger.info("ALL mailboxes response : " + responseBody);
		JSONArray accountsArray = new JSONArray(responseBody);
		for (int i = 0; i < accountsArray.length(); i++){
			JSONObject accountObject = (JSONObject)accountsArray.get(i);
			try{
				String accountId = (String)accountObject.get("id");
				String userName = (String)accountObject.get("username");
				String firstName = (String)accountObject.get("first_name");
				String lastName = (String)accountObject.get("username");
				JSONArray emailArray = accountObject.getJSONArray("email_addresses");
				List<String> emailAddresses = new ArrayList<String>();
				for (int x = 0; i < emailArray.length(); x++){
					String emailAddress = emailArray.getString(x);
					emailAddresses.add(emailAddress);
				}
				int noOfMessages = accountObject.getInt("nb_messages");
				
			}catch(Exception ex){
				logger.error("Error parsing Account JSON object", ex);
			}
		}
		
	}
    
    
	/**
	 * Updates the mailbox with new emails
	 * @param mailbox
	 * @param limit number new emails
	 * @return
	 */
	public UserMailBox updateMailBox(UserMailBox mailbox, int limit) {
		int offset = mailbox.getEmailCount();
		String emailAccount = mailbox.getAccountId();
		String emailAddress = mailbox.getEmailId();
		logger.info("Syncing Email Account : " + emailAccount + " email offset : " + offset);
		Map<String, String> params = new HashMap<String, String>();
		params.put("limit", String.valueOf(limit));
		params.put("sort_order", "asc");
		params.put("include_body", String.valueOf(1));
		params.put("include_headers", String.valueOf(1));
		params.put("include_flags", String.valueOf(1));
		
		ContextIOResponse cio = contextio_v20.allMessages(emailAccount, params);
		
		//contextio 1.1. impl
		//params.put("since", String.valueOf(0));
		//params.put("limit", String.valueOf(limit));
		//ContextIOResponse cio = contextio_v11.allMessages(emailAddress, params);
		
		logger.info("allmessages response : " + cio.getRawResponse().getBody() );
		
		//process the allmessages reply
		JSONArray jsonArray = new JSONArray(cio.getRawResponse().getBody());
		
		if (jsonArray != null && jsonArray.length() > 0){
			for (int i = 0; i < jsonArray.length(); i++) {
				//iterating over email objects
				JSONObject jsonObj = (JSONObject) jsonArray.get(i);

				try {		
					JSONEmailProcessor jsonProcessor = new JSONEmailProcessor(jsonObj);
					String emailMessageID = jsonProcessor.getEmailMessageId();
					String gmailThreadID =jsonProcessor.getGmailThreadId();
					//String gmailMessageID = jsonProcessor.getGmailMessageId();
					String subject = jsonProcessor.getSubject();
					int messageTimestamp = jsonProcessor.getMessageDate();
					String fromAddress = jsonProcessor.getFromAddress();
					List<String> toAddresses = jsonProcessor.getToAddresses();
					List<String> ccAddresses = jsonProcessor.getCCAddresses();
					List<String> folders = jsonProcessor.getFolders();
					
					Email email = new Email();
					email.setSubject(subject);
					email.setMessageId(emailMessageID);
					email.setGmailThreadId(gmailThreadID);
					email.setSentTimestamp(messageTimestamp);
					email.setFromAddress(fromAddress);
					email.setCcAddresses(ccAddresses);
					email.setToAddresses(toAddresses);
					email.setFolders(folders);
					//retrieving message content of the email
					System.out.println("the email account : " + emailAccount);
					//email = this.getEmailMessageContent(emailAccount, emailMessageID, email);
					mailbox.addEmail(email);
					
				} catch (Exception e) {
					logger.error("Error while encoding Json message in UPDATE_MAILBOX method", e);
				}
			}
		}
		else {
			//no more results from contextio..persist the mailbox
			mailbox.setSyncing(false);
			//container.persist(mailbox);
			
		}
		logger.info(jsonArray.length() + " mails retrieved from : " + mailbox.getEmailId() );
		return mailbox;
		
	}
	
	/**
	 * add message content fields to the email passed
	 * @param emailAccount
	 * @param msgId
	 * @param email
	 * @return email with message content
	 */
	@Programmatic
	public Email getEmailMessageContent(String emailAccount, String msgId, Email email){
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("emailmessageid", msgId);
		
		ContextIOResponse cioMessageText = contextio_v11.messageText(
				emailAccount, emailParams);
		JSONObject messageJson = new JSONObject(cioMessageText
				.getRawResponse().getBody());
		
		logger.info("email messagetext response : " + cioMessageText.getRawResponse().getBody());
		
		JSONArray messageData = messageJson.getJSONArray("data");
		JSONObject messageObj = (JSONObject) messageData.get(0);

		String contentType = messageObj.getString("type");
		String charSet = messageObj.getString("charset");
		String content = messageObj.getString("content");
		
		List<String> contentTokens = EmailUtils.tokenizeContent(content);
		List<String> subjectTokens = EmailUtils.tokenizeContent(email.getSubject());
		email.setSubjectTerms(subjectTokens);
		email.setBodyTerms(contentTokens);
		email.setContentType(contentType);
		email.setCharSet(charSet);
		return email;
	}
	
	public Map<String,String> discoverIMAPSettings(String emailId){
		Map<String, String> imapSettings = new HashMap<String, String>();
		
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		ContextIOResponse imapDiscoverResponse = contextio_v11.imap_discover(emailParams);
		JSONObject imapResponseJson = new JSONObject(imapDiscoverResponse.getRawResponse().getBody());
		JSONObject accountData = (JSONObject)imapResponseJson.get("data");
		Boolean found = (Boolean)accountData.get("found");
		if (found){
			JSONObject imapObject = (JSONObject)accountData.get("imap");
			String server = (String)imapObject.get("server");
			String username = (String)imapObject.get("username");
			Number port = (Number)imapObject.get("port");
			Boolean useSSL = (Boolean)imapObject.get("useSSL");
			Boolean oauth = (Boolean)imapObject.get("oauth");
			
			imapSettings.put("server", server);
			imapSettings.put("username", username);
			imapSettings.put("port", port.toString());
			imapSettings.put("useSSL", useSSL.toString());
			imapSettings.put("oauth", oauth.toString());	
		}
		return imapSettings;
	}
	
	public UserMailBox connectMailBox(@Named("email address")String emailId, @Named("password")String password, @Named("First Name")String fname, @Named("Last Name")String lname){
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		emailParams.put("password", password);
		emailParams.put("firstname", fname);
		emailParams.put("lastname", lname);
		
		Map<String, String> imapSettings = discoverIMAPSettings(emailId);
		if(imapSettings.get("useSSL").equals("true")){
			imapSettings.remove("useSSL");
			emailParams.put("usessl","1");
		} else {
			emailParams.put("usessl","0");
		}
		emailParams.putAll(imapSettings);
		
		logger.info("The parameters for imap_addAccount operation are as following");
		for(String key: emailParams.keySet()){
			logger.info(key + " : " + emailParams.get(key));
		} 
		
		if (!isEmailAccountConnected(emailId)){
			ContextIOResponse addAccountResponse = contextio_v11.imap_addAccount(emailParams);
			String addAccntMsg = addAccountResponse.getRawResponse().getBody();
			logger.info("ContextIO addAccount response : " + addAccntMsg);
			JSONObject addAccountObject = new JSONObject(addAccntMsg);
			JSONObject addAccountDataObject = (JSONObject)addAccountObject.get("data");
			Integer success = (Integer)addAccountDataObject.get("success");
			
			if (success == 1){
				UserMailBox mb = new UserMailBox();
				mb.setEmailId(emailId);
				mb.setUserFirstName(fname);
				mb.setUserLastName(lname);
				return mb;
			}else {
				String feedbackCode = (String)addAccountDataObject.get("feedbackCode");
				logger.info("ContextIO.connectMailBox(" + emailId +") failed. feedback code : " + feedbackCode);
				return null;
			}
		}else {
			logger.info("Email : " + emailId + " is already connected");
			//TODO: must return the already connected mailbox from datastore using domainObject container
			return null;
			
		}
		
	}
	
	public boolean isEmailAccountConnected(@Named("email address")String emailId){
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		try {
			ContextIOResponse accountInfoResponse = contextio_v11.imap_accountInfo(emailParams);
			String accountInfoResponseString = accountInfoResponse.getRawResponse().getBody();
			logger.info("ContextIO Account Info Response : "  + accountInfoResponseString);
			JSONObject accountInfoJson = new JSONObject(accountInfoResponseString);
			JSONObject accountInfoDataJson = (JSONObject)accountInfoJson.get("data");
			JSONArray emails = (JSONArray)accountInfoDataJson.get("emails");
			if (null != emails){
				for(int x=0 ; x < emails.length() ; x++){
					String emailAddress = emails.getString(x);
					logger.info("connected email adddresses :" + emailAddress);
				}
				return true;
			} else {
				return false;
			}
			
		} catch (JSONException e) {
			logger.error("Error getting account info of : " + emailId, e);
			return false;
		}
	}
	

	@Inject
	DomainObjectContainer container;
	
    //region > dependencies
    @javax.inject.Inject
    private ApplicationSettingsService applicationSettingsService;
 
    private ContextIO_V11 contextio_v11;
    private ContextIO_V20 contextio_v20;
    //endregion
}
