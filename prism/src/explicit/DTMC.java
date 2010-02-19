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

/**
 * Explicit representation of discrete-time Markov chain (DTMC)
 */
public class DTMC extends Model
{
	// Model type
	public static ModelType modelType = ModelType.DTMC;

	// Transition matrix (distribution list) 
	protected List<Distribution> trans;

	// Rewards
	protected List<Double> transRewards;
	protected Double transRewardsConstant;

	// Other statistics
	protected int numTransitions;

	/**
	 * Constructor: empty DTMC.
	 */
	public DTMC()
	{
		initialise(0);
	}

	/**
	 * Constructor: new DTMC with fixed number of states.
	 */
	public DTMC(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Initialise: new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		trans = new ArrayList<Distribution>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new Distribution());
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
		numTransitions -= trans.get(i).size();
		trans.get(i).clear();
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
			trans.add(new Distribution());
			numStates++;
		}
	}

	/**
	 * Set the probability for a transition. 
	 */
	public void setProbability(int i, int j, double prob)
	{
		Distribution distr = trans.get(i);
		if (distr.get(i) != 0.0)
			numTransitions--;
		if (prob != 0.0)
			numTransitions++;
		trans.get(i).set(j, prob);
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
	 * Set the reward for (all) transitions in state s to r.
	 */
	public void setTransitionReward(int s, double r)
	{
		// This would replace any constant reward definition, if it existed
		transRewardsConstant = null;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<Double>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(0.0);
		}
		// Set reward
		transRewards.set(s, r);
	}

	/**
	 * Get the number of nondeterministic choices in state s (always 1 for a DTMC).
	 */
	public int getNumChoices(int s)
	{
		return 1;
	}

	/**
	 * Get the transitions (a distribution) for state s.
	 */
	public Distribution getTransitions(int s)
	{
		return trans.get(s);
	}

	/**
	 * Get the transition reward (if any) for the transitions in state s.
	 */
	public double getTransitionReward(int s)
	{
		if (transRewardsConstant != null)
			return transRewardsConstant;
		if (transRewards == null)
			return 0.0;
		return transRewards.get(s);
	}

	/**
	 * Returns true if state s2 is a successor of state s1.
	 */
	public boolean isSuccessor(int s1, int s2)
	{
		return trans.get(s1).contains(s2);
	}

	/**
	 * Get the total number of transitions in the model.
	 */
	public int getNumTransitions()
	{
		return numTransitions;
	}

	/**
	 * Checks for deadlocks (states with no transitions) and throws an exception if any exist.
	 * States in 'except' (If non-null) are excluded from the check.
	 */
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("DTMC has a deadlock in state " + i);
		}
	}

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 */
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, n, lineNum = 0;
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
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					ss = s.split(" ");
					i = Integer.parseInt(ss[0]);
					j = Integer.parseInt(ss[1]);
					prob = Double.parseDouble(ss[2]);
					setProbability(i, j, prob);
				}
				s = in.readLine();
				lineNum++;
			}
			// Close file
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + modelType);
		}
		// Set initial state (assume 0)
		initialStates.add(0);
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
		int i;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + modelType + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				for (Map.Entry<Integer, Double> e : trans.get(i)) {
					out.write(i + " -> " + e.getKey() + " [ label=\"");
					out.write(e.getValue() + "\" ];\n");
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
		s += " (" + getNumInitialStates() + " initial)";
		s += ", " + numTransitions + " transitions";
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
		if (o == null || !(o instanceof DTMC))
			return false;
		DTMC dtmc = (DTMC) o;
		if (numStates != dtmc.numStates)
			return false;
		if (!initialStates.equals(dtmc.initialStates))
			return false;
		if (!trans.equals(dtmc.trans))
			return false;
		return true;
	}
}
