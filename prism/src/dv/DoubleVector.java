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

import parser.ast.RelOp;
import prism.*;
import jdd.*;
import odd.*;

/**
 * Class to encapsulate a vector of doubles, stored natively.
 */
public class DoubleVector
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
	// CUDD manager
	//------------------------------------------------------------------------------

	// CUDD manager
	
	// JNI method to set CUDD manager for native code
	public static void setCUDDManager()
	{
		DV_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	private static native void DV_SetCUDDManager(long ddm);
	
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
	 * Create a new DoubleVector of size {@code size} with all entries set to 0.
	 * @throws PrismException if there is insufficient memory to create the array.
	 */
	public DoubleVector(int size) throws PrismException
	{
		v = DV_CreateZeroVector(size);
		if (v == 0) throw new PrismException("Out of memory");
		n = size;
	}
	
	private native long DV_CreateZeroVector(int n);
	
	/**
	 * Create a new DoubleVector from a pointer {@code vect} to an existing native double array of size {@code size}.
	 */
	public DoubleVector(long vector, int size)
	{
		v = vector;
		n = size;
	}
	
	/**
	 * Create a new DoubleVector from an existing MTBDD representation of an array.
	 * <br>[ DEREFS: <i>none</i> ]
	 * @throws PrismException if number of states is too large (uses Java int based indices)
	 */
	public DoubleVector(JDDNode dd, JDDVars vars, ODDNode odd) throws PrismException
	{
		long numStates = odd.getEOff() + odd.getTOff();
		if (numStates > Integer.MAX_VALUE) {
			throw new PrismNotSupportedException("Can not create DoubleVector with more than " + Integer.MAX_VALUE + " states, have " + numStates + " states");
		}
		v = DV_ConvertMTBDD(dd.ptr(), vars.array(), vars.n(), odd.ptr());
		n = (int)numStates;
	}
	
	private native long DV_ConvertMTBDD(long dd, long vars, int num_vars, long odd);

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
	public double getElement(int i)
	{
		return DV_GetElement(v, n, i);
	}

	private native double DV_GetElement(long v, int n, int i);
	
	// Mutators
	
	/**
	 * Set element {@code i} of the vector to value {@code d}.
	 */
	public void setElement(int i, double d)
	{
		DV_SetElement(v, n, i, d);
	}

	private native void DV_SetElement(long v, int n, int i, double d);
	
	/**
	 * Set all elements of the vector to the same value {@code d}.
	 */
	public void setAllElements(double d)
	{
		DV_SetAllElements(v, n, d);
	}

	private native void DV_SetAllElements(long v, int n, double d);
	
	// round off
	private native void DV_RoundOff(long v, int n, int places);
	public void roundOff(int places)
	{
		DV_RoundOff(v, n, places);
	}

	// subtract all values from 1
	private native void DV_SubtractFromOne(long v, int n);
	public void subtractFromOne() 
	{
		DV_SubtractFromOne(v, n);
	}

	// add another vector to this one
	private native void DV_Add(long v, int n, long v2);
	public void add(DoubleVector dv) 
	{
		DV_Add(v, n, dv.v);
	}

	// multiply vector by a constant
	private native void DV_TimesConstant(long v, int n, double d);
	public void timesConstant(double d) 
	{
		DV_TimesConstant(v, n, d);
	}

	// compute dot (inner) product of this and another vector
	private native double DV_DotProduct(long v, int n, long v2);
	public double dotProduct(DoubleVector dv) 
	{
		return DV_DotProduct(v, n, dv.v);
	}


	private native void DV_Filter(long v, long filter, double d, long vars, int num_vars, long odd);
	/**
	 * Filter vector using a bdd (set elements not in filter to d)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public void filter(JDDNode filter, double d, JDDVars vars, ODDNode odd)
	{
		DV_Filter(v, filter.ptr(), d, vars.array(), vars.n(), odd.ptr());
	}

	/**
	 * Filter vector using a bdd (set elements not in filter to 0)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public void filter(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		DV_Filter(v, filter.ptr(), 0.0, vars.array(), vars.n(), odd.ptr());
	}
	
	// apply max operator, i.e. v[i] = max(v[i], v2[i]), where v2 is an mtbdd
	private native void DV_MaxMTBDD(long v, long v2, long vars, int num_vars, long odd);
	public void maxMTBDD(JDDNode v2, JDDVars vars, ODDNode odd)
	{
		DV_MaxMTBDD(v, v2.ptr(), vars.array(), vars.n(), odd.ptr());
	}

	// clear (free memory)
	private native void DV_Clear(long v);
	public void clear() 
	{
		DV_Clear(v);
	}

	// get number of non zeros
	private native int DV_GetNNZ(long v, int n);
	public int getNNZ()
	{
		return DV_GetNNZ(v, n);
	}

	// get value of first element in BDD filter
	private native double DV_FirstFromBDD(long v, long filter, long vars, int num_vars, long odd);
	public double firstFromBDD(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		return DV_FirstFromBDD(v, filter.ptr(), vars.array(), vars.n(), odd.ptr());
	}
	
	// get min value over BDD filter
	private native double DV_MinOverBDD(long v, long filter, long vars, int num_vars, long odd);
	public double minOverBDD(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		return DV_MinOverBDD(v, filter.ptr(), vars.array(), vars.n(), odd.ptr());
	}
	
	// get max value over BDD filter
	private native double DV_MaxOverBDD(long v, long filter, long vars, int num_vars, long odd);
	public double maxOverBDD(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		return DV_MaxOverBDD(v, filter.ptr(), vars.array(), vars.n(), odd.ptr());
	}

	private native double DV_MaxFiniteOverBDD(long v, long filter, long vars, int num_vars, long odd);
	public double maxFiniteOverBDD(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		return DV_MaxOverBDD(v, filter.ptr(), vars.array(), vars.n(), odd.ptr());
	}

	// sum elements of vector according to a bdd (used for csl steady state operator)
	private native double DV_SumOverBDD(long v, long filter, long vars, int num_vars, long odd);
	public double sumOverBDD(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		return DV_SumOverBDD(v, filter.ptr(), vars.array(), vars.n(), odd.ptr());
	}
	
	// do a weighted sum of the elements of a double array and the values the mtbdd passed in
	// (used for csl reward steady state operator)
	private native double DV_SumOverMTBDD(long v, long mult, long vars, int num_vars, long odd);
	public double sumOverMTBDD(JDDNode mult, JDDVars vars, ODDNode odd)
	{
		return DV_SumOverMTBDD(v, mult.ptr(), vars.array(), vars.n(), odd.ptr());
	}
	
	// sum up the elements of a double array, over a subset of its dd vars
	// the dd var subset must be a continuous range of vars, identified by indices: first_var, last_var
	// store the result in a new DoubleVector (whose indices are given by odd2)
	private native void DV_SumOverDDVars(long vec, long vec2, long vars, int num_vars, int first_var, int last_var, long odd, long odd2);
	// throws PrismException on out-of-memory
	public DoubleVector sumOverDDVars(JDDVars vars, ODDNode odd, ODDNode odd2, int first_var, int last_var) throws PrismException
	{
		DoubleVector dv2 = new DoubleVector((int)(odd2.getEOff() + odd2.getTOff()));
		DV_SumOverDDVars(v, dv2.v, vars.array(), vars.n(), first_var, last_var, odd.ptr(), odd2.ptr());
		return dv2;
	}
	
	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as relational operator and bound)
	 */
	public JDDNode getBDDFromInterval(String relOpString, double bound, JDDVars vars, ODDNode odd)
	{
		return getBDDFromInterval(RelOp.parseSymbol(relOpString), bound, vars, odd);
	}
	
	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as relational operator and bound)
	 */
	public JDDNode getBDDFromInterval(RelOp relOp, double bound, JDDVars vars, ODDNode odd)
	{
		JDDNode sol = null;
		
		switch (relOp) {
		case GEQ:
			sol = JDD.ptrToNode(
				DV_BDDGreaterThanEquals(v, bound, vars.array(), vars.n(), odd.ptr())
			);
			break;
		case GT:
			sol = JDD.ptrToNode(
				DV_BDDGreaterThan(v, bound, vars.array(), vars.n(), odd.ptr())
			);
			break;
		case LEQ:
			sol = JDD.ptrToNode(
				DV_BDDLessThanEquals(v, bound, vars.array(), vars.n(), odd.ptr())
			);
			break;
		case LT:
			sol = JDD.ptrToNode(
				DV_BDDLessThan(v, bound, vars.array(), vars.n(), odd.ptr())
			);
			break;
		default:
			// Don't handle
		}
		
		return sol;
	}
	private native long DV_BDDGreaterThanEquals(long v, double bound, long vars, int num_vars, long odd);
	private native long DV_BDDGreaterThan(long v, double bound, long vars, int num_vars, long odd);
	private native long DV_BDDLessThanEquals(long v, double bound, long vars, int num_vars, long odd);
	private native long DV_BDDLessThan(long v, double bound, long vars, int num_vars, long odd);
	
	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as lower/upper bound)
	 */
	public JDDNode getBDDFromInterval(double lo, double hi, JDDVars vars, ODDNode odd)
	{
		JDDNode sol;
		
		sol = JDD.ptrToNode(
			DV_BDDInterval(v, lo, hi, vars.array(), vars.n(), odd.ptr())
		);
		
		return sol;
	}
	private native long DV_BDDInterval(long v, double lo, double hi, long vars, int num_vars, long odd);
	
	/**
	 * 	Generate BDD for states whose value is close to 'value'
	 * (within absolute error 'epsilon')
	 */
	public JDDNode getBDDFromCloseValueAbs(double value, double epsilon, JDDVars vars, ODDNode odd)
	{
		JDDNode sol;
		
		sol = JDD.ptrToNode(
			DV_BDDCloseValueAbs(v, value, epsilon, vars.array(), vars.n(), odd.ptr())
		);
		
		return sol;
	}
	private native long DV_BDDCloseValueAbs(long v, double value, double epsilon, long vars, int num_vars, long odd);
	
	/**
	 * 	Generate BDD for states whose value is close to 'value'
	 * (within relative error 'epsilon')
	 */
	public JDDNode getBDDFromCloseValueRel(double value, double epsilon, JDDVars vars, ODDNode odd)
	{
		JDDNode sol;
		
		sol = JDD.ptrToNode(
			DV_BDDCloseValueRel(v, value, epsilon, vars.array(), vars.n(), odd.ptr())
		);
		
		return sol;
	}
	private native long DV_BDDCloseValueRel(long v, double value, double epsilon, long vars, int num_vars, long odd);
	
	/**
	 * Convert to an MTBDD representation.
	 */
	private native long DV_ConvertToMTBDD(long v, long vars, int num_vars, long odd);
	public JDDNode convertToMTBDD(JDDVars vars, ODDNode odd)
	{
		JDDNode sol;
		
		sol = JDD.ptrToNode(
			DV_ConvertToMTBDD(v, vars.array(), vars.n(), odd.ptr())
		);
		
		return sol;
	}
	
	// print (all, including nonzeros)
	public void print(PrismLog log)
	{
		int i;
		double d;
		
		for (i = 0; i < n; i++) {
			d = DV_GetElement(v, n, i);
			log.print(d + " ");
		}
		log.println();
	}
}

//------------------------------------------------------------------------------
