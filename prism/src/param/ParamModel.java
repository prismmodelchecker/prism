//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import explicit.MDPGeneric;
import explicit.Model;
import explicit.ModelExplicit;
import explicit.SuccessorsIterator;
import explicit.graphviz.Decorator;
import parser.Values;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

/**
 * Represents a parametric Markov model.
 * This class is used to store all types of parametric model, both models
 * with and without nondeterminism, discrete- as well as continuous-time.
 * This turned out the be the most convenient way to implement model checking
 * for parametric models.
 */
public final class ParamModel extends ModelExplicit implements MDPGeneric<Function>
{
	/** total number of nondeterministic choices over all states */
	private int numTotalChoices;
	/** total number of probabilistic transitions over all states */
	private int numTotalTransitions;
	/** the maximal number of choices per state, over all states */
	private int numMaxChoices;
	/** begin and end of state transitions */
	private int[] rows;
	/** begin and end of a distribution in a nondeterministic choice */
	private int[] choices;
	/** targets of distribution branches */
	private int[] cols;
	/** probabilities of distribution branches */
	private Function[] nonZeros;
	/** labels - per transition, <i>not</i> per action */
	private String[] labels;
	/** total sum of leaving rates for a given nondeterministic choice */
	private Function[] sumRates;
	/** model type */
	private ModelType modelType;
	/** function factory which manages functions used on transitions, etc. */
	private FunctionFactory functionFactory;

	/**
	 * Constructs a new parametric model.
	 */
	ParamModel()
	{
		numStates = 0;
		numTotalChoices = 0;
		numTotalTransitions = 0;
		initialStates = new LinkedList<Integer>();
		deadlocks = new TreeSet<Integer>();
	}

	/**
	 * Sets the type of the model.
	 * 
	 * @param modelType type the model shall have
	 */
	void setModelType(ModelType modelType)
	{
		this.modelType = modelType;
	}

	// Accessors (for Model)

	@Override
	public ModelType getModelType()
	{
		return modelType;
	}

	@Override
	public Values getConstantValues()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumTransitions()
	{
		return numTotalTransitions;
	}

	@Override
	public SuccessorsIterator getSuccessors(final int s)
	{
		return SuccessorsIterator.chain(new Iterator<SuccessorsIterator>() {
			private int choice = 0;
			private int choices = getNumChoices(s);

			@Override
			public boolean hasNext()
			{
				return choice < choices;
			}

			@Override
			public SuccessorsIterator next()
			{
				return getSuccessors(s, choice++);
			}
		});
	}

	/**
	 * Get a SuccessorsIterator for state s and choice i.
	 * @param s The state
	 * @param i Choice index
	 */
	public SuccessorsIterator getSuccessors(int s, int i)
	{
		return new SuccessorsIterator()
		{
			final int start = choiceBegin(stateBegin(s) + i);
			int col = start;
			final int end = choiceBegin(stateBegin(s) + i + 1);

			@Override
			public boolean hasNext()
			{
				return col < end;
			}

			@Override
			public int nextInt()
			{
				assert (col < end);
				int i = col;
				col++;
				return cols[i];
			}

			@Override
			public boolean successorsAreDistinct()
			{
				return false;
			}
		};
	}
	
	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		for (int choice = stateBegin(s1); choice < stateEnd(s1); choice++) {
			for (int succ = choiceBegin(choice); succ < choiceEnd(choice); succ++) {
				if (succState(succ) == s2) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get an iterator over the transitions from choice {@code i} of state {@code s}.
	 * <br>
	 * For DTMC/CTMCs, there is only a single choice.
	 */
	public Iterator<Entry<Integer, Function>> getTransitionsIterator(final int s, final int i)
	{
		return new Iterator<Entry<Integer, Function>>()
		{
			final int start = choiceBegin(stateBegin(s) + i);
			int col = start;
			final int end = choiceBegin(stateBegin(s) + i + 1);

			@Override
			public boolean hasNext()
			{
				return col < end;
			}

			@Override
			public Entry<Integer, Function> next()
			{
				assert (col < end);
				final int i = col;
				col++;
				return new Entry<Integer, Function>()
				{
					int key = cols[i];
					Function value = nonZeros[i];

					@Override
					public Integer getKey()
					{
						return key;
					}

					@Override
					public Function getValue()
					{
						return value;
					}

					@Override
					public Function setValue(Function arg0)
					{
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out)
	{
		int i, j, numChoices;
		Object action;
		TreeMap<Integer, Function> sorted;
		// Output transitions to .tra file
		if (modelType.nondeterministic()) {
			out.print(numStates + " " + getNumChoices() + " " + getNumTransitions() + "\n");
		} else {
			out.print(numStates + " " + getNumTransitions() + "\n");
		}
		sorted = new TreeMap<Integer, Function>();
		for (i = 0; i < numStates; i++) {
			numChoices = getNumChoices(i);
			for (j = 0; j < numChoices; j++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				Iterator<Map.Entry<Integer, Function>> iter = getTransitionsIterator(i, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Function> e = iter.next();
					sorted.put(e.getKey(), e.getValue());
				}
				// Print out (sorted) transitions
				for (Map.Entry<Integer, Function> e : sorted.entrySet()) {
					Object value;
					if (e.getValue().isConstant()) {
						value = e.getValue().asBigRational();
					} else {
						value = e.getValue();
					}
					if (modelType.nondeterministic()) {
						out.print(i + " " + j + " " + e.getKey() + " " + value);
					} else {
						out.print(i + " " + e.getKey() + " " + value);
					}
					action = getAction(i, j);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
				sorted.clear();
			}
		}
	}

	@Override
	public void exportTransitionsToDotFile(int i, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		int numChoices = getNumChoices(i);
		for (int j = 0; j < numChoices; j++) {
			// action = getAction(i, j);
			String nij = null;
			if (modelType.nondeterministic()) {
				nij = "n" + i + "_" + j;
				out.print(i + " -> " + nij + " ");
				explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
				d.attributes().put("arrowhead", "none");
				d.setLabel(Integer.toString(j));

				if (decorators != null) {
					for (Decorator decorator : decorators) {
						d = decorator.decorateTransition(i, j, d);
					}
				}
				out.print(d);
				out.println(";");

				out.print(nij + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");
			}

			Iterator<Entry<Integer, Function>> it = getTransitionsIterator(i, j);
			while (it.hasNext()) {
				Entry<Integer, Function> e = it.next();

				// Note: For CTMCs, param stores the embedded DTMC, so we output that

				if (!modelType.nondeterministic()) {
					out.print(i + " -> " + e.getKey() + " ");
				} else {
					out.print(nij + " -> " + e.getKey() + " ");
				}

				Object value;
				if (e.getValue().isConstant()) {
					value = e.getValue().asBigRational();
				} else {
					value = e.getValue();
				}

				explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
				d.setLabel(value.toString());

				if (decorators != null) {
					for (Decorator decorator : decorators) {
						if (!modelType.nondeterministic()) {
							d = decorator.decorateProbability(i, e.getKey(), value, d);
						} else {
							d = decorator.decorateProbability(i, e.getKey(), j, value, d);
						}
					}
				}

				out.print(d);
				out.println(";");
			}
		}
	}

	@Override
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String infoString()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	// Other
	
	public int getNumChoices(int state)
	{
		return stateEnd(state) - stateBegin(state);
	}

	@Override
	public int getMaxNumChoices()
	{
		return numMaxChoices;
	}

	@Override
	public int getNumChoices()
	{
		return numTotalChoices;
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		return choiceEnd(stateBegin(s) + i) - choiceBegin(stateBegin(s) + i);
	}

	@Override
	public Object getAction(int s, int i)
	{
		return null;
	}

	/**
	 * Allocates memory for subsequent construction of model. 
	 * 
	 * @param numStates number of states of the model
	 * @param numTotalChoices total number of nondeterministic choices of the model
	 * @param numTotalSuccessors total number of probabilistic transitions of the model
	 */
	void reserveMem(int numStates, int numTotalChoices, int numTotalSuccessors)
	{
		rows = new int[numStates + 1];
		choices = new int[numTotalChoices + 1];
		labels = new String[numTotalSuccessors];
		cols = new int[numTotalSuccessors];
		nonZeros = new Function[numTotalSuccessors];
		sumRates = new Function[numTotalChoices];
	}

	/**
	 * Finish the current state.
	 * Starting with the 0th state, this function shall be called once all
	 * nondeterministic decisions of the current nth state have been added.
	 * Subsequent method calls of {@code finishChoice} and {@code addTransition}
	 * will then apply to the (n+1)th state. Notice that this method must be
	 * called for each state of the method, even the last one, once all its
	 * transitions have been added.
	 */
	void finishState()
	{
		int state = numStates;
		rows[numStates + 1] = numTotalChoices;
		numStates++;

		numMaxChoices = Math.max(numMaxChoices, getNumChoices(state));
	}

	/**
	 * Finished the current nondeterministic choice.
	 * Subsequent calls of {@code addTransition} will in turn add probabilistic
	 * branches to the next nondeterministic choice. Notice that this method has
	 * to be called for all nondeterministic choices of a state, even the last
	 * one. Notice that DTMCs and CTMCs should only have a single
	 * nondeterministic choice per state.
	 */
	void finishChoice()
	{
		choices[numTotalChoices + 1] = numTotalTransitions;
		numTotalChoices++;
	}

	/**
	 * Adds a probabilistic branch to the current nondeterministic choice.
	 * Notice that by this function the probability to leave to a given state
	 * shall be specified, <i>not</i> the rate to this state. Instead, the
	 * sum of rates leaving a certain nondeterministic decision shall be
	 * specified using {@code setSumLeaving}.
	 * 
	 * @param toState to which state the probabilistic choice leads
	 * @param probFn with which probability it leads to this state
	 * @param action action with which the choice is labelled
	 */
	void addTransition(int toState, Function probFn, String action)
	{
		cols[numTotalTransitions] = toState;
		nonZeros[numTotalTransitions] = probFn;
		labels[numTotalTransitions] = action;
		numTotalTransitions++;
	}

	/**
	 * Sets the total sum of leaving rate of the current nondeterministic choice.
	 * For discrete-time models, this function shall always be called with
	 * {@code leaving = 1}.
	 * 
	 * @param leaving total sum of leaving rate of the current nondeterministic choice
	 */
	void setSumLeaving(Function leaving)
	{
		sumRates[numTotalChoices] = leaving;
	}

	/**
	 * Returns the number of the first nondeterministic choice of {@code state}.
	 * 
	 * @param state state to return number of first nondeterministic choice of
	 * @return number of first nondeterministic choice of {@code state}
	 */
	int stateBegin(int state)
	{
		return rows[state];
	}

	/**
	 * Returns the number of the last nondeterministic choice of {@code state} plus one.
	 * 
	 * @param state state to return number of last nondeterministic choice of
	 * @return number of first nondeterministic choice of {@code state} plus one
	 */
	int stateEnd(int state)
	{
		return rows[state + 1];
	}

	/**
	 * Returns the first probabilistic branch of the given nondeterministic decision.
	 * 
	 * @param choice choice of which to return the first probabilitic branch
	 * @return number of first probabilistic branch of {@choice}
	 */
	int choiceBegin(int choice)
	{
		return choices[choice];
	}

	/**
	 * Returns the last probabilistic branch of the given nondeterministic decision plus one.
	 * 
	 * @param choice choice of which to return the first probabilitic branch
	 * @return number of last probabilistic branch of {@choice} plus one
	 */
	int choiceEnd(int choice)
	{
		return choices[choice + 1];
	}

	/**
	 * Returns the successor state of the given probabilistic branch.
	 * 
	 * @param succNr probabilistic branch to return successor state of
	 * @return state which probabilistic branch leads to
	 */
	int succState(int succNr)
	{
		return cols[succNr];
	}

	/**
	 * Returns the probability of the given probabilistic branch
	 * 
	 * @param succNr probabilistic branch to return probability of
	 * @return probability of given probabilistic branch
	 */
	Function succProb(int succNr)
	{
		return nonZeros[succNr];
	}

	/**
	 * Returns the label of the given probabilistic branch
	 * 
	 * @param succNr probabilistic branch to return label of
	 * @return label of given probabilistic branch
	 */
	String getLabel(int succNr)
	{
		return labels[succNr];
	}

	/**
	 * Returns the total sum of leaving rates of the given nondeterministic choice 
	 * 
	 * @param choice nondeterministic choice to return sum of leaving rates of
	 * @return sum of leaving rates of given nondeterministic choice
	 */
	Function sumLeaving(int choice)
	{
		return sumRates[choice];
	}

	/**
	 * Instantiates the parametric model at a given point.
	 * All transition probabilities, etc. will be evaluated and set as
	 * probabilities of the concrete model at the given point. 
	 * <br>
	 * If {@code checkWellDefinedness} is {@code true}, checks that the
	 * instantiated probabilities are actually probabilities and graph-preserving.
	 * If that is not the case, this method then returns {@code null}.
	 * 
	 * @param point point to instantiate model at
	 * @param checkWellDefinedness should we check that the model is well-defined?
	 * @return nonparametric model instantiated at {@code point}
	 */
	ParamModel instantiate(Point point, boolean checkWellDefinedness)
	{
		ParamModel result = new ParamModel();
		result.setModelType(getModelType());
		result.reserveMem(numStates, numTotalChoices, numTotalTransitions);
		result.initialStates = new LinkedList<Integer>(this.initialStates);
		for (int state = 0; state < numStates; state++) {
			for (int choice = stateBegin(state); choice < stateEnd(state); choice++) {
				for (int succ = choiceBegin(choice); succ < choiceEnd(choice); succ++) {
					BigRational p = succProb(succ).evaluate(point);
					if (checkWellDefinedness) {
						if (p.isSpecial() || p.compareTo(BigRational.ONE) == 1 || p.signum() <= 0) {
							// For graph-preservation, probabilities have to be >0 and <=1
							// Note: for CTMCs, succProb yields the probabilities of the embedded
							// DTMC, while the rates are available separately (sumLeaving)
							return null;
						}
					}
					result.addTransition(succState(succ), functionFactory.fromBigRational(p), labels[succ]);

				}
				result.setSumLeaving(functionFactory.fromBigRational(this.sumLeaving(choice).evaluate(point)));
				result.finishChoice();
			}
			result.finishState();
		}
		result.functionFactory = this.functionFactory;

		return result;
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the function factory to be used in this parametric model.
	 * 
	 * @param functionFactory function factory to be used in this parametric model
	 */
	void setFunctionFactory(FunctionFactory functionFactory)
	{
		this.functionFactory = functionFactory;
	}

	/**
	 * Returns the function factory used in this parametric model.
	 * 
	 * @return function factory used in this parametric model
	 */
	FunctionFactory getFunctionFactory()
	{
		return functionFactory;
	}

}
