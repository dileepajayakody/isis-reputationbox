package org.nic.isis.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.common.Similarity;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@ObjectType("EMAILRECIPIENTCLUSTER")
public class EmailRecipientCluster extends EmailCluster {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailRecipientCluster.class);
	
	private List<Email> recipientEmails;
	private List<String> recipientEmailIds;
	private int noOfMessagesDirect;
	private int noOfMessagesCCd;
	
	private int noOfMessagesDirectAndAnswered;
	private int noOfMessagesCCdAndAnswered;
	private int noOfMessagesDirectAndSeen;
	private int noOfMessagesCCdAndSeen;
	
	
	public static final int DIRECT_ANSWERED_WEIGHT = 5, CC_ANSWERED_WEIGHT = 4,
			DIRECT_SEEN_WEIGHT = 3, CC_SEEN_WEIGHT = 2;
	
	public EmailRecipientCluster(double[] v) {
		super(v);
		this.setClusterType(RECIPIENT_CLUSTER_TYPE);
	}

	public EmailRecipientCluster(String id) {
		super(id);
		this.setClusterType(RECIPIENT_CLUSTER_TYPE);
	}
	
	public void addEmail(String emailId, Email email){
		//super.addEmail(emailId, email);
		super.addVector(email.getRecipientContextVector());
		if(this.getRecipientEmailIds() == null){
			this.setRecipientEmailIds(new ArrayList<String>());
		}
		if(this.getRecipientEmails() == null){
			this.setRecipientEmails(new ArrayList<Email>());
		}
		
		//logger.info(" Adding email " + email.getMessageId() + "to content cluster : " + this.id);
		this.recipientEmails.add(email);
		this.recipientEmailIds.add(emailId);
		
		//addEmailAtributes
		if (email.isAnswered()) {
			if(email.isDirect()){
				this.noOfMessagesDirectAndAnswered++;
			}else if(email.isCCd()){
				this.noOfMessagesCCdAndAnswered++;
			}else{
				this.addMessageAnswered();
			}
			//calculate the response time for the email
			//String receivedMessageId = email.getMessageId();
			
		}
		if (email.isFlagged()) {
			this.addMessageFlagged();
		}
		if (email.isSeen()){
			if(email.isDirect()){
				this.noOfMessagesDirectAndSeen++;
			}else if(email.isCCd()){
				this.noOfMessagesCCdAndSeen++;
			}else{
				this.addMessageSeen();
			}
		}
		if(email.isDeleted()){
			this.addMessageDeleted();
		}
		//commenting due to direct&seen/answered, ccd&seen/answered logic
		if(email.isDirect()){
			this.addDirectMessage();
		}
		if(email.isCCd()){
			this.addCCMessage();
		}
		/*centroid = VectorsMath.addArrays(centroid, email.getRecipientContextVector());
		centroid = VectorsMath.devideArray(centroid, emails.size());*/
	}
	
	public void removeEmail(String emailId) {
		
		for(int i=0; i< this.getRecipientEmails().size(); i++){
			Email email = this.getRecipientEmails().get(i);
			if(email.getMessageId().equals(emailId)){
				if(email.isAnswered()){
					if(email.isDirect()){
						this.noOfMessagesDirectAndAnswered--;
					}else if(email.isCCd()){
						this.noOfMessagesCCdAndAnswered--;
					}else{
						decrementMessageAnswered();
					}
				}
				if (email.isFlagged()) {
					this.decrementMessageFlagged();
				}
				if (email.isSeen()){
					if(email.isDirect()){
						this.noOfMessagesDirectAndSeen--;
					}else if(email.isCCd()){
						this.noOfMessagesCCdAndSeen--;
					}else{
						this.decrementMessageSeen();
					}
				}
				if(email.isDeleted()){
					this.decrementMessageDeleted();
				}
				if(email.isDirect()){
					this.decrementDirectMessage();
				}
				if(email.isCCd()){
					this.decrementCCMessage();
				}

				this.getRecipientEmails().remove(i);
				//logger.info("REMOVING email : " + email.getMessageId() + " from cluster : " + this.id);
				double[] emailRecipientContextVector = email.getRecipientContextVector();
				this.centroid = VectorsMath.substractArrays(this.centroid, emailRecipientContextVector);
				
				break;
			}
		}
	}
	public double calculateClusterReputationScore(){
		noOfMessagesDirectAndAnswered = 0;
		noOfMessagesCCdAndAnswered = 0;
		noOfMessagesDirectAndSeen = 0;
		noOfMessagesCCdAndSeen = 0;
		
		for(Email email : this.getRecipientEmails()){
			if(email.isAnswered()){
				if(email.isDirect()){
					this.noOfMessagesDirectAndAnswered++;
				}else if(email.isCCd()){
					this.noOfMessagesCCdAndAnswered++;
				}else{
					this.noOfMessagesAnswered++;
				}
			}
			if (email.isFlagged()) {
				noOfMessagesFlagged++;
			}
			if (email.isSeen()){
				if(email.isDirect()){
					this.noOfMessagesDirectAndSeen++;
				}else if(email.isCCd()){
					this.noOfMessagesCCdAndSeen++;
				}else{
					noOfMessagesSeen++;
				}
			}
			if(email.isDeleted()){
				noOfMessagesDeleted++;
			}
		}
		int totalPositiveWeight = FLAGGED_WEIGHT + ANSWERERD_WEIGHT
				+ SEEN_WEIGHT + DIRECT_ANSWERED_WEIGHT + DIRECT_SEEN_WEIGHT 
				+ CC_ANSWERED_WEIGHT + CC_SEEN_WEIGHT;
		double reputationScore = (double)((noOfMessagesFlagged * FLAGGED_WEIGHT)
				+ (noOfMessagesAnswered * ANSWERERD_WEIGHT)
				+ (noOfMessagesSeen * SEEN_WEIGHT) 
				+ (noOfMessagesDirectAndAnswered * DIRECT_ANSWERED_WEIGHT) 
				+ (noOfMessagesCCdAndAnswered * CC_ANSWERED_WEIGHT)
				+ (noOfMessagesDirectAndSeen * DIRECT_SEEN_WEIGHT)
				+ (noOfMessagesCCdAndSeen * CC_SEEN_WEIGHT)
				- (noOfMessagesDeleted * DELETED_WEIGHT))
				
				/ (double)(this.getRecipientEmails().size() * totalPositiveWeight);
		
		logger.info("Calculating reputation score of recipient cluster id : " + this.id 
				+ " : (flagged : "+ noOfMessagesFlagged + " * " + FLAGGED_WEIGHT + ") + (answered other emails: "
				+ noOfMessagesAnswered + " * " + ANSWERERD_WEIGHT + ") + (direct and answered : "
				+ noOfMessagesDirectAndAnswered + " * " + DIRECT_ANSWERED_WEIGHT + ") + (ccd and answered:"
				+ noOfMessagesCCdAndAnswered + " * " + CC_ANSWERED_WEIGHT + ") + (ccd and seen:"
				+ noOfMessagesCCdAndSeen + " * " + CC_SEEN_WEIGHT + ") + (direct and seen:"
				+ noOfMessagesCCdAndSeen + " * " + DIRECT_SEEN_WEIGHT + ") + (seen other emails:"
				+ noOfMessagesSeen + " * " + SEEN_WEIGHT + ") - (deleted:"
				+ noOfMessagesDeleted + " * " + DELETED_WEIGHT + ") / (" 
				+ this.getRecipientEmails().size() + " * " + totalPositiveWeight + ")"
				+ " Score: " + reputationScore);
		this.setReputationScore(reputationScore);
		return this.reputationScore;
	}

	@Programmatic
	public double getSimilarity(double[] vector) {
		//return Similarity.cosineSimilarity(getAverageCentroid(), vector);
		return Similarity.cosineSimilarity(centroid, vector);
	}
	
	public double[] getAverageCentroid() {
		// TODO Auto-generated method stub
		double[] avgCentroid = centroid;
		if(this.getRecipientEmails() != null && this.getRecipientEmails().size() > 0){
			avgCentroid = VectorsMath.devideArray(avgCentroid, this.getRecipientEmails().size());	
		
		}
		return avgCentroid ;
	}
	
	public double getSumOfSquaresError(){
		double sumOfSquaredError = 0; 
		for(Email email : this.getRecipientEmails()){
			double[] x = email.getRecipientContextVector();
			double[] centroid = this.getCentroid();
			double squaredDistance = VectorsMath.getSquaredDistance(x, centroid);
			sumOfSquaredError += squaredDistance;
		}
		return sumOfSquaredError;
	}
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getRecipientEmails() {
		return recipientEmails;
	}

	public void setRecipientEmails(List<Email> recipientEmails) {
		this.recipientEmails = recipientEmails;
	}

	public List<String> getRecipientEmailIds() {
		return recipientEmailIds;
	}

	public void setRecipientEmailIds(List<String> recipientEmailIds) {
		this.recipientEmailIds = recipientEmailIds;
	}

	public int getNoOfMessagesDirect() {
		return noOfMessagesDirect;
	}

	public void setNoOfMessagesDirect(int noOfMessagesDirect) {
		this.noOfMessagesDirect = noOfMessagesDirect;
	}

	public int getNoOfMessagesCCd() {
		return noOfMessagesCCd;
	}

	public void setNoOfMessagesCCd(int noOfMessagesCCd) {
		this.noOfMessagesCCd = noOfMessagesCCd;
	}
	
	public void addDirectMessage(){
		this.noOfMessagesDirect++;
	}
	
	public void decrementDirectMessage(){
		this.noOfMessagesDirect--;
	}
	
	public void addCCMessage(){
		this.noOfMessagesCCd++;
	}
	
	public void decrementCCMessage(){
		this.noOfMessagesCCd--;
	}

	public int getNoOfMessagesDirectAndAnswered() {
		return noOfMessagesDirectAndAnswered;
	}

	public void setNoOfMessagesDirectAndAnswered(
			int noOfMessagesDirectAndAnswered) {
		this.noOfMessagesDirectAndAnswered = noOfMessagesDirectAndAnswered;
	}

	public int getNoOfMessagesCCdAndAnswered() {
		return noOfMessagesCCdAndAnswered;
	}

	public void setNoOfMessagesCCdAndAnswered(int noOfMessagesCCdAndAnswered) {
		this.noOfMessagesCCdAndAnswered = noOfMessagesCCdAndAnswered;
	}

	public int getNoOfMessagesDirectAndSeen() {
		return noOfMessagesDirectAndSeen;
	}

	public void setNoOfMessagesDirectAndSeen(int noOfMessagesDirectAndSeen) {
		this.noOfMessagesDirectAndSeen = noOfMessagesDirectAndSeen;
	}

	public int getNoOfMessagesCCdAndSeen() {
		return noOfMessagesCCdAndSeen;
	}

	public void setNoOfMessagesCCdAndSeen(int noOfMessagesCCdAndSeen) {
		this.noOfMessagesCCdAndSeen = noOfMessagesCCdAndSeen;
	}

	
}
