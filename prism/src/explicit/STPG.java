//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.*;
import java.io.*;

import prism.ModelType;
import prism.PrismException;
import prism.PrismUtils;

/**
 * Explicit representation of stochastic two-player game (STPG)
 */
public class STPG extends Model
{
	// Model type
	public static ModelType modelType = ModelType.STPG;

	// Transition function (Steps)
	protected List<ArrayList<DistributionSet>> trans;

	// Rewards
	protected List<List<Double>> transRewards;
	protected Double transRewardsConstant;

	// Flag: allow dupes in distribution sets?
	public boolean allowDupes = false;

	// Other statistics
	protected int numDistrSets;
	protected int numDistrs;
	protected int numTransitions;
	protected int maxNumDistrSets;
	protected int maxNumDistrs;

	/**
	 * Constructor: empty STPG.
	 */
	public STPG()
	{
		initialise(0);
	}

	/**
	 * Constructor: new STPG with fixed number of states.
	 */
	public STPG(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Constructor: build an STPG from an MDP
	 */
	public STPG(MDP m)
	{
		DistributionSet set;
		int i;
		initialise(m.numStates);
		for (i = 0; i < numStates; i++) {
			set = newDistributionSet(null);
			set.addAll(m.getChoices(i));
			addDistributionSet(i, set);
		}
	}

	/**
	 * Initialise: new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrSets = numDistrs = numTransitions = 0;
		maxNumDistrSets = maxNumDistrs = 0;
		trans = new ArrayList<ArrayList<DistributionSet>>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new ArrayList<DistributionSet>());
		}
		clearAllRewards();
	}

	/**
	 * Clear all information for a state (i.e. remove all transitions).
	 */
	public void clearState(int i)
	{
		// Do nothing if state does not exist
		if (i >= numStates || i < 0)
			return;
		// Clear data structures and update stats
		List<DistributionSet> list = trans.get(i);
		numDistrSets -= list.size();
		for (DistributionSet set : list) {
			numDistrs -= set.size();
			for (Distribution distr : set)
				numTransitions -= distr.size();
		}
		//TODO: recompute maxNumDistrSets
		//TODO: recompute maxNumDistrs
		// Remove all distribution sets
		trans.set(i, new ArrayList<DistributionSet>(0));
	}

	/**
	 * Add a new state and return its index.
	 */
	public int addState()
	{
		addStates(1);
		return numStates - 1;
	}

	/**
	 * Add multiple new states.
	 */
	public void addStates(int numToAdd)
	{
		for (int i = 0; i < numToAdd; i++) {
			trans.add(new ArrayList<DistributionSet>());
		}
		numStates += numToAdd;
	}

	/**
	 * Creates a new distribution set suitable for passing to addDistributionSet(...)
	 * i.e. a data structure consistent with the internals of the this class.
	 * Action label (any Object type) must be specified.
	 */
	public DistributionSet newDistributionSet(Object action)
	{
		return new DistributionSet(action);
	}

	/**
	 * Add distribution set 'newSet' to state s (which must exist).
	 * Distribution set is only actually added if it does not already exists for state s.
	 * (Assuming 'allowDupes' flag is not enabled.)
	 * Returns the index of the (existing or newly added) set.
	 * Returns -1 in case of error.
	 */
	public int addDistributionSet(int s, DistributionSet newSet)
	{
		ArrayList<DistributionSet> set;
		// Check state exists
		if (s >= numStates || s < 0)
			return -1;
		// Add distribution set (if new)
		set = trans.get(s);
		if (!allowDupes) {
			int i = set.indexOf(newSet);
			if (i != -1)
				return i;
		}
		set.add(newSet);
		// Update stats
		numDistrSets++;
		maxNumDistrSets = Math.max(maxNumDistrSets, set.size());
		numDistrs += newSet.size();
		maxNumDistrs = Math.max(maxNumDistrs, newSet.size());
		for (Distribution distr : newSet)
			numTransitions += distr.size();
		return set.size() - 1;
	}

	/**
	 * Remove all rewards from the model
	 */
	public void clearAllRewards()
	{
		transRewards = null;
		transRewardsConstant = null;
	}

	/**
	 * Set a constant reward for all transitions
	 */
	public void setConstantTransitionReward(double r)
	{
		// This replaces any other reward definitions
		transRewards = null;
		// Store as a Double (because we use null to check for its existence)
		transRewardsConstant = new Double(r);
	}

	/**
	 * Set the reward for choice i in some state s to r.
	 */
	public void setTransitionReward(int s, int i, double r)
	{
		// This would replace any constant reward definition, if it existed
		transRewardsConstant = null;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<List<Double>>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(null);
		}
		// If no rewards for state i yet, create list
		if (transRewards.get(s) == null) {
			int n = trans.get(s).size();
			List<Double> list = new ArrayList<Double>(n);
			for (int j = 0; j < n; j++) {
				list.add(0.0);
			}
			transRewards.set(s, list);
		}
		// Set reward
		transRewards.get(s).set(i, r);
	}

	/**
	 * Get the number of nondeterministic (player 1) choices in state s.
	 */
	public int getNumChoices(int s)
	{
		return trans.get(s).size();
	}

	/**
	 * Get the list of choices (distribution sets) for state s.
	 */
	public List<DistributionSet> getChoices(int s)
	{
		return trans.get(s);
	}
	
	/**
	 * Get the ith choice (distribution set) for state s.
	 */
	public DistributionSet getChoice(int s, int i)
	{
		return trans.get(s).get(i);
	}
	
	/**
	 * Get the transition reward (if any) for choice i of state s.
	 */
	public double getTransitionReward(int s, int i)
	{
		List<Double> list;
		if (transRewardsConstant != null)
			return transRewardsConstant;
		if (transRewards == null || (list = transRewards.get(s)) == null)
			return 0.0;
		return list.get(i);
	}

	/**
	 * Returns true if state s2 is a successor of state s1.
	 */
	public boolean isSuccessor(int s1, int s2)
	{
		for (DistributionSet distrs : trans.get(s1)) {
			for (Distribution distr : distrs) {
				if (distr.contains(s2))
					return true;
			}
		}
		return false;
	}

	/**
	 * Check if all the successors states of a state are in a set.
	 * @param s: The state to check
	 * @param set: The set to test for inclusion
	 */
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		for (DistributionSet distrs : trans.get(s)) {
			for (Distribution distr : distrs) {
				if (!distr.isSubsetOf(set))
					return false;
			}
		}
		return true;
	}

	/**
	 * Get the total number of player 1 choices (distribution sets) over all states.
	 */
	public int getNumP1Choices()
	{
		return numDistrSets;
	}
	
	/**
	 * Get the total number of player 2 choices (distributions) over all states.
	 */
	public int getNumP2Choices()
	{
		return numDistrs;
	}
	
	/**
	 * Get the total number of transitions in the model.
	 */
	public int getNumTransitions()
	{
		return numTransitions;
	}
	
	/**
	 * Get the maximum number of player 1 choices (distribution sets) in any state.
	 */
	public int getMaxNumP1Choices()
	{
		// TODO: Recompute if necessary
		return maxNumDistrSets;
	}
	
	/**
	 * Get the maximum number of player 2 choices (distributions) in any state.
	 */
	public int getMaxNumP2Choices()
	{
		// TODO: Recompute if necessary
		return maxNumDistrs;
	}
	
	/**
	 * Checks for deadlocks (states with no choices) and throws an exception if any exist.
	 * States in 'except' (If non-null) are excluded from the check.
	 */
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("STPG has a deadlock in state " + i);
		}
		// TODO: Check for empty distributions sets too?
	}

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 * [Not supported for STPGs]
	 */
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new PrismException("Building from PRISM explicit output not supported for STPGs");
	}

	/**
	 * Do a matrix-vector multiplication followed by two min/max ops, i.e. one step of value iteration.
	 * @param vect: Vector to multiply by
	 * @param min1: Min or max for player 1 (true=min, false=max)
	 * @param min2: Min or max for player 2 (true=min, false=max)
	 * @param result: Vector to store result in
	 * @param subset: Only do multiplication for these rows
	 * @param complement: If true, 'subset' is taken to be its complement
	 */
	public void mvMultMinMax(double vect[], boolean min1, boolean min2, double result[], BitSet subset,
			boolean complement)
	{
		int s = -1;
		while (s < numStates) {
			// Pick next state
			s = (subset == null) ? s + 1 : complement ? subset.nextClearBit(s + 1) : subset.nextSetBit(s + 1);
			if (s < 0)
				break;
			// Do operation
			result[s] = mvMultMinMaxSingle(s, vect, min1, min2);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication followed by two min/max ops.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min1: Min or max for player 1 (true=min, false=max)
	 * @param min2: Min or max for player 2 (true=min, false=max)
	 */
	public double mvMultMinMaxSingle(int s, double vect[], boolean min1, boolean min2)
	{
		int k;
		double d, prob, minmax1, minmax2;
		boolean first1, first2;
		ArrayList<DistributionSet> step;

		minmax1 = 0;
		first1 = true;
		step = trans.get(s);
		for (DistributionSet distrs : step) {
			minmax2 = 0;
			first2 = true;
			for (Distribution distr : distrs) {
				// Compute sum for this distribution
				d = 0.0;
				for (Map.Entry<Integer, Double> e : distr) {
					k = (Integer) e.getKey();
					prob = (Double) e.getValue();
					d += prob * vect[k];
				}
				// Check whether we have exceeded min/max so far
				if (first2 || (min2 && d < minmax2) || (!min2 && d > minmax2))
					minmax2 = d;
				first2 = false;
			}
			// Check whether we have exceeded min/max so far
			if (first1 || (min1 && minmax2 < minmax1) || (!min1 && minmax2 > minmax1))
				minmax1 = minmax2;
			first1 = false;
		}

		return minmax1;
	}

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min: Min or max for player 1 (true=min, false=max)
	 * @param min: Min or max for player 2 (true=min, false=max)
	 * @param val: Min or max value to match
	 */
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min1, boolean min2, double val)
	{
		int j, k;
		double d, prob, minmax2;
		boolean first2;
		List<Integer> res;
		ArrayList<DistributionSet> step;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		j = -1;
		step = trans.get(s);
		for (DistributionSet distrs : step) {
			j++;
			minmax2 = 0;
			first2 = true;
			for (Distribution distr : distrs) {
				// Compute sum for this distribution
				d = 0.0;
				for (Map.Entry<Integer, Double> e : distr) {
					k = (Integer) e.getKey();
					prob = (Double) e.getValue();
					d += prob * vect[k];
				}
				// Check whether we have exceeded min/max so far
				if (first2 || (min2 && d < minmax2) || (!min2 && d > minmax2))
					minmax2 = d;
				first2 = false;
			}
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, minmax2, 1e-12, false)) {
				res.add(j);
				//res.add(distrs.getAction());
			}
		}

		return res;
	}

	/**
	 * Export to a dot file.
	 */
	public void exportToDotFile(String filename) throws PrismException
	{
		exportToDotFile(filename, null);
	}

	/**
	 * Export to explicit format readable by PRISM (i.e. a .tra file, etc.).
	 */
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	/**
	 * Export to a dot file, highlighting states in 'mark'.
	 */
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		int i, j, k;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + modelType + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				j = -1;
				for (DistributionSet distrs : trans.get(i)) {
					j++;
					k = -1;
					for (Distribution distr : distrs) {
						k++;
						for (Map.Entry<Integer, Double> e : distr) {
							out.write(i + " -> " + e.getKey() + " [ label=\"");
							out.write(j + "," + k + ":" + e.getValue() + "\" ];\n");
						}
					}
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write " + modelType + " to file \"" + filename + "\"" + e);
		}
	}

	/**
	 * Get string with model info/stats.
	 */
	public String infoString()
	{
		String s = "";
		s += numStates + " states";
		s += ", " + numDistrSets + " distribution sets";
		s += ", " + numDistrs + " distributions";
		s += ", " + numTransitions + " transitions";
		s += ", p1max/avg = " + maxNumDistrSets + "/" + PrismUtils.formatDouble2dp(((double) numDistrSets) / numStates);
		s += ", p2max/avg = " + maxNumDistrs + "/" + PrismUtils.formatDouble2dp(((double) numDistrs) / numDistrSets);
		return s;
	}

	/**
	 * Get transition function as string.
	 */
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		first = true;
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + trans.get(i);
		}
		s += " ]";
		return s;
	}

	/**
	 * Equality check.
	 */
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof STPG))
			return false;
		STPG stpg = (STPG) o;
		if (numStates != stpg.numStates)
			return false;
		if (!initialStates.equals(stpg.initialStates))
			return false;
		if (!trans.equals(stpg.trans))
			return false;
		return true;
	}
}
