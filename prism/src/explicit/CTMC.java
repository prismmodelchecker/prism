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

package explicit;

import java.util.BitSet;

import prism.ModelType;

/**
 * Interface for classes that provide (read) access to an explicit-state CTMC.
 */
public interface CTMC extends DTMC
{
	// Accessors (for Model) - default implementations
	
	@Override
	default ModelType getModelType()
	{
		return ModelType.CTMC;
	}

	// Accessors
	
	/**
	 * Get the exit rate for state {@code i}.
	 * i.e. sum_j R(i,j)
	 */
	public double getExitRate(int i);
	
	/**
	 * Compute the maximum exit rate.
	 * i.e. max_i { sum_j R(i,j) }
	 */
	public double getMaxExitRate();
	
	/**
	 * Compute the maximum exit rate over states in {@code subset}.
	 * i.e. max_{i in subset} { sum_j R(i,j) }
	 */
	public double getMaxExitRate(BitSet subset);
	
	/**
	 * Compute the default rate used to uniformise this CTMC. 
	 */
	public double getDefaultUniformisationRate();
	
	/**
	 * Compute the default rate used to uniformise this CTMC,
	 * assuming that all states *not* in {@code nonAbs} have been made absorbing.
	 */
	public double getDefaultUniformisationRate(BitSet nonAbs);
	
	/**
	 * Build the embedded DTMC for this CTMC, in implicit form
	 * (i.e. where the details are computed on the fly from this one).
	 */
	public DTMC buildImplicitEmbeddedDTMC();

	/**
	 * Get the embedded DTMC for this CTMC, in implicit form
	 * (i.e. where the details are computed on the fly from this one).
	 * <p>
	 * If there is no cached embedded DTMC, build it and cache it.
	 * Otherwise, return the cached one.
	 * <p>
	 * If the underlying CTMC has changed, build a fresh one using
	 * buildImplicitEmbeddedDTMC, which will update the cached embedded
	 * DTMC.
	 */
	public DTMC getImplicitEmbeddedDTMC();

	/**
	 * Build (a new) embedded DTMC for this CTMC.
	 */
	public DTMCSimple buildEmbeddedDTMC();

	/**
	 * Convert this CTMC into a uniformised CTMC.
	 * @param q Uniformisation rate
	 */
	public void uniformise(double q);

	/**
	 * Build the uniformised DTMC for this CTMC, in implicit form
	 * (i.e. where the details are computed on the fly from this one).
	 * @param q Uniformisation rate
	 */
	public DTMC buildImplicitUniformisedDTMC(double q);

	/**
	 * Build (a new) uniformised DTMC for this CTMC.
	 * @param q Uniformisation rate
	 */
	public DTMCSimple buildUniformisedDTMC(double q);

}
