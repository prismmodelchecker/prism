//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import java.util.*;

import jdd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.ast.*;
import parser.visitor.ASTTraverse;
import jltl2dstar.*;

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
	protected boolean precomp;
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

		// Inherit some options from parent Prism object.
		// Store locally and/or pass onto engines.
		precomp = prism.getPrecomp();
		fairness = prism.getFairness();
		switch (engine) {
		case Prism.MTBDD:
			PrismMTBDD.setTermCrit(prism.getTermCrit());
			PrismMTBDD.setTermCritParam(prism.getTermCritParam());
			PrismMTBDD.setMaxIters(prism.getMaxIters());
			break;
		case Prism.SPARSE:
			PrismSparse.setTermCrit(prism.getTermCrit());
			PrismSparse.setTermCritParam(prism.getTermCritParam());
			PrismSparse.setMaxIters(prism.getMaxIters());
			PrismSparse.setCompact(prism.getCompact());
		case Prism.HYBRID:
			PrismHybrid.setTermCrit(prism.getTermCrit());
			PrismHybrid.setTermCritParam(prism.getTermCritParam());
			PrismHybrid.setMaxIters(prism.getMaxIters());
			PrismHybrid.setCompact(prism.getCompact());
			PrismHybrid.setSBMaxMem(prism.getSBMaxMem());
			PrismHybrid.setNumSBLevels(prism.getNumSBLevels());
		}
	}

	// -----------------------------------------------------------------------------------
	// Check a property, i.e. an expression
	// -----------------------------------------------------------------------------------

	// Check expression (recursive)

	public StateProbs checkExpression(Expression expr) throws PrismException
	{
		StateProbs res;

		// P operator
		if (expr instanceof ExpressionProb) {
			res = checkExpressionProb((ExpressionProb) expr);
		}
		// R operator
		else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward((ExpressionReward) expr);
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(expr);
		}

		// Filter out non-reachable states from solution
		// (only necessary for symbolically stored vectors)
		if (res instanceof StateProbsMTBDD)
			res.filter(reach);

		return res;
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	// P operator

	protected StateProbs checkExpressionProb(ExpressionProb expr) throws PrismException
	{
		Expression pb; // probability bound (expression)
		double p = 0; // probability bound (actual value)
		String relOp; // relational operator
		boolean min; // are we finding min (true) or max (false) probs

		JDDNode sol;
		StateProbs probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateDouble(constantValues, null);
			if (p < 0 || p > 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}

		// Check for trivial (i.e. stupid) cases
		if (pb != null) {
			if ((p == 0 && relOp.equals(">=")) || (p == 1 && relOp.equals("<="))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p + " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return new StateProbsMTBDD(reach, model);
			} else if ((p == 0 && relOp.equals("<")) || (p == 1 && relOp.equals(">"))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p + " - formula trivially satisfies no states\n");
				return new StateProbsMTBDD(JDD.Constant(0), model);
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
		boolean qual = pb != null && ((p == 0) || (p == 1)) && precomp;
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
			return new StateProbsMTBDD(sol, model);
		}
	}

	// R operator

	protected StateProbs checkExpressionReward(ExpressionReward expr) throws PrismException
	{
		Object rs; // reward struct index
		Expression rb; // reward bound (expression)
		double r = 0; // reward bound (actual value)
		String relOp; // relational operator
		boolean min; // are we finding min (true) or max (false) rewards
		Expression expr2; // expression

		JDDNode stateRewards = null, transRewards = null, sol;
		StateProbs rewards = null;
		int i;

		// get info from reward operator
		rs = expr.getRewardStructIndex();
		relOp = expr.getRelOp();
		rb = expr.getReward();
		if (rb != null) {
			r = rb.evaluateDouble(constantValues, null);
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
			i = ((Expression) rs).evaluateInt(constantValues, null);
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
				mainLog.print("\nWarning: checking for reward " + relOp + " " + r + " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return new StateProbsMTBDD(reach, model);
			} else if (r == 0 && relOp.equals("<")) {
				mainLog.print("\nWarning: checking for reward " + relOp + " " + r + " - formula trivially satisfies no states\n");
				return new StateProbsMTBDD(JDD.Constant(0), model);
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
			return new StateProbsMTBDD(sol, model);
		}
	}

	// Contents of a P operator, i.e. a path formula

	protected StateProbs checkProbPathFormula(Expression expr, boolean qual, boolean min) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and then pass control to appropriate method.
		if (expr.isSimplePathFormula()) {
			return checkProbPathFormulaSimple(expr, qual, min);
		} else {
			return checkProbPathFormulaLTL(expr, qual, min);
			//throw new PrismException("LTL-style path formulas are not yet supported");
		}
	}

	// Simple path formula for P operator (one temporal op, possibly negated)

	protected StateProbs checkProbPathFormulaSimple(Expression expr, boolean qual, boolean min) throws PrismException
	{
		StateProbs probs = null;

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

	// LTL-like path formula for P operator

	protected StateProbs checkProbPathFormulaLTL(Expression expr, boolean qual, boolean min) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateProbs probsProduct = null, probs = null;
		Expression ltl;
		Vector<JDDNode> labelDDs;
		DRA dra;
		NondetModel modelProduct;
		NondetModelChecker mcProduct;
		JDDNode startMask;
		JDDVars draDDRowVars;
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

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(prism);

		// Model check maximal state formulas
		labelDDs = new Vector<JDDNode>();
		ltl = mcLtl.checkMaximalStateFormulas(this, model, expr.deepCopy(), labelDDs);

		// Convert LTL formula to deterministic Rabin automaton (DRA)
		// For min probabilities, need to negate the formula
		// (But check fairness setting since this may affect min/max)
		mainLog.println("\nBuilding deterministic Rabin automaton (for " + (min && !fairness ? "!" : "") + ltl + ")...");
		l = System.currentTimeMillis();
		if (min && !fairness) {
			dra = LTL2Rabin.ltl2rabin(ltl.convertForJltl2ba().negate());
		} else {
			dra = LTL2Rabin.ltl2rabin(ltl.convertForJltl2ba());
		}
		mainLog.println("\nDRA has " + dra.size() + " states, " + dra.acceptance().size() + " pairs.");
		// dra.print(System.out);
		l = System.currentTimeMillis() - l;
		mainLog.println("\nTime for Rabin translation: " + l / 1000.0 + " seconds.");

		// Build product of MDP and automaton
		mainLog.println("\nConstructing MDP-DRA product...");
		modelProduct = mcLtl.constructProductMDP(dra, model, labelDDs);
		mainLog.println();
		modelProduct.printTransInfo(mainLog, prism.getExtraDDInfo());
		// prism.exportStatesToFile(modelProduct, Prism.EXPORT_PLAIN, null);
		// prism.exportTransToFile(modelProduct, true, Prism.EXPORT_PLAIN, null);

		// Find accepting maximum end components
		mainLog.println("\nFinding accepting end components...");
		JDDNode acc = mcLtl.findAcceptingStates(dra, modelProduct, fairness);

		// Compute reachability probabilities
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new NondetModelChecker(prism, modelProduct, null);
		probsProduct = mcProduct.checkProbUntil(modelProduct.getReach(), acc, qual, min && fairness);

		// subtract from 1 if we're model checking a negated formula for regular Pmin
		if (min && !fairness) {
			probsProduct.subtractFromOne();
		}

		// Convert probability vector to original model
		// First, filter over DRA start states
		// (which we can get from initial states of product model,
		// because of the way it is constructed)
		startMask = modelProduct.getStart();
		probsProduct.filter(startMask);
		// Then sum over DD vars for the DRA state
		draDDRowVars = new JDDVars();
		draDDRowVars.addVars(modelProduct.getAllDDRowVars());
		draDDRowVars.removeVars(allDDRowVars);
		probs = probsProduct.sumOverDDVars(draDDRowVars, model);

		// Deref, clean up
		probsProduct.clear();
		modelProduct.clear();
		for (i = 0; i < labelDDs.size(); i++) {
			JDD.Deref(labelDDs.get(i));
		}
		JDD.Deref(acc);

		return probs;
	}

	// next

	protected StateProbs checkProbNext(ExpressionTemporal expr, boolean min) throws PrismException
	{
		JDDNode b;
		StateProbs probs = null;

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

	// bounded until

	protected StateProbs checkProbBoundedUntil(ExpressionTemporal expr, boolean min) throws PrismException
	{
		int time;
		JDDNode b1, b2;
		StateProbs probs = null;

		// get info from bounded until
		time = expr.getUpperBound().evaluateInt(constantValues, null);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in bounded until formula");
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
			probs = new StateProbsMTBDD(b2, model);
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

	// until (unbounded)

	// this method is split into two steps so that the LTL model checker can use the second part directly

	protected StateProbs checkProbUntil(ExpressionTemporal expr, boolean qual, boolean min) throws PrismException
	{
		JDDNode b1, b2;
		StateProbs probs = null;

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

	// until (unbounded): b1/b2 are bdds for until operands

	protected StateProbs checkProbUntil(JDDNode b1, JDDNode b2, boolean qual, boolean min) throws PrismException
	{
		JDDNode splus, newb1, newb2;
		StateProbs probs = null;
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
			mainLog.print("\nWarning: probability bound in formula is" + " 0/1 so exact probabilities may not be computed\n");
			// for fairness, we compute max here
			probs = computeUntilProbsQual(trans01, newb1, newb2, min && !fairness);
		}
		// otherwise actually compute probabilities
		else {
			// for fairness, we compute max here
			try {
				probs = computeUntilProbs(trans, trans01, newb1, newb2, min && !fairness);
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

	// inst reward

	protected StateProbs checkRewardInst(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min)
			throws PrismException
	{
		int time; // time bound
		StateProbs rewards = null;

		// get info from bounded until
		time = expr.getUpperBound().evaluateInt(constantValues, null);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in instantaneous reward property");
		}

		// compute rewards
		rewards = computeInstRewards(trans, stateRewards, time, min);

		return rewards;
	}

	// reach reward

	protected StateProbs checkRewardReach(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, boolean min)
			throws PrismException
	{
		JDDNode b;
		StateProbs rewards = null;

		// check operand OK (should have detected on type check)
		if (expr.getOperand2().getType() != Expression.BOOLEAN)
			throw new PrismException("Invalid path formula");

		// model check operand first
		b = checkExpressionDD(expr.getOperand2());

		// print out some info about num states
		// mainLog.print("\nb = " + JDD.GetNumMintermsString(b,
		// allDDRowVars.n()) + " states\n");

		// compute rewards
		try {
			rewards = computeReachRewards(trans, trans01, stateRewards, transRewards, b, min);
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

	protected StateProbs computeNextProbs(JDDNode tr, JDDNode b, boolean min)
	{
		JDDNode tmp;
		StateProbs probs = null;

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
		probs = new StateProbsMTBDD(tmp, model);

		return probs;
	}

	// compute probabilities for bounded until

	protected StateProbs computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2, int time, boolean min)
			throws PrismException
	{
		JDDNode yes, no, maybe;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;

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
			} else if (precomp) {
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
			probs = new StateProbsMTBDD(yes, model);
		}
		// otherwise explicitly compute the remaining probabilities
		else {
			// compute probabilities
			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.NondetBoundedUntil(tr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, yes,
							maybe, time, min);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.NondetBoundedUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, time, min);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.NondetBoundedUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, time, min);
					probs = new StateProbsDV(probsDV, model);
					break;
				default:
					throw new PrismException("Engine does not support this numerical method");
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

	protected StateProbs computeUntilProbsQual(JDDNode tr01, JDDNode b1, JDDNode b2, boolean min)
	{
		JDDNode yes = null, no = null, maybe;
		StateProbs probs = null;

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
			probs = new StateProbsMTBDD(yes, model);
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
			probs = new StateProbsMTBDD(JDD.Apply(JDD.PLUS, yes, JDD.Apply(JDD.TIMES, maybe, JDD.Constant(0.5))), model);
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

	protected StateProbs computeUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2, boolean min) throws PrismException
	{
		JDDNode yes, no, maybe;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;

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
			// if precomputation enabled
			if (precomp) {
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
			}
			// if precomputation not enabled
			else {
				// yes
				JDD.Ref(b2);
				yes = b2;
				// no
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

		// if maybe is empty, we have the answer already...
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(yes);
			probs = new StateProbsMTBDD(yes, model);
		}
		// otherwise we compute the actual probabilities
		else {
			// compute probabilities
			mainLog.println("\nComputing remaining probabilities...");

			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.NondetUntil(tr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.NondetUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.NondetUntil(tr, odd, allDDRowVars, allDDColVars, allDDNondetVars, yes, maybe, min);
					probs = new StateProbsDV(probsDV, model);
					break;
				default:
					throw new PrismException("Engine does not support this numerical method");
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

	// compute rewards for inst reward

	protected StateProbs computeInstRewards(JDDNode tr, JDDNode sr, int time, boolean min) throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;

		// a trivial case: "=0"
		if (time == 0) {
			JDD.Ref(sr);
			rewards = new StateProbsMTBDD(sr, model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.NondetInstReward(tr, sr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, time,
							min, start);
					rewards = new StateProbsMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.NondetInstReward(tr, sr, odd, allDDRowVars, allDDColVars, allDDNondetVars, time, min, start);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				default:
					throw new PrismException("Engine does not support this numerical method");
				}
			} catch (PrismException e) {
				throw e;
			}
		}

		return rewards;
	}

	// compute rewards for reach reward

	protected StateProbs computeReachRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b, boolean min)
			throws PrismException
	{
		JDDNode inf, maybe, prob1, no;
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;

		Vector<JDDNode> zeroCostEndComponents = null;
		
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
				
				if (prism.getCheckZeroLoops())
				{
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
						
					ECComputer ecComp = new ECComputerDefault(prism, zeroReach, zeroTrans, zeroTrans01, model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars());
					ecComp.computeECs();
					
					zeroCostEndComponents =  ecComp.getVectECs();
									
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

		if (prism.getCheckZeroLoops())
		{
			// need to deal with zero loops yet
			if (min && zeroCostEndComponents != null && zeroCostEndComponents.size() > 0) {
				mainLog.println("\nWarning: PRISM detected your model contains " +  zeroCostEndComponents.size() + " zero-reward " + ((zeroCostEndComponents.size() == 1) ? "loop." : "loops."));
				mainLog.println("Your minimum rewards may be too low...");
			}
		}
		else if (min) {
			mainLog.println("\nWarning: PRISM hasn't checked for zero-reward loops.");
			mainLog.println("Your minimum rewards may be too low...");
		}

		// print out yes/no/maybe
		mainLog.print("\ngoal = " + JDD.GetNumMintermsString(b, allDDRowVars.n()));
		mainLog.print(", inf = " + JDD.GetNumMintermsString(inf, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");

		// if maybe is empty, we have the rewards already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(inf);
			rewards = new StateProbsMTBDD(JDD.ITE(inf, JDD.PlusInfinity(), JDD.Constant(0)), model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.NondetReachReward(tr, sr, trr, odd, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars,
							b, inf, maybe, min);
					rewards = new StateProbsMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.NondetReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, allDDNondetVars, b, inf, maybe,
							min);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					throw new PrismException("This functionality is not yet supported for this engine");
					// rewardsDV = PrismHybrid.NondetReachReward(tr, sr, trr,
					// odd, allDDRowVars, allDDColVars, allDDNondetVars, b, inf,
					// maybe, min);
					// rewards = new StateProbsDV(rewardsDV, model);
					// break;
				default:
					throw new PrismException("Engine does not support this numerical method");
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
}

// ------------------------------------------------------------------------------
