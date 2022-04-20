package prism;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

import java.io.File;
import java.util.List;

/**
 * This Class extends the ExplicitFilesRewardGenerator for the mtbdd engine.
 *
 * It is necessary that initRewardGenerator() is executed, before the state rewards can be extracted.
 */

public class ExplicitFilesRewardGenerator4MTBDD extends ExplicitFilesRewardGenerator
{
	protected JDDNode[] stateRewards; // state reward structs

	protected int numVars;

	protected int statesArray[][] = null; // Explicit storage of states

	protected JDDVars[] varDDRowVars; // dd vars (row/col) for each module variable
	protected JDDVars[] varDDColVars;

	protected boolean rewardGeneratorInitialized = false;

	public ExplicitFilesRewardGenerator4MTBDD(PrismComponent parent, List<File> stateRewardsFile, int numStates) throws PrismException
	{
		super(parent, stateRewardsFile, numStates);
		stateRewards = new JDDNode[stateRewardsFiles.size()];
	}

	/**
	 * This method initializes additional parameters, which are needed for the store of state rewards for the mtbdd.
	 * @param statesArray
	 * @param varDDColVars
	 * @param varDDRowVars
	 * @param numVars
	 */
	public void initRewardGenerator(int[][] statesArray, JDDVars[] varDDColVars, JDDVars[] varDDRowVars, int numVars)
	{
		this.statesArray = statesArray;
		this.varDDColVars = varDDColVars;
		this.varDDRowVars = varDDRowVars;
		this.numVars = numVars;
		this.rewardGeneratorInitialized = true;
	}

	/**
	 * Stores stateRewards in the required format for mtbdd.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param i state index
	 * @param d reward value
	 */
	protected void storeReward(int rewardStructIndex, int i, double d)
	{
		if (!rewardGeneratorInitialized)
			throw new IllegalStateException("Reward generator is not initialized!");

		// construct element of vector mtbdd
		// case where we don't have a state list...
		JDDNode tmp;
		if (statesArray == null) {
			tmp = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[0], i, 1.0);
		}
		// case where we do have a state list...
		else {
			tmp = JDD.Constant(1);
			for (int j = 0; j < numVars; j++) {
				tmp = JDD.Times(tmp, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[j], statesArray[i][j], 1));
			}
		}
		// add it into mtbdd for state rewards
		stateRewards[rewardStructIndex] = JDD.Plus(stateRewards[rewardStructIndex], JDD.Times(JDD.Constant(d), tmp));
	}

	/**
	 * Get all reward structures in mtbdd format.
	 *
	 * @return reward structures
	 * @throws PrismException if an error occurs during reward extraction
	 */
	public JDDNode[] getRewardStructs() throws PrismException
	{
		// Ensure all rewards are loaded
		for (int k = 0, numFiles = stateRewardsFiles.size(); k < numFiles; k++) {
			getStateReward(k);
		}
		return stateRewards;
	}

	/**
	 * Lazy load rewards from file when requested.
	 * 
	 * @param rewardIndex index of the reward structure
	 * @return a JDDNode for the reward structure
	 * @throws PrismException if an error occurs during reward extraction
	 */
	public JDDNode getStateReward(int rewardIndex) throws PrismException
	{
		if (stateRewards[rewardIndex] == null) {
			stateRewards[rewardIndex] = JDD.Constant(0);
			extractStateRewards(rewardIndex);
		}
		return stateRewards[rewardIndex];
	}
}