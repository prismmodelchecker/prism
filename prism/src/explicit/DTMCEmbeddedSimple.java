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

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import explicit.rewards.MCRewards;
import parser.State;
import parser.Values;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Simple explicit-state representation of a DTMC, constructed (implicitly) as the embedded DTMC of a CTMC.
 * i.e. P(i,j) = R(i,j) / E(i) if E(i) > 0 and P(i,i) = 1 otherwise
 * where E(i) is the exit rate for state i: sum_j R(i,j).
 * This class is read-only: most of data is pointers to other model info.
 */
public class DTMCEmbeddedSimple implements DTMC
{
	// Parent CTMC
	protected CTMCSimple ctmc;
	// Also store num states for easy access
	protected int numStates;
	// Exit rates vector
	protected double exitRates[];
	// Number of extra transitions added (just for stats)
	protected int numExtraTransitions;

	/**
	 * Constructor: create from CTMC.
	 */
	public DTMCEmbeddedSimple(CTMCSimple ctmc)
	{
		this.ctmc = ctmc;
		this.numStates = ctmc.getNumStates();
		exitRates = new double[numStates];
		numExtraTransitions = 0;
		for (int i = 0; i < numStates; i++) {
			exitRates[i] = ctmc.getTransitions(i).sum();
			if (exitRates[i] == 0)
				numExtraTransitions++;
		}
	}

	// Accessors (for Model)

	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	public int getNumStates()
	{
		return ctmc.getNumStates();
	}

	public int getNumInitialStates()
	{
		return ctmc.getNumInitialStates();
	}

	public Iterable<Integer> getInitialStates()
	{
		return ctmc.getInitialStates();
	}

	public int getFirstInitialState()
	{
		return ctmc.getFirstInitialState();
	}

	public boolean isInitialState(int i)
	{
		return ctmc.isInitialState(i);
	}

	public boolean isFixedDeadlockState(int i)
	{
		return ctmc.isFixedDeadlockState(i);
	}

	public List<State> getStatesList()
	{
		return ctmc.getStatesList();
	}
	
	public Values getConstantValues()
	{
		return ctmc.getConstantValues();
	}
	
	public int getNumTransitions()
	{
		return ctmc.getNumTransitions() + numExtraTransitions;
	}

	public boolean isSuccessor(int s1, int s2)
	{
		return exitRates[s1] == 0 ? (s1 == s2) : ctmc.isSuccessor(s1, s2);
	}

	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return exitRates[s] == 0 ? set.get(s) : ctmc.allSuccessorsInSet(s, set); 
	}

	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return exitRates[s] == 0 ? set.get(s) : ctmc.someSuccessorsInSet(s, set); 
	}

	public int getNumChoices(int s)
	{
		// Always 1 for a DTMC
		return 1;
	}

	public void checkForDeadlocks() throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		// No deadlocks by definition
	}

	public BitSet findDeadlocks(boolean fix) throws PrismException
	{
		// No deadlocks by definition
		return new BitSet();
	}

	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	public void exportToPrismExplicitTra(PrismLog out) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	public void exportToDotFile(String filename) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += numStates + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions (incl. " + numExtraTransitions + " self-loops)";
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	// Accessors (for DTMC)

	public double getNumTransitions(int s)
	{
		// TODO
		throw new RuntimeException("Not implemented yet");
	}

	public Iterator<Entry<Integer,Double>> getTransitionsIterator(int s)
	{
		// TODO
		throw new RuntimeException("Not implemented yet");
	}

	public void prob0step(BitSet subset, BitSet u, BitSet result)
	{
		int i;
		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
				result.set(i, someSuccessorsInSet(i, u));
			}
		}
	}

	public void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		int i;
		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
				result.set(i, someSuccessorsInSet(i, v) && allSuccessorsInSet(i, u));
			}
		}
	}

	public void mvMult(double vect[], double result[], BitSet subset, boolean complement)
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultSingle(s, vect);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultSingle(s, vect);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultSingle(s, vect);
		}
	}

	public double mvMultSingle(int s, double vect[])
	{
		int k;
		double d, er, prob;
		Distribution distr;

		distr = ctmc.getTransitions(s);
		d = 0.0;
		er = exitRates[s];
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			d += vect[s];
		}
		// Exit rate > 0
		else {
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			d /= er;
		}

		return d;
	}

	@Override
	public double mvMultGS(double vect[], BitSet subset, boolean complement, boolean absolute)
	{
		int s;
		double d, diff, maxDiff = 0.0;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				d = mvMultJacSingle(s, vect);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				d = mvMultJacSingle(s, vect);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				d = mvMultJacSingle(s, vect);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}
		return maxDiff;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		int k;
		double diag, d, er, prob;
		Distribution distr;

		distr = ctmc.getTransitions(s);
		diag = d = 0.0;
		er = exitRates[s];
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			return 0.0;
		}
		// Exit rate > 0
		else {
			// (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
			// = (sum_{j!=s} (R(s,j)/E(s))*vect[j]) / (1-(P(s,s)/E(s)))
			// = (sum_{j!=s} R(s,j)*vect[j]) / (E(s)-P(s,s))
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				// Non-diagonal entries only
				if (k != s) {
					d += prob * vect[k];
				} else {
					diag = prob;
				}
			}
			d /= (er - diag);
		}
		
		return d;
	}

	public void mvMultRew(double vect[], MCRewards mcRewards, double result[], BitSet subset, boolean complement)
	{
		int s, numStates;
		numStates = ctmc.getNumStates();
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultRewSingle(s, vect, mcRewards);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultRewSingle(s, vect, mcRewards);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultRewSingle(s, vect, mcRewards);
		}
	}

	public double mvMultRewSingle(int s, double vect[], MCRewards mcRewards)
	{
		int k;
		double d, er, prob;
		Distribution distr;

		distr = ctmc.getTransitions(s);
		er = exitRates[s];
		d = 0;
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			d += vect[s];
		}
		// Exit rate > 0
		else {
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			d /= er;
		}
		d += mcRewards.getStateReward(s);

		return d;
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		int i, j;
		double prob, er;
		Distribution distr;
		
		// Initialise result to 0
		for (j = 0; j < numStates; j++) {
			result[j] = 0;
		}
		// Go through matrix elements (by row)
		for (i = 0; i < numStates; i++) {
			distr = ctmc.getTransitions(i);
			er = exitRates[i];
			// Exit rate 0: prob 1 self-loop
			if (er == 0) {
				result[i] += vect[i];
			}
			// Exit rate > 0
			else {
				for (Map.Entry<Integer, Double> e : distr) {
					j = (Integer) e.getKey();
					prob = (Double) e.getValue();
					result[j] += (prob / er) * vect[i];
				}
			}
		}
	}

	@Override
	public String toString()
	{
		int i, numStates;
		boolean first;
		String s = "";
		s += "ctmc: " + ctmc;
		first = true;
		s = ", exitRates: [ ";
		numStates = getNumStates();
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + exitRates[i];
		}
		s += " ]";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCEmbeddedSimple))
			return false;
		DTMCEmbeddedSimple dtmc = (DTMCEmbeddedSimple) o;
		if (!ctmc.equals(dtmc.ctmc))
			return false;
		if (!exitRates.equals(dtmc.exitRates))
			return false;
		if (numExtraTransitions != dtmc.numExtraTransitions)
			return false;
		return true;
	}
}
