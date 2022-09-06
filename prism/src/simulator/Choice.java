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

package simulator;

import parser.*;
import prism.*;

public interface Choice
{
	/**
	 * Scale probability/rate of all transitions, multiplying by d.
	 */
	public void scaleProbabilitiesBy(double d);
	
	/**
	 * Get the module/action for this choice, as an integer index
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public int getModuleOrActionIndex();
	
	/**
	 * Get the module/action for this choice, as a string
	 * (form is "module" or "[action]")
	 */
	public String getModuleOrAction();
	
	/**
	 * Get the number of transitions in this choice.
	 */
	public int size();
	
	/**
	 * Get the updates of the ith transition, as a string.
	 * This is in abbreviated form, i.e. x'=1, rather than x'=x+1.
	 * Format is: x'=1, y'=0, with empty string for empty update.
	 * Only variables updated are included in list (even if unchanged).
	 */
	public String getUpdateString(int i, State currentState) throws PrismLangException;
	
	/**
	 * Get the updates of the ith transition, as a string.
	 * This is in full, i.e. of the form x'=x+1, rather than x'=1.
	 * Format is: (x'=x+1) & (y'=y-1), with empty string for empty update.
	 * Only variables updated are included in list.
	 * Note that expressions may have been simplified from original model. 
	 */
	public String getUpdateStringFull(int i);
	
	/**
	 * Compute the target for the ith transition, based on a current state,
	 * returning the result as a new State object copied from the existing one.
	 */
	public State computeTarget(int i, State currentState) throws PrismLangException;
	
	/**
	 * Compute the target for the ith transition, based on a current state.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. currentState and newState should be equal when passed in.) 
	 */
	public void computeTarget(int i, State currentState, State newState) throws PrismLangException;
	
	/**
	 * Get the probability/rate for the ith transition.
	 */
	public double getProbability(int i);
	
	/**
	 * Get the sum of probabilities/rates for all transitions.
	 */
	public double getProbabilitySum();
	
	/**
	 * Return the index of a transition according to a probability (or rate) sum, x.
	 * i.e. return the index of the first transition in this choice for which the
	 * sum of probabilities/rates for that and all prior transitions exceeds x.
	 */
	public int getIndexByProbabilitySum(double x);
	
	public void checkValid(ModelType modelType) throws PrismException;
	
	/**
	 * Check whether the transitions in this choice (from a particular state)
	 * would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public void checkForErrors(State currentState, VarList varList) throws PrismException;
}
