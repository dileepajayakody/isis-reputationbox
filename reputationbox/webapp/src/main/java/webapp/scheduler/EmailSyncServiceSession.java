package webapp.scheduler;

import javax.inject.Inject;

import org.apache.isis.core.runtime.sessiontemplate.AbstractIsisSessionTemplate;
import org.nic.isis.reputation.services.EmailService;
import org.nic.isis.reputation.services.EmailSyncService;

public class EmailSyncServiceSession extends AbstractIsisSessionTemplate {

	@Inject
	EmailSyncService emailSyncService;

	@Override
	protected void doExecute(Object context) {
		syncEmails();
	}

	public void syncEmails() {
		System.out.println("TEST!!!!   syncing emails periodically...");
		//emailSyncService.sync();
	}
}
