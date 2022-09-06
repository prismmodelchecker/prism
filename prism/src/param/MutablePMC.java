//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Representation of mutable parametric Markov chain.
 * This class is intended to be used in combination with the
 * {@code StateEliminator}, which uses this class to compute values of
 * parametric Markov models.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see StateEliminator
 */
final class MutablePMC {
	/** function factory to which functions in this object belong */
	private FunctionFactory functionFactory;
	/** assignment of rewards to each state */
	private Function[] rewards;
	/** assignment of time to each state */
	private Function[] times;
	/** for each state, provides list of leaving transition probabilities */
	ArrayList<LinkedList<Function>> transitionProbs;
	/** for each state, provides list of leaving transition targets */
	ArrayList<LinkedList<Integer>> transitionTargets;
	/** for each state, provides list of states which have transitions to this state */
	ArrayList<LinkedList<Integer>> incoming;
	/** true iff uses a reward structure */
	private boolean useRewards;
	/** true iff uses time reward structure */
	private boolean useTime;
	/** initial states */
	private BitSet initStates;
	/** target states */
	private BitSet targetStates;
	/** total number of states */
	private int numStates;

	/**
	 * Constructs a new mutable parametric Markov chain.
	 * 
	 * @param functionFactory function factory used to maintain rational functions
	 * @param numStates total number of states this parametric Markov chain shall have
	 * @param useRewards true iff parametric Markov chain constructed shall use rewards
	 * @param useTime true iff parametric Markov chain constructed needs time entry
	 */
	MutablePMC(FunctionFactory functionFactory, int numStates, boolean useRewards, boolean useTime) {
		this.numStates = numStates;
		this.functionFactory = functionFactory;
		transitionProbs = new ArrayList<LinkedList<Function>>(numStates);
		transitionTargets = new ArrayList<LinkedList<Integer>>(numStates);
		incoming = new ArrayList<LinkedList<Integer>>(numStates);		
		for (int state = 0; state < numStates; state++) {
			transitionTargets.add(new LinkedList<Integer>());
			transitionProbs.add(new LinkedList<Function>());
			incoming.add(new LinkedList<Integer>());
		}
		this.useRewards = useRewards;
		this.useTime = useTime;
		initStates = new BitSet(numStates);
		targetStates = new BitSet(numStates);
		if (useRewards) {
			rewards = new Function[numStates];
			for (int i = 0; i < numStates; i++) {
				rewards[i] = functionFactory.getZero();
			}
		}
		if (useTime) {
			times = new Function[numStates];
			for (int i = 0; i < numStates; i++) {
				times[i] = functionFactory.getZero();
			}
		}
	}
	
	/**
	 * Returns function factory maintaining functions used in this object.
	 * 
	 * @return function factory maintaining functions
	 */
	FunctionFactory getFunctionFactory()
	{
		return functionFactory;
	}
	
	/**
	 * Adds a probabilistic transition.
	 * 
	 * @param from state transition starts from
	 * @param to state transition leads to
	 * @param prob probability of transition
	 */
	void addTransition(int from, int to, Function prob)
	{
		ListIterator<Integer> toIter = transitionTargets.get(from).listIterator();
		ListIterator<Function> valIter = transitionProbs.get(from).listIterator();
		boolean alreadyThere = false;
		while (toIter.hasNext() && !alreadyThere) {
			int succ = toIter.next();
			Function succProb = valIter.next();
			if (succ == to) {
				valIter.set(succProb.add(prob));
				alreadyThere = true;
			}
		}
		if (!alreadyThere) {
			transitionTargets.get(from).add(to);
			transitionProbs.get(from).add(prob);
			incoming.get(to).add(from);
		}
	}
	
	/**
	 * Returns the probability of a given transition
	 * 
	 * @param from source state of transition
	 * @param to target state of transition
	 * @return probability to move from given state to given state
	 */
	Function getTransProb(int from, int to)
	{
		Function prob = null;
		ListIterator<Integer> toIter = transitionTargets.get(from).listIterator();
		ListIterator<Function> valIter = transitionProbs.get(from).listIterator();		
		while (toIter.hasNext()) {
			int succ = toIter.next();
			Function succProb = valIter.next();
			if (succ == to) {
				prob = succProb;
				break;
			}
		}
		
		if (prob == null) {
			prob = functionFactory.getZero();
		}
		
		return prob;
	}
	
	/**
	 * Returns probability of the self-loop in a given state.
	 * 
	 * @param state state to return self-loop probability of
	 * @return self-loop probability of given state
	 */
	Function getSelfLoopProb(int state)
	{
		Function loopProb = null;
		
		ListIterator<Integer> toIter = transitionTargets.get(state).listIterator();
		ListIterator<Function> valIter = transitionProbs.get(state).listIterator();		
		while (toIter.hasNext()) {
			int to = toIter.next();
			Function val = valIter.next();
			if (to == state) {
				loopProb = val;
				break;
			}
		}
		if (loopProb == null) {
			loopProb = functionFactory.getZero();
		}
		
		return loopProb;
	}
	
	/**
	 * Makes a given state absorbing.
	 * This means removing all leaving transitions and adding a self-loop
	 * with probability one.
	 * 
	 * @param state state to make absorbing
	 */
	void makeAbsorbing(int state)
	{
		LinkedList<Integer> loop = new LinkedList<Integer>();
		loop.add(state);
		LinkedList<Function> one = new LinkedList<Function>();
		one.add(functionFactory.getOne());
		transitionTargets.set(state, loop);
		transitionProbs.set(state, one);
	}

	/**
	 * Sets whether given state shall be an initial state
	 * 
	 * @param state state which shall or shall not be an initial state
	 * @param targetState true iff given state shall be an initial state
	 */
	void setInitState(int state, boolean targetState)
	{
		initStates.set(state, targetState);
	}

	/**
	 * Sets whether given state shall be a target state
	 * 
	 * @param state state which shall or shall not be a target state
	 * @param targetState true iff given state shall be a target state
	 */
	void setTargetState(int state, boolean targetState)
	{
		targetStates.set(state, targetState);
	}
	
	/**
	 * Set reward of a given state.
	 * 
	 * @param state state to set reward of
	 * @param reward reward to set for given state
	 */
	void setReward(int state, Function reward)
	{
		rewards[state] = reward;
	}

	/**
	 * Set time of a given state.
	 * 
	 * @param state state to set reward of
	 * @param time time to set for given state
	 */
	void setTime(int state, Function time)
	{
		times[state] = time;
	}

	/**
	 * Get time of a given state.
	 * 
	 * @param state state to get time of
	 * @return time of given state
	 */
	Function getTime(int state)
	{
		return times[state];
	}
	
	/**
	 * Checks whether given state is a target state.
	 * 
	 * @param state state to check whether it is a target state
	 * @return true iff given state is a target state
	 */
	boolean isTargetState(int state)
	{
		return targetStates.get(state);
	}

	/**
	 * Checks whether there are some target states
	 * 
	 * @return true iff there are some target states
	 */
	boolean hasTargetStates()
	{
		return targetStates.cardinality() != 0;
	}
	
	/**
	 * Checks whether given state is an initial state.
	 * 
	 * @param state state to check whether it is an initial state
	 * @return true iff given state is an initial state
	 */
	boolean isInitState(int state)
	{
		return initStates.get(state);
	}

	/**
	 * Gets the reward of a given state.
	 * 
	 * @param state state to get reward of
	 * @return reward of given state
	 */
	Function getReward(int state)
	{
		return rewards[state];
	}
	
	/**
	 * Returns number of states.
	 * 
	 * @return number of states
	 */
	int getNumStates()
	{
		return numStates;
	}
	
	/**
	 * Returns whether pmc uses rewards.
	 * 
	 * @return true iff rewards are used
	 */
	boolean isUseRewards()
	{
		return useRewards;
	}
	
	/**
	 * Returns whether pmc uses time.
	 * 
	 * @return true iff uses time
	 */
	boolean isUseTime()
	{
		return useTime;
	}
	
	/*
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		
		for (int state = 0; state < numStates; state++) {
			result.append("state " + state);
			if (useRewards) {
				result.append(" " + rewards[state]);
			}
			if (useTime) {
				result.append(" " + times[state]);
			}
			result.append("\n");
		}
		
		return result.toString();
	}
	*/
}
