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

import prism.PrismLog;

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
	static native void DD_Ref(long dd);
	static native void DD_Deref(long dd);
	private static native void DD_PrintCacheInfo();
	private static native boolean DD_GetErrorFlag();
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
	private static native double DD_FindMinPositive(long dd);
	private static native double DD_FindMax(long dd);
	private static native double DD_FindMaxFinite(long dd);
	private static native long DD_RestrictToFirst(long dd, long vars, int num_vars);
	private static native boolean DD_IsZeroOneMTBDD(long dd);
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

	// helpers for DebugJDD, package visibility
	static native int DebugJDD_GetRefCount(long dd);
	static native long[] DebugJDD_GetExternalRefCounts();

	/**
	 * An exception indicating that CUDD ran out of memory or that another internal error
	 * occurred.
	 * <br>
	 * This exception is thrown by ptrToNode if a NULL pointer is returned by one of the native
	 * DD methods. It is generally not safe to use the CUDD library after this error occurred,
	 * so the program should quit as soon as feasible.
	 */
	public static class CuddOutOfMemoryException extends RuntimeException {
		private static final long serialVersionUID = -3094099053041270477L;

		/** Constructor */
		CuddOutOfMemoryException() {
			super("Out of memory (or other internal error) in the CUDD library");
		}
	}

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
		long ptr = dd.ptr();
		// For robustness, catch NULL pointer
		// In general, this should not happen,
		// as constructing a JDDNode with NULL
		// pointer should not happen...
		if (ptr == 0) {
			throw new CuddOutOfMemoryException();
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.Ref(dd);
		} else {
			DD_Ref(ptr);
		}
	}
	
	/**
	 * dereference dd
	 * <br>[ REFS: <i>none</i>, DEREFS: dd ]
	 */
	public static void Deref(JDDNode dd)
	{
		long ptr = dd.ptr();
		// For robustness, catch NULL pointer
		// In general, this should not happen,
		// as constructing a JDDNode with NULL
		// pointer should not happen...
		if (ptr == 0) {
			throw new CuddOutOfMemoryException();
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.Deref(dd);
		} else {
			DD_Deref(ptr);
		}
	}

	/**
	 * Dereference dds, multi-argument variant.
	 * The dds have to be non-{@code null}.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>all argument dds</i> ]
	 */
	public static void Deref(JDDNode... dds)
	{
		for (JDDNode d : dds) {
			JDD.Deref(d);
		}
	}

	/**
	 * Dereference array of JDDNodes, by dereferencing all (non-null) elements.
	 * <br>[ REFS: <i>none</i>, DEREFS: all elements of dds ]
	 * @param dds the array of JDDNodes
	 * @param n the expected length of the array (for detecting problems with refactoring)
	 */
	public static void DerefArray(JDDNode[] dds, int n)
	{
		if (n != dds.length) {
			throw new RuntimeException("Mismatch in length of dd array and expected length!");
		}
		for (JDDNode dd : dds) {
			if (dd != null)
				JDD.Deref(dd);
		}
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
		return ptrToNode(DD_Create());
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
			return ptrToNode(DD_Constant(value));
	}
		
	/**
	 * create new constant (plus infinity)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode PlusInfinity()
	{
		return ptrToNode(DD_PlusInfinity());
	}
	
	/**
	 * create new constant (minus infinity)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode MinusInfinity()
	{
		return ptrToNode(DD_MinusInfinity());
	}
	
	/**
	 * create new variable dd (1 if var i is true, 0 if not)
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode Var(int i)
	{
		return ptrToNode(DD_Var(i));
	}
	
	/**
	 * not of dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Not(JDDNode dd)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd);
		}
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_Not(dd.ptr()));
	}
	
	/**
	 * or of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Or(JDDNode dd1, JDDNode dd2)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd1);
			SanityJDD.checkIsZeroOneMTBDD(dd2);
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
		}
		return ptrToNode(DD_Or(dd1.ptr(), dd2.ptr()));
	}

	/**
	 * Multi-operand Or (0/1-MTBDD disjunction) operation.
	 * Operands are processed from left-to-right.
	 * <br>
	 * Returns JDD.Constant(0) for empty argument list
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>all arguments</i> ]
	 */
	public static JDDNode Or(JDDNode... nodes)
	{
		if (nodes.length == 0)
			return JDD.Constant(0);

		JDDNode result = nodes[0];
		for (int i = 1; i < nodes.length; i++) {
			// note: Java overloading rules ensure that fixed arity
			// methods take precedence. So, for two-operand Or,
			// the Or(JDDNode,JDDNode) method above is called and we don't
			// run into an infinite recursion
			result = Or(result, nodes[i]);
		}

		return result;
	}
	
	/**
	 * and of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode And(JDDNode dd1, JDDNode dd2)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd1);
			SanityJDD.checkIsZeroOneMTBDD(dd2);
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
		}
			
		return ptrToNode(DD_And(dd1.ptr(), dd2.ptr()));
	}

	/**
	 * Multi-operand And (0/1-MTBDD conjunction) operation.
	 * Operands are processed from left-to-right.
	 * <br>
	 * Returns JDD.Constant(1) for empty argument list
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>all arguments</i> ]
	 */
	public static JDDNode And(JDDNode... nodes)
	{
		if (nodes.length == 0)
			return JDD.Constant(1);

		JDDNode result = nodes[0];
		for (int i = 1; i < nodes.length; i++) {
			// note: Java overloading rules ensure that fixed arity
			// methods take precedence. So, for two-operand And,
			// the And(JDDNode,JDDNode) method above is called and we don't
			// run into an infinite recursion
			result = And(result, nodes[i]);
		}

		return result;
	}

	/**
	 * xor of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Xor(JDDNode dd1, JDDNode dd2)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd1);
			SanityJDD.checkIsZeroOneMTBDD(dd2);
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
		}
		return ptrToNode(DD_Xor(dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * implies of dd1, dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Implies(JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
		}
		return ptrToNode(DD_Implies(dd1.ptr(), dd2.ptr()));
	}
	
	/**
	 * equivalence of dd1, dd2 (have to be 0/1-MTBDDs)
	 * [ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Equiv(JDDNode dd1, JDDNode dd2)
	{
		return Not(Xor(dd1, dd2));
	}
	
	/**
	 * generic apply operation
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode Apply(int op, JDDNode dd1, JDDNode dd2)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
		}
		return ptrToNode(DD_Apply(op, dd1.ptr(), dd2.ptr()));
	}

	/**
	 * Multi-operand Apply(JDD.TIMES) (multiplication) operation.
	 * Operands are processed from left-to-right.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>all arguments</i> ]
	 */
	public static JDDNode Times(JDDNode node, JDDNode... nodes) {
		JDDNode result = node;
		for (JDDNode n : nodes) {
			result = Apply(JDD.TIMES, result, n);
		}

		return result;
	}

	/**
	 * Multi-operand Apply(JDD.PLUS) (addition) operation.
	 * Operands are processed from left-to-right.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>all arguments</i> ]
	 */
	public static JDDNode Plus(JDDNode node, JDDNode... nodes) {
		JDDNode result = node;
		for (JDDNode n : nodes) {
			result = Apply(JDD.PLUS, result, n);
		}

		return result;
	}

	/**
	 * Multi-operand Apply(JDD.MAX) (maximum) operation.
	 * Operands are processed from left-to-right.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>all arguments</i> ]
	 */
	public static JDDNode Max(JDDNode node, JDDNode... nodes) {
		JDDNode result = node;
		for (JDDNode n : nodes) {
			result = Apply(JDD.MAX, result, n);
		}

		return result;
	}

	/**
	 * Multi-operand Apply(JDD.MIN) (minimum) operation.
	 * Operands are processed from left-to-right.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>all arguments</i> ]
	 */
	public static JDDNode Min(JDDNode node, JDDNode... nodes) {
		JDDNode result = node;
		for (JDDNode n : nodes) {
			result = Apply(JDD.MIN, result, n);
		}

		return result;
	}

	/**
	 * generic monadic apply operation
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode MonadicApply(int op, JDDNode dd)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_MonadicApply(op, dd.ptr()));
	}
	
	/**
	 * restrict dd based on cube
	 * <br>[ REFS: <i>result</i>, DEREFS: dd, cube ]
	 */
	public static JDDNode Restrict(JDDNode dd, JDDNode cube)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd);
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd);
			DebugJDD.DD_Method_Argument(cube);
		}
		return ptrToNode(DD_Restrict(dd.ptr(), cube.ptr()));
	}
	
	/**
	 * ITE (if-then-else) operation
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2, dd3 ]
	 */
	public static JDDNode ITE(JDDNode dd1, JDDNode dd2, JDDNode dd3)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd1);
		}
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
			DebugJDD.DD_Method_Argument(dd3);
		}
		return ptrToNode(DD_ITE(dd1.ptr(), dd2.ptr(), dd3.ptr()));
	}
		
	/**
	 * Returns true if the two BDDs intersect (i.e. conjunction is non-empty).
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static boolean AreIntersecting(JDDNode dd1, JDDNode dd2)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd1);
			SanityJDD.checkIsZeroOneMTBDD(dd2);
		}
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
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd1);
			SanityJDD.checkIsZeroOneMTBDD(dd2);
		}
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
	 * permute (-&gt;) variables in dd (cf. swap)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode PermuteVariables(JDDNode dd, JDDVars old_vars, JDDVars new_vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_PermuteVariables(dd.ptr(), old_vars.array(), new_vars.array(), old_vars.n()));
	}

	/**
	 * swap (&lt;-&gt;) variables in dd (cf. permute)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode SwapVariables(JDDNode dd, JDDVars old_vars, JDDVars new_vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_SwapVariables(dd.ptr(), old_vars.array(), new_vars.array(), old_vars.n()));
	}

	/**
	 * build x &gt; y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesGreaterThan(JDDVars x_vars, JDDVars y_vars)
	{
		return ptrToNode(DD_VariablesGreaterThan(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x &gt;= y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesGreaterThanEquals(JDDVars x_vars, JDDVars y_vars)
	{
		return ptrToNode(DD_VariablesGreaterThanEquals(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x &lt; y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesLessThan(JDDVars x_vars, JDDVars y_vars)
	{
		return ptrToNode(DD_VariablesLessThan(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x &lt;= y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesLessThanEquals(JDDVars x_vars, JDDVars y_vars)
	{
		return ptrToNode(DD_VariablesLessThanEquals(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	/**
	 * build x == y for variables x, y
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode VariablesEquals(JDDVars x_vars, JDDVars y_vars)
	{
		return ptrToNode(DD_VariablesEquals(x_vars.array(), y_vars.array(), x_vars.n()));
	}

	// wrapper methods for dd_abstr

	/**
	 * existential abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode ThereExists(JDDNode dd, JDDVars vars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd);
		}
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_ThereExists(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * universal abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode ForAll(JDDNode dd, JDDVars vars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd);
		}
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_ForAll(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * sum abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode SumAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_SumAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * product abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode ProductAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_ProductAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * min abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode MinAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_MinAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	/**
	 * max abstraction of vars from dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode MaxAbstract(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_MaxAbstract(dd.ptr(), vars.array(), vars.n()));
	}
	
	// wrapper methods for dd_term

	/**
	 * converts dd to a 0-1 dd, based on the interval (threshold, +inf)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode GreaterThan(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_GreaterThan(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval [threshold, +inf)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode GreaterThanEquals(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_GreaterThanEquals(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval (-inf, threshold)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode LessThan(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_LessThan(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval (-inf, threshold]
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode LessThanEquals(JDDNode dd, double threshold)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_LessThanEquals(dd.ptr(), threshold));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval [threshold, threshold]
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Equals(JDDNode dd, double value)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_Equals(dd.ptr(), value));
	}
	
	/**
	 * converts dd to a 0-1 dd, based on the interval [lower, upper]
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Interval(JDDNode dd, double lower, double upper)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_Interval(dd.ptr(), lower, upper));
	}
	
	/**
	 * rounds terminals in dd to a certain number of decimal places
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode RoundOff(JDDNode dd, int places)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_RoundOff(dd.ptr(), places));
	}
	
	/**
	 * returns true if sup norm of dd1 and dd2 is less than epsilon, returns false otherwise
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static boolean EqualSupNorm(JDDNode dd1, JDDNode dd2, double epsilon)
	{
		boolean rv = DD_EqualSupNorm(dd1.ptr(), dd2.ptr(), epsilon);
		checkForCuddError();
		return rv;
	}

	/**
	 * returns true if dd is a 0/1-MTBDD, i.e., all terminal nodes are either 0 or 1
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static boolean IsZeroOneMTBDD(JDDNode dd)
	{
		return DD_IsZeroOneMTBDD(dd.ptr());
	}

	/**
	 * returns minimum terminal in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double FindMin(JDDNode dd)
	{
		double rv = DD_FindMin(dd.ptr());
		checkForCuddError();
		return rv;
	}

	/**
	 * Returns minimal positive terminal in dd, i.e.,
	 * the smallest constant greater than zero.
	 * If there is none, returns +infinity.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double FindMinPositive(JDDNode dd)
	{
		double rv = DD_FindMinPositive(dd.ptr());
		checkForCuddError();
		return rv;
	}

	/**
	 * returns maximum terminal in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double FindMax(JDDNode dd)
	{
		double rv = DD_FindMax(dd.ptr());
		checkForCuddError();
		return rv;
	}

	/**
	 * Returns maximal finite positive terminal in dd, i.e.,
	 * If there is none, returns -infinity.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double FindMaxFinite(JDDNode dd)
	{
		double rv = DD_FindMaxFinite(dd.ptr());
		checkForCuddError();
		return rv;
	}

	/**
	 * returns dd restricted to first non-zero path (cube)
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode RestrictToFirst(JDDNode dd, JDDVars vars)
	{
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_RestrictToFirst(dd.ptr(), vars.array(), vars.n()));
	}

	// wrapper methods for dd_info

	/**
	 * returns number of nodes in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static int GetNumNodes(JDDNode dd)
	{
		int rv = DD_GetNumNodes(dd.ptr());
		checkForCuddError();
		return rv;
	}
	
	/**
	 * returns number of terminals in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static int GetNumTerminals(JDDNode dd)
	{
		int rv = DD_GetNumTerminals(dd.ptr());
		checkForCuddError();
		return rv;
	}
	
	/**
	 * returns number of minterms in dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double GetNumMinterms(JDDNode dd, int num_vars)
	{
		double rv = DD_GetNumMinterms(dd.ptr(), num_vars);
		checkForCuddError();
		return rv;
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
		double rv = DD_GetNumPaths(dd.ptr());
		checkForCuddError();
		return rv;
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
	 * Returns {@true} if {@code dd} is a single satisfying
	 * assignment to the variables in {@code vars}.
	 * <br>
	 * This is the case if there is a single path to the ONE constant
	 * and exactly the variables of {@code vars} occur on the path.
	 * <br>
	 * <b>Note:</b> The variables in {@code vars} have to sorted
	 * in increasing index order! Use JDDVars.sortByIndex() to achieve this
	 * if the ordering is not guaranteed by construction.
	 * @param dd the DD
	 * @param vars the variables
	 */
	public static boolean isSingleton(JDDNode dd, JDDVars vars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsZeroOneMTBDD(dd);
			SanityJDD.checkVarsAreSorted(vars);
		}

		for (int i = 0; i < vars.n(); i++) {
			if (dd.isConstant())
				return false;

			if (vars.getVar(i).getIndex() != dd.getIndex())
				return false;

			JDDNode t = dd.getThen();
			JDDNode e = dd.getElse();

			if (t.equals(JDD.ZERO)) {
				dd = e;
			} else if (e.equals(JDD.ZERO)) {
				dd = t;
			} else {
				// then or else have to be ZERO
				return false;
			}
		}

		return dd.equals(JDD.ONE);
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
		return ptrToNode(DD_GetSupport(dd.ptr()));
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
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode SetVectorElement(JDDNode dd, JDDVars vars, long index, double value)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
		}
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_SetVectorElement(dd.ptr(), vars.array(), vars.n(), index, value));
	}
	
	/**
	 * sets element in matrix dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode SetMatrixElement(JDDNode dd, JDDVars rvars, JDDVars cvars, long rindex, long cindex, double value)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
		}
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_SetMatrixElement(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), rindex, cindex, value));
	}
	
	/**
	 * sets element in 3d matrix dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Set3DMatrixElement(JDDNode dd, JDDVars rvars, JDDVars cvars, JDDVars lvars, long rindex, long cindex, long lindex, double value)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars, lvars);
		}
		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_Set3DMatrixElement(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), lvars.array(), lvars.n(), rindex, cindex, lindex, value));
	}
	
	/**
	 * get element in vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static double GetVectorElement(JDDNode dd, JDDVars vars, long index)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
		}
		double rv = DD_GetVectorElement(dd.ptr(), vars.array(), vars.n(), index);
		checkForCuddError();
		return rv;
	}
	
	/**
	 * creates dd for identity matrix
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public static JDDNode Identity(JDDVars rvars, JDDVars cvars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.check(rvars.n() == cvars.n(), "Mismatch of JDDVars sizes");
		}
		return ptrToNode(DD_Identity(rvars.array(), cvars.array(), rvars.n()));
	}
	
	/**
	 * returns transpose of matrix dd
	 * <br>[ REFS: <i>result</i>, DEREFS: dd ]
	 */
	public static JDDNode Transpose(JDDNode dd, JDDVars rvars, JDDVars cvars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
		}

		if (DebugJDD.debugEnabled)
			DebugJDD.DD_Method_Argument(dd);
		return ptrToNode(DD_Transpose(dd.ptr(), rvars.array(), cvars.array(), rvars.n()));
	}
	
	/**
	 * returns matrix multiplication of matrices dd1 and dd2
	 * <br>[ REFS: <i>result</i>, DEREFS: dd1, dd2 ]
	 */
	public static JDDNode MatrixMultiply(JDDNode dd1, JDDNode dd2, JDDVars vars, int method)
	{
		if (DebugJDD.debugEnabled) {
			DebugJDD.DD_Method_Argument(dd1);
			DebugJDD.DD_Method_Argument(dd2);
		}
		return ptrToNode(DD_MatrixMultiply(dd1.ptr(), dd2.ptr(), vars.array(), vars.n(), method));
	}

	/**
	 * Print the minterms for a JDDNode (using the support of dd as variables).
	 * <br>
	 * Positive variables are marked with 1, negatives with 0 and don't cares are marked with -
 	 * <br>[ REFS: <i>none</i>, DEREFS: dd ]
 	 *
	 * @param log the output log
	 * @param dd the MTBDD
	 */
	public static void PrintMinterms(PrismLog log, JDDNode dd)
	{
		PrintMinterms(log, dd, null);
	}

	/**
	 * Print the minterms for a JDDNode (using the support of dd as variables).
	 * <br>
	 * Positive variables are marked with 1, negatives with 0 and don't cares are marked with -
	 * <br>[ REFS: <i>none</i>, DEREFS: dd ]
 	 *
	 * @param log the output log
	 * @param dd the MTBDD
	 * @param name an optional description to be printed ({@code null} for none)
	 */
	public static void PrintMinterms(PrismLog log, JDDNode dd, String description)
	{
		JDDNode csSupport = GetSupport(dd);
		JDDVars vars = JDDVars.fromCubeSet(csSupport);
		PrintMinterms(log, dd, vars, description);
		vars.derefAll();
	}

	/**
	 * Print the minterms for a JDDNode over the variables {@code vars}.
	 * <br>
	 * Positive variables are marked with 1, negatives with 0 and don't cares are marked with -
	 * <br>
	 * {@code vars} has to be ordered with increasing variable indizes and
	 * has to contain all variables in the support of {@code dd}.
 	 * <br>[ REFS: <i>none</i>, DEREFS: dd ]
 	 *
	 * @param log the output log
	 * @param dd the MTBDD
	 * @param vars JDDVars of the relevant variables
	 * @param description an optional description to be printed ({@code null} for none)
	 * @throws IllegalArgumentException if {@code vars} does not fullfil the constraints
	 */
	public static void PrintMinterms(PrismLog log, JDDNode dd, JDDVars vars, String description)
	{
		try {
			if (description != null)
				log.println(description+":");
			log.print(" Variables: (");
			boolean first = true;
			for (JDDNode var : vars) {
				if (!first) log.print(",");
				first = false;
				log.print(var.getIndex());
			}
			log.println(")");
			char[] minterm = new char[vars.n()];
			for (int i = 0; i< minterm.length; i++) {
				minterm[i] = '-';
			}
			PrintMintermsRec(log, dd, vars, 0, minterm);
		} finally {
			JDD.Deref(dd);
		}
	}

	/**
	 * Recursively print the minterms for a JDDNode over the variables {@code vars}.
	 * <br>
	 * Positive variables are marked with 1, negatives with 0 and don't cares are marked with -
	 * <br>
	 * {@code vars} has to be ordered with increasing variable indizes and
	 * has to contain all variables in the support of {@code dd}.
 	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
 	 *
	 * @param log the output log
	 * @param dd the MTBDD
	 * @param vars JDDVars of the relevant variables
	 * @param cur_index the current index into {@code vars}
	 * @param minterm character array of length {@code vars.n()} storing the current (partial) minterm
	 * @throws IllegalArgumentException if {@code vars} does not fullfil the constraints
	 */
	private static void PrintMintermsRec(PrismLog log, JDDNode dd, JDDVars vars, int cur_index, char[] minterm)
	{
		if (dd.isConstant()) {
			// base case: we are at the ZERO sink, don't print
			if (dd.equals(JDD.ZERO))
				return;

			// print the current minterm buffer
			log.print(" |");
			for (char c : minterm) {
				log.print(c);
			}
			// ... and the constant value
			log.println("| = " +dd.getValue());
		} else {
			// Get the current variable index
			int index = dd.getIndex();
			// As long as there are variables left in vars
			while (cur_index < vars.n()) {
				int var_index = vars.getVar(cur_index).getIndex();
				// We are at the level of the next var in vars
				if (var_index == index) {
					// Recurse for else
					minterm[cur_index]='0';
					PrintMintermsRec(log, dd.getElse(), vars, cur_index+1, minterm);
					// Recurse for then
					minterm[cur_index]='1';
					PrintMintermsRec(log, dd.getThen(), vars, cur_index+1, minterm);
					// ... and we are done
					minterm[cur_index]='-';
					return;
				} else if (var_index < index) {
					// The next variable in vars is less then the current dd index
					//  -> don't care
					minterm[cur_index]='-';
					// Go to next variable in vars
					++cur_index;
					// ... and continue
					continue;
				} else {
					// var_index > index
					// Either the vars are not ordered correctly or
					// the dd has relevant variables not included in vars
					// To help with debugging, differentiate between the two cases...
					for (JDDNode var : vars) {
						if (var.getIndex() == index) {
							// There is a var with the current index in vars, but
							// not at the correct position
							throw new IllegalArgumentException("PrintMinterms: vars array does not appear to be sorted correctly (DD index = "+index+", var index = "+var_index+")");
						}
					}
					// otherwise, the dd depends on a variable not in vars
					throw new IllegalArgumentException("PrintMinterms: MTBDD depends on variable "+index+", not included in vars");
				}
			}
			if (vars.n() == 0) {
				throw new IllegalArgumentException("PrintMinterms: MTBDD depends on variable "+index+", not included in vars");
			}
			throw new UnsupportedOperationException("PrintMinterms: Implementation error");
		}
	}

	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVector(JDDNode dd, JDDVars vars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
		}
		DD_PrintVector(dd.ptr(), vars.array(), vars.n(), NORMAL);
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVector(JDDNode dd, JDDVars vars, int accuracy)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
		}
		DD_PrintVector(dd.ptr(), vars.array(), vars.n(), accuracy);
	}
	
	/**
	 * prints out matrix dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintMatrix(JDDNode dd, JDDVars rvars, JDDVars cvars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
		}
		DD_PrintMatrix(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), NORMAL);
	}
	
	/**
	 * prints out matrix dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintMatrix(JDDNode dd, JDDVars rvars, JDDVars cvars, int accuracy)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
		}
		DD_PrintMatrix(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), accuracy);
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVectorFiltered(JDDNode dd, JDDNode filter, JDDVars vars)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
			SanityJDD.checkIsDDOverVars(filter, vars);
		}
		DD_PrintVectorFiltered(dd.ptr(), filter.ptr(), vars.array(), vars.n(), NORMAL);
	}
	
	/**
	 * prints out vector dd
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void PrintVectorFiltered(JDDNode dd, JDDNode filter, JDDVars vars, int accuracy)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
			SanityJDD.checkIsDDOverVars(filter, vars);
		}
		DD_PrintVectorFiltered(dd.ptr(), filter.ptr(), vars.array(), vars.n(), accuracy);
	}
	
	/**
	 * traverse vector dd and call setElement method of VectorConsumer for each non zero element
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void TraverseVector(JDDNode dd, JDDVars vars, JDDVectorConsumer vc, int code)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, vars);
		}
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
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
			SanityJDD.check(rvars.n() == cvars.n(), "Mismatch of JDDVars sizes");
		}
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
	 *  [a] (x=0) -&gt; 0.3:(x'=1) + 0.7:(x'=2);
	 *  [b] (x=0) -&gt; 1:(x'=2);
	 *  [a] (x=2) -&gt; (x'=1);
	 *  [a] (x=1) -&gt; (x'=0);
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
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars, nvars);
			SanityJDD.check(rvars.n() == cvars.n(), "Mismatch of JDDVars sizes");
		}
		DD_Export3dMatrixToPPFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), nvars.array(), nvars.n(), filename);
	}

	/**
	 * export matrix dd to a matlab file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportMatrixToMatlabFile(JDDNode dd, JDDVars rvars, JDDVars cvars, String name, String filename)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
			SanityJDD.check(rvars.n() == cvars.n(), "Mismatch of JDDVars sizes");
		}
		DD_ExportMatrixToMatlabFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), name, filename);
	}

	/**
	 * export matrix dd to a spy file
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void ExportMatrixToSpyFile(JDDNode dd, JDDVars rvars, JDDVars cvars, int depth, String filename)
	{
		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(dd, rvars, cvars);
			SanityJDD.check(rvars.n() == cvars.n(), "Mismatch of JDDVars sizes");
		}
		DD_ExportMatrixToSpyFile(dd.ptr(), rvars.array(), rvars.n(), cvars.array(), cvars.n(), depth, filename);
	}

	/**
	 * Convert a (referenced) ptr returned from Cudd into a JDDNode.
	 * <br>Throws a CuddOutOfMemoryException if the pointer is NULL.
	 * <br>[ REFS: <i>none</i> ]
	 */
	public static JDDNode ptrToNode(long ptr)
	{
		if (ptr == 0L) {
			throw new CuddOutOfMemoryException();
		}
		if (DebugJDD.debugEnabled) {
			return DebugJDD.ptrToNode(ptr);
		} else {
			return new JDDNode(ptr);
		}
	}

	/**
	 * Check whether the DD error flag is set, indicating an
	 * out-of-memory situation in CUDD or another internal error.
	 * If the flag is set, throws a {@code CuddOutOfMemoryException}.
	 */
	public static void checkForCuddError()
	{
		if (DD_GetErrorFlag())
			throw new CuddOutOfMemoryException();
	}

}

//------------------------------------------------------------------------------
