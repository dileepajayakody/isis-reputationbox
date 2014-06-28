package org.nic.isis.reputation.services;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.nic.isis.reputation.dom.UserMailBox;
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
	public synchronized void syncMailBoxes() {
		List<UserMailBox> allMailBoxes = listAllMailBoxes();
		if (allMailBoxes == null || allMailBoxes.isEmpty()) {
			logger.info("There is no mailboxes in datastore. creating a new one");
			allMailBoxes = new ArrayList<UserMailBox>();
			allMailBoxes.add(create("gdc2013demo@gmail.com"));
		}
		for (UserMailBox mailBox : allMailBoxes) {
			mailBox = contextIOService.updateMailBox(mailBox, 20);
			container.persistIfNotAlready(mailBox);
			container.flush();

		}
	}

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	public UserMailBox create(final String userId) {
		final UserMailBox mb = container.newTransientInstance(UserMailBox.class);
		mb.setEmailId(userId);
		container.persistIfNotAlready(mb);
		return mb;
	}

	public void connectMailBox(
            @Named("Email Id") String emailId,
			@Named("Password") String password,
			@Named("First Name") String fname, @Named("Last Name") String lname) {
		UserMailBox newMb = contextIOService.connectMailBox(emailId, password,
				fname, lname);
		container.persistIfNotAlready(newMb);
	}

    //region > dependencies
    @Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
    //endregion
}
