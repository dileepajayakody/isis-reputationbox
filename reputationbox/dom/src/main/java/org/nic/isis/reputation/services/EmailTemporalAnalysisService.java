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
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.vector.TernaryVector;

public class EmailTemporalAnalysisService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailTemporalAnalysisService.class);

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
		/*
		 * only subject tokens
		 * 
		 * String processedBodyTokenStream = email.getBodyContent()
		 * .getTokenStream(); textIndex.processDocument(new BufferedReader(new
		 * StringReader( processedBodyTokenStream)));
		 */
		double[] textContextVector = new double[textIndex.getVectorLength()];
		List<String> allWords = new ArrayList<String>();
		allWords.addAll(email.getBodyContent().getStringTokens().keySet());
		allWords.addAll(email.getSubjectContent().getStringTokens().keySet());
		// calculating the sum of all words semantic vectors in the document
		for (String word : allWords) {
			double[] wordSemanticVector = textIndex.getVector(word);

			if (null != email.getSubjectContent().getStringTokens().get(word)) {
				int frequency = email.getSubjectContent().getStringTokens()
						.get(word);
				int totalNumberOfTokens = email.getSubjectContent()
						.getStringTokens().size();

				// add the semantic vector of the word * the no. of
				// times(normalized) its
				// mentioned in the doc
				textContextVector = VectorsMath.addArrays(textContextVector,
						wordSemanticVector, EmailUtils.getNormalizedFrequency(
								frequency, totalNumberOfTokens));
			}

			/*
			 * only subject tokens if (null !=
			 * email.getBodyContent().getStringTokens().get(word)) { int
			 * frequency = email.getBodyContent().getStringTokens() .get(word);
			 * int totalNumberOfTokens =
			 * email.getBodyContent().getStringTokens().size(); // add the
			 * semantic vector of the word * the no. of times its // mentioned
			 * in the doc textContextVector =
			 * RandomIndexing.addArrays(textContextVector, wordSemanticVector,
			 * EmailUtils.getNormalizedFrequency(frequency,
			 * totalNumberOfTokens)); }
			 */

		}
		email.setTextContextVector(textContextVector);
		return textIndex;
	}

	/**
	 * processes the semantic vectors for the associated email addresses using
	 * random indexing
	 * 
	 * @param email
	 * @param peopleIndex
	 * @throws IOException
	 */
	public RandomIndexing processPeopleSemantics(Email email,
			RandomIndexing peopleIndex) throws IOException {
		List<String> toAddresses = email.getToAddresses();
		List<String> ccAddresses = email.getCcAddresses();
		String fromAddress = email.getFromAddress();

		String toAddressStr = "";
		for (String toAddress : toAddresses) {
			toAddressStr += " " + toAddress;
		}

		String ccAddressStr = "";
		for (String ccAddress : ccAddresses) {
			ccAddressStr += " " + ccAddress;
		}
		peopleIndex.processDocument(new BufferedReader(new StringReader(
				toAddressStr)));
		peopleIndex.processDocument(new BufferedReader(new StringReader(
				ccAddressStr)));
		peopleIndex.processDocument(new BufferedReader(new StringReader(
				fromAddress)));

		double[] peopleContextVector = new double[peopleIndex.getVectorLength()];
		// adding toAddresses
		for (String toAddress : toAddresses) {
			// double[] toAddressContextVector =
			// recipientIndex.getContextVector(toAddress);
			TernaryVector toAddressIndexVector = peopleIndex
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

				peopleContextVector = RandomIndexing.add(peopleContextVector,
						toAddressIndexVector);
			}
		}
		// adding ccAddresses
		for (String ccAddress : ccAddresses) {
			double[] ccAddressContextVector = peopleIndex
					.getVector(ccAddress);
			TernaryVector ccAddressIndexVector = peopleIndex
					.getWordToIndexVector().get(ccAddress);
			if (ccAddressIndexVector != null) {
				peopleContextVector = RandomIndexing.add(peopleContextVector,
						ccAddressIndexVector);
			}
		}
		// adding from address
		double[] fromAddressContextVector = peopleIndex
				.getVector(fromAddress);
		TernaryVector fromAddressIndexVector = peopleIndex
				.getWordToIndexVector().get(fromAddress);
		if (fromAddressIndexVector != null) {
			peopleContextVector = RandomIndexing.add(peopleContextVector,
					fromAddressIndexVector);
		}

		email.setRecipientContextVector(peopleContextVector);
		return peopleIndex;
	}

	// region > dependencies
	@Inject
	DomainObjectContainer container;
}
