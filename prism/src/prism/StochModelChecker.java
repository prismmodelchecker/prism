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

import jdd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.ast.*;

/*
 * Model checker for CTMCs.
 * 
 * Much of StochModelChecker's functionality is inherited from the parent
 * class ProbModelChecker (for DTMCs). Main differences are: 
 *  - bounded until: time bounds are doubles and computation different
 *  - next/unbounded until: prob computation uses embedded Markov chain
 *  - cumulative/instantaneous reward: times are doubles, computation different
 *  - reach rewards: ...
 *  - doTransient
 *  
 *  TODO: finish this doc
 */
public class StochModelChecker extends ProbModelChecker
{
	// Constructor

	public StochModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	// bounded until

	protected StateProbs checkProbBoundedUntil(PathExpressionTemporal pe) throws PrismException
	{
		Expression expr1, expr2;
		double lTime, uTime; // time bounds
		Expression expr;
		JDDNode b1, b2, tmp;
		StateProbs tmpProbs = null, probs = null;

		// get operands
		if (!(pe.getOperand1() instanceof PathExpressionExpr) || !(pe.getOperand2() instanceof PathExpressionExpr))
			throw new PrismException("Invalid path formula");
		expr1 = ((PathExpressionExpr) pe.getOperand1()).getExpression();
		expr2 = ((PathExpressionExpr) pe.getOperand2()).getExpression();

		// get info from bounded until

		// lower bound is 0 if not specified
		// (i.e. if until is of form U<=t)
		expr = pe.getLowerBound();
		if (expr != null) {
			lTime = expr.evaluateDouble(constantValues, null);
			if (lTime < 0) {
				throw new PrismException("Invalid lower bound " + lTime + " in time-bounded until formula");
			}
		} else {
			lTime = 0;
		}
		// upper bound is -1 if not specified
		// (i.e. if until is of form U>=t)
		expr = pe.getUpperBound();
		if (expr != null) {
			uTime = expr.evaluateDouble(constantValues, null);
			if (uTime < 0) {
				throw new PrismException("Invalid upper bound " + uTime + " in time-bounded until formula");
			}
			if (uTime < lTime) {
				throw new PrismException("Upper bound must exceed lower bound in time-bounded until formula");
			}
		} else {
			uTime = -1;
		}

		// model check operands first
		b1 = checkExpressionDD(expr1);
		try {
			b2 = checkExpressionDD(expr2);
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
		if (lTime == 0 && uTime == 0) {
			// prob is 1 in b2 states, 0 otherwise
			JDD.Ref(b2);
			probs = new StateProbsMTBDD(b2, model);
		} else {

			// break down into different cases to compute probabilities

			// >= lTime
			if (uTime == -1) {
				// check for special case of lTime == 0, this is actually an
				// unbounded until
				if (lTime == 0) {
					// compute probs
					try {
						probs = computeUntilProbs(trans, trans01, b1, b2);
					} catch (PrismException e) {
						JDD.Deref(b1);
						JDD.Deref(b2);
						throw e;
					}
				} else {
					// compute unbounded until probs
					try {
						tmpProbs = computeUntilProbs(trans, trans01, b1, b2);
					} catch (PrismException e) {
						JDD.Deref(b1);
						JDD.Deref(b2);
						throw e;
					}
					// compute bounded until probs
					try {
						probs = computeBoundedUntilProbs(trans, trans01, b1, b1, lTime, tmpProbs);
					} catch (PrismException e) {
						tmpProbs.clear();
						JDD.Deref(b1);
						JDD.Deref(b2);
						throw e;
					}
					tmpProbs.clear();
				}
			}
			// <= uTime
			else if (lTime == 0) {
				// nb: uTime != 0 since would be caught above (trivial case)
				JDD.Ref(b1);
				JDD.Ref(b2);
				tmp = JDD.And(b1, JDD.Not(b2));
				try {
					probs = computeBoundedUntilProbs(trans, trans01, b2, tmp, uTime, null);
				} catch (PrismException e) {
					JDD.Deref(tmp);
					JDD.Deref(b1);
					JDD.Deref(b2);
					throw e;
				}
				JDD.Deref(tmp);
			}
			// [lTime,uTime] (including where lTime == uTime)
			else {
				JDD.Ref(b1);
				JDD.Ref(b2);
				tmp = JDD.And(b1, JDD.Not(b2));
				try {
					tmpProbs = computeBoundedUntilProbs(trans, trans01, b2, tmp, uTime - lTime, null);
				} catch (PrismException e) {
					JDD.Deref(tmp);
					JDD.Deref(b1);
					JDD.Deref(b2);
					throw e;
				}
				JDD.Deref(tmp);
				try {
					probs = computeBoundedUntilProbs(trans, trans01, b1, b1, lTime, tmpProbs);
				} catch (PrismException e) {
					tmpProbs.clear();
					JDD.Deref(b1);
					JDD.Deref(b2);
					throw e;
				}
				tmpProbs.clear();
			}
		}

		// derefs
		JDD.Deref(b1);
		JDD.Deref(b2);

		return probs;
	}

	// cumulative reward

	protected StateProbs checkRewardCumul(PathExpressionTemporal pe, JDDNode stateRewards, JDDNode transRewards)
			throws PrismException
	{
		double time; // time
		StateProbs rewards = null;

		// get info from inst reward
		time = pe.getUpperBound().evaluateDouble(constantValues, null);
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

	protected StateProbs checkRewardInst(PathExpressionTemporal pe, JDDNode stateRewards, JDDNode transRewards)
			throws PrismException
	{
		double time; // time
		StateProbs sr = null, rewards = null;

		// get info from inst reward
		time = pe.getUpperBound().evaluateDouble(constantValues, null);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in instantaneous reward property");
		}

		// compute rewards

		// a trivial case: "=0"
		if (time == 0) {
			JDD.Ref(stateRewards);
			rewards = new StateProbsMTBDD(stateRewards, model);
		} else {
			// convert state rewards vector to appropriate type (depending on
			// engine)
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(stateRewards);
				sr = new StateProbsMTBDD(stateRewards, model);
				break;
			case Prism.SPARSE:
				sr = new StateProbsDV(stateRewards, model);
				break;
			case Prism.HYBRID:
				sr = new StateProbsDV(stateRewards, model);
				break;
			}
			// and for the computation, we can reuse the computation for
			// time-bounded until formulae
			// which is nice
			try {
				rewards = computeBoundedUntilProbs(trans, trans01, reach, reach, time, sr);
			} catch (PrismException e) {
				sr.clear();
				throw e;
			}
			sr.clear();
		}

		return rewards;
	}

	// -----------------------------------------------------------------------------------
	// do transient computation
	// -----------------------------------------------------------------------------------

	// transient computation (from initial states)

	public StateProbs doTransient(double time) throws PrismException
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
			if (time == 0.0) {
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
		JDDNode diags, emb;
		StateProbs probs = null;

		// Compute embedded Markov chain
		JDD.Ref(tr);
		diags = JDD.SumAbstract(tr, allDDColVars);
		JDD.Ref(tr);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		mainLog.println("\nDiagonals vector: " + JDD.GetInfoString(diags, allDDRowVars.n()));
		mainLog.println("Embedded Markov chain: " + JDD.GetInfoString(emb, allDDRowVars.n() * 2));

		// And then use superclass (ProbModelChecker)
		// to compute probabilities
		probs = super.computeNextProbs(emb, b);

		// derefs
		JDD.Deref(diags);
		JDD.Deref(emb);

		return probs;
	}

	// compute probabilities for bounded until

	// nb: this is a generic function used by several different parts of the csl
	// bounded until
	// model checking algorithm. it actually computes, for each state, the sum
	// over 'b2' states
	// of the probability of being in that state at time 'time' multiplied by
	// the corresponding
	// probability in the vector 'multProbs', assuming that all states not in
	// 'nonabs' are absorbing
	// nb: if 'multProbs' is null it is assumed to be all 1s
	// nb: if not null, the type (StateProbsDV/MTBDD) of 'multProbs' must match
	// the current engine
	// i.e. DV for sparse/hybrid, MTBDD for mtbdd

	protected StateProbs computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b2, JDDNode nonabs, double time,
			StateProbs multProbs) throws PrismException
	{
		JDDNode multProbsMTBDD, probsMTBDD;
		DoubleVector multProbsDV, probsDV;
		StateProbs probs = null;

		// if nonabs is empty and multProbs was null, we don't need to do any
		// further solution
		// likewise if time = 0 (and in this case we know multProbs will be null
		// anyway
		// because U[0,0] is treated as a special case)
		if ((nonabs.equals(JDD.ZERO) && multProbs == null) || (time == 0)) {
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(b2);
				probs = new StateProbsMTBDD(b2, model);
				break;
			case Prism.SPARSE:
			case Prism.HYBRID:
				probs = new StateProbsDV(b2, model);
				break;
			}
		}
		// otherwise explicitly compute the probabilities
		else {
			// compute probabilities
			try {
				switch (engine) {
				case Prism.MTBDD:
					multProbsMTBDD = (multProbs == null) ? null : ((StateProbsMTBDD) multProbs).getJDDNode();
					probsMTBDD = PrismMTBDD.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time,
							multProbsMTBDD);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					multProbsDV = (multProbs == null) ? null : ((StateProbsDV) multProbs).getDoubleVector();
					probsDV = PrismSparse.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time,
							multProbsDV);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					multProbsDV = (multProbs == null) ? null : ((StateProbsDV) multProbs).getDoubleVector();
					probsDV = PrismHybrid.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time,
							multProbsDV);
					probs = new StateProbsDV(probsDV, model);
					break;
				}
			} catch (PrismException e) {
				throw e;
			}
		}

		return probs;
	}

	// compute probabilities for until (general case)

	protected StateProbs computeUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2) throws PrismException
	{
		JDDNode diags, emb;
		StateProbs probs = null;

		// Compute embedded Markov chain
		JDD.Ref(tr);
		diags = JDD.SumAbstract(tr, allDDColVars);
		JDD.Ref(tr);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		mainLog.println("\nDiagonals vector: " + JDD.GetInfoString(diags, allDDRowVars.n()));
		mainLog.println("Embedded Markov chain: " + JDD.GetInfoString(emb, allDDRowVars.n() * 2));

		// And then use superclass (ProbModelChecker)
		// to compute probabilities
		probs = super.computeUntilProbs(emb, tr01, b1, b2);

		// derefs
		JDD.Deref(diags);
		JDD.Deref(emb);

		return probs;
	}

	// compute cumulative rewards

	protected StateProbs computeCumulRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, double time)
			throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;

		// compute rewards
		try {
			switch (engine) {
			case Prism.MTBDD:
				rewardsMTBDD = PrismMTBDD.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsMTBDD(rewardsMTBDD, model);
				break;
			case Prism.SPARSE:
				rewardsDV = PrismSparse.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				rewardsDV = PrismHybrid.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
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

	// compute rewards for reach reward

	protected StateProbs computeReachRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b)
			throws PrismException
	{
		JDDNode diags, emb;
		StateProbs rewards = null;

		// Compute embedded Markov chain
		JDD.Ref(tr);
		diags = JDD.SumAbstract(tr, allDDColVars);
		JDD.Ref(tr);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		mainLog.println("\nDiagonals vector: " + JDD.GetInfoString(diags, allDDRowVars.n()));
		mainLog.println("Embedded Markov chain: " + JDD.GetInfoString(emb, allDDRowVars.n() * 2));

		// And then use superclass (ProbModelChecker)
		// to compute probabilities
		rewards = super.computeReachRewards(emb, tr01, sr, trr, b);

		// derefs
		JDD.Deref(diags);
		JDD.Deref(emb);

		return rewards;
	}

	// compute transient probabilities

	protected StateProbs computeTransientProbs(JDDNode tr, JDDNode init, double time) throws PrismException
	{
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;

		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.StochTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.StochTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.StochTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
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
