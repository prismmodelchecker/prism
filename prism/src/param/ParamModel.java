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

import java.io.File;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import parser.Values;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import explicit.ModelExplicit;

/**
 * Represents a parametric Markov model.
 * This class is used to store all types of parametric model, both models
 * with and without nondeterminism, discrete- as well as continuous-time.
 * This turned out the be the most convenient way to implement model checking
 * for parametric models.
 */
final class ParamModel extends ModelExplicit
{
	/** total number of nondeterministic choices over all states */
	private int numTotalChoices;
	/** total number of probabilistic transitions over all states */
	private int numTotalTransitions;
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
	public Iterator<Integer> getSuccessorsIterator(int s)
	{
		throw new UnsupportedOperationException();
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

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		throw new UnsupportedOperationException();
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
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog log)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void exportTransitionsToDotFile(int i, PrismLog out)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
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

	public int getNumTotalChoices()
	{
		return numTotalChoices;
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
		rows[numStates + 1] = numTotalChoices;
		numStates++;
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
	 * 
	 * @param point point to instantiate model at
	 * @return nonparametric model instantiated at {@code point}
	 */
	ParamModel instantiate(Point point)
	{
		ParamModel result = new ParamModel();
		result.reserveMem(numStates, numTotalChoices, numTotalTransitions);
		result.initialStates = new LinkedList<Integer>(this.initialStates);
		for (int state = 0; state < numStates; state++) {
			for (int choice = stateBegin(state); choice < stateEnd(state); choice++) {
				for (int succ = choiceBegin(choice); succ < choiceEnd(choice); succ++) {
					result.addTransition(succState(succ), functionFactory.fromBigRational(succProb(succ).evaluate(point)), labels[succ]);
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
