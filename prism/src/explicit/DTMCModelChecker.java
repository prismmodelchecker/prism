//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package explicit;

import java.io.File;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.Vector;

import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.Expression;
import prism.ModelType;
import prism.OptionsIntervalIteration;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.PrismUtils;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import automata.DA;
import common.IntSet;
import common.StopWatch;
import common.IterableBitSet;
import explicit.LTLModelChecker.LTLProduct;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.MDPFromDTMC;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;

/**
 * Explicit-state model checker for discrete-time Markov chains (DTMCs).
 */
public class DTMCModelChecker extends ProbModelChecker
{
	/**
	 * Create a new DTMCModelChecker, inherit basic state from parent (unless null).
	 */
	public DTMCModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	// Model checking functions

	@Override
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct, probs;
		LTLModelChecker.LTLProduct<DTMC> product;
		DTMCModelChecker mcProduct;

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);

		// Build product of Markov chain and automaton
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.RABIN,
				AcceptanceType.REACH,
				AcceptanceType.BUCHI,
				AcceptanceType.STREETT,
				AcceptanceType.GENERIC
		};
		product = mcLtl.constructProductMC(this, (DTMC)model, expr, statesOfInterest, allowedAcceptance);

		// Output product, if required
		if (getExportProductTrans()) {
				mainLog.println("\nExporting product transition matrix to file \"" + getExportProductTransFilename() + "\"...");
				product.getProductModel().exportToPrismExplicitTra(getExportProductTransFilename());
		}
		if (getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + getExportProductStatesFilename() + "\"...");
			PrismFileLog out = new PrismFileLog(getExportProductStatesFilename());
			VarList newVarList = (VarList) modulesFile.createVarList().clone();
			String daVar = "_da";
			while (newVarList.getIndex(daVar) != -1) {
				daVar = "_" + daVar;
			}
			newVarList.addVar(0, new Declaration(daVar, new DeclarationIntUnbounded()), 1, null);
			product.getProductModel().exportStates(Prism.EXPORT_PLAIN, newVarList, out);
			out.close();
		}
		
		// Find accepting states + compute reachability probabilities
		BitSet acc;
		if (product.getAcceptance() instanceof AcceptanceReach) {
			mainLog.println("\nSkipping BSCC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		} else {
			mainLog.println("\nFinding accepting BSCCs...");
			acc = mcLtl.findAcceptingBSCCs(product.getProductModel(), product.getAcceptance());
		}
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new DTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachProbs(product.getProductModel(), acc); 
		probsProduct = StateValues.createFromDoubleArray(res.soln, product.getProductModel());

		// Output vector over product, if required
		if (getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(getExportProductVectorFilename());
				probsProduct.print(out, false, false, false, false);
				out.close();
		}
		
		// Mapping probabilities in the original model
		probs = product.projectToOriginalModel(probsProduct);
		probsProduct.clear();

		return probs;
	}

	/**
	 * Compute rewards for a co-safe LTL reward operator.
	 */
	protected StateValues checkRewardCoSafeLTL(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		MCRewards productRewards;
		StateValues rewardsProduct, rewards;
		DTMCModelChecker mcProduct;
		LTLProduct<DTMC> product;

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);

		// Model check maximal state formulas and construct DFA, with the special
		// handling needed for cosafety reward translation
		Vector<BitSet> labelBS = new Vector<BitSet>();
		DA<BitSet, AcceptanceReach> da = mcLtl.constructDFAForCosafetyRewardLTL(this, model, expr, labelBS);

		StopWatch timer = new StopWatch(mainLog);
		mainLog.println("\nConstructing " + model.getModelType() + "-" + da.getAutomataType() + " product...");
		timer.start(model.getModelType() + "-" + da.getAutomataType() + " product");
		product = mcLtl.constructProductModel(da, (DTMC)model, labelBS, statesOfInterest);
		timer.stop("product has " + product.getProductModel().infoString());

		// Adapt reward info to product model
		productRewards = ((MCRewards) modelRewards).liftFromModel(product);

		// Output product, if required
		if (getExportProductTrans()) {
				mainLog.println("\nExporting product transition matrix to file \"" + getExportProductTransFilename() + "\"...");
				product.getProductModel().exportToPrismExplicitTra(getExportProductTransFilename());
		}
		if (getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + getExportProductStatesFilename() + "\"...");
			PrismFileLog out = new PrismFileLog(getExportProductStatesFilename());
			VarList newVarList = (VarList) modulesFile.createVarList().clone();
			String daVar = "_da";
			while (newVarList.getIndex(daVar) != -1) {
				daVar = "_" + daVar;
			}
			newVarList.addVar(0, new Declaration(daVar, new DeclarationIntUnbounded()), 1, null);
			product.getProductModel().exportStates(Prism.EXPORT_PLAIN, newVarList, out);
			out.close();
		}

		// Find accepting states + compute reachability rewards
		BitSet acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();

		mainLog.println("\nComputing reachability rewards...");
		mcProduct = new DTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachRewards((DTMC)product.getProductModel(), productRewards, acc);
		rewardsProduct = StateValues.createFromDoubleArray(res.soln, product.getProductModel());

		// Output vector over product, if required
		if (getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(getExportProductVectorFilename());
				rewardsProduct.print(out, false, false, false, false);
				out.close();
		}

		// Mapping rewards in the original model
		rewards = product.projectToOriginalModel(rewardsProduct);
		rewardsProduct.clear();

		return rewards;
	}
	
	public ModelCheckerResult computeInstantaneousRewards(DTMC dtmc, MCRewards mcRewards, int k, BitSet statesOfInterest) throws PrismException
	{
		if (statesOfInterest.cardinality() == 1) {
			return computeInstantaneousRewardsForwards(dtmc, mcRewards, k, statesOfInterest.nextSetBit(0));
		} else {
			return computeInstantaneousRewardsBackwards(dtmc, mcRewards, k);
		}
	}
	
	public ModelCheckerResult computeInstantaneousRewardsBackwards(DTMC dtmc, MCRewards mcRewards, int k) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Store num states
		n = dtmc.getNumStates();

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards instantaneous rewards computation...");

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = mcRewards.getStateReward(i);

		// Start iterations
		for (iters = 0; iters < k; iters++) {
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, null, false);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards transient instantaneous rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	public ModelCheckerResult computeInstantaneousRewardsForwards(DTMC dtmc, MCRewards mcRewards, int k, int stateOfInterest) throws PrismException
	{
		// Build a point probability distribution for the required state  
		double[] initDist = new double[dtmc.getNumStates()];
		initDist[stateOfInterest] = 1.0;
		
		// Compute (forward) transient probabilities
		ModelCheckerResult res = computeTransientProbs(dtmc, k, initDist);
		
		// Compute expected value (from initial state)
		int n = dtmc.getNumStates();
		double avg = 0.0;
		for (int i = 0; i < n; i++) {
			avg += res.soln[i] *= mcRewards.getStateReward(i);
		}

		// Reuse vector/result storage
		for (int i = 0; i < n; i++) {
			res.soln[i] = 0.0;
		}
		res.soln[stateOfInterest] = avg;
		
		return res;
	}
	
	public ModelCheckerResult computeCumulativeRewards(DTMC dtmc, MCRewards mcRewards, double t) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;
		int right = (int) t;

		// Store num states
		n = dtmc.getNumStates();

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards cumulative rewards computation...");

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Start iterations
		for (iters = 0; iters < right; iters++) {
			// Matrix-vector multiply plus adding rewards
			dtmc.mvMult(soln, soln2, null, false);
			for (i = 0; i < n; i++) {
				soln2[i] += mcRewards.getStateReward(i);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards cumulative rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	public ModelCheckerResult computeTotalRewards(DTMC dtmc, MCRewards mcRewards) throws PrismException
	{
		ModelCheckerResult res = null;
		int n, numBSCCs = 0;
		long timer;

		if (getDoIntervalIteration()) {
			throw new PrismNotSupportedException("Interval iteration for total rewards is currently not supported");
		}

		// Switch to a supported method, if necessary
		if (!(linEqMethod == LinEqMethod.POWER)) {
			linEqMethod = LinEqMethod.POWER;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		// Store num states
		n = dtmc.getNumStates();

		// Start total rewards computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting total reward computation...");

		// Compute bottom strongly connected components (BSCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		numBSCCs = bsccs.size();

		// Find BSCCs with non-zero reward
		BitSet bsccsNonZero = new BitSet();
		for (int b = 0; b < numBSCCs; b++) {
			BitSet bscc = bsccs.get(b);
			for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
				if (mcRewards.getStateReward(i) > 0) {
					bsccsNonZero.or(bscc);
					break;
				}
			}
		}
		mainLog.print("States in non-zero reward BSCCs: " + bsccsNonZero.cardinality() + "\n");

		// Find states with infinite reward (those reach a non-zero reward BSCC with prob > 0)
		BitSet inf;
		if (preRel) {
			// prob0 using predecessor relation
			PredecessorRelation pre = dtmc.getPredecessorRelation(this, true);
			inf = prob0(dtmc, null, bsccsNonZero, pre);
		} else {
			// prob0 using fixed point algorithm
			inf = prob0(dtmc, null, bsccsNonZero);
		}
		inf.flip(0, n);
		int numInf = inf.cardinality();
		mainLog.println("inf=" + numInf + ", maybe=" + (n - numInf));
		
		// Compute rewards
		// (do this using the functions for "reward reachability" properties but with no targets)
		switch (linEqMethod) {
		case POWER:
			res = computeReachRewardsValIter(dtmc, mcRewards, new BitSet(), inf, null, null);
			break;
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
		}

		// Finished total reward computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Total reward computation");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		// Return results
		return res;
	}

	// Steady-state/transient probability computation

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doSteadyState(DTMC dtmc) throws PrismException
	{
		return doSteadyState(dtmc, (StateValues) null);
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doSteadyState(DTMC dtmc, File initDistFile) throws PrismException
	{
		StateValues initDist = readDistributionFromFile(initDistFile, dtmc);
		return doSteadyState(dtmc, initDist);
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over,
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues doSteadyState(DTMC dtmc, StateValues initDist) throws PrismException
	{
		StateValues initDistNew = (initDist == null) ? buildInitialDistribution(dtmc) : initDist;
		ModelCheckerResult res = computeSteadyStateProbs(dtmc, initDistNew.getDoubleArray());
		return StateValues.createFromDoubleArray(res.soln, dtmc);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(DTMC dtmc, int k) throws PrismException
	{
		return doTransient(dtmc, k, (StateValues) null);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * @param dtmc The DTMC
	 * @param k Time step
	 * @param initDistFile File containing initial distribution
	 */
	public StateValues doTransient(DTMC dtmc, int k, File initDistFile) throws PrismException
	{
		StateValues initDist = readDistributionFromFile(initDistFile, dtmc);
		return doTransient(dtmc, k, initDist);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time step 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over,
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param k Time step
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues doTransient(DTMC dtmc, int k, StateValues initDist) throws PrismException
	{
		StateValues initDistNew = (initDist == null) ? buildInitialDistribution(dtmc) : initDist;
		ModelCheckerResult res = computeTransientProbs(dtmc, k, initDistNew.getDoubleArray());
		return StateValues.createFromDoubleArray(res.soln, dtmc);
	}

	// Numerical computation functions

	/**
	 * Compute next=state probabilities.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param dtmc The DTMC
	 * @param target Target states
	 */
	public ModelCheckerResult computeNextProbs(DTMC dtmc, BitSet target) throws PrismException
	{
		ModelCheckerResult res = null;
		int n;
		double soln[], soln2[];
		long timer;

		timer = System.currentTimeMillis();

		// Store num states
		n = dtmc.getNumStates();

		// Create/initialise solution vector(s)
		soln = Utils.bitsetToDoubleArray(target, n);
		soln2 = new double[n];

		// Next-step probabilities 
		dtmc.mvMult(soln, soln2, null, false);

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln2;
		res.numIters = 1;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Given a value vector x, compute the probability:
	 *   v(s) = Sum_s' P(s,s')*x(s')   for s labeled with a,
	 *   v(s) = 0                      for s not labeled with a.
	 *
	 * @param dtmc the DTMC model
	 * @param a the set of states labeled with a
	 * @param x the value vector
	 */
	protected double[] computeRestrictedNext(DTMC dtmc, BitSet a, double[] x)
	{
		double[] soln;
		int n;

		// Store num states
		n = dtmc.getNumStates();

		// initialized to 0.0
		soln = new double[n];

		// Next-step probabilities multiplication
		// restricted to a states
		dtmc.mvMult(x, soln, a, false);

		return soln;
	}

	/**
	 * Compute reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param dtmc The DTMC
	 * @param target Target states
	 */
	public ModelCheckerResult computeReachProbs(DTMC dtmc, BitSet target) throws PrismException
	{
		return computeReachProbs(dtmc, null, target, null, null);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 */
	public ModelCheckerResult computeUntilProbs(DTMC dtmc, BitSet remain, BitSet target) throws PrismException
	{
		return computeReachProbs(dtmc, remain, target, null, null);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachProbs(DTMC dtmc, BitSet remain, BitSet target, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int n, numYes, numNo;
		long timer, timerProb0, timerProb1;
		PredecessorRelation pre = null;
		// Local copy of setting
		LinEqMethod linEqMethod = this.linEqMethod;

		// Switch to a supported method, if necessary
		switch (linEqMethod)
		{
		case POWER:
		case GAUSS_SEIDEL:
		case BACKWARDS_GAUSS_SEIDEL:
		case JACOBI:
			break; // supported
		default:
			linEqMethod = LinEqMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		if (doIntervalIteration && (!precomp || !prob0 || !prob1)) {
			throw new PrismNotSupportedException("Interval iteration requires precomputations to be active");
		}

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting probabilistic reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()) {
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
				}
			}
			target = targetNew;
		}

		// If required, export info about target states
		if (getExportTarget()) {
			BitSet bsInit = new BitSet(n);
			for (int i = 0; i < n; i++) {
				bsInit.set(i, dtmc.isInitialState(i));
			}
			List<BitSet> labels = Arrays.asList(bsInit, target);
			List<String> labelNames = Arrays.asList("init", "target");
			mainLog.println("\nExporting target states info to file \"" + getExportTargetFilename() + "\"...");
			exportLabels(dtmc, labels, labelNames, Prism.EXPORT_PLAIN, new PrismFileLog(getExportTargetFilename()));
		}

		if (precomp && (prob0 || prob1) && preRel) {
			pre = dtmc.getPredecessorRelation(this, true);
		}

		// Precomputation
		timerProb0 = System.currentTimeMillis();
		if (precomp && prob0) {
			if (preRel) {
				no = prob0(dtmc, remain, target, pre);
			} else {
				no = prob0(dtmc, remain, target);
			}
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			if (preRel) {
				yes = prob1(dtmc, remain, target, pre);
			} else {
				yes = prob1(dtmc, remain, target);
			}
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		boolean termCritAbsolute = termCrit == TermCrit.ABSOLUTE;

		// Compute probabilities
		IterationMethod iterationMethod = null;

		switch (linEqMethod) {
		case POWER:
			iterationMethod = new IterationMethodPower(termCritAbsolute, termCritParam);
			break;
		case JACOBI:
			iterationMethod = new IterationMethodJacobi(termCritAbsolute, termCritParam);
			break;
		case GAUSS_SEIDEL:
		case BACKWARDS_GAUSS_SEIDEL: {
			boolean backwards = linEqMethod == LinEqMethod.BACKWARDS_GAUSS_SEIDEL;
			iterationMethod = new IterationMethodGS(termCritAbsolute, termCritParam, backwards);
			break;
		}
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
		}

		if (doIntervalIteration) {
			res = doIntervalIterationReachProbs(dtmc, no, yes, init, known, iterationMethod, getDoTopologicalValueIteration());
		} else {
			res = doValueIterationReachProbs(dtmc, no, yes, init, known, iterationMethod, getDoTopologicalValueIteration());
		}

		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timeProb0 = timerProb0 / 1000.0;
		res.timePre = (timerProb0 + timerProb1) / 1000.0;

		return res;
	}


	/**
	 * Prob0 precomputation algorithm (using predecessor relation),
	 * i.e. determine the states of a DTMC which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all states")
	 * @param target Target states
	 * @param pre The predecessor relation
	 */
	public BitSet prob0(DTMC dtmc, BitSet remain, BitSet target, PredecessorRelation pre)
	{
		BitSet canReachTarget, result;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		if (!silentPrecomputations)
			mainLog.println("Starting Prob0...");

		// Special case: no target states
		if (target.isEmpty()) {
			BitSet soln = new BitSet(dtmc.getNumStates());
			soln.set(0, dtmc.getNumStates());
			return soln;
		}

		// calculate all states that can reach 'target'
		// while remaining in 'remain' in the underlying graph,
		// where all the 'target' states are made absorbing
		canReachTarget = pre.calculatePreStar(remain, target, target);

		// prob0 = complement of 'canReachTarget'
		result = new BitSet();
		result.set(0, dtmc.getNumStates(), true);
		result.andNot(canReachTarget);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		if (!silentPrecomputations) {
			mainLog.print("Prob0");
			mainLog.println(" took " + timer / 1000.0 + " seconds.");
		}

		return result;
	}

	/**
	 * Prob0 precomputation algorithm (using a fixed-point computation),
	 * i.e. determine the states of a DTMC which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all")
	 * @param target Target states
	 */
	public BitSet prob0(DTMC dtmc, BitSet remain, BitSet target)
	{
		int n, iters;
		BitSet u, soln, unknown;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		if (!silentPrecomputations)
			mainLog.println("Starting Prob0...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(dtmc.getNumStates());
			soln.set(0, dtmc.getNumStates());
			return soln;
		}

		// Initialise vectors
		n = dtmc.getNumStates();
		u = new BitSet(n);
		soln = new BitSet(n);

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Fixed point loop
		iters = 0;
		u_done = false;
		// Least fixed point - should start from 0 but we optimise by
		// starting from 'target', thus bypassing first iteration
		u.or(target);
		soln.or(target);
		while (!u_done) {
			iters++;
			// Single step of Prob0
			dtmc.prob0step(unknown, u, soln);
			// Check termination
			u_done = soln.equals(u);
			// u = soln
			u.clear();
			u.or(soln);
		}

		// Negate
		u.flip(0, n);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		if (!silentPrecomputations) {
			mainLog.print("Prob0");
			mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");
		}

		return u;
	}

	/**
	 * Prob1 precomputation algorithm (using predecessor relation),
	 * i.e. determine the states of a DTMC which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param pre The predecessor relation of the DTMC
	 */
	public BitSet prob1(DTMC dtmc, BitSet remain, BitSet target, PredecessorRelation pre) {
		// Implements the constrained reachability algorithm from
		// Baier, Katoen: Principles of Model Checking (Corollary 10.31 Qualitative Constrained Reachability)
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		if (!silentPrecomputations)
			mainLog.println("Starting Prob1...");

		// Special case: no 'target' states
		if (target.isEmpty()) {
			// empty set
			return new BitSet();
		}

		// mark all states in 'target' and all states not in 'remain' as absorbing
		BitSet absorbing = new BitSet();
		if (remain != null) {
			// complement remain
			absorbing.set(0, dtmc.getNumStates(), true);
			absorbing.andNot(remain);
		} else {
			// for remain == null, remain consists of all states
			// thus, absorbing = the empty set is already the complementation of remain
		}
		// union with 'target'
		absorbing.or(target);

		// M' = DTMC where all 'absorbing' states are considered to be absorbing

		// the set of states that satisfy E [ F target ] in M'
		// Pre*(target)
		BitSet canReachTarget = pre.calculatePreStar(null, target, absorbing);

		// complement canReachTarget
		// S\Pre*(target)
		BitSet canNotReachTarget = new BitSet();
		canNotReachTarget.set(0, dtmc.getNumStates(), true);
		canNotReachTarget.andNot(canReachTarget);

		// the set of states that can reach a canNotReachTarget state in M'
		// Pre*(S\Pre*(target))
		BitSet probTargetNot1 = pre.calculatePreStar(null, canNotReachTarget, absorbing);

		// complement probTargetNot1
		// S\Pre*(S\Pre*(target))
		BitSet result = new BitSet();
		result.set(0, dtmc.getNumStates(), true);
		result.andNot(probTargetNot1);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		if (!silentPrecomputations) {
			mainLog.print("Prob1");
			mainLog.println(" took " + timer / 1000.0 + " seconds.");
		}

		return result;
	}

	/**
	 * Prob1 precomputation algorithm (using a fixed-point computation)
	 * i.e. determine the states of a DTMC which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all")
	 * @param target Target states
	 */
	public BitSet prob1(DTMC dtmc, BitSet remain, BitSet target)
	{
		int n, iters;
		BitSet u, v, soln, unknown;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		if (!silentPrecomputations)
			mainLog.println("Starting Prob1...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(dtmc.getNumStates());
		}

		// Initialise vectors
		n = dtmc.getNumStates();
		u = new BitSet(n);
		v = new BitSet(n);
		soln = new BitSet(n);

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Nested fixed point loop
		iters = 0;
		u_done = false;
		// Greatest fixed point
		u.set(0, n);
		while (!u_done) {
			v_done = false;
			// Least fixed point - should start from 0 but we optimise by
			// starting from 'target', thus bypassing first iteration
			v.clear();
			v.or(target);
			soln.clear();
			soln.or(target);
			while (!v_done) {
				iters++;
				// Single step of Prob1
				dtmc.prob1step(unknown, u, v, soln);
				// Check termination (inner)
				v_done = soln.equals(v);
				// v = soln
				v.clear();
				v.or(soln);
			}
			// Check termination (outer)
			u_done = v.equals(u);
			// u = v
			u.clear();
			u.or(v);
		}

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		if (!silentPrecomputations) {
			mainLog.print("Prob1");
			mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");
		}

		return u;
	}

	/**
	 * Compute reachability probabilities using value iteration.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null), 'init' must also be given and is used for the exact values.
	 * @param topological do topological value iteration?
	 */
	protected ModelCheckerResult doValueIterationReachProbs(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known, IterationMethod iterationMethod, boolean topological) throws PrismException
	{
		BitSet unknown;
		int i, n;
		double initVal;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (topological ? "topological, " : "" ) + "with " + iterationMethod.getDescriptionShort();
		mainLog.println("Starting value iteration (" + description + ")...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit DTMC ReachProbs value iteration (" + description + ")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = dtmc.getNumStates();

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) passed in initial value; (4) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above. 
		initVal = (valIterDir == ValIterDir.BELOW) ? 0.0 : 1.0;
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					init[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			} else {
				for (i = 0; i < n; i++)
					init[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			}
		} else {
			init = new double[n];
			for (i = 0; i < n; i++)
				init[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : initVal;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);

		IterationMethod.IterationValIter iterationReachProbs = iterationMethod.forMvMult(dtmc);
		iterationReachProbs.init(init);

		if (iterationsExport != null)
			iterationsExport.exportVector(init, 0);

		IntSet unknownStates = IntSet.asIntSet(unknown);

		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, dtmc, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = dtmc.mvMultJacSingle(s, soln);
			};

			// run the actual value iteration
			return iterationMethod.doTopologicalValueIteration(this, description, sccs, iterationReachProbs, singletonSCCSolver, timer, iterationsExport);
		} else {
			// run the actual value iteration
			return iterationMethod.doValueIteration(this, description, iterationReachProbs, unknownStates, timer, iterationsExport);
		}
	}


	/**
	 * Compute reachability probabilities using value iteration.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null), 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachProbsValIter(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known) throws PrismException
	{
		IterationMethodPower iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
		return doValueIterationReachProbs(dtmc, no, yes, init, known, iterationMethod, false);
	}

	/**
	 * Compute reachability probabilities using Gauss-Seidel (forward).
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null), 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachProbsGaussSeidel(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known) throws PrismException
	{
		return computeReachProbsGaussSeidel(dtmc, no, yes, init, known, false);
	}

	/**
	 * Compute reachability probabilities using Gauss-Seidel.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 * @param backwards do backward Gauss-Seidel?
	 */
	protected ModelCheckerResult computeReachProbsGaussSeidel(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known, boolean backwards) throws PrismException
	{
		IterationMethodGS iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, backwards);
		return doValueIterationReachProbs(dtmc, no, yes, init, known, iterationMethod, false);
	}

	/**
	 * Compute reachability probabilities using power method (interval variant).
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten), will be ignored if known == null
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 * @param topological do topological interval iteration?
	 */
	protected ModelCheckerResult doIntervalIterationReachProbs(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known, IterationMethod iterationMethod, boolean topological) throws PrismException
	{
		BitSet unknown;
		int i, n;
		double initBelow[], initAbove[];
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (topological ? "topological, " : "" ) + "with " + iterationMethod.getDescriptionShort();
		mainLog.println("Starting interval iteration (" + description + ")...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit DTMC ReachProbs interval iteration  (" + description + ")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		initBelow = (init == null) ? new double[n] : init;
		initAbove = new double[n];

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above.
		if (known != null && init != null) {
			for (i = 0; i < n; i++) {
				initBelow[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : 0.0;
				initAbove[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : 1.0;
			}
		} else {
			for (i = 0; i < n; i++) {
				initBelow[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 :  0.0;
				initAbove[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 :  1.0;
			}
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);

		if (iterationsExport != null) {
			iterationsExport.exportVector(initBelow, 0);
			iterationsExport.exportVector(initAbove, 1);
		}

		IntSet unknownStates = IntSet.asIntSet(unknown);

		OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(this);

		final boolean enforceMonotonicFromBelow = iiOptions.isEnforceMonotonicityFromBelow();
		final boolean enforceMonotonicFromAbove = iiOptions.isEnforceMonotonicityFromAbove();
		final boolean checkMonotonic = iiOptions.isCheckMonotonicity();

		if (!enforceMonotonicFromAbove) {
			getLog().println("Note: Interval iteration is configured to not enforce monotonicity from above.");
		}
		if (!enforceMonotonicFromBelow) {
			getLog().println("Note: Interval iteration is configured to not enforce monotonicity from below.");
		}

		IterationMethod.IterationIntervalIter below = iterationMethod.forMvMultInterval(dtmc, true, enforceMonotonicFromBelow, checkMonotonic);
		IterationMethod.IterationIntervalIter above = iterationMethod.forMvMultInterval(dtmc, false, enforceMonotonicFromAbove, checkMonotonic);

		below.init(initBelow);
		above.init(initAbove);

		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, dtmc, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = dtmc.mvMultJacSingle(s, soln);
			};

			// run the actual value iteration
			return iterationMethod.doTopologicalIntervalIteration(this, description, sccs, below, above, singletonSCCSolver, timer, iterationsExport);
		} else {
			// run the actual value iteration
			return iterationMethod.doIntervalIteration(this, description, below, above, unknownStates, timer, iterationsExport);
		}

	}



	/**
	 * Compute bounded reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target} within k steps.
	 * @param dtmc The DTMC
	 * @param target Target states
	 * @param k Bound
	 */
	public ModelCheckerResult computeBoundedReachProbs(DTMC dtmc, BitSet target, int k) throws PrismException
	{
		return computeBoundedReachProbs(dtmc, null, target, k, null, null);
	}

	/**
	 * Compute bounded until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 */
	public ModelCheckerResult computeBoundedUntilProbs(DTMC dtmc, BitSet remain, BitSet target, int k) throws PrismException
	{
		return computeBoundedReachProbs(dtmc, remain, target, k, null, null);
	}

	/**
	 * Compute bounded reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 * @param init Initial solution vector - pass null for default
	 * @param results Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult computeBoundedReachProbs(DTMC dtmc, BitSet remain, BitSet target, int k, double init[], double results[]) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting bounded probabilistic reachability...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use passed in initial vector, if present
		if (init != null) {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : init[i];
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;
		}
		// Store intermediate results if required
		// (compute min/max value over initial states for first step)
		if (results != null) {
			// TODO: whether this is min or max should be specified somehow
			results[0] = Utils.minMaxOverArraySubset(soln2, dtmc.getInitialStates(), true);
		}

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Start iterations
		iters = 0;
		while (iters < k) {

			iters++;
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, unknown, false);
			// Store intermediate results if required
			// (compute min/max value over initial states for this step)
			if (results != null) {
				// TODO: whether this is min or max should be specified somehow
				results[iters] = Utils.minMaxOverArraySubset(soln2, dtmc.getInitialStates(), true);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Bounded probabilistic reachability");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Compute upper bound for maximum expected reward, using the variant specified in the settings.
	 * @param dtmc the model
	 * @param mcRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @param inf the infinity states
	 * @return upper bound on R=?[ F target ] for all states
	 */
	double computeReachRewardsUpperBound(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		if (unknown.isEmpty()) {
			mainLog.println("Skipping upper bound computation, no unknown states...");
			return 0;
		}

		// inf and target states become trap states (with self-loops)
		BitSet trapStates = (BitSet) target.clone();
		trapStates.or(inf);
		DTMCAlteredDistributions cleanedDTMC = DTMCAlteredDistributions.addSelfLoops(dtmc, trapStates);

		OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(this);

		double upperBound = 0.0;
		String method = null;
		switch (iiOptions.getBoundMethod()) {
		case VARIANT_1_COARSE:
			upperBound = computeReachRewardsUpperBoundVariant1Coarse(cleanedDTMC, mcRewards, target, unknown, inf);
			method = "variant 1, coarse";
			break;
		case VARIANT_1_FINE:
			upperBound = computeReachRewardsUpperBoundVariant1Fine(cleanedDTMC, mcRewards, target, unknown, inf);
			method = "variant 1, fine";
			break;
		case VARIANT_2:
			upperBound = computeReachRewardsUpperBoundVariant2(cleanedDTMC, mcRewards, target, unknown, inf);
			method = "variant 2";
			break;
		case DEFAULT:
		case DSMPI:
		{
			MDP mdp = new MDPFromDTMC(cleanedDTMC);
			MDPRewards mdpRewards = new MDPRewards() {

				@Override
				public double getStateReward(int s)
				{
					return mcRewards.getStateReward(s);
				}

				@Override
				public double getTransitionReward(int s, int i)
				{
					return 0;
				}

				@Override
				public MDPRewards liftFromModel(Product<? extends Model> product)
				{
					throw new RuntimeException("Unsupported");
				}

				@Override
				public boolean hasTransitionRewards()
				{
					return false;
				}
			};
			upperBound = DijkstraSweepMPI.computeUpperBound(this, mdp, mdpRewards, target, unknown);
			method = "Dijkstra Sweep MPI";
			break;
		}
		}

		if (method == null) {
			throw new PrismException("Unknown upper bound heuristic");
		}

		mainLog.println("Upper bound for expectation (" + method + "): " + upperBound);

		return upperBound;
	}

	/**
	 * Compute upper bound for maximum expected reward (variant 1, coarse),
	 * i.e., does not compute separate q_t / p_t per SCC.
	 * Uses Rs = S, i.e., does not take reachability into account.
	 * @param dtmc the model
	 * @param mcRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @param inf the infinity states
	 * @return upper bound on R=?[ F target ] for all states
	 */
	double computeReachRewardsUpperBoundVariant1Coarse(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		double[] boundsOnExpectedVisits = new double[dtmc.getNumStates()];
		int[] Ct = new int[dtmc.getNumStates()];

		StopWatch timer = new StopWatch(getLog());
		timer.start("computing an upper bound for expected reward");

		SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, dtmc, true, null);
		BitSet trivial = new BitSet();

		double q = 0;
		for (int scc = 0, numSCCs = sccs.getNumSCCs(); scc < numSCCs; scc++) {
			IntSet statesForSCC = sccs.getStatesForSCC(scc);

			int cardinality = statesForSCC.cardinality();

			PrimitiveIterator.OfInt itSCC = statesForSCC.iterator();
			while (itSCC.hasNext()) {
				int s = itSCC.nextInt();
				Ct[s] = cardinality;

				if (target.get(s) || inf.get(s)) {
					// trap states
					assert(cardinality == 1);
					break;  // continue with next SCC
				}

				double probRemain = 0;
				boolean allRemain = true;  // all successors remain in the SCC?
				boolean hasSelfloop = false;
				for (Iterator<Entry<Integer, Double>> it = dtmc.getTransitionsIterator(s); it.hasNext(); ) {
					Entry<Integer, Double> t = it.next();
					if (statesForSCC.get(t.getKey())) {
						probRemain += t.getValue();
						hasSelfloop = true;
					} else {
						allRemain = false;
					}
				}

				if (!allRemain) { // action in the set X
					q = Math.max(q, probRemain);
				}

				if (cardinality == 1 && !hasSelfloop) {
					trivial.set(s);
				}
			}
		}

		double p = 1;
		for (int s = 0; s < dtmc.getNumStates(); s++) {
			for (Iterator<Entry<Integer, Double>> it = dtmc.getTransitionsIterator(s); it.hasNext(); ) {
				Entry<Integer, Double> t = it.next();
				p = Math.min(p, t.getValue());
			}
		}

		double upperBound = 0;
		for (int s = 0; s < dtmc.getNumStates(); s++) {
			if (target.get(s) || inf.get(s)) {
				// inf or target states: not relevant, set visits to 0, ignore in summation
				boundsOnExpectedVisits[s] = 0.0;
			} else if (unknown.get(s)) {
				if (trivial.get(s)) {
					// s is a trivial SCC: seen at most once
					boundsOnExpectedVisits[s] = 1.0;
				} else {
					boundsOnExpectedVisits[s] = 1 / (Math.pow(p, Ct[s]-1) * (1.0-q));
				}

				upperBound += boundsOnExpectedVisits[s] * mcRewards.getStateReward(s);
			}
		}

		timer.stop();

		if (OptionsIntervalIteration.from(this).isBoundComputationVerbose()) {
			mainLog.println("Upper bound for max expectation computation (variant 1, coarse):");
			mainLog.println("p = " + p);
			mainLog.println("q = " + q);
			mainLog.println("|Ct| = " + Arrays.toString(Ct));
			mainLog.println("ζ* = " + Arrays.toString(boundsOnExpectedVisits));
		}

		if (!Double.isFinite(upperBound)) {
			throw new PrismException("Problem computing an upper bound for the expectation, did not get finite result");
		}

		return upperBound;
	}

	/**
	 * Compute upper bound for maximum expected reward (variant 1, fine).
	 * i.e., does compute separate q_t / p_t per SCC.
	 * Uses Rs = S, i.e., does not take reachability into account.
	 * @param dtmc the model
	 * @param mcRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @param inf the infinity states
	 * @return upper bound on R=?[ F target ] for all states
	 */
	double computeReachRewardsUpperBoundVariant1Fine(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		double[] boundsOnExpectedVisits = new double[dtmc.getNumStates()];
		double[] qt = new double[dtmc.getNumStates()];
		double[] pt = new double[dtmc.getNumStates()];
		int[] Ct = new int[dtmc.getNumStates()];

		StopWatch timer = new StopWatch(getLog());
		timer.start("computing an upper bound for expected reward");

		SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, dtmc, true, null);
		BitSet trivial = new BitSet();

		for (int scc = 0, numSCCs = sccs.getNumSCCs(); scc < numSCCs; scc++) {
			IntSet statesForSCC = sccs.getStatesForSCC(scc);

			double q = 0;
			double p = 1;
			int cardinality = statesForSCC.cardinality();

			PrimitiveIterator.OfInt itSCC = statesForSCC.iterator();
			while (itSCC.hasNext()) {
				int s = itSCC.nextInt();
				Ct[s] = cardinality;

				double probRemain = 0;
				boolean allRemain = true;  // all successors remain in the SCC?
				boolean hasSelfloop = false;
				for (Iterator<Entry<Integer, Double>> it = dtmc.getTransitionsIterator(s); it.hasNext(); ) {
					Entry<Integer, Double> t = it.next();
					if (statesForSCC.get(t.getKey())) {
						probRemain += t.getValue();
						p = Math.min(p, t.getValue());
						hasSelfloop = true;
					} else {
						// outgoing edge
						allRemain = false;
					}
				}

				if (!allRemain) { // action in the set Xt
					q = Math.max(q, probRemain);
				}

				if (cardinality == 1 && !hasSelfloop) {
					trivial.set(s);
				}
			}

			for (int s : statesForSCC) {
				qt[s] = q;
				pt[s] = p;
			}
		}

		double upperBound = 0;
		for (int s = 0; s < dtmc.getNumStates(); s++) {
			if (target.get(s) || inf.get(s)) {
				// inf or target states: not relevant, set visits to 0, ignore in summation
				boundsOnExpectedVisits[s] = 0.0;
			} else if (unknown.get(s)) {
				if (trivial.get(s)) {
					// s is a trivial SCC: seen at most once
					boundsOnExpectedVisits[s] = 1.0;
				} else {
					if (pt[s] == 1.0) {
						//throw new PrismException("Upper bound computation had p_t = 1 for state " + s);
					}
					boundsOnExpectedVisits[s] = 1 / (Math.pow(pt[s], Ct[s]-1) * (1.0-qt[s]));
				}

				upperBound += boundsOnExpectedVisits[s] * mcRewards.getStateReward(s);
			} else {
				throw new PrismException("Bogus arguments: inf/target/unknown should partition state space");
			}
		}

		timer.stop();

		if (OptionsIntervalIteration.from(this).isBoundComputationVerbose()) {
			mainLog.println("Upper bound for max expectation computation (variant 1, fine):");
			mainLog.println("pt = " + Arrays.toString(pt));
			mainLog.println("qt = " + Arrays.toString(qt));
			mainLog.println("|Ct| = " + Arrays.toString(Ct));
			mainLog.println("ζ* = " + Arrays.toString(boundsOnExpectedVisits));
		}

		if (!Double.isFinite(upperBound)) {
			throw new PrismException("Problem computing an upper bound for the expectation, did not get finite result");
		}

		return upperBound;
	}


	/**
	 * Compute upper bound for maximum expected reward (variant 2).
	 * Uses Rs = S, i.e., does not take reachability into account.
	 * @param dtmc the model
	 * @param mcRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @param inf the infinity states
	 * @return upper bound on R=?[ F target ] for all states
	 */
	double computeReachRewardsUpperBoundVariant2(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		double[] dt = new double[dtmc.getNumStates()];
		double[] boundsOnExpectedVisits = new double[dtmc.getNumStates()];

		StopWatch timer = new StopWatch(getLog());
		timer.start("computing an upper bound for expected reward");

		SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, dtmc, true, unknown::get);

		BitSet T = (BitSet) target.clone();

		@SuppressWarnings("unused")
		int i = 0;
		while (true) {
			BitSet Si = new BitSet();
			i++;

			// TODO: might be inefficient, worst-case quadratic runtime...
			for (PrimitiveIterator.OfInt it = IterableBitSet.getClearBits(T, dtmc.getNumStates() -1 ).iterator(); it.hasNext(); ) {
				int s = it.nextInt();
//				mainLog.println("Check " + s + " against " + T);
				if (dtmc.someSuccessorsInSet(s, T)) {
					Si.set(s);
				}
			}

			if (Si.isEmpty()) {
				break;
			}

			// mainLog.println("S" + i + " = " + Si);
			// mainLog.println("T = " + T);

			for (PrimitiveIterator.OfInt it = IterableBitSet.getSetBits(Si).iterator(); it.hasNext(); ) {
				final int t = it.nextInt();
				final int sccIndexForT = sccs.getSCCIndex(t);
				double d = dtmc.sumOverTransitions(t, (int __, int u, double prob) -> {
					// mainLog.println("t = " + t + ", u = " + u + ", prob = " + prob);
					if (!T.get(u))
						return 0.0;

					boolean inSameSCC = (sccs.getSCCIndex(u) == sccIndexForT);
					double d_u_t = inSameSCC ? dt[u] : 1.0;
					// mainLog.println("d_u_t = " + d_u_t);
					return d_u_t * prob;
				});
				dt[t] = d;
				// mainLog.println("d["+t+"] = " + d);
			}

			T.or(Si);
		}

		double upperBound = 0;
		for (PrimitiveIterator.OfInt it = IterableBitSet.getSetBits(unknown).iterator(); it.hasNext();) {
			int s = it.nextInt();
			boundsOnExpectedVisits[s] = 1 / dt[s];
			upperBound += boundsOnExpectedVisits[s] * mcRewards.getStateReward(s);
		}

		timer.stop();

		if (OptionsIntervalIteration.from(this).isBoundComputationVerbose()) {
			mainLog.println("Upper bound for max expectation computation (variant 2):");
			mainLog.println("d_t = " + Arrays.toString(dt));
			mainLog.println("ζ* = " + Arrays.toString(boundsOnExpectedVisits));
		}

		if (!Double.isFinite(upperBound)) {
			throw new PrismException("Problem computing an upper bound for the expectation, did not get finite result");
		}

		return upperBound;
	}


	/**
	 * Compute expected reachability rewards.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 */
	public ModelCheckerResult computeReachRewards(DTMC dtmc, MCRewards mcRewards, BitSet target) throws PrismException
	{
		return computeReachRewards(dtmc, mcRewards, target, null, null);
	}

	/**
	 * Compute expected reachability rewards.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param init Optionally, an initial solution vector (may be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachRewards(DTMC dtmc, MCRewards mcRewards, BitSet target, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet inf;
		int n, numTarget, numInf;
		long timer, timerProb1;
		// Local copy of setting
		LinEqMethod linEqMethod = this.linEqMethod;

		// Switch to a supported method, if necessary
		switch (linEqMethod)
		{
		case POWER:
		case GAUSS_SEIDEL:
		case BACKWARDS_GAUSS_SEIDEL:
		case JACOBI:
			break; // supported
		default:
			linEqMethod = LinEqMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()) {
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
				}
			}
			target = targetNew;
		}

		// Precomputation (not optional)
		timerProb1 = System.currentTimeMillis();
		if (preRel) {
			// prob1 via predecessor relation
			PredecessorRelation pre = dtmc.getPredecessorRelation(this, true);
			inf = prob1(dtmc, null, target, pre);
		} else {
			// prob1 via fixed-point algorithm
			inf = prob1(dtmc, null, target);
		}
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		boolean termCritAbsolute = termCrit == TermCrit.ABSOLUTE;

		IterationMethod iterationMethod;

		// Compute rewards
		switch (linEqMethod) {
		case POWER:
			iterationMethod = new IterationMethodPower(termCritAbsolute, termCritParam);
			break;
		case JACOBI:
			iterationMethod = new IterationMethodJacobi(termCritAbsolute, termCritParam);
			break;
		case GAUSS_SEIDEL:
		case BACKWARDS_GAUSS_SEIDEL: {
			boolean backwards = linEqMethod == LinEqMethod.BACKWARDS_GAUSS_SEIDEL;
			iterationMethod = new IterationMethodGS(termCritAbsolute, termCritParam, backwards);
			break;
		}
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
		}

		if (doIntervalIteration) {
			res = doIntervalIterationReachRewards(dtmc, mcRewards, target, inf, init, known, iterationMethod, getDoTopologicalValueIteration());
		} else {
			res = doValueIterationReachRewards(dtmc, mcRewards, target, inf, init, known, iterationMethod, getDoTopologicalValueIteration());
		}

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timePre = timerProb1 / 1000.0;

		return res;
	}

	/**
	 * Compute expected reachability rewards using value iteration.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsValIter(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet inf, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit DTMC ReachRewards value iteration");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) passed in initial value; (4) 0.0
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		if (iterationsExport != null)
			iterationsExport.exportVector(soln, 0);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			//mainLog.println(soln);
			iters++;
			// Matrix-vector multiply
			dtmc.mvMultRew(soln, mcRewards, soln2, unknown, false);

			if (iterationsExport != null)
				iterationsExport.exportVector(soln, 0);

			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		if (iterationsExport != null)
			iterationsExport.close();

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute expected reachability rewards using value iteration.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 * @param topological do topological value iteration?
	 */
	protected ModelCheckerResult doValueIterationReachRewards(DTMC dtmc, final MCRewards mcRewards, BitSet target, BitSet inf, double init[], BitSet known, IterationMethod iterationMethod, boolean topological) throws PrismException
	{
		BitSet unknown;
		int i, n;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (topological ? "topological, " : "" ) + "with " + iterationMethod.getDescriptionShort();
		mainLog.println("Starting value iteration (" + description + ") ...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit DTMC ReachRewards value iteration (" + description + ")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = dtmc.getNumStates();

		// Initialise solution vector. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) passed in initial value; (4) 0.0
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					init[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					init[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			init = new double[n];
			for (i = 0; i < n; i++)
				init[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		if (iterationsExport != null)
			iterationsExport.exportVector(init, 0);

		IntSet unknownStates = IntSet.asIntSet(unknown);
		IterationMethod.IterationValIter forMvMultRew = iterationMethod.forMvMultRew(dtmc, mcRewards);
		forMvMultRew.init(init);

		if (topological) {
			SCCInfo sccs = new SCCInfo(n);
			SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccs);
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			sccComputer.computeSCCs(false, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = dtmc.mvMultRewJacSingle(s, soln, mcRewards);
			};

			return iterationMethod.doTopologicalValueIteration(this, description, sccs, forMvMultRew, singletonSCCSolver, timer, iterationsExport);
		} else {
			return iterationMethod.doValueIteration(this, description, forMvMultRew, unknownStates, timer, iterationsExport);
		}
	}

	/**
	 * Compute expected reachability rewards using interval iteration.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 * @param topological do topological interval iteration?
	 */
	protected ModelCheckerResult doIntervalIterationReachRewards(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet inf, double init[], BitSet known, IterationMethod iterationMethod, boolean topological)
			throws PrismException
	{
		BitSet unknown;
		int i, n;
		double init_below[], init_above[];
		long timer;

		// Store num states
		n = dtmc.getNumStates();

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(this);

		double upperBound;
		if (iiOptions.hasManualUpperBound()) {
			upperBound = iiOptions.getManualUpperBound();
			getLog().printWarning("Upper bound for interval iteration manually set to " + upperBound);
		} else {
			upperBound = computeReachRewardsUpperBound(dtmc, mcRewards, target, unknown, inf);
		}

		double lowerBound;
		if (iiOptions.hasManualLowerBound()) {
			lowerBound = iiOptions.getManualLowerBound();
			getLog().printWarning("Lower bound for interval iteration manually set to " + lowerBound);
		} else {
			lowerBound = 0.0;
		}

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (topological ? "topological, " : "" ) + "with " + iterationMethod.getDescriptionShort();
		mainLog.println("Starting interval iteration (" + description + ") ...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit DTMC ReachRewards interval iteration (" + description + ") ...");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Create solution vector(s)
		init_below = (init == null) ? new double[n] : init;
		init_above = new double[n];

		// Initialise solution vector from below. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) lowerBound
		if (init != null && known != null) {
			for (i = 0; i < n; i++)
				init_below[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : lowerBound;
		} else {
			for (i = 0; i < n; i++)
				init_below[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : lowerBound;
		}

		// Initialise solution vector from above. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) upperBound
		if (init != null && known != null) {
			for (i = 0; i < n; i++)
				init_above[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : upperBound;
		} else {
			for (i = 0; i < n; i++)
				init_above[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : upperBound;
		}


		if (iterationsExport != null) {
			iterationsExport.exportVector(init_below, 0);
			iterationsExport.exportVector(init_above, 1);
		}

		IntSet unknownStates = IntSet.asIntSet(unknown);

		final boolean enforceMonotonicFromBelow = iiOptions.isEnforceMonotonicityFromBelow();
		final boolean enforceMonotonicFromAbove = iiOptions.isEnforceMonotonicityFromAbove();
		final boolean checkMonotonic = iiOptions.isCheckMonotonicity();

		if (!enforceMonotonicFromAbove) {
			getLog().println("Note: Interval iteration is configured to not enforce monotonicity from above.");
		}
		if (!enforceMonotonicFromBelow) {
			getLog().println("Note: Interval iteration is configured to not enforce monotonicity from below.");
		}

		IterationMethod.IterationIntervalIter below = iterationMethod.forMvMultRewInterval(dtmc, mcRewards, true, enforceMonotonicFromBelow, checkMonotonic);
		IterationMethod.IterationIntervalIter above = iterationMethod.forMvMultRewInterval(dtmc, mcRewards, false, enforceMonotonicFromAbove, checkMonotonic);

		below.init(init_below);
		above.init(init_above);

		ModelCheckerResult rv;
		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, dtmc, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = dtmc.mvMultRewJacSingle(s, soln, mcRewards);
			};

			// run the actual value iteration
			rv = iterationMethod.doTopologicalIntervalIteration(this, description, sccs, below, above, singletonSCCSolver, timer, iterationsExport);
		} else {
			// run the actual value iteration
			rv = iterationMethod.doIntervalIteration(this, description, below, above, unknownStates, timer, iterationsExport);
		}

		double max_v = PrismUtils.findMaxFinite(rv.soln, unknownStates.iterator());
		if (max_v != Double.NEGATIVE_INFINITY) {
			mainLog.println("Maximum finite value in solution vector at end of interval iteration: " + max_v);
		}

		return rv;
	}

	/**
	 * Compute (forwards) steady-state probabilities
	 * i.e. compute the long-run probability of being in each state,
	 * assuming the initial distribution {@code initDist}. 
	 * For space efficiency, the initial distribution vector will be modified and values over-written,  
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public ModelCheckerResult computeSteadyStateProbs(DTMC dtmc, double initDist[]) throws PrismException
	{
		StopWatch watch = new StopWatch().start();

		// Store num states
		int numStates = dtmc.getNumStates();
		// Create results vector
		double[] solnProbs = new double[numStates];

		// Compute bottom strongly connected components (BSCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		BitSet notInBSCCs = sccStore.getNotInBSCCs();
		int numBSCCs = bsccs.size();

		// Compute support of initial distribution
		int numInit = 0;
		BitSet init = new BitSet();
		for (int i = 0; i < numStates; i++) {
			if (initDist[i] > 0)
				init.set(i);
				numInit++;
		}
		// Determine whether initial states are all in the same BSCC 
		int initInOneBSCC = -1;
		for (int b = 0; b < numBSCCs; b++) {
			// test subset via setminus
			BitSet notInB = (BitSet) init.clone();
			notInB.andNot(bsccs.get(b));
			if (notInB.isEmpty()) {
				// all init states in b
				// >> finish
				initInOneBSCC = b;
				break;
			} else if (notInB.cardinality() < numInit) {
				// some init states in b and some not
				// >> abort
				break;
			}
			// no init state in b
			// >> try next BSCC
		}

		// If all initial states are in the same BSCC, it's easy...
		// Just compute steady-state probabilities for the BSCC
		if (initInOneBSCC > -1) {
			mainLog.println("\nInitial states are all in one BSCC (so no reachability probabilities computed)");
			BitSet bscc = bsccs.get(initInOneBSCC);
			computeSteadyStateProbsForBSCC(dtmc, bscc, solnProbs);
		}

		// Otherwise, have to consider all the BSCCs
		else {
			// Compute probability of reaching each BSCC from initial distribution 
			double[] probBSCCs = new double[numBSCCs];
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing probability of reaching BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute probabilities
				double[] reachProbs = computeUntilProbs(dtmc, notInBSCCs, bscc).soln;
				// Compute probability of reaching BSCC, which is dot product of
				// vectors for initial distribution and probabilities of reaching it
				probBSCCs[b] = 0.0;
				for (int i = 0; i < numStates; i++) {
					probBSCCs[b] += initDist[i] * reachProbs[i];
				}
				mainLog.print("\nProbability of reaching BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
			}

			// Compute steady-state probabilities for each BSCC 
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing steady-state probabilities for BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute steady-state probabilities for the BSCC
				computeSteadyStateProbsForBSCC(dtmc, bscc, solnProbs);
				// Multiply by BSCC reach prob
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1))
					solnProbs[i] *= probBSCCs[b];
			}
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = solnProbs;
		res.timeTaken = watch.elapsedSeconds();
		return res;
	}

	/**
	 * Perform (backwards) steady-state probabilities, as required for (e.g. CSL) model checking.
	 * Compute, for each initial state s, the sum over all states s'
	 * of the steady-state probability of being in s'
	 * multiplied by the corresponding probability in the vector {@code multProbs}.
	 * If {@code multProbs} is null, it is assumed to be all 1s.
	 * @param dtmc The DTMC
	 * @param multProbs Multiplication vector (optional: null means all 1s)
	 */
	public ModelCheckerResult computeSteadyStateBackwardsProbs(DTMC dtmc, double multProbs[]) throws PrismException
	{
		StopWatch watch = new StopWatch().start();

		// Store num states
		int numStates = dtmc.getNumStates();

		// Compute bottom strongly connected components (BSCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		BitSet notInBSCCs = sccStore.getNotInBSCCs();
		int numBSCCs = bsccs.size();

		// Compute steady-state probability for each BSCC...
		double[] probBSCCs = new double[numBSCCs];
		double[] ssProbs = new double[numStates];
		for (int b = 0; b < numBSCCs; b++) {
			mainLog.println("\nComputing steady state probabilities for BSCC " + (b + 1));
			BitSet bscc = bsccs.get(b);
			// Compute steady-state probabilities for the BSCC
			computeSteadyStateProbsForBSCC(dtmc, bscc, ssProbs);
			// Compute weighted sum of probabilities with multProbs
			probBSCCs[b] = 0.0;
			if (multProbs == null) {
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
					probBSCCs[b] += ssProbs[i];
				}
			} else {
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
					probBSCCs[b] += multProbs[i] * ssProbs[i];
				}
			}
			mainLog.print("\nValue for BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
		}

		// Create/initialise prob vector
		double[] soln = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			soln[i] = 0.0;
		}

		// If every state is in a BSCC, it's much easier...
		if (notInBSCCs.isEmpty()) {
			mainLog.println("\nAll states are in BSCCs (so no reachability probabilities computed)");
			for (int b = 0; b < numBSCCs; b++) {
				BitSet bscc = bsccs.get(b);
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1))
					soln[i] += probBSCCs[b];
			}
		}

		// Otherwise we have to do more work...
		else {
			// Compute probabilities of reaching each BSCC...
			for (int b = 0; b < numBSCCs; b++) {
				// Skip BSCCs with zero probability
				if (probBSCCs[b] == 0.0)
					continue;
				mainLog.println("\nComputing probabilities of reaching BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute probabilities
				double[] reachProbs = computeUntilProbs(dtmc, notInBSCCs, bscc).soln;
				// Multiply by value for BSCC, add to total
				for (int i = 0; i < numStates; i++) {
					soln[i] += reachProbs[i] * probBSCCs[b];
				}
			}
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = soln;
		res.timeTaken = watch.elapsedSeconds();
		return res;
	}

	/**
	 * Compute steady-state probabilities for a BSCC
	 * i.e. compute the long-run probability of being in each state of the BSCC.
	 * No initial distribution is specified since it does not affect the result.
	 * The result will be stored in the relevant portion of a full vector,
	 * whose size equals the number of states in the DTMC.
	 * Optionally, pass in an existing vector to be used for this purpose;
	 * only the entries of this vector are changed that correspond to the BSCC states.
	 * <p>
	 * To ensure convergence, we use the iteration matrix<br/>
	 * {@code P = (Q * deltaT + I)} where<br/>
	 * {@code Q} is the generator matrix,
	 * {@code deltaT} a preconditioning factor and
	 * {@code I} is the the identity matrix.<br/>
	 * See <em>William J. Stewart: "Introduction to the Numerical Solution of Markov Chains"</em> p.124 for details.
	 * </p>
	 * @param dtmc The DTMC
	 * @param states The BSCC to be analysed
	 * @param result Storage for result (ignored if null)
	 */
	public ModelCheckerResult computeSteadyStateProbsForBSCC(DTMC dtmc, BitSet states, double result[]) throws PrismException
	{
		if (dtmc.getModelType() != ModelType.DTMC) {
			throw new PrismNotSupportedException("Explicit engine currently does not support steady-state computation for " + dtmc.getModelType());
		}
		IterableBitSet bscc = new IterableBitSet(states);

		// Start value iteration
		mainLog.println("Starting value iteration...");
		StopWatch watch = new StopWatch(mainLog).start();

		// Store num states
		int numStates = dtmc.getNumStates();

		// Create solution vector(s)
		// Use the passed in vector, if present
		double[] soln = result == null ? new double[numStates] : result;
		double[] diagsQ = new double[numStates];
		double maxDiagsQ = 0.0;

		// Initialise the solution vector with an equiprobable distribution
		// over the BSCC states.
		// Additionally, compute the diagonal entries of the generator matrix Q.
		// Recall that the entries of the generator matrix are given by
		//     Q(s,t) = prob(s,t)   for s != t
		// and Q(s,s) = -sum_{s!=t} prob(s,t),
		// i.e., diagsQ[s] = -sum_{s!=t} prob(s,t).
		// Furthermore, compute max |diagsQ[s]|.
		double equiprob = 1.0 / states.cardinality();
		for (OfInt iter = bscc.iterator(); iter.hasNext();) {
			int state = iter.nextInt();

			// Equiprobable for BSCC states.
			soln[state] = equiprob;

			// Note: diagsQ[state] = 0.0, as it was freshly created
			// Compute negative exit rate (ignoring a possible self-loop)
			dtmc.forEachTransition(state, (s, t, prob) -> {
				if (s != t) {
					diagsQ[state] -= prob;
				}
			});

			// Note: If there are no outgoing transitions, diagsQ[state] = 0, which is fine

			// Update maximal absolute diagonal entry value of Q
			// As diagsQ[s] <= 0, Math.abs(diagsQ[s]) = -diagsQ[s]
			maxDiagsQ = Math.max(maxDiagsQ, -diagsQ[state]);
		}

		// Compute preconditioning factor deltaT
		// In William J. Stewart: "Introduction to the Numerical Solution of Markov Chains",
		// deltaT = 0.99 / maxDiagsQ is proposed;
		// in the symbolic engines deltaT is computed as 0.99 / max exit[s], i.e., where
		// the denominator corresponds to the maximal exit rate (where self loops are included).
		// Currently, use the same deltaT values as in the symbolic engines,
		// so for DTMCs, as exit[s]=1 for all states, deltaT is 0.99:
		double deltaT = 0.99;
		// TODO: Test and switch to deltaT computed as below, should lead to faster convergence.
		// double deltaT = 0.99 / maxDiagsQ;

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = ExportIterations.createWithUniqueFilename("Explicit DTMC BSCC steady state value iteration", "iterations-ss-bscc");
			iterationsExport.exportVector(soln);
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// create copy of the solution vector
		double[] soln2 = soln.clone();

		// Start iterations
		int iters = 0;
		boolean done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Do vector-matrix multiplication step in (deltaT*Q + I)
			dtmc.vmMultPowerSteadyState(soln, soln2, diagsQ, deltaT, bscc);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, bscc, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			double[] tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;

			if (iterationsExport != null) {
				iterationsExport.exportVector(soln);
			}
		}

		// Finished value iteration
		watch.stop();
		mainLog.println("Power method: " + iters + " iterations in " + watch.elapsedSeconds() + " seconds.");

		// normalise solution
		PrismUtils.normalise(soln, bscc);

		if (iterationsExport != null) {
			// export the normalised vector
			iterationsExport.exportVector(soln);
			iterationsExport.close();
		}

		if (result != null && result != soln) {
			// If result vector was passed in as method argument,
			// it can be the case that result does not point to the current soln vector (most recent values)
			// but to the soln2 vector.
			// In that case, we copy the relevant values from soln to result.
			for (OfInt iter = bscc.iterator(); iter.hasNext();) {
				int state = iter.nextInt();
				result[state] = soln[state];
			}
		}
		// store only one result vector, free temporary vectors
		result = soln;
		soln = soln2 = null;

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.\n" +
			             "Consider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = result;
		res.numIters = iters;
		res.timeTaken = watch.elapsedSeconds();
		return res;
	}

	/**
	 * Compute transient probabilities
	 * i.e. compute the probability of being in each state at time step {@code k},
	 * assuming the initial distribution {@code initDist}. 
	 * For space efficiency, the initial distribution vector will be modified and values over-written,  
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param k Time step
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public ModelCheckerResult computeTransientProbs(DTMC dtmc, int k, double initDist[]) throws PrismException
	{
		ModelCheckerResult res;
		int n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start transient probability computation
		timer = System.currentTimeMillis();
		mainLog.println("Starting transient probability computation...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		// Use the passed in vector, if present
		soln = initDist;
		soln2 = new double[n];

		// Start iterations
		for (iters = 0; iters < k; iters++) {
			// Matrix-vector multiply
			dtmc.vmMult(soln, soln2);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished transient probability computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Transient probability computation");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		DTMCModelChecker mc;
		DTMCSimple dtmc;
		ModelCheckerResult res;
		try {
			// Two examples of building and solving a DTMC

			int version = 2;
			if (version == 1) {

				// 1. Read in from .tra and .lab files
				//    Run as: PRISM_MAINCLASS=explicit.DTMCModelChecker bin/prism dtmc.tra dtmc.lab target_label
				mc = new DTMCModelChecker(null);
				dtmc = new DTMCSimple();
				dtmc.buildFromPrismExplicit(args[0]);
				dtmc.addInitialState(0);
				//System.out.println(dtmc);
				Map<String, BitSet> labels = StateModelChecker.loadLabelsFile(args[1]);
				//System.out.println(labels);
				BitSet target = labels.get(args[2]);
				if (target == null)
					throw new PrismException("Unknown label \"" + args[2] + "\"");
				for (int i = 3; i < args.length; i++) {
					if (args[i].equals("-nopre"))
						mc.setPrecomp(false);
				}
				res = mc.computeReachProbs(dtmc, target);
				System.out.println(res.soln[0]);

			} else {

				// 2. Build DTMC directly
				//    Run as: PRISM_MAINCLASS=explicit.DTMCModelChecker bin/prism
				//    (example taken from p.14 of Lec 5 of http://www.prismmodelchecker.org/lectures/pmc/) 
				mc = new DTMCModelChecker(null);
				dtmc = new DTMCSimple(6);
				dtmc.setProbability(0, 1, 0.1);
				dtmc.setProbability(0, 2, 0.9);
				dtmc.setProbability(1, 0, 0.4);
				dtmc.setProbability(1, 3, 0.6);
				dtmc.setProbability(2, 2, 0.1);
				dtmc.setProbability(2, 3, 0.1);
				dtmc.setProbability(2, 4, 0.5);
				dtmc.setProbability(2, 5, 0.3);
				dtmc.setProbability(3, 3, 1.0);
				dtmc.setProbability(4, 4, 1.0);
				dtmc.setProbability(5, 5, 0.3);
				dtmc.setProbability(5, 4, 0.7);
				System.out.println(dtmc);
				BitSet target = new BitSet();
				target.set(4);
				BitSet remain = new BitSet();
				remain.set(1);
				remain.flip(0, 6);
				System.out.println(target);
				System.out.println(remain);
				res = mc.computeUntilProbs(dtmc, remain, target);
				System.out.println(res.soln[0]);
			}
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
