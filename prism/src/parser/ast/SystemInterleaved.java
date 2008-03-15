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

public class SystemInterleaved extends SystemDefn
{
	// Vector of operands
	private Vector<SystemDefn> operands;
	
	// Constructor
	
	public SystemInterleaved()
	{
		operands = new Vector<SystemDefn>();
	}
	
	// Set methods
	
	public void addOperand(SystemDefn s)
	{
		operands.addElement(s);
	}
		
	public void setOperand(int i, SystemDefn s)
	{
		operands.setElementAt(s, i);
	}
			
	// Get methods
	
	public int getNumOperands()
	{
		return operands.size();
	}
	
	public SystemDefn getOperand(int i)
	{
		return operands.elementAt(i);
	}
		
	// Methods required for SystemDefn (all subclasses should implement):
	
	/**
	 * Get list of all modules appearing (recursively).
	 */
	public void getModules(Vector<String> v)
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getModules(v);
		}
	}

	/**
	 * Get list of all synchronising actions _introduced_ (recursively).
	 */
	public void getSynchs(Vector<String> v)
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getSynchs(v);
		}
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
		
		n = getNumOperands();
		for (i = 0; i < n-1; i++) {
			s = s + getOperand(i) + " ||| ";
		}
		if (n > 0) {
			s = s + getOperand(n-1);
		}
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public SystemDefn deepCopy()
	{
		int i, n;
		SystemInterleaved ret = new SystemInterleaved();
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			ret.addOperand(getOperand(i).deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
