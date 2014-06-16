package org.nic.isis.reputation.services;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;

import dom.simple.SimpleObject;

public class EmailSyncService {

	
	public void sync(){
		List<UserMailBox> allMailBoxes = listAllMailBoxes();
		if (allMailBoxes == null || allMailBoxes.isEmpty()){
			System.out.println("since there is no mailboxes creating a new one");
			allMailBoxes = new ArrayList<UserMailBox>();
			allMailBoxes.add(create("gdc2013demo@gmail.com"));
		}
		for(UserMailBox mailBox : allMailBoxes){
			mailBox = contextIOService.synMailBox(mailBox, 20);
			container.persist(mailBox);
			
		}
	}
	
	@Programmatic
	public List<UserMailBox> listAllMailBoxes(){
		return container.allInstances(UserMailBox.class);
	}
	
	@Programmatic
	public UserMailBox create(
	            final String userId) {
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
