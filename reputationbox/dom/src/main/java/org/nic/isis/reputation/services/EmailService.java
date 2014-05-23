package org.nic.isis.reputation.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.reputation.viewmodels.EmailViewModel;
import org.scribe.model.Response;

import at.tomtasche.contextio.ContextIO;
import at.tomtasche.contextio.ContextIOResponse;

public class EmailService {

	@Inject
	DomainObjectContainer container;
	
	ContextIO contextio = new ContextIO("65kd0b3k", "CetIiO0Ke0Klb2u8");
	
	
	 public List<EmailViewModel> allMessages() {
		 	List<EmailViewModel> emailList = new ArrayList<EmailViewModel>();
		 	Map<String, String> params = new HashMap<String, String>();
			params.put("since", "0");
	        params.put("limit", "20");		
			ContextIOResponse cio = contextio.allMessages("gdc2013demo@gmail.com", params);
			Response cioResponse = cio.getRawResponse();
			String responseString = cioResponse.getBody();
			JSONObject json = new JSONObject(responseString);
		    JSONArray data = json.getJSONArray("data");
		    
		    for(int i=0; i < data.length(); i++){
		    	JSONObject jsonObj = (JSONObject)data.get(i);
		    	//EmailViewModel evm = container.newViewModelInstance(EmailViewModel.class,jsonObj.toString());
		    	EmailViewModel evm = new EmailViewModel();
		    	evm.viewModelInit(jsonObj.toString());
		    	emailList.add(evm);
		    }
		    
		    return emailList;
	    }

	
}
