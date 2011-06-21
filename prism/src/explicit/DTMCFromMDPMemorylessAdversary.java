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

import explicit.rewards.MCRewards;

import parser.State;
import parser.Values;
import prism.ModelType;
import prism.PrismException;

/**
 * Explicit-state representation of a DTMC, constructed (implicitly)
 * from an MDP and a memoryless adversary, specified as an array of integer indices.
 * This class is read-only: most of data is pointers to other model info.
 */
public class DTMCFromMDPMemorylessAdversary implements DTMC
{
	// Parent MDP
	protected MDP mdp;
	// Also store num states for easy access
	protected int numStates;
	// Adversary
	protected int adv[];

	/**
	 * Constructor: create from MDP and memoryless adversary.
	 */
	public DTMCFromMDPMemorylessAdversary(MDP mdp, int adv[])
	{
		this.mdp = mdp;
		this.numStates = mdp.getNumStates();
		this.adv = adv;
	}

	// Accessors (for Model)

	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	public int getNumStates()
	{
		return mdp.getNumStates();
	}

	public int getNumInitialStates()
	{
		return mdp.getNumInitialStates();
	}

	public Iterable<Integer> getInitialStates()
	{
		return mdp.getInitialStates();
	}

	public int getFirstInitialState()
	{
		return mdp.getFirstInitialState();
	}

	public boolean isInitialState(int i)
	{
		return mdp.isInitialState(i);
	}

	public boolean isFixedDeadlockState(int i)
	{
		return mdp.isFixedDeadlockState(i);
	}

	public List<State> getStatesList()
	{
		return mdp.getStatesList();
	}
	
	public Values getConstantValues()
	{
		return mdp.getConstantValues();
	}
	
	public int getNumTransitions()
	{
		throw new RuntimeException("Not implemented");
	}

	public boolean isSuccessor(int s1, int s2)
	{
		throw new RuntimeException("Not implemented yet");
	}

	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		throw new RuntimeException("Not implemented yet");
	}

	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		throw new RuntimeException("Not implemented yet");
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
		return mdp.infoString() + " + " + "???"; // TODO
	}

	@Override
	public String infoStringTable()
	{
		return mdp.infoString() + " + " + "???\n"; // TODO
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

	public double getTransitionReward(int s)
	{
		// TODO
		return 0;
	}

	public void prob0step(BitSet subset, BitSet u, BitSet result)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	public void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		// TODO
		throw new Error("Not yet supported");
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
		return mdp.mvMultSingle(s, adv[s], vect);
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
		return mdp.mvMultJacSingle(s, adv[s], vect);
	}

	public void mvMultRew(double vect[], MCRewards mcRewards, double result[], BitSet subset, boolean complement)
	{
		int s, numStates;
		numStates = mdp.getNumStates();
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
		throw new RuntimeException("Not implemented yet");
		//return mdp.mvMultRewSingle(s, adv[s], vect);
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		throw new RuntimeException("Not implemented yet"); // TODO
	}

	@Override
	public String toString()
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean equals(Object o)
	{
		throw new RuntimeException("Not implemented yet");
	}
}
