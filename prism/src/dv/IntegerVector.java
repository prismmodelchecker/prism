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

package dv;

import prism.*;
import jdd.*;
import odd.*;

/**
 * Class to encapsulate a vector of integer, stored natively.
 */
public class IntegerVector
{
	//------------------------------------------------------------------------------
	// Load JNI stuff from shared library
	//------------------------------------------------------------------------------

	static
	{
		try {
			System.loadLibrary("dv");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	//------------------------------------------------------------------------------
	// Instance variables/methods
	//------------------------------------------------------------------------------

	/**
	 * Vector contents (C/C++ pointer cast to a long)
	 */
	private long v;
	/**
	 * Size of vector
	 */
	private int n;
	
	// Constructors
	
	/**
	 * Create a new IntegerVector of size {@code size} with all entries set to 0.
	 * @throws PrismException if there is insufficient memory to create the array.
	 */
	public IntegerVector(int size) throws PrismException
	{
		v = IV_CreateZeroVector(size);
		if (v == 0) throw new PrismException("Out of memory");
		n = size;
	}
	
	private native long IV_CreateZeroVector(int n);
	
	/**
	 * Create a new IntegerVector from a pointer {@code vect} to an existing native integer array of size {@code size}.
	 */
	public IntegerVector(long vector, int size)
	{
		v = vector;
		n = size;
	}
	
	/**
	 * Create a new IntegerVector from an existing MTBDD representation of an array.
	 * <br>[ DEREFS: <i>none</i> ]
	 * @throws PrismException if number of states is too large (uses Java int based indices)
	 */
	public IntegerVector(JDDNode dd, JDDVars vars, ODDNode odd) throws PrismException
	{
		ODDUtils.checkInt(odd, "Can not create IntegerVector");

		v = IV_ConvertMTBDD(dd.ptr(), vars.array(), vars.n(), odd.ptr());
		n = (int)odd.getNumStates();
	}
	
	private native long IV_ConvertMTBDD(long dd, long vars, int num_vars, long odd);
	
	// Accessors
	
	/**
	 * Get the pointer to the native vector (C/C++ pointer cast to a long).
	 */
	public long getPtr()
	{
		return v;
	}

	/**
	 * Get the size of the vector.
	 */
	public int getSize()
	{
		return n;
	}

	/**
	 * Get element {@code i} of the vector.
	 */
	public int getElement(int i)
	{
		return IV_GetElement(v, n, i);
	}

	private native int IV_GetElement(long v, int n, int i);
	
	// Mutators
	
	/**
	 * Set element {@code i} of the vector to value {@code j}.
	 */
	public void setElement(int i, int j)
	{
		IV_SetElement(v, n, i, j);
	}

	private native void IV_SetElement(long v, int n, int i, int j);
	
	/**
	 * Set all elements of the vector to the same value {@code j}.
	 */
	public void setAllElements(int j)
	{
		IV_SetAllElements(v, n, j);
	}

	private native void IV_SetAllElements(long v, int n, int j);
	
	/**
	 * Clear storage of the vector.
	 */
	public void clear() 
	{
		IV_Clear(v);
	}

	private native void IV_Clear(long v);
	
	/**
	 * Print the (all elements, including non-zeros) to a log.
	 */
	public void print(PrismLog log)
	{
		int i, j;
		
		for (i = 0; i < n; i++) {
			j = IV_GetElement(v, n, i);
			log.print(j + " ");
		}
		log.println();
	}
}

//------------------------------------------------------------------------------
