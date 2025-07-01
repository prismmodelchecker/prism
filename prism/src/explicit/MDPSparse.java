//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
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
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import common.IterableStateSet;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import io.ExplicitModelImporter;
import io.IOUtils;
import parser.State;
import prism.ActionListOwner;
import prism.PrismException;
import prism.PrismUtils;

/**
 * Sparse matrix (non-mutable) explicit-state representation of an MDP.
 * This is much faster to access than e.g. MDPSimple and should also be more compact.
 * The catch is that you have to create the model all in one go and then can't modify it.
 */
public class MDPSparse extends MDPExplicit<Double>
{
	// Sparse matrix storing transition function (Steps)
	/** Probabilities for each transition (array of size numTransitions) */
	protected double nonZeros[];
	/** Column (destination) indices for each transition (array of size numTransitions) */
	protected int cols[];
	/** Indices into nonZeros/cols giving the start of the transitions for each choice (distribution);
	 * array is of size numDistrs+1 and last entry is always equal to numTransitions */
	protected int choiceStarts[];
	/** Indices into choiceStarts giving the start of the choices for each state;
	 * array is of size numStates+1 and last entry is always equal to numDistrs */
	protected int rowStarts[];

	// Action labels
	/** Array of action labels for choices;
	 * if null, there are no actions; otherwise, is an array of size numDistrs */
	protected Object actions[];

	// Other statistics
	protected int numDistrs;
	protected int numTransitions;
	protected int maxNumDistrs;

	// Constructors

	/**
	 * Constructor: Build new MDPSparse from arbitrary MDP type.
	 *
	 * @param mdp some MDP
	 */
	public MDPSparse(final MDP<Double> mdp)
	{
		this(mdp, false);
	}

	/**
	 * Constructor: Build new MDPSparse from arbitrary MDP type.
	 *
	 * @param mdp some MDP
	 * @param sort Whether or not to sort column indices
	 */
	public MDPSparse(final MDP<Double> mdp, boolean sort)
	{
		initialise(mdp.getNumStates());
		if (mdp instanceof ActionListOwner) {
			actionList.copyFrom(((ActionListOwner) mdp).getActionList());
		}
		setStatesList(mdp.getStatesList());
		setConstantValues(mdp.getConstantValues());
		for (String label : mdp.getLabels()) {
			addLabel(label, mdp.getLabelStates(label));
		}

		// Copy stats
		numDistrs = mdp.getNumChoices();
		numTransitions = mdp.getNumTransitions();
		maxNumDistrs = mdp.getMaxNumChoices();
		// Initialise transition function
		nonZeros = new double[numTransitions];
		cols = new int[numTransitions];
		choiceStarts = new int[numDistrs + 1];
		rowStarts = new int[numStates + 1];
		actions = hasActionLabels(mdp) ? new Object[numDistrs] : null;

		// Copy transition function
		final TreeMap<Integer, Double> sorted = sort ? new TreeMap<Integer, Double>() : null;
		int rowIndex = 0, choiceIndex = 0;
		for (int state = 0; state < numStates; state++) {
			if (mdp.isInitialState(state)) {
				addInitialState(state);
			}
			if (mdp.isDeadlockState(state)) {
				deadlocks.add(state);
			}

			rowStarts[state] = rowIndex;
			if (actions != null) {
				for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
					actions[rowIndex + choice] = mdp.getAction(state, choice);
				}
			}
			for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
				choiceStarts[rowIndex] = choiceIndex;
				for (Iterator<Entry<Integer, Double>> transitions = mdp.getTransitionsIterator(state, choice); transitions.hasNext();) {
					final Map.Entry<Integer, Double> trans = transitions.next();
					if (sort) {
						sorted.put(trans.getKey(), trans.getValue());
					} else {
						cols[choiceIndex] = trans.getKey();
						nonZeros[choiceIndex] = trans.getValue();
						choiceIndex++;
					}
				}
				if (sort) {
					for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
						cols[choiceIndex] = e.getKey();
						nonZeros[choiceIndex] = e.getValue();
						choiceIndex++;
					}
					sorted.clear();
				}
				rowIndex++;
			}
		}
		choiceStarts[numDistrs] = numTransitions;
		rowStarts[numStates] = numDistrs;
	}

	/** Helper: Does the given MDP have action labels on any of the choices? */
	private static boolean hasActionLabels(final MDP<?> mdp)
	{
		for (int state = 0, numStates = mdp.getNumStates(); state < numStates; state++) {
			for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
				if (mdp.getAction(state, choice) != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Copy constructor (from MDPSimple).
	 */
	public MDPSparse(MDPSimple<Double> mdp)
	{
		this(mdp, false);
	}

	/**
	 * Copy constructor (from MDPSimple). Optionally, transitions within choices
	 * are sorted (by ascending order of column index).
	 * @param mdp The MDP to copy
	 * @param sort Whether or not to sort column indices
	 */
	public MDPSparse(MDPSimple<Double> mdp, boolean sort)
	{
		int i, j, k;
		TreeMap<Integer, Double> sorted = null;
		initialise(mdp.getNumStates());
		copyFrom(mdp);
		// Copy stats
		numDistrs = mdp.getNumChoices();
		numTransitions = mdp.getNumTransitions();
		maxNumDistrs = mdp.getMaxNumChoices();
		// Copy transition function
		if (sort) {
			sorted = new TreeMap<Integer, Double>();
		}
		nonZeros = new double[numTransitions];
		cols = new int[numTransitions];
		choiceStarts = new int[numDistrs + 1];
		rowStarts = new int[numStates + 1];
		j = k = 0;
		for (i = 0; i < numStates; i++) {
			rowStarts[i] = j;
			for (Distribution<Double> distr : mdp.trans.get(i)) {
				choiceStarts[j] = k;
				for (Map.Entry<Integer, Double> e : distr) {
					if (sort) {
						sorted.put(e.getKey(), e.getValue());
					} else {
						cols[k] = e.getKey();
						nonZeros[k] = e.getValue();
						k++;
					}
				}
				if (sort) {
					for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
						cols[k] = e.getKey();
						nonZeros[k] = e.getValue();
						k++;
					}
					sorted.clear();
				}
				j++;
			}
		}
		choiceStarts[numDistrs] = numTransitions;
		rowStarts[numStates] = numDistrs;
		// Copy the actions too
		// Note: could pass 'mdp' or 'this' to convertToSparseStorage (use latter for consistency)
		actions = mdp.actions.convertToSparseStorage(this);
	}

	/**
	 * Copy constructor (from MDPSimple). Optionally, transitions within choices
	 * are sorted (by ascending order of column index). Also, optionally, a state
	 * index permutation can be provided, i.e. old state index i becomes index permut[i].
	 * Note: a states list, if present, will not be permuted and should be set
	 * separately afterwards if required.
	 * @param mdp The MDP to copy
	 * @param sort Whether or not to sort column indices
	 * @param permut State space permutation
	 */
	public MDPSparse(MDPSimple<Double> mdp, boolean sort, int permut[])
	{
		int i, j, k;
		TreeMap<Integer, Double> sorted = null;
		int permutInv[];
		initialise(mdp.getNumStates());
		copyFrom(mdp, permut);
		// Copy stats
		numDistrs = mdp.getNumChoices();
		numTransitions = mdp.getNumTransitions();
		maxNumDistrs = mdp.getMaxNumChoices();
		// Compute the inverse of the permutation
		permutInv = new int[numStates];
		for (i = 0; i < numStates; i++) {
			permutInv[permut[i]] = i;
		}
		// Copy transition function
		if (sort) {
			sorted = new TreeMap<Integer, Double>();
		}
		nonZeros = new double[numTransitions];
		cols = new int[numTransitions];
		choiceStarts = new int[numDistrs + 1];
		rowStarts = new int[numStates + 1];
		j = k = 0;
		for (i = 0; i < numStates; i++) {
			rowStarts[i] = j;
			for (Distribution<Double> distr : mdp.trans.get(permutInv[i])) {
				choiceStarts[j] = k;
				for (Map.Entry<Integer, Double> e : distr) {
					if (sort) {
						sorted.put(permut[e.getKey()], e.getValue());
					} else {
						cols[k] = (Integer) permut[e.getKey()];
						nonZeros[k] = (Double) e.getValue();
						k++;
					}
				}
				if (sort) {
					for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
						cols[k] = (Integer) e.getKey();
						nonZeros[k] = (Double) e.getValue();
						k++;
					}
					sorted.clear();
				}
				j++;
			}
		}
		choiceStarts[numDistrs] = numTransitions;
		rowStarts[numStates] = numDistrs;
		// Copy the actions too (after permuting)
		// Note: we pass _this_ new, permuted model to convertToSparseStorage
		actions = new ChoiceActionsSimple(mdp.actions, permut).convertToSparseStorage(this);
	}

	/**
	 * Copy constructor for a (sub-)MDP from a given MDP.
	 * The states and actions will be indexed as given by the order
	 * of the lists {@code states} and {@code actions}.
	 * @param mdp MDP to copy from
	 * @param states States to copy
	 * @param actions Actions to copy
	 */
	public MDPSparse(MDP<Double> mdp, List<Integer> states, List<List<Integer>> actions)
	{
		initialise(states.size());
		if (mdp instanceof ActionListOwner) {
			actionList.copyFrom(((ActionListOwner) mdp).getActionList());
		}
		for (int in : mdp.getInitialStates()) {
			addInitialState(in);
		}
		for (int dl : mdp.getDeadlockStates()) {
			addDeadlockState(dl);
		}
		statesList = new ArrayList<State>();
		for (int s : states) {
			statesList.add(mdp.getStatesList().get(s));
		}
		numDistrs = 0;
		numTransitions = 0;
		maxNumDistrs = 0;
		for (int i = 0; i < states.size(); i++) {
			int s = states.get(i);
			final int numChoices = actions.get(s).size();
			numDistrs += numChoices;
			if (numChoices > maxNumDistrs) {
				maxNumDistrs = numChoices;
			}
			for (int a : actions.get(s)) {
				numTransitions += mdp.getNumTransitions(s, a);
			}
		}
		nonZeros = new double[numTransitions];
		cols = new int[numTransitions];
		choiceStarts = new int[numDistrs + 1];
		rowStarts = new int[numStates + 1];
		this.actions = new Object[numDistrs];
		int choiceIndex = 0;
		int colIndex = 0;
		int[] reverseStates = new int[mdp.getNumStates()];
		for (int i = 0; i < states.size(); i++) {
			reverseStates[states.get(i)] = i;
		}
		for (int i = 0; i < states.size(); i++) {
			int s = states.get(i);
			rowStarts[i] = choiceIndex;
			for (int a : actions.get(s)) {
				choiceStarts[choiceIndex] = colIndex;
				this.actions[choiceIndex] = mdp.getAction(s, a);
				choiceIndex++;
				Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(s, a);
				while (it.hasNext()) {
					Entry<Integer, Double> next = it.next();
					cols[colIndex] = reverseStates[next.getKey()];
					nonZeros[colIndex] = next.getValue();
					colIndex++;
				}
			}
		}
		choiceStarts[numDistrs] = numTransitions;
		rowStarts[numStates] = numDistrs;
	}

	/**
	 * Construct an empty MDP (e.g. for subsequent explicit import)
	 */
	public MDPSparse()
	{
	}

	// Mutators (other)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrs = numTransitions = maxNumDistrs = 0;
		actions = null;
	}

	@Override
	public void buildFromExplicitImport(ExplicitModelImporter modelImporter) throws PrismException
	{
		initialise(modelImporter.getNumStates());
		numDistrs = modelImporter.getNumChoices();
		numTransitions = modelImporter.getNumTransitions();
		rowStarts = new int[numStates + 1];
		choiceStarts = new int[numDistrs + 1];
		cols = new int[numTransitions];
		nonZeros = new double[numTransitions];
		actions = new Object[numDistrs];
		IOUtils.MDPTransitionConsumer<Double> cons = new IOUtils.MDPTransitionConsumer<>() {
			int sLast = -1;
			int iLast = -1;
			int count = 0;
			int countCh = 0;

			@Override
			public void accept(int s, int i, int s2, Double d, Object a) throws PrismException
			{
				if (s < sLast) {
					throw new PrismException("Imported states/transitions must be in ascending order");
				}
				if (s != sLast) {
					rowStarts[s] = countCh;
					sLast = s;
					iLast = -1;
				}
				if (i < iLast) {
					throw new PrismException("Imported states/transitions must be in ascending order");
				}
				if (i != iLast) {
					choiceStarts[countCh] = count;
					actions[countCh] = a;
					countCh++;
					iLast = i;
				}
				cols[count] = s2;
				nonZeros[count] = d;
				count++;
			}
		};
		rowStarts[numStates] = numDistrs;
		choiceStarts[numDistrs] = numTransitions;
		modelImporter.extractMDPTransitions(cons);
		actionList.markNeedsRecomputing();
		// Compute maxNumDistrs
		maxNumDistrs = 0;
		for (int s = 0; s < numStates; s++) {
			maxNumDistrs = Math.max(maxNumDistrs, getNumChoices(s));
		}
	}

	// Accessors (for Model)

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
		return numTransitions;
	}

	@Override
	public int getNumTransitions(int s)
	{
		return choiceStarts[rowStarts[s + 1]] - choiceStarts[rowStarts[s]];
	}

	private SuccessorsIterator colsIterator(int start, int end, boolean distinct)
	{
		return new SuccessorsIterator() {
			int cur = start;

			@Override
			public boolean successorsAreDistinct()
			{
				return distinct;
			}

			@Override
			public boolean hasNext()
			{
				return cur < end;
			}

			@Override
			public int nextInt()
			{
				return cols[cur++];
			}
		};
	}

	@Override
	public SuccessorsIterator getSuccessors(final int s)
	{
		// Assumes that only non-zero entries are stored
		int start = choiceStarts[rowStarts[s]];
		int end = choiceStarts[rowStarts[s + 1]];
		// we can guarantee that the successors are distinct if there is at most one successor...
		boolean distinct = (start == end || start + 1 == end);
		return colsIterator(start, end, distinct);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			// Note that no distributions is a deadlock, not an empty distribution
			if (getNumChoices(i) == 0) {
				addDeadlockState(i);
				if (fix) {
					throw new PrismException("Can't fix deadlocks in an MDPSparse since it cannot be modified after construction");
				}
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (getNumChoices(i) == 0 && (except == null || !except.get(i)))
				throw new PrismException("MDP has a deadlock in state " + i);
		}
	}

	// Accessors (for NondetModel)

	@Override
	public int getNumChoices(int s)
	{
		return rowStarts[s + 1] - rowStarts[s];
	}

	@Override
	public int getMaxNumChoices()
	{
		return maxNumDistrs;
	}

	@Override
	public int getNumChoices()
	{
		return numDistrs;
	}

	@Override
	public Object getAction(int s, int i)
	{
		return i < 0 || actions == null ? null : actions[rowStarts[s] + i];
	}

	@Override
	public SuccessorsIterator getSuccessors(final int s, final int i)
	{
		int start = choiceStarts[rowStarts[s] + i];
		int end = choiceStarts[rowStarts[s] + i + 1];
		// we assume here that the successors for a single choice are distinct
		return colsIterator(start, end, true);
	}

	// Accessors (for MDP)

	@Override
	public int getNumTransitions(int s, int i)
	{
		return choiceStarts[rowStarts[s] + i + 1] - choiceStarts[rowStarts[s] + i];
	}

	@Override
	public void forEachTransition(int s, int i, TransitionConsumer<Double> c)
	{
		for (int col = choiceStarts[rowStarts[s] + i], stop = choiceStarts[rowStarts[s] + i + 1]; col < stop; col++) {
			c.accept(s, cols[col], nonZeros[col]);
		}
	}

	@Override
	public void forEachDoubleTransition(int s, int i, DoubleTransitionConsumer c)
	{
		for (int col = choiceStarts[rowStarts[s] + i], stop = choiceStarts[rowStarts[s] + i + 1]; col < stop; col++) {
			c.accept(s, cols[col], nonZeros[col]);
		}
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int s, final int i)
	{
		return new Iterator<Entry<Integer, Double>>()
		{
			final int start = choiceStarts[rowStarts[s] + i];
			int col = start;
			final int end = choiceStarts[rowStarts[s] + i + 1];

			@Override
			public boolean hasNext()
			{
				return col < end;
			}

			@Override
			public Entry<Integer, Double> next()
			{
				assert (col < end);
				final int i = col;
				col++;
				return new AbstractMap.SimpleImmutableEntry<Integer, Double>(cols[i], nonZeros[i]);
			}
		};
	}

	@Override
	public void prob0step(BitSet subset, BitSet u, boolean forall, BitSet result)
	{
		int j, k, l1, h1, l2, h2;
		boolean b1, some;
		for (int i : new IterableStateSet(subset, numStates)) {
			b1 = forall; // there exists or for all
			l1 = rowStarts[i];
			h1 = rowStarts[i + 1];
			for (j = l1; j < h1; j++) {
				some = false;
				l2 = choiceStarts[j];
				h2 = choiceStarts[j + 1];
				for (k = l2; k < h2; k++) {
					// Assume that only non-zero entries are stored
					if (u.get(cols[k])) {
						some = true;
						break;
					}
				}
				if (forall) {
					if (!some) {
						b1 = false;
						break;
					}
				} else {
					if (some) {
						b1 = true;
						break;
					}
				}
			}
			result.set(i, b1);
		}
	}

	@Override
	public void prob1Astep(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		int j, k, l1, h1, l2, h2;
		boolean b1, some, all;
		for (int i : new IterableStateSet(subset, numStates)) {
			b1 = true;
			l1 = rowStarts[i];
			h1 = rowStarts[i + 1];
			for (j = l1; j < h1; j++) {
				some = false;
				all = true;
				l2 = choiceStarts[j];
				h2 = choiceStarts[j + 1];
				for (k = l2; k < h2; k++) {
					// Assume that only non-zero entries are stored
					if (!u.get(cols[k])) {
						all = false;
						break; // Stop early (already know b1 will be set to false)
					}
					if (v.get(cols[k])) {
						some = true;
					}
				}
				if (!(some && all)) {
					b1 = false;
					break;
				}
			}
			result.set(i, b1);
		}
	}

	@Override
	public void prob1Estep(BitSet subset, BitSet u, BitSet v, BitSet result, int strat[])
	{
		int j, k, l1, h1, l2, h2, stratCh = -1;
		boolean b1, some, all;
		for (int i : new IterableStateSet(subset, numStates)) {
			b1 = false;
			l1 = rowStarts[i];
			h1 = rowStarts[i + 1];
			for (j = l1; j < h1; j++) {
				some = false;
				all = true;
				l2 = choiceStarts[j];
				h2 = choiceStarts[j + 1];
				for (k = l2; k < h2; k++) {
					// Assume that only non-zero entries are stored
					if (!u.get(cols[k])) {
						all = false;
						break; // Stop early (already know b1 will not be set to true)
					}
					if (v.get(cols[k])) {
						some = true;
					}
				}
				if (some && all) {
					b1 = true;
					// If strategy generation is enabled, remember optimal choice
					if (strat != null)
						stratCh = j - l1;
					break;
				}
			}
			// If strategy generation is enabled, store optimal choice
			// (only if this the first time we add the state to S^yes)
			if (strat != null & b1 & !result.get(i)) {
				strat[i] = stratCh;
			}
			// Store result
			result.set(i, b1);
		}
	}

	@Override
	public void prob1step(BitSet subset, BitSet u, BitSet v, boolean forall, BitSet result)
	{
		int j, k, l1, h1, l2, h2;
		boolean b1, some, all;
		for (int i : new IterableStateSet(subset, numStates)) {
			b1 = forall; // there exists or for all
			l1 = rowStarts[i];
			h1 = rowStarts[i + 1];
			for (j = l1; j < h1; j++) {
				some = false;
				all = true;
				l2 = choiceStarts[j];
				h2 = choiceStarts[j + 1];
				for (k = l2; k < h2; k++) {
					// Assume that only non-zero entries are stored
					if (v.get(cols[k])) {
						some = true;
					}
					if (!u.get(cols[k])) {
						all = false;
					}
				}
				if (forall) {
					if (!(some && all)) {
						b1 = false;
						break;
					}
				} else {
					if (some && all) {
						b1 = true;
						break;
					}
				}
			}
			result.set(i, b1);
		}
	}

	@Override
	public boolean prob1stepSingle(int s, int i, BitSet u, BitSet v)
	{
		int j, k, l2, h2;
		boolean some, all;

		j = rowStarts[s] + i;
		some = false;
		all = true;
		l2 = choiceStarts[j];
		h2 = choiceStarts[j + 1];
		for (k = l2; k < h2; k++) {
			// Assume that only non-zero entries are stored
			if (v.get(cols[k])) {
				some = true;
			}
			if (!u.get(cols[k])) {
				all = false;
			}
		}
		return some && all;
	}

	@Override
	public double mvMultMinMaxSingle(int s, double vect[], boolean min, int strat[])
	{
		int j, k, l1, h1, l2, h2, stratCh = -1;
		double d, minmax;
		boolean first;

		minmax = 0;
		first = true;
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			// Compute sum for this distribution
			d = 0.0;
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				d += nonZeros[k] * vect[cols[k]];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = j - l1;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null & !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min, double val)
	{
		int j, k, l1, h1, l2, h2;
		double d;
		List<Integer> res;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			// Compute sum for this distribution
			d = 0.0;
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				d += nonZeros[k] * vect[cols[k]];
			}
			// Store strategy info if value matches
			if (PrismUtils.doublesAreEqual(val, d)) {
				res.add(j - l1);
			}
		}

		return res;
	}

	@Override
	public double mvMultSingle(int s, int i, double vect[])
	{
		int j, k, l2, h2;
		double d;

		j = rowStarts[s] + i;
		// Compute sum for this distribution
		d = 0.0;
		l2 = choiceStarts[j];
		h2 = choiceStarts[j + 1];
		for (k = l2; k < h2; k++) {
			d += nonZeros[k] * vect[cols[k]];
		}

		return d;
	}

	@Override
	public double mvMultJacMinMaxSingle(int s, double vect[], boolean min, int strat[])
	{
		int j, k, l1, h1, l2, h2, stratCh = -1;
		double diag, d, minmax;
		boolean first;

		minmax = 0;
		first = true;
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			diag = 1.0;
			// Compute sum for this distribution
			d = 0.0;
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				if (cols[k] != s) {
					d += nonZeros[k] * vect[cols[k]];
				} else {
					diag -= nonZeros[k];
				}
			}
			if (diag > 0)
				d /= diag;
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = j - l1;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null & !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public double mvMultJacSingle(int s, int i, double vect[])
	{
		int j, k, l2, h2;
		double diag, d;

		j = rowStarts[s] + i;
		diag = 1.0;
		// Compute sum for this distribution
		d = 0.0;
		l2 = choiceStarts[j];
		h2 = choiceStarts[j + 1];
		for (k = l2; k < h2; k++) {
			if (cols[k] != s) {
				d += nonZeros[k] * vect[cols[k]];
			} else {
				diag -= nonZeros[k];
			}
		}
		if (diag > 0)
			d /= diag;

		return d;
	}

	@Override
	public double mvMultRewMinMaxSingle(int s, double vect[], MDPRewards<Double> mdpRewards, boolean min, int strat[])
	{
		int j, k, l1, h1, l2, h2, stratCh = -1;
		double d, minmax;
		boolean first;

		minmax = 0;
		first = true;
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			// Compute sum for this distribution
			d = mdpRewards.getTransitionReward(s, j - l1);
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				d += nonZeros[k] * vect[cols[k]];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = j - l1;
			}
			first = false;
		}
		// Add state reward (doesn't affect min/max)
		minmax += mdpRewards.getStateReward(s);
		// If strategy generation is enabled, store optimal choice
		if (strat != null & !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public double mvMultRewSingle(int s, int i, double[] vect, MCRewards<Double> mcRewards)
	{
		int j, k, l2, h2;
		double d;

		j = rowStarts[s] + i;
		// Compute sum for this distribution
		// TODO: use transition rewards when added to DTMCss
		// d = mcRewards.getTransitionReward(s);
		d = 0;
		l2 = choiceStarts[j];
		h2 = choiceStarts[j + 1];
		for (k = l2; k < h2; k++) {
			d += nonZeros[k] * vect[cols[k]];
		}
		d += mcRewards.getStateReward(s);
		return d;
	}
	
	@Override
	public double mvMultRewJacMinMaxSingle(int s, double vect[], MDPRewards<Double> mdpRewards, boolean min, int strat[])
	{
		int j, k, l1, h1, l2, h2, stratCh = -1;
		double diag, d, minmax;
		boolean first;

		minmax = 0;
		first = true;
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			diag = 1.0;
			boolean onlySelfloops = true;
			// Compute sum for this distribution
			// (note: have to add state rewards in the loop for Jacobi)
			d = mdpRewards.getStateReward(s);
			d += mdpRewards.getTransitionReward(s, j - l1);
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				if (cols[k] != s) {
					onlySelfloops = false;
					d += nonZeros[k] * vect[cols[k]];
				} else {
					diag -= nonZeros[k];
				}
			}
			// Catch special case of probability 1 self-loop (Jacobi does it wrong)
			if (onlySelfloops) {
				if (d != 0) {
					// always choosing the selfloop-action will produce infinite reward
					d = (d>0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
				} else {
					// no reward & only self-loops: d remains 0
					d = 0;
				}
			} else if (diag > 0) {
				// not only self-loops, do Jacobi division
				d /= diag;
			}

			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = j - l1;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null & !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], MDPRewards<Double> mdpRewards, boolean min, double val)
	{
		int j, k, l1, h1, l2, h2;
		double d;
		List<Integer> res;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			// Compute sum for this distribution
			d = mdpRewards.getTransitionReward(s, j - l1);
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				d += nonZeros[k] * vect[cols[k]];
			}
			d += mdpRewards.getStateReward(s);
			// Store strategy info if value matches
			if (PrismUtils.doublesAreEqual(val, d)) {
				res.add(j - l1);
			}
		}

		return res;
	}

	@Override
	public void mvMultRight(int[] states, int[] strat, double[] source, double[] dest)
	{
		for (int s : states) {
			int j, l2, h2;
			int k = strat[s];
			j = rowStarts[s] + k;
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				dest[cols[k]] += nonZeros[k] * source[s];
			}
		}
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i, j, k, l1, h1, l2, h2;
		Object o;
		String s = "";
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (i > 0)
				s += ", ";
			s += i + ": [";
			l1 = rowStarts[i];
			h1 = rowStarts[i + 1];
			for (j = l1; j < h1; j++) {
				if (j > l1)
					s += ",";
				o = getAction(i, j - l1);
				if (o != null)
					s += o + ":";
				s += "{";
				l2 = choiceStarts[j];
				h2 = choiceStarts[j + 1];
				for (k = l2; k < h2; k++) {
					if (k > l2)
						s += ", ";
					s += cols[k] + ":" + nonZeros[k];
				}
				s += "}";
			}
			s += "]";
		}
		s += " ]";

		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof MDPSparse))
			return false;
		MDPSparse mdp = (MDPSparse) o;
		if (numStates != mdp.numStates)
			return false;
		if (!initialStates.equals(mdp.initialStates))
			return false;
		if (!Utils.doubleArraysAreEqual(nonZeros, mdp.nonZeros))
			return false;
		if (!Utils.intArraysAreEqual(cols, mdp.cols))
			return false;
		if (!Utils.intArraysAreEqual(choiceStarts, mdp.choiceStarts))
			return false;
		if (!Utils.intArraysAreEqual(rowStarts, mdp.rowStarts))
			return false;
		// TODO: compare actions (complicated: null = null,null,null,...)
		return true;
	}
}
