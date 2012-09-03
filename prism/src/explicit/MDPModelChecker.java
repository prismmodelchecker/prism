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

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import parser.ast.Expression;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismUtils;
import explicit.rewards.MDPRewards;

/**
 * Explicit-state model checker for Markov decision processes (MDPs).
 */
public class MDPModelChecker extends ProbModelChecker
{
	// Model checking functions

	/**
	 * Compute probabilities for the contents of a P operator.
	 */
	protected StateValues checkProbPathFormula(Model model, Expression expr, boolean min) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and then pass control to appropriate method. 
		if (expr.isSimplePathFormula()) {
			return checkProbPathFormulaSimple(model, expr, min);
		} else {
			throw new PrismException("Explicit engine does not yet handle LTL-style path formulas");
		}
	}

	/**
	 * Compute probabilities for a simple, non-LTL path operator.
	 */
	protected StateValues checkProbPathFormulaSimple(Model model, Expression expr, boolean min) throws PrismException
	{
		StateValues probs = null;

		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				probs = checkProbPathFormulaSimple(model, exprUnary.getOperand(), min);
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Compute, then subtract from 1 
				probs = checkProbPathFormulaSimple(model, exprUnary.getOperand(), !min);
				probs.timesConstant(-1.0);
				probs.plusConstant(1.0);
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(model, exprTemp, min);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(model, exprTemp, min);
				} else {
					probs = checkProbUntil(model, exprTemp, min);
				}
			}
			// Anything else - convert to until and recurse
			else {
				probs = checkProbPathFormulaSimple(model, exprTemp.convertToUntilForm(), min);
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		return probs;
	}

	/**
	 * Compute probabilities for a next operator.
	 */
	protected StateValues checkProbNext(Model model, ExpressionTemporal expr, boolean min) throws PrismException
	{
		BitSet target = null;
		ModelCheckerResult res = null;

		// Model check the operand
		target = checkExpression(model, expr.getOperand2()).getBitSet();

		res = computeNextProbs((MDP) model, target, min);
		return StateValues.createFromDoubleArray(res.soln, model);
	}
	
	/**
	 * Compute probabilities for a bounded until operator.
	 */
	protected StateValues checkProbBoundedUntil(Model model, ExpressionTemporal expr, boolean min) throws PrismException
	{
		int time;
		BitSet b1, b2;
		StateValues probs = null;
		ModelCheckerResult res = null;

		// get info from bounded until
		time = expr.getUpperBound().evaluateInt(constantValues);
		if (expr.upperBoundIsStrict())
			time--;
		if (time < 0) {
			String bound = expr.upperBoundIsStrict() ? "<" + (time + 1) : "<=" + time;
			throw new PrismException("Invalid bound " + bound + " in bounded until formula");
		}

		// model check operands first
		b1 = checkExpression(model, expr.getOperand1()).getBitSet();
		b2 = checkExpression(model, expr.getOperand2()).getBitSet();

		// print out some info about num states
		// mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1,
		// allDDRowVars.n()));
		// mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2,
		// allDDRowVars.n()) + " states\n");

		// Compute probabilities

		// a trivial case: "U<=0"
		if (time == 0) {
			// prob is 1 in b2 states, 0 otherwise
			probs = StateValues.createFromBitSetAsDoubles(b2, model);
		} else {
			res = computeBoundedUntilProbs((MDP) model, b1, b2, time, min);
			probs = StateValues.createFromDoubleArray(res.soln, model);
		}

		return probs;
	}

	/**
	 * Compute probabilities for an (unbounded) until operator.
	 */
	protected StateValues checkProbUntil(Model model, ExpressionTemporal expr, boolean min) throws PrismException
	{
		BitSet b1, b2;
		StateValues probs = null;
		ModelCheckerResult res = null;

		// model check operands first
		b1 = checkExpression(model, expr.getOperand1()).getBitSet();
		b2 = checkExpression(model, expr.getOperand2()).getBitSet();

		// print out some info about num states
		// mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1,
		// allDDRowVars.n()));
		// mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2,
		// allDDRowVars.n()) + " states\n");

		res = computeUntilProbs((MDP) model, b1, b2, min);
		probs = StateValues.createFromDoubleArray(res.soln, model);

		return probs;
	}

	/**
	 * Compute rewards for the contents of an R operator.
	 */
	protected StateValues checkRewardFormula(Model model, MDPRewards modelRewards, Expression expr, boolean min) throws PrismException
	{
		StateValues rewards = null;
		
		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_F:
				rewards = checkRewardReach(model, modelRewards, exprTemp, min);
				break;
			default:
				throw new PrismException("Explicit engine does not yet handle the " + exprTemp.getOperatorSymbol() + " operator in the R operator");
			}
		}
		
		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		return rewards;
	}

	/**
	 * Compute rewards for a reachability reward operator.
	 */
	protected StateValues checkRewardReach(Model model, MDPRewards modelRewards, ExpressionTemporal expr, boolean min) throws PrismException
	{
		BitSet b;
		StateValues rewards = null;
		ModelCheckerResult res = null;

		// model check operand first
		b = checkExpression(model, expr.getOperand2()).getBitSet();

		// print out some info about num states
		// mainLog.print("\nb = " + JDD.GetNumMintermsString(b1,
		// allDDRowVars.n()));

		res = computeReachRewards((MDP) model, modelRewards, b, min);
		rewards = StateValues.createFromDoubleArray(res.soln, model);

		return rewards;
	}

	// Numerical computation functions

	/**
	 * Compute next=state probabilities.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param mdp The MDP
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult computeNextProbs(MDP mdp, BitSet target, boolean min) throws PrismException
	{
		ModelCheckerResult res = null;
		int n;
		double soln[], soln2[];
		long timer;

		timer = System.currentTimeMillis();
		
		// Store num states
		n = mdp.getNumStates();

		// Create/initialise solution vector(s)
		soln = Utils.bitsetToDoubleArray(target, n);
		soln2 = new double[n];

		// Next-step probabilities 
		mdp.mvMultMinMax(soln, min, soln2, null, false, null);

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln2;
		res.numIters = 1;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute reachability probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target}.
	 * @param mdp The MDP
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult computeReachProbs(MDP mdp, BitSet target, boolean min) throws PrismException
	{
		return computeReachProbs(mdp, null, target, min, null, null);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in @{code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult computeUntilProbs(MDP mdp, BitSet remain, BitSet target, boolean min) throws PrismException
	{
		return computeReachProbs(mdp, remain, target, min, null, null);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in @{code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachProbs(MDP mdp, BitSet remain, BitSet target, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int i, n, numYes, numNo;
		long timer, timerProb0, timerProb1;
		boolean genAdv;
		// Local copy of setting
		MDPSolnMethod mdpSolnMethod = this.mdpSolnMethod;

		// Switch to a supported method, if necessary
		if (mdpSolnMethod == MDPSolnMethod.LINEAR_PROGRAMMING) {
			mdpSolnMethod = MDPSolnMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to MDP solution method \"" + mdpSolnMethod.fullName() + "\"");
		}
		
		// Check for some unsupported combinations
		if (mdpSolnMethod == MDPSolnMethod.VALUE_ITERATION && valIterDir == ValIterDir.ABOVE) {
			if (!(precomp && prob0)) 
				throw new PrismException("Precomputation (Prob0) must be enabled for value iteration from above");
			if (!min) 
				throw new PrismException("Value iteration from above only works for minimum probabilities");
		}

		// Are we generating an optimal adversary?
		genAdv = exportAdv;

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting probabilistic reachability (" + (min ? "min" : "max") + ")...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		mdp.checkForDeadlocks(target);

		// Store num states
		n = mdp.getNumStates();

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
			no = prob0(mdp, remain, target, min);
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1 && !genAdv) {
			yes = prob1(mdp, remain, target, min);
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// Compute probabilities
		switch (mdpSolnMethod) {
		case VALUE_ITERATION:
			res = computeReachProbsValIter(mdp, no, yes, min, init, known);
			break;
		case GAUSS_SEIDEL:
			res = computeReachProbsGaussSeidel(mdp, no, yes, min, init, known);
			break;
		case POLICY_ITERATION:
			res = computeReachProbsPolIter(mdp, no, yes, min);
			break;
		case MODIFIED_POLICY_ITERATION:
			res = computeReachProbsModPolIter(mdp, no, yes, min);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + mdpSolnMethod.fullName());
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
	 * i.e. determine the states of an MDP which, with min/max probability 0,
	 * reach a state in {@code target}, while remaining in those in @{code remain}.
	 * {@code min}=true gives Prob0E, {@code min}=false gives Prob0A. 
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public BitSet prob0(MDP mdp, BitSet remain, BitSet target, boolean min)
	{
		int n, iters;
		BitSet u, soln, unknown;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob0 (" + (min ? "min" : "max") + ")...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(mdp.getNumStates());
			soln.set(0, mdp.getNumStates());
			return soln;
		}

		// Initialise vectors
		n = mdp.getNumStates();
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
			mdp.prob0step(unknown, u, min, soln);
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
		mainLog.print("Prob0 (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Prob1 precomputation algorithm.
	 * i.e. determine the states of an MDP which, with min/max probability 1,
	 * reach a state in {@code target}, while remaining in those in @{code remain}.
	 * {@code min}=true gives Prob1A, {@code min}=false gives Prob1E. 
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public BitSet prob1(MDP mdp, BitSet remain, BitSet target, boolean min)
	{
		int n, iters;
		BitSet u, v, soln, unknown;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob1 (" + (min ? "min" : "max") + ")...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(mdp.getNumStates());
		}

		// Initialise vectors
		n = mdp.getNumStates();
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
				mdp.prob1step(unknown, u, v, min, soln);
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
		mainLog.print("Prob1 (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Compute reachability probabilities using value iteration.
	 * @param mdp The MDP
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsValIter(MDP mdp, BitSet no, BitSet yes, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], initVal;
		int adv[] = null;
		boolean genAdv, done;
		long timer;

		// Are we generating an optimal adversary?
		genAdv = exportAdv;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.getNumStates();

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

		// Create/initialise adversary storage
		if (genAdv) {
			adv = new int[n];
			for (i = 0; i < n; i++) {
				adv[i] = -1;
			}
		}

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply and min/max ops
			mdp.mvMultMinMax(soln, min, soln2, unknown, false, genAdv ? adv : null);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error
		if (!done) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}
		
		// Process adversary
		if (genAdv) {
			// Prune adversary
			restrictAdversaryToReachableStates(mdp, adv);
			// Print adversary
			PrismLog out = new PrismFileLog(exportAdvFilename);
			out.print("Adv:");
			for (i = 0; i < n; i++) {
				out.print(" " + i + ":" + adv[i]);
			}
			out.println();
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute reachability probabilities using Gauss-Seidel (including Jacobi-style updates).
	 * @param mdp The MDP
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsGaussSeidel(MDP mdp, BitSet no, BitSet yes, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], initVal, maxDiff;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting Gauss-Seidel (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.getNumStates();

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
			maxDiff = mdp.mvMultGSMinMax(soln, min, unknown, false, termCrit == TermCrit.ABSOLUTE);
			// Check termination
			done = maxDiff < termCritParam;
		}

		// Finished Gauss-Seidel
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Gauss-Seidel");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error
		if (!done) {
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
	 * Compute reachability probabilities using policy iteration.
	 * @param mdp: The MDP
	 * @param no: Probability 0 states
	 * @param yes: Probability 1 states
	 * @param min: Min or max probabilities (true=min, false=max)
	 */
	protected ModelCheckerResult computeReachProbsPolIter(MDP mdp, BitSet no, BitSet yes, boolean min) throws PrismException
	{
		ModelCheckerResult res;
		int i, n, iters, totalIters;
		double soln[], soln2[];
		boolean done;
		long timer;
		int adv[];
		DTMCModelChecker mcDTMC;
		DTMC dtmc;

		// Re-use solution to solve each new adversary?
		boolean reUseSoln = true;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting policy iteration (" + (min ? "min" : "max") + ")...");

		// Create a DTMC model checker (for solving policies)
		mcDTMC = new DTMCModelChecker();
		mcDTMC.inheritSettings(this);
		mcDTMC.setLog(new PrismDevNullLog());

		// Store num states
		n = mdp.getNumStates();

		// Create solution vectors
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = soln2[i] = yes.get(i) ? 1.0 : 0.0;

		// Generate initial adversary
		adv = new int[n];
		for (i = 0; i < n; i++)
			adv[i] = 0;

		// Start iterations
		iters = totalIters = 0;
		done = false;
		while (!done) {
			iters++;
			// Solve induced DTMC for adversary
			dtmc = new DTMCFromMDPMemorylessAdversary(mdp, adv);
			res = mcDTMC.computeReachProbsGaussSeidel(dtmc, no, yes, reUseSoln ? soln : null, null);
			soln = res.soln;
			totalIters += res.numIters;
			// Check if optimal, improve non-optimal choices
			mdp.mvMultMinMax(soln, min, soln2, null, false, null);
			done = true;
			for (i = 0; i < n; i++) {
				// Don't look at no/yes states - we don't store adversary info for them,
				// so they might appear non-optimal
				if (no.get(i) || yes.get(i))
					continue;
				if (!PrismUtils.doublesAreClose(soln[i], soln2[i], termCritParam, termCrit == TermCrit.ABSOLUTE)) {
					done = false;
					List<Integer> opt = mdp.mvMultMinMaxSingleChoices(i, soln, min, soln2[i]);
					// If update adversary if strictly better
					if (!opt.contains(adv[i]))
						adv[i] = opt.get(0);
				}
			}
		}

		// Finished policy iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Policy iteration");
		mainLog.println(" took " + iters + " cycles (" + totalIters + " iterations in total) and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = totalIters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute reachability probabilities using modified policy iteration.
	 * @param mdp: The MDP
	 * @param no: Probability 0 states
	 * @param yes: Probability 1 states
	 * @param min: Min or max probabilities (true=min, false=max)
	 */
	protected ModelCheckerResult computeReachProbsModPolIter(MDP mdp, BitSet no, BitSet yes, boolean min) throws PrismException
	{
		ModelCheckerResult res;
		int i, n, iters, totalIters;
		double soln[], soln2[];
		boolean done;
		long timer;
		int adv[];
		DTMCModelChecker mcDTMC;
		DTMC dtmc;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting modified policy iteration (" + (min ? "min" : "max") + ")...");

		// Create a DTMC model checker (for solving policies)
		mcDTMC = new DTMCModelChecker();
		mcDTMC.inheritSettings(this);
		mcDTMC.setLog(new PrismDevNullLog());

		// Limit iters for DTMC solution - this implements "modified" policy iteration
		mcDTMC.setMaxIters(100);

		// Store num states
		n = mdp.getNumStates();

		// Create solution vectors
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = soln2[i] = yes.get(i) ? 1.0 : 0.0;

		// Generate initial adversary
		adv = new int[n];
		for (i = 0; i < n; i++)
			adv[i] = 0;

		// Start iterations
		iters = totalIters = 0;
		done = false;
		while (!done) {
			iters++;
			// Solve induced DTMC for adversary
			dtmc = new DTMCFromMDPMemorylessAdversary(mdp, adv);
			res = mcDTMC.computeReachProbsGaussSeidel(dtmc, no, yes, soln, null);
			soln = res.soln;
			totalIters += res.numIters;
			// Check if optimal, improve non-optimal choices
			mdp.mvMultMinMax(soln, min, soln2, null, false, null);
			done = true;
			for (i = 0; i < n; i++) {
				// Don't look at no/yes states - we don't store adversary info for them,
				// so they might appear non-optimal
				if (no.get(i) || yes.get(i))
					continue;
				if (!PrismUtils.doublesAreClose(soln[i], soln2[i], termCritParam, termCrit == TermCrit.ABSOLUTE)) {
					done = false;
					List<Integer> opt = mdp.mvMultMinMaxSingleChoices(i, soln, min, soln2[i]);
					adv[i] = opt.get(0);
				}
			}
		}

		// Finished policy iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Modified policy iteration");
		mainLog.println(" took " + iters + " cycles (" + totalIters + " iterations in total) and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = totalIters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Construct strategy information for min/max reachability probabilities.
	 * (More precisely, list of indices of choices resulting in min/max.)
	 * (Note: indices are guaranteed to be sorted in ascending order.)
	 * @param mdp The MDP
	 * @param state The state to generate strategy info for
	 * @param target The set of target states to reach
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param lastSoln Vector of values from which to recompute in one iteration 
	 */
	public List<Integer> probReachStrategy(MDP mdp, int state, BitSet target, boolean min, double lastSoln[]) throws PrismException
	{
		double val = mdp.mvMultMinMaxSingle(state, lastSoln, min, null);
		return mdp.mvMultMinMaxSingleChoices(state, lastSoln, min, val);
	}

	/**
	 * Compute bounded reachability probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target} within k steps.
	 * @param mdp The MDP
	 * @param target Target states
	 * @param k Bound
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult computeBoundedReachProbs(MDP mdp, BitSet target, int k, boolean min) throws PrismException
	{
		return computeBoundedReachProbs(mdp, null, target, k, min, null, null);
	}

	/**
	 * Compute bounded until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in @{code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult computeBoundedUntilProbs(MDP mdp, BitSet remain, BitSet target, int k, boolean min) throws PrismException
	{
		return computeBoundedReachProbs(mdp, remain, target, k, min, null, null);
	}

	/**
	 * Compute bounded reachability/until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in @{code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param results Optional array of size k+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult computeBoundedReachProbs(MDP mdp, BitSet remain, BitSet target, int k, boolean min, double init[], double results[])
			throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting bounded probabilistic reachability (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.getNumStates();

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
			results[0] = Utils.minMaxOverArraySubset(soln2, mdp.getInitialStates(), true);
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
			// Matrix-vector multiply and min/max ops
			mdp.mvMultMinMax(soln, min, soln2, unknown, false, null);
			// Store intermediate results if required
			// (compute min/max value over initial states for this step)
			if (results != null) {
				// TODO: whether this is min or max should be specified somehow
				results[iters] = Utils.minMaxOverArraySubset(soln2, mdp.getInitialStates(), true);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Print vector (for debugging)
		mainLog.println(soln);

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Bounded probabilistic reachability (" + (min ? "min" : "max") + ")");
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
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ModelCheckerResult computeReachRewards(MDP mdp, MDPRewards mdpRewards, BitSet target, boolean min) throws PrismException
	{
		return computeReachRewards(mdp, mdpRewards, target, min, null, null);
	}

	/**
	 * Compute expected reachability rewards.
	 * i.e. compute the min/max reward accumulated to reach a state in {@code target}.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachRewards(MDP mdp, MDPRewards mdpRewards, BitSet target, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet inf;
		int i, n, numTarget, numInf;
		long timer, timerProb1;
		// Local copy of setting
		MDPSolnMethod mdpSolnMethod = this.mdpSolnMethod;

		// Switch to a supported method, if necessary
		if (!(mdpSolnMethod == MDPSolnMethod.VALUE_ITERATION || mdpSolnMethod == MDPSolnMethod.GAUSS_SEIDEL)) {
			mdpSolnMethod = MDPSolnMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to MDP solution method \"" + mdpSolnMethod.fullName() + "\"");
		}

		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting expected reachability (" + (min ? "min" : "max") + ")...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		mdp.checkForDeadlocks(target);

		// Store num states
		n = mdp.getNumStates();

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
		inf = prob1(mdp, null, target, !min);
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// Compute rewards
		switch (mdpSolnMethod) {
		case VALUE_ITERATION:
			res = computeReachRewardsValIter(mdp, mdpRewards, target, inf, min, init, known);
			break;
		case GAUSS_SEIDEL:
			res = computeReachRewardsGaussSeidel(mdp, mdpRewards, target, inf, min, init, known);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + mdpSolnMethod.fullName());
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
	 * Compute expected reachability rewards using Gauss-Seidel (including Jacobi-style updates).
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsGaussSeidel(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet inf, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], maxDiff;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting Gauss-Seidel (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.getNumStates();

		// Create solution vector(s)
		soln = (init == null) ? new double[n] : init;

		// Initialise solution vector. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) passed in initial value; (4) 0.0
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
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
			// Matrix-vector multiply and min/max ops
			maxDiff = mdp.mvMultRewGSMinMax(soln, mdpRewards, min, unknown, false, termCrit == TermCrit.ABSOLUTE);
			// Check termination
			done = maxDiff < termCritParam;
		}

		// Finished Gauss-Seidel
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Gauss-Seidel (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error
		if (!done) {
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
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsValIter(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet inf, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.getNumStates();

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
			// Matrix-vector multiply and min/max ops
			mdp.mvMultRewMinMax(soln, mdpRewards, min, soln2, unknown, false, null);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error
		if (!done) {
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
	 * Construct strategy information for min/max expected reachability.
	 * (More precisely, list of indices of choices resulting in min/max.)
	 * (Note: indices are guaranteed to be sorted in ascending order.)
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param state The state to generate strategy info for
	 * @param target The set of target states to reach
	 * @param min Min or max rewards (true=min, false=max)
	 * @param lastSoln Vector of values from which to recompute in one iteration 
	 */
	public List<Integer> expReachStrategy(MDP mdp, MDPRewards mdpRewards, int state, BitSet target, boolean min, double lastSoln[]) throws PrismException
	{
		double val = mdp.mvMultRewMinMaxSingle(state, lastSoln, mdpRewards, min, null);
		return mdp.mvMultRewMinMaxSingleChoices(state, lastSoln, mdpRewards, min, val);
	}

	/**
	 * Restrict a (memoryless) adversary for an MDP, stored as an integer array of choice indices,
	 * to the states of the MDP that are reachable under that adversary.  
	 * @param mdp The MDP
	 * @param adv The adversary
	 */
	public void restrictAdversaryToReachableStates(MDP mdp,int adv[])
	{
		BitSet restrict = new BitSet();
		BitSet explore = new BitSet();
		// Get initial states
		for (int is : mdp.getInitialStates()) {
			restrict.set(is);
			explore.set(is);
		}
		// Compute reachable states (store in 'restrict') 
		boolean foundMore = true;
		while (foundMore) {
			foundMore = false;
			for (int s = explore.nextSetBit(0); s >= 0; s = explore.nextSetBit(s + 1)) {
				explore.set(s, false);
				if (adv[s] >= 0) {
					Iterator<Map.Entry<Integer, Double>> iter = mdp.getTransitionsIterator(s, adv[s]);
					while (iter.hasNext()) {
						Map.Entry<Integer, Double> e = iter.next();
						int dest = e.getKey();
						if (!restrict.get(dest)) {
							foundMore = true;
							restrict.set(dest);
							explore.set(dest);
						}
					}
				}
			}
		}
		// Set adversary choice for non-reachable state to -1
		int n = mdp.getNumStates();
		for (int s = restrict.nextClearBit(0); s < n; s = restrict.nextClearBit(s + 1)) {
			adv[s] = -1;
		}
	}
	
	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		MDPModelChecker mc;
		MDPSimple mdp;
		ModelCheckerResult res;
		BitSet init, target;
		Map<String, BitSet> labels;
		boolean min = true;
		try {
			mc = new MDPModelChecker();
			mdp = new MDPSimple();
			mdp.buildFromPrismExplicit(args[0]);
			//System.out.println(mdp);
			labels = mc.loadLabelsFile(args[1]);
			//System.out.println(labels);
			init = labels.get("init");
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			for (int i = 3; i < args.length; i++) {
				if (args[i].equals("-min"))
					min = true;
				else if (args[i].equals("-max"))
					min = false;
				else if (args[i].equals("-nopre"))
					mc.setPrecomp(false);
			}
			res = mc.computeReachProbs(mdp, target, min);
			System.out.println(res.soln[init.nextSetBit(0)]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
