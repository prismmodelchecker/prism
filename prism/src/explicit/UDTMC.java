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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;

import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MCRewards;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state uncertain DTMC.
 */
public interface UDTMC<Value> extends Model<Value>
{
	// Accessors (for Model) - default implementations
	
	@Override
	public default ModelType getModelType()
	{
		return ModelType.UDTMC;
	}

	// Accessors
	
	/**
	 * Checks that transition probability interval lower bounds are positive
	 * and throws an exception if any are not.
	 */
	public default void checkLowerBoundsArePositive() throws PrismException
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_P { sum_j P(s,j)*vect[j] }
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 */
	public default void mvMultUnc(double vect[], MinMax minMax, double result[], BitSet subset, boolean complement)
	{
		mvMultUnc(vect, minMax, result, new IterableStateSet(subset, getNumStates(), complement).iterator());
	}

	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_P { sum_j P(s,j)*vect[j] }
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param result Vector to store result in
	 * @param states Perform computation for these rows, in the iteration order
	 */
	public default void mvMultUnc(double vect[], MinMax minMax, double result[], PrimitiveIterator.OfInt states)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultUncSingle(s, vect, minMax);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max,
	 * i.e. return min/max_P { sum_j P(s,j)*vect[j] }
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 */
	public double mvMultUncSingle(int s, double vect[], MinMax minMax);

	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_P { rew(s) + sum_j P(s,j)*vect[j] }
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 */
	public default void mvMultRewUnc(double vect[], MCRewards<Double> mcRewards, MinMax minMax, double result[], BitSet subset, boolean complement)
	{
		mvMultRewUnc(vect, mcRewards, minMax, result, new IterableStateSet(subset, getNumStates(), complement).iterator());
	}

	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_P { rew(s) + sum_j P(s,j)*vect[j] }
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param result Vector to store result in
	 * @param states Perform computation for these rows, in the iteration order
	 */
	public default void mvMultRewUnc(double vect[], MCRewards<Double> mcRewards, MinMax minMax, double result[], PrimitiveIterator.OfInt states)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultRewUncSingle(s, vect, mcRewards, minMax);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards followed by min/max,
	 * i.e. return min/max_P { rew(s) + sum_j P(s,j)*vect[j] }
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 */
	public default double mvMultRewUncSingle(int s, double vect[], MCRewards<Double> mcRewards, MinMax minMax)
	{
		double d = mcRewards.getStateReward(s);
		// TODO d += mcRewards.getTransitionReward(s);
		d += mvMultUncSingle(s, vect, minMax);
		return d;
	}
	
	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_P { sum_j P(s,j)*vect[j] }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 */
	public default double mvMultUncGS(double vect[], MinMax minMax, PrimitiveIterator.OfInt states, boolean absolute)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			//d = mvMultJacSingle(s, vect, minMax);
			// Just do a normal (non-Jacobi) state update - not so easy to adapt for intervals
			d = mvMultUncSingle(s, vect, minMax);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration,
	 * i.e. for each s: result[s] = min/max_P { rew(s) + sum_j P(s,j)*vect[j] }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 */
	public default double mvMultRewUncGS(double vect[], MCRewards<Double> mcRewards, MinMax minMax, PrimitiveIterator.OfInt states, boolean absolute)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			//d = mvMultJacSingle(s, vect, minMax);
			// Just do a normal (non-Jacobi) state update - not so easy to adapt for intervals
			d = mvMultRewUncSingle(s, vect, mcRewards, minMax);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}
}
