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
 * Check for type-correctness and compute type.
 */
public class TypeCheck extends ASTTraverse
{
	public TypeCheck()
	{
	}

	public void visitPost(ModulesFile e) throws PrismLangException
	{
		if (e.getInitialStates() != null && e.getInitialStates().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: Initial states definition must be Boolean", e
					.getInitialStates());
		}
	}

	public void visitPost(FormulaList e) throws PrismLangException
	{
		// Formulas are defined at the text level and are type checked after
		// substitutions have been applied
	}

	public void visitPost(LabelList e) throws PrismLangException
	{
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getLabel(i).getType() != Expression.BOOLEAN) {
				throw new PrismLangException("Type error: Label \"" + e.getLabelName(i) + "\" is not Boolean", e
						.getLabel(i));
			}
		}
	}

	public void visitPost(ConstantList e) throws PrismLangException
	{
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getConstant(i) != null && !Expression.canAssignTypes(e.getConstantType(i), e.getConstant(i).getType())) {
				throw new PrismLangException("Type mismatch in definition of constant \"" + e.getConstantName(i)
						+ "\"", e.getConstant(i));
			}
		}
	}

	public void visitPost(Declaration e) throws PrismLangException
	{
		if (e.getLow() != null && !Expression.canAssignTypes(e.getType(), e.getLow().getType())) {
			throw new PrismLangException("Type error: Minimum value of variable \"" + e.getName()
					+ "\" does not match", e.getLow());
		}
		if (e.getHigh() != null && !Expression.canAssignTypes(e.getType(), e.getHigh().getType())) {
			throw new PrismLangException("Type error: Maximum value of variable \"" + e.getName()
					+ "\" does not match", e.getHigh());
		}
		if (e.getStart() != null && !Expression.canAssignTypes(e.getType(), e.getStart().getType())) {
			throw new PrismLangException("Type error: Initial value of variable \"" + e.getName()
					+ "\" does not match", e.getStart());
		}
	}

	public void visitPost(Command e) throws PrismLangException
	{
		if (e.getGuard().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: Guard is not Boolean", e.getGuard());
		}
	}

	public void visitPost(Updates e) throws PrismLangException
	{
		int i, n;
		n = e.getNumUpdates();
		for (i = 0; i < n; i++) {
			if (e.getProbability(i) != null) if (e.getProbability(i).getType() == Expression.BOOLEAN) {
				throw new PrismLangException("Type error: Update probability/rate cannot be Boolean", e
						.getProbability(i));
			}
		}
	}

	public void visitPost(Update e) throws PrismLangException
	{
		int i, n;
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			if (!Expression.canAssignTypes(e.getType(i), e.getExpression(i).getType())) {
				throw new PrismLangException("Type error in update to variable \"" + e.getVar(i) + "\"", e
						.getExpression(i));
			}
		}
	}

	public void visitPost(RewardStructItem e) throws PrismLangException
	{
		if (e.getStates().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error in reward struct item: guard must be Boolean", e.getStates());
		}
		if (!Expression.canAssignTypes(Expression.DOUBLE, e.getReward().getType())) {
			throw new PrismLangException("Type error in reward struct item: value must be an int or double", e
					.getReward());
		}
	}

	public void visitPost(ExpressionITE e) throws PrismLangException
	{
		int t1 = e.getOperand1().getType();
		int t2 = e.getOperand2().getType();
		int t3 = e.getOperand3().getType();

		if (t1 != Expression.BOOLEAN) {
			throw new PrismLangException("Type error:  condition of ? operator is not Boolean", e.getOperand1());
		}
		if (!(Expression.canAssignTypes(t2, t3) || Expression.canAssignTypes(t3, t2))) {
			throw new PrismLangException("Type error: types for then/else operands of ? operator must match", e);
		}

		if (t2 == Expression.BOOLEAN)
			e.setType(Expression.BOOLEAN);
		else if (t2 == Expression.INT && t3 == Expression.INT)
			e.setType(Expression.INT);
		else
			e.setType(Expression.DOUBLE);
	}

	public void visitPost(ExpressionBinaryOp e) throws PrismLangException
	{
		int t1 = e.getOperand1().getType();
		int t2 = e.getOperand2().getType();

		switch (e.getOperator()) {
		case ExpressionBinaryOp.IMPLIES:
		case ExpressionBinaryOp.OR:
		case ExpressionBinaryOp.AND:
			if (t1 != Expression.BOOLEAN) {
				throw new PrismLangException("Type error: " + e.getOperatorSymbol()
						+ " applied to non-Boolean expression", e.getOperand1());
			}
			if (t2 != Expression.BOOLEAN) {
				throw new PrismLangException("Type error: " + e.getOperatorSymbol()
						+ " applied to non-Boolean expression", e.getOperand2());
			}
			e.setType(Expression.BOOLEAN);
			break;
		case ExpressionBinaryOp.EQ:
		case ExpressionBinaryOp.NE:
			if ((t1 == Expression.BOOLEAN && t2 != Expression.BOOLEAN)
					|| (t2 == Expression.BOOLEAN && t1 != Expression.BOOLEAN)) {
				throw new PrismLangException("Type error: Can't compare Booleans with ints/doubles", e);
			}
			e.setType(Expression.BOOLEAN);
			break;
		case ExpressionBinaryOp.GT:
		case ExpressionBinaryOp.GE:
		case ExpressionBinaryOp.LT:
		case ExpressionBinaryOp.LE:
			if (!((t1 == Expression.INT || t1 == Expression.DOUBLE) && (t2 == Expression.INT || t2 == Expression.DOUBLE))) {
				throw new PrismLangException("Type error: " + e.getOperatorSymbol()
						+ " can only compare ints or doubles", e);
			}
			e.setType(Expression.BOOLEAN);
			break;
		case ExpressionBinaryOp.PLUS:
		case ExpressionBinaryOp.MINUS:
		case ExpressionBinaryOp.TIMES:
			if (t1 == Expression.BOOLEAN) {
				throw new PrismLangException(
						"Type error: " + e.getOperatorSymbol() + " cannot be applied to Boolean", e.getOperand1());
			}
			if (t2 == Expression.BOOLEAN) {
				throw new PrismLangException(
						"Type error: " + e.getOperatorSymbol() + " cannot be applied to Boolean", e.getOperand2());
			}
			e.setType(t1 == Expression.DOUBLE || t2 == Expression.DOUBLE ? Expression.DOUBLE : Expression.INT);
			break;
		case ExpressionBinaryOp.DIVIDE:
			if (t1 == Expression.BOOLEAN) {
				throw new PrismLangException(
						"Type error: " + e.getOperatorSymbol() + " cannot be applied to Boolean", e.getOperand1());
			}
			if (t2 == Expression.BOOLEAN) {
				throw new PrismLangException(
						"Type error: " + e.getOperatorSymbol() + " cannot be applied to Boolean", e.getOperand2());
			}
			e.setType(Expression.DOUBLE);
			break;
		}
	}

	public void visitPost(ExpressionUnaryOp e) throws PrismLangException
	{
		int t = e.getOperand().getType();

		switch (e.getOperator()) {
		case ExpressionUnaryOp.NOT:
			if (t != Expression.BOOLEAN) {
				throw new PrismLangException("Type error: " + e.getOperatorSymbol()
						+ " applied to non-Boolean expression", e.getOperand());
			}
			e.setType(Expression.BOOLEAN);
			break;
		case ExpressionUnaryOp.MINUS:
			if (!(t == Expression.INT || t == Expression.DOUBLE)) {
				throw new PrismLangException(
						"Type error: " + e.getOperatorSymbol() + " cannot be applied to Boolean", e.getOperand());
			}
			e.setType(t);
			break;
		case ExpressionUnaryOp.PARENTH:
			e.setType(t);
			break;
		}
	}

	public void visitPost(ExpressionFunc e) throws PrismLangException
	{
		int i, n, types[];

		// Get types of operands
		n = e.getNumOperands();
		types = new int[n];
		for (i = 0; i < n; i++) {
			types[i] = e.getOperand(i).getType();
		}

		// Check types of operands are ok
		switch (e.getNameCode()) {
		case ExpressionFunc.MIN:
		case ExpressionFunc.MAX:
		case ExpressionFunc.FLOOR:
		case ExpressionFunc.CEIL:
		case ExpressionFunc.POW:
		case ExpressionFunc.LOG:
			// All operands must be ints or doubles
			for (i = 0; i < n; i++) {
				if (types[i] == Expression.BOOLEAN) {
					throw new PrismLangException(
							"Type error: Boolean argument not allowed as argument to function \"" + e.getName() + "\"",
							e.getOperand(i));
				}
			}
			break;
		case ExpressionFunc.MOD:
			// All operands must be ints
			for (i = 0; i < n; i++) {
				if (types[i] != Expression.INT) {
					throw new PrismLangException("Type error: non-integer argument to  function \"" + e.getName()
							+ "\"", e.getOperand(i));
				}
			}
			break;
		default:
			throw new PrismLangException("Cannot type check unknown function", e);
		}

		// Determine type of this function
		switch (e.getNameCode()) {
		case ExpressionFunc.MIN:
		case ExpressionFunc.MAX:
			// int if all ints, double otherwise
			for (i = 0; i < n; i++) {
				if (types[i] == Expression.DOUBLE) {
					e.setType(Expression.DOUBLE);
					break;
				}
			}
			e.setType(Expression.INT);
			break;
		case ExpressionFunc.FLOOR:
		case ExpressionFunc.CEIL:
		case ExpressionFunc.MOD:
			// Resulting type is always int
			e.setType(Expression.INT);
			break;
		case ExpressionFunc.POW:
		case ExpressionFunc.LOG:
			// Resulting type is always double
			e.setType(Expression.DOUBLE);
			break;
		}
	}

	public void visitPost(ExpressionIdent e) throws PrismLangException
	{
		// Should never happpen
		throw new PrismLangException("Cannot determine type of unknown identifier", e);
	}

	public void visitPost(ExpressionLiteral e) throws PrismLangException
	{
		// Type already known
	}

	public void visitPost(ExpressionConstant e) throws PrismLangException
	{
		// Type already known
	}

	public void visitPost(ExpressionFormula e) throws PrismLangException
	{
		// Should never happpen
		throw new PrismLangException("Cannot determine type of formulas", e);
	}

	public void visitPost(ExpressionVar e) throws PrismLangException
	{
		// Type already known
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		if (e.getProb() != null && !Expression.canAssignTypes(Expression.DOUBLE, e.getProb().getType())) {
			throw new PrismLangException("Type error: P operator probability bound is not a double", e.getProb());
		}
		if (e.getFilter() != null && e.getFilter().getExpression().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: P operator filter is not a Boolean", e.getFilter().getExpression());
		}
		e.setType(e.getProb() == null ? Expression.DOUBLE : Expression.BOOLEAN);
	}

	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		if (e.getRewardStructIndex() != null && e.getRewardStructIndex() instanceof Expression) {
			Expression rsi = (Expression) e.getRewardStructIndex();
			if (rsi.getType() != Expression.INT) {
				throw new PrismLangException("Type error: Reward structure index must be string or integer", rsi);
			}
		}
		if (e.getReward() != null && !Expression.canAssignTypes(Expression.DOUBLE, e.getReward().getType())) {
			throw new PrismLangException("Type error: R operator reward bound is not a double", e.getReward());
		}
		if (e.getFilter() != null && e.getFilter().getExpression().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: R operator filter is not a Boolean", e.getFilter().getExpression());
		}
		e.setType(e.getReward() == null ? Expression.DOUBLE : Expression.BOOLEAN);
	}

	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		if (e.getProb() != null && !Expression.canAssignTypes(Expression.DOUBLE, e.getProb().getType())) {
			throw new PrismLangException("Type error: S operator probability bound is not a double", e.getProb());
		}
		if (e.getFilter() != null && e.getFilter().getExpression().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: P operator filter is not a Boolean", e.getFilter().getExpression());
		}
		e.setType(e.getProb() == null ? Expression.DOUBLE : Expression.BOOLEAN);
	}

	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		e.setType(Expression.BOOLEAN);
	}

	public void visitPost(PathExpressionTemporal e) throws PrismLangException
	{
		if (e.getOperand1() != null && e.getOperand1().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: Argument of " + e.getOperatorSymbol()
					+ " operator is not Boolean", e.getOperand1());
		}
		if (e.getOperand2() != null && e.getOperand2().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: Argument of " + e.getOperatorSymbol()
					+ " operator is not Boolean", e.getOperand2());
		}
		if (e.getLowerBound() != null && !Expression.canAssignTypes(Expression.DOUBLE, e.getLowerBound().getType())) {
			throw new PrismLangException("Type error: Lower bound in " + e.getOperatorSymbol()
					+ " operator must be an int or double", e.getLowerBound());
		}
		if (e.getUpperBound() != null && !Expression.canAssignTypes(Expression.DOUBLE, e.getUpperBound().getType())) {
			throw new PrismLangException("Type error: Upper bound in " + e.getOperatorSymbol()
					+ " operator must be an int or double", e.getUpperBound());
		}
		switch (e.getOperator()) {
		case PathExpressionTemporal.P_X:
		case PathExpressionTemporal.P_U:
		case PathExpressionTemporal.P_F:
		case PathExpressionTemporal.P_G:
			e.setType(Expression.BOOLEAN);
			break;
		case PathExpressionTemporal.R_C:
		case PathExpressionTemporal.R_I:
		case PathExpressionTemporal.R_F:
		case PathExpressionTemporal.R_S:
			e.setType(Expression.DOUBLE);
			break;
		}
	}

	public void visitPost(PathExpressionLogical e) throws PrismLangException
	{
		if (e.getOperand1() != null && e.getOperand1().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: Argument of " + e.getOperatorSymbol()
					+ " operator is not Boolean", e.getOperand1());
		}
		if (e.getOperand2() != null && e.getOperand2().getType() != Expression.BOOLEAN) {
			throw new PrismLangException("Type error: Argument of " + e.getOperatorSymbol()
					+ " operator is not Boolean", e.getOperand2());
		}
		e.setType(Expression.BOOLEAN);
	}
	
	public void visitPost(PathExpressionExpr e) throws PrismLangException
	{
		int t = e.getExpression().getType();
		if (t != Expression.BOOLEAN) {
			throw new PrismLangException("Type error expressions in path operator is not Boolean", e.getExpression());
		}
		e.setType(Expression.BOOLEAN);
	}
}
