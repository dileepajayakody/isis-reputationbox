package org.nic.isis.reputation.dom;

import java.util.HashSet;
import java.util.Set;
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
@ObjectType("EMAILCONTACT")
public class EmailContact {

    //region > constructor
    public EmailContact(){
        emailAddresses = new HashSet<EmailAddress>();
    }
    //endregion

    //region > name (property)
    private String name;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
    //endregion

    //region > contactReputation (property)
	private Reputation contactReputation;

    @javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public Reputation getContactReputation() {
		return contactReputation;
	}

	public void setContactReputation(Reputation contactReputation) {
		this.contactReputation = contactReputation;
	}
    //endregion

    //region > emailAddresses (collection), addEmailAddress (action)
	private Set<EmailAddress> emailAddresses;

    @javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public Set<EmailAddress> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set<EmailAddress> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	public void addEmailAddress(EmailAddress emailAddress){
		emailAddresses.add(emailAddress);
	}
    //endregion

}
