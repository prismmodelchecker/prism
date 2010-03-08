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

import prism.ModelType;

/**
 * Interface for classes that provide (read-only) access to an explicit-state CTMC.
 */
public interface CTMC extends DTMC
{
	// Model type
	public static ModelType modelType = ModelType.CTMC;

	/**
	 * Compute the maximum exit rate (ignoring self-loops).
	 */
	public double getMaxExitRate();
	
	/**
	 * Compute the default rate used to uniformise this CTMC. 
	 */
	public double getDefaultUniformisationRate();
	
	/**
	 * Build the embedded DTMC for this CTMC, in implicit form
	 * (i.e. where the details are computed on the fly from this one).
	 */
	public DTMC buildImplicitEmbeddedDTMC();

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
