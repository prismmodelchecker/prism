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

package common.iterable;

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

/**
 * Base class for Iterables returning empty iterators,
 * static helpers for common primitive iterators.
 */
public abstract class EmptyIterable<E> implements FunctionalIterable<E>
{
	private static final Of<?>    OF        = new Of<>();
	private static final OfDouble OF_DOUBLE = new OfDouble();
	private static final OfInt    OF_INT    = new OfInt();
	private static final OfLong   OF_LONG   = new OfLong();

	@SuppressWarnings("unchecked")
	public static <E> Of<E> Of()
	{
		return (Of<E>) OF;
	}

	public static OfDouble OfDouble()
	{
		return OF_DOUBLE;
	}

	public static OfInt OfInt()
	{
		return OF_INT;
	}

	public static OfLong OfLong()
	{
		return OF_LONG;
	}

	public static class Of<E> extends EmptyIterable<E>
	{
		private Of() {};

		@Override
		public EmptyIterator.Of<E> iterator()
		{
			return EmptyIterator.Of();
		}
	}

	public static class OfDouble extends EmptyIterable<Double> implements IterableDouble
	{
		private OfDouble() {};
	
		@Override
		public EmptyIterator.OfDouble iterator()
		{
			return EmptyIterator.OfDouble();
		}
	}

	public static class OfInt extends EmptyIterable<Integer> implements IterableInt
	{
		private OfInt() {};

		@Override
		public EmptyIterator.OfInt iterator()
		{
			return EmptyIterator.OfInt();
		}
	}

	public static class OfLong extends EmptyIterable<Long> implements IterableLong
	{
		private OfLong() {};

		@Override
		public EmptyIterator.OfLong iterator()
		{
			return EmptyIterator.OfLong();
		}
	}
}