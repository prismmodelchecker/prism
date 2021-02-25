//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package simulator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.LabelList;
import parser.ast.PropertiesFile;
import parser.type.Type;
import prism.ModelGenerator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismUtils;
import prism.Result;
import prism.ResultsCollection;
import prism.RewardGenerator;
import prism.UndefinedConstants;
import simulator.method.SimulationMethod;
import simulator.sampler.Sampler;
import strat.Strategy;
import userinterface.graph.Graph;

/**
 * A discrete event simulation engine.
 * 
 * The SimulatorEngine class provides support for:
 * <UL>
 * <LI> Manual/random generation of paths through a model
 * <LI> Statistical model checking via Monte-Carlo sampling techniques
 * </UL>
 * 
 * After creating a SimulatorEngine object, you load a model with the methods:
 * <UL>
 * <LI> {@link #loadModel}
 * </UL>
 * The input to these methods is a model (ModelGenerator),
 * in which all constants have already been defined,
 * and a RewardGenerator for its reward definitions.
 * The latter can be omitted if not needed.
 * 
 * You can initialise creation of a path with:
 * <UL>
 * <LI> {@link #createNewPath} if you want to create a path that will be stored in full
 * <LI> {@link #createNewOnTheFlyPath} if you don't need the full path
 * </UL>
 * 
 * At this point, you can also load labels and properties into the simulator, whose values
 * will be available during path generation. Use:
 * <UL>
 * <LI> {@link #addLabel}
 * <LI> {@link #addProperty}
 * </UL>
 * 
 * To actually initialise the path with an initial state (or to reinitialise later) use:
 * <UL>
 * <LI> {@link #initialisePath}
 * </UL>
 * 
 * For statistical model checking, instead of path creation, use:
 * <UL>
 * <LI> {@link #isPropertyOKForSimulation}
 * <LI> {@link #checkPropertyForSimulation}
 * <LI> {@link #modelCheckSingleProperty}
 * <LI> {@link #modelCheckMultipleProperties}
 * <LI> {@link #modelCheckExperiment}
 * </UL>
 */
public class SimulatorEngine extends PrismComponent
{
	// The current parsed model + info
	private ModelGenerator modelGen;
	private RewardGenerator rewardGen;
	private ModelType modelType;
	// Variable info
	private VarList varList;
	private int numVars;
	// Constant definitions from model file
	private Values mfConstants;

	// Objects from model checking
	// Reachable states
	private List<State> reachableStates;
	// Strategy
	private Strategy strategy;

	// Labels + properties info
	protected List<Expression> labels;
	private List<Expression> properties;
	private List<Sampler> propertySamplers;

	// Current path info
	protected Path path;
	protected boolean onTheFly;
	// Current state (note: this info is duplicated in the path - it is always the same
	// as path.getCurrentState(); we maintain it separately for efficiency,
	// i.e. to avoid creating new State objects at every step)
	protected State currentState;
	
	// State for which transition list applies
	// (if null, just the default - i.e. the last state in the current path)
	protected State transitionListState;
	
	// Temporary storage for manipulating states/rewards
	protected double tmpStateRewards[];
	protected double tmpTransitionRewards[];

	// Random number generator
	private RandomNumberGenerator rng;

	/**
	 * Utility class to store a reference to a transition,
	 * broken up into the index of its (nondetermnistic) choice {@code i}
	 * and the index {@code offset} within transitions of the choice.
	 */
	public class Ref
	{
		public int i;
		public int offset;
	}

	// ------------------------------------------------------------------------------
	// Basic setup + model loading
	// ------------------------------------------------------------------------------

	/**
	 * Constructor for the simulator engine.
	 */
	public SimulatorEngine(PrismComponent parent)
	{
		super(parent);
		modelGen = null;
		modelType = null;
		varList = null;
		numVars = 0;
		mfConstants = null;
		labels = null;
		properties = null;
		propertySamplers = null;
		path = null;
		onTheFly = true;
		currentState = null;
		transitionListState = null;
		tmpStateRewards = null;
		tmpTransitionRewards = null;
		rng = new RandomNumberGenerator();
	}

	/**
	 * Loads a new model (and its rewards) into the simulator.
	 * Note: All constants in the model must have already been defined.
	 * @param modelGen The model generator for simulation
	 * @param rewardGen Reward generator for simulation (null if not needed)
	 */
	public void loadModel(ModelGenerator modelGen, RewardGenerator rewardGen) throws PrismException
	{
		// Create an empty RewardGenerator if missing
		if (rewardGen == null) {
			rewardGen = new RewardGenerator() {} ;
		}
		
		// Store model, some info and constants
		this.modelGen = modelGen;
		this.rewardGen = rewardGen;
		modelType = modelGen.getModelType();
		mfConstants = modelGen.getConstantValues();

		// Get variable list (symbol table) for model 
		varList = modelGen.createVarList();
		numVars = varList.getNumVars();
		
		// Initialise storage (should be re-done for each new path etc. but doesn't hurt)
		initialise();
	}

	/**
	 * Loads a new model (without any rewards) into the simulator.
	 * Note: All constants in the model must have already been defined.
	 * Typically, {@link #loadModel(ModelGenerator, RewardGenerator) is used.
	 * @param modelGen The model generator for simulation
	 */
	public void loadModel(ModelGenerator modelGen) throws PrismException
	{
		loadModel(modelGen, null);
	}
	
	/**
	 * Initialise: Set up (or reset) variables for simulation.
	 */
	private void initialise() throws PrismException
	{
		// Create state/transition/rewards storage
		currentState = new State(numVars);
		tmpStateRewards = new double[rewardGen.getNumRewardStructs()];
		tmpTransitionRewards = new double[rewardGen.getNumRewardStructs()];

		// Clear storage for strategy
		strategy = null;

		// Create storage for labels/properties
		labels = new ArrayList<Expression>();
		properties = new ArrayList<Expression>();
		propertySamplers = new ArrayList<Sampler>();
	}

	/**
	 * Get access to the currently loaded model
	 */
	public ModelGenerator getModel()
	{
		return modelGen;
	}
	
	/**
	 * Get access to the currently loaded reward generator
	 */
	public RewardGenerator getRewardGenerator()
	{
		return rewardGen;
	}
	
	// ------------------------------------------------------------------------------
	// Path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Create a new path for the current model.
	 */
	public void createNewPath() throws PrismException
	{
		// Reset simulation variables
		initialise();
		// Create empty (full) path object associated with this model
		path = new PathFull(modelGen, rewardGen);
		onTheFly = false;
	}

	/**
	 * Create a new on-the-fly path for the current model.
	 */
	public void createNewOnTheFlyPath() throws PrismException
	{
		// Reset simulation variables
		initialise();
		// Create empty (on-the-fly_ path object associated with this model
		path = new PathOnTheFly(modelGen, rewardGen);
		onTheFly = true;
	}

	/**
	 * Initialise (or re-initialise) the simulation path, starting with a specific (or random) initial state.
	 * @param initialState Initial state (if null, use default, selecting randomly if needed)
	 */
	public void initialisePath(State initialState) throws PrismException
	{
		// Store passed in state as current state
		if (initialState != null) {
			currentState.copy(initialState);
		}
		// Or pick default/random one
		else {
			if (modelGen.hasSingleInitialState()) {
				currentState.copy(modelGen.getInitialState());
			} else {
				throw new PrismNotSupportedException("Random choice of multiple initial states not yet supported");
			}
		}
		// Get initial observation
		State currentObs = modelGen.getObservation(currentState);
		// Get initial state reward
		calculateStateRewards(currentState, tmpStateRewards);
		// Initialise stored path
		path.initialise(currentState, currentObs, tmpStateRewards);
		// Explore initial state in model generator
		computeTransitionsForState(currentState);
		// Reset and then update samplers for any loaded properties
		resetSamplers();
		updateSamplers();
		// Initialise the strategy (if loaded)
		initialiseStrategy();
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. If this is a continuous-time model, the time to be spent
	 * in the state before leaving is picked randomly.
	 */
	public void manualTransition(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		if (modelType.continuousTime()) {
			double r = modelGen.getProbabilitySum();
			executeTimedTransition(i, offset, rng.randomExpDouble(r), index);
		} else {
			executeTransition(i, offset, index);
		}
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. In addition, specify the amount of time to be spent in
	 * the current state before this transition occurs.
	 * [continuous-time models only]
	 */
	public void manualTransition(int index, double time) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		executeTimedTransition(i, offset, time, index);
	}

	/**
	 * Select, at random, a transition from the current transition list and execute it.
	 * For continuous-time models, the time to be spent in the state before leaving is also picked randomly.
	 * If there is currently a deadlock, no transition is taken and the function returns false.
	 * Otherwise, the function returns true indicating that a transition was successfully taken. 
	 */
	public boolean automaticTransition() throws PrismException
	{

		// Check for deadlock; if so, stop and return false
		if (modelGen.getNumChoices() == 0)
			return false;
		//throw new PrismException("Deadlock found at state " + path.getCurrentState().toString(modelGen));

		Ref ref;
		double d, r;
		int i, j;
		switch (modelType) {
		case DTMC:
			// Pick a random number to determine choice/transition
			d = rng.randomUnifDouble();
			ref = new Ref();
			getChoiceIndexByProbabilitySum(d, ref);
			// Execute
			executeTransition(ref.i, ref.offset, -1);
			break;
		case MDP:
		case POMDP:
			// Pick a random choice
			i = rng.randomUnifInt(modelGen.getNumChoices());
			// Pick a random transition from this choice
			d = rng.randomUnifDouble();
			j = getTransitionIndexByProbabilitySum(i, d);
			// Execute
			executeTransition(i, j, -1);
			break;
		case CTMC:
			// Get sum of all rates
			r = modelGen.getProbabilitySum();
			// Pick a random number to determine choice/transition
			d = rng.randomUnifDouble(r);
			ref = new Ref();
			getChoiceIndexByProbabilitySum(d, ref);
			// Execute
			executeTimedTransition(ref.i, ref.offset, rng.randomExpDouble(r), -1);
			break;
		case LTS:
			// Pick a random choice
			i = rng.randomUnifInt(modelGen.getNumChoices());
			// Execute
			executeTransition(i, 0, -1);
			break;
		default:
			throw new PrismNotSupportedException(modelType + " not supported");
		}

		return true;
	}

	/**
	 * Get a reference to a transition according to a total probability (or rate) sum, x.
	 * i.e.the first transition for which the sum of probabilities/rates of that and all prior
	 * transitions (across all choices) exceeds x.
	 * @param x Probability (or rate) sum
	 * @param ref Empty transition reference to store result
	 */
	private void getChoiceIndexByProbabilitySum(double x, Ref ref) throws PrismException
	{
		int numChoices = modelGen.getNumChoices();
		double d = 0.0, tot = 0.0;
		int i = 0;
		// Add up choice prob/rate sums to find choice
		for (i = 0; x >= tot && i < numChoices; i++) {
			d = modelGen.getChoiceProbabilitySum(i);
			tot += d;
		}
		ref.i = i - 1;
		// Pick transition within choice
		ref.offset = getTransitionIndexByProbabilitySum(i - 1, x - (tot - d));
	}

	/**
	 * Get the index of a transition (within a choice) according to a probability (or rate) sum, x.
	 * i.e. the first transition for which the sum of probabilities/rates of that and all prior
	 * transitions (across all choices) exceeds x.
	 * @param i Index of the choice
	 * @param x Probability (or rate) sum
	 */
	private int getTransitionIndexByProbabilitySum(int i, double x) throws PrismException
	{
		int numTransitions = modelGen.getNumTransitions(i);
		if (numTransitions > 1) {
			double tot = 0.0;
			int offset = 0;
			for (offset = 0; x >= tot && offset < numTransitions; offset++) {
				tot += modelGen.getTransitionProbability(i, offset);
			}
			return offset - 1;
		} else {
			return 0;
		}
	}

	/**
	 * Select, at random, n successive transitions and execute them.
	 * For continuous-time models, the time to be spent in each state before leaving is also picked randomly.
	 * If a deadlock is found, the process stops.
	 * Optionally, if a deterministic loop is detected, the process stops.
	 * The function returns the number of transitions successfully taken. 
	 */
	public int automaticTransitions(int n, boolean stopOnLoops) throws PrismException
	{
		int i = 0;
		while (i < n && !(stopOnLoops && path.isLooping())) {
			if (!automaticTransition())
				break;
			i++;
		}
		return i;
	}

	/**
	 * Randomly select and execute transitions until the specified time delay is first exceeded.
	 * (Time is measured from the initial execution of this method, not total time.)
	 * (For discrete-time models, this just results in ceil(time) steps being executed.)
	 * If a deadlock is found, the process stops.
	 * The function returns the number of transitions successfully taken. 
	 */
	public int automaticTransitions(double time, boolean stopOnLoops) throws PrismException
	{
		// For discrete-time models, this just results in ceil(time) steps being executed.
		if (!modelType.continuousTime()) {
			return automaticTransitions((int) Math.ceil(time), false);
		} else {
			int i = 0;
			double targetTime = path.getTotalTime() + time;
			while (path.getTotalTime() < targetTime) {
				if (automaticTransition())
					i++;
				else
					break;
			}
			return i;
		}
	}

	/**
	 * Backtrack to a particular step within the current path.
	 * Time point should be >=0 and <= the total path size. 
	 * (Not applicable for on-the-fly paths)
	 * @param step The step to backtrack to.
	 */
	public void backtrackTo(int step) throws PrismException
	{
		// Check step is valid
		if (step < 0) {
			throw new PrismException("Cannot backtrack to a negative step index");
		}
		if (step > path.size()) {
			throw new PrismException("There is no step " + step + " to backtrack to");
		}
		// Back track in path
		((PathFull) path).backtrack(step);
		// Update current state
		currentState.copy(path.getCurrentState());
		// Re-explore current state in model generator
		computeTransitionsForState(currentState);
		// Recompute samplers for any loaded properties
		recomputeSamplers();
	}

	/**
	 * Backtrack to a particular (continuous) time point within the current path.
	 * Time point should be >=0 and <= the total path time. 
	 * (Not applicable for on-the-fly paths)
	 * @param time The amount of time to backtrack.
	 */
	public void backtrackTo(double time) throws PrismException
	{
		// Check step is valid
		if (time < 0) {
			throw new PrismException("Cannot backtrack to a negative time point");
		}
		if (time > path.getTotalTime()) {
			throw new PrismException("There is no time point " + time + " to backtrack to");
		}
		PathFull pathFull = (PathFull) path;
		// Get length (non-on-the-fly paths will never exceed length Integer.MAX_VALUE) 
		long nLong = path.size();
		if (nLong > Integer.MAX_VALUE)
			throw new PrismException("PathFull cannot deal with paths over length " + Integer.MAX_VALUE);
		int n = (int) nLong;
		// Find the index of the step we are in at that point
		// i.e. the first state whose cumulative time on entering exceeds 'time'
		int step;
		for (step = 0; step <= n && pathFull.getCumulativeTime(step) < time; step++)
			;
		// Then backtrack to this step
		backtrackTo(step);
	}

	/**
	 * Remove the prefix of the current path up to the given path step.
	 * Index step should be >=0 and <= the total path size. 
	 * (Not applicable for on-the-fly paths)
	 * @param step The step before which state will be removed.
	 */
	public void removePrecedingStates(int step) throws PrismException
	{
		// Check step is valid
		if (step < 0) {
			throw new PrismException("Cannot remove states before a negative step index");
		}
		if (step > path.size()) {
			throw new PrismException("There is no step " + step + " in the path");
		}
		// Modify path
		((PathFull) path).removePrecedingStates(step);
		// (No need to update currentState or re-generate transitions) 
		// Recompute samplers for any loaded properties
		recomputeSamplers();
	}

	/**
	 * Compute the transition table for an earlier step in the path.
	 * (Not applicable for on-the-fly paths)
	 * If you do this mid-path, don't forget to reset the transition table
	 * with <code>computeTransitionsForCurrentState</code> because this
	 * is not kept track of by the simulator.
	 */
	public void computeTransitionsForStep(int step) throws PrismException
	{
		computeTransitionsForState(((PathFull) path).getState(step));
	}

	/**
	 * Re-compute the transition table for the current state.
	 */
	public void computeTransitionsForCurrentState() throws PrismException
	{
		computeTransitionsForState(path.getCurrentState());
	}

	/**
	 * Re-compute the transition table for a particular state.
	 */
	private void computeTransitionsForState(State state) throws PrismException
	{
		modelGen.exploreState(state);
		transitionListState = state;
	}

	// ------------------------------------------------------------------------------
	// Methods for loading objects from model checking: paths, strategies, etc.
	// ------------------------------------------------------------------------------

	/**
	 * Load the set of reachable states for the currently loaded model into the simulator.
	 */
	public void loadReachableStates(List<State> reachableStates)
	{
		this.reachableStates = reachableStates;
	}

	/**
	 * Load a strategy for the currently loaded model into the simulator.
	 */
	public void loadStrategy(Strategy strategy)
	{
		this.strategy = strategy;
	}

	/**
	 * Construct a path through the currently loaded model to match a supplied path,
	 * specified as a PathFullInfo object.
	 * @param newPath Path to match
	 */
	public void loadPath(PathFullInfo newPath) throws PrismException
	{
		long numStepsLong = newPath.size();
		if (numStepsLong > Integer.MAX_VALUE)
			throw new PrismException("PathFull cannot deal with paths over length " + Integer.MAX_VALUE);
		int numSteps = (int) numStepsLong;
		
		createNewPath();
		State nextState, state = newPath.getState(0);
		initialisePath(state);
		for (int i = 0; i < numSteps; i++) {
			nextState = newPath.getState(i + 1);
			// Find matching transition
			// (just look at states for now)
			int numChoices = modelGen.getNumChoices();
			boolean found = false;
			for (int j = 0; j < numChoices; j++) {
				int numTransitions = modelGen.getNumTransitions(j);
				for (int offset = 0; offset < numTransitions; offset++) {
					if (modelGen.computeTransitionTarget(j, offset).equals(nextState)) {
						found = true;
						if (modelType.continuousTime() && newPath.hasTimeInfo())
							manualTransition(j, newPath.getTime(i));
						else
							manualTransition(j);
						break;
					}
				}
			}
			if (!found) {
				throw new PrismException("Path loading failed at step " + (i + 1));
			}
			state = nextState;
		}
	}

	// ------------------------------------------------------------------------------
	// Methods for adding/querying labels and properties
	// ------------------------------------------------------------------------------

	/**
	 * Add a label to the simulator, whose value will be computed during path generation.
	 * The resulting index of the label is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the label must have been defined in the current model.
	 * If there are additional constants (e.g. from a properties file),
	 * then use the {@link #addLabel(Expression, PropertiesFile)} method. 
	 */
	public int addLabel(Expression label) throws PrismLangException
	{
		return addLabel(label, null);
	}

	/**
	 * Add a label to the simulator, whose value will be computed during path generation.
	 * The resulting index of the label is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the label must have been defined in the current model
	 * or be supplied in the (optional) passed in PropertiesFile.
	 */
	public int addLabel(Expression label, PropertiesFile pf) throws PrismLangException
	{
		// Take a copy, get rid of any constants and simplify
		Expression labelNew = label.deepCopy();
		labelNew = (Expression) labelNew.replaceConstants(mfConstants);
		if (pf != null) {
			labelNew = (Expression) labelNew.replaceConstants(pf.getConstantValues());
		}
		labelNew = (Expression) labelNew.simplify();
		// Add to list and return index
		labels.add(labelNew);
		return labels.size() - 1;
	}

	/**
	 * Add a (path) property to the simulator, whose value will be computed during path generation.
	 * The resulting index of the property is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the label must have been defined in the current model.
	 * If there are additional constants (e.g. from a properties file),
	 * then use the {@link #addProperty(Expression, PropertiesFile)} method. 
	 */
	public int addProperty(Expression prop) throws PrismException
	{
		return addProperty(prop, null);
	}

	/**
	 * Add a (path) property to the simulator, whose value will be computed during path generation.
	 * The resulting index of the property is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the property must have been defined in the current model
	 * or be supplied in the (optional) passed in PropertiesFile.
	 * In case of error, the property is not added an exception is thrown.
	 */
	public int addProperty(Expression prop, PropertiesFile pf) throws PrismException
	{
		// Take a copy
		Expression propNew = prop.deepCopy();
		// Combine label lists from model/property file, then expand property refs/labels in property
		// For now, model labels are only handled if stored in pf.getCombinedLabelList(),
		// which currently only happens if the model came from a ModulesFile
		LabelList combinedLabelList = (pf == null) ? null : pf.getCombinedLabelList();
		propNew = (Expression) propNew.expandPropRefsAndLabels(pf, combinedLabelList);
		// Then get rid of any constants and simplify
		propNew = (Expression) propNew.replaceConstants(mfConstants);
		if (pf != null) {
			propNew = (Expression) propNew.replaceConstants(pf.getConstantValues());
		}
		propNew = (Expression) propNew.simplify();
		// Create sampler
		Sampler sampler = Sampler.createSampler(propNew, modelGen, rewardGen);
		// Update lists and return index
		// (do this right at the end so that lists only get updated if there are no errors)
		properties.add(propNew);
		propertySamplers.add(sampler);
		return properties.size() - 1;
	}

	/**
	 * Get the current value of a previously added label (specified by its index).
	 */
	public boolean queryLabel(int index) throws PrismLangException
	{
		return labels.get(index).evaluateBoolean(path.getCurrentState());
	}

	/**
	 * Get the value, at the specified path step, of a previously added label (specified by its index).
	 * (Not applicable for on-the-fly paths)
	 * @param step The index of the step to check for
	 */
	public boolean queryLabel(int index, int step) throws PrismLangException
	{
		return labels.get(index).evaluateBoolean(((PathFull) path).getState(step));
	}

	/**
	 * Check whether the current state is an initial state.
	 */
	public boolean queryIsInitial() throws PrismException
	{
		// Currently multiple initial states are not supported so this is easy to check
		return path.getCurrentState().equals(modelGen.getInitialState());
	}

	/**
	 * Check whether a particular step in the current path is an initial state.
	 * @param step The index of the step to check for
	 * (Not applicable for on-the-fly paths)
	 */
	public boolean queryIsInitial(int step) throws PrismException
	{
		// Currently multiple initial states are not supported so this is easy to check
		return ((PathFull) path).getState(step).equals(modelGen.getInitialState());
	}

	/**
	 * Check whether the current state is a deadlock.
	 */
	public boolean queryIsDeadlock() throws PrismException
	{
		return modelGen.isDeadlock();
	}

	/**
	 * Check whether a particular step in the current path is a deadlock.
	 * (Not applicable for on-the-fly paths)
	 * @param step The index of the step to check for
	 */
	public boolean queryIsDeadlock(int step) throws PrismException
	{
		// By definition, earlier states in the path cannot be deadlocks
		return step == path.size() ? modelGen.getNumChoices() == 0 : false;
	}

	/**
	 * Check the value for a particular property in the current path.
	 * If the value is not known yet, the result will be null.
	 * @param index The index of the property to check
	 */
	public Object queryProperty(int index)
	{
		if (index < 0 || index >= propertySamplers.size()) {
			mainLog.printWarning("Can't query property " + index + ".");
			return null;
		}
		Sampler sampler = propertySamplers.get(index);
		return sampler.isCurrentValueKnown() ? sampler.getCurrentValue() : null;
	}

	// ------------------------------------------------------------------------------
	// Private methods for path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Utility method for computing state rewards for all reward structs.
	 */
	private void calculateStateRewards(State state, double[] stateRewards) throws PrismException
	{
		int numRewardStructs = rewardGen.getNumRewardStructs();
		for (int r = 0; r < numRewardStructs; r++) {
			stateRewards[r] = rewardGen.getStateReward(r, state);
		}
	}
	
	/**
	 * Utility method for computing transition rewards for all reward structs.
	 */
	private void calculateTransitionRewards(State state, Object action, double[] transitionRewards) throws PrismException
	{
		int numRewardStructs = rewardGen.getNumRewardStructs();
		for (int r = 0; r < numRewardStructs; r++) {
			transitionRewards[r] = rewardGen.getStateActionReward(r, state, action);
		}
	}
	
	/**
	 * Execute a transition from the current transition list and update path (if being stored).
	 * Transition is specified by index of its choice and offset within it. If known, its index
	 * (within the whole list) can optionally be specified (since this may be needed for storage
	 * in the path). If this is -1, it will be computed automatically if needed.
	 * @param i Index of choice containing transition to execute
	 * @param offset Index within choice of transition to execute
	 * @param index (Optionally) index of transition within whole list (-1 if unknown)
	 */
	private void executeTransition(int i, int offset, int index) throws PrismException
	{
		// If required (for full paths), calculate transition index
		if (!onTheFly && index == -1) {
			index = modelGen.getTotalIndexOfTransition(i, offset);
		}
		// Get probability and action for transition
		double p = modelGen.getTransitionProbability(i, offset);
		Object action = modelGen.getChoiceAction(i);
		String actionString = modelGen.getChoiceActionString(i);
		// Compute its transition rewards
		calculateTransitionRewards(path.getCurrentState(), action, tmpTransitionRewards);
		// Compute next state
		currentState.copy(modelGen.computeTransitionTarget(i, offset));
		// Compute observation for new state
		State currentObs = modelGen.getObservation(currentState);
		// Compute state rewards for new state
		calculateStateRewards(currentState, tmpStateRewards);
		// Update path
		path.addStep(index, action, actionString, p, tmpTransitionRewards, currentState, currentObs, tmpStateRewards, modelGen);
		// Explore new state in model generator
		computeTransitionsForState(currentState);
		// Update samplers for any loaded properties
		updateSamplers();
		// Update strategy (if loaded)
		updateStrategy();
	}

	/**
	 * Execute a (timed) transition from the current transition list and update path (if being stored).
	 * Transition is specified by index of its choice and offset within it. If known, its index
	 * (within the whole list) can optionally be specified (since this may be needed for storage
	 * in the path). If this is -1, it will be computed automatically if needed.
	 * In addition, the amount of time to be spent in the current state before this transition occurs should be specified.
	 * [continuous-time models only]
	 * @param i Index of choice containing transition to execute
	 * @param offset Index within choice of transition to execute
	 * @param time Time for transition
	 * @param index (Optionally) index of transition within whole list (-1 if unknown)
	 */
	private void executeTimedTransition(int i, int offset, double time, int index) throws PrismException
	{
		// If required (for full paths), calculate transition index
		if (!onTheFly && index == -1) {
			index = modelGen.getTotalIndexOfTransition(i, offset);
		}
		// Get probability and action for transition
		double p = modelGen.getTransitionProbability(i, offset);
		Object action = modelGen.getChoiceAction(i);
		String actionString = modelGen.getChoiceActionString(i);
		// Compute its transition rewards
		calculateTransitionRewards(path.getCurrentState(), action, tmpTransitionRewards);
		// Compute next state
		currentState.copy(modelGen.computeTransitionTarget(i, offset));
		// Compute observation for new state
		State currentObs = modelGen.getObservation(currentState);
		// Compute state rewards for new state
		calculateStateRewards(currentState, tmpStateRewards);
		// Update path
		path.addStep(time, index, action, actionString, p, tmpTransitionRewards, currentState, currentObs, tmpStateRewards, modelGen);
		// Explore new state in model generator
		computeTransitionsForState(currentState);
		// Update samplers for any loaded properties
		updateSamplers();
		// Update strategy (if loaded)
		updateStrategy();
	}

	/**
	 * Reset samplers for any loaded properties.
	 */
	private void resetSamplers() throws PrismLangException
	{
		for (Sampler sampler : propertySamplers) {
			sampler.reset();
		}
	}

	/**
	 * Notify samplers for any loaded properties that a new step has occurred.
	 */
	private void updateSamplers() throws PrismException
	{
		for (Sampler sampler : propertySamplers) {
			sampler.update(path, modelGen);
		}
	}

	/**
	 * Recompute the state of samplers for any loaded properties based on the whole current path.
	 * (Not applicable for on-the-fly paths)
	 */
	private void recomputeSamplers() throws PrismException
	{
		resetSamplers();
		// Get length (non-on-the-fly paths will never exceed length Integer.MAX_VALUE) 
		long nLong = path.size();
		if (nLong > Integer.MAX_VALUE)
			throw new PrismLangException("PathFull cannot deal with paths over length " + Integer.MAX_VALUE);
		int n = (int) nLong;
		// Loop
		PathFullPrefix prefix = new PathFullPrefix((PathFull) path, 0);
		for (int i = 0; i <= n; i++) {
			prefix.setPrefixLength(i);
			for (Sampler sampler : propertySamplers) {
				sampler.update(prefix, null);
				// TODO: fix this optimisation 
			}
		}
	}

	/**
	 * Initialise the state of the loaded strategy, if present, based on the current state.
	 */
	private void initialiseStrategy()
	{
		if (strategy != null) {
			State state = getCurrentState();
			int s = reachableStates.indexOf(state);
			strategy.initialise(s);
		}
	}

	/**
	 * Update the state of the loaded strategy, if present, based on the last step that occurred.
	 */
	private void updateStrategy()
	{
		if (strategy != null) {
			State state = getCurrentState();
			int s = reachableStates.indexOf(state);
			Object action = path.getPreviousAction();
			strategy.update(action, s);
		}
	}

	// ------------------------------------------------------------------------------
	// Queries regarding model
	// ------------------------------------------------------------------------------

	/**
	 * Returns the number of variables in the current model.
	 */
	public int getNumVariables()
	{
		return numVars;
	}

	/**
	 * Returns the name of the ith variable in the current model.
	 * (Returns null if index i is out of range.)
	 */
	public String getVariableName(int i)
	{
		return (i < numVars && i >= 0) ? varList.getName(i) : null;
	}

	/**
	 * Returns the type of the ith variable in the current model.
	 * (Returns null if index i is out of range.)
	 */
	public Type getVariableType(int i)
	{
		return (i < numVars && i >= 0) ? varList.getType(i) : null;
	}

	/**
	 * Returns the index of a variable name, as stored by the simulator for the current model.
	 * Returns -1 if the action name does not exist. 
	 */
	public int getIndexOfVar(String name) throws PrismException
	{
		return varList.getIndex(name);
	}

	// ------------------------------------------------------------------------------
	// Querying of current state and its available choices/transitions
	// ------------------------------------------------------------------------------

	/**
	 * Get the state for which the simulator is currently supplying information about its transitions. 
	 * Usually, this is the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be this state instead.
	 */
	public State getTransitionListState()
	{
		return (transitionListState == null) ? path.getCurrentState() : transitionListState;
		
	}
	
	/**
	 * Returns the current number of available choices.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getNumChoices() throws PrismException
	{
		return modelGen.getNumChoices();
	}

	/**
	 * Returns the current (total) number of available transitions.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getNumTransitions() throws PrismException
	{
		return modelGen.getNumTransitions();
	}

	/**
	 * Returns the current number of available transitions in choice i.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getNumTransitions(int i) throws PrismException
	{
		return modelGen.getNumTransitions(i);
	}

	/**
	 * Get the index of the choice containing a transition of a given index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getChoiceIndexOfTransition(int index) throws PrismException
	{
		return modelGen.getChoiceIndexOfTransition(index);
	}

	/**
	 * Get the action label of a transition, specified by its index/offset.
	 * (null for asynchronous/independent transitions)
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public Object getTransitionAction(int i, int offset) throws PrismException
	{
		return modelGen.getTransitionAction(i, offset);
	}

	/**
	 * Get the action label of a transition, specified by its index.
	 * (null for asynchronous/independent transitions)
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public Object getTransitionAction(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		return modelGen.getTransitionAction(i, offset);
	}

	/**
	 * Get a string describing the action of a transition, specified by its index/offset.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionActionString(int i, int offset) throws PrismException
	{
		return modelGen.getTransitionActionString(i, offset);
	}

	/**
	 * Get a string describing the action of a transition, specified by its index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionActionString(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		return getTransitionActionString(i, offset);
	}

	/**
	 * Get the probability/rate of a transition within a choice, specified by its index/offset.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public double getTransitionProbability(int i, int offset) throws PrismException
	{
		return modelGen.getTransitionProbability(i, offset);
	}

	/**
	 * Get the probability/rate of a transition, specified by its index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public double getTransitionProbability(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		return getTransitionProbability(i, offset);
	}

	/**
	 * Get a string describing the update comprising a transition, specified by its index/offset.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionUpdateString(int i, int offset) throws PrismException
	{
		return modelGen.getTransitionUpdateString(i, offset);
	}

	/**
	 * Get a string describing the update comprising a transition, specified by its index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionUpdateString(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		return getTransitionUpdateString(i, offset);
	}

	/**
	 * Get a verbose string describing the update comprising a transition, specified by its index/offset.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionUpdateStringFull(int i, int offset) throws PrismException
	{
		return modelGen.getTransitionUpdateStringFull(i, offset);
	}

	/**
	 * Get a verbose string describing the update comprising a transition, specified by its index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionUpdateStringFull(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		return getTransitionUpdateStringFull(i, offset);
	}

	/**
	 * Get the target (as a new State object) of a transition within a choice, specified by its index/offset.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public State computeTransitionTarget(int i, int offset) throws PrismException
	{
		return modelGen.computeTransitionTarget(i, offset);
	}

	/**
	 * Get the target of a transition (as a new State object), specified by its index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public State computeTransitionTarget(int index) throws PrismException
	{
		int i = modelGen.getChoiceIndexOfTransition(index);
		int offset = modelGen.getChoiceOffsetOfTransition(index);
		return computeTransitionTarget(i, offset);
	}

	// ------------------------------------------------------------------------------
	// Querying of current path (full or on-the-fly)
	// ------------------------------------------------------------------------------

	/**
	 * Get access to the {@code Path} object storing the current path.
	 * This object is only valid until the next time {@link #createNewPath} is called. 
	 */
	public Path getPath()
	{
		return (Path) path;
	}

	/**
	 * Get the size of the current path (number of steps; or number of states - 1).
	 */
	public long getPathSize()
	{
		return path.size();
	}

	/**
	 * Returns the current state being explored by the simulator.
	 */
	public State getCurrentState()
	{
		return path.getCurrentState();
	}

	/**
	 * Returns the previous state of the current path in the simulator.
	 */
	public State getPreviousState()
	{
		return path.getPreviousState();
	}

	/**
	 * Get the total time elapsed so far (where zero time has been spent in the current (final) state).
	 * For discrete-time models, this is just the number of steps (but returned as a double).
	 */
	public double getTotalTimeForPath()
	{
		return path.getTotalTime();
	}

	/**
	 * Get the total reward accumulated so far
	 * (includes reward for previous transition but no state reward for current (final) state).
	 * @param rsi Reward structure index
	 */
	public double getTotalCumulativeRewardForPath(int rsi)
	{
		return path.getTotalCumulativeReward(rsi);
	}

	// ------------------------------------------------------------------------------
	// Querying of current path (full paths only)
	// ------------------------------------------------------------------------------

	/**
	 * Get access to the {@code PathFull} object storing the current path.
	 * (Not applicable for on-the-fly paths)
	 * This object is only valid until the next time {@link #createNewPath} is called. 
	 */
	public PathFull getPathFull()
	{
		return (PathFull) path;
	}

	/**
	 * Get the value of a variable at a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 * @param varIndex The index of the variable to look up
	 */
	public Object getVariableValueOfPathStep(int step, int varIndex)
	{
		return ((PathFull) path).getState(step).varValues[varIndex];
	}

	/**
	 * Get the state at a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public State getStateOfPathStep(int step)
	{
		return ((PathFull) path).getState(step);
	}

	/**
	 * Get the observation at a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public State getObservationOfPathStep(int step)
	{
		return ((PathFull) path).getObservation(step);
	}

	/**
	 * Get a state reward for the state at a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public double getStateRewardOfPathStep(int step, int rsi)
	{
		return ((PathFull) path).getStateReward(step, rsi);
	}

	/**
	 * Get the total time spent up until entering a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public double getCumulativeTimeUpToPathStep(int step)
	{
		return ((PathFull) path).getCumulativeTime(step);
	}

	/**
	 * Get the total (state and transition) reward accumulated up until entering a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public double getCumulativeRewardUpToPathStep(int step, int rsi)
	{
		return ((PathFull) path).getCumulativeReward(step, rsi);
	}

	/**
	 * Get the time spent in a state at a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public double getTimeSpentInPathStep(int step)
	{
		return ((PathFull) path).getTime(step);
	}

	/**
	 * Get the index of the choice taken for a given step.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public int getChoiceOfPathStep(int step)
	{
		return ((PathFull) path).getChoice(step);
	}

	/**
	 * Get the action taken in a given step.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public Object getActionOfPathStep(int step)
	{
		return ((PathFull) path).getAction(step);
	}

	/**
	 * Get a string describing the action taken in a given step.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public String getActionStringOfPathStep(int step)
	{
		return ((PathFull) path).getActionString(step);
	}

	/**
	 * Get a transition reward associated with a given step.
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public double getTransitionRewardOfPathStep(int step, int rsi)
	{
		return ((PathFull) path).getTransitionReward(step, rsi);
	}

	/**
	 * Check whether the current path is in a deterministic loop.
	 */
	public boolean isPathLooping()
	{
		return path.isLooping();
	}

	/**
	 * Get at which step a deterministic loop (if present) starts.
	 */
	public long loopStart()
	{
		return path.loopStart();
	}

	/**
	 * Get at which step a deterministic loop (if present) ends.
	 */
	public long loopEnd()
	{
		return path.loopEnd();
	}

	/**
	 * Export the current path to a file in a simple space separated format.
	 * (Not applicable for on-the-fly paths)
	 * @param file File to which the path should be exported to (mainLog if null).
	 */
	public void exportPath(File file) throws PrismException
	{
		exportPath(file, false);
	}

	/**
	 * Export the current path to a file in a simple space separated format.
	 * (Not applicable for on-the-fly paths)
	 * @param file File to which the path should be exported to (mainLog if null).
	 * @param showRewards Export reward information with the path
	 */
	public void exportPath(File file, boolean showRewards) throws PrismException
	{
		exportPath(file, false, showRewards, " ", null);
	}

	/**
	 * Export the current path to a file.
	 * (Not applicable for on-the-fly paths)
	 * @param file File to which the path should be exported to (mainLog if null).
	 * @param timeCumul Show time in cumulative form?
	 * @param colSep String used to separate columns in display
	 * @param vars Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportPath(File file, boolean timeCumul, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		exportPath(file, timeCumul, false, colSep, vars);
	}

	/**
	 * Export the current path to a file.
	 * (Not applicable for on-the-fly paths)
	 * @param file File to which the path should be exported to (mainLog if null).
	 * @param timeCumul Show time in cumulative form?
	 * @param showRewards Export reward information with the path
	 * @param colSep String used to separate columns in display
	 * @param vars Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportPath(File file, boolean timeCumul, boolean showRewards, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		PrismLog log = null;
		try {
			if (path == null)
				throw new PrismException("There is no path to export");
			// create new file log or use main log
			if (file != null) {
				log = new PrismFileLog(file.getPath());
				if (!log.ready()) {
					throw new PrismException("Could not open file \"" + file + "\" for output");
				}
				mainLog.println("\nExporting path to file \"" + file + "\"...");
			} else {
				log = mainLog;
				log.println();
			}
			((PathFull) path).exportToLog(log, timeCumul, showRewards, colSep, vars);
			if (file != null)
				log.close();
		} finally {
			if (file != null && log != null)
				log.close();
		}
	}

	/**
	 * Plot the current path on a Graph.
	 * @param graphModel Graph on which to plot path
	 */
	public void plotPath(Graph graphModel) throws PrismException
	{
		((PathFull) path).plotOnGraph(graphModel);
	}

	// ------------------------------------------------------------------------------
	// Model checking (statistical/approximate)
	// ------------------------------------------------------------------------------

	/**
	 * Check whether a property is suitable for statistical/approximate model checking using the simulator.
	 */
	public boolean isPropertyOKForSimulation(Expression expr)
	{
		return isPropertyOKForSimulationString(expr) == null;
	}

	/**
	 * Check whether a property is suitable for statistical/approximate model checking using the simulator.
	 * If not, an explanatory error message is thrown as an exception.
	 */
	public void checkPropertyForSimulation(Expression expr) throws PrismException
	{
		String errMsg = isPropertyOKForSimulationString(expr);
		if (errMsg != null)
			throw new PrismNotSupportedException(errMsg);
	}

	/**
	 * Check whether a property is suitable for statistical/approximate model checking using the simulator.
	 * If yes, return null; if not, return an explanatory error message.
	 */
	private String isPropertyOKForSimulationString(Expression expr)
	{
		// Simulator can only be applied to P or R properties (without filters)
		if (!(expr instanceof ExpressionProb || expr instanceof ExpressionReward)) {
			if (expr instanceof ExpressionFilter) {
				if (((ExpressionFilter) expr).getOperand() instanceof ExpressionProb || ((ExpressionFilter) expr).getOperand() instanceof ExpressionReward)
					return "Simulator cannot handle P or R properties with filters";
			}
			return "Simulator can only handle P or R properties";
		}
		// Check that there are no nested probabilistic operators
		try {
			if (expr.computeProbNesting() > 1) {
				return "Simulator cannot handle nested P, R or S operators";
			}
		} catch (PrismException e) {
			return "Simulator cannot handle this property: " + e.getMessage();
		}
		// Simulator cannot handle cumulative reward properties without a time bound
		if (expr instanceof ExpressionReward) {
			Expression exprTemp = ((ExpressionReward) expr).getExpression();
			if (exprTemp instanceof ExpressionTemporal) {
				if (((ExpressionTemporal) exprTemp).getOperator() == ExpressionTemporal.R_C) {
					if (((ExpressionTemporal) exprTemp).getUpperBound() == null) {
						return "Simulator cannot handle cumulative reward properties without time bounds";
					}
				}
			}
		}
		// No errors
		return null;
	}

	/**
	 * Perform statistical/approximate model checking of a property on the current model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Returns a Result object, except in case of error, where an Exception is thrown.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param expr The property to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 */
	public Result modelCheckSingleProperty(PropertiesFile propertiesFile, Expression expr, State initialState, long maxPathLength,
			SimulationMethod simMethod) throws PrismException
	{
		ArrayList<Expression> exprs;
		Result res[];

		// Just do this via the 'multiple properties' method
		exprs = new ArrayList<Expression>();
		exprs.add(expr);
		res = modelCheckMultipleProperties(propertiesFile, exprs, initialState, maxPathLength, simMethod);

		if (res[0].getResult() instanceof PrismException)
			throw (PrismException) res[0].getResult();
		else
			return res[0];
	}

	/**
	 * Perform statistical/approximate model checking of properties on the current model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Returns an array of results, some of which may contain Exception objects if there were errors.
	 * In the case of an error which affects all properties, an exception is thrown.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param exprs The properties to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 */
	public Result[] modelCheckMultipleProperties(PropertiesFile propertiesFile, List<Expression> exprs, State initialState,
			long maxPathLength, SimulationMethod simMethod) throws PrismException
	{
		// Create path to be used
		createNewOnTheFlyPath();

		// Make sure any missing parameters that can be computed before simulation
		// are computed now (sometimes this has been done already, e.g. for GUI display).
		simMethod.computeMissingParameterBeforeSim();

		// Print details to log
		mainLog.println("\nSimulation method: " + simMethod.getName() + " (" + simMethod.getFullName() + ")");
		mainLog.println("Simulation method parameters: " + simMethod.getParametersString());
		mainLog.println("Simulation parameters: max path length=" + maxPathLength);

		// Add the properties to the simulator (after a check that they are valid)
		Result[] results = new Result[exprs.size()];
		int[] indices = new int[exprs.size()];
		int validPropsCount = 0;
		for (int i = 0; i < exprs.size(); i++) {
			try {
				checkPropertyForSimulation(exprs.get(i));
				indices[i] = addProperty(exprs.get(i), propertiesFile);
				validPropsCount++;
				// Attach a SimulationMethod object to each property's sampler
				SimulationMethod simMethodNew = simMethod.clone();
				propertySamplers.get(indices[i]).setSimulationMethod(simMethodNew);
				// Pass property details to SimuationMethod
				// (note that we use the copy stored in properties, which has been processed)
				try {
					simMethodNew.setExpression(properties.get(indices[i]));
				} catch (PrismException e) {
					// In case of error, also need to remove property/sampler from list
					properties.remove(indices[i]);
					propertySamplers.remove(indices[i]);
					throw e;
				}
			} catch (PrismException e) {
				results[i] = new Result(e);
				indices[i] = -1;
			}
		}

		// As long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			doSampling(initialState, maxPathLength);
		}

		// Process the results
		for (int i = 0; i < results.length; i++) {
			// If there was no earlier error, extract result etc.
			if (indices[i] != -1) {
				Sampler sampler = propertySamplers.get(indices[i]);
				SimulationMethod sm = sampler.getSimulationMethod();
				// Compute/print any missing parameters that need to be done after simulation
				sm.computeMissingParameterAfterSim();
				// Extract result from SimulationMethod and store
				try {
					results[i] = new Result(sm.getResult(sampler));
					results[i].setAccuracy(sampler.getSimulationMethod().getResultAccuracy(sampler));
				} catch (PrismException e) {
					results[i] = new Result(e);
				}
			}
		}

		// Warning for nondeterministic models
		if (results.length > 0) {
			ModelType currentModelType = modelGen.getModelType();
			if (currentModelType.nondeterministic() && currentModelType.removeNondeterminism() != currentModelType) {
				mainLog.println("Warning: Nondeterminism in " + currentModelType.name() + " was resolved uniformly");
			}
		}

		
		// Display results to log
		if (results.length == 1) {
			if (!(results[0].getResult() instanceof PrismException))
				mainLog.println("\nResult: " + results[0].getResultAndAccuracy());
		} else {
			mainLog.println("\nResults:");
			for (int i = 0; i < results.length; i++)
				mainLog.println(exprs.get(i) + " : " + results[i].getResultAndAccuracy());
		}
		
		return results;
	}

	/**
	 * Perform a statistical/approximate model checking experiment on the current model, using the simulator
	 * (specified by values for undefined constants from the property only).
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Results are stored in the ResultsCollection object passed in,
	 * some of which may be Exception objects if there were errors.
	 * In the case of an error which affects all properties, an exception is thrown.
	 * Note: All constants in the model file must have already been defined.
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param undefinedConstants Details of constant ranges defining the experiment
	 * @param resultsCollection Where to store the results
	 * @param expr The property to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 * @throws PrismException if something goes wrong with the sampling algorithm
	 * @throws InterruptedException if the thread is interrupted
	 */
	public void modelCheckExperiment(PropertiesFile propertiesFile, UndefinedConstants undefinedConstants,
			ResultsCollection resultsCollection, Expression expr, State initialState, long maxPathLength, SimulationMethod simMethod) throws PrismException,
			InterruptedException
	{
		// Create path to be used
		createNewOnTheFlyPath();

		// Make sure any missing parameters that can be computed before simulation
		// are computed now (sometimes this has been done already, e.g. for GUI display).
		simMethod.computeMissingParameterBeforeSim();

		// Print details to log
		mainLog.println("\nSimulation method: " + simMethod.getName() + " (" + simMethod.getFullName() + ")");
		mainLog.println("Simulation method parameters: " + simMethod.getParametersString());
		mainLog.println("Simulation parameters: max path length=" + maxPathLength);

		// Add the properties to the simulator (after a check that they are valid)
		int n = undefinedConstants.getNumPropertyIterations();
		Values definedPFConstants = new Values();
		Result[] results = new Result[n];
		Values[] pfcs = new Values[n];
		int[] indices = new int[n];

		int validPropsCount = 0;
		for (int i = 0; i < n; i++) {
			definedPFConstants = undefinedConstants.getPFConstantValues();
			pfcs[i] = definedPFConstants;
			// for simulation, use non-exact constant evaluation
			propertiesFile.setSomeUndefinedConstants(definedPFConstants, false);
			try {
				checkPropertyForSimulation(expr);
				indices[i] = addProperty(expr, propertiesFile);
				validPropsCount++;
				// Attach a SimulationMethod object to each property's sampler
				SimulationMethod simMethodNew = simMethod.clone();
				propertySamplers.get(indices[i]).setSimulationMethod(simMethodNew);
				// Pass property details to SimuationMethod
				// (note that we use the copy stored in properties, which has been processed)
				try {
					simMethodNew.setExpression(properties.get(indices[i]));
				} catch (PrismException e) {
					// In case of error, also need to remove property/sampler from list
					// (NB: this will be at the end of the list so no re-indexing issues)
					properties.remove(indices[i]);
					propertySamplers.remove(indices[i]);
					throw e;
				}
			} catch (PrismException e) {
				results[i] = new Result(e);
				indices[i] = -1;
			}
			undefinedConstants.iterateProperty();
		}

		// As long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			doSampling(initialState, maxPathLength);
		}

		// Process the results
		for (int i = 0; i < n; i++) {
			// If there was no earlier error, extract result etc.
			if (indices[i] != -1) {
				Sampler sampler = propertySamplers.get(indices[i]);
				SimulationMethod sm = sampler.getSimulationMethod();
				// Compute/print any missing parameters that need to be done after simulation
				sm.computeMissingParameterAfterSim();
				// Extract result from SimulationMethod and store
				try {
					results[i] = new Result(sm.getResult(sampler));
					results[i].setAccuracy(sampler.getSimulationMethod().getResultAccuracy(sampler));
				} catch (PrismException e) {
					results[i] = new Result(e);
				}
			}
			// Store result in the ResultsCollection
			resultsCollection.setResult(undefinedConstants.getMFConstantValues(), pfcs[i], results[i].getResult());
		}

		// Display results to log
		mainLog.println("\nResults:");
		for (int i = 0; i < results.length; i++)
			mainLog.println(pfcs[i] + " : " + results[i].getResultAndAccuracy());
		//mainLog.print(resultsCollection.toStringPartial(undefinedConstants.getMFConstantValues(), true, " ", " : ", false));
	}

	/**
	 * Execute sampling for the set of currently loaded properties.
	 * Sample paths are from the specified initial state and maximum length.
	 * Termination of the sampling process occurs when the SimulationMethod object
	 * for all properties indicate that it is finished.
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 */
	private void doSampling(State initialState, long maxPathLength) throws PrismException
	{
		int iters;
		long i;
		// Flags
		boolean stoppedEarly = false;
		boolean deadlocksFound = false;
		boolean allDone = false;
		boolean allKnown = false;
		boolean someUnknownButBounded = false;
		boolean shouldStopSampling = false;
		// Path stats
		double avgPathLength = 0;
		long minPathFound = 0, maxPathFound = 0;
		// Progress info
		int lastPercentageDone = 0;
		int percentageDone = 0;
		// Timing info
		long start, stop;
		double time_taken;

		// Start
		start = System.currentTimeMillis();
		mainLog.print("\nSampling progress: [");
		mainLog.flush();

		// Main sampling loop
		iters = 0;
		while (!shouldStopSampling) {

			// See if all properties are done; if so, stop sampling
			allDone = true;
			for (Sampler sampler : propertySamplers) {
				if (!sampler.getSimulationMethod().shouldStopNow(iters, sampler))
					allDone = false;
			}
			if (allDone)
				break;

			// Display progress (of slowest property)
			percentageDone = 100;
			for (Sampler sampler : propertySamplers) {
				percentageDone = Math.min(percentageDone, sampler.getSimulationMethod().getProgress(iters, sampler));
			}
			if (percentageDone > lastPercentageDone) {
				lastPercentageDone = percentageDone;
				mainLog.print(" " + lastPercentageDone + "%");
				mainLog.flush();
			}

			iters++;

			// Start the new path for this iteration (sample)
			initialisePath(initialState);

			// Generate a path
			allKnown = false;
			someUnknownButBounded = false;
			i = 0;
			while ((!allKnown && i < maxPathLength) || someUnknownButBounded) {
				// Check status of samplers
				allKnown = true;
				someUnknownButBounded = false;
				for (Sampler sampler : propertySamplers) {
					if (!sampler.isCurrentValueKnown()) {
						allKnown = false;
						if (sampler.needsBoundedNumSteps())
							someUnknownButBounded = true;
					}
				}
				// Stop when all answers are known or we have reached max path length
				// (but don't stop yet if there are "bounded" samplers with unkown values)
				if ((allKnown || i >= maxPathLength) && !someUnknownButBounded)
					break;
				// Make a random transition
				automaticTransition();
				i++;
			}

			// TODO: Detect deadlocks so we can report a warning

			// Update path length statistics
			avgPathLength = (avgPathLength * (iters - 1) + (i)) / iters;
			minPathFound = (iters == 1) ? i : Math.min(minPathFound, i);
			maxPathFound = (iters == 1) ? i : Math.max(maxPathFound, i);

			// If not all samplers could produce values, this an error
			if (!allKnown) {
				stoppedEarly = true;
				break;
			}

			// Update state of samplers based on last path
			for (Sampler sampler : propertySamplers) {
				sampler.updateStats();
			}
		}

		// Print details
		if (!stoppedEarly) {
			if (!shouldStopSampling)
				mainLog.print(" 100% ]");
			mainLog.println();
			stop = System.currentTimeMillis();
			time_taken = (stop - start) / 1000.0;
			mainLog.print("\nSampling complete: ");
			mainLog.print(iters + " iterations in " + time_taken + " seconds (average " + PrismUtils.formatDouble(2, time_taken / iters) + ")\n");
			mainLog.print("Path length statistics: average " + PrismUtils.formatDouble(2, avgPathLength) + ", min " + minPathFound + ", max " + maxPathFound
					+ "\n");
		} else {
			mainLog.print(" ...\n\nSampling terminated early after " + iters + " iterations.\n");
		}

		// Print a warning if deadlocks occurred at any point
		if (deadlocksFound)
			mainLog.printWarning("Deadlocks were found during simulation: self-loops were added.");

		// Print a warning if simulation was stopped by the user
		if (shouldStopSampling)
			mainLog.printWarning("Simulation was terminated before completion.");

		// write to feedback file with true to indicate that we have finished sampling
		// Write_Feedback(iteration_counter, numIters, true);

		if (stoppedEarly) {
			throw new PrismException(
					"One or more of the properties being sampled could not be checked on a sample. Consider increasing the maximum path length");
		}
	}

	/**
	 * Halt the sampling algorithm in its tracks (not implemented).
	 */
	public void stopSampling()
	{
		// TODO
	}
}
