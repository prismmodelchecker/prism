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
	private ArrayList<Integer> transitionIndices = new ArrayList<Integer>();
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
		return transitionOffsets.get(i);
	}

	// Random selection of a choice 
	
	/**
	 * Get a reference to a transition according to a total probability (rate) sum, x.
	 * i.e.the first transition for which the sum of probabilities of all prior transitions
	 * (across all choices) exceeds x.
	 * Note: this only really makes sense for models where these are rates, rather than probabilities.
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
			ref.offset = choice.getIndexByProbabilitySum(tot - d);
		} else {
			ref.offset = 0;
		}
	}

	// Direct access to transition info
	
	/**
	 * Get a string describing the action/module of a transition, specified by its index.
	 */
	public String getTransitionModuleOrAction(int index)
	{
		return getChoiceOfTransition(index).getModuleOrAction();
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
	 */
	public String getTransitionUpdateString(int index)
	{
		return getChoiceOfTransition(index).getUpdateString(transitionOffsets.get(index));
	}
	
	/**
	 * Get the target of a transition (as a new State object), specified by its index.
	 */
	public State computeTransitionTarget(int index, State oldState) throws PrismLangException
	{
		return getChoiceOfTransition(index).computeTarget(transitionOffsets.get(index), oldState);
	}
	
	@Override
	public String toString()
	{
		String s = "";
		boolean first = true;
		for (Choice tr : choices) {
			if (first)
				first = false;
			else
				s += ", ";
			s += tr.toString();
		}
		return s;
	}
}
