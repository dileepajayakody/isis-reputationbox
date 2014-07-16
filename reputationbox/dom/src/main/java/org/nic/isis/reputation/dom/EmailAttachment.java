package org.nic.isis.reputation.dom;

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
@ObjectType("EMAILATTACHMENT")
public class EmailAttachment {

	
	private String attachmentId;
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="false")
	public String getAttachmentId() {
		return attachmentId;
	}
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}
	
	private String fileName;
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="false")
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	private int size;
	
	@javax.jdo.annotations.Persistent
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	
	private String type;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="false")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}
