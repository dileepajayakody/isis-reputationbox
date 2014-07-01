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

/**
 * Class to manage Context.IO API access
 * 
 * @author Thomas Taschauer | tomtasche.at
 */
public class ContextIOService {

	static final String ENDPOINT = "api.context.io";
	private final static Logger logger = LoggerFactory
			.getLogger(ContextIOService.class);


    //region > constructor
    public ContextIOService(){
        this.ssl = true;
        this.saveHeaders = false;
        this.apiVersion = "1.1";
    }
    /**
     * Instantiate a new ContextIO object. Your OAuth consumer key and secret can be
     * found under the "settings" tab of the developer console (https://console.context.io/#settings)
     * @param key Your Context.IO OAuth consumer key
     * @param secret Your Context.IO OAuth consumer secret
     */
    public ContextIOService(String key, String secret) {
        this();
        this.key = key;
        this.secret = secret;
    }
    //endregion

    //region > init
    @PostConstruct
    public void init(){

        //this.key = applicationSettingsService.find("contextIOApiKey").valueAsString();
        //this.secret = applicationSettingsService.find("contextIOApiSecret").valueAsString();
        this.key = "65kd0b3k";
        this.secret = "CetIiO0Ke0Klb2u8";
        this.ssl = true;
        this.saveHeaders = false;
        this.apiVersion = "1.1";
		/*List<ApplicationSetting> appSettings = applicationSettingsService.listAll();
		for (ApplicationSetting setting:appSettings){
			logger.info(setting.getKey() + " : " + setting.getValueRaw() + " desc :" + setting.getDescription() );
		}*/
    }
    //endregion

    //region > key (property, programmatic)
    String key;
	@Programmatic
	public String getKey() {
		return key;
	}

	@Programmatic
	public void setKey(String key) {
		this.key = key;
	}
    //endregion

    //region > secret (property, programmatic)
	String secret;
	@Programmatic
	public String getSecret() {
		return secret;
	}

	@Programmatic
	public void setSecret(String secret) {
		this.secret = secret;
	}
    //endregion

	@Programmatic
	public void setLastResponse(ContextIOResponse lastResponse) {
		this.lastResponse = lastResponse;
	}

    //region > ssl (property, programmatic)
	boolean ssl;

	@Programmatic
	public boolean isSsl() {
		return ssl;
	}

	/**
	 * Specify whether or not API calls should be made over a secure connection.
	 * HTTPS is used on all calls by default.
	 * @param sslOn Set to false to make calls over HTTP, true to use HTTPS
	 */
	@Programmatic
	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}
    //endregion

    //region > apiVersion (property, programmatic)
	String apiVersion;
	@Programmatic
	public String getApiVersion() {
		return apiVersion;
	}

	/**
	 * Set the API version. By default, the latest official version will be used
	 * for all calls.
	 * @param apiVersion Context.IO API version to use
	 */
	@Programmatic
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
    //endregion

    //region > saveHeaders (property, programmatic)
	boolean saveHeaders;
	@Programmatic
	public boolean isSaveHeaders() {
		return saveHeaders;
	}

	@Programmatic
	public void setSaveHeaders(boolean saveHeaders) {
		this.saveHeaders = saveHeaders;
	}
    //endregion

    //region > authHeaders (property, programmatic)
	boolean authHeaders;
	@Programmatic
	public boolean isAuthHeaders() {
		return authHeaders;
	}

	/**
	 * Specify whether OAuth parameters should be included as URL query parameters
	 * or sent as HTTP Authorization headers. The default is URL query parameters.
	 * @param authHeadersOn Set to true to use HTTP Authorization headers, false to use URL query params
	 */
	@Programmatic
	public void setAuthHeaders(boolean authHeaders) {
		this.authHeaders = authHeaders;
	}
    //endregion

    //region > lastResponse (property, programmatic, read-only)
    ContextIOResponse lastResponse;
	/**
	 * Returns the ContextIOResponse object for the last API call.
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse getLastResponse() {
		return lastResponse;
	}
    //endregion

    //region > addresses (programmatic)
    /**
	 * Returns the 20 contacts with whom the most emails were exchanged.
	 * @link http://context.io/docs/1.1/addresses
	 * @param account accountId or email address of the mailbox you want to query
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse addresses(String account) {
		return get(account, "adresses.json", null);
	}
    //endregion

    //region > allFiles (programmatic)
	/**
	 * Returns the 25 most recent attachments found in a mailbox. Use limit to change that number.
	 * @link http://context.io/docs/1.1/allfiles
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: since, limit
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse allFiles(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"since", "limit"});

		return get(account, "allfiles.json", params);
	}
    //endregion

    //region > allMessages (programmatic)
	/**
	 * Returns the 25 most recent mails found in a mailbox. Use limit to change that number.
	 * This is useful if you're polling a mailbox for new messages and want all new messages
	 * indexed since a given timestamp.
	 * @link http://context.io/docs/1.1/allmessages
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: since, limit
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse allMessages(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"since", "limit"});

		return get(account, "allmessages.json", params);
	}
    //endregion

    //region > contactFiles (programmatic)
	/**
	 * This call returns the latest attachments exchanged with one
	 * or more email addresses
	 * @link http://context.io/docs/1.1/contactfiles
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'email', 'to', 'from', 'cc', 'bcc', 'limit'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse contactFiles(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"email", "to", "from", "cc", "bcc", "limit"});

		return get(account, "contactfiles.json", params);
	}
    //endregion

    //region > contactMessages (programmatic)
	/**
	 * This call returns list of email messages for one or more contacts. Use the email
	 * parameter to get emails where a contact appears in the recipients or is the sender.
	 * Use to, from and cc parameters for more precise control.
	 * @link http://context.io/docs/1.1/contactmessages
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'email', 'to', 'from', 'cc', 'bcc', 'limit'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse contactMessages(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"email", "to", "from", "cc", "bcc", "limit"});

		return get(account, "contactmessages.json", params);
	}
    //endregion

    //region > contactSearch (programmatic)
	/**
	 * This call search the lists of contacts.
	 * @link http://context.io/docs/1.1/contactsearch
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'search'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse contactSearch(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"search"});

		return get(account, "contactsearch.json", params);
	}
    //endregion

    //region > diffSummary (programmatic)
	/**
	 * Given two files, this will return the list of insertions and deletions made
	 * from the oldest of the two files to the newest one.
	 * @link http://context.io/docs/1.1/diffsummary
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'fileId1', 'fileId2'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse diffSummary(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"fileId1", "fileId2"});

		params.put("generate", "1");

		return get(account, "diffsummary.json", params);
	}
    //endregion

    //region > downloadFile, fileRevisions, relatedFiles, fileSearch (programmatic)
	/**
	 * Returns the content a given attachment. If you want to save the attachment to
	 * a file, set $saveAs to the destination file name. If $saveAs is left to null,
	 * the function will return the file data.
	 * on the
	 * @link http://context.io/docs/1.1/downloadfile
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'fileId'
	 * @param saveAs Path to local file where the attachment should be saved to.
	 * @return mixed
	 */
	@Programmatic
	public void downloadFile(String account, Map<String, String> params, File saveAs) {
		throw new UnsupportedOperationException("Not yet implemented, sorry.");
	}

	/**
	 * Returns a list of revisions attached to other emails in the
	 * mailbox for one or more given files (see fileid parameter below).
	 * @link http://context.io/docs/1.1/filerevisions
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'fileId', 'fileName'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse fileRevisions(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"fileid", "filename"});

		return get(account, "filerevisions.json", params);
	}

	/**
	 * Returns a list of files that are related to the given file.
	 * Currently, relation between files is based on how similar their names are.
	 * You must specify either the fileId of fileName parameter
	 * @link http://context.io/docs/1.1/relatedfiles
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'fileId', 'fileName'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse relatedFiles(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"fileid", "filename"});

		return get(account, "relatedfiles.json", params);
	}
    //endregion

    //region > fileSearch (programmatic)
	/**
	 *
	 * @link http://context.io/docs/1.1/filesearch
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'fileName'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse fileSearch(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"filename"});

		return get(account, "filesearch.json", params);
	}
    //endregion

    //region > IMAP account stuff(programmatic)
	/**
	 *
	 * @link http://context.io/docs/1.1/imap/accountinfo
	 */
	@Programmatic
	public ContextIOResponse imap_accountInfo(Map<String, String> params) {
		params = filterParams(params, new String[] {"email", "userid"});

		return get("", "imap/accountinfo.json", params);
	}

	/**
	 * @link http://context.io/docs/1.1/imap/addaccount
	 * @param params Query parameters for the API call: 'email', 'server', 'username', 'password', 'oauthconsumername', 'oauthtoken', 'oauthtokensecret', 'usessl', 'port'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse imap_addAccount(Map<String, String> params) {
		params = filterParams(params, new String[] {"email", "server", "username", "oauthconsumername", "oauthtoken", "oauthtokensecret", "password", "usessl", "port", "firstname", "lastname"});

		return get("", "imap/addaccount.json", params);
	}

    /**
     * Modify the IMAP server settings of an already indexed account
     * @link http://context.io/docs/1.1/imap/modifyaccount
     * @param params Query parameters for the API call: 'credentials', 'mailboxes'
     * @return ContextIOResponse
     */
    @Programmatic
    public ContextIOResponse imap_modifyAccount(String account, Map<String, String> params) {
        params = filterParams(params, new String[] {"credentials", "mailboxes"});

        return get(account, "imap/modifyaccount.json", params);
    }

    /**
     * Remove the connection to an IMAP account
     * @link http://context.io/docs/1.1/imap/removeaccount
     * @return ContextIOResponse
     */
    @Programmatic
    public ContextIOResponse imap_removeAccount(String account, Map<String, String> params) {
        params = filterParams(params, new String[] {"label"});

        return get(account, "imap/removeaccount.json", params);
    }
    //endregion

    //region > IMAP settings
    /**
	 * Attempts to discover IMAP settings for a given email address
	 * @link http://context.io/docs/1.1/imap/discover
	 * @param params either a string or assoc array
	 *    with email as its key
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse imap_discover(Map<String, String> params) {
		// TODO: differs from original implementiation
		params = filterParams(params, new String[] {"email"});

		return get("", "imap/discover.json", params);
	}
    //endregion

    //region > imap_resetStatus (programmatic)

    /**
	 * When Context.IO can't connect to your IMAP server,
	 * the IMAP server gets flagged as unavailable in our database.
	 * Use this call to re-enable the syncing.
	 * @link http://context.io/docs/1.1/imap/resetstatus
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse imap_resetStatus(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"label"});

		return get(account, "imap/resetstatus.json", params);
	}
    //endregion

    //region > IMAP oAuth
    /**
	 *
	 * @link http://context.io/docs/1.1/imap/oauthproviders
	 */
	@Programmatic
	public ContextIOResponse imap_deleteOAuthProvider(Map<String, String> params) {
		params = filterParams(params, new String[] {"key"});

		params.put("action", "delete");

		return get("", "imap/oauthproviders.json", params);
	}

	/**
	 *
	 * @link http://context.io/docs/1.1/imap/oauthproviders
	 */
	@Programmatic
	public ContextIOResponse imap_setOAuthProvider(Map<String, String> params) {
		params = filterParams(params, new String[] {"type", "key", "secret"});

		return get("", "imap/oauthproviders.json", params);
	}

	/**
	 *
	 * @link http://context.io/docs/1.1/imap/oauthproviders
	 */
	@Programmatic
	public ContextIOResponse imap_getOAuthProviders(Map<String, String> params) {
		params = filterParams(params, new String[] {"key"});

		return get("", "imap/oauthproviders.json", params);
	}
    //endregion

    //region > messageHeaders, messageInfo, messageText, search (for message)

    /**
	 * Returns the message headers of a message.
	 * A message can be identified by the value of its Message-ID header
	 * or by the combination of the date sent timestamp and email address
	 * of the sender.
	 * @link http://context.io/docs/1.1/messageheaders
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'emailMessageId', 'from', 'dateSent',
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse messageHeaders(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"emailmessageid", "from", "datesent"});

		return get(account, "messageheaders.json", params);
	}

	/**
	 * Returns document and contact information about a message.
	 * A message can be identified by the value of its Message-ID header
	 * or by the combination of the date sent timestamp and email address
	 * of the sender.
	 * @link http://context.io/docs/1.1/messageinfo
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'emailMessageId', 'from', 'dateSent', 'server', 'mbox', 'uid'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse messageInfo(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"emailmessageid", "from", "datesent", "server", "mbox", "uid"});

		return get(account, "messageinfo.json", params);
	}

	/**
	 * Returns the message body (excluding attachments) of a message.
	 * A message can be identified by the value of its Message-ID header
	 * or by the combination of the date sent timestamp and email address
	 * of the sender.
	 * @link http://context.io/docs/1.1/messagetext
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'emailMessageId', 'from', 'dateSent','type
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse messageText(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"emailmessageid", "from", "datesent", "type"});

		return get(account, "messagetext.json", params);
	}

	/**
	 * Returns message information
	 * @link http://context.io/docs/1.1/search
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'subject', 'limit'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse search(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"subject", "limit"});

		return get(account, "search.json", params);
	}
    //endregion

    //region > thread

    /**
	 * Returns message and contact information about a given email thread.
	 * @link http://context.io/docs/1.1/threadinfo
	 * @param account accountId or email address of the mailbox you want to query
	 * @param params Query parameters for the API call: 'gmailthreadid'
	 * @return ContextIOResponse
	 */
	@Programmatic
	public ContextIOResponse threadInfo(String account, Map<String, String> params) {
		params = filterParams(params, new String[] {"gmailthreadid", "emailmessageid"});

		return get(account, "threadinfo.json", params);
	}
    //endregion

    //region > build_baseurl, build_url
	@Programmatic
	public String build_baseurl() {
		String url = "http";
		if (ssl) {
			url = "https";
		}

		return url + "://" + ENDPOINT + "/" + apiVersion + '/';
	}

	@Programmatic
	public String build_url(String action) {
		return build_baseurl() + action;
	}
    //endregion

    //region > helpers (get, post)
    @Programmatic
	public ContextIOResponse[] get(String[] accounts, String action, Map<String, String> params) {
		ContextIOResponse[] responses = new ContextIOResponse[accounts.length];
		for (int i = 0; i < accounts.length; i++) {
			responses[i] = doCall("GET", accounts[i], action, params);
		}

		return responses;
	}

	@Programmatic
	public ContextIOResponse get(String account, String action, Map<String, String> params) {
		return doCall("GET", account, action, params);
	}

	@Programmatic
	public ContextIOResponse post(String account, String action, Map<String, String> params) {
		return doCall("POST", account, action, params);
	}

	@Programmatic
	public ContextIOResponse doCall(String method, String account, String action, Map<String, String> params) {
		// TODO: differs from original implementiation
		
		if (account != null && !account.equals("")) {
			if (params == null) {
				params = new HashMap<String, String>();
			}

			params.put("account", account);
		}

		String baseUrl = build_url(action);
		if ("GET".equals(method)) {
			baseUrl = URLUtils.appendParametersToQueryString(baseUrl, params);
		}

		OAuthService service = new ServiceBuilder().provider(ContextIOApi.class).apiKey(this.key).apiSecret(this.secret).build();
		OAuthRequest request = new OAuthRequest(Verb.GET, baseUrl);
		
		Token nullToken = new Token("", "");
		service.signRequest(nullToken, request);

		Response oauthResponse = request.send();

		lastResponse = new ContextIOResponse(oauthResponse.getCode(), request.getHeaders(), oauthResponse.getHeaders(), oauthResponse);
		if (lastResponse.isHasError()) {
			return null;
		} else {
			return lastResponse;
		}
	}

	@Programmatic
	public Map<String, String> filterParams(Map<String, String> givenParams, String[] validParams) {
		Map<String, String> filteredParams = new HashMap<String, String>();

		for (String validKey : validParams) {
			for (String givenKey : givenParams.keySet()) {
				if (givenKey.equalsIgnoreCase(validKey)) {
					filteredParams.put(validKey, givenParams.get(givenKey));
				}
			}
		}

		return filteredParams;
	}
    //endregion

    //service methods
	
	/**
	 * Updates the mailbox with new emails
	 * @param mailbox
	 * @param limit number new emails
	 * @return
	 */
	public UserMailBox updateMailBox(UserMailBox mailbox, int limit) {
		int since = mailbox.getLastIndexTimestamp();
		logger.info("Syncing Email Account : " + mailbox.getEmailId() + " since last indexed timestamp : " + since);
		Map<String, String> params = new HashMap<String, String>();
		params.put("since", String.valueOf(since));
		params.put("limit", String.valueOf(limit));
		ContextIOResponse cio = this.allMessages(mailbox.getEmailId(), params);
		
		//if since=0 this is the intial email retrieval, need to index all emails upto now..
		
		JSONObject json = new JSONObject(cio.getRawResponse().getBody());
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
				//retrieving message content of the email
				email = this.getEmailMessageContent(mailbox.getEmailId(), emailMessageID, email);
				
				mailbox.addEmail(email);
				
			} catch (Exception e) {
				logger.error("Error while encoding Json message", e);
			}
		}
		
		mailbox.setLastIndexTimestamp(lastIndexedTimestamp);
		logger.info(data.length() + " mails retrieved from : " + mailbox.getEmailId() + " indexed timestamp : " + lastIndexedTimestamp);
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
		ContextIOResponse cioMessageText = this.messageText(
				emailAccount, emailParams);
		JSONObject messageJson = new JSONObject(cioMessageText
				.getRawResponse().getBody());
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
		ContextIOResponse imapDiscoverResponse = this.imap_discover(emailParams);
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
			ContextIOResponse addAccountResponse = this.imap_addAccount(emailParams);
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
			//must return the already connected mailbox from datastore using domainObject container
			return null;
			
		}
		
	}
	
	public boolean isEmailAccountConnected(@Named("email address")String emailId){
		Map<String, String> emailParams = new HashMap<String, String>();
		emailParams.put("email", emailId);
		try {
			ContextIOResponse accountInfoResponse = this.imap_accountInfo(emailParams);
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
    //endregion
}
