package org.nic.isis.reputation.dom;

import java.util.Date;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("REPUTATION")
public class Reputation implements Comparable<Reputation>{

	private double reputationScore;
	private Date reputationDate;
	private ReputationCriteria criteria;
	
	@Override
	public int compareTo(Reputation o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getReputationScore() {
		return reputationScore;
	}

	public void setReputationScore(double reputationScore) {
		this.reputationScore = reputationScore;
	}
	
	public void calcReputation(ReputationCriteria criteria){
		
	}

}
