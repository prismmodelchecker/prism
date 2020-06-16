//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package parser;

import java.util.*;

import prism.*;
import parser.VarList.Var;
import parser.ast.*;
import parser.type.*;

/**
 * Class to store information about the set of variables in a PRISM model.
 * Assumes that any constants in the model have been given fixed values.
 * Thus, min/max values for all variables are known.
 * VarList also takes care of how each variable will be encoded to an integer
 * (e.g. for (MT)BDD representation).
 */
public class VarList
{
	// List of variables
	private List<Var> vars;
	// Mapping from names to indices
	private Map<String, Integer> nameMap;
	// Total number of bits needed  to encode
	private int totalNumBits;

	/**
	 * Construct empty variable list.
	 */
	public VarList()
	{
		vars = new ArrayList<Var>();
		nameMap = new HashMap<String, Integer>();
		totalNumBits = 0;
	}

	/**
	* Construct variable list for a ModelInfo object.
	*/
	public VarList(ModelInfo modelInfo) throws PrismException
	{
		this();

		int numVars = modelInfo.getNumVars();
		for (int i = 0; i < numVars; i++) {
			DeclarationType declType = modelInfo.getVarDeclarationType(i);
			int module = modelInfo.getVarModuleIndex(i);
			addVar(modelInfo.getVarName(i), declType, module, modelInfo.getConstantValues());
		}
	}
	 
	/**
	 * Add a new variable to the end of the VarList.
	 * @param decl Declaration defining the variable
	 * @param module Index of module containing variable
	 * @param constantValues Values of constants needed to evaluate low/high/etc.
	 */
	public void addVar(Declaration decl, int module, Values constantValues) throws PrismLangException
	{
		addVar(decl.getName(), decl.getDeclType(), module, constantValues);
	}

	/**
	 * Add a new variable at position i in the VarList.
	 * @param decl Declaration defining the variable
	 * @param module Index of module containing variable
	 * @param constantValues Values of constants needed to evaluate low/high/etc.
	 */
	public void addVar(int i, Declaration decl, int module, Values constantValues) throws PrismLangException
	{
		Var var = createVar(decl.getName(), decl.getDeclType(), module, constantValues);
		vars.add(i, var);
		totalNumBits += getRangeLogTwo(i);
		// Recompute name map
		int j, n;
		n = getNumVars();
		nameMap = new HashMap<String, Integer>(n);
		for (j = 0; j < n; j++) {
			nameMap.put(getName(j), j);
		}
	}

	/**
	 * Add a new variable to the end of the VarList.
	 * @param name Variable name
	 * @param declType Type declaration defining the variable
	 * @param module Index of module containing variable
	 * @param constantValues Values of constants needed to evaluate low/high/etc.
	 */
	public void addVar(String name, DeclarationType declType, int module, Values constantValues) throws PrismLangException
	{
		Var var = createVar(name, declType, module, constantValues);
		vars.add(var);
		totalNumBits += getRangeLogTwo(vars.size() - 1);
		nameMap.put(name, vars.size() - 1);
	}

	/**
	 * Create a new variable object to the store in the list.
	 * @param name Variable name
	 * @param declType Type declaration defining the variable
	 * @param module Index of module containing variable
	 * @param constantValues Values of constants needed to evaluate low/high/etc.
	 */
	private Var createVar(String name, DeclarationType declType, int module, Values constantValues) throws PrismLangException
	{
		Var var;
		int low, high;

		// Create new Var object
		var = new Var(name, declType.getType());
		var.declType = declType;
		var.module = module;
		
		// Variable is a bounded integer
		if (declType instanceof DeclarationInt) {
			DeclarationInt intdecl = (DeclarationInt) declType;
			low = intdecl.getLow().evaluateInt(constantValues);
			high = intdecl.getHigh().evaluateInt(constantValues);
			// Check range is valid
			if (high - low <= 0) {
				String s = "Invalid range (" + low + "-" + high + ") for variable \"" + name + "\"";
				throw new PrismLangException(s, declType);
			}
			if ((long) high - (long) low >= Integer.MAX_VALUE) {
				String s = "Range for variable \"" + name + "\" (" + low + "-" + high + ") is too big";
				throw new PrismLangException(s, declType);
			}
		}
		// Variable is a Boolean
		else if (declType instanceof DeclarationBool) {
			low = 0;
			high = 1;
		}
		// Variable is a clock
		else if (declType instanceof DeclarationClock) {
			// Create dummy info
			low = 0;
			high = 1;
		}
		// Variable is an (unbounded) integer
		else if (declType instanceof DeclarationIntUnbounded) {
			// Create dummy range info
			low = 0;
			high = 1;
		}
		else {
			throw new PrismLangException("Unknown variable type \"" + declType + "\" in declaration", declType);
		}

		// Store low/high and return
		var.low = low;
		var.high = high;

		return var;
	}

	/**
	 * Get the number of variables stored in this list.  
	 */
	public int getNumVars()
	{
		return vars.size();
	}

	/**
	 * Look up the index of a variable, as stored in this list, by name.
	 * Returns -1 if there is no such variable. 
	 */
	public int getIndex(String name)
	{
		Integer i = nameMap.get(name);
		return (i == null) ? -1 : i;
	}

	/**
	 * Check if there is a variable of a given name in this list.
	 */
	public boolean exists(String name)
	{
		return getIndex(name) != -1;
	}

	/**
	 * Get the declaration type of the ith variable in this list.
	 */
	public DeclarationType getDeclarationType(int i)
	{
		return vars.get(i).declType;
	}

	/**
	 * Get the name of the ith variable in this list.
	 */
	public String getName(int i)
	{
		return vars.get(i).name;
	}

	/**
	 * Get the type of the ith variable in this list.
	 */
	public Type getType(int i)
	{
		return vars.get(i).type;
	}

	/**
	 * Get the index of the module of the ith variable in this list (-1 denotes global variable).
	 */
	public int getModule(int i)
	{
		return vars.get(i).module;
	}

	/**
	 * Get the low value of the ith variable in this list (when encoded as an integer).
	 */
	public int getLow(int i)
	{
		return vars.get(i).low;
	}

	/**
	 * Get the high value of the ith variable in this list (when encoded as an integer).
	 */
	public int getHigh(int i)
	{
		return vars.get(i).high;
	}

	/**
	 * Get the range of the ith variable in this list (when encoded as an integer).
	 */
	public int getRange(int i)
	{
		return vars.get(i).high - vars.get(i).low + 1;
	}

	/**
	 * Get the number of bits required to store the ith variable in this list (when encoded as an integer).
	 */
	public int getRangeLogTwo(int i)
	{
		return (int) Math.ceil(PrismUtils.log2(getRange(i)));
	}

	/**
	 * Get the total number of bits required to store all variables in this list (when encoded as integers).
	 */
	public int getTotalNumBits()
	{
		return totalNumBits;
	}

	/**
	 * Get the value (as an Object) for the ith variable, from its encoding as an integer. 
	 */
	public Object decodeFromInt(int i, int val)
	{
		Type type = getType(i);
		// Integer type
		if (type instanceof TypeInt) {
			return val + getLow(i);
		}
		// Boolean type
		else if (type instanceof TypeBool) {
			return val != 0;
		}
		// Anything else
		return null;
	}

	/**
	 * Get the integer encoding of a value for the ith variable, specified as an Object.
	 * The Object is assumed to be of correct type (e.g. Integer, Boolean).
	 * Throws an exception if Object is of the wrong type.
	 * Also throws an exception if the value is out of range.
	 */
	public int encodeToInt(int i, Object val) throws PrismLangException
	{
		Type type = getType(i);
		try {
			// Integer type
			if (type instanceof TypeInt) {
				int intVal = ((TypeInt) type).castValueTo(val).intValue();
				if (intVal < getLow(i) || intVal > getHigh(i)) {
					throw new PrismLangException("Value " + val + " out of range for variable " + getName(i));
				}
				return intVal - getLow(i);
			}
			// Boolean type
			else if (type instanceof TypeBool) {
				return ((TypeBool) type).castValueTo(val).booleanValue() ? 1 : 0;
			}
			// Anything else
			else {
				throw new PrismLangException("Unknown type " + type + " for variable " + getName(i));
			}
		} catch (ClassCastException e) {
			throw new PrismLangException("Value " + val + " is wrong type for variable " + getName(i));
		}
	}

	/**
	 * Get the integer encoding of a value for the ith variable, specified as a string.
	 */
	public int encodeToIntFromString(int i, String s) throws PrismLangException
	{
		Type type = getType(i);
		// Integer type
		if (type instanceof TypeInt) {
			try {
				int iVal = Integer.parseInt(s);
				if (iVal < getLow(i) || iVal > getHigh(i)) {
					throw new PrismLangException("Value " + iVal + " out of range for variable " + getName(i));
				}
				return iVal - getLow(i);
			} catch (NumberFormatException e) {
				throw new PrismLangException("\"" + s + "\" is not a valid integer value");
			}
		}
		// Boolean type
		else if (type instanceof TypeBool) {
			if (s.equals("true"))
				return 1;
			else if (s.equals("false"))
				return 0;
			else
				throw new PrismLangException("\"" + s + "\" is not a valid Boolean value");

		}
		// Anything else
		else {
			throw new PrismLangException("Unknown type " + type + " for variable " + getName(i));
		}
	}

	/**
	 * Get a list of all possible values for a subset of the variables in this list.
	 * @param vars The subset of variables
	 */
	public List<Values> getAllValues(List<String> vars) throws PrismLangException
	{
		int i, j, k, n, lo, hi;
		Vector<Values> allValues;
		Values vals, valsNew;

		allValues = new Vector<Values>();
		allValues.add(new Values());
		for (String var : vars) {
			i = getIndex(var);
			if (getType(i) instanceof TypeBool) {
				n = allValues.size();
				for (j = 0; j < n; j++) {
					vals = allValues.get(j);
					valsNew = new Values(vals);
					valsNew.setValue(var, true);
					allValues.add(valsNew);
					vals.addValue(var, false);
				}
			} else if (getType(i) instanceof TypeInt) {
				lo = getLow(i);
				hi = getHigh(i);
				n = allValues.size();
				for (j = 0; j < n; j++) {
					vals = allValues.get(j);
					for (k = lo + 1; k < hi + 1; k++) {
						valsNew = new Values(vals);
						valsNew.setValue(var, k);
						allValues.add(valsNew);
					}
					vals.addValue(var, lo);
				}
			} else {
				throw new PrismLangException("Cannot determine all values for a variable of type " + getType(i));
			}
		}

		return allValues;
	}

	/**
	 * Get a list of all possible states over the variables in this list. Use with care!
	 */
	public List<State> getAllStates() throws PrismLangException
	{
		List<State> allStates;
		State state, stateNew;

		int numVars = getNumVars();
		allStates = new ArrayList<State>();
		allStates.add(new State(numVars));
		for (int i = 0; i < numVars; i++) {
			if (getType(i) instanceof TypeBool) {
				int n = allStates.size();
				for (int j = 0; j < n; j++) {
					state = allStates.get(j);
					stateNew = new State(state);
					stateNew.setValue(i, true);
					state.setValue(i, false);
					allStates.add(stateNew);
				}
			} else if (getType(i) instanceof TypeInt) {
				int lo = getLow(i);
				int hi = getHigh(i);
				int n = allStates.size();
				for (int j = 0; j < n; j++) {
					state = allStates.get(j);
					for (int k = lo + 1; k < hi + 1; k++) {
						stateNew = new State(state);
						stateNew.setValue(i, k);
						allStates.add(stateNew);
					}
					state.setValue(i, lo);
				}
			} else {
				throw new PrismLangException("Cannot determine all values for a variable of type " + getType(i));
			}
		}

		return allStates;
	}

	/**
	 * Convert a bit vector representing a single state to a State object. 
	 */
	public State convertBitSetToState(BitSet bits)
	{
		int i, n, j, var, val;
		State state;
		state = new State(getNumVars());
		var = val = j = 0;
		n = totalNumBits;
		for (i = 0; i < n; i++) {
			if (bits.get(i))
				val += (1 << (getRangeLogTwo(var) - j - 1));
			if (j >= getRangeLogTwo(var) - 1) {
				state.setValue(var, decodeFromInt(var, val));
				var++;
				val = 0;
				j = 0;
			} else {
				j++;
			}
		}
		return state;
	}

	/**
	 * Clone this list.
	 */
	public Object clone()
	{
		int i, n;
		n = getNumVars();
		VarList rv = new VarList();
		rv.vars = new ArrayList<Var>(n);
		rv.nameMap = new HashMap<String, Integer>(n);
		for (i = 0; i < n; i++) {
			rv.vars.add(new Var(vars.get(i)));
			rv.nameMap.put(getName(i), i);
		}
		return rv;
	}

	/**
	 * Class to store information about a single variable.
	 */
	class Var
	{
		// Name
		public String name;
		// Type
		public Type type;
		// Further type info from variable declaration
		public DeclarationType declType;
		// Index of containing module (-1 for a global)
		public int module;
		// Info about how variable is encoded as an integer
		public int low;
		public int high;

		/** Default constructor */
		public Var(String name, Type type)
		{
			this.name = name;
			this.type = type;
		}

		/** Copy constructor */
		public Var(Var var)
		{
			name = var.name;
			type = var.type;
			declType = (DeclarationType) var.declType.deepCopy();
			module = var.module;
			low = var.low;
			high = var.high;
		}
	}
}
