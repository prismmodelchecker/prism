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
	// TODO: need mult actions
	//protected String action;
	
	protected int moduleOrActionIndex;
	
	// List of multiple targets and associated info
	// Size of list is stored implicitly in target.length
	// TODO: convert to arrays?
	protected List<List<Update>> updates;
	protected List<Double> probability;
	protected List<Command> command;

	public ChoiceListFlexi()
	{
		updates = new ArrayList<List<Update>>();
		probability = new ArrayList<Double>();
		command = new ArrayList<Command>();
	}

	// Set methods

	public void setModuleOrActionIndex(int moduleOrActionIndex)
	{
		this.moduleOrActionIndex = moduleOrActionIndex;
	}
	
	public void add(String action, double probability, List<Update> ups, Command command)
	{
		//this.action = action;
		this.updates.add(ups);
		this.probability.add(probability);
		this.command.add(command);
	}

	public void scaleProbabilitiesBy(double d)
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			probability.set(i, probability.get(i) * d);
		}
	}

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
				add("?", pi * getProbability(j), list, null);
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

	public int getModuleOrActionIndex()
	{
		return moduleOrActionIndex;
	}

	public String getModuleOrAction()
	{
		Update u = updates.get(0).get(0);
		Command c = u.getParent().getParent();
		if ("".equals(c.getSynch()))
			return c.getParent().getName();
		else
			return "[" + c.getSynch() + "]";
	}

	public int size()
	{
		return probability.size();
	}

	public String getUpdateString(int i)
	{
		String s = "(";
		for (Update up : updates.get(i))
			s += up;
		s += ")";
		return s;
	}
	
	public State computeTarget(int i, State oldState) throws PrismLangException
	{
		if (i < 0 || i >= size())
			throw new PrismLangException("Choice does not have an element " + i);
		State newState = new State(oldState);
		for (Update up : updates.get(i))
			up.update(oldState, newState);
		return newState;
	}

	public void computeTarget(int i, State oldState, State newState) throws PrismLangException
	{
		if (i < 0 || i >= size())
			throw new PrismLangException("Choice does not have an element " + i);
		for (Update up : updates.get(i))
			up.update(oldState, newState);
	}
	
	public State computeTarget(State oldState) throws PrismLangException
	{
		return computeTarget(0, oldState);
	}

	public void computeTarget(State oldState, State newState) throws PrismLangException
	{
		computeTarget(0, oldState, newState);
	}

	public double getProbability()
	{
		return getProbability(0);
	}

	public double getProbability(int i)
	{
		if (i < 0 || i >= size())
			return -1;
		//throw new PrismLangException("Invalid grouped transition index " + i);
		return probability.get(i);
	}

	public double getProbabilitySum()
	{
		double sum = 0.0;
		for (double d : probability)
			sum += d;
		return sum;
	}

	public Command getCommand()
	{
		return getCommand(0);
	}

	public Command getCommand(int i)
	{
		if (i < 0 || i >= size())
			return null;
		//throw new PrismLangException("Invalid grouped transition index " + i);
		return command.get(i);
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
