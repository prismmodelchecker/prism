//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
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


package explicit;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import common.iterable.FilteringIterator;
import common.iterable.SingletonIterator;

/**
 * Base class and static helpers for iterators over successor states.
 * <br>
 * Carries information whether the successors are known to be distinct
 * and helpers to ensure that this is the case, i.e., allowing iteration
 * over the set of successors or the multiset of successors. The latter
 * might have better performance, as no deduplication is needed.
  */
public abstract class SuccessorsIterator implements PrimitiveIterator.OfInt
{
	/** Is it guaranteed that every successor will occur only once? */
	public abstract boolean successorsAreDistinct();

	@Override
	public abstract boolean hasNext();

	@Override
	public abstract int nextInt();

	/**
	 * Return a SuccessorsIterator that guarantees that there
	 * are no duplicates in the successor states, i.e., iterating
	 * over the set of successors instead of a multiset.
	 * <br>
	 * The iterator this method is called on can not be used afterwards,
	 * only the returned iterator.
	 */
	public SuccessorsIterator distinct()
	{
		if (successorsAreDistinct()) {
			return this;
		} else {
			return new SuccessorsIteratorFromOfInt(FilteringIterator.dedupe(this), true);
		}
	}

	/** Provide an IntStream */
	public IntStream stream()
	{
		return StreamSupport.intStream(
				Spliterators.spliteratorUnknownSize(this, successorsAreDistinct() ? Spliterator.DISTINCT : 0),
				false);
	}

	/** Wrapper, underlying iterator is an OfInt iterator */
	private static class SuccessorsIteratorFromOfInt extends SuccessorsIterator {
		private java.util.PrimitiveIterator.OfInt it;
		private boolean distinct;

		public SuccessorsIteratorFromOfInt(PrimitiveIterator.OfInt it, boolean distinct)
		{
			this.it = it;
			this.distinct = distinct;
		}

		@Override
		public boolean successorsAreDistinct()
		{
			return distinct;
		}

		@Override
		public boolean hasNext()
		{
			return it.hasNext();
		}

		@Override
		public int nextInt()
		{
			return it.nextInt();
		}
	}

	/** Wrapper, underlying iterator is an Iterator<Integer> iterator */
	private static class SuccessorsIteratorFromIterator extends SuccessorsIterator {
		private Iterator<Integer> it;
		private boolean distinct;

		public SuccessorsIteratorFromIterator(Iterator<Integer> it, boolean distinct)
		{
			this.it = it;
			this.distinct = distinct;
		}

		@Override
		public boolean hasNext()
		{
			return it.hasNext();
		}

		@Override
		public Integer next()
		{
			return it.next();
		}

		@Override
		public int nextInt()
		{
			return it.next();
		}

		@Override
		public boolean successorsAreDistinct()
		{
			return distinct;
		}
	};

	/** Helper, empty iterator */
	private static class SuccessorsIteratorEmpty extends SuccessorsIterator {
		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public int nextInt()
		{
			throw new NoSuchElementException();
		}

		@Override
		public boolean successorsAreDistinct()
		{
			return true;
		}
	};

	/** Helper, chain multiple SuccessorsIterators */
	private static class ChainedSuccessorsIterator extends SuccessorsIterator {
		private Iterator<SuccessorsIterator> iterators;
		private SuccessorsIterator current;
		private boolean distinct;

		public ChainedSuccessorsIterator(Iterator<SuccessorsIterator> iterators)
		{
			this.iterators = iterators;
			current = iterators.hasNext() ? iterators.next() : null;
			if (current != null && !iterators.hasNext()) {
				// only a single successor iterator, can inherit elementsAreDistinct
				distinct = current.successorsAreDistinct();
			} else {
				// can not guarantee that successors are distinct
				distinct = false;
			}
		}

		@Override
		public boolean hasNext()
		{
			if (current == null) {
				return false;
			}

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
			current = null;
			return false;
		}

		@Override
		public int nextInt()
		{
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return current.nextInt();
		}

		@Override
		public boolean successorsAreDistinct()
		{
			return distinct;
		}
	};

	/** Obtain a SuccessorsIterator with the given distinctness guarantee from an Iterator<Integer> */
	public static SuccessorsIterator from(Iterator<Integer> it, boolean distinctElements)
	{
		return new SuccessorsIteratorFromIterator(it, distinctElements);
	}

	/** Obtain a SuccessorsIterator with the given distinctness guarantee from an OfInt */
	public static SuccessorsIterator from(PrimitiveIterator.OfInt it, boolean distinctElements)
	{
		return new SuccessorsIteratorFromOfInt(it, distinctElements);
	}

	/** Obtain a SuccessorsIterator for a single state */
	public static SuccessorsIterator fromSingleton(int i)
	{
		return new SuccessorsIteratorFromOfInt(new SingletonIterator.OfInt(i), true);
	}

	/** Obtain an empty SuccessorsIterator */
	public static SuccessorsIterator empty()
	{
		return new SuccessorsIteratorEmpty();
	}

	/** Obtain a SuccessorsIterator, chaining multiple SuccessorsIterators one after the other */
	public static SuccessorsIterator chain(Iterator<SuccessorsIterator> iterators)
	{
		return new ChainedSuccessorsIterator(iterators);
	}
}
