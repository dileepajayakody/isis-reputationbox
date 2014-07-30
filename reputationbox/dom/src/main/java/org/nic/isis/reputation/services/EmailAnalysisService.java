package org.nic.isis.reputation.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.ri.RandomIndexing;
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
		String processedTokenStream = email.getBodyContent().getTokenStream();
		randomIndex.processDocument(new BufferedReader(new StringReader(
				processedTokenStream)));
		int[] docSemanticVector = new int[randomIndex.getVectorLength()];
		for (String word : email.getBodyContent().getStringTokens().keySet()) {
			int[] semanticVector = randomIndex.getContextVector(word);
			int frequency = email.getBodyContent().getStringTokens().get(word);
				//add the semantic vector of the word * the no. of times its mentioned in the doc
				docSemanticVector = RandomIndexing.addVectors(
						docSemanticVector, semanticVector, frequency);

		}
		email.setDocumentContextVector(docSemanticVector);
		return randomIndex;
	}
	
	// region > dependencies
	@Inject
	DomainObjectContainer container;
}
