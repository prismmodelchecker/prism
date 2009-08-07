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

public class ChoiceList implements Choice
{
	protected String action;
	// List of multiple targets and associated info
	// Size of list is stored implicitly in target.length
	// TODO: convert to arrays?
	protected List<State> target;
	protected List<Double> probability;
	protected List<Command> command;
	
	public ChoiceList(int n)
	{
		target = new ArrayList<State>(n);
		probability = new ArrayList<Double>(n);
		command = new ArrayList<Command>(n);
	}
	
	// Set methods
	
	public void setAction(String action)
	{
		this.action = action;
	}

	public void setTarget(int i, State target)
	{
		if (i < 0 || i >= size())
			return;
		this.target.set(i, target);
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
	
	// Get methods
	
	public String getAction()
	{
		return action;
	}

	public int size()
	{
		return target.size();
	}
	
	public State getTarget()
	{
		return getTarget(0);
	}

	public State getTarget(int i)
	{
		if (i < 0 || i >= size())
			return null;
			//throw new PrismLangException("Invalid grouped transition index " + i);
		return target.get(i);
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

	public void setTarget(State target)
	{
		setTarget(0, target);
	}

	public void addTarget(State target)
	{
		this.target.add(target);
	}

	public int getIndexByProbSum(double x)
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
			s += getProbability(i)+":"+getTarget(i);
		}
		return s;
	}
}
