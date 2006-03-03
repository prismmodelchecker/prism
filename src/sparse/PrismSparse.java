//==============================================================================
//	
//	File:		PrismSparse.java
//	Author:		Dave Parker
//	Date:		10/04/01
//	Desc:		Main Java file for sparse engine
//				(JNI wrappers for get/set methods for package wide global variables)
//				(JNI wrappers for blocks of sparse code)
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

package sparse;

import java.io.FileNotFoundException;

import prism.*;
import jdd.*;
import dv.*;
import odd.*;

//----------------------------------------------------------------------------------------------

public class PrismSparse
{
	//----------------------------------------------------------------------------------------------
	// load jni stuff from shared library
	//----------------------------------------------------------------------------------------------

	static
	{
		try {
			System.loadLibrary("prismsparse");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	//----------------------------------------------------------------------------------------------
	// initialise/close down methods
	//----------------------------------------------------------------------------------------------

	public static void initialise(PrismLog mainLog, PrismLog techLog)
	{
		setCUDDManager();
		setMainLog(mainLog);
		setTechLog(techLog);
	}
	
	public static void closeDown()
	{
		// tidy up any JNI stuff
		PS_FreeGlobalRefs();
	}
	
	// tidy up in jni (free global references)
	private static native void PS_FreeGlobalRefs();

	//----------------------------------------------------------------------------------------------
	// cudd manager
	//----------------------------------------------------------------------------------------------

	// cudd manager
	
	// jni method to set cudd manager for native code
	private static native void PS_SetCUDDManager(int ddm);
	public static void setCUDDManager()
	{
		PS_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	//----------------------------------------------------------------------------------------------
	// logs
	//----------------------------------------------------------------------------------------------

	// main log
	
	// place to store main log for java code
	private static PrismLog mainLog;
	// jni method to set main log for native code
	private static native void PS_SetMainLog(PrismLog log);
	// method to set main log both in java and c++
	public static void setMainLog(PrismLog log)
	{
		mainLog = log;
		PS_SetMainLog(log);
	}
	
	// tech log
	
	// place to store tech log for java code
	private static PrismLog techLog;
	// jni method to set tech log for native code
	private static native void PS_SetTechLog(PrismLog log);
	// method to set tech log both in java and c++
	public static void setTechLog(PrismLog log)
	{
		techLog = log;
		PS_SetTechLog(log);
	}

	//------------------------------------------------------------------------------
	// numerical method stuff
	//----------------------------------------------------------------------------------------------
	
	private static native void PS_SetLinEqMethod(int i);
	public static void setLinEqMethod(int i)
	{
		PS_SetLinEqMethod(i);
	}
	
	private static native void PS_SetLinEqMethodParam(double d);
	public static void setLinEqMethodParam(double d)
	{
		PS_SetLinEqMethodParam(d);
	}
	
	private static native void PS_SetTermCrit(int i);
	public static void setTermCrit(int i)
	{
		PS_SetTermCrit(i);
	}
	
	private static native void PS_SetTermCritParam(double d);
	public static void setTermCritParam(double d)
	{
		PS_SetTermCritParam(d);
	}
	
	private static native void PS_SetMaxIters(int i);
	public static void setMaxIters(int i)
	{
		PS_SetMaxIters(i);
	}

	//------------------------------------------------------------------------------
	// use "compact modified" sparse matrix storage?
	//------------------------------------------------------------------------------

	private static native void PS_SetCompact(boolean b);
	public static void setCompact(boolean b)
	{
		PS_SetCompact(b);
	}

	//----------------------------------------------------------------------------------------------
	// JNI wrappers for blocks of sparse code
	//----------------------------------------------------------------------------------------------

	//----------------------------------------------------------------------------------------------
	// probabilistic/dtmc stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (probabilistic/dtmc)
	private static native int PS_ProbBoundedUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int yes, int maybe, int bound);
	public static DoubleVector ProbBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, int bound)
	{
		int ptr = PS_ProbBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), bound);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (probabilistic/dtmc)
	private static native int PS_ProbUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int yes, int maybe);
	public static DoubleVector ProbUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe)
	{
		int ptr = PS_ProbUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr());
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl reach reward (probabilistic/dtmc)
	private static native int PS_ProbReachReward(int trans, int sr, int trr, int odd, int rv, int nrv, int cv, int ncv, int goal, int inf, int maybe);
	public static DoubleVector ProbReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe)
	{
		int ptr = PS_ProbReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr());
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
	// nondeterministic/mdp stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (nondeterministic/mdp)
	private static native int PS_NondetBoundedUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int yes, int maybe, int time, boolean minmax);
	public static DoubleVector NondetBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, int time, boolean minmax)
	{
		int ptr = PS_NondetBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), time, minmax);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (nondeterministic/mdp)
	private static native int PS_NondetUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int yes, int maybe, boolean minmax);
	public static DoubleVector NondetUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax)
	{
		int ptr = PS_NondetUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl reach reward (nondeterministic/mdp)
	private static native int PS_NondetReachReward(int trans, int sr, int trr, int odd, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int goal, int inf, int maybe, boolean minmax);
	public static DoubleVector NondetReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, boolean minmax)
	{
		int ptr = PS_NondetReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), minmax);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
	// stochastic/ctmc stuff
	//----------------------------------------------------------------------------------------------

	// csl time bounded until (stochastic/ctmc)
	private static native int PS_StochBoundedUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int yes, int maybe, double time, int mult);
	public static DoubleVector StochBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, double time, DoubleVector multProbs)
	{
		int mult = (multProbs == null) ? 0 : multProbs.getPtr();
		int ptr = PS_StochBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), time, mult);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// csl cumulative reward (stochastic/ctmc)
	private static native int PS_StochCumulReward(int trans, int sr, int trr, int odd, int rv, int nrv, int cv, int ncv, double time);
	public static DoubleVector StochCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, double time)
	{
		int ptr = PS_StochCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// steady state (stochastic/ctmc)
	private static native int PS_StochSteadyState(int trans, int odd, int init, int rv, int nrv, int cv, int ncv);
	public static DoubleVector StochSteadyState(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols)
	{
		int ptr = PS_StochSteadyState(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n());
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// transient (stochastic/ctmc)
	private static native int PS_StochTransient(int trans, int odd, int init, int rv, int nrv, int cv, int ncv, double time);
	public static DoubleVector StochTransient(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols, double time)
	{
		int ptr = PS_StochTransient(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
	// export methods
	//------------------------------------------------------------------------------

	// export (probabilistic/dtmc)
	private static native int PS_ProbExport(int trans, int rv, int nrv, int cv, int ncv, int odd, int exportType, String filename);
	public static void ProbExport(JDDNode trans, JDDVars rows, JDDVars cols, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PS_ProbExport(trans.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}
	
	// export (stochastic/ctmc)
	private static native int PS_StochExport(int trans, int rv, int nrv, int cv, int ncv, int odd, int exportType, String filename);
	public static void StochExport(JDDNode trans, JDDVars rows, JDDVars cols, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PS_StochExport(trans.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}
	
	// export (nondeterministic/mdp)
	private static native int PS_NondetExport(int trans, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int odd, int exportType, String filename);
	public static void NondetExport(JDDNode trans, JDDVars rows, JDDVars cols, JDDVars nondet, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PS_NondetExport(trans.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}

	//------------------------------------------------------------------------------
	// generic iterative solution methods
	//------------------------------------------------------------------------------

	// power method
	private static native int PS_Power(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose);
	public static DoubleVector Power(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose)
	{
		int ptr = PS_Power(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// jor method
	private static native int PS_JOR(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose, boolean row_sums, double omega);
	public static DoubleVector JOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega)
	{
		int ptr = PS_JOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// sor method
	private static native int PS_SOR(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose, boolean row_sums, double omega, boolean forwards);
	public static DoubleVector SOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean forwards)
	{
		int ptr = PS_SOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, forwards);
		return (ptr == 0) ? null : new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
}

//------------------------------------------------------------------------------
