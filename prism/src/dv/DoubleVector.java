//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: Java wrapper class for double vector
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

public class DoubleVector
{
	//------------------------------------------------------------------------------
	// load jni stuff from shared library
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
	// cudd manager
	//------------------------------------------------------------------------------

	// cudd manager
	
	// jni method to set cudd manager for native code
	private static native void DV_SetCUDDManager(long ddm);
	public static void setCUDDManager()
	{
		DV_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	//------------------------------------------------------------------------------
	// instance variables/methods
	//------------------------------------------------------------------------------

	// data
	private long v; // vector (actually a C/C++ pointer cast to a long)
	private int n; // size
	
	// constructors
	
	private native long DV_CreateZeroVector(int n);
	public DoubleVector(int size) throws PrismException
	{
		v = DV_CreateZeroVector(size);
		if (v == 0) throw new PrismException("Out of memory");
		n = size;
	}
	
	public DoubleVector(long vector, int size)
	{
		v = vector;
		n = size;
	}
	
	private native long DV_ConvertMTBDD(long dd, long vars, int num_vars, long odd);
	public DoubleVector(JDDNode dd, JDDVars vars, ODDNode odd)
	{
		v = DV_ConvertMTBDD(dd.ptr(), vars.array(), vars.n(), odd.ptr());
		n = (int)(odd.getEOff() + odd.getTOff());
	}
	
	// get methods
	
	public long getPtr()
	{
		return v;
	}

	public int getSize()
	{
		return n;
	}

	// get element
	private native double DV_GetElement(long v, int n, int i);
	public double getElement(int i)
	{
		return DV_GetElement(v, n, i);
	}

	// set element
	private native void DV_SetElement(long v, int n, int i, double d);
	public void setElement(int i, double d)
	{
		DV_SetElement(v, n, i, d);
	}

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

	// filter vector using a bdd (set elements not in filter to 0)
	private native void DV_Filter(long v, long filter, long vars, int num_vars, long odd);
	public void filter(JDDNode filter, JDDVars vars, ODDNode odd)
	{
		DV_Filter(v, filter.ptr(), vars.array(), vars.n(), odd.ptr());
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
	public JDDNode getBDDFromInterval(String relOp, double bound, JDDVars vars, ODDNode odd)
	{
		JDDNode sol = null;
		
		if (relOp.equals(">=")) {
			sol = new JDDNode(
				DV_BDDGreaterThanEquals(v, bound, vars.array(), vars.n(), odd.ptr())
			);
		}
		else if (relOp.equals(">")) {
			sol = new JDDNode(
				DV_BDDGreaterThan(v, bound, vars.array(), vars.n(), odd.ptr())
			);
		}
		else if (relOp.equals("<=")) {
			sol = new JDDNode(
				DV_BDDLessThanEquals(v, bound, vars.array(), vars.n(), odd.ptr())
			);
		}
		else if (relOp.equals("<")) {
			sol = new JDDNode(
				DV_BDDLessThan(v, bound, vars.array(), vars.n(), odd.ptr())
			);
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
		
		sol = new JDDNode(
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
		
		sol = new JDDNode(
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
		
		sol = new JDDNode(
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
		
		sol = new JDDNode(
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
