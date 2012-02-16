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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.ast.ExpressionFormula;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionProp;
import parser.ast.ExpressionUnaryOp;
import parser.ast.ExpressionVar;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;
import prism.PrismLangException;
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
	//protected PrismSettings settings = new PrismSettings();

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

	// Settings methods
	
	/**
	 * Set settings from a PRISMSettings object.
	 */
	public void setSettings(PrismSettings settings) throws PrismException
	{
		verbosity = settings.getBoolean(PrismSettings.PRISM_VERBOSE) ? 10 : 1;
	}

	/**
	 * Inherit settings (and other info) from another model checker object.
	 */
	public void inheritSettings(StateModelChecker other)
	{
		setLog(other.getLog());
		setVerbosity(other.getVerbosity());
	}

	/**
	 * Print summary of current settings.
	 */
	public void printSettings()
	{
		mainLog.print("verbosity = " + verbosity + " ");
	}

	// Set methods for flags/settings

	/**
	 * Set verbosity level, i.e. amount of output produced.
	 */
	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
	}

	// Get methods for flags/settings

	public int getVerbosity()
	{
		return verbosity;
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
		ExpressionFilter exprFilter = null;
		long timer = 0;
		StateValues vals;
		String resultString;
		
		// Create storage for result
		result = new Result();

		// Remove labels from property, using combined label list (on a copy of the expression)
		// This is done now so that we can handle labels nested below operators that are not
		// handled natively by the model checker yet (just evaluate()ed in a loop).
		expr = (Expression) expr.deepCopy().expandLabels(propertiesFile.getCombinedLabelList());

		// Also evaluate/replace any constants
		//expr = (Expression) expr.replaceConstants(constantValues);

		// The final result of model checking will be a single value. If the expression to be checked does not
		// already yield a single value (e.g. because a filter has not been explicitly included), we need to wrap
		// a new (invisible) filter around it. Note that some filters (e.g. print/argmin/argmax) also do not
		// return single values and have to be treated in this way.
		if (!expr.returnsSingleValue()) {
			// New filter depends on expression type and number of initial states.
			// Boolean expressions...
			if (expr.getType() instanceof TypeBool) {
				// Result is true iff true for all initial states
				exprFilter = new ExpressionFilter("forall", expr, new ExpressionLabel("init"));
			}
			// Non-Boolean (double or integer) expressions...
			else {
				// Result is for the initial state, if there is just one,
				// or the range over all initial states, if multiple
				if (model.getNumInitialStates() == 1) {
					exprFilter = new ExpressionFilter("state", expr, new ExpressionLabel("init"));
				} else {
					exprFilter = new ExpressionFilter("range", expr, new ExpressionLabel("init"));
				}
			}
		}
		// Even, when the expression does already return a single value, if the the outermost operator
		// of the expression is not a filter, we still need to wrap a new filter around it.
		// e.g. 2*filter(...) or 1-P=?[...{...}]
		// This because the final result of model checking is only stored when we process a filter.
		else if (!(expr instanceof ExpressionFilter)) {
			// We just pick the first value (they are all the same)
			exprFilter = new ExpressionFilter("first", expr, new ExpressionLabel("init"));
			// We stop any additional explanation being displayed to avoid confusion.
			exprFilter.setExplanationEnabled(false);
		}
		
		// For any case where a new filter was created above...
		if (exprFilter != null) {
			// Make it invisible (not that it will be displayed)
			exprFilter.setInvisible(true);
			// Compute type of new filter expression (will be same as child)
			exprFilter.typeCheck();
			// Store as expression to be model checked
			expr = exprFilter;
		}

		// Do model checking and store result vector
		timer = System.currentTimeMillis();
		vals = checkExpression(model, expr);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		// Print result to log
		resultString = "Result";
		if (!("Result".equals(expr.getResultName())))
			resultString += " (" + expr.getResultName().toLowerCase() + ")";
		resultString += ": " + result.getResultString();
		mainLog.print("\n" + resultString + "\n");
		
		// Clean up
		vals.clear();

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
		if (expr instanceof ExpressionBinaryOp) {
			res = checkExpressionBinaryOp(model, (ExpressionBinaryOp) expr);
		}
		// Unary ops
		else if (expr instanceof ExpressionUnaryOp) {
			res = checkExpressionUnaryOp(model, (ExpressionUnaryOp) expr);
		}
		// Functions
		else if (expr instanceof ExpressionFunc) {
			res = checkExpressionFunc(model, (ExpressionFunc) expr);
		}
		// Identifiers
		else if (expr instanceof ExpressionIdent) {
			// Should never happen
			throw new PrismException("Unknown identifier \"" + ((ExpressionIdent) expr).getName() + "\"");
		}
		// Literals
		else if (expr instanceof ExpressionLiteral) {
			res = checkExpressionLiteral(model, (ExpressionLiteral) expr);
		}
		// Constants
		else if (expr instanceof ExpressionConstant) {
			res = checkExpressionConstant(model, (ExpressionConstant) expr);
		}
		// Formulas
		else if (expr instanceof ExpressionFormula) {
			// This should have been defined or expanded by now.
			if (((ExpressionFormula) expr).getDefinition() != null)
				return checkExpression(model, ((ExpressionFormula) expr).getDefinition());
			else
				throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula) expr).getName() + "\"");
		}
		// Variables
		else if (expr instanceof ExpressionVar) {
			res = checkExpressionVar(model, (ExpressionVar) expr);
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
		// Anything else - error
		else {
			throw new PrismException("Couldn't check " + expr.getClass());
		}

		return res;
	}

	/**
	 * Model check a binary operator.
	 */
	protected StateValues checkExpressionBinaryOp(Model model, ExpressionBinaryOp expr) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		int op = expr.getOperator();
		
		// Check operands recursively
		try {
			res1 = checkExpression(model, expr.getOperand1());
			res2 = checkExpression(model, expr.getOperand2());
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}
		
		// Apply operation
		res1.applyBinaryOp(op, res2);
		res2.clear();
		
		return res1;
	}

	/**
	 * Model check a unary operator.
	 */
	protected StateValues checkExpressionUnaryOp(Model model, ExpressionUnaryOp expr) throws PrismException
	{
		StateValues res1 = null;
		int op = expr.getOperator();
		
		// Check operand recursively
		res1 = checkExpression(model, expr.getOperand());
		
		// Parentheses are easy - nothing to do:
		if (op == ExpressionUnaryOp.PARENTH)
			return res1;

		// Apply operation
		res1.applyUnaryOp(op);
		
		return res1;
	}

	/**
	 * Model check a function.
	 */
	protected StateValues checkExpressionFunc(Model model, ExpressionFunc expr) throws PrismException
	{
		switch (expr.getNameCode()) {
		case ExpressionFunc.MIN:
		case ExpressionFunc.MAX:
			return checkExpressionFuncNary(model, expr);
		case ExpressionFunc.FLOOR:
		case ExpressionFunc.CEIL:
			return checkExpressionFuncUnary(model, expr);
		case ExpressionFunc.POW:
		case ExpressionFunc.MOD:
		case ExpressionFunc.LOG:
			return checkExpressionFuncBinary(model, expr);
		default:
			throw new PrismException("Unrecognised function \"" + expr.getName() + "\"");
		}
	}

	protected StateValues checkExpressionFuncUnary(Model model, ExpressionFunc expr) throws PrismException
	{
		StateValues res1 = null;
		int op = expr.getNameCode();
		
		// Check operand recursively
		res1 = checkExpression(model, expr.getOperand(0));
		
		// Apply operation
		try {
			res1.applyFunctionUnary(op);
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			if (e instanceof PrismLangException)
				((PrismLangException) e).setASTElement(expr);
			throw e;
		}
		
		return res1;
	}
	
	protected StateValues checkExpressionFuncBinary(Model model, ExpressionFunc expr) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		int op = expr.getNameCode();
		
		// Check operands recursively
		try {
			res1 = checkExpression(model, expr.getOperand(0));
			res2 = checkExpression(model, expr.getOperand(1));
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}
		
		// Apply operation
		try {
			res1.applyFunctionBinary(op, res2);
			res2.clear();
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			if (res2 != null)
				res2.clear();
			if (e instanceof PrismLangException)
				((PrismLangException) e).setASTElement(expr);
			throw e;
		}
		
		return res1;
	}
	
	protected StateValues checkExpressionFuncNary(Model model, ExpressionFunc expr) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		int i, n, op = expr.getNameCode();
		
		// Check first operand recursively
		res1 = checkExpression(model, expr.getOperand(0));
		// Go through remaining operands
		n = expr.getNumOperands();
		for (i = 1; i < n; i++) {
			// Check next operand recursively
			try {
				res2 = checkExpression(model, expr.getOperand(i));
			} catch (PrismException e) {
				if (res2 != null)
					res2.clear();
				throw e;
			}
			// Apply operation
			try {
				res1.applyFunctionBinary(op, res2);
				res2.clear();
			} catch (PrismException e) {
				if (res1 != null)
					res1.clear();
				if (res2 != null)
					res2.clear();
				if (e instanceof PrismLangException)
					((PrismLangException) e).setASTElement(expr);
				throw e;
			}
		}
		
		return res1;
	}
	
	/**
	 * Model check a literal.
	 */
	protected StateValues checkExpressionLiteral(Model model, ExpressionLiteral expr) throws PrismException
	{
		return new StateValues(expr.getType(), expr.evaluate(), model);
	}

	/**
	 * Model check a constant.
	 */
	protected StateValues checkExpressionConstant(Model model, ExpressionConstant expr) throws PrismException
	{
		return new StateValues(expr.getType(), expr.evaluate(constantValues), model);
	}

	/**
	 * Model check a variable reference.
	 */
	protected StateValues checkExpressionVar(Model model, ExpressionVar expr) throws PrismException
	{
		int numStates = model.getNumStates();
		StateValues res = new StateValues(expr.getType(), model);
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
		return res;
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
				bs.set(i, model.isDeadlockState(i));
			}
			return StateValues.createFromBitSet(bs, model);
		} else if (expr.getName().equals("init")) {
			int numStates = model.getNumStates();
			BitSet bs = new BitSet(numStates);
			for (i = 0; i < numStates; i++) {
				bs.set(i, model.isInitialState(i));
			}
			return StateValues.createFromBitSet(bs, model);
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
		// Filter info
		Expression filter;
		FilterOperator op;
		String filterStatesString;
		boolean filterInit, filterInitSingle, filterTrue;
		BitSet bsFilter = null;
		// Result info
		StateValues vals = null, resVals = null;
		BitSet bsMatch = null, bs;
		StateValues states;
		boolean b = false;
		int count = 0;
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
		bsFilter = checkExpression(model, filter).getBitSet();
		// Check if filter state set is empty; we treat this as an error
		if (bsFilter.isEmpty()) {
			throw new PrismException("Filter satisfies no states");
		}
		// Remember whether filter is for the initial state and, if so, whether there's just one
		filterInit = (filter instanceof ExpressionLabel && ((ExpressionLabel) filter).getName().equals("init"));
		filterInitSingle = filterInit & model.getNumInitialStates() == 1;
		// Print out number of states satisfying filter
		if (!filterInit)
			mainLog.println("\nStates satisfying filter " + filter + ": " + bsFilter.cardinality());

		// Compute result according to filter type
		op = expr.getOperatorType();
		switch (op) {
		case PRINT:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(filterTrue ? ":" : " that are also in filter " + filter + ":");
				vals.printFiltered(mainLog, bsFilter);
			} else {
				mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
				vals.printFiltered(mainLog, bsFilter);
			}
			// Result vector is unchanged; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = vals;
			// Set vals to null to stop it being cleared below
			vals = null;
			break;
		case MIN:
			// Compute min
			// Store as object/vector
			resObj = vals.minOverBitSet(bsFilter);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Minimum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			// TODO: un-hard-code precision once StateValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			break;
		case MAX:
			// Compute max
			// Store as object/vector
			resObj = vals.maxOverBitSet(bsFilter);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Maximum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			// TODO: un-hard-code precision once StateValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			break;
		case ARGMIN:
			// Compute/display min
			resObj = vals.minOverBitSet(bsFilter);
			mainLog.print("\nMinimum value over " + filterStatesString + ": " + resObj);
			// Find states that (are close to) selected value
			// TODO: un-hard-code precision once StateValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			// Store states in vector; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = StateValues.createFromBitSet(bsMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with minimum value: " + bsMatch.cardinality());
			bsMatch = null;
			break;
		case ARGMAX:
			// Compute/display max
			resObj = vals.maxOverBitSet(bsFilter);
			mainLog.print("\nMaximum value over " + filterStatesString + ": " + resObj);
			// Find states that (are close to) selected value
			// TODO: un-hard-code precision once StateValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			// Store states in vector; for ARGMAX, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = StateValues.createFromBitSet(bsMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with maximum value: " + bsMatch.cardinality());
			bsMatch = null;
			break;
		case COUNT:
			// Compute count
			count = vals.countOverBitSet(bsFilter);
			// Store as object/vector
			resObj = new Integer(count);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = filterTrue ? "Count of satisfying states" : "Count of satisfying states also in filter";
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case SUM:
			// Compute sum
			// Store as object/vector
			resObj = vals.sumOverBitSet(bsFilter);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Sum over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case AVG:
			// Compute average
			// Store as object/vector
			resObj = vals.averageOverBitSet(bsFilter);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Average over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case FIRST:
			// Find first value
			resObj = vals.firstFromBitSet(bsFilter);
			resVals = new StateValues(expr.getType(), resObj, model); 
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
			resObj = new prism.Interval(vals.minOverBitSet(bsFilter), vals.maxOverBitSet(bsFilter));
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
			// Get access to BitSet for this
			bs = vals.getBitSet();
			// Print some info to log
			mainLog.print("\nNumber of states satisfying " + expr.getOperand() + ": ");
			mainLog.print(bs.cardinality());
			mainLog.println(bs.cardinality() == model.getNumStates() ? " (all in model)" : "");
			// Check "for all" over filter
			b = vals.forallOverBitSet(bsFilter);
			// Store as object/vector
			resObj = new Boolean(b);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Property " + (b ? "" : "not ") + "satisfied in ";
			mainLog.print("\nProperty satisfied in " + vals.countOverBitSet(bsFilter));
			if (filterInit) {
				if (filterInitSingle) {
					resultExpl += "the initial state";
				} else {
					resultExpl += "all initial states";
				}
				mainLog.println(" of " + model.getNumInitialStates() + " initial states.");
			} else {
				if (filterTrue) {
					resultExpl += "all states";
					mainLog.println(" of all " + model.getNumStates() + " states.");
				} else {
					resultExpl += "all filter states";
					mainLog.println(" of " + bsFilter.cardinality() + " filter states.");
				}
			}
			break;
		case EXISTS:
			// Get access to BitSet for this
			bs = vals.getBitSet();
			// Check "there exists" over filter
			b = vals.existsOverBitSet(bsFilter);
			// Store as object/vector
			resObj = new Boolean(b);
			resVals = new StateValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Property satisfied in ";
			if (filterTrue) {
				resultExpl += b ? "at least one state" : "no states";
			} else {
				resultExpl += b ? "at least one filter state" : "no filter states";
			}
			mainLog.println("\n" + resultExpl);
			break;
		case STATE:
			// Check filter satisfied by exactly one state
			if (bsFilter.cardinality() != 1) {
				String s = "Filter should be satisfied in exactly 1 state";
				s += " (but \"" + filter + "\" is true in " + bsFilter.cardinality() + " states)";
				throw new PrismException(s);
			}
			// Find first (only) value
			// Store as object/vector
			resObj = vals.firstFromBitSet(bsFilter);
			resVals = new StateValues(expr.getType(), resObj, model); 
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
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		// For some operators, print out some matching states
		if (bsMatch != null) {
			states = StateValues.createFromBitSet(bsMatch, model);
			mainLog.print("\nThere are " + bsMatch.cardinality() + " states with ");
			mainLog.print(expr.getType() instanceof TypeDouble ? "(approximately) " : "" + "this value");
			boolean verbose = verbosity > 0; // TODO
			if (!verbose && bsMatch.cardinality() > 10) {
				mainLog.print(".\nThe first 10 states are displayed below. To view them all, enable verbose mode or use a print filter.\n");
				states.print(mainLog, 10);
			} else {
				mainLog.print(":\n");
				states.print(mainLog);
			}
		}

		// Store result
		result.setResult(resObj);
		// Set result explanation (if none or disabled, clear)
		if (expr.getExplanationEnabled() && resultExpl != null) {
			result.setExplanation(resultExpl.toLowerCase());
		} else {
			result.setExplanation(null);
		}

		// Clear up
		if (vals != null)
			vals.clear();

		return resVals;
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
