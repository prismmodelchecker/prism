//==============================================================================
//	
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package prism;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import common.iterable.Range;
import dv.DoubleVector;
import explicit.CTMC;
import explicit.CTMCModelChecker;
import explicit.ConstructModel;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.ExplicitFiles2Model;
import explicit.FastAdaptiveUniformisation;
import explicit.FastAdaptiveUniformisationModelChecker;
import explicit.ModelModelGenerator;
import explicit.PartiallyObservableModel;
import hybrid.PrismHybrid;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import odd.ODDUtils;
import param.BigRational;
import param.FunctionFactory;
import param.ModelBuilder;
import param.ParamModel;
import param.ParamModelChecker;
import param.ParamResult;
import parser.PrismParser;
import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ForLoop;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.Accuracy.AccuracyLevel;
import pta.DigitalClocks;
import pta.PTAModelChecker;
import simulator.GenerateSimulationPath;
import simulator.ModulesFileModelGenerator;
import simulator.SimulatorEngine;
import simulator.method.SimulationMethod;
import sparse.PrismSparse;
import strat.Strategy;
import strat.StrategyExportOptions;
import strat.StrategyExportOptions.StrategyExportType;
import strat.StrategyGenerator;

/**
 * Main class for all PRISM's core functionality.
 * This is independent of the user interface (command line or gui).
 */
public class Prism extends PrismComponent implements PrismSettingsListener
{
	/** PRISM version (e.g. "4.0.3"). Read from prism.Version. */
	private static String version = prism.Version.versionString;

	/** Optional PRISM version suffix (e.g. "dev", "beta"). Read from prism.Version. */
	private static String versionSuffix = prism.Version.versionSuffixString;

	//------------------------------------------------------------------------------
	// Constants
	//------------------------------------------------------------------------------

	// underlying computation engine
	public static final int MTBDD = 1;
	public static final int SPARSE = 2;
	public static final int HYBRID = 3;
	public static final int EXPLICIT = 4;
	private static String[] engineStrings = { "?", "MTBDD", "Sparse", "Hybrid", "Explicit" };

	// methods for solving linear equation systems
	public static final int POWER = 1;
	public static final int JACOBI = 2;
	public static final int GAUSSSEIDEL = 3;
	public static final int BGAUSSSEIDEL = 4;
	public static final int PGAUSSSEIDEL = 5;
	public static final int BPGAUSSSEIDEL = 6;
	public static final int JOR = 7;
	public static final int SOR = 8;
	public static final int BSOR = 9;
	public static final int PSOR = 10;
	public static final int BPSOR = 11;

	// methods for solving MDPs
	public static final int MDP_VALITER = 1;
	public static final int MDP_GAUSSSEIDEL = 2;
	public static final int MDP_POLITER = 3;
	public static final int MDP_MODPOLITER = 4;
	public static final int MDP_LP = 5;

	// methods for solving multi-objective queries on MDPs
	public static final int MDP_MULTI_VALITER = 1;
	public static final int MDP_MULTI_GAUSSSEIDEL = 2;
	public static final int MDP_MULTI_LP = 3;

	// termination criterion for iterative methods
	public static final int ABSOLUTE = 1;
	public static final int RELATIVE = 2;

	// options for model matrix export
	public static final int EXPORT_PLAIN = 1;
	public static final int EXPORT_MATLAB = 2;
	public static final int EXPORT_DOT = 3;
	public static final int EXPORT_MRMC = 4;
	public static final int EXPORT_ROWS = 5;
	public static final int EXPORT_DOT_STATES = 6;

	// options for adversary export
	public static final int EXPORT_ADV_NONE = 1;
	public static final int EXPORT_ADV_DTMC = 2;
	public static final int EXPORT_ADV_MDP = 3;

	// methods for SCC decomposition
	public static final int XIEBEEREL = 1;
	public static final int LOCKSTEP = 2;
	public static final int SCCFIND = 3;

	// state space cut-off to trigger MTBDD engine
	protected static final int MTBDD_STATES_THRESHOLD = 100000000;
	
	//------------------------------------------------------------------------------
	// Settings / flags / options
	//------------------------------------------------------------------------------

	// Export parsed PRISM model?
	protected boolean exportPrism = false;
	protected File exportPrismFile = null;
	protected boolean exportPrismConst = false;
	protected File exportPrismConstFile = null;
	// Export digital clocks translation PRISM model?
	protected boolean exportDigital = false;
	protected File exportDigitalFile = null;
	// Export target state info?
	protected boolean exportTarget = false;
	protected String exportTargetFilename = null;
	// Export product model info?
	protected boolean exportProductTrans = false;
	protected String exportProductTransFilename = null;
	protected boolean exportProductStates = false;
	protected String exportProductStatesFilename = null;
	protected boolean exportProductVector = false;
	protected String exportProductVectorFilename = null;
	// Store the final results vector after model checking?
	protected boolean storeVector = false;
	// Generate/store a strategy during model checking?
	protected boolean genStrat = false;
	// Should any generated strategies should be restricted to the states reachable under them?
	protected boolean restrictStratToReach = true;
	// Do bisimulation minimisation before model checking?
	protected boolean doBisim = false;

	// A few miscellaneous options (i.e. defunct/hidden/undocumented/etc.)
	// See constructor below for default values

	private boolean doReach = true; // do reachability? (sometimes might want to skip it)
	private boolean bsccComp = true; // do bscc computation before steady-state?
	private boolean checkZeroLoops = false;

	// MTBDD construction method (NOW DEFUNCT)
	//  1 - use with ordering 1: nondet vars form a tree at the top
	//  3 - use with ordering 2: zero for nonexistant bits
	// nb: option 2 removed because it was stupid
	private int construction = 3;

	// MTBDD variable ordering
	//  1 - (s ... s) (l ... l) (r c ... r c)
	//  2 - (s l ... l r c ... r c) (s l ... l r c ... r c) ...
	private int ordering = 1;

	// Method to use for (symbolic) state-space reachability
	private int reachMethod = REACH_BFS;

	//------------------------------------------------------------------------------
	// Parsers/translators/model checkers/simulators/etc.
	//------------------------------------------------------------------------------

	private static PrismParser thePrismParser = null;
	private static boolean prismParserInUse = false;
	private SimulatorEngine theSimulator = null;

	//------------------------------------------------------------------------------
	// Event listeners
	//------------------------------------------------------------------------------

	private List<PrismModelListener> modelListeners;

	//------------------------------------------------------------------------------
	// State
	//------------------------------------------------------------------------------

	public enum ModelSource {
		PRISM_MODEL, MODEL_GENERATOR, EXPLICIT_FILES, BUILT_MODEL
	}

	public enum ModelBuildType {
		SYMBOLIC, EXPLICIT
	}
	
	public enum PrismEngine {
		SYMBOLIC, EXPLICIT
	}
	
	// Info about currently loaded model, if any
	// Model source
	private ModelSource currentModelSource = ModelSource.PRISM_MODEL;
	// Model type
	private ModelType currentModelType = null;
	// Model info
	private ModelInfo currentModelInfo = null;
	// PRISM model (null if none loaded)
	private ModulesFile currentModulesFile = null;
	// Model generator (null if none loaded)
	private ModelGenerator<?> currentModelGenerator = null;
	// Reward generator (null if none loaded)
	private RewardGenerator<?> currentRewardGenerator = null;
	// Constants to be defined for PRISM model
	private Values currentDefinedMFConstants = null;
	// Was currentDefinedMFConstants evaluated exactly?
	private boolean currentDefinedMFConstantsAreExact = false;
	// Built model storage - symbolic or explicit - at most one is non-null
	private Model currentModel = null;
	private explicit.Model<?> currentModelExpl = null;
	private ModelBuildType currentModelBuildType = null;
	// Are we doing digital clocks translation for PTAs?
	boolean digital = false;

	// The last strategy that was generated
	private Strategy<?> strategy = null;
	
	// Info for explicit files load
	private File explicitFilesStatesFile = null;
	private File explicitFilesTransFile = null;
	private File explicitFilesLabelsFile = null;
	private List<File> explicitFilesStateRewardsFiles = new ArrayList<>();
	private int explicitFilesNumStates = -1;

	// Has the CUDD library been initialised yet?
	private boolean cuddStarted = false;

	// Info about automatic engine switching
	private int engineOld = -1;
	private boolean engineSwitched = false;

	//------------------------------------------------------------------------------
	// Constructors + options methods
	//------------------------------------------------------------------------------

	/**
	 * Construct a new Prism object.
	 * @param mainLog PrismLog where all output will be sent.
	 */
	public Prism(PrismLog mainLog)
	{
		// set up log
		this.mainLog = mainLog;
		// set up some default options
		settings = new PrismSettings();
		// add this Prism object as a results listener
		settings.addSettingsListener(this);
		// create list of model listeners
		modelListeners = new ArrayList<PrismModelListener>();
	}

	/**
	 * Construct a new Prism object.
	 * @deprecated ({@code techLog} is no longer used, use the {@link #prism.Prism(PrismLog)} constructor instead).
	 */
	@Deprecated
	public Prism(PrismLog mainLog, PrismLog techLog)
	{
		this(mainLog);
	}

	/**
	 * Read in PRISM settings from the default file (see PrismSettings.getLocationForSettingsFile()).
	 * If no file exists, attempt to create a new one with default settings.
	 */
	public void loadUserSettingsFile()
	{
		loadUserSettingsFile(null);
	}

	/**
	 * Read in PRISM settings from a specified file.
	 * If the file is null, use the default (see PrismSettings.getLocationForSettingsFile()).
	 * If no file exists, attempt to create a new one with default settings.
	 */
	public void loadUserSettingsFile(File settingsFile)
	{
		// load user's default settings
		try {
			if (settingsFile == null)
				settings.loadSettingsFile();
			else
				settings.loadSettingsFile(settingsFile);
		} catch (PrismException e) {
			// if there were no user defaults to load, create them
			try {
				if (settingsFile == null)
					settings.saveSettingsFile();
				else
					settings.saveSettingsFile(settingsFile);
			} catch (PrismException ex) {
				mainLog.printWarning("Failed to create new PRISM settings file.");
			}
		}
	}

	// Set methods

	/**
	 * Set the PrismLog where messages and model checking output will be sent.
	 */
	public void setMainLog(PrismLog l)
	{
		// store new log
		mainLog = l;
		// pass to other components
		JDD.SetOutputStream(mainLog.getFilePointer());
		PrismMTBDD.setMainLog(mainLog);
		PrismSparse.setMainLog(mainLog);
		PrismHybrid.setMainLog(mainLog);
	}

	// Set methods for main prism settings
	// (provided for convenience and for compatibility with old code)

	public void setEngine(int e) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_ENGINE, e);
	}

	public void setVerbose(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_VERBOSE, b);
	}

	public void setFairness(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_FAIRNESS, b);
	}

	public void setPrecomp(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_PRECOMPUTATION, b);
	}

	public void setProb0(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_PROB0, b);
	}

	public void setProb1(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_PROB1, b);
	}

	public void setFixDeadlocks(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_FIX_DEADLOCKS, b);
	}

	public void setDoProbChecks(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_DO_PROB_CHECKS, b);
	}

	public void setSumRoundOff(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_SUM_ROUND_OFF, d);
	}

	public void setCompact(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_COMPACT, b);
	}

	public void setLinEqMethod(int i) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_LIN_EQ_METHOD, i);
	}

	public void setLinEqMethodParam(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_LIN_EQ_METHOD_PARAM, d);
	}

	public void setMDPSolnMethod(int i) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_MDP_SOLN_METHOD, i);
	}

	public void setMDPMultiSolnMethod(int i) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_MDP_MULTI_SOLN_METHOD, i);
	}

	public void setTermCrit(int i) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_TERM_CRIT, i);
	}

	public void setTermCritParam(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_TERM_CRIT_PARAM, d);
	}

	public void setMaxIters(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_MAX_ITERS, i);
	}

	public void setGridResolution(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_GRID_RESOLUTION, i);
	}

	public void setCUDDMaxMem(String s) throws PrismException
	{
		settings.set(PrismSettings.PRISM_CUDD_MAX_MEM, s);
	}

	public void setCUDDEpsilon(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_CUDD_EPSILON, d);
	}

	public void setNumSBLevels(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_NUM_SB_LEVELS, i);
	}

	public void setSBMaxMem(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_SB_MAX_MEM, i);
	}

	public void setNumSORLevels(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_NUM_SOR_LEVELS, i);
	}

	public void setSORMaxMem(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_SOR_MAX_MEM, i);
	}

	public void setDoSSDetect(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_DO_SS_DETECTION, b);
	}

	public void setExtraDDInfo(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_EXTRA_DD_INFO, b);
	}

	public void setExtraReachInfo(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_EXTRA_REACH_INFO, b);
	}

	public void setSCCMethod(int i) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_SCC_METHOD, i);
	}

	public void setExportAdv(int i) throws PrismException
	{
		settings.setChoice(PrismSettings.PRISM_EXPORT_ADV, i);
	}

	public void setExportAdvFilename(String s) throws PrismException
	{
		settings.set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, s);
	}

	// Set methods for miscellaneous options

	public void setExportPrism(boolean b) throws PrismException
	{
		exportPrism = b;
	}

	/**
	 * Set file to export parsed PRISM file to (null = stdout).
	 */
	public void setExportPrismFile(File f) throws PrismException
	{
		exportPrismFile = f;
	}

	public void setExportPrismConst(boolean b) throws PrismException
	{
		exportPrismConst = b;
	}

	/**
	 * Set file to export parsed PRISM file, with constants expanded, to (null = stdout).
	 */
	public void setExportPrismConstFile(File f) throws PrismException
	{
		exportPrismConstFile = f;
	}

	public void setExportDigital(boolean b) throws PrismException
	{
		exportDigital = b;
	}

	/**
	 * Set file to export digital clocks translation PRISM file to (null = stdout).
	 */
	public void setExportDigitalFile(File f) throws PrismException
	{
		exportDigitalFile = f;
	}

	public void setExportTarget(boolean b) throws PrismException
	{
		exportTarget = b;
	}

	public void setExportTargetFilename(String s) throws PrismException
	{
		exportTargetFilename = s;
	}

	public void setExportProductTrans(boolean b) throws PrismException
	{
		exportProductTrans = b;
	}

	public void setExportProductTransFilename(String s) throws PrismException
	{
		exportProductTransFilename = s;
	}

	public void setExportProductStates(boolean b) throws PrismException
	{
		exportProductStates = b;
	}

	public void setExportProductStatesFilename(String s) throws PrismException
	{
		exportProductStatesFilename = s;
	}

	public void setExportProductVector(boolean b) throws PrismException
	{
		exportProductVector = b;
	}

	public void setExportProductVectorFilename(String s) throws PrismException
	{
		exportProductVectorFilename = s;
	}

	/**
	 * Specify whether or not to store the final results vector after model checking.
	 */
	public void setStoreVector(boolean storeVector)
	{
		this.storeVector = storeVector;
	}

	/**
	 * Specify whether or not a strategy should be generated during model checking.
	 */
	public void setGenStrat(boolean genStrat)
	{
		this.genStrat = genStrat;
	}

	/**
	 * Specify whether or not any generated strategies should be restricted to the states reachable under them.
	 */
	public void setRestrictStratToReach(boolean restrictStratToReach)
	{
		this.restrictStratToReach = restrictStratToReach;
	}

	/**
	 * Specify whether or not to do bisimulation minimisation before model checking.
	 */
	public void setDoBisim(boolean doBisim)
	{
		this.doBisim = doBisim;
	}

	public void setDoReach(boolean b) throws PrismException
	{
		doReach = b;
	}

	public void setBSCCComp(boolean b) throws PrismException
	{
		bsccComp = b;
	}

	public void setCheckZeroLoops(boolean checkZeroLoops)
	{
		this.checkZeroLoops = checkZeroLoops;
	}

	public void setConstruction(int i) throws PrismException
	{
		construction = i;
	}

	public void setOrdering(int i) throws PrismException
	{
		ordering = i;
	}

	public static int REACH_BFS = 1;
	public static int REACH_FRONTIER = 2;

	public void setReachMethod(int reachMethod)
	{
		this.reachMethod = reachMethod;
	}

	// Get methods

	/**
	 * Get the name of this tool.
	 * ("PRISM" by default, extensions might override) 
	 */
	public static String getToolName()
	{
		return "PRISM";
	}

	/**
	 * Get the name of the command-line version of this tool.
	 * ("prism" by default, extensions might override) 
	 */
	public static String getCommandLineName()
	{
		return "prism";
	}

	/**
	 * Get current version number, as a string. 
	 */
	public static String getVersion()
	{
		String v = version;
		// Append version suffix (e.g. "dev", "beta") if non-empty
		if (versionSuffix.length() > 0) {
			v += "." + versionSuffix;
		}
		return v;
	}

	public PrismLog getMainLog()
	{
		return mainLog;
	}

	public PrismSettings getSettings()
	{
		return settings;
	}

	// Get methods for main prism settings
	// (as above, provided for convenience and for compatibility with old code)

	public int getEngine()
	{
		return settings.getChoice(PrismSettings.PRISM_ENGINE);
	}

	/**
	 * Is the current engine "Explicit"?
	 */
	public boolean getExplicit()
	{
		return getEngine() == Prism.EXPLICIT;
	}

	public boolean getFixDeadlocks()
	{
		return settings.getBoolean(PrismSettings.PRISM_FIX_DEADLOCKS);
	}

	public boolean getDoProbChecks()
	{
		return settings.getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS);
	}

	public double getSumRoundOff()
	{
		return settings.getDouble(PrismSettings.PRISM_SUM_ROUND_OFF);
	}

	public int getLinEqMethod()
	{
		return settings.getChoice(PrismSettings.PRISM_LIN_EQ_METHOD);
	}

	public double getLinEqMethodParam()
	{
		return settings.getDouble(PrismSettings.PRISM_LIN_EQ_METHOD_PARAM);
	}

	public int getMDPSolnMethod()
	{
		return settings.getChoice(PrismSettings.PRISM_MDP_SOLN_METHOD);
	}

	public int getMDPMultiSolnMethod()
	{
		return settings.getChoice(PrismSettings.PRISM_MDP_MULTI_SOLN_METHOD);
	}

	public int getTermCrit()
	{
		return settings.getChoice(PrismSettings.PRISM_TERM_CRIT);
	}

	public double getTermCritParam()
	{
		return settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM);
	}

	public int getMaxIters()
	{
		return settings.getInteger(PrismSettings.PRISM_MAX_ITERS);
	}

	public int getGridResolution()
	{
		return settings.getInteger(PrismSettings.PRISM_GRID_RESOLUTION);
	}

	public boolean getVerbose()
	{
		return settings.getBoolean(PrismSettings.PRISM_VERBOSE);
	}

	public boolean getPrecomp()
	{
		return settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION);
	}

	public boolean getProb0()
	{
		return settings.getBoolean(PrismSettings.PRISM_PROB0);
	}

	public boolean getProb1()
	{
		return settings.getBoolean(PrismSettings.PRISM_PROB1);
	}

	public boolean getPreRel()
	{
		return settings.getBoolean(PrismSettings.PRISM_PRE_REL);
	}

	public boolean getFairness()
	{
		return settings.getBoolean(PrismSettings.PRISM_FAIRNESS);
	}

	public int getSBMaxMem()
	{
		return settings.getInteger(PrismSettings.PRISM_SB_MAX_MEM);
	}

	public int getNumSBLevels()
	{
		return settings.getInteger(PrismSettings.PRISM_NUM_SB_LEVELS);
	}

	public int getSORMaxMem()
	{
		return settings.getInteger(PrismSettings.PRISM_SOR_MAX_MEM);
	}

	public boolean getDoSSDetect()
	{
		return settings.getBoolean(PrismSettings.PRISM_DO_SS_DETECTION);
	}

	public boolean getExtraDDInfo()
	{
		return settings.getBoolean(PrismSettings.PRISM_EXTRA_DD_INFO);
	}

	public boolean getExtraReachInfo()
	{
		return settings.getBoolean(PrismSettings.PRISM_EXTRA_REACH_INFO);
	}

	public int getNumSORLevels()
	{
		return settings.getInteger(PrismSettings.PRISM_NUM_SOR_LEVELS);
	}

	public boolean getCompact()
	{
		return settings.getBoolean(PrismSettings.PRISM_COMPACT);
	}

	public String getCUDDMaxMem()
	{
		return settings.getString(PrismSettings.PRISM_CUDD_MAX_MEM);
	}

	public double getCUDDEpsilon()
	{
		return settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON);
	}

	public int getSCCMethod()
	{
		return settings.getChoice(PrismSettings.PRISM_SCC_METHOD);
	}

	public int getExportAdv()
	{
		return settings.getChoice(PrismSettings.PRISM_EXPORT_ADV);
	}

	public String getExportAdvFilename()
	{
		return settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME);
	}

	// Get methods for miscellaneous options

	public boolean getExportPrism()
	{
		return exportPrism;
	}

	public File getExportPrismFile()
	{
		return exportPrismFile;
	}

	public boolean getExportPrismConst()
	{
		return exportPrismConst;
	}

	public File getExportPrismConstFile()
	{
		return exportPrismConstFile;
	}

	public boolean getExportTarget()
	{
		return exportTarget;
	}

	public String getExportTargetFilename()
	{
		return exportTargetFilename;
	}

	public boolean getExportProductTrans()
	{
		return exportProductTrans;
	}

	public String getExportProductTransFilename()
	{
		return exportProductTransFilename;
	}

	public boolean getExportProductStates()
	{
		return exportProductStates;
	}

	public String getExportProductStatesFilename()
	{
		return exportProductStatesFilename;
	}

	public boolean getExportProductVector()
	{
		return exportProductVector;
	}

	public String getExportProductVectorFilename()
	{
		return exportProductVectorFilename;
	}

	/**
	 * Whether or not to store the final results vector after model checking.
	 */
	public boolean getStoreVector()
	{
		return storeVector;
	}

	/**
	 * Whether or not a strategy should be generated during model checking.
	 */
	public boolean getGenStrat()
	{
		return genStrat;
	}

	/**
	 * Whether or not any generated strategies should be restricted to the states reachable under them.
	 */
	public boolean getRestrictStratToReach()
	{
		return restrictStratToReach;
	}

	/**
	 * Whether or not to do bisimulation minimisation before model checking.
	 */
	public boolean getDoBisim()
	{
		return doBisim;
	}

	public boolean getDoReach()
	{
		return doReach;
	}

	public boolean getBSCCComp()
	{
		return bsccComp;
	}

	public boolean getCheckZeroLoops()
	{
		return this.checkZeroLoops;
	}

	public int getConstruction()
	{
		return construction;
	}

	public int getOrdering()
	{
		return ordering;
	}

	public int getReachMethod()
	{
		return reachMethod;
	}

	/**
	 * Returns an integer containing flags for the C++ implementation of interval iteration,
	 * derived from the current settings object.
	 */
	public int getIntervalIterationFlags() throws PrismException
	{
		int flags = 0;

		OptionsIntervalIteration iiOptions = OptionsIntervalIteration.from(settings);

		if (iiOptions.isEnforceMonotonicityFromBelow())
			flags += 1;

		if (iiOptions.isEnforceMonotonicityFromAbove())
			flags += 2;

		if (iiOptions.isSelectMidpointForResult())
			flags += 4;  // select midpoint for result

		return flags;
	}



	public void addModelListener(PrismModelListener listener)
	{
		modelListeners.add(listener);
	}

	// String methods for options

	public static String getEngineString(int engine)
	{
		return engineStrings[engine];
	}

	/**
	 * Let PrismSettings object notify us things have changed
	 */
	public void notifySettings(PrismSettings settings)
	{
		if (cuddStarted) {
			JDD.SetCUDDEpsilon(settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON));
			try {
				long cuddMaxMem = PrismUtils.convertMemoryStringtoKB(getCUDDMaxMem());
				JDD.SetCUDDMaxMem(cuddMaxMem);
			} catch (PrismException e) {
				// Fail silently if memory string is invalid 
			}
		}
		jdd.SanityJDD.enabled = settings.getBoolean(PrismSettings.PRISM_JDD_SANITY_CHECKS);
		PrismSparse.SetExportIterations(settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS));
		PrismHybrid.SetExportIterations(settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS));
		PrismMTBDD.SetExportIterations(settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS));
	}

	//------------------------------------------------------------------------------
	// Access to parser, simulator, etc.
	//------------------------------------------------------------------------------

	/**
	 * Get (exclusive) access to the PRISM parser.
	 * Not usually used externally - use the ready-made model/property parse methods instead.
	 */
	public static PrismParser getPrismParser() throws InterruptedException
	{
		// Note: this mutex mechanism is based on public domain code by Doug Lea
		if (Thread.interrupted())
			throw new InterruptedException();
		// this code is synchronized on the whole Prism class
		// (because this is a static method)
		synchronized (Prism.class) {
			try {
				// wait until parser is free
				while (prismParserInUse) {
					Prism.class.wait();
				}
				// lock parser
				prismParserInUse = true;
				// return parser, creating anew if necessary
				if (thePrismParser == null)
					thePrismParser = new PrismParser();
				return thePrismParser;
			} catch (InterruptedException e) {
				Prism.class.notify();
				throw e;
			}
		}
	}

	/**
	 * Release (exclusive) access to the PRISM parser.
	 */
	public static synchronized void releasePrismParser()
	{
		prismParserInUse = false;
		Prism.class.notify();
	}

	/**
	 * Get the SimulatorEngine object for PRISM, creating if necessary.
	 */
	public SimulatorEngine getSimulator()
	{
		if (theSimulator == null) {
			theSimulator = new SimulatorEngine(this);
		}
		return theSimulator;
	}

	/**
	 * Get an SCCComputer object.
	 * Type (i.e. algorithm) depends on SCCMethod PRISM option.
	 */
	public SCCComputer getSCCComputer(Model model) throws PrismException
	{
		return SCCComputer.createSCCComputer(this, model);
	}

	/**
	 * Get an SCCComputer object.
	 * Type (i.e. algorithm) depends on SCCMethod PRISM option.
	 */
	public SCCComputer getSCCComputer(JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars) throws PrismException
	{
		return SCCComputer.createSCCComputer(this, reach, trans01, allDDRowVars, allDDColVars);
	}

	/**
	 * Get an SCCComputer object for the explicit engine.
	 * @param consumer the SCCConsumer
	 */
	public explicit.SCCComputer getExplicitSCCComputer(explicit.Model<?> model, explicit.SCCConsumer consumer) throws PrismException
	{
		return explicit.SCCComputer.createSCCComputer(this, model, consumer);
	}

	/**
	 * Get an ECComputer object.
	 */
	public ECComputer getECComputer(NondetModel model) throws PrismException
	{
		return ECComputer.createECComputer(this, model);
	}

	/**
	 * Get an ECComputer object.
	 */
	public ECComputer getECComputer(JDDNode reach, JDDNode trans, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars, JDDVars allDDNondetVars)
			throws PrismException
	{
		return ECComputer.createECComputer(this, reach, trans, trans01, allDDRowVars, allDDColVars, allDDNondetVars);
	}

	/**
	 * Get an ECComputer object for the explicit engine.
	 */
	public explicit.ECComputer getExplicitECComputer(explicit.NondetModel<?> model) throws PrismException
	{
		return explicit.ECComputer.createECComputer(this, model);
	}

	//------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------

	/**
	 * Get access to the list of all PRISM language keywords.
	 */
	public static List<String> getListOfKeywords()
	{
		return PrismParser.getListOfKeywords();
	}

	//------------------------------------------------------------------------------
	// Main API methods
	//------------------------------------------------------------------------------

	/**
	 * Initialise PRISM.
	 */
	public void initialise() throws PrismException
	{
		boolean verbose = this.getSettings().getBoolean("PRISM_VERBOSE");
		mainLog.setVerbosityLevel(verbose ? PrismLog.VL_ALL : PrismLog.VL_DEFAULT);
		mainLog.print(getToolName() + "\n");
		mainLog.print(new String(new char[getToolName().length()]).replace("\0", "=") + "\n");
		mainLog.print("\nVersion: " + getVersion() + "\n");
		mainLog.print("Date: " + new java.util.Date() + "\n");
		try {
			String h = java.net.InetAddress.getLocalHost().getHostName();
			mainLog.print("Hostname: " + h + "\n");
		} catch (java.net.UnknownHostException e) {
		}
		mainLog.print("Memory limits: cudd=" + getCUDDMaxMem());
		mainLog.println(", java(heap)=" + PrismUtils.convertBytesToMemoryString(Runtime.getRuntime().maxMemory()));

		// initialise cudd/jdd
		long cuddMaxMem = PrismUtils.convertMemoryStringtoKB(getCUDDMaxMem());
		JDD.InitialiseCUDD(cuddMaxMem, getCUDDEpsilon());
		cuddStarted = true;
		JDD.SetOutputStream(mainLog.getFilePointer());

		// initialise libraries/engines
		PrismNative.initialise(this);
		PrismMTBDD.initialise(mainLog, mainLog);
		PrismSparse.initialise(mainLog, mainLog);
		PrismHybrid.initialise(mainLog, mainLog);

		// set cudd manager in other packages
		DoubleVector.setCUDDManager();
		ODDUtils.setCUDDManager();
	}

	/**
	 * Set the random seed for the simulator.
	 */
	public void setSimulatorSeed(int seed) throws PrismException
	{
		getSimulator().setRandomNumberGeneratorSeed(seed);
	}

	/**
	 * Parse a PRISM model from a file.
	 * @param file File to read in
	 */
	public ModulesFile parseModelFile(File file) throws FileNotFoundException, PrismLangException
	{
		return parseModelFile(file, null);
	}

	/**
	 * Parse a PRISM model from a file.
	 * @param file File to read in
	 * @param typeOverride Optionally, override model type here (null if unused)
	 */
	public ModulesFile parseModelFile(File file, ModelType typeOverride) throws FileNotFoundException, PrismLangException
	{
		FileInputStream strModel;
		PrismParser prismParser;
		ModulesFile modulesFile = null;

		// open file
		strModel = new FileInputStream(file);

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse file
				modulesFile = prismParser.parseModulesFile(strModel, typeOverride);
			} finally {
				// release prism parser
				releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}

		modulesFile.tidyUp();

		return modulesFile;
	}

	/**
	 * Parse a PRISM model from a string.
	 * @param s String containing model
	 */
	public ModulesFile parseModelString(String s) throws PrismLangException
	{
		return parseModelString(s, null);
	}

	/**
	 * Parse a PRISM model from a string.
	 * @param s String containing model
	 * @param typeOverride Optionally, override model type here (null if unused)
	 */
	public ModulesFile parseModelString(String s, ModelType typeOverride) throws PrismLangException
	{
		PrismParser prismParser;
		ModulesFile modulesFile = null;

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse string
				modulesFile = prismParser.parseModulesFile(new ByteArrayInputStream(s.getBytes()), typeOverride);
			} finally {
				// release prism parser
				releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}

		modulesFile.tidyUp();

		return modulesFile;
	}

	/**
	 * Import a PRISM model from a PEPA model in a file
	 * @param file File to read in
	 */
	public ModulesFile importPepaFile(File file) throws PrismException, PrismLangException
	{
		return importModelFile("pepa", file);
	}

	/**
	 * Import a PRISM model from a PEPA model in a string
	 * @param s String containing model
	 */
	public ModulesFile importPepaString(String s) throws PrismException, PrismLangException
	{
		return importModelString("pepa", s);
	}

	/**
	 * Import a PRISM model from an SBML model in a file
	 * @param file File to read in
	 */
	public ModulesFile importSBMLFile(File file) throws PrismException, PrismLangException
	{
		return importModelFile("sbml", file);
	}

	/**
	 * Import a PRISM model from an SBML model in a string
	 * @param s String containing model
	 */
	public ModulesFile importSBMLString(String s) throws PrismException, PrismLangException
	{
		return importModelString("sbml", s);
	}

	/**
	 * Import a PRISM model by translating from another language
	 */
	public ModulesFile importModelFile(String lang, File file) throws PrismException, PrismLangException
	{
		PrismLanguageTranslator importer = createPrismLanguageTranslator(lang);
		importer.load(file);
		String prismModelString = importer.translateToString();
		return parseModelString(prismModelString);
	}

	/**
	 * Import a PRISM model by translating from another language
	 */
	public ModulesFile importModelString(String lang, String s) throws PrismException, PrismLangException
	{
		PrismLanguageTranslator importer = createPrismLanguageTranslator(lang);
		importer.load(s);
		String prismModelString = importer.translateToString();
		return parseModelString(prismModelString);
	}

	/**
	 * Create a translator to the PRISM language.
	 */
	private PrismLanguageTranslator createPrismLanguageTranslator(String lang) throws PrismException
	{
		PrismLanguageTranslator importer = null;
		switch (lang) {
		case "pepa":
			importer = new PEPA2Prism();
			break;
		case "sbml":
			importer = new SBML2Prism();
			break;
		default:
			throw new PrismException("Unknown import language \"" + lang + "\"");
		}
		return importer;
	}

	/**
	 * Import a PRISM model from a PRISM preprocessor file
	 * @param file File to read in
	 * @param params List of values for preprocessor parameters
	 */
	public ModulesFile importPrismPreprocFile(File file, String params[]) throws PrismException
	{
		String modelString;

		// Compile preprocessor file to a string
		Preprocessor pp = new Preprocessor(this, file);
		pp.setParameters(params);
		modelString = pp.preprocess();
		if (modelString == null) {
			throw new PrismException("No preprocessing information");
		}

		// Parse string as PRISM model and return
		return parseModelString(modelString);
	}

	/**
	 * Parse a PRISM properties file, using the currently loaded model
	 * for context (i.e. definitions of variables, constants, labels, etc.). 
	 * @param file File to read in
	 */
	public PropertiesFile parsePropertiesFile(File file) throws FileNotFoundException, PrismLangException
	{
		return parsePropertiesFile(currentModelInfo, file, true);
	}

	/**
	 * Parse a PRISM properties file, using a specific ModelInfo object (e.g. ModulesFile)
	 * for context (i.e. definitions of variables, constants, labels, etc.).
	 * Usually, just use {@link #parsePropertiesFile(File)}, which uses the currently loaded model. 
	 * @param modelInfo Accompanying model info (null if not needed)
	 * @param file File to read in
	 */
	public PropertiesFile parsePropertiesFile(ModelInfo modelInfo, File file) throws FileNotFoundException, PrismLangException
	{
		return parsePropertiesFile(modelInfo, file, true);
	}

	/**
	 * Parse a PRISM properties file, using the currently loaded model
	 * for context (i.e. definitions of variables, constants, labels, etc.). 
	 * You can also choose whether to do "tidy", i.e. post-parse checks and processing
	 * (this must be done at some point but may want to postpone to allow parsing of files with errors). 
	 * @param file File to read in
	 * @param tidy Whether or not to do "tidy" (post-parse checks and processing)
	 */
	public PropertiesFile parsePropertiesFile(File file, boolean tidy) throws FileNotFoundException, PrismLangException
	{
		return parsePropertiesFile(currentModelInfo, file, tidy);
	}

	/**
	 * Parse a PRISM properties file, using a specific ModelInfo object (e.g. ModulesFile)
	 * for context (i.e. definitions of variables, constants, labels, etc.).
	 * You can also choose whether to do "tidy", i.e. post-parse checks and processing
	 * (this must be done at some point but may want to postpone to allow parsing of files with errors). 
	 * Usually, just use {@link #parsePropertiesFile(File, boolean)}, which uses the currently loaded model. 
	 * @param modelInfo Accompanying model info (null if not needed)
	 * @param file File to read in
	 * @param tidy Whether or not to do "tidy" (post-parse checks and processing)
	 */
	public PropertiesFile parsePropertiesFile(ModelInfo modelInfo, File file, boolean tidy) throws FileNotFoundException, PrismLangException
	{
		FileInputStream strProperties;
		PrismParser prismParser;
		PropertiesFile propertiesFile = null;

		// open file
		strProperties = new FileInputStream(file);

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse file
				propertiesFile = prismParser.parsePropertiesFile(modelInfo, strProperties);
			} finally {
				// release prism parser
				releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}

		if (tidy)
			propertiesFile.tidyUp();

		return propertiesFile;
	}

	/**
	 * Parse a PRISM properties file from a string, using the currently loaded model
	 * for context (i.e. definitions of variables, constants, labels, etc.). 
	 * @param s String to parse
	 */
	public PropertiesFile parsePropertiesString(String s) throws PrismLangException
	{
		return parsePropertiesString(currentModelInfo, s);
	}

	/**
	 * Parse a PRISM properties file from a string, using a specific ModelInfo object (e.g. ModulesFile)
	 * for context (i.e. definitions of variables, constants, labels, etc.).
	 * Usually, just use {@link #parsePropertiesString(String)}, which uses the currently loaded model. 
	 * @param modelInfo Accompanying model info (null if not needed)
	 * @param s String to parse
	 */
	public PropertiesFile parsePropertiesString(ModelInfo modelInfo, String s) throws PrismLangException
	{
		PrismParser prismParser;
		PropertiesFile propertiesFile = null;

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse string
				propertiesFile = prismParser.parsePropertiesFile(modelInfo, new ByteArrayInputStream(s.getBytes()));
			} finally {
				// release prism parser
				releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}

		propertiesFile.tidyUp();

		return propertiesFile;
	}

	/**
	 * Parse a single PRISM expression from a string.
	 * @param s String to parse
	 */
	public static Expression parseSingleExpressionString(String s) throws PrismLangException
	{
		PrismParser prismParser;
		Expression expr;

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse expression
				expr = prismParser.parseSingleExpression(new ByteArrayInputStream(s.getBytes()));
			} finally {
				// release prism parser
				releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}

		return expr;
	}

	/**
	 * Parse a for-loop (as used e.g. in preprocessor) from a string.
	 * @param s String to parse
	 */
	public ForLoop parseForLoopString(String s) throws PrismLangException
	{
		PrismParser prismParser;
		ForLoop fl;

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse for loop
				fl = prismParser.parseForLoop(new ByteArrayInputStream(s.getBytes()));
			} finally {
				// release prism parser
				releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}

		return fl;
	}

	/**
	 * Clear storage of the current model, if any.
	 */
	public void clearModel() throws PrismException
	{
		currentModelSource = null;
		currentModelType = null;
		currentModelInfo = null;
		currentModulesFile = null;
		currentModelGenerator = null;
		currentRewardGenerator = null;
		currentDefinedMFConstants = null;
		// Clear built model storage too
		clearBuiltModel();
	}
	
	/**
	 * Load a (parsed) PRISM model, which will be stored and used for subsequent model checking etc.
	 * Some model constants can still be undefined at this stage.
	 * Pass in null to clear storage of the current model.
	 * @param modulesFile The PRISM model
	 */
	public void loadPRISMModel(ModulesFile modulesFile) throws PrismException
	{
		if (modulesFile == null) {
			clearModel();
		}
		// Update model info
		currentModelSource = ModelSource.PRISM_MODEL;
		currentModelType = modulesFile.getModelType();
		currentModelInfo = modulesFile;
		currentModulesFile = modulesFile;
		// Don't create ModelGenerator yet; do it on demand
		currentModelGenerator = null;
		// For now, use ModulesFile as a RewardGenerator (via reward struct objects)
		// If we build a ModelGenerator too, that can be used for rewards too
		currentRewardGenerator = modulesFile;
		currentDefinedMFConstants = null;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Print basic model info
		mainLog.println();
		mainLog.println("Type:        " + currentModelInfo.getModelType());
		mainLog.println("Modules:     " + String.join(" ", currentModulesFile.getModuleNames()));
		mainLog.println("Variables:   " + String.join(" ", currentModelInfo.getVarNames()));
		if (currentModulesFile.getModelType().partiallyObservable()) {
			mainLog.println("Observables: " + String.join(" ", currentModulesFile.getObservableNames()));
		}

		// If required, export parsed PRISM model
		if (exportPrism) {
			try {
				exportPRISMModel(exportPrismFile);
			}
			// In case of error, just print a warning
			catch (FileNotFoundException e) {
				mainLog.printWarning("PRISM code export failed: Couldn't open file \"" + exportPrismFile + "\" for output");
			} catch (PrismException e) {
				mainLog.printWarning("PRISM code export failed: " + e.getMessage());
			}
		}
	}

	/**
	 * Load a model generator, which will be stored and used for subsequent model checking etc.
	 * Some model constants can still be undefined at this stage.
	 * Pass in null to clear storage of the current model.
	 * @param modelGen The model generator
	 */
	public void loadModelGenerator(ModelGenerator<?> modelGen) throws PrismException
	{
		if (modelGen == null) {
			clearModel();
		}
		// Update model info
		currentModelSource = ModelSource.MODEL_GENERATOR;
		currentModelType = modelGen.getModelType();
		currentModelInfo = modelGen;
		currentModulesFile = null;
		currentModelGenerator = modelGen;
		// Create a blank reward generator if nor provided by the model generator
		if (modelGen instanceof RewardGenerator) {
			currentRewardGenerator = (RewardGenerator<?>) modelGen;
		} else {
			currentRewardGenerator = new RewardGenerator<Object>() {};
		}
		currentDefinedMFConstants = null;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Print basic model info
		mainLog.println();
		mainLog.println("Generator: " + currentModelGenerator.getClass().getName());
		mainLog.println("Type:      " + currentModelInfo.getModelType());
		mainLog.println("Variables: " + String.join(" ", currentModelInfo.getVarNames()));
	}

	/**
	 * Set (some or all) undefined constants for the currently loaded PRISM model
	 * (assuming they have changed since the last time this was called).
	 * <br>
	 * Constants are evaluated using standard (integer, floating-point) arithmetic.
	 * @param definedMFConstants The constant values
	 */
	public void setPRISMModelConstants(Values definedMFConstants) throws PrismException
	{
		setPRISMModelConstants(definedMFConstants, false);
	}

	/**
	 * Set (some or all) undefined constants for the currently loaded PRISM model
	 * (assuming they have changed since the last time this was called).
	 * @param definedMFConstants The constant values
	 * @param exact if true, do exact evaluation of constants (using BigRational)
	 */
	public void setPRISMModelConstants(Values definedMFConstants, boolean exact) throws PrismException
	{
		// If there is no change in constants, there is nothing to do
		boolean currentMFNone = currentDefinedMFConstants == null || currentDefinedMFConstants.getNumValues() == 0;
		boolean newMFNone = definedMFConstants == null || definedMFConstants.getNumValues() == 0;
		if (currentMFNone && newMFNone && currentDefinedMFConstantsAreExact == exact) {
			return;
		}
		if (currentDefinedMFConstants != null &&
		    currentDefinedMFConstants.equals(definedMFConstants) &&
		    currentDefinedMFConstantsAreExact == exact) {
			return;
		}

		// Clear any existing built model(s)
		clearBuiltModel();
		// Store constants here and in ModulesFile
		currentDefinedMFConstants = definedMFConstants;
		currentDefinedMFConstantsAreExact = exact;
		if (currentModulesFile != null) {
			currentModulesFile.setSomeUndefinedConstants(definedMFConstants, exact);
		}
		if (currentModelGenerator != null) {
			currentModelGenerator.setSomeUndefinedConstants(definedMFConstants, exact);
		}

		// If required, export parsed PRISM model, with constants expanded
		if (exportPrismConst) {
			try {
				exportPRISMModelWithExpandedConstants(exportPrismConstFile);
			}
			// In case of error, just print a warning
			catch (FileNotFoundException e) {
				mainLog.printWarning("PRISM code export failed: Couldn't open file \"" + exportPrismConstFile + "\" for output");
			} catch (PrismException e) {
				mainLog.printWarning("PRISM code export failed: " + e.getMessage());
			}
		}
	}

	/**
	 * Load a (built) model, with an accompanying (parsed) PRISM model.
	 * These will be stored and used for subsequent model checking etc.
	 * Pass in nulls to clear storage of the current model.
	 * @param modulesFile The PRISM model
	 */
	public void loadPRISMModelAndBuiltModel(ModulesFile modulesFile, Model model)
	{
		currentModelSource = ModelSource.PRISM_MODEL;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Store model info
		currentModulesFile = modulesFile;
		currentModel = model;
		currentModelBuildType = ModelBuildType.SYMBOLIC;
		// Reset dependent info
		currentModelType = currentModulesFile == null ? null : currentModulesFile.getModelType();
		currentModelInfo = currentModulesFile;
		currentDefinedMFConstants = null;
	}

	/**
	 * Load a (built) model, without an accompanying (parsed) PRISM model.
	 * The model will be stored and used for subsequent model checking etc.
	 * Pass in null to clear storage of the current model.
	 * @param model The built model
	 */
	public void loadBuiltModel(Model model)
	{
		currentModelSource = ModelSource.BUILT_MODEL;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Store model info
		currentModulesFile = null;
		currentModel = model;
		currentModelBuildType = ModelBuildType.SYMBOLIC;
		// Reset dependent info
		currentModelType = currentModel == null ? null : currentModel.getModelType();
		currentDefinedMFConstants = null;
	}

	/**
	 * Load files containing an explicit list of transitions/etc. for subsequent model building.
	 * @param statesFile File containing a list of states (optional, can be null)
	 * @param transFile File containing the list of transitions (required)
	 * @param labelsFile File containing label definitions (optional, can be null)
	 * @param stateRewardsFiles Files containing state reward definitions (optional, can be null)
	 * @param typeOverride Model type (auto-detected if {@code null})
	 */
	public void loadModelFromExplicitFiles(File statesFile, File transFile, File labelsFile, List<File> stateRewardsFiles, ModelType typeOverride) throws PrismException
	{
		currentModelSource = ModelSource.EXPLICIT_FILES;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Construct ModelInfo
		ExplicitFiles2ModelInfo ef2mi = new ExplicitFiles2ModelInfo(this);
		currentModelInfo = ef2mi.buildModelInfo(statesFile, transFile, labelsFile, typeOverride);
		currentModulesFile = null;
		// Construct reward generator
		currentRewardGenerator = ef2mi.buildRewardInfo((stateRewardsFiles == null || stateRewardsFiles.isEmpty()) ? null : stateRewardsFiles.get(0));
		// Store explicit files info for later
		explicitFilesStatesFile = statesFile;
		explicitFilesTransFile = transFile;
		explicitFilesLabelsFile = labelsFile;
		explicitFilesStateRewardsFiles = stateRewardsFiles == null ? new ArrayList<>() : new ArrayList<>(stateRewardsFiles);
		explicitFilesNumStates = ef2mi.getNumStates();
		// Reset dependent info
		currentModelType = currentModelInfo == null ? null : currentModelInfo.getModelType();
		currentDefinedMFConstants = null;
	}

	/**
	 * Get the type of the currently stored model.
	 */
	public ModelType getModelType()
	{
		return currentModelType;
	}

	/**
	 * Get the currently stored (parsed) PRISM model.
	 */
	public ModulesFile getPRISMModel()
	{
		return currentModulesFile;
	}

	/**
	 * Get the currently selected model checking engine.
	 */
	public PrismEngine getCurrentEngine()
	{
		if (getEngine() == Prism.EXPLICIT) {
			return PrismEngine.EXPLICIT;
		} else {
			return PrismEngine.SYMBOLIC;
		}
	}
	
	/**
	 * Get the ModelGenerator for the currently loaded model.
	 * This will have either have been loaded directly
	 * using {@link #loadModelGenerator(ModelGenerator)}
	 * or is created automatically, e.g., for a PRISM model that has been loaded
	 * using {@link #loadPRISMModel(ModulesFile)}.
	 * If it needs to be created, it will be stored as well as returned.
	 * An accompanying reward generator may also be created.
	 * Throw an explanatory exception if not possible.
	 */
	public ModelGenerator<?> getModelGenerator() throws PrismException
	{
		// Only rebuild if not already present
		if (currentModelGenerator == null) {
			switch (currentModelSource) {
			case PRISM_MODEL:
				if (currentModulesFile != null) {
					// Create a model generator via ModulesFileModelGenerator
					currentModelGenerator = ModulesFileModelGenerator.create(currentModulesFile, this);
					currentRewardGenerator = ((ModulesFileModelGenerator<?>) currentModelGenerator);
				} else {
					throw new PrismException("There is no currently loaded PRISM model");
				}
				break;
			case MODEL_GENERATOR:
				// Not present - must be an error
				throw new PrismException("There is no currently loaded model generator");
			case BUILT_MODEL:
			case EXPLICIT_FILES:
				// If possible, model building from files creates a ModelGenerator too 
				buildModelIfRequired();
				break;
			default:
				throw new PrismException("Unknown model source");
			}
		}
		// Default error message if we failed to create a ModelGenerator
		if (currentModelGenerator == null) {
			throw new PrismException("Could not create a model generator");
		}
		return currentModelGenerator;
	}

	/**
	 * Get the values that have been provided for undefined constants in the current model.
	 */
	public Values getUndefinedModelValues()
	{
		return currentDefinedMFConstants;
	}
	
	/**
	 * Get the currently stored built (symbolic) model.
	 */
	public Model getBuiltModel()
	{
		return currentModel;
	}

	/**
	 * Get the currently stored built explicit model.
	 */
	public explicit.Model<?> getBuiltModelExplicit()
	{
		return currentModelExpl;
	}

	/**
	 * Returns true if the current model is of a type that can be built (e.g. not a PTA).
	 */
	public boolean modelCanBeBuilt()
	{
		if (currentModelType.realTime())
			return false;
		return true;
	}

	/**
	 * Get the model build type used/needed by a given model checking engine.
	 * Returns null if model is not needed (or unknown).
	 */
	public ModelBuildType getModelBuildTypeForEngine(PrismEngine engine)
	{
		switch (engine) {
		case EXPLICIT:
			return ModelBuildType.EXPLICIT;
		case SYMBOLIC:
			return ModelBuildType.SYMBOLIC;
		default:
			return null;
		}
	}
	
	/**
	 * Returns true if the current model has been built (for the currently selected engine).
	 */
	public boolean modelIsBuilt()
	{
		ModelBuildType modelBuildTypeForEngine = getModelBuildTypeForEngine(getCurrentEngine());
		return modelBuildTypeForEngine != null && currentModelBuildType == modelBuildTypeForEngine;
	}

	/**
	 * Get the currently stored strategy (null if none)
	 */
	public Strategy<?> getStrategy()
	{
		return strategy;
	}

	/**
	 * Build the currently loaded PRISM model and store for later use.
	 * The built model can be accessed subsequently via either
	 * {@link #getBuiltModel()} or {@link #getBuiltModelExplicit()},
	 * depending on the engine currently selected.
	 * Only call this to explicitly force a built; normally it is done automatically.
	 */
	public void buildModel() throws PrismException
	{
		mainLog.printSeparator();
		chooseEngineForModelBuild();
		doBuildModel();
	}

	/**
	 * Build the currently loaded PRISM model, if it needs to be done,
	 * i.e. if it has not been constructed yet for the current engine.
	 */
	public void buildModelIfRequired() throws PrismException
	{
		chooseEngineForModelBuild();
		if (!modelIsBuilt())
			doBuildModel();
	}

	/**
	 * If required (based on model type), switch engine to enable building.
	 */
	private void chooseEngineForModelBuild()
	{
		// For some models, automatically switch engine
		switch (currentModelType) {
			case IDTMC:
			case IMDP:
			case LTS:
			case POMDP:
				if (getCurrentEngine() == PrismEngine.SYMBOLIC) {
					mainLog.println("\nSwitching to explicit engine, which supports " + currentModelType + "s...");
					engineOld = getEngine();
					engineSwitched = true;
					try {
						setEngine(Prism.EXPLICIT);
					} catch (PrismException e) {
						// Won't happen
					}
				}
				break;
			// For other models, switch engine back if changed earlier
			default:
				if (engineSwitched) {
					try {
						setEngine(engineOld);
					} catch (PrismException e) {
						// Won't happen
					}
					engineSwitched = false;
				}
		}
	}

	/**
	 * Build the currently loaded PRISM model and store for later use.
	 * The built model can be accessed subsequently via either
	 * {@link #getBuiltModel()} or {@link #getBuiltModelExplicit()},
	 * depending on the engine currently selected.
	 */
	private void doBuildModel() throws PrismException
	{
		long l; // timer

		// Clear any existing built model(s)
		clearBuiltModel();

		try {
			if (currentModelType.realTime()) {
				throw new PrismException("You cannot build a " + currentModelType + " model explicitly, only perform model checking");
			}

			mainLog.print("\nBuilding model...\n");
			if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
				mainLog.println("Model constants: " + currentDefinedMFConstants);

			// Build model
			l = System.currentTimeMillis();
			
			switch (getCurrentEngine()) {
			case SYMBOLIC:
				switch (currentModelSource) {
				case PRISM_MODEL:
					Modules2MTBDD mod2mtbdd = new Modules2MTBDD(this, currentModulesFile);
					currentModel = mod2mtbdd.translate();
					currentModelExpl = null;
					currentModelBuildType = ModelBuildType.SYMBOLIC;
					break;				
				case MODEL_GENERATOR:
					ModelGenerator2MTBDD modelGen2mtbdd = new ModelGenerator2MTBDD(this);
					ModelGenerator<Double> modelGenDbl = (ModelGenerator<Double>) getModelGenerator();
					currentModel = modelGen2mtbdd.build(modelGenDbl, (RewardGenerator<Double>) currentRewardGenerator);
					currentModelExpl = null;
					currentModelBuildType = ModelBuildType.SYMBOLIC;
					break;
				case EXPLICIT_FILES:
					ExplicitFiles2MTBDD expf2mtbdd = new ExplicitFiles2MTBDD(this);
					ExplicitFilesRewardGenerator4MTBDD erfg4m = new ExplicitFilesRewardGenerator4MTBDD(this, explicitFilesStateRewardsFiles, explicitFilesNumStates);
					currentModel = expf2mtbdd.build(explicitFilesStatesFile, explicitFilesTransFile, explicitFilesLabelsFile, currentModelInfo, explicitFilesNumStates, erfg4m);
					currentModelExpl = null;
					currentModelBuildType = ModelBuildType.SYMBOLIC;
					// Also build a RewardGenerator
					// (needed e.g. when (multiple) reward files are later exported)
					currentRewardGenerator = erfg4m;
					// No current support for building a ModelGenerator
					// (e.g. for simulation)
					break;
				default:
					throw new PrismException("Cannot do symbolic model construction for model source " + currentModelSource);
				}
				break;
			case EXPLICIT:
				switch (currentModelSource) {
				case PRISM_MODEL:
				case MODEL_GENERATOR:
					try {
						getModelGenerator();
					} catch (PrismException e){
						throw e.prepend("Explicit engine: ");
					}
					ConstructModel constructModel = new ConstructModel(this);
					constructModel.setFixDeadlocks(getFixDeadlocks());
					currentModelExpl = constructModel.constructModel(currentModelGenerator);
					currentModel = null;
					currentModelBuildType = ModelBuildType.EXPLICIT;
					break;
				case EXPLICIT_FILES:
					ExplicitFiles2Model expf2model = new ExplicitFiles2Model(this);
					currentModelExpl = expf2model.build(explicitFilesStatesFile, explicitFilesTransFile, explicitFilesLabelsFile, currentModelInfo, explicitFilesNumStates);
					currentModel = null;
					currentModelBuildType = ModelBuildType.EXPLICIT;
					// Also build a Model/RewardGenerator
					// (the latter since rewards are built later, the former e.g. for simulation)
					currentModelGenerator = new ModelModelGenerator<>(currentModelExpl, currentModelInfo);
					ExplicitFilesRewardGenerator efrg4e = new ExplicitFilesRewardGenerator4Explicit(this, explicitFilesStateRewardsFiles, explicitFilesNumStates);
					efrg4e.setStatesList(currentModelExpl.getStatesList());
					currentRewardGenerator = efrg4e;
					break;
				default:
					throw new PrismException("Cannot do explicit model construction for model source " + currentModelSource);
				}
				break;
			default:
				throw new PrismException("Unknown engine " + getCurrentEngine());
			}
			
			l = System.currentTimeMillis() - l;
			mainLog.println("\nTime for model construction: " + l / 1000.0 + " seconds.");

			// For digital clocks, do some extra checks on the built model
			if (digital) {
				doBuildModelDigitalClocksChecks();
			}

			// Deal with deadlocks
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				StateList deadlocks = currentModel.getDeadlockStates();
				int numDeadlocks = deadlocks.size();
				if (numDeadlocks > 0) {
					if (getFixDeadlocks()) {
						mainLog.printWarning("Deadlocks detected and fixed in " + numDeadlocks + " states");
					} else {
						currentModel.printTransInfo(mainLog, getExtraDDInfo());
						mainLog.print("\n" + numDeadlocks + " deadlock states found");
						if (!getVerbose() && numDeadlocks > 10) {
							mainLog.print(". The first 10 are below. Use verbose mode to view them all.\n");
							deadlocks.print(mainLog, 10);
						} else {
							mainLog.print(":\n");
							deadlocks.print(mainLog);
						}
						mainLog.print("\nTip: Use the \"fix deadlocks\" option to automatically add self-loops in deadlock states.\n");
						throw new PrismException("Model contains " + numDeadlocks + " deadlock states");
					}
				}
			} else {
				explicit.StateValues deadlocks = currentModelExpl.getDeadlockStatesList();
				int numDeadlocks = currentModelExpl.getNumDeadlockStates();
				if (numDeadlocks > 0) {
					if (getFixDeadlocks()) {
						mainLog.printWarning("Deadlocks detected and fixed in " + numDeadlocks + " states");
					} else {
						mainLog.print(currentModelExpl.infoStringTable());
						mainLog.print("\n" + numDeadlocks + " deadlock states found");
						if (!getVerbose() && numDeadlocks > 10) {
							mainLog.print(". The first 10 are below. Use verbose mode to view them all.\n");
							deadlocks.print(mainLog, 10);
						} else {
							mainLog.print(":\n");
							deadlocks.print(mainLog);
						}
						mainLog.print("\nTip: Use the \"fix deadlocks\" option to automatically add self-loops in deadlock states.\n");
						throw new PrismException("Model contains " + numDeadlocks + " deadlock states");
					}
				}
			}

			// Print model stats
			mainLog.println();
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				mainLog.println("Type:        " + currentModel.getModelType());
				currentModel.printTransInfo(mainLog, getExtraDDInfo());
			} else {
				mainLog.println("Type:        " + currentModelExpl.getModelType());
				mainLog.print(currentModelExpl.infoStringTable());
			}

			// Notify model listeners of build success
			for (PrismModelListener listener : modelListeners) {
				if (listener != null)
					listener.notifyModelBuildSuccessful();
			}
		} catch (PrismException e) {
			// Notify model listeners of build failure
			for (PrismModelListener listener : modelListeners) {
				if (listener != null)
					listener.notifyModelBuildFailed(e);
			}
			// Throw exception anyway
			throw e;
		}
	}

	private void doBuildModelDigitalClocksChecks() throws PrismException
	{
		// For digital clocks, by construction, deadlocks can only occur from timelocks (and are not allowed)
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			StateList deadlocks = currentModel.getDeadlockStates();
			if (deadlocks.size() > 0) {
				throw new PrismException("Timelock in PTA, e.g. in state (" + deadlocks.getFirstAsValues() + ")");
			}
		} else {
			if (currentModelExpl.getNumDeadlockStates() > 0) {
				int dl = currentModelExpl.getFirstDeadlockState();
				String dls = currentModelExpl.getStatesList().get(dl).toString(currentModelInfo);
				throw new PrismException("Timelock in PTA, e.g. in state " + dls);
			}
		}

		/*// Create new model checker object and do model checking
		PropertiesFile pf = parsePropertiesString("filter(exists,!\"invariants\"); E[F!\"invariants\"]");
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			ModelChecker mc = new NondetModelChecker(this, currentModel, pf);
			if (((Boolean) mc.check(pf.getProperty(0)).getResult()).booleanValue()) {
				mainLog.println(mc.check(pf.getProperty(1)).getCounterexample());
			}
			//sv.pr
			//mainLog.println("XX" + res.getResult());
		} else {
			explicit.StateModelChecker mc = new MDPModelChecker();
			mc.setLog(mainLog);
			mc.setSettings(settings);
			mc.setModulesFileAndPropertiesFile(currentModulesFile, pf);
			explicit.StateValues sv = mc.checkExpression(currentModelExpl, pf.getProperty(0));
			sv.print(mainLog, 1);
		}*/
	}

	/**
	 * Build a model from a PRISM modelling language description, storing it symbolically,
	 * as MTBDDs) via explicit-state reachability and model construction.
	 * It is assumed that all constants in the model file have been defined by now.  
	 * @param modulesFile Model to build
	 */
	public Model buildModelExplicit(ModulesFile modulesFile) throws PrismException
	{
		long l; // timer
		ConstructModel constructModel;
		explicit.Model<?> modelExpl;
		Model model;
		List<State> statesList;

		if (modulesFile.getModelType().realTime()) {
			throw new PrismException("You cannot build a " + modulesFile.getModelType() + " model explicitly, only perform model checking");
		}

		mainLog.print("\nBuilding model...\n");
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);

		constructModel = new ConstructModel(this);
		ModelGenerator<Double> modelGen = ModulesFileModelGenerator.createForDoubles(modulesFile, this);
		modelExpl = constructModel.constructModel(modelGen);
		statesList = constructModel.getStatesList();

		// create Explicit2MTBDD object
		ExplicitModel2MTBDD expm2mtbdd = new ExplicitModel2MTBDD(this);

		// build model
		l = System.currentTimeMillis();
		model = expm2mtbdd.buildModel((explicit.Model<Double>) modelExpl, statesList, modulesFile, false);
		l = System.currentTimeMillis() - l;

		mainLog.println("\nTime for model construction: " + l / 1000.0 + " seconds.");

		return model;
	}

	/**
	 * Export the currently loaded and parsed PRISM model to a file.
	 * @param file File to export to
	 */
	public void exportPRISMModel(File file) throws FileNotFoundException, PrismException
	{
		mainLog.print("\nExporting parsed PRISM file ");
		mainLog.println(getDestinationStringForFile(file));
		PrismLog tmpLog = getPrismLogForFile(file);
		tmpLog.print(currentModulesFile.toString());
		// tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the currently loaded and parsed PRISM model to a file,
	 * after expanding all constants to their actual, defined values.
	 * @param file File to export to
	 */
	public void exportPRISMModelWithExpandedConstants(File file) throws FileNotFoundException, PrismException
	{
		mainLog.print("\nExporting parsed PRISM file (with constant expansion) ");
		mainLog.println(getDestinationStringForFile(file));
		PrismLog tmpLog = getPrismLogForFile(file);
		ModulesFile mfTmp = (ModulesFile) currentModulesFile.deepCopy();
		mfTmp = (ModulesFile) mfTmp.replaceConstants(currentModulesFile.getConstantValues());
		// NB: Don't use simplify() here because doesn't work for the purposes of printing out
		// (e.g. loss of parentheses causes precedence problems)
		tmpLog.print(mfTmp.toString());
		// tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the currently loaded model's transition matrix to a Spy file.
	 * @param file File to export to
	 */
	public void exportToSpyFile(File file) throws FileNotFoundException, PrismException
	{
		int depth;
		JDDNode tmp;

		if (getCurrentEngine() == PrismEngine.EXPLICIT) {
			throw new PrismNotSupportedException("Export to Spy file not yet supported by explicit engine");
		}

		// Build model, if necessary
		buildModelIfRequired();

		mainLog.println("\nExporting to spy file \"" + file + "\"...");

		// choose depth
		depth = currentModel.getAllDDRowVars().n();
		if (depth > 9)
			depth = 9;

		// get rid of non det vars if necessary
		tmp = currentModel.getTrans();
		JDD.Ref(tmp);
		if (currentModelType == ModelType.MDP) {
			tmp = JDD.MaxAbstract(tmp, ((NondetModel) currentModel).getAllDDNondetVars());
		}

		// export to spy file
		JDD.ExportMatrixToSpyFile(tmp, currentModel.getAllDDRowVars(), currentModel.getAllDDColVars(), depth, file.getPath());
		JDD.Deref(tmp);
	}

	/**
	 * Export the MTBDD for the currently loaded model's transition matrix to a Dot file.
	 * @param file File to export to
	 */
	public void exportToDotFile(File file) throws FileNotFoundException, PrismException
	{
		if (getCurrentEngine() == PrismEngine.EXPLICIT) {
			throw new PrismNotSupportedException("Export to Dot file not yet supported by explicit engine");
		}

		// Build model, if necessary
		buildModelIfRequired();

		// Check again (in case engine was switched)
		if (getCurrentEngine() == PrismEngine.EXPLICIT) {
			throw new PrismNotSupportedException("Export to Dot file not yet supported by explicit engine");
		}
		
		// Export to dot file
		mainLog.println("\nExporting to dot file \"" + file + "\"...");
		JDD.ExportDDToDotFileLabelled(currentModel.getTrans(), file.getPath(), currentModel.getDDVarNames());
	}

	/**
	 * Export the currently loaded model's transition matrix to a file (or to the log)
	 * @param ordered Ensure that (source) states are in ascending order?
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_DOT}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_ROWS}
	 * <li> {@link #EXPORT_DOT_STATES}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportTransToFile(boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		// can only do ordered version of export for MDPs
		if (currentModelType == ModelType.MDP) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition matrix for MDPs; using ordered.");
			ordered = true;
		}
		// can only do ordered version of export for MRMC
		if (exportType == EXPORT_MRMC) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition matrix in MRMC format; using ordered.");
			ordered = true;
		}
		// can only do ordered version of export for rows format
		if (exportType == EXPORT_ROWS) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition matrix in rows format; using ordered.");
			ordered = true;
		}

		// Build model, if necessary
		buildModelIfRequired();

		// print message
		mainLog.print("\nExporting transition matrix ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// do export
		int precision = settings.getInteger(PrismSettings.PRISM_EXPORT_MODEL_PRECISION);
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			currentModel.exportToFile(exportType, ordered, file, precision);
		} else {
			PrismLog tmpLog = getPrismLogForFile(file);
			switch (exportType) {
			case Prism.EXPORT_PLAIN:
				currentModelExpl.exportToPrismExplicitTra(tmpLog, precision);
				break;
			case Prism.EXPORT_MATLAB:
				throw new PrismNotSupportedException("Export not yet supported");
			case Prism.EXPORT_DOT:
				currentModelExpl.exportToDotFile(tmpLog, precision);
				break;
			case Prism.EXPORT_DOT_STATES:
				currentModelExpl.exportToDotFile(tmpLog, null, true, precision);
				break;
			case Prism.EXPORT_MRMC:
			case Prism.EXPORT_ROWS:
				throw new PrismNotSupportedException("Export not yet supported");
			}
			tmpLog.close();
		}

		// for export to dot with states, need to do a bit more
		if (currentModelBuildType == ModelBuildType.SYMBOLIC && exportType == EXPORT_DOT_STATES) {
			// open (appending to) existing new file log or use main log
			PrismLog tmpLog = getPrismLogForFile(file, true);
			// insert states info into dot file
			currentModel.getReachableStates().printDot(tmpLog);
			// print footer
			tmpLog.println("}");
			// tidy up
			if (file != null)
				tmpLog.close();
		}
	}

	/**
	 * Export the currently loaded model's state rewards to a file (or files, or stdout).
	 * If there is more than 1 reward structure, then multiple files are generated
	 * (e.g. "rew.sta" becomes "rew1.sta", "rew2.sta", ...)
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_MRMC}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		int numRewardStructs = currentRewardGenerator.getNumRewardStructs();
		if (numRewardStructs == 0) {
			mainLog.println("\nOmitting state reward export as there are no reward structures");
			return;
		}
		
		// Rows format does not apply to vectors
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		mainLog.print("\nExporting state rewards ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// Do export, writing to multiple files if necessary
		int precision = settings.getInteger(PrismSettings.PRISM_EXPORT_MODEL_PRECISION);
		boolean noexportheaders = !settings.getBoolean(PrismSettings.PRISM_EXPORT_MODEL_HEADERS);
		List <String> files = new ArrayList<>();
		for (int r = 0; r < numRewardStructs; r++) {
			String filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, r + 1);
				files.add(filename);
			}
			File fileToUse = (filename == null) ? null : new File(filename);
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				currentModel.exportStateRewardsToFile(r, exportType, fileToUse, precision, noexportheaders);
			} else {
				explicit.StateModelChecker mcExpl = createModelCheckerExplicit(null);
				try (PrismLog out = getPrismLogForFile(fileToUse)){
					((explicit.ProbModelChecker) mcExpl).exportStateRewardsToFile(currentModelExpl, r, exportType, out, noexportheaders, precision);
				} catch (PrismNotSupportedException e1) {
					mainLog.println("\nReward export failed: " + e1.getMessage());
					try {
						if (fileToUse != null) {
							fileToUse.delete();
						}
					} catch (SecurityException e2) {
						// Cannot delete File; continue
					}
				}
			}
		}
		
		if (files.size() > 1) {
			mainLog.println("Rewards were exported to multiple files: " + PrismUtils.joinString(files, ","));
		}
	}
	
	/**
	 * Export the currently loaded model's transition rewards to a file
	 * @param ordered Ensure that (source) states are in ascending order?
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_ROWS}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportTransRewardsToFile(boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		int numRewardStructs = currentRewardGenerator.getNumRewardStructs();
		if (numRewardStructs == 0) {
			mainLog.println("\nOmitting transition reward export as there are no reward structures");
			return;
		}
		
		// Can only do ordered version of export for MDPs
		if (currentModelType == ModelType.MDP) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition reward matrix for MDPs; using ordered.");
			ordered = true;
		}
		// Can only do ordered version of export for MRMC
		if (exportType == EXPORT_MRMC) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition reward matrix in MRMC format; using ordered.");
			ordered = true;
		}
		// Can only do ordered version of export for rows format
		if (exportType == EXPORT_ROWS) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition matrix in rows format; using ordered.");
			ordered = true;
		}

		// Build model, if necessary
		buildModelIfRequired();

		mainLog.print("\nExporting transition rewards ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// Do export, writing to multiple files if necessary
		int precision = settings.getInteger(PrismSettings.PRISM_EXPORT_MODEL_PRECISION);
		boolean noexportheaders = !settings.getBoolean(PrismSettings.PRISM_EXPORT_MODEL_HEADERS);
		List <String> files = new ArrayList<>();
		for (int r = 0; r < numRewardStructs; r++) {
			String filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, r + 1);
				files.add(filename);
			}
			File fileToUse = (filename == null) ? null : new File(filename);
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				currentModel.exportTransRewardsToFile(r, exportType, ordered, fileToUse, precision, noexportheaders);
			} else {
				explicit.StateModelChecker mcExpl = createModelCheckerExplicit(null);
				try (PrismLog out = getPrismLogForFile(fileToUse)){
					((explicit.ProbModelChecker) mcExpl).exportTransRewardsToFile(currentModelExpl, r, exportType, out, noexportheaders, precision);
				} catch (PrismNotSupportedException e1) {
					mainLog.println("\nReward export failed: " + e1.getMessage());
					try {
						if (fileToUse != null) {
							fileToUse.delete();
						}
					} catch (SecurityException e2) {
						// Cannot delete File; continue
					}
				}
			}
		}
		
		if (files.size() > 1) {
			mainLog.println("Rewards were exported to multiple files: " + PrismUtils.joinString(files, ","));
		}
	}

	/**
	 * Export the currently loaded model's bottom strongly connected components (BSCCs) to a file
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportBSCCsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		int i, n;
		long l; // timer
		PrismLog tmpLog;
		SCCComputer sccComputer = null;
		explicit.SCCConsumerStore sccConsumerExpl = null;
		//Vector<JDDNode> bsccs;
		//JDDNode not, bscc;

		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		// Compute BSCCs
		mainLog.println("\nComputing BSCCs...");
		l = System.currentTimeMillis();
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			sccComputer = getSCCComputer(currentModel);
			sccComputer.computeBSCCs();
		} else {
			sccConsumerExpl = new explicit.SCCConsumerStore();
			getExplicitSCCComputer(currentModelExpl, sccConsumerExpl).computeSCCs();
		}
		l = System.currentTimeMillis() - l;
		mainLog.println("\nTime for BSCC computation: " + l / 1000.0 + " seconds.");

		// print message
		mainLog.print("\nExporting BSCCs ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(file);

		// print header: list of model vars
		if (exportType == EXPORT_MATLAB)
			tmpLog.print("% ");
		tmpLog.print("Variables: (");
		for (i = 0; i < currentModelInfo.getNumVars(); i++) {
			tmpLog.print(currentModelInfo.getVarName(i));
			if (i < currentModelInfo.getNumVars() - 1)
				tmpLog.print(",");
		}
		tmpLog.println(")");

		// print states for each bscc
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			n = sccComputer.getBSCCs().size();
		} else {
			n = sccConsumerExpl.getBSCCs().size();
		}
		for (i = 0; i < n; i++) {
			tmpLog.println();
			if (exportType == EXPORT_MATLAB)
				tmpLog.print("% ");
			tmpLog.println("BSCC " + (i + 1) + "/" + n + ":");
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("bscc" + (i + 1) + "=[");
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				if (exportType != EXPORT_MATLAB)
					new StateListMTBDD(sccComputer.getBSCCs().get(i), currentModel).print(tmpLog);
				else
					new StateListMTBDD(sccComputer.getBSCCs().get(i), currentModel).printMatlab(tmpLog);
				JDD.Deref(sccComputer.getBSCCs().get(i));
			} else {
				explicit.StateValues.createFromBitSet(sccConsumerExpl.getBSCCs().get(i), currentModelExpl).print(tmpLog, true, exportType == EXPORT_MATLAB, true, true);
			}
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("];");
		}

		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			JDD.Deref(sccComputer.getNotInBSCCs());
		}

		// tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the (states of the) currently loaded model's maximal end components (MECs) to a file
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportMECsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		int i, n;
		long l; // timer
		PrismLog tmpLog;
		ECComputer ecComputer = null;
		explicit.ECComputer ecComputerExpl = null;

		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		// Compute MECs
		mainLog.println("\nComputing MECs...");
		l = System.currentTimeMillis();
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			ecComputer = getECComputer((NondetModel) currentModel);
			ecComputer.computeMECStates();
		} else {
			ecComputerExpl = getExplicitECComputer((explicit.NondetModel<?>) currentModelExpl);
			ecComputerExpl.computeMECStates();
		}
		l = System.currentTimeMillis() - l;
		mainLog.println("\nTime for MEC computation: " + l / 1000.0 + " seconds.");

		// print message
		mainLog.print("\nExporting MECs ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(file);

		// print header: list of model vars
		if (exportType == EXPORT_MATLAB)
			tmpLog.print("% ");
		tmpLog.print("Variables: (");
		for (i = 0; i < currentModelInfo.getNumVars(); i++) {
			tmpLog.print(currentModelInfo.getVarName(i));
			if (i < currentModelInfo.getNumVars() - 1)
				tmpLog.print(",");
		}
		tmpLog.println(")");

		// print states for each mec
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			n = ecComputer.getMECStates().size();
		} else {
			n = ecComputerExpl.getMECStates().size();
		}
		for (i = 0; i < n; i++) {
			tmpLog.println();
			if (exportType == EXPORT_MATLAB)
				tmpLog.print("% ");
			tmpLog.println("MEC " + (i + 1) + "/" + n + ":");
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("mec" + (i + 1) + "=[");
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				if (exportType != EXPORT_MATLAB)
					new StateListMTBDD(ecComputer.getMECStates().get(i), currentModel).print(tmpLog);
				else
					new StateListMTBDD(ecComputer.getMECStates().get(i), currentModel).printMatlab(tmpLog);
				JDD.Deref(ecComputer.getMECStates().get(i));
			} else {
				explicit.StateValues.createFromBitSet(ecComputerExpl.getMECStates().get(i), currentModelExpl).print(tmpLog, true, exportType == EXPORT_MATLAB,
						true, true);
			}
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("];");
		}

		// tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the (states of the) currently loaded model's strongly connected components (SCCs) to a file
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportSCCsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		int i, n;
		long l; // timer
		PrismLog tmpLog;
		SCCComputer sccComputer = null;
		explicit.SCCConsumerStore sccConsumerExpl = null;

		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		// Compute SCCs
		mainLog.println("\nComputing SCCs...");
		l = System.currentTimeMillis();
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			sccComputer = getSCCComputer(currentModel);
			sccComputer.computeSCCs();
		} else {
			sccConsumerExpl = new explicit.SCCConsumerStore();
			getExplicitSCCComputer(currentModelExpl, sccConsumerExpl).computeSCCs();
		}
		l = System.currentTimeMillis() - l;
		mainLog.println("\nTime for SCC computation: " + l / 1000.0 + " seconds.");

		// print message
		mainLog.print("\nExporting SCCs ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(file);

		// print header: list of model vars
		if (exportType == EXPORT_MATLAB)
			tmpLog.print("% ");
		tmpLog.print("Variables: (");
		for (i = 0; i < currentModelInfo.getNumVars(); i++) {
			tmpLog.print(currentModelInfo.getVarName(i));
			if (i < currentModelInfo.getNumVars() - 1)
				tmpLog.print(",");
		}
		tmpLog.println(")");

		// print states for each scc
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			n = sccComputer.getSCCs().size();
		} else {
			n = sccConsumerExpl.getSCCs().size();
		}
		for (i = 0; i < n; i++) {
			tmpLog.println();
			if (exportType == EXPORT_MATLAB)
				tmpLog.print("% ");
			tmpLog.println("SCC " + (i + 1) + "/" + n + ":");
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("scc" + (i + 1) + "=[");
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				if (exportType != EXPORT_MATLAB)
					new StateListMTBDD(sccComputer.getSCCs().get(i), currentModel).print(tmpLog);
				else
					new StateListMTBDD(sccComputer.getSCCs().get(i), currentModel).printMatlab(tmpLog);
				JDD.Deref(sccComputer.getSCCs().get(i));
			} else {
				explicit.StateValues.createFromBitSet(sccConsumerExpl.getSCCs().get(i), currentModelExpl).print(tmpLog, true, exportType == EXPORT_MATLAB, true, true);
			}
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("];");
		}

		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			JDD.Deref(sccComputer.getNotInSCCs());
		}

		// tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the states satisfying labels from the properties file to a file.
	 * The PropertiesFile should correspond to the currently loaded model.
	 * @param propertiesFile The properties file (for further labels)
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN}
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportPropLabelsToFile(PropertiesFile propertiesFile, int exportType, File file) throws FileNotFoundException, PrismException
	{
		Objects.requireNonNull(propertiesFile);

		// Collect names of labels to export from properties file
		List<String> labelNames = new ArrayList<String>();
		LabelList ll = propertiesFile.getLabelList();
		new Range(ll.size()).map((int i) -> ll.getLabelName(i)).collect(labelNames);

		doExportLabelsToFile(propertiesFile, exportType, file, labelNames);
	}

	/**
	 * Export the states satisfying labels from the currently loaded model and (optionally) a properties file to a file.
	 * The PropertiesFile should correspond to the currently loaded model. 
	 * @param propertiesFile The properties file, for further labels (ignored if null)
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportLabelsToFile(PropertiesFile propertiesFile, int exportType, File file) throws FileNotFoundException, PrismException
	{
		// Collect names of labels to export from model
		List<String> labelNames = new ArrayList<String>();
		labelNames.add("init");
		labelNames.add("deadlock");
		labelNames.addAll(currentModelInfo.getLabelNames());
		// Collect names of labels to export from properties file
		if (propertiesFile != null) {
			LabelList ll = propertiesFile.getLabelList();
			new Range(ll.size()).map((int i) -> ll.getLabelName(i)).collect(labelNames);
		}

		doExportLabelsToFile(propertiesFile, exportType, file, labelNames);
	}

	/**
	 * Export the states satisfying labels from the currently loaded model and/or a properties file to a file.
	 * The PropertiesFile should correspond to the currently loaded model.
	 * @param propertiesFile The properties file, for further labels (ignored if null)
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN}
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 * @param labelNames The list of label names to export
	 */
	private void doExportLabelsToFile(PropertiesFile propertiesFile, int exportType, File file, List<String> labelNames) throws PrismException, FileNotFoundException
	{
		// Build model, if necessary
		buildModelIfRequired();

		// Print message
		mainLog.print("\nExporting labels and satisfying states ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// Export
		if (currentModelBuildType != ModelBuildType.SYMBOLIC) {
			PrismLog out = getPrismLogForFile(file);
			explicit.StateModelChecker mcExpl = createModelCheckerExplicit(propertiesFile);
			mcExpl.exportLabels(currentModelExpl, labelNames, exportType, out);
			out.close();
		} else {
			StateModelChecker mc = createModelChecker(propertiesFile);
			mc.exportLabels(labelNames, exportType, file);
		}
	}

	/**
	 * Export the currently loaded model's states to a file
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportStatesToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		PrismLog tmpLog;

		// No specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// Rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		// Print message
		mainLog.print("\nExporting list of reachable states ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// Create new file log or use main log
		tmpLog = getPrismLogForFile(file);

		// Export
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			currentModel.exportStates(exportType, tmpLog);
		} else {
			currentModelExpl.exportStates(exportType, currentModelInfo.createVarList(), tmpLog);
		}

		// Tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the observations for the currently loaded model to a file
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN}
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportObservationsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		if (!currentModelType.partiallyObservable()) {
			mainLog.println("\nOmitting observations export as the model is not partially observable");
			return;
		}

		// No specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// Rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		// Print message
		mainLog.print("\nExporting list of observations ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// Create new file log or use main log
		PrismLog tmpLog = getPrismLogForFile(file);

		// Export (explicit engine only)
		((PartiallyObservableModel<?>) currentModelExpl).exportObservations(exportType, currentModelInfo, tmpLog);

		// Tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Perform model checking of a property on the currently loaded model and return result.
	 * Here, the property is passed as a string and parsed first. Usually, you would use the other
	 * model checking methods which assume you have already parsed the property separately.
	 * This is just a simplified method for convenience. The property string can in fact be a whole
	 * properties file, e.g. you can define labels/constants/etc. too, but an exception will be
	 * thrown if there is more than one property present.    
	 * @param propertyString The property (in fact properties file) to check as a string
	 */
	public Result modelCheck(String propertyString) throws PrismException
	{
		PropertiesFile propertiesFile = parsePropertiesString(propertyString);
		if (propertiesFile.getNumProperties() != 1) {
			throw new PrismException("There should be exactly one property to check (there are " + propertiesFile.getNumProperties() + ")");
		}
		return modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
	}

	/**
	 * Perform model checking of a property on the currently loaded model and return result.
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 */
	public Result modelCheck(PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		return modelCheck(propertiesFile, new Property(expr));
	}

	/**
	 * Perform model checking of a property on the currently loaded model and return result.
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param prop The property to check
	 */
	public Result modelCheck(PropertiesFile propertiesFile, Property prop) throws PrismException, PrismLangException
	{
		Result res = null;
		Values definedPFConstants = propertiesFile.getConstantValues();
		boolean engineSwitch = false, switchToMTBDDEngine = false, switchedToExplicitEngine = false;
		int lastEngine = -1;

		if (!digital)
			mainLog.printSeparator();
		mainLog.println("\nModel checking: " + prop);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		if (definedPFConstants != null && definedPFConstants.getNumValues() > 0)
			mainLog.println("Property constants: " + definedPFConstants);

		// Check that property is valid for the current model type
		prop.getExpression().checkValid(currentModelType);

		// Remove old strategy if present
		clearStrategy();
		
		// PTA (and similar) model checking is handled separately
		if (currentModelType.realTime()) {
			return modelCheckPTA(propertiesFile, prop.getExpression(), definedPFConstants);
		}

		// For exact model checking
		if (settings.getBoolean(PrismSettings.PRISM_EXACT_ENABLED)) {
			return modelCheckExact(propertiesFile, prop);
		}
		// For fast adaptive uniformisation
		if (currentModelType == ModelType.CTMC && settings.getString(PrismSettings.PRISM_TRANSIENT_METHOD).equals("Fast adaptive uniformisation")) {
			FastAdaptiveUniformisationModelChecker fauMC;
			fauMC = new FastAdaptiveUniformisationModelChecker(this, currentModulesFile, propertiesFile);
			return fauMC.check(prop.getExpression());
		}
		// Heuristic choices of engine/method
		if (settings.getString(PrismSettings.PRISM_HEURISTIC).equals("Speed")) {
			mainLog.printWarning("Switching to sparse engine and (backwards) Gauss Seidel (default for heuristic=speed).");
			engineSwitch = true;
			lastEngine = getEngine();
			setEngine(Prism.SPARSE);
			settings.set(PrismSettings.PRISM_LIN_EQ_METHOD, "Backwards Gauss-Seidel");

		}
		// Auto-switch to explicit engine if required
		if (currentModelType == ModelType.MDP && !Expression.containsMultiObjective(prop.getExpression())) {
			if (getMDPSolnMethod() != Prism.MDP_VALITER && getCurrentEngine() == PrismEngine.SYMBOLIC) {
				mainLog.printWarning("Switching to explicit engine to allow use of chosen MDP solution method.");
				engineSwitch = true;
				lastEngine = getEngine();
				switchedToExplicitEngine = true;
				setEngine(Prism.EXPLICIT);
			}
		}
		if (Expression.containsNonProbLTLFormula(prop.getExpression()) && getCurrentEngine() == PrismEngine.SYMBOLIC) {
			mainLog.printWarning("Switching to explicit engine to allow non-probabilistic LTL model checking.");
			engineSwitch = true;
			lastEngine = getEngine();
			switchedToExplicitEngine = true;
			setEngine(Prism.EXPLICIT);
		}
		if (settings.getBoolean(PrismSettings.PRISM_INTERVAL_ITER) && getCurrentEngine() == PrismEngine.SYMBOLIC) {
			if (currentModelType == ModelType.MDP && Expression.containsMinReward(prop.getExpression())) {
				mainLog.printWarning("Switching to explicit engine to allow interval iteration on Rmin operator.");
				engineSwitch = true;
				lastEngine = getEngine();
				switchedToExplicitEngine = true;
				setEngine(Prism.EXPLICIT);
			}
		}
		if ((currentModelType == ModelType.IDTMC || currentModelType == ModelType.IMDP) && getCurrentEngine() == PrismEngine.SYMBOLIC) {
			mainLog.printWarning("Switching to explicit engine to allow model checking of interval model.");
			engineSwitch = true;
			lastEngine = getEngine();
			switchedToExplicitEngine = true;
			setEngine(Prism.EXPLICIT);
		}
		if (genStrat && getCurrentEngine() == PrismEngine.SYMBOLIC) {
			mainLog.printWarning("Switching to explicit engine to allow strategy generation.");
			engineSwitch = true;
			lastEngine = getEngine();
			switchedToExplicitEngine = true;
			setEngine(Prism.EXPLICIT);
		}
		try {
			// Build model, if necessary
			buildModelIfRequired();

			// Compatibility check
			if (genStrat && currentModelType.nondeterministic() && getCurrentEngine() == PrismEngine.SYMBOLIC) {
				if (!((NondetModel) currentModel).areAllChoiceActionsUnique())
					throw new PrismException("Cannot generate strategies with the current engine "
							+ "because some state of the model do not have unique action labels for each choice. "
							+ "Either switch to the explicit engine or add more action labels to the model");
			}

			// Check if we need to switch to MTBDD engine
			if (getCurrentEngine() == PrismEngine.SYMBOLIC && getEngine() != MTBDD && !switchedToExplicitEngine) {
				long n = currentModel.getNumStates();
				// Either because number of states is two big for double-valued solution vectors
				if (n == -1 || n > Integer.MAX_VALUE) {
					mainLog.printWarning("Switching to MTBDD engine, as number of states is too large for " + engineStrings[getEngine()] + " engine.");
					switchToMTBDDEngine = true;
				}
				// Or based on heuristic choices of engine/method
				// (sparse/hybrid typically v slow if need to work with huge state spaces)
				else if (settings.getString(PrismSettings.PRISM_HEURISTIC).equals("Speed") && n > MTBDD_STATES_THRESHOLD) {
					mainLog.printWarning("Switching to MTBDD engine (default for heuristic=speed and this state space size).");
					switchToMTBDDEngine = true;
				}
				// NB: Need to make sure solution methods supported for MTBDDs are used
				if (switchToMTBDDEngine) {
					engineSwitch = true;
					lastEngine = getEngine();
					setEngine(Prism.MTBDD);
					settings.set(PrismSettings.PRISM_LIN_EQ_METHOD, "Jacobi");
				}
			}

			// Create new model checker object and do model checking
			if (getCurrentEngine() == PrismEngine.SYMBOLIC) {
				ModelChecker mc = createModelChecker(propertiesFile);
				res = mc.check(prop.getExpression());
			} else {
				explicit.StateModelChecker mc = createModelCheckerExplicit(propertiesFile);
				res = mc.check(currentModelExpl, prop.getExpression());
			}
			
			// If model checking generated a strategy, store it
			if (res.getStrategy() != null) {
				strategy = res.getStrategy();
			}
		} finally {
			// Undo auto-switch (if any)
			if (engineSwitch) {
				setEngine(lastEngine);
			}
		}

		// Return result
		return res;
	}

	/**
	 * Perform model checking of a property on the currently loaded PTA PRISM model and return result.
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 * @param definedPFConstants Optional values info for properties file (to display in log) 
	 */
	public Result modelCheckPTA(PropertiesFile propertiesFile, Expression expr, Values definedPFConstants) throws PrismException, PrismLangException
	{
		// Check that property is valid for this model type
		// and create new model checker object
		expr.checkValid(currentModelType);

		// Remove old strategy if present
		clearStrategy();
		
		// Digital clocks translation
		if (settings.getString(PrismSettings.PRISM_PTA_METHOD).equals("Digital clocks") || currentModelType == ModelType.POPTA) {
			digital = true;
			ModulesFile oldModulesFile = currentModulesFile;
			try {
				DigitalClocks dc = new DigitalClocks(this);
				dc.translate(oldModulesFile, propertiesFile, expr);
				loadPRISMModel(dc.getNewModulesFile());
				// If required, export generated PRISM model
				if (exportDigital) {
					try {
						exportPRISMModel(exportDigitalFile);
					}
					// In case of error, just print a warning
					catch (FileNotFoundException e) {
						mainLog.printWarning("PRISM code export failed: Couldn't open file \"" + exportDigitalFile + "\" for output");
					} catch (PrismException e) {
						mainLog.printWarning("PRISM code export failed: " + e.getMessage());
					}
				}
				return modelCheck(dc.getNewPropertiesFile(), dc.getNewPropertyToCheck());
			} finally {
				digital = false;
				// Preserve strategy (setting to null stops it being cleared with model)
				Strategy strategyDigital = strategy;
				strategy = null;
				// Restore/clear model info
				currentModulesFile = oldModulesFile;
				currentModelType = oldModulesFile.getModelType();
				clearBuiltModel();
				// Restore strategy
				strategy = strategyDigital;
			}
		}
		// Other methods
		else {
			PTAModelChecker mcPta;
			mcPta = new PTAModelChecker(this, currentModulesFile, propertiesFile);
			return mcPta.check(expr);
		}
	}

	/**
	 * Check if the currently loaded model is suitable for analysis with the simulator.
	 * If not, an explanatory exception is thrown.
	 */
	public void checkModelForSimulation() throws PrismException
	{
		// Get (but ignore) the ModelGenerator.
		// If creation failed before, this tries again, throwing an explanatory exception.
		try {
			getModelGenerator();
			// No support for real-time models yet
			if (currentModelType.realTime()) {
				throw new PrismException(currentModelType + "s are not currently supported");
			}
		} catch (PrismException e) {
			throw e.prepend("Simulation not possible: ");
		}
	}

	/**
	 * Load the current model into the simulator.
	 */
	public void loadModelIntoSimulator() throws PrismException
	{
		// Get the ModelGenerator.
		// If creation failed before, this tries again, throwing an explanatory exception.
		ModelGenerator<?> modelGenForSim = null;
		try {
			modelGenForSim = getModelGenerator();
		} catch (PrismException e) {
			throw e.prepend("Simulation not possible: ");
		}
		// Load into simulator
		getSimulator().loadModel((ModelGenerator<Double>) modelGenForSim, (RewardGenerator<Double>) currentRewardGenerator);
	}

	/**
	 * Load the current strategy (if any) into the simulator.
	 * Does nothing if there is no strategy to load, or is not able to be simulated.
	 */
	public void loadStrategyIntoSimulator() throws PrismException
	{
		if (getStrategy() != null && getStrategy() instanceof StrategyGenerator) {
			getSimulator().loadStrategy((StrategyGenerator) getStrategy());
		}
	}

	/**
	 * Check whether a property is suitable for approximate model checking using the simulator.
	 * @param expr The property to check.
	 */
	public boolean isPropertyOKForSimulation(Expression expr)
	{
		return getSimulator().isPropertyOKForSimulation(expr);
	}

	/**
	 * Check if a property is suitable for analysis with the simulator.
	 * If not, an explanatory exception is thrown.
	 * @param expr The property to check.
	 */
	public void checkPropertyForSimulation(Expression expr) throws PrismException
	{
		getSimulator().checkPropertyForSimulation(expr);
	}

	/**
	 * Perform approximate model checking of a property on the currently loaded model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Returns a Result object, except in case of error, where an Exception is thrown.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 * @param definedPFConstants Optional values info for properties file (to display in log) 
	 * @param initialState Initial state (if null, use default, selecting randomly if needed)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 */
	public Result modelCheckSimulator(PropertiesFile propertiesFile, Expression expr, Values definedPFConstants, State initialState, long maxPathLength,
			SimulationMethod simMethod) throws PrismException
	{
		// Print info
		mainLog.printSeparator();
		mainLog.println("\nSimulating: " + expr);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		if (definedPFConstants != null && definedPFConstants.getNumValues() > 0)
			mainLog.println("Property constants: " + definedPFConstants);

		if (currentModelType.nondeterministic() && currentModelType.removeNondeterminism() != currentModelType) {
			mainLog.printWarning("For simulation, nondeterminism in " + currentModelType + " is resolved uniformly (resulting in " + currentModelType.removeNondeterminism() + ").");
		}

		// Check that property is valid for this model type
		expr.checkValid(currentModelType.removeNondeterminism());

		// Remove old strategy if present
		clearStrategy();
		
		// Do simulation
		loadModelIntoSimulator();
		Result res = getSimulator().modelCheckSingleProperty(propertiesFile, expr, initialState, maxPathLength, simMethod);

		return res;
	}

	/**
	 * Perform approximate model checking of several properties (simultaneously) on the currently loaded model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Returns an array of results, some of which may be Exception objects if there were errors.
	 * In the case of an error which affects all properties, an exception is thrown.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param exprs The properties to check
	 * @param definedPFConstants Optional values info for properties file (to display in log) 
	 * @param initialState Initial state (if null, use default, selecting randomly if needed)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 */
	public Result[] modelCheckSimulatorSimultaneously(PropertiesFile propertiesFile, List<Expression> exprs, Values definedPFConstants, State initialState,
			long maxPathLength, SimulationMethod simMethod) throws PrismException
	{
		// Print info
		mainLog.printSeparator();
		mainLog.print("\nSimulating");
		if (exprs.size() == 1) {
			mainLog.println(": " + exprs.get(0));
		} else {
			mainLog.println(" " + exprs.size() + " properties:");
			for (int i = 0; i < exprs.size(); i++) {
				mainLog.println(" " + exprs.get(i));
			}
		}
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		if (definedPFConstants != null && definedPFConstants.getNumValues() > 0)
			mainLog.println("Property constants: " + definedPFConstants);

		if (currentModelType.nondeterministic() && currentModelType.removeNondeterminism() != currentModelType) {
			mainLog.printWarning("For simulation, nondeterminism in " + currentModelType + " is resolved uniformly (resulting in " + currentModelType.removeNondeterminism() + ").");
		}

		// Check that properties are valid for this model type
		for (Expression expr : exprs)
			expr.checkValid(currentModelType.removeNondeterminism());

		// Remove old strategy if present
		clearStrategy();
		
		// Do simulation
		loadModelIntoSimulator();
		Result[] resArray = getSimulator().modelCheckMultipleProperties(propertiesFile, exprs, initialState, maxPathLength, simMethod);

		return resArray;
	}

	/**
	 * Perform an approximate model checking experiment on the currently loaded model, using the simulator.
	 * (specified by values for undefined constants from the property only).
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Results are stored in the ResultsCollection object passed in,
	 * some of which may be Exception objects if there were errors.
	 * In the case of an error which affects all properties, an exception is thrown.
	 * Note: All constants in the model file must have already been defined.
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param undefinedConstants Details of constant ranges defining the experiment
	 * @param results Where to store the results
	 * @param expr The property to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 * @throws PrismException if something goes wrong with the sampling algorithm
	 * @throws InterruptedException if the thread is interrupted
	 */
	public void modelCheckSimulatorExperiment(PropertiesFile propertiesFile, UndefinedConstants undefinedConstants, ResultsCollection results, Expression expr,
			State initialState, long maxPathLength, SimulationMethod simMethod) throws PrismException, InterruptedException
	{
		// Print info
		mainLog.printSeparator();
		mainLog.println("\nSimulating: " + expr);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		mainLog.println("Property constants: " + undefinedConstants.getPFDefinedConstantsString());

		// Remove old strategy if present
		clearStrategy();
		
		// Do simulation
		loadModelIntoSimulator();
		getSimulator().modelCheckExperiment(propertiesFile, undefinedConstants, results, expr, initialState, maxPathLength, simMethod);
	}

	/**
	 * Perform model checking on the currently loaded model using exact methods
	 * (currently, this is done via the parametric model checking functionality)
	 * @param propertiesFile parent properties file
	 * @param prop property to model check
	 */
	public Result modelCheckExact(PropertiesFile propertiesFile, Property prop) throws PrismException
	{
		// Some checks
		if (!(currentModelType == ModelType.DTMC || currentModelType == ModelType.CTMC || currentModelType == ModelType.MDP))
			throw new PrismNotSupportedException("Exact model checking is only supported for DTMCs, CTMCs and MDPs");

		if (currentModelType == ModelType.MDP && getFairness())
			throw new PrismNotSupportedException("Exact model checking does not support checking MDPs under fairness");

		// Remove old strategy if present
		clearStrategy();
		
		// Set up a dummy parameter (not used)
		String[] paramNames = new String[] { "dummy" };
		String[] paramLowerBounds = new String[] { "0" };
		String[] paramUpperBounds = new String[] { "1" };
		// And execute parameteric model checking
		param.ModelBuilder builder = new ModelBuilder(this, param.ParamMode.EXACT);
		FunctionFactory functionFactory = builder.getFunctionFactory(paramNames, paramLowerBounds, paramUpperBounds);
		ParamModel modelExpl = builder.constructModel(ModulesFileModelGenerator.createForRationalFunctions(currentModulesFile, functionFactory, this));
		ParamModelChecker mc = new ParamModelChecker(this, param.ParamMode.EXACT);
		mc.setParameters(paramNames, paramLowerBounds, paramUpperBounds);
		mc.setModulesFileAndPropertiesFile(currentModulesFile, propertiesFile);

		if (digital) {
			// have to do deadlock checks, as we are in digital clock mode for PTA checking,
			// cf. doBuildModelDigitalClocksChecks()
			if (modelExpl.getNumDeadlockStates() > 0) {
				int dl = modelExpl.getFirstDeadlockState();
				String dls = modelExpl.getStatesList().get(dl).toString(currentModelInfo);
				throw new PrismException("Timelock in PTA, e.g. in state " + dls);
			}
		}

		Result result = mc.check(modelExpl, prop.getExpression());

		// Convert result of parametric model checking to a single value,
		// either boolean for boolean properties or a rational for numeric properties
		// There should be just one region since no parameters are used
		ParamResult paramResult = (ParamResult) result.getResult();
		result.setResult(paramResult.getSimpleResult(prop.getType()));
		result.setAccuracy(new Accuracy(AccuracyLevel.EXACT));

		// Print result to log
		String resultString = "Result";
		resultString += ": " + result.getResultAndAccuracy();
		if (result.getResult() instanceof BigRational) {
			resultString += " (" + ((BigRational) result.getResult()).toApproximateString() + ")";
		}
		mainLog.println("\n" + resultString);

		return result;
	}

	/**
	 * Perform parametric model checking on the currently loaded model.
	 * @param propertiesFile parent properties file
	 * @param prop property to model check
	 * @param paramNames parameter names
	 * @param paramLowerBounds lower bounds of parameters
	 * @param paramUpperBounds upper bounds of parameters
	 */
	public Result modelCheckParametric(PropertiesFile propertiesFile, Property prop, String[] paramNames, String[] paramLowerBounds, String[] paramUpperBounds)
			throws PrismException
	{
		// Some checks
		if (paramNames == null) {
			throw new PrismException("Must specify some parameters when using " + "the parametric analysis");
		}
		if (!(currentModelType == ModelType.DTMC || currentModelType == ModelType.CTMC || currentModelType == ModelType.MDP))
			throw new PrismNotSupportedException("Parametric model checking is only supported for DTMCs, CTMCs and MDPs");

		if (currentModelType == ModelType.MDP && getFairness())
			throw new PrismNotSupportedException("Parametric model checking does not support checking MDPs under fairness");

		Values definedPFConstants = propertiesFile.getConstantValues();
		Values constlist = currentModulesFile.getConstantValues();
		for (int pnr = 0; pnr < paramNames.length; pnr++) {
			constlist.removeValue(paramNames[pnr]);
		}

		// Print info
		mainLog.printSeparator();
		mainLog.println("\nParametric model checking: " + prop);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		if (definedPFConstants != null && definedPFConstants.getNumValues() > 0)
			mainLog.println("Property constants: " + definedPFConstants);

		// Remove old strategy if present
		clearStrategy();
		
		param.ModelBuilder builder = new ModelBuilder(this, param.ParamMode.PARAMETRIC);
		FunctionFactory functionFactory = builder.getFunctionFactory(paramNames, paramLowerBounds, paramUpperBounds);
		ParamModel modelExpl = builder.constructModel(ModulesFileModelGenerator.createForRationalFunctions(currentModulesFile, functionFactory, this));
		ParamModelChecker mc = new ParamModelChecker(this, param.ParamMode.PARAMETRIC);
		mc.setParameters(paramNames, paramLowerBounds, paramUpperBounds);
		mc.setModulesFileAndPropertiesFile(currentModulesFile, propertiesFile);
		Result result = mc.check(modelExpl, prop.getExpression());

		// Print result to log
		String resultString = "Result";
		if (!("Result".equals(prop.getExpression().getResultName())))
			resultString += " (" + prop.getExpression().getResultName().toLowerCase() + ")";
		resultString += ": " + result.getResultString();
		mainLog.print("\n" + resultString);

		return result;
	}

	/**
	 * Export the current strategy. The associated model should be attached to the strategy.
	 * Strictly, speaking that does not need to be the currently loaded model,
	 * but it would probably have been discarded if that was not the case.
	 * @param exportOptions The options for export
	 * @param file File to output the path to (stdout if null)
	 */
	public void exportStrategy(StrategyExportOptions exportOptions, File file) throws FileNotFoundException, PrismException
	{
		if (getStrategy() != null) {
			exportStrategy(getStrategy(), exportOptions, file);
		} else {
			throw new PrismException("There is no current strategy to export");
		}
	}

	/**
	 * Export the current strategy. The associated model should be attached to the strategy.
	 * Strictly, speaking that does not need to be the currently loaded model,
	 * but it would probably have been discarded if that was not the case.
	 * @param exportType The type of export
	 * @param file File to output the path to (stdout if null)
	 */
	public void exportStrategy(StrategyExportType exportType, File file) throws FileNotFoundException, PrismException
	{
		exportStrategy(new StrategyExportOptions(exportType), file);
	}

	/**
	 * Export a strategy. The associated model should be attached to the strategy.
	 * Strictly, speaking that does not need to be the currently loaded model,
	 * but it would probably have been discarded if that was not the case.
	 * @param strat The strategy
	 * @param exportOptions The options for export
	 * @param file File to output the path to (stdout if null)
	 */
	public void exportStrategy(Strategy<?> strat, StrategyExportOptions exportOptions, File file) throws FileNotFoundException, PrismException
	{
		// Print message
		mainLog.print("\nExporting strategy " + exportOptions.description() + " ");
		mainLog.println(getDestinationStringForFile(file));

		// Export to file (or use main log)
		PrismLog tmpLog = getPrismLogForFile(file);
		exportOptions = exportOptions.clone();
		exportOptions.setModelPrecision(settings.getInteger(PrismSettings.PRISM_EXPORT_MODEL_PRECISION));
		strat.export(tmpLog, exportOptions);
		if (file != null) {
			tmpLog.close();
		}
	}

	/**
	 * Export a strategy. The associated model should be attached to the strategy.
	 * Strictly, speaking that does not need to be the currently loaded model,
	 * but it would probably have been discarded if that was not the case.
	 * @param strat The strategy
	 * @param exportType The type of export
	 * @param file File to output the path to (stdout if null)
	 */
	public void exportStrategy(Strategy<?> strat, StrategyExportType exportType, File file) throws FileNotFoundException, PrismException
	{
		exportStrategy(strat, new StrategyExportOptions(exportType), file);
	}

	/**
	 * Generate a random path through the currently loaded model using the simulator.
	 * @param details Information about the path to be generated
	 * @param maxPathLength The maximum length of path to generate
	 * @param file File to output the path to (stdout if null)
	 */
	public void generateSimulationPath(String details, long maxPathLength, File file) throws PrismException, PrismLangException
	{
		loadModelIntoSimulator();
		GenerateSimulationPath genPath = new GenerateSimulationPath(getSimulator(), mainLog);
		genPath.generateSimulationPath(null, details, maxPathLength, file);
	}

	/**
	 * Compute steady-state probabilities for the current model (DTMCs/CTMCs only).
	 * Output probability distribution to log. 
	 */
	public void doSteadyState() throws PrismException
	{
		doSteadyState(EXPORT_PLAIN, null, null);
	}

	/**
	 * Compute steady-state probabilities for the current model (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if {@code fileOut} is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doSteadyState(int exportType, File fileOut, File fileIn) throws PrismException
	{
		long l = 0; // timer
		StateValues probs = null;
		explicit.StateValues probsExpl = null;
		PrismLog tmpLog;

		// Do some checks
		if (!(currentModelType == ModelType.CTMC || currentModelType == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN; // no specific states format for MRMC
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN; // rows format does not apply to states output

		// Print message
		mainLog.printSeparator();
		mainLog.println("\nComputing steady-state probabilities...");

		// Build model, if necessary
		buildModelIfRequired();

		l = System.currentTimeMillis();
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			probs = computeSteadyStateProbabilities(currentModel, fileIn);
		} else {
			probsExpl = computeSteadyStateProbabilitiesExplicit(currentModelExpl, fileIn);
		}
		l = System.currentTimeMillis() - l;

		// print message
		mainLog.print("\nPrinting steady-state probabilities ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(fileOut));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(fileOut);

		// print out or export probabilities
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			probs.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null, fileOut == null);
		} else {
			probsExpl.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null, fileOut == null);
		}

		// print out computation time
		mainLog.println("\nTime for steady-state probability computation: " + l / 1000.0 + " seconds.");

		// tidy up
		if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
			probs.clear();
		} else {
			probsExpl.clear();
		}
		if (fileOut != null)
			tmpLog.close();
	}

	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC) using symbolic engines.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	protected StateValues computeSteadyStateProbabilities(Model model, File fileIn) throws PrismException
	{
		ProbModelChecker mc;
		if (model.getModelType() == ModelType.DTMC) {
			mc = new ProbModelChecker(this, model, null);
		} else if (model.getModelType() == ModelType.CTMC) {
			mc = new StochModelChecker(this, model, null);
		} else {
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		}
		return mc.doSteadyState(fileIn);
	}

	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC) using the explicit engine.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	protected explicit.StateValues computeSteadyStateProbabilitiesExplicit(explicit.Model<?> model, File fileIn) throws PrismException
	{
		explicit.StateValues probs;
		switch (model.getModelType()) {
		case DTMC: {
			DTMCModelChecker mcDTMC = new DTMCModelChecker(this);
			probs = mcDTMC.doSteadyState((DTMC<Double>) model, fileIn);
			break;
		}
		case CTMC: {
			CTMCModelChecker mcCTMC = new CTMCModelChecker(this);
			probs = mcCTMC.doSteadyState((CTMC<Double>) model, fileIn);
			break;
		}
		default:
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		}
		return probs;
	}

	/**
	 * Compute transient probabilities (forwards) for the current model (DTMCs/CTMCs only).
	 * Output probability distribution to log.
	 * For a discrete-time model, {@code time} will be cast to an integer.
	 */
	public void doTransient(double time) throws PrismException
	{
		doTransient(time, EXPORT_PLAIN, null, null);
	}

	/**
	 * Compute transient probabilities (forwards) for the current model (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if {@code fileOut} is null, to log). 
	 * For a discrete-time model, {@code time} will be cast to an integer.
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doTransient(double time, int exportType, File fileOut, File fileIn) throws PrismException
	{
		long l = 0; // timer
		ModelChecker mc = null;
		StateValues probs = null;
		explicit.StateValues probsExpl = null;
		PrismLog tmpLog;

		// Do some checks
		if (!(currentModelType == ModelType.CTMC || currentModelType == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		if (time < 0)
			throw new PrismException("Cannot compute transient probabilities for negative time value");
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN; // no specific states format for MRMC
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN; // rows format does not apply to states output

		// Print message
		mainLog.printSeparator();
		String strTime = currentModelType.continuousTime() ? Double.toString(time) : Integer.toString((int) time);
		mainLog.println("\nComputing transient probabilities (time = " + strTime + ")...");

		l = System.currentTimeMillis();

		// FAU
		if (currentModelType == ModelType.CTMC && settings.getString(PrismSettings.PRISM_TRANSIENT_METHOD).equals("Fast adaptive uniformisation")) {
			if (fileIn != null) {
				throw new PrismException("Fast adaptive uniformisation cannot read an initial distribution from a file");
			}
			ModulesFileModelGenerator<Double> prismModelGen = ModulesFileModelGenerator.createForDoubles(currentModulesFile, this);
			FastAdaptiveUniformisation fau = new FastAdaptiveUniformisation(this, prismModelGen);
			fau.setConstantValues(currentModulesFile.getConstantValues());
			probsExpl = fau.doTransient(time);
		}
		// Non-FAU
		else {
			// Build model, if necessary
			buildModelIfRequired();
			// Symbolic
			if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
				if (currentModelType == ModelType.DTMC) {
					mc = new ProbModelChecker(this, currentModel, null);
					probs = ((ProbModelChecker) mc).doTransient((int) time, fileIn);
				} else {
					mc = new StochModelChecker(this, currentModel, null);
					probs = ((StochModelChecker) mc).doTransient(time, fileIn);
				}
			}
			// Explicit
			else {
				if (currentModelType == ModelType.DTMC) {
					DTMCModelChecker mcDTMC = new DTMCModelChecker(this);
					probsExpl = mcDTMC.doTransient((DTMC<Double>) currentModelExpl, (int) time, fileIn);
				} else if (currentModelType == ModelType.CTMC) {
					CTMCModelChecker mcCTMC = new CTMCModelChecker(this);
					probsExpl = mcCTMC.doTransient((CTMC<Double>) currentModelExpl, time, fileIn);
				} else {
					throw new PrismException("Transient probabilities only computed for DTMCs/CTMCs");
				}
			}
		}

		l = System.currentTimeMillis() - l;

		// print message
		mainLog.print("\nPrinting transient probabilities ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(fileOut));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(fileOut);

		// print out or export probabilities
		if (probs != null)
			probs.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null, fileOut == null);
		else
			probsExpl.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null, fileOut == null);

		// print out computation time
		mainLog.println("\nTime for transient probability computation: " + l / 1000.0 + " seconds.");

		// tidy up
		if (probs != null)
			probs.clear();
		if (probsExpl != null)
			probsExpl.clear();
		if (fileOut != null)
			tmpLog.close();
	}

	/**
	 * Compute transient probabilities (forwards) for the current model (DTMCs/CTMCs only)
	 * for a range of time points. Each distribution is computed incrementally.
	 * Output probability distribution to a file (or, if file is null, to log).
	 * Time points are specified using an UndefinedConstants with a single ranging variable  
	 * (of the appropriate type (int/double) and with arbitrary name).
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doTransient(UndefinedConstants times, int exportType, File fileOut, File fileIn) throws PrismException
	{
		int i, timeInt = 0, initTimeInt = 0;
		double timeDouble = 0, initTimeDouble = 0;
		Object time;
		long l = 0; // timer
		StateValues probs = null, initDist = null;
		explicit.StateValues probsExpl = null, initDistExpl = null;
		PrismLog tmpLog = null;
		File fileOutActual = null;

		// Do some checks
		if (!(currentModelType == ModelType.CTMC || currentModelType == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN; // no specific states format for MRMC
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN; // rows format does not apply to states output

		// Step through required time points
		for (i = 0; i < times.getNumPropertyIterations(); i++) {

			// Get time, check non-negative
			time = times.getPFConstantValues().getValue(0);
			if (currentModelType.continuousTime())
				timeDouble = ((Double) time).doubleValue();
			else
				timeInt = ((Integer) time).intValue();
			if (currentModelType.continuousTime() ? (((Double) time).doubleValue() < 0) : (((Integer) time).intValue() < 0))
				throw new PrismException("Cannot compute transient probabilities for negative time value");

			// Print message
			mainLog.printSeparator();
			mainLog.println("\nComputing transient probabilities (time = " + time + ")...");

			l = System.currentTimeMillis();

			// FAU
			if (currentModelType == ModelType.CTMC && settings.getString(PrismSettings.PRISM_TRANSIENT_METHOD).equals("Fast adaptive uniformisation")) {
				if (fileIn != null) {
					throw new PrismException("Fast adaptive uniformisation cannot read an initial distribution from a file");
				}
				ModulesFileModelGenerator<Double> prismModelGen = ModulesFileModelGenerator.createForDoubles(currentModulesFile, this);
				FastAdaptiveUniformisation fau = new FastAdaptiveUniformisation(this, prismModelGen);
				fau.setConstantValues(currentModulesFile.getConstantValues());
				if (i == 0) {
					probsExpl = fau.doTransient(timeDouble);
					initTimeDouble = 0.0;
				} else {
					probsExpl = fau.doTransient(timeDouble - initTimeDouble, probsExpl);
				}
			}
			// Non-FAU
			else {
				// Build model, if necessary
				buildModelIfRequired();
				// Symbolic
				if (currentModelBuildType == ModelBuildType.SYMBOLIC) {
					if (currentModelType.continuousTime()) {
						StochModelChecker mc = new StochModelChecker(this, currentModel, null);
						if (i == 0) {
							initDist = mc.readDistributionFromFile(fileIn);
							initTimeDouble = 0;
						}
						probs = ((StochModelChecker) mc).doTransient(timeDouble - initTimeDouble, initDist);
					} else {
						ProbModelChecker mc = new ProbModelChecker(this, currentModel, null);
						if (i == 0) {
							initDist = mc.readDistributionFromFile(fileIn);
							initTimeInt = 0;
						}
						probs = ((ProbModelChecker) mc).doTransient(timeInt - initTimeInt, initDist);
					}
				}
				// Explicit
				else {
					if (currentModelType.continuousTime()) {
						CTMCModelChecker mc = new CTMCModelChecker(this);
						if (i == 0) {
							initDistExpl = mc.readDistributionFromFile(fileIn, currentModelExpl);
							initTimeDouble = 0;
						}
						probsExpl = mc.doTransient((CTMC<Double>) currentModelExpl, timeDouble - initTimeDouble, initDistExpl);
					} else {
						DTMCModelChecker mc = new DTMCModelChecker(this);
						if (i == 0) {
							initDistExpl = mc.readDistributionFromFile(fileIn, currentModelExpl);
							initTimeInt = 0;
						}
						probsExpl = mc.doTransient((DTMC<Double>) currentModelExpl, timeInt - initTimeInt, initDistExpl);
					}
				}
			}

			l = System.currentTimeMillis() - l;

			// If output is to a file and there are multiple points, change filename
			if (fileOut != null && times.getNumPropertyIterations() > 1) {
				fileOutActual = new File(PrismUtils.addSuffixToFilename(fileOut.getPath(), time.toString()));
			} else {
				fileOutActual = fileOut;
			}

			// print message
			mainLog.print("\nPrinting transient probabilities ");
			mainLog.print(getStringForExportType(exportType) + " ");
			mainLog.println(getDestinationStringForFile(fileOutActual));

			// create new file log or use main log
			tmpLog = getPrismLogForFile(fileOutActual);

			// print out or export probabilities
			if (probs != null)
				probs.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null);
			else if (!settings.getString(PrismSettings.PRISM_TRANSIENT_METHOD).equals("Fast adaptive uniformisation")) {
				probsExpl.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null, true);
			} else {
				// If full state space not computed, don't print vectors and always show states
				probsExpl.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, true, false);
			}

			// print out computation time
			mainLog.println("\nTime for transient probability computation: " + l / 1000.0 + " seconds.");

			// Prepare for next iteration
			initDist = probs;
			initDistExpl = probsExpl;
			initTimeInt = timeInt;
			initTimeDouble = timeDouble;
			times.iterateProperty();
		}

		// tidy up
		if (probs != null)
			probs.clear();
		if (probsExpl != null)
			probsExpl.clear();
		if (fileOut != null)
			tmpLog.close();
	}

	public void explicitBuildTest() throws PrismException
	{
		/* old code...
		String tmpFile = "";
		try {
			explicit.ConstructModel constructModel = new explicit.ConstructModel(getSimulator(), mainLog);
			mainLog.println("\nConstructing model explicitly...");
			explicit.Model modelExplicit = constructModel.constructModel(currentModulesFile);
			tmpFile = File.createTempFile("explicitbuildtest", ".tra").getAbsolutePath();
			tmpFile = "explicitbuildtest.tra";
			mainLog.println("\nExporting (explicit) model to \"" + tmpFile + "1\"...");
			modelExplicit.exportToPrismExplicitTra(tmpFile + "1");
			mainLog.println("\nExporting (normal) model to \"" + tmpFile + "2\"...");
			exportTransToFile(true, Prism.EXPORT_PLAIN, new File(tmpFile + "2"));
			explicit.ModelSimple modelExplicit2 = null;
			switch (currentModelType) {
			case DTMC:
				modelExplicit2 = new explicit.DTMCSimple();
				break;
			case CTMC:
				modelExplicit2 = new explicit.CTMCSimple();
				break;
			case MDP:
				modelExplicit2 = new explicit.MDPSimple();
				break;
			}
			modelExplicit2.buildFromPrismExplicit(tmpFile + "2");
			if (!modelExplicit.equals(modelExplicit2)) {
				throw new PrismException("Explicit models differ");
			}
		} catch (IOException e) {
			throw new PrismException("Could not create temporary file \"" + tmpFile + "\"");
		}*/
	}

	/**
	 * Clear the built model if needed (free/deallocate memory etc).
	 * This resets {@code currentModel} and {@code currentModelExpl} to {@code null}.
	 * Also clear objects that connect to a build model, notably strategies.
	 */
	private void clearBuiltModel()
	{
		if (currentModel != null) {
			currentModel.clear();
			currentModel = null;
		}
		currentModelExpl = null;
		currentModelBuildType = null;
		clearStrategy();
	}

	/**
	 * Clear the currently stored strategy if present (free/deallocate memory etc).
	 * This resets {@code strategy} to {@code null}.
	 */
	private void clearStrategy()
	{
		if (strategy != null) {
			strategy.clear();
			strategy = null;
		}
	}
	
	/**
	 * Clear up and close down.
	 */
	public void closeDown()
	{
		closeDown(true);
	}

	/**
	 * Clear up and close down.
	 * @param check Whether to perform checks on CUDD status when shutting it down. 
	 */
	public void closeDown(boolean check)
	{
		// Clear any built model(s)
		clearBuiltModel();
		// Close down libraries/engines
		PrismNative.closeDown();
		PrismMTBDD.closeDown();
		PrismSparse.closeDown();
		PrismHybrid.closeDown();
		ParamModelChecker.closeDown();
		// Close down CUDD/JDD
		if (cuddStarted) {
			JDD.CloseDownCUDD(check);
			// reset CUDD status
			cuddStarted = false;
		}
	}

	/**
	 * Utility method to create and initialise a (symbolic) model checker based on the current model.
	 * @param propertiesFile Optional properties file for extra info needed during model checking (can be null)
	 */
	private StateModelChecker createModelChecker(PropertiesFile propertiesFile) throws PrismException
	{
		// Create a dummy properties file if none exist
		// (the symbolic model checkers rely on this to store e.g. model labels)
		if (propertiesFile == null) {
			propertiesFile = parsePropertiesString("");
		}
		// Create model checker
		StateModelChecker mc = StateModelChecker.createModelChecker(currentModelType, this, currentModel, propertiesFile);
		// Pass any additional local settings
		// TODO

		return mc;
	}

	/**
	 * Utility method to create and initialise an (explicit) model checker based on the current model.
	 * @param propertiesFile Optional properties file for extra info needed during model checking (can be null)
	 */
	private explicit.StateModelChecker createModelCheckerExplicit(PropertiesFile propertiesFile) throws PrismException
	{
		// Create model checker
		explicit.StateModelChecker mc = explicit.StateModelChecker.createModelChecker(currentModelType, this);
		mc.setModelCheckingInfo(currentModelInfo, propertiesFile, currentRewardGenerator);
		// Pass any additional local settings
		mc.setExportTarget(exportTarget);
		mc.setExportTargetFilename(exportTargetFilename);
		mc.setExportProductTrans(exportProductTrans);
		mc.setExportProductTransFilename(exportProductTransFilename);
		mc.setExportProductStates(exportProductStates);
		mc.setExportProductStatesFilename(exportProductStatesFilename);
		mc.setExportProductVector(exportProductVector);
		mc.setExportProductVectorFilename(exportProductVectorFilename);
		mc.setStoreVector(storeVector);
		mc.setGenStrat(genStrat);
		mc.setRestrictStratToReach(restrictStratToReach);
		mc.setDoBisim(doBisim);

		return mc;
	}

	/**
	 * Either create a new PrismFileLog for {@code file} or,
	 * if {@code file} is null, return {@code mainLog}.
	 * Throws a {@code PrismException} if there is a problem opening the file.
	 */
	private PrismLog getPrismLogForFile(File file) throws PrismException
	{
		return getPrismLogForFile(file, false);
	}

	/**
	 * Either create a new PrismFileLog for {@code file} or,
	 * if {@code file} is null, return {@code mainLog}.
	 * Throws a {@code PrismException} if there is a problem opening the file.
	 * If {@code append} is true, file should be opened in "append" mode.
	 */
	private PrismLog getPrismLogForFile(File file, boolean append) throws PrismException
	{
		// create new file log or use main log
		PrismLog tmpLog;
		if (file != null) {
			tmpLog = PrismFileLog.create(file.getPath(), append);
		} else {
			tmpLog = mainLog;
		}
		return tmpLog;
	}

	/**
	 * Get a string describing an output format, e.g. "in plain text format" for EXPORT_PLAIN.
	 */
	private static String getStringForExportType(int exportType)
	{
		switch (exportType) {
		case EXPORT_PLAIN:
			return "in plain text format";
		case EXPORT_MATLAB:
			return "in Matlab format";
		case EXPORT_DOT:
			return "in Dot format";
		case EXPORT_MRMC:
			return "in MRMC format";
		case EXPORT_ROWS:
			return "in rows format";
		case EXPORT_DOT_STATES:
			return "in Dot format (with states)";
		default:
			return "in ? format";
		}
	}

	/**
	 * Get a string describing the output destination specified by a File:
	 * "to file \"filename\"..." if non-null; "below:" if null
	 */
	private static String getDestinationStringForFile(File file)
	{
		return (file == null) ? "below:" : "to file \"" + file + "\"...";
	}

	//------------------------------------------------------------------------------
	// Old API methods, supported via new one
	//------------------------------------------------------------------------------

	/**
	 * @deprecated
	 * Load a PRISM model, build it, store for later use and return.
	 * Reachability and model construction are done symbolically, i.e. using (MT)BDDs.
	 * It is assumed that all constants in the PRISM model file have been defined by now.  
	 * @param modulesFile Model to build
	 */
	@Deprecated
	public Model buildModel(ModulesFile modulesFile) throws PrismException
	{
		loadPRISMModel(modulesFile);
		buildModel();
		return getBuiltModel();
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its transition matrix to a Spy file.
	 * @param model The model
	 * @param file File to export to
	 */
	@Deprecated
	public void exportToSpyFile(Model model, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportToSpyFile(file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export the MTBDD for its transition matrix to a Dot file.
	 * @param model The model
	 * @param file File to export to
	 */
	@Deprecated
	public void exportToDotFile(Model model, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportToDotFile(file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its transition matrix to a file
	 * @param model The model
	 * @param ordered Ensure that (source) states are in ascending order?
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_DOT}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_ROWS}
	 * <li> {@link #EXPORT_DOT_STATES}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		exportTransToFile(model, ordered, exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its transition matrix to a file (or to the log)
	 * @param model The model
	 * @param ordered Ensure that (source) states are in ascending order?
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_DOT}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_ROWS}
	 * <li> {@link #EXPORT_DOT_STATES}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportTransToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportTransToFile(ordered, exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its state rewards to a file
	 * @param model The model
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_MRMC}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportStateRewardsToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportStateRewardsToFile(exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its transition rewards to a file
	 * @param model The model
	 * @param ordered Ensure that (source) states are in ascending order?
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_ROWS}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportTransRewardsToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportTransRewardsToFile(ordered, exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its bottom strongly connected components (BSCCs) to a file
	 * @param model The model
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportBSCCsToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportBSCCsToFile(exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export the states satisfying labels from it and a properties file to a file
	 * The PropertiesFile should correspond to the model. 
	 * @param model The model
	 * @param modulesFile The corresponding (parsed) PRISM model (for the labels)
	 * @param propertiesFile The properties file (for further labels)
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportLabelsToFile(Model model, ModulesFile modulesFile, PropertiesFile propertiesFile, int exportType, File file)
			throws FileNotFoundException, PrismException
	{
		loadPRISMModelAndBuiltModel(modulesFile, model);
		exportLabelsToFile(propertiesFile, exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model and export its states to a file
	 * @param model The model
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	@Deprecated
	public void exportStatesToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportStateRewardsToFile(exportType, file);
	}

	/**
	 * @deprecated
	 * Load a (built) model, perform model checking of a property on it and return result.
	 * @param model The model to check
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 */
	@Deprecated
	public Result modelCheck(Model model, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		loadBuiltModel(model);
		return modelCheck(propertiesFile, expr);
	}

	/**
	 * @deprecated
	 * Load a PRISM PTA model, perform model checking of a property on it and return result.
	 * @param modulesFile The corresponding (parsed) PRISM model (for the labels)
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 */
	@Deprecated
	public Result modelCheckPTA(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		loadPRISMModel(modulesFile);
		return modelCheckPTA(propertiesFile, expr, null);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public Result modelCheckSimulator(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr, State initialState, long maxPathLength,
			SimulationMethod simMethod) throws PrismException
	{
		loadPRISMModel(modulesFile);
		return modelCheckSimulator(propertiesFile, expr, null, initialState, maxPathLength, simMethod);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public Result[] modelCheckSimulatorSimultaneously(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Expression> exprs, State initialState,
			long maxPathLength, SimulationMethod simMethod) throws PrismException
	{
		loadPRISMModel(modulesFile);
		return modelCheckSimulatorSimultaneously(propertiesFile, exprs, null, initialState, maxPathLength, simMethod);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void modelCheckSimulatorExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile, UndefinedConstants undefinedConstants,
			ResultsCollection results, Expression propertyToCheck, State initialState, long maxPathLength, SimulationMethod simMethod) throws PrismException,
			InterruptedException
	{
		loadPRISMModel(modulesFile);
		modelCheckSimulatorExperiment(propertiesFile, undefinedConstants, results, propertyToCheck, initialState, maxPathLength, simMethod);
	}

	/**
	 * @deprecated
	 * Load (built) model and compute steady-state probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to log. 
	 */
	@Deprecated
	public void doSteadyState(Model model) throws PrismException
	{
		doSteadyState(model, EXPORT_PLAIN, null);
	}

	/**
	 * @deprecated
	 * Load (built) model and compute steady-state probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 */
	@Deprecated
	public void doSteadyState(Model model, int exportType, File file) throws PrismException
	{
		loadBuiltModel(model);
		doSteadyState(exportType, file, null);
	}

	/**
	 * @deprecated
	 * Load (built) model and compute transient probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to log. 
	 */
	@Deprecated
	public void doTransient(Model model, double time) throws PrismException
	{
		doTransient(model, time, EXPORT_PLAIN, null, null);
	}

	/**
	 * @deprecated
	 * Load (built) model and compute transient probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	@Deprecated
	public void doTransient(Model model, double time, int exportType, File file, File fileIn) throws PrismException
	{
		loadBuiltModel(model);
		doTransient(time, exportType, file, fileIn);
	}
}

//------------------------------------------------------------------------------
