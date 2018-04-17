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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;

import common.IterableStateSet;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import prism.PrismUtils;

/**
 * Interface for classes that provide (read) access to an explicit-state MDP,
 * where the transition probabilities are stored as double floating point values.
 * <br>
 * For the generic methods, e.g., the prob0 / prob1 precomputations that do not
 * care about the concrete values, see {@link explicit.MDPGeneric}.
 */
public interface MDP extends MDPGeneric<Double>
{
	/**
	 * Get an iterator over the transitions from choice {@code i} of state {@code s}.
	 */
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s, int i);

	/**
	 * Functional interface for a consumer,
	 * accepting transitions (s,t,d), i.e.,
	 * from state s to state t with value d.
	 */
	@FunctionalInterface
	public interface TransitionConsumer {
		void accept(int s, int t, double d);
	}

	/**
	 * Iterate over the outgoing transitions of state {@code s} and choice {@code i}
	 * and call the accept method of the consumer for each of them:
	 * <br>
	 * Call {@code accept(s,t,d)} where t is the successor state d = P(s,i,t)
	 * is the probability from s to t with choice i.
	 * <p>
	 * <i>Default implementation</i>: The default implementation relies on iterating over the
	 * iterator returned by {@code getTransitionsIterator()}.
	 * <p><i>Note</i>: This method is the base for the default implementation of the numerical
	 * computation methods (mvMult, etc). In derived classes, it may thus be worthwhile to
	 * provide a specialised implementation for this method that avoids using the Iterator mechanism.
	 *
	 * @param s the state s
	 * @param i the choice i
	 * @param c the consumer
	 */
	public default void forEachTransition(int s, int i, TransitionConsumer c)
	{
		for (Iterator<Entry<Integer, Double>> it = getTransitionsIterator(s, i); it.hasNext(); ) {
			Entry<Integer, Double> e = it.next();
			c.accept(s, e.getKey(), e.getValue());
		}
	}

	/**
	 * Functional interface for a function
	 * mapping transitions (s,t,d), i.e.,
	 * from state s to state t with value d,
	 * to a double value.
	 */
	@FunctionalInterface
	public interface TransitionToDoubleFunction {
		double apply(int s, int t, double d);
	}

	/**
	 * Iterate over the outgoing transitions of state {@code s} and choice {@code i},
	 * call the function {@code f} and return the sum of the result values:
	 * <br>
	 * Return sum_t f(s, t, P(s,i,t)), where t ranges over the i-successors of s.
	 *
	 * @param s the state s
	 * @param c the consumer
	 */
	public default double sumOverTransitions(final int s, final int i, final TransitionToDoubleFunction f)
	{
		class Sum {
			double sum = 0.0;

			void accept(int s, int t, double d)
			{
				sum += f.apply(s, t, d);
			}
		}

		Sum sum = new Sum();
		forEachTransition(s, i, sum::accept);

		return sum.sum;
	}

	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for all s: result[s] = min/max_k { sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement, int strat[])
	{
		mvMultMinMax(vect, min, result, new IterableStateSet(subset, getNumStates(), complement).iterator(), strat);
	}

	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for all s: result[s] = min/max_k { sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param result Vector to store result in
	 * @param states Perform computation for these rows, in the iteration order
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultMinMax(double vect[], boolean min, double result[], PrimitiveIterator.OfInt states, int strat[])
	{
		while (states.hasNext()) {
			final int s = states.nextInt();
			result[s] = mvMultMinMaxSingle(s, vect, min, strat);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max,
	 * i.e. return min/max_k { sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultMinMaxSingle(int s, double vect[], boolean min, int strat[])
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = mvMultSingle(s, choice, vect);

			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max (true=min, false=max)
	 * @param val Min or max value to match
	 */
	public default List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min, double val)
	{
		// Create data structures to store strategy
		final List<Integer> result = new ArrayList<Integer>();
		// One row of matrix-vector operation
		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = mvMultSingle(s, choice, vect);

			// Store strategy info if value matches
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				result.add(choice);
			}
		}

		return result;
	}

	/**
	 * Do a single row of matrix-vector multiplication for a specific choice.
	 * @param s State (row) index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 */
	public default double mvMultSingle(int s, int i, double vect[])
	{
		return sumOverTransitions(s, i, (int __, int t, double prob) -> {
			return prob * vect[t];
		});
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param min Min or max for (true=min, false=max)
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultGSMinMax(double vect[], boolean min, BitSet subset, boolean complement, boolean absolute, int strat[])
	{
		return mvMultGSMinMax(vect, min, new IterableStateSet(subset, getNumStates(), complement).iterator(), absolute, strat);
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param min Min or max for (true=min, false=max)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultGSMinMax(double vect[], boolean min, PrimitiveIterator.OfInt states, boolean absolute, int strat[])
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			d = mvMultJacMinMaxSingle(s, vect, min, strat);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max in the context of interval iteration.
	 * i.e. for all s: vect[s] = min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param min Min or max for (true=min, false=max)
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param ensureMonotonic ensure monotonicity
	 * @param fromBelow iteration from below or from above? (for ensureMonotonicity)
	 */
	public default void mvMultGSMinMaxIntervalIter(double vect[], boolean min, PrimitiveIterator.OfInt states, int strat[], boolean ensureMonotonic, boolean fromBelow)
	{
		double d;
		while (states.hasNext()) {
			final int s = states.nextInt();
			d = mvMultJacMinMaxSingle(s, vect, min, strat);
			if (ensureMonotonic) {
				if (fromBelow) {
					// from below: do max old and new
					if (vect[s] > d) {
						d = vect[s];
					}
				} else {
					// from above: do min old and new
					if (vect[s] < d) {
						d = vect[s];
					}
				}
				vect[s] = d;
			} else {
				vect[s] = d;
			}
		}
	}

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication followed by min/max.
	 * i.e. return min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultJacMinMaxSingle(int s, double vect[], boolean min, int strat[])
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			double d = mvMultJacSingle(s, choice, vect);

			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null) {
					stratCh = choice;
				}
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;

	}

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication for a specific choice.
	 * i.e. return min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * @param s Row index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 */
	public default double mvMultJacSingle(int s, int i, double vect[])
	{
		class Jacobi {
			double diag = 1.0;
			double d = 0.0;

			void accept(int s, int t, double prob) {
				if (t != s) {
					d += prob * vect[t];
				} else {
					diag -= prob;
				}
			}
		}

		Jacobi jac = new Jacobi();
		forEachTransition(s, i, jac::accept);

		double d = jac.d;
		double diag = jac.diag;
		if (diag > 0)
			d /= diag;

		return d;
	}

	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration.
	 * i.e. for all s: result[s] = min/max_k { rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultRewMinMax(double vect[], MDPRewards mdpRewards, boolean min, double result[], BitSet subset, boolean complement, int strat[])
	{
		for (OfInt it = new IterableStateSet(subset, getNumStates(), complement).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, strat);
		}
	}

	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration.
	 * i.e. for all s: result[s] = min/max_k { rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param result Vector to store result in
	 * @param states Perform computation for these rows, in the iteration order
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultRewMinMax(double vect[], MDPRewards mdpRewards, boolean min, double result[], PrimitiveIterator.OfInt states, int strat[])
	{
		while (states.hasNext()) {
			final int s = states.nextInt();
			result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, strat);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards followed by min/max.
	 * i.e. return min/max_k { rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultRewMinMaxSingle(int s, double vect[], MDPRewards mdpRewards, boolean min, int strat[])
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			double d = mvMultRewSingle(s, choice, vect, mdpRewards);
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards for a specific choice.
	 * i.e. rew(s) + rew_i(s) + sum_j P_i(s,j)*vect[j]
	 * @param s State (row) index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards (MDP rewards)
	 */
	public default double mvMultRewSingle(int s, int i, double vect[], MDPRewards mdpRewards)
	{
		double d = mdpRewards.getStateReward(s);
		d += mdpRewards.getTransitionReward(s, i);
		d += sumOverTransitions(s, i, (__, t, prob) -> {
			return prob * vect[t];
		});
		return d;
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards for a specific choice.
	 * i.e. rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j]
	 * @param s State (row) index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards (DTMC rewards)
	 */
	public default double mvMultRewSingle(int s, int i, double vect[], MCRewards mcRewards)
	{
		double d = mcRewards.getStateReward(s);
		// TODO: add transition rewards when added to MCRewards
		d += sumOverTransitions(s, i, (__, t, prob) -> {
			return prob * vect[t];
		});
		return d;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication and sum of rewards followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { rew(s) + rew_k(s) + (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @return The maximum difference between old/new elements of {@code vect}
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultRewGSMinMax(double vect[], MDPRewards mdpRewards, boolean min, BitSet subset, boolean complement, boolean absolute, int strat[])
	{
		return mvMultRewGSMinMax(vect, mdpRewards, min, new IterableStateSet(subset, getNumStates(), complement).iterator(), absolute, strat);
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication and sum of rewards followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { rew(s) + rew_k(s) + (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @return The maximum difference between old/new elements of {@code vect}
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultRewGSMinMax(double vect[], MDPRewards mdpRewards, boolean min, PrimitiveIterator.OfInt states, boolean absolute, int strat[])
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			d = mvMultRewJacMinMaxSingle(s, vect, mdpRewards, min, strat);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication and sum of rewards followed by min/max,
	 * for interval iteration.
	 * i.e. for all s: vect[s] = min/max_k { rew(s) + rew_k(s) + (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * @param ensureMonotonic enforce monotonicity?
	 * @param fromBelow interval iteration from below? (for ensureMonotonic)
	 */
	public default void mvMultRewGSMinMaxIntervalIter(double vect[], MDPRewards mdpRewards, boolean min, PrimitiveIterator.OfInt states, int strat[], boolean ensureMonotonic, boolean fromBelow)
	{
		double d;
		while (states.hasNext()) {
			final int s = states.nextInt();
			d = mvMultRewJacMinMaxSingle(s, vect, mdpRewards, min, strat);
			if (ensureMonotonic) {
				if (fromBelow) {
					// from below: do max old and new
					if (vect[s] > d) {
						d = vect[s];
					}
				} else {
					// from above: do min old and new
					if (vect[s] < d) {
						d = vect[s];
					}
				}
				vect[s] = d;
			} else {
				vect[s] = d;
			}
		}
	}

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication and sum of rewards followed by min/max.
	 * i.e. return min/max_k { rew(s) + rew_k(s) + (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s State (row) index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max for (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultRewJacMinMaxSingle(int s, double vect[], MDPRewards mdpRewards, boolean min, int strat[])
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			double d = mvMultRewJacSingle(s, choice, vect, mdpRewards);
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null) {
					stratCh = choice;
				}
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}


	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication and sum of rewards,
	 * for a specific choice.
	 * i.e. return rew(s) + rew_i(s) + (sum_{j!=s} P_i(s,j)*vect[j]) / 1-P_i(s,s) }
	 * @param s State (row) index
	 * @param i the choice index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 */
	public default double mvMultRewJacSingle(int s, int i, double vect[], MDPRewards mdpRewards)
	{
		class Jacobi {
			double diag = 1.0;
			double d = mdpRewards.getStateReward(s) + mdpRewards.getTransitionReward(s, i);
			boolean onlySelfLoops = true;

			void accept(int s, int t, double prob) {
				if (t != s) {
					d += prob * vect[t];
					onlySelfLoops = false;
				} else {
					diag -= prob;
				}
			}
		}

		Jacobi jac = new Jacobi();
		forEachTransition(s, i, jac::accept);

		double d = jac.d;
		double diag = jac.diag;

		if (jac.onlySelfLoops) {
			if (d != 0) {
				// always choosing the selfloop-action will produce infinite reward
				d = (d > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
			} else {
				// no reward & only self-loops: d remains 0
				d = 0;
			}
		} else {
			// not only self-loops, do Jacobi division
			if (diag > 0)
				d /= diag;
		}

		return d;
	}

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication and sum of rewards.
	 * @param s State (row) index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max (true=min, false=max)
	 * @param val Min or max value to match
	 */
	public default List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], MDPRewards mdpRewards, boolean min, double val)
	{
		// Create data structures to store strategy
		final List<Integer> result = new ArrayList<Integer>();

		// One row of matrix-vector operation
		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			double d = mvMultRewSingle(s, choice, vect, mdpRewards);
			// Store strategy info if value matches
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				result.add(choice);
			}
		}

		return result;
	}

	/**
	 * Multiply the probability matrix induced by the MDP and {@code strat}
	 * to the right of {@code source}. Only those entries in {@code source}
	 * and only those columns in the probability matrix are considered, that
	 * are elements of {@code states}.
	 * 
	 * The result of this multiplication is added to the contents of {@code dest}.
	 *   
	 * @param states States for which to multiply
	 * @param strat (Memoryless) strategy to use
	 * @param source Vector to multiply matrix with
	 * @param dest Vector to write result to.
	 */
	public default void mvMultRight(int[] states, int[] strat, double[] source, double[] dest)
	{
		for (int state : states) {
			forEachTransition(state, strat[state], (int s, int t, double prob) -> {
				dest[t] += prob * source[s];
			});
		}
	}

}
