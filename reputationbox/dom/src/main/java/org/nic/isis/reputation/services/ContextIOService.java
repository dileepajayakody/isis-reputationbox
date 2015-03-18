package org.nic.isis.reputation.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.services.settings.ApplicationSettingsService;
import org.apache.isis.applib.services.settings.ApplicationSettingsServiceRW;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.reputation.dom.ContextVectorMap;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailFlag;
import org.nic.isis.reputation.dom.IndexVectorMap;
import org.nic.isis.reputation.dom.TextContent;
import org.nic.isis.reputation.dom.TextTokenMap;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.reputation.utils.JSONEmailProcessor;
import org.nic.isis.reputation.utils.URLUtils;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.ri.SemanticSpace;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs.ContentInfo;
import at.tomtasche.contextio.ContextIOApi;
import at.tomtasche.contextio.ContextIOResponse;
import at.tomtasche.contextio.ContextIO_V11;
import at.tomtasche.contextio.ContextIO_V20;

/**
 * Class to manage Context.IO API access
 * 
 */
@Hidden
public class ContextIOService {

	private final static Logger logger = LoggerFactory
			.getLogger(ContextIOService.class);

	
	// region > init
	@PostConstruct
	public void init() {

		// this.key =
		// applicationSettingsService.find("contextIOApiKey").valueAsString();
		// this.secret =
		// applicationSettingsService.find("contextIOApiSecret").valueAsString();

		String key = "33333";
		String secret = "xxxx";
		String accountId = "eeeeeedddd";
		
		contextio_v11 = new ContextIO_V11();
		contextio_v20 = new ContextIO_V20();

		
		contextio_v11.setKey(key);
		contextio_v20.setKey(key);

		contextio_v11.setSecret(secret);
		contextio_v20.setSecret(secret);

		contextio_v11.setAccountId(accountId);
		contextio_v20.setAccountId(accountId);

		contextio_v11.setSsl(true);
		contextio_v20.setSsl(true);

		contextio_v11.setSaveHeaders(true);
		contextio_v20.setSaveHeaders(true);
	}

	// endregion

	// service methods
	/**
	 * Updates the mailbox with new emails
	 * 
	 * @param mailbox
	 * @param limit
	 *            number new emails
	 * @param processType to indicate the analysisType : randomIndexing(1), temporalrandomIndexing(2) etc           
	 * @return
	 */
	@Programmatic
	public UserMailBox updateMailBox(UserMailBox mailbox, int limit, int processType, long mailboxAddedTimeStamp) {

		Map<String, String> params = new HashMap<String, String>();
		SemanticSpace textSemantics = null;
		SemanticSpace recipientSemantics = null;
		
		//getting indexVectors and contextVectors of the mailbox, if null intialize them
		IndexVectorMap contentIndexVectorMap =  mailbox.getContentIndexVectorMap();
		if(contentIndexVectorMap == null){
			contentIndexVectorMap = new IndexVectorMap();
		}
		ContextVectorMap contentContextVectorMap = mailbox.getContentContextVectorMap();
		if(contentContextVectorMap == null){
			contentContextVectorMap = new ContextVectorMap();
		}
		IndexVectorMap recipientIndexVectorMap = mailbox.getRecipientIndexVectorMap();
		if(recipientIndexVectorMap == null){
			recipientIndexVectorMap = new IndexVectorMap();
		}
		ContextVectorMap recipientContextVectorMap = mailbox.getRecipientContextVectorMap();
		if(recipientContextVectorMap == null){
			recipientContextVectorMap = new ContextVectorMap();
		}
		logger.info("Starting updating mailbox using ContextIOService..");
		logger.info(" the content index words sizes : " + contentIndexVectorMap.getIndexWords().size() + " : " 
				+ contentIndexVectorMap.getIndexVectors().size());
		logger.info(" the content context words : " + contentContextVectorMap.getContextWords().size() + " : "
				+ contentContextVectorMap.getContextVectors().size());
		logger.info(" the recipient index words : " + recipientIndexVectorMap.getIndexWords().size() + " : " 
				+ recipientIndexVectorMap.getIndexVectors().size());
		logger.info(" the recipient context words : " + recipientContextVectorMap.getContextWords().size() + " : "
				+ recipientContextVectorMap.getContextVectors().size());
		
		//get emails for last 1/2 months initially	
		long mailsFromDateTimeStamp = mailboxAddedTimeStamp - (1 * 2628000 / 8);

		// contextio 2.0 impl
		int offset = mailbox.getAllEmails().size();
		String emailAccount = mailbox.getAccountId();
		params.put("offset", String.valueOf(offset));
		params.put("limit", String.valueOf(limit));
		params.put("sort_order", "asc");
		params.put("include_body", String.valueOf(1));
		//params.put("include_headers", String.valueOf(1));
		params.put("include_flags", String.valueOf(1));
		params.put("date_after", String.valueOf(mailsFromDateTimeStamp));
		// get emails in inbox
		params.put("folder", "INBOX");

		try {
			ContextIOResponse cio = contextio_v20.getAllMessages(emailAccount,
					params);
			int httpResponseCode = cio.getCode();
			// check if the http response returns an error: eg: 401
			if (httpResponseCode != 200) {
				logger.error(" Email response from ContextIO returns a :"
						+ httpResponseCode);
			} else {
				JSONArray emailData = new JSONArray(cio.getRawResponse()
						.getBody());
				
				//check if there is more email data to retrieve
				if (emailData != null && emailData.length() > 0) {
					// initializing a random indexing object with mailbox's text
					// index vectors
					if(processType == 1){
						
						//testing by dileepa
						/*textSemantics = new RandomIndexing(
								mailbox.getWordToIndexVector(),
								mailbox.getWordToMeaningMap());
						*/
						textSemantics = new RandomIndexing(contentIndexVectorMap.getIndexVectorMap(), 
								contentContextVectorMap.getContextVectorMap());
						
						// initializing a random indexing object with mailbox's
						// recipient index vectors
						
						//testing by dileepa
						/*recipientSemantics = new RandomIndexing(
								mailbox.getRecipientToIndexVector(),
								mailbox.getRecipientToMeaningMap());
						*/
						recipientSemantics = new RandomIndexing(recipientIndexVectorMap.getIndexVectorMap(),
								recipientContextVectorMap.getContextVectorMap());
						
					}
					
					//the new emails should be processed and rep results should be sent to the mailbox
					List<Email> newEmails = new ArrayList<Email>();
					
					for (int i = 0; i < emailData.length(); i++) {
						// iterating over emails
						JSONObject emailObject = (JSONObject) emailData.get(i);
						try {
							JSONEmailProcessor jsonProcessor = new JSONEmailProcessor(
									emailObject);
							String emailMessageID = jsonProcessor
									.getEmailMessageId();
							String gmailThreadID = jsonProcessor
									.getGmailThreadId();
							// String gmailMessageID =
							// jsonProcessor.getGmailMessageId();
							String subject = jsonProcessor.getSubject();
							//because db.persistence length for String is 255
							if(subject.length() > 255){
								subject = subject.substring(0, 254);
							}

							long messageTimestamp = jsonProcessor
									.getMessageDate();
							String fromAddress = jsonProcessor.getFromAddress();
							List<String> toAddresses = jsonProcessor
									.getToAddresses();
							List<String> ccAddresses = jsonProcessor
									.getCCAddresses();
							EmailFlag emailFlags = jsonProcessor.getFlags();
							String body = jsonProcessor.getBodyContent();
							List<String> folders = jsonProcessor.getFolders();

							// process the content
							//logger.info("processing email subject content...");
							TextContent subjectTextContent = EmailUtils
									.processText(subject);
							//logger.info("processing email body content...");
							TextContent bodyTextContent = EmailUtils
									.processText(body);
							
							Email email = new Email();

							email.setMailboxId(mailbox.getEmailId());
							
							email.setSubject(subject);
							email.setMessageId(emailMessageID);
							email.setGmailThreadId(gmailThreadID);
							email.setSentTimestamp(messageTimestamp);
							email.setFromAddress(fromAddress);
							email.setCcAddresses(ccAddresses);
							email.setToAddresses(toAddresses);
							
							for(String toAddress : toAddresses){
								if(toAddress.equalsIgnoreCase(mailbox.getEmailId())){
									email.setDirect(true);
									break;
								}
							}
							for(String ccAddress : ccAddresses){
								if(ccAddress.equalsIgnoreCase(mailbox.getEmailId())){
									email.setCCd(true);
									break;
								}
							}
							//labels/folders for the email
							email.setFolders(folders);
							//email.setEmailFlags(emailFlags);
							//processing email flags
							if(emailFlags.isFlagged()){
								email.setFlagged(true);
							}
							if(emailFlags.isAnswered()){
								email.setAnswered(true);
							}
							if(emailFlags.isDeleted()){
								email.setDeleted(true);
							}
							if(emailFlags.isSeen()){
								email.setSeen(true);
							}
								
							email.setSubjectContent(subjectTextContent);
							email.setBodyContent(bodyTextContent);
							
							
							TextTokenMap textTokenMap = new TextTokenMap();
							textTokenMap.populateWordFrequenciesFromTextContent(subjectTextContent);
							textTokenMap.populateWordFrequenciesFromTextContent(bodyTextContent);
							
							email.setWordFrequencyMap(textTokenMap);
							
							//just intake emails from inbox; we are only considering incoming emails for clustering
							//email.setFolders(folders);
							//have to process headers too

							// building context vectors for text and recipients
							// on the go
							if(processType == 1){
							/*	logger.info("Processing text semantics for email id : " + email.getMessageId() + "\n subject: " 
										+ subject + " \n body  : " + body);*/
								textSemantics = emailAnalysisService
										.processTextSemantics(email, textSemantics);
								
							//	logger.info("Processing recipient semantics for email id : " + email.getMessageId());
								recipientSemantics = emailAnalysisService
										.processPeopleSemantics(email,
												recipientSemantics);
								
							}
							
							
							logger.info("adding email to mailbox with size ["
									+ mailbox.getAllEmails().size() + " ] subject: "
									+ email.getSubject() + " email sent timestamp : " + email.getSentTimestamp());
							//adding email to mailbox...
							mailbox.addEmail(email);
							newEmails.add(email);
							logger.info("end of process for email....\n"
									+ "===========================================================================\n");
						} catch (Exception e) {
							logger.error(
									"Error while creating email and adding to mailbox",
									e);
						}
					}
					
					// saving new text vectors in the mailbox
					
					/*mailbox.setWordToIndexVector(textSemantics
							.getWordToIndexVector());
					//this global wordToMeaningVector is not used in RI now, its initialized per email
					//after comparing performance, remove this
					mailbox.setWordToMeaningMap(textSemantics
							.getWordToMeaningVector());
					*/
					contentIndexVectorMap.setIndexVectorMap(textSemantics.getWordToIndexVector());
					contentContextVectorMap.setContextVectorMap(textSemantics.getWordToMeaningVector());
					
					//logger.error("the new content Index vector for mailbox : " + mailbox.getEmailId() + " : " + contentIndexVectorMap.toString());
					mailbox.setContentIndexVectorMap(contentIndexVectorMap);
					//logger.error("the new content Context vector for mailbox : " + mailbox.getEmailId() + " : " + contentContextVectorMap.toString());
					mailbox.setContentContextVectorMap(contentContextVectorMap);
					
					// saving new recipient vectors in the mailbox
					/*mailbox.setRecipientToIndexVector(recipientSemantics
							.getWordToIndexVector());
					//this global wordToMeaningVector is not used in RI now, its initialized per email
					//after comparing performance, remove this
					mailbox.setRecipientToMeaningMap(recipientSemantics
							.getWordToMeaningVector());*/
					
					recipientIndexVectorMap.setIndexVectorMap(recipientSemantics.getWordToIndexVector());
					//logger.info("the recipientSemantics wordToIndexVector size: " + recipientSemantics.getWordToIndexVector().size());
					recipientContextVectorMap.setContextVectorMap(recipientSemantics.getWordToMeaningVector());
					//logger.info("the recipientSemantics wordToMeaningVector size: " + recipientSemantics.getWordToMeaningVector().size());
					
					//logger.error("the new recipient Index vector for mailbox : " + mailbox.getEmailId() + " : " + recipientIndexVectorMap.toString());
					mailbox.setRecipientIndexVectorMap(recipientIndexVectorMap);
					//logger.error("the new recipient Context vector for mailbox : " + mailbox.getEmailId() + " : " + recipientContextVectorMap.toString());
					mailbox.setRecipientContextVectorMap(recipientContextVectorMap);
					
					
					//before sending results create a cluster profile and add it to cluster results
				/*	List<EmailContentCluster> contentClusters = mailbox.getReputationDataModel().getContentClusters();
					for(EmailContentCluster cluster : contentClusters){
						List<Email> emails = cluster.getContentEmails();
						Map<String, Integer> contentWords = new HashMap<String, Integer>();
						for(Email mail: emails){
							Map<String, Integer> wordFrequencies = mail.getWordFrequencyMap().getWordFrequencyMap();
							
						}
					}*/

					//sending the reputation results of the new emails processed
					EmailUtils.sendReputationResults(mailbox.getEmailId(), newEmails);
					
					//if all emails are processed and the last set of emailData are lesser than the limit set
					if(emailData.length() < limit){
						mailbox.setSyncing(false);
					}
					
					
				} else {
					// no more emails to sync
					mailbox.setSyncing(false);
				}
				logger.info(emailData.length() + " mails retrieved from : "
						+ mailbox.getEmailId() + " from offset : " + offset);

			}
		} catch (Exception e) {
			logger.error("Error while updating mailbox", e);
		}

		return mailbox;
	}

	public Map<String, String> discoverIMAPSettings(String emailId) {
		Map<String, String> imapSettings = new HashMap<String, String>();

		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		ContextIOResponse imapDiscoverResponse = contextio_v11
				.imap_discover(emailParams);
		JSONObject imapResponseJson = new JSONObject(imapDiscoverResponse
				.getRawResponse().getBody());
		JSONObject accountData = (JSONObject) imapResponseJson.get("data");
		Boolean found = (Boolean) accountData.get("found");
		if (found) {
			JSONObject imapObject = (JSONObject) accountData.get("imap");
			String server = (String) imapObject.get("server");
			String username = (String) imapObject.get("username");
			Number port = (Number) imapObject.get("port");
			Boolean useSSL = (Boolean) imapObject.get("useSSL");
			Boolean oauth = (Boolean) imapObject.get("oauth");

			imapSettings.put("server", server);
			imapSettings.put("username", username);
			imapSettings.put("port", port.toString());
			imapSettings.put("useSSL", useSSL.toString());
			imapSettings.put("oauth", oauth.toString());
		}
		return imapSettings;
	}

	public UserMailBox connectMailBox(@Named("email address") String emailId,
			@Named("password") String password,
			@Named("First Name") String fname, @Named("Last Name") String lname) {
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		emailParams.put("password", password);
		emailParams.put("firstname", fname);
		emailParams.put("lastname", lname);

		Map<String, String> imapSettings = discoverIMAPSettings(emailId);
		if (imapSettings.get("useSSL").equals("true")) {
			imapSettings.remove("useSSL");
			emailParams.put("usessl", "1");
		} else {
			emailParams.put("usessl", "0");
		}
		emailParams.putAll(imapSettings);

		logger.info("The parameters for imap_addAccount operation are as following");
		for (String key : emailParams.keySet()) {
			logger.info(key + " : " + emailParams.get(key));
		}

		if (!isEmailAccountConnected(emailId)) {

			ContextIOResponse addAccountResponse = contextio_v11
					.imap_addAccount(emailParams);
			String addAccntMsg = addAccountResponse.getRawResponse().getBody();
			logger.info("ContextIO addAccount response : " + addAccntMsg);
			JSONObject addAccountObject = new JSONObject(addAccntMsg);
			JSONObject addAccountDataObject = (JSONObject) addAccountObject
					.get("data");
			Integer success = (Integer) addAccountDataObject.get("success");

			if (success == 1) {
				UserMailBox mb = new UserMailBox();
				mb.setEmailId(emailId);
				mb.setUserFirstName(fname);
				mb.setUserLastName(lname);
				return mb;
			} else {
				String feedbackCode = (String) addAccountDataObject
						.get("feedbackCode");
				logger.error("ContextIO.connectMailBox(" + emailId
						+ ") failed. feedback code : " + feedbackCode);
				return null;
			}
		} else {
			logger.info("Mailbox : " + emailId + " is already connected");
			return findUserMailBox(emailId);
		}

	}

	public UserMailBox findUserMailBox(@Named("email id") String emailId) {
		QueryDefault<UserMailBox> query = QueryDefault.create(
				UserMailBox.class, "findMailbox", "emailId", emailId);
		return container.firstMatch(query);
	}

	public boolean isEmailAccountConnected(
			@Named("email address") String emailId) {
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		try {
			ContextIOResponse accountInfoResponse = contextio_v11
					.imap_accountInfo(emailParams);
			String accountInfoResponseString = accountInfoResponse
					.getRawResponse().getBody();
			logger.info("ContextIO Account Info Response : "
					+ accountInfoResponseString);
			JSONObject accountInfoJson = new JSONObject(
					accountInfoResponseString);
			JSONObject accountInfoDataJson = (JSONObject) accountInfoJson
					.get("data");
			JSONArray emails = (JSONArray) accountInfoDataJson.get("emails");
			if (null != emails) {
				for (int x = 0; x < emails.length(); x++) {
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

	// testing contextio 2.0 requests
	public void listAllMailBoxes() {
		ContextIOResponse accountsResponse = contextio_v20.getAccounts();
	}

	@javax.inject.Inject
	DomainObjectContainer container;
	// region > dependencies
	@javax.inject.Inject
	private ApplicationSettingsService applicationSettingsService;
	@javax.inject.Inject
	EmailAnalysisService emailAnalysisService;
	private ContextIO_V11 contextio_v11;
	private ContextIO_V20 contextio_v20;

	// endregion
}
