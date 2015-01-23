//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
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

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.State;
import parser.Values;
import parser.VarList;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

/**
 * Class for creating a sub-model of any NondetModel, please note the translate* methods
 * used to translate between state ids for model and sub-model. Created sub-model will have new 
 * state numbering from 0 to number of states in the sub model.
 */
public class SubNondetModel implements NondetModel
{

	private NondetModel model = null;
	private BitSet states = null;
	private Map<Integer, BitSet> actions = null;
	private BitSet initialStates = null;
	private List<State> statesList = null;
	private Map<Integer, Integer> stateLookupTable = new HashMap<Integer, Integer>();
	private Map<Integer, Map<Integer, Integer>> actionLookupTable = new HashMap<Integer, Map<Integer, Integer>>();
	private Map<Integer, Integer> inverseStateLookupTable = new HashMap<Integer, Integer>();

	private int numTransitions = 0;
	private int maxNumChoices = 0;
	private int numChoices = 0;

	public SubNondetModel(NondetModel model, BitSet states, Map<Integer, BitSet> actions, BitSet initialStates)
	{
		this.model = model;
		this.states = states;
		this.actions = actions;
		this.initialStates = initialStates;

		generateStatistics();
		generateLookupTable(states, actions);
	}

	@Override
	public ModelType getModelType()
	{
		return model.getModelType();
	}

	@Override
	public int getNumStates()
	{
		return states.cardinality();
	}

	@Override
	public int getNumInitialStates()
	{
		return initialStates.cardinality();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		List<Integer> is = new ArrayList<Integer>();
		for (int i = initialStates.nextSetBit(0); i >= 0; i = initialStates.nextSetBit(i + 1)) {
			is.add(translateState(i));
		}

		return is;
	}

	@Override
	public int getFirstInitialState()
	{
		return translateState(initialStates.nextSetBit(0));
	}

	@Override
	public boolean isInitialState(int i)
	{
		return initialStates.get(translateState(i));
	}

	@Override
	public int getNumDeadlockStates()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getFirstDeadlockState()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<State> getStatesList()
	{
		// We use lazy generation because in many cases the state list is not needed
		if (statesList == null) {
			statesList = generateSubStateList(states);
		}
		return statesList;
	}

	private List<State> generateSubStateList(BitSet states)
	{
		List<State> statesList = new ArrayList<State>();
		for (int i = 0; i < model.getNumStates(); i++) {
			if (states.get(i)) {
				statesList.add(model.getStatesList().get(i));
			}
		}
		return statesList;
	}

	@Override
	public Values getConstantValues()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<String> getLabels() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int s)
	{
		s = translateState(s);
		HashSet<Integer> succs = new HashSet<Integer>();
		for (int i = 0; i < model.getNumChoices(s); i++) {
			if (actions.get(s).get(i)) {
				Iterator<Integer> it = model.getSuccessorsIterator(s, i);
				while (it.hasNext()) {
					int j = it.next();
					succs.add(inverseTranslateState(j));
				}
			}
		}
		return succs.iterator();
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		s1 = translateState(s1);
		s2 = translateState(s2);
		return model.isSuccessor(s1, s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog log)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark, boolean showStates)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int strat[])
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportStates(int exportType, VarList varList, PrismLog log) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String infoString()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String infoStringTable()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumChoices(int s)
	{
		s = translateState(s);
		return actions.get(s).cardinality();
	}

	@Override
	public int getMaxNumChoices()
	{
		return maxNumChoices;
	}

	@Override
	public int getNumChoices()
	{
		return numChoices;
	}

	@Override
	public Object getAction(int s, int i)
	{
		int sOriginal = translateState(s);
		int iOriginal = translateAction(s, i);

		return model.getAction(sOriginal, iOriginal);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		throw new RuntimeException("Not implemented");
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		int sOriginal = translateState(s);
		int iOriginal = translateAction(s, i);
		return model.getNumTransitions(sOriginal, iOriginal);
	}
	
	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		Iterator<Integer> successors = getSuccessorsIterator(s,i);
		while (successors.hasNext()) {
			Integer successor = successors.next();
			if (!set.get(successor)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		Iterator<Integer> successors = getSuccessorsIterator(s,i);
		while (successors.hasNext()) {
			Integer successor = successors.next();
			if (set.get(successor)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int s, int i)
	{
		int sOriginal = translateState(s);
		int iOriginal = translateAction(s, i);
		List<Integer> succ = new ArrayList<Integer>();
		Iterator<Integer> it = model.getSuccessorsIterator(sOriginal, iOriginal);
		while (it.hasNext()) {
			int j = it.next();
			succ.add(inverseTranslateState(j));
		}
		return succ.iterator();
	}

	private BitSet translateSet(BitSet set)
	{
		BitSet translatedBitSet = new BitSet();
		for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
			translatedBitSet.set(translateState(i));
		}
		return translatedBitSet;
	}

	private void generateStatistics()
	{
		for (int i = 0; i < model.getNumStates(); i++) {
			if (states.get(i)) {
				numTransitions += getTransitions(i);
				numChoices += actions.get(i).cardinality();
				maxNumChoices = Math.max(maxNumChoices, model.getNumChoices(i));
			}
		}
	}

	private int getTransitions(int state)
	{
		int transitions = 0;
		for (int i = 0; i < model.getNumChoices(state); i++) {
			if (actions.get(state).get(i)) {
				transitions += model.getNumTransitions(state, i);
			}
		}
		return transitions;
	}

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		throw new RuntimeException("Not implemented");
	}

	private void generateLookupTable(BitSet states, Map<Integer, BitSet> actions)
	{
		for (int i = 0; i < model.getNumStates(); i++) {
			if (states.get(i)) {
				inverseStateLookupTable.put(i, stateLookupTable.size());
				stateLookupTable.put(stateLookupTable.size(), i);
				Map<Integer, Integer> r = new HashMap<Integer, Integer>();
				for (int j = 0; j < model.getNumChoices(i); j++) {
					if (actions.get(i).get(j)) {
						r.put(r.size(), j);
					}
				}
				actionLookupTable.put(actionLookupTable.size(), r);
			}
		}
	}

	public int translateState(int s)
	{
		return stateLookupTable.get(s);
	}

	private int inverseTranslateState(int s)
	{
		return inverseStateLookupTable.get(s);
	}

	public int translateAction(int s, int i)
	{
		return actionLookupTable.get(s).get(i);
	}
}
