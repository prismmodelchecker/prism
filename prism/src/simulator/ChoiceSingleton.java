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
import prism.*;

public class ChoiceSingleton implements Choice
{
	protected String action;
	protected List<Update> updates; 
	protected double probability;
	protected Command command;

	// Constructor
	
	public ChoiceSingleton()
	{
		updates = new ArrayList<Update>(1);
	}
	
	// Set methods
	
	public void setAction(String action)
	{
		this.action = action;
	}

	public void addUpdate(Update up)
	{
		updates.add(up);
	}

	public void setProbability(double probability)
	{
		this.probability = probability;
	}

	@Override
	public void scaleProbabilitiesBy(double d)
	{
		probability *= d;
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

	public int size()
	{
		return 1;
	}
	
	public int getIndexByProbabilitySum(double x)
	{
		return 0;
	}
	
	public String getAction()
	{
		return action;
	}

	public String getUpdateString(int i, State currentState)
	{
		String s = "(";
		for (Update up : updates)
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
		State newState = new State(oldState);
		for (Update up : updates)
			up.update(oldState, newState);
		return newState;
	}

	public void computeTarget(State oldState, State newState) throws PrismLangException
	{
		for (Update up : updates)
			up.update(oldState, newState);
	}

	public State computeTarget(int i, State oldState) throws PrismLangException
	{
		if (i == 0) return computeTarget(oldState);
		else throw new PrismLangException("Choice does not have an element " + i);
	}

	public void computeTarget(int i, State oldState, State newState) throws PrismLangException
	{
		if (i == 0) computeTarget(oldState, newState);
		else throw new PrismLangException("Choice does not have an element " + i);
	}

	public double getProbability()
	{
		return probability;
	}

	public double getProbability(int i)
	{
		return (i == 0) ? probability : -1;
	}

	public double getProbabilitySum()
	{
		return probability;
	}

	public Command getCommand()
	{
		return command;
	}

	public Command getCommand(int i)
	{
		return (i == 0) ? command : null;
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
	
	@Override
	public String toString()
	{
		return "-{" + ("".equals(action) ?  "" : action+"," ) + probability + "}->" + updates; 
	}
}
