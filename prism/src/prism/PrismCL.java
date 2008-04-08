//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package prism;

import java.io.*;

import parser.*;
import parser.ast.*;

// prism - command line version

public class PrismCL
{
	// flags
	private boolean verbose;
	private boolean fixdl = false;
	private boolean importpepa = false;
	private boolean importtrans = false;
	private boolean importstates = false;
	private boolean steadystate = false;
	private boolean dotransient = false;
	private boolean exporttrans = false;
	private boolean exportstaterewards = false;
	private boolean exporttransrewards = false;
	private boolean exportstates = false;
	private boolean exportlabels = false;
	private boolean exportspy = false;
	private boolean exportdot = false;
	private boolean exporttransdot = false;
	private boolean exportresults = false;
	private boolean exportPlainDeprecated = false;
	private int exportType = Prism.EXPORT_PLAIN;
	private boolean exportordered = true;
	private boolean simulate = false;
	private boolean simpath = false;
	private int typeOverride = 0;
	
	// property info
	private int propertyToCheck = -1;
	private String propertyString = "";
	
	// argument to -const switch
	private String constSwitch = null;
	
	// argument to -simpath switch
	private String simpathDetails = null;
	
	// import info
	private String importInitString = null;
	
	// files/filenames
	private String mainLogFilename = "stdout";
	private String techLogFilename = "stdout";
	private String modelFilename = null;
	private String importStatesFilename = null;
	private String propertiesFilename = null;
	private String exportTransFilename = null;
	private String exportStateRewardsFilename = null;
	private String exportTransRewardsFilename = null;
	private String exportStatesFilename = null;
	private String exportLabelsFilename = null;
	private String exportSpyFilename = null;
	private String exportDotFilename = null;
	private String exportTransDotFilename = null;
	private String exportResultsFilename = null;
	private String simpathFilename = null;
	
	// logs
	private PrismLog mainLog = null;
	private PrismLog techLog = null;
	
	// prism object
	private Prism prism = null;
	
	// storage for parsed model/properties files
	private ModulesFile modulesFile = null;
	private PropertiesFile propertiesFile = null;
	
	// info about which properties to model check
	private int numPropertiesToCheck = 0;
	private Expression propertiesToCheck[] = null;
	
	// info about undefined constants
	private UndefinedConstants undefinedConstants;
	private Values definedMFConstants;
	private Values definedPFConstants;
	
	// built model storage
	private Model model = null;
	
	// results
	private ResultsCollection results[] = null;
	
	// time for transient computation
	private String transientTime;
	
	// simulation info
	private double simApprox;
	private double simConfidence;
	private int simNumSamples;
	private int simMaxPath;
	private boolean simApproxGiven = false;
	private boolean simConfidenceGiven = false;
	private boolean simNumSamplesGiven = false;
	private boolean simMaxPathGiven = false;
	
	// entry point - run method
	
	public void run(String[] args)
	{
		int i, j, k;
		Result res;
		
		// initialise
		try {
			initialise(args);
		}
		catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// parse model/properties
		try {
			doParsing();
		}
		catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// sort out properties to check
		sortProperties();
		
		// sort out undefined constants
		try {
			undefinedConstants = new UndefinedConstants(modulesFile, propertiesFile);
			undefinedConstants.defineUsingConstSwitch(constSwitch);
		}
		catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// initialise storage for results
		results = new ResultsCollection[numPropertiesToCheck];
		for (i = 0; i < numPropertiesToCheck; i++) {
			results[i] = new ResultsCollection(undefinedConstants, propertiesToCheck[i].getResultName());
		}
		
		// iterate through as many models as necessary
		for (i = 0; i < undefinedConstants.getNumModelIterations(); i++) {
			
			definedMFConstants = undefinedConstants.getMFConstantValues();
			if (definedMFConstants != null) if (definedMFConstants.getNumValues() > 0) mainLog.println("\nModel constants: " + definedMFConstants);
			
			// set values for ModulesFile constants
			try {
				modulesFile.setUndefinedConstants(definedMFConstants);
			}
			catch (PrismException e) {
				// in case of error, report it, store as result for any properties, and go on to the next model
				// (should never happen)
				error(e.getMessage());
				try {
					for (j = 0; j < numPropertiesToCheck; j++) {
						results[j].setMultipleErrors(definedMFConstants, null, e);
					}
				}
				catch (PrismException e2) {
					error("Problem storing results");
				}
				undefinedConstants.iterateModel();
				continue;
			}
			
			// if requested, generate a random path with simulator (and then skip anything else)
			if (simpath) {
				try {
					File f = (simpathFilename.equals("stdout")) ? null : new File(simpathFilename);
					prism.generateSimulationPath(modulesFile, simpathDetails, simMaxPath, f);
				}
				catch (PrismException e) {
					error(e.getMessage());
				}
				undefinedConstants.iterateModel();
				continue;
			}
			
			// only do explicit model construction if necessary
			if (!simulate) {
				
				// build model
				try {
					buildModel();
				}
				catch (PrismException e) {
					// in case of error, report it, store as result for any properties, and go on to the next model
					error(e.getMessage());
					try {
						for (j = 0; j < numPropertiesToCheck; j++) {
							results[j].setMultipleErrors(definedMFConstants, null, e);
						}
					}
					catch (PrismException e2) {
						error("Problem storing results");
					}
					undefinedConstants.iterateModel();
					continue;
				}
				
				// do any exports
				doExports();
				
				// do steady state comp if required
				if (steadystate) {
					try {
						doSteadyState();
					}
					catch (PrismException e) {
						// in case of error, report it and proceed
						error(e.getMessage());
					}
				}
				
				// do transient comp if required
				if (dotransient) {
					try {
						doTransient();
					}
					catch (PrismException e) {
						// in case of error, report it and proceed
						error(e.getMessage());
					}
				}
			}
			
			// export labels/states
			if (exportlabels) {
				try {
					if (propertiesFile != null) {
						definedPFConstants = undefinedConstants.getPFConstantValues();
						propertiesFile.setUndefinedConstants(definedPFConstants);
					}
					File f = (exportLabelsFilename.equals("stdout")) ? null : new File(exportLabelsFilename);
					prism.exportLabelsToFile(model, modulesFile, propertiesFile, exportType, f);
				}
				// in case of error, report it and proceed
				catch (FileNotFoundException e) {
					mainLog.println("Couldn't open file \"" + exportLabelsFilename + "\" for output");
				}
				catch (PrismException e) {
					mainLog.println("\nError: " + e.getMessage() + ".");
				}
			}
			
			// work through list of properties to be checked
			for (j = 0; j < numPropertiesToCheck; j++) {
				
				// for simulation we can do multiple values of property constants simultaneously
				if (simulate && undefinedConstants.getNumPropertyIterations() > 1) {
					try {
						mainLog.println("\n-------------------------------------------");
						mainLog.println("\nSimulating: " + propertiesToCheck[j]);
						if (definedMFConstants != null) if (definedMFConstants.getNumValues() > 0) mainLog.println("Model constants: " + definedMFConstants);
						mainLog.println("Property constants: " + undefinedConstants.getPFDefinedConstantsString());
						mainLog.println("Simulation parameters: approx = "+simApprox+", conf = "+simConfidence+", num samples = "+simNumSamples+", max path len = "+simMaxPath+")");
						prism.modelCheckSimulatorExperiment(modulesFile, propertiesFile, undefinedConstants, results[j], propertiesToCheck[j], modulesFile.getInitialValues(), simNumSamples, simMaxPath);
					}
					catch (PrismException e) {
						// in case of (overall) error, report it, store as result for property, and proceed
						error(e.getMessage());
						try {
							results[j].setMultipleErrors(definedMFConstants, null, e);
						}
						catch (PrismException e2) {
							error("Problem storing results");
						}
						undefinedConstants.iterateModel();
						continue;
					}
					catch (InterruptedException e) {
						// ignore - won't get interrupted
					}
				}
				// otherwise, treat each case individually
				else {
					for (k = 0; k < undefinedConstants.getNumPropertyIterations(); k++) {
						
						try {
							// set values for PropertiesFile constants
							if (propertiesFile != null) {
								definedPFConstants = undefinedConstants.getPFConstantValues();
								propertiesFile.setUndefinedConstants(definedPFConstants);
							}
							
							// do model checking
							mainLog.println("\n-------------------------------------------");
							mainLog.println("\n"+(simulate?"Simulating":"Model checking")+": " + propertiesToCheck[j]);
							if (definedMFConstants != null) if (definedMFConstants.getNumValues() > 0) mainLog.println("Model constants: " + definedMFConstants);
							if (definedPFConstants != null) if (definedPFConstants.getNumValues() > 0) mainLog.println("Property constants: " + definedPFConstants);
							if (!simulate) {
								res = prism.modelCheck(model, propertiesFile, propertiesToCheck[j]);
							}
							else {
								mainLog.println("Simulation parameters: approx = "+simApprox+", conf = "+simConfidence+", num samples = "+simNumSamples+", max path len = "+simMaxPath+")");
								res = prism.modelCheckSimulator(modulesFile, propertiesFile, propertiesToCheck[j], modulesFile.getInitialValues(), simNumSamples, simMaxPath);
							}
						}
						catch (PrismException e) {
							// in case of error, report it, store exception as the result and proceed
							error(e.getMessage());
							res = new Result(e);
						}
						
						// store result of model checking
						try {
							results[j].setResult(definedMFConstants, definedPFConstants, res.getResult());
						}
						catch (PrismException e) {
							error("Problem storing results");
						}
						
						// iterate to next property
						undefinedConstants.iterateProperty();
					}
				}
			}
			
			// clear model
			if (!simulate) {
				model.clear();
			}
			
			// iterate to next model
			undefinedConstants.iterateModel();
		}
		
		// export results (if required)
		if (exportresults) {
			
			mainLog.print("\nExporting results ");
			if (!exportResultsFilename.equals("stdout")) mainLog.println("to file \"" + exportResultsFilename + "\"..."); else mainLog.println("below:");
			PrismFileLog tmpLog = new PrismFileLog(exportResultsFilename);
			if (!tmpLog.ready()) {
				errorAndExit("Couldn't open file \"" + exportResultsFilename + "\" for output");
			}
			for (i = 0; i < numPropertiesToCheck; i++) {
				if (i > 0) tmpLog.println();
				tmpLog.print(propertiesToCheck[i] + ":\n" + results[i].toString(false, " ", " "));
			}
			tmpLog.close();
		}
		
		// close down
		closeDown();
	}

	// initialise
	
	private void initialise(String[] args) throws PrismException
	{
		int i;
		
		// default to logs going to stdout
		// this means all errors etc. can be safely sent to the log
		// even if a new log is created shortly
		mainLog = new PrismFileLog("stdout");
		techLog = new PrismFileLog("stdout");
		
		// create prism object
		prism = new Prism(mainLog, techLog);
		
		// get prism defaults
		verbose = prism.getVerbose();
		
		// parse command line arguments
		parseArguments(args);
		
		// initialise
		prism.initialise();
		
		// print command line for reference
		mainLog.print("Command line: prism");
		for (i = 0; i < args.length; i++) mainLog.print(" " + args[i]);
		mainLog.println();
		
		// do some processing of the options
		processOptions();
	}

	// parse model and properties
	
	private void doParsing() throws PrismException
	{
		int i;
		
		// parse model
		
		try {
			if (importpepa) {
				mainLog.print("\nImporting PEPA file \"" + modelFilename + "\"...\n");
				modulesFile = prism.importPepaFile(new File(modelFilename));
			}
			else if (importtrans) {
				mainLog.print("\nImporting model (");
				switch (typeOverride) {
				case ModulesFile.PROBABILISTIC: mainLog.print("DTMC"); break;
				case ModulesFile.NONDETERMINISTIC: mainLog.print("MDP"); break;
				case ModulesFile.STOCHASTIC: mainLog.print("CTMC"); break;
				default: mainLog.print("MDP"); break;
				}
				if (importstates) {
					mainLog.print(") from files \"" + importStatesFilename + "\" and \"" + modelFilename + "\"...\n");
					modulesFile = prism.parseExplicitModel(new File(importStatesFilename), new File(modelFilename), typeOverride, importInitString);
				} else {
					mainLog.print(") from file \"" + modelFilename + "\"...\n");
					modulesFile = prism.parseExplicitModel(null, new File(modelFilename), typeOverride, importInitString);
				}
			}
			else {
				mainLog.print("\nParsing model file \"" + modelFilename + "\"...\n");
				modulesFile = prism.parseModelFile(new File(modelFilename), typeOverride);
			}
		}
		catch (FileNotFoundException e) {
			errorAndExit("File \"" + modelFilename + "\" not found");
		}
		catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// parse properties
		
		try {
			// if properties file specified...
			if (propertiesFilename != null) {
				mainLog.print("\nParsing properties file \"" + propertiesFilename + "\"...\n");
				propertiesFile = prism.parsePropertiesFile(modulesFile, new File(propertiesFilename));
			}
			// if properties were given on command line...
			else if (!propertyString.equals("")) {
				propertiesFile = prism.parsePropertiesString(modulesFile, propertyString);
			}
			else {
				propertiesFile = null;
			}
		}	
		catch (FileNotFoundException e) {
			errorAndExit("File \"" + propertiesFilename + "\" not found");
		}
		catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// print out properties (if any)
		
		if (propertiesFile == null) return;
		mainLog.print("\n" + propertiesFile.getNumProperties());
		mainLog.print(" propert" + ((propertiesFile.getNumProperties()==1)?"y":"ies") + ":\n");
		for (i = 0; i < propertiesFile.getNumProperties(); i++) {
			mainLog.println("(" + (i+1) + ") " + propertiesFile.getProperty(i));
		}
	}

	// sort out which properties need checking
	
	private void sortProperties()
	{
		int i;
		
		// no properties to check
		if (propertiesFile == null) {
			numPropertiesToCheck = 0;
			propertiesToCheck = null;
		}
		// unless specified, verify all properties
		else if (propertyToCheck == -1) {
			numPropertiesToCheck = propertiesFile.getNumProperties();
			propertiesToCheck = new Expression[numPropertiesToCheck];
			for (i = 0; i < numPropertiesToCheck; i++) {
				propertiesToCheck[i] =  propertiesFile.getProperty(i);
			}
		}
		// otherwise just verify the relevant property
		else {
			if (propertyToCheck > 0 && propertyToCheck <= propertiesFile.getNumProperties()) {
				numPropertiesToCheck = 1;
				propertiesToCheck = new Expression[1];
				propertiesToCheck[0] =  propertiesFile.getProperty(propertyToCheck-1);
			}
			else {
				errorAndExit("There is not a property " + propertyToCheck + " to verify");
			}
		}
	}

	// build model
	
	private void buildModel() throws PrismException
	{
		StateList states;
		int i;
		
		mainLog.println("\n-------------------------------------------");
		
		// build model
		if (importtrans) {
			model = prism.buildExplicitModel();
		}
		else {
			model = prism.buildModel(modulesFile);
		}
		
		// print model info
		mainLog.println("\nType:        " + model.getType());
		mainLog.print("Modules:     ");
		for (i = 0; i < model.getNumModules(); i++) {
			mainLog.print(model.getModuleName(i) + " ");
		}
		mainLog.println();
		mainLog.print("Variables:   ");
		for (i = 0; i < model.getNumVars(); i++) {
			mainLog.print(model.getVarName(i) + " ");
		}
		mainLog.println();
		
		// check for deadlocks
		states = model.getDeadlockStates();
		if (states != null) {
			if (states.size() > 0) {
				// if requested, remove them
				if (fixdl) {
					mainLog.print("\nWarning: " + states.size() + " deadlock states detected; adding self-loops in these states...\n");
					model.fixDeadlocks();
				}
				// otherwise print error and bail out
				else {
					mainLog.println();
					model.printTransInfo(mainLog, prism.getExtraDDInfo());
					mainLog.print("\nError: Model contains " + states.size() + " deadlock states");
					if (!verbose && states.size() > 10) {
						mainLog.print(".\nThe first 10 deadlock states are displayed below. To view them all use the -v switch.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					mainLog.print("\nTip: Use the -fixdl switch to automatically add self-loops in deadlock states.\n");
					model.clear();
					exit();
				}
			}
		}
		
		// print more model info
		mainLog.println();
		model.printTransInfo(mainLog, prism.getExtraDDInfo());
	}

	// do any exporting requested
	
	private void doExports()
	{
		// export transition matrix to a file
		if (exporttrans) {
			try {
				File f = (exportTransFilename.equals("stdout")) ? null : new File(exportTransFilename);
				prism.exportTransToFile(model, exportordered, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransFilename + "\" for output");
			}
			
			if (exportPlainDeprecated) mainLog.println("\nWarning: The -exportplain switch is now deprecated. Please use -exporttrans in future.");
		}
		
		// export state rewards to a file
		if (exportstaterewards) {
			try {
				File f = (exportStateRewardsFilename.equals("stdout")) ? null : new File(exportStateRewardsFilename);
				prism.exportStateRewardsToFile(model, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportStateRewardsFilename + "\" for output");
			}
			catch (PrismException e) {
				error(e.getMessage());
			}
		}
		
		// export transition rewards to a file
		if (exporttransrewards) {
			try {
				File f = (exportTransRewardsFilename.equals("stdout")) ? null : new File(exportTransRewardsFilename);
				prism.exportTransRewardsToFile(model, exportordered, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransRewardsFilename + "\" for output");
			}
			catch (PrismException e) {
				error(e.getMessage());
			}
		}
		
		// export states list
		if (exportstates) {
			try {
				File f = (exportStatesFilename.equals("stdout")) ? null : new File(exportStatesFilename);
				prism.exportStatesToFile(model, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportStatesFilename + "\" for output");
			}
		}
		
		// export to spy file
		if (exportspy) {
			try {
				prism.exportToSpyFile(model, new File(exportSpyFilename));
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportSpyFilename + "\" for output");
			}
		}
		
		// export mtbdd to dot file
		if (exportdot) {
			try {
				prism.exportToDotFile(model, new File(exportDotFilename));
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportDotFilename + "\" for output");
			}
		}
		
		// export transition matrix graph to dot file
		if (exporttransdot) {
			try {
				File f = (exportTransDotFilename.equals("stdout")) ? null : new File(exportTransDotFilename);
				prism.exportTransToFile(model, exportordered, Prism.EXPORT_DOT, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransDotFilename + "\" for output");
			}
		}
	}

	// do steady state computation (if required)
	
	private void doSteadyState() throws PrismException
	{
		// compute steady-state probabilities
		if (model instanceof StochModel || model instanceof ProbModel) {
			prism.doSteadyState(model);
		}
		else {
			mainLog.println("\nWarning: Steady-state probabilities only computed for DTMCs/CTMCs.");
		}
	}

	// do transient computation (if required)
	
	private void doTransient() throws PrismException
	{
		double d;
		int i;
		
		// compute transient probabilities
		if (model instanceof StochModel) {
			try {
				d = Double.parseDouble(transientTime);
			}
			catch (NumberFormatException e) {
				throw new PrismException("Invalid value \""+transientTime+"\" for transient probability computation");
			}
			prism.doTransient(model, d);
		}
		else if (model instanceof ProbModel) {
			try {
				i = Integer.parseInt(transientTime);
			}
			catch (NumberFormatException e) {
				throw new PrismException("Invalid value \""+transientTime+"\" for transient probability computation");
			}
			prism.doTransient(model, i);
		}
		else {
			mainLog.println("\nWarning: Transient probabilities only computed for DTMCs/CTMCs.");
		}
	}

	// close down
	
	private void closeDown()
	{
		// clear up and close down
		prism.closeDown(true);
		mainLog.println();
	}

	// parse command line arguments
	
	public void parseArguments(String[] args) throws PrismException
	{
		int i, j;
		double d;
		String sw;
		PrismLog log;
		
		for (i = 0; i < args.length; i++) {
		
			// if is a switch...
			if (args[i].length() > 0 && args[i].charAt(0) == '-') {
			
				sw = args[i].substring(1);
				
				// print help
				if (sw.equals("help") || sw.equals("?")) {
					printHelp();
					exit();
				}
				// print version
				else if (sw.equals("version")) {
					printVersion();
					exit();
				}
				
				// property/properties given in command line
				else if (sw.equals("pctl") || sw.equals("csl")) {
					if (i < args.length-1) {
						propertyString = args[++i];
					}
					else {
						errorAndExit("No property specified for -"+sw+" switch");
					}
				}
				// do steady state
				else if (sw.equals("steadystate") || sw.equals("ss")) {
					steadystate = true;
				}
				// do steady state
				else if (sw.equals("transient") || sw.equals("tr")) {
					if (i < args.length-1) {
						try {
							dotransient = true;
							transientTime = args[++i];
							// Make sure transient time parses as a +ve double
							d = Double.parseDouble(transientTime);
							if (d < 0) throw new NumberFormatException("");
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// export transition matrix to file
				else if (sw.equals("exporttrans")) {
					if (i < args.length-1) {
						exporttrans = true;
						exportTransFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export state rewards to file
				else if (sw.equals("exportstaterewards")) {
					if (i < args.length-1) {
						exportstaterewards = true;
						exportStateRewardsFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export transition rewards to file
				else if (sw.equals("exporttransrewards")) {
					if (i < args.length-1) {
						exporttransrewards = true;
						exportTransRewardsFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export both state/transition rewards to file
				else if (sw.equals("exportrewards")) {
					if (i < args.length-2) {
						exportstaterewards = true;
						exporttransrewards = true;
						exportStateRewardsFilename = args[++i];
						exportTransRewardsFilename = args[++i];
					}
					else {
						errorAndExit("Two files must be specified for -"+sw+" switch");
					}
				}
				// export states
				else if (sw.equals("exportstates")) {
					if (i < args.length-1) {
						exportstates = true;
						exportStatesFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export labels/states
				else if (sw.equals("exportlabels")) {
					if (i < args.length-1) {
						exportlabels = true;
						exportLabelsFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export to spy file
				else if (sw.equals("exportspy")) {
					if (i < args.length-1) {
						exportspy = true;
						exportSpyFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export to dot file
				else if (sw.equals("exportdot")) {
					if (i < args.length-1) {
						exportdot = true;
						exportDotFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export transition matrix graph to dot file
				else if (sw.equals("exporttransdot")) {
					if (i < args.length-1) {
						exporttransdot = true;
						exportTransDotFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// export results
				else if (sw.equals("exportresults")) {
					if (i < args.length-1) {
						exportresults = true;
						exportResultsFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// switch export mode to "matlab"
				else if (sw.equals("exportmatlab")) {
					exportType = Prism.EXPORT_MATLAB;
				}
				// switch export mode to "mrmc"
				else if (sw.equals("exportmrmc")) {
					exportType = Prism.EXPORT_MRMC;
				}
				// switch export mode to "rows"
				else if (sw.equals("exportrows")) {
					exportType = Prism.EXPORT_ROWS;
				}
				// export model to plain text file (deprecated)
				else if (sw.equals("exportplain")) {
					if (i < args.length-1) {
						exporttrans = true;
						exportType = Prism.EXPORT_PLAIN;
						exportTransFilename = args[++i];
						exportPlainDeprecated = true;
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// exported matrix entries are ordered
				else if (sw.equals("exportordered") || sw.equals("ordered")) {
					exportordered = true;
				}
				// exported matrix entries are unordered
				else if (sw.equals("exportunordered") || sw.equals("unordered")) {
					exportordered = false;
				}
				// change model type to pepa
				else if (sw.equals("importpepa")) {
					importpepa = true;
				}
				// import model from explicit format
				else if (sw.equals("importtrans")) {
					importtrans = true;
				}
				// import states for explicit model import
				else if (sw.equals("importstates")) {
					if (i < args.length-1) {
						importstates = true;
						importStatesFilename = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// import initial states info for explicit model import
				else if (sw.equals("importinit")) {
					if (i < args.length-1) {
						importInitString = args[++i];
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// override model type to dtmc
				else if (sw.equals("dtmc")) {
					typeOverride = ModulesFile.PROBABILISTIC;
				}
				// override model type to mdp
				else if (sw.equals("mdp")) {
					typeOverride = ModulesFile.NONDETERMINISTIC;
				}
				// override model type to ctmc
				else if (sw.equals("ctmc")) {
					typeOverride = ModulesFile.STOCHASTIC;
				}
				// use simulator
				else if (sw.equals("sim")) {
					simulate = true;
				}
				// generate path with simulator
				else if (sw.equals("simpath")) {
					if (i < args.length-2) {
						simpath = true;
						simpathDetails = args[++i];
						simpathFilename = args[++i];
					}
					else {
						errorAndExit("The -"+sw+" switch requires two arguments (path details, filename)");
					}
				}
				// which property to check
				else if (sw.equals("prop") || sw.equals("property")) {
					if (i < args.length-1) {
						try {
							propertyToCheck = Integer.parseInt(args[++i]);
							if (propertyToCheck < 1) throw new NumberFormatException();
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				
				// definition of undefined constants
				else if (sw.equals("const")) {
					if (i < args.length-1) {
						// store argument for later use
						constSwitch = args[++i];
					}
					else {
						errorAndExit("Incomplete -"+sw+" switch");
					}
				}
				
				// logs
				
				// specify main log
				else if (sw.equals("mainlog")) {
					if (i < args.length-1) {
						mainLogFilename = args[++i];
						// use temporary storage because an error would go to the old log
						log = new PrismFileLog(mainLogFilename);
						if (!log.ready()) {
							errorAndExit("Couldn't open log file \"" + mainLogFilename + "\"");
						}
						mainLog = log;
						prism.setMainLog(mainLog);
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				// specify mtbdd log
				else if (sw.equals("techlog")) {
					if (i < args.length-1) {
						techLogFilename = args[++i];
						log = new PrismFileLog(techLogFilename);
						if (!log.ready()) {
							errorAndExit("Couldn't open log file \"" + techLogFilename + "\"");
						}
						techLog = log;
						prism.setTechLog(techLog);
					}
					else {
						errorAndExit("No file specified for -"+sw+" switch");
					}
				}
				
				// engine
				
				// set engine to 'mtbdd'
				else if (sw.equals("mtbdd") || sw.equals("m")) {
					prism.setEngine(Prism.MTBDD);
					// and change default ordering
					prism.setOrdering(2);
				}
				// set engine to 'sparse'
				else if (sw.equals("sparse") || sw.equals("s")) {
					prism.setEngine(Prism.SPARSE);
				}
				// set engine to 'hybrid'
				else if (sw.equals("hybrid") || sw.equals("h")) {
					prism.setEngine(Prism.HYBRID);
				}
				
				// model construction options
				
				// mtbdd construction method
				else if (sw.equals("c1")) {
					prism.setConstruction(1);
				}
				else if (sw.equals("c2")) {
					prism.setConstruction(2);
				}
				else if (sw.equals("c3")) {
					prism.setConstruction(3);
				}
				// mtbdd variable ordering
				else if (sw.equals("o1")) {
					prism.setOrdering(1);
				}
				else if (sw.equals("o2")) {
					prism.setOrdering(2);
				}
				// reachability off
				else if (sw.equals("noreach")) {
					prism.setDoReach(false);
				}
				// prob/rate checks off
				else if (sw.equals("noprobchecks")) {
					prism.setDoProbChecks(false);
				}
				
				// model checking options
				
				// linear equation solver method
				else if (sw.equals("power") || sw.equals("pow") || sw.equals("pwr")) {
					prism.setLinEqMethod(Prism.POWER);
				}
				else if (sw.equals("jacobi") || sw.equals("jac")) {
					prism.setLinEqMethod(Prism.JACOBI);
				}
				else if (sw.equals("gaussseidel") || sw.equals("gs")) {
					prism.setLinEqMethod(Prism.GAUSSSEIDEL);
				}
				else if (sw.equals("bgaussseidel") || sw.equals("bgs")) {
					prism.setLinEqMethod(Prism.BGAUSSSEIDEL);
				}
				else if (sw.equals("pgaussseidel") || sw.equals("pgs")) {
					prism.setLinEqMethod(Prism.PGAUSSSEIDEL);
				}
				else if (sw.equals("bpgaussseidel") || sw.equals("bpgs")) {
					prism.setLinEqMethod(Prism.BPGAUSSSEIDEL);
				}
				else if (sw.equals("jor")) {
					prism.setLinEqMethod(Prism.JOR);
					prism.setLinEqMethodParam(0.9);
				}
				else if (sw.equals("sor")) {
					prism.setLinEqMethod(Prism.SOR);
					prism.setLinEqMethodParam(0.9);
				}
				else if (sw.equals("bsor")) {
					prism.setLinEqMethod(Prism.BSOR);
					prism.setLinEqMethodParam(0.9);
				}
				else if (sw.equals("psor")) {
					prism.setLinEqMethod(Prism.PSOR);
					prism.setLinEqMethodParam(0.9);
				}
				else if (sw.equals("bpsor")) {
					prism.setLinEqMethod(Prism.BPSOR);
					prism.setLinEqMethodParam(0.9);
				}
				// linear equation solver parameter
				else if (sw.equals("omega")) {
					if (i < args.length-1) {
						try {
							d = Double.parseDouble(args[++i]);
							prism.setLinEqMethodParam(d);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// termination criterion (iterative methods)
				else if (sw.equals("absolute") || sw.equals("abs")) {
					prism.setTermCrit(Prism.ABSOLUTE);
				}
				else if (sw.equals("relative") || sw.equals("rel")) {
					prism.setTermCrit(Prism.RELATIVE);
				}
				// termination criterion parameter
				else if (sw.equals("epsilon") || sw.equals("e")) {
					if (i < args.length-1) {
						try {
							d = Double.parseDouble(args[++i]);
							if (d < 0) throw new NumberFormatException("");
							prism.setTermCritParam(d);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// max iters
				else if (sw.equals("maxiters")) {
					if (i < args.length-1) {
						try {
							j = Integer.parseInt(args[++i]);
							if (j < 0) throw new NumberFormatException("");
							prism.setMaxIters(j);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// verbose on
				else if (sw.equals("verbose") || sw.equals("v")) {
					prism.setVerbose(true);
					verbose = true;
				}
				// extra dd info on
				else if (sw.equals("extraddinfo")) {
					prism.setExtraDDInfo(true);
				}
				// extra reach info on
				else if (sw.equals("extrareachinfo")) {
					prism.setExtraReachInfo(true);
				}
				// precomputation algs off
				else if (sw.equals("nopre")) {
					prism.setPrecomp(false);
				}
				// fairness on
				else if (sw.equals("fair")) {
					prism.setFairness(true);
				}
				// fairness off
				else if (sw.equals("nofair")) {
					prism.setFairness(false);
				}
				// fix deadlocks
				else if (sw.equals("fixdl")) {
					fixdl = true;
				}
				// no bscc computation
				else if (sw.equals("nobscc")) {
					prism.setBSCCComp(false);
				}
				// no steady-state detection
				else if (sw.equals("nossdetect")) {
					prism.setDoSSDetect(false);
				}
				// sparse bits info
				else if (sw.equals("sbmax")) {
					if (i < args.length-1) {
						try {
							j = Integer.parseInt(args[++i]);
							if (j < 0) throw new NumberFormatException();
							prism.setSBMaxMem(j);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				else if (sw.equals("sbl")) {
					if (i < args.length-1) {
						try {
							j = Integer.parseInt(args[++i]);
							if (j < -1) throw new NumberFormatException();
							prism.setNumSBLevels(j);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// hybrid sor info
				else if (sw.equals("sormax") || sw.equals("gsmax")) {
					if (i < args.length-1) {
						try {
							j = Integer.parseInt(args[++i]);
							if (j < 0) throw new NumberFormatException();
							prism.setSORMaxMem(j);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				else if (sw.equals("sorl") || sw.equals("gsl")) {
					if (i < args.length-1) {
						try {
							j = Integer.parseInt(args[++i]);
							if (j < -1) throw new NumberFormatException();
							prism.setNumSORLevels(j);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// turn off compact option for sparse matrix storage
				else if (sw.equals("nocompact")) {
					prism.setCompact(false);
				}
				// cudd settings
				else if (sw.equals("cuddmaxmem")) {
					if (i < args.length-1) {
						try {
							j = Integer.parseInt(args[++i]);
							if (j < 0) throw new NumberFormatException();
							prism.setCUDDMaxMem(j);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				else if (sw.equals("cuddepsilon")) {
					if (i < args.length-1) {
						try {
							d = Double.parseDouble(args[++i]);
							if (d < 0) throw new NumberFormatException("");
							prism.setCUDDEpsilon(d);
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				
				// simulation approximation parameter
				else if (sw.equals("simapprox")) {
					if (i < args.length-1) {
						try {
							simApprox = Double.parseDouble(args[++i]);
							if (simApprox <= 0) throw new NumberFormatException("");
							simApproxGiven = true;
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// simulation confidence parameter
				else if (sw.equals("simconf")) {
					if (i < args.length-1) {
						try {
							simConfidence = Double.parseDouble(args[++i]);
							if (simConfidence <= 0 || simConfidence >= 1) throw new NumberFormatException("");
							simConfidenceGiven = true;
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// simulation number of samples
				else if (sw.equals("simsamples")) {
					if (i < args.length-1) {
						try {
							simNumSamples = Integer.parseInt(args[++i]);
							if (simNumSamples <= 0) throw new NumberFormatException("");
							simNumSamplesGiven = true;
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				// simulation max path length
				else if (sw.equals("simpathlen")) {
					if (i < args.length-1) {
						try {
							simMaxPath = Integer.parseInt(args[++i]);
							if (simMaxPath <= 0) throw new NumberFormatException("");
							simMaxPathGiven = true;
						}
						catch (NumberFormatException e) {
							errorAndExit("Invalid value for -"+sw+" switch");
						}
					}
					else {
						errorAndExit("No value specified for -"+sw+" switch");
					}
				}
				
				// unknown switch - error
				else {
					errorAndExit("Invalid switch -" + sw + " (type \"prism -help\" for full list)");
				}
			}
			// otherwise argument must be a filename
			else if (modelFilename == null) {
				modelFilename = args[i];
			}
			else if (propertiesFilename == null) {
				propertiesFilename = args[i];
			}
			// anything else - must be something wrong with command line syntax
			else {
				errorAndExit("Invalid argument syntax");
			}
		}
	}

	// do some processing of the options
		
	private void processOptions()
	{
		int j;
		
		// make sure a model file is specified
		if (modelFilename == null) {
			mainLog.println("Usage: prism [options] <model-file> [<properties-file>] [more-options]");
			mainLog.println("\nFor more information, type: prism -help");
			exit();
		}
		
		// check not trying to do gauss-seidel with mtbdd engine
		if (prism.getEngine() == Prism.MTBDD) {
			j = prism.getLinEqMethod();
			if (j == Prism.GAUSSSEIDEL || j == Prism.BGAUSSSEIDEL || j == Prism.PGAUSSSEIDEL || j == Prism.BPGAUSSSEIDEL) {
				errorAndExit("Gauss-Seidel and its variants are currently not supported by the MTBDD engine");
			}
			if (j == Prism.SOR || j == Prism.BSOR || j == Prism.PSOR || j == Prism.BPSOR) {
				errorAndExit("SOR and its variants are currently not supported by the MTBDD engine");
			}
		}
		
		// or pseudo methods with sparse engine
		else if (prism.getEngine() == Prism.SPARSE) {
			j = prism.getLinEqMethod();
			if (j == Prism.PGAUSSSEIDEL || j == Prism.BPGAUSSSEIDEL || j == Prism.PSOR || j == Prism.BPSOR) {
				errorAndExit("Pseudo Gauss-Seidel/SOR methods are currently not supported by the sparse engine");
			}
		}
		
		// compute simulation parameters
		
		// print a warning if user tried to specify all three params
		if (simApproxGiven && simConfidenceGiven && simNumSamplesGiven)
			mainLog.println("\nWarning: Cannot specify all three simulation parameters; ignoring approximation parameter.");
		// start with default values where not supplied
		if (!simApproxGiven) simApprox = prism.getSettings().getDouble(PrismSettings.SIMULATOR_DEFAULT_APPROX);
		if (!simConfidenceGiven) simConfidence = prism.getSettings().getDouble(PrismSettings.SIMULATOR_DEFAULT_CONFIDENCE);
		if (!simNumSamplesGiven) simNumSamples = prism.getSettings().getInteger(PrismSettings.SIMULATOR_DEFAULT_NUM_SAMPLES);
		// which one are we going to compute from the other two?
		// (note have to compute one so that the three params are consistent)
		// number of samples gets priority - if this is specified, always use it
		if (simNumSamplesGiven) {
			// if approximation, but not confidence given, compute confidence
			if (simApproxGiven && !simConfidenceGiven) {
				simConfidence = prism.computeSimulationConfidence(simApprox, simNumSamples);
			}
			// otherwise compute approximation
			else {
				simApprox = prism.computeSimulationApproximation(simConfidence, simNumSamples);
			}
		}
		else {
			simNumSamples = prism.computeSimulationNumSamples(simApprox, simConfidence);
		}
		// finally, use default value for max path length if not supplied
		if (!simMaxPathGiven) simMaxPath = prism.getSettings().getInteger(PrismSettings.SIMULATOR_DEFAULT_MAX_PATH);
	}

	// print help message
	
	private void printHelp()
	{
		mainLog.println("Usage: prism <model-file> [<properties-file>] [options]");
		mainLog.println();
		mainLog.println("Options:");
		mainLog.println("========");
		mainLog.println();
		mainLog.println("-help .......................... Display this help message");
		mainLog.println("-version ....................... Display tool version");
		mainLog.println();
		mainLog.println("-pctl <prop> (or -csl <prop>) .. Model check the PCTL/CSL property <prop>");
		mainLog.println("-property <n> (or -prop <n>) ... Only model check property <n> from the properties file");
		mainLog.println("-const <vals> .................. Run an experiment using constant values <vals>");
		mainLog.println("-steadystate (or -ss) .......... Compute steady-state probabilities (CTMCs only)");
		mainLog.println("-transient <x> (or -tr <x>) .... Compute transient probabilities for time <x> (CTMCs only)");
		mainLog.println();
		mainLog.println("-importpepa .................... Model description is in PEPA, not the PRISM language");
		mainLog.println("-importtrans <file> ............ Import the transition matrix directly from a text file");
		mainLog.println("-importstates <file>............ Import the list of states directly from a text file");
		mainLog.println("-importinit <expr>.............. Specify the initial state for explicitly imported models");
		mainLog.println("-dtmc .......................... Force imported/built model to be a DTMC");
		mainLog.println("-ctmc .......................... Force imported/built model to be a CTMC");
		mainLog.println("-mdp ........................... Force imported/built model to be an MDP");
		mainLog.println();
		mainLog.println("-exportresults <file> .......... Export the results of model checking to a file");
		mainLog.println("-exporttrans <file> ............ Export the transition matrix to a file");
		mainLog.println("-exportstaterewards <file> ..... Export the state rewards vector to a file");
		mainLog.println("-exporttransrewards <file> ..... Export the transition rewards matrix to a file");
		mainLog.println("-exportstates <file> ........... Export the list of reachable states to a file");
		mainLog.println("-exportlabels <file> ........... Export the list of labels and satisfying states to a file");
		mainLog.println("-exportmatlab .................. When exporting matrices/vectors/labels/etc., use Matlab format");
		mainLog.println("-exportmrmc .................... When exporting matrices/vectors/labels, use MRMC format");
		mainLog.println("-exportrows .................... When exporting matrices, put a whole row on one line");
		mainLog.println("-exportordered ................. When exporting matrices, order entries (by row) [default]");
		mainLog.println("-exportunordered ............... When exporting matrices, don't order entries");
		mainLog.println("-exporttransdot <file> ......... Export the transition matrix graph to a dot file");
		mainLog.println("-exportdot <file> .............. Export the transition matrix MTBDD to a dot file");
		mainLog.println();
		mainLog.println("-mtbdd (or -m) ................. Use the MTBDD engine");
		mainLog.println("-sparse (or -s) ................ Use the Sparse engine");
		mainLog.println("-hybrid (or -h) ................ Use the Hybrid engine [default]");
		mainLog.println();
		mainLog.println("-power (or -pow, -pwr) ......... Use the Power method for numerical computation");
		mainLog.println("-jacobi (or -jac) .............. Use Jacobi for numerical computation [default]");
		mainLog.println("-gaussseidel (or -gs) .......... Use Gauss-Seidel for numerical computation");
		mainLog.println("-bgaussseidel (or -bgs) ........ Use Backwards Gauss-Seidel for numerical computation");
		mainLog.println("-pgaussseidel (or -pgs) ........ Use Pseudo Gauss-Seidel for numerical computation");
		mainLog.println("-bpgaussseidel (or -bpgs) ...... Use Backwards Pseudo Gauss-Seidel for numerical computation");
		mainLog.println("-jor ........................... Use JOR for numerical computation");
		mainLog.println("-sor ........................... Use SOR for numerical computation");
		mainLog.println("-bsor .......................... Use Backwards SOR for numerical computation");
		mainLog.println("-psor .......................... Use Pseudo SOR for numerical computation");
		mainLog.println("-bpsor ......................... Use Backwards Pseudo SOR for numerical computation");
		mainLog.println("-omega <x> ..................... Set over-relaxation parameter (for JOR/SOR/...) [default 0.9]");
		mainLog.println();
		mainLog.println("-relative (or -rel) ............ Use relative error for detecting convergence [default]");
		mainLog.println("-absolute (or -abs) ............ Use absolute error for detecting convergence");
		mainLog.println("-epsilon <x> (or -e <x>) ....... Set value of epsilon (for convergence check) [default 1e-6]");
		mainLog.println("-maxiters <n> .................. Set max number of iterations [default 10000]");
		mainLog.println();
		mainLog.println("-verbose (or -v) ............... Verbose mode: print out state lists and probability vectors");
		mainLog.println("-extraddinfo ................... Display extra info about some (MT)BDDs");
		mainLog.println("-extrareachinfo ................ Display extra info about progress of reachability");
		mainLog.println("-nopre ......................... Skip precomputation algorithms");
		mainLog.println("-fair .......................... Use fairness (when model checking MDPs)");
		mainLog.println("-nofair ........................ Don't use fairness (when model checking MDPs) [default]");
		mainLog.println("-fixdl ......................... Automatically put self-loops in deadlock states");
		mainLog.println("-nocompact ..................... Switch off \"compact\" sparse storage schemes");
		mainLog.println("-noprobchecks .................. Disable checks on model probabilities/rates");
		mainLog.println("-nossdetect .................... Disable steady-state detection for CTMC transient computations");
		mainLog.println();
		mainLog.println("-sbmax <n> ..................... Set memory limit (KB) (for hybrid engine) [default 1024]");
		mainLog.println("-sbl <n> ....................... Set number of levels (for hybrid engine) [default -1]");
		mainLog.println("-gsmax <n> (or sormax <n>) ..... Set memory limit (KB) for hybrid GS/SOR [default 1024]");
		mainLog.println("-gsl <n> (or sorl <n>) ......... Set number of levels for hybrid GS/SOR [default -1]");
		mainLog.println();
		mainLog.println("-cuddmaxmem <n> ................ Set max memory for CUDD package (KB) [default 200x1024]");
		mainLog.println("-cuddepsilon <x> ............... Set epsilon value for CUDD package [default 1e-15]");
		mainLog.println();
		mainLog.println("-sim ........................... Use the PRISM simulator to approximate results of model checking");
		mainLog.println("-simapprox <x> ................. Set the approximation parameter for the simulator");
		mainLog.println("-simconf <x> ................... Set the confidence parameter for the simulator");
		mainLog.println("-simsamples <n> ................ Set the number of samples for the simulator");
		mainLog.println("-simpathlen <n> ................ Set the maximum path length for the simulator");
		mainLog.println("-simpath <options> <file>....... Generate a random path with the simulator");
	}

	// print version
	
	private void printVersion()
	{
		mainLog.println("PRISM version " + Prism.getVersion());
	}

	// report (non-fatal) error
	
	private void error(String s)
	{
		mainLog.println("\nError: " + s + ".");
	}

	// report error and exit cleanly
	
	private void errorAndExit(String s)
	{
		prism.closeDown(false);
		error(s);
		System.exit(1);
	}

	// exit cleanly (no error)
	
	private void exit()
	{		
		prism.closeDown(true);
		System.exit(0);
	}

	// main method

	public static void main(String[] args)
	{
		new PrismCL().run(args);
	}
}

//------------------------------------------------------------------------------
