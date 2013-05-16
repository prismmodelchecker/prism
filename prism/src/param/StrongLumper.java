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
import java.util.ListIterator;
import java.util.Map.Entry;

final class StrongLumper extends Lumper {

	StrongLumper(MutablePMC origPmc) {
		super(origPmc);
	}

	private HashMap<HashSet<Integer>, Function> stateSignature(int state)
	{
		HashMap<HashSet<Integer>, Function> signature = new HashMap<HashSet<Integer>, Function>();
		ListIterator<Integer> toStateIter = origPmc.transitionTargets.get(state).listIterator();
		ListIterator<Function> toProbIter = origPmc.transitionProbs.get(state).listIterator();
		while (toStateIter.hasNext()) {
			int toState = toStateIter.next();
			Function toStateProb = toProbIter.next();
			HashSet<Integer> toBlock = partition.getStateBlock(toState);
			Function toBlockProb = signature.get(toBlock);
			if (toBlockProb == null) {
				toBlockProb = origPmc.getFunctionFactory().getZero();
			}
			toBlockProb = toBlockProb.add(toStateProb);
			signature.put(toBlock, toBlockProb);
		}
		return signature;
	}
	
	@Override
	protected void refineBlock(HashSet<Integer> oldBlock, ArrayList<HashSet<Integer>> newBlocks) {
		HashMap<HashMap<HashSet<Integer>, Function>, HashSet<Integer>> signatures = new HashMap<HashMap<HashSet<Integer>, Function>, HashSet<Integer>>();
		for (int state : oldBlock) {
			HashMap<HashSet<Integer>, Function> signature = stateSignature(state);
			HashSet<Integer> newBlock = signatures.get(signature);
			if (newBlock == null) {
				newBlock = new HashSet<Integer>();
				signatures.put(signature, newBlock);
			}
			newBlock.add(state);
		}
		for (HashSet<Integer> block : signatures.values()) {
			newBlocks.add(block);
		}
	}

	@Override
	protected void buildQuotient() {
		optPmc = new MutablePMC(origPmc.getFunctionFactory(), blocks.size(), origPmc.isUseRewards(), origPmc.isUseTime());
		for (int newState = 0; newState < blocks.size(); newState++) {
			HashSet<Integer> fromBlock = blocks.get(newState);
			int someOldState = fromBlock.iterator().next();
			HashMap<HashSet<Integer>, Function> signature = stateSignature(someOldState);
			for (Entry<HashSet<Integer>, Function> entry : signature.entrySet()) {
				optPmc.addTransition(newState, blockToNumber.get(entry.getKey()), entry.getValue());
			}
			if (origPmc.isUseRewards()) {
				optPmc.setReward(newState, origPmc.getReward(someOldState));
				if (optPmc.getReward(newState).equals(origPmc.getFunctionFactory().getZero())) {
					optPmc.setTargetState(newState, true);					
				}
				if (origPmc.isUseTime()) {
					optPmc.setTime(newState, origPmc.getTime(someOldState));
				}
			} else {
				optPmc.setTargetState(newState, origPmc.isTargetState(someOldState));
			}
			for (int oldState : fromBlock) {
				if (origPmc.isInitState(oldState)) {
					optPmc.setInitState(newState, true);
				}
			}
		}
	}

}
