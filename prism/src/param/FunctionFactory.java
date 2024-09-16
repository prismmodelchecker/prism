//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Birmingham/Oxford)
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

package param;

import parser.EvaluateContext;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeInt;
import parser.visitor.ASTTraverse;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

import java.util.HashMap;

/**
 * Generates new functions, stores valid ranges of parameters, etc.
 */
public abstract class FunctionFactory
{
	/** names of parameters */
	protected String[] parameterNames;
	/** lower bounds of parameters */
	protected BigRational[] lowerBounds;
	/** upper bounds of parameters */
	protected BigRational[] upperBounds;
	/** maps variable name to index in {@code parameterNames}, @code lowerBounds} and {@code upperBounds} */
	protected HashMap<String, Integer> varnameToInt;
	/** for expression evaluation (no constants need, just a convenient way to force exact evaluation */
	protected EvaluateContext ec = EvaluateContext.create(EvaluateContext.EvalMode.EXACT);

	/**
	 * Create a FunctionFactory based on PRISM settings and parameter details.
	 * @param paramNames names of parameters
	 * @param lowerStr lower bounds of parameters as strings
	 * @param upperStr upper bounds of parameters as strings
	 * @param settings PRISM settings
	 */
	public static FunctionFactory create(String[] paramNames, String[] lowerStr, String[] upperStr, PrismSettings settings) throws PrismException
	{
		// Convert parameter info from strings to numbers
		BigRational[] lower = new BigRational[lowerStr.length];
		BigRational[] upper = new BigRational[upperStr.length];
		for (int param = 0; param < lowerStr.length; param++) {
			lower[param] = new BigRational(lowerStr[param]);
			upper[param] = new BigRational(upperStr[param]);
		}

		// Create function factory
		String functionType = settings.getString(PrismSettings.PRISM_PARAM_FUNCTION);
		if (functionType.equals("JAS")) {
			return new JasFunctionFactory(paramNames, lower, upper);
		} else if (functionType.equals("JAS-cached")) {
			return new CachedFunctionFactory(new JasFunctionFactory(paramNames, lower, upper));
		} else if (functionType.equals("DAG")) {
			double dagMaxError = settings.getDouble(PrismSettings.PRISM_PARAM_DAG_MAX_ERROR);
			return new DagFunctionFactory(paramNames, lower, upper, dagMaxError, false);
		} else {
			throw new PrismException("Unknown function factory type \"" + functionType + "\"");
		}
	}

	/**
	 * Creates a new function factory.
	 * {@code parameterNames}, {@code lowerBounds}, {@code upperBounds} all
	 * must have the same length. Each index into these arrays represents
	 * respectively the name, lower and upper bound of a given variable.
	 * After this function factory has been created, the number of variables,
	 * their names and bounds are fixed and cannot be changed anymore.
	 * 
	 * @param parameterNames names of parameters
	 * @param lowerBounds lower bounds of parameters
	 * @param upperBounds upper bounds of parameters
	 */
	public FunctionFactory(String[] parameterNames, BigRational[] lowerBounds, BigRational[] upperBounds) {
		this.parameterNames = parameterNames;
		this.lowerBounds = lowerBounds;
		this.upperBounds = upperBounds;
		this.varnameToInt = new HashMap<String, Integer>();
		for (int var = 0; var < parameterNames.length; var++) {
			varnameToInt.put(parameterNames[var], var);
		}
	}

	/**
	 * Returns a function representing the number one.
	 * @return function representing the number one
	 */
	public abstract Function getOne();
	
	/**
	 * Returns a function representing the number zero.
	 * @return function representing the number zero
	 */
	public abstract Function getZero();

	/**
	 * Returns a function representing not-a-number.
	 * @return function representing not-a-number
	 */
	public abstract Function getNaN();
	
	/**
	 * Returns a function representing positive infinity.
	 * @return function representing the positive infinity
	 */
	public abstract Function getInf();
	
	/**
	 * Returns a function representing negative infinity.
	 * @return function representing the negative infinity
	 */
	public abstract Function getMInf();
	
	/**
	 * Returns a new function which represents the same value as the
	 * {@code BigRational} {@code bigRat}.
	 * 
	 * @param bigRat value to create a function of
	 * @return function representing the same value as {@code bigRat}
	 */
	public abstract Function fromBigRational(BigRational bigRat);
	
	/**
	 * Returns a function representing a single variable. 
	 * 
	 * @param var the variable to create a function of
	 * @return function consisting only in one variable
	 */
	public abstract Function getVar(int var);


	/**
	 * Returns a function representing a single variable. 
	 * 
	 * @param var name of the variable to create a function of
	 * @return function consisting only in one variable
	 */
	public Function getVar(String var) {
		return getVar(varnameToInt.get(var));
	}
	
	/**
	 * Returns name of variable with the given index.
	 * 
	 * @param var index of the variable to obtain name of
	 * @return name of {@code var}
	 */
	public String getParameterName(int var) {
		return parameterNames[var];
	}

	/**
	 * Returns lower bound of variable with the given index.
	 * 
	 * @param var index of the variable to obtain lower bound of
	 * @return lower bound of {@code var}
	 */
	public BigRational getLowerBound(int var) {
		return lowerBounds[var];
	}
	
	/**
	 * Returns upper bound of variable with the given index.
	 * 
	 * @param var index of the variable to obtain upper bound of
	 * @return upper bound of {@code var}
	 */
	public BigRational getUpperBound(int var) {
		return upperBounds[var];
	}
	
	/**
	 * Returns number of variables used in this function factory.
	 * @return
	 */
	public int getNumVariables() {
		return parameterNames.length;
	}
	
	/**
	 * Returns a function representing the value of the given number.
	 * 
	 * @param from number to create function of
	 * @return function representing the number {@code from}
	 */
	public Function fromLong(long from) {
		return fromBigRational(new BigRational(from));
	}
	
	/**
	 * Transform PRISM expression to rational function.
	 * If successful, a function representing the given expression will be
	 * constructed. This is however not always possible, as not each PRISM
	 * expression can be represented as a rational function. In this case
	 * a {@code PrismException} will be thrown.
	 * 
	 * @param expr PRISM expression to transform to rational function
	 * @return rational function representing the given PRISM expression
	 * @throws PrismException thrown if {@code expr} cannot be represented as rational function
	 */
	public Function expr2function(Expression expr) throws PrismLangException
	{
		return expr2function(expr, null);
	}
	
	/**
	 * Transform PRISM expression to rational function.
	 * If successful, a function representing the given expression will be
	 * constructed. This is however not always possible, as not each PRISM
	 * expression can be represented as a rational function. In this case
	 * a {@code PrismException} will be thrown.
	 * 
	 * @param expr PRISM expression to transform to rational function
	 * @return rational function representing the given PRISM expression
	 * @throws PrismException thrown if {@code expr} cannot be represented as rational function
	 */
	public Function expr2function(Expression expr, Values constantValues) throws PrismLangException
	{
		if (expr instanceof ExpressionLiteral) {
			String exprString = ((ExpressionLiteral) expr).getString();
			if (exprString == null || exprString.equals("")) {
				throw new PrismLangException("Cannot create rational function from literal for which no string is set", expr);
			}
			return fromBigRational(new BigRational(exprString));
		} else if (expr instanceof ExpressionConstant) {
			String exprString = ((ExpressionConstant) expr).getName();
			if (constantValues != null && constantValues.contains(exprString)) {
				Object val = constantValues.getValueOf(exprString);
				return fromBigRational(new BigRational(val.toString()));
			} else {
				return getVar(exprString);
			}
		} else if (expr instanceof ExpressionBinaryOp) {
			ExpressionBinaryOp binExpr = ((ExpressionBinaryOp) expr);
			// power is handled differently due to some constraints
			if (binExpr.getOperator() ==  ExpressionBinaryOp.POW) {
				// power is supported if the exponent is an integer and doesn't refer parametric constants
				if (!containsParameter(binExpr.getOperand2(), constantValues) && binExpr.getOperand2().getType() instanceof TypeInt) {
					int exp = binExpr.getOperand2().evaluateInt(ec);
					Function f1 = expr2function(binExpr.getOperand1());
					return f1.pow(exp);
				} else {
					throw new PrismLangException("Cannot create rational function for expression " + expr, expr);
				}
			}
			// other arithmetic binary operators:
			Function f1 = expr2function(binExpr.getOperand1());
			Function f2 = expr2function(binExpr.getOperand2());
			switch (binExpr.getOperator()) {
			case ExpressionBinaryOp.PLUS:
				return f1.add(f2);
			case ExpressionBinaryOp.MINUS:
				return f1.subtract(f2);
			case ExpressionBinaryOp.TIMES:
				return f1.multiply(f2);
			case ExpressionBinaryOp.DIVIDE:
				return f1.divide(f2);
			default:
				throw new PrismLangException("Cannot create rational function for this operator: " + expr, expr);
			}
		} else if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp unExpr = ((ExpressionUnaryOp) expr);
			Function f = expr2function(unExpr.getOperand());
			switch (unExpr.getOperator()) {
			case ExpressionUnaryOp.MINUS:
				return f.negate();
			case ExpressionUnaryOp.PARENTH:
				return f;
			default:
				throw new PrismLangException("Cannot create rational function for this operator: " + expr, expr);
			}
		} else if (expr instanceof ExpressionITE){
			ExpressionITE iteExpr = (ExpressionITE) expr;
			// ITE expressions where the if-expression does not
			// depend on a parametric constant are supported
			if (!containsParameter(iteExpr.getOperand1(), constantValues)) {
				boolean ifValue = iteExpr.getOperand1().evaluateBoolean(ec);
				if (ifValue) {
					return expr2function(iteExpr.getOperand2());
				} else {
					return expr2function(iteExpr.getOperand3());
				}
			} else {
				throw new PrismLangException("Cannot create rational function for expression " + expr, expr);
				}
		} else if (expr instanceof ExpressionFunc) {
			// functions (min, max, floor, ...) are supported if
			// they don't refer to parametric constants in their arguments
			// and can be exactly evaluated
			if (!containsParameter(expr, constantValues)) {
				BigRational value = expr.evaluateBigRational(ec);
				return fromBigRational(value);
			} else {
				throw new PrismLangException("Cannot create rational function for this function: " + expr, expr);
			}
		} else {
			throw new PrismLangException("Cannot create rational function for expression " + expr, expr);
		}
	}

	/**
	 * Returns true if the expression contains a reference to a parameter,
	 * i.e., an ExpressionConstant not in the provided constantValues list.
	 */
	private static boolean containsParameter(Expression expr, Values constantValues)
	{
		try {
			// check for time bounds, don't recurse into P/R/SS subformulas
			expr.accept(new ASTTraverse()
			{
				public void visitPre(ExpressionConstant e) throws PrismLangException
				{
					String exprString = ((ExpressionConstant) e).getName();
					if (constantValues == null || !constantValues.contains(exprString)) {
						throw new PrismLangException("Found one");
					}
				}
			});
		} catch (PrismLangException e) {
			return true;
		}
		return false;
	}
}
