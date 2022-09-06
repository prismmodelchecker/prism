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

// TODO allow null values in entries

package param;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntPredicate;

import common.IterableStateSet;
import parser.State;
import parser.type.TypeBool;
import prism.PrismLog;

/**
 * Class to assign a value to each state of a model.
 * Used by {@code RegionValues}.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see RegionValues
 */
public final class StateValues
{
	/** assigns values to each state of the model */
	private ArrayList<StateValue> values;
	/** initial state of the model */
	private int initState;

	/**
	 * Constructs new set of state values.
	 * Each state is mapped to {@code null}.
	 * 
	 * @param numStates number of states of model
	 * @param initState initial state of the model
	 */
	public StateValues(int numStates, int initState)
	{
		values = new ArrayList<StateValue>(numStates);
		for (int state = 0; state < numStates; state++) {
			values.add(state, null);
		}
		this.initState = initState;
	}

	/**
	 * Constructs new set of state values.
	 * Each state is mapped to the given value.
	 * 
	 * @param numStates number of states of the model
	 * @param initState initial state of the model
	 * @param value value to map all states to
	 */
	public StateValues(int numStates, int initState, StateValue value)
	{
		this(numStates, initState);
		for (int state = 0; state < numStates; state++) {
			values.set(state, value);
		}
	}

	/**
	 * Constructs new set of state values.
	 * Each state is mapped to the given value.
	 * 
	 * @param numStates number of states of the model
	 * @param initState initial state of the model
	 * @param value value to map all states to
	 */
	public StateValues(int numStates, int initState, boolean value)
	{
		this(numStates, initState, new StateBoolean(value));
	}

	@Override
	public String toString()
	{
		return values.get(initState).toString();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof StateValues)) {
			return false;
		}

		StateValues result = (StateValues) obj;

		for (int i = 0; i < values.size(); i++) {
			if (!values.get(i).equals(result.values.get(i))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 0;

		for (int i = 0; i < values.size(); i++) {
			hash = values.get(i).hashCode() + (hash << 6) + (hash << 16) - hash;
		}

		return hash;
	}

	/**
	 * Get value of given state.
	 * 
	 * @param state state to get value of
	 * @return value of given state
	 */
	public StateValue getStateValue(int state)
	{
		return values.get(state);
	}

	/**
	 * Set value of given state.
	 * 
	 * @param state state to set value of
	 * @param value value to set for state
	 */
	public void setStateValue(int state, StateValue value)
	{
		values.set(state, value);
	}

	/**
	 * Set value of given state.
	 * 
	 * @param state state to set value of
	 * @param value value to set for state
	 */
	public void setStateValue(int state, boolean value)
	{
		values.set(state, new StateBoolean(value));
	}

	/**
	 * Get value of given state as rational function.
	 * If the value of the state is not a function, this will lead to an error.
	 * 
	 * @param state state to get value of
	 * @return value of the state as a function
	 */
	public Function getStateValueAsFunction(int state)
	{
		return (Function) values.get(state);
	}

	/**
	 * Get value of given state as boolean.
	 * If the value of the state is not a boolean, this will lead to an error.
	 * 
	 * @param state state to get value of
	 * @return value of the state as a boolean
	 */
	public boolean getStateValueAsBoolean(int state)
	{
		return ((StateBoolean) values.get(state)).getValue();
	}

	/**
	 * Get value of initial state as rational function.
	 * If the value of the initial state is not a function, this will lead to an error.
	 * 
	 * @return value of the initial state as a function
	 */
	public Function getInitStateValueAsFunction()
	{
		return (Function) values.get(initState);
	}

	/**
	 * Get value of initial state as boolean.
	 * If the value of the initial state is not a boolean, this will lead to an error.
	 * 
	 * @return value of the initial state as a boolean
	 */
	public boolean getInitStateValueAsBoolean()
	{
		return ((StateBoolean) values.get(initState)).getValue();
	}

	/**
	 * Returns number of states of the model.
	 * 
	 * @return number of states of the model
	 */
	public int getNumStates()
	{
		return values.size();
	}

	/**
	 * Converts this state value assignment to a bitset.
	 * For this to work, all states must be mapped to booleans.
	 * 
	 * @return bitset representing this state value assignment
	 */
	public BitSet toBitSet()
	{
		BitSet result = new BitSet(values.size());
		for (int state = 0; state < values.size(); state++) {
			result.set(state, getStateValueAsBoolean(state));
		}
		return result;
	}

	/**
	 * Instantiates the value to which each state is mapped at the given point.
	 * For this to work, all states must be mapped to rational functions.
	 * 
	 * @param point point to instantiate state values
	 * @return array of {@code BigRational}s mapping each state to evaluated value
	 */
	public BigRational[] instantiate(Point point)
	{
		BigRational[] result = new BigRational[values.size()];
		for (int state = 0; state < values.size(); state++) {
			result[state] = this.getStateValueAsFunction(state).evaluate(point);
		}
		return result;
	}

	/**
	 * Print part of vector to a log/file.
	 * @param log The log
	 * @param mode the mode
	 * @param filter A BitSet specifying which states to print for (null if all).
	 * @param printSparse Print non-zero/non-false elements only?
	 * @param printStates Print states (variable values) for each element?
	 * @param printIndices Print state indices for each element?
	 */
	public void printFiltered(PrismLog log, ParamMode mode, parser.type.Type type, BitSet filter, List<State> statesList, boolean printSparse, boolean printStates, boolean printIndices)
	{
		int count = 0;

		IntPredicate nonZero =
				(type instanceof TypeBool)
				? (int n) -> {return getStateValueAsBoolean(n);}
				: (int n) -> {return !getStateValueAsFunction(n).isZero();};

		// Print vector
		for (int n : new IterableStateSet(filter, getNumStates())) {
			if (!printSparse || nonZero.test(n)) {
				if (printIndices) {
					log.print(n);
					log.print(":");
				}

				if (printStates && statesList != null)
					log.print(statesList.get(n).toString());
				if (printSparse && type instanceof TypeBool) {
					log.println();
				} else {
					if (printIndices || printStates)
						log.print("=");

					if (type instanceof TypeBool) {
						log.println(getStateValueAsBoolean(n));
					} else {
						if (mode == ParamMode.EXACT) {
							BigRational value = getStateValueAsFunction(n).asBigRational();
							log.println(value + "   (" + value.toApproximateString() + ")");
						} else {
							log.println(getStateValueAsFunction(n));
						}
					}
				}
				count++;
			}
		}

		// Check if all zero
		if (printSparse && count == 0) {
			log.println("(all zero)");
			return;
		}
	}

}
