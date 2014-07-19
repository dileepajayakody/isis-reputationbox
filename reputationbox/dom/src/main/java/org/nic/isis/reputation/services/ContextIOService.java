package org.nic.isis.reputation.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.processing.Messager;
import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.services.settings.ApplicationSetting;
import org.apache.isis.applib.services.settings.ApplicationSettingsService;
import org.apache.isis.applib.services.settings.ApplicationSettingsServiceRW;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.TextContent;
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

	// region > init
	@PostConstruct
	public void init() {

		// this.key =
		// applicationSettingsService.find("contextIOApiKey").valueAsString();
		// this.secret =
		// applicationSettingsService.find("contextIOApiSecret").valueAsString();

		contextio_v11 = new ContextIO_V11();
		contextio_v20 = new ContextIO_V20();

		String key = "65kd0b3k";
		// String key = "2n38y7uw";
		contextio_v11.setKey(key);
		contextio_v20.setKey(key);

		String secret = "CetIiO0Ke0Klb2u8";
		// String secret = " NzkC9fCpgpo3DtLj";
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

	// endregion

	// service methods

	/**
	 * Updates the mailbox with new emails
	 * 
	 * @param mailbox
	 * @param limit
	 *            number new emails
	 * @return
	 */
	public UserMailBox updateMailBox(UserMailBox mailbox, int limit) {
		int offset = mailbox.getEmailCount();
		String emailAccount = mailbox.getAccountId();
		String emailAddress = mailbox.getEmailId();
		logger.info("Syncing mailbox for : " + emailAddress
				+ " since last indexed timestamp : "
				+ mailbox.getLastIndexTimestamp());

		// contextio 1.1. impl
		Map<String, String> params = new HashMap<String, String>();

		params.put("since", String.valueOf(mailbox.getLastIndexTimestamp()));
		params.put("limit", String.valueOf(limit));
		ContextIOResponse cio = contextio_v11.allMessages(emailAddress, params);

		// contextio 2.0 impl
		// currently contextio_v2 allMessages response resturns a 403
		// needs to be fixed, and switched to contextio2.0 api
		/*
		 * params.put("limit", String.valueOf(limit)); params.put("sort_order",
		 * "asc"); params.put("include_body", String.valueOf(1));
		 * params.put("include_headers", String.valueOf(1));
		 * params.put("include_flags", String.valueOf(1));
		 * 
		 * ContextIOResponse cio = contextio_v20.getAllMessages(emailAccount,
		 * params);
		 */

		JSONObject json = new JSONObject(cio.getRawResponse().getBody());
		JSONArray data = json.getJSONArray("data");
		int lastIndexedTimestamp = json.getInt("timestamp");

		if (data != null && data.length() > 0) {
			for (int i = 0; i < data.length(); i++) {
				// iterating over emails
				JSONObject emailObject = (JSONObject) data.get(i);
				try {
					JSONEmailProcessor jsonProcessor = new JSONEmailProcessor(
							emailObject);
					String emailMessageID = jsonProcessor.getEmailMessageId();
					String gmailThreadID = jsonProcessor.getGmailThreadId();
					// String gmailMessageID =
					// jsonProcessor.getGmailMessageId();
					String subject = jsonProcessor.getSubject();
					int messageTimestamp = jsonProcessor.getMessageDate();
					String fromAddress = jsonProcessor.getFromAddress();
					List<String> toAddresses = jsonProcessor.getToAddresses();
					List<String> ccAddresses = jsonProcessor.getCCAddresses();

					Email email = new Email();
					email.setSubject(subject);
					email.setMessageId(emailMessageID);
					email.setGmailThreadId(gmailThreadID);
					email.setSentTimestamp(messageTimestamp);
					email.setFromAddress(fromAddress);
					email.setCcAddresses(ccAddresses);
					email.setToAddresses(toAddresses);
					// retrieving message content of the email
					email = this.getEmailMessageContent(emailAddress,
							emailMessageID, email);
					email = this.getMessageHeaders(emailAddress,
							emailMessageID, email);
					mailbox.addEmail(email);

				} catch (Exception e) {
					logger.error("Error while decoding email JSON message ", e);
				}
			}
			mailbox.setSyncing(false);
			mailbox.setLastIndexTimestamp(lastIndexedTimestamp);
			logger.info(data.length() + " mails retrieved from : "
					+ mailbox.getEmailId());
		}

		return mailbox;
	}

	/**
	 * add message content fields to the email passed
	 * 
	 * @param emailAddress
	 * @param msgId
	 * @param email
	 * @return email with message content
	 */
	@Programmatic
	public Email getEmailMessageContent(String emailAddress, String msgId,
			Email email) {
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("emailmessageid", msgId);

		ContextIOResponse cioMessageText = contextio_v11.messageText(
				emailAddress, emailParams);
		JSONObject messageJson = new JSONObject(cioMessageText.getRawResponse()
				.getBody());

		JSONArray messageData = messageJson.getJSONArray("data");
		JSONObject messageObj = (JSONObject) messageData.get(0);

		String contentType = messageObj.getString("type");
		String charSet = messageObj.getString("charset");
		String content = messageObj.getString("content");

		logger.info("passing content to EmailUtils : \n" + content);
		TextContent bodyContent = EmailUtils.processText(content);
		TextContent subjectContent = EmailUtils.processText(email.getSubject());

		email.setSubjectContent(subjectContent);
		email.setBodyContent(bodyContent);
		email.setContentType(contentType);
		email.setCharSet(charSet);

		/*
		 * logger.info("email : " + msgId + " email body actual word strings : "
		 * ); String wordTokenStr = ""; List<String> wordStrings =
		 * bodyContent.getStringTokens(); for(String token : wordStrings){
		 * wordTokenStr += token + "\n"; } logger.info(wordTokenStr);
		 */
		return email;
	}

	public Email getMessageHeaders(String emailAddress, String msgId,
			Email email) {
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("emailmessageid", msgId);
		ContextIOResponse cioMessageText = contextio_v11.messageHeaders(
				emailAddress, emailParams);
		JSONObject messageJson = new JSONObject(cioMessageText.getRawResponse()
				.getBody());

		String messageData = messageJson.getString("data");
		logger.info("message headers for email: " + msgId + " : " + messageData);

		return email;
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
			logger.info("Email : " + emailId + " is already connected");
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

	public void listThreads() {
		contextio_v20.getAllThreads(contextio_v20.getAccountId(), null);
	}

	public void listMessages() {
		contextio_v20.getAllMessages(contextio_v20.getAccountId(), null);
	}

	public void listContacts() {
		contextio_v20.getAllContacts(contextio_v20.getAccountId(), null);
	}

	public void testDiscovery() {
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("source_type", "IMAP");
		emailParams.put("email", "dileepajayakody@gmail.com");
		ContextIOResponse discoveryRes = contextio_v20.discovery(emailParams);
	}

	@Inject
	DomainObjectContainer container;

	// region > dependencies
	@javax.inject.Inject
	private ApplicationSettingsService applicationSettingsService;

	private ContextIO_V11 contextio_v11;
	private ContextIO_V20 contextio_v20;
	// endregion
}
