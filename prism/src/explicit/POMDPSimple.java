//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import explicit.rewards.MDPRewards;
import parser.State;
import prism.PrismException;
import prism.PrismUtils;

/**
 * Simple explicit-state representation of a POMDP.
 * Basically a {@link MDPSimple} with observability info.
 * <br><br>
 * POMDPs require that states with the same observation have
 * the same set of available actions. This class further requires
 * that these actions appear in the same order (in terms of
 * choice indexing) in each equivalent state. This is enforced
 * when calling setObservation().
 */
public class POMDPSimple<Value> extends MDPSimple<Value> implements POMDP<Value>
{
	// Observations
	protected ObservationsSimple observations;

	// Constructors

	/**
	 * Constructor: empty POMDP.
	 */
	public POMDPSimple()
	{
		super();
		observations = new ObservationsSimple();
	}

	/**
	 * Constructor: new POMDP with fixed number of states.
	 */
	public POMDPSimple(int numStates)
	{
		super(numStates);
		observations = new ObservationsSimple(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public POMDPSimple(POMDPSimple<Value> pomdp)
	{
		super(pomdp);
		observations = new ObservationsSimple(pomdp.observations);
	}

	/**
	 * Construct a POMDP from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public POMDPSimple(POMDPSimple<Value> pomdp, int permut[])
	{
		super(pomdp, permut);
		observations = new ObservationsSimple(pomdp.observations, permut);
	}

	/**
	 * Construct a POMDP from an existing MDP.
	 */
	public POMDPSimple(MDPSimple<Value> mdp)
	{
		super(mdp);
		observations = new ObservationsSimple(mdp.numStates);
		observations.setIdentityObservations();
	}

	// Mutators (for ModelSimple)

	@Override
	public void clearState(int s)
	{
		super.clearState(s);
		observations.clearState(s);
	}

	@Override
	public void addStates(int numToAdd)
	{
		super.addStates(numToAdd);
		observations.addStates(numToAdd);
	}

	// Mutators (other)
	
	/**
	 * Set the associated (read-only) observation list.
	 */
	public void setObservationsList(List<State> observationsList)
	{
		observations.setObservationsList(observationsList);
	}

	/**
	 * Set the associated (read-only) unobservation list.
	 */
	public void setUnobservationsList(List<State> unobservationsList)
	{
		observations.setUnobservationsList(unobservationsList);
	}

	/**
	 * Set the observation info for a state.
	 * If the actions for existing states with this observation do not match,
	 * an explanatory exception is thrown (so this should be done after transitions
	 * have been added to the state). Optionally, a list of names of the
	 * observables can be passed for error reporting.
	 * @param s State
	 * @param observ Observation
	 * @param unobserv Unobservation
	 * @param observableNames Names of observables (optional)
	 */
	public void setObservation(int s, State observ, State unobserv, List<String> observableNames) throws PrismException
	{
		observations.setObservation(s, observ, unobserv, observableNames, this);
	}
	
	/**
	 * Assign observation with index o to state s.
	 * (assumes observation has already been added to the list)
	 * If the actions for existing states with this observation do not match,
	 * an explanatory exception is thrown (so this should be done after transitions
	 * have been added to the state).
	 */
	protected void setObservation(int s, int o) throws PrismException
	{
		observations.setObservation(s, o, this);
	}
	
	// Accessors (for PartiallyObservableModel)
	
	@Override
	public List<State> getObservationsList()
	{
		return observations.getObservationsList();
	}

	@Override
	public List<State> getUnobservationsList()
	{
		return observations.getUnobservationsList();
	}

	@Override
	public int getObservation(int s)
	{
		return observations.getObservation(s);
	}

	@Override
	public int getUnobservation(int s)
	{
		return observations.getUnobservation(s);
	}

	@Override
	public int getNumChoicesForObservation(int o)
	{
		return getNumChoices(observations.getObservationState(o));
	}
	
	// Accessors (for POMDP)

	@Override
	public Object getActionForObservation(int o, int i)
	{
		return getAction(observations.getObservationState(o), i);
	}
	
	@Override
	public Belief getInitialBelief()
	{
		double[] initialBeliefInDist = new double[numStates];
		for (Integer i : initialStates) {
			initialBeliefInDist[i] = 1;
		}
		PrismUtils.normalise(initialBeliefInDist);
		return new Belief(initialBeliefInDist, this);
	}

	@Override
	public double[] getInitialBeliefInDist()
	{
		double[] initialBeliefInDist = new double[numStates];
		for (Integer i : initialStates) {
			initialBeliefInDist[i] = 1;
		}
		PrismUtils.normalise(initialBeliefInDist);
		return initialBeliefInDist;
	}

	@Override
	public Belief getBeliefAfterChoice(Belief belief, int i)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double[] nextBeliefInDist = getBeliefInDistAfterChoice(beliefInDist, i);
		return new Belief(nextBeliefInDist, this);
	}

	@Override
	public double[] getBeliefInDistAfterChoice(double[] beliefInDist, int i)
	{
		int n = beliefInDist.length;
		double[] nextBeliefInDist = new double[n];
		for (int sp = 0; sp < n; sp++) {
			if (beliefInDist[sp] >= 1.0e-6) {
				@SuppressWarnings("unchecked")
				Distribution<Double> distr = (Distribution<Double>) getChoice(sp, i);
				for (Map.Entry<Integer, Double> e : distr) {
					int s = (Integer) e.getKey();
					double prob = (Double) e.getValue();
					nextBeliefInDist[s] += beliefInDist[sp] * prob;
				}
			}
		}
		return nextBeliefInDist;
	}

	@Override
	public Belief getBeliefAfterChoiceAndObservation(Belief belief, int i, int o)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double[] nextBeliefInDist = getBeliefInDistAfterChoiceAndObservation(beliefInDist, i, o);
		Belief nextBelief = new Belief(nextBeliefInDist, this);
		assert(nextBelief.so == o);
		return nextBelief;
	}

	@Override
	public double[] getBeliefInDistAfterChoiceAndObservation(double[] beliefInDist, int i, int o)
	{
		int n = beliefInDist.length;
		double[] nextBelief = new double[n];
		double[] beliefAfterAction = this.getBeliefInDistAfterChoice(beliefInDist, i);
		double prob;
		for (int s = 0; s < n; s++) {
			prob = beliefAfterAction[s] * getObservationProb(s, o);
			nextBelief[s] = prob;
		}
		PrismUtils.normalise(nextBelief);
		return nextBelief;
	}

	@Override // SLOW
	public double getObservationProbAfterChoice(Belief belief, int i, int o)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double prob = getObservationProbAfterChoice(beliefInDist, i, o);
		return prob;
	}

	@Override // SLOW
	public double getObservationProbAfterChoice(double[] beliefInDist, int i, int o)
	{
		double[] beliefAfterAction = this.getBeliefInDistAfterChoice(beliefInDist, i);
		int s;
		double prob = 0;
		for (s = 0; s < beliefAfterAction.length; s++) {
			prob += beliefAfterAction[s] * getObservationProb(s, o);
		}
		return prob;
	}

	@Override
	public HashMap<Integer, Double> computeObservationProbsAfterAction(double[] belief, int i)
	{
		HashMap<Integer, Double> probs = new HashMap<>();
		double[] beliefAfterAction = this.getBeliefInDistAfterChoice(belief, i);
		for (int s = 0; s < beliefAfterAction.length; s++) {
			int o = getObservation(s);
			double probToAdd = beliefAfterAction[s];
			if (probToAdd > 1e-6) {
				Double lookup = probs.get(o);
				if (lookup == null) {
					probs.put(o, probToAdd);
				} else {
					probs.put(o, lookup + probToAdd);
				}
			}
		}
		return probs;
	}
	
	@Override
	public double getRewardAfterChoice(Belief belief, int i, MDPRewards<Double> mdpRewards)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double cost = getRewardAfterChoice(beliefInDist, i, mdpRewards);
		return cost;
	}

	@Override
	public double getRewardAfterChoice(double[] beliefInDist, int i, MDPRewards<Double> mdpRewards)
	{
		double cost = 0;
		for (int s = 0; s < beliefInDist.length; s++) {
			if (beliefInDist[s] == 0) {
				cost += 0;
			} else {
				cost += beliefInDist[s] * (mdpRewards.getTransitionReward(s, i) + mdpRewards.getStateReward(s));
			}

		}
		return cost;
	}

	// Helpers
	
	protected Belief beliefInDistToBelief(double[] beliefInDist)
	{
		int so = -1;
		double[] bu = new double[this.getNumUnobservations()];
		for (int s = 0; s < beliefInDist.length; s++) {
			if (beliefInDist[s] != 0) {
				so = this.getObservation(s);
				bu[this.getUnobservation(s)] += beliefInDist[s];
			}
		}
		Belief belief = null;
		if (so != -1) {
			belief = new Belief(so, bu);
		} else {
			System.err.println("Something wrong in POMDPSimple.beliefInDistToBelief(double[] beliefInDist)");
		}
		return belief;
	}
	
	// Standard methods

	@Override
	public String toString()
	{
		int i, j, n;
		Object o;
		String s = "";
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (i > 0)
				s += ", ";
			s += i + "(" + getObservation(i) + "/" + getUnobservation(i) + "): ";
			s += "[";
			n = getNumChoices(i);
			for (j = 0; j < n; j++) {
				if (j > 0)
					s += ",";
				o = getAction(i, j);
				if (o != null)
					s += o + ":";
				s += trans.get(i).get(j);
			}
			s += "]";
		}
		s += " ]\n";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof POMDPSimple))
			return false;
		POMDPSimple<?> mdp = (POMDPSimple<?>) o;
		if (numStates != mdp.numStates)
			return false;
		if (!initialStates.equals(mdp.initialStates))
			return false;
		if (!trans.equals(mdp.trans))
			return false;
		// TODO: compare actions (complicated: null = null,null,null,...)
		return true;
	}
}
