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

import java.io.*;
import java.util.*;

import jdd.*;
import dv.*;
import explicit.*;
import odd.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.*;
import parser.ast.*;
import simulator.*;
import simulator.method.SimulationMethod;
import pta.*;
import prism.Model;

/**
 * Main class for all PRISM's core functionality.
 * This is independent of the user interface (command line or gui).
 */
public class Prism implements PrismSettingsListener
{
	// prism version
	private static String version = "4.0.3";
	// prism version suffix
	//private static String versionSuffix = "";
	private static String versionSuffix = ".dev"; //".rXXXX";

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

	//------------------------------------------------------------------------------
	// Settings / flags / options
	//------------------------------------------------------------------------------

	// Main PRISM settings
	private PrismSettings settings;

	// Export parsed PRISM model?
	protected boolean exportPrism = false;
	protected File exportPrismFile = null;
	protected boolean exportPrismConst = false;
	protected File exportPrismConstFile = null;
	// Export target state info?
	protected boolean exportTarget = false;
	protected String exportTargetFilename = null;
	// Export product model info?
	protected boolean exportProductTrans = false;
	protected String exportProductTransFilename = null;
	protected boolean exportProductStates = false;
	protected String exportProductStatesFilename = null;

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

	// Round-off threshold for places where doubles are summed and compared to integers
	// (e.g. checking that probabilities sum to 1 in an update).
	private double sumRoundOff = 1e-5;

	// Method to use for (symbolic) state-space reachability
	private int reachMethod = REACH_BFS;

	//------------------------------------------------------------------------------
	// Logs
	//------------------------------------------------------------------------------

	private PrismLog mainLog; // one log for most output
	private PrismLog techLog; // another one for technical/diagnostic output

	//------------------------------------------------------------------------------
	// Parsers/translators/model checkers/simulators/etc.
	//------------------------------------------------------------------------------

	private static PrismParser thePrismParser = null;
	private static boolean prismParserInUse = false;
	private ExplicitFiles2MTBDD expf2mtbdd = null;
	private ExplicitModel2MTBDD expm2mtbdd = null;
	private SimulatorEngine theSimulator = null;

	//------------------------------------------------------------------------------
	// Event listeners
	//------------------------------------------------------------------------------

	private List<PrismModelListener> modelListeners;

	//------------------------------------------------------------------------------
	// State
	//------------------------------------------------------------------------------

	private enum ModelSource {
		PRISM_MODEL, EXPLICIT_FILES, BUILT_MODEL
	}

	// Info about currently loaded model, if any
	// Model source
	private ModelSource currentModelSource = ModelSource.PRISM_MODEL;
	// Model type
	private ModelType currentModelType = null;
	// PRISM model (null if none loaded, or built model given directly)
	private ModulesFile currentModulesFile = null;
	// Constants to be defined for PRISM model
	private Values currentDefinedMFConstants = null;
	// Built model storage - symbolic or explicit - at most one is non-null
	private Model currentModel = null;
	private explicit.Model currentModelExpl = null;
	// Are we doing digital clocks translation for PTAs?
	boolean digital = false;

	// Has the CUDD library been initialised yet?
	private boolean cuddStarted = false;

	//------------------------------------------------------------------------------
	// Constructors + options methods
	//------------------------------------------------------------------------------

	/**
	 * Construct a new Prism object.
	 * @param mainLog PrismLog where messages and model checking output will be sent.
	 * @param techLog PrismLog for output of detailed technical info (not really used).
	 */
	public Prism(PrismLog mainLog, PrismLog techLog)
	{
		// set up logs
		this.mainLog = mainLog;
		this.techLog = techLog;

		// set up some default options
		settings = new PrismSettings();
		// load user's default settings
		try {
			settings.loadSettingsFile();
		} catch (PrismException e) {
			// if there were no user defaults to load, create them
			try {
				settings.saveSettingsFile();
			} catch (PrismException ex) {
				mainLog.printWarning("Failed to create new PRISM settings file.");
			}
		}
		// add this Prism object as a results listener
		settings.addSettingsListener(this);
		// create list of model listeners
		modelListeners = new ArrayList<PrismModelListener>();
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
		PrismMTBDD.setMainLog(mainLog);
		PrismSparse.setMainLog(mainLog);
		PrismHybrid.setMainLog(mainLog);
	}

	/**
	 * Set the PrismLog for output of detailed technical info (not really used).
	 */
	public void setTechLog(PrismLog l)
	{
		// store new log
		techLog = l;
		// pass to other components
		JDD.SetOutputStream(techLog.getFilePointer());
		PrismMTBDD.setTechLog(techLog);
		PrismSparse.setTechLog(techLog);
		PrismHybrid.setTechLog(techLog);
	}

	// Set methods for main prism settings
	// (provided for convenience and for compatibility with old code)

	public void setEngine(int e) throws PrismException
	{
		settings.set(PrismSettings.PRISM_ENGINE, e - 1); // note index offset correction
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

	public void setCompact(boolean b) throws PrismException
	{
		settings.set(PrismSettings.PRISM_COMPACT, b);
	}

	public void setLinEqMethod(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_LIN_EQ_METHOD, i - 1); // note index offset correction
	}

	public void setLinEqMethodParam(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_LIN_EQ_METHOD_PARAM, d);
	}

	public void setMDPSolnMethod(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_MDP_SOLN_METHOD, i - 1); // note index offset correction
	}

	public void setTermCrit(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_TERM_CRIT, i - 1); // note index offset correction
	}

	public void setTermCritParam(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_TERM_CRIT_PARAM, d);
	}

	public void setMaxIters(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_MAX_ITERS, i);
	}

	public void setCUDDMaxMem(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_CUDD_MAX_MEM, i);
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
		settings.set(PrismSettings.PRISM_SCC_METHOD, i - 1); // note index offset correction
	}

	public void setExportAdv(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_EXPORT_ADV, i - 1); // note index offset correction
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

	public void setSumRoundOff(double d) throws PrismException
	{
		sumRoundOff = d;
	}

	public static int REACH_BFS = 1;
	public static int REACH_FRONTIER = 2;

	public void setReachMethod(int reachMethod)
	{
		this.reachMethod = reachMethod;
	}

	// Get methods

	public static String getVersion()
	{
		return version + versionSuffix;
	}

	public PrismLog getMainLog()
	{
		return mainLog;
	}

	public PrismLog getTechLog()
	{
		return techLog;
	}

	public PrismSettings getSettings()
	{
		return settings;
	}

	// Get methods for main prism settings
	// (as above, provided for convenience and for compatibility with old code)

	public int getEngine()
	{
		return settings.getInteger(PrismSettings.PRISM_ENGINE) + 1;
	} //note the correction

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

	public int getLinEqMethod()
	{
		return settings.getInteger(PrismSettings.PRISM_LIN_EQ_METHOD) + 1;
	} //NOTE THE CORRECTION for the ChoiceSetting index

	public double getLinEqMethodParam()
	{
		return settings.getDouble(PrismSettings.PRISM_LIN_EQ_METHOD_PARAM);
	}

	public int getMDPSolnMethod()
	{
		return settings.getInteger(PrismSettings.PRISM_MDP_SOLN_METHOD) + 1;
	} //NOTE THE CORRECTION for the ChoiceSetting index

	public int getTermCrit()
	{
		return settings.getInteger(PrismSettings.PRISM_TERM_CRIT) + 1;
	} //NOTE THE CORRECTION for the ChoiceSetting index

	public double getTermCritParam()
	{
		return settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM);
	}

	public int getMaxIters()
	{
		return settings.getInteger(PrismSettings.PRISM_MAX_ITERS);
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

	public long getCUDDMaxMem()
	{
		return settings.getInteger(PrismSettings.PRISM_CUDD_MAX_MEM);
	}

	public double getCUDDEpsilon()
	{
		return settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON);
	}

	public int getSCCMethod()
	{
		return settings.getInteger(PrismSettings.PRISM_SCC_METHOD) + 1;
	} //NOTE THE CORRECTION for the ChoiceSetting index

	public int getExportAdv()
	{
		return settings.getInteger(PrismSettings.PRISM_EXPORT_ADV) + 1;
	} //NOTE THE CORRECTION for the ChoiceSetting index

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

	public double getSumRoundOff()
	{
		return sumRoundOff;
	}

	public int getReachMethod()
	{
		return reachMethod;
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
			JDD.SetCUDDMaxMem(settings.getInteger(PrismSettings.PRISM_CUDD_MAX_MEM));
		}
	}

	//------------------------------------------------------------------------------
	// Access to parser, simulator, etc.
	//------------------------------------------------------------------------------

	/**
	 * Get (exclusive) access to the PRISM parser.
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
	public SCCComputer getSCCComputer(Model model)
	{
		return getSCCComputer(model.getReach(), model.getTrans01(), model.getAllDDRowVars(), model.getAllDDColVars());
	}

	/**
	 * Get an SCCComputer object.
	 * Type (i.e. algorithm) depends on SCCMethod PRISM option.
	 */
	public SCCComputer getSCCComputer(JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars)
	{
		SCCComputer sccComputer;
		switch (getSCCMethod()) {
		case Prism.LOCKSTEP:
			sccComputer = new SCCComputerLockstep(this, reach, trans01, allDDRowVars, allDDColVars);
			break;
		case Prism.SCCFIND:
			sccComputer = new SCCComputerSCCFind(this, reach, trans01, allDDRowVars, allDDColVars);
			break;
		case Prism.XIEBEEREL:
			sccComputer = new SCCComputerXB(this, reach, trans01, allDDRowVars, allDDColVars);
			break;
		default:
			sccComputer = new SCCComputerLockstep(this, reach, trans01, allDDRowVars, allDDColVars);
		}
		return sccComputer;
	}

	//------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------

	/**
	 * Compare two version numbers of PRISM (strings).
	 * Example ordering: { "1", "2.0", "2.1.alpha", "2.1.alpha.r5555", "2.1.alpha.r5557", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev.r6666", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"};
	 * Returns: 1 if v1>v2, -1 if v1<v2, 0 if v1=v2
	 */
	public static int compareVersions(String v1, String v2)
	{
		String ss1[], ss2[], tmp[];
		int i, n, x;
		double s1 = 0, s2 = 0;
		boolean s1num, s2num;

		// Exactly equal
		if (v1.equals(v2))
			return 0;
		// Otherwise split into sections
		ss1 = v1.split("\\.");
		ss2 = v2.split("\\.");
		// Pad if one is shorter
		n = Math.max(ss1.length, ss2.length);
		if (ss1.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss1.length; i++)
				tmp[i] = ss1[i];
			for (i = ss1.length; i < n; i++)
				tmp[i] = "";
			ss1 = tmp;
		}
		if (ss2.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss2.length; i++)
				tmp[i] = ss2[i];
			for (i = ss2.length; i < n; i++)
				tmp[i] = "";
			ss2 = tmp;
		}
		// Loop through sections of string
		for (i = 0; i < n; i++) {
			// 2.1.alpha < 2.1, etc.
			// 2.1.alpha < 2.1.alpha2 < 2.1.alpha3, etc.
			// so replace alphax with -10000+x
			if (ss1[i].matches("alpha.*")) {
				try {
					if (ss1[i].length() == 5)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(5));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (-10000 + x);
			}
			if (ss2[i].matches("alpha.*")) {
				try {
					if (ss2[i].length() == 5)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(5));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (-10000 + x);
			}
			// 2.1.beta < 2.1, etc.
			// 2.1.beta < 2.1.beta2 < 2.1.beta3, etc.
			// so replace betax with -100+x
			if (ss1[i].matches("beta.*")) {
				try {
					if (ss1[i].length() == 4)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(4));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (-100 + x);
			}
			if (ss2[i].matches("beta.*")) {
				try {
					if (ss2[i].length() == 4)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(4));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (-100 + x);
			}
			// 2 < 2.1, etc.
			// so treat 2 as 2.0
			if (ss1[i].equals(""))
				ss1[i] = "0";
			if (ss2[i].equals(""))
				ss2[i] = "0";
			// 2.1 < 2.1.dev, etc.
			// 2.1.dev < 2.1.dev2 < 2.1.dev3, etc.
			// so replace devx with 0.5+x/1000
			if (ss1[i].matches("dev.*")) {
				try {
					if (ss1[i].length() == 3)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(3));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (0.5 + x / 1000.0);
			}
			if (ss2[i].matches("dev.*")) {
				try {
					if (ss2[i].length() == 3)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(3));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (0.5 + x / 1000.0);
			}
			// replace rx (e.g. as in 4.0.alpha.r5555) with x
			if (ss1[i].matches("r.*")) {
				try {
					x = Integer.parseInt(ss1[i].substring(1));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + x;
			}
			if (ss2[i].matches("r.*")) {
				try {
					x = Integer.parseInt(ss2[i].substring(1));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + x;
			}
			// See if strings are integers
			try {
				s1num = true;
				s1 = Double.parseDouble(ss1[i]);
			} catch (NumberFormatException e) {
				s1num = false;
			}
			try {
				s2num = true;
				s2 = Double.parseDouble(ss2[i]);
			} catch (NumberFormatException e) {
				s2num = false;
			}
			if (s1num && s2num) {
				if (s1 < s2)
					return -1;
				if (s1 > s2)
					return 1;
				if (s1 == s2)
					continue;
			}
		}

		return 0;
	}

	/*// Simple test harness for compareVersions
	public static void main(String[] args)
	{
		 String v[] =  { "1", "2.0", "2.1.alpha", "2.1.alpha.r5555", "2.1.alpha.r5557", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev.r6666", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"};
		 for (int i = 0; i < v.length; i++) {
			 for (int j = 0; j < v.length; j++) {
				 int d = compareVersions(v[i], v[j]);
				 System.out.print(d == 1 ? ">" : d==0 ? "=" : d==-1 ? "<" : "?");
				 if (d != compareVersions(""+i, ""+j))
					 System.out.print("ERR(" + v[i] + "," + v[j] + ")");
					 
			 }
			 System.out.println();
		 }
	}*/

	/**
	 * Get access to the list of all PRISM language keywords.
	 */
	public static List<String> getListOfKeyords()
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
		mainLog.print("PRISM\n=====\n");
		mainLog.print("\nVersion: " + getVersion() + "\n");
		mainLog.print("Date: " + new java.util.Date() + "\n");
		try {
			String h = java.net.InetAddress.getLocalHost().getHostName();
			mainLog.print("Hostname: " + h + "\n");
		} catch (java.net.UnknownHostException e) {
		}

		// initialise cudd/jdd
		JDD.InitialiseCUDD(getCUDDMaxMem(), getCUDDEpsilon());
		cuddStarted = true;
		JDD.SetOutputStream(techLog.getFilePointer());

		// initialise libraries/engines
		PrismNative.initialise(this);
		PrismMTBDD.initialise(mainLog, techLog);
		PrismSparse.initialise(mainLog, techLog);
		PrismHybrid.initialise(mainLog, techLog);

		// set cudd manager in other packages
		DoubleVector.setCUDDManager();
		ODDUtils.setCUDDManager();
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
		String modelString;

		// compile pepa file to string
		try {
			modelString = pepa.compiler.Main.compile("" + file);
		} catch (pepa.compiler.InternalError e) {
			throw new PrismException("Could not import PEPA file:\n" + e.getMessage());
		}

		// parse string as prism model and return
		return parseModelString(modelString);
	}

	/**
	 * Import a PRISM model from a PEPA model in a string
	 * @param s String containing model
	 */
	public ModulesFile importPepaString(String s) throws PrismException, PrismLangException
	{
		File pepaFile = null;
		String modelString;

		// create temporary file containing pepa model
		try {
			pepaFile = File.createTempFile("tempPepa" + System.currentTimeMillis(), ".pepa");
			FileWriter write = new FileWriter(pepaFile);
			write.write(s);
			write.close();
		} catch (IOException e) {
			if (pepaFile != null)
				pepaFile.delete();
			throw new PrismException("Couldn't create temporary file for PEPA conversion");
		}

		// compile pepa file to string
		try {
			modelString = pepa.compiler.Main.compile("" + pepaFile);
		} catch (pepa.compiler.InternalError e) {
			if (pepaFile != null)
				pepaFile.delete();
			throw new PrismException("Could not import PEPA file:\n" + e.getMessage());
		}

		// parse string as prism model and return
		return parseModelString(modelString);
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
	 * Parse a PRISM properties file. Typically, you need to pass in the corresponding PRISM model
	 * (for access to constants, etc.) but if left null, a blank one is created automatically.
	 * @param mf Accompanying ModulesFile (null if not needed)
	 * @param file File to read in
	 */
	public PropertiesFile parsePropertiesFile(ModulesFile mf, File file) throws FileNotFoundException, PrismLangException
	{
		return parsePropertiesFile(mf, file, true);
	}

	/**
	 * Parse a PRISM properties file. Typically, you need to pass in the corresponding PRISM model
	 * (for access to constants, etc.) but if left null, a blank one is created automatically.
	 * You can also choose whether to do "tidy", i.e. post-parse checks and processing
	 * (this must be done at some point but may want to postpone to allow parsing of files with errors). 
	 * @param mf Accompanying ModulesFile (null if not needed)
	 * @param file File to read in
	 * @param tidy Whether or not to do "tidy" (post-parse checks and processing)
	 */
	public PropertiesFile parsePropertiesFile(ModulesFile mf, File file, boolean tidy) throws FileNotFoundException, PrismLangException
	{
		FileInputStream strProperties;
		PrismParser prismParser;
		PropertiesFile propertiesFile = null;

		// open file
		strProperties = new FileInputStream(file);

		// if null modules file passed, create a blank one
		if (mf == null) {
			mf = new ModulesFile();
			mf.setFormulaList(new FormulaList());
			mf.setConstantList(new ConstantList());
		}

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse file
				propertiesFile = prismParser.parsePropertiesFile(mf, strProperties);
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
	 * Parse a PRISM properties file from a string. Typically, you need to pass in the corresponding PRISM model
	 * (for access to constants, etc.) but if left null, a blank one is created automatically.
	 * @param mf Accompanying ModulesFile (null if not needed)
	 * @param s String to parse
	 */
	public PropertiesFile parsePropertiesString(ModulesFile mf, String s) throws PrismLangException
	{
		PrismParser prismParser;
		PropertiesFile propertiesFile = null;

		// if null modules file passed, create a blank one
		if (mf == null) {
			mf = new ModulesFile();
			mf.setFormulaList(new FormulaList());
			mf.setConstantList(new ConstantList());
		}

		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			prismParser = getPrismParser();
			try {
				// parse string
				propertiesFile = prismParser.parsePropertiesFile(mf, new ByteArrayInputStream(s.getBytes()));
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
	public Expression parseSingleExpressionString(String s) throws PrismLangException
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
	 * Load a (parsed) PRISM model, which will be stored and used for subsequent model checking etc.
	 * Some model constants can still be undefined at this stage.
	 * Pass in null to clear storage of the current PRISM model.
	 * @param modulesFile The PRISM model
	 */
	public void loadPRISMModel(ModulesFile modulesFile)
	{
		currentModelSource = ModelSource.PRISM_MODEL;
		// Store PRISM model
		currentModulesFile = modulesFile;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Reset dependent info
		currentModelType = currentModulesFile == null ? null : currentModulesFile.getModelType();
		currentDefinedMFConstants = null;
		currentModel = null;
		currentModelExpl = null;

		// Print basic model info
		mainLog.println("\nType:        " + currentModulesFile.getModelType());
		mainLog.print("Modules:     ");
		for (int i = 0; i < currentModulesFile.getNumModules(); i++) {
			mainLog.print(currentModulesFile.getModuleName(i) + " ");
		}
		mainLog.println();
		mainLog.print("Variables:   ");
		for (int i = 0; i < currentModulesFile.getNumVars(); i++) {
			mainLog.print(currentModulesFile.getVarName(i) + " ");
		}
		mainLog.println();

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
	 * Set any undefined constants for the currently loaded PRISM model
	 * (assuming they have changed since the last time this was called).
	 * @param definedMFConstants The constant values
	 */
	public void setPRISMModelConstants(Values definedMFConstants) throws PrismLangException
	{
		if (currentDefinedMFConstants == null && definedMFConstants == null)
			return;
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.equals(definedMFConstants))
			return;

		// Clear any existing built model(s)
		clearBuiltModel();
		// Store constants here and in ModulesFile
		currentDefinedMFConstants = definedMFConstants;
		currentModulesFile.setUndefinedConstants(definedMFConstants);
		// Reset dependent info
		currentModel = null;
		currentModelExpl = null;

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
		// Reset dependent info
		currentModelType = currentModulesFile == null ? null : currentModulesFile.getModelType();
		currentDefinedMFConstants = null;
		currentModelExpl = null;
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
		// Reset dependent info
		currentModelType = currentModel == null ? null : currentModel.getModelType();
		currentDefinedMFConstants = null;
		currentModelExpl = null;
	}

	/**
	 * Load files containing an explicit list of transitions/etc. for subsequent model building.
	 * A corresponding ModulesFile object is created and returned.
	 * @param statesFile File containing a list of states (optional, can be null)
	 * @param transFile File containing the list of transitions (required)
	 * @param labelsFile File containing label definitions (optional, can be null)
	 * @param typeOverride Type of model to be built (optional, use null if not required)
	 */
	public ModulesFile loadModelFromExplicitFiles(File statesFile, File transFile, File labelsFile, ModelType typeOverride) throws PrismException
	{
		currentModelSource = ModelSource.EXPLICIT_FILES;
		// Clear any existing built model(s)
		clearBuiltModel();
		// Create ExplicitFiles2MTBDD object and build state space
		expf2mtbdd = new ExplicitFiles2MTBDD(this, statesFile, transFile, labelsFile, typeOverride);
		currentModulesFile = expf2mtbdd.buildStates();
		// Reset dependent info
		currentModelType = currentModulesFile == null ? null : currentModulesFile.getModelType();
		currentDefinedMFConstants = null;
		currentModel = null;
		currentModelExpl = null;

		return currentModulesFile;
	}

	/**
	 * Get the type of the currently stored model.
	 * @return
	 */
	public ModelType getModelType()
	{
		return currentModelType;
	}

	/**
	 * Get the currently stored (parsed) PRISM model.
	 * @return
	 */
	public ModulesFile getPRISMModel()
	{
		return currentModulesFile;
	}

	/**
	 * Get the currently stored built (symbolic) model.
	 * @return
	 */
	public Model getBuiltModel()
	{
		return currentModel;
	}

	/**
	 * Get the currently stored built explicit model.
	 * @return
	 */
	public explicit.Model getBuiltModelExplicit()
	{
		return currentModelExpl;
	}

	/**
	 * Returns true if the current model is of a type that can be built (e.g. not a PTA).
	 */
	public boolean modelCanBeBuilt()
	{
		if (currentModelType == ModelType.PTA)
			return false;
		return true;
	}

	/**
	 * Returns true if the current model has been built (for the currently selected engine).
	 */
	public boolean modelIsBuilt()
	{
		return (getExplicit() ? (currentModelExpl != null) : (currentModel != null));
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
		doBuildModel();
	}

	/**
	 * Build the currently loaded PRISM model, if it needs to be done,
	 * i.e. if it has not been constructed yet for the current engine.
	 */
	public void buildModelIfRequired() throws PrismException
	{
		if (!modelIsBuilt())
			doBuildModel();
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
			if (currentModulesFile == null)
				throw new PrismException("There is no currently loaded PRISM model to build");

			if (currentModelType == ModelType.PTA) {
				throw new PrismException("You cannot build a PTA model explicitly, only perform model checking");
			}

			mainLog.print("\nBuilding model...\n");
			if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
				mainLog.println("Model constants: " + currentDefinedMFConstants);

			// Build model
			l = System.currentTimeMillis();
			switch (currentModelSource) {
			case PRISM_MODEL:
				if (!getExplicit()) {
					Modules2MTBDD mod2mtbdd = new Modules2MTBDD(this, currentModulesFile);
					currentModel = mod2mtbdd.translate();
					currentModelExpl = null;
				} else {
					ConstructModel constructModel = new ConstructModel(getSimulator(), mainLog);
					constructModel.setFixDeadlocks(getFixDeadlocks());
					currentModelExpl = constructModel.constructModel(currentModulesFile, false, true);
					currentModel = null;
				}
				// if (...) ... currentModel = buildModelExplicit(currentModulesFile);
				break;
			case EXPLICIT_FILES:
				if (!getExplicit()) {
					// check ExplicitFiles2MTBDD object created
					if (expf2mtbdd == null)
						throw new PrismException("ExplicitFiles2MTBDD object never created");
					currentModel = expf2mtbdd.buildModel();
				} else {
					throw new PrismException("Explicit import not yet supported for explicit engine");
				}
				break;
			default:
				throw new PrismException("Don't know how to build model from source " + currentModelSource);
			}
			l = System.currentTimeMillis() - l;
			mainLog.println("\nTime for model construction: " + l / 1000.0 + " seconds.");

			// Deal with deadlocks
			if (!getExplicit()) {
				StateList deadlocks = currentModel.getDeadlockStates();
				int numDeadlocks = deadlocks.size();
				if (numDeadlocks > 0) {
					if (digital) {
						// For digital clocks, by construction, deadlocks can only occur from timelocks (and are not allowed)
						throw new PrismException("Timelock in PTA, e.g. in state (" + deadlocks.getFirstAsValues() + ")");
					}
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
					if (digital) {
						// For digital clocks, by construction, deadlocks can only occur from timelocks (and are not allowed)
						int dl = currentModelExpl.getFirstDeadlockState();
						String dls = currentModelExpl.getStatesList().get(dl).toString(currentModulesFile);
						throw new PrismException("Timelock in PTA, e.g. in state " + dls);
					}
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
			if (!getExplicit()) {
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
		explicit.Model modelExpl;
		Model model;
		List<State> statesList;

		if (modulesFile.getModelType() == ModelType.PTA) {
			throw new PrismException("You cannot build a PTA model explicitly, only perform model checking");
		}

		mainLog.print("\nBuilding model...\n");
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);

		constructModel = new ConstructModel(getSimulator(), mainLog);
		modelExpl = constructModel.constructModel(modulesFile);
		statesList = constructModel.getStatesList();

		// create Explicit2MTBDD object
		expm2mtbdd = new ExplicitModel2MTBDD(this);

		// build model
		l = System.currentTimeMillis();
		model = expm2mtbdd.buildModel(modelExpl, statesList, modulesFile, false);
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
	}

	/**
	 * Export the currently loaded model's transition matrix to a Spy file.
	 * @param file File to export to
	 */
	public void exportToSpyFile(File file) throws FileNotFoundException, PrismException
	{
		int depth;
		JDDNode tmp;

		if (getExplicit())
			throw new PrismException("Export to Spy file not yet supported by explicit engine");

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
		if (getExplicit())
			throw new PrismException("Export to Dot file not yet supported by explicit engine");

		// Build model, if necessary
		buildModelIfRequired();

		mainLog.println("\nExporting to dot file \"" + file + "\"...");

		// export to dot file
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
		if (!getExplicit()) {
			currentModel.exportToFile(exportType, ordered, file);
		} else {
			PrismLog tmpLog = getPrismLogForFile(file);
			switch (exportType) {
			case Prism.EXPORT_PLAIN:
				currentModelExpl.exportToPrismExplicitTra(tmpLog);
				break;
			case Prism.EXPORT_MATLAB:
			case Prism.EXPORT_DOT:
			case Prism.EXPORT_MRMC:
			case Prism.EXPORT_ROWS:
			case Prism.EXPORT_DOT_STATES:
				throw new PrismException("Export not yet supported");
			}
		}

		// for export to dot with states, need to do a bit more
		if (!getExplicit() && exportType == EXPORT_DOT_STATES) {
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
	 * Export the currently loaded model's state rewards to a file
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_MRMC}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		String s;

		if (getExplicit())
			throw new PrismException("Export of state rewards not yet supported by explicit engine");

		// rows format does not apply to vectors
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		// Build model, if necessary
		buildModelIfRequired();

		// print message
		mainLog.print("\nExporting state rewards vector ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// do export
		s = currentModel.exportStateRewardsToFile(exportType, file);
		if (s != null)
			mainLog.println("Rewards exported to files: " + s);
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
		String s;

		if (getExplicit())
			throw new PrismException("Export of transition rewards not yet supported by explicit engine");

		// can only do ordered version of export for MDPs
		if (currentModelType == ModelType.MDP) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition reward matrix for MDPs; using ordered.");
			ordered = true;
		}
		// can only do ordered version of export for MRMC
		if (exportType == EXPORT_MRMC) {
			if (!ordered)
				mainLog.printWarning("Cannot export unordered transition reward matrix in MRMC format; using ordered.");
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
		mainLog.print("\nExporting transition rewards matrix ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// do export
		s = currentModel.exportTransRewardsToFile(exportType, ordered, file);
		if (s != null)
			mainLog.println("Rewards exported to files: " + s);
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
		SCCComputer sccComputer;
		Vector<JDDNode> bsccs;
		JDDNode not, bscc;

		if (getExplicit())
			throw new PrismException("Export of BSCCs not yet supported by explicit engine");

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
		sccComputer = getSCCComputer(currentModel);
		l = System.currentTimeMillis();
		sccComputer.computeBSCCs();
		l = System.currentTimeMillis() - l;
		bsccs = sccComputer.getVectBSCCs();
		not = sccComputer.getNotInBSCCs();
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
		for (i = 0; i < currentModel.getNumVars(); i++) {
			tmpLog.print(currentModel.getVarName(i));
			if (i < currentModel.getNumVars() - 1)
				tmpLog.print(",");
		}
		tmpLog.println(")");

		// print states for each bscc
		n = bsccs.size();
		for (i = 0; i < n; i++) {
			tmpLog.println();
			if (exportType == EXPORT_MATLAB)
				tmpLog.print("% ");
			tmpLog.println("BSCC " + (i + 1) + "/" + n + ":");
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("bscc" + (i + 1) + "=[");
			bscc = bsccs.get(i);
			if (exportType != EXPORT_MATLAB)
				new StateListMTBDD(bscc, currentModel).print(tmpLog);
			else
				new StateListMTBDD(bscc, currentModel).printMatlab(tmpLog);
			if (exportType == EXPORT_MATLAB)
				tmpLog.println("];");
			JDD.Deref(bscc);
		}

		JDD.Deref(not);

		// tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Export the states satisfying labels from the currently loaded model and a properties file to a file.
	 * The PropertiesFile should correspond to the currently loaded model. 
	 * @param propertiesFile The properties file (for further labels)
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportLabelsToFile(PropertiesFile propertiesFile, int exportType, File file) throws FileNotFoundException, PrismException
	{
		int i, n;
		LabelList ll;
		Expression expr;
		prism.StateModelChecker mc = null;
		JDDNode dd, labels[];
		String labelNames[];

		if (getExplicit())
			throw new PrismException("Export of labels not yet supported by explicit engine");

		// get label list and size
		if (propertiesFile == null) {
			ll = currentModulesFile.getLabelList();
			n = ll.size();
		} else {
			ll = propertiesFile.getCombinedLabelList();
			n = ll.size();
		}

		// Build model, if necessary
		buildModelIfRequired();

		// print message
		mainLog.print("\nExporting labels and satisfying states ");
		mainLog.println(getDestinationStringForFile(file));

		// convert labels to bdds
		if (n > 0) {
			mc = new prism.StateModelChecker(this, currentModel, propertiesFile);
		}
		labels = new JDDNode[n + 2];
		labels[0] = currentModel.getStart();
		labels[1] = currentModel.getDeadlocks();
		for (i = 0; i < n; i++) {
			expr = ll.getLabel(i);
			dd = mc.checkExpressionDD(expr);
			labels[i + 2] = dd;
		}
		// put names for labels in an array
		labelNames = new String[n + 2];
		labelNames[0] = "init";
		labelNames[1] = "deadlock";
		for (i = 0; i < n; i++) {
			labelNames[i + 2] = ll.getLabelName(i);
		}

		// export them to a file
		PrismMTBDD.ExportLabels(labels, labelNames, "l", currentModel.getAllDDRowVars(), currentModel.getODD(), exportType, (file != null) ? file.getPath()
				: null);

		// deref dds
		for (i = 0; i < n; i++) {
			JDD.Deref(labels[i + 2]);
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
		if (!getExplicit()) {
			currentModel.exportStates(exportType, tmpLog);
		} else {
			currentModelExpl.exportStates(exportType, currentModulesFile.createVarList(), tmpLog);
		}

		// Tidy up
		if (file != null)
			tmpLog.close();
	}

	/**
	 * Perform model checking of a property on the currently loaded model and return result.
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 */
	public Result modelCheck(PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		Result res = null;
		Values definedPFConstants = propertiesFile.getConstantValues();

		if (!digital)
			mainLog.printSeparator();
		mainLog.println("\nModel checking: " + expr);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		if (definedPFConstants != null && definedPFConstants.getNumValues() > 0)
			mainLog.println("Property constants: " + definedPFConstants);

		// Check that property is valid for the current model type
		expr.checkValid(currentModelType);

		// For PTAs...
		if (currentModelType == ModelType.PTA) {
			return modelCheckPTA(propertiesFile, expr, definedPFConstants);
		}

		// Build model, if necessary
		buildModelIfRequired();

		// Create new model checker object and do model checking
		if (!getExplicit()) {
			ModelChecker mc = null;
			switch (currentModelType) {
			case DTMC:
				mc = new ProbModelChecker(this, currentModel, propertiesFile);
				break;
			case MDP:
				mc = new NondetModelChecker(this, currentModel, propertiesFile);
				break;
			case CTMC:
				mc = new StochModelChecker(this, currentModel, propertiesFile);
				break;
			default:
				throw new PrismException("Unknown model type " + currentModelType);
			}
			res = mc.check(expr);
		} else {
			explicit.StateModelChecker mc = null;
			switch (currentModelType) {
			case DTMC:
				mc = new DTMCModelChecker();
				break;
			case MDP:
				mc = new MDPModelChecker();
				break;
			case CTMC:
				mc = new CTMCModelChecker();
				break;
			case CTMDP:
				mc = new CTMDPModelChecker();
				break;
			case STPG:
				mc = new STPGModelChecker();
				break;
			default:
				throw new PrismException("Unknown model type " + currentModelType);
			}
			mc.setLog(mainLog);
			mc.setSettings(settings);
			mc.setModulesFileAndPropertiesFile(currentModulesFile, propertiesFile);
			res = mc.check(currentModelExpl, expr);
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

		// Digital clocks translation
		if (settings.getString(PrismSettings.PRISM_PTA_METHOD).equals("Digital clocks")) {
			digital = true;
			ModulesFile oldModulesFile = currentModulesFile;
			try {
				DigitalClocks dc = new DigitalClocks(this);
				dc.translate(oldModulesFile, propertiesFile, expr);
				currentModulesFile = dc.getNewModulesFile();
				currentModulesFile.setUndefinedConstants(oldModulesFile.getConstantValues());
				currentModelType = ModelType.MDP;
				// If required, export generated PRISM model
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
				return modelCheck(propertiesFile, expr);
			} finally {
				digital = false;
				currentModulesFile = oldModulesFile;
				currentModelType = ModelType.PTA;
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
	public Result modelCheckSimulator(PropertiesFile propertiesFile, Expression expr, Values definedPFConstants, State initialState, int maxPathLength,
			SimulationMethod simMethod) throws PrismException
	{
		Object res = null;

		// Print info
		mainLog.printSeparator();
		mainLog.println("\nSimulating: " + expr);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		if (definedPFConstants != null && definedPFConstants.getNumValues() > 0)
			mainLog.println("Property constants: " + definedPFConstants);

		// Check that property is valid for this model type
		expr.checkValid(currentModelType);

		// Do simulation
		res = getSimulator().modelCheckSingleProperty(currentModulesFile, propertiesFile, expr, initialState, maxPathLength, simMethod);

		return new Result(res);
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
			int maxPathLength, SimulationMethod simMethod) throws PrismException
	{
		Object[] res = null;

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

		// Check that properties are valid for this model type
		for (Expression expr : exprs)
			expr.checkValid(currentModelType);

		// Do simulation
		res = getSimulator().modelCheckMultipleProperties(currentModulesFile, propertiesFile, exprs, initialState, maxPathLength, simMethod);

		Result[] resArray = new Result[res.length];
		for (int i = 0; i < res.length; i++)
			resArray[i] = new Result(res[i]);
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
	 * @param resultsCollection Where to store the results
	 * @param expr The property to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 * @throws PrismException if something goes wrong with the sampling algorithm
	 * @throws InterruptedException if the thread is interrupted
	 */
	public void modelCheckSimulatorExperiment(PropertiesFile propertiesFile, UndefinedConstants undefinedConstants, ResultsCollection results, Expression expr,
			State initialState, int pathLength, SimulationMethod simMethod) throws PrismException, InterruptedException
	{
		// Print info
		mainLog.printSeparator();
		mainLog.println("\nSimulating: " + expr);
		if (currentDefinedMFConstants != null && currentDefinedMFConstants.getNumValues() > 0)
			mainLog.println("Model constants: " + currentDefinedMFConstants);
		mainLog.println("Property constants: " + undefinedConstants.getPFDefinedConstantsString());

		// Do simulation
		getSimulator().modelCheckExperiment(currentModulesFile, propertiesFile, undefinedConstants, results, expr, initialState, pathLength, simMethod);
	}

	/**
	 * Generate a random path through the model using the simulator.
	 * @param modulesFile The model
	 * @param details Information about the path to be generated
	 * @param maxPathLength The maximum length of path to generate
	 * @param file File to output the path to (stdout if null)
	 */
	public void generateSimulationPath(ModulesFile modulesFile, String details, int maxPathLength, File file) throws PrismException, PrismLangException
	{
		GenerateSimulationPath genPath = new GenerateSimulationPath(getSimulator(), mainLog);
		genPath.generateSimulationPath(modulesFile, null, details, maxPathLength, file);
	}

	/**
	 * Compute steady-state probabilities for the current model (DTMCs/CTMCs only).
	 * Output probability distribution to log. 
	 */
	public void doSteadyState() throws PrismException
	{
		doSteadyState(EXPORT_PLAIN, null);
	}

	/**
	 * Compute steady-state probabilities for the current model (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 */
	public void doSteadyState(int exportType, File file) throws PrismException
	{
		long l = 0; // timer
		ModelChecker mc = null;
		StateValues probs = null;
		explicit.StateValues probsExpl = null;
		PrismLog tmpLog;

		if (!(currentModelType == ModelType.CTMC || currentModelType == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");

		if (file != null && getEngine() == MTBDD)
			throw new PrismException("Steady-state probability export not supported for MTBDD engine");
		// TODO: auto-switch?

		mainLog.printSeparator();
		mainLog.println("\nComputing steady-state probabilities...");

		// Build model, if necessary
		buildModelIfRequired();

		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		l = System.currentTimeMillis();

		if (!getExplicit()) {
			if (currentModel.getModelType() == ModelType.DTMC) {
				mc = new ProbModelChecker(this, currentModel, null);
				probs = ((ProbModelChecker) mc).doSteadyState();
			} else if (currentModel.getModelType() == ModelType.CTMC) {
				mc = new StochModelChecker(this, currentModel, null);
				probs = ((StochModelChecker) mc).doSteadyState();
			} else {
				throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
			}
		} else {
			if (currentModelExpl.getModelType() == ModelType.DTMC) {
				DTMCModelChecker mcDTMC = new DTMCModelChecker();
				mcDTMC.setLog(mainLog);
				mcDTMC.setSettings(settings);
				probsExpl = mcDTMC.doSteadyState((DTMC) currentModelExpl, null);
			} else if (currentModelType == ModelType.CTMC) {
				throw new PrismException("Not implemented yet");
			} else {
				throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
			}
		}

		l = System.currentTimeMillis() - l;

		// print message
		mainLog.print("\nPrinting steady-state probabilities ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(file);

		// print out or export probabilities
		if (!getExplicit())
			probs.print(tmpLog, file == null, exportType == EXPORT_MATLAB, file == null);
		else
			probsExpl.print(tmpLog, file == null, exportType == EXPORT_MATLAB, file == null, true);

		// print out computation time
		mainLog.println("\nTime for steady-state probability computation: " + l / 1000.0 + " seconds.");

		// tidy up
		if (!getExplicit())
			probs.clear();
		else
			probsExpl.clear();
		if (file != null)
			tmpLog.close();
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
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * For a discrete-time model, {@code time} will be cast to an integer.
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doTransient(double time, int exportType, File file, File fileIn) throws PrismException
	{
		long l = 0; // timer
		ModelChecker mc = null;
		StateValues probs = null;
		explicit.StateValues probsExpl = null;
		PrismLog tmpLog;

		if (!(currentModelType == ModelType.CTMC || currentModelType == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");

		if (time < 0)
			throw new PrismException("Cannot compute transient probabilities for negative time value");

		if (file != null && getEngine() == MTBDD)
			throw new PrismException("Transient probability export only supported for sparse/hybrid engines");

		mainLog.printSeparator();
		if (currentModelType.continuousTime()) {
			mainLog.println("\nComputing transient probabilities (time = " + time + ")...");
		} else {
			mainLog.println("\nComputing transient probabilities (time = " + (int) time + ")...");
		}

		// Build model, if necessary
		buildModelIfRequired();

		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

		l = System.currentTimeMillis();

		if (!getExplicit()) {
			if (currentModelType == ModelType.DTMC) {
				mc = new ProbModelChecker(this, currentModel, null);
				probs = ((ProbModelChecker) mc).doTransient((int) time, fileIn);
			} else {
				mc = new StochModelChecker(this, currentModel, null);
				probs = ((StochModelChecker) mc).doTransient(time, fileIn);
			}
		} else {
			if (currentModelType == ModelType.DTMC) {
				throw new PrismException("Not implemented yet");
			} else if (currentModelType == ModelType.CTMC) {
				CTMCModelChecker mcCTMC = new CTMCModelChecker();
				mcCTMC.setLog(mainLog);
				mcCTMC.setSettings(settings);
				probsExpl = mcCTMC.doTransient((CTMC) currentModelExpl, time, fileIn);
			} else {
				throw new PrismException("Transient probabilities only computed for DTMCs/CTMCs");
			}
		}

		l = System.currentTimeMillis() - l;

		// print message
		mainLog.print("\nPrinting transient probabilities ");
		mainLog.print(getStringForExportType(exportType) + " ");
		mainLog.println(getDestinationStringForFile(file));

		// create new file log or use main log
		tmpLog = getPrismLogForFile(file);

		// print out or export probabilities
		if (!getExplicit())
			probs.print(tmpLog, file == null, exportType == EXPORT_MATLAB, file == null);
		else
			probsExpl.print(tmpLog, file == null, exportType == EXPORT_MATLAB, file == null, true);

		// print out computation time
		mainLog.println("\nTime for transient probability computation: " + l / 1000.0 + " seconds.");

		// tidy up
		if (!getExplicit())
			probs.clear();
		else
			probsExpl.clear();
		if (file != null)
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
	public void doTransient(UndefinedConstants times, int exportType, File file, File fileIn) throws PrismException
	{
		int i, timeInt = 0, initTimeInt = 0;
		double timeDouble = 0, initTimeDouble = 0;
		Object time;
		long l = 0; // timer
		StateValues probs = null, initDist = null;
		explicit.StateValues probsExpl = null, initDistExpl = null;
		PrismLog tmpLog = null;

		if (!(currentModelType == ModelType.CTMC || currentModelType == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");

		if (file != null && getEngine() == MTBDD)
			throw new PrismException("Transient probability export only supported for sparse/hybrid engines");

		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC)
			exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS)
			exportType = EXPORT_PLAIN;

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

			mainLog.printSeparator();
			mainLog.println("\nComputing transient probabilities (time = " + time + ")...");
			
			// Build model, if necessary
			buildModelIfRequired();

			l = System.currentTimeMillis();

			if (!getExplicit()) {
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
			} else {
				if (currentModelType.continuousTime()) {
					CTMCModelChecker mc = new CTMCModelChecker();
					mc.setLog(mainLog);
					mc.setSettings(settings);
					if (i == 0) {
						initDistExpl = mc.readDistributionFromFile(fileIn, currentModelExpl);
						initTimeDouble = 0;
					}
					probsExpl = mc.doTransient((CTMC) currentModelExpl, timeDouble - initTimeDouble, initDistExpl);
				} else {
					throw new PrismException("Not implemented yet");
				}
			}

			l = System.currentTimeMillis() - l;

			// print message
			mainLog.print("\nPrinting transient probabilities ");
			mainLog.print(getStringForExportType(exportType) + " ");
			mainLog.println(getDestinationStringForFile(file));

			// create new file log or use main log
			tmpLog = getPrismLogForFile(file);

			// print out or export probabilities
			if (!getExplicit())
				probs.print(tmpLog, file == null, exportType == EXPORT_MATLAB, file == null);
			else
				probsExpl.print(tmpLog, file == null, exportType == EXPORT_MATLAB, file == null, true);

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
		if (!getExplicit())
			probs.clear();
		else
			probsExpl.clear();
		if (file != null)
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
	 * Clear the built model if needed (free/deallocate memory etc)
	 */
	private void clearBuiltModel()
	{
		if (currentModel != null)
			currentModel.clear();
		/*if (currentModelExpl != null)
			currentModelExpl.clear();*/
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
		// Close down CUDD/JDD
		if (cuddStarted) {
			JDD.CloseDownCUDD(check);
			if (jdd.DebugJDD.debugEnabled)
				DebugJDD.endLifeCycle();
		}
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
			tmpLog = new PrismFileLog(file.getPath(), append);
			if (!tmpLog.ready()) {
				throw new PrismException("Could not open file \"" + file + "\" for output");
			}
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
	 * Old API: 
	 * Load a PRISM model, build it, store for later use and return.
	 * Reachability and model construction are done symbolically, i.e. using (MT)BDDs.
	 * It is assumed that all constants in the PRISM model file have been defined by now.  
	 * @param modulesFile Model to build
	 */
	public Model buildModel(ModulesFile modulesFile) throws PrismException
	{
		loadPRISMModel(modulesFile);
		buildModel();
		return getBuiltModel();
	}

	/**
	 * Old API:
	 * Load a (built) model and export its transition matrix to a Spy file.
	 * @param model The model
	 * @param file File to export to
	 */
	public void exportToSpyFile(Model model, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportToSpyFile(file);
	}

	/**
	 * Old API:
	 * Load a (built) model and export the MTBDD for its transition matrix to a Dot file.
	 * @param model The model
	 * @param file File to export to
	 */
	public void exportToDotFile(Model model, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportToDotFile(file);
	}

	/**
	 * Old API:
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
	public void exportToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		exportTransToFile(model, ordered, exportType, file);
	}

	/**
	 * Old API:
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
	public void exportTransToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportTransToFile(ordered, exportType, file);
	}

	/**
	 * Old API:
	 * Load a (built) model and export its state rewards to a file
	 * @param model The model
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_MRMC}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportStateRewardsToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportStateRewardsToFile(exportType, file);
	}

	/**
	 * Old API:
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
	public void exportTransRewardsToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportTransRewardsToFile(ordered, exportType, file);
	}

	/**
	 * Old API:
	 * Load a (built) model and export its bottom strongly connected components (BSCCs) to a file
	 * @param model The model
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportBSCCsToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportBSCCsToFile(exportType, file);
	}

	/**
	 * Old API:
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
	public void exportLabelsToFile(Model model, ModulesFile modulesFile, PropertiesFile propertiesFile, int exportType, File file)
			throws FileNotFoundException, PrismException
	{
		loadPRISMModelAndBuiltModel(modulesFile, model);
		exportLabelsToFile(propertiesFile, exportType, file);
	}

	/**
	 * Old API:
	 * Load a (built) model and export its states to a file
	 * @param model The model
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportStatesToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		loadBuiltModel(model);
		exportStateRewardsToFile(exportType, file);
	}

	/**
	 * Old API:
	 * Load a (built) model, perform model checking of a property on it and return result.
	 * @param model The model to check
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 */
	public Result modelCheck(Model model, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		loadBuiltModel(model);
		return modelCheck(propertiesFile, expr);
	}

	/**
	 * Old API:
	 * Load a PRISM PTA model, perform model checking of a property on it and return result.
	 * @param modulesFile The corresponding (parsed) PRISM model (for the labels)
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property to check
	 */
	public Result modelCheckPTA(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		loadPRISMModel(modulesFile);
		return modelCheckPTA(propertiesFile, expr, null);
	}

	/**
	 * Old API:
	 */
	public Result modelCheckSimulator(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr, State initialState, int maxPathLength,
			SimulationMethod simMethod) throws PrismException
	{
		loadPRISMModel(modulesFile);
		return modelCheckSimulator(propertiesFile, expr, null, initialState, maxPathLength, simMethod);
	}

	/**
	 * Old API:
	 */
	public Result[] modelCheckSimulatorSimultaneously(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Expression> exprs, State initialState,
			int maxPathLength, SimulationMethod simMethod) throws PrismException
	{
		loadPRISMModel(modulesFile);
		return modelCheckSimulatorSimultaneously(propertiesFile, exprs, null, initialState, maxPathLength, simMethod);
	}

	/**
	 * Old API:
	 */
	public void modelCheckSimulatorExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile, UndefinedConstants undefinedConstants,
			ResultsCollection results, Expression propertyToCheck, State initialState, int pathLength, SimulationMethod simMethod) throws PrismException,
			InterruptedException
	{
		loadPRISMModel(modulesFile);
		modelCheckSimulatorExperiment(propertiesFile, undefinedConstants, results, propertyToCheck, initialState, pathLength, simMethod);
	}

	/**
	 * Old API:
	 * Load (built) model and compute steady-state probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to log. 
	 */
	public void doSteadyState(Model model) throws PrismException
	{
		doSteadyState(model, EXPORT_PLAIN, null);
	}

	/**
	 * Old API:
	 * Load (built) model and compute steady-state probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 */
	public void doSteadyState(Model model, int exportType, File file) throws PrismException
	{
		loadBuiltModel(model);
		doSteadyState(exportType, file);
	}

	/**
	 * Old API:
	 * Load (built) model and compute transient probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to log. 
	 */
	public void doTransient(Model model, double time) throws PrismException
	{
		doTransient(model, time, EXPORT_PLAIN, null, null);
	}

	/**
	 * Old API:
	 * Load (built) model and compute transient probabilities (DTMCs/CTMCs only).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doTransient(Model model, double time, int exportType, File file, File fileIn) throws PrismException
	{
		loadBuiltModel(model);
		doTransient(time, exportType, file, fileIn);
	}
}

//------------------------------------------------------------------------------
