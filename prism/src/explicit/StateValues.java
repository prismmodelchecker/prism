//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.BitSet;

import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;

/**
 * Class for explicit-state storage of a state-indexed vector of (integer or double) values.
 */
public class StateValues
{
	// Type (int, double or boolean)
	protected Type type;
	// Size
	protected int size;
	// Vector (only one used, depending on type)
	protected int[] valuesI;
	protected double[] valuesD;
	protected BitSet valuesB;

	// CONSTRUCTORS, etc.

	/**
	 * Construct a new empty state values vector of unspecified type.
	 * (Mostly for internal use.)
	 */
	public StateValues()
	{
		type = null;
		size = 0;
		valuesI = null;
		valuesD = null;
		valuesB = null;
	}

	/**
	 * Construct a new state values vector of the given type and size.
	 * All values are initially set to zero.
	 */
	public StateValues(Type type, int size) throws PrismLangException
	{
		this(type, size, type.defaultValue());
	}

	/**
	 * Construct a new state values vector of the given type and size,
	 * initialising all values to {@code init}.
	 * Throws an exception of {@code init} is of the wrong type.
	 * @param type Value type
	 * @param size Vector size
	 * @param init Initial value for all states (as an appropriate Object)
	 */
	public StateValues(Type type, int size, Object init) throws PrismLangException
	{
		super();
		int i;
		this.type = type;
		this.size = size;
		// Create/initialise array of appropriate type
		if (type instanceof TypeInt) {
			valuesI = new int[size];
			Integer objI = (Integer) type.castValueTo(init);
			int initI = objI.intValue();
			for (i = 0; i < size; i++)
				valuesI[i] = initI;
		} else if (type instanceof TypeDouble) {
			valuesD = new double[size];
			Double objD = (Double) type.castValueTo(init);
			double initD = objD.doubleValue();
			for (i = 0; i < size; i++)
				valuesD[i] = initD;
		} else if (type instanceof TypeBool) {
			Boolean objB = (Boolean) type.castValueTo(init);
			boolean initB = objB.booleanValue();
			if (initB) {
				valuesB = new BitSet(size);
				valuesB.set(0, size);
			} else {
				valuesB = new BitSet();
			}
		} else {
			throw new PrismLangException("Cannot create an vector of type " + type);
		}
	}

	/**
	 * Create a new (double-valued) state values vector from an existing array of doubles.
	 * The array is stored directly, not copied.
	 */
	public static StateValues createFromDoubleArray(double[] array)
	{
		StateValues sv = new StateValues();
		sv.type = TypeDouble.getInstance();
		sv.size = array.length;
		sv.valuesD = array;
		return sv;
	}

	/**
	 * Create a new (Boolean-valued) state values vector from an existing BitSet.
	 * The BitSet is stored directly, not copied.
	 */
	public static StateValues createFromBitSet(BitSet bs, int size)
	{
		StateValues sv = new StateValues();
		sv.type = TypeBool.getInstance();
		sv.size = size;
		sv.valuesB = bs;
		return sv;
	}

	/**
	 * Create a new (double-valued) state values vector from a BitSet,
	 * where each entry is 1.0 if in the bitset, 0.0 otherwise.
	 * The size must also be given since this is not explicit in the bitset.
	 * The bitset is not modified or stored.
	 */
	public static StateValues createFromBitSetAsDoubles(int size, BitSet bitset)
	{
		double[] array = new double[size];
		for (int i = 0; i < size; i++) {
			array[i] = bitset.get(i) ? 1.0 : 0.0;
		}
		return createFromDoubleArray(array);
	}

	// CONVERSION METHODS

	/*// convert to StateValuesDV (nothing to do)
	public StateValuesDV convertToStateValuesDV()
	{
		return this;
	}
	
	// convert to StateValuesMTBDD, destroy (clear) old vector
	public StateValuesMTBDD convertToStateValuesMTBDD()
	{
		StateValuesMTBDD res = new StateValuesMTBDD(values.convertToMTBDD(vars, odd), model);
		clear();
		return res;
	}*/

	/**
	 * Generate BitSet for states in the given interval
	 * (interval specified as relational operator and bound)
	 */
	public BitSet getBitSetFromInterval(String relOp, double bound)
	{
		BitSet sol = new BitSet();

		if (type instanceof TypeInt) {
			if (relOp.equals(">=")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] >= bound);
				}
			} else if (relOp.equals(">")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] > bound);
				}
			} else if (relOp.equals("<=")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] <= bound);
				}
			} else if (relOp.equals("<")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] < bound);
				}
			}
		} else if (type instanceof TypeDouble) {
			if (relOp.equals(">=")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] >= bound);
				}
			} else if (relOp.equals(">")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] > bound);
				}
			} else if (relOp.equals("<=")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] <= bound);
				}
			} else if (relOp.equals("<")) {
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] < bound);
				}
			}
		}

		return sol;
	}

	// METHODS TO MODIFY VECTOR

	public void setIntValue(int i, int val)
	{
		valuesI[i] = val;
	}

	public void setDoubleValue(int i, double val)
	{
		valuesD[i] = val;
	}

	public void setBooleanValue(int i, boolean val)
	{
		valuesB.set(i, val);
	}

	public void and(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.type instanceof TypeBool)) {
			throw new PrismException("Conjunction can only be applied to Boolean vectors");
		}
		valuesB.and(sv.valuesB);
	}
	
	// ...

	// clear (free memory)

	/**
	 * Clear the vector, i.e. free any used memory.
	 * (Well, actually, just set pointer to null and wait for later garbage collection.)
	 */
	public void clear()
	{
		valuesI = null;
		valuesD = null;
		valuesB = null;
	}

	// METHODS TO ACCESS VECTOR DATA

	/**
	 * Get the value of the ith element of the vector, as an Object.
	 */
	public Object getValue(int i)
	{
		if (type instanceof TypeInt) {
			return valuesI[i];
		} else if (type instanceof TypeDouble) {
			return valuesD[i];
		} else if (type instanceof TypeBool) {
			return valuesB.get(i);
		} else {
			return null;
		}
	}

	/**
	 * For Boolean-valued vectors, get the BitSet storing the data.
	 */
	public BitSet getBitSet()
	{
		return valuesB;
	}
	
	/*
	// get num non zeros
	
	public int getNNZ()
	{
		return values.getNNZ();
	}
	
	public String getNNZString()
	{
		return "" + getNNZ();
	}
	*/

	// PRINTING STUFF

	/**
	 * Print vector to a log/file (non-zero entries only)
	 */
	public void print(PrismLog log) throws PrismException
	{
		int i;
		for (i = 0; i < size; i++) {
			log.println(getValue(i));
		}
	}

	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		print(log); //TODO
	}

	/**
	 * Make a (deep) copy of this vector
	 */
	public StateValues deepCopy() throws PrismException
	{
		// TODO
		throw new PrismException("Not implemented yet");
	}
}
