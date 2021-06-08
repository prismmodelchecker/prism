//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package param;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import explicit.IndexedSet;
import explicit.StateStorage;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionUnaryOp;
import prism.ModelGeneratorSymbolic;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;

/**
 * Class to construct a parametric Markov model.
 */
public final class ModelBuilder extends PrismComponent
{
	/** mode (parametric / exact) */
	private final ParamMode mode;
	/** the ModelGeneratorSymbolic interface providing the model to be transformed to a {@code ParamModel} */
	private ModelGeneratorSymbolic modelGenSym;
	/** function factory used in the constructed parametric model */
	private FunctionFactory functionFactory;
	/** names of parameters */
	private String[] paramNames;
	/** lower bounds of parameters */
	private BigRational[] lower;
	/** upper bounds of parameters */
	private BigRational[] upper;
	/** name of function type to use, as read from PRISM settings */
	private String functionType;
	/** maximal error probability of DAG function representation */
	private double dagMaxError;

	/** local storage made static for use in anonymous class */
	private static Map<String,Expression> constExprs;
	
	/**
	 * Constructor
	 */
	public ModelBuilder(PrismComponent parent, ParamMode mode) throws PrismException
	{
		super(parent);
		this.mode = mode;
		// If present, initialise settings from PrismSettings
		if (settings != null) {
			functionType = settings.getString(PrismSettings.PRISM_PARAM_FUNCTION);
			dagMaxError = settings.getDouble(PrismSettings.PRISM_PARAM_DAG_MAX_ERROR);
		}
	}
	
	/**
	 * Transform PRISM expression to rational function.
	 * If successful, a function representing the given expression will be
	 * constructed. This is however not always possible, as not each PRISM
	 * expression can be represented as a rational function. In this case
	 * a {@code PrismException} will be thrown.
	 * 
	 * @param factory function factory used to construct function
	 * @param expr PRISM expression to transform to rational function
	 * @return rational function representing the given PRISM expression
	 * @throws PrismException thrown if {@code expr} cannot be represented as rational function
	 */
	public Function expr2function(FunctionFactory factory, Expression expr) throws PrismException
	{
		if (expr instanceof ExpressionLiteral) {
			String exprString = ((ExpressionLiteral) expr).getString();
			if (exprString == null || exprString.equals("")) {
				throw new PrismException("cannot convert from literal " + "for which no string is set");
			}
			return factory.fromBigRational(new BigRational(exprString));
		} else if (expr instanceof ExpressionConstant) {
			String exprString = ((ExpressionConstant) expr).getName();
			if (modelGenSym.getConstantValues().contains(exprString)) {
				Object val = modelGenSym.getConstantValues().getValueOf(exprString);
				return factory.fromBigRational(new BigRational(val.toString()));
			}
			Expression constExpr = modelGenSym.getUnknownConstantDefinition(exprString);
			if (constExpr == null) {
				return factory.getVar(exprString);
			} else {
				return expr2function(factory, constExpr);
			}
		} else if (expr instanceof ExpressionBinaryOp) {
			ExpressionBinaryOp binExpr = ((ExpressionBinaryOp) expr);
			Function f1 = expr2function(factory, binExpr.getOperand1());
			Function f2 = expr2function(factory, binExpr.getOperand2());
			switch (binExpr.getOperator()) {
			case ExpressionBinaryOp.PLUS:
				return f1.add(f2);
			case ExpressionBinaryOp.MINUS:
				return f1.subtract(f2);
			case ExpressionBinaryOp.TIMES:
				return f1.multiply(f2);
			case ExpressionBinaryOp.DIVIDE:
				return f1.divide(f2);
			default:
				throw new PrismNotSupportedException("For " + mode.engine() + ", analysis with rate/probability of " + expr + " not implemented");
			}
		} else if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp unExpr = ((ExpressionUnaryOp) expr);
			Function f = expr2function(factory, unExpr.getOperand());
			switch (unExpr.getOperator()) {
			case ExpressionUnaryOp.MINUS:
				return f.negate();
			case ExpressionUnaryOp.PARENTH:
				return f;
			default:
				throw new PrismNotSupportedException("For " + mode.engine() + ", analysis with rate/probability of " + expr + " not implemented");
			}
		} else if (expr instanceof ExpressionITE){
			ExpressionITE iteExpr = (ExpressionITE) expr;
			// ITE expressions where the if-expression does not
			// depend on a parametric constant are supported
			if (iteExpr.getOperand1().isConstant()) {
				try {
					// non-parametric constants and state variable values have
					// been already partially expanded, so if this evaluation
					// succeeds there are no parametric constants involved
					boolean ifValue = iteExpr.getOperand1().evaluateExact().toBoolean();
					if (ifValue) {
						return expr2function(factory, iteExpr.getOperand2());
					} else {
						return expr2function(factory, iteExpr.getOperand3());
					}
				} catch (PrismException e) {
					// Most likely, a parametric constant occurred.
					// Do nothing here, exception is thrown below
				}
			}
			throw new PrismNotSupportedException("For " + mode.engine() + ", analysis with rate/probability of " + expr + " not implemented");
		} else if (expr instanceof ExpressionFunc) {
			// functions (min, max, floor, ...) are supported if
			// they don't refer to parametric constants in their arguments
			// and can be exactly evaluated
			try {
				// non-parametric constants and state variable values have
				// been already partially expanded, so if this evaluation
				// succeeds there are no parametric constants involved
				BigRational value = expr.evaluateExact();
				return factory.fromBigRational(value);
			} catch (PrismException e) {
				// Most likely, a parametric constant occurred.
				throw new PrismNotSupportedException("For " + mode.engine() + ", analysis with rate/probability of " + expr + " not implemented");
			}
		} else {
			throw new PrismNotSupportedException("For " + mode.engine() + ", analysis with rate/probability of " + expr + " not implemented");
		}
	}

	/**
	 * Get the parameter names as a list of strings.
	 * @return the parameter names
	 */
	public List<String> getParameterNames()
	{
		return Arrays.asList(paramNames);
	}

	/**
	 * Construct a parametric model and return it.
	 * All of {@code paramNames}, {@code lower}, {@code} upper must have the same length,
	 * and {@code lower} bounds of parameters must not be higher than {@code upper} bounds.
	 * @param modelGenSym The ModelGeneratorSymbolic interface providing the model 
	 * @param paramNames names of parameters
	 * @param lowerStr lower bounds of parameters
	 * @param upperStr upper bounds of parameters
	 */
	public ParamModel constructModel(ModelGeneratorSymbolic modelGenSym, String[] paramNames, String[] lowerStr, String[] upperStr) throws PrismException
	{
		// No model construction for real-time, models
		if (modelGenSym.getModelType().realTime()) {
			throw new PrismNotSupportedException("For " + mode.engine() + ", you cannot build a " + modelGenSym.getModelType() + " model explicitly, only perform model checking");
		}

		// Store model generator and parameter info
		this.modelGenSym = modelGenSym;
		this.paramNames = paramNames;
		lower = new BigRational[lowerStr.length];
		upper = new BigRational[upperStr.length];
		for (int param = 0; param < lowerStr.length; param++) {
			lower[param] = new BigRational(lowerStr[param]);
			upper[param] = new BigRational(upperStr[param]);
		}
		
		// Create function factory
		if (functionType.equals("JAS")) {
			functionFactory = new JasFunctionFactory(paramNames, lower, upper);
		} else if (functionType.equals("JAS-cached")) {
			functionFactory = new CachedFunctionFactory(new JasFunctionFactory(paramNames, lower, upper));
		} else if (functionType.equals("DAG")) {
			functionFactory = new DagFunctionFactory(paramNames, lower, upper, dagMaxError, false);
		}
		// And pass it to the model generator
		modelGenSym.setSymbolic(this, functionFactory);
		
		// First, set values for any constants in the model
		// (but do this *symbolically* - partly because some constants are parameters and therefore unknown,
		// but also to keep values like 1/3 as expressions rather than being converted to doubles,
		// resulting in a loss of precision)
		/*ConstantList constantList = modulesFile.getConstantList();
		constExprs = constantList.evaluateConstantsPartially(modulesFile.getUndefinedConstantValues(), null);
		modulesFile = (ModulesFile) modulesFile.deepCopy();
		modulesFile = (ModulesFile) modulesFile.accept(new ASTTraverseModify()
		{
			public Object visit(ExpressionConstant e) throws PrismLangException
			{
				Expression expr = constExprs.get(e.getName());
				return (expr != null) ? expr.deepCopy() : e;
			}
		});*/
		
		// Build/return model
		mainLog.print("\nBuilding model (" + mode.engine() + ")...\n");
		long time = System.currentTimeMillis();
		ParamModel modelExpl = doModelConstruction(modelGenSym);
		time = System.currentTimeMillis() - time;
		mainLog.print("\n"+modelExpl.infoStringTable());
		mainLog.println("\nTime for model construction: " + time / 1000.0 + " seconds.");
		
		return modelExpl;
	}

	/**
	 * Reserves memory needed for parametric model and reserves necessary space.
	 * Afterwards, transition probabilities etc. can be added. 
	 * 
	 * @param modulesFile modules file of which to explore states
	 * @param model model in which to reserve memory
	 * @param modelType type of the model to construct
	 * @param engine the engine used to compute state successors, etc.
	 * @param states list of states to be filled by this method
	 * @throws PrismException thrown if problems in underlying methods occur
	 */
	private void reserveMemoryAndExploreStates(ModelGeneratorSymbolic modelGenSym, ParamModel model, StateStorage<State> states) throws PrismException
	{
		boolean isNonDet = modelGenSym.getModelType().nondeterministic();
		int numStates = 0;
		int numTotalChoices = 0;
		int numTotalSuccessors = 0;

		LinkedList<State> explore = new LinkedList<State>();

		State state = modelGenSym.getInitialState();
		states.add(state);
		explore.add(state);
		numStates++;

		while (!explore.isEmpty()) {
			state = explore.removeFirst();
			modelGenSym.exploreState(state);
			int numChoices = modelGenSym.getNumChoices();
			if (isNonDet) {
				numTotalChoices += numChoices;
			} else {
				numTotalChoices += 1;
			}
			for (int choiceNr = 0; choiceNr < numChoices; choiceNr++) {
				int numSuccessors = modelGenSym.getNumTransitions(choiceNr);
				numTotalSuccessors += numSuccessors;
				for (int succNr = 0; succNr < numSuccessors; succNr++) {
					State stateNew = modelGenSym.computeTransitionTarget(choiceNr, succNr); 
					if (states.add(stateNew)) {
						numStates++;
						explore.add(stateNew);
						states.add(stateNew);
					}
				}
			}
			if (numChoices == 0) {
				if (isNonDet) {
					numTotalChoices++;
				}
				numTotalSuccessors++;
			}
		}

		model.reserveMem(numStates, numTotalChoices, numTotalSuccessors);
	}

	/**
	 * Construct model once function factory etc. has been allocated.
	 * 
	 * @param modulesFile modules file of which to construct parametric model
	 * @return parametric model constructed
	 * @throws PrismException thrown if model cannot be constructed
	 */
	private ParamModel doModelConstruction(ModelGeneratorSymbolic modelGenSym) throws PrismException
	{
		final ModelType modelType;

		if (!modelGenSym.hasSingleInitialState()) {
			throw new PrismNotSupportedException("For " + mode.engine() + ", cannot do explicit-state reachability if there are multiple initial states");
		}

		boolean doProbChecks = getSettings().getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS);

		mainLog.print("\nComputing reachable states...");
		mainLog.flush();
		long timer = System.currentTimeMillis();
		modelType = modelGenSym.getModelType();
		ParamModel model = new ParamModel();
		model.setModelType(modelType);
		if (modelType != ModelType.DTMC && modelType != ModelType.CTMC && modelType != ModelType.MDP) {
			throw new PrismNotSupportedException("For " + mode.engine() + ", unsupported model type: " + modelType);
		}
		// need? SymbolicEngine engine = new SymbolicEngine(modulesFile, this, functionFactory);

		boolean isNonDet = modelType == ModelType.MDP;
		boolean isContinuous = modelType == ModelType.CTMC;
		StateStorage<State> states = new IndexedSet<State>(true);
		reserveMemoryAndExploreStates(modelGenSym, model, states);
		int[] permut = states.buildSortingPermutation();
		List<State> statesList = states.toPermutedArrayList(permut);
		model.setStatesList(statesList);
		model.addInitialState(permut[0]);
		int stateNr = 0;
		for (State state : statesList) {
			modelGenSym.exploreState(state);
			int numChoices = modelGenSym.getNumChoices();
			boolean computeSumOut = !isNonDet;
			boolean checkChoiceSumEqualsOne = doProbChecks && model.getModelType().choicesSumToOne();

			// sumOut = the sum over all outgoing choices from this state
			Function sumOut = functionFactory.getZero();
			for (int choiceNr = 0; choiceNr < numChoices; choiceNr++) {
				int numSuccessors = modelGenSym.getNumTransitions(choiceNr);

				// sumOutForChoice = the sum over all outgoing transitions for this choice
				Function sumOutForChoice = functionFactory.getZero();
				for (int succNr = 0; succNr < numSuccessors; succNr++) {
					Function probFunc = modelGenSym.getTransitionProbabilityFunction(choiceNr, succNr);
					if (computeSumOut)
						sumOut = sumOut.add(probFunc);
					if (checkChoiceSumEqualsOne)
						sumOutForChoice = sumOutForChoice.add(probFunc);
				}
				if (checkChoiceSumEqualsOne) {
					if (!sumOutForChoice.equals(functionFactory.getOne())) {
						if (sumOutForChoice.isConstant()) {
							// as the sum is constant, we know that it is really not 1
							throw new PrismLangException("Probabilities sum to " + sumOutForChoice.asBigRational() + " instead of 1 in state "
									+ state.toString(modelGenSym) + " for some command");
						} else {
							throw new PrismLangException("In state " + state.toString(modelGenSym) + " the probabilities sum to "
									+ sumOutForChoice + " for some command, which can not be determined to be equal to 1 (to ignore, use -noprobchecks option)");
						}
					}
				}
			}

			if (sumOut.isZero()) {
				// set sumOut to 1 for deadlock state or if we are in nonDet model
				sumOut = functionFactory.getOne();
			}
			for (int choiceNr = 0; choiceNr < numChoices; choiceNr++) {
				Object action = modelGenSym.getChoiceAction(choiceNr);
				int numSuccessors = modelGenSym.getNumTransitions(choiceNr);
				for (int succNr = 0; succNr < numSuccessors; succNr++) {
					State stateNew = modelGenSym.computeTransitionTarget(choiceNr, succNr);
					Function probFn = modelGenSym.getTransitionProbabilityFunction(choiceNr, succNr);

					if (modelType == ModelType.CTMC) {
						// check rate (non-normal?)
						if (probFn.isInf() || probFn.isMInf() || probFn.isNaN()) {
							throw new PrismException("For state " + state.toString(modelGenSym) + ", illegal rate " + probFn.asBigRational());
						}
						// check rate, if constant (negative?)
						if (probFn.isConstant() && probFn.asBigRational().signum() < 0) {
							throw new PrismException("For state " + state.toString(modelGenSym) + ", negative rate " + probFn.asBigRational());
						}
					}

					// divide by sumOut
					// for DTMC, this normalises over the choices
					// for CTMC this builds the embedded DTMC
					// for MDP this does nothing (sumOut is set to 1)
					probFn = probFn.divide(sumOut);

					if (modelType == ModelType.DTMC || modelType == ModelType.MDP) {
						// check probability (non-normal?)
						if (probFn.isInf() || probFn.isMInf() || probFn.isNaN()) {
							throw new PrismException("For state " + state.toString(modelGenSym) + ", illegal probability " + probFn.asBigRational());
						}
						// check probability, if constant (negative?)
						if (probFn.isConstant() && probFn.asBigRational().signum() < 0) {
							throw new PrismException("For state " + state.toString(modelGenSym) + ", negative probability " + probFn.asBigRational());
						}
					}

					model.addTransition(permut[states.get(stateNew)], probFn, action == null ? "" : action.toString());
				}
				if (isNonDet) {
					model.setSumLeaving(isContinuous ? sumOut : functionFactory.getOne());
					model.finishChoice();
				}
			}
			if (numChoices == 0) {
				model.addDeadlockState(stateNr);
				model.addTransition(stateNr, functionFactory.getOne(), null);
				if (isNonDet) {
					model.setSumLeaving(isContinuous ? sumOut : functionFactory.getOne());
					model.finishChoice();
				}
			}
			if (!isNonDet) {
				model.setSumLeaving(isContinuous ? sumOut : functionFactory.getOne());
				model.finishChoice();
			}
			model.finishState();
			stateNr++;
		}
		model.setFunctionFactory(functionFactory);

		mainLog.println();
		
		mainLog.print("Reachable states exploration and model construction");
		mainLog.println(" done in " + ((System.currentTimeMillis() - timer) / 1000.0) + " secs.");

		return model;
	}
}
