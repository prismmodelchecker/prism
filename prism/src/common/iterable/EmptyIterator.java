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

import java.util.NoSuchElementException;

/**
 * Base class for empty Iterators,
 * static helpers for common primitive iterators.
 */
public abstract class EmptyIterator<E> implements FunctionalIterator<E>
{
	private static final Of<?> OF           = new Of<>();
	private static final OfInt OF_INT       = new OfInt();
	private static final OfLong OF_LONG     = new OfLong();
	private static final OfDouble OF_DOUBLE = new OfDouble();

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

	@Override
	public boolean hasNext()
	{
		return false;
	}



	public static class Of<E> extends EmptyIterator<E>
	{
		private Of() {};

		@Override
		public E next()
		{
			throw new NoSuchElementException();
		}
	}



	public static class OfDouble extends EmptyIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		private OfDouble() {};

		@Override
		public double nextDouble()
		{
			throw new NoSuchElementException();
		}
	}



	public static class OfInt extends EmptyIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		private OfInt() {};

		@Override
		public int nextInt()
		{
			throw new NoSuchElementException();
		}
	}



	public static class OfLong extends EmptyIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		private OfLong() {};

		@Override
		public long nextLong()
		{
			throw new NoSuchElementException();
		}
	}
}
