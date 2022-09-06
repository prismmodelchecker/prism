//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package simulator.sampler;

import prism.ModelGenerator;
import prism.PrismException;
import simulator.Path;

/**
 * Samplers for properties that associate a simulation path with a Boolean value.
 */
public abstract class SamplerBoolean extends Sampler
{
	// Value of current path
	protected boolean value;
	// Whether the actual value should be the negation of 'value'
	protected boolean negated = false;
	// Stats over all paths
	protected int numSamples;
	protected int numTrue;

	@Override
	public void reset()
	{
		valueKnown = false;
		value = false;
	}

	@Override
	public void resetStats()
	{
		numSamples = 0;
		numTrue = 0;
	}

	@Override
	public abstract boolean update(Path path, ModelGenerator modelGen) throws PrismException;

	@Override
	public void updateStats()
	{
		numSamples++;
		// XOR: value && !negated || !value && negated 
		if (value != negated)
			numTrue++;
	}

	@Override
	public Object getCurrentValue()
	{
		// XOR: value && !negated || !value && negated 
		return new Boolean(value != negated);
	}

	@Override
	public double getMeanValue()
	{
		return numTrue / (double) numSamples;
	}

	@Override
	public double getVariance()
	{
		// Estimator to the variance (see p.24 of Vincent Nimal's MSc thesis)
		if (numSamples <= 1) {
			return 0.0;
		} else {
			return (numTrue * ((double) numSamples - numTrue) / ( numSamples * (numSamples - 1.0))); 
		}
		
		// An alternative, below, would be to use the empirical mean
		// (this is not equivalent (or unbiased) but, asymptotically, is the same)
		//double mean = numTrue / (double) numSamples;
		//return mean * (1.0 - mean);
	}

	@Override
	public double getLikelihoodRatio(double p1, double p0) throws PrismException
	{
		// See Sec 5.3 of Vincent Nimal's MSc thesis for details
		return Math.pow(p1 / p0, numTrue) * Math.pow((1 - p1) / (1 - p0), numSamples - numTrue);
	}

	/**
	 * Negate the meaning of this sampler.
	 */
	public boolean negate()
	{
		return negated = !negated;
	}

	/**
	 * Is this sampler negated?
	 */
	public boolean getNegated()
	{
		return negated;
	}
}
