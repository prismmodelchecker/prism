//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import java.util.BitSet;

import edu.jas.arith.BigInteger;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.Monomial;
import edu.jas.ufd.Quotient;
import parser.ast.Expression;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionLiteral;
import parser.type.TypeDouble;
import prism.PrismException;

/**
 * Rational function representation using the Java Algebra System (JAS).
 * Functions are stored in the form {@code num / den}. JAS takes care that
 * {@code num} and {@code den} stay coprime. This decreases the memory
 * needed to represent a function, allows for easier comparism, etc. On the
 * other hand, the cancellation algorithm is expensive. The function factory
 * class used with these functions is {@code JasFunctionFactory}.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see <a href="http://krum.rz.uni-mannheim.de/jas/">http://krum.rz.uni-mannheim.de/jas/</a>
 * @see Function
 * @see JasFunctionFactory
 */
final class JasFunction extends Function {
	/** JAS object the function is wrapping */
	private Quotient<BigInteger> jas;
	/** numerator of function (stored if needed) */
	Polynomial num;
	/** denominator of function (stored if needed) */
	Polynomial den;
	/** type of function (rational function, infinity, etc.) */
	int type;
	final static int NORMAL = 0;
	final static int INF = 1;
	final static int MINF = 2;
	final static int NAN = 3;

	// constructors

	/**
	 * Creates a new JAS function.
	 *
	 * @param functionContext function context of this function
	 * @param jas JAS object this function object is wrapping
	 * @param type type of function represented
	 */
	JasFunction(JasFunctionFactory functionContext, Quotient<BigInteger> jas, int type) {
		super(functionContext);
		this.jas = jas;
		this.num = null;
		this.den = null;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return toStringExpression();
	}

	/**
	 * Convert to string formatted as Expression.
	 */
	public String toStringExpression() {
		try {
			return asExpression().toString();
		} catch (PrismException e) {
			return "?";
		}
	}

	/**
	 * Convert to string formatted using JAS methods.
	 */
	public String toStringJas()
	{
		if (isNaN()) {
			return "NaN";
		} else if (isInf()) {
			return "Infinity";
		} else if (isMInf()) {
			return "-Infinity";
		}

		return jas.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JasFunction)) {
			return false;
		}
		JasFunction function = (JasFunction) obj;
		if (isNaN()) {
			return function.isNaN();
		}
		if (isInf()) {
			return function.isInf();
		}
		if (isMInf()) {
			return function.isMInf();
		}
		return jas.equals(function.jas);
	}
	
	@Override
	public int hashCode() {
		return jas.hashCode();
	}
	
	/**
	 * Returns JAS object this function is wrapping.
	 * 
	 * @return JAS object this function is wrapping
	 */
	Quotient<BigInteger> getJas()
	{
		return jas;
	}
	
	@Override
	public Function add(Function other)
	{
		if (this.isNaN() || other.isNaN()) {
			return factory.getNaN();
		}
		if (this.isInf() || other.isInf()) {
			if (this.isMInf() || other.isMInf()) {
				return factory.getZero();
			}
			return factory.getInf();
		}
		if (this.isMInf() || other.isMInf()) {
			return factory.getMInf();
		}
		return new JasFunction((JasFunctionFactory) factory, jas.sum(((JasFunction)other).jas), NORMAL);
	}

	@Override
	public Function negate()
	{
		if (this.isNaN()) {
			return factory.getNaN();			
		}
		if (this.isInf()) {
			return factory.getMInf();
		}
		if (this.isMInf()) {
			return factory.getMInf();			
		}
		return new JasFunction((JasFunctionFactory) factory, jas.negate(), NORMAL);
	}
	
	@Override
	public Function multiply(Function other)
	{
		if (this.isNaN() || other.isNaN()) {
			return factory.getNaN();
		}
		if (this.isZero() || other.isZero()) {
			return factory.getZero();
		}
		if (this.isInf() || other.isInf()) {
			if (this.isMInf() || other.isMInf()) {
				return factory.getMInf();
			} else {
				return factory.getInf();
			}
		}
		return new JasFunction((JasFunctionFactory) factory, jas.multiply(((JasFunction) other).jas), NORMAL);		
	}
	
	@Override
	public Function divide(Function other)
	{
		if (this.isNaN() || other.isNaN()) {
			return factory.getNaN();
		}
		if (other.isInf() || other.isMInf()) {
			return factory.getNaN();
		}
		if (other.isZero()) {
			if (this.isConstant()) {
				// evaluate constant to return either NaN, Inf or -Inf, using BigRational division
				return factory.fromBigRational(this.asBigRational().divide(BigRational.ZERO));
			} else {
				// non-constant
				// TODO: Fix, should be 'this / 0', but that can't be represented by JAS...
				return factory.getInf();
			}
		}
		if (this.isZero()) {
			return factory.getZero();
		}
		return new JasFunction((JasFunctionFactory) factory, jas.divide(((JasFunction) other).jas), NORMAL);
	}

	@Override
	public Function pow(int exp)
	{
		if (this.isNaN()) {
			return factory.getNaN();
		}
		if (this.isInf()) {
			return factory.getMInf();
		}
		if (this.isMInf()) {
			return factory.getMInf();
		}
		return new JasFunction((JasFunctionFactory) factory, jas.power(exp), NORMAL);
	}

	@Override
	public Function star()
	{
		if (this.isNaN()) {
			return factory.getNaN();
		}
		Quotient<BigInteger> one =  ((JasFunctionFactory) factory).getJasQuotRing().getONE();
		Quotient<BigInteger> result = one.subtract(jas);
		result = one.divide(result);
		return new JasFunction((JasFunctionFactory) factory, result, NORMAL);
	}

	/**
	 * Transforms a JAS polynomial to a Polynomial object.
	 * 
	 * @param j JAS polynomial
	 * @return polynomial of Polynomial class
	 */
	private Polynomial jasToPoly(GenPolynomial<BigInteger> j)
	{
		int numVariables = j.numberOfVariables();
		Polynomial result = new Polynomial(numVariables, j.length());
		for (Monomial<BigInteger> jasMono : j) {
			java.math.BigInteger coeff = (jasMono.coefficient()).getVal();
			ExpVector jasExpo = jasMono.exponent();
			ArrayList<Integer> expo = new ArrayList<Integer>();
			for (int var = 0; var < numVariables; var++) {
				expo.add((int)jasExpo.getVal(var));
			}
			result.addTerm(coeff, expo);
		}
		
		return result;
	}
	
	@Override
	public Function toConstraint() {
		if (isNaN() || isInf() || isMInf()) {
			return this;
		}
		if (num == null) {
			num = jasToPoly(jas.num);
		}
		if (den == null) {
			den = jasToPoly(jas.den);
		}
		BigRational[] offset = new BigRational[factory.getNumVariables()]; 
		for (int dim = 0; dim < factory.getNumVariables(); dim++) {
			offset[dim] = factory.getUpperBound(dim).subtract(factory.getLowerBound(dim));
		}
		BigRational evaluated = BigRational.ZERO;
		while (evaluated.isZero() || evaluated.isSpecial()) {
			for (int dim = 0; dim < factory.getNumVariables(); dim++) {
				offset[dim] = offset[dim].divide(2);
			}
			BigRational[] point = new BigRational[factory.getNumVariables()];
			for (int dim = 0; dim < factory.getNumVariables(); dim++) {
				point[dim] = factory.getLowerBound(dim).add(offset[dim]);
			}
			evaluated = den.evaluate(new Point(point), false);
		}
		if (evaluated.signum() == -1) {
			return new JasFunction((JasFunctionFactory) factory, jas.multiply(jas.den).negate(), NORMAL);
		} else {
			return new JasFunction((JasFunctionFactory) factory, jas.multiply(jas.den), NORMAL);
		}
	}

	@Override
	public BigRational evaluate(Point point, boolean cancel) {
		if (isNaN()) {
			return BigRational.NAN;
		} else if (isInf()) {
			return BigRational.INF;
		} else if (isMInf()) {
			return BigRational.MINF;
		}
		if (num == null) {
			num = jasToPoly(jas.num);
		}
		if (den == null) {
			den = jasToPoly(jas.den);
		}
		if (isNaN()) {
			return BigRational.NAN;
		}
		return num.evaluate(point, cancel).divide(den.evaluate(point, cancel), cancel);
	}

	@Override
	public BigRational evaluate(Point point) {
		return evaluate(point, true);
	}
	
	@Override
	public boolean check(Point point, boolean strict)
	{
		BigRational value = evaluate(point, false);
		int compare = value.signum();
		return strict ? (compare > 0) : (compare >= 0);
	}

	@Override
	public Expression asExpression() throws PrismException
	{
		if (isNaN()) {
			return new ExpressionLiteral(TypeDouble.getInstance(), BigRational.NAN, BigRational.NAN.toString());
		} else if (isInf()) {
			return new ExpressionLiteral(TypeDouble.getInstance(), BigRational.INF, BigRational.INF.toString());
		} else if (isMInf()) {
			return new ExpressionLiteral(TypeDouble.getInstance(), BigRational.MINF, BigRational.MINF.toString());
		}
		Expression expr =  jasPoly2expr(jas.num);
		if (!jas.den.isONE()) {
			expr = Expression.Divide(expr, jasPoly2expr(jas.den));
		}
		return expr;
	}

	/**
	 * Transform a JAS polynomial to an Expression object.
	 */
	private Expression jasPoly2expr(GenPolynomial<BigInteger> jasPoly) throws PrismException
	{
		// Special case: 0
		if (jasPoly.isZERO()) {
			return new ExpressionLiteral(TypeDouble.getInstance(), BigInteger.ZERO, "0");
		}

		// Extract info about coefficients and (expressions for) variable powers
		int numVariables = jasPoly.numberOfVariables();
		ArrayList<java.math.BigInteger> coeffsAbs = new ArrayList<>();
		BitSet coeffsNeg = new BitSet();
		BitSet coeffsOne = new BitSet();
		ArrayList<Expression> exprPows = new ArrayList<>();
		int numCoeffs = 0;
		int firstPosCoeff = -1;
		// Iterate through monomials
		for (Monomial<BigInteger> jasMono : jasPoly) {
			java.math.BigInteger coeff = jasMono.coefficient().getVal();
			if (coeff.signum() != 0) {
				// Store (absolute) coefficient and sign/unity info
				java.math.BigInteger coeffAbs = coeff.signum() > 0 ? coeff : coeff.negate();
				coeffsAbs.add(coeffAbs);
				coeffsNeg.set(numCoeffs, coeff.signum() < 0);
				coeffsOne.set(numCoeffs, coeffAbs.equals(java.math.BigInteger.ONE));
				if (firstPosCoeff == -1 && coeff.signum() > 0 ) {
					firstPosCoeff = numCoeffs;
				}
				// Convert variable powers to Expression (or null if none)
				ExpVector jasExpo = jasMono.exponent();
				Expression exprPow = null;
				for (int var = 0; var < numVariables; var++) {
					int power = (int) jasExpo.getVal(var);
					if (power < 0) {
						throw new PrismException("Polynomials with negative powers not supported");
					}
					if (power > 0) {
						Expression exprVar = new ExpressionConstant(factory.getParameterName(var), TypeDouble.getInstance());
						if (power > 1) {
							exprVar = Expression.Pow(exprVar, Expression.Int(power));
						}
						exprPow = (exprPow == null) ? exprVar : Expression.Times(exprPow, exprVar);
					}
					/*for (; power > 0; power--) {
						// Build x^power as x*x*...*x
						Expression exprConst = new ExpressionConstant(factory.getParameterName(var), TypeDouble.getInstance());
						exprPow = (exprPow == null) ? exprConst : Expression.Times(exprPow, exprConst);
					}*/
				}
				exprPows.add(exprPow);
				numCoeffs++;
			}
		}

		// Convert to Expression
		Expression exprPoly = null, exprMono = null;
		// If possible, put a positive coefficient first
		int first = firstPosCoeff != -1 ? firstPosCoeff : 0;
		// First monomial (negation handled by negating coefficient)
		if (exprPows.get(first) == null) {
			// No variable powers - just use coefficient
			java.math.BigInteger coeff = coeffsNeg.get(first) ? coeffsAbs.get(first).negate() : coeffsAbs.get(first);
			exprMono = new ExpressionLiteral(TypeDouble.getInstance(), coeff, coeff.toString());
		} else {
			if (coeffsOne.get(first)) {
				// Coefficient = 1 (or -1)
				exprMono = coeffsNeg.get(first) ? Expression.Minus(exprPows.get(first)) : exprPows.get(first);
			} else {
				// Pre-multiply by non-1 coefficient
				java.math.BigInteger coeff = coeffsNeg.get(first) ? coeffsAbs.get(first).negate() : coeffsAbs.get(first);
				exprMono = Expression.Times(new ExpressionLiteral(TypeDouble.getInstance(), coeff, coeff.toString()), exprPows.get(first));
			}
		}
		exprPoly = exprMono;
		// Remaining monomials (negation handled by joining with minus)
		for (int i = 0; i < numCoeffs; i++) {
			if (i != first) {
				if (exprPows.get(i) == null) {
					// No variable powers - just use (absolute) coefficient
					exprMono = new ExpressionLiteral(TypeDouble.getInstance(), coeffsAbs.get(i), coeffsAbs.get(i).toString());
				} else {
					// Pre-multiply by (absolute) coefficient if non-1
					exprMono = coeffsOne.get(i) ? exprPows.get(i) : Expression.Times(new ExpressionLiteral(TypeDouble.getInstance(), coeffsAbs.get(i), coeffsAbs.get(i).toString()), exprPows.get(i));
				}
				exprPoly = coeffsNeg.get(i) ? Expression.Minus(exprPoly, exprMono) : Expression.Plus(exprPoly, exprMono);
			}
		}

		return exprPoly;
	}

	@Override
	public BigRational asBigRational() {
		if (isNaN()) {
			return BigRational.NAN;
		} else if (isInf()) {
			return BigRational.INF;
		} else if (isMInf()) {
			return BigRational.MINF;
		}
		BigRational[] point = new BigRational[factory.getNumVariables()];
		for (int dim = 0; dim < factory.getNumVariables(); dim++) {
			point[dim] = new BigRational(0);
		}
		return evaluate(new Point(point));
	}

	@Override
	public boolean isNaN() {
		return type == NAN;
	}

	@Override
	public boolean isInf() {
		return type == INF;
	}

	@Override
	public boolean isMInf() {
		return type == MINF;
	}

	@Override
	public boolean isOne() {
		if (type != NORMAL) {
			return false;
		}
		return jas.isONE();
	}

	@Override
	public boolean isZero() {
		if (type != NORMAL) {
			return false;
		}
		return jas.isZERO();
	}

	@Override
	public boolean isConstant()
	{
		// inf, NaN are constant
		if (type != NORMAL)
			return true;

		// special handling for ZERO, as jas.isConstant() returns false
		// for zero...
		if (jas.isZERO()) {
			return true;
		}

		return jas.isConstant();
	}


}
