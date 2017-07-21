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

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;

import common.IterableStateSet;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;

/**
 * Interface for classes that provide (read) access to an explicit-state MDP.
 */
public interface MDP extends NondetModel
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
	 * Perform a single step of precomputation algorithm Prob0, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for all/some choices,
	 * there is a transition to a state in {@code u}.
	 * Quantification over choices is determined by {@code forall}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param forall For-all or there-exists (true=for-all, false=there-exists)
	 * @param result Store results here
	 */
	public default void prob0step(final BitSet subset, final BitSet u, final boolean forall, final BitSet result)
	{
		for (OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			boolean b1 = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				boolean b2 = someSuccessorsInSet(s, choice, u);
				if (forall) {
					if (!b2) {
						b1 = false;
						break;
					}
				} else {
					if (b2) {
						b1 = true;
						break;
					}
				}
			}
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1A, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for all choices,
	 * there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param result Store results here
	 */
	public default void prob1Astep(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		boolean b1;
		for (OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			b1 = true;
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				if (!(successorsSafeAndCanReach(s, choice, u, v))) {
					b1 = false;
					break;
				}
			}
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1E, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for some choice,
	 * there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * Optionally, store optimal (memoryless) strategy info for 1 states. 
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param result Store results here
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void prob1Estep(BitSet subset, BitSet u, BitSet v, BitSet result, int strat[])
	{
		int stratCh = -1;
		boolean b1;
		for (OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			b1 = false;
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				if (successorsSafeAndCanReach(s, choice, u, v)) {
					b1 = true;
					// If strategy generation is enabled, remember optimal choice
					if (strat != null)
						stratCh = choice;
					break;
				}
			}
			// If strategy generation is enabled, store optimal choice
			// (only if this the first time we add the state to S^yes)
			if (strat != null & b1 & !result.get(s)) {
				strat[s] = stratCh;
			}
			// Store result
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for all/some choices,
	 * there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * Quantification over choices is determined by {@code forall}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param forall For-all or there-exists (true=for-all, false=there-exists)
	 * @param result Store results here
	 */
	public default void prob1step(BitSet subset, BitSet u, BitSet v, boolean forall, BitSet result)
	{
		boolean b1, b2;
		for (OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			b1 = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				b2 = successorsSafeAndCanReach(s, choice, u, v);
				if (forall) {
					if (!b2) {
						b1 = false;
						break;
					}
				} else {
					if (b2) {
						b1 = true;
						break;
					}
				}
			}
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1 for a single state/choice,
	 * i.e., return whether there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * @param s State (row) index
	 * @param i Choice index
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 */
	public default boolean prob1stepSingle(int s, int i, BitSet u, BitSet v)
	{
		return successorsSafeAndCanReach(s, i, u, v);
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
	public void mvMultMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement, int strat[]);

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max,
	 * i.e. return min/max_k { sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info. 
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public double mvMultMinMaxSingle(int s, double vect[], boolean min, int strat[]);

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max (true=min, false=max)
	 * @param val Min or max value to match
	 */
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min, double val);

	/**
	 * Do a single row of matrix-vector multiplication for a specific choice.
	 * @param s State (row) index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 */
	public double mvMultSingle(int s, int i, double vect[]);

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
	public double mvMultGSMinMax(double vect[], boolean min, BitSet subset, boolean complement, boolean absolute, int strat[]);

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication followed by min/max.
	 * i.e. return min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * Optionally, store optimal (memoryless) strategy info. 
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public double mvMultJacMinMaxSingle(int s, double vect[], boolean min, int strat[]);

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication for a specific choice.
	 * i.e. return min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * @param s Row index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 */
	public double mvMultJacSingle(int s, int i, double vect[]);

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
	public void mvMultRewMinMax(double vect[], MDPRewards mdpRewards, boolean min, double result[], BitSet subset, boolean complement, int strat[]);

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
	public double mvMultRewMinMaxSingle(int s, double vect[], MDPRewards mdpRewards, boolean min, int strat[]);

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards for a specific choice.
	 * i.e. rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j]
	 * @param s State (row) index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 */
	public double mvMultRewSingle(int s, int i, double vect[], MCRewards mcRewards);

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
	public double mvMultRewGSMinMax(double vect[], MDPRewards mdpRewards, boolean min, BitSet subset, boolean complement, boolean absolute, int strat[]);

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
	public double mvMultRewJacMinMaxSingle(int s, double vect[], MDPRewards mdpRewards, boolean min, int strat[]);

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication and sum of rewards.
	 * @param s State (row) index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max (true=min, false=max)
	 * @param val Min or max value to match
	 */
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], MDPRewards mdpRewards, boolean min, double val);

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
	public void mvMultRight(int[] states, int[] strat, double[] source, double[] dest);
}
