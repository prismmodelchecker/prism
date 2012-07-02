//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
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

package simulator.sampler;

import simulator.*;
import simulator.method.SimulationMethod;
import parser.ast.*;
import prism.PrismException;
import prism.PrismLangException;

/**
 * A Sampler determines values corresponding to a path property based on a sequence of simulation paths.
 * It determines the corresponding value for a single path, and also keeps track of the mean/variance
 * of this value over multiple traces. A Sampler can also be connected to a SimulationMethod object,
 * which is responsible for determining how many paths are required and how the final value is used.
 */
public abstract class Sampler
{
	protected boolean valueKnown;
	protected SimulationMethod simulationMethod;

	/**
	 * Is the current value of the sampler known, based on the path seen so far?
	 */
	public boolean isCurrentValueKnown()
	{
		return valueKnown;
	}

	/**
	 * Does this sampler only require a bounded number of path steps?
	 * i.e. is it safe to keep sampling beyond the maximum path length
	 * even if this sampler does not know its value yet?
	 * Conservatively, we assume "no"; override if required. 
	 */
	public boolean needsBoundedNumSteps()
	{
		return false;
	}

	/**
	 * Reset the current value of the sampler and whether it is known or not.
	 */
	public abstract void reset();

	/**
	 * Reset all statistics for the sampler.
	 */
	public abstract void resetStats();

	/**
	 * Update the current value of the sampler based on the current simulation path.
	 * It is assumed that this is called at every step of the path.
	 * We also need the transition list for the current (end) state of the path
	 * to check if there is a deadlock or self-loop.
	 * This returns true if the sampler's value becomes (or is already) known.
	 */
	public abstract boolean update(Path path, TransitionList transList) throws PrismLangException;

	/**
	 * Update the statistics for the sampler, assuming that the current path is finished.
	 */
	public abstract void updateStats();

	/**
	 * Get the current value of the sampler.
	 */
	public abstract Object getCurrentValue();

	/**
	 * Get the (estimated) mean value from the sampler, over all paths seen.
	 */
	public abstract double getMeanValue();

	/**
	 * Get the (estimated) variance from the sampler, over all paths seen.
	 */
	public abstract double getVariance();

	/**
	 * Get the ratio of the likelihoods for the distribution followed by the samples of this sampler
	 * with the given parameters for hypotheses H1 and H0).
	 * @param p1 Probability (or expectation) for hypothesis H1
	 * @param p0 Probability (or expectation) for hypothesis H0
	 */
	public abstract double getLikelihoodRatio(double p1, double p0) throws PrismException;

	/**
	 * Set the attached SimulationMethod object.
	 */
	public void setSimulationMethod(SimulationMethod simulationMethod)
	{
		this.simulationMethod = simulationMethod;
	}
	
	/**
	 * Get the attached SimulationMethod object.
	 */
	public SimulationMethod getSimulationMethod()
	{
		return simulationMethod;
	}
	
	/**
	 * Get an explanation of the result for the attached SimulationMethod object.
	 * @throws PrismException if we can't get a result for some reason.
	 */
	public String getSimulationMethodResultExplanation() throws PrismException
	{
		return simulationMethod.getResultExplanation(this);
	}
	
	// Static methods for sampler creation

	/**
	 * Create a sampler for an expression (P=? or R=?).
	 * Expression should contain no constants/formula/etc.
	 * The model to which the property applies should also be specified. 
	 */
	public static Sampler createSampler(Expression expr, ModulesFile mf) throws PrismException
	{
		Sampler sampler = null;
		// P=?
		if (expr instanceof ExpressionProb) {
			ExpressionProb propProb = (ExpressionProb) expr;
			// Test whether this is a simple path formula (i.e. non-LTL)
			if (propProb.getExpression().isSimplePathFormula()) {
				sampler = createSamplerForProbPathPropertySimple(propProb.getExpression(), mf);
			} else {
				throw new PrismException("LTL-style path formulas are not supported by the simulator");
			}
		}
		// R=?
		else if (expr instanceof ExpressionReward) {
			sampler = createSamplerForRewardProperty((ExpressionReward) expr, mf);
		}
		// Neither
		else {
			throw new PrismException("Can't create sampler for property \"" + expr + "\"");
		}
		return sampler;
	}

	private static SamplerBoolean createSamplerForProbPathPropertySimple(Expression expr, ModulesFile mf) throws PrismException
	{
		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				return createSamplerForProbPathPropertySimple(exprUnary.getOperand(), mf);
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Recurse, then negate meaning
				SamplerBoolean sampler = createSamplerForProbPathPropertySimple(exprUnary.getOperand(), mf);
				sampler.negate();
				return sampler;
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				return new SamplerNext(exprTemp);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					if (mf.getModelType().continuousTime()) {
						// Continuous-time bounded until
						return new SamplerBoundedUntilCont(exprTemp);
					} else {
						// Discrete-time bounded until
						return new SamplerBoundedUntilDisc(exprTemp);
					}
				} else {
					// Unbounded until
					return new SamplerUntil(exprTemp);
				}
			}
			// Anything else - convert to until and recurse
			else {
				return createSamplerForProbPathPropertySimple(exprTemp.convertToUntilForm(), mf);
			}
		}

		throw new PrismException("Can't create sampler for property \"" + expr + "\"");
	}

	private static SamplerDouble createSamplerForRewardProperty(ExpressionReward expr, ModulesFile mf) throws PrismException
	{
		// Extract reward structure index
		Object rs = expr.getRewardStructIndex();
		int rsi = -1;
		if (mf.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		if (rs == null) {
			rsi = 0;
		} else if (rs instanceof Expression) {
			rsi = ((Expression) rs).evaluateInt();
			rs = new Integer(rsi); // for better error reporting below
			rsi = (rsi < 1 || rsi > mf.getNumRewardStructs()) ? -1 : rsi - 1;
		} else if (rs instanceof String) {
			rsi = mf.getRewardStructIndex((String) rs);
		}
		if (rsi == -1)
			throw new PrismException("Invalid reward structure index \"" + rs + "\"");

		// Construct sampler based on type
		ExpressionTemporal exprTemp = (ExpressionTemporal) expr.getExpression();
		switch (exprTemp.getOperator()) {
		case ExpressionTemporal.R_C:
			if (mf.getModelType().continuousTime()) {
				// Continuous-time cumulative reward
				return new SamplerRewardCumulCont(exprTemp, rsi);
			} else {
				// Discrete-time cumulative reward
				return new SamplerRewardCumulDisc(exprTemp, rsi);
			}
		case ExpressionTemporal.R_I:
			if (mf.getModelType().continuousTime()) {
				// Continuous-time instantaneous reward
				return new SamplerRewardInstCont(exprTemp, rsi);
			} else {
				// Discrete-time instantaneous reward
				return new SamplerRewardInstDisc(exprTemp, rsi);
			}
		case ExpressionTemporal.R_F:
			// reachability reward
			return new SamplerRewardReach(exprTemp, rsi);
		}

		throw new PrismException("Can't create sampler for property \"" + expr + "\"");
	}
}
