package org.nic.isis.reputation.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.nic.isis.reputation.dom.UserMailBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.query.QueryDefault;
import org.datanucleus.store.rdbms.request.UpdateRequest;

import edu.ucla.sspace.vector.Vector;

public class EmailService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailService.class);

	/**
	 * sync all mailboxes with new emails since last indexed timestamp
	 */
	public void syncMailBoxes() {
		List<UserMailBox> allMailBoxes = listAllMailBoxes();
		if (allMailBoxes == null || allMailBoxes.isEmpty()) {
			logger.info("There is no mailboxes in test-datastore. creating test mailboxes for connected accounts in contextio");
			allMailBoxes = new ArrayList<UserMailBox>();
			allMailBoxes.add(create("gdc2013demo@gmail.com"));
		}
		
		for (UserMailBox mailBox : allMailBoxes) {
			//for testing email update and analysis in one transaction..
			try {
				mailBox = contextIOService.updateMailBox(mailBox, 5);
				mailBox.analyseEmails();
				Set<String> allWords = mailBox.getRandomIndex().getWords();
				logger.info("The context vectors of emails processed by Random Indexing: " + mailBox.getEmailId());
				for (String word : allWords) {
					Vector contextVector = mailBox.getRandomIndex().getVector(word);
					String vectorString = "";
					for (int i = 0; i < contextVector.length(); i++){
						Integer val = (Integer)contextVector.getValue(i);
						vectorString += "[" + i + " : " + val + "], ";  
					}
					logger.info(word + " : " + vectorString);
				}
			} catch (Exception e) {
				logger.error("Error occurred  " , e);
			}
			container.persist(mailBox);
			
			/*if (!mailBox.isSyncing()) {
				mailBox.setSyncing(true);
				while (mailBox.isSyncing()) {
					contextIOService.updateMailBox(mailBox, 20);
				}
				container.persist(mailBox);
				logger.info("updated the mailBox: " + mailBox.getEmailId()
						+ " with " + mailBox.getEmailCount() + " emails");
			}*/

		}
	}

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	@Programmatic
	public UserMailBox create(final String userId) {
		UserMailBox mb = container.newTransientInstance(UserMailBox.class);
		mb.setEmailId(userId);
		mb.setAccountId("53214991facaddd22d812863");
		container.persistIfNotAlready(mb);
		return mb;
	}

	public void connectMailBox(@Named("Email Id") String emailId,
			@Named("Password") String password,
			@Named("First Name") String fname, @Named("Last Name") String lname) {
		UserMailBox newMb = contextIOService.connectMailBox(emailId, password,
				fname, lname);

	}

	// region > dependencies
	@Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
	// endregion
}
