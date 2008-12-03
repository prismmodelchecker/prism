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

package prism;

import jdd.*;
import parser.ast.*;

/*
 * Non probabilistic model checker, initially for CTL.
 */
public class NonProbModelChecker extends StateModelChecker
{
	// Constructor

	public NonProbModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);
	}

	// -----------------------------------------------------------------------------------
	// Check a property, i.e. an expression
	// -----------------------------------------------------------------------------------

	// Check expression (recursive)

	public StateProbs checkExpression(Expression expr) throws PrismException
	{
		StateProbs res;

		// E operator
		if (expr instanceof ExpressionExists) {
			res = checkExpressionExists(((ExpressionExists) expr).getExpression());
		}
		// A operator
		if (expr instanceof ExpressionForAll) {
			res = checkExpressionForAll(((ExpressionForAll) expr).getExpression());
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(expr);
		}

		// Filter out non-reachable states from solution
		// (only necessary for symbolically stored vectors)
		if (res instanceof StateProbsMTBDD)
			res.filter(reach);

		return res;
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	// E operator

	protected StateProbs checkExpressionExists(Expression expr) throws PrismException
	{
		throw new PrismException("CTL model checking is not yet supported");
	}
	
	// A operator

	protected StateProbs checkExpressionForAll(Expression expr) throws PrismException
	{
		throw new PrismException("CTL model checking is not yet supported");
	}
}

// ------------------------------------------------------------------------------
