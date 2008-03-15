//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package simulator;

import java.util.*;
import java.io.*;

import parser.*;
import parser.ast.*;
import parser.visitor.ASTTraverse;
import prism.*;

/**
 * The SimulatorEngine class uses the JNI to provide a Java interface to the PRISM
 * simulator engine library, written in c++.  There are two ways to use this API:
 * <UL>
 *    <LI>    To access the engine directly to perform tasks such as model
 *            exploration.</LI>
 *    <LI>    To perform Monte-Carlo sampling techniques for approximate property
 *            verification.</LI>
 *    <LI>    To generate whole random paths at once.</LI>
 * </UL>
 * Each type of use only builds the relevant data structures of the simulator
 * engine.
 *
 * <H3>Model Exploration</H3>
 *
 * In order to load a PRISM model into the simulator engine, a valid
 * <tt>ModulesFile</tt>, that has had all of its constants defined, can
 * be loaded using the <tt>startNewPath</tt> method.  If the simulator engine
 * is to be used to reason with path properties, a PropertiesFile object is
 * passed. The initial state must also be provided.
 *
 * At this point, the simulator engine will automatically calculate the set of
 * updates to that initial state.  This update set can be queried by the methods
 * such as:
 *
 * <UL>
 *    <LI>    <tt>getNumUpdates()</tt></LI>
 *    <LI>    <tt>getActionLabelOfUpdate(int index)</tt></LI>
 *    <LI>    <tt>getProbabilityOfUpdate(int index)</tt></LI>
 *    <LI>    <tt>getNumAssignmentsOfUpdate(int index)</tt></LI>
 *    <LI>    <tt>getAssignmentVariableIndexOfUpdate(int updateIndex,
 *                assignmentIndex)</tt></LI>
 *    <LI>    <tt>getAssignmentValueOfUpdate(int updateIndex,
 *                assignmentIndex)</tt></LI>
 * </UL>
 *
 * It should be noted that all references to variables, action labels
 * etc... are of the simulators stored index.  There are a number of methods
 * provided to convert the string representations of these items to their
 * appropriate indices:
 *
 * <UL>
 *    <LI>    <tt>getIndexOfVar(String var)</tt></LI>
 *    <LI>    <tt>getIndicesOfAction(String var)</tt></LI>
 * </UL>
 *
 * At this point, there are three options:
 *
 * <UL>
 *    <LI>    Reset the path with a new initial state using the
 *            <tt>restartPath(Values initialState)</tt> method.</LI>
 *    <LI>    Tell the simulator engine to update the current state
 *            by executing the assignments of a manually selected choice from
 *            the update set.  This is done by the
 *            <tt>manualUpdate(int index)</tt> for dtmcs and mdps and the
 *            <tt>manualUpdate(int index, double timeTaken)</tt> for ctmcs.</LI>
 *    <LI>    Request that the simulator engine update the current state with
 *            a randomly sampled choice from the update set.  This can be
 *            repeated n times.  This is done by the
 *            <tt>automaticChoices(int n, ...)</tt> method.
 * </UL>
 *
 * The simulator engine maintains an execution path of all of the previous
 * states of the system.  It is possible to access the information using
 * the following methods:
 *
 * <UL>
 *    <LI>    <tt>getDataSize()</tt></LI>
 *    <LI>    <tt>getPathData(int pathIndex, int variableIndex)</tt></LI>
 *    <LI>    <tt>getTimeSpentInPathState(int pathIndex)</tt></LI>
 *    <LI>    <tt>getCumulativeTimeSpentInPathState(int pathIndex)</tt></LI>
 *    <LI>    <tt>getStateRewardOfPathState(int pathIndex, int i)</tt></LI>
 *    <LI>    <tt>getTotalStateRewardOfPathState(int pathIndex, int i)</tt> (Cumulative)</LI>
 *    <LI>    <tt>getTransitionRewardOfPathState(int pathIndex, int i)</tt></LI>
 *    <LI>    <tt>getTotalTransitionRewardOfPathState(int pathIndex, int i)</tt> (Cumulative)</LI>
 * </UL>
 *
 * The simulator engine automatically detects loops in execution paths, and
 * there are a couple of methods used to query this: <tt>boolean isPathLooping()</tt>
 * and <tt>int loopStart()</tt>
 *
 * Functionality to backtrack to previous states of the path is provided by the
 * <tt>backtrack(int toPathIndex)</tt> method and the ability to remove states
 * from the path before a given index is provids by the
 * <tt>removePrecedingStates</tt> method.
 *
 * There are two ways in which the path can be analysed:
 *
 * <UL>
 *    <LI>    The engine can be loaded with PCTL/CSL formulae via the
 *            <tt>addPCTLRewardFormula</tt> and <tt>addPCTLProbFormula</tt>
 *            methods and the <tt>queryPathFormula(int index)</tt> and
 *            <tt>queryPathFormulaNumeric(int index)</tt> methods can be
 *            used to obtain results over the current execution path. Note
 *            that the <tt>PropertiesFile</tt> object for these properties
 *            should have been provided so that any constant values can
 *            be obtained. </LI>
 *    <LI>    The engine can be loaded with state proposition expressions
 *            via the <tt>loadProposition</tt> method.  These propositions
 *            can be used to determine whether a state at a particular index
 *            in the execution path satisfies a particular (potentially complex)
 *            boolean expression
 *            (<tt>queryProposition(int index, int propIndex)</tt>).
 *            Further functionality is provided to see
 *            whether these states are initial
 *            (<tt>queryIsIntitial(int index)</tt>) or deadlock
 *            (<tt>queryIsDeadlock(int index)</tt>) states.
 * </UL>
 *
 * The <tt>deallocateEngine()</tt> method should be used after using the engine,
 * or before a new model, or path is started.  This simply cleans up the memory
 * in the c++ engine.
 *
 * <H3>Monte-Carlo Sampling</H3>
 *
 * Three methods are provided to make the interface cleaner, if it is only used
 * for approximate model checking.  Each method deals with all model loading,
 * algorithm execution and tidying up afterwards.  The three methods are:
 *
 * <UL>
 *    <LI>    <tt>modelCheckSingleProperty</tt>.  Loads the given
 *            <tt>ModuleFile</tt>
 *            and PCTL/CSL formula, provided the constants are defined.  The
 *            initial state in the <tt>Value</tt> object is loaded into the
 *            simulator and then a given number of randomly generated paths
 *            are computed up to the given maximum path length (including
 *            loop detection when appropriate).  The property is evaluated
 *            over each path and the average probability or reward is returned
 *            as a Double object.</LI>
 *    <LI>    <tt>modelCheckMultipleProperties</tt>.  Similar to the single
 *            property method, except takes advantage of the fact that it is
 *            usually more efficient to deal to multiple properties at the
 *            same time.
 *    <LI>    <tt>modelCheckExperiment</tt>.  Deals with all of the logistics
 *            of performing an experiment and storing the results in an
 *            appropriate <tt>ResultsCollection</tt> object.
 * </UL>
 *
 * <H3>Path Generation</H3>
 *
 * The following utility method is used for generating (and exporting)
 * a single path satsifying certain criteria:
 *
 * <UL>
 *    <LI> <tt>generateSimulationPath</tt>.
 * </UL>
 *
 * @author Andrew Hinton
 */
public class SimulatorEngine
{
	//------------------------------------------------------------------------------
	//	CONSTANTS
	//------------------------------------------------------------------------------
	
	//Errors
	/**
	 * Used by the simulator engine to report that something has caused an exception.
	 * The actual content of the error can be queried using the
	 * <tt>getLastErrorMessage()</tt> method.
	 */
	public static final int ERROR = -1;
	/**
	 * Used by the simulator engine to report that an index parameter was out
	 * of range.
	 */
	public static final int OUTOFRANGE = -1;
	/**
	 * Used by the simulator engine to indicate that a returned pointer is NULL.
	 */
	public static final int NULL = 0;
	
	//Model type
	/**
	 * A constant for the model type
	 */
	public static final int NOT_LOADED = 0;
	/**
	 * A constant for the model type
	 */
	public static final int PROBABILISTIC = ModulesFile.PROBABILISTIC; //dtmc
	/**
	 * A constant for the model type
	 */
	public static final int NONDETERMINISTIC = ModulesFile.NONDETERMINISTIC; //mdp
	/**
	 * A constant for the model type
	 */
	public static final int STOCHASTIC = ModulesFile.STOCHASTIC; //ctmc
	
	//Undefined values
	/**
	 * Used throughout the simulator for undefined integers
	 */
	public static final int UNDEFINED_INT = Integer.MIN_VALUE+1;
	/**
	 * Used throughout the simulator for undefined doubles
	 */
	public static final double UNDEFINED_DOUBLE = -10E23f;
	
	//Infinity
	/**
	 * Used by the simulator engine, usually for infinite rewards. etc...
	 */
	public static final double INFINITY = 10E23f;
	
	//Expression types
	/**
	 * Used to refer to the type of an expression
	 */
	public static final int INTEGER = Expression.INT;
	/**
	 * Used to refer to the type of an expression
	 */
	public static final int DOUBLE = Expression.DOUBLE;
	/**
	 * Used to refer to the type of an expression
	 */
	public static final int BOOLEAN = Expression.BOOLEAN;
	
	// Types of random path to generate
	public static final int SIM_PATH_NUM_STEPS = 0;
	public static final int SIM_PATH_TIME = 1;
	public static final int SIM_PATH_DEADLOCK = 2;
	
	//------------------------------------------------------------------------------
	//	CLASS MEMBERS
	//------------------------------------------------------------------------------
	
	//The current parsed model
	private ModulesFile modulesFile;
	
	//Current model
	private Values constants;
	private Map varIndices;
	private String[] varNames;
	private int[] varTypes;
	private Map actionIndices;
	private String[] actionNames;
	private Map moduleIndices;
	private String[] moduleNames;
	
	//PRISM parsed properties files
	private PropertiesFile propertiesFile;
	
	//Current Properties
	private Values propertyConstants;
	private ArrayList loadedProperties;
	
	
	//------------------------------------------------------------------------------
	//	STATIC INITIALISER
	//------------------------------------------------------------------------------
	
	/**
	 *	Load jni stuff from shared library
	 */
	static
	{
		try
		{
			System.loadLibrary("simengine");
		}
		catch (UnsatisfiedLinkError e)
		{
			System.out.println(e);
			System.exit(1);
		}
	}
	
	
	//------------------------------------------------------------------------------
	//	CONSTRUCTOR
	//------------------------------------------------------------------------------
	
	/**
	 * Constructor for the simulator Engine.
	 */
	public SimulatorEngine()
	{
		varIndices = null;
		varNames = null;
		varTypes = null;
		actionIndices = null;
		actionNames = null;
		moduleIndices = null;
		moduleNames = null;
		modulesFile = null;
		propertiesFile = null;
		constants = null;
		propertyConstants = null;
		loadedProperties = null;
	}
	
	//------------------------------------------------------------------------------
	//	PRISM LOG
	//------------------------------------------------------------------------------
	
	// place to store main log for java code
	private static PrismLog mainLog;
	// jni method to set main log for native code
	private static native void Set_Main_Log(PrismLog log);
	// method to set main log both in java and c++
	public static void setMainLog(PrismLog log)
	{
		mainLog = log;
		Set_Main_Log(log);
	}
	
	
	//------------------------------------------------------------------------------
	//	MODEL INITIALISATION
	//------------------------------------------------------------------------------
	
	/**
	 * Deallocates all simulator data structures from memory.
	 * @throws SimulatorException If there was a problem with the deallocation, an exception is thrown.
	 */
	public void deallocateEngine() throws SimulatorException
	{
		//Call tidy up code in the c++ engine
		int num =	tidyUpEverything();
		
		if(num == ERROR)
			throw new SimulatorException(getLastErrorMessage());
		
		varIndices = null;
		varNames = null;
		varTypes = null;
		actionIndices = null;
		actionNames = null;
		moduleIndices = null;
		moduleNames = null;
		modulesFile = null;
		propertiesFile = null;
		constants = null;
		propertyConstants = null;
		loadedProperties = null;
		
		
	}
	
	/**
	 *	Cleans up all c++ structures including the loaded model,
	 *	loaded properties, the current path and any sampling
	 *	information.  Returns ERROR if there was a problem.
	 */
	private static native int tidyUpEverything();
	
	
	//------------------------------------------------------------------------------
	//	MODEL LOADING METHODS
	//------------------------------------------------------------------------------
	
	/**
	 *	Notifies the simulator engine of a PRISM parsed ModulesFile.
	 *	Loads in the model stored in modulesFile with the constants stored in constants.
	 *	It also initialises the state space with the appropriate number of variables.
	 *	If there are any problems with loading the model,
	 *	is called before a SimulatorException is thrown.
	 */
	private void loadModulesFile(ModulesFile modulesFile, Values constants) throws SimulatorException
	{
		this.modulesFile = modulesFile;
		this.constants = constants;
		
		// check for presence of system...endsystem
		if (modulesFile.getSystemDefn() != null) {
			throw new SimulatorException("Sorry - the simulator does not currently handle the system...endsystem construct");
		}
		
		try
		{
			int result; //used to look for errors
			
			//ALLOCATE STATE SPACE AND SETUP VARIABLES
			varIndices = new HashMap();
			//count variables to set up array
			int numVars = modulesFile.getNumGlobals();
			for(int i = 0; i < modulesFile.getNumModules(); i++)
				numVars += modulesFile.getModule(i).getNumDeclarations();
			
			varNames = new String[numVars];
			varTypes = new int[numVars];
			
			//Get globals from ModulesFile
			numVars = modulesFile.getNumGlobals();
			for(int i = 0; i < numVars; i++)
			{
				varIndices.put(modulesFile.getGlobal(i).getName(), new Integer(i)); //variable table to help later
				varNames[i] = modulesFile.getGlobal(i).getName();
				varTypes[i] = modulesFile.getGlobal(i).getType();
			}
			
			//Get variables from each Module
			int start = numVars;
			for(int i = 0; i < modulesFile.getNumModules(); i++)
			{
				Module m = modulesFile.getModule(i);
				int numDec = m.getNumDeclarations();
				for(int j = 0; j < numDec; j++)
				{
					Declaration d = m.getDeclaration(j);
					varIndices.put(m.getDeclaration(j).getName(), new Integer(start+j));
					varNames[start+j] = m.getDeclaration(j).getName();
					varTypes[start+j] = m.getDeclaration(j).getType();
				}
				
				numVars+= numDec;
				start = numVars;
			}
			
			
			result = allocateStateSpace(numVars); //notify the c++ engine to construct variable table
			if(result == ERROR)
			{
				throw new SimulatorException(getLastErrorMessage());
			}
			
			
			//Initialise model
			
			actionIndices = new HashMap();
			moduleIndices = new HashMap();
			
			//SETUP MODEL
			int type = modulesFile.getType();
			
			//Count commands
			int countCommands = 0;
			for(int i = 0; i < modulesFile.getNumModules(); i++)
				countCommands += modulesFile.getModule(i).getNumCommands();
			
			//Setup actions (synchs and modules)
			//This is where we may need to look at systemdefinition stuff
			int acind = 0;
			for(int i = 0; i < modulesFile.getNumModules(); i++)
			{
				Vector v = modulesFile.getSynchs();
				for(int j = 0; j < v.size(); j++)
				{
					String s = (String)v.get(j);
					if(!actionIndices.containsKey(s))
					{
						actionIndices.put(s, new Integer(acind++));
					}
				}
				
			}
			moduleNames = new String[modulesFile.getNumModules()];
			for(int j = 0; j < modulesFile.getNumModules(); j++)
			{
				String s = modulesFile.getModule(j).getName();
				moduleIndices.put(s, new Integer(j));
				moduleNames[j] = s;
			}
			
			actionNames = new String[acind];
			acind = 0;
			for(int i = 0; i < modulesFile.getNumModules(); i++)
			{
				Vector v = modulesFile.getSynchs();
				for(int j = 0; j < v.size(); j++)
				{
					String s = (String)v.get(j);
					boolean store = true;
					
					for(int k = 0; k < acind; k++)
					{
						if(actionNames[k].equals(s))
							store = false;
					}
					
					if(store) actionNames[acind++] = s;
					
				}
				
			}
			
			//Count Rewards
			
			int numRewardStructs = 0;
			int numStateRewards[], numTransitionRewards[];
			
			numRewardStructs = modulesFile.getNumRewardStructs();
			numStateRewards = new int[numRewardStructs];
			numTransitionRewards = new int[numRewardStructs];
			
			for (int i = 0; i < numRewardStructs; i++) {
				
				numStateRewards[i] = 0;
				numTransitionRewards[i] = 0;
				
				RewardStruct rewards = modulesFile.getRewardStruct(i);
				RewardStructItem reward;
				
				//count the number of each type of reward
				for(int j = 0; j < rewards.getNumItems(); j++)
				{
					reward = rewards.getRewardStructItem(j);
					if(reward.isTransitionReward()) numTransitionRewards[i]++;
					else numStateRewards[i]++;
				}
			}
			
			//Perform the allocation
			
			result = allocateModel(type, countCommands, numRewardStructs, numStateRewards, numTransitionRewards, modulesFile.getNumModules(), acind);
			if(result == ERROR)
			{
				throw new SimulatorException(getLastErrorMessage());
			}
			
			
			
			//Build transition table
			for(int i = 0; i < modulesFile.getNumModules(); i++)
			{
				Module m = modulesFile.getModule(i);
				for(int j = 0; j < m.getNumCommands(); j++)
				{
					Command c = m.getCommand(j);
					
					//Find the action index for this transition
					//This is where we may need to look at systemdefinition stuff
					String synch = c.getSynch();
					int actionIndex =
					(synch.equals("")) ? -1 //if no synch
					: ((Integer)(actionIndices.get(synch))).intValue();		//otherwise, use the synch
					
					int moduleIndex =
					((Integer)(moduleIndices.get(m.getName()))).intValue();
					
					//Create the guard expression
					long guardPointer = c.getGuard().toSimulator(this);
					if(guardPointer == NULL)
					{
						throw new SimulatorException("Problem with loading model into simulator: null guardPointer in "+m.getName());
					}
					//Find out number of updates
					Updates ups = c.getUpdates();
					int numUpdates = ups.getNumUpdates();
					
					//Construct the command
					long commandPointer = createCommand(guardPointer, actionIndex, moduleIndex, numUpdates);
					
					if(commandPointer == NULL)
					{
						throw new SimulatorException("Problem with loading model into simulator: null commandPointer in "+m.getName());
					}
					//add all the updates to the command
					for(int k = 0; k < numUpdates; k++)
					{
						//Create probability expression for this update
						Expression p = ups.getProbability(k);
						if (p == null) p = Expression.Double(1.0);
						long probPointer = p.toSimulator(this);
						if(probPointer == NULL)
						{
							throw new SimulatorException("Problem with loading model into simulator: null probPointer in "+m.getName());
						}
						//Construct the update
						Update u = ups.getUpdate(k);
						int numAssignments = u.getNumElements();
						long updatePointer = addUpdate(commandPointer, probPointer, numAssignments);
						if(updatePointer == NULL)
						{
							throw new SimulatorException("Problem with loading model into simulator: null updatePointer in "+m.getName());
						}
						//add all the assignments to the update
						for(int ii = 0; ii < numAssignments; ii++)
						{
							//find the index of the variable for this update
							String varName = u.getVar(ii);
							int varIndex = ((Integer)varIndices.get(varName)).intValue();
							
							//construct the rhs
							long rhsPointer = u.getExpression(ii).toSimulator(this);
							if(rhsPointer == NULL)
							{
								throw new SimulatorException("Problem with loading model into simulator: null rhs in "+m.getName());
							}
							//construct the assignment
							long assign = addAssignment(updatePointer, varIndex, rhsPointer);
							if(assign == NULL)
							{
								throw new SimulatorException("Problem with loading model into simulator: null assignment in "+m.getName());
							}
						}
					}
					result = setupAddTransition(commandPointer);
					if(result == ERROR)
					{
						throw new SimulatorException(getLastErrorMessage());
					}
				}
			}
			
			
			//SETUP REWARDS
			
			for (int i = 0; i < numRewardStructs; i++) {
				
				RewardStruct rewards = modulesFile.getRewardStruct(i);
				RewardStructItem reward;
				
				//count the number of each type of reward
				for(int j = 0; j < rewards.getNumItems(); j++)
				{
					reward = rewards.getRewardStructItem(j);
					long rewardPointer = reward.toSimulator(this);
					if(rewardPointer == NULL)
						throw new SimulatorException("Problem with loading model into simulator: null reward, "+reward.toString());
					if(reward.isTransitionReward())
						result = setupAddTransitionReward(i, rewardPointer);
					else
						result = setupAddStateReward(i, rewardPointer);
					if(result == ERROR)
						throw new SimulatorException(getLastErrorMessage());
				}
			}
		}
		catch(SimulatorException e)
		{
			deallocateEngine();
			throw e;
		}
		catch(PrismLangException e)
		{
			deallocateEngine();
			throw new SimulatorException(e.getMessage());
		}
	}
	
	
	
	//These set methods should ONLY be used when loading the model
	
	
	
	/**
	 *	Model loading helper method
	 *	Sets up the c++ engine state space variable table to have the given number of
	 *	variables.
	 *	Returns ERROR if there is a problem.
	 */
	private static native int allocateStateSpace(int numberOfVariables);
	
	/**
	 *	Allocates space for a new model in the c++ engine according to the
	 *	given parameters.
	 */
	private static native int allocateModel(int type, int noCommands, int noRewardStructs, int noStateRewards[],
	int noTransitionRewards[], int noModules, int noActions);
	
	/**
	 *	Model loading helper method
	 *	Adds the CCommand object stored at the location of commandPointer
	 *	to the c++ engine transition table.
	 *	Returns ERROR if there is a problem
	 */
	private static native int setupAddTransition(long commandPointer);
	
	/**
	 *	Model loading helper method
	 *	Adds the CStateRewards object stored at the location of rewardPointer
	 *	to the ith state rewards table.
	 *	Returns ERROR if there is a problem
	 */
	private static native int setupAddStateReward(int i, long rewardPointer);
	
	/**
	 *	Model loading helper method
	 *	Adds the CTransitionRewards object stored at the location of rewardPointer
	 *	to the ith transition rewards table.
	 *	Returns ERROR if there is a problem
	 */
	private static native int setupAddTransitionReward(int i, long rewardPointer);
	
	
	//------------------------------------------------------------------------------
	//	MODEL ACCESS METHODS
	//------------------------------------------------------------------------------
	
	/**
	 * Provides access to the indices of variable names.
	 * @param name the variable string.
	 * @throws SimulatorException if the string was not found or is invalid.
	 * @return the index of the given variable string.
	 */
	public int getIndexOfVar(String name) throws SimulatorException
	{
		if(varIndices.containsKey(name))
			return ((Integer)varIndices.get(name)).intValue();
		else throw new SimulatorException("Cannot get variable index, name: "+name+" does not exist");
	}
	
	/**
	 * Provides access to the indices of action labels.
	 * @param name the action label name.
	 * @throws SimulatorException if the string was not found or invalid.
	 * @return the index of the given action string.
	 */
	public int getIndexOfAction(String name) throws SimulatorException
	{
		if(actionIndices.containsKey(name))
			return ((Integer)actionIndices.get(name)).intValue();
		else throw new SimulatorException("Cannot get action index, name: "+name+" does not exist");
	}
	
	/**
	 * Provides access to the loaded <ModulesFile>'s constants.
	 * @return the loaded <ModulesFile>'s constants.
	 */
	public Values getConstants()
	{
		if(constants == null)
		{
			constants = new Values();
		}
		return constants;
	}
	
	
	//------------------------------------------------------------------------------
	//	PATH INITIALISATION AND SETUP METHODS
	//------------------------------------------------------------------------------
	
	/**
	 * Sets up the initial state of the simulator using a Values object
	  */
	private void loadInitialState(Values initialState) throws SimulatorException
	{
		int i, ind, value;
		try
		{
			//Define the initial values in the model
			for(i = 0; i < initialState.getNumValues(); i++)
			{
				ind = ((Integer)varIndices.get(initialState.getName(i))).intValue();
				if(initialState.getType(i) == BOOLEAN)
					value = (initialState.getBooleanValue(i)) ? 1 : 0;
				else if(initialState.getType(i) == INTEGER)
					value = initialState.getIntValue(i);
				else throw new SimulatorException("Invalid type for variable " + initialState.getName(i));
				defineVariable(ind, value);
			}
		}
		catch(PrismException e)
		{
			throw new SimulatorException(e.getMessage());
		}
	}

	/**
	 * Starts a new path, loads the model data structure with its constants defined
	 * constants. Prerequisites: CONSTANTS MUST BE DEFINED BEFORE CALLING THIS
	 * METHOD
	 * @param modulesFile The <tt>ModulesFile</tt> object to be converted into the simulator
	 * data structure for a PRISM model.
	 * @param propertiesFile The <tt>PropertiesFile</tt> object that any pctl path properties
	 * belong to.
	 * @param initialState The intial state for the new path.
	 * @throws SimulatorException If anything goes wrong with the model loading, or the initialisation
	 * of the new path.
	 */
	public void startNewPath(ModulesFile modulesFile, PropertiesFile propertiesFile, Values initialState) throws SimulatorException
	{
		deallocateEngine();
		
		loadModulesFile(modulesFile, modulesFile.getConstantValues());
		
		if (propertiesFile == null) propertiesFile = new PropertiesFile(modulesFile);
		
		propertyConstants = propertiesFile.getConstantValues();
		this.propertiesFile = propertiesFile;
		
		int result = allocatePCTLManager();
		if(result == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
		
		result = allocatePath();
		if(result == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
		
		// Call startNewPath to actually begin a new trace with the initial state
		restartNewPath(initialState);
	}
	
	/**
	 * Restarts the simulation path with the given intial state.
	 * @param initialState the initial state to be set into the simulator engine.
	 * @throws SimulatorException if there is any problem with setting this state.
	 */
	public void restartNewPath(Values initialState) throws SimulatorException
	{
		//load the initial state into the model
		try
		{
			//Define the initial values in the model
			for(int i = 0; i < initialState.getNumValues(); i++)
			{
				int index = ((Integer)varIndices.get(initialState.getName(i))).intValue();
				int value;
				if(initialState.getType(i) == BOOLEAN)
					value = (initialState.getBooleanValue(i)) ? 1 : 0;
				else if(initialState.getType(i) == INTEGER)
					value = initialState.getIntValue(i);
				else throw new SimulatorException("Cannot start new path: Invalid type for variable "+initialState.getName(i));
				defineVariable(index, value);
			}
		}
		catch(PrismException e)
		{
			throw new SimulatorException("Error when starting new path: "+e.getMessage());
		}
		
		int result = startPath();
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
		
	}
	
	/**
	 *	Allocates an execution path in memory for the purposes of model
	 *	exploration.  This should not be used if wishing to do sampling.
	 */
	private static native int allocatePath();
	
	/**
	 *	Gives the variable at the given index a value.  This is used
	 *	to define the initial state (hence private)
	 */
	private static native void defineVariable(int varIndex, int value);
	
	/**
	 *	Adds the initial state to the path.
	 */
	private static native int startPath();
	
	
	//------------------------------------------------------------------------------
	//	PATH ACCESS METHODS
	//------------------------------------------------------------------------------
	
	/**
	 * Returns the number of variables and hence the width of the path table.
	 * @return the number of variables of the current model.
	 */
	public int getNumVariables()
	{
		return varNames.length;
	}
	
	/**
	 * Returns the name of the variable at the given index.
	 * @param i the variable index
	 * @throws SimulatorException if the variable index is out of range.
	 * @return the string representation of the indexed variable.
	 */
	public String getVariableName(int i) throws SimulatorException
	{
		if(varNames != null && i < varNames.length && i >= 0)
		{
			return varNames[i];
		}
		else throw new SimulatorException("Variable out of range");
	}
	
	/**
	 * Returns the type of the variable stored at the given index.
	 * @param i the index of the variable.
	 * @throws SimulatorException if the variable index is out of range.
	 * @return either <tt>INTEGER</tt>, <tt>DOUBLE</tt> or
	 * <tt>BOOLEAN</tt> or depending on the type of the
	 * indexed variable.
	 */
	public int getVariableType(int i) throws SimulatorException
	{
		if(varTypes != null && i < varTypes.length && i >= 0)
		{
			return varTypes[i];
		}
		else throw new SimulatorException("Variable out of range");
	}
	
	/**
	 * Returns the number of states stored in the current path table.
	 * @return the number of states stored in the current path table.
	 */
	public static native int getPathSize();
	
	/**
	 * Returns the value stored for the variable at varIndex at the
	 * path index: stateIndex.
	 * @param varIndex the index of the variable of interest.
	 * @param stateIndex the index of the path state of interest
	 * @return the value stored for the variable at varIndes at the given
	 * stateIndex.
	 */
	public static native int getPathData(int varIndex, int stateIndex);
	
	/**
	 * Returns the time spent in the state at the given path index.
	 * @param stateIndex the index of the path state of interest
	 * @return the time spent in the state at the given path index.
	 */
	public static native double getTimeSpentInPathState(int stateIndex);
	
	/**
	 * Returns the cumulative time spent in the states upto a given path index.
	 * @param stateIndex the index of the path state of interest
	 * @return the time spent in the state at the given path index.
	 */
	public static native double getCumulativeTimeSpentInPathState(int stateIndex);
	
	/**
	 * Returns the ith state reward of the state at the given path index.
	 * @param stateIndex the index of the path state of interest
	 * @param i the index of the reward structure
	 * @return the state reward of the state at the given path index.
	 */
	public static native double getStateRewardOfPathState(int stateIndex, int i);
	
	/**
	 * Returns the ith transition reward of (moving out of) the state at
	 * the given path index.
	 * @param stateIndex the index of the path state of interest
	 * @param i the index of the reward structure
	 * @return the transition reward of (moving out of) the state at
	 * the given path index.
	 */
	public static native double getTransitionRewardOfPathState(int stateIndex, int i);
	
	/**
	 * Cumulative version of getStateRewardOfPathState.
	 */
	public static native double getTotalStateRewardOfPathState(int stateIndex, int i);
	
	/**
	 * Cumulative version of getTransitionRewardOfPathState.
	 */
	public static native double getTotalTransitionRewardOfPathState(int stateIndex, int i);	
	
	/**
	 * Returns the total path time.
	 * @return the total path time.
	 */
	public static native double getTotalPathTime();
	
	/**
	 * Returns the total path reward.
	 * @return the total path reward.
	 */
	public static native double getTotalPathReward(int i);
	
	/**
	 * Returns the total state reward for the path.
	 * @return the total state reward for the path.
	 */
	public static native double getTotalStateReward(int i);
	
	/**
	 * Returns the total transition reward for the path.
	 * @return the total transition reward for the path.
	 */
	public static native double getTotalTransitionReward(int i);
	
	/**
	 * Returns whether the current path is in a looping state
	 * @return whether the current path is in a looping state
	 */
	public static native boolean isPathLooping();
	
	/**
	 * Returns where a loop starts
	 * @return where a loop starts
	 */
	public static native int loopStart();
	
	/**
	 * Returns where a loop ends
	 * @return where a loop ends
	 */
	public static native int loopEnd();
	
	/**
	 * Returns the index of the update chosen for the precalculated old update set.
	 * @param oldStep the old path step of interest
	 * @return the index of the update chosen for the precalculated old update set.
	 */
	public static native int getChosenIndexOfOldUpdate(int oldStep);
	
	/**
	 * Exports the current path to a file in a simple space separated format.
	 * @param file the file to which the path should be exported to (mainLog if null).
	 * @throws SimulatorException if there is a problem with writing the file.
	 */
	public void exportPath(File file) throws SimulatorException
	{
		exportPath(file, false, " ", null);
	}
	
	/**
	 * Exports the current path to a file in a simple space separated format.
	 * @param file the file to which the path should be exported to (mainLog if null).
	 * @param timeCumul show time in cumulative form?
	 * @param colSep string used to separate columns in display
	 * @param vars only print these vars (and steps which change them) (ignore if null)
	 * @throws SimulatorException if there is a problem with writing the file.
	 */
	public void exportPath(File file,  boolean timeCumul, String colSep, ArrayList vars) throws SimulatorException
	{
		PrismLog log;
		
		// create new file log or use main log
		if (file != null) {
			log = new PrismFileLog(file.getPath());
			if (!log.ready()) {
				throw new SimulatorException("Could not open file \""+file+"\" for output");
			}
			mainLog.println("\nExporting path to file \""+file+"\"...");
		} else {
			log = mainLog;
			log.println();
		}
		
		exportPathToLog(log, timeCumul, colSep, vars);
	}

	/**
	 * Exports the current path to a file in a simple space separated format.
	 * @param filename the PrismLog to which the path should be exported to
	 * @param colSep string used to separate columns in display
	 * @param vars only print these vars (and steps which change them) (ignore if null)
	 * @throws SimulatorException if there is a problem with writing the file.
	 */
	public void exportPathToLog(PrismLog log,  boolean timeCumul, String colSep, ArrayList vars) throws SimulatorException
	{
		int i, j, n, nv, nr;
		double d, t;
		boolean stochastic = (modulesFile.getType() == ModulesFile.STOCHASTIC);
		boolean changed;
		int varsNum = 0, varsIndices[] = null;
		
		if(modulesFile == null)
		{
			log.flush();
			log.close();
			return;
		}
		
		// Get sizes
		n = getPathSize();
		nv = getNumVariables();
		nr = modulesFile.getNumRewardStructs();
		
		// if necessary, store info about which vars to display
		if (vars != null) {
			varsNum = vars.size();
			varsIndices = new int[varsNum];
			for (i = 0; i < varsNum; i++) varsIndices[i] = ((Integer)vars.get(i)).intValue();
		}
		
		// Write header
		log.print("step");
		if (vars == null) for(j = 0; j < nv; j++) log.print(colSep+varNames[j]);
		else for(j = 0; j < varsNum; j++) log.print(colSep+varNames[varsIndices[j]]);
		if (stochastic) log.print(colSep+(timeCumul?"time":"time_in_state"));
		if (nr == 1) {
			log.print(colSep+"state_reward"+colSep+"transition_reward");
		} else {
			for(j = 0; j < nr; j++) log.print(colSep+"state_reward"+(j+1)+colSep+"transition_reward"+(j+1));
		}
		log.println();
		
		// Write path
		t = 0.0;
		for(i = 0; i < n; i++)
		{
			// (if required) see if relevant vars have changed
			if (vars != null && i > 0) {
				changed = false;
				for (j = 0; j < varsNum; j++) {
					if (getPathData(varsIndices[j], i) != getPathData(varsIndices[j], i-1)) changed = true;
				}
				if (!changed) { d = (i<n-1) ? getTimeSpentInPathState(i) : 0.0; t += d; continue; }
			}
			// write state index
			log.print(i);
			// write vars
			if (vars == null) {
				for(j = 0; j < nv; j++) {
					log.print(colSep);
					if(varTypes[j] == Expression.BOOLEAN) log.print((getPathData(j, i) == 0) ? "false" : "true");
					else log.print(getPathData(j, i));
				}
			} else {
				for(j = 0; j < varsNum; j++) {
					log.print(colSep);
					if(varTypes[j] == Expression.BOOLEAN) log.print((getPathData(varsIndices[j], i) == 0) ? "false" : "true");
					else log.print(getPathData(varsIndices[j], i));
				}
			}
			if(stochastic)
			{
				d = (i<n-1) ? getTimeSpentInPathState(i) : 0.0;
				log.print(colSep+(timeCumul?t:d));
				t += d;
			}
			for(j = 0; j < nr; j++) {
				log.print(colSep+((i<n-1)?getStateRewardOfPathState(i, j):0.0));
				log.print(colSep+((i<n-1)?getTransitionRewardOfPathState(i, j):0.0));
			}
			log.println();
		}
		
		log.flush();
		log.close();
	}
	
	
	//------------------------------------------------------------------------------
	//	UPDATE HANDLER UPDATE METHODS
	//------------------------------------------------------------------------------
	
	/**
	 * This function applies the indexed update of
	 * the current list of updates to the model. This method is specific to
	 * DTMCs and MDPs.
	 * @param index the index of the selected update to be applied.
	 * @throws SimulatorException if the index is out of range, or there was a problem with performing
	 * the update.
	 */
	public void manualUpdate(int index) throws SimulatorException
	{
		int result = makeManualUpdate(index);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 * This function applies the index update of the current
	 * list of updates to the model.  The time spent in the
	 * last state is also given (for ctmcs).  Use -1.0 for this
	 * parameter if an automatically generated time is required.
	 * @param index the index of the selected update to be applied.
	 * @param time_in_state the time spent in the last state. Use -1.0 for this
	 * parameter if an automatically generated time is required.
	 * @throws SimulatorException if the index is out of range, or there was a problem with performing
	 * the update.
	 */
	public void manualUpdate(int index, double time_in_state) throws SimulatorException
	{
		int result = makeManualUpdate(index, time_in_state);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 *	Ask c++ engine to make the update at the given index.
	 *	Returns OUTOFRANGE (=-1) if the index is out of range.
	 */
	private static native int makeManualUpdate(int index);
	
	/**
	 *	Ask c++ engine to make the update at the given index.
	 *	Returns OUTOFRANGE (=-1) if the index is out of range.
	 */
	private static native int makeManualUpdate(int index, double time_in_state);
	
	/**
	 * This function makes n automatic choices of updates to the global
	 * state.
	 * @param n the number of automatic choices to be made.
	 * @throws SimulatorException if something goes wrong when updating the state.
	 */
	public void automaticChoices(int n) throws SimulatorException
	{
		automaticChoices(n, true);
	}
		
	/**
	 * This function makes n automatic choices of updates to the global
	 * state.
	 * @param n the number of automatic choices to be made.
	 * @param detect whether to employ loop detection.
	 * @throws SimulatorException if something goes wrong when updating the state.
	 */
	public void automaticChoices(int n, boolean detect) throws SimulatorException
	{
		int result = doAutomaticChoices(n, detect);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 * This function makes a number of automatic choices of updates to the global
	 * state, untill `time' has passed.
	 * @param time is the length of time to pass.
	 * @throws SimulatorException if something goes wrong when updating the state.
	 */
	public void automaticChoices(double time) throws SimulatorException
	{
		automaticChoices(time, true);
	}
		
	/**
	 * This function makes n automatic choices of updates to the global
	 * state, untill `time' has passed.
	 * @param time is the length of time to pass.
	 * @param detect whether to employ loop detection.
	 * @throws SimulatorException if something goes wrong when updating the state.
	 */
	public void automaticChoices(double time, boolean detect) throws SimulatorException
	{
		int result = doAutomaticChoices(time, detect);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 *	Ask c++ engine to make n automatic updates to the global state.
	 */
	private static native int doAutomaticChoices(int n, boolean detect);
	
	/**
	 *	Ask c++ engine to make some automatic updates to the global state, up to some time.
	 */
	private static native int doAutomaticChoices(double time, boolean detect);
	
	/**
	 * This function backtracks the current path to the state of the
	 * given index
	 * @param step the path index to backtrack to.
	 * @throws SimulatorException is something goes wrong when backtracking.
	 */
	public void backtrack(int step) throws SimulatorException
	{
		int result = doBacktrack(step);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 * This function backtracks the current path to such that
	 * the cumulative time is equal or less than the time parameter.
	 * @param time the cumulative time to backtrack to.
	 * @throws SimulatorException is something goes wrong when backtracking.
	 */
	public void backtrack(double time) throws SimulatorException
	{
		int result = doBacktrack(time);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 *	Asks the c++ engine to backtrack to the given step.
	 *	Returns OUTOFRANGE (=-1) if step is out of range
	 */
	private static native int doBacktrack(int step);
	
	/**
	 *	Asks the c++ engine to backtrack to some given time of the path.
	 *	Returns OUTOFRANGE (=-1) if time is out of range
	 */
	private static native int doBacktrack(double time);
	
	/**
	 * This function removes states of the path that precede those of the given
	 * index
	 * @param step the index before which the states should be removed.
	 * @throws SimulatorException if anything goes wrong with the state removal.
	 */
	public void removePrecedingStates(int step) throws SimulatorException
	{
		int result = doRemovePrecedingStates(step);
		if(result == ERROR)
			throw new SimulatorException(getLastErrorMessage());
	}
	
	/**
	 *	Asks the c++ engine to remove states which precde the given step.
	 *	Returns OUTOFRANGE (=-1) if step is out of range
	 */
	private static native int doRemovePrecedingStates(int step);
	
	/**
	 * Asks the c++ engine to calculate the updates again for an old
	 * state in the model, so that they can be queried at any time.
	 * @param step the old step of interest.
	 * @return a number
	 */
	public static native int calculateOldUpdates(int step);
	
	/**
	 * Tells the c++ engine that it can recalculate the current update
	 * set because we are finished with the old one.
	 * @return a number
	 */
	public static native int finishedWithOldUpdates();
	
	
	//------------------------------------------------------------------------------
	//	UPDATE HANDLER ACCESS METHODS
	//	(R5.7)  For any current global state, the simualtor will be to
	//	provide a list of possible updates to that global state.
	//------------------------------------------------------------------------------
	
	/**
	 * Returns the current number of available updates.
	 * @return the current number of available updates.
	 */
	public static native int getNumUpdates();
	
	/**
	 * Returns the action label of the update at the given
	 * index.
	 * @param i the index of the update of interest.
	 * @return the action label of the update at the given
	 * index.
	 */
	public String getActionLabelOfUpdate(int i)
	{
		int actionIndex = getActionIndexOfUpdate(i);
		if(actionIndex < 0 || actionIndex >= actionNames.length) return "[]";
		else return "[" +actionNames[actionIndex] + "]";
	}
	
	/**
	 *	Asks the c++ engine for the index of the action of the
	 *	current update at the given index.
	 */
	private static native int getActionIndexOfUpdate(int i);
	
	/**
	 * Returns the module name of the udpate at the given
	 * index.
	 * @param i the index of the update of interest.
	 * @return the module name of the udpate at the given
	 * index.
	 */
	public String getModuleNameOfUpdate(int i)
	{
		int moduleIndex = getModuleIndexOfUpdate(i);
		if(moduleIndex < 0 || moduleIndex >= moduleNames.length) return "";
		else return moduleNames[moduleIndex];
	}
	
	/**
	 * Asks the c++ engine for the index of the module of the
	 * current update at the given index
	 * @param i the index of the update of interest.
	 * @return the index of the module of the
	 * current update at the given index
	 */
	public static native int getModuleIndexOfUpdate(int i);
	
	/**
	 * Returns the probability/rate of the update at the given index
	 * @param i the index of the update of interest.
	 * @return the probability/rate of the update at the given index.
	 */
	public static native double getProbabilityOfUpdate(int i);
	
	/**
	 * Returns a string representation of the assignments for the current
	 * update at the given index.
	 * @param index the index of the update of interest.
	 * @return a string representation of the assignments for the current
	 * update at the given index.
	 */
	public String getAssignmentDescriptionOfUpdate(int index)
	{
		String assignment = "";
		int numAssigns = getNumAssignmentsOfUpdate(index);
		
		
		
		//System.out.println("getAssignmentDesctipyionOfCurrentUpdate" + index);
		for(int i = 0; i < numAssigns; i++)
		{
			int var = getAssignmentVariableIndexOfUpdate(index, i);
			int value = getAssignmentValueOfUpdate(index, i);
			
			if(var < 0 || var >= varNames.length) return "";
			String varName = varNames[var];
			if(varTypes[var] == Expression.BOOLEAN)
				if(value == 0)
					assignment += varName + "\'=false";
				else
					assignment += varName + "\'=true";
			else
				assignment += varName + "\'=" + value;
			
			if(i != numAssigns-1) assignment+=", ";
		}
		return assignment;
	}
	
	/**
	 *	Returns the number of assignments for the current update at the
	 *	given index.
	 */
	private static native int getNumAssignmentsOfUpdate(int i);
	
	/**
	 *	Returns the index of the variable being assigned to for the
	 *	current update at the given index (updateIndex) and for its
	 *	assignment indexed assignmentIndex
	 */
	private static native int getAssignmentVariableIndexOfUpdate(int updateIndex, int assignmentIndex);
	
	/**
	 *	Returns the value of the assignment for the current update at
	 *	the given index (updateIndex and for its assignment indexed
	 *	assignmentIndex.
	 */
	private static native int getAssignmentValueOfUpdate(int updateIndex, int assignmentIndex);
	
	/**
	 * For mdps, updates can belong to different probability distributions. These
	 * probability distributions are indexed.  This returns the probability
	 * distribution that the indexed update belongs to.
	 * @param updateIndex the index of the update of interest.
	 * @return the probability
	 * distribution that the indexed update belongs to.
	 */
	public static native int getDistributionIndexOfUpdate(int updateIndex);
	
	
	//------------------------------------------------------------------------------
	//	PROPERTIES AND SAMPLING (not yet sorted)
	//------------------------------------------------------------------------------
	
	/**
	 *	Allocate space for storage of PCTL Formulae
	 */
	private static native int allocatePCTLManager();
	
	/**
	 *	Allocate space for storage of sampling information
	 */
	private static native int allocateSampling();
	
	/**
	 * Provides access to the propertyConstants
	 * @return the propertyConstants
	 */
	public Values getPropertyConstants()
	{
		if(propertyConstants == null)
		{
			propertyConstants = new Values();
		}
		return propertyConstants;
	}
	
	/**
	 * Convenience method performing various things done before sampling
	  */
	private void setupForSampling(ModulesFile modulesFile, PropertiesFile propertiesFile) throws SimulatorException
	{
		int resultError;
		
		deallocateEngine();
		
		loadModulesFile(modulesFile, modulesFile.getConstantValues());
		
		propertyConstants = propertiesFile.getConstantValues();
		this.propertiesFile = propertiesFile;
		
		resultError = allocatePCTLManager();
		if(resultError == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
		resultError = allocateSampling();
		if(resultError == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
	}
	
	/**
	 * Gets (double) result from simulator for a given property/index and process result
	  */
	private Object processSamplingResult(Expression expr, int index)
	{
		if(index == -1)
		{
			return new SimulatorException("Property cannot be handled by the PRISM simulator");
		}
		else
		{
			double result = getSamplingResult(index);
			if(result == UNDEFINED_DOUBLE) result = Double.POSITIVE_INFINITY;
			// only handle P=?/R=? properties (we could check against the bounds in P>p etc. but results would be a bit dubious)
			if (expr instanceof ExpressionProb) {
				if (((ExpressionProb)expr).getProb() == null) {
					return new Double(result);
				} else {
					return new SimulatorException("Property cannot be handled by the PRISM simulator");
				}
			} else if (expr instanceof ExpressionReward) {
				if (((ExpressionReward)expr).getReward() == null) {
					return new Double(result);
				} else {
					return new SimulatorException("Property cannot be handled by the PRISM simulator");
				}
			}
			else {
				return new SimulatorException("Property cannot be handled by the PRISM simulator");
			}
		}
	}
	
	//PCTL Stuff
	
	public static native int exportBinary(String filename);
	
	public void exportBinaryForSingleProperty(
	ModulesFile modulesFile,
	PropertiesFile propertiesFile,
	Expression expr,
	Values initialState,
	String filename
	) throws SimulatorException
	{
		setupForSampling(modulesFile, propertiesFile);
		
		int index = addProperty(expr);
		
		if(index == -1)
			throw new SimulatorException("Property cannot be handled by the PRISM simulator");
		
		loadInitialState(initialState);
		
		int resultError = exportBinary(filename);
		
		if(resultError == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
		
		deallocateEngine();
	}
	
	public void exportBinaryForMultipleProperties(
	ModulesFile modulesFile,
	PropertiesFile propertiesFile,
	ArrayList exprs,
	Values initialState,
	String filename
	) throws SimulatorException
	{
		setupForSampling(modulesFile, propertiesFile);
		
		int[] indices = addProperties(exprs);
		
		boolean existsOne = false;
		for(int i = 0; i < indices.length; i++)
		{
			if(indices[i]>=0)
			{
				existsOne = true;
				break;
			}
		}
		if(!existsOne)
			throw new SimulatorException("None of the selected properties can be handled by the PRISM simulator");
		
		loadInitialState(initialState);
		
		int resultError = exportBinary(filename);
		
		if(resultError == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
		
		deallocateEngine();
	}
	
	
	/**
	 *	Returns an ArrayList of Values objects for the each propery iteration
	 */
	public ArrayList exportBinaryForExperiment(
	userinterface.properties.GUIExperiment exp,
	java.lang.Thread experimentThread,
	ModulesFile modulesFile,
	PropertiesFile propertiesFile,
	UndefinedConstants undefinedConstants,
	Expression propertyToCheck,
	Values initialState,
	String filename
	) throws PrismException, InterruptedException, SimulatorException
	{
		setupForSampling(modulesFile, propertiesFile);
		
		ArrayList definedPFConstantsCollection = new ArrayList();
		Values definedPFConstants = new Values();
		
		int[]indices = new int[undefinedConstants.getNumPropertyIterations()];
		int[]types = new int[undefinedConstants.getNumPropertyIterations()];
		
		for(int i = 0; i < undefinedConstants.getNumPropertyIterations(); i++)
		{
			if (propertiesFile != null)
			{
				definedPFConstants = undefinedConstants.getPFConstantValues();
				definedPFConstantsCollection.add(definedPFConstants);
				propertiesFile.setUndefinedConstants(definedPFConstants);
				
				propertyConstants = propertiesFile.getConstantValues();
			}
			indices[i] = addProperty(propertyToCheck);
			types[i] = propertyToCheck.getType();
			
			undefinedConstants.iterateProperty();
		}
		
		loadInitialState(initialState);
		
		int resultError = exportBinary(filename);
		
		if(resultError == ERROR)
		{
			throw new SimulatorException(getLastErrorMessage());
		}
		
		deallocateEngine();
		
		return definedPFConstantsCollection;
	}
	
	/**
	 * This method completely encapsulates the model checking of a property
	 * so long as:
	 * prerequisites	modulesFile constants should all be defined
	 * propertiesFile constants should all be defined
	 *
	 * <P>The returned result is:
	 * <UL>
	 *    <LI> A Double object: for =?[] properties
	 * </UL>
	 * @param modulesFile The ModulesFile, constants already defined.
	 * @param propertiesFile The PropertiesFile containing the property of interest, constants defined.
	 * @param expr The property of interest
	 * @param initialState The initial state for the sampling.
	 * @param noIterations The number of iterations for the sampling algorithm
	 * @param maxPathLength the maximum path length for the sampling algorithm.
	 * @throws SimulatorException if anything goes wrong.
	 * @return the result.
	 */
	public Object modelCheckSingleProperty(
	ModulesFile modulesFile,
	PropertiesFile propertiesFile,
	Expression expr,
	Values initialState,
	int noIterations,
	int maxPathLength
	) throws SimulatorException
	{
		ArrayList exprs;
		Object res[];
		
		exprs = new ArrayList();
		exprs.add(expr);
		res = modelCheckMultipleProperties(modulesFile, propertiesFile, exprs, initialState, noIterations, maxPathLength);
		
		if (res[0] instanceof SimulatorException) throw (SimulatorException)res[0]; else return res[0];
	}
	
	/**
	 * This method completely encapsulates the model checking of multiple properties
	 * prerequisites	modulesFile constants should all be defined
	 * propertiesFile constants should all be defined
	 *
	 * <P>The returned result is an array each item of which is either:
	 * <UL>
	 *    <LI> Double object: for =?[] properties
	 *    <LI> An exception if there was a problem
	 * </UL>
	 * @param modulesFile The ModulesFile, constants already defined.
	 * @param propertiesFile The PropertiesFile containing the property of interest, constants defined.
	 * @param exprs The properties of interest
	 * @param initialState The initial state for the sampling.
	 * @param noIterations The number of iterations for the sampling algorithm
	 * @param maxPathLength the maximum path length for the sampling algorithm.
	 * @throws SimulatorException if anything goes wrong.
	 * @return the result.
	 */
	public Object[] modelCheckMultipleProperties(
	ModulesFile modulesFile,
	PropertiesFile propertiesFile,
	ArrayList exprs,
	Values initialState,
	int noIterations,
	int maxPathLength
	) throws SimulatorException
	{
		setupForSampling(modulesFile, propertiesFile);
		
		Object[] results = new Object[exprs.size()];
		int[] indices = new int[exprs.size()];
		
		// Add the properties to the simulator (after a check that they are valid)
		int validPropsCount = 0;
		for(int i = 0; i < exprs.size(); i++) {
			try {
				checkPropertyForSimulation((Expression)exprs.get(i), modulesFile.getType());
				indices[i] = addProperty((Expression)exprs.get(i));
				if (indices[i] >= 0) validPropsCount++;
			}
			catch (SimulatorException e) {
				results[i] = e;
				indices[i] = -1;
			}
		}
		
		// as long as there are at least some valid props, do sampling
		if(validPropsCount > 0) {
			loadInitialState(initialState);
			int result = doSampling(noIterations, maxPathLength);
			if(result == ERROR)
			{
				throw new SimulatorException(getLastErrorMessage());
			}
		}
		
		// process the results
		for(int i = 0; i < results.length; i++)
		{
			// if we have already stored an error for this property, keep it as the result
			if (!(results[i] instanceof SimulatorException))
				results[i] = processSamplingResult((Expression)exprs.get(i), indices[i]);
		}
		
		deallocateEngine();
		
		// display results to log
		if (results.length == 1) {
			if (!(results[0] instanceof SimulatorException)) mainLog.println("\nResult: " + results[0]);
		}
		else {
			mainLog.println("\nResults:");
			for(int i = 0; i < results.length; i++) mainLog.println(exprs.get(i) + " : " + results[i]);
		}
		
		return results;
	}
	
	/**
	 * Deals with all of the logistics
	 * of performing an experiment and storing the results in an
	 * appropriate <tt>ResultsCollection</tt> object.
	 * @param exp the experiment in which to store the results.
	 * @param modulesFile The ModulesFile, constants already defined.
	 * @param propertiesFile The PropertiesFile containing the property of interest, constants defined.
	 * @param undefinedConstants defining what should be done for the experiment
	 * @param resultsCollection where the results should be stored
	 * @param propertyToCheck the property to check for the experiment
	 * @param initialState The initial state for the sampling.
	 * @param maxPathLength the maximum path length for the sampling algorithm.
	 * @param noIterations The number of iterations for the sampling algorithm
	 * @throws PrismException if something goes wrong with the results reporting
	 * @throws InterruptedException if the thread is interrupted
	 * @throws SimulatorException if something goes wrong with the sampling algorithm
	 */
	public void modelCheckExperiment(
	ModulesFile modulesFile,
	PropertiesFile propertiesFile,
	UndefinedConstants undefinedConstants,
	ResultsCollection resultsCollection,
	Expression propertyToCheck,
	Values initialState,
	int maxPathLength,
	int noIterations
	) throws PrismException, InterruptedException, SimulatorException
	{
		int n;
		Values definedPFConstants = new Values();
		
		setupForSampling(modulesFile, propertiesFile);
		
		n = undefinedConstants.getNumPropertyIterations();
		Object[] results = new Object[n];
		Values[] pfcs = new Values[n];
		int[] indices = new int[n];
		
		// Add the properties to the simulator (after a check that they are valid)
		int validPropsCount = 0;
		for(int i = 0; i < n; i++) {
			definedPFConstants = undefinedConstants.getPFConstantValues();
			pfcs[i] = definedPFConstants;
			propertiesFile.setUndefinedConstants(definedPFConstants);
			propertyConstants = propertiesFile.getConstantValues();
			try {
				checkPropertyForSimulation(propertyToCheck, modulesFile.getType());
				indices[i] = addProperty(propertyToCheck);
				if (indices[i] >= 0) validPropsCount++;
			}
			catch (SimulatorException e) {
				results[i] = e;
				indices[i] = -1;
			}
			undefinedConstants.iterateProperty();
		}
		
		// as long as there are at least some valid props, do sampling
		if(validPropsCount > 0) {
			loadInitialState(initialState);
			int result = doSampling(noIterations, maxPathLength);
			if(result == ERROR)
			{
				throw new SimulatorException(getLastErrorMessage());
			}
		}
		
		// process the results
		for(int i = 0; i < n; i++)
		{
			// if we have already stored an error for this property, keep it as the result, otherwise process
			if (!(results[i] instanceof Exception))
				results[i] = processSamplingResult(propertyToCheck, indices[i]);
			// store it in the ResultsCollection
			resultsCollection.setResult(undefinedConstants.getMFConstantValues(), pfcs[i], results[i]);
		}
		
		deallocateEngine();
		
		// display results to log
		if (indices.length == 1) {
			if (!(results[0] instanceof Exception)) mainLog.println("\nResult: " + results[0]);
		}
		else {
			mainLog.println("\nResults:");
			mainLog.print(resultsCollection.toStringPartial(undefinedConstants.getMFConstantValues(), true, " ", " : ", false));
		}
	}

	/**
	 * Generate a random path with the simulator
	 * @param modulesFile The ModulesFile, constants already defined
	 * @param initialState The first state of the path
	 * @param details Information about the path to be generated
	 * @param file The file to output the path to (stdout if null)
	 * @throws SimulatorException if something goes wrong
	 */
	public void generateSimulationPath(ModulesFile modulesFile, Values initialState, String details, int maxPathLength, File file) throws SimulatorException, PrismException
	{
		String s, ss[];
		int i, j, n;
		double t = 0.0;
		boolean done;
		boolean stochastic = (modulesFile.getType() == ModulesFile.STOCHASTIC);
		int simPathType = -1;
		int simPathLength = 0 ;
		double simPathTime = 0.0 ;
		String simPathSep = " ";
		ArrayList simVars = null;
		VarList varList;
		boolean simLoopCheck = true;
		int simPathRepeat = 1;
		
		// parse details
		ss = details.split(",");
		n = ss.length;
		for (i = 0; i < n; i++) {
			if (ss[i].indexOf("time=") == 0) {
				// path with upper time limit
				simPathType = SIM_PATH_TIME;
				try {
					simPathTime = Double.parseDouble(ss[i].substring(5));
					if (simPathTime < 0.0) throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new SimulatorException("Invalid path time limit \""+ss[i]+"\"");
				}
			}
			else if (ss[i].equals("deadlock")) {
				// path until deadlock
				simPathType = SIM_PATH_DEADLOCK;
			}
			else if (ss[i].indexOf("sep=") == 0) {
				// specify column separator to display path
				simPathSep = ss[i].substring(4);
				if (simPathSep.equals("space")) { simPathSep = " "; continue; }
				if (simPathSep.equals("tab")) { simPathSep = "\t"; continue; }
				if (simPathSep.equals("comma")) { simPathSep = ","; continue; }
				throw new SimulatorException("Separator must be one of: \"space\", \"tab\", \"comma\"");
			}
			else if (ss[i].indexOf("vars=") == 0) {
				// specify which variables to display
				varList = modulesFile.createVarList();
				simVars = new ArrayList();
				done = false;
				s = ss[i].substring(5);
				if (s.length() < 1 || s.charAt(0) != '(')
					throw new SimulatorException("Invalid format for \"vars=(...)\"");
				s = s.substring(1);
				if (s.indexOf(')')>-1) { s = s.substring(0, s.length()-1); done = true; }
				j = varList.getIndex(s);
				if (j == -1) throw new SimulatorException("Unknown variable \""+s+"\" in \"vars=(...)\" list");
				simVars.add(new Integer(j));
				while (i < n && !done) {
					s = ss[++i];
					if (s.indexOf(')')>-1) { s = s.substring(0, s.length()-1); done = true; }
					j = varList.getIndex(s);
					if (j == -1) throw new SimulatorException("Unknown variable \""+s+"\" in \"vars=(...)\" list");
					simVars.add(new Integer(j));
				}
			}
			else if (ss[i].indexOf("loopcheck=") == 0) {
				// switch loop detection on/off (default is on)
				s = ss[i].substring(10);
				if (s.equals("true")) { simLoopCheck = true; continue; }
				if (s.equals("false")) { simLoopCheck = false; continue; }
				throw new SimulatorException("Value for \"loopcheck\" flag must be \"true\" or \"false\"");
			}
			else if (ss[i].indexOf("repeat=") == 0) {
				// how many times to repeat path generation until successful (for "deadlock" option)
				try {
					simPathRepeat = Integer.parseInt(ss[i].substring(7));
					if (simPathRepeat < 1) throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new SimulatorException("Value for \"repeat\" option must be a positive integer");
				}
			}
			else {
				// path of fixed number of steps
				simPathType = SIM_PATH_NUM_STEPS;
				try {
					simPathLength = Integer.parseInt(ss[i]);
					if (simPathLength < 0) throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new SimulatorException("Invalid path length \""+ss[i]+"\"");
				}
			}
		}
		if (simPathType < 0) throw new SimulatorException("Invalid path details \""+details+"\"");
		
		// print details
		switch (simPathType) {
		case SIM_PATH_NUM_STEPS: mainLog.println("\nGenerating random path of length "+simPathLength+" steps..."); break;
		case SIM_PATH_TIME: mainLog.println("\nGenerating random path with time limit "+simPathTime+"..."); break;
		case SIM_PATH_DEADLOCK: mainLog.println("\nGenerating random path until deadlock state..."); break;
		}
		
		// display warning if attempt to use "repeat=" option and not "deadlock" option
		if (simPathRepeat > 1 && simPathType != SIM_PATH_DEADLOCK) {
			simPathRepeat = 1;
			mainLog.println("\nWarning: Ignoring \"repeat\" option - it is only valid when looking for deadlocks.");					
		}
		
		// generate path
		startNewPath(modulesFile, null, initialState);
		for (j = 0; j < simPathRepeat; j++) {
			if (j > 0) restartNewPath(initialState);
			i = 0;
			t = 0.0;
			done = false;
			while (!done) {
				// generate a single step of path
				automaticChoices(1, simLoopCheck);
				if (stochastic) t += getTimeSpentInPathState(i++); else t = ++i;
				// check for termination (depending on type)
				switch (simPathType) {
				case SIM_PATH_NUM_STEPS: if (i >= simPathLength) done = true; break;
				case SIM_PATH_TIME: if (t >= simPathTime || i >= maxPathLength) done = true; break;
				case SIM_PATH_DEADLOCK: if (queryIsDeadlock() == 1 || i >= maxPathLength) done = true; break;
				}
				// stop if a loop was found (and loop checking was not disabled)
				if (simLoopCheck && isPathLooping()) break;
			}
			
			// if we are generating multiple paths (to find a deadlock) only stop if deadlock actually found
			if (simPathType == SIM_PATH_DEADLOCK && queryIsDeadlock() == 1) break;
		}
		if (j < simPathRepeat) j++;
		
		// display warning if a deterministic loop was detected (but not in case where multiple paths generated)
		if (simLoopCheck && isPathLooping() && simPathRepeat == 1) {
			mainLog.println("\nWarning: Deterministic loop detected after "+i+" steps (use loopcheck=false option to extend path).");
		}
		
		// if we needed multiple paths to find a deadlock, say how many
		if (simPathRepeat > 1 && j > 1) mainLog.println("\n"+j+" paths were generated.");
		
		// export path
		if (simPathType == SIM_PATH_DEADLOCK && queryIsDeadlock() != 1) {
			mainLog.print("\nNo deadlock state found within "+maxPathLength+" steps");
			if (simPathRepeat > 1) mainLog.print(" (generated "+simPathRepeat+" paths)");
			mainLog.println(".");
		} else {
			exportPath(file, true, simPathSep, simVars);
		}
		
		// warning if stopped early
		if (simPathType == SIM_PATH_TIME && t < simPathTime) {
			mainLog.println("\nWarning: Path terminated before time "+simPathTime+" because maximum path length ("+maxPathLength+") reached.");
		}
	}

	/**
	 * Returns a pointer to the built reward formula
	 * @param expr the ExpressionReward to be built into the engine.
	 * @return a pointer to the built reward formula
	 */
	public long addExpressionReward(ExpressionReward expr)
	{
		Values allConstants = new Values();
		PathExpressionTemporal pe;
		Expression expr2;
		long exprPtr2;
		double time;
		Object rs = null;
		int rsi = -1;
		
		if (!(expr.getPathExpression() instanceof PathExpressionTemporal)) return -1;
		pe = (PathExpressionTemporal)expr.getPathExpression();
		 
		allConstants.addValues(getConstants());
		allConstants.addValues(getPropertyConstants());
		
		// process reward struct index
		rs = expr.getRewardStructIndex();
		if (rs == null) {
			rsi = 0;
		}
		else if (rs instanceof Expression) {
			try {
				rsi = ((Expression)rs).evaluateInt(allConstants, null) - 1;
			} catch(PrismException e) {
				System.err.println("Property: "+pe.toString()+" could not be used in the simulator because: \n"+ e.toString());
				return -1;
			}
		}
		else if (rs instanceof String) {
			rsi = modulesFile.getRewardStructIndex((String)rs);
		}
		
		try {
			switch (pe.getOperator()) {
			
			case PathExpressionTemporal.R_C:
				time = pe.getUpperBound().evaluateDouble(allConstants, null);
				return loadPctlCumulative(rsi, time);
				
			case PathExpressionTemporal.R_I:
				time = pe.getUpperBound().evaluateDouble(allConstants, null);
				return loadPctlInstantanious(rsi, time);
				
			case PathExpressionTemporal.R_F:
				if (!(pe.getOperand2() instanceof PathExpressionExpr)) return -1;
				expr2 = ((PathExpressionExpr) pe.getOperand2()).getExpression();
				exprPtr2 = expr2.toSimulator(this);
				return loadPctlReachability(rsi, exprPtr2);
				
			default: return -1;
			}
		}
		catch(PrismException e) {
			System.err.println("Property: "+pe.toString()+" could not be used in the simulator because: \n"+ e.toString());
			return -1;
		}
	}
	
	/**
	 * Returns a pointer to the built prob formula
	 * @param expr the ExpressionProb to be built into the engine.
	 * @return a pointer to the built prob formula
	 */
	public long addExpressionProb(ExpressionProb expr)
	{
		Values allConstants = new Values();
		PathExpressionTemporal pe;
		Expression expr1, expr2;
		long exprPtr1, exprPtr2;
		double time1, time2;
		
		if (!(expr.getPathExpression() instanceof PathExpressionTemporal)) return -1;
		pe = (PathExpressionTemporal)expr.getPathExpression();
		 
		allConstants.addValues(getConstants());
		allConstants.addValues(getPropertyConstants());
		
		try {
			switch (pe.getOperator()) {
			
			case PathExpressionTemporal.P_X:
				if (!(pe.getOperand2() instanceof PathExpressionExpr)) return -1;
				expr2 = ((PathExpressionExpr) pe.getOperand2()).getExpression();
				exprPtr2 = expr2.toSimulator(this);
				return loadPctlNext(exprPtr2);
				
			case PathExpressionTemporal.P_U:
				if (!(pe.getOperand1() instanceof PathExpressionExpr)) return -1;
				expr1 = ((PathExpressionExpr) pe.getOperand1()).getExpression();
				exprPtr1 = expr1.toSimulator(this);
				if (!(pe.getOperand2() instanceof PathExpressionExpr)) return -1;
				expr2 = ((PathExpressionExpr) pe.getOperand2()).getExpression();
				exprPtr2 = expr2.toSimulator(this);
				if (pe.hasBounds()) {
					time1 = (pe.getLowerBound() == null) ?0.0 : pe.getLowerBound().evaluateDouble(allConstants, null);
					time2 = (pe.getUpperBound() == null) ?Integer.MAX_VALUE : pe.getUpperBound().evaluateDouble(allConstants, null);
					return loadPctlBoundedUntil(exprPtr1, exprPtr2, time1, time2);
				}else {
					return loadPctlUntil(exprPtr1, exprPtr2);
				}

			case PathExpressionTemporal.P_F:
				expr1 = Expression.True();
				exprPtr1 = expr1.toSimulator(this);
				if (!(pe.getOperand2() instanceof PathExpressionExpr)) return -1;
				expr2 = ((PathExpressionExpr) pe.getOperand2()).getExpression();
				exprPtr2 = expr2.toSimulator(this);
				if (pe.hasBounds()) {
					time1 = (pe.getLowerBound() == null) ?0.0 : pe.getLowerBound().evaluateDouble(allConstants, null);
					time2 = (pe.getUpperBound() == null) ?Integer.MAX_VALUE : pe.getUpperBound().evaluateDouble(allConstants, null);
					return loadPctlBoundedUntil(exprPtr1, exprPtr2, time1, time2);
				}else {
					return loadPctlUntil(exprPtr1, exprPtr2);
				}
				
			case PathExpressionTemporal.P_G:
				expr1 = Expression.True();
				exprPtr1 = expr1.toSimulator(this);
				if (!(pe.getOperand2() instanceof PathExpressionExpr)) return -1;
				expr2 = Expression.Not(((PathExpressionExpr) pe.getOperand2()).getExpression());
				exprPtr2 = expr2.toSimulator(this);
				if (pe.hasBounds()) {
					time1 = (pe.getLowerBound() == null) ?0.0 : pe.getLowerBound().evaluateDouble(allConstants, null);
					time2 = (pe.getUpperBound() == null) ?Integer.MAX_VALUE : pe.getUpperBound().evaluateDouble(allConstants, null);
					return loadPctlBoundedUntilNegated(exprPtr1, exprPtr2, time1, time2);
				}else {
					return loadPctlUntilNegated(exprPtr1, exprPtr2);
				}
				
			default: return -1;
			}
		}
		catch(PrismException e) {
			System.err.println("Property: "+pe.toString()+" could not be used in the simulator because: \n"+ e.toString());
			return -1;
		}
	}
	
	/** Returns the index of the property, if it can be added, -1 if nots
	 */
	private int addProperty(Expression prop)
	{
		Values allConstants = new Values();
		allConstants.addValues(getConstants());
		allConstants.addValues(getPropertyConstants());
		
		long pathPointer;
		if(prop instanceof ExpressionProb) {
			pathPointer = addExpressionProb((ExpressionProb)prop);
			if(pathPointer == -1) return -1;
			
			if (((ExpressionProb)prop).getProb() == null) {
				return loadProbQuestion(pathPointer);
			}
			else {
				return -1;
			}
		}
		else if	(prop instanceof ExpressionReward) {
			pathPointer = addExpressionReward((ExpressionReward)prop);
			if(pathPointer == -1) return -1;
			
			if (((ExpressionReward)prop).getReward() == null) {
				return loadRewardQuestion(pathPointer);
			}
			else {
				return -1;
			}
		}
		else return -1;
	}
	
	
	private static native int doSampling(int noIterations, int maxPathLength);
	
	private static native double getSamplingResult(int propertyIndex);
	
	private static native int getNumReachedMaxPath(int propertyIndex);
	
	/**
	 * Used to halt the sampling algorithm in its tracks.
	 */
	public static native void stopSampling();
	
	/**
	 *	Returns an array of indices of the properties within
	 *	the simulator... -1 means that the formula could not be
	 *	loaded
	 */
	private int[] addProperties(ArrayList props)
	{
		int [] indices = new int[props.size()];
		for(int i = 0; i < props.size(); i++)
		{
			indices[i] = addProperty((Expression)(props.get(i)));
		}
		return indices;
	}
	
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
		return Math.sqrt((4.0 * log(10, 2.0/confid))/numSamples);
	}
	public double computeSimulationConfidence(double approx, int numSamples)
	{
		return 2.0 / Math.pow(10, (numSamples*approx*approx)/4.0);
	}
	public int computeSimulationNumSamples(double approx, double confid)
	{
		return (int)Math.ceil(4.0 * log(10, 2.0/confid) / (approx*approx));
	}
	public static double log(double base, double x)
	{
		return Math.log(x) / Math.log(base);
	}

	// Method to check if a property is suitable for simulation
	// If not, throws an exception with the reason
	
	public void checkPropertyForSimulation(Expression prop, int modelType) throws SimulatorException
	{
		// Check general validity of property
		try {
			prop.checkValid(modelType);
		} catch (PrismLangException e) {
			throw new SimulatorException(e.getMessage());
		}
		
		// Simulator can only be applied to P=? or R=? properties
		boolean ok = true;
		PathExpression pe = null;
		if (!(prop instanceof ExpressionProb || prop instanceof ExpressionReward)) ok = false;
		else if (prop instanceof ExpressionProb) {
			if ((((ExpressionProb)prop).getProb() != null)) ok = false;
			pe = ((ExpressionProb)prop).getPathExpression();
		}
		else if (prop instanceof ExpressionReward) {
			if ((((ExpressionReward)prop).getReward() != null)) ok = false;
			pe = ((ExpressionReward)prop).getPathExpression();
		}
		if (!ok) throw new SimulatorException("Simulator can only be applied to properties of the form \"P=? [ ... ]\" or \"R=? [ ... ]\"");
		
		// Check that there are no nested probabilistic operators
		try {
			pe.accept(new ASTTraverse() {
				public void visitPre(ExpressionProb e) throws PrismLangException { throw new PrismLangException(""); }
				public void visitPre(ExpressionReward e) throws PrismLangException { throw new PrismLangException(""); }
				public void visitPre(ExpressionSS e) throws PrismLangException { throw new PrismLangException(""); }
			});
		}
		catch (PrismLangException e) {
			throw new SimulatorException("Simulator cannot handle nested P, R or S operators");
		}
	}

	//------------------------------------------------------------------------------
	//	STATE PROPOSITION METHODS
	//------------------------------------------------------------------------------
	
	/**
	 *	Loads the boolean expression stored at exprPointer into the simulator engine.
	 *	Returns the index of where the proposition is being stored
	 */
	public static native int loadProposition(long exprPointer);
	
	/**
	 *	For the state proposition stored at the given index, this returns 1 if it
	 *	is true in the current state and 0 if not.  -1 is returned if the index
	 *	is invalid.
	 */
	public static native int queryProposition(int index);
	
	/**
	 *	For the state proposition stored at the given index, this returns 1 if it
	 *	is true in the state at the given path step and 0 if not.  -1 is returned if the index
	 *	is invalid.
	 */
	public static native int queryProposition(int index, int step);
	
	/**
	 *	For the current state, this returns 1 if it is the same as
	 *	the initial state for the current path
	 */
	public static native int queryIsInitial();
	
	/**
	 *	For the state at the given index, this returns 1 if it is
	 *	the same as the initial state for the current path
	 */
	public static native int queryIsInitial(int step);
	
	/**
	 *	For the current state, this returns 1 if it is a deadlock
	 *	state.
	 */
	public static native int queryIsDeadlock();
	
	/**
	 *	For the state at the given index, this returns 1 if it is
	 *	a deadlock state.
	 */
	public static native int queryIsDeadlock(int step);
	
	//------------------------------------------------------------------------------
	//	PATH FORMULA METHODS
	//------------------------------------------------------------------------------
	
	/**
	 *	Loads the path formula stored at pathPointer into the simulator engine.
	 *	Returns the index of where the path is being stored
	 */
	public static native int findPathFormulaIndex(long pathPointer);
	
	/**
	 *	For the path formula at the given index, this returns:
	 *	<UL>
	 *	    <LI> -1 if the answer is unknown
	 *	    <LI> 0 if the answer is false
	 *	    <LI> 1 if the answer is true
	 *	    <LI> 2 if the answer is numeric
	 *	</UL>
	 */
	public static native int queryPathFormula(int index);
	
	/**
	 *	Returns the numberic answer for the indexed path formula
	 */
	public static native double queryPathFormulaNumeric(int index);
	
	//------------------------------------------------------------------------------
	//	EXPRESSION CREATION METHODS
	//	These methods provide facilities to create expressions (either for
	//	the model or for properties).
	//	The data structures are built in c++.  The methods return integers
	//	that are actually pointers to CExpression objects.  They are
	//	public because they are used in the
	//------------------------------------------------------------------------------
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalConstant(int constIndex, int type);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealConstant(int constIndex);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createIntegerVar(int varIndex);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createBooleanVar(int varIndex);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createDouble(double value);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createInteger(int value);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createBoolean(boolean value);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createCeil(long exprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createFloor(long exprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalPow(long baseExpressionPointer, long exponentExpressionPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealPow(long baseExpressionPointer, long exponentExpressionPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createMod(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createLog(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNot(long exprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createAnd(long[] exprPointers);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createOr(long[] exprPointers);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalMax(long[] exprPointers);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalMin(long[] exprPointers);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealMax(long[] exprPointers);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealMin(long[] exprPointers);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalTimes(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalPlus(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalMinus(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealTimes(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealPlus(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealMinus(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createDivide(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createIte(long conditionPointer, long truePointer, long falsePointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealIte(long conditionPointer, long truePointer, long falsePointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalEquals(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealEquals(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalNotEquals(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealNotEquals(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalLessThan(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealLessThan(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalGreaterThan(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealGreaterThan(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalLessThanEqual(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealLessThanEqual(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createNormalGreaterThanEqual(long lexprPointer, long rexprPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createRealGreaterThanEqual(long lexprPointer, long rexprPointer);
	
	
	//------------------------------------------------------------------------------
	//	PCTL FORMULAE CREATION METHODS
	//	These methods provide facilities to build PCTL and CSL Formulae
	//------------------------------------------------------------------------------
	
	//internal formulae (these methods return a pointer to the formula's memory
	//location
	
	private static native long loadPctlBoundedUntil(long leftExprPointer, long rightExprPointer, double lowerBound, double upperBound);
	private static native long loadPctlBoundedUntilNegated(long leftExprPointer, long rightExprPointer, double lowerBound, double upperBound);
	
	private static native long loadPctlUntil(long leftExprPointer, long rightExprPointer);
	private static native long loadPctlUntilNegated(long leftExprPointer, long rightExprPointer);
	
	private static native long loadPctlNext(long exprPointer);
	
	private static native long loadPctlReachability(int rsi, long expressionPointer);
	
	private static native long loadPctlCumulative(int rsi, double time);
	
	private static native long loadPctlInstantanious(int rsi, double time);
	
	//prob formulae (these return the index of the property within the engine.
	
	private static native int loadProbQuestion(long pathPointer);
	
	
	//rewards formulae (these return the index of the property within the engine.
	
	private static native int loadRewardQuestion(long pathPointer);
	
	
	//------------------------------------------------------------------------------
	//	TRANSITION TABLE CREATION METHODS
	//	These methods provide facilities to build the transition table's commands,
	//	updates and assignments.
	//	The data structures are built in c++.  The methods return integers
	//	that are actually pointers.
	//------------------------------------------------------------------------------
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createCommand(long guardPointer, int actionIndex, int moduleIndex, int numUpdates);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long addUpdate(long commandPointer, long probPointer, int numAssignments);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long addAssignment(long updatePointer, int varIndex, long rhsPointer);
	
	
	//------------------------------------------------------------------------------
	//	REWARDS TABLE CREATION METHODS
	//	These methods provide facilities to build the rewards tables.
	//------------------------------------------------------------------------------
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createStateReward(long guardPointer, long rewardPointer);
	
	/**
	 * Used by the recursive model/properties loading methods (not part of the API)
	 */
	public static native long createTransitionReward(int actionIndex, long guardPointer, long rewardPointer);
	
	//------------------------------------------------------------------------------
	//	UTILITY METHODS
	//	These are for debugging purposes only
	//------------------------------------------------------------------------------
	
	/**
	 * Convienience method to print an expression at a given pointer location
	 * @param exprPointer the expression pointer.
	 */
	public static native void printExpression(long exprPointer);
	
	/**
	 * Returns a string representation of the expression at the given pointer location.
	 * @param exprPointer the pointer location of the expression.
	 * @return a string representation of the expression at the given pointer location.
	 */
	public static native String expressionToString(long exprPointer);
	
	/**
	 * Deletes an expression from memory.
	 * @deprecated A bit drastic now.
	 * @param exprPointer the pointer to the location of the expression
	 */
	public static native void deleteExpression(long exprPointer); //use with care!
	
	/**
	 * Returns a string representation of the loaded simulator engine model.
	 * @return a string representation of the loaded simulator engine model.
	 */
	public static native String modelToString();
	
	/**
	 * Returns a string representation of the stored path.
	 * @return a string representation of the stored path.
	 */
	public static native String pathToString();
	
	/**
	 * Prints the current update set to the command line.
	 */
	public static native void printCurrentUpdates();
	
	
	//------------------------------------------------------------------------------
	//	ERROR HANDLING
	//------------------------------------------------------------------------------
	
	/**
	 * If any native method has had a problem, it should have passed a message
	 * to the error handler.  This method returns that message.
	 * @return returns the last c++ error message.
	 */
	public static native String getLastErrorMessage();
	
}
//==================================================================================
