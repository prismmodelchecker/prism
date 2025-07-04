//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.Function;

import common.IterableStateSet;
import common.iterable.PrimitiveIterable;
import explicit.rewards.MCRewards;
import io.ExplicitModelImporter;
import io.IOUtils;
import prism.ActionListOwner;
import prism.Pair;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Sparse matrix (non-mutable) explicit-state representation of a DTMC.
 * This is much faster to access than e.g. DTMCSimple and should also be more compact.
 * The catch is that you have to create the model all in one go and then can't modify it.
 */
public class DTMCSparse extends DTMCExplicit<Double>
{
	// Sparse matrix storing transition function (Steps)
	/** Indices into probabilities/columns giving the start of the transitions for each state (distribution);
	 * array is of size numStates+1 and last entry is always equal to getNumTransitions() */
	private int rows[];
	/** Column (destination) indices for each transition (array of size numTransitions) */
	private int columns[];
	/** Probabilities for each transition (array of size numTransitions) */
	private double probabilities[];
	/** Optionally, action labels for each transition (array of size numTransitions or null) */
	private Object actions[];

	public DTMCSparse(final DTMC<Double> dtmc)
	{
		initialise(dtmc.getNumStates());
		if (dtmc instanceof ActionListOwner) {
			actionList.copyFrom(((ActionListOwner) dtmc).getActionList());
		}
		for (Integer state : dtmc.getDeadlockStates()) {
			deadlocks.add(state);
		}
		for (Integer state : dtmc.getInitialStates()) {
			initialStates.add(state);
		}
		constantValues = dtmc.getConstantValues();
		varList = dtmc.getVarList();
		statesList = dtmc.getStatesList();
		for (String label : dtmc.getLabels()) {
			labels.put(label, dtmc.getLabelStates(label));
		}

		// Copy transition function
		final int numTransitions = dtmc.getNumTransitions();
		rows = new int[numStates + 1];
		rows[numStates] = numTransitions;
		columns = new int[numTransitions];
		probabilities = new double[numTransitions];
		for (int state=0, column=0; state<numStates; state++) {
			rows[state] = column;
			for (Iterator<Entry<Integer, Pair<Double, Object>>> transitions = dtmc.getTransitionsAndActionsIterator(state); transitions.hasNext();) {
				final Entry<Integer, Pair<Double, Object>> transition = transitions.next();
				final double probability = transition.getValue().first;
				if (probability > 0) {
					columns[column] = transition.getKey();
					probabilities[column] = probability;
					if (transition.getValue().second != null) {
						if (actions == null) {
							actions = new Object[numTransitions];
						}
						actions[column] = transition.getValue().second;
					}
					column++;
				}
			}
		}
		predecessorRelation = dtmc.hasStoredPredecessorRelation() ? dtmc.getPredecessorRelation(null, false) : null;
	}

	public DTMCSparse(final DTMC<Double> dtmc, int[] permut)
	{
		initialise(dtmc.getNumStates());
		if (dtmc instanceof ActionListOwner) {
			actionList.copyFrom(((ActionListOwner) dtmc).getActionList());
		}
		for (Integer state : dtmc.getDeadlockStates()) {
			deadlocks.add(permut[state]);
		}
		for (Integer state : dtmc.getInitialStates()) {
			initialStates.add(permut[state]);
		}
		constantValues = dtmc.getConstantValues();
		varList = dtmc.getVarList();
		statesList = null;
		labels.clear();

		// Compute the inverse of the permutation
		int[] permutInv = new int[numStates];
		for (int state = 0; state < numStates; state++) {
			permutInv[permut[state]] = state;
		}
		// Copy transition function
		final int numTransitions = dtmc.getNumTransitions();
		rows = new int[numStates + 1];
		rows[numStates] = numTransitions;
		columns = new int[numTransitions];
		probabilities = new double[numTransitions];
		for (int state=0, column=0; state<numStates; state++) {
			rows[state] = column;
			final int originalState = permutInv[state];
			for (Iterator<Entry<Integer, Pair<Double, Object>>> transitions = dtmc.getTransitionsAndActionsIterator(originalState); transitions.hasNext();) {
				final Entry<Integer, Pair<Double, Object>> transition = transitions.next();
				final double probability = transition.getValue().first;
				if (probability > 0) {
					columns[column] = permut[transition.getKey()];
					probabilities[column] = probability;
					if (transition.getValue().second != null) {
						if (actions == null) {
							actions = new Object[numTransitions];
						}
						actions[column] = transition.getValue().second;
					}
					column++;
				}
			}
		}
	}

	/**
	 * Construct an empty DTMC (e.g. for subsequent explicit import)
	 */
	public DTMCSparse()
	{
	}

	//--- Model ---

	@Override
	public List<Object> findActionsUsed()
	{
		if (actions == null) {
			return Collections.singletonList(null);
		} else {
			LinkedHashSet<Object> allActions = new LinkedHashSet<>();
			int n = actions.length;
			for (int i = 0; i < n; i++) {
				allActions.add(actions[i]);
			}
			return new ArrayList<>(allActions);
		}
	}

	@Override
	public boolean onlyNullActionUsed()
	{
		return actions == null;
	}

	@Override
	public int getNumTransitions()
	{
		return rows[numStates];
	}

	@Override
	public int getNumTransitions(int state)
	{
		return rows[state + 1] - rows[state];
	}

	@Override
	public OfInt getSuccessorsIterator(final int state)
	{
		return Arrays.stream(columns, rows[state], rows[state+1]).iterator();
	}

	@Override
	public SuccessorsIterator getSuccessors(int state)
	{
		// We assume here that all the successor states for a given state are distinct
		return SuccessorsIterator.from(getSuccessorsIterator(state), true);
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		for (int i=rows[s1], stop=rows[s1+1]; i < stop; i++) {
			if (columns[i] == s2) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean allSuccessorsInSet(final int state, final BitSet set)
	{
		for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
			if (!set.get(columns[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(final int state, final BitSet set)
	{
		for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
			if (set.get(columns[i])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int state=0; state<numStates; state++) {
			if (rows[state] == rows[state+1]) {
				if (fix) {
					throw new PrismException("Can't fix deadlocks in an DTMCSparse since it cannot be modified after construction");
				}
				deadlocks.add(state);
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int state=0; state < numStates; state++) {
			if (rows[state] == rows[state+1] && (except == null || !except.get(state)))
				throw new PrismException("DTMC has a deadlock in state " + state);
		}
	}



	//--- ModelExplicit ---

	@Override
	public void buildFromExplicitImport(ExplicitModelImporter modelImporter) throws PrismException
	{
		initialise(modelImporter.getNumStates());
		actionList.markNeedsRecomputing();
		int numTransitions = modelImporter.getNumTransitions();
		rows = new int[numStates + 1];
		columns = new int[numTransitions];
		probabilities = new double[numTransitions];
		actions = new Object[numTransitions];
		IOUtils.MCTransitionConsumer<Double> cons = new IOUtils.MCTransitionConsumer<>() {
			int sLast = -1;
			int count = 0;
			@Override
			public void accept(int s, int s2, Double d, Object a) throws PrismException
			{
				if (s < sLast) {
					throw new PrismException("Imported states/transitions must be in ascending order");
				}
				if (s != sLast) {
					rows[s] = count;
					sLast = s;
				}
				columns[count] = s2;
				probabilities[count] = d;
				actions[count] = a;
				count++;
			}
		};
		rows[numStates] = numTransitions;
		modelImporter.extractMCTransitions(cons);
	}



	//--- DTMC ---

	@Override
	public void forEachTransition(int state, TransitionConsumer<Double> consumer)
	{
		for (int col = rows[state], stop = rows[state+1]; col < stop; col++) {
			consumer.accept(state, columns[col], probabilities[col]);
		}
	}

	@Override
	public void forEachDoubleTransition(int state, DoubleTransitionConsumer consumer)
	{
		for (int col = rows[state], stop = rows[state+1]; col < stop; col++) {
			consumer.accept(state, columns[col], probabilities[col]);
		}
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		return new Iterator<Entry<Integer, Double>>()
		{
			final int start = rows[state];
			int col = start;
			final int end = rows[state + 1];

			@Override
			public boolean hasNext()
			{
				return col < end;
			}

			@Override
			public Entry<Integer, Double> next()
			{
				assert (col < end);
				final int index = col;
				col++;
				return new AbstractMap.SimpleImmutableEntry<>(columns[index], probabilities[index]);
			}
		};
	}

	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(int state)
	{
		return new Iterator<Entry<Integer, Pair<Double, Object>>>()
		{
			final int start = rows[state];
			int col = start;
			final int end = rows[state + 1];

			@Override
			public boolean hasNext()
			{
				return col < end;
			}

			@Override
			public Entry<Integer, Pair<Double, Object>> next()
			{
				assert (col < end);
				final int index = col;
				col++;
				Pair probAction = new Pair<>(probabilities[index], actions == null ? null : actions[index]);
				return new AbstractMap.SimpleImmutableEntry<>(columns[index], probAction);
			}
		};
	}

	@Override
	public Iterator<Object> getActionsIterator(int s)
	{
		return new Iterator<>()
		{
			final int start = rows[s];
			int col = start;
			final int end = rows[s + 1];

			@Override
			public boolean hasNext()
			{
				return col < end;
			}

			@Override
			public Object next()
			{
				return actions == null ? null : actions[col++];
			}
		};
	}

	@Override
	public boolean prob0step(final int s, final BitSet u)
	{
		boolean hasTransitionToU = false;
		for (int i=rows[s], stop=rows[s+1]; i < stop; i++) {
			final int successor = columns[i];
			if (u.get(successor)) {
				hasTransitionToU = true;
				break;
			}
		}
		return hasTransitionToU;
	}

	@Override
	public boolean prob1step(final int s, final BitSet u, final BitSet v)
	{
		boolean allTransitionsToU = true;
		boolean hasTransitionToV = false;
		for (int i=rows[s], stop=rows[s+1]; i < stop; i++) {
			final int successor = columns[i];
			if (!u.get(successor)) {
				// early abort, as overall result is false
				allTransitionsToU = false;
				break;
			}
			hasTransitionToV = hasTransitionToV || v.get(successor);
		}
		return (allTransitionsToU && hasTransitionToV);
	}

	@Override
	public double mvMultSingle(final int state, final double[] vect)
	{
		double d = 0.0;
		for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
			final int target = columns[i];
			final double probability = probabilities[i];
			d += probability * vect[target];
		}
		return d;
	}

	@Override
	public double mvMultJacSingle(final int state, final double[] vect)
	{
		double diag = 1.0;
		double d = 0.0;
		for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
			final int target = columns[i];
			final double probability = probabilities[i];
			if (target != state) {
				d += probability * vect[target];
			} else {
				diag -= probability;
			}
		}
		if (diag > 0) {
			d /= diag;
		}
		return d;
	}

	@Override
	public double mvMultRewSingle(final int state, final double[] vect, final MCRewards<Double> mcRewards)
	{
		double d = mcRewards.getStateReward(state);
		for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
			final int target = columns[i];
			final double probability = probabilities[i];
			//d += probability * (mcRewards.getTransitionReward(state, i-rows[state]) + vect[target]);
			d += probability * vect[target];
		}
		return d;
	}

	@Override
	public void vmMult(final double[] vect, final double[] result)
	{
		// Initialise result to 0
		Arrays.fill(result, 0);
		// Go through matrix elements (by row)
		for (int state = 0; state < numStates; state++) {
			for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
				int target = columns[i];
				double probability = probabilities[i];
				result[target] += probability * vect[state];
			}
		}
	}

	@Override
	public void vmMultPowerSteadyState(double vect[], double result[], double[] diagsQ, double deltaT, PrimitiveIterable.OfInt states)
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
		for (OfInt it = states.iterator(); it.hasNext(); ) {
			int state = it.nextInt();

			// ... handle all Q(state,t) entries of the generator matrix
			for (int i=rows[state], stop=rows[state+1]; i < stop; i++) {
				int target = columns[i];
				double prob = probabilities[i];
				if (state != target) {
					// ignore self loop, diagonal entries of the generator matrix handled above
					// update result vector entry for the *successor* state
					result[target] += deltaT * prob * vect[state];
				}
			}
		}
	}



	//--- Object ---

	@Override
	public String toString()
	{
		final Function<Integer, Entry<Integer, Distribution<Double>>> getDistribution = new Function<Integer, Entry<Integer, Distribution<Double>>>()
		{
			@Override
			public final Entry<Integer, Distribution<Double>> apply(final Integer state)
			{
				final Distribution<Double> distribution = new Distribution<>(getTransitionsIterator(state), getEvaluator());
				return new AbstractMap.SimpleImmutableEntry<>(state, distribution);
			}
		};
		String s = "trans: [ ";
		IterableStateSet states = new IterableStateSet(numStates);
		Iterator<Entry<Integer, Distribution<Double>>> distributions = states.iterator().map(getDistribution);
		while (distributions.hasNext()) {
			final Entry<Integer, Distribution<Double>> dist = distributions.next();
			s += dist.getKey() + ": " + dist.getValue();
			if (distributions.hasNext()) {
				s += ", ";
			}
		}
		return s + " ]";
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCSparse))
			return false;
		final DTMCSparse dtmc = (DTMCSparse) o;
		if (numStates != dtmc.numStates)
			return false;
		if (!initialStates.equals(dtmc.initialStates))
			return false;
		if (!Utils.doubleArraysAreEqual(probabilities, dtmc.probabilities))
			return false;
		if (!Utils.intArraysAreEqual(columns, dtmc.columns))
			return false;
		if (!Utils.intArraysAreEqual(rows, dtmc.rows))
			return false;
		// TODO: compare actions (complicated: null = null,null,null,...)
		return true;
	}
}
