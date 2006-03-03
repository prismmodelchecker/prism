//==============================================================================
//	
//	File:		JDD.java
//	Date:		22/5/00
//	Author:		Dave Parker
//	
//------------------------------------------------------------------------------
//	
//	Copyright (c) 2002-2004, Dave Parker
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

package jdd;

import java.io.*;
import java.util.Vector;

public class JDD
{
	// dd library functions
	public static native int GetCUDDManager();
	// dd
	private static native void DD_SetOutputStream(int fp);
	private static native int DD_GetOutputStream();
	// dd_cudd
	private static native void DD_InitialiseCUDD();
	private static native void DD_InitialiseCUDD(long max_mem, double epsilon);
	private static native void DD_SetCUDDMaxMem(long max_mem);
	private static native void DD_SetCUDDEpsilon(double epsilon);
	private static native void DD_CloseDownCUDD(boolean check);
	private static native void DD_Ref(int dd);
	private static native void DD_Deref(int dd);
	private static native void DD_PrintCacheInfo();
	// dd_basics
	private static native int DD_Create();
	private static native int DD_Constant(double value);
	private static native int DD_PlusInfinity();
	private static native int DD_MinusInfinity();
	private static native int DD_Var(int i);
	private static native int DD_Not(int dd);
	private static native int DD_Or(int dd1, int dd2);
	private static native int DD_And(int dd1, int dd2);
	private static native int DD_Xor(int dd1, int dd2);
	private static native int DD_Implies(int dd1, int dd2);
	private static native int DD_Apply(int op, int dd1, int dd2);
	private static native int DD_MonadicApply(int op, int dd);
	private static native int DD_Restrict(int dd, int cube);
	private static native int DD_ITE(int dd1, int dd2, int dd3);
	// dd_vars
	private static native int DD_PermuteVariables(int dd, int old_vars, int new_vars, int num_vars);
	private static native int DD_SwapVariables(int dd, int old_vars, int new_vars, int num_vars);
	// dd_abstr
	private static native int DD_ThereExists(int dd, int vars, int num_vars);
	private static native int DD_ForAll(int dd, int vars, int num_vars);
	private static native int DD_SumAbstract(int dd, int vars, int num_vars);
	private static native int DD_ProductAbstract(int dd, int vars, int num_vars);
	private static native int DD_MinAbstract(int dd, int vars, int num_vars);
	private static native int DD_MaxAbstract(int dd, int vars, int num_vars);
	// dd_term
	private static native int DD_GreaterThan(int dd, double threshold);
	private static native int DD_GreaterThanEquals(int dd, double threshold);
	private static native int DD_LessThan(int dd, double threshold);
	private static native int DD_LessThanEquals(int dd, double threshold);
	private static native int DD_Equals(int dd, double value);
	private static native int DD_Interval(int dd, double lower, double upper);
	private static native int DD_RoundOff(int dd, int places);
	private static native boolean DD_EqualSupNorm(int dd1, int dd2, double epsilon);
	private static native double DD_FindMin(int dd);
	private static native double DD_FindMax(int dd);
	private static native int DD_RestrictToFirst(int dd, int vars, int num_vars);
	// dd_info
	private static native int DD_GetNumNodes(int dd);
	private static native int DD_GetNumTerminals(int dd);
	private static native double DD_GetNumMinterms(int dd, int num_vars);
	private static native double DD_GetNumPaths(int dd);
	private static native void DD_PrintInfo(int dd, int num_vars);
	private static native void DD_PrintInfoBrief(int dd, int num_vars);
	private static native void DD_PrintSupport(int dd);
	private static native int DD_GetSupport(int dd);
	private static native void DD_PrintTerminals(int dd);
	private static native void DD_PrintTerminalsAndNumbers(int dd, int num_vars);
	// dd_matrix
	private static native int DD_SetVectorElement(int dd, int vars, int num_vars, long index, double value);
	private static native int DD_SetMatrixElement(int dd, int rvars, int num_rvars, int cvars, int num_cvars, long rindex, long cindex, double value);
	private static native int DD_Set3DMatrixElement(int dd, int rvars, int num_rvars, int cvars, int num_cvars, int lvars, int num_lvars, long rindex, long cindex, long lindex, double value);
	private static native double DD_GetVectorElement(int dd, int vars, int num_vars, long index);
	private static native int DD_Identity(int rvars, int cvars, int num_vars);
	private static native int DD_Transpose(int dd, int rvars, int cvars, int num_vars);
	private static native int DD_MatrixMultiply(int dd1, int dd2, int vars, int num_vars, int method);
	private static native void DD_PrintVector(int dd, int vars, int num_vars, int accuracy);
	private static native void DD_PrintMatrix(int dd, int rvars, int num_rvars, int cvars, int num_cvars, int accuracy);
	private static native void DD_PrintVectorFiltered(int dd, int filter, int vars, int num_vars, int accuracy);
	// dd_export
	private static native void DD_ExportDDToDotFile(int dd, String filename);
	private static native void DD_ExportDDToDotFileLabelled(int dd, String filename, Vector var_names);
	private static native void DD_ExportMatrixToPPFile(int dd, int rvars, int num_rvars, int cvars, int num_cvars, String filename);
	private static native void DD_ExportMatrixToMatlabFile(int dd, int rvars, int num_rvars, int cvars, int num_cvars, String name, String filename);
	private static native void DD_ExportMatrixToSpyFile(int dd, int rvars, int num_rvars, int cvars, int num_cvars, int depth, String filename);
	// misc
	private static native void DD_Printf(String text);

	static
	{
		try {
			System.loadLibrary("jdd");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	// apply operations
	public static final int PLUS = 1;
	public static final int MINUS = 2;
	public static final int TIMES = 3;
	public static final int DIVIDE = 4;
	public static final int MIN = 5;
	public static final int MAX = 6;
	public static final int EQUALS = 7;
	public static final int NOTEQUALS = 8;
	public static final int GREATERTHAN = 9;
	public static final int GREATERTHANEQUALS = 10;
	public static final int LESSTHAN = 11;
	public static final int LESSTHANEQUALS = 12;
	public static final int FLOOR = 13;
	public static final int CEIL = 14;
	public static final int POW = 15;
	public static final int MOD = 16;

	// print vector/matrix accuracy
	public static final int ZERO_ONE = 1;
	public static final int LOW = 2;
	public static final int NORMAL = 3;
	public static final int HIGH = 4;
	public static final int LIST = 5;
	
	// matrix multiply methods
	public static final int CMU = 1;
	public static final int BOULDER = 2;
	
	// constant dds
	public static JDDNode ZERO;
	public static JDDNode ONE;
		
	// wrapper methods for dd
	
	public static void SetOutputStream(int fp)
	{
		DD_SetOutputStream(fp);
	}
	
	public static int GetOutputStream()
	{
		return DD_GetOutputStream();
	}
	
	// wrapper methods for dd_cudd

	// initialise cudd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void InitialiseCUDD()
	{
		DD_InitialiseCUDD();
		ZERO = Constant(0);
		ONE = Constant(1);
	}
		
	// initialise cudd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void InitialiseCUDD(long max_mem, double epsilon)
	{
		DD_InitialiseCUDD(max_mem, epsilon);
		ZERO = Constant(0);
		ONE = Constant(1);
	}
		
	// set cudd max memory
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void SetCUDDMaxMem(long max_mem)
	{
		DD_SetCUDDMaxMem(max_mem);
	}
		
	// set cudd epsilon
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void SetCUDDEpsilon(double epsilon)
	{
		DD_SetCUDDEpsilon(epsilon);
	}
		
	// close down cudd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void CloseDownCUDD() { CloseDownCUDD(true); }
	public static void CloseDownCUDD(boolean check)
	{
		Deref(ZERO);
		Deref(ONE);
		DD_CloseDownCUDD(check);
	}
	
	// reference dd
	// [ REFS: dd, DEREFS: <none> ]
	
	public static void Ref(JDDNode dd)
	{
		DD_Ref(dd.ptr());
	}
	
	// dereference dd
	// [ REFS: <none>, DEREFS: dd ]
	
	public static void Deref(JDDNode dd)
	{
		DD_Deref(dd.ptr());
	}

	// print cudd cache info
	// [ REFS: <none>, DEREFS: <none> ]

	public static void PrintCacheInfo()
	{
		DD_PrintCacheInfo();
	}
	
	// wrapper methods for dd_basics

	// create new (zero) dd
	// [ REFS: <result>, DEREFS: <none> ]

	public static JDDNode Create()
	{
		return new JDDNode(DD_Create());
	}
	
	// create new constant dd
	// [ REFS: <result>, DEREFS: <none> ]

	public static JDDNode Constant(double value)
	{
		return new JDDNode(DD_Constant(value));
	}
		
	// create new constant (plus infinity)
	// [ REFS: <result>, DEREFS: <none> ]

	public static JDDNode PlusInfinity()
	{
		return new JDDNode(DD_PlusInfinity());
	}
	
	// create new constant (minus infinity)
	// [ REFS: <result>, DEREFS: <none> ]

	public static JDDNode MinusInfinity()
	{
		return new JDDNode(DD_MinusInfinity());
	}
	
	// create new variable dd (1 if var i is true, 0 if not)
	// [ REFS: <result>, DEREFS: <none> ]

	public static JDDNode Var(int i)
	{
		return new JDDNode(DD_Var(i));
	}
	
	// not of dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode Not(JDDNode dd)
	{
		return new JDDNode(DD_Not(dd.ptr()));
	}
	
	// or of dd1, dd2
	// [ REFS: <result>, DEREFS: dd1, dd2 ]

	public static JDDNode Or(JDDNode dd1, JDDNode dd2)
	{
		return new JDDNode(DD_Or(dd1.ptr(), dd2.ptr()));
	}
	
	// and of dd1, dd2
	// [ REFS: <result>, DEREFS: dd1, dd2 ]

	public static JDDNode And(JDDNode dd1, JDDNode dd2)
	{
		return new JDDNode(DD_And(dd1.ptr(), dd2.ptr()));
	}
	
	// xor of dd1, dd2
	// [ REFS: <result>, DEREFS: dd1, dd2 ]

	public static JDDNode Xor(JDDNode dd1, JDDNode dd2)
	{
		return new JDDNode(DD_Xor(dd1.ptr(), dd2.ptr()));
	}
	
	// implies of dd1, dd2
	// [ REFS: <result>, DEREFS: dd1, dd2 ]

	public static JDDNode Implies(JDDNode dd1, JDDNode dd2)
	{
		return new JDDNode(DD_Implies(dd1.ptr(), dd2.ptr()));
	}
	
	// generic apply operation
	// [ REFS: <result>, DEREFS: dd1, dd2 ]

	public static JDDNode Apply(int op, JDDNode dd1, JDDNode dd2)
	{
		return new JDDNode(DD_Apply(op, dd1.ptr(), dd2.ptr()));
	}
	
	// generic monadic apply operation
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode MonadicApply(int op, JDDNode dd)
	{
		return new JDDNode(DD_MonadicApply(op, dd.ptr()));
	}
	
	// restrict dd based on cube
	// [ REFS: <result>, DEREFS: dd, cube ]

	public static JDDNode Restrict(JDDNode dd, JDDNode cube)
	{
		return new JDDNode(DD_Restrict(dd.ptr(), cube.ptr()));
	}
	
	// ITE (if-then-else) operation
	// [ REFS: <result>, DEREFS: dd1, dd2, dd3 ]

	public static JDDNode ITE(JDDNode dd1, JDDNode dd2, JDDNode dd3)
	{
		return new JDDNode(DD_ITE(dd1.ptr(), dd2.ptr(), dd3.ptr()));
	}
		
	// wrapper methods for dd_vars
	
	// permute (->) variables in dd (cf. swap)
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode PermuteVariables(JDDNode dd, JDDVars old_vars, JDDVars new_vars)
	{
		return new JDDNode(DD_PermuteVariables(dd.ptr(), old_vars.array(), new_vars.array(), old_vars.n()));
	}

	// swap (<->) variables in dd (cf. permute)
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode SwapVariables(JDDNode dd, JDDVars old_vars, JDDVars new_vars)
	{
		return new JDDNode(DD_SwapVariables(dd.ptr(), old_vars.array(), new_vars.array(), old_vars.n()));
	}

	// wrapper methods for dd_abstr

	// existential abstraction of vars from dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode ThereExists(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_ThereExists(dd.ptr(), vars.array(), vars.n()));
	}
	
	// universal abstraction of vars from dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode ForAll(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_ForAll(dd.ptr(), vars.array(), vars.n()));
	}
	
	// sum abstraction of vars from dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode SumAbstract(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_SumAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	// product abstraction of vars from dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode ProductAbstract(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_ProductAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	// min abstraction of vars from dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode MinAbstract(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_MinAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	// max abstraction of vars from dd
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode MaxAbstract(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_MaxAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	// wrapper methods for dd_term

	// converts dd to a 0-1 dd, based on the interval (threshold, +inf)
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode GreaterThan(JDDNode dd, double threshold)
	{
		return new JDDNode(DD_GreaterThan(dd.ptr(), threshold));
	}
	
	// converts dd to a 0-1 dd, based on the interval [threshold, +inf)
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode GreaterThanEquals(JDDNode dd, double threshold)
	{
		return new JDDNode(DD_GreaterThanEquals(dd.ptr(), threshold));
	}
	
	// converts dd to a 0-1 dd, based on the interval (-inf, threshold)
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode LessThan(JDDNode dd, double threshold)
	{
		return new JDDNode(DD_LessThan(dd.ptr(), threshold));
	}
	
	// converts dd to a 0-1 dd, based on the interval (-inf, threshold]
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode LessThanEquals(JDDNode dd, double threshold)
	{
		return new JDDNode(DD_LessThanEquals(dd.ptr(), threshold));
	}
	
	// converts dd to a 0-1 dd, based on the interval [threshold, threshold]
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode Equals(JDDNode dd, double value)
	{
		return new JDDNode(DD_Equals(dd.ptr(), value));
	}
	
	// converts dd to a 0-1 dd, based on the interval [lower, upper]
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode Interval(JDDNode dd, double lower, double upper)
	{
		return new JDDNode(DD_Interval(dd.ptr(), lower, upper));
	}
	
	// rounds terminals in dd to a certain number of decimal places
	// [ REFS: <result>, DEREFS: dd ]

	public static JDDNode RoundOff(JDDNode dd, int places)
	{
		return new JDDNode(DD_RoundOff(dd.ptr(), places));
	}
	
	// returns true if sup norm of dd1 and dd2 is less than epsilon, returns false otherwise
	// [ REFS: <none>, DEREFS: <none> ]

	public static boolean EqualSupNorm(JDDNode dd1, JDDNode dd2, double epsilon)
	{
		return DD_EqualSupNorm(dd1.ptr(), dd2.ptr(), epsilon);
	}
	
	// returns minimum terminal in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static double FindMin(JDDNode dd)
	{
		return DD_FindMin(dd.ptr());
	}
	
	// returns maximum terminal in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static double FindMax(JDDNode dd)
	{
		return DD_FindMax(dd.ptr());
	}
	
	// returns dd restricted to first non-zero path (cube)
	// [ REFS: <result>, DEREFS: <dd> ]
	
	public static JDDNode RestrictToFirst(JDDNode dd, JDDVars vars)
	{
		return new JDDNode(DD_RestrictToFirst(dd.ptr(), vars.array(), vars.n()));
	}

	// wrapper methods for dd_info

	// returns number of nodes in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static int GetNumNodes(JDDNode dd)
	{
		return DD_GetNumNodes(dd.ptr());
	}
	
	// returns number of terminals in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static int GetNumTerminals(JDDNode dd)
	{
		return DD_GetNumTerminals(dd.ptr());
	}
	
	// returns number of minterms in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static double GetNumMinterms(JDDNode dd, int num_vars)
	{
		return DD_GetNumMinterms(dd.ptr(), num_vars);
	}
	
	// get number of minterms as a string (have to store as a double
	// because can be very big but want to print out as a decimal)
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static String GetNumMintermsString(JDDNode dd, int num_vars)
	{
		double minterms;
		
		minterms = GetNumMinterms(dd, num_vars);
		if (minterms <= Long.MAX_VALUE) {
			return "" + (long)minterms;
		}
		else {
			return "" + minterms;
		}
	}

	// returns number of paths in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static double GetNumPaths(JDDNode dd)
	{
		return DD_GetNumPaths(dd.ptr());
	}
	
	// get number of paths as a string (have to store as a double
	// because can be very big but want to print out as a decimal)
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static String GetNumPathsString(JDDNode dd)
	{
		double paths;
		
		paths = GetNumPaths(dd);
		if (paths <= Long.MAX_VALUE) {
			return "" + (long)paths;
		}
		else {
			return "" + paths;
		}
	}
	
	// prints out info for dd (nodes, terminals, minterms)
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintInfo(JDDNode dd, int num_vars)
	{		
		DD_PrintInfo(dd.ptr(), num_vars);
	}
	
	// prints out compact info for dd (nodes, terminals, minterms)
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintInfoBrief(JDDNode dd, int num_vars)
	{
		DD_PrintInfoBrief(dd.ptr(), num_vars);
	}
	
	// prints out support for dd (all dd variables present)
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintSupport(JDDNode dd)
	{
		DD_PrintSupport(dd.ptr());
	}
	
	// returns support for dd (all dd variables present) as a cube of the dd vars
	// [ REFS: <result>, DEREFS: <none> ]
	
	public static JDDNode GetSupport(JDDNode dd)
	{
		return new JDDNode(DD_GetSupport(dd.ptr()));
	}
	
	// print out all values of all terminals in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintTerminals(JDDNode dd)
	{
		DD_PrintTerminals(dd.ptr());
	}
	
	// print out all values of all terminals (and number of each) in dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintTerminalsAndNumbers(JDDNode dd, int num_vars)
	{
		DD_PrintTerminalsAndNumbers(dd.ptr(), num_vars);
	}

	// wrapper methods for dd_matrix

	// sets element in vector dd
	// [ REFS: <result>, DEREFS: <dd> ]
	
	public static JDDNode SetVectorElement(JDDNode dd, JDDVars vars, long index, double value)
	{
		return new JDDNode(DD_SetVectorElement(dd.ptr(), vars.array(), vars.n(), index, value));
	}
	
	// sets element in matrix dd
	// [ REFS: <result>, DEREFS: <dd> ]
	
	public static JDDNode SetMatrixElement(JDDNode dd, JDDVars rvars, JDDVars cvars, long rindex, long cindex, double value)
	{
		return new JDDNode(DD_SetMatrixElement(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), rindex, cindex, value));
	}
	
	// sets element in 3d matrix dd
	// [ REFS: <result>, DEREFS: <dd> ]
	
	public static JDDNode Set3DMatrixElement(JDDNode dd, JDDVars rvars, JDDVars cvars, JDDVars lvars, long rindex, long cindex, long lindex, double value)
	{
		return new JDDNode(DD_Set3DMatrixElement(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), lvars.array(), lvars.n(), rindex, cindex, lindex, value));
	}
	
	// get element in vector dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static double GetVectorElement(JDDNode dd, JDDVars vars, long index)
	{
		return DD_GetVectorElement(dd.ptr(), vars.array(), vars.n(), index);
	}
	
	// creates dd for identity matrix
	// [ REFS: <result>, DEREFS: <none> ]
	
	public static JDDNode Identity(JDDVars rvars, JDDVars cvars)
	{
		return new JDDNode(DD_Identity(rvars.array(), cvars.array(), rvars.n()));
	}
	
	// returns transpose of matrix dd
	// [ REFS: <result>, DEREFS: <dd> ]
	
	public static JDDNode Transpose(JDDNode dd, JDDVars rvars, JDDVars cvars)
	{
		return new JDDNode(DD_Transpose(dd.ptr(), rvars.array(), cvars.array(), rvars.n()));
	}
	
	// returns matrix multiplication of matrices dd1 and dd2
	// [ REFS: <result>, DEREFS: <dd1, dd2> ]
	
	public static JDDNode MatrixMultiply(JDDNode dd1, JDDNode dd2, JDDVars vars, int method)
	{
		return new JDDNode(DD_MatrixMultiply(dd1.ptr(), dd2.ptr(), vars.array(), vars.n(), method));
	}
	
	// prints out vector dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintVector(JDDNode dd, JDDVars vars)
	{
		DD_PrintVector(dd.ptr(), vars.array(), vars.n(), NORMAL);
	}
	
	// prints out vector dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintVector(JDDNode dd, JDDVars vars, int accuracy)
	{
		DD_PrintVector(dd.ptr(), vars.array(), vars.n(), accuracy);
	}
	
	// prints out matrix dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintMatrix(JDDNode dd, JDDVars rvars, JDDVars cvars)
	{
		DD_PrintMatrix(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), NORMAL);
	}
	
	// prints out matrix dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintMatrix(JDDNode dd, JDDVars rvars, JDDVars cvars, int accuracy)
	{
		DD_PrintMatrix(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), accuracy);
	}
	
	// prints out vector dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintVectorFiltered(JDDNode dd, JDDNode filter, JDDVars vars)
	{
		DD_PrintVectorFiltered(dd.ptr(), filter.ptr(), vars.array(), vars.n(), NORMAL);
	}
	
	// prints out vector dd
	// [ REFS: <none>, DEREFS: <none> ]
	
	public static void PrintVectorFiltered(JDDNode dd, JDDNode filter, JDDVars vars, int accuracy)
	{
		DD_PrintVectorFiltered(dd.ptr(), filter.ptr(), vars.array(), vars.n(), accuracy);
	}
	
	// traverse vector dd and call setElement method of VectorConsumer for each non zero element
	// [ REFS: <none>, DEREFS: <none> ]

	public static void TraverseVector(JDDNode dd, JDDVars vars, JDDVectorConsumer vc, int code)
	{
		TraverseVectorRec(dd, vars, 0, 0, vc, code);
	}
	
	// recursive part of TraverseVector
	
	private static void TraverseVectorRec(JDDNode dd, JDDVars vars, int varStart, long count, JDDVectorConsumer vc, int code)
	{
		JDDNode n, s;
		
		if (dd.equals(JDD.ZERO)) {
			return;
		}
				
		if (varStart == vars.getNumVars()) {
			vc.setElement(count, dd.getValue(), code);
		}
		else {
			// split into 2 cases
			JDD.Ref(dd);
			JDD.Ref(vars.getVar(varStart));
			n = JDD.Restrict(dd, JDD.Not(vars.getVar(varStart)));
			JDD.Ref(dd);
			JDD.Ref(vars.getVar(varStart));
			s = JDD.Restrict(dd, vars.getVar(varStart));
			
			TraverseVectorRec(n, vars, varStart+1, count, vc, code);
			TraverseVectorRec(s, vars, varStart+1, count+(1l << (vars.getNumVars()-varStart-1)), vc, code);
			
			JDD.Deref(n);
			JDD.Deref(s);
		}
	}

	// wrapper methods for dd_export

	// export dd to a dot file
	// [ REFS: <none>, DEREFS: <none> ]

	public static void ExportDDToDotFile(JDDNode dd, String filename)
	{
		DD_ExportDDToDotFile(dd.ptr(), filename);
	}
	
	// export dd to a dot file
	// [ REFS: <none>, DEREFS: <none> ]

	public static void ExportDDToDotFileLabelled(JDDNode dd, String filename, Vector varNames)
	{
		DD_ExportDDToDotFileLabelled(dd.ptr(), filename, varNames);
	}
	
	// export matrix dd to a pp file
	// [ REFS: <none>, DEREFS: <none> ]

	public static void ExportMatrixToPPFile(JDDNode dd, JDDVars rvars, JDDVars cvars, String filename)
	{
		DD_ExportMatrixToPPFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), filename);
	}

	// export matrix dd to a matlab file
	// [ REFS: <none>, DEREFS: <none> ]

	public static void ExportMatrixToMatlabFile(JDDNode dd, JDDVars rvars, JDDVars cvars, String name, String filename)
	{
		DD_ExportMatrixToMatlabFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), name, filename);
	}

	// export matrix dd to a spy file
	// [ REFS: <none>, DEREFS: <none> ]

	public static void ExportMatrixToSpyFile(JDDNode dd, JDDVars rvars, JDDVars cvars, int depth, String filename)
	{
		DD_ExportMatrixToSpyFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), depth, filename);
	}
}

//------------------------------------------------------------------------------
