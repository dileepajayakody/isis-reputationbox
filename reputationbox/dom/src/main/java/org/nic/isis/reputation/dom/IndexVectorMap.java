package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.ri.RandomIndexing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.vector.TernaryVector;

/**
 * Data Structure for IndexVectors for words
 * @author dileepa
 *
 */
@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("INDEXVECTORMAP")
public class IndexVectorMap {

	List<String> indexWords = new ArrayList<String>();
	List<TernaryVector> indexVectors = new ArrayList<TernaryVector>();

	private final static Logger logger = LoggerFactory
			.getLogger(IndexVectorMap.class);
	
	public IndexVectorMap(){
		
	}
	
	public synchronized void putIndexVector(String key, TernaryVector value) {
		int[] arrayVal = value.toArray();
		/*String valueStr = "<";
		for(int x : arrayVal){
			valueStr += ", " + x;
		}
		valueStr += ">";
		*/
		int[] posArray = value.positiveDimensions();
		int[] negArray = value.negativeDimensions();
		String posIndexes = "";
		for(int index :  posArray){
			posIndexes += index + ", ";
		}
		String negIndexes = "";
		for(int index :  negArray){
			negIndexes += index + ", ";
		}
		
		//logger.warn("putting indexVector for word : " + key + " pos Indexes : " + posIndexes + " neg Indexes : " + negIndexes);
	    if (!indexWords.contains(key)) {
	      indexWords.add(key);
	      indexVectors.add(value);
	    } else
	      indexVectors.set(indexWords.indexOf(key), value);
	  }
	
	 public TernaryVector getIndexVector(String key) {
		TernaryVector returnedVector = null; 
		if (!indexWords.contains(key)) {
			/*logger.warn("No index vector found for key : " + key
					+ " hence returning null");*/
			returnedVector = null;
		} else {
			returnedVector = indexVectors.get(indexWords.indexOf(key));
			int[] posArray = returnedVector.positiveDimensions();
			int[] negArray = returnedVector.negativeDimensions();
			String posIndexes = "";
			for (int index : posArray) {
				posIndexes += index + ", ";
			}
			String negIndexes = "";
			for (int index : negArray) {
				negIndexes += index + ", ";
			}
			//logger.warn("GETTING indexVector for word : " + key + " pos Indexes : " + posIndexes +  " neg Indexes : " + negIndexes);
		}
		
		/*int[] arrayVal = returnedVector.toArray();
		String valueStr = "<";
		for(int x : arrayVal){
			valueStr += ", " + x;
		}
		valueStr += ">";
		*/
		
		return returnedVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<String> getIndexWords() {
		return indexWords;
	}

	public void setIndexWords(List<String> indexWords) {
		this.indexWords = indexWords;
	}
	
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<TernaryVector> getIndexVectors() {
		return indexVectors;
	}

	public void setIndexVectors(List<TernaryVector> indexVectors) {
		this.indexVectors = indexVectors;
	}
	
	@Programmatic
	public Map<String, TernaryVector> getIndexVectorMap(){
		Map<String, TernaryVector> returned = new HashMap<String, TernaryVector>();
		for(String word : indexWords){
			returned.put(word, getIndexVector(word));
		}
		return returned;
	}
	
	@Programmatic
	public void setIndexVectorMap(Map<String, TernaryVector> indexVectors){
		
		for(String key : indexVectors.keySet()){
			this.putIndexVector(key, indexVectors.get(key));
		}
	}

	public String toString(){
		String ivString = " size of indexvectormap : " + indexWords.size() + "\n";
			
		for(String iword : indexWords) {
			TernaryVector iv = this.getIndexVector(iword);
			String posIndexes = "";
			String negIndexes = "";	
			
			int[] posArray = iv.positiveDimensions();
			int[] negArray = iv.negativeDimensions();
			for(int index :  posArray){
				posIndexes += index + ", ";
			}
			for(int index :  negArray){
				negIndexes += index + ", ";
			}
			ivString += " Index word : " + iword + "  posIndexes: " + posIndexes + " negIndexes : " + negIndexes + " \n";
		}
		return ivString;
		
	}
}
