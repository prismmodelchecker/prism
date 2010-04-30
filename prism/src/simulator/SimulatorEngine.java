//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

import parser.*;
import parser.ast.*;
import parser.type.*;
import parser.visitor.ASTTraverse;
import prism.*;

/**
 * The SimulatorEngine class uses the JNI to provide a Java interface to the PRISM simulator engine library, written in
 * c++. There are two ways to use this API:
 * <UL>
 * <LI> To access the engine directly to perform tasks such as model exploration.</LI>
 * <LI> To perform Monte-Carlo sampling techniques for approximate property verification.</LI>
 * <LI> To generate whole random paths at once.</LI>
 * </UL>
 * Each type of use only builds the relevant data structures of the simulator engine.
 * 
 * <H3>Model Exploration</H3>
 * 
 * In order to load a PRISM model into the simulator engine, a valid <tt>ModulesFile</tt>, that has had all of its
 * constants defined, can be loaded using the <tt>startNewPath</tt> method. If the simulator engine is to be used to
 * reason with path properties, a PropertiesFile object is passed. The initial state must also be provided.
 * 
 * At this point, the simulator engine will automatically calculate the set of updates to that initial state. This
 * update set can be queried by the methods such as:
 * 
 * <UL>
 * <LI> <tt>getNumUpdates()</tt></LI>
 * <LI> <tt>getActionLabelOfUpdate(int index)</tt></LI>
 * <LI> <tt>getProbabilityOfUpdate(int index)</tt></LI>
 * <LI> <tt>getNumAssignmentsOfUpdate(int index)</tt></LI>
 * <LI> <tt>getAssignmentVariableIndexOfUpdate(int updateIndex,
 *                assignmentIndex)</tt></LI>
 * <LI> <tt>getAssignmentValueOfUpdate(int updateIndex,
 *                assignmentIndex)</tt></LI>
 * </UL>
 * 
 * It should be noted that all references to variables, action labels etc... are of the simulators stored index. There
 * are a number of methods provided to convert the string representations of these items to their appropriate indices:
 * 
 * <UL>
 * <LI> <tt>getIndexOfVar(String var)</tt></LI>
 * <LI> <tt>getIndicesOfAction(String var)</tt></LI>
 * </UL>
 * 
 * At this point, there are three options:
 * 
 * <UL>
 * <LI> Reset the path with a new initial state using the <tt>restartPath(Values initialState)</tt> method.</LI>
 * <LI> Tell the simulator engine to update the current state by executing the assignments of a manually selected choice
 * from the update set. This is done by the <tt>manualUpdate(int index)</tt> for dtmcs and mdps and the
 * <tt>manualUpdate(int index, double timeTaken)</tt> for ctmcs.</LI>
 * <LI> Request that the simulator engine update the current state with a randomly sampled choice from the update set.
 * This can be repeated n times. This is done by the <tt>automaticChoices(int n, ...)</tt> method.
 * </UL>
 * 
 * The simulator engine maintains an execution path of all of the previous states of the system. It is possible to
 * access the information using the following methods:
 * 
 * <UL>
 * <LI> <tt>getDataSize()</tt></LI>
 * <LI> <tt>getPathData(int pathIndex, int variableIndex)</tt></LI>
 * <LI> <tt>getTimeSpentInPathState(int pathIndex)</tt></LI>
 * <LI> <tt>getCumulativeTimeSpentInPathState(int pathIndex)</tt></LI>
 * <LI> <tt>getStateRewardOfPathState(int pathIndex, int i)</tt></LI>
 * <LI> <tt>getTotalStateRewardOfPathState(int pathIndex, int i)</tt> (Cumulative)</LI>
 * <LI> <tt>getTransitionRewardOfPathState(int pathIndex, int i)</tt></LI>
 * <LI> <tt>getTotalTransitionRewardOfPathState(int pathIndex, int i)</tt> (Cumulative)</LI>
 * </UL>
 * 
 * The simulator engine automatically detects loops in execution paths, and there are a couple of methods used to query
 * this: <tt>boolean isPathLooping()</tt> and <tt>int loopStart()</tt>
 * 
 * Functionality to backtrack to previous states of the path is provided by the <tt>backtrack(int toPathIndex)</tt>
 * method and the ability to remove states from the path before a given index is provids by the
 * <tt>removePrecedingStates</tt> method.
 * 
 * There are two ways in which the path can be analysed:
 * 
 * <UL>
 * <LI> The engine can be loaded with PCTL/CSL formulae via the <tt>addPCTLRewardFormula</tt> and
 * <tt>addPCTLProbFormula</tt> methods and the <tt>queryPathFormula(int index)</tt> and
 * <tt>queryPathFormulaNumeric(int index)</tt> methods can be used to obtain results over the current execution path.
 * Note that the <tt>PropertiesFile</tt> object for these properties should have been provided so that any constant
 * values can be obtained. </LI>
 * <LI> The engine can be loaded with state proposition expressions via the <tt>loadProposition</tt> method. These
 * propositions can be used to determine whether a state at a particular index in the execution path satisfies a
 * particular (potentially complex) boolean expression (<tt>queryProposition(int index, int propIndex)</tt>).
 * Further functionality is provided to see whether these states are initial (<tt>queryIsIntitial(int index)</tt>)
 * or deadlock (<tt>queryIsDeadlock(int index)</tt>) states.
 * </UL>
 * 
 * The <tt>deallocateEngine()</tt> method should be used after using the engine, or before a new model, or path is
 * started. This simply cleans up the memory in the c++ engine.
 * 
 * <H3>Monte-Carlo Sampling</H3>
 * 
 * Three methods are provided to make the interface cleaner, if it is only used for approximate model checking. Each
 * method deals with all model loading, algorithm execution and tidying up afterwards. The three methods are:
 * 
 * <UL>
 * <LI> <tt>modelCheckSingleProperty</tt>. Loads the given <tt>ModuleFile</tt> and PCTL/CSL formula, provided the
 * constants are defined. The initial state in the <tt>Value</tt> object is loaded into the simulator and then a given
 * number of randomly generated paths are computed up to the given maximum path length (including loop detection when
 * appropriate). The property is evaluated over each path and the average probability or reward is returned as a Double
 * object.</LI>
 * <LI> <tt>modelCheckMultipleProperties</tt>. Similar to the single property method, except takes advantage of the
 * fact that it is usually more efficient to deal to multiple properties at the same time.
 * <LI> <tt>modelCheckExperiment</tt>. Deals with all of the logistics of performing an experiment and storing the
 * results in an appropriate <tt>ResultsCollection</tt> object.
 * </UL>
 * 
 * <H3>Path Generation</H3>
 * 
 * The following utility method is used for generating (and exporting) a single path satisfying certain criteria:
 * 
 * <UL>
 * <LI> <tt>generateSimulationPath</tt>.
 * </UL>
 * 
 * @author Andrew Hinton
 */

// REMOVED:
// exportBinary functions

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
	// Synchronising action info
	private Vector<String> synchs;
	private int numSynchs;

	// Properties info
	private PropertiesFile propertiesFile;
	private List<ExpressionTemporal> pathProps;
	private Values propertyConstants;
	private ArrayList loadedProperties;

	// Updater object for model
	protected Updater updater;
	protected TransitionList transitionList;
	// Random number generator
	private RandomNumberGenerator rng;

	// TODO: ... more from below

	// NEW STUFF:
	protected boolean onTheFly;

	protected State lastState;
	protected State currentState;
	protected double currentStateRewards[];

	// PATH:
	protected Path path = null;
	// TRANSITIONS:


	protected List<Expression> labels;

	// ------------------------------------------------------------------------------
	// CONSTANTS
	// ------------------------------------------------------------------------------

	// Errors
	/**
	 * Used by the simulator engine to report that something has caused an exception. The actual content of the error
	 * can be queried using the <tt>getLastErrorMessage()</tt> method.
	 */
	public static final int ERROR = -1;
	/**
	 * Used by the simulator engine to report that an index parameter was out of range.
	 */
	public static final int OUTOFRANGE = -1;
	/**
	 * Used by the simulator engine to indicate that a returned pointer is NULL.
	 */
	public static final int NULL = 0;

	// Model type
	/**
	 * A constant for the model type
	 */
	public static final int NOT_LOADED = 0;

	// Undefined values
	/**
	 * Used throughout the simulator for undefined integers
	 */
	public static final int UNDEFINED_INT = Integer.MIN_VALUE + 1;
	/**
	 * Used throughout the simulator for undefined doubles
	 */
	public static final double UNDEFINED_DOUBLE = -10E23f;

	// ------------------------------------------------------------------------------
	// CLASS MEMBERS
	// ------------------------------------------------------------------------------

	private Map<String, Integer> varIndices;

	// Current model
	private Values constants;


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
		propertiesFile = null;
		constants = null;
		propertyConstants = null;
		loadedProperties = null;
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
	 * Create a new path for a model and (possibly) some properties.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 * @param propertiesFile Properties to check during simulation TODO: change?
	 */
	public void createNewPath(ModulesFile modulesFile, PropertiesFile propertiesFile) throws PrismException
	{
		// Store model/properties
		loadModulesFile(modulesFile);
		this.propertiesFile = (propertiesFile == null) ? new PropertiesFile(modulesFile) : propertiesFile;
		propertyConstants = this.propertiesFile.getConstantValues();
		// Create empty path object associated with this model
		path = new Path(this, modulesFile);
		// This is not on-the-fly
		onTheFly = false;
	}

	/**
	 * Create a new on-the-fly path for a model and (possibly) some properties.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 * @param propertiesFile Properties to check during simulation TODO: change?
	 */
	public void createNewOnTheFlyPath(ModulesFile modulesFile, PropertiesFile propertiesFile) throws PrismException
	{
		// Store model/properties
		loadModulesFile(modulesFile);
		this.propertiesFile = (propertiesFile == null) ? new PropertiesFile(modulesFile) : propertiesFile;
		propertyConstants = this.propertiesFile.getConstantValues();
		// This is on-the-fly
		onTheFly = true;
	}

	/**
	 * Initialise (or re-initialise) the simulation path, starting with a specific (or random) initial state.
	 * @param initialState Initial state (if null, is selected randomly)
	 */
	public void initialisePath(Values initialState) throws PrismException
	{
		// TODO: need this method?
		initialisePath(new State(initialState));
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
			// TODO
			//currentState...
			throw new PrismException("Random initial start state not yet supported");
		}
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Initialise stored path if necessary
		if (!onTheFly)
			path.initialise(currentState, currentStateRewards);
		// Generate updates for initial state 
		updater.calculateTransitions(currentState, transitionList);
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. If this is a continuous time model, the time to be spent
	 * in the state before leaving is picked randomly.
	 */
	public void manualUpdate(int index) throws PrismException
	{
		int i = transitionList.getChoiceIndexOfTransition(index);
		int offset = transitionList.getChoiceOffsetOfTransition(index);
		if (modelType.continuousTime())
			executeTimedTransition(i, offset, -1, index);
		else
			executeTransition(i, offset, index);
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. In addition, specify the amount of time to be spent in
	 * the current state before this transition occurs. If -1, this is picked randomly.
	 * [continuous-time models only]
	 */
	public void manualUpdate(int index, double time) throws PrismException
	{
		int i = transitionList.getChoiceIndexOfTransition(index);
		int offset = transitionList.getChoiceOffsetOfTransition(index);
		executeTimedTransition(i, offset, time, index);
	}

	/**
	 * This function makes n automatic choices of updates to the global state.
	 * 
	 * @param n
	 *            the number of automatic choices to be made.
	 * @throws PrismException
	 *             if something goes wrong when updating the state.
	 */
	public void automaticChoices(int n) throws PrismException
	{
		automaticChoices(n, true);
	}

	public void automaticChoices(int n, boolean detect) throws PrismException
	{
		int i;
		for (i = 0; i < n; i++)
			automaticChoice(detect);
	}

	public void automaticChoice(boolean detect) throws PrismException
	{
		// just one for now...

		Choice choice;
		int numChoices, i, j;
		double d, r;

		switch (modelType) {
		case DTMC:
		case MDP:
			// Check for deadlock
			numChoices = transitionList.getNumChoices();
			if (numChoices == 0)
				throw new PrismException("Deadlock found at state " + currentState.toString(modulesFile));
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
			// Check for deadlock
			numChoices = transitionList.getNumChoices();
			if (numChoices == 0)
				throw new PrismException("Deadlock found at state " + currentState.toString(modulesFile));
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

	}

	/**
	 * This function makes a number of automatic choices of updates to the global state, untill `time' has passed.
	 * 
	 * @param time		Values v = path.get(pathLength);

	 *            is the length of time to pass.
	 * @throws PrismException
	 *             if something goes wrong when updating the state.
	 */
	public void automaticChoices(double time) throws PrismException
	{
		automaticChoices(time, true);
	}

	/**
	 * This function makes n automatic choices of updates to the global state, untill `time' has passed.
	 * 
	 * @param time
	 *            is the length of time to pass.
	 * @param detect
	 *            whether to employ loop detection.
	 * @throws PrismException
	 *             if something goes wrong when updating the state.
	 */
	public void automaticChoices(double time, boolean detect) throws PrismException
	{
		/*int result = doAutomaticChoices(time, detect);
		if (result == ERROR)
			throw new PrismException(getLastErrorMessage());*/
	}

	/**
	 * This function backtracks the current path to the state of the given index
	 * 
	 * @param step
	 *            the path index to backtrack to.
	 * @throws PrismException
	 *             is something goes wrong when backtracking.
	 */
	public void backtrack(int step) throws PrismException
	{
		int result = doBacktrack(step);
		if (result == ERROR)
			throw new PrismException(getLastErrorMessage());
	}

	/**
	 * This function backtracks the current path to such that the cumulative time is equal or less than the time
	 * parameter.
	 * 
	 * @param time
	 *            the cumulative time to backtrack to.
	 * @throws PrismException
	 *             is something goes wrong when backtracking.
	 */
	public void backtrack(double time) throws PrismException
	{
		// Backtrack(time) in simpath.cc
		int result = doBacktrack(time);
		if (result == ERROR)
			throw new PrismException(getLastErrorMessage());
	}

	/**
	 * Asks the c++ engine to backtrack to the given step. Returns OUTOFRANGE (=-1) if step is out of range
	 */
	private static native int doBacktrack(int step);

	/**
	 * Asks the c++ engine to backtrack to some given time of the path. Returns OUTOFRANGE (=-1) if time is out of range
	 */
	private static native int doBacktrack(double time);

	/**
	 * This function removes states of the path that precede those of the given index
	 * 
	 * @param step
	 *            the index before which the states should be removed.
	 * @throws PrismException
	 *             if anything goes wrong with the state removal.
	 */
	public void removePrecedingStates(int step) throws PrismException
	{
		int result = doRemovePrecedingStates(step);
		if (result == ERROR)
			throw new PrismException(getLastErrorMessage());
	}

	/**
	 * Asks the c++ engine to remove states which precde the given step. Returns OUTOFRANGE (=-1) if step is out of
	 * range
	 */
	private static native int doRemovePrecedingStates(int step);

	/**
	 * Compute the transition table for an earlier step in the path.
	 */
	public void computeTransitionsForStep(int step) throws PrismException
	{
		updater.calculateTransitions(path.getState(step), transitionList);
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
		// Take a copy, get rid of any constants and simplify
		Expression labelNew = label.deepCopy();
		labelNew = (Expression) labelNew.replaceConstants(constants);
		labelNew = (Expression) labelNew.simplify();
		labels.add(labelNew);
		return labels.size() - 1;
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
		labelNew = (Expression) labelNew.replaceConstants(constants);
		labelNew = (Expression) labelNew.replaceConstants(pf.getConstantValues());
		labelNew = (Expression) labelNew.simplify();
		labels.add(labelNew);
		return labels.size() - 1;
	}

	/**
	 * Add a (path) property to the simulator, whose value will be computed during path generation.
	 * The resulting index of the property is returned: this is used for later queries about the property.
	 */
	private int addProperty(Expression prop)
	{
		// clone, converttountil, simplify ???
		
		// TODO
		return -1;
	}

	/**
	 * Get the current value of a previously added label (specified by its index).
	 */
	public boolean queryLabel(int index) throws PrismLangException
	{
		return labels.get(index).evaluateBoolean(currentState);
	}

	/**
	 * Get the value, at the specified path step, of a previously added label (specified by its index).
	 */
	public boolean queryLabel(int index, int step) throws PrismLangException
	{
		return labels.get(index).evaluateBoolean(path.getState(step));
	}

	/**
	 * Check whether the current state is an initial state.
	 */
	public boolean queryIsInitial() throws PrismLangException
	{
		// Currently init...endinit is not supported so this is easy to check
		return currentState.equals(new State(modulesFile.getInitialValues()));
	}

	/**
	 * Check whether a particular step in the current path is an initial state.
	 */
	public boolean queryIsInitial(int step) throws PrismLangException
	{
		// Currently init...endinit is not supported so this is easy to check
		return path.getState(step).equals(new State(modulesFile.getInitialValues()));
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
	 */
	public boolean queryIsDeadlock(int step) throws PrismLangException
	{
		// By definition, earlier states in the path cannot be deadlocks
		return step == path.size() ? queryIsDeadlock() : false;
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
		this.constants = modulesFile.getConstantValues();

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
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(constants).simplify();
		mainLog.println(modulesFile);
		
		// Create state/transition storage
		lastState = new State(numVars);
		currentState = new State(numVars);
		currentStateRewards = new double[modulesFile.getNumRewardStructs()];
		transitionList = new TransitionList();

		// Create updater for model
		updater = new Updater(this, modulesFile);

		// Create storage for labels
		labels = new ArrayList<Expression>();
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
		// Get corresponding choice
		Choice choice = transitionList.getChoice(i);
		// Remember last state and compute next one (and its state rewards)
		lastState.copy(currentState);
		choice.computeTarget(offset, lastState, currentState);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Store path info (if necessary)
		if (!onTheFly) {
			if (index == -1)
				index = transitionList.getTotalIndexOfTransition(i, offset);
			path.addStep(index, choice.getAction(), currentStateRewards, currentState, currentStateRewards);
			// TODO: first currentStateRewards in above should be new *trans* rewards!
		}
		// Generate updates for next state 
		updater.calculateTransitions(currentState, transitionList);
	}

	/**
	 * Execute a (timed) transition from the current transition list and update path (if being stored).
	 * Transition is specified by index of its choice and offset within it. If known, its index
	 * (within the whole list) can optionally be specified (since this may be needed for storage
	 * in the path). If this is -1, it will be computed automatically if needed.
	 * In addition, the amount of time to be spent in the current state before this transition occurs
	 * should be specified. If -1, this is picked randomly.
	 * [continuous-time models only]
	 * @param i Index of choice containing transition to execute
	 * @param offset Index within choice of transition to execute
	 * @param time Time for transition
	 * @param index (Optionally) index of transition within whole list (-1 if unknown)
	 */
	private void executeTimedTransition(int i, int offset, double time, int index) throws PrismException
	{
		// Get corresponding choice
		Choice choice = transitionList.getChoice(i);
		// Remember last state and compute next one (and its state rewards)
		lastState.copy(currentState);
		choice.computeTarget(offset, lastState, currentState);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Store path info (if necessary)
		if (!onTheFly) {
			if (index == -1)
				index = transitionList.getTotalIndexOfTransition(i, offset);
			path.addStep(time, index, choice.getAction(), currentStateRewards, currentState, currentStateRewards);
			// TODO: first currentStateRewards in above should be new *trans* rewards!
		}
		// Generate updates for next state 
		updater.calculateTransitions(currentState, transitionList);
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

	/**
	 * Provides access to the loaded <ModulesFile>'s constants.
	 * @return the loaded <ModulesFile>'s constants.
	 */
	// TODO: remove?
	public Values getConstants()
	{
		if (constants == null) {
			constants = new Values();
		}
		return constants;
	}

	// ------------------------------------------------------------------------------
	// Querying of current state and its available choices/transitions
	// ------------------------------------------------------------------------------

	/**
	 * Returns the current state being explored by the simulator.
	 */
	public State getCurrentState()
	{
		return currentState;
	}

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

	/**
	 * TODO
	 */
	public String getTransitionModuleOrAction(int index)
	{
		return transitionList.getTransitionActionString(index);
		//return transitionList.getChoiceOfTransition(index).getAction();
	}

	/**
	 * Returns a string representation of the assignments for the current update at the given index.
	 * TODO
	 */
	public String getAssignmentDescriptionOfUpdate(int index)
	{
		return transitionList.getTransitionUpdateString(index);
	}

	// ------------------------------------------------------------------------------
	// Querying of current path
	// ------------------------------------------------------------------------------

	/**
	 * Returns the number of states stored in the current path table.
	 * 
	 * @return the number of states stored in the current path table.
	 */
	public int getPathSize()
	{
		return path.size();
	}

	/**
	 * Returns the value stored for the variable at varIndex at the path index: stateIndex.
	 * 
	 * @param varIndex
	 *            the index of the variable of interest.
	 * @param stateIndex
	 *            the index of the path state of interest
	 * @return the value stored for the variable at varIndes at the given stateIndex.
	 */
	public Object getPathData(int varIndex, int stateIndex)
	{
		return path.getState(stateIndex).varValues[varIndex];
	}

	public String getActionOfPathStep(int step)
	{
		return path.getAction(step);
	}

	/**
	 * Returns the time spent in the state at the given path index.
	 */
	public double getTimeSpentInPathState(int index)
	{
		return path.getTime(index);
	}

	/**
	 * Returns the cumulative time spent in the states up to (and including) a given path index.
	 */
	public double getCumulativeTimeSpentInPathState(int index)
	{
		return path.getCumulativeTime(index);
	}

	/**
	 * Returns the ith state reward of the state at the given path index.
	 * 
	 * @param stateIndex
	 *            the index of the path state of interest
	 * @param i
	 *            the index of the reward structure
	 * @return the state reward of the state at the given path index.
	 */
	public double getStateRewardOfPathState(int step, int stateIndex)
	{
		return path.getStateReward(step, stateIndex);
	}

	/**
	 * Returns the ith transition reward of (moving out of) the state at the given path index.
	 * 
	 * @param stateIndex
	 *            the index of the path state of interest
	 * @param i
	 *            the index of the reward structure
	 * @return the transition reward of (moving out of) the state at the given path index.
	 */
	public double getTransitionRewardOfPathState(int stateIndex, int i)
	{
		return 99;
	}

	/**
	 * Cumulative version of getStateRewardOfPathState.
	 */
	public double getTotalStateRewardOfPathState(int stateIndex, int i)
	{
		return 99;
	}

	/**
	 * Cumulative version of getTransitionRewardOfPathState.
	 */
	public double getTotalTransitionRewardOfPathState(int stateIndex, int i)
	{
		return 99;
	}

	/**
	 * Returns the total path time.
	 * 
	 * @return the total path time.
	 */
	public double getTotalPathTime()
	{
		return 99;
	}

	/**
	 * Returns the total path reward.
	 * 
	 * @return the total path reward.
	 */
	public double getTotalPathReward(int i)
	{
		return 99;
	}

	/**
	 * Returns the total state reward for the path.
	 * 
	 * @return the total state reward for the path.
	 */
	public double getTotalStateReward(int i)
	{
		return 99;
	}

	/**
	 * Returns the total transition reward for the path.
	 * 
	 * @return the total transition reward for the path.
	 */
	public double getTotalTransitionReward(int i)
	{
		return 99;
	}

	/**
	 * Returns whether the current path is in a looping state
	 * 
	 * @return whether the current path is in a looping state
	 */
	// TODO
	public boolean isPathLooping()
	{
		return false;
	}

	/**
	 * Returns where a loop starts
	 * 
	 * @return where a loop starts
	 */
	public native int loopStart();

	/**
	 * Returns where a loop ends
	 * 
	 * @return where a loop ends
	 */
	public static native int loopEnd();

	/**
	 * Returns the index of the update chosen for an earlier state in the path.
	 */
	public int getChosenIndexOfOldUpdate(int step)
	{
		return path.getChoice(step);
	}

	/**
	 * Exports the current path to a file in a simple space separated format.
	 * @param file: File to which the path should be exported to (mainLog if null).
	 */
	public void exportPath(File file) throws PrismException
	{
		if (path == null)
			throw new PrismException("There is no path to export");
		exportPath(file, false, " ", null);
	}

	/**
	 * Exports the current path to a file.
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
		path.exportToLog(log, timeCumul, colSep, vars);
	}

	// ------------------------------------------------------------------------------
	// PROPERTIES AND SAMPLING (not yet sorted)
	// ------------------------------------------------------------------------------

	/**
	 * Provides access to the propertyConstants
	 * 
	 * @return the propertyConstants
	 */
	public Values getPropertyConstants()
	{
		if (propertyConstants == null) {
			propertyConstants = new Values();
		}
		return propertyConstants;
	}

	/**
	 * Gets (double) result from simulator for a given property/index and process result
	 */
	private Object processSamplingResult(Expression expr, int index)
	{
		if (index == -1) {
			return new PrismException("Property cannot be handled by the PRISM simulator");
		} else {
			double result = getSamplingResult(index);
			if (result == UNDEFINED_DOUBLE)
				result = Double.POSITIVE_INFINITY;
			// only handle P=?/R=? properties (we could check against the bounds in P>p etc. but results would be a bit
			// dubious)
			if (expr instanceof ExpressionProb) {
				if (((ExpressionProb) expr).getProb() == null) {
					return new Double(result);
				} else {
					return new PrismException("Property cannot be handled by the PRISM simulator");
				}
			} else if (expr instanceof ExpressionReward) {
				if (((ExpressionReward) expr).getReward() == null) {
					return new Double(result);
				} else {
					return new PrismException("Property cannot be handled by the PRISM simulator");
				}
			} else {
				return new PrismException("Property cannot be handled by the PRISM simulator");
			}
		}
	}

	// PCTL Stuff

	/**
	 * This method completely encapsulates the model checking of a property so long as: prerequisites modulesFile
	 * constants should all be defined propertiesFile constants should all be defined
	 * 
	 * <P>
	 * The returned result is:
	 * <UL>
	 * <LI> A Double object: for =?[] properties
	 * </UL>
	 * 
	 * @param modulesFile
	 *            The ModulesFile, constants already defined.
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
	public Object modelCheckSingleProperty(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr,
			Values initialState, int noIterations, int maxPathLength) throws PrismException
	{
		ArrayList exprs;
		Object res[];

		exprs = new ArrayList();
		exprs.add(expr);
		res = modelCheckMultipleProperties(modulesFile, propertiesFile, exprs, initialState, noIterations,
				maxPathLength);

		if (res[0] instanceof PrismException)
			throw (PrismException) res[0];
		else
			return res[0];
	}

	/**
	 * Perform approximate model checking of properties on a model, using the simulator.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param modulesFile Model for simulation, constants defined
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param exprs The properties to check
	 * @param initialState
	 *            The initial state for the sampling.
	 * @param noIterations The number of iterations (i.e. number of samples to generate)
	 * @param maxPathLength The maximum path length for sampling
	 *  
	 * <P>
	 * The returned result is an array each item of which is either:
	 * <UL>
	 * <LI> Double object: for =?[] properties
	 * <LI> An exception if there was a problem
	 * </UL>
	 * 
	 */
	public Object[] modelCheckMultipleProperties(ModulesFile modulesFile, PropertiesFile propertiesFile,
			List<Expression> exprs, Values initialState, int noIterations, int maxPathLength)
			throws PrismException
	{
		createNewOnTheFlyPath(modulesFile, propertiesFile);

		Object[] results = new Object[exprs.size()];
		int[] indices = new int[exprs.size()];

		// TODO: move addProperty stuff into startNewPath above

		// Add the properties to the simulator (after a check that they are valid)
		int validPropsCount = 0;
		for (int i = 0; i < exprs.size(); i++) {
			try {
				checkPropertyForSimulation((Expression) exprs.get(i), modulesFile.getModelType());
				indices[i] = addProperty((Expression) exprs.get(i));
				if (indices[i] >= 0)
					validPropsCount++;
			} catch (PrismException e) {
				results[i] = e;
				indices[i] = -1;
			}
		}

		// as long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			initialisePath(initialState);
			int result = 0;//doSampling(noIterations, maxPathLength);
			if (result == ERROR) {
				throw new PrismException(getLastErrorMessage());
			}
		}

		// process the results
		for (int i = 0; i < results.length; i++) {
			// if we have already stored an error for this property, keep it as the result
			if (!(results[i] instanceof PrismException))
				results[i] = processSamplingResult((Expression) exprs.get(i), indices[i]);
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
	public void modelCheckExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile,
			UndefinedConstants undefinedConstants, ResultsCollection resultsCollection, Expression propertyToCheck,
			Values initialState, int maxPathLength, int noIterations) throws PrismException, InterruptedException,
			PrismException
	{
		int n;
		Values definedPFConstants = new Values();

		createNewOnTheFlyPath(modulesFile, propertiesFile);

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
			propertyConstants = propertiesFile.getConstantValues();
			try {
				checkPropertyForSimulation(propertyToCheck, modulesFile.getModelType());
				indices[i] = addProperty(propertyToCheck);
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
			int result = 0;//doSampling(noIterations, maxPathLength);
			if (result == ERROR) {
				throw new PrismException(getLastErrorMessage());
			}
		}

		// process the results
		for (int i = 0; i < n; i++) {
			// if we have already stored an error for this property, keep it as the result, otherwise process
			if (!(results[i] instanceof Exception))
				results[i] = processSamplingResult(propertyToCheck, indices[i]);
			// store it in the ResultsCollection
			resultsCollection.setResult(undefinedConstants.getMFConstantValues(), pfcs[i], results[i]);
		}

		// display results to log
		if (indices.length == 1) {
			if (!(results[0] instanceof Exception))
				mainLog.println("\nResult: " + results[0]);
		} else {
			mainLog.println("\nResults:");
			mainLog.print(resultsCollection.toStringPartial(undefinedConstants.getMFConstantValues(), true, " ", " : ",
					false));
		}
	}

	/*
	private int doSampling(State initialState, int numIters, int maxPathLength)
	{
		int iteration_counter = 0;
		int last_percentage_done = -1;
		int percentage_done = -1;
		double average_path_length = 0;
		int min_path_length = 0, max_path_length = 0;
		boolean stopped_early = false;
		boolean deadlocks_found = false;
		//The current path
		int current_index;
		
		//double* path_cost = new double[no_reward_structs];
		//double* total_state_cost = new double[no_reward_structs];
		//double* total_transition_cost = new double[no_reward_structs];
		
		// Timing info
		long start, start2, stop;
		double time_taken;
		
		//Loop detection requires that deterministic paths are
		//stored, until that determinism is broken.  This path
		//is allocated dynamically, 10 steps at a time.
		//CSamplingLoopDetectionHandler* loop_detection = new CSamplingLoopDetectionHandler();
		
		//TODO: allow stopping of sampling mid-way through?
		//External flag set to false
		boolean should_stop_sampling = false;
		
		start = start2 = System.currentTimeMillis();
		
		mainLog.print("\nSampling progress: [");
		mainLog.flush();
		
		//The loop continues until each pctl/csl formula is satisfied:
		//E.g. usually, this is when the correct number of iterations
		//has been performed, but for some, such as cumulative rewards
		//this can be early if a loop is detected at any point, this
		//is all handled by the All_Done_Sampling() function
		
		// Main sampling loop
		while (!should_stop_sampling && !isSamplingDone()) {
			
			// Display progress
			percentage_done = ((10*(iteration_counter))/numIters)*10;
			if (percentage_done > last_percentage_done) {
				last_percentage_done = percentage_done;
				//cout << " " << last_percentage_done << "%" << endl;
				mainLog.print(" " + last_percentage_done + "%");
				mainLog.flush();
			}
			
			//do polling and feedback every 2 seconds
			stop = System.currentTimeMillis();
			if (stop - start2 > 2000) {
				//Write_Feedback(iteration_counter, numIters, false);
				//int poll = Poll_Control_File();
				//if(poll & STOP_SAMPLING == STOP_SAMPLING)
				//	should_stop_sampling = true;
				 start2 = System.currentTimeMillis();
			}
			
			iteration_counter++;
			
			// Start the new path for this iteration (sample)
			initialisePath(initialState);			
			
			//loop_detection->Reset();
			
			
			notifyPathFormulae(last_state, state_variables, loop_detection);
			
			// Generate a path, up to at most maxPathLength steps
			for (current_index = 0; 
				!All_PCTL_Answers_Known(loop_detection) &&	//not got answers for all properties yet
				//!loop_detection->Is_Proven_Looping()		//not looping (removed this - e.g. cumul rewards can't stop yet)
				current_index < path_length;				//not exceeding path_length
				current_index++)
			{
				// Make a random transition
				automaticChoice(detect);
				
				//if(!loop_detection->Is_Proven_Looping()) { // removed this: for e.g. cumul rewards need to keep counting in loops...
				
				if (modelType.continuousTime()) {
					double time_in_state = Get_Sampled_Time();
					
					last_state->time_spent_in_state = time_in_state;
					
					for (int i = 0; i < no_reward_structs; i++) {
						last_state->state_cost[i] = last_state->state_instant_cost[i]*time_in_state;
						last_state->transition_cost[i] = Get_Transition_Reward(i);
						total_state_cost[i] += last_state->state_instant_cost[i]*time_in_state;
						total_transition_cost[i] += last_state->transition_cost[i];
						path_cost[i] = total_state_cost[i] + total_transition_cost[i];
						
						last_state->cumulative_state_cost[i] = total_state_cost[i];
						last_state->cumulative_transition_cost[i] = total_transition_cost[i];
					}
					
					Notify_Path_Formulae(last_state, state_variables, loop_detection);
				}
				else
				{
					for (int i = 0; i < no_reward_structs; i++) {
						last_state->state_cost[i] = last_state->state_instant_cost[i];
						last_state->transition_cost[i] = Get_Transition_Reward(i);
						total_state_cost[i] += last_state->state_instant_cost[i];
						total_transition_cost[i] += last_state->transition_cost[i];
						path_cost[i] = total_state_cost[i] + total_transition_cost[i];
						
						last_state->cumulative_state_cost[i] = total_state_cost[i];
						last_state->cumulative_transition_cost[i] = total_transition_cost[i];	
					}
					
					Notify_Path_Formulae(last_state, state_variables, loop_detection); 
				}
				//}
			
			}
			
			// record if we found any deadlocks (can check this outside path gen loop because never escape deadlocks)
			if (loop_detection->Is_Deadlock()) deadlocks_found = true;
			
			// compute path length statistics so far
			average_path_length = (average_path_length*(iteration_counter-1)+(current_index))/iteration_counter;
			min_path_length = (iteration_counter == 1) ? current_index : ((current_index < min_path_length) ? current_index : min_path_length);
			max_path_length = (iteration_counter == 1) ? current_index : ((current_index > max_path_length) ? current_index : max_path_length);
			
			//Get samples and notify sample collectors
			Do_A_Sample(loop_detection);
			
			// stop early if any of the properties couldn't be sampled
			if (Get_Total_Num_Reached_Max_Path() > 0) { stopped_early = true; break; }
			
		}//end sampling while
		
		if (!stopped_early) {
			if (!should_stop_sampling)
				mainLog.print(" 100% ]");
			mainLog.println();
			stop = System.currentTimeMillis();
			time_taken = (stop - start) / 1000.0;
			mainLog.print("\nSampling complete: ");
			mainLog.print(iteration_counter + " iterations in " + time_taken + " seconds (average " + time_taken/iteration_counter + ")\n");
			mainLog.print("Path length statistics: average " + average_path_length + ", min " + min_path_length + ", max " + max_path_length + "\n");
		} else {
			mainLog.print(" ...\n\nSampling terminated early after " + iteration_counter + " iterations.\n");
		}
		
		// print a warning if deadlocks occurred at any point
		if (deadlocks_found)
			mainLog.print("\nWarning: Deadlocks were found during simulation: self-loops were added\n");
		
		// print a warning if simulation was stopped by the user
		if (should_stop_sampling)
			mainLog.print("\nWarning: Simulation was terminated before completion.\n");
		
		//write to feedback file with true to indicate that we have finished sampling
		Write_Feedback(iteration_counter, numIters, true);
		
		//Print_Sampling_Results();
		delete loop_detection;
		delete[] starting_variables;
		delete last_state;
		delete[] path_cost;
		delete[] total_state_cost;
		delete[] total_transition_cost;
		
		if (stopped_early) {
			Report_Error("One or more of the properties being sampled could not be checked on a sample. Consider increasing the maximum path length");
			throw 0;
		}
		
		
		// TODO:
		return -1;
	}*/

	private void notifyPathFormulae()
	{
		/*for(int i = 0; i< no_path_formulae; i++)
		{
			if(!registered_path_formulae[i]->Is_Answer_Known(loop_detection))
				registered_path_formulae[i]->Notify_State(last_state, current_state);
		}*/
	}
	
	private boolean isSamplingDone()
	{
		// TODO
		return false;
	}
	
	private static native double getSamplingResult(int propertyIndex);

	private static native int getNumReachedMaxPath(int propertyIndex);

	/**
	 * Used to halt the sampling algorithm in its tracks.
	 */
	public static native void stopSampling();

	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public LabelList getLabelList()
	{
		return propertiesFile.getCombinedLabelList();
	}

	// Methods to compute parameters for simulation

	public double computeSimulationApproximation(double confid, int numSamples)
	{
		return Math.sqrt((4.0 * log(10, 2.0 / confid)) / numSamples);
	}

	public double computeSimulationConfidence(double approx, int numSamples)
	{
		return 2.0 / Math.pow(10, (numSamples * approx * approx) / 4.0);
	}

	public int computeSimulationNumSamples(double approx, double confid)
	{
		return (int) Math.ceil(4.0 * log(10, 2.0 / confid) / (approx * approx));
	}

	public static double log(double base, double x)
	{
		return Math.log(x) / Math.log(base);
	}

	// Method to check if a property is suitable for simulation
	// If not, throws an exception with the reason

	public void checkPropertyForSimulation(Expression prop, ModelType modelType) throws PrismException
	{
		// Check general validity of property
		try {
			prop.checkValid(modelType);
		} catch (PrismLangException e) {
			throw new PrismException(e.getMessage());
		}

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
			throw new PrismException(
					"Simulator can only be applied to properties of the form \"P=? [ ... ]\" or \"R=? [ ... ]\"");

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

	// ------------------------------------------------------------------------------
	// PATH FORMULA METHODS
	// ------------------------------------------------------------------------------

	/**
	 * Loads the path formula stored at pathPointer into the simulator engine. Returns the index of where the path is
	 * being stored
	 */
	public static native int findPathFormulaIndex(long pathPointer);

	/**
	 * For the path formula at the given index, this returns:
	 * <UL>
	 * <LI> -1 if the answer is unknown
	 * <LI> 0 if the answer is false
	 * <LI> 1 if the answer is true
	 * <LI> 2 if the answer is numeric
	 * </UL>
	 */
	public static native int queryPathFormula(int index);

	/**
	 * Returns the numberic answer for the indexed path formula
	 */
	public static native double queryPathFormulaNumeric(int index);

	// ------------------------------------------------------------------------------
	// UTILITY METHODS
	// These are for debugging purposes only
	// ------------------------------------------------------------------------------

	/**
	 * Convienience method to print an expression at a given pointer location
	 * 
	 * @param exprPointer
	 *            the expression pointer.
	 */
	public static native void printExpression(long exprPointer);

	/**
	 * Returns a string representation of the expression at the given pointer location.
	 * 
	 * @param exprPointer
	 *            the pointer location of the expression.
	 * @return a string representation of the expression at the given pointer location.
	 */
	public static native String expressionToString(long exprPointer);

	/**
	 * Returns a string representation of the loaded simulator engine model.
	 * 
	 * @return a string representation of the loaded simulator engine model.
	 */
	public static native String modelToString();

	/**
	 * Returns a string representation of the stored path.
	 * 
	 * @return a string representation of the stored path.
	 */
	public static native String pathToString();

	/**
	 * Prints the current update set to the command line.
	 */
	public static native void printCurrentUpdates();

	// ------------------------------------------------------------------------------
	// ERROR HANDLING
	// ------------------------------------------------------------------------------

	/**
	 * If any native method has had a problem, it should have passed a message to the error handler. This method returns
	 * that message.
	 * 
	 * @return returns the last c++ error message.
	 */
	public static native String getLastErrorMessage();

}
// ==================================================================================
