package org.nic.isis.reputation.dom;
import java.util.List;
import java.util.Map;

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

	private Map<String, Integer> stringTokens;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getStringTokens() {
		return stringTokens;
	}

	public void setStringTokens(Map<String, Integer> stringTokens) {
		this.stringTokens = stringTokens;
	}

	private Map<String, Integer> urls;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getUrls() {
		return urls;
	}

	public void setUrls(Map<String, Integer> urls) {
		this.urls = urls;
	}

	private Map<String, Integer> numbers;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getNumbers() {
		return numbers;
	}

	public void setNumbers(Map<String, Integer> numbers) {
		this.numbers = numbers;
	}

	private Map<String, Integer> emoticons;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getEmoticons() {
		return emoticons;
	}

	public void setEmoticons(Map<String, Integer> emoticons) {
		this.emoticons = emoticons;
	}

	private Map<String, Integer> emails;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getEmails() {
		return emails;
	}

	public void setEmails(Map<String, Integer> emails) {
		this.emails = emails;
	}

}
