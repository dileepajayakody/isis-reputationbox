package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("TEXTTOKENMAP")
public class TextTokenMap {
	private List<String> textWordsList;
	private List<Integer> wordFrequencyList;
	
	private final static Logger logger = LoggerFactory
			.getLogger(TextTokenMap.class);
	public TextTokenMap(){
		textWordsList = new ArrayList<String>();
		wordFrequencyList = new ArrayList<Integer>();
	}
	
	
	public synchronized void putTokenToMap(String key, Integer value) {
		if (!textWordsList.contains(key)) {
	      textWordsList.add(key);
	      wordFrequencyList.add(value);
	    } else {
	    	int frequency = wordFrequencyList.get(textWordsList.indexOf(key));
	    	frequency = frequency + value;
	    	wordFrequencyList.set(textWordsList.indexOf(key), frequency);
	    }
	      
	  }
	
	
	 public int getWordFrequency(String key) {
		 int index = -1;
		 if(textWordsList.contains(key)){
			index = textWordsList.indexOf(key);
			Integer freq = wordFrequencyList.get(index);
			if(freq != null){
				return freq;
			} else {
				return -1;
			}
		}else {
			return -1;
		}
		 	
	}
	
	@Programmatic 
	public Map<String, Integer> getWordFrequencyMap(){
		Map<String, Integer> wordFreqMap = new HashMap<String, Integer>();
		for(String token: this.textWordsList){
			int index = textWordsList.indexOf(token);
			Integer frequency = wordFrequencyList.get(index);
			wordFreqMap.put(token, frequency);
			
		}
		return wordFreqMap;
	}
	
	public void setWordFrequencyMap(Map<String,Integer> freqMap){
		for(String key: freqMap.keySet()){
			this.textWordsList.add(key);
			this.wordFrequencyList.set(this.textWordsList.indexOf(key), freqMap.get(key));
		}
	}

	@Programmatic
	public void populateWordFrequenciesFromTextContent(TextContent txtContent){
		Map<String,Integer> wordFrequencies = txtContent.getStringTokens();
		for(String key : wordFrequencies.keySet()){
			int val = wordFrequencies.get(key);
			this.putTokenToMap(key, val);
		}
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<String> getTextWordsList() {
		return textWordsList;
	}


	public void setTextWordsList(List<String> textWordsList) {
		this.textWordsList = textWordsList;
	}


	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Integer> getWordFrequencyList() {
		return wordFrequencyList;
	}


	public void setWordFrequencyList(List<Integer> wordFrequencyList) {
		this.wordFrequencyList = wordFrequencyList;
	}

	
}
