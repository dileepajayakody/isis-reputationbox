package at.tomtasche.contextio;

import java.util.HashMap;
import java.util.Map;

import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.utils.URLUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextIO_V20 extends ContextIOApi {

	private final static Logger logger = LoggerFactory
			.getLogger(ContextIO_V20.class);

	// region > constructor
	public ContextIO_V20() {
		this.ssl = true;
		this.saveHeaders = false;
		this.apiVersion = "2.0";
	}

	public ContextIO_V20(String key, String secret) {
		this();
		this.key = key;
		this.secret = secret;
	}

	// endregion

	// region > allMessages (programmatic)
	/**
	 * Returns the 100 most recent mails found in a mailbox. Use limit to change
	 * that number.
	 * 
	 * @link http://context.io/docs/2.0/accounts/messages
	 * @param accountId
	 *            contextio accountId of the connected mailbox you want to query
	 * @param params
	 *            Query parameters for the API call: include_body,
	 *            include_headers, limit
	 * @return ContextIOResponse
	 */
	public ContextIOResponse getAllMessages(String accountId,
			Map<String, String> params) {
		// params = filterParams(params, new String[] { "include_thread_size",
		// "sort_order", "limit", "offset" });

		return get(accountId, "messages/", params);
	}

	public ContextIOResponse getAllThreads(String accountId,
			Map<String, String> params) {
		return get(accountId, "threads", params);
	}

	public ContextIOResponse getAllContacts(String accountId,
			Map<String, String> params) {
		return get(accountId, "contacts", params);
	}

	// endregion

	// region > allAccounts (programmatic)

	public ContextIOResponse getAccounts() {
		return get(null, "accounts", null);
	}

	// endregion

	// region > addAccount (programmatic)

	public ContextIOResponse addAccount(Map<String, String> params) {
		return post(null, "accounts", params);
	}

	// endregion

	public ContextIOResponse discovery(Map<String, String> params) {
		return get(null, "discovery", params);
	}

	// region > helpers (get, post)

	public ContextIOResponse get(String accountId, String action,
			Map<String, String> params) {
		return doCall("GET", accountId, action, params);
	}

	public ContextIOResponse post(String accountId, String action,
			Map<String, String> params) {
		return doCall("POST", accountId, action, params);
	}

	public ContextIOResponse doCall(String method, String accountId,
			String action, Map<String, String> params) {
		String actionURL = action;
		if (accountId != null && !accountId.equals("")) {
			actionURL = "accounts/" + accountId + "/" + action;
		}

		String baseUrl = build_url(actionURL);
		OAuthService service = new ServiceBuilder()
				.provider(ContextIOApi.class).apiKey(this.key)
				.apiSecret(this.secret).build();

		baseUrl = URLUtils.appendParametersToQueryString(baseUrl, params);

		logger.info("ContextIO 2.0 Request URL : " + baseUrl);

		OAuthRequest request = null;
		if ("GET".equals(method)) {
			request = new OAuthRequest(Verb.GET, baseUrl);
		} else if ("POST".equals(method)) {
			request = new OAuthRequest(Verb.POST, baseUrl);
		}

		Token nullToken = new Token("", "");
		service.signRequest(nullToken, request);

		Response oauthResponse = request.send();

		lastResponse = new ContextIOResponse(oauthResponse.getCode(),
				request.getHeaders(), oauthResponse.getHeaders(), oauthResponse);
		logger.info("The request headers for contextio 2.0 : ");
		for (String k : request.getHeaders().keySet()) {
			logger.info("key : " + k + " value : "
					+ request.getHeaders().get(k));
		}

		logger.info("oauth response from contextio 2.0 : ");
		for (String key : oauthResponse.getHeaders().keySet()) {
			logger.info("oauth header key : " + key + "  value : "
					+ oauthResponse.getHeaders().get(key));
		}
		logger.info("oauth response msg: " + oauthResponse.getMessage());
		logger.info("oauth response code: " + oauthResponse.getCode());
		logger.info("oauth response body: " + oauthResponse.getBody());

		logger.info("The response from contextio 2.0 : "
				+ lastResponse.getRawResponse().getBody());
		if (lastResponse.isHasError()) {
			logger.info("LAST response errored : " + lastResponse.getCode());
			return null;
		} else {
			return lastResponse;
		}
	}

	public Map<String, String> filterParams(Map<String, String> givenParams,
			String[] validParams) {
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
	// endregion

}
