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
import prism.PrismException;
import prism.PrismLangException;

/**
 * Samplers for properties that associate a simulation path with a real (double) value.
 */
public abstract class SamplerDouble extends Sampler
{
	// Value of current path
	protected double value;
	// Stats over all paths
	protected double valueSum;
	protected double valueSumSq;
	protected int numSamples;

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
		valueSumSq = 0.0;
		numSamples = 0;
	}

	@Override
	public abstract boolean update(Path path, TransitionList transList) throws PrismLangException;

	@Override
	public void updateStats()
	{
		valueSum += value;
		valueSumSq += value * value;
		numSamples++;
	}

	@Override
	public Object getCurrentValue()
	{
		return new Double(value);
	}

	@Override
	public double getMeanValue()
	{
		return valueSum / numSamples;
	}

	@Override
	public double getVariance()
	{
		// Estimator to the variance (see p.24 of Vincent Nimal's MSc thesis)
		if (numSamples <= 1) {
			return 0.0;
		} else {
			double mean = valueSum / numSamples;
			return (valueSumSq - numSamples * mean * mean) / (numSamples - 1.0);
		}
		
		// An alternative, below, would be to use the empirical mean
		// (this is not equivalent (or unbiased) but, asymptotically, is the same)
		//double mean = valueSum / numSamples;
		//return (valueSumSq / numSamples) - (mean * mean);
	}

	@Override
	public double getLikelihoodRatio(double p1, double p0) throws PrismException
	{
		// See Sec 6.3 of Vincent Nimal's MSc thesis for details
		// (in which mu1=p1 and mu0=p0)
		if (numSamples <= 1)
			return 0.0;
		if (valueSumSq == 0)
			throw new PrismException("Cannot compute likelihood ratio with null variance");
		// Compute maximum likelihood estimator of variance
		double MLE = valueSumSq / numSamples - (valueSum * valueSum) / numSamples / numSamples;
		double lr = (-1 / (2 * MLE)) * (numSamples * (p1 * p1 - p0 * p0) - 2 * valueSum * (p1 - p0));
		if (Double.isNaN(lr)) {
			throw new PrismException("Error computing likelihood ratio");
		}
		return Math.exp(lr);
	}
}
