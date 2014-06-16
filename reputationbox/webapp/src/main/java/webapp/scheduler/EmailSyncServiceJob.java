package webapp.scheduler;


public class EmailSyncServiceJob extends AbstractIsisQuartzJob {

	public EmailSyncServiceJob() {
		super(new EmailSyncServiceSession());
		
	}

}
