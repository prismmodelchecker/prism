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

/**
 * This class stores the result of a single verification/simulation.
 */
public class Result
{
	// The result of model checking
	private Object result;
	// Explanatory text for result (optional)
	private String resultString;
	// Counterexample (optional)
	private Object cex;
	
	/**
	 * Construct an empty Result object.
	 */
	public Result()
	{
		this.result = null;
		this.resultString = "";
		this.cex = null;
	}
	
	/**
	 * Create a Result object based on a result.
	 * (Result string will be set automatically in some cases.)
	 */
	public Result(Object result)
	{
		this();
		setResult(result);
	}
	
	/**
	 * Create a Result object based on a result and a string of explanatory text.
	 */
	public Result(Object result, String resultString)
	{
		this();
		setResultAndString(result, resultString);
	}
	
	/**
	 * Set the result.
	 * (Result string will be set automatically in some cases.)
	 */
	public void setResult(Object result)
	{
		this.result = result;
		if (result instanceof Exception)
				this.resultString = "Error: "+((Exception)result).getMessage();
		else
				this.resultString = ""+result;
	}
	
	/**
	 * Set the result and the string of explanatory text.
	 */
	public void setResultAndString(Object result, String resultString)
	{
		this.result = result;
		this.resultString = resultString;
	}
	
	/**
	 * Set the string of explanatory text.
	 */
	public void setResultString(String resultString)
	{
		this.resultString = resultString;
	}
	
	/**
	 * Set the counterexample (null denotes n/a).
	 */
	public void setCounterexample(Object cex)
	{
		this.cex = cex;
	}
	
	/**
	 * Get the result.
	 */
	public Object getResult()
	{
		return result;
	}

	/**
	 * Get the result string (explanatory text).
	 */
	public String getResultString()
	{
		return resultString;
	}

	/**
	 * Get the counterexample (null denotes n/a).
	 */
	public Object getCounterexample()
	{
		return cex;
	}
	
	@Override
	public String toString()
	{
		return resultString;
	}
}
