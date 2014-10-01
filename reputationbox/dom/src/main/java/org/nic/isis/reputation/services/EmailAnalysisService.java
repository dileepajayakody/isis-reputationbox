package org.nic.isis.reputation.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailFlag;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.similarity.Cluster;
import org.nic.isis.similarity.CosineSimilarity;
import org.nic.isis.similarity.KMeansCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.vector.TernaryVector;

public class EmailAnalysisService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailAnalysisService.class);

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	/**
	 * processes the semantic vectors for the content of the email using random
	 * indexing
	 * 
	 * @param email
	 * @param textIndex
	 * @throws IOException
	 */
	public RandomIndexing processTextSemantics(Email email,
			RandomIndexing textIndex) throws IOException {
		// processing subject and body seperately as 2 documents
		String processedSubjectTokenStream = email.getSubjectContent()
				.getTokenStream();
		textIndex.processDocument(new BufferedReader(new StringReader(
				processedSubjectTokenStream)));

		String processedBodyTokenStream = email.getBodyContent()
				.getTokenStream();
		textIndex.processDocument(new BufferedReader(new StringReader(
				processedBodyTokenStream)));

		double[] textContextVector = new double[textIndex.getVectorLength()];
		List<String> allWords = new ArrayList<String>();
		allWords.addAll(email.getBodyContent().getStringTokens().keySet());
		allWords.addAll(email.getSubjectContent().getStringTokens().keySet());
		// calculating the sum of all words semantic vectors in the document
		for (String word : allWords) {
			double[] wordSemanticVector = textIndex.getContextVector(word);

			if (null != email.getBodyContent().getStringTokens().get(word)) {
				int frequency = email.getBodyContent().getStringTokens()
						.get(word);
				// add the semantic vector of the word * the no. of times its
				// mentioned in the doc
				textContextVector = RandomIndexing.addArrays(textContextVector,
						wordSemanticVector, frequency);
			}
			if (null != email.getSubjectContent().getStringTokens().get(word)) {
				int frequency = email.getSubjectContent().getStringTokens()
						.get(word);
				// add the semantic vector of the word * the no. of times its
				// mentioned in the doc
				textContextVector = RandomIndexing.addArrays(textContextVector,
						wordSemanticVector, frequency);
			}
		}
		email.setTextContextVector(textContextVector);
		return textIndex;
	}

	/**
	 * processes the semantic vectors for the associated email addresses using
	 * random indexing
	 * 
	 * @param email
	 * @param recipientIndex
	 * @throws IOException
	 */
	public RandomIndexing processRecipientSemantics(Email email,
			RandomIndexing recipientIndex) throws IOException {
		List<String> toAddresses = email.getToAddresses();
		List<String> ccAddresses = email.getCcAddresses();
		String toAddressStr = "";
		for (String toAddress : toAddresses) {
			toAddressStr += " " + toAddress;
		}

		String ccAddressStr = "";
		for (String ccAddress : ccAddresses) {
			ccAddressStr += " " + ccAddress;
		}
		recipientIndex.processDocument(new BufferedReader(new StringReader(
				toAddressStr)));
		recipientIndex.processDocument(new BufferedReader(new StringReader(
				ccAddressStr)));

		double[] recipientContextVector = new double[recipientIndex
				.getVectorLength()];
		for (String toAddress : toAddresses) {
			// double[] toAddressContextVector =
			// recipientIndex.getContextVector(toAddress);
			TernaryVector toAddressIndexVector = recipientIndex
					.getWordToIndexVector().get(toAddress);
			if (toAddressIndexVector != null) {

				/*
				 * logger.info("vector for toAddress : " + toAddress); String
				 * vectorString = "["; for (int i = 0; i <
				 * toAddressContextVector.length; i++) { double val =
				 * toAddressContextVector[i]; vectorString += val + ", "; }
				 * vectorString += "]"; logger.info(toAddress
				 * +" context vector : " + vectorString);
				 */

				recipientContextVector = RandomIndexing.add(
						recipientContextVector, toAddressIndexVector);
			}
		}
		for (String ccAddress : ccAddresses) {
			double[] ccAddressContextVector = recipientIndex
					.getContextVector(ccAddress);
			TernaryVector ccAddressIndexVector = recipientIndex
					.getWordToIndexVector().get(ccAddress);
			if (ccAddressIndexVector != null) {
				recipientContextVector = RandomIndexing.add(
						recipientContextVector, ccAddressIndexVector);
			}
		}
		email.setRecipientContextVector(recipientContextVector);
		return recipientIndex;
	}

	public void calculateCosineSimilarityofEmails(UserMailBox mb) {
		List<Email> emails = mb.getAllEmails();
		for (Email email : emails) {
			for (Email innerEmail : emails) {
				if (!email.getMessageId().equalsIgnoreCase(
						innerEmail.getMessageId())) {
					double[] v1 = email.getTextContextVector();
					double[] v2 = innerEmail.getTextContextVector();
					double similarity = CosineSimilarity
							.calculateCosineSimilarity(v1, v2);
					logger.info(" Similarity of EMAIL_1 : "
							+ email.getSubject() + " and \n EMAIL_2 : "
							+ innerEmail.getSubject() + " =  " + similarity);
				}
			}
		}

	}

	public void kMeansClusterText(UserMailBox mb) {
		KMeansCluster kmeans = new KMeansCluster();
		List<Email> allEmails = mb.getAllEmails();
		List<Cluster> clusters = kmeans.cluster(allEmails,
				KMeansCluster.TEXT_CLUSTER_TYPE);

		for (Cluster cluster : clusters) {
			logger.info(" ");
			logger.info("Cluster ID : " + cluster.getId()
					+ "===========================================");
			int clusterSize = cluster.size();
			for (int i = 0; i < clusterSize; i++) {
				Email email = cluster.getEmail(i);
				logger.info("Email subject : " + email.getSubject());
			}
		}

	}

	public void kMeansClusterRecipients(UserMailBox mb) {
		KMeansCluster kmeans = new KMeansCluster();
		List<Email> allEmails = mb.getAllEmails();
		List<Cluster> clusters = kmeans.cluster(allEmails,
				KMeansCluster.RECIPIENT_CLUSTER_TYPE);
		for (Cluster cluster : clusters) {
			logger.info(" ");
			logger.info("Cluster ID : " + cluster.getId()
					+ "===========================================");
			int clusterSize = cluster.size();
			for (int i = 0; i < clusterSize; i++) {
				Email email = cluster.getEmail(i);
				List<String> toAddresses = email.getToAddresses();
				List<String> ccAddresses = email.getCcAddresses();
				logger.info("Email subject : " + email.getSubject()
						+ " | To Addresses : " + toAddresses.toString()
						+ " | cc Addresses : " + ccAddresses.toString());

			}
		}

	}

	/**
	 * Analyse level of response and importance of the emails in the cluster
	 */
	public void analyseCluster(Cluster cluster) {
		int clusterSize = cluster.size();
		
		for (int i = 0; i < clusterSize; i++) {
			Email email = cluster.getEmail(i);
			EmailFlag flags = email.getEmailFlags();
			if (flags.isAnswered()) {
				cluster.addMessageAnswered();
				//calculate the response time for the email
				String receivedMessageId = email.getMessageId();
				
			}
			if (flags.isFlagged()) {
				cluster.addMessageFlagged();
			}
			if (flags.isSeen()){
				cluster.addMessageSeen();
			}
			if(flags.isDeleted()){
				cluster.addMessageDeleted();
			}

		}
	}

	// region > dependencies
	@Inject
	DomainObjectContainer container;
}
