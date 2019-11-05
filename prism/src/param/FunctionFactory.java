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

import java.util.HashMap;

/**
 * Generates new functions, stores valid ranges of parameters, etc.
 * 
 * @see Function
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
public abstract class FunctionFactory {
	/** names of parameters */
	protected String[] parameterNames;
	/** lower bounds of parameters */
	protected BigRational[] lowerBounds;
	/** upper bounds of parameters */
	protected BigRational[] upperBounds;
	/** maps variable name to index in {@code parameterNames},
	 * {@code lowerBounds} and {@code upperBounds}
	 */
	protected HashMap<String, Integer> varnameToInt;
	
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
}
