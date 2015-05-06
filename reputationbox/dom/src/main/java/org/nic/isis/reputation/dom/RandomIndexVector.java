package org.nic.isis.reputation.dom;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;

import edu.ucla.sspace.vector.TernaryVector;

/**
 * Data Structure for IndexVectors for words
 * @author dileepa
 *
 */
@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("RANDOMINDEXVECTOR")
public class RandomIndexVector {

	private String word;
	//private TernaryVector indexVector;
	private int[] positiveIndexes;
	private int[] negativeIndexes;
	
	//private double[] contextVector;
	private int wordDocFrequency;
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
//	@javax.jdo.annotations.Persistent
//	@javax.jdo.annotations.Column(allowsNull = "true") 
//	public TernaryVector getIndexVector() {
//		return indexVector;
//	}
//	public void setIndexVector(TernaryVector indexVector) {
//		this.indexVector = indexVector;
//	}
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public int getWordDocFrequency() {
		return wordDocFrequency;
	}
	public void setWordDocFrequency(int wordDocFrequency) {
		this.wordDocFrequency = wordDocFrequency;
	
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public int[] getPositiveIndexes() {
		return positiveIndexes;
	}
	public void setPositiveIndexes(int[] positiveIndexes) {
		this.positiveIndexes = positiveIndexes;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public int[] getNegativeIndexes() {
		return negativeIndexes;
	}
	public void setNegativeIndexes(int[] negativeIndexes) {
		this.negativeIndexes = negativeIndexes;
	}
	
//	@javax.jdo.annotations.Persistent
//	@javax.jdo.annotations.Column(allowsNull = "true") 
//	public double[] getContextVector() {
//		return contextVector;
//	}
//	public void setContextVector(double[] contextVector) {
//		this.contextVector = contextVector;
//	}
	
}
