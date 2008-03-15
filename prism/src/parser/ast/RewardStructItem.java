//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package parser.ast;

import parser.visitor.*;
import prism.PrismLangException;

public class RewardStructItem extends ASTElement
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

	// Set methods
	
	public void setStates(Expression e)
	{
		states = e;
	}
	
	public void setReward(Expression e)
	{
		reward = e;
	}
	
	// Get methods
	
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
	 *	Returns whether this reward is a state (false) or transition (true) reward
	 */
	public boolean isTransitionReward()
	{
		return (synch!=null);
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		
		if (synch != null) s += "[" + synch + "] ";
		s += states + " : " + reward + ";";
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		RewardStructItem ret = new RewardStructItem(synch, states.deepCopy(), reward.deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
