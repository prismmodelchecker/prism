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

import java.util.*;
import java.util.Map.Entry;

import common.iterable.EmptyIterable;
import common.iterable.EmptyIterator;
import common.iterable.FunctionalPrimitiveIterable;
import common.iterable.FunctionalPrimitiveIterator;
import explicit.MDP;
import parser.State;
import parser.Values;
import parser.VarList;

/**
 * An MDPView that takes an existing MDP and removes
 * all outgoing choices for certain states.
 */
public class MDPDroppedAllChoices<Value> extends MDPView<Value>
{
	private MDP<Value> model;
	private BitSet states;



	public MDPDroppedAllChoices(final MDP<Value> model, final BitSet dropped)
	{
		this.model = model;
		this.states = dropped;
	}

	public MDPDroppedAllChoices(final MDPDroppedAllChoices<Value> dropped)
	{
		super(dropped);
		model = dropped.model;
		states = dropped.states;
	}



	//--- Cloneable ---

	@Override
	public MDPDroppedAllChoices<Value> clone()
	{
		return new MDPDroppedAllChoices<>(this);
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


	@Override
	public FunctionalPrimitiveIterator.OfInt getSuccessorsIterator(final int state)
	{
		return states.get(state) ? EmptyIterator.ofInt() : model.getSuccessorsIterator(state);
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return states.get(state) ? 0 : model.getNumChoices(state);
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		if (states.get(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return model.getAction(state, choice);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		if (states.get(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return model.getSuccessorsIterator(state, choice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(final int state, final int choice)
	{
		if (states.get(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return model.getTransitionsIterator(state, choice);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		states = new BitSet();
	}

}
