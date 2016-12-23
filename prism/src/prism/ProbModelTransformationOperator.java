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

package prism;

import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Description of a transformation operation for a ProbModel (DTMC/CTMC).
 * The transformation can request the allocation of extra state variables,
 * and specify new transition matrix, initial states and reward information.
 * <br>
 * After use, the transformation should be cleared by calling {@code clear()}.
 * <br>
 * Use ProbModel.getTransformed() to obtain a transformed model.
 */
public abstract class ProbModelTransformationOperator
{
	/** The original model */
	protected ProbModel originalModel;
	/** The extra state variables (rows) */
	protected JDDVars extraRowVars;
	/** The extra state variables (columns) */
	protected JDDVars extraColVars;

	/**
	 * Constructor with the original model that is to be transformed.
	 * The original model is not changed and will not be cleared when
	 * {@code clear()} is called.
 	 */
	public ProbModelTransformationOperator(ProbModel model)
	{
		originalModel = model;
	}

	/**
	 * Clear the transformation.
	 */
	public void clear()
	{
		if (extraRowVars != null) {
			extraRowVars.derefAll();
		}
		if (extraColVars != null) {
			extraColVars.derefAll();
		}
	}

	/**
	 * Return the name (prefix) to use for any new extra state variables.
	 * Default implementation: Return "extra".
	 */
	public String getExtraStateVariableName()
	{
		return "extra";
	}

	/**
	 * Get the number of extra state variables needed for this transformation.
	 * This is the number n of state variables and will lead to the allocation of
	 * 2*n variables, n row and n column variables (interleaved).
	 */
	public abstract int getExtraStateVariableCount();

	/**
	 * This method is called to notify the transformation operator
	 * about the state variables that were allocated.
	 * <br>
	 * The extraRowVars and extraColVars are copies, i.e., this method is responsible
	 * to ensure that they will be derefed eventually.
	 * <br>
	 * Default implementation: Store in extraRowVars and extraColVars fields.
	 * <br>[ STORES: extraRowVars, extraColVars ]
	 */
	public void hookExtraStateVariableAllocation(JDDVars extraRowVars, JDDVars extraColVars)
	{
		this.extraRowVars = extraRowVars;
		this.extraColVars = extraColVars;
	}

	/**
	 * Get the transformed transition function.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public abstract	JDDNode getTransformedTrans();

	/**
	 * Get the transformed start function.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public abstract JDDNode getTransformedStart();

	/**
	 * Get the transformed state reward relation, given the old reward relation.
	 * <br>
	 * Default implementation: Return the old reward relation, unchanged.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedStateReward(JDDNode oldReward)
	{
		return oldReward.copy();
	}

	/**
	 * Get the transformed transition reward relation, given the old reward relation.
	 * <br>
	 * Default implementation: Return the old reward relation, unchanged.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedTransReward(JDDNode oldReward)
	{
		return oldReward.copy();
	}

	/**
	 * Get the transformed (lifted) state set corresponding to a label in the original model.
	 * As a convenience, the reachable states in the transformed model are provided as well.
	 * <br>
	 * Default implementation: Return the old state set, i.e.,
	 * assume don't cares for the extra state variables.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedLabelStates(JDDNode oldLabelStates, JDDNode transformedReach)
	{
		return oldLabelStates.copy();
	}

	/**
	 * Directly provide the set of reachable states in the transformed model (optional).
	 * If the set of reachable states is not known, this function should return {@code null}
	 * and normal reachability computations will be performed.
	 * <br/>
	 * Default implementation: return {@code null}
	 * <br>[ REFS: <i>result</i> ]
	 */
	public JDDNode getReachableStates() throws PrismException {
		return null;
	}

	/**
	 * Provide a set of states in the transformed model that is known to be reachable,
	 * which is then used as a seed in the subsequent reachability computation (speed-up).
	 * <br>
	 * If this transformation operator does not want to provide this information,
	 * this function should return {@code null} and normal reachability computations
	 * will be performed (from the start states).
	 * <br/>
	 * Default implementation: return {@code null}
	 * <br>[ REFS: <i>result</i> ]
	 */
	public JDDNode getReachableStateSeed() throws PrismException {
		return null;
	}

	/**
	 * If true, this transformation may produce deadlock states and the normal
	 * deadlock check should not be invoked for the transformed model.
	 * <br>
	 * Default implementation: return {@code false}
	 */
	public boolean deadlocksAreFine()
	{
		return false;
	}

}
