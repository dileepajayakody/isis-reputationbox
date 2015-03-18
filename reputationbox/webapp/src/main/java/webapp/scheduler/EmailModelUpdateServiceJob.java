package webapp.scheduler;

public class EmailModelUpdateServiceJob extends AbstractIsisQuartzJob {

	public EmailModelUpdateServiceJob() {
		super(new EmailDataModelSyncServiceSession());
	}

}
