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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import explicit.rewards.MDPRewards;
import prism.ModelType;
import prism.PrismLog;
import prism.PrismUtils;

/**
 * Interface for classes that provide (read) access to an explicit-state POMDP.
 * <br><br>
 * POMDPs require that states with the same observation have the same set of
 * available actions. Class implementing this interface must further ensure
 * that these actions appear in the same order (in terms of choice indexing)
 * in each observationally equivalent state.
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
	default void exportToPrismExplicitTra(PrismLog out)
	{
		// Output transitions to .tra file
		int numStates = getNumStates();
		out.print(numStates + " " + getNumChoices() + " " + getNumTransitions() + " " + getNumObservations() + "\n");
		TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
		for (int i = 0; i < numStates; i++) {
			int numChoices = getNumChoices(i);
			for (int j = 0; j < numChoices; j++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> e = iter.next();
					sorted.put(e.getKey(), e.getValue());
				}
				// Print out (sorted) transitions
				for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.print(i + " " + j + " " + e.getKey() + " " + PrismUtils.formatDouble(e.getValue()) + " " + getObservation(e.getKey()));
					Object action = getAction(i, j);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
				sorted.clear();
			}
		}
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
	 * Get the action label (if any) for choice {@code i} of observation {@code o}
	 * (this is the same for all states with this observation).
	 */
	public Object getActionForObservation(int o, int i);
	
	/**
	 * Get the initial belief state, as a {@link Belief} object.
	 */
	public Belief getInitialBelief();

	/**
	 * Get the initial belief state, as an array of probabilities over all states.
	 */
	public double[] getInitialBeliefInDist();

	/**
	 * Get the belief state (as a {@link Belief} object)
	 * after taking the {@code i}th choice from belief state {@code belief}.
	 */
	public Belief getBeliefAfterChoice(Belief belief, int i);

	/**
	 * Get the belief state (as an array of probabilities over all states)
	 * after taking the {@code i}th choice from belief state {@code belief}.
	 */
	public double[] getBeliefInDistAfterChoice(double[] belief, int i);

	/**
	 * Get the belief state (as a {@link Belief} object)
	 * after taking the {@code i}th choice from belief state {@code belief}
	 * and seeing observation {@code o} in the next state.
	 */
	public Belief getBeliefAfterChoiceAndObservation(Belief belief, int i, int o);

	/**
	 * Get the belief state (as an array of probabilities over all states)
	 * after taking the {@code i}th choice from belief state {@code belief}
	 * and seeing observation {@code o} in the next state.
	 */
	public double[] getBeliefInDistAfterChoiceAndObservation(double[] belief, int i, int o);

	/**
	 * Get the probability of seeing observation {@code o} after taking the
	 * {@code i}th choice from belief state {@code belief}.
	 */
	public double getObservationProbAfterChoice(Belief belief, int i, int o);

	/**
	 * Get the probability of seeing observation {@code o} after taking the
	 * {@code i}th choice from belief state {@code belief}.
	 * The belief state is given as an array of probabilities over all states.
	 */
	public double getObservationProbAfterChoice(double[] belief, int i, int o);

	/**
	 * Get the (non-zero) probabilities of seeing each observation after taking the
	 * {@code i}th choice from belief state {@code belief}.
	 * The belief state is given as an array of probabilities over all states.
	 */
	public HashMap<Integer, Double> computeObservationProbsAfterAction(double[] belief, int i);
	
	/**
	 * Get the expected (state and transition) reward value when taking the
	 * {@code i}th choice from belief state {@code belief}.
	 */
	public double getRewardAfterChoice(Belief belief, int i, MDPRewards mdpRewards);

	/**
	 * Get the expected (state and transition) reward value when taking the
	 * {@code i}th choice from belief state {@code belief}.
	 * The belief state is given as an array of probabilities over all states.
	 */
	public double getRewardAfterChoice(double[] belief, int i, MDPRewards mdpRewards);
}
