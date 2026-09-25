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

package simulator.sampler;

import parser.ast.Expression;
import parser.ast.ExpressionTemporal;
import prism.ModelGenerator;
import prism.PrismException;
import simulator.Path;

public class SamplerRewardReach extends SamplerDouble
{
	private Expression target;
	private int rewardStructIndex;
	private double disc;
	private double discRewardSum;
	private double discFactor;

	/**
	 * Construct a sampler for a reachability reward property.
	 * Passed in ExpressionTemporal should be a property of this type.
	 * Reward structure index should also be specified.
	 * All constants should have already been evaluated/replaced.
	 */
	public SamplerRewardReach(ExpressionTemporal expr, int rewardStructIndex) throws PrismException
	{
		// Make sure expression is of the correct type
		// Then extract other required info
		if (expr.getOperator() != ExpressionTemporal.P_F)
			throw new PrismException("Error creating Sampler");
		target = expr.getOperand2();
		this.rewardStructIndex = rewardStructIndex;
		disc = getRewardDiscount(expr);
		// Initialise sampler info
		reset();
		resetStats();
	}

	@Override
	public void reset()
	{
		super.reset();
		discRewardSum = 0.0;
		discFactor = 1.0;
	}

	@Override
	public boolean update(Path path, ModelGenerator modelGen) throws PrismException
	{
		// If the answer is already known we should do nothing
		if (valueKnown)
			return true;
		// Check that the latest rewards are non-negative
		checkPreviousStepRewardsNonNegative(path, rewardStructIndex);
		// Reward for step t (state s_t, plus transition s_t->s_t+1) is weighted by disc^t
		if (disc != 1.0 && path.size() > 0) {
			discRewardSum += discFactor * (path.getPreviousStateReward(rewardStructIndex) + path.getPreviousTransitionReward(rewardStructIndex));
			discFactor *= disc;
		}
		if (path.evaluateBooleanInCurrentState(target)) {
			valueKnown = true;
			value = disc != 1.0 ? discRewardSum : path.getTotalCumulativeReward(rewardStructIndex);
		}
		// With discounting, the value is finite even if the target is never reached,
		// and nothing more is added once the discount factor has become 0
		else if (disc != 1.0 && discFactor == 0.0) {
			valueKnown = true;
			value = discRewardSum;
		}
		// Or, if we are now at a deadlock/self-loop, the target will never be reached
		else if (modelGen != null && (modelGen.isDeadlock() || path.isLooping())) {
			valueKnown = true;
			// Undiscounted, the reward to reach the target is then infinite (as for the other engines)
			if (disc == 1.0) {
				value = Double.POSITIVE_INFINITY;
			}
			// With discounting, the remaining reward is known exactly: the same reward r
			// every step from now on (for a deadlock, a self-loop with no transition reward)
			else {
				double r = modelGen.isDeadlock() ? path.getCurrentStateReward(rewardStructIndex)
						: path.getPreviousStateReward(rewardStructIndex) + path.getPreviousTransitionReward(rewardStructIndex);
				value = discRewardSum + discFactor * r / (1.0 - disc);
			}
		}

		return valueKnown;
	}
}
