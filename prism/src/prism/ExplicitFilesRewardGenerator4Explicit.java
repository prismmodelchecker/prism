package prism;

import java.io.File;
import java.util.BitSet;
import java.util.List;

/**
 * This Class extends the ExplicitFilesRewardGenerator for the explicit engine.
 */

public class ExplicitFilesRewardGenerator4Explicit extends ExplicitFilesRewardGenerator
{
	protected double[][] stateRewards; // state reward structures

	public ExplicitFilesRewardGenerator4Explicit(PrismComponent parent, List<File> stateRewardsFiles, int numStates) throws PrismException
	{
		super(parent, stateRewardsFiles, numStates);
		stateRewards = new double[getNumRewardStructs()][];
	}

	/**
	 * Stores the state rewards in the required format for explicit.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param i state index
	 * @param d reward value
	 */
	protected void storeReward(int rewardStructIndex, int i, double d)
	{
		stateRewards[rewardStructIndex][i] = d;
	}

	/**
	 * Lazy load rewards from file when requested.
	 *
	 * @param r The index of the reward structure to use
	 * @param s The index of the state in which to evaluate the rewards
	 * @return state reward
	 * @throws PrismException if an error occurs during reward extraction
	 */
	@Override
	public double getStateReward(int r, int s) throws PrismException
	{
		if (stateRewards[r] == null) {
			stateRewards[r] = new double[numStates];
			extractStateRewards(r);
		}
		return stateRewards[r][s];
	}
}