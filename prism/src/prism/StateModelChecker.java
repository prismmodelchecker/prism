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

import dv.DoubleVector;
import jdd.*;
import odd.*;
import parser.*;
import parser.ast.*;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.type.*;

// Base class for model checkers - does state-based evaluations (no temporal/probabilistic)

public class StateModelChecker implements ModelChecker
{
	// PRISM stuff
	protected Prism prism;
	protected PrismLog mainLog;
	protected PrismLog techLog;

	// Properties file
	protected PropertiesFile propertiesFile;

	// Constant values
	protected Values constantValues;

	// Model info
	protected Model model;
	protected VarList varList;
	protected JDDNode trans;
	protected JDDNode trans01;
	protected JDDNode transActions;
	protected JDDNode start;
	protected JDDNode reach;
	protected ODDNode odd;
	protected JDDVars allDDRowVars;
	protected JDDVars allDDColVars;
	protected JDDVars[] varDDRowVars;

	// The result of model checking will be stored here
	protected Result result;

	// Options:

	// Which engine to use
	protected int engine;
	// Parameter for termination criterion
	protected double termCritParam;
	// Verbose mode?
	protected boolean verbose;

	// Constructor

	public StateModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// Initialise
		this.prism = prism;
		mainLog = prism.getMainLog();
		techLog = prism.getTechLog();
		model = m;
		propertiesFile = pf;
		constantValues = new Values();
		constantValues.addValues(model.getConstantValues());
		if (pf != null)
			constantValues.addValues(pf.getConstantValues());
		varList = model.getVarList();
		trans = model.getTrans();
		trans01 = model.getTrans01();
		transActions = model.getTransActions();
		start = model.getStart();
		reach = model.getReach();
		odd = model.getODD();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		varDDRowVars = model.getVarDDRowVars();

		// Inherit some options from parent Prism object
		// Store locally and/or pass onto engines
		engine = prism.getEngine();
		termCritParam = prism.getTermCritParam();
		verbose = prism.getVerbose();
	}

	/**
	 * Additional constructor for creating stripped down StateModelChecker for
	 * expression to MTBDD conversions.
	 */
	public StateModelChecker(Prism prism, VarList varList, JDDVars allDDRowVars, JDDVars[] varDDRowVars,
			Values constantValues) throws PrismException
	{
		// Initialise
		this.prism = prism;
		mainLog = prism.getMainLog();
		techLog = prism.getTechLog();
		this.varList = varList;
		this.varDDRowVars = varDDRowVars;
		this.constantValues = constantValues;
		// Create dummy model
		reach = null;
		allDDRowVars.refAll();
		model = new ProbModel(JDD.Constant(0), JDD.Constant(0), new JDDNode[] {}, new JDDNode[] {}, null, allDDRowVars,
				new JDDVars(), null, 0, null, null, null, 0, varList, varDDRowVars, null, constantValues);
	}

	/**
	 * Clean up the dummy model created when using the abbreviated constructor
	 */
	public void clearDummyModel()
	{
		model.clear();
	}

	/**
	 * Model check an expression, process and return the result.
	 */
	public Result check(Expression expr) throws PrismException
	{
		long timer = 0;
		StateListMTBDD states;
		StateValues vals;
		String resultExpl, resultString;
		double res = 0.0, minRes = 0.0, maxRes = 0.0;
		boolean satInit = false;
		int numSat = 0;

		// Create storage for result
		result = new Result();

		// Do model checking and store result vector
		timer = System.currentTimeMillis();
		vals = checkExpression(expr);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		// Boolean results
		if (expr.getType() instanceof TypeBool) {

			// Convert to StateList object
			vals = vals.convertToStateValuesMTBDD();
			states = new StateListMTBDD(((StateValuesMTBDD) vals).getJDDNode(), model);
			JDD.Ref(((StateValuesMTBDD) vals).getJDDNode());
			// And check how many states are satisfying
			numSat = states.size();

			// Result returned depends on the number of initial states
			// and whether it is just a single value (e.g. if the top-level operator is a filter)

			// Case where this is a single value (e.g. filter)
			if (expr.returnsSingleValue()) {
				// Get result for initial state (although it is the same for all states)
				satInit = states.includesAll(start);
				result.setResult(new Boolean(satInit));
				result.setExplanation(null);
			}
			// Case where there is a single initial state
			else if (model.getNumStartStates() == 1) {
				// Result is true iff satisfied in single initial state
				satInit = states.includesAll(start);
				resultExpl = "property " + (satInit ? "" : "not ") + "satisfied in the initial state";
				result.setResult(new Boolean(satInit));
				result.setExplanation(resultExpl);
			}
			// Case where there are multiple initial states
			else {
				// Result is true iff satisfied in all initial states
				satInit = states.includesAll(start);
				resultExpl = "property ";
				if (satInit)
					resultExpl += "satisfied in all " + model.getNumStartStatesString();
				else
					resultExpl += "only satisifed in " + numSat + " of " + model.getNumStartStatesString();
				resultExpl += " initial states";
				result.setResult(new Boolean(satInit));
				result.setExplanation(resultExpl);
			}

			// Print extra info to log
			if (!expr.returnsSingleValue()) {
				// Print number of satisfying states to log
				mainLog.print("\nNumber of satisfying states: ");
				mainLog.print(numSat == -1 ? ">" + Integer.MAX_VALUE : "" + numSat);
				mainLog.println(states.includesAll(reach) ? " (all)" : "");
				// If in "verbose" mode, print out satisfying states to log
				if (verbose) {
					mainLog.println("\nSatisfying states:");
					states.print(mainLog);
				}
			}

			// Clean up
			states.clear();
		}

		// Non-Boolean (double or integer) results
		else {
			// Result returned depends on the number of initial states
			// and whether it is just a single value (e.g. from if the top-level operator is a filter)

			// Case where this is a single value (e.g. filter)
			if (expr.returnsSingleValue()) {
				// Get result for initial state (although it is the same for all states)
				res = vals.firstFromBDD(start);
				result.setResult(new Double(res));
				result.setExplanation(null);
			}
			// Case where there is a single initial state
			else if (model.getNumStartStates() == 1) {
				// Result is the value for the single initial state
				res = vals.firstFromBDD(start);
				// TODO: Object resX = (expr.getType() instanceof TypeInt) ? new Integer((int) res) 
				resultExpl = "value in the initial state";
				result.setResult(new Double(res));
				result.setExplanation(resultExpl);
			}
			// Case where there are multiple initial states
			else {
				// Result is the interval of values from all initial states
				minRes = vals.minOverBDD(start);
				maxRes = vals.maxOverBDD(start);
				// TODO: This will be a range, eventually
				// (for now just do first val, as before)
				// TODO: also need to handle integer-typed case
				// resultExpl = "range over " + model.getNumStartStatesString() + " initial states";
				res = vals.firstFromBDD(start);
				resultExpl = "value for first of " + model.getNumStartStatesString() + " initial states";
				result.setResult(new Double(res));
				result.setExplanation(resultExpl);
			}

			// Print extra info to log
			if (!expr.returnsSingleValue()) {
				// If in "verbose" mode, print out result values to log
				if (verbose) {
					mainLog.println("\nResults (non-zero only) for all states:");
					vals.print(mainLog);
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
		vals.clear();

		// Return result
		return result;
	}

	/**
	 * Model check an expression and return a vector result values over all states.
	 */
	public StateValues checkExpression(Expression expr) throws PrismException
	{
		StateValues res;

		// If-then-else
		if (expr instanceof ExpressionITE) {
			res = checkExpressionITE((ExpressionITE) expr);
		}
		// Binary ops
		else if (expr instanceof ExpressionBinaryOp) {
			res = checkExpressionBinaryOp((ExpressionBinaryOp) expr);
		}
		// Unary ops
		else if (expr instanceof ExpressionUnaryOp) {
			res = checkExpressionUnaryOp((ExpressionUnaryOp) expr);
		}
		// Functions
		else if (expr instanceof ExpressionFunc) {
			res = checkExpressionFunc((ExpressionFunc) expr);
		}
		// Identifiers
		else if (expr instanceof ExpressionIdent) {
			// Should never happen
			throw new PrismException("Unknown identifier \"" + ((ExpressionIdent) expr).getName() + "\"");
		}
		// Literals
		else if (expr instanceof ExpressionLiteral) {
			res = checkExpressionLiteral((ExpressionLiteral) expr);
		}
		// Constants
		else if (expr instanceof ExpressionConstant) {
			res = checkExpressionConstant((ExpressionConstant) expr);
		}
		// Formulas
		else if (expr instanceof ExpressionFormula) {
			// This should have been defined or expanded by now.
			if (((ExpressionFormula) expr).getDefinition() != null)
				return checkExpression(((ExpressionFormula) expr).getDefinition());
			else
				throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula) expr).getName() + "\"");
		}
		// Variables
		else if (expr instanceof ExpressionVar) {
			res = checkExpressionVar((ExpressionVar) expr);
		}
		// Labels
		else if (expr instanceof ExpressionLabel) {
			res = checkExpressionLabel((ExpressionLabel) expr);
		}
		// Filter
		else if (expr instanceof ExpressionFilter) {
			res = checkExpressionFilter((ExpressionFilter) expr);
		}
		// Anything else - error
		else {
			throw new PrismException("Couldn't check " + expr.getClass());
		}

		// Filter out non-reachable states from solution
		// (only necessary for symbolically stored vectors)
		// (skip if reach is null, e.g. if just being used to convert arbitrary expressions)
		if (res instanceof StateValuesMTBDD && reach != null)
			res.filter(reach);

		return res;
	}

	// Check expression, convert to symbolic form (if not already), return BDD

	public JDDNode checkExpressionDD(Expression expr) throws PrismException
	{
		return checkExpression(expr).convertToStateValuesMTBDD().getJDDNode();
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	/*
	 * These check methods (and similar ones in subclasses of this class) return
	 * a StateValues object which is a vector, over all states, of values. This can
	 * be represented either symbolically (as an (MT)BDD, encapsulated in a StateValuesMTBDD
	 * object) or explicitly (as an array of doubles, encapsulated in a StateValuesDV
	 * object, containing a DoubleVector object).
	 * 
	 * It is always possible to convert between these two forms but this will not always be
	 * efficient. In particular, we want to avoid creating: (a) explicit vectors for very large
	 * models where the vector can only be feasibly stored as an MTBDD; (b) and symbolic
	 * vectors for irregular vectors which ar small enough to be stored explicitly but would
	 * blow up as an MTBDD.
	 * 
	 * Various schemes (and user preferences/configurations) are possible. Currently:
	 * 
	 * - simple, atomic expressions (constants, variable references, etc.) are
	 *   created symbolically (since this is easy and usually efficient)
	 * - for arithmetic operations, the result is stored explicitly if one or
	 *   more of its operands is explicit and symbolic otherwise
	 * - operators with Boolean results are always stored symbolically
	 * 
	 * So, currently the only time that explicit vectors are created anew is (in subclasses
	 * of this model checker) for e.g. the result of P=?, R=?, S=?, etc. operators. And for
	 * these operators, the result will be symbolic if the MTBDD engine is being used (or
	 * in some cases where the answer is trivial, e.g. 1 for all states). For P>p etc.
	 * properties, the vector will be stored symbolically since values are Booleans. 
	 */

	// Check an 'if-then-else'
	protected StateValues checkExpressionITE(ExpressionITE expr) throws PrismException
	{
		StateValues res1 = null, res2 = null, res3 = null;
		JDDNode dd, dd1, dd2, dd3;
		DoubleVector dv2, dv3;

		// Check operands recursively
		try {
			res1 = checkExpression(expr.getOperand1());
			res2 = checkExpression(expr.getOperand2());
			res3 = checkExpression(expr.getOperand3());
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			if (res2 != null)
				res2.clear();
			if (res3 != null)
				res3.clear();
			throw e;
		}

		// Operand 1 is boolean so should be symbolic
		dd1 = res1.convertToStateValuesMTBDD().getJDDNode();

		// If both operands 2/3 are symbolic, result will be symbolic
		if (res2 instanceof StateValuesMTBDD && res3 instanceof StateValuesMTBDD) {
			dd2 = ((StateValuesMTBDD) res2).getJDDNode();
			dd3 = ((StateValuesMTBDD) res3).getJDDNode();
			dd = JDD.ITE(dd1, dd2, dd3);
			return new StateValuesMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv2 = res2.convertToStateValuesDV().getDoubleVector();
			dv2.filter(dd1, allDDRowVars, odd);
			dv3 = res3.convertToStateValuesDV().getDoubleVector();
			dd1 = JDD.Not(dd1);
			dv3.filter(dd1, allDDRowVars, odd);
			dv2.add(dv3);
			dv3.clear();
			JDD.Deref(dd1);
			return new StateValuesDV(dv2, model);
		}
	}

	// Check a binary operator

	protected StateValues checkExpressionBinaryOp(ExpressionBinaryOp expr) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		JDDNode dd, dd1, dd2;
		DoubleVector dv1, dv2;
		int i, n, op = expr.getOperator();

		// Optimisations are possible for relational operators
		// (note dubious use of knowledge that op IDs are consecutive)
		if (op >= ExpressionBinaryOp.EQ && op <= ExpressionBinaryOp.LE) {
			return checkExpressionRelOp(op, expr.getOperand1(), expr.getOperand2());
		}

		// Check operands recursively
		try {
			res1 = checkExpression(expr.getOperand1());
			res2 = checkExpression(expr.getOperand2());
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			if (res2 != null)
				res2.clear();
			throw e;
		}

		// If both operands are symbolic, result will be symbolic
		if (res1 instanceof StateValuesMTBDD && res2 instanceof StateValuesMTBDD) {
			dd1 = ((StateValuesMTBDD) res1).getJDDNode();
			dd2 = ((StateValuesMTBDD) res2).getJDDNode();
			// Apply operation
			switch (op) {
			case ExpressionBinaryOp.IMPLIES:
				dd = JDD.Or(JDD.Not(dd1), dd2);
				break;
			case ExpressionBinaryOp.OR:
				dd = JDD.Or(dd1, dd2);
				break;
			case ExpressionBinaryOp.AND:
				dd = JDD.And(dd1, dd2);
				break;
			case ExpressionBinaryOp.PLUS:
				dd = JDD.Apply(JDD.PLUS, dd1, dd2);
				break;
			case ExpressionBinaryOp.MINUS:
				dd = JDD.Apply(JDD.MINUS, dd1, dd2);
				break;
			case ExpressionBinaryOp.TIMES:
				dd = JDD.Apply(JDD.TIMES, dd1, dd2);
				break;
			case ExpressionBinaryOp.DIVIDE:
				dd = JDD.Apply(JDD.DIVIDE, dd1, dd2);
				break;
			default:
				throw new PrismException("Unknown binary operator");
			}
			return new StateValuesMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv1 = res1.convertToStateValuesDV().getDoubleVector();
			dv2 = res2.convertToStateValuesDV().getDoubleVector();
			n = dv1.getSize();
			// Apply operation
			switch (op) {
			case ExpressionBinaryOp.IMPLIES:
			case ExpressionBinaryOp.OR:
			case ExpressionBinaryOp.AND:
				throw new PrismException("Internal error: Explicit evaluation of Boolean");
				//for (i = 0; i < n; i++) dv1.setElement(i, (!(dv1.getElement(i)>0) || (dv2.getElement(i)>0)) ? 1.0 : 0.0);
				//for (i = 0; i < n; i++) dv1.setElement(i, ((dv1.getElement(i)>0) || (dv2.getElement(i)>0)) ? 1.0 : 0.0);
				//for (i = 0; i < n; i++) dv1.setElement(i, ((dv1.getElement(i)>0) && (dv2.getElement(i)>0)) ? 1.0 : 0.0);
			case ExpressionBinaryOp.PLUS:
				for (i = 0; i < n; i++)
					dv1.setElement(i, dv1.getElement(i) + dv2.getElement(i));
				break;
			case ExpressionBinaryOp.MINUS:
				for (i = 0; i < n; i++)
					dv1.setElement(i, dv1.getElement(i) - dv2.getElement(i));
				break;
			case ExpressionBinaryOp.TIMES:
				for (i = 0; i < n; i++)
					dv1.setElement(i, dv1.getElement(i) * dv2.getElement(i));
				break;
			case ExpressionBinaryOp.DIVIDE:
				for (i = 0; i < n; i++)
					dv1.setElement(i, dv1.getElement(i) / dv2.getElement(i));
				break;
			default:
				throw new PrismException("Unknown binary operator");
			}
			dv2.clear();
			return new StateValuesDV(dv1, model);
		}
	}

	// Check a relational operator (=, !=, >, >=, < <=)

	protected StateValues checkExpressionRelOp(int op, Expression expr1, Expression expr2) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		JDDNode dd, dd1, dd2;
		String s;

		// Check for some easy (and common) special cases before resorting to
		// the general case

		// var relop int
		if (expr1 instanceof ExpressionVar && expr2.isConstant() && expr2.getType() instanceof TypeInt) {
			ExpressionVar e1;
			Expression e2;
			int i, j, l, h, v;
			e1 = (ExpressionVar) expr1;
			e2 = expr2;
			// get var's index
			s = e1.getName();
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismException("Unknown variable \"" + s + "\"");
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			dd = JDD.Constant(0);
			i = e2.evaluateInt(constantValues, null);
			switch (op) {
			case ExpressionBinaryOp.EQ:
				if (i >= l && i <= h)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], i - l, 1);
				break;
			case ExpressionBinaryOp.NE:
				if (i >= l && i <= h)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], i - l, 1);
				dd = JDD.Not(dd);
				break;
			case ExpressionBinaryOp.GT:
				for (j = i + 1; j <= h; j++)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			case ExpressionBinaryOp.GE:
				for (j = i; j <= h; j++)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			case ExpressionBinaryOp.LT:
				for (j = i - 1; j >= l; j--)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			case ExpressionBinaryOp.LE:
				for (j = i; j >= l; j--)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			default:
				throw new PrismException("Unknown relational operator");
			}
			return new StateValuesMTBDD(dd, model);
		}
		// int relop var
		else if (expr1.isConstant() && expr1.getType() instanceof TypeInt && expr2 instanceof ExpressionVar) {
			Expression e1;
			ExpressionVar e2;
			int i, j, l, h, v;
			e1 = expr1;
			e2 = (ExpressionVar) expr2;
			// get var's index
			s = e2.getName();
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismException("Unknown variable \"" + s + "\"");
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			dd = JDD.Constant(0);
			i = e1.evaluateInt(constantValues, null);
			switch (op) {
			case ExpressionBinaryOp.EQ:
				if (i >= l && i <= h)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], i - l, 1);
				break;
			case ExpressionBinaryOp.NE:
				if (i >= l && i <= h)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], i - l, 1);
				dd = JDD.Not(dd);
				break;
			case ExpressionBinaryOp.GT:
				for (j = i - 1; j >= l; j--)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			case ExpressionBinaryOp.GE:
				for (j = i; j >= l; j--)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			case ExpressionBinaryOp.LT:
				for (j = i + 1; j <= h; j++)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			case ExpressionBinaryOp.LE:
				for (j = i; j <= h; j++)
					dd = JDD.SetVectorElement(dd, varDDRowVars[v], j - l, 1);
				break;
			default:
				throw new PrismException("Unknown relational operator");
			}
			return new StateValuesMTBDD(dd, model);
		}

		// General case.
		// Since the result is a Boolean and thus returned as an MTBDD, we
		// just convert both operands to MTBDDs first. Optimisations would be possible here.
		// Check operands recursively
		try {
			res1 = checkExpression(expr1);
			res2 = checkExpression(expr2);
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			if (res2 != null)
				res2.clear();
			throw e;
		}
		dd1 = res1.convertToStateValuesMTBDD().getJDDNode();
		dd2 = res2.convertToStateValuesMTBDD().getJDDNode();
		switch (op) {
		case ExpressionBinaryOp.EQ:
			dd = JDD.Apply(JDD.EQUALS, dd1, dd2);
			break;
		case ExpressionBinaryOp.NE:
			dd = JDD.Apply(JDD.NOTEQUALS, dd1, dd2);
			break;
		case ExpressionBinaryOp.GT:
			dd = JDD.Apply(JDD.GREATERTHAN, dd1, dd2);
			break;
		case ExpressionBinaryOp.GE:
			dd = JDD.Apply(JDD.GREATERTHANEQUALS, dd1, dd2);
			break;
		case ExpressionBinaryOp.LT:
			dd = JDD.Apply(JDD.LESSTHAN, dd1, dd2);
			break;
		case ExpressionBinaryOp.LE:
			dd = JDD.Apply(JDD.LESSTHANEQUALS, dd1, dd2);
			break;
		default:
			throw new PrismException("Unknown relational operator");
		}
		return new StateValuesMTBDD(dd, model);
	}

	// Check a unary operator

	protected StateValues checkExpressionUnaryOp(ExpressionUnaryOp expr) throws PrismException
	{
		StateValues res1 = null;
		JDDNode dd, dd1;
		DoubleVector dv1;
		int i, n, op = expr.getOperator();

		// Check operand recursively
		try {
			res1 = checkExpression(expr.getOperand());
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}

		// Parentheses are easy - nothing to do:
		if (op == ExpressionUnaryOp.PARENTH)
			return res1;

		// If operand is symbolic, result will be symbolic
		if (res1 instanceof StateValuesMTBDD) {
			dd1 = ((StateValuesMTBDD) res1).getJDDNode();
			// Apply operation
			switch (op) {
			case ExpressionUnaryOp.NOT:
				dd = JDD.Not(dd1);
				break;
			case ExpressionUnaryOp.MINUS:
				dd = JDD.Apply(JDD.MINUS, JDD.Constant(0), dd1);
				break;
			default:
				throw new PrismException("Unknown unary operator");
			}
			return new StateValuesMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv1 = res1.convertToStateValuesDV().getDoubleVector();
			n = dv1.getSize();
			// Apply operation
			switch (op) {
			case ExpressionUnaryOp.NOT:
				throw new PrismException("Internal error: Explicit evaluation of Boolean");
				//for (i = 0; i < n; i++) dv1.setElement(i, (dv1.getElement(i)>0) ? 0.0 : 1.0);
			case ExpressionUnaryOp.MINUS:
				for (i = 0; i < n; i++)
					dv1.setElement(i, -dv1.getElement(i));
				break;
			default:
				throw new PrismException("Unknown unary operator");
			}
			return new StateValuesDV(dv1, model);
		}
	}

	// Check a 'function'

	protected StateValues checkExpressionFunc(ExpressionFunc expr) throws PrismException
	{
		switch (expr.getNameCode()) {
		case ExpressionFunc.MIN:
		case ExpressionFunc.MAX:
			return checkExpressionFuncNary(expr);
		case ExpressionFunc.FLOOR:
		case ExpressionFunc.CEIL:
			return checkExpressionFuncUnary(expr);
		case ExpressionFunc.POW:
		case ExpressionFunc.MOD:
		case ExpressionFunc.LOG:
			return checkExpressionFuncBinary(expr);
		default:
			throw new PrismException("Unrecognised function \"" + expr.getName() + "\"");
		}
	}

	protected StateValues checkExpressionFuncUnary(ExpressionFunc expr) throws PrismException
	{
		StateValues res1 = null;
		JDDNode dd1;
		DoubleVector dv1;
		int i, n, op = expr.getNameCode();

		// Check operand recursively
		try {
			res1 = checkExpression(expr.getOperand(0));
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}
		// Symbolic
		if (res1 instanceof StateValuesMTBDD) {
			dd1 = ((StateValuesMTBDD) res1).getJDDNode();
			switch (op) {
			case ExpressionFunc.FLOOR:
				dd1 = JDD.MonadicApply(JDD.FLOOR, dd1);
				break;
			case ExpressionFunc.CEIL:
				dd1 = JDD.MonadicApply(JDD.CEIL, dd1);
				break;
			}
			return new StateValuesMTBDD(dd1, model);
		}
		// Explicit
		else {
			dv1 = res1.convertToStateValuesDV().getDoubleVector();
			n = dv1.getSize();
			switch (op) {
			case ExpressionFunc.FLOOR:
				for (i = 0; i < n; i++)
					dv1.setElement(i, Math.floor(dv1.getElement(i)));
				break;
			case ExpressionFunc.CEIL:
				for (i = 0; i < n; i++)
					dv1.setElement(i, Math.ceil(dv1.getElement(i)));
				break;
			}
			return new StateValuesDV(dv1, model);
		}
	}

	protected StateValues checkExpressionFuncBinary(ExpressionFunc expr) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		JDDNode dd = null, dd1, dd2;
		DoubleVector dv1, dv2;
		int i, n, op = expr.getNameCode();
		double d = 0.0;

		// Check operands recursively
		try {
			res1 = checkExpression(expr.getOperand(0));
			res2 = checkExpression(expr.getOperand(1));
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			if (res2 != null)
				res2.clear();
			throw e;
		}
		// If both operands are symbolic, result will be symbolic
		if (res1 instanceof StateValuesMTBDD && res2 instanceof StateValuesMTBDD) {
			dd1 = ((StateValuesMTBDD) res1).getJDDNode();
			dd2 = ((StateValuesMTBDD) res2).getJDDNode();
			switch (op) {
			case ExpressionFunc.POW:
				dd = JDD.Apply(JDD.POW, dd1, dd2);
				break;
			case ExpressionFunc.MOD:
				dd = JDD.Apply(JDD.MOD, dd1, dd2);
				break;
			case ExpressionFunc.LOG:
				dd = JDD.Apply(JDD.LOGXY, dd1, dd2);
				break;
			}
			return new StateValuesMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv1 = res1.convertToStateValuesDV().getDoubleVector();
			dv2 = res2.convertToStateValuesDV().getDoubleVector();
			n = dv1.getSize();
			switch (op) {
			case ExpressionFunc.POW:
				for (i = 0; i < n; i++)
					dv1.setElement(i, Math.pow(dv1.getElement(i), dv2.getElement(i)));
				break;
			case ExpressionFunc.MOD:
				for (i = 0; i < n; i++) {
					d = (int) dv2.getElement(i);
					d = (d == 0) ? Double.NaN : (int) dv1.getElement(i) % (int) d;
					dv1.setElement(i, d);
				}
				break;
			case ExpressionFunc.LOG:
				for (i = 0; i < n; i++)
					dv1.setElement(i, PrismUtils.log(dv1.getElement(i), dv2.getElement(i)));
				break;
			}
			dv2.clear();
			return new StateValuesDV(dv1, model);
		}
	}

	protected StateValues checkExpressionFuncNary(ExpressionFunc expr) throws PrismException
	{
		StateValues res1 = null, res2 = null;
		JDDNode dd1, dd2;
		DoubleVector dv1, dv2;
		int i, i2, n, n2, op = expr.getNameCode();
		boolean symbolic;

		// Check first operand recursively
		try {
			res1 = checkExpression(expr.getOperand(0));
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}
		// Go through remaining operands
		// Switch to explicit as soon as an operand is explicit
		n = expr.getNumOperands();
		symbolic = (res1 instanceof StateValuesMTBDD);
		for (i = 1; i < n; i++) {
			try {
				res2 = checkExpression(expr.getOperand(i));
			} catch (PrismException e) {
				if (res2 != null)
					res2.clear();
				throw e;
			}
			// Explicit
			if (!symbolic || !(res2 instanceof StateValuesMTBDD)) {
				symbolic = false;
				dv1 = res1.convertToStateValuesDV().getDoubleVector();
				dv2 = res2.convertToStateValuesDV().getDoubleVector();
				n2 = dv1.getSize();
				switch (op) {
				case ExpressionFunc.MIN:
					for (i2 = 0; i2 < n2; i2++)
						dv1.setElement(i2, Math.min(dv1.getElement(i), dv2.getElement(i)));
					break;
				case ExpressionFunc.MAX:
					for (i2 = 0; i2 < n2; i2++)
						dv1.setElement(i2, Math.max(dv1.getElement(i), dv2.getElement(i)));
					break;
				}
				dv2.clear();
				res1 = new StateValuesDV(dv1, model);
			}
			// Symbolic
			else {
				dd1 = ((StateValuesMTBDD) res1).getJDDNode();
				dd2 = ((StateValuesMTBDD) res2).getJDDNode();
				switch (op) {
				case ExpressionFunc.MIN:
					dd1 = JDD.Apply(JDD.MIN, dd1, dd2);
					break;
				case ExpressionFunc.MAX:
					dd1 = JDD.Apply(JDD.MAX, dd1, dd2);
					break;
				}
				res1 = new StateValuesMTBDD(dd1, model);
			}
		}

		return res1;
	}

	// Check a literal

	protected StateValues checkExpressionLiteral(ExpressionLiteral expr) throws PrismException
	{
		JDDNode dd;
		try {
			dd = JDD.Constant(expr.evaluateDouble());
		} catch (PrismLangException e) {
			throw new PrismException("Unknown literal type");
		}
		return new StateValuesMTBDD(dd, model);
	}

	// Check a constant

	protected StateValues checkExpressionConstant(ExpressionConstant expr) throws PrismException
	{
		int i;
		JDDNode dd;

		i = constantValues.getIndexOf(expr.getName());
		if (i == -1)
			throw new PrismException("Couldn't evaluate constant \"" + expr.getName() + "\"");
		try {
			dd = JDD.Constant(constantValues.getDoubleValue(i));
		} catch (PrismLangException e) {
			throw new PrismException("Unknown type for constant \"" + expr.getName() + "\"");
		}

		return new StateValuesMTBDD(dd, model);
	}

	// Check a variable reference

	protected StateValues checkExpressionVar(ExpressionVar expr) throws PrismException
	{
		String s;
		int v, l, h, i;
		JDDNode dd;

		s = expr.getName();
		// get the variable's index
		v = varList.getIndex(s);
		if (v == -1) {
			throw new PrismException("Unknown variable \"" + s + "\"");
		}
		// get some info on the variable
		l = varList.getLow(v);
		h = varList.getHigh(v);
		// create dd
		dd = JDD.Constant(0);
		for (i = l; i <= h; i++) {
			dd = JDD.SetVectorElement(dd, varDDRowVars[v], i - l, i);
		}

		return new StateValuesMTBDD(dd, model);
	}

	// Check label

	protected StateValues checkExpressionLabel(ExpressionLabel expr) throws PrismException
	{
		LabelList ll;
		JDDNode dd;
		int i;

		// treat special cases
		if (expr.getName().equals("deadlock")) {
			dd = model.getFixedDeadlocks();
			JDD.Ref(dd);
			return new StateValuesMTBDD(dd, model);
		} else if (expr.getName().equals("init")) {
			dd = start;
			JDD.Ref(dd);
			return new StateValuesMTBDD(dd, model);
		} else {
			// get expression associated with label
			ll = propertiesFile.getCombinedLabelList();
			i = ll.getLabelIndex(expr.getName());
			if (i == -1)
				throw new PrismException("Unknown label \"" + expr.getName() + "\" in property");
			// check recursively
			return checkExpression(ll.getLabel(i));
		}
	}

	// Check filter

	protected StateValues checkExpressionFilter(ExpressionFilter expr) throws PrismException
	{
		FilterOperator op;
		Expression filter;
		StateValues vals = null, res = null;
		JDDNode ddFilter = null, ddMatch = null, dd;
		StateListMTBDD states;
		boolean empty = false;
		double d = 0.0;
		boolean b = false;

		// Check operand recursively
		vals = checkExpression(expr.getOperand());
		// Translate filter
		filter = expr.getFilter();
		if (filter == null)
			filter = Expression.True();
		ddFilter = checkExpressionDD(filter);
		// Check if filter state set is empty
		// (display warning and optimise/catch below)
		if (ddFilter.equals(JDD.ZERO)) {
			empty = true;
			mainLog.println("\nWarning: Filter " + filter + " satisfies no states");
		}

		// Compute result according to filter type
		op = expr.getOperatorType();
		switch (op) {
		case PRINT:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(Expression.isTrue(filter) ? ":" : " that are also in filter " + filter + ":");
				dd = vals.deepCopy().convertToStateValuesMTBDD().getJDDNode();
				new StateListMTBDD(dd, model).print(mainLog);
				JDD.Deref(dd);
			} else { // TODO: integer-typed case
				mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
				vals.printFiltered(mainLog, ddFilter);
			}
			// Result is unchanged
			res = vals;
			// Set vals to null to stop it being cleared below
			vals = null;
			break;
		case MIN:
			d = empty ? Double.POSITIVE_INFINITY : vals.minOverBDD(ddFilter);
			mainLog.println("\nFilter: minimum value for states satisfying " + filter + ": " + d);
			res = new StateValuesMTBDD(JDD.Constant(d), model);
			// Also find states that (are close to) selected value for display to log
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			break;
		case MAX:
			d = empty ? Double.NEGATIVE_INFINITY : vals.maxOverBDD(ddFilter);
			mainLog.println("\nFilter: maximum value for states satisfying " + filter + ": " + d);
			res = new StateValuesMTBDD(JDD.Constant(d), model);
			// Also find states that (are close to) selected value for display to log
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			break;
		case ARGMIN:
			d = empty ? Double.POSITIVE_INFINITY : vals.minOverBDD(ddFilter);
			mainLog.println("\nFilter: minimum value for states satisfying " + filter + ": " + d);
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			res = new StateValuesMTBDD(ddMatch, model);
			mainLog.println("Filter: number of states with minimum value: " + res.getNNZString());
			ddMatch = null;
			break;
		case ARGMAX:
			d = empty ? Double.NEGATIVE_INFINITY : vals.maxOverBDD(ddFilter);
			mainLog.println("\nFilter: maximum value for states satisfying " + filter + ": " + d);
			ddMatch = vals.getBDDFromCloseValue(d, prism.getTermCritParam(), prism.getTermCrit() == Prism.ABSOLUTE);
			JDD.Ref(ddFilter);
			ddMatch = JDD.And(ddMatch, ddFilter);
			res = new StateValuesMTBDD(ddMatch, model);
			mainLog.println("Filter: number of states with maximum value: " + res.getNNZString());
			ddMatch = null;
			break;
		case COUNT:
			if (empty)
				d = 0;
			else {
				vals.filter(ddFilter);
				d = vals.getNNZ();
			}
			mainLog.println("\nFilter: count of states satisfying " + filter + ": " + (int) d);
			res = new StateValuesMTBDD(JDD.Constant(d), model);
			break;
		case SUM:
			d = empty ? 0 : vals.sumOverBDD(ddFilter);
			mainLog.println("\nFilter: sum over states satisfying " + filter + ": " + (int) d);
			res = new StateValuesMTBDD(JDD.Constant(d), model);
			break;
		case AVG:
			if (empty)
				throw new PrismException("Can't take an average over an empty filter");
			d = vals.sumOverBDD(ddFilter) / JDD.GetNumMinterms(ddFilter, allDDRowVars.n());
			mainLog.println("\nFilter: average over states satisfying " + filter + ": " + d);
			res = new StateValuesMTBDD(JDD.Constant(d), model);
			break;
		case FIRST:
			if (empty)
				throw new PrismException("Can't select the first value from an empty filter");
			d = vals.sumOverBDD(ddFilter) / JDD.GetNumMinterms(ddFilter, allDDRowVars.n());
			mainLog.println("\nFilter: value for first state satisfying " + filter + ": " + d);
			res = new StateValuesMTBDD(JDD.Constant(d), model);
			break;
		case FORALL:
			// Get access to BDD for this
			dd = vals.convertToStateValuesMTBDD().getJDDNode();
			// Set vals to null so that is not clear()-ed twice
			vals = null;
			// Check "for all" over filter
			JDD.Ref(ddFilter);
			dd = JDD.And(dd, ddFilter);
			b = dd.equals(ddFilter);
			JDD.Deref(dd);
			res = new StateValuesMTBDD(JDD.Constant(b ? 1.0 : 0.0), model);
			break;
		case EXISTS:
			// Get access to BDD for this
			dd = vals.convertToStateValuesMTBDD().getJDDNode();
			// Set vals to null so that is not clear()-ed twice
			vals = null;
			// Check "there exists" over filter
			JDD.Ref(ddFilter);
			dd = JDD.And(dd, ddFilter);
			b = !dd.equals(JDD.ZERO);
			JDD.Deref(dd);
			res = new StateValuesMTBDD(JDD.Constant(b ? 1.0 : 0.0), model);
			break;
		default:
			JDD.Deref(ddFilter);
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		// For some operators, print out some matching states
		if (ddMatch != null) {
			states = new StateListMTBDD(ddMatch, model);
			int numSat = states.size();
			mainLog.print("\nThere are ");
			mainLog.print(numSat == -1 ? ">" + Integer.MAX_VALUE : "" + numSat);
			mainLog.print(" states with (approximately) this value");
			if (!verbose && (numSat == -1 || numSat > 10)) {
				mainLog.print(".\nThe first 10 states are displayed below. To view them all, enable verbose mode or use filters.\n");
				states.print(mainLog, 10);
			} else {
				mainLog.print(":\n");
				states.print(mainLog);
			}
			JDD.Deref(ddMatch);
		}

		// Derefs, clears
		JDD.Deref(ddFilter);
		if (vals != null)
			vals.clear();

		return res;
	}
}

// ------------------------------------------------------------------------------
