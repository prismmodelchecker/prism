//==============================================================================
//	
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

import parser.State;
import prism.PrismException;

public interface ModelExplorer
{
	public State getDefaultInitialState() throws PrismException;
	
	public void queryState(State state) throws PrismException;
	
	public void queryState(State state, double time) throws PrismException;
	
	/**
	 * Returns the current number of available choices.
	 * @throws PrismException 
	 */
	public int getNumChoices() throws PrismException;

	/**
	 * Returns the current (total) number of available transitions.
	 * @throws PrismException 
	 */
	public int getNumTransitions() throws PrismException;
	
	/**
	 * Returns the current number of available transitions in choice i.
	 * @throws PrismException 
	 */
	public int getNumTransitions(int i) throws PrismException;

	/**
	 * Get the probability/rate of a transition within a choice, specified by its index/offset.
	 */
	public double getTransitionProbability(int i, int offset) throws PrismException;

	/**
	 * Get the action label of a transition as a string, specified by its index/offset.
	 * (null for asynchronous/independent transitions)
	 * (see also {@link #getTransitionModuleOrAction(int, int)} and {@link #getTransitionModuleOrActionIndex(int, int)})
	 * TODO: change return type to Object?
	 * @throws PrismException 
	 */
	public String getTransitionAction(int i, int offset) throws PrismException;

	/**
	 * Get the action label of a transition as a string, specified by its index.
	 * (null for asynchronous/independent transitions)
	 * (see also {@link #getTransitionModuleOrAction(int)} and {@link #getTransitionModuleOrActionIndex(int)})
	 * TODO: change return type to Object?
	 * @throws PrismException 
	 */
	public String getTransitionAction(int index) throws PrismException;

	/**
	 * Get the probability/rate of a transition, specified by its index.
	 */
	public double getTransitionProbability(int i) throws PrismException;
	
	/**
	 * Get the sum of probabilities/rates for transitions.
	 */
//	public double getTransitionProbabilitySum() throws PrismException;
	
	/**
	 * Get the target (as a new State object) of a transition within a choice, specified by its index/offset.
	 */
	public State computeTransitionTarget(int i, int offset) throws PrismException;
	
	/**
	 * Get the target of a transition (as a new State object), specified by its index.
	 */
	public State computeTransitionTarget(int i) throws PrismException;
}
