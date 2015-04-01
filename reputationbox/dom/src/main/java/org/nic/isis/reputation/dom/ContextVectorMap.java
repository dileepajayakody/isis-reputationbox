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

import edu.ucla.sspace.vector.TernaryVector;

public class ContextVectorMap {

	private List<String> contextWords;
	private List<double[]> contextVectors;
	private final static Logger logger = LoggerFactory
			.getLogger(ContextVectorMap.class);
	
	public ContextVectorMap(){
		contextWords = new ArrayList<String>();
		contextVectors = new ArrayList<double[]>();
	}
	
	@Programmatic
	public synchronized void putContextVector(String key, double[] value) {
	    
		if (!contextWords.contains(key)) {
	      contextWords.add(key);
	      contextVectors.add(value);
	    } else
	      contextVectors.set(contextWords.indexOf(key), value);
	  }
	
	 public double[] getContextVector(String key) {
		if (!contextWords.contains(key)){
			logger.warn("No context vector found for key : " + key
					+ " hence returning null; VERY ODD");
			return null;
		}else {
			return contextVectors.get(contextWords.indexOf(key));
		}
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<String> getContextWords() {
		return contextWords;
	}

	public void setContextWords(List<String> contextWords) {
		this.contextWords = contextWords;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<double[]> getContextVectors() {
		return contextVectors;
	}

	public void setContextVectors(List<double[]> contextVectors) {
		this.contextVectors = contextVectors;
	}
	
	@Programmatic
	public Map<String, double[]> getContextVectorMap(){
		Map<String, double[]> contextVectorMap = new HashMap<String, double[]>();
		for(String word : contextWords){
			contextVectorMap.put(word, getContextVector(word));
		}
		return contextVectorMap;
	}
	
	@Programmatic
	public void setContextVectorMap(Map<String, double[]> contxtVectors){
		contextWords = new ArrayList<String>();
	    contextVectors = new ArrayList<double[]>();
		for(String key : contxtVectors.keySet()){
			//this.putContextVector(key, contxtVectors.get(key));
			contextWords.add(key);
			int ind = contextWords.indexOf(key);
			if(contextVectors.size() <= ind){
				 while(contextVectors.size() <= (ind + 1)){
					 contextVectors.add(null);
		    	  }
			}
			contextVectors.set(ind,contxtVectors.get(key));
		}
	}


	public String toString(){
		String ivString = "size of contextWords Map : " + contextVectors.size() + "\n";
		for(String iword : contextWords) {
			String cvString = "";
			double [] cv = this.getContextVector(iword);	
			for(double x :  cv){
				cvString += x + ", ";
			}
			ivString += " Context word : " + iword + " vectorString: " + cvString + " \n";
		}
		return ivString;
		
	}
}
