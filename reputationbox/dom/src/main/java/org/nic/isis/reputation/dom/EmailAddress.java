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
@ObjectType("EMAILADDRESS")
public class EmailAddress {

	public EmailAddress(){
		
	}
	public EmailAddress(String emailId){
		this.emailId = emailId;
	}
    //region > emailId (property)
	private String emailId;

    @javax.jdo.annotations.Persistent
	@javax.jdo.annotations.PrimaryKey
	@javax.jdo.annotations.Column(allowsNull="false")
	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
    //endregion

    //region > name (property)
	@javax.jdo.annotations.Persistent
	private String name;

	@javax.jdo.annotations.Column(allowsNull="true")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
    //endregion

}
