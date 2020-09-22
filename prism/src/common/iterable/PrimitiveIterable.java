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

import java.util.Iterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * A base interface for primitive specializations of {@code Iterable}.
 * Specialized sub interfaces are provided for the types {@link OfInt int}, {@link OfLong long} and {@link OfDouble double}.
 *
 * @param <E> the type of elements hold by this PrimitiveIterable
 * @param <E_CONS> the type of primitive consumer
 *
 * @see Iterable
 * @see PrimitiveIterator
 */
public interface PrimitiveIterable<E, E_CONS> extends Iterable<E>
{
	/**
	 * Convert an Iterable&lt;Double&gt; to a PrimitiveIterable.OfDouble by unboxing each element.
	 * If the argument's Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of PrimitiveIterable.OfDouble.
	 *
	 * @param iterable the {@link Iterable}&lt;Double&gt; to extend
	 * @return a {@link PrimitiveIterable.OfDouble} that is either the argument or an adaptor on the argument
	 */
	static PrimitiveIterable.OfDouble unboxDouble(Iterable<Double> iterable)
	{
		Objects.requireNonNull(iterable);

		return () -> unboxDouble(iterable.iterator());
	}

	/**
	 * Convert an Iterator&lt;Double&gt; to a PrimitiveIterator.OfDouble by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of PrimitiveIterator.OfDouble.
	 *
	 * @param iterator the {@link Iterator}&lt;Double&gt; to extend
	 * @return a {@link PrimitiveIterator.OfDouble} that is either the argument or an adaptor on the argument
	 */
	static PrimitiveIterator.OfDouble unboxDouble(Iterator<Double> iterator)
	{
		Objects.requireNonNull(iterator);

		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return (PrimitiveIterator.OfDouble) iterator;
		}

		return new PrimitiveIterator.OfDouble()
		{
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public double nextDouble()
			{
				return iterator.next();
			}
		};
	}

	/**
	 * Convert an Iterable&lt;Integer&gt; to a PrimitiveIterable.OfInt by unboxing each element.
	 * If the argument's Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of PrimitiveIterable.OfInt.
	 *
	 * @param iterable the {@link Iterable}&lt;Integer&gt; to extend
	 * @return a {@link PrimitiveIterable.OfInt} that is either the argument or an adaptor on the argument
	 */
	static PrimitiveIterable.OfInt unboxInt(Iterable<Integer> iterable)
	{
		Objects.requireNonNull(iterable);

		return () -> unboxInt(iterable.iterator());
	}

	/**
	 * Convert an Iterator&lt;Integer&gt; to a PrimitiveIterator.OfInt by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of PrimitiveIterator.OfInt.
	 *
	 * @param iterator the {@link Iterator}&lt;Integer&gt; to extend
	 * @return a {@link PrimitiveIterator.OfInt} that is either the argument or an adaptor on the argument
	 */
	static PrimitiveIterator.OfInt unboxInt(Iterator<Integer> iterator)
	{
		Objects.requireNonNull(iterator);

		if (iterator instanceof PrimitiveIterator.OfInt) {
			return (PrimitiveIterator.OfInt) iterator;
		}

		return new PrimitiveIterator.OfInt()
		{
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public int nextInt()
			{
				return iterator.next();
			}
		};
	}

	/**
	 * Convert an Iterable&lt;Long&gt; to a PrimitiveIterable.OfLong by unboxing each element.
	 * If the argument's Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of PrimitiveIterable.OfLong.
	 *
	 * @param iterable the {@link Iterable}&lt;Long&gt; to extend
	 * @return a {@link PrimitiveIterable.OfLong} that is either the argument or an adaptor on the argument
	 */
	static PrimitiveIterable.OfLong unboxLong(Iterable<Long> iterable)
	{
		Objects.requireNonNull(iterable);

		return () -> unboxLong(iterable.iterator());
	}

	/**
	 * Convert an Iterator&lt;Long&gt; to a PrimitiveIterator.OfLong by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of PrimitiveIterator.OfLong.
	 *
	 * @param iterator the {@link Iterator}&lt;Long&gt; to extend
	 * @return a {@link PrimitiveIterator.OfLong} that is either the argument or an adaptor on the argument
	 */
	static PrimitiveIterator.OfLong unboxLong(Iterator<Long> iterator)
	{
		Objects.requireNonNull(iterator);

		if (iterator instanceof PrimitiveIterator.OfLong) {
			return (PrimitiveIterator.OfLong) iterator;
		}

		return new PrimitiveIterator.OfLong()
		{
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public long nextLong()
			{
				return iterator.next();
			}
		};
	}

	@Override
	PrimitiveIterator<E, E_CONS> iterator();

	/**
	 * Perform the given action for each element of the receiver.
	 *
	 * @see Iterable#forEach(java.util.function.Consumer)
	 */
	default void forEach(E_CONS action)
	{
		iterator().forEachRemaining(action);
	}



	/**
	 * Specialisation for {@code double} of a PrimitiveIterable.
	 */
	interface OfDouble extends PrimitiveIterable<Double, DoubleConsumer>
	{
		@Override
		PrimitiveIterator.OfDouble iterator();
	}



	/**
	 * Specialisation for {@code int} of a PrimitiveIterable.
	 */
	interface OfInt extends PrimitiveIterable<Integer, IntConsumer>
	{
		@Override
		PrimitiveIterator.OfInt iterator();
	}



	/**
	 * Specialisation for {@code long} of a PrimitiveIterable.
	 */
	interface OfLong extends PrimitiveIterable<Long, LongConsumer>
	{
		@Override
		PrimitiveIterator.OfLong iterator();
	}
}
