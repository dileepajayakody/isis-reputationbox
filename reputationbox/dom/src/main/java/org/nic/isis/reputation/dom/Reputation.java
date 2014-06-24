package org.nic.isis.reputation.dom;

import java.util.Date;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.util.ObjectContracts;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("REPUTATION")
public class Reputation implements Comparable<Reputation>{

	@Persistent
	private double reputationScore;
	
	public double getReputationScore() {
		return reputationScore;
	}

	public void setReputationScore(double reputationScore) {
		this.reputationScore = reputationScore;
	}
	
	@Persistent
	private Date reputationDate;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public Date getReputationDate() {
		return reputationDate;
	}

	public void setReputationDate(Date reputationDate) {
		this.reputationDate = reputationDate;
	}

	@Persistent
	private ReputationCriteria criteria;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public ReputationCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(ReputationCriteria criteria) {
		this.criteria = criteria;
	}

	@Override
	public int compareTo(Reputation other) {
		 return ObjectContracts.compare(this, other, "reputationScore");
	}

	public void calcReputation(ReputationCriteria criteria){
		
	}

}
