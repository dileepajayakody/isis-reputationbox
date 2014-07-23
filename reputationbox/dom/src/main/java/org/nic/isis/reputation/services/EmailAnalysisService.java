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
import org.nic.isis.reputation.dom.UserMailBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.ri.RandomIndexing;
import edu.ucla.sspace.vector.Vector;

public class EmailAnalysisService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailAnalysisService.class);

	
	public void analyseMailBoxes() throws IOException {
		List<UserMailBox> allMailBoxes = listAllMailBoxes();
		synchronized (this) {
			for (final UserMailBox mailBox : allMailBoxes) {
				mailBox.analyseEmails();
				Set<String> allWords = mailBox.getRandomIndex().getWords();
				logger.info("Printing the context vectors of emails in mailbox: " + mailBox.getEmailId());
				for (String word : allWords) {
					Vector contextVector = mailBox.getRandomIndex().getVector(word);
					String vectorString = "";
					for (int i = 0; i < contextVector.length(); i++){
						Integer val = (Integer)contextVector.getValue(i);
						vectorString += "[" + i + " : " + val + "], ";  
					}
					logger.info(word + " : " + vectorString);
				}

				container.persist(mailBox);
			}

		}

	}

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	// region > dependencies
	@Inject
	DomainObjectContainer container;
}
