package org.nic.isis.reputation.services;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmailSyncService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailSyncService.class);
	
	/**
	 * sync all mailboxes with new emails since last indexed timestamp
	 */
	public void sync(){
		List<UserMailBox> allMailBoxes = listAllMailBoxes();
		if (allMailBoxes == null || allMailBoxes.isEmpty()){
			logger.info("There is no mailboxes in datastore. creating a new one");
			allMailBoxes = new ArrayList<UserMailBox>();
			allMailBoxes.add(create("gdc2013demo@gmail.com"));
		}
		for(UserMailBox mailBox : allMailBoxes){
			mailBox = contextIOService.synMailBox(mailBox, 20);
			container.persist(mailBox);
			container.flush();
			
		}
	}
	
	@Programmatic
	public List<UserMailBox> listAllMailBoxes(){
		return container.allInstances(UserMailBox.class);
	}
	
	public UserMailBox create(final String userId) {
	        final UserMailBox mb = container.newTransientInstance(UserMailBox.class);
	        mb.setEmailId(userId);;
	        container.persistIfNotAlready(mb);
	        return mb;
	 }
	
	@Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
}
