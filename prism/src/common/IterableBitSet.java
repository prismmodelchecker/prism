//==============================================================================
//	
//	Copyright (c) 2014-
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

package common;

import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;

import common.iterable.IterableInt;

/**
 * Convenience class to loop easily over the set/clear bits of a BitSet.
 *
 * For example:<br/><br/>
 * <code>for (Integer index : getSetBits(set)) { ... }</code><br/>
 */
public class IterableBitSet implements IterableInt
{
	private BitSet set;
	private boolean clearBits = false;
	private boolean reversed = false;
	private Integer maxIndex = null;

	/**
	 * Constructor for an Iterable that iterates over the set bits of {@code set}
	 */
	public IterableBitSet(BitSet set)
	{
		this.set = set;
		this.clearBits = false;
		this.reversed = false;
	}

	/**
	 * Constructor for an Iterable that iterates over bits of the given set {@code set},
	 * up to the maximal index given by {@code maxIndex} (optional).
	 * If {@code clearBits} is {@code true}, iterate over the cleared bits instead of the set bits
	 * (requires {@code maxIndex} to be non-null).
	 * @param set the underlying BitSet
	 * @param maxIndex the maximal index for iteration (negative = iterate over the empty set, {@code null} = no restrictions)
	 * @param clearBits if true, iterate over the cleared bits in the BitSet
	 */
	public IterableBitSet(BitSet set, Integer maxIndex, boolean clearBits)
	{
		this(set, maxIndex, clearBits, false);
	}

	/**
	 * Constructor for an Iterable that iterates over bits of the given set {@code set},
	 * up to the maximal index given by {@code maxIndex} (optional).
	 * If {@code clearBits} is {@code true}, iterate over the cleared bits instead of the set bits
	 * (requires {@code maxIndex} to be non-null).
	 * If {@code reversed} is set, iterate in reverse order (highest to lowest).
	 * @param set the underlying BitSet
	 * @param maxIndex the maximal index for iteration (negative = iterate over the empty set, {@code null} = no restrictions)
	 * @param clearBits if true, iterate over the cleared bits in the BitSet
	 * @param reversed if true, iterate in reversed order
	 */
	public IterableBitSet(BitSet set, Integer maxIndex, boolean clearBits, boolean reversed)
	{
		this.set = set;
		this.maxIndex = maxIndex;
		this.clearBits = clearBits;
		this.reversed = reversed;
	}

	/** Implementation of the iterator over the set bits */
	private class SetBitsIterator implements OfInt
	{
		private int current = -1;
		private int next = set.nextSetBit(0);

		@Override
		public boolean hasNext()
		{
			if (maxIndex != null && next > maxIndex) {
				// limit to 0 ... maxIndex
				return false;
			}
			return next >= 0;
		}

		@Override
		public int nextInt()
		{
			if (hasNext()) {
				current = next;
				next = set.nextSetBit(current + 1);
				return current;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			set.clear(current);
		}
	}

	/** Implementation of the iterator over the set bits (reverse order) */
	private class SetBitsReversedIterator implements OfInt
	{
		private int current = -1;
		private int next = set.nextSetBit(0);

		public SetBitsReversedIterator()
		{
			current = -1;
			if (maxIndex != null) {
				// only consider set bits with index <= maxIndex
				next = set.previousSetBit(maxIndex);
			} else {
				next = set.length() - 1;  // highest set bit
			}
		}

		@Override
		public boolean hasNext()
		{
			return next >= 0;
		}

		@Override
		public int nextInt()
		{
			if (hasNext()) {
				current = next;
				next = set.previousSetBit(current - 1);
				return current;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			set.clear(current);
		}
	}

	/** Implementation of the iterator over the cleared bits, requires that {@code maxIndex != null} */
	private class ClearBitsIterator implements OfInt
	{
		private int current = -1;
		private int next = set.nextClearBit(0);

		@Override
		public boolean hasNext()
		{
			if (next > maxIndex) {
				// limit to 0 ... maxIndex
				return false;
			}
			return true;
		}

		@Override
		public int nextInt()
		{
			if (hasNext()) {
				current = next;
				next = set.nextClearBit(current + 1);
				return current;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			set.set(current);
		}
	}

	/** Implementation of the iterator over the clear bits (reverse order), requires that {@code maxIndex != null} */
	private class ClearBitsReversedIterator implements OfInt
	{
		private int current = -1;
		private int next = set.nextSetBit(0);

		public ClearBitsReversedIterator()
		{
			current = -1;
			// only consider clear bits with index <= maxIndex
			next = set.previousClearBit(maxIndex);
		}

		@Override
		public boolean hasNext()
		{
			return next >= 0;
		}

		@Override
		public int nextInt()
		{
			if (hasNext()) {
				current = next;
				next = set.previousClearBit(current - 1);
				return current;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			set.clear(current);
		}
	}

	@Override
	public OfInt iterator()
	{
		if (clearBits == false) {
			if (reversed) {
				return new SetBitsReversedIterator();
			} else {
				return new SetBitsIterator();
			}
		} else {
			if (reversed) {
				return new ClearBitsReversedIterator();
			} else {
				return new ClearBitsIterator();
			}
		}
	}

	/**
	 * Get an IterableBitSet that iterates over the bits of {@code set} that are set.
	 * @param set a BitSet
	 * @return an IterableBitSet over the set bits
	 */
	public static IterableBitSet getSetBits(BitSet set)
	{
		return new IterableBitSet(set);
	}

	/**
	 * Get an IterableBitSet that iterates over the bits of {@code set} that are set,
	 * in reverse order (highest to lowest index).
	 * @param set a BitSet
	 * @return an IterableBitSet over the set bits
	 */
	public static IterableBitSet getSetBitsReversed(BitSet set)
	{
		return new IterableBitSet(set, null, false, true);
	}

	/**
	 * Get an IterableBitSet that iterates over the cleared bits of {@code set}, up to {@code maxIndex}
	 * @param set a BitSet
	 * @param maxIndex the maximal index
	 * @return an IterableBitSet over the cleared bits
	 */
	public static IterableBitSet getClearBits(BitSet set, int maxIndex)
	{
		return new IterableBitSet(set, maxIndex, true);
	}

	/**
	 * Get an IterableBitSet that iterates over the cleared bits of {@code set}, up to {@code maxIndex},
	 * in reverse order (highest to lowest index).
	 * @param set a BitSet
	 * @return an IterableBitSet over the set bits
	 */
	public static IterableBitSet getClearBitsReversed(BitSet set, int maxIndex)
	{
		return new IterableBitSet(set, maxIndex, true, true);
	}

	/**
	 * Simple test method.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args)
	{
		BitSet test = new BitSet();
		test.set(1);
		test.set(2);
		test.set(3);
		test.set(5);
		test.set(8);
		test.set(13);
		test.set(21);

		System.out.println("\n" + test + " - set bits:");
		for (Integer index : getSetBits(test)) {
			System.out.println(index);
		}

		test.clear();
		for (@SuppressWarnings("unused")
		     Integer index : getSetBits(test)) {
			throw new RuntimeException("BitSet should be empty!");
		}
	}
}
