package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.clustering.CentroidCluster;
import org.nic.isis.clustering.EmailCluster;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.reputation.services.EmailService;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dileepa
 *
 */
@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("EMAILREPUTATIONDATAMODEL")
public class EmailReputationDataModel {

	List<EmailContentCluster> contentClusters;
	List<EmailRecipientCluster> recipientClusters;
	
	
	//new variables for importance vectors
	//direct emails
	List<Email> flaggedEmails;
	List<Email> repliedEmails;
	List<Email> seenEmails;
	
	//list emails
	List<Email> flaggedListEmails;
	List<Email> repliedListEmails;
	List<Email> seenListEmails;
	
	//spam emails which have some how managed to get into inbox
	//identified by spam,precedence headers
	List<Email> spamEmails;
			
	private final static Logger logger = LoggerFactory
			.getLogger(EmailReputationDataModel.class);
	
	public EmailReputationDataModel(){
		contentClusters = new ArrayList<EmailContentCluster>();
		recipientClusters = new ArrayList<EmailRecipientCluster>();
		
		flaggedEmails = new ArrayList<Email>();
		repliedEmails = new ArrayList<Email>();
		seenEmails = new ArrayList<Email>();
		
		flaggedListEmails = new ArrayList<Email>();
		repliedListEmails = new ArrayList<Email>();
		seenListEmails = new ArrayList<Email>();
		
		spamEmails = new ArrayList<Email>();
		
	}
	
	public double getSumOfSquaredErrorForContentClusters(){
		double sumOfSquaresError = 0;
		for(EmailContentCluster cluster : this.getContentClusters()){
			sumOfSquaresError += cluster.getSumOfSquaresError();
		}
		return sumOfSquaresError;
	}
	
	public double getSumOfSquaredErrorForRecipientClusters(){
		double sumOfSquaresError = 0;
		for(EmailRecipientCluster cluster : this.getRecipientClusters()){
			sumOfSquaresError += cluster.getSumOfSquaresError();
		}
		return sumOfSquaresError;
	}
	
	
	/**
	 * for content clusters
	 * if min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are compact and well clustered
	 * @return dunnIndex to validate cluster quality
	 */
	public double getDunnIndexForContentClusters(){
		//min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are CWS
		double di = 0;
		double minInterClusterDistance = 0;
		double maxIntraClusterDistance = 0;
		
		for(EmailContentCluster c1: this.getContentClusters()){
			for(EmailContentCluster c2 : this.getContentClusters()){
				
				if(c1.getId() != c2.getId()){
					double[] v1 = c1.getCentroid();
					double[] v2 = c2.getCentroid();
					double dis = VectorsMath.getDistance(v1, v2);
//					logger.info("inter-cluster distance between : " + c1.getId() + " and " + c2.getId() 
//								+ " distance: "+ dis);
//					
					if(minInterClusterDistance == 0) {
						minInterClusterDistance = dis;
					} else {
						if(dis < minInterClusterDistance) {
							minInterClusterDistance = dis;
						}
					}
				}
			}
		}
		
		for(EmailContentCluster c: this.getContentClusters()){
			double sumOfIntraClusterDistance = 0;
			double intraClusterDistance = 0;
			
			for(Email mail : c.getContentEmails()){
				//distance from the centroid
				double[] v1 = mail.getTextContextVector();
				double dis = VectorsMath.getDistance(v1, c.getCentroid());
				sumOfIntraClusterDistance += dis;
			}
			
			intraClusterDistance = sumOfIntraClusterDistance / c.getContentEmails().size();
			//logger.info("intracluster distance : " + intraClusterDistance);
			if(maxIntraClusterDistance < intraClusterDistance){
				maxIntraClusterDistance = intraClusterDistance;
			}
		}
		
		logger.info("min inter-cluster distance : " + minInterClusterDistance + " max. intracluster distance : " + maxIntraClusterDistance );
		double dunnIndex = minInterClusterDistance/maxIntraClusterDistance;
		
		return dunnIndex;
	}

	/**
	 * if min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are compact and well clustered
	 * @return dunnIndex to validate cluster quality
	 */
	public double getDunnIndexForRecipientClusters(){
		//min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are CWS
		double di = 0;
		double minInterClusterDistance = 0;
		double maxIntraClusterDistance = 0;
		
		for(EmailRecipientCluster c1: this.getRecipientClusters()){
			for(EmailRecipientCluster c2 : this.getRecipientClusters()){
				
				if(c1.getId() != c2.getId()){
					double[] v1 = c1.getCentroid();
					double[] v2 = c2.getCentroid();
					
					double dis = VectorsMath.getDistance(v1, v2);
//					logger.info("inter-cluster distance between : " + c1.getId() + " and " + c2.getId() 
//							+ " distance: "+ dis);
				
					if(minInterClusterDistance == 0) {
						minInterClusterDistance = dis;
					} else {
						if(dis < minInterClusterDistance) {
							minInterClusterDistance = dis;
						}
					}
					
				}
			}
		}
		
		for(EmailRecipientCluster c: this.getRecipientClusters()){
			double sumOfIntraClusterDistance = 0;
			double intraClusterDistance = 0;
			
			for(Email mail : c.getRecipientEmails()){
				//distance from the centroid
				double[] v1 = mail.getRecipientContextVector();
				double dis = VectorsMath.getDistance(v1, c.getCentroid());
				sumOfIntraClusterDistance += dis;
			}
			
			intraClusterDistance = sumOfIntraClusterDistance / c.getRecipientEmails().size();
			//logger.info("intracluster distance : " + intraClusterDistance);
			if(maxIntraClusterDistance < intraClusterDistance){
				maxIntraClusterDistance = intraClusterDistance;
			}
		}
		logger.info("min inter-cluster distance : " + minInterClusterDistance + " max. intracluster distance : " + maxIntraClusterDistance );
		double dunnIndex = minInterClusterDistance/maxIntraClusterDistance;
		
		return dunnIndex;
	}
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailContentCluster> getContentClusters() {
		return contentClusters;
	}
	public void setContentClusters(List<EmailContentCluster> contentClusters) {
		this.contentClusters = contentClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getRecipientClusters() {
		return recipientClusters;
	}
	public void setRecipientClusters(List<EmailRecipientCluster> recipientClusters) {
		this.recipientClusters = recipientClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getFlaggedEmails() {
		return flaggedEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getRepliedEmails() {
		return repliedEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getSeenEmails() {
		return seenEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getSpamEmails() {
		return spamEmails;
	}

	public void setFlaggedEmails(List<Email> flaggedEmails) {
		this.flaggedEmails = flaggedEmails;
	}

	public void setRepliedEmails(List<Email> repliedEmails) {
		this.repliedEmails = repliedEmails;
	}

	public void setSeenEmails(List<Email> seenEmails) {
		this.seenEmails = seenEmails;
	}

	public void setSpamEmails(List<Email> spamEmails) {
		this.spamEmails = spamEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getFlaggedListEmails() {
		return flaggedListEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getRepliedListEmails() {
		return repliedListEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getSeenListEmails() {
		return seenListEmails;
	}

	public void setFlaggedListEmails(List<Email> flaggedListEmails) {
		this.flaggedListEmails = flaggedListEmails;
	}

	public void setRepliedListEmails(List<Email> repliedListEmails) {
		this.repliedListEmails = repliedListEmails;
	}

	public void setSeenListEmails(List<Email> seenListEmails) {
		this.seenListEmails = seenListEmails;
	}

}
