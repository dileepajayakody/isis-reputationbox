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
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.services.settings.ApplicationSettingsService;
import org.apache.isis.applib.services.settings.ApplicationSettingsServiceRW;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailFlag;
import org.nic.isis.reputation.dom.TextContent;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.reputation.utils.JSONEmailProcessor;
import org.nic.isis.reputation.utils.URLUtils;
import org.nic.isis.ri.RandomIndexing;
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

		String key = "samplekey";
		contextio_v11.setKey(key);
		contextio_v20.setKey(key);

		String secret = "sample secret";
		contextio_v11.setSecret(secret);
		contextio_v20.setSecret(secret);

		String accountId = "sample account id";
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

		Map<String, String> params = new HashMap<String, String>();

		// contextio 2.0 impl
		int offset = mailbox.getEmailCount();
		String emailAccount = mailbox.getAccountId();
		params.put("offset", String.valueOf(offset));
		params.put("limit", String.valueOf(limit));
		params.put("sort_order", "desc");
		params.put("include_body", String.valueOf(1));
		params.put("include_headers", String.valueOf(1));
		params.put("include_flags", String.valueOf(1));
		// get emails in inbox
		params.put("folder", "[Gmail]/INBOX");

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

				if (emailData != null && emailData.length() > 0) {
					// initializing a random indexing object with mailbox's text
					// semantics
					RandomIndexing textIndexing = new RandomIndexing(
							mailbox.getWordToIndexVector(),
							mailbox.getWordToMeaningMap());
					// initializing a random indexing object with mailbox's
					// recipient semantics
					RandomIndexing recipientIndexing = new RandomIndexing(
							mailbox.getRecipientToIndexVector(),
							mailbox.getRecipientToMeaningMap());

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
							int messageTimestamp = jsonProcessor
									.getMessageDate();
							String fromAddress = jsonProcessor.getFromAddress();
							List<String> toAddresses = jsonProcessor
									.getToAddresses();
							List<String> ccAddresses = jsonProcessor
									.getCCAddresses();
							EmailFlag emailFlags = jsonProcessor.getFlags();
							String body = jsonProcessor.getBodyContent();

							// process the content
							TextContent bodyTextContent = EmailUtils
									.processText(body);
							TextContent subjectTextContent = EmailUtils
									.processText(subject);

							Email email = new Email();

							email.setSubject(subject);
							email.setMessageId(emailMessageID);
							email.setGmailThreadId(gmailThreadID);
							email.setSentTimestamp(messageTimestamp);
							email.setFromAddress(fromAddress);
							email.setCcAddresses(ccAddresses);
							email.setToAddresses(toAddresses);
							email.setEmailFlags(emailFlags);
							email.setSubjectContent(subjectTextContent);
							email.setBodyContent(bodyTextContent);

							// building context vectors for text and recipients
							// on the go
							textIndexing = emailAnalysisService
									.processTextSemantics(email, textIndexing);
							recipientIndexing = emailAnalysisService
									.processRecipientSemantics(email,
											recipientIndexing);
							mailbox.addEmail(email);
							logger.info("adding email ["
									+ mailbox.getEmailCount() + " ] subject: "
									+ email.getSubject());

						} catch (Exception e) {
							logger.error(
									"Error while decoding email JSON message",
									e);
						}
					}
					// saving new text vectors in the mailbox
					mailbox.setWordToIndexVector(textIndexing
							.getWordToIndexVector());
					mailbox.setWordToMeaningMap(textIndexing
							.getWordToMeaningVector());

					// saving new recipient vectors in the mailbox
					mailbox.setRecipientToIndexVector(recipientIndexing
							.getWordToIndexVector());
					mailbox.setRecipientToMeaningMap(recipientIndexing
							.getWordToMeaningVector());

				} else {
					// no more emails to sync
					mailbox.setSyncing(false);
				}
				logger.info(emailData.length() + " mails retrieved from : "
						+ mailbox.getEmailId() + " from offset : " + offset);

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
