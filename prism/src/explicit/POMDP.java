//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import explicit.rewards.MDPRewards;
import prism.ModelType;
import prism.PrismUtils;

/**
 * Interface for classes that provide (read) access to an explicit-state POMDP.
 */
public interface POMDP extends MDP, PartiallyObservableModel
{
	// Accessors (for Model) - default implementations
	
	@Override
	default ModelType getModelType()
	{
		return ModelType.POMDP;
	}

	@Override
	default String infoString()
	{
		String s = "";
		s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		s += ", " + getNumChoices() + " choices";
		s += ", dist max/avg = " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / getNumStates());
		s += ", " + getNumObservations() + " observables";
		s += ", " + getNumUnobservations() + " unobservables";
		return s;
	}

	@Override
	default String infoStringTable()
	{
		String s = "";
		s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
		s += "Obs/unobs:   " + getNumObservations() + "/" + getNumUnobservations() + "\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		s += "Choices:     " + getNumChoices() + "\n";
		s += "Max/avg:     " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / getNumStates()) + "\n";
		return s;
	}

	// Accessors
	
	/**
	 * Get initial belief state
	 */
	public Belief getInitialBelief();

	/**
	 * Get initial belief state as an distribution over all states (array).
	 */
	public double[] getInitialBeliefInDist();

	/**
	 * Get the updated belief after action {@code action}.
	 */
	public Belief getBeliefAfterAction(Belief belief, int action);

	/**
	 * Get the updated belief after action {@code action} using the distribution over all states belief representation.
	 */
	public double[] getBeliefInDistAfterAction(double[] belief, int action);

	/**
	 * Get the updated belief after action {@code action} and observation {@code observation}.
	 */
	public Belief getBeliefAfterActionAndObservation(Belief belief, int action, int observation);

	/**
	 * Get the updated belief after action {@code action} and observation {@code observation} using the distribution over all states belief representation.
	 */
	public double[] getBeliefInDistAfterActionAndObservation(double[] belief, int action, int observation);

	/**
	 * Get the probability of an observation {@code observation}} after action {@code action} from belief {@code belief}.
	 */
	public double getObservationProbAfterAction(Belief belief, int action, int observation);

	public double getObservationProbAfterAction(double[] belief, int action, int observation);

	/**
	 * Get the cost (reward) of an action {@code action}} from a belief {@code belief}.
	 */
	public double getCostAfterAction(Belief belief, int action, MDPRewards mdpRewards);

	public double getCostAfterAction(double[] belief, int action, MDPRewards mdpRewards);
}
