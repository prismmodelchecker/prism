//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
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

package prism;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import odd.ODDUtils;

import jdd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import strat.MDStrategyIV;
import hybrid.*;
import parser.ast.*;
import parser.visitor.ASTTraverse;

/*
 * Model checker for MDPs
 */
public class NondetModelChecker extends NonProbModelChecker
{
	// Model (MDP)
	protected NondetModel model;

	// Extra (MDP) model info
	protected JDDNode nondetMask;
	protected JDDVars allDDNondetVars;

	// Options (in addition to those inherited from StateModelChecker):

	// Use 0,1 precomputation algorithms?
	// if 'precomp' is false, this disables all (non-essential) use of prob0/prob1
	// if 'precomp' is true, the values of prob0/prob1 determine what is used
	// (currently prob0/prob are not under user control)
	protected boolean precomp;
	protected boolean prob0;
	protected boolean prob1;
	// Use fairness?
	protected boolean fairness;

	// Constructor

	public NondetModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);
		if (!(m instanceof NondetModel)) {
			throw new PrismException("Wrong model type passed to NondetModelChecker.");
		}
		model = (NondetModel) m;
		nondetMask = model.getNondetMask();
		allDDNondetVars = model.getAllDDNondetVars();

		// Inherit some options from parent Prism object and store locally.
		precomp = prism.getPrecomp();
		prob0 = prism.getProb0();
		prob1 = prism.getProb1();
		fairness = prism.getFairness();

		// Display warning and/or make changes for some option combinations
		boolean advGenNeeded = genStrat || (prism.getExportAdv() != Prism.EXPORT_ADV_NONE);
		if (advGenNeeded) {
			if (engine != Prism.SPARSE) {
				mainLog.println("Switching engine since only sparse engine currently supports this computation...");
				engine = Prism.SPARSE;
			}
			if (precomp && prob1) {
				mainLog.printWarning("Disabling Prob1 since this is needed for adversary generation");
				prob1 = false;
			}
		}

		// Pass some options onto native code.
		PrismNative.setCompact(prism.getCompact());
		PrismNative.setTermCrit(prism.getTermCrit());
		PrismNative.setTermCritParam(prism.getTermCritParam());
		PrismNative.setMaxIters(prism.getMaxIters());
		PrismNative.setSBMaxMem(prism.getSBMaxMem());
		PrismNative.setNumSBLevels(prism.getNumSBLevels());
		PrismNative.setExportAdv(prism.getExportAdv());
		PrismNative.setExportAdvFilename(prism.getExportAdvFilename());
	}

	// -----------------------------------------------------------------------------------
	// Check a property, i.e. an expression
	// -----------------------------------------------------------------------------------

	// Check expression (recursive)

	public StateValues checkExpression(Expression expr) throws PrismException
	{
		StateValues res;

		// P operator
		if (expr instanceof ExpressionProb) {
			res = checkExpressionProb((ExpressionProb) expr);
		}
		// R operator
		else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward((ExpressionReward) expr);
		}
		// Multi-objective
		else if (expr instanceof ExpressionFunc) {
			// Detect "multi" function
			if (((ExpressionFunc) expr).getName().equals("multi")) {
				res = checkExpressionMultiObjective((ExpressionFunc) expr);
			}
			// For any other function, check as normal
			else {
				res = super.checkExpression(expr);
			}
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(expr);
		}

		// Filter out non-reachable states from solution
		// (only necessary for symbolically stored vectors)
		if (res instanceof StateValuesMTBDD)
			res.filter(reach);

		return res;
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	// P operator

	protected StateValues checkExpressionProb(ExpressionProb expr) throws PrismException
	{
		Expression pb; // probability bound (expression)
		double p = 0; // probability bound (actual value)
		String relOp; // relational operator
		boolean min; // are we finding min (true) or max (false) probs

		JDDNode sol;
		StateValues probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateDouble(constantValues);
			if (p < 0 || p > 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}

		// Check for trivial (i.e. stupid) cases
		if (pb != null) {
			if ((p == 0 && relOp.equals(">=")) || (p == 1 && relOp.equals("<="))) {
				mainLog.printWarning("Checking for probability " + relOp + " " + p + " - formula trivially satisfies all states");
				JDD.Ref(reach);
				return new StateValuesMTBDD(reach, model);
			} else if ((p == 0 && relOp.equals("<")) || (p == 1 && relOp.equals(">"))) {
				mainLog.printWarning("Checking for probability " + relOp + " " + p + " - formula trivially satisfies no states");
				return new StateValuesMTBDD(JDD.Constant(0), model);
			}
		}

		// Determine whether min or max probabilities needed
		if (relOp.equals(">") || relOp.equals(">=") || relOp.equals("min=")) {
			// min
			min = true;
		} else if (relOp.equals("<") || relOp.equals("<=") || relOp.equals("max=")) {
			// max
			min = false;
		} else {
			throw new PrismException("Can't use \"P=?\" for MDPs; use \"Pmin=?\" or \"Pmax=?\"");
		}

		// Compute probabilities
		boolean qual = pb != null && ((p == 0) || (p == 1)) && precomp && prob0 && prob1;
		probs = checkProbPathFormula(expr.getExpression(), qual, min);

		// Print out probabilities
		if (verbose) {
			mainLog.print("\n" + (min ? "Minimum" : "Maximum") + " probabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}

		// For =? properties, just return values
		if (pb == null) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			sol = probs.getBDDFromInterval(relOp, p);
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
			// free vector
			probs.clear();
			return new StateValuesMTBDD(sol, model);
		}
	}

	// R operator

	protected StateValues checkExpressionReward(ExpressionReward expr) throws PrismException
	{
		Object rs; // reward struct index
		Expression rb; // reward bound (expression)
		double r = 0; // reward bound (actual value)
		String relOp; // relational operator
		boolean min; // are we finding min (true) or max (false) rewards
		Expression expr2; // expression

		JDDNode stateRewards = null, transRewards = null, sol;
		StateValues rewards = null;
		int i;

		// get info from reward operator
		rs = expr.getRewardStructIndex();
		relOp = expr.getRelOp();
		rb = expr.getReward();
		if (rb != null) {
			r = rb.evaluateDouble(constantValues);
			if (r < 0)
				throw new PrismException("Invalid reward bound " + r + " in R operator");
		}

		// get reward info
		if (model.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		if (rs == null) {
			stateRewards = model.getStateRewards(0);
			transRewards = model.getTransRewards(0);
		} else if (rs instanceof Expression) {
			i = ((Expression) rs).evaluateInt(constantValues);
			rs = new Integer(i); // for better error reporting below
			stateRewards = model.getStateRewards(i - 1);
			transRewards = model.getTransRewards(i - 1);
		} else if (rs instanceof String) {
			stateRewards = model.getStateRewards((String) rs);
			transRewards = model.getTransRewards((String) rs);
		}
		if (stateRewards == null || transRewards == null)
			throw new PrismException("Invalid reward structure index \"" + rs + "\"");

		// check for trivial (i.e. stupid) cases
		if (rb != null) {
			if (r == 0 && relOp.equals(">=")) {
				mainLog.printWarning("Checking for reward " + relOp + " " + r + " - formula trivially satisfies all states");
				JDD.Ref(reach);
				return new StateValuesMTBDD(reach, model);
			} else if (r == 0 && relOp.equals("<")) {
				mainLog.printWarning("Checking for reward " + relOp + " " + r + " - formula trivially satisfies no states");
				return new StateValuesMTBDD(JDD.Constant(0), model);
			}
		}

		// determine whether min or max rewards needed
		if (relOp.equals(">") || relOp.equals(">=") || relOp.equals("min=")) {
			// min
			min = true;
		} else if (relOp.equals("<") || relOp.equals("<=") || relOp.equals("max=")) {
			// max
			min = false;
		} else {
			throw new PrismException("Can't use \"R=?\" for MDPs; use \"Rmin=?\" or \"Rmax=?\"");
		}

		// compute rewards
		expr2 = expr.getExpression();
		if (expr2 instanceof ExpressionTemporal) {
			switch (((ExpressionTemporal) expr2).getOperator()) {
			case ExpressionTemporal.R_C:
				rewards = checkRewardCumul((ExpressionTemporal) expr2, stateRewards, transRewards, min);
				break;
			case ExpressionTemporal.R_I:
				rewards = checkRewardInst((ExpressionTemporal) expr2, stateRewards, transRewards, min);
				break;
			case ExpressionTemporal.R_F:
				rewards = checkRewardReach((ExpressionTemporal) expr2, stateRewards, transRewards, min);
				break;
			}
		}
		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		// print out rewards
		if (verbose) {
			mainLog.print("\n" + (min ? "Minimum" : "Maximum") + " rewards (non-zero only) for all states:\n");
			rewards.print(mainLog);
		}

		// For =? properties, just return values
		if (rb == null) {
			return rewards;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			sol = rewards.getBDDFromInterval(relOp, r);
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
			// free vector
			rewards.clear();
			return new StateValuesMTBDD(sol, model);
		}
	}

	//takes one operand of multi-objective expression and extracts operator, values and reward functions from it.
	protected void extractInfoFromMultiObjectiveOperand(Expression operand, OpsAndBoundsList opsAndBounds, List<JDDNode> rewardsIndex, List<String> targetName,
			List<Expression> targetExprs, boolean isFirst) throws PrismException
	{
		String relOp;
		int stepBound = 0;
		ExpressionProb exprProb = null;
		ExpressionReward exprReward = null;
		ExpressionTemporal exprTemp;
		// Only do P or R operators
		if (!(operand instanceof ExpressionProb || operand instanceof ExpressionReward))
			throw new PrismException("Multiple objectives must be P or R operators");
		//if (expr.getOperand(i) instanceof ExpressionReward && i > 0)
		//	throw new PrismException("R operator can only be used as first argument of multi function");
		if (operand instanceof ExpressionProb) {
			exprProb = (ExpressionProb) operand;
			exprReward = null;
		} else {
			exprReward = (ExpressionReward) operand;
			exprProb = null;
			Object rs = exprReward.getRewardStructIndex();
			JDDNode transRewards = null;
			JDDNode stateRewards = null;
			if (model.getNumRewardStructs() == 0)
				throw new PrismException("Model has no rewards specified");
			if (rs == null) {
				transRewards = model.getTransRewards(0);
				stateRewards = model.getStateRewards(0);
			} else if (rs instanceof Expression) {
				int j = ((Expression) rs).evaluateInt(constantValues);
				rs = new Integer(j); // for better error reporting below
				transRewards = model.getTransRewards(j - 1);
				stateRewards = model.getStateRewards(j - 1);
			} else if (rs instanceof String) {
				transRewards = model.getTransRewards((String) rs);
				stateRewards = model.getStateRewards((String) rs);	
			}

			//check if there are state rewards and display a warning
			if (stateRewards != null && !stateRewards.equals(JDD.ZERO))
				throw new PrismException("Multi-objective model checking does not support state rewards; please convert to transition rewards");
			if (transRewards == null)
				throw new PrismException("Invalid reward structure index \"" + rs + "\"");
			rewardsIndex.add(transRewards);
		}
		// Get the cumulative step bound for reward 
		if (exprReward != null) {
			exprTemp = (ExpressionTemporal) exprReward.getExpression();
			
			// We only allow C or C<=k reward operators, others such as F are not supported currently
			if (exprTemp.getOperator() != ExpressionTemporal.R_C) {
				throw new PrismException("Reward operators in multi-objective properties must be C or C<=k (" + exprTemp.getOperatorSymbol()
						+ " is not yet supported)");
			}
			
			if (exprTemp.getUpperBound() != null) {
				//This is the case of C<=k
				stepBound = exprTemp.getUpperBound().evaluateInt(constantValues);
			} else {
				//Here we just have C
				stepBound = -1;
			}
			/*if (exprTemp.getOperator() != ExpressionTemporal.R_F) {
				throw new PrismException("Multi-objective only supports F operator for rewards");
			}*/
		}
		if (exprProb != null) {// || ((ExpressionTemporal) exprReward.getExpression()).getOperator() != ExpressionTemporal.R_C) {
			exprTemp = (ExpressionTemporal) exprProb.getExpression();
			//TODO we currently ignore the lower bound
			if (exprTemp.getUpperBound() != null) {
				stepBound = exprTemp.getUpperBound().evaluateInt(constantValues);
			}
			else
				stepBound = -1;
		}

		// Get info from P/R operator
		// Store relational operator as an integer
		// Only do max=? or lower bounds
		relOp = (exprProb != null) ? exprProb.getRelOp() : exprReward.getRelOp();
		Operator op;
		if (relOp.equals("max=")) {
			op = (exprProb != null) ? Operator.P_MAX : Operator.R_MAX;
		} else if (relOp.equals(">")) // currently do not support
			//relOps.add(1);
			throw new PrismException("Multi-objective properties can not use strict inequalities on P/R operators");
		else if (relOp.equals(">=")) {
			op = (exprProb != null) ? Operator.P_GE : Operator.R_GE;
		} else if (relOp.equals("min=")) {
			op = (exprProb != null) ? Operator.P_MIN : Operator.R_MIN;
		} else if (relOp.equals("<")) // currently do not support
			//relOps.add(6);
			throw new PrismException("Multi-objective properties can not use strict inequalities on P/R operators");
		else if (relOp.equals("<=")) {
			op = (exprProb != null) ? Operator.P_LE : Operator.R_LE;
		} else
			throw new PrismException("Multi-objective properties can only contain P/R operators with max/min=? or lower/upper probability bounds");
		// Store probability/rewards bound
		Expression pb = (exprProb != null) ? exprProb.getProb() : exprReward.getReward();
		if (pb != null) {
			double p = pb.evaluateDouble(constantValues);
			if ((exprProb != null) && (p < 0 || p > 1))
				throw new PrismException("Invalid probability bound " + p + " in P operator");
			if ((exprProb == null) && (p < 0))
				throw new PrismException("Invalid reward bound " + p + " in P operator");
			if (exprProb != null && relOp.equals("<="))
				p = 1 - p;

			opsAndBounds.add(op, p, stepBound);
		} else {
			opsAndBounds.add(op, -1.0, stepBound);
		}

		// Now extract targets
		// Also store which ones are just reachability (in reachExpr)
		if (exprProb != null) {
			targetExprs.add(exprProb.getExpression());
			// TODO check if the following line is unneeded: it only kept track of probs vs rewards
			// reachExpr[i] = false;
			targetName.add(exprProb.getExpression().toString());
		} else {
			targetExprs.add(null);
			// TODO check if the following line is unneeded: it only kept track of probs vs rewards
			// reachExpr[i] = true;
			targetName.add("");
		}
	}

	//Prints info about the product model in multi-objective
	protected void outputProductMulti(NondetModel modelProduct) throws PrismException
	{
		// Print product info
		mainLog.println();
		modelProduct.printTransInfo(mainLog, prism.getExtraDDInfo());

		// Output product, if required
		if (prism.getExportProductTrans()) {
			try {
				mainLog.println("\nExporting product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"...");
				prism.exportTransToFile(modelProduct, true, Prism.EXPORT_PLAIN, new File(prism.getExportProductTransFilename()));
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"");
			}
		}
		if (prism.getExportProductStates()) {
			try {
				mainLog.println("\nExporting product state space to file \"" + prism.getExportProductStatesFilename() + "\"...");
				prism.exportStatesToFile(modelProduct, Prism.EXPORT_PLAIN, new File(prism.getExportProductStatesFilename()));
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export product state space to file \"" + prism.getExportProductStatesFilename() + "\"");
			}
		}
	}

	//computes accepting end component for the Rabin automaton dra.
	//Vojta: in addition to calling a method which does the computation
	//there are some other bits which I don't currently understand
	protected JDDNode computeAcceptingEndComponent(DRA<BitSet> dra, NondetModel modelProduct, JDDVars draDDRowVars, JDDVars draDDColVars,
			List<JDDNode> allecs, List<JDDNode> statesH, List<JDDNode> statesL, //Vojta: at the time of writing this I have no idea what these two parameters do, so I don't know how to call them
			LTLModelChecker mcLtl, boolean conflictformulaeGtOne, String name) throws PrismException
	{
		mainLog.println("\nFinding accepting end components for " + name + "...");
		long l = System.currentTimeMillis();
		// increase ref count for checking conflict formulas
		if (conflictformulaeGtOne) {
			for (JDDNode n : statesH)
				JDD.Ref(n);
			for (JDDNode n : statesL)
				JDD.Ref(n);
		}
		JDDNode ret = mcLtl.findMultiAcceptingStates(dra, modelProduct, draDDRowVars, draDDColVars, false, allecs, statesH, statesL);
		//targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false));
		l = System.currentTimeMillis() - l;
		mainLog.println("Time for end component identification: " + l / 1000.0 + " seconds.");
		return ret;
	}

	protected void removeNonZeroMecsForMax(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> rewardsIndex, OpsAndBoundsList opsAndBounds,
			int numTargets, DRA<BitSet> dra[], JDDVars draDDRowVars[], JDDVars draDDColVars[]) throws PrismException
	{
		List<JDDNode> mecs = mcLtl.findMECStates(modelProduct, modelProduct.getReach());
		JDDNode removedActions = JDD.Constant(0);
		JDDNode rmecs = JDD.Constant(0);
		boolean mecflags[] = new boolean[mecs.size()];
		for (int i = 0; i < rewardsIndex.size(); i++)
			if (opsAndBounds.getRewardOperator(i) == Operator.R_MAX || opsAndBounds.getRewardOperator(i) == Operator.R_GE) {
				JDD.Ref(rewardsIndex.get(i));
				JDDNode actions = JDD.GreaterThan(rewardsIndex.get(i), 0.0);
				if (!actions.equals(JDD.ZERO))
					for (int j = 0; j < mecs.size(); j++) {
						JDDNode mec = mecs.get(j);
						JDD.Ref(mec);
						JDDNode mecactions = mcLtl.maxStableSetTrans1(modelProduct, mec);
						JDD.Ref(actions);
						mecactions = JDD.And(actions, mecactions);
						if (!mecactions.equals(JDD.ZERO)) {
							JDD.Ref(mec);
							rmecs = JDD.Or(rmecs, mec);
						}
						removedActions = JDD.Or(removedActions, mecactions);
					}
				JDD.Deref(actions);
			}
		for (JDDNode mec : mecs)
			JDD.Deref(mec);
		// TODO: check if the model satisfies the LTL constraints 
		if (!rmecs.equals(JDD.ZERO)) {
			boolean constraintViolated = false;
			if (JDD.AreInterecting(modelProduct.getStart(), rmecs)) {
				constraintViolated = true;
				JDD.Deref(rmecs);
			} else {
				// find all action pointing to mecs from outside a
				JDD.Ref(rmecs);
				JDDNode rtarget = JDD.PermuteVariables(rmecs, modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars());
				JDD.Ref(modelProduct.getTrans01());
				rtarget = JDD.And(modelProduct.getTrans01(), rtarget);
				rtarget = JDD.And(rtarget, JDD.Not(rmecs));
				//rtarget = JDD.ThereExists(rtarget, modelProduct.getAllDDColVars());
				// find target states for LTL formulae
				Vector<JDDNode> tmptargetDDs = new Vector<JDDNode>();
				List<JDDNode> tmpmultitargetDDs = new ArrayList<JDDNode>();
				List<Integer> tmpmultitargetIDs = new ArrayList<Integer>();
				ArrayList<DRA<BitSet>> tmpdra = new ArrayList<DRA<BitSet>>();
				ArrayList<JDDVars> tmpdraDDRowVars = new ArrayList<JDDVars>();
				ArrayList<JDDVars> tmpdraDDColVars = new ArrayList<JDDVars>();
				int count = 0;
				for (int i = 0; i < numTargets; i++)
					if (opsAndBounds.isProbabilityObjective(i) && opsAndBounds.getOperator(i) != Operator.P_MAX
							&& opsAndBounds.getOperator(i) != Operator.P_MIN) {
						tmpdra.add(dra[i]);
						tmpdraDDRowVars.add(draDDRowVars[i]);
						tmpdraDDColVars.add(draDDColVars[i]);
						count++;
					}
				if (count > 0) {
					// TODO: distinguish whether rtarget is empty
					DRA newdra[] = new DRA[count];
					tmpdra.toArray(newdra);
					JDDVars newdraDDRowVars[] = new JDDVars[count];
					tmpdraDDRowVars.toArray(newdraDDRowVars);
					JDDVars newdraDDColVars[] = new JDDVars[count];
					tmpdraDDColVars.toArray(newdraDDColVars);

					findTargetStates(modelProduct, mcLtl, count, count, new boolean[count], newdra, newdraDDRowVars, newdraDDColVars, tmptargetDDs,
							tmpmultitargetDDs, tmpmultitargetIDs);

					OpsAndBoundsList tmpOpsAndBounds = new OpsAndBoundsList();

					for (int i = 0; i < opsAndBounds.probSize(); i++) {
						if (opsAndBounds.getProbOperator(i) != Operator.P_MAX) {
							tmpOpsAndBounds.add(opsAndBounds.getProbOperator(i),
									opsAndBounds.getProbBound(i),
									opsAndBounds.getProbStepBound(i));
						}
					}

					tmpOpsAndBounds.add(Operator.R_MAX, -1.0, -1);

					ArrayList<JDDNode> tmprewards = new ArrayList<JDDNode>(1);
					tmprewards.add(rtarget);
					double prob = (Double) computeMultiReachProbs(modelProduct, mcLtl, tmprewards, modelProduct.getStart(), tmptargetDDs, tmpmultitargetDDs,
							tmpmultitargetIDs, tmpOpsAndBounds, count > 1);
					if (prob > 0.0) { // LTL formulae can be satisfied
						constraintViolated = true;
					} else if (Double.isNaN(prob))
						throw new PrismException("The LTL formulae in multi-objective query cannot be satisfied!\n");
				} else {
					// end components with non-zero rewards can always be reached
					constraintViolated = true;
				}

				//JDD.Deref(rtarget);
				for (JDDNode tt : tmptargetDDs)
					JDD.Deref(tt);
				for (JDDNode tt : tmpmultitargetDDs)
					JDD.Deref(tt);
			}
			if (constraintViolated) {
				throw new PrismException("Cannot use multi-objective model checking with maximising objectives and non-zero reward end compoments");
			}

			JDD.Ref(removedActions);
			modelProduct.trans = JDD.Apply(JDD.TIMES, modelProduct.trans, JDD.Not(removedActions));
			modelProduct.trans01 = JDD.Apply(JDD.TIMES, modelProduct.trans01, JDD.Not(removedActions));
		} else {
			JDD.Deref(rmecs);
			JDD.Deref(removedActions);
		}

	}

	//TODO is conflictformulae actually just no of prob?
	protected void checkConflictsInObjectives(NondetModel modelProduct, LTLModelChecker mcLtl, int conflictformulae, int numTargets,
			OpsAndBoundsList opsAndBounds, DRA<BitSet> dra[], JDDVars draDDRowVars[], JDDVars draDDColVars[], List<JDDNode> targetDDs,
			List<ArrayList<JDDNode>> allstatesH, List<ArrayList<JDDNode>> allstatesL, List<JDDNode> multitargetDDs, List<Integer> multitargetIDs)
			throws PrismException
	{
		DRA[] tmpdra = new DRA[conflictformulae];
		JDDVars[] tmpdraDDRowVars = new JDDVars[conflictformulae];
		JDDVars[] tmpdraDDColVars = new JDDVars[conflictformulae];
		List<JDDNode> tmptargetDDs = new ArrayList<JDDNode>(conflictformulae);
		List<List<JDDNode>> tmpallstatesH = new ArrayList<List<JDDNode>>(conflictformulae);
		List<List<JDDNode>> tmpallstatesL = new ArrayList<List<JDDNode>>(conflictformulae);
		int count = 0;
		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				tmpdra[count] = dra[i];
				tmpdraDDRowVars[count] = draDDRowVars[i];
				tmpdraDDColVars[count] = draDDColVars[i];
				// tricky part; double check when reachExpr is set for efficiency
				tmptargetDDs.add(targetDDs.get(count));
				tmpallstatesH.add(allstatesH.get(i));
				tmpallstatesL.add(allstatesL.get(i));
				count++;
			}
		List<List<Integer>> tmpmultitargetIDs = new ArrayList<List<Integer>>();

		mcLtl.findMultiConflictAcceptingStates(tmpdra, modelProduct, tmpdraDDRowVars, tmpdraDDColVars, tmptargetDDs, tmpallstatesH, tmpallstatesL,
				multitargetDDs, tmpmultitargetIDs);
		// again. double check when reachExpr is set
		count = 0;
		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				targetDDs.remove(count);
				targetDDs.add(count, tmptargetDDs.get(count));
				count++;
			}

		// deal with reachability targets
		for (int i = 0; i < tmpmultitargetIDs.size(); i++) {
			multitargetIDs.add(changeToInteger(tmpmultitargetIDs.get(i)));
			//System.out.println("multitargetIDs["+i+"] = " + multitargetIDs.get(i));
		}

		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				List<JDDNode> tmpLH = allstatesH.get(i);
				for (JDDNode n : tmpLH)
					JDD.Deref(n);
				tmpLH = allstatesL.get(i);
				for (JDDNode n : tmpLH)
					JDD.Deref(n);
			}
	}

	/**
	 * This function throws an exception if the parameter expr contains
	 * a subexpression which is a time-bounded operator.
	 * @param expr A multi objective 
	 * @throws PrismException
	 */
	private void ensureNoTimeBounded(ExpressionFunc expr) throws PrismException
	{
		try {
			expr.accept(new ASTTraverse()
			{
				public void visitPre(ExpressionTemporal e) throws PrismLangException
				{
					if (e.getLowerBound() != null)
						throw new PrismLangException(e.getOperatorSymbol());
					if (e.getUpperBound() != null)
						throw new PrismLangException(e.getOperatorSymbol());
				}
			});
		} catch (PrismLangException e) {
			String s = "Temporal operators (like " + e.getMessage() + ")";
			s += " cannot have time bounds for multi-objective properties";
			throw new PrismException(s);
		}
	}

	protected void addDummyFormula(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> targetDDs, OpsAndBoundsList opsAndBounds)
			throws PrismException
	{
		List<JDDNode> tmpecs = mcLtl.findMECStates(modelProduct, modelProduct.getReach());
		JDDNode acceptingStates = JDD.Constant(0);
		for (JDDNode set : tmpecs)
			acceptingStates = JDD.Or(acceptingStates, set);
		targetDDs.add(acceptingStates);
		opsAndBounds.add(Operator.P_GE, 0.0, -1);
	}

	//TODO: dra's element is changed here, not neat.
	protected NondetModel constructDRAandProductMulti(NondetModel model, LTLModelChecker mcLtl, Expression ltl, int i, DRA<BitSet> dra[], Operator operator,
			Expression targetExpr, JDDVars draDDRowVars, JDDVars draDDColVars, JDDNode ddStateIndex) throws PrismException
	{
		// Model check maximal state formulas
		Vector<JDDNode> labelDDs = new Vector<JDDNode>();
		ltl = mcLtl.checkMaximalStateFormulas(this, model, targetExpr.deepCopy(), labelDDs);

		// Convert LTL formula to deterministic Rabin automaton (DRA)
		// For min probabilities, need to negate the formula
		// (add parentheses to allow re-parsing if required)
		if (Operator.isMinOrLe(operator))
			ltl = Expression.Not(Expression.Parenth(ltl));
		mainLog.println("\nBuilding deterministic Rabin automaton (for " + ltl + ")...");
		long l = System.currentTimeMillis();
		dra[i] = LTLModelChecker.convertLTLFormulaToDRA(ltl);
		mainLog.print("\nDRA has " + dra[i].size() + " states, " + ", " + dra[i].getNumAcceptancePairs() + " pairs.");
		//dra[i].print(System.out);
		l = System.currentTimeMillis() - l;
		mainLog.println("\nTime for Rabin translation: " + l / 1000.0 + " seconds.");

		// Build product of MDP and automaton
		mainLog.println("\nConstructing MDP-DRA product...");

		NondetModel modelNew = mcLtl.constructProductMDP(dra[i], model, labelDDs, draDDRowVars, draDDColVars, false, i == 0 ? ddStateIndex : model.getStart());

		modelNew.printTransInfo(mainLog, prism.getExtraDDInfo());
		// Deref label BDDs
		for (int j = 0; j < labelDDs.size(); j++) {
			JDD.Deref(labelDDs.get(j));
		}
		return modelNew;
	}

	protected List<JDDNode> computeAllEcs(NondetModel modelProduct, LTLModelChecker mcLtl, ArrayList<ArrayList<JDDNode>> allstatesH,
			ArrayList<ArrayList<JDDNode>> allstatesL, JDDNode acceptanceVector_H, JDDNode acceptanceVector_L, JDDVars draDDRowVars[], JDDVars draDDColVars[],
			OpsAndBoundsList opsAndBounds, int numTargets) throws PrismException
	{
		//use acceptanceVector_H and acceptanceVector_L to speed up scc computation
		JDD.Ref(acceptanceVector_H);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode candidateStates = JDD.Apply(JDD.TIMES, modelProduct.getTrans01(), acceptanceVector_H);
		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				acceptanceVector_H = JDD.PermuteVariables(acceptanceVector_H, draDDRowVars[i], draDDColVars[i]);
			}
		candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_H);
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDColVars());
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDNondetVars());
		// find all maximal end components
		List<JDDNode> allecs = mcLtl.findMECStates(modelProduct, candidateStates, acceptanceVector_L);
		JDD.Deref(candidateStates);
		JDD.Deref(acceptanceVector_L);
		return allecs;
	}

	/**
	 * 
	 * @param modelProduct
	 * @param rewardsIndex
	 * @param relOpsReward
	 * @return True if some transitions were removed
	 */
	protected boolean removeNonZeroRewardTrans(NondetModel modelProduct, List<JDDNode> rewardsIndex, OpsAndBoundsList opsAndBounds)
	{
		boolean transchanged = false;
		for (int i = 0; i < rewardsIndex.size(); i++)
			if (opsAndBounds.getRewardOperator(i) == Operator.R_MIN || opsAndBounds.getRewardOperator(i) == Operator.R_LE) {
				// Get non-zero reward actions
				JDD.Ref(rewardsIndex.get(i));
				JDDNode actions = JDD.GreaterThan(rewardsIndex.get(i), 0.0);
				if (!actions.equals(JDD.ZERO)) {
					//mainLog.println("Removing non-zero reward actions in reward #" + i);
					JDD.Ref(actions);
					if (!transchanged)
						JDD.Ref(modelProduct.getTrans());
					JDDNode tmp = JDD.ITE(actions, JDD.Constant(0), modelProduct.getTrans());
					modelProduct.trans = tmp;
					if (!transchanged)
						JDD.Ref(modelProduct.getTrans01());
					tmp = JDD.ITE(actions, JDD.Constant(0), modelProduct.getTrans01());
					modelProduct.trans01 = tmp;
					transchanged = true;
				}
			}
		return transchanged;
	}

	// Multi-objective
	protected StateValues checkExpressionMultiObjective(ExpressionFunc expr) throws PrismException
	{
		// Objective/target info
		int numTargets;
		List<Expression> targetExprs;
		List<JDDNode> targetDDs, multitargetDDs = null;
		List<Integer> multitargetIDs = null;
		OpsAndBoundsList opsAndBounds;

		// TODO this only stored prob vs reward objective anyway
		// boolean[] reachExpr; // whether target is just reachability (true) or LTL (false)
		// LTL/product model stuff
		NondetModel modelProduct, modelNew;
		JDDVars[] draDDRowVars, draDDColVars;
		LTLModelChecker mcLtl;
		Expression[] ltl;
		DRA<BitSet>[] dra;
		// State index info
		JDDNode ddStateIndex = null;
		// Misc
		boolean negateresult = false;
		int conflictformulae = 0;
		boolean hasMaxReward = false;
		//boolean hasLTLconstraint = false;

		// Make sure we are only expected to compute a value for a single state
		// Assuming yes, determine index of state of interest and build BDD
		if (currentFilter == null || !(currentFilter.getOperator() == Filter.FilterOperator.STATE))
			throw new PrismException("Multi-objective model checking can only compute values from a single state");
		else {
			int stateIndex = currentFilter.getStateIndex();
			ddStateIndex = ODDUtils.SingleIndexToDD(stateIndex, odd, allDDRowVars);
		}

		// Check format and extract bounds/etc.
		numTargets = expr.getNumOperands();
		opsAndBounds = new OpsAndBoundsList();
		List<JDDNode> rewards = new ArrayList<JDDNode>(numTargets);
		List<JDDNode> rewardsIndex = new ArrayList<JDDNode>(numTargets);

		// No time-bounded variants of the temporal operators allowed for multi-objective (or LTL)
		// TODO removed since it is allowed for valiter.
		// TODO make sure it is treated correctly for all params
		//ensureNoTimeBounded(expr);

		ArrayList<String> targetName = new ArrayList<String>();
		targetExprs = new ArrayList<Expression>(numTargets);
		for (int i = 0; i < numTargets; i++) {
			extractInfoFromMultiObjectiveOperand(expr.getOperand(i), opsAndBounds, rewardsIndex, targetName, targetExprs, i == 0);
		}

		//currently we do 1 numerical subject to booleans, or multiple numericals only 
		if (opsAndBounds.numberOfNumerical() > 1 &&
				opsAndBounds.numberOfNumerical() < opsAndBounds.probSize() + opsAndBounds.rewardSize()) {
			throw new PrismException("Multiple min/max queries cannot be combined with boolean queries.");
		}
		
		negateresult = opsAndBounds.contains(Operator.P_MIN);
		hasMaxReward = opsAndBounds.contains(Operator.R_GE) || opsAndBounds.contains(Operator.R_MAX);

		// Create array to store target DDs
		targetDDs = new ArrayList<JDDNode>(numTargets);

		// Multi-objective model checking

		// Create arrays to store LTL/DRA info
		ltl = new Expression[numTargets];
		dra = new DRA[numTargets];
		draDDRowVars = new JDDVars[numTargets];
		draDDColVars = new JDDVars[numTargets];

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(prism);

		// Product is initially just the original model (we build it recursively)
		modelProduct = model;

		// Go through probabilistic objectives and construct product MDP.
		boolean originalmodel = true;
		for (int i = 0; i < numTargets; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				draDDRowVars[i] = new JDDVars();
				draDDColVars[i] = new JDDVars();
				modelNew = constructDRAandProductMulti(modelProduct, mcLtl, ltl[i], i, dra, opsAndBounds.getOperator(i), targetExprs.get(i), draDDRowVars[i],
						draDDColVars[i], ddStateIndex);
				// Deref old product (unless is the original model)
				if (i > 0 & !originalmodel)
					modelProduct.clear();
				// Store new product
				modelProduct = modelNew;
				originalmodel = false;
			}
		}

		// TODO: move this above
		// Replace min by max and <= by >=
		opsAndBounds.makeAllProbUp();

		//print some info
		outputProductMulti(modelProduct);

		for (JDDNode rindex : rewardsIndex) {
			JDD.Ref(rindex);
			JDD.Ref(modelProduct.getTrans01());
			rewards.add(JDD.Apply(JDD.TIMES, rindex, modelProduct.getTrans01()));
		}

		// Removing actions with non-zero reward from the product for maximum cases
		if (hasMaxReward /*& hasLTLconstraint*/) {
			removeNonZeroMecsForMax(modelProduct, mcLtl, rewardsIndex, opsAndBounds, numTargets, dra, draDDRowVars, draDDColVars);
		}

		// Remove all non-zero reward from trans in order to search for zero reward end components
		JDDNode tmptrans = modelProduct.getTrans();
		JDDNode tmptrans01 = modelProduct.getTrans01();
		boolean transchanged = removeNonZeroRewardTrans(modelProduct, rewardsIndex, opsAndBounds);

		// Compute all maximal end components
		ArrayList<ArrayList<JDDNode>> allstatesH = new ArrayList<ArrayList<JDDNode>>(numTargets);
		ArrayList<ArrayList<JDDNode>> allstatesL = new ArrayList<ArrayList<JDDNode>>(numTargets);
		JDDNode acceptanceVector_H = JDD.Constant(0);
		JDDNode acceptanceVector_L = JDD.Constant(0);
		for (int i = 0; i < numTargets; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				ArrayList<JDDNode> statesH = new ArrayList<JDDNode>();
				ArrayList<JDDNode> statesL = new ArrayList<JDDNode>();
				for (int k = 0; k < dra[i].getNumAcceptancePairs(); k++) {
					JDDNode tmpH = JDD.Constant(0);
					JDDNode tmpL = JDD.Constant(0);
					for (int j = 0; j < dra[i].size(); j++) {
						if (!dra[i].getAcceptanceL(k).get(j)) {
							tmpH = JDD.SetVectorElement(tmpH, draDDRowVars[i], j, 1.0);
						}
						if (dra[i].getAcceptanceK(k).get(j)) {
							tmpL = JDD.SetVectorElement(tmpL, draDDRowVars[i], j, 1.0);
						}
					}
					statesH.add(tmpH);
					JDD.Ref(tmpH);
					acceptanceVector_H = JDD.Or(acceptanceVector_H, tmpH);
					statesL.add(tmpL);
					JDD.Ref(tmpL);
					acceptanceVector_L = JDD.Or(acceptanceVector_L, tmpL);
				}
				allstatesH.add(i, statesH);
				allstatesL.add(i, statesL);
			} else {
				allstatesH.add(i, null);
				allstatesL.add(i, null);
			}
		}

		// Find accepting maximum end components for each LTL formula
		List<JDDNode> allecs = computeAllEcs(modelProduct, mcLtl, allstatesH, allstatesL, acceptanceVector_H, acceptanceVector_L, draDDRowVars, draDDColVars,
				opsAndBounds, numTargets);

		for (int i = 0; i < numTargets; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				targetDDs.add(computeAcceptingEndComponent(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], allecs, allstatesH.get(i),
						allstatesL.get(i), mcLtl, conflictformulae > 1, targetName.get(i)));
			} else {
				// Fixme: maybe not efficient
				if (targetExprs.get(i) != null) {
					JDDNode dd = checkExpressionDD(targetExprs.get(i));
					JDD.Ref(modelProduct.getReach());
					dd = JDD.And(dd, modelProduct.getReach());
					targetDDs.add(dd);
				}
			}
		}

		// check if there are conflicts in objectives
		if (conflictformulae > 1) {
			multitargetDDs = new ArrayList<JDDNode>();
			multitargetIDs = new ArrayList<Integer>();
			checkConflictsInObjectives(modelProduct, mcLtl, conflictformulae, numTargets, opsAndBounds, dra, draDDRowVars, draDDColVars, targetDDs, allstatesH,
					allstatesL, multitargetDDs, multitargetIDs);
			Object o = multitargetDDs;
		}

		for (JDDNode ec : allecs)
			JDD.Deref(ec);

		//new StateListMTBDD(modelProduct.getReach(), modelProduct).print(mainLog);
		//try { prism.exportTransToFile(modelProduct, true, Prism.EXPORT_DOT_STATES, new java.io.File("product.dot")); } catch(Exception e) {}

		// Note: for multi-objective model checking, we construct the product MDP for only a single initial state
		// (unlike for normal LTL model checking) so it is safe to use modelProduct.getStart() here to pass in the initial states.

		// Add a dummy LTL formula to get generate target states when there is no LTL formula in the query
		// TODO most probably this is not needed for non-LP solution methods
		if (targetDDs.isEmpty() && prism.getMDPSolnMethod() == Prism.MDP_MULTI_LP) {
			addDummyFormula(modelProduct, mcLtl, targetDDs, opsAndBounds);
		}

		// Put unmodified trans and trans01 to modelProduct
		if (transchanged) {
			JDD.Deref(modelProduct.getTrans());
			JDD.Deref(modelProduct.getTrans01());
			modelProduct.trans = tmptrans;
			modelProduct.trans01 = tmptrans01;
		}

		Object value = computeMultiReachProbs(modelProduct, mcLtl, rewards, modelProduct.getStart(), targetDDs, multitargetDDs, multitargetIDs, opsAndBounds,
				conflictformulae > 1);
		
		// Deref, clean up
		if (ddStateIndex != null)
			JDD.Deref(ddStateIndex);
		if (modelProduct != null && modelProduct != model)
			modelProduct.clear();
		for (int i = 0; i < numTargets; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				draDDRowVars[i].derefAll();
				draDDColVars[i].derefAll();
			}
		}
		for (JDDNode t : targetDDs)
			JDD.Deref(t);
		if (multitargetDDs != null)
			for (JDDNode t : multitargetDDs)
				JDD.Deref(t);

		if (value instanceof TileList) {
			if (opsAndBounds.numberOfNumerical() == 2) {			
				synchronized(TileList.getStoredTileLists()) {
					//in multi-obj result probs go first, so we have to swap order if needed
					if (expr.getOperand(0) instanceof ExpressionReward && expr.getOperand(1) instanceof ExpressionProb) {
						TileList.storedFormulasX.add(expr.getOperand(1));
						TileList.storedFormulasY.add(expr.getOperand(0));
					} else {
						TileList.storedFormulasX.add(expr.getOperand(0));
						TileList.storedFormulasY.add(expr.getOperand(1));
					}
					TileList.storedFormulas.add(expr);
					TileList.storedTileLists.add((TileList) value);
				}
			} //else, i.e. in 3D, the output was treated in the algorithm itself.

			return new StateValuesVoid(value);
		}
		else if (value instanceof Double) {
			// Return result. Note: we only compute the value for a single state.
			if (negateresult) {
				value = 1 - (Double) value;
			}
	
			return new StateValuesMTBDD(JDD.Constant((Double) value), model);
		}
		else throw new PrismException("Do not know how to treat the returned value " + value);
			
	}

	protected void findTargetStates(NondetModel modelProduct, LTLModelChecker mcLtl, int numTargets, int conflictformulae, boolean reachExpr[], DRA dra[],
			JDDVars draDDRowVars[], JDDVars draDDColVars[], List<JDDNode> targetDDs, List<JDDNode> multitargetDDs, List<Integer> multitargetIDs)
			throws PrismException
	{
		int i, j;
		long l;

		// Compute all maximal end components
		ArrayList<ArrayList<JDDNode>> allstatesH = new ArrayList<ArrayList<JDDNode>>(numTargets);
		ArrayList<ArrayList<JDDNode>> allstatesL = new ArrayList<ArrayList<JDDNode>>(numTargets);
		JDDNode acceptanceVector_H = JDD.Constant(0);
		JDDNode acceptanceVector_L = JDD.Constant(0);
		for (i = 0; i < numTargets; i++) {
			if (!reachExpr[i]) {
				ArrayList<JDDNode> statesH = new ArrayList<JDDNode>();
				ArrayList<JDDNode> statesL = new ArrayList<JDDNode>();
				for (int k = 0; k < dra[i].getNumAcceptancePairs(); k++) {
					JDDNode tmpH = JDD.Constant(0);
					JDDNode tmpL = JDD.Constant(0);
					for (j = 0; j < dra[i].size(); j++) {
						if (!dra[i].getAcceptanceL(k).get(j)) {
							tmpH = JDD.SetVectorElement(tmpH, draDDRowVars[i], j, 1.0);
						}
						if (dra[i].getAcceptanceK(k).get(j)) {
							tmpL = JDD.SetVectorElement(tmpL, draDDRowVars[i], j, 1.0);
						}
					}
					statesH.add(tmpH);
					JDD.Ref(tmpH);
					acceptanceVector_H = JDD.Or(acceptanceVector_H, tmpH);
					statesL.add(tmpL);
					JDD.Ref(tmpL);
					acceptanceVector_L = JDD.Or(acceptanceVector_L, tmpL);
				}
				allstatesH.add(i, statesH);
				allstatesL.add(i, statesL);
			} else {
				allstatesH.add(i, null);
				allstatesL.add(i, null);
			}
		}

		// Find accepting maximum end components for each LTL formula
		List<JDDNode> allecs = null;
		//use acceptanceVector_H and acceptanceVector_L to speed up scc computation
		/*// check number of states in each scc
		allecs = mcLtl.findEndComponents(modelProduct, modelProduct.getReach());
		StateListMTBDD vl;
		int totalNum = 0;
		for(JDDNode set : allecs) {
			vl = new StateListMTBDD(set, modelProduct);
			totalNum += vl.size() - 1;
		}
		mainLog.println("Total number of states can be saved: " + totalNum);*/

		JDD.Ref(acceptanceVector_H);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode candidateStates = JDD.Apply(JDD.TIMES, modelProduct.getTrans01(), acceptanceVector_H);
		for (i = 0; i < numTargets; i++)
			if (!reachExpr[i]) {
				acceptanceVector_H = JDD.PermuteVariables(acceptanceVector_H, draDDRowVars[i], draDDColVars[i]);
			}
		candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_H);
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDColVars());
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDNondetVars());
		// find all maximal end components
		allecs = mcLtl.findMECStates(modelProduct, candidateStates, acceptanceVector_L);
		JDD.Deref(candidateStates);
		JDD.Deref(acceptanceVector_L);

		for (i = 0; i < numTargets; i++) {
			if (!reachExpr[i]) {
				//mainLog.println("\nFinding accepting end components for " + targetName[i] + "...");
				l = System.currentTimeMillis();
				// increase ref count for checking conflict formulas
				if (conflictformulae > 1) {
					List<JDDNode> tmpLH = allstatesH.get(i);
					for (JDDNode n : tmpLH)
						JDD.Ref(n);
					tmpLH = allstatesL.get(i);
					for (JDDNode n : tmpLH)
						JDD.Ref(n);
				}
				targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false, allecs, allstatesH.get(i),
						allstatesL.get(i)));
				//targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false));
				l = System.currentTimeMillis() - l;
				mainLog.println("Time for end component identification: " + l / 1000.0 + " seconds.");
			} /*else {
				// Fixme: maybe not efficient
				if(targetExprs.get(i) != null) {
					dd = checkExpressionDD(targetExprs.get(i));
					JDD.Ref(modelProduct.getReach());
					dd = JDD.And(dd, modelProduct.getReach());
					targetDDs.add(dd);
				}
				}*/
			//mainLog.print("\n    number of targets = " + JDD.GetNumMintermsString(targetDDs.get(i), modelProduct.getAllDDRowVars().n()));
			//if(i>0)
			//	mainLog.print("\n  total number of targets = " + 
			//			JDD.GetNumMintermsString(JDD.Or(targetDDs.get(i), targetDDs.get(i-1)), modelProduct.getAllDDRowVars().n()));
		}

		// check if there are conflicts in objectives
		if (conflictformulae > 1) {
			DRA[] tmpdra = new DRA[conflictformulae];
			JDDVars[] tmpdraDDRowVars = new JDDVars[conflictformulae];
			JDDVars[] tmpdraDDColVars = new JDDVars[conflictformulae];
			List<JDDNode> tmptargetDDs = new ArrayList<JDDNode>(conflictformulae);
			List<List<JDDNode>> tmpallstatesH = new ArrayList<List<JDDNode>>(conflictformulae);
			List<List<JDDNode>> tmpallstatesL = new ArrayList<List<JDDNode>>(conflictformulae);
			int count = 0;
			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					tmpdra[count] = dra[i];
					tmpdraDDRowVars[count] = draDDRowVars[i];
					tmpdraDDColVars[count] = draDDColVars[i];
					// tricky part; double check when reachExpr is set for efficiency
					tmptargetDDs.add(targetDDs.get(count));
					tmpallstatesH.add(allstatesH.get(i));
					tmpallstatesL.add(allstatesL.get(i));
					count++;
				}
			//multitargetDDs = new ArrayList<JDDNode>();
			List<List<Integer>> tmpmultitargetIDs = new ArrayList<List<Integer>>();

			mcLtl.findMultiConflictAcceptingStates(tmpdra, modelProduct, tmpdraDDRowVars, tmpdraDDColVars, tmptargetDDs, tmpallstatesH, tmpallstatesL,
					multitargetDDs, tmpmultitargetIDs);
			// again. double check when reachExpr is set
			count = 0;
			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					targetDDs.remove(count);
					targetDDs.add(count, tmptargetDDs.get(count));
					count++;
				}

			// deal with reachability targets
			//multitargetIDs = new ArrayList<Integer>(tmpmultitargetIDs.size());
			for (i = 0; i < tmpmultitargetIDs.size(); i++) {
				multitargetIDs.add(changeToInteger(tmpmultitargetIDs.get(i)));
			}

			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					List<JDDNode> tmpLH = allstatesH.get(i);
					for (JDDNode n : tmpLH)
						JDD.Deref(n);
					tmpLH = allstatesL.get(i);
					for (JDDNode n : tmpLH)
						JDD.Deref(n);
				}
		}

		for (JDDNode ec : allecs)
			JDD.Deref(ec);
	}

	// Model checking functions
	
	/**
	 * Compute probabilities for the contents of a P operator.
	 */
	protected StateValues checkProbPathFormula(Expression expr, boolean qual, boolean min) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and then pass control to appropriate method.
		if (expr.isSimplePathFormula()) {
			return checkProbPathFormulaSimple(expr, qual, min);
		} else {
			return checkProbPathFormulaLTL(expr, qual, min);
		}
	}

	/**
	 * Compute probabilities for a simple, non-LTL path operator.
	 */
	protected StateValues checkProbPathFormulaSimple(Expression expr, boolean qual, boolean min) throws PrismException
	{
		StateValues probs = null;

		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				probs = checkProbPathFormulaSimple(exprUnary.getOperand(), qual, min);
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Flip min/max, then subtract from 1
				probs = checkProbPathFormulaSimple(exprUnary.getOperand(), qual, !min);
				probs.subtractFromOne();
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(exprTemp, min);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(exprTemp, min);
				} else {
					probs = checkProbUntil(exprTemp, qual, min);
				}
			}
			// Anything else - convert to until and recurse
			else {
				probs = checkProbPathFormulaSimple(exprTemp.convertToUntilForm(), qual, min);
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		return probs;
	}

	/**
	 * Compute probabilities for a next operator.
	 */
	protected StateValues checkProbNext(ExpressionTemporal expr, boolean min) throws PrismException
	{
		JDDNode b;
		StateValues probs = null;

		// model check operand first
		b = checkExpressionDD(expr.getOperand2());

		// print out some info about num states
		// mainLog.print("\nb = " + JDD.GetNumMintermsString(b,
		// allDDRowVars.n()) + " states\n");

		// compute probabilities
		probs = computeNextProbs(trans, b, min);

		// derefs
		JDD.Deref(b);

		return probs;
	}

	/**
	 * Compute probabilities for a bounded until operator.
	 */
	protected StateValues checkProbBoundedUntil(ExpressionTemporal expr, boolean min) throws PrismException
	{
		int time;
		JDDNode b1, b2;
		StateValues probs = null;

		// get info from bounded until
		time = expr.getUpperBound().evaluateInt(constantValues);
		if (expr.upperBoundIsStrict())
			time--;
		if (time < 0) {
			String bound = expr.upperBoundIsStrict() ? "<" + (time + 1) : "<=" + time;
			throw new PrismException("Invalid bound " + bound + " in bounded until formula");
		}

		// model check operands first
		b1 = checkExpressionDD(expr.getOperand1());
		try {
			b2 = checkExpressionDD(expr.getOperand2());
		} catch (PrismException e) {
			JDD.Deref(b1);
			throw e;
		}

		// print out some info about num states
		// mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1,
		// allDDRowVars.n()));
		// mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2,
		// allDDRowVars.n()) + " states\n");

		// compute probabilities

		// a trivial case: "U<=0"
		if (time == 0) {
			// prob is 1 in b2 states, 0 otherwise
			JDD.Ref(b2);
			probs = new StateValuesMTBDD(b2, model);
		} else {
			try {
				probs = computeBoundedUntilProbs(trans, trans01, b1, b2, time, min);
			} catch (PrismException e) {
				JDD.Deref(b1);
				JDD.Deref(b2);
				throw e;
			}
		}

		// derefs
		JDD.Deref(b1);
		JDD.Deref(b2);

		return probs;
	}

	/**
	 * Compute probabilities for an (unbounded) until operator.
	 * Note: This method is split into two steps so that the LTL model checker can use the second part directly.
	 */
	protected StateValues checkProbUntil(ExpressionTemporal expr, boolean qual, boolean min) throws PrismException
	{
		JDDNode b1, b2;
		StateValues probs = null;

		// model check operands first
		b1 = checkExpressionDD(expr.getOperand1());
		try {
			b2 = checkExpressionDD(expr.getOperand2());
		} catch (PrismException e) {
			JDD.Deref(b1);
			throw e;
		}

		// print out some info about num states
		// mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1,
		// allDDRowVars.n()));
		// mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2,
		// allDDRowVars.n()) + " states\n");

		try {
			probs = checkProbUntil(b1, b2, qual, min);
		} catch (PrismException e) {
			JDD.Deref(b1);
			JDD.Deref(b2);
			throw e;
		}

		// derefs
		JDD.Deref(b1);
		JDD.Deref(b2);

		return probs;
	}

	/**
	 * Compute probabilities for an (unbounded) until operator.
	 * @param b1 Remain in these states
	 * @param b2 Target states
	 * @param qual True if only qualititative (0/1) results are needed
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	protected StateValues checkProbUntil(JDDNode b1, JDDNode b2, boolean qual, boolean min) throws PrismException
	{
		JDDNode splus, newb1, newb2;
		StateValues probs = null;
		long l;

		// compute probabilities

		// if doing min with fairness, we solve a different problem
		// (as in christel's habilitation)
		if (min && fairness) {

			// print out reminder that we have to do conversion for fairness
			mainLog.print("\nDoing conversion for fairness...\n");
			// start timer
			l = System.currentTimeMillis();
			// convert to new problem
			newb2 = PrismMTBDD.Prob0A(trans01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
			JDD.Ref(newb2);
			splus = JDD.Not(newb2);
			JDD.Ref(splus);
			JDD.Ref(b2);
			newb1 = JDD.And(splus, JDD.Not(b2));
			JDD.Deref(splus);
			JDD.Ref(reach);
			newb1 = JDD.And(reach, newb1);
			JDD.Ref(reach);
			newb2 = JDD.And(reach, newb2);
			// stop timer
			l = System.currentTimeMillis() - l;
			// print out conversion info
			mainLog.print("\nTime for fairness conversion: " + l / 1000.0 + " seconds.\n");
			// mainLog.print("\nb1 = " + JDD.GetNumMintermsString(newb1, allDDRowVars.n()));
			// mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(newb2, allDDRowVars.n()) + " states\n");
		} else {
			JDD.Ref(b1);
			newb1 = b1;
			JDD.Ref(b2);
			newb2 = b2;
		}

		// if requested (i.e. when prob bound is 0 or 1 and precomputation algorithms are enabled),
		// compute probabilities qualitatively
		if (qual) {
			mainLog.print("\nProbability bound in formula is 0/1 so not computing exact probabilities...\n");
			// for fairness, we compute max here
			probs = computeUntilProbsQual(trans01, newb1, newb2, min && !fairness);
		}
		// otherwise actually compute probabilities
		else {
			// for fairness, we compute max here
			try {
				probs = computeUntilProbs(trans, transActions, trans01, newb1, newb2, min && !fairness);
			} catch (PrismException e) {
				JDD.Deref(newb1);
				JDD.Deref(newb2);
				throw e;
			}
		}

		// if we're doing min with fairness,
		// we need to subtract the probabilities from
		// one to get the actual answer
		if (min && fairness) {
			probs.subtractFromOne();
		}

		// Derefs
		JDD.Deref(newb1);
		JDD.Deref(newb2);

		return probs;
	}

	/**
	 * Compute probabilities for an LTL path formula
	 */
	protected StateValues checkProbPathFormulaLTL(Expression expr, boolean qual, boolean min) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct = null, probs = null;
		Expression ltl;
		Vector<JDDNode> labelDDs;
		DRA<BitSet> dra;
		NondetModel modelProduct;
		NondetModelChecker mcProduct;
		JDDNode startMask;
		JDDVars draDDRowVars, draDDColVars;
		int i;
		long l;

		// Can't do LTL with time-bounded variants of the temporal operators
		try {
			expr.accept(new ASTTraverse()
			{
				public void visitPre(ExpressionTemporal e) throws PrismLangException
				{
					if (e.getLowerBound() != null)
						throw new PrismLangException(e.getOperatorSymbol());
					if (e.getUpperBound() != null)
						throw new PrismLangException(e.getOperatorSymbol());
				}
			});
		} catch (PrismLangException e) {
			String s = "Temporal operators (like " + e.getMessage() + ")";
			s += " cannot have time bounds for LTL properties";
			throw new PrismException(s);
		}

		// Can't do "dfa" properties yet
		if (expr instanceof ExpressionFunc && ((ExpressionFunc) expr).getName().equals("dfa")) {
			throw new PrismException("Model checking for \"dfa\" specifications not supported yet");
		}

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(prism);

		// Model check maximal state formulas
		labelDDs = new Vector<JDDNode>();
		ltl = mcLtl.checkMaximalStateFormulas(this, model, expr.deepCopy(), labelDDs);

		// Convert LTL formula to deterministic Rabin automaton (DRA)
		// For min probabilities, need to negate the formula
		// (But check fairness setting since this may affect min/max)
		// (add parentheses to allow re-parsing if required)
		if (min && !fairness) {
			ltl = Expression.Not(Expression.Parenth(ltl));
		}
		mainLog.println("\nBuilding deterministic Rabin automaton (for " + ltl + ")...");
		l = System.currentTimeMillis();
		dra = LTLModelChecker.convertLTLFormulaToDRA(ltl);
		mainLog.println("\nDRA has " + dra.size() + " states, " + dra.getNumAcceptancePairs() + " pairs.");
		/*try {
			mainLog.print(dra);
			dra.printDot(new java.io.PrintStream("dra.dot"));
		} catch(Exception e) {}*/
		l = System.currentTimeMillis() - l;
		mainLog.println("\nTime for Rabin translation: " + l / 1000.0 + " seconds.");

		// Build product of MDP and automaton
		mainLog.println("\nConstructing MDP-DRA product...");
		draDDRowVars = new JDDVars();
		draDDColVars = new JDDVars();
		modelProduct = mcLtl.constructProductMDP(dra, model, labelDDs, draDDRowVars, draDDColVars);
		mainLog.println();
		modelProduct.printTransInfo(mainLog, prism.getExtraDDInfo());
		// Output product, if required
		if (prism.getExportProductTrans()) {
			try {
				mainLog.println("\nExporting product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"...");
				modelProduct.exportToFile(Prism.EXPORT_PLAIN, true, new File(prism.getExportProductTransFilename()));
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"");
			}
		}
		if (prism.getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + prism.getExportProductStatesFilename() + "\"...");
			modelProduct.exportStates(Prism.EXPORT_PLAIN, new PrismFileLog(prism.getExportProductStatesFilename()));
		}

		// Find accepting MECs + compute reachability probabilities
		mainLog.println("\nFinding accepting end components...");
		JDDNode acc = mcLtl.findAcceptingECStatesForRabin(dra, modelProduct, draDDRowVars, draDDColVars, fairness);
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new NondetModelChecker(prism, modelProduct, null);
		probsProduct = mcProduct.checkProbUntil(modelProduct.getReach(), acc, qual, min && fairness);

		// subtract from 1 if we're model checking a negated formula for regular Pmin
		if (min && !fairness) {
			probsProduct.subtractFromOne();
		}

		// Convert probability vector to original model
		// First, filter over DRA start states
		startMask = mcLtl.buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(model.getReach());
		startMask = JDD.And(model.getReach(), startMask);
		probsProduct.filter(startMask);
		// Then sum over DD vars for the DRA state
		probs = probsProduct.sumOverDDVars(draDDRowVars, model);

		// Deref, clean up
		probsProduct.clear();
		modelProduct.clear();
		for (i = 0; i < labelDDs.size(); i++) {
			JDD.Deref(labelDDs.get(i));
		}
		JDD.Deref(acc);
		JDD.Deref(startMask);
		draDDRowVars.derefAll();
		draDDColVars.derefAll();

		return probs;
	}

	/**
	 * Compute rewards for a cumulative reward operator.
	 */
	protected StateValues checkRewardCumul(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min) throws PrismException
	{
		int time; // time
		StateValues rewards = null;

		// check that there is an upper time bound
		if (expr.getUpperBound() == null) {
			throw new PrismException("Cumulative reward operator without time bound (C) is only allowed for multi-objective queries");
		}

		// get info from inst reward
		time = expr.getUpperBound().evaluateInt(constantValues);
		if (time < 0) {
			throw new PrismException("Invalid time bound " + time + " in cumulative reward formula");
		}

		// a trivial case: "<=0"
		if (time == 0) {
			rewards = new StateValuesMTBDD(JDD.Constant(0), model);
		} else {
			// compute rewards
			try {
				rewards = computeCumulRewards(trans, stateRewards, transRewards, time, min);
			} catch (PrismException e) {
				throw e;
			}
		}

		return rewards;
	}

	/**
	 * Compute rewards for an instantaneous reward operator.
	 */
	protected StateValues checkRewardInst(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min) throws PrismException
	{
		int time; // time bound
		StateValues rewards = null;

		// get info from bounded until
		time = expr.getUpperBound().evaluateInt(constantValues);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in instantaneous reward property");
		}

		// compute rewards
		rewards = computeInstRewards(trans, stateRewards, time, min);

		return rewards;
	}

	/**
	 * Compute rewards for a reachability reward operator.
	 */
	protected StateValues checkRewardReach(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min) throws PrismException
	{
		JDDNode b;
		StateValues rewards = null;

		// model check operand first
		b = checkExpressionDD(expr.getOperand2());

		// print out some info about num states
		// mainLog.print("\nb = " + JDD.GetNumMintermsString(b,
		// allDDRowVars.n()) + " states\n");

		// compute rewards
		try {
			rewards = computeReachRewards(trans, transActions, trans01, stateRewards, transRewards, b, min);
		} catch (PrismException e) {
			JDD.Deref(b);
			throw e;
		}

		// derefs
		JDD.Deref(b);

		return rewards;
	}

	// -----------------------------------------------------------------------------------
	// probability computation methods
	// -----------------------------------------------------------------------------------

	// compute probabilities for next

	protected StateValues computeNextProbs(JDDNode tr, JDDNode b, boolean min)
	{
		JDDNode tmp;
		StateValues probs = null;

		// matrix multiply: trans * b
		JDD.Ref(b);
		tmp = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		JDD.Ref(tr);
		tmp = JDD.MatrixMultiply(tr, tmp, allDDColVars, JDD.BOULDER);
		// (then min or max)
		if (min) {
			// min
			JDD.Ref(nondetMask);
			tmp = JDD.Apply(JDD.MAX, tmp, nondetMask);
			tmp = JDD.MinAbstract(tmp, allDDNondetVars);
		} else {
			// max
			tmp = JDD.MaxAbstract(tmp, allDDNondetVars);
		}
		probs = new StateValuesMTBDD(tmp, model);

		return probs;
	}

	// compute probabilities for bounded until

	protected StateValues computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2, int time, boolean min) throws PrismException
	{
		JDDNode yes, no, maybe;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateValues probs = null;

		// compute yes/no/maybe states
		if (b2.equals(JDD.ZERO)) {
			yes = JDD.Constant(0);
			JDD.Ref(reach);
			no = reach;
			maybe = JDD.Constant(0);
		} else if (b1.equals(JDD.ZERO)) {
			JDD.Ref(b2);
			yes = b2;
			JDD.Ref(reach);
			JDD.Ref(b2);
			no = JDD.And(reach, JDD.Not(b2));
			maybe = JDD.Constant(0);
		} else {
			// yes
			JDD.Ref(b2);
			yes = b2;
			// no
			if (yes.equals(reach)) {
				no = JDD.Constant(0);
			} else if (precomp && prob0) {
				if (min) {
					// "min prob = 0" equates to "there exists a prob 0"
					no = PrismMTBDD.Prob0E(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b1, yes);
				} else {
					// "max prob = 0" equates to "all probs 0"
					no = PrismMTBDD.Prob0A(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, yes);
				}
			} else {
				JDD.Ref(reach);
				JDD.Ref(b1);
				JDD.Ref(b2);
				no = JDD.And(reach, JDD.Not(JDD.Or(b1, b2)));
			}
			// maybe
			JDD.Ref(reach);
			JDD.Ref(yes);
			JDD.Ref(no);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(yes, no)));
		}

		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, allDDRowVars.n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");

		// if maybe is empty, we have the probabilities already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(yes);
			probs = new StateValuesMTBDD(yes, model);
		}
		// otherwise explicitly compute the remaining probabilities
		else {
			// compute probabilities
			mainLog.println("\nComputing probabilities...");
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.NondetBoundedUntil(tr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, time, min);
					probs = new StateValuesMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.NondetBoundedUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, time, min);
					probs = new StateValuesDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.NondetBoundedUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, time, min);
					probs = new StateValuesDV(probsDV, model);
					break;
				default:
					throw new PrismException("Unknown engine");
				}
			} catch (PrismException e) {
				JDD.Deref(yes);
				JDD.Deref(no);
				JDD.Deref(maybe);
				throw e;
			}
		}

		// derefs
		JDD.Deref(yes);
		JDD.Deref(no);
		JDD.Deref(maybe);

		return probs;
	}

	// compute probabilities for until (for qualitative properties)

	// note: this function doesn't need to know anything about fairness
	// it is just told whether to compute min or max probabilities

	protected StateValues computeUntilProbsQual(JDDNode tr01, JDDNode b1, JDDNode b2, boolean min)
	{
		JDDNode yes = null, no = null, maybe;
		StateValues probs = null;

		// note: we know precomputation is enabled else this function wouldn't
		// have been called

		// compute yes/no/maybe states
		if (b2.equals(JDD.ZERO)) {
			yes = JDD.Constant(0);
			JDD.Ref(reach);
			no = reach;
			maybe = JDD.Constant(0);
		} else if (b1.equals(JDD.ZERO)) {
			JDD.Ref(b2);
			yes = b2;
			JDD.Ref(reach);
			JDD.Ref(b2);
			no = JDD.And(reach, JDD.Not(b2));
			maybe = JDD.Constant(0);
		} else {
			// min
			if (min) {
				// no: "min prob = 0" equates to "there exists an adversary prob equals 0"
				no = PrismMTBDD.Prob0E(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
				// yes: "min prob = 1" equates to "for all adversaries prob equals 1"
				yes = PrismMTBDD.Prob1A(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, no, b2);
			}
			// max
			else {
				// no: "max prob = 0" equates to "for all adversaries prob equals 0"
				no = PrismMTBDD.Prob0A(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
				// yes: "max prob = 1" equates to "there exists an adversary prob equals 1"
				yes = PrismMTBDD.Prob1E(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2, no);
			}
			// maybe
			JDD.Ref(reach);
			JDD.Ref(yes);
			JDD.Ref(no);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(yes, no)));
		}

		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, allDDRowVars.n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");

		// if maybe is empty, we have the answer already...
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(yes);
			probs = new StateValuesMTBDD(yes, model);
		}
		// otherwise we set the probabilities for maybe states to be 0.5
		// (actual probabilities for these states are unknown but definitely >0
		// and <1)
		// (this is safe because the results of this function will only be used
		// to compare against 0/1 bounds)
		// (this is not entirely elegant but is simpler and less error prone
		// than
		// trying to work out whether to use 0/1 for all case of min/max,
		// fairness, future/global, etc.)
		else {
			JDD.Ref(yes);
			JDD.Ref(maybe);
			probs = new StateValuesMTBDD(JDD.Apply(JDD.PLUS, yes, JDD.Apply(JDD.TIMES, maybe, JDD.Constant(0.5))), model);
		}

		// derefs
		JDD.Deref(yes);
		JDD.Deref(no);
		JDD.Deref(maybe);

		return probs;
	}

	// compute probabilities for until (general case)

	// note: this function doesn't need to know anything about fairness
	// it is just told whether to compute min or max probabilities

	protected StateValues computeUntilProbs(JDDNode tr, JDDNode tra, JDDNode tr01, JDDNode b1, JDDNode b2, boolean min) throws PrismException
	{
		JDDNode yes, no, maybe;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateValues probs = null;

		// If required, export info about target states 
		if (prism.getExportTarget()) {
			JDDNode labels[] = { model.getStart(), b2 };
			String labelNames[] = { "init", "target" };
			try {
				mainLog.println("\nExporting target states info to file \"" + prism.getExportTargetFilename() + "\"...");
				PrismMTBDD.ExportLabels(labels, labelNames, "l", model.getAllDDRowVars(), model.getODD(), Prism.EXPORT_PLAIN, prism.getExportTargetFilename());
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export target to file \"" + prism.getExportTargetFilename() + "\"");
			}
		}

		// compute yes/no/maybe states
		if (b2.equals(JDD.ZERO)) {
			yes = JDD.Constant(0);
			JDD.Ref(reach);
			no = reach;
			maybe = JDD.Constant(0);
		} else if (b1.equals(JDD.ZERO)) {
			JDD.Ref(b2);
			yes = b2;
			JDD.Ref(reach);
			JDD.Ref(b2);
			no = JDD.And(reach, JDD.Not(b2));
			maybe = JDD.Constant(0);
		} else {
			// no
			// if precomputation enabled
			// (nb: prob1 needs prob0)
			if (precomp && (prob0 || prob1)) {
				// min
				if (min) {
					// no: "min prob = 0" equates to "there exists an adversary prob equals 0"
					no = PrismMTBDD.Prob0E(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
				}
				// max
				else {
					// no: "max prob = 0" equates to "for all adversaries prob equals 0"
					no = PrismMTBDD.Prob0A(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
				}
			}
			// if precomputation not enabled
			else {
				// no
				JDD.Ref(reach);
				JDD.Ref(b1);
				JDD.Ref(b2);
				no = JDD.And(reach, JDD.Not(JDD.Or(b1, b2)));
			}
			// yes
			// if precomputation enabled
			if (precomp && prob1) {
				// min
				if (min) {
					// yes: "min prob = 1" equates to "for all adversaries prob equals 1"
					yes = PrismMTBDD.Prob1A(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, no, b2);
				}
				// max
				else {
					// yes: "max prob = 1" equates to "there exists an adversary prob equals 1"
					yes = PrismMTBDD.Prob1E(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2, no);
				}
			}
			// if precomputation not enabled
			else {
				// yes
				JDD.Ref(b2);
				yes = b2;
			}
			// maybe
			JDD.Ref(reach);
			JDD.Ref(yes);
			JDD.Ref(no);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(yes, no)));
		}

		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, allDDRowVars.n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");

		// if maybe is empty, we have the answer already...
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(yes);
			probs = new StateValuesMTBDD(yes, model);
		}
		// otherwise we compute the actual probabilities
		else {
			// compute probabilities
			mainLog.println("\nComputing remaining probabilities...");
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.NondetUntil(tr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
					probs = new StateValuesMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					IntegerVector strat = null;
					if (genStrat) {
						JDDNode ddStrat = JDD.ITE(yes, JDD.Constant(-2), JDD.Constant(-1));
						strat = new IntegerVector(ddStrat, allDDRowVars, odd);
						JDD.Deref(ddStrat);
					}
					probsDV = PrismSparse.NondetUntil(tr, tra, model.getSynchs(), odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min, strat);
					if (genStrat) {
						result.setStrategy(new MDStrategyIV(model, strat));
					}
					probs = new StateValuesDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.NondetUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
					probs = new StateValuesDV(probsDV, model);
					break;
				default:
					throw new PrismException("Unknown engine");
				}
			} catch (PrismException e) {
				JDD.Deref(yes);
				JDD.Deref(no);
				JDD.Deref(maybe);
				throw e;
			}
		}

		// derefs
		JDD.Deref(yes);
		JDD.Deref(no);
		JDD.Deref(maybe);

		return probs;
	}

	// compute (max) probabilities for multi-objective reachability

	protected Object computeMultiReachProbs(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> rewards, JDDNode st, List<JDDNode> targets,
			List<JDDNode> combinations, List<Integer> combinationIDs, OpsAndBoundsList opsAndBounds, boolean hasconflictobjectives) throws PrismException
	{
		JDDNode yes, no, maybe, bottomec = null;
		Object value;
		int i, j, n;
		// Local copy of setting
		int engine = this.engine;

		//JDDNode maybe_r = null; // maybe states for the reward formula
		//JDDNode trr = null; // transition rewards
		//int op1 = relOps.get(0).intValue(); // the operator of the first objective query

		n = targets.size();

		JDDNode labels[] = new JDDNode[n];
		String labelNames[] = new String[n];
		// Build temporary DDs for combined targets
		for (i = 0; i < n; i++) {
			JDD.Ref(targets.get(i));
			JDDNode tmp = targets.get(i);
			if (combinations != null) {
				for (j = 0; j < combinations.size(); j++) {
					if ((combinationIDs.get(j) & (1 << i)) > 0) {
						JDD.Ref(combinations.get(j));
						tmp = JDD.Or(tmp, combinations.get(j));
					}
				}
			}
			labels[i] = tmp;
			labelNames[i] = "target" + i;
		}

		if (prism.getExportTarget()) {
			try {
				mainLog.print("\nExporting target states info to file \"" + prism.getExportTargetFilename() + "\"...");
				PrismMTBDD.ExportLabels(labels, labelNames, "l", modelProduct.getAllDDRowVars(), modelProduct.getODD(), Prism.EXPORT_PLAIN,
						prism.getExportTargetFilename());
			} catch (FileNotFoundException e) {
				mainLog.println("\nWarning: Could not export target to file \"" + prism.getExportTargetFilename() + "\"");
			}
		}

		// yes - union of targets (just to compute no)
		yes = JDD.Constant(0);
		//n = targets.size();
		for (i = 0; i < n; i++) {
			JDD.Ref(targets.get(i));
			yes = JDD.Or(yes, targets.get(i));
		}
		if (combinations != null)
			for (i = 0; i < combinations.size(); i++) {
				JDD.Ref(combinations.get(i));
				yes = JDD.Or(yes, combinations.get(i));
			}

		if (opsAndBounds.rewardSize() == 0)
			no = PrismMTBDD.Prob0A(modelProduct.getTrans01(), modelProduct.getReach(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
					modelProduct.getAllDDNondetVars(), modelProduct.getReach(), yes);
		else {
			no = JDD.Constant(0);
			bottomec = PrismMTBDD.Prob0A(modelProduct.getTrans01(), modelProduct.getReach(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
					modelProduct.getAllDDNondetVars(), modelProduct.getReach(), yes);
			List<JDDNode> becs = mcLtl.findBottomEndComponents(modelProduct, bottomec);
			JDD.Deref(bottomec);
			bottomec = JDD.Constant(0);
			for (JDDNode ec : becs)
				bottomec = JDD.Or(bottomec, ec);
		}

		/*if(op1>2) { // the first query is about reward
			JDDNode no_r = PrismMTBDD.Prob0A(modelProduct.getTrans01(), modelProduct.getReach(), 
					modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(), 
					modelProduct.getAllDDNondetVars(), modelProduct.getReach(), targets.get(0));
			JDD.Ref(no_r);
			maybe_r = JDD.And(modelProduct.getReach(), JDD.Not(JDD.Or(targets.get(0), no_r)));
			trr = modelProduct.getTransRewards();
		}*/

		// maybe
		JDD.Ref(modelProduct.getReach());
		JDD.Ref(yes);
		JDD.Ref(no);
		maybe = JDD.And(modelProduct.getReach(), JDD.Not(JDD.Or(yes, no)));

		for (i = 0; i < rewards.size(); i++) {
			JDDNode tmp = rewards.remove(i);
			JDD.Ref(no);
			tmp = JDD.Apply(JDD.TIMES, tmp, JDD.Not(no));
			rewards.add(i, tmp);
		}

		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, modelProduct.getAllDDRowVars().n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, modelProduct.getAllDDRowVars().n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, modelProduct.getAllDDRowVars().n()) + "\n");

		// compute probabilities
		mainLog.println("\nComputing remaining probabilities...");
		// switch engine, if necessary
		if (engine == Prism.HYBRID) {
			mainLog.println("Switching engine since only sparse engine currently supports this computation...");
			engine = Prism.SPARSE;
		}
		mainLog.println("Engine: " + Prism.getEngineString(engine));
		
		int method = prism.getMDPMultiSolnMethod();
		
		try {
			if (engine != Prism.SPARSE)
				throw new PrismException("Currently only sparse engine supports multi-objective properties");
		
			if (method == Prism.MDP_MULTI_LP) {
				//LP currently does not support Pareto
				if (opsAndBounds.numberOfNumerical() > 1) {
					throw new PrismException("Linear programming method currently does not support generating of Pareto curves.");
				}
				
				if (opsAndBounds.rewardSize() > 0) {
					if (hasconflictobjectives) {
						value = PrismSparse.NondetMultiReachReward1(modelProduct.getTrans(), modelProduct.getTransActions(), modelProduct.getSynchs(),
								modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(),
								targets, combinations, combinationIDs, opsAndBounds, maybe, st, rewards, bottomec);
					} else {
						value = PrismSparse.NondetMultiReachReward(modelProduct.getTrans(), modelProduct.getTransActions(), modelProduct.getSynchs(),
								modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(),
								targets, opsAndBounds, maybe, st, rewards, bottomec);
					}
				} else {
					if (hasconflictobjectives) {
						value = PrismSparse.NondetMultiReach1(modelProduct.getTrans(), modelProduct.getTransActions(), modelProduct.getSynchs(),
								modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(),
								targets, combinations, combinationIDs, opsAndBounds, maybe, st);
					} else {
						value = PrismSparse.NondetMultiReach(modelProduct.getTrans(), modelProduct.getTransActions(), modelProduct.getSynchs(),
								modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(),
								targets, opsAndBounds, maybe, st);
					}
				}
			} else if (method == Prism.MDP_MULTI_GAUSSSEIDEL || method == Prism.MDP_MULTI_VALITER) {
				double timePre = System.currentTimeMillis();
				value = weightedMultiReachProbs(modelProduct, yes, maybe, st, labels, rewards, opsAndBounds);
				double timePost = System.currentTimeMillis();
				double time = ((double) (timePost - timePre)) / 1000.0;
				mainLog.println("Multi-objective value iterations took " + time + " s.");
			} else {
				throw new PrismException("Don't know how to model-check using the method: "
						+ method);
			}
		} catch (PrismException e) {
			throw e;
		} finally {
			// derefs
			if (opsAndBounds.rewardSize() > 0)
				JDD.Deref(bottomec);
			JDD.Deref(yes);
			JDD.Deref(no);
			JDD.Deref(maybe);
			for (int k = 0; k < labels.length; k++)
				JDD.Deref(labels[k]);
			for (i = 0; i < rewards.size(); i++) {
				JDD.Deref(rewards.get(i));
			}
		}
		
		return value;
	}

	// compute cumulative rewards

	protected StateValues computeCumulRewards(JDDNode tr, JDDNode sr, JDDNode trr, int time, boolean min) throws PrismException
	{
		DoubleVector rewardsDV;
		StateValues rewards = null;

		// compute rewards
		mainLog.println("\nComputing rewards...");
		mainLog.println("Engine: " + Prism.getEngineString(engine));
		try {
			switch (engine) {
			case Prism.MTBDD:
				throw new PrismException("MTBDD engine does not yet support this type of property (use the sparse engine instead)");
			case Prism.SPARSE:
				rewardsDV = PrismSparse.NondetCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, allDDNondetVars, time, min);
				rewards = new StateValuesDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				throw new PrismException("Hybrid engine does not yet support this type of property (use the sparse engine instead)");
			default:
				throw new PrismException("Unknown engine");
			}
		} catch (PrismException e) {
			throw e;
		}

		return rewards;
	}

	// compute rewards for inst reward

	protected StateValues computeInstRewards(JDDNode tr, JDDNode sr, int time, boolean min) throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;
		// Local copy of setting
		int engine = this.engine;

		// a trivial case: "=0"
		if (time == 0) {
			JDD.Ref(sr);
			rewards = new StateValuesMTBDD(sr, model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			mainLog.println("\nComputing rewards...");
			// switch engine, if necessary
			if (engine == Prism.HYBRID) {
				mainLog.println("Switching engine since hybrid engine does yet support this computation...");
				engine = Prism.SPARSE;
			}
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.NondetInstReward(tr, sr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, time, min, start);
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.NondetInstReward(tr, sr, odd, allDDRowVars, allDDColVars, allDDNondetVars, time, min, start);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					throw new PrismException("Hybrid engine does not yet support this type of property (use sparse or MTBDD engine instead)");
				default:
					throw new PrismException("Unknown engine");
				}
			} catch (PrismException e) {
				throw e;
			}
		}

		return rewards;
	}

	// compute rewards for reach reward

	protected StateValues computeReachRewards(JDDNode tr, JDDNode tra, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b, boolean min) throws PrismException
	{
		JDDNode inf, maybe, prob1, no;
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;
		// Local copy of setting
		int engine = this.engine;

		List<JDDNode> zeroCostEndComponents = null;

		// compute states which can't reach goal with probability 1
		if (b.equals(JDD.ZERO)) {
			JDD.Ref(reach);
			inf = reach;
			maybe = JDD.Constant(0);
		} else if (b.equals(reach)) {
			inf = JDD.Constant(0);
			maybe = JDD.Constant(0);
		} else {
			if (!min) {
				// compute states for which some adversaries don't reach goal with probability 1
				// note that prob1a (unlike prob1e) requires no/b2, not b1/b2
				// hence we have to call prob0e first
				no = PrismMTBDD.Prob0E(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, reach, b);
				prob1 = PrismMTBDD.Prob1A(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, no, b);
				JDD.Deref(no);
				JDD.Ref(reach);
				inf = JDD.And(reach, JDD.Not(prob1));
			} else {

				if (prism.getCheckZeroLoops()) {
					// find states transitions that have no cost
					JDD.Ref(sr);
					JDD.Ref(reach);
					JDDNode zeroReach = JDD.And(reach, JDD.Apply(JDD.EQUALS, sr, JDD.Constant(0)));
					JDD.Ref(b);
					zeroReach = JDD.And(zeroReach, JDD.Not(b));
					JDD.Ref(trr);
					JDDNode zeroTrr = JDD.Apply(JDD.EQUALS, trr, JDD.Constant(0));
					JDD.Ref(trans);
					JDD.Ref(zeroTrr);
					JDDNode zeroTrans = JDD.And(trans, zeroTrr);
					JDD.Ref(trans01);
					JDDNode zeroTrans01 = JDD.And(trans01, zeroTrr);

					ECComputer ecComp = new ECComputerDefault(prism, zeroReach, zeroTrans, zeroTrans01, model.getAllDDRowVars(), model.getAllDDColVars(),
							model.getAllDDNondetVars());
					ecComp.computeMECStates();

					zeroCostEndComponents = ecComp.getMECStates();

					JDD.Deref(zeroReach);
					JDD.Deref(zeroTrans);
					JDD.Deref(zeroTrans01);
				}

				// compute states for which all adversaries don't reach goal with probability 1
				no = PrismMTBDD.Prob0A(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, reach, b);
				prob1 = PrismMTBDD.Prob1E(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, reach, b, no);
				JDD.Deref(no);
				JDD.Ref(reach);
				inf = JDD.And(reach, JDD.Not(prob1));
			}
			JDD.Ref(reach);
			JDD.Ref(inf);
			JDD.Ref(b);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(inf, b)));
		}

		if (prism.getCheckZeroLoops()) {
			// need to deal with zero loops yet
			if (min && zeroCostEndComponents != null && zeroCostEndComponents.size() > 0) {
				mainLog.printWarning("PRISM detected your model contains " + zeroCostEndComponents.size() + " zero-reward "
						+ ((zeroCostEndComponents.size() == 1) ? "loop." : "loops.\n") + "Your minimum rewards may be too low...");
			}
		} else if (min) {
			mainLog.printWarning("PRISM hasn't checked for zero-reward loops.\n" + "Your minimum rewards may be too low...");
		}

		// print out yes/no/maybe
		mainLog.print("\ngoal = " + JDD.GetNumMintermsString(b, allDDRowVars.n()));
		mainLog.print(", inf = " + JDD.GetNumMintermsString(inf, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");

		// if maybe is empty, we have the rewards already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(inf);
			rewards = new StateValuesMTBDD(JDD.ITE(inf, JDD.PlusInfinity(), JDD.Constant(0)), model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			mainLog.println("\nComputing remaining rewards...");
			// switch engine, if necessary
			if (engine == Prism.HYBRID) {
				mainLog.println("Switching engine since hybrid engine does yet support this computation...");
				engine = Prism.SPARSE;
			}
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.NondetReachReward(tr, sr, trr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b, inf, maybe, min);
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.NondetReachReward(tr, tra, model.getSynchs(), sr, trr, odd, allDDRowVars, allDDColVars, allDDNondetVars, b, inf,
							maybe, min);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					throw new PrismException("Hybrid engine does not yet support this type of property (use sparse or MTBDD engine instead)");
					// rewardsDV = PrismHybrid.NondetReachReward(tr, sr, trr,
					// odd, allDDRowVars, allDDColVars, allDDNondetVars, b, inf,
					// maybe, min);
					// rewards = new StateValuesDV(rewardsDV, model);
					// break;
				default:
					throw new PrismException("Unknown engine");
				}
			} catch (PrismException e) {
				JDD.Deref(inf);
				JDD.Deref(maybe);
				throw e;
			}
		}

		if (zeroCostEndComponents != null)
			for (JDDNode zcec : zeroCostEndComponents)
				JDD.Deref(zcec);

		// derefs
		JDD.Deref(inf);
		JDD.Deref(maybe);

		return rewards;
	}

	/**
	 * Check whether state set represented by dd is "weak absorbing",
	 * i.e. whether set is closed under all successors.
	 * Note: does not deref dd.
	 */
	private boolean checkWeakAbsorbing(JDDNode dd, NondetModel model)
	{
		boolean result;

		JDD.Ref(model.getTrans01());
		JDD.Ref(dd);
		JDDNode tmp = JDD.And(model.getTrans01(), dd);
		tmp = JDD.SwapVariables(tmp, model.getAllDDRowVars(), model.getAllDDColVars());
		tmp = JDD.ThereExists(tmp, model.getAllDDNondetVars());
		tmp = JDD.ThereExists(tmp, model.getAllDDColVars());
		JDD.Ref(model.getReach());
		tmp = JDD.And(model.getReach(), tmp);
		JDD.Ref(dd);
		tmp = JDD.And(tmp, JDD.Not(dd));
		result = tmp.equals(JDD.ZERO);
		JDD.Deref(tmp);

		return result;
	}

	private int changeToInteger(List<Integer> ids)
	{
		int k = 0;
		for (int i = 0; i < ids.size(); i++) {
			int j = 1;
			if (ids.get(i) > 0)
				j = j << ids.get(i);
			k += j;
		}
		return k;
	}

	protected Object weightedMultiReachProbs(NondetModel modelProduct, JDDNode yes_ones, JDDNode maybe, JDDNode st, JDDNode[] targets, List<JDDNode> rewards, OpsAndBoundsList opsAndBounds)
			throws PrismException
	{
		int numberOfMaximizing = opsAndBounds.numberOfNumerical();

		if (numberOfMaximizing > 2)
			throw new PrismException("Number of maximizing objectives must be at most 3");

		if (numberOfMaximizing >= 2 && opsAndBounds.probSize() + opsAndBounds.rewardSize() > numberOfMaximizing)
				throw new PrismException("Number of maximizing objectives can be 2 or 3 only if there are no other (i.e. bounded) objectives present");
			
		if (numberOfMaximizing >= 2) {
			return generateParetoCurve(modelProduct, yes_ones, maybe, st, targets, rewards, opsAndBounds);
		} else
			return targetDrivenMultiReachProbs(modelProduct, yes_ones, maybe, st, targets, rewards, opsAndBounds);
	}
	
	protected TileList generateParetoCurve(NondetModel modelProduct, JDDNode yes_ones, JDDNode maybe, final JDDNode st, JDDNode[] targets,
			List<JDDNode> rewards, OpsAndBoundsList opsAndBounds) throws PrismException
	{
		int numberOfPoints = 0;
		int rewardStepBounds[] = new int[rewards.size()];
		for (int i = 0; i < rewardStepBounds.length; i++)
			rewardStepBounds[i] = opsAndBounds.getRewardStepBound(i);
		
		int probStepBounds[] = new int[targets.length];
		for (int i = 0; i < probStepBounds.length; i++)
			probStepBounds[i] = opsAndBounds.getProbStepBound(i);
		
		
		double timer = System.currentTimeMillis();
		boolean min = false;

		//convert minimizing rewards to maximizing
		for (int i = 0; i < opsAndBounds.rewardSize(); i++) {
			if (opsAndBounds.getRewardOperator(i) == Operator.R_LE) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}
			
			if (opsAndBounds.getRewardOperator(i) == Operator.R_MIN) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}
		}
		
		double tolerance = prism.getSettings().getDouble(PrismSettings.PRISM_PARETO_EPSILON);
		int maxIters = prism.getSettings().getInteger(PrismSettings.PRISM_MULTI_MAX_POINTS);

		NativeIntArray adversary = new NativeIntArray((int) modelProduct.getNumStates());
		int dimProb = targets.length;
		int dimReward = rewards.size();
		Point targetPoint = new Point(dimProb + dimReward);
		ArrayList<Point> computedPoints = new ArrayList<Point>();
		ArrayList<Point> computedDirections = new ArrayList<Point>();
		ArrayList<Point> pointsForInitialTile = new ArrayList<Point>();

		//create vectors and sparse matrices for the objectives
		final DoubleVector[] probDoubleVectors = new DoubleVector[dimProb];
		final NDSparseMatrix[] rewSparseMatrices = new NDSparseMatrix[dimReward];

		JDD.Ref(modelProduct.getTrans());
		JDD.Ref(modelProduct.getReach());

		//create a sparse matrix for transitions
		JDDNode a = JDD.Apply(JDD.TIMES, modelProduct.getTrans(), modelProduct.getReach());

		if (!min) {
			JDD.Ref(a);
			JDDNode tmp = JDD.And(JDD.Equals(a, 1.0), JDD.Identity(modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars()));
			a = JDD.ITE(tmp, JDD.Constant(0), a);
		}

		NDSparseMatrix trans_matrix = NDSparseMatrix.BuildNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
				modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars());

		//create double vectors for probabilistic objectives
		for (int i = 0; i < dimProb; i++) {
			probDoubleVectors[i] = new DoubleVector(targets[i], modelProduct.getAllDDRowVars(), modelProduct.getODD());
		}

		//create sparse matrices for reward objectives
		for (int i = 0; i < dimReward; i++) {
			NDSparseMatrix rew_matrix = NDSparseMatrix.BuildSubNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
					modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), rewards.get(i));
			rewSparseMatrices[i] = rew_matrix;
		}

		JDD.Deref(a);

		for (int i = 0; i < dimProb; i++) {
			double[] result;
			
			double[] weights = new double[dimProb + dimReward];
			weights[i] = 1.0;
			/*if (prism.getMDPMultiSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				//System.out.println("Doing GS");
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, new DoubleVector[] {probDoubleVectors[i]}, new int[] {probStepBounds[i]}, null,
							new double[] { 1.0 }, null);
			} else {
				//System.out.println("Not doing GS");
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, new DoubleVector[] {probDoubleVectors[i]}, new int[] {probStepBounds[i]}, null,
						new double[] { 1.0 }, null);
			}*/
			if (prism.getMDPSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
						trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			} else {
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
					 	trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			}
			
			//The following is thrown because in this case the i-th dimension is
			//zero and we might have problems when getting an separating hyperplane.
			/*if (result[0] == 0)
				throw new PrismException("The probabilistic objective number " + i + " is degenerate since the optimal value is also the least optimal value." );
			*/
			targetPoint = new Point(result);
			pointsForInitialTile.add(targetPoint);
		}

		for (int i = 0; i < dimReward; i++) {
			if (verbose) {
				mainLog.println("Getting an upper bound on maximizing objective " + i);
			}

			double[] result;
			double[] weights = new double[dimProb + dimReward];
			weights[i] = 1.0;
			/*System.out.println(prism.getMDPSolnMethod());
			if (prism.getMDPSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				//System.out.println("Doing GS");
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, null, null, new NDSparseMatrix[] { rewSparseMatrices[i] },
							new double[] { 1.0 }, new int[] { rewardStepBounds[i] });
			} else {
				//System.out.println("Not doing GS");
					result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, null, null, new NDSparseMatrix[] { rewSparseMatrices[i] },
							new double[] { 1.0 }, new int[] { rewardStepBounds[i] });
			}*/
			if (prism.getMDPSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
						trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			} else {
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
					 	trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			}
			
			numberOfPoints++;
			targetPoint = new Point(result);
			pointsForInitialTile.add(targetPoint);
			
			if (verbose) {
				mainLog.println("Upper bound is " + Arrays.toString(result));
			}
		}
		
		if (verbose)
			mainLog.println("Points for initial tile: " + pointsForInitialTile);
		
		Tile initialTile = new Tile(pointsForInitialTile);
		TileList tileList = new TileList(initialTile, opsAndBounds, tolerance);
		
		Point direction = tileList.getCandidateHyperplane();
		
		if (verbose) {
			mainLog.println("The initial direction is " + direction);
		}
		
		boolean decided = false;
		int iters = 0;
		int output = 0;
		while (iters < maxIters) {
			iters++;

			//create the weights array
			double[] weights = new double[dimProb + dimReward];
			for (int i = 0; i < dimProb + dimReward; i++) {
				weights[i] = direction.getCoord(i);
			}

			double[] result;
			if (prism.getMDPSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
						trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			} else {
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
					 	trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			}
			
		/*	//Minimizing operators are negated, and for Pareto we need to maximize.
			for (int i = 0; i < dimProb; i++) {
				if (opsAndBounds.getOperator(i) == Operator.P_MIN) {
					result[i] = -(1-result[i]);
				}
			} */
			
			numberOfPoints++;
			
			//collect the numbers obtained from methods executed above.
			Point newPoint = new Point(result);

			if (verbose) {
				mainLog.println("\n" + numberOfPoints + ": New point is " + newPoint + ".");
				mainLog.println("TileList:" + tileList);
			}

			computedPoints.add(newPoint);
			computedDirections.add(direction);

			tileList.addNewPoint(newPoint);
			//mainLog.println("\nTiles after adding: " + tileList);
			//compute new direction
			direction = tileList.getCandidateHyperplane();

			if (verbose) {
				mainLog.println("New direction is " + direction);
				//mainLog.println("TileList: " + tileList);
				
			}

			if (direction == null) {
				//no tile could be improved
				decided = true;
				break;
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("The value iteration(s) took " + timer / 1000.0 + " seconds altogether.");
		mainLog.println("Number of weight vectors used: " + numberOfPoints);

		if (!decided)
			throw new PrismException("The computation did not finish in " + maxIters
					+ " target point iterations, try increasing this number using the -multimaxpoints switch.");
		else {
			String paretoFile = prism.getSettings().getString(PrismSettings.PRISM_EXPORT_PARETO_FILENAME);
			
			//export to file if required
			if (paretoFile != null && !paretoFile.equals("")) {
				MultiObjUtils.exportPareto(tileList, paretoFile);
				mainLog.println("Exported Pareto curve. To see it, run\n etc/scripts/prism-pareto.py " + paretoFile);
			}
			
			mainLog.println("Computed " + tileList.getNumberOfDifferentPoints() + " points altogether:\n");
			mainLog.println(tileList.getPoints().toString());			
			
			return tileList;
		}	
	}

	protected double targetDrivenMultiReachProbs(NondetModel modelProduct, JDDNode yes_ones, JDDNode maybe, final JDDNode st, JDDNode[] targets,
			List<JDDNode> rewards, OpsAndBoundsList opsAndBounds) throws PrismException
	{
		int numberOfPoints = 0;
		int rewardStepBounds[] = new int[rewards.size()];
		for (int i = 0; i < rewardStepBounds.length; i++)
			rewardStepBounds[i] = opsAndBounds.getRewardStepBound(i);
		
		int probStepBounds[] = new int[targets.length];
		for (int i = 0; i < probStepBounds.length; i++)
			probStepBounds[i] = opsAndBounds.getProbStepBound(i);
		
		
		double timer = System.currentTimeMillis();
		boolean min = false;

		//convert minimizing rewards to maximizing
		for (int i = 0; i < opsAndBounds.rewardSize(); i++) {
			if (opsAndBounds.getRewardOperator(i) == Operator.R_LE) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}
			
			if (opsAndBounds.getRewardOperator(i) == Operator.R_MIN) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}
		}
		
		boolean maximizingProb = (opsAndBounds.probSize() > 0 && (opsAndBounds.getProbOperator(0) == Operator.P_MAX || opsAndBounds.getProbOperator(0) == Operator.P_MIN));
		boolean maximizingReward = (opsAndBounds.rewardSize() > 0 && (opsAndBounds.getRewardOperator(0) == Operator.R_MAX || opsAndBounds.getRewardOperator(0) == Operator.R_MIN));
		boolean maximizingNegated = (maximizingProb && opsAndBounds.getProbOperator(0) == Operator.P_MIN) || (maximizingReward && opsAndBounds.getRewardOperator(0) == Operator.R_MIN);

		int maxIters = prism.getSettings().getInteger(PrismSettings.PRISM_MULTI_MAX_POINTS);

		NativeIntArray adversary = new NativeIntArray((int) modelProduct.getNumStates());
		int dimProb = targets.length;
		int dimReward = rewards.size();
		Point targetPoint = new Point(dimProb + dimReward);
		ArrayList<Point> computedPoints = new ArrayList<Point>();
		ArrayList<Point> computedDirections = new ArrayList<Point>();

		ArrayList<Point> pointsForInitialTile = new ArrayList<Point>();

		//create vectors and sparse matrices for the objectives
		final DoubleVector[] probDoubleVectors = new DoubleVector[dimProb];
		final NDSparseMatrix[] rewSparseMatrices = new NDSparseMatrix[dimReward];

		JDD.Ref(modelProduct.getTrans());
		JDD.Ref(modelProduct.getReach());

		//create a sparse matrix for transitions
		JDDNode a = JDD.Apply(JDD.TIMES, modelProduct.getTrans(), modelProduct.getReach());

		if (!min) {
			JDD.Ref(a);
			JDDNode tmp = JDD.And(JDD.Equals(a, 1.0), JDD.Identity(modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars()));
			a = JDD.ITE(tmp, JDD.Constant(0), a);
		}

		NDSparseMatrix trans_matrix = NDSparseMatrix.BuildNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
				modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars());

		//create double vectors for probabilistic objectives
		for (int i = 0; i < dimProb; i++) {
			probDoubleVectors[i] = new DoubleVector(targets[i], modelProduct.getAllDDRowVars(), modelProduct.getODD());
		}

		//create sparse matrices for reward objectives
		for (int i = 0; i < dimReward; i++) {
			NDSparseMatrix rew_matrix = NDSparseMatrix.BuildSubNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
					modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), rewards.get(i));
			rewSparseMatrices[i] = rew_matrix;
		}

		JDD.Deref(a);

		//initialize the target point
		for (int i = 0; i < dimProb; i++) {
			targetPoint.setCoord(i, opsAndBounds.getProbBound(i));
		}
		if (maximizingProb) {
			targetPoint.setCoord(0, 1.0);
		}

		for (int i = 0; i < dimReward; i++) {
			//multiply by -1 in case of minimizing, that converts it to maximizing.
			double t = (opsAndBounds.getRewardOperator(i) == Operator.R_LE) ? -opsAndBounds.getRewardBound(i) : opsAndBounds.getRewardBound(i);
			targetPoint.setCoord(i + dimProb, t);
		}
		if (maximizingReward) {
			if (verbose) {
				mainLog.println("Getting an upper bound on maximizing objective");
			}

			double[] result;
			if (prism.getMDPSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				//System.out.println("Doing GS");
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, null, null, new NDSparseMatrix[] { rewSparseMatrices[0] },
							new double[] { 1.0 }, new int[] { rewardStepBounds[0] });
			} else {
				//System.out.println("Not doing GS");
					result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, null, null, new NDSparseMatrix[] { rewSparseMatrices[0] },
							new double[] { 1.0 }, new int[] { rewardStepBounds[0] });
			}
			numberOfPoints++;
				
			targetPoint.setCoord(dimProb, result[0]);

			if (verbose) {
				mainLog.println("Upper bound is " + result[0]);
			}
		}

		Point direction = MultiObjUtils.getWeights(targetPoint, computedPoints);
		
		if (verbose) {
			mainLog.println("The initial target point is " + targetPoint);
			mainLog.println("The initial direction is " + direction);
		}

		boolean decided = false;
		boolean isAchievable = false;
		int iters = 0;
		int output = 0;
		while (iters < maxIters) {
			iters++;

			//create the weights array
			double[] weights = new double[dimProb + dimReward];
			for (int i = 0; i < dimProb + dimReward; i++) {
				weights[i] = direction.getCoord(i);
			}

			double[] result;
			if (prism.getMDPSolnMethod() == Prism.MDP_MULTI_GAUSSSEIDEL) {
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
						trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			} else {
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(),
						modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), false, st, adversary,
					 	trans_matrix, probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			}
			numberOfPoints++;
			
			//collect the numbers obtained from methods executed above.
			Point newPoint = new Point(result);

			if (verbose) {
				mainLog.println("New point is " + newPoint + ".");
			}

			computedPoints.add(newPoint);
			computedDirections.add(direction);

			
			//if (prism.getExportMultiGraphs())
			//	MultiObjUtils.printGraphFileDebug(targetPoint, computedPoints, computedDirections, prism.getExportMultiGraphsDir(), output++);

			//check if the new point together with the direction shows the point is unreachable
			double dNew = 0.0;
			for (int i = 0; i < dimProb + dimReward; i++) {
				dNew += newPoint.getCoord(i) * direction.getCoord(i);
			}

			double dTarget = 0.0;
			for (int i = 0; i < dimProb + dimReward; i++) {
				dTarget += targetPoint.getCoord(i) * direction.getCoord(i);
			}

			if (dTarget > dNew) {
				if (maximizingProb || maximizingReward) {
					int maximizingCoord = (maximizingProb) ? 0 : dimProb;
					double rest = dNew - (dTarget - direction.getCoord(maximizingCoord) * targetPoint.getCoord(maximizingCoord));
					if ((!maximizingNegated && rest < 0) || (maximizingNegated && rest > 0)) {
						//target can't be lowered
						decided = true;
						targetPoint.setCoord(maximizingCoord, Double.NaN);
						if (verbose)
							mainLog.println("Decided, target is " + targetPoint);
						break;
					} else {
						double lowered = rest / direction.getCoord(maximizingCoord);
						targetPoint.setCoord(maximizingCoord, lowered);
						//HACK
						if (lowered == Double.NEGATIVE_INFINITY) {
							targetPoint.setCoord(maximizingCoord, Double.NaN);
							mainLog.println("\nThe constraints are not achievable!\n");
							decided = true;
							isAchievable = false;
							break;
						}
						if (verbose)
							mainLog.println("Target lowered to " + targetPoint);

						//if (prism.getExportMultiGraphs())
						//	MultiObjUtils.printGraphFileDebug(targetPoint, computedPoints, computedDirections, prism.getExportMultiGraphsDir(), output++);
					}
				} else {
					decided = true;
					isAchievable = false;
					break;
				}
			}

			//compute new direction
			direction = MultiObjUtils.getWeights(targetPoint, computedPoints);

			if (verbose) {
				mainLog.println("New direction is " + direction);
			}

			if (direction == null || computedDirections.contains(direction)) //The second disjunct is for convergence
			{
				//there is no hyperplane strictly separating the target from computed points
				//hence we can conclude that the point is reachable
				decided = true;
				isAchievable = true;
				break;
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("The value iteration(s) took " + timer / 1000.0 + " seconds altogether.");
		mainLog.println("Number of weight vectors used: " + numberOfPoints);

		if (!decided)
			throw new PrismException("The computation did not finish in " + maxIters
					+ " target point iterations, try increasing this number using the -multimaxpoints switch.");
		if (maximizingProb || maximizingReward) {
			int maximizingCoord = (maximizingProb) ? 0 : dimProb;
			return (maximizingNegated) ? -targetPoint.getCoord(maximizingCoord) : targetPoint.getCoord(maximizingCoord);
		} else {
			return (isAchievable) ? 1.0 : 0.0;
		}
	}
	
}

// ------------------------------------------------------------------------------
