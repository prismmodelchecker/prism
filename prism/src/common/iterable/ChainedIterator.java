//==============================================================================
//	
//	Copyright (c) 2015-
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

package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

/**
 * A helper class implementing an Iterator that chains a sequence of iterators.
 * Returns all the elements of the first iterator, then the elements of the
 * second iterator and so on.
 * <p>
 * The calls to {@code next()} of the underlying iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * This iterator does not support the {@code remove()} method, even if the underlying
 * iterators support it.
 */

public abstract class ChainedIterator<E, I extends Iterator<E>> implements FunctionalIterator<E>
{
	/** An iterator for the sequence of iterators that will be chained */
	protected Iterator<? extends I> iterators;
	/** The current iterator in the sequence of iterators */
	protected I current;

	/**
	 * Constructor for chaining a variable number of Iterators.
	 * @param iterators a variable number of Iterator to be chained */
	@SafeVarargs
	public ChainedIterator(I... iterators)
	{
		if (iterators.length > 0) {
			this.iterators = new ArrayIterator.Of<>(iterators, 1, iterators.length);
			this.current   = iterators[0];
		} else {
			// set empty instances;
			release();
		}
	}

	/**
	 * Constructor for chaining a variable number of Iterators.
	 * @param iterators a variable number of Iterator to be chained */
	public ChainedIterator(Iterator<? extends I>iterators)
	{
		if (iterators.hasNext()) {
			this.iterators = iterators;
			this.current   = iterators.next();
		} else {
			// set empty instances;
			release();
		}
	}

	/**
	 * Constructor for chaining a variable number of Iterators.
	 * @param iterator  an Iterator to prepend
	 * @param iterators a variable number of Iterator to be chained */
	@SafeVarargs
	public ChainedIterator(I iterator, I... iterators)
	{
		this(iterator, new ArrayIterator.Of<>(iterators));
	}

	/**
	 * Constructor for chaining Iterator, with the sequence provided by an Iterators.
	 * @param iterator  an Iterator to prepend
	 * @param iterators an Iterator the provides the sequence of Iterator to be chained
	 **/
	public ChainedIterator(I iterator, Iterator<? extends I> iterators)
	{
		this.iterators = iterators;
		this.current   = iterator;
	}

	@Override
	public boolean hasNext()
	{
		if (current.hasNext()) {
			// the current iterator has another element
			return true;
		}

		// the current iterator has no more elements,
		// search for the next iterator that as an element
		while (iterators.hasNext()) {
			// consider the next iterator
			current = iterators.next();
			if (current.hasNext()) {
				// iterator has element, keep current and return true
				return true;
			}
		}
		// there are no more iterators / elements
		release();
		return false;
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action)
	{
		current.forEachRemaining(action);
		iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
		release();
	}

	@Override
	public int count()
	{
		int count = 0;
		while (hasNext()) {
			if (current instanceof FunctionalIterator) {
				count += ((FunctionalIterator<E>) current).count();
				continue;
			}
			while (current.hasNext()) {
				current.next();
				count++;
			}
		}
		release();
		return count;
	}

	@Override
	public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		Objects.requireNonNull(accumulator);
		T result = identity;
		while (hasNext()) {
			if (current instanceof FunctionalIterator) {
				result = ((FunctionalIterator<? extends E>) current).reduce(result, accumulator);
				continue;
			}
			while (current.hasNext()) {
				E next = current.next();
				result = accumulator.apply(result, next);
			}
		}
		release();
		return result;
	}

	protected void requireNext()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}

	protected void release() {
		iterators = EmptyIterator.Of();
	}



	public static class Of<E> extends ChainedIterator<E, Iterator<E>>
	{
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public Of(Iterator<? extends E>... iterators)
		{
			super((Iterator<E>[]) iterators);
		}

		@SuppressWarnings("unchecked")
		public Of(Iterator<? extends Iterator<? extends E>> iterators)
		{
			super((Iterator<? extends Iterator<E>>) iterators);
		}

		@SuppressWarnings("unchecked")
		@SafeVarargs
		public Of(Iterator<? extends E> iterator, Iterator<? extends E>... iterators)
		{
			super((Iterator<E>) iterator, (Iterator<E>[]) iterators);
		}

		@SuppressWarnings("unchecked")
		public Of(Iterator<? extends E> iterator, Iterator<? extends Iterator<? extends E>> iterators) {
			super((Iterator<E>) iterator, (Iterator<? extends Iterator<E>>) iterators);
		}

		@Override
		public E next()
		{
			requireNext();
			return current.next();
		}

		@Override
		protected void release()
		{
			super.release();
			current = EmptyIterator.Of();
		}
	}



	public static class OfDouble extends ChainedIterator<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		@SafeVarargs
		public OfDouble(PrimitiveIterator.OfDouble... iterators)
		{
			super(iterators);
		}

		public OfDouble(Iterator<? extends PrimitiveIterator.OfDouble> iterators)
		{
			super(iterators);
		}

		@SafeVarargs
		public OfDouble(PrimitiveIterator.OfDouble iterator, PrimitiveIterator.OfDouble... iterators)
		{
			super(iterator, iterators);
		}

		public OfDouble(PrimitiveIterator.OfDouble iterator, Iterator<PrimitiveIterator.OfDouble> iterators) {
			super(iterator, iterators);
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return current.nextDouble();
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			current.forEachRemaining(action);
			iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
			release();
		}

		@Override
		public boolean contains(double d)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfDouble) current).contains(d)) {
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if (d == current.nextDouble()) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			int count = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					count += ((FunctionalPrimitiveIterator.OfDouble) current).count();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					current.nextDouble();
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfDouble) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					double next = current.nextDouble();
					result      = accumulator.apply(result, next);
				}
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfDouble) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					double next = current.nextDouble();
					result      = accumulator.applyAsDouble(result, next);
				}
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			super.release();
			current = EmptyIterator.OfDouble();
		}
	}



	public static class OfInt extends ChainedIterator<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		@SafeVarargs
		public OfInt(PrimitiveIterator.OfInt... iterators)
		{
			super(iterators);
		}

		public OfInt(Iterator<? extends PrimitiveIterator.OfInt> iterators)
		{
			super(iterators);
		}

		@SafeVarargs
		public OfInt(PrimitiveIterator.OfInt iterator, PrimitiveIterator.OfInt... iterators)
		{
			super(iterator, iterators);
		}

		public OfInt(PrimitiveIterator.OfInt iterator, Iterator<PrimitiveIterator.OfInt> iterators) {
			super(iterator, iterators);
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return current.nextInt();
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			current.forEachRemaining(action);
			iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
			release();
		}

		@Override
		public boolean contains(int d)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfInt) current).contains(d)) {
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if (d == current.nextInt()) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			int count = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					count += ((FunctionalPrimitiveIterator.OfInt) current).count();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					current.nextInt();
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfInt) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					int next = current.nextInt();
					result      = accumulator.apply(result, next);
				}
			}
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfInt) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					int next = current.nextInt();
					result   = accumulator.applyAsInt(result, next);
				}
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			super.release();
			current = EmptyIterator.OfInt();
		}
	}



	public static class OfLong extends ChainedIterator<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		@SafeVarargs
		public OfLong(PrimitiveIterator.OfLong... iterators)
		{
			super(iterators);
		}

		public OfLong(Iterator<? extends PrimitiveIterator.OfLong> iterators)
		{
			super(iterators);
		}

		@SafeVarargs
		public OfLong(PrimitiveIterator.OfLong iterator, PrimitiveIterator.OfLong... iterators)
		{
			super(iterator, iterators);
		}

		public OfLong(PrimitiveIterator.OfLong iterator, Iterator<PrimitiveIterator.OfLong> iterators) {
			super(iterator, iterators);
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return current.nextLong();
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			current.forEachRemaining(action);
			iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
			release();
		}

		@Override
		public boolean contains(long d)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfLong) current).contains(d)) {
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if (d == current.nextLong()) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			int count = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					count += ((FunctionalPrimitiveIterator.OfLong) current).count();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					current.nextLong();
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfLong) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					long next = current.nextLong();
					result      = accumulator.apply(result, next);
				}
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfLong) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					long next = current.nextLong();
					result    = accumulator.applyAsLong(result, next);
				}
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			super.release();
			current = EmptyIterator.OfLong();
		}
	}
}
