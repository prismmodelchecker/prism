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

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.function.IntFunction;

import common.IterableStateSet;
import common.iterable.MappingIterator;
import explicit.DTMC;
import explicit.Distribution;
import explicit.SuccessorsIterator;

/**
 * Base class for a DTMC view, i.e., a virtual DTMC that is obtained
 * by mapping from some other model on-the-fly.
 * <br>
 * The main job of sub-classes is to provide an appropriate
 * getTransitionsIterator() method. Several other methods, providing
 * meta-data on the model have to be provided as well. For examples,
 * see the sub-classes contained in this package.
 */
public abstract class DTMCView extends ModelView implements DTMC, Cloneable
{
	public DTMCView()
	{
		super();
	}

	public DTMCView(final ModelView model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		final IntFunction<Entry<Integer, Distribution>> getDistribution = new IntFunction<Entry<Integer, Distribution>>()
		{
			@Override
			public final Entry<Integer, Distribution> apply(final int state)
			{
				final Distribution distribution = new Distribution(getTransitionsIterator(state));
				return new AbstractMap.SimpleImmutableEntry<>(state, distribution);
			}
		};
		String s = "trans: [ ";
		final IterableStateSet states = new IterableStateSet(getNumStates());
		final Iterator<Entry<Integer, Distribution>> distributions = new MappingIterator.FromInt<>(states, getDistribution);
		while (distributions.hasNext()) {
			final Entry<Integer, Distribution> dist = distributions.next();
			s += dist.getKey() + ": " + dist.getValue();
			if (distributions.hasNext()) {
				s += ", ";
			}
		}
		return s + " ]";
	}



	//--- Model ---

	@Override
	public SuccessorsIterator getSuccessors(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state);

		return SuccessorsIterator.from(new PrimitiveIterator.OfInt() {
			public boolean hasNext() {return transitions.hasNext();}
			public int nextInt() {return transitions.next().getKey();}
		}, true);
	}
}
