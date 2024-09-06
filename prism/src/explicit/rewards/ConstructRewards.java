//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package explicit.rewards;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import explicit.DTMC;
import explicit.MDP;
import explicit.Model;
import explicit.NondetModel;
import parser.State;
import parser.Values;
import parser.ast.ASTElement;
import parser.ast.Expression;
import parser.ast.RewardStruct;
import prism.Evaluator;
import prism.Pair;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.RewardGenerator;
import prism.RewardGenerator.RewardLookup;

public class ConstructRewards extends PrismComponent
{
	public ConstructRewards(PrismComponent parent)
	{
		super(parent);
	}

	/** Allow negative rewards, i.e., weights. Defaults to false. */
	protected boolean allowNegative = false;

	/** Set flag that negative rewards are allowed, i.e., weights */
	public void allowNegativeRewards()
	{
		allowNegative = true;
	}

	/**
	 * Construct expected reward, i.e., using probability-weighted sum for any rewards
	 * attached to transitions, assigning them to states/choices. Defaults to false.
	 */
	protected boolean expectedRewards = false;

	/**
	 * Specify whether to construct expected reward, i.e., using probability-weighted sum for any rewards
	 * attached to transitions, assigning them to states/choices. Defaults to false.
	 */
	public void setExpectedRewards(boolean expectedRewards)
	{
		this.expectedRewards = expectedRewards;
	}

	/**
	 * Construct the rewards for a model from a reward generator. 
	 * @param model The model
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	@SuppressWarnings("unchecked")
	public <Value> Rewards<Value> buildRewardStructure(Model<Value> model, RewardGenerator<Value> rewardGen, int r) throws PrismException
	{
		// If the RewardGenerator already has the rewards built, use this (after checking)
		if (rewardGen.isRewardLookupSupported(RewardLookup.BY_REWARD_OBJECT)) {
			Rewards<Value> rewardsObj = rewardGen.getRewardObject(r);
			checkRewardObject(rewardsObj, rewardGen.getRewardObjectModel(), rewardGen.getRewardEvaluator());
			return rewardsObj;
		}
		// Extract some model info
		int numStates = model.getNumStates();
		List<State> statesList = model.getStatesList();
		// Create reward structure object of appropriate type
		// (can be more efficient if just double-valued state rewards)
		RewardsExplicit<Value> rewards;
		boolean nondet = model.getModelType().nondeterministic();
		boolean dbl = rewardGen.getRewardEvaluator().one() instanceof Double;
		boolean sr = !(rewardGen.rewardStructHasTransitionRewards(r) && !(expectedRewards && !nondet));
		if (dbl && sr) {
			rewards = (RewardsExplicit<Value>) new StateRewardsArray(numStates);
		} else {
			rewards = new RewardsSimple<>(numStates);
		}
		rewards.setEvaluator(rewardGen.getRewardEvaluator());
		// Add rewards
		for (int s = 0; s < numStates; s++) {
			// State rewards
			if (rewardGen.rewardStructHasStateRewards(r)) {
				Value rew = getAndCheckStateReward(s, rewardGen, r, statesList);
				rewards.addToStateReward(s, rew);
			}
			// Transition rewards
			if (rewardGen.rewardStructHasTransitionRewards(r)) {
				// Don't add rewards to transitions added to "fix" deadlock states
				if (model.isDeadlockState(s)) {
					continue;
				}
				// Nondet models (reward on choice/action)
				if (nondet) {
					NondetModel<Value> nondetModel = (NondetModel<Value>) model;
					int numChoices = nondetModel.getNumChoices(s);
					for (int k = 0; k < numChoices; k++) {
						Value rew = getAndCheckStateActionReward(s, nondetModel.getAction(s, k), rewardGen, r, statesList);
						rewards.addToTransitionReward(s, k, rew);
					}
				}
				// Markov chain models (rewards on transitions)
				else {
					DTMC<Value> mcModel = (DTMC<Value>) model;
					Iterator<Map.Entry<Integer, Pair<Value, Object>>> iter = mcModel.getTransitionsAndActionsIterator(s);
					int i = 0;
					while (iter.hasNext()) {
						Map.Entry<Integer, Pair<Value, Object>> e = iter.next();
						Value rew = getAndCheckStateActionReward(s, e.getValue().second, rewardGen, r, statesList);
						if (rewardGen.getRewardEvaluator().isZero(rew)) {
							i++;
							continue;
						}
						if (expectedRewards) {
							Value rewWeighted = rewardGen.getRewardEvaluator().multiply(e.getValue().first, rew);
							rewards.addToStateReward(s, rewWeighted);
						} else {
							rewards.addToTransitionReward(s, i, rew);
						}
						i++;
					}
				}
			}
		}
		return rewards;
	}

	/**
	 * Get a state reward for a specific state and reward structure from a RewardGenerator.
	 * Also check that the state reward is legal. Throw an exception if not.
	 * @param s The index of the state
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 * @param statesList List of states (maybe needed for state look up)
	 */
	private <Value> Value getAndCheckStateReward(int s, RewardGenerator<Value> rewardGen, int r, List<State> statesList) throws PrismException
	{
		Evaluator<Value> eval = rewardGen.getRewardEvaluator();
		Value rew = eval.zero();
		Object stateIndex = null;
		if (rewardGen.isRewardLookupSupported(RewardLookup.BY_STATE)) {
			State state = statesList.get(s);
			stateIndex = state;
			rew = rewardGen.getStateReward(r, state);
		} else if (rewardGen.isRewardLookupSupported(RewardLookup.BY_STATE_INDEX)) {
			stateIndex = s;
			rew = rewardGen.getStateReward(r, s);
		} else {
			throw new PrismException("Unknown reward lookup mechanism for reward generator");
		}
		checkStateReward(rew, eval, stateIndex, null);
		return rew;
	}

	/**
	 * Get a state-action reward for a specific state and reward structure from a RewardGenerator.
	 * Also check that the state reward is legal. Throw an exception if not.
	 * @param s The index of the state
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 * @param statesList List of states (maybe needed for state look up)
	 */
	private <Value> Value getAndCheckStateActionReward(int s, Object action, RewardGenerator<Value> rewardGen, int r, List<State> statesList) throws PrismException
	{
		Evaluator<Value> eval = rewardGen.getRewardEvaluator();
		Value rew = eval.zero();
		Object stateIndex = null;
		if (rewardGen.isRewardLookupSupported(RewardLookup.BY_STATE)) {
			State state = statesList.get(s);
			stateIndex = state;
			rew = rewardGen.getStateActionReward(r, state, action);
		} else if (rewardGen.isRewardLookupSupported(RewardLookup.BY_STATE_INDEX)) {
			stateIndex = s;
			rew = rewardGen.getStateActionReward(r, s, action);
		} else {
			throw new PrismException("Unknown reward lookup mechanism for reward generator");
		}
		checkTransitionReward(rew, eval, stateIndex, null);
		return rew;
	}

	/**
	 * Construct rewards from a model and reward structure. 
	 * @param model The model
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public Rewards<Double> buildRewardStructure(Model<Double> model, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		// Special case: constant rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			checkStateReward(rew, null, rewStr.getReward(0));
			return new StateRewardsConstant<>(rew);
		}
		// Extract some model info
		int numStates = model.getNumStates();
		List<State> statesList = model.getStatesList();
		// Create reward structure object of appropriate type
		// (can be more efficient if just (double-valued) state rewards)
		RewardsExplicit<Double> rewards;
		boolean nondet = model.getModelType().nondeterministic();
		boolean sr = !(rewStr.getNumTransItems() > 0 && !(expectedRewards && !nondet));
		if (sr) {
			rewards = new StateRewardsArray(numStates);
		} else {
			rewards = new RewardsSimple<>(numStates);
		}
		// Add rewards
		int n = rewStr.getNumItems();
		for (int i = 0; i < n; i++) {
			Expression guard = rewStr.getStates(i);
			String action = rewStr.getSynch(i);
			for (int s = 0; s < numStates; s++) {
				// Is guard satisfied?
				if (guard.evaluateBoolean(constantValues, statesList.get(s))) {
					// Transition reward
					if (rewStr.getRewardStructItem(i).isTransitionReward()) {
						// Don't add rewards to transitions added to "fix" deadlock states
						if (model.isDeadlockState(s)) {
							continue;
						}
						// Nondet models (reward on choice/action)
						if (nondet) {
							NondetModel<Double> nondetModel = (NondetModel<Double>) model;
							int numChoices = nondetModel.getNumChoices(s);
							for (int k = 0; k < numChoices; k++) {
								Object mdpAction = nondetModel.getAction(s, k);
								if (mdpAction == null ? (action.isEmpty()) : mdpAction.equals(action)) {
									double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(s));
									checkTransitionReward(rew, statesList.get(s), rewStr.getReward(i));
									rewards.addToTransitionReward(s, k, rew);
								}
							}
						} else {
							DTMC<Double> mcModel = (DTMC<Double>) model;
							Iterator<Map.Entry<Integer, Pair<Double, Object>>> iter = mcModel.getTransitionsAndActionsIterator(s);
							int j = 0;
							while (iter.hasNext()) {
								Map.Entry<Integer, Pair<Double, Object>> e = iter.next();
								Object mcAction = e.getValue().second;
								if (mcAction == null ? (action.isEmpty()) : mcAction.equals(action)) {
									double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(s));
									if (expectedRewards) {
										double rewWeighted = e.getValue().first * rew;
										rewards.addToStateReward(s, rewWeighted);
									} else {
										rewards.addToTransitionReward(s, j, rew);
									}
								}
								j++;
							}
						}
					}
					// State reward
					else {
						double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(s));
						checkStateReward(rew, statesList.get(s), rewStr.getReward(i));
						rewards.addToStateReward(s, rew);
					}
				}
			}
		}
		return rewards;
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from files exported explicitly by PRISM. 
	 * @param mc The DTMC or CTMC
	 * @param rews The file containing state rewards (ignored if null)
	 * @param rewt The file containing transition rewards (ignored if null)
	 */
	public MCRewards<Double> buildMCRewardsFromPrismExplicit(DTMC<Double> mc, File rews, File rewt) throws PrismException
	{
		String s, ss[];
		int i, lineNum = 0;
		double reward;
		StateRewardsArray rewSA = new StateRewardsArray(mc.getNumStates());

		if (rews != null) {
			// Open state rewards file, automatic close
			try (BufferedReader in = new BufferedReader(new FileReader(rews))) {
				// Ignore first line
				s = in.readLine();
				lineNum = 1;
				if (s == null) {
					throw new PrismException("Missing first line of state rewards file");
				}
				// Go though list of state rewards in file
				s = in.readLine();
				lineNum++;
				while (s != null) {
					s = s.trim();
					if (s.length() > 0) {
						ss = s.split(" ");
						i = Integer.parseInt(ss[0]);
						reward = Double.parseDouble(ss[1]);
						checkStateReward(reward, i, null);
						rewSA.setStateReward(i, reward);
					}
					s = in.readLine();
					lineNum++;
				}
			} catch (IOException e) {
				throw new PrismException("Could not read state rewards from file \"" + rews + "\"" + e);
			} catch (NumberFormatException e) {
				throw new PrismException("Problem in state rewards file (line " + lineNum + ") for MDP");
			}
		}

		if (rewt != null) {
			throw new PrismNotSupportedException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}

		return rewSA;
	}
	
	/**
	 * Construct the rewards for an MDP from files exported explicitly by PRISM.
	 * @param mdp The MDP
	 * @param rews The file containing state rewards (ignored if null)
	 * @param rewt The file containing transition rewards (ignored if null)
	 */
	public MDPRewards<Double> buildMDPRewardsFromPrismExplicit(MDP<Double> mdp, File rews, File rewt) throws PrismException
	{
		String s, ss[];
		int i, j, lineNum = 0;
		double reward;
		MDPRewardsSimple<Double> rs = new MDPRewardsSimple<>(mdp.getNumStates());

		if (rews != null) {
			// Open state rewards file, automatic close
			try (BufferedReader in = new BufferedReader(new FileReader(rews))) {
				// Ignore first line
				s = in.readLine();
				lineNum = 1;
				if (s == null) {
					throw new PrismException("Missing first line of state rewards file");
				}
				// Go though list of state rewards in file
				s = in.readLine();
				lineNum++;
				while (s != null) {
					s = s.trim();
					if (s.length() > 0) {
						ss = s.split(" ");
						i = Integer.parseInt(ss[0]);
						reward = Double.parseDouble(ss[1]);
						checkStateReward(reward, i, null);
						rs.setStateReward(i, reward);
					}
					s = in.readLine();
					lineNum++;
				}
			} catch (IOException e) {
				throw new PrismException("Could not read state rewards from file \"" + rews + "\"" + e);
			} catch (NumberFormatException e) {
				throw new PrismException("Problem in state rewards file (line " + lineNum + ") for MDP");
			}
		}

		if (rewt != null) {
			// Open transition rewards file, automatic close
			try (BufferedReader in = new BufferedReader(new FileReader(rewt))) {
				// Ignore first line
				s = in.readLine();
				lineNum = 1;
				if (s == null) {
					throw new PrismException("Missing first line of transition rewards file");
				}
				// Go though list of transition rewards in file
				s = in.readLine();
				lineNum++;
				while (s != null) {
					s = s.trim();
					if (s.length() > 0) {
						ss = s.split(" ");
						i = Integer.parseInt(ss[0]);
						j = Integer.parseInt(ss[1]);
						reward = Double.parseDouble(ss[3]);
						checkTransitionReward(reward, i, null);
						rs.setTransitionReward(i, j, reward);
					}
					s = in.readLine();
					lineNum++;
				}

			} catch (IOException e) {
				throw new PrismException("Could not read transition rewards from file \"" + rewt + "\"" + e);
			} catch (NumberFormatException e) {
				throw new PrismException("Problem in transition rewards file (line " + lineNum + ") for MDP");
			}
		}

		return rs;
	}

	/**
	 * Check that a state reward is legal. Throw an exception if not.
	 * Optionally, provide a state where the error occurs (as an Object),
	 * and/or a pointer to where the error occurs syntactically (as an ASTElement) 
	 * @param rew The reward value
	 * @param eval Evaluator matching the type {@code Value} of the reward value
	 * @param stateIndex The index of the state, for error reporting (optional)
	 * @param ast Where the error occurred, for error reporting (optional)
	 */
	private <Value> void checkStateReward(Value rew, Evaluator<Value> eval, Object stateIndex, ASTElement ast) throws PrismException
	{
		String error = null;
		// We omit the check in symbolic (parametric) cases - too expensive
		if (!eval.isSymbolic()) {
			if (!eval.isFinite(rew)) {
				error = "State reward is not finite";
			} else if (!allowNegative && !eval.geq(rew, eval.zero())) {
				error = "State reward is negative (" + rew + ")";
			}
		}
		if (error != null) {
			if (stateIndex != null) {
				error += " at state " + stateIndex;
			}
			if (ast != null) {
				throw new PrismLangException(error, ast);
			} else {
				throw new PrismException(error);
			}
		}
	}

	/**
	 * Check that a transition reward is legal. Throw an exception if not.
	 * Optionally, provide a state where the error occurs (as an Object),
	 * and/or a pointer to where the error occurs syntactically (as an ASTElement) 
	 * @param rew The reward value
	 * @param eval Evaluator matching the type {@code Value} of the reward value
	 * @param stateIndex The index of the state, for error reporting (optional)
	 * @param ast Where the error occurred, for error reporting (optional)
	 */
	private <Value> void checkTransitionReward(Value rew, Evaluator<Value> eval, Object stateIndex, ASTElement ast) throws PrismException
	{
		String error = null;
		// We omit the check in symbolic (parametric) cases - too expensive
		if (!eval.isSymbolic()) {
			if (!eval.isFinite(rew)) {
				error = "Transition reward is not finite";
			} else if (!allowNegative && !eval.geq(rew, eval.zero())) {
				error = "Transition reward is negative (" + rew + ")";
			}
		}
		if (error != null) {
			if (stateIndex != null) {
				error += " at state " + stateIndex;
			}
			if (ast != null) {
				throw new PrismLangException(error, ast);
			} else {
				throw new PrismException(error);
			}
		}
	}

	/**
	 * Check that all state/transition rewards in a Rewards object are legal. Throw an exception if not.
	 * Optionally, provide a state where the error occurs (as an Object),
	 * and/or a pointer to where the error occurs syntactically (as an ASTElement)
	 * @param rewards The rewards
	 * @param model The model for the rewards
	 * @param eval Evaluator matching the type {@code Value} of the reward value
	 */
	private <Value> void checkRewardObject(Rewards<Value> rewards, Model<Value> model, Evaluator<Value> eval) throws PrismException
	{
		int numStates = model.getNumStates();
		// State rewards
		for (int s = 0; s < numStates; s++) {
			checkStateReward(rewards.getStateReward(s), eval, s, null);
		}
		// Transition rewards (nondet models)
		if (model.getModelType().nondeterministic()) {
			for (int s = 0; s < numStates; s++) {
				int numChoices = ((NondetModel<?>) model).getNumChoices(s);
				for (int i = 0; i < numChoices; i++) {
					checkTransitionReward(rewards.getTransitionReward(s, i), eval, s, null);
				}
			}
		}
		// Transition rewards (Markov chain like models)
		else {
			for (int s = 0; s < numStates; s++) {
				int numTrans = model.getNumTransitions(s);
				for (int i = 0; i < numTrans; i++) {
					checkTransitionReward(rewards.getTransitionReward(s, i), eval, s, null);
				}
			}
		}
	}

	/**
	 * Check that a (double-valued) state reward is legal. Throw an exception if not.
	 * Optionally, provide a state where the error occurs (as an Object),
	 * and/or a pointer to where the error occurs syntactically (as an ASTElement) 
	 * @param rew The reward value
	 * @param stateIndex The index of the state, for error reporting (optional)
	 * @param ast Where the error occurred, for error reporting (optional)
	 */
	private <Value> void checkStateReward(double rew, Object stateIndex, ASTElement ast) throws PrismException
	{
		checkStateReward(rew, Evaluator.forDouble(), stateIndex, ast);
	}

	/**
	 * Check that a (double-valued) transition reward is legal. Throw an exception if not.
	 * Optionally, provide a state where the error occurs (as an Object),
	 * and/or a pointer to where the error occurs syntactically (as an ASTElement) 
	 * @param rew The reward value
	 * @param stateIndex The index of the state, for error reporting (optional)
	 * @param ast Where the error occurred, for error reporting (optional)
	 */
	private <Value> void checkTransitionReward(double rew, Object stateIndex, ASTElement ast) throws PrismException
	{
		checkTransitionReward(rew, Evaluator.forDouble(), stateIndex, ast);
	}
}
