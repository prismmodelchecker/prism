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

package parser;

import java.util.Vector;

import prism.PrismException;

public class SystemParallel extends SystemDefn
{
	// pair of operands
	
	private SystemDefn operand1;
	private SystemDefn operand2;
	
	// vector of synchronising actions
	
	private Vector actions;
	
	// constructors
	
	public SystemParallel()
	{
		actions = new Vector();
	}
	
	public SystemParallel(SystemDefn s1, SystemDefn s2)
	{
		operand1 = s1;
		operand2 = s2;
		actions = new Vector();
	}
	
	// set methods
	
	public void setOperand1(SystemDefn s1)
	{
		operand1 = s1;
	}
	
	public void setOperand2(SystemDefn s2)
	{
		operand2 = s2;
	}
	
	public void addAction(String s)
	{
		actions.addElement(s);
	}
		
	public void setAction(int i, String s)
	{
		actions.setElementAt(s, i);
	}
			
	// get methods
	
	public SystemDefn getOperand1()
	{
		return operand1;
	}
	
	public SystemDefn getOperand2()
	{
		return operand2;
	}

	public int getNumActions()
	{
		return actions.size();
	}
	
	public String getAction(int i)
	{
		return (String)actions.elementAt(i);
	}
		
	public boolean containsAction(String s)
	{
		return actions.contains(s);
	}
		
	// get list of all modules appearing (recursively)
	
	public void getModules(Vector v)
	{
		operand1.getModules(v);
		operand2.getModules(v);
	}

	// get list of all synchronising actions _introduced_ (recursively)

	public void getSynchs(Vector v)
	{
		operand1.getSynchs(v);
		operand2.getSynchs(v);
	}
	
	// check
	
	public void check(Vector synchs) throws PrismException
	{
		int i, n;
		String s;
		Vector v;
		
		// check all actions are valid
		n = getNumActions();
		for (i = 0; i < n; i++) {
			s = getAction(i);
			if (!(synchs.contains(s))) {
				throw new PrismException("Invalid action \"" + s + "\" in \"system\" construct");
			}
		}
		
		// check each action is not used more than once
		v = new Vector();
		n = getNumActions();
		for (i = 0; i < n; i++) {
			s = getAction(i);
			if (v.contains(s)) {
				throw new PrismException("Action \"" + s + "\" is duplicated in parallel composition in \"system\" construct");
			}
			else {
				v.addElement(s);
			}
		}
		
		// recurse
		operand1.check(synchs);
		operand2.check(synchs);
	}
	
	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		s = s + operand1 + " |[";
		n = getNumActions();
		for (i = 0; i < n-1; i++) {
			s = s + getAction(i) + ",";
		}
		if (n > 0) {
			s = s + getAction(n-1);
		}
		s = s + "]| " + operand2;
		
		return s;
	}
}

//------------------------------------------------------------------------------
