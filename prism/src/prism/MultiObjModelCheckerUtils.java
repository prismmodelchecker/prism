//==============================================================================
//
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;

/**
 * Engine-agnostic utilities for parsing and validating multi-objective model checking queries.
 */
public class MultiObjModelCheckerUtils
{
	/**
	 * Extract operator, bound, step-bound and path formula from one operand of a multi-obj
	 * query, and store the results in {@code moQuery}.
	 *
	 * @param exprQuant      The P or R operator defining one objective
	 * @param moQuery        Accumulator for operator/bound/step-bound/formula info
	 * @param constantValues Model constant values for evaluating step bounds
	 * @param origPosition   Position of this operand in the multi(...) argument list
	 */
	public static void extractOperatorAndStepBound(ExpressionQuant exprQuant, MultiObjQuery moQuery,
	                                                Values constantValues, int origPosition) throws PrismException
	{
		ExpressionProb exprProb = null;
		ExpressionReward exprReward = null;

		if (exprQuant instanceof ExpressionProb) {
			exprProb = (ExpressionProb) exprQuant;
		} else if (exprQuant instanceof ExpressionReward) {
			exprReward = (ExpressionReward) exprQuant;
		} else {
			throw new PrismException("Multi-objective properties can only contain P and R operators");
		}

		// Check that the temporal/reward operator is supported, and store step bounds if present
		int stepBound = 0;
		if (exprProb != null) {
			Expression expr = exprProb.getExpression();
			if (expr.isSimplePathFormula() && Expression.isReach(expr)) {
				ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
				if (exprTemp.getLowerBound() != null) {
					throw new PrismException("Lower time bounds are not supported in multi-objective queries");
				}
				stepBound = (exprTemp.getUpperBound() != null)
				            ? exprTemp.getUpperBound().evaluateInt(constantValues)
				            : -1;
			} else {
				if (Expression.containsTemporalTimeBounds(expr)) {
					throw new PrismException("Time bounds in multi-objective queries can only be on F or C operators");
				} else {
					stepBound = -1;
				}
			}
		}
		if (exprReward != null) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) exprReward.getExpression();
			if (exprTemp.getOperator() != ExpressionTemporal.R_C) {
				throw new PrismException("Only the C and C<=k reward operators are currently supported for multi-objective properties (not "
				                         + exprTemp.getOperatorSymbol() + ")");
			}
			stepBound = (exprTemp.getUpperBound() != null)
			            ? exprTemp.getUpperBound().evaluateInt(constantValues)
			            : -1;
		}

		// Get/check/store info about relational operator and bound
		OpRelOpBound opInfo = exprQuant.getRelopBoundInfo(constantValues);
		RelOp relOp = opInfo.getRelOp();
		if (relOp.isStrict()) {
			throw new PrismException("Multi-objective properties can not use strict inequalities on P/R operators");
		}
		Operator op;
		if (relOp == RelOp.MAX) {
			op = (exprProb != null) ? Operator.P_MAX : Operator.R_MAX;
		} else if (relOp == RelOp.GEQ) {
			op = (exprProb != null) ? Operator.P_GE : Operator.R_GE;
		} else if (relOp == RelOp.MIN) {
			op = (exprProb != null) ? Operator.P_MIN : Operator.R_MIN;
		} else if (relOp == RelOp.LEQ) {
			op = (exprProb != null) ? Operator.P_LE : Operator.R_LE;
		} else {
			throw new PrismException("Multi-objective properties can only contain P/R operators with max/min=? or lower/upper probability bounds");
		}
		double p = opInfo.isNumeric() ? -1.0 : opInfo.getBound();
		if (opInfo.isProbabilistic() && opInfo.getRelOp().isUpperBound()) {
			p = 1 - p;
		}
		Expression pathFormula = (exprProb != null) ? exprProb.getExpression() : null;
		moQuery.add(opInfo, op, p, stepBound, origPosition, pathFormula);
	}

	/**
	 * Validate the structure of a multi-objective query after all objectives have been parsed.
	 * Throws a {@link PrismException} if the combination of objectives is not supported.
	 */
	public static void validateQueryStructure(MultiObjQuery moQuery) throws PrismException
	{
		// Allow: 1 numerical + any number of boolean objectives, OR multiple numericals with no booleans
		if (moQuery.numberOfNumerical() > 1
		        && moQuery.numberOfNumerical() < moQuery.numProbObjectives() + moQuery.numRewardObjectives()) {
			throw new PrismException("Cannot combine Pareto queries with constrained objectives");
		}
	}
}
