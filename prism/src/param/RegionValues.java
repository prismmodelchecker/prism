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

import java.util.BitSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Assigns to the different regions different values over model states.
 * For each region for which values have been decided, an object of this
 * class contains a value for each state.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
public final class RegionValues implements Iterable<Entry<Region, StateValues>>
{
	/** list of all regions */
	private ArrayList<Region> regions;
	/** assigning values to regions */
	private HashMap<Region, StateValues> values;
	/** region factory this object belongs to */
	private RegionFactory factory;

	// constructors

	public RegionValues(RegionFactory factory)
	{
		regions = new ArrayList<Region>();
		values = new HashMap<Region, StateValues>();
		this.factory = factory;
	}

	public int getNumStates()
	{
		return factory.getNumStates();
	}

	public int getInitState()
	{
		return factory.getInitialState();
	}

	public void add(Region region, StateValues result)
	{
		regions.add(region);
		values.put(region, result);
	}

	/**
	 * Helper function for simplify.
	 * Subsumes some of the regions. If it contains true, must be called
	 * again until it returns false.
	 * 
	 * @return true iff regions were subsumed
	 */
	private boolean simplifyIter()
	{
		boolean changed = false;
		ArrayList<Region> newRegions = new ArrayList<Region>();
		HashMap<Region, StateValues> newValues = new HashMap<Region, StateValues>();
		HashSet<Region> done = new HashSet<Region>();

		for (Region region1 : regions) {
			for (Region region2 : regions) {
				if (values.get(region1).equals(values.get(region2)) && region1.adjacent(region2) && !done.contains(region1) && !done.contains(region2)) {
					done.add(region1);
					done.add(region2);
					Region newRegion = region1.glue(region2);
					newRegions.add(newRegion);
					newValues.put(newRegion, values.get(region1));
					changed = true;
					break;
				}
			}
		}
		for (Region region : regions) {
			if (!done.contains(region)) {
				newRegions.add(region);
				newValues.put(region, values.get(region));
			}
		}

		regions.clear();
		values.clear();
		regions.addAll(newRegions);
		values.putAll(newValues);

		return changed;
	}

	/**
	 * Simplify by subsuming adjacent regions with same value.
	 */
	public void simplify()
	{
		if (factory.isSubsumeRegions()) {
			while (simplifyIter())
				;
		}
	}

	public Region getRegion(int number)
	{
		return regions.get(number);
	}

	public int getNumRegions()
	{
		return regions.size();
	}

	public StateValues getResult(int number)
	{
		return values.get(regions.get(number));
	}

	public StateValues getResult(Region region)
	{
		return values.get(region);
	}

	public void cosplit(RegionValues other)
	{
		this.simplify();
		other.simplify();

		ArrayList<Region> newRegions = new ArrayList<Region>();
		HashMap<Region, StateValues> thisNewStateValues = new HashMap<Region, StateValues>();
		HashMap<Region, StateValues> otherNewStateValues = new HashMap<Region, StateValues>();
		for (Region thisRegion : this.regions) {
			for (Region otherRegion : other.regions) {
				Region newRegion = thisRegion.conjunct(otherRegion);
				if (newRegion != null) {
					newRegions.add(newRegion);
					thisNewStateValues.put(newRegion, this.values.get(thisRegion));
					otherNewStateValues.put(newRegion, other.values.get(otherRegion));
				}
			}
		}

		this.regions = new ArrayList<Region>(newRegions);
		this.values = thisNewStateValues;
		other.regions = new ArrayList<Region>(newRegions);
		other.values = otherNewStateValues;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < regions.size(); i++) {
			Region region = regions.get(i);
			builder.append(region);
			builder.append(": ");
			builder.append(values.get(region));
			builder.append("\n");
		}

		return builder.toString();
	}

	public void addAll(RegionValues other)
	{
		int numOtherRegions = other.getNumRegions();
		for (int i = 0; i < numOtherRegions; i++) {
			Region region = other.getRegion(i);
			regions.add(region);
			values.put(region, other.getResult(region));
		}
	}

	public void clearExcept(BitSet except)
	{
		for (Region region : regions) {
			StateValues vals = values.get(region);
			for (int state = 0; state < vals.getNumStates(); state++) {
				if (!except.get(state)) {
					StateValue oldValue = vals.getStateValue(state);
					if (oldValue instanceof StateBoolean) {
						vals.setStateValue(state, false);
					} else if (oldValue instanceof Function) {
						vals.setStateValue(state, factory.getFunctionFactory().getZero());
					} else {
						throw new RuntimeException();
					}
				}
			}
		}
		simplify();
	}

	public void clearExceptInit()
	{
		if (regions.isEmpty()) {
			return;
		}
		int numStates = values.get(regions.get(0)).getNumStates();
		BitSet except = new BitSet(numStates);
		except.set(factory.getInitialState(), true);
		clearExcept(except);
	}

	@Override
	public Iterator<Entry<Region, StateValues>> iterator()
	{
		return values.entrySet().iterator();
	}

	public RegionFactory getRegionFactory()
	{
		return factory;
	}

	public boolean booleanValues()
	{
		if (regions.isEmpty()) {
			return true;
		}
		return values.get(regions.get(0)).getStateValue(0) instanceof StateBoolean;
	}

	public RegionValues binaryOp(int op, RegionValues other)
	{
		RegionValues result = new RegionValues(factory);
		RegionValuesIntersections co = new RegionValuesIntersections(this, other);
		for (RegionIntersection inter : co) {
			Region region = inter.getRegion();
			StateValues value1 = inter.getStateValues1();
			StateValues value2 = inter.getStateValues2();
			RegionValues values = region.binaryOp(op, value1, value2);
			result.addAll(values);
		}
		return result;
	}

	public RegionValues binaryOp(int op, BigRational p)
	{
		RegionValues result = new RegionValues(factory);
		Function pFn = factory.getFunctionFactory().fromBigRational(p);
		StateValues pValue = new StateValues(values.get(regions.get(0)).getNumStates(), factory.getInitialState(), pFn);
		for (Region region : regions) {
			RegionValues vals = region.binaryOp(op, values.get(region), pValue);
			result.addAll(vals);
		}

		return result;
	}

	public RegionValues op(int op, BitSet whichStates)
	{
		RegionValues result = new RegionValues(factory);
		if (op == Region.FIRST) {
			for (Region region : regions) {
				StateValue firstValue;
				int firstState = whichStates.nextSetBit(0);
				firstValue = values.get(region).getStateValue(firstState);
				StateValues resValues = new StateValues(getNumStates(), getInitState());
				for (int state = 0; state < getNumStates(); state++) {
					resValues.setStateValue(state, firstValue);
				}
				result.add(region, resValues);
			}
		} else if (op == Region.PLUS || op == Region.AVG) {
			for (Region region : regions) {
				StateValues vals = values.get(region);
				Function sum = factory.getFunctionFactory().getZero();
				for (int state = 0; state < getNumStates(); state++) {
					if (whichStates.get(state)) {
						sum = sum.add(vals.getStateValueAsFunction(state));
					}
				}
				if (op == Region.AVG) {
					sum = sum.divide(whichStates.cardinality());
				}
				StateValues resValues = new StateValues(getNumStates(), getInitState(), sum);
				result.add(region, resValues);
			}
		} else if (op == Region.COUNT) {
			for (Region region : regions) {
				StateValues vals = values.get(region);
				int count = 0;
				for (int state = 0; state < getNumStates(); state++) {
					if (whichStates.get(state)) {
						if (vals.getStateValueAsBoolean(state)) {
							count++;
						}
					}
				}
				Function countFn = factory.getFunctionFactory().fromLong(count);
				StateValues resValues = new StateValues(getNumStates(), getInitState(), countFn);
				result.add(region, resValues);
			}
		} else if (op == Region.FORALL) {
			for (Region region : regions) {
				StateValues vals = values.get(region);
				boolean forall = true;
				for (int state = 0; state < getNumStates(); state++) {
					if (whichStates.get(state)) {
						if (!vals.getStateValueAsBoolean(state)) {
							forall = false;
							break;
						}
					}
				}
				StateValues resValues = new StateValues(getNumStates(), getInitState(), forall);
				result.add(region, resValues);
			}
		} else if (op == Region.EXISTS) {
			for (Region region : regions) {
				StateValues vals = values.get(region);
				boolean exists = false;
				for (int state = 0; state < getNumStates(); state++) {
					if (whichStates.get(state)) {
						if (vals.getStateValueAsBoolean(state)) {
							exists = true;
							break;
						}
					}
				}
				StateValues resValues = new StateValues(getNumStates(), getInitState(), exists);
				result.add(region, resValues);
			}
		} else {
			throw new RuntimeException("unknown operator");
		}

		return result;
	}

	public boolean parameterIndependent()
	{
		simplify();
		if (regions.size() > 1) {
			return false;
		}
		if (!regions.get(0).volume().equals(factory.getFunctionFactory().getOne().asBigRational())) {
			return false;
		}

		return true;
	}

	public StateValues getStateValues()
	{
		return values.get(regions.get(0));
	}

	public void clearNotNeeded(BitSet needStates)
	{
		for (Region region : regions) {
			StateValues vals = values.get(region);
			for (int state = 0; state < vals.getNumStates(); state++) {
				if (!needStates.get(state)) {
					if (vals.getStateValue(state) instanceof Function) {
						vals.setStateValue(state, factory.getFunctionFactory().getZero());
					} else {
						vals.setStateValue(state, false);
					}
				}
			}
		}
		simplify();
	}

	public String filteredString(BitSet filter)
	{
		StringBuilder builder = new StringBuilder();

		for (Region region : regions) {
			builder.append(region);
			StateValues vals = values.get(region);
			for (int stateNr = filter.nextSetBit(0); stateNr >= 0; stateNr = filter.nextSetBit(stateNr + 1)) {
				builder.append(stateNr);
				builder.append(":");
				//				builder.append(statesList.get(stateNr).toString());
				builder.append("=");
				builder.append(vals.getStateValue(stateNr));
			}
		}

		return builder.toString();
	}

	public RegionValues unaryOp(int parserUnaryOpToRegionOp)
	{
		RegionValues result = new RegionValues(factory);
		for (Region region : regions) {
			StateValues value = unaryOp(parserUnaryOpToRegionOp, values.get(region));
			result.add(region, value);
		}
		return result;
	}

	private StateValues unaryOp(int op, StateValues stateValues)
	{
		StateValues result = new StateValues(getNumStates(), getInitState());
		for (int state = 0; state < stateValues.getNumStates(); state++) {
			StateValue value = null;
			switch (op) {
			case Region.UMINUS:
				value = stateValues.getStateValueAsFunction(state).negate();
				break;
			case Region.NOT:
				value = new StateBoolean(!stateValues.getStateValueAsBoolean(state));
				break;
			case Region.PARENTH:
				value = stateValues.getStateValue(state);
				break;
			}
			result.setStateValue(state, value);
		}
		return result;
	}
}
