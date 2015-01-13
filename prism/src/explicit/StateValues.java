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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;

import parser.State;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionUnaryOp;
import parser.ast.RelOp;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismUtils;
import prism.StateVector;

/**
 * Class for explicit-state storage of a state-indexed vector of values (int, double, boolean).
 */
public class StateValues implements StateVector
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
			Integer objI = ((TypeInt) type).castValueTo(init);
			int initI = objI.intValue();
			for (i = 0; i < size; i++)
				valuesI[i] = initI;
		} else if (type instanceof TypeDouble) {
			valuesD = new double[size];
			Double objD = ((TypeDouble) type).castValueTo(init);
			double initD = objD.doubleValue();
			for (i = 0; i < size; i++)
				valuesD[i] = initD;
		} else if (type instanceof TypeBool) {
			Boolean objB = ((TypeBool) type).castValueTo(init);
			boolean initB = objB.booleanValue();
			if (initB) {
				valuesB = new BitSet(size);
				valuesB.set(0, size);
			} else {
				valuesB = new BitSet();
			}
		} else {
			throw new PrismLangException("Cannot create a vector of type " + type);
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
	public BitSet getBitSetFromInterval(String relOpString, double bound) throws PrismException
	{
		return getBitSetFromInterval(RelOp.parseSymbol(relOpString), bound);
	}

	/**
	 * Generate BitSet for states in the given interval
	 * (interval specified as relational operator and bound)
	 */
	public BitSet getBitSetFromInterval(RelOp relOp, double bound) throws PrismException
	{
		BitSet sol = new BitSet();

		if (type instanceof TypeInt) {
			switch (relOp) {
			case GEQ:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] >= bound);
				}
				break;
			case GT:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] > bound);
				}
				break;
			case LEQ:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] <= bound);
				}
				break;
			case LT:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesI[i] < bound);
				}
				break;
			default:
				throw new PrismException("Unsupported operator " + relOp + " for getBitSetFromInterval()");
			}
		} else if (type instanceof TypeDouble) {
			switch (relOp) {
			case GEQ:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] >= bound);
				}
				break;
			case GT:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] > bound);
				}
				break;
			case LEQ:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] <= bound);
				}
				break;
			case LT:
				for (int i = 0; i < size; i++) {
					sol.set(i, valuesD[i] < bound);
				}
				break;
			default:
				throw new PrismException("Unsupported operator " + relOp + " for getBitSetFromInterval()");
			}
		} else {
			throw new PrismException("Can't getBitSetFromInterval for a vector of type " + type);
		}

		return sol;
	}

	/**
	 * Generate BitSet for states whose value is close to 'value'
	 * (within either absolute or relative error 'epsilon')
	 * The type of 'value' is assumed to match that of the vector.
	 */
	public BitSet getBitSetFromCloseValue(Object value, double epsilon, boolean abs) throws PrismException
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
		} else {
			throw new PrismException("Can't getBitSetFromCloseValue for a vector of type " + type);
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

	/**
	 * Modify the vector by applying If-Then-Else, i.e. {@code svIf} ? {@code svThen} : {@code this}.
	 */
	public void applyITE(StateValues svIf, StateValues svThen) throws PrismException
	{
		if (!(svIf.type instanceof TypeBool)) {
			throw new PrismException("Type error in ? operator");
		}
		if (type instanceof TypeInt) {
			if (svThen.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] = svIf.valuesB.get(i) ? svThen.valuesI[i] : valuesI[i];
				}
			} else if (svThen.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = svIf.valuesB.get(i) ? svThen.valuesD[i] : valuesD[i];
				}
				valuesI = null;
			} else {
				throw new PrismException("Type error in ? operator");
			}
		} else if (type instanceof TypeDouble) {
			if (svThen.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = svIf.valuesB.get(i) ? svThen.valuesI[i] : valuesD[i];
				}
			} else if (svThen.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = svIf.valuesB.get(i) ? svThen.valuesD[i] : valuesD[i];
				}
			} else {
				throw new PrismException("Type error in ? operator");
			}
		} else if (type instanceof TypeBool) {
			if (svThen.type instanceof TypeBool) {
				for (int i = svIf.valuesB.nextSetBit(0); i >= 0; i = svIf.valuesB.nextSetBit(i + 1)) {
					valuesB.set(i, svThen.valuesB.get(i));
				}
			} else {
				throw new PrismException("Type error in ? operator");
			}
		} else {
			throw new PrismException("Type error in ? operator");
		}
	}

	/**
	 * Modify the vector by applying binary operator {@code op} with second operand {@code sv},
	 * where {@code op} refers to the codes in {@link ExpressionBinaryOp}.
	 */
	public void applyBinaryOp(int op, StateValues sv) throws PrismException
	{
		switch (op) {
		case ExpressionBinaryOp.IMPLIES:
			implies(sv);
			break;
		case ExpressionBinaryOp.IFF:
			iff(sv);
			break;
		case ExpressionBinaryOp.OR:
			or(sv);
			break;
		case ExpressionBinaryOp.AND:
			and(sv);
			break;
		case ExpressionBinaryOp.EQ:
			equals(sv);
			break;
		case ExpressionBinaryOp.NE:
			notEquals(sv);
			break;
		case ExpressionBinaryOp.GT:
			greaterThan(sv);
			break;
		case ExpressionBinaryOp.GE:
			greaterThanEquals(sv);
			break;
		case ExpressionBinaryOp.LT:
			lessThan(sv);
			break;
		case ExpressionBinaryOp.LE:
			lessThanEquals(sv);
			break;
		case ExpressionBinaryOp.PLUS:
			plus(sv);
			break;
		case ExpressionBinaryOp.MINUS:
			minus(sv);
			break;
		case ExpressionBinaryOp.TIMES:
			times(sv);
			break;
		case ExpressionBinaryOp.DIVIDE:
			divide(sv);
			break;
		default:
			throw new PrismException("Unknown binary operator");
		}
	}

	/**
	 * Modify the vector by applying 'implies' with operand {@code sv}.
	 */
	public void implies(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.type instanceof TypeBool)) {
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
		if (!(type instanceof TypeBool) || !(sv.type instanceof TypeBool)) {
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
		if (!(type instanceof TypeBool) || !(sv.type instanceof TypeBool)) {
			throw new PrismException("Operator | can only be applied to Boolean vectors");
		}
		valuesB.or(sv.valuesB);
	}

	/**
	 * Modify the vector by applying 'and' with operand {@code sv}.
	 */
	public void and(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeBool) || !(sv.type instanceof TypeBool)) {
			throw new PrismException("Operator & can only be applied to Boolean vectors");
		}
		valuesB.and(sv.valuesB);
	}

	/**
	 * Modify the vector by applying 'equals' with operand {@code sv}.
	 */
	public void equals(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] == sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] == sv.valuesD[i]);
				}
			}
		} else if (type instanceof TypeDouble) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] == sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] == sv.valuesD[i]);
				}
			}
		} else if (type instanceof TypeBool) {
			if (sv.type instanceof TypeBool) {
				valuesB.xor(sv.valuesB);
				valuesB.flip(0, size);
			}
		}
		type = TypeBool.getInstance();
		valuesI = null;
		valuesD = null;
	}

	/**
	 * Modify the vector by applying 'not-equals' with operand {@code sv}.
	 */
	public void notEquals(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] != sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] != sv.valuesD[i]);
				}
			}
		} else if (type instanceof TypeDouble) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] != sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] != sv.valuesD[i]);
				}
			}
		} else if (type instanceof TypeBool) {
			if (sv.type instanceof TypeBool) {
				valuesB.xor(sv.valuesB);
			}
		}
		type = TypeBool.getInstance();
		valuesI = null;
		valuesD = null;
	}

	/**
	 * Modify the vector by applying '>' with operand {@code sv}.
	 */
	public void greaterThan(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] > sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] > sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator > cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] > sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] > sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator > cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator > cannot be applied to Boolean vectors");
		}
		type = TypeBool.getInstance();
		valuesI = null;
		valuesD = null;
	}

	/**
	 * Modify the vector by applying '>=' with operand {@code sv}.
	 */
	public void greaterThanEquals(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] >= sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] >= sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator >= cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] >= sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] >= sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator >= cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator >= cannot be applied to Boolean vectors");
		}
		type = TypeBool.getInstance();
		valuesI = null;
		valuesD = null;
	}

	/**
	 * Modify the vector by applying '<' with operand {@code sv}.
	 */
	public void lessThan(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] < sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] < sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator < cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] < sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] < sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator < cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator < cannot be applied to Boolean vectors");
		}
		type = TypeBool.getInstance();
		valuesI = null;
		valuesD = null;
	}

	/**
	 * Modify the vector by applying '<=' with operand {@code sv}.
	 */
	public void lessThanEquals(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] <= sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesI[i] <= sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator <= cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			valuesB = new BitSet();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] <= sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesB.set(i, valuesD[i] <= sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Operator <= cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator <= cannot be applied to Boolean vectors");
		}
		type = TypeBool.getInstance();
		valuesI = null;
		valuesD = null;
	}

	/**
	 * Modify the vector by applying 'plus' with operand {@code sv}.
	 */
	public void plus(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] += sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = valuesI[i] + sv.valuesD[i];
				}
				valuesI = null;
			} else {
				throw new PrismException("Operator + cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] += sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] += sv.valuesD[i];
				}
			} else {
				throw new PrismException("Operator + cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator + cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'plus' with a constant.
	 */
	public void plusConstant(int val) throws PrismException
	{
		if (type instanceof TypeInt) {
			for (int i = 0; i < size; i++) {
				valuesI[i] += val;
			}
		} else if (type instanceof TypeDouble) {
			for (int i = 0; i < size; i++) {
				valuesD[i] += val;
			}
		} else {
			throw new PrismException("Operator + cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'plus' with a constant.
	 */
	public void plusConstant(double val) throws PrismException
	{
		if (type instanceof TypeInt) {
			// Change type
			valuesD = new double[size];
			type = TypeDouble.getInstance();
			for (int i = 0; i < size; i++) {
				valuesD[i] = valuesI[i] + val;
			}
			valuesI = null;
		} else if (type instanceof TypeDouble) {
			for (int i = 0; i < size; i++) {
				valuesD[i] += val;
			}
		} else {
			throw new PrismException("Operator + cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'minus' with operand {@code sv}.
	 */
	public void minus(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] -= sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = valuesI[i] - sv.valuesD[i];
				}
				valuesI = null;
			} else {
				throw new PrismException("Operator - cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] -= sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] -= sv.valuesD[i];
				}
			} else {
				throw new PrismException("Operator - cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator - cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'times' with operand {@code sv}.
	 */
	public void times(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] *= sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = valuesI[i] * sv.valuesD[i];
				}
				valuesI = null;
			} else {
				throw new PrismException("Operator * cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] *= sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] *= sv.valuesD[i];
				}
			} else {
				throw new PrismException("Operator * cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator * cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'times' with a constant.
	 */
	public void timesConstant(int val) throws PrismException
	{
		if (type instanceof TypeInt) {
			for (int i = 0; i < size; i++) {
				valuesI[i] *= val;
			}
		} else if (type instanceof TypeDouble) {
			for (int i = 0; i < size; i++) {
				valuesD[i] *= val;
			}
		} else {
			throw new PrismException("Operator + cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'times' with a constant.
	 */
	public void timesConstant(double val) throws PrismException
	{
		if (type instanceof TypeInt) {
			// Change type
			valuesD = new double[size];
			type = TypeDouble.getInstance();
			for (int i = 0; i < size; i++) {
				valuesD[i] = valuesI[i] * val;
			}
			valuesI = null;
		} else if (type instanceof TypeDouble) {
			for (int i = 0; i < size; i++) {
				valuesD[i] *= val;
			}
		} else {
			throw new PrismException("Operator + cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'divide' with operand {@code sv}.
	 */
	public void divide(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesD = new double[size];
			type = TypeDouble.getInstance();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ((double) valuesI[i]) / sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = valuesI[i] / sv.valuesD[i];
				}
			} else {
				throw new PrismException("Operator / cannot be applied to Boolean vectors");
			}
			valuesI = null;
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] /= sv.valuesI[i];
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] /= sv.valuesD[i];
				}
			} else {
				throw new PrismException("Operator / cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Operator / cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying unary operator {@code op},
	 * where {@code op} refers to the codes in {@link ExpressionUnaryOp}.
	 */
	public void applyUnaryOp(int op) throws PrismException
	{
		switch (op) {
		case ExpressionUnaryOp.NOT:
			not();
			break;
		case ExpressionUnaryOp.MINUS:
			minus();
			break;
		default:
			throw new PrismException("Unknown binary operator");
		}
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

	/**
	 * Modify the vector by applying (unary) 'minus'.
	 */
	public void minus() throws PrismException
	{
		if (type instanceof TypeInt) {
			for (int i = 0; i < size; i++) {
				valuesI[i] = -valuesI[i];
			}
		} else if (type instanceof TypeDouble) {
			for (int i = 0; i < size; i++) {
				valuesD[i] = -valuesD[i];
			}
		} else {
			throw new PrismException("Operator - cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying (unary) function {@code op},
	 * where {@code op} refers to the codes in {@link parser.ast.ExpressionFunc}.
	 */
	public void applyFunctionUnary(int op) throws PrismException
	{
		switch (op) {
		case ExpressionFunc.FLOOR:
			floor();
			break;
		case ExpressionFunc.CEIL:
			ceil();
			break;
		default:
			throw new PrismException("Unknown unary function");
		}
	}

	/**
	 * Modify the vector by applying 'floor'.
	 */
	public void floor() throws PrismException
	{
		if (type instanceof TypeInt) {
			// Nothing to do
		} else if (type instanceof TypeDouble) {
			valuesI = new int[size];
			type = TypeInt.getInstance();
			for (int i = 0; i < size; i++) {
				valuesI[i] = ExpressionFunc.evaluateFloor(valuesD[i]);
			}
			valuesD = null;
		} else {
			throw new PrismException("Function floor cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'ceil'.
	 */
	public void ceil() throws PrismException
	{
		if (type instanceof TypeInt) {
			// Nothing to do
		} else if (type instanceof TypeDouble) {
			valuesI = new int[size];
			type = TypeInt.getInstance();
			for (int i = 0; i < size; i++) {
				valuesI[i] = ExpressionFunc.evaluateCeil(valuesD[i]);
			}
			valuesD = null;
		} else {
			throw new PrismException("Function ceil cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying (binary or N-ary) function {@code op} with second operand {@code sv},
	 * where {@code op} refers to the codes in {@link parser.ast.ExpressionFunc}.
	 */
	public void applyFunctionBinary(int op, StateValues sv) throws PrismException
	{
		switch (op) {
		case ExpressionFunc.POW:
			pow(sv);
			break;
		case ExpressionFunc.MOD:
			mod(sv);
			break;
		case ExpressionFunc.LOG:
			log(sv);
			break;
		case ExpressionFunc.MIN:
			min(sv);
			break;
		case ExpressionFunc.MAX:
			max(sv);
			break;
		default:
			throw new PrismException("Unknown binary function");
		}
	}

	/**
	 * Modify the vector by applying 'pow' with second operand {@code sv}.
	 */
	public void pow(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] = ExpressionFunc.evaluatePowInt(valuesI[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluatePowDouble(valuesI[i], sv.valuesD[i]);
				}
				valuesI = null;
			} else {
				throw new PrismException("Function pow() cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluatePowDouble(valuesD[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluatePowDouble(valuesD[i], sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Function pow() cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Function pow() cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'mod' with second operand {@code sv}.
	 */
	public void mod(StateValues sv) throws PrismException
	{
		if (!(type instanceof TypeInt && sv.type instanceof TypeInt)) {
			throw new PrismException("Function mod() can only be applied to integer vectors");
		}
		for (int i = 0; i < size; i++) {
			valuesI[i] = ExpressionFunc.evaluateMod(valuesI[i], sv.valuesI[i]);
		}
	}

	/**
	 * Modify the vector by applying 'log' with operand {@code sv}.
	 */
	public void log(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			valuesD = new double[size];
			type = TypeDouble.getInstance();
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluateLog(valuesI[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluateLog(valuesI[i], sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Function log() cannot be applied to Boolean vectors");
			}
			valuesI = null;
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluateLog(valuesD[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = ExpressionFunc.evaluateLog(valuesD[i], sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Function log() cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Function log() cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'min' with operand {@code sv}.
	 */
	public void min(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] = Math.min(valuesI[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = Math.min(valuesI[i], sv.valuesD[i]);
				}
				valuesI = null;
			} else {
				throw new PrismException("Function min() cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = Math.min(valuesD[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = Math.min(valuesD[i], sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Function min() cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Function min() cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Modify the vector by applying 'max' with operand {@code sv}.
	 */
	public void max(StateValues sv) throws PrismException
	{
		if (type instanceof TypeInt) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesI[i] = Math.max(valuesI[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				valuesD = new double[size];
				type = TypeDouble.getInstance();
				for (int i = 0; i < size; i++) {
					valuesD[i] = Math.max(valuesI[i], sv.valuesD[i]);
				}
				valuesI = null;
			} else {
				throw new PrismException("Function max() cannot be applied to Boolean vectors");
			}
		} else if (type instanceof TypeDouble) {
			if (sv.type instanceof TypeInt) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = Math.max(valuesD[i], sv.valuesI[i]);
				}
			} else if (sv.type instanceof TypeDouble) {
				for (int i = 0; i < size; i++) {
					valuesD[i] = Math.max(valuesD[i], sv.valuesD[i]);
				}
			} else {
				throw new PrismException("Function max() cannot be applied to Boolean vectors");
			}
		} else {
			throw new PrismException("Function max() cannot be applied to Boolean vectors");
		}
	}

	/**
	 * Set the elements of this vector by reading them in from a file.
	 */
	public void readFromFile(File file) throws PrismException
	{
		BufferedReader in;
		String s;
		int lineNum = 0, count = 0;
		boolean hasIndices = false;

		try {
			// Open file for reading
			in = new BufferedReader(new FileReader(file));
			// Read remaining lines
			s = in.readLine();
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
						int i = Integer.parseInt(s);
						setIntValue(count, i);
					} else if (type instanceof TypeDouble) {
						double d = Double.parseDouble(s);
						setDoubleValue(count, d);
					} else if (type instanceof TypeBool) {
						boolean b = Boolean.parseBoolean(s);
						setBooleanValue(count, b);
					}
					count++;
				}
				s = in.readLine();
				lineNum++;
			}
			// Close file
			in.close();
			// Check size
			if (!hasIndices && count < size)
				throw new PrismException("Too few values in file \"" + file + "\" (" + count + ", not " + size + ")");
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of file \"" + file + "\"");
		}
	}

	// ...

	@Override
	public void clear()
	{
		// Actually, just set pointers to null and wait for later garbage collection.
		valuesI = null;
		valuesD = null;
		valuesB = null;
	}

	// METHODS TO ACCESS VECTOR DATA

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
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
	 * For integer-valued vectors, get the int array storing the data.
	 */
	public int[] getIntArray()
	{
		return valuesI;
	}

	/**
	 * For double-valued vectors, get the double array storing the data.
	 */
	public double[] getDoubleArray()
	{
		return valuesD;
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
			log.println("(all zero)");
			return;
		}

		// Footer for Matlab format
		if (printMatlab && !printSparse)
			log.println("];");
	}

	private boolean printLine(PrismLog log, int n, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
	{
		if (!printSparse || isNonZero(n)) {
			if (printMatlab) {
				if (printSparse) {
					log.println("v(" + (n + 1) + ")=" + getValue(n) + ";");
				} else {
					log.println(getValue(n));
				}
			} else {
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
					log.println(getValue(n));
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
