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

import parser.ast.ExpressionIdent;
import parser.ast.ExpressionVar;
import parser.ast.Update;
import parser.type.Type;
import prism.PrismLangException;

/**
 * Find all references to variables, replace any identifier objects with variable objects,
 * check variables exist and store their index (as defined by the containing ModuleFile).
 */
public class FindAllVars extends ASTTraverseModify
{
	private List<String> varIdents;
	private List<Type> varTypes;
	
	public FindAllVars(List<String> varIdents, List<Type> varTypes)
	{
		this.varIdents = varIdents;
		this.varTypes = varTypes;
	}
	
	// Note that this is done with VisitPost, i.e. after recursively visiting children.
	// This is ok because we can modify rather than create a new object so don't need to return it.
	public void visitPost(Update e) throws PrismLangException
	{
		int i, j, n;
		String s;
		// For each element of update
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			// Check variable exists
			j = varIdents.indexOf(e.getVar(i));
			if (j == -1) {
				s = "Unknown variable \"" + e.getVar(i) + "\" in update";
				throw new PrismLangException(s, e.getVarIdent(i));
			}
			// Store the type
			e.setType(i, varTypes.get(j));
			// And store the variable index
			e.setVarIndex(i, j);
		}
	}
	
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		int i;
		// See if identifier corresponds to a variable
		i = varIdents.indexOf(e.getName());
		if (i != -1) {
			// If so, replace it with an ExpressionVar object
			ExpressionVar expr = new ExpressionVar(e.getName(), varTypes.get(i));
			expr.setPosition(e);
			// Store variable index
			expr.setIndex(i);
			return expr;
		}
		// Otherwise, leave it unchanged
		return e;
	}
	
	// Also re-compute info for ExpressionVar objects in case variable indices have changed
	public Object visit(ExpressionVar e) throws PrismLangException
	{
		int i;
		// See if identifier corresponds to a variable
		i = varIdents.indexOf(e.getName());
		if (i != -1) {
			// If so, set the index
			e.setIndex(i);
			return e;
		}
		// Otherwise, there is a problem
		throw new PrismLangException("Unknown variable " + e.getName() + " in ExpressionVar object", e);
	}
}

