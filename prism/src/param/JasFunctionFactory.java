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

import java.util.Arrays;
import java.util.Collections;
import edu.jas.arith.BigInteger;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.ufd.Quotient;
import edu.jas.ufd.QuotientRing;

/**
 * Function factory class for use with {@code JasFunction}.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see FunctionFactory
 * @see JasFunction
 */
final class JasFunctionFactory extends FunctionFactory {
	private GenPolynomialRing<BigInteger> jasPolyRing;
	private QuotientRing<BigInteger> jasQuotRing;
	private JasFunction zero;
	private JasFunction one;
	private JasFunction nan;
	private JasFunction inf;
	private JasFunction minf;
	private JasFunction[] parameters;

	/**
	 * Creates a new function factory.
	 * 
	 * @param parameterNames names of parameters
	 * @param lowerBounds lower bounds of parameters
	 * @param upperBounds upper bounds of parameters
	 */
	JasFunctionFactory(String[] parameterNames, BigRational[] lowerBounds, BigRational[] upperBounds)
	{
		super(parameterNames, lowerBounds, upperBounds);
		String[] pNameReversed = new String[parameterNames.length];
		System.arraycopy(parameterNames, 0, pNameReversed, 0, parameterNames.length);
		Collections.reverse(Arrays.asList(pNameReversed));
		this.parameterNames = parameterNames;
		
		BigInteger fac = new BigInteger();
		jasPolyRing = new GenPolynomialRing<BigInteger>(fac,pNameReversed.length,pNameReversed);
		jasQuotRing = new QuotientRing<BigInteger>(jasPolyRing);
		one = new JasFunction(this, jasQuotRing.getONE(), JasFunction.NORMAL);
		zero = new JasFunction(this, jasQuotRing.getZERO(), JasFunction.NORMAL);
		nan = new JasFunction(this, jasQuotRing.getZERO(), JasFunction.NAN);
		inf = new JasFunction(this, jasQuotRing.getZERO(), JasFunction.INF);
		minf = new JasFunction(this, jasQuotRing.getZERO(), JasFunction.MINF);
		parameters = new JasFunction[parameterNames.length];
		for (int param = 0; param < parameterNames.length; param++) {
			parameters[param] = new JasFunction(this, jasQuotRing.parse(parameterNames[param]), JasFunction.NORMAL);
		}
	}	

	@Override
	public Function getOne()
	{
		return one;
	}
	
	@Override
	public Function getZero()
	{
		return zero;
	}

	@Override
	public Function getNaN()
	{
		return nan;
	}

	@Override
	public Function getInf()
	{
		return inf;
	}
	
	@Override
	public Function getMInf()
	{
		return minf;
	}

	/**
	 * Get JAS ring for rational functions used by this factory.
	 * 
	 * @return JAS ring for rational functions used by this factory
	 */
	QuotientRing<BigInteger> getJasQuotRing()
	{
		return jasQuotRing;
	}
	
	/**
	 * Get JAS ring for polynomials used by this factory.
	 * In JAS, rational functions are built on polynomial rings, and this
	 * function returns this corresponding ring.
	 * 
	 * @return JAS ring for polynomials used by this factory
	 */
	GenPolynomialRing<BigInteger> getJasPolyRing()
	{
		return jasPolyRing;
	}

	@Override
	public Function fromBigRational(BigRational from) {
		if (from.isSpecial()) {
			if (from.isInf())
				return getInf();
			else if (from.isMInf())
				return getMInf();
			else if (from.isNaN())
				return getNaN();
			else
				throw new RuntimeException("Implementation error");
		}

		Quotient<BigInteger> result = jasQuotRing.fromInteger(from.getNum());
		Quotient<BigInteger> den = jasQuotRing.fromInteger(from.getDen());
		result = result.divide(den);
		
		return new JasFunction(this, result, JasFunction.NORMAL);
	}

	@Override
	public Function getVar(int var) {
		return parameters[var];
	}
}
