//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

public class RewardStruct
{
	private Vector items;	// list of items
	
	// constructor
	
	public RewardStruct()
	{
		items = new Vector();
	}

	// add reward info
	
	public void addItem(String synch, Expression states, Expression reward)
	{
		items.add(new RewardStructItem(synch, states, reward));
	}
	
	public void addItem(RewardStructItem rsi)
	{
		items.add(rsi);
	}

	// get methods
	
	public int getNumItems()
	{
		return items.size();
	}
	
	public RewardStructItem getRewardStructItem(int i)
	{
		return (RewardStructItem)items.elementAt(i);
	}
	
	public String getSynch(int i)
	{
		return ((RewardStructItem)items.elementAt(i)).getSynch();
	}
	
	public Expression getStates(int i)
	{
		return ((RewardStructItem)items.elementAt(i)).getStates();
	}
	
	public Expression getReward(int i)
	{
		return ((RewardStructItem)items.elementAt(i)).getReward();
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		// go thru item list
		n = getNumItems();
		for (i = 0; i < n; i++) {
			getRewardStructItem(i).findAllFormulas(formulaList);
		}
	}

	// expand any formulas
	
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		// go thru item list
		n = getNumItems();
		for (i = 0; i < n; i++) {
			getRewardStructItem(i).expandFormulas(formulaList);
		}
	}

	// find all constants (i.e. locate idents which are constants)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		
		// go thru item list
		n = getNumItems();
		for (i = 0; i < n; i++) {
			getRewardStructItem(i).findAllConstants(constantList);
		}
	}

	// find all variables (i.e. locate idents which are variables)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		
		// go thru item list
		n = getNumItems();
		for (i = 0; i < n; i++) {
			getRewardStructItem(i).findAllVars(varIdents, varTypes);
		}
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		int i, n;
		
		// go thru item list
		n = getNumItems();
		for (i = 0; i < n; i++) {
			getRewardStructItem(i).check();
		}
	}

	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		s += "rewards\n\n";
		n = getNumItems();
		for (i = 0; i < n; i++) {
			s += "\t" + items.elementAt(i) + "\n";
		}
		s += "\nendrewards\n";
		
		return s;
	}
	
	// print
	
	public void print()
	{
		System.out.println(toString());
	}
}

//------------------------------------------------------------------------------
