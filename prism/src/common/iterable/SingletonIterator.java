//==============================================================================
//	
//	Copyright (c) 2016-
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

package common.iterable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Abstract base class for Iterators ranging over a single element.
 *
 * @param <E> type of the Iterators's elements
 */
public abstract class SingletonIterator<E> implements FunctionalIterator<E>
{
	@Override
	public SingletonIterator<E> dedupe()
	{
		return this;
	}

	@Override
	public SingletonIterator<E> distinct()
	{
		return this;
	}



	/**
	 * Generic implementation of an singleton Iterator.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends SingletonIterator<E>
	{
		/** The single element */
		protected Optional<E> element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public Of(E element)
		{
			this.element = Optional.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public E next()
		{
			E next = element.get();
			release();
			return next;
		}

		@Override
		public void release()
		{
			element = Optional.empty();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an singleoton Iterator.
	 */
	public static class OfDouble extends SingletonIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The single element */
		protected OptionalDouble element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfDouble(double element)
		{
			this.element = OptionalDouble.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public double nextDouble()
		{
			double next = element.getAsDouble();
			release();
			return next;
		}

		@Override
		public SingletonIterator.OfDouble dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfDouble distinct()
		{
			return this;
		}

		@Override
		public OptionalDouble max()
		{
			OptionalDouble max = element;
			release();
			return max;
		}

		@Override
		public OptionalDouble min()
		{
			return max();
		}

		@Override
		public double sum()
		{
			return max().orElse(0.0);
		}

		@Override
		public void release()
		{
			element = OptionalDouble.empty();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an singleoton Iterator.
	 */
	public static class OfInt extends SingletonIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The single element */
		protected OptionalInt element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfInt(int element)
		{
			this.element = OptionalInt.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public int nextInt()
		{
			int next = element.getAsInt();
			release();
			return next;
		}

		@Override
		public SingletonIterator.OfInt dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfInt distinct()
		{
			return this;
		}

		@Override
		public OptionalInt max()
		{
			OptionalInt max = element;
			release();
			return max;
		}

		@Override
		public OptionalInt min()
		{
			return max();
		}

		@Override
		public long sum()
		{
			return max().orElse(0);
		}

		@Override
		public void release()
		{
			element = OptionalInt.empty();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an singleoton Iterator.
	 */
	public static class OfLong extends SingletonIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The single element */
		protected OptionalLong element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfLong(Long element)
		{
			this.element = OptionalLong.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public long nextLong()
		{
			long next = element.getAsLong();
			release();
			return next;
		}

		@Override
		public SingletonIterator.OfLong dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfLong distinct()
		{
			return this;
		}

		@Override
		public OptionalLong max()
		{
			OptionalLong max = element;
			release();
			return max;

		}

		@Override
		public OptionalLong min()
		{
			return element;
		}

		@Override
		public long sum()
		{
			return element.orElse(0);
		}

		@Override
		public void release()
		{
			element = OptionalLong.empty();
		}
	}
}