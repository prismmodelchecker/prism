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

import java.util.Collections;
import java.util.List;

import parser.Values;
import parser.VarList;
import parser.ast.RewardStruct;
import parser.type.Type;

/**
 * Interface for classes that provide some basic (syntactic) information about a probabilistic model.
 */
public interface ModelInfo
{
	/**
	 * Get the type of probabilistic model.
	 */
	public ModelType getModelType();

	/**
	 * Set values for *some* undefined constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using standard (integer, floating-point) arithmetic.
	 */
	public default void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		// By default, assume there are no constants to define 
		if (someValues != null && someValues.getNumValues() > 0)
			throw new PrismException("This model has no constants to set");
	}

	/**
	 * Set values for *some* undefined constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using either using standard (integer, floating-point) arithmetic
	 * or exact arithmetic, depending on the value of the {@code exact} flag.
	 */
	public default void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismException
	{
		// default implementation: use implementation for setSomeUndefinedConstants(Values)
		// for non-exact, error for exact
		//
		// implementers should override both this method and setSomeUndefinedConstants(Values)
		// above
		if (!exact)
			setSomeUndefinedConstants(someValues);
		else
			throw new PrismException("This model can not set constants in exact mode");
	}

	/**
	 * Get access to the values for all constants in the model, including the 
	 * undefined constants set previously via the method {@link #setUndefinedConstants(Values)}.
	 * Until they are set for the first time, this method returns null.  
	 */
	public default Values getConstantValues()
	{
		// By default, assume there are no constants to define 
		return new Values();
	}

	/**
	 * Does the model contain unbounded variables?
	 */
	public default boolean containsUnboundedVariables()
	{
		// By default, assume all variables are finite-ranging
		return false;
	}

	/**
	 * Get the number of variables in the model. 
	 */
	public int getNumVars();
	
	/**
	 * Get the names of all the variables in the model.
	 */
	public List<String> getVarNames();
	
	/**
	 * Look up the index of a variable in the model by name.
	 * Returns -1 if there is no such variable.
	 */
	public default int getVarIndex(String name)
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().indexOf(name);
	}

	/**
	 * Get the name of the {@code i}th variable in the model.
	 * {@code i} should always be between 0 and getNumVars() - 1. 
	 */
	public default String getVarName(int i)
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().get(i);
	}

	/**
	 * Get the types of all the variables in the model.
	 */
	public List<Type> getVarTypes();

	/**
	 * Get the type of the {@code i}th variable in the model.
	 * {@code i} should always be between 0 and getNumVars() - 1. 
	 */
	public default Type getVarType(int i) throws PrismException
	{
		// Default implementation just extracts from getVarTypes() 
		return getVarTypes().get(i);
	}

	/**
	 * Get the number of labels (atomic propositions) defined for the model. 
	 */
	public default int getNumLabels()
	{
		// Default implementation just extracts from getLabelNames() 
		return getLabelNames().size();
	}
	
	/**
	 * Get the names of all the labels in the model.
	 */
	public default List<String> getLabelNames()
	{
		// No labels by default
		return Collections.emptyList();
	}
	
	/**
	 * Get the name of the {@code i}th label of the model.
	 * {@code i} should always be between 0 and getNumLabels() - 1. 
	 */
	public default String getLabelName(int i) throws PrismException
	{
		// Default implementation just extracts from getLabelNames() 
		try {
			return getLabelNames().get(i);
		} catch (IndexOutOfBoundsException e) {
			throw new PrismException("Label number " + i + " not defined");
		}
	}
	
	/**
	 * Get the index of the label with name {@code name}.
	 * Indexed from 0. Returns -1 if label of that name does not exist.
	 */
	public default int getLabelIndex(String name)
	{
		// Default implementation just extracts from getLabelNames() 
		return getLabelNames().indexOf(name);
	}
	
	/**
	 * Get the number of reward structures in the model.
	 */
	public default int getNumRewardStructs()
	{
		// Default implementation just extracts from getRewardStructNames() 
		return getRewardStructNames().size();
	}
	
	/**
	 * Get a list of the names of the reward structures in the model.
	 */
	public default List<String> getRewardStructNames()
	{
		// No reward structures by default
		return Collections.emptyList();
	}
	
	/**
	 * Get the index of a module by its name
	 * (indexed from 0, not from 1 like at the user (property language) level).
	 * Returns -1 if name does not exist.
	 */
	public default int getRewardStructIndex(String name)
	{
		// Default implementation just extracts from getRewardStructNames() 
		return getRewardStructNames().indexOf(name);
	}

	/**
	 * Get a reward structure by its index
	 * (indexed from 0, not from 1 like at the user (property language) level).
	 * Returns null if index is out of range.
	 */
	public default RewardStruct getRewardStruct(int i)
	{
		// No reward structures by default
		return null;
	}

	/**
	 * Returns true if the reward structure with index i defines transition rewards.
	 * (indexed from 0, not from 1 like at the user (property language) level)
	 * If this returns false, the model checker is allowed to ignore them (which may be more efficient).
	 * If using an algorithm or implementation that does not support transition rewards,
	 * you may need to return false here (as well as not defining transition rewards).
	 */
	public default boolean rewardStructHasTransitionRewards(int i)
	{
		// By default, assume that any reward structures that do exist may have transition rewards
		return true;
	}

	// TODO: can we remove this?
	public VarList createVarList() throws PrismException;
}
