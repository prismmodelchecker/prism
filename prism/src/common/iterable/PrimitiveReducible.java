package common.iterable;

import common.functions.*;

import java.util.*;
import java.util.function.*;

/**
 * A base type for primitive specializations of the methods provided by {@link Reducible}.
 * Specialized sub interfaces are provided for {@code double}, {@code int} and {@code long}:
 * <ul>
 *     <li>{@link PrimitiveReducible.OfDouble}</li>
 *     <li>{@link PrimitiveReducible.OfInt}</li>
 *     <li>{@link PrimitiveReducible.OfLong}</li>
 * </ul>
 *
 * @param <E> the type of elements returned by this Reducible
 * @param <E_CONS> the type of primitive consumers for the elements
 *
 * @see java.util.stream.DoubleStream DoubleStream
 * @see java.util.stream.IntStream IntStream
 * @see java.util.stream.LongStream LongStream
 */
public interface PrimitiveReducible<E, E_CAT, E_CONS> extends Reducible<E, E_CAT>
{
	/**
	 * Primitive specialisation of {@link Reducible#forEach}.
	 *
	 * @param action the action to be performed with each element
	 * @see PrimitiveIterator#forEachRemaining(Object);
	 */
	void forEach(E_CONS action);



	/**
	 * Specialisation for {@code double} of a {@link PrimitiveReducible}.
	 */
	interface OfDouble<E_CAT> extends PrimitiveReducible<Double, E_CAT, DoubleConsumer>
	{
		// Transforming Methods

		@Override
		default OfDouble<E_CAT> dedupe()
		{
			DoublePredicate isFirst = new DoublePredicate()
			{
				boolean start = true;
				double previous = 0.0;

				@Override
				public boolean test(double d)
				{
					if (start) {
						start = false;
					} else if (previous == d) {
						return false;
					} else if (Double.isNaN(previous) && Double.isNaN(d)) {
						// Circumvent Double.NaN != Double.NaN
						return false;
					}
					previous = d;
					return true;
				}
			};
			return filter(isFirst);
		}

		@Override
		default OfDouble<E_CAT> distinct()
		{
			return filter(new Distinct.OfDouble());
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code double}.
		 *
		 * @see Reducible#filter(java.util.function.Predicate)
		 */
		OfDouble<E_CAT> filter(DoublePredicate predicate);

		/**
		 * Primitive specialisation of {@code map} for {@code double}.
		 *
		 * @see Reducible#map(java.util.function.Function)
		 */
		<T> Reducible<T, ?> map(DoubleFunction<? extends T> function);

		/**
		 * Primitive specialisation of {@code mapToDouble} for {@code double}.
		 *
		 * @see Reducible#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		OfDouble<?> mapToDouble(DoubleUnaryOperator function);

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code double}.
		 *
		 * @see Reducible#mapToInt(java.util.function.ToIntFunction)
		 */
		OfInt<?> mapToInt(DoubleToIntFunction function);

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code double}.
		 *
		 * @see Reducible#mapToLong(java.util.function.ToLongFunction)
		 */
		OfLong<?> mapToLong(DoubleToLongFunction function);




		// Accumulations Methods (Consuming)

		@Override
		default OfDouble<E_CAT> consume()
		{
			forEach((double each) -> {});
			return this;
		}

		/**
		 * Primitive specialisation of {@code allMatch} for {@code double}.
		 *
		 * @see Reducible#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(DoublePredicate predicate)
		{
			return ! anyMatch(predicate.negate());
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code double}.
		 *
		 * @see Reducible#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(DoublePredicate predicate)
		{
			return ! filter(predicate).isEmpty();
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code double}.
		 *
		 * @see Reducible#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(DoublePredicate predicate)
		{
			return ! anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code double}.
		 *
		 * @see Reducible#collect(Object[])
		 */
		default double[] collect(double[] array)
		{
			return collect(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code double}.
		 *
		 * @see Reducible#collect(Object[], int)
		 */
		default double[] collect(double[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see Reducible#collectAndCount(Object[])
		 */
		default int collectAndCount(double[] array)
		{
			return collectAndCount(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see Reducible#collectAndCount(Object[], int)
		 */
		default int collectAndCount(double[] array, int offset)
		{
			Objects.requireNonNull(array);
			int index = reduce(offset, (int i, double e) -> {
				array[i] = e;
				return i + 1;
			});
			return index - offset;
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble collectDistinct()
		{
			Distinct.OfDouble unseen = new Distinct.OfDouble();
			filter(unseen).consume();
			return unseen.getSeen();
		}

		@Override
		default boolean contains(Object obj)
		{
			if (obj instanceof Double) {
				return contains(((Double) obj).doubleValue());
			}
			return false;
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code double}.
		 * Inclusion is defined in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 *
		 * @see Reducible#contains(Object)
		 */
		default boolean contains(double d)
		{
			return anyMatch(Double.isNaN(d) ? (double e) -> Double.isNaN(e) : (double e) -> e == d);
		}

		@Override
		default long count()
		{
			// avoid auto-boxing
			return reduce(0L, (long c, double d) -> c + 1);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code double}.
		 *
		 * @see Reducible#count(java.util.function.Predicate)
		 */
		default long count(DoublePredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code double}.
		 *
		 * @see Reducible#detect(java.util.function.Predicate)
		 */
		double detect(DoublePredicate predicate);

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see Math#max(double, double).
		 */
		default OptionalDouble max()
		{
			return reduce((DoubleBinaryOperator) Math::max);
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see Math#min(double, double).
		 */
		default OptionalDouble min()
		{
			return reduce((DoubleBinaryOperator) Math::min);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see Reducible#reduce(java.util.function.BinaryOperator)
		 */
		OptionalDouble reduce(DoubleBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see Reducible#reduce(Object, java.util.function.BiFunction)
		 */
		<T> T reduce(T init, ObjDoubleFunction<T, T> accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see Reducible#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		double reduce(double init, DoubleBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see Reducible#reduce(int, common.functions.IntObjToIntFunction)
		 */
		int reduce(int init, IntDoubleToIntFunction accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see Reducible#reduce(long, common.functions.LongObjToLongFunction)
		 */
		long reduce(long init, LongDoubleToLongFunction accumulator);

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default double sum()
		{
			return reduce(0.0, (DoubleBinaryOperator) Double::sum);
		}
	}



	/**
	 * Specialisation for {@code int} of a PrimitiveReducible.
	 */
	interface OfInt<E_CAT> extends PrimitiveReducible<Integer, E_CAT, IntConsumer>
	{
		// Transforming Methods

		@Override
		default OfInt<E_CAT> dedupe()
		{
			IntPredicate isFirst = new IntPredicate()
			{
				boolean start = true;
				int previous = 0;

				@Override
				public boolean test(int i)
				{
					if (start) {
						start = false;
					} else if (previous == i) {
						return false;
					}
					previous = i;
					return true;
				}
			};
			return filter(isFirst);
		}

		@Override
		default OfInt<E_CAT> distinct()
		{
			return filter(new Distinct.OfInt());
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code int}.
		 *
		 * @see Reducible#filter(java.util.function.Predicate)
		 */
		OfInt<E_CAT> filter(IntPredicate predicate);

		/**
		 * Primitive specialisation of {@code map} for {@code int}.
		 *
		 * @see Reducible#map(java.util.function.Function)
		 */
		<T> Reducible<T, ?> map(IntFunction<? extends T> function);

		/**
		 * Primitive specialisation of {@code mapToDouble} for {@code int}.
		 *
		 * @see Reducible#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		OfDouble<?> mapToDouble(IntToDoubleFunction function);

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code int}.
		 *
		 * @see Reducible#mapToInt(java.util.function.ToIntFunction)
		 */
		OfInt<?> mapToInt(IntUnaryOperator function);

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code int}.
		 *
		 * @see Reducible#mapToLong(java.util.function.ToLongFunction)
		 */
		OfLong<?> mapToLong(IntToLongFunction function);


		// Accumulations Methods (Consuming)

		@Override
		default OfInt<E_CAT> consume()
		{
			forEach((int each) -> {});
			return this;
		}

		/**
		 * Primitive specialisation of {@code allMatch} for {@code int}.
		 *
		 * @see Reducible#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(IntPredicate predicate)
		{
			return ! anyMatch(predicate.negate());
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code int}.
		 *
		 * @see Reducible#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(IntPredicate predicate)
		{
			return ! filter(predicate).isEmpty();
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code int}.
		 *
		 * @see Reducible#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(IntPredicate predicate)
		{
			return ! anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int}.
		 *
		 * @see Reducible#collect(Object[])
		 */
		default int[] collect(int[] array)
		{
			return collect(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int}.
		 *
		 * @see Reducible#collect(Object[], int)
		 */
		default int[] collect(int[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int} storing the elements in a {@link BitSet}.
		 * This method fails if the receiver yields a negative integer.
		 *
		 * @param indices a {@link BitSet}
		 * @return the BitSet with all elements added to it
		 * @throws IndexOutOfBoundsException if an integer is negative
		 * @see Reducible#collect(java.util.Collection)
		 */
		default BitSet collect(BitSet indices)
		{
			forEach((IntConsumer) indices::set);
			return indices;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int}.
		 *
		 * @see Reducible#collectAndCount(Object[])
		 */
		default int collectAndCount(int[] array)
		{
			return collectAndCount(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int}.
		 *
		 * @see Reducible#collectAndCount(Object[])
		 */
		default int collectAndCount(int[] array, int offset)
		{
			Objects.requireNonNull(array);
			int index = reduce(offset, (int i, int e) -> {
				array[i] = e;
				return i + 1;
			});
			return index - offset;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int} storing the elements in a {@link BitSet}.
		 * This method fails if the receiver yields a negative integer.
		 *
		 * @param indices a {@link BitSet}
		 * @return the number of elements added to the BitSet
		 * @throws IndexOutOfBoundsException if an integer is negative
		 * @see Reducible#collectAndCount(java.util.Collection)
		 */
		default int collectAndCount(BitSet indices)
		{
			Objects.requireNonNull(indices);
			return reduce(0, (int c, int i) -> {
				indices.set(i);
				return c + 1;
			});
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt collectDistinct()
		{
			Distinct.OfInt unseen = new Distinct.OfInt();
			filter(unseen).consume();
			return unseen.getSeen();
		}

		@Override
		default boolean contains(Object obj)
		{
			if (obj instanceof Integer) {
				return contains(((Integer) obj).intValue());
			}
			return false;
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code int}.
		 *
		 * @see Reducible#contains(Object)
		 */
		default boolean contains(int i)
		{
			return anyMatch((int e) -> e == i);
		}

		@Override
		default long count()
		{
			// avoid auto-boxing
			return reduce(0, (long c, long i) -> c + 1);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code int}.
		 *
		 * @see Reducible#count(java.util.function.Predicate)
		 */
		default long count(IntPredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code int}.
		 *
		 * @see Reducible#detect(java.util.function.Predicate)
		 */
		int detect(IntPredicate predicate);

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see Math#max(int, int).
		 */
		default OptionalInt max()
		{
			return reduce((IntBinaryOperator) Math::max);
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see Math#min(int, int).
		 */
		default OptionalInt min()
		{
			return reduce((IntBinaryOperator) Math::min);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code double}.
		 *
		 * @see Reducible#reduce(java.util.function.BinaryOperator)
		 */
		OptionalDouble reduce(DoubleBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see Reducible#reduce(java.util.function.BinaryOperator)
		 */
		OptionalInt reduce(IntBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code long}.
		 *
		 * @see Reducible#reduce(java.util.function.BinaryOperator)
		 */
		OptionalLong reduce(LongBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see Reducible#reduce(Object, java.util.function.BiFunction)
		 */
		<T> T reduce(T init, ObjIntFunction<T, T> accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code double}.
		 *
		 * @see Reducible#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		double reduce(double init, DoubleBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see Reducible#reduce(int, common.functions.IntObjToIntFunction)
		 */
		int reduce(int init, IntBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code long}.
		 *
		 * @see Reducible#reduce(long, common.functions.LongObjToLongFunction)
		 */
		long reduce(long init, LongBinaryOperator accumulator);

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default long sum()
		{
			return reduce(0L, (LongBinaryOperator) Long::sum);
		}
	}



	/**
	 * Specialisation for {@code long} of a PrimitiveReducible.
	 */
	interface OfLong<E_CAT> extends PrimitiveReducible<Long, E_CAT, LongConsumer>
	{
		// Transforming Methods

		@Override
		default OfLong<E_CAT> dedupe()
		{
			LongPredicate isFirst = new LongPredicate()
			{
				boolean start = true;
				long previous = 0;

				@Override
				public boolean test(long l)
				{
					if (start) {
						start = false;
					} else if (previous == l) {
						return false;
					}
					previous = l;
					return true;
				}
			};
			return filter(isFirst);
		}

		@Override
		default OfLong<E_CAT> distinct()
		{
			return filter(new Distinct.OfLong());
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code long}.
		 *
		 * @see Reducible#filter(java.util.function.Predicate)
		 */
		OfLong<E_CAT> filter(LongPredicate predicate);

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see Reducible#map(java.util.function.Function)
		 */
		<T> Reducible<T, ?> map(LongFunction<? extends T> function);

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see Reducible#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		OfDouble<?> mapToDouble(LongToDoubleFunction function);

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see Reducible#mapToInt(java.util.function.ToIntFunction)
		 */
		OfInt<?> mapToInt(LongToIntFunction function);

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see Reducible#mapToLong(java.util.function.ToLongFunction)
		 */
		OfLong<?> mapToLong(LongUnaryOperator function);



		// Accumulations Methods (Consuming)

		@Override
		default OfLong<E_CAT> consume()
		{
			forEach((long each) -> {});
			return this;
		}

		/**
		 * Primitive specialisation of {@code allMatch} for {@code long}.
		 *
		 * @see Reducible#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(LongPredicate predicate)
		{
			return ! anyMatch(predicate.negate());
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code long}.
		 *
		 * @see Reducible#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(LongPredicate predicate)
		{
			return ! filter(predicate).isEmpty();
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code long}.
		 *
		 * @see Reducible#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(LongPredicate predicate)
		{
			return ! anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code long}.
		 *
		 * @see Reducible#collect(Object[])
		 */
		default long[] collect(long[] array)
		{
			return collect(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code long}.
		 *
		 * @see Reducible#collect(Object[], int)
		 */
		default long[] collect(long[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code long}.
		 *
		 * @see Reducible#collectAndCount(Object[])
		 */
		default int collectAndCount(long[] array)
		{
			return collectAndCount(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code long}.
		 *
		 * @see Reducible#collectAndCount(Object[], int)
		 */
		default int collectAndCount(long[] array, int offset)
		{
			Objects.requireNonNull(array);
			int index = reduce(offset, (int i, long e) -> {array[i] = e; return i + 1;});
			return index - offset;
		}

		@Override
		default FunctionalPrimitiveIterable.OfLong collectDistinct()
		{
			Distinct.OfLong unseen = new Distinct.OfLong();
			filter(unseen).consume();
			return unseen.getSeen();
		}

		@Override
		default boolean contains(Object obj)
		{
			if (obj instanceof Long) {
				return contains(((Long) obj).longValue());
			}
			return false;
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code long}.
		 *
		 * @see Reducible#contains(Object)
		 */
		default boolean contains(long l)
		{
			return anyMatch((long e) -> e == l);
		}

		@Override
		default long count()
		{
			// avoid auto-boxing
			return reduce(0, (long c, long l) -> c + 1);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code long}.
		 *
		 * @see Reducible#count(java.util.function.Predicate)
		 */
		default long count(LongPredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code long}.
		 *
		 * @see Reducible#detect(java.util.function.Predicate)
		 */
		long detect(LongPredicate predicate);

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see Math#max(long, long).
		 */
		default OptionalLong max()
		{
			return reduce((LongBinaryOperator) Math::max);
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see Math#min(long, long).
		 */
		default OptionalLong min()
		{
			return reduce((LongBinaryOperator) Math::min);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see Reducible#reduce(java.util.function.BinaryOperator)
		 */
		OptionalLong reduce(LongBinaryOperator accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see Reducible#reduce(Object, java.util.function.BiFunction)
		 */
		<T> T reduce(T init, ObjLongFunction<T, T> accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see Reducible#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		double reduce(double init, DoubleLongToDoubleFunction accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see Reducible#reduce(int, common.functions.IntObjToIntFunction)
		 */
		int reduce(int init, IntLongToIntFunction accumulator);

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see Reducible#reduce(long, common.functions.LongObjToLongFunction)
		 */
		long reduce(long init, LongBinaryOperator accumulator);

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default long sum()
		{
			return reduce(0L, (LongBinaryOperator) Long::sum);
		}
	}
}
