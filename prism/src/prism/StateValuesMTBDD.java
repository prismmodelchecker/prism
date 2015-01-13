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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import jdd.*;
import odd.*;
import parser.VarList;
import parser.ast.RelOp;
import parser.type.*;

// Class for state-indexed vectors of (integer or double) values, represented by an MTBDD

public class StateValuesMTBDD implements StateValues
{
	// MTBDD storing vector of values
	JDDNode values;
	
	// info from model
	Model model;
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
	
	public StateValuesMTBDD(JDDNode p, Model m)
	{
		int i;
		
		// store values vector mtbdd
		values = p;
		
		// get info from model
		model = m;
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

	// CONVERSION METHODS
	
	// convert to StateValuesDV, destroy (clear) old vector
	public StateValuesDV convertToStateValuesDV()
	{
		StateValuesDV res = new StateValuesDV(values, model);
		clear();
		return res;
	}
	
	// convert to StateValuesMTBDD (nothing to do)
	public StateValuesMTBDD convertToStateValuesMTBDD()
	{
		return this;
	}
	
	// METHODS TO MODIFY VECTOR
	
	/**
	 * Set element i of this vector to value d. 
	 */
	public void setElement(int i, double d)
	{
		ODDNode ptr;
		JDDNode dd;
		int j, k;
		
		// Use ODD to build BDD for state index i
		dd = JDD.Constant(1);
		ptr = odd;
		j = i;
		for (k = 0; k < numVars; k++) {
			JDD.Ref(vars.getVar(k));
			if (j >= ptr.getEOff()) {
				j -= ptr.getEOff();
				dd = JDD.And(dd, vars.getVar(k));
				ptr = ptr.getThen();
			} else {
				dd = JDD.And(dd, JDD.Not(vars.getVar(k)));
				ptr = ptr.getElse();
			}
		}
		
		// Add element to vector MTBDD
		values = JDD.ITE(dd, JDD.Constant(d), values);
	}
	
	/**
	 * Set the elements of this vector by reading them in from a file.
	 */
	public void readFromFile(File file) throws PrismException
	{
		BufferedReader in;
		String s;
		int lineNum = 0, count = 0;
		double d;
		boolean hasIndices = false;
		long size = model.getNumStates();
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(file));
			// read remaining lines
			s = in.readLine(); lineNum++;
			while (s != null) {
				s = s.trim();
				if (!("".equals(s))) {
					// If entry is of form "i=x", use i as index not count
					// (otherwise, assume line i contains value for state index i)
					if (s.contains("=")) {
						hasIndices = true;
						String ss[] = s.split("=");
						count = Integer.parseInt(ss[0]);
						s = ss[1];
					}
					if (count + 1 > size) {
						in.close();
						throw new PrismException("Too many values in file \"" + file + "\" (more than " + size + ")");
					}
					d = Double.parseDouble(s);
					setElement(count, d);
					count++;
				}
				s = in.readLine(); lineNum++;
			}
			// Close file
			in.close();
			// Check size
			if (!hasIndices && count < size)
				throw new PrismException("Too few values in file \"" + file + "\" (" + count + ", not " + size + ")");
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of file \"" + file + "\"");
		}
	}
	
	// round
	
	public void roundOff(int places)
	{
		values = JDD.RoundOff(values, places);
	}
	
	// subtract all values from 1
	
	public void subtractFromOne() 
	{
		JDD.Ref(reach);
		values = JDD.Apply(JDD.MINUS, reach, values);
	}
	
	// add another vector to this one
	
	public void add(StateValues sp) 
	{
		StateValuesMTBDD spm = (StateValuesMTBDD) sp;
		JDD.Ref(spm.values);
		values = JDD.Apply(JDD.PLUS, values, spm.values);
	}
	
	// multiply vector by a constant
	
	public void timesConstant(double d) 
	{
		values = JDD.Apply(JDD.TIMES, values, JDD.Constant(d));
	}
	
	// compute dot (inner) product of this and another vector
	
	public double dotProduct(StateValues sp) 
	{
		StateValuesMTBDD spm = (StateValuesMTBDD) sp;
		JDD.Ref(values);
		JDD.Ref(spm.values);
		JDDNode tmp = JDD.Apply(JDD.TIMES, values, spm.values);
		tmp = JDD.SumAbstract(tmp, vars);
		double d = JDD.FindMax(tmp);
		JDD.Deref(tmp);
		return d;
	}
	
	// filter vector using a bdd (set elements not in filter to 0)
	
	public void filter(JDDNode filter)
	{
		JDD.Ref(filter);
		values = JDD.Apply(JDD.TIMES, values, filter);
	}
	
	// apply max operator, i.e. vec[i] = max(vec[i], vec2[i]), where vec2 is an mtbdd
	
	public void maxMTBDD(JDDNode vec2)
	{
		JDD.Ref(vec2);
		values = JDD.Apply(JDD.MAX, values, vec2);
	}
	
	@Override
	public void clear()
	{
		JDD.Deref(values);
	}

	// METHODS TO ACCESS VECTOR DATA
	
	@Override
	public int getSize()
	{
		return (int) model.getNumStates();
	}
	
	@Override
	public Object getValue(int i)
	{
		JDDNode dd = values;
		ODDNode ptr = odd;
		int o = 0;
		for (int k = 0; k < numVars; k++) {
			if (ptr.getEOff() > i - o) {
				dd = dd.getIndex() > vars.getVarIndex(k) ? dd : dd.getElse();
				ptr = ptr.getElse();
			} else {
				dd = dd.getIndex() > vars.getVarIndex(k) ? dd : dd.getThen();
				o += ptr.getEOff();
				ptr = ptr.getThen();
			}
		}
		// TODO: cast to Integer or Double as required?
		return dd.getValue();
	}
	
	// get mtbdd
	
	public JDDNode getJDDNode()
	{
		return values;
	}
	
	// get num non zeros
	
	public int getNNZ()
	{
		double nnz = JDD.GetNumMinterms(values, numDDRowVars);
		return (nnz > Integer.MAX_VALUE) ? -1 : (int)Math.round(nnz);
	}
	
	public String getNNZString()
	{
		return "" + getNNZ();
	}
	
	// Filter operations
	
	/**
	 * Get the value of first vector element that is in the (BDD) filter.
	 */
	public double firstFromBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		// filter filter
		JDD.Ref(filter);
		JDD.Ref(reach);
		tmp = JDD.And(filter, reach);
		
		// This shouldn't really be called with an empty filter.
		// But we check for this anyway and return NaN.
		// This is unfortunately indistinguishable from the case
		// where the vector does actually contain NaN. Ho hum.
		if (tmp.equals(JDD.ZERO)) return Double.NaN;
				
		// remove all but first element of filter
		tmp = JDD.RestrictToFirst(tmp, vars);
		
		// then apply filter to values
		JDD.Ref(values);
		tmp = JDD.Apply(JDD.TIMES, values, tmp);
		
		// extract single value (we use SumAbstract could use e.g. MaxAbstract, etc.)
		tmp = JDD.SumAbstract(tmp, vars);
		d = tmp.getValue();
		JDD.Deref(tmp);
		
		return d;
	}
	
	/**
	 * Get the minimum value of those that are in the (BDD) filter.
	 */
	public double minOverBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		// filter filter
		JDD.Ref(filter);
		JDD.Ref(reach);
		tmp = JDD.And(filter, reach);
		
		// min of an empty set is +infinity
		if (tmp.equals(JDD.ZERO)) return Double.POSITIVE_INFINITY;
		
		// set non-reach states to infinity
		JDD.Ref(values);
		tmp = JDD.ITE(tmp, values, JDD.PlusInfinity());
		
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
		
		// max of an empty set is -infinity
		if (tmp.equals(JDD.ZERO)) return Double.NEGATIVE_INFINITY;
		
		// set non-reach states to infinity
		JDD.Ref(values);
		tmp = JDD.ITE(tmp, values, JDD.MinusInfinity());
		
		d = JDD.FindMax(tmp);
		JDD.Deref(tmp);
		
		return d;
	}
	
	/**
	 * Get the sum of those elements that are in the (BDD) filter.
	 */
	public double sumOverBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;
		
		JDD.Ref(values);
		JDD.Ref(filter);
		tmp = JDD.Apply(JDD.TIMES, values, filter);
		tmp = JDD.SumAbstract(tmp, vars);
		d = tmp.getValue();
		JDD.Deref(tmp);
		
		return d;
	}
	
	/**
	 * Do a weighted sum of the elements of the vector and the values the mtbdd passed in
	 * (used for CSL reward steady state operator).
	 */
	public double sumOverMTBDD(JDDNode mult)
	{
		JDDNode tmp;
		double d;
		
		JDD.Ref(values);
		JDD.Ref(mult);
		tmp = JDD.Apply(JDD.TIMES, values, mult);
		tmp = JDD.SumAbstract(tmp, vars);
		d = tmp.getValue();
		JDD.Deref(tmp);
		
		return d;
	}
	
	/**
	* Sum up the elements of the vector, over a subset of its DD vars
	* store the result in a new StateValues (for newModel)
	* @throws PrismException (on out-of-memory)
	*/
	public StateValues sumOverDDVars(JDDVars sumVars, Model newModel)
	{
		JDDNode tmp;
		
		JDD.Ref(values);
		tmp = JDD.SumAbstract(values, sumVars);
		
		return new StateValuesMTBDD(tmp, newModel);
	}
	
	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as relational operator and bound)
	 */
	public JDDNode getBDDFromInterval(String relOpString, double bound)
	{
		return getBDDFromInterval(RelOp.parseSymbol(relOpString), bound);
	}
	
	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as relational operator and bound)
	 */
	public JDDNode getBDDFromInterval(RelOp relOp, double bound)
	{
		JDDNode sol = null;
		
		JDD.Ref(values);
		switch (relOp) {
		case GEQ:
			sol = JDD.GreaterThanEquals(values, bound);
			break;
		case GT:
			sol = JDD.GreaterThan(values, bound);
			break;
		case LEQ:
			sol = JDD.LessThanEquals(values, bound);
			break;
		case LT:
			sol = JDD.LessThan(values, bound);
		default:
			// Don't handle
		}
		
		return sol;
	}
	
	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as lower/upper bound)
	 */
	public JDDNode getBDDFromInterval(double lo, double hi)
	{
		JDDNode sol;
		
		JDD.Ref(values);
		sol = JDD.Interval(values, lo, hi);
		
		return sol;
	}

	/**
	 * 	Generate BDD for states whose value is close to 'value'
	 * (within either absolute or relative error 'epsilon')
	 */
	public JDDNode getBDDFromCloseValue(double value, double epsilon, boolean abs)
	{
		if (abs)
			return getBDDFromCloseValueAbs(value, epsilon);
		else
			return getBDDFromCloseValueRel(value, epsilon);
	}
	
	/**
	 * Generate BDD for states whose value is close to 'value'
	 * (within absolute error 'epsilon')
	 */
	public JDDNode getBDDFromCloseValueAbs(double value, double epsilon)
	{
		JDDNode sol;
		
		// TODO: infinite cases
		// if (isinf(values[i])) return (isinf(value) && (values[i] > 0) == (value > 0)) ? 1 : 0
		// else (fabs(values[i] - value) < epsilon);
		
		JDD.Ref(values);
		sol = JDD.Interval(values, value - epsilon, value + epsilon);
		
		return sol;
	}
	
	/**
	 * 	Generate BDD for states whose value is close to 'value'
	 * (within relative error 'epsilon')
	 */
	public JDDNode getBDDFromCloseValueRel(double value, double epsilon)
	{
		JDDNode sol;
		
		// TODO: infinite cases
		// if (isinf(values[i])) return (isinf(value) && (values[i] > 0) == (value > 0)) ? 1 : 0
		// else (fabs(values[i] - value) < epsilon);

		// TODO: this should be relative, not absolute error
		// (see doubles_are_close_rel in dv.cc for reference)
		JDD.Ref(values);
		sol = JDD.Interval(values, value - epsilon, value + epsilon);
		
		return sol;
	}
	
	// PRINTING STUFF
	
	/**
	 * Print vector to a log/file (non-zero entries only)
	 */
	public void print(PrismLog log) throws PrismException
	{
		int i;
		
		// check if all zero
		if (values.equals(JDD.ZERO)) {
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
		printRec(values, 0, odd, 0);
		//log.println();
	}
	
	/**
	 * Print vector to a log/file.
	 * @param log The log
	 * @param printSparse Print non-zero elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 */
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		print(log, printSparse, printMatlab, printStates, true);
	}

	/**
	 * Print vector to a log/file.
	 * @param log The log
	 * @param printSparse Print non-zero elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 * @param printIndices Print state indices for each element? 
	 */
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		// Because non-sparse output from MTBDD requires a bit more effort...
		if (printSparse) print(log);
		else throw new PrismException("Not supported");
		// Note we also ignore printMatlab/printStates/printIndices due to laziness
	}
	
	/**
	 * Recursive part of print method.
	 * 
	 * (NB: this would be very easy - i.e. not even
	 *  any recursion - if we didn't want to print
	 *  out the values of the module variables as well
	 *  which requires traversal of the odd as well
	 *  as the vector)
	 * (NB2: traversal of vector/odd is still quite simple;
	 *  tricky bit is keeping track of variable values
	 *  throughout traversal - we want to be efficient
	 *  and not compute the values from scratch each
	 *  time, but we also want to avoid passing arrays
	 *  into the recursive method)
	 */
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
				if (varList.getType(i) instanceof TypeInt) {
					outputLog.print(varValues[i]+varList.getLow(i));
				}
				// boolean variable
				else {
					outputLog.print(varValues[i] == 1);
				}
				if (i < j-1) outputLog.print(",");
			}
			outputLog.print(")=" + dd.getValue());
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

	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * @param log The log
	 * @param filter A BDD specifying which states to print for.
	 */
	public void printFiltered(PrismLog log, JDDNode filter) throws PrismException
		{
		int i;
		JDDNode tmp;
		
		// filter out
		JDD.Ref(values);
		JDD.Ref(filter);
		tmp = JDD.Apply(JDD.TIMES, values, filter);
		
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
	
	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * @param log The log
	 * @param filter A BDD specifying which states to print for.
	 * @param printSparse Print non-zero elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element? 
	 */
	public void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		// Because non-sparse output from MTBDD requires a bit more effort... 
		if (printSparse) printFiltered(log, filter);
		else throw new PrismException("Not supported");
		// Note we also ignore printMatlab/printStates due to laziness
	}
	
	/**
	 * Make a (deep) copy of this vector
	 */
	public StateValuesMTBDD deepCopy() throws PrismException
	{
		JDD.Ref(values);
		return new StateValuesMTBDD(values, model);
	}
}
