//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import strat.Strategy;

/**
 * This class stores the result of a single verification/simulation.
 * It also stores other info/objects that may be passed back to the user,
 * e.g. an explanation of the result, counterexample, or optimal strategy.
 */
public class Result
{
	// The result of model checking
	private Object result;
	// Explanatory text for result (optional)
	private String explanation;
	// Counterexample (optional)
	private Object cex;
	// Strategy (optional)
	private Strategy strat;
	// Solution vector (optional)
	private StateVector vect;
	
	/**
	 * Construct an empty Result object.
	 */
	public Result()
	{
		this.result = null;
		this.explanation = null;
		this.cex = null;
		this.strat = null;
		this.vect = null;
	}
	
	/**
	 * Create a Result object based on a result.
	 */
	public Result(Object result)
	{
		this();
		setResult(result);
	}
	
	/**
	 * Set the result.
	 */
	public void setResult(Object result)
	{
		this.result = result;
	}
	
	/**
	 * Set the string of explanatory text (null denotes absent).
	 */
	public void setExplanation(String explanation)
	{
		this.explanation = explanation;
	}
	
	/**
	 * Set the counterexample (null denotes n/a).
	 */
	public void setCounterexample(Object cex)
	{
		this.cex = cex;
	}
	
	/**
	 * Set the strategy (null denotes n/a).
	 */
	public void setStrategy(Strategy strat)
	{
		this.strat = strat;
	}
	
	/**
	 * Set the result vector (null denotes n/a).
	 */
	public void setVector(StateVector vect)
	{
		this.vect = vect;
	}
	
	/**
	 * Get the result.
	 */
	public Object getResult()
	{
		return result;
	}

	/**
	 * Get the explanatory text string (null denotes absent).
	 */
	public String getExplanation()
	{
		return explanation;
	}

	/**
	 * Get the counterexample (null denotes n/a).
	 */
	public Object getCounterexample()
	{
		return cex;
	}
	
	/**
	 * Get the strategy (null denotes n/a).
	 */
	public Strategy getStrategy()
	{
		return strat;
	}
	
	/**
	 * Get the result vector (null denotes n/a).
	 */
	public StateVector getVector()
	{
		return vect;
	}
	
	/**
	 * Get a string of the result and (if present) explanatory text
	 */
	public String getResultString()
	{
		String s = result.toString();
		if (explanation != null)
			s += " (" + explanation +")";
		return s;
	}
	
	/**
	 * Get the string representation of the actual result value.
	 */
	public String toString()
	{
		return result == null ? "null" : result.toString();
	}
}
