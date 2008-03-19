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
	private Object result = null;
	private String resultString = "";
	
	public Result()
	{
		this.result = null;
		this.resultString = "";
	}
	
	public Result(Object result)
	{
		this.result = result;
		if (result instanceof Exception)
				this.resultString = "Error: "+((Exception)result).getMessage();
		else
				this.resultString = ""+result;
	}
	
	public Result(Object result, String resultString)
	{
		this.result = result;
		this.resultString = resultString;
	}
	
	public Object getResult()
	{
		return result;
	}

	public String getResultString()
	{
		return resultString;
	}

	public void setResultString(String resultString)
	{
		this.resultString = resultString;
	}
	
	public String toString()
	{
		return resultString;
	}
}
