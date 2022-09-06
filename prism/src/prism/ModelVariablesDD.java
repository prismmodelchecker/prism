//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package prism;

import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Management and information storage about the DD variables
 * used by a symbolic model.
 * <br>
 * After use, the variable references should be cleared by
 * calling {@code clear()}.
 */
public class ModelVariablesDD
{
	/** Vector of DD variable names for the allocated variables */
	private Vector<String> ddVarNames = new Vector<String>();
	/** The allocated variables */
	private JDDVars ddVariables = new JDDVars();
	/** The next free variable index */
	private int nextDDVarIndex = 0;

	/** Storage for the preallocated extra state variables */
	private JDDVars extraStateVariables = new JDDVars();
	/** Storage for the preallocated extra action variables */
	private JDDVars extraActionVariables = new JDDVars();

	/** Constructor */
	public ModelVariablesDD()
	{
	}

	/**
	 * Create a copy of this ModelVariablesDD.
	 * All variable information is copied, i.e., the
	 * result has to be cleared after use.
	 */
	public ModelVariablesDD copy()
	{
		ModelVariablesDD result = new ModelVariablesDD();
		result.ddVarNames = new Vector<String>(ddVarNames);
		result.ddVariables = ddVariables.copy();
		result.extraStateVariables = extraStateVariables.copy();
		result.extraActionVariables = extraActionVariables.copy();
		result.nextDDVarIndex = nextDDVarIndex;

		return result;
	}

	/** Clear all variable references */
	public void clear()
	{
		ddVariables.derefAll();
		extraStateVariables.derefAll();
		extraActionVariables.derefAll();
	}

	/** Return the vector with variable names for a given variable index */
	public Vector<String> getDDVarNames()
	{
		return ddVarNames;
	}

	/**
	 * Allocate a new variable for the model.
	 * <br>[ REFS: <i>result</i> ]
	 * @param name a name for the variable
	 */
	public JDDNode allocateVariable(String name)
	{
		JDDNode v = JDD.Var(nextDDVarIndex);
		nextDDVarIndex++;

		ddVariables.addVar(v.copy());

		ddVarNames.add(name);
		return v;
	}

	/** Preallocate the given number of state variables */
	public void preallocateExtraStateVariables(int count)
	{
		for (int i=0; i<count; i++) {
			JDDNode v = allocateVariable("");
			extraStateVariables.addVar(v);
		}
	}

	/**
	 * Allocate a new state variable from the pool of
	 * preallocated state variables.
	 * <br>
	 * If the pool is empty, allocate a new variable
	 * at the end.
	 * <br> [ REFS: <i>result</i> ]
	 * @param name the name for the variable
	 */
	public JDDNode allocateExtraStateVariable(String name)
	{
		JDDNode v;
		if (extraStateVariables.getNumVars() > 0) {
			v = extraStateVariables.getVar(0);
			extraStateVariables.removeVar(v);

			ddVarNames.set(v.getIndex(), name);
		} else {
			// allocate at the end
			v = allocateVariable(name);
		}
		return v;
	}

	/**
	 * Returns true if there are enough preallocated state variables
	 * such that {@code 2*bits} variables can be allocated from the pool
	 */
	public boolean canPrependExtraStateVariable(int bits)
	{
		return (bits*2 <= extraStateVariables.getNumVars());
	}

	/**
	 * Allocate {@code bits} pairs of row and col variables.
	 * <br>
	 * If {@code prepend} is true and there is not enough space
	 * in the preallocated state variable pool, throws an exception.
	 * <br>[ REFS: <i>result</i> ]
	 * @param bits number of state variable pairs
	 * @param name the name of the variable
	 * @param prepend should the variables be allocated from the preallocated pool?
	 */
	public JDDVars allocateExtraStateVariable(int bits, String name, boolean prepend) throws PrismException
	{
		int newVarCount = 2*bits;

		JDDVars result = new JDDVars();
		if (newVarCount == 0) return result;

		if (prepend) {
			if (!canPrependExtraStateVariable(bits)) {
				throw new PrismException("Can not prepend "+(newVarCount)+" extra row/col variables");
			}

			// take the bottom nVarCount extra variables from the
			// extraStateVariables array, starting with index v
			int v = extraStateVariables.getNumVars() - newVarCount;
			for (int i=0; i < bits; i++) {
				// transfer row, col from extraStateVariables to result,
				// no need to ref again
				JDDNode row_var = extraStateVariables.getVar(v++);
				JDDNode col_var = extraStateVariables.getVar(v++);

				result.addVar(row_var);
				result.addVar(col_var);

				ddVarNames.set(row_var.getIndex(), name+"."+i);
				ddVarNames.set(col_var.getIndex(), name+"'."+i);
			}

			// remove variables from extraStateVariables
			extraStateVariables.removeVars(result);

			return result;
		} else {
			for (int i=0; i < bits; i++) {
				result.addVar(allocateVariable(name+"."+i));
				result.addVar(allocateVariable(name+"'."+i));
			}
		}

		return result;
	}

	/** Preallocate the given number of action variables */
	public void preallocateExtraActionVariables(int count)
	{
		for (int i=0;i<count;i++) {
			JDDNode v = allocateVariable("");
			extraActionVariables.addVar(v);
		}
	}

	/**
	 * Allocate {@code n} action variables.
	 * <br>
	 * If there is not enough space in the preallocated variable pool, throws an exception.
	 * <br>[ REFS: <i>result</i> ]
	 * @param n number of action variables to allocate
	 * @param name the name of the variable
	 * 	 */
	public JDDVars allocateExtraActionVariable(int n, String name) throws PrismException
	{
		JDDVars result = new JDDVars();
		if (n == 0) return result;

		if (extraActionVariables.getNumVars() < n) {
			throw new PrismException("Not enough extra action variables preallocated, please increase using -ddextraactionvars switch!");
		}

		int v = extraActionVariables.getNumVars() - n;
		for (int i=0; i < n; i++) {
			// transfer action var from extraActionVariables to result,
			// no need to ref again
			JDDNode action_var = extraActionVariables.getVar(v++);
			result.addVar(action_var);
			ddVarNames.set(action_var.getIndex(), name+"."+i);
		}

		// remove variables from extraActionVariables
		extraActionVariables.removeVars(result);

		return result;
	}

	/**
	 * Get the extra state variables.
	 * This is not a copy.
	 *
	 * <br>[ REFs: <i>none</i>, DEREFs: <i>none</i> ]
	 */
	public JDDVars getExtraStateVariables() {
		return extraStateVariables;
	}

	/**
	 * Get the extra action variables.
	 * This is not a copy.
	 *
	 * <br>[ REFs: <i>none</i>, DEREFs: <i>none</i> ]
	 */
	public JDDVars getExtraActionVariables() {
		return extraActionVariables;
	}

}
