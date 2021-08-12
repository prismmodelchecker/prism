/*******************************************************************************
 * SolvePOMDP
 * Copyright (C) 2017 Erwin Walraven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package explicit;

import java.util.ArrayList;

import cern.colt.Arrays;

public class AlphaVector {
	private double[] entries;
	private int originU;
	private int originW;
	private int action = -1;             // the action associated with this vector
	
	// attributes used for construction of policy graph
	private int index = -1;              // index of the vector that was used to create the backprojection
	private int obs = -1;                // observation that was used to create this backprojection
	private int[] obsSource = null;      // after cross-summing, this array contains an index of a previous-stage vector for each observation
	
	public AlphaVector(double[] entries) {
		this.entries = entries;
	}
	
	/**
	 * Get entry i of the vector
	 * @param i index
	 * @return entry i
	 */
	public double getEntry(int i) {
		assert i < entries.length;
		return entries[i];
	}
	
	/**
	 * Get all entries of the vector
	 * @return vector entries
	 */
	public double[] getEntries() {
		return entries;
	}
	
	/**
	 * Replaces entries in this vector
	 * @param entries
	 */
	public void setEntries(double[] entries) {
		this.entries = entries;
	}
	
	/**
	 * Get minimum of the entries in this vector
	 * @return max value
	 */
	public double getMinValue() {
		double minValue = Double.POSITIVE_INFINITY;
		
		for(int i=0; i<entries.length; i++) {
			if(entries[i] < minValue) {
				minValue = entries[i];
			}
		}
		
		return minValue;
	}
	
	/**
	 * Get average of the entries in this vector
	 * @return average
	 */
	public double getAverageValue() {
		double sumValues = 0.0;
		
		for(int i=0; i<entries.length; i++) {
			sumValues += entries[i];
		}
		
		return sumValues / ((double) entries.length);
	}
	
	/**
	 * Get number of entries in the vecot
	 * @return number of entries
	 */
	public int size() {
		return entries.length;
	}
	
	/**
	 * Get vector u from U and w from W that were used to compute this vector when computing a cross sum
	 * @param u index of u
	 * @param w index of w
	 */
	public void setOrigin(int u, int w) {
		originU = u;
		originW = w;
	}
	
	/**
	 * Get vector u from U that was used to create this vector
	 * @return index of u
	 */
	public int getOriginU() {
		return originU;
	}
	
	/**
	 * Get vector w from W that was used to create this vector
	 * @return index of w
	 */
	public int getOriginW() {
		return originW;
	}
	
	/**
	 * Get the observation which was used to create this vector when computing the backprojection
	 * @return obs source
	 */
	public int getObs() {
		return obs;
	}
	
	/**
	 * Set the observation which was used to create this vector when computing the backprojection
	 * @param o observation
	 */
	public void setObs(int o) {
		this.obs = o;
	}
	
	/**
	 * Get the array containing a source vector for each observation
	 * @return array containing sources
	 */
	public int[] getObsSource() {
		return obsSource;
	}
	
	/**
	 * Initialize the vector source array
	 * @param numObservations number of observations
	 */
	public void initObsSource(int numObservations) {
		this.obsSource = new int[numObservations];
		for(int o=0; o<numObservations; o++) {
			this.obsSource[o] = -1;
		}
	}
	
	/**
	 * Set the source vector for a specific observation
	 * @param o observation ID
	 * @param vectorID vector ID
	 */
	public void setObsSource(int o, int vectorID) {
		assert this.obsSource != null;
		this.obsSource[o] = vectorID;
	}
	
	/**
	 * Get action associated with this vector
	 * @return action
	 */
	public int getAction() {
		return action;
	}
	
	/**
	 * Set action associated with this vector
	 * @param a action
	 */
	public void setAction(int a) {
		action = a;
	}
	
	/**
	 * Set the index of this vector. It is used in RBIP to define collection indices using original vector indices
	 * @param i index
	 */
	public void setIndex(int i) {
		index = i;
	}
	
	/**
	 * Get the index of this vector
	 * @return index
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Compute dot product of this vector and elements of v
	 * @param v vector v
	 * @return dot product
	 */
	public double getDotProduct(double[] v) {
		assert entries != null;
		assert v != null;
		assert entries.length == v.length;
		double dp = 0.0;
		
		for(int i=0; i<entries.length; i++) {
			dp += entries[i] * v[i];
		}
		
		return dp;
	}
	
	/**
	 * Returns true if the vector is pointwise dominated by at least one vector in U
	 * @param U vector set U
	 * @return true iff this vector is pointwise dominated
	 */
	public boolean isPointwiseDominated(ArrayList<AlphaVector> U) {		
		for(AlphaVector u : U) {
			double[] uEntries = u.getEntries();
			
			boolean dominated = true;
			
			for(int i=0; i<uEntries.length; i++) {
				dominated = dominated && (entries[i] <= uEntries[i]);
				
				if(i < uEntries.length-1 && !dominated) {
					// w has an entry that is larger than entry in u, so skip the remainder of u
					break;
				}
			}
			
			if(dominated) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns true iff entries in other vectors are identical
	 * @param otherVector vector to compare with
	 * @return true iff identical
	 */
	public boolean equals(AlphaVector otherVector) {
		if(otherVector.size() != size()) {
			return false;
		}
		else {
			boolean retValue = true;
			
			for(int i=0; i<size()&&retValue; i++) {
				retValue = retValue && (entries[i]==otherVector.getEntry(i));
			}
			
			return retValue;
		}
	}
	
	/**
	 * Get string representation of this vector
	 */
	public String toString() {
		String ret = "<AlphaVector(";
		
		for(int i=0; i<entries.length; i++) {
			ret += entries[i]+" ";
		}
		
		return ret+")>";
	}
	
	/**
	 * Compute cross sum of two vector sets
	 * @param l1 vector set 1
	 * @param l2 vector set 2
	 * @return cross sum of two vector sets
	 */
	public static ArrayList<AlphaVector> crossSum(ArrayList<AlphaVector> l1, ArrayList<AlphaVector> l2) {
		ArrayList<AlphaVector> crossList = new ArrayList<AlphaVector>();
		
		for(int i=0; i<l1.size(); i++) {
			for(int j=0; j<l2.size(); j++) {
				AlphaVector sumVector = AlphaVector.sumVectors(l1.get(i), l2.get(j));
				sumVector.setOrigin(i, j);
				crossList.add(sumVector);
			}
		}
		
		return crossList;
	}
	
	/**
	 * Compute cross sum of two vector sets and keep track of policy graph info
	 * @param l1 vector set 1
	 * @param l2 vector set 2
	 * @return cross sum of two vector sets
	 */
	public static ArrayList<AlphaVector> crossSumPolicyGraph(ArrayList<AlphaVector> l1, ArrayList<AlphaVector> l2, int numObservations) {
		ArrayList<AlphaVector> crossList = new ArrayList<AlphaVector>();
		
		for(int i=0; i<l1.size(); i++) {
			for(int j=0; j<l2.size(); j++) {
				AlphaVector firstVector = l1.get(i);
				AlphaVector secondVector = l2.get(j);
				
				AlphaVector sumVector = AlphaVector.sumVectors(firstVector, secondVector);
				sumVector.setOrigin(i, j);
				
				// set policy graph information
				sumVector.initObsSource(numObservations);
				
				if(firstVector.getObsSource() != null) {
					// firstVector is a partial cross sum, so we copy information
					for(int o=0; o<numObservations; o++) {
						if(firstVector.getObsSource()[o] != -1) {
							sumVector.setObsSource(o, firstVector.getObsSource()[o]);
						}
					}
				}
				else {
					// firstVector is a backprojection, so we set the vector source for the observation that was used
					int o = firstVector.getObs();
					int k = firstVector.getIndex();
					assert o != -1 && k != -1;
					sumVector.setObsSource(o, k);
				}

				if(secondVector.getObsSource() != null) {
					// secondVector is a partial cross sum, so we copy information
					for(int o=0; o<numObservations; o++) {
						if(secondVector.getObsSource()[o] != -1) {
							sumVector.setObsSource(o, secondVector.getObsSource()[o]);
						}
					}
				}
				else {
					// secondVector is a backprojection, so we set the vector source for the observation that was used
					int o = secondVector.getObs();
					int k = secondVector.getIndex();
					assert o != -1 && k != -1;
					sumVector.setObsSource(o, k);
				}
				
				crossList.add(sumVector);
			}
		}
		
		return crossList;
	}
	
	/**
	 * Compute cross sum of vector u and all vectors in W, and skip vector in W defined by the index
	 * @param u vector u
	 * @param W vector set W
	 * @param skipIndex vector in W to be skipped
	 * @return cross sum
	 */
	public static ArrayList<AlphaVector> crossSumRestricted(AlphaVector u, ArrayList<AlphaVector> W, int skipIndex) {
		ArrayList<AlphaVector> l1 = new ArrayList<AlphaVector>();
		l1.add(u);
		
		ArrayList<AlphaVector> crossList = new ArrayList<AlphaVector>();
		
		for(int i=0; i<l1.size(); i++) {
			for(int j=0; j<W.size(); j++) {
				if(j != skipIndex) {
					AlphaVector sumVector = AlphaVector.sumVectors(l1.get(i), W.get(j));
					sumVector.setOrigin(i, j);
					crossList.add(sumVector);
				}
			}
		}
		
		return crossList;
	}
	
	/**
	 * Compute sum of two vectors
	 * @param v1 vector 1
	 * @param v2 vector 2
	 * @return sum of the vectors
	 */
	public static AlphaVector sumVectors(AlphaVector v1, AlphaVector v2) {
		assert v1.size() == v2.size();
		double[] newEntries = new double[v1.size()];
		
		for(int s=0; s<newEntries.length; s++) {
			newEntries[s] = v1.getEntry(s) + v2.getEntry(s);
		}
		
		assert v1.getAction() == v2.getAction();
		int action = v1.getAction();
		
		AlphaVector newVector = new AlphaVector(newEntries);
		newVector.setAction(action);
		
		return newVector;
	}
	
	/**
	 * Check whether first vector is lexicographically greater than the second
	 * @param v1 first vector
	 * @param v2 second vector
	 * @return true iff v1 is lexicographically greater than v2
	 */
	private static boolean lexGreater(AlphaVector v1, AlphaVector v2) {
		assert v1.size() == v2.size();
		
		for(int i=0; i<v1.size(); i++) {
			double v1Entry = v1.getEntry(i);
			double v2Entry = v2.getEntry(i);
			
			if(v1Entry != v2Entry) {
				if(v1Entry > v2Entry) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Get index of the best vector in U at belief point b
	 * @param b belief b
	 * @param U vector set U
	 * @return index of the best vector in U at b
	 */
	public static int getBestVectorIndex(double[] b, ArrayList<AlphaVector> U) {
		

			double max = Double.NEGATIVE_INFINITY;
			int wIndex = -1;
			AlphaVector w = null;
			
			for(int i=0; i<U.size(); i++) {
				AlphaVector u = U.get(i);
				double product = u.getDotProduct(b);
				
				if(product > max) {
					wIndex = i;
					w = u;
					max = product;
				}
				else if(product == max && lexGreater(u, w)) {
					wIndex = i;
					w = u;
				}
			}
			return wIndex;

		
	}
	
	public static int getBestVectorIndexMinMax(double[] b, ArrayList<AlphaVector> U, boolean isMin) {
		if (isMin) {
			double min = Double.POSITIVE_INFINITY;
			int wIndex = -1;
			AlphaVector w = null;
			
			for(int i=0; i<U.size(); i++) {
				AlphaVector u = U.get(i);
				double product = u.getDotProduct(b);
				
				if(product < min) {
					wIndex = i;
					w = u;
					min = product;
				}
				else if(product == min && lexGreater(w, u)) {
					wIndex = i;
					w = u;
				}
			}
			return wIndex;
		}
		else {
			double max = Double.NEGATIVE_INFINITY;
			int wIndex = -1;
			AlphaVector w = null;
			
			for(int i=0; i<U.size(); i++) {
				AlphaVector u = U.get(i);
				double product = u.getDotProduct(b);
				
				if(product > max) {
					wIndex = i;
					w = u;
					max = product;
				}
				else if(product == max && lexGreater(u, w)) {
					wIndex = i;
					w = u;
				}
			}
			
			return wIndex;
		}
	}
	
	/**
	 * Get value of belief b in vector set U
	 * @param b belief b
	 * @param U vector set U
	 * @return the value
	 */
	public static double getValue(double[] b, ArrayList<AlphaVector> U) {
		double max = Double.NEGATIVE_INFINITY;
		
		for(int i=0; i<U.size(); i++) {
			AlphaVector u = U.get(i);
			double product = u.getDotProduct(b);
			
			if(product > max){
				max = product;
			}
		}
		
		return max;
	}
	public static double getValueMin(double[] b, ArrayList<AlphaVector> U) {
		double min = Double.POSITIVE_INFINITY;
		double  min2 = Double.POSITIVE_INFINITY;  
		
		for(int i=0; i<U.size(); i++) {

			
			AlphaVector u = U.get(i);

			double product = u.getDotProduct(b);

			if(product < min){
				min = product;
			}
			
			if (product<min2 && product >0) {
				min2 = product;
			}
		}
		//if (Double.isFinite(min2))
		//	return min2;
		return min;
	}
	
	public static double getValueMinMax(double[] b, ArrayList<AlphaVector> U, boolean isMin) {
		if (isMin) {
		double min = Double.POSITIVE_INFINITY;
		
		for(int i=0; i<U.size(); i++) {
			AlphaVector u = U.get(i);
			double product = u.getDotProduct(b);
			
			if(product < min){
				min = product;
			}
		}
		
			return min;
		}
		else {
			double max = Double.NEGATIVE_INFINITY;
			
			for(int i=0; i<U.size(); i++) {
				AlphaVector u = U.get(i);
				double product = u.getDotProduct(b);
				
				if(product > max){
					max = product;
				}
			}
			
			return max;
		}
	}
}



