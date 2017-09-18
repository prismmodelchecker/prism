//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import hybrid.PrismHybrid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import odd.ODDUtils;
import parser.BooleanUtils;
import parser.ast.Coalition;
import parser.ast.Expression;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.PropertiesFile;
import parser.ast.RelOp;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypePathBool;
import parser.type.TypePathDouble;
import prism.LTLModelChecker.LTLProduct;
import sparse.PrismSparse;
import strat.MDStrategyIV;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceReachDD;
import acceptance.AcceptanceType;
import automata.DA;
import common.StopWatch;
import dv.DoubleVector;
import dv.IntegerVector;
import explicit.MinMax;

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

	public NondetModelChecker createNewModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		return new NondetModelChecker(prism, m, pf);
	}

	// Model checking functions

	@Override
	public StateValues checkExpression(Expression expr, JDDNode statesOfInterest) throws PrismException
	{
		StateValues res;

		// <<>> or [[]] operator
		if (expr instanceof ExpressionStrategy) {
			res = checkExpressionStrategy((ExpressionStrategy) expr, statesOfInterest);
		}
		// P operator
		else if (expr instanceof ExpressionProb) {
			res = checkExpressionProb((ExpressionProb) expr, statesOfInterest);
		}
		// R operator
		else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward((ExpressionReward) expr, statesOfInterest);
		}
		// Multi-objective
		else if (expr instanceof ExpressionFunc) {
			// Detect "multi" function
			if (((ExpressionFunc) expr).getName().equals("multi")) {
				res = checkExpressionMultiObjective((ExpressionFunc) expr, statesOfInterest);
			}
			// For any other function, check as normal
			else {
				res = super.checkExpression(expr, statesOfInterest);
			}
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(expr, statesOfInterest);
		}

		// Filter out non-reachable states from solution
		// (only necessary for symbolically stored vectors)
		if (res instanceof StateValuesMTBDD)
			res.filter(reach);

		return res;
	}

	/**
	 * Model check a <<>> or [[]] operator expression.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkExpressionStrategy(ExpressionStrategy expr, JDDNode statesOfInterest) throws PrismException
	{
		// Will we be quantifying universally or existentially over strategies/adversaries?
		boolean forAll = !expr.isThereExists();
		
		// Extract coalition info
		Coalition coalition = expr.getCoalition();
		// Deal with the coalition operator here and then remove it
		if (coalition != null) {
			if (coalition.isEmpty()) {
				// An empty coalition negates the quantification ("*" has no effect)
				forAll = !forAll;
			}
			coalition = null;
		}

		// Process operand(s)
		List<Expression> exprs = expr.getOperands();
		// Pass onto relevant method:
		// Single P operator
		if (exprs.size() == 1 && exprs.get(0) instanceof ExpressionProb) {
			return checkExpressionProb((ExpressionProb) exprs.get(0), forAll, statesOfInterest);
		}
		// Single R operator
		else if (exprs.size() == 1 && exprs.get(0) instanceof ExpressionReward) {
			return checkExpressionReward((ExpressionReward) exprs.get(0), forAll, statesOfInterest);
		}
		// Anything else is treated as multi-objective 
		else {
			return checkExpressionMultiObjective(exprs, forAll, statesOfInterest);
		}
	}
	
	/**
	 * Model check a P operator expression.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkExpressionProb(ExpressionProb expr, JDDNode statesOfInterest) throws PrismException
	{
		// Use the default semantics for a standalone P operator
		// (i.e. quantification over all strategies)
		return checkExpressionProb(expr, true, statesOfInterest);
	}
	
	/**
	 * Model check a P operator expression.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 *
	 * @param expr The P operator expression
	 * @param forAll Are we checking "for all strategies" (true) or "there exists a strategy" (false)? [irrelevant for numerical (=?) queries]
	 */
	protected StateValues checkExpressionProb(ExpressionProb expr, boolean forAll, JDDNode statesOfInterest) throws PrismException
	{
		// Get info from P operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType(), forAll);
		
		// Check for trivial (i.e. stupid) cases
		if (opInfo.isTriviallyTrue()) {
			mainLog.printWarning("Checking for probability " + opInfo.relOpBoundString() + " - formula trivially satisfies all states");
			JDD.Ref(reach);
			JDD.Deref(statesOfInterest);
			return new StateValuesMTBDD(reach, model);
		} else if (opInfo.isTriviallyFalse()) {
			mainLog.printWarning("Checking for probability " + opInfo.relOpBoundString() + " - formula trivially satisfies no states");
			JDD.Deref(statesOfInterest);
			return new StateValuesMTBDD(JDD.Constant(0), model);
		}

		// Compute probabilities
		boolean qual = opInfo.isQualitative() && precomp && prob0 && prob1;
		StateValues probs = checkProbPathFormula(expr.getExpression(), qual, minMax.isMin(), statesOfInterest);

		// Print out probabilities
		if (verbose) {
			mainLog.print("\n" + (minMax.isMin() ? "Minimum" : "Maximum") + " probabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}

		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			JDDNode sol = probs.getBDDFromInterval(opInfo.getRelOp(), opInfo.getBound());
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
			// free vector
			probs.clear();
			return new StateValuesMTBDD(sol, model);
		}
	}

	/**
	 * Model check an R operator expression.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkExpressionReward(ExpressionReward expr, JDDNode statesOfInterest) throws PrismException
	{
		// Use the default semantics for a standalone R operator
		// (i.e. quantification over all strategies)
	    return checkExpressionReward(expr, true, statesOfInterest);
	}
	
	/**
	 * Model check an R operator expression.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 *
	 * @param expr The R operator expression
	 * @param forAll Are we checking "for all strategies" (true) or "there exists a strategy" (false)? [irrelevant for numerical (=?) queries]
	 */
	protected StateValues checkExpressionReward(ExpressionReward expr, boolean forAll, JDDNode statesOfInterest) throws PrismException
	{
		// Get info from R operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType(), forAll);

		// Get rewards
		Object rs = expr.getRewardStructIndex();
		JDDNode stateRewards = getStateRewardsByIndexObject(rs, model, constantValues);
		JDDNode transRewards = getTransitionRewardsByIndexObject(rs, model, constantValues);

		// Compute rewards
		StateValues rewards = null;
		Expression expr2 = expr.getExpression();
		if (expr2.getType() instanceof TypePathDouble) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr2;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_C:
				if (exprTemp.hasBounds()) {
					rewards = checkRewardCumul(exprTemp, stateRewards, transRewards, minMax.isMin(), statesOfInterest);
				} else {
					rewards = checkRewardTotal(exprTemp, stateRewards, transRewards, minMax.isMin(), statesOfInterest);
				}
				break;
			case ExpressionTemporal.R_I:
				rewards = checkRewardInst(exprTemp, stateRewards, transRewards, minMax.isMin(), statesOfInterest);
				break;
			}
		} else if (expr2.getType() instanceof TypePathBool || expr2.getType() instanceof TypeBool) {
			rewards = checkRewardPathFormula(expr2, stateRewards, transRewards, minMax.isMin(), statesOfInterest);
		}

		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		// print out rewards
		if (verbose) {
			mainLog.print("\n" + (minMax.isMin() ? "Minimum" : "Maximum") + " rewards (non-zero only) for all states:\n");
			rewards.print(mainLog);
		}

		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return rewards;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			JDDNode sol = rewards.getBDDFromInterval(opInfo.getRelOp(), opInfo.getBound());
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
			// free vector
			rewards.clear();
			return new StateValuesMTBDD(sol, model);
		}
	}

	/**
	 * Model check a multi-objective query (from the contents of a strategy operator).
	 * Return the result as a StateValues object (usually this gives values for all states,
	 * but for a multi-objective query, we just give a single value, i.e., the statesOfInterest should be a singleton set).
	 *
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 * @param exprs The list of Expressions specifying the objectives
	 * @param forAll Are we checking "for all strategies" (true) or "there exists a strategy" (false)? [irrelevant for numerical (=?) queries]
	 * @param statesOfInterest the states of interest
	 */
	protected StateValues checkExpressionMultiObjective(List<Expression> exprs, boolean forAll, JDDNode statesOfInterest) throws PrismException
	{
		if (fairness) {
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Multi-objective reasoning under fairness currently not supported");
		}

		// For now, just support a single expression (which may encode a Boolean combination of objectives)
		if (exprs.size() > 1) {
			JDD.Deref(statesOfInterest);
			throw new PrismException("Cannot currently check strategy operators with lists of expressions");
		}
		Expression exprSub = exprs.get(0);

		// Boolean
		if (exprSub.getType() instanceof TypeBool) {
			// Copy expression because we will modify it
			exprSub = (ExpressionStrategy) exprSub.deepCopy();
			// We will solve an existential query, so negate if universal
			if (forAll) {
				exprSub = Expression.Not(exprSub);
			}
			// Convert to DNF
			List<List<Expression>> dnf = BooleanUtils.convertToDNFLists(exprSub);
			// Check all "propositions" of DNF are valid
			for (List<Expression> conjunction : dnf) {
				for (Expression prop : conjunction) {
					if (Expression.isNot(prop)) {
						prop = ((ExpressionUnaryOp) prop).getOperand();
					}
					if (!(prop instanceof ExpressionQuant)) {
						JDD.Deref(statesOfInterest);
						throw new PrismException("Expression " + prop + " is not allowed in a multi-objective query");
					}
				}
			}
			// Push negation inside objectives
			for (List<Expression> conjunction : dnf) {
				for (int j = 0; j < conjunction.size(); j++) {
					Expression prop = conjunction.get(j);
					if (Expression.isNot(prop)) {
						ExpressionQuant exprQuant = (ExpressionQuant) ((ExpressionUnaryOp) prop).getOperand();
						exprQuant.setRelOp(exprQuant.getRelOp().negate());
						conjunction.set(j, exprQuant);
					}
				}
			}
			// Print reduced query
			mainLog.println("\nReducing multi-objective query to DNF: " + BooleanUtils.convertDNFListsToExpression(dnf));

			// Only handle a single disjunct for now
			if (dnf.size() > 1) {
				JDD.Deref(statesOfInterest);
				throw new PrismException("Multi-objective model checking of multiple disjuncts not yet supported");
			}
			// Convert to multi(...)
			ExpressionFunc exprMulti = new ExpressionFunc("multi");
			for (Expression conjunct : dnf.get(0)) {
				exprMulti.addOperand(conjunct);
			}
			// Handle negation
			if (forAll) {
				return checkExpression(Expression.Not(exprMulti), statesOfInterest);
			} else {
				return checkExpressionMultiObjective(exprMulti, statesOfInterest);
			}
		} else if (exprSub.getType() instanceof TypeDouble) {
			return checkExpressionMultiObjective(exprs, statesOfInterest);
		} else {
			JDD.Deref(statesOfInterest);
			throw new PrismException("Multi-objective model checking not supported for: " + exprSub);
		}
	}
	
	/**
	 * Model check a multi-objective expression and return the result.
	 * For multi-objective queries, we only find the value for one state.
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkExpressionMultiObjective(ExpressionFunc expr, JDDNode statesOfInterest) throws PrismException
	{
		// Extract objective list from 'multi' function
		List<Expression> exprs = new ArrayList<Expression>();
		int n = expr.getNumOperands();
		for (int i = 0; i < n; i++) {
			exprs.add(expr.getOperand(i));
		}
		return checkExpressionMultiObjective(exprs, statesOfInterest);
	}
	
	/**
	 * Model check a multi-objective expression and return the result.
	 * For multi-objective queries, we only find the value for one state.
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkExpressionMultiObjective(List<Expression> exprs, JDDNode statesOfInterest) throws PrismException
	{
		// Objective/target info
		List<JDDNode> multitargetDDs = null;
		List<Integer> multitargetIDs = null;

		// LTL/product model stuff
		NondetModel modelProduct, modelNew;
		JDDVars[] draDDRowVars, draDDColVars;
		MultiObjModelChecker mcMo;
		LTLModelChecker mcLtl;
		Expression[] ltl;
		DA<BitSet,AcceptanceRabin>[] dra;
		// State index info
		// Misc
		boolean negateresult = false;
		int conflictformulae = 0;
		boolean hasMaxReward = false;
		//boolean hasLTLconstraint = false;

		if (fairness) {
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Multi-objective reasoning under fairness currently not supported");
		}

		if (doIntervalIteration) {
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Interval iteration currently not supported for multi-objective reasoning");
		}

		// Make sure we are only expected to compute a value for a single state,
		// i.e., that statesOfInterest is a singleton
		if (!JDD.isSingleton(statesOfInterest, model.getAllDDRowVars())) {
			JDD.Deref(statesOfInterest);
			throw new PrismException("Multi-objective model checking can only compute values from a single state");
		}
		JDDNode stateOfInterest = statesOfInterest;

		// Can't do LTL with time-bounded variants of the temporal operators
		// TODO removed since it is allowed for valiter.
		// TODO make sure it is treated correctly for all params
		/*if (Expression.containsTemporalTimeBounds(expr)) {
			throw new PrismException("Time-bounded operators not supported in LTL: " + expr);
		}*/

		// Check format and extract bounds/etc.
		int numObjectives = exprs.size();
		OpsAndBoundsList opsAndBounds = new OpsAndBoundsList();
		List<JDDNode> transRewardsList = new ArrayList<JDDNode>();
		List<Expression> pathFormulas = new ArrayList<Expression>(numObjectives);
		for (int i = 0; i < numObjectives; i++) {
			extractInfoFromMultiObjectiveOperand((ExpressionQuant) exprs.get(i), opsAndBounds, transRewardsList, pathFormulas, i);
		}

		//currently we do 1 numerical subject to booleans, or multiple numericals only 
		if (opsAndBounds.numberOfNumerical() > 1 &&
				opsAndBounds.numberOfNumerical() < opsAndBounds.probSize() + opsAndBounds.rewardSize()) {
			JDD.Deref(stateOfInterest);
			throw new PrismException("Multiple min/max queries cannot be combined with boolean queries.");
		}
		
		negateresult = opsAndBounds.contains(Operator.P_MIN);
		hasMaxReward = opsAndBounds.contains(Operator.R_GE) || opsAndBounds.contains(Operator.R_MAX);

		// Multi-objective model checking

		// Create arrays to store LTL/DRA info
		ltl = new Expression[numObjectives];
		dra = new DA[numObjectives];
		draDDRowVars = new JDDVars[numObjectives];
		draDDColVars = new JDDVars[numObjectives];

		// For LTL/multi-obj model checking routines
		mcLtl = new LTLModelChecker(prism);
		mcMo = new MultiObjModelChecker(prism, prism);

		// Product is initially just the original model (we build it recursively)
		modelProduct = model;

		// Go through probabilistic objectives and construct product MDP.
		long l = System.currentTimeMillis();
		boolean originalmodel = true;
		for (int i = 0; i < numObjectives; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				draDDRowVars[i] = new JDDVars();
				draDDColVars[i] = new JDDVars();
				modelNew = mcMo.constructDRAandProductMulti(modelProduct, mcLtl, this, ltl[i], i, dra, opsAndBounds.getOperator(i), pathFormulas.get(i),
						draDDRowVars[i], draDDColVars[i], stateOfInterest);
				// Deref old product (unless is the original model)
				if (i > 0 & !originalmodel)
					modelProduct.clear();
				// Store new product
				modelProduct = modelNew;
				originalmodel = false;
			}
		}
		l = System.currentTimeMillis() - l;
		mainLog.println("Total time for product construction: " + l / 1000.0 + " seconds.");

		// TODO: move this above
		// Replace min by max and <= by >=
		opsAndBounds.makeAllProbUp();

		// Print some info
		outputProductMulti(modelProduct);

		// Construct rewards for product model
		List<JDDNode> transRewardsListProduct = new ArrayList<JDDNode>();
		for (JDDNode transRewards : transRewardsList) {
			JDD.Ref(transRewards);
			JDD.Ref(modelProduct.getTrans01());
			transRewardsListProduct.add(JDD.Apply(JDD.TIMES, transRewards, modelProduct.getTrans01()));
		}

		// Removing actions with non-zero reward from the product for maximum cases
		if (hasMaxReward /*& hasLTLconstraint*/) {
			mcMo.removeNonZeroMecsForMax(modelProduct, mcLtl, transRewardsList, opsAndBounds, numObjectives, dra, draDDRowVars, draDDColVars);
		}

		// Remove all non-zero reward from trans in order to search for zero reward end components
		JDDNode tmptrans = modelProduct.getTrans();
		JDDNode tmptrans01 = modelProduct.getTrans01();
		boolean transchanged = mcMo.removeNonZeroRewardTrans(modelProduct, transRewardsList, opsAndBounds);

		// Compute all maximal end components
		ArrayList<ArrayList<JDDNode>> allstatesH = new ArrayList<ArrayList<JDDNode>>(numObjectives);
		ArrayList<ArrayList<JDDNode>> allstatesL = new ArrayList<ArrayList<JDDNode>>(numObjectives);
		JDDNode acceptanceVector_H = JDD.Constant(0);
		JDDNode acceptanceVector_L = JDD.Constant(0);
		for (int i = 0; i < numObjectives; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				ArrayList<JDDNode> statesH = new ArrayList<JDDNode>();
				ArrayList<JDDNode> statesL = new ArrayList<JDDNode>();
				for (int k = 0; k < dra[i].getAcceptance().size(); k++) {
					JDDNode tmpH = JDD.Constant(0);
					JDDNode tmpL = JDD.Constant(0);
					for (int j = 0; j < dra[i].size(); j++) {
						if (!dra[i].getAcceptance().get(k).getL().get(j)) {
							tmpH = JDD.SetVectorElement(tmpH, draDDRowVars[i], j, 1.0);
						}
						if (dra[i].getAcceptance().get(k).getK().get(j)) {
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
		List<JDDNode> allecs = mcMo.computeAllEcs(modelProduct, mcLtl, allstatesH, allstatesL, acceptanceVector_H, acceptanceVector_L, draDDRowVars, draDDColVars,
				opsAndBounds, numObjectives);

		// Create array to store BDDs for targets (i.e. accepting EC states); probability objectives only 
		List<JDDNode> targetDDs = new ArrayList<JDDNode>(numObjectives);
		for (int i = 0; i < numObjectives; i++) {
			if (opsAndBounds.isProbabilityObjective(i)) {
				mainLog.println("\nFinding accepting end components for " + pathFormulas.get(i).toString() + "...");
				targetDDs.add(mcMo.computeAcceptingEndComponent(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], allecs, allstatesH.get(i),
						allstatesL.get(i), mcLtl, conflictformulae > 1));
			} else {
				// (not used currently)
				// Fixme: maybe not efficient
				if (pathFormulas.get(i) != null) {
					JDDNode dd = checkExpressionDD(pathFormulas.get(i), model.getReach().copy());
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
			mcMo.checkConflictsInObjectives(modelProduct, mcLtl, conflictformulae, numObjectives, opsAndBounds, dra, draDDRowVars, draDDColVars, targetDDs, allstatesH,
					allstatesL, multitargetDDs, multitargetIDs);
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

		Object value;
		try {
			// Do multi-objective computation
			value = mcMo.computeMultiReachProbs(modelProduct, mcLtl, transRewardsListProduct, modelProduct.getStart(), targetDDs, multitargetDDs, multitargetIDs, opsAndBounds,
			                                    conflictformulae > 1);
		} finally {
			// Deref, clean up
			JDD.Deref(stateOfInterest);
			if (modelProduct != null && modelProduct != model)
				modelProduct.clear();
			for (int i = 0; i < numObjectives; i++) {
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
		}

		if (value instanceof TileList) {
			if (opsAndBounds.numberOfNumerical() == 2) {			
				synchronized(TileList.getStoredTileLists()) {
					TileList.storedFormulasX.add(exprs.get(0));
					TileList.storedFormulasY.add(exprs.get(1));

					TileList.storedFormulas.add(exprs);
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

	/**
	 * Extract the information from the operator defining one objective of a multi-objective query,
	 * store the info in the passed in arrays and so some checks.
	 *  
	 * @param exprQuant The operator for the objective
	 * @param opsAndBounds Where to add info about ops/bounds
	 * @param transRewardsList Where to add the transition rewards (R operators only)
	 * @param pathFormulas Where to store the path formulas (for P operators; null for R operators)
	 * @param origPosition The position (starting from 0) at which this operand occured in the call of multi(...)
	 */
	protected void extractInfoFromMultiObjectiveOperand(ExpressionQuant exprQuant, OpsAndBoundsList opsAndBounds, List<JDDNode> transRewardsList,
			List<Expression> pathFormulas, int origPosition) throws PrismException
	{
		ExpressionProb exprProb = null;
		ExpressionReward exprReward = null;
		
		// Check if it's a P or an R operator
		if (exprQuant instanceof ExpressionProb) {
			exprProb = (ExpressionProb) exprQuant;
			exprReward = null;
		} else if (exprQuant instanceof ExpressionReward) {
			exprReward = (ExpressionReward) exprQuant;
			exprProb = null;
		} else {
			throw new PrismException("Multi-objective properties can only contain P and R operators");
		}

		// For a reward objective, store the transition rewards
		if (exprReward != null) {
			Object rs = exprReward.getRewardStructIndex();
			// Check there are no state rewards (which are not currently supported), and throw an exception if there are
			JDDNode stateRewards = getStateRewardsByIndexObject(rs, model, constantValues);
			if (stateRewards != null && !stateRewards.equals(JDD.ZERO)) {
				throw new PrismNotSupportedException("Multi-objective model checking does not support state rewards; please convert to transition rewards");
			}
			// Add transition rewards to list
			transRewardsList.add(getTransitionRewardsByIndexObject(rs, model, constantValues));
		}
		
		// Check that the temporal/reward operator is supported, and store step bounds if present
		int stepBound = 0;
		if (exprProb != null) {
			// F<=k is allowed
			Expression expr = exprProb.getExpression();
			if (expr.isSimplePathFormula() && Expression.isReach(expr)) {
				ExpressionTemporal exprTemp = ((ExpressionTemporal) expr);
				if (exprTemp.getLowerBound() != null) {
					throw new PrismException("Lower time bounds are not supported in multi-objective queries");
				}
				if (exprTemp.getUpperBound() != null) {
					stepBound = exprTemp.getUpperBound().evaluateInt(constantValues);
				} else {
					stepBound = -1;
				}
			} else {
				if (Expression.containsTemporalTimeBounds(expr)) {
					throw new PrismException("Time bounds in multi-objective queries can only be on F or C operators");
				} else {
					stepBound = -1;
				}
			}
		}
		if (exprReward != null) {
			ExpressionTemporal exprTemp = ((ExpressionTemporal) exprReward.getExpression());
			// We only allow C or C<=k reward operators, others such as F are not supported currently
			if (exprTemp.getOperator() != ExpressionTemporal.R_C) {
				throw new PrismException("Only the C and C>=k reward operators are currently supported for multi-objective properties (not "
						+ exprTemp.getOperatorSymbol() + ")");
			}
			// R [ C<=k ]
			if (exprTemp.getUpperBound() != null) {
				stepBound = exprTemp.getUpperBound().evaluateInt(constantValues);
			}
			// R [ C ]
			else {
				stepBound = -1;
			}
		}
		
		// Get/check/store info about relational operator and bound
		OpRelOpBound opInfo = exprQuant.getRelopBoundInfo(constantValues);
		RelOp relOp = opInfo.getRelOp();
		if (relOp.isStrict()) {
			throw new PrismException("Multi-objective properties can not use strict inequalities on P/R operators");
		}
		Operator op;
		if (relOp == RelOp.MAX) {
			op = (exprProb != null) ? Operator.P_MAX : Operator.R_MAX;
		} else if (relOp == RelOp.GEQ) {
			op = (exprProb != null) ? Operator.P_GE : Operator.R_GE;
		} else if (relOp == RelOp.MIN) {
			op = (exprProb != null) ? Operator.P_MIN : Operator.R_MIN;
		} else if (relOp == RelOp.LEQ) {
			op = (exprProb != null) ? Operator.P_LE : Operator.R_LE;
		} else {
			throw new PrismException("Multi-objective properties can only contain P/R operators with max/min=? or lower/upper probability bounds");
		}
		// Find bound
		double p = opInfo.isNumeric() ? -1.0 : opInfo.getBound();
		// Subtract bound from 1 if of the form P<=p
		if (opInfo.isProbabilistic() && opInfo.getRelOp().isUpperBound()) {
			p = 1 - p;
		}
		// Store bound
		opsAndBounds.add(opInfo, op, p, stepBound, origPosition);

		// Finally, extract path formulas
		if (exprProb != null) {
			pathFormulas.add(exprProb.getExpression());
		}
		if (exprReward != null) {
			pathFormulas.add(null);
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
		OpRelOpBound opInfo = new OpRelOpBound("P", RelOp.GEQ, 0.0);
		opsAndBounds.add(opInfo, Operator.P_GE, 0.0, -1, -1);
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
				modelProduct.exportToFile(Prism.EXPORT_PLAIN, true, new File(prism.getExportProductTransFilename()));
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"");
			}
		}
		if (prism.getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + prism.getExportProductStatesFilename() + "\"...");
			PrismFileLog out = new PrismFileLog(prism.getExportProductStatesFilename());
			modelProduct.exportStates(Prism.EXPORT_PLAIN, out);
			out.close();
		}
	}

	// Model checking functions
	
	/**
	 * Compute probabilities for the contents of a P operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkProbPathFormula(Expression expr, boolean qual, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and whether we want to use the corresponding algorithms
		boolean useSimplePathAlgo = expr.isSimplePathFormula();

		if (useSimplePathAlgo &&
		    prism.getSettings().getBoolean(PrismSettings.PRISM_PATH_VIA_AUTOMATA) &&
		    LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expr)) {
			// If PRISM_PATH_VIA_AUTOMATA is true, we want to use the LTL engine
			// whenever possible
			useSimplePathAlgo = false;
		}

		if (useSimplePathAlgo) {
			return checkProbPathFormulaSimple(expr, qual, min, statesOfInterest);
		} else {
			return checkProbPathFormulaLTL(expr, qual, min, statesOfInterest);
		}
	}

	/**
	 * Compute probabilities for a simple, non-LTL path operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkProbPathFormulaSimple(Expression expr, boolean qual, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		boolean negated = false;
		StateValues probs = null;

		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);

		// Negation
		if (expr instanceof ExpressionUnaryOp &&
		    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
			// mark as negated, switch from min to max and vice versa
			negated = true;
			min = !min;
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}

		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(exprTemp, min, statesOfInterest);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(exprTemp, min, statesOfInterest);
				} else {
					probs = checkProbUntil(exprTemp, qual, min, statesOfInterest);
				}
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		if (negated) {
			// Subtract from 1 for negation
			probs.subtractFromOne();
		}

		return probs;
	}

	/**
	 * Compute probabilities for a next operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkProbNext(ExpressionTemporal expr, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		JDDNode b;
		StateValues probs = null;

		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);

		// model check operand first, statesOfInterest = all
		b = checkExpressionDD(expr.getOperand2(), model.getReach().copy());

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
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkProbBoundedUntil(ExpressionTemporal expr, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		JDDNode b1, b2;
		StateValues probs = null;
		Integer lowerBound;
		IntegerBound bounds;
		int i;

		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);

		// get and check bounds information
		bounds = IntegerBound.fromExpressionTemporal(expr, constantValues, true);

		// model check operands first, statesOfInterest = all
		b1 = checkExpressionDD(expr.getOperand1(), model.getReach().copy());
		try {
			b2 = checkExpressionDD(expr.getOperand2(), model.getReach().copy());
		} catch (PrismException e) {
			JDD.Deref(b1);
			throw e;
		}

		// print out some info about num states
		// mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1,
		// allDDRowVars.n()));
		// mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2,
		// allDDRowVars.n()) + " states\n");

		if (bounds.hasLowerBound()) {
			lowerBound = bounds.getLowestInteger();
		} else {
			lowerBound = 0;
		}

		Integer windowSize = null;  // unbounded
		if (bounds.hasUpperBound()) {
			windowSize = bounds.getHighestInteger() - lowerBound;
		}

		// compute probabilities for Until<=windowSize
		if (windowSize == null) {
			// unbounded
			try {
				probs = checkProbUntil(b1, b2, false, min);
			} catch (PrismException e) {
				JDD.Deref(b1);
				JDD.Deref(b2);
				throw e;
			}
		} else if (windowSize == 0) {
			// the trivial case: windowSize = 0
			// prob is 1 in b2 states, 0 otherwise
			JDD.Ref(b2);
			probs = new StateValuesMTBDD(b2, model);
		} else {
			try {
				probs = computeBoundedUntilProbs(trans, trans01, b1, b2, windowSize, min);
			} catch (PrismException e) {
				JDD.Deref(b1);
				JDD.Deref(b2);
				throw e;
			}
		}

		// perform lowerBound restricted next-step computations to
		// deal with lower bound.
		if (lowerBound > 0) {
			for (i = 0; i < lowerBound; i++) {
				probs = computeRestrictedNext(trans, b1, probs, min);
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
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkProbUntil(ExpressionTemporal expr, boolean qual, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		JDDNode b1, b2;
		StateValues probs = null;

		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);

		// model check operands first
		b1 = checkExpressionDD(expr.getOperand1(), model.getReach().copy());
		try {
			b2 = checkExpressionDD(expr.getOperand2(), model.getReach().copy());
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
	 * Compute probabilities for an (unbounded) until operator (b1 U b2).
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 *
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
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkProbPathFormulaLTL(Expression expr, boolean qual, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct = null, probs = null;
		Vector<JDDNode> labelDDs = new Vector<JDDNode>();
		DA<BitSet, ? extends AcceptanceOmega> da;
		NondetModel modelProduct;
		NondetModelChecker mcProduct;
		JDDNode startMask;
		JDDVars daDDRowVars, daDDColVars;
		int i;
		long l;

		// For min probabilities, need to negate the formula
		// (But check fairness setting since this may affect min/max)
		// (add parentheses to allow re-parsing if required)
		if (min && !fairness) {
			expr = Expression.Not(Expression.Parenth(expr));
		}

		// Can't do "dfa" properties yet
		if (expr instanceof ExpressionFunc && ((ExpressionFunc) expr).getName().equals("dfa")) {
			throw new PrismException("Model checking for \"dfa\" specifications not supported yet");
		}

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(prism);
		
		// Convert LTL formula to deterministic automaton (DA)
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.BUCHI,
				AcceptanceType.RABIN,
				AcceptanceType.GENERALIZED_RABIN,
				AcceptanceType.REACH
		};
		try {
			da = mcLtl.constructDAForLTLFormula(this, model, expr, labelDDs, allowedAcceptance);
		} catch (Exception e) {
			JDD.Deref(statesOfInterest);
			throw e;
		}

		// Build product of MDP and automaton
		l = System.currentTimeMillis();
		mainLog.println("\nConstructing MDP-"+da.getAutomataType()+" product...");
		daDDRowVars = new JDDVars();
		daDDColVars = new JDDVars();
		modelProduct = mcLtl.constructProductMDP(da, model, labelDDs, daDDRowVars, daDDColVars, statesOfInterest);
		l = System.currentTimeMillis() - l;
		mainLog.println("Time for product construction: " + l / 1000.0 + " seconds.");
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
			PrismFileLog out = new PrismFileLog(prism.getExportProductStatesFilename());
			modelProduct.exportStates(Prism.EXPORT_PLAIN, out);
			out.close();
		}

		// Find accepting states + compute reachability probabilities
		AcceptanceOmegaDD acceptance = da.getAcceptance().toAcceptanceDD(daDDRowVars);
		JDDNode acc;
		if (acceptance instanceof AcceptanceReachDD) {
			mainLog.println("\nSkipping accepting MEC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReachDD) acceptance).getGoalStates();
			JDD.Ref(modelProduct.getReach());
			acc = JDD.And(acc, modelProduct.getReach());
		} else {
			mainLog.println("\nFinding accepting end components...");
			acc = mcLtl.findAcceptingECStates(acceptance, modelProduct, daDDRowVars, daDDColVars, fairness);
		}
		acceptance.clear();
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new NondetModelChecker(prism, modelProduct, null);
		probsProduct = mcProduct.checkProbUntil(modelProduct.getReach(), acc, qual, min && fairness);

		// subtract from 1 if we're model checking a negated formula for regular Pmin
		if (min && !fairness) {
			probsProduct.subtractFromOne();
		}

		// Output vector over product, if required
		if (prism.getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + prism.getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(prism.getExportProductVectorFilename());
				probsProduct.print(out, false, false, false, false);
				out.close();
		}
		
		// Convert probability vector to original model
		// First, filter over DRA start states
		startMask = mcLtl.buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(model.getReach());
		startMask = JDD.And(model.getReach(), startMask);
		probsProduct.filter(startMask);
		// Then sum over DD vars for the DRA state
		probs = probsProduct.sumOverDDVars(daDDRowVars, model);

		// Deref, clean up
		probsProduct.clear();
		modelProduct.clear();
		for (i = 0; i < labelDDs.size(); i++) {
			JDD.Deref(labelDDs.get(i));
		}
		JDD.Deref(acc);
		JDD.Deref(startMask);
		daDDRowVars.derefAll();
		daDDColVars.derefAll();

		return probs;
	}

	/**
	 * Compute rewards for a cumulative reward operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkRewardCumul(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		int time; // time
		StateValues rewards = null;

		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);

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
	 * Compute rewards for a total reward operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkRewardTotal(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);
		StateValues rewards = computeTotalRewards(trans, trans01, transActions, stateRewards, transRewards, min);
		return rewards;
	}

	/**
	 * Compute rewards for an instantaneous reward operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkRewardInst(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		int time; // time bound
		StateValues rewards = null;

		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);

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
	 * Compute rewards for a reachability reward operator (either simple reachability or co-safe LTL).
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkRewardPathFormula(Expression expr, JDDNode stateRewards, JDDNode transRewards, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		if (Expression.isReach(expr)) {
			return checkRewardReach((ExpressionTemporal) expr, stateRewards, transRewards, min, statesOfInterest);
		}
		else if (Expression.isCoSafeLTLSyntactic(expr, true)) {
			return checkRewardCoSafeLTL(expr, stateRewards, transRewards, min, statesOfInterest);
		}
		JDD.Deref(statesOfInterest);
		throw new PrismException("R operator contains a path formula that is not syntactically co-safe: " + expr);
	}

	/**
	 * Compute rewards for a reachability reward operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkRewardReach(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		JDDNode b;
		StateValues rewards = null;

		if (fairness && !min) {
			// Rmax with fairness not supported; Rmin computation is unaffected
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Maximum reward computation currently not supported under fairness.");
		}

		// currently, ignore statesOfInterest
		JDD.Deref(statesOfInterest);

		// No time bounds allowed
		if (expr.hasBounds()) {
			throw new PrismNotSupportedException("R operator cannot contain a bounded F operator: " + expr);
		}
		
		// model check operand first, statesOfInterest = all
		b = checkExpressionDD(expr.getOperand2(), model.getReach().copy());

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

	/**
	 * Compute rewards for a co-safe LTL reward operator.
	 * The result will have valid results at least for the states of interest (use model.getReach().copy() for all reachable states)
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	protected StateValues checkRewardCoSafeLTL(Expression expr, JDDNode stateRewards, JDDNode transRewards, boolean min, JDDNode statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues rewardsProduct = null, rewards = null;
		Vector<JDDNode> labelDDs = new Vector<JDDNode>();
		DA<BitSet, AcceptanceReach> da;
		LTLProduct<NondetModel> modelProduct;
		NondetModelChecker mcProduct;
		long l;

		if (fairness && !min) {
			// Rmax with fairness not supported; Rmin computation is unaffected
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Maximum reward computation currently not supported under fairness.");
		}

		if (Expression.containsTemporalTimeBounds(expr)) {
			if (model.getModelType().continuousTime()) {
				JDD.Deref(statesOfInterest);
				throw new PrismException("DA construction for time-bounded operators not supported for " + model.getModelType()+".");
			}

			if (expr.isSimplePathFormula()) {
				// Convert simple path formula to canonical form,
				// DA is then generated by LTL2RabinLibrary.
				//
				// The conversion to canonical form has to happen here, because once
				// checkMaximalStateFormulas has been called, the formula should not be modified
				// anymore, as converters may expect that the generated labels for maximal state
				// formulas only appear positively
				expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);
			} else {
				JDD.Deref(statesOfInterest);
				throw new PrismException("Time-bounded operators not supported in LTL: " + expr);
			}
		}

		// Can't do "dfa" properties yet
		if (expr instanceof ExpressionFunc && ((ExpressionFunc) expr).getName().equals("dfa")) {
			JDD.Deref(statesOfInterest);
			throw new PrismException("Model checking for \"dfa\" specifications not supported yet");
		}

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(prism);

		// Model check maximal state formulas and construct DFA, with the special
		// handling needed for cosafety reward translation
		da = mcLtl.constructDFAForCosafetyRewardLTL(this, model, expr, labelDDs);

		// If required, export DA
		if (prism.getSettings().getExportPropAut()) {
			mainLog.println("Exporting DA to file \"" + prism.getSettings().getExportPropAutFilename() + "\"...");
			PrintStream out = PrismUtils.newPrintStream(prism.getSettings().getExportPropAutFilename());
			da.print(out, prism.getSettings().getExportPropAutType());
			out.close();
			//da.printDot(new java.io.PrintStream("da.dot"));
		}

		// Build product of MDP and automaton
		modelProduct = mcLtl.constructProductMDP(model, da, labelDDs, statesOfInterest);
		// Output product, if required
		if (prism.getExportProductTrans()) {
			try {
				mainLog.println("\nExporting product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"...");
				modelProduct.getProductModel().exportToFile(Prism.EXPORT_PLAIN, true, new File(prism.getExportProductTransFilename()));
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export product transition matrix to file \"" + prism.getExportProductTransFilename() + "\"");
			}
		}
		if (prism.getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + prism.getExportProductStatesFilename() + "\"...");
			PrismFileLog out = new PrismFileLog(prism.getExportProductStatesFilename());
			modelProduct.getProductModel().exportStates(Prism.EXPORT_PLAIN, out);
			out.close();
		}

		// Adapt reward info to product model
		JDDNode stateRewardsProduct = JDD.Apply(JDD.TIMES, stateRewards.copy(), modelProduct.getProductModel().getReach().copy());
		JDDNode transRewardsProduct = JDD.Apply(JDD.TIMES, transRewards.copy(), modelProduct.getProductModel().getTrans01().copy());
		
		// Find accepting states + compute reachability rewards
		AcceptanceReachDD acceptance = (AcceptanceReachDD) modelProduct.getProductAcceptance();
		// acc is already restricted to the product model's reachable states
		JDDNode acc = acceptance.getGoalStates();

		mainLog.println("\nComputing reachability rewards...");
		mcProduct = createNewModelChecker(prism, modelProduct.getProductModel(), null);
		rewardsProduct = mcProduct.computeReachRewards(modelProduct.getProductModel().getTrans(),
		                                               modelProduct.getProductModel().getTransActions(),
		                                               modelProduct.getProductModel().getTrans01(),
		                                               stateRewardsProduct,
		                                               transRewardsProduct,
		                                               acc,
		                                               min);

		// Convert reward vector to original model
		rewards = modelProduct.projectToOriginalModel(rewardsProduct);

		// Deref, clean up
		JDD.Deref(stateRewardsProduct);
		JDD.Deref(transRewardsProduct);
		modelProduct.clear();
		JDD.Deref(acc);

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
	protected StateValues computeRestrictedNext(JDDNode tr, JDDNode a, StateValues x, boolean min)
	{
		JDDNode tmp;
		StateValuesMTBDD probs = null;

		// ensure that values are given in MTBDD format
		StateValuesMTBDD ddX = x.convertToStateValuesMTBDD();

		tmp = ddX.getJDDNode();
		JDD.Ref(tmp);
		tmp = JDD.PermuteVariables(tmp, allDDRowVars, allDDColVars);
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

		// label is 0/1 BDD, MIN sets all values to 0 for states not in a
		JDD.Ref(a);
		tmp = JDD.Apply(JDD.MIN, tmp, a);

		probs = new StateValuesMTBDD(tmp, model);
		ddX.clear();
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

		boolean doPmaxQuotient = getSettings().getBoolean(PrismSettings.PRISM_PMAX_QUOTIENT);

		if (doIntervalIteration) {
			if (!(precomp && prob0 && prob1)) {
				throw new PrismNotSupportedException("Precomputations (Prob0 & Prob1) must be enabled for interval iteration");
			}

			if (!min) {
				// for Pmax and interval iteration, pmaxQuotient is required
				doPmaxQuotient = true;
			}
		}

		if (doPmaxQuotient && min) {
			// don't do pmaxQuotient for min
			doPmaxQuotient = false;
		}

		if (doPmaxQuotient) {
			if (!(precomp && prob0 && prob1)) {
				throw new PrismNotSupportedException("Precomputations (Prob0 & Prob1) must be enabled for -pmaxquotient setting");
			}
		}

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
				MDPQuotient transform = null;
				NondetModel transformed = null;
				JDDNode yesInQuotient = null;
				JDDNode maybeInQuotient = null;

				if (doPmaxQuotient) {
					if (!tr.equals(model.getTrans()) ||
					    !tra.equals(model.getTransActions()) ||
					    !tr01.equals(model.getTrans01())) {
						throw new PrismException("Can currently not compute MEC quotient for changed functions");
					}

					mainLog.println("\nBuilding quotient MDP, collapsing maximal end components as well as yes and no states...");
					StopWatch ecWatch = new StopWatch(mainLog);
					ecWatch.start("computing maximal end components");
					ECComputer ec = ECComputer.createECComputer(this, model);
					// find MECs in the maybe states
					ec.computeMECStates(maybe);
					ecWatch.stop("found " + ec.getMECStates().size() + " MECs");

					List<JDDNode> ecs = new ArrayList<JDDNode>(ec.getMECStates());

					ecs.add(yes.copy());
					ecs.add(no.copy());

					StopWatch watchTransform = new StopWatch(mainLog);
					watchTransform.start("building MEC quotient");
					transform = MDPQuotient.transform(this, model, ecs, model.getReach().copy());
					watchTransform.stop();

					if (false) {
						try {
							model.exportToFile(Prism.EXPORT_DOT, true, new File("model.dot"));
							transform.getTransformedModel().exportToFile(Prism.EXPORT_DOT, true, new File("quotient.dot"));
						} catch (FileNotFoundException e) {
						}
					}
					transformed = transform.getTransformedModel();

					mainLog.println("\nQuotient MDP:");
					transformed.printTransInfo(mainLog);

					yesInQuotient = transform.mapStateSetToQuotient(yes.copy());
					maybeInQuotient = transform.mapStateSetToQuotient(maybe.copy());
				}

				switch (engine) {
				case Prism.MTBDD:
					if (doIntervalIteration) {
						if (transform != null) {
							probsMTBDD = PrismMTBDD.NondetUntilInterval(transformed.getTrans(),
							                                            transformed.getODD(),
							                                            transformed.getNondetMask(),
							                                            transformed.getAllDDRowVars(),
							                                            transformed.getAllDDColVars(),
							                                            transformed.getAllDDNondetVars(),
							                                            yesInQuotient,
							                                            maybeInQuotient,
							                                            min,
							                                            prism.getIntervalIterationFlags());
						} else {
							probsMTBDD = PrismMTBDD.NondetUntilInterval(tr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min, prism.getIntervalIterationFlags());
						}
					} else {
						if (transform != null) {
							probsMTBDD = PrismMTBDD.NondetUntil(transformed.getTrans(),
							                                    transformed.getODD(),
							                                    transformed.getNondetMask(),
							                                    transformed.getAllDDRowVars(),
							                                    transformed.getAllDDColVars(),
							                                    transformed.getAllDDNondetVars(),
							                                    yesInQuotient,
							                                    maybeInQuotient,
							                                    min);
						} else {
							probsMTBDD = PrismMTBDD.NondetUntil(tr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
						}
					}
					probs = new StateValuesMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					IntegerVector strat = null;
					if (genStrat) {
						JDDNode ddStrat = JDD.ITE(yes, JDD.Constant(-2), JDD.Constant(-1));
						strat = new IntegerVector(ddStrat, allDDRowVars, odd);
						JDD.Deref(ddStrat);
					}
					if (doIntervalIteration) {
						if (transform != null) {
							strat = null;  // strategy generation with the quotient not yet supported
							probsDV = PrismSparse.NondetUntilInterval(transformed.getTrans(),
							                                          transformed.getTransActions(),
							                                          transformed.getSynchs(),
							                                          transformed.getODD(),
							                                          transformed.getAllDDRowVars(),
							                                          transformed.getAllDDColVars(),
							                                          transformed.getAllDDNondetVars(),
							                                          yesInQuotient,
							                                          maybeInQuotient,
							                                          min,
							                                          strat,
							                                          prism.getIntervalIterationFlags());
							probs = new StateValuesDV(probsDV, transformed);
						} else {
							probsDV = PrismSparse.NondetUntilInterval(tr, tra, model.getSynchs(), odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min, strat, prism.getIntervalIterationFlags());
							probs = new StateValuesDV(probsDV, model);
						}
					} else {
						if (transform != null) {
							strat = null;  // strategy generation with the quotient not yet supported
							probsDV = PrismSparse.NondetUntil(transformed.getTrans(),
							                                  transformed.getTransActions(),
							                                  transformed.getSynchs(),
							                                  transformed.getODD(),
							                                  transformed.getAllDDRowVars(),
							                                  transformed.getAllDDColVars(),
							                                  transformed.getAllDDNondetVars(),
							                                  yesInQuotient,
							                                  maybeInQuotient,
							                                  min,
							                                  strat);
							probs = new StateValuesDV(probsDV, transformed);
						} else {
							probsDV = PrismSparse.NondetUntil(tr, tra, model.getSynchs(), odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min, strat);
							probs = new StateValuesDV(probsDV, model);
						}
					}
					if (genStrat && strat != null) {
						result.setStrategy(new MDStrategyIV(model, strat));
					}
					break;
				case Prism.HYBRID:
					if (doIntervalIteration) {
						if (transform != null) {
							probsDV = PrismHybrid.NondetUntilInterval(transformed.getTrans(),
							                                          transformed.getODD(),
							                                          transformed.getAllDDRowVars(),
							                                          transformed.getAllDDColVars(),
							                                          transformed.getAllDDNondetVars(),
							                                          yesInQuotient,
							                                          maybeInQuotient,
							                                          min,
							                                          prism.getIntervalIterationFlags());
							probs = new StateValuesDV(probsDV, transformed);
						} else {
							probsDV = PrismHybrid.NondetUntilInterval(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min, prism.getIntervalIterationFlags());
							probs = new StateValuesDV(probsDV, model);
						}
					} else {
						if (transform != null) {
							probsDV = PrismHybrid.NondetUntil(transformed.getTrans(),
							                                  transformed.getODD(),
							                                  transformed.getAllDDRowVars(),
							                                  transformed.getAllDDColVars(),
							                                  transformed.getAllDDNondetVars(),
							                                  yesInQuotient,
							                                  maybeInQuotient,
							                                  min);
							probs = new StateValuesDV(probsDV, transformed);
						} else {
							probsDV = PrismHybrid.NondetUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
							probs = new StateValuesDV(probsDV, model);
						}
					}
					break;
				default:
					throw new PrismException("Unknown engine");
				}

				if (transform != null) {
					// we have to project back to the original
					probs = transform.projectToOriginalModel(probs);
					transform.clear();
					JDD.Deref(yesInQuotient, maybeInQuotient);
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
				throw new PrismNotSupportedException("MTBDD engine does not yet support this type of property (use the sparse engine instead)");
			case Prism.SPARSE:
				rewardsDV = PrismSparse.NondetCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, allDDNondetVars, time, min);
				rewards = new StateValuesDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				throw new PrismNotSupportedException("Hybrid engine does not yet support this type of property (use the sparse engine instead)");
			default:
				throw new PrismException("Unknown engine");
			}
		} catch (PrismException e) {
			throw e;
		}

		return rewards;
	}

	// compute total rewards

	protected StateValues computeTotalRewards(JDDNode tr, JDDNode tr01, JDDNode tra, JDDNode sr, JDDNode trr, boolean min) throws PrismException
	{
		if (min) {
			throw new PrismNotSupportedException("Expected minimum total reward (C) is not yet supported for MDPs.");
		} else {
			// max. We don't know if there are positive ECs, so we can't skip precomputation
			return computeTotalRewardsMax(tr, tr01, tra, sr, trr, false);
		}
	}

	protected StateValues computeTotalRewardsMax(JDDNode tr, JDDNode tr01, JDDNode tra, JDDNode sr, JDDNode trr, boolean noPositiveECs) throws PrismException
	{
		JDDNode inf, maybe;
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;
		// Local copy of setting
		int engine = this.engine;

		// For Rmax[ C ] computation, fairness does not affect the result

		if (doIntervalIteration) {
			throw new PrismNotSupportedException("Interval iteration for total rewards is currently not supported");
		}

		// Start expected total reward
		mainLog.println("\nStarting total expected reward (max)...");

		long timer = System.currentTimeMillis();

		if (noPositiveECs) {
			// no inf states
			inf = JDD.Constant(0);
			maybe = reach.copy();
		} else {
			mainLog.println("Precomputation: Find positive end components...");

			long timerPre = System.currentTimeMillis();

			JDDNode posRewStates = JDD.GreaterThan(sr.copy(), 0);
			JDDNode posRewTrans = JDD.GreaterThan(trr.copy(), 0);

			ECComputer ecComp = new ECComputerDefault(prism, reach, trans, trans01,
			                                          model.getAllDDRowVars(),
			                                          model.getAllDDColVars(),
			                                          model.getAllDDNondetVars());
			ecComp.computeMECStates();

			JDDNode positiveECs = JDD.Constant(0);

			for (JDDNode ec : ecComp.getMECStates()) {
				boolean positive = false;
				if (JDD.AreIntersecting(ec, posRewStates)) {
					positive = true;
				} else {
					// check for positive transition rewards
					JDDNode ecTrans = ecComp.getStableTransitions(ec.copy());
					if (JDD.AreIntersecting(ecTrans, posRewTrans)) {
						positive = true;
					}
					JDD.Deref(ecTrans);
				}

				if (positive) {
					positiveECs = JDD.Or(positiveECs, ec.copy());
				}
				JDD.Deref(ec);
			}

			JDD.Deref(posRewStates, posRewTrans);

			// inf = Pmax[ <> positiveECs ] > 0
			//     = ! (Pmax[ <> positiveECs ] = 0)
			maybe = PrismMTBDD.Prob0A(trans01, reach,  // Pmax[ <> positiveECs ] = 0
			                          model.getAllDDRowVars(),
			                          model.getAllDDColVars(),
			                          model.getAllDDNondetVars(),
			                          reach,
			                          positiveECs);
			inf = JDD.And(reach.copy(), JDD.Not(maybe.copy()));  // !(Pmax[ <> positive ECs ] = 0) = Pmax[ <> positiveECs ] > 0

			JDD.Deref(positiveECs);

			timerPre = System.currentTimeMillis() - timerPre;
			mainLog.print("Total expected reward precomputation took " + timerPre / 1000.0 + " seconds, ");
			mainLog.print(JDD.GetNumMintermsString(inf, allDDRowVars.n()) + " infinite states, ");
			mainLog.println(JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + " states remaining.");
		}

		// if maybe is empty, we have the rewards already
		if (maybe.equals(JDD.ZERO)) {
			rewards = new StateValuesMTBDD(JDD.ITE(inf.copy(), JDD.PlusInfinity(), JDD.Constant(0)), model);
			JDD.Deref(maybe, inf);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			mainLog.println("\nComputing remaining total rewards...");
			// switch engine, if necessary
			if (engine == Prism.HYBRID) {
				mainLog.println("Switching engine since hybrid engine does yet support this computation...");
				engine = Prism.SPARSE;
			}
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.NondetReachReward(tr, sr, trr, odd, nondetMask,
					                                            allDDRowVars, allDDColVars, allDDNondetVars,
					                                            JDD.ZERO,  // goal = empty set
					                                            inf,
					                                            maybe,
					                                            false);  // max
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.NondetReachReward(tr, tra, model.getSynchs(), sr, trr, odd,
					                                          allDDRowVars, allDDColVars, allDDNondetVars,
					                                          JDD.ZERO,  // goal = empty set
					                                          inf,
					                                          maybe,
					                                          false);  // max
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					throw new PrismNotSupportedException("Hybrid engine does not yet support this type of property (use sparse or MTBDD engine instead)");
				default:
					throw new PrismException("Unknown engine");
				}
			} finally {
				JDD.Deref(inf);
				JDD.Deref(maybe);
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("Time for total reward computation: " + timer/1000.0 + " seconds.");

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

		if (doIntervalIteration && min) {
			throw new PrismNotSupportedException("Currently, Rmin is not supported with interval iteration and the symbolic engines");
		}

		// If required, export info about target states
		if (prism.getExportTarget()) {
			JDDNode labels[] = { model.getStart(), b };
			String labelNames[] = { "init", "target" };
			try {
				mainLog.println("\nExporting target states info to file \"" + prism.getExportTargetFilename() + "\"...");
				PrismMTBDD.ExportLabels(labels, labelNames, "l", model.getAllDDRowVars(), model.getODD(), Prism.EXPORT_PLAIN, prism.getExportTargetFilename());
			} catch (FileNotFoundException e) {
				mainLog.printWarning("Could not export target to file \"" + prism.getExportTargetFilename() + "\"");
			}
		}

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

		JDDNode lower = null;
		JDDNode upper = null;

		// if maybe is empty, we have the rewards already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(inf);
			rewards = new StateValuesMTBDD(JDD.ITE(inf, JDD.PlusInfinity(), JDD.Constant(0)), model);
		}
		// otherwise we compute the actual rewards
		else {

			if (doIntervalIteration) {
				OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(this);

				double upperBound;
				if (iiOptions.hasManualUpperBound()) {
					upperBound = iiOptions.getManualUpperBound();
					getLog().printWarning("Upper bound for interval iteration manually set to " + upperBound);
				} else {
					upperBound = ProbModelChecker.computeReachRewardsUpperBound(this, model, tr, sr, trr, b, maybe);
				}
				upper = JDD.ITE(maybe.copy(), JDD.Constant(upperBound), JDD.Constant(0));

				double lowerBound;
				if (iiOptions.hasManualLowerBound()) {
					lowerBound = iiOptions.getManualLowerBound();
					getLog().printWarning("Lower bound for interval iteration manually set to " + lowerBound);
				} else {
					lowerBound = 0.0;
				}
				lower = JDD.ITE(maybe.copy(), JDD.Constant(lowerBound), JDD.Constant(0));
			}

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
					if (doIntervalIteration) {
						rewardsMTBDD = PrismMTBDD.NondetReachRewardInterval(tr, sr, trr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b, inf, maybe, lower, upper, min, prism.getIntervalIterationFlags());
					} else {
						rewardsMTBDD = PrismMTBDD.NondetReachReward(tr, sr, trr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b, inf, maybe, min);
					}
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					if (doIntervalIteration) {
						rewardsDV = PrismSparse.NondetReachRewardInterval(tr, tra, model.getSynchs(), sr, trr, odd, allDDRowVars, allDDColVars, allDDNondetVars, b, inf,
								maybe, lower, upper, min, prism.getIntervalIterationFlags());
					} else {
						rewardsDV = PrismSparse.NondetReachReward(tr, tra, model.getSynchs(), sr, trr, odd, allDDRowVars, allDDColVars, allDDNondetVars, b, inf,
								maybe, min);
					}
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
				if (lower != null) JDD.Deref(lower);
				if (upper != null) JDD.Deref(upper);
				throw e;
			}
		}

		if (zeroCostEndComponents != null)
			for (JDDNode zcec : zeroCostEndComponents)
				JDD.Deref(zcec);

		if (doIntervalIteration) {
			double max_v = rewards.maxFiniteOverBDD(maybe);
			if (max_v != Double.NEGATIVE_INFINITY) {
				mainLog.println("Maximum finite value in solution vector at end of interval iteration: " + max_v);
			}
		}

		// derefs
		JDD.Deref(inf);
		JDD.Deref(maybe);
		if (lower != null) JDD.Deref(lower);
		if (upper != null) JDD.Deref(upper);

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

	
}

// ------------------------------------------------------------------------------
