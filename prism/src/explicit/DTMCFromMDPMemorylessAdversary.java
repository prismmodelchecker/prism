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

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.iterable.MappingIterator;
import parser.State;
import parser.Values;
import prism.Pair;
import prism.PrismException;
import prism.PrismNotSupportedException;
import explicit.rewards.MCRewards;

/**
 * Explicit-state representation of a DTMC, constructed (implicitly)
 * from an MDP and a memoryless adversary, specified as an array of integer indices.
 * This class is read-only: most of data is pointers to other model info.
 */
public class DTMCFromMDPMemorylessAdversary extends DTMCExplicit
{
	// Parent MDP
	protected MDP mdp;
	// Adversary (array of choice indices; -1 denotes no choice)
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

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new PrismNotSupportedException("Not supported");
	}

	// Accessors (for Model)

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

	public boolean isDeadlockState(int i)
	{
		return mdp.isDeadlockState(i);
	}

	public List<State> getStatesList()
	{
		return mdp.getStatesList();
	}

	public Values getConstantValues()
	{
		return mdp.getConstantValues();
	}

	public int getNumTransitions(int s)
	{
		return adv[s] >= 0 ? mdp.getNumTransitions(s, adv[s]) : 0;
	}

	public SuccessorsIterator getSuccessors(final int s)
	{
		if (adv[s] >= 0) {
			return mdp.getSuccessors(s, adv[s]);
		} else {
			return SuccessorsIterator.empty();
		}
	}

	public int getNumChoices(int s)
	{
		// Always 1 for a DTMC
		return 1;
	}

	public void findDeadlocks(boolean fix) throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks() throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		// No deadlocks by definition
	}

	// Accessors (for DTMC)

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s)
	{
		if (adv[s] >= 0) {
			return mdp.getTransitionsIterator(s, adv[s]);
		} else {
			// Empty iterator
			return Collections.<Entry<Integer,Double>>emptyIterator(); 
		}
	}

	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(int s)
	{
		if (adv[s] >= 0) {
			final Iterator<Entry<Integer, Double>> transitions = mdp.getTransitionsIterator(s, adv[s]);
			return new MappingIterator.From<>(transitions, transition -> DTMC.attachAction(transition, mdp.getAction(s, adv[s])));
		} else {
			// Empty iterator
			return Collections.<Entry<Integer,Pair<Double, Object>>>emptyIterator(); 
		}
	}

	@Override
	public double mvMultSingle(int s, double vect[])
	{
		return adv[s] >= 0 ? mdp.mvMultSingle(s, adv[s], vect) : 0;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		return adv[s] >= 0 ? mdp.mvMultJacSingle(s, adv[s], vect) : 0;
	}

	@Override
	public double mvMultRewSingle(int s, double vect[], MCRewards mcRewards)
	{
		return adv[s] >= 0 ? mdp.mvMultRewSingle(s, adv[s], vect, mcRewards) : 0;
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		throw new RuntimeException("Not implemented yet");
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
