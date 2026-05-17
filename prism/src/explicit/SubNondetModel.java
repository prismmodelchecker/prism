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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.IterableStateSet;
import io.ModelExportOptions;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.ActionList;
import prism.ActionListOwner;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

/**
 * Class for creating a sub-model of any NondetModel, please note the translate* methods
 * used to translate between state ids for model and sub-model. Created sub-model will have new 
 * state numbering from 0 to number of states in the sub model.
 */
public class SubNondetModel<Value> implements NondetModel<Value>, ActionListOwner
{

	private NondetModel<Value> model = null;
	private ActionList actionList;
	private BitSet states = null;
	private BitSet initialStates = null;
	private List<State> statesList = null;

	/** sub-index → original state index */
	private int[] subToOrig;
	/** original state index → sub-index (-1 if not in sub-model) */
	private int[] origToSub;
	/** sub-index → array of original action indices, in order */
	private int[][] subActionToOrig;
	/** cached state count */
	private int numSubStates;

	/**
	 * (Optionally) the stored predecessor relation. Becomes inaccurate after the model is changed!
	 */
	protected PredecessorRelation predecessorRelation;

	private int numTransitions = 0;
	private int maxNumChoices = 0;
	private int numChoices = 0;

	public SubNondetModel(NondetModel<Value> model, BitSet states, Map<Integer, BitSet> actions, BitSet initialStates)
	{
		this.model = model;
		actionList = new ActionList(this::findActionsUsed);
		this.states = states;
		this.initialStates = initialStates;
		init(states, actions);
		// actions is not stored; eligible for GC once this constructor returns
	}

	@Override
	public ModelType getModelType()
	{
		return model.getModelType();
	}

	@Override
	public ActionList getActionList()
	{
		return actionList;
	}

	@Override
	public List<Object> getActions()
	{
		return actionList.getActions();
	}

	@Override
	public int actionIndex(Object action)
	{
		return actionList.actionIndex(action);
	}

	@Override
	public int getNumStates()
	{
		return numSubStates;
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
		for (int i : new IterableStateSet(states, model.getNumStates())){
			statesList.add(model.getStatesList().get(i));
		}
		return statesList;
	}

	@Override
	public VarList getVarList()
	{
		// we can return the varList of the model, as we do not change
		// the variables in the model
		return model.getVarList();
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
	public boolean hasLabel(String name)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
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
	public void exportToPrismExplicitTra(PrismLog log, int precision)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismLanguage(String filename, int precision) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportStates(VarList varList, PrismLog out, ModelExportOptions exportOptions) throws PrismException
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
		return subActionToOrig[s].length;
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
	public int getNumTransitions(int s, int i)
	{
		int sOriginal = translateState(s);
		int iOriginal = translateAction(s, i);
		return model.getNumTransitions(sOriginal, iOriginal);
	}

	@Override
	public SuccessorsIterator getSuccessors(int s, int i)
	{
		int sOriginal = translateState(s);
		int iOriginal = translateAction(s, i);

		SuccessorsIterator it = model.getSuccessors(sOriginal, iOriginal);
		return new SuccessorsIterator() {

			@Override
			public boolean successorsAreDistinct()
			{
				return it.successorsAreDistinct();
			}

			@Override
			public boolean hasNext()
			{
				return it.hasNext();
			}

			@Override
			public int nextInt()
			{
				return inverseTranslateState(it.next());
			}

		};
	}

	private BitSet translateSet(BitSet set)
	{
		BitSet translatedBitSet = new BitSet();
		for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
			translatedBitSet.set(translateState(i));
		}
		return translatedBitSet;
	}

	/**
	 * Single-pass initialisation: build lookup arrays and accumulate statistics.
	 * Not stored after construction so the caller's actions map can be GC'd immediately.
	 */
	private void init(BitSet states, Map<Integer, BitSet> actions)
	{
		numSubStates = states.cardinality();
		origToSub = new int[model.getNumStates()];
		Arrays.fill(origToSub, -1);
		subToOrig = new int[numSubStates];
		subActionToOrig = new int[numSubStates][];

		int subIdx = 0;
		for (int origState : new IterableStateSet(states, model.getNumStates())) {
			origToSub[origState] = subIdx;
			subToOrig[subIdx] = origState;

			BitSet validActions = actions.get(origState);
			int na = validActions.cardinality();
			int[] origActions = new int[na];
			int j = 0;
			for (int a = validActions.nextSetBit(0); a >= 0; a = validActions.nextSetBit(a + 1)) {
				origActions[j++] = a;
			}
			subActionToOrig[subIdx] = origActions;

			numChoices += na;
			maxNumChoices = Math.max(maxNumChoices, na);
			for (int a : origActions) {
				numTransitions += model.getNumTransitions(origState, a);
			}

			subIdx++;
		}
	}

	@Override
	public Model<Value> constructInducedModel(MDStrategy<Value> strat)
	{
		throw new RuntimeException("Not implemented");
	}

	public int translateState(int s)
	{
		return subToOrig[s];
	}

	private int inverseTranslateState(int s)
	{
		return origToSub[s];
	}

	public int translateAction(int s, int i)
	{
		return subActionToOrig[s][i];
	}

	@Override
	public boolean hasStoredPredecessorRelation() {
		return (predecessorRelation != null);
	}

	@Override
	public PredecessorRelation getPredecessorRelation(prism.PrismComponent parent, boolean storeIfNew) {
		if (predecessorRelation != null) {
			return predecessorRelation;
		}

		PredecessorRelation pre = PredecessorRelation.forModel(parent, this);

		if (storeIfNew) {
			predecessorRelation = pre;
		}
		return pre;
	}

	@Override
	public void clearPredecessorRelation() {
		predecessorRelation = null;
	}
}
