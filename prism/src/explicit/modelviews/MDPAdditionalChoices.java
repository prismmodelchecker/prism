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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import common.iterable.FunctionalPrimitiveIterable;
import common.iterable.SingletonIterator;
import explicit.MDP;
import parser.State;
import parser.Values;
import parser.VarList;

/**
 * An MDPView that takes an existing MDP and
 * adds additional choices to certain states.
 */
public class MDPAdditionalChoices<Value> extends MDPView<Value>
{
	private MDP<Value> model;
	private IntFunction<List<Iterator<Entry<Integer, Value>>>> choices;
	private IntFunction<List<Object>> actions;

	/**
	 * If {@code choices} returns {@code null} for a state and a choice, no additional choice is added.
	 * If {@code actions} is {@code null} or returns {@code null} for a state, no additional action is attached.
	 *
	 * @param model
	 * @param choices
	 * @param actions
	 */
	public MDPAdditionalChoices(final MDP<Value> model, final IntFunction<List<Iterator<Entry<Integer, Value>>>> choices,
			IntFunction<List<Object>> actions)
	{
		this.model = model;
		this.choices = choices;
		this.actions = actions;
	}

	public MDPAdditionalChoices(final MDPAdditionalChoices<Value> additional)
	{
		super(additional);
		model = additional.model;
		choices = additional.choices;
		actions = additional.actions;
	}



	//--- Cloneable ---

	@Override
	public MDPView<Value> clone()
	{
		return new MDPAdditionalChoices<>(this);
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
	public FunctionalPrimitiveIterable.OfInt getInitialStates()
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


	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return model.getNumChoices(state) + getNumAdditionalChoices(state);
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final int numOriginalChoices = model.getNumChoices(state);
		if (choice < numOriginalChoices) {
			return model.getAction(state, choice);
		}
		if (actions == null) {
			final int numChoices = numOriginalChoices + getNumAdditionalChoices(state);
			if (choice < numChoices) {
				return null;
			}
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}

		final List<Object> additional = actions.apply(state);
		return (additional == null) ?  null : additional.get(choice - numOriginalChoices);
	}


	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(final int state, final int choice)
	{
		final int numOriginalChoices = model.getNumChoices(state);
		if (choice < numOriginalChoices) {
			return model.getTransitionsIterator(state, choice);
		}
		try {
			return choices.apply(state).get(choice - numOriginalChoices);
		} catch (NullPointerException | IndexOutOfBoundsException e)
		{
			// alter message of exception
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
	}

	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = fixDeadlocks((MDP<Value>) this.clone());
		choices = (int element) -> {return (List<Iterator<Entry<Integer, Value>>>)null;};
		actions = null;
	}



	//--- instance methods ---

	private int getNumAdditionalChoices(final int state)
	{
		final List<Iterator<Entry<Integer, Value>>> additional = choices.apply(state);
		return (additional == null) ? 0 : additional.size();
	}



	//--- static methods ---

	public static <Value> MDPView<Value> fixDeadlocks(final MDP<Value> model)
	{
		final BitSet deadlockStates = model.getDeadlockStates().collect(new BitSet());
		final MDPView<Value> fixed = addSelfLoops(model, deadlockStates);
		fixed.deadlockStates = deadlockStates;
		fixed.fixedDeadlocks = true;
		return fixed;
	}

	public static <Value> MDPView<Value> addSelfLoops(final MDP<Value> model, final BitSet states)
	{
		return addSelfLoops(model, states::get);
	}

	public static <Value> MDPView<Value> addSelfLoops(final MDP<Value> model, final IntPredicate states)
	{
		final IntFunction<List<Iterator<Entry<Integer, Value>>>> addSelfLoops = new IntFunction<List<Iterator<Entry<Integer, Value>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Value>>> apply(final int state)
			{
				if (states.test(state)) {
					Entry<Integer,Value> transition = new AbstractMap.SimpleImmutableEntry<>(state, model.getEvaluator().one());
					return Collections.singletonList(new SingletonIterator.Of<>(transition));
				}
				return null;
			}
		};
		return new MDPAdditionalChoices<Value>(model, addSelfLoops, null);
	}
}
