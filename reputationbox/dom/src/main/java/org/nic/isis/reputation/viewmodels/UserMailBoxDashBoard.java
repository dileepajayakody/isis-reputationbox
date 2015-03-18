package org.nic.isis.reputation.viewmodels;

import java.util.List;

import org.apache.isis.applib.AbstractViewModel;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Render;
import org.apache.isis.applib.annotation.Render.Type;
import org.nic.isis.reputation.services.EmailService;
import org.nic.isis.reputation.dom.*;

public class UserMailBoxDashBoard extends AbstractViewModel{

	//region > identification in the UI
    public String title() {
        return "Connected User Mailboxes";
    }
    
    private String momento;
    
	@Override
	public void viewModelInit(String momento) {
		this.momento = momento;
	}

	@Override
	public String viewModelMemento() {
		return momento;
	}

	@Named("List Mail Boxes")
    @Render(Type.EAGERLY)
	public List<UserMailBox> listAllMailBoxes(){
		return emailService.listAllMailBoxes();
	}
	@javax.inject.Inject
	DomainObjectContainer container; 
	@javax.inject.Inject
	EmailService emailService;
	
	
}
