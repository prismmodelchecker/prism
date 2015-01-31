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

	// The filter to be applied to the current property
	protected Filter currentFilter;

	// The result of model checking will be stored here
	protected Result result;

	// Options:

	// Which engine to use
	protected int engine;
	// Parameter for termination criterion
	protected double termCritParam;
	// Verbose mode?
	protected boolean verbose;
	// Store the final results vector after model checking?
	protected boolean storeVector = false; 
	// Generate/store a strategy during model checking?
	protected boolean genStrat = false;

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
		storeVector = prism.getStoreVector();
		genStrat = prism.getGenStrat();
	}

	/**
	 * Additional constructor for creating stripped down StateModelChecker for
	 * expression to MTBDD conversions.
	 */
	public StateModelChecker(Prism prism, VarList varList, JDDVars allDDRowVars, JDDVars[] varDDRowVars, Values constantValues) throws PrismException
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
		model = new ProbModel(JDD.Constant(0), JDD.Constant(0), new JDDNode[] {}, new JDDNode[] {}, null, allDDRowVars, new JDDVars(), null, 0, null, null,
				null, 0, varList, varDDRowVars, null, constantValues);
	}

	/**
	 * Create a model checker (a subclass of this one) for a given model type
	 */
	public static ModelChecker createModelChecker(ModelType modelType, Prism prism, Model model, PropertiesFile propertiesFile) throws PrismException
	{
		ModelChecker mc = null;
		switch (modelType) {
		case DTMC:
			mc = new ProbModelChecker(prism, model, propertiesFile);
			break;
		case MDP:
			mc = new NondetModelChecker(prism, model, propertiesFile);
			break;
		case CTMC:
			mc = new StochModelChecker(prism, model, propertiesFile);
			break;
		default:
			throw new PrismException("Cannot create model checker for model type " + modelType);
		}
		return mc;
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
		StateValues vals;
		String resultString;

		// Create storage for result
		result = new Result();

		// Remove any existing filter info
		currentFilter = null;

		// Wrap a filter round the property, if needed
		// (in order to extract the final result of model checking) 
		ExpressionFilter exprFilter = ExpressionFilter.addDefaultFilterIfNeeded(expr, model.getNumStartStates() == 1);
		// And if we need to store a copy of the results vector, make a note of this
		if (storeVector) {
			exprFilter.setStoreVector(true);
		}
		expr = exprFilter;
		
		// Do model checking and store result vector
		timer = System.currentTimeMillis();
		vals = checkExpression(expr);
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
		// Property refs
		else if (expr instanceof ExpressionProp) {
			res = checkExpressionProp((ExpressionProp) expr);
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
			case ExpressionBinaryOp.IFF:
				dd = JDD.Not(JDD.Xor(dd1, dd2));
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
			i = e2.evaluateInt(constantValues);
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
			i = e1.evaluateInt(constantValues);
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
		res1 = checkExpression(expr.getOperand());

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
		case ExpressionFunc.MULTI:
			throw new PrismException("Multi-objective model checking is not supported for " + model.getModelType() + "s");
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
		res1 = checkExpression(expr.getOperand(0));
		// Symbolic
		if (res1 instanceof StateValuesMTBDD) {
			dd1 = ((StateValuesMTBDD) res1).getJDDNode();
			switch (op) {
			case ExpressionFunc.FLOOR:
				// NB: Floor result kept as double, so don't need to check if operand is NaN
				dd1 = JDD.MonadicApply(JDD.FLOOR, dd1);
				break;
			case ExpressionFunc.CEIL:
				// NB: Ceil result kept as double, so don't need to check if operand is NaN
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
				// NB: Floor result kept as double, so don't need to check if operand is NaN
				for (i = 0; i < n; i++)
					dv1.setElement(i, Math.floor(dv1.getElement(i)));
				break;
			case ExpressionFunc.CEIL:
				// NB: Ceil result kept as double, so don't need to check if operand is NaN
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
			throw e;
		}
		// If both operands are symbolic, result will be symbolic
		if (res1 instanceof StateValuesMTBDD && res2 instanceof StateValuesMTBDD) {
			dd1 = ((StateValuesMTBDD) res1).getJDDNode();
			dd2 = ((StateValuesMTBDD) res2).getJDDNode();
			switch (op) {
			case ExpressionFunc.POW:
				// Deref dd1/dd2 because may still need below
				JDD.Ref(dd1);
				JDD.Ref(dd2);
				dd = JDD.Apply(JDD.POW, dd1, dd2);
				// Check for some possible problems in case of integer power
				// (denote error with NaN for states with problems)
				if (expr.getType() instanceof TypeInt) {
					// Negative exponent not allowed for integer power
					JDD.Ref(dd2);
					dd = JDD.ITE(JDD.LessThan(dd2, 0), JDD.Constant(0.0 / 0.0), dd);
					// Check for integer overflow 
					JDD.Ref(dd);
					dd = JDD.ITE(JDD.GreaterThan(dd, Integer.MAX_VALUE), JDD.Constant(0.0 / 0.0), dd);
				}
				// Late deref of dd1/dd2 because needed above
				JDD.Deref(dd1);
				JDD.Deref(dd2);
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
				// For integer power, have to check for errors and flag as NaN
				if (expr.getType() instanceof TypeInt) {
					double base, exp, pow;
					for (i = 0; i < n; i++) {
						base = dv1.getElement(i);
						exp = dv2.getElement(i);
						pow = Math.pow(base, exp);
						dv1.setElement(i, (exp < 0 || pow > Integer.MAX_VALUE) ? 0.0 / 0.0 : pow);
					}
				} else {
					for (i = 0; i < n; i++)
						dv1.setElement(i, Math.pow(dv1.getElement(i), dv2.getElement(i)));
				}
				break;
			case ExpressionFunc.MOD:
				for (i = 0; i < n; i++) {
					double div = (int) dv2.getElement(i);
					// Non-positive divisor not allowed (flag as NaN)
					d = (div <= 0) ? Double.NaN : (int) dv1.getElement(i) % (int) div;
					// Take care of negative case (% is remainder, not modulo)
					dv1.setElement(i, d < 0 ? d + div : d);
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
		res1 = checkExpression(expr.getOperand(0));
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
			dd = model.getDeadlocks();
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

	// Check property ref

	protected StateValues checkExpressionProp(ExpressionProp expr) throws PrismException
	{
		// Look up property and check recursively
		Property prop = propertiesFile.lookUpPropertyObjectByName(expr.getName());
		if (prop != null) {
			mainLog.println("\nModel checking : " + prop);
			return checkExpression(prop.getExpression());
		} else {
			throw new PrismException("Unknown property reference " + expr);
		}
	}

	// Check filter

	protected StateValues checkExpressionFilter(ExpressionFilter expr) throws PrismException
	{
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

		// For some types of filter, store info that may be used to optimise model checking
		op = expr.getOperatorType();
		if (op == FilterOperator.STATE) {
			currentFilter = new Filter(Filter.FilterOperator.STATE, ODDUtils.GetIndexOfFirstFromDD(ddFilter, odd, allDDRowVars));
		} else if (op == FilterOperator.FORALL && filterInit && filterInitSingle) {
			currentFilter = new Filter(Filter.FilterOperator.STATE, ODDUtils.GetIndexOfFirstFromDD(ddFilter, odd, allDDRowVars));
		} else if (op == FilterOperator.FIRST && filterInit && filterInitSingle) {
			currentFilter = new Filter(Filter.FilterOperator.STATE, ODDUtils.GetIndexOfFirstFromDD(ddFilter, odd, allDDRowVars));
		} else {
			currentFilter = null;
		}

		// Check operand recursively
		vals = checkExpression(expr.getOperand());

		// Print out number of states satisfying filter
		if (!filterInit)
			mainLog.println("\nStates satisfying filter " + filter + ": " + statesFilter.sizeString());

		// Compute result according to filter type
		op = expr.getOperatorType();
		switch (op) {
		case PRINT:
		case PRINTALL:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(filterTrue ? ":" : " that are also in filter " + filter + ":");
				dd = vals.deepCopy().convertToStateValuesMTBDD().getJDDNode();
				JDD.Ref(ddFilter);
				dd = JDD.And(dd, ddFilter);
				new StateListMTBDD(dd, model).print(mainLog);
				JDD.Deref(dd);
			} else {
				// TODO: integer-typed case: either add to print method or store in StateValues
				if (op == FilterOperator.PRINT) {
					mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
					vals.printFiltered(mainLog, ddFilter);
				} else {
					mainLog.println("\nResults (including zeros) for filter " + filter + ":");
					vals.printFiltered(mainLog, ddFilter, false, false, true);
				}
			}
			// Result vector is unchanged; for PRINT/PRINTALL, don't store a single value (in resObj)
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
			// Results of type void are handled differently
			if (expr.getType() instanceof TypeVoid) {
				// Extract result from StateValuesVoid object 
				resObj = ((StateValuesVoid) vals).getValue();
				// Leave result vector unchanged: for a range, result is only available from Result object
				resVals = vals;
				// Set vals to null to stop it being cleared below
				vals = null;
			} else {
				// Find first (only) value
				d = vals.firstFromBDD(ddFilter);
				// Store as object/vector
				if (expr.getType() instanceof TypeInt) {
					resObj = new Integer((int) d);
				} else if (expr.getType() instanceof TypeDouble) {
					resObj = new Double(d);
				} else if (expr.getType() instanceof TypeBool) {
					resObj = new Boolean(d > 0);
				} else {
					throw new PrismException("Don't know how to handle result of type " + expr.getType());
				}
				resVals = new StateValuesMTBDD(JDD.Constant(d), model);
			}
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
			mainLog.print((expr.getType() instanceof TypeDouble ? "(approximately) " : "") + "this value");
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
		// Store vector if requested (and if not, clear it)
		if (storeVector) {
			result.setVector(vals);
		} else if (vals != null) {
			vals.clear();
		}
		// Other derefs
		JDD.Deref(ddFilter);

		return resVals;
	}

	// Utility functions for symbolic model checkers 
	
	/**
	 * Get the state rewards (from a model) corresponding to the index of this R operator.
	 * Throws an exception (with explanatory message) if it cannot be found.
	 */
	public JDDNode getStateRewardsByIndexObject(Object rs, Model model, Values constantValues) throws PrismException
	{
		JDDNode stateRewards = null;
		if (model.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		if (rs == null) {
			stateRewards = model.getStateRewards(0);
		} else if (rs instanceof Expression) {
			int i = ((Expression) rs).evaluateInt(constantValues);
			rs = new Integer(i); // for better error reporting below
			stateRewards = model.getStateRewards(i - 1);
		} else if (rs instanceof String) {
			stateRewards = model.getStateRewards((String) rs);
		}
		if (stateRewards == null)
			throw new PrismException("Invalid reward structure index \"" + rs + "\"");
		return stateRewards; 
	}
	
	/**
	 * Get the transition rewards (from a model) corresponding to the index of this R operator.
	 * Throws an exception (with explanatory message) if it cannot be found.
	 */
	public JDDNode getTransitionRewardsByIndexObject(Object rs, Model model, Values constantValues) throws PrismException
	{
		JDDNode transRewards = null;
		if (model.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		if (rs == null) {
			transRewards = model.getTransRewards(0);
		} else if (rs instanceof Expression) {
			int i = ((Expression) rs).evaluateInt(constantValues);
			rs = new Integer(i); // for better error reporting below
			transRewards = model.getTransRewards(i - 1);
		} else if (rs instanceof String) {
			transRewards = model.getTransRewards((String) rs);
		}
		if (transRewards == null)
			throw new PrismException("Invalid reward structure index \"" + rs + "\"");
		return transRewards; 
	}

	/** Get the constant values (both from the modules file and the properties file) */
	@Override
	public Values getConstantValues()
	{
		return constantValues;
	}
}

// ------------------------------------------------------------------------------
