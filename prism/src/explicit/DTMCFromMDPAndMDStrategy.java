//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
import prism.PrismException;
import prism.PrismNotSupportedException;
import strat.MDStrategy;

/**
* Explicit-state representation of a DTMC, constructed (implicitly)
* from an MDP and a memoryless deterministic (MD) adversary.
* This class is read-only: most of the data is pointers to other model info.
*/
public class DTMCFromMDPAndMDStrategy extends DTMCExplicit
{
	// Parent MDP
	protected MDP mdp;
	// MD strategy
	protected MDStrategy strat;

	/**
	 * Constructor: create from MDP and memoryless adversary.
	 */
	public DTMCFromMDPAndMDStrategy(MDP mdp, MDStrategy strat)
	{
		this.mdp = mdp;
		this.numStates = mdp.getNumStates();
		this.strat = strat;
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
		return strat.isChoiceDefined(s) ? mdp.getNumTransitions(s, strat.getChoiceIndex(s)) : 0;
	}

	public SuccessorsIterator getSuccessors(final int s)
	{
		if (strat.isChoiceDefined(s)) {
			return mdp.getSuccessors(s, strat.getChoiceIndex(s));
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

	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s)
	{
		if (strat.isChoiceDefined(s)) {
			return mdp.getTransitionsIterator(s, strat.getChoiceIndex(s));
		} else {
			// Empty iterator
			Map<Integer,Double> empty = Collections.emptyMap();
			return empty.entrySet().iterator();
		}
	}

	@Override
	public void forEachTransition(int s, TransitionConsumer c)
	{
		if (!strat.isChoiceDefined(s)) {
			return;
		}
		mdp.forEachTransition(s, strat.getChoiceIndex(s), c::accept);
	}

	@Override
	public double mvMultSingle(int s, double vect[])
	{
		return strat.isChoiceDefined(s) ? mdp.mvMultSingle(s, strat.getChoiceIndex(s), vect) : 0;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		return strat.isChoiceDefined(s) ? mdp.mvMultJacSingle(s, strat.getChoiceIndex(s), vect) : 0;
	}

	@Override
	public double mvMultRewSingle(int s, double vect[], MCRewards mcRewards)
	{
		return strat.isChoiceDefined(s) ? mdp.mvMultRewSingle(s, strat.getChoiceIndex(s), vect, mcRewards) : 0;
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
