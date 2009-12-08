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

import java.util.*;

import jdd.*;
import parser.*;
import parser.ast.*;

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

	public StateProbs checkExpression(Expression expr) throws PrismException
	{
		StateProbs res;

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
		if (res instanceof StateProbsMTBDD)
			res.filter(reach);

		return res;
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	/**
	 * Model check a CTL exists (E) operator.
	 */
	protected StateProbs checkExpressionExists(Expression expr) throws PrismException
	{
		StateProbs res = null;

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
				// Compute, then subtract from 1 
				res = checkExpressionExists(exprUnary.getOperand()); //TODO: forall
				res.subtractFromOne(); //TODO
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				// TODO
				throw new PrismException("CTL model checking of this operator is not yet supported");
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					// TODO
					throw new PrismException("CTL model checking of this operator is not yet supported");
				} else {
					res = checkExistsUntil(exprTemp);
				}
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
	 * Model check a CTL exists until (EU) operator.
	 */
	protected StateProbs checkExistsUntil(ExpressionTemporal expr) throws PrismException
	{
		JDDNode b1, b2, transRel, tmp, tmp2, tmp3, tmp4, init = null;
		ArrayList<JDDNode> cexDDs = null;
		boolean done, cexDone = false;
		List<State> cexStates;
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
			transRel = JDD.ThereExists(trans01, ((NondetModel)model).getAllDDNondetVars());
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
				if (JDD.AreInterecting(tmp2, init))
					cexDone = true;
			}
			JDD.Deref(tmp);
			tmp = tmp2;
		}

		// Process the counterexample info to produce a trace 
		if (doGenCex) {
			mainLog.println("\nProcessing counterexample trace (length " + (cexDDs.size() - 1) + ")...");
			// First state of counterexample (at end of array) is initial state
			JDD.Deref(cexDDs.get(cexDDs.size() - 1));
			JDD.Ref(init);
			cexDDs.set(cexDDs.size() - 1, init);
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
					int action = (int)JDD.FindMax(tmp3);
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
				cexStates = new ArrayList<State>(cexDDs.size());
				for (i = cexDDs.size() - 1; i >= 0; i--) {
					cexStates.add(model.convertBddToState(cexDDs.get(i)));
					JDD.Deref(cexDDs.get(i));
				}
				result.setCounterexample(cexStates);
			}
		}

		l = System.currentTimeMillis() - l;

		// Derefs
		JDD.Deref(b1);
		JDD.Deref(b2);
		JDD.Deref(transRel);

		// Print iterations/timing info
		mainLog.println("\nCTL EU fixpoint: " + iters + " iterations in " + (l / 1000.0) + " seconds");

		return new StateProbsMTBDD(tmp, model);
	}

	/**
	 * Model check a CTL forall (A) operator.
	 */
	protected StateProbs checkExpressionForAll(Expression expr) throws PrismException
	{
		throw new PrismException("CTL model checking of this operator is not yet supported");
	}
}

// ------------------------------------------------------------------------------
