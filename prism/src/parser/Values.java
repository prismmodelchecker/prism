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

import java.util.Vector;
import java.text.*;

import prism.PrismException;

// class to store a list of typed constant/variable values

public class Values //implements Comparable
{
	Vector names;
	Vector values;
	
	// constructors
	
	public Values()
	{
		names = new Vector();
		values = new Vector();
	}
	
	// add value (type of value detetmined by type of Object)
	// (note: no checking for duplication/inconsistencies/etc.)
	
	public void addValue(String n, Object o)
	{
		names.addElement(n);
		values.addElement(o);
	}
	
	// add multiple values
	// (note: no checking for duplication/inconsistencies/etc.)
	
	public void addValues(Values v)
	{
		int i, n;
		
		if (v == null) return;
		n = v.getNumValues();
		for (i = 0; i < n; i ++) {
			addValue(v.getName(i), v.getValue(i));
		}
	}
	
	// set value (overwrite if already present)
	// returns 0 if added, -1 if overwritten
	
	public int setValue(String n, Object o)
	{
		int i = getIndexOf(n);
		if (i == -1) {
			addValue(n, o);
			return 0;
		}
		else {
			values.setElementAt(o, i);
			return -1;
		}
	}
	
	// set multiple values (overwrite if already present)
	// returns num values overwritten
	
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
	
	// remove value (if present)
	
	public void removeValue(int i)
	{
		if (i >= 0 && i < getNumValues()) {
			names.removeElementAt(i);
			values.removeElementAt(i);
		}
	}
	
	// remove value (if present)
	
	public void removeValue(String n)
	{
		int i = getIndexOf(n);
		if (i != -1) {
			removeValue(i);
		}
	}
	
	// get methods
	
	public int getNumValues()
	{
		return names.size();
	}
	
	public String getName(int i)
	{
		return (String)names.elementAt(i);
	}

	public int getIndexOf(String n)
	{
		return names.indexOf(n);
	}
	
	public boolean contains(String n)
	{
		return names.contains(n);
	}
	
	public int getType(int i)
	{
		Object o = values.elementAt(i);
		if (o instanceof Integer) return Expression.INT;
		if (o instanceof Double) return Expression.DOUBLE;
		if (o instanceof Boolean) return Expression.BOOLEAN;
		else return 0;
	}

	public Object getValue(int i)
	{
		return values.elementAt(i);
	}

	public int getIntValue(int i) throws PrismException
	{
		Object o;
		
		o = values.elementAt(i);
		
		if (!(o instanceof Integer)) {
			throw new PrismException("Cannot get integer value for \"" + getName(i) + "\"");
		}
		
		return ((Integer)o).intValue();
	}

	public double getDoubleValue(int i) throws PrismException
	{
		Object o;
		
		o = values.elementAt(i);
		
		if (o instanceof Boolean) {
			throw new PrismException("Cannot get double value for \"" + getName(i) + "\"");
		}
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		if (o instanceof Double) {
			return ((Double)o).doubleValue();
		}
		
		throw new PrismException("Cannot get double value for \"" + getName(i) + "\"");
	}

	public boolean getBooleanValue(int i) throws PrismException
	{
		Object o;
		
		o = values.elementAt(i);
		
		if (!(o instanceof Boolean)) {
			throw new PrismException("Cannot get boolean value for \"" + getName(i) + "\"");
		}
		
		return ((Boolean)o).booleanValue();
	}

	public Object getValueOf(String n) throws PrismException
	{
		int i;
		
		i = getIndexOf(n);
		if (i == -1) throw new PrismException("Couldn't get value for \"" + n + "\"");
		return getValue(i);
	}

	public int getIntValueOf(String n) throws PrismException
	{
		int i;
		
		i = getIndexOf(n);
		if (i == -1) throw new PrismException("Couldn't get value for \"" + n + "\"");
		return getIntValue(i);
	}

	public double getDoubleValueOf(String n) throws PrismException
	{
		int i;
		
		i = getIndexOf(n);
		if (i == -1) throw new PrismException("Couldn't get value for \"" + n + "\"");
		return getDoubleValue(i);
	}

	public boolean getBooleanValueOf(String n) throws PrismException
	{
		int i;
		
		i = getIndexOf(n);
		if (i == -1) throw new PrismException("Couldn't get value for \"" + n + "\"");
		return getBooleanValue(i);
	}

	// compare for equality
	
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

	// clone
	
	public Object clone()
	{
		Values res;
		int i, n;
		String s;
		Object o;
		
		res = new Values();
		n = getNumValues();
		for (i = 0; i < n; i++) {
			s = getName(i);
			o = getValue(i);
			if (o instanceof Integer) o = new Integer(((Integer)o).intValue());
			else if (o instanceof Double) o = new Double(((Double)o).doubleValue());
			else o = new Boolean(((Boolean)o).booleanValue());
			res.addValue(s, o);
		}
		
		return res;
	}

	// to string
	
	public String toString()
	{
		int i, n;
		String s;
		
		n = getNumValues();
		s = "";
		for (i = 0; i < n; i++) {
			s += getName(i) + "=" + valToString(getValue(i));
			if (i < n-1) s += ", ";
		}
		
		return s;
	}
	
	private String valToString(Object o)
	{
		String s;
		
		if (o instanceof Double) {
			NumberFormat nf = new DecimalFormat();
			nf.setMaximumFractionDigits(6);
			s = nf.format(((Double)o).doubleValue());
		} else {
			s = o.toString();
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
