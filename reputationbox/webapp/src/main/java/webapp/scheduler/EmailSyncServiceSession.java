package webapp.scheduler;

import javax.inject.Inject;

import org.apache.isis.core.runtime.sessiontemplate.AbstractIsisSessionTemplate;
import org.nic.isis.reputation.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSyncServiceSession extends AbstractIsisSessionTemplate {

	@Inject
	EmailService emailService;
	
	private final static Logger logger = LoggerFactory
			.getLogger(EmailSyncServiceSession.class);

	@Override
	protected void doExecute(Object context) {
		syncEmails();
	}

	public void syncEmails() {
		logger.info("TEST!!!!   Syncing emails periodically...");
		//emailService.sync();
	}
}
