//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package prism;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ExplicitFilesRewardGenerator extends PrismComponent implements RewardGenerator
{
	// File(s) to read in rewards from
	private File stateRewardsFile;
	// Model info
	private int numStates;
	// Local storage of rewards
	private boolean stateRewardsLoaded = false; 
	private double[] stateRewards;
	
	/**
	 * Constructor
	 */
	public ExplicitFilesRewardGenerator(PrismComponent parent)
	{
		super(parent);
	}

	/**
	 * Constructor
	 */
	public ExplicitFilesRewardGenerator(PrismComponent parent, File stateRewardsFile, int numStates) throws PrismException
	{
		this(parent);
		this.stateRewardsFile = stateRewardsFile;
		this.numStates = numStates;
	}

	/**
	 * Extract the state rewards from the file and store locally.
	 */
	private void extractStateRewards() throws PrismException
	{
		int lineNum = -1;
		stateRewards = new double[numStates];
		// open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(stateRewardsFile))) {
			// skip first line
			in.readLine();
			lineNum = 1;
			// read remaining lines
			String s = in.readLine();
			lineNum++;
			while (s != null) {
				// skip blank lines
				s = s.trim();
				if (s.length() > 0) {
					// split into two parts
					String[] ss = s.split(" ");
					// determine which state this line describes
					int i = Integer.parseInt(ss[0]);
					if (i < 0 || i >= numStates) {
						throw new PrismException("Invalid state index " + i);
					}
					// determine which state this line describes
					double d = Double.parseDouble(ss[1]);
					// store
					stateRewards[i] = d;
				}
				// read next line
				s = in.readLine();
				lineNum++;
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + stateRewardsFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of states file \"" + stateRewardsFile + "\"");
		}
	}
	
	// Methods to implement RewardGenerator
	
	@Override
	public List<String> getRewardStructNames()
	{
		return Collections.singletonList("");
	}
	
	@Override
	public boolean rewardStructHasTransitionRewards(int r)
	{
		return false;
	}
	
	@Override
	public boolean isRewardLookupSupported(RewardLookup lookup)
	{
		return lookup == RewardLookup.BY_STATE_INDEX;
	}
	
	@Override
	public double getStateReward(int r, int s) throws PrismException
	{
		if (r != 0) {
			throw new PrismException("Only one reward structure has been imported");
		}
		if (!stateRewardsLoaded) {
			extractStateRewards();
		}
		return stateRewards[s];
	}
}
