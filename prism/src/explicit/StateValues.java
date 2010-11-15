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

import java.util.*;

import dv.DoubleVector;

import parser.type.*;
import prism.PrismException;

/**
 * Class for explicit-state storage of a state-indexed vector of (integer or double) values.
 */
public class StateValues
{
	// Type (int or double)
	protected Type type;
	// Size
	protected int size;
	// Vector (only one used, depending on type)
	protected int[] valuesI;
	protected double[] valuesD;
	
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
	}
	
	/**
	 * Construct a new state values vector of the given type and size.
	 * All values are initially set to zero.
	 */
	public StateValues(Type type, int size)
	{
		// TODO: check this: ? probably always returns Double due to typing
		this(type, size, type instanceof TypeInt ? new Integer(0) : new Double(0.0));
	}
	
	/**
	 * Construct a new state values vector of the given type and size,
	 * initialising all values to 'init'.
	 * @param type Value type (int/double)
	 * @param size Vector size
	 * @param init Initial value for all states (as Integer or Double object)
	 */
	public StateValues(Type type, int size, Object init)
	{
		int i, initI;
		double initD;
		this.type = type;
		this.size = size;
		valuesI = null;
		valuesD = null;
		// Create/initialise array of appropriate type
		if (type instanceof TypeInt) {
			valuesI = new int[size];
			initI = ((Integer)init).intValue();
			for (i = 0; i < size; i++)
				valuesI[i] = initI;
		} else if (type instanceof TypeDouble) {
			valuesD = new double[size];
			initD = ((Double)init).doubleValue();
			for (i = 0; i < size; i++)
				valuesD[i] = initD;
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
		sv.valuesI = null;
		sv.valuesD = array;
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
	
	// METHODS TO MODIFY VECTOR
	
	// ...
	
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
		} else {
			return null;
		}
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
	
	/**
	 * Make a (deep) copy of this vector
	 */
	public StateValues deepCopy() throws PrismException
	{
		// TODO
		throw new PrismException("Not impleneted yet");
	}
}
