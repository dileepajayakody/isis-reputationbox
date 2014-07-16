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
import org.datanucleus.store.rdbms.request.UpdateRequest;

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
			//allMailBoxes.add(create("reputationbox1@gmail.com"));
		}
		
		for (final UserMailBox mailBox : allMailBoxes) {
			if (!mailBox.isSyncing()){
				mailBox.setSyncing(true);
				while (mailBox.isSyncing()){
					contextIOService.updateMailBox(mailBox, 20);
				}
				container.persist(mailBox);
				logger.info("updated the mailBox: " + mailBox.getEmailId() + " with " + mailBox.getEmailCount() + " emails");
			}
/*
			if (!mailBox.isSyncing()){
				logger.info("Starting sync mail box thread for : " + mailBox.getEmailId());
				Runnable mailBoxRunnable = new Runnable(){
					@Override
					public void run() {
						while (mailBox.isSyncing()){
							contextIOService.updateMailBox(mailBox, 5);
							
						}
					}
				};
				
				Thread mailBoxSyncThread = new Thread(mailBoxRunnable);
				mailBoxSyncThread.start();*/
					
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

	public void connectMailBox(
            @Named("Email Id") String emailId,
			@Named("Password") String password,
			@Named("First Name") String fname, @Named("Last Name") String lname) {
		UserMailBox newMb = contextIOService.connectMailBox(emailId, password,
				fname, lname);

		if(null != newMb){
			UserMailBoxUpdateThread mbUpdateThread = new UserMailBoxUpdateThread(newMb, contextIOService);
			mbUpdateThread.start();
		}else {
			logger.info("couldn't connect the mailbox for : " + emailId);
		}

	}

    //region > dependencies
    @Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
    //endregion
}
