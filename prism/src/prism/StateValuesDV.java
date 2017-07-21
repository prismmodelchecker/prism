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

import java.io.*;

import dv.*;
import jdd.*;
import odd.*;
import parser.VarList;
import parser.ast.RelOp;
import parser.type.*;

/**
 * Class for state-indexed vectors of (integer or double) values,
 * represented by a vector of doubles.
 */
public class StateValuesDV implements StateValues
{
	/** Double vector storing values */
	DoubleVector values;

	// info from model
	/** The underlying model */
	Model model;
	/** The BDD row variables of the underlying model */
	JDDVars vars;
	/** The number of BDD row variables in the underlying model */
	int numVars;
	/** The ODD for the reachable states of the underlying model */
	ODDNode odd;
	/** The VarList of the underlying model*/
	VarList varList;

	// stuff to keep track of variable values in print method
	int[] varSizes;
	int[] varValues;
	int currentVar;
	int currentVarLevel;
	int counter;

	/** Log for output from print method */
	PrismLog outputLog;

	/** Flag: printSparse (only non-zero values) */
	boolean printSparse = true;
	/** Flag: printMatlab */
	boolean printMatlab = false;
	/** Flag: printStates (variable values on the model) */
	boolean printStates = true;
	/** Flag: printIndizes (indizes for the states) */
	boolean printIndices = true;

	// CONSTRUCTORS

	/**
	 * Constructor from a double vector (which is stored, not copied).
	 * @param values the double vector
	 * @param model the underlying model
	 */
	public StateValuesDV(DoubleVector values, Model model)
	{
		// store values vector
		this.values = values;

		// get info from model
		setModel(model);
	}

	/**
	 * Constructor from an MTBDD.
	 * <br>
	 * Note: The JDDNode dd must only be non-zero for reachable states
	 * (otherwise bad things happen)
	 * <br>[ DEREFS: <i>none</i> ]
	 * @param dd the double vector
	 * @param model the underlying model
	 */
	public StateValuesDV(JDDNode dd, Model model) throws PrismException
	{
		// TODO: Enforce/check that dd is zero for all non-reachable states
		this(new DoubleVector(dd, model.getAllDDRowVars(), model.getODD()), model);
	}

	/** Helper method: Store information about the underlying model */
	private void setModel(Model model)
	{
		this.model = model;
		vars = model.getAllDDRowVars();
		numVars = vars.n();
		odd = model.getODD();
		varList = model.getVarList();

		// initialise arrays
		varSizes = new int[varList.getNumVars()];
		for (int i = 0; i < varList.getNumVars(); i++) {
			varSizes[i] = varList.getRangeLogTwo(i);
		}
		varValues = new int[varList.getNumVars()];
	}

	@Override
	public void switchModel(Model newModel)
	{
		setModel(newModel);
	}

	// CONVERSION METHODS

	@Override
	public StateValuesDV convertToStateValuesDV()
	{
		// convert to StateValuesDV (nothing to do)
		return this;
	}

	@Override
	public StateValuesMTBDD convertToStateValuesMTBDD()
	{
		// convert to StateValuesMTBDD, destroy (clear) old vector
		StateValuesMTBDD res = new StateValuesMTBDD(values.convertToMTBDD(vars, odd), model);
		clear();
		return res;
	}

	// METHODS TO MODIFY VECTOR

	/**
	 * Set element i of this vector to value d. 
	 */
	private void setElement(int i, double d)
	{
		values.setElement(i, d);
	}

	@Override
	public void readFromFile(File file) throws PrismException
	{
		BufferedReader in;
		String s;
		int lineNum = 0, count = 0;
		double d;
		boolean hasIndices = false;
		int size = values.getSize();

		try {
			// Open file for reading
			in = new BufferedReader(new FileReader(file));
			// Read remaining lines
			s = in.readLine();
			lineNum++;
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
				s = in.readLine();
				lineNum++;
			}
			// Close file
			in.close();
			// Check size
			if (!hasIndices && count < size)
				throw new PrismException("Too few values in file \"" + file + "\" (" + count + ", not " + size + ")");
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of file \"" + file + "\"");
		}
	}

	@Override
	public void roundOff(int places)
	{
		values.roundOff(places);
	}

	@Override
	public void subtractFromOne()
	{
		values.subtractFromOne();
	}

	@Override
	public void add(StateValues sp)
	{
		values.add(((StateValuesDV) sp).values);
	}

	@Override
	public void timesConstant(double d)
	{
		values.timesConstant(d);
	}

	@Override
	public double dotProduct(StateValues sv)
	{
		return values.dotProduct(((StateValuesDV) sv).values);
	}

	@Override
	public void filter(JDDNode filter)
	{
		values.filter(filter, vars, odd);
	}

	@Override
	public void filter(JDDNode filter, double d)
	{
		values.filter(filter, d, vars, odd);
	}

	@Override
	public void maxMTBDD(JDDNode vec2)
	{
		values.maxMTBDD(vec2, vars, odd);
	}

	@Override
	public void clear()
	{
		values.clear();
	}

	// METHODS TO ACCESS VECTOR DATA

	@Override
	public int getSize()
	{
		return values.getSize();
	}
	
	@Override
	public Object getValue(int i)
	{
		// TODO: cast to Integer or Double as required?
		return values.getElement(i);
	}

	/** Get the underlying double vector of this StateValuesDV */
	public DoubleVector getDoubleVector()
	{
		return values;
	}

	@Override
	public int getNNZ()
	{
		return values.getNNZ();
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
		return values.firstFromBDD(filter, vars, odd);
	}

	@Override
	public double minOverBDD(JDDNode filter)
	{
		return values.minOverBDD(filter, vars, odd);
	}

	@Override
	public double maxOverBDD(JDDNode filter)
	{
		return values.maxOverBDD(filter, vars, odd);
	}

	@Override
	public double maxFiniteOverBDD(JDDNode filter)
	{
		return values.maxFiniteOverBDD(filter, vars, odd);
	}

	@Override
	public double sumOverBDD(JDDNode filter)
	{
		return values.sumOverBDD(filter, vars, odd);
	}

	@Override
	public double sumOverMTBDD(JDDNode mult)
	{
		return values.sumOverMTBDD(mult, vars, odd);
	}

	@Override
	public StateValues sumOverDDVars(JDDVars sumVars, Model newModel) throws PrismException
	{
		DoubleVector tmp;

		tmp = values.sumOverDDVars(model.getAllDDRowVars(), odd, newModel.getODD(), sumVars.getMinVarIndex(), sumVars.getMaxVarIndex());

		return (StateValues) new StateValuesDV(tmp, newModel);
	}

	@Override
	public JDDNode getBDDFromInterval(String relOpString, double bound)
	{
		return getBDDFromInterval(RelOp.parseSymbol(relOpString), bound);
	}

	@Override
	public JDDNode getBDDFromInterval(RelOp relOp, double bound)
	{
		return values.getBDDFromInterval(relOp, bound, vars, odd);
	}

	/**
	 * 	Generate BDD for states in the given interval
	 * (interval specified as lower/upper bound)
	 */
	public JDDNode getBDDFromInterval(double lo, double hi)
	{
		return values.getBDDFromInterval(lo, hi, vars, odd);
	}

	@Override
	public JDDNode getBDDFromCloseValue(double value, double epsilon, boolean abs)
	{
		if (abs)
			return values.getBDDFromCloseValueAbs(value, epsilon, vars, odd);
		else
			return values.getBDDFromCloseValueRel(value, epsilon, vars, odd);
	}

	@Override
	public JDDNode getBDDFromCloseValueAbs(double value, double epsilon)
	{
		return values.getBDDFromCloseValueAbs(value, epsilon, vars, odd);
	}

	@Override
	public JDDNode getBDDFromCloseValueRel(double value, double epsilon)
	{
		return values.getBDDFromCloseValueRel(value, epsilon, vars, odd);
	}

	// PRINTING STUFF

	@Override
	public void print(PrismLog log) throws PrismException
	{
		print(log, true, false, true, true);
	}

	@Override
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		print(log, printSparse, printMatlab, printStates, true);
	}

	@Override
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		int i;

		// store flags
		this.printSparse = printSparse;
		this.printMatlab = printMatlab;
		this.printStates = printStates;
		this.printIndices = printIndices;

		// header for matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + values.getSize() + ",1);");

		// check if all zero
		if (printSparse && !printMatlab && values.getNNZ() == 0) {
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

		// footer for matlab format
		if (printMatlab && !printSparse)
			log.println("];");
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
	private void printRec(int level, ODDNode o, int n)
	{
		double d;

		// base case - at bottom
		if (level == numVars) {
			d = values.getElement(n);
			printLine(n, d);
			return;
		}
		// recurse
		else {
			if (o.getEOff() > 0) {
				currentVarLevel++;
				if (currentVarLevel == varSizes[currentVar]) {
					currentVar++;
					currentVarLevel = 0;
				}
				printRec(level + 1, o.getElse(), n);
				currentVarLevel--;
				if (currentVarLevel == -1) {
					currentVar--;
					currentVarLevel = varSizes[currentVar] - 1;
				}
			}
			if (o.getTOff() > 0) {
				varValues[currentVar] += (1 << (varSizes[currentVar] - 1 - currentVarLevel));
				currentVarLevel++;
				if (currentVarLevel == varSizes[currentVar]) {
					currentVar++;
					currentVarLevel = 0;
				}
				printRec(level + 1, o.getThen(), (int) (n + o.getEOff()));
				currentVarLevel--;
				if (currentVarLevel == -1) {
					currentVar--;
					currentVarLevel = varSizes[currentVar] - 1;
				}
				varValues[currentVar] -= (1 << (varSizes[currentVar] - 1 - currentVarLevel));
			}
		}
	}

	@Override
	public void printFiltered(PrismLog log, JDDNode filter) throws PrismException
	{
		printFiltered(log, filter, true, false, true, true);
	}

	@Override
	public void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		printFiltered(log, filter, printSparse, printMatlab, printStates, true);
	}

	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * @param log The log
	 * @param filter A BDD specifying which states to print for.
	 * @param printSparse Print non-zero elements only? 
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element?
	 * @param printIndizes Print indizes before states? 
	 */
	public void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
			throws PrismException
	{
		int i;

		// store flags
		this.printSparse = printSparse;
		this.printMatlab = printMatlab;
		this.printStates = printStates;
		this.printIndices = printIndices;

		// header for matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + values.getSize() + ",1);");

		// set up a counter so we can check if there were no non-zero elements
		counter = 0;

		// set up and call recursive print
		outputLog = log;
		for (i = 0; i < varList.getNumVars(); i++) {
			varValues[i] = 0;
		}
		currentVar = 0;
		currentVarLevel = 0;
		printFilteredRec(0, odd, 0, filter);

		// check  if all zero
		if (printSparse && !printMatlab && counter == 0) {
			log.println("(all zero)");
			return;
		}

		// footer for matlab format
		if (printMatlab && !printSparse)
			log.println("];");
	}

	/**
	 * Recursive part of printFiltered method.
	 *
	 * So we need to traverse filter too.
	 * See also notes above for printRec. 
	 */
	private void printFilteredRec(int level, ODDNode o, int n, JDDNode filter)
	{
		double d;
		JDDNode newFilter;

		// don't print if the filter is zero
		if (filter.equals(JDD.ZERO)) {
			return;
		}

		// base case - at bottom
		if (level == numVars) {
			d = values.getElement(n);
			printLine(n, d);
			return;
		}
		// recurse
		else {
			if (o.getEOff() > 0) {
				currentVarLevel++;
				if (currentVarLevel == varSizes[currentVar]) {
					currentVar++;
					currentVarLevel = 0;
				}
				JDD.Ref(filter);
				JDD.Ref(vars.getVar(level));
				newFilter = JDD.Apply(JDD.TIMES, filter, JDD.Not(vars.getVar(level)));
				printFilteredRec(level + 1, o.getElse(), n, newFilter);
				JDD.Deref(newFilter);
				currentVarLevel--;
				if (currentVarLevel == -1) {
					currentVar--;
					currentVarLevel = varSizes[currentVar] - 1;
				}
			}
			if (o.getTOff() > 0) {
				varValues[currentVar] += (1 << (varSizes[currentVar] - 1 - currentVarLevel));
				currentVarLevel++;
				if (currentVarLevel == varSizes[currentVar]) {
					currentVar++;
					currentVarLevel = 0;
				}
				JDD.Ref(filter);
				JDD.Ref(vars.getVar(level));
				newFilter = JDD.Apply(JDD.TIMES, filter, vars.getVar(level));
				printFilteredRec(level + 1, o.getThen(), (int) (n + o.getEOff()), newFilter);
				JDD.Deref(newFilter);
				currentVarLevel--;
				if (currentVarLevel == -1) {
					currentVar--;
					currentVarLevel = varSizes[currentVar] - 1;
				}
				varValues[currentVar] -= (1 << (varSizes[currentVar] - 1 - currentVarLevel));
			}
		}
	}

	private void printLine(int n, double d)
	{
		int i, j;
		// increment counter (used in printFiltered)
		if (d > 0)
			counter++;
		// do printing
		if (!printSparse || d != 0) {
			if (printMatlab) {
				if (printSparse) {
					outputLog.println("v(" + (n + 1) + ")=" + d + ";");
				} else {
					outputLog.println(d);
				}
			} else {
				if (printIndices) {
					outputLog.print(n);
				}
				if (printStates) {
					outputLog.print(":");
					outputLog.print("(");
					j = varList.getNumVars();
					for (i = 0; i < j; i++) {
						// integer variable
						if (varList.getType(i) instanceof TypeInt) {
							outputLog.print(varValues[i] + varList.getLow(i));
						}
						// boolean variable
						else {
							outputLog.print(varValues[i] == 1);
						}
						if (i < j - 1)
							outputLog.print(",");
					}
					outputLog.print(")");
				}
				if (printIndices || printStates)
					outputLog.print("=");
				outputLog.println(d);
			}
			//return true;
		} else {
			//return false;
		}
	}

	@Override
	public StateValuesDV deepCopy() throws PrismException
	{
		// Clone vector
		DoubleVector dv = new DoubleVector(values.getSize());
		dv.add(values);
		// Return copy
		return new StateValuesDV(dv, model);
	}
}
