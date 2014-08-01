package org.nic.isis.reputation.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.similarity.CosineSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmailAnalysisService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailAnalysisService.class);

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	/**
	 * processes the semantic vectors for the content of the email using random indexing
	 * @param email
	 * @param randomIndex
	 * @throws IOException
	 */
	public RandomIndexing processTextSemantics(Email email, RandomIndexing randomIndex) throws IOException {
		//processing subject and body seperately as 2 documents
		String processedSubjectTokenStream = email.getSubjectContent().getTokenStream();
		randomIndex.processDocument(new BufferedReader(new StringReader(processedSubjectTokenStream)));
		
		String processedBodyTokenStream = email.getBodyContent().getTokenStream();
		randomIndex.processDocument(new BufferedReader(new StringReader(
				processedBodyTokenStream)));
		
		int[] docSemanticVector = new int[randomIndex.getVectorLength()];
		List<String> allWords = new ArrayList<String>();
		allWords.addAll(email.getBodyContent().getStringTokens().keySet());
		allWords.addAll(email.getSubjectContent().getStringTokens().keySet());
		
		for (String word : allWords) {
			int[] semanticVector = randomIndex.getContextVector(word);
			int frequency = email.getBodyContent().getStringTokens().get(word);
				//add the semantic vector of the word * the no. of times its mentioned in the doc
				docSemanticVector = RandomIndexing.addVectors(
						docSemanticVector, semanticVector, frequency);

		}
		email.setDocumentContextVector(docSemanticVector);
		return randomIndex;
	}
	
	public void calculateCosineSimilarityofEmails(UserMailBox mb){
		List<Email> emails = mb.getAllEmails();
		for(Email email : emails){
			for (Email innerEmail : emails){
				if (!email.getMessageId().equalsIgnoreCase(innerEmail.getMessageId())){
					int[] v1 = email.getDocumentContextVector();
					int[] v2 = innerEmail.getDocumentContextVector();
					double similarity = CosineSimilarity.calculateCosineSimilarity(v1, v2);
					logger.info(" Similarity of EMAIL_1 : " + email.getSubject() + " and \n EMAIL_2 : " + innerEmail.getSubject() + " =  " + similarity);
				}
			}
		}

	}
	// region > dependencies
	@Inject
	DomainObjectContainer container;
}
