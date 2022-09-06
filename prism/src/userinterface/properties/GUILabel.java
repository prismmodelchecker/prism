//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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
import prism.*;

public class GUILabel
{
	public GUIMultiProperties parent;
	public String name;
	public String label;
	public Exception parseError;
	
	public GUILabel(GUIMultiProperties parent, String name, String label)
	{
		this.parent = parent;
		this.name = name;
		this.label = label;
		this.parseError = null;
	}
	
	public void parse()
	{
		Expression expr = null;
		this.parseError = null;
		// See if label definition is parseable
		try {
			// Check name is a valid identifier
			try { expr = parent.getPrism().parseSingleExpressionString(name); }
			catch (PrismLangException e) { throw new PrismException("Invalid label name \""+name+"\""); }
			if (expr == null || !(expr instanceof ExpressionIdent)) throw new PrismException("Invalid label name \""+name+"\"");
			// Check (non-empty) label definition is valid (single) expression
			try { if (!("".equals(label))) parent.getPrism().parseSingleExpressionString(label); }
			catch (PrismLangException e) { throw new PrismException("Invalid expression \""+label+"\""); }
		}
		catch (PrismException e) {
			this.parseError = e;
		}
	}
	
	public boolean isParseable() { return parseError==null; }
	
	public String toString()
	{
		return "label \""+getNameString()+"\" = "+getValueString()+";";
	}
	
	public String getNameString()
	{
		return name;
	}
	
	public String getValueString()
	{
		return label;
	}
}
