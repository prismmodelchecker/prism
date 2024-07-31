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

import io.PrismExplicitImporter;
import parser.State;
import prism.PrismComponent;
import prism.PrismException;
import prism.RewardGenerator;

import java.util.List;

/**
 * Class to import rewards from explicit files and expose them via a RewardGenerator.
 */
public class ExplicitFiles2Rewards extends PrismComponent implements RewardGenerator
{
	// Importer from files
	protected PrismExplicitImporter importer;
	// Reward info (stored as RewardGenerator) from importer
	protected RewardGenerator rewardInfo;
	// State list (optionally)
	protected List<State> statesList;

	// Local reward storage
	protected double[][] stateRewards;

	/**
	 * Construct a ExplicitFiles2Rewards object for a specified importer.
	 * The rewards are actually imported/stored later, on demand.
	 */
	public ExplicitFiles2Rewards(PrismComponent parent, PrismExplicitImporter importer) throws PrismException
	{
		super(parent);
		this.importer = importer;
		rewardInfo = importer.getRewardInfo();
		// Initialise storage
		stateRewards = new double[rewardInfo.getNumRewardStructs()][];
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
	 * @param d Reward value
	 */
	protected void storeReward(int r, int s, double d)
	{
		stateRewards[r][s] = d;
	}

	// Methods to implement RewardGenerator

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
		return (lookup == RewardLookup.BY_STATE_INDEX) || (lookup == RewardLookup.BY_STATE && statesList != null);
	}

	@Override
	public Double getStateReward(int r, State state) throws PrismException
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
	public Double getStateReward(int r, int s) throws PrismException
	{
	 	// Lazily load rewards from file when requested
		int numStates = importer.getNumStates();
		if (stateRewards[r] == null) {
			stateRewards[r] = new double[numStates];
			importer.extractStateRewards(r, (i, d) -> storeReward(r, i, d));
		}
		return stateRewards[r][s];
	}
}
