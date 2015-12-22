//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Nishan Kamaleson <nxk249@bham.ac.uk> (University of Birmingham)
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

package prism;

import java.util.ArrayList;
import java.util.List;

import parser.State;
import parser.Values;
import parser.ast.RewardStruct;
import parser.type.Type;

/**
 * Default implementation of the {@link ModelGenerator} interface
 */
public abstract class DefaultModelGenerator implements ModelGenerator
{
	// Methods for ModelInfo interface
	
	@Override
	public abstract ModelType getModelType();
	
	@Override
	public void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		if (someValues != null && someValues.getNumValues() > 0)
			throw new PrismException("This model has no constants to set");
	}

	@Override
	public Values getConstantValues()
	{
		// Empty values
		return new Values();
	}
	
	@Override
	public boolean containsUnboundedVariables()
	{
		return false;
	}

	@Override
	public abstract int getNumVars();
	
	@Override
	public abstract List<String> getVarNames();

	@Override
	public int getVarIndex(String name)
	{
		return getVarNames().indexOf(name);
	}

	@Override
	public String getVarName(int i)
	{
		return getVarNames().get(i);
	}

	@Override
	public abstract List<Type> getVarTypes();
	
	@Override
	public int getNumLabels()
	{
		return 0;	
	}
	
	@Override
	public String getLabelName(int i) throws PrismException
	{
		throw new PrismException("Label number \"" + i + "\" not defined");
	}
	
	public int getLabelIndex(String label)
	{
		return -1;
	}
	
	@Override
	public int getNumRewardStructs()
	{
		return 0;
	}
	
	@Override
	public int getRewardStructIndex(String name)
	{
		return -1;
	}
	
	@Override
	public RewardStruct getRewardStruct(int i)
	{
		return null;
	}
	
	// Methods for ModelGenerator interface
	
	public boolean hasSingleInitialState() throws PrismException
	{
		// Default to the case of a single initial state
		return true;
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException
	{
		// Default to the case of a single initial state
		ArrayList<State> initStates = new ArrayList<State>();
		initStates.add(getInitialState());
		return initStates;
	}

	@Override
	public abstract State getInitialState() throws PrismException;

	@Override
	public abstract void exploreState(State exploreState) throws PrismException;

	@Override
	public abstract State getExploreState();

	@Override
	public abstract int getNumChoices() throws PrismException;

	@Override
	public int getNumTransitions() throws PrismException
	{
		// Default implementation
		int tot = 0;
		int n = getNumChoices();
		for (int i = 0; i < n; i++) {
			tot += getNumTransitions(i);
		}
		return tot;
	}

	@Override
	public abstract int getNumTransitions(int i) throws PrismException;

	@Override
	public abstract Object getTransitionAction(int i) throws PrismException;

	@Override
	public abstract Object getTransitionAction(int i, int offset) throws PrismException;

	@Override
	public abstract double getTransitionProbability(int i, int offset) throws PrismException;

	@Override
	public abstract State computeTransitionTarget(int i, int offset) throws PrismException;

	@Override
	public boolean isLabelTrue(String label) throws PrismException
	{
		// Look up label and then check by index
		int i = getLabelIndex(label);
		if (i == -1) {
			throw new PrismException("Label \"" + label + "\" not defined");
		} else {
			return isLabelTrue(i);
		}
	}
	
	@Override
	public boolean isLabelTrue(int i) throws PrismException
	{
		throw new PrismException("Label number \"" + i + "\" not defined");
	}

	@Override
	public double getStateReward(int index, State state) throws PrismException
	{
		return 0.0;
	}
}
