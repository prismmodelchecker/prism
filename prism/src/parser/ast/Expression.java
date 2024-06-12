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

import jltl2ba.SimpleLTL;
import param.BigRational;
import parser.BooleanUtils;
import parser.EvaluateContext;
import parser.EvaluateContext.EvalMode;
import parser.EvaluateContextConstants;
import parser.EvaluateContextState;
import parser.EvaluateContextSubstate;
import parser.EvaluateContextValues;
import parser.State;
import parser.Values;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.type.TypePathBool;
import parser.visitor.ASTTraverse;
import parser.visitor.CheckValid;
import parser.visitor.ConvertForJltl2ba;
import parser.visitor.DeepCopy;
import parser.visitor.ExpressionTraverseNonNested;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;

// Abstract class for PRISM language expressions

public abstract class Expression extends ASTElement
{
	/**
	 * Is this expression constant?
	 */
	public abstract boolean isConstant();

	/**
	 * Is this expression a proposition?
	 * (i.e. something that can be evaluated on a single state in isolation)
	 */
	public abstract boolean isProposition();

	/**
	 * Evaluate this expression and return the result as an Object
	 * of the appropriate type for the type of this expression and the
	 * evaluation mode of the {@link EvaluateContext} object.
	 * E.g. a "double" is returned as a Double for floating point mode (EvalMode.FP)
	 * but as a BigRational for exact mode (EvalMode.EXACT).
	 * Note: assumes that type checking has been done already.
	 */
	public abstract Object evaluate(EvaluateContext ec) throws PrismLangException;

	/**
	 * Evaluate this expression exactly and return the result as a BigRational.
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Note: assumes that type checking has been done already.
	 */
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		// Evaluate using a copy of the EvaluateContext
		// where the evaluation mode is set to EXACT.
		// Then convert the result to BigRational, regardless of type.
		return BigRational.from(evaluate(new EvaluateContext()
		{
			public EvalMode getEvaluationMode()
			{
				return EvalMode.EXACT;
			}
			
			@Override
			public Object getVarValue(String name, int index)
			{
				return ec.getVarValue(name, index);
			}
			
			@Override
			public Object getConstantValue(String name)
			{
				return ec.getConstantValue(name);
			}
		}));
	}

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

	/**
	 * Ordered list defining precedence of operators in the PRISM expression grammar.
	 * Earlier in the list means lower precedence (binds less tightly).
	 */
	enum Precedence {
		TEMPORAL_BINARY,
		TEMPORAL_UNARY,
		ITE,
		IMPLIES,
		IFF,
		OR,
		AND,
		NOT,
		EQUALITY,
		RELOP,
		PLUS_MINUS,
		TIMES_DIVIDE,
		POW,
		UNARY_MINUS,
		BASIC
	}

	/**
	 * Get the relative precedence ordering of this expression in the PRISM expression grammar,
	 * primarily for the purposes of making sure toString() is parseable.
	 */
	public Precedence getPrecedence()
	{
		// Default to "basic" (atomic, highest precedence)
		return Precedence.BASIC;
	}

	/**
	 * Returns true if {@code expr1} has (strictly) lower operator precedence than {@code expr2},
	 * i.e., if {@code expr1} needs parenthesising when a child of {@code expr2}.
	 */
	public static boolean hasPrecedenceLessThan(Expression expr1, Expression expr2)
	{
		return expr2.getPrecedence() != Precedence.BASIC && expr1.getPrecedence().ordinal() < expr2.getPrecedence().ordinal();
	}

	/**
	 * Returns true if {@code expr1} has lower or equal operator precedence than {@code expr2},
	 * i.e., if {@code expr1} needs parenthesising when a child of {@code expr2}.
	 */
	public static boolean hasPrecedenceLessThanOrEquals(Expression expr1, Expression expr2)
	{
		return expr2.getPrecedence() != Precedence.BASIC && expr1.getPrecedence().ordinal() <= expr2.getPrecedence().ordinal();
	}

	// Overwritten version of deepCopy() and deepCopy(DeepCopy copier) from superclass ASTElement (to reduce casting).

	@Override
	public abstract Expression deepCopy(DeepCopy copier) throws PrismLangException;

	@Override
	public Expression deepCopy()
	{
		return (Expression) super.deepCopy();
	}

	// Overwritten version of clone() from superclass ASTElement (to reduce casting).
	
	@Override
	public Expression clone()
	{
		return (Expression) super.clone();
	}

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
	 * Returns {@code true} if this expression is a path formula.
	 * If {@code allowNestedOperators==false} then we don't allow
	 * nested P, R, ... operators.
	 * @param allowNestedOperators allow nested P, R, ... operators?
	 */
	public boolean isPathFormula(boolean allowNestedOperators)
	{
		try {
			if (getType() == null) {
				this.typeCheck();
			}
			if (getType() == TypePathBool.getInstance() ||
			    getType() == TypeBool.getInstance()) {
				if (!allowNestedOperators) {
					if (this.computeProbNesting() >= 1) {
						return false;
					}
				}
				return true;
			}

			return false;
		} catch (PrismLangException e) {
			return false;
		}
	}

	/**
	 * Convert a property expression (an LTL formula) into the classes used by
	 * the jltl2ba (and jltl2dstar) libraries.
	 */
	public jltl2ba.SimpleLTL convertForJltl2ba() throws PrismLangException
	{
		ConvertForJltl2ba converter = new ConvertForJltl2ba();
		return converter.convert(this);
	}

	/**
	 * Evaluate this expression, using no constant or variable values.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate() throws PrismLangException
	{
		return evaluate(new EvaluateContextConstants(null));
	}

	/**
	 * Evaluate this expression, based on values for constants (but not variables).
	 * Constant values are supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(Values constantValues) throws PrismLangException
	{
		return evaluate(new EvaluateContextConstants(constantValues));
	}

	/**
	 * Evaluate this expression, based on values for constants/variables.
	 * Each set of values is supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluate(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression, based on values for variables (but not constants).
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public Object evaluate(State state) throws PrismLangException
	{
		return evaluate(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression, based on values for constants/variables.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that type checking has been done.
	 */
	public Object evaluate(Values constantValues, State state) throws PrismLangException
	{
		return evaluate(new EvaluateContextState(constantValues, state));
	}

	/**
	 * Evaluate this expression, based on values for some variables (but not constants).
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public Object evaluate(State substate, int[] varMap) throws PrismLangException
	{
		return evaluate(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression, based on values for constants and some variables.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that type checking has been done.
	 */
	public Object evaluate(Values constantValues, State substate, int[] varMap) throws PrismLangException
	{
		return evaluate(new EvaluateContextSubstate(constantValues, substate, varMap));
	}

	/**
	 * Evaluate this expression as an integer.
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors,
	 * and converts to evaluation mode FP (e.g. from EXACT) if needed.
	 */
	public int evaluateInt(EvaluateContext ec) throws PrismLangException
	{
		return (Integer) TypeInt.getInstance().castValueTo(evaluate(ec), EvalMode.FP);
	}

	/**
	 * Evaluate this expression as an integer, using no constant or variable values.
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Note: assumes that type checking has been done already.
	 */
	public int evaluateInt() throws PrismLangException
	{
		return evaluateInt(new EvaluateContextConstants(null));
	}

	/**
	 * Evaluate this expression as an integer, based on values for constants (but not variables).
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public int evaluateInt(Values constantValues) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextConstants(constantValues));
	}

	/**
	 * Evaluate this expression as an integer, based on values for constants/variables.
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Each set of values is supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public int evaluateInt(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression as an integer, based on values for variables (but not constants).
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public int evaluateInt(State state) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression as an integer, based on values for constants/variables.
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that type checking has been done.
	 */
	public int evaluateInt(Values constantValues, State state) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextState(constantValues, state));
	}

	/**
	 * Evaluate this expression as an integer, based on values for some variables (but not constants).
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public int evaluateInt(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression as an integer, based on values for constants and some variables.
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that type checking has been done.
	 */
	public int evaluateInt(Values constantValues, State substate, int[] varMap) throws PrismLangException
	{
		return evaluateInt(new EvaluateContextSubstate(constantValues, substate, varMap));
	}

	/**
	 * Evaluate this object as an integer.
	 * This assumes that the type of the expression is int.
	 * Basically casts the result to an int, checking for any type errors.
	 */
	public static int evaluateObjectAsInt(Object o) throws PrismLangException
	{
		return (Integer) TypeInt.getInstance().castValueTo(o, EvalMode.FP);
	}

	/**
	 * Evaluate this expression as a double.
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors,
	 * and converts to evaluation mode FP (e.g. from EXACT) if needed.
	 * Any typing issues cause an exception (but: we do allow conversion of boolean to 0.0/1.0).
	 */
	public double evaluateDouble(EvaluateContext ec) throws PrismLangException
	{
		return (Double) TypeDouble.getInstance().castValueTo(evaluate(ec), EvalMode.FP);
	}

	/**
	 * Evaluate this expression as a double, using no constant or variable values.
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Note: assumes that type checking has been done already.
	 */
	public double evaluateDouble() throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextConstants(null));
	}

	/**
	 * Evaluate this expression as a double, based on values for constants (but not variables).
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public double evaluateDouble(Values constantValues) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextConstants(constantValues));
	}

	/**
	 * Evaluate this expression as a double, based on values for constants/variables.
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Each set of values is supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public double evaluateDouble(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression as a double, based on values for variables (but not constants).
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public double evaluateDouble(State state) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression as a double, based on values for constants/variables.
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that type checking has been done.
	 */
	public double evaluateDouble(Values constantValues, State state) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextState(constantValues, state));
	}

	/**
	 * Evaluate this expression as a double, based on values for some variables (but not constants).
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public double evaluateDouble(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression as a double, based on values for constants and some variables.
	 * This assumes that the type of the expression is (or can be cast to) double.
	 * Basically casts the result to a double, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that type checking has been done.
	 */
	public double evaluateDouble(Values constantValues, State substate, int[] varMap) throws PrismLangException
	{
		return evaluateDouble(new EvaluateContextSubstate(constantValues, substate, varMap));
	}

	/**
	 * Evaluate this expression as a boolean.
	 * This assumes that the type of the expression is bool.
	 * Basically casts the result to a boolean, checking for any type errors.
	 */
	public boolean evaluateBoolean(EvaluateContext ec) throws PrismLangException
	{
		return TypeBool.getInstance().castValueTo(evaluate(ec), EvalMode.FP);
	}

	/**
	 * Evaluate this expression as a boolean, using no constant or variable values.
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Note: assumes that type checking has been done already.
	 */
	public boolean evaluateBoolean() throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextConstants(null));
	}

	/**
	 * Evaluate this expression as a boolean, based on values for constants (but not variables).
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public boolean evaluateBoolean(Values constantValues) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextConstants(constantValues));
	}

	/**
	 * Evaluate this expression as a boolean, based on values for constants/variables.
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Each set of values is supplied as a Values object. 
	 * Note: assumes that type checking has been done already.
	 */
	public boolean evaluateBoolean(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression as a boolean, based on values for variables (but not constants).
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public boolean evaluateBoolean(State state) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression as a boolean, based on values for constants/variables.
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that type checking has been done.
	 */
	public boolean evaluateBoolean(Values constantValues, State state) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextState(constantValues, state));
	}

	/**
	 * Evaluate this expression as a boolean, based on values for some variables (but not constants).
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public boolean evaluateBoolean(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression as a boolean, based on values for constants and some variables.
	 * Basically casts the result to a boolean, checking for any type errors.
	 * Constant values are supplied as a Values object. 
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that type checking has been done.
	 */
	public boolean evaluateBoolean(Values constantValues, State substate, int[] varMap) throws PrismLangException
	{
		return evaluateBoolean(new EvaluateContextSubstate(constantValues, substate, varMap));
	}

	
	/**
	 * Evaluate this expression exactly to a BigRational, using no constant or variable values.
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Note: assumes that type checking has been done already.
	 */
	public BigRational evaluateExact() throws PrismLangException
	{
		return evaluateExact(new EvaluateContextConstants(null));
	}

	/**
	 * Evaluate this expression exactly to a BigRational, based on values for constants (but not variables).
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Constant values are supplied as a Values object.
	 * Note: assumes that type checking has been done already.
	 */
	public BigRational evaluateExact(Values constantValues) throws PrismLangException
	{
		return evaluateExact(new EvaluateContextConstants(constantValues));
	}

	/**
	 * Evaluate this expression exactly to a BigRational, based on values for constants/variables.
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Each set of values is supplied as a Values object.
	 * Note: assumes that type checking has been done already.
	 */
	public BigRational evaluateExact(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluateExact(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate this expression exactly to a BigRational, based on values for variables (but not constants).
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public BigRational evaluateExact(State state) throws PrismLangException
	{
		return evaluateExact(new EvaluateContextState(state));
	}

	/**
	 * Evaluate this expression exactly to a BigRational, based on values for constants/variables.
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Constant values are supplied as a Values object.
	 * Variable values are supplied as a State object, i.e. array of variable values.
	 * Note: assumes that type checking has been done.
	 */
	public BigRational evaluateExact(Values constantValues, State state) throws PrismLangException
	{
		return evaluateExact(new EvaluateContextState(constantValues, state));
	}

	/**
	 * Evaluate this expression exactly to a BigRational, based on values for some variables (but not constants).
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that constants have been evaluated and type checking has been done.
	 */
	public BigRational evaluateExact(State substate, int[] varMap) throws PrismLangException
	{
		return evaluateExact(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Evaluate this expression exactly to a BigRational, based on values for constants and some variables.
	 * This is regardless of the type (e.g. ints, booleans are also converted).
	 * Constant values are supplied as a Values object.
	 * Variable values are supplied as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset).
	 * If any variables required for evaluation are missing, this will fail with an exception.
	 * Note: assumes that type checking has been done.
	 */
	public BigRational evaluateExact(Values constantValues, State substate, int[] varMap) throws PrismLangException
	{
		return evaluateExact(new EvaluateContextSubstate(constantValues, substate, varMap));
	}

	// Static constructors for convenience

	public static ExpressionLiteral True()
	{
		return new ExpressionLiteral(TypeBool.getInstance(), true);
	}

	public static ExpressionLiteral False()
	{
		return new ExpressionLiteral(TypeBool.getInstance(), false);
	}

	public static ExpressionLiteral Int(int i)
	{
		return new ExpressionLiteral(TypeInt.getInstance(), i);
	}

	public static ExpressionLiteral Double(double d)
	{
		return new ExpressionLiteral(TypeDouble.getInstance(), d);
	}

	public static ExpressionLiteral Literal(Object o) throws PrismLangException
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

	public static ExpressionUnaryOp Not(Expression expr)
	{
		return new ExpressionUnaryOp(ExpressionUnaryOp.NOT, expr);
	}

	public static ExpressionBinaryOp And(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.AND, expr1, expr2);
	}

	public static ExpressionBinaryOp Or(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.OR, expr1, expr2);
	}

	public static ExpressionBinaryOp Iff(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.IFF, expr1, expr2);
	}

	public static ExpressionBinaryOp Implies(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.IMPLIES, expr1, expr2);
	}

	public static ExpressionBinaryOp Plus(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.PLUS, expr1, expr2);
	}

	public static ExpressionUnaryOp Minus(Expression expr)
	{
		return new ExpressionUnaryOp(ExpressionUnaryOp.MINUS, expr);
	}

	public static ExpressionBinaryOp Minus(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.MINUS, expr1, expr2);
	}

	public static ExpressionBinaryOp Times(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.TIMES, expr1, expr2);
	}

	public static ExpressionBinaryOp Divide(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.DIVIDE, expr1, expr2);
	}

	public static ExpressionBinaryOp Pow(Expression expr1, Expression expr2)
	{
		return new ExpressionBinaryOp(ExpressionBinaryOp.POW, expr1, expr2);
	}

	public static ExpressionUnaryOp Parenth(Expression expr)
	{
		return new ExpressionUnaryOp(ExpressionUnaryOp.PARENTH, expr);
	}

	public static ExpressionTemporal Next(Expression expr) {
		return new ExpressionTemporal(ExpressionTemporal.P_X, null, expr);
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

	public static boolean isFilter(Expression expr, ExpressionFilter.FilterOperator opType)
	{
		return expr instanceof ExpressionFilter && ((ExpressionFilter) expr).getOperatorType() == opType;
	}
	
	/**
	 * Test if an expression is a function of type {@code nameCode}.
	 */
	public static boolean isFunc(Expression expr, int nameCode)
	{
		return expr instanceof ExpressionFunc && ((ExpressionFunc) expr).getNameCode() == nameCode;
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

	/**
	 * Test if an expression is a reachability path formula (F phi), possibly with a time bound.
	 */
	public static boolean isReach(Expression expr)
	{
		if (expr instanceof ExpressionTemporal) {
			if (((ExpressionTemporal) expr).getOperator() == ExpressionTemporal.P_F) {
				return ((ExpressionTemporal) expr).getOperand2().getType() instanceof TypeBool;
			}
		}
		return false;
	}

	/**
	 * Test if an expression contains time bounds on temporal operators 
	 */
	public static boolean containsTemporalTimeBounds(Expression expr)
	{
		try {
			// check for time bounds, don't recurse into P/R/SS subformulas
			expr.accept(new ExpressionTraverseNonNested()
			{
				public void visitPre(ExpressionTemporal e) throws PrismLangException
				{
					if (e.hasBounds())
						throw new PrismLangException("");
				}
			});
		} catch (PrismLangException e) {
			return true;
		}
		return false;
	}
	
	/**
	 * Test if an expression contains a minimum expected reward operator. 
	 * Actually, this returns true if there is an R operator with "min=?" or a lower bound attached to it,
	 * so this is just an approximation. (For example, an R operator might be embedded within
	 * an "exists" strategy operator)
	 */
	public static boolean containsMinReward(Expression expr)
	{
		try {
			ASTTraverse astt = new ASTTraverse()
			{
				public void visitPost(ExpressionReward e) throws PrismLangException
				{
					if (e.isMin()) {
						throw new PrismLangException("Found one", e);
					}
				}
			};
			expr.accept(astt);
		} catch (PrismLangException e) {
			return true;
		}
		return false;
	}
	
	/**
	 * Test if an expression contains a non-probabilistic LTL formula (i.e., a non-simple path formula). 
	 */
	public static boolean containsNonProbLTLFormula(Expression expr)
	{
		try {
			ASTTraverse astt = new ASTTraverse()
			{
				public void visitPost(ExpressionForAll e) throws PrismLangException
				{
					if (!e.getExpression().isSimplePathFormula())
						throw new PrismLangException("Found one", e);
				}
				
				public void visitPost(ExpressionExists e) throws PrismLangException
				{
					if (!e.getExpression().isSimplePathFormula())
						throw new PrismLangException("Found one", e);
				}
			};
			expr.accept(astt);
		} catch (PrismLangException e) {
			return true;
		}
		return false;
	}
	
	/**
	 * Test if an expression contains a multi(...) property within 
	 */
	public static boolean containsMultiObjective(Expression expr)
	{
		try {
			ASTTraverse astt = new ASTTraverse()
			{
				public void visitPost(ExpressionFunc e) throws PrismLangException
				{
					if (e.getNameCode() == ExpressionFunc.MULTI)
						throw new PrismLangException("Found one", e);
				}
			};
			expr.accept(astt);
		} catch (PrismLangException e) {
			return true;
		}
		return false;
	}
	
	/**
	 * Test if an expression is an LTL formula and is in positive normal form,
	 * i.e. where negation only occurs at the level of state formulae.
	 * This means that the operators => and <=> are also disallowed. 
	 */
	public static boolean isPositiveNormalFormLTL(Expression expr)
	{
		// State formulae (negated or otherwise) are OK
		if (expr.getType() instanceof TypeBool)
			return true;
		// Otherwise recurse, looking for negations...
		else if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnOp = (ExpressionUnaryOp) expr;
			int op = exprUnOp.getOperator();
			switch (op) {
			// Negation is not allowed
			// (since we already checked for state formulae that include negation)
			case ExpressionUnaryOp.NOT:
				return false;
			default:
				return isPositiveNormalFormLTL(exprUnOp.getOperand());
			}
		}
		else if (expr instanceof ExpressionBinaryOp) {
			ExpressionBinaryOp exprBinOp = (ExpressionBinaryOp) expr;
			int op = exprBinOp.getOperator();
			switch (op) {
			// => and <=> are not allowed
			case ExpressionBinaryOp.IMPLIES:
			case ExpressionBinaryOp.IFF:
				return false;
			default:
				return isPositiveNormalFormLTL(exprBinOp.getOperand1()) && isPositiveNormalFormLTL(exprBinOp.getOperand2());
			}
		}
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			if (exprTemp.getOperand1() != null && !isPositiveNormalFormLTL(exprTemp.getOperand1())) {
				return false;
			}
			if (exprTemp.getOperand2() != null && !isPositiveNormalFormLTL(exprTemp.getOperand2())) {
				return false;
			}
			return true;
		}
		// If we get here, it is probably not even LTL
		return false;
	}
	
	/**
	 * Test if an expression is a co-safe LTL formula, detected syntactically
	 * (i.e. if it is in positive normal form and only uses X, F and U).
	 */
	public static boolean isCoSafeLTLSyntactic(Expression expr)
	{
		return isCoSafeLTLSyntactic(expr, false);
	}

	/**
	 * Test if an expression is a co-safe LTL formula, detected syntactically
	 * (i.e. if it is in positive normal form and only uses X, F and U).
	 * If {@code convert} is true, the expression is first converted into positive normal form,
	 * and then it is checked whether it only uses X, F and U.
	 * For example, a => ! (G b) would return true if (and only if) {@code convert} was true.
	 */
	public static boolean isCoSafeLTLSyntactic(Expression expr, boolean convert)
	{
		// Convert to or check for positive normal form
		if (convert) {
			expr = BooleanUtils.convertLTLToPositiveNormalForm(expr.deepCopy());
		} else {
			if (!isPositiveNormalFormLTL(expr))
				return false;
		}
		// Check temporal operators (don't recurse into P/R/S subformulas)
		try {
			ASTTraverse astt = new ExpressionTraverseNonNested()
			{
				public void visitPost(ExpressionTemporal e) throws PrismLangException
				{
					if (e.getOperator() == ExpressionTemporal.P_X)
						return;
					if (e.getOperator() == ExpressionTemporal.P_F)
						return;
					if (e.getOperator() == ExpressionTemporal.P_U)
						return;
					throw new PrismLangException("Found non-X/F/U", e);
				}
			};
			expr.accept(astt);
		} catch (PrismLangException e) {
			return false;
		}
		// All good
		return true;
	}

	/**
	 * Converts an Expression that is a simple path formula to a canonical form:
	 * Either a single non-negated next-step operator
	 * or a single until-operator, optionally preceded by a single negation.
	 * Parentheses are removed.
	 * @param expr the simple path formula
	 * @return the canonical expression
	 */
	public static Expression convertSimplePathFormulaToCanonicalForm(Expression expr) throws PrismLangException
	{
		boolean negated = false;

		if (!expr.isSimplePathFormula()) {
			throw new PrismLangException("Expression is not a simple path formula.");
		}

		// Negation/parentheses
		while (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// remove parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				expr = exprUnary.getOperand();
			}
			// deal with negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				negated = !negated;
				expr = exprUnary.getOperand();
			} else {
				throw new PrismLangException("Expression is not a simple path formula: Unexpected unary operator "+exprUnary.getOperatorSymbol());
			}
		}

		// Temporal operator
		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				if (negated) {
					if (exprTemp.hasBounds()) throw new PrismLangException("Next-Step operator should not have bounds!");

					// ! X expr  <=> X ! expr
					return new ExpressionTemporal(ExpressionTemporal.P_X, null,
					                              Expression.Not(Expression.Parenth(exprTemp.getOperand2())));
				} else {
					// X expr
					return exprTemp;
				}
			} else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				// Until
				expr = exprTemp;
			} else {
				// other operators: convert
				expr = exprTemp.convertToUntilForm();
			}
		} else {
			throw new PrismLangException("Expression is not a simple path formula: Unsupported expression "+expr.toString());
		}

		if (negated) {
			if (expr instanceof ExpressionUnaryOp &&
			    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
				// remove the outer negation
				return ((ExpressionUnaryOp)expr).getOperand();
			} else {
				// negate
				return Expression.Not(expr);
			}
		}

		return expr;
	}

	/**
	 * Create a property expression (an LTL formula) from the classes used by the jltl2ba (and jltl2dstar) libraries.
	 */
	public static Expression createFromJltl2ba(SimpleLTL ltl) throws PrismException
	{
		switch (ltl.kind) {
		case AND:
			return Expression.And(createFromJltl2ba(ltl.left), createFromJltl2ba(ltl.right));
		case AP:
			return new ExpressionLabel(ltl.ap);
		case EQUIV:
			return Expression.Iff(createFromJltl2ba(ltl.left), createFromJltl2ba(ltl.right));
		case FALSE:
			return Expression.False();
		case FINALLY:
			return new ExpressionTemporal(ExpressionTemporal.P_F, null, createFromJltl2ba(ltl.left));
		case GLOBALLY:
			return new ExpressionTemporal(ExpressionTemporal.P_G, null, createFromJltl2ba(ltl.left));
		case IMPLIES:
			return Expression.Implies(createFromJltl2ba(ltl.left), createFromJltl2ba(ltl.right));
		case NEXT:
			return new ExpressionTemporal(ExpressionTemporal.P_X, null, createFromJltl2ba(ltl.left));
		case NOT:
			return Expression.Not(createFromJltl2ba(ltl.left));
		case OR:
			return Expression.Or(createFromJltl2ba(ltl.left), createFromJltl2ba(ltl.right));
		case RELEASE:
			return new ExpressionTemporal(ExpressionTemporal.P_R, createFromJltl2ba(ltl.left), createFromJltl2ba(ltl.right));
		case TRUE:
			return Expression.True();
		case UNTIL:
			return new ExpressionTemporal(ExpressionTemporal.P_U, createFromJltl2ba(ltl.left), createFromJltl2ba(ltl.right));
		default:
			throw new PrismException("Cannot convert jltl2ba formula " + ltl);
		}
	}

	/**
	 * Convert {@code expr} to a string, enclosing in () if its precedence is (strictly) less than {@code parent}.
	 * @param expr Expression to convert to string
	 * @param parent Parent expression directly containing {@code expr}
	 */
	public static String toStringPrecLt(Expression expr, Expression parent)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(expr.toString());
		if (hasPrecedenceLessThan(expr, parent)) {
			builder.insert(0, "(");
			builder.append(")");
		}
		return builder.toString();
	}

	/**
	 * Convert {@code expr} to a string, enclosing in () if its precedence is less than or equal to its {@code parent}.
	 * @param expr Expression to convert to string
	 * @param parent Parent expression directly containing {@code expr}
	 */
	public static String toStringPrecLeq(Expression expr, Expression parent)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(expr.toString());
		if (hasPrecedenceLessThanOrEquals(expr, parent)) {
			builder.insert(0, "(");
			builder.append(")");
		}
		return builder.toString();
	}
}

//------------------------------------------------------------------------------
