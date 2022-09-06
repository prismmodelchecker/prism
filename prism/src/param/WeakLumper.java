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

/**
 * Weak bisimulation lumper.
 * Notice that weak bisimulation is only valid for unbounded reachability,
 * but must not be used for expected accumulated rewards or long-run
 * average rewards.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class WeakLumper extends Lumper {

	/**
	 * Construct a new weak bisimulation lumper.
	 * 
	 * @param origPmc Markov chain to construct lumper for
	 */
	WeakLumper(MutablePMC origPmc) {
		super(origPmc);
	}
	
	/**
	 * Construct the weak bisimulation signature of given block.
	 * The signature is a mapping of blocks to the probability to move
	 * from the given state to any state of the block under the condition
	 * that the block is left. Thus, it is only defined for states which have
	 * a non-zero probability of leaving the block in one step. The function
	 * returns {@code null} for states ("silent" states) for which this is
	 * not the case.
	 * 
	 * @param state state to compute signature of
	 * @return signature of this state
	 */
	private HashMap<HashSet<Integer>, Function> stateSignature(int state, HashSet<Integer> ownClass)
	{
		HashMap<HashSet<Integer>, Function> signature = new HashMap<HashSet<Integer>, Function>();
		ListIterator<Integer> toStateIter = origPmc.transitionTargets.get(state).listIterator();
		ListIterator<Function> toProbIter = origPmc.transitionProbs.get(state).listIterator();

		/* compute probability to remain in block in one step */
		Function slProb = origPmc.getFunctionFactory().getZero();
		while (toStateIter.hasNext()) {
			int toState = toStateIter.next();
			Function toStateProb = toProbIter.next();
			if (ownClass.contains(toState)) {
				slProb = slProb.add(toStateProb);
			}
		}
		/* for states which cannot leave their block directly, return {@code null} */
		if (slProb.equals(origPmc.getFunctionFactory().getOne())) {
			return null;
		}
		/* 1 / (1 - slProb) */
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
	
	/**
	 * Refines a given block to a list of new blocks for weak bisimulation.
	 * New blocks are as follows: some of the new blocks consist of the
	 * states which can leave their block ("non-silent" states) and which
	 * have the same signature. In addition, such a block contains the states
	 * which cannot leave their block in one step ("silent" states) and which
	 * can only reach states of this particular new block. Other blocks
	 * consist of silent states which can reach more than one particular
	 * signature block. For these kind of blocks, we have a new block for
	 * each combination of new blocks they might reach. For instance, if
	 * there are new blocks (based on a signature) A,B,C, we add blocks
	 * {A,B},{B,C} and {A,B,C}, containing silent states which can reach
	 * A and B / B and C / A, B and C. 
	 * 
	 * @param oldBlock block to refine
	 * @param newBlocks list of new blocks generated
	 */
	@Override
	protected void refineBlock(HashSet<Integer> oldBlock,
			ArrayList<HashSet<Integer>> newBlocks) {
		ArrayList<Integer> nonSilent = new ArrayList<Integer>(oldBlock.size());
		HashSet<Integer> silent = new HashSet<Integer>();
		HashMap<HashMap<HashSet<Integer>, Function>, HashSet<Integer>> signatures = new HashMap<HashMap<HashSet<Integer>, Function>, HashSet<Integer>>();
		HashMap<Integer, HashSet<Integer>> stateToBlock = new HashMap<Integer, HashSet<Integer>>();
		/* compute signatures of states of old block and divide into silent/
		 * nonsilent states. Silent states are states which cannot leave the
		 * block in one step. */
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
		
		/* non-silent states reach only the new block they are contained in */
		HashMap<Integer, HashSet<HashSet<Integer>>> reachWhichBlocks = new HashMap<Integer, HashSet<HashSet<Integer>>>();
		for (int state : oldBlock) {
			HashSet<HashSet<Integer>> predReachBlocks = new HashSet<HashSet<Integer>>();
			if (!silent.contains(state)) {
				predReachBlocks.add(stateToBlock.get(state));
			}
			reachWhichBlocks.put(state, predReachBlocks);
		}
		
		/* collect all silent states which can reach a particular
		 * non-silent state by performing a backwards depth-first search.
		 * Mark silent states one comes across with the block of the
		 * non-silent state. We can already stop the search if we know
		 * that the state has previously been visited from another
		 * state from the same block. */
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
		/* compute new blocks, add the nonempty ones to list of new blocks */
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

	/**
	 * Build the weak bisimulation quotient from the blocks computed.
	 * Transition probabilities are basically based on the weak bisimulation
	 * signature. However, we must take care that we use a non-silent state
	 * to compute transition probabilties from. Also, states which can never
	 * leave their block after an arbitrary number of steps ("divergent"
	 * states) must lead to adding a self loop in their containing block.
	 */
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
	
	/**
	 * Creates an initial partitioning.
	 * This function is based on the of the {@code Lumper} class. However,
	 * for the weak bisimulation lumping to work correctly, for each block
	 * of the initial partitioning, we have to split off "divergent" states.
	 * Divergent states are states which can never leave their block, after
	 * any number of steps.
	 */
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
