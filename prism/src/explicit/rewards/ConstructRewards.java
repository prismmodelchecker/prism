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

package explicit.rewards;

import java.util.List;

import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.ast.RewardStruct;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import explicit.MDP;
import explicit.Model;

public class ConstructRewards
{
	protected PrismLog mainLog;

	public ConstructRewards()
	{
		this(new PrismFileLog("stdout"));
	}

	public ConstructRewards(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from a model and reward structure. 
	 * @param model The DTMC or CTMC
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public MCRewards buildMCRewardStructure(Model model, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		Expression guard;
		int i, j, n, numStates;

		if (rewStr.getNumTransItems() > 0) {
			throw new PrismException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}
		// Special case: constant rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			return new StateRewardsConstant(rewStr.getReward(0).evaluateDouble(constantValues));
		}
		// Normal: state rewards
		else {
			numStates = model.getNumStates();
			statesList = model.getStatesList();
			MCRewardsStateArray rewSA = new MCRewardsStateArray(numStates);
			n = rewStr.getNumItems();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				for (j = 0; j < numStates; j++) {
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						rewSA.setStateReward(j, rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j)));
					}
				}
			}
			return rewSA;
		}
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from a model and reward structure. 
	 * @param model The DTMC or CTMC
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
			return new StateRewardsConstant(rewStr.getReward(0).evaluateDouble(constantValues));
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
							numChoices = mdp.getNumChoices(j);
							for (k = 0; k < numChoices; k++) {
								mdpAction = mdp.getAction(j, k);
								if (mdpAction == null ? (action == null) : mdpAction.equals(action)) {
									rewSimple.setTransitionReward(j, k, rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j)));
								}
							}
						}
						// State reward
						else {
							rewSimple.setStateReward(j, rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j)));
						}
					}
				}
			}
			return rewSimple;
		}
	}
}
