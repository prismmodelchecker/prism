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

import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import explicit.rewards.MDPRewards;
import parser.State;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;

/**
 * Sparse matrix (non-mutable) explicit-state representation of an MDP.
 * This is much faster to access than e.g. MDPSimple and should also be more compact.
 * The catch is that you have to create the model all in one go and then can't modify it.
 */
public class MDPSparse extends ModelSparse implements MDP
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
	 * Copy constructor (from MDPSimple).
	 */
	public MDPSparse(MDPSimple mdp)
	{
		this(mdp, false);
	}

	/**
	 * Copy constructor (from MDPSimple). Optionally, transitions within choices
	 * are sorted (by ascending order of column index).
	 * @param mdp The MDP to copy
	 * @param sort Whether or not to sort column indices
	 */
	public MDPSparse(MDPSimple mdp, boolean sort)
	{
		int i, j, k;
		TreeMap<Integer, Double> sorted = null;
		initialise(mdp.getNumStates());
		for (int in : mdp.getInitialStates()) {
			addInitialState(in);
		}
		statesList = mdp.getStatesList();
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
		actions = new Object[numDistrs];
		j = k = 0;
		for (i = 0; i < numStates; i++) {
			rowStarts[i] = j;
			for (int l = 0; l < mdp.actions.get(i).size(); l++) {
				actions[j + l] = mdp.actions.get(i).get(l);
			}
			for (Distribution distr : mdp.trans.get(i)) {
				choiceStarts[j] = k;
				for (Map.Entry<Integer, Double> e : distr) {
					if (sort) {
						sorted.put(e.getKey(), e.getValue());
					} else {
						cols[k] = (Integer) e.getKey();
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
	public MDPSparse(MDPSimple mdp, boolean sort, int permut[])
	{
		int i, j, k;
		TreeMap<Integer, Double> sorted = null;
		int permutInv[];
		initialise(mdp.getNumStates());
		for (int in : mdp.getInitialStates()) {
			addInitialState(permut[in]);
		}
		// Don't copy states list (it will be wrong)
		statesList = null;
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
		actions = new Object[numDistrs];
		j = k = 0;
		for (i = 0; i < numStates; i++) {
			rowStarts[i] = j;
			for (int l = 0; l < mdp.actions.get(permutInv[i]).size(); l++) {
				actions[j + l] = mdp.actions.get(permutInv[i]).get(l);
			}
			for (Distribution distr : mdp.trans.get(permutInv[i])) {
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
	}

	/**
	 * Copy constructor for a (sub-)MDP from a given MDP.
	 * The states and actions will be indexed as given by the order
	 * of the lists {@code states} and {@code actions}.
	 * @param mdp MDP to copy from
	 * @param states States to copy
	 * @param actions Actions to copy
	 */
	public MDPSparse(MDP mdp, List<Integer> states, List<List<Integer>> actions)
	{
		initialise(states.size());
		for (int in : mdp.getInitialStates()) {
			addInitialState(in);
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

	// Mutators (for ModelSparse)

	@Override
	protected void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrs = numTransitions = maxNumDistrs = 0;
		actions = null;
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, k, iLast, kLast, jCount, kCount, n, lineNum = 0;
		double prob;

		try {
			// Open file
			in = new BufferedReader(new FileReader(new File(filename)));
			// Parse first line to get num states
			s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("Missing first line of .tra file");
			ss = s.split(" ");
			n = Integer.parseInt(ss[0]);
			// Initialise
			initialise(n);
			// Set initial state (assume 0)
			initialStates.add(0);
			// Store stats
			numDistrs = Integer.parseInt(ss[1]);
			numTransitions = Integer.parseInt(ss[2]);
			// Go though list of transitions in file
			iLast = -1;
			kLast = -1;
			jCount = kCount = 0;
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					ss = s.split(" ");
					i = Integer.parseInt(ss[0]);
					k = Integer.parseInt(ss[1]);
					j = Integer.parseInt(ss[2]);
					prob = Double.parseDouble(ss[3]);
					// For a new state
					if (i != iLast) {
						rowStarts[i] = kCount;
					}
					// For a new state or distribution
					if (i != iLast || k != kLast) {
						choiceStarts[kCount] = jCount;
						kCount++;
					}
					// Store transition
					cols[jCount] = j;
					nonZeros[jCount] = prob;
					// Prepare for next iter
					iLast = i;
					kLast = k;
					jCount++;
				}
				s = in.readLine();
				lineNum++;
			}
			choiceStarts[numDistrs] = numTransitions;
			rowStarts[numStates] = numDistrs;
			// Compute maxNumDistrs
			maxNumDistrs = 0;
			for (i = 0; i < numStates; i++) {
				maxNumDistrs = Math.max(maxNumDistrs, getNumChoices(i));
			}
			// Close file
			in.close();
			// Sanity checks
			if (kCount != numDistrs) {
				throw new PrismException("Choice count is wrong in tra file (" + kCount + "!=" + numTransitions + ")");
			}
			if (jCount != numTransitions) {
				throw new PrismException("Transition count is wrong in tra file (" + kCount + "!=" + numTransitions + ")");
			}
		} catch (IOException e) {
			System.exit(1);
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
	}

	// Accessors (for ModelSparse)

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		int j, k, l1, h1, l2, h2;
		l1 = rowStarts[s1];
		h1 = rowStarts[s1 + 1];
		for (j = l1; j < h1; j++) {
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				// Assume that only non-zero entries are stored
				if (cols[k] == s2) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		int j, k, l1, h1, l2, h2;
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				// Assume that only non-zero entries are stored
				if (!set.get(cols[k])) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		int j, k, l1, h1, l2, h2;
		l1 = rowStarts[s];
		h1 = rowStarts[s + 1];
		for (j = l1; j < h1; j++) {
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				// Assume that only non-zero entries are stored
				if (set.get(cols[k])) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int getNumChoices(int s)
	{
		return rowStarts[s + 1] - rowStarts[s];
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (getNumChoices(i) == 0 && (except == null || !except.get(i)))
				throw new PrismException("MDP has a deadlock in state " + i);
		}
	}

	@Override
	public BitSet findDeadlocks(boolean fix) throws PrismException
	{
		int i;
		BitSet deadlocks = new BitSet();
		for (i = 0; i < numStates; i++) {
			// Note that no distributions is a deadlock, not an empty distribution
			if (getNumChoices(i) == 0)
				deadlocks.set(i);
		}
		if (fix) {
			// TODO disallow this call?
			throw new RuntimeException("Can't modify this model");
		}
		return deadlocks;
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out) throws PrismException
	{
		// Note: In PRISM .tra files, transitions are usually sorted by column index within choices
		// (to ensure this is the case, you may need to sort the model when constructing)
		int i, j, k, l1, h1, l2, h2;
		Object action;
		// Output transitions to .tra file
		out.print(numStates + " " + numDistrs + " " + numTransitions + "\n");
		for (i = 0; i < numStates; i++) {
			l1 = rowStarts[i];
			h1 = rowStarts[i + 1];
			for (j = l1; j < h1; j++) {
				l2 = choiceStarts[j];
				h2 = choiceStarts[j + 1];
				for (k = l2; k < h2; k++) {
					out.print(i + " " + (j - l1) + " " + cols[k] + " " + PrismUtils.formatDouble(nonZeros[k]));
					action = getAction(i, j - l1);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
			}
		}
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		int i, j, k, l1, h1, l2, h2;
		String nij;
		Object action;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				l1 = rowStarts[i];
				h1 = rowStarts[i + 1];
				for (j = l1; j < h1; j++) {
					action = getAction(i, j - l1);
					nij = "n" + i + "_" + (j - l1);
					out.write(i + " -> " + nij + " [ arrowhead=none,label=\"" + j);
					if (action != null)
						out.write(":" + action);
					out.write("\" ];\n");
					out.write(nij + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");
					l2 = choiceStarts[j];
					h2 = choiceStarts[j + 1];
					for (k = l2; k < h2; k++) {
						out.write(nij + " -> " + cols[k] + " [ label=\"" + nonZeros[k] + "\" ];\n");
					}
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		int i, j, k, l1, h1, l2, h2;
		FileWriter out;
		Object action;
		try {
			// Output transitions to PRISM language file
			out = new FileWriter(filename);
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (numStates - 1) + "];\n");
			for (i = 0; i < numStates; i++) {
				l1 = rowStarts[i];
				h1 = rowStarts[i + 1];
				for (j = l1; j < h1; j++) {
					// Print out transitions
					action = getAction(i, j - l1);
					out.write(action != null ? ("[" + action + "]") : "[]");
					out.write("x=" + i + "->");
					l2 = choiceStarts[j];
					h2 = choiceStarts[j + 1];
					for (k = l2; k < h2; k++) {
						if (k > l2)
							out.write("+");
						// Note use of PrismUtils.formatDouble to match PRISM-exported files
						out.write(PrismUtils.formatDouble(nonZeros[k]) + ":(x'=" + cols[k] + ")");
					}
					out.write(";\n");
				}
			}
			out.write("endmodule\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += numStates + " states (" + getNumInitialStates() + " initial)";
		s += ", " + numTransitions + " transitions";
		s += ", " + numDistrs + " choices";
		s += ", dist max/avg = " + maxNumDistrs + "/" + PrismUtils.formatDouble2dp(((double) numDistrs) / numStates);
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + numTransitions + "\n";
		s += "Choices:     " + numDistrs + "\n";
		s += "Max/avg:     " + maxNumDistrs + "/" + PrismUtils.formatDouble2dp(((double) numDistrs) / numStates) + "\n";
		return s;
	}

	// Accessors (for MDP)

	@Override
	public Object getAction(int s, int i)
	{
		return actions == null ? null : actions[rowStarts[s] + i];
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		return choiceStarts[rowStarts[s] + i + 1] - choiceStarts[rowStarts[s] + i];
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
				return new Entry<Integer, Double>()
				{
					int key = cols[i];
					double value = nonZeros[i];

					@Override
					public Integer getKey()
					{
						return key;
					}

					@Override
					public Double getValue()
					{
						return value;
					}

					@Override
					public Double setValue(Double arg0)
					{
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public void prob0step(BitSet subset, BitSet u, boolean forall, BitSet result)
	{
		int i, j, k, l1, h1, l2, h2;
		boolean b1, some;
		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
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
							continue;
						}
					}
					if (forall) {
						if (!some) {
							b1 = false;
							continue;
						}
					} else {
						if (some) {
							b1 = true;
							continue;
						}
					}
				}
				result.set(i, b1);
			}
		}
	}

	@Override
	public void prob1step(BitSet subset, BitSet u, BitSet v, boolean forall, BitSet result)
	{
		int i, j, k, l1, h1, l2, h2;
		boolean b1, some, all;
		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
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
							continue;
						}
					} else {
						if (some && all) {
							b1 = true;
							continue;
						}
					}
				}
				result.set(i, b1);
			}
		}
	}

	@Override
	public void mvMultMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement, int adv[])
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
		}
	}

	@Override
	public double mvMultMinMaxSingle(int s, double vect[], boolean min, int adv[])
	{
		int j, k, l1, h1, l2, h2;
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
				// If adversary generation is enabled, remember optimal choice
				if (adv != null) {
					// Only remember strictly better choices
					// (required if either player is doing max)
					if (adv[s] == -1 || (min && minmax < vect[s]) || (!min && minmax > vect[s])) {
						adv[s] = j;
					}
				}
			}
			first = false;
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
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				res.add(j - l1);
			}
		}

		return res;
	}

	@Override
	public double mvMultSingle(int s, int k, double vect[])
	{
		int j, l2, h2;
		double d;

		j = rowStarts[s] + k;
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
	public double mvMultGSMinMax(double vect[], boolean min, BitSet subset, boolean complement, boolean absolute)
	{
		int s;
		double d, diff, maxDiff = 0.0;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				d = mvMultJacMinMaxSingle(s, vect, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				d = mvMultJacMinMaxSingle(s, vect, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				d = mvMultJacMinMaxSingle(s, vect, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
			// Use this code instead for backwards Gauss-Seidel
			/*for (s = numStates - 1; s >= 0; s--) {
				if (subset.get(s)) {
					d = mvMultJacMinMaxSingle(s, vect, min);
					diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
					maxDiff = diff > maxDiff ? diff : maxDiff;
					vect[s] = d;
				}
			}*/
		}
		return maxDiff;
	}

	@Override
	public double mvMultJacMinMaxSingle(int s, double vect[], boolean min)
	{
		int j, k, l1, h1, l2, h2;
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
			if (first || (min && d < minmax) || (!min && d > minmax))
				minmax = d;
			first = false;
		}

		return minmax;
	}

	@Override
	public double mvMultJacSingle(int s, int k, double vect[])
	{
		int j, l2, h2;
		double diag, d;

		j = rowStarts[s] + k;
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
	public void mvMultRewMinMax(double vect[], MDPRewards mdpRewards, boolean min, double result[], BitSet subset, boolean complement, int adv[])
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv);
		}
	}

	@Override
	public double mvMultRewMinMaxSingle(int s, double vect[], MDPRewards mdpRewards, boolean min, int adv[])
	{
		int j, k, l1, h1, l2, h2;
		double d, minmax;
		boolean first;

		// TODO: implement adv. gen.

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
			if (first || (min && d < minmax) || (!min && d > minmax))
				minmax = d;
			first = false;
		}
		minmax += mdpRewards.getStateReward(s);

		return minmax;
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], MDPRewards mdpRewards, boolean min, double val)
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
			d = mdpRewards.getTransitionReward(s, j);
			l2 = choiceStarts[j];
			h2 = choiceStarts[j + 1];
			for (k = l2; k < h2; k++) {
				d += nonZeros[k] * vect[cols[k]];
			}
			// Store strategy info if value matches
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				res.add(j - l1);
			}
		}

		return res;
	}

	@Override
	public void mvMultRight(int[] states, int[] adv, double[] source, double[] dest)
	{
		for (int s : states) {
			int j, l2, h2;
			int k = adv[s];
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
