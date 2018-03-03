//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class SingletonIterable<E> implements FunctionalIterable<E>
{
	public static class Of<E> extends SingletonIterable<E>
	{
		final E element;

		public Of(E theElement)
		{
			element = theElement;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new SingletonIterator.Of<>(element);
		}
	}



	public static class OfDouble extends SingletonIterable<Double> implements IterableDouble
	{
		final double element;
	
		public OfDouble(double theElement)
		{
			element = theElement;
		}
	
		@Override
		public SingletonIterator.OfDouble iterator()
		{
			return new SingletonIterator.OfDouble(element);
		}
	}



	public static class OfInt extends SingletonIterable<Integer> implements IterableInt
	{
		final int element;

		public OfInt(int theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfInt iterator()
		{
			return new SingletonIterator.OfInt(element);
		}
	}



	public static class OfLong extends SingletonIterable<Long> implements IterableLong
	{
		final long element;

		public OfLong(long theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfLong iterator()
		{
			return new SingletonIterator.OfLong(element);
		}
	}
}
