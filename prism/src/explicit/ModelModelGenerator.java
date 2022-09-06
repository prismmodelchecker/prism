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
import parser.type.Type;
import prism.ModelGenerator;
import prism.ModelInfo;
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Class that implements the ModelGenerator interface using an explicit model.
 * Allows e.g. simulation of a built model.
 */
public class ModelModelGenerator implements ModelGenerator
{
	// Explicit model + info
	private Model model;
	private ModelInfo modelInfo;
	
	/** Index of the state current being explored */
	private int sExplore = -1;
	
	// Temporary storage of transitions for a state
	
	private class Transitions
	{
		Object action;
		List<Integer> succs;
		List<Double> probs;
		
		public Transitions()
		{
			action = null;
			succs = new ArrayList<>();
			probs = new ArrayList<>();
		}
	}
	
	private List<Transitions> trans = new ArrayList<>();
	
	public ModelModelGenerator(Model model, ModelInfo modelInfo)
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
	public VarList createVarList() throws PrismException
	{
		return modelInfo.createVarList();
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
			storeTransitions(null, ((CTMC) model).getTransitionsIterator(sExplore));
			break;
		case DTMC:
			storeTransitions(null, ((DTMC) model).getTransitionsIterator(sExplore));
			break;
		case MDP:
			int numChoices = ((MDP) model).getNumChoices(sExplore);
			for (int i = 0; i < numChoices; i++) {
				Object action = ((MDP) model).getAction(sExplore, i);
				storeTransitions(action, ((MDP) model).getTransitionsIterator(sExplore, i));
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
	private void storeTransitions(Object action, Iterator<Map.Entry<Integer, Double>> transitionsIterator)
	{
		Transitions t = new Transitions();
		t.action = action;
		while (transitionsIterator.hasNext()) {
			Map.Entry<Integer, Double> e = transitionsIterator.next();
			t.succs.add(e.getKey());
			t.probs.add(e.getValue());
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
	public Object getTransitionAction(int i, int offset) throws PrismException
	{
		return trans.get(i).action;
	}

	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException
	{
		return trans.get(i).probs.get(offset);
	}

	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException
	{
		return model.getStatesList().get(trans.get(i).succs.get(offset));
	}
}
