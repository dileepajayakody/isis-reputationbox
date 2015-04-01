package webapp.scheduler;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.core.runtime.sessiontemplate.AbstractIsisSessionTemplate;
import org.apache.isis.core.runtime.system.transaction.TransactionalClosureAbstract;
import org.nic.isis.reputation.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Hidden
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
		logger.info("Retrieving emails periodically...");
		//emailService.syncMailBoxes(100);
		emailService.updateNew();
	}
	
	
}
