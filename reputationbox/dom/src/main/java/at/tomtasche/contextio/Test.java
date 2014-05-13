package at.tomtasche.contextio;

import java.util.HashMap;
import java.util.Map;

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
		
		//System.out.println(dokdok.allMessages("tomtasche@gmail.com", params).rawResponse.getBody());
		ContextIO gdcDemo = new ContextIO("65kd0b3k", "CetIiO0Ke0Klb2u8");
		//ContextIO gdcDemo = new ContextIO("", "");
		System.out.println(gdcDemo.allMessages("gdc2013demo@gmail.com", params).rawResponse.getBody());
		//ContextIOResponse mailResponse = gdcDemo.allMessages("gdc2013demo@gmail.com", params);
		
	}
}
