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

import parser.EvaluateContext.EvalMode;
import parser.EvaluateContextFull;
import parser.State;
import parser.ast.Expression;
import parser.type.TypeBool;
import prism.ModelGenerator;
import prism.PrismLangException;

/**
 * Classes that store and manipulate a path though a model.
 */
public abstract class Path
{
	// MUTATORS
	
	/**
	 * Initialise the path with an initial state, observation and rewards.
	 * Note: State objects and array will be copied, not stored directly.
	 */
	public abstract void initialise(State initialState, State initialObs, double[] initialStateRewards);

	/**
	 * Add a step to the path.
	 * Note: State object and arrays will be copied, not stored directly.
	 */
	public abstract void addStep(int choice, Object action, String actionString, Object probability, double[] transRewards, State newState, State newObs, double[] newStateRewards, ModelGenerator modelGen);

	/**
	 * Add a timed step to the path.
	 * Note: State object and arrays will be copied, not stored directly.
	 */
	public abstract void addStep(double time, int choice, Object action, String actionString, Object probability, double[] transRewards, State newState, State newObs, double[] newStateRewards, ModelGenerator modelGen);

	/**
	 * Set the strategy info (mode and next decision) for the current state.
	 */
	public abstract void setStrategyInfoForCurrentState(int memory, Object decision);
	
	// ACCESSORS

	/**
	 * Check whether this path includes continuous-time information, e.g. delays for a CTMC.
	 */
	public abstract boolean continuousTime();

	/**
	 * Get the size of the path (number of steps; or number of states - 1).
	 */
	public abstract long size();

	/**
	 * Get the number of states in the path (0 if not initialised; otherwise size() + 1).
	 */
	public abstract long numStates();

	/**
	 * Get the previous state, i.e. the penultimate state of the current path.
	 */
	public abstract State getPreviousState();

	/**
	 * Get the current state, i.e. the current final state of the path.
	 */
	public abstract State getCurrentState();

	/**
	 * Get the observation for the previous state, i.e. for the penultimate state of the current path.
	 */
	public abstract State getPreviousObservation();

	/**
	 * Get the observation for the current state, i.e. for the current final state of the path.
	 */
	public abstract State getCurrentObservation();

	/**
	 * Get the action taken in the previous step.
	 */
	public abstract Object getPreviousAction();
	
	/**
	 * Get a string describing the action taken in the previous step.
	 */
	public abstract String getPreviousActionString();
	
	/**
	 * Get the probability or rate associated wuth the previous step.
	 */
	public abstract Object getPreviousProbability();
	
	/**
	 * Get the total time elapsed so far (where zero time has been spent in the current (final) state).
	 * For discrete-time models, this is just the number of steps (but returned as a double).
	 */
	public abstract double getTotalTime();
	
	/**
	 * For paths with continuous-time info, get the time spent in the previous state.
	 */
	public abstract double getTimeInPreviousState();
	
	/**
	 * Get the total reward accumulated so far
	 * (includes reward for previous transition but no state reward for current (final) state).
	 * @param rsi Reward structure index
	 */
	public abstract double getTotalCumulativeReward(int rsi);
	
	/**
	 * Get the state reward for the previous state.
	 * (For continuous-time models, need to multiply this by time spent in the state.)
	 * @param rsi Reward structure index
	 */
	public abstract double getPreviousStateReward(int rsi);
	
	/**
	 * Get the state rewards for the previous state.
	 * (For continuous-time models, need to multiply these by time spent in the state.)
	 */
	public abstract double[] getPreviousStateRewards();
	
	/**
	 * Get the transition reward for the transition between the previous and current states.
	 * @param rsi Reward structure index
	 */
	public abstract double getPreviousTransitionReward(int rsi);

	/**
	 * Get the transition rewards for the transition between the previous and current states.
	 */
	public abstract double[] getPreviousTransitionRewards();

	/**
	 * Get the state reward for the current state.
	 * (For continuous-time models, need to multiply this by time spent in the state.)
	 * @param rsi Reward structure index
	 */
	public abstract double getCurrentStateReward(int rsi);
	
	/**
	 * Get the state rewards for the current state.
	 * (For continuous-time models, need to multiply these by time spent in the state.)
	 */
	public abstract double[] getCurrentStateRewards();
	
	/**
	 * Get the memory of the strategy (if present) for the current state.
	 */
	public abstract int getCurrentStrategyMemory();
	
	/**
	 * Get the decision taken by the strategy (if present) in the current state.
	 */
	public abstract Object getCurrentStrategyDecision();
	
	/**
	 * Does the path contain a deterministic loop?
	 */
	public abstract boolean isLooping();
	
	/**
	 * What is the step index of the start of the deterministic loop, if it exists?
	 */
	public abstract long loopStart();
	
	/**
	 * What is the step index of the end of the deterministic loop, if it exists?
	 */
	public abstract long loopEnd();
	
	// UTILITY METHODS

	/**
	 * Evaluate an expression in the current state of the path.
	 * This takes in to account both the state variables and observables.
	 */
	public Object evaluateInCurrentState(Expression expr) throws PrismLangException
	{
		EvaluateContextFull ec = new EvaluateContextFull(getCurrentState(), getCurrentObservation());
		return expr.evaluate(ec);
	}
	
	/**
	 * Evaluate a Boolean-valued expression in the current state of the path.
	 * This takes in to account both the state variables and observables.
	 */
	public boolean evaluateBooleanInCurrentState(Expression expr) throws PrismLangException
	{
		EvaluateContextFull ec = new EvaluateContextFull(getCurrentState(), getCurrentObservation());
		return TypeBool.getInstance().castValueTo(expr.evaluate(ec), EvalMode.FP);
	}

	/**
	 * Evaluate an expression in the penultimate state of the path.
	 * This takes in to account both the state variables and observables.
	 */
	public Object evaluateInPreviousState(Expression expr) throws PrismLangException
	{
		EvaluateContextFull ec = new EvaluateContextFull(getPreviousState(), getPreviousObservation());
		return expr.evaluate(ec);
	}
	
	/**
	 * Evaluate a Boolean-valued expression in the penultimate state of the path.
	 * This takes in to account both the state variables and observables.
	 */
	public boolean evaluateBooleanInPreviousState(Expression expr) throws PrismLangException
	{
		EvaluateContextFull ec = new EvaluateContextFull(getPreviousState(), getPreviousObservation());
		return TypeBool.getInstance().castValueTo(expr.evaluate(ec), EvalMode.FP);
	}
}
