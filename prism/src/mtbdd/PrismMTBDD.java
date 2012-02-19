//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Gethin Norman <gethin.norman@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package mtbdd;

import java.io.FileNotFoundException;

import prism.*;
import jdd.*;
import odd.*;

//------------------------------------------------------------------------------

public class PrismMTBDD
{
	//------------------------------------------------------------------------------
	// load jni stuff from shared library
	//------------------------------------------------------------------------------

	static
	{
		try {
			System.loadLibrary("prismmtbdd");
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
		PM_FreeGlobalRefs();
	}

	// tidy up in jni (free global references)
	private static native void PM_FreeGlobalRefs();

	//------------------------------------------------------------------------------
	// cudd manager
	//------------------------------------------------------------------------------

	// cudd manager
	
	// jni method to set cudd manager for native code
	private static native void PM_SetCUDDManager(long ddm);
	public static void setCUDDManager()
	{
		PM_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	//------------------------------------------------------------------------------
	// logs
	//------------------------------------------------------------------------------

	// main log
	
	// place to store main log for java code
	private static PrismLog mainLog;
	// jni method to set main log for native code
	private static native void PM_SetMainLog(PrismLog log);
	// method to set main log both in java and c++
	public static void setMainLog(PrismLog log)
	{
		mainLog = log;
		PM_SetMainLog(log);
	}
	
	// tech log
	
	// place to store tech log for java code
	private static PrismLog techLog;
	// jni method to set tech log for native code
	private static native void PM_SetTechLog(PrismLog log);
	// method to set tech log both in java and c++
	public static void setTechLog(PrismLog log)
	{
		techLog = log;
		PM_SetTechLog(log);
	}

	//------------------------------------------------------------------------------
	// error message
	//------------------------------------------------------------------------------
	
	private static native String PM_GetErrorMessage();
	public static String getErrorMessage()
	{
		return PM_GetErrorMessage();
	}

	//------------------------------------------------------------------------------
	// JNI wrappers for blocks of mtbdd code
	//------------------------------------------------------------------------------

	//------------------------------------------------------------------------------
	// reachability based stuff
	//------------------------------------------------------------------------------

	// reachability
	private static native long PM_Reachability(long trans01, long rv, int nrv, long cv, int ncv, long start);
	public static JDDNode Reachability(JDDNode trans01, JDDVars rows, JDDVars cols, JDDNode start)
	{
		long ptr = PM_Reachability(trans01.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), start.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until probability 1 precomputation (probabilistic/dtmc)
	private static native long PM_Prob1(long trans01, long reach, long rv, int nrv, long cv, int ncv, long b1, long b2, long no);
	public static JDDNode Prob1(JDDNode trans01, JDDNode reach,JDDVars rows, JDDVars cols, JDDNode b1, JDDNode b2, JDDNode no)// throws PrismException
	{
		long ptr = PM_Prob1(trans01.ptr(), reach.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), b1.ptr(), b2.ptr(), no.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until probability 0 precomputation (probabilistic/dtmc)
	private static native long PM_Prob0(long trans01, long reach, long rv, int nrv, long cv, int ncv, long b1, long b2);
	public static JDDNode Prob0(JDDNode trans01, JDDNode reach, JDDVars rows, JDDVars cols, JDDNode b1, JDDNode b2)// throws PrismException
	{
		long ptr = PM_Prob0(trans01.ptr(), reach.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), b1.ptr(), b2.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until probability 1 precomputation - there exists (nondeterministic/mdp)
	private static native long PM_Prob1E(long trans01, long reach, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long b1, long b2, long no);
	public static JDDNode Prob1E(JDDNode trans01, JDDNode reach, JDDVars rows, JDDVars cols, JDDVars nd, JDDNode b1, JDDNode b2, JDDNode no)// throws PrismException
	{
		long ptr = PM_Prob1E(trans01.ptr(), reach.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nd.array(), nd.n(), b1.ptr(), b2.ptr(), no.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until probability 1 precomputation - for all (nondeterministic/mdp)
	private static native long PM_Prob1A(long trans01, long reach, long mask, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long no, long b2);
	public static JDDNode Prob1A(JDDNode trans01, JDDNode reach, JDDNode nondetMask, JDDVars rows, JDDVars cols, JDDVars nd, JDDNode no, JDDNode b2)// throws PrismException
	{
		long ptr = PM_Prob1A(trans01.ptr(), reach.ptr(), nondetMask.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nd.array(), nd.n(), no.ptr(), b2.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until probability 0 precomputation - there exists (nondeterministic/mdp)
	private static native long PM_Prob0E(long trans01, long reach, long mask, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long b1, long b2);
	public static JDDNode Prob0E(JDDNode trans01, JDDNode reach, JDDNode nondetMask, JDDVars rows, JDDVars cols, JDDVars nd, JDDNode b1, JDDNode b2)// throws PrismException
	{
		long ptr = PM_Prob0E(trans01.ptr(), reach.ptr(), nondetMask.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nd.array(), nd.n(), b1.ptr(), b2.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until probability 0 precomputation - for all (nondeterministic/mdp)
	private static native long PM_Prob0A(long trans01, long reach, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long b1, long b2);
	public static JDDNode Prob0A(JDDNode trans01, JDDNode reach, JDDVars rows, JDDVars cols, JDDVars nd, JDDNode b1, JDDNode b2)// throws PrismException
	{
		long ptr = PM_Prob0A(trans01.ptr(), reach.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nd.array(), nd.n(), b1.ptr(), b2.ptr());
		//if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	//------------------------------------------------------------------------------
	// probabilistic/dtmc stuff
	//------------------------------------------------------------------------------

	// pctl bounded until (probabilistic/dtmc)
	private static native long PM_ProbBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, int bound);
	public static JDDNode ProbBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, int bound) throws PrismException
	{
		long ptr = PM_ProbBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until (probabilistic/dtmc)
	private static native long PM_ProbUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe);
	public static JDDNode ProbUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe) throws PrismException
	{
		long ptr = PM_ProbUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl cumulative reward (probabilistic/dtmc)
	private static native long PM_ProbCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, int bound);
	public static JDDNode ProbCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, int bound) throws PrismException
	{
		long ptr = PM_ProbCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), bound);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	// pctl inst reward (probabilistic/dtmc)
	private static native long PM_ProbInstReward(long trans, long sr, long odd, long rv, int nrv, long cv, int ncv, int time);
	public static JDDNode ProbInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		long ptr = PM_ProbInstReward(trans.ptr(), sr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	// pctl reach reward (probabilistic/dtmc)
	private static native long PM_ProbReachReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long goal, long inf, long maybe);
	public static JDDNode ProbReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe) throws PrismException
	{
		long ptr = PM_ProbReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	// transient (probabilistic/dtmc)
	private static native long PM_ProbTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, int time);
	public static JDDNode ProbTransient(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		long ptr = PM_ProbTransient(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	//------------------------------------------------------------------------------
	// nondeterministic/mdp stuff
	//------------------------------------------------------------------------------

	// pctl bounded until (nondeterministic/mdp)
	private static native long PM_NondetBoundedUntil(long trans, long odd, long mask, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, int bound, boolean minmax);
	public static JDDNode NondetBoundedUntil(JDDNode trans, ODDNode odd, JDDNode nondetMask, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, int bound, boolean minmax) throws PrismException
	{
		long ptr = PM_NondetBoundedUntil(trans.ptr(), odd.ptr(), nondetMask.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), bound, minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl until (nondeterministic/mdp)
	private static native long PM_NondetUntil(long trans, long odd, long mask, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, boolean minmax);
	public static JDDNode NondetUntil(JDDNode trans, ODDNode odd, JDDNode nondetMask, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax) throws PrismException
	{
		long ptr = PM_NondetUntil(trans.ptr(), odd.ptr(), nondetMask.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// pctl inst reward (nondeterministic/mdp)
	private static native long PM_NondetInstReward(long trans, long sr, long odd, long mask, long rv, int nrv, long cv, int ncv, long ndv, int nndv, int time, boolean minmax, long init);
	public static JDDNode NondetInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDNode nondetMask, JDDVars rows, JDDVars cols, JDDVars nondet, int time, boolean minmax, JDDNode init) throws PrismException
	{
		long ptr = PM_NondetInstReward(trans.ptr(), sr.ptr(), odd.ptr(), nondetMask.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), time, minmax, init.ptr());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	// pctl reach reward (nondeterministic/mdp)
	private static native long PM_NondetReachReward(long trans, long sr, long trr, long odd, long mask, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long goal, long inf, long maybe, boolean minmax);
	public static JDDNode NondetReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDNode nondetMask, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, boolean minmax) throws PrismException
	{
		long ptr = PM_NondetReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), nondetMask.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	//------------------------------------------------------------------------------
	// stochastic/ctmc stuff
	//------------------------------------------------------------------------------

	// csl time bounded until (stochastic/ctmc)
	private static native long PM_StochBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, double time, long mult);
	public static JDDNode StochBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, double time, JDDNode multProbs) throws PrismException
	{
		long mult = (multProbs == null) ? 0 : multProbs.ptr();
		long ptr = PM_StochBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), time, mult);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// csl cumulative reward (stochastic/ctmc)
	private static native long PM_StochCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, double time);
	public static JDDNode StochCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		long ptr = PM_StochCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// steady state (stochastic/ctmc)
	private static native long PM_StochSteadyState(long trans, long odd, long init, long rv, int nrv, long cv, int ncv);
	public static JDDNode StochSteadyState(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols) throws PrismException
	{
		long ptr = PM_StochSteadyState(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n());
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// transient (stochastic/ctmc)
	private static native long PM_StochTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, double time);
	public static JDDNode StochTransient(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		long ptr = PM_StochTransient(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	//------------------------------------------------------------------------------
	// export methods
	//------------------------------------------------------------------------------

	// export vector
	private static native int PM_ExportVector(long vector, String name, long vars, int nv, long odd, int exportType, String filename);
	public static void ExportVector(JDDNode vector, String name, JDDVars vars, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PM_ExportVector(vector.ptr(), name, vars.array(), vars.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}
	
	// export matrix
	private static native int PM_ExportMatrix(long matrix, String name, long rv, int nrv, long cv, int ncv, long odd, int exportType, String filename);
	public static void ExportMatrix(JDDNode matrix, String name, JDDVars rows, JDDVars cols, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		int res = PM_ExportMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}
	
	// export labels
	private static native int PM_ExportLabels(long labels[], String labelNames[], String name, long vars, int nv, long odd, int exportType, String filename);
	public static void ExportLabels(JDDNode labels[], String labelNames[], String name, JDDVars vars, ODDNode odd, int exportType, String filename) throws FileNotFoundException
	{
		long ptrs[] = new long[labels.length];
		for (int i = 0; i < labels.length; i++)
			ptrs[i] = labels[i].ptr();
		int res = PM_ExportLabels(ptrs, labelNames, name, vars.array(), vars.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
	}

	//------------------------------------------------------------------------------
	// generic iterative solution methods
	//------------------------------------------------------------------------------

	// power method
	private static native long PM_Power(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose);
	public static JDDNode Power(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose) throws PrismException
	{
		long ptr = PM_Power(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}
	
	// jor method
	private static native long PM_JOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, double omega);
	public static JDDNode JOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, double omega) throws PrismException
	{
		long ptr = PM_JOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, omega);
		if (ptr == 0) throw new PrismException(getErrorMessage());
		return new JDDNode(ptr);
	}

	//------------------------------------------------------------------------------
}
