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
import java.util.function.Function;

import common.iterable.Reducible;
import io.ExplicitModelImporter;
import prism.Evaluator;
import prism.Pair;
import prism.PrismException;

/**
 * Simple explicit-state representation of a DTMC.
 */
public class DTMCSimple<Value> extends DTMCExplicit<Value> implements ModelSimple<Value>
{
	// Transition successors
	protected List<List<Integer>> succ;
	// Transition probabilities
	protected List<List<Value>> trans;
	// Transition actions
	// (stored for each state-"choice", where a "choice" is an index into the above lists)
	protected ChoiceActionsSimple actions;

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
	public DTMCSimple(DTMCSimple<Value> dtmc)
	{
		this(dtmc.numStates);
		copyFrom(dtmc);
		for (int i = 0; i < numStates; i++) {
			succ.set(i, new ArrayList<>(dtmc.succ.get(i)));
			trans.set(i, new ArrayList<>(dtmc.trans.get(i)));
		}
		actions = new ChoiceActionsSimple(dtmc.actions);
	}

	/**
	 * Construct a DTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Pointer to states list is NOT copied (since now wrong).
	 */
	public DTMCSimple(DTMCSimple<Value> dtmc, int permut[])
	{
		this(dtmc.numStates);
		copyFrom(dtmc, permut);
		for (int i = 0; i < numStates; i++) {
			int numSucc = dtmc.succ.get(i).size();
			for (int j = 0; j < numSucc; j++) {
				succ.get(permut[i]).add(permut[dtmc.succ.get(i).get(j)]);
				trans.get(permut[i]).add(dtmc.trans.get(i).get(j));
			}
		}
		actions = new ChoiceActionsSimple(dtmc.actions, permut);
	}

	/**
	 * Construct a DTMCSimple object from a DTMC object.
	 */
	public DTMCSimple(DTMC<Value> dtmc)
	{
		this(dtmc, p -> p);
	}

	/**
	 * Construct a DTMCSimple object from a DTMC object,
	 * mapping probability values using the provided function.
	 * There is no attempt to check that distributions sum to one.
	 */
	public DTMCSimple(DTMC<Value> dtmc, Function<? super Value, ? extends Value> probMap)
	{
		this(dtmc, probMap, dtmc.getEvaluator());
	}

	/**
	 * Construct a DTMCSimple object from a DTMC object,
	 * mapping probability values using the provided function.
	 * There is no attempt to check that distributions sum to one.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 */
	public <T> DTMCSimple(DTMC<T> dtmc, Function<? super T, ? extends Value> probMap, Evaluator<Value> eval)
	{
		this(dtmc.getNumStates());
		copyFrom(dtmc);
		setEvaluator(eval);
		int numStates = getNumStates();
		for (int i = 0; i < numStates; i++) {
			Iterator<Entry<Integer, Pair<T, Object>>> iter = dtmc.getTransitionsAndActionsIterator(i);
			while (iter.hasNext()) {
				Map.Entry<Integer, Pair<T, Object>> e = iter.next();
				addToProbability(i, e.getKey(), probMap.apply(e.getValue().first), e.getValue().second );
			}
		}
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		succ = new ArrayList<>(numStates);
		trans = new ArrayList<>(numStates);
		for (int i = 0; i < numStates; i++) {
			succ.add(new ArrayList<>());
			trans.add(new ArrayList<>());
		}
		actions = new ChoiceActionsSimple();
	}

	@Override
	public void clearState(int i)
	{
		// Do nothing if state does not exist
		if (i >= numStates || i < 0)
			return;
		// Clear data structures
		succ.get(i).clear();
		trans.get(i).clear();
		actions.clearState(i);
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
			succ.add(new ArrayList<>());
			trans.add(new ArrayList<>());
			numStates++;
		}
	}

	@Override
	public void buildFromExplicitImport(ExplicitModelImporter modelImporter) throws PrismException
	{
		initialise(modelImporter.getNumStates());
		modelImporter.extractMCTransitions(this::setProbability, getEvaluator());
	}

	// Mutators (other)

	/**
	 * Set the probability for a transition.
	 */
	public void setProbability(int i, int j, Value prob)
	{
		setProbability(i, j, prob, null);
	}

	/**
	 * Set the probability for a transition.
	 */
	public void setProbability(int i, int j, Value prob, Object action)
	{
		List<Integer> iSucc = succ.get(i);
		List<Value> iTrans = trans.get(i);
		int numSucc = succ.get(i).size();
		// Check for existing transition
		for (int k = 0; k < numSucc; k++) {
			if (iSucc.get(k) == j && actions.actionMatches(i, k, action)) {
				if (getEvaluator().isZero(prob)) {
					iSucc.remove(k);
					iTrans.remove(k);
				} else {
					iTrans.set(k, prob);
				}
				return;
			}
		}
		// No existing transition
		iSucc.add(j);
		iTrans.add(prob);
		actions.setAction(i, numSucc, action);
	}

	/**
	 * Add to the probability for a transition.
	 */
	public void addToProbability(int i, int j, Value prob)
	{
		addToProbability(i, j, prob, null);
	}

	/**
	 * Add to the probability for a transition.
	 */
	public void addToProbability(int i, int j, Value prob, Object action)
	{
		if (getEvaluator().isZero(prob)) {
			return;
		}
		List<Integer> iSucc = succ.get(i);
		List<Value> iTrans = trans.get(i);
		int numSucc = succ.get(i).size();
		// Check for existing transition
		for (int k = 0; k < numSucc; k++) {
			if (iSucc.get(k) == j && actions.actionMatches(i, k, action)) {
				iTrans.set(k, getEvaluator().add(iTrans.get(k), prob));
				return;
			}
		}
		// No existing transition
		iSucc.add(j);
		iTrans.add(prob);
		actions.setAction(i, numSucc, action);
	}

	// Accessors (for Model)

	@Override
	public int getNumTransitions(int s)
	{
		return succ.get(s).size();
	}

	/** Get an iterator over the successors of state s */
	@Override
	public Iterator<Integer> getSuccessorsIterator(final int s)
	{
		// Remove duplicates
		return new HashSet<>(succ.get(s)).iterator();
	}

	@Override
	public SuccessorsIterator getSuccessors(int s)
	{
		return SuccessorsIterator.from(getSuccessorsIterator(s), true);
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		return succ.get(s1).contains(s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return Reducible.extend(succ.get(s)).allMatch(set::get);
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return Reducible.extend(succ.get(s)).anyMatch(set::get);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (succ.get(i).isEmpty()) {
				addDeadlockState(i);
				if (fix)
					setProbability(i, i, getEvaluator().one(), null);
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (succ.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("DTMC has a deadlock in state " + i);
		}
	}

	// Accessors (for DTMC)

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(int s)
	{
		// Create iterator (no removal of duplicates)
		return new Iterator<>() {
			private final int n = succ.get(s).size();
			private int i = 0;

			@Override
			public Entry<Integer, Value> next()
			{
				return new AbstractMap.SimpleImmutableEntry<>(succ.get(s).get(i), trans.get(s).get(i++));
			}

			@Override
			public boolean hasNext()
			{
				return i < n;
			}
		};
	}

	@Override
	public Iterator<Entry<Integer, Pair<Value, Object>>> getTransitionsAndActionsIterator(int s)
	{
		// Create iterator (no removal of duplicates)
		return new Iterator<>() {
			private final int n = succ.get(s).size();
			private int i = 0;

			@Override
			public Entry<Integer, Pair<Value, Object>> next()
			{
				Pair<Value, Object> probAction = new Pair<>(trans.get(s).get(i), actions.getAction(s, i));
				return new AbstractMap.SimpleImmutableEntry<>(succ.get(s).get(i++), probAction);
			}

			@Override
			public boolean hasNext()
			{
				return i < n;
			}
		};
	}

	// Accessors (other)

	/**
	 * Get the transitions (a distribution) for state s.
	 */
	public Distribution<Value> getTransitions(int s)
	{
		return new Distribution<>(getTransitionsIterator(s), getEvaluator());
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		s = "[ ";
		first = true;
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + toStringDistr(i);
		}
		s += " ]";
		return s;
	}

	protected String toStringDistr(int i)
	{
		String s = "";
		boolean first = true;
		s = "{";
		int numSucc = succ.get(i).size();
		for (int j = 0; j < numSucc; j++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += succ.get(i).get(j) + "=" + trans.get(i).get(j);
			Object action = actions.getAction(i, j);
			if (action != null) {
				s += ":" + action;
			}
		}
		s += "}";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCSimple))
			return false;
		if (!super.equals(o))
			return false;
		DTMCSimple<?> dtmc = (DTMCSimple<?>) o;
		if (!succ.equals(dtmc.succ))
			return false;
		if (!trans.equals(dtmc.trans))
			return false;
		if (!actions.equals(dtmc.actions))
			return false;
		return true;
	}
}
