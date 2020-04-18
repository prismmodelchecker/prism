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
import prism.Accuracy;
import prism.PrismException;
import prism.Accuracy.AccuracyLevel;
import prism.Accuracy.AccuracyType;
import simulator.sampler.Sampler;

/**
 * SimulationMethod class(es) for the CI ("confidence interval") approach.
 */
public abstract class CIMethod extends SimulationMethod
{
	// Has the missing parameter been computed yet?
	protected boolean missingParameterComputed;

	// CI parameters:
	// Interval width
	protected double width;
	// Confidence level (alpha)
	protected double confidence;
	// Number of samples
	protected int numSamples;

	// Property info
	// Operator in P/R: 0=quantitative, -1=lower bound, 1=upper bound
	protected int prOp;
	// Probability/reward bound (if any)
	protected double theta;

	public CIMethod()
	{
		missingParameterComputed = false;
		width = 0.0;
		confidence = 0.0;
		numSamples = 0;
	}

	@Override
	public String getName()
	{
		return "CI";
	}

	@Override
	public String getFullName()
	{
		return "Confidence Interval";
	}

	@Override
	public void reset()
	{
		missingParameterComputed = false;
	}

	@Override
	public void computeMissingParameterBeforeSim() throws PrismException
	{
		// Nothing to do (always computed after simulation)
	}

	@Override
	public void setExpression(Expression expr) throws PrismException
	{
		Expression bound;
		RelOp relOp;

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

		// Process bound/relop
		if (bound == null) {
			prOp = 0;
			theta = -1.0; // junk
		} else {
			prOp = relOp.isLowerBound() ? -1 : 1;
			theta = bound.evaluateDouble();
		}
	}

	public abstract void computeMissingParameterAfterSim();

	public abstract Object getMissingParameter() throws PrismException;

	public abstract String getParametersString();

	public abstract boolean shouldStopNow(int iters, Sampler sampler);

	public abstract int getProgress(int iters, Sampler sampler);

	@Override
	public Object getResult(Sampler sampler) throws PrismException
	{
		double mean = sampler.getMeanValue();
		//double variance = sampler.getVariance();
		//double stddev = Math.sqrt(variance);
		switch (prOp) {
		case 0: // 0=quantitative
			return new Double(mean);
			//return new prism.Interval(mean - stddev, mean + stddev);
			//return new Double(stddev / mean); // noise
		case -1: // -1=lower bound
			if (mean >= theta + width)
				return new Boolean(true);
			else if (mean <= theta - width)
				return new Boolean(false);
			else
				throw new PrismException("Approximation is not precise enough to get a result");
		case 1: // 1=upper bound
			if (mean >= theta + width)
				return new Boolean(false);
			else if (mean <= theta - width)
				return new Boolean(true);
			else
				throw new PrismException("Approximation is not precise enough to get a result");
		default:
			throw new PrismException("Unknown property type");
		}
	}

	@Override
	public Accuracy getResultAccuracy(Sampler sampler) throws PrismException
	{
		Accuracy accuracy;
		// Quantitative
		if (prOp == 0) {
			accuracy = new Accuracy(AccuracyLevel.PROBABLY_BOUNDED, width, AccuracyType.ABSOLUTE);
		}
		// Bounded (accuracy error bound is meaningless)
		else {
			accuracy = new Accuracy(AccuracyLevel.PROBABLY_BOUNDED, 0.0, AccuracyType.ABSOLUTE);
		}
		accuracy.setProbability(1.0 - confidence);
		return accuracy;
	}

	@Override
	public String getResultExplanation(Sampler sampler) throws PrismException
	{
		return "confidence interval is " + sampler.getMeanValue() + " +/- " + width + ", based on " + (100.0 * (1.0 - confidence)) + "% confidence level";
	}
}
