//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
 * Interface for classes supporting read-access to state-indexed vectors of values. 
 */
public interface StateVector
{
	/**
	 * Return the size of the vector (i.e. the number of elements).
	 */
	public int getSize();

	/**
	 * Get the value of the ith element of the vector, as an Object.
	 */
	public Object getValue(int i) throws PrismNotSupportedException;
	
	/**
	 * Clear the vector, i.e. free any used memory.
	 */
	public void clear();
	
	/**
	 * Print vector to a log/file (non-zero/non-false entries only).
	 */
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException;
}
