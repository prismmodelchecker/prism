//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
//	* Ludwig Pauly <ludwigpauly@gmail.com> (TU Dresden)
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

import explicit.rewards.Rewards;
import explicit.rewards.RewardsSimple;
import io.ExplicitModelImporter;
import io.IOUtils;
import parser.State;
import prism.Evaluator;
import prism.PrismComponent;
import prism.PrismException;
import prism.RewardGenerator;

import java.util.List;

/**
 * Class to import rewards from explicit files and expose them via a RewardGenerator.
 */
public class ExplicitFiles2Rewards<Value> extends PrismComponent implements RewardGenerator<Value>
{
	// Importer from files
	protected ExplicitModelImporter importer;
	// Reward info (stored as RewardGenerator) from importer
	protected RewardGenerator<?> rewardInfo;
	// Model that rewards are for
	protected Model<Value> model;
	// Evaluator for reward values
	protected Evaluator<Value> eval;
	// State list (optionally)
	protected List<State> statesList;

	// Local reward storage
	protected RewardsSimple<Value>[] rewards;

	/**
	 * Construct a ExplicitFiles2Rewards object for a specified importer.
	 * The rewards are actually imported/stored later, on demand.
	 */
	public ExplicitFiles2Rewards(PrismComponent parent, ExplicitModelImporter importer) throws PrismException
	{
		this(parent, importer, (Evaluator<Value>) Evaluator.forDouble());
	}

	/**
	 * Construct a ExplicitFiles2Rewards object for a specified importer.
	 * The rewards are actually imported/stored later, on demand.
	 */
	public ExplicitFiles2Rewards(PrismComponent parent, ExplicitModelImporter importer, Evaluator<Value> eval) throws PrismException
	{
		super(parent);
		this.importer = importer;
		this.eval = eval;
		rewardInfo = importer.getRewardInfo();
		// Initialise storage
		rewards = new RewardsSimple[rewardInfo.getNumRewardStructs()];
	}

	/**
	 * Provide access to the model for which the rewards are to be defined.
	 * Needed to look up information when storing transition rewards.
	 * The model's attached states list is also stored.
	 */
	public void setModel(Model<Value> model)
	{
		this.model = model;
		setStatesList(model.getStatesList());
	}

	/**
	 * Optionally, provide a list of model states,
	 * so that rewards can be looked up by State object, as well as state index.
	 */
	public void setStatesList(List<State> statesList)
	{
		this.statesList = statesList;
	}

	/**
	 * Store a state reward.
	 * @param r Reward structure index
	 * @param s State index
	 * @param v Reward value
	 */
	protected void storeStateReward(int r, int s, Value v)
	{
		rewards[r].setStateReward(s, v);
	}

	/**
	 * Store a (Markov chain) transition reward.
	 * @param r Reward structure index
	 * @param s State index (source)
	 * @param s2 State index (destination)
	 * @param v Reward value
	 */
	protected void storeMCTransitionReward(int r, int s, int s2, Value v)
	{
		// Find successor index for state s2 (from state s)
		SuccessorsIterator it = model.getSuccessors(s);
		int i = 0;
		while (it.hasNext()) {
			if (it.nextInt() == s2) {
				rewards[r].setTransitionReward(s, i, v);
				return;
			}
			i++;
		}
		// No matching transition found
	}

	/**
	 * Store a (MDP) transition reward.
	 * @param r Reward structure index
	 * @param s State index (source)
	 * @param i Choice index
	 * @param s2 State index (destination)
	 * @param v Reward value
	 */
	protected void storeMDPTransitionReward(int r, int s, int i, int s2, Value v)
	{
		// For now, don't bother to check that the reward is the same for all s2
		// for a given state s and index i (so the last one in the file will define it)
		rewards[r].setTransitionReward(s, i, v);
	}

	// Methods to implement RewardGenerator

	@Override
	public Evaluator<Value> getRewardEvaluator()
	{
		return eval;
	}

	@Override
	public List<String> getRewardStructNames()
	{
		return rewardInfo.getRewardStructNames();
	}

	@Override
	public int getNumRewardStructs()
	{
		return rewardInfo.getNumRewardStructs();
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int r)
	{
		return rewardInfo.rewardStructHasTransitionRewards(r);
	}

	@Override
	public boolean isRewardLookupSupported(RewardLookup lookup)
	{
		return (lookup == RewardLookup.BY_STATE_INDEX) || (lookup == RewardLookup.BY_STATE && statesList != null) || (lookup == RewardLookup.BY_REWARD_OBJECT);
	}

	@Override
	public Value getStateReward(int r, State state) throws PrismException
	{
		if (statesList == null) {
			throw new PrismException("Reward lookup by State not possible since state list is missing");
		}
		int s = statesList.indexOf(state);
		if (s == -1) {
			throw new PrismException("Unknown state " + state);
		}
		return getStateReward(r, s);
	}

	@Override
	public Value getStateReward(int r, int s) throws PrismException
	{
		return getRewardObject(r).getStateReward(s);
	}

	@Override
	public Rewards<Value> getRewardObject(int r) throws PrismException
	{
		// Lazily load rewards from file when requested
		if (rewards[r] == null) {
			rewards[r] = new RewardsSimple<>(importer.getNumStates());
			rewards[r].setEvaluator(eval);
			importer.extractStateRewards(r, (i, v) -> storeStateReward(r, i, v), eval);
			if (!model.getModelType().nondeterministic()) {
				importer.extractMCTransitionRewards(r, (s, s2, v) -> storeMCTransitionReward(r, s, s2, v), eval);
			} else {
				importer.extractMDPTransitionRewards(r,
					new IOUtils.TransitionStateRewardConsumer<Value>() {
						int sLast = -1;
						int iLast = -1;
						Value vLast = null;
						int count = 0;
						public void accept(int s, int i, int s2, Value v) throws PrismException
						{
							count++;
							// Check that transition rewards for the same state/choice are the same
							// (currently no support for state-choice-state rewards)
							if (s == sLast && i == iLast) {
								if (!eval.equals(vLast, v)) {
									throw new PrismException("mismatching transition rewards " + vLast + " and " + v + " in choice " + i + " of state " + s);
								}
							}
							// And check that were rewards on all successors for each choice
							// (for speed, we just check that the right number were present)
							else {
								if (sLast != -1 && count != ((NondetModel<?>) model).getNumTransitions(sLast, iLast)) {
									throw new PrismException("wrong number of transition rewards in choice " + iLast + " of state " + sLast);
								}
								sLast = s;
								iLast = i;
								vLast = v;
								count = 0;
							}
							storeMDPTransitionReward(r, s, i, s2, v);
						}
					}, eval);
			}
		}
		return rewards[r];
	}

	@Override
	public Model<Value> getRewardObjectModel() throws PrismException
	{
		return model;
	}
}
