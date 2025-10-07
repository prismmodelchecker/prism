//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <david.parker@cs.bham.ac.uk> (University of Birmingham)
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import parser.State;
import parser.VarList;
import parser.ast.DeclarationType;
import parser.type.Type;
import prism.ModelGenerator;
import prism.ModelInfo;
import prism.ModelType;
import prism.Pair;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Class that implements the ModelGenerator interface using an explicit model.
 * Allows e.g. simulation of a built model.
 */
public class ModelModelGenerator<Value> implements ModelGenerator<Value>
{
	// Explicit model + info
	private Model<Value> model;
	private ModelInfo modelInfo;
	
	/** Index of the state current being explored */
	private int sExplore = -1;
	
	// Temporary storage of transitions for a state
	
	private class Transitions
	{
		Object choiceAction;
		List<Integer> succs;
		List<Value> probs;
		List<Object> transActions;

		public Transitions()
		{
			choiceAction = null;
			succs = new ArrayList<>();
			probs = new ArrayList<>();
			transActions = null;
		}
	}
	
	private List<Transitions> trans = new ArrayList<>();
	
	public ModelModelGenerator(Model<Value> model, ModelInfo modelInfo)
	{
		this.model = model;
		this.modelInfo = modelInfo; 
	}
	
	@Override
	public ModelType getModelType()
	{
		return model.getModelType();
	}

	@Override
	public List<String> getVarNames()
	{
		return modelInfo.getVarNames();
	}

	@Override
	public List<Type> getVarTypes()
	{
		return modelInfo.getVarTypes();
	}

	@Override
	public DeclarationType getVarDeclarationType(int i) throws PrismException
	{
		return modelInfo.getVarDeclarationType(i);
	}

	@Override
	public int getVarModuleIndex(int i)
	{
		return modelInfo.getVarModuleIndex(i);
	}
	
	@Override
	public String getModuleName(int i)
	{
		return modelInfo.getModuleName(i);
	}
	
	@Override
	public VarList createVarList() throws PrismException
	{
		return modelInfo.createVarList();
	}

	@Override
	public List<Object> getActions()
	{
		return model.getActions();
	}

	@Override
	public State getInitialState() throws PrismException
	{
		int sInitial = model.getFirstInitialState();
		return model.getStatesList().get(sInitial);
	}

	@Override
	public void exploreState(State exploreState) throws PrismException
	{
		// Look up index of state to explore
		sExplore = model.getStatesList().indexOf(exploreState);
		// Extract transitions and store 
		trans.clear();
		switch (model.getModelType()) {
		case CTMC:
			storeTransitionsAndActions(((CTMC<Value>) model).getTransitionsAndActionsIterator(sExplore));
			break;
		case DTMC:
			storeTransitionsAndActions(((DTMC<Value>) model).getTransitionsAndActionsIterator(sExplore));
			break;
		case MDP:
			int numChoices = ((MDP<Value>) model).getNumChoices(sExplore);
			for (int i = 0; i < numChoices; i++) {
				Object action = ((MDP<Value>) model).getAction(sExplore, i);
				storeTransitions(action, ((MDP<Value>) model).getTransitionsIterator(sExplore, i));
			}
			break;
		case CTMDP:
		case LTS:
		case PTA:
		case SMG:
		default:
			throw new PrismNotSupportedException("Model generation not supported for " + model.getModelType() + "s");
		}
	}

	/**
	 * Store the transitions extracted from an action and Model-provided iterator.
	 */
	private void storeTransitions(Object choiceAction, Iterator<Map.Entry<Integer, Value>> transitionsIterator)
	{
		Transitions t = new Transitions();
		t.choiceAction = choiceAction;
		while (transitionsIterator.hasNext()) {
			Map.Entry<Integer, Value> e = transitionsIterator.next();
			t.succs.add(e.getKey());
			t.probs.add(e.getValue());
		}
		trans.add(t);
	}

	private void storeTransitionsAndActions(Iterator<java.util.Map.Entry<Integer, Pair<Value, Object>>> transitionsAndActionsIterator)
	{
		Transitions t = new Transitions();
		t.transActions = new ArrayList<>();
		while (transitionsAndActionsIterator.hasNext()) {
			Map.Entry<Integer, Pair<Value, Object>> e = transitionsAndActionsIterator.next();
			t.succs.add(e.getKey());
			t.probs.add(e.getValue().first);
			t.transActions.add(e.getValue().second);
		}
		trans.add(t);
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		return trans.size();
	}

	@Override
	public int getNumTransitions(int i) throws PrismException
	{
		return trans.get(i).succs.size();
	}

	@Override
	public Object getChoiceAction(int i) throws PrismException
	{
		return trans.get(i).choiceAction;
	}

	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException
	{
		List<Object> transActions = trans.get(i).transActions;
		return transActions == null ? null : transActions.get(offset);
	}

	@Override
	public Value getTransitionProbability(int i, int offset) throws PrismException
	{
		return trans.get(i).probs.get(offset);
	}

	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException
	{
		return model.getStatesList().get(trans.get(i).succs.get(offset));
	}
}
