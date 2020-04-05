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
import java.util.List;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import odd.ODDUtils;
import prism.NativeIntArray;
import prism.OpsAndBoundsList;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNative;
import prism.PrismNotSupportedException;
import dv.DoubleVector;
import dv.IntegerVector;

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

	/**
	 * Check that number of reachable states is in a range that can be handled by
	 * the sparse engine methods.
	 * @throws PrismNotSupportedException if that is not the case
	 */
	private static void checkNumStates(ODDNode odd) throws PrismNotSupportedException
	{
		// currently, the sparse engine internally uses int (signed 32bit) index values
		// so, if the number of states is larger than Integer.MAX_VALUE, there is a problem
		ODDUtils.checkInt(odd, "Currently, the sparse engine cannot handle models");
	}

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

	private static native void PS_SetExportIterations(boolean value);
	public static void SetExportIterations(boolean value)
	{
		PS_SetExportIterations(value);
	}

	//------------------------------------------------------------------------------
	// error message
	//------------------------------------------------------------------------------
	
	private static native String PS_GetErrorMessage();
	public static String getErrorMessage()
	{
		return PS_GetErrorMessage();
	}

	/**
	 * Generate the proper exception for an error from the native layer.
	 * Gets the error message and returns the corresponding exception,
	 * i.e., if the message contains "not supported" then a PrismNotSupportedException
	 * is returned, otherwise a plain PrismException.
	 */
	private static PrismException generateExceptionForError()
	{
		String msg = getErrorMessage();
		if (msg.contains("not supported")) {
			return new PrismNotSupportedException(msg);
		} else {
			return new PrismException(msg);
		}
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
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), bound);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (probabilistic/dtmc)
	private static native long PS_ProbUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe);
	public static DoubleVector ProbUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr());
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl until (probabilistic/dtmc), interval variant
	private static native long PS_ProbUntilInterval(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, int flags);
	public static DoubleVector ProbUntilInterval(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbUntilInterval(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl cumulative reward (probabilistic/dtmc)
	private static native long PS_ProbCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, int bound);
	public static DoubleVector ProbCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, int bound) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), bound);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl inst reward (probabilistic/dtmc)
	private static native long PS_ProbInstReward(long trans, long sr, long odd, long rv, int nrv, long cv, int ncv, int time);
	public static DoubleVector ProbInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbInstReward(trans.ptr(), sr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (probabilistic/dtmc)
	private static native long PS_ProbReachReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long goal, long inf, long maybe);
	public static DoubleVector ProbReachReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbReachReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr());
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (probabilistic/dtmc), interval variant
	private static native long PS_ProbReachRewardInterval(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long goal, long inf, long maybe, long lower, long upper, int flags);
	public static DoubleVector ProbReachRewardInterval(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode goal, JDDNode inf, JDDNode maybe, JDDNode lower, JDDNode upper, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbReachRewardInterval(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), goal.ptr(), inf.ptr(), maybe.ptr(), lower.ptr(), upper.ptr(), flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// transient (probabilistic/dtmc)
	private static native long PS_ProbTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, int time);
	public static DoubleVector ProbTransient(JDDNode trans, ODDNode odd, DoubleVector init, JDDVars rows, JDDVars cols, int time) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_ProbTransient(trans.ptr(), odd.ptr(), init.getPtr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
	// nondeterministic/mdp stuff
	//----------------------------------------------------------------------------------------------

	// pctl bounded until (nondeterministic/mdp)
	private static native long PS_NondetBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, int time, boolean minmax);
	public static DoubleVector NondetBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, int time, boolean minmax) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), time, minmax);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// pctl until (nondeterministic/mdp)
	private static native long PS_NondetUntil(long trans, long trans_actions, List<String> synchs, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, boolean minmax, long strat);
	public static DoubleVector NondetUntil(JDDNode trans, JDDNode transActions, List<String> synchs, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax, IntegerVector strat) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetUntil(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax, (strat == null) ? 0 : strat.getPtr());
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl until (nondeterministic/mdp), interval iteration
	private static native long PS_NondetUntilInterval(long trans, long trans_actions, List<String> synchs, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long yes, long maybe, boolean minmax, long strat, int flags);
	public static DoubleVector NondetUntilInterval(JDDNode trans, JDDNode transActions, List<String> synchs, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode yes, JDDNode maybe, boolean minmax, IntegerVector strat, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetUntilInterval(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), yes.ptr(), maybe.ptr(), minmax, (strat == null) ? 0 : strat.getPtr(), flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl cumulative reward (probabilistic/mdp)
	private static native long PS_NondetCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, int bound, boolean minmax);
	public static DoubleVector NondetCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, int bound, boolean minmax) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), bound, minmax);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl inst reward (nondeterministic/mdp)
	private static native long PS_NondetInstReward(long trans, long sr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, int time, boolean minmax, long init);
	public static DoubleVector NondetInstReward(JDDNode trans, JDDNode sr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, int time, boolean minmax, JDDNode init) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetInstReward(trans.ptr(), sr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), time, minmax, init.ptr());
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (nondeterministic/mdp)
	private static native long PS_NondetReachReward(long trans, long trans_actions, List<String> synchs, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long goal, long inf, long maybe, boolean minmax);
	public static DoubleVector NondetReachReward(JDDNode trans, JDDNode transActions, List<String> synchs, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, boolean minmax) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetReachReward(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), minmax);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// pctl reach reward (nondeterministic/mdp), interval iteration
	private static native long PS_NondetReachRewardInterval(long trans, long trans_actions, List<String> synchs, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long goal, long inf, long maybe, long lower, long upper, boolean minmax, int flags);
	public static DoubleVector NondetReachRewardInterval(JDDNode trans, JDDNode transActions, List<String> synchs, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode goal, JDDNode inf, JDDNode maybe, JDDNode lower, JDDNode upper, boolean minmax, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_NondetReachRewardInterval(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), goal.ptr(), inf.ptr(), maybe.ptr(), lower.ptr(), upper.ptr(), minmax, flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	private static native double[] PS_NondetMultiObj(long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, boolean minmax, long start, long ptr_adversary, long ptr_TransSparseMatrix, List<String> synchs, long[] ptr_yes_vec, int[] probStepBounds, long[] ptr_RewSparseMatrix, double[] rewardWeights, int[] rewardStepBounds);
	public static double[] NondetMultiObj(ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, boolean minmax, JDDNode start, NativeIntArray adversary, NDSparseMatrix transSparseMatrix, List<String> synchs, DoubleVector[] yes_vec, int[] probStepBounds, NDSparseMatrix[] rewSparseMatrix, double[] rewardWeights, int[] rewardStepBounds) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long[] ptr_ndsp_r = null;
		if (rewSparseMatrix != null) {
			ptr_ndsp_r = new long[rewSparseMatrix.length];
			for (int i = 0; i < ptr_ndsp_r.length; i++)
				ptr_ndsp_r[i] = (rewSparseMatrix[i]!=null) ? rewSparseMatrix[i].getPtr() : 0;
		}
			
		long[] ptr_yes_vec = null;
		if (yes_vec != null) {
			ptr_yes_vec = new long[yes_vec.length];
			for (int i = 0; i < yes_vec.length; i++)
				ptr_yes_vec[i] = (yes_vec[i]!=null) ? yes_vec[i].getPtr() : 0;
		}
		
		double[] ret = PS_NondetMultiObj(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), minmax, start.ptr(), adversary.getPtr(), transSparseMatrix.getPtr(), synchs, ptr_yes_vec, probStepBounds, ptr_ndsp_r, rewardWeights, rewardStepBounds);
		if (ret == null)
			throw new PrismException(getErrorMessage());
		else
			return ret;
	
	}
	
	private static native double[] PS_NondetMultiObjGS(long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, boolean minmax, long start, long ptr_adversary, long ptr_TransSparseMatrix, long[] ptr_yes_vec, long[] ptr_RewSparseMatrix, double[] rewardWeights);
	public static double[] NondetMultiObjGS(ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, boolean minmax, JDDNode start, NativeIntArray adversary, NDSparseMatrix transSparseMatrix, DoubleVector[] yes_vec, NDSparseMatrix[] rewSparseMatrix, double[] rewardWeights) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long[] ptr_ndsp_r = null;
		if (rewSparseMatrix != null) {
			ptr_ndsp_r = new long[rewSparseMatrix.length];
			for (int i = 0; i < ptr_ndsp_r.length; i++)
				ptr_ndsp_r[i] = (rewSparseMatrix[i]!=null) ? rewSparseMatrix[i].getPtr() : 0;
		}
			
		long[] ptr_yes_vec = null;
		if (yes_vec != null) {
			ptr_yes_vec = new long[yes_vec.length];
			for (int i = 0; i < yes_vec.length; i++)
				ptr_yes_vec[i] = (yes_vec[i]!=null) ? yes_vec[i].getPtr() : 0;
		}
		
		double[] ret = PS_NondetMultiObjGS(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), minmax, start.ptr(), adversary.getPtr(), transSparseMatrix.getPtr(), ptr_yes_vec, ptr_ndsp_r, rewardWeights);
		if (ret == null)
			throw new PrismException(getErrorMessage());
		else
			return ret;
	}

	// multi-objective (nondeterministic/mdp)
	private static native double PS_NondetMultiReach(long trans, long trans_actions, List<String> synchs, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long targets[], int relops[], double bounds[], long maybe, long start);
	public static double NondetMultiReach(JDDNode trans, JDDNode transActions, List<String> synchs, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, List<JDDNode> targets, OpsAndBoundsList opsAndBounds, JDDNode maybe, JDDNode start) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		// Convert lists to arrays for passing to JNI
		int i, n = targets.size();
		long targetsArr[] = new long[n];
		int relOpsArr[] = new int[n];
		double boundsArr[] = new double[n];
		for (i = 0; i < n; i++) {
			targetsArr[i] = targets.get(i).ptr();
			relOpsArr[i] = opsAndBounds.getProbOperator(i).toNumber();
			boundsArr[i] = opsAndBounds.getProbBound(i);
		}
		double res = //relOps.get(0).intValue()>2 ? 
				PS_NondetMultiReach(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, relOpsArr, boundsArr, maybe.ptr(), start.ptr()); //:
					//PS_NondetMultiReach(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, relOpsArr, boundsArr, maybe.ptr(), start.ptr(), (checkReach? 1 : 0),
							//0, 0);
		if (res == -1) throw new PrismException(getErrorMessage());
		return res;
	}
	
	private static native double PS_NondetMultiReach1(long trans, long trans_actions, List<String> synchs, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long targets[], long combinations[], int combinationIDs[], int relops[], double bounds[], long maybe, long start);
	public static double NondetMultiReach1(JDDNode trans, JDDNode transActions, List<String> synchs, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, List<JDDNode> targets, List<JDDNode> combinations, List<Integer> combinationIDs, OpsAndBoundsList opsAndBounds, JDDNode maybe, JDDNode start) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		// Convert lists to arrays for passing to JNI
		int i, n = targets.size();
		long targetsArr[] = new long[n];
		int relOpsArr[] = new int[n];
		double boundsArr[] = new double[n];
		for (i = 0; i < n; i++) {
			targetsArr[i] = targets.get(i).ptr();
			relOpsArr[i] = opsAndBounds.getProbOperator(i).toNumber();
			boundsArr[i] = opsAndBounds.getProbBound(i);
		}
		long combinationsArr[] = new long[combinations.size()];
		int combinationIDsArr[] = new int[combinationIDs.size()];
		for(i=0; i<combinations.size(); i++) {
			combinationsArr[i] = combinations.get(i).ptr();
		}
		for(i=0; i<combinationIDs.size(); i++)
			combinationIDsArr[i] = combinationIDs.get(i);
		double res = //relOps.get(0).intValue()>2 ? 
				PS_NondetMultiReach1(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, combinationsArr, combinationIDsArr, relOpsArr, boundsArr, maybe.ptr(), start.ptr()); //:
					//PS_NondetMultiReach(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, relOpsArr, boundsArr, maybe.ptr(), start.ptr(), (checkReach? 1 : 0),
							//0, 0);
		if (res == -1) throw new PrismException(getErrorMessage());
		return res;
	}
	
	// multi-objective (nondeterministic/mdp)
	private static native double PS_NondetMultiReachReward(long trans, long trans_actions, List<String> synchs, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long targets[], int relopsProb[], double boundsProb[], int relopsReward[], double boundsReward[], long maybe, long start, long trr[], long becs);
	public static double NondetMultiReachReward(JDDNode trans, JDDNode transActions, List<String> synchs, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, List<JDDNode> targets, OpsAndBoundsList opsAndBounds, JDDNode maybe, JDDNode start,
			List<JDDNode> trr, JDDNode becs) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		// Convert lists to arrays for passing to JNI
		int i;//, n = targets.size();
		long targetsArr[] = new long[targets.size()];
		int relOpsProbArr[] = new int[opsAndBounds.probSize()];
		double boundsProbArr[] = new double[opsAndBounds.probSize()];
		int relOpsRewardArr[] = new int[opsAndBounds.rewardSize()];
		double boundsRewardArr[] = new double[opsAndBounds.rewardSize()];
		long trrArr[] = new long[trr.size()];
		long becsArr = becs.ptr();
		for (i = 0; i < targets.size(); i++) 
			targetsArr[i] = targets.get(i).ptr();
		for (i = 0; i < opsAndBounds.probSize(); i++) {
			relOpsProbArr[i] = opsAndBounds.getProbOperator(i).toNumber();
			boundsProbArr[i] = opsAndBounds.getProbBound(i);
		}
		for (i = 0; i < opsAndBounds.rewardSize(); i++) {
			relOpsRewardArr[i] = opsAndBounds.getRewardOperator(i).toNumber();
			boundsRewardArr[i] = opsAndBounds.getRewardBound(i);
		}
		for (i = 0; i < trr.size(); i++) 
			trrArr[i] = trr.get(i).ptr();
		
		double res = //relOps.get(0).intValue()>2 ? 
				PS_NondetMultiReachReward(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, relOpsProbArr, boundsProbArr, relOpsRewardArr, boundsRewardArr, maybe.ptr(), start.ptr(),
				trrArr, becsArr); //:
					//PS_NondetMultiReach(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, relOpsArr, boundsArr, maybe.ptr(), start.ptr(), (checkReach? 1 : 0),
							//0, 0);
		if (res == -1) throw new PrismException(getErrorMessage());
		return res;
	}
	private static native double PS_NondetMultiReachReward1(long trans, long trans_actions, List<String> synchs, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long targets[], long combinations[], int combinationIDs[], int relopsProb[], double boundsProb[], int relopsReward[], double boundsReward[], long maybe, long start, long trr[], long becs);
	public static double NondetMultiReachReward1(JDDNode trans, JDDNode transActions, List<String> synchs, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, List<JDDNode> targets, List<JDDNode> combinations, List<Integer> combinationIDs, OpsAndBoundsList opsAndBounds, JDDNode maybe, JDDNode start,
			List<JDDNode> trr, JDDNode becs) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		// Convert lists to arrays for passing to JNI
		int i;//, n = targets.size();
		long targetsArr[] = new long[targets.size()];
		int relOpsProbArr[] = new int[opsAndBounds.probSize()];
		double boundsProbArr[] = new double[opsAndBounds.probSize()];
		int relOpsRewardArr[] = new int[opsAndBounds.rewardSize()];
		double boundsRewardArr[] = new double[opsAndBounds.rewardSize()];
		long trrArr[] = new long[trr.size()];
		long becsArr = becs.ptr();
		for (i = 0; i < targets.size(); i++) 
			targetsArr[i] = targets.get(i).ptr();
		for (i = 0; i < opsAndBounds.probSize(); i++) {
			relOpsProbArr[i] = opsAndBounds.getProbOperator(i).toNumber();
			boundsProbArr[i] = opsAndBounds.getProbBound(i);
		}
		for (i = 0; i < opsAndBounds.rewardSize(); i++) {
			relOpsRewardArr[i] = opsAndBounds.getRewardOperator(i).toNumber(); 
			boundsRewardArr[i] = opsAndBounds.getRewardBound(i);
		}
		for (i = 0; i < trr.size(); i++) 
			trrArr[i] = trr.get(i).ptr();
		long combinationsArr[] = new long[combinations.size()];
		int combinationIDsArr[] = new int[combinationIDs.size()];
		for(i=0; i<combinations.size(); i++) {
			combinationsArr[i] = combinations.get(i).ptr();
		}
		for(i=0; i<combinationIDs.size(); i++)
			combinationIDsArr[i] = combinationIDs.get(i);
		
		double res = //relOps.get(0).intValue()>2 ? 
				PS_NondetMultiReachReward1(trans.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, combinationsArr, combinationIDsArr, relOpsProbArr, boundsProbArr, relOpsRewardArr, boundsRewardArr, maybe.ptr(), start.ptr(),
				trrArr, becsArr); //:
					//PS_NondetMultiReach(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), targetsArr, relOpsArr, boundsArr, maybe.ptr(), start.ptr(), (checkReach? 1 : 0),
							//0, 0);
		if (res == -1) throw new PrismException(getErrorMessage());
		return res;
	}
		
	//----------------------------------------------------------------------------------------------
	// stochastic/ctmc stuff
	//----------------------------------------------------------------------------------------------

	// csl time bounded until (stochastic/ctmc)
	private static native long PS_StochBoundedUntil(long trans, long odd, long rv, int nrv, long cv, int ncv, long yes, long maybe, double time, long mult);
	public static DoubleVector StochBoundedUntil(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDNode yes, JDDNode maybe, double time, DoubleVector multProbs) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long mult = (multProbs == null) ? 0 : multProbs.getPtr();
		long ptr = PS_StochBoundedUntil(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), yes.ptr(), maybe.ptr(), time, mult);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// csl cumulative reward (stochastic/ctmc)
	private static native long PS_StochCumulReward(long trans, long sr, long trr, long odd, long rv, int nrv, long cv, int ncv, double time);
	public static DoubleVector StochCumulReward(JDDNode trans, JDDNode sr, JDDNode trr, ODDNode odd, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_StochCumulReward(trans.ptr(), sr.ptr(), trr.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// steady state (stochastic/ctmc)
	private static native long PS_StochSteadyState(long trans, long odd, long init, long rv, int nrv, long cv, int ncv);
	public static DoubleVector StochSteadyState(JDDNode trans, ODDNode odd, JDDNode init, JDDVars rows, JDDVars cols) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_StochSteadyState(trans.ptr(), odd.ptr(), init.ptr(), rows.array(), rows.n(), cols.array(), cols.n());
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}
	
	// transient (stochastic/ctmc)
	private static native long PS_StochTransient(long trans, long odd, long init, long rv, int nrv, long cv, int ncv, double time);
	public static DoubleVector StochTransient(JDDNode trans, ODDNode odd, DoubleVector init, JDDVars rows, JDDVars cols, double time) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_StochTransient(trans.ptr(), odd.ptr(), init.getPtr(), rows.array(), rows.n(), cols.array(), cols.n(), time);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//------------------------------------------------------------------------------
	// export methods
	//------------------------------------------------------------------------------

	// export matrix
	private static native int PS_ExportMatrix(long matrix, String name, long rv, int nrv, long cv, int ncv, long odd, int exportType, String filename);
	public static void ExportMatrix(JDDNode matrix, String name, JDDVars rows, JDDVars cols, ODDNode odd, int exportType, String filename) throws FileNotFoundException, PrismException
	{
		checkNumStates(odd);
		int res = PS_ExportMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
		else if (res == -2) {
			throw new PrismException("Out of memory building matrix for export");
		}
	}
	
	// export mdp
	private static native int PS_ExportMDP(long mdp, long trans_actions, List<String> synchs, String name, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long odd, int exportType, String filename);
	public static void ExportMDP(JDDNode mdp, JDDNode transActions, List<String> synchs, String name, JDDVars rows, JDDVars cols, JDDVars nondet, ODDNode odd, int exportType, String filename) throws FileNotFoundException, PrismException
	{
		checkNumStates(odd);
		int res = PS_ExportMDP(mdp.ptr(), (transActions == null) ? 0 : transActions.ptr(), synchs, name, rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
		else if (res == -2) {
			throw new PrismException("Out of memory building matrix for export");
		}
	}
	
	// export sub-mdp, i.e. mdp transition rewards
	private static native int PS_ExportSubMDP(long mdp, long submdp, String name, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long odd, int exportType, String filename);
	public static void ExportSubMDP(JDDNode mdp, JDDNode submdp, String name, JDDVars rows, JDDVars cols, JDDVars nondet, ODDNode odd, int exportType, String filename) throws FileNotFoundException, PrismException
	{
		checkNumStates(odd);
		int res = PS_ExportSubMDP(mdp.ptr(), submdp.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), odd.ptr(), exportType, filename);
		if (res == -1) {
			throw new FileNotFoundException();
		}
		else if (res == -2) {
			throw new PrismException("Out of memory building matrix for export");
		}
	}

	//------------------------------------------------------------------------------
	// generic iterative solution methods
	//------------------------------------------------------------------------------

	// power method
	private static native long PS_Power(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose);
	public static DoubleVector Power(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_Power(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// power method, interval variant
	private static native long PS_PowerInterval(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long lower, long upper, boolean transpose, int flags);
	public static DoubleVector PowerInterval(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode lower, JDDNode upper, boolean transpose, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_PowerInterval(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), lower.ptr(), upper.ptr(), transpose, flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// jor method
	private static native long PS_JOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega);
	public static DoubleVector JOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_JOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// jor method, interval variant
	private static native long PS_JORInterval(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long lower, long upper, boolean transpose, boolean row_sums, double omega, int flags);
	public static DoubleVector JORInterval(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode lower, JDDNode upper, boolean transpose, boolean row_sums, double omega, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_JORInterval(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), lower.ptr(), upper.ptr(), transpose, row_sums, omega, flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// sor method
	private static native long PS_SOR(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long init, boolean transpose, boolean row_sums, double omega, boolean forwards);
	public static DoubleVector SOR(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode init, boolean transpose, boolean row_sums, double omega, boolean forwards) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_SOR(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), init.ptr(), transpose, row_sums, omega, forwards);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	// sor method, interval variant
	private static native long PS_SORInterval(long odd, long rv, int nrv, long cv, int ncv, long a, long b, long lower, long upper, boolean transpose, boolean row_sums, double omega, boolean forwards, int flags);
	public static DoubleVector SORInterval(ODDNode odd, JDDVars rows, JDDVars cols, JDDNode a, JDDNode b, JDDNode lower, JDDNode upper, boolean transpose, boolean row_sums, double omega, boolean forwards, int flags) throws PrismException
	{
		checkNumStates(odd);
		PrismNative.resetModelCheckingInfo();
		long ptr = PS_SORInterval(odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), a.ptr(), b.ptr(), lower.ptr(), upper.ptr(), transpose, row_sums, omega, forwards, flags);
		if (ptr == 0) throw generateExceptionForError();
		return new DoubleVector(ptr, (int)(odd.getEOff() + odd.getTOff()));
	}

	//----------------------------------------------------------------------------------------------
}

//------------------------------------------------------------------------------
