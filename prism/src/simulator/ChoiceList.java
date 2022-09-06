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
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;

public class ChoiceList implements Choice
{
	protected String action;
	// List of multiple targets and associated info
	// Size of list is stored implicitly in target.length // TODO: no
	// TODO: convert to arrays?
	protected List<List<Update>> updates;
	protected List<Double> probability;
	protected List<Command> command;
	
	public ChoiceList(int n)
	{
		updates = new ArrayList<List<Update>>(n);
		probability = new ArrayList<Double>(n);
		command = new ArrayList<Command>(n);
	}
	
	// Set methods
	
	public void setAction(String action)
	{
		this.action = action;
	}

	public void setProbability(double probability)
	{
		setProbability(0, probability);
	}

	public void addProbability(double probability)
	{
		this.probability.add(probability);
	}

	public void setProbability(int i, double probability)
	{
		if (i < 0 || i >= size())
			return;
		this.probability.set(i, probability);
	}

	public void setCommand(Command command)
	{
		setCommand(0, command);
	}

	public void addCommand(Command command)
	{
		this.command.add(command);
	}

	public void setCommand(int i, Command command)
	{
		if (i < 0 || i >= size())
			return;
		this.command.set(i, command);
	}
	
	@Override
	public void scaleProbabilitiesBy(double d)
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			probability.set(i, probability.get(i) * d);
		}
	}

	// Get methods
	
	public int getModuleOrActionIndex()
	{
		return 0; // TODO
	}

	public String getModuleOrAction()
	{
		return null; // TODO
	}

	public String getAction()
	{
		return action;
	}

	public int size()
	{
		return probability.size();
	}
	
	public String getUpdateString(int i, State currentState) throws PrismLangException
	{
		String s = "(";
		for (Update up : updates.get(i))
			s += up;
		s += ")";
		return s;
	}
	
	public String getUpdateStringFull(int i)
	{
		return null;
	}
	
	public State computeTarget(State oldState) throws PrismLangException
	{
		return computeTarget(0, oldState);
	}

	public void computeTarget(State oldState, State newState) throws PrismLangException
	{
		computeTarget(0, oldState, newState);
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

	public int getIndexByProbabilitySum(double x)
	{
		int i, n;
		double d;
		n = size();
		i = 0;
		d = 0.0;
		for (i = 0; x >= d && i < n; i++) {
			d += probability.get(i);
		}
		return i - 1;
	}
	
	@Override
	public void checkValid(ModelType modelType) throws PrismException
	{
		// TODO
	}
	
	@Override
	public void checkForErrors(State currentState, VarList varList) throws PrismException
	{
		// TODO
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
				s+= " + ";
			s += getProbability(i)+":"+updates.get(i);
		}
		return s;
	}
}
