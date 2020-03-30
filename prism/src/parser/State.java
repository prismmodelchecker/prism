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

import java.util.Arrays;
import java.util.List;

import prism.ModelInfo;
import prism.PrismLangException;

/**
 * Class to store a model state, i.e. a mapping from variables to values.
 * Stores as an array of Objects, where indexing is defined by a model. 
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
	 * Need access to model info in case variables are not ordered correctly.
	 * Throws an exception if any variables are undefined. 
	 * @param v Values object to copy.
	 * @param modelInfo Model info (for variable info/ordering)
	 */
	public State(Values v, ModelInfo modelInfo) throws PrismLangException
	{
		this(v, modelInfo, true);
	}

	/**
	 * Construct by copying existing Values object.
	 * Need access to model info in case variables are not ordered correctly.
	 * If requested, throws an exception if any variables are undefined. 
	 * @param v Values object to copy.
	 * @param modelInfo Model info (for variable info/ordering)
	 * @param checkAllDef If true, check all variables are present
	 */
	public State(Values v, ModelInfo modelInfo, boolean checkAllDef) throws PrismLangException
	{
		int i, j, n;
		n = v.getNumValues();
		if (checkAllDef && n != modelInfo.getNumVars()) {
			throw new PrismLangException("Wrong number of variables in state");
		}
		varValues = new Object[n];
		for (i = 0; i < n; i++) {
			varValues[i] = null;
		}
		for (i = 0; i < n; i++) {
			j = modelInfo.getVarIndex(v.getName(i));
			if (j == -1) {
				throw new PrismLangException("Unknown variable " + v.getName(i) + " in state");
			}
			if (varValues[i] != null) {
				throw new PrismLangException("Duplicated variable " + v.getName(i) + " in state");
			}
			varValues[i] = v.getValue(i);
		}
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
	 * Set the {@code i}th value to {@code val}.
	 */
	public State setValue(int i, Object val)
	{
		varValues[i] = val;
		return this;
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
		return Arrays.hashCode(varValues);
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

	@Override
	public int compareTo(State s)
	{
		return compareTo(s, 0);
	}
	
	/**
	 * Compare this state to another state {@code s} (in the style of {@link #compareTo(State)},
	 * first comparing variables with index greater than or equal to {@code j},
	 * and then comparing variables with index less than {@code j}.
	 */
	public int compareTo(State s, int j)
	{
		int i, c, n;
		Object svv[];
		
		// Can't compare to null
		if (s == null)
			throw new NullPointerException();
		
		// States of different size are incomparable 
		svv = s.varValues;
		n = varValues.length;
		if (n != svv.length)
			throw new ClassCastException("States are different sizes");
		
		if (j > n-1)
			throw new ClassCastException("Variable index is incorrect");
		
		// Go through variables j...n-1
		for (i = j; i < n; i++) {
			c = compareObjects(varValues[i], svv[i]);
			if (c != 0)
				return c;
			else
				continue;
		}
		
		// Go through variables 0...j
		for (i = 0; i < j; i++) {
			c = compareObjects(varValues[i], svv[i]);
			if (c != 0)
				return c;
			else
				continue;
		}
		
		return 0;
	}

	/**
	 * Get string representation, e.g. "(0,true,5)". 
	 */
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
	 * Get string representation, without outer parentheses, e.g. "0,true,5". 
	 */
	public String toStringNoParentheses()
	{
		int i, n;
		String s = "";
		n = varValues.length;
		for (i = 0; i < n; i++) {
			if (i > 0)
				s += ",";
			s += varValues[i];
		}
		return s;
	}

	/**
	 * Get string representation, e.g. "(a=0,b=true,c=5)", 
	 * with variables names (taken from a String list). 
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
	 * Get string representation, e.g. "(a=0,b=true,c=5)", 
	 * with variables names (taken from model info). 
	 */
	public String toString(ModelInfo modelInfo)
	{
		int i, n;
		String s = "(";
		n = varValues.length;
		for (i = 0; i < n; i++) {
			if (i > 0)
				s += ",";
			s += modelInfo.getVarName(i) + "=" + varValues[i];
		}
		s += ")";
		return s;
	}
	
	/**
	 * Utility method for comparing values stored as Objects.
	 * Return values are as for the standard Comparable.compareTo method.
	 */
	@SuppressWarnings("unchecked")
	public static int compareObjects(Object o1, Object o2)
	{
		// Things that already implement Comparable (Integer, Double)
		if (o1 instanceof Comparable && o2 instanceof Comparable && o1.getClass().equals(o2.getClass())) {
			return ((Comparable<Object>) o1).compareTo((Comparable<Object>) o2);
		}
		// Two lists of comparable objects
		else if (o1 instanceof List && o2 instanceof List) {
			List<?> l1 = (List<?>) o1;
			List<?> l2 = (List<?>) o2;
			int size = l1.size();
			if (l2.size() != size) {
				throw new ClassCastException("Can't compare " + o1 + " and " + o2 + " since their sizes differ");
			}
			for (int i = 0; i < size; i++) {
				int c = compareObjects(l1.get(i), l2.get(i));
				if (c != 0)
					return c;
				else
					continue;
			}
			return 0;
		} else {
			throw new ClassCastException("Can't compare " + o1.getClass() + " and " + o2.getClass());
		}
	}
}
