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
	
	//for subject/body separately
	private double[] importantTopicsSubjectsReplied;
	private double[] importantTopicsSubjectsOnlySeen;
	private double[] importantTopicsSubjectsFlagged;

	private double[] importantTopicsBodyReplied;
	private double[] importantTopicsBodyOnlySeen;
	private double[] importantTopicsBodyFlagged;
	
	
	//important people profiles for the mailbox
	private double[] importantPeopleReplied;
	private double[] importantPeopleOnlySeen;
	private double[] importantPeopleFlagged;
	
	//for separate from/to/cc vectors
	private double[] importantPeopleFromReplied;
	private double[] importantPeopleFromOnlySeen;
	private double[] importantPeopleFromFlagged;
	
	private double[] importantPeopleToReplied;
	private double[] importantPeopleToOnlySeen;
	private double[] importantPeopleToFlagged;
	
	private double[] importantPeopleCCReplied;
	private double[] importantPeopleCCOnlySeen;
	private double[] importantPeopleCCFlagged;
	
	
	
	private double[] importantNLPKeywordsReplied;
	private double[] importantNLPKeywordsOnlySeen;
	private double[] importantNLPKeywordsFlagged;
	
	
	//important list emails; which are handled separately because the load of list emails is high 
	private double[] importantListTopicsReplied;
	private double[] importantListTopicsOnlySeen;
	private double[] importantListTopicsFlagged;
	
	private double[] importantListTopicsSubjectsReplied;
	private double[] importantListTopicsSubjectsOnlySeen;
	private double[] importantListTopicsSubjectsFlagged;

	private double[] importantListTopicsBodyReplied;
	private double[] importantListTopicsBodyOnlySeen;
	private double[] importantListTopicsBodyFlagged;
	
	//important people profiles for the mailbox
	private double[] importantListPeopleReplied;
	private double[] importantListPeopleOnlySeen;
	private double[] importantListPeopleFlagged;
	
	//for separate from/to/cc vectors
	private double[] importantListPeopleFromReplied;
	private double[] importantListPeopleFromOnlySeen;
	private double[] importantListPeopleFromFlagged;
		
	private double[] importantListPeopleToReplied;
	private double[] importantListPeopleToOnlySeen;
	private double[] importantListPeopleToFlagged;
		
	private double[] importantListPeopleCCReplied;
	private double[] importantListPeopleCCOnlySeen;
	private double[] importantListPeopleCCFlagged;
	
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
	
	private double dunnIndexForRecipientClusters;
	private double dunnIndexForContentClusters;
	private double dunnIndexForSubjectBodyClusters;
	private double sseContentClusters;
	private double sseRecipientClusters;
	private double sseSubjectBodyClusters;
	
	
	
	//clusters in the profiles
	private List<EmailWeightedSubjectBodyContentCluster> repliedProfileContentClusters;
	private List<EmailRecipientCluster> repliedProfilePeopleClusters;
	
	private List<EmailWeightedSubjectBodyContentCluster> seenProfileContentClusters;
	private List<EmailRecipientCluster> seenProfilePeopleClusters;
	
	private List<EmailWeightedSubjectBodyContentCluster> flaggedProfileContentClusters;
	private List<EmailRecipientCluster> flaggedProfilePeopleClusters;
	
	//list emails
	private List<EmailWeightedSubjectBodyContentCluster> repliedListProfileContentClusters;
	private List<EmailRecipientCluster> repliedListProfilePeopleClusters;
	private List<EmailWeightedSubjectBodyContentCluster> seenListProfileContentClusters;
	private List<EmailRecipientCluster> seenListProfilePeopleClusters;
	
	private List<EmailWeightedSubjectBodyContentCluster> flaggedListProfileContentClusters;
	private List<EmailRecipientCluster> flaggedListProfilePeopleClusters;
	
	
	//spam emails which have some how managed to get into inbox
	//identified by spam,precedence headers
	Set<Email> spamEmails;
			
	private final static Logger logger = LoggerFactory
			.getLogger(EmailReputationDataModel.class);
	
	public EmailReputationDataModel(){
		contentClusters = new ArrayList<EmailContentCluster>();
		recipientClusters = new ArrayList<EmailRecipientCluster>();
		weightedSubjectBodyClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		
		//profile clusters
		repliedProfileContentClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		repliedProfilePeopleClusters = new ArrayList<EmailRecipientCluster>();
		seenProfileContentClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		seenProfilePeopleClusters = new ArrayList<EmailRecipientCluster>();
		setFlaggedProfileContentClusters(new ArrayList<EmailWeightedSubjectBodyContentCluster>());
		flaggedProfilePeopleClusters = new ArrayList<EmailRecipientCluster>();
		
		//list email profile clusters
		repliedListProfileContentClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		repliedListProfilePeopleClusters = new ArrayList<EmailRecipientCluster>();
		seenListProfileContentClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		seenListProfilePeopleClusters = new ArrayList<EmailRecipientCluster>();
		flaggedListProfileContentClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
		flaggedListProfilePeopleClusters = new ArrayList<EmailRecipientCluster>();
		
		
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
		
		importantTopicsSubjectsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantTopicsSubjectsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantTopicsSubjectsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];

		importantTopicsBodyReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantTopicsBodyOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantTopicsBodyFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		
		//important people profiles for the mailbox
		importantPeopleReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantPeopleFromReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleFromOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleFromFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantPeopleToReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleToOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleToFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantPeopleCCReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleCCOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantPeopleCCFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantNLPKeywordsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantNLPKeywordsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantNLPKeywordsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		
		//important list emails; which are handled separately because the load of list emails is high 
		importantListTopicsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantListTopicsSubjectsReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsSubjectsOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsSubjectsFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];

		importantListTopicsBodyReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsBodyOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListTopicsBodyFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		
		//important people profiles for the mailbox
		importantListPeopleReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantListPeopleFromReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleFromOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleFromFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantListPeopleToReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleToOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleToFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
		importantListPeopleCCReplied = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleCCOnlySeen = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		importantListPeopleCCFlagged = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		
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
	public double calculateDunnIndexForContentClusters(){
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
		this.setDunnIndexForContentClusters(dunnIndex);
		return dunnIndex;
	}

	/**
	 * if min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are compact and well clustered
	 * @return dunnIndex to validate cluster quality
	 */
	public double calculateDunnIndexForRecipientClusters(){
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
		double dunnIndex = minInterClusterDistance/maxIntraClusterDistance;
		this.setDunnIndexForRecipientClusters(dunnIndex);
		logger.info("min inter-cluster distance : " + minInterClusterDistance + " max. intracluster distance : " + maxIntraClusterDistance + " recipient cluster dunn index :" + dunnIndex);
		
		return dunnIndex;
	}
	
	/**
	 * for subject, body clusters
	 * if min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are compact and well clustered
	 * @return dunnIndex to validate cluster quality
	 */
	public double calculateDunnIndexForSubjectBodyClusters(){
		//min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are CWS
		double minInterClusterSubjectDistance = 0;
		double maxIntraClusterSubjectDistance = 0;
		
		double minInterClusterBodyDistance = 0;
		double maxIntraClusterBodyDistance = 0;
		
		for(EmailWeightedSubjectBodyContentCluster c1: this.getWeightedSubjectBodyClusters()){
			for(EmailWeightedSubjectBodyContentCluster c2 : this.getWeightedSubjectBodyClusters()){
				
				if(c1.getId() != c2.getId()){
					double[] v1 = c1.getSubjectCentroid();
					double[] v2 = c2.getSubjectCentroid();
					double subjectDis = VectorsMath.getDistance(v1, v2);
					
					double[] v3 = c1.getBodyCentroid();
					double[] v4 = c2.getBodyCentroid();
					double bodyDis = VectorsMath.getDistance(v3, v4);
					
//					logger.info("inter-cluster distance between : " + c1.getId() + " and " + c2.getId() 
//								+ " distance: "+ dis);
//					
					if(minInterClusterSubjectDistance == 0) {
						minInterClusterSubjectDistance = subjectDis;
					} else {
						if(subjectDis < minInterClusterSubjectDistance) {
							minInterClusterSubjectDistance = subjectDis;
						}
					}
					
					if(minInterClusterBodyDistance == 0) {
						minInterClusterBodyDistance = bodyDis;
					} else {
						if(bodyDis < minInterClusterBodyDistance) {
							minInterClusterBodyDistance = bodyDis;
						}
					}
				}
			}
		}
		
		for(EmailWeightedSubjectBodyContentCluster c: this.getWeightedSubjectBodyClusters()){
			double sumOfIntraClusterSubjectDistance = 0;
			double intraClusterSubjectDistance = 0;
			
			double sumOfIntraClusterBodyDistance = 0;
			double intraClusterBodyDistance = 0;
			
			for(Email mail : c.getSubjectBodyContentEmails()){
				//distance from the centroid
				double[] v1 = mail.getSubjectContextVector();
				double subjectDis = VectorsMath.getDistance(v1, c.getSubjectCentroid());
				
				double[] v2 = mail.getBodyContextVector();
				double bodyDis = VectorsMath.getDistance(v1, c.getBodyCentroid());
				
				
				sumOfIntraClusterSubjectDistance += subjectDis;
				sumOfIntraClusterBodyDistance += bodyDis;
			}
			
			intraClusterSubjectDistance = sumOfIntraClusterSubjectDistance / c.getSubjectBodyContentEmails().size();
			intraClusterBodyDistance = sumOfIntraClusterBodyDistance / c.getSubjectBodyContentEmails().size();
			
			//logger.info("intracluster distance : " + intraClusterDistance);
			if(maxIntraClusterSubjectDistance < intraClusterSubjectDistance){
				maxIntraClusterSubjectDistance = intraClusterSubjectDistance;
			}
			if(maxIntraClusterBodyDistance < intraClusterBodyDistance){
				maxIntraClusterBodyDistance = intraClusterBodyDistance;
			}
		}
		
		double subjectDunnIndex = minInterClusterSubjectDistance/maxIntraClusterSubjectDistance;
		double bodyDunnIndex = minInterClusterBodyDistance/maxIntraClusterBodyDistance;
		double avgDunnIndex = (subjectDunnIndex + bodyDunnIndex)/2;
		this.setDunnIndexForSubjectBodyClusters(avgDunnIndex);
		logger.info("min inter-cluster subject distance : " + minInterClusterSubjectDistance + " max. intracluster subject distance : " + maxIntraClusterSubjectDistance );
		logger.info("min inter-cluster body distance : " + minInterClusterBodyDistance + " max. intracluster body distance : " + maxIntraClusterBodyDistance );
		logger.info("avg. dunn index for subject-body clusters : " + avgDunnIndex );
		
		return avgDunnIndex;
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
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsSubjectsReplied() {
		return importantTopicsSubjectsReplied;
	}

	public void setImportantTopicsSubjectsReplied(
			double[] importantTopicsSubjectsReplied) {
		this.importantTopicsSubjectsReplied = importantTopicsSubjectsReplied;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsSubjectsOnlySeen() {
		return importantTopicsSubjectsOnlySeen;
	}

	public void setImportantTopicsSubjectsOnlySeen(
			double[] importantTopicsSubjectsOnlySeen) {
		this.importantTopicsSubjectsOnlySeen = importantTopicsSubjectsOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsSubjectsFlagged() {
		return importantTopicsSubjectsFlagged;
	}

	public void setImportantTopicsSubjectsFlagged(
			double[] importantTopicsSubjectsFlagged) {
		this.importantTopicsSubjectsFlagged = importantTopicsSubjectsFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsBodyReplied() {
		return importantTopicsBodyReplied;
	}

	public void setImportantTopicsBodyReplied(double[] importantTopicsBodyReplied) {
		this.importantTopicsBodyReplied = importantTopicsBodyReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsBodyOnlySeen() {
		return importantTopicsBodyOnlySeen;
	}

	public void setImportantTopicsBodyOnlySeen(double[] importantTopicsBodyOnlySeen) {
		this.importantTopicsBodyOnlySeen = importantTopicsBodyOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantTopicsBodyFlagged() {
		return importantTopicsBodyFlagged;
	}

	public void setImportantTopicsBodyFlagged(double[] importantTopicsBodyFlagged) {
		this.importantTopicsBodyFlagged = importantTopicsBodyFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleFromReplied() {
		return importantPeopleFromReplied;
	}

	public void setImportantPeopleFromReplied(double[] importantPeopleFromReplied) {
		this.importantPeopleFromReplied = importantPeopleFromReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleFromOnlySeen() {
		return importantPeopleFromOnlySeen;
	}

	public void setImportantPeopleFromOnlySeen(double[] importantPeopleFromOnlySeen) {
		this.importantPeopleFromOnlySeen = importantPeopleFromOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleFromFlagged() {
		return importantPeopleFromFlagged;
	}

	public void setImportantPeopleFromFlagged(double[] importantPeopleFromFlagged) {
		this.importantPeopleFromFlagged = importantPeopleFromFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleToReplied() {
		return importantPeopleToReplied;
	}

	public void setImportantPeopleToReplied(double[] importantPeopleToReplied) {
		this.importantPeopleToReplied = importantPeopleToReplied;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleToOnlySeen() {
		return importantPeopleToOnlySeen;
	}

	public void setImportantPeopleToOnlySeen(double[] importantPeopleToOnlySeen) {
		this.importantPeopleToOnlySeen = importantPeopleToOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleToFlagged() {
		return importantPeopleToFlagged;
	}

	public void setImportantPeopleToFlagged(double[] importantPeopleToFlagged) {
		this.importantPeopleToFlagged = importantPeopleToFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleCCReplied() {
		return importantPeopleCCReplied;
	}

	public void setImportantPeopleCCReplied(double[] importantPeopleCCReplied) {
		this.importantPeopleCCReplied = importantPeopleCCReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleCCOnlySeen() {
		return importantPeopleCCOnlySeen;
	}

	public void setImportantPeopleCCOnlySeen(double[] importantPeopleCCOnlySeen) {
		this.importantPeopleCCOnlySeen = importantPeopleCCOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantPeopleCCFlagged() {
		return importantPeopleCCFlagged;
	}

	public void setImportantPeopleCCFlagged(double[] importantPeopleCCFlagged) {
		this.importantPeopleCCFlagged = importantPeopleCCFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsSubjectsReplied() {
		return importantListTopicsSubjectsReplied;
	}

	public void setImportantListTopicsSubjectsReplied(
			double[] importantListTopicsSubjectsReplied) {
		this.importantListTopicsSubjectsReplied = importantListTopicsSubjectsReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsSubjectsOnlySeen() {
		return importantListTopicsSubjectsOnlySeen;
	}

	public void setImportantListTopicsSubjectsOnlySeen(
			double[] importantListTopicsSubjectsOnlySeen) {
		this.importantListTopicsSubjectsOnlySeen = importantListTopicsSubjectsOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsSubjectsFlagged() {
		return importantListTopicsSubjectsFlagged;
	}

	public void setImportantListTopicsSubjectsFlagged(
			double[] importantListTopicsSubjectsFlagged) {
		this.importantListTopicsSubjectsFlagged = importantListTopicsSubjectsFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsBodyReplied() {
		return importantListTopicsBodyReplied;
	}

	public void setImportantListTopicsBodyReplied(
			double[] importantListTopicsBodyReplied) {
		this.importantListTopicsBodyReplied = importantListTopicsBodyReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsBodyOnlySeen() {
		return importantListTopicsBodyOnlySeen;
	}

	public void setImportantListTopicsBodyOnlySeen(
			double[] importantListTopicsBodyOnlySeen) {
		this.importantListTopicsBodyOnlySeen = importantListTopicsBodyOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListTopicsBodyFlagged() {
		return importantListTopicsBodyFlagged;
	}

	public void setImportantListTopicsBodyFlagged(
			double[] importantListTopicsBodyFlagged) {
		this.importantListTopicsBodyFlagged = importantListTopicsBodyFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleFromReplied() {
		return importantListPeopleFromReplied;
	}

	public void setImportantListPeopleFromReplied(
			double[] importantListPeopleFromReplied) {
		this.importantListPeopleFromReplied = importantListPeopleFromReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleFromOnlySeen() {
		return importantListPeopleFromOnlySeen;
	}

	public void setImportantListPeopleFromOnlySeen(
			double[] importantListPeopleFromOnlySeen) {
		this.importantListPeopleFromOnlySeen = importantListPeopleFromOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleFromFlagged() {
		return importantListPeopleFromFlagged;
	}

	public void setImportantListPeopleFromFlagged(
			double[] importantListPeopleFromFlagged) {
		this.importantListPeopleFromFlagged = importantListPeopleFromFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleToReplied() {
		return importantListPeopleToReplied;
	}

	public void setImportantListPeopleToReplied(
			double[] importantListPeopleToReplied) {
		this.importantListPeopleToReplied = importantListPeopleToReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleToOnlySeen() {
		return importantListPeopleToOnlySeen;
	}

	public void setImportantListPeopleToOnlySeen(
			double[] importantListPeopleToOnlySeen) {
		this.importantListPeopleToOnlySeen = importantListPeopleToOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleToFlagged() {
		return importantListPeopleToFlagged;
	}

	public void setImportantListPeopleToFlagged(
			double[] importantListPeopleToFlagged) {
		this.importantListPeopleToFlagged = importantListPeopleToFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleCCReplied() {
		return importantListPeopleCCReplied;
	}

	public void setImportantListPeopleCCReplied(
			double[] importantListPeopleCCReplied) {
		this.importantListPeopleCCReplied = importantListPeopleCCReplied;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleCCOnlySeen() {
		return importantListPeopleCCOnlySeen;
	}

	public void setImportantListPeopleCCOnlySeen(
			double[] importantListPeopleCCOnlySeen) {
		this.importantListPeopleCCOnlySeen = importantListPeopleCCOnlySeen;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getImportantListPeopleCCFlagged() {
		return importantListPeopleCCFlagged;
	}

	public void setImportantListPeopleCCFlagged(
			double[] importantListPeopleCCFlagged) {
		this.importantListPeopleCCFlagged = importantListPeopleCCFlagged;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getDunnIndexForSubjectBodyClusters() {
		return dunnIndexForSubjectBodyClusters;
	}

	public void setDunnIndexForSubjectBodyClusters(
			double dunnIndexForSubjectBodyClusters) {
		this.dunnIndexForSubjectBodyClusters = dunnIndexForSubjectBodyClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getSseContentClusters() {
		return sseContentClusters;
	}

	public void setSseContentClusters(double sseContentClusters) {
		this.sseContentClusters = sseContentClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getSseRecipientClusters() {
		return sseRecipientClusters;
	}

	public void setSseRecipientClusters(double sseRecipientClusters) {
		this.sseRecipientClusters = sseRecipientClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getSseSubjectBodyClusters() {
		return sseSubjectBodyClusters;
	}

	public void setSseSubjectBodyClusters(double sseSubjectBodyClusters) {
		this.sseSubjectBodyClusters = sseSubjectBodyClusters;
	}

	
	public void setDunnIndexForRecipientClusters(
			double dunnIndexForRecipientClusters) {
		this.dunnIndexForRecipientClusters = dunnIndexForRecipientClusters;
	}

	public void setDunnIndexForContentClusters(double dunnIndexForContentClusters) {
		this.dunnIndexForContentClusters = dunnIndexForContentClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getDunnIndexForRecipientClusters() {
		return dunnIndexForRecipientClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getDunnIndexForContentClusters() {
		return dunnIndexForContentClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getRepliedProfileContentClusters() {
		return repliedProfileContentClusters;
	}

	public void setRepliedProfileContentClusters(
			List<EmailWeightedSubjectBodyContentCluster> repliedProfileContentClusters) {
		this.repliedProfileContentClusters = repliedProfileContentClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getRepliedProfilePeopleClusters() {
		return repliedProfilePeopleClusters;
	}

	public void setRepliedProfilePeopleClusters(
			List<EmailRecipientCluster> repliedProfilePeopleClusters) {
		this.repliedProfilePeopleClusters = repliedProfilePeopleClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getSeenProfileContentClusters() {
		return seenProfileContentClusters;
	}

	public void setSeenProfileContentClusters(
			List<EmailWeightedSubjectBodyContentCluster> seenProfileContentClusters) {
		this.seenProfileContentClusters = seenProfileContentClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getSeenProfilePeopleClusters() {
		return seenProfilePeopleClusters;
	}

	public void setSeenProfilePeopleClusters(
			List<EmailRecipientCluster> seenProfilePeopleClusters) {
		this.seenProfilePeopleClusters = seenProfilePeopleClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getRepliedListProfileContentClusters() {
		return repliedListProfileContentClusters;
	}

	public void setRepliedListProfileContentClusters(
			List<EmailWeightedSubjectBodyContentCluster> repliedListProfileContentClusters) {
		this.repliedListProfileContentClusters = repliedListProfileContentClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getRepliedListProfilePeopleClusters() {
		return repliedListProfilePeopleClusters;
	}

	public void setRepliedListProfilePeopleClusters(
			List<EmailRecipientCluster> repliedListProfilePeopleClusters) {
		this.repliedListProfilePeopleClusters = repliedListProfilePeopleClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getSeenListProfileContentClusters() {
		return seenListProfileContentClusters;
	}

	public void setSeenListProfileContentClusters(
			List<EmailWeightedSubjectBodyContentCluster> seenListProfileContentClusters) {
		this.seenListProfileContentClusters = seenListProfileContentClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getSeenListProfilePeopleClusters() {
		return seenListProfilePeopleClusters;
	}

	public void setSeenListProfilePeopleClusters(
			List<EmailRecipientCluster> seenListProfilePeopleClusters) {
		this.seenListProfilePeopleClusters = seenListProfilePeopleClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getFlaggedProfileContentClusters() {
		return flaggedProfileContentClusters;
	}

	public void setFlaggedProfileContentClusters(
			List<EmailWeightedSubjectBodyContentCluster> flaggedProfileContentClusters) {
		this.flaggedProfileContentClusters = flaggedProfileContentClusters;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getFlaggedProfilePeopleClusters() {
		return flaggedProfilePeopleClusters;
	}

	public void setFlaggedProfilePeopleClusters(
			List<EmailRecipientCluster> flaggedProfilePeopleClusters) {
		this.flaggedProfilePeopleClusters = flaggedProfilePeopleClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailWeightedSubjectBodyContentCluster> getFlaggedListProfileContentClusters() {
		return flaggedListProfileContentClusters;
	}

	public void setFlaggedListProfileContentClusters(
			List<EmailWeightedSubjectBodyContentCluster> flaggedListProfileContentClusters) {
		this.flaggedListProfileContentClusters = flaggedListProfileContentClusters;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailRecipientCluster> getFlaggedListProfilePeopleClusters() {
		return flaggedListProfilePeopleClusters;
	}

	public void setFlaggedListProfilePeopleClusters(
			List<EmailRecipientCluster> flaggedListProfilePeopleClusters) {
		this.flaggedListProfilePeopleClusters = flaggedListProfilePeopleClusters;
	}

}
