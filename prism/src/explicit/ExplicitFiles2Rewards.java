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
import explicit.rewards.Rewards2RewardGenerator;
import explicit.rewards.RewardsSimple;
import io.ExplicitModelImporter;
import io.IOUtils;
import prism.Evaluator;
import prism.PrismComponent;
import prism.PrismException;
import prism.RewardGenerator;

/**
 * Class to import rewards from explicit files and expose them via a {@link RewardGenerator}.
 */
public class ExplicitFiles2Rewards<Value> extends PrismComponent
{
	// Importer from files
	protected ExplicitModelImporter importer;
	// Model that rewards are for
	protected Model<Value> model;
	// Evaluator for reward values
	protected Evaluator<Value> eval;
	// Local reward storage
	protected RewardsSimple<Value>[] rewards;

	/**
	 * Construct a ExplicitFiles2Rewards object for a specified importer/model.
	 * The rewards are actually imported/stored later, on demand.
	 * The evaluator for rewards is extracted from the model.
	 */
	public ExplicitFiles2Rewards(PrismComponent parent, ExplicitModelImporter importer, Model<Value> model) throws PrismException
	{
		this(parent, importer, model, model.getEvaluator());
	}

	/**
	 * Construct a ExplicitFiles2Rewards object for a specified importer.
	 * The rewards are actually imported/stored later, on demand.
	 */
	public ExplicitFiles2Rewards(PrismComponent parent, ExplicitModelImporter importer, Model<?> model, Evaluator<Value> eval) throws PrismException
	{
		super(parent);
		this.importer = importer;
		this.model = (Model<Value>) model;
		this.eval = eval;
		// Pass model to importer
		this.importer.setModel(this.model);
		// Initialise storage
		rewards = new RewardsSimple[importer.getRewardInfo().getNumRewardStructs()];
	}

	/**
	 * Get access to the rewards, as a {@link RewardGenerator}.
	 */
	public RewardGenerator<Value> getRewardGenerator() throws PrismException
	{
		return new Rewards2RewardGenerator<Value>(importer.getRewardInfo(), model, eval)
		{
			@Override
			public Rewards<Value> getTheRewardObject(int r) throws PrismException
			{
				return ExplicitFiles2Rewards.this.getTheRewardObject(r);
			}
		};
	}

	/**
	 * Provide the rewards when requested, importing them from the explicit files.
	 */
	protected Rewards<Value> getTheRewardObject(int r) throws PrismException
	{
		// Lazily load rewards from file when requested
		if (rewards[r] == null) {
			rewards[r] = new RewardsSimple<>(importer.getNumStates());
			rewards[r].setEvaluator(eval);
			importer.extractStateRewards(r, (i, v) -> storeStateReward(r, i, v), eval);
			if (!model.getModelType().nondeterministic()) {
				importer.extractMCTransitionRewards(r, (s, s2, v) -> storeMCTransitionReward(r, s, s2, v), eval);
			} else {
				importer.extractMDPTransitionRewards(r, (s, i, v) -> storeMDPTransitionReward(r, s, i, v), eval);
			}
		}
		return rewards[r];
	}

	// Methods to create local reward storage

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
		rewards[r].setTransitionReward(s, s2, v);
	}

	/**
	 * Store a (MDP) transition reward.
	 * @param r Reward structure index
	 * @param s State index (source)
	 * @param i Choice index
	 * @param v Reward value
	 */
	protected void storeMDPTransitionReward(int r, int s, int i, Value v)
	{
		rewards[r].setTransitionReward(s, i, v);
	}
}
