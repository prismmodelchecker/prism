//==============================================================================
//	
//	Copyright (c) 2002-
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
import odd.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.*;
import parser.ast.*;
import simulator.*;
import pta.*;

/**
 * Main class for all PRISM's core functionality.
 * This is independent of the user interface (command line or gui).
 */
public class Prism implements PrismSettingsListener
{
	// prism version
	private static String version = "4.0.alpha";
	// prism version suffix
	private static String rev = "$Rev: 2167$";
	private static String versionSuffix = ".r" + rev.substring(6, rev.length() - 1);
	
	//------------------------------------------------------------------------------
	// Constants
	//------------------------------------------------------------------------------
	
	// underlying computation engine
	public static final int MTBDD = 1;
	public static final int SPARSE = 2;
	public static final int HYBRID = 3;
	
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
	
	// Some additional settings (here because not available from main options panel in GUI) 
	// Export target state info?
	protected boolean exportTarget;
	protected String exportTargetFilename;
	
	// A few miscellaneous options (i.e. defunct/hidden/undocumented/etc.)
	// See constructor below for default values
	
	private boolean doReach;	// do reachability? (sometimes might want to skip it)
	private boolean bsccComp;	// do bscc computation before steady-state?
	private boolean checkZeroLoops;
	
	// MTBDD construction method (NOW DEFUNCT)
	//  1 - use with ordering 1: nondet vars form a tree at the top
	//  3 - use with ordering 2: zero for nonexistant bits
	// nb: option 2 removed because it was stupid
	private int construction;

	// MTBDD variable ordering
	//  1 - (s ... s) (l ... l) (r c ... r c)
	//  2 - (s l ... l r c ... r c) (s l ... l r c ... r c) ...
	private int ordering;
	
	// Round-off threshold for places where doubles are summed and compared to integers
	// (e.g. checking that probabilities sum to 1 in an update).
	private double sumRoundOff;
	
	//------------------------------------------------------------------------------
	// Logs
	//------------------------------------------------------------------------------
	
	private PrismLog mainLog; // one log for most output
	private PrismLog techLog; // another one for technical/diagnostic output
	
	//------------------------------------------------------------------------------
	// parsers/translators
	//------------------------------------------------------------------------------
	
	private static PrismParser thePrismParser = null;
	private static boolean prismParserInUse = false;
	private Modules2MTBDD mod2mtbdd = null;
	private Explicit2MTBDD exp2mtbdd = null;
	
	//------------------------------------------------------------------------------
	// model checkers/simulators
	//------------------------------------------------------------------------------
	
	private ModelChecker mc = null;
	private SimulatorEngine theSimulator = null;
	
	//------------------------------------------------------------------------------
	// flags
	//------------------------------------------------------------------------------
	
	private boolean cuddStarted = false;

	//------------------------------------------------------------------------------
	// methods
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
		}
		catch(PrismException e) {
			// if there were no user defaults to load, create them
			try
			{
				settings.saveSettingsFile();
			}
			catch(PrismException ex)
			{
				System.err.println("Warning: Failed to create new PRISM settings file.");
			}
		}
		// add this Prism object as a listener
		settings.addSettingsListener(this);
		
		// default values for miscellaneous options 
		exportTarget = false;
		exportTargetFilename = null;
		doReach = true;
		bsccComp = true;
		checkZeroLoops = false;
		construction = 3;
		ordering = 1;
		sumRoundOff = 1e-5;
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
		settings.set(PrismSettings.PRISM_ENGINE, e-1); // note index offset correction
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
		settings.set(PrismSettings.PRISM_LIN_EQ_METHOD, i-1); // note index offset correction
	}
	
	public void setLinEqMethodParam(double d) throws PrismException
	{
		settings.set(PrismSettings.PRISM_LIN_EQ_METHOD_PARAM,  d);
	}
	
	public void setTermCrit(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_TERM_CRIT, i-1); // note index offset correction
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
		settings.set(PrismSettings.PRISM_SCC_METHOD, i-1); // note index offset correction
	}
	
	public void setExportAdv(int i) throws PrismException
	{
		settings.set(PrismSettings.PRISM_EXPORT_ADV, i-1); // note index offset correction
	}

	public void setExportAdvFilename(String s) throws PrismException
	{
		settings.set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, s);
	}
	
	// Set methods for miscellaneous options
	
	public void setExportTarget(boolean b) throws PrismException
	{
		exportTarget = b;
	}
	
	public void setExportTargetFilename(String s) throws PrismException
	{
		exportTargetFilename = s;
	}
	
	public void setDoReach(boolean b) throws PrismException
	{
		doReach = b;
	}
	
	public void setBSCCComp(boolean b) throws PrismException
	{
		bsccComp = b;
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
	
	// Get methods
	
	public static String getVersion()
	{ return version + versionSuffix; }
	
	public PrismLog getMainLog()
	{ return mainLog; }
	
	public PrismLog getTechLog()
	{ return techLog; }
	
	// Get methods for main prism settings
	// (as above, provided for convenience and for compatibility with old code)
	
	public int getEngine()
	{ return settings.getInteger(PrismSettings.PRISM_ENGINE)+1; } //note the correction
	
	public boolean getDoProbChecks()
	{ return settings.getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS); }
	
	public int getLinEqMethod()
	{ return settings.getInteger(PrismSettings.PRISM_LIN_EQ_METHOD)+1; } //NOTE THE CORRECTION for the ChoiceSetting index
	
	public double getLinEqMethodParam()
	{ return settings.getDouble(PrismSettings.PRISM_LIN_EQ_METHOD_PARAM); }
	
	public int getTermCrit()
	{ return settings.getInteger(PrismSettings.PRISM_TERM_CRIT)+1; } //NOTE THE CORRECTION for the ChoiceSetting index
	
	public double getTermCritParam()
	{ return settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM); }
	
	public int getMaxIters()
	{ return settings.getInteger(PrismSettings.PRISM_MAX_ITERS); }
	
	public boolean getVerbose()
	{ return settings.getBoolean(PrismSettings.PRISM_VERBOSE); }
	
	public boolean getPrecomp()
	{ return settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION); }
	
	public boolean getFairness()
	{ return settings.getBoolean(PrismSettings.PRISM_FAIRNESS); }
	
	public int getSBMaxMem()
	{ return settings.getInteger(PrismSettings.PRISM_SB_MAX_MEM); }
	
	public int getNumSBLevels()
	{ return settings.getInteger(PrismSettings.PRISM_NUM_SB_LEVELS); }
	
	public int getSORMaxMem()
	{ return settings.getInteger(PrismSettings.PRISM_SOR_MAX_MEM); }
	
	public boolean getDoSSDetect()
	{ return settings.getBoolean(PrismSettings.PRISM_DO_SS_DETECTION); }
	
	public boolean getExtraDDInfo()
	{ return settings.getBoolean(PrismSettings.PRISM_EXTRA_DD_INFO); }
	
	public boolean getExtraReachInfo()
	{ return settings.getBoolean(PrismSettings.PRISM_EXTRA_REACH_INFO); }
	
	public int getNumSORLevels()
	{ return settings.getInteger(PrismSettings.PRISM_NUM_SOR_LEVELS); }
	
	public boolean getCompact()
	{ return settings.getBoolean(PrismSettings.PRISM_COMPACT); }
	
	public long getCUDDMaxMem()
	{ return settings.getInteger(PrismSettings.PRISM_CUDD_MAX_MEM); }
	
	public double getCUDDEpsilon()
	{ return settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON); }
	
	public int getSCCMethod()
	{ return settings.getInteger(PrismSettings.PRISM_SCC_METHOD)+1; } //NOTE THE CORRECTION for the ChoiceSetting index
	
	public int getExportAdv()
	{ return settings.getInteger(PrismSettings.PRISM_EXPORT_ADV)+1; } //NOTE THE CORRECTION for the ChoiceSetting index
	
	public String getExportAdvFilename()
	{ return settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME); }
	
	// Get methods for miscellaneous options
	
	public boolean getExportTarget()
	{return exportTarget; }
	
	public String getExportTargetFilename()
	{ return exportTargetFilename; }
	
	public boolean getDoReach()
	{ return doReach; }
	
	public boolean getBSCCComp()
	{ return bsccComp; }
	
	public int getConstruction()
	{ return construction; }
	
	public int getOrdering()
	{ return ordering; }
	
	public double getSumRoundOff()
	{ return sumRoundOff; }
	
	/**
	 * Get (exclusive) access to the PRISM parser.
	 */
	public static PrismParser getPrismParser() throws InterruptedException
	{
		// Note: this mutex mechanism is based on public domain code by Doug Lea
		if (Thread.interrupted()) throw new InterruptedException();
		// this code is synchronized on the whole Prism class
		// (because this is a static method)
		synchronized (Prism.class) {
			try {
				// wait until parser is free
				while (prismParserInUse) { Prism.class.wait(); }
				// lock parser
				prismParserInUse = true;
				// return parser, creating anew if necessary
				if (thePrismParser == null) thePrismParser = new PrismParser();
				return thePrismParser;
			}
			catch (InterruptedException e) {
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
	
	/**
	 * Let PrismSettings object notify us things have changed
	 */
	public void notifySettings(PrismSettings settings)
	{
		if (cuddStarted)
		{
			JDD.SetCUDDEpsilon(settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON));
			JDD.SetCUDDMaxMem(settings.getInteger(PrismSettings.PRISM_CUDD_MAX_MEM));
		}
	}

	/**
	 * Compare two version numbers of PRISM (strings).
	 * Example ordering: { "1", "2", "2.0", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"}
	 * Returns: 1 if v1>v2, -1 if v1<v2, 0 if v1=v2
	 */
	public static int compareVersions(String v1, String v2)
	{
		String ss1[], ss2[], tmp[];
		int s1 = 0, s2 = 0, i, n;
		boolean s1num, s2num;
		
		// Exactly equal
		if (v1.equals(v2)) return 0;
		// Otherwise split into sections
		ss1 = v1.split("\\.");
		ss2 = v2.split("\\.");
		// Pad if one is shorter
		n = Math.max(ss1.length, ss2.length);
		if (ss1.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss1.length; i++) tmp[i] = ss1[i];
			for (i = ss1.length; i < n; i++) tmp[i] = "";
			ss1 = tmp;
		}
		if (ss2.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss2.length; i++) tmp[i] = ss2[i];
			for (i = ss2.length; i < n; i++) tmp[i] = "";
			ss2 = tmp;
		}
		// Loop through sections of string
		for (i = 0; i < n; i++) {
			// 2.1.beta < 2.1, etc.
			if (ss1[i].matches("beta.*")) {
				if (ss2[i].matches("beta.*")) {
					// 2.1.beta < 2.1.beta2 < 2.1.beta3, etc.
					try { if (ss1[i].length() == 4) s1 = 0; else s1 = Integer.parseInt(ss1[i].substring(4)); } catch (NumberFormatException e) { return 0; }
					try { if (ss2[i].length() == 4) s2 = 0; else s2 = Integer.parseInt(ss2[i].substring(4)); } catch (NumberFormatException e) { return 0; }
					if (s1 == s2) return 0; else return (s1 > s2) ? 1 : -1;
				}
				else return -1;
			}
			if (ss2[i].matches("beta.*")) return 1;
			// 2 < 2.1, etc.
			if (ss1[i].equals("")) { if (ss2[i].equals("")) return 0; else return -1; }
			if (ss2[i].equals("")) return 1;
			// 2.1 < 2.1.dev, etc.
			if (ss1[i].matches("dev.*")) {
				if (ss2[i].matches("dev.*")) {
					// 2.1.dev < 2.1.dev2 < 2.1.dev3, etc.
					try { if (ss1[i].length() == 3) s1 = 0; else s1 = Integer.parseInt(ss1[i].substring(3)); } catch (NumberFormatException e) { return 0; }
					try { if (ss2[i].length() == 3) s2 = 0; else s2 = Integer.parseInt(ss2[i].substring(3)); } catch (NumberFormatException e) { return 0; }
					if (s1 == s2) return 0; else return (s1 > s2) ? 1 : -1;
				}
				else return -1;
			}
			if (ss2[i].matches("dev.*")) return 1;
			// See if strings are integers
			try { s1num = true; s1 = Integer.parseInt(ss1[i]); } catch (NumberFormatException e) { s1num = false; }
			try { s2num = true; s2 = Integer.parseInt(ss2[i]); } catch (NumberFormatException e) { s2num = false; }
			// 2.5 < 3.1, etc. 
			if (s1num && s2num) {
				if (s1 < s2) return -1;
				if (s1 > s2) return 1;
				if (s1 == s2) continue;
			}
		}
		
		return 0;
	}

    /**
	 * Get access to the list of all PRISM language keywords.
	 */
	public static List<String> getListOfKeyords()
	{
		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			try {
				return getPrismParser().getListOfKeywords();
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			return null;
		}
	}
	
	/**
	 * Initialise PRISM.
	 */
	public void initialise() throws PrismException
	{
		mainLog.print("PRISM\n=====\n");
		mainLog.print("\nVersion: " + getVersion() + "\n");
		mainLog.print("Date: " + new java.util.Date() + "\n");
		try {
			String h = java.net.InetAddress.getLocalHost().getHostName();
			mainLog.print("Hostname: " + h + "\n");
		} catch (java.net.UnknownHostException e) {}
		
		// initialise cudd/jdd
		JDD.InitialiseCUDD(getCUDDMaxMem(), getCUDDEpsilon());
		cuddStarted = true;
		JDD.SetOutputStream(techLog.getFilePointer());
		
		// initialise all three engines
		PrismMTBDD.initialise(mainLog, techLog);
		PrismSparse.initialise(mainLog, techLog);
		PrismHybrid.initialise(mainLog, techLog);
		
		// set cudd manager in other packages
		DoubleVector.setCUDDManager();
		ODDUtils.setCUDDManager();
	}
	
	// parse model from file
	
	public ModulesFile parseModelFile(File file) throws FileNotFoundException, PrismLangException { return parseModelFile(file, null); }
	
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
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		
		modulesFile.tidyUp();
		
		return modulesFile;
	}

	// parse model from string
	
	public ModulesFile parseModelString(String s) throws PrismLangException { return parseModelString(s, null); }
	
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
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		
		modulesFile.tidyUp();
		
		return modulesFile;
	}

	// import pepa from file
	
	public ModulesFile importPepaFile(File file) throws PrismException, PrismLangException
	{
		String modelString;
		
		// compile pepa file to string
		try {
			modelString = pepa.compiler.Main.compile("" + file);
		}
		catch (pepa.compiler.InternalError e) {
			throw new PrismException("Could not import PEPA file:\n" + e.getMessage());
		}
		
		// parse string as prism model and return
		return parseModelString(modelString);
	}
	
	// import pepa from string
	
	public ModulesFile importPepaString(String s) throws PrismException, PrismLangException
	{
		File pepaFile = null;
		String modelString;
		
		// create temporary file containing pepa model
		try
		{
			pepaFile = File.createTempFile("tempPepa" + System.currentTimeMillis(), ".pepa");
			FileWriter write = new FileWriter(pepaFile);
			write.write(s);
			write.close();
		}
		catch(IOException e)
		{
			if(pepaFile != null) pepaFile.delete();
			throw new PrismException("Couldn't create temporary file for PEPA conversion");
		}
		
		// compile pepa file to string
		try {
			modelString = pepa.compiler.Main.compile("" + pepaFile);
		}
		catch (pepa.compiler.InternalError e) {
			if(pepaFile != null) pepaFile.delete();
			throw new PrismException("Could not import PEPA file:\n" + e.getMessage());
		}
		
		// parse string as prism model and return
		return parseModelString(modelString);
	}

	// parse properties from file
	// nb: need to pass in modules file to access its constants
	//     but if its null, we just create a blank one for you.
	
	public PropertiesFile parsePropertiesFile(ModulesFile mf, File file) throws FileNotFoundException, PrismLangException
	{ return parsePropertiesFile(mf, file, true); }
	
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
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		
		if (tidy) propertiesFile.tidyUp();
		
		return propertiesFile;
	}

	// parse properties from string
	// nb: need to pass in modules file to access its constants
	//     but if its null, we just create a blank one for you.
	
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
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		
		propertiesFile.tidyUp();
		
		return propertiesFile;
	}

	// parse single expression from string
	
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
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		
		return expr;
	}

	// parse for loop from string
	
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
			}
			finally {
				// release prism parser
				releasePrismParser();
			}
		}
		catch(InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		
		return fl;
	}

	// build model, i.e. convert ModulesFile to Model
	// optionally pass in a short message to identify the model being built
	// (typically the values of undefined constants)
	
	public Model buildModel(ModulesFile modulesFile) throws PrismException
	{
		return buildModel(modulesFile, "");
	}
	
	public Model buildModel(ModulesFile modulesFile, String msg) throws PrismException
	{
		long l; // timer
		Model model;
		
		mainLog.print("\nBuilding model");
		if (msg != null) if (msg.length() > 0) mainLog.print(" (" + msg + ")");
		mainLog.print("...\n");
		
		// create translator
		mod2mtbdd = new Modules2MTBDD(this, modulesFile);
		
		// build model
		l = System.currentTimeMillis();
		model = mod2mtbdd.translate();
		l = System.currentTimeMillis() - l;
		
		mainLog.println("\nTime for model construction: " + l/1000.0 + " seconds.");
		
		return model;
	}
	
	/**
	 * Builds a model from files containing an explicit list of transitions/etc.
	 * This is step 1 of the process; buildExplicitModel(...) should be called subsequently.
	 * @param statesFile File containing the list of states (optional, can be null)
	 * @param transFile File containing the list of transitions (required)
	 * @param typeOverride Type of model to be built (optional, use null if not required)
	 * @throws PrismException
	 */
	public ModulesFile parseExplicitModel(File statesFile, File transFile, File labelsFile, ModelType typeOverride) throws PrismException
	{
		// create Explicit2MTBDD object
		exp2mtbdd = new Explicit2MTBDD(this, statesFile, transFile, labelsFile, typeOverride);
		
		// build state space
		return exp2mtbdd.buildStates();
	}
	
	/**
	 * Builds a model from files containing an explicit list of transitions/etc.
	 * This is step 2 of the process; parseExplicitModel(...) should have been called first.
	 */
	public Model buildExplicitModel() throws PrismException
	{
		long l; // timer
		Model model;
		
		// check Explicit2MTBDD object created
		if (exp2mtbdd == null) throw new PrismException("Explicit2MTBDD object never created");
		
		mainLog.print("\nBuilding model...\n");
		
		// build model
		l = System.currentTimeMillis();
		model = exp2mtbdd.buildModel();
		l = System.currentTimeMillis() - l;
		
		mainLog.println("\nTime for model construction: " + l/1000.0 + " seconds.");
		
		return model;
	}
	
	// export trans to a spy file
	
	public void exportToSpyFile(Model model, File file) throws FileNotFoundException
	{
		int depth;
		JDDNode tmp;
		
		mainLog.println("\nExporting to spy file \"" + file + "\"...");
		
		// choose depth
		depth = model.getAllDDRowVars().n();
		if (depth > 9) depth = 9;
		
		// get rid of non det vars if necessary
		tmp = model.getTrans();
		JDD.Ref(tmp);
		if (model.getModelType() == ModelType.MDP)
		{
			tmp = JDD.MaxAbstract(tmp, ((NondetModel)model).getAllDDNondetVars());
		}
		
		// export to spy file
		JDD.ExportMatrixToSpyFile(tmp, model.getAllDDRowVars(), model.getAllDDColVars(), depth, file.getPath());
		JDD.Deref(tmp);
	}
	
	// export trans to a dot file
	
	public void exportToDotFile(Model model, File file) throws FileNotFoundException
	{
		mainLog.println("\nExporting to dot file \"" + file + "\"...");
		
		// export to dot file
		JDD.ExportDDToDotFileLabelled(model.getTrans(), file.getPath(), model.getDDVarNames());
	}
	
	// alias for export transition matrix to a file (plain, matlab, ...)
	
	public void exportToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{ exportTransToFile(model, ordered, exportType, file); }
	
	// export transition matrix to a file (plain, matlab, ...)
	
	public void exportTransToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		// can only do ordered version of export for MDPs
		if (model.getModelType() == ModelType.MDP) {
			if (!ordered) mainLog.println("\nWarning: Cannot export unordered transition matrix for MDPs; using ordered.");
			ordered = true;
		}
		// can only do ordered version of export for MRMC
		if (exportType == EXPORT_MRMC) {
			if (!ordered) mainLog.println("\nWarning: Cannot export unordered transition matrix in MRMC format; using ordered.");
			ordered = true;
		}
		// can only do ordered version of export for rows format
		if (exportType == EXPORT_ROWS) {
			if (!ordered) mainLog.println("\nWarning: Cannot export unordered transition matrix in rows format; using ordered.");
			ordered = true;
		}
		
		// print message
		mainLog.print("\nExporting transition matrix ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		case EXPORT_DOT: mainLog.print("in Dot format "); break;
		case EXPORT_MRMC: mainLog.print("in MRMC format "); break;
		case EXPORT_ROWS: mainLog.print("in rows format "); break;
		case EXPORT_DOT_STATES: mainLog.print("in Dot format (with states) "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// do export
		model.exportToFile(exportType, ordered, file);
		
		// for export to dotm with states, need to do a bit more
		if (exportType == EXPORT_DOT_STATES) {
			// open (appending to) existing new file log or use main log
			PrismLog tmpLog;
			if (file != null) {
				tmpLog = new PrismFileLog(file.getPath(), true);
				if (!tmpLog.ready()) {
					throw new FileNotFoundException();
				}
			} else {
				tmpLog = mainLog;
			}
			// insert states info into dot file
			model.getReachableStates().printDot(tmpLog);
			// print footer
			tmpLog.println("}");
			// tidy up
			if (file != null) tmpLog.close();
		}
	}

	// export state rewards to a file (plain, matlab, ...)
	
	public void exportStateRewardsToFile(Model model, int exportType, File file) throws FileNotFoundException, PrismException
	{
		String s;
		
		// rows format does not apply to vectors
		if (exportType == EXPORT_ROWS) exportType = EXPORT_PLAIN;
		
		// print message
		mainLog.print("\nExporting state rewards vector ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		case EXPORT_MRMC: mainLog.print("in MRMC format "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// do export
		s = model.exportStateRewardsToFile(exportType, file);
		if (s != null) mainLog.println("Rewards exported to files: "+s);
	}

	// export transition rewards to a file (plain, matlab, ...)
	
	public void exportTransRewardsToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		String s;
		
		// can only do ordered version of export for MDPs
		if (model.getModelType() == ModelType.MDP) {
			if (!ordered) mainLog.println("\nWarning: Cannot export unordered transition reward matrix for MDPs; using ordered");
			ordered = true;
		}
		// can only do ordered version of export for MRMC
		if (exportType == EXPORT_MRMC) {
			if (!ordered) mainLog.println("\nWarning: Cannot export unordered transition reward matrix in MRMC format; using ordered");
			ordered = true;
		}
		// can only do ordered version of export for rows format
		if (exportType == EXPORT_ROWS) {
			if (!ordered) mainLog.println("\nWarning: Cannot export unordered transition matrix in rows format; using ordered.");
			ordered = true;
		}
		
		// print message
		mainLog.print("\nExporting transition rewards matrix ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		case EXPORT_MRMC: mainLog.print("in MRMC format "); break;
		case EXPORT_ROWS: mainLog.print("in rows format "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// do export
		s = model.exportTransRewardsToFile(exportType, ordered, file);
		if (s != null) mainLog.println("Rewards exported to files: "+s);
	}

	// export states list to a file (plain, matlab, ...)
	// file == null mean export to log
	
	public void exportBSCCsToFile(Model model, int exportType, File file) throws FileNotFoundException
	{
		int i, n;
		long l; // timer
		PrismLog tmpLog;
		SCCComputer sccComputer;
		Vector<JDDNode> bsccs;
		JDDNode not, bscc;
		
		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC) exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS) exportType = EXPORT_PLAIN;
		
		// Compute BSCCs
		mainLog.println("\nComputing BSCCs...");
		sccComputer = getSCCComputer(model);
		l = System.currentTimeMillis();
		sccComputer.computeBSCCs();
		l = System.currentTimeMillis() - l;
		bsccs = sccComputer.getVectBSCCs();
		not = sccComputer.getNotInBSCCs();
		mainLog.println("\nTime for BSCC computation: " + l/1000.0 + " seconds.");
		
		// print message
		mainLog.print("\nExporting BSCCs ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (file != null) {
			tmpLog = new PrismFileLog(file.getPath());
			if (!tmpLog.ready()) {
				throw new FileNotFoundException();
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print header: list of model vars
		if (exportType == EXPORT_MATLAB) tmpLog.print("% ");
		tmpLog.print("Variables: (");
		for (i = 0; i < model.getNumVars(); i++) {
			tmpLog.print(model.getVarName(i));
			if (i < model.getNumVars()-1) tmpLog.print(",");
		}
		tmpLog.println(")");
		
		// print states for each bscc
		n = bsccs.size();
		for (i = 0; i < n; i++) {
			tmpLog.println();
			if (exportType == EXPORT_MATLAB) tmpLog.print("% ");
			tmpLog.println("BSCC "+(i+1)+"/"+n+":");
			if (exportType == EXPORT_MATLAB) tmpLog.println("bscc"+(i+1)+"=[");
			bscc = bsccs.get(i);
			if (exportType != EXPORT_MATLAB)
				new StateListMTBDD(bscc, model).print(tmpLog);
			else
				new StateListMTBDD(bscc, model).printMatlab(tmpLog);
			if (exportType == EXPORT_MATLAB) tmpLog.println("];");
			JDD.Deref(bscc);
		}
		
		JDD.Deref(not);
		
		// tidy up
		if (file != null) tmpLog.close();
	}

	// export labels and satisfying states to a file
	// file == null mean export to log
	
	public void exportLabelsToFile(Model model, ModulesFile modulesFile, PropertiesFile propertiesFile, int exportType, File file) throws FileNotFoundException, PrismException
	{
		int i, n;
		LabelList ll;
		Expression expr;
		Values constantValues;
		StateModelChecker mc = null;
		JDDNode dd, labels[];
		String labelNames[];
		
		// get label list and size
		if (propertiesFile == null) {
			ll = modulesFile.getLabelList();
			n = ll.size();
		} else {
			ll = propertiesFile.getCombinedLabelList();
			n = ll.size();
		}
		
		// print message
		mainLog.print("\nExporting labels and satisfying states ");
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// convert labels to bdds
		if (n > 0) {
			constantValues = new Values();
			constantValues.addValues(model.getConstantValues());
			if (propertiesFile != null) constantValues.addValues(propertiesFile.getConstantValues());
			mc = new StateModelChecker(this, model, null);
		}
		labels = new JDDNode[n+2];
		labels[0] = model.getStart();
		labels[1] = model.getFixedDeadlocks();
		for (i = 0; i < n; i++) {
			expr = ll.getLabel(i);
			dd = mc.checkExpressionDD(expr);
			labels[i+2] = dd;
		}
		// put names for labels in an array
		labelNames = new String[n+2];
		labelNames[0] = "init";
		labelNames[1] = "deadlock";
		for (i = 0; i < n; i++) {
			labelNames[i+2] = ll.getLabelName(i);
		}
		
		// export them to a file
		PrismMTBDD.ExportLabels(labels, labelNames, "l", model.getAllDDRowVars(), model.getODD(), exportType, (file != null)?file.getPath():null);
		
		// deref dds
		for (i = 0; i < n; i++) {
			JDD.Deref(labels[i+2]);
		}
	}

	public void exportStatesToFile(Model model, int exportType, File file) throws FileNotFoundException
	{
		int i;
		PrismLog tmpLog;
		
		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC) exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS) exportType = EXPORT_PLAIN;
		
		// print message
		mainLog.print("\nExporting list of reachable states ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (file != null) {
			tmpLog = new PrismFileLog(file.getPath());
			if (!tmpLog.ready()) {
				throw new FileNotFoundException();
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print header: list of model vars
		if (exportType == EXPORT_MATLAB) tmpLog.print("% ");
		tmpLog.print("(");
		for (i = 0; i < model.getNumVars(); i++) {
			tmpLog.print(model.getVarName(i));
			if (i < model.getNumVars()-1) tmpLog.print(",");
		}
		tmpLog.println(")");
		if (exportType == EXPORT_MATLAB) tmpLog.println("states=[");
		
		// print states
		if (exportType != EXPORT_MATLAB)
			model.getReachableStates().print(tmpLog);
		else
			model.getReachableStates().printMatlab(tmpLog);
		
		// print footer
		if (exportType == EXPORT_MATLAB) tmpLog.println("];");
		
		// tidy up
		if (file != null) tmpLog.close();
	}
	
	/**
	 * Perform model checking of a property on a model and return result.
	 * @param model The model (DTMC, CTMC or MDP)
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property
	 */
	public Result modelCheck(Model model, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		Result res;
		
		// Check that property is valid for this model type
		// and create new model checker object
		expr.checkValid(model.getModelType());
		switch (model.getModelType()) {
		case DTMC:
			mc = new ProbModelChecker(this, model, propertiesFile);
			break;
		case MDP:
			mc = new NondetModelChecker(this, model, propertiesFile);
			break;
		case CTMC:
			mc = new StochModelChecker(this, model, propertiesFile);
			break;
		default:
			throw new PrismException("Unknown model type"+model.getModelType());
		}
		
		// Do model checking
		res = mc.check(expr);
		
		// Return result
		return res;
	}
	
	/**
	 * Perform model checking of a property on a PTA model and return result.
	 * @param modulesFile The PTA model (language-level)
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param expr The property
	 */
	public Result modelCheckPTA(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		PTAModelChecker mcPta;
		Result res;
		
		// Check that property is valid for this model type
		// and create new model checker object
		expr.checkValid(modulesFile.getModelType());
		mcPta = new PTAModelChecker(this, modulesFile, propertiesFile);
		// Do model checking
		res = mcPta.check(expr);
		
		return res;
	}
	
	// compute parameters for simulation
	
	public double computeSimulationApproximation(double confid, int numSamples)
	{
		return getSimulator().computeSimulationApproximation(confid, numSamples);
	}
	public double computeSimulationConfidence(double approx, int numSamples)
	{
		return getSimulator().computeSimulationConfidence(approx, numSamples);
	}
	public int computeSimulationNumSamples(double approx, double confid)
	{
		return getSimulator().computeSimulationNumSamples(approx, confid);
	}

	// check if a property is suitable for analysis with the simulator
	
	public void checkPropertyForSimulation(Expression expr) throws PrismException
	{
		getSimulator().checkPropertyForSimulation(expr);
	}
	
	// model check using simulator
	// returns result or throws an exception in case of error
	
	public Result modelCheckSimulator(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr, Values initialState, int noIterations, int maxPathLength) throws PrismException
	{
		Object res = null;
		
		// Check that property is valid for this model type
		expr.checkValid(modulesFile.getModelType());
		
		// Do model checking
		res = getSimulator().modelCheckSingleProperty(modulesFile, propertiesFile, expr, new State(initialState), noIterations, maxPathLength);
		
		return new Result(res);
	}
	
	// model check using simulator (several properties simultaneously)
	// returns an array of results, some of which may be Exception objects if there were errors
	// in the case of an error which affects all properties, an exception is thrown
	
	public Result[] modelCheckSimulatorSimultaneously(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Expression> exprs, Values initialState, int noIterations, int maxPathLength) throws PrismException
	{
		Object[] res = null;
		
		// Check that properties are valid for this model type
		for (Expression expr : exprs)
			expr.checkValid(modulesFile.getModelType());
		
		// Do model checking
		res = getSimulator().modelCheckMultipleProperties(modulesFile, propertiesFile, exprs, new State(initialState), noIterations, maxPathLength);
		
		Result[] resArray = new Result[res.length];
		for (int i = 0; i < res.length; i++) resArray[i] = new Result(res[i]);
		return resArray;
	}
	
	// model check using simulator (ranging over some constants - from properties file only)
	// results are stored in the ResultsCollection, some of which may be Exception objects if there were errors
	// in the case of an error which affects all properties, an exception is thrown
	
	public void modelCheckSimulatorExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile, UndefinedConstants undefinedConstants, ResultsCollection results, Expression propertyToCheck, Values initialState, int noIterations, int pathLength) throws PrismException, InterruptedException
	{
		getSimulator().modelCheckExperiment(modulesFile, propertiesFile, undefinedConstants, results, propertyToCheck, new State(initialState), pathLength, noIterations);
	}
	
	/**
	 * Generate a random path through the model using the simulator.
	 * @param modulesFile The model
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param details Information about the path to be generated
	 * @param file File to output the path to (stdout if null)
	 */
	public void generateSimulationPath(ModulesFile modulesFile, String details, int maxPathLength, File file) throws PrismException, PrismLangException
	{
		GenerateSimulationPath genPath = new GenerateSimulationPath(getSimulator(), mainLog);
		genPath.generateSimulationPath(modulesFile, modulesFile.getInitialValues(), details, maxPathLength, file);
	}
	
	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC).
	 * Output probability distribution to log. 
	 */
	public void doSteadyState(Model model) throws PrismException
	{
		doSteadyState(model, EXPORT_PLAIN, null);
	}
	
	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 */
	public void doSteadyState(Model model, int exportType, File fileOut) throws PrismException
	{
		long l = 0; // timer
		StateValues probs = null;
		PrismLog tmpLog;
		
		if (fileOut != null && getEngine() == MTBDD)
			throw new PrismException("Transient probability export only supported for sparse/hybrid engines");
		
		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC) exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS) exportType = EXPORT_PLAIN;
		
		mainLog.println("\nComputing steady-state probabilities...");
		l = System.currentTimeMillis();

		if (model.getModelType() == ModelType.DTMC) {
			mc = new ProbModelChecker(this, model, null);
			probs = ((ProbModelChecker)mc).doSteadyState();
		}
		else if (model.getModelType() == ModelType.CTMC) {
			mc = new StochModelChecker(this, model, null);
			probs = ((StochModelChecker)mc).doSteadyState();
		}
		else {
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		}
		
		l = System.currentTimeMillis() - l;
		
		// print message
		mainLog.print("\nPrinting steady-state probabilities ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (fileOut != null) mainLog.println("to file \"" + fileOut + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (fileOut != null) {
			tmpLog = new PrismFileLog(fileOut.getPath());
			if (!tmpLog.ready()) {
				throw new PrismException("Could not open file \"" + fileOut + "\" for output");
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print out or export probabilities
		probs.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null);
		
		// print out computation time
		mainLog.println("\nTime for steady-state probability computation: " + l/1000.0 + " seconds.");
		
		// tidy up
		probs.clear();
		if (fileOut != null) tmpLog.close();
	}
	
	/**
	 * Compute transient probabilities (for a DTMC or CTMC).
	 * Output probability distribution to log. 
	 */
	public void doTransient(Model model, double time) throws PrismException
	{
		doTransient(model, time, EXPORT_PLAIN, null, null);
	}
	
	/**
	 * Compute transient probabilities (for a DTMC or CTMC).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doTransient(Model model, double time, int exportType, File fileOut, File fileIn) throws PrismException
	{
		long l = 0; // timer
		StateValues probs = null;
		PrismLog tmpLog;
		
		if (time < 0) throw new PrismException("Cannot compute transient probabilities for negative time value");
		
		if (fileOut != null && getEngine() == MTBDD)
			throw new PrismException("Transient probability export only supported for sparse/hybrid engines");
		
		// no specific states format for MRMC
		if (exportType == EXPORT_MRMC) exportType = EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == EXPORT_ROWS) exportType = EXPORT_PLAIN;
		
		// create new model checker object
		mc = new StochModelChecker(this, model, null);
		
		l = System.currentTimeMillis();

		if (model.getModelType() == ModelType.DTMC) {
			mainLog.println("\nComputing transient probabilities (time = " + (int)time + ")...");
			mc = new ProbModelChecker(this, model, null);
			probs = ((ProbModelChecker)mc).doTransient((int)time, fileIn);
		}
		else if (model.getModelType() == ModelType.CTMC) {
			mainLog.println("\nComputing transient probabilities (time = " + time + ")...");
			mc = new StochModelChecker(this, model, null);
			probs = ((StochModelChecker)mc).doTransient(time, fileIn);
		}
		else {
			throw new PrismException("Transient probabilities only computed for DTMCs/CTMCs");
		}
		
		l = System.currentTimeMillis() - l;
		
		// print message
		mainLog.print("\nPrinting transient probabilities ");
		switch (exportType) {
		case EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (fileOut != null) mainLog.println("to file \"" + fileOut + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (fileOut != null) {
			tmpLog = new PrismFileLog(fileOut.getPath());
			if (!tmpLog.ready()) {
				throw new PrismException("Could not open file \"" + fileOut + "\" for output");
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print out or export probabilities
		probs.print(tmpLog, fileOut == null, exportType == EXPORT_MATLAB, fileOut == null);
		
		// print out computation time
		mainLog.println("\nTime for transient probability computation: " + l/1000.0 + " seconds.");

		// tidy up
		probs.clear();
		if (fileOut != null) tmpLog.close();
	}
	
	// clear up and close down
	
	public void closeDown()
	{ closeDown(true); }
	public void closeDown(boolean check)
	{
		// close down engines
		PrismMTBDD.closeDown();
		PrismSparse.closeDown();
		PrismHybrid.closeDown();
		// close down jdd/cudd
		if (cuddStarted)
		{
			JDD.CloseDownCUDD(check);
		}
	}
	
	// Access method for the settings
	
	public PrismSettings getSettings()
	{
		return settings;
	}

	public void setCheckZeroLoops(boolean checkZeroLoops)
	{
		this.checkZeroLoops  = checkZeroLoops;		
	}

	public boolean getCheckZeroLoops()
	{
		return this.checkZeroLoops;
	}		
}

//------------------------------------------------------------------------------
