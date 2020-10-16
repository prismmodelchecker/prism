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

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;

import common.IterableStateSet;
import common.iterable.IterableInt;
import common.iterable.MappingIterator;
import prism.ModelType;
import prism.Pair;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import explicit.graphviz.Decorator;
import explicit.rewards.MCRewards;

/**
 * Interface for classes that provide (read) access to an explicit-state DTMC.
 */
public interface DTMC extends Model
{
	// Accessors (for Model) - default implementations
	
	@Override
	default ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	default void exportToPrismExplicitTra(PrismLog out)
	{
		// Output transitions to .tra file
		int numStates = getNumStates();
		out.print(numStates + " " + getNumTransitions() + "\n");
		TreeMap<Integer, Pair<Double, Object>> sorted = new TreeMap<Integer, Pair<Double, Object>>();
		for (int i = 0; i < numStates; i++) {
			// Extract transitions and sort by destination state index (to match PRISM-exported files)
			Iterator<Map.Entry<Integer,Pair<Double, Object>>> iter = getTransitionsAndActionsIterator(i);
			while (iter.hasNext()) {
				Map.Entry<Integer, Pair<Double, Object>> e = iter.next();
				sorted.put(e.getKey(), e.getValue());
			}
			// Print out (sorted) transitions
			for (Map.Entry<Integer, Pair<Double, Object>> e : sorted.entrySet()) {
				// Note use of PrismUtils.formatDouble to match PRISM-exported files
				out.print(i + " " + e.getKey() + " " + PrismUtils.formatDouble(e.getValue().first));
				Object action = e.getValue().second; 
				if (action != null && !"".equals(action)) {
					out.print(" " + action);
				}
				out.print("\n");
			}
			sorted.clear();
		}
	}

	@Override
	default void exportTransitionsToDotFile(int i, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		// Iterate through outgoing transitions for this state
		Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i);
		while (iter.hasNext()) {
			Map.Entry<Integer, Double> e = iter.next();
			// Print a new dot file line for the arrow for this transition
			out.print(i + " -> " + e.getKey());
			// Annotate this arrow with the probability 
			explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
			d.setLabel(e.getValue().toString());
			// Apply any other decorators requested
			if (decorators != null) {
				for (Decorator decorator : decorators) {
					d = decorator.decorateProbability(i, e.getKey(), e.getValue(), d);
				}
			}
			// Append to the dot file line for this transition
			out.println(" " + d.toString() + ";");
		}
	}

	@Override
	default void exportToPrismLanguage(final String filename) throws PrismException
	{
		try (FileWriter out = new FileWriter(filename)) {
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (getNumStates() - 1) + "];\n");
			final TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
			for (int state = 0, max = getNumStates(); state < max; state++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
					final Entry<Integer, Double> transition = transitions.next();
					sorted.put(transition.getKey(), transition.getValue());
				}
				// Print out (sorted) transitions
				out.write("[]x=" + state + "->");
				boolean first = true;
				for (Entry<Integer, Double> transition : sorted.entrySet()) {
					if (first)
						first = false;
					else
						out.write("+");
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.write(PrismUtils.formatDouble(transition.getValue()) + ":(x'=" + transition.getKey() + ")");
				}
				out.write(";\n");
				sorted.clear();
			}
			out.write("endmodule\n");
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	default String infoString()
	{
		String s = "";
		s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		return s;
	}

	@Override
	default String infoStringTable()
	{
		String s = "";
		s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	// Accessors
	
	/**
	 * Get an iterator over the transitions from state s.
	 */
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s);

	/**
	 * Get an iterator over the transitions from state s, with their attached actions if present.
	 */
	public default Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(int s)
	{
		// Default implementation just adds null actions 
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(s);
		return new MappingIterator.From<>(transitions, transition -> attachAction(transition, null));
	}

	/**
	 * Attach an action to a transition, assuming iterators as used in
	 * {@link #getTransitionsIterator(int)} and {@link #getTransitionsAndActionsIterator(int)}
	 */
	public static Entry<Integer, Pair<Double, Object>> attachAction(final Entry<Integer, Double> transition, final Object action)
	{
		final Integer state = transition.getKey();
		final Double probability = transition.getValue();
		return new AbstractMap.SimpleImmutableEntry<>(state, new Pair<>(probability, action));
	}

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
	 * Iterate over the outgoing transitions of state {@code s} and call the accept method
	 * of the consumer for each of them:
	 * <br>
	 * Call {@code accept(s,t,d)} where t is the successor state and,
	 * in a DTMC, d = P(s,t) is the probability from s to t,
	 * while in CTMC, d = R(s,t) is the rate from s to t.
	 * <p>
	 * <i>Default implementation</i>: The default implementation relies on iterating over the
	 * iterator returned by {@code getTransitionsIterator()}.
	 * <p><i>Note</i>: This method is the base for the default implementation of the numerical
	 * computation methods (mvMult, etc). In derived classes, it may thus be worthwhile to
	 * provide a specialised implementation for this method that avoids using the Iterator mechanism.
	 *
	 * @param s the state s
	 * @param c the consumer
	 */
	public default void forEachTransition(int s, TransitionConsumer c)
	{
		for (Iterator<Entry<Integer, Double>> it = getTransitionsIterator(s); it.hasNext(); ) {
			Entry<Integer, Double> e = it.next();
			c.accept(s, e.getKey(), e.getValue());
		}
	}

	/**
	 * Functional interface for a function
	 * mapping transitions (s,t,d), i.e.,
	 * from state s to state t with value d
	 * to a double value.
	 */
	@FunctionalInterface
	public interface TransitionToDoubleFunction {
		double apply(int s, int t, double d);
	}

	/**
	 * Iterate over the outgoing transitions of state {@code s}, call the function {@code f}
	 * and return the sum of the result values:
	 * <br>
	 * Return sum_t f(s, t, P(s,t)), where t ranges over the successors of s.
	 *
	 * @param s the state s
	 * @param c the consumer
	 */
	public default double sumOverTransitions(final int s, final TransitionToDoubleFunction f)
	{
		class Sum {
			double sum = 0.0;

			void accept(int s, int t, double d)
			{
				sum += f.apply(s, t, d);
			}
		}

		Sum sum = new Sum();
		forEachTransition(s, sum::accept);

		return sum.sum;
	}

	/**
	 * Perform a single step of precomputation algorithm Prob0 for a single state,
	 * i.e., for the state {@code s} returns true iff there is a transition from
	 * {@code s} to a state in {@code u}.
	 * <br>
	 * <i>Default implementation</i>: Iterates using {@code getSuccessors()} and performs the check.
	 * @param s The state in question
	 * @param u Set of states {@code u}
	 * @return true iff there is a transition from s to a state in u
	 */
	public default boolean prob0step(int s, BitSet u)
	{
		for (SuccessorsIterator succ = getSuccessors(s); succ.hasNext(); ) {
			int t = succ.nextInt();
			if (u.get(t))
				return true;
		}
		return false;
	}

	/**
	 * Perform a single step of precomputation algorithm Prob0, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff there is a transition to a state in {@code u}.
	 * <br>
	 * <i>Default implementation</i>: Iterate over {@code subset} and use {@code prob0step(s,u)}
	 * to determine result for {@code s}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param result Store results here
	 */
	public default void prob0step(BitSet subset, BitSet u, BitSet result)
	{
		for (OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			int s = it.nextInt();
			result.set(s, prob0step(s,u));
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1 for a single state,
	 * i.e., for states s return true iff there is a transition to a state in
	 * {@code v} and all transitions go to states in {@code u}.
	 * @param s The state in question
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @return true iff there is a transition from s to a state in v and all transitions go to u.
	 */
	public default boolean prob1step(int s, BitSet u, BitSet v)
	{
		boolean allTransitionsToU = true;
		boolean hasTransitionToV = false;
		for (SuccessorsIterator succ = getSuccessors(s); succ.hasNext(); ) {
			int t = succ.nextInt();
			if (!u.get(t)) {
				allTransitionsToU = false;
				// early abort, as overall result is false
				break;
			}
			hasTransitionToV = hasTransitionToV || v.get(t);
		}
		return (allTransitionsToU && hasTransitionToV);
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param result Store results here
	 */
	public default void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		for (OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			int s = it.nextInt();
			result.set(s, prob1step(s,u,v));
		}
	}

	/**
	 * Do a matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. for all s: result[s] = sum_j P(s,j)*vect[j]
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 */
	public default void mvMult(double vect[], double result[], BitSet subset, boolean complement)
	{
		mvMult(vect, result, new IterableStateSet(subset, getNumStates(), complement).iterator());
	}

	/**
	 * Do a matrix-vector multiplication for the DTMC's transition probability matrix P
	 * and the vector {@code vect} passed in, for the state indices provided by the iterator,
	 * i.e., for all s of {@code states}: result[s] = sum_j P(s,j)*vect[j]
	 * <p>
	 * If the state indices in the iterator are not distinct, the result will still be valid,
	 * but this situation should be avoided for performance reasons.
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param states Perform multiplication for these rows, in the iteration order
	 */
	public default void mvMult(double vect[], double result[], PrimitiveIterator.OfInt states)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultSingle(s, vect);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. return sum_j P(s,j)*vect[j]
	 * @param s Row index
	 * @param vect Vector to multiply by
	 */
	public default double mvMultSingle(int s, double vect[])
	{
		return sumOverTransitions(s, (__, t, prob) -> {
			return prob * vect[t];
		});
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in,
	 * storing new values directly in {@code vect} as computed.
	 * i.e. for all s: vect[s] = (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultGS(double vect[], BitSet subset, boolean complement, boolean absolute)
	{
		return mvMultGS(vect, new IterableStateSet(subset, getNumStates(), complement).iterator(), absolute);
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in,
	 * storing new values directly in {@code vect} as computed.
	 * The order and subset of states is given by the iterator {@code states},
	 * i.e. for s in {@code states}: vect[s] = (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param states Do multiplication for these rows, in this order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultGS(double vect[], PrimitiveIterator.OfInt states, boolean absolute)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			int s = states.nextInt();

			d = mvMultJacSingle(s, vect);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication (in the interval iteration context) for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in,
	 * storing new values directly in {@code vect} as computed (for use in interval iteration).
	 * i.e. for all s: vect[s] = (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * @param vect Vector to multiply by (and store the result in)
	 * @param states Do multiplication for these rows, in this order
	 * @param ensureMonotonic ensure monotonicity
	 * @param checkMonotonic check monotonicity
	 * @param fromBelow iteration from below or from above? (for ensureMonotonicity, checkMonotonicity)
	 */
	public default void mvMultGSIntervalIter(double vect[], PrimitiveIterator.OfInt states, boolean ensureMonotonic, boolean checkMonotonic, boolean fromBelow) throws PrismException
	{
		double d;
		while (states.hasNext()) {
			int s = states.nextInt();

			d = mvMultJacSingle(s, vect);
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
			}
			if (checkMonotonic) {
				if (fromBelow) {
					if (vect[s] > d) {
						throw new PrismException("Monotonicity violated (from below): old value " + vect[s] + " > new value " + d);
					}
				} else {
					if (vect[s] < d) {
						throw new PrismException("Monotonicity violated (from above): old value " + vect[s] + " < new value " + d);
					}
				}
			}
			vect[s] = d;
		}
	}

	/**
	 * Do a Jacobi-style matrix-vector multiplication for the DTMC's transition probability matrix P
	 * and the vector {@code vect} passed in, for the state indices provided by the iterator,
	 * i.e., for all s of {@code states}: result[s] = (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * <p>
	 * If the state indices in the iterator are not distinct, the result will still be valid,
	 * but this situation should be avoided for performance reasons.
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param states Perform multiplication for these rows, in the iteration order
	 */
	public default void mvMultJac(double vect[], double result[], PrimitiveIterator.OfInt states)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultJacSingle(s, vect);
		}
	}

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. return (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * @param s Row index
	 * @param vect Vector to multiply by
	 */
	public default double mvMultJacSingle(int s, double vect[])
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
		forEachTransition(s, jac::accept);

		double d = jac.d;
		double diag = jac.diag;
		if (diag > 0)
			d /= diag;

		return d;
	}

	/**
	 * Do a matrix-vector multiplication and sum of action reward.
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 */
	public default void mvMultRew(double vect[], MCRewards mcRewards, double result[], BitSet subset, boolean complement)
	{
		mvMultRew(vect, mcRewards, result, new IterableStateSet(subset, getNumStates(), complement).iterator());
	}

	/**
	 * Do a matrix-vector multiplication and sum of action reward.
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param result Vector to store result in
	 * @param states Do multiplication for these rows, in the specified order
	 */
	public default void mvMultRew(double vect[], MCRewards mcRewards, double result[], PrimitiveIterator.OfInt states)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultRewSingle(s, vect, mcRewards);
		}
	}

	/**
	 * Do a matrix-vector multiplication and sum of action reward (Jacobi).
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param result Vector to store result in
	 * @param states Do multiplication for these rows, in the specified order
	 */
	public default void mvMultRewJac(double vect[], MCRewards mcRewards, double result[], PrimitiveIterator.OfInt states)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultRewJacSingle(s, vect, mcRewards);
		}
	}

	/**
	 * Do a matrix-vector multiplication and sum of action reward (Gauss-Seidel).
	 * @param vect Vector to multiply by and store result in
	 * @param mcRewards The rewards
	 * @param states Do multiplication for these rows, in the specified order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultRewGS(double vect[], MCRewards mcRewards, PrimitiveIterator.OfInt states, boolean absolute)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			int s = states.nextInt();
			d = mvMultRewJacSingle(s, vect, mcRewards);

			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a matrix-vector multiplication and sum of action reward (Gauss-Seidel, interval iteration context).
	 * @param vect Vector to multiply by and store result in
	 * @param mcRewards The rewards
	 * @param states Do multiplication for these rows, in the specified order
	 * @param ensureMonotonic ensure monotonicity
	 * @param checkMonotonic check monotonicity
	 * @param fromBelow iteration from below or from above? (for ensureMonotonicity, checkMonotonicity)
	 */
	public default void mvMultRewGSIntervalIter(double vect[], MCRewards mcRewards, PrimitiveIterator.OfInt states, boolean ensureMonotonic, boolean checkMonotonic, boolean fromBelow) throws PrismException
	{
		double d;
		while (states.hasNext()) {
			int s = states.nextInt();

			d = mvMultRewJacSingle(s, vect, mcRewards);
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
			}
			if (checkMonotonic) {
				if (fromBelow) {
					if (vect[s] > d) {
						throw new PrismException("Monotonicity violated (from below): old value " + vect[s] + " > new value " + d);
					}
				} else {
					if (vect[s] < d) {
						throw new PrismException("Monotonicity violated (from above): old value " + vect[s] + " < new value " + d);
					}
				}
			}
			vect[s] = d;
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of action reward.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 */
	public default double mvMultRewSingle(int s, double vect[], MCRewards mcRewards)
	{
		double d = mcRewards.getStateReward(s);
		d += sumOverTransitions(s, (__, t, prob) -> {
			return prob * vect[t];
		});
		return d;
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of reward, Jacobi-style,
	 * i.e., return  ( rew(s) + sum_{t!=s} P(s,t)*vect[t] ) / (1 - P(s,s))
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 */
	public default double mvMultRewJacSingle(int s, double vect[], MCRewards mcRewards)
	{
		class Jacobi {
			double diag = 1.0;
			double d = mcRewards.getStateReward(s);
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
		forEachTransition(s, jac::accept);

		double d = jac.d;
		double diag = jac.diag;

		if (jac.onlySelfLoops) {
			if (d != 0) {
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
	 * Do a vector-matrix multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. for all s: result[s] = sum_i P(i,s)*vect[i]
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 */
	public default void vmMult(double vect[], double result[])
	{
		// Initialise result to 0
		Arrays.fill(result, 0);
		// Go through matrix elements (by row)
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			forEachTransition(state, (s, t, prob) -> {
				result[t] += prob * vect[s];
			});
		}
	}

	/**
	 * Do a vector-matrix multiplication for steady-state computation with the power method.
	 * <p>
	 * Computes<br>
	 * {@code result = vect * P} with matrix
	 * {@code P = (Q * deltaT + I)} where<br/>
	 * {@code Q} is the generator matrix,
	 * {@code deltaT} the preconditioning factor and
	 * {@code I} is the the identity matrix.<br/>
	 * Please refer to <em>William J. Stewart: "Introduction to the Numerical Solution of Markov Chains"</em> p.124 for details.
	 * </p>
	 * <p>
	 * If the {@code states} argument only specifies a subset of the state space,
	 * only those entries of the {@code result} vector are modified that are either
	 * states in {@code states} or their successors; other entries remain unchanged.
	 * Thus, it generally only makes sense to use this method with a state sets that consists
	 * of (the union of) bottom strongly-connected components (BSCCs).
	 * </p>
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param diagsQ vector of the diagonal entries of the generator matrix Q, i.e., diagsQ[s] = -sum_{s!=t} prob(s,t)
	 * @param deltaT deltaT conditioning factor
	 * @param states subset of states to consider
	 */
	public default void vmMultPowerSteadyState(double vect[], double[] result, double[] diagsQ, double deltaT, IterableInt states)
	{
		// Recall that the generator matrix Q has entries
		//       Q(s,s) = -sum_{t!=s} prob(s,t)
		// and   Q(s,t) = prob(s,t)  for s!=t
		// The values Q(s,s) are passed in via the diagsQ vector, while the
		// values Q(s,t) correspond to the normal transitions

		// Initialise result for relevant states to vect[s] * (deltaT * diagsQ[s] + 1),
		// i.e., handle the product with the diagonal entries of (deltaT * Q) + I
		for (OfInt it = states.iterator(); it.hasNext(); ) {
			int state = it.nextInt();
			result[state] = vect[state] * ((deltaT * diagsQ[state]) + 1.0);
		}

		// For each relevant state...
		for (OfInt it = states.iterator(); it.hasNext();) {
			int state = it.nextInt();

			// ... handle all Q(state,t) entries of the generator matrix
			forEachTransition(state, (s, t, prob) -> {
				if (s != t) {
					// ignore self loop, diagonal entries of the generator matrix handled above
					// update result vector entry for the *successor* state
					result[t] += deltaT * prob * vect[s];
				}
			});
		}
	}

}
