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

// note: although this makes no difference to the meaning
// of the expression, it means we can keep the user's
// original bracketting for display purposes

public class SystemBrackets extends SystemDefn
{
	// operand
	
	private SystemDefn operand;

	// constructors
	
	public SystemBrackets()
	{
	}

	public SystemBrackets(SystemDefn s)
	{
		operand = s;
	}

	// set method
	
	public void setOperand(SystemDefn s)
	{
		operand = s;
	}
	
	// get method
	
	public SystemDefn getOperand()
	{
		return operand;
	}
	
	// get list of all modules appearing (recursively)
	
	public void getModules(Vector v)
	{
		operand.getModules(v);
	}

	// get list of all synchronising actions _introduced_ (recursively)

	public void getSynchs(Vector v)
	{
		operand.getSynchs(v);
	}
	
	// check
	
	public void check(Vector synchs) throws PrismException
	{
		operand.check(synchs);
	}
	
	// convert to string
		
	public String toString()
	{
		return "(" + operand + ")";
	}
}

//------------------------------------------------------------------------------
