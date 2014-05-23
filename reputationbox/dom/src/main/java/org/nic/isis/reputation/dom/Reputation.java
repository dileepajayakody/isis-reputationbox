package org.nic.isis.reputation.dom;

import java.util.Date;

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
