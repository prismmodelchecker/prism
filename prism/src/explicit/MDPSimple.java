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
 * Simple explicit-state representation of an MDP.
 */
public class MDPSimple extends ModelSimple implements MDP
{
	// Transition function (Steps)
	protected List<List<Distribution>> trans;

	// Action labels
	// (null in element s means no actions for that state)
	protected List<List<Object>> actions;

	// Rewards
	// (if transRewardsConstant non-null, use this for all transitions; otherwise, use transRewards list)
	// (for transRewards, null in element s means no rewards for that state)
	protected Double transRewardsConstant;
	protected List<List<Double>> transRewards;

	// Flag: allow duplicates in distribution sets?
	protected boolean allowDupes = false;

	// Other statistics
	protected int numDistrs;
	protected int numTransitions;
	protected int maxNumDistrs;
	protected boolean maxNumDistrsOk;

	// Constructors

	/**
	 * Constructor: empty MDP.
	 */
	public MDPSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new MDP with fixed number of states.
	 */
	public MDPSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public MDPSimple(MDPSimple mdp)
	{
		this(mdp.numStates);
		for (int in : mdp.getInitialStates()) {
			addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			for (Distribution distr : mdp.trans.get(i)) {
				addChoice(i, new Distribution(distr));
			}
		}
		// TODO: copy actions, rewards
	}

	/**
	 * Constructor: new MDP copied from an existing DTMC.
	 */
	public MDPSimple(DTMCSimple dtmc)
	{
		this(dtmc.getNumStates());
		for (int s : dtmc.getInitialStates()) {
			addInitialState(s);
		}
		for (int s = 0; s < numStates; s++) {
			addChoice(s, new Distribution(dtmc.getTransitions(s)));
		}
		// TODO: copy rewards, etc.
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrs = numTransitions = maxNumDistrs = 0;
		maxNumDistrsOk = true;
		trans = new ArrayList<List<Distribution>>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new ArrayList<Distribution>());
		}
		actions = null;
		clearAllRewards();
	}

	@Override
	public void clearState(int s)
	{
		// Do nothing if state does not exist
		if (s >= numStates || s < 0)
			return;
		// Clear data structures and update stats
		List<Distribution> list = trans.get(s);
		numDistrs -= list.size();
		for (Distribution distr : list) {
			numTransitions -= distr.size();
		}
		maxNumDistrsOk = false;
		trans.get(s).clear();
		if (actions != null && actions.get(s) != null)
			actions.get(s).clear();
		if (transRewards != null && transRewards.get(s) != null)
			transRewards.get(s).clear();
	}

	@Override
	public int addState()
	{
		addStates(1);
		return numStates - 1;
	}

	@Override
	public void addStates(int numToAdd)
	{
		for (int i = 0; i < numToAdd; i++) {
			trans.add(new ArrayList<Distribution>());
			if (actions != null)
				actions.add(null);
			if (transRewards != null)
				transRewards.add(null);
			numStates++;
		}
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		BufferedReader in;
		Distribution distr;
		String s, ss[];
		int i, j, k, iLast, kLast, n, lineNum = 0;
		double prob;

		try {
			// Open file
			in = new BufferedReader(new FileReader(new File(filename)));
			// Parse first line to get num states
			s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("Missing first line of .tra file");
			ss = s.split(" ");
			n = Integer.parseInt(ss[0]);
			// Initialise
			initialise(n);
			// Go though list of transitions in file
			iLast = -1;
			kLast = -1;
			distr = null;
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					ss = s.split(" ");
					i = Integer.parseInt(ss[0]);
					k = Integer.parseInt(ss[1]);
					j = Integer.parseInt(ss[2]);
					prob = Double.parseDouble(ss[3]);
					// For a new state or distribution
					if (i != iLast || k != kLast) {
						// Add any previous distribution to the last state, create new one
						if (distr != null) {
							addChoice(iLast, distr);
						}
						distr = new Distribution();
					}
					// Add transition to the current distribution
					distr.add(j, prob);
					// Prepare for next iter
					iLast = i;
					kLast = k;
				}
				s = in.readLine();
				lineNum++;
			}
			// Add previous distribution to the last state
			addChoice(iLast, distr);
			// Close file
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
		// Set initial state (assume 0)
		initialStates.add(0);
	}

	// Mutators (other)

	/**
	 * Add a choice (distribution 'distr') to state s (which must exist).
	 * Distribution is only actually added if it does not already exists for state s.
	 * (Assuming 'allowDupes' flag is not enabled.)
	 * Returns the index of the (existing or newly added) distribution.
	 * Returns -1 in case of error.
	 */
	public int addChoice(int s, Distribution distr)
	{
		List<Distribution> set;
		// Check state exists
		if (s >= numStates || s < 0)
			return -1;
		// Add distribution (if new)
		set = trans.get(s);
		if (!allowDupes) {
			int i = set.indexOf(distr);
			if (i != -1)
				return i;
		}
		set.add(distr);
		// Add null action if necessary
		if (actions != null && actions.get(s) != null)
			actions.get(s).add(null);
		// Add zero reward if necessary
		if (transRewards != null && transRewards.get(s) != null)
			transRewards.get(s).add(0.0);
		// Update stats
		numDistrs++;
		maxNumDistrs = Math.max(maxNumDistrs, set.size());
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
	 * Set the action label for choice i in some state s.
	 */
	public void setAction(int s, int i, Object o)
	{
		// If no actions array created yet, create it
		if (actions == null) {
			actions = new ArrayList<List<Object>>(numStates);
			for (int j = 0; j < numStates; j++)
				actions.add(null);
		}
		// If no actions for state i yet, create list
		if (actions.get(s) == null) {
			int n = trans.get(s).size();
			List<Object> list = new ArrayList<Object>(n);
			for (int j = 0; j < n; j++) {
				list.add(0.0);
			}
			actions.set(s, list);
		}
		// Set actions
		actions.get(s).set(i, o);
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

	// Accessors (for ModelSimple)

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		for (Distribution distr : trans.get(s1)) {
			if (distr.contains(s2))
				return true;
		}
		return false;
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		for (Distribution distr : trans.get(s)) {
			if (!distr.isSubsetOf(set))
				return false;
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		for (Distribution distr : trans.get(s)) {
			if (distr.containsOneOf(set))
				return true;
		}
		return false;
	}

	@Override
	public int getNumChoices(int s)
	{
		return trans.get(s).size();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("MDP has a deadlock in state " + i);
		}
		// TODO: Check for empty distributions too?
	}

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		int i, j;
		String filename = null;
		FileWriter out;
		try {
			// Output transitions to .tra file
			filename = baseFilename + ".tra";
			out = new FileWriter(filename);
			out.write(numStates + " " + numDistrs + " " + numTransitions + "\n");
			for (i = 0; i < numStates; i++) {
				j = -1;
				for (Distribution distr : trans.get(i)) {
					j++;
					for (Map.Entry<Integer, Double> e : distr) {
						out.write(i + " " + j + " " + e.getKey() + " " + PrismUtils.formatDouble(e.getValue()) + "\n");
					}
				}
			}
			out.close();
			// Output transition rewards to .trew file
			// TODO
			filename = baseFilename + ".trew";
			out = new FileWriter(filename);
			out.write(numStates + " " + "?" + " " + "?" + "\n");
			for (i = 0; i < numStates; i++) {
				j = -1;
				for (Distribution distr : trans.get(i)) {
					j++;
					for (Map.Entry<Integer, Double> e : distr) {
						out.write(i + " " + j + " " + e.getKey() + " " + "1.0" + "\n");
					}
				}
			}
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		int i, j;
		String nij;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				j = -1;
				for (Distribution distr : trans.get(i)) {
					j++;
					nij = "n" + i + "_" + j;
					out.write(i + " -> " + nij + " [ arrowhead=none,label=\"" + j + "\" ];\n");
					out.write(nij + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");
					for (Map.Entry<Integer, Double> e : distr) {
						out.write(nij + " -> " + e.getKey() + " [ label=\"" + e.getValue() + "\" ];\n");
					}
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += numStates + " states";
		s += " (" + getNumInitialStates() + " initial)";
		s += ", " + numDistrs + " distributions";
		s += ", " + numTransitions + " transitions";
		s += ", dist max/avg = " + getMaxNumChoices() + "/"
				+ PrismUtils.formatDouble2dp(((double) numDistrs) / numStates);
		return s;
	}

	// Accessors (for MDP)

	@Override
	public boolean someSuccessorsInSetForAllChoices(int s, BitSet set)
	{
		for (Distribution distr : trans.get(s)) {
			if (!distr.containsOneOf(set))
				return false;
		}
		return true;
	}

	@Override
	public boolean someAllSuccessorsInSetForSomeChoices(int s, BitSet set1, BitSet set2)
	{
		for (Distribution distr : trans.get(s)) {
			if (distr.containsOneOf(set1) && distr.isSubsetOf(set2))
				return true;
		}
		return false;
	}

	@Override
	public boolean someAllSuccessorsInSetForAllChoices(int s, BitSet set1, BitSet set2)
	{
		for (Distribution distr : trans.get(s)) {
			if (!distr.containsOneOf(set1) || !distr.isSubsetOf(set2))
				return false;
		}
		return true;
	}

	@Override
	public double getTransitionReward(int s, int i)
	{
		List<Double> list;
		if (transRewardsConstant != null)
			return transRewardsConstant;
		if (transRewards == null || (list = transRewards.get(s)) == null)
			return 0.0;
		return list.get(i);
	}

	@Override
	public void mvMultMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement)
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultMinMaxSingle(s, vect, min);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultMinMaxSingle(s, vect, min);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultMinMaxSingle(s, vect, min);
		}
	}

	@Override
	public double mvMultMinMaxSingle(int s, double vect[], boolean min)
	{
		int k;
		double d, prob, minmax;
		boolean first;
		List<Distribution> step;

		minmax = 0;
		first = true;
		step = trans.get(s);
		for (Distribution distr : step) {
			// Compute sum for this distribution
			d = 0.0;
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax))
				minmax = d;
			first = false;
		}

		return minmax;
	}

	@Override
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min, double val)
	{
		int j, k;
		double d, prob;
		List<Integer> res;
		List<Distribution> step;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		j = -1;
		step = trans.get(s);
		for (Distribution distr : step) {
			j++;
			// Compute sum for this distribution
			d = 0.0;
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				res.add(j);
				//res.add(distrs.getAction());
			}
		}

		return res;
	}

	@Override
	public void mvMultRewMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement)
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultRewMinMaxSingle(s, vect, min);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultRewMinMaxSingle(s, vect, min);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultRewMinMaxSingle(s, vect, min);
		}
	}

	@Override
	public double mvMultRewMinMaxSingle(int s, double vect[], boolean min)
	{
		int j, k;
		double d, prob, minmax;
		boolean first;
		List<Distribution> step;

		minmax = 0;
		first = true;
		j = -1;
		step = trans.get(s);
		for (Distribution distr : step) {
			j++;
			// Compute sum for this distribution
			d = getTransitionReward(s, j);
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax))
				minmax = d;
			first = false;
		}

		return minmax;
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], boolean min, double val)
	{
		int j, k;
		double d, prob;
		List<Integer> res;
		List<Distribution> step;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		j = -1;
		step = trans.get(s);
		for (Distribution distr : step) {
			j++;
			// Compute sum for this distribution
			d = getTransitionReward(s, j);
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				res.add(j);
				//res.add(distrs.getAction());
			}
		}

		return res;
	}

	// Accessors (other)

	/**
	 * Get the list of choices (distributions) for state s.
	 */
	public List<Distribution> getChoices(int s)
	{
		return trans.get(s);
	}

	/**
	 * Get the ith choice (distribution) for state s.
	 */
	public Distribution getChoice(int s, int i)
	{
		return trans.get(s).get(i);
	}

	/**
	 * Get the action (if any) for choice i of state s.
	 */
	public Object getAction(int s, int i)
	{
		List<Object> list;
		if (actions == null || (list = actions.get(s)) == null)
			return null;
		return list.get(i);
	}

	/**
	 * Get the total number of choices (distributions) over all states.
	 */
	public int getNumChoices()
	{
		return numDistrs;
	}

	/**
	 * Get the maximum number of choices (distributions) in any state.
	 */
	public int getMaxNumChoices()
	{
		// Recompute if necessary
		if (!maxNumDistrsOk) {
			maxNumDistrs = 0;
			for (int s = 0; s < numStates; s++)
				maxNumDistrs = Math.max(maxNumDistrs, getNumChoices(s));
		}
		return maxNumDistrs;
	}

	// Standard methods

	@Override
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
			if (actions != null)
				s += actions.get(i);
			if (transRewards != null)
				s += transRewards.get(i);
		}
		s += " ]";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof MDPSimple))
			return false;
		MDPSimple mdp = (MDPSimple) o;
		if (numStates != mdp.numStates)
			return false;
		if (!initialStates.equals(mdp.initialStates))
			return false;
		if (!trans.equals(mdp.trans))
			return false;
		// TODO: compare actions (complicated: null = null,null,null,...)
		// TODO: compare rewards (complicated: null = 0,0,0,0)
		return true;
	}
}
