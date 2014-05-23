package at.tomtasche.contextio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.reputation.services.EmailService;
import org.nic.isis.reputation.viewmodels.EmailViewModel;

/**
 * 
 * @author Thomas Taschauer | tomtasche.at
 *
 */
public class Test {

	public static void main(String[] args) {
		//ContextIO dokdok = new ContextIO("YOURKEY", "YOURSECRET");
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("since", "0");
		
		/*ContextIO gdcDemo = new ContextIO("65kd0b3k", "CetIiO0Ke0Klb2u8");
		String cioResponseStr = gdcDemo.allMessages("gdc2013demo@gmail.com", params).getRawResponse().getBody(); */

		
		EmailService emailService = new EmailService();
		List<EmailViewModel> emailVms = emailService.allMessages();
		System.out.println("number of mails received : " + emailVms.size());
		for(EmailViewModel vm : emailVms){
			System.out.println("messagId : " + vm.getMessageId() + " subject : " + vm.getSubject());
		}
	}
}
