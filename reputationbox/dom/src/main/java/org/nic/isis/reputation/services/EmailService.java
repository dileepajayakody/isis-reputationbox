package org.nic.isis.reputation.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.features.PeopleFeature;
import org.nic.isis.reputation.viewmodels.EmailViewModel;
import org.scribe.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.lsa.LatentSemanticAnalysis;
import edu.ucla.sspace.matrix.NoTransform;
import edu.ucla.sspace.matrix.factorization.SingularValueDecompositionLibC;
import edu.ucla.sspace.text.StringDocument;
import edu.ucla.sspace.vector.DoubleVector;
import at.tomtasche.contextio.ContextIO;
import at.tomtasche.contextio.ContextIOResponse;

public class EmailService {

	@Inject
	DomainObjectContainer container;

	ContextIO contextio = new ContextIO("65kd0b3k", "CetIiO0Ke0Klb2u8");
	String emailAccount = "gdc2013demo@gmail.com";
	UserMailBox mailBox = new UserMailBox(emailAccount,"TestMailBox");
	
	private final static Logger logger = LoggerFactory
			.getLogger(EmailService.class);

	/*public void calculateEmailTfIdfVectors() {
		mailBox.parseAllEmails(mailBox.getAllEmails().values());
		mailBox.calculateTfIdf();
		Set<String> allTerms = mailBox.getAllTerms().keySet();
		
		
		String termStr = "[";
		for (int i=0; i < allTerms.size(); i++ ){
			String term = allTerms.get(i);
			termStr += i+ " : " + term + ", ";
		}
		termStr+="]";
		logger.info("All terms in emails ..\n" + termStr);
		System.out.println("All terms in emails ..\n" + termStr);
		
		for(String emailId : mailBox.getTfIdfEmailMap().keySet()){
			double[] tfidfVector = mailBox.getTfIdfEmailMap().get(emailId);
			logger.info(emailId + " :" + Arrays.toString(tfidfVector));
		}

	}*/
	
	/*public void calculateLSA(){
		 try {
			LatentSemanticAnalysis lsa =
			            new LatentSemanticAnalysis(true, 2, new NoTransform(), 
			                                       new SingularValueDecompositionLibC(),
			                                       false, new StringBasisMapping());
			for (Email m : mailBox.getAllEmails().values()){
				lsa.processDocument(new BufferedReader(new StringReader(m.getBody())));
			}
			lsa.processSpace(System.getProperties());
			String query = "wso2 carbon";
	        DoubleVector projected = lsa.project(new StringDocument(query));
	       System.out.println(projected.toArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/


}
