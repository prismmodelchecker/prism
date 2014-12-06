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

import edu.jas.arith.BigInteger;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.Monomial;
import edu.jas.ufd.Quotient;

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
		if (isNaN()) {
			return "NaN";
		} else if (isInf()) {
			return "Inf";
		} else if (isMInf()) {
			return "MInf";
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
		if (other.isZero()) {
			if (this.isZero()) {
				return factory.getNaN();
			} else {
				return factory.getInf();
			}
		}
		if (this.isZero()) {
			return factory.getZero();
		}
		return new JasFunction((JasFunctionFactory) factory, jas.divide(((JasFunction) other).jas), NORMAL);
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
}
