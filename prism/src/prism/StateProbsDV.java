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
import parser.type.*;;

// state probability vector (double vector)

public class StateProbsDV implements StateProbs
{
	// prob vector
	DoubleVector probs;
	
	// info from model
	Model model;
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
	
	// flags
	boolean printSparse = true;
	boolean printMatlab = false;
	boolean printStates = true;

	// CONSTRUCTORS
	
	public StateProbsDV(DoubleVector p, Model m)
	{
		int i;
		
		// store prob vector
		probs = p;
		
		// get info from model
		model = m;
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
		// (note: dd must only be non-zero for reachable states)
		// (otherwise bad things happen)
		this(new DoubleVector(dd, model.getAllDDRowVars(), model.getODD()), model);
	}
	
	// CONVERSION METHODS
	
	// convert to StateProbsDV (nothing to do)
	public StateProbsDV convertToStateProbsDV()
	{
		return this;
	}
	
	// convert to StateProbsMTBDD, destroy (clear) old vector
	public StateProbsMTBDD convertToStateProbsMTBDD()
	{
		StateProbsMTBDD res = new StateProbsMTBDD(probs.convertToMTBDD(vars, odd), model);
		clear();
		return res;
	}
	
	// METHODS TO MODIFY VECTOR
	
	/**
	 * Set element i of this vector to value d. 
	 */
	private void setElement(int i, double d)
	{
		probs.setElement(i, d);
	}
	
	// read from file
	
	public void readFromFile(File file) throws PrismException
	{
		BufferedReader in;
		String s;
		int lineNum = 0, count = 0;
		double d;
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(file));
			// read remaining lines
			s = in.readLine(); lineNum++;
			while (s != null) {
				s = s.trim();
				if (!("".equals(s))) {
					if (count + 1 > probs.getSize())
						throw new PrismException("Too many values in initial distribution (" + (count + 1) + ", not " + probs.getSize() + ")");
					d = Double.parseDouble(s);
					setElement(count, d);
					count++;
				}
				s = in.readLine(); lineNum++;
			}
			// close file
			in.close();
			// check size
			if (count < probs.getSize())
				throw new PrismException("Too few values in initial distribution (" + count + ", not " + probs.getSize() + ")");
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
	
	// do a weighted sum of the elements of the vector and the values the mtbdd passed in
	// (used for csl reward steady state operator)
	
	public double sumOverMTBDD(JDDNode mult)
	{
		return probs.sumOverMTBDD(mult, vars, odd);
	}
	
	// sum up the elements of the vector, over a subset of its dd vars
	// store the result in a new StateProbsDV (for newModel)
	// throws PrismException on out-of-memory

	public StateProbs sumOverDDVars(JDDVars sumVars, Model newModel) throws PrismException
	{
		DoubleVector tmp;
		
		tmp = probs.sumOverDDVars(model.getAllDDRowVars(), odd, newModel.getODD(), sumVars.getMinVarIndex(), sumVars.getMaxVarIndex());
		
		return new StateProbsDV(tmp, newModel);
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
	
	/**
	 * Print vector to a log/file (non-zero entries only)
	 */
	public void print(PrismLog log) throws PrismException
	{
		print(log, true, false, true);
	}
	
	/**
	 * Print vector to a log/file.
	 * @param log: The log
	 * @param printSparse: Print non-zero elements only? 
	 * @param printMatlab: Print in Matlab format?
	 * @param printStates: Print states (variable values) for each element? 
	 */
public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		int i;
		
		// store flags
		this.printSparse = printSparse;
		this.printMatlab = printMatlab;
		this.printStates = printStates;
		
		// header for matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + probs.getSize() + ",1);");
		
		// check if all zero
		if (printSparse && !printMatlab && probs.getNNZ() == 0) {
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
			d = probs.getElement(n);
			printLine(n, d);
			return;
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
	
	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * @param log: The log
	 * @param filter: A BDD specifying which states to print for.
	 */
	public void printFiltered(PrismLog log, JDDNode filter) throws PrismException
	{
		printFiltered(log, filter, true, false, true);
	}
	
	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * @param log: The log
	 * @param filter: A BDD specifying which states to print for.
	 * @param printSparse: Print non-zero elements only? 
	 * @param printMatlab: Print in Matlab format?
	 * @param printStates: Print states (variable values) for each element? 
	 */
	public void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		int i;
		
		// store flags
		this.printSparse = printSparse;
		this.printMatlab = printMatlab;
		this.printStates = printStates;
		
		// header for matlab format
		if (printMatlab)
			log.println(!printSparse ? "v = [" : "v = sparse(" + probs.getSize() + ",1);");
		
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
			d = probs.getElement(n);
			printLine(n, d);
			return;
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
	
	private void printLine(int n, double d)
	{
		int i, j;
		if (!printSparse || d != 0) {
			if (printSparse)
				outputLog.print(printMatlab ? "v(" + (n + 1) + ")" : n);
			if (printStates && !printMatlab) {
				outputLog.print(":(");
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
				outputLog.print(")");
			}
			if (printSparse)
				outputLog.print("=");
			outputLog.print(d);
			if (printMatlab && printSparse)
				outputLog.print(";");
			outputLog.println();
		}
	}
	
	/**
	 * Make a (deep) copy of this vector
	 */
	public StateProbsDV deepCopy() throws PrismException
	{
		// Clone vector
		DoubleVector dv = new DoubleVector(probs.getSize());
		dv.add(probs);
		// Return copy
		return new StateProbsDV(dv, model);
	}
}
