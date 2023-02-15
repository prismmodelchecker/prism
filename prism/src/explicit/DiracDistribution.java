//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
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

package explicit;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import common.iterable.SingletonIterator;

public class DiracDistribution implements Iterable<Entry<Integer, Double>>
{
	private final Entry<Integer, Double> transition;

	public DiracDistribution(final int state)
	{
		this.transition = new Transition(state);
	}

	public Iterator<Entry<Integer, Double>> iterator()
	{
		return new SingletonIterator.Of<>(transition);
	}

	public static Iterator<Entry<Integer, Double>> iterator(final int state)
	{
		return new SingletonIterator.Of<>((Entry<Integer, Double>) new Transition(state));
	}

	public static class Transition implements Map.Entry<Integer, Double>
	{
		private final Integer state;

		public Transition(final int state)
		{
			this.state = state;
		}

		@Override
		public Integer getKey()
		{
			return state;
		}

		@Override
		public Double getValue()
		{
			return 1.0;
		}

		@Override
		public Double setValue(final Double value)
		{
			throw new UnsupportedOperationException("immutable entry");
		}
	}
}