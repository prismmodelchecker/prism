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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.functions.primitive.PairPredicateInt;
import explicit.Distribution;
import explicit.MDP;
import parser.State;
import parser.Values;
import parser.VarList;

/**
 * An MDPView that takes an existing MDP and removes certain choices.
 */
public class MDPDroppedChoicesCached extends MDPView
{
	private MDP model;

	/** pointer to first for state s */
	private int[] startChoice;
	/** mapping of choice to choice in original MDP */
	private int[] mapping;

	public MDPDroppedChoicesCached(final MDP model, final PairPredicateInt dropped)
	{
		this.model = model;

		int n = model.getNumStates();
		startChoice = new int[model.getNumStates()+1];
		// number of choices in original model is an upper bound
		mapping = new int[model.getNumChoices()];
		int j = 0;
		for (int s = 0; s < n; s++) {
			startChoice[s] = j;
			for (int choice = 0, numChoices = model.getNumChoices(s); choice < numChoices; choice++) {
				if (!dropped.test(s, choice)) {
					mapping[j] = choice;
					j++;
				}
			}
		}
		startChoice[n] = j;
		int numPreservedChoices = j;

		if (numPreservedChoices < mapping.length) {
			// truncate mapping array
			mapping = Arrays.copyOf(mapping, numPreservedChoices);
		}
	}

	public MDPDroppedChoicesCached(final MDPDroppedChoicesCached dropped)
	{
		super(dropped);
		model = dropped.model;
		startChoice = dropped.startChoice;
		mapping = dropped.mapping;
	}

	//--- Cloneable ---

	@Override
	public MDPDroppedChoicesCached clone()
	{
		return new MDPDroppedChoicesCached(this);
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


	//--- NondetModel ---

	@Override
	public int getNumChoices()
	{
		return mapping.length;
	}

	@Override
	public int getNumChoices(final int state)
	{
		return startChoice[state + 1] - startChoice[state];
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getAction(state, originalChoice);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getSuccessorsIterator(state, originalChoice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getTransitionsIterator(state, originalChoice);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks((MDP) this.clone());
	}



	//--- static methods

	public static MDPDroppedChoicesCached dropDenormalizedDistributions(final MDP model)
	{
		final PairPredicateInt denormalizedChoices = new PairPredicateInt()
		{
			@Override
			public boolean test(int state, int choice)
			{
				final Distribution distribution = new Distribution(model.getTransitionsIterator(state, choice));
				return distribution.sum() < 1;
			}
		};
		return new MDPDroppedChoicesCached(model, denormalizedChoices);
	}

	public int mapChoiceToOriginalModel(final int state, final int choice)
	{
		int first = startChoice[state];
		if (choice >= getNumChoices(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}

		return mapping[first + choice];
	}

}
