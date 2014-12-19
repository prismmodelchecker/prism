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
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import parser.ast.Expression;
import parser.type.TypeDouble;
import prism.DRA;
import prism.Pair;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismUtils;
import explicit.rewards.MCRewards;

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
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct, probs;
		Expression ltl;
		DRA<BitSet> dra;
		Model modelProduct;
		DTMCModelChecker mcProduct;
		long time;

		// Can't do LTL with time-bounded variants of the temporal operators
		if (Expression.containsTemporalTimeBounds(expr)) {
			throw new PrismException("Time-bounded operators not supported in LTL: " + expr);
		}

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);

		// Model check maximal state formulas
		Vector<BitSet> labelBS = new Vector<BitSet>();
		ltl = mcLtl.checkMaximalStateFormulas(this, model, expr.deepCopy(), labelBS);

		// Convert LTL formula to deterministic Rabin automaton (DRA)
		mainLog.println("\nBuilding deterministic Rabin automaton (for " + ltl + ")...");
		time = System.currentTimeMillis();
		dra = LTLModelChecker.convertLTLFormulaToDRA(ltl);
		int draSize = dra.size();
		mainLog.println("DRA has " + dra.size() + " states, " + dra.getNumAcceptancePairs() + " pairs.");
		time = System.currentTimeMillis() - time;
		mainLog.println("Time for Rabin translation: " + time / 1000.0 + " seconds.");
		// If required, export DRA 
		if (settings.getExportPropAut()) {
			mainLog.println("Exporting DRA to file \"" + settings.getExportPropAutFilename() + "\"...");
			PrismLog out = new PrismFileLog(settings.getExportPropAutFilename());
			out.println(dra);
			out.close();
			//dra.printDot(new java.io.PrintStream("dra.dot"));
		}

		// Build product of Markov chain and automaton
		mainLog.println("\nConstructing MC-DRA product...");
		Pair<Model, int[]> pair = mcLtl.constructProductMC(dra, (DTMC) model, labelBS);
		modelProduct = pair.first;
		int invMap[] = pair.second;
		mainLog.print("\n" + modelProduct.infoStringTable());

		// Find accepting states + compute reachability probabilities
		BitSet acc = null;
		if (dra.isDFA()) {
			// For a DFA, just collect the accept states
			mainLog.println("\nSkipping BSCC detection since DRA is a DFA...");
			acc = mcLtl.findTargetStatesForRabin(dra, modelProduct, invMap);
		} else {
			// Usually, we have to detect BSCCs in the product
			mainLog.println("\nFinding accepting BSCCs...");
			acc = mcLtl.findAcceptingBSCCsForRabin(dra, modelProduct, invMap);
		}
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new DTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		probsProduct = StateValues.createFromDoubleArray(mcProduct.computeReachProbs((DTMC) modelProduct, acc).soln, modelProduct);

		// Mapping probabilities in the original model
		double[] probsProductDbl = probsProduct.getDoubleArray();
		double[] probsDbl = new double[model.getNumStates()];

		// Get the probabilities for the original model by taking the initial states
		// of the product and projecting back to the states of the original model
		for (int i : modelProduct.getInitialStates()) {
			int s = invMap[i] / draSize;
			probsDbl[s] = probsProductDbl[i];
		}

		probs = StateValues.createFromDoubleArray(probsDbl, model);
		probsProduct.clear();

		return probs;
	}

	public ModelCheckerResult computeInstantaneousRewards(DTMC dtmc, MCRewards mcRewards, double t) throws PrismException
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
		mainLog.println("\nStarting backwards instantaneous rewards computation...");

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = mcRewards.getStateReward(i);

		// Start iterations
		for (iters = 0; iters < right; iters++) {
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
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time step 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over,
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param k Time step
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues doTransient(DTMC dtmc, int k, double initDist[]) throws PrismException
	{
		throw new PrismException("Not implemented yet");
	}

	// Utility methods for probability distributions

	/**
	 * Generate a probability distribution, stored as a StateValues object, from a file.
	 * If {@code distFile} is null, so is the return value.
	 */
	public StateValues readDistributionFromFile(File distFile, Model model) throws PrismException
	{
		StateValues dist = null;

		if (distFile != null) {
			mainLog.println("\nImporting probability distribution from file \"" + distFile + "\"...");
			// Build an empty vector 
			dist = new StateValues(TypeDouble.getInstance(), model);
			// Populate vector from file
			dist.readFromFile(distFile);
		}

		return dist;
	}

	/**
	 * Build a probability distribution, stored as a StateValues object,
	 * from the initial states info of the current model: either probability 1 for
	 * the (single) initial state or equiprobable over multiple initial states.
	 */
	public StateValues buildInitialDistribution(Model model) throws PrismException
	{
		StateValues dist = null;

		// Build an empty vector 
		dist = new StateValues(TypeDouble.getInstance(), model);
		// Populate vector (equiprobable over initial states)
		double d = 1.0 / model.getNumInitialStates();
		for (int in : model.getInitialStates()) {
			dist.setDoubleValue(in, d);
		}

		return dist;
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
	 * while remaining in those in @{code remain}.
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
	 * while remaining in those in @{code remain}.
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
		int i, n, numYes, numNo;
		long timer, timerProb0, timerProb1;
		// Local copy of setting
		LinEqMethod linEqMethod = this.linEqMethod;

		// Switch to a supported method, if necessary
		if (!(linEqMethod == LinEqMethod.POWER || linEqMethod == LinEqMethod.GAUSS_SEIDEL)) {
			linEqMethod = LinEqMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting probabilistic reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null) {
			BitSet targetNew = new BitSet(n);
			for (i = 0; i < n; i++) {
				targetNew.set(i, target.get(i) || (known.get(i) && init[i] == 1.0));
			}
			target = targetNew;
		}

		// Precomputation
		timerProb0 = System.currentTimeMillis();
		if (precomp && prob0) {
			no = prob0(dtmc, remain, target);
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			yes = prob1(dtmc, remain, target);
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// Compute probabilities
		switch (linEqMethod) {
		case POWER:
			res = computeReachProbsValIter(dtmc, no, yes, init, known);
			break;
		case GAUSS_SEIDEL:
			res = computeReachProbsGaussSeidel(dtmc, no, yes, init, known);
			break;
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
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
	 * Prob0 precomputation algorithm.
	 * i.e. determine the states of a DTMC which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in @{code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
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
		mainLog.print("Prob0");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Prob1 precomputation algorithm.
	 * i.e. determine the states of a DTMC which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in @{code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
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
		mainLog.print("Prob1");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Compute reachability probabilities using value iteration.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsValIter(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], initVal;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) passed in initial value; (4) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above. 
		initVal = (valIterDir == ValIterDir.BELOW) ? 0.0 : 1.0;
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : initVal;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, unknown, false);
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
	 * Compute reachability probabilities using Gauss-Seidel.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsGaussSeidel(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], initVal, maxDiff;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting Gauss-Seidel...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector
		soln = (init == null) ? new double[n] : init;

		// Initialise solution vector. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) passed in initial value; (4) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above. 
		initVal = (valIterDir == ValIterDir.BELOW) ? 0.0 : 1.0;
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : initVal;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			maxDiff = dtmc.mvMultGS(soln, unknown, false, termCrit == TermCrit.ABSOLUTE);
			// Check termination
			done = maxDiff < termCritParam;
		}

		// Finished Gauss-Seidel
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Gauss-Seidel");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

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
	 * within k steps, and while remaining in states in @{code remain}.
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
	 * within k steps, and while remaining in states in @{code remain}.
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
		int i, n, numTarget, numInf;
		long timer, timerProb1;
		// Local copy of setting
		LinEqMethod linEqMethod = this.linEqMethod;

		// Switch to a supported method, if necessary
		if (!(linEqMethod == LinEqMethod.POWER)) {
			linEqMethod = LinEqMethod.POWER;
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
		if (init != null && known != null) {
			BitSet targetNew = new BitSet(n);
			for (i = 0; i < n; i++) {
				targetNew.set(i, target.get(i) || (known.get(i) && init[i] == 0.0));
			}
			target = targetNew;
		}

		// Precomputation (not optional)
		timerProb1 = System.currentTimeMillis();
		inf = prob1(dtmc, null, target);
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// Compute rewards
		switch (linEqMethod) {
		case POWER:
			res = computeReachRewardsValIter(dtmc, mcRewards, target, inf, init, known);
			break;
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
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

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			//mainLog.println(soln);
			iters++;
			// Matrix-vector multiply
			dtmc.mvMultRew(soln, mcRewards, soln2, unknown, false);
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
		ModelCheckerResult res;
		BitSet startNot, bscc;
		double probBSCCs[], solnProbs[], reachProbs[];
		int n, numBSCCs = 0, allInOneBSCC;
		long timer;

		timer = System.currentTimeMillis();

		// Store num states
		n = dtmc.getNumStates();
		// Create results vector
		solnProbs = new double[n];

		// Compute bottom strongly connected components (BSCCs)
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc);
		sccComputer.computeBSCCs();
		List<BitSet> bsccs = sccComputer.getBSCCs();
		BitSet notInBSCCs = sccComputer.getNotInBSCCs();
		numBSCCs = bsccs.size();

		// See which states in the initial distribution do *not* have non-zero prob
		startNot = new BitSet();
		for (int i = 0; i < n; i++) {
			if (initDist[i] == 0)
				startNot.set(i);
		}
		// Determine whether initial states are all in a single BSCC 
		allInOneBSCC = -1;
		for (int b = 0; b < numBSCCs; b++) {
			if (!bsccs.get(b).intersects(startNot)) {
				allInOneBSCC = b;
				break;
			}
		}

		// If all initial states are in a single BSCC, it's easy...
		// Just compute steady-state probabilities for the BSCC
		if (allInOneBSCC != -1) {
			mainLog.println("\nInitial states all in one BSCC (so no reachability probabilities computed)");
			bscc = bsccs.get(allInOneBSCC);
			computeSteadyStateProbsForBSCC(dtmc, bscc, solnProbs);
		}

		// Otherwise, have to consider all the BSCCs
		else {

			// Compute probability of reaching each BSCC from initial distribution 
			probBSCCs = new double[numBSCCs];
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing probability of reaching BSCC " + (b + 1));
				bscc = bsccs.get(b);
				// Compute probabilities
				reachProbs = computeUntilProbs(dtmc, notInBSCCs, bscc).soln;
				// Compute probability of reaching BSCC, which is dot product of
				// vectors for initial distribution and probabilities of reaching it
				probBSCCs[b] = 0.0;
				for (int i = 0; i < n; i++) {
					probBSCCs[b] += initDist[i] * reachProbs[i];
				}
				mainLog.print("\nProbability of reaching BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
			}

			// Compute steady-state probabilities for each BSCC 
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing steady-state probabilities for BSCC " + (b + 1));
				bscc = bsccs.get(b);
				// Compute steady-state probabilities for the BSCC
				computeSteadyStateProbsForBSCC(dtmc, bscc, solnProbs);
				// Multiply by BSCC reach prob
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1))
					solnProbs[i] *= probBSCCs[b];
			}
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = solnProbs;
		timer = System.currentTimeMillis() - timer;
		res.timeTaken = timer / 1000.0;
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
		ModelCheckerResult res;
		BitSet bscc;
		double probBSCCs[], ssProbs[], reachProbs[], soln[];
		int n, numBSCCs = 0;
		long timer;

		timer = System.currentTimeMillis();

		// Store num states
		n = dtmc.getNumStates();

		// Compute bottom strongly connected components (BSCCs)
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc);
		sccComputer.computeBSCCs();
		List<BitSet> bsccs = sccComputer.getBSCCs();
		BitSet notInBSCCs = sccComputer.getNotInBSCCs();
		numBSCCs = bsccs.size();

		// Compute steady-state probability for each BSCC...
		probBSCCs = new double[numBSCCs];
		ssProbs = new double[n];
		for (int b = 0; b < numBSCCs; b++) {
			mainLog.println("\nComputing steady state probabilities for BSCC " + (b + 1));
			bscc = bsccs.get(b);
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
		soln = new double[n];
		for (int i = 0; i < n; i++) {
			soln[i] = 0.0;
		}

		// If every state is in a BSCC, it's much easier...
		if (notInBSCCs.isEmpty()) {
			mainLog.println("\nAll states are in BSCCs (so no reachability probabilities computed)");
			for (int b = 0; b < numBSCCs; b++) {
				bscc = bsccs.get(b);
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
				bscc = bsccs.get(b);
				// Compute probabilities
				reachProbs = computeUntilProbs(dtmc, notInBSCCs, bscc).soln;
				// Multiply by value for BSCC, add to total
				for (int i = 0; i < n; i++) {
					soln[i] += reachProbs[i] * probBSCCs[b];
				}
			}
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		timer = System.currentTimeMillis() - timer;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute steady-state probabilities for a BSCC
	 * i.e. compute the long-run probability of being in each state of the BSCC.
	 * No initial distribution is specified since it does not affect the result.
	 * The result will be stored in the relevant portion of a full vector,
	 * whose size equals the number of states in the DTMC.
	 * Optionally, pass in an existing vector to be used for this purpose.
	 * @param dtmc The DTMC
	 * @param bscc The BSCC to be analysed
	 * @param result Storage for result (ignored if null)
	 */
	public ModelCheckerResult computeSteadyStateProbsForBSCC(DTMC dtmc, BitSet bscc, double result[]) throws PrismException
	{
		ModelCheckerResult res;
		int n, iters;
		double soln[], soln2[], tmpsoln[];
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		// Use the passed in vector, if present
		soln = result == null ? new double[n] : result;
		soln2 = new double[n];

		// Initialise solution vectors. Equiprobable for BSCC states.
		double equiprob = 1.0 / bscc.cardinality();
		for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1))
			soln[i] = soln2[i] = equiprob;

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			dtmc.vmMult(soln, soln2);
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
		throw new PrismException("Not implemented yet");
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
				//System.out.println(dtmc);
				Map<String, BitSet> labels = mc.loadLabelsFile(args[1]);
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
