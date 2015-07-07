//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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
 * Enumeration that represents one of the operators used in multi-objective
 * verification. Note that strict inequalities are missing as these are not
 * allowed in current implementation of multi-objective.
 */
public enum Operator {
	P_MAX(0), R_MAX(3), P_MIN(5), R_MIN(8), P_GE(2), R_GE(4), P_LE(7), R_LE(9);
	   
	private int intValue = -1;
	
	private Operator(int i)
	{
		this.intValue = i;
	}
	
   	/**
   	 * Returns {@code true} if op is one {@code Operator.P_MIN},
   	 * {@code Operator.R_MIN}, {@code Operator.P_LE}, or {@code Operator.R_LE}.
   	 */
	public static boolean isMinOrLe(Operator op)
	{
		switch (op)
		{
			case P_MIN:
			case R_MIN:
			case P_LE:
			case R_LE:
				return true;
			default:
				return false;
		}
	}
	
	/**
	 * Returns a number representing the current operator. These numbers
	 * are used in the C code.
	 */
	public int toNumber()
	{
		return this.intValue;
	}
}
