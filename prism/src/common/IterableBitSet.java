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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Convenience class to loop easily over the set/clear bits of a BitSet.
 *
 * For example:<br/><br/>
 * <code>for (Integer index : getSetBits(set)) { ... }</code><br/>
 */
public class IterableBitSet implements Iterable<Integer>
{
	private BitSet set;
	private boolean clearBits = false;
	private Integer maxIndex = null;

	/**
	 * Constructor for an Iterable that iterates over the set bits of {@code set}
	 */
	public IterableBitSet(BitSet set)
	{
		this.set = set;
		this.clearBits = false;
	}

	/**
	 * Constructor for an Iterable that iterates over bits of the given set {@code set},
	 * up to the maximal index given by {@code maxIndex}. If {@code clearBits} is {@code true},
	 * iterate over the cleared bits instead of the set bits.
	 * @param set the underlying BitSet
	 * @param maxIndex the maximal index for iteration (negative = iterate over the empty set)
	 * @param clearBits if true, iterate over the cleared bits in the BitSet
	 */
	public IterableBitSet(BitSet set, int maxIndex, boolean clearBits)
	{
		this.set = set;
		this.maxIndex = maxIndex;
		this.clearBits = clearBits;
	}

	/** Implementation of the iterator over the set bits */
	private class SetBitsIterator implements Iterator<Integer> {
		private int index = set.nextSetBit(0);

		@Override
		public boolean hasNext() {
			if (maxIndex != null && index > maxIndex) {
				// limit to 0 ... maxIndex
				return false;
			}
			return index >= 0;
		}

		@Override
		public Integer next() {
			if (hasNext()) {
				Integer next = index;
				index = set.nextSetBit(index + 1);
				return next;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/** Implementation of the iterator over the cleared bits, requires that {@code maxIndex != null} */
	private class ClearBitsIterator implements Iterator<Integer> {
		private int index = set.nextClearBit(0);

		@Override
		public boolean hasNext() {
			if (index > maxIndex) {
				// limit to 0 ... maxIndex
				return false;
			}
			return true;
		}

		@Override
		public Integer next() {
			if (hasNext()) {
				Integer next = index;
				index = set.nextClearBit(index + 1);
				return next;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Iterator<Integer> iterator() {
		if (clearBits == false) {
			return new SetBitsIterator();
		} else {
			return new ClearBitsIterator();
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
		for (@SuppressWarnings("unused") Integer index : getSetBits(test)) {
			throw new RuntimeException("BitSet should be empty!");
		}
	}
}
