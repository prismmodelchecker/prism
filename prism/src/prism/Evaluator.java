//==============================================================================
//	
//	Copyright (c) 2020-
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

package prism;

import param.BigRational;
import param.Function;
import param.FunctionFactory;
import parser.EvaluateContext.EvalMode;
import parser.EvaluateContextState;
import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.type.TypeDouble;

/**
 * Interface specifying operations that need to be supported for classes used to
 * store values for probabilities/rewards/etc. in generic storage classes,
 * e.g., subclasses of {@link explicit.Model}{@code <Value>}. Such classes
 * store values as an arbitrary type {@code Value} but also need to provide
 * an {@link Evaluator} object to manipulate them. This approach is intended to
 * allow existing types (e.g. Double, BigRational, Function) to be used for for
 * generic models without the need for creating additional wrappers.
 * 
 * For evaluators where values are non-scalar and represent a set of different
 * values (e.g., intervals or symbolic functions), the methods should return a
 * result that holds for all possible values. If this cannot be done, an
 * UnsupportedOperationException can be thrown.
 */
public interface Evaluator<Value>
{
	// Static methods to create Evaluator instances for common types
	
	public static Evaluator<Double> createForDoubles()
	{
		return evalDbl;
	}

	public static Evaluator<BigRational> createForBigRationals()
	{
		return evalBigRat;
	}

	public static Evaluator<Function> createForRationalFunctions(FunctionFactory functionFactory)
	{
		return new EvaluatorFunction(functionFactory);
	}
	
	// Methods in the Evaluator interface
	
	/**
	 * Get the value zero.
	 */
	public Value zero();

	/**
	 * Get the value one.
	 */
	public Value one();

	/**
	 * Check if a value {@code x} is equal to zero.
	 */
	public boolean isZero(Value x);

	/**
	 * Check if a value {@code x} is equal to one.
	 */
	public boolean isOne(Value x);

	/**
	 * Check if a value {@code x} is finite (not +/- infinite or NaN).
	 */
	public boolean isFinite(Value x);
	
	/**
	 * Compute the sum of {@code x} and {@code y}.
	 */
	public Value add(Value x, Value y);

	/**
	 * Compute {@code x} minus {@code y}.
	 */
	public Value subtract(Value x, Value y);

	/**
	 * Compute {@code x} multiplied by {@code y}.
	 */
	public Value multiply(Value x, Value y);

	/**
	 * Compute {@code x} divided by {@code y}.
	 */
	public Value divide(Value x, Value y);

	/**
	 * Is {@code x} greater than {@code y}?
	 */
	public boolean gt(Value x, Value y);

	/**
	 * Is {@code x} greater than or equal to {@code y}?
	 */
	public boolean geq(Value x, Value y);

	/**
	 * Is {@code x} "equal" to {@code y}?
	 * Note: this is typically a weaker check than Value.equals(), e.g.,
	 * a floating-point implementation might incorporate a round-off tolerance.
	 */
	public boolean equals(Value x, Value y);

	/**
	 * Check that the sum of probabilities in a distribution is legal.
	 * Throws an explanation exception if not.
	 * By default, this amounts to checking that it is equal to 1.
	 * A floating-point implementation might incorporate a round-off tolerance.
	 */
	public void checkProbabilitySum(Value sum) throws PrismException;
	
	/**
	 * Evaluate an expression in a state to type {@code Value},
	 * and with values for any constants (optionally) provided.
	 * Values for constants need to be of appropriate type, e.g.,
	 * using exact arithmetic if the Evaluator is doing likewise.
	 */
	public Value evaluate(Expression expr, Values constantValues, State state) throws PrismLangException;

	/**
	 * Evaluate an expression in a state to type {@code Value}
	 */
	public default Value evaluate(Expression expr, State state) throws PrismLangException
	{
		return evaluate(expr, null, state);
	}

	/**
	 * Parse a value from a string.
	 * Throws NumberFormatException in case of a parsing error.
	 */
	public Value fromString(String s) throws NumberFormatException;
	
	/**
	 * Convert value {@code x} to a double (if possible).
	 */
	public double toDouble(Value x);

	/**
	 * Convert value {@code x} to a string for use in an exported file,
	 * such as a transitions (.tra) file or similar. For example,
	 * PRISM exports doubles in a consistent fashion (different from
	 * Java's Double.toString) to be compatible with export code in C++.
	 */
	public default String toStringExport(Value x)
	{
		return x.toString();
	}
	
	/**
	 * Convert value {@code x} to a string for use in an exported file,
	 * such as a transitions (.tra) file or similar. For example,
	 * PRISM exports doubles in a consistent fashion (different from
	 * Java's Double.toString) to be compatible with export code in C++.
	 * If relevant/possible, this is shown to {@code precision} significant places.
	 */
	public default String toStringExport(Value x, int precision)
	{
		// By default, just ignore precision
		return toStringExport(x);
	}
	
	/**
	 * Convert value {@code x} to a string for use in a PRISM model/property.
	 */
	public default String toStringPrism(Value x)
	{
		// Just do the same as toStringExport, by default
		return toStringExport(x);
	}
	
	/**
	 * Convert value {@code x} to a string for use in a PRISM model/property.
	 * If relevant/possible, this is shown to {@code precision} significant places.
	 */
	public default String toStringPrism(Value x, int precision)
	{
		// Just do the same as toStringExport, by default
		return toStringExport(x, precision);
	}
	
	/**
	 * Does this Evaluator work with exact values?
	 */
	public boolean exact();

	/**
	 * Does this Evaluator work with symbolic expressions, rather than scalar values?
	 */
	public boolean isSymbolic();
	
	/**
	 * Get the evaluation model used by this Evaluator.
	 */
	public EvalMode evalMode();

	// Static Evaluator instances for common types
	
	// Evaluator for doubles
	
	static Evaluator<Double> evalDbl = new EvaluatorDouble();
	
	class EvaluatorDouble implements Evaluator<Double>
	{
		@Override
		public Double zero()
		{
			return 0.0;
		}

		@Override
		public Double one()
		{
			return 1.0;
		}

		@Override
		public boolean isZero(Double x)
		{
			return x == 0.0;
		}

		@Override
		public boolean isOne(Double x)
		{
			// We allow round-off error here
			return PrismUtils.doublesAreEqual(x, 1.0);
		}

		@Override
		public boolean isFinite(Double x)
		{
			return Double.isFinite(x);
		}

		@Override
		public Double add(Double x, Double y)
		{
			return x + y;
		}

		@Override
		public Double subtract(Double x, Double y)
		{
			return x - y;
		}

		@Override
		public Double multiply(Double x, Double y)
		{
			return x * y;
		}

		@Override
		public Double divide(Double x, Double y)
		{
			return x / y;
		}

		@Override
		public boolean gt(Double x, Double y)
		{
			return x > y;
		}

		@Override
		public boolean geq(Double x, Double y)
		{
			return x >= y;
		}

		@Override
		public boolean equals(Double x, Double y)
		{
			// We allow round-off error here
			return PrismUtils.doublesAreEqual(x, y);
		}
		
		@Override
		public void checkProbabilitySum(Double sum) throws PrismException
		{
			// We allow round-off error here
			if (!PrismUtils.doublesAreEqual(sum, 1.0)) {
				throw new PrismException("Probabilities sum to " + sum);
			}
		}
		
		@Override
		public Double evaluate(Expression expr, Values constantValues, State state) throws PrismLangException
		{
			return expr.evaluateDouble(constantValues, state);
		}

		@Override
		public double toDouble(Double x)
		{
			return x;
		}

		@Override
		public String toStringExport(Double x)
		{
			return PrismUtils.formatDouble(x);
		}
		
		@Override
		public String toStringExport(Double x, int precision)
		{
			return PrismUtils.formatDouble(precision, x);
		}
		
		@Override
		public Double fromString(String s) throws NumberFormatException
		{
			return Double.parseDouble(s);
		}

		@Override
		public boolean exact()
		{
			return false;
		}
		
		@Override
		public boolean isSymbolic()
		{
			return false;
		}
		
		@Override
		public EvalMode evalMode()
		{
			return EvalMode.FP;
		}
	};

	// Evaluator for rationals (using param.BigRational)
	
	static Evaluator<BigRational> evalBigRat = new EvaluatorBigRational();
	
	class EvaluatorBigRational implements Evaluator<BigRational>
	{
		@Override
		public BigRational zero()
		{
			return BigRational.ZERO;
		}

		@Override
		public BigRational one()
		{
			return BigRational.ONE;
		}

		@Override
		public boolean isZero(BigRational x)
		{
			return x.equals(BigRational.ZERO);
		}

		@Override
		public boolean isOne(BigRational x)
		{
			return x.equals(BigRational.ONE);
		}

		@Override
		public boolean isFinite(BigRational x)
		{
			return !(x.isInf() || x.isMInf() | x.isNaN());
		}

		@Override
		public BigRational add(BigRational x, BigRational y)
		{
			return x.add(y);
		}

		@Override
		public BigRational subtract(BigRational x, BigRational y)
		{
			return x.subtract(y);
		}

		@Override
		public BigRational multiply(BigRational x, BigRational y)
		{
			return x.multiply(y);
		}

		@Override
		public BigRational divide(BigRational x, BigRational y)
		{
			return x.divide(y);
		}

		@Override
		public boolean gt(BigRational x, BigRational y)
		{
			return x.compareTo(y) > 0;
		}

		@Override
		public boolean geq(BigRational x, BigRational y)
		{
			return x.compareTo(y) >= 0;
		}

		@Override
		public boolean equals(BigRational x, BigRational y)
		{
			return x.equals(y);
		}
		
		@Override
		public void checkProbabilitySum(BigRational sum) throws PrismException
		{
			if (!isOne(sum)) {
				throw new PrismException("Probabilities sum to " + sum);
			}
		}
		
		@Override
		public BigRational evaluate(Expression expr, Values constantValues, State state) throws PrismLangException
		{
			Object value = expr.evaluate(new EvaluateContextState(constantValues, state).setEvaluationMode(EvalMode.EXACT));
			return (BigRational) TypeDouble.getInstance().castValueTo(value, EvalMode.EXACT);
		}

		@Override
		public double toDouble(BigRational x)
		{
			return x.doubleValue();
		}

		@Override
		public BigRational fromString(String s) throws NumberFormatException
		{
			return new BigRational(s);
		}

		@Override
		public boolean exact()
		{
			return true;
		}
		
		@Override
		public boolean isSymbolic()
		{
			return false;
		}
		
		@Override
		public EvalMode evalMode()
		{
			return EvalMode.EXACT;
		}
	};
	
	// Evaluator for rational functions (using param.Function)
	
	class EvaluatorFunction implements Evaluator<Function>
	{
		protected FunctionFactory functionFactory;

		public EvaluatorFunction(FunctionFactory functionFactory)
		{
			this.functionFactory = functionFactory;
		}

		@Override
		public Function zero()
		{
			return functionFactory.getZero();
		}

		@Override
		public Function one()
		{
			return functionFactory.getOne();
		}

		@Override
		public boolean isZero(Function x)
		{
			// Technically, not quite right since it could miss some cases
			// where the function is globally zero. But, if this returns true,
			// then it _is_ zero for all values, so still useful to have
			return x.isZero();
		}

		@Override
		public boolean isOne(Function x)
		{
			// Technically, not quite right since it could miss some cases
			// where the function is globally one. But, if this returns true,
			// then it _is_ one for all values, so still useful to have
			return x.isOne();
		}

		@Override
		public boolean isFinite(Function x)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Function add(Function x, Function y)
		{
			return x.add(y);
		}

		@Override
		public Function subtract(Function x, Function y)
		{
			return x.subtract(y);
		}

		@Override
		public Function multiply(Function x, Function y)
		{
			return x.multiply(y);
		}

		@Override
		public Function divide(Function x, Function y)
		{
			return x.divide(y);
		}

		@Override
		public boolean gt(Function x, Function y)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean geq(Function x, Function y)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean equals(Function x, Function y)
		{
			return x.equals(y);
		}
		
		@Override
		public void checkProbabilitySum(Function sum) throws PrismException
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Function evaluate(Expression expr, Values constantValues, State state) throws PrismLangException
		{
			//return expr.getType().castFromBigRational(expr.evaluateExact(state);
			return functionFactory.expr2function((Expression) expr.deepCopy().evaluatePartially(constantValues, state));
		}

		@Override
		public double toDouble(Function x)
		{
			// Cannot do this, in general
			throw new UnsupportedOperationException();
		}

		@Override
		public Function fromString(String s) throws NumberFormatException
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean exact()
		{
			return true;
		}

		@Override
		public boolean isSymbolic()
		{
			return true;
		}
		
		@Override
		public EvalMode evalMode()
		{
			// Return EXACT for now since exact computation uses this
			return EvalMode.EXACT;
		}
	}
}
