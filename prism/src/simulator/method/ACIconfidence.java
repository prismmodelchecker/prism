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
 * SimulationMethod class for the ACI ("asymptotic confidence interval") approach.
 * Case where 'confidence' is unknown parameter.
 */
public final class ACIconfidence extends CIMethod
{
	// Estimate of variance (from sampling)
	private double varEstimator;

	public ACIconfidence(double width, int iterations)
	{
		this.width = width;
		this.numSamples = iterations;
		varEstimator = 0.0;
	}

	@Override
	public String getName()
	{
		return "ACI";
	}

	@Override
	public String getFullName()
	{
		return "Asymptotic Confidence Interval";
	}

	@Override
	public void computeMissingParameterAfterSim()
	{
		confidence = 2 * (1.0 - Probability.normal(width * Math.sqrt(numSamples / varEstimator)));
		missingParameterComputed = true;
	}

	@Override
	public Object getMissingParameter() throws PrismException
	{
		if (!missingParameterComputed)
			throw new PrismException("Missing parameter not computed yet");
		return confidence;
	}
	
	@Override
	public String getParametersString()
	{
		if (!missingParameterComputed)
			return "width=" + width + ", confidence=" + "unknown" + ", number of samples=" + numSamples;
		else
			return "width=" + width + ", confidence=" + confidence + ", number of samples=" + numSamples;
	}

	@Override
	public boolean shouldStopNow(int iters, Sampler sampler)
	{
		if (iters >= numSamples) {
			// Store final variance for confidence computation later
			varEstimator = sampler.getVariance();
			return true;
		}
		return false;
	}

	@Override
	public int getProgress(int iters, Sampler sampler)
	{
		// Easy: percentage of iters done so far
		return ((10 * iters) / numSamples) * 10;
	}
	
	@Override
	public SimulationMethod clone()
	{
		ACIconfidence m = new ACIconfidence(width, numSamples);
		// Remaining CIMethod stuff
		m.confidence = confidence;
		m.missingParameterComputed = missingParameterComputed;
		m.prOp = prOp;
		m.theta = theta;
		// Local stuff
		m.varEstimator = varEstimator;
		return m;
	}
}
