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

import java.util.Objects;
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

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class MappingIterable<S, E, I extends Iterable<S>> implements FunctionalIterable<E>
{
	protected final I iterable;

	public MappingIterable(I iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}

	public static IterableDouble toDouble(Iterable<Double> iterable)
	{
		if (iterable instanceof IterableDouble) {
			return (IterableDouble) iterable;
		}
		return new ToDouble<>(iterable, Double::intValue);
	}

	public static IterableInt toInt(Iterable<Integer> iterable)
	{
		if (iterable instanceof IterableInt) {
			return (IterableInt) iterable;
		}
		return new ToInt<>(iterable, Integer::intValue);
	}

	public static IterableLong toLong(Iterable<Long> iterable)
	{
		if (iterable instanceof IterableLong) {
			return (IterableLong) iterable;
		}
		return new ToLong<>(iterable, Long::intValue);
	}



	public static class From<S, E> extends MappingIterable<S, E, Iterable<S>>
	{
		protected final Function<? super S, ? extends E> function;

		public From(Iterable<S> iterable, Function<? super S, ? extends E> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.From<>(iterable.iterator(), function);
		}
	}



	public static class ToDouble<E> extends MappingIterable<E, Double, Iterable<E>> implements IterableDouble
	{
		protected ToDoubleFunction<? super E> function;
	
		public ToDouble(Iterable<E> iterable, ToDoubleFunction<? super E> function)
		{
			super(iterable);
			this.function = function;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.ToDouble<>(iterable.iterator(), function);
		}
	}



	public static class ToInt<E> extends MappingIterable<E, Integer, Iterable<E>> implements IterableInt
	{
		protected ToIntFunction<? super E> function;

		public ToInt(Iterable<E> iterable, ToIntFunction<? super E> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.ToInt<>(iterable.iterator(), function);
		}
	}



	public static class ToLong<E> extends MappingIterable<E, Long, Iterable<E>> implements IterableLong
	{
		protected ToLongFunction<? super E> function;

		public ToLong(Iterable<E> iterable, ToLongFunction<? super E> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.ToLong<>(iterable.iterator(), function);
		}
	}



	public static class FromDouble<E> extends MappingIterable<Double, E, IterableDouble>
	{
		protected DoubleFunction<? extends E> function;
	
		public FromDouble(IterableDouble iterable, DoubleFunction<? extends E> function)
		{
			super(iterable);
			this.function = function;
		}
	
		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.FromDouble<>(iterable.iterator(), function);
		}
	}



	public static class FromDoubleToDouble extends MappingIterable<Double, Double, IterableDouble> implements IterableDouble
	{
		protected DoubleUnaryOperator function;
	
		public FromDoubleToDouble(IterableDouble iterable, DoubleUnaryOperator function)
		{
			super(iterable);
			this.function = function;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.FromDoubleToDouble(iterable.iterator(), function);
		}
	}



	public static class FromDoubleToInt extends MappingIterable<Double, Integer, IterableDouble> implements IterableInt
	{
		protected DoubleToIntFunction function;
	
		public FromDoubleToInt(IterableDouble iterable, DoubleToIntFunction function)
		{
			super(iterable);
			this.function = function;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.FromDoubleToInt(iterable.iterator(), function);
		}
	}



	public static class FromDoubleToLong extends MappingIterable<Double, Long, IterableDouble> implements IterableLong
	{
		protected DoubleToLongFunction function;
	
		public FromDoubleToLong(IterableDouble iterable, DoubleToLongFunction function)
		{
			super(iterable);
			this.function = function;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.FromDoubleToLong(iterable.iterator(), function);
		}
	}



	public static class FromIntToDouble extends MappingIterable<Integer, Double, IterableInt> implements IterableDouble
	{
		protected IntToDoubleFunction function;

		public FromIntToDouble(IterableInt iterable, IntToDoubleFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.FromIntToDouble(iterable.iterator(), function);
		}
	}



	public static class FromInt<E> extends MappingIterable<Integer, E, IterableInt>
	{
		protected IntFunction<? extends E> function;

		public FromInt(IterableInt iterable, IntFunction<? extends E> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.FromInt<E>(iterable.iterator(), function);
		}
	}



	public static class FromIntToInt extends MappingIterable<Integer, Integer, IterableInt> implements IterableInt
	{
		protected IntUnaryOperator function;

		public FromIntToInt(IterableInt iterable, IntUnaryOperator function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.FromIntToInt(iterable.iterator(), function);
		}
	}



	public static class FromIntToLong extends MappingIterable<Integer, Long, IterableInt> implements IterableLong
	{
		protected IntToLongFunction function;

		public FromIntToLong(IterableInt iterable, IntToLongFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.FromIntToLong(iterable.iterator(), function);
		}
	}



	public static class FromLong<E> extends MappingIterable<Long, E, IterableLong>
	{
		protected LongFunction<? extends E> function;

		public FromLong(IterableLong iterable, LongFunction<? extends E> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.FromLong<>(iterable.iterator(), function);
		}
	}



	public static class FromLongToDouble extends MappingIterable<Long, Double, IterableLong> implements IterableDouble
	{
		protected LongToDoubleFunction function;
	
		public FromLongToDouble(IterableLong iterable, LongToDoubleFunction function)
		{
			super(iterable);
			this.function = function;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.FromLongToDouble(iterable.iterator(), function);
		}
	}



	public static class FromLongToInt extends MappingIterable<Long, Integer, IterableLong> implements IterableInt
	{
		protected LongToIntFunction function;

		public FromLongToInt(IterableLong iterable, LongToIntFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.FromLongToInt(iterable.iterator(), function);
		}
	}



	public static class FromLongToLong extends MappingIterable<Long, Long, IterableLong> implements IterableLong
	{
		protected LongUnaryOperator function;

		public FromLongToLong(IterableLong iterable, LongUnaryOperator function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.FromLongToLong(iterable.iterator(), function);
		}
	}
}