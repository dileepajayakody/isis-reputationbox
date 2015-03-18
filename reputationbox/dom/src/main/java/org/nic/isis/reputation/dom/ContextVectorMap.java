package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;

import edu.ucla.sspace.vector.TernaryVector;


@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("CONTEXTVECTORMAP")
public class ContextVectorMap {

	List<String> contextWords = new ArrayList<String>();
	List<double[]> contextVectors = new ArrayList<double[]>();

	public ContextVectorMap(){
		
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
		if (!contextWords.contains(key))
			return null;
		else {
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
		for(String key : contxtVectors.keySet()){
			this.putContextVector(key, contxtVectors.get(key));
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
