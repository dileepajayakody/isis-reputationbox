package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.clustering.CentroidCluster;
import org.nic.isis.clustering.EmailCluster;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.clustering.EmailWeightedSubjectBodyContentCluster;
import org.nic.isis.reputation.services.EmailService;
import org.nic.isis.ri.RandomIndexing;
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
	List<EmailWeightedSubjectBodyContentCluster> weightedSubjectBodyClusters;
	
	
	private double[] spamVector;
	private double[] spamPeopleVector;
	private double[] spamNLPKeywordVector;

	//important content profiles for the mailbox; for direct emails
	private double[] importantTopicsReplied;
	private double[] importantTopicsOnlySeen;
	private double[] importantTopicsFlagged;
	//important people profiles for the mailbox
	private double[] importantPeopleReplied;
	private double[] importantPeopleOnlySeen;
	private double[] importantPeopleFlagged;
	
	private double[] importantNLPKeywordsReplied;
	private double[] importantNLPKeywordsOnlySeen;
	private double[] importantNLPKeywordsFlagged;
	
	
	//important list emails; which are handled separately because the load of list emails is high 
	private double[] importantListTopicsReplied;
	private double[] importantListTopicsOnlySeen;
	private double[] importantListTopicsFlagged;
	//important people profiles for the mailbox
	private double[] importantListPeopleReplied;
	private double[] importantListPeopleOnlySeen;
	private double[] importantListPeopleFlagged;
	
	private double[] importantListNLPKeywordsReplied;
	private double[] importantListNLPKeywordsOnlySeen;
	private double[] importantListNLPKeywordsFlagged;
	//new variables for importance vectors
	//direct emails
	Set<Email> flaggedEmails;
	Set<Email> repliedEmails;
	Set<Email> seenEmails;
	
	//list emails
	Set<Email> flaggedListEmails;
	Set<Email> repliedListEmails;
	Set<Email> seenListEmails;
	
	
	
	//spam emails which have some how managed to get into inbox
	//identified by spam,precedence headers
	Set<Email> spamEmails;
			
	private final static Logger logger = LoggerFactory
			.getLogger(EmailReputationDataModel.class);
	
	public EmailReputationDataModel(){
		contentClusters = new ArrayList<EmailContentCluster>();
		recipientClusters = new ArrayList<EmailRecipientCluster>();
		weightedSubjectBodyClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		
		flaggedEmails = new HashSet<Email>();
		repliedEmails = new HashSet<Email>();
		seenEmails = new HashSet<Email>();
		
		flaggedListEmails = new HashSet<Email>();
		repliedListEmails = new HashSet<Email>();
		seenListEmails = new HashSet<Email>();
		
		spamEmails = new HashSet<Email>();
		spamVector = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		spamPeopleVector  = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		spamNLPKeywordVector = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];

		importantTopicsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantTopicsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantTopicsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		//important people profiles for the mailbox
		importantPeopleReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantNLPKeywordsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantNLPKeywordsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantNLPKeywordsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		
		//important list emails; which are handled separately because the load of list emails is high 
		importantListTopicsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		//important people profiles for the mailbox
		importantListPeopleReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantListNLPKeywordsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListNLPKeywordsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListNLPKeywordsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
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
	public Set<Email> getFlaggedEmails() {
		return flaggedEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<Email> getRepliedEmails() {
		return repliedEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<Email> getSeenEmails() {
		return seenEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<Email> getSpamEmails() {
		return spamEmails;
	}

	public void setFlaggedEmails(Set<Email> flaggedEmails) {
		this.flaggedEmails = flaggedEmails;
	}

	public void setRepliedEmails(Set<Email> repliedEmails) {
		this.repliedEmails = repliedEmails;
	}

	public void setSeenEmails(Set<Email> seenEmails) {
		this.seenEmails = seenEmails;
	}

	public void setSpamEmails(Set<Email> spamEmails) {
		this.spamEmails = spamEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<Email> getFlaggedListEmails() {
		return flaggedListEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<Email> getRepliedListEmails() {
		return repliedListEmails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<Email> getSeenListEmails() {
		return seenListEmails;
	}

	public void setFlaggedListEmails(Set<Email> flaggedListEmails) {
		this.flaggedListEmails = flaggedListEmails;
	}

	public void setRepliedListEmails(Set<Email> repliedListEmails) {
		this.repliedListEmails = repliedListEmails;
	}

	public void setSeenListEmails(Set<Email> seenListEmails) {
		this.seenListEmails = seenListEmails;
	}


	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getSpamVector() {
		return spamVector;
	}

	public void setSpamVector(double[] spamVector) {
		this.spamVector = spamVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getSpamPeopleVector() {
		return spamPeopleVector;
	}

	public void setSpamPeopleVector(double[] spamPeopleVector) {
		this.spamPeopleVector = spamPeopleVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getSpamNLPKeywordVector() {
		return spamNLPKeywordVector;
	}

	public void setSpamNLPKeywordVector(double[] spamNLPKeywordVector) {
		this.spamNLPKeywordVector = spamNLPKeywordVector;
	}

	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsReplied() {
		return importantTopicsReplied;
	}

	public void setImportantTopicsReplied(double[] importantTopicsReplied) {
		this.importantTopicsReplied = importantTopicsReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsOnlySeen() {
		return importantTopicsOnlySeen;
	}

	public void setImportantTopicsOnlySeen(double[] importantTopicsOnlySeen) {
		this.importantTopicsOnlySeen = importantTopicsOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsFlagged() {
		return importantTopicsFlagged;
	}

	public void setImportantTopicsFlagged(double[] importantTopicsFlagged) {
		this.importantTopicsFlagged = importantTopicsFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleReplied() {
		return importantPeopleReplied;
	}

	public void setImportantPeopleReplied(double[] importantPeopleReplied) {
		this.importantPeopleReplied = importantPeopleReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleOnlySeen() {
		return importantPeopleOnlySeen;
	}

	public void setImportantPeopleOnlySeen(double[] importantPeopleOnlySeen) {
		this.importantPeopleOnlySeen = importantPeopleOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleFlagged() {
		return importantPeopleFlagged;
	}

	public void setImportantPeopleFlagged(double[] importantPeopleFlagged) {
		this.importantPeopleFlagged = importantPeopleFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantNLPKeywordsReplied() {
		return importantNLPKeywordsReplied;
	}

	public void setImportantNLPKeywordsReplied(double[] importantNLPKeywordsReplied) {
		this.importantNLPKeywordsReplied = importantNLPKeywordsReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantNLPKeywordsOnlySeen() {
		return importantNLPKeywordsOnlySeen;
	}

	public void setImportantNLPKeywordsOnlySeen(
			double[] importantNLPKeywordsOnlySeen) {
		this.importantNLPKeywordsOnlySeen = importantNLPKeywordsOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantNLPKeywordsFlagged() {
		return importantNLPKeywordsFlagged;
	}

	public void setImportantNLPKeywordsFlagged(double[] importantNLPKeywordsFlagged) {
		this.importantNLPKeywordsFlagged = importantNLPKeywordsFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsReplied() {
		return importantListTopicsReplied;
	}

	public void setImportantListTopicsReplied(double[] importantListTopicsReplied) {
		this.importantListTopicsReplied = importantListTopicsReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsOnlySeen() {
		return importantListTopicsOnlySeen;
	}

	public void setImportantListTopicsOnlySeen(double[] importantListTopicsOnlySeen) {
		this.importantListTopicsOnlySeen = importantListTopicsOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsFlagged() {
		return importantListTopicsFlagged;
	}

	public void setImportantListTopicsFlagged(double[] importantListTopicsFlagged) {
		this.importantListTopicsFlagged = importantListTopicsFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleReplied() {
		return importantListPeopleReplied;
	}

	public void setImportantListPeopleReplied(double[] importantListPeopleReplied) {
		this.importantListPeopleReplied = importantListPeopleReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleOnlySeen() {
		return importantListPeopleOnlySeen;
	}

	public void setImportantListPeopleOnlySeen(double[] importantListPeopleOnlySeen) {
		this.importantListPeopleOnlySeen = importantListPeopleOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleFlagged() {
		return importantListPeopleFlagged;
	}

	public void setImportantListPeopleFlagged(double[] importantListPeopleFlagged) {
		this.importantListPeopleFlagged = importantListPeopleFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListNLPKeywordsReplied() {
		return importantListNLPKeywordsReplied;
	}

	public void setImportantListNLPKeywordsReplied(
			double[] importantListNLPKeywordsReplied) {
		this.importantListNLPKeywordsReplied = importantListNLPKeywordsReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListNLPKeywordsOnlySeen() {
		return importantListNLPKeywordsOnlySeen;
	}

	public void setImportantListNLPKeywordsOnlySeen(
			double[] importantListNLPKeywordsOnlySeen) {
		this.importantListNLPKeywordsOnlySeen = importantListNLPKeywordsOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListNLPKeywordsFlagged() {
		return importantListNLPKeywordsFlagged;
	}

	public void setImportantListNLPKeywordsFlagged(
			double[] importantListNLPKeywordsFlagged) {
		this.importantListNLPKeywordsFlagged = importantListNLPKeywordsFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getWeightedSubjectBodyClusters() {
		return weightedSubjectBodyClusters;
	}

	public void setWeightedSubjectBodyClusters(
			List<EmailWeightedSubjectBodyContentCluster> weightedSubjectBodyClusters) {
		this.weightedSubjectBodyClusters = weightedSubjectBodyClusters;
	}

}
