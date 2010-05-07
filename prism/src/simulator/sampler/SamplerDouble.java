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

import simulator.*;
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
		numSamples = 0;
	}

	@Override
	public abstract boolean update(Path path) throws PrismLangException;

	@Override
	public void updateStats()
	{
		valueSum += value;
		numSamples++;
	}

	@Override
	public Object getCurrentValue()
	{
		return new Double(value);
	}

	@Override
	public Object getMeanValue()
	{
		return new Double(valueSum / numSamples);
	}
}
