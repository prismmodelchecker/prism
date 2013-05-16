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
 * Implements a {@code RegionFactory} producing {@code BoxRegion}s.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see RegionFactory
 * @see BoxRegion
 */
final class BoxRegionFactory extends RegionFactory {
	/** method to use to split boxes produces with this factory.
	 * split either at all or only at the longest side. */
	private int splitMethod;
	
	/**
	 * Constructs a new box region factory.
	 * 
	 * @param functionFactory function factory to use
	 * @param constraintChecker constraint checker to use
	 * @param precision remaining undecided area to allow
	 * @param numStates number of model states
	 * @param initialState initial states of model
	 * @param subsumeRegions whether to subsume regions if possible
	 * @param splitMethod method to split regions
	 */
	BoxRegionFactory(FunctionFactory functionFactory, ConstraintChecker constraintChecker, BigRational precision,
			int numStates, int initialState, boolean subsumeRegions, int splitMethod)
	{
		this.functionFactory = functionFactory;
		this.constraintChecker = constraintChecker;
		this.precision = precision;
		this.numStates = numStates;
		this.initialState = initialState;
		this.subsumeRegions = subsumeRegions;
		this.splitMethod = splitMethod;
	}
	
	@Override
	RegionValues completeCover(StateValues values)
	{
		RegionValues result = new RegionValues(this);
		BigRational[] lower = new BigRational[functionFactory.getNumVariables()];
		BigRational[] upper = new BigRational[functionFactory.getNumVariables()];
		for (int var = 0; var < functionFactory.getNumVariables(); var++) {
			lower[var] = functionFactory.getLowerBound(var);
			upper[var] = functionFactory.getUpperBound(var);
		}
		BoxRegion region = new BoxRegion(this, lower, upper);
		result.add(region, values);
		return result;
	}

	/**
	 * Returns the method used to split regions.
	 * See values specified in {@code Region}.
	 * 
	 * @return method used to split regions
	 */
	int getSplitMethod()
	{
		return splitMethod;
	}
}
