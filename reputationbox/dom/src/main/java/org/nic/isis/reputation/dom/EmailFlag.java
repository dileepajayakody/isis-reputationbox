package org.nic.isis.reputation.dom;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;

/*@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("EMAILFLAG")*/
public class EmailFlag {

	private boolean seen = false;

	public boolean isSeen() {
		return seen;
	}

	public void setSeen(boolean isSeen) {
		this.seen = isSeen;
	}
	
	private boolean answered = false;
	
	public boolean isAnswered() {
		return answered;
	}

	public void setAnswered(boolean answered) {
		this.answered = answered;
	}

	//if starred
	private boolean flagged = false;

	public boolean isFlagged() {
		return flagged;
	}

	public void setFlagged(boolean flagged) {
		this.flagged = flagged;
	}
	
	private boolean  deleted;
	
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	private boolean draft;
	
	public boolean isDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	
	private boolean nonJunk;
	
	public boolean isNonJunk() {
		return nonJunk;
	}

	public void setNonJunk(boolean nonJunk) {
		this.nonJunk = nonJunk;
	}
	

}
