//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import prism.PrismException;
import strat.MDStrategy;

/**
 * Simple explicit-state representation of an LTS (labelled transition system).
 * 
 * Each transition from a state is considered to be a separate (singleton) "choice",
 * following the terminology in other nondeterministic explicit-state models.
 */
public class LTSSimple extends ModelExplicit implements LTS, NondetModelSimple
{
	// Transition relation
	protected List<List<Integer>> trans;
	
	// Action labels
	protected ChoiceActionsSimple actions;

	// Statistics: total number of transitions
	protected int numTransitions;

	// Constructors

	/**
	 * Constructor: empty LTS.
	 */
	public LTSSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new LTS with fixed number of states.
	 */
	public LTSSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Construct an LTS from an existing one.
	 */
	public LTSSimple(LTSSimple lts)
	{
		this(lts.getNumStates());
		copyFrom(lts);
		// Copy storage directly
		for (int s = 0; s < numStates; s++) {
			List<Integer> succs = trans.get(s);
			for (int succ : lts.trans.get(s)) {
				succs.add(succ);
			}
		}
		actions = new ChoiceActionsSimple(lts.actions);
		// Copy stats too
		numTransitions = lts.numTransitions;
	}

	/**
	 * Construct an LTS from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public LTSSimple(LTSSimple lts, int permut[])
	{
		this(lts.getNumStates());
		copyFrom(lts, permut);
		// Copy storage directly
		for (int s = 0; s < numStates; s++) {
			List<Integer> succs = trans.get(permut[s]);
			for (int succ : lts.trans.get(s)) {
				succs.add(permut[succ]);
			}
		}
		actions = new ChoiceActionsSimple(lts.actions, permut);
		// Copy stats too
		numTransitions = lts.numTransitions;
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		trans = new ArrayList<List<Integer>>();
		for (int i = 0; i < numStates; i++) {
			trans.add(new ArrayList<Integer>());
		}
		actions = new ChoiceActionsSimple();
		numTransitions = 0;
	}

	@Override
	public void clearState(int s)
	{
		// Clear data structures and update stats
		List<Integer> list = trans.get(s);
		numTransitions -= list.size();
		list.clear();
		actions.clearState(s);
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
			trans.add(new ArrayList<Integer>());
			numStates++;
		}
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	// Mutators (other)
	
	/**
	 * Add a transition from state {@code s} (which must exist) to state {code t}.
	 */
	public void addTransition(int s, int t)
	{
		// We don't care if a transition from s to t already exists
		trans.get(s).add(t);
		numTransitions++;
	}

	/**
	 * Add a transition from state {@code s} (which must exist)
	 * to state {code t}, labelled with {@code action}.
	 */
	public void addActionLabelledTransition(int s, int t, Object action)
	{
		// We don't care if a transition from s to t already exists
		trans.get(s).add(t);
		actions.setAction(s, trans.get(s).size() - 1, action);
		numTransitions++;
	}

	/**
	 * Set the action label for choice/transition i in some state s.
	 * Note that i is the index of the choice/transition, not the destination.
	 */
	public void setAction(int s, int i, Object action)
	{
		actions.setAction(s, i, action);
	}

	// Accessors (for Model)
	
	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public int getNumTransitions(int s)
	{
		return getNumChoices(s);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty()) {
				addDeadlockState(i);
				if (fix) {
					addTransition(i, i);
				}
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("Model has a deadlock in state " + i);
		}
	}
	
	// Accessors (for NondetModel)

	@Override
	public int getNumChoices(int s)
	{
		// one choice per successor for s
		return trans.get(s).size();
	}

	@Override
	public int getNumChoices()
	{
		return numTransitions;
	}

	@Override
	public Object getAction(int s, int i)
	{
		return actions.getAction(s, i);
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		if (i < getNumChoices(s)) {
			// one transition per choice
			return 1;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		// single successor for s, i
		return set.get(trans.get(s).get(i));
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		// single successor for s, i
		return set.get(trans.get(s).get(i));
	}

	@Override
	public SuccessorsIterator getSuccessors(int s, int i)
	{
		// single successor for s, i
		return SuccessorsIterator.fromSingleton(trans.get(s).get(i));
	}

	@Override
	public SuccessorsIterator getSuccessors(int s)
	{
		return SuccessorsIterator.from(trans.get(s).iterator(), false);
	}

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		throw new UnsupportedOperationException();
	}
	
	// Accessors (for LTS)
	
	@Override
	public int getSuccessor(int s, int i)
	{
		return trans.get(s).get(i); 
	}
}
