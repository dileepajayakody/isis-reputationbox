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

	private String name;
	private Reputation contactReputation;
	private Set<EmailAddress> emailAddresses;
	
	public EmailContact(){
		emailAddresses = new HashSet<EmailAddress>();
	}
	
	public void addEmailAddress(EmailAddress emailAddress){
		emailAddresses.add(emailAddress);
	}
}
