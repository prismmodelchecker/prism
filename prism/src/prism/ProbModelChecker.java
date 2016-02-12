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

package prism;

import hybrid.PrismHybrid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceReachDD;
import acceptance.AcceptanceType;
import automata.DA;
import automata.LTL2DA;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.Expression;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.PropertiesFile;
import parser.ast.RelOp;
import parser.type.TypeBool;
import parser.type.TypePathBool;
import parser.type.TypePathDouble;
import sparse.PrismSparse;
import dv.DoubleVector;

/*
 * Model checker for DTMCs.
 */
public class ProbModelChecker extends NonProbModelChecker
{
	// Model (DTMC or CTMC)
	protected ProbModel model;

	// Options (in addition to those inherited from StateModelChecker):

	// Use 0,1 precomputation algorithms?
	// if 'precomp' is false, this disables all (non-essential) use of prob0/prob1
	// if 'precomp' is true, the values of prob0/prob1 determine what is used
	// (currently prob0/prob are not under user control)
	protected boolean precomp;
	protected boolean prob0;
	protected boolean prob1;
	// Do BSCC computation?
	protected boolean bsccComp;

	// Constructor

	public ProbModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);
		if (!(m instanceof ProbModel)) {
			throw new PrismException("Wrong model type passed to ProbModelChecker.");
		}
		model = (ProbModel) m;

		// Inherit some options from parent Prism object.
		// Store locally and/or pass onto native code.
		precomp = prism.getPrecomp();
		prob0 = prism.getProb0();
		prob1 = prism.getProb1();
		bsccComp = prism.getBSCCComp();
		PrismNative.setCompact(prism.getCompact());
		PrismNative.setLinEqMethod(prism.getLinEqMethod());
		PrismNative.setLinEqMethodParam(prism.getLinEqMethodParam());
		PrismNative.setTermCrit(prism.getTermCrit());
		PrismNative.setTermCritParam(prism.getTermCritParam());
		PrismNative.setMaxIters(prism.getMaxIters());
		PrismNative.setSBMaxMem(prism.getSBMaxMem());
		PrismNative.setNumSBLevels(prism.getNumSBLevels());
		PrismNative.setSORMaxMem(prism.getSORMaxMem());
		PrismNative.setNumSORLevels(prism.getNumSORLevels());
		PrismNative.setDoSSDetect(prism.getDoSSDetect());
		PrismNative.setExportAdv(prism.getExportAdv());
		PrismNative.setExportAdvFilename(prism.getExportAdvFilename());
	}

	// Override-able "Constructor"

	public ProbModelChecker createNewModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		return new ProbModelChecker(prism, m, pf);
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
		// S operator
		else if (expr instanceof ExpressionSS) {
			res = checkExpressionSteadyState((ExpressionSS) expr);
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
		// Get info from P operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);

		// Check for trivial (i.e. stupid) cases
		if (opInfo.isTriviallyTrue()) {
			mainLog.printWarning("Checking for probability " + opInfo.relOpBoundString() + " - formula trivially satisfies all states");
			JDD.Ref(reach);
			return new StateValuesMTBDD(reach, model);
		} else if (opInfo.isTriviallyFalse()) {
			mainLog.printWarning("Checking for probability " + opInfo.relOpBoundString() + " - formula trivially satisfies no states");
			return new StateValuesMTBDD(JDD.Constant(0), model);
		}

		// Print a warning if Pmin/Pmax used
		if (opInfo.getRelOp() == RelOp.MIN || opInfo.getRelOp() == RelOp.MAX) {
			mainLog.printWarning("\"Pmin=?\" and \"Pmax=?\" operators are identical to \"P=?\" for DTMCs/CTMCs");
		}

		// Compute probabilities
		boolean qual = opInfo.isQualitative() && precomp && prob0 && prob1;
		StateValues probs = checkProbPathFormula(expr.getExpression(), qual);

		// Print out probabilities
		if (prism.getVerbose()) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
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

	// R operator

	protected StateValues checkExpressionReward(ExpressionReward expr) throws PrismException
	{
		// Get info from R operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		
		// Get rewards
		Object rs = expr.getRewardStructIndex();
		JDDNode stateRewards = getStateRewardsByIndexObject(rs, model, constantValues);
		JDDNode transRewards = getTransitionRewardsByIndexObject(rs, model, constantValues);

		// Print a warning if Rmin/Rmax used
		if (opInfo.getRelOp() == RelOp.MIN || opInfo.getRelOp() == RelOp.MAX) {
			mainLog.printWarning("\"Rmin=?\" and \"Rmax=?\" operators are identical to \"R=?\" for DTMCs/CTMCs");
		}

		// Compute rewards
		StateValues rewards = null;
		Expression expr2 = expr.getExpression();
		if (expr2.getType() instanceof TypePathDouble) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr2;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_C:
				if (exprTemp.hasBounds()) {
					rewards = checkRewardCumul(exprTemp, stateRewards, transRewards);
				} else {
					rewards = checkRewardTotal(exprTemp, stateRewards, transRewards);
				}
				break;
			case ExpressionTemporal.R_I:
				rewards = checkRewardInst(exprTemp, stateRewards, transRewards);
				break;
			case ExpressionTemporal.R_S:
				rewards = checkRewardSS(exprTemp, stateRewards, transRewards);
				break;
			}
		} else if (expr2.getType() instanceof TypePathBool || expr2.getType() instanceof TypeBool) {
			rewards = checkRewardPathFormula(expr2, stateRewards, transRewards);
		}
		
		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		// Print out rewards
		if (prism.getVerbose()) {
			mainLog.print("\nRewards (non-zero only) for all states:\n");
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

	// S operator

	protected StateValues checkExpressionSteadyState(ExpressionSS expr) throws PrismException
	{
		// BSCC stuff
		List<JDDNode> bsccs = null;
		JDDNode notInBSCCs = null;
		// MTBDD stuff
		JDDNode b = null, bscc, sol, tmp;
		// Other stuff
		StateValues probs = null, totalProbs = null;
		int i, numBSCCs = 0;
		double d, probBSCCs[];

		// Get info from S operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);

		// Check for trivial (i.e. stupid) cases
		if (opInfo.isTriviallyTrue()) {
			mainLog.printWarning("Checking for probability " + opInfo.relOpBoundString() + " - formula trivially satisfies all states");
			JDD.Ref(reach);
			return new StateValuesMTBDD(reach, model);
		} else if (opInfo.isTriviallyFalse()) {
			mainLog.printWarning("Checking for probability " + opInfo.relOpBoundString() + " - formula trivially satisfies no states");
			return new StateValuesMTBDD(JDD.Constant(0), model);
		}

		try {
			// Model check argument
			b = checkExpressionDD(expr.getExpression());

			// Compute bottom strongly connected components (BSCCs)
			if (bsccComp) {
				SCCComputer sccComputer = prism.getSCCComputer(model);
				sccComputer.computeBSCCs();
				bsccs = sccComputer.getBSCCs();
				notInBSCCs = sccComputer.getNotInBSCCs();
				numBSCCs = bsccs.size();
			}
			// Unless we've been told to skip it
			else {
				mainLog.println("\nSkipping BSCC computation...");
				bsccs = new Vector<JDDNode>();
				JDD.Ref(reach);
				bsccs.add(reach);
				notInBSCCs = JDD.Constant(0);
				numBSCCs = 1;
			}

			// Compute steady-state probability for each BSCC...
			probBSCCs = new double[numBSCCs];
			for (i = 0; i < numBSCCs; i++) {
				mainLog.println("\nComputing steady state probabilities for BSCC " + (i + 1));
				bscc = bsccs.get(i);
				// Compute steady state probabilities
				probs = computeSteadyStateProbsForBSCC(trans, bscc);
				if (verbose) {
					mainLog.print("\nBSCC " + (i + 1) + " steady-state probabilities: \n");
					probs.print(mainLog);
				}
				// Sum probabilities over BDD b
				d = probs.sumOverBDD(b);
				probBSCCs[i] = d;
				mainLog.print("\nBSCC " + (i + 1) + " probability: " + d + "\n");
				// Free vector
				probs.clear();
			}

			// If every state is in a BSCC, it's much easier...
			if (notInBSCCs.equals(JDD.ZERO)) {
				mainLog.println("\nAll states are in BSCCs (so no reachability probabilities computed)");
				// There are more efficient ways to do this if we just create the solution BDD directly
				// But we actually build the prob vector so it can be printed out if necessary
				tmp = JDD.Constant(0);
				for (i = 0; i < numBSCCs; i++) {
					bscc = bsccs.get(i);
					JDD.Ref(bscc);
					tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(probBSCCs[i]), bscc));
				}
				totalProbs = new StateValuesMTBDD(tmp, model);
			}
			// Otherwise we have to do more work...
			else {

				// Initialise total probabilities vector
				switch (engine) {
				case Prism.MTBDD:
					totalProbs = new StateValuesMTBDD(JDD.Constant(0), model);
					break;
				case Prism.SPARSE:
					totalProbs = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
					break;
				case Prism.HYBRID:
					totalProbs = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
					break;
				}

				// Compute probabilities of reaching each BSCC...
				for (i = 0; i < numBSCCs; i++) {
					// Skip BSCCs with zero probability
					if (probBSCCs[i] == 0.0)
						continue;
					mainLog.println("\nComputing probabilities of reaching BSCC " + (i + 1));
					bscc = bsccs.get(i);
					// Compute probabilities
					probs = computeUntilProbs(trans, trans01, notInBSCCs, bscc);
					if (verbose) {
						mainLog.print("\nBSCC " + (i + 1) + " reachability probabilities: \n");
						probs.print(mainLog);
					}
					// Multiply by BSCC prob, add to total
					probs.timesConstant(probBSCCs[i]);
					totalProbs.add(probs);
					// Free vector
					probs.clear();
				}
			}

			// print out probabilities
			if (verbose) {
				mainLog.print("\nS operator probabilities: \n");
				totalProbs.print(mainLog);
			}
		} catch (PrismException e) {
			// Tidy up and pass on the exception
			if (b != null)
				JDD.Deref(b);
			for (i = 0; i < numBSCCs; i++) {
				if (bsccs.get(i) != null)
					JDD.Deref(bsccs.get(i));
			}
			if (notInBSCCs != null)
				JDD.Deref(notInBSCCs);
			if (totalProbs != null)
				totalProbs.clear();
			throw e;
		}

		// Tidy up
		if (b != null)
			JDD.Deref(b);
		for (i = 0; i < numBSCCs; i++) {
			if (bsccs.get(i) != null)
				JDD.Deref(bsccs.get(i));
		}
		if (notInBSCCs != null)
			JDD.Deref(notInBSCCs);

		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return totalProbs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			sol = totalProbs.getBDDFromInterval(opInfo.getRelOp(), opInfo.getBound());
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
			// free vector
			totalProbs.clear();
			return new StateValuesMTBDD(sol, model);
		}
	}

	// Contents of a P operator

	protected StateValues checkProbPathFormula(Expression expr, boolean qual) throws PrismException
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
			return checkProbPathFormulaSimple(expr, qual);
		} else {
			return checkProbPathFormulaLTL(expr, qual);
		}
	}

	protected StateValues checkProbPathFormulaSimple(Expression expr, boolean qual) throws PrismException
	{
		boolean negated = false;
		StateValues probs = null;

		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);

		// Negation
		if (expr instanceof ExpressionUnaryOp &&
		    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
			negated = true;
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}

		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(exprTemp);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(exprTemp);
				} else {
					probs = checkProbUntil(exprTemp, qual);
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

	// LTL-like path formula for P operator

	protected StateValues checkProbPathFormulaLTL(Expression expr, boolean qual) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct = null, probs = null;
		Vector<JDDNode> labelDDs = new Vector<JDDNode>();
		DA<BitSet, ? extends AcceptanceOmega> da;
		ProbModel modelProduct;
		ProbModelChecker mcProduct;
		JDDNode startMask;
		JDDVars daDDRowVars, daDDColVars;
		int i;

		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.RABIN,
				AcceptanceType.REACH,
				AcceptanceType.GENERIC
		};
		mcLtl = new LTLModelChecker(prism);
		da = mcLtl.constructDAForLTLFormula(this, model, expr, labelDDs, allowedAcceptance);

		// Build product of Markov chain and automaton
		// (note: might be a CTMC - StochModelChecker extends this class)
		mainLog.println("\nConstructing MC-"+da.getAutomataType()+" product...");
		daDDRowVars = new JDDVars();
		daDDColVars = new JDDVars();
		modelProduct = mcLtl.constructProductMC(da, model, labelDDs, daDDRowVars, daDDColVars);
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
			mainLog.println("\nSkipping BSCC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReachDD) acceptance).getGoalStates();
			JDD.Ref(modelProduct.getReach());
			acc = JDD.And(acc, modelProduct.getReach());
		} else {
			mainLog.println("\nFinding accepting BSCCs...");
			acc = mcLtl.findAcceptingBSCCs(acceptance, modelProduct);
		}
		acceptance.clear();
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = createNewModelChecker(prism, modelProduct, null);
		probsProduct = mcProduct.checkProbUntil(modelProduct.getReach(), acc, qual);

		// Convert probability vector to original model
		// First, filter over DRA start states
		startMask = mcLtl.buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(model.getReach());
		startMask = JDD.And(model.getReach(), startMask);
		probsProduct.filter(startMask);
		// Then sum over DD vars for the DA state (could also have used,
		// e.g. max, since there is just one state for each valuation of daDDRowVars) 
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

	// next

	protected StateValues checkProbNext(ExpressionTemporal expr) throws PrismException
	{
		JDDNode b;
		StateValues probs = null;

		// model check operand first
		b = checkExpressionDD(expr.getOperand2());

		// print out some info about num states
		// mainLog.print("\nb = " + JDD.GetNumMintermsString(b,
		// allDDRowVars.n()) + " states\n");

		// compute probabilities
		probs = computeNextProbs(trans, b);

		// derefs
		JDD.Deref(b);

		return probs;
	}

	// bounded until

	protected StateValues checkProbBoundedUntil(ExpressionTemporal expr) throws PrismException
	{
		JDDNode b1, b2;
		StateValues probs = null;
		Integer lowerBound;
		IntegerBound bounds;
		int i;

		// get and check bounds information
		bounds = IntegerBound.fromExpressionTemporal(expr, constantValues, true);

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
				probs = computeUntilProbs(trans, trans01, b1, b2);
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
				probs = computeBoundedUntilProbs(trans, trans01, b1, b2, windowSize);
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
				probs = computeRestrictedNext(trans, b1, probs);
			}
		}

		// derefs
		JDD.Deref(b1);
		JDD.Deref(b2);

		return probs;
	}

	// until (unbounded)

	// this method is split into two steps so that the LTL model checker can use the second part directly

	protected StateValues checkProbUntil(ExpressionTemporal expr, boolean qual) throws PrismException
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
			probs = checkProbUntil(b1, b2, qual);
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

	// until (unbounded): b1/b2 are bdds for until operands

	protected StateValues checkProbUntil(JDDNode b1, JDDNode b2, boolean qual) throws PrismException
	{
		StateValues probs = null;

		// compute probabilities

		// if requested (i.e. when prob bound is 0 or 1 and precomputation algorithms are enabled),
		// compute probabilities qualitatively
		if (qual) {
			mainLog.print("\nProbability bound in formula is 0/1 so not computing exact probabilities...\n");
			probs = computeUntilProbsQual(trans01, b1, b2);
		}
		// otherwise actually compute probabilities
		else {
			probs = computeUntilProbs(trans, trans01, b1, b2);
		}

		return probs;
	}

	// cumulative reward

	protected StateValues checkRewardCumul(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
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

		// compute rewards

		// a trivial case: "<=0"
		if (time == 0) {
			rewards = new StateValuesMTBDD(JDD.Constant(0), model);
		} else {
			// compute rewards
			try {
				rewards = computeCumulRewards(trans, trans01, stateRewards, transRewards, time);
			} catch (PrismException e) {
				throw e;
			}
		}

		return rewards;
	}

	// cumulative reward

	protected StateValues checkRewardTotal(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		StateValues rewards = computeTotalRewards(trans, trans01, stateRewards, transRewards);
		return rewards;
	}

	// inst reward

	protected StateValues checkRewardInst(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		int time; // time
		StateValues rewards = null;

		// get info from inst reward
		time = expr.getUpperBound().evaluateInt(constantValues);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in instantaneous reward property");
		}

		// compute rewards
		rewards = computeInstRewards(trans, stateRewards, time);

		return rewards;
	}

	/**
	 * Compute rewards for a reachability reward operator.
	 */
	protected StateValues checkRewardPathFormula(Expression expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		if (Expression.isReach(expr)) {
			return checkRewardReach((ExpressionTemporal) expr, stateRewards, transRewards);
		}
		else if (Expression.isCoSafeLTLSyntactic(expr, true)) {
			return checkRewardCoSafeLTL(expr, stateRewards, transRewards);
		}
		throw new PrismException("R operator contains a path formula that is not syntactically co-safe: " + expr);
	}
	
	// reach reward

	protected StateValues checkRewardReach(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		JDDNode b;
		StateValues rewards = null;

		// No time bounds allowed
		if (expr.hasBounds()) {
			throw new PrismNotSupportedException("R operator cannot contain a bounded F operator: " + expr);
		}
		
		// model check operand first
		b = checkExpressionDD(expr.getOperand2());

		// print out some info about num states
		// mainLog.print("\nb = " + JDD.GetNumMintermsString(b,
		// allDDRowVars.n()) + " states\n");

		// compute rewards
		try {
			rewards = computeReachRewards(trans, trans01, stateRewards, transRewards, b);
		} catch (PrismException e) {
			JDD.Deref(b);
			throw e;
		}

		// derefs
		JDD.Deref(b);

		return rewards;
	}

	// co-safe LTL reward

	protected StateValues checkRewardCoSafeLTL(Expression expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues rewardsProduct = null, rewards = null;
		Expression ltl;
		Vector<JDDNode> labelDDs;
		DA<BitSet, ? extends AcceptanceOmega> da;
		ProbModel modelProduct;
		ProbModelChecker mcProduct;
		JDDNode startMask;
		JDDVars daDDRowVars, daDDColVars;
		int i;
		long l;

		if (Expression.containsTemporalTimeBounds(expr)) {
			if (model.getModelType().continuousTime()) {
				throw new PrismException("DA construction for time-bounded operators not supported for " + model.getModelType()+".");
			}

			if (!expr.isSimplePathFormula()) {
				throw new PrismException("Time-bounded operators not supported in LTL: " + expr);
			}
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

		// Convert LTL formula to deterministic automaton (DA)
		mainLog.println("\nBuilding deterministic automaton (for " + ltl + ")...");
		l = System.currentTimeMillis();
		LTL2DA ltl2da = new LTL2DA(prism);
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.RABIN,
				AcceptanceType.REACH
		};
		da = ltl2da.convertLTLFormulaToDA(ltl, constantValues, allowedAcceptance);
		mainLog.println(da.getAutomataType()+" has " + da.size() + " states, " + da.getAcceptance().getSizeStatistics()+".");
		l = System.currentTimeMillis() - l;
		mainLog.println("Time for deterministic automaton translation: " + l / 1000.0 + " seconds.");
		// If required, export DA 
		if (prism.getSettings().getExportPropAut()) {
			mainLog.println("Exporting DA to file \"" + prism.getSettings().getExportPropAutFilename() + "\"...");
			PrintStream out = PrismUtils.newPrintStream(prism.getSettings().getExportPropAutFilename());
			da.print(out, prism.getSettings().getExportPropAutType());
			out.close();
			//da.printDot(new java.io.PrintStream("da.dot"));
		}

		// Build product of Markov chain and automaton
		// (note: might be a CTMC - StochModelChecker extends this class)
		mainLog.println("\nConstructing MC-"+da.getAutomataType()+" product...");
		daDDRowVars = new JDDVars();
		daDDColVars = new JDDVars();
		l = System.currentTimeMillis();
		modelProduct = mcLtl.constructProductMC(da, model, labelDDs, daDDRowVars, daDDColVars);
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

		// Adapt reward info to product model
		JDD.Ref(stateRewards);
		JDD.Ref(modelProduct.getReach());
		JDDNode stateRewardsProduct = JDD.Apply(JDD.TIMES, stateRewards, modelProduct.getReach());
		JDD.Ref(transRewards);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode transRewardsProduct = JDD.Apply(JDD.TIMES, transRewards, modelProduct.getTrans01());
		
		// Find accepting states + compute reachability rewards
		AcceptanceOmegaDD acceptance = da.getAcceptance().toAcceptanceDD(daDDRowVars);
		JDDNode acc = null;
		if (acceptance instanceof AcceptanceReachDD) {
			mainLog.println("\nSkipping BSCC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReachDD) acceptance).getGoalStates();
			JDD.Ref(modelProduct.getReach());
			acc = JDD.And(acc, modelProduct.getReach());
		} else {
			mainLog.println("\nFinding accepting BSCCs...");
			acc = mcLtl.findAcceptingBSCCs(acceptance, modelProduct);
		}
		acceptance.clear();
		mainLog.println("\nComputing reachability rewards...");
		mcProduct = createNewModelChecker(prism, modelProduct, null);
		rewardsProduct = mcProduct.computeReachRewards(modelProduct.getTrans(), modelProduct.getTrans01(), stateRewardsProduct, transRewardsProduct, acc);

		// Convert reward vector to original model
		// First, filter over DRA start states
		startMask = mcLtl.buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(model.getReach());
		startMask = JDD.And(model.getReach(), startMask);
		rewardsProduct.filter(startMask);
		// Then sum over DD vars for the DRA state
		rewards = rewardsProduct.sumOverDDVars(daDDRowVars, model);

		// Deref, clean up
		JDD.Deref(stateRewardsProduct);
		JDD.Deref(transRewardsProduct);
		rewardsProduct.clear();
		modelProduct.clear();
		for (i = 0; i < labelDDs.size(); i++) {
			JDD.Deref(labelDDs.get(i));
		}
		JDD.Deref(acc);
		JDD.Deref(startMask);
		daDDRowVars.derefAll();
		daDDColVars.derefAll();

		return rewards;
	}

	// steady state reward

	protected StateValues checkRewardSS(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		// bscc stuff
		List<JDDNode> vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode newStateRewards, bscc, tmp;
		// other stuff
		StateValues probs = null, rewards = null;
		int i, numBSCCs;
		double d, rewBSCCs[];

		// compute rewards corresponding to each state
		JDD.Ref(trans);
		JDD.Ref(transRewards);
		newStateRewards = JDD.SumAbstract(JDD.Apply(JDD.TIMES, trans, transRewards), allDDColVars);
		JDD.Ref(stateRewards);
		newStateRewards = JDD.Apply(JDD.PLUS, newStateRewards, stateRewards);

		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			SCCComputer sccComputer = prism.getSCCComputer(model);
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			numBSCCs = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector<JDDNode>();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			numBSCCs = 1;
		}

		// compute steady state for each bscc...
		rewBSCCs = new double[numBSCCs];
		for (i = 0; i < numBSCCs; i++) {

			mainLog.println("\nComputing steady state probabilities for BSCC " + (i + 1));

			// get bscc
			bscc = vectBSCCs.get(i);

			// compute steady state probabilities
			try {
				probs = computeSteadyStateProbsForBSCC(trans, bscc);
			} catch (PrismException e) {
				JDD.Deref(newStateRewards);
				for (i = 0; i < numBSCCs; i++) {
					JDD.Deref(vectBSCCs.get(i));
				}
				JDD.Deref(notInBSCCs);
				throw e;
			}

			// print out probabilities
			if (verbose) {
				mainLog.print("\nBSCC " + (i + 1) + " steady-state probabilities: \n");
				probs.print(mainLog);
			}

			// do weighted sum of probabilities and rewards
			JDD.Ref(bscc);
			JDD.Ref(newStateRewards);
			tmp = JDD.Apply(JDD.TIMES, bscc, newStateRewards);
			d = probs.sumOverMTBDD(tmp);
			rewBSCCs[i] = d;
			mainLog.print("\nBSCC " + (i + 1) + " Reward: " + d + "\n");
			JDD.Deref(tmp);

			// free vector
			probs.clear();
		}

		// if every state is in a bscc, it's much easier...
		if (notInBSCCs.equals(JDD.ZERO)) {

			mainLog.println("\nAll states are in BSCCs (so no reachability probabilities computed)");

			// build the reward vector
			tmp = JDD.Constant(0);
			for (i = 0; i < numBSCCs; i++) {
				bscc = vectBSCCs.get(i);
				JDD.Ref(bscc);
				tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(rewBSCCs[i]), bscc));
			}
			rewards = new StateValuesMTBDD(tmp, model);
		}
		// otherwise we have to do more work...
		else {

			// initialise rewards vector
			switch (engine) {
			case Prism.MTBDD:
				rewards = new StateValuesMTBDD(JDD.Constant(0), model);
				break;
			case Prism.SPARSE:
				rewards = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			case Prism.HYBRID:
				rewards = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			}

			// compute probabilities of reaching each bscc...
			for (i = 0; i < numBSCCs; i++) {

				// skip bsccs with zero reward
				if (rewBSCCs[i] == 0.0)
					continue;

				mainLog.println("\nComputing probabilities of reaching BSCC " + (i + 1));

				// get bscc
				bscc = vectBSCCs.get(i);

				// compute probabilities
				probs = computeUntilProbs(trans, trans01, notInBSCCs, bscc);

				// print out probabilities
				if (verbose) {
					mainLog.print("\nBSCC " + (i + 1) + " reachability probabilities: \n");
					probs.print(mainLog);
				}

				// times by bscc reward, add to total
				probs.timesConstant(rewBSCCs[i]);
				rewards.add(probs);

				// free vector
				probs.clear();
			}
		}

		// derefs
		JDD.Deref(newStateRewards);
		for (i = 0; i < numBSCCs; i++) {
			JDD.Deref(vectBSCCs.get(i));
		}
		JDD.Deref(notInBSCCs);

		return rewards;
	}

	// -----------------------------------------------------------------------------------
	// do steady state computation
	// -----------------------------------------------------------------------------------

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doSteadyState() throws PrismException
	{
		return doSteadyState((StateValues) null);
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doSteadyState(File initDistFile) throws PrismException
	{
		StateValues initDist = readDistributionFromFile(initDistFile);
		return doSteadyState(initDist);
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over and
	 * then deleted afterwards, so if you wanted it, take a copy. 
	 */
	public StateValues doSteadyState(StateValues initDist) throws PrismException
	{
		StateValues initDistNew = (initDist == null) ? buildInitialDistribution() : initDist;
		return computeSteadyStateProbs(trans, initDistNew);
	}

	// -----------------------------------------------------------------------------------
	// do transient computation
	// -----------------------------------------------------------------------------------

	/**
	 * Compute transient probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(int time) throws PrismException
	{
		return doTransient(time, (StateValues) null);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(int time, File initDistFile) throws PrismException
	{
		StateValues initDist = readDistributionFromFile(initDistFile);
		return doTransient(time, initDist);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over and
	 * then deleted afterwards, so if you wanted it, take a copy. 
	 */
	public StateValues doTransient(int time, StateValues initDist) throws PrismException
	{
		StateValues initDistNew = (initDist == null) ? buildInitialDistribution() : initDist;
		return computeTransientProbs(trans, initDistNew, time);
	}

	// -----------------------------------------------------------------------------------
	// Utility methods for probability distributions
	// -----------------------------------------------------------------------------------

	/**
	 * Generate a probability distribution, stored as a StateValues object, from a file.
	 * The type of storage (MTBDD or double vector) matches the current engine.
	 * If {@code distFile} is null, so is the return value.
	 */
	public StateValues readDistributionFromFile(File distFile) throws PrismException
	{
		StateValues dist = null;

		if (distFile != null) {
			mainLog.println("\nImporting probability distribution from file \"" + distFile + "\"...");
			// Build an empty vector of the appropriate type 
			if (engine == Prism.MTBDD) {
				dist = new StateValuesMTBDD(JDD.Constant(0), model);
			} else {
				dist = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
			}
			// Populate vector from file
			dist.readFromFile(distFile);
		}

		return dist;
	}

	/**
	 * Build a probability distribution, stored as a StateValues object,
	 * from the initial states info of the current model: either probability 1 for
	 * the (single) initial state or equiprobable over multiple initial states.
	 * The type of storage (MTBDD or double vector) matches the current engine.
	 */
	private StateValues buildInitialDistribution()
	{
		StateValues dist = null;
		JDDNode init;

		// first construct as MTBDD
		// get initial states of model
		start = model.getStart();
		// compute initial probability distribution (equiprobable over all start states)
		JDD.Ref(start);
		init = JDD.Apply(JDD.DIVIDE, start, JDD.Constant(JDD.GetNumMinterms(start, allDDRowVars.n())));
		// if using MTBDD engine, distribution needs to be an MTBDD
		if (engine == Prism.MTBDD) {
			dist = new StateValuesMTBDD(init, model);
		}
		// for sparse/hybrid engines, distribution needs to be a double vector
		else {
			dist = new StateValuesDV(init, model);
			JDD.Deref(init);
		}

		return dist;
	}

	// -----------------------------------------------------------------------------------
	// probability computation methods
	// -----------------------------------------------------------------------------------

	// compute probabilities for next

	protected StateValues computeNextProbs(JDDNode tr, JDDNode b)
	{
		JDDNode tmp;
		StateValues probs = null;

		// matrix multiply: trans * b
		JDD.Ref(b);
		tmp = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		JDD.Ref(tr);
		tmp = JDD.MatrixMultiply(tr, tmp, allDDColVars, JDD.BOULDER);
		probs = new StateValuesMTBDD(tmp, model);

		return probs;
	}

	/**
	 * Given a value vector x, compute the probability:
	 *   v(s) = Sum_s' P(s,s')*x(s')   for s labeled with a,
	 *   v(s) = 0                      for s not labeled with a.
	 *
	 * Clears the StateValues object x.
	 *
	 * @param tr the transition matrix
	 * @param a the set of states labeled with a
	 * @param x the value vector
	 */
	protected StateValues computeRestrictedNext(JDDNode tr, JDDNode a, StateValues x)
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

		// label is 0/1 BDD, MIN sets all values to 0 for states not in a
		JDD.Ref(a);
		tmp = JDD.Apply(JDD.MIN, tmp, a);

		ddX.clear();
		probs = new StateValuesMTBDD(tmp, model);
		return probs;
	}

	// compute probabilities for bounded until

	protected StateValues computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2, int time) throws PrismException
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
				no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, b1, yes);
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
					probsMTBDD = PrismMTBDD.ProbBoundedUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe, time);
					probs = new StateValuesMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.ProbBoundedUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe, time);
					probs = new StateValuesDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.ProbBoundedUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe, time);
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

	protected StateValues computeUntilProbsQual(JDDNode tr01, JDDNode b1, JDDNode b2)
	{
		JDDNode yes, no, maybe;
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
			// no/yes
			no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, b1, b2);
			yes = PrismMTBDD.Prob1(tr01, reach, allDDRowVars, allDDColVars, b1, b2, no);
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
		// otherwise we set the probabilities for maybe states to be 0.5
		// (actual probabilities for these states are unknown but definitely >0
		// and <1)
		// (this is safe because the results of this function will only be used
		// to compare against 0/1 bounds)
		// (this is not entirely elegant but is simpler and less error prone
		// than
		// trying to work out whether to use 0/1 for all case of future/global, etc.)
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

	protected StateValues computeUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2) throws PrismException
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
			// no/yes
			if (precomp && (prob0 || prob1)) {
				no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, b1, b2);
			} else {
				JDD.Ref(reach);
				JDD.Ref(b1);
				JDD.Ref(b2);
				no = JDD.And(reach, JDD.Not(JDD.Or(b1, b2)));
			}
			if (precomp && prob1) {
				yes = PrismMTBDD.Prob1(tr01, reach, allDDRowVars, allDDColVars, b1, b2, no);
			} else {
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

		// if maybe is empty, we have the probabilities already
		if (maybe.equals(JDD.ZERO)) {
			// we make sure to return a vector of the appropriate type
			// (doublevector for hybrid/sparse, mtbdd for mtbdd)
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(yes);
				probs = new StateValuesMTBDD(yes, model);
				break;
			case Prism.SPARSE:
			case Prism.HYBRID:
				probs = new StateValuesDV(yes, model);
				break;
			}
		}
		// otherwise we compute the actual probabilities
		else {
			// compute probabilities
			mainLog.println("\nComputing remaining probabilities...");
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateValuesMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateValuesDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
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

	// compute cumulative rewards

	protected StateValues computeCumulRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, int time) throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;

		// compute rewards
		mainLog.println("\nComputing rewards...");
		mainLog.println("Engine: " + Prism.getEngineString(engine));
		try {
			switch (engine) {
			case Prism.MTBDD:
				rewardsMTBDD = PrismMTBDD.ProbCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateValuesMTBDD(rewardsMTBDD, model);
				break;
			case Prism.SPARSE:
				rewardsDV = PrismSparse.ProbCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateValuesDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				rewardsDV = PrismHybrid.ProbCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateValuesDV(rewardsDV, model);
				break;
			default:
				throw new PrismException("Unknown engine");
			}
		} catch (PrismException e) {
			throw e;
		}

		return rewards;
	}

	// compute total rewards

	protected StateValues computeTotalRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr) throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;
		// BSCC stuff
		List<JDDNode> bsccs = null;
		JDDNode notInBSCCs = null;
		int numBSCCs = 0;

		// Compute bottom strongly connected components (BSCCs)
		SCCComputer sccComputer = prism.getSCCComputer(model);
		sccComputer.computeBSCCs();
		bsccs = sccComputer.getBSCCs();
		notInBSCCs = sccComputer.getNotInBSCCs();
		numBSCCs = bsccs.size();

		// Find BSCCs with non-zero reward
		JDD.Ref(sr);
		JDDNode srNonZero = JDD.GreaterThan(sr, 0);
		JDD.Ref(trr);
		JDDNode trrNonZero = JDD.GreaterThan(trr, 0);
		JDDNode bsccsNonZero = JDD.Constant(0);
		for (int b = 0; b < numBSCCs; b++) {
			if (JDD.AreIntersecting(bsccs.get(b), srNonZero) || JDD.AreIntersecting(bsccs.get(b), trrNonZero)) {
				JDD.Ref(bsccs.get(b));
				bsccsNonZero = JDD.Or(bsccsNonZero, bsccs.get(b));
			}
		}
		JDD.Deref(srNonZero);
		JDD.Deref(trrNonZero);
		mainLog.print("States in non-zero reward BSCCs: " + JDD.GetNumMintermsString(bsccsNonZero, allDDRowVars.n()));

		// Find states with infinite reward (those reach a non-zero reward BSCC with prob > 0)  
		JDDNode inf = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, reach, bsccsNonZero);
		inf = JDD.And(reach.copy(), JDD.Not(inf));
		JDDNode maybe = JDD.And(reach.copy(), JDD.Not(inf.copy()));
		JDD.Deref(bsccsNonZero);

		// Print out inf/maybe
		mainLog.print("\ninf = " + JDD.GetNumMintermsString(inf, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");

		// If maybe is empty, we have the rewards already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(inf);
			rewards = new StateValuesMTBDD(JDD.ITE(inf, JDD.PlusInfinity(), JDD.Constant(0)), model);
		}
		// Otherwise we compute the actual rewards
		else {
			// Compute the rewards
			// (do this using the functions for "reward reachability" properties but with no targets)
			mainLog.println("\nComputing remaining rewards...");
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, JDD.ZERO, inf, maybe);
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, JDD.ZERO, inf, maybe);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					rewardsDV = PrismHybrid.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, JDD.ZERO, inf, maybe);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				default:
					throw new PrismException("Unknown engine");
				}
			} catch (PrismException e) {
				JDD.Deref(inf);
				JDD.Deref(maybe);
				throw e;
			}
		}

		// Tidy up
		for (int b = 0; b < numBSCCs; b++) {
			if (bsccs.get(b) != null)
				JDD.Deref(bsccs.get(b));
		}
		if (start != notInBSCCs)
			JDD.Deref(notInBSCCs);
		JDD.Deref(inf);
		JDD.Deref(maybe);

		return rewards;
	}

	// compute rewards for inst reward

	protected StateValues computeInstRewards(JDDNode tr, JDDNode sr, int time) throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;

		// a trivial case: "=0"
		if (time == 0) {
			JDD.Ref(sr);
			rewards = new StateValuesMTBDD(sr, model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			mainLog.println("\nComputing rewards...");
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.ProbInstReward(tr, sr, odd, allDDRowVars, allDDColVars, time);
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.ProbInstReward(tr, sr, odd, allDDRowVars, allDDColVars, time);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					rewardsDV = PrismHybrid.ProbInstReward(tr, sr, odd, allDDRowVars, allDDColVars, time);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
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

	protected StateValues computeReachRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b) throws PrismException
	{
		JDDNode inf, maybe;
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateValues rewards = null;

		// compute states which can't reach goal with probability 1
		if (b.equals(JDD.ZERO)) {
			JDD.Ref(reach);
			inf = reach;
			maybe = JDD.Constant(0);
		} else if (b.equals(reach)) {
			inf = JDD.Constant(0);
			maybe = JDD.Constant(0);
		} else {
			JDDNode no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, reach, b);
			JDDNode prob1 = PrismMTBDD.Prob1(tr01, reach, allDDRowVars, allDDColVars, reach, b, no);
			JDD.Deref(no);
			JDD.Ref(reach);
			inf = JDD.And(reach, JDD.Not(prob1));
			JDD.Ref(reach);
			JDD.Ref(inf);
			JDD.Ref(b);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(inf, b)));
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
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateValuesMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					rewardsDV = PrismHybrid.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateValuesDV(rewardsDV, model);
					break;
				default:
					throw new PrismException("Unknown engine");
				}
			} catch (PrismException e) {
				JDD.Deref(inf);
				JDD.Deref(maybe);
				throw e;
			}
		}

		// derefs
		JDD.Deref(inf);
		JDD.Deref(maybe);

		return rewards;
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Use the passed in vector initDist as the initial probability distribution (time 0).
	 * The type of this should match the current engine
	 * (i.e. StateValuesMTBDD for MTBDD, StateValuesDV for sparse/hybrid). 
	 * For reasons of efficiency, this vector will be trampled over and
	 * then deleted afterwards, so if you wanted it, take a copy. 
	 * @param tr The transition probability matrix for the DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues computeSteadyStateProbs(JDDNode tr, StateValues initDist) throws PrismException
	{
		// BSCC stuff
		List<JDDNode> bsccs = null;
		JDDNode notInBSCCs = null;
		// MTBDD stuff
		JDDNode start = null, bscc;
		// Other stuff
		StateValues probs = null, solnProbs = null;
		double probBSCCs[];
		int numBSCCs = 0, allInOneBSCC = -1;

		try {

			// Compute bottom strongly connected components (BSCCs)
			if (bsccComp) {
				SCCComputer sccComputer = prism.getSCCComputer(model);
				sccComputer.computeBSCCs();
				bsccs = sccComputer.getBSCCs();
				notInBSCCs = sccComputer.getNotInBSCCs();
				numBSCCs = bsccs.size();
			}
			// Unless we've been told to skip it
			else {
				mainLog.println("\nSkipping BSCC computation...");
				bsccs = new Vector<JDDNode>();
				JDD.Ref(reach);
				bsccs.add(reach);
				notInBSCCs = JDD.Constant(0);
				numBSCCs = 1;
			}

			// See which states in the initial distribution have non-zero prob
			start = initDist.getBDDFromInterval(">", 0);
			// Determine whether initial states are all in a single BSCC 
			allInOneBSCC = -1;
			for (int b = 0; b < numBSCCs; b++) {
				if (JDD.IsContainedIn(start, bsccs.get(b))) {
					allInOneBSCC = b;
					break;
				}
			}

			// If all initial states are in a single BSCC, it's easy...
			// Just compute steady-state probabilities for the BSCC
			if (allInOneBSCC != -1) {
				mainLog.println("\nInitial states all in one BSCC (so no reachability probabilities computed)");
				bscc = bsccs.get(allInOneBSCC);
				solnProbs = computeSteadyStateProbsForBSCC(trans, bscc);
			}

			// Otherwise, have to consider all the BSCCs
			else {

				// Initialise total probabilities vector
				switch (engine) {
				case Prism.MTBDD:
					solnProbs = new StateValuesMTBDD(JDD.Constant(0), model);
					break;
				case Prism.SPARSE:
					solnProbs = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
					break;
				case Prism.HYBRID:
					solnProbs = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
					break;
				}

				// Compute probability of reaching each BSCC from initial distribution 
				probBSCCs = new double[numBSCCs];
				for (int b = 0; b < numBSCCs; b++) {
					mainLog.println("\nComputing probability of reaching BSCC " + (b + 1));
					bscc = bsccs.get(b);
					// Compute probabilities
					probs = computeUntilProbs(trans, trans01, notInBSCCs, bscc);
					// Compute probability of reaching BSCC, which is dot product of
					// vectors for initial distribution and probabilities of reaching it
					probBSCCs[b] = probs.dotProduct(initDist);
					mainLog.print("\nProbability of reaching BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
					// Free vector
					probs.clear();
				}

				// Compute steady-state probs for each BSCC
				for (int b = 0; b < numBSCCs; b++) {
					mainLog.println("\nComputing steady-state probabilities for BSCC " + (b + 1));
					bscc = bsccs.get(b);
					// Compute steady-state probabilities for the BSCC
					probs = computeSteadyStateProbsForBSCC(trans, bscc);
					// Print out probabilities
					if (verbose) {
						mainLog.print("\nBSCC " + (b + 1) + " Steady-State Probabilities: \n");
						probs.print(mainLog);
					}
					// Multiply by BSCC reach prob, add to total
					probs.timesConstant(probBSCCs[b]);
					solnProbs.add(probs);
					// Free vector
					probs.clear();
				}
			}
		} catch (PrismException e) {
			// Tidy up and pass on the exception
			if (start != null)
				JDD.Deref(start);
			for (int b = 0; b < numBSCCs; b++) {
				if (bsccs.get(b) != null)
					JDD.Deref(bsccs.get(b));
			}
			if (start != notInBSCCs)
				JDD.Deref(notInBSCCs);
			if (solnProbs != null)
				solnProbs.clear();
			if (initDist != null)
				initDist.clear();
			throw e;
		}

		// Tidy up
		if (start != null)
			JDD.Deref(start);
		for (int b = 0; b < numBSCCs; b++) {
			if (bsccs.get(b) != null)
				JDD.Deref(bsccs.get(b));
		}
		if (start != notInBSCCs)
			JDD.Deref(notInBSCCs);
		if (initDist != null)
			initDist.clear();

		return solnProbs;
	}

	/**
	 * Compute steady-state probabilities for a BSCC
	 * i.e. compute the long-run probability of being in each state of the BSCC.
	 * No initial distribution is specified since it does not affect the result.
	 * The result will be stored in a full vector whose size equals the number of states in the DTMC.
	 * @param tr The transition probability matrix for the whole DTMC
	 * @param bscc The BSCC to be analysed
	 */
	protected StateValues computeSteadyStateProbsForBSCC(JDDNode tr, JDDNode bscc) throws PrismException
	{
		JDDNode trf, init;
		long n;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateValues probs = null;

		// work out number of states in the BSCC
		if (bscc.equals(reach)) {
			// avoid a call to GetNumMinterms in this simple (and common) case
			n = model.getNumStates();
		} else {
			n = Math.round(JDD.GetNumMinterms(bscc, allDDRowVars.n()));
		}

		// special case: 1 state BSCC (in fact, we *need* to check for this
		// special case because the general  solution work breaks)
		if (n == 1) {
			// answer is trivially one in the single state
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(bscc);
				return new StateValuesMTBDD(bscc, model);
			case Prism.SPARSE:
				return new StateValuesDV(bscc, model);
			case Prism.HYBRID:
				return new StateValuesDV(bscc, model);
			}
		}

		// filter out unwanted states from transition matrix
		JDD.Ref(tr);
		JDD.Ref(bscc);
		trf = JDD.Apply(JDD.TIMES, tr, bscc);
		JDD.Ref(bscc);
		trf = JDD.Apply(JDD.TIMES, trf, JDD.PermuteVariables(bscc, allDDRowVars, allDDColVars));

		// compute initial solution (equiprobable)
		JDD.Ref(bscc);
		init = JDD.Apply(JDD.DIVIDE, bscc, JDD.Constant(n));

		// compute remaining probabilities
		mainLog.println("\nComputing probabilities...");
		mainLog.println("Engine: " + Prism.getEngineString(engine));
		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateValuesMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateValuesDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateValuesDV(probsDV, model);
				break;
			default:
				throw new PrismException("Unknown engine");
			}
		} catch (PrismException e) {
			JDD.Deref(trf);
			JDD.Deref(init);
			throw e;
		}

		// derefs
		JDD.Deref(trf);
		JDD.Deref(init);

		return probs;
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Use the passed in vector initDist as the initial probability distribution (time 0).
	 * The type of this should match the current engine
	 * (i.e. StateValuesMTBDD for MTBDD, StateValuesDV for sparse/hybrid). 
	 * For reasons of efficiency, this vector will be trampled over and
	 * then deleted afterwards, so if you wanted it, take a copy. 
	 * @param tr The transition probability matrix for the DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 * @param time Time step
	 */
	protected StateValues computeTransientProbs(JDDNode tr, StateValues initDist, int time) throws PrismException
	{
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateValues probs = null;

		// special case: time = 0
		if (time == 0) {
			// we are allowed to keep the init vector, so no need to clone
			return initDist;
		}

		// general case
		mainLog.println("\nComputing probabilities...");
		mainLog.println("Engine: " + Prism.getEngineString(engine));
		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.ProbTransient(tr, odd, ((StateValuesMTBDD) initDist).getJDDNode(), allDDRowVars, allDDColVars, time);
				probs = new StateValuesMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.ProbTransient(tr, odd, ((StateValuesDV) initDist).getDoubleVector(), allDDRowVars, allDDColVars, time);
				probs = new StateValuesDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.ProbTransient(tr, odd, ((StateValuesDV) initDist).getDoubleVector(), allDDRowVars, allDDColVars, time);
				probs = new StateValuesDV(probsDV, model);
				break;
			default:
				throw new PrismException("Unknown engine");
			}
		} catch (PrismException e) {
			throw e;
		}

		return probs;
	}
}

// ------------------------------------------------------------------------------
