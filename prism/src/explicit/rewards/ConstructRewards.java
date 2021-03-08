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
import java.util.List;

import explicit.DTMC;
import explicit.MDP;
import explicit.Model;
import parser.State;
import parser.Values;
import parser.ast.ASTElement;
import parser.ast.Expression;
import parser.ast.RewardStruct;
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
	 * Construct the rewards for a model from a reward generator. 
	 * @param model The model
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public Rewards buildRewardStructure(Model model, RewardGenerator rewardGen, int r) throws PrismException
	{
		switch (model.getModelType()) {
		case DTMC:
		case CTMC:
			return buildMCRewardStructure((DTMC) model, rewardGen, r);
		case MDP:
		case POMDP:
			return buildMDPRewardStructure((MDP) model, rewardGen, r);
		default:
			throw new PrismNotSupportedException("Cannot build rewards for " + model.getModelType() + "s");
		}
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from a reward generator. 
	 * @param mc The DTMC or CTMC
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public MCRewards buildMCRewardStructure(DTMC mc, RewardGenerator rewardGen, int r) throws PrismException
	{
		if (rewardGen.rewardStructHasTransitionRewards(r)) {
			throw new PrismNotSupportedException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}
		int numStates = mc.getNumStates();
		List<State> statesList = mc.getStatesList();
		StateRewardsArray rewSA = new StateRewardsArray(numStates);
		for (int s = 0; s < numStates; s++) {
			if (rewardGen.rewardStructHasStateRewards(r)) {
				double rew = getAndCheckStateReward(s, rewardGen, r, statesList);
				rewSA.addToStateReward(s, rew);
			}
		}
		return rewSA;
	}

	/**
	 * Construct the rewards for an MDP from a reward generator. 
	 * @param mdp The MDP
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public MDPRewards buildMDPRewardStructure(MDP mdp, RewardGenerator rewardGen, int r) throws PrismException
	{
		int numStates = mdp.getNumStates();
		List<State> statesList = mdp.getStatesList();
		MDPRewardsSimple rewSimple = new MDPRewardsSimple(numStates);
		for (int s = 0; s < numStates; s++) {
			if (rewardGen.rewardStructHasStateRewards(r)) {
				double rew = getAndCheckStateReward(s, rewardGen, r, statesList);
				rewSimple.addToStateReward(s, rew);
			}
			if (rewardGen.rewardStructHasTransitionRewards(r)) {
				// Don't add rewards to transitions added to "fix" deadlock states
				if (mdp.isDeadlockState(s)) {
					continue;
				}
				int numChoices = mdp.getNumChoices(s);
				for (int k = 0; k < numChoices; k++) {
					double rew = getAndCheckStateActionReward(s, mdp.getAction(s, k), rewardGen, r, statesList);
					rewSimple.addToTransitionReward(s, k, rew);
				}
			}
		}
		return rewSimple;
	}
	
	/**
	 * Get a state reward for a specific state and reward structure from a RewardGenerator.
	 * Also check that the state reward is legal. Throw an exception if not.
	 * @param s The index of the state
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 * @param statesLists List of states (maybe needed for state look up)
	 */
	private double getAndCheckStateReward(int s, RewardGenerator rewardGen, int r, List<State> statesList) throws PrismException
	{
		double rew = 0;
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
		checkStateReward(rew, stateIndex, null);
		return rew;
	}

	/**
	 * Get a state reward for a specific state and reward structure from a RewardGenerator.
	 * Also check that the state reward is legal. Throw an exception if not.
	 * @param s The index of the state
	 * @param rewardGen The RewardGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 * @param statesLists List of states (maybe needed for state look up)
	 */
	private double getAndCheckStateActionReward(int s, Object action, RewardGenerator rewardGen, int r, List<State> statesList) throws PrismException
	{
		double rew = 0;
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
		checkTransitionReward(rew, stateIndex, null);
		return rew;
	}

	/**
	 * Construct rewards from a model and reward structure. 
	 * @param model The model
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public Rewards buildRewardStructure(Model model, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		switch (model.getModelType()) {
		case DTMC:
		case CTMC:
			return buildMCRewardStructure((DTMC) model, rewStr, constantValues);
		case MDP:
		case POMDP:
			return buildMDPRewardStructure((MDP) model, rewStr, constantValues);
		default:
			throw new PrismNotSupportedException("Cannot build rewards for " + model.getModelType() + "s");
		}
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from a model and reward structure. 
	 * @param mc The DTMC or CTMC
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public MCRewards buildMCRewardStructure(DTMC mc, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		Expression guard;
		int i, j, n, numStates;

		if (rewStr.getNumTransItems() > 0) {
			throw new PrismNotSupportedException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}
		// Special case: constant rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			checkStateReward(rew, null, rewStr.getReward(0));
			return new StateRewardsConstant(rew);
		}
		// Normal: state rewards
		else {
			numStates = mc.getNumStates();
			statesList = mc.getStatesList();
			StateRewardsArray rewSA = new StateRewardsArray(numStates);
			n = rewStr.getNumItems();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				for (j = 0; j < numStates; j++) {
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
						checkStateReward(rew, statesList.get(j), rewStr.getReward(i));
						rewSA.addToStateReward(j, rew);
					}
				}
			}
			return rewSA;
		}
	}

	/**
	 * Construct the rewards for an MDP from a model and reward structure. 
	 * @param mdp The MDP
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public MDPRewards buildMDPRewardStructure(MDP mdp, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		Expression guard;
		String action;
		Object mdpAction;
		int i, j, k, n, numStates, numChoices;

		// Special case: constant state rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			checkStateReward(rew, null, rewStr.getReward(0));
			return new StateRewardsConstant(rew);
		}
		// Normal: state and transition rewards
		else {
			numStates = mdp.getNumStates();
			statesList = mdp.getStatesList();
			MDPRewardsSimple rewSimple = new MDPRewardsSimple(numStates);
			n = rewStr.getNumItems();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				action = rewStr.getSynch(i);
				for (j = 0; j < numStates; j++) {
					// Is guard satisfied?
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						// Transition reward
						if (rewStr.getRewardStructItem(i).isTransitionReward()) {
							if (mdp.isDeadlockState(j)) {
								// As state s is a deadlock state, any outgoing transition
								// was added to "fix" the deadlock and thus does not get a reward.
								// Skip to next state
								continue;
							}
							numChoices = mdp.getNumChoices(j);
							for (k = 0; k < numChoices; k++) {
								mdpAction = mdp.getAction(j, k);
								if (mdpAction == null ? (action.isEmpty()) : mdpAction.equals(action)) {
									double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
									checkTransitionReward(rew, statesList.get(j), rewStr.getReward(i));
									rewSimple.addToTransitionReward(j, k, rew);
								}
							}
						}
						// State reward
						else {
							double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
							checkStateReward(rew, statesList.get(j), rewStr.getReward(i));
							rewSimple.addToStateReward(j, rew);
						}
					}
				}
			}
			return rewSimple;
		}
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from files exported explicitly by PRISM. 
	 * @param mc The DTMC or CTMC
	 * @param rews The file containing state rewards (ignored if null)
	 * @param rewt The file containing transition rewards (ignored if null)
	 */
	public MCRewards buildMCRewardsFromPrismExplicit(DTMC mc, File rews, File rewt) throws PrismException
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
	 * @param model The MDP
	 * @param rews The file containing state rewards (ignored if null)
	 * @param rewt The file containing transition rewards (ignored if null)
	 */
	public MDPRewards buildMDPRewardsFromPrismExplicit(MDP mdp, File rews, File rewt) throws PrismException
	{
		String s, ss[];
		int i, j, lineNum = 0;
		double reward;
		MDPRewardsSimple rs = new MDPRewardsSimple(mdp.getNumStates());

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
	 * @param stateIndex The index of the state, for error reporting (optional)
	 * @param ast Where the error occurred, for error reporting (optional)
	 */
	private void checkStateReward(double rew, Object stateIndex, ASTElement ast) throws PrismException
	{
		String error = null;
		if (!Double.isFinite(rew)) {
			error = "State reward is not finite";
		} else if (!allowNegative && rew < 0) {
			error = "State reward is negative (" + rew + ")";
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
	 * Check that a state reward is legal. Throw an exception if not.
	 * @param rew The reward value
	 * Optionally, provide a state where the error occurs (as an Object),
	 * and/or a pointer to where the error occurs syntactically (as an ASTElement) 
	 * @param rew The reward value
	 * @param stateIndex The index of the state, for error reporting (optional)
	 * @param ast Where the error occurred, for error reporting (optional)
	 */
	private void checkTransitionReward(double rew, Object stateIndex, ASTElement ast) throws PrismException
	{
		String error = null;
		if (!Double.isFinite(rew)) {
			error = "Transition reward is not finite";
		} else if (!allowNegative && rew < 0) {
			error = "Transition reward is negative (" + rew + ")";
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
}
