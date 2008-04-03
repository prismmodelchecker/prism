//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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
	private static native void PS_SetCUDDManager(long ddm);
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

	//------------------------------------------------------------------------------
	// use steady-state detection?
	//------------------------------------------------------------------------------
	
	private static native void PS_SetDoSSDetect(boolean b);
	public static void setDoSSDetect(boolean b)
	{
		PS_SetDoSSDetect(b);
	}

	//------------------------------------------------------------------------------
	// error message
	//------------------------------------------------------------------------------
	
	private static native String PS_GetErrorMessage();
	public static String getErrorMessage()
	{
		return PS_GetErrorMessage();
	}

	//----------------------------------------------------------------------------------------------
	// JNI wrappers for blocks of sparse code
	//----------------------------------------------------------------------------------------------

	//----------------------------------------------------------------------------------------------
	// probabilistic/dtmc stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (probabilistic/dtmc)
	private static native long PS_ProbBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, int bound);
	public static DoubleVector ProbBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, int bound) throws PrismException
	{
		long ptr = PS_ProbBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (probabilistic/dtmc)
	private static native long PS_ProbUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe);
	public static DoubleVector ProbUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe) throws PrismException
	{
		long ptr = PS_ProbUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl cumulative reward (probabilistic/dtmc)
	private static native long PS_ProbCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, int bound);
	public static DoubleVector ProbCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, int bound) throws PrismException
	{
		long ptr = PS_ProbCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl inst reward (probabilistic/dtmc)
	private static native long PS_ProbInstReward(long trans, long sr, long odd, long rv, int nrv, long cv, int ncv, int time);
	public static DoubleVector ProbInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		long ptr = PS_ProbInstReward(trans.ptr(), sr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (probabilistic/dtmc)
	private static native long PS_ProbReachReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long goal, long inf, long maybe);
	public static DoubleVector ProbReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe) throws PrismException
	{
		long ptr = PS_ProbReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// transient (probabilistic/dtmc)
	private static native long PS_ProbTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, int time);
	public static DoubleVector ProbTransient(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		long ptr = PS_ProbTransient(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
	// nondeterministic/mdp stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (nondeterministic/mdp)
	private static native long PS_NondetBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, int time, boolean minmax);
	public static DoubleVector NondetBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, int time, boolean minmax) throws PrismException
	{
		long ptr = PS_NondetBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), time, minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (nondeterministic/mdp)
	private static native long PS_NondetUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, boolean minmax);
	public static DoubleVector NondetUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax) throws PrismException
	{
		long ptr = PS_NondetUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl inst reward (nondeterministic/mdp)
	private static native long PS_NondetInstReward(long trans, long sr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, int time, boolean minmax, long init);
	public static DoubleVector NondetInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, int time, boolean minmax, JDDNode init) throws PrismException
	{
		long ptr = PS_NondetInstReward(trans.ptr(), sr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), time, minmax, init.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (nondeterministic/mdp)
	private static native long PS_NondetReachReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long goal, long inf, long maybe, boolean minmax);
	public static DoubleVector NondetReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, boolean minmax) throws PrismException
	{
		long ptr = PS_NondetReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
	// stochastic/ctmc stuff
	//----------------------------------------------------------------------------------------------

	// csl time bounded until (stochastic/ctmc)
	private static native long PS_StochBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, double time, long mult);
	public static DoubleVector StochBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, double time, DoubleVector multProbs) throws PrismException
	{
		long mult = (multProbs == null) ? 0 : multProbs.getPtr();
		long ptr = PS_StochBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), time, mult);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// csl cumulative reward (stochastic/ctmc)
	private static native long PS_StochCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, double time);
	public static DoubleVector StochCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		long ptr = PS_StochCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// steady state (stochastic/ctmc)
	private static native long PS_StochSteadyState(long trans, long odd, long init, long rv, int nrv, long cv, int ncv);
	public static DoubleVector StochSteadyState(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols) throws PrismException
	{
		long ptr = PS_StochSteadyState(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// transient (stochastic/ctmc)
	private static native long PS_StochTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, double time);
	public static DoubleVector StochTransient(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		long ptr = PS_StochTransient(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
	// export methods
	//------------------------------------------------------------------------------

	// export matrix
	private static native int PS_ExportMatrix(long matrix, String name, long rv, int nrv, long cv, int ncv, long odd, int exportType, String filename);
	public static void ExportMatrix(JDDNode matrix, String name, JDDVars rows, JDDVars cols, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PS_ExportMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}
	
	// export mdp
	private static native int PS_ExportMDP(long mdp, String name, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long odd, int exportType, String filename);
	public static void ExportMDP(JDDNode mdp, String name, JDDVars rows, JDDVars cols, JDDVars nondet, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PS_ExportMDP(mdp.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}
	
	// export sub-mdp, i.e. mdp transition rewards
	private static native int PS_ExportSubMDP(long mdp, long submdp, String name, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long odd, int exportType, String filename);
	public static void ExportSubMDP(JDDNode mdp, JDDNode submdp, String name, JDDVars rows, JDDVars cols, JDDVars nondet, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PS_ExportSubMDP(mdp.ptr(), submdp.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}

	//------------------------------------------------------------------------------
	// generic iterative solution methods
	//------------------------------------------------------------------------------

	// power method
	private static native long PS_Power(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose);
	public static DoubleVector Power(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose) throws PrismException
	{
		long ptr = PS_Power(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// jor method
	private static native long PS_JOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega);
	public static DoubleVector JOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega) throws PrismException
	{
		long ptr = PS_JOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// sor method
	private static native long PS_SOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega, boolean forwards);
	public static DoubleVector SOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean forwards) throws PrismException
	{
		long ptr = PS_SOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, forwards);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
}

//------------------------------------------------------------------------------
