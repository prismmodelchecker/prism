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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

final class WeakLumper extends Lumper {

	WeakLumper(MutablePMC origPmc) {
		super(origPmc);
	}
	
	private HashMap<HashSet<Integer>, Function> stateSignature(int state, HashSet<Integer> ownClass)
	{
		HashMap<HashSet<Integer>, Function> signature = new HashMap<HashSet<Integer>, Function>();
		ListIterator<Integer> toStateIter = origPmc.transitionTargets.get(state).listIterator();
		ListIterator<Function> toProbIter = origPmc.transitionProbs.get(state).listIterator();

		Function slProb = origPmc.getFunctionFactory().getZero();
		while (toStateIter.hasNext()) {
			int toState = toStateIter.next();
			Function toStateProb = toProbIter.next();
			if (ownClass.contains(toState)) {
				slProb = slProb.add(toStateProb);
			}
		}
		if (slProb.equals(origPmc.getFunctionFactory().getOne())) {
			return null;
		}
		Function star = slProb.star();

		toStateIter = origPmc.transitionTargets.get(state).listIterator();
		toProbIter = origPmc.transitionProbs.get(state).listIterator();
		while (toStateIter.hasNext()) {
			int toState = toStateIter.next();
			Function toStateProb = toProbIter.next();
			HashSet<Integer> toBlock = partition.getStateBlock(toState);
			if (ownClass != toBlock) {
				toStateProb = star.multiply(toStateProb);
				Function toBlockProb = signature.get(toBlock);
				if (toBlockProb == null) {
					toBlockProb = origPmc.getFunctionFactory().getZero();
				}
				toBlockProb = toBlockProb.add(toStateProb);
				signature.put(toBlock, toBlockProb);
			}
		}
		return signature;
	}
	
	@Override
	protected void refineBlock(HashSet<Integer> oldBlock,
			ArrayList<HashSet<Integer>> newBlocks) {
		ArrayList<Integer> nonSilent = new ArrayList<Integer>(oldBlock.size());
		HashSet<Integer> silent = new HashSet<Integer>();
		HashMap<HashMap<HashSet<Integer>, Function>, HashSet<Integer>> signatures = new HashMap<HashMap<HashSet<Integer>, Function>, HashSet<Integer>>();
		HashMap<Integer, HashSet<Integer>> stateToBlock = new HashMap<Integer, HashSet<Integer>>(); 
		for (int state : oldBlock) {
			HashMap<HashSet<Integer>, Function> signature = stateSignature(state, oldBlock);
			if (signature != null) {
				nonSilent.add(state);
				HashSet<Integer> newBlock = signatures.get(signature);
				if (newBlock == null) {
					newBlock = new HashSet<Integer>();
					signatures.put(signature, newBlock);
				}
				newBlock.add(state);
				stateToBlock.put(state, newBlock);
			} else {
				silent.add(state);
			}
		}
		
		HashMap<Integer, HashSet<HashSet<Integer>>> reachWhichBlocks = new HashMap<Integer, HashSet<HashSet<Integer>>>();
		for (int state : oldBlock) {
			HashSet<HashSet<Integer>> predReachBlocks = new HashSet<HashSet<Integer>>();
			if (!silent.contains(state)) {
				predReachBlocks.add(stateToBlock.get(state));
			}
			reachWhichBlocks.put(state, predReachBlocks);
		}
		
		for (int state : nonSilent) {
			HashSet<Integer> block = stateToBlock.get(state);
			ArrayDeque<Integer> stack = new ArrayDeque<Integer>();
			stack.push(state);
			while (!stack.isEmpty()) {
				int stackState = stack.pop();
				for (int predState : origPmc.incoming.get(stackState)) {
					HashSet<HashSet<Integer>> predReachBlocks = reachWhichBlocks.get(predState);
					if (oldBlock.contains(predState) && silent.contains(predState) && !predReachBlocks.contains(block)) {
						predReachBlocks.add(block);
						stack.push(predState);
					}
				}
			}
		}
		HashMap<HashSet<HashSet<Integer>>, HashSet<Integer>> remap = new HashMap<HashSet<HashSet<Integer>>, HashSet<Integer>>();
		for (Entry<Integer, HashSet<HashSet<Integer>>> entry : reachWhichBlocks.entrySet()) {
			HashSet<Integer> sigStates = remap.get(entry.getValue());
			if (sigStates == null) {
				sigStates = new HashSet<Integer>();
				remap.put(entry.getValue(), sigStates);
			}
			sigStates.add(entry.getKey());
		}
		for (HashSet<Integer> block : remap.values()) {
			if (!block.isEmpty()) {
				newBlocks.add(block);
			}
		}
	}

	@Override
	protected void buildQuotient() {
		optPmc = new MutablePMC(origPmc.getFunctionFactory(), blocks.size(), origPmc.isUseRewards(), false);
		for (int newState = 0; newState < blocks.size(); newState++) {
			HashMap<HashSet<Integer>, Function> signature = null;
			int oldState = -1;
			HashSet<Integer> fromBlock = blocks.get(newState);
			Iterator<Integer> iter = fromBlock.iterator();
			while (iter.hasNext()) {
				oldState = iter.next();
				signature = stateSignature(oldState, fromBlock);
				if (signature != null) {
					break;
				}
			}
			if (signature == null) {
				optPmc.addTransition(newState, newState, origPmc.getFunctionFactory().getOne());				
			} else {
				for (Entry<HashSet<Integer>, Function> entry : signature.entrySet()) {
					optPmc.addTransition(newState, blockToNumber.get(entry.getKey()), entry.getValue());
				}
			}
			if (origPmc.isUseRewards()) {
				optPmc.setReward(newState, origPmc.getReward(oldState));
			} else {
				optPmc.setTargetState(newState, origPmc.isTargetState(oldState));
			}
		}
	}
	
	@Override
	protected void createInitialPartition() {
		super.createInitialPartition();
		ArrayList<HashSet<Integer>> newBlocks = new ArrayList<HashSet<Integer>>();
		while (partition.mayChange()) {
			HashSet<Integer> oldBlock = partition.nextChangeableBlock();
			HashSet<Integer> leaveSet = new HashSet<Integer>();
			ArrayList<Integer> directLeaving = new ArrayList<Integer>();
			for (int state : oldBlock) {
				for (int toState : origPmc.transitionTargets.get(state)) {
					if (!oldBlock.contains(toState)) {
						leaveSet.add(state);
						directLeaving.add(state);
						break;
					}
				}
			}
			for (int state : directLeaving) {
				ArrayDeque<Integer> stack = new ArrayDeque<Integer>();
				stack.push(state);
				while (!stack.isEmpty()) {
					int leaving = stack.pop();
					for (int inState : origPmc.incoming.get(leaving)) {
						if (oldBlock.contains(inState) && !leaveSet.contains(inState)) {
							leaveSet.add(inState);
							stack.push(inState);
						}
					}
				}
			}
			if (!leaveSet.isEmpty()) {
				newBlocks.add(leaveSet);
			}
			HashSet<Integer> staying = new HashSet<Integer>(oldBlock);
			staying.removeAll(leaveSet);
			if (!staying.isEmpty()) {
				newBlocks.add(staying);
			}
		}
		partition.addBlocks(newBlocks);
		partition.markAllBlocksAsNew();
	}
}
