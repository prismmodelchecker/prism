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

import java.io.*;

import jdd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.ast.*;
import parser.type.*;

/*
 * Model checker for CTMCs.
 * 
 * Much of StochModelChecker's functionality is inherited from the parent
 * class ProbModelChecker (for DTMCs). Main differences are:
 * 
 *  - Time-bounded operators have bounds that are doubles so the processing
 *    and computation for these is different. Methods:
 *    + checkProbBoundedUntil/checkRewardCumul/checkRewardInst
 *    + computeBoundedUntilProbs/computeCumulRewards
 *    
 *  - Likewise, transient probabilities are different. Methods:
 *    + doTransient/computeTransientProbs
 *    
 *  - In various cases, before we can reuse the numerical computation
 *    code from ProbModelChecker, we have to wrap the methods with code
 *    that first computes the embedded DTMC for the CTMC. This includes:
 *    + computeNextProbs/computeUntilProbs/computeReachRewards
 *      (for computeReachRewards we also modify the rewards)
 */
public class StochModelChecker extends ProbModelChecker
{
	// Constructor

	public StochModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		super(prism, m, pf);
	}

	// Override-able "Constructor"

	public ProbModelChecker createNewModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		return new StochModelChecker(prism, m, pf);
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	// bounded until
	@Override
	protected StateValues checkProbBoundedUntil(ExpressionTemporal expr, JDDNode statesOfInterest) throws PrismException
	{
		double lTime, uTime; // time bounds
		Expression exprTmp;
		JDDNode b1, b2, tmp;
		StateValues tmpProbs = null, probs = null;

		JDD.Deref(statesOfInterest);

		// get info from bounded until

		// lower bound is 0 if not specified
		// (i.e. if until is of form U<=t)
		exprTmp = expr.getLowerBound();
		if (exprTmp != null) {
			lTime = exprTmp.evaluateDouble(constantValues);
			if (lTime < 0) {
				throw new PrismException("Invalid lower bound " + lTime + " in time-bounded until formula");
			}
		} else {
			lTime = 0;
		}
		// upper bound is -1 if not specified
		// (i.e. if until is of form U>=t)
		exprTmp = expr.getUpperBound();
		if (exprTmp != null) {
			uTime = exprTmp.evaluateDouble(constantValues);
			if (uTime < 0 || (uTime == 0 && expr.upperBoundIsStrict())) {
				String bound = (expr.upperBoundIsStrict() ? "<" : "<=") + uTime;
				throw new PrismException("Invalid upper bound " + bound + " in time-bounded until formula");
			}
			if (uTime < lTime) {
				throw new PrismException("Upper bound must exceed lower bound in time-bounded until formula");
			}
		} else {
			uTime = -1;
		}

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

		// compute probabilities

		// a trivial case: "U<=0"
		if (lTime == 0 && uTime == 0) {
			// prob is 1 in b2 states, 0 otherwise
			JDD.Ref(b2);
			probs = new StateValuesMTBDD(b2, model);
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
				// set values to exactly 1 for target (b2) states
				// (these are computed inexactly during uniformisation)
				probs.maxMTBDD(b2);
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
	@Override
	protected StateValues checkRewardCumul(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, JDDNode statesOfInterest)
			throws PrismException
	{
		double time; // time
		StateValues rewards = null;

		JDD.Deref(statesOfInterest);

		// get info from inst reward
		time = expr.getUpperBound().evaluateDouble(constantValues);
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

	// inst reward

	@Override
	protected StateValues checkRewardInst(ExpressionTemporal expr, JDDNode stateRewards, JDDNode transRewards, JDDNode statesOfInterest)
			throws PrismException
	{
		double time; // time
		StateValues sr = null, rewards = null;

		JDD.Deref(statesOfInterest);

		// get info from inst reward
		time = expr.getUpperBound().evaluateDouble(constantValues);
		if (time < 0) {
			throw new PrismException("Invalid bound " + time + " in instantaneous reward property");
		}

		// compute rewards

		// a trivial case: "=0"
		if (time == 0) {
			JDD.Ref(stateRewards);
			rewards = new StateValuesMTBDD(stateRewards, model);
		} else {
			// convert state rewards vector to appropriate type (depending on
			// engine)
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(stateRewards);
				sr = new StateValuesMTBDD(stateRewards, model);
				break;
			case Prism.SPARSE:
				sr = new StateValuesDV(stateRewards, model);
				break;
			case Prism.HYBRID:
				sr = new StateValuesDV(stateRewards, model);
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

	
	@Override
	protected StateValues checkRewardCoSafeLTL(Expression expr, JDDNode stateRewards, JDDNode transRewards, JDDNode statesOfInterest) throws PrismException
	{
		// compute state sets for the maximal state formulas,
		// attach as fresh labels which replace the state formulas in
		// expr
		expr = handleMaximalStateFormulas(expr);

		// Compute embedded Markov chain, don't convert reward structures of the
		// model, as we use the rewards given as parameters
		ProbModel embeddedDTMC = ((StochModel)model).getEmbeddedDTMC(mainLog, false);

		JDDNode stateRewardsDTMC = null;
		try {
			// state rewards are scaled, nothing to do for transition rewards
			JDDNode diags = JDD.SumAbstract(trans.copy(), allDDColVars);
			stateRewardsDTMC = JDD.Apply(JDD.DIVIDE, stateRewards.copy(), diags);

			ProbModelChecker embeddedMC = (ProbModelChecker) createModelChecker(embeddedDTMC);
			StateValues sv = embeddedMC.checkRewardCoSafeLTL(expr, stateRewardsDTMC, transRewards, statesOfInterest);

			// update the model in the StateValues object back to the CTMC
			sv.switchModel(model);
			return sv;
		} finally {
			embeddedDTMC.clear();
			if (stateRewardsDTMC != null)
				JDD.Deref(stateRewardsDTMC);
		}
	}

	// -----------------------------------------------------------------------------------
	// do transient computation
	// -----------------------------------------------------------------------------------

	/**
	 * Compute transient probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(double time) throws PrismException
	{
		return doTransient(time, (StateValues) null);
	}
	
	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(double time, File initDistFile) throws PrismException
	{
		StateValues initDist = null;

		if (initDistFile != null) {
			mainLog.println("\nImporting initial probability distribution from file \"" + initDistFile + "\"...");
			// Build an empty vector of the appropriate type 
			if (engine == Prism.MTBDD) {
				initDist = new StateValuesMTBDD(JDD.Constant(0), model);
			} else {
				initDist = new StateValuesDV(new DoubleVector((int) model.getNumStates()), model);
			}
			// Populate vector from file
			initDist.readFromFile(initDistFile);
		}
		
		return doTransient(time, initDist);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over and
	 * then deleted afterwards, so if you wanted it, take a copy. 
	 */
	public StateValues doTransient(double time, StateValues initDist) throws PrismException
	{
		// mtbdd stuff
		JDDNode start, init;
		// other stuff
		StateValues initDistNew = null, probs = null;

		// build initial distribution (if not specified)
		if (initDist == null) {
			// first construct as MTBDD
			// get initial states of model
			start = model.getStart();
			// compute initial probability distribution (equiprobable over all start states)
			JDD.Ref(start);
			init = JDD.Apply(JDD.DIVIDE, start, JDD.Constant(JDD.GetNumMinterms(start, allDDRowVars.n())));
			// if using MTBDD engine, distribution needs to be an MTBDD
			if (engine == Prism.MTBDD) {
				initDistNew = new StateValuesMTBDD(init, model);
			}
			// for sparse/hybrid engines, distribution needs to be a double vector
			else {
				initDistNew = new StateValuesDV(init, model);
				JDD.Deref(init);
			}
		} else {
			initDistNew = initDist;
		}
		
		// compute transient probabilities
		probs = computeTransientProbs(trans, initDistNew, time);

		return probs;
	}

	// -----------------------------------------------------------------------------------
	// probability computation methods
	// -----------------------------------------------------------------------------------

	// compute probabilities for next
	@Override
	protected StateValues computeNextProbs(JDDNode tr, JDDNode b)
	{
		JDDNode diags, emb;
		StateValues probs = null;

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
	// nb: if not null, the type (StateValuesDV/MTBDD) of 'multProbs' must match
	// the current engine
	// i.e. DV for sparse/hybrid, MTBDD for mtbdd

	protected StateValues computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b2, JDDNode nonabs, double time,
			StateValues multProbs) throws PrismException
	{
		JDDNode multProbsMTBDD, probsMTBDD;
		DoubleVector multProbsDV, probsDV;
		StateValues probs = null;

		// if nonabs is empty and multProbs was null, we don't need to do any
		// further solution
		// likewise if time = 0 (and in this case we know multProbs will be null
		// anyway
		// because U[0,0] is treated as a special case)
		if ((nonabs.equals(JDD.ZERO) && multProbs == null) || (time == 0)) {
			switch (engine) {
			case Prism.MTBDD:
				JDD.Ref(b2);
				probs = new StateValuesMTBDD(b2, model);
				break;
			case Prism.SPARSE:
			case Prism.HYBRID:
				probs = new StateValuesDV(b2, model);
				break;
			}
		}
		// otherwise explicitly compute the probabilities
		else {
			// compute probabilities
			mainLog.println("\nComputing probabilities...");
			mainLog.println("Engine: " + Prism.getEngineString(engine));
			try {
				switch (engine) {
				case Prism.MTBDD:
					multProbsMTBDD = (multProbs == null) ? null : ((StateValuesMTBDD) multProbs).getJDDNode();
					probsMTBDD = PrismMTBDD.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time,
							multProbsMTBDD);
					probs = new StateValuesMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					multProbsDV = (multProbs == null) ? null : ((StateValuesDV) multProbs).getDoubleVector();
					probsDV = PrismSparse.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time,
							multProbsDV);
					probs = new StateValuesDV(probsDV, model);
					break;
				case Prism.HYBRID:
					multProbsDV = (multProbs == null) ? null : ((StateValuesDV) multProbs).getDoubleVector();
					probsDV = PrismHybrid.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time,
							multProbsDV);
					probs = new StateValuesDV(probsDV, model);
					break;
				}
			} catch (PrismException e) {
				throw e;
			}
		}

		return probs;
	}

	// compute probabilities for until (general case)
	@Override
	protected StateValues computeUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2) throws PrismException
	{
		JDDNode diags, emb;
		StateValues probs = null;

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

	protected StateValues computeCumulRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, double time)
			throws PrismException
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
				rewardsMTBDD = PrismMTBDD.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateValuesMTBDD(rewardsMTBDD, model);
				break;
			case Prism.SPARSE:
				rewardsDV = PrismSparse.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateValuesDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				rewardsDV = PrismHybrid.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
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

	@Override
	protected StateValues computeTotalRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr) throws PrismException
	{
		JDDNode diags, emb, srNew;
		StateValues rewards = null;

		// Compute embedded Markov chain
		JDD.Ref(tr);
		diags = JDD.SumAbstract(tr, allDDColVars);
		JDD.Ref(tr);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		mainLog.println("\nDiagonals vector: " + JDD.GetInfoString(diags, allDDRowVars.n()));
		mainLog.println("Embedded Markov chain: " + JDD.GetInfoString(emb, allDDRowVars.n() * 2));

		// Convert rewards
		JDD.Ref(sr);
		JDD.Ref(diags);
		srNew = JDD.Apply(JDD.DIVIDE, sr, diags);

		// And then use superclass (ProbModelChecker)
		// to compute rewards
		rewards = super.computeTotalRewards(emb, tr01, srNew, trr);

		// derefs
		JDD.Deref(diags);
		JDD.Deref(emb);
		JDD.Deref(srNew);

		return rewards;
	}
	
	// compute rewards for reach reward
	@Override
	protected StateValues computeReachRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b)
			throws PrismException
	{
		JDDNode diags, emb, srNew;
		StateValues rewards = null;

		// Compute embedded Markov chain
		JDD.Ref(tr);
		diags = JDD.SumAbstract(tr, allDDColVars);
		JDD.Ref(tr);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		mainLog.println("\nDiagonals vector: " + JDD.GetInfoString(diags, allDDRowVars.n()));
		mainLog.println("Embedded Markov chain: " + JDD.GetInfoString(emb, allDDRowVars.n() * 2));

		// Convert rewards
		JDD.Ref(sr);
		JDD.Ref(diags);
		srNew = JDD.Apply(JDD.DIVIDE, sr, diags);

		// And then use superclass (ProbModelChecker)
		// to compute rewards
		rewards = super.computeReachRewards(emb, tr01, srNew, trr, b);

		// derefs
		JDD.Deref(diags);
		JDD.Deref(emb);
		JDD.Deref(srNew);

		return rewards;
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Use the passed in vector initDist as the initial probability distribution (time 0).
	 * The type of this should match the current engine
	 * (i.e. StateValuesMTBDD for MTBDD, StateValuesDV for sparse/hybrid). 
	 * For reasons of efficiency, this vector will be trampled over and
	 * then deleted afterwards, so if you wanted it, take a copy. 
	 */
	protected StateValues computeTransientProbs(JDDNode tr, StateValues initDist, double time) throws PrismException
	{
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateValues probs = null;

		// special case: time = 0
		if (time == 0.0) {
			// we are allowed to keep the init vector, so no need to clone
			return initDist;
		}
		
		// general case
		mainLog.println("\nComputing probabilities...");
		mainLog.println("Engine: " + Prism.getEngineString(engine));
		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.StochTransient(tr, odd, ((StateValuesMTBDD) initDist).getJDDNode(), allDDRowVars, allDDColVars, time);
				probs = new StateValuesMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.StochTransient(tr, odd, ((StateValuesDV) initDist).getDoubleVector(), allDDRowVars, allDDColVars, time);
				probs = new StateValuesDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.StochTransient(tr, odd, ((StateValuesDV) initDist).getDoubleVector(), allDDRowVars, allDDColVars, time);
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
