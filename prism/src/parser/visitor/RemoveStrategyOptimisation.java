//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionStrategy;
import parser.ast.RelOp;
import prism.PrismLangException;

/**
 * Visitor to remove the optimisation over strategies from a property,
 * e.g., for checking it on a model induced by a strategy, with no nondeterminism left:
 * min/max are dropped from P/R/S operators (e.g. {@code Pmax=?} becomes {@code P=?}),
 * as are strategy quantifiers ({@code <<>>}, {@code [[]]}), including around a combination
 * of objectives, which are then checked for the single strategy. Bounds are left unchanged.
 * For models with uncertainty (e.g., interval models), the optimisation over
 * the uncertainty is kept (e.g. {@code Pmaxmin=?} becomes {@code Pmin=?}).
 */
public class RemoveStrategyOptimisation extends ASTTraverseModify
{
	/** Keep the optimisation over uncertainty? */
	private boolean uncertain;

	/**
	 * Constructor.
	 * @param uncertain Keep the optimisation over uncertainty (e.g., for interval models)?
	 */
	public RemoveStrategyOptimisation(boolean uncertain)
	{
		this.uncertain = uncertain;
	}

	@Override
	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		e.setRelOp(removeStrategyOptimisation(e.getRelOp()));
	}

	@Override
	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		e.setRelOp(removeStrategyOptimisation(e.getRelOp()));
	}

	@Override
	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		e.setRelOp(removeStrategyOptimisation(e.getRelOp()));
	}

	@Override
	public Object visit(ExpressionStrategy e) throws PrismLangException
	{
		super.visit(e);
		if (e.getNumOperands() != 1) {
			throw new PrismLangException("Strategy quantifiers with multiple operands are not supported here", e);
		}
		return e.getOperand(0);
	}

	/**
	 * Remove the optimisation over strategies from a relational operator.
	 */
	private RelOp removeStrategyOptimisation(RelOp relOp)
	{
		if (relOp == null) {
			return null;
		}
		switch (relOp) {
		case MIN:
		case MINMIN:
		case MAXMIN:
			return uncertain ? RelOp.MIN : RelOp.EQ;
		case MAX:
		case MAXMAX:
		case MINMAX:
			return uncertain ? RelOp.MAX : RelOp.EQ;
		default:
			return relOp;
		}
	}
}
