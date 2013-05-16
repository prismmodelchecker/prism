//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

abstract class Lumper {
	/**
	 * Kind of lumping to be used.
	 */
	enum BisimType {
		NULL,
		STRONG,
		WEAK
	}
	
	/** stores the original model to be lumped */
	protected MutablePMC origPmc;
	/** stores the lumped model */
	protected MutablePMC optPmc;
	/** stores the state partitioning */
	protected Partition partition;
	/** maintains a mapping from blocks to block numbers */
	protected HashMap<HashSet<Integer>, Integer> blockToNumber;
	/** list of all blocks */
	protected ArrayList<HashSet<Integer>> blocks;
	/** maps states from original to containing states in lumped model */
	protected int[] originalToOptimised;

	Lumper(MutablePMC origPmc)
	{
		this.origPmc = origPmc;
		partition = new Partition(origPmc);
		createInitialPartition();
		lump();
		mapBlocksToNumber();
		buildQuotient();
	}

	protected Lumper()
	{
		
	}

	class RewardEntry
	{
		final Function reward;
		final Function time;
		
		RewardEntry(Function reward, Function time) {
			this.reward = reward;
			this.time = time;
		}
		
		@Override
		public int hashCode()
		{
			int hash = reward.hashCode() << 13;
			if (time != null) {
				hash |= time.hashCode();
			}
			return hash;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof RewardEntry)) {
				return false;
			}
			
			RewardEntry other = (RewardEntry) obj;
			if (!this.reward.equals(other.reward)) {
				return false;
			}
			if ((this.time == null) != (other.time == null)) {
				return false;
			}
			if (this.time != null && !this.time.equals(other.time)) {
				return false;
			}
			return true;
		}
	}
	
	protected void createInitialPartition()
	{
//		while (partition.mayChange()) {
			HashSet<Integer> oldBlock = partition.nextChangeableBlock();
			if (!origPmc.isUseRewards()) {
				HashSet<Integer> safeStates = new HashSet<Integer>();
				HashSet<Integer> unsafeStates = new HashSet<Integer>();
				for (int state : oldBlock) {
					if (origPmc.isTargetState(state)) {
						unsafeStates.add(state);
					} else {
						safeStates.add(state);
					}
				}
				ArrayList<HashSet<Integer>> newBlocks = new ArrayList<HashSet<Integer>>(); 
				if (safeStates.size() != 0) {
					newBlocks.add(safeStates);
				}
				if (unsafeStates.size() != 0) {
					newBlocks.add(unsafeStates);
				}
				partition.addBlocks(newBlocks);
			} else {
				HashMap<RewardEntry, HashSet<Integer>> rewardToStateMap = new HashMap<RewardEntry, HashSet<Integer>>();
				for (int state : oldBlock) {
					Function reward = origPmc.getReward(state);
					Function time = null;
					if (origPmc.isUseTime()) {
						time = origPmc.getTime(state);
					}
					RewardEntry entry = new RewardEntry(reward, time);
					HashSet<Integer> block = rewardToStateMap.get(entry);
					if (block == null) {
						block = new HashSet<Integer>();
						rewardToStateMap.put(entry, block);
					}
					block.add(state);
				}
				ArrayList<HashSet<Integer>> newBlocks = new ArrayList<HashSet<Integer>>(); 
				for (HashSet<Integer> block : rewardToStateMap.values()) {
					if (block.size() != 0) {
						newBlocks.add(block);
					}
				}
				partition.addBlocks(newBlocks);
			}
//		}
			partition.markAllBlocksAsNew();
	}
	
	MutablePMC getOptimised()
	{
		return optPmc;
	}
	
	protected void mapBlocksToNumber()
	{
		blocks = partition.getAllBlocks();
		blockToNumber = new HashMap<HashSet<Integer>, Integer>();
		originalToOptimised = new int[origPmc.getNumStates()];
		int nextBlockNumber = 0;
		for (HashSet<Integer> block : blocks) {
			blockToNumber.put(block, nextBlockNumber);
			for (int state : block) {
				originalToOptimised[state] = nextBlockNumber;
			}
			nextBlockNumber++;
		}
	}
	
	int[] getOriginalToOptimised()
	{
		return originalToOptimised;
	}
	
	protected void lump()
	{
		ArrayList<HashSet<Integer>> newBlocks = new ArrayList<HashSet<Integer>>();
		while (partition.mayChange()) {
			newBlocks.clear();
			HashSet<Integer> oldBlock = partition.nextChangeableBlock();
			refineBlock(oldBlock, newBlocks);
			partition.addBlocks(newBlocks);
		}
	}
	
	abstract protected void refineBlock(HashSet<Integer> oldBlock, ArrayList<HashSet<Integer>> newBlocks);
	abstract protected void buildQuotient();
}
