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
import java.util.Random;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.Iterator;
import cern.colt.Arrays;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import explicit.rewards.StateRewardsSimple;
import explicit.rewards.WeightedSumMDPRewards;
import prism.Accuracy;
import prism.AccuracyFactory;
import prism.Pair;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismUtils;
import cern.colt.Arrays;

public class AlphaMatrix {
	private double [][] matrix;
	private int action = -1;
	//private int [] actions;
	private int originU;
	private int originW;
	
	// attributes used for construction of policy graph
	private int index = -1;              // index of the vector that was used to create the backprojection
	private int obs = -1;                // observation that was used to create this backprojection
	private int[] obsSource = null;      // after cross-summing, this array contains an index of a previous-stage vector for each observation
	
	
	public AlphaMatrix (double [][] matrix) {
		this.matrix = matrix;
	}
	

	public void setMatrix(double [][] matrix) {
		this.matrix = matrix;
	}
	
	public double [][] getMatrix(){
		return matrix;
	}
	
	public double[] getValues(int i) {
		double [] values = new double [matrix[0].length];
		for (int obj=0; obj<matrix[0].length; obj++) {
			values[obj] = matrix[i][obj];
		}
		return values;
	}
	public int getNumStates() {
		return matrix.length;
	}
	
	public int getNumObjectives() {
		return matrix[0].length;
	}
	
	public AlphaMatrix clone() {
		AlphaMatrix B = new AlphaMatrix(matrix) ;
		B.setAction(action);
		return B;
	}
	/**
	 * Compute sum of two matrices
	 * @param v1 matrix 1
	 * @param v2 matrix 2
	 * @return sum of the matrices
	 */
	public static AlphaMatrix sumMatrices(AlphaMatrix v1, AlphaMatrix v2) {

		double[][] newMatrix = new double[v1.getMatrix().length][v1.getMatrix()[0].length];
		
		for (int i=0; i<v1.getMatrix().length; i++) {
			for (int j=0; j<v1.getMatrix()[0].length; j++) {
				newMatrix[i][j]= v1.getMatrix()[i][j]+v2.getMatrix()[i][j];
			}	
		}
		AlphaMatrix newAlphaMatrix = new AlphaMatrix(newMatrix);
		newAlphaMatrix.setAction(v1.getAction());
		
		return newAlphaMatrix;
	}
	
	//get the value of MOMDP: v= belief *AlhpaMatrix * weights
	public double value (Belief b, double [] weights, POMDP pomdp) {
		ArrayList <Object> allActions = new ArrayList<Object> ();
		for (int s =0; s<pomdp.getNumStates();s++) {
			List <Object> availableActionsForState = pomdp.getAvailableActions(s);

			for (Object a: availableActionsForState) {
				if (!allActions.contains(a) & a!= null) {
					allActions.add(a);
				}
			}
		}
		
		ArrayList <Object> availableActionsForBelief = new ArrayList<Object> ();
		for (int s =0; s<pomdp.getNumStates();s++) {
			if ((b.toDistributionOverStates(pomdp)[s])>0){
				List <Object> availableActionsForState = pomdp.getAvailableActions(s);
				for (Object a: availableActionsForState) {
					if (!availableActionsForBelief.contains(a)) {
						availableActionsForBelief.add(a);
					}
				}
			}
		}
		if (!availableActionsForBelief.contains(allActions.get(action))) {
			return Double.NEGATIVE_INFINITY;
		}
		
		double [] belief = b.toDistributionOverStates(pomdp);
		double value = 0.0;
		for (int i=0; i<belief.length; i++) {
			for (int j=0; j< weights.length; j++) {
				value += belief[i] * matrix[i][j] * weights[j];
			}
		}
		return value;
	}
	
	
	// is AlphaMatrix b in List A
	public static boolean contains(ArrayList<AlphaMatrix> A, AlphaMatrix b) {
		for (int i=0; i<A.size();i++) {
			AlphaMatrix u = A.get(i);
			if ( u.getAction()==b.getAction() & u.getMatrix().equals(b.getMatrix()) ) {
				return true;
			}  
		}
		return false;
	}

	
	public static double getMaxValue (Belief belief, ArrayList<AlphaMatrix> U, double weights[], POMDP pomdp) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i=0; i<U.size(); i++) {
			AlphaMatrix u = U.get(i);
			double value = u.value(belief, weights, pomdp);
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	
	public static int getMaxValueIndex (Belief belief, ArrayList<AlphaMatrix> U, double weights[], POMDP pomdp) {
		double max = Double.NEGATIVE_INFINITY;
		int index =-1;
		for (int i=0; i<U.size(); i++) {
			AlphaMatrix u = U.get(i);
			double value = u.value(belief, weights, pomdp);
			if (value > max) {
				max = value;
				index =i;
			}
		}
		return index;
	}
	
	/**
	 * Get string representation of this vector
	 */
	public String toString() {
		String ret = "AlphaMatrix: action=" + action+"\n";
			for (int j=0; j<matrix[0].length; j++) {
				ret += "(AlphaVector "+ j+")";
				for (int i=0; i<matrix.length; i++) {
					ret += " "+ matrix[i][j]+",";
				}
				ret +="\n";
			}
		return ret;
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
}
