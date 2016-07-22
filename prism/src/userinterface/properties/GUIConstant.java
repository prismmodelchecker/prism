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

package userinterface.properties;

import parser.ast.*;
import parser.type.*;
import prism.*;

class GUIConstant
{
	public GUIMultiProperties parent;
	public String name;
	public String constant;
	public Type type;
	public Exception parseError;
	
	public GUIConstant(GUIMultiProperties parent, String name, String constant, Type type)
	{
		this.parent = parent;
		this.name = name;
		this.constant = constant;
		this.type = type;
		this.parseError = null;
	}
	
	public void parse()
	{
		Expression expr = null;
		this.parseError = null;
		// See if constant definition is parseable
		try {
			// Check name is a valid identifier
			try { expr = Prism.parseSingleExpressionString(name); }
			catch (PrismLangException e) { throw new PrismException("Invalid constant name \""+name+"\""); }
			if (expr == null || !(expr instanceof ExpressionIdent)) throw new PrismException("Invalid constant name \""+name+"\"");
			// Check (non-empty) constant definition is valid (single) expression
			try { if (!("".equals(constant))) Prism.parseSingleExpressionString(constant); }
			catch (PrismLangException e) { throw new PrismException("Invalid expression \""+constant+"\""); }
		}
		catch (PrismException e) {
			this.parseError = e;
		}
	}
	
	public boolean isParseable() { return parseError==null; }
	
	public String toString()
	{
		return "const "+type.getTypeString()+" "+name+getValueString()+";";
	}
	
	public String getValueString()
	{
		if ("".equals(constant)) return "";
		else return " = " + constant; 
	}
}
