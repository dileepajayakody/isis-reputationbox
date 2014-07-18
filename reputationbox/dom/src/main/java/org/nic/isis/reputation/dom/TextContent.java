package org.nic.isis.reputation.dom;
import java.util.List;

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
@ObjectType("TEXTCONTENT")
public class TextContent {

	private List<String> stringTokens;

	public List<String> getStringTokens() {
		return stringTokens;
	}

	public void setStringTokens(List<String> stringTokens) {
		this.stringTokens = stringTokens;
	}

	private List<String> urls;

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

	private List<String> numbers;

	public List<String> getNumbers() {
		return numbers;
	}

	public void setNumbers(List<String> numbers) {
		this.numbers = numbers;
	}

	private List<String> emoticons;

	public List<String> getEmoticons() {
		return emoticons;
	}

	public void setEmoticons(List<String> emoticons) {
		this.emoticons = emoticons;
	}

	private List<String> emails;

	public List<String> getEmails() {
		return emails;
	}

	public void setEmails(List<String> emails) {
		this.emails = emails;
	}

}
