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
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Abstract base class for Iterators that map elements using a function {@code f: S -> E}.
 * Yields the result of applying {@code f} to each each element of the underlying Iterator:
 * Iterator(S) -{@code f}-> Iterator(E)
 * <p>
 * The calls to {@code next()} of the underlying Iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * Implementations should release the underlying Iterator after iteration.
 *
 * @param <S> type of the underlying Iterator's elements
 * @param <E> type of the Iterator's elements after mapping
 * @param <I> type of the underlying Iterator
 */
public abstract class MappingIterator<S, E, I extends Iterator<S>> implements FunctionalIterator<E>
{
	/** The Iterator which elements are mapped */
	protected I iterator;

	/**
	 * Constructor for a MappingIterator without a mapping function.
	 *
	 * @param iterator an Iterator to be mapped
	 */
	public MappingIterator(I iterator)
	{
		Objects.requireNonNull(iterator);
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		if (iterator.hasNext()) {
			return true;
		}
		release();
		return false;
	}

	@Override
	public long count()
	{
		// do not apply mapping, just count elements of underlying iterator
		return Reducible.extend(iterator).count();
	}



	/**
	 * Generic implementation of a MappingIterator using a function {@code f: S -> E}.
	 *
	 * @param <S> type of the underlying Iterator's elements
	 * @param <E> type of the Iterator's elements after mapping
	 */
	public static class ObjToObj<S, E> extends MappingIterator<S, E, Iterator<S>>
	{
		/** The function the Iterator uses to map the elements */
		protected final Function<? super S, ? extends E> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public ObjToObj(Iterator<S> iterator, Function<? super S, ? extends E> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.next());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: S -> double}.
	 *
	 * @param <S> type of the underlying Iterator's elements
	 */
	public static class ObjToDouble<S> extends MappingIterator<S, Double, Iterator<S>> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The function the Iterator uses to map the elements */
		protected final ToDoubleFunction<? super S> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public ObjToDouble(Iterator<S> iterator, ToDoubleFunction<? super S> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.next());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: S -> int}.
	 *
	 * @param <S> type of the underlying Iterator's elements
	 */
	public static class ObjToInt<S> extends MappingIterator<S, Integer, Iterator<S>> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The function the Iterator uses to map the elements */
		protected final ToIntFunction<? super S> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public ObjToInt(Iterator<S> iterator, ToIntFunction<? super S> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.next());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: S -> long}.
	 *
	 * @param <S> type of the underlying Iterator's elements
	 */
	public static class ObjToLong<S> extends MappingIterator<S, Long, Iterator<S>> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The function the Iterator uses to map the elements */
		protected final ToLongFunction<? super S> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public ObjToLong(Iterator<S> iterator, ToLongFunction<? super S> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.next());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: double -> E}.
	 *
	 * @param <E> type of the Iterator's elements after mapping
	 */
	public static class DoubleToObj<E> extends MappingIterator<Double, E, PrimitiveIterator.OfDouble>
	{
		/** The function the Iterator uses to map the elements */
		protected final DoubleFunction<? extends E> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public DoubleToObj(OfDouble iterator, DoubleFunction<? extends E> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.nextDouble());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: double -> double}.
	 */
	public static class DoubleToDouble extends MappingIterator<Double, Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The function the Iterator uses to map the elements */
		protected final DoubleUnaryOperator function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public DoubleToDouble(PrimitiveIterator.OfDouble iterator, DoubleUnaryOperator function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.nextDouble());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: double -> int}.
	 */
	public static class DoubleToInt extends MappingIterator<Double, Integer, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The function the Iterator uses to map the elements */
		protected final DoubleToIntFunction function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public DoubleToInt(PrimitiveIterator.OfDouble iterator, DoubleToIntFunction function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.nextDouble());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: double -> long}.
	 */
	public static class DoubleToLong extends MappingIterator<Double, Long, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The function the Iterator uses to map the elements */
		protected final DoubleToLongFunction function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public DoubleToLong(PrimitiveIterator.OfDouble iterator, DoubleToLongFunction function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.nextDouble());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: int -> E}.
	 *
	 * @param <E> type of the Iterator's elements after mapping
	 */
	public static class IntToObj<E> extends MappingIterator<Integer, E, PrimitiveIterator.OfInt>
	{
		/** The function the Iterator uses to map the elements */
		protected final IntFunction<? extends E> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public IntToObj(OfInt iterator, IntFunction<? extends E> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.nextInt());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: int -> double}.
	 */
	public static class IntToDouble extends MappingIterator<Integer, Double, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The function the Iterator uses to map the elements */
		protected final IntToDoubleFunction function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public IntToDouble(PrimitiveIterator.OfInt iterator, IntToDoubleFunction function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.nextInt());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: int -> int}.
	 */
	public static class IntToInt extends MappingIterator<Integer, Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The function the Iterator uses to map the elements */
		protected final IntUnaryOperator function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public IntToInt(PrimitiveIterator.OfInt iterator, IntUnaryOperator function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.nextInt());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: int -> long}.
	 */
	public static class IntToLong extends MappingIterator<Integer, Long, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The function the Iterator uses to map the elements */
		protected final IntToLongFunction function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public IntToLong(PrimitiveIterator.OfInt iterator, IntToLongFunction function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.nextInt());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: long -> E}.
	 *
	 * @param <E> type of the Iterator's elements after mapping
	 */
	public static class LongToObj<E> extends MappingIterator<Long, E, PrimitiveIterator.OfLong>
	{
		/** The function the Iterator uses to map the elements */
		protected final LongFunction<? extends E> function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public LongToObj(OfLong iterator, LongFunction<? extends E> function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.nextLong());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofLong();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: long -> double}.
	 */
	public static class LongToDouble extends MappingIterator<Long, Double, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The function the Iterator uses to map the elements */
		protected final LongToDoubleFunction function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public LongToDouble(PrimitiveIterator.OfLong iterator, LongToDoubleFunction function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.nextLong());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofLong();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: long -> int}.
	 */
	public static class LongToInt extends MappingIterator<Long, Integer, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The function the Iterator uses to map the elements */
		protected final LongToIntFunction function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public LongToInt(PrimitiveIterator.OfLong iterator, LongToIntFunction function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.nextLong());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofLong();
		}
	}



	/**
	 * Primitive specialisation of a MappingIterator using a function {@code f: long -> long}.
	 */
	public static class LongToLong extends MappingIterator<Long, Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The function the Iterator uses to map the elements */
		protected final LongUnaryOperator function;

		/**
		 * Constructor for an Iterator that maps elements using a function.
		 *
		 * @param iterator an Iterator to be mapped
		 * @param function a function used to map the elements
		 */
		public LongToLong(PrimitiveIterator.OfLong iterator, LongUnaryOperator function)
		{
			super(iterator);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.nextLong());
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofLong();
		}
	}
}