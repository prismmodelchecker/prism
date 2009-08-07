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

import parser.*;
import parser.ast.*;

public class ChoiceSingleton implements Choice
{
	protected String action;
	protected State target;
	protected double probability;
	protected Command command;

	// Set methods
	
	public void setAction(String action)
	{
		this.action = action;
	}

	public void setTarget(State target)
	{
		this.target = target;
	}

	public void setProbability(double probability)
	{
		this.probability = probability;
	}

	public void setCommand(Command command)
	{
		this.command = command;
	}
	
	// Get methods
	
	public int size()
	{
		return 1;
	}
	
	public int getIndexByProbSum(double x)
	{
		return 0;
	}
	
	public String getAction()
	{
		return action;
	}

	public State getTarget()
	{
		return target;
	}

	public State getTarget(int i)
	{
		return (i == 0) ? target : null;
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
	public String toString()
	{
		return "-{" + ("".equals(action) ?  "" : action+"," ) + probability + "}->" + target; 
	}
}
