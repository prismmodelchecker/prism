//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import parser.ast.ExpressionFormula;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionIdent;
import prism.PrismLangException;

/**
 * Perform any required semantic checks. These are just simple checks on expressions, mostly.
 * For semantic checks on models and properties, specifically, see:
 * {@link parser.visitor.ModulesFileSemanticCheck} and {@link parser.visitor.PropertiesSemanticCheck}. 
 * These checks are done *before* any undefined constants have been defined.
 */
public class SemanticCheck extends ASTTraverse
{
	public SemanticCheck()
	{
	}

	public void visitPost(ExpressionFunc e) throws PrismLangException
	{
		// Check function name is valid
		if (e.getNameCode() == -1) {
			throw new PrismLangException("Unknown function \"" + e.getName() + "\"", e);
		}
		// Check num arguments
		if (e.getNumOperands() < e.getMinArity()) {
			throw new PrismLangException("Not enough arguments to \"" + e.getName() + "\" function", e);
		}
		if (e.getMaxArity() != -1 && e.getNumOperands() > e.getMaxArity()) {
			throw new PrismLangException("Too many arguments to \"" + e.getName() + "\" function", e);
		}
	}

	public void visitPost(ExpressionIdent e) throws PrismLangException
	{
		// By the time the expression is checked, this should
		// have been converted to an ExpressionVar/ExpressionConstant/...
		throw new PrismLangException("Undeclared identifier", e);
	}

	public void visitPost(ExpressionFormula e) throws PrismLangException
	{
		// This should have been defined or expanded by now
		if (e.getDefinition() == null)
			throw new PrismLangException("Unexpanded formula", e);
	}
}
