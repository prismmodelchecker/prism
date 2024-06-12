package simulator;

import java.util.ArrayList;
import java.util.List;

import common.Interval;
import param.BigRational;
import param.Function;
import param.FunctionFactory;
import parser.EvaluateContext;
import parser.EvaluateContextState;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.ConstantList;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionLiteral;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.RewardStruct;
import parser.type.Type;
import parser.type.TypeClock;
import parser.visitor.ASTTraverseModify;
import prism.Evaluator;
import prism.ModelGenerator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.RewardGenerator;

public class ModulesFileModelGenerator<Value> implements ModelGenerator<Value>, RewardGenerator<Value>
{
	// Parent PrismComponent (logs, settings etc.)
	protected PrismComponent parent;
	
	// Evaluator for values/states
	protected Evaluator<Value> eval;
	
	// Interval evaluator for probability intervals, if needed
	protected Evaluator<Interval<Value>> evalInt;
	
	// Evaluation context for expressions
	protected EvaluateContextState ec;
	
	// PRISM model info
	/** The original modules file (might have unresolved constants) */
	protected ModulesFile originalModulesFile;
	/** The modules file used for generating (has no unresolved constants after {@code initialise}) */
	protected ModulesFile modulesFile;
	protected ModelType modelType;
	protected Values mfConstants;
	protected VarList varList;
	protected LabelList labelList;
	protected List<String> labelNames;
	
	// Model exploration info
	
	// State currently being explored
	protected State exploreState;
	
	// Updater object for model
	protected Updater<Value> updater;
	// List of currently available transitions
	protected TransitionList<Value> transitionList;
	// Has the transition list been built? 
	protected boolean transitionListBuilt;
	
	// List of currently available transitions (interval models)
	protected TransitionList<Interval<Value>> transitionListInt;
	// Updater object for model (interval models)
	protected Updater<Interval<Value>> updaterInt;
	// Has the transition list been built? (interval models)
	protected boolean transitionListIntBuilt;
	
	// Global clock invariant (conjunction of per-module invariants)
	protected Expression invariant;
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * This method assumes that doubles are used to represent probabilities (rather than, say, exact arithmetic).
	 * The method takes care of {@link Evaluator} creation so should be preferred to calling constructors directly.
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 * @param parent Parent, used e.g. for settings (can be null)
	 */
	public static ModulesFileModelGenerator<?> create(ModulesFile modulesFile, PrismComponent parent) throws PrismException
	{
		return create(modulesFile, false, parent);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * If {@code exact} is true, the ModelGenerator will use {@link BigRational}s not doubles for probabilities.
	 * The method takes care of {@link Evaluator} creation so should be preferred to calling constructors directly.
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 * @param exact Use exact arithmetic?
	 * @param parent Parent, used e.g. for settings (can be null)
	 */
	public static ModulesFileModelGenerator<?> create(ModulesFile modulesFile, boolean exact, PrismComponent parent) throws PrismException
	{
		return new ModulesFileModelGenerator<>(modulesFile, createEvaluator(modulesFile, exact), parent);
	}
	
	/**
	 * Helper function to create an Evaluator of the appropriate type.
	 */
	private static Evaluator<?> createEvaluator(ModulesFile modulesFile, boolean exact) throws PrismException
	{
		if (!exact) {
			return Evaluator.forDouble();
		} else {
			return Evaluator.forBigRational();
		}
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * This method builds a generator for a parametric model using the function factory provided.
	 * Use this method to guarantee getting a {@code ModulesFileModelGenerator<Function>}.
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 * @param functionFactory Factory for creating/manipulating rational functions 
	 * @param parent Parent, used e.g. for settings (can be null)
	 */
	public static ModulesFileModelGenerator<Function> createForRationalFunctions(ModulesFile modulesFile, FunctionFactory functionFactory, PrismComponent parent) throws PrismException
	{
		Evaluator<Function> eval = Evaluator.forRationalFunction(functionFactory);
		return new ModulesFileModelGenerator<>(modulesFile, eval, parent);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * This method assumes that doubles are used to represent probabilities (rather than, say, exact arithmetic).
	 * Use this method to guarantee getting a {@code ModulesFileModelGenerator<Double>}.
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 * @param parent Parent, used e.g. for settings (can be null)
	 */
	public static ModulesFileModelGenerator<Double> createForDoubles(ModulesFile modulesFile, PrismComponent parent) throws PrismException
	{
		Evaluator<Double> eval = Evaluator.forDouble();
		return new ModulesFileModelGenerator<>(modulesFile, eval, parent);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * This constructor assumes that doubles are used to represent probabilities (rather than, say, exact arithmetic).
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 */
	public ModulesFileModelGenerator(ModulesFile modulesFile) throws PrismException
	{
		this(modulesFile, (PrismComponent) null);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * This constructor assumes that doubles are used to represent probabilities (rather than, say, exact arithmetic).
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 * @param parent Parent, used e.g. for settings (can be null)
	 */
	@SuppressWarnings("unchecked")
	public ModulesFileModelGenerator(ModulesFile modulesFile, PrismComponent parent) throws PrismException
	{
		this(modulesFile, (Evaluator<Value>) Evaluator.forDouble(), parent);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a {@link ModulesFile} instance.
	 * Takes in an {@link Evaluator}{@code <Value>} to match the type parameter {@code Value} of this class.
	 * Throw an explanatory exception if the model generator cannot be created.
	 * @param modulesFile The PRISM model
	 * @param eval Evaluator matching the type parameter {@code Value} of this class
	 * @param parent Parent, used e.g. for settings (can be null)
	 */
	public ModulesFileModelGenerator(ModulesFile modulesFile, Evaluator<Value> eval, PrismComponent parent) throws PrismException
	{
		this.parent = parent;
		this.eval = eval;
		if (modulesFile.getModelType().uncertain()) {
			evalInt = eval.createIntervalEvaluator();
		}
		
		// No support for system...endsystem yet
		if (modulesFile.getSystemDefn() != null) {
			throw new PrismException("The system...endsystem construct is not currently supported");
		}
		
		// Store basic model info
		this.modulesFile = modulesFile;
		this.originalModulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		
		// If there are no constants to define, go ahead and initialise;
		// Otherwise, setSomeUndefinedConstants needs to be called when the values are available  
		mfConstants = modulesFile.getConstantValues();
		if (mfConstants != null) {
			initialise();
		}
		
		// Create evaluate context for re-use
		ec = new EvaluateContextState(mfConstants, new State(modulesFile.getNumVars()));
		ec.setEvaluationMode(eval.evalMode());
	}
	
	/**
	 * (Re-)Initialise the class ready for model exploration
	 * (can only be done once any constants needed have been provided)
	 */
	private void initialise() throws PrismException
	{
		// Evaluate and replace constants on (a copy) of the modules file
		// We do this using a custom traversal, rather than just calling
		// replaceConstants() or evaluatePartially() because we also need
		// to expand undefined constants (e.g., for parametric model checking)
		modulesFile = (ModulesFile) modulesFile.deepCopy();
		ConstantList constantList = modulesFile.getConstantList();
		modulesFile = (ModulesFile) modulesFile.accept(new ASTTraverseModify()
		{
			public Object visit(ExpressionConstant e) throws PrismLangException
			{
				String name = e.getName();
				// Constants whose values have been fixed (directly or indirectly)
				// are replaced with constant literals
				int i = mfConstants.getIndexOf(name);
				if (i != -1) {
					return new ExpressionLiteral(e.getType(), mfConstants.getValue(i));
				}
				// Otherwise, see if there is definition (in terms of other constants)
				// in the model's constant list
				int i2 = constantList.getConstantIndex(e.getName());
				if (i2 != -1 && constantList.getConstant(i2) != null) {
					return constantList.getConstant(i2).accept(this);
				}
				// If not, the constant cannot be defined (might, for example,
				// be a parameter in parametric model checking). So leave unchanged.
				return e;
			}
			
		});
		// Optimise arithmetic expressions (not in exact mode: can create some round-off issues)
		if (!eval.exact()) {
			modulesFile = (ModulesFile) modulesFile.simplify();
		}
		// Get info
		varList = modulesFile.createVarList();
		labelList = modulesFile.getLabelList();
		labelNames = labelList.getLabelNames();
		
		// Create data structures for exploring model
		if (!modelType.uncertain()) {
			updater = new Updater<Value>(modulesFile, varList, eval, parent);
			transitionList = new TransitionList<Value>(eval);
		} else {
			updaterInt = new Updater<Interval<Value>>(modulesFile, varList, evalInt, parent);
			transitionListInt = new TransitionList<Interval<Value>>(evalInt);
		}
		transitionListBuilt = false;
		transitionListIntBuilt = false;
	}
	
	// Methods for ModelInfo interface
	
	@Override
	public ModelType getModelType()
	{
		return modelType;
	}

	@Override
	public void setSomeUndefinedConstants(EvaluateContext ecUndefined) throws PrismException
	{
		// We start again with a copy of the original modules file
		// and set the constants in the copy.
		// As {@code initialise()} can replace references to constants
		// with the concrete values in modulesFile, this ensures that we
		// start again at a place where references to constants have not
		// yet been replaced.
		modulesFile = (ModulesFile) originalModulesFile.deepCopy();
		modulesFile.setSomeUndefinedConstants(ecUndefined);
		mfConstants = modulesFile.getConstantValues();
		ec.setConstantValues(mfConstants);
		initialise();
	}
	
	@Override
	public Values getConstantValues()
	{
		return mfConstants;
	}
	
	@Override
	public boolean containsUnboundedVariables()
	{
		return modulesFile.containsUnboundedVariables();
	}
	
	@Override
	public int getNumVars()
	{
		return modulesFile.getNumVars();
	}
	
	@Override
	public List<String> getVarNames()
	{
		return modulesFile.getVarNames();
	}

	@Override
	public List<Type> getVarTypes()
	{
		return modulesFile.getVarTypes();
	}

	@Override
	public DeclarationType getVarDeclarationType(int i) throws PrismException
	{
		return modulesFile.getVarDeclarationType(i);
	}
	
	@Override
	public int getVarModuleIndex(int i)
	{
		return modulesFile.getVarModuleIndex(i);
	}
	
	@Override
	public String getModuleName(int i)
	{
		return modulesFile.getModuleName(i);
	}
	
	@Override
	public VarList createVarList() throws PrismException
	{
		return varList;
	}
	
	@Override
	public boolean isVarObservable(int i)
	{
		return modulesFile.isVarObservable(i);
	}
	
	@Override
	public int getNumLabels()
	{
		return labelList.size();	
	}

	@Override
	public String getActionStringDescription()
	{
		return "Module/[action]";
	}
	
	@Override
	public List<String> getLabelNames()
	{
		return labelNames;
	}
	
	@Override
	public String getLabelName(int i) throws PrismException
	{
		return labelList.getLabelName(i);
	}
	
	@Override
	public int getLabelIndex(String label)
	{
		return labelList.getLabelIndex(label);
	}
	
	@Override
	public List<String> getObservableNames()
	{
		return modulesFile.getObservableNames();
	}
	
	// Methods for ModelGenerator interface
	
	@Override
	public Evaluator<Value> getEvaluator()
	{
		return eval;
	}
	
	@Override
	public Evaluator<Interval<Value>> getIntervalEvaluator()
	{
		return evalInt;
	}
	
	@Override
	public boolean hasSingleInitialState() throws PrismException
	{
		return modulesFile.getInitialStates() == null;
	}
	
	@Override
	public State getInitialState() throws PrismException
	{
		if (modulesFile.getInitialStates() == null) {
			return modulesFile.getDefaultInitialState();
		} else {
			// Inefficient but probably won't be called
			return getInitialStates().get(0);
		}
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException
	{
		List<State> initStates = new ArrayList<State>();
		// Easy (normal) case: just one initial state
		if (modulesFile.getInitialStates() == null) {
			State state = modulesFile.getDefaultInitialState();
			initStates.add(state);
		}
		// Otherwise, there may be multiple initial states
		// For now, we handle this is in a very inefficient way
		else {
			Expression init = modulesFile.getInitialStates();
			List<State> allPossStates = varList.getAllStates();
			for (State possState : allPossStates) {
				if (init.evaluateBoolean(ec.setState(possState))) {
					initStates.add(possState);
				}
			}
		}
		return initStates;
	}

	@Override
	public void exploreState(State exploreState) throws PrismException
	{
		this.exploreState = exploreState;
		transitionListBuilt = false;
	}
	
	@Override
	public int getNumChoices() throws PrismException
	{
		return getTransitionList().getNumChoices();
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		return getTransitionList().getNumTransitions();
	}

	@Override
	public int getNumTransitions(int i) throws PrismException
	{
		return getTransitionList().getChoice(i).size();
	}

	@Override
	public int getChoiceIndexOfTransition(int index) throws PrismException
	{
		return getTransitionList().getChoiceIndexOfTransition(index);
	}
	
	@Override
	public int getChoiceOffsetOfTransition(int index) throws PrismException
	{
		return getTransitionList().getChoiceOffsetOfTransition(index);
	}
	
	@Override
	public int getTotalIndexOfTransition(int i, int offset) throws PrismException
	{
		return getTransitionList().getTotalIndexOfTransition(i, offset);
	}
	
	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		int a = transitions.getTransitionModuleOrActionIndex(transitions.getTotalIndexOfTransition(i, offset));
		return a < 0 ? null : modulesFile.getSynch(a - 1);
	}

	@Override
	public String getTransitionActionString(int i, int offset) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		int a = transitions.getTransitionModuleOrActionIndex(transitions.getTotalIndexOfTransition(i, offset));
		return getDescriptionForModuleOrActionIndex(a);
	}
	
	@Override
	public Object getChoiceAction(int index) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		int a = transitions.getChoiceModuleOrActionIndex(index);
		return a < 0 ? null : modulesFile.getSynch(a - 1);
	}

	@Override
	public String getChoiceActionString(int index) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		int a = transitions.getChoiceModuleOrActionIndex(index);
		return getDescriptionForModuleOrActionIndex(a);
	}

	/**
	 * Utility method to get a description for an action label:
	 * "[a]" for a synchronous action a and "M" for an unlabelled
	 * action belonging to a module M. Takes in an integer index:
	 * -i for independent in ith module, i for synchronous on ith action
	 * (in both cases, modules/actions are 1-indexed) 
	 */ 
	private String getDescriptionForModuleOrActionIndex(int a)
	{
		if (a < 0) {
			return modulesFile.getModuleName(-a - 1);
		} else if (a > 0) {
			return "[" + modulesFile.getSynchs().get(a - 1) + "]";
		} else {
			return "?";
		}
	}
	
	@Override
	public Expression getChoiceClockGuard(int i) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		return transitions.getChoice(i).getClockGuard();
	}
	
	@Override
	public Value getTransitionProbability(int i, int offset) throws PrismException
	{
		TransitionList<Value> transitions = getTransitionListScalars();
		if (transitions != null) {
			return transitions.getChoice(i).getProbability(offset);
		} else {
			throw new PrismException("Cannot get scalar transition probability for " + getModelType());
		}
	}

	@Override
	public Value getChoiceProbabilitySum(int i) throws PrismException
	{
		TransitionList<Value> transitions = getTransitionListScalars();
		if (transitions != null) {
			return transitions.getChoice(i).getProbabilitySum();
		} else {
			throw new PrismException("Cannot get scalar transition probability for " + getModelType());
		}
	}
	
	@Override
	public Value getProbabilitySum() throws PrismException
	{
		TransitionList<Value> transitions = getTransitionListScalars();
		if (transitions != null) {
			return transitions.getProbabilitySum();
		} else {
			throw new PrismException("Cannot get scalar transition probability for " + getModelType());
		}
	}
	
	@Override
	public Interval<Value> getTransitionProbabilityInterval(int i, int offset) throws PrismException
	{
		TransitionList<Interval<Value>> transitions = getTransitionListIntervals();
		if (transitions != null) {
			return transitions.getChoice(i).getProbability(offset);
		} else {
			throw new PrismException("Cannot get transition probability interval for " + getModelType());
		}
	}

	@Override
	public String getTransitionUpdateString(int i, int offset) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		return transitions.getTransitionUpdateString(transitions.getTotalIndexOfTransition(i, offset), exploreState);
	}
	
	@Override
	public String getTransitionUpdateStringFull(int i, int offset) throws PrismException
	{
		TransitionList<?> transitions = getTransitionList();
		return transitions.getTransitionUpdateStringFull(transitions.getTotalIndexOfTransition(i, offset));
	}
	
	@Override
	public State computeTransitionTarget(int index, int offset) throws PrismException
	{
		return getTransitionList().getChoice(index).computeTarget(offset, exploreState, varList);
	}

	@Override
	public boolean isLabelTrue(int i) throws PrismException
	{
		Expression expr = labelList.getLabel(i);
		return expr.evaluateBoolean(ec.setState(exploreState));
	}
	
	@Override
	public Expression getClockInvariant() throws PrismException
	{
		// Compute the conjunction of all per-module invariants, if not already done
		if (invariant == null) {
			int numModules = modulesFile.getNumModules();
			for (int m = 0; m < numModules; m++) {
				Expression invariantMod = modulesFile.getModule(m).getInvariant();
				if (invariantMod != null) {
					invariant = (invariant == null) ? invariantMod : Expression.And(invariant, invariantMod);
				}
			}
		}
		if (invariant == null) {
			return null;
		}
		// Replace non-clock variables with their values and simplify
		int numVars = varList.getNumVars();
		State stateNoClocks = new State(exploreState);
		for (int v = 0; v < numVars; v++) {
			if (varList.getType(v) instanceof TypeClock) {
				stateNoClocks.varValues[v] = null;
			}
		}
		return (Expression) invariant.deepCopy().evaluatePartially(ec.setState(stateNoClocks)).simplify();
	}
	
	@Override
	public State getObservation(State state) throws PrismException
	{
		if (!modelType.partiallyObservable()) {
			return null;
		}
		int numObservables = getNumObservables();
		State sObs = new State(numObservables);
		for (int i = 0; i < numObservables; i++) {
			Object oObs = modulesFile.getObservable(i).getDefinition().evaluate(ec.setState(state));
			sObs.setValue(i, oObs);
		}
		return sObs;
	}
	
	// Methods for RewardGenerator interface

	@Override
	public Evaluator<Value> getRewardEvaluator()
	{
		return eval;
	}
	
	@Override
	public List<String> getRewardStructNames()
	{
		return modulesFile.getRewardStructNames();
	}
	
	@Override
	public boolean rewardStructHasStateRewards(int i)
	{
		return modulesFile.rewardStructHasStateRewards(i);
	}
	
	@Override
	public boolean rewardStructHasTransitionRewards(int i)
	{
		return modulesFile.rewardStructHasTransitionRewards(i);
	}
	
	@Override
	public Value getStateReward(int r, State state) throws PrismException
	{
		RewardStruct rewStr = modulesFile.getRewardStruct(r);
		int n = rewStr.getNumItems();
		Value d = eval.zero();
		for (int i = 0; i < n; i++) {
			if (!rewStr.getRewardStructItem(i).isTransitionReward()) {
				Expression guard = rewStr.getStates(i);
				boolean guardSat = guard.evaluateBoolean(ec.setState(state));
				if (guardSat) {
					Value rew = eval.evaluate(rewStr.getReward(i), modulesFile.getConstantValues(), state);
					// Check reward is finite/non-negative (would be checked at model construction time,
					// but more fine grained error reporting can be done here)
					// Note use of original model since modulesFile may have been simplified
					if (!eval.isFinite(rew)) {
						throw new PrismLangException("Reward structure is not finite at state " + state, rewStr.getReward(i));
					}
					if (!eval.geq(rew, eval.zero())) {
						throw new PrismLangException("Reward structure is negative + (" + rew + ") at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
					}
					d = eval.add(d, rew);
				}
			}
		}
		return d;
	}

	@Override
	public Value getStateActionReward(int r, State state, Object action) throws PrismException
	{
		RewardStruct rewStr = modulesFile.getRewardStruct(r);
		int n = rewStr.getNumItems();
		Value d = eval.zero();
		for (int i = 0; i < n; i++) {
			if (rewStr.getRewardStructItem(i).isTransitionReward()) {
				Expression guard = rewStr.getStates(i);
				String cmdAction = rewStr.getSynch(i);
				if (action == null ? (cmdAction.isEmpty()) : action.equals(cmdAction)) {
					boolean guardSat = guard.evaluateBoolean(ec.setState(state));
					if (guardSat) {
						Value rew = eval.evaluate(rewStr.getReward(i), modulesFile.getConstantValues(), state);
						// Check reward is finite/non-negative (would be checked at model construction time,
						// but more fine grained error reporting can be done here)
						// Note use of original model since modulesFile may have been simplified
						if (!eval.isFinite(rew)) {
							throw new PrismLangException("Reward structure is not finite at state " + state, rewStr.getReward(i));
						}
						if (!eval.geq(rew, eval.zero())) {
							throw new PrismLangException("Reward structure is negative + (" + rew + ") at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
						}
						d = eval.add(d, rew);
					}
				}
			}
		}
		return d;
	}

	// Local utility methods
	
	/**
	 * Returns the current list of available transitions, generating it first if this has not yet been done.
	 */
	private TransitionList<?> getTransitionList() throws PrismException
	{
		return modelType.uncertain() ? getTransitionListIntervals() : getTransitionListScalars();
	}
	
	/**
	 * Returns the current list of available transitions, generating it first if this has not yet been done.
	 */
	private TransitionList<Value> getTransitionListScalars() throws PrismException
	{
		if (modelType.uncertain()) {
			return null;
		}
		// Compute the current transition list, if required
		if (!transitionListBuilt) {
			updater.calculateTransitions(exploreState, transitionList);
			transitionListBuilt = true;
		}
		return transitionList;
	}
	
	/**
	 * Returns the current list of available transitions (interval models), generating it first if this has not yet been done.
	 */
	private TransitionList<Interval<Value>> getTransitionListIntervals() throws PrismException
	{
		if (!modelType.uncertain()) {
			return null;
		}
		// Compute the current transition list, if required
		if (!transitionListBuilt) {
			updaterInt.calculateTransitions(exploreState, transitionListInt);
			transitionListIntBuilt = true;
		}
		return transitionListInt;
	}
}
