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
 * Description of a transformation operation for a NondetModel (MDP).
 * The transformation can request the allocation of extra state and action variables,
 * and specify new transition matrix, initial states and reward information.
 * <br>
 * After use, the transformation should be cleared by calling {@code clear()}.
 */
public abstract class NondetModelTransformationOperator
{
	/** The original model */
	protected NondetModel originalModel;
	/** The extra state variables (rows) */
	protected JDDVars extraRowVars;
	/** The extra state variables (columns) */
	protected JDDVars extraColVars;
	/** The extra action (non-deterministic choice) variables */
	protected JDDVars extraActionVars;

	/**
	 * Constructor with the original model that is to be transformed.
	 * The original model is not changed and will not be cleared when
	 * {@code clear()} is called.
	 */
	public NondetModelTransformationOperator(NondetModel model)
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
		if (extraActionVars != null) {
			extraActionVars.derefAll();
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
	 * Return the name (prefix) to use for any new extra action variable.
	 * Default implementation: Return "tau".
	 */
	public String getExtraActionVariableName()
	{
		return "tau";
	}

	/**
	 * Get the number of extra state variables needed for this transformation.
	 * This is the number n of state variables and will lead to the allocation of
	 * 2*n variables, n row and n column variables (interleaved).
	 */
	public abstract int getExtraStateVariableCount();

	/**
	 * Get the number of needed extra action variables for this transformation.
	 * This will result in the allocation of n additional non-deterministic choice variables.
	 */
	public abstract int getExtraActionVariableCount();

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
	 * This method is called to notify the transformation about the action variables
	 * that were allocated.
	 * <br>
	 * The extraActionsVars are copies, i.e., this method is responsible
	 * to ensure that they will be derefed eventually.
	 * <br>
	 * Default implementation: store in extraActionVars field.
	 * <br>[ STORES: extraActionVars ]
	 */
	public void hookExtraActionVariableAllocation(JDDVars extraActionVars)
	{
		this.extraActionVars = extraActionVars;
	}

	/**
	 * Get the transformed transition function.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public abstract	JDDNode getTransformedTrans() throws PrismException;

	/**
	 * Get the transformed start function.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public abstract JDDNode getTransformedStart() throws PrismException;

	/**
	 * Get the transformed state reward relation, given the old reward relation.
	 * <br>
	 * Default implementation: Return the old reward relation, unchanged.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedStateReward(JDDNode oldReward) throws PrismException
	{
		return oldReward.copy();
	}

	/**
	 * Get the transformed transition reward relation, given the old reward relation.
	 * <br>
	 * Default implementation: Return the old reward relation, unchanged.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedTransReward(JDDNode oldReward) throws PrismException
	{
		return oldReward.copy();
	}

	/**
	 * Get the transformed trans actions relation,
	 * mapping (state,action) to action index.
	 * Action index 0 indicates unnamed action.
	 * <br>
	 * Default implementation: The old trans action relation is returned.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedTransActions() throws PrismException
	{
		if (originalModel.getTransActions() == null)
			return null;
		return originalModel.getTransActions().copy();
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
