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

package parser.ast;

import parser.*;
import parser.visitor.*;
import prism.ModelType;
import prism.PrismLangException;
import parser.type.*;

// Abstract class for PRISM language expressions

public abstract class Expression extends ASTElement
{
	/**
	 * Is this expression constant?
	 */
	public abstract boolean isConstant();

	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public abstract Object evaluate(EvaluateContext ec) throws PrismLangException;

	/**
	  * Get "name" of the result of this expression (used for y-axis of any graphs plotted)
	  */
	public String getResultName()
	{
		return "Result";
	}

	/**
	 * Does this expression equate to a single value, rather than a different value for all states,
	 * when evaluated during model checking?
	 */
	public abstract boolean returnsSingleValue();
	
	// Overrided version of deepCopy() from superclass ASTElement (to reduce casting).

	/**
	 * Perform a deep copy.
	 */
	public abstract Expression deepCopy();

	// Utility methods:

	/**
	 * Check expression (property) for validity with respect to a particular model type
	 * (i.e. whether not it is a property that can be model checked for that model type).
	 */
	public void checkValid(ModelType modelType) throws PrismLangException
	{
		CheckValid visitor = new CheckValid(modelType);
		accept(visitor);
	}

	/**
	 * Determine whether expression is a valid "simple" path formula , i.e. a formula
	 * that could occur in the P operator of a PCTL/CSL formula (not LTL, PCTL*).
	 * This is defined as a single instance of a temporal operator (X, U, F, etc.),
	 * possibly negated. Strictly speaking, negations are not allowed in PCTL/CSL
	 * but they can always be converted to a dual formula which is.   
	 */
	public boolean isSimplePathFormula() throws PrismLangException
	{
		// One (or more) top-level negations is allowed.
		// Top-level parentheses also OK.
		if (this instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp expr = (ExpressionUnaryOp) this;
			int op = expr.getOperator();
			if (op == ExpressionUnaryOp.NOT || op == ExpressionUnaryOp.PARENTH) {
				return expr.getOperand().isSimplePathFormula();
			} else {
				return false;
			}
		}
		// Otherwise, must be a temporal operator.
		else if (this instanceof ExpressionTemporal) {
			ExpressionTemporal expr = (ExpressionTemporal) this;
			// And children, if present, must be state (not path) formulas
			if (expr.getOperand1() != null && !(expr.getOperand1().getType() instanceof TypeBool)) {
				return false;
			}
			if (expr.getOperand2() != null && !(expr.getOperand2().getType() instanceof TypeBool)) {
				return false;
			}
			return true;
		}
		// Default: false.
		return false;
	}

	/**
	 * Convert a property expression (an LTL formula) into the classes used by
	 * the jltl2ba (and jltl2dstar) libraries.
	 */
	public jltl2ba.SimpleLTL convertForJltl2ba() throws PrismLangException
	{
		ConvertForJltl2ba visitor = new ConvertForJltl2ba();
		accept(visitor);
		return visitor.getFormula(this);
	}

	/**
	 * Evaluate this expression, based on Values for constants/variables; return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluate(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression, based on a State object, i.e. array of variable values; return result.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public Object evaluate(State state) throws PrismLangException
	{
		return evaluate(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression, based on a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 */
	public Object evaluate(State substate, int[] varMap) throws PrismLangException
	{
		return evaluate(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression, using no constant or variable values.
	 */
	public Object evaluate() throws PrismLangException
	{
		return evaluate(new EvaluateContextValues(null, null));
	}

	/**
	 * Evaluate this expression as an integer.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0/1).
	 */
	public int evaluateInt(EvaluateContext ec) throws PrismLangException
	{
		Object o = evaluate(ec);
		if (o instanceof Integer) {
			return ((Integer) o).intValue();
		}
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue() ? 1 : 0;
		}
		throw new PrismLangException("Cannot evaluate to an integer", this);
	}

	/**
	 * Evaluate this expression as an integer.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0/1).
	 */
	public int evaluateInt(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression as an integer.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0/1).
	 */
	public int evaluateInt(State state) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression as an integer.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0/1).
	 */
	public int evaluateInt(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression as an integer.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0/1).
	 */
	public int evaluateInt() throws PrismLangException
	{
		return evaluateInt(new EvaluateContextValues(null, null));
	}

	/**
	 * Evaluate this expression as a double.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0.0/1.0).
	 */
	public double evaluateDouble(EvaluateContext ec) throws PrismLangException
	{
		Object o = evaluate(ec);
		if (o instanceof Integer) {
			return ((Integer) o).intValue();
		}
		if (o instanceof Double) {
			return ((Double) o).doubleValue();
		}
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue() ? 1.0 : 0.0;
		}
		throw new PrismLangException("Cannot evaluate to a double", this);
	}

	/**
	 * Evaluate this expression as a double.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0.0/1.0).
	 */
	public double evaluateDouble(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression as a double.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0.0/1.0).
	 */
	public double evaluateDouble(State state) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression as a double.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0.0/1.0).
	 */
	public double evaluateDouble(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression as a double.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0.0/1.0).
	 */
	public double evaluateDouble() throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextValues(null, null));
	}

	/**
	 * Evaluate this expression as a boolean.
	 * Any typing issues cause an exception.
	 */
	public boolean evaluateBoolean(EvaluateContext ec) throws PrismLangException
	{
		Object o = evaluate(ec);
		if (!(o instanceof Boolean)) {
			throw new PrismLangException("Cannot evaluate to a boolean", this);
		}
		return ((Boolean) o).booleanValue();
	}

	/**
	 * Evaluate this expression as a boolean.
	 * Any typing issues cause an exception.
	 */
	public boolean evaluateBoolean(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression as a boolean.
	 * Any typing issues cause an exception.
	 */
	public boolean evaluateBoolean(State state) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression as a boolean.
	 * Any typing issues cause an exception.
	 */
	public boolean evaluateBoolean(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression as a boolean.
	 * Any typing issues cause an exception.
	 */
	public boolean evaluateBoolean() throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextValues(null, null));
	}

	// Static constructors for convenience

	public static Expression True()
	{
		return new ExpressionLiteral(TypeBool.getInstance(), true);
	}

	public static Expression False()
	{
		return new ExpressionLiteral(TypeBool.getInstance(), false);
	}

	public static Expression Int(int i)
	{
		return new ExpressionLiteral(TypeInt.getInstance(), i);
	}

	public static Expression Double(double d)
	{
		return new ExpressionLiteral(TypeDouble.getInstance(), d);
	}

	public static Expression Literal(Object o) throws PrismLangException
	{
		if (o instanceof Integer) {
			return Int(((Integer) o).intValue());
		} else if (o instanceof Double) {
			return Double(((Double) o).doubleValue());
		} else if (o instanceof Boolean) {
			return (((Boolean) o).booleanValue() ? True() : False());
		} else {
			throw new PrismLangException("Unknown object type " + o.getClass());
		}
	}

	public static Expression Not(Expression expr)
	{
		return new ExpressionUnaryOp(ExpressionUnaryOp.NOT, expr);
	}

	public static Expression And(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.AND, expr1, expr2);
	}

	public static Expression Or(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.OR, expr1, expr2);
	}

	public static Expression Iff(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.IFF, expr1, expr2);
	}

	public static Expression Implies(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.IMPLIES, expr1, expr2);
	}

	public static Expression Plus(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.PLUS, expr1, expr2);
	}

	public static Expression Minus(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.MINUS, expr1, expr2);
	}

	public static Expression Times(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.TIMES, expr1, expr2);
	}

	public static Expression Divide(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.DIVIDE, expr1, expr2);
	}

	public static Expression Parenth(Expression expr)
	{
		return new ExpressionUnaryOp(ExpressionUnaryOp.PARENTH, expr);
	}

	// Static testers for convenience

	public static boolean isTrue(Expression expr)
	{
		return expr instanceof ExpressionLiteral && ((ExpressionLiteral) expr).getValue().equals(true);
	}

	public static boolean isFalse(Expression expr)
	{
		return expr instanceof ExpressionLiteral && ((ExpressionLiteral) expr).getValue().equals(false);
	}

	public static boolean isInt(Expression expr)
	{
		return expr instanceof ExpressionLiteral && expr.getType() instanceof TypeInt;
	}

	public static boolean isDouble(Expression expr)
	{
		return expr instanceof ExpressionLiteral && expr.getType() instanceof TypeDouble;
	}

	public static boolean isNot(Expression expr)
	{
		return expr instanceof ExpressionUnaryOp && ((ExpressionUnaryOp) expr).getOperator() == ExpressionUnaryOp.NOT;
	}

	public static boolean isAnd(Expression expr)
	{
		return expr instanceof ExpressionBinaryOp
				&& ((ExpressionBinaryOp) expr).getOperator() == ExpressionBinaryOp.AND;
	}

	public static boolean isOr(Expression expr)
	{
		return expr instanceof ExpressionBinaryOp && ((ExpressionBinaryOp) expr).getOperator() == ExpressionBinaryOp.OR;
	}

	public static boolean isIff(Expression expr)
	{
		return expr instanceof ExpressionBinaryOp
				&& ((ExpressionBinaryOp) expr).getOperator() == ExpressionBinaryOp.IFF;
	}

	public static boolean isImplies(Expression expr)
	{
		return expr instanceof ExpressionBinaryOp
				&& ((ExpressionBinaryOp) expr).getOperator() == ExpressionBinaryOp.IMPLIES;
	}

	public static boolean isParenth(Expression expr)
	{
		return expr instanceof ExpressionUnaryOp
				&& ((ExpressionUnaryOp) expr).getOperator() == ExpressionUnaryOp.PARENTH;
	}

	public static boolean isRelOp(Expression expr)
	{
		return expr instanceof ExpressionBinaryOp
				&& ExpressionBinaryOp.isRelOp(((ExpressionBinaryOp) expr).getOperator());
	}

	/**
	 * Test if an expression is a quantitative property (P=?, R=? or S=?) 
	 */
	public static boolean isQuantitative(Expression expr)
	{
		if (expr instanceof ExpressionProb) {
			return ((ExpressionProb) expr).getProb() == null;
		}
		else if (expr instanceof ExpressionReward) {
			return ((ExpressionReward) expr).getReward() == null;
		}
		else if (expr instanceof ExpressionSS) {
			return ((ExpressionSS) expr).getProb() == null;
		}
		return false;
	}
}

//------------------------------------------------------------------------------
