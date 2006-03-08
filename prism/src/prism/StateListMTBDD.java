//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

import parser.VarList;
import jdd.*;
import odd.*;
import parser.Values;
import parser.Expression;

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
	
	// output in Matlab format?
	boolean matlab = false;
	
	// constructor
	
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

	// return size (number of states in list)
	
	public int size()
	{
		return (size > Integer.MAX_VALUE) ? -1 : (int)size;
	}
	
	public String sizeString()
	{
		return (size > Long.MAX_VALUE) ? "" + size : "" + (long)size;
	}

	// print whole list
	
	public void print(PrismLog log)
	{
		limit = false;
		count = 0;
		doPrint(log);
	}
	public void printMatlab(PrismLog log)
	{
		matlab = true;
		print(log);
		matlab = false;
	}
	
	// print first n states of list
	
	public void print(PrismLog log, int n)
	{
		limit = true;
		numToPrint = n;
		count = 0;
		doPrint(log);
	}
	public void printMatlab(PrismLog log, int n)
	{
		matlab = true;
		print(log, n);
		matlab = false;
	}
	
	// printing method
	
	public void doPrint(PrismLog log)
	{
		int i;
		
		outputLog = log;
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
		int i;
		JDDNode e, t;
		
		// if we've printed enough states, stop
		if (limit) if (count >= numToPrint) return;
		
		// zero constant - bottom out of recursion
		if (dd.equals(JDD.ZERO)) return;
		
		// base case - at bottom (nonzero terminal)
		if (level == numVars) {
			
			if (!matlab) outputLog.print(n + ":(");
			for (i = 0; i < varList.getNumVars()-1; i++) {
				outputLog.print((varValues[i]+varList.getLow(i)) + ",");
			}
			i = varList.getNumVars()-1;
			outputLog.print((varValues[i]+varList.getLow(i)));
			if (!matlab) outputLog.print(")");
			outputLog.println();
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

	// check for state inclusion (state given by bdd)
	
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

	// get first state as Values object
	
	public Values getFirstAsValues() throws PrismException
	{
		Values values;
		int i, j, n, n2, v;
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
		for (i = 0; i < n; i++) {
			v = 0;
			n2 = varSizes[i];
			for (j = 0; j < n2; j++) {
				if (!tmp.getElse().equals(JDD.ZERO)) {
					tmp = tmp.getElse();
				} else {
					tmp = tmp.getThen();
					v += (1 << (n2-1-j));
				}
			}
			v += varList.getLow(i);
			if (varList.getType(i) == Expression.INT) {
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

