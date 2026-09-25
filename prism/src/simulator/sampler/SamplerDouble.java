//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
//	* Marcin Copik <mcopik@gmail.com>
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

import prism.ModelGenerator;
import prism.PrismException;
import simulator.Path;

/**
 * Samplers for properties that associate a simulation path with a real (double) value.
 */
public abstract class SamplerDouble extends Sampler
{
	/** Value of current path */
	protected double value;
	
	// Stats over all paths
	
	/** Number of samples so far */
	protected int numSamples;
	/** Sum of values (used to compute mean) */
	protected double valueSum;
	/** Correction value used when tracking variance */
	protected double correctionTerm;
	/** Sum of values, each shifted by the correction term (see below) */
	protected double valueSumShifted;
	/** Sum of squares of values, each shifted by the correction term (see below) */
	protected double valueSumShiftedSq;
	/** Has any path had value +infinity? (if so, so is the mean, exactly) */
	protected boolean sampledInfinity;

	/**
	 * NB: In order to improve the numerical stability for the computation of variance,
	 * we stored a shifted version of the mean, using a correction term that is an
	 * estimate of the mean (for which, we just use the first value sampled). 
	 * Source: "Algorithms for Computing the Sample Variance: Analysis and Recommendations", T.F. Chan et al.
	 */
	
	@Override
	public void reset()
	{
		valueKnown = false;
		value = 0.0;
	}

	@Override
	public void resetStats()
	{
		valueSum = 0.0;
		valueSumShifted = 0.0;
		valueSumShiftedSq = 0.0;
		numSamples = 0;
		sampledInfinity = false;
	}

	@Override
	public abstract boolean update(Path path, ModelGenerator modelGen) throws PrismException;

	/**
	 * Check that the rewards for the most recent step of a path are non-negative,
	 * as required for properties that accumulate reward (instantaneous reward allows negative values).
	 */
	protected static void checkPreviousStepRewardsNonNegative(Path path, int rsi) throws PrismException
	{
		if (path.size() > 0) {
			double rew = Math.min(path.getPreviousStateReward(rsi), path.getPreviousTransitionReward(rsi));
			if (rew < 0) {
				throw new PrismException("Reward structure is negative (" + rew + ") at state " + path.getPreviousState());
			}
		}
	}

	@Override
	public void updateStats()
	{
		// An infinite value would make the sums below infinite/NaN; just record it
		if (value == Double.POSITIVE_INFINITY) {
			sampledInfinity = true;
			numSamples++;
			return;
		}
		if (numSamples == 0)
			correctionTerm = value;
		valueSum += value;
		valueSumShifted += value - correctionTerm;
		valueSumShiftedSq += Math.pow(value - correctionTerm, 2);
		numSamples++;
	}

	@Override
	public Object getCurrentValue()
	{
		return Double.valueOf(value);
	}

	@Override
	public double getMeanValue()
	{
		if (sampledInfinity) {
			return Double.POSITIVE_INFINITY;
		}
		return valueSum / numSamples;
	}

	@Override
	public double getVariance()
	{
		// Return estimator to the variance
		// (0 if an infinite value was sampled, since then the mean is known to be infinite)
		if (numSamples <= 1 || sampledInfinity) {
			return 0.0;
		} else {
			double meanShifted = valueSumShifted / numSamples;
			return (valueSumShiftedSq - numSamples * meanShifted * meanShifted) / (numSamples - 1.0);
		}
		
		// Note:
		// * As explained at the top of this class, variance is computed using
		//   a shifted version of the mean to improve numerical stability
		// * We divide by numSamples -1. An alternative would be to use the empirical mean, i.e. dividing by numSamples. 
		//   This is not equivalent (or unbiased) but, asymptotically, is the same.
		//   See p.24 of: Vincent Nimal, "Statistical Approaches for Probabilistic Model Checking", MSc Thesis
	}

	@Override
	public double getLikelihoodRatio(double p1, double p0) throws PrismException
	{
		// For details, see Sec 6.3 of: Vincent Nimal, "Statistical Approaches for Probabilistic Model Checking", MSc Thesis
		// (in which mu1=p1 and mu0=p0)
		if (numSamples <= 1)
			return 0.0;
		// An infinite mean is conclusive evidence for the larger of the two hypotheses
		// (use extreme but finite values, since SPRT treats 0/infinity as inconclusive)
		if (sampledInfinity)
			return p1 > p0 ? Double.MAX_VALUE : Double.MIN_VALUE;
		if (valueSumShiftedSq == 0)
			throw new PrismException("Cannot compute likelihood ratio with null variance");
		// Compute maximum likelihood estimator of variance
		double MLE = valueSumShiftedSq / numSamples - (valueSumShifted * valueSumShifted) / numSamples / numSamples;
		double lr = (-1 / (2 * MLE)) * (numSamples * (p1 * p1 - p0 * p0) - 2 * valueSum * (p1 - p0));
		if (Double.isNaN(lr)) {
			throw new PrismException("Error computing likelihood ratio");
		}
		return Math.exp(lr);
	}
}
