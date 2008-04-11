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

import java.util.Vector;

import jdd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.ast.*;

/*
 * Model checker for DTMCs.
 */
public class ProbModelChecker extends StateModelChecker
{
	// SCC computer
	protected SCCComputer sccComputer;

	// Options (in addition to those inherited from StateModelChecker):

	// Use 0,1 precomputation algorithms?
	protected boolean precomp;
	// Do BSCC computation?
	protected boolean bsccComp;

	// Constructor

	public ProbModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);

		// Create SCCComputer object
		sccComputer = new SCCComputer(mainLog, techLog, model);

		// Inherit some options from parent Prism object.
		// Store locally and/or pass onto engines.
		precomp = prism.getPrecomp();
		bsccComp = prism.getBSCCComp();
		switch (engine) {
		case Prism.MTBDD:
			PrismMTBDD.setLinEqMethod(prism.getLinEqMethod());
			PrismMTBDD.setLinEqMethodParam(prism.getLinEqMethodParam());
			PrismMTBDD.setTermCrit(prism.getTermCrit());
			PrismMTBDD.setTermCritParam(prism.getTermCritParam());
			PrismMTBDD.setMaxIters(prism.getMaxIters());
			PrismMTBDD.setDoSSDetect(prism.getDoSSDetect());
			break;
		case Prism.SPARSE:
			PrismSparse.setLinEqMethod(prism.getLinEqMethod());
			PrismSparse.setLinEqMethodParam(prism.getLinEqMethodParam());
			PrismSparse.setTermCrit(prism.getTermCrit());
			PrismSparse.setTermCritParam(prism.getTermCritParam());
			PrismSparse.setMaxIters(prism.getMaxIters());
			PrismSparse.setCompact(prism.getCompact());
			PrismSparse.setDoSSDetect(prism.getDoSSDetect());
		case Prism.HYBRID:
			PrismHybrid.setLinEqMethod(prism.getLinEqMethod());
			PrismHybrid.setLinEqMethodParam(prism.getLinEqMethodParam());
			PrismHybrid.setTermCrit(prism.getTermCrit());
			PrismHybrid.setTermCritParam(prism.getTermCritParam());
			PrismHybrid.setMaxIters(prism.getMaxIters());
			PrismHybrid.setCompact(prism.getCompact());
			PrismHybrid.setSBMaxMem(prism.getSBMaxMem());
			PrismHybrid.setNumSBLevels(prism.getNumSBLevels());
			PrismHybrid.setSORMaxMem(prism.getSORMaxMem());
			PrismHybrid.setNumSORLevels(prism.getNumSORLevels());
			PrismHybrid.setDoSSDetect(prism.getDoSSDetect());
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
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p
						+ " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return new StateProbsMTBDD(reach, model);
			} else if ((p == 0 && relOp.equals("<")) || (p == 1 && relOp.equals(">"))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p
						+ " - formula trivially satisfies no states\n");
				return new StateProbsMTBDD(JDD.Constant(0), model);
			}
		}

		// Print a warning if Pmin/Pmax used
		if (relOp.equals("min=") || relOp.equals("max=")) {
			mainLog.print("\nWarning: \"Pmin=?\" and \"Pmax=?\" operators are identical to \"P=?\" for DTMCs/CTMCs\n");
		}

		// Compute probabilities
		boolean qual = pb != null && ((p == 0) || (p == 1)) && precomp;
		probs = checkProbPathExpression(expr.getExpression(), qual);

		// Print out probabilities
		if (prism.getVerbose()) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
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
				throw new PrismException("Invalid reward bound " + r + " in R[] formula");
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
				mainLog.print("\nWarning: checking for reward " + relOp + " " + r
						+ " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return new StateProbsMTBDD(reach, model);
			} else if (r == 0 && relOp.equals("<")) {
				mainLog.print("\nWarning: checking for reward " + relOp + " " + r
						+ " - formula trivially satisfies no states\n");
				return new StateProbsMTBDD(JDD.Constant(0), model);
			}
		}

		// print a warning if Rmin/Rmax used
		if (relOp.equals("min=") || relOp.equals("max=")) {
			mainLog.print("\nWarning: \"Rmin=?\" and \"Rmax=?\" operators are identical to \"R=?\" for DTMCs/CTMCs\n");
		}

		// compute rewards
		expr2 = expr.getExpression();
		if (expr2 instanceof ExpressionTemporal) {
			switch (((ExpressionTemporal) expr2).getOperator()) {
			case ExpressionTemporal.R_C:
				rewards = checkRewardCumul((ExpressionTemporal) expr2, stateRewards, transRewards);
				break;
			case ExpressionTemporal.R_I:
				rewards = checkRewardInst((ExpressionTemporal) expr2, stateRewards, transRewards);
				break;
			case ExpressionTemporal.R_F:
				rewards = checkRewardReach((ExpressionTemporal) expr2, stateRewards, transRewards);
				break;
			case ExpressionTemporal.R_S:
				rewards = checkRewardSS((ExpressionTemporal) expr2, stateRewards, transRewards);
				break;
			}
		}
		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		// print out rewards
		if (prism.getVerbose()) {
			mainLog.print("\nRewards (non-zero only) for all states:\n");
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

	// S operator

	protected StateProbs checkExpressionSteadyState(ExpressionSS expr) throws PrismException
	{
		Expression pb; // probability bound (expression)
		double p = 0; // probability bound (actual value)
		String relOp; // relational operator

		// bscc stuff
		Vector vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode b, bscc, sol, tmp;
		// other stuff
		StateProbs probs = null, totalProbs = null;
		int i, n;
		double d, probBSCCs[];

		// get info from steady-state operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateDouble(constantValues, null);
			if (p < 0 || p > 1)
				throw new PrismException("Invalid probability bound " + p + " in S operator");
		}

		// check for trivial (i.e. stupid) cases
		if (pb != null) {
			if ((p == 0 && relOp.equals(">=")) || (p == 1 && relOp.equals("<="))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p
						+ " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return new StateProbsMTBDD(reach, model);
			} else if ((p == 0 && relOp.equals("<")) || (p == 1 && relOp.equals(">"))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p
						+ " - formula trivially satisfies no states\n");
				return new StateProbsMTBDD(JDD.Constant(0), model);
			}
		}

		// model check argument
		b = checkExpressionDD(expr.getExpression());

		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			mainLog.print("\nComputing (B)SCCs...");
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getVectBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			n = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			n = 1;
		}

		// compute steady state for each bscc...
		probBSCCs = new double[n];
		for (i = 0; i < n; i++) {

			mainLog.println("\nComputing steady state probabilities for BSCC " + (i + 1));

			// get bscc
			bscc = (JDDNode) vectBSCCs.elementAt(i);

			// compute steady state probabilities
			try {
				probs = computeSteadyStateProbs(trans, bscc);
			} catch (PrismException e) {
				JDD.Deref(b);
				for (i = 0; i < n; i++) {
					JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
				}
				JDD.Deref(notInBSCCs);
				throw e;
			}

			// print out probabilities
			if (verbose) {
				mainLog.print("\nBSCC " + (i + 1) + " steady-state probabilities: \n");
				probs.print(mainLog);
			}

			// sum probabilities over bdd b
			d = probs.sumOverBDD(b);
			probBSCCs[i] = d;
			mainLog.print("\nBSCC " + (i + 1) + " probability: " + d + "\n");

			// free vector
			probs.clear();
		}

		// if every state is in a bscc, it's much easier...
		if (notInBSCCs.equals(JDD.ZERO)) {

			mainLog.println("\nAll states are in a BSCC (so no reachability probabilities computed)");

			// there's more efficient ways to do this if we just create the
			// solution bdd directly
			// but we actually build the prob vector so it can be printed out if
			// necessary
			tmp = JDD.Constant(0);
			for (i = 0; i < n; i++) {
				bscc = (JDDNode) vectBSCCs.elementAt(i);
				JDD.Ref(bscc);
				tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(probBSCCs[i]), bscc));
			}
			totalProbs = new StateProbsMTBDD(tmp, model);
		}
		// otherwise we have to do more work...
		else {

			// initialise total probabilities vector
			switch (engine) {
			case Prism.MTBDD:
				totalProbs = new StateProbsMTBDD(JDD.Constant(0), model);
				break;
			case Prism.SPARSE:
				totalProbs = new StateProbsDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			case Prism.HYBRID:
				totalProbs = new StateProbsDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			}

			// compute probabilities of reaching each bscc...
			for (i = 0; i < n; i++) {

				// skip bsccs with zero probability
				if (probBSCCs[i] == 0.0)
					continue;

				mainLog.println("\nComputing probabilities of reaching BSCC " + (i + 1));

				// get bscc
				bscc = (JDDNode) vectBSCCs.elementAt(i);

				// compute probabilities
				try {
					probs = computeUntilProbs(trans, trans01, notInBSCCs, bscc);
				} catch (PrismException e) {
					JDD.Deref(b);
					for (i = 0; i < n; i++) {
						JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
					}
					JDD.Deref(notInBSCCs);
					totalProbs.clear();
					throw e;
				}

				// print out probabilities
				if (verbose) {
					mainLog.print("\nBSCC " + (i + 1) + " reachability probabilities: \n");
					probs.print(mainLog);
				}

				// times by bscc prob, add to total
				probs.timesConstant(probBSCCs[i]);
				totalProbs.add(probs);

				// free vector
				probs.clear();
			}
		}

		// print out probabilities
		if (verbose) {
			mainLog.print("\nS operator probabilities: \n");
			totalProbs.print(mainLog);
		}

		// derefs
		JDD.Deref(b);
		for (i = 0; i < n; i++) {
			JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
		}
		JDD.Deref(notInBSCCs);

		// For =? properties, just return values
		if (pb == null) {
			return totalProbs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			sol = totalProbs.getBDDFromInterval(relOp, p);
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
			// free vector
			totalProbs.clear();
			return new StateProbsMTBDD(sol, model);
		}
	}

	// Contents of a P operator

	protected StateProbs checkProbPathExpression(Expression expr, boolean qual) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and then pass control to appropriate method. 
		if (expr.isSimplePathFormula()) {
			return checkProbPathExpressionSimple(expr, qual);
		} else {
			throw new PrismException("LTL-style path formulas are not supported");
		}
	}

	protected StateProbs checkProbPathExpressionSimple(Expression expr, boolean qual) throws PrismException
	{
		StateProbs probs = null;

		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				probs = checkProbPathExpressionSimple(exprUnary.getOperand(), qual);
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Compute, then subtract from 1 
				probs = checkProbPathExpressionSimple(exprUnary.getOperand(), qual);
				probs.subtractFromOne();
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
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
			// Anything else - convert to until and recurse
			else {
				probs = checkProbPathExpressionSimple(exprTemp.convertToUntilForm(), qual);
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		return probs;
	}

	// next

	protected StateProbs checkProbNext(ExpressionTemporal expr) throws PrismException
	{
		JDDNode b;
		StateProbs probs = null;

		// check not LTL
		if (expr.getOperand2().getType() != Expression.BOOLEAN)
			throw new PrismException("Invalid path formula");

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

	protected StateProbs checkProbBoundedUntil(ExpressionTemporal expr) throws PrismException
	{
		int time;
		JDDNode b1, b2;
		StateProbs probs = null;

		// check not LTL
		if (expr.getOperand1().getType() != Expression.BOOLEAN)
			throw new PrismException("Invalid path formula");
		if (expr.getOperand2().getType() != Expression.BOOLEAN)
			throw new PrismException("Invalid path formula");

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
				probs = computeBoundedUntilProbs(trans, trans01, b1, b2, time);
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

	protected StateProbs checkProbUntil(ExpressionTemporal expr, boolean qual) throws PrismException
	{
		JDDNode b1, b2;
		StateProbs probs = null;

		// check not LTL
		if (expr.getOperand1().getType() != Expression.BOOLEAN)
			throw new PrismException("Invalid path formula");
		if (expr.getOperand2().getType() != Expression.BOOLEAN)
			throw new PrismException("Invalid path formula");

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

		// if requested (i.e. when prob bound is 0 or 1 and precomputation algorithms are enabled),
		// compute probabilities qualitatively
		if (qual) {
			mainLog.print("\nWarning: probability bound in formula is"
					+ " 0/1 so exact probabilities may not be computed\n");
			probs = computeUntilProbsQual(trans01, b1, b2);
		}
		// otherwise actually compute probabilities
		else {
			try {
				probs = computeUntilProbs(trans, trans01, b1, b2);
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

	// cumulative reward

	protected StateProbs checkRewardCumul(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards)
			throws PrismException
	{
		int time; // time
		StateProbs rewards = null;

		// get info from inst reward
		time = expr.getUpperBound().evaluateInt(constantValues, null);
		if (time < 0) {
			throw new PrismException("Invalid time bound " + time + " in cumulative reward formula");
		}

		// compute rewards

		// a trivial case: "<=0"
		if (time == 0) {
			rewards = new StateProbsMTBDD(JDD.Constant(0), model);
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

	// inst reward

	protected StateProbs checkRewardInst(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards)
			throws PrismException
	{
		int time; // time
		StateProbs rewards = null;

		// get info from inst reward
		time = expr.getUpperBound().evaluateInt(constantValues, null);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in instantaneous reward property");
		}

		// compute rewards
		rewards = computeInstRewards(trans, stateRewards, time);

		return rewards;
	}

	// reach reward

	protected StateProbs checkRewardReach(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards)
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
			rewards = computeReachRewards(trans, trans01, stateRewards, transRewards, b);
		} catch (PrismException e) {
			JDD.Deref(b);
			throw e;
		}

		// derefs
		JDD.Deref(b);

		return rewards;
	}

	// steady state reward

	protected StateProbs checkRewardSS(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards)
			throws PrismException
	{
		// bscc stuff
		Vector vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode newStateRewards, bscc, tmp;
		// other stuff
		StateProbs probs = null, rewards = null;
		int i, n;
		double d, rewBSCCs[];

		// compute rewards corresponding to each state
		JDD.Ref(trans);
		JDD.Ref(transRewards);
		newStateRewards = JDD.SumAbstract(JDD.Apply(JDD.TIMES, trans, transRewards), allDDColVars);
		JDD.Ref(stateRewards);
		newStateRewards = JDD.Apply(JDD.PLUS, newStateRewards, stateRewards);

		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			mainLog.print("\nComputing (B)SCCs...");
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getVectBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			n = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			n = 1;
		}

		// compute steady state for each bscc...
		rewBSCCs = new double[n];
		for (i = 0; i < n; i++) {

			mainLog.println("\nComputing steady state probabilities for BSCC " + (i + 1));

			// get bscc
			bscc = (JDDNode) vectBSCCs.elementAt(i);

			// compute steady state probabilities
			try {
				probs = computeSteadyStateProbs(trans, bscc);
			} catch (PrismException e) {
				JDD.Deref(newStateRewards);
				for (i = 0; i < n; i++) {
					JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
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

			mainLog.println("\nAll states are in a BSCC (so no reachability probabilities computed)");

			// build the reward vector
			tmp = JDD.Constant(0);
			for (i = 0; i < n; i++) {
				bscc = (JDDNode) vectBSCCs.elementAt(i);
				JDD.Ref(bscc);
				tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(rewBSCCs[i]), bscc));
			}
			rewards = new StateProbsMTBDD(tmp, model);
		}
		// otherwise we have to do more work...
		else {

			// initialise rewards vector
			switch (engine) {
			case Prism.MTBDD:
				rewards = new StateProbsMTBDD(JDD.Constant(0), model);
				break;
			case Prism.SPARSE:
				rewards = new StateProbsDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			case Prism.HYBRID:
				rewards = new StateProbsDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			}

			// compute probabilities of reaching each bscc...
			for (i = 0; i < n; i++) {

				// skip bsccs with zero reward
				if (rewBSCCs[i] == 0.0)
					continue;

				mainLog.println("\nComputing probabilities of reaching BSCC " + (i + 1));

				// get bscc
				bscc = (JDDNode) vectBSCCs.elementAt(i);

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
		for (i = 0; i < n; i++) {
			JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
		}
		JDD.Deref(notInBSCCs);

		return rewards;
	}

	// -----------------------------------------------------------------------------------
	// do steady state computation
	// -----------------------------------------------------------------------------------

	// steady state computation (from initial states)

	public StateProbs doSteadyState() throws PrismException
	{
		// bscc stuff
		Vector vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode start, bscc, tmp;
		// other stuff
		StateProbs probs = null, solnProbs = null;
		double d, probBSCCs[];
		int i, n, whichBSCC, bsccCount;

		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			mainLog.print("\nComputing (B)SCCs...");
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getVectBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			n = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			n = 1;
		}

		// get initial states of model
		start = model.getStart();

		// see how many bsccs contain initial states and, if just one, which one
		whichBSCC = -1;
		bsccCount = 0;
		for (i = 0; i < n; i++) {
			bscc = (JDDNode) vectBSCCs.elementAt(i);
			JDD.Ref(bscc);
			JDD.Ref(start);
			tmp = JDD.And(bscc, start);
			if (!tmp.equals(JDD.ZERO)) {
				bsccCount++;
				if (bsccCount == 1)
					whichBSCC = i;
			}
			JDD.Deref(tmp);
		}

		// if all initial states are in a single bscc, it's easy...
		JDD.Ref(notInBSCCs);
		JDD.Ref(start);
		tmp = JDD.And(notInBSCCs, start);
		if (tmp.equals(JDD.ZERO) && bsccCount == 1) {

			JDD.Deref(tmp);

			mainLog.println("\nInitial states all in one BSCC (so no reachability probabilities computed)");

			// get bscc
			bscc = (JDDNode) vectBSCCs.elementAt(whichBSCC);

			// compute steady-state probabilities for the bscc
			try {
				solnProbs = computeSteadyStateProbs(trans, bscc);
			} catch (PrismException e) {
				for (i = 0; i < n; i++) {
					JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
				}
				JDD.Deref(notInBSCCs);
				throw e;
			}
		}

		// otherwise have to consider all the bsccs
		else {

			JDD.Deref(tmp);

			// initialise total probabilities vector
			switch (engine) {
			case Prism.MTBDD:
				solnProbs = new StateProbsMTBDD(JDD.Constant(0), model);
				break;
			case Prism.SPARSE:
				solnProbs = new StateProbsDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			case Prism.HYBRID:
				solnProbs = new StateProbsDV(new DoubleVector((int) model.getNumStates()), model);
				break;
			}

			// compute prob of reaching each bscc from initial state
			probBSCCs = new double[n];
			for (i = 0; i < n; i++) {

				mainLog.println("\nComputing probability of reaching BSCC " + (i + 1));

				// get bscc
				bscc = (JDDNode) vectBSCCs.elementAt(i);

				// compute probabilities
				try {
					probs = computeUntilProbs(trans, trans01, notInBSCCs, bscc);
				} catch (PrismException e) {
					for (i = 0; i < n; i++) {
						JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
					}
					JDD.Deref(notInBSCCs);
					solnProbs.clear();
					throw e;
				}

				// sum probabilities over bdd for initial state
				// and then divide by number of start states
				// (we assume an equiprobable initial probability distribution
				// over all initial states)
				d = probs.sumOverBDD(start);
				d /= model.getNumStartStates();
				probBSCCs[i] = d;
				mainLog.print("\nBSCC " + (i + 1) + " Probability: " + d + "\n");

				// free vector
				probs.clear();
			}

			// compute steady-state for each bscc
			for (i = 0; i < n; i++) {

				mainLog.println("\nComputing steady-state probabilities for BSCC " + (i + 1));

				// get bscc
				bscc = (JDDNode) vectBSCCs.elementAt(i);

				// compute steady-state probabilities for the bscc
				try {
					probs = computeSteadyStateProbs(trans, bscc);
				} catch (PrismException e) {
					for (i = 0; i < n; i++) {
						JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
					}
					JDD.Deref(notInBSCCs);
					solnProbs.clear();
					throw e;
				}

				// print out probabilities
				if (verbose) {
					mainLog.print("\nBSCC " + (i + 1) + " Steady-State Probabilities: \n");
					probs.print(mainLog);
				}

				// times by bscc reach prob, add to total
				probs.timesConstant(probBSCCs[i]);
				solnProbs.add(probs);

				// free vector
				probs.clear();
			}
		}

		// derefs
		for (i = 0; i < n; i++) {
			JDD.Deref((JDDNode) vectBSCCs.elementAt(i));
		}
		JDD.Deref(notInBSCCs);

		return solnProbs;
	}

	// -----------------------------------------------------------------------------------
	// do transient computation
	// -----------------------------------------------------------------------------------

	// transient computation (from initial states)

	public StateProbs doTransient(int time) throws PrismException
	{
		// mtbdd stuff
		JDDNode start, init;
		// other stuff
		StateProbs probs = null;

		// get initial states of model
		start = model.getStart();

		// and hence compute initial probability distribution (equiprobable over
		// all start states)
		JDD.Ref(start);
		init = JDD.Apply(JDD.DIVIDE, start, JDD.Constant(JDD.GetNumMinterms(start, allDDRowVars.n())));

		// compute transient probabilities
		try {
			// special case: time = 0
			if (time == 0) {
				JDD.Ref(init);
				probs = new StateProbsMTBDD(init, model);
			} else {
				probs = computeTransientProbs(trans, init, time);
			}
		} catch (PrismException e) {
			JDD.Deref(init);
			throw e;
		}

		// derefs
		JDD.Deref(init);

		return probs;
	}

	// -----------------------------------------------------------------------------------
	// probability computation methods
	// -----------------------------------------------------------------------------------

	// compute probabilities for next

	protected StateProbs computeNextProbs(JDDNode tr, JDDNode b)
	{
		JDDNode tmp;
		StateProbs probs = null;

		// matrix multiply: trans * b
		JDD.Ref(b);
		tmp = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		JDD.Ref(tr);
		tmp = JDD.MatrixMultiply(tr, tmp, allDDColVars, JDD.BOULDER);
		probs = new StateProbsMTBDD(tmp, model);

		return probs;
	}

	// compute probabilities for bounded until

	protected StateProbs computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2, int time)
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
			probs = new StateProbsMTBDD(yes, model);
		}
		// otherwise explicitly compute the remaining probabilities
		else {
			// compute probabilities
			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.ProbBoundedUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe, time);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.ProbBoundedUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe, time);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.ProbBoundedUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe, time);
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

	protected StateProbs computeUntilProbsQual(JDDNode tr01, JDDNode b1, JDDNode b2)
	{
		JDDNode yes, no, maybe;
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
			// yes
			yes = PrismMTBDD.Prob1(tr01, allDDRowVars, allDDColVars, b1, b2);
			if (yes.equals(reach)) {
				no = JDD.Constant(0);
			} else {
				no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, b1, yes);
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
			probs = new StateProbsMTBDD(JDD.Apply(JDD.PLUS, yes, JDD.Apply(JDD.TIMES, maybe, JDD.Constant(0.5))), model);
		}

		// derefs
		JDD.Deref(yes);
		JDD.Deref(no);
		JDD.Deref(maybe);

		return probs;
	}

	// compute probabilities for until (general case)

	protected StateProbs computeUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2) throws PrismException
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
			if (precomp) {
				yes = PrismMTBDD.Prob1(tr01, allDDRowVars, allDDColVars, b1, b2);
			} else {
				JDD.Ref(b2);
				yes = b2;
			}
			// no
			if (yes.equals(reach)) {
				no = JDD.Constant(0);
			} else if (precomp) {
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
			// we make sure to return a vector of the appropriate type
			// (doublevector for hybrid/sparse, mtbdd for mtbdd)
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(yes);
				probs = new StateProbsMTBDD(yes, model);
				break;
			case Prism.SPARSE:
			case Prism.HYBRID:
				probs = new StateProbsDV(yes, model);
				break;
			}
		}
		// otherwise we compute the actual probabilities
		else {
			// compute probabilities
			mainLog.println("\nComputing remaining probabilities...");

			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
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

	// compute cumulative rewards

	protected StateProbs computeCumulRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, int time)
			throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;

		// compute rewards
		try {
			switch (engine) {
			case Prism.MTBDD:
				rewardsMTBDD = PrismMTBDD.ProbCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsMTBDD(rewardsMTBDD, model);
				break;
			case Prism.SPARSE:
				rewardsDV = PrismSparse.ProbCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				rewardsDV = PrismHybrid.ProbCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsDV(rewardsDV, model);
				break;
			default:
				throw new PrismException("Engine does not support this numerical method");
			}
		} catch (PrismException e) {
			throw e;
		}

		return rewards;
	}

	// compute rewards for inst reward

	protected StateProbs computeInstRewards(JDDNode tr, JDDNode sr, int time) throws PrismException
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
					rewardsMTBDD = PrismMTBDD.ProbInstReward(tr, sr, odd, allDDRowVars, allDDColVars, time);
					rewards = new StateProbsMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.ProbInstReward(tr, sr, odd, allDDRowVars, allDDColVars, time);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					rewardsDV = PrismHybrid.ProbInstReward(tr, sr, odd, allDDRowVars, allDDColVars, time);
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

	protected StateProbs computeReachRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b)
			throws PrismException
	{
		JDDNode inf, maybe;
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;

		// compute states which can't reach goal with probability 1
		if (b.equals(JDD.ZERO)) {
			JDD.Ref(reach);
			inf = reach;
			maybe = JDD.Constant(0);
		} else if (b.equals(reach)) {
			inf = JDD.Constant(0);
			maybe = JDD.Constant(0);
		} else {
			JDDNode prob1 = PrismMTBDD.Prob1(tr01, allDDRowVars, allDDColVars, reach, b);
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
			rewards = new StateProbsMTBDD(JDD.ITE(inf, JDD.PlusInfinity(), JDD.Constant(0)), model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			mainLog.println("\nComputing remaining rewards...");

			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf,
							maybe);
					rewards = new StateProbsMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse
							.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					rewardsDV = PrismHybrid
							.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				default:
					throw new PrismException("Engine does not support this numerical method");
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

	// compute steady-state probabilities

	// tr = the rate matrix for the whole Markov chain
	// states = the subset of reachable states (e.g. bscc) for which
	// steady-state is to be done

	protected StateProbs computeSteadyStateProbs(JDDNode tr, JDDNode subset) throws PrismException
	{
		JDDNode trf, init;
		long n;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;

		// work out number of states in 'subset'
		if (tr.equals(reach)) {
			// avoid a call to GetNumMinterms in this simple (and common) case
			n = model.getNumStates();
		} else {
			n = Math.round(JDD.GetNumMinterms(subset, allDDRowVars.n()));
		}

		// special case - there is only one state in 'subset'
		// (in fact, we need to check for this special case because the general
		// solution work breaks)
		if (n == 1) {
			// answer is trivially one in the single state
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(subset);
				return new StateProbsMTBDD(subset, model);
			case Prism.SPARSE:
				return new StateProbsDV(subset, model);
			case Prism.HYBRID:
				return new StateProbsDV(subset, model);
			}
		}

		// filter out unwanted states from transition matrix
		JDD.Ref(tr);
		JDD.Ref(subset);
		trf = JDD.Apply(JDD.TIMES, tr, subset);
		JDD.Ref(subset);
		trf = JDD.Apply(JDD.TIMES, trf, JDD.PermuteVariables(subset, allDDRowVars, allDDColVars));

		// compute initial solution (equiprobable)
		JDD.Ref(subset);
		init = JDD.Apply(JDD.DIVIDE, subset, JDD.Constant(n));

		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateProbsMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateProbsDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateProbsDV(probsDV, model);
				break;
			default:
				throw new PrismException("Engine does not support this numerical method");
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

	// compute transient probabilities

	protected StateProbs computeTransientProbs(JDDNode tr, JDDNode init, int time) throws PrismException
	{
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;

		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.ProbTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.ProbTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.ProbTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsDV(probsDV, model);
				break;
			default:
				throw new PrismException("Engine does not support this numerical method");
			}
		} catch (PrismException e) {
			throw e;
		}

		return probs;
	}
}

// ------------------------------------------------------------------------------
