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

package common;

import java.util.BitSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import common.IterableBitSet;

/**
 * Interface for an ordered set of integers that allows efficient
 * (1) iteration (normal and reversed order)
 * and (2) efficient tests of set membership.
 * <br>
 * Provides static helpers for wrapping a BitSet or a singleton value.
 */
public interface IntSet extends Iterable<Integer>
{
	/** Return a PrimitiveIterator.OfInt iterator for iteration, normal order */
	public OfInt iterator();

	/** Return a PrimitiveIterator.OfInt iterator for iteration, reversed order */
	public OfInt reversedIterator();

	/** Return the cardinality (number of elements) for this set */
	public int cardinality();

	/** Return true if {@code index} is a member of this set */
	public boolean contains(int index);

	/**
	 * Return true if {@code index} is a member of this set
	 * (convenience method to ease migration from use of BitSet).
	 * <p>
	 * <i>Default implementation</i>: Calls contains(index).
	 */
	public default boolean get(int index)
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
	public default boolean contains(IntSet other)
	{
		return other.stream().allMatch(this::contains);
	}

	/**
	 * Return true if this set contains the {@code other}
	 * set.
	 * <p>
	 * <i>Default implementation</i>:
	 * Uses contains(IntSet other).
	 */
	public default boolean contains(BitSet other)
	{
		return contains(asIntSet(other));
	}

	/**
	 * Produce an IntStream for this set.
	 * <p>
	 * <i>Default implementation</i>:
	 * Wrap iterator() into an intStream.
	 */
	public default IntStream stream() {
		return StreamSupport.intStream(
				() -> Spliterators.spliterator(
						iterator(), cardinality(),
						Spliterator.DISTINCT),
				Spliterator.SIZED | Spliterator.DISTINCT,
				false);
	}

	/** Return this set as a String */
	public default String asString()
	{
		// can't overload toString() with a default method in interface
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		boolean first = true;
		for (OfInt it = iterator(); it.hasNext(); ) {
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
	public static class IntSetFromBitSet implements IntSet
	{
		/** The wrapped BitSet */
		private BitSet bs;
		/** The cardinality of the underlying BitSet (cached, -1 = not yet computed) */
		int cardinality = -1;

		/** Constructor */
		public IntSetFromBitSet(BitSet bs)
		{
			this.bs = bs;
		}

		@Override
		public OfInt iterator()
		{
			return IterableBitSet.getSetBits(bs).iterator();
		}

		@Override
		public OfInt reversedIterator()
		{
			return IterableBitSet.getSetBitsReversed(bs).iterator();
		}

		@Override
		public IntStream stream()
		{
			return bs.stream();
		}

		@Override
		public int cardinality()
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
	public static class SingletonIntSet implements IntSet
	{
		/** The single member of this singleton set */
		private int singleMember;

		/**
		 * Constructor.
		 * @param singleMember the single member of this set
		 */
		public SingletonIntSet(int singleMember)
		{
			this.singleMember = singleMember;
		}

		@Override
		public OfInt iterator()
		{
			return new OfInt() {
				boolean done = false;
				@Override
				public boolean hasNext()
				{
					return !done;
				}
				@Override
				public int nextInt()
				{
					done = true;
					return singleMember;
				}
			};
		}

		@Override
		public OfInt reversedIterator()
		{
			// iteration order does not matter for singleton set
			return iterator();
		}

		@Override
		public int cardinality()
		{
			return 1;
		}

		@Override
		public boolean contains(int index)
		{
			return index == singleMember;
		}

		@Override
		public String toString()
		{
			return "{" + singleMember + "}";
		}

	}

	/**
	 * Static constructor for obtaining an IntSet from a BitSet
	 * <p>
	 * Note: The BitSet should not be modified as long as the derived IntSet is in use.
	 * @param bs The underlying BitSet
	 */
	public static IntSet asIntSet(BitSet bs)
	{
		return new IntSetFromBitSet(bs);
	}

	/**
	 * Static constructor for obtaining an IntSet for a singleton set.
	 * @param singleMember The single member of the singleton set
	 */
	public static IntSet asIntSet(int singleMember)
	{
		return new SingletonIntSet(singleMember);
	}

}

