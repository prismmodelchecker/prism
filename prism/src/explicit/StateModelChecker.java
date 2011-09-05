//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package explicit;

import java.io.*;
import java.util.*;

import parser.State;
import parser.Values;
import parser.ast.*;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.type.*;
import prism.Prism;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;
import prism.PrismSettings;
import prism.Result;

/**
 * Super class for explicit-state model checkers
 */
public class StateModelChecker
{
	// Log for output (default to System.out)
	protected PrismLog mainLog = new PrismPrintStreamLog(System.out);

	// PRISM settings object
	protected PrismSettings settings = new PrismSettings();

	// Model file (for reward structures, etc.)
	protected ModulesFile modulesFile = null;

	// Properties file (for labels, constants, etc.)
	protected PropertiesFile propertiesFile = null;

	// Constants (extracted from model/properties)
	protected Values constantValues;

	// The result of model checking will be stored here
	protected Result result;

	// Flags/settings

	// Verbosity level
	protected int verbosity = 0;
	// Iterative numerical method termination criteria
	protected TermCrit termCrit = TermCrit.RELATIVE;
	// Parameter for iterative numerical method termination criteria
	protected double termCritParam = 1e-8;
	// Max iterations for numerical solution
	protected int maxIters = 100000; // TODO: make same as PRISM?
	// Use precomputation algorithms in model checking?
	protected boolean precomp = true;
	protected boolean prob0 = true;
	protected boolean prob1 = true;
	// Direction of convergence for value iteration (lfp/gfp)
	protected ValIterDir valIterDir = ValIterDir.BELOW;
	// Method used for numerical solution
	protected SolnMethod solnMethod = SolnMethod.VALUE_ITERATION;

	// Enums for flags/settings

	// Iterative numerical method termination criteria
	public enum TermCrit {
		ABSOLUTE, RELATIVE
	};

	// Direction of convergence for value iteration (lfp/gfp)
	public enum ValIterDir {
		BELOW, ABOVE
	};

	// Method used for numerical solution
	public enum SolnMethod {
		VALUE_ITERATION, GAUSS_SEIDEL, POLICY_ITERATION, MODIFIED_POLICY_ITERATION
	};

	// Setters/getters

	/**
	 * Set log for output.
	 */
	public void setLog(PrismLog log)
	{
		this.mainLog = log;
	}

	/**
	 * Get log for output.
	 */
	public PrismLog getLog()
	{
		return mainLog;
	}

	/**
	 * Set PRISM settings object.
	 */
	public void setSettings(PrismSettings settings)
	{
		this.settings = settings;
	}

	/**
	 * Get PRISM settings object.
	 */
	public PrismSettings getSettings()
	{
		return settings;
	}

	/**
	 * Set the attached model file (for e.g. reward structures when model checking)
	 * and the attached properties file (for e.g. constants/labels when model checking)
	 */
	public void setModulesFileAndPropertiesFile(ModulesFile modulesFile, PropertiesFile propertiesFile)
	{
		this.modulesFile = modulesFile;
		this.propertiesFile = propertiesFile;
		// Get combined constant values from model/properties
		constantValues = new Values();
		constantValues.addValues(modulesFile.getConstantValues());
		if (propertiesFile != null)
			constantValues.addValues(propertiesFile.getConstantValues());
	}

	// Set methods for flags/settings

	/**
	 * Set verbosity level, i.e. amount of output produced.
	 */
	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
	}

	/**
	 * Set termination criteria type for numerical iterative methods.
	 */
	public void setTermCrit(TermCrit termCrit)
	{
		this.termCrit = termCrit;
	}

	/**
	 * Set termination criteria parameter (epsilon) for numerical iterative methods.
	 */
	public void setTermCritParam(double termCritParam)
	{
		this.termCritParam = termCritParam;
	}

	/**
	 * Set maximum number of iterations for numerical iterative methods.
	 */
	public void setMaxIters(int maxIters)
	{
		this.maxIters = maxIters;
	}

	/**
	 * Set whether or not to use precomputation (Prob0, Prob1, etc.).
	 */
	public void setPrecomp(boolean precomp)
	{
		this.precomp = precomp;
	}

	/**
	 * Set whether or not to use Prob0 precomputation
	 */
	public void setProb0(boolean prob0)
	{
		this.prob0 = prob0;
	}

	/**
	 * Set whether or not to use Prob1 precomputation
	 */
	public void setProb1(boolean prob1)
	{
		this.prob1 = prob1;
	}

	/**
	 * Set direction of convergence for value iteration (lfp/gfp).
	 */
	public void setValIterDir(ValIterDir valIterDir)
	{
		this.valIterDir = valIterDir;
	}

	/**
	 * Set method used for numerical solution.
	 */
	public void setSolnMethod(SolnMethod solnMethod)
	{
		this.solnMethod = solnMethod;
	}

	// Get methods for flags/settings

	public int getVerbosity()
	{
		return verbosity;
	}

	public TermCrit getTermCrit()
	{
		return termCrit;
	}

	public double getTermCritParam()
	{
		return termCritParam;
	}

	public int getMaxIters()
	{
		return maxIters;
	}

	public boolean getPrecomp()
	{
		return precomp;
	}

	public boolean getProb0()
	{
		return prob0;
	}

	public boolean getProb1()
	{
		return prob1;
	}

	public ValIterDir getValIterDir()
	{
		return valIterDir;
	}

	public SolnMethod getSolnMethod()
	{
		return solnMethod;
	}

	/**
	 * Inherit settings from another model checker object.
	 */
	public void inheritSettings(StateModelChecker other)
	{
		setLog(other.getLog());
		setVerbosity(other.getVerbosity());
		setTermCrit(other.getTermCrit());
		setTermCritParam(other.getTermCritParam());
		setMaxIters(other.getMaxIters());
		setPrecomp(other.getPrecomp());
		setProb0(other.getProb0());
		setProb1(other.getProb1());
		setValIterDir(other.getValIterDir());
		setSolnMethod(other.getSolnMethod());
	}

	/**
	 * Print summary of current settings.
	 */
	public void printSettings()
	{
		mainLog.print("\nMC Settings:");
		mainLog.print(" verbosity = " + verbosity);
		mainLog.print(" termCrit = " + termCrit);
		mainLog.print(" termCritParam = " + termCritParam);
		mainLog.print(" maxIters = " + maxIters);
		mainLog.print(" precomp = " + precomp);
		mainLog.print(" prob0 = " + prob0);
		mainLog.print(" prob1 = " + prob1);
		mainLog.print(" valIterDir = " + valIterDir);
		mainLog.print(" solnMethod = " + solnMethod);
		mainLog.println();
	}

	// Model checking functions

	/**
	 * Model check an expression, process and return the result.
	 * Information about states and model constants should be attached to the model.
	 * For other required info (labels, reward structures, etc.), use the methods
	 * {@link #setModulesFile} and {@link #setPropertiesFile}
	 * to attach the original model/properties files.
	 */
	public Result check(Model model, Expression expr) throws PrismException
	{
		long timer = 0;
		Object vals;
		BitSet valsBitSet;
		StateValues valsSV;
		String resultExpl, resultString;
		double res = 0.0, minRes = 0.0, maxRes = 0.0;
		boolean satInit = false;
		int numSat = 0;

		// Create storage for result
		result = new Result();

		// Remove labels from property, using combined label list (on a copy of the expression)
		// This is done now so that we can handle labels nested below operators that are not
		// handled natively by the model checker yet (just evaluate()ed in a loop).
		expr = (Expression) expr.deepCopy().expandLabels(propertiesFile.getCombinedLabelList());

		// Also evaluate/replace any constants
		//expr = (Expression) expr.replaceConstants(constantValues);

		// Do model checking and store result vector
		timer = System.currentTimeMillis();
		valsSV = checkExpression(model, expr);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		// Boolean results
		if (expr.getType() instanceof TypeBool) {

			// Cast to Bitset
			valsBitSet = valsSV.getBitSet();
			// And check how many states are satisfying
			numSat = valsBitSet.cardinality();

			// Result returned depends on the number of initial states
			// and whether it is just a single value (e.g. if the top-level operator is a filter)

			// Case where this is a single value (e.g. filter)
			if (expr.returnsSingleValue()) {
				// Get result for initial state (although it is the same for all states)
				satInit = valsBitSet.get(model.getFirstInitialState());
				result.setResult(new Boolean(satInit));
				result.setExplanation(null);
			}
			// Case where there is a single initial state
			else if (model.getNumInitialStates() == 1) {
				// Result is true iff satisfied in single initial state
				satInit = valsBitSet.get(model.getFirstInitialState());
				resultExpl = "property " + (satInit ? "" : "not ") + "satisfied in the initial state";
				result.setResult(new Boolean(satInit));
				result.setExplanation(resultExpl);
			}
			// Case where there are multiple initial states
			else {
				throw new PrismException("Not yet supported"); // TODO
				/*// Result is true iff satisfied in all initial states
				satInit = states.includesAll(start);
				resultExpl = "property ";
				if (satInit)
					resultExpl += "satisfied in all " + model.getNumStartStatesString();
				else
					resultExpl += "only satisifed in " + numSat + " of " + model.getNumStartStatesString();
				resultExpl += " initial states";
				result.setResult(new Boolean(satInit));
				result.setExplanation(resultExpl);*/
			}

			// Print extra info to log
			if (!expr.returnsSingleValue()) {
				// Print number of satisfying states to log
				mainLog.print("\nNumber of satisfying states: ");
				mainLog.print(numSat == -1 ? ">" + Integer.MAX_VALUE : "" + numSat);
				// TODO: mainLog.println(states.includesAll(reach) ? " (all)" : "");
				// If in "verbose" mode, print out satisfying states to log
				if (verbosity > 0) {
					mainLog.println("\nSatisfying states:");
					//TODO: states.print(mainLog);
				}
			}

			// Clean up
			//states.clear();
		}

		// Non-Boolean (double or integer) results
		else {
			// Result returned depends on the number of initial states
			// and whether it is just a single value (e.g. from if the top-level operator is a filter)

			// Case where this is a single value (e.g. filter)
			if (expr.returnsSingleValue()) {
				// Get result for initial state (although it is the same for all states)
				Object o = valsSV.getValue(model.getFirstInitialState());
				result.setResult(o);
				result.setExplanation(null);
			}
			// Case where there is a single initial state
			else if (model.getNumInitialStates() == 1) {
				// Result is the value for the single initial state
				Object o = valsSV.getValue(model.getFirstInitialState());
				resultExpl = "value in the initial state";
				result.setResult(o);
				result.setExplanation(resultExpl);
			}
			// Case where there are multiple initial states
			else {
				// TODO
				/*// Result is the interval of values from all initial states
				minRes = vals.minOverBDD(start);
				maxRes = vals.maxOverBDD(start);
				// TODO: This will be a range, eventually
				// (for now just do first val, as before)
				// TODO: also need to handle integer-typed case
				// resultExpl = "range over " + model.getNumStartStatesString() + " initial states";
				res = vals.firstFromBDD(start);
				resultExpl = "value for first of " + model.getNumStartStatesString() + " initial states";
				result.setResult(new Double(res));
				result.setExplanation(resultExpl);*/
			}

			// Print extra info to log
			if (!expr.returnsSingleValue()) {
				// If in "verbose" mode, print out result values to log
				if (getVerbosity() > 5) {
					mainLog.println("\nResults (non-zero only) for all states:");
					mainLog.print(valsSV);
				}
			}
		}

		// Print result to log
		resultString = "Result";
		if (!("Result".equals(expr.getResultName())))
			resultString += " (" + expr.getResultName().toLowerCase() + ")";
		resultString += ": " + result;
		if (result.getExplanation() != null)
			resultString += " (" + result.getExplanation() + ")";
		mainLog.print("\n" + resultString + "\n");

		// Clean up
		//vals.clear();

		// Return result
		return result;
	}

	/**
	 * Model check an expression and return a vector result values over all states.
	 * Information about states and model constants should be attached to the model.
	 * For other required info (labels, reward structures, etc.), use the methods
	 * {@link #setModulesFile} and {@link #setPropertiesFile}
	 * to attach the original model/properties files.
	 */
	public StateValues checkExpression(Model model, Expression expr) throws PrismException
	{
		StateValues res = null;

		// Binary ops
		// (just "and" for now - more to come later)
		if (expr instanceof ExpressionBinaryOp && Expression.isAnd(expr)) {
			res = checkExpressionBinaryOp(model, (ExpressionBinaryOp) expr);
		}
		// Literals
		else if (expr instanceof ExpressionLiteral) {
			res = checkExpressionLiteral(model, (ExpressionLiteral) expr);
		}
		// Labels
		else if (expr instanceof ExpressionLabel) {
			res = checkExpressionLabel(model, (ExpressionLabel) expr);
		}
		// Property refs
		else if (expr instanceof ExpressionProp) {
			res = checkExpressionProp(model, (ExpressionProp) expr);
		}
		// Filter
		else if (expr instanceof ExpressionFilter) {
			res = checkExpressionFilter(model, (ExpressionFilter) expr);
		}

		// Anything else - just evaluate expression repeatedly
		else {
			// Evaluate/replace any constants first
			expr = (Expression) expr.replaceConstants(constantValues);
			
			int numStates = model.getNumStates();
			res = new StateValues(expr.getType(), numStates);
			List<State> statesList = model.getStatesList();
			if (expr.getType() instanceof TypeBool) {
				for (int i = 0; i < numStates; i++) {
					res.setBooleanValue(i, expr.evaluateBoolean(statesList.get(i)));
				}
			} else if (expr.getType() instanceof TypeInt) {
				for (int i = 0; i < numStates; i++) {
					res.setIntValue(i, expr.evaluateInt(statesList.get(i)));
				}
			} else if (expr.getType() instanceof TypeDouble) {
				for (int i = 0; i < numStates; i++) {
					res.setDoubleValue(i, expr.evaluateDouble(statesList.get(i)));
				}
			}
		}
		// Anything else - error
		/*else {
			throw new PrismException("Couldn't check " + expr.getClass());
		}*/

		return res;
	}

	/**
	 * Model check a binary operator.
	 */
	protected StateValues checkExpressionBinaryOp(Model model, ExpressionBinaryOp expr) throws PrismException
	{
		// (just "and" for now - more to come later)
		StateValues res1 = checkExpression(model, expr.getOperand1());
		StateValues res2 = checkExpression(model, expr.getOperand2());
		res1.and(res2);
		res2.clear();
		return res1;
	}

	/**
	 * Model check a literal.
	 */
	protected StateValues checkExpressionLiteral(Model model, ExpressionLiteral expr) throws PrismException
	{
		return new StateValues(expr.getType(), model.getNumStates(), expr.evaluate());
	}

	/**
	 * Model check a label.
	 */
	protected StateValues checkExpressionLabel(Model model, ExpressionLabel expr) throws PrismException
	{
		LabelList ll;
		int i;

		// treat special cases
		if (expr.getName().equals("deadlock")) {
			int numStates = model.getNumStates();
			BitSet bs = new BitSet(numStates);
			for (i = 0; i < numStates; i++) {
				bs.set(i, model.isFixedDeadlockState(i));
			}
			return StateValues.createFromBitSet(bs, numStates);
		} else if (expr.getName().equals("init")) {
			int numStates = model.getNumStates();
			BitSet bs = new BitSet(numStates);
			for (i = 0; i < numStates; i++) {
				bs.set(i, model.isInitialState(i));
			}
			return StateValues.createFromBitSet(bs, numStates);
		} else {
			ll = propertiesFile.getCombinedLabelList();
			i = ll.getLabelIndex(expr.getName());
			if (i == -1)
				throw new PrismException("Unknown label \"" + expr.getName() + "\" in property");
			// check recursively
			return checkExpression(model, ll.getLabel(i));
		}
	}

	// Check property ref

	protected StateValues checkExpressionProp(Model model, ExpressionProp expr) throws PrismException
	{
		// Look up property and check recursively
		Property prop = propertiesFile.lookUpPropertyObjectByName(expr.getName());
		if (prop != null) {
			mainLog.println("\nModel checking : " + prop);
			return checkExpression(model, prop.getExpression());
		} else {
			throw new PrismException("Unknown property reference " + expr);
		}
	}

	// Check filter

	protected StateValues checkExpressionFilter(Model model, ExpressionFilter expr) throws PrismException
	{
		throw new PrismException("Explicit engine does not yet handle filters");
		
		/*
		// Filter info
		Expression filter;
		FilterOperator op;
		String filterStatesString;
		StateListMTBDD statesFilter;
		boolean filterInit, filterInitSingle, filterTrue;
		JDDNode ddFilter = null;
		// Result info
		StateValues vals = null, resVals = null;
		JDDNode ddMatch = null, dd;
		StateListMTBDD states;
		double d = 0.0, d2 = 0.0;
		boolean b = false;
		String resultExpl = null;
		Object resObj = null;

		// Check operand recursively
		vals = checkExpression(model, expr.getOperand());
		// Translate filter
		filter = expr.getFilter();
		// Create default filter (true) if none given
		if (filter == null)
			filter = Expression.True();
		// Remember whether filter is "true"
		filterTrue = Expression.isTrue(filter);
		// Store some more info
		filterStatesString = filterTrue ? "all states" : "states satisfying filter";
		ddFilter = checkExpressionDD(filter);
		statesFilter = new StateListMTBDD(ddFilter, model);
		// Check if filter state set is empty; we treat this as an error
		if (ddFilter.equals(JDD.ZERO)) {
			throw new PrismException("Filter satisfies no states");
		}
		// Remember whether filter is for the initial state and, if so, whether there's just one
		filterInit = (filter instanceof ExpressionLabel && ((ExpressionLabel) filter).getName().equals("init"));
		filterInitSingle = filterInit & model.getNumStartStates() == 1;
		// Print out number of states satisfying filter
		if (!filterInit)
			mainLog.println("\nStates satisfying filter " + filter + ": " + statesFilter.sizeString());

		// Compute result according to filter type
		op = expr.getOperatorType();
		switch (op) {
		case PRINT:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(filterTrue ? ":" : " that are also in filter " + filter + ":");
				dd = vals.deepCopy().convertToStateValuesMTBDD().getJDDNode();
				new StateListMTBDD(dd, model).print(mainLog);
				JDD.Deref(dd);
			} else {
				// TODO: integer-typed case: either add to print method or store in StateValues
				mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
				vals.printFiltered(mainLog, ddFilter);
			}
			// Result vector is unchanged; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = vals;
			// Set vals to null to stop it being cleared below
			vals = null;
			break;
		case MIN:
			// Compute min
			d = vals.minOverBDD(ddFilter);
			// Store as object/vector (note crazy Object cast to avoid Integer->int auto conversion)
			resObj = (expr.getType() instanceof TypeInt) ? ((Object) new Integer((int) d)) : (new Double(d));
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = "Minimum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			break;
		case MAX:
			// Compute max
			d = vals.maxOverBDD(ddFilter);
			// Store as object/vector (note crazy Object cast to avoid Integer->int auto conversion)
			resObj = (expr.getType() instanceof TypeInt) ? ((Object) new Integer((int) d)) : (new Double(d));
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = "Maximum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			break;
		case ARGMIN:
			// Compute/display min
			d = vals.minOverBDD(ddFilter);
			mainLog.print("\nMinimum value over " + filterStatesString + ": ");
			mainLog.println((expr.getType() instanceof TypeInt) ? ((Object) new Integer((int) d)) : (new Double(d)));
			// Find states that (are close to) selected value
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			// Store states in vector; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = new StateValuesMTBDD(ddMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with minimum value: " + resVals.getNNZString());
			ddMatch = null;
			break;
		case ARGMAX:
			// Compute/display max
			d = vals.maxOverBDD(ddFilter);
			mainLog.print("\nMaximum value over " + filterStatesString + ": ");
			mainLog.println((expr.getType() instanceof TypeInt) ? ((Object) new Integer((int) d)) : (new Double(d)));
			// Find states that (are close to) selected value
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			// Store states in vector; for ARGMAX, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = new StateValuesMTBDD(ddMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with maximum value: " + resVals.getNNZString());
			ddMatch = null;
			break;
		case COUNT:
			// Compute count
			vals.filter(ddFilter);
			d = vals.getNNZ();
			// Store as object/vector
			resObj = new Integer((int) d);
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = filterTrue ? "Count of satisfying states" : "Count of satisfying states also in filter";
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case SUM:
			// Compute sum
			d = vals.sumOverBDD(ddFilter);
			// Store as object/vector (note crazy Object cast to avoid Integer->int auto conversion)
			resObj = (expr.getType() instanceof TypeInt) ? ((Object) new Integer((int) d)) : (new Double(d));
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = "Sum over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case AVG:
			// Compute average
			d = vals.sumOverBDD(ddFilter) / JDD.GetNumMinterms(ddFilter, allDDRowVars.n());
			// Store as object/vector
			resObj = new Double(d);
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = "Average over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case FIRST:
			// Find first value
			d = vals.firstFromBDD(ddFilter);
			// Store as object/vector
			if (expr.getType() instanceof TypeInt) {
				resObj = new Integer((int) d);
			} else if (expr.getType() instanceof TypeDouble) {
				resObj = new Double(d);
			} else {
				resObj = new Boolean(d > 0);
			}
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = "Value in ";
			if (filterInit) {
				resultExpl += filterInitSingle ? "the initial state" : "first initial state";
			} else {
				resultExpl += filterTrue ? "the first state" : "first state satisfying filter";
			}
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case RANGE:
			// Find range of values
			d = vals.minOverBDD(ddFilter);
			d2 = vals.maxOverBDD(ddFilter);
			// Store as object
			if (expr.getOperand().getType() instanceof TypeInt) {
				resObj = new prism.Interval((int) d, (int) d2);
			} else {
				resObj = new prism.Interval(d, d2);
			}
			// Leave result vector unchanged: for a range, result is only available from Result object
			resVals = vals;
			// Set vals to null to stop it being cleared below
			vals = null;
			// Create explanation of result and print some details to log
			resultExpl = "Range of values over ";
			resultExpl += filterInit ? "initial states" : filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case FORALL:
			// Get access to BDD for this
			dd = vals.convertToStateValuesMTBDD().getJDDNode();
			// Print some info to log
			mainLog.print("\nNumber of states satisfying " + expr.getOperand() + ": ");
			states = new StateListMTBDD(dd, model);
			mainLog.print(states.sizeString());
			mainLog.println(states.includesAll(reach) ? " (all in model)" : "");
			// Check "for all" over filter, store result
			JDD.Ref(ddFilter);
			dd = JDD.And(dd, ddFilter);
			states = new StateListMTBDD(dd, model);
			b = dd.equals(ddFilter);
			// Store as object/vector
			resObj = new Boolean(b);
			resVals = new StateValuesMTBDD(JDD.Constant(b ? 1.0 : 0.0), model);
			// Set vals to null so that is not clear()-ed twice
			vals = null;
			// Create explanation of result and print some details to log
			resultExpl = "Property " + (b ? "" : "not ") + "satisfied in ";
			mainLog.print("\nProperty satisfied in " + states.sizeString());
			if (filterInit) {
				if (filterInitSingle) {
					resultExpl += "the initial state";
				} else {
					resultExpl += "all initial states";
				}
				mainLog.println(" of " + model.getNumStartStatesString() + " initial states.");
			} else {
				if (filterTrue) {
					resultExpl += "all states";
					mainLog.println(" of all " + model.getNumStatesString() + " states.");
				} else {
					resultExpl += "all filter states";
					mainLog.println(" of " + statesFilter.sizeString() + " filter states.");
				}
			}
			// Derefs
			JDD.Deref(dd);
			break;
		case EXISTS:
			// Get access to BDD for this
			dd = vals.convertToStateValuesMTBDD().getJDDNode();
			// Check "there exists" over filter
			JDD.Ref(ddFilter);
			dd = JDD.And(dd, ddFilter);
			b = !dd.equals(JDD.ZERO);
			// Store as object/vector
			resObj = new Boolean(b);
			resVals = new StateValuesMTBDD(JDD.Constant(b ? 1.0 : 0.0), model);
			// Set vals to null so that is not clear()-ed twice
			vals = null;
			// Create explanation of result and print some details to log
			resultExpl = "Property satisfied in ";
			if (filterTrue) {
				resultExpl += b ? "at least one state" : "no states";
			} else {
				resultExpl += b ? "at least one filter state" : "no filter states";
			}
			mainLog.println("\n" + resultExpl);
			// Derefs
			JDD.Deref(dd);
			break;
		case STATE:
			// Check filter satisfied by exactly one state
			if (statesFilter.size() != 1) {
				String s = "Filter should be satisfied in exactly 1 state";
				s += " (but \"" + filter + "\" is true in " + statesFilter.size() + " states)";
				throw new PrismException(s);
			}
			// Find first (only) value
			d = vals.firstFromBDD(ddFilter);
			// Store as object/vector
			if (expr.getType() instanceof TypeInt) {
				resObj = new Integer((int) d);
			} else if (expr.getType() instanceof TypeDouble) {
				resObj = new Double(d);
			} else {
				resObj = new Boolean(d > 0);
			}
			resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			// Create explanation of result and print some details to log
			resultExpl = "Value in ";
			if (filterInit) {
				resultExpl += "the initial state";
			} else {
				resultExpl += "the filter state";
			}
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		default:
			JDD.Deref(ddFilter);
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		// For some operators, print out some matching states
		if (ddMatch != null) {
			states = new StateListMTBDD(ddMatch, model);
			mainLog.print("\nThere are " + states.sizeString() + " states with ");
			mainLog.print(expr.getType() instanceof TypeDouble ? "(approximately) " : "" + "this value");
			if (!verbose && (states.size() == -1 || states.size() > 10)) {
				mainLog.print(".\nThe first 10 states are displayed below. To view them all, enable verbose mode or use a print filter.\n");
				states.print(mainLog, 10);
			} else {
				mainLog.print(":\n");
				states.print(mainLog);
			}
			JDD.Deref(ddMatch);
		}

		// Store result
		result.setResult(resObj);
		// Set result explanation (if none or disabled, clear)
		if (expr.getExplanationEnabled() && resultExpl != null) {
			result.setExplanation(resultExpl.toLowerCase());
		} else {
			result.setExplanation(null);
		}

		// Derefs, clears
		JDD.Deref(ddFilter);
		if (vals != null)
			vals.clear();

		return resVals;*/
	}
	
	/**
	 * Loads labels from a PRISM labels file and stores them in BitSet objects.
	 * (Actually, it returns a map from label name Strings to BitSets.)
	 * (Note: the size of the BitSet may be smaller than the number of states.) 
	 */
	public Map<String, BitSet> loadLabelsFile(String filename) throws PrismException
	{
		BufferedReader in;
		ArrayList<String> labels;
		BitSet bitsets[];
		Map<String, BitSet> res;
		String s, ss[];
		int i, j, k;

		try {
			// Open file
			in = new BufferedReader(new FileReader(new File(filename)));
			// Parse first line to get label list
			s = in.readLine();
			if (s == null)
				throw new PrismException("Empty labels file");
			ss = s.split(" ");
			labels = new ArrayList<String>(ss.length);
			for (i = 0; i < ss.length; i++) {
				s = ss[i];
				j = s.indexOf('=');
				if (j < 0)
					throw new PrismException("Corrupt labels file (line 1)");
				k = Integer.parseInt(s.substring(0, j));
				while (labels.size() <= k)
					labels.add("?");
				labels.set(k, s.substring(j + 2, s.length() - 1));
			}
			// Build list of bitsets
			bitsets = new BitSet[labels.size()];
			for (i = 0; i < bitsets.length; i++)
				bitsets[i] = new BitSet();
			// Parse remaining lines
			s = in.readLine();
			while (s != null) {
				// Skip blank lines
				s = s.trim();
				if (s.length() > 0) {
					// Split line
					ss = s.split(":");
					i = Integer.parseInt(ss[0].trim());
					ss = ss[1].trim().split(" ");
					for (j = 0; j < ss.length; j++) {
						if (ss[j].length() == 0)
							continue;
						k = Integer.parseInt(ss[j]);
						// Store label info
						bitsets[k].set(i);
					}
				}
				// Prepare for next iter
				s = in.readLine();
			}
			// Close file
			in.close();
			// Build BitSet map
			res = new HashMap<String, BitSet>();
			for (i = 0; i < labels.size(); i++) {
				if (!labels.get(i).equals("?")) {
					res.put(labels.get(i), bitsets[i]);
				}
			}
			return res;
		} catch (IOException e) {
			throw new PrismException("Error reading labels file \"" + filename + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error in labels file");
		}
	}
}
