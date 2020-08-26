//==============================================================================
//
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import common.IntDoubleBiConsumer;
import prism.PrismUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;


/**
 * Explicit representation of a probability distribution.
 * Basically, a mapping from (integer-valued) indices to (non-zero, double-valued) probabilities.
 */
public final class Distribution implements Iterable<Map.Entry<Integer, Double>>
{
	private static final int[] EMPTY_INTS = new int[0];
	private static final double[] EMPTY_DOUBLES = new double[0];

	private int[] keys;
	private double[] values;

	public static Distribution of() {
		return new Distribution();
	}

	public static Distribution of(int key, double value) {
		return new Distribution(key, value);
	}

	public static Distribution copyOf(Distribution distribution) {
		return new Distribution(distribution);
	}

	public static Distribution permute(Distribution distribution, int[] permutation) {
		Builder build = new Builder(distribution.size());
		for (int i = 0, keysLength = distribution.keys.length; i < keysLength; i++) {
			build.add(permutation[distribution.keys[i]], distribution.values[i]);
		}
		return build.build();
	}

	/**
	 * Create an empty distribution.
	 */
	public Distribution()
	{
		keys = EMPTY_INTS;
		values = EMPTY_DOUBLES;
	}

	/**
	 * Create a singleton distribution.
	 */
	public Distribution(int key, double value)
	{
		keys = new int[] { key };
		values = new double[] { value };
	}

	/**
	 * Copy constructor.
	 */
	public Distribution(Distribution distr)
	{
		keys = distr.keys.clone();
		values = distr.values.clone();
	}

	/**
	 * Construct a distribution from an iterator over transitions.
	 */
	public Distribution(Iterator<Map.Entry<Integer, Double>> transitions)
	{
		this();
		while (transitions.hasNext()) {
			Map.Entry<Integer, Double> trans = transitions.next();
			add(trans.getKey(), trans.getValue());
		}
	}

	/**
	 * Construct a distribution from an existing one and an index permutation,
	 * i.e. in which index i becomes index permut[i].
	 * Note: have to build the new distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public Distribution(Distribution distr, int permut[])
	{
		keys = EMPTY_INTS;
		values = EMPTY_DOUBLES;
		distr.forEach((key, value) -> add(permut[key], value));
	}

	private Distribution(int[] keys, double[] values)
	{
		assert keys.length == values.length;
		this.keys = keys;
		this.values = values;
	}

	/**
	 * Clear all entries of the distribution.
	 */
	public void clear()
	{
		keys = EMPTY_INTS;
		values = EMPTY_DOUBLES;
	}

	/**
	 * Add 'prob' to the probability for index 'j'.
	 * Return boolean indicating whether or not there was already
	 * non-zero probability for this index (i.e. false denotes new transition).
	 */
	public boolean add(int j, double prob)
	{
		int index = Arrays.binarySearch(keys, j);

		if (prob == 0.d) {
			// No new transition
			return index >= 0;
		}

		if (index >= 0) {
			// Have this key already
			values[index] += prob;
			return true;
		}
		// Need to insert it - compute insertion point
		index = -(index + 1);

		int[] localKeys = new int[keys.length + 1];
		double[] localValues = new double[keys.length + 1];

		System.arraycopy(keys, 0, localKeys, 0, index);
		System.arraycopy(values, 0, localValues, 0, index);
		localKeys[index] = j;
		localValues[index] = prob;
		System.arraycopy(keys, index, localKeys, index + 1, keys.length - index);
		System.arraycopy(values, index, localValues, index + 1, keys.length - index);

		keys = localKeys;
		values = localValues;

		return false;
	}

	/**
	 * Set the probability for index 'j' to 'prob'.
	 */
	public void set(int j, double prob)
	{
		int index = Arrays.binarySearch(keys, j);

		if (index >= 0) {
			values[index] = prob;
		} else {
			add(j, prob);
		}
	}

	/**
	 * Get the probability for index j.
	 */
	public double get(int j)
	{
		int index = Arrays.binarySearch(keys, j);
		return index < 0 ? 0.0d : values[index];
	}

	/**
	 * Returns true if index j is in the support of the distribution.
	 */
	public boolean contains(int j)
	{
		return Arrays.binarySearch(keys, j) >= 0;
	}

	/**
	 * Returns true if all indices in the support of the distribution are in the set.
	 */
	public boolean isSubsetOf(Collection<Integer> set)
	{
		for (int key : keys) {
			if (!set.contains(key)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if all indices in the support of the distribution are in the set.
	 */
	public boolean isSubsetOf(BitSet set)
	{
		for (int key : keys) {
			if (!set.get(key))
				return false;
		}
		return true;
	}

	/**
	 * Returns true if at least one index in the support of the distribution is in the set.
	 */
	public boolean containsOneOf(Collection<Integer> set)
	{
		for (int key : keys) {
			if (set.contains(key))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if at least one index in the support of the distribution is in the set.
	 */
	public boolean containsOneOf(BitSet set)
	{
		for (int key : keys) {
			if (set.get(key))
				return true;
		}
		return false;
	}

	/**
	 * Get the support of the distribution.
	 */
	public Set<Integer> getSupport()
	{
		Set<Integer> support = new HashSet<>(keys.length);
		for (int key : keys) {
			support.add(key);
		}
		return support;
	}

	/**
	 * Gets the support of the distribution as an array. This must not be modified.
	 */
	public int[] getSupportArray() {
		return keys;
	}

	/**
	 * Get an iterator over the entries of the map defining the distribution.
	 */
	@Override
	public Iterator<Map.Entry<Integer, Double>> iterator()
	{
		return new EntryIterator();
	}

	/**
	 * Returns true if the distribution is empty.
	 */
	public boolean isEmpty()
	{
		return keys.length == 0;
	}

	/**
	 * Get the size of the support of the distribution.
	 */
	public int size()
	{
		return keys.length;
	}

	/**
	 * Get the mean of the distribution.
	 */
	public double mean()
	{
		double d = 0.0;
		for (int i = 0; i < keys.length; i++) {
			d += keys[i] * values[i];
		}
		return d;
	}

	/**
	 * Get the variance of the distribution.
	 */
	public double variance()
	{
		double mean = mean();
		double meanSq = 0.0;
		for (int i = 0; i < keys.length; i++) {
			meanSq += keys[i] * keys[i] * values[i];
		}
		return Math.abs(meanSq - mean * mean);
	}

	/**
	 * Get the standard deviation of the distribution.
	 */
	public double standardDeviation()
	{
		return Math.sqrt(variance());
	}

	/**
	 * Get the relative standard deviation of the distribution,
	 * i.e., as a percentage of the mean.
	 */
	public double standardDeviationRelative()
	{
		return 100.0 * standardDeviation() / mean();
	}

	/**
	 * Get the sum of the probabilities in the distribution.
	 */
	public double sum()
	{
		double d = 0.0d;
		for (double value : values) {
			d += value;
		}
		return d;
	}

	public double sumWeighted(double[] weights)
	{
		double d = 0.0d;
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			d += weights[keys[i]] * values[i];
		}
		return d;
	}

	public double sumWeighted(IntToDoubleFunction weights)
	{
		double d = 0.0d;
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			d += weights.applyAsDouble(keys[i]) * values[i];
		}
		return d;
	}

	public double sumWeightedAllBut(IntToDoubleFunction weights, int j)
	{
		double d = 0.0d;
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			int state = keys[i];
			if (state == j) {
				continue;
			}
			d += weights.applyAsDouble(state) * values[i];
		}
		return d;
	}

	public double sumWeightedAllBut(IntToDoubleFunction weights, IntPredicate predicate)
	{
		double d = 0.0d;
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			int state = keys[i];
			if (predicate.test(state)) {
				continue;
			}
			d += weights.applyAsDouble(state) * values[i];
		}
		return d;
	}

	/**
	 * Get the sum of all the probabilities in the distribution except for index j.
	 */
	public double sumAllBut(int j)
	{
		double d = 0.0d;
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			if (keys[i] != j)
				d += values[i];
		}
		return d;
	}

	public double sumAllBut(IntPredicate predicate)
	{
		double d = 0.0d;
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			if (!predicate.test(keys[i]))
				d += values[i];
		}
		return d;
	}

	/**
	 * Create a new distribution, based on a mapping from the indices
	 * used in this distribution to a different set of indices.
	 * If a key is mapped to {@literal -1}, it is removed.
	 */
	public Distribution map(int map[])
	{
		Builder builder = new Builder(size());
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			int newKey = map[keys[i]];
			if (newKey != -1)
				builder.add(newKey, values[i]);
		}
		return builder.build();
	}

	/**
	 * Create a new distribution, based on a mapping from the indices
	 * used in this distribution to a different set of indices.
	 * If a key is mapped to {@literal -1}, it is removed.
	 */
	public Distribution map(IntUnaryOperator map)
	{
		Builder builder = new Builder(size());
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			int newKey = map.applyAsInt(keys[i]);
			if (newKey != -1)
				builder.add(newKey, values[i]);
		}
		return builder.build();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Distribution other = (Distribution) o;
		return Arrays.equals(keys, other.keys) &&
				PrismUtils.doublesAreClose(values, other.values, PrismUtils.epsilonDouble, false);
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(keys);
	}

	@Override
	public String toString()
	{
		if (keys.length == 0) {
			return "{}";
		}
		StringBuilder builder = new StringBuilder();
		builder.append('{');
		builder.append(keys[0]).append('=').append(values[0]);
		for (int i = 1; i < keys.length; i++) {
			builder.append(", ").append(keys[i]).append('=').append(values[i]);
		}
		builder.append('}');
		return builder.toString();
	}

	public String toStringCSV()
	{
		StringBuilder builder = new StringBuilder("Value");
		forEach((int key) -> builder.append(", ").append(key));
		builder.append("\nProbability");
		forEach((int key, double value) -> builder.append(", ").append(value));
		builder.append("\n");
		return builder.toString();
	}

	public void forEach(IntDoubleBiConsumer consumer)
	{
		for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
			consumer.accept(keys[i], values[i]);
		}
	}

	public void forEach(IntConsumer consumer)
	{
		for (int key : keys) {
			consumer.accept(key);
		}
	}

	public void scale(double scale)
	{
		assert scale > 0d;
		for (int i = 0; i < values.length; i++) {
			values[i] *= scale;
		}
	}

	public void scale()
	{
		scale(1 / sum());
	}

	public static final class Builder
	{
		private static final int INITIAL_SIZE = 16;
		private int[] keys;
		private double[] values;

		public Builder()
		{
			this(INITIAL_SIZE);
		}

		public Builder(int size)
		{
			keys = new int[size];
			values = new double[size];
			Arrays.fill(keys, Integer.MAX_VALUE);
		}

		public boolean add(int j, double prob)
		{
			int index = Arrays.binarySearch(keys, j);

			if (index >= 0) {
				// Have this key already
				values[index] += prob;
				return true;
			}
			if (prob == 0.0d)
				return true;

			index = -(index + 1);

			if (keys[keys.length - 1] != Integer.MAX_VALUE) {
				// No space left, resize
				int oldSize = keys.length;
				int newSize = oldSize * 2 + 1;
				keys = Arrays.copyOf(keys, newSize);
				values = Arrays.copyOf(values, newSize);
				Arrays.fill(keys, oldSize, newSize, Integer.MAX_VALUE);
			} else if (keys[index] == Integer.MAX_VALUE) {
				// Insertion point is free, overwrite it
				assert index == 0 || keys[index - 1] < Integer.MAX_VALUE;
				keys[index] = j;
				values[index] = prob;
				return false;
			}

			System.arraycopy(keys, index, keys, index + 1, keys.length - index - 1);
			System.arraycopy(values, index, values, index + 1, keys.length - index - 1);
			keys[index] = j;
			values[index] = prob;
			return false;
		}

		public Distribution build()
		{
			if (keys.length == 0 || keys[0] == Integer.MAX_VALUE) {
				// No entries at all
				return new Distribution();
			}
			if (keys[keys.length - 1] != Integer.MAX_VALUE) {
				// Our array is nicely filled
				return new Distribution(keys, values);
			}

			// Compact the array, removing superfluous indices. We could binary-search for MAX_VALUE - 1 here, too.
			assert keys.length >= 2;
			int maxIndex = keys.length - 2;
			while (keys[maxIndex] == Integer.MAX_VALUE) {
				maxIndex--;
			}
			int[] keys = Arrays.copyOf(this.keys, maxIndex + 1);
			double[] values = Arrays.copyOf(this.values, maxIndex + 1);
			return new Distribution(keys, values);
		}
	}

	private class DistributionEntry implements Map.Entry<Integer, Double>
	{
		private int index = -1;

		@Override public Integer getKey()
		{
			return keys[index];
		}

		@Override public Double getValue()
		{
			return values[index];
		}
		@Override public Double setValue(Double prob)
		{
			double oldValue = values[index];
			values[index] = prob;
			return oldValue;
		}
	}

	private class EntryIterator implements Iterator<Map.Entry<Integer, Double>>
	{
		private final DistributionEntry entry = new DistributionEntry();

		@Override
		public boolean hasNext()
		{
			return (entry.index + 1) < keys.length;
		}

		@Override
		public Map.Entry<Integer, Double> next()
		{
			entry.index++;
			return entry;
		}
	}
}
