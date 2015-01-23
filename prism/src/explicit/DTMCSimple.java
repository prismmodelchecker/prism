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
import java.util.Map.Entry;
import java.io.*;

import common.IterableStateSet;

import explicit.rewards.*;
import prism.PrismException;

/**
 * Simple explicit-state representation of a DTMC.
 */
public class DTMCSimple extends DTMCExplicit implements ModelSimple
{
	// Transition matrix (distribution list) 
	protected List<Distribution> trans;

	// Other statistics
	protected int numTransitions;

	// Constructors

	/**
	 * Constructor: empty DTMC.
	 */
	public DTMCSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new DTMC with fixed number of states.
	 */
	public DTMCSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public DTMCSimple(DTMCSimple dtmc)
	{
		this(dtmc.numStates);
		copyFrom(dtmc);
		for (int i = 0; i < numStates; i++) {
			trans.set(i, new Distribution(dtmc.trans.get(i)));
		}
		numTransitions = dtmc.numTransitions;
	}

	/**
	 * Construct a DTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Pointer to states list is NOT copied (since now wrong).
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public DTMCSimple(DTMCSimple dtmc, int permut[])
	{
		this(dtmc.numStates);
		copyFrom(dtmc, permut);
		for (int i = 0; i < numStates; i++) {
			trans.set(permut[i], new Distribution(dtmc.trans.get(i), permut));
		}
		numTransitions = dtmc.numTransitions;
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		trans = new ArrayList<Distribution>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new Distribution());
		}
	}

	@Override
	public void clearState(int i)
	{
		// Do nothing if state does not exist
		if (i >= numStates || i < 0)
			return;
		// Clear data structures and update stats
		numTransitions -= trans.get(i).size();
		trans.get(i).clear();
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
			trans.add(new Distribution());
			numStates++;
		}
	}

	@Override
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
			if (s == null) {
				in.close();
				throw new PrismException("Missing first line of .tra file");
			}
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
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
		// Set initial state (assume 0)
		initialStates.add(0);
	}

	// Mutators (other)

	/**
	 * Set the probability for a transition. 
	 */
	public void setProbability(int i, int j, double prob)
	{
		Distribution distr = trans.get(i);
		if (distr.get(j) != 0.0)
			numTransitions--;
		if (prob != 0.0)
			numTransitions++;
		distr.set(j, prob);
	}

	/**
	 * Add to the probability for a transition. 
	 */
	public void addToProbability(int i, int j, double prob)
	{
		if (!trans.get(i).add(j, prob)) {
			if (prob != 0.0)
				numTransitions++;
		}
	}

	// Accessors (for Model)

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int s)
	{
		return trans.get(s).getSupport().iterator();
	}
	
	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		return trans.get(s1).contains(s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return (trans.get(s).isSubsetOf(set));
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return (trans.get(s).containsOneOf(set));
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty()) {
				addDeadlockState(i);
				if (fix)
					setProbability(i, i, 1.0);
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("DTMC has a deadlock in state " + i);
		}
	}

	// Accessors (for DTMC)

	@Override
	public int getNumTransitions(int s)
	{
		return trans.get(s).size();
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s)
	{
		return trans.get(s).iterator();
	}

	@Override
	public void prob0step(BitSet subset, BitSet u, BitSet result)
	{
		Distribution distr;
		for (int i : new IterableStateSet(subset, numStates)) {
			distr = trans.get(i);
			result.set(i, distr.containsOneOf(u));
		}
	}

	@Override
	public void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		Distribution distr;
		for (int i : new IterableStateSet(subset, numStates)) {
			distr = trans.get(i);
			result.set(i, distr.containsOneOf(v) && distr.isSubsetOf(u));
		}
	}

	@Override
	public double mvMultSingle(int s, double vect[])
	{
		int k;
		double d, prob;
		Distribution distr;

		distr = trans.get(s);
		d = 0.0;
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			d += prob * vect[k];
		}

		return d;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		int k;
		double diag, d, prob;
		Distribution distr;

		distr = trans.get(s);
		diag = 1.0;
		d = 0.0;
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			if (k != s) {
				d += prob * vect[k];
			} else {
				diag -= prob;
			}
		}
		if (diag > 0)
			d /= diag;

		return d;
	}

	@Override
	public double mvMultRewSingle(int s, double vect[], MCRewards mcRewards)
	{
		int k;
		double d, prob;
		Distribution distr;

		distr = trans.get(s);
		d = mcRewards.getStateReward(s);
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			d += prob * vect[k];
		}

		return d;
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		int i, j;
		double prob;
		Distribution distr;
		
		// Initialise result to 0
		for (j = 0; j < numStates; j++) {
			result[j] = 0;
		}
		// Go through matrix elements (by row)
		for (i = 0; i < numStates; i++) {
			distr = trans.get(i);
			for (Map.Entry<Integer, Double> e : distr) {
				j = (Integer) e.getKey();
				prob = (Double) e.getValue();
				result[j] += prob * vect[i];
			}

		}
	}

	// Accessors (other)

	/**
	 * Get the transitions (a distribution) for state s.
	 */
	public Distribution getTransitions(int s)
	{
		return trans.get(s);
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		first = true;
		s = "trans: [ ";
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

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCSimple))
			return false;
		if (!super.equals(o))
			return false;
		DTMCSimple dtmc = (DTMCSimple) o;
		if (!trans.equals(dtmc.trans))
			return false;
		if (numTransitions != dtmc.numTransitions)
			return false;
		return true;
	}
}
