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
 * Interface for classes that provide (read) access to an explicit-state CTMDP.
 */
public interface CTMDP extends MDP
{
	// Accessors (for Model) - default implementations
	
	@Override
	default ModelType getModelType()
	{
		return ModelType.CTMDP;
	}

	// Accessors
	
	// TODO: copy/modify functions from CTMC
	
	/**
	 * Compute the maximum exit rate.
	 */
	public double getMaxExitRate();
	
	/**
	 * Check if the CTMDP is locally uniform, i.e. each state has the same exit rate for all actions. 
	 */
	public boolean isLocallyUniform();
	
	/**
	 * Build the discretised (DT)MDP for this CTMDP, in implicit form
	 * (i.e. where the details are computed on the fly from this one).
	 * @param tau Step duration
	 */
	public MDP buildImplicitDiscretisedMDP(double tau);

	/**
	 * Build (a new) discretised (DT)MDP for this CTMDP.
	 * @param tau Step duration
	 */
	public MDPSimple buildDiscretisedMDP(double tau);
}
