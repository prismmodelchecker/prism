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

import java.util.Vector;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Find all idents which are variables, replace with ExpressionVar, return result.
 * Also make sure all variable references (e.g. in updates) are valid.
 */
public class FindAllVars extends ASTTraverseModify
{
	private Vector varIdents;
	private Vector varTypes;
	
	public FindAllVars(Vector varIdents, Vector varTypes)
	{
		this.varIdents = varIdents;
		this.varTypes = varTypes;
	}
	
	public void visitPost(Update e) throws PrismLangException
	{
		int i, j, n;
		String s;
		// Check all variables in this update exist.
		// Also store their types for later use.
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			j = varIdents.indexOf(e.getVar(i));
			if (j == -1) {
				s = "Unknown variable \"" + e.getVar(i) + "\" in update";
				throw new PrismLangException(s, e.getVarIdent(i));
			}
			e.setType(i, ((Integer)(varTypes.elementAt(j))).intValue());
		}
	}
	
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		int i;
		// See if identifier corresponds to a variable
		i = varIdents.indexOf(e.getName());
		if (i != -1) {
			// If so, replace it with an ExpressionVar object
			ExpressionVar expr = new ExpressionVar(e.getName(), ((Integer)varTypes.elementAt(i)).intValue());
			expr.setPosition(e);
			return expr;
		}
		// Otherwise, leave it unchanged
		return e;
	}
}

