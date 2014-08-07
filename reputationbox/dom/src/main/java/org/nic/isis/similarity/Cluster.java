package org.nic.isis.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nic.isis.ri.RandomIndexing;


public class Cluster {
  
  private String id;
  private Map<String,int[]> docs = new HashMap<String,int[]>();
  private List<String> docIds = new ArrayList<String>();
  
  private int[] centroid = null;
  
  public Cluster(String id) {
    this.id = id;
  }
  
  public String getId() {
    return id;
  }
  
  public Set<String> getDocumentIds() {
    return docs.keySet();
  }

  public String getDocumentId(int pos) {
    return docIds.get(pos);
  }
  
  public int[] getDocument(String documentName) {
    return docs.get(documentName);
  }

  public int[] getDocument(int pos) {
    return docs.get(docIds.get(pos));
  }
  
  public void addDocument(String docName, int[] docMatrix) {
    docs.put(docName, docMatrix);
    docIds.add(docName);
  }

  public void removeDocument(String docName) {
    docs.remove(docName);
    docIds.remove(docName);
  }

  public int size() {
    return docs.size();
  }
  
  public boolean contains(String docName) {
    return docs.containsKey(docName);
  }
  
  /**
   * Returns a document consisting of the average of the 
   * vectors of the documents in the cluster. 
   * 
   * @return the centroid of the cluster
   */
  public int[] getCentroid() {
    if (docs.size() == 0) {
      return null;
    }
    int[] d = docs.get(docIds.get(0));
    centroid = new int[d.length]; 
    for (String docName : docs.keySet()) {
      int[] docArray = docs.get(docName);
      centroid = RandomIndexing.addArrays(centroid, docArray);
    }
    centroid = RandomIndexing.devideArray(centroid, docs.size());
    return centroid;
  }


  
  /**
   * Returns the cosine similarity between the centroid of this cluster
   * and the new document.
   * @param doc the document to be compared for similarity.
   * @return the similarity of the centroid of the cluster to the document
   */
  public double getSimilarity(int[] doc) {
    if (centroid != null) {
    	double similarity = CosineSimilarity.calculateCosineSimilarity(centroid, doc);
    	return similarity;
    }
    return 0.0D;
  }
  
  @Override
  public String toString() {
    return id + ":" + docs.keySet().toString();
  }
}