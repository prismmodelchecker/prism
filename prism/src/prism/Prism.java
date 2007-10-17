//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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
import java.util.Vector;
import java.util.ArrayList;

import jdd.*;
import dv.*;
import odd.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.*;
import simulator.*;

// prism class - main class for model checker
// (independent of user interface (command line or gui))

public class Prism implements PrismSettingsListener
{
	// prism version
	private static String version = "3.1.1.dev";
	
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
	
	//------------------------------------------------------------------------------
	// Settings / flags / options
	//------------------------------------------------------------------------------
	
	// Main PRISM settings
	private PrismSettings settings;
	
	// A few miscellaneous options (i.e. defunct/hidden/undocumented/etc.)
	
	private boolean doReach;	// do reachability? (sometimes might want to skip it)
	private boolean bsccComp;	// do bscc computation before steady-state?
	
	// mtbdd construction method
	//  1 - use with ordering 1: nondet vars form a tree at the top
	//  3 - use with ordering 2: zero for nonexistant bits
	// nb: option 2 removed because it was stupid
	private int construction;

	// mtbdd variable ordering
	//  1 - (s ... s) (l ... l) (r c ... r c)
	//  2 - (s l ... l r c ... r c) (s l ... l r c ... r c) ...
	private int ordering;
	
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
	
	// constructor
	
	public Prism(PrismLog l1, PrismLog l2)
	{
		// set up logs
		mainLog = l1;
		techLog = l2;
		
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
		doReach = true;
		bsccComp = true;
		construction = 0;
		ordering = 1;
	}
	
	// set methods
	
	public void setMainLog(PrismLog l)
	{
		// store new log
		mainLog = l;
		// pass to other components
		PrismMTBDD.setMainLog(mainLog);
		PrismSparse.setMainLog(mainLog);
		PrismHybrid.setMainLog(mainLog);
	}
	
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
	
	// set methods for main prism settings
	// provided for convenience and for compatability with old code
	
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
	
	// set methods for miscellaneous options
	
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
	
	// get methods
	
	public static String getVersion()
	{ return version; }
	
	public PrismLog getMainLog()
	{ return mainLog; }
	
	public PrismLog getTechLog()
	{ return techLog; }
	
	// get methods for main prism settings
	// as above, provided for convenience and for compatability with old code
	
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
	
	public int getNumSORLevels()
	{ return settings.getInteger(PrismSettings.PRISM_NUM_SOR_LEVELS); }
	
	public boolean getCompact()
	{ return settings.getBoolean(PrismSettings.PRISM_COMPACT); }
	
	public long getCUDDMaxMem()
	{ return settings.getInteger(PrismSettings.PRISM_CUDD_MAX_MEM); }
	
	public double getCUDDEpsilon()
	{ return settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON); }
	
	// get methods for miscellaneous options
	
	public boolean getDoReach()
	{ return doReach; }
	
	public boolean getBSCCComp()
	{ return bsccComp; }
	
	public int getConstruction()
	{ return construction; }
	
	public int getOrdering()
	{ return ordering; }
	
	// get/release (exclusive) access to the prism parser
	
	// note: this mutex mechanism is based on public domain code by Doug Lea
	
	public static PrismParser getPrismParser() throws InterruptedException
	{
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
	
	public static synchronized void releasePrismParser()
	{
		prismParserInUse = false;
		Prism.class.notify(); 
	}

	// get Simulator object, creating if necessary
	
	public SimulatorEngine getSimulator()
	{
		if (theSimulator == null) {
			theSimulator = new SimulatorEngine();
			theSimulator.setMainLog(mainLog);
		}
		return theSimulator; 
	}

	// Let PrismSettings object notify us things have changed
	
	public void notifySettings(PrismSettings settings)
	{
		if (cuddStarted)
		{
			JDD.SetCUDDEpsilon(settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON));
			JDD.SetCUDDMaxMem(settings.getInteger(PrismSettings.PRISM_CUDD_MAX_MEM));
		}
	}

	// Compare two version numbers of PRISM (strings)
	// Returns: 1 if v1>v2, -1 if v1<v2, 0 if v1=v2
	// Example ordering: { "1", "2", "2.0", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"}
	
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

	// initialise
	
	public void initialise() throws PrismException
	{
		mainLog.print("PRISM\n=====\n");
		mainLog.print("\nVersion: " + version + "\n");
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
	
	public ModulesFile parseModelFile(File file) throws FileNotFoundException, ParseException, PrismException { return parseModelFile(file, 0); }
	
	public ModulesFile parseModelFile(File file, int typeOverride) throws FileNotFoundException, ParseException, PrismException
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
			throw new ParseException("Concurrency error in parser");
		}
		
		modulesFile.tidyUp();
		
		return modulesFile;
	}

	// parse model from string
	
	public ModulesFile parseModelString(String s) throws ParseException, PrismException { return parseModelString(s, 0); }
	
	public ModulesFile parseModelString(String s, int typeOverride) throws ParseException, PrismException
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
			throw new ParseException("Concurrency error in parser");
		}
		
		modulesFile.tidyUp();
		
		return modulesFile;
	}

	// import pepa from file
	
	public ModulesFile importPepaFile(File file) throws ParseException, PrismException
	{
		ModulesFile modulesFile;
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
	
	public ModulesFile importPepaString(String s) throws ParseException, PrismException
	{
		File pepaFile = null;
		ModulesFile modulesFile;
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

	// parse pctl properties from file
	// nb: need to pass in modules file to access its constants
	//     but if its null, we just create a blank one for you.
	
	public PropertiesFile parsePropertiesFile(ModulesFile mf, File file) throws FileNotFoundException, ParseException, PrismException
	{ return parsePropertiesFile(mf, file, true); }
	
	public PropertiesFile parsePropertiesFile(ModulesFile mf, File file, boolean tidy) throws FileNotFoundException, ParseException, PrismException
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
			throw new ParseException("Concurrency error in parser");
		}
		
		if (tidy) propertiesFile.tidyUp();
		
		return propertiesFile;
	}

	// parse pctl properties from string
	// nb: need to pass in modules file to access its constants
	//     but if its null, we just create a blank one for you.
	
	public PropertiesFile parsePropertiesString(ModulesFile mf, String s) throws ParseException, PrismException
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
			throw new ParseException("Concurrency error in parser");
		}
		
		propertiesFile.tidyUp();
		
		return propertiesFile;
	}

	// parse single expression from string
	
	public Expression parseSingleExpressionString(String s) throws ParseException, PrismException
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
			throw new ParseException("Concurrency error in parser");
		}
		
		return expr;
	}

	// parse for loop from string
	
	public ForLoop parseForLoopString(String s) throws ParseException, PrismException
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
			throw new ParseException("Concurrency error in parser");
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
		mod2mtbdd = new Modules2MTBDD(mainLog, techLog, modulesFile);
		mod2mtbdd.setOption("ordering", getOrdering());
		mod2mtbdd.setOption("construction", getConstruction());
		mod2mtbdd.setOption("doreach", getDoReach());
		mod2mtbdd.setOption("doprobchecks", getDoProbChecks());
		
		// build model
		l = System.currentTimeMillis();
		model = mod2mtbdd.translate();
		l = System.currentTimeMillis() - l;
		
		mainLog.println("\nTime for model construction: " + l/1000.0 + " seconds.");
		
		return model;
	}
	
	public ModulesFile parseExplicitModel(File statesFile, File transFile, int typeOverride, String initString) throws PrismException
	{
		// create Explicit2MTBDD object
		exp2mtbdd = new Explicit2MTBDD(mainLog, techLog, statesFile, transFile, typeOverride, initString);
		exp2mtbdd.setOption("doreach", getDoReach());
		
		// build state space
		return exp2mtbdd.buildStates();
	}
	
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
		if (model instanceof NondetModel)
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
	
	public void exportToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException
	{ exportTransToFile(model, ordered, exportType, file); }
	
	// export transition matrix to a file (plain, matlab, ...)
	
	public void exportTransToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException
	{
		// can only do ordered version of export for MDPs
		if (model instanceof NondetModel) {
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
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// do export
		model.exportToFile(exportType, ordered, file);
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
		if (model instanceof NondetModel) {
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

	// export labels and satisfying states to a file
	// file == null mean export to log
	
	public void exportLabelsToFile(Model model, PropertiesFile propertiesFile, int exportType, File file) throws FileNotFoundException, PrismException
	{
		int i, n;
		LabelList ll;
		Expression expr;
		Values constantValues;
		Expression2MTBDD expr2mtbdd = null;
		JDDNode dd, labels[];
		String labelNames[];
		
		// get label list and size
		if (propertiesFile == null) {
			ll = null;
			n = 0;
		} else {
			ll = propertiesFile.getLabelList();
			n = ll.size();
		}
		
		// print message
		mainLog.print("\nExporting labels and satisfying states ");
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// convert labels to bdds
		if (n > 0) {
			constantValues = new Values();
			constantValues.addValues(model.getConstantValues());
			constantValues.addValues(propertiesFile.getConstantValues());
			expr2mtbdd = new Expression2MTBDD(mainLog, techLog, model.getVarList(), model.getVarDDRowVars(), constantValues);
			expr2mtbdd.setFilter(model.getReach());
		}
		labels = new JDDNode[n+2];
		labels[0] = model.getStart();
		labels[1] = model.getFixedDeadlocks();
		for (i = 0; i < n; i++) {
			expr = ll.getLabel(i);
			dd = expr2mtbdd.translateExpression(expr);
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

	// model checking
	// returns result or throws an exception in case of error
	
	public Object modelCheck(Model model, PropertiesFile propertiesFile, PCTLFormula f) throws PrismException
	{
		Object res;
		
		// check that formula is valid pctl/csl
		if (model instanceof ProbModel || model instanceof NondetModel)
		{
			f.checkValidPCTL();
		}
		if (model instanceof StochModel)
		{
			f.checkValidCSL();
		}
		
		// create new model checker object
		if (model instanceof ProbModel)
		{
			mc = new ProbModelChecker(mainLog, techLog, model, propertiesFile);
		}
		else if (model instanceof NondetModel)
		{
			mc = new NondetModelChecker(mainLog, techLog, model, propertiesFile);
		}
		else if (model instanceof StochModel)
		{
			mc = new StochModelChecker(mainLog, techLog, model, propertiesFile);
		}
		else
		{
			throw new PrismException("Unknown model type");
		}
		
		// configure model checker
		mc.setEngine(getEngine());
		if (!(model instanceof NondetModel))
		{
			mc.setOption("lineqmethod", getLinEqMethod());
			mc.setOption("lineqmethodparam", getLinEqMethodParam());
		}
		mc.setOption("termcrit", getTermCrit());
		mc.setOption("termcritparam", getTermCritParam());
		mc.setOption("maxiters", getMaxIters());
		mc.setOption("verbose", getVerbose());
		mc.setOption("precomp", getPrecomp());
		if (model instanceof StochModel)
		{
			mc.setOption("bscccomp", getBSCCComp());
			mc.setOption("dossdetect", getDoSSDetect());
		}
		if (model instanceof NondetModel)
		{
			mc.setOption("fairness", getFairness());
		}
		if (getEngine() == HYBRID || getEngine() == SPARSE)
		{
			mc.setOption("compact", getCompact());
		}
		if (getEngine() == HYBRID)
		{
			mc.setOption("sbmaxmem", getSBMaxMem());
			mc.setOption("numsblevels", getNumSBLevels());
		}
		if ((getEngine() == HYBRID) && !(model instanceof NondetModel))
		{
			mc.setOption("sormaxmem", getSORMaxMem());
			mc.setOption("numsorlevels", getNumSORLevels());
		}
		
		//		// print out which engine we are using
		//		mainLog.print("\nEngine: ");
		//		switch (engine) {
		//		case MTBDD: mainLog.println("MTBDD"); break;
		//		case SPARSE: mainLog.println("Sparse"); break;
		//		case HYBRID: mainLog.println("Hybrid"); break;
		//		default: mainLog.print("?????"); break;
		//		}
		
		// do model checking
		res = mc.check(f);
		
		// return result
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
	
	public void checkPropertyForSimulation(ModulesFile modulesFile, PropertiesFile propertiesFile, PCTLFormula f) throws PrismException
	{
		try {
			getSimulator().checkPropertyForSimulation(modulesFile, propertiesFile, f);
		}
		catch (SimulatorException e) {
			throw new PrismException(e.getMessage());
		}
	}
	
	// model check using simulator
	// returns result or throws an exception in case of error
	
	public Object modelCheckSimulator(ModulesFile modulesFile, PropertiesFile propertiesFile, PCTLFormula f, Values initialState, int noIterations, int maxPathLength) throws PrismException
	{
		Object res = null;
		
		//do model checking
		try
		{
			res = getSimulator().modelCheckSingleProperty(modulesFile, propertiesFile, f, initialState, noIterations, maxPathLength);
		}
		catch(SimulatorException e)
		{
			throw new PrismException(e.getMessage());
		}
		
		return res;
	}
	
	// model check using simulator (several properties simultaneously)
	// returns an array of results, some of which may be Exception objects if there were errors
	// in the case of an error which affects all properties, an exception is thrown
	
	public Object[] modelCheckSimulatorSimultaneously(ModulesFile modulesFile, PropertiesFile propertiesFile, ArrayList fs, Values initialState, int noIterations, int maxPathLength) throws PrismException
	{
		Object[] res = null;
		
		try
		{
			res = getSimulator().modelCheckMultipleProperties(modulesFile, propertiesFile, fs, initialState, noIterations, maxPathLength);
		}
		catch(SimulatorException e)
		{
			throw new PrismException(e.getMessage());
		}
		
		return res;
	}
	
	// model check using simulator (ranging over some constants - from properties file only)
	// results are stored in the ResultsCollection, some of which may be Exception objects if there were errors
	// in the case of an error which affects all properties, an exception is thrown
	
	public void modelCheckSimulatorExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile, UndefinedConstants undefinedConstants, ResultsCollection results, PCTLFormula propertyToCheck, Values initialState, int noIterations, int pathLength) throws PrismException, InterruptedException
	{
		try
		{
			getSimulator().modelCheckExperiment(modulesFile, propertiesFile, undefinedConstants, results, propertyToCheck, initialState, pathLength, noIterations);
		}
		catch(SimulatorException e)
		{
			throw new PrismException(e.getMessage());
		}
		catch(PrismException e)
		{
			throw e;
		}
	}
	
	// generate a random path through the model using the simulator
	
	public void generateSimulationPath(ModulesFile modulesFile, String details, int maxPathLength, File file) throws PrismException
	{
		// do path generation
		try {
			getSimulator().generateSimulationPath(modulesFile, modulesFile.getInitialValues(), details, maxPathLength, file);
		}
		catch (SimulatorException e) {
			throw new PrismException(e.getMessage());
		}
	}
	
	// do steady state
	
	public void doSteadyState(Model model) throws PrismException
	{
		long l = 0; // timer
		StateProbs probs = null;
		
		mainLog.println("\nComputing steady-state probabilities...");
		
		// create/configure new model checker object
		mc = new StochModelChecker(mainLog, techLog, model, null);
		switch (getEngine())
		{
			case MTBDD: mc.setEngine(MTBDD); break;
			case SPARSE: mc.setEngine(SPARSE); break;
			case HYBRID: mc.setEngine(HYBRID); break;
			default: break;
		}
		mc.setOption("lineqmethod", getLinEqMethod());
		mc.setOption("lineqmethodparam", getLinEqMethodParam());
		mc.setOption("termcrit", getTermCrit());
		mc.setOption("termcritparam", getTermCritParam());
		mc.setOption("maxiters", getMaxIters());
		mc.setOption("verbose", getVerbose());
		mc.setOption("bscccomp", getBSCCComp());
		if (getEngine() == HYBRID || getEngine() == SPARSE)
		{
			mc.setOption("compact", getCompact());
		}
		if (getEngine() == HYBRID)
		{
			mc.setOption("sbmaxmem", getSBMaxMem());
			mc.setOption("numsblevels", getNumSBLevels());
			mc.setOption("sormaxmem", getSORMaxMem());
			mc.setOption("numsorlevels", getNumSORLevels());
		}
		
		//		// print out which engine we are using
		//		mainLog.print("\nEngine: ");
		//		switch (engine) {
		//		case MTBDD: mainLog.println("MTBDD"); break;
		//		case SPARSE: mainLog.println("Sparse"); break;
		//		case HYBRID: mainLog.println("Hybrid"); break;
		//		default: mainLog.print("?????"); break;
		//		}
		
		// do steady state calculation
		l = System.currentTimeMillis();
		probs = ((StochModelChecker)mc).doSteadyState();
		l = System.currentTimeMillis() - l;
		
		// print out probabilities
		mainLog.print("\nProbabilities: \n");
		probs.print(mainLog);
		probs.clear();
		
		// print out model checking time
		mainLog.println("\nTime for steady-state probability computation: " + l/1000.0 + " seconds.");
	}
	
	// do transient
	
	public void doTransient(Model model, double time) throws PrismException
	{
		long l = 0; // timer
		StateProbs probs = null;
		
		mainLog.println("\nComputing transient probabilities (time = " + time + ")...");
		
		// create/configure new model checker object
		mc = new StochModelChecker(mainLog, techLog, model, null);
		switch (getEngine())
		{
			case MTBDD: mc.setEngine(MTBDD); break;
			case SPARSE: mc.setEngine(SPARSE); break;
			case HYBRID: mc.setEngine(HYBRID); break;
			default: break;
		}
		mc.setOption("lineqmethod", getLinEqMethod()); // don't really need
		mc.setOption("lineqmethodparam", getLinEqMethodParam()); // don't really need
		mc.setOption("termcrit", getTermCrit());
		mc.setOption("termcritparam", getTermCritParam());
		mc.setOption("maxiters", getMaxIters()); // don't really need
		mc.setOption("verbose", getVerbose());
		mc.setOption("bscccomp", getBSCCComp()); // don't really need
		mc.setOption("dossdetect", getDoSSDetect());
		if (getEngine() == HYBRID || getEngine() == SPARSE)
		{
			mc.setOption("compact", getCompact());
		}
		if (getEngine() == HYBRID)
		{
			mc.setOption("sbmaxmem", getSBMaxMem());
			mc.setOption("numsblevels", getNumSBLevels());
			mc.setOption("sormaxmem", getSORMaxMem()); // don't really need
			mc.setOption("numsorlevels", getNumSORLevels()); // don't really need
		}
		
		//		// print out which engine we are using
		//		mainLog.print("\nEngine: ");
		//		switch (engine) {
		//		case MTBDD: mainLog.println("MTBDD"); break;
		//		case SPARSE: mainLog.println("Sparse"); break;
		//		case HYBRID: mainLog.println("Hybrid"); break;
		//		default: mainLog.print("?????"); break;
		//		}
		
		// do steady state calculation
		l = System.currentTimeMillis();
		probs = ((StochModelChecker)mc).doTransient(time);
		l = System.currentTimeMillis() - l;
		
		// print out probabilities
		mainLog.print("\nProbabilities: \n");
		probs.print(mainLog);
		probs.clear();
		
		// print out model checking time
		mainLog.println("\nTime for transient probability computation: " + l/1000.0 + " seconds.");
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
	
	//access method for the settings
	
	public PrismSettings getSettings()
	{
		return settings;
	}
}

//------------------------------------------------------------------------------
