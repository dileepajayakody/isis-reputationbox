package org.nic.isis.reputation.dom;

public class EmailBody {

	private boolean hasAttachments;
	public String getMessageContent() {
		return messageContent;
	}
	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}
	public boolean isHasImages() {
		return hasImages;
	}
	public void setHasImages(boolean hasImages) {
		this.hasImages = hasImages;
	}
	public int getNoOfImages() {
		return noOfImages;
	}
	public void setNoOfImages(int noOfImages) {
		this.noOfImages = noOfImages;
	}
	public int getNoOfAttachments() {
		return noOfAttachments;
	}
	public void setNoOfAttachments(int noOfAttachments) {
		this.noOfAttachments = noOfAttachments;
	}
	private String messageContent;
	private boolean hasImages;
	
	int noOfImages;
	int noOfAttachments;
	public boolean isHasAttachments() {
		return hasAttachments;
	}
	public void setHasAttachments(boolean hasAttachments) {
		this.hasAttachments = hasAttachments;
	}
	
	public boolean isHasHtml() {
		return hasHtml;
	}
	public void setHasHtml(boolean hasHtml) {
		this.hasHtml = hasHtml;
	}
	private boolean hasHtml;
	
}
