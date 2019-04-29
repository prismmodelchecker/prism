//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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
 * Functional interface for a consumer of (state, value) pairs,
 * used in iteration over a StateValues vector, e.g., printing.
 */
@FunctionalInterface
public interface StateAndValueConsumer
{

	/**
	 * Accept a state/value pair.
	 * <br>
	 * The values of the state variables are provided as integers,
	 * with boolean values mapped to 0 and 1, respectively.
	 * @param varValues an integer array with the state variable values
	 * @param value the value for this state
	 * @param stateIndex the state index (-1 indicates that no index information is available)
	 */
	void accept(int[] varValues, double value, long stateIndex);

}
