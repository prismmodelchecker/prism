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

/**
 * SimulationMethod class for the APMC ("approximate probabilistic model checking")
 * approach of Herault/Lassaigne/Magniette/Peyronnet (VMCAI'04).
 * Case where 'confidence' is unknown parameter.
 */
public class APMCconfidence extends APMCMethod
{
	public APMCconfidence(double approximation, int iterations)
	{
		this.approximation = approximation;
		this.numSamples = iterations;
	}

	@Override
	public void computeMissingParameterBeforeSim() throws PrismException
	{
		if (approximation * approximation * numSamples < Math.log(2.0) / 2.0) {
			String msg = "For APMC, samples (N) and approximation (eps) must satisfy N*eps^2 >= ln(2)/2";
			msg += ". Increase samples and/or approximation";
			throw new PrismException(msg);
		}
		confidence = 2.0 / Math.exp((numSamples * approximation * approximation) * 2.0);
		missingParameterComputed = true;
	}

	@Override
	public Object getMissingParameter() throws PrismException
	{
		if (!missingParameterComputed)
			computeMissingParameterBeforeSim();
		return confidence;
	}

	@Override
	public String getParametersString()
	{
		if (!missingParameterComputed)
			return "approximation=" + approximation + ", confidence=" + "unknown" + ", number of samples=" + numSamples;
		else
			return "approximation=" + approximation + ", confidence=" + confidence + ", number of samples=" + numSamples;
	}
	
	@Override
	public SimulationMethod clone()
	{
		APMCconfidence m = new APMCconfidence(approximation, numSamples);
		m.confidence = confidence;
		m.missingParameterComputed = missingParameterComputed;
		m.prOp = prOp;
		m.theta = theta;
		return m;
	}
}
