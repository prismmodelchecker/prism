//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

public class SystemFullParallel extends SystemDefn
{
	// vector of operands
	
	private Vector operands;
	
	// constructor
	
	public SystemFullParallel()
	{
		operands = new Vector();
	}
	
	// set methods
	
	public void addOperand(SystemDefn s)
	{
		operands.addElement(s);
	}
		
	public void setOperand(int i, SystemDefn s)
	{
		operands.setElementAt(s, i);
	}
			
	// get methods
	
	public int getNumOperands()
	{
		return operands.size();
	}
	
	public SystemDefn getOperand(int i)
	{
		return (SystemDefn)operands.elementAt(i);
	}
		
	// get list of all modules appearing (recursively)
	
	public void getModules(Vector v)
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getModules(v);
		}
	}

	// get list of all synchronising actions _introduced_ (recursively)

	public void getSynchs(Vector v)
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getSynchs(v);
		}
	}
	
	// check
	
	public void check(Vector synchs) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).check(synchs);
		}
	}
		
	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		n = getNumOperands();
		for (i = 0; i < n-1; i++) {
			s = s + getOperand(i) + " || ";
		}
		if (n > 0) {
			s = s + getOperand(n-1);
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
