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

package simulator;

import java.util.*;
import java.io.*;

import simulator.sampler.*;
import parser.*;
import parser.ast.*;
import parser.type.*;
import parser.visitor.ASTTraverse;
import prism.*;

/**
 * The SimulatorEngine class provides support for:
 * <UL>
 * <LI> State/model exploration
 * <LI> Manual/random path generation
 * <LI> Monte-Carlo sampling techniques for approximate property verification
 * </UL>
 * 
 * After creating a SimulatorEngine object, you can build paths or explore models using:
 * <UL>
 * <LI> <code>createNewPath</code> if you want to create a path that will be stored in full
 * <LI> <code>createNewOnTheFlyPath</code> if just want to do e.g. model exploration
 * </UL>
 * The input to these methods is a model (ModulesFile) in which all constants have already been defined.
 * 
 * At this point, you can also load labels and properties into the simulator, whose values
 * will be available during path generation. Use:
 * <UL>
 * <LI> <code>addLabel</code>
 * <LI> <code>addProperty</code>
 * </UL>
 * 
 * To actually initialise the path with an initial state (or to reinitialise later) use:
 * <UL>
 * <LI> <code>initialisePath</code>
 * <LI> <code>addProperty</code>
 * </UL>
 * 
 * To see the transitions available in the current state, use:
 * <UL>
 * <LI> ... TODO
 * </UL>
 * 
 * For path manipulation...
 * <UL>
 * <LI> ... TODO
 * </UL>
 * 
 * ...
 * TODO
 * 
 * For sampling-based approximate model checking, use:
 * <UL>
 * <LI> <code>checkPropertyForSimulation</code>
 * <LI> <code>modelCheckSingleProperty</code>
 * <LI> <code>modelCheckMultipleProperties</code>
 * <LI> <code>modelCheckExperiment</code>
 * </UL>
 */
public class SimulatorEngine
{
	// PRISM stuff
	protected Prism prism;
	protected PrismLog mainLog;

	// The current parsed model + info
	private ModulesFile modulesFile;
	private ModelType modelType;
	// Variable info
	private VarList varList;
	private int numVars;
	private Map<String, Integer> varIndices;
	// Synchronising action info
	private Vector<String> synchs;
	private int numSynchs;
	// Constant definitions from model file
	private Values mfConstants;

	// Labels + properties info
	protected List<Expression> labels;
	private List<Expression> properties;
	private List<Sampler> propertySamplers;

	// Current path info
	protected Path path;
	protected boolean onTheFly;

	// TODO: remove these now can get from path?
	protected State previousState;
	protected State currentState;

	// TODO: just temp storage?
	// (if so, remove 'current', 'prev' from names?)
	protected double currentStateRewards[];
	protected double previousTransitionRewards[];

	// List of currently available transitions
	protected TransitionList transitionList;

	// Updater object for model
	protected Updater updater;

	// Random number generator
	private RandomNumberGenerator rng;

	// ------------------------------------------------------------------------------
	// Basic setup
	// ------------------------------------------------------------------------------

	/**
	 * Constructor for the simulator engine.
	 */
	public SimulatorEngine(Prism prism)
	{
		this.prism = prism;
		setMainLog(prism.getMainLog());
		// TODO: finish this code once class members finalised
		rng = new RandomNumberGenerator();
		varIndices = null;
		modulesFile = null;
		mfConstants = null;
		properties = null;
		propertySamplers = null;
	}

	/**
	 * Set the log to which any output is sent. 
	 */
	public void setMainLog(PrismLog log)
	{
		mainLog = log;
	}

	/**
	 * Get access to the parent Prism object
	 */
	public Prism getPrism()
	{
		return prism;
	}

	// ------------------------------------------------------------------------------
	// Path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Create a new path for a model.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 */
	public void createNewPath(ModulesFile modulesFile) throws PrismException
	{
		// Store model
		loadModulesFile(modulesFile);
		// Create empty (full) path object associated with this model
		path = new PathFull(this, modulesFile);
		onTheFly = false;
	}

	/**
	 * Create a new on-the-fly path for a model.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 */
	public void createNewOnTheFlyPath(ModulesFile modulesFile) throws PrismException
	{
		// Store model
		loadModulesFile(modulesFile);
		// Create empty (on-the-fly_ path object associated with this model
		path = new PathOnTheFly(this, modulesFile);
		onTheFly = true;
	}

	/**
	 * Initialise (or re-initialise) the simulation path, starting with a specific (or random) initial state.
	 * @param initialState Initial state (if null, is selected randomly)
	 */
	public void initialisePath(State initialState) throws PrismException
	{
		// Store a copy of passed in state
		if (initialState != null) {
			currentState.copy(new State(initialState));
		}
		// Or pick a random one
		else {
			throw new PrismException("Random initial start state not yet supported");
		}
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Initialise stored path if necessary
		path.initialise(currentState, currentStateRewards);
		// Reset and then update samplers for any loaded properties
		resetSamplers();
		updateSamplers();
		// Generate updates for initial state 
		updater.calculateTransitions(currentState, transitionList);
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. If this is a continuous-time model, the time to be spent
	 * in the state before leaving is picked randomly.
	 */
	public void manualTransition(int index) throws PrismException
	{
		int i = transitionList.getChoiceIndexOfTransition(index);
		int offset = transitionList.getChoiceOffsetOfTransition(index);
		if (modelType.continuousTime()) {
			double r = transitionList.getProbabilitySum();
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
		int i = transitionList.getChoiceIndexOfTransition(index);
		int offset = transitionList.getChoiceOffsetOfTransition(index);
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
		Choice choice;
		int numChoices, i, j;
		double d, r;

		// Check for deadlock; if so, stop and return false
		numChoices = transitionList.getNumChoices();
		if (numChoices == 0)
			return false;
		//throw new PrismException("Deadlock found at state " + currentState.toString(modulesFile));

		switch (modelType) {
		case DTMC:
		case MDP:
			// Pick a random choice
			i = rng.randomUnifInt(numChoices);
			choice = transitionList.getChoice(i);
			// Pick a random transition from this choice
			d = rng.randomUnifDouble();
			j = choice.getIndexByProbabilitySum(d);
			// Execute
			executeTransition(i, j, -1);
			break;
		case CTMC:
			// Get sum of all rates
			r = transitionList.getProbabilitySum();
			// Pick a random number to determine choice/transition
			d = r * rng.randomUnifDouble();
			TransitionList.Ref ref = transitionList.new Ref();
			transitionList.getChoiceIndexByProbabilitySum(d, ref);
			// Execute
			executeTimedTransition(ref.i, ref.offset, rng.randomExpDouble(r), -1);
			break;
		}

		return true;
	}

	/**
	 * Select, at random, n successive transitions and execute them.
	 * For continuous-time models, the time to be spent in each state before leaving is also picked randomly.
	 * If a deadlock is found, the process stops.
	 * The function returns the number of transitions successfully taken. 
	 */
	public int automaticTransitions(int n) throws PrismException
	{
		int i = 0;
		while (i < n) {
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
	public int automaticTransitions(double time) throws PrismException
	{
		// For discrete-time models, this just results in ceil(time) steps being executed.
		if (!modelType.continuousTime()) {
			return automaticTransitions((int) Math.ceil(time));
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
		// Update previous/current state info
		if (step == 0)
			previousState.clear();
		else
			previousState.copy(path.getPreviousState());
		currentState = path.getCurrentState();
		// Recompute samplers for any loaded properties
		recomputeSamplers();
		// Generate updates for new current state 
		updater.calculateTransitions(currentState, transitionList);
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
		int step, n;
		PathFull pathFull = (PathFull) path;
		n = path.size();
		// Find the index of the step we are in at that point
		// i.e. the first state whose cumulative time on entering exceeds 'time'
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
		// Update previous state info (if required)
		// (No need to update current state info)
		if (path.size() == 1)
			previousState.clear();
		// Recompute samplers for any loaded properties
		recomputeSamplers();
		// (No need to re-generate updates for new current state) 
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
		updater.calculateTransitions(((PathFull) path).getState(step), transitionList);
	}

	/**
	 * Re-compute the transition table for the current state.
	 */
	public void computeTransitionsForCurrentState() throws PrismException
	{
		updater.calculateTransitions(currentState, transitionList);
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
	 */
	public int addProperty(Expression prop, PropertiesFile pf) throws PrismException
	{
		// Take a copy
		Expression propNew = prop.deepCopy();
		// Combine label lists from model/property file, then remove labels from property 
		LabelList combinedLabelList = (pf == null) ? modulesFile.getLabelList() : pf.getCombinedLabelList();
		propNew = (Expression) propNew.expandLabels(combinedLabelList);
		// Then get rid of any constants and simplify
		propNew = (Expression) propNew.replaceConstants(mfConstants);
		if (pf != null) {
			propNew = (Expression) propNew.replaceConstants(pf.getConstantValues());
		}
		propNew = (Expression) propNew.simplify();
		// Create sampler, update lists and return index
		properties.add(propNew);
		propertySamplers.add(Sampler.createSampler(propNew, modulesFile));
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
	public boolean queryIsInitial() throws PrismLangException
	{
		// Currently init...endinit is not supported so this is easy to check
		return path.getCurrentState().equals(new State(modulesFile.getInitialValues()));
	}

	/**
	 * Check whether a particular step in the current path is an initial state.
	 * @param step The index of the step to check for
	 * (Not applicable for on-the-fly paths)
	 */
	public boolean queryIsInitial(int step) throws PrismLangException
	{
		// Currently init...endinit is not supported so this is easy to check
		return ((PathFull) path).getState(step).equals(new State(modulesFile.getInitialValues()));
	}

	/**
	 * Check whether the current state is a deadlock.
	 */
	public boolean queryIsDeadlock() throws PrismLangException
	{
		return transitionList.getNumChoices() == 0;
	}

	/**
	 * Check whether a particular step in the current path is a deadlock.
	 * (Not applicable for on-the-fly paths)
	 * @param step The index of the step to check for
	 */
	public boolean queryIsDeadlock(int step) throws PrismLangException
	{
		// By definition, earlier states in the path cannot be deadlocks
		return step == path.size() ? queryIsDeadlock() : false;
	}

	/**
	 * Check the value for a particular property in the current path.
	 * If the value is not known yet, the result will be null.
	 * @param index The index of the property to check
	 */
	public Object queryProperty(int index)
	{
		Sampler sampler = propertySamplers.get(index);
		return sampler.isCurrentValueKnown() ? sampler.getCurrentValue() : null;
	}

	// ------------------------------------------------------------------------------
	// Private methods for path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Loads a new PRISM model into the simulator.
	 * @param modulesFile The parsed PRISM model
	 */
	private void loadModulesFile(ModulesFile modulesFile) throws PrismException
	{
		// Store model, some info and constants
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		this.mfConstants = modulesFile.getConstantValues();

		// Check for presence of system...endsystem
		if (modulesFile.getSystemDefn() != null) {
			throw new PrismException("Sorry - the simulator does not currently handle the system...endsystem construct");
		}

		// Get variable list (symbol table) for model 
		varList = modulesFile.createVarList();
		numVars = varList.getNumVars();

		// Build mapping between var names
		// TODO: push into VarList?
		varIndices = new HashMap<String, Integer>();
		for (int i = 0; i < numVars; i++) {
			varIndices.put(varList.getName(i), i);
		}

		// Get list of synchronising actions
		synchs = modulesFile.getSynchs();
		numSynchs = synchs.size();

		// Evaluate constants and optimise (a copy of) modules file for simulation
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(mfConstants).simplify();

		// Create state/transition storage
		previousState = new State(numVars);
		currentState = new State(numVars);
		currentStateRewards = new double[modulesFile.getNumRewardStructs()];
		previousTransitionRewards = new double[modulesFile.getNumRewardStructs()];
		transitionList = new TransitionList();

		// Create updater for model
		updater = new Updater(this, modulesFile);

		// Create storage for labels/properties
		labels = new ArrayList<Expression>();
		properties = new ArrayList<Expression>();
		propertySamplers = new ArrayList<Sampler>();
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
		// Get corresponding choice and, if required (for full paths), calculate transition index
		Choice choice = transitionList.getChoice(i);
		if (!onTheFly && index == -1)
			index = transitionList.getTotalIndexOfTransition(i, offset);
		// Compute its transition rewards
		updater.calculateTransitionRewards(currentState, choice, previousTransitionRewards);
		// Remember last state and compute next one (and its state rewards)
		previousState.copy(currentState);
		choice.computeTarget(offset, previousState, currentState);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Update path
		path.addStep(index, choice.getModuleOrActionIndex(), previousTransitionRewards, currentState, currentStateRewards);
		// Update samplers for any loaded properties
		updateSamplers();
		// Generate updates for next state 
		updater.calculateTransitions(currentState, transitionList);
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
		// Get corresponding choice and, if required (for full paths), calculate transition index
		Choice choice = transitionList.getChoice(i);
		if (!onTheFly && index == -1)
			index = transitionList.getTotalIndexOfTransition(i, offset);
		// Compute its transition rewards
		updater.calculateTransitionRewards(currentState, choice, previousTransitionRewards);
		// Remember last state and compute next one (and its state rewards)
		previousState.copy(currentState);
		choice.computeTarget(offset, previousState, currentState);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Update path
		path.addStep(time, index, choice.getModuleOrActionIndex(), previousTransitionRewards, currentState, currentStateRewards);
		// Update samplers for any loaded properties
		updateSamplers();
		// Generate updates for next state 
		updater.calculateTransitions(currentState, transitionList);
	}

	/**
	 * Reset samplers for any loaded properties that a new step has occurred.
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
	private void updateSamplers() throws PrismLangException
	{
		for (Sampler sampler : propertySamplers) {
			sampler.update(path);
		}
	}

	/**
	 * Recompute the state of samplers for any loaded properties based on the whole current path.
	 * (Not applicable for on-the-fly paths)
	 */
	private void recomputeSamplers() throws PrismLangException
	{
		int i, n;
		resetSamplers();
		n = path.size();
		PathFullPrefix prefix = new PathFullPrefix((PathFull) path, 0);
		for (i = 0; i <= n; i++) {
			prefix.setPrefixLength(i);
			for (Sampler sampler : propertySamplers) {
				sampler.update(prefix);
			}
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

	/**
	 * Returns the index of an action name, as stored by the simulator for the current model.
	 * Returns -1 if the action name does not exist. 
	 */
	public int getIndexOfAction(String name)
	{
		return synchs.indexOf(name);
	}

	// ------------------------------------------------------------------------------
	// Querying of current state and its available choices/transitions
	// ------------------------------------------------------------------------------

	/**
	 * Returns the current number of available choices.
	 */
	public int getNumChoices()
	{
		return transitionList.getNumChoices();
	}

	/**
	 * Returns the current (total) number of available transitions.
	 */
	public int getNumTransitions()
	{
		return transitionList.getNumTransitions();
	}

	/**
	 * Returns the current number of available transitions in choice i.
	 */
	public int getNumTransitions(int i)
	{
		return transitionList.getChoice(i).size();
	}

	/**
	 * Get the index of the choice containing a transition of a given index.
	 */
	public int getChoiceIndexOfTransition(int index)
	{
		return transitionList.getChoiceIndexOfTransition(index);
	}

	/**
	 * Get a string describing the action/module of a transition, specified by its index.
	 */
	public String getTransitionModuleOrAction(int index)
	{
		return transitionList.getTransitionModuleOrAction(index);
	}

	/**
	 * Get the probability/rate of a transition within a choice, specified by its index/offset.
	 */
	public double getTransitionProbability(int i, int offset) throws PrismException
	{
		double p = transitionList.getChoice(i).getProbability(offset);
		// For DTMCs, we need to normalise (over choices)
		return (modelType == ModelType.DTMC ? p / transitionList.getNumChoices() : p);
	}

	/**
	 * Get the probability/rate of a transition, specified by its index.
	 */
	public double getTransitionProbability(int index) throws PrismException
	{
		double p = transitionList.getTransitionProbability(index);
		// For DTMCs, we need to normalise (over choices)
		return (modelType == ModelType.DTMC ? p / transitionList.getNumChoices() : p);
	}

	/**
	 * Get a string describing the updates making up a transition, specified by its index.
	 */
	public String getTransitionUpdateString(int index)
	{
		return transitionList.getTransitionUpdateString(index);
	}

	/**
	 * Get the target (as a new State object) of a transition within a choice, specified by its index/offset.
	 */
	public State computeTransitionTarget(int i, int offset) throws PrismLangException
	{
		return transitionList.getChoice(i).computeTarget(offset, currentState);
	}

	/**
	 * Get the target of a transition (as a new State object), specified by its index.
	 */
	public State computeTransitionTarget(int index) throws PrismLangException
	{
		return transitionList.computeTransitionTarget(index, currentState);
	}

	// ------------------------------------------------------------------------------
	// Querying of current path (full or on-the-fly)
	// ------------------------------------------------------------------------------

	/**
	 * Get the size of the current path (number of steps; or number of states - 1).
	 */
	public int getPathSize()
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
	 * For paths with continuous-time info, get the total time elapsed so far
	 * (where zero time has been spent in the current (final) state).
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
	 * Get the index i of the action taken for a given step.
	 * If i>0, then i-1 is the index of an action label (0-indexed)
	 * If i<0, then -i-1 is the index of a module (0-indexed)
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public int getModuleOrActionIndexOfPathStep(int step)
	{
		return ((PathFull) path).getModuleOrActionIndex(step);
	}

	/**
	 * Get a string describing the action/module of a given step.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public String getModuleOrActionOfPathStep(int step)
	{
		return ((PathFull) path).getModuleOrAction(step);
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
		return false; // TODO
	}

	/**
	 * Check whether a deterministic loop (if present) starts.
	 */
	public int loopStart()
	{
		return -1; // TODO
	}

	/**
	 * Check whether a deterministic loop (if present) starts.
	 */
	public int loopEnd()
	{
		return -1; // TODO
	}

	/**
	 * Export the current path to a file in a simple space separated format.
	 * (Not applicable for on-the-fly paths)
	 * @param file: File to which the path should be exported to (mainLog if null).
	 */
	public void exportPath(File file) throws PrismException
	{
		exportPath(file, false, " ", null);
	}

	/**
	 * Export the current path to a file.
	 * (Not applicable for on-the-fly paths)
	 * @param file: File to which the path should be exported to (mainLog if null).
	 * @param timeCumul: Show time in cumulative form?
	 * @param colSep: String used to separate columns in display
	 * @param vars: Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportPath(File file, boolean timeCumul, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		PrismLog log;
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
		((PathFull) path).exportToLog(log, timeCumul, colSep, vars);
	}

	// ------------------------------------------------------------------------------
	// Model checking (approximate)
	// ------------------------------------------------------------------------------

	/**
	 * Check whether a property is suitable for approximate model checking using the simulator.
	 * If not, an explanatory error message is thrown as an exception.
	 */
	public void checkPropertyForSimulation(Expression prop) throws PrismException
	{
		// Simulator can only be applied to P=? or R=? properties
		boolean ok = true;
		Expression expr = null;
		if (!(prop instanceof ExpressionProb || prop instanceof ExpressionReward))
			ok = false;
		else if (prop instanceof ExpressionProb) {
			if ((((ExpressionProb) prop).getProb() != null))
				ok = false;
			expr = ((ExpressionProb) prop).getExpression();
		} else if (prop instanceof ExpressionReward) {
			if ((((ExpressionReward) prop).getReward() != null))
				ok = false;
			expr = ((ExpressionReward) prop).getExpression();
		}
		if (!ok)
			throw new PrismException("Simulator can only handle P=? or R=? properties");

		// Check that there are no nested probabilistic operators
		try {
			expr.accept(new ASTTraverse()
			{
				public void visitPre(ExpressionProb e) throws PrismLangException
				{
					throw new PrismLangException("");
				}

				public void visitPre(ExpressionReward e) throws PrismLangException
				{
					throw new PrismLangException("");
				}

				public void visitPre(ExpressionSS e) throws PrismLangException
				{
					throw new PrismLangException("");
				}
			});
		} catch (PrismLangException e) {
			throw new PrismException("Simulator cannot handle nested P, R or S operators");
		}
	}

	// Methods to compute parameters for simulation

	public double computeSimulationApproximation(double confid, int numSamples)
	{
		return Math.sqrt((4.0 * PrismUtils.log(2.0 / confid, 10)) / numSamples);
	}

	public double computeSimulationConfidence(double approx, int numSamples)
	{
		return 2.0 / Math.pow(10, (numSamples * approx * approx) / 4.0);
	}

	public int computeSimulationNumSamples(double approx, double confid)
	{
		return (int) Math.ceil(4.0 * PrismUtils.log(2.0 / confid, 10) / (approx * approx));
	}

	/**
	 * Perform approximate model checking for a single property.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 * 
	 * <P>
	 * The returned result is:
	 * <UL>
	 * <LI> A Double object: for =?[] properties
	 * </UL>
	 * 
	 * @param propertiesFile
	 *            The PropertiesFile containing the property of interest, constants defined.
	 * @param expr
	 *            The property of interest
	 * @param initialState
	 *            The initial state for the sampling.
	 * @param noIterations
	 *            The number of iterations for the sampling algorithm
	 * @param maxPathLength
	 *            the maximum path length for the sampling algorithm.
	 * @throws PrismException
	 *             if anything goes wrong.
	 * @return the result.
	 */
	public Object modelCheckSingleProperty(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr, State initialState, int numIters,
			int maxPathLength) throws PrismException
	{
		ArrayList<Expression> exprs;
		Object res[];

		exprs = new ArrayList<Expression>();
		exprs.add(expr);
		res = modelCheckMultipleProperties(modulesFile, propertiesFile, exprs, initialState, numIters, maxPathLength);

		if (res[0] instanceof PrismException)
			throw (PrismException) res[0];
		else
			return res[0];
	}

	/**
	 * Perform approximate model checking of properties on a model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, a random initial state each time.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param modulesFile Model for simulation, constants defined
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param exprs The properties to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param numIters The number of iterations (i.e. number of samples to generate)
	 * @param maxPathLength The maximum path length for sampling
	 */
	public Object[] modelCheckMultipleProperties(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Expression> exprs, State initialState,
			int numIters, int maxPathLength) throws PrismException
	{
		createNewOnTheFlyPath(modulesFile);

		Object[] results = new Object[exprs.size()];
		int[] indices = new int[exprs.size()];

		// Add the properties to the simulator (after a check that they are valid)
		int validPropsCount = 0;
		for (int i = 0; i < exprs.size(); i++) {
			try {
				checkPropertyForSimulation((Expression) exprs.get(i));
				indices[i] = addProperty((Expression) exprs.get(i), propertiesFile);
				if (indices[i] >= 0)
					validPropsCount++;
			} catch (PrismException e) {
				results[i] = e;
				indices[i] = -1;
			}
		}

		// as long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			doSampling(initialState, numIters, maxPathLength);
		}

		// process the results
		for (int i = 0; i < results.length; i++) {
			// if we have already stored an error for this property, keep it as the result
			if (!(results[i] instanceof PrismException))
				results[i] = propertySamplers.get(indices[i]).getMeanValue();
		}

		// display results to log
		if (results.length == 1) {
			if (!(results[0] instanceof PrismException))
				mainLog.println("\nResult: " + results[0]);
		} else {
			mainLog.println("\nResults:");
			for (int i = 0; i < results.length; i++)
				mainLog.println(exprs.get(i) + " : " + results[i]);
		}

		return results;
	}

	/**
	 * Deals with all of the logistics of performing an experiment and storing the results in an appropriate
	 * <tt>ResultsCollection</tt> object.
	 * 
	 * @param exp
	 *            the experiment in which to store the results.
	 * @param modulesFile
	 *            The ModulesFile, constants already defined.
	 * @param propertiesFile
	 *            The PropertiesFile containing the property of interest, constants defined.
	 * @param undefinedConstants
	 *            defining what should be done for the experiment
	 * @param resultsCollection
	 *            where the results should be stored
	 * @param propertyToCheck
	 *            the property to check for the experiment
	 * @param initialState
	 *            The initial state for the sampling.
	 * @param maxPathLength
	 *            the maximum path length for the sampling algorithm.
	 * @param noIterations
	 *            The number of iterations for the sampling algorithm
	 * @throws PrismException
	 *             if something goes wrong with the results reporting
	 * @throws InterruptedException
	 *             if the thread is interrupted
	 * @throws PrismException
	 *             if something goes wrong with the sampling algorithm
	 */
	public void modelCheckExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile, UndefinedConstants undefinedConstants,
			ResultsCollection resultsCollection, Expression propertyToCheck, State initialState, int maxPathLength, int noIterations) throws PrismException,
			InterruptedException, PrismException
	{
		int n;
		Values definedPFConstants = new Values();

		createNewOnTheFlyPath(modulesFile);

		n = undefinedConstants.getNumPropertyIterations();
		Object[] results = new Object[n];
		Values[] pfcs = new Values[n];
		int[] indices = new int[n];

		// Add the properties to the simulator (after a check that they are valid)
		int validPropsCount = 0;
		for (int i = 0; i < n; i++) {
			definedPFConstants = undefinedConstants.getPFConstantValues();
			pfcs[i] = definedPFConstants;
			propertiesFile.setUndefinedConstants(definedPFConstants);
			try {
				checkPropertyForSimulation(propertyToCheck);
				indices[i] = addProperty(propertyToCheck, propertiesFile);
				if (indices[i] >= 0)
					validPropsCount++;
			} catch (PrismException e) {
				results[i] = e;
				indices[i] = -1;
			}
			undefinedConstants.iterateProperty();
		}

		// as long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			initialisePath(initialState);
			//doSampling(noIterations, maxPathLength);
		}

		// process the results
		for (int i = 0; i < n; i++) {
			// if we have already stored an error for this property, keep it as the result
			if (!(results[i] instanceof Exception))
				results[i] = propertySamplers.get(indices[i]).getMeanValue();
			// store it in the ResultsCollection
			resultsCollection.setResult(undefinedConstants.getMFConstantValues(), pfcs[i], results[i]);
		}

		// display results to log
		if (indices.length == 1) {
			if (!(results[0] instanceof Exception))
				mainLog.println("\nResult: " + results[0]);
		} else {
			mainLog.println("\nResults:");
			mainLog.print(resultsCollection.toStringPartial(undefinedConstants.getMFConstantValues(), true, " ", " : ", false));
		}
	}

	private void doSampling(State initialState, int numIters, int maxPathLength) throws PrismException
	{
		int i, iters;
		// Flags
		boolean stoppedEarly = false;
		boolean deadlocksFound = false;
		boolean allKnown = false;
		boolean shouldStopSampling = false;
		// Path stats
		double avgPathLength = 0;
		int minPathFound = 0, maxPathFound = 0;
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
		while (!shouldStopSampling && iters < numIters) {

			// Display progress
			percentageDone = ((10 * (iters)) / numIters) * 10;
			if (percentageDone > lastPercentageDone) {
				lastPercentageDone = percentageDone;
				mainLog.print(" " + lastPercentageDone + "%");
				mainLog.flush();
			}

			iters++;

			// Start the new path for this iteration (sample)
			initialisePath(initialState);
			
			// Generate a path, up to at most maxPathLength steps
			for (i = 0; i < maxPathLength; i++) {
				// See if all samplers have established a value; if so, stop.
				allKnown = true;
				for (Sampler sampler : propertySamplers) {
					if (!sampler.isCurrentValueKnown())
						allKnown = false;
				}
				if (allKnown)
					break;
				// Make a random transition
				automaticTransition();
			}

			// record if we found any deadlocks (can check this outside path gen loop because never escape deadlocks)
			//if (loop_detection->Is_Deadlock()) deadlocksFound = true;
			// TODO

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

		// print a warning if deadlocks occurred at any point
		if (deadlocksFound)
			mainLog.print("\nWarning: Deadlocks were found during simulation: self-loops were added\n");

		// print a warning if simulation was stopped by the user
		if (shouldStopSampling)
			mainLog.print("\nWarning: Simulation was terminated before completion.\n");

		//write to feedback file with true to indicate that we have finished sampling
		//Write_Feedback(iteration_counter, numIters, true);

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
