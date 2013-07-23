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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionExists;
import parser.ast.ExpressionForAll;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.PropertiesFile;
import cex.CexPathAsBDDs;
import cex.CexPathStates;

/*
 * Non probabilistic model checker, initially for CTL.
 */
public class NonProbModelChecker extends StateModelChecker
{
	// Counterexample stuff
	private boolean doGenCex = true;

	// Constructor

	public NonProbModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);
	}

	// -----------------------------------------------------------------------------------
	// Check a property, i.e. an expression
	// -----------------------------------------------------------------------------------

	// Check expression (recursive)

	public StateValues checkExpression(Expression expr) throws PrismException
	{
		StateValues res;

		// E operator
		if (expr instanceof ExpressionExists) {
			res = checkExpressionExists(((ExpressionExists) expr).getExpression());
		}
		// A operator
		else if (expr instanceof ExpressionForAll) {
			res = checkExpressionForAll(((ExpressionForAll) expr).getExpression());
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

	/**
	 * Model check a CTL exists (E) operator.
	 */
	protected StateValues checkExpressionExists(Expression expr) throws PrismException
	{
		StateValues res = null;

		// Check whether this is a simple path formula (i.e. CTL, not LTL)
		if (!expr.isSimplePathFormula()) {
			throw new PrismException("(Non-probabilistic) LTL model checking is not supported");
		}

		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				res = checkExpressionExists(exprUnary.getOperand());
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Compute, then negate 
				res = checkExpressionForAll(exprUnary.getOperand());
				res.subtractFromOne();
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			if (exprTemp.hasBounds()) {
				throw new PrismException("Model checking of bounded CTL operators is not supported");
			}
			// Next (EX)
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				// TODO
				throw new PrismException("CTL model checking of the E X operator is not yet supported");
			}
			// Until (EU)
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				res = checkExistsUntil(exprTemp);
			}
			// Globally (EG)
			else if (exprTemp.getOperator() == ExpressionTemporal.P_G) {
				res = checkExistsGlobally(exprTemp);
			}
			// Anything else - convert to until and recurse
			else {
				res = checkExpressionExists(exprTemp.convertToUntilForm());
			}
		}

		if (res == null)
			throw new PrismException("Unrecognised path operator in E operator");

		return res;
	}

	/**
	 * Model check a CTL forall (A) operator.
	 */
	protected StateValues checkExpressionForAll(Expression expr) throws PrismException
	{
		StateValues res = null;

		// Check whether this is a simple path formula (i.e. CTL, not LTL)
		if (!expr.isSimplePathFormula()) {
			throw new PrismException("(Non-probabilistic) LTL model checking is not supported");
		}

		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				res = checkExpressionForAll(exprUnary.getOperand());
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Compute, then negate 
				res = checkExpressionExists(exprUnary.getOperand());
				res.subtractFromOne();
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			if (exprTemp.hasBounds()) {
				throw new PrismException("Model checking of bounded CTL operators is not supported");
			}
			// Next (AX)
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				// TODO
				throw new PrismException("CTL model checking of the A X operator is not yet supported");
			}
			// Until (AU)
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				throw new PrismException("CTL model checking of the A U operator is not yet supported");
			}
			// Eventually (AF)
			else if (exprTemp.getOperator() == ExpressionTemporal.P_F) {
				// Reduce to EG (AF phi = !AG !phi)
				ExpressionTemporal exprCopy = (ExpressionTemporal) exprTemp.deepCopy();
				exprCopy.setOperator(ExpressionTemporal.P_G);
				exprCopy.setOperand2(Expression.Not(exprCopy.getOperand2()));
				res = checkExpressionExists(exprCopy);
				res.subtractFromOne();
			}
			// Anything else - convert to until and recurse
			else {
				res = checkExpressionForAll(exprTemp.convertToUntilForm());
			}
		}

		if (res == null)
			throw new PrismException("Unrecognised path operator in E operator");

		return res;
	}

	/**
	 * Model check a CTL exists until (EU) operator.
	 */
	protected StateValues checkExistsUntil(ExpressionTemporal expr) throws PrismException
	{
		JDDNode b1, b2, transRel, tmp, tmp2, tmp3, init = null;
		ArrayList<JDDNode> cexDDs = null;
		JDDNode cexInit = null;
		boolean done, cexDone = false;
		Vector<String> cexActions;
		int iters, i;
		long l;

		// Model check operands first
		b1 = checkExpressionDD(expr.getOperand1());
		try {
			b2 = checkExpressionDD(expr.getOperand2());
		} catch (PrismException e) {
			JDD.Deref(b1);
			throw e;
		}

		l = System.currentTimeMillis();

		// Create storage for counterexample, if required
		if (doGenCex) {
			cexDDs = new ArrayList<JDDNode>();
			cexDone = false;
			init = model.getStart();
		}

		// Get transition relation
		if (model.getModelType() == ModelType.MDP) {
			JDD.Ref(trans01);
			transRel = JDD.ThereExists(trans01, ((NondetModel) model).getAllDDNondetVars());
		} else {
			JDD.Ref(trans01);
			transRel = trans01;
		}

		// Fixpoint loop
		done = false;
		iters = 0;
		tmp = JDD.Constant(0);
		while (!done) {
			iters++;
			JDD.Ref(tmp);
			tmp2 = JDD.PermuteVariables(tmp, allDDRowVars, allDDColVars);
			JDD.Ref(transRel);
			tmp2 = JDD.And(tmp2, transRel);
			tmp2 = JDD.ThereExists(tmp2, allDDColVars);
			JDD.Ref(b1);
			tmp2 = JDD.And(b1, tmp2);
			JDD.Ref(b2);
			tmp2 = JDD.Or(b2, tmp2);
			if (tmp2.equals(tmp)) {
				done = true;
			}
			// Store counterexample info, if required (and not finished)
			if (doGenCex && !cexDone) {
				// Store the newly discovered states from this iteration
				JDD.Ref(tmp);
				JDD.Ref(tmp2);
				cexDDs.add(JDD.And(tmp2, JDD.Not(tmp)));
				// See if we have found the initial state yet, and if so, don't store any more info
				if (JDD.AreInterecting(tmp2, init)) {
					cexDone = true;
					// Choose an initial state (in case there are several) which intersects
					JDD.Ref(tmp2);
					JDD.Ref(init);
					cexInit = JDD.And(tmp2, init);
					cexInit = JDD.RestrictToFirst(cexInit, allDDRowVars);
				}
			}
			JDD.Deref(tmp);
			tmp = tmp2;
		}

		// Print iterations/timing info
		l = System.currentTimeMillis() - l;
		mainLog.println("\nCTL EU fixpoint: " + iters + " iterations in " + (l / 1000.0) + " seconds");

		// Process the counterexample info to produce a trace 
		if (doGenCex) {
			if (!cexDone) {
				for (i = 0; i < cexDDs.size(); i++) {
					JDD.Deref(cexDDs.get(i));
				}
			} else {
				mainLog.println("Processing counterexample trace (" + cexDDs.size() + " states long)...");
				// First state of counterexample (at end of array) is initial state
				JDD.Deref(cexDDs.get(cexDDs.size() - 1));
				cexDDs.set(cexDDs.size() - 1, cexInit);
				// Go through remaining steps of counterexample
				for (i = cexDDs.size() - 2; i >= 0; i--) {
					// Get states that are a successor of the previous state of the counterexample
					JDD.Ref(cexDDs.get(i + 1));
					JDD.Ref(transRel);
					tmp3 = JDD.And(cexDDs.get(i + 1), transRel);
					tmp3 = JDD.ThereExists(tmp3, allDDRowVars);
					tmp3 = JDD.PermuteVariables(tmp3, allDDColVars, allDDRowVars);
					// Intersect with possible states for this step of the counterexample
					JDD.Ref(cexDDs.get(i));
					tmp3 = JDD.And(tmp3, cexDDs.get(i));
					// Pick one of these state (the first)
					tmp3 = JDD.PermuteVariables(JDD.RestrictToFirst(tmp3, allDDColVars), allDDColVars, allDDRowVars);
					// Replace this state in the counterexample
					JDD.Deref(cexDDs.get(i));
					cexDDs.set(i, tmp3);
				}
				// Construct counterexample object
				CexPathStates cex = new CexPathStates(model);
				for (i = cexDDs.size() - 1; i >= 0; i--) {
					cex.addState(model.convertBddToState(cexDDs.get(i)));
					JDD.Deref(cexDDs.get(i));
				}
				result.setCounterexample(cex);
				if (1 == 2) {
					// For an MDP model, build a list of actions from counterexample
					if (model.getModelType() == ModelType.MDP) {
						cexActions = new Vector<String>();
						for (i = cexDDs.size() - 1; i >= 1; i--) {
							JDD.Ref(trans01);
							JDD.Ref(cexDDs.get(i));
							tmp3 = JDD.And(trans01, cexDDs.get(i));
							JDD.Ref(cexDDs.get(i - 1));
							tmp3 = JDD.And(tmp3, JDD.PermuteVariables(cexDDs.get(i - 1), allDDRowVars, allDDColVars));
							tmp3 = JDD.ThereExists(tmp3, allDDColVars);
							JDD.Ref(transActions);
							tmp3 = JDD.Apply(JDD.TIMES, tmp3, transActions);
							int action = (int) JDD.FindMax(tmp3);
							cexActions.add(action > 0 ? model.getSynchs().get(action - 1) : "");
							JDD.Deref(tmp3);
							JDD.Deref(cexDDs.get(i));
						}
						JDD.Deref(cexDDs.get(0));
						mainLog.println("Counterexample (action sequence): " + cexActions);
						result.setCounterexample(cexActions);
					}
					// Otherwise, convert list of BDDs to list of states
					else {
						CexPathAsBDDs cexBDDs = new CexPathAsBDDs(model);
						for (i = cexDDs.size() - 1; i >= 0; i--) {
							cexBDDs.addState(cexDDs.get(i));
							JDD.Deref(cexDDs.get(i));
						}
						result.setCounterexample(cexBDDs);
					}
				}
			}
		}

		// Derefs
		JDD.Deref(b1);
		JDD.Deref(b2);
		JDD.Deref(transRel);

		return new StateValuesMTBDD(tmp, model);
	}

	/**
	 * Model check a CTL exists globally (EG) operator.
	 */
	protected StateValues checkExistsGlobally(ExpressionTemporal expr) throws PrismException
	{
		JDDNode b2, transRel, tmp, tmp2;
		boolean done;
		int iters, i;
		long l;
		// SCC stuff
		List<JDDNode> sccs = null;
		JDDNode notInSCCs = null;
		int numSCCs = 0;

		// Model check operand first
		b2 = checkExpressionDD(expr.getOperand2());

		l = System.currentTimeMillis();

		// Get transition relation
		if (model.getModelType() == ModelType.MDP) {
			JDD.Ref(trans01);
			transRel = JDD.ThereExists(trans01, ((NondetModel) model).getAllDDNondetVars());
		} else {
			JDD.Ref(trans01);
			transRel = trans01;
		}

		// Strip out non-b2 states
		JDD.Ref(b2);
		transRel = JDD.And(transRel, b2);
		JDD.Ref(b2);
		transRel = JDD.And(transRel, JDD.PermuteVariables(b2, allDDRowVars, allDDColVars));

		// Find SCCs in resulting stripped down transition relation
		SCCComputer sccComputer = prism.getSCCComputer(reach, transRel, allDDRowVars, allDDColVars);
		sccComputer.computeSCCs();
		sccs = sccComputer.getSCCs();
		notInSCCs = sccComputer.getNotInSCCs();
		numSCCs = sccs.size();

		// Collect states from non-trivial SCCs in 'target'
		// (non-trivial = has a transition)
		JDDNode target = JDD.Create();
		for (i = 0; i < numSCCs; i++) {
			JDDNode scc = sccs.get(i);
			if (scc != null) {
				if (JDD.AreInterecting(scc, transRel)) {
					JDD.Ref(scc);
					target = JDD.Or(target, scc);
				}
			}
		}
		mainLog.println("\nCTL EG non-trivial SCC states: " + JDD.GetNumMintermsString(target, allDDRowVars.n()));

		// Fixpoint loop - backwards reach
		done = false;
		iters = 0;
		tmp = JDD.Constant(0);
		while (!done) {
			iters++;
			JDD.Ref(tmp);
			tmp2 = JDD.PermuteVariables(tmp, allDDRowVars, allDDColVars);
			JDD.Ref(transRel);
			tmp2 = JDD.And(tmp2, transRel);
			tmp2 = JDD.ThereExists(tmp2, allDDColVars);
			JDD.Ref(target);
			tmp2 = JDD.Or(target, tmp2);
			if (tmp2.equals(tmp)) {
				done = true;
			}
			JDD.Deref(tmp);
			tmp = tmp2;
		}

		// Print iterations/timing info
		l = System.currentTimeMillis() - l;
		mainLog.println("CTL EG reachability fixpoint: " + iters + " iterations in " + (l / 1000.0) + " seconds");

		// Derefs
		JDD.Deref(b2);
		JDD.Deref(target);
		JDD.Deref(transRel);
		for (i = 0; i < numSCCs; i++) {
			if (sccs.get(i) != null)
				JDD.Deref(sccs.get(i));
		}
		if (notInSCCs != null)
			JDD.Deref(notInSCCs);

		return new StateValuesMTBDD(tmp, model);
	}
}

// ------------------------------------------------------------------------------
