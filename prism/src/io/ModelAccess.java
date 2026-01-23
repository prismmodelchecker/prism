//==============================================================================
//
//	Copyright (c) 2024-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package io;

import common.Interval;
import common.iterable.SingletonIterator;
import explicit.*;
import explicit.rewards.Rewards;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleIterators;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntIterators;
import param.BigRational;
import prism.Evaluator;
import prism.ModelType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Interface for (read-only) access to model data, unified across all model types.
 * Also provides compressed spare row (CSR) style access to the data.
 * This is generic, where probabilities/rates/etc. are of type {@code Value}.
 */
public interface ModelAccess<Value>
{
	/**
	 * Get the type of this model.
	 */
	ModelType getModelType();

	/**
	 * Get an Evaluator for the values stored in this Model for probabilities etc.
	 */
	Evaluator<Value> getEvaluator();

	/**
	 * Get a list of the action labels attached to choices/transitions.
	 * This can be a superset of the action labels that are actually used in the model.
	 * Action labels can be any Object, but will often be treated as a string,
	 * so should at least have a meaningful toString() method implemented.
	 * Absence of an action label is denoted by null,
	 * and null is also included in this list if there are unlabelled choices/transitions.
	 */
	List<Object> getActions();

	/**
	 * Get strings for the action labels attached to choices/transitions.
	 * The empty/absent action (null) is included here as "".
	 */
	List<String> getActionStrings();

	/**
	 * Get the number of players.
	 */
	int getNumPlayers();

	/**
	 * Get the number of observations.
	 */
	int getNumObservations();

	/**
	 * Get the number of states.
	 */
	int getNumStates();

	/**
	 * Get the number of nondeterministic choices in state s.
	 */
	int getNumChoices(int s);

	/**
	 * Get the total number of nondeterministic choices over all states.
	 */
	default int getNumChoices()
	{
		int numChoices = 0;
		for (int s = 0, numStates = getNumStates(); s < numStates; s++) {
			numChoices += getNumChoices(s);
		}
		return numChoices;
	}
	/**
	 * Get the number of transitions from choice {@code i} of state {@code s}.
	 */
	int getNumTransitions(int s, int i);

	/**
	 * Get the number of transitions from state s.
	 */
	default int getNumTransitions(int s)
	{
		int numChoices = getNumChoices(s);
		int numTransitions = 0;
		for (int i = 0; i < numChoices; i++) {
			numTransitions += getNumTransitions(s, i);
		}
		return numTransitions;
	}

	/**
	 * Get the total number of transitions.
	 */
	default int getNumTransitions()
	{
		int numStates = getNumStates();
		int numTransitions = 0;
		for (int s = 0; s < numStates; s++) {
			numTransitions += getNumTransitions(s);
		}
		return numTransitions;
	}

	/**
	 * Get an iterator over the transitions from choice {@code i} of state {@code s}.
	 * For CTMCs, this returns the embedded DTMC transitions.
	 * For interval models, this returns just the lower bound of each probability interval.
	 * For LTSs, this returns a singleton iterator with a null probability value.
	 */
	Iterator<Map.Entry<Integer, Value>> getTransitionsIterator(int s, int i);

	/**
	 * Get the successor (target state) of the {@code j}th transition from choice {@code i} of state {@code s}.
	 */
	int getTransitionSuccessor(int s, int i, int j);

	/**
	 * Get the exit rate for state {@code s} of a CTMC (returns null for other models).
	 */
	Value getExitRate(int s);

	/**
	 * Get the action label for choice {@code i} of state {@code s}.
	 * This is null if unlabelled.
	 */
	Object getChoiceAction(int s, int i);

	/**
	 * Get the index of the action label for choice {@code i} of state {@code s}.
	 * This is 0 if unlabelled.
	 */
	int getChoiceActionIndex(int s, int i);

	/**
	 * Get the action labels for the transitions from choice {@code i} of state {@code s}.
	 * These are null if unlabelled.
	 */
	Iterator<Object> getTransitionActionsIterator(int s, int i);

	/**
	 * Get the indices of the action labels for transitions from choice {@code i} of state {@code s}.
	 * These are 0 if unlabelled.
	 */
	PrimitiveIterator.OfInt getTransitionActionIndicesIterator(int s, int i);

	/**
	 * Get the number of initial states.
	 */
	int getNumInitialStates();

	/**
	 * Get the initial states.
	 */
	PrimitiveIterator.OfInt getInitialStates();

	/**
	 * Get the (deterministic) observation for state {@code s}
	 * of a partially observable model (returns -1 for other models).
	 */
	int getStateObservation(int s);

	/**
	 * For interval models, Get the underlying model over {@code Interval<Value>}.
	 */
	ModelAccess<Interval<Value>> getIntervalModel();

	// Wrapper around existing model classes (for now)

	/**
	 * Create a {@link ModelAccess} wrapped around a {@link Model}.
	 * @param model The model
	 */
	static <Value> ModelAccess<Value> wrap(Model<Value> model)
	{
		return new ModelAccess<>() {

			@Override
			public ModelType getModelType()
			{
				return model.getModelType();
			}

			@Override
			public Evaluator<Value> getEvaluator()
			{
				return model.getEvaluator();
			}

			@Override
			public List<Object> getActions()
			{
				return model.getActions();
			}

			@Override
			public List<String> getActionStrings()
			{
				return model.getActionStrings();
			}

			@Override
			public int getNumPlayers()
			{
				return model.getNumPlayers();
			}

			@Override
			public int getNumObservations()
			{
				if (getModelType().partiallyObservable()) {
					return ((PartiallyObservableModel<?>) model).getNumObservations();
				} else {
					return 0;
				}
			}

			@Override
			public int getNumStates()
			{
				return model.getNumStates();
			}

			@Override
			public int getNumChoices(int s)
			{
				if (model instanceof NondetModel) {
					return ((NondetModel<Value>) model).getNumChoices(s);
				} else {
					return 1;
				}
			}

			@Override
			public int getNumTransitions(int s, int i)
			{
				if (model instanceof NondetModel) {
					return ((NondetModel<Value>) model).getNumTransitions(s, i);
				} else {
					// assume i==0
					return model.getNumTransitions(s);
				}
			}

			@Override
			public int getTransitionSuccessor(int s, int i, int j)
			{
				// TODO: Inefficient!
				Iterator<Map.Entry<Integer, Value>> iter = getTransitionsIterator(s, i);
				for (int k = 0; k < j; k++) {
					iter.next();
				}
				return iter.next().getKey();
			}

			@Override
			public Value getExitRate(int s)
			{
				if (model instanceof CTMC) {
					return ((CTMC<Value>) model).getExitRate(s);
				} else {
					return null;
				}
			}

			@Override
			public Iterator<Map.Entry<Integer, Value>> getTransitionsIterator(int s, int i)
			{
				if (model instanceof MDP) {
					return ((MDP<Value>) model).getTransitionsIterator(s, i);
				} else if (model instanceof CTMC) {
					// assume i==0
					return ((CTMC<Value>) model).getEmbeddedTransitionsIterator(s);
				} else if (model instanceof DTMC) {
					// assume i==0
					return ((DTMC<Value>) model).getTransitionsIterator(s);
				} else if (model instanceof LTS) {
					return new SingletonIterator.Of<>(new AbstractMap.SimpleImmutableEntry<>(((LTS<Value>) model).getSuccessor(s, i), (Value) null));
				} else if (model instanceof IMDP) {
					return ((IMDP<Value>) model).getIntervalModel().getTransitionsMappedIterator(s, i, Interval::getLower);
				} else if (model instanceof IDTMC) {
					return ((IDTMC<Value>) model).getIntervalModel().getTransitionsMappedIterator(s, Interval::getLower);
				} else if (model instanceof IPOMDP) {
					return ((IPOMDP<Value>) model).getIntervalModel().getTransitionsMappedIterator(s, i, Interval::getLower);
				} else {
					return null;
				}
			}

			@Override
			public Object getChoiceAction(int s, int i)
			{
				if (model instanceof NondetModel) {
					return ((NondetModel<Value>) model).getAction(s, i);
				} else {
					return null;
				}
			}

			@Override
			public int getChoiceActionIndex(int s, int i)
			{
				if (model instanceof NondetModel) {
					return ((NondetModel<Value>) model).getActionIndex(s, i);
				} else {
					return -1;
				}
			}

			@Override
			public Iterator<Object> getTransitionActionsIterator(int s, int i)
			{
				if (model instanceof DTMC) {
					// assume i==0
					return ((DTMC<Value>) model).getActionsIterator(s);
				} else if (model instanceof IDTMC) {
						// assume i==0
						return ((IDTMC<Value>) model).getIntervalModel().getActionsIterator(s);
				} else {
					return Collections.nCopies(getNumTransitions(s, i), null).iterator();
				}
			}

			@Override
			public PrimitiveIterator.OfInt getTransitionActionIndicesIterator(int s, int i)
			{
				if (model instanceof DTMC) {
					// assume i==0
					return ((DTMC<Value>) model).getActionIndicesIterator(s);
				} else if (model instanceof IDTMC) {
					// assume i==0
					return ((IDTMC<Value>) model).getIntervalModel().getActionIndicesIterator(s);
				} else {
					return Utils.nCopiesIntIterator(getNumTransitions(s, i), -1);
				}
			}

			@Override
			public int getNumInitialStates()
			{
				return model.getNumInitialStates();
			}

			@Override
			public PrimitiveIterator.OfInt getInitialStates()
			{
				return IntIterators.asIntIterator(model.getInitialStates().iterator());
			}

			@Override
			public int getStateObservation(int s)
			{
				if (model instanceof PartiallyObservableModel) {
					return ((PartiallyObservableModel<Value>) model).getObservation(s);
				} else {
					return -1;
				}
			}

			@Override
			public ModelAccess<Interval<Value>> getIntervalModel()
			{
				if (model instanceof IntervalModel){
					return ModelAccess.wrap(((IntervalModel<Value>) model).getIntervalModel());
				} else {
					return null;
				}
			}

		};
	}

	// Various iterators, with default implementations

	/**
	 * Get, for all states, the offset at which its choices start, as an iterator.
	 * The iterator returns {@code }numStates+1} values, with the final value
	 * equalling the total count of all choices. As used in column sparse row format.
	 */
	default PrimitiveIterator.OfInt getStateChoiceOffsets()
	{
		return new ModelAccessIterators.GetStateChoiceCounts<>(this)
		{
			boolean first = true;
			int iCount = 0;

			@Override
			public int nextInt()
			{
				if (first) {
					first = false;
					return 0;
				}
				iCount += super.nextInt();
				return iCount;
			}
		};
	}

	/**
	 * Get, for all states, the offset at which its transitions start, as an iterator.
	 * The iterator returns {@code }numStates+1} values, with the final value
	 * equalling the total count of all transitions. As used in column sparse row format.
	 */
	default PrimitiveIterator.OfInt getStateTransitionOffsets()
	{
		return new ModelAccessIterators.GetStateTransitionCounts<>(this)
		{
			boolean first = true;
			int jCount = 0;

			@Override
			public int nextInt()
			{
				if (first) {
					first = false;
					return 0;
				}
				jCount += super.nextInt();
				return jCount;
			}
		};
	}

	/**
	 * Get, for all choices, the offset at which its transitions start, as an iterator.
	 * The iterator returns {@code }numChoices+1} values, with the final value
	 * equalling the total count of all transitions. As used in column sparse row format.
	 */
	default PrimitiveIterator.OfInt getChoiceTransitionOffsets()
	{
		return new ModelAccessIterators.GetChoiceTransitionCounts<>(this)
		{
			boolean first = true;
			int jCount = 0;

			@Override
			public int nextInt()
			{
				if (first) {
					first = false;
					return 0;
				}
				jCount += super.nextInt();
				return jCount;
			}
		};
	}

	/**
	 * Get the probabilities for all transitions, as an iterator.
	 * For interval models, this returns two probabilities (lower then upper bound) for each transition.
	 */
	default Iterator<Value> getTransitionProbabilities()
	{
		if (getModelType().intervals()) {
			return new ModelAccessIterators.UnpackIntervals<>(getIntervalModel().getTransitionProbabilities());
		} else {
			return new ModelAccessIterators.GetTransitionValues<>(this)
			{
				@Override
				public Value getValue()
				{
					return transition.getValue();
				}
			};
		}
	}

	/**
	 * Get the probabilities for all transitions, as an iterator,
	 * encoded into primitive types (see {@link #valuesToPrimitives(Iterator)}).
	 * For interval models, this returns two probabilities (lower then upper bound) for each transition.
	 */
	default Iterator<?> getTransitionProbabilitiesAsPrimitives()
	{
		return valuesToPrimitives(getTransitionProbabilities());
	}

	/**
	 * Get the successor states for all transitions, as an iterator.
	 */
	default PrimitiveIterator.OfInt getTransitionSuccessors()
	{
		return new ModelAccessIterators.GetTransitionIntValues<>(this)
		{
			@Override
			public int getIntValue()
			{
				return transition.getKey();
			}
		};
	}

	/**
	 * Get the exit rates for all states, as an iterator.
	 */
	default Iterator<Value> getExitRates()
	{
		return new ModelAccessIterators.GetStateValues<>(this)
		{
			@Override
			public Value getValue()
			{
				return model.getExitRate(s);
			}
		};
	}

	/**
	 * Get the exit rates for all states, as an iterator,
	 * encoded into primitive types (see {@link #valuesToPrimitives(Iterator)}).
	 */
	default Iterator<?> getExitRatesAsPrimitives()
	{
		return valuesToPrimitives(getExitRates());
	}

	/**
	 * Get the actions for all choices, as an iterator.
	 */
	default Iterator<Object> getChoiceActions()
	{
		if (getModelType().nondeterministic()) {
			return new ModelAccessIterators.GetChoiceValues<>(this)
			{
				@Override
				public Object getValue()
				{
					return model.getChoiceAction(s, i);
				}
			};
		} else {
			return Collections.nCopies(getNumChoices(), null).iterator();
		}
	}

	/**
	 * Get the action indices for all choices, as an iterator.
	 */
	default PrimitiveIterator.OfInt getChoiceActionIndices()
	{
		if (getModelType().nondeterministic()) {
			return new ModelAccessIterators.GetChoiceIntValues<>(this)
			{
				@Override
				public int getIntValue()
				{
					return model.getChoiceActionIndex(s, i);
				}
			};
		} else {
			return Utils.nCopiesIntIterator(getNumChoices(), -1);
		}
	}

	/**
	 * Get the action indices for all transitions, as an iterator.
	 */
	default PrimitiveIterator.OfInt getTransitionActionIndices()
	{
		return new ModelAccessIterators.GetTransitionIntValues<>(this, true)
		{
			@Override
			public int getIntValue()
			{
				return actionIndex;
			}
		};
	}

	/**
	 * Get the observations for all states, as an iterator.
	 */
	default PrimitiveIterator.OfInt getStateObservations()
	{
		return new ModelAccessIterators.GetStateIntValues<>(this)
		{
			@Override
			public int getIntValue()
			{
				return model.getStateObservation(s);
			}
		};
	}

	/**
	 * Get all state rewards from a {@link Rewards} object, as an iterator.
	 * @param rewards The rewards
	 */
	default <E> Iterator<E> getStateRewards(Rewards<E> rewards)
	{
		return new ModelAccessIterators.GetStateValues<>(this)
		{
			@Override
			public E getValue()
			{
				return rewards.getStateReward(s);
			}
		};
	}

	/**
	 * Get all state rewards from a {@link Rewards} object, as an iterator,
	 * encoded into primitive types (see {@link #valuesToPrimitives(Iterator)}).
	 * @param rewards The rewards
	 */
	default <E> Iterator<?> getStateRewardsAsPrimitives(Rewards<E> rewards)
	{
		return valuesToPrimitives(getStateRewards(rewards));
	}

	/**
	 * Get all transition rewards from a {@link Rewards} object, as an iterator.
	 * @param rewards The rewards
	 */
	default <E> Iterator<E> getTransitionRewards(Rewards<E> rewards)
	{
		if (getModelType().nondeterministic()) {
			return new ModelAccessIterators.GetChoiceValues<>(this)
			{
				@Override
				public E getValue()
				{
					return rewards.getTransitionReward(s, i);
				}
			};
		} else {
			return new ModelAccessIterators.GetTransitionValues<>(this)
			{
				@Override
				public E getValue()
				{
					return rewards.getTransitionReward(s, j);
				}
			};
		}
	}

	/**
	 * Get all transition rewards from a {@link Rewards} object, as an iterator,
	 * encoded into primitive types (see {@link #valuesToPrimitives(Iterator)}).
	 * @param rewards The rewards
	 */
	default <E> Iterator<?> getTransitionRewardsAsPrimitives(Rewards<E> rewards)
	{
		return valuesToPrimitives(getTransitionRewards(rewards));
	}

	// Utility methods

	/**
	 * Convert (continuous numerical) values, supplied via an iterator,
	 * to an encoding into primitive types, also as an iterator.
	 * Double values are returned as-is, while BigRational values are unpacked into pairs of longs
	 * @param values The values to encode
	 */
	default Iterator<?> valuesToPrimitives(Iterator<?> values)
	{
		if (getEvaluator().exact()) {
			return new ModelAccessIterators.UnpackBigRationals((Iterator<BigRational>) values);
		} else {
			return DoubleIterators.asDoubleIterator(values);
		}
	}

	/**
	 * Decode (continuous numerical) values, supplied via a consumer,
	 * from an encoding into primitive types, and pass to a Value consumer.
	 * Double values are passed as-is, while BigRational values are converted to pairs of longs
	 * @param eval Evaluator for the values
	 * @param valueConsumer The consumer for the values to encode
	 */
	static <Value> Consumer<?> primitivesToValues(Evaluator<Value> eval, Consumer<Value> valueConsumer)
	{
		if (eval.exact()) {
			Consumer<BigRational> consumerBigRationals = (Consumer<BigRational>) valueConsumer;
			return new ModelAccessIterators.PackBigRationals(consumerBigRationals);
		} else {
			return valueConsumer;
		}
	}

	// Utility classes

	/**
	 * Helper class to create a list of Values, decoded from their
	 * encoding into primitives (see {@link #valuesToPrimitives(Iterator)}).
	 */
	class ValueListFromPrimitives<Value>
	{
		List<Value> valueList;
		Consumer<?> primitiveConsumer;

		@SuppressWarnings("unchecked")
		ValueListFromPrimitives(Evaluator<Value> eval, int size)
		{
			if (eval.exact()) {
				//LongList valueListLongs = new LongArrayList(numTransitions);
				List<BigRational> valueListBigRationals = new ArrayList<>(size);
				valueList = (List<Value>) valueListBigRationals;
				primitiveConsumer = new ModelAccessIterators.PackBigRationals(valueListBigRationals::add);
			} else {
				DoubleList valueListDoubles = new DoubleArrayList(size);
				valueList = (List<Value>) valueListDoubles;
				primitiveConsumer = (it.unimi.dsi.fastutil.doubles.DoubleConsumer) valueListDoubles::add;
			}
		}
	}
}
