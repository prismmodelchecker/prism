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

/**
 * Class for state-indexed vectors of (integer or double) values, represented by an MTBDD
 */
public class StateValuesMTBDD implements StateValues
{
	/** MTBDD storing vector of values */
	JDDNode values;

	/** Accuracy info */
	Accuracy accuracy;
	
	// info from model
	/** The underlying model */
	Model model;
	/** The BDD row variables of the underlying model */
	JDDVars vars;
	JDDNode reach;
	/** The number of BDD row variables in the underlying model */
	int numDDRowVars;
	/** The number of BDD row variables in the underlying model */
	int numVars;
	/** The ODD for the reachable states of the underlying model */
	ODDNode odd;
	/** The VarList of the underlying model*/
	VarList varList;

	// CONSTRUCTOR
	
	/**
	 * Constructor from a JDDNode (which is stored, not copied).
	 * Also set the accuracy info.
	 * <br>[ STORES: values, derefed on later call to clear() ]
	 * @param values the JddNode for the values
	 * @param model the underlying model
	 */
	public StateValuesMTBDD(JDDNode values, Model model)
	{
		this (values, model, null);
	}

	/**
	 * Constructor from a JDDNode (which is stored, not copied).
	 * Also set the accuracy info.
	 * <br>[ STORES: values, derefed on later call to clear() ]
	 * @param values the JddNode for the values
	 * @param model the underlying model
	 * @param accuracy result accuracy info
	 */
	public StateValuesMTBDD(JDDNode values, Model model, Accuracy accuracy)
	{
		// store values vector mtbdd and accuracy
		this.values = values;
		setAccuracy(accuracy);
		// get info from model
		setModel(model);
	}

	/** Helper method: Store information about the underlying model */
	private void setModel(Model model)
	{
		this.model = model;
		vars = model.getAllDDRowVars();
		reach = model.getReach();
		numDDRowVars = model.getNumDDRowVars();
		numVars = vars.n();
		odd = model.getODD();
		varList = model.getVarList();
	}

	@Override
	public void switchModel(Model newModel)
	{
		setModel(newModel);
	}

	@Override
	public void setAccuracy(Accuracy accuracy)
	{
		this.accuracy = accuracy;
	}
	
	// CONVERSION METHODS
	
	@Override
	public StateValuesDV convertToStateValuesDV() throws PrismException
	{
		// convert to StateValuesDV, destroy (clear) old vector
		StateValuesDV res = new StateValuesDV(values, model, accuracy);
		clear();
		return res;
	}

	@Override
	public StateValuesMTBDD convertToStateValuesMTBDD()
	{
		// convert to StateValuesMTBDD (nothing to do)
		return this;
	}

	// METHODS TO MODIFY VECTOR

	/**
	 * Set element i of this vector to value d. 
	 */
	public void setElement(int i, double d) throws PrismNotSupportedException
	{
		ODDNode ptr;
		JDDNode dd;
		int j, k;

		ODDUtils.checkInt(odd, "Cannot set element via index for model");

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

	@Override
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

	@Override
	public void roundOff(int places)
	{
		values = JDD.RoundOff(values, places);
	}

	@Override
	public void subtractFromOne() 
	{
		JDD.Ref(reach);
		values = JDD.Apply(JDD.MINUS, reach, values);
	}

	@Override
	public void add(StateValues sp) 
	{
		StateValuesMTBDD spm = (StateValuesMTBDD) sp;
		JDD.Ref(spm.values);
		values = JDD.Apply(JDD.PLUS, values, spm.values);
	}

	@Override
	public void timesConstant(double d) 
	{
		values = JDD.Apply(JDD.TIMES, values, JDD.Constant(d));
	}

	@Override
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

	@Override
	public void filter(JDDNode filter)
	{
		JDD.Ref(filter);
		values = JDD.Apply(JDD.TIMES, values, filter);
	}

	@Override
	public void filter(JDDNode filter, double d)
	{
		// If filter, then keep value, else constant d,
		// but only for the reachable states
		values = JDD.Times(reach.copy(), JDD.ITE(filter.copy(), values, JDD.Constant(d)));
	}

	@Override
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
	public Object getValue(int i) throws PrismNotSupportedException
	{
		JDDNode dd = values;

		ODDUtils.checkInt(odd, "Cannot get value for state index in model");

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

	/**
	 * Get the underlying JDDNode of this StateValuesMTBDD.
	 * <br>
	 * Note: The returned JDDNode is NOT a copy, i.e., the caller
	 * is responsible that the node does not get derefed.
	 * <br>[ REFS: <i>none</i> ]
	 */
	public JDDNode getJDDNode()
	{
		return values;
	}

	@Override
	public int getNNZ()
	{
		double nnz = JDD.GetNumMinterms(values, numDDRowVars);
		return (nnz > Integer.MAX_VALUE) ? -1 : (int)Math.round(nnz);
	}

	@Override
	public String getNNZString()
	{
		return "" + getNNZ();
	}

	// Filter operations

	@Override
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

	@Override
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

	@Override
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

	@Override
	public double maxFiniteOverBDD(JDDNode filter)
	{
		JDDNode tmp;
		double d;

		// filter filter
		JDD.Ref(filter);
		JDD.Ref(reach);
		tmp = JDD.And(filter, reach);

		// max of an empty set is -infinity
		if (tmp.equals(JDD.ZERO)) {
			JDD.Deref(tmp);
			return Double.NEGATIVE_INFINITY;
		}

		// set non-reach states to infinity
		JDD.Ref(values);
		tmp = JDD.ITE(tmp, values, JDD.MinusInfinity());

		d = JDD.FindMaxFinite(tmp);
		JDD.Deref(tmp);

		return d;
	}

	@Override
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

	@Override
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

	@Override
	public StateValues sumOverDDVars(JDDVars sumVars, Model newModel)
	{
		JDDNode tmp;
		
		JDD.Ref(values);
		tmp = JDD.SumAbstract(values, sumVars);
		
		return new StateValuesMTBDD(tmp, newModel);
	}

	@Override
	public JDDNode getBDDFromInterval(String relOpString, double bound)
	{
		return getBDDFromInterval(RelOp.parseSymbol(relOpString), bound);
	}

	@Override
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
			break;
		default:
			// Don't handle
		}
		
		return sol;
	}

	@Override
	public JDDNode getBDDFromInterval(double lo, double hi)
	{
		JDDNode sol;
		
		JDD.Ref(values);
		sol = JDD.Interval(values, lo, hi);
		
		return sol;
	}

	@Override
	public JDDNode getBDDFromCloseValue(double value, double epsilon, boolean abs)
	{
		if (abs)
			return getBDDFromCloseValueAbs(value, epsilon);
		else
			return getBDDFromCloseValueRel(value, epsilon);
	}

	@Override
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

	@Override
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

	@Override
	public Accuracy getAccuracy()
	{
		return accuracy;
	}
	
	// PRINTING STUFF

	/**
	 * Print the state values for a JDDNode, representing an MTBDD over the row vars of a model.
	 * <br>[ REFS: <i>none</i>, DEREFS: value ]
	 * @param log the output log
	 * @param values the MTBDD node
	 * @param model the Model (for the variable information)
	 * @param description an optional description for printing (may be {@code null})
	 */
	public static void print(PrismLog log, JDDNode values, Model model, String description) throws PrismException
	{
		StateValuesMTBDD sv = null;
		try {
			sv = new StateValuesMTBDD(values, model);
			if (description != null) log.println(description);
			sv.print(log);
		} finally {
			if (sv != null)
				sv.clear();
		}
	}

	@Override
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		if (odd == null) {
			if (printMatlab && printSparse) {
				throw new PrismNotSupportedException("Cannot print in sparse Matlab format, as there is no ODD");
			}
			// we have no index information -> can't print state index
			printIndices = false;
		}

		// header for matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + getSize() + ",1);");

		// check if all zero
		if (printSparse && !printMatlab && values.equals(JDD.ZERO)) {
			log.println("(all zero)");
			return;
		}

		// set up and call recursive print
		StateIterator it = new StateIterator(new StateAndValuePrinter(log, varList, printSparse, printMatlab, printStates, printIndices));
		if (printSparse) {
			it.iterateSparse(values);
		} else {
			it.iterateFiltered(model.getReach(), values);
		}

		// footer for matlab format
		if (printMatlab && !printSparse)
			log.println("];");
	}

	private class StateIterator {
		private int[] varSizes;
		private int[] varValues;
		private int currentVar;
		private int currentVarLevel;

		private StateAndValueConsumer consumer;

		public StateIterator(StateAndValueConsumer consumer)
		{
			this.consumer = consumer;

			varValues = new int[varList.getNumVars()];
			for (int i = 0; i < varList.getNumVars(); i++) {
				varValues[i] =  varList.getLow(i);
			}

			varSizes = new int[varList.getNumVars()];
			for (int i = 0; i < varList.getNumVars(); i++) {
				varSizes[i] = varList.getRangeLogTwo(i);
			}

			currentVar = 0;
			currentVarLevel = 0;
		}

		public void iterateSparse(JDDNode dd)
		{
			// if we don't have an ODD, we set the index to -1
			long stateIndex = (odd == null ? -1 : 0);
			iterateSparseRec(dd, 0, odd, stateIndex);
		}

		/**
		 * Iterate over all state/value pairs in ddValue that
		 * are included in the 0/1-DD ddFilter.
		 *
		 * @param ddFilter 0/1-DD for the states that should be considered
		 * @param ddValue the values for the states (has to be zero for all states not in ddFilter)
		 */
		public void iterateFiltered(JDDNode ddFilter, JDDNode ddValue)
		{
			// if we don't have an ODD, we set the index to -1
			long stateIndex = (odd == null ? -1 : 0);
			iterateRec(ddFilter, ddValue, 0, odd, stateIndex);
		}

		/**
		 * Recursive part of iteration (only non-zero values).
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
		private void iterateSparseRec(JDDNode dd, int level, ODDNode o, long stateIndex)
		{
			JDDNode e, t;

			// zero constant - bottom out of recursion
			if (dd.equals(JDD.ZERO)) return;

			// base case - at bottom (nonzero terminal)
			if (level == numVars) {
				consumer.accept(varValues, dd.getValue(), stateIndex);
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

			ODDNode oe = (o != null ? o.getElse() : null);
			ODDNode ot = (o != null ? o.getThen() : null);
			long eoff = (o != null ? o.getEOff() : 0);

			// then recurse...
			currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
			iterateSparseRec(e, level+1, oe, stateIndex);
			currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
			varValues[currentVar] += (1 << (varSizes[currentVar]-1-currentVarLevel));
			currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
			iterateSparseRec(t, level+1, ot, stateIndex + eoff);
			currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
			varValues[currentVar] -= (1 << (varSizes[currentVar]-1-currentVarLevel));
		}

		/**
		 * Recursive part of iteration (all elements,
		 * including where the value is zero).
		 *
		 * @param ddFilter 0/1-DD for the states that should be considered
		 * @param ddValue the values for the states (has to be zero for all states not in ddFilter)
		 * @param level the current variable level
		 * @param o the current node in the ODD
		 * @param n the current state index
		 */
		private void iterateRec(JDDNode ddFilter, JDDNode ddValue, int level, ODDNode o, long stateIndex)
		{
			/*
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

			// ddFilter is zero - bottom out of recursion
			if (ddFilter.equals(JDD.ZERO)) return;

			// base case - at bottom (nonzero terminal)
			if (level == numVars) {
				consumer.accept(varValues, ddValue.getValue(), stateIndex);
				return;
			}

			JDDNode eFilter, tFilter, eValue, tValue;

			// select 'else' and 'then' branches ( filter dd )
			if (ddFilter.getIndex() > vars.getVarIndex(level)) {
				eFilter = tFilter = ddFilter;
			} else {
				eFilter = ddFilter.getElse();
				tFilter = ddFilter.getThen();
			}

			// select 'else' and 'then' branches ( value dd )
			if (ddValue.getIndex() > vars.getVarIndex(level)) {
				eValue = tValue = ddValue;
			} else {
				eValue = ddValue.getElse();
				tValue = ddValue.getThen();
			}

			ODDNode oe = (o != null ? o.getElse() : null);
			ODDNode ot = (o != null ? o.getThen() : null);
			long eoff = (o != null ? o.getEOff() : 0);

			// then recurse...
			currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
			iterateRec(eFilter, eValue, level+1, oe, stateIndex);
			currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
			varValues[currentVar] += (1 << (varSizes[currentVar]-1-currentVarLevel));
			currentVarLevel++; if (currentVarLevel == varSizes[currentVar]) { currentVar++; currentVarLevel=0; }
			iterateRec(tFilter, tValue, level+1, ot, stateIndex + eoff);
			currentVarLevel--; if (currentVarLevel == -1) { currentVar--; currentVarLevel=varSizes[currentVar]-1; }
			varValues[currentVar] -= (1 << (varSizes[currentVar]-1-currentVarLevel));
		}
	}

	@Override
	public void iterate(StateAndValueConsumer consumer, boolean sparse)
	{
		StateIterator it = new StateIterator(consumer);
		if (sparse) {
			it.iterateSparse(values);
		} else {
			it.iterateFiltered(reach, values);
		}
	}

	@Override
	public void iterateFiltered(JDDNode filter, StateAndValueConsumer consumer, boolean sparse)
	{
		// filter out
		JDDNode tmp = JDD.Apply(JDD.TIMES, values.copy(), filter.copy());

		StateIterator it = new StateIterator(consumer);

		if (sparse) {
			it.iterateSparse(tmp);
		} else {
			it.iterateFiltered(filter, tmp);
		}

		JDD.Deref(tmp);
	}

	@Override
	public void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		JDDNode tmp;

		if (odd == null) {
			if (printMatlab && printSparse) {
				throw new PrismNotSupportedException("Cannot print in sparse Matlab format, as there is no ODD");
			}
			// we have no index information -> can't print state index
			printIndices = false;
		}

		// filter out
		JDD.Ref(values);
		JDD.Ref(filter);
		tmp = JDD.Apply(JDD.TIMES, values, filter);

		// header for matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + getSize() + ",1);");

		// check if all zero
		if (printSparse && !printMatlab && tmp.equals(JDD.ZERO)) {
			JDD.Deref(tmp);
			log.println("(all zero)");
			return;
		}

		// set up and call recursive print
		StateIterator it = new StateIterator(new StateAndValuePrinter(log, varList, printSparse, printMatlab, printStates, printIndices));
		if (printSparse) {
			it.iterateSparse(tmp);
		} else {
			it.iterateFiltered(filter, tmp);
		}

		// footer for matlab format
		if (printMatlab && !printSparse)
			log.println("];");

		//log.println();
		JDD.Deref(tmp);
	}

	/**
	 * Make a (deep) copy of this vector
	 */
	public StateValuesMTBDD deepCopy() throws PrismException
	{
		JDD.Ref(values);
		return new StateValuesMTBDD(values, model, accuracy);
	}
}
