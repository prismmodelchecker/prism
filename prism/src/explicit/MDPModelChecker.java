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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.Vector;

import common.IterableStateSet;
import common.StopWatch;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.Expression;
import prism.OptionsIntervalIteration;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.PrismUtils;
import strat.MDStrategyArray;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import automata.DA;
import common.IntSet;
import common.IterableBitSet;
import explicit.modelviews.EquivalenceRelationInteger;
import explicit.modelviews.MDPDroppedAllChoices;
import explicit.modelviews.MDPEquiv;
import explicit.rewards.MCRewards;
import explicit.rewards.MCRewardsFromMDPRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;

/**
 * Explicit-state model checker for Markov decision processes (MDPs).
 */
public class MDPModelChecker extends ProbModelChecker
{
	/**
	 * Create a new MDPModelChecker, inherit basic state from parent (unless null).
	 */
	public MDPModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}
	
	// Model checking functions

	@Override
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct, probs;
		MDPModelChecker mcProduct;
		LTLModelChecker.LTLProduct<MDP> product;

		// For min probabilities, need to negate the formula
		// (add parentheses to allow re-parsing if required)
		if (minMax.isMin()) {
			expr = Expression.Not(Expression.Parenth(expr.deepCopy()));
		}

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);

		// Build product of MDP and automaton
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.BUCHI,
				AcceptanceType.RABIN,
				AcceptanceType.GENERALIZED_RABIN,
				AcceptanceType.REACH
		};
		product = mcLtl.constructProductMDP(this, (MDP)model, expr, statesOfInterest, allowedAcceptance);
		
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
			mainLog.println("\nSkipping accepting MEC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		} else {
			mainLog.println("\nFinding accepting MECs...");
			acc = mcLtl.findAcceptingECStates(product.getProductModel(), product.getAcceptance());
		}
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new MDPModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachProbs((MDP) product.getProductModel(), acc, false);
		probsProduct = StateValues.createFromDoubleArray(res.soln, product.getProductModel());

		// Subtract from 1 if we're model checking a negated formula for regular Pmin
		if (minMax.isMin()) {
			probsProduct.timesConstant(-1.0);
			probsProduct.plusConstant(1.0);
		}

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
		MDPRewards productRewards;
		StateValues rewardsProduct, rewards;
		MDPModelChecker mcProduct;
		LTLModelChecker.LTLProduct<MDP> product;

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);

		// Model check maximal state formulas and construct DFA, with the special
		// handling needed for cosafety reward translation
		Vector<BitSet> labelBS = new Vector<BitSet>();
		DA<BitSet, AcceptanceReach> da = mcLtl.constructDFAForCosafetyRewardLTL(this, model, expr, labelBS);

		StopWatch timer = new StopWatch(getLog());
		mainLog.println("\nConstructing " + model.getModelType() + "-" + da.getAutomataType() + " product...");
		timer.start(model.getModelType() + "-" + da.getAutomataType() + " product");
		product = mcLtl.constructProductModel(da, (MDP)model, labelBS, statesOfInterest);
		timer.stop("product has " + product.getProductModel().infoString());

		// Adapt reward info to product model
		productRewards = ((MDPRewards) modelRewards).liftFromModel(product);

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
		mcProduct = new MDPModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachRewards((MDP)product.getProductModel(), productRewards, acc, minMax.isMin());
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
	 * Given a value vector x, compute the probability:
	 *   v(s) = min/max sched [ Sum_s' P_sched(s,s')*x(s') ]  for s labeled with a,
	 *   v(s) = 0   for s not labeled with a.
	 *
	 * Clears the StateValues object x.
	 *
	 * @param tr the transition matrix
	 * @param a the set of states labeled with a
	 * @param x the value vector
	 * @param min compute min instead of max
	 */
	public double[] computeRestrictedNext(MDP mdp, BitSet a, double[] x, boolean min)
	{
		int n;
		double soln[];

		// Store num states
		n = mdp.getNumStates();

		// initialized to 0.0
		soln = new double[n];

		// Next-step probabilities multiplication
		// restricted to a states
		mdp.mvMultMinMax(x, min, soln, a, false, null);

		return soln;
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
	 * while remaining in those in {@code remain}.
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
	 * while remaining in those in {@code remain}.
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values).
	 * Also, 'known' values cannot be passed for some solution methods, e.g. policy iteration.  
	 */
	public ModelCheckerResult computeReachProbs(MDP mdp, BitSet remain, BitSet target, boolean min, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int n, numYes, numNo;
		long timer, timerProb0, timerProb1;
		int strat[] = null;
		// Local copy of setting
		MDPSolnMethod mdpSolnMethod = this.mdpSolnMethod;

		boolean doPmaxQuotient = this.doPmaxQuotient;

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
		if (doIntervalIteration) {
			if (!min && (genStrat || exportAdv)) {
				throw new PrismNotSupportedException("Currently, explicit engine does not support adversary construction for interval iteration and Pmax");
			}
			if (mdpSolnMethod != MDPSolnMethod.VALUE_ITERATION && mdpSolnMethod != MDPSolnMethod.GAUSS_SEIDEL) {
				throw new PrismNotSupportedException("Currently, explicit engine only supports interval iteration with value iteration or Gauss-Seidel for MDPs");
			}
			if (init != null)
				throw new PrismNotSupportedException("Interval iteration currently not supported with provided initial values");
			if (!(precomp && prob0 && prob1)) {
				throw new PrismNotSupportedException("Precomputations (Prob0 & Prob1) must be enabled for interval iteration");
			}

			if (!min) {
				doPmaxQuotient = true;
			}
		}
		if (mdpSolnMethod == MDPSolnMethod.POLICY_ITERATION || mdpSolnMethod == MDPSolnMethod.MODIFIED_POLICY_ITERATION) {
			if (known != null) {
				throw new PrismException("Policy iteration methods cannot be passed 'known' values for some states");
			}
		}

		if (doPmaxQuotient && min) {
			// for Pmin, don't do quotient
			doPmaxQuotient = false;
		}

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting probabilistic reachability (" + (min ? "min" : "max") + ")...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		mdp.checkForDeadlocks(target);

		// Store num states
		n = mdp.getNumStates();

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
				bsInit.set(i, mdp.isInitialState(i));
			}
			List<BitSet> labels = Arrays.asList(bsInit, target);
			List<String> labelNames = Arrays.asList("init", "target");
			mainLog.println("\nExporting target states info to file \"" + getExportTargetFilename() + "\"...");
			exportLabels(mdp, labels, labelNames, Prism.EXPORT_PLAIN, new PrismFileLog(getExportTargetFilename()));
		}

		// If required, create/initialise strategy storage
		// Set choices to -1, denoting unknown
		// (except for target states, which are -2, denoting arbitrary)
		if (genStrat || exportAdv) {
			strat = new int[n];
			for (int i = 0; i < n; i++) {
				strat[i] = target.get(i) ? -2 : -1;
			}
		}

		// Precomputation
		timerProb0 = System.currentTimeMillis();
		if (precomp && prob0) {
			no = prob0(mdp, remain, target, min, strat);
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			yes = prob1(mdp, remain, target, min, strat);
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// If still required, store strategy for no/yes (0/1) states.
		// This is just for the cases max=0 and min=1, where arbitrary choices suffice (denoted by -2)
		if (genStrat || exportAdv) {
			if (min) {
				for (int i = yes.nextSetBit(0); i >= 0; i = yes.nextSetBit(i + 1)) {
					if (!target.get(i))
						strat[i] = -2;
				}
			} else {
				for (int i = no.nextSetBit(0); i >= 0; i = no.nextSetBit(i + 1)) {
					strat[i] = -2;
				}
			}
		}

		// Compute probabilities (if needed)
		if (numYes + numNo < n) {

			if (!min && doPmaxQuotient) {
				MDPEquiv maxQuotient = maxQuotient(mdp, yes, no);
				// MDPEquiv retains original state space, making the states that are not used
				// trap states.
				// yesInQuotient is the representative for the yes equivalence class
				BitSet yesInQuotient = new BitSet();
				yesInQuotient.set(maxQuotient.mapStateToRestrictedModel(yes.nextSetBit(0)));
				// noInQuotient is the representative for the no equivalence class as well
				// as the non-representative states (the states in any equivalence class
				// that are not the representative for the class). As the latter states
				// are traps, we can just add them to the no set
				BitSet noInQuotient = new BitSet();
				noInQuotient.set(maxQuotient.mapStateToRestrictedModel(no.nextSetBit(0)));
				noInQuotient.or(maxQuotient.getNonRepresentativeStates());
				MDPSparse quotientModel = new MDPSparse(maxQuotient);

				ModelCheckerResult res1 = computeReachProbsNumeric(quotientModel,
				                                                   mdpSolnMethod,
				                                                   noInQuotient,
				                                                   yesInQuotient,
				                                                   min,
				                                                   init,
				                                                   known,
				                                                   strat);

				res = new ModelCheckerResult();
				res.numIters = res1.numIters;
				res.timeTaken = res1.timeTaken;
				res.soln = new double[mdp.getNumStates()];
				for (int i = 0; i < n; i++) {
					if (yes.get(i)) {
						res.soln[i] = 1.0;
					} else if (no.get(i)) {
						res.soln[i] = 0.0;
					} else {
						res.soln[i] = res1.soln[maxQuotient.mapStateToRestrictedModel(i)];
					}
				}
			} else {
				res = computeReachProbsNumeric(mdp, mdpSolnMethod, no, yes, min, init, known, strat);
			}
		} else {
			res = new ModelCheckerResult();
			res.soln = Utils.bitsetToDoubleArray(yes, n);
		}

		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Store strategy
		if (genStrat) {
			res.strat = new MDStrategyArray(mdp, strat);
		}
		// Export adversary
		if (exportAdv) {
			// Prune strategy, if needed
			if (getRestrictStratToReach()) {
				restrictStrategyToReachableStates(mdp, strat);
			}
			// Export
			PrismLog out = new PrismFileLog(exportAdvFilename);
			new DTMCFromMDPMemorylessAdversary(mdp, strat).exportToPrismExplicitTra(out);
			out.close();
		}

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timeProb0 = timerProb0 / 1000.0;
		res.timePre = (timerProb0 + timerProb1) / 1000.0;

		return res;
	}

	protected ModelCheckerResult computeReachProbsNumeric(MDP mdp, MDPSolnMethod method, BitSet no, BitSet yes, boolean min, double init[], BitSet known, int strat[]) throws PrismException
	{
		ModelCheckerResult res = null;

		IterationMethod iterationMethod = null;
		switch (method) {
		case VALUE_ITERATION:
			iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
			break;
		case GAUSS_SEIDEL:
			iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, false);
			break;
		case POLICY_ITERATION:
			if (doIntervalIteration) {
				throw new PrismNotSupportedException("Interval iteration currently not supported for policy iteration");
			}
			res = computeReachProbsPolIter(mdp, no, yes, min, strat);
			break;
		case MODIFIED_POLICY_ITERATION:
			if (doIntervalIteration) {
				throw new PrismNotSupportedException("Interval iteration currently not supported for policy iteration");
			}
			res = computeReachProbsModPolIter(mdp, no, yes, min, strat);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + mdpSolnMethod.fullName());
		}

		if (res == null) { // not yet computed, use iterationMethod
			if (!doIntervalIteration) {
				res = doValueIterationReachProbs(mdp, no, yes, min, init, known, iterationMethod, getDoTopologicalValueIteration(), strat);
			} else {
				res = doIntervalIterationReachProbs(mdp, no, yes, min, init, known, iterationMethod, getDoTopologicalValueIteration(), strat);
			}
		}

		return res;
	}

	/**
	 * Prob0 precomputation algorithm.
	 * i.e. determine the states of an MDP which, with min/max probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * {@code min}=true gives Prob0E, {@code min}=false gives Prob0A. 
	 * Optionally, for min only, store optimal (memoryless) strategy info for 0 states. 
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public BitSet prob0(MDPGeneric<?> mdp, BitSet remain, BitSet target, boolean min, int strat[])
	{
		int n, iters;
		BitSet u, soln, unknown;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		if (!silentPrecomputations)
			mainLog.println("Starting Prob0 (" + (min ? "min" : "max") + ")...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(mdp.getNumStates());
			soln.set(0, mdp.getNumStates());

			// for min, generate strategy, any choice (-2) is fine
			if (min && strat != null) {
				Arrays.fill(strat, -2);
			}
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
		if (!silentPrecomputations) {
			mainLog.print("Prob0 (" + (min ? "min" : "max") + ")");
			mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");
		}

		// If required, generate strategy. This is for min probs,
		// so it can be done *after* the main prob0 algorithm (unlike for prob1).
		// We simply pick, for all "no" states, the first choice for which all transitions stay in "no"
		if (strat != null) {
			for (int i = u.nextSetBit(0); i >= 0; i = u.nextSetBit(i + 1)) {
				int numChoices = mdp.getNumChoices(i);
				for (int k = 0; k < numChoices; k++) {
					if (mdp.allSuccessorsInSet(i, k, u)) {
						strat[i] = k;
						continue;
					}
				}
			}
		}

		return u;
	}

	/**
	 * Prob1 precomputation algorithm.
	 * i.e. determine the states of an MDP which, with min/max probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * {@code min}=true gives Prob1A, {@code min}=false gives Prob1E. 
	 * Optionally, for max only, store optimal (memoryless) strategy info for 1 states. 
	 * @param mdp The MDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public BitSet prob1(MDPGeneric<?> mdp, BitSet remain, BitSet target, boolean min, int strat[])
	{
		int n, iters;
		BitSet u, v, soln, unknown;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		if (!silentPrecomputations)
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
				if (min)
					mdp.prob1Astep(unknown, u, v, soln);
				else
					mdp.prob1Estep(unknown, u, v, soln, null);
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

		// If we need to generate a strategy, do another iteration of the inner loop for this
		// We could do this during the main double fixed point above, but we would generate surplus
		// strategy info for non-1 states during early iterations of the outer loop,
		// which are not straightforward to remove since this method does not know which states
		// already have valid strategy info from Prob0.
		// Notice that we only need to look at states in u (since we already know the answer),
		// so we restrict 'unknown' further 
		unknown.and(u);
		if (!min && strat != null) {
			v_done = false;
			v.clear();
			v.or(target);
			soln.clear();
			soln.or(target);
			while (!v_done) {
				mdp.prob1Estep(unknown, u, v, soln, strat);
				v_done = soln.equals(v);
				v.clear();
				v.or(soln);
			}
			u_done = v.equals(u);
		}

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		if (!silentPrecomputations) {
			mainLog.print("Prob1 (" + (min ? "min" : "max") + ")");
			mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");
		}

		return u;
	}

	/**
	 * Compute reachability probabilities using value iteration.
	 * Optionally, store optimal (memoryless) strategy info. 
	 * @param mdp The MDP
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsValIter(MDP mdp, BitSet no, BitSet yes, boolean min, double init[], BitSet known, int strat[])
			throws PrismException
	{
		IterationMethodPower iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
		return doValueIterationReachProbs(mdp, no, yes, min, init, known, iterationMethod, false, strat);
	}

	/**
	 * Compute reachability probabilities using value iteration.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param mdp The MDP
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param iterationMethod The iteration method
	 * @param topological Do topological value iteration?
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null), 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult doValueIterationReachProbs(MDP mdp, BitSet no, BitSet yes, boolean min, double init[], BitSet known, IterationMethod iterationMethod, boolean topological, int strat[])
			throws PrismException
	{
		BitSet unknown;
		int i, n;
		double initVal;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (min ? "min" : "max")
				+ (topological ? ", topological": "" )
				+ ", with " + iterationMethod.getDescriptionShort();

		mainLog.println("Starting value iteration (" + description + ")...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit MDP ReachProbs value iteration (" + description + ")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = mdp.getNumStates();

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

		if (iterationsExport != null)
			iterationsExport.exportVector(init, 0);

		IterationMethod.IterationValIter iteration = iterationMethod.forMvMultMinMax(mdp, min, strat);
		iteration.init(init);

		IntSet unknownStates = IntSet.asIntSet(unknown);

		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = mdp.mvMultJacMinMaxSingle(s, soln, min, strat);
			};

			// run the actual value iteration
			return iterationMethod.doTopologicalValueIteration(this, description, sccs, iteration, singletonSCCSolver, timer, iterationsExport);
		} else {
			// run the actual value iteration
			return iterationMethod.doValueIteration(this, description, iteration, unknownStates, timer, iterationsExport);
		}
	}

	/**
	 * Compute reachability probabilities using interval iteration.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param mdp The MDP
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param iterationMethod The iteration method
	 * @param topological Do topological value iteration?
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult doIntervalIterationReachProbs(MDP mdp, BitSet no, BitSet yes, boolean min, double init[], BitSet known, IterationMethod iterationMethod, boolean topological, int strat[])
			throws PrismException
	{
		BitSet unknown;
		int i, n;
		double initBelow[], initAbove[];
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (min ? "min" : "max")
				+ (topological ? ", topological": "" )
				+ ", with " + iterationMethod.getDescriptionShort();

		mainLog.println("Starting interval iteration (" + description + ")...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit MDP ReachProbs interval iteration (" + description + ")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = mdp.getNumStates();

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

		IterationMethod.IterationIntervalIter below = iterationMethod.forMvMultMinMaxInterval(mdp, min, strat, true, enforceMonotonicFromBelow, checkMonotonic);
		IterationMethod.IterationIntervalIter above = iterationMethod.forMvMultMinMaxInterval(mdp, min, strat, false, enforceMonotonicFromAbove, checkMonotonic);
		below.init(initBelow);
		above.init(initAbove);

		IntSet unknownStates = IntSet.asIntSet(unknown);

		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = mdp.mvMultJacMinMaxSingle(s, soln, min, strat);
			};

			// run the actual value iteration
			return iterationMethod.doTopologicalIntervalIteration(this, description, sccs, below, above, singletonSCCSolver, timer, iterationsExport);
		} else {
			// run the actual value iteration
			return iterationMethod.doIntervalIteration(this, description, below, above, unknownStates, timer, iterationsExport);
		}
	}

	/**
	 * Compute reachability probabilities using Gauss-Seidel (including Jacobi-style updates).
	 * @param mdp The MDP
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param min Min or max probabilities (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsGaussSeidel(MDP mdp, BitSet no, BitSet yes, boolean min, double init[], BitSet known, int strat[])
			throws PrismException
	{
		IterationMethodGS iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, false);
		return doValueIterationReachProbs(mdp, no, yes, min, init, known, iterationMethod, false, strat);
	}

	/**
	 * Compute reachability probabilities using policy iteration.
	 * Optionally, store optimal (memoryless) strategy info. 
	 * @param mdp: The MDP
	 * @param no: Probability 0 states
	 * @param yes: Probability 1 states
	 * @param min: Min or max probabilities (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	protected ModelCheckerResult computeReachProbsPolIter(MDP mdp, BitSet no, BitSet yes, boolean min, int strat[]) throws PrismException
	{
		ModelCheckerResult res;
		int i, n, iters, totalIters;
		double soln[], soln2[];
		boolean done;
		long timer;
		DTMCModelChecker mcDTMC;
		DTMC dtmc;

		// Re-use solution to solve each new policy (strategy)?
		boolean reUseSoln = true;

		// Start policy iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting policy iteration (" + (min ? "min" : "max") + ")...");

		// Create a DTMC model checker (for solving policies)
		mcDTMC = new DTMCModelChecker(this);
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

		// If not passed in, create new storage for strategy and initialise
		// Initial strategy just picks first choice (0) everywhere
		if (strat == null) {
			strat = new int[n];
			for (i = 0; i < n; i++)
				strat[i] = 0;
		}
		// Otherwise, just initialise for states not in yes/no
		// (Optimal choices for yes/no should already be known)
		else {
			for (i = 0; i < n; i++)
				if (!(no.get(i) || yes.get(i)))
					strat[i] = 0;
		}

		boolean backwardsGS = (linEqMethod == LinEqMethod.BACKWARDS_GAUSS_SEIDEL);

		// Start iterations
		iters = totalIters = 0;
		done = false;
		while (!done) {
			iters++;
			// Solve induced DTMC for strategy
			dtmc = new DTMCFromMDPMemorylessAdversary(mdp, strat);
			res = mcDTMC.computeReachProbsGaussSeidel(dtmc, no, yes, reUseSoln ? soln : null, null, backwardsGS);
			soln = res.soln;
			totalIters += res.numIters;
			// Check if optimal, improve non-optimal choices
			mdp.mvMultMinMax(soln, min, soln2, null, false, null);
			done = true;
			for (i = 0; i < n; i++) {
				// Don't look at no/yes states - we may not have strategy info for them,
				// so they might appear non-optimal
				if (no.get(i) || yes.get(i))
					continue;
				if (!PrismUtils.doublesAreClose(soln[i], soln2[i], termCritParam, termCrit == TermCrit.ABSOLUTE)) {
					done = false;
					List<Integer> opt = mdp.mvMultMinMaxSingleChoices(i, soln, min, soln2[i]);
					// Only update strategy if strictly better
					if (!opt.contains(strat[i]))
						strat[i] = opt.get(0);
				}
			}
		}

		// Finished policy iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Policy iteration");
		mainLog.println(" took " + iters + " cycles (" + totalIters + " iterations in total) and " + timer / 1000.0 + " seconds.");

		// Return results
		// (Note we don't add the strategy - the one passed in is already there
		// and might have some existing choices stored for other states).
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
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	protected ModelCheckerResult computeReachProbsModPolIter(MDP mdp, BitSet no, BitSet yes, boolean min, int strat[]) throws PrismException
	{
		ModelCheckerResult res;
		int i, n, iters, totalIters;
		double soln[], soln2[];
		boolean done;
		long timer;
		DTMCModelChecker mcDTMC;
		DTMC dtmc;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting modified policy iteration (" + (min ? "min" : "max") + ")...");

		// Create a DTMC model checker (for solving policies)
		mcDTMC = new DTMCModelChecker(this);
		mcDTMC.inheritSettings(this);
		mcDTMC.setLog(new PrismDevNullLog());

		// Limit iters for DTMC solution - this implements "modified" policy iteration
		mcDTMC.setMaxIters(100);
		mcDTMC.setErrorOnNonConverge(false);

		// Store num states
		n = mdp.getNumStates();

		// Create solution vectors
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = soln2[i] = yes.get(i) ? 1.0 : 0.0;

		// If not passed in, create new storage for strategy and initialise
		// Initial strategy just picks first choice (0) everywhere
		if (strat == null) {
			strat = new int[n];
			for (i = 0; i < n; i++)
				strat[i] = 0;
		}
		// Otherwise, just initialise for states not in yes/no
		// (Optimal choices for yes/no should already be known)
		else {
			for (i = 0; i < n; i++)
				if (!(no.get(i) || yes.get(i)))
					strat[i] = 0;
		}

		boolean backwardsGS = (linEqMethod == LinEqMethod.BACKWARDS_GAUSS_SEIDEL);

		// Start iterations
		iters = totalIters = 0;
		done = false;
		while (!done) {
			iters++;
			// Solve induced DTMC for strategy
			dtmc = new DTMCFromMDPMemorylessAdversary(mdp, strat);
			res = mcDTMC.computeReachProbsGaussSeidel(dtmc, no, yes, soln, null, backwardsGS);
			soln = res.soln;
			totalIters += res.numIters;
			// Check if optimal, improve non-optimal choices
			mdp.mvMultMinMax(soln, min, soln2, null, false, null);
			done = true;
			for (i = 0; i < n; i++) {
				// Don't look at no/yes states - we don't store strategy info for them,
				// so they might appear non-optimal
				if (no.get(i) || yes.get(i))
					continue;
				if (!PrismUtils.doublesAreClose(soln[i], soln2[i], termCritParam, termCrit == TermCrit.ABSOLUTE)) {
					done = false;
					List<Integer> opt = mdp.mvMultMinMaxSingleChoices(i, soln, min, soln2[i]);
					strat[i] = opt.get(0);
				}
			}
		}

		// Finished policy iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Modified policy iteration");
		mainLog.println(" took " + iters + " cycles (" + totalIters + " iterations in total) and " + timer / 1000.0 + " seconds.");

		// Return results
		// (Note we don't add the strategy - the one passed in is already there
		// and might have some existing choices stored for other states).
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
	 * within k steps, and while remaining in states in {@code remain}.
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
	 * within k steps, and while remaining in states in {@code remain}.
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
		mainLog.println("\nStarting bounded probabilistic reachability (" + (min ? "min" : "max") + ")...");

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
	 * Compute expected cumulative (step-bounded) rewards.
	 * i.e. compute the min/max reward accumulated within {@code k} steps.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ModelCheckerResult computeCumulativeRewards(MDP mdp, MDPRewards mdpRewards, int k, boolean min) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		long timer;
		double soln[], soln2[], tmpsoln[];

		// Start expected cumulative reward
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected cumulative reward (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.getNumStates();

		// Create/initialise solution vector(s)
		soln = new double[n];
		soln2 = new double[n];
		for (i = 0; i < n; i++)
			soln[i] = soln2[i] = 0.0;

		// Start iterations
		iters = 0;
		while (iters < k) {
			iters++;
			// Matrix-vector multiply and min/max ops
			mdp.mvMultRewMinMax(soln, mdpRewards, min, soln2, null, false, null);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Expected cumulative reward (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;

		return res;
	}

	/**
	 * Compute upper bound for maximum expected reward, with the method specified in the settings.
	 * @param mdp the model
	 * @param mdpRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @param inf the infinite states
	 * @return upper bound on Rmax=?[ F target ] for all states
	 */
	double computeReachRewardsMaxUpperBound(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		if (unknown.isEmpty()) {
			mainLog.println("Skipping upper bound computation, no unknown states...");
			return 0;
		}

		// inf and target states become trap states (with dropped choices)
		BitSet trapStates = (BitSet) target.clone();
		trapStates.or(inf);
		MDP cleanedMDP = new MDPDroppedAllChoices(mdp, trapStates);

		OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(this);

		double upperBound = 0.0;
		String method = null;
		switch (iiOptions.getBoundMethod()) {
		case VARIANT_1_COARSE:
			upperBound = computeReachRewardsMaxUpperBoundVariant1Coarse(cleanedMDP, mdpRewards, target, unknown, inf);
			method = "variant 1, coarse";
			break;
		case VARIANT_1_FINE:
			upperBound = computeReachRewardsMaxUpperBoundVariant1Fine(cleanedMDP, mdpRewards, target, unknown, inf);
			method = "variant 1, fine";
			break;
		case DEFAULT:
		case VARIANT_2:
			upperBound = computeReachRewardsMaxUpperBoundVariant2(cleanedMDP, mdpRewards, target, unknown, inf);
			method = "variant 2";
			break;
		case DSMPI:
			throw new PrismNotSupportedException("Dijkstra Sweep MPI upper bound heuristic can not be used for Rmax");
		}

		if (method == null) {
			throw new PrismException("Unknown upper bound heuristic");
		}

		mainLog.println("Upper bound for max expectation (" + method + "): " + upperBound);
		return upperBound;
	}

	/**
	 * Compute upper bound for minimum expected reward, with the method specified in the settings.
	 * @param mdp the model
	 * @param mdpRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @param inf the infinite states
	 * @return upper bound on Rmin=?[ F target ] for all unknown states
	 */
	double computeReachRewardsMinUpperBound(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		// inf and target states become trap states (with dropped choices)
		BitSet trapStates = (BitSet) target.clone();
		trapStates.or(inf);
		MDP cleanedMDP = new MDPDroppedAllChoices(mdp, trapStates);

		OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(this);

		double upperBound = 0.0;
		String method = null;
		switch (iiOptions.getBoundMethod()) {
		case DEFAULT:
		case DSMPI:
			upperBound = DijkstraSweepMPI.computeUpperBound(this, mdp, mdpRewards, target, unknown);
			method = "Dijkstra Sweep MPI";
			break;
		case VARIANT_1_COARSE:
			upperBound = computeReachRewardsMaxUpperBoundVariant1Coarse(cleanedMDP, mdpRewards, target, unknown, inf);
			method = "using Rmax upper bound via variant 1, coarse";
			break;
		case VARIANT_1_FINE:
			upperBound = computeReachRewardsMaxUpperBoundVariant1Fine(cleanedMDP, mdpRewards, target, unknown, inf);
			method = "using Rmax upper bound via variant 1, fine";
			break;
		case VARIANT_2:
			upperBound = computeReachRewardsMaxUpperBoundVariant2(cleanedMDP, mdpRewards, target, unknown, inf);
			method = "using Rmax upper bound via variant 2";
			break;
		}

		if (method == null) {
			throw new PrismException("Unknown upper bound heuristic");
		}

		mainLog.println("Upper bound for min expectation (" + method + "): " + upperBound);
		return upperBound;
	}

	/**
	 * Return true if the MDP is contracting for all states in the 'unknown'
	 * set, i.e., if Pmin=1( unknown U target) holds.
	 */
	private boolean isContracting(MDP mdp, BitSet unknown, BitSet target)
	{
		// compute Pmin=1( unknown U target )
		BitSet pmin1 = prob1(mdp, unknown, target, true, null);
		BitSet tmp = (BitSet) unknown.clone();
		tmp.andNot(pmin1);
		if (!tmp.isEmpty()) {
			// unknown is not contained in pmin1, not contracting
			return false;
		}
		return true;
	}

	/**
	 * Compute upper bound for maximum expected reward (variant 1, coarse),
	 * i.e., does not compute separate q_t / p_t per SCC.
	 * Uses Rs = S, i.e., does not take reachability into account.
	 * @param mdp the model
	 * @param mdpRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @return upper bound on Rmax=?[ F target ] for all states
	 */
	double computeReachRewardsMaxUpperBoundVariant1Coarse(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		double[] boundsOnExpectedVisits = new double[mdp.getNumStates()];
		double[] maxRews = new double[mdp.getNumStates()];
		int[] Ct = new int[mdp.getNumStates()];

		StopWatch timer = new StopWatch(getLog());
		timer.start("computing an upper bound for maximal expected reward");

		SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, null);
		BitSet trivial = new BitSet();

		double q = 0;
		for (int scc = 0, numSCCs = sccs.getNumSCCs(); scc < numSCCs; scc++) {
			IntSet statesForSCC = sccs.getStatesForSCC(scc);

			int cardinality = statesForSCC.cardinality();

			PrimitiveIterator.OfInt itSCC = statesForSCC.iterator();
			while (itSCC.hasNext()) {
				int s = itSCC.nextInt();
				Ct[s] = cardinality;

				boolean hasSelfloop = false;
				for (int ch = 0; ch < mdp.getNumChoices(s); ch++) {
					double probRemain = 0;
					boolean allRemain = true;  // all successors remain in the SCC?
					for (Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(s, ch); it.hasNext(); ) {
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
				}

				if (cardinality == 1 && !hasSelfloop) {
					trivial.set(s);
				}
			}
		}

		double p = 1;
		for (int s = 0; s < mdp.getNumStates(); s++) {
			double maxRew = 0;
			for (int ch = 0; ch < mdp.getNumChoices(s); ch++) {
				for (Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(s, ch); it.hasNext(); ) {
					Entry<Integer, Double> t = it.next();
					p = Math.min(p, t.getValue());

					double rew = mdpRewards.getStateReward(s) + mdpRewards.getTransitionReward(s, ch);
					maxRew = Math.max(maxRew, rew);
				}
			}
			maxRews[s] = maxRew;
		}

		double upperBound = 0;
		for (int s = 0; s < mdp.getNumStates(); s++) {
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
				upperBound += boundsOnExpectedVisits[s] * maxRews[s];
			}
		}

		if (OptionsIntervalIteration.from(this).isBoundComputationVerbose()) {
			mainLog.println("Upper bound for max expectation computation (variant 1, coarse):");
			mainLog.println("p = " + p);
			mainLog.println("q = " + q);
			mainLog.println("|Ct| = " + Arrays.toString(Ct));
			mainLog.println("ζ* = " + Arrays.toString(boundsOnExpectedVisits));
			mainLog.println("maxRews = " + Arrays.toString(maxRews));
		}

		timer.stop();
		// mainLog.println("Upper bound for max expectation (variant 1, coarse): " + upperBound);

		if (!Double.isFinite(upperBound)) {
			throw new PrismException("Problem computing an upper bound for the expectation, did not get finite result");
		}

		return upperBound;
	}

	/**
	 * Compute upper bound for maximum expected reward (variant 1, fine).
	 * i.e., does compute separate q_t / p_t per SCC.
	 * Uses Rs = S, i.e., does not take reachability into account.
	 * @param mdp the model
	 * @param mdpRewards the rewards
	 * @param target the target states
	 * @param unknown the states that are not target or infinity states
	 * @return upper bound on Rmax=?[ F target ] for all states
	 */
	double computeReachRewardsMaxUpperBoundVariant1Fine(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		double[] boundsOnExpectedVisits = new double[mdp.getNumStates()];
		double[] qt = new double[mdp.getNumStates()];
		double[] pt = new double[mdp.getNumStates()];
		double[] maxRews = new double[mdp.getNumStates()];
		int[] Ct = new int[mdp.getNumStates()];

		StopWatch timer = new StopWatch(getLog());
		timer.start("computing an upper bound for maximal expected reward");

		SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, null);
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
				boolean hasSelfloop = false;

				for (int ch = 0; ch < mdp.getNumChoices(s); ch++) {

					double probRemain = 0;
					boolean allRemain = true;  // all successors remain in the SCC?
					for (Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(s, ch); it.hasNext(); ) {
						Entry<Integer, Double> t = it.next();
						if (statesForSCC.get(t.getKey())) {
							probRemain += t.getValue();
							p = Math.min(p, t.getValue());
							hasSelfloop = true;
						} else {
							allRemain = false;
						}
					}

					if (!allRemain) { // action in the set Xt
						q = Math.max(q, probRemain);
					}
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

		for (int s = 0; s < mdp.getNumStates(); s++) {
			double maxRew = 0;
			for (int ch = 0; ch < mdp.getNumChoices(s); ch++) {
				double rew = mdpRewards.getStateReward(s) + mdpRewards.getTransitionReward(s, ch);
				maxRew = Math.max(maxRew, rew);
			}
			maxRews[s] = maxRew;
		}

		double upperBound = 0;
		for (int s = 0; s < mdp.getNumStates(); s++) {
			if (target.get(s) || inf.get(s)) {
				// inf or target states: not relevant, set visits to 0, ignore in summation
				boundsOnExpectedVisits[s] = 0.0;
			} else if (unknown.get(s)) {
				if (trivial.get(s)) {
					// s is a trivial SCC: seen at most once
					boundsOnExpectedVisits[s] = 1.0;
				} else {
					boundsOnExpectedVisits[s] = 1 / (Math.pow(pt[s], Ct[s]-1) * (1.0-qt[s]));
				}
				upperBound += boundsOnExpectedVisits[s] * maxRews[s];
			}
		}

		timer.stop();

		if (OptionsIntervalIteration.from(this).isBoundComputationVerbose()) {
			mainLog.println("Upper bound for max expectation computation (variant 1, fine):");
			mainLog.println("pt = " + Arrays.toString(pt));
			mainLog.println("qt = " + Arrays.toString(qt));
			mainLog.println("|Ct| = " + Arrays.toString(Ct));
			mainLog.println("ζ* = " + Arrays.toString(boundsOnExpectedVisits));
			mainLog.println("maxRews = " + Arrays.toString(maxRews));
		}

		// mainLog.println("Upper bound for max expectation (variant 1, fine): " + upperBound);

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
	double computeReachRewardsMaxUpperBoundVariant2(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet unknown, BitSet inf) throws PrismException
	{
		double[] dt = new double[mdp.getNumStates()];
		double[] boundsOnExpectedVisits = new double[mdp.getNumStates()];
		double[] maxRews = new double[mdp.getNumStates()];

		StopWatch timer = new StopWatch(getLog());
		timer.start("computing an upper bound for expected reward");

		SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, unknown::get);

		BitSet T = (BitSet) target.clone();

		@SuppressWarnings("unused")
		int i = 0;
		while (true) {
			BitSet Si = new BitSet();
			i++;

			// TODO: might be inefficient, worst-case quadratic runtime...
			for (PrimitiveIterator.OfInt it = IterableBitSet.getClearBits(T, mdp.getNumStates() -1 ).iterator(); it.hasNext(); ) {
				int s = it.nextInt();
				// mainLog.println("Check " + s + " against " + T);
				boolean allActionsReachT = true;
				for (int choice = 0, choices = mdp.getNumChoices(s); choice < choices; choice++) {
					if (!mdp.someSuccessorsInSet(s, choice, T)) {
						allActionsReachT = false;
						break;
					}
				}
				if (allActionsReachT) {
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

				double min = Double.POSITIVE_INFINITY;
				for (int choice = 0, choices = mdp.getNumChoices(t); choice < choices; choice++) {
					// mainLog.println("State " + t + ", choice = " + choice);
					double d = mdp.sumOverTransitions(t, choice, (int __, int u, double prob) -> {
						// mainLog.println("t = " + t + ", u = " + u + ", prob = " + prob);
						if (!T.get(u))
							return 0.0;

						boolean inSameSCC = (sccs.getSCCIndex(u) == sccIndexForT);
						double d_u_t = inSameSCC ? dt[u] : 1.0;
						// mainLog.println("d_u_t = " + d_u_t);
						return d_u_t * prob;
					});
					if (d < min) {
						min = d;
					}
				}
				dt[t] = min;
				// mainLog.println("d["+t+"] = " + dt[t]);
			}

			T.or(Si);
		}

		for (int s = 0; s < mdp.getNumStates(); s++) {
			double maxRew = 0;
			for (int ch = 0; ch < mdp.getNumChoices(s); ch++) {
				double rew = mdpRewards.getStateReward(s) + mdpRewards.getTransitionReward(s, ch);
				maxRew = Math.max(maxRew, rew);
			}
			maxRews[s] = maxRew;
		}

		double upperBound = 0;
		for (PrimitiveIterator.OfInt it = IterableBitSet.getSetBits(unknown).iterator(); it.hasNext();) {
			int s = it.nextInt();
			boundsOnExpectedVisits[s] = 1 / dt[s];
			upperBound += boundsOnExpectedVisits[s] * maxRews[s];
		}

		timer.stop();

		if (OptionsIntervalIteration.from(this).isBoundComputationVerbose()) {
			mainLog.println("Upper bound for max expectation computation (variant 2):");
			mainLog.println("d_t = " + Arrays.toString(dt));
			mainLog.println("ζ* = " + Arrays.toString(boundsOnExpectedVisits));
		}

		// mainLog.println("Upper bound for expectation (variant 2): " + upperBound);

		if (!Double.isFinite(upperBound)) {
			throw new PrismException("Problem computing an upper bound for the expectation, did not get finite result");
		}

		return upperBound;
	}

	/**
	 * Compute expected instantaneous reward,
	 * i.e. compute the min/max expected reward of the states after {@code k} steps.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param k the number of steps
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ModelCheckerResult computeInstantaneousRewards(MDP mdp, MDPRewards mdpRewards, final int k, boolean min)
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Store num states
		n = mdp.getNumStates();

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards instantaneous rewards computation...");

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = mdpRewards.getStateReward(i);

		// Start iterations
		for (iters = 0; iters < k; iters++) {
			// Matrix-vector multiply
			mdp.mvMultMinMax(soln, min, soln2, null, false, null);
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

	/**
	 * Compute total expected rewards.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ModelCheckerResult computeTotalRewards(MDP mdp, MDPRewards mdpRewards, boolean min) throws PrismException
	{
		if (min) {
			throw new PrismNotSupportedException("Minimum total expected reward not supported in explicit engine");
		} else {
			// max. We don't know if there are positive ECs, so we can't skip precomputation
			return computeTotalRewardsMax(mdp, mdpRewards, false);
		}
	}

	/**
	 * Compute maximal total expected rewards.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param noPositiveECs if true, there are no positive ECs, i.e., all states have finite values (skip precomputation)
	 */
	public ModelCheckerResult computeTotalRewardsMax(MDP mdp, MDPRewards mdpRewards, boolean noPositiveECs) throws PrismException
	{
		ModelCheckerResult res = null;
		int n;
		long timer;
		BitSet inf;

		// Local copy of setting
		MDPSolnMethod mdpSolnMethod = this.mdpSolnMethod;

		// Switch to a supported method, if necessary
		if (!(mdpSolnMethod == MDPSolnMethod.VALUE_ITERATION || mdpSolnMethod == MDPSolnMethod.GAUSS_SEIDEL || mdpSolnMethod == MDPSolnMethod.POLICY_ITERATION)) {
			mdpSolnMethod = MDPSolnMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to MDP solution method \"" + mdpSolnMethod.fullName() + "\"");
		}
		if (getDoIntervalIteration()) {
			throw new PrismNotSupportedException("Interval iteration for total rewards is currently not supported");
		}

		// Start expected total reward
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting total expected reward (max)...");

		// Store num states
		n = mdp.getNumStates();

		long timerPre;

		if (noPositiveECs) {
			// no inf states
			inf = new BitSet();
			timerPre = 0;
		} else {
			mainLog.println("Precomputation: Find positive end components...");

			timerPre = System.currentTimeMillis();

			ECComputer ecs = ECComputer.createECComputer(this, mdp);
			ecs.computeMECStates();
			BitSet positiveECs = new BitSet();
			for (BitSet ec : ecs.getMECStates()) {
				// check if this MEC is positive
				boolean positiveEC = false;
				for (int state : new IterableStateSet(ec, n)) {
					if (mdpRewards.getStateReward(state) > 0) {
						// state with positive reward in this MEC
						positiveEC = true;
						break;
					}
					for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
						if (mdpRewards.getTransitionReward(state, choice) > 0 &&
								mdp.allSuccessorsInSet(state, choice, ec)) {
							// choice from this state with positive reward back into this MEC
							positiveEC = true;
							break;
						}
					}
				}
				if (positiveEC) {
					positiveECs.or(ec);
				}
			}

			// inf = Pmax[ <> positiveECs ] > 0
			//     = ! (Pmax[ <> positiveECs ] = 0)
			inf = prob0(mdp, null, positiveECs, false, null);  // Pmax[ <> positiveECs ] = 0
			inf.flip(0,n);  // !(Pmax[ <> positive ECs ] = 0) = Pmax[ <> positiveECs ] > 0

			timerPre = System.currentTimeMillis() - timerPre;
			mainLog.println("Precomputation took " + timerPre / 1000.0 + " seconds, " + inf.cardinality() + " infinite states, " + (n - inf.cardinality()) + " states remaining.");
		}

		// Compute rewards
		// do standard max reward calculation, but with empty target set
		switch (mdpSolnMethod) {
		case VALUE_ITERATION:
			res = computeReachRewardsValIter(mdp, mdpRewards, new BitSet(), inf, false, null, null, null);
			break;
		case GAUSS_SEIDEL:
			res = computeReachRewardsGaussSeidel(mdp, mdpRewards, new BitSet(), inf, false, null, null, null);
			break;
		case POLICY_ITERATION:
			res = computeReachRewardsPolIter(mdp, mdpRewards, new BitSet(), inf, false, null);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + mdpSolnMethod.fullName());
		}

		// Finished expected total reward
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected total reward took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timePre = timerPre / 1000.0;

		// Return results
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
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values).  
	 * Also, 'known' values cannot be passed for some solution methods, e.g. policy iteration.  
	 */
	public ModelCheckerResult computeReachRewards(MDP mdp, MDPRewards mdpRewards, BitSet target, boolean min, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet inf;
		int n, numTarget, numInf;
		long timer, timerProb1;
		int strat[] = null;
		// Local copy of setting
		MDPSolnMethod mdpSolnMethod = this.mdpSolnMethod;

		// Switch to a supported method, if necessary
		if (!(mdpSolnMethod == MDPSolnMethod.VALUE_ITERATION || mdpSolnMethod == MDPSolnMethod.GAUSS_SEIDEL || mdpSolnMethod == MDPSolnMethod.POLICY_ITERATION)) {
			mdpSolnMethod = MDPSolnMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to MDP solution method \"" + mdpSolnMethod.fullName() + "\"");
		}

		// Check for some unsupported combinations
		if (mdpSolnMethod == MDPSolnMethod.POLICY_ITERATION) {
			if (known != null) {
				throw new PrismException("Policy iteration methods cannot be passed 'known' values for some states");
			}
		}
		if (doIntervalIteration) {
			if (mdpSolnMethod != MDPSolnMethod.VALUE_ITERATION && mdpSolnMethod != MDPSolnMethod.GAUSS_SEIDEL) {
				throw new PrismNotSupportedException("Currently, explicit engine only supports interval iteration with value iteration or Gauss-Seidel for MDPs");
			}
		}

		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability (" + (min ? "min" : "max") + ")...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		mdp.checkForDeadlocks(target);

		// Store num states
		n = mdp.getNumStates();
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
				bsInit.set(i, mdp.isInitialState(i));
			}
			List<BitSet> labels = Arrays.asList(bsInit, target);
			List<String> labelNames = Arrays.asList("init", "target");
			mainLog.println("\nExporting target states info to file \"" + getExportTargetFilename() + "\"...");
			exportLabels(mdp, labels, labelNames, Prism.EXPORT_PLAIN, new PrismFileLog(getExportTargetFilename()));
		}

		// If required, create/initialise strategy storage
		// Set choices to -1, denoting unknown
		// (except for target states, which are -2, denoting arbitrary)
		if (genStrat || exportAdv || mdpSolnMethod == MDPSolnMethod.POLICY_ITERATION) {
			strat = new int[n];
			for (int i = 0; i < n; i++) {
				strat[i] = target.get(i) ? -2 : -1;
			}
		}
		
		// Precomputation (not optional)
		timerProb1 = System.currentTimeMillis();
		inf = prob1(mdp, null, target, !min, strat);
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;
		
		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// If required, generate strategy for "inf" states.
		if (genStrat || exportAdv || mdpSolnMethod == MDPSolnMethod.POLICY_ITERATION) {
			if (min) {
				// If min reward is infinite, all choices give infinity
				// So the choice can be arbitrary, denoted by -2; 
				for (int i = inf.nextSetBit(0); i >= 0; i = inf.nextSetBit(i + 1)) {
					strat[i] = -2;
				}
			} else {
				// If max reward is infinite, there is at least one choice giving infinity.
				// So we pick, for all "inf" states, the first choice for which some transitions stays in "inf".
				for (int i = inf.nextSetBit(0); i >= 0; i = inf.nextSetBit(i + 1)) {
					int numChoices = mdp.getNumChoices(i);
					for (int k = 0; k < numChoices; k++) {
						if (mdp.someSuccessorsInSet(i, k, inf)) {
							strat[i] = k;
							continue;
						}
					}
				}
			}
		}

		ZeroRewardECQuotient quotient = null;
		boolean doZeroMECCheckForMin = true;
		if (min & doZeroMECCheckForMin) {
			StopWatch zeroMECTimer = new StopWatch(mainLog);
			zeroMECTimer.start("checking for zero-reward ECs");
			mainLog.println("For Rmin, checking for zero-reward ECs...");
			BitSet unknown = (BitSet) inf.clone();
			unknown.flip(0, mdp.getNumStates());
			unknown.andNot(target);
			quotient = ZeroRewardECQuotient.getQuotient(this, mdp, unknown, mdpRewards);

			if (quotient == null) {
				zeroMECTimer.stop("no zero-reward ECs found, proceeding normally");
			} else {
				zeroMECTimer.stop("built quotient MDP with " + quotient.getNumberOfZeroRewardMECs() + " zero-reward MECs");
				if (strat != null) {
					throw new PrismException("Constructing a strategy for Rmin in the presence of zero-reward ECs is currently not supported");
				}
			}
		}

		if (quotient != null) {
			BitSet newInfStates = (BitSet)inf.clone();
			newInfStates.or(quotient.getNonRepresentativeStates());
			int quotientModelStates = quotient.getModel().getNumStates() - newInfStates.cardinality();
			mainLog.println("Computing Rmin in zero-reward EC quotient model (" + quotientModelStates + " relevant states)...");
			res = computeReachRewardsNumeric(quotient.getModel(), quotient.getRewards(), mdpSolnMethod, target, newInfStates, min, init, known, strat);
			quotient.mapResults(res.soln);
		} else {
			res = computeReachRewardsNumeric(mdp, mdpRewards, mdpSolnMethod, target, inf, min, init, known, strat);
		}

		// Store strategy
		if (genStrat) {
			res.strat = new MDStrategyArray(mdp, strat);
		}
		// Export adversary
		if (exportAdv) {
			// Prune strategy, if needed
			if (getRestrictStratToReach()) {
				restrictStrategyToReachableStates(mdp, strat);
			}
			// Export
			PrismLog out = new PrismFileLog(exportAdvFilename);
			new DTMCFromMDPMemorylessAdversary(mdp, strat).exportToPrismExplicitTra(out);
			out.close();
		}

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timePre = timerProb1 / 1000.0;

		return res;
	}

	protected ModelCheckerResult computeReachRewardsNumeric(MDP mdp, MDPRewards mdpRewards, MDPSolnMethod method, BitSet target, BitSet inf, boolean min, double init[], BitSet known, int strat[]) throws PrismException
	{
		ModelCheckerResult res = null;

		IterationMethod iterationMethod = null;
		switch (method) {
		case VALUE_ITERATION:
			iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
			break;
		case GAUSS_SEIDEL:
			iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, false);
			break;
		case POLICY_ITERATION:
			if (doIntervalIteration) {
				throw new PrismNotSupportedException("Interval iteration currently not supported for policy iteration");
			}
			res = computeReachRewardsPolIter(mdp, mdpRewards, target, inf, min, strat);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + method.fullName());
		}

		if (res == null) { // not yet computed, use iterationMethod
			if (!doIntervalIteration) {
				res = doValueIterationReachRewards(mdp, mdpRewards, iterationMethod, target, inf, min, init, known, getDoTopologicalValueIteration(), strat);
			} else {
				res = doIntervalIterationReachRewards(mdp, mdpRewards, iterationMethod, target, inf, min, init, known, getDoTopologicalValueIteration(), strat);
			}
		}

		return res;
	}

	/**
	 * Compute expected reachability rewards using value iteration.
	 * Optionally, store optimal (memoryless) strategy info. 
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsValIter(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet inf, boolean min, double init[], BitSet known, int strat[])
			throws PrismException
	{
		IterationMethodPower iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
		return doValueIterationReachRewards(mdp, mdpRewards, iterationMethod, target, inf, min, init, known, false, strat);
	}

	/**
	 * Compute expected reachability rewards using value iteration.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param topological Do topological value iteration?
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult doValueIterationReachRewards(MDP mdp, MDPRewards mdpRewards, IterationMethod iterationMethod, BitSet target, BitSet inf, boolean min, double init[], BitSet known, boolean topological, int strat[])
			throws PrismException
	{
		BitSet unknown;
		int i, n;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (min ? "min" : "max") + (topological ? ", topological" : "" ) + ", with " + iterationMethod.getDescriptionShort();
		mainLog.println("Starting value iteration (" + description + ")...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit MDP ReachRewards value iteration (" + description +")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Store num states
		n = mdp.getNumStates();

		// Initialise solution vectors. Use (where available) the following in order of preference:
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

		IterationMethod.IterationValIter forMvMultRewMinMax = iterationMethod.forMvMultRewMinMax(mdp, mdpRewards, min, strat);
		forMvMultRewMinMax.init(init);

		IntSet unknownStates = IntSet.asIntSet(unknown);

		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = mdp.mvMultRewJacMinMaxSingle(s, soln, mdpRewards, min, strat);
			};

			// run the actual value iteration
			return iterationMethod.doTopologicalValueIteration(this, description, sccs, forMvMultRewMinMax, singletonSCCSolver, timer, iterationsExport);
		} else {
			// run the actual value iteration
			return iterationMethod.doValueIteration(this, description, forMvMultRewMinMax, unknownStates, timer, iterationsExport);
		}
	}

	/**
	 * Compute expected reachability rewards using Gauss-Seidel (including Jacobi-style updates).
	 * Optionally, store optimal (memoryless) strategy info. 
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsGaussSeidel(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet inf, boolean min, double init[],
			BitSet known, int strat[]) throws PrismException
	{
		IterationMethodGS iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, false);
		return doValueIterationReachRewards(mdp, mdpRewards, iterationMethod, target, inf, min, init, known, false, strat);
	}

	/**
	 * Compute expected reachability rewards using interval iteration
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param topological do topological interval iteration
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult doIntervalIterationReachRewards(MDP mdp, MDPRewards mdpRewards, IterationMethod iterationMethod, BitSet target, BitSet inf, boolean min, double init[], BitSet known, boolean topological, int strat[])
			throws PrismException
	{
		BitSet unknown;
		int i, n;
		double initBelow[], initAbove[];
		long timer;

		// Store num states
		n = mdp.getNumStates();

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
			if (min) {
				upperBound = computeReachRewardsMinUpperBound(mdp, mdpRewards, target, unknown, inf);
			} else {
				upperBound = computeReachRewardsMaxUpperBound(mdp, mdpRewards, target, unknown, inf);
			}
		}

		double lowerBound;
		if (iiOptions.hasManualLowerBound()) {
			lowerBound = iiOptions.getManualLowerBound();
			getLog().printWarning("Lower bound for interval iteration manually set to " + lowerBound);
		} else {
			lowerBound = 0.0;
		}

		if (min) {
			if (!isContracting(mdp, unknown, target)) {
				throw new PrismNotSupportedException("Interval iteration for Rmin and non-contracting MDP currently not supported");
			} else {
				mainLog.println("Relevant sub-MDP is contracting, proceed...");
			}
		}

		// Start value iteration
		timer = System.currentTimeMillis();
		String description = (min ? "min" : "max") + (topological ? ", topological" : "") + ", with " + iterationMethod.getDescriptionShort();
		mainLog.println("Starting interval iteration (" + description + ")...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Explicit MDP ReachRewards interval iteration (" + description + ")");
			mainLog.println("Exporting iterations to " + iterationsExport.getFileName());
		}

		// Create initial solution vector(s)
		initBelow = (init == null) ? new double[n] : init;
		initAbove = new double[n];

		// Initialise solution vector from below. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) lowerBound
		if (init != null && known != null) {
			for (i = 0; i < n; i++)
				initBelow[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : lowerBound;
		} else {
			for (i = 0; i < n; i++)
				initBelow[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : lowerBound;
		}

		// Initialise solution vector from above. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) upperBound
		if (init != null && known != null) {
			for (i = 0; i < n; i++)
				initAbove[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : upperBound;
		} else {
			for (i = 0; i < n; i++)
				initAbove[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : upperBound;
		}

		if (iterationsExport != null) {
			iterationsExport.exportVector(initBelow, 0);
			iterationsExport.exportVector(initAbove, 1);
		}

		final boolean enforceMonotonicFromBelow = iiOptions.isEnforceMonotonicityFromBelow();
		final boolean enforceMonotonicFromAbove = iiOptions.isEnforceMonotonicityFromAbove();
		final boolean checkMonotonic = iiOptions.isCheckMonotonicity();

		if (!enforceMonotonicFromAbove) {
			getLog().println("Note: Interval iteration is configured to not enforce monotonicity from above.");
		}
		if (!enforceMonotonicFromBelow) {
			getLog().println("Note: Interval iteration is configured to not enforce monotonicity from below.");
		}

		IterationMethod.IterationIntervalIter below = iterationMethod.forMvMultRewMinMaxInterval(mdp, mdpRewards, min, strat, true, enforceMonotonicFromBelow, checkMonotonic);
		IterationMethod.IterationIntervalIter above = iterationMethod.forMvMultRewMinMaxInterval(mdp, mdpRewards, min, strat, false, enforceMonotonicFromAbove, checkMonotonic);
		below.init(initBelow);
		above.init(initAbove);

		IntSet unknownStates = IntSet.asIntSet(unknown);

		ModelCheckerResult rv;
		if (topological) {
			// Compute SCCInfo, including trivial SCCs in the subgraph obtained when only considering
			// states in unknown
			SCCInfo sccs = SCCComputer.computeTopologicalOrdering(this, mdp, true, unknown::get);

			IterationMethod.SingletonSCCSolver singletonSCCSolver = (int s, double[] soln) -> {
				soln[s] = mdp.mvMultRewJacMinMaxSingle(s, soln, mdpRewards, min, strat);
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
	 * Compute expected reachability rewards using policy iteration.
	 * The array {@code strat} is used both to pass in the initial strategy for policy iteration,
	 * and as storage for the resulting optimal strategy (if needed).
	 * Passing in an initial strategy is required when some states have infinite reward,
	 * to avoid the possibility of policy iteration getting stuck on an infinite-value strategy.
	 * @param mdp The MDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	protected ModelCheckerResult computeReachRewardsPolIter(MDP mdp, MDPRewards mdpRewards, BitSet target, BitSet inf, boolean min, int strat[])
			throws PrismException
	{
		ModelCheckerResult res;
		int i, n, iters, totalIters;
		double soln[], soln2[];
		boolean done;
		long timer;
		DTMCModelChecker mcDTMC;
		DTMC dtmc;
		MCRewards mcRewards;

		// Re-use solution to solve each new policy (strategy)?
		boolean reUseSoln = true;

		// Start policy iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting policy iteration (" + (min ? "min" : "max") + ")...");

		// Create a DTMC model checker (for solving policies)
		mcDTMC = new DTMCModelChecker(this);
		mcDTMC.inheritSettings(this);
		mcDTMC.setLog(new PrismDevNullLog());

		// Store num states
		n = mdp.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;

		// If not passed in, create new storage for strategy and initialise
		// Initial strategy just picks first choice (0) everywhere
		if (strat == null) {
			strat = new int[n];
			for (i = 0; i < n; i++)
				strat[i] = 0;
		}
			
		// Start iterations
		iters = totalIters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Solve induced DTMC for strategy
			dtmc = new DTMCFromMDPMemorylessAdversary(mdp, strat);
			mcRewards = new MCRewardsFromMDPRewards(mdpRewards, strat);
			res = mcDTMC.computeReachRewardsValIter(dtmc, mcRewards, target, inf, reUseSoln ? soln : null, null);
			soln = res.soln;
			totalIters += res.numIters;
			// Check if optimal, improve non-optimal choices
			mdp.mvMultRewMinMax(soln, mdpRewards, min, soln2, null, false, null);
			done = true;
			for (i = 0; i < n; i++) {
				// Don't look at target/inf states - we may not have strategy info for them,
				// so they might appear non-optimal
				if (target.get(i) || inf.get(i))
					continue;
				if (!PrismUtils.doublesAreClose(soln[i], soln2[i], termCritParam, termCrit == TermCrit.ABSOLUTE)) {
					done = false;
					List<Integer> opt = mdp.mvMultRewMinMaxSingleChoices(i, soln, mdpRewards, min, soln2[i]);
					// Only update strategy if strictly better
					if (!opt.contains(strat[i]))
						strat[i] = opt.get(0);
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
	 * Restrict a (memoryless) strategy for an MDP, stored as an integer array of choice indices,
	 * to the states of the MDP that are reachable under that strategy.  
	 * @param mdp The MDP
	 * @param strat The strategy
	 */
	public void restrictStrategyToReachableStates(MDP mdp, int strat[])
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
				if (strat[s] >= 0) {
					Iterator<Map.Entry<Integer, Double>> iter = mdp.getTransitionsIterator(s, strat[s]);
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
		// Set strategy choice for non-reachable state to -1
		int n = mdp.getNumStates();
		for (int s = restrict.nextClearBit(0); s < n; s = restrict.nextClearBit(s + 1)) {
			strat[s] = -3;
		}
	}

	/**
	 * Compute the end component quotient (for use with PMax),
	 * each maximal end component is collapsed to a single state,
	 * likewise the yes and no regions, respectively.
	 */
	private MDPEquiv maxQuotient(MDP mdp, BitSet yes, BitSet no) throws PrismException
	{
		BitSet maybe = new BitSet();
		maybe.set(0, mdp.getNumStates());
		maybe.andNot(yes);
		maybe.andNot(no);

		ECComputer ec = ECComputer.createECComputer(this, mdp);

		ec.computeMECStates(maybe);
		List<BitSet> mecs = ec.getMECStates();
		mecs.add(yes);
		mecs.add(no);

		EquivalenceRelationInteger eq = new EquivalenceRelationInteger(mecs);
		BasicModelTransformation<MDP, MDPEquiv> quotientTransform = MDPEquiv.transformDroppingLoops(mdp, eq);
		MDPEquiv quotient = quotientTransform.getTransformedModel();

		//mdp.exportToDotFile("original.dot");
		//quotient.exportToDotFile("maxQuotient.dot");

		int realStates = quotient.getNumStates() - quotient.getNonRepresentativeStates().cardinality();
		mainLog.println("Max-Quotient MDP: " + realStates + " equivalence classes / non-trap states.");

		return quotient;
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
			mc = new MDPModelChecker(null);
			mdp = new MDPSimple();
			mdp.buildFromPrismExplicit(args[0]);
			mdp.addInitialState(0);
			//System.out.println(mdp);
			labels = StateModelChecker.loadLabelsFile(args[1]);
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
