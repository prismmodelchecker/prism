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

public class SystemHide extends SystemDefn
{
	// Operand
	private SystemDefn operand;
	// Actions to be hidden
	private Vector<String> actions;
	
	// Constructors
	
	public SystemHide()
	{
		actions = new Vector<String>();
	}
	
	public SystemHide(SystemDefn s)
	{
		operand = s;
		actions = new Vector<String>();
	}
	
	// Set methods
	
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
	
	// Get methods
	
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
		return actions.elementAt(i);
	}
		
	public boolean containsAction(String s)
	{
		return actions.contains(s);
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
	
	/**
	 * Perform a deep copy.
	 */
	public SystemDefn deepCopy()
	{
		int i, n;
		SystemHide ret = new SystemHide(getOperand().deepCopy());
		n = getNumActions();
		for (i = 0; i < n; i++) {
			ret.addAction(getAction(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
