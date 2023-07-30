//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;

import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MDPRewards;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state interval MDP.
 */
public interface IMDP<Value> extends MDP<Interval<Value>>
{
	// Accessors (for Model) - default implementations
	
	@Override
	public default ModelType getModelType()
	{
		return ModelType.IMDP;
	}

	// Accessors
	
	/**
	 * Checks that transition probability interval lower bounds are positive
	 * and throws an exception if any are not.
	 */
	public default void checkLowerBoundsArePositive() throws PrismException
	{
		Evaluator<Interval<Value>> eval = getEvaluator();
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = getNumChoices(s);
			for (int j = 0; j < numChoices; j++) {
				Iterator<Map.Entry<Integer, Interval<Value>>> iter = getTransitionsIterator(s, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Interval<Value>> e = iter.next();
					// NB: we phrase the check as an operation on intervals, rather than
					// accessing the lower bound directly, to make use of the evaluator
					if (!eval.gt(e.getValue(), eval.zero())) {
						List<State> sl = getStatesList();
						String state = sl == null ? "" + s : sl.get(s).toString();
						throw new PrismException("Transition probability has lower bound of 0 in state " + state);
					}
				}
			}
		}
	}
	
	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_k min/max_P { sum_j P(s,k,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultUnc(double vect[], MinMax minMax, double result[], BitSet subset, boolean complement, int[] strat)
	{
		mvMultUnc(vect, minMax, result, new IterableStateSet(subset, getNumStates(), complement).iterator(), strat);
	}

	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_k min/max_P { sum_j P(s,k,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param result Vector to store result in
	 * @param states Perform computation for these rows, in the iteration order
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultUnc(double vect[], MinMax minMax, double result[], PrimitiveIterator.OfInt states, int[] strat)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultUncSingle(s, vect, minMax, strat);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max,
	 * i.e. return min/max_k min/max_P { sum_j P(s,k,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultUncSingle(int s, double vect[], MinMax minMax, int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;
		boolean min = minMax.isMin();

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = mvMultUncSingle(s, choice, vect, minMax);

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
	 * Do a single row of matrix-vector multiplication for a specific choice k
	 * i.e. return min/max_P { sum_j P(s,k,j)*vect[j] }
	 * @param s State (row) index
	 * @param k Choice index
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 */
	public default double mvMultUncSingle(int s, int k, double vect[], MinMax minMax)
	{
		@SuppressWarnings("unchecked")
		DoubleIntervalDistribution did = IntervalUtils.extractDoubleIntervalDistribution(((IMDP<Double>) this).getTransitionsIterator(s, k), getNumTransitions(s, k)); 
		return IDTMC.mvMultUncSingle(did, vect, minMax);
	}
	
	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_k min/max_P { rew(s) + rew_k(s) + sum_j P(s,k,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultRewUnc(double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, double result[], BitSet subset, boolean complement, int[] strat)
	{
		mvMultRewUnc(vect, mdpRewards, minMax, result, new IterableStateSet(subset, getNumStates(), complement).iterator(), strat);
	}

	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_k min/max_P { rew(s) + rew_k(s) + sum_j P(s,k,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param result Vector to store result in
	 * @param states Perform computation for these rows, in the iteration order
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultRewUnc(double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, double result[], PrimitiveIterator.OfInt states, int[] strat)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultRewUncSingle(s, vect, mdpRewards, minMax, strat);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards followed by min/max,
	 * i.e. return min/max_k min/max_P { rew(s) + rew_k(s) + sum_j P(s,k,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultRewUncSingle(int s, double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;
		boolean min = minMax.isMin();

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			double d = mvMultRewUncSingle(s, choice, vect, mdpRewards, minMax);
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
	 * Do a single row of matrix-vector multiplication for a specific choice k
	 * i.e. return min/max_P { rew(s) + rew_k(s) + sum_j P(s,k,j)*vect[j] }
	 * @param s State (row) index
	 * @param k Choice index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards (MDP rewards)
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 */
	public default double mvMultRewUncSingle(int s, int k, double vect[], MDPRewards<Double> mdpRewards, MinMax minMax)
	{
		double d = mdpRewards.getStateReward(s);
		d += mdpRewards.getTransitionReward(s, k);
		d += mvMultUncSingle(s, k, vect, minMax);
		return d;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_k min/max_P { rew(s) + rew_k(s) + sum_j P(s,k,j)*vect[j] }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 */
	public default double mvMultUncGS(double vect[], MinMax minMax, PrimitiveIterator.OfInt states, boolean absolute, int[] strat)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			//d = mvMultJacSingle(s, vect, minMax);
			// Just do a normal (non-Jacobi) state update - not so easy to adapt for intervals
			d = mvMultUncSingle(s, vect, minMax, strat);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_k min/max_P { rew(s) + rew_k(s) + sum_j P(s,k,j)*vect[j] }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min/max info (strategy and uncertainty)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 */
	public default double mvMultRewUncGS(double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, PrimitiveIterator.OfInt states, boolean absolute, int[] strat)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			//d = mvMultJacSingle(s, vect, minMax);
			// Just do a normal (non-Jacobi) state update - not so easy to adapt for intervals
			d = mvMultRewUncSingle(s, vect, mdpRewards, minMax, strat);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}
}
