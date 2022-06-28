//==============================================================================
//	
//	Copyright (c) 2021-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package pta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import parser.ParserUtils;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionVar;
import parser.type.TypeClock;
import parser.type.TypeInt;
import prism.PrismLangException;

public class PTAUtils
{
	/**
	 * Check whether a PRISM expression (over clock variables) is a "simple" clock constraint, i.e. of the form
	 * x~c or x~y where x and y are clocks, c is an integer-valued expression and ~ is one of <, <=, >=, >, =.
	 * Throws an explanatory exception if not.
	 * @param expr: The expression to be checked.
	 */
	public static void checkIsSimpleClockConstraint(Expression expr) throws PrismLangException
	{
		ExpressionBinaryOp exprRelOp;
		Expression expr1, expr2;
		int op, clocks = 0;

		// Check is rel op
		if (!Expression.isRelOp(expr))
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		// Split into parts
		exprRelOp = (ExpressionBinaryOp) expr;
		op = exprRelOp.getOperator();
		expr1 = exprRelOp.getOperand1();
		expr2 = exprRelOp.getOperand2();
		// Check operator is of allowed type
		if (!ExpressionBinaryOp.isRelOp(op))
			throw new PrismLangException("Can't use operator " + exprRelOp.getOperatorSymbol() + " in clock constraint \"" + expr + "\"", expr);
		if (op == ExpressionBinaryOp.NE)
			throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
		// LHS
		if (expr1.getType() instanceof TypeClock) {
			if (!(expr1 instanceof ExpressionVar)) {
				throw new PrismLangException("Invalid clock expression \"" + expr1 + "\"", expr1);
			}
			clocks++;
		} else if (expr1.getType() instanceof TypeInt) {
			if (!expr1.isConstant()) {
				throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
			}
		} else {
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		}
		// RHS
		if (expr2.getType() instanceof TypeClock) {
			if (!(expr2 instanceof ExpressionVar)) {
				throw new PrismLangException("Invalid clock expression \"" + expr2 + "\"", expr2);
			}
			clocks++;
		} else if (expr2.getType() instanceof TypeInt) {
			if (!expr2.isConstant()) {
				throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
			}
		} else {
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		}
		// Should be at least one clock
		if (clocks == 0)
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
	}

	/**
	 * Convert a PRISM expression representing a (simple) clock constraint into
	 * the Constraint data structures used in the pta package.
	 * Actually creates a list of constraints (since e.g. x=c maps to multiple constraints) 
	 * @param expr: The expression to be converted.
	 * @param constantValues Values for constants appearing in the expression 
	 * @param pta: The PTA for which this constraint will be used. 
	 */
	public static List<Constraint> exprToConstraint(Expression expr, Values constantValues, PTA pta) throws PrismLangException
	{
		ExpressionBinaryOp exprRelOp;
		Expression expr1, expr2;
		int x, y, v;
		List<Constraint> res = new ArrayList<Constraint>();

		// Check is rel op and split into parts
		if (!Expression.isRelOp(expr))
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		exprRelOp = (ExpressionBinaryOp) expr;
		expr1 = exprRelOp.getOperand1();
		expr2 = exprRelOp.getOperand2();
		// 3 cases...
		if (expr1.getType() instanceof TypeClock) {
			// Comparison of two clocks (x ~ y)
			if (expr2.getType() instanceof TypeClock) {
				x = pta.getClockIndex(((ExpressionVar) expr1).getName());
				if (x < 0)
					throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr1).getName() + "\"", expr);
				y = pta.getClockIndex(((ExpressionVar) expr2).getName());
				if (y < 0)
					throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr2).getName() + "\"", expr);
				switch (exprRelOp.getOperator()) {
				case ExpressionBinaryOp.EQ:
					res.add(Constraint.buildXGeqY(x, y));
					res.add(Constraint.buildXLeqY(x, y));
					break;
				case ExpressionBinaryOp.NE:
					throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
				case ExpressionBinaryOp.GT:
					res.add(Constraint.buildXGtY(x, y));
					break;
				case ExpressionBinaryOp.GE:
					res.add(Constraint.buildXGeqY(x, y));
					break;
				case ExpressionBinaryOp.LT:
					res.add(Constraint.buildXLtY(x, y));
					break;
				case ExpressionBinaryOp.LE:
					res.add(Constraint.buildXLeqY(x, y));
					break;
				}
				return res;
			}
			// Comparison of clock and integer (x ~ v)
			else {
				x = pta.getClockIndex(((ExpressionVar) expr1).getName());
				if (x < 0)
					throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr1).getName() + "\"", expr);
				v = expr2.evaluateInt(constantValues);
				switch (exprRelOp.getOperator()) {
				case ExpressionBinaryOp.EQ:
					res.add(Constraint.buildGeq(x, v));
					res.add(Constraint.buildLeq(x, v));
					break;
				case ExpressionBinaryOp.NE:
					throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
				case ExpressionBinaryOp.GT:
					res.add(Constraint.buildGt(x, v));
					break;
				case ExpressionBinaryOp.GE:
					res.add(Constraint.buildGeq(x, v));
					break;
				case ExpressionBinaryOp.LT:
					res.add(Constraint.buildLt(x, v));
					break;
				case ExpressionBinaryOp.LE:
					res.add(Constraint.buildLeq(x, v));
					break;
				}
				return res;
			}
		}
		// Comparison of integer and clock (v ~ x)
		else if (expr2.getType() instanceof TypeClock) {
			x = pta.getClockIndex(((ExpressionVar) expr2).getName());
			if (x < 0)
				throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr2).getName() + "\"", expr);
			v = expr1.evaluateInt(constantValues);
			switch (exprRelOp.getOperator()) {
			case ExpressionBinaryOp.EQ:
				res.add(Constraint.buildGeq(x, v));
				res.add(Constraint.buildLeq(x, v));
				break;
			case ExpressionBinaryOp.NE:
				throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
			case ExpressionBinaryOp.GT:
				res.add(Constraint.buildLt(x, v));
				break;
			case ExpressionBinaryOp.GE:
				res.add(Constraint.buildLeq(x, v));
				break;
			case ExpressionBinaryOp.LT:
				res.add(Constraint.buildGt(x, v));
				break;
			case ExpressionBinaryOp.LE:
				res.add(Constraint.buildGeq(x, v));
				break;
			}
			return res;
		}
		throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
	}
	
	/**
	 * Convert a PRISM expression representing a conjunction of (simple) clock constraints
	 * into Constraint data structures used in the pta package and pass to a consumer.
	 * An exception is thrown if any conjunct is not a simple clock constraints (or true/false).
	 * Since this is a conjunction, any "true" constraints are omitted. 
	 * @param expr: The expression to be converted.
	 * @param constantValues Values for constants appearing in the expression 
	 * @param pta: The PTA for which this constraint will be used.
	 * @param consumer: The consumer to pass the Constraints to 
	 */
	public static void exprConjToConstraintConsumer(Expression expr, Values constantValues, PTA pta, Consumer<Constraint> consumer) throws PrismLangException
	{
		List<Expression> exprs = ParserUtils.splitConjunction(expr);
		for (Expression ex : exprs) {
			if (!(Expression.isTrue(ex) || Expression.isFalse(ex))) {
				checkIsSimpleClockConstraint(ex);
			}
		}
		for (Expression ex : exprs) {
			if (!Expression.isTrue(ex)) {
				for (Constraint c : PTAUtils.exprToConstraint(ex, constantValues, pta)) {
					consumer.accept(c);
				}
			}
		}
	}
	
	/**
	 * Convert a PRISM expression representing a conjunction of (simple) clock constraints
	 * into a list of Constraint data structures used in the pta package.
	 * An exception is thrown if any conjunct is not a simple clock constraints (or true/false).
	 * Since this is a conjunction, any "true" constraints are omitted. 
	 * @param expr: The expression to be converted.
	 * @param constantValues Values for constants appearing in the expression 
	 * @param pta: The PTA for which this constraint will be used.
	 */
	public static List<Constraint> exprConjToConstraintList(Expression expr, Values constantValues, PTA pta) throws PrismLangException
	{
		List<Constraint> cons = new ArrayList<>();
		exprConjToConstraintConsumer(expr, constantValues, pta, c -> cons.add(c));
		return cons;
	}
	
	/**
	 * TODO
	 * @param clockValues
	 * @param pta
	 * @return
	 */
	public static Zone regionForPoint(double[] clockValues, PTA pta)
	{
		if (clockValues[0] >=2 && clockValues[0] <=2.2) {
			System.out.println("hmm");
		}
		int numClocks = pta.getNumClocks();
		Zone z = DBM.createTrue(pta);
		for (int i = 1; i <= numClocks; i++) {
			double x = clockValues[i - 1];
			int xF = (int) Math.floor(x);
			int xC = (int) Math.ceil(x);
			if (x == xF) {
				z.addConstraint(Constraint.buildGeq(i, xF));
			} else {
				z.addConstraint(Constraint.buildGt(i, xF));
			}
			if (x == xC) {
				z.addConstraint(Constraint.buildLeq(i, xC));
			} else {
				z.addConstraint(Constraint.buildLt(i, xC));
			}

			for (int j = 1; j <= numClocks; j++) {
				if (i != j) {
					double xy = x - clockValues[j - 1];
					int xyF = (int) Math.floor(xy);
					int xyC = (int) Math.ceil(xy);
					// doublesareclose?
					if (xy == xyF) {
						z.addConstraint(j, i, DB.createLeq(-xyF));
					} else {
						z.addConstraint(j, i, DB.createLt(-xyF));
					}
					if (xy == xyC) {
						z.addConstraint(i, j, DB.createLeq(xyC));
					} else {
						z.addConstraint(i, j, DB.createLt(xyC));
					}
				}
			}
		}
		return z;
	}
}
