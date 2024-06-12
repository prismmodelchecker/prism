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

package parser.visitor;

import java.util.List;

import parser.ast.*;
import parser.type.Type;
import prism.PrismLangException;

/**
 * Find all idents which are constants, replace with ExpressionConstant, return result.
 */
public class FindAllConstants extends ASTTraverseModify
{
	// Either:
	private ConstantList constantList;
	// Or:
	private List<String> constIdents;
	private List<Type> constTypes;
	
	public FindAllConstants(ConstantList constantList)
	{
		this.constantList = constantList;
	}
	
	public FindAllConstants(List<String> constIdents, List<Type> constTypes)
	{
		this.constIdents = constIdents;
		this.constTypes = constTypes;
	}
	
	private int getConstantIndex(String name)
	{
		if (constantList != null) {
			return constantList.getConstantIndex(name);
		} else {
			return constIdents.indexOf(name);
		}
	}
	
	private Type getConstantType(int i)
	{
		if (constantList != null) {
			return constantList.getConstantType(i);
		} else {
			return constTypes.get(i);
		}
	}
	
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		// See if identifier corresponds to a constant
		int i = getConstantIndex(e.getName());
		if (i != -1) {
			// If so, replace it with an ExpressionConstant object
			ExpressionConstant expr = new ExpressionConstant(e.getName(), getConstantType(i));
			expr.setPosition(e);
			return expr;
		}
		// Otherwise, leave it unchanged
		return e;
	}
}

