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
	public ArrayList<Choice> choices = new ArrayList<Choice>();
	public ArrayList<Integer> transitionIndices = new ArrayList<Integer>();
	public ArrayList<Integer> transitionOffsets = new ArrayList<Integer>();
	public int numChoices = 0;
	public int numTransitions = 0;
	public double probSum = 0.0;

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

	// Get access to Choice objects 
	
	/**
	 * Get the ith choice.
	 */
	public Choice getChoice(int i)
	{
		return choices.get(i);
	}

	/**
	 * Get the choice containing the ith transition.
	 */
	public Choice getChoiceOfTransition(int i)
	{
		return choices.get(transitionIndices.get(i));
	}

	// Get index/offset info
	
	/**
	 * Get the index of the choice containing the ith transition. 
	 */
	public int getChoiceIndexOfTransition(int i)
	{
		return transitionIndices.get(i);
	}

	/**
	 * Get the offset of the ith transition within its containing choice.
	 */
	public int getChoiceOffsetOfTransition(int i)
	{
		return transitionOffsets.get(i);
	}

	/**
	 * Get the (total) index of a transition from the index of its containing choice and its offset within it.
	 */
	public int getTotalIndexOfTransition(int i, int offset)
	{
		return transitionOffsets.get(i);
	}

	// Access to transition info
	
	// get prob (or rate) of ith transition
	public double getTransitionProbability(int i)
	{
		return getChoiceOfTransition(i).getProbability(transitionOffsets.get(i));
	}
	
	public String getTransitionActionString(int i)
	{
		return getChoiceOfTransition(i).getAction();
	}
	
	public String getTransitionUpdateString(int i)
	{
		return getChoiceOfTransition(i).getUpdateString(transitionOffsets.get(i));
	}
	
	// compute target of ith transition
	public State computeTransitionTarget(int i, State oldState) throws PrismLangException
	{
		return getChoiceOfTransition(i).computeTarget(transitionOffsets.get(i), oldState);
	}
	
	@Override
	public String toString()
	{
		String s = "";
		for (Choice tr : choices)
			s += tr.toString();
		return s;
	}
}
