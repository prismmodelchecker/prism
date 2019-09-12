//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.*;

import jdd.*;
import odd.*;
import parser.State;
import parser.Values;
import parser.VarList;

/**
 * Stores a list of states as a BDD (or as a 0-1 MTBDD).
 */
public class StateListMTBDD implements StateList
{
	// States list, as a (0-1) MTBDD
	protected JDDNode states;
	
	// Info needed from model
	protected JDDVars vars;
	protected int numVars;
	protected ODDNode odd;
	protected VarList varList;
	protected double size;
	
	// stuff to keep track of variable values in print method
	protected int[] varSizes;
	protected int[] varValues;
	protected int currentVar;
	protected int currentVarLevel;
	
	// stuff to control printing limit
	protected boolean limit;
	protected int numToPrint;
	protected int count;
	
	// log for output from print method
	protected PrismLog outputLog;
	
	// string array when exporting
	protected List<String> strList;
	
	// output format
	protected enum OutputFormat { NORMAL, MATLAB, DOT, STRINGS };
	protected OutputFormat outputFormat = OutputFormat.NORMAL;
	
	// Constructors

	/**
	 * Create a states list from a BDD and the associated model.
	 * @param states The list of states
	 * @param model The model
	 */
	public StateListMTBDD(JDDNode states, Model model)
	{
		int i;
		
		// store states list bdd
		this.states = states;
		
		// get info from model
		vars = model.getAllDDRowVars();
		numVars = vars.n();
		odd = model.getODD();
		varList = model.getVarList();
		
		// count number of states in list
		size = JDD.GetNumMinterms(states, model.getNumDDRowVars());
		
		// initialise arrays
		varSizes = new int[varList.getNumVars()];
		for (i = 0; i < varList.getNumVars(); i++) {
			varSizes[i] = varList.getRangeLogTwo(i);
		}
		varValues = new int[varList.getNumVars()];
	}

	/**
	 * Create a states list from a BDD and the associated info about variables/indexing.
	 * @param states The list of states
	 * @param vars BDD variables used to represent states
	 * @param odd ODD storing state indexing info
	 * @param varList Information about (language-level) variables
	 */
	public StateListMTBDD(JDDNode states, JDDVars vars, ODDNode odd, VarList varList)
	{
		int i;

		// store states vector mtbdd
		this.states = states;

		// store variable/indexing info
		this.vars = vars;
		this.numVars = vars.n();
		this.odd = odd;
		this.varList = varList;

		// count number of states in list
		size = JDD.GetNumMinterms(states, numVars);

		// initialise arrays
		varSizes = new int[varList.getNumVars()];
		for (i = 0; i < varList.getNumVars(); i++) {
			varSizes[i] = varList.getRangeLogTwo(i);
		}
		varValues = new int[varList.getNumVars()];
	}

	@Override
	public int size()
	{
		return (size > Integer.MAX_VALUE) ? -1 : (int)Math.round(size);
	}
	
	@Override
	public String sizeString()
	{
		return (size > Long.MAX_VALUE) ? "" + size : "" + Math.round(size);
	}

	@Override
	public void print(PrismLog log)
	{
		outputFormat = OutputFormat.NORMAL;
		limit = false;
		outputLog = log;
		doPrint();
		if (count == 0)
			outputLog.println("(none)");
	}
	
	@Override
	public void print(PrismLog log, int n)
	{
		outputFormat = OutputFormat.NORMAL;
		limit = true;
		numToPrint = n;
		outputLog = log;
		doPrint();
		if (count == 0)
			outputLog.println("(none)");
	}
	
	@Override
	public void printMatlab(PrismLog log)
	{
		outputFormat = OutputFormat.MATLAB;
		limit = false;
		outputLog = log;
		doPrint();
	}
	
	@Override
	public void printMatlab(PrismLog log, int n)
	{
		outputFormat = OutputFormat.MATLAB;
		limit = true;
		numToPrint = n;
		outputLog = log;
		doPrint();
	}
	
	@Override
	public void printDot(PrismLog log) throws PrismException
	{
		outputFormat = OutputFormat.DOT;
		limit = false;
		outputLog = log;

		if (odd == null) {
			throw new PrismNotSupportedException("Cannot export state list as DOT, too many states");
		}
		doPrint();
	}
	
	@Override
	public List<String> exportToStringList()
	{
		strList = new ArrayList<String>((int)size);
		outputFormat = OutputFormat.STRINGS;
		limit = false;
		doPrint();
		return strList;
	}
	
	/**
	 * Implementation of printing.
	 */
	private void doPrint()
	{
		int i;
		
		count = 0;
		for (i = 0; i < varList.getNumVars(); i++) {
			varValues[i] = 0;
		}
		currentVar = 0;
		currentVarLevel = 0;
		printRec(states, 0, odd, 0);
		//log.println();
	}

	/**
	 * Main recursive part of state printing.
	 * NB: Traversal of the MTBDD/ODD is quite simple; the  tricky bit is keeping track of variable values
	 * throughout traversal - we want to be efficient and not compute the values from scratch each
	 * time, but we also want to avoid passing arrays into the recursive method.
	 */
	private void printRec(JDDNode dd, int level, ODDNode o, long n)
	{
		int i, j;
		JDDNode e, t;
		String varsString;
		
		// if we've printed enough states, stop
		if (limit) if (count >= numToPrint) return;
		
		// zero constant - bottom out of recursion
		if (dd.equals(JDD.ZERO)) return;
		
		// base case - at bottom (nonzero terminal)
		if (level == numVars) {
			
			switch (outputFormat) {
			case NORMAL:
				if (o != null) {
					outputLog.print(n + ":(");
				} else {
					// we have no index
					outputLog.print("(");
				}
				break;
			case MATLAB: break;
			case DOT:
				assert(o != null);  // should not happen, missing ODD is caught before
				outputLog.print(n + " [label=\"" + n + "\\n(");
				break;
			case STRINGS: break;
			}
			j = varList.getNumVars();
			varsString = "";
			for (i = 0; i < j; i++) {
				varsString += varList.decodeFromInt(i, varValues[i]).toString();
				if (i < j-1) {
					varsString += ",";
				}
			}
			switch (outputFormat) {
			case NORMAL: outputLog.println(varsString + ")"); break;
			case MATLAB: outputLog.println(varsString); break;
			case DOT: outputLog.println(varsString + ")\"];"); break;
			case STRINGS: strList.add(varsString);
			}
			count++;
			
			return;
		}
		
		// select else and then branches
		else if (dd.getIndex() > vars.getVarIndex(level)) {
			e = t = dd;
		}
		else {
			e = dd.getElse();
			t = dd.getThen();
		}

		ODDNode oe = (o != null ? o.getElse() : null);
		ODDNode ot = (o != null ? o.getThen() : null);
		long eoff = (o != null ? o.getEOff() : 0);

		// then recurse...
		currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
		printRec(e, level+1, oe, n);
		currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
		varValues[currentVar] += (1 << (varSizes[currentVar]-1-currentVarLevel));
		currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
		printRec(t, level+1, ot, n + eoff);
		currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
		varValues[currentVar] -= (1 << (varSizes[currentVar]-1-currentVarLevel));
	}

	@Override
	public boolean includes(JDDNode set)
	{
		JDDNode tmp;
		boolean incl;
		
		JDD.Ref(states);
		JDD.Ref(set);
		tmp = JDD.And(states, set);
		incl = !tmp.equals(JDD.ZERO);
		JDD.Deref(tmp);
		
		return incl;	
	}

	@Override
	public boolean includesAll(JDDNode set)
	{
		JDDNode tmp;
		boolean incl;
		
		JDD.Ref(states);
		JDD.Ref(set);
		tmp = JDD.And(states, set);
		incl = tmp.equals(set);
		JDD.Deref(tmp);
		
		return incl;	
	}

	@Override
	public Values getFirstAsValues() throws PrismException
	{
		Values values;
		int i, j, n, n2, level, v;
		JDDNode first, tmp;
		Object o;
		
		// check there is a first state
		if (size < 1) throw new PrismException("The state list contains no states");
		
		// get bdd of first state
		JDD.Ref(states);
		first = JDD.RestrictToFirst(states, vars);
		
		// traverse bdd, top to bottom, getting val (v) for each var
		tmp = states;
		values = new Values();
		n = varList.getNumVars();
		level = 0;
		for (i = 0; i < n; i++) {
			v = 0;
			n2 = varSizes[i];
			for (j = 0; j < n2; j++) {
				if (tmp.getIndex() > vars.getVarIndex(level)) {
					// tmp = tmp;
				} else if (!tmp.getElse().equals(JDD.ZERO)) {
					tmp = tmp.getElse();
				} else {
					tmp = tmp.getThen();
					v += (1 << (n2-1-j));
				}
				level++;
			}
			o = varList.decodeFromInt(i, v);
			values.addValue(varList.getName(i), o);
		}
		
		// derefs
		JDD.Deref(first);
		
		return values;
	}

	@Override
	public int getIndexOfState(State state) throws PrismNotSupportedException
	{
		// Traverse BDD/ODD, top to bottom, computing index
		JDDNode ptr = states;
		ODDNode o = odd;

		ODDUtils.checkInt(odd, "Cannot get index of state in model");

		int level = 0;
		int index = 0;
		// Iterate through variables
		int n = varList.getNumVars();
		for (int i = 0; i < n; i++) {
			int valInt = -1;
			try {
				valInt = varList.encodeToInt(i, state.varValues[i]); 
			} catch (PrismLangException e) {
				// Problem looking up variable - bail out 
				return -1;
			}
			// Iterate through bits for this variable
			int n2 = varSizes[i];
			for (int j = 0; j < n2; j++) {
				// Traverse BDD (need to double check state is in the set)
				if (ptr.equals(JDD.ZERO)) {
					return -1;
				} else if (ptr.getIndex() > vars.getVarIndex(level)) {
					// ptr = ptr;
				} else if ((valInt & (1 << (n2-1-j))) == 0) {
					ptr = ptr.getElse();
				} else {
					ptr.getThen();					
				}
				level++;
				// Traverse ODD (to get index)
				if ((valInt & (1 << (n2-1-j))) == 0) {
					o = o.getElse();
				} else {
					index += o.getEOff();
					o = o.getThen();	
				}
				
			}
		}
		return index;
	}
	
	@Override
	public void clear()
	{
		JDD.Deref(states);
	}
}
