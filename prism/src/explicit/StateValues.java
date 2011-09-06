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
import java.util.List;

import parser.State;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismUtils;

/**
 * Class for explicit-state storage of a state-indexed vector of values (int, double, boolean).
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
	
	// Model info
	protected List<State> statesList;

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

	/**
	 * Generate BitSet for states whose value is close to 'value'
	 * (within either absolute or relative error 'epsilon')
	 * The type of 'value' is assumed to match that of the vector.
	 */
	public BitSet getBitSetFromCloseValue(Object value, double epsilon, boolean abs)
	{
		BitSet sol = new BitSet();

		if (type instanceof TypeInt) {
			int valueI = ((Integer) value).intValue();
			for (int i = 0; i < size; i++) {
				sol.set(i, PrismUtils.doublesAreClose(valuesI[i], valueI, epsilon, abs));
			}
		} else if (type instanceof TypeDouble) {
			double valueD = ((Double) value).doubleValue();
			for (int i = 0; i < size; i++) {
				sol.set(i, PrismUtils.doublesAreClose(valuesD[i], valueD, epsilon, abs));
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
	 * Is the ith element of the vector non-zero (or non-false)?
	 */
	public boolean isNonZero(int i)
	{
		if (type instanceof TypeInt) {
			return valuesI[i] != 0;
		} else if (type instanceof TypeDouble) {
			return valuesD[i] != 0.0;
		} else if (type instanceof TypeBool) {
			return valuesB.get(i);
		} else {
			return false;
		}
	}

	/**
	 * For Boolean-valued vectors, get the BitSet storing the data.
	 */
	public BitSet getBitSet()
	{
		return valuesB;
	}

	/**
	 * Get the number of states for which the value is non-zero/non-false.
	 */
	public int getNNZ()
	{
		int count = 0;
		if (type instanceof TypeBool) {
			count = valuesB.cardinality();
		} else {
			for (int i = 0; i < size; i++) {
				if (isNonZero(i))
					count++;
			}
		}
		return count;
	}

	/**
	 * Get (as a string) the number of states for which the value is non-zero/non-false.
	 */
	public String getNNZString()
	{
		return "" + getNNZ();
	}

	// Filter operations

	/**
	 * Get the value of first vector element that is in the (BitSet) filter.
	 */
	public Object firstFromBitSet(BitSet filter)
	{
		return getValue(filter.nextSetBit(0));
	}

	/**
	 * Get the minimum value of those that are in the (BitSet) filter.
	 */
	public Object minOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeInt) {
			int minI = Integer.MAX_VALUE;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (valuesI[i] < minI)
					minI = valuesI[i];
			}
			return new Integer(minI);
		} else if (type instanceof TypeDouble) {
			double minD = Double.POSITIVE_INFINITY;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (valuesD[i] < minD)
					minD = valuesD[i];
			}
			return new Double(minD);
		}
		throw new PrismException("Can't take min over a vector of type " + type);
	}

	/**
	 * Get the maximum value of those that are in the (BitSet) filter.
	 */
	public Object maxOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeInt) {
			int maxI = Integer.MIN_VALUE;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (valuesI[i] > maxI)
					maxI = valuesI[i];
			}
			return new Integer(maxI);
		} else if (type instanceof TypeDouble) {
			double maxD = Double.NEGATIVE_INFINITY;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (valuesD[i] > maxD)
					maxD = valuesD[i];
			}
			return new Double(maxD);
		}
		throw new PrismException("Can't take max over a vector of type " + type);
	}

	/**
	 * Check if value is true for all states in the (BitSet) filter.
	 */
	public boolean forallOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeBool) {
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (!valuesB.get(i))
					return false;
			}
			return true;
		}
		throw new PrismException("Can't take for-all over a vector of type " + type);
	}

	/**
	 * Check if there exists a true value for some state in the (BitSet) filter.
	 */
	public boolean existsOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeBool) {
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (valuesB.get(i))
					return true;
			}
			return false;
		}
		throw new PrismException("Can't take there-exists over a vector of type " + type);
	}

	/**
	 * Count the number of states with value true from those in the (BitSet) filter.
	 */
	public int countOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeBool) {
			int count = 0;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				if (valuesB.get(i))
					count++;
			}
			return new Integer(count);
		}
		throw new PrismException("Can't take count over a vector of type " + type);
	}

	/**
	 * Get the sum of values for states that are in the (BitSet) filter.
	 */
	public Object sumOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeInt) {
			int sumI = 0;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				sumI += valuesI[i];
			}
			return new Integer(sumI);
		} else if (type instanceof TypeDouble) {
			double sumD = 0.0;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				sumD += valuesD[i];
			}
			return new Double(sumD);
		}
		throw new PrismException("Can't take sum over a vector of type " + type);
	}

	/**
	 * Get the average of values for states that are in the (BitSet) filter.
	 */
	public double averageOverBitSet(BitSet filter) throws PrismException
	{
		if (type instanceof TypeInt) {
			int sumI = 0;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				sumI += valuesI[i];
			}
			return ((double) sumI) / filter.cardinality();
		} else if (type instanceof TypeDouble) {
			double sumD = 0.0;
			for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
				sumD += valuesD[i];
			}
			return sumD / filter.cardinality();
		}
		throw new PrismException("Can't take average over a vector of type " + type);
	}

	// PRINTING STUFF

	/**
	 * Print vector to a log/file (non-zero entries only)
	 */
	public void print(PrismLog log) throws PrismException
	{
		print(log, true, false, true);
	}

	/**
	 * Print vector to a log/file.
	 * @param log The log
	 * @param printSparse Print non-zero elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 */
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		int i;
		
		// Header for Matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + size + ",1);");
		
		// Check if all zero
		if (printSparse && !printMatlab && getNNZ() == 0) {
			log.println("(all zero)");
			return;
		}

		// Print vector
		for (i = 0; i < size; i++) {
			if (!printSparse || isNonZero(i)) {
				if (printSparse)
					log.print(printMatlab ? "v(" + (i + 1) + ")" : i);
				if (printStates && !printMatlab && statesList != null) {
					log.print(":" + statesList.get(i).toString());
				}
				if (printSparse)
					log.print("=");
				log.print(getValue(i));
				if (printMatlab && printSparse)
					log.print(";");
				log.println();
			}
		}
		
		// Footer for Matlab format
		if (printMatlab && !printSparse)
			log.println("];");
	}

	/**
	 * Make a (deep) copy of this vector
	 */
	public StateValues deepCopy() throws PrismException
	{
		StateValues sv = new StateValues();
		sv.type = type;
		sv.size = size;
		if (valuesI != null) {
			sv.valuesI = Utils.cloneIntArray(valuesI);
		}
		if (valuesD != null) {
			sv.valuesD = Utils.cloneDoubleArray(valuesD);
		}
		if (valuesB != null) {
			sv.valuesB = (BitSet) valuesB.clone();
		}
		sv.statesList = statesList;
		return sv;
	}
}
