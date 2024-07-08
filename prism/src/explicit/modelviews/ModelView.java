//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

import java.util.BitSet;
import java.util.List;
import java.util.PrimitiveIterator.OfInt;

import common.iterable.FilteringIterable;
import common.iterable.FunctionalPrimitiveIterable;
import common.IterableBitSet;
import common.IterableStateSet;
import explicit.Model;
import explicit.PredecessorRelation;
import explicit.StateValues;
import parser.State;
import parser.VarList;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;

/**
 * Base class for an DTMCView or MDPView,
 * handling common tasks.
 */
public abstract class ModelView<Value> implements Model<Value>
{
	protected BitSet deadlockStates = new BitSet();
	protected boolean fixedDeadlocks = false;
	protected PredecessorRelation predecessorRelation;



	public ModelView()
	{
	}

	public ModelView(final ModelView<Value> model)
	{
		deadlockStates = (BitSet) model.deadlockStates.clone();
		fixedDeadlocks = model.fixedDeadlocks;
	}



	//--- Model ---

	@Override
	public int getNumDeadlockStates()
	{
		return deadlockStates.cardinality();
	}

	@Override
	public FunctionalPrimitiveIterable.OfInt getDeadlockStates()
	{
		return new IterableBitSet(deadlockStates);
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		return StateValues.createFromBitSet(deadlockStates, this);
	}

	@Override
	public int getFirstDeadlockState()
	{
		return deadlockStates.nextSetBit(0);
	}

	@Override
	public boolean isDeadlockState(final int state)
	{
		return deadlockStates.get(state);
	}


	@Override
	public void findDeadlocks(final boolean fix) throws PrismException
	{
		for (int s : findDeadlocks(new BitSet())) {
			deadlockStates.set(s);
		}

		if (fix && !fixedDeadlocks) {
			fixDeadlocks();
			fixedDeadlocks = true;
		}
	}

	public FunctionalPrimitiveIterable.OfInt findDeadlocks(final BitSet except)
	{
		IterableStateSet states = new IterableStateSet(except, getNumStates(), true);
		return new FilteringIterable.OfInt(states, state -> !getSuccessorsIterator(state).hasNext());
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		checkForDeadlocks(null);
	}

	@Override
	public void checkForDeadlocks(final BitSet except) throws PrismException
	{
		OfInt deadlocks = findDeadlocks(except).iterator();
		if (deadlocks.hasNext()) {
			throw new PrismException(getModelType() + " has a deadlock in state " + deadlocks.nextInt());
		}
	}

	@Override
	public boolean hasStoredPredecessorRelation()
	{
		return (predecessorRelation != null);
	}

	@Override
	public PredecessorRelation getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		if (predecessorRelation != null) {
			return predecessorRelation;
		}

		final PredecessorRelation pre = PredecessorRelation.forModel(parent, this);

		if (storeIfNew) {
			predecessorRelation = pre;
		}
		return pre;
	}

	@Override
	public void clearPredecessorRelation()
	{
		predecessorRelation = null;
	}



	//--- instance methods ---

	protected abstract void fixDeadlocks();

	/**
	 * Tell whether the receiver is a virtual or explicit model.
	 * Virtual models may impose a significant overhead on any computations.
	 *
	 * @return true iff model is a virtual model
	 */
	public boolean isVirtual()
	{
		return true;
	}
}
