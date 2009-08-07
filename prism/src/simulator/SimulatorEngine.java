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
	// Log
	protected PrismLog mainLog;
	
	// Random number generator
	private RandomNumberGenerator rng;
	
	// The current parsed model + info
	private ModulesFile modulesFile;
	private ModelType modelType;
	// Variable info
	private VarList varList;
	private int numVars;
	// Synchronising action info
	private Vector<String> synchs;
	private int numSynchs;
	
	// TODO: ... more from below


	// NEW STUFF:
	protected boolean onTheFly;
	// Updater object for model
	protected Updater updater;
	
	protected State lastState;
	protected State currentState;
	protected double currentStateRewards[];
	
	// PATH:
	protected Path path = null;
	// TRANSITIONS:
	
	protected TransitionList transitionList;
	
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

	// Infinity
	/**
	 * Used by the simulator engine, usually for infinite rewards. etc...
	 */
	public static final double INFINITY = 10E23f;

	// ------------------------------------------------------------------------------
	// CLASS MEMBERS
	// ------------------------------------------------------------------------------

	private Map<String,Integer> varIndices;
	
	// Current model
	private Values constants;

	// PRISM parsed properties files
	private PropertiesFile propertiesFile;

	// Current Properties
	private Values propertyConstants;
	private ArrayList loadedProperties;

	// ------------------------------------------------------------------------------
	// Basic setup
	// ------------------------------------------------------------------------------

	/**
	 * Constructor for the simulator engine.
	 */
	public SimulatorEngine()
	{
		// TODO
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

	// ------------------------------------------------------------------------------
	// Path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Create a new path for a model and (possibly) some properties.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile: Model for simulation
	 * @param propertiesFile: Properties to check during simulation TODO: change?
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
	 * @param modulesFile: Model for simulation
	 * @param propertiesFile: Properties to check during simulation TODO: change?
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
	 * @param initialState: Initial state (if null, is selected randomly)
	 */
	public void initialisePath(Values initialState) throws PrismException
	{
		// Store a copy of passed in state
		if (initialState != null) {
			currentState.copy(new State(initialState));
		}
		// Or pick a random one
		else {
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
	 * Execute a particular transition, specified by its index.
	 * This function applies the indexed update of the current list of updates to the model. This method is specific to
	 * DTMCs and MDPs.
	 * 
	 */
	public void manualUpdate(int index) throws PrismException
	{
		int choice = transitionList.getChoiceIndexOfTransition(index);
		int index2 = transitionList.getChoiceOffsetOfTransition(index);
		applyUpdate(choice, index2);
	}

	// TODO: make so that  this is general  
	private void applyUpdate(int choice, int index) throws PrismException
	{
		
		State state = transitionList.getChoice(choice).getTarget(index);
		lastState.copy(currentState);
		currentState.copy(state);
		mainLog.println(state);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// TODO: first currentStateRewards should be new Trans rewards!
		path.addStep(index, currentStateRewards, currentState, currentStateRewards);
		updater.calculateTransitions(currentState, transitionList);
	}

	/**
	 * This function applies the index update of the current list of updates to the model. The time spent in the last
	 * state is also given (for ctmcs). Use -1.0 for this parameter if an automatically generated time is required.
	 * 
	 * @param index
	 *            the index of the selected update to be applied.
	 * @param time_in_state
	 *            the time spent in the last state. Use -1.0 for this parameter if an automatically generated time is
	 *            required.
	 * @throws PrismException
	 *             if the index is out of range, or there was a problem with performing the update.
	 */
	public void manualUpdate(int index, double time_in_state) throws PrismException
	{
		//TODO int result = makeManualUpdate(index, time_in_state);
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

	/**
	 * This function makes n automatic choices of updates to the global state.
	 * 
	 * @param n
	 *            the number of automatic choices to be made.
	 * @param detect
	 *            whether to employ loop detection.
	 * @throws PrismException
	 *             if something goes wrong when updating the state.
	 */
	public void automaticChoices(int n, boolean detect) throws PrismException
	{
		// just one for now...
		
		int numChoices, i;
		double d;
		Choice ch;
		
		switch (modelType) {
		case DTMC:
		case MDP:
			numChoices = transitionList.numChoices;
			if (numChoices == 0)
				throw new PrismException("Deadlock found at state " + currentState.toString(modulesFile));
			// Pick a random (nondeterministic choice)
			i = rng.randomInt(numChoices);
			ch = transitionList.getChoice(i);
			// Pick a random 
			double x = rng.randomDouble();
			int j = ch.getIndexByProbSum(x);
			applyUpdate(i, j);
			break;
		case CTMC:
			// TODO: automaticUpdateContinuous();
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
	 * Asks the c++ engine to calculate the updates again for an old state in the model, so that they can be queried at
	 * any time.
	 * 
	 * @param step
	 *            the old step of interest.
	 */
	public void calculateOldUpdates(int step)
	{
		moveToStep(step);
	}

	/**
	 * like backtrack but don't actually modify path 
	 */
	public void moveToStep(int step)
	{
		updater.calculateTransitions(path.getState(step), transitionList);
	}

	/**
	 * Tells the c++ engine that it can recalculate the current update set because we are finished with the old one.
	 * 
	 * @return a number
	 */
	public static native int finishedWithOldUpdates();

	// ------------------------------------------------------------------------------
	// Private methods for path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Loads a new PRISM model into the simulator.
	 * @param modulesFile: The parsed PRISM model
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
		
		// Evaluate constants and optimise modules file for simulation
		modulesFile = (ModulesFile) modulesFile.replaceConstants(constants).simplify();
		
		// Create state/transition storage
		lastState = new State(numVars);
		currentState = new State(numVars);
		currentStateRewards = new double[modulesFile.getNumRewardStructs()];
		transitionList = new TransitionList();
		
		// Create updater for model
		updater = new Updater(modulesFile);
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
	// Path querying
	// ------------------------------------------------------------------------------

	/**
	 * Returns the current state being explored by the simulator.
	 */
	public State getCurrentState()
	{
		return currentState;
	}

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

	/**
	 * Returns the time spent in the state at the given path index.
	 * 
	 * @param stateIndex
	 *            the index of the path state of interest
	 * @return the time spent in the state at the given path index.
	 */
	public double getTimeSpentInPathState(int stateIndex)
	{
		return path.getTime(stateIndex);
	}

	/**
	 * Returns the cumulative time spent in the states upto a given path index.
	 * 
	 * @param stateIndex
	 *            the index of the path state of interest
	 * @return the time spent in the state at the given path index.
	 */
	public double getCumulativeTimeSpentInPathState(int stateIndex)
	{
		return 99;
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
	public double getStateRewardOfPathState(int stateIndex, int i)
	{
		return 99;
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
	 * Returns the index of the update chosen for the precalculated old update set.
	 * 
	 * @param oldStep
	 *            the old path step of interest
	 * @return the index of the update chosen for the precalculated old update set.
	 */
	public int getChosenIndexOfOldUpdate(int oldStep)
	{
		return path.getChoice(oldStep);
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
	// UPDATE HANDLER UPDATE METHODS
	// ------------------------------------------------------------------------------

	/**
	 * Returns the current number of available transitionss.
	 */
	public int getNumTransitions()
	{
		return transitionList.numTransitions;
	}

	/**
	 * Returns the action label of a transition in the list of those currently available.
	 * An empty string denotes an unlabelled (asynchronous) transition.
	 * @param i: The index of the transition being queried
	 */
	public String getTransitionAction(int index) throws PrismException
	{
		if (index < 0 || index >= transitionList.numTransitions)
			throw new PrismException("Invalid transition index " + index);
		return transitionList.getChoiceOfTransition(index).getAction();
	}

	/**
	 * Returns the module name of the udpate at the given index.
	 * 
	 * @param i
	 *            the index of the update of interest.
	 * @return the module name of the udpate at the given index.
	 */
	public String getTransitionModuleOrAction(int index) throws PrismException
	{
		String action = getTransitionAction(index);
		if ("".equals(index)) {
			return transitionList.getChoiceOfTransition(index).getCommand().getParent().getName();
		} else {
			return "[" + action + "]";
		}
	}

	/**
	 * Asks the c++ engine for the index of the module of the current update at the given index
	 * 
	 * @param i
	 *            the index of the update of interest.
	 * @return the index of the module of the current update at the given index
	 */
	public int getModuleIndexOfUpdate(int i)
	{
		// TODO
		return 0;
	}

	/**
	 * Returns the probability/rate of the update at the given index
	 * 
	 * @param i
	 *            the index of the update of interest.
	 * @return the probability/rate of the update at the given index.
	 */
	public double getTransitionProbability(int index) throws PrismException
	{
		// TODO
		if (index < 0 || index >= transitionList.numTransitions)
			throw new PrismException("Invalid transition index " + index);
		return transitionList.getTransitionProbability(index);
	}

	public State getTransitionTarget(int index) //throws PrismException
	{
		// TODO
		if (index < 0 || index >= transitionList.numTransitions)
			return null;
		//throw new PrismException("Invalid transition index " + index);
		return transitionList.getTransitionTarget(index);
	}

	/**
	 * Returns a string representation of the assignments for the current update at the given index.
	 * 
	 * @param index
	 *            the index of the update of interest.
	 * @return a string representation of the assignments for the current update at the given index.
	 */
	public String getAssignmentDescriptionOfUpdate(int index)
	{
		int i, n;
		boolean first = true;
		State v = path.getCurrentState();
		State v2 = getTransitionTarget(index);
		String s = "";
		n = getNumVariables();
		for (i = 0; i < n; i++) {
			if (!v.varValues[i].equals(v2.varValues[i])) {
				if (first)
					first = false;
				else
					s += "&";
				s += "(" + getVariableName(i) + "'=" + v2.varValues[i] + ")";
			}
		}
		return s;
	}

	/**
	 * Returns the index of the variable being assigned to for the current update at the given index (updateIndex) and
	 * for its assignment indexed assignmentIndex
	 */
	private static native int getAssignmentVariableIndexOfUpdate(int updateIndex, int assignmentIndex);

	/**
	 * Returns the value of the assignment for the current update at the given index (updateIndex and for its assignment
	 * indexed assignmentIndex.
	 */
	private static native int getAssignmentValueOfUpdate(int updateIndex, int assignmentIndex);

	/**
	 * For mdps, updates can belong to different probability distributions. These probability distributions are indexed.
	 * This returns the probability distribution that the indexed update belongs to.
	 * 
	 * @param updateIndex
	 *            the index of the update of interest.
	 * @return the probability distribution that the indexed update belongs to.
	 */
	public int getDistributionIndexOfUpdate(int updateIndex)
	{
		// TODO
		return 0;
	}

	// ------------------------------------------------------------------------------
	// PROPERTIES AND SAMPLING (not yet sorted)
	// ------------------------------------------------------------------------------

	/**
	 * Allocate space for storage of sampling information
	 */
	private static native int allocateSampling();

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
	 * This method completely encapsulates the model checking of multiple properties prerequisites modulesFile constants
	 * should all be defined propertiesFile constants should all be defined
	 * 
	 * <P>
	 * The returned result is an array each item of which is either:
	 * <UL>
	 * <LI> Double object: for =?[] properties
	 * <LI> An exception if there was a problem
	 * </UL>
	 * 
	 * @param modulesFile
	 *            The ModulesFile, constants already defined.
	 * @param propertiesFile
	 *            The PropertiesFile containing the property of interest, constants defined.
	 * @param exprs
	 *            The properties of interest
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
	public Object[] modelCheckMultipleProperties(ModulesFile modulesFile, PropertiesFile propertiesFile,
			ArrayList<Expression> exprs, Values initialState, int noIterations, int maxPathLength) throws PrismException
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
			int result = doSampling(noIterations, maxPathLength);
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
			int result = doSampling(noIterations, maxPathLength);
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

	/**
	 * Returns the index of the property, if it can be added, -1 if nots
	 */
	private int addProperty(Expression prop)
	{
		// TODO
		return -1;
	}

	private static native int doSampling(int noIterations, int maxPathLength);

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
	// STATE PROPOSITION METHODS
	// ------------------------------------------------------------------------------

	/**
	 * Loads the boolean expression stored at exprPointer into the simulator engine. Returns the index of where the
	 * proposition is being stored
	 */
	public static native int loadProposition(long exprPointer);

	/**
	 * For the state proposition stored at the given index, this returns 1 if it is true in the current state and 0 if
	 * not. -1 is returned if the index is invalid.
	 */
	public static native int queryProposition(int index);

	/**
	 * For the state proposition stored at the given index, this returns 1 if it is true in the state at the given path
	 * step and 0 if not. -1 is returned if the index is invalid.
	 */
	public static native int queryProposition(int index, int step);

	/**
	 * For the current state, this returns 1 if it is the same as the initial state for the current path
	 */
	public int queryIsInitial()
	{
		return 0; // TODO
	}

	/**
	 * For the state at the given index, this returns 1 if it is the same as the initial state for the current path
	 */
	public int queryIsInitial(int step)
	{
		return 0; // TODO
	}

	/**
	 * For the current state, this returns 1 if it is a deadlock state.
	 */
	// TODO
	public int queryIsDeadlock()
	{
		return 0;
	}

	/**
	 * For the state at the given index, this returns 1 if it is a deadlock state.
	 */
	public int queryIsDeadlock(int step)
	{
		return 0; // TODO
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
