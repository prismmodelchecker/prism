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

public class SystemHide extends SystemDefn
{
	// operand
	
	private SystemDefn operand;
	
	// vector of actions to be hidden
	
	private Vector actions;
	
	// constructors
	
	public SystemHide()
	{
		actions = new Vector();
	}
	
	public SystemHide(SystemDefn s)
	{
		operand = s;
		actions = new Vector();
	}
	
	// set methods
	
	public void setOperand(SystemDefn s)
	{
		operand = s;
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
	
	public SystemDefn getOperand()
	{
		return operand;
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
		operand.getModules(v);
	}

	// get list of all synchronising actions _introduced_ (recursively)
	
	public void getSynchs(Vector v)
	{
		// recurse
		operand.getSynchs(v);
	}

	// check
	
	public void check(Vector synchs) throws PrismException
	{
		int i, n;
		String s;
		Vector v;
		
		// check all hidden actions are valid
		n = getNumActions();
		for (i = 0; i < n; i++) {
			s = getAction(i);
			if (!(synchs.contains(s))) {
				throw new PrismException("Invalid action \"" + s + "\" in \"system\" construct");
			}
		}
		
		// check each action is not hidden more than once
		v = new Vector();
		n = getNumActions();
		for (i = 0; i < n; i++) {
			s = getAction(i);
			if (v.contains(s)) {
				throw new PrismException("Action \"" + s + "\" is hidden more than once in \"system\" construct");
			}
			else {
				v.addElement(s);
			}
		}
		
		// recurse
		operand.check(synchs);
	}
	
	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		s = s + operand + "/{";
		n = getNumActions();
		for (i = 0; i < n-1; i++) {
			s = s + getAction(i) + ",";
		}
		if (n > 0) {
			s = s + getAction(n-1);
		}
		s = s + "}";
		
		return s;
	}
}

//------------------------------------------------------------------------------
