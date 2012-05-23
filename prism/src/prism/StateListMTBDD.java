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

package prism;

import java.util.*;

import jdd.*;
import odd.*;
import parser.Values;
import parser.VarList;
import parser.type.*;

// list of states (mtbdd)

public class StateListMTBDD implements StateList
{
	// states vector mtbdd
	JDDNode states;
	
	// info from model
	JDDVars vars;
	int numVars;
	ODDNode odd;
	VarList varList;
	double size;
	
	// stuff to keep track of variable values in print method
	int[] varSizes;
	int[] varValues;
	int currentVar;
	int currentVarLevel;
	
	// stuff to control printing limit
	boolean limit;
	int numToPrint;
	int count;
	
	// log for output from print method
	PrismLog outputLog;
	
	// string array when exporting
	List<String> strList;
	
	// output format
	enum OutputFormat { NORMAL, MATLAB, DOT, STRINGS };
	OutputFormat outputFormat = OutputFormat.NORMAL;
	
	// Constructors

	public StateListMTBDD(JDDNode s, Model model)
	{
		int i;
		
		// store states vector mtbdd
		states = s;
		
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

	public StateListMTBDD(JDDNode s, JDDVars vars, ODDNode odd, VarList varList)
	{
		int i;

		// store states vector mtbdd
		states = s;

		// get info from model
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

	// return size (number of states in list)
	
	public int size()
	{
		return (size > Integer.MAX_VALUE) ? -1 : (int)Math.round(size);
	}
	
	public String sizeString()
	{
		return (size > Long.MAX_VALUE) ? "" + size : "" + Math.round(size);
	}

	// print/export whole list
	
	public void print(PrismLog log)
	{
		outputFormat = OutputFormat.NORMAL;
		limit = false;
		outputLog = log;
		doPrint();
		if (count == 0)
			outputLog.println("(none)");
	}
	public void printMatlab(PrismLog log)
	{
		outputFormat = OutputFormat.MATLAB;
		limit = false;
		outputLog = log;
		doPrint();
	}
	public void printDot(PrismLog log)
	{
		outputFormat = OutputFormat.DOT;
		limit = false;
		outputLog = log;
		doPrint();
	}
	public List<String> exportToStringList()
	{
		strList = new ArrayList<String>((int)size);
		outputFormat = OutputFormat.STRINGS;
		limit = false;
		doPrint();
		return strList;
	}
	
	// print first n states of list
	
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
	
	public void printMatlab(PrismLog log, int n)
	{
		outputFormat = OutputFormat.MATLAB;
		limit = true;
		numToPrint = n;
		outputLog = log;
		doPrint();
	}
	
	// printing method
	
	public void doPrint()
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

	// recursive bit of print
	// (nb: traversal of mtbdd/odd is quite simple,
	//  tricky bit is keeping track of variable values
	//  throughout traversal - we want to be efficient
	//  and not compute the values from scratch each
	//  time, but we also want to avoid passing arrays
	//  into the resursive method)
	
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
			case NORMAL: outputLog.print(n + ":("); break;
			case MATLAB: break;
			case DOT: outputLog.print(n + " [label=\"" + n + "\\n("); break;
			}
			j = varList.getNumVars();
			varsString = "";
			for (i = 0; i < j; i++) {
				// integer variable
				if (varList.getType(i) instanceof TypeInt) {
					varsString += varValues[i]+varList.getLow(i);
				}
				// boolean variable
				else {
					varsString += (varValues[i] == 1);
				}
				if (i < j-1) varsString += ",";
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
		
		// then recurse...
		currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
		printRec(e, level+1, o.getElse(), n);
		currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
		varValues[currentVar] += (1 << (varSizes[currentVar]-1-currentVarLevel));
		currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
		printRec(t, level+1, o.getThen(), n+o.getEOff());
		currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
		varValues[currentVar] -= (1 << (varSizes[currentVar]-1-currentVarLevel));
	}

	/**
	 * Check for (partial) state inclusion - state(s) given by BDD
	 */
	public boolean includes(JDDNode state)
	{
		JDDNode tmp;
		boolean incl;
		
		JDD.Ref(states);
		JDD.Ref(state);
		tmp = JDD.And(states, state);
		incl = !tmp.equals(JDD.ZERO);
		JDD.Deref(tmp);
		
		return incl;	
	}

	/**
	 * Check for (full) state inclusion - state(s) given by BDD
	 */
	public boolean includesAll(JDDNode state)
	{
		JDDNode tmp;
		boolean incl;
		
		JDD.Ref(states);
		JDD.Ref(state);
		tmp = JDD.And(states, state);
		incl = tmp.equals(state);
		JDD.Deref(tmp);
		
		return incl;	
	}

	// get first state as Values object
	
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
			v += varList.getLow(i);
			if (varList.getType(i) instanceof TypeInt) {
				o = new Integer(v);
			} else {
				o = new Boolean(v == 1);
			}
			values.addValue(varList.getName(i), o);
		}
		
		// derefs
		JDD.Deref(first);
		
		return values;
	}

	// clear
	
	public void clear()
	{
		JDD.Deref(states);
	}
}

