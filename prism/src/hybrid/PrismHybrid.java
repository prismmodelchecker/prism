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
	private static native void PH_SetCUDDManager(long ddm);
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
	// error message
	//------------------------------------------------------------------------------
	
	private static native String PH_GetErrorMessage();
	public static String getErrorMessage()
	{
		return PH_GetErrorMessage();
	}

	//------------------------------------------------------------------------------
	// numerical computation detail queries
	//------------------------------------------------------------------------------
	
	private static native double PH_GetLastUnif();
	public static double getLastUnif()
	{
		return PH_GetLastUnif();
	}

	//------------------------------------------------------------------------------
	// JNI wrappers for blocks of hybrid code
	//------------------------------------------------------------------------------

	//------------------------------------------------------------------------------
	// probabilistic/dtmc stuff
	//------------------------------------------------------------------------------

	// pctl bounded until (probabilistic/dtmc)
	private static native long PH_ProbBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, int bound);
	public static DoubleVector ProbBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, int bound) throws PrismException
	{
		long ptr = PH_ProbBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (probabilistic/dtmc)
	private static native long PH_ProbUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe);
	public static DoubleVector ProbUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe) throws PrismException
	{
		long ptr = PH_ProbUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl cumulative reward (probabilistic/dtmc)
	private static native long PH_ProbCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, int bound);
	public static DoubleVector ProbCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, int bound) throws PrismException
	{
		long ptr = PH_ProbCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl cumulative reward (probabilistic/dtmc)
	private static native long PH_ProbInstReward(long trans, long sr, long odd, long rv, int nrv, long cv, int ncv, int time);
	public static DoubleVector ProbInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		long ptr = PH_ProbInstReward(trans.ptr(), sr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (probabilistic/dtmc)
	private static native long PH_ProbReachReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long goal, long inf, long maybe);
	public static DoubleVector ProbReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe) throws PrismException
	{
		long ptr = PH_ProbReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// transient (probabilistic/dtmc)
	private static native long PH_ProbTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, int time);
	public static DoubleVector ProbTransient(JDDNode trans, ODDNode odd, DoubleVector init, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		long ptr = PH_ProbTransient(trans.ptr(), odd.ptr(), init.getPtr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	//----------------------------------------------------------------------------------------------
	// nondeterministic/mdp stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (nondeterministic/mdp)
	private static native long PH_NondetBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, int time, boolean minmax);
	public static DoubleVector NondetBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, int time, boolean minmax) throws PrismException
	{
		long ptr = PH_NondetBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), time, minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (nondeterministic/mdp)
	private static native long PH_NondetUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, boolean minmax);
	public static DoubleVector NondetUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax) throws PrismException
	{
		long ptr = PH_NondetUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl reach reward (nondeterministic/mdp)
	private static native long PH_NondetReachReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long goal, long inf, long maybe, boolean minmax);
	public static DoubleVector NondetReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, boolean minmax) throws PrismException
	{
		long ptr = PH_NondetReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
	// stochastic/ctmc stuff
	//------------------------------------------------------------------------------

	// csl bounded until (stochastic/ctmc)
	private static native long PH_StochBoundedUntil(long trans, long od, long rv, int nrv, long cv, int ncv, long yes, long maybe, double time, long mult);
	public static DoubleVector StochBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, double time, DoubleVector multProbs) throws PrismException
	{
		long mult = (multProbs == null) ? 0 : multProbs.getPtr();
		long ptr = PH_StochBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), time, mult);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// csl cumulative reward (stochastic/ctmc)
	private static native long PH_StochCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, double time);
	public static DoubleVector StochCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		long ptr = PH_StochCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// steady state (stochastic/ctmc)
	private static native long PH_StochSteadyState(long trans, long od, long init, long rv, int nrv, long cv, int ncv);
	public static DoubleVector StochSteadyState(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols) throws PrismException
	{
		long ptr = PH_StochSteadyState(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// transient (stochastic/ctmc)
	private static native long PH_StochTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, double time);
	public static DoubleVector StochTransient(JDDNode trans, ODDNode odd, DoubleVector init, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		long ptr = PH_StochTransient(trans.ptr(), odd.ptr(), init.getPtr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	//------------------------------------------------------------------------------
	// generic iterative solution methods
	//------------------------------------------------------------------------------

	// power method
	private static native long PH_Power(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose);
	public static DoubleVector Power(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose) throws PrismException
	{
		long ptr = PH_Power(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// jor method
	private static native long PH_JOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega);
	public static DoubleVector JOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega) throws PrismException
	{
		long ptr = PH_JOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// sor method
	private static native long PH_SOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega, boolean backwards);
	public static DoubleVector SOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean backwards) throws PrismException
	{
		long ptr = PH_SOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, backwards);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// psor method
	private static native long PH_PSOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega, boolean backwards);
	public static DoubleVector PSOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean backwards) throws PrismException
	{
		long ptr = PH_PSOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, backwards);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
}

//------------------------------------------------------------------------------
