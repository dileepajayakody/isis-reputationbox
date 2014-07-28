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

	@javax.jdo.annotations.NotPersistent
	private String tokenStream;
	

	@javax.jdo.annotations.NotPersistent
	public String getTokenStream() {
		return tokenStream;
	}

	public void setTokenStream(String tokenStream) {
		this.tokenStream = tokenStream;
	}
	
	private Map<String, Integer> stringTokens;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getStringTokens() {
		return stringTokens;
	}

	public void setStringTokens(Map<String, Integer> stringTokens) {
		this.stringTokens = stringTokens;
	}

	private Map<String, Integer> urlTokens;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getUrlTokens() {
		return urlTokens;
	}

	public void setUrlTokens(Map<String, Integer> urls) {
		this.urlTokens = urls;
	}

	private Map<String, Integer> numberTokens;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getNumberTokens() {
		return numberTokens;
	}

	public void setNumberTokens(Map<String, Integer> numbers) {
		this.numberTokens = numbers;
	}

	private Map<String, Integer> emoticonTokens;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getEmoticonTokens() {
		return emoticonTokens;
	}

	public void setEmoticonTokens(Map<String, Integer> emoticons) {
		this.emoticonTokens = emoticons;
	}

	private Map<String, Integer> emailTokens;

	@javax.jdo.annotations.Column(allowsNull="true")
	public Map<String, Integer> getEmailTokens() {
		return emailTokens;
	}

	public void setEmailTokens(Map<String, Integer> emails) {
		this.emailTokens = emails;
	}

}
