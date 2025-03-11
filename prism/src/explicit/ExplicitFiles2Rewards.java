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
}
