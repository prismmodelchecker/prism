//==============================================================================
//
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import explicit.Model;
import parser.State;
import prism.Evaluator;
import prism.PrismException;
import prism.RewardGenerator;
import prism.RewardInfo;

import java.util.List;

/**
 * Base class for exposing {@link Rewards} as a {@link RewardGenerator}.
 * Also needed are: syntactic information about reward (e.g., names),
 * provided as a {@link RewardInfo}; the {@link Model} corresponding
 * to the {@link Rewards} ;and an {@link Evaluator} for reward values.
 * The latter three are provided when this class is constructed;
 * the rewards themselves are provided by subclasses implementing
 * {@link #getTheRewardObject(int)}, allowing them to be built lazily.
 */
public abstract class Rewards2RewardGenerator<Value> implements RewardGenerator<Value>
{
	// Reward info from importer
	protected RewardInfo rewardInfo;
	// Model that rewards are for
	protected Model<Value> model;
	// Evaluator for reward values
	protected Evaluator<Value> eval;
	// State list (optionally)
	protected List<State> statesList;

	/**
	 * Construct a Rewards2RewardGenerator object, storing the reward info and model.
	 * The evaluator for rewards is extracted from the model.
	 */
	public Rewards2RewardGenerator(RewardGenerator<?> rewardInfo, Model<Value> model) throws PrismException
	{
		this(rewardInfo, model, model.getEvaluator());
	}

	/**
	 * Construct a Rewards2RewardGenerator object, storing the reward info, model and evaluator.
	 */
	public Rewards2RewardGenerator(RewardInfo rewardInfo, Model<Value> model, Evaluator<Value> eval) throws PrismException
	{
		this.rewardInfo = rewardInfo;
		this.model = model;
		this.eval = eval;
		// Also store the model's states list. If present, it can be used
		// to support reward look up by State, rather than just state index.
		statesList = model.getStatesList();
	}

	/**
	 * Provide a {@link Rewards} object representing the {@code r}th reward structure
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 */
	public abstract Rewards<Value> getTheRewardObject(int r) throws PrismException;

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
		return getTheRewardObject(r);
	}

	@Override
	public Model<Value> getRewardObjectModel() throws PrismException
	{
		return model;
	}
}
