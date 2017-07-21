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

import java.io.File;

import jdd.JDDNode;
import jdd.JDDVars;
import parser.ast.RelOp;

/**
 * Interface for classes for state-indexed vectors of (integer or double) values
 */
public interface StateValues extends StateVector
{
	/** Converts to StateValuesDV, destroys (clear) this vector */
	StateValuesDV convertToStateValuesDV() throws PrismException;

	/** Converts to StateValuesMTBDD, destroys (clear) this vector */
	StateValuesMTBDD convertToStateValuesMTBDD();

	/**
	 * Switch the underlying model of this StateValues object.
	 * The new model has to be compatible with the old one,
	 * i.e., it has to have the same row variables and reachable states.
	 * <br>
	 * An example would be to switch between a CTMC and the embedded DTMC,
	 * i.e., where both models by construction have the same structure and state space.
	 * <br>
	 * Note that there are currently <b>no sanity checks</b>, so be careful when
	 * using this method.
	 * @param newModel the new model
	 */
	void switchModel(Model newModel);

	/**
	 * Set the elements of this vector by reading them in from a file.
	 */
	void readFromFile(File file) throws PrismException;

	/** Round the values of this vector to the given number of decimal places. */
	void roundOff(int places);

	/** Subtract all values of this vector from 1 */
	void subtractFromOne();

	/**
	 * Add the values of the other StateValues vector to the values of this vector.
	 * <br>
	 * The other StateValues vector has to have the same class as this vector.
	 */
	void add(StateValues sp);

	/** Multiplies the values of this vector with the constant {@code d}. */
	void timesConstant(double d);

	/**
	 * Compute dot (inner) product of this and another vector.
	 * <br>
	 * The other StateValues vector has to have the same class as this vector.
	 */
	double dotProduct(StateValues sp);

	/**
	 * Filter this vector using a BDD (set elements not in filter to 0).
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	void filter(JDDNode filter);

	/**
	 * Filter this vector using a BDD (set elements not in filter to constant {@code d}).
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	void filter(JDDNode filter, double d);

	/**
	 * Apply max operator, i.e. vec[i] = max(vec[i], vec2[i]), where vec2 is an MTBDD
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	public void maxMTBDD(JDDNode vec2);

	/** Clear the stored information. */
	void clear();

	/** Get the number of non-zero values in this vector. */
	int getNNZ();

	/** Get the number of non-zero values in this vector as a String. */
	String getNNZString();

	/**
	 * Get the value of the first vector element that is in the (BDD) filter.
	 * <br>
	 * Should be called with a non-empty filter. For an empty filter, will return
	 * {@code Double.NaN}.
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	double firstFromBDD(JDDNode filter);

	/**
	 * Get the minimum value of those that are in the (BDD) filter.
	 * <br>
	 * If the filter is empty for this vector, returns positive infinity.
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	double minOverBDD(JDDNode filter);

	/**
	 * Get the maximum value of those that are in the (BDD) filter.
	 * <br>
	 * If the filter is empty for this vector, returns negative infinity.
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	double maxOverBDD(JDDNode filter);

	/**
	 * Get the maximum finite value of those that are in the (BDD) filter.
	 * <br>
	 * If the filter is empty for this vector or all values for the filter are non-finite,
	 * returns negative infinity.
	 * <br>[ DEREFS: <i>none</i> ]
	 */
	public double maxFiniteOverBDD(JDDNode filter);

	/**
	 * Get the sum of those elements that are in the (BDD) filter.
	 * If the filter is empty for this vector, returns 0.
 	 * <br>[ DEREFS: <i>none</i> ]
	 */
	double sumOverBDD(JDDNode filter);

	/**
	 * Do a weighted sum of the elements of the vector and the values the MTBDD passed in
	 * (used for CSL reward steady state operator).
	 */
	double sumOverMTBDD(JDDNode mult);

	/**
	* Sum up the elements of the vector, over a subset of its DD vars,
	* store the result in a new StateValues (for newModel).
	* <br>[ DEREFS: <i>none</i> ]
	*/
	StateValues sumOverDDVars(JDDVars sumVars, Model newModel) throws PrismException;

	/** Returns an Object with the value of the i-th entry in this vector. */
	Object getValue(int i);

	/**
	 * Generate BDD for states in the given interval
	 * (interval specified as relational operator and bound)
	 * <br>[ REFS: <i>result</i> ]
	 */
	JDDNode getBDDFromInterval(String relOpString, double bound);

	/**
	 * Generate BDD for states in the given interval
	 * (interval specified as relational operator and bound)
	 * <br>[ REFS: <i>result</i> ]
	 */
	JDDNode getBDDFromInterval(RelOp relOp, double bound);

	/**
	 * Generate BDD for states in the given interval
	 * (interval specified as inclusive lower/upper bound)
	 * <br>[ REFS: <i>result</i> ]
	 */
	JDDNode getBDDFromInterval(double lo, double hi);

	/**
	 * Generate BDD for states whose value is close to 'value'
	 * (within either absolute or relative error 'epsilon')
	 * <br>[ REFS: <i>result</i> ]
	 * @param val the value
	 * @param epsilon the error bound
	 * @param abs true for absolute error calculation
	 */
	JDDNode getBDDFromCloseValue(double val, double epsilon, boolean abs);

	/**
	 * Generate BDD for states whose value is close to 'value'
	 * (within absolute error 'epsilon')
	 * <br>[ REFS: <i>result</i> ]
	 * @param val the value
	 * @param epsilon the error bound
	 */
	JDDNode getBDDFromCloseValueAbs(double val, double epsilon);

	/**
	 * Generate BDD for states whose value is close to 'value'
	 * (within relative error 'epsilon')
	 * <br>[ REFS: <i>result</i> ]
	 * @param val the value
	 * @param epsilon the error bound
	 */
	JDDNode getBDDFromCloseValueRel(double val, double epsilon);

	/**
	 * Print vector to a log/file (non-zero entries only)
	 */
	void print(PrismLog log) throws PrismException;

	/**
	 * Print vector to a log/file.
	 * @param log The log
	 * @param printSparse Print non-zero elements only?
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element?
	 */
	void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException;

	/**
	 * Print vector to a log/file.
	 * @param log The log
	 * @param printSparse Print non-zero elements only?
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element?
	 * @param printIndices Print state indices for each element?
	 */
	void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException;

	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * <br>[ DEREFS: <i>none</i> ]
	 * @param log The log
	 * @param filter A BDD specifying which states to print for.
	 */
	void printFiltered(PrismLog log, JDDNode filter) throws PrismException;

	/**
	 * Print part of a vector to a log/file (non-zero entries only).
	 * <br>[ DEREFS: <i>none</i> ]
	 * @param log The log
	 * @param filter A BDD specifying which states to print for.
	 * @param printSparse Print non-zero elements only?
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print states (variable values) for each element?
	 */
	void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException;

	/**
	 * Make a (deep) copy of this vector
	 */
	StateValues deepCopy() throws PrismException; 
}
