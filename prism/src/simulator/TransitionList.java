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

package simulator;

import java.util.*;

import parser.*;
import prism.*;

public class TransitionList
{
	private ArrayList<Choice> choices = new ArrayList<Choice>();
	/** The index of the choice containing each transition. */
	private ArrayList<Integer> transitionIndices = new ArrayList<Integer>();
	/** The offset with the choice containing each transition. */
	private ArrayList<Integer> transitionOffsets = new ArrayList<Integer>();
	private int numChoices = 0;
	private int numTransitions = 0;
	private double probSum = 0.0;

	// TODO: document this
	public class Ref
	{
		public int i;
		public int offset;
		//int index;
		//Choice ch;
	}

	public void clear()
	{
		choices.clear();
		transitionIndices.clear();
		transitionOffsets.clear();
		numChoices = 0;
		numTransitions = 0;
		probSum = 0.0;
	}

	public void add(Choice tr)
	{
		int i, n;
		choices.add(tr);
		n = tr.size();
		for (i = 0; i < n; i++) {
			transitionIndices.add(choices.size() - 1);
			transitionOffsets.add(i);
		}
		numChoices++;
		numTransitions += tr.size();
		probSum += tr.getProbabilitySum();
	}
	
	/**
	 * Scale probability/rate of all transitions in all choices, multiplying by d.
	 */
	public void scaleProbabilitiesBy(double d)
	{
		for (int i = 0; i < numChoices; i++) {
			getChoice(i).scaleProbabilitiesBy(d);
		}
	}
	
	// ACCESSORS

	/**
	 * Get the number of choices.
	 */
	public int getNumChoices()
	{
		return numChoices;
	}

	/**
	 * Get the number of transitions.
	 */
	public int getNumTransitions()
	{
		return numTransitions;
	}

	/**
	 * Get the total sum of all probabilities (or rates).
	 */
	public double getProbabilitySum()
	{
		return probSum;
	}

	// Get access to Choice objects 

	/**
	 * Get the ith choice.
	 */
	public Choice getChoice(int i)
	{
		return choices.get(i);
	}

	/**
	 * Get the choice containing a transition of a given index.
	 */
	public Choice getChoiceOfTransition(int index)
	{
		return choices.get(transitionIndices.get(index));
	}

	// Get index/offset info

	/**
	 * Get the index of the choice containing a transition of a given index.
	 */
	public int getChoiceIndexOfTransition(int index)
	{
		return transitionIndices.get(index);
	}

	/**
	 * Get the offset of a transition within its containing choice.
	 */
	public int getChoiceOffsetOfTransition(int index)
	{
		return transitionOffsets.get(index);
	}

	/**
	 * Get the (total) index of a transition from the index of its containing choice and its offset within it.
	 */
	public int getTotalIndexOfTransition(int i, int offset)
	{
		return transitionIndices.indexOf(i) + offset;
	}

	// Random selection of a choice 

	/**
	 * Get a reference to a transition according to a total probability (or rate) sum, x.
	 * i.e.the first transition for which the sum of probabilities/rates of that and all prior
	 * transitions (across all choices) exceeds x.
	 * @param x Probability (or rate) sum
	 * @param ref Empty transition reference to store result
	 */
	public void getChoiceIndexByProbabilitySum(double x, Ref ref)
	{
		int i;
		Choice choice;
		double d = 0.0, tot = 0.0;
		// Add up choice prob/rate sums to find choice
		for (i = 0; x >= tot && i < numChoices; i++) {
			d = getChoice(i).getProbabilitySum();
			tot += d;
		}
		ref.i = i - 1;
		// Pick transition within choice 
		choice = getChoice(i - 1);
		if (choice.size() > 1) {
			ref.offset = choice.getIndexByProbabilitySum(x - (tot - d));
		} else {
			ref.offset = 0;
		}
	}

	// Direct access to transition info

	/**
	 * Get a string describing the action/module of a transition, specified by its index.
	 * (form is "module" or "[action]")
	 */
	public String getTransitionModuleOrAction(int index)
	{
		return getChoiceOfTransition(index).getModuleOrAction();
	}

	/**
	 * Get the index of the action/module of a transition, specified by its index.
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public int getTransitionModuleOrActionIndex(int index)
	{
		return getChoiceOfTransition(index).getModuleOrActionIndex();
	}

	/**
	 * Get the index of the action/module of a choice, specified by its index.
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public int getChoiceModuleOrActionIndex(int index)
	{
		return getChoice(index).getModuleOrActionIndex();
	}

	/**
	 * Get the probability/rate of a transition, specified by its index.
	 */
	public double getTransitionProbability(int index)
	{
		return getChoiceOfTransition(index).getProbability(transitionOffsets.get(index));
	}

	/**
	 * Get a string describing the updates making up a transition, specified by its index.
	 * This is in abbreviated form, i.e. x'=1, rather than x'=x+1.
	 * Format is: x'=1, y'=0, with empty string for empty update.
	 * Only variables updated are included in list (even if unchanged).
	 */
	public String getTransitionUpdateString(int index, State currentState) throws PrismLangException
	{
		return getChoiceOfTransition(index).getUpdateString(transitionOffsets.get(index), currentState);
	}

	/**
	 * Get a string describing the updates making up a transition, specified by its index.
	 * This is in full, i.e. of the form x'=x+1, rather than x'=1.
	 * Format is: (x'=x+1) & (y'=y-1), with empty string for empty update.
	 * Only variables updated are included in list.
	 * Note that expressions may have been simplified from original model. 
	 */
	public String getTransitionUpdateStringFull(int index)
	{
		return getChoiceOfTransition(index).getUpdateStringFull(transitionOffsets.get(index));
	}

	/**
	 * Get the target of a transition (as a new State object), specified by its index.
	 */
	public State computeTransitionTarget(int index, State currentState) throws PrismLangException
	{
		return getChoiceOfTransition(index).computeTarget(transitionOffsets.get(index), currentState);
	}
	
	// Other checks and queries
	
	/**
	 * Is there a deadlock (i.e. no available transitions)?
	 */
	public boolean isDeadlock()
	{
		return numChoices == 0;
	}

	/**
	 * Are the choices deterministic? (i.e. a single probability 1.0 transition)
	 * (will also return true for a continuous-time model matching this
	 * definition, since TransitionList does not know about model type)
	 */
	public boolean isDeterministic()
	{
		return numTransitions == 1 && getChoice(0).getProbability(0) == 1.0;
	}

	/**
	 * Is there a deterministic self-loop, i.e. do all transitions go to the current state.
	 */
	public boolean isDeterministicSelfLoop(State currentState)
	{
		// TODO: make more efficient, and also limit calls to it
		// (e.g. only if already stayed in state twice?)
		int i, n;
		State newState = new State(currentState);
		try {
			for (Choice ch : choices) {
				n = ch.size();
				for (i = 0; i < n; i++) {
					ch.computeTarget(i, currentState, newState);
					if (!currentState.equals(newState)) {
						// Found a non-loop
						return false;
					}
				}
			}
		} catch (PrismLangException e) {
			// If anything goes wrong when evaluating, just return false.
			return false;
		}
		// All targets loop
		return true;
	}

	/**
	 * Check the validity of the available transitions for a given model type.
	 * Throw a PrismException if an error is found.
	 */
	public void checkValid(ModelType modelType) throws PrismException
	{
		for (Choice ch : choices) {
			ch.checkValid(modelType);
		}
	}
	
	/**
	 * Check whether the available transitions (from a particular state)
	 * would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public void checkForErrors(State currentState, VarList varList) throws PrismException
	{
		for (Choice ch : choices) {
			ch.checkForErrors(currentState, varList);
		}
	}
	
	@Override
	public String toString()
	{
		String s = "";
		boolean first = true;
		for (Choice ch : choices) {
			if (first)
				first = false;
			else
				s += ", ";
			s += ch.toString();
		}
		return s;
	}
}
