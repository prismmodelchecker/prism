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
	public abstract void update(Path path) throws PrismLangException;

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
	public Object getMeanValue()
	{
		return new Double(numTrue / (double) numSamples);
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
