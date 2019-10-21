//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

/*
 * TODO
 * - lumpers should start convert directly from ParamModel plus scheduler and rewards
 *   rather than from AlterablePMC as is done currently
 * - could print to log for which parameter values results will be valid
 * - could implement steady-state properties for models w.o.
 *   nondeterminism but not needed at least not for next paper
 * - could implement DAG-like functions + probabilistic equality
 *   - for each function num and den would each be pointers into DAG
 *   - then either exact equality (expensive)
 *   - or probabilistic equality (Schwartz-Zippel)
 * - also, DAG-like regexp representation possible
 * - for comparism with previous work, use 
 * - could implement other types of regions apart from boxes
 * - could later improve support for optimisation over parameters
 *   - using apache math commons
 *   - or ipopt (zip file with java support for linux, windows, mac os x exists)
 * - libraries used should be loaded by classloader to make easier to use in
 *   projects where we cannot use GPLed code (just delete library and that's it)
 * - could later add support for abstraction of functions
 * - could integrate in GUI (student project?)
 * - if time left, add JUnit tests at least for BigRational and maybe functions and regions
 *   basically for all classes where interface is more or less fixed
 * - could try to bind to Ginac for comparability, but probably not much difference
 * - should integrate binding to solvers (RAHD and the like) at some point
 */

package param;

import java.util.BitSet;
import java.util.List;

import param.Lumper.BisimType;
import param.StateEliminator.EliminationOrder;
import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionExists;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.ast.ExpressionForAll;
import parser.ast.ExpressionFormula;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionProp;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import parser.ast.RelOp;
import parser.ast.RewardStruct;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.type.TypePathBool;
import parser.type.TypePathDouble;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismSettings;
import prism.PrismNotSupportedException;
import prism.Result;
import edu.jas.kern.ComputerThreads;
import explicit.Model;

/**
 * Model checker for parametric Markov models.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final public class ParamModelChecker extends PrismComponent
{
	// Model file (for reward structures, etc.)
	private ModulesFile modulesFile = null;

	// Properties file (for labels, constants, etc.)
	private PropertiesFile propertiesFile = null;

	// Constants (extracted from model/properties)
	private Values constantValues;

	// The result of model checking will be stored here
	private Result result;
	
	// Flags/settings

	/** The mode (parametric or exact)? */
	private ParamMode mode;

	// Verbosity level
	private int verbosity = 0;
	
	private BigRational[] paramLower;
	private BigRational[] paramUpper;

	private FunctionFactory functionFactory;
	private RegionFactory regionFactory;
	private ConstraintChecker constraintChecker;
	private ValueComputer valueComputer;
	
	private BigRational precision;
	private int splitMethod;
	private EliminationOrder eliminationOrder;
	private int numRandomPoints;
	private Lumper.BisimType bisimType;
	private boolean simplifyRegions;

	private ModelBuilder modelBuilder;
	
	/**
	 * Constructor
	 */
	public ParamModelChecker(PrismComponent parent, ParamMode mode) throws PrismException
	{
		super(parent);
		this.mode = mode;

		// If present, initialise settings from PrismSettings
		if (settings != null) {
		verbosity = settings.getBoolean(PrismSettings.PRISM_VERBOSE) ? 10 : 1;
		precision = new BigRational(settings.getString(PrismSettings.PRISM_PARAM_PRECISION));
		String splitMethodString = settings.getString(PrismSettings.PRISM_PARAM_SPLIT);
		if (splitMethodString.equals("Longest")) {
			splitMethod = BoxRegion.SPLIT_LONGEST;
		} else if (splitMethodString.equals("All")) {
			splitMethod = BoxRegion.SPLIT_ALL;
		} else {
			throw new PrismException("unknown region splitting method " + splitMethodString);				
		}
		String eliminationOrderString = settings.getString(PrismSettings.PRISM_PARAM_ELIM_ORDER);
		if (eliminationOrderString.equals("Arbitrary")) {
			eliminationOrder = EliminationOrder.ARBITRARY;
		} else if (eliminationOrderString.equals("Forward")) {
			eliminationOrder = EliminationOrder.FORWARD;
		} else if (eliminationOrderString.equals("Forward-reversed")) {
			eliminationOrder = EliminationOrder.FORWARD_REVERSED;
		} else if (eliminationOrderString.equals("Backward")) {
			eliminationOrder = EliminationOrder.BACKWARD;
		} else if (eliminationOrderString.equals("Backward-reversed")) {
			eliminationOrder = EliminationOrder.BACKWARD_REVERSED;
		} else if (eliminationOrderString.equals("Random")) {
			eliminationOrder = EliminationOrder.RANDOM;
		} else {
			throw new PrismException("unknown state elimination order " + eliminationOrderString);				
		}
		numRandomPoints = settings.getInteger(PrismSettings.PRISM_PARAM_RANDOM_POINTS);
		String bisimTypeString = settings.getString(PrismSettings.PRISM_PARAM_BISIM);
		if (bisimTypeString.equals("Weak")) {
			bisimType = BisimType.WEAK;
		} else if (bisimTypeString.equals("Strong")) {
			bisimType = BisimType.STRONG;
		} else if (bisimTypeString.equals("None")) {
			bisimType = BisimType.NULL;
		} else {
			throw new PrismException("unknown bisimulation type " + bisimTypeString);							
		}
		simplifyRegions = settings.getBoolean(PrismSettings.PRISM_PARAM_SUBSUME_REGIONS);
		}
	}
	
	// Setters/getters

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

	public ParamMode getMode()
	{
		return mode;
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
		ParamModel paramModel = (ParamModel) model;
		functionFactory = paramModel.getFunctionFactory();
		constraintChecker = new ConstraintChecker(numRandomPoints);
		regionFactory = new BoxRegionFactory(functionFactory, constraintChecker, precision,
				model.getNumStates(), model.getFirstInitialState(), simplifyRegions, splitMethod);
		valueComputer = new ValueComputer(this, mode, paramModel, regionFactory, precision, eliminationOrder, bisimType);
		
		long timer = 0;
		
		// Remove labels from property, using combined label list (on a copy of the expression)
		// This is done now so that we can handle labels nested below operators that are not
		// handled natively by the model checker yet (just evaluate()ed in a loop).
		expr = (Expression) expr.deepCopy().expandLabels(propertiesFile.getCombinedLabelList());

		// Also evaluate/replace any constants
		//expr = (Expression) expr.replaceConstants(constantValues);

		// Wrap a filter round the property, if needed
		// (in order to extract the final result of model checking) 
		expr = ExpressionFilter.addDefaultFilterIfNeeded(expr, model.getNumInitialStates() == 1);

		// Do model checking and store result vector
		timer = System.currentTimeMillis();
		BitSet needStates = new BitSet(model.getNumStates());
		needStates.set(0, model.getNumStates());
		RegionValues vals = checkExpression(paramModel, expr, needStates);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		if (constraintChecker.unsoundCheckWasUsed()) {
			mainLog.printWarning("Computation of Boolean values / parameter regions used heuristic sampling, results are potentially inaccurate.");
		}

		// Store result
		result = new Result();
		vals.clearExceptInit();
		result.setResult(new ParamResult(mode, vals, modelBuilder, functionFactory));
		
		/* // Output plot to tex file
		if (paramLower.length == 2) {
			try {
				FileOutputStream file = new FileOutputStream("out.tex");
				ResultExporter printer = new ResultExporter();
				printer.setOutputStream(file);
				printer.setRegionValues(vals);
				printer.setPointsPerDimension(19);
				printer.print();
				file.close();
			} catch (Exception e) {
				throw new PrismException("file could not be written");
			}
		}*/
		
		return result;
	}
	
	private int parserBinaryOpToRegionOp(int parserOp) throws PrismException
	{
		int regionOp;
		switch (parserOp) {
		case ExpressionBinaryOp.IMPLIES:
			regionOp = Region.IMPLIES;
			break;
		case ExpressionBinaryOp.IFF:
			regionOp = Region.IMPLIES;
			break;
		case ExpressionBinaryOp.OR:
			regionOp = Region.OR;
			break;
		case ExpressionBinaryOp.AND:
			regionOp = Region.AND;
			break;
		case ExpressionBinaryOp.EQ:
			regionOp = Region.EQ;
			break;
		case ExpressionBinaryOp.NE:
			regionOp = Region.NE;
			break;
		case ExpressionBinaryOp.GT:
			regionOp = Region.GT;
			break;
		case ExpressionBinaryOp.GE:
			regionOp = Region.GE;
			break;
		case ExpressionBinaryOp.LT:
			regionOp = Region.LT;
			break;
		case ExpressionBinaryOp.LE:
			regionOp = Region.LE;
			break;
		case ExpressionBinaryOp.PLUS:
			regionOp = Region.PLUS;
			break;
		case ExpressionBinaryOp.MINUS:
			regionOp = Region.MINUS;
			break;
		case ExpressionBinaryOp.TIMES:
			regionOp = Region.TIMES;
			break;
		case ExpressionBinaryOp.DIVIDE:
			regionOp = Region.DIVIDE;
			break;
		default:
			throw new PrismNotSupportedException("operator \"" + ExpressionBinaryOp.opSymbols[parserOp]
					+ "\" not currently supported for " + mode + " analyses");
		}
		return regionOp;
	}

	private int parserUnaryOpToRegionOp(int parserOp) throws PrismException
	{
		int regionOp;
		switch (parserOp) {
		case ExpressionUnaryOp.MINUS:
			regionOp = Region.UMINUS;
			break;
		case ExpressionUnaryOp.NOT:
			regionOp = Region.NOT;
			break;
		case ExpressionUnaryOp.PARENTH:
			regionOp = Region.PARENTH;
			break;
		default:
			throw new PrismNotSupportedException("operator \"" + ExpressionBinaryOp.opSymbols[parserOp]
					+ "\" not currently supported for " + mode + " analyses");
		}
		return regionOp;
	}

	/**
	 * Model check an expression and return a vector result values over all states.
	 * Information about states and model constants should be attached to the model.
	 * For other required info (labels, reward structures, etc.), use the methods
	 * {@link #setModulesFile} and {@link #setPropertiesFile}
	 * to attach the original model/properties files.
	 */
	RegionValues checkExpression(ParamModel model, Expression expr, BitSet needStates) throws PrismException
	{
		RegionValues res;
		if (expr instanceof ExpressionUnaryOp) {
			res = checkExpressionUnaryOp(model, (ExpressionUnaryOp) expr, needStates);
		} else if (expr instanceof ExpressionBinaryOp) {
			res = checkExpressionBinaryOp(model, (ExpressionBinaryOp) expr, needStates);
		} else if (expr instanceof ExpressionITE) {
			res = checkExpressionITE(model, (ExpressionITE) expr, needStates);
		} else if (expr instanceof ExpressionLabel) {
			res = checkExpressionLabel(model, (ExpressionLabel) expr, needStates);
		} else if (expr instanceof ExpressionFormula) {
			// This should have been defined or expanded by now.
			if (((ExpressionFormula) expr).getDefinition() != null)
				res = checkExpression(model, ((ExpressionFormula) expr).getDefinition(), needStates);
			else
				throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula) expr).getName() + "\"");
		} else if (expr instanceof ExpressionProp) {
			res = checkExpressionProp(model, (ExpressionProp) expr, needStates);
		} else if (expr instanceof ExpressionFilter) {
			if (((ExpressionFilter) expr).isParam()) {
				res = checkExpressionFilterParam(model, (ExpressionFilter) expr, needStates);
			} else {
				res = checkExpressionFilter(model, (ExpressionFilter) expr, needStates);
			}
		} else if (expr instanceof ExpressionProb) {
			res = checkExpressionProb(model, (ExpressionProb) expr, needStates);
		} else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward(model, (ExpressionReward) expr, needStates);
		} else if (expr instanceof ExpressionSS) {
			res = checkExpressionSteadyState(model, (ExpressionSS) expr, needStates);
		} else if (expr instanceof ExpressionForAll || expr instanceof ExpressionExists) {
			throw new PrismNotSupportedException("Non-probabilistic CTL model checking is currently not supported in the " + mode.engine());
		} else if (expr instanceof ExpressionFunc && ((ExpressionFunc)expr).getNameCode() == ExpressionFunc.MULTI) {
			throw new PrismNotSupportedException("Multi-objective model checking is not supported in the " + mode.engine());
		} else {
			res = checkExpressionAtomic(model, expr, needStates);
		}
		return res;
	}

	private RegionValues checkExpressionAtomic(ParamModel model, Expression expr, BitSet needStates) throws PrismException
	{
		expr = (Expression) expr.deepCopy().replaceConstants(constantValues);
		
		int numStates = model.getNumStates();
		List<State> statesList = model.getStatesList();
		StateValues stateValues = new StateValues(numStates, model.getFirstInitialState());
		int[] varMap = new int[statesList.get(0).varValues.length];
		for (int var = 0; var < varMap.length; var++) {
			varMap[var] = var;
		}
		for (int state = 0; state < numStates; state++) {
			Expression exprVar = (Expression) expr.deepCopy().evaluatePartially(statesList.get(state), varMap);
			if (needStates.get(state)) {
				if (exprVar instanceof ExpressionLiteral) {
					ExpressionLiteral exprLit = (ExpressionLiteral) exprVar;
					if (exprLit.getType() instanceof TypeBool) {
						stateValues.setStateValue(state, exprLit.evaluateBoolean());
					} else if (exprLit.getType() instanceof TypeInt || exprLit.getType() instanceof TypeDouble) {
						String exprStr = exprLit.getString();
						BigRational exprRat = new BigRational(exprStr);
						stateValues.setStateValue(state, functionFactory.fromBigRational(exprRat));
					} else {
						throw new PrismNotSupportedException("model checking expresssion " + expr + " not supported for " + mode + " models");
					}
				} else if (exprVar instanceof ExpressionConstant) {
					ExpressionConstant exprConst = (ExpressionConstant) exprVar;
					stateValues.setStateValue(state, functionFactory.getVar(exprConst.getName()));
				} else {
					throw new PrismNotSupportedException("cannot handle expression " + expr + " in " + mode + " analysis");
				}
			} else {
				if (exprVar.getType() instanceof TypeBool) {
					stateValues.setStateValue(state, false);
				} else {
					stateValues.setStateValue(state, functionFactory.getZero());						
				}
			}
		}	
		return regionFactory.completeCover(stateValues);
	}

	protected RegionValues checkExpressionUnaryOp(ParamModel model, ExpressionUnaryOp expr, BitSet needStates) throws PrismException
	{
		RegionValues resInner = checkExpression(model, expr.getOperand(), needStates);
		resInner.clearNotNeeded(needStates);

		return resInner.unaryOp(parserUnaryOpToRegionOp(expr.getOperator()));
	}

	/**
	 * Model check a binary operator.
	 */
	protected RegionValues checkExpressionBinaryOp(ParamModel model, ExpressionBinaryOp expr, BitSet needStates) throws PrismException
	{
		RegionValues res1 = checkExpression(model, expr.getOperand1(), needStates);
		RegionValues res2 = checkExpression(model, expr.getOperand2(), needStates);
		res1.clearNotNeeded(needStates);
		res2.clearNotNeeded(needStates);

		return res1.binaryOp(parserBinaryOpToRegionOp(expr.getOperator()), res2);
	}

	/**
	 * Model check an If-Then-Else operator.
	 */
	protected RegionValues checkExpressionITE(ParamModel model, ExpressionITE expr, BitSet needStates) throws PrismException
	{
		RegionValues resI = checkExpression(model, expr.getOperand1(), needStates);
		RegionValues resT = checkExpression(model, expr.getOperand2(), needStates);
		RegionValues resE = checkExpression(model, expr.getOperand3(), needStates);
		resI.clearNotNeeded(needStates);
		resT.clearNotNeeded(needStates);
		resE.clearNotNeeded(needStates);

		return resI.ITE(resT, resE);
	}

	/**
	 * Model check a label.
	 */
	protected RegionValues checkExpressionLabel(ParamModel model, ExpressionLabel expr, BitSet needStates) throws PrismException
	{
		LabelList ll;
		int i;
		
		// treat special cases
		if (expr.isDeadlockLabel()) {
			int numStates = model.getNumStates();
			StateValues stateValues = new StateValues(numStates, model.getFirstInitialState());
			for (i = 0; i < numStates; i++) {
				stateValues.setStateValue(i, model.isDeadlockState(i));
			}
			return regionFactory.completeCover(stateValues);
		} else if (expr.isInitLabel()) {
			int numStates = model.getNumStates();
			StateValues stateValues = new StateValues(numStates, model.getFirstInitialState());
			for (i = 0; i < numStates; i++) {
				stateValues.setStateValue(i, model.isInitialState(i));
			}
			return regionFactory.completeCover(stateValues);
		} else {
			ll = propertiesFile.getCombinedLabelList();
			i = ll.getLabelIndex(expr.getName());
			if (i == -1)
				throw new PrismException("Unknown label \"" + expr.getName() + "\" in property");
			// check recursively
			return checkExpression(model, ll.getLabel(i), needStates);
		}
	}

	// Check property ref

	protected RegionValues checkExpressionProp(ParamModel model, ExpressionProp expr, BitSet needStates) throws PrismException
	{
		// Look up property and check recursively
		Property prop = propertiesFile.lookUpPropertyObjectByName(expr.getName());
		if (prop != null) {
			mainLog.println("\nModel checking : " + prop);
			return checkExpression(model, prop.getExpression(), needStates);
		} else {
			throw new PrismException("Unknown property reference " + expr);
		}
	}

	// Check filter

	protected RegionValues checkExpressionFilter(ParamModel model, ExpressionFilter expr, BitSet needStates) throws PrismException
	{
		Expression filter = expr.getFilter();
		if (filter == null) {
			filter = Expression.True();
		}
		boolean filterTrue = Expression.isTrue(filter);
		
		BitSet needStatesInner = new BitSet(needStates.size());
		needStatesInner.set(0, needStates.size());
		RegionValues rvFilter = checkExpression(model, filter, needStatesInner);
		if (!rvFilter.parameterIndependent()) {
			throw new PrismException("currently, parameter-dependent filters are not supported");
		}
		BitSet bsFilter = rvFilter.getStateValues().toBitSet();
		// Check filter satisfied by exactly one state
		FilterOperator op = expr.getOperatorType();
		if (op == FilterOperator.STATE && bsFilter.cardinality() != 1) {
			String s = "Filter should be satisfied in exactly 1 state";
			s += " (but \"" + filter + "\" is true in " + bsFilter.cardinality() + " states)";
			throw new PrismException(s);
		}
		if (op == FilterOperator.FIRST) {
			// only first state is of interest
			bsFilter.clear(bsFilter.nextSetBit(0) + 1, bsFilter.length());
		}
		RegionValues vals = checkExpression(model, expr.getOperand(), bsFilter);

		// Check if filter state set is empty; we treat this as an error
		if (bsFilter.isEmpty()) {
			throw new PrismException("Filter satisfies no states");
		}
		
		// Remember whether filter is for the initial state and, if so, whether there's just one
		boolean filterInit = (filter instanceof ExpressionLabel && ((ExpressionLabel) filter).isInitLabel());
		// Print out number of states satisfying filter
		if (!filterInit) {
			mainLog.println("\nStates satisfying filter " + filter + ": " + bsFilter.cardinality());
		}
			
		// Compute result according to filter type
		RegionValues resVals = null;
		switch (op) {
		case PRINT:
		case PRINTALL:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(filterTrue ? ":" : " that are also in filter " + filter + ":");
			} else {
				mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
			}

			vals.printFiltered(mainLog, mode, expr.getType(), bsFilter,
				               model.getStatesList(),
				               op == FilterOperator.PRINT, // printSparse if PRINT
				               true,  // print state values
				               true); // print state index

			resVals = vals;
			break;
		case MIN:
		case MAX:
		case ARGMIN:
		case ARGMAX:
			throw new PrismNotSupportedException("operation not implemented for " + mode + " models");
		case COUNT:
			resVals = vals.op(Region.COUNT, bsFilter);
			break;
		case SUM:
			resVals = vals.op(Region.PLUS, bsFilter);
			break;
		case AVG:
			resVals = vals.op(Region.AVG, bsFilter);
			break;
		case FIRST:
			if (bsFilter.cardinality() < 1) {
				throw new PrismException("Filter should be satisfied in at least 1 state.");
			}
			resVals = vals.op(Region.FIRST, bsFilter);
			break;
		case RANGE:
			throw new PrismNotSupportedException("operation not implemented for " + mode + " models");
		case FORALL:
			resVals = vals.op(Region.FORALL, bsFilter);
			break;
		case EXISTS:
			resVals = vals.op(Region.EXISTS, bsFilter);
			break;
		case STATE:
			resVals = vals.op(Region.FIRST, bsFilter);
			break;
		default:
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		return resVals;
	}

	// check filter over parameters
	
	protected RegionValues checkExpressionFilterParam(ParamModel model, ExpressionFilter expr, BitSet needStates) throws PrismException
	{
		// Filter info
		Expression filter = expr.getFilter();
		// Create default filter (true) if none given
		if (filter == null) {
			filter = Expression.True();
		}
		RegionValues rvFilter = checkExpression(model, filter, needStates);
		RegionValues vals = checkExpression(model, expr.getOperand(), needStates);

		Optimiser opt = new Optimiser(vals, rvFilter, expr.getOperatorType() == FilterOperator.MIN);
		System.out.println("\n" + opt.optimise());
		
		return null;

		/*
		// Remember whether filter is for the initial state and, if so, whether there's just one
		filterInit = (filter instanceof ExpressionLabel && ((ExpressionLabel) filter).isInitLabel());
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
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Minimum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			// TODO: un-hard-code precision once RegionValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			break;
		case MAX:
			// Compute max
			// Store as object/vector
			resObj = vals.maxOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Maximum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			// TODO: un-hard-code precision once RegionValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			break;
		case ARGMIN:
			// Compute/display min
			resObj = vals.minOverBitSet(bsFilter);
			mainLog.print("\nMinimum value over " + filterStatesString + ": " + resObj);
			// Find states that (are close to) selected value
			// TODO: un-hard-code precision once RegionValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			// Store states in vector; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = RegionValues.createFromBitSet(bsMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with minimum value: " + bsMatch.cardinality());
			bsMatch = null;
			break;
		case ARGMAX:
			// Compute/display max
			resObj = vals.maxOverBitSet(bsFilter);
			mainLog.print("\nMaximum value over " + filterStatesString + ": " + resObj);
			// Find states that (are close to) selected value
			bsMatch = vals.getBitSetFromCloseValue(resObj, precision, false);
			bsMatch.and(bsFilter);
			// Store states in vector; for ARGMAX, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = RegionValues.createFromBitSet(bsMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with maximum value: " + bsMatch.cardinality());
			bsMatch = null;
			break;
		case COUNT:
			// Compute count
			int count = vals.countOverBitSet(bsFilter);
			// Store as object/vector
			resObj = new Integer(count);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = filterTrue ? "Count of satisfying states" : "Count of satisfying states also in filter";
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case SUM:
			// Compute sum
			// Store as object/vector
			resObj = vals.sumOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Sum over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case AVG:
			// Compute average
			// Store as object/vector
			resObj = vals.averageOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Average over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case FIRST:
			// Find first value
			resObj = vals.firstFromBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
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
			if(paras == null) {
				bs = vals.getBitSet();
				// Print some info to log
				mainLog.print("\nNumber of states satisfying " + expr.getOperand() + ": ");
				mainLog.print(bs.cardinality());
				mainLog.println(bs.cardinality() == model.getNumStates() ? " (all in model)" : "");
				// Check "for all" over filter
				b = vals.forallOverBitSet(bsFilter);
				// Store as object/vector
				resObj = new Boolean(b);
				resVals = new RegionValues(expr.getType(), resObj, model); 
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
			}
			break;
		case EXISTS:
			// Get access to BitSet for this
			bs = vals.getBitSet();
			// Check "there exists" over filter
			b = vals.existsOverBitSet(bsFilter);
			// Store as object/vector
			resObj = new Boolean(b);
			resVals = new RegionValues(expr.getType(), resObj, model); 
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
			if(paras == null) {
				// Check filter satisfied by exactly one state
				if (bsFilter.cardinality() != 1) {
					String s = "Filter should be satisfied in exactly 1 state";
					s += " (but \"" + filter + "\" is true in " + bsFilter.cardinality() + " states)";
					throw new PrismException(s);
				}
				// Find first (only) value
				// Store as object/vector
				resObj = vals.firstFromBitSet(bsFilter);
				resVals = new RegionValues(expr.getType(), resObj, model); 
				// Create explanation of result and print some details to log
				resultExpl = "Value in ";
				if (filterInit) {
					resultExpl += "the initial state";
				} else {
					resultExpl += "the filter state";
				}
				mainLog.println("\n" + resultExpl + ": " + resObj);
			}
			break;
		default:
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		// For some operators, print out some matching states
		if (bsMatch != null) {
			states = RegionValues.createFromBitSet(bsMatch, model);
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
		*/
	}
	
	/**
	 * Model check a P operator expression and return the values for all states.
	 */
	protected RegionValues checkExpressionProb(ParamModel model, ExpressionProb expr, BitSet needStates) throws PrismException
	{
		Expression pb; // Probability bound (expression)
		BigRational p = null; // Probability bound (actual value)
		//String relOp; // Relational operator
		//boolean min = false; // For nondeterministic models, are we finding min (true) or max (false) probs
		ModelType modelType = model.getModelType();
		RelOp relOp;
		boolean min = false;

		RegionValues probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateExact(constantValues);
			if (p.compareTo(0) == -1 || p.compareTo(1) == 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		// Compute probabilities
		if (!expr.getExpression().isSimplePathFormula()) {
			throw new PrismNotSupportedException(mode.Engine() + " does not yet handle LTL-style path formulas");
		}
		probs = checkProbPathFormulaSimple(model, expr.getExpression(), min, needStates);
		probs.clearNotNeeded(needStates);

		if (verbosity > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(probs);
		}
		// For =? properties, just return values
		if (pb == null) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			return probs.binaryOp(Region.getOp(relOp.toString()), p);
		}
	}
	
	private RegionValues checkProbPathFormulaSimple(ParamModel model, Expression expr, boolean min, BitSet needStates) throws PrismException
	{
		boolean negated = false;
		RegionValues probs = null;
		
		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);
		
		// Negation
		if (expr instanceof ExpressionUnaryOp &&
		    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
			negated = true;
			min = !min;
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}
			
		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				throw new PrismNotSupportedException("Next operator not supported by " + mode + " engine");
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				BitSet needStatesInner = new BitSet(model.getNumStates());
				needStatesInner.set(0, model.getNumStates());
				RegionValues b1 = checkExpression(model, exprTemp.getOperand1(), needStatesInner);
				RegionValues b2 = checkExpression(model, exprTemp.getOperand2(), needStatesInner);
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(model, b1, b2, min);
				} else {
					probs = checkProbUntil(model, b1, b2, min, needStates);
				}
			}
		}
		
		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		if (negated) {
			// Subtract from 1 for negation
			probs = probs.binaryOp(new BigRational(1, 1), parserBinaryOpToRegionOp(ExpressionBinaryOp.MINUS));
		}
		
		return probs;
	}

	private RegionValues checkProbUntil(ParamModel model, RegionValues b1, RegionValues b2, boolean min, BitSet needStates) throws PrismException {
		return valueComputer.computeUnbounded(b1, b2, min, null);
	}
		
	private RegionValues checkProbBoundedUntil(ParamModel model, RegionValues b1, RegionValues b2, boolean min) throws PrismException {
		ModelType modelType = model.getModelType();
		//RegionValues probs;
		switch (modelType) {
		case CTMC:
			throw new PrismNotSupportedException("Bounded until operator not supported by " + mode + " engine");
		case DTMC:
			throw new PrismNotSupportedException("Bounded until operator not supported by " + mode + " engine");
		case MDP:
			throw new PrismNotSupportedException("Bounded until operator not supported by " + mode + " engine");
		default:
			throw new PrismNotSupportedException("Cannot model check for a " + modelType);
		}

		//return probs;
	}

	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected RegionValues checkExpressionReward(ParamModel model, ExpressionReward expr, BitSet needStates) throws PrismException
	{
		Expression rb; // Reward bound (expression)
		BigRational r = null; // Reward bound (actual value)
		RegionValues rews = null;
		boolean min = false;

		// Get info from reward operator
		
		RewardStruct rewStruct = modulesFile.getRewardStruct(expr.getRewardStructIndexByIndexObject(modulesFile.getRewardStructNames(), constantValues));
		RelOp relOp = expr.getRelOp();
		rb = expr.getReward();
		if (rb != null) {
			r = rb.evaluateExact(constantValues);
			if (r.compareTo(0) == -1)
				throw new PrismException("Invalid reward bound " + r + " in R[] formula");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		ParamRewardStruct rew = constructRewards(model, rewStruct, constantValues);
		mainLog.println("Building reward structure...");
		rews = checkRewardFormula(model, rew, expr.getExpression(), min, needStates);
		rews.clearNotNeeded(needStates);

		// Print out probabilities
		if (verbosity > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(rews);
		}

		// For =? properties, just return values
		if (rb == null) {
			return rews;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			return rews.binaryOp(Region.getOp(relOp.toString()), r);
		}
	}
	
	private RegionValues checkRewardFormula(ParamModel model,
			ParamRewardStruct rew, Expression expr, boolean min, BitSet needStates) throws PrismException {
		RegionValues rewards = null;

		if (expr.getType() instanceof TypePathDouble) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_S:
				rewards = checkRewardSteady(model, rew, exprTemp, min, needStates);				
				break;
			default:
				throw new PrismNotSupportedException(mode.Engine() + " does not yet handle the " + exprTemp.getOperatorSymbol() + " operator in the R operator");
			}
		} else if (expr.getType() instanceof TypePathBool || expr.getType() instanceof TypeBool) {
			rewards = checkRewardPathFormula(model, rew, expr, min, needStates);
		}

		if (rewards == null) {
			throw new PrismException("Unrecognised operator in R operator");
		}
		
		return rewards;
	}

	/**
	 * Compute rewards for a path formula in a reward operator.
	 */
	private RegionValues checkRewardPathFormula(ParamModel model, ParamRewardStruct rew, Expression expr, boolean min, BitSet needStates) throws PrismException
	{
		if (Expression.isReach(expr)) {
			return checkRewardReach(model, rew, (ExpressionTemporal) expr, min, needStates);
		} else if (Expression.isCoSafeLTLSyntactic(expr, true)) {
			throw new PrismNotSupportedException(mode.Engine() + " does not yet support co-safe reward computation");
		} else {
			throw new PrismException("R operator contains a path formula that is not syntactically co-safe: " + expr);
		}
	}

	private RegionValues checkRewardReach(ParamModel model,
			ParamRewardStruct rew, ExpressionTemporal expr, boolean min, BitSet needStates) throws PrismException {
		RegionValues allTrue = regionFactory.completeCover(true);
		BitSet needStatesInner = new BitSet(needStates.size());
		needStatesInner.set(0, needStates.size());
		RegionValues reachSet = checkExpression(model, expr.getOperand2(), needStatesInner);
		return valueComputer.computeUnbounded(allTrue, reachSet, min, rew);
	}
	
	private RegionValues checkRewardSteady(ParamModel model,
			ParamRewardStruct rew, ExpressionTemporal expr, boolean min, BitSet needStates) throws PrismException {
		if (model.getModelType() != ModelType.DTMC && model.getModelType() != ModelType.CTMC) {
			throw new PrismNotSupportedException(mode.Engine() + " long-run average rewards are only supported for DTMCs and CTMCs");
		}
		RegionValues allTrue = regionFactory.completeCover(true);
		BitSet needStatesInner = new BitSet(needStates.size());
		needStatesInner.set(0, needStates.size());
		return valueComputer.computeSteadyState(allTrue, min, rew);
	}

	private ParamRewardStruct constructRewards(ParamModel model, RewardStruct rewStruct, Values constantValues2)
			throws PrismException {
		int numStates = model.getNumStates();
		List<State> statesList = model.getStatesList();
		ParamRewardStruct rewSimple = new ParamRewardStruct(functionFactory, model.getNumChoices());
		int numRewItems = rewStruct.getNumItems();
		for (int rewItem = 0; rewItem < numRewItems; rewItem++) {
			Expression expr = rewStruct.getReward(rewItem);
			expr = (Expression) expr.deepCopy().replaceConstants(constantValues);
			Expression guard = rewStruct.getStates(rewItem);
			String action = rewStruct.getSynch(rewItem);
			boolean isTransitionReward = rewStruct.getRewardStructItem(rewItem).isTransitionReward();
			for (int state = 0; state < numStates; state++) {
				if (isTransitionReward && model.isDeadlockState(state)) {
					// As state is a deadlock state, any outgoing transition
					// was added to "fix" the deadlock and thus does not get a reward.
					// Skip to next state
					continue;
				}
				if (guard.evaluateExact(constantValues, statesList.get(state)).toBoolean()) {
					int[] varMap = new int[statesList.get(0).varValues.length];
					for (int i = 0; i < varMap.length; i++) {
						varMap[i] = i;
					}
					Expression exprState = (Expression) expr.deepCopy().evaluatePartially(statesList.get(state), varMap);
					Function newReward = modelBuilder.expr2function(functionFactory, exprState);
					for (int choice = model.stateBegin(state); choice < model.stateEnd(state); choice++) {
						Function sumOut = model.sumLeaving(choice);
						Function choiceReward;
						if (!isTransitionReward) {
							// for state reward, scale by sumOut
							// For DTMC/MDP, this changes nothing;
							// for CTMC this takes the expected duration
							// in this state into account
							choiceReward = newReward.divide(sumOut);
						} else {
							choiceReward = functionFactory.getZero();
							for (int succ = model.choiceBegin(choice); succ < model.choiceEnd(choice); succ++) {
								String mdpAction = model.getLabel(succ);
								if ((isTransitionReward && (mdpAction == null ? (action.isEmpty()) : mdpAction.equals(action)))) {
									choiceReward = choiceReward.add(newReward.multiply(model.succProb(succ)));
								}
							}
							// does not get scaled by sumOut
						}
						rewSimple.addReward(choice, choiceReward);
					}
				}
			}
		}
		return rewSimple;
	}
	
	/**
	 * Model check an S operator expression and return the values for all states.
	 */
	protected RegionValues checkExpressionSteadyState(ParamModel model, ExpressionSS expr, BitSet needStates) throws PrismException
	{
		Expression pb; // Probability bound (expression)
		BigRational p = null; // Probability bound (actual value)
		//String relOp; // Relational operator
		//boolean min = false; // For nondeterministic models, are we finding min (true) or max (false) probs
		ModelType modelType = model.getModelType();
		RelOp relOp;
		boolean min = false;

		RegionValues probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateExact(constantValues);
			if (p.compareTo(0) == -1 || p.compareTo(1) == 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		// Compute probabilities
		probs = checkProbSteadyState(model, expr.getExpression(), min, needStates);
		probs.clearNotNeeded(needStates);

		if (verbosity > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(probs);
		}
		// For =? properties, just return values
		if (pb == null) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			return probs.binaryOp(Region.getOp(relOp.toString()), p);
		}
	}

	private RegionValues checkProbSteadyState(ParamModel model, Expression expr, boolean min, BitSet needStates)
	throws PrismException
	{
		BitSet needStatesInner = new BitSet(model.getNumStates());
		needStatesInner.set(0, model.getNumStates());
		RegionValues b = checkExpression(model,expr, needStatesInner);
		if (model.getModelType() != ModelType.DTMC
				&& model.getModelType() != ModelType.CTMC) {
			throw new PrismNotSupportedException(mode.Engine() + " currently only implements steady state for DTMCs and CTMCs.");
		}
		return valueComputer.computeSteadyState(b, min, null);
	}

	/**
	 * Set parameters for parametric analysis.
	 * 
	 * @param paramNames names of parameters
	 * @param lower lower bounds of parameters
	 * @param upper upper bounds of parameters
	 */
	public void setParameters(String[] paramNames, String[] lower, String[] upper) {
		if (paramNames == null || lower == null || upper == null) {
			throw new IllegalArgumentException("all arguments of this functions must be non-null");
		}
		if (paramNames.length != lower.length || lower.length != upper.length) {
			throw new IllegalArgumentException("all arguments of this function must have the same length");
		}
		
		paramLower = new BigRational[lower.length];
		paramUpper = new BigRational[upper.length];
		
		for (int i = 0; i < paramNames.length; i++) {
			if (paramNames[i] == null || lower[i] == null || upper[i] == null)  {
				throw new IllegalArgumentException("all entries in arguments of this function must be non-null");
			}
			paramLower[i] = new BigRational(lower[i]);
			paramUpper[i] = new BigRational(upper[i]);
		}
	}	
			
	public static void closeDown() {
		ComputerThreads.terminate();
	}

	public void setModelBuilder(ModelBuilder builder)
	{
		this.modelBuilder = builder;
	}	
}
