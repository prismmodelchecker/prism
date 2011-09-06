//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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
 * This class stores an interval of numerical values.
 */
public class Interval
{
	// Lower/upper value
	public Object lower;
	public Object upper;
	
	/**
	 * Construct an Interval.
	 * (lower and upper should be of the same type: Integer or Double)
	 */
	public Interval(Object lower, Object upper)
	{
		this.lower = lower;
		this.upper = upper;
	}
	
	/**
	 * Construct an integer Interval.
	 */
	public Interval(Integer lower, Integer upper)
	{
		this.lower = lower;
		this.upper = upper;
	}
	
	/**
	 * Construct a double Interval.
	 */
	public Interval(Double lower, Double upper)
	{
		this.lower = lower;
		this.upper = upper;
	}
	
	@Override
	public String toString()
	{
		return "[" + lower + "," + upper + "]";
	}
}
