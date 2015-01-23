//==============================================================================
//	
//	Copyright (c) 2014-
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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A convenience wrapper around IterableBitSet that handles the three cases of
 * iterating over the set or cleared bits of a BitSet representing a set of states
 * as well as iterating over all states if the BitSet is {@code null}.
 */
public class IterableStateSet implements Iterable<Integer> {
	private BitSet setOfStates;
	private Integer numStates = null;
	private boolean complement = false;

	/**
	 * Constructor (iterate over all states 0..numStates-1)
	 * 
	 * @param numStates the number of states in the model, i.e., with indizes 0..numStates-1
	 */
	public IterableStateSet(int numStates)
	{
		this(null, numStates, false);
	}

	/**
	 * Constructor (iterate over the sets given by setOfStates or over all states)
	 * 
	 * @param setOfStates the BitSet representing state indizes in the model.
	 *                    {@code null} signifies "all states in the model"
	 * @param numStates the number of states in the model, i.e., with indizes 0..numStates-1
	 */
	public IterableStateSet(BitSet setOfStates, int numStates)
	{
		this(setOfStates, numStates, false);
	}

	/**
	 * Constructor (most general form)
	 *
	 * @param setOfStates the BitSet representing state indizes in the model.
	 *                    {@code null} signifies "all states in the model"
	 * @param numStates the number of states in the model, i.e., with indizes 0..numStates-1
	 * @param complement if {@code true}, iterate over all the states not included in setOfStates
	 */
	public IterableStateSet(BitSet setOfStates, int numStates, boolean complement)
	{
		this.setOfStates = setOfStates;
		this.numStates = numStates;
		this.complement = complement;
	}

	/** Implementation of an Iterator that iterates over all state indizes 0..numStates-1 */
	private class AllStatesIterator implements Iterator<Integer>
	{
		private int current = 0;

		@Override
		public boolean hasNext()
		{
			if (current < numStates) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Integer next()
		{
			if (hasNext()) {
				return current++;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	};

	/** Implementation of an Iterator that iterates over the empty state set */
	private class NoStatesIterator implements Iterator<Integer>
	{
		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public Integer next()
		{
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	};

	@Override
	public Iterator<Integer> iterator()
	{
		if (setOfStates == null) {
			if (!complement) {
				// return iterator over all states
				return new AllStatesIterator();
			} else {
				return new NoStatesIterator();
			}
		} else {
			// return appropriate IterableBitSet with maxIndex = numStates-1
			return new IterableBitSet(setOfStates, numStates-1, complement).iterator();
		}
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
		test.set(3);

		int numStates = 5;

		System.out.println("\n" + test + " - included states:");
		for (Integer index : new IterableStateSet(test, numStates)) {
			System.out.println(index);
		}

		System.out.println("\n" + test + " - excluded states:");
		for (Integer index : new IterableStateSet(test, numStates, true)) {
			System.out.println(index);
		}

		System.out.println("\nAll " + numStates + " states:");
		for (Integer index : new IterableStateSet(null, numStates)) {
			System.out.println(index);
		}

		System.out.println("\nNo " + numStates + " states:");
		for (Integer index : new IterableStateSet(null, numStates, true)) {
			System.out.println(index);
		}
	}
}
