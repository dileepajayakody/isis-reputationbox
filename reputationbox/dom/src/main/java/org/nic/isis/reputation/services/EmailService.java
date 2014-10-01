package org.nic.isis.reputation.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.ri.RandomIndexing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;

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
			allMailBoxes.add(createSample());
		}

		for (UserMailBox mailBox : allMailBoxes) {
			try {
				if (!mailBox.isSyncing()) {
					mailBox.setSyncing(true);
					//iterate all emails from the beginning (upto 50 for testing..)
					while (mailBox.isSyncing() && mailBox.getEmailCount() < 50) {
						mailBox = contextIOService.updateMailBox(mailBox, 10);
					}
					logger.info("updated the mailBox: " + mailBox.getEmailId()
							+ " with " + mailBox.getEmailCount() + " emails");
				}
						
			} catch (Exception e) {
				logger.error("Error occurred  ", e);
			}
			
			//need to run the analysis as a separate process in a periodical approach
			//Should we cluster all emails from beginning or just within a particular time-window
			logger.info("Emails clustered according to text cooccurence");
			emailAnalysisService.kMeansClusterText(mailBox);
			logger.info("Emails clustered according to recipient cooccurence");
			emailAnalysisService.kMeansClusterRecipients(mailBox);
			
			container.persistIfNotAlready(mailBox);
		}
	}

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	@Programmatic
	//sample mailbox
	public UserMailBox createSample() {
		UserMailBox mb = container.newTransientInstance(UserMailBox.class);
		mb.setEmailId("dileepajayakody@gmail.com");
		//mb.setAccountId("530f0d8eb4810fd65d6d2149");
		mb.setAccountId("542a4e2e8c157f9741090c95");
		container.persistIfNotAlready(mb);
		return mb;
	}

	@Programmatic
	public UserMailBox connectMailBox(@Named("Email Id") String emailId,
			@Named("Password") String password,
			@Named("First Name") String fname, @Named("Last Name") String lname) {
		UserMailBox newMb = contextIOService.connectMailBox(emailId, password,
				fname, lname);
		return newMb;

	}

	// region > dependencies
	@Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
	@Inject
	EmailAnalysisService emailAnalysisService;
	// endregion
}
