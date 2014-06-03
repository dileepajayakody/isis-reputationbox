/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nic.isis.reputation.dom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nic.isis.tfidf.CosineSimilarity;
import org.nic.isis.tfidf.TfIdf;

public class UserMailBox {

	private String userId;
	private String userName;
	//emailId: email
	private Map<String,Email> allEmails;
	private List<EmailContact> allEmailContacts;

	// This variable will hold all terms of each email in a list
	// emailId:termsList
	private Map<String, List<String>> termsInEmails = new HashMap<String, List<String>>();
	// all terms in the documents
	private List<String> allTerms = new ArrayList<String>();
	// emailId: tfidfVector (the index of the array corresponds to the index of
	// allTerms to get the term)
	private Map<String, double[]> tfIdfEmailMap = new HashMap<String, double[]>();

	public UserMailBox() {
		allEmails = new HashMap<String,Email>();
		setAllEmailContacts(new ArrayList<EmailContact>());
	}

	public UserMailBox(String userId, String userName) {
		this();
		this.userId = userId;
		this.userName = userName;
	}

	public void addEmail(Email email){
		this.allEmails.put(email.getMessageId(),email);
	}
	/**
	 * Method to parse emails
	 * 
	 * @param filePath
	 *            : source file path
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void parseAllEmails(Collection<Email> emails) {
		for (Email email : emails) {
			parseEmail(email);
		}
	}

	public List<String> parseEmail(Email email) {
		List<String> emailTermsList = new ArrayList<String>();
		emailTermsList.addAll(tokenizeContent(email.getSubject()));
		emailTermsList.addAll(tokenizeContent(email.getBody()));

		for (String term : emailTermsList) {
			if (!allTerms.contains(term)) { // avoid duplicate entry
				allTerms.add(term);
			}
		}
		termsInEmails.put(email.getMessageId(), emailTermsList);
		return emailTermsList;
	}

	public static List<String> tokenizeContent(String content) {
		// to get individual terms
		String[] tokenizedTerms = content.replaceAll("[\\W&&[^\\s]]", "")
				.split("\\W+");
		return Arrays.asList(tokenizedTerms);
	}

	/**
	 * Method to create termVector according to its tfidf score.
	 */
	public void calculateTfIdf() {
		double tf; // term frequency
		double idf; // inverse document frequency
		double tfidf; // term frequency inverse document frequency
		for (String emailId : termsInEmails.keySet()) {

			List<String> allEmailTermsList = termsInEmails.get(emailId);
			double[] tfidfvectors = new double[allTerms.size()];
			int count = 0;
			
			for (String term : allTerms) {
				tf = TfIdf.tfCalculator(allEmailTermsList, term);
				idf = TfIdf.idfCalculator(termsInEmails.values(), term);
				tfidf = tf * idf;
				tfidfvectors[count] = tfidf;
				count++;
			}
			tfIdfEmailMap.put(emailId, tfidfvectors); // storing document
														// vectors;
		}
	}

	/**
	 * Method to calculate cosine similarity between all the documents.
	 */
	public void getCosineSimilarity() {
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
	}

	public List<String> getAllTerms() {
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

}
