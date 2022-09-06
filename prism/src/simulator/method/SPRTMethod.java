//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator.method;

import parser.ast.Expression;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.RelOp;
import prism.PrismException;
import simulator.sampler.Sampler;
import simulator.sampler.SamplerDouble;

/**
 * SimulationMethod class for the SPRT (Sequential probability ratio test) approach,
 * as used by Younes/Simmons (CAV'02).
 * See also Younes/Kwiatkowska/Norman/Parker (TACAS'04) and Vincent Nimal's MSc thesis.
 */
public final class SPRTMethod extends SimulationMethod
{
	// Hypothesis H0: probability/expectation >= theta + delta
	// Hypothesis H1: probability/expectation <= theta - delta

	// SPRT parameters
	// alpha: probability of type I error (false acceptance of H1)
	private double alpha;
	// beta: probability of type II error (false acceptance of H0)
	private double beta;
	// delta: half the width of the indifference region
	private double delta;

	// Probability/expectation for hypothesis 0/1 (used for likelihood ratio)
	private double p0;
	private double p1;
	// Decision: is H0 true?
	private boolean h0true;
	// Total number of iters (samples) needed
	private int computedIterations;
	// Is missing parameter (num iter) computed yet? 
	private boolean missingParameterComputed;

	/**
	 * Construct SimulationMethod for SPRT.
	 * @param alpha Probability of type I error
	 * @param beta Probability of type II error
	 * @param delta Indifference parameter
	 */
	public SPRTMethod(double alpha, double beta, double delta)
	{
		this.alpha = alpha;
		this.beta = beta;
		this.delta = delta;
		this.p0 = 0;
		this.p1 = 0;
		this.h0true = false;
		this.computedIterations = 0;
		this.missingParameterComputed = false;
	}

	@Override
	public String getName()
	{
		return "SPRT";
	}

	@Override
	public String getFullName()
	{
		return "Sequential Probability Ratio Test";
	}

	@Override
	public void reset()
	{
		this.h0true = false;
		this.missingParameterComputed = false;
	}

	@Override
	public void computeMissingParameterBeforeSim() throws PrismException
	{
		// Nothing to do (num iters computed on-the-fly)
	}

	@Override
	public void setExpression(Expression expr) throws PrismException
	{
		Expression bound;
		RelOp relOp;
		double theta;

		// For P properties...
		if (expr instanceof ExpressionProb) {
			bound = ((ExpressionProb) expr).getProb();
			relOp = ((ExpressionProb) expr).getRelOp();
		}
		// For R properties...
		else if (expr instanceof ExpressionReward) {
			bound = ((ExpressionReward) expr).getReward();
			relOp = ((ExpressionReward) expr).getRelOp();
		}
		// Other (error)
		else {
			throw new PrismException("Cannot approximate " + expr + " using simulation");
		}

		// There must be a probability/reward bound to use SPRT
		if (bound == null) {
			throw new PrismException("Cannot use SPRT on a quantitative (=?) property");
		} else {
			theta = bound.evaluateDouble();
			// Check of out-of-range errors
			if (theta - delta <= 0) {
				String s = "Indifference for SPRT method (" + delta + ") is too wide";
				s += " (bound " + theta + " is too close to 0)";
				throw new PrismException(s);
			}
			if (theta + delta > 1 && expr instanceof ExpressionProb) {
				String s = "Indifference for SPRT method (" + delta + ") is too wide";
				s += " (bound " + theta + " is too close to 1)";
				throw new PrismException(s);
			}
			// Set p0/p1 values for two hypotheses
			// Default case is that H0 means probability/reward >= theta + delta
			if (relOp.isLowerBound()) {
				p0 = theta + delta;
				p1 = theta - delta;
			}
			// If the bound is reversed, just swap p0/p1
			else {
				p0 = theta - delta;
				p1 = theta + delta;
			}
		}
	}

	@Override
	public void computeMissingParameterAfterSim()
	{
		// Nothing to do (this is done in shouldStopNow)
	}

	@Override
	public Object getMissingParameter() throws PrismException
	{
		if (!missingParameterComputed)
			throw new PrismException("Missing parameter not computed yet");
		return computedIterations;
	}

	@Override
	public String getParametersString()
	{
		if (!missingParameterComputed)
			return "type I error=" + alpha + ", type II error=" + beta + ", delta=" + delta + ", iterations=unknown";
		else
			return "type I error=" + alpha + ", type II error=" + beta + ", delta=" + delta + ", iterations=" + computedIterations;
	}

	@Override
	public boolean shouldStopNow(int iters, Sampler sampler)
	{
		// Too soon
		if (iters < 2)
			return false;
		// Get likelihood ratio; any problems, don't stop
		double likelihoodRatio;
		try {
			likelihoodRatio = sampler.getLikelihoodRatio(p1, p0);
		} catch (PrismException e) {
			return false;
		}
		if (sampler instanceof SamplerDouble && (likelihoodRatio <= 0.0 || Double.isInfinite(likelihoodRatio)))
			return false;

		// Accept H0
		if (likelihoodRatio <= beta / (1 - alpha)) {
			h0true = true;
			missingParameterComputed = true;
			computedIterations = iters;
			return true;
		}
		// Accept H1
		if (likelihoodRatio >= (1 - beta) / alpha) {
			h0true = false;
			missingParameterComputed = true;
			computedIterations = iters;
			return true;
		}
		// Not done yet
		return false;
	}

	@Override
	public int getProgress(int iters, Sampler sampler)
	{
		// No good measure of progress unfortunately
		return 0;
	}

	@Override
	public Object getResult(Sampler sampler) throws PrismException
	{
		// Is hypothesis H0 true?
		return new Boolean(h0true);
	}

	@Override
	public String getResultExplanation(Sampler sampler){
		return computedIterations + " samples needed to decide property " + h0true;
	}
	
	@Override
	public SimulationMethod clone()
	{
		SPRTMethod m = new SPRTMethod(alpha, beta, delta);
		m.p0 = p0;
		m.p1 = p1;
		m.h0true = h0true;
		m.computedIterations = computedIterations;
		m.missingParameterComputed = missingParameterComputed;
		return m;
	}
}
