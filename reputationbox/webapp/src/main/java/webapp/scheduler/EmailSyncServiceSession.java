package webapp.scheduler;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.apache.isis.core.runtime.sessiontemplate.AbstractIsisSessionTemplate;
import org.apache.isis.core.runtime.system.transaction.TransactionalClosureAbstract;
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
		
		 this.getTransactionManager(this.getPersistenceSession()).executeWithinTransaction(new
				   TransactionalClosureAbstract() {
				               @Override
				               public void execute() {
				            	   syncEmails();
				               }
				           });
	}

	public void syncEmails() {
		logger.info("TEST!!!!   Syncing emails periodically...");
		emailService.syncMailBoxes();
	}
	
	
}
