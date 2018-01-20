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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

/**
 * Implements a region representation by a box (hyper-rectangle).
 * This means that for each parameter we have a lower and upper bound, and
 * the region contains all points so that the corresponding dimensions are
 * (not strictly) between these lower and upper bounds.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see BoxRegionFactory
 */
final class BoxRegion extends Region {
	/** used to produce random inner points of box regions */
	static Random random = new Random();
	
	/** regions are only to be split at longest side */
	static final int SPLIT_LONGEST = 1;
	/** regions are to be split at all sides on each split */
	static final int SPLIT_ALL = 2;
	
	/** lower bound for each parameter */
	private BigRational[] lower;
	/** upper bound for each parameter */
	private BigRational[] upper;
	
	// constructors
	
	/**
	 * Constructs a new box region.
	 * Does not assign lower and upper bounds, that is the regions is
	 * not direcly usable after construction.
	 *  
	 * @param factory factory this box region belongs to
	 */
	private BoxRegion(BoxRegionFactory factory)
	{
		this.factory = factory;
		lower = new BigRational[factory.numVariables()];
		upper = new BigRational[factory.numVariables()];
	}
	
	/**
	 * Constructs a new box region.
	 * 
	 * @param factory factory this box region belongs to
	 * @param lower lower bounds for each parameter
	 * @param upper upper bounds for each parameter
	 */
	BoxRegion(BoxRegionFactory factory, BigRational[] lower, BigRational[] upper)
	{
		this.factory = factory;
		this.lower = new BigRational[lower.length];
		this.upper = new BigRational[upper.length];
		System.arraycopy(lower, 0, this.lower, 0, lower.length);
		System.arraycopy(upper, 0, this.upper, 0, upper.length);
	}
	
	/**
	 * Copy constructor for box regions.
	 * For internal use, because regions are generally immutable, so that
	 * there is no need to have external copy constructors.
	 * 
	 * @param other region to copy from
	 */
	private BoxRegion(BoxRegion other)
	{
		this.factory = other.factory;
		this.lower = new BigRational[other.lower.length];
		this.upper = new BigRational[other.upper.length];
		System.arraycopy(other.lower, 0, this.lower, 0, other.lower.length);
		System.arraycopy(other.upper, 0, this.upper, 0, other.upper.length);		
	}
	
	/**
	 * Sets lower and upper bound of given parameter.
	 * 
	 * @param dim parameter to set bounds of
	 * @param lower new lower bound of parameter
	 * @param upper new upper bound of parameter
	 */
	private void setDimension(int dim, BigRational lower, BigRational upper)
	{
		this.lower[dim] = lower;
		this.upper[dim] = upper;
	}
	
	@Override
	int getDimensions()
	{
		return lower.length;
	}
	
	/**
	 * Get lower bound of given parameter in region.
	 * 
	 * @param dim parameter to get lower bound of
	 * @return lower bound of given parameter in region
	 */
	BigRational getDimensionLower(int dim)
	{
		return lower[dim];
	}

	/**
	 * Get upper bound of given parameter in region.
	 * 
	 * @param dim parameter to get lower bound of
	 * @return upper bound of given parameter in region
	 */	
	BigRational getDimensionUpper(int dim)
	{
		return upper[dim];
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof BoxRegion)) {
			return false;
		}
		
		BoxRegion other = (BoxRegion) object;
		
		if (this.getDimensions() != other.getDimensions()) {
			return false;
		}
		
		for (int i = 0; i < getDimensions(); i++) {
			if (!this.lower[i].equals(other.lower[i])) {
				return false;
			}
			if (!this.upper[i].equals(other.upper[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 0;
		
		for (int i = 0; i < getDimensions(); i++) {
			hash = lower[i].hashCode() + (hash << 6) + (hash << 16) - hash;
			hash = upper[i].hashCode() + (hash << 6) + (hash << 16) - hash;
		}
		
		return hash;
	}
	
	/**
	 * Gets the central points of the region.
	 *
	 * @return point in the centre of the region
	 */
	Point getMidPoint()
	{
		BigRational[] point = new BigRational[lower.length];
		for (int dim = 0; dim < lower.length; dim++) {
			BigRational mid = lower[dim].add(upper[dim]).divide(2);
			point[dim] = mid;
		}
		return new Point(point);
	}
	
	/**
	 * Get volume of the region.
	 * The volumes of disjoint region which cover the whole parameter
	 * space sum up to 1. This implies that the volumes are normalised
	 * according to the upper and lower bounds of the parameters.
	 * 
	 * @return volume of this region
	 */
	@Override
	BigRational volume()
	{
		BigRational volume = BigRational.ONE;
		for (int dim = 0; dim < lower.length; dim++) {
			volume = volume.multiply(upper[dim].subtract(lower[dim]));
			volume = volume.divide(factory.sideWidth(dim));
		}
		return volume;
	}
	
	@Override
	boolean contains(Point point)
	{
		for (int dim = 0; dim < getDimensions(); dim++) {
			if (point.getDimension(dim).compareTo(lower[dim]) < 0) {
				return false;
			}
			if (point.getDimension(dim).compareTo(upper[dim]) > 0) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	boolean contains(Region region)
	{
		BoxRegion other = (BoxRegion) region;
		for (int dim = 0; dim < getDimensions(); dim++) {
			if (other.lower[dim].compareTo(this.lower[dim]) == -1) {
				return false;
			}
			if (other.upper[dim].compareTo(this.upper[dim]) == +1) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		for (int dim = 0; dim < getDimensions(); dim++) {
			builder.append("[");
			builder.append(lower[dim].doubleValue());
			builder.append(",");
			builder.append(upper[dim].doubleValue());
			builder.append("]");
			if (dim < getDimensions() - 1) {
				builder.append(",");
			}
		}
		builder.append(")");
		return builder.toString();
	}

	@Override
	RegionValues binaryOp(int op, StateValues values1, StateValues values2) {
		RegionValues result;
		if ((op == Region.EQ || op == Region.NE || op == Region.GT || op == Region.GE || op ==  Region.LT || op == Region.LE)) {
			result = cmpOp(op, values1, values2);
		} else {
			result = new RegionValues(factory);
			int numStates = values1.getNumStates();
			StateValues values = new StateValues(numStates, factory.getInitialState());
			for (int state = 0; state < numStates; state++) {
				if (op == Region.IMPLIES || op == Region.IFF || op == Region.OR || op == Region.AND) {
					boolean stateVal = booleanOp(op, values1.getStateValueAsBoolean(state), values2.getStateValueAsBoolean(state));
					values.setStateValue(state, stateVal);
				} else if (op == Region.PLUS || op == Region.MINUS || op == Region.TIMES || op == Region.DIVIDE) {
					Function stateVal = arithOp(op, values1.getStateValueAsFunction(state), values2.getStateValueAsFunction(state));
					values.setStateValue(state, stateVal);
				} else {
					throw new UnsupportedOperationException("operator not yet implemented for parametric analyses");
				}
			}
			result.add(this, values);
		}
		return result;
	}

	private RegionValues cmpOp(int op, StateValues op1, StateValues op2) {
		ConstraintChecker checker = factory.getConstraintChecker();
		RegionsTODO remaining = new RegionsTODO();
		remaining.add(this);
		BigRational requiredVolume = this.volume().multiply(BigRational.ONE.subtract(factory.getPrecision()));
		BigRational doneVolume = BigRational.ZERO;
		RegionValues result = new RegionValues(factory);
		Function lastFunction = null;
		while (doneVolume.compareTo(requiredVolume) == -1) {
			BoxRegion region = (BoxRegion) remaining.poll();
			StateValues newValues = new StateValues(op1.getNumStates(), factory.getInitialState());
			boolean allDecided = true;
			for (int state = 0; state < op1.getNumStates(); state++) {
				StateValue op1Val = op1.getStateValue(state);
				StateValue op2Val = op2.getStateValue(state);
				Function op1ValFn = op1Val instanceof Function ? (Function) op1Val : null;
				Function op2ValFn = op2Val instanceof Function ? (Function) op2Val : null;
				if (op == Region.EQ) {
					if (op1Val instanceof StateBoolean) {
						newValues.setStateValue(state, op1Val.equals(op2Val));						
					}
					else if (op1Val.equals(op2Val)) {
						newValues.setStateValue(state, true);			
					} else if (checker.check(region, op1ValFn.subtract(op2ValFn), true)) {
						newValues.setStateValue(state, false);	
					} else if (checker.check(region, op2ValFn.subtract(op1ValFn), true)) {
						newValues.setStateValue(state, false);	
					} else {
						allDecided = false;
						break;
					}
				} else if (op == Region.NE) {
					if (op1Val instanceof StateBoolean) {
						newValues.setStateValue(state, !op1Val.equals(op2Val));
					} else if (op1Val.equals(op2Val)) {
						newValues.setStateValue(state, false);
					} else if (checker.check(region, op1ValFn.subtract(op2ValFn), true)) {
						newValues.setStateValue(state, true);
					} else if (checker.check(region, op2ValFn.subtract(op1ValFn), true)) {
						newValues.setStateValue(state, true);
					} else {
						allDecided = false;
						break;
					}
				} else {
					boolean strict = op == Region.GT || op == Region.LT;
					Function cmpTrue = (op == Region.LT || op == Region.LE) ? op2ValFn.subtract(op1ValFn) : op1ValFn.subtract(op2ValFn);
					if (checker.check(region, cmpTrue, strict)) {
						newValues.setStateValue(state, true);
					} else {
						Function cmpFalse = (op == Region.LT || op == Region.LE) ? op1ValFn.subtract(op2ValFn) : op2ValFn.subtract(op1ValFn);
						if (checker.check(region, cmpFalse, !strict)) {
							newValues.setStateValue(state, false);	
						} else {
							allDecided = false;
							lastFunction = op2ValFn.subtract(op1ValFn);
							break;
						}
					}
				}
			}
			if (allDecided) {
				result.add(region, newValues);
				doneVolume = doneVolume.add(region.volume());
			} else {
				remaining.addAll(region.split(lastFunction));
			}
		}
		
		return result;
	}
	
	/**
	 * Performs given operation on two rational functions.
	 * 
	 * @param op operation to perform, see values in {@code Region}
	 * @param op1 first operand
	 * @param op2  second operand
	 * @return value of operation
	 */
	private Function arithOp(int op, Function op1, Function op2) {
		Function result = null;
		switch (op) {
		case Region.PLUS:
			result = op1.add(op2);
			break;
		case Region.MINUS:
			result = op1.subtract(op2);
			break;
		case Region.TIMES:
			result = op1.multiply(op2);
			break;
		case Region.DIVIDE:
			result = op1.divide(op2);
			break;
		default:
			throw new IllegalArgumentException("unsupported arithmetic operator number " + op);
		}
		return result;
	}

	/**
	 * Performs given operation on two booleans.
	 * 
	 * @param op operation to perform, see values in {@code Region}
	 * @param op1 first operand
	 * @param op2  second operand
	 * @return value of operation
	 */
	private boolean booleanOp(int op, boolean op1, boolean op2) {
		boolean result = false;
		
		switch (op) {
		case Region.IMPLIES:
			result = !op1 || op2;
			break;
		case Region.IFF:
			result = op1 == op2;
			break;
		case Region.OR:
			result = op1 || op2;
			break;
		case Region.AND:
			result = op1 && op2;
			break;
		default:
			throw new IllegalArgumentException("unsupported boolean operator number " + op);
		}
		return result;
	}
	
	@Override
	RegionValues ITE(StateValues valuesI, StateValues valuesT, StateValues valuesE)
	{
		RegionValues result = new RegionValues(factory);
		int numStates = valuesI.getNumStates();
		StateValues values = new StateValues(numStates, factory.getInitialState());
		for (int state = 0; state < numStates; state++) {
			if (valuesI.getStateValueAsBoolean(state)) {
				values.setStateValue(state, valuesT.getStateValue(state));
			} else {
				values.setStateValue(state, valuesE.getStateValue(state));
			}
		}
		result.add(this, values);

		return result;
	}

	/**
	 * Split region in longest dimension.
	 * 
	 * @return set of new regions covering same area
	 */
	private ArrayList<Region> splitLongest()
	{
		int longestSide = -1;
		BigRational longestLength = BigRational.ZERO;
		for (int side = 0; side < lower.length; side++) {
			BigRational sideLength = upper[side].subtract(lower[side]);
			if (sideLength.compareTo(longestLength) == 1) {
				longestSide = side;
				longestLength = sideLength;
			}
		}
		
		ArrayList<Region> result = new ArrayList<Region>();
		BoxRegion region1 = new BoxRegion(this);
		BoxRegion region2 = new BoxRegion(this);
		BigRational mid = (lower[longestSide].add(upper[longestSide])).divide(2);
		region1.upper[longestSide] = mid;
		region2.lower[longestSide] = mid;
		result.add(region1);
		result.add(region2);
				
		return result;
	}
	
	/**
	 * Split region in all dimensions.
	 * 
	 * @return set of new regions covering same area
	 */
	private ArrayList<Region> splitAll()
	{
		ArrayList<Region> result = new ArrayList<Region>();
		final int numParts = 2;
		final int numNewRegions = (int) Math.pow(numParts, lower.length);
		BigRational[] newLower = new BigRational[lower.length];
		BigRational[] newUpper = new BigRational[upper.length];
		for (int newRegionNr = 0; newRegionNr < numNewRegions; newRegionNr++) {
			int regionRest = newRegionNr;
			for (int var = 0; var < lower.length; var++) {
				int lowerPart = regionRest % numParts;
				regionRest /= numParts;
				BigRational partLength = upper[var].subtract(lower[var]).divide(numParts);
				newLower[var] = lower[var].add(partLength.multiply(lowerPart));
				newUpper[var] = lower[var].add(partLength.multiply(lowerPart + 1));
			}
			result.add(new BoxRegion((BoxRegionFactory) factory, newLower, newUpper));
		}
		return result;
	}

	@Override
	ArrayList<Region> split(Function constraint)
	{
		// TODO could implement more clever splitting using constraints
		return split();
	}

	@Override
	ArrayList<Region> split()
	{
		if (((BoxRegionFactory) factory).getSplitMethod() == SPLIT_LONGEST) {
			return splitLongest();
		} else if (((BoxRegionFactory) factory).getSplitMethod() == SPLIT_ALL) {
			return splitAll();
		} else {
			throw new RuntimeException();
		}
	}	
	
	@Override
	ArrayList<Point> specialPoints()
	{
		ArrayList<Point> result = new ArrayList<Point>();
		int numEdges = (int) Math.pow(2, lower.length);
		for (int edgeNr = 0; edgeNr < numEdges; edgeNr++) {
			int regionRest = edgeNr;
			BigRational[] point = new BigRational[lower.length];
			for (int dim = 0; dim < lower.length; dim++) {
				boolean useLower = ( 0 == (regionRest % 2));
				regionRest /= 2;
				if (useLower) {
					point[dim] = lower[dim];
				} else {
					point[dim] = upper[dim];					
				}
			}
			result.add(new Point(point));
		}
		return result;
	}
	
	@Override
	Point randomPoint()
	{
		BigRational[] point = new BigRational[lower.length];
		BigInteger maxInt = new BigInteger(Long.toString((long) Math.pow(2, 60))); 
		for (int dim = 0; dim < lower.length; dim++) {
			BigInteger rndInt = new BigInteger(60, random);
			BigRational rndRat = new BigRational(rndInt, maxInt);
			rndRat = lower[dim].add(upper[dim].subtract(lower[dim]).multiply(rndRat));
			point[dim] = rndRat;
		}
		
		return new Point(point);
	}

	@Override
	BoxRegion conjunct(Region region) {
		BoxRegion other = (BoxRegion) region;
		
		BoxRegion result = new BoxRegion((BoxRegionFactory) factory);
		for (int dim = 0; dim < lower.length; dim++) {
			if (this.upper[dim].compareTo(other.lower[dim]) <= 0) {
				return null;
			}
			if (this.lower[dim].compareTo(other.upper[dim]) >= 0) {
				return null;
			}
			BigRational newLower = this.lower[dim].max(other.lower[dim]);
			BigRational newUpper = this.upper[dim].min(other.upper[dim]);
			if (newLower.equals(newUpper)) {
				return null;
			}
			result.setDimension(dim, newLower, newUpper);
		}
		return result;
	}

	/**
	 * Checks whether this region is adjacent to another region in given dimension.
	 * In this case, both can be glued by @{code glue}.
	 * 
	 * @param other region to check whether adjacent in given dimension
	 * @param adjDim dimension where to check adjacency
	 * @return true iff regions are adjacent in given dimension
	 */
	private boolean adjacent(Region region, int adjDim)
	{
		BoxRegion other = (BoxRegion) region;
		for (int dim = 0; dim < this.getDimensions(); dim++) {
			if (dim != adjDim) {
				if (!this.getDimensionLower(dim).equals(other.getDimensionLower(dim))) {
					return false;
				}
				if (!this.getDimensionUpper(dim).equals(other.getDimensionUpper(dim))) {
					return false;
				}
			}
		}
		
		return this.getDimensionUpper(adjDim).equals(other.getDimensionLower(adjDim));
	}

	@Override
	boolean adjacent(Region region)
	{
		BoxRegion other = (BoxRegion) region;
		for (int dim = 0; dim < getDimensions(); dim++) {
			if (adjacent(other, dim)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	Region glue(Region region) {
		BoxRegion other = (BoxRegion) region;
		BoxRegion result = new BoxRegion((BoxRegionFactory) this.getFactory());
		
		for (int dim = 0; dim < result.getDimensions(); dim++) {
			result.setDimension(dim, this.getDimensionLower(dim), other.getDimensionUpper(dim));
		}

		return result;
	}
}
