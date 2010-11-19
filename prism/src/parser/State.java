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

package parser;

import java.util.*;

import parser.ast.ModulesFile;
import prism.PrismLangException;

/**
 * Class to store a model state, i.e. a mapping from variables to values.
 * Stores as an array of Objects, where indexing is defined by the ModulesFile. 
 */
public class State implements Comparable<State>
{
	public Object varValues[];

	/**
	 * Construct empty (uninitialised) state.
	 * @param n Number of variables.
	 */
	public State(int n)
	{
		varValues = new Object[n];
	}

	/**
	 * Construct by copying existing State object.
	 * @param s State to copy.
	 */
	public State(State s)
	{
		this(s.varValues.length);
		copy(s);
	}

	/**
	 * Construct by concatenating two existing State objects.
	 */
	public State(State s1, State s2)
	{
		Object[] arr1 = (Object[]) s1.varValues;
		Object[] arr2 = (Object[]) s2.varValues;
		varValues = new Object[arr1.length + arr2.length];
		int i;
		for (i = 0; i < arr1.length; i++)
			varValues[i] = arr1[i];
		for (i = 0; i < arr2.length; i++)
			varValues[arr1.length + i] = arr2[i];
	}

	/**
	 * Construct by copying existing Values object.
	 * @param v Values object to copy.
	 */
	//FIXME:don't assume v has correct order  
	public State(Values v) throws PrismLangException
	{
		int i, n;
		n = v.getNumValues();
		varValues = new Object[n];
		for (i = 0; i < n; i++)
			varValues[i] = v.getValue(i);
	}

	/**
	 * Clear: set all values to null
	 */
	public void clear()
	{
		int i, n;
		n = varValues.length;
		for (i = 0; i < n; i++)
			varValues[i] = null;
	}

	/**
	 * 
	 */
	public void setValue(int i, Object val)
	{
		varValues[i] = val;
	}

	/**
	 * Copy contents of an existing state.
	 * @param s State to copy.
	 */
	public void copy(State s)
	{
		int i, n;
		n = varValues.length;
		for (i = 0; i < n; i++)
			varValues[i] = s.varValues[i];
	}

	@Override
	public int hashCode()
	{
		// Simple hash code
		return varValues.length > 0 ? varValues[0].hashCode() : 0;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof State))
			return false;

		int i, n;
		State s = (State) o;
		n = varValues.length;
		if (n != s.varValues.length)
			return false;
		for (i = 0; i < n; i++) {
			if (!(varValues[i]).equals(s.varValues[i]))
				return false;
		}
		return true;
	}

	public int compareTo(State s)
	{
		int i, j, n;
		Object svv[], o1, o2;
		// Can't compare to null
		if (s == null)
			throw new NullPointerException();
		// States of different size are incomparable 
		svv = s.varValues;
		n = varValues.length;
		if (n != svv.length)
			throw new ClassCastException("States are different sizes");
		// Go through array
		for (i = 0; i < n; i++) {
			o1 = varValues[i];
			o2 = svv[i];
			if (o1 instanceof Integer && o2 instanceof Integer) {
				j = ((Integer) o1).compareTo((Integer) o2);
				if (j != 0)
					return j;
				else
					continue;
			} else if (o1 instanceof Boolean && o2 instanceof Boolean) {
				j = ((Boolean) o1).compareTo((Boolean) o2);
				if (j != 0)
					return j;
				else
					continue;
			} else {
				throw new ClassCastException("Can't compare " + o1.getClass() + " and " + o2.getClass());
			}
		}
		return 0;
	}

	@Override
	public String toString()
	{
		int i, n;
		String s = "(";
		n = varValues.length;
		for (i = 0; i < n; i++) {
			if (i > 0)
				s += ",";
			s += varValues[i];
		}
		s += ")";
		return s;
	}

	/**
	 * Get string representation with variables names (taken from a String list). 
	 */
	public String toString(List<String> varNames)
	{
		int i, n;
		String s = "(";
		n = varValues.length;
		for (i = 0; i < n; i++) {
			if (i > 0)
				s += ",";
			s += varNames.get(i) + "=" + varValues[i];
		}
		s += ")";
		return s;
	}

	/**
	 * Get string representation with variables names (taken from a ModulesFile). 
	 */
	public String toString(ModulesFile mf)
	{
		int i, n;
		String s = "(";
		n = varValues.length;
		for (i = 0; i < n; i++) {
			if (i > 0)
				s += ",";
			s += mf.getVarName(i) + "=" + varValues[i];
		}
		s += ")";
		return s;
	}
}
