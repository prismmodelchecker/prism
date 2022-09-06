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

package jdd;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

/**
 * Container for MTBDD variables.
 * Each variable is represented by a JDDNode (result of JDD.Var(), a projection function).
 *
 * It is assumed in general that each JDDNode held in a JDDVars container
 * counts as a single reference and that a JDDVars object is cleared using derefAll()
 * when no longer used. This will dereference all the variables contained in the JDDVars
 * object.
 */
public class JDDVars implements Iterable<JDDNode>
{
	private Vector<JDDNode> vars;
	private long array;
	private boolean arrayBuilt;
	
	private native long DDV_BuildArray();
	private native void DDV_FreeArray(long a);
	private native int DDV_GetIndex(long dd);

	static
	{
		try {
			System.loadLibrary("jdd");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	/**
	 * Constructor.
	 */
	public JDDVars()
	{
		vars = new Vector<JDDNode>();
		array = 0;
		arrayBuilt = false;
	}

	/**
	 * Appends a variable to this JDDVars container.
	 * <br>
	 * [ DEREFs: var (on derefAll call) ]
	 */
	public void addVar(JDDNode var)
	{
		vars.addElement(var);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}
	
	/**
	 * Appends the variables of another JDDVars container to this container.
	 * Does not increase the refcount of the JDDNodes!
	 * <br>
	 * This method is deprecated, better use copy() or copyVarsFrom() instead.
	 * These simplify variable reference count debugging.
	 */
	@Deprecated
	public void addVars(JDDVars ddv)
	{
		vars.addAll(ddv.vars);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}

	/**
	 * Creates a copy of this JDDVars container,
	 * containing referenced copies of each variable JDDNode in this container.
	 */
	public JDDVars copy()
	{
		JDDVars result = new JDDVars();
		for (JDDNode var : this) {
			result.addVar(var.copy());
		}
		return result;
	}

	/**
	 * Copies variables from another JDDVars container,
	 * appending to this container.
	 * Does a (referencing) copy of each of the variable JDDNodes.
	 */
	public void copyVarsFrom(JDDVars ddv) {
		for (JDDNode var : ddv) {
			addVar(var.copy());
		}
	}
	
	/**
	 * Copy an array of JDDVars[] by copying each JDDVars container.
	 * The copy will have fully referenced JDDNodes.
	 */
	public static JDDVars[] copyArray(JDDVars[] vararray)
	{
		JDDVars[] result = new JDDVars[vararray.length];
		for (int i = 0;  i< vararray.length; i++) {
			result[i] = vararray[i].copy();
		}
		return result;
	}


	/**
	 * Copy JDDNodes from another JDDVars, merge into the existing variables,
	 * sorting by the variable indices. Afterwards, this JDDVars container
	 * is fully sorted by variable indices, i.e., the existing variables are
	 * sorted as well.
	 * @param ddv the new variables
	 */
	public void mergeVarsFrom(JDDVars ddv) {
		copyVarsFrom(ddv);
		sortByIndex();
	}

	/**
	 * Remove variable v from container. Does not decrease the refcount.
	 */
	public void removeVar(JDDNode v)
	{
		vars.remove(v);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}

	/**
	 * Removes the JDDNodes contained in ddv from this JDDVars container.
	 * Does not decrease the refcount!
	 */
	public void removeVars(JDDVars ddv)
	{
		vars.removeAll(ddv.vars);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}

	/** Returns the number of variables stored in this JDDVars container. */
	public int getNumVars()
	{
		return vars.size();
	}

	/**
	 * Returns the JDDNode for the i-th stored variable.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getVar(int i)
	{
		return (JDDNode)vars.elementAt(i);
	}

	/**
	 * Returns the internal Cudd pointer for the i-th stored variable.
	 */
	public long getVarPtr(int i)
	{
		return ((JDDNode)vars.elementAt(i)).ptr();
	}

	/**
	 * Returns the Cudd variable index for the i-th stored variable.
	 */
	public int getVarIndex(int i)
	{
		return DDV_GetIndex(((JDDNode)vars.elementAt(i)).ptr());
	}

	/**
	 * Returns the minimal Cudd variable index for the stored variables,
	 * or -1 if there are no stored variables.
	 */
	public int getMinVarIndex()
	{
		int i, j, n, min;
		n = vars.size();
		if (n == 0) return -1;
		min = getVarIndex(0);
		for (i = 1; i < n; i++) {
			j = getVarIndex(i);
			if (j < min) min = j;
		}
		return min;
	}

	/**
	 * Returns the maximal Cudd variable index for the stored variables,
	 * or -1 if there are no stored variables.
	 */
	public int getMaxVarIndex()
	{
		int i, j, n, max;
		n = vars.size();
		if (n == 0) return -1;
		max = getVarIndex(0);
		for (i = 1; i < n; i++) {
			j = getVarIndex(i);
			if (j > max) max = j;
		}
		return max;
	}

	/**
	 * Increases the refcount of all contained JDDNodes.
	 * <br>
	 * This method is deprecated, please use copy() and
	 * copyVarsFrom() instead.
	 * This simplifies reference counting debugging.
	 */
	@Deprecated
	public void refAll()
	{
		int i;
		
		for (i = 0; i < vars.size(); i++) {
			JDD.Ref((JDDNode)vars.elementAt(i));
		}
	}

	/**
	 * Decreases the refcount of all contained JDDNodes.
	 */
	public void derefAll()
	{
		int i;

		for (i = 0; i < vars.size(); i++) {
			JDD.Deref((JDDNode)vars.elementAt(i));
		}
	}

	/**
	 * Calls derefAll on all JDDVars elements of a JDDVars[] array.
	 */
	public static void derefAllArray(JDDVars[] vars)
	{
		for (JDDVars v : vars) {
			v.derefAll();
		}
	}

	/**
	 * Constructs a JNI array for the stored variables
	 * that can be passed to the C-based functions.
	 */
	public long array()
	{
		if (arrayBuilt) {
			return array;
		}
		else {
			array = DDV_BuildArray();
			arrayBuilt = true;
			return array;
		}
	}

	/**
	 * Returns the number of stored variables.
	 */
	public int n()
	{
		return vars.size();
	}

	@Override
	public Iterator<JDDNode> iterator()
	{
		return vars.iterator();
	}

	@Override
	public String toString()
	{
		int i;
		String s = "{";
		
		for (i = 0; i < vars.size() - 1; i++) {
			s = s + getVarIndex(i) + ", ";
		}
		if (vars.size() > 0) {
			s = s + getVarIndex(vars.size() - 1);
		}
		s += "}";
		
		return s;
	}
	
	
	/**
	 * Converts a DD cubeset (conjunction of variables)
	 * to a corresponding JDDVars array.<br>
	 * <br> [ REFS: <i>the variables in the returned JDDVars container</i>, DEREFS: cubeSet ]
	 */
	public static JDDVars fromCubeSet(JDDNode cubeSet)
	{
		try {
			JDDVars result = new JDDVars();

			JDDNode current = cubeSet;
			// We do not need to bother with reference manipulation,
			// as we only call getThen() and getElse(), which do not increase
			// the refcount
			while (!current.equals(JDD.ONE)) {
				if (current.isConstant()) {
					// may not be any other constant than ONE
					throw new IllegalArgumentException("JDDVars.fromCubeSet: The argument is not a cubeset");
				}
				if (!current.getElse().equals(JDD.ZERO)) {
					// else always has to point to ZERO
					throw new IllegalArgumentException("JDDVars.fromCubeSet: The argument is not a cubeset");
				}

				int index = current.getIndex();
				JDDNode var = JDD.Var(index);
				result.addVar(var);

				current = current.getThen();
			}

			return result;
		} finally {
			JDD.Deref(cubeSet);
		}
	}

	/**
	 * Constructs a DD cubeset (conjunction of variables)
	 * corresponding to this JDDVars container.<br>
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode toCubeSet()
	{
		JDDNode result = JDD.Constant(1);
		for (JDDNode var : vars) {
			result = JDD.And(result, var.copy());
		}
		return result;
	}

	/**
	 * Constructs a 0/1-ADD that is the conjunction of
	 * the negated variables, i.e.,
	 * And(Not(v_1), Not(v_2), ..., Not(v_n))
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode allZero()
	{
		JDDNode result = JDD.Constant(1);
		for (JDDNode var : vars) {
			result = JDD.And(result, JDD.Not(var.copy()));
		}
		return result;
	}

	/** Sort the variables in this container by their variable index. */
	public void sortByIndex()
	{
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;

		Collections.sort(vars, new Comparator<JDDNode>() {
			@Override
			public int compare(JDDNode a, JDDNode b)
			{
				return new Integer(a.getIndex()).compareTo(b.getIndex());
			}
		});
	}
}

//------------------------------------------------------------------------------
