package webapp.scheduler;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.core.runtime.sessiontemplate.AbstractIsisSessionTemplate;
import org.apache.isis.core.runtime.system.transaction.TransactionalClosureAbstract;
import org.nic.isis.reputation.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Hidden
public class EmailDataModelSyncServiceSession extends AbstractIsisSessionTemplate {

	@Inject
	EmailService emailService;
	
	private final static Logger logger = LoggerFactory
			.getLogger(EmailDataModelSyncServiceSession.class);
	
	@Override
	protected void doExecute(Object arg0) {
		 this.getTransactionManager(this.getPersistenceSession()).executeWithinTransaction(new
				   TransactionalClosureAbstract() {
				               @Override
				               public void execute() {
				            	   updateDataModel();
				               }
				           });
	}

	public void updateDataModel() {
		logger.info("Updating the email data model periodically...");
		emailService.updateEmailModels();
	}
}
