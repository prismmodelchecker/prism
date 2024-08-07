//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package symbolic.model;

import jdd.JDDNode;
import prism.PrismException;
import symbolic.states.StateValues;

/**
 * Interface for a model transformation.
 */
public interface ModelTransformation<OriginalModel extends Model, TransformedModel extends Model> {

	/** Get the original model. */
	public OriginalModel getOriginalModel();

	/** Get the transformed model. */
	public TransformedModel getTransformedModel();

	/** Clear the transformed model and all other intermediate BDD information.
	 *  Should be called when the transformation is not needed anymore. Does not clear the
	 *  original model.
	 */
	public void clear();

	/**
	 * Take a {@code StateValues} object for the transformed model and
	 * project the values to the original model.
	 * <br>
	 * The {@code svTransformedModel} argument is consumed/cleared and should not be
	 * used afterwards.
	 * @param svTransformedModel a {@code StateValues} object for the transformed model
	 * @return a corresponding {@code StateValues} object for the original model.
	 */
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException;

	/**
	 * Get the transformed state set of the states of interest,
	 * i.e., the set of states that should be calculated to allow a successful
	 * application of the projectToOriginalModel method.
	 *
	 * <br>[REF: <i>result</i>]
	 */
	public JDDNode getTransformedStatesOfInterest();
}
