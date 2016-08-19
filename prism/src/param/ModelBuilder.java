//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
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

package param;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import parser.State;
import parser.ast.ConstantList;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionUnaryOp;
import parser.ast.ModulesFile;
import parser.visitor.ASTTraverseModify;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import prism.PrismNotSupportedException;
import explicit.IndexedSet;
import explicit.StateStorage;

/**
 * Class to construct a parametric Markov model.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see ParamModel
 */
public final class ModelBuilder extends PrismComponent
{
	/** {@code ModulesFile} to be transformed to a {@code ParamModel} */
	private ModulesFile modulesFile;
	/** parametric model constructed from {@code modulesFile} */
	private ParamModel model;
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
	public ModelBuilder(PrismComponent parent) throws PrismException
	{
		super(parent);
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
	Function expr2function(FunctionFactory factory, Expression expr) throws PrismException
	{
		if (expr instanceof ExpressionLiteral) {
			String exprString = ((ExpressionLiteral) expr).getString();
			if (exprString == null || exprString.equals("")) {
				throw new PrismException("cannot convert from literal " + "for which no string is set");
			}
			return factory.fromBigRational(new BigRational(exprString));
		} else if (expr instanceof ExpressionConstant) {
			String exprString = ((ExpressionConstant) expr).getName();
			int index = modulesFile.getConstantList().getConstantIndex(exprString);
			if (index != -1) {
				Expression constExpr = modulesFile.getConstantList().getConstant(index);
				if (constExpr != null) {
					return expr2function(factory, constExpr);
				} else {
					return factory.getVar(exprString);
				}
			} else {
				throw new PrismException("Invalid parametric constant definition used");
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
				throw new PrismNotSupportedException("parametric analysis with rate/probability of " + expr + " not implemented");
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
				throw new PrismNotSupportedException("parametric analysis with rate/probability of " + expr + " not implemented");
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
					boolean ifValue = iteExpr.getOperand1().evaluateBoolean();
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
			throw new PrismNotSupportedException("parametric analysis with rate/probability of " + expr + " not implemented");
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
				throw new PrismNotSupportedException("parametric analysis with rate/probability of " + expr + " not implemented");
			}
		} else {
			throw new PrismNotSupportedException("parametric analysis with rate/probability of " + expr + " not implemented");
		}
	}

	// setters and getters

	/**
	 * Set modules file to be transformed to parametric Markov model.
	 * 
	 * @param modulesFile modules file to be transformed to parametric Markov model
	 */
	public void setModulesFile(ModulesFile modulesFile)
	{
		this.modulesFile = modulesFile;
	}

	/**
	 * Set parameter informations.
	 * Obviously, all of {@code paramNames}, {@code lower}, {@code} upper
	 * must have the same length, and {@code lower} bounds of parameters must
	 * not be higher than {@code upper} bounds.
	 * 
	 * @param paramNames names of parameters
	 * @param lower lower bounds of parameters
	 * @param upper upper bounds of parameters
	 */
	public void setParameters(String[] paramNames, String[] lower, String[] upper)
	{
		this.paramNames = paramNames;
		this.lower = new BigRational[lower.length];
		this.upper = new BigRational[upper.length];
		for (int param = 0; param < lower.length; param++) {
			this.lower[param] = new BigRational(lower[param]);
			this.upper[param] = new BigRational(upper[param]);
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
	 * Construct parametric Markov model.
	 * For this to work, module file, PRISM log, etc. must have been set
	 * beforehand.
	 * 
	 * @throws PrismException in case the model cannot be constructed
	 */
	public void build() throws PrismException
	{
		if (functionType.equals("JAS")) {
			functionFactory = new JasFunctionFactory(paramNames, lower, upper);
		} else if (functionType.equals("JAS-cached")) {
			functionFactory = new CachedFunctionFactory(new JasFunctionFactory(paramNames, lower, upper));
		} else if (functionType.equals("DAG")) {
			functionFactory = new DagFunctionFactory(paramNames, lower, upper, dagMaxError, false);
		}
		long time;

		if (modulesFile.getModelType() == ModelType.PTA) {
			throw new PrismNotSupportedException("You cannot build a PTA model explicitly, only perform model checking");
		}

		mainLog.print("\nBuilding model...\n");

		// build model
		time = System.currentTimeMillis();
		// First, set values for any constants in the model
		// (but do this *symbolically* - partly because some constants are parameters and therefore unknown,
		// but also to keep values like 1/3 as expressions rather than being converted to doubles,
		// resulting in a loss of precision)
		ConstantList constantList = modulesFile.getConstantList();
		constExprs = constantList.evaluateConstantsPartially(modulesFile.getUndefinedConstantValues(), null);
		modulesFile = (ModulesFile) modulesFile.deepCopy();
		modulesFile = (ModulesFile) modulesFile.accept(new ASTTraverseModify()
		{
			public Object visit(ExpressionConstant e) throws PrismLangException
			{
				Expression expr = constExprs.get(e.getName());
				return (expr != null) ? expr.deepCopy() : e;
			}
		});
		ParamModel modelExpl = constructModel(modulesFile);
		time = System.currentTimeMillis() - time;

		mainLog.print("\n"+modelExpl.infoStringTable());
		
		mainLog.println("\nTime for model construction: " + time / 1000.0 + " seconds.");
		model = modelExpl;
	}

	/**
	 * Returns the constructed parametric Markov model.
	 * 
	 * @return constructed parametric Markov model
	 */
	public explicit.Model getModel()
	{
		return model;
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
	private void reserveMemoryAndExploreStates(ModulesFile modulesFile, ParamModel model, ModelType modelType, SymbolicEngine engine, StateStorage<State> states)
			throws PrismException
	{
		boolean isNonDet = modelType == ModelType.MDP;
		int numStates = 0;
		int numTotalChoices = 0;
		int numTotalSuccessors = 0;

		LinkedList<State> explore = new LinkedList<State>();

		State state = modulesFile.getDefaultInitialState();
		states.add(state);
		explore.add(state);
		numStates++;

		while (!explore.isEmpty()) {
			state = explore.removeFirst();
			TransitionList tranlist = engine.calculateTransitions(state, true);  // Suppress warnings
			int numChoices = tranlist.getNumChoices();
			if (isNonDet) {
				numTotalChoices += numChoices;
			} else {
				numTotalChoices += 1;
			}
			for (int choiceNr = 0; choiceNr < numChoices; choiceNr++) {
				int numSuccessors = tranlist.getChoice(choiceNr).size();
				numTotalSuccessors += numSuccessors;
				for (int succNr = 0; succNr < numSuccessors; succNr++) {
					State stateNew = tranlist.getChoice(choiceNr).computeTarget(succNr, state);
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
	private ParamModel constructModel(ModulesFile modulesFile) throws PrismException
	{
		ModelType modelType;

		if (modulesFile.getInitialStates() != null) {
			throw new PrismNotSupportedException("Cannot do explicit-state reachability if there are multiple initial states");
		}

		boolean doProbChecks = getSettings().getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS);

		mainLog.print("\nComputing reachable states...");
		mainLog.flush();
		long timer = System.currentTimeMillis();
		modelType = modulesFile.getModelType();
		ParamModel model = new ParamModel();
		model.setModelType(modelType);
		if (modelType != ModelType.DTMC && modelType != ModelType.CTMC && modelType != ModelType.MDP) {
			throw new PrismNotSupportedException("Unsupported model type: " + modelType);
		}
		SymbolicEngine engine = new SymbolicEngine(modulesFile, this, functionFactory);

		if (modulesFile.getInitialStates() != null) {
			throw new PrismNotSupportedException("Explicit model construction does not support multiple initial states");
		}

		boolean isNonDet = modelType == ModelType.MDP;
		boolean isContinuous = modelType == ModelType.CTMC;
		StateStorage<State> states = new IndexedSet<State>(true);
		reserveMemoryAndExploreStates(modulesFile, model, modelType, engine, states);
		int[] permut = states.buildSortingPermutation();
		List<State> statesList = states.toPermutedArrayList(permut);
		model.setStatesList(statesList);
		model.addInitialState(permut[0]);
		int stateNr = 0;
		for (State state : statesList) {
			TransitionList tranlist = engine.calculateTransitions(state, false);
			int numChoices = tranlist.getNumChoices();

			boolean computeSumOut = !isNonDet;
			boolean checkChoiceSumEqualsOne = doProbChecks && model.getModelType().choicesSumToOne();

			// sumOut = the sum over all outgoing choices from this state
			Function sumOut = functionFactory.getZero();
			for (int choiceNr = 0; choiceNr < numChoices; choiceNr++) {
				ChoiceListFlexi choice = tranlist.getChoice(choiceNr);
				int numSuccessors = choice.size();

				// sumOutForChoice = the sum over all outgoing transitions for this choice
				Function sumOutForChoice = functionFactory.getZero();
				for (int succNr = 0; succNr < numSuccessors; succNr++) {
					ChoiceListFlexi succ = tranlist.getChoice(choiceNr);
					Function probFunc = succ.getProbability(succNr);
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
									+ state.toString(modulesFile) + " for some command");
						} else {
							throw new PrismLangException("In state " + state.toString(modulesFile) + " the probabilities sum to "
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
				ChoiceListFlexi choice = tranlist.getChoice(choiceNr);
				int a = tranlist.getTransitionModuleOrActionIndex(tranlist.getTotalIndexOfTransition(choiceNr, 0));
				String action = a < 0 ? null : modulesFile.getSynch(a - 1);
				int numSuccessors = choice.size();
				for (int succNr = 0; succNr < numSuccessors; succNr++) {
					ChoiceListFlexi succ = tranlist.getChoice(choiceNr);
					State stateNew = succ.computeTarget(succNr, state);
					Function probFn = succ.getProbability(succNr);
					// divide by sumOut
					// for DTMC, this normalises over the choices
					// for CTMC this builds the embedded DTMC
					// for MDP this does nothing (sumOut is set to 1)
					probFn = probFn.divide(sumOut);
					model.addTransition(permut[states.get(stateNew)], probFn, action);
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
