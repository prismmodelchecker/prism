//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

package parser;

import java.util.Vector;

import prism.PrismException;
import simulator.*;

public class RewardStructItem
{
	private String synch;
	private Expression states;
	private Expression reward;
	
	// constructor
	
	public RewardStructItem(String a, Expression s, Expression r)
	{
		synch = a;
		states = s;
		reward = r;
	}

	// get methods
	
	public String getSynch()
	{
		return synch;
	}
	
	public Expression getStates()
	{
		return states;
	}
	
	public Expression getReward()
	{
		return reward;
	}
	
	/**
	 *	Returns whether this reward is a state(false) or transition(true) reward
	 */
	public boolean isTransitionReward()
	{
		return (synch!=null);
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		states = states.findAllFormulas(formulaList);
		reward = reward.findAllFormulas(formulaList);
	}

	// expand any formulas
	
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		states = states.expandFormulas(formulaList);
		reward = reward.expandFormulas(formulaList);
	}

	// find all constants (i.e. locate idents which are constants)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		states = states.findAllConstants(constantList);
		reward = reward.findAllConstants(constantList);
	}

	// find all variables (i.e. locate idents which are variables)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		states = states.findAllVars(varIdents, varTypes);
		reward = reward.findAllVars(varIdents, varTypes);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		// check expressions are ok
		states.check();
		reward.check();
		// type check expressions
		if (!Expression.canAssignTypes(Expression.DOUBLE, reward.getType())) {
			throw new PrismException("Type error in state reward \"" + this + "\"");
		}
		if (states.getType() != Expression.BOOLEAN) {
			throw new PrismException("Type error in state reward \"" + this + "\"");
		}
	}

	/**
	 *	Convert to simulator data structures for rewards
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		if(synch == null) //state reward
		{
			return SimulatorEngine.createStateReward(states.toSimulator(sim), reward.toSimulator(sim));
		}
		else //transition reward
		{
			if(synch.equals(""))
				return SimulatorEngine.createTransitionReward(-1, states.toSimulator(sim), reward.toSimulator(sim));
			else
				return SimulatorEngine.createTransitionReward(sim.getIndexOfAction(synch), states.toSimulator(sim), reward.toSimulator(sim));
		}
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		
		if (synch != null) s += "[" + synch + "] ";
		s += states + " : " + reward + ";";
		
		return s;
	}

	// print
	
	public void print()
	{
		System.out.println(toString());
	}
}

//------------------------------------------------------------------------------
