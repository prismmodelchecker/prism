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

import prism.PrismException;
import simulator.sampler.Sampler;
import cern.jet.stat.Probability;

/**
 * SimulationMethod class for the CI ("confidence interval") approach.
 * Case where 'iterations' (number of samples) is unknown parameter.
 */
public final class CIiterations extends CIMethod
{
	// For reward properties, manually specified number of iterations
	// after which to conclude whether we are in S^2=0 case or not
	private int reqIterToConclude;
	private boolean reqIterToConcludeGiven;
	// For reward properties, maximum value of reward allows
	// automatic detection of whether we are in S^2=0 case or not
	private double maxReward;
	// Final number of iterations of sampling 
	private int computedIterations;
	// Square of quantile
	private double squaredQuantile;

	// CONSTRUCTORS

	// probabilities, automatic
	public CIiterations(double confidenceLevel, double width)
	{
		this.confidence = confidenceLevel;
		this.width = width;
		reqIterToConclude = 0;
		reqIterToConcludeGiven = false;
		maxReward = 1.0;
		computedIterations = 0;
		squaredQuantile = 0.0;
	}

	// probabilities or rewards, manual
	public CIiterations(double confidenceLevel, double width, int reqIterToConclude)
	{
		this.confidence = confidenceLevel;
		this.width = width;
		this.reqIterToConclude = reqIterToConclude;
		reqIterToConcludeGiven = true;
		maxReward = 1.0;
		computedIterations = 0;
		squaredQuantile = 0.0;
	}

	// rewards, automatic
	public CIiterations(double confidenceLevel, double width, double maxReward)
	{
		this.confidence = confidenceLevel;
		this.width = width;
		this.maxReward = maxReward;
		reqIterToConclude = 0;
		reqIterToConcludeGiven = false;
		computedIterations = 0;
		squaredQuantile = 0.0;
	}

	@Override
	public void computeMissingParameterAfterSim()
	{
		// Store iters (computed earlier)
		numSamples = computedIterations;
		missingParameterComputed = true;
	}

	@Override
	public Object getMissingParameter() throws PrismException
	{
		if (!missingParameterComputed)
			throw new PrismException("Missing parameter not computed yet");
		return numSamples;
	}

	@Override
	public String getParametersString()
	{
		if (!missingParameterComputed)
			return "width=" + width + ", confidence=" + confidence + ", number of samples=unknown";
		else
			return "width=" + width + ", confidence=" + confidence + ", number of samples=" + numSamples;
	}

	@Override
	public boolean shouldStopNow(int iters, Sampler sampler)
	{
		double quantile = 0.0;

		// Need at least 2 iterations
		// (Student's t-distribution only defined for v > 0)
		// (and variance is always 0 for iters = 1)
		if (iters < 2)
			return false;

		// We cannot conclude yet whether it is a "S^2=0" case or if the estimator is still valid (i.e. std error > 0)
		if (sampler.getVariance() <= 0.0) {
			// automatic
			if (!reqIterToConcludeGiven && maxReward / width > iters)
				return false;
			// "manual"
			if (reqIterToConcludeGiven && reqIterToConclude > iters)
				return false;
		}

		// See if required number of iterations for the expected confidence is reached yet
		// (Note: Colt's studentTinverse seems to break for v=1 so do manually)
		if (iters - 1 > 1) {
			quantile = Probability.studentTInverse(confidence, iters - 1);
		} else {
			// PDF for v=1 is 1/2 + arctan(x)/pi
			// Want x for pdf = 1-conf/2 
			quantile = Math.tan((0.5 - confidence / 2) * Math.PI);
		}
		squaredQuantile = quantile * quantile;
		if (sampler.getVariance() > 0.0 && iters < sampler.getVariance() * squaredQuantile / (width * width))
			return false;

		// Store final number of iterations (to compute missing parameter later)
		computedIterations = iters;
		return true;
	}

	@Override
	public int getProgress(int iters, Sampler sampler)
	{
		// 2 iterations needed to compute variance of the sampler
		if (sampler.getVariance() <= 0.0 || iters < 2)
			return 0;
		return 10 * ((int) (100.0 * (double) (iters + 1) / (sampler.getVariance() * squaredQuantile / (width * width))) / 10);
	}
	
	@Override
	public SimulationMethod clone()
	{
		CIiterations m = new CIiterations(confidence, width);
		// Remaining CIMethod stuff
		m.numSamples = numSamples;
		m.missingParameterComputed = missingParameterComputed;
		m.prOp = prOp;
		m.theta = theta;
		// Local stuff
		m.reqIterToConclude = reqIterToConclude;
		m.reqIterToConcludeGiven = reqIterToConcludeGiven;
		m.maxReward = maxReward;
		m.computedIterations = computedIterations;
		m.squaredQuantile = squaredQuantile;
		return m;
	}
}
