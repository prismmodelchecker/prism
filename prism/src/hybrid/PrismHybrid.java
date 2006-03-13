//==============================================================================
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

package hybrid;

import prism.*;
import jdd.*;
import dv.*;
import odd.*;

//------------------------------------------------------------------------------

public class PrismHybrid
{
	//------------------------------------------------------------------------------
	// load jni stuff from shared library
	//------------------------------------------------------------------------------

	static
	{
		try {
			System.loadLibrary("prismhybrid");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	//------------------------------------------------------------------------------
	// initialise/close down methods
	//------------------------------------------------------------------------------

	public static void initialise(PrismLog mainLog, PrismLog techLog)
	{
		setCUDDManager();
		setMainLog(mainLog);
		setTechLog(techLog);
	}
	
	public static void closeDown()
	{
		// tidy up any JNI stuff
		PH_FreeGlobalRefs();
	}

	// tidy up in jni (free global references)
	private static native void PH_FreeGlobalRefs();

	//------------------------------------------------------------------------------
	// cudd manager
	//------------------------------------------------------------------------------

	// cudd manager
	
	// jni method to set cudd manager for native code
	private static native void PH_SetCUDDManager(int ddm);
	public static void setCUDDManager()
	{
		PH_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	//------------------------------------------------------------------------------
	// logs
	//------------------------------------------------------------------------------

	// main log
	
	// place to store main log for java code
	private static PrismLog mainLog;
	// jni method to set main log for native code
	private static native void PH_SetMainLog(PrismLog log);
	// method to set main log both in java and c++
	public static void setMainLog(PrismLog log)
	{
		mainLog = log;
		PH_SetMainLog(log);
	}
	
	// tech log
	
	// place to store tech log for java code
	private static PrismLog techLog;
	// jni method to set tech log for native code
	private static native void PH_SetTechLog(PrismLog log);
	// method to set tech log both in java and c++
	public static void setTechLog(PrismLog log)
	{
		techLog = log;
		PH_SetTechLog(log);
	}

	//------------------------------------------------------------------------------
	// numerical method stuff
	//------------------------------------------------------------------------------
	
	private static native void PH_SetLinEqMethod(int i);
	public static void setLinEqMethod(int i)
	{
		PH_SetLinEqMethod(i);
	}
	
	private static native void PH_SetLinEqMethodParam(double d);
	public static void setLinEqMethodParam(double d)
	{
		PH_SetLinEqMethodParam(d);
	}
	
	private static native void PH_SetTermCrit(int i);
	public static void setTermCrit(int i)
	{
		PH_SetTermCrit(i);
	}
	
	private static native void PH_SetTermCritParam(double d);
	public static void setTermCritParam(double d)
	{
		PH_SetTermCritParam(d);
	}
	
	private static native void PH_SetMaxIters(int i);
	public static void setMaxIters(int i)
	{
		PH_SetMaxIters(i);
	}

	//------------------------------------------------------------------------------
	// sparse bits info
	//------------------------------------------------------------------------------
	
	private static native void PH_SetSBMaxMem(int i);
	public static void setSBMaxMem(int i)
	{
		PH_SetSBMaxMem(i);
	}
	
	private static native void PH_SetNumSBLevels(int i);
	public static void setNumSBLevels(int i)
	{
		PH_SetNumSBLevels(i);
	}
	
	//------------------------------------------------------------------------------
	// hybrid sor info
	//------------------------------------------------------------------------------
	
	private static native void PH_SetSORMaxMem(int i);
	public static void setSORMaxMem(int i)
	{
		PH_SetSORMaxMem(i);
	}
	
	private static native void PH_SetNumSORLevels(int i);
	public static void setNumSORLevels(int i)
	{
		PH_SetNumSORLevels(i);
	}
	
	//------------------------------------------------------------------------------
	// use "compact modified" sparse matrix storage?
	//------------------------------------------------------------------------------
	
	private static native void PH_SetCompact(boolean b);
	public static void setCompact(boolean b)
	{
		PH_SetCompact(b);
	}

	//------------------------------------------------------------------------------
	// error message
	//------------------------------------------------------------------------------
	
	private static native String PH_GetErrorMessage();
	public static String getErrorMessage()
	{
		return PH_GetErrorMessage();
	}

	//------------------------------------------------------------------------------
	// JNI wrappers for blocks of hybrid code
	//------------------------------------------------------------------------------

	//------------------------------------------------------------------------------
	// probabilistic/dtmc stuff
	//------------------------------------------------------------------------------

	// pctl bounded until (probabilistic/dtmc)
	private static native int PH_ProbBoundedUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int yes, int maybe, int bound);
	public static DoubleVector ProbBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, int bound) throws PrismException
	{
		int ptr = PH_ProbBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (probabilistic/dtmc)
	private static native int PH_ProbUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int yes, int maybe);
	public static DoubleVector ProbUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe) throws PrismException
	{
		int ptr = PH_ProbUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl reach reward (probabilistic/dtmc)
	private static native int PH_ProbReachReward(int trans, int sr, int trr, int odd, int rv, int nrv, int cv, int ncv, int goal, int inf, int maybe);
	public static DoubleVector ProbReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe) throws PrismException
	{
		int ptr = PH_ProbReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
	// nondeterministic/mdp stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (nondeterministic/mdp)
	private static native int PH_NondetBoundedUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int yes, int maybe, int time, boolean minmax);
	public static DoubleVector NondetBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, int time, boolean minmax) throws PrismException
	{
		int ptr = PH_NondetBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), time, minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (nondeterministic/mdp)
	private static native int PH_NondetUntil(int trans, int odd, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int yes, int maybe, boolean minmax);
	public static DoubleVector NondetUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax) throws PrismException
	{
		int ptr = PH_NondetUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl reach reward (nondeterministic/mdp)
	private static native int PH_NondetReachReward(int trans, int sr, int trr, int odd, int rv, int nrv, int cv, int ncv, int ndv, int nndv, int goal, int inf, int maybe, boolean minmax);
	public static DoubleVector NondetReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, boolean minmax) throws PrismException
	{
		int ptr = PH_NondetReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
	// stochastic/ctmc stuff
	//------------------------------------------------------------------------------

	// csl bounded until (stochastic/ctmc)
	private static native int PH_StochBoundedUntil(int trans, int od, int rv, int nrv, int cv, int ncv, int yes, int maybe, double time, int mult);
	public static DoubleVector StochBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, double time, DoubleVector multProbs) throws PrismException
	{
		int mult = (multProbs == null) ? 0 : multProbs.getPtr();
		int ptr = PH_StochBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), time, mult);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// csl cumulative reward (stochastic/ctmc)
	private static native int PH_StochCumulReward(int trans, int sr, int trr, int odd, int rv, int nrv, int cv, int ncv, double time);
	public static DoubleVector StochCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		int ptr = PH_StochCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// steady state (stochastic/ctmc)
	private static native int PH_StochSteadyState(int trans, int od, int init, int rv, int nrv, int cv, int ncv);
	public static DoubleVector StochSteadyState(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols) throws PrismException
	{
		int ptr = PH_StochSteadyState(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// transient (stochastic/ctmc)
	private static native int PH_StochTransient(int trans, int odd, int init, int rv, int nrv, int cv, int ncv, double time);
	public static DoubleVector StochTransient(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		int ptr = PH_StochTransient(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	//------------------------------------------------------------------------------
	// generic iterative solution methods
	//------------------------------------------------------------------------------

	// power method
	private static native int PH_Power(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose);
	public static DoubleVector Power(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose) throws PrismException
	{
		int ptr = PH_Power(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// jor method
	private static native int PH_JOR(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose, boolean row_sums, double omega);
	public static DoubleVector JOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega) throws PrismException
	{
		int ptr = PH_JOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// sor method
	private static native int PH_SOR(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose, boolean row_sums, double omega, boolean backwards);
	public static DoubleVector SOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean backwards) throws PrismException
	{
		int ptr = PH_SOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, backwards);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// psor method
	private static native int PH_PSOR(int odd, int rv, int nrv, int cv, int ncv, int a, int b, int init, boolean transpose, boolean row_sums, double omega, boolean backwards);
	public static DoubleVector PSOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean backwards) throws PrismException
	{
		int ptr = PH_PSOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, backwards);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
}

//------------------------------------------------------------------------------
