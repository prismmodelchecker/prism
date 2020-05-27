//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;

import common.IterableStateSet;
import common.iterable.FunctionalPrimitiveIterator;
import parser.State;
import parser.ast.ExpressionFilter;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.Accuracy;
import prism.AccuracyFactory;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.ResultTesting;
import prism.StateVector;

/**
 * Class for explicit-state storage of a state-indexed vector of values.
 * <br><br>
 * The values can be of any supported {@link Type}.
 * They are stored as the corresponding {@link Object}
 * as specified by the return type of {@link Type#castValueTo(Object)}.
 */
public class StateValues implements StateVector, Iterable<Object>
{
	// Vector info
	
	/** Type of value stored */
	protected Type type;
	/** Size of vector */
	protected int size;
	/** Computed accuracy of stored values (optional) */
	public Accuracy accuracy = null;
	
	// Model info
	
	/** Corresponding list of State objects */
	protected List<State> statesList;

	// Vector storage (only one used, depending on type)
	
	/** Specialised storage for boolean values */
	protected BitSet valuesB;
	/** General purpose storage for other value types */
	protected Object[] valuesO;
	
	// Functional interfaces
	
	@FunctionalInterface
	public interface Predicate
	{
		public boolean test(Object v) throws PrismException;
	}
	
	@FunctionalInterface
	public interface UnaryFunction
	{
		public Object apply(Object v) throws PrismException;
	}
	
	@FunctionalInterface
	public interface BinaryFunction
	{
		public Object apply(Object v1, Object v2) throws PrismException;
	}
	
	@FunctionalInterface
	public interface TernaryFunction
	{
		public Object apply(Object v1, Object v2, Object v3) throws PrismException;
	}
	
	@FunctionalInterface
	public interface ValueDefinition
	{
		public Object apply(int i) throws PrismException;
	}
	
	// Constructors

	/**
	 * Construct a new empty state values vector of unspecified type.
	 * (Mostly for internal use.)
	 */
	public StateValues()
	{
		type = null;
		size = 0;
		valuesB = null;
		valuesO = null;
	}

	/**
	 * Construct a new state values vector for values of a specified type.
	 * The values for states are provided as a mapping from integer index to Object.
	 * The passed in model determines the vector size (and states list).
	 */
	public StateValues(Type type, ValueDefinition values, Model<?> model) throws PrismException
	{
		initialise(type, model);
		setFromValueDefinition(values);
	}
	
	/**
	 * Construct a new state values vector with the same value in every state.
	 * The passed in model determines the vector size (and states list).
	 */
	public StateValues(Type type, Object value, Model<?> model) throws PrismException
	{
		initialise(type, model);
		setToSingleValue(value);
	}

	/**
	 * Construct a new state values vector for values of a specified type.
	 * The value for all states is set to the default value for the type.
	 * The passed in model determines the vector size (and states list).
	 */
	public StateValues(Type type, Model<?> model) throws PrismException
	{
		this(type, type.defaultValue(), model);
	}

	// Utility methods for initialisation/construction
	
	/**
	 * Initialise the vector/model info (but not the value storage)
	 */
	private void initialise(Type type, Model<?> model)
	{
		this.type = type;
		this.size = model.getNumStates();
		this.statesList = model.getStatesList();
		this.valuesB = null;
		this.valuesO = null;
	}

	/**
	 * Initialise the vector/model info (but not the value storage)
	 */
	private void initialise(Type type, List<State> statesList)
	{
		this.type = type;
		this.size = statesList.size();
		this.statesList = statesList;
		this.valuesB = null;
		this.valuesO = null;
	}

	/**
	 * Initialise value storage based on a ValueDefinition
	 */
	private void setFromValueDefinition(ValueDefinition values) throws PrismException
	{
		// Assumed to be called straight after initialise
		// (so size/type is known, but all storage is null)
		initStorage(type);
		for (int i = 0; i < size; i++) {
			setValue(i, values.apply(i));
		}
	}

	/**
	 * Initialise value storage based on a single value
	 */
	private void setToSingleValue(Object value) throws PrismException
	{
		// Assumed to be called straight after initialise
		// (so size is known, but all storage is null)
		initStorage(type);
		if (type instanceof TypeBool) {
			if ((boolean) value) {
				valuesB = new BitSet(size);
				valuesB.set(0, size);
			} else {
				valuesB = new BitSet();
			}
		} else {
			for (int i = 0; i < size; i++) {
				valuesO[i] = value;
			}
		}
	}
	
	/**
	 * Create (or recreate) the storage for values of the specified type
	 * Uses existing storage if possible (assumes no change in size)
	 */
	private void initStorage(Type typeNew)
	{
		if (typeNew instanceof TypeBool) {
			if (valuesB == null) {
				valuesB = new BitSet();
			}
		} else {
			if (valuesO == null) {
				valuesO = new Object[size];
			}
		}
	}
	
	/**
	 * Cleared un-needed value storage (if the type has changed)
	 */
	private void clearOldStorage()
	{
		if (type != null) {
			if (type instanceof TypeBool) {
				valuesO = null;
			} else {
				valuesB = null;
			}
		}
	}
	
	// Static creation methods
	
	/**
	 * Create a new state values vector for values of a specified type.
	 * The values for states are provided as a mapping from integer index to Object.
	 * The passed in model determines the vector size (and states list).
	 */
	public static StateValues create(Type type, ValueDefinition values, Model<?> model) throws PrismException
	{
		StateValues sv = new StateValues();
		sv.initialise(type, model);
		sv.setFromValueDefinition(values);
		return sv;
	}
	
	/**
	 * Create a new state values vector for values of a specified type,
	 * where the value is the same for every state.
	 * The passed in model determines the vector size (and states list).
	 */
	public static StateValues createFromSingleValue(Type type, Object value, Model<?> model) throws PrismException
	{
		StateValues sv = new StateValues();
		sv.initialise(type, model);
		sv.setToSingleValue(value);
		return sv;
	}

	/**
	 * Create a new state values vector from an existing array of values.
	 * The array is stored directly, not copied.
	 * Also set associated model (whose state space size should match vector size).
	 */
	public static StateValues createFromObjectArray(Type type, Object[] array, Model<?> model)
	{
		StateValues sv = new StateValues();
		sv.initialise(type, model);
		sv.valuesO = array;
		return sv;
	}

	/**
	 * Create a new (double-valued) state values vector from an existing array of doubles.
	 * Also set associated model (whose state space size should match vector size).
	 */
	public static StateValues createFromDoubleArray(double[] array, Model<?> model) throws PrismException
	{
		return create(TypeDouble.getInstance(), i -> array[i], model);
	}

	/**
	 * Create a new (double-valued) state values vector from an existing array of doubles.
	 */
	public static StateValues createFromDoubleArray(double[] array, List<State> statesList) throws PrismException
	{
		StateValues sv = new StateValues();
		sv.initialise(TypeDouble.getInstance(), statesList);
		sv.setFromValueDefinition(i -> array[i]);
		return sv;
	}

	/**
	 * Create a new (double-valued) state values vector from an existing array of doubles,
	 * stored in a ModelCheckerResult object. Accuracy information is also extracted.
	 * Also set associated model (whose state space size should match vector size).
	 */
	public static StateValues createFromDoubleArrayResult(ModelCheckerResult res, Model<?> model) throws PrismException
	{
		StateValues sv = createFromDoubleArray(res.soln, model);
		sv.setAccuracy(res.accuracy);
		return sv;
	}
	
	/**
	 * Create a new (Boolean-valued) state values vector from an existing BitSet.
	 * The BitSet is stored directly, not copied.
	 * Also set associated model (and this determines the vector size).
	 */
	public static StateValues createFromBitSet(BitSet bs, Model<?> model)
	{
		StateValues sv = new StateValues();
		sv.initialise(TypeBool.getInstance(), model);
		sv.valuesB = bs;
		return sv;
	}

	/**
	 * Create a new (double-valued) state values vector from a BitSet,
	 * where each entry is 1.0 if in the bitset, 0.0 otherwise.
	 * Also set associated model (and this determines the vector size).
	 * The bitset is not modified or stored.
	 * The accuracy for the result is also set automatically.
	 */
	public static StateValues createFromBitSetAsDoubles(BitSet bitset, Model<?> model) throws PrismException
	{
		StateValues sv = create(TypeDouble.getInstance(), i -> bitset.get(i) ? 1.0 : 0.0, model);
		sv.setAccuracy(AccuracyFactory.doublesFromQualitative());
		return sv;
	}
	
	/**
	 * Create a new state values vector, reading in the values from a file.
	 */
	public static StateValues createFromFile(Type type, File file, Model<?> model) throws PrismException
	{
		StateValues sv = new StateValues();
		sv.initialise(type, model);
		sv.readFromFile(file);
		return sv;
	}
	
	// Other methods to create new vectors
	
	/**
	 * Create a new StateValues, copied from this one, but mapped to a new model.
	 * @param newModel The new model
	 * @param reverseStateMapping Mapping from indices of the new model to the old one
	 */
	public StateValues mapToNewModel(Model<?> newModel, IntFunction<Integer> reverseStateMapping) throws PrismException
	{
		int numStates = newModel.getNumStates();
		StateValues sv = create(type, i -> {
			final Integer j = reverseStateMapping.apply(i);
			if (j != null) {
				if (j >= numStates) {
					throw new IndexOutOfBoundsException("State index error when mapping between models");
				}
				return getValue(j);
			} else {
				return type.defaultValue();
			}
		}, newModel);
		sv.setAccuracy(getAccuracy());
		return sv;
	}

	/**
	 * Create a new StateValues, copied from this one, but mapped to the original model
	 * that was used to construct a product. This function assumes that the product model
	 * has at most one initial state per state in the original model. The value of this
	 * state is then projected to the corresponding state of the original model.
	 * @param product The product object
	 */
	public StateValues projectToOriginalModel(final Product<?> product) throws PrismException
	{
		Model<?> productModel = product.getProductModel();
		Model<?> originalModel = product.getOriginalModel();
		StateValues sv = new StateValues(type, originalModel);
		for (int productState : productModel.getInitialStates()) {
			int modelState = product.getModelState(productState);
			sv.setValue(modelState, getValue(productState));
		}
		sv.setAccuracy(getAccuracy());
		return sv;
	}
	
	// Other set methods
	
	/**
	 * Set the accuracy.
	 */
	public void setAccuracy(Accuracy accuracy)
	{
		this.accuracy = accuracy;
	}
	
	// Methods to modify vector values
	
	@Override
	public void clear()
	{
		// Actually, just set pointers to null and wait for later garbage collection.
		valuesB = null;
		valuesO = null;
	}
	
	/**
	 * Set the value for state index {@code i} to {@code value}.
	 * The type of Object passed in for {@code value} should be the
	 * appropriate one for the type of this StateValues.
	 */
	public void setValue(int i, Object value) throws PrismLangException
	{
		if (type instanceof TypeBool) {
			valuesB.set(i, (boolean) value);
		} else {
			valuesO[i] = value;
		}
	}

	/**
	 * Set the value for state index {@code i} to {@code value},
	 * assuming that the type of the StateValues is going to become
	 * {@code typeOverride}, even if it is not right now.
	 * The type of Object passed in for {@code value} should be the
	 * appropriate one for {@code typeOverride}.
	 * For internal use.
	 */
	private void setValue(int i, Object value, Type typeOverride) throws PrismLangException
	{
		if (typeOverride instanceof TypeBool) {
			valuesB.set(i, (boolean) value);
		} else {
			valuesO[i] = value;
		}
	}

	/**
	 * Modify the vector by applying (pointwise) a predicate, i.e., each element
	 * of the vector will become the result of applying the predicate to itself.
	 * @param predicate Predicate definition
	 */
	public void applyPredicate(Predicate predicate) throws PrismException
	{
		initStorage(TypeBool.getInstance());
		valuesB = getBitSetFromPredicate(predicate);
		type = TypeBool.getInstance();
		clearOldStorage();
	}
	
	/**
	 * Modify the vector by applying (pointwise) a unary function.
	 * @param retType Function return type
	 * @param func Function definition
	 */
	public void applyFunction(Type retType, UnaryFunction func) throws PrismException
	{
		initStorage(retType);
		for (int i = 0; i < size; i++) {
			setValue(i, func.apply(getValue(i)), retType);
		}
		type = retType;
		clearOldStorage();
	}

	/**
	 * Modify the vector by applying (pointwise) a unary function, only over the states in {@code subset}.
	 * @param retType Function return type
	 * @param func Function definition
	 * @param subset Subset of states for application (all if null)
	 */
	public void applyFunction(Type retType, UnaryFunction func, BitSet subset) throws PrismException
	{
		initStorage(retType);
		for (FunctionalPrimitiveIterator.OfInt iter = new IterableStateSet(subset, size).iterator(); iter.hasNext();) {
			int i = iter.nextInt();
			setValue(i, func.apply(getValue(i)), retType);
		}
		type = retType;
		clearOldStorage();
	}

	/**
	 * Modify the vector by applying (pointwise) a binary function
	 * to this and another vector.
	 * @param retType Function return type
	 * @param func Function definition
	 * @param sv2 Vector 2
	 */
	public void applyFunction(Type retType, BinaryFunction func, StateValues sv2) throws PrismException
	{
		initStorage(retType);
		for (int i = 0; i < size; i++) {
			setValue(i, func.apply(getValue(i), sv2.getValue(i)), retType);
		}
		type = retType;
		clearOldStorage();
	}
	
	/**
	 * Modify the vector by applying (pointwise) a binary function
	 * to this and another vector, only over the states in {@code subset}.
	 * @param retType Function return type
	 * @param func Function definition
	 * @param sv2 Vector 2
	 * @param subset Subset of states for application (all if null)
	 */
	public void applyFunction(Type retType, BinaryFunction func, StateValues sv2, BitSet subset) throws PrismException
	{
		initStorage(retType);
		for (FunctionalPrimitiveIterator.OfInt iter = new IterableStateSet(subset, size).iterator(); iter.hasNext();) {
			int i = iter.nextInt();
			setValue(i, func.apply(getValue(i), sv2.getValue(i)), retType);
		}
		type = retType;
		clearOldStorage();
	}
	
	/**
	 * Modify the vector by applying (pointwise) a ternary function
	 * to this and two other vectors.
	 * @param retType Function return type
	 * @param func Function definition
	 * @param sv2 Vector 2
	 * @param sv3 Vector 3
	 */
	public void applyFunction(Type retType, TernaryFunction func, StateValues sv2, StateValues sv3) throws PrismException
	{
		initStorage(retType);
		for (int i = 0; i < size; i++) {
			setValue(i, func.apply(getValue(i), sv2.getValue(i), sv3.getValue(i)), retType);
		}
		type = retType;
		clearOldStorage();
	}
	
	/**
	 * Modify the vector by applying (pointwise) a ternary function
	 * to this and two other vectors, only over the states in {@code subset}.
	 * @param retType Function return type
	 * @param func Function definition
	 * @param sv2 Vector 2
	 * @param sv3 Vector 3
	 * @param subset Subset of states for application (all if null)
	 */
	public void applyFunction(Type retType, TernaryFunction func, StateValues sv2, StateValues sv3, BitSet subset) throws PrismException
	{
		initStorage(retType);
		for (FunctionalPrimitiveIterator.OfInt iter = new IterableStateSet(subset, size).iterator(); iter.hasNext();) {
			int i = iter.nextInt();
			setValue(i, func.apply(getValue(i), sv2.getValue(i), sv3.getValue(i)), retType);
		}
		type = retType;
		clearOldStorage();
	}
	
	/**
	 * Set the elements of this vector by reading them in from a file.
	 * The values in the file should match the existing type of this StateValues.
	 */
	public void readFromFile(File file) throws PrismException
	{
		initStorage(type);
		int lineNum = 0, count = 0;
		boolean hasIndices = false;
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			String s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (!("".equals(s))) {
					// If entry is of form "i=x", use i as index not count
					// (otherwise, assume line i contains value for state index i)
					if (s.contains("=")) {
						hasIndices = true;
						String ss[] = s.split("=");
						count = Integer.parseInt(ss[0]);
						s = ss[1];
					}
					if (count + 1 > size) {
						in.close();
						throw new PrismException("Too many values in file \"" + file + "\" (more than " + size + ")");
					}
					if (type instanceof TypeInt) {
						setValue(count, Integer.parseInt(s));
					} else if (type instanceof TypeDouble) {
						setValue(count, Double.parseDouble(s));
					} else if (type instanceof TypeBool) {
						setValue(count, Boolean.parseBoolean(s));
					}
					count++;
				}
				s = in.readLine();
				lineNum++;
			}
			// Check size
			if (!hasIndices && count < size) {
				throw new PrismException("Too few values in file \"" + file + "\" (" + count + ", not " + size + ")");
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of file \"" + file + "\"");
		}
	}

	// Methods to modify vector values storing Boolean values
	
	/**
	 * Modify the vector by applying 'implies' with operand {@code sv}.
	 */
	public void implies(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.getType() instanceof TypeBool)) {
			throw new PrismException("Operator => can only be applied to Boolean vectors");
		}
		valuesB.flip(0, size);
		valuesB.or(sv.valuesB);
	}

	/**
	 * Modify the vector by applying 'iff' with operand {@code sv}.
	 */
	public void iff(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.getType() instanceof TypeBool)) {
			throw new PrismException("Operator <=> can only be applied to Boolean vectors");
		}
		valuesB.xor(sv.valuesB);
		valuesB.flip(0, size);
	}

	/**
	 * Modify the vector by applying 'or' with operand {@code sv}.
	 */
	public void or(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.getType() instanceof TypeBool)) {
			throw new PrismException("Operator | can only be applied to Boolean vectors");
		}
		valuesB.or(sv.valuesB);
	}

	/**
	 * Modify the vector by applying 'and' with operand {@code sv}.
	 */
	public void and(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.getType() instanceof TypeBool)) {
			throw new PrismException("Operator & can only be applied to Boolean vectors");
		}
		valuesB.and(sv.valuesB);
	}

	/**
	 * Complement the (boolean) vector.
	 */
	public void complement() throws PrismException
	{
		if (!(type instanceof TypeBool)) {
			throw new PrismException("Can only complement Boolean vectors");
		}

		valuesB.flip(0, size);
	}
	
	/**
	 * Modify the vector by applying 'not'
	 */
	public void not() throws PrismException
	{
		if (!(type instanceof TypeBool)) {
			throw new PrismException("Operator ! can only be applied to Boolean vectors");
		}
		valuesB.flip(0, size);
	}

	// Accessors
	
	/**
	 * Get the type of the values stored
	 */
	public Type getType()
	{
		return type;
	}
	
	@Override
	public int getSize()
	{
		return size;
	}

	/**
	 * Get the accuracy.
	 */
	public Accuracy getAccuracy()
	{
		return accuracy;
	}

	@Override
	public Object getValue(int i)
	{
		if (type instanceof TypeBool) {
			return valuesB.get(i);
		} else {
			return valuesO[i];
		}
	}

	/**
	 * Get the value of first vector element that is in the (BitSet) filter.
	 */
	public Object firstFromBitSet(BitSet filter)
	{
		return getValue(filter.nextSetBit(0));
	}

	@Override
	public Iterator<Object> iterator()
	{
		return new Iterator<Object>()
		{
			int i = 0;
			
			@Override
			public Object next()
			{
				return getValue(i++);
			}
			
			@Override
			public boolean hasNext()
			{
				return i < size;
			}
		};
	}
	
	/**
	 * Return a filtered view of this vector, only including
	 * the values for states whose index is set in {@code filter}.
	 */
	public Iterable<Object> filtered(BitSet filter)
	{
		return new Iterable<Object>()
		{
			@Override
			public Iterator<Object> iterator()
			{
				return new Iterator<Object>()
				{
					int i = filter.nextSetBit(0);
					
					@Override
					public Object next()
					{
						Object o = getValue(i);
						i = filter.nextSetBit(i + 1);
						return o;
					}
					
					@Override
					public boolean hasNext()
					{
						return i >= 0;
					}
				};
			}
		};
	}
	
	/**
	 * For Boolean-valued vectors, get the BitSet storing the data.
	 */
	public BitSet getBitSet()
	{
		return valuesB;
	}

	/**
	 * For double-valued vectors, get a double array storing the data.
	 */
	public double[] getDoubleArray()
	{
		double[] valuesD = new double[size];
		for (int i = 0; i < size; i++) {
			valuesD[i] = (double) valuesO[i];
		}
		return valuesD;
	}

	/**
	 * Get a BitSet representing the states which satisfy a predicate.
	 */
	public BitSet getBitSetFromPredicate(Predicate predicate) throws PrismException
	{
		BitSet bs = new BitSet();
		for (int i = 0; i < size; i++) {
			bs.set(i, predicate.test(getValue(i)));
		}
		return bs;
	}
	
	/**
	 * Get a BitSet for the states whose value is (approximately) equal to {@code value}.
	 * For double values, equality is only checked approximately, either using the stored
	 * accuracy for the vector, if present, or a default accuracy level otherwise.
	 * For other types, values must be identical.
	 * The type of {@code value} is assumed to match that of the vector.
	 */
	public BitSet getBitSetFromCloseValue(Object value) throws PrismException
	{
		Accuracy accuracy = ResultTesting.getTestingAccuracy(getAccuracy());
		return getBitSetFromCloseValue(value, accuracy);
	}

	/**
	 * Get a BitSet for the states whose value is (approximately) equal to {@code value}.
	 * For double values, equality is only checked approximately, either using the specified
	 * accuracy (within either absolute or relative error {@code epsilon}).
	 * The type of {@code value} is assumed to match that of the vector.
	 */
	public BitSet getBitSetFromCloseValue(Object value, double epsilon, boolean abs) throws PrismException
	{
		Accuracy accuracy = new Accuracy(Accuracy.AccuracyLevel.BOUNDED, epsilon, abs);
		return getBitSetFromCloseValue(value, accuracy);
	}

	/**
	 * Get a BitSet for the states whose value is (approximately) equal to {@code value}.
	 * For double values, equality is only checked approximately, either using the accuracy.
	 * For other types, values must be identical.
	 * The type of {@code value} is assumed to match that of the vector.
	 */
	public BitSet getBitSetFromCloseValue(Object value, Accuracy accMatch) throws PrismException
	{
		return getBitSetFromPredicate(v -> ExpressionFilter.isClose(v, value, getType(), accMatch));
	}

	// Printing methods

	/**
	 * Print vector to a log/file (non-zero/non-false entries only).
	 */
	public void print(PrismLog log)
	{
		doPrinting(log, -1, null, true, false, true, true);
	}

	/**
	 * Print up to {@code limit} entries of a vector to a log/file (non-zero/non-false entries only).
	 */
	public void print(PrismLog log, int limit)
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
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
	{
		doPrinting(log, -1, null, printSparse, printMatlab, printStates, printIndices);
	}

	/**
	 * Print part of vector to a log/file (non-zero/non-false entries only).
	 * @param log The log
	 * @param filter A BitSet specifying which states to print for.
	 */
	public void printFiltered(PrismLog log, BitSet filter)
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
			log.println(type == TypeBool.getInstance() ? "(none)" : "(all zero)");
			return;
		}

		// Footer for Matlab format
		if (printMatlab && !printSparse)
			log.println("];");
	}

	private boolean printLine(PrismLog log, int n, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
	{
		if (!printSparse || isValueNonZero(getValue(n))) {
			if (printMatlab) {
				if (printSparse) {
					log.println("v(" + (n + 1) + ")=" + getValue(n) + ";");
				} else {
					log.println(getValue(n));
				}
			} else {
				if (printIndices) {
					log.print(n);
				}
				if (printStates && statesList != null) {
					if (printIndices) {
						log.print(":");
					}
					log.print(statesList.get(n).toString());
				}
				if (printSparse && type instanceof TypeBool) {
					log.println();
				} else {
					if (printIndices || printStates) {
						log.print("=");
					}
					log.println(getValue(n));
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Is the given vector value non-zero/non-false? (for sparse printing)
	 */
	private boolean isValueNonZero(Object value)
	{
		if (value instanceof Integer) {
			return ((int) value) != 0;
		} else if (value instanceof Double) {
			return ((double) value) != 0.0;
		} else if (value instanceof Boolean) {
			return (boolean) value;
		}
		return true;
	}

	// Standard methods
	
	/**
	 * Make a (deep) copy of this vector
	 */
	public StateValues deepCopy() throws PrismException
	{
		StateValues sv = new StateValues();
		sv.type = type;
		sv.size = size;
		sv.accuracy = accuracy;
		sv.statesList = statesList;
		if (valuesB != null) {
			sv.valuesB = (BitSet) valuesB.clone();
		}
		if (valuesO != null) {
			sv.valuesO = valuesO.clone();
		}
		return sv;
	}

	@Override
	public String toString()
	{
		if (type instanceof TypeBool) {
			return valuesB.toString();
		} else {
			return Arrays.toString(valuesO);
		}
	}
}
