/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.nic.isis.tfidf.CosineSimilarity;
import org.nic.isis.tfidf.TfIdf;


import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;
import edu.ucla.sspace.text.DocumentPreprocessor;
import edu.ucla.sspace.text.EnglishStemmer;


/**
 * @author dileepa
 *
 */
@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("USERMAILBOX")
public class UserMailBox {

	@PrimaryKey
	@Persistent
	private String emailId;
	@Persistent
	private String userName;
	@Persistent
	private int emailCount = 0;
	@Persistent
	private int termCount = 0;
	//emailId: email
	@Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	private Map<String,Email> allEmails = new HashMap<String,Email>();
	@Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	private List<EmailContact> allEmailContacts = new ArrayList<EmailContact>() ;
	@Persistent
	private int lastIndexTimestamp = 0;

	// This variable will hold all terms of each email in a list
	// emailIdIndex:termsList
	//private Map<String, List<String>> termsInEmails = new HashMap<String, List<String>>();
	
	//emailId : index
	@Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	private Map<String, Integer> emailIdMap = new HashMap<String, Integer>();
	// term: index, all terms in the documents
	@Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	private Map<String, Integer> allTerms = new HashMap<String, Integer>();
	// emailId: tfidfVector (the index of the array corresponds to the index of
	// allTerms to get the term)
	@Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	private Map<String, double[]> tfIdfEmailMap = new HashMap<String, double[]>();

	public UserMailBox() {
	
	}

	public UserMailBox(String userId, String userName) {
		this.emailId = userId;
		this.userName = userName;
			
	}

	public void addEmail(Email email){
		emailIdMap.put(email.getMessageId(),emailCount);
		this.allEmails.put(email.getMessageId(),email);
		emailCount++;
		
	}
	
	public List<String> parseEmail(Email email) {
		List<String> emailTermsList = new ArrayList<String>();
		emailTermsList.addAll(tokenizeContent(email.getSubject()));
		emailTermsList.addAll(tokenizeContent(email.getBody()));

		for (String term : emailTermsList) {
			if (!allTerms.containsKey(term)) { // avoid duplicate entry
				allTerms.put(term, termCount);
				termCount++;
			}
		}
		//termsInEmails.put(email.getMessageId(), emailTermsList);
		email.setTermList(emailTermsList);
		return emailTermsList;
	}

	
	/**
	 * tokenize the email content and performs various preprocessing
	 * @param content
	 * @return
	 */
	public static List<String> tokenizeContent(String content) {
		// to get individual terms
		DocumentPreprocessor docPreproc = new DocumentPreprocessor();
		content = docPreproc.process(content);
		String[] tokenizedTerms = content.replaceAll("[\\W&&[^\\s]]", "")
				.split("\\W+");
		//stem words to avoid same word being indexed with same meaning
		EnglishStemmer stemmer = new EnglishStemmer();
		for(int i=0; i < tokenizedTerms.length; i++){
			String stemmedToken = stemmer.stem(tokenizedTerms[i]);
			tokenizedTerms[i] = stemmedToken;
		}
		
		return Arrays.asList(tokenizedTerms);
	}

	/**
	 * Method to create termVector according to its tfidf score.
	 */
/*	public void calculateTfIdf() {
		double tf; // term frequency
		double idf; // inverse document frequency
		double tfidf; // term frequency inverse document frequency
		for (String emailId : termsInEmails.keySet()) {

			List<String> termsInAnEmail = termsInEmails.get(emailId);
			double[] tfidfvectors = new double[allTerms.size()];
			int count = 0;
			//row:term, column:email
			Matrix tfMatrix = new YaleSparseMatrix(allTerms.size(),termsInEmails.size());
			Matrix idfMatrix = new YaleSparseMatrix(allTerms.size(),termsInEmails.size());
			
			
			
			for (String term : allTerms.keySet()) {
				tf = TfIdf.tfCalculator(termsInAnEmail, term);
				idf = TfIdf.idfCalculator(termsInEmails.values(), term);
				tfidf = tf * idf;
				tfidfvectors[count] = tfidf;
				count++;
				
			}		
			tfIdfEmailMap.put(emailId, tfidfvectors); // storing document
													// vectors;
		}
	}*/

	/**
	 * Method to calculate cosine similarity between all the documents.
	 */
	/*public void getCosineSimilarity() {
		for (String emailId1 : tfIdfEmailMap.keySet()) {
			for (String emailId2 : tfIdfEmailMap.keySet()) {
				if (!emailId1.equalsIgnoreCase(emailId2)){
					System.out.println("cosine similarity between email : "
							+ emailId1
							+ " and "
							+ emailId2
							+ "  =  "
							+ CosineSimilarity.calculateCosineSimilarity(
									tfIdfEmailMap.get(emailId1),
									tfIdfEmailMap.get(emailId2)));

				}
				
			}
		}
	}*/

	
	public Map<String, Integer> getAllTerms() {
		return allTerms;
	}

	public Map<String, double[]> getTfIdfEmailMap() {
		return tfIdfEmailMap;
	}

	public Map<String,Email> getAllEmails() {
		return allEmails;
	}

	public void setAllEmails(Map<String,Email> allEmails) {
		this.allEmails = allEmails;
	}

	public List<EmailContact> getAllEmailContacts() {
		return allEmailContacts;
	}

	public void setAllEmailContacts(List<EmailContact> allEmailContacts) {
		this.allEmailContacts = allEmailContacts;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String userId) {
		this.emailId = userId;
	}

	public int getLastIndexTimestamp() {
		return lastIndexTimestamp;
	}

	public void setLastIndexTimestamp(int lastIndexTimestamp) {
		this.lastIndexTimestamp = lastIndexTimestamp;
	}
}
