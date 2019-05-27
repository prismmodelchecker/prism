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

import java.util.ArrayList;

import param.BigRational;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.ModelInfo;
import prism.PrismLangException;
import prism.PrismUtils;

/**
 * Class to store a list of typed constant/variable values.
 * (Basically, just a mapping from String to Object)
 */
public class Values implements Cloneable //implements Comparable
{
	protected ArrayList<String> names;
	protected ArrayList<Object> values;
	
	// Constructors
	
	/**
	 * Construct a new, empty Values object
	 */
	public Values()
	{
		names = new ArrayList<String>();
		values = new ArrayList<Object>();
	}
	
	/**
	 * Construct a new Values object by copying an existing one.
	 * If the existing one is null, it is treated as empty. 
	 */
	@SuppressWarnings("unchecked")
	public Values(Values v)
	{
		if (v == null) {
			names = new ArrayList<String>();
			values = new ArrayList<Object>();
		} else {
			names = (ArrayList<String>) v.names.clone();
			values = (ArrayList<Object>) v.values.clone();
		}
	}
	
	/**
	 * Construct a new Values object by merging two existing ones.
	 * There is no checking for duplicates.
	 * Either can be null and, if so, is treated as empty. 
	 */
	public Values(Values v1, Values v2)
	{
		this(v1);
		addValues(v2);
	}
	
	/**
	 * Construct a new Values object by copying existing State object.
	 * If it is null, it is treated as empty. 
	 * Need access to model info for variable names.
	 * @param s State object to copy.
	 * @param modelInfo Corresponding modelInfo (for variable info/ordering)
	 */
	public Values(State s, ModelInfo modelInfo)
	{
		this();
		if (s == null) return;
		int n = s.varValues.length;
		for (int i = 0; i < n; i++) {
			addValue(modelInfo.getVarName(i), s.varValues[i]);
		}
	}
	
	/**
	 * Add a value (type of value determined by type of Object).
	 * (Note: there is no checking for duplication/inconsistencies/etc.)
	 * @param name Constant/variable name
	 * @param value Value
	 */
	public void addValue(String name, Object value)
	{
		names.add(name);
		values.add(value);
	}
	
	/**
	 * Add multiple values, specified as a {@link Values} object {@code v}.
	 * If {@code v} is null, it is treated as empty. 
	 * (Note: there is no checking for duplication/inconsistencies/etc.)
	 */
	public void addValues(Values v)
	{
		int i, n;
		
		if (v == null) return;
		n = v.getNumValues();
		for (i = 0; i < n; i ++) {
			addValue(v.getName(i), v.getValue(i));
		}
	}
	
	/**
	 * Set a value (overwrite if already present)
	 * Returns 0 if added, -1 if overwritten
	 * @param name Constant/variable name
	 * @param value Value
	 */
	public int setValue(String name, Object value)
	{
		int i = getIndexOf(name);
		if (i == -1) {
			addValue(name, value);
			return 0;
		}
		else {
			values.set(i, value);
			return -1;
		}
	}
	
	/**
	 * Set multiple values (overwrite if already present)
	 * Returns number of values overwritten.
	 */
	public int setValues(Values v)
	{
		int i, n, c = 0;
		
		if (v == null) return c;
		n = v.getNumValues();
		for (i = 0; i < n; i ++) {
			if (setValue(v.getName(i), v.getValue(i)) == 0) c++;
		}
		
		return c;
	}
	
	/**
	 * Remove the {@code i}th value added (if present)
	 */
	public void removeValue(int i)
	{
		if (i >= 0 && i < getNumValues()) {
			names.remove(i);
			values.remove(i);
		}
	}
	
	/**
	 * Remove the value for constant/variable {@code name} (if present)
	 */
	public void removeValue(String name)
	{
		int i = getIndexOf(name);
		if (i != -1) {
			removeValue(i);
		}
	}
	
	// Get methods
	
	/**
	 * Get the number of values.
	 */
	public int getNumValues()
	{
		return names.size();
	}
	
	/**
	 * Get the variable/constant name for the {@code i}th value.
	 */
	public String getName(int i)
	{
		return names.get(i);
	}

	/**
	 * Get the index of the value for variable/constant {@code name} (-1 if not present).
	 */
	public int getIndexOf(String name)
	{
		return names.indexOf(name);
	}
	
	/**
	 * Is there a value for variable/constant {@code name}?
	 */
	public boolean contains(String name)
	{
		return names.contains(name);
	}
	
	/**
	 * Get the type for the {@code i}th value.
	 * (This is based on the type of the Object storing the value.)
	 */
	public Type getType(int i)
	{
		Object o = values.get(i);
		if (o instanceof Integer) return TypeInt.getInstance();
		if (o instanceof Double)  return TypeDouble.getInstance();
		if (o instanceof BigRational) return TypeDouble.getInstance();
		if (o instanceof Boolean) return TypeBool.getInstance();
		else return null;
	}

	/**
	 * Get the {@code i}th value.
	 */
	public Object getValue(int i)
	{
		return values.get(i);
	}
	
	/**
	 * Evaluate {@code i}th value as an int.
	 * (Note: Booleans get mapped to 0/1)
	 */
	public int getIntValue(int i) throws PrismLangException
	{
		Object o;

		o = values.get(i);

		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue() ? 1 : 0;
		}
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		if (o instanceof BigRational) {
			return ((BigRational)o).toInt();
		}

		throw new PrismLangException("Cannot get integer value for \"" + getName(i) + "\"");
	}

	/**
	 * Evaluate {@code i}th value as a double.
	 * (Note: Booleans get mapped to 0.0/1.0)
	 */
	public double getDoubleValue(int i) throws PrismLangException
	{
		Object o;
		
		o = values.get(i);
		
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue() ? 1.0 : 0.0;
		}
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		if (o instanceof Double) {
			return ((Double)o).doubleValue();
		}
		if (o instanceof BigRational) {
			return ((BigRational)o).doubleValue();
		}
		
		throw new PrismLangException("Cannot get double value for \"" + getName(i) + "\"");
	}

	/**
	 * Evaluate {@code i}th value as a Boolean.
	 */
	public boolean getBooleanValue(int i) throws PrismLangException
	{
		Object o;

		o = values.get(i);

		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue();
		} else if (o instanceof BigRational) {
			return ((BigRational)o).toBoolean();
		} else {
			throw new PrismLangException("Cannot get boolean value for \"" + getName(i) + "\"");
		}
	}

	/**
	 * Get the value for variable/constant {@code name}.
	 * @throws PrismLangException if no value is present.
	 */
	public Object getValueOf(String name) throws PrismLangException
	{
		int i;
		
		i = getIndexOf(name);
		if (i == -1) throw new PrismLangException("Couldn't get value for \"" + name + "\"");
		return getValue(i);
	}

	/**
	 * Evaluate variable/constant {@code name} as an int.
	 * (Note: Booleans get mapped to 0/1)
	 */
	public int getIntValueOf(String name) throws PrismLangException
	{
		int i;
		
		i = getIndexOf(name);
		if (i == -1) throw new PrismLangException("Couldn't get value for \"" + name + "\"");
		return getIntValue(i);
	}

	/**
	 * Evaluate variable/constant {@code name} as a double.
	 * (Note: Booleans get mapped to 0.0/1.0)
	 */
	public double getDoubleValueOf(String n) throws PrismLangException
	{
		int i;
		
		i = getIndexOf(n);
		if (i == -1) throw new PrismLangException("Couldn't get value for \"" + n + "\"");
		return getDoubleValue(i);
	}

	/**
	 * Evaluate variable/constant {@code name} as a boolean.
	 */
	public boolean getBooleanValueOf(String n) throws PrismLangException
	{
		int i;
		
		i = getIndexOf(n);
		if (i == -1) throw new PrismLangException("Couldn't get value for \"" + n + "\"");
		return getBooleanValue(i);
	}

	@Override
	public boolean equals(Object o)
	{
		int i, j, n;
		Values v;
		String s;
		
		// trivial case: null arg
		if (o == null) return false;
		// another trivial case: wrong type
		try { v = (Values)o; } catch (ClassCastException e) { return false; }
		// check sizes are equal
		n = getNumValues();
		if (v.getNumValues() != n) return false;
		// check each value
		for (i = 0; i < n; i++) {
			s = getName(i);
			j = v.getIndexOf(s);
			if (j == -1) return false;
			o = v.getValue(j);
			if (!getValue(i).equals(o)) return false;
		}
		return true;
	}

	// compare
	
// 	public int compareTo(Object o)
// 	{
// 		int i, j, n, c;
// 		Values v;
// 		String s;
// 		Object c1, c2;
// 		
// 		// trivial case: null arg
// 		if (o == null) throw new NullPointerException();
// 		// cast
// 		v = (Values)o;
// 		// check sizes are equal
// 		n = getNumValues();
// 		if (v.getNumValues() != n) throw new ClassCastException();
// 		// check each value
// 		for (i = 0; i < n; i++) {
// 			s = getName(i);
// 			j = v.getIndexOf(s);
// 			if (j == -1) throw new ClassCastException();
// 			c1 = getValue(i);
// 			c2 = v.getValue(j);
// 			if (c1 instanceof Double) c = ((Double)c1).compareTo(c2);
// 			else if (c1 instanceof Integer) c = ((Integer)c1).compareTo(c2);
// 			else {
// 				boolean b1 = ((Boolean)c1).booleanValue();
// 				boolean b2 = ((Boolean)c2).booleanValue();
// 				if (b1 == b2) c = 0; else if (b1 == false) c = -1; else c = 1;
// 			}
// 			if (c != 0) return c;
// 		}
// 		return 0;
// 	}

	@SuppressWarnings("unchecked")
	@Override
	public Values clone()
	{
		Values clone;
		try {
			clone = (Values) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError("Object#clone is expected to work for Cloneable objects.", e);
		}
		clone.names = (ArrayList<String>) names.clone();
		clone.values = (ArrayList<Object>) values.clone();
		return clone;
	}

	@Override
	public String toString()
	{
		return toString(true, ",");
	}
	
	/**
	 * Return a string representation of this Values object, e.g. "x=1,y=2".
	 * If {@code printNames} is false, the "x="s are omitted.
	 * The separator ("," above) can be specified in {@code separator}.
	 * @param printNames Print variable/constant names?
	 * @param separator String used to separate values in the list 
	 */
	public String toString(boolean printNames, String separator)
	{
		// Build string of form "x=1,y=2"
		int n = getNumValues();
		String s = "";
		for (int i = 0; i < n; i++) {
			if (printNames) {
				s += getName(i) + "=";
			}
			s += valToString(getValue(i));
			if (i < n-1) {
				s += separator;
			}
		}
		return s;
	}
	
	/**
	 * Build a string for two Values objects, combined.
	 * Either can be null, meaning an empty object
	 * If both are empty the resulting string is "".
	 */
	public static String toStringConcatenated(Values v1, Values v2)
	{
		String s = "", s2;
		if (v1 != null) {
			s += v1.toString();
		}
		if (v2 != null) {
			s2 = v2.toString();
			if (s2.length() > 0) {
				if (s.length() > 0)
					s += ",";
				s += s2;
			}
		}
		return s;
	}
	
	/**
	 * Format a value (as an Object) as a string.
	 */
	private String valToString(Object o)
	{
		String s;
		
		if (o instanceof Double) {
			s = PrismUtils.formatDouble((double)o);
		} else {
			s = o.toString();
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
