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
import it.unimi.dsi.fastutil.longs.LongConsumer;
import param.BigRational;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;

public class ModelAccessIterators
{
	// Iterators over states

	/**
	 * An iterator over values of type {@code E} for each state of a {@link ModelAccess<Value>}.
	 */
	public abstract static class GetStateValues<Value, E> implements Iterator<E>
	{
		final ModelAccess<Value> model;
		final int numStates;
		int s; // current state: s<numStates or hasNext = false
		boolean hasNext;
		int numChoices;
		E value;

		GetStateValues(ModelAccess<Value> model)
		{
			this.model = model;
			numStates = model.getNumStates();
			hasNext = numStates > 0;
		}

		/**
		 * Increment iterator counters
		 */
		protected void incr()
		{
			s++;
			if (s >= numStates) {
				hasNext = false;
			}
		}

		/**
		 * Get the value for the current state.
		 */
		protected abstract E getValue();

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public E next()
		{
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			value = getValue();
			incr();
			return value;
		}
	}

	/**
	 * An iterator over integer values for each state of a {@link ModelAccess<Value>}.
	 */
	public abstract static class GetStateIntValues<Value> extends GetStateValues<Value,Integer> implements PrimitiveIterator.OfInt
	{
		int intValue;

		GetStateIntValues(ModelAccess<Value> model)
		{
			super(model);
		}

		/**
		 * Get the integer value for the current state.
		 */
		protected abstract int getIntValue();

		@Override
		public Integer getValue()
		{
			return getIntValue();
		}

		@Override
		public int nextInt()
		{
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			intValue = getIntValue();
			incr();
			return intValue;
		}
	}

	/**
	 * An iterator over the choice counts for each state of a {@link ModelAccess<Value>}.
	 */
	public static class GetStateChoiceCounts<Value> extends ModelAccessIterators.GetStateIntValues<Value>
	{
		GetStateChoiceCounts(ModelAccess<Value> model)
		{
			super(model);
		}

		@Override
		public int getIntValue()
		{
			return model.getNumChoices(s);
		}
	}

	/**
	 * An iterator over the transition counts for each state of a {@link ModelAccess<Value>}.
	 * Deals with empty (no choice) states.
	 */
	public static class GetStateTransitionCounts<Value> extends ModelAccessIterators.GetStateIntValues<Value>
	{
		GetStateTransitionCounts(ModelAccess<Value> model)
		{
			super(model);
		}

		@Override
		public int getIntValue()
		{
			return model.getNumTransitions(s);
		}
	}

	// Iterators over choices

	/**
	 * An iterator over values of type {@code E} for each choice of a {@link ModelAccess<Value>}.
	 * Deals with empty (no choice) states.
	 */
	public abstract static class GetChoiceValues<Value, E> implements Iterator<E>
	{
		final ModelAccess<Value> model;
		final int numStates;
		int s; // current state: s<numStates or hasNext = false
		int numChoices; // num choices in state s (0 if s==numStates)
		int i; // current choice: i<numChoices or i==0==numChoices
		boolean hasNext;
		E value;

		GetChoiceValues(ModelAccess<Value> model)
		{
			this.model = model;
			numStates = model.getNumStates();
			numChoices = numStates > 0 ? model.getNumChoices(0) : 0;
			hasNext = numStates > 0;
			skipEmpty();
		}

		/**
		 * Advance to the next available choice, skipping empty states.
		 * After this, either {@code (s,i)} is the next available choice,
		 * or there are no more and {@code hasNext} is false.
		 */
		protected void skipEmpty()
		{
			while (hasNext) {
				if (s < numStates && numChoices > 0) {
					return;
				}
				incr();
			}
		}

		/**
		 * Increment iterator counters
		 */
		protected void incr()
		{
			i++;
			if (i >= numChoices) {
				s++;
				if (s >= numStates) {
					hasNext = false;
					return;
				}
				numChoices = model.getNumChoices(s);
				i = 0;
			}
		}

		/**
		 * Get the value for the current choice.
		 */
		protected abstract E getValue();

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public E next()
		{
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			value = getValue();
			incr();
			skipEmpty();
			return value;
		}
	}

	/**
	 * An iterator over integer values for each choice of a {@link ModelAccess<Value>}.
	 */
	public abstract static class GetChoiceIntValues<Value> extends GetChoiceValues<Value,Integer> implements PrimitiveIterator.OfInt
	{
		int intValue;

		GetChoiceIntValues(ModelAccess<Value> model)
		{
			super(model);
		}

		/**
		 * Get the integer value for the current state.
		 */
		protected abstract int getIntValue();

		@Override
		public Integer getValue()
		{
			return getIntValue();
		}

		@Override
		public int nextInt()
		{
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			intValue = getIntValue();
			incr();
			return intValue;
		}
	}

	/**
	 * An iterator over the transition counts for each choice of a {@link ModelAccess<Value>}.
	 * Deals with empty (no choice) states.
	 */
	public static class GetChoiceTransitionCounts<Value> extends ModelAccessIterators.GetChoiceIntValues<Value>
	{
		GetChoiceTransitionCounts(ModelAccess<Value> model)
		{
			super(model);
		}

		@Override
		public int getIntValue()
		{
			return model.getNumTransitions(s, i);
		}
	}

	// Iterators over transitions

	/**
	 * An iterator over values associated to each transition of a {@link ModelAccess<Value>}.
	 * Deals with empty (no choice) states amd empty (no transition) choices.
	 */
	public abstract static class GetTransitionValues<Value,E> implements Iterator<E>
	{
		final ModelAccess<Value> model;
		final boolean storeActions;
		final int numStates;
		int s; // current state: s<numStates or hasNext = false
		int numChoices; // num choices in state s (0 if s==numStates)
		int i; // current choice: i<numChoices or i==0==numChoices
		Iterator<Map.Entry<Integer, Value>> itTransitions; // transitions iterator (null if numChoices==0)
		PrimitiveIterator.OfInt itActionIndices; // (optional) transition action indices iterator (null if numChoices==0)
		int j; // current transition index
		Map.Entry<Integer, Value> transition; // current transition
		int actionIndex; // (optional) current transition action index
		boolean hasNext;
		E value;

		GetTransitionValues(ModelAccess<Value> model)
		{
			this(model, false);
		}

		GetTransitionValues(ModelAccess<Value> model, boolean storeActions)
		{
			this.model = model;
			this.storeActions = storeActions;
			numStates = model.getNumStates();
			numChoices = numStates > 0 ? model.getNumChoices(0) : 0;
			itTransitions = numChoices > 0 ? model.getTransitionsIterator(s, i) : null;
			if (storeActions) {
				itActionIndices = numChoices > 0 ? model.getTransitionActionIndicesIterator(s, i) : null;
			}
			hasNext = numStates > 0;
			skipEmpty();
		}

		/**
		 * Advance to the next available transition, skipping empty states/choices.
		 * After this, either {@code itTransitions.next()} gives the next transition,
		 * or there are no more and {@code hasNext} is false.
		 */
		protected void skipEmpty()
		{
			while (hasNext) {
				if (s < numStates && numChoices > 0 && itTransitions.hasNext()) {
					return;
				}
				incr();
			}
		}

		/**
		 * Increment iterator counters
		 */
		protected void incr()
		{
			i++;
			if (i >= numChoices) {
				s++;
				if (s >= numStates) {
					hasNext = false;
					return;
				}
				numChoices = model.getNumChoices(s);
				i = 0;
			}
			itTransitions = numChoices > 0 ? model.getTransitionsIterator(s, i) : null;
			if (storeActions) {
				itActionIndices = numChoices > 0 ? model.getTransitionActionIndicesIterator(s, i) : null;
			}
			j = 0;
		}

		/**
		 * Get the value for the current transition.
		 */
		protected abstract E getValue();

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public E next()
		{
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			// Store value for transition and move on to the next one
			transition = itTransitions.next();
			if (storeActions) {
				actionIndex = itActionIndices.next();
			}
			value = getValue();
			if (!itTransitions.hasNext()) {
				incr();
				skipEmpty();
			} else {
				j++;
			}
			return value;
		}
	}

	/**
	 * An iterator over integer values for each transition of a {@link ModelAccess<Value>}.
	 */
	public abstract static class GetTransitionIntValues<Value> extends GetTransitionValues<Value,Integer> implements PrimitiveIterator.OfInt
	{
		int intValue;

		GetTransitionIntValues(ModelAccess<Value> model)
		{
			super(model);
		}

		GetTransitionIntValues(ModelAccess<Value> model, boolean storeActions)
		{
			super(model, storeActions);
		}

		/**
		 * Get the integer value for the current state.
		 */
		protected abstract int getIntValue();

		@Override
		public Integer getValue()
		{
			return getIntValue();
		}

		@Override
		public int nextInt()
		{
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			// Store value for transition and move on to the next one
			transition = itTransitions.next();
			if (storeActions) {
				actionIndex = itActionIndices.next();
			}
			intValue = getIntValue();
			if (!itTransitions.hasNext()) {
				incr();
				skipEmpty();
			}
			return intValue;
		}
	}

	/**
	 * Class to unpack intervals of values, supplied as an iterator, and return
	 * as an iterator over all values, i.e., both the lower and upper bounds.
	 */
	public static class UnpackIntervals<V> implements Iterator<V>
	{
		Iterator<Interval<V>> iter;
		Interval<V> next;
		boolean first = true;

		public UnpackIntervals(Iterator<Interval<V>> iter)
		{
			this.iter = iter;
		}

		@Override
		public boolean hasNext()
		{
			return !first || iter.hasNext();
		}

		@Override
		public V next()
		{
			if (first) {
				next = iter.next();
				first = false;
				return next.getLower();
			} else {
				first = true;
				return next.getUpper();
			}
		}
	}

	/**
	 * Class to unpack big rationals, supplied as an iterator, and return
	 * as an iterator over longs, i.e., both the numerator and denominator.
	 */
	public static class UnpackBigRationals implements PrimitiveIterator.OfLong
	{
		Iterator<BigRational> iter;
		BigRational next;
		boolean first = true;

		public UnpackBigRationals(Iterator<BigRational> iter)
		{
			this.iter = iter;
		}

		@Override
		public boolean hasNext()
		{
			return !first || iter.hasNext();
		}

		@Override
		public long nextLong()
		{
			if (first) {
				next = iter.next();
				first = false;
				return next.getNum().longValueExact();
			} else {
				first = true;
				return next.getDen().longValueExact();
			}
		}
	}

	/**
	 * Class to provide a list of big rationals, provided as a list of longs
	 * representing the numerators and denominators, successively.
	 */
	public static class PackBigRationals implements LongConsumer
	{
		Consumer<BigRational> cons;
		long num;
		boolean first = true;

		public PackBigRationals(Consumer<BigRational> cons)
		{
			this.cons = cons;
		}

		@Override
		public void accept(long value)
		{
			if (first) {
				num = value;
				first = false;
			} else {
				first = true;
				cons.accept(new BigRational(num, value));
			}
		}
	}
}
