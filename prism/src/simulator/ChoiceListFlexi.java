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
import parser.ast.*;
import prism.PrismLangException;

public class ChoiceListFlexi implements Choice
{
	// Module/action info, encoded as an integer.
	// For an independent (non-synchronous) choice, this is -i,
	// where i is the 1-indexed module index.
	// For a synchronous choice, this is the 1-indexed action index.
	protected int moduleOrActionIndex;

	// List of multiple updates and associated probabilities/rates
	// Size of list is stored implicitly in target.length
	// Probabilities/rates are already evaluated, target states are not
	// but are just stored as lists of updates (for efficiency)
	protected List<List<Update>> updates;
	protected List<Double> probability;

	/**
	 * Create empty choice.
	 */
	public ChoiceListFlexi()
	{
		updates = new ArrayList<List<Update>>();
		probability = new ArrayList<Double>();
	}

	// Set methods

	/**
	 * Set the module/action for this choice, encoded as an integer
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public void setModuleOrActionIndex(int moduleOrActionIndex)
	{
		this.moduleOrActionIndex = moduleOrActionIndex;
	}

	/**
	 * Add a transition to this choice.
	 * @param probability Probability (or rate) of the transition
	 * @param ups List of Update objects defining transition
	 */
	public void add(double probability, List<Update> ups)
	{
		this.updates.add(ups);
		this.probability.add(probability);
	}

	/**
	 * Scale probability/rate of all transitions, multiplying by d.
	 */
	public void scaleProbabilitiesBy(double d)
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			probability.set(i, probability.get(i) * d);
		}
	}

	/**
	 * Modify this choice, constructing product of it with another.
	 */
	public void productWith(ChoiceListFlexi ch)
	{
		List<Update> list;
		int i, j, n, n2;
		double pi;

		n = ch.size();
		n2 = size();
		// Loop through each (ith) element of new choice (skipping first)
		for (i = 1; i < n; i++) {
			pi = ch.getProbability(i);
			// Loop through each (jth) element of existing choice
			for (j = 0; j < n2; j++) {
				// Create new element (i,j) of product 
				list = new ArrayList<Update>(updates.get(j).size() + ch.updates.get(i).size());
				for (Update u : updates.get(j)) {
					list.add(u);
				}
				for (Update u : ch.updates.get(i)) {
					list.add(u);
				}
				add(pi * getProbability(j), list);
			}
		}
		// Modify elements of current choice to get (0,j) elements of product
		pi = ch.getProbability(0);
		for (j = 0; j < n2; j++) {
			for (Update u : ch.updates.get(0)) {
				updates.get(j).add(u);
			}
			probability.set(j, pi * probability.get(j));
		}
	}

	// Get methods

	/**
	 * Get the module/action for this choice, as an integer index
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public int getModuleOrActionIndex()
	{
		return moduleOrActionIndex;
	}

	/**
	 * Get the module/action for this choice, as a string
	 * (form is "module" or "[action]")
	 */
	public String getModuleOrAction()
	{
		// Action label (or absence of) will be the same for all updates in a choice
		Update u = updates.get(0).get(0);
		Command c = u.getParent().getParent();
		if ("".equals(c.getSynch()))
			return c.getParent().getName();
		else
			return "[" + c.getSynch() + "]";
	}

	/**
	 * Get the number of transitions in this choice.
	 */
	public int size()
	{
		return probability.size();
	}

	/**
	 * Get the updates of the ith transition, as a string.
	 * This is in abbreviated form, i.e. x'=1, rather than x'=x+1.
	 * Format is: x'=1, y'=0, with empty string for empty update.
	 * Only variables updated are included in list (even if unchanged).
	 */
	public String getUpdateString(int i, State currentState) throws PrismLangException
	{
		int j, n;
		String s = "";
		boolean first = true;
		for (Update up : updates.get(i)) {
			n = up.getNumElements();
			for (j = 0; j < n; j++) {
				if (first)
					first = false;
				else
					s += ", ";
				s += up.getVar(j) + "'=" + up.getExpression(j).evaluate(currentState);
			}
		}
		return s;
	}

	/**
	 * Get the updates of the ith transition, as a string.
	 * This is in full, i.e. of the form x'=x+1, rather than x'=1.
	 * Format is: (x'=x+1) & (y'=y-1), with empty string for empty update.
	 * Only variables updated are included in list.
	 * Note that expressions may have been simplified from original model. 
	 */
	public String getUpdateStringFull(int i)
	{
		String s = "";
		boolean first = true;
		for (Update up : updates.get(i)) {
			if (up.getNumElements() == 0)
				continue;
			if (first)
				first = false;
			else
				s += " & ";
			s += up;
		}
		return s;
	}

	/**
	 * Compute the target for the ith transition, based on a current state,
	 * returning the result as a new State object copied from the existing one.
	 * NB: for efficiency, there are no bounds checks done on i.
	 */
	public State computeTarget(int i, State currentState) throws PrismLangException
	{
		State newState = new State(currentState);
		for (Update up : updates.get(i))
			up.update(currentState, newState);
		return newState;
	}

	/**
	 * Compute the target for the ith transition, based on a current state.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. currentState and newState should be equal when passed in.) 
	 * NB: for efficiency, there are no bounds checks done on i.
	 */
	public void computeTarget(int i, State currentState, State newState) throws PrismLangException
	{
		for (Update up : updates.get(i))
			up.update(currentState, newState);
	}

	public State computeTarget(State currentState) throws PrismLangException
	{
		return computeTarget(0, currentState);
	}

	public void computeTarget(State currentState, State newState) throws PrismLangException
	{
		computeTarget(0, currentState, newState);
	}

	/**
	 * Get the probability rate for the ith transition.
	 * NB: for efficiency, there are no bounds checks done on i.
	 */
	public double getProbability(int i)
	{
		return probability.get(i);
	}

	public double getProbability()
	{
		return getProbability(0);
	}

	public double getProbabilitySum()
	{
		double sum = 0.0;
		for (double d : probability)
			sum += d;
		return sum;
	}

	/**
	 * Return the index of a transition according to a probability (or rate) sum, x.
	 * i.e. return the index of the first transition in this choice for which the
	 * sum of probabilities/rates for all prior transitions exceeds x.
	 */
	public int getIndexByProbabilitySum(double x)
	{
		int i, n;
		double d;
		n = size();
		d = 0.0;
		for (i = 0; x >= d && i < n; i++) {
			d += probability.get(i);
		}
		return i - 1;
	}

	public String toString()
	{
		int i, n;
		boolean first = true;
		String s = "";
		n = size();
		for (i = 0; i < n; i++) {
			if (first)
				first = false;
			else
				s += " + ";
			s += getProbability(i) + ":" + updates.get(i);
		}
		return s;
	}
}
