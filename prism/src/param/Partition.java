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
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

final class Partition {
	final class SmallFirstComparator implements Comparator<HashSet<Integer>> {
		@Override
		public int compare(HashSet<Integer> o1, HashSet<Integer> o2) {
			int size1 = o1.size();
			int size2 = o2.size();
			// TODO should actually be other way round, but slower then... ??
			if (size1 < size2) {
				return 1;
			} else if (size1 == size2) {
				return 0;
			} else {
				return -1;
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof SmallFirstComparator;
		}
		
		@Override
		public int hashCode() {
			return 0;
		}
	}

	private MutablePMC pmc;
	private HashSet<HashSet<Integer>> blocks;
	private ArrayList<HashSet<Integer>> stateToBlock;
	private PriorityQueue<HashSet<Integer>> mayChange;
	private HashSet<HashSet<Integer>> mayChangeHash;
	private HashSet<Integer> nextBlock;

	Partition(MutablePMC pmc)
	{
		this.pmc = pmc;
		blocks = new HashSet<HashSet<Integer>>();
		stateToBlock = new ArrayList<HashSet<Integer>>(pmc.getNumStates());
		HashSet<Integer> initialBlock = new HashSet<Integer>(1);
		mayChange = new PriorityQueue<HashSet<Integer>>(11, new SmallFirstComparator());
		mayChangeHash = new HashSet<HashSet<Integer>>();
		for (int state = 0; state < pmc.getNumStates(); state++) {
			initialBlock.add(state);
			stateToBlock.add(initialBlock);
		}
		blocks.add(initialBlock);
		mayChange.add(initialBlock);
		mayChangeHash.add(initialBlock);
	}

	HashSet<Integer> nextChangeableBlock()
	{
		nextBlock = mayChange.poll();
		mayChangeHash.remove(nextBlock);
		blocks.remove(nextBlock);
		return nextBlock;
	}

	void addBlocks(ArrayList<HashSet<Integer>> newBlocks)
	{
		blocks.addAll(newBlocks);
		
		for (HashSet<Integer> block : newBlocks) {
			for (int state : block) {
				stateToBlock.set(state, block);
			}
			if (block.equals(nextBlock)) {
				return;
			}
			mayChange.add(block);
			mayChangeHash.add(block);
		}
		for (HashSet<Integer> block : newBlocks) {
			for (int state : block) {		
				for (int predec : pmc.incoming.get(state)) {
					HashSet<Integer> predecBlock = stateToBlock.get(predec);
					if (!nextBlock.contains(predec) && !mayChangeHash.contains(predecBlock) && (predecBlock.size() > 1)) {
						mayChange.add(predecBlock);
						mayChangeHash.add(predecBlock);
					}
				}
			}
		}
	}

	boolean mayChange()
	{
		return !mayChange.isEmpty();
	}
	
	ArrayList<HashSet<Integer>> getAllBlocks()
	{
		ArrayList<HashSet<Integer>> allBlocks = new ArrayList<HashSet<Integer>>(blocks.size());
		for (HashSet<Integer> block : blocks) {
			allBlocks.add(block);
		}
		
		return allBlocks;
	}
	
	HashSet<Integer> getStateBlock(int state)
	{
		return stateToBlock.get(state);
	}
	
	void markAllBlocksAsNew()
	{
		mayChange.clear();
		mayChangeHash.clear();
		for (HashSet<Integer> block : blocks) {
			mayChange.add(block);
			mayChangeHash.add(block);
		}
	}
}
