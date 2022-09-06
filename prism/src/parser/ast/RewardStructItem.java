//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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
	// Synchronising action:
	// * null = none (i.e. state reward)
	// * "" = empty/tau/asynchronous action (i.e. transition reward)
	// * "act" = "act"-labelled action (i.e. transition reward)
	private String synch;
	// Index of action label in model's list of all actions ("synchs")
	// This is 1-indexed, with 0 denoting an independent ("tau"-labelled) command.
	// -1 denotes either none (i.e. state reward, synch==null) or not (yet) known.
	private int synchIndex;
	// Guard expression
	private Expression states;
	// Reward expression
	private Expression reward;
	
	// constructor
	
	public RewardStructItem(String a, Expression s, Expression r)
	{
		synch = a;
		synchIndex = -1;
		states = s;
		reward = r;
	}

	// Set methods
	
	public void setSynch(String s)
	{
		synch = s;
	}
	
	public void setSynchIndex(int i)
	{
		synchIndex = i;
	}
	
	public void setStates(Expression e)
	{
		states = e;
	}
	
	public void setReward(Expression e)
	{
		reward = e;
	}
	
	// Get methods
	
	/**
	 * Get the action label for this command. For independent ("tau"-labelled) commands,
	 * this is the empty string "" (it should never be null).
	 */
	public String getSynch()
	{
		return synch;
	}
	
	/**
	 * Get the index of the action label for this command (in the model's list of actions).
	 * This is 1-indexed, with 0 denoting an independent ("tau"-labelled) command.
	 * -1 denotes either none (i.e. state reward, synch==null) or not (yet) known.
	 */
	public int getSynchIndex()
	{
		return synchIndex;
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
		ret.setSynchIndex(getSynchIndex());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
