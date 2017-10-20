//==============================================================================
//	
//	Copyright (c) 2016-
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

package explicit.modelviews;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import common.functions.primitive.PairPredicateInt;
import common.IterableBitSet;

/**
 * An object representing an equivalence relation between integers (typically state indices),
 * from a given list of equivalent classes (each represented by a BitSet).
 */
public class EquivalenceRelationInteger implements PairPredicateInt
{
	final protected Map<Integer, BitSet> classes = new HashMap<Integer, BitSet>();
	final protected BitSet nonRepresentatives    = new BitSet();

	public EquivalenceRelationInteger() {}

	public EquivalenceRelationInteger(final Iterable<BitSet> equivalenceClasses)
	{
		for (BitSet equivalenceClass : equivalenceClasses) {
			switch (equivalenceClass.cardinality()) {
			case 0:
				throw new IllegalArgumentException("expected non-empty classes");
			case 1:
				continue;
			default:
				for (Integer i : new IterableBitSet(equivalenceClass)) {
					if (classes.put(i, equivalenceClass) != null) {
						throw new IllegalArgumentException("expected disjoint classes");
					}
				}
				nonRepresentatives.or(equivalenceClass);
				nonRepresentatives.clear(equivalenceClass.nextSetBit(0));
			}
		}
	}

	@Override
	public boolean test(final int i, final int j)
	{
		if (i == j) {
			return true;
		}
		final BitSet equivalenceClass = classes.get(i);
		return equivalenceClass != null && equivalenceClass.get(j);
	}

	public int getRepresentative(final int i)
	{
		final BitSet equivalenceClass = classes.get(i);
		return equivalenceClass == null ? i : equivalenceClass.nextSetBit(0);
	}

	public BitSet getEquivalenceClass(final int i)
	{
		BitSet equivalenceClass = getEquivalenceClassOrNull(i);
		if (equivalenceClass == null) {
			equivalenceClass = new BitSet(i + 1);
			equivalenceClass.set(i);
		}
		return equivalenceClass;
	}

	/**
	 * Return the equivalence class of {@code i} if {@code i} is no singleton, otherwise {@code null}.
	 * @param i a number
	 * @return {@code null} if [i] is a singleton, otherwise [i]
	 */
	public BitSet getEquivalenceClassOrNull(final int i)
	{
		return classes.get(i);
	}

	public BitSet getNonRepresentatives()
	{
		return nonRepresentatives;
	}

	public boolean isRepresentative(final int i)
	{
		return ! nonRepresentatives.get(i);
	}



	public static class KeepSingletons extends EquivalenceRelationInteger
	{
		public KeepSingletons(Iterable<BitSet> equivalenceClasses)
		{
			for (BitSet equivalenceClass : equivalenceClasses) {
				switch (equivalenceClass.cardinality()) {
				case 0:
					throw new IllegalArgumentException("expected non-empty classes");
				default:
					for (Integer i : new IterableBitSet(equivalenceClass)) {
						if (classes.put(i, equivalenceClass) != null) {
							throw new IllegalArgumentException("expected disjoint classes");
						}
					}
					nonRepresentatives.or(equivalenceClass);
					nonRepresentatives.clear(equivalenceClass.nextSetBit(0));
				}
			}
		}

		@Override
		public BitSet getEquivalenceClassOrNull(int i)
		{
			BitSet equivalenceClass = super.getEquivalenceClassOrNull(i);
			if (equivalenceClass == null) {
				return null;
			}
			return (equivalenceClass.cardinality() == 1) ? null : equivalenceClass;
		}

		public BitSet getOriginalEquivalenceClass(int i)
		{
			return classes.get(i);
		}
	}
}
