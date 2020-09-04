//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

package common;

import java.util.BitSet;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import common.IterableBitSet;
import common.iterable.FunctionalPrimitiveIterable;
import common.iterable.FunctionalPrimitiveIterator;
import common.iterable.SingletonIterable;

/**
 * Interface for an ordered set of integers that allows efficient
 * (1) iteration (normal and reversed order)
 * and (2) efficient tests of set membership.
 * <br>
 * Provides static helpers for wrapping a BitSet or a singleton value.
 */
public interface IntSet extends FunctionalPrimitiveIterable.OfInt
{
	/** Return a FunctionalPrimitiveIterator.OfInt iterator for iteration, reversed order */
	FunctionalPrimitiveIterator.OfInt reversedIterator();

	/** Return the cardinality (number of elements) for this set */
	default long cardinality()
	{
		return count();
	}

	/**
	 * Return true if {@code index} is a member of this set
	 * (convenience method to ease migration from use of BitSet).
	 * <p>
	 * <i>Default implementation</i>: Calls contains(index).
	 */
	default boolean get(int index)
	{
		return contains(index);
	}

	/**
	 * Return true if this set contains the {@code other}
	 * set.
	 * <p>
	 * <i>Default implementation</i>:
	 * Tests via contains for all elements of other.
	 */
	default boolean contains(IntSet other)
	{
		return other.allMatch((IntPredicate) this::contains);
	}

	/**
	 * Return true if this set contains the {@code other}
	 * set.
	 * <p>
	 * <i>Default implementation</i>:
	 * Uses contains(IntSet other).
	 */
	default boolean contains(BitSet other)
	{
		return contains(asIntSet(other));
	}

	/**
	 * Produce an IntStream for this set.
	 * <p>
	 * <i>Default implementation</i>:
	 * Wrap iterator() into an intStream.
	 */
	default IntStream stream()
	{
		return StreamSupport.intStream(
				() -> spliterator(),
				Spliterator.SIZED | Spliterator.DISTINCT,
				false);
	}

	@Override
	default Spliterator.OfInt spliterator()
	{
		return Spliterators.spliterator(
				iterator(),
				cardinality(),
				Spliterator.SIZED | Spliterator.DISTINCT);
	}

	/** Return this set as a String */
	@Override
	default String asString()
	{
		// can't overload toString() with a default method in interface
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		boolean first = true;
		for (PrimitiveIterator.OfInt it = iterator(); it.hasNext(); ) {
			if (!first)
				sb.append(",");
			first = false;
			sb.append(it.nextInt());
		}
		sb.append("}");
		return sb.toString();
	}



	/**
	 * Wrapper class for obtaining an IntSet from a BitSet.
	 * <p>
	 * Note: The BitSet should not be modified as long as the
	 * derived IntSet is in use.
	 */
	static class IntSetFromBitSet implements IntSet
	{
		/** The wrapped BitSet */
		protected BitSet bs;
		/** The cardinality of the underlying BitSet (cached, -1 = not yet computed) */
		int cardinality = -1;

		/** Constructor */
		public IntSetFromBitSet(BitSet bs)
		{
			Objects.requireNonNull(bs);
			this.bs = bs;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return IterableBitSet.getSetBits(bs).iterator();
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt reversedIterator()
		{
			return IterableBitSet.getSetBitsReversed(bs).iterator();
		}

		@Override
		public IntStream stream()
		{
			return bs.stream();
		}

		@Override
		public long count()
		{
			// not yet computed?
			if (cardinality == -1)
				cardinality = bs.cardinality();

			return cardinality;
		}

		@Override
		public boolean contains(int index)
		{
			return bs.get(index);
		}

		@Override
		public String toString()
		{
			return asString();
		}
	};



	/** Convenience class for simulating a singleton set */
	static class SingletonIntSet extends SingletonIterable.OfInt implements IntSet
	{
		/**
		 * Constructor.
		 * @param singleMember the single member of this set
		 */
		public SingletonIntSet(int singleMember)
		{
			super(singleMember);
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt reversedIterator()
		{
			// iteration order does not matter for singleton set
			return iterator();
		}

		@Override
		public String toString()
		{
			return "{" + element + "}";
		}
	}

	/**
	 * Factory method for obtaining an IntSet from a BitSet
	 * <p>
	 * Note: The BitSet should not be modified as long as the derived IntSet is in use.
	 *
	 * @param bs The underlying BitSet
	 */
	static IntSet asIntSet(BitSet bs)
	{
		return new IntSetFromBitSet(bs);
	}

	/**
	 * Factory method for obtaining an IntSet for a singleton set.
	 *
	 * @param singleMember The single member of the singleton set
	 */
	static IntSet asIntSet(int singleMember)
	{
		return new SingletonIntSet(singleMember);
	}
}
