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

import parser.ast.*;
import prism.PrismLangException;

/**
 * Expand all formulas, return result.
 */
public class ExpandFormulas extends ASTTraverseModify
{
	private FormulaList formulaList;
	
	public ExpandFormulas(FormulaList formulaList)
	{
		this.formulaList = formulaList;
	}
	
	public Object visit(ExpressionFormula e) throws PrismLangException
	{
		int i;
		Expression expr;
		
		// See if identifier corresponds to a formula
		i = formulaList.getFormulaIndex(e.getName());
		if (i != -1) {
			// If so, replace it with the corresponding expression
			expr = formulaList.getFormula(i);
			// But also recursively expand that
			// (don't clone it to avoid duplication of work)
			expr = (Expression)expr.expandFormulas(formulaList);
			// Put in brackets so precedence is preserved
			// (for display purposes only; in case of re-parse)
			expr = Expression.Parenth(expr);
			// Return replacement expression
			return expr;
		}
		
		// Couldn't find definition - leave unchanged.
		return e;
	}
}

