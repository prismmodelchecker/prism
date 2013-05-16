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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.ListIterator;

final class StateEliminator {
	enum EliminationOrder {
		ARBITRARY("arbitrary"),
		FORWARD("forward"),
		FORWARD_REVERSED("forward-reversed"),
		BACKWARD("backward"),
		BACKWARD_REVERSED("backward-reversed"),
		RANDOM("random");
	
		private String name;
		EliminationOrder(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	
	private MutablePMC pmc;
	private EliminationOrder eliminationOrder;
	
	StateEliminator(MutablePMC pmc, EliminationOrder eliminationOrder)
	{
		this.pmc = pmc;
		this.eliminationOrder = eliminationOrder;
	}
	
	private int[] collectStatesForward()
	{
		int[] states = new int[pmc.getNumStates()];
		BitSet seen = new BitSet(pmc.getNumStates());
		HashSet<Integer> current = new HashSet<Integer>();
		int nextStateNr = 0;
		for (int state = 0; state < pmc.getNumStates(); state++) {
			if (pmc.isInitState(state)) {
				states[nextStateNr] = state;
				seen.set(state, true);
				current.add(state);
				nextStateNr++;
			}
		}
		while (!current.isEmpty()) {
			HashSet<Integer> next = new HashSet<Integer>();
			for (int state : current) {
				for (int succState : pmc.transitionTargets.get(state)) {
					if (!seen.get(succState)) {
						seen.set(succState, true);
						next.add(succState);
						states[nextStateNr] = succState;
						nextStateNr++;
					}
				}
			}
			current = next;
		}
		return states;
	}
	
	private int[] collectStatesBackward()
	{
		int[] states = new int[pmc.getNumStates()];
		BitSet seen = new BitSet(pmc.getNumStates());
		HashSet<Integer> current = new HashSet<Integer>();
		int nextStateNr = 0;
		for (int state = 0; state < pmc.getNumStates(); state++) {
			if (pmc.isTargetState(state)) {
				current.add(state);
				states[nextStateNr] = state;
				seen.set(state, true);
				nextStateNr++;
			}
		}
		while (!current.isEmpty()) {
			HashSet<Integer> next = new HashSet<Integer>();
			for (int state : current) {
				for (int succState : pmc.incoming.get(state)) {
					if (!seen.get(succState)) {
						seen.set(succState, true);
						next.add(succState);
						states[nextStateNr] = succState;
						nextStateNr++;
					}
				}
			}
			current = next;
		}
		
		/* might not find all states when doing as above,
		 * so add missing ones */
		HashSet<Integer> allStates = new HashSet<Integer>();
		for (int stateNr = 0; stateNr < states.length; stateNr++) {
			int state = states[stateNr];
			allStates.add(state);
		}
		for (int state = 0; state < pmc.getNumStates(); state++) {
			if (!allStates.contains(state)) {
				states[nextStateNr] = state;
				nextStateNr++;
			}
		}
		
		return states;
	}
	
	private boolean precompute()
	{
		if (!pmc.isUseTime() && !pmc.hasTargetStates()) {
			for (int state = 0; state < pmc.getNumStates(); state++) {
				pmc.makeAbsorbing(state);
				if (pmc.isUseRewards()) {
					pmc.setReward(state, pmc.getFunctionFactory().getInf());
				}
			}
			return true;
		}

		if (pmc.isUseRewards()) {
			int[] backStatesArr = collectStatesBackward();
			HashSet<Integer> reaching = new HashSet<Integer>();
			for (int stateNr = 0; stateNr < backStatesArr.length; stateNr++) {
				reaching.add(backStatesArr[stateNr]);
			}
			for (int state = 0; state < pmc.getNumStates(); state++) {
				if (!pmc.isUseTime() && !reaching.contains(state)) {
					pmc.setReward(state, pmc.getFunctionFactory().getInf());
				} 
			}
		}
		
		return false;
	}
	
	void eliminate()
	{
		if (precompute()) {
			return;
		}
		
		int[] states = new int[pmc.getNumStates()];

		switch (eliminationOrder) {
		case ARBITRARY:
			for (int state = 0; state < pmc.getNumStates(); state++) {
				states[state] = state;
			}
			break;
		case FORWARD:
			states = collectStatesForward();
			break;
		case FORWARD_REVERSED:
			states = collectStatesForward();
			Collections.reverse(Arrays.asList(states));
			break;
		case BACKWARD:
			states = collectStatesBackward();
			break;
		case BACKWARD_REVERSED:
			states = collectStatesBackward();
			Collections.reverse(Arrays.asList(states));
			break;
		case RANDOM:
			for (int state = 0; state < pmc.getNumStates(); state++) {
				states[state] = state;
			}
			Collections.shuffle(Arrays.asList(states));
			break;
		default:
			throw new RuntimeException("unknown state elimination order");
		}
		
		for (int stateNr = 0; stateNr < pmc.getNumStates(); stateNr++) {
			eliminate(states[stateNr]);
		}
	}
			
	class NewTransition {
		final int fromState;
		final int toState;
		final Function prob;

		public NewTransition(int fromState, int toState, Function prob)
		{
			this.fromState = fromState;
			this.toState = toState;
			this.prob = prob;
		}
		
	}

	private void eliminate(int midState)
	{
		Function loopProb = pmc.getSelfLoopProb(midState);
		if (loopProb.equals(pmc.getFunctionFactory().getOne())) {
			return;
		}
		Function slStar = loopProb.star();
		
		if (pmc.isUseRewards()) {
			pmc.setReward(midState, pmc.getReward(midState).multiply(slStar));
			for (int from : pmc.incoming.get(midState)) {
				if (from != midState) {
					pmc.setReward(from, pmc.getReward(from).add(pmc.getTransProb(from, midState).multiply(pmc.getReward(midState))));
				}
			}
		}
		if (pmc.isUseTime()) {
			pmc.setTime(midState, pmc.getTime(midState).multiply(slStar));
			for (int from : pmc.incoming.get(midState)) {
				if (from != midState) {
					pmc.setTime(from, pmc.getTime(from).add(pmc.getTransProb(from, midState).multiply(pmc.getTime(midState))));
				}
			}
		}

		ArrayList<NewTransition> newTransitions = new ArrayList<NewTransition>(); 
		
		for (int fromState : pmc.incoming.get(midState)) {
			if (fromState != midState) {
				Function fromToMid = pmc.getTransProb(fromState, midState);
				ListIterator<Integer> toStateIter = pmc.transitionTargets.get(midState).listIterator();
				ListIterator<Function> toProbIter = pmc.transitionProbs.get(midState).listIterator();
				while (toStateIter.hasNext()) {
					int toState = toStateIter.next();
					Function midToTo = toProbIter.next();
					if (toState != midState) {
						Function fromToToAdd = fromToMid.multiply(slStar.multiply(midToTo));
						newTransitions.add(new NewTransition(fromState, toState, fromToToAdd));
					}
				}
			}
		}
				
		for (int fromState : pmc.incoming.get(midState)) {
			ListIterator<Integer> toStateIter = pmc.transitionTargets.get(fromState).listIterator();
			ListIterator<Function> toProbIter = pmc.transitionProbs.get(fromState).listIterator();
			while (toStateIter.hasNext()) {
				int state = toStateIter.next();
				toProbIter.next();
				if (state == midState) {
					toStateIter.remove();
					toProbIter.remove();
					break;
				}
			}
		}
		
		for (NewTransition newTransition : newTransitions) {
			pmc.addTransition(newTransition.fromState, newTransition.toState, newTransition.prob);
		}

		ListIterator<Integer> toStateIter = pmc.transitionTargets.get(midState).listIterator();
		ListIterator<Function> toProbIter = pmc.transitionProbs.get(midState).listIterator();
		while (toStateIter.hasNext()) {
			int toState = toStateIter.next();
			Function toProb = toProbIter.next();
			if (midState != toState) {
				toProbIter.set(slStar.multiply(toProb));
			}
		}
		toStateIter = pmc.transitionTargets.get(midState).listIterator();
		toProbIter = pmc.transitionProbs.get(midState).listIterator();
		while (toStateIter.hasNext()) {
			int toState = toStateIter.next();
			toProbIter.next();
			if (midState == toState) {
				toStateIter.remove();
				toProbIter.remove();
				break;
			}
		}
		
		pmc.incoming.get(midState).clear();
	}
	
	Function getResult(int state)
	{
		if (pmc.isUseRewards() && !pmc.isUseTime()) {
			return pmc.getReward(state);
		} else if (pmc.isUseRewards() && pmc.isUseTime()) {
			ListIterator<Integer> toStateIter = pmc.transitionTargets.get(state).listIterator();
			ListIterator<Function> toProbIter = pmc.transitionProbs.get(state).listIterator();
			Function result = pmc.getFunctionFactory().getZero();
			while (toStateIter.hasNext()) {
				int toState = toStateIter.next();
				Function toProb = toProbIter.next();
				result = result.add(toProb.multiply(pmc.getReward(toState)).divide(pmc.getTime(toState)));
			}
			return result;			
		} else {
			ListIterator<Integer> toStateIter = pmc.transitionTargets.get(state).listIterator();
			ListIterator<Function> toProbIter = pmc.transitionProbs.get(state).listIterator();
			Function result = pmc.getFunctionFactory().getZero();
			while (toStateIter.hasNext()) {
				int toState = toStateIter.next();
				Function toProb = toProbIter.next();
				if (pmc.isTargetState(toState)) {
					result = result.add(toProb);
				}
			}
			return result;
		}
	}
}
