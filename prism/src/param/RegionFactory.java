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

/**
 * Class to produce new regions of a given subclass of {@code Region}.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see RegionFactory
 */
abstract class RegionFactory {
	/** function factory used to produce functions used as values of regions */
	protected FunctionFactory functionFactory;
	/** constraint checker to check properties of regions */
	protected ConstraintChecker constraintChecker;
	/** maximal amount of undecided area for region cover produced */
	protected BigRational precision;
	/** initial state of the model the considered */
	protected int initialState;
	/** number of states of the model used */
	protected int numStates;
	/** decides whether regions should be subsumed if possible,
	 * that is if they can be represented by a single larger region
	 * and are assigned the same values */
	protected boolean subsumeRegions;

	/**
	 * Covers the whole parameter space with the values specified.
	 * 
	 * @param values values to assign to each state for all valid valuations
	 * @return cover of whole parameter region
	 */
	abstract RegionValues completeCover(StateValues values);
	
	/**
	 * Covers the whole parameter space with the values specified.
	 * Assigns the same values to each state.
	 * 
	 * @param value values to assign for all valid evaluations (same for each state)
	 * @return cover of whole parameter region
	 */
	RegionValues completeCover(StateValue value)
	{
		StateValues values = new StateValues(numStates, initialState, value);
		return completeCover(values);
	}

	/**
	 * Covers the whole parameter space with the values specified.
	 * Assigns the same values to each state.
	 * 
	 * @param value values to assign for all valid evaluations (same for each state)
	 * @return cover of whole parameter region
	 */
	RegionValues completeCover(boolean value)
	{
		return completeCover(new StateBoolean(value));
	}
	
	/**
	 * Get lower bound of given parameter.
	 * 
	 * @param dim parameter to get lower bound of
	 * @return lower bound of given parameter
	 */
	BigRational getLowerBound(int dim)
	{
		return functionFactory.getLowerBound(dim);
	}
	
	/**
	 * Get upper bound of given parameter.
	 * 
	 * @param dim parameter to get upper bound fo
	 * @return upper bound of given parameter
	 */
	BigRational getUpperBound(int dim)
	{
		return functionFactory.getUpperBound(dim);
	}
	
	/**
	 * Returns diameter of range of given variable {@code dim}.
	 * 
	 * @param dim variable to return diameter of
	 * @return diameter of range of variable {@code dim}
	 */
	BigRational sideWidth(int dim)
	{
		return functionFactory.getUpperBound(dim).subtract(functionFactory.getLowerBound(dim));
	}
	
	/**
	 * Returns function factory used to produce functions used as values of regions.
	 * @return function factory used to produce functions used as values of regions
	 */
	FunctionFactory getFunctionFactory()
	{
		return functionFactory;
	}
	
	/**
	 * Returns constraint checker to check properties of regions.
	 * 
	 * @return constraint checker to check properties of regions
	 */
	ConstraintChecker getConstraintChecker()
	{
		return constraintChecker;
	}
	
	/**
	 * Returns maximal amount of undecided area for region cover produced.
	 * 
	 * @return maximal amount of undecided area for region cover produced
	 */
	BigRational getPrecision()
	{
		return precision;
	}

	/**
	 * Returns number of parameters used.
	 * 
	 * @return number of parameters
	 */
	int numVariables() {
		return functionFactory.getNumVariables();
	}

	/**
	 * Returns number of states of the model this {@code RegionFactory} is used for.
	 * 
	 * @return number of states
	 */
	int getNumStates()
	{
		return numStates;
	}
	
	/**
	 * Returns the initial state of the model this {@code RegionFactory} is used for.
	 * 
	 * @return initial state of model
	 */
	int getInitialState()
	{
		return initialState;
	}
	
	/**
	 * Returns whether regions are to be subsumed if possible.
	 * 
	 * @return true iff regions shall be subsumed if possible
	 */
	boolean isSubsumeRegions()
	{
		return subsumeRegions;
	}
}
