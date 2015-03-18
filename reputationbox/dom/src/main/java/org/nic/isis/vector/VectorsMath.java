package org.nic.isis.vector;

public class VectorsMath {

	/**
	 * Math function to add (vector2 * frequency) to vector1
	 * 
	 * @param vector1
	 * @param vector2
	 * @param frequency : to multiply vector2 by frequency and add to vector1
	 * @return
	 */
	public static double[] addArrays(double[] vector1, double[] vector2, double frequency) {
		if (vector2.length != vector1.length)
			throw new IllegalArgumentException(
					"int arrays of different sizes cannot be added");

		int length = vector2.length;
		for (int i = 0; i < length; i++) {
			double value = (vector2[i] * frequency) + vector1[i];
			vector1[i] = value;
		}
		return vector1;
	}
	
	/**
	 * Math function to add (vector2 to vector1)
	 * 
	 * @param vector1
	 * @param vector2
	 * @return
	 */
	public static double[] addArrays(double[] vector1, double[] vector2) {
		if (vector2.length != vector1.length)
			throw new IllegalArgumentException(
					"int arrays of different sizes cannot be added");

		int length = vector2.length;
		for (int i = 0; i < length; i++) {
			double value = vector2[i] + vector1[i];
			vector1[i] = value;
		}
		return vector1;
	}
	
	
	/**
	 * Math function to add vector2 to vector1
	 * 
	 * @param vector1
	 * @param vector2
	 * @param frequency
	 * @return
	 */
	public static double[] devideArray(double[] vector, int divisionFactor) {
		int length = vector.length;
		double[] resultVector = new double[length];
		for (int i = 0; i < length; i++) {
			double value = vector[i] / divisionFactor;
			resultVector[i] = value;
		}
		return resultVector;
	}
	
	public static double getSquaredDistance(double[] v1, double[] v2){
		int length = v1.length;
		double sum = 0;
		if(v1.length == v2.length){
			for(int i=0; i < length; i++){
				double distance = v1[i]-v2[i];
				double squaredDistance = distance * distance;
				sum += squaredDistance;
			}
		} else {
			throw new IllegalArgumentException(
					"vectors of different sizes cannot be used to get sum of squared value");
		}
		return sum;
	}
	
	public static double getDistance(double[] v1, double[] v2){
		int length = v1.length;
		double sum = 0;
		if(v1.length == v2.length){
			for(int i=0; i < length; i++){
				double distance = v1[i]-v2[i];
				double squaredDistance = distance * distance;
				sum += squaredDistance;
			}
		} else {
			throw new IllegalArgumentException(
					"vectors of different sizes cannot be used to get sum of squared value");
		}
		return Math.sqrt(sum);
	}

	/**
	 * add v2 to v1 and devide v1 by 2 to get the average vector from v1,v2
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double[] getMergedVector(double[] v1, double[] v2){
		v1 = VectorsMath.addArrays(v1, v2);
		v1 = VectorsMath.devideArray(v1, 2);
		return v1;
	}
}
