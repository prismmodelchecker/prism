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

package explicit;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionExists;
import parser.ast.ExpressionForAll;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Explicit-state, non-probabilistic model checker.
 */
public class NonProbModelChecker extends StateModelChecker
{
	/**
	 * Create a new NonProbModelChecker, inherit basic state from parent (unless null).
	 */
	public NonProbModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}
	
	@Override
	public StateValues checkExpression(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		StateValues res;

		// E operator
		if (expr instanceof ExpressionExists) {
			throw new PrismException("CTL model checking is not yet supported by the explicit engine");
		}
		// A operator
		else if (expr instanceof ExpressionForAll) {
			throw new PrismException("CTL model checking is not yet supported by the explicit engine");
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(model, expr, statesOfInterest);
		}

		return res;
	}
}

