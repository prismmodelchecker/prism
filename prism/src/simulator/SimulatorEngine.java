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

import simulator.sampler.*;
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
	protected double currentStateRewards[];

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
	public void manualTransition(int index, double time) throws PrismException
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
	public void automaticTransitions(int n) throws PrismException
	{
		automaticTransitions(n, true);
	}

	public void automaticTransitions(int n, boolean detect) throws PrismException
	{
		int i;
		for (i = 0; i < n; i++)
			automaticTransition(detect);
	}

	/**
	 * Select, at random, a transition from the current transition list and execute it.
	 * For continuous-time models, the time to be spent in the state before leaving is also picked randomly.
	 * @param detect Whether...
	 * throws an exception if deadlock...
	 */
	public void automaticTransition(boolean detect) throws PrismException
	{
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
	 * Randomly select and execute transitions until the specified time delay is first exceeded.
	 * (Time is measured from the initial execution of this method, not total time.)
	 * (For discrete-time models, this just results in ceil(time) steps being executed.)
	 * (If deadlock...
	 */
	/**
	 * This function makes a number of automatic choices of updates to the global state, untill `time' has passed.
	 * 
	 * @param time  is the length of time to pass.
	 * @throws PrismException
	 *             if something goes wrong when updating the state.
	 */
	public void automaticTransitions(double time) throws PrismException
	{
		automaticTransitions(time, true);
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
	public void automaticTransitions(double time, boolean detect) throws PrismException
	{
		// For discrete-time models, this just results in ceil(time) steps being executed.
		if (!modelType.continuousTime()) {
			automaticTransitions((int) Math.ceil(time), detect);
		} else {
			double startTime = path.getTotalTime();
			double currentTime = startTime;
			// Loop until delay exceed
			while (currentTime - startTime < time) {
				/* Break when looping. */
				//if (detect && loop_detection->Is_Proven_Looping()) 
				//break;

				if (queryIsDeadlock())
					/* Break when deadlocking. */
					//	if (loop_detection->Is_Deadlock()) 
					break;

				double probability = 0.0;
				//Automatic_Update(loop_detection, probability);

				// Because we cannot guarantee that we know the selected index, we have to show this.
				//stored_path[current_index]->choice_made = PATH_NO_CHOICE_MADE;
				//stored_path[current_index]->probability = probability;

				// Unless requested not to (detect==false), this function will stop exploring when a loop is detected.
				// Because Automatic_Update() checks for loops before making a transition, we overshoot.
				// Hence at this point if we are looping we step back a state,
				// i.e. reset state_variables and don't add new state to the path.

				/*if (detect && loop_detection->Is_Proven_Looping()) 
				{
					stored_path[current_index]->Make_Current_State_This();
				}
				else 
				{						
					// Add state to path (unless we have stayed in the same state because of deadlock).
					if (!loop_detection->Is_Deadlock()) 
						Add_Current_State_To_Path();	
					currentTime = path_timer;
				}
				
				Calculate_State_Reward(state_variables);
				}
				
				Calculate_Updates(state_variables);
				
				// check for looping
				if(Are_Updates_Deterministic())
				{
				loop_detection->Notify_Deterministic_State(false);
				}
				else
				{
				loop_detection->Notify_Deterministic_Path_End();
				}*/

			}
		}
	}

	/**
	 * Backtrack to a particular step within the current path.
	 * (Not applicable for on-the-fly paths)
	 * @param step The step to backtrack to.
	 */
	public void backtrack(int step) throws PrismException
	{
		// Check step is valid
		if (step > path.size()) {
			throw new PrismException("There is no step " + step + " to backtrack to");
		}
		
		/*
		((PathFull) path).backTrack(step);
		// if go back at least one step, escape deadlock
		if (step < current_index) loop_detection->Set_Deadlock(false);
		
		current_index = step;
		
		//copy the state stored in this path to model_variables
		stored_path[current_index]->Make_Current_State_This();
		
		//recalculate timer and rewards
		path_timer = 0.0;
		for(int i = 0; i < no_reward_structs; i++) {
			path_cost[i] = 0.0;
			total_state_cost[i] = 0.0;
			total_transition_cost[i] = 0.0;
		}
		
		for(int i = 0; i < current_index; i++)
		{
			if(stored_path[i]->time_known)
				path_timer += stored_path[i]->time_spent_in_state;
			
			for (int j = 0; j < no_reward_structs; j++) {
				total_state_cost[j] += stored_path[i]->state_cost[j];
				total_transition_cost[j] += stored_path[i]->transition_cost[j];
			}
		}
		for (int j = 0; j < no_reward_structs; j++) {
			path_cost[j] = total_state_cost[j] + total_transition_cost[j];
		}
		
		Recalculate_Path_Formulae();
		
		Calculate_State_Reward(state_variables);
		
		Calculate_Updates(state_variables);
		
		loop_detection->Backtrack(step);
		*/
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
		//doBacktrack(time);
	}

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
		//doRemovePrecedingStates(step);
	}

	/**
	 * Compute the transition table for an earlier step in the path.
	 * (Not applicable for on-the-fly paths)
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
		// Get corresponding choice
		Choice choice = transitionList.getChoice(i);
		// Remember last state and compute next one (and its state rewards)
		previousState.copy(currentState);
		choice.computeTarget(offset, previousState, currentState);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Store path info, first calculating transition index for full paths
		if (!onTheFly && index == -1)
			index = transitionList.getTotalIndexOfTransition(i, offset);
		path.addStep(index, choice.getAction(), currentStateRewards, currentState, currentStateRewards);
		// TODO: first currentStateRewards in above should be new *trans* rewards!
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
		previousState.copy(currentState);
		choice.computeTarget(offset, previousState, currentState);
		updater.calculateStateRewards(currentState, currentStateRewards);
		// Store path info, first calculating transition index for full paths
		if (!onTheFly && index == -1)
			index = transitionList.getTotalIndexOfTransition(i, offset);
		path.addStep(time, index, choice.getAction(), currentStateRewards, currentState, currentStateRewards);
		// TODO: first currentStateRewards in above should be new *trans* rewards!
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
			if (!sampler.isCurrentValueKnown())
				sampler.update(path);
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
	 * Get the action label taken for a given step.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public String getActionOfPathStep(int step)
	{
		return ((PathFull) path).getAction(step);
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
	// PROPERTIES AND SAMPLING (not yet sorted)
	// ------------------------------------------------------------------------------

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
	public Object modelCheckSingleProperty(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr, Values initialState, int noIterations,
			int maxPathLength) throws PrismException
	{
		ArrayList exprs;
		Object res[];

		exprs = new ArrayList();
		exprs.add(expr);
		res = modelCheckMultipleProperties(modulesFile, propertiesFile, exprs, initialState, noIterations, maxPathLength);

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
	public Object[] modelCheckMultipleProperties(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Expression> exprs, Values initialState,
			int noIterations, int maxPathLength) throws PrismException
	{
		createNewOnTheFlyPath(modulesFile);

		Object[] results = new Object[exprs.size()];
		int[] indices = new int[exprs.size()];

		// Add the properties to the simulator (after a check that they are valid)
		int validPropsCount = 0;
		for (int i = 0; i < exprs.size(); i++) {
			try {
				checkPropertyForSimulation((Expression) exprs.get(i), modulesFile.getModelType());
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
			doSampling(new State(initialState), noIterations, maxPathLength);
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
			ResultsCollection resultsCollection, Expression propertyToCheck, Values initialState, int maxPathLength, int noIterations) throws PrismException,
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
				checkPropertyForSimulation(propertyToCheck, modulesFile.getModelType());
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
		int iters = 0;
		int lastPercentageDone = 0;
		int percentageDone = 0;
		double avgPathLength = 0;
		int minPathFound = 0, maxPathFound = 0;
		boolean stoppedEarly = false;
		boolean deadlocks_found = false;
		boolean allKnown = false;
		int i;

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
		while (!should_stop_sampling && !isSamplingDone() && iters < numIters) {

			// Display progress
			percentageDone = ((10 * (iters)) / numIters) * 10;
			if (percentageDone > lastPercentageDone) {
				lastPercentageDone = percentageDone;
				mainLog.print(" " + lastPercentageDone + "%");
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

			iters++;

			// Start the new path for this iteration (sample)
			initialisePath(initialState);

			//loop_detection->Reset();

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
				automaticTransition(true);

				//if(!loop_detection->Is_Proven_Looping()) { // removed this: for e.g. cumul rewards need to keep counting in loops...

				/*if (modelType.continuousTime()) {
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
				}*/
				//}

			}

			// record if we found any deadlocks (can check this outside path gen loop because never escape deadlocks)
			//if (loop_detection->Is_Deadlock()) deadlocks_found = true;

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
			if (!should_stop_sampling)
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
		if (deadlocks_found)
			mainLog.print("\nWarning: Deadlocks were found during simulation: self-loops were added\n");

		// print a warning if simulation was stopped by the user
		if (should_stop_sampling)
			mainLog.print("\nWarning: Simulation was terminated before completion.\n");

		//write to feedback file with true to indicate that we have finished sampling
		//Write_Feedback(iteration_counter, numIters, true);

		if (stoppedEarly) {
			throw new PrismException(
					"One or more of the properties being sampled could not be checked on a sample. Consider increasing the maximum path length");
		}
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
			throw new PrismException("Simulator can only be applied to properties of the form \"P=? [ ... ]\" or \"R=? [ ... ]\"");

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
