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

import java.util.Vector;

import parser.visitor.*;
import prism.PrismLangException;

public class RewardStruct extends ASTElement
{
	private String name;		// name (optional)
	private Vector<RewardStructItem> items;		// list of items
	private int numStateItems;	// how may of the items are state rewards
	private int numTransItems;	// how may of the items are transition rewards
	
	// Constructor
	
	public RewardStruct()
	{
		name = "";
		items = new Vector<RewardStructItem>();
		numStateItems = 0;
		numTransItems = 0;
	}

	public void setName(String n)
	{
		name = n;
	}

	// Set methods
	
	public void addItem(String synch, Expression states, Expression reward)
	{
		addItem(new RewardStructItem(synch, states, reward));
	}
	
	public void addItem(RewardStructItem rsi)
	{
		items.add(rsi);
		if (rsi.isTransitionReward()) numTransItems++; else numStateItems++;
	}

	public void setRewardStructItem(int i, RewardStructItem rsi)
	{
		if (getRewardStructItem(i).isTransitionReward()) numTransItems--; else numStateItems--;
		items.set(i, rsi);
		if (rsi.isTransitionReward()) numTransItems++; else numStateItems++;
	}

	// Get methods
	
	public String getName()
	{
		return name;
	}
	
	public int getNumItems()
	{
		return items.size();
	}
	
	public int getNumStateItems()
	{
		return numStateItems;
	}
	
	public int getNumTransItems()
	{
		return numTransItems;
	}
	
	public RewardStructItem getRewardStructItem(int i)
	{
		return items.elementAt(i);
	}
	
	public String getSynch(int i)
	{
		return getRewardStructItem(i).getSynch();
	}
	
	public Expression getStates(int i)
	{
		return getRewardStructItem(i).getStates();
	}
	
	public Expression getReward(int i)
	{
		return getRewardStructItem(i).getReward();
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
		int i, n;
		String s = "";
		
		s += "rewards";
		if (name != null && name.length() > 0) s += " \""+name+"\"";
		s += " \n\n";
		n = getNumItems();
		for (i = 0; i < n; i++) {
			s += "\t" + getRewardStructItem(i) + "\n";
		}
		s += "\nendrewards\n";
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		RewardStruct ret = new RewardStruct();
		ret.setName(name);
		n = getNumItems();
		for (i = 0; i < n; i++) {
			ret.addItem((RewardStructItem)getRewardStructItem(i).deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
