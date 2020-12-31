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
import parser.Observation;
import parser.Unobservation;
import prism.PrismException;
import prism.PrismUtils;

/**
 * Simple explicit-state representation of a POMDP.
 * Basically a {@link MDPSimple} with observability info.
 */
public class POMDPSimple extends MDPSimple implements POMDP
{
	/** Information about the observations of this model,
	 * i.e. the Observation object corresponding to each observation index. */
	protected List<Observation> observationsList;

	/** Information about the unobservations of this model,
	 * i.e. the Unobservation object corresponding to each unobservation index. */
	protected List<Unobservation> unobservationsList;

	/** One state corresponding to each observation (used to look up info about it) */
	protected List<Integer> observationStates;
	
	/** Observable assigned to each state */
	protected List<Integer> observablesMap;

	/** Unobservable assigned to each state */
	protected List<Integer> unobservablesMap;

	// Constructors

	/**
	 * Constructor: empty POMDP.
	 */
	public POMDPSimple()
	{
		super();
		initialiseObservables();
	}

	/**
	 * Constructor: new POMDP with fixed number of states.
	 */
	public POMDPSimple(int numStates)
	{
		super(numStates);
		initialiseObservables();
	}

	/**
	 * Copy constructor.
	 */
	public POMDPSimple(POMDPSimple pomdp)
	{
		super(pomdp);
		observationsList = new ArrayList<>(pomdp.observationsList);
		unobservationsList = new ArrayList<>(pomdp.unobservationsList);
		observationStates = new ArrayList<>(pomdp.observationStates);
		observablesMap = new ArrayList<>(pomdp.observablesMap);
		unobservablesMap = new ArrayList<>(pomdp.unobservablesMap);
	}

	/**
	 * Construct a POMDP from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public POMDPSimple(POMDPSimple pomdp, int permut[])
	{
		super(pomdp, permut);
		observationsList = new ArrayList<>(pomdp.observationsList);
		unobservationsList = new ArrayList<>(pomdp.unobservationsList);
		int numObservations = pomdp.getNumObservations();
		observationStates = new ArrayList<>(numObservations);
		for (int o = 0; o < numObservations; o++) {
			int s = pomdp.observationStates.get(o);
			observationStates.add(s == -1 ? -1 : permut[s]);
		}
		observablesMap = new ArrayList<Integer>(getNumStates());
		unobservablesMap = new ArrayList<Integer>(getNumStates());
		for (int s = 0; s < numStates; s++) {
			observablesMap.add(-1);
			unobservablesMap.add(-1);
		}
		for (int s = 0; s < numStates; s++) {
			observablesMap.set(permut[s], pomdp.observablesMap.get(s));
			unobservablesMap.set(permut[s], pomdp.unobservablesMap.get(s));
		}
	}

	/**
	 * Construct a POMDP from an existing MDP.
	 */
	public POMDPSimple(MDPSimple mdp)
	{
		super(mdp);
		initialiseObservables(mdp.numStates);
		for (int s = 0; s < numStates; s++) {
			// Observation of a state is the state itself
			try {
				setObservation(s, s);
			} catch (PrismException e) {
				// Won't happen
			}
			// Unobservation of a state is null
			unobservablesMap.set(s, null);
		}
	}

	/**
	 * Initialise storage for observable info
	 */
	protected void initialiseObservables()
	{
		observationsList = new ArrayList<>();
		unobservationsList = new ArrayList<>();
		observationStates = new ArrayList<>();
		observablesMap = new ArrayList<>();
		unobservablesMap = new ArrayList<>();
	}

	/**
	 * Initialise storage for observable info when the model has {@code numStates} states
	 */
	protected void initialiseObservables(int numStates)
	{
		observationsList = new ArrayList<>();
		unobservationsList = new ArrayList<>();
		observationStates = new ArrayList<>();
		observablesMap = new ArrayList<>(numStates);
		unobservablesMap = new ArrayList<>(numStates);
		for (int i = 0; i < numStates; i++) {
			observablesMap.add(-1);
			unobservablesMap.add(-1);
		}
	}

	// Mutators (for ModelSimple)

	@Override
	public void clearState(int s)
	{
		super.clearState(s);
		observablesMap.set(s, -1);
		unobservablesMap.set(s, -1);
	}

	@Override
	public void addStates(int numToAdd)
	{
		super.addStates(numToAdd);
		for (int i = 0; i < numToAdd; i++) {
			observablesMap.add(-1);
			unobservablesMap.add(-1);
		}
	}

	// Mutators (other)
	
	/**
	 * Set the associated (read-only) observation list.
	 */
	public void setObservationsList(List<Observation> observationsList)
	{
		this.observationsList = observationsList;
	}

	/**
	 * Set the associated (read-only) observation list.
	 */
	public void setUnobservationsList(List<Unobservation> unobservationsList)
	{
		this.unobservationsList = unobservationsList;
	}

	/**
	 * Set the observation info for a state.
	 * If the actions for existing states with this observation do not match,
	 * an explanatory exception is thrown (so this should be done after transitions
	 * have been added to the state).
	 */
	public void setObservation(int s, Observation observ, Unobservation unobserv) throws PrismException
	{
		int oIndex = observationsList.indexOf(observ);
		if (oIndex == -1) {
			observationsList.add(observ);
			observationStates.add(-1);
			oIndex = observationsList.size() - 1;
		}
		try {
			setObservation(s, oIndex);
		} catch (PrismException e) {
			throw new PrismException("Problem with observation " + observ + ": " + e.getMessage());
		}
		int unobservIndex = unobservationsList.indexOf(unobserv);
		if (unobservIndex == -1) {
			unobservationsList.add(unobserv);
			unobservIndex = unobservationsList.size() - 1;
		}
		unobservablesMap.set(s, unobservIndex);
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
		// Set observation
		observablesMap.set(s, o);
		// If this is first state with this observation, store its index
		int observationState = observationStates.get(o);
		if (observationState == -1) {
			observationStates.set(o, s);
		}
		// Otherwise, check that the actions for existing states with
		// the same observation match this one
		else {
			// Get and sort action strings for existing state(s)
			List<String> observationStateActions = new ArrayList<>();
			int numChoices = getNumChoices(observationState);
			for (int i = 0; i < numChoices; i++) {
				Object action = getAction(observationState, i);
				observationStateActions.add(action == null ? "" : action.toString());
			}
	        Collections.sort(observationStateActions);
			// Get and sort action strings for the new state
			List<String> sActions = new ArrayList<>();
			numChoices = getNumChoices(s);
			for (int i = 0; i < numChoices; i++) {
				Object action = getAction(s, i);
				sActions.add(action == null ? "" : action.toString());
			}
	        Collections.sort(sActions);
	        // Check match
			if (!(observationStateActions.equals(sActions))) {
				throw new PrismException("Differing actions found in states: " + observationStateActions + " vs. " + sActions);
			}
		}
	}
	
	// Accessors (for PartiallyObservableModel)
	
	@Override
	public List<Observation> getObservationsList()
	{
		return observationsList;
	}

	@Override
	public List<Unobservation> getUnobservationsList()
	{
		return unobservationsList;
	}

	@Override
	public int getObservation(int s)
	{
		return observablesMap == null ? -1 : observablesMap.get(s);
	}

	@Override
	public int getUnobservation(int s)
	{
		return unobservablesMap.get(s);
	}

	@Override
	public int getNumChoicesForObservation(int o)
	{
		return getNumChoices(observationStates.get(o));
	}
	
	// Accessors (for POMDP)

	@Override
	public Belief getInitialBelief()
	{
		double[] initialBeliefInDist = new double[numStates];
		for (Integer i : initialStates) {
			initialBeliefInDist[i] = 1;
		}
		PrismUtils.normalise(initialBeliefInDist);
		return beliefInDistToBelief(initialBeliefInDist);
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
	public double getCostAfterAction(Belief belief, int action, MDPRewards mdpRewards)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double cost = getCostAfterAction(beliefInDist, action, mdpRewards);
		return cost;
	}

	@Override
	public double getCostAfterAction(double[] beliefInDist, int action, MDPRewards mdpRewards)
	{
		double cost = 0;
		for (int i = 0; i < beliefInDist.length; i++) {
			if (beliefInDist[i] == 0) {
				cost += 0;
			} else {
				cost += beliefInDist[i] * (mdpRewards.getTransitionReward(i, action) + mdpRewards.getStateReward(i));
			}

		}
		return cost;
	}

	@Override
	public Belief getBeliefAfterAction(Belief belief, int action)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double[] nextBeliefInDist = getBeliefInDistAfterAction(beliefInDist, action);
		return beliefInDistToBelief(nextBeliefInDist);
	}

	@Override
	public double[] getBeliefInDistAfterAction(double[] beliefInDist, int action)
	{
		int n = beliefInDist.length;
		double[] nextBeliefInDist = new double[n];
		for (int sp = 0; sp < n; sp++) {
			if (beliefInDist[sp] >= 1.0e-6) {
				Distribution distr = getChoice(sp, action);
				for (Map.Entry<Integer, Double> e : distr) {
					int s = (Integer) e.getKey();
					double prob = (Double) e.getValue();
					nextBeliefInDist[s] += beliefInDist[sp] * prob;
				}
			}
		}
		return nextBeliefInDist;
	}

	@Override // SLOW
	public double getObservationProbAfterAction(Belief belief, int action, int observation)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double prob = getObservationProbAfterAction(beliefInDist, action, observation);
		return prob;
	}

	@Override // SLOW
	public double getObservationProbAfterAction(double[] beliefInDist, int action, int observation)
	{
		double[] beliefAfterAction = this.getBeliefInDistAfterAction(beliefInDist, action);
		int s;
		double prob = 0;
		for (s = 0; s < beliefAfterAction.length; s++) {
			prob += beliefAfterAction[s] * getObservationProb(s, observation);
		}
		return prob;
	}

	public void computeObservationProbsAfterAction(double[] beliefInDist, int action, HashMap<Integer, Double> observation_probs)
	{
		double[] beliefAfterAction = this.getBeliefInDistAfterAction(beliefInDist, action);
		for (int s = 0; s < beliefAfterAction.length; s++) {
			int o = getObservation(s);
			double probToAdd = beliefAfterAction[s];
			if (probToAdd > 1e-6) {
				Double lookup = observation_probs.get(o);
				if (lookup == null)
					observation_probs.put(o, probToAdd);
				else
					observation_probs.put(o, lookup + probToAdd);
			}
		}
	}
	
	@Override
	public Belief getBeliefAfterActionAndObservation(Belief belief, int action, int observation)
	{
		double[] beliefInDist = belief.toDistributionOverStates(this);
		double[] nextBeliefInDist = getBeliefInDistAfterActionAndObservation(beliefInDist, action, observation);
		Belief nextBelief = beliefInDistToBelief(nextBeliefInDist);
		if (nextBelief.so != observation) {
			System.err.println(nextBelief.so + "<--" + observation
					+ " something wrong with POMDPSimple.getBeliefAfterActionAndObservation(Belief belief, int action, int observation)");
		}
		return nextBelief;
	}

	@Override
	public double[] getBeliefInDistAfterActionAndObservation(double[] beliefInDist, int action, int observation)
	{
		int n = beliefInDist.length;
		double[] nextBelief = new double[n];
		double[] beliefAfterAction = this.getBeliefInDistAfterAction(beliefInDist, action);
		int i;
		double prob;
		for (i = 0; i < n; i++) {
			prob = beliefAfterAction[i] * getObservationProb(i, observation);
			nextBelief[i] = prob;
		}
		PrismUtils.normalise(nextBelief);
		return nextBelief;
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
		POMDPSimple mdp = (POMDPSimple) o;
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
