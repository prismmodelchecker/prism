//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	* Christian von Essen <christian.vonessen@imag.fr> (VERIMAG)
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

package jdd;

import java.util.*;

public class JDD
{
	// dd library functions
	public static native long GetCUDDManager();
	// dd
	private static native void DD_SetOutputStream(long fp);
	private static native long DD_GetOutputStream();
	// dd_cudd
	private static native void DD_InitialiseCUDD();
	private static native void DD_InitialiseCUDD(long max_mem, double epsilon);
	private static native void DD_SetCUDDMaxMem(long max_mem);
	private static native void DD_SetCUDDEpsilon(double epsilon);
	private static native void DD_CloseDownCUDD(boolean check);
	private static native void DD_Ref(long dd);
	private static native void DD_Deref(long dd);
	private static native void DD_PrintCacheInfo();
	// dd_basics
	private static native long DD_Create();
	private static native long DD_Constant(double value);
	private static native long DD_PlusInfinity();
	private static native long DD_MinusInfinity();
	private static native long DD_Var(int i);
	private static native long DD_Not(long dd);
	private static native long DD_Or(long dd1, long dd2);
	private static native long DD_And(long dd1, long dd2);
	private static native long DD_Xor(long dd1, long dd2);
	private static native long DD_Implies(long dd1, long dd2);
	private static native long DD_Apply(int op, long dd1, long dd2);
	private static native long DD_MonadicApply(int op, long dd);
	private static native long DD_Restrict(long dd, long cube);
	private static native long DD_ITE(long dd1, long dd2, long dd3);
	// dd_vars
	private static native long DD_PermuteVariables(long dd, long old_vars, long new_vars, int num_vars);
	private static native long DD_SwapVariables(long dd, long old_vars, long new_vars, int num_vars);
	private static native long DD_VariablesGreaterThan(long x_vars, long y_vars, int num_vars);
	private static native long DD_VariablesGreaterThanEquals(long x_vars, long y_vars, int num_vars);
	private static native long DD_VariablesLessThan(long x_vars, long y_vars, int num_vars);
	private static native long DD_VariablesLessThanEquals(long x_vars, long y_vars, int num_vars);
	private static native long DD_VariablesEquals(long x_vars, long y_vars, int num_vars);
	// dd_abstr
	private static native long DD_ThereExists(long dd, long vars, int num_vars);
	private static native long DD_ForAll(long dd, long vars, int num_vars);
	private static native long DD_SumAbstract(long dd, long vars, int num_vars);
	private static native long DD_ProductAbstract(long dd, long vars, int num_vars);
	private static native long DD_MinAbstract(long dd, long vars, int num_vars);
	private static native long DD_MaxAbstract(long dd, long vars, int num_vars);
	// dd_term
	private static native long DD_GreaterThan(long dd, double threshold);
	private static native long DD_GreaterThanEquals(long dd, double threshold);
	private static native long DD_LessThan(long dd, double threshold);
	private static native long DD_LessThanEquals(long dd, double threshold);
	private static native long DD_Equals(long dd, double value);
	private static native long DD_Interval(long dd, double lower, double upper);
	private static native long DD_RoundOff(long dd, int places);
	private static native boolean DD_EqualSupNorm(long dd1, long dd2, double epsilon);
	private static native double DD_FindMin(long dd);
	private static native double DD_FindMax(long dd);
	private static native long DD_RestrictToFirst(long dd, long vars, int num_vars);
	// dd_info
	private static native int DD_GetNumNodes(long dd);
	private static native int DD_GetNumTerminals(long dd);
	private static native double DD_GetNumMinterms(long dd, int num_vars);
	private static native double DD_GetNumPaths(long dd);
	private static native void DD_PrintInfo(long dd, int num_vars);
	private static native void DD_PrintInfoBrief(long dd, int num_vars);
	private static native void DD_PrintSupport(long dd);
	private static native void DD_PrintSupportNames(long dd, List<String> var_names);
	private static native long DD_GetSupport(long dd);
	private static native void DD_PrintTerminals(long dd);
	private static native void DD_PrintTerminalsAndNumbers(long dd, int num_vars);
	// dd_matrix
	private static native long DD_SetVectorElement(long dd, long vars, int num_vars, long index, double value);
	private static native long DD_SetMatrixElement(long dd, long rvars, int num_rvars, long cvars, int num_cvars, long rindex, long cindex, double value);
	private static native long DD_Set3DMatrixElement(long dd, long rvars, int num_rvars, long cvars, int num_cvars, long lvars, int num_lvars, long rindex, long cindex, long lindex, double value);
	private static native double DD_GetVectorElement(long dd, long vars, int num_vars, long index);
	private static native long DD_Identity(long rvars, long cvars, int num_vars);
	private static native long DD_Transpose(long dd, long rvars, long cvars, int num_vars);
	private static native long DD_MatrixMultiply(long dd1, long dd2, long vars, int num_vars, int method);
	private static native void DD_PrintVector(long dd, long vars, int num_vars, int accuracy);
	private static native void DD_PrintMatrix(long dd, long rvars, int num_rvars, long cvars, int num_cvars, int accuracy);
	private static native void DD_PrintVectorFiltered(long dd, long filter, long vars, int num_vars, int accuracy);
	// dd_export
	private static native void DD_ExportDDToDotFile(long dd, String filename);
	private static native void DD_ExportDDToDotFileLabelled(long dd, String filename, List<String> var_names);
	private static native void DD_ExportMatrixToPPFile(long dd, long rvars, int num_rvars, long cvars, int num_cvars, String filename);
	private static native void DD_Export3dMatrixToPPFile(long dd, long rvars, int num_rvars, long cvars, int num_cvars, long nvars, int num_nvars, String filename);
	private static native void DD_ExportMatrixToMatlabFile(long dd, long rvars, int num_rvars, long cvars, int num_cvars, String name, String filename);
	private static native void DD_ExportMatrixToSpyFile(long dd, long rvars, int num_rvars, long cvars, int num_cvars, int depth, String filename);

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
	public static final int LOGXY = 17;

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
	public static JDDNode PLUS_INFINITY;
	public static JDDNode MINUS_INFINITY;
		
	// wrapper methods for dd
	
	public static void SetOutputStream(long fp)
	{
		DD_SetOutputStream(fp);
	}
	
	public static long GetOutputStream()
	{
		return DD_GetOutputStream();
	}
	
	// wrapper methods for dd_cudd

	/**
	 * initialise cudd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void InitialiseCUDD()
	{
		DD_InitialiseCUDD();
		ZERO = Constant(0);
		ONE = Constant(1);
		PLUS_INFINITY = JDD.PlusInfinity();
		MINUS_INFINITY = JDD.MinusInfinity();
	}
		
	/**
	 * initialise cudd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void InitialiseCUDD(long max_mem, double epsilon)
	{
		DD_InitialiseCUDD(max_mem, epsilon);
		ZERO = Constant(0);
		ONE = Constant(1);
		PLUS_INFINITY = JDD.PlusInfinity();
		MINUS_INFINITY = JDD.MinusInfinity();
	}
		
	/**
	 * set cudd max memory
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void SetCUDDMaxMem(long max_mem)
	{
		DD_SetCUDDMaxMem(max_mem);
	}
		
	/**
	 * set cudd epsilon
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void SetCUDDEpsilon(double epsilon)
	{
		DD_SetCUDDEpsilon(epsilon);
	}
		
	/**
	 * close down cudd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void CloseDownCUDD() { CloseDownCUDD(true); }
	public static void CloseDownCUDD(boolean check)
	{
		Deref(ZERO);
		Deref(ONE);
		Deref(PLUS_INFINITY);
		Deref(MINUS_INFINITY);
		if (jdd.DebugJDD.debugEnabled)
			DebugJDD.endLifeCycle();
		DD_CloseDownCUDD(check);
	}
	
	/**
	 * reference dd
	 * <br>[ REFS: dd, DEREFS: <i>none</i> ]
	 */
	public static void Ref(JDDNode dd)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.increment(dd);
		DD_Ref(dd.ptr());
	}
	
	/**
	 * dereference dd
	 * <br>[ REFS: <i>none</i>, DEREFS: dd ]
	 */
	public static void Deref(JDDNode dd)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		DD_Deref(dd.ptr());
	}

	/**
	 * print cudd cache info
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintCacheInfo()
	{
		DD_PrintCacheInfo();
	}
	
	// wrapper methods for dd_basics

	/**
	 * create new (zero) dd
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode Create()
	{
		return new JDDNode(DD_Create());
	}
	
	/**
	 * create new constant dd
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode Constant(double value)
	{
		if (Double.isInfinite(value))
			return value > 0 ? JDD.PlusInfinity() : JDD.MinusInfinity();
		else
			return new JDDNode(DD_Constant(value));
	}
		
	/**
	 * create new constant (plus infinity)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode PlusInfinity()
	{
		return new JDDNode(DD_PlusInfinity());
	}
	
	/**
	 * create new constant (minus infinity)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode MinusInfinity()
	{
		return new JDDNode(DD_MinusInfinity());
	}
	
	/**
	 * create new variable dd (1 if var i is true, 0 if not)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode Var(int i)
	{
		return new JDDNode(DD_Var(i));
	}
	
	/**
	 * not of dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Not(JDDNode dd)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_Not(dd.ptr()));
	}
	
	/**
	 * or of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Or(JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
		}
		return new JDDNode(DD_Or(dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * and of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode And(JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
		}
			
		return new JDDNode(DD_And(dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * xor of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Xor(JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
		}
		return new JDDNode(DD_Xor(dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * implies of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Implies(JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
		}
		return new JDDNode(DD_Implies(dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * generic apply operation
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Apply(int op, JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
		}
		return new JDDNode(DD_Apply(op, dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * generic monadic apply operation
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode MonadicApply(int op, JDDNode dd)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_MonadicApply(op, dd.ptr()));
	}
	
	/**
	 * restrict dd based on cube
	 * <br>[ REFS: <i>result</i>, DEREFS: dd, cube ]
	 */
	public static JDDNode Restrict(JDDNode dd, JDDNode cube)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd);
			DebugJDD.decrement(cube);
		}
		return new JDDNode(DD_Restrict(dd.ptr(), cube.ptr()));
	}
	
	/**
	 * ITE (if-then-else) operation
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2, dd3 ]
	 */
	public static JDDNode ITE(JDDNode dd1, JDDNode dd2, JDDNode dd3)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
			DebugJDD.decrement(dd3);
		}
		return new JDDNode(DD_ITE(dd1.ptr(), dd2.ptr(), dd3.ptr()));
	}
		
	/**
	 * Returns true if the two BDDs intersect (i.e. conjunction is non-empty).
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static boolean AreInterecting(JDDNode dd1, JDDNode dd2)
	{
		JDDNode tmp;
		boolean res;
		JDD.Ref(dd1);
		JDD.Ref(dd2);
		tmp = JDD.And(dd1, dd2);
		res = !tmp.equals(JDD.ZERO);
		JDD.Deref(tmp);
		return res;
	}
	
	/**
	 * Returns true if {@code dd1} is contained in {@code dd2}.
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static boolean IsContainedIn(JDDNode dd1, JDDNode dd2)
	{
		JDDNode tmp;
		boolean res;
		JDD.Ref(dd1);
		JDD.Ref(dd2);
		/*tmp = JDD.Implies(dd1, dd2);
		res = tmp.equals(JDD.ONE);*/
		tmp = JDD.And(dd1, dd2);
		res = tmp.equals(dd1);
		JDD.Deref(tmp);
		return res;
	}
	
	// wrapper methods for dd_vars
	
	/**
	 * permute (->) variables in dd (cf. swap)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode PermuteVariables(JDDNode dd, JDDVars old_vars, JDDVars new_vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_PermuteVariables(dd.ptr(), old_vars.array(), new_vars.array(), old_vars.n()));
	}

	/**
	 * swap (<->) variables in dd (cf. permute)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode SwapVariables(JDDNode dd, JDDVars old_vars, JDDVars new_vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_SwapVariables(dd.ptr(), old_vars.array(), new_vars.array(), old_vars.n()));
	}

	/**
	 * build x > y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesGreaterThan(JDDVars x_vars, JDDVars y_vars)
	{
		return new JDDNode(DD_VariablesGreaterThan(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x >= y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesGreaterThanEquals(JDDVars x_vars, JDDVars y_vars)
	{
		return new JDDNode(DD_VariablesGreaterThanEquals(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x < y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesLessThan(JDDVars x_vars, JDDVars y_vars)
	{
		return new JDDNode(DD_VariablesLessThan(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x <= y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesLessThanEquals(JDDVars x_vars, JDDVars y_vars)
	{
		return new JDDNode(DD_VariablesLessThanEquals(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x == y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesEquals(JDDVars x_vars, JDDVars y_vars)
	{
		return new JDDNode(DD_VariablesEquals(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	// wrapper methods for dd_abstr

	/**
	 * existential abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode ThereExists(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_ThereExists(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * universal abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode ForAll(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_ForAll(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * sum abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode SumAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_SumAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * product abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode ProductAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_ProductAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * min abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode MinAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_MinAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * max abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode MaxAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_MaxAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	// wrapper methods for dd_term

	/**
	 * converts dd to a 0-1 dd, based on the interval (threshold, +inf)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode GreaterThan(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_GreaterThan(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval [threshold, +inf)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode GreaterThanEquals(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_GreaterThanEquals(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval (-inf, threshold)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode LessThan(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_LessThan(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval (-inf, threshold]
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode LessThanEquals(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_LessThanEquals(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval [threshold, threshold]
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Equals(JDDNode dd, double value)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_Equals(dd.ptr(), value));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval [lower, upper]
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Interval(JDDNode dd, double lower, double upper)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_Interval(dd.ptr(), lower, upper));
	}
	
	/**
	 * rounds terminals in dd to a certain number of decimal places
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode RoundOff(JDDNode dd, int places)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_RoundOff(dd.ptr(), places));
	}
	
	/**
	 * returns true if sup norm of dd1 and dd2 is less than epsilon, returns false otherwise
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static boolean EqualSupNorm(JDDNode dd1, JDDNode dd2, double epsilon)
	{
		return DD_EqualSupNorm(dd1.ptr(), dd2.ptr(), epsilon);
	}
	
	/**
	 * returns minimum terminal in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double FindMin(JDDNode dd)
	{
		return DD_FindMin(dd.ptr());
	}
	
	/**
	 * returns maximum terminal in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double FindMax(JDDNode dd)
	{
		return DD_FindMax(dd.ptr());
	}
	
	/**
	 * returns dd restricted to first non-zero path (cube)
	 * <br>[ REFS: <i>result</i>, DEREFS: <dd> ]
	 */
	public static JDDNode RestrictToFirst(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_RestrictToFirst(dd.ptr(), vars.array(), vars.n()));
	}

	// wrapper methods for dd_info

	/**
	 * returns number of nodes in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static int GetNumNodes(JDDNode dd)
	{
		return DD_GetNumNodes(dd.ptr());
	}
	
	/**
	 * returns number of terminals in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static int GetNumTerminals(JDDNode dd)
	{
		return DD_GetNumTerminals(dd.ptr());
	}
	
	/**
	 * returns number of minterms in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double GetNumMinterms(JDDNode dd, int num_vars)
	{
		return DD_GetNumMinterms(dd.ptr(), num_vars);
	}
	
	/**
	 * get number of minterms as a string (have to store as a double
	 * because can be very big but want to print out as a decimal)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
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

	/**
	 * returns number of paths in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double GetNumPaths(JDDNode dd)
	{
		return DD_GetNumPaths(dd.ptr());
	}
	
	/**
	 * get number of paths as a string (have to store as a double
	 * because can be very big but want to print out as a decimal)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
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
	
	/**
	 * prints out info for dd (nodes, terminals, minterms)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintInfo(JDDNode dd, int num_vars)
	{		
		DD_PrintInfo(dd.ptr(), num_vars);
	}
	
	/**
	 * prints out compact info for dd (nodes, terminals, minterms)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintInfoBrief(JDDNode dd, int num_vars)
	{
		DD_PrintInfoBrief(dd.ptr(), num_vars);
	}
	
	/**
	 * gets info for dd as string (nodes, terminals, minterms)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static String GetInfoString(JDDNode dd, int num_vars)
	{
		return GetNumNodes(dd)+" nodes ("+GetNumTerminals(dd)+" terminal), "+GetNumMintermsString(dd, num_vars)+" minterms";
	}
	
	/**
	 * gets compact info for dd as string (nodes, terminals, minterms)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static String GetInfoBriefString(JDDNode dd, int num_vars)
	{
		return "["+GetNumNodes(dd)+","+GetNumTerminals(dd)+","+GetNumMintermsString(dd, num_vars)+"]";
	}
	
	/**
	 * Prints out the support of a DD (i.e. all DD variables that are actually present).
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintSupport(JDDNode dd)
	{
		DD_PrintSupport(dd.ptr());
	}
	
	/**
	 * Prints out the support of a DD (i.e. all DD variables that are actually present),
	 * using the passed in list of DD variable names.
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintSupportNames(JDDNode dd, List<String> varNames)
	{
		DD_PrintSupportNames(dd.ptr(), varNames);
	}
	
	/**
	 * returns support for dd (all dd variables present) as a cube of the dd vars
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode GetSupport(JDDNode dd)
	{
		return new JDDNode(DD_GetSupport(dd.ptr()));
	}
	
	/**
	 * print out all values of all terminals in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintTerminals(JDDNode dd)
	{
		DD_PrintTerminals(dd.ptr());
	}
	
	/**
	 * get list of values of all terminals in dd as string (native method sends to stdout)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static String GetTerminalsString(JDDNode dd)
	{
		return GetTerminalsString(dd, 0, false);
	}
	
	/**
	 * print out all values of all terminals (and number of each) in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintTerminalsAndNumbers(JDDNode dd, int num_vars)
	{
		DD_PrintTerminalsAndNumbers(dd.ptr(), num_vars);
	}
	
	/**
	 * get list of values of all terminals (and number of each) in dd (native method sends to stdout)
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static String GetTerminalsAndNumbersString(JDDNode dd, int num_vars)
	{
		return GetTerminalsString(dd, num_vars, true);
	}
	
	// Generic code for two GetTerminals...String methods above
	
	public static String GetTerminalsString(JDDNode dd, int num_vars, boolean and_numbers)
	{
		JDDNode tmp, tmp2;
		double min, max, num, count = 0.0;
		String s = "";

		// Take a copy of dd	
		JDD.Ref(dd);
		tmp = dd;
		// Check the min (will use at end)
		min = JDD.FindMin(tmp);
		// Loop through terminals in descending order
		while (!tmp.equals(JDD.MINUS_INFINITY)) {
			// Find next (max) terminal and display
			max = JDD.FindMax(tmp);
			s += max + " ";
			// Remove the terminals, counting/displaying number if required
			JDD.Ref(tmp);
			tmp2 = JDD.Equals(tmp, max);
			if (and_numbers) {
				num = JDD.GetNumMinterms(tmp2, num_vars);
				count += num;
				s += "(" + (long)num + ") ";
			}
			tmp = JDD.ITE(tmp2, JDD.MinusInfinity(), tmp);
		}
		JDD.Deref(tmp);
		// Finally, print if there are (and possibly how many) minus infinities
		if (and_numbers) {
			if (count < (1<<num_vars)) s += "-inf (" + ((1<<num_vars)-count) + ")";
		} else {
			if (min == -Double.POSITIVE_INFINITY) s += "-inf";
		}
		
		return s;
	}

	// wrapper methods for dd_matrix

	/**
	 * sets element in vector dd
	 * <br>[ REFS: <i>result</i>, DEREFS: <dd> ]
	 */
	public static JDDNode SetVectorElement(JDDNode dd, JDDVars vars, long index, double value)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_SetVectorElement(dd.ptr(), vars.array(), vars.n(), index, value));
	}
	
	/**
	 * sets element in matrix dd
	 * <br>[ REFS: <i>result</i>, DEREFS: <dd> ]
	 */
	public static JDDNode SetMatrixElement(JDDNode dd, JDDVars rvars, JDDVars cvars, long rindex, long cindex, double value)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_SetMatrixElement(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), rindex, cindex, value));
	}
	
	/**
	 * sets element in 3d matrix dd
	 * <br>[ REFS: <i>result</i>, DEREFS: <dd> ]
	 */
	public static JDDNode Set3DMatrixElement(JDDNode dd, JDDVars rvars, JDDVars cvars, JDDVars lvars, long rindex, long cindex, long lindex, double value)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_Set3DMatrixElement(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), lvars.array(), lvars.n(), rindex, cindex, lindex, value));
	}
	
	/**
	 * get element in vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double GetVectorElement(JDDNode dd, JDDVars vars, long index)
	{
		return DD_GetVectorElement(dd.ptr(), vars.array(), vars.n(), index);
	}
	
	/**
	 * creates dd for identity matrix
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode Identity(JDDVars rvars, JDDVars cvars)
	{
		return new JDDNode(DD_Identity(rvars.array(), cvars.array(), rvars.n()));
	}
	
	/**
	 * returns transpose of matrix dd
	 * <br>[ REFS: <i>result</i>, DEREFS: <dd> ]
	 */
	public static JDDNode Transpose(JDDNode dd, JDDVars rvars, JDDVars cvars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.decrement(dd);
		return new JDDNode(DD_Transpose(dd.ptr(), rvars.array(), cvars.array(), rvars.n()));
	}
	
	/**
	 * returns matrix multiplication of matrices dd1 and dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: <dd1, dd2> ]
	 */
	public static JDDNode MatrixMultiply(JDDNode dd1, JDDNode dd2, JDDVars vars, int method)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.decrement(dd1);
			DebugJDD.decrement(dd2);
		}
		return new JDDNode(DD_MatrixMultiply(dd1.ptr(), dd2.ptr(), vars.array(), vars.n(), method));
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVector(JDDNode dd, JDDVars vars)
	{
		DD_PrintVector(dd.ptr(), vars.array(), vars.n(), NORMAL);
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVector(JDDNode dd, JDDVars vars, int accuracy)
	{
		DD_PrintVector(dd.ptr(), vars.array(), vars.n(), accuracy);
	}
	
	/**
	 * prints out matrix dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintMatrix(JDDNode dd, JDDVars rvars, JDDVars cvars)
	{
		DD_PrintMatrix(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), NORMAL);
	}
	
	/**
	 * prints out matrix dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintMatrix(JDDNode dd, JDDVars rvars, JDDVars cvars, int accuracy)
	{
		DD_PrintMatrix(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), accuracy);
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVectorFiltered(JDDNode dd, JDDNode filter, JDDVars vars)
	{
		DD_PrintVectorFiltered(dd.ptr(), filter.ptr(), vars.array(), vars.n(), NORMAL);
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVectorFiltered(JDDNode dd, JDDNode filter, JDDVars vars, int accuracy)
	{
		DD_PrintVectorFiltered(dd.ptr(), filter.ptr(), vars.array(), vars.n(), accuracy);
	}
	
	/**
	 * traverse vector dd and call setElement method of VectorConsumer for each non zero element
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
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

	/**
	 * export dd to a dot file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportDDToDotFile(JDDNode dd, String filename)
	{
		DD_ExportDDToDotFile(dd.ptr(), filename);
	}
	
	/**
	 * export dd to a dot file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportDDToDotFileLabelled(JDDNode dd, String filename, List<String> varNames)
	{
		DD_ExportDDToDotFileLabelled(dd.ptr(), filename, varNames);
	}
	
	/**
	 * export matrix dd to a pp file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportMatrixToPPFile(JDDNode dd, JDDVars rvars, JDDVars cvars, String filename)
	{
		DD_ExportMatrixToPPFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), filename);
	}
	
	/**
	 * Given a BDD that represents transition matrices of an MDP, this method
     * outputs one matrix for every action. Note that the output is in fact
     * not a PP file, but several PP files concatenated into one file.
	 *
	 * For example, for a model with the variable
	 * 	x : [0..2];
	 * and transitions
	 *  [a] (x=0) -> 0.3:(x'=1) + 0.7:(x'=2);
	 *  [b] (x=0) -> 1:(x'=2);
	 *  [a] (x=2) -> (x'=1);
	 *  [a] (x=1) -> (x'=0);
	 * the output would be (e.g.)
 	 *  4
	 *	4
	 *	0 2 1.000000
	 *	4
	 *	0 1 0.300000
	 *	1 0 1.000000
	 *	0 2 0.700000
	 *	2 1 1.000000
	 *  4
	 *
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void Export3dMatrixToPPFile(JDDNode dd, JDDVars rvars, JDDVars cvars, JDDVars nvars, String filename)
	{
		DD_Export3dMatrixToPPFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), nvars.array(), nvars.n(), filename);
	}

	/**
	 * export matrix dd to a matlab file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportMatrixToMatlabFile(JDDNode dd, JDDVars rvars, JDDVars cvars, String name, String filename)
	{
		DD_ExportMatrixToMatlabFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), name, filename);
	}

	/**
	 * export matrix dd to a spy file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportMatrixToSpyFile(JDDNode dd, JDDVars rvars, JDDVars cvars, int depth, String filename)
	{
		DD_ExportMatrixToSpyFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), depth, filename);
	}
}

//------------------------------------------------------------------------------
