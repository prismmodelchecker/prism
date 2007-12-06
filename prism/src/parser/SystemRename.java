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

public class SystemRename extends SystemDefn
{
	// operand
	
	private SystemDefn operand;
	
	// vectors for pairs of actions to be renamed
	
	private Vector from;
	private Vector to;
	
	// constructors
	
	public SystemRename()
	{
		from = new Vector();
		to = new Vector();
	}
	
	public SystemRename(SystemDefn s)
	{
		operand = s;
		from = new Vector();
		to = new Vector();
	}
	
	// set methods
	
	public void setOperand(SystemDefn s)
	{
		operand = s;
	}
	
	public void addRename(String s1, String s2)
	{
		from.addElement(s1);
		to.addElement(s2);
	}
		
	public void setRename(int i, String s1, String s2)
	{
		from.setElementAt(s1, i);
		to.setElementAt(s2, i);
	}
			
	// get methods
	
	public SystemDefn getOperand()
	{
		return operand;
	}
	
	public int getNumRenames()
	{
		return from.size();
	}
	
	public String getFrom(int i)
	{
		return (String)from.elementAt(i);
	}
		
	public String getTo(int i)
	{
		return (String)to.elementAt(i);
	}
		
	public String getNewName(String s)
	{
		int i;
		
		i = from.indexOf(s);
		if (i == -1) {
			return s;
		}
		else {
			return (String)to.elementAt(i);
		}
	}
		
	// get list of all modules appearing (recursively)
	
	public void getModules(Vector v)
	{
		operand.getModules(v);
	}

	// get list of all synchronising actions _introduced_ (recursively)
	
	public void getSynchs(Vector v)
	{
		int i, n;
		String s;
		
		// add action names in renames
		// (only look in 'to' vector because we're only
		//  interested in actions _introduced_)
		n = getNumRenames();
		for (i = 0; i < n; i++) {
			s = getTo(i);
			if (!(v.contains(s))) v.addElement(s);
		}
		
		// recurse
		operand.getSynchs(v);
	}

	// check
	
	public void check(Vector synchs) throws PrismException
	{
		int i, n;
		String s;
		Vector v;
		
		// check all renames involve valid actions
		n = getNumRenames();
		for (i = 0; i < n; i++) {
			s = getFrom(i);
			if (!(synchs.contains(s))) {
				throw new PrismException("Invalid action \"" + s + "\" in \"system\" construct");
			}
			s = getTo(i);
			if (!(synchs.contains(s))) {
				throw new PrismException("Invalid action \"" + s + "\" in \"system\" construct");
			}
		}
		
		// check each action is not renamed more than once
		v = new Vector();
		n = getNumRenames();
		for (i = 0; i < n; i++) {
			s = getFrom(i);
			if (v.contains(s)) {
				throw new PrismException("Action \"" + s + "\" is renamed more than once in \"system\" construct");
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
		
		s = s + operand + "{";
		n = getNumRenames();
		for (i = 0; i < n-1; i++) {
			s = s + getFrom(i) + "<-" + getTo(i) + ",";
		}
		if (n > 0) {
			s = s + getFrom(n-1) + "<-" + getTo(n-1);
		}
		s = s + "}";
		
		return s;
	}
}

//------------------------------------------------------------------------------
