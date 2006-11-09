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

import jdd.*;
import odd.*;
import parser.VarList;
import parser.Expression;

// state probability vector (mtbdd)

public class StateProbsMTBDD implements StateProbs
{
	// prob vector mtbdd
	JDDNode probs;
	
	// info from model
	JDDVars vars;
	JDDNode reach;
	int numDDRowVars;
	int numVars;
	ODDNode odd;
	VarList varList;
	
	// stuff to keep track of variable values in print method
	int[] varSizes;
	int[] varValues;
	int currentVar;
	int currentVarLevel;
	
	// log for output from print method
	PrismLog outputLog;

	// CONSTRUCTOR
	
	public StateProbsMTBDD(JDDNode p, Model model)
	{
		int i;
		
		// store prob vector mtbdd
		probs = p;
		
		// get info from model
		vars = model.getAllDDRowVars();
		reach = model.getReach();
		numDDRowVars = model.getNumDDRowVars();
		numVars = vars.n();
		odd = model.getODD();
		varList = model.getVarList();
		
		// initialise arrays
		varSizes = new int[varList.getNumVars()];
		for (i = 0; i < varList.getNumVars(); i++) {
			varSizes[i] = varList.getRangeLogTwo(i);
		}
		varValues = new int[varList.getNumVars()];
	}

	// METHODS TO MODIFY VECTOR
	
	// round
	
	public void roundOff(int places)
	{
		probs = JDD.RoundOff(probs, places);
	}
	
	// subtract all probabilities from 1
	
	public void subtractFromOne() 
	{
		JDD.Ref(reach);
		probs = JDD.Apply(JDD.MINUS, reach, probs);
	}
	
	// add another vector to this one
	
	public void add(StateProbs sp) 
	{
		StateProbsMTBDD spm = (StateProbsMTBDD) sp;
		JDD.Ref(spm.probs);
		probs = JDD.Apply(JDD.PLUS, probs, spm.probs);
	}
	
	// multiply vector by a constant
	
	public void timesConstant(double d) 
	{
		probs = JDD.Apply(JDD.TIMES, probs, JDD.Constant(d));
	}
	
	// filter vector using a bdd (set elements not in filter to 0)
	
	public void filter(JDDNode filter)
	{
		JDD.Ref(filter);
		probs = JDD.Apply(JDD.TIMES, probs, filter);
	}
	
	// clear
	
	public void clear()
	{
		JDD.Deref(probs);
	}

	// METHODS TO ACCESS VECTOR DATA
	
	// get mtbdd
	
	public JDDNode getJDDNode()
	{
		return probs;
	}
	
	// get num non zeros
	
	public int getNNZ()
	{
		double nnz = JDD.GetNumMinterms(probs, numDDRowVars);
		return (nnz > Integer.MAX_VALUE) ? -1 : (int)Math.round(nnz);
	}
	
	public String getNNZString()
	{
		return "" + getNNZ();
	}
	
	// get value of first element in BDD filter
	
	public double firstFromBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		// filter filter
		JDD.Ref(filter);
		JDD.Ref(reach);
		tmp = JDD.And(filter, reach);
		
		// should never be called with empty filter, but trap and return -1
		if (tmp.equals(JDD.ZERO)) return -1;
		
		// remove all but first element of filter
		tmp = JDD.RestrictToFirst(tmp, vars);
		
		// then apply filter to probs
		JDD.Ref(probs);
		tmp = JDD.Apply(JDD.TIMES, probs, tmp);
		
		// extract single value (we use SumAbstract could use e.g. MaxAbstract, etc.)
		tmp = JDD.SumAbstract(tmp, vars);
		d = tmp.getValue();
		JDD.Deref(tmp);
		
		return d;
	}
	
	// get min value over BDD filter
	
	public double minOverBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		// filter filter
		JDD.Ref(filter);
		JDD.Ref(reach);
		tmp = JDD.And(filter, reach);
		
		// should never be called with empty filter, but trap and return infinity
		if (tmp.equals(JDD.ZERO)) return 1.0/0.0;
		
		// set non-reach states to infinity
		JDD.Ref(probs);
		tmp = JDD.ITE(tmp, probs, JDD.PlusInfinity());
		
		d = JDD.FindMin(tmp);
		JDD.Deref(tmp);
		
		return d;
	}
	
	// get max value over BDD filter
	
	public double maxOverBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		// filter filter
		JDD.Ref(filter);
		JDD.Ref(reach);
		tmp = JDD.And(filter, reach);
		
		// should never be called with empty filter, but trap and return -infinity
		if (tmp.equals(JDD.ZERO)) return -1.0/0.0;
		
		// set non-reach states to infinity
		JDD.Ref(probs);
		tmp = JDD.ITE(tmp, probs, JDD.MinusInfinity());
		
		d = JDD.FindMax(tmp);
		JDD.Deref(tmp);
		
		return d;
	}
	
	// sum elements of vector according to a bdd (used for csl steady state operator)
	
	public double sumOverBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		JDD.Ref(probs);
		JDD.Ref(filter);
		tmp = JDD.Apply(JDD.TIMES, probs, filter);
		tmp = JDD.SumAbstract(tmp, vars);
		d = tmp.getValue();
		JDD.Deref(tmp);
		
		return d;
	}
	
	// do a weighted sum of the elements of a double array and the values the mtbdd passed in
	// (used for csl reward steady state operator)
	
	public double sumOverMTBDD(JDDNode mult)
	{
		JDDNode tmp;
		double d;
		
		JDD.Ref(probs);
		JDD.Ref(mult);
		tmp = JDD.Apply(JDD.TIMES, probs, mult);
		tmp = JDD.SumAbstract(tmp, vars);
		d = tmp.getValue();
		JDD.Deref(tmp);
		
		return d;
	}
	
	// generate bdd (from an interval: relative operator and bound)
	
	public JDDNode getBDDFromInterval(String relOp, double bound)
	{
		JDDNode sol = null;
		
		JDD.Ref(probs);
		if (relOp.equals(">=")) {
			sol = JDD.GreaterThanEquals(probs, bound);
		}
		else if (relOp.equals(">")) {
			sol = JDD.GreaterThan(probs, bound);
		}
		else if (relOp.equals("<=")) {
			sol = JDD.LessThanEquals(probs, bound);
		}
		else if (relOp.equals("<")) {
			sol = JDD.LessThan(probs, bound);
		}
		
		return sol;
	}
	
	// generate bdd from an interval (lower/upper bound)
	
	public JDDNode getBDDFromInterval(double lo, double hi)
	{
		JDDNode sol;
		
		JDD.Ref(probs);
		sol = JDD.Interval(probs, lo, hi);
		
		return sol;
	}

	// PRINTING STUFF
	
	// print vector (non zeros only)
	
	public void print(PrismLog log)
	{
		int i;
		
		// check if all zero
		if (probs.equals(JDD.ZERO)) {
			log.println("(all zero)");
			return;
		}
		
		// set up and call recursive print
		outputLog = log;
		for (i = 0; i < varList.getNumVars(); i++) {
			varValues[i] = 0;
		}
		currentVar = 0;
		currentVarLevel = 0;
		printRec(probs, 0, odd, 0);
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

		// zero constant - bottom out of recursion
		if (dd.equals(JDD.ZERO)) return;

		// base case - at bottom (nonzero terminal)
		if (level == numVars) {
		
			outputLog.print(n + ":(");
			j = varList.getNumVars();
			for (i = 0; i < j; i++) {
				// integer variable
				if (varList.getType(i) == Expression.INT) {
					outputLog.print(varValues[i]+varList.getLow(i));
				}
				// boolean variable
				else {
					outputLog.print(varValues[i] == 1);
				}
				if (i < j-1) outputLog.print(",");
			}
			outputLog.print(")=" + dd.getValue() + " ");
			outputLog.println();
			return;
		}
		
		// select 'else' and 'then' branches
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

	// print filtered vector (non zeros only)
	
	public void printFiltered(PrismLog log, JDDNode filter)
	{
		int i;
		JDDNode tmp;
		
		// filter out
		JDD.Ref(probs);
		JDD.Ref(filter);
		tmp = JDD.Apply(JDD.TIMES, probs, filter);
		
		// check if all zero
		if (tmp.equals(JDD.ZERO)) {
			JDD.Deref(tmp);
			log.println("(all zero)");
			return;
		}
		
		// set up and call recursive print
		outputLog = log;
		for (i = 0; i < varList.getNumVars(); i++) {
			varValues[i] = 0;
		}
		currentVar = 0;
		currentVarLevel = 0;
		printRec(tmp, 0, odd, 0);
		//log.println();
		JDD.Deref(tmp);
	}
}
