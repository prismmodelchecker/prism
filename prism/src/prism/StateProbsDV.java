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

import dv.*;
import jdd.*;
import odd.*;
import parser.VarList;
import parser.Expression;

// state probability vector (double vector)

public class StateProbsDV implements StateProbs
{
	// prob vector
	DoubleVector probs;
	
	// info from model
	JDDVars vars;
	int numVars;
	ODDNode odd;
	VarList varList;
	
	// stuff to keep track of variable values in print method
	int[] varSizes;
	int[] varValues;
	int currentVar;
	int currentVarLevel;
	int counter;
	
	// log for output from print method
	PrismLog outputLog;

	// CONSTRUCTORS
	
	public StateProbsDV(DoubleVector p, Model model)
	{
		int i;
		
		// store prob vector
		probs = p;
		
		// get info from model
		vars = model.getAllDDRowVars();
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
	
	public StateProbsDV(JDDNode dd, Model model)
	{
		// construct double vector from an mtbdd
		this(new DoubleVector(dd, model.getAllDDRowVars(), model.getODD()), model);
	}

	// METHODS TO MODIFY VECTOR
	
	// round
	
	public void roundOff(int places)
	{
		probs.roundOff(places);
	}
	
	// subtract all probabilities from 1
	
	public void subtractFromOne() 
	{
		probs.subtractFromOne();
	}
	
	// add another vector to this one
	
	public void add(StateProbs sp) 
	{
		probs.add(((StateProbsDV)sp).probs);
	}
	
	// multiply vector by a constant
	
	public void timesConstant(double d) 
	{
		probs.timesConstant(d);
	}
	
	// filter vector using a bdd (set elements not in filter to 0)
	
	public void filter(JDDNode filter)
	{
		probs.filter(filter, vars, odd);
	}
	
	// clear (free memory)
	
	public void clear()
	{
		probs.clear();
	}

	// METHODS TO ACCESS VECTOR DATA
	
	// get vector
	
	public DoubleVector getDoubleVector()
	{
		return probs;
	}
	
	// get num non zeros
	
	public int getNNZ()
	{
		return probs.getNNZ();
	}
	
	public String getNNZString()
	{
		return "" + getNNZ();
	}
	
	// get value of first element in BDD filter
	
	public double firstFromBDD(JDDNode filter)
	{
		return probs.firstFromBDD(filter, vars, odd);
	}
	
	// get min value over BDD filter
	
	public double minOverBDD(JDDNode filter)
	{
		return probs.minOverBDD(filter, vars, odd);
	}
	
	// get max value over BDD filter
	
	public double maxOverBDD(JDDNode filter)
	{
		return probs.maxOverBDD(filter, vars, odd);
	}
	
	// sum elements of vector according to a bdd (used for csl steady state operator)
	
	public double sumOverBDD(JDDNode filter)
	{
		return probs.sumOverBDD(filter, vars, odd);
	}
	
	// do a weighted sum of the elements of a double array and the values the mtbdd passed in
	// (used for csl reward steady state operator)
	
	public double sumOverMTBDD(JDDNode mult)
	{
		return probs.sumOverMTBDD(mult, vars, odd);
	}
	
	// generate bdd from an interval (relative operator and bound)
	
	public JDDNode getBDDFromInterval(String relOp, double bound)
	{
		return probs.getBDDFromInterval(relOp, bound, vars, odd);
	}
	
	// generate bdd from an interval (lower/upper bound)
	
	public JDDNode getBDDFromInterval(double lo, double hi)
	{
		return probs.getBDDFromInterval(lo, hi, vars, odd);
	}

	// PRINTING STUFF
	
	// print vector (non zeros only)
	
	public void print(PrismLog log)
	{
		int i;
		
		// check if all zero
		if (probs.getNNZ() == 0) {
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
		printRec(0, odd, 0);
	}
	
	// recursive bit of print
	
	// (nb: this would be very easy - i.e. not even
	//  any recursion - if we didn't want to print
	//  out the values of the module variables as well
	//  which requires traversal of the odd as well
	//  as the vector)
	// (nb2: traversal of vector/odd is still quite simple,
	//  tricky bit is keeping track of variable values
	//  throughout traversal - we want to be efficient
	//  and not compute the values from scratch each
	//  time, but we also want to avoid passing arrays
	//  into the resursive method)
	
	private void printRec(int level, ODDNode o, int n)
	{
		int i, j;
		double d;
		
		// base case - at bottom
		if (level == numVars) {
			d = probs.getElement(n);
			if (d != 0) {
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
				outputLog.print(")=" + d + " ");
				outputLog.println();
				return;
			}
		}
		// recurse
		else {
			if (o.getEOff() > 0) {
				currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
				printRec(level+1, o.getElse(), n);
				currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
			}
			if (o.getTOff() > 0) {
				varValues[currentVar] += (1 << (varSizes[currentVar]-1-currentVarLevel));
				currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
				printRec(level+1, o.getThen(), (int)(n+o.getEOff()));
				currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
				varValues[currentVar] -= (1 << (varSizes[currentVar]-1-currentVarLevel));
			}
		}
	}

	// print filtered vector (non zeros only)
	
	public void printFiltered(PrismLog log, JDDNode filter)
	{
		int i;
		
		// check if all zero
		if (probs.getNNZ() == 0) {
			log.println("(all zero)");
			return;
		}
		// still might be though, depending on filter
		// so we kep an explicit counter
		counter = 0;
		
		// set up and call recursive print
		outputLog = log;
		for (i = 0; i < varList.getNumVars(); i++) {
			varValues[i] = 0;
		}
		currentVar = 0;
		currentVarLevel = 0;
		printFilteredRec(0, odd, 0, filter);
		
		// check again if all zero
		if (counter == 0) {
			log.println("(all zero)");
			return;
		}
	}
	
	// recursive bit of printFiltered
	
	// same as recursive bit of print (above)
	// but we also traverse filter
	
	private void printFilteredRec(int level, ODDNode o, int n, JDDNode filter)
	{
		int i;
		double d;
		JDDNode newFilter;
		
		// don't print if the filter is zero
		if (filter.equals(JDD.ZERO)) {
			return;
		}
		
		// base case - at bottom
		if (level == numVars) {
			d = probs.getElement(n);
			if (d != 0) {
				outputLog.print(n + ":(");
				for (i = 0; i < varList.getNumVars()-1; i++) {
					outputLog.print((varValues[i]+varList.getLow(i)) + ",");
				}
				i = varList.getNumVars()-1;
				outputLog.print((varValues[i]+varList.getLow(i)));
				outputLog.print(")=" + d + " ");
				outputLog.println();
				counter++;
				return;
			}
		}
		// recurse
		else {
			if (o.getEOff() > 0) {
				currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
				JDD.Ref(filter); JDD.Ref(vars.getVar(level));
				newFilter = JDD.Apply(JDD.TIMES, filter, JDD.Not(vars.getVar(level)));
				printFilteredRec(level+1, o.getElse(), n, newFilter);
				JDD.Deref(newFilter);
				currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
			}
			if (o.getTOff() > 0) {
				varValues[currentVar] += (1 << (varSizes[currentVar]-1-currentVarLevel));
				currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
				JDD.Ref(filter); JDD.Ref(vars.getVar(level));
				newFilter = JDD.Apply(JDD.TIMES, filter, vars.getVar(level));
				printFilteredRec(level+1, o.getThen(), (int)(n+o.getEOff()), newFilter);
				JDD.Deref(newFilter);
				currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
				varValues[currentVar] -= (1 << (varSizes[currentVar]-1-currentVarLevel));
			}
		}
	}
}
