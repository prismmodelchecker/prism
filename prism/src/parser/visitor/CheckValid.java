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
 * Perform any required semantic checks. Optionally pass in parent ModulesFile
 * and PropertiesFile for some additional checks (or leave null);
 */
public class CheckValid extends ASTTraverse
{
	private int modelType = 0;

	public CheckValid(int modelType)
	{
		this.modelType = modelType;
	}

	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		if (modelType == ModulesFile.NONDETERMINISTIC) {
			throw new PrismLangException("The S operator cannot be used for MDPs");
		}
	}

	public void visitPost(PathExpressionTemporal e) throws PrismLangException
	{
		if (e.getOperator() == PathExpressionTemporal.R_S) {
			if (modelType == ModulesFile.NONDETERMINISTIC) {
				throw new PrismLangException("Steady-state reward properties cannot be used for MDPs");
			}
		}
		if (e.getLowerBound() != null) {
			if (modelType == ModulesFile.PROBABILISTIC) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be of the form \"<=k\" for DTMCs");
			}
			if (modelType == ModulesFile.NONDETERMINISTIC) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be of the form \"<=k\" for MDPs");
			}
		}
		if (e.getUpperBound() != null && e.getUpperBound().getType() != Expression.INT) {
			if (modelType == ModulesFile.PROBABILISTIC) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be integers for DTMCs");
			}
			if (modelType == ModulesFile.NONDETERMINISTIC) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be integers for MDPs");
			}
		}
	}
	
	public void visitPost(PathExpressionLogical e) throws PrismLangException
	{
		throw new PrismLangException("Logical operators for paths are not yet supported");
	}
}
