//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit.modelviews;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import common.iterable.FilteringIterator;
import common.iterable.SingletonIterator;
import parser.State;
import parser.Values;
import parser.VarList;
import explicit.DTMC;

/**
 * A view of a DTMC where for selected states the transitions are changed.
 * <br>
 * The new transitions are given by a function (int state) -> Iterator<Entry<Integer, Double>,
 * i.e., providing an iterator over the outgoing transitions. A return value of {@code null}
 * is interpreted as "keep the original transitions".
 */
public class DTMCAlteredDistributions extends DTMCView
{
	private static final Predicate<Entry<Integer, Double>> nonZero =
			(Entry<Integer, Double> e) -> {return e.getValue() > 0.0; };

	private DTMC model;
	private IntFunction<Iterator<Entry<Integer, Double>>> mapping;



	/**
	 * If {@code mapping} returns {@code null} for a state, the original transitions are preserved.
	 *
	 * @param model a DTMC
	 * @param mapping from states to (new) distributions or null
	 */
	public DTMCAlteredDistributions(final DTMC model, final IntFunction<Iterator<Entry<Integer, Double>>> mapping)
	{
		this.model = model;
		this.mapping = mapping;
	}

	public DTMCAlteredDistributions(final DTMCAlteredDistributions altered)
	{
		super(altered);
		model = altered.model;
		mapping = altered.mapping;
	}



	//--- Cloneable ---

	@Override
	public DTMCAlteredDistributions clone()
	{
		return new DTMCAlteredDistributions(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return model.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList()
	{
		return model.getStatesList();
	}

	@Override
	public VarList getVarList()
	{
		return model.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return model.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(final String name)
	{
		return model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return model.getLabels();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return model.hasLabel(name);
	}



	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = mapping.apply(state);
		if (transitions == null) {
			return model.getTransitionsIterator(state);
		}
		return new FilteringIterator.Of<>(transitions, nonZero);
	}



	//--- DTMCView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = fixDeadlocks(this.clone());
		mapping = state -> null;
	}



	//--- static methods ---

	public static DTMCAlteredDistributions fixDeadlocks(final DTMC model)
	{
		final BitSet deadlockStates = new BitSet();
		model.getDeadlockStates().forEach(deadlockStates::set);
		final DTMCAlteredDistributions fixed = addSelfLoops(model, deadlockStates);
		fixed.deadlockStates = deadlockStates;
		fixed.fixedDeadlocks = true;
		return fixed;
	}

	/**
	 * Return a view where the outgoing transitions for all states in the given set
	 * are replaced by probability 1 self-loops.
	 */
	public static DTMCAlteredDistributions addSelfLoops(final DTMC model, final BitSet states)
	{
		final IntFunction<Iterator<Entry<Integer, Double>>> addLoops = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (states.get(state)) {
					Entry<Integer,Double> transition = new AbstractMap.SimpleImmutableEntry<>(state, 1.0);
					return new SingletonIterator.Of<>(transition);
				}
				return null;
			}
		};
		return new DTMCAlteredDistributions(model, addLoops);
	}
}