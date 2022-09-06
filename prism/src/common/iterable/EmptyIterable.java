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

import java.util.Iterator;

/**
 * Base class for Iterables returning empty iterators,
 * static helpers for common primitive iterators.
 */
public abstract class EmptyIterable<T> implements Iterable<T>
{
	private static final Of<?> OF = new Of<>();
	private static final OfInt OF_INT = new OfInt();
	private static final OfDouble OF_DOUBLE = new OfDouble();

	@SuppressWarnings("unchecked")
	public static <T> Of<T> Of() {
		return (Of<T>) OF;
	}

	public static OfInt OfInt() {
		return OF_INT;
	}

	public static OfDouble OfDouble() {
		return OF_DOUBLE;
	}

	public static class Of<T> extends EmptyIterable<T>
	{
		private Of() {};

		@Override
		public Iterator<T> iterator()
		{
			return EmptyIterator.Of();
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

	public static class OfDouble extends EmptyIterable<Double> implements IterableDouble
	{
		private OfDouble() {};

		@Override
		public EmptyIterator.OfDouble iterator()
		{
			return EmptyIterator.OfDouble();
		}
	}
}
