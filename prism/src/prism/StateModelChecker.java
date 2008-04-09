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

// Model checker for DTMCs

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
	protected JDDNode start;
	protected JDDNode reach;
	protected ODDNode odd;
	protected JDDVars allDDRowVars;
	protected JDDVars allDDColVars;
	protected JDDVars[] varDDRowVars;

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

	// -----------------------------------------------------------------------------------
	// Check a property, i.e. an expression
	// -----------------------------------------------------------------------------------

	public Result check(Expression expr) throws PrismException
	{
		return check(expr, null);
	}

	public Result check(Expression expr, Filter filter) throws PrismException
	{
		long timer = 0;
		JDDNode ddFilter = null, tmp;
		StateListMTBDD states;
		StateProbs vals;
		String resultString;
		double minRes = 0.0, maxRes = 0.0;
		Result res;

		// Filters are only allowed for non-Boolean properties
		if (expr.getType() == Expression.BOOLEAN && filter != null) {
			throw new PrismException("Filters cannot be applied to Boolean-valued properties");
		}

		// Translate filter (if present)
		if (filter != null) {
			ddFilter = checkExpressionDD(filter.getExpression());
			if (ddFilter.equals(JDD.ZERO)) {
				throw new PrismException("Filter " + filter + " satisfies no states");
			}
		}

		// Do model checking and store result
		timer = System.currentTimeMillis();
		vals = checkExpression(expr);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		// Process results of model checking - depends on type

		// Boolean results
		if (expr.getType() == Expression.BOOLEAN) {

			// Convert to StateList object
			states = new StateListMTBDD(((StateProbsMTBDD) vals).getJDDNode(), model);
			JDD.Ref(((StateProbsMTBDD) vals).getJDDNode());

			// See if satisfied in all/initial states
			boolean satAll = states.includesAll(reach);
			boolean satInit = states.includesAll(start);

			// Print number of satisfying states to log
			mainLog.print("\nNumber of satisfying states: ");
			mainLog.print(states.size());
			if (satAll) {
				mainLog.print(" (all)");
			} else if (satInit) {
				mainLog.print((model.getNumStartStates() == 1) ? " (including initial state)"
						: " (including all initial states)");
			} else {
				mainLog.print((model.getNumStartStates() == 1) ? " (initial state not satisfied)"
						: " (initial states not all satisfied)");
			}
			mainLog.print("\n");

			// If in "verbose" mode, print out satisfying states to log
			if (verbose) {
				mainLog.print("\nSatisfying states:");
				if (states.size() > 0) {
					mainLog.print("\n");
					states.print(mainLog);
				} else {
					mainLog.print(" (none)\n");
				}
			}

			// Result is true if all states satisfy, false otherwise
			resultString = satAll + " (property " + (satAll ? "" : "not ") + "satisfied in all states)";
			res = new Result(satAll ? new Boolean(true) : new Boolean(false), resultString);

			// Print result to log
			mainLog.print("\nResult: " + resultString + "\n");

			// Clean up
			states.clear();
		}

		// Non-Boolean results
		else {
			if (filter == null) {
				if (model.getNumStartStates() > 1) {
					mainLog.print("\nWarning: There are multiple initial states;");
					mainLog.print(" the result of model checking is for the first one: ");
					model.getStartStates().print(mainLog, 1);
				}
			} else {
				if (filter.minRequested()) {
					minRes = vals.minOverBDD(ddFilter);
					mainLog.print("\nMinimum value for states satisfying " + filter.getExpression() + ": " + minRes
							+ "\n");
					tmp = vals.getBDDFromInterval(minRes - termCritParam, minRes + termCritParam);
					JDD.Ref(ddFilter);
					tmp = JDD.And(tmp, ddFilter);
					states = new StateListMTBDD(tmp, model);
					mainLog.print("There are " + states.size() + " states with this minimum value (+/- "
							+ termCritParam + ")");
					if (!verbose && states.size() > 10) {
						mainLog
								.print(".\nThe first 10 states are displayed below. To view them all, use verbose mode.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					JDD.Deref(tmp);
				}
				if (filter.maxRequested()) {
					maxRes = vals.maxOverBDD(ddFilter);
					mainLog.print("\nMaximum value for states satisfying " + filter.getExpression() + ": " + maxRes
							+ "\n");
					tmp = vals.getBDDFromInterval(maxRes - termCritParam, maxRes + termCritParam);
					JDD.Ref(ddFilter);
					tmp = JDD.And(tmp, ddFilter);
					states = new StateListMTBDD(tmp, model);
					mainLog.print("There are " + states.size() + " states with this maximum value (+/- "
							+ termCritParam + ")");
					if (!verbose && states.size() > 10) {
						mainLog
								.print(".\nThe first 10 states are displayed below. To view them all, use verbose mode.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					JDD.Deref(tmp);
				}
			}
			if (filter != null && filter.minRequested())
				res = new Result(minRes);
			else if (filter != null && filter.maxRequested())
				res = new Result(maxRes);
			else
				res = new Result(vals.firstFromBDD(filter != null ? ddFilter : start));

			// Print result to log
			resultString = "Result";
			if (!("Result".equals(expr.getResultName())))
				resultString += " (" + expr.getResultName().toLowerCase() + ")";
			resultString += ": " + res;
			mainLog.print("\n" + resultString + "\n");
		}

		// Clean up
		if (ddFilter != null)
			JDD.Deref(ddFilter);
		vals.clear();

		// Return result
		return res;
	}

	// Check expression (recursive)

	public StateProbs checkExpression(Expression expr) throws PrismException
	{
		StateProbs res;

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
			// Should never happen
			throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula) expr).getName() + "\"");
		}
		// Variables
		else if (expr instanceof ExpressionVar) {
			res = checkExpressionVar((ExpressionVar) expr);
		}
		// Labels
		else if (expr instanceof ExpressionLabel) {
			res = checkExpressionLabel((ExpressionLabel) expr);
		} else {
			throw new PrismException("Couldn't check " + expr.getClass());
		}

		// Filter out non-reachable states from solution
		// (only necessary for symbolically stored vectors)
		if (res instanceof StateProbsMTBDD)
			res.filter(reach);

		return res;
	}
	
	// Check expression, convert to symbolic form (if not already), return BDD
	
	public JDDNode checkExpressionDD(Expression expr) throws PrismException
	{
		return checkExpression(expr).convertToStateProbsMTBDD().getJDDNode();
	}

	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	/*
	 * These check methods (and similar ones in subclasses of this class) return
	 * a StateProbs object which is a vector, over all states, of values. This can
	 * be represented either symbolically (as an (MT)BDD, encapsulated in a StateProbsMTBDD
	 * object) or explicitly (as an array of doubles, encapsulated in a StateProbsDV
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
	private StateProbs checkExpressionITE(ExpressionITE expr) throws PrismException
	{
		StateProbs res1 = null, res2 = null, res3 = null;
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
		dd1 = res1.convertToStateProbsMTBDD().getJDDNode();

		// If both operands 2/3 are symbolic, result will be symbolic
		if (res2 instanceof StateProbsMTBDD && res3 instanceof StateProbsMTBDD) {
			dd2 = ((StateProbsMTBDD) res2).getJDDNode();
			dd3 = ((StateProbsMTBDD) res3).getJDDNode();
			dd = JDD.ITE(dd1, dd2, dd3);
			return new StateProbsMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv2 = res2.convertToStateProbsDV().getDoubleVector();
			dv2.filter(dd1, allDDRowVars, odd);
			dv3 = res3.convertToStateProbsDV().getDoubleVector();
			dd1 = JDD.Not(dd1);
			dv3.filter(dd1, allDDRowVars, odd);
			dv2.add(dv3);
			dv3.clear();
			JDD.Deref(dd1);
			return new StateProbsDV(dv2, model);
		}
	}

	// Check a binary operator

	private StateProbs checkExpressionBinaryOp(ExpressionBinaryOp expr) throws PrismException
	{
		StateProbs res1 = null, res2 = null;
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
		if (res1 instanceof StateProbsMTBDD && res2 instanceof StateProbsMTBDD) {
			dd1 = ((StateProbsMTBDD) res1).getJDDNode();
			dd2 = ((StateProbsMTBDD) res2).getJDDNode();
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
			return new StateProbsMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv1 = res1.convertToStateProbsDV().getDoubleVector();
			dv2 = res2.convertToStateProbsDV().getDoubleVector();
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
			return new StateProbsDV(dv1, model);
		}
	}

	// Check a relational operator (=, !=, >, >=, < <=)

	private StateProbs checkExpressionRelOp(int op, Expression expr1, Expression expr2) throws PrismException
	{
		StateProbs res1 = null, res2 = null;
		JDDNode dd, dd1, dd2;
		String s;

		// Check for some easy (and common) special cases before resorting to
		// the general case

		// var relop int
		if (expr1 instanceof ExpressionVar && expr2.isConstant() && expr2.getType() == Expression.INT) {
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
			return new StateProbsMTBDD(dd, model);
		}
		// int relop var
		else if (expr1.isConstant() && expr1.getType() == Expression.INT && expr2 instanceof ExpressionVar) {
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
			return new StateProbsMTBDD(dd, model);
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
		dd1 = res1.convertToStateProbsMTBDD().getJDDNode();
		dd2 = res2.convertToStateProbsMTBDD().getJDDNode();
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
		return new StateProbsMTBDD(dd, model);
	}

	// Check a unary operator

	private StateProbs checkExpressionUnaryOp(ExpressionUnaryOp expr) throws PrismException
	{
		StateProbs res1 = null;
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
		if (res1 instanceof StateProbsMTBDD) {
			dd1 = ((StateProbsMTBDD) res1).getJDDNode();
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
			return new StateProbsMTBDD(dd, model);
		}
		// Otherwise result will be explicit
		else {
			dv1 = res1.convertToStateProbsDV().getDoubleVector();
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
			return new StateProbsDV(dv1, model);
		}
	}

	// Check a 'function'

	private StateProbs checkExpressionFunc(ExpressionFunc expr) throws PrismException
	{
		switch (expr.getNameCode()) {
		case ExpressionFunc.MIN:
			return checkExpressionFuncMinMax(expr, true);
		case ExpressionFunc.MAX:
			return checkExpressionFuncMinMax(expr, false);
		case ExpressionFunc.FLOOR:
			return checkExpressionFuncFloor(expr);
		case ExpressionFunc.CEIL:
			return checkExpressionFuncCeil(expr);
		case ExpressionFunc.POW:
			return checkExpressionFuncPow(expr);
		case ExpressionFunc.MOD:
			return checkExpressionFuncMod(expr);
		case ExpressionFunc.LOG:
			return checkExpressionFuncLog(expr);
		default:
			throw new PrismException("Unrecognised function \"" + expr.getName() + "\"");
		}
	}

	private StateProbs checkExpressionFuncMinMax(ExpressionFunc expr, boolean min) throws PrismException
	{
		StateProbs res1 = null, res2 = null;
		JDDNode dd1, dd2;
		DoubleVector dv1, dv2;
		int i, i2, n, n2;
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
		symbolic = (res1 instanceof StateProbsMTBDD);
		for (i = 1; i < n; i++) {
			try {
				res2 = checkExpression(expr.getOperand(i));
			} catch (PrismException e) {
				if (res2 != null)
					res2.clear();
				throw e;
			}
			// Explicit
			if (!symbolic || !(res2 instanceof StateProbsMTBDD)) {
				symbolic = false;
				dv1 = res1.convertToStateProbsDV().getDoubleVector();
				dv2 = res2.convertToStateProbsDV().getDoubleVector();
				n2 = dv1.getSize();
				for (i2 = 0; i2 < n2; i2++) {
					if (min) dv1.setElement(i2, Math.min(dv1.getElement(i2), dv2.getElement(i2)));
					else dv1.setElement(i2, Math.max(dv1.getElement(i2), dv2.getElement(i2)));
				}
				dv2.clear();
				res1 = new StateProbsDV(dv1, model);
			}
			// Symbolic
			else {
				dd1 = ((StateProbsMTBDD) res1).getJDDNode();
				dd2 = ((StateProbsMTBDD) res2).getJDDNode();
				dd1 = JDD.Apply(min ? JDD.MIN : JDD.MAX, dd1, dd2);
				res1 = new StateProbsMTBDD(dd1, model);
			}
		}

		return res1;
	}

	private StateProbs checkExpressionFuncFloor(ExpressionFunc expr) throws PrismException
	{
		StateProbs res1 = null;
		JDDNode dd1;
		DoubleVector dv1;
		int i, n;

		// Check operand recursively
		try {
			res1 = checkExpression(expr.getOperand(0));
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}
		// Symbolic
		if (res1 instanceof StateProbsMTBDD) {
			dd1 = ((StateProbsMTBDD) res1).getJDDNode();
			dd1 = JDD.MonadicApply(JDD.FLOOR, dd1);
			return new StateProbsMTBDD(dd1, model);
		}
		// Explicit
		else {
			dv1 = res1.convertToStateProbsDV().getDoubleVector();
			n = dv1.getSize();
			for (i = 0; i < n; i++)
				dv1.setElement(i, Math.floor(dv1.getElement(i)));
			return new StateProbsDV(dv1, model);
		}
	}

	private StateProbs checkExpressionFuncCeil(ExpressionFunc expr) throws PrismException
	{
		StateProbs res1 = null;
		JDDNode dd1;
		DoubleVector dv1;
		int i, n;

		// Check operand recursively
		try {
			res1 = checkExpression(expr.getOperand(0));
		} catch (PrismException e) {
			if (res1 != null)
				res1.clear();
			throw e;
		}
		// Symbolic
		if (res1 instanceof StateProbsMTBDD) {
			dd1 = ((StateProbsMTBDD) res1).getJDDNode();
			dd1 = JDD.MonadicApply(JDD.CEIL, dd1);
			return new StateProbsMTBDD(dd1, model);
		}
		// Explicit
		else {
			dv1 = res1.convertToStateProbsDV().getDoubleVector();
			n = dv1.getSize();
			for (i = 0; i < n; i++)
				dv1.setElement(i, Math.ceil(dv1.getElement(i)));
			return new StateProbsDV(dv1, model);
		}
	}

	private StateProbs checkExpressionFuncPow(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;

		dd1 = checkExpressionDD(expr.getOperand(0));
		dd2 = checkExpressionDD(expr.getOperand(1));
		dd = JDD.Apply(JDD.POW, dd1, dd2);

		return new StateProbsMTBDD(dd, model);
	}

	private StateProbs checkExpressionFuncMod(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;

		dd1 = checkExpressionDD(expr.getOperand(0));
		dd2 = checkExpressionDD(expr.getOperand(1));
		dd = JDD.Apply(JDD.MOD, dd1, dd2);

		return new StateProbsMTBDD(dd, model);
	}

	private StateProbs checkExpressionFuncLog(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;

		dd1 = checkExpressionDD(expr.getOperand(0));
		dd2 = checkExpressionDD(expr.getOperand(1));
		dd = JDD.Apply(JDD.LOGXY, dd1, dd2);

		return new StateProbsMTBDD(dd, model);
	}

	// Check a literal

	private StateProbs checkExpressionLiteral(ExpressionLiteral expr) throws PrismException
	{
		JDDNode dd;
		switch (expr.getType()) {
		case Expression.BOOLEAN:
			dd = JDD.Constant(expr.evaluateBoolean(null, null) ? 1.0 : 0.0);
			break;
		case Expression.INT:
			dd = JDD.Constant(expr.evaluateInt(null, null));
			break;
		case Expression.DOUBLE:
			dd = JDD.Constant(expr.evaluateDouble(null, null));
			break;
		default:
			throw new PrismException("Unknown literal type");
		}
		return new StateProbsMTBDD(dd, model);
	}

	// Check a constant

	private StateProbs checkExpressionConstant(ExpressionConstant expr) throws PrismException
	{
		int i;
		JDDNode dd;

		i = constantValues.getIndexOf(expr.getName());
		if (i == -1)
			throw new PrismException("Couldn't evaluate constant \"" + expr.getName() + "\"");
		switch (constantValues.getType(i)) {
		case Expression.INT:
			dd = JDD.Constant(constantValues.getIntValue(i));
			break;
		case Expression.DOUBLE:
			dd = JDD.Constant(constantValues.getDoubleValue(i));
			break;
		case Expression.BOOLEAN:
			dd = JDD.Constant(constantValues.getBooleanValue(i) ? 1.0 : 0.0);
			break;
		default:
			throw new PrismException("Unknown type for constant \"" + expr.getName() + "\"");
		}

		return new StateProbsMTBDD(dd, model);
	}

	// Check a variable reference

	private StateProbs checkExpressionVar(ExpressionVar expr) throws PrismException
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

		return new StateProbsMTBDD(dd, model);
	}

	// Check label

	private StateProbs checkExpressionLabel(ExpressionLabel expr) throws PrismException
	{
		LabelList ll;
		JDDNode dd;
		int i;

		// treat special cases
		if (expr.getName().equals("deadlock")) {
			dd = model.getFixedDeadlocks();
			JDD.Ref(dd);
			return new StateProbsMTBDD(dd, model);
		} else if (expr.getName().equals("init")) {
			dd = start;
			JDD.Ref(dd);
			return new StateProbsMTBDD(dd, model);
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
}

// ------------------------------------------------------------------------------
