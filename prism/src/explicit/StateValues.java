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
	 * Construct a new state values vector of the given type.
	 * All values are initially set to zero/false.
	 * Also set associated model (and this determines the vector size).
	 * @param type Value type
	 * @param model Associated model 
	 */
	public StateValues(Type type, Model model) throws PrismLangException
	{
		this(type, model.getNumStates());
		statesList = model.getStatesList();
	}

	/**
	 * Construct a new state values vector of the given type and size.
	 * All values are initially set to zero/false.
	 */
	public StateValues(Type type, int size) throws PrismLangException
	{
		this(type, size, type.defaultValue());
	}

	/**
	 * Construct a new state values vector of the given type, initialising all values to {@code init}.
	 * Also set associated model (and this determines the vector size).
	 * Throws an exception of {@code init} is of the wrong type.
	 * @param type Value type
	 * @param init Initial value for all states (as an appropriate Object)
	 * @param model Associated model 
	 */
	public StateValues(Type type, Object init, Model model) throws PrismLangException
	{
		this(type, model.getNumStates(), init);
		statesList = model.getStatesList();
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
	 * Also set associated model (whose state space size should match vector size).
	 */
	public static StateValues createFromDoubleArray(double[] array, Model model)
	{
		StateValues sv = new StateValues();
		sv.type = TypeDouble.getInstance();
		sv.size = array.length;
		sv.valuesD = array;
		sv.statesList = model.getStatesList();
		return sv;
	}

	/**
	 * Create a new (Boolean-valued) state values vector from an existing BitSet.
	 * The BitSet is stored directly, not copied.
	 * Also set associated model (and this determines the vector size).
	 */
	public static StateValues createFromBitSet(BitSet bs, Model model)
	{
		StateValues sv = new StateValues();
		sv.type = TypeBool.getInstance();
		sv.size = model.getNumStates();
		sv.valuesB = bs;
		sv.statesList = model.getStatesList();
		return sv;
	}

	/**
	 * Create a new (double-valued) state values vector from a BitSet,
	 * where each entry is 1.0 if in the bitset, 0.0 otherwise.
	 * Also set associated model (and this determines the vector size).
	 * The bitset is not modified or stored.
	 */
	public static StateValues createFromBitSetAsDoubles(BitSet bitset, Model model)
	{
		int size = model.getNumStates();
		double[] array = new double[size];
		for (int i = 0; i < size; i++) {
			array[i] = bitset.get(i) ? 1.0 : 0.0;
		}
		return createFromDoubleArray(array, model);
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
	 * Print vector to a log/file (non-zero/non-false entries only).
	 */
	public void print(PrismLog log) throws PrismException
	{
		doPrinting(log, -1, null, true, false, true, true);
	}

	/**
	 * Print up to {@code limit} entries of a vector to a log/file (non-zero/non-false entries only).
	 */
	public void print(PrismLog log, int limit) throws PrismException
	{
		doPrinting(log, limit, null, true, false, true, true);
	}

	/**
	 * Print vector to a log/file.
	 * @param log The log
	 * @param printSparse Print non-zero/non-false elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 * @param printIndices Print state indices for each element? 
	 */
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		doPrinting(log, -1, null, printSparse, printMatlab, printStates, printIndices);
	}

	/**
	 * Print part of vector to a log/file (non-zero/non-false entries only).
	 * @param log The log
	 * @param filter A BitSet specifying which states to print for.
	 */
	public void printFiltered(PrismLog log, BitSet filter) throws PrismException
	{
		doPrinting(log, -1, filter, true, false, true, true);
	}

	/**
	 * Print part of vector to a log/file.
	 * @param log The log
	 * @param filter A BitSet specifying which states to print for (null if all).
	 * @param printSparse Print non-zero/non-false elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 * @param printIndices Print state indices for each element? 
	 */
	public void printFiltered(PrismLog log, BitSet filter, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
			throws PrismException
	{
		doPrinting(log, -1, filter, printSparse, printMatlab, printStates, printIndices);
	}

	/**
	 * Print part of vector to a log/file.
	 * @param log The log
	 * @param limit Maximum number of entries to print (-1 = no limit)
	 * @param filter A BitSet specifying which states to print for (null if all).
	 * @param printSparse Print non-zero/non-false elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 * @param printIndices Print state indices for each element? 
	 */
	private void doPrinting(PrismLog log, int limit, BitSet filter, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
			throws PrismException
	{
		int i, count = 0;

		if (limit == -1)
			limit = Integer.MAX_VALUE;
		
		// Header for Matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + size + ",1);");

		// Print vector
		if (filter == null) {
			for (i = 0; i < size & count < limit; i++) {
				if (printLine(log, i, printSparse, printMatlab, printStates, printIndices))
					count++;
			}
		} else {
			for (i = filter.nextSetBit(0); i >= 0 && count < limit; i = filter.nextSetBit(i + 1)) {
				if (printLine(log, i, printSparse, printMatlab, printStates, printIndices))
					count++;
			}
		}

		// Check if all zero
		if (printSparse && !printMatlab && count == 0) {
			log.println("(all zero)");
			return;
		}

		// Footer for Matlab format
		if (printMatlab && !printSparse)
			log.println("];");
	}

	private boolean printLine(PrismLog log, int i, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		if (!printSparse || isNonZero(i)) {
			if (printMatlab) {
				if (printSparse) {
					log.println("v(" + (i + 1) + ")=" + getValue(i) + ";");
				} else {
					log.println(getValue(i));
				}
			} else {
				if (printIndices)
					log.print(i);
				if (printStates && statesList != null)
					log.print(":" + statesList.get(i).toString());
				if (printSparse && type instanceof TypeBool) {
					log.println();
				} else {
					if (printIndices || printStates)
						log.print("=");
					log.println(getValue(i));
				}
			}
			return true;
		} else {
			return false;
		}
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
