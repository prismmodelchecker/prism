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

public class SystemRename extends SystemDefn
{
	// Operand
	private SystemDefn operand;
	// Vectors for pairs of actions to be renamed
	private Vector<String> from;
	private Vector<String> to;
	
	// Constructors
	
	public SystemRename()
	{
		from = new Vector<String>();
		to = new Vector<String>();
	}
	
	public SystemRename(SystemDefn s)
	{
		operand = s;
		from = new Vector<String>();
		to = new Vector<String>();
	}
	
	// Set methods
	
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
			
	// Get methods
	
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
		return from.elementAt(i);
	}
		
	public String getTo(int i)
	{
		return to.elementAt(i);
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
		
	// Methods required for SystemDefn (all subclasses should implement):
	
	/**
	 * Get list of all modules appearing (recursively).
	 */
	public void getModules(Vector<String> v)
	{
		operand.getModules(v);
	}

	/**
	 * Get list of all synchronising actions _introduced_ (recursively).
	 */
	public void getSynchs(Vector<String> v)
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
	
	/**
	 * Perform a deep copy.
	 */
	public SystemDefn deepCopy()
	{
		int i, n;
		SystemRename ret = new SystemRename(getOperand().deepCopy());
		n = getNumRenames();
		for (i = 0; i < n; i++) {
			ret.addRename(getFrom(i), getTo(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
