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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.Prism.StrategyExportType;
import simulator.GenerateSimulationPath;
import simulator.method.ACIconfidence;
import simulator.method.ACIiterations;
import simulator.method.ACIwidth;
import simulator.method.APMCapproximation;
import simulator.method.APMCconfidence;
import simulator.method.APMCiterations;
import simulator.method.CIconfidence;
import simulator.method.CIiterations;
import simulator.method.CIwidth;
import simulator.method.SPRTMethod;
import simulator.method.SimulationMethod;

// prism - command line version

public class PrismCL implements PrismModelListener
{
	// flags
	private boolean importpepa = false;
	private boolean importprismpp = false;
	private boolean importtrans = false;
	private boolean importstates = false;
	private boolean importlabels = false;
	private boolean importinitdist = false;
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
	private boolean exporttransdotstates = false;
	private boolean exportsccs = false;
	private boolean exportbsccs = false;
	private boolean exportmecs = false;
	private boolean exportresults = false;
	private boolean exportresultsmatrix = false;
	private boolean exportresultscsv = false;
	private boolean exportPlainDeprecated = false;
	private boolean exportModelNoBasename = false;
	private int exportType = Prism.EXPORT_PLAIN;
	private boolean exportordered = true;
	private boolean exportstrat = false;
	private boolean simulate = false;
	private boolean simpath = false;
	private boolean param = false;
	private ModelType typeOverride = null;
	private boolean orderingOverride = false;
	private boolean explicitbuild = false;
	private boolean explicitbuildtest = false;
	private boolean nobuild = false;
	private boolean test = false;
	private boolean testExitsOnFail = true;

	// property info
	private Object propertyToCheck = null;
	private String propertyString = "";

	// argument to -const switch
	private String constSwitch = null;

	// argument to -simpath switch
	private String simpathDetails = null;

	// argument to -importprismpp switch
	private String prismppParams = null;

	// argument to -param switch
	private String paramSwitch = null;

	// files/filenames
	private String mainLogFilename = "stdout";
	private String techLogFilename = "stdout";
	private String settingsFilename = null;
	private String modelFilename = null;
	private String importStatesFilename = null;
	private String importLabelsFilename = null;
	private String importInitDistFilename = null;
	private String propertiesFilename = null;
	private String exportTransFilename = null;
	private String exportStateRewardsFilename = null;
	private String exportTransRewardsFilename = null;
	private String exportStatesFilename = null;
	private String exportLabelsFilename = null;
	private String exportSpyFilename = null;
	private String exportDotFilename = null;
	private String exportTransDotFilename = null;
	private String exportTransDotStatesFilename = null;
	private String exportSCCsFilename = null;
	private String exportBSCCsFilename = null;
	private String exportMECsFilename = null;
	private String exportResultsFilename = null;
	private String exportSteadyStateFilename = null;
	private String exportTransientFilename = null;
	private String exportStratFilename = null;
	private String simpathFilename = null;

	// logs
	private PrismLog mainLog = null;
	private PrismLog techLog = null;

	// prism object
	private Prism prism = null;

	// storage for parsed model/properties files
	private ModulesFile modulesFile = null;
	private PropertiesFile propertiesFile = null;

	// model failure info
	boolean modelBuildFail = false;
	Exception modelBuildException = null;

	// info about which properties to model check
	private int numPropertiesToCheck = 0;
	private List<Property> propertiesToCheck = null;

	// info about undefined constants
	private UndefinedConstants undefinedConstants[];
	private UndefinedConstants undefinedMFConstants;
	private Values definedMFConstants;
	private Values definedPFConstants;

	// results
	private ResultsCollection results[] = null;

	// time for transient computation
	private String transientTime;

	// simulation info
	private String simMethodName = null;
	private double simApprox;
	private double simConfidence;
	private int simNumSamples;
	private double simWidth;
	private int reqIterToConclude;
	private double simMaxReward;
	private long simMaxPath;
	private boolean simApproxGiven = false;
	private boolean simConfidenceGiven = false;
	private boolean simNumSamplesGiven = false;
	private boolean simWidthGiven = false;
	private boolean reqIterToConcludeGiven = false;
	private boolean simMaxRewardGiven = false;
	private boolean simMaxPathGiven = false;
	private boolean simManual = false;
	private SimulationMethod simMethod = null;

	// strategy export info
	private Prism.StrategyExportType exportStratType = StrategyExportType.ACTIONS;
	
	// parametric analysis info
	private String[] paramLowerBounds = null;
	private String[] paramUpperBounds = null;
	private String[] paramNames = null;

	// entry point - run method

	public void run(String[] args)
	{
		int i, j, k;
		Result res;

		// Initialise
		initialise(args);

		// Parse/load model/properties
		doParsing();

		// Sort out properties to check
		sortProperties();

		// process info about undefined constants
		try {
			// first, see which constants are undefined
			// (one set of info for model, and one set of info for each property)
			if (exportlabels)
				undefinedMFConstants = new UndefinedConstants(modulesFile, propertiesFile, true);
			else
				undefinedMFConstants = new UndefinedConstants(modulesFile, null);
			undefinedConstants = new UndefinedConstants[numPropertiesToCheck];
			for (i = 0; i < numPropertiesToCheck; i++) {
				undefinedConstants[i] = new UndefinedConstants(modulesFile, propertiesFile, propertiesToCheck.get(i));
			}
			// may need to remove some constants if they are used for parametric methods
			if (param) {
				undefinedMFConstants.removeConstants(paramNames);
				for (i = 0; i < numPropertiesToCheck; i++) {
					undefinedConstants[i].removeConstants(paramNames);
				}
			}
			// then set up value using const switch definitions
			undefinedMFConstants.defineUsingConstSwitch(constSwitch);
			for (i = 0; i < numPropertiesToCheck; i++) {
				undefinedConstants[i].defineUsingConstSwitch(constSwitch);
			}
		} catch (PrismException e) {
			errorAndExit(e.getMessage());
		}

		// initialise storage for results
		results = new ResultsCollection[numPropertiesToCheck];
		for (i = 0; i < numPropertiesToCheck; i++) {
			results[i] = new ResultsCollection(undefinedConstants[i], propertiesToCheck.get(i).getExpression().getResultName());
		}

		// iterate through as many models as necessary
		for (i = 0; i < undefinedMFConstants.getNumModelIterations(); i++) {

			// set values for ModulesFile constants
			try {
				definedMFConstants = undefinedMFConstants.getMFConstantValues();
				prism.setPRISMModelConstants(definedMFConstants);
			} catch (PrismException e) {
				// in case of error, report it, store as result for any properties, and go on to the next model
				// (might happen for example if overflow or another numerical problem is detected at this stage)
				error(e.getMessage());
				for (j = 0; j < numPropertiesToCheck; j++) {
					results[j].setMultipleErrors(definedMFConstants, null, e);
				}
				// iterate to next model
				undefinedMFConstants.iterateModel();
				for (j = 0; j < numPropertiesToCheck; j++) {
					undefinedConstants[j].iterateModel();
				}
				continue;
			}

			modelBuildFail = false;

			// if requested, generate a random path with the simulator
			if (simpath) {
				try {
					if (!simMaxPathGiven)
						simMaxPath = prism.getSettings().getLong(PrismSettings.SIMULATOR_DEFAULT_MAX_PATH);
					File f = (simpathFilename.equals("stdout")) ? null : new File(simpathFilename);
					prism.generateSimulationPath(modulesFile, simpathDetails, simMaxPath, f);
				} catch (PrismException e) {
					error(e.getMessage());
				}
				// iterate to next model
				undefinedMFConstants.iterateModel();
				for (j = 0; j < numPropertiesToCheck; j++) {
					undefinedConstants[j].iterateModel();
				}
			}

			// Do any model exports
			doExports();
			if (modelBuildFail)
				continue;

			// Do steady-state/transient probability computation, if required
			doSteadyState();
			if (modelBuildFail)
				continue;
			doTransient();
			if (modelBuildFail)
				continue;

			// Work through list of properties to be checked
			for (j = 0; j < numPropertiesToCheck; j++) {

				// for simulation we can do multiple values of property constants simultaneously
				if (simulate && undefinedConstants[j].getNumPropertyIterations() > 1) {
					try {
						simMethod = processSimulationOptions(propertiesToCheck.get(j).getExpression());
						prism.modelCheckSimulatorExperiment(propertiesFile, undefinedConstants[j], results[j], propertiesToCheck.get(j).getExpression(), null,
								simMaxPath, simMethod);
					} catch (PrismException e) {
						// in case of (overall) error, report it, store as result for property, and proceed
						error(e.getMessage());
						results[j].setMultipleErrors(definedMFConstants, null, e);
						continue;
					} catch (InterruptedException e) {
						// ignore - won't get interrupted
					}
				}
				// otherwise, treat each case individually
				else {
					for (k = 0; k < undefinedConstants[j].getNumPropertyIterations(); k++) {

						try {
							// Set values for PropertiesFile constants
							if (propertiesFile != null) {
								definedPFConstants = undefinedConstants[j].getPFConstantValues();
								propertiesFile.setSomeUndefinedConstants(definedPFConstants);
							}
							// Normal model checking
							if (!simulate && !param) {
								res = prism.modelCheck(propertiesFile, propertiesToCheck.get(j));
							}
							// Parametric model checking
							else if (param) {
								res = prism.modelCheckParametric(propertiesFile, propertiesToCheck.get(j), paramNames, paramLowerBounds, paramUpperBounds);
							}
							// Approximate (simulation-based) model checking
							else if (simulate) {
								simMethod = processSimulationOptions(propertiesToCheck.get(j).getExpression());
								res = prism.modelCheckSimulator(propertiesFile, propertiesToCheck.get(j).getExpression(), definedPFConstants, null, simMaxPath,
										simMethod);
								simMethod.reset();
							} else {
								throw new PrismException("Cannot use parametric model checking and simulation at the same time");
							}
						} catch (PrismException e) {
							// in case of error, report it, store exception as the result and proceed
							error(e.getMessage(), true);
							res = new Result(e);
						}

						// in case of build failure during model checking, store as result for any const values and continue
						if (modelBuildFail) {
							results[j].setMultipleErrors(definedMFConstants, null, modelBuildException);
							break;
						}

						// store result of model checking
						results[j].setResult(definedMFConstants, definedPFConstants, res.getResult());

						// if a counterexample was generated, display it
						Object cex = res.getCounterexample();
						if (cex != null) {
							mainLog.println("\nCounterexample/witness:");
							mainLog.println(cex);
							/*SimulatorEngine engine = prism.getSimulator();
							try {
								engine.loadPath(modulesFile, (CexPathStates) cex);
								engine.exportPath(null, true, ",", null);
							} catch (PrismException e) {
								error(e.getMessage());
							}*/
							if (cex instanceof cex.CexPathAsBDDs) {
								((cex.CexPathAsBDDs) cex).clear();
							}
						}

						// if a strategy was generated, and we need to export it, do so
						if (exportstrat && res.getStrategy() != null) {
							try {
								prism.exportStrategy(res.getStrategy(), exportStratType, exportStratFilename.equals("stdout") ? null : new File(exportStratFilename));
							}
							// in case of error, report it and proceed
							catch (FileNotFoundException e) {
								error("Couldn't open file \"" + exportStratFilename + "\" for output");
							} catch (PrismException e) {
								error(e.getMessage());
							}
						}

						// if required, check result against expected value
						if (test) {
							try {
								mainLog.println();
								Values allConsts = new Values(definedMFConstants);
								allConsts.addValues(definedPFConstants);
								if (propertiesToCheck.get(j).checkAgainstExpectedResult(res.getResult(), allConsts)) {
									mainLog.println("Testing result: PASS");
								} else {
									mainLog.println("Testing result: NOT TESTED");
								}
							} catch (PrismException e) {
								mainLog.println("Testing result: FAIL: " + e.getMessage());
								if (testExitsOnFail)
									errorAndExit("Testing failed");
							}
						}

						// iterate to next property
						undefinedConstants[j].iterateProperty();
					}
				}

				// in case of build failure during model checking, store as result for any further properties and continue
				if (modelBuildFail) {
					for (j++; j < numPropertiesToCheck; j++) {
						results[j].setMultipleErrors(definedMFConstants, null, modelBuildException);
					}
					break;
				}
			}

			// Explicitly request a build if necessary
			if (propertiesToCheck.size() == 0 && !steadystate && !dotransient && !simpath && !nobuild && prism.modelCanBeBuilt() && !prism.modelIsBuilt()) {
				try {
					prism.buildModel();
				} catch (PrismException e) {
					error(e.getMessage());
				}
			}

			// iterate to next model
			undefinedMFConstants.iterateModel();
			for (j = 0; j < numPropertiesToCheck; j++) {
				undefinedConstants[j].iterateModel();
			}
		}

		// export results (if required)
		if (exportresults) {

			mainLog.print("\nExporting results ");
			if (exportresultsmatrix)
				mainLog.print("in matrix form ");
			if (!exportResultsFilename.equals("stdout"))
				mainLog.println("to file \"" + exportResultsFilename + "\"...");
			else
				mainLog.println("below:\n");
			PrismFileLog tmpLog = new PrismFileLog(exportResultsFilename);
			if (!tmpLog.ready()) {
				errorAndExit("Couldn't open file \"" + exportResultsFilename + "\" for output");
			}

			String sep = exportresultscsv ? ", " : "\t";
			for (i = 0; i < numPropertiesToCheck; i++) {
				if (i > 0)
					tmpLog.println();
				if (numPropertiesToCheck > 1) {
					if (sep.equals(", "))
						tmpLog.print("\"" + propertiesToCheck.get(i) + ":\"\n");
					else
						tmpLog.print(propertiesToCheck.get(i) + ":\n");
				}
				if (!exportresultsmatrix) {
					tmpLog.println(results[i].toString(false, sep, sep));
				} else {
					tmpLog.println(results[i].toStringMatrix(sep));
				}
			}
			tmpLog.close();
		}

		// close down
		closeDown();
	}

	/**
	 * Initialise.
	 */
	private void initialise(String[] args)
	{
		try {
			// prepare storage for parametric model checking
			// default to logs going to stdout
			// this means all errors etc. can be safely sent to the log
			// even if a new log is created shortly
			mainLog = new PrismFileLog("stdout");
			techLog = new PrismFileLog("stdout");

			// create prism object(s)
			prism = new Prism(mainLog, techLog);
			prism.addModelListener(this);

			// parse command line arguments
			parseArguments(args);

			// load setting file if requested
			if (settingsFilename != null)
				prism.loadUserSettingsFile(new File(settingsFilename));

			// initialise
			prism.initialise();

			// print command line for reference
			printArguments(args);

			// do some processing of the options
			processOptions();
		} catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
	}

	/**
	 * Parse model and properties, load model into PRISM. 
	 */
	private void doParsing()
	{
		int i;
		File sf = null, lf = null;

		// parse model

		try {
			if (importpepa) {
				mainLog.print("\nImporting PEPA file \"" + modelFilename + "\"...\n");
				modulesFile = prism.importPepaFile(new File(modelFilename));
			} else if (importprismpp) {
				mainLog.print("\nImporting PRISM preprocessor file \"" + modelFilename + "\"...\n");
				String prismppParamsList[] = ("? " + prismppParams).split(" ");
				modulesFile = prism.importPrismPreprocFile(new File(modelFilename), prismppParamsList);
			} else if (importtrans) {
				mainLog.print("\nImporting model (");
				mainLog.print(typeOverride == null ? "MDP" : typeOverride);
				mainLog.print(") from \"" + modelFilename + "\"");
				if (importstates) {
					mainLog.print(", \"" + importStatesFilename + "\"");
					sf = new File(importStatesFilename);
				}
				if (importlabels) {
					mainLog.print(", \"" + importLabelsFilename + "\"");
					lf = new File(importLabelsFilename);
				}
				mainLog.println("...");
				modulesFile = prism.loadModelFromExplicitFiles(sf, new File(modelFilename), lf, typeOverride);
			} else {
				mainLog.print("\nParsing model file \"" + modelFilename + "\"...\n");
				modulesFile = prism.parseModelFile(new File(modelFilename), typeOverride);
			}
		} catch (FileNotFoundException e) {
			errorAndExit("File \"" + modelFilename + "\" not found");
		} catch (PrismException e) {
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
			} else {
				propertiesFile = null;
			}
		} catch (FileNotFoundException e) {
			errorAndExit("File \"" + propertiesFilename + "\" not found");
		} catch (PrismException e) {
			errorAndExit(e.getMessage());
		}

		// print out properties (if any)

		if (propertiesFile != null) {
			mainLog.print("\n" + propertiesFile.getNumProperties());
			mainLog.print(" propert" + ((propertiesFile.getNumProperties() == 1) ? "y" : "ies") + ":\n");
			for (i = 0; i < propertiesFile.getNumProperties(); i++) {
				mainLog.println("(" + (i + 1) + ") " + propertiesFile.getPropertyObject(i));
			}
		}

		// Load model into PRISM (if not done already)
		if (!importtrans) {
			prism.loadPRISMModel(modulesFile);
		}
	}

	/**
	 * Sort out which properties need checking. 
	 */
	private void sortProperties()
	{
		int i;

		propertiesToCheck = new ArrayList<Property>();

		// no properties to check
		if (propertiesFile == null) {
			numPropertiesToCheck = 0;
		}
		// unless specified, verify all properties
		else if (propertyToCheck == null) {
			numPropertiesToCheck = propertiesFile.getNumProperties();
			for (i = 0; i < numPropertiesToCheck; i++) {
				propertiesToCheck.add(propertiesFile.getPropertyObject(i));
			}
		}
		// otherwise just verify the relevant property
		else {
			if (propertyToCheck instanceof Integer) {
				int propIndex = (Integer) propertyToCheck;
				if (propIndex <= 0 || propIndex > propertiesFile.getNumProperties())
					errorAndExit("There is not a property " + propIndex + " to verify");
				numPropertiesToCheck = 1;
				propertiesToCheck.add(propertiesFile.getPropertyObject(propIndex - 1));
			} else if (propertyToCheck instanceof String) {
				Property p = propertiesFile.getPropertyObjectByName((String) propertyToCheck);
				if (p == null)
					errorAndExit("There is not a property \"" + propertyToCheck + "\" to check");
				numPropertiesToCheck = 1;
				propertiesToCheck.add(p);
			} else {
				errorAndExit("There is not a property " + propertyToCheck + " to check");
			}
		}
	}

	// do any exporting requested

	private void doExports()
	{
		// export transition matrix to a file
		if (exporttrans) {
			try {
				File f = (exportTransFilename.equals("stdout")) ? null : new File(exportTransFilename);
				prism.exportTransToFile(exportordered, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}

			if (exportPlainDeprecated)
				mainLog.printWarning("The -exportplain switch is now deprecated. Please use -exporttrans in future.");
		}

		// export state rewards to a file
		if (exportstaterewards) {
			try {
				File f = (exportStateRewardsFilename.equals("stdout")) ? null : new File(exportStateRewardsFilename);
				prism.exportStateRewardsToFile(exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportStateRewardsFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export transition rewards to a file
		if (exporttransrewards) {
			try {
				File f = (exportTransRewardsFilename.equals("stdout")) ? null : new File(exportTransRewardsFilename);
				prism.exportTransRewardsToFile(exportordered, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransRewardsFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export states list
		if (exportstates) {
			try {
				File f = (exportStatesFilename.equals("stdout")) ? null : new File(exportStatesFilename);
				prism.exportStatesToFile(exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportStatesFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export to spy file
		if (exportspy) {
			try {
				prism.exportToSpyFile(new File(exportSpyFilename));
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportSpyFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export mtbdd to dot file
		if (exportdot) {
			try {
				prism.exportToDotFile(new File(exportDotFilename));
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportDotFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export transition matrix graph to dot file
		if (exporttransdot) {
			try {
				File f = (exportTransDotFilename.equals("stdout")) ? null : new File(exportTransDotFilename);
				prism.exportTransToFile(exportordered, Prism.EXPORT_DOT, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransDotFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export transition matrix graph to dot file (with states)
		if (exporttransdotstates) {
			try {
				File f = (exportTransDotStatesFilename.equals("stdout")) ? null : new File(exportTransDotStatesFilename);
				prism.exportTransToFile(exportordered, Prism.EXPORT_DOT_STATES, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportTransDotStatesFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export labels/states
		if (exportlabels) {
			try {
				if (propertiesFile != null) {
					definedPFConstants = undefinedMFConstants.getPFConstantValues();
					propertiesFile.setSomeUndefinedConstants(definedPFConstants);
				}
				File f = (exportLabelsFilename.equals("stdout")) ? null : new File(exportLabelsFilename);
				prism.exportLabelsToFile(propertiesFile, exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				mainLog.println("Couldn't open file \"" + exportLabelsFilename + "\" for output");
			} catch (PrismException e) {
				mainLog.println("\nError: " + e.getMessage() + ".");
			}
		}

		// export SCCs to a file
		if (exportsccs) {
			try {
				File f = (exportSCCsFilename.equals("stdout")) ? null : new File(exportSCCsFilename);
				prism.exportSCCsToFile(exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportSCCsFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export BSCCs to a file
		if (exportbsccs) {
			try {
				File f = (exportBSCCsFilename.equals("stdout")) ? null : new File(exportBSCCsFilename);
				prism.exportBSCCsToFile(exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportBSCCsFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}

		// export MECs to a file
		if (exportmecs) {
			try {
				File f = (exportMECsFilename.equals("stdout")) ? null : new File(exportMECsFilename);
				prism.exportMECsToFile(exportType, f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportMECsFilename + "\" for output");
			} catch (PrismException e) {
				error(e.getMessage());
			}
		}
	}

	/**
	 * Do steady-state probability computation (if required).
	 */
	private void doSteadyState()
	{
		File exportSteadyStateFile = null;

		if (steadystate) {
			try {
				// Choose destination for output (file or log)
				if (exportSteadyStateFilename == null || exportSteadyStateFilename.equals("stdout"))
					exportSteadyStateFile = null;
				else
					exportSteadyStateFile = new File(exportSteadyStateFilename);
				// Compute steady-state probabilities
				prism.doSteadyState(exportType, exportSteadyStateFile, importinitdist ? new File(importInitDistFilename) : null);
			} catch (PrismException e) {
				// In case of error, report it and proceed
				error(e.getMessage());
			}
		}
	}

	/**
	 * Do transient probability computation (if required).
	 */
	private void doTransient()
	{
		ModelType modelType;
		File exportTransientFile = null;

		if (dotransient) {
			try {
				// Choose destination for output (file or log)
				if (exportTransientFilename == null || exportTransientFilename.equals("stdout"))
					exportTransientFile = null;
				else
					exportTransientFile = new File(exportTransientFilename);

				// Determine model type
				modelType = prism.getModelType();

				// Parse time specification, store as UndefinedConstant for constant T
				String timeType = modelType.continuousTime() ? "double" : "int";
				UndefinedConstants ucTransient = new UndefinedConstants(null, prism.parsePropertiesString(null, "const " + timeType + " T; T;"));
				try {
					ucTransient.defineUsingConstSwitch("T=" + transientTime);
				} catch (PrismException e) {
					if (transientTime.contains(":"))
						errorAndExit("\"" + transientTime + "\" is not a valid time range for a " + modelType);
					else
						errorAndExit("\"" + transientTime + "\" is not a valid time for a " + modelType);
				}

				// Compute transient probabilities
				prism.doTransient(ucTransient, exportType, exportTransientFile, importinitdist ? new File(importInitDistFilename) : null);
			}
			// In case of error, report it and proceed
			catch (PrismException e) {
				error(e.getMessage());
			}
		}
	}

	/**
	 * Close down.
	 */
	private void closeDown()
	{
		// clear up and close down
		prism.closeDown(true);
		// notify about any warnings
		int numWarnings = mainLog.getNumberOfWarnings();
		if (numWarnings > 0) {
			mainLog.printSeparator();
			mainLog.print("\nNote: There ");
			if (numWarnings == 1)
				mainLog.print("was 1 warning");
			else
				mainLog.print("were " + numWarnings + " warnings");
			mainLog.println(" during computation.");
		}
		mainLog.println();
		// Close logs (in case they are files)
		mainLog.close();
		techLog.close();
	}

	// PrismModelListener methods

	@Override
	public void notifyModelBuildSuccessful()
	{
	}

	@Override
	public void notifyModelBuildFailed(PrismException e)
	{
		modelBuildFail = true;
		modelBuildException = e;
	}

	/**
	 * Process command-line arguments/switches.
	 */
	private void parseArguments(String[] args) throws PrismException
	{
		int i, j;
		String sw, s;
		PrismLog log;

		constSwitch = "";
		paramSwitch = "";

		for (i = 0; i < args.length; i++) {

			// if is a switch...
			if (args[i].length() > 0 && args[i].charAt(0) == '-') {

				// Remove "-"
				sw = args[i].substring(1);
				// Remove optional second "-" (i.e. we allow switches of the form --sw too)
				if (sw.charAt(0) == '-')
					sw = sw.substring(1);

				// Note: the order of these switches should match the -help output (just to help keep track of things).
				// But: processing of "PRISM" options is done elsewhere in PrismSettings
				// Any "hidden" options, i.e. not in -help text/manual, are indicated as such.

				// print help
				if (sw.equals("help") || sw.equals("?")) {
					// see if userg requested help for a specific switch, e.g. -help simpath
					// note: this is one of the few places where a second argument is optional,
					// which is possible here because -help should usually be the only switch provided
					if (i < args.length - 1) {
						printHelpSwitch(args[++i]);
					} else {
						printHelp();
					}
					exit();
				}
				// print version
				else if (sw.equals("version")) {
					printVersion();
					exit();
				}
				// load settings
				else if (sw.equals("settings")) {
					if (i < args.length - 1) {
						settingsFilename = args[++i].trim();
					} else {
						errorAndExit("Incomplete -" + sw + " switch");
					}
				}
				// print a list of all keywords (hidden option)
				else if (sw.equals("keywords")) {
					printListOfKeywords();
					exit();
				}

				// property/properties given in command line
				else if (sw.equals("pf") || sw.equals("pctl") || sw.equals("csl")) {
					if (i < args.length - 1) {
						propertyString = args[++i];
					} else {
						errorAndExit("No property specified for -" + sw + " switch");
					}
				}
				// which property to check (int index or string name)
				else if (sw.equals("prop") || sw.equals("property")) {
					if (i < args.length - 1) {
						try {
							propertyToCheck = Integer.parseInt(args[++i]);
						} catch (NumberFormatException e) {
							propertyToCheck = args[i];
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// definition of undefined constants
				else if (sw.equals("const")) {
					if (i < args.length - 1) {
						// store argument for later use (append if already partially specified)
						if ("".equals(constSwitch))
							constSwitch = args[++i].trim();
						else
							constSwitch += "," + args[++i].trim();
					} else {
						errorAndExit("Incomplete -" + sw + " switch");
					}
				}
				// defining a parameter
				else if (sw.equals("param")) {
					param = true;
					if (i < args.length - 1) {
						// store argument for later use (append if already partially specified)
						if ("".equals(paramSwitch)) {
							paramSwitch = args[++i].trim();
						} else {
							paramSwitch += "," + args[++i].trim();
						}
					} else {
						errorAndExit("Incomplete -" + sw + " switch");
					}
				}
				// do steady-state probability computation
				else if (sw.equals("steadystate") || sw.equals("ss")) {
					steadystate = true;
				}
				// do transient probability computation
				else if (sw.equals("transient") || sw.equals("tr")) {
					if (i < args.length - 1) {
						dotransient = true;
						transientTime = args[++i];
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// generate random path with simulator
				else if (sw.equals("simpath")) {
					if (i < args.length - 2) {
						simpath = true;
						simpathDetails = args[++i];
						simpathFilename = args[++i];
					} else {
						errorAndExit("The -" + sw + " switch requires two arguments (path details, filename)");
					}
				}
				// disable model construction
				else if (sw.equals("nobuild")) {
					nobuild = true;
				}
				// enable "testing" mode
				else if (sw.equals("test")) {
					test = true;
				}
				// enable "test all" mode (don't stop on errors)
				// (overrides -test switch)
				else if (sw.equals("testall")) {
					test = true;
					testExitsOnFail = false;
				}

				// IMPORT OPTIONS:

				// change model type to pepa
				else if (sw.equals("importpepa")) {
					importpepa = true;
				}
				// Import model from PRISM preprocessor (hidden option)
				else if (sw.equals("importprismpp")) {
					if (i < args.length - 1) {
						importprismpp = true;
						prismppParams = args[++i];
					} else {
						errorAndExit("No parameters specified for -" + sw + " switch");
					}
				}
				// import model from explicit file(s)
				else if (sw.equals("importmodel")) {
					if (i < args.length - 1) {
						processImportModelSwitch(args[++i]);
					} else {
						errorAndExit("No file/options specified for -" + sw + " switch");
					}
				}
				// import transition matrix from explicit format
				else if (sw.equals("importtrans")) {
					importtrans = true;
				}
				// import states for explicit model import
				else if (sw.equals("importstates")) {
					if (i < args.length - 1) {
						importstates = true;
						importStatesFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// import labels for explicit model import
				else if (sw.equals("importlabels")) {
					if (i < args.length - 1) {
						importlabels = true;
						importLabelsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// import initial distribution e.g. for transient probability distribution
				else if (sw.equals("importinitdist")) {
					if (i < args.length - 1) {
						importinitdist = true;
						importInitDistFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// override model type to dtmc
				else if (sw.equals("dtmc")) {
					typeOverride = ModelType.DTMC;
				}
				// override model type to mdp
				else if (sw.equals("mdp")) {
					typeOverride = ModelType.MDP;
				}
				// override model type to ctmc
				else if (sw.equals("ctmc")) {
					typeOverride = ModelType.CTMC;
				}

				// EXPORT OPTIONS:

				// export results
				else if (sw.equals("exportresults")) {
					if (i < args.length - 1) {
						exportresults = true;
						// Parse filename/options
						s = args[++i];
						// Assume use of : to split filename/options but check for , if : not found
						// (this was the old notation)
						String halves[] = splitFilesAndOptions(s);
						if (halves[1].length() == 0 && halves[0].indexOf(',') > -1) {
							int comma = halves[0].indexOf(',');
							halves[1] = halves[0].substring(comma + 1);
							halves[0] = halves[0].substring(0, comma);
						}
						exportResultsFilename = halves[0];
						String ss[] = halves[1].split(",");
						for (j = 0; j < ss.length; j++) {
							if (ss[j].equals("")) {
							} else if (ss[j].equals("csv"))
								exportresultscsv = true;
							else if (ss[j].equals("matrix"))
								exportresultsmatrix = true;
							else
								errorAndExit("Unknown option \"" + ss[j] + "\" for -" + sw + " switch");
						}
					} else {
						errorAndExit("No file/options specified for -" + sw + " switch");
					}
				}
				// export model to explicit file(s)
				else if (sw.equals("exportmodel")) {
					if (i < args.length - 1) {
						processExportModelSwitch(args[++i]);
					} else {
						errorAndExit("No file/options specified for -" + sw + " switch");
					}
				}
				// export transition matrix to file
				else if (sw.equals("exporttrans")) {
					if (i < args.length - 1) {
						exporttrans = true;
						exportTransFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export state rewards to file
				else if (sw.equals("exportstaterewards")) {
					if (i < args.length - 1) {
						exportstaterewards = true;
						exportStateRewardsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export transition rewards to file
				else if (sw.equals("exporttransrewards")) {
					if (i < args.length - 1) {
						exporttransrewards = true;
						exportTransRewardsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export both state/transition rewards to file
				else if (sw.equals("exportrewards")) {
					if (i < args.length - 2) {
						exportstaterewards = true;
						exporttransrewards = true;
						exportStateRewardsFilename = args[++i];
						exportTransRewardsFilename = args[++i];
					} else {
						errorAndExit("Two files must be specified for -" + sw + " switch");
					}
				}
				// export states
				else if (sw.equals("exportstates")) {
					if (i < args.length - 1) {
						exportstates = true;
						exportStatesFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export labels/states
				else if (sw.equals("exportlabels")) {
					if (i < args.length - 1) {
						exportlabels = true;
						exportLabelsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
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
				// exported matrix entries are ordered
				else if (sw.equals("exportordered") || sw.equals("ordered")) {
					exportordered = true;
				}
				// exported matrix entries are unordered
				else if (sw.equals("exportunordered") || sw.equals("unordered")) {
					exportordered = false;
				}
				// export transition matrix graph to dot file
				else if (sw.equals("exporttransdot")) {
					if (i < args.length - 1) {
						exporttransdot = true;
						exportTransDotFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export transition matrix graph to dot file (with states)
				else if (sw.equals("exporttransdotstates")) {
					if (i < args.length - 1) {
						exporttransdotstates = true;
						exportTransDotStatesFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export transition matrix MTBDD to dot file
				else if (sw.equals("exportdot")) {
					if (i < args.length - 1) {
						exportdot = true;
						exportDotFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export sccs to file
				else if (sw.equals("exportsccs")) {
					if (i < args.length - 1) {
						exportsccs = true;
						exportSCCsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export bsccs to file
				else if (sw.equals("exportbsccs")) {
					if (i < args.length - 1) {
						exportbsccs = true;
						exportBSCCsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export mecs to file
				else if (sw.equals("exportmecs")) {
					if (i < args.length - 1) {
						exportmecs = true;
						exportMECsFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export steady-state probs (as opposed to displaying on screen) 
				else if (sw.equals("exportsteadystate") || sw.equals("exportss")) {
					if (i < args.length - 1) {
						exportSteadyStateFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export transient probs (as opposed to displaying on screen) 
				else if (sw.equals("exporttransient") || sw.equals("exporttr")) {
					if (i < args.length - 1) {
						exportTransientFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export strategy
				else if (sw.equals("exportstrat")) {
					if (i < args.length - 1) {
						processExportStratSwitch(args[++i]);
					} else {
						errorAndExit("No file/options specified for -" + sw + " switch");
					}
				}
				// export prism model to file
				else if (sw.equals("exportprism")) {
					if (i < args.length - 1) {
						String filename = args[++i];
						File f = (filename.equals("stdout")) ? null : new File(filename);
						prism.setExportPrism(true);
						prism.setExportPrismFile(f);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export prism model to file (with consts expanded)
				else if (sw.equals("exportprismconst")) {
					if (i < args.length - 1) {
						String filename = args[++i];
						File f = (filename.equals("stdout")) ? null : new File(filename);
						prism.setExportPrismConst(true);
						prism.setExportPrismConstFile(f);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export digital clocks translation prism model to file
				else if (sw.equals("exportdigital")) {
					if (i < args.length - 1) {
						String filename = args[++i];
						File f = (filename.equals("stdout")) ? null : new File(filename);
						prism.setExportDigital(true);
						prism.setExportDigitalFile(f);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export reachability target info to file (hidden option)
				else if (sw.equals("exporttarget")) {
					if (i < args.length - 1) {
						prism.setExportTarget(true);
						prism.setExportTargetFilename(args[++i]);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export product transition matrix to file (hidden option)
				else if (sw.equals("exportprodtrans")) {
					if (i < args.length - 1) {
						prism.setExportProductTrans(true);
						prism.setExportProductTransFilename(args[++i]);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export product states to file (hidden option)
				else if (sw.equals("exportprodstates")) {
					if (i < args.length - 1) {
						prism.setExportProductStates(true);
						prism.setExportProductStatesFilename(args[++i]);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export model to plain text file (deprecated option so hidden)
				else if (sw.equals("exportplain")) {
					if (i < args.length - 1) {
						exporttrans = true;
						exportType = Prism.EXPORT_PLAIN;
						exportTransFilename = args[++i];
						exportPlainDeprecated = true;
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// export to spy file (hidden option)
				else if (sw.equals("exportspy")) {
					if (i < args.length - 1) {
						exportspy = true;
						exportSpyFilename = args[++i];
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}

				// NB: Following the ordering of the -help text, more options go here,
				// but these are processed in the PrismSettings class; see below 

				// SIMULATION OPTIONS:

				// use simulator for approximate/statistical model checking
				else if (sw.equals("sim")) {
					simulate = true;
				}
				// simulation-based model checking methods
				else if (sw.equals("simmethod")) {
					if (i < args.length - 1) {
						s = args[++i];
						if (s.equals("ci") || s.equals("aci") || s.equals("apmc") || s.equals("sprt"))
							simMethodName = s;
						else
							errorAndExit("Unrecognised option for -" + sw + " switch (options are: ci, aci, apmc, sprt)");
					} else {
						errorAndExit("No parameter specified for -" + sw + " switch");
					}
				}
				// simulation number of samples
				else if (sw.equals("simsamples")) {
					if (i < args.length - 1) {
						try {
							simNumSamples = Integer.parseInt(args[++i]);
							if (simNumSamples <= 0)
								throw new NumberFormatException("");
							simNumSamplesGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// simulation confidence parameter
				else if (sw.equals("simconf")) {
					if (i < args.length - 1) {
						try {
							simConfidence = Double.parseDouble(args[++i]);
							if (simConfidence <= 0 || simConfidence >= 1)
								throw new NumberFormatException("");
							simConfidenceGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// simulation confidence interval width
				else if (sw.equals("simwidth")) {
					if (i < args.length - 1) {
						try {
							simWidth = Double.parseDouble(args[++i]);
							if (simWidth <= 0)
								throw new NumberFormatException("");
							simWidthGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// simulation approximation parameter
				else if (sw.equals("simapprox")) {
					if (i < args.length - 1) {
						try {
							simApprox = Double.parseDouble(args[++i]);
							if (simApprox <= 0)
								throw new NumberFormatException("");
							simApproxGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// use the number of iterations given instead of automatically deciding whether the variance is null ot not
				else if (sw.equals("simmanual")) {
					simManual = true;
				}
				// simulation number of samples to conclude S^2=0 or not
				else if (sw.equals("simvar")) {
					if (i < args.length - 1) {
						try {
							reqIterToConclude = Integer.parseInt(args[++i]);
							if (reqIterToConclude <= 0)
								throw new NumberFormatException("");
							reqIterToConcludeGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// maximum value of reward
				else if (sw.equals("simmaxrwd")) {
					if (i < args.length - 1) {
						try {
							simMaxReward = Double.parseDouble(args[++i]);
							if (simMaxReward <= 0.0)
								throw new NumberFormatException("");
							simMaxRewardGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}
				// simulation max path length
				else if (sw.equals("simpathlen")) {
					if (i < args.length - 1) {
						try {
							simMaxPath = Long.parseLong(args[++i]);
							if (simMaxPath <= 0)
								throw new NumberFormatException("");
							simMaxPathGiven = true;
						} catch (NumberFormatException e) {
							errorAndExit("Invalid value for -" + sw + " switch");
						}
					} else {
						errorAndExit("No value specified for -" + sw + " switch");
					}
				}

				// FURTHER OPTIONS - NEED TIDYING/FIXING

				// zero-reward loops check on
				else if (sw.equals("zerorewardcheck")) {
					prism.setCheckZeroLoops(true);
				}
				// explicit-state model construction
				else if (sw.equals("explicitbuild")) {
					explicitbuild = true;
				}
				// (hidden) option for testing of prototypical explicit-state model construction
				else if (sw.equals("explicitbuildtest")) {
					explicitbuildtest = true;
				}

				// MISCELLANEOUS UNDOCUMENTED/UNUSED OPTIONS:

				// specify main log (hidden option)
				else if (sw.equals("mainlog")) {
					if (i < args.length - 1) {
						mainLogFilename = args[++i];
						// use temporary storage because an error would go to the old log
						log = new PrismFileLog(mainLogFilename);
						if (!log.ready()) {
							errorAndExit("Couldn't open log file \"" + mainLogFilename + "\"");
						}
						mainLog = log;
						prism.setMainLog(mainLog);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// specify mtbdd log (hidden option)
				else if (sw.equals("techlog")) {
					if (i < args.length - 1) {
						techLogFilename = args[++i];
						log = new PrismFileLog(techLogFilename);
						if (!log.ready()) {
							errorAndExit("Couldn't open log file \"" + techLogFilename + "\"");
						}
						techLog = log;
						prism.setTechLog(techLog);
					} else {
						errorAndExit("No file specified for -" + sw + " switch");
					}
				}
				// mtbdd construction method (hidden option)
				else if (sw.equals("c1")) {
					prism.setConstruction(1);
				} else if (sw.equals("c2")) {
					prism.setConstruction(2);
				} else if (sw.equals("c3")) {
					prism.setConstruction(3);
				}
				// mtbdd variable ordering (hidden option)
				else if (sw.equals("o1")) {
					prism.setOrdering(1);
					orderingOverride = true;
				} else if (sw.equals("o2")) {
				} else if (sw.equals("o2")) {
					prism.setOrdering(2);
					orderingOverride = true;
				} else if (sw.equals("o2")) {
				}
				// reachability off (hidden option)
				else if (sw.equals("noreach")) {
					prism.setDoReach(false);
				}
				// no bscc computation (hidden option)
				else if (sw.equals("nobscc")) {
					prism.setBSCCComp(false);
				}
				// reachability options (hidden options)
				else if (sw.equals("frontier")) {
					prism.setReachMethod(Prism.REACH_FRONTIER);
				} else if (sw.equals("bfs")) {
					prism.setReachMethod(Prism.REACH_BFS);
				}
				// enable bisimulation minimisation before model checking (hidden option)
				else if (sw.equals("bisim")) {
					prism.setDoBisim(true);
				}

				// Other switches - pass to PrismSettings

				else {
					i = prism.getSettings().setFromCommandLineSwitch(args, i) - 1;
				}
			}
			// otherwise argument must be a filename
			else if (modelFilename == null) {
				modelFilename = args[i];
			} else if (propertiesFilename == null) {
				propertiesFilename = args[i];
			}
			// anything else - must be something wrong with command line syntax
			else {
				errorAndExit("Invalid argument syntax");
			}
		}
	}

	/**
	 * Process the arguments (files, options) to the -importmodel switch
	 * NB: This is done at the time of parsing switches (not later)
	 * because other individual switches (e.g. -importXXX) can later override
	 * parts of the configurations set up here.
	 */
	private void processImportModelSwitch(String filesOptionsString) throws PrismException
	{
		// Split into files/options (on :)
		String halves[] = splitFilesAndOptions(filesOptionsString);
		String filesString = halves[0];
		String optionsString = halves[1];
		// Split files into basename/extensions
		int i = filesString.lastIndexOf('.');
		if (i == -1)
			throw new PrismException("No file name extension(s) in file(s) \"" + filesString + "\" for -importmodel");
		String basename = filesString.substring(0, i);
		String extList = filesString.substring(i + 1);
		String exts[] = extList.split(",");
		// Process file extensions
		for (String ext : exts) {
			// Items to import
			if (ext.equals("all")) {
				importtrans = true;
				modelFilename = basename + ".tra";
				importstates = true;
				importStatesFilename = basename + ".sta";
				importlabels = true;
				importLabelsFilename = basename + ".lab";
			} else if (ext.equals("tra")) {
				importtrans = true;
				modelFilename = basename + ".tra";
			} else if (ext.equals("tra")) {
				importtrans = true;
				modelFilename = basename + ".tra";
			} else if (ext.equals("sta")) {
				importstates = true;
				importStatesFilename = basename + ".sta";
			} else if (ext.equals("lab")) {
				importlabels = true;
				importLabelsFilename = basename + ".lab";
			}
			// Unknown extension
			else {
				throw new PrismException("Unknown extension \"" + ext + "\" for -exportmodel switch");
			}
			// Check at least the transition matrix was imported
			if (!importtrans) {
				throw new PrismException("You must import the transition matrix when using -importmodel (use option \"tra\" or \"all\")");
			}
		}
		// No options supported currently
		/*// Process options
		String options[] = optionsString.split(",");
		for (String opt : options) {
			// Ignore ""
			if (opt.equals("")) {
			}
			// Unknown option
			else {
				throw new PrismException("Unknown option \"" + opt + "\" for -exportmodel switch");
			}
		}*/
	}

	/**
	 * Process the arguments (files, options) to the -exportmodel switch
	 * NB: This is done at the time of parsing switches (not later)
	 * because other individual switches (e.g. -exportmatlab) can later override
	 * parts of the configurations set up here.
	 */
	private void processExportModelSwitch(String filesOptionsString) throws PrismException
	{
		// Split into files/options (on :)
		String halves[] = splitFilesAndOptions(filesOptionsString);
		String filesString = halves[0];
		String optionsString = halves[1];
		// Split files into basename/extensions
		int i = filesString.lastIndexOf('.');
		if (i == -1)
			throw new PrismException("No file name extension(s) in file(s) \"" + filesString + "\" for -exportmodel");
		String basename = filesString.substring(0, i);
		String extList = filesString.substring(i + 1);
		String exts[] = extList.split(",");
		// Check for empty base name (e.g. ".all") - will be replaced with modelname
		if (basename.length() == 0) {
			basename = "modelFileBasename";
			exportModelNoBasename = true;
		}
		// Process file extensions
		for (String ext : exts) {
			// Items to export
			if (ext.equals("all")) {
				exporttrans = true;
				exportTransFilename = basename.equals("stdout") ? "stdout" : basename + ".tra";
				exportstaterewards = true;
				exportStateRewardsFilename = basename.equals("stdout") ? "stdout" : basename + ".srew";
				exporttransrewards = true;
				exportTransRewardsFilename = basename.equals("stdout") ? "stdout" : basename + ".trew";
				exportstates = true;
				exportStatesFilename = basename.equals("stdout") ? "stdout" : basename + ".sta";
				exportlabels = true;
				exportLabelsFilename = basename.equals("stdout") ? "stdout" : basename + ".lab";
			} else if (ext.equals("tra")) {
				exporttrans = true;
				exportTransFilename = basename.equals("stdout") ? "stdout" : basename + ".tra";
			} else if (ext.equals("tra")) {
				exporttrans = true;
				exportTransFilename = basename.equals("stdout") ? "stdout" : basename + ".tra";
			} else if (ext.equals("srew")) {
				exportstaterewards = true;
				exportStateRewardsFilename = basename.equals("stdout") ? "stdout" : basename + ".srew";
			} else if (ext.equals("trew")) {
				exporttransrewards = true;
				exportTransRewardsFilename = basename.equals("stdout") ? "stdout" : basename + ".trew";
			} else if (ext.equals("rew")) {
				exportstaterewards = true;
				exportStateRewardsFilename = basename.equals("stdout") ? "stdout" : basename + ".srew";
				exporttransrewards = true;
				exportTransRewardsFilename = basename.equals("stdout") ? "stdout" : basename + ".trew";
			} else if (ext.equals("sta")) {
				exportstates = true;
				exportStatesFilename = basename.equals("stdout") ? "stdout" : basename + ".sta";
			} else if (ext.equals("lab")) {
				exportlabels = true;
				exportLabelsFilename = basename.equals("stdout") ? "stdout" : basename + ".lab";
			}
			// Unknown extension
			else {
				throw new PrismException("Unknown extension \"" + ext + "\" for -exportmodel switch");
			}
		}
		// Process options
		String options[] = optionsString.split(",");
		for (String opt : options) {
			// Ignore ""
			if (opt.equals("")) {
			}
			// Export type
			else if (opt.equals("matlab")) {
				exportType = Prism.EXPORT_MATLAB;
			} else if (opt.equals("mrmc")) {
				exportType = Prism.EXPORT_MRMC;
			} else if (opt.equals("rows")) {
				exportType = Prism.EXPORT_ROWS;
			} /*else if (opt.startsWith("type=")) {
				String exportTypeString = opt.substring(5);
				if (exportTypeString.equals("matlab")) {
					exportType = Prism.EXPORT_MATLAB;
				} else if (exportTypeString.equals("mrmc")) {
					exportType = Prism.EXPORT_MRMC;
				} else if (exportTypeString.equals("rows")) {
					exportType = Prism.EXPORT_ROWS;
				} else {
					throw new PrismException("Unknown type \"" + opt + "\" for -exportmodel switch");
				}
				}*/
			// Unordered/ordered
			else if (opt.equals("unordered")) {
				exportordered = false;
			} else if (opt.equals("ordered")) {
				exportordered = true;
			}
			// Unknown option
			else {
				throw new PrismException("Unknown option \"" + opt + "\" for -exportmodel switch");
			}
		}
	}

	/**
	 * Process the arguments (files, options) to the -exportstrat switch
	 */
	private void processExportStratSwitch(String filesOptionsString) throws PrismException
	{
		// Split into files/options (on :)
		String halves[] = splitFilesAndOptions(filesOptionsString);
		String fileString = halves[0];
		String optionsString = halves[1];
		// Store some settings (here and in PRISM)
		exportstrat = true;
		exportStratFilename = fileString;
		prism.setGenStrat(true);
		// Process options
		String options[] = optionsString.split(",");
		for (String opt : options) {
			// Ignore ""
			if (opt.equals("")) {
			}
			else if (opt.startsWith("type")) {
				if (!opt.startsWith("type="))
					throw new PrismException("No value provided for \"type\" option of -exportstrat");
				String optVal = opt.substring(5);
				if (optVal.equals("actions"))
					exportStratType = StrategyExportType.ACTIONS;
				else if (optVal.equals("indices"))
					exportStratType = StrategyExportType.INDICES;
				else if (optVal.equals("induced"))
					exportStratType = StrategyExportType.INDUCED_MODEL;
				else if (optVal.equals("dot"))
					exportStratType = StrategyExportType.DOT_FILE;
				else
					throw new PrismException("Unknown value \"" + optVal + "\" provided for \"type\" option of -exportstrat");
			}
			// Unknown option
			else {
				throw new PrismException("Unknown option \"" + opt + "\" for -exportstrat switch");
			}
		}
	}

	/**
	 * Split a string of the form <files>:<options> into its two parts.
	 * The latter can be empty, in which case the : is optional.
	 * Instances of :\ are ignored (nor treated as :) in case there is a Windows filename.
	 * @return the two parts as an array of two strings.
	 */
	private static String[] splitFilesAndOptions(String filesOptionsString)
	{
		String res[] = new String[2];
		// Split into files/options (on :)
		int i = filesOptionsString.indexOf(':');
		while (filesOptionsString.length() > i + 1 && filesOptionsString.charAt(i + 1) == '\\') {
			i = filesOptionsString.indexOf(':', i + 1);
		}
		if (i != -1) {
			res[0] = filesOptionsString.substring(0, i);
			res[1] = filesOptionsString.substring(i + 1);
		} else {
			res[0] = filesOptionsString;
			res[1] = "";
		}
		return res;
	}

	// print command line arguments

	public void printArguments(String[] args)
	{
		String s;
		int i;
		mainLog.print("Command line: prism");
		// Print out the command-line arguments
		for (i = 0; i < args.length; i++) {
			s = args[i];
			// If necessary add quotes so can be pasted back into a shell
			// (where "necessary" means contains any non-safe characters)
			if (s.matches(".*[^_a-zA-Z0-9\\./\\-=].*")) {
				s = "'" + s + "'";
			}
			mainLog.print(" " + s);
		}
		mainLog.println();
	}

	// do some processing of the options

	private void processOptions() throws PrismException
	{
		int j;

		// make sure a model file is specified
		if (modelFilename == null) {
			mainLog.println("Usage: prism [options] <model-file> [<properties-file>] [more-options]");
			mainLog.println("\nFor more information, type: prism -help");
			exit();
		}

		// default to alternative ordering for MTBDD engine
		if (prism.getEngine() == Prism.MTBDD && !orderingOverride) {
			try {
				prism.setOrdering(2);
			} catch (PrismException e) {
				// Can't go wrong
			}
		}

		// explicit overrides explicit build
		if (prism.getExplicit()) {
			explicitbuild = false;
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

		// process info about parametric constants
		if (param) {
			String[] paramDefs = paramSwitch.split(",");
			paramNames = new String[paramDefs.length];
			paramLowerBounds = new String[paramDefs.length];
			paramUpperBounds = new String[paramDefs.length];
			for (int pdNr = 0; pdNr < paramDefs.length; pdNr++) {
				if (!paramDefs[pdNr].contains("=")) {
					paramNames[pdNr] = paramDefs[pdNr];
					paramLowerBounds[pdNr] = "0";
					paramUpperBounds[pdNr] = "1";
				} else {
					String[] paramDefSplit = paramDefs[pdNr].split("=");
					paramNames[pdNr] = paramDefSplit[0];
					paramDefSplit[1] = paramDefSplit[1].trim();
					String[] upperLower = paramDefSplit[1].split(":");
					if (upperLower.length != 2)
						throw new PrismException("Invalid range \"" + paramDefSplit[1] + "\" for parameter " + paramNames[pdNr]);
					paramLowerBounds[pdNr] = upperLower[0].trim();
					paramUpperBounds[pdNr] = upperLower[1].trim();
				}
			}
		}

		// plug in basename for -exportmodel switch if needed
		if (exportModelNoBasename) {
			String modelFileBasename = modelFilename;
			if (modelFileBasename.lastIndexOf('.') > -1)
				modelFileBasename = modelFilename.substring(0, modelFileBasename.lastIndexOf('.'));
			if (exporttrans)
				exportTransFilename = exportTransFilename.replaceFirst("modelFileBasename", modelFileBasename);
			if (exportstaterewards)
				exportStateRewardsFilename = exportStateRewardsFilename.replaceFirst("modelFileBasename", modelFileBasename);
			if (exporttransrewards)
				exportTransRewardsFilename = exportTransRewardsFilename.replaceFirst("modelFileBasename", modelFileBasename);
			if (exportstates)
				exportStatesFilename = exportStatesFilename.replaceFirst("modelFileBasename", modelFileBasename);
			if (exportlabels)
				exportLabelsFilename = exportLabelsFilename.replaceFirst("modelFileBasename", modelFileBasename);
		}
	}

	/**
	 * Process the simulation-related command-line options and generate
	 * a SimulationMethod object to be used for approximate model checking.
	 * @param expr The property to be checked (note: constants may not be defined)
	 * @throws PrismException if there are problems with the specified options
	 */
	private SimulationMethod processSimulationOptions(Expression expr) throws PrismException
	{
		SimulationMethod aSimMethod = null;

		// See if property to be checked is a reward (R) operator
		boolean isReward = (expr instanceof ExpressionReward);

		// See if property to be checked is quantitative (=?)
		boolean isQuant = Expression.isQuantitative(expr);

		// Pick defaults for simulation settings not set from command-line
		if (!simApproxGiven)
			simApprox = prism.getSettings().getDouble(PrismSettings.SIMULATOR_DEFAULT_APPROX);
		if (!simConfidenceGiven)
			simConfidence = prism.getSettings().getDouble(PrismSettings.SIMULATOR_DEFAULT_CONFIDENCE);
		if (!simNumSamplesGiven)
			simNumSamples = prism.getSettings().getInteger(PrismSettings.SIMULATOR_DEFAULT_NUM_SAMPLES);
		if (!simWidthGiven)
			simWidth = prism.getSettings().getDouble(PrismSettings.SIMULATOR_DEFAULT_WIDTH);

		if (!reqIterToConcludeGiven)
			reqIterToConclude = prism.getSettings().getInteger(PrismSettings.SIMULATOR_DECIDE);
		if (!simMaxRewardGiven)
			simMaxReward = prism.getSettings().getDouble(PrismSettings.SIMULATOR_MAX_REWARD);
		if (!simMaxPathGiven)
			simMaxPath = prism.getSettings().getLong(PrismSettings.SIMULATOR_DEFAULT_MAX_PATH);

		// Pick a default method, if not specified
		// (CI for quantitative, SPRT for bounded)
		if (simMethodName == null) {
			simMethodName = isQuant ? "ci" : "sprt";
		}

		// CI
		if (simMethodName.equals("ci")) {
			if (simWidthGiven && simConfidenceGiven && simNumSamplesGiven) {
				throw new PrismException("Cannot specify all three parameters (width/confidence/samples) for CI method");
			}
			if (!simWidthGiven) {
				// Default (unless width specified) is to leave width unknown
				aSimMethod = new CIwidth(simConfidence, simNumSamples);
			} else if (!simNumSamplesGiven) {
				// Next preferred option (unless specified) is unknown samples
				if (simManual)
					aSimMethod = new CIiterations(simConfidence, simWidth, reqIterToConclude);
				else
					aSimMethod = (isReward ? new CIiterations(simConfidence, simWidth, simMaxReward) : new CIiterations(simConfidence, simWidth));
			} else {
				// Otherwise confidence unknown
				aSimMethod = new CIconfidence(simWidth, simNumSamples);
			}
			if (simApproxGiven) {
				mainLog.printWarning("Option -simapprox is not used for the CI method and is being ignored");
			}
		}
		// ACI
		else if (simMethodName.equals("aci")) {
			if (simWidthGiven && simConfidenceGiven && simNumSamplesGiven) {
				throw new PrismException("Cannot specify all three parameters (width/confidence/samples) for ACI method");
			}
			if (!simWidthGiven) {
				// Default (unless width specified) is to leave width unknown
				aSimMethod = new ACIwidth(simConfidence, simNumSamples);
			} else if (!simNumSamplesGiven) {
				// Next preferred option (unless specified) is unknown samples
				if (simManual)
					aSimMethod = new ACIiterations(simConfidence, simWidth, reqIterToConclude);
				else
					aSimMethod = (isReward ? new ACIiterations(simConfidence, simWidth, simMaxReward) : new CIiterations(simConfidence, simWidth));
			} else {
				// Otherwise confidence unknown
				aSimMethod = new ACIconfidence(simWidth, simNumSamples);
			}
			if (simApproxGiven) {
				mainLog.printWarning("Option -simapprox is not used for the ACI method and is being ignored");
			}
		}
		// APMC
		else if (simMethodName.equals("apmc")) {
			if (isReward) {
				throw new PrismException("Cannot use the APMC method on reward properties; try CI (switch -simci) instead");
			}
			if (simApproxGiven && simConfidenceGiven && simNumSamplesGiven) {
				throw new PrismException("Cannot specify all three parameters (approximation/confidence/samples) for APMC method");
			}
			if (!simApproxGiven) {
				// Default (unless width specified) is to leave approximation unknown
				aSimMethod = new APMCapproximation(simConfidence, simNumSamples);
			} else if (!simNumSamplesGiven) {
				// Next preferred option (unless specified) is unknown samples
				aSimMethod = new APMCiterations(simConfidence, simApprox);
			} else {
				// Otherwise confidence unknown
				aSimMethod = new APMCconfidence(simApprox, simNumSamples);
			}
			if (simWidthGiven) {
				mainLog.printWarning("Option -simwidth is not used for the APMC method and is being ignored");
			}
		}
		// SPRT
		else if (simMethodName.equals("sprt")) {
			if (isQuant) {
				throw new PrismException("Cannot use SPRT on a quantitative (=?) property");
			}
			aSimMethod = new SPRTMethod(simConfidence, simConfidence, simWidth);
			if (simApproxGiven) {
				mainLog.printWarning("Option -simapprox is not used for the SPRT method and is being ignored");
			}
			if (simNumSamplesGiven) {
				mainLog.printWarning("Option -simsamples is not used for the SPRT method and is being ignored");
			}
		} else
			throw new PrismException("Unknown simulation method \"" + simMethodName + "\"");

		return aSimMethod;
	}

	/**
	 * Print a -help message, i.e. a list of the command-line switches.
	 */
	private void printHelp()
	{
		mainLog.println("Usage: prism <model-file> [<properties-file>] [options]");
		mainLog.println();
		mainLog.println("Options:");
		mainLog.println("========");
		mainLog.println();
		mainLog.println("-help .......................... Display this help message");
		mainLog.println("-version ....................... Display PRISM version info");
		mainLog.println("-settings <file>................ Load settings from <file>");
		mainLog.println();
		mainLog.println("-pf <props> (or -pctl or -csl) . Model check properties <props>");
		mainLog.println("-property <n> (or -prop <n>) ... Only model check property with index/name <n>");
		mainLog.println("-const <vals> .................. Define constant values as <vals> (e.g. for experiments)");
		mainLog.println("-steadystate (or -ss) .......... Compute steady-state probabilities (D/CTMCs only)");
		mainLog.println("-transient <x> (or -tr <x>) .... Compute transient probabilities for time (or time range) <x> (D/CTMCs only)");
		mainLog.println("-simpath <options> <file>....... Generate a random path with the simulator");
		mainLog.println("-nobuild ....................... Skip model construction (just do parse/export)");
		mainLog.println("-test .......................... Enable \"test\" mode");
		mainLog.println("-testall ....................... Enable \"test\" mode, but don't exit on error");
		mainLog.println();
		mainLog.println("IMPORT OPTIONS:");
		mainLog.println("-importpepa .................... Model description is in PEPA, not the PRISM language");
		mainLog.println("-importmodel <files> ........... Import the model directly from text file(s)");
		mainLog.println("-importtrans <file> ............ Import the transition matrix directly from a text file");
		mainLog.println("-importstates <file>............ Import the list of states directly from a text file");
		mainLog.println("-importlabels <file>............ Import the list of labels directly from a text file");
		mainLog.println("-importinitdist <file>.......... Specify the initial probability distribution for transient analysis");
		mainLog.println("-dtmc .......................... Force imported/built model to be a DTMC");
		mainLog.println("-ctmc .......................... Force imported/built model to be a CTMC");
		mainLog.println("-mdp ........................... Force imported/built model to be an MDP");
		mainLog.println();
		mainLog.println("EXPORT OPTIONS:");
		mainLog.println("-exportresults <file[:options]>  Export the results of model checking to a file");
		mainLog.println("-exportmodel <files[:options]> . Export the built model to file(s)");
		mainLog.println("-exporttrans <file> ............ Export the transition matrix to a file");
		mainLog.println("-exportstaterewards <file> ..... Export the state rewards vector to a file");
		mainLog.println("-exporttransrewards <file> ..... Export the transition rewards matrix to a file");
		mainLog.println("-exportrewards <file1> <file2>.. Export state/transition rewards to files 1/2");
		mainLog.println("-exportstates <file> ........... Export the list of reachable states to a file");
		mainLog.println("-exportlabels <file> ........... Export the list of labels and satisfying states to a file");
		mainLog.println("-exportmatlab .................. When exporting matrices/vectors/labels/etc., use Matlab format");
		mainLog.println("-exportmrmc .................... When exporting matrices/vectors/labels, use MRMC format");
		mainLog.println("-exportrows .................... When exporting matrices, put a whole row on one line");
		mainLog.println("-exportordered ................. When exporting matrices, order entries (by row) [default]");
		mainLog.println("-exportunordered ............... When exporting matrices, don't order entries");
		mainLog.println("-exporttransdot <file> ......... Export the transition matrix graph to a dot file");
		mainLog.println("-exporttransdotstates <file> ... Export the transition matrix graph to a dot file, with state info");
		mainLog.println("-exportdot <file> .............. Export the transition matrix MTBDD to a dot file");
		mainLog.println("-exportsccs <file> ............. Compute and export all SCCs of the model");
		mainLog.println("-exportbsccs <file> ............ Compute and export all BSCCs of the model");
		mainLog.println("-exportmecs <file> ............. Compute and export all maximal end components (MDPs only)");
		mainLog.println("-exportsteadystate <file> ...... Export steady-state probabilities to a file");
		mainLog.println("-exporttransient <file> ........ Export transient probabilities to a file");
		mainLog.println("-exportprism <file> ............ Export final PRISM model to a file");
		mainLog.println("-exportprismconst <file> ....... Export final PRISM model with expanded constants to a file");

		PrismSettings.printHelp(mainLog);

		mainLog.println();
		mainLog.println("SIMULATION OPTIONS:");
		mainLog.println("-sim ........................... Use the PRISM simulator to approximate results of model checking");
		mainLog.println("-simmethod <name> .............. Specify the method for approximate model checking (ci, aci, apmc, sprt)");
		mainLog.println("-simsamples <n> ................ Set the number of samples for the simulator (CI/ACI/APMC methods)");
		mainLog.println("-simconf <x> ................... Set the confidence parameter for the simulator (CI/ACI/APMC methods)");
		mainLog.println("-simwidth <x> .................. Set the interval width for the simulator (CI/ACI methods)");
		mainLog.println("-simapprox <x> ................. Set the approximation parameter for the simulator (APMC method)");
		mainLog.println("-simmanual ..................... Do not use the automated way of deciding whether the variance is null or not");
		mainLog.println("-simvar <n> .................... Set the minimum number of samples to know the variance is null or not");
		mainLog.println("-simmaxrwd <x> ................. Set the maximum reward -- useful to display the CI/ACI methods progress");
		mainLog.println("-simpathlen <n> ................ Set the maximum path length for the simulator");

		mainLog.println();
		mainLog.println("You can also use \"prism -help xxx\" for help on some switches -xxx with non-obvious syntax.");
	}

	/**
	 * Print a -help xxx message, i.e. display help on a specific switch
	 */
	private void printHelpSwitch(String sw)
	{
		// Remove "-" from start of switch, in case present (it shouldn't be really)
		if (sw.charAt(0) == '-')
			sw = sw.substring(1);

		// -const
		if (sw.equals("const")) {
			mainLog.println("Switch: -const <vals>\n");
			mainLog.println("<vals> is a comma-separated list of values or value ranges for undefined constants");
			mainLog.println("in the model or properties (i.e. those declared without values, such as \"const int a;\").");
			mainLog.println("You can either specify a single value (a=1), a range (a=1:10) or a range with a step (a=1:2:50).");
			mainLog.println("For convenience, constant definutions can also be split across multiple -const switches.");
			mainLog.println("\nExamples:");
			mainLog.println(" -const a=1,b=5.6,c=true");
			mainLog.println(" -const a=1:10,b=5.6");
			mainLog.println(" -const a=1:2:50,b=5.6");
			mainLog.println(" -const a=1:2:50 -const b=5.6");
		}
		// -simpath
		else if (sw.equals("simpath")) {
			mainLog.println("Switch: -simpath <options> <file>\n");
			mainLog.println("Generate a random path with the simulator and export it to <file> (or to the screen if <file>=\"stdout\").");
			mainLog.println("<options> is a comma-separated list of options taken from:");
			GenerateSimulationPath.printOptions(mainLog);
		}
		// -importmodel
		else if (sw.equals("importmodel")) {
			mainLog.println("Switch: -importmodel <files>\n");
			mainLog.println("Import the model directly from text file(s).");
			mainLog.println("Use a list of file extensions to indicate which files should be read, e.g.:");
			mainLog.println("\n -importmodel in.tra,in.sta\n");
			mainLog.println("Possible extensions are: .tra, .sta, .lab");
			mainLog.println("Use extension .all to import all, e.g.:");
			mainLog.println("\n -importmodel in.all\n");
		}
		// -exportresults
		else if (sw.equals("exportresults")) {
			mainLog.println("Switch: -exportresults <file[:options]>\n");
			mainLog.println("Exports the results of model checking to <file> (or to the screen if <file>=\"stdout\").");
			mainLog.println("The default behaviour is to export a list of results in text form, using tabs to separate items.");
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * csv - Export results as comma-separated values");
			mainLog.println(" * matrix - Export results as one or more 2D matrices (e.g. for surface plots)");
		}
		// -exportmodel
		else if (sw.equals("exportmodel")) {
			mainLog.println("Switch: -exportmodel <files[:options]>\n");
			mainLog.println("Export the built model to file(s) (or to the screen if <file>=\"stdout\").");
			mainLog.println("Use a list of file extensions to indicate which files should be generated, e.g.:");
			mainLog.println("\n -exportmodel out.tra,out.sta\n");
			mainLog.println("Possible extensions are: .tra, .srew, .trew, .sta, .lab");
			mainLog.println("Use extension .all to export all and .rew to export both .srew/.trew, e.g.:");
			mainLog.println("\n -exportmodel out.all\n");
			mainLog.println("Omit the file basename to use the basename of the model file, e.g.:");
			mainLog.println("\n -exportmodel .all\n");
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * mrmc - export data in MRMC format");
			mainLog.println(" * matlab - export data in Matlab format");
			mainLog.println(" * rows - export matrices with one row/distribution on each line");
			mainLog.println(" * ordered - output states indices in ascending order [default]");
			mainLog.println(" * unordered - don't output states indices in ascending order");
		}
		// Try PrismSettings
		else if (PrismSettings.printHelpSwitch(mainLog, sw)) {
			return;
		}
		// Unknown
		else {
			mainLog.println("Sorry - no help available for switch -" + sw);
		}
	}

	// print version

	private void printVersion()
	{
		mainLog.println("PRISM version " + Prism.getVersion());
	}

	/**
	 * Print out a list of all PRISM language keywords.
	 */
	private void printListOfKeywords()
	{
		List<String> list = Prism.getListOfKeyords();
		mainLog.print("PRISM keywords:");
		for (String s : list) {
			mainLog.print(" " + s);
		}
		mainLog.println();
	}

	/**
	 * Report a (non-fatal) error to the log.
	 */
	private void error(String s)
	{
		error(s, false);
	}

	/**
	 * Report a (non-fatal) error to the log.
	 * Optionally, requested that we do not exit, even if test mode is enabled
	 */
	private void error(String s, boolean dontExit)
	{
		// If (and only if) we are in "test" (and not "testall") mode, treat any error as fatal
		if (test && testExitsOnFail && !dontExit) {
			errorAndExit(s);
		}
		// Normal case: just display error message, but don't exit
		mainLog.println("\nError: " + s + ".");
	}

	/**
	 * Report a (fatal) error and exit cleanly (with exit code 1).
	 */
	private void errorAndExit(String s)
	{
		prism.closeDown(false);
		mainLog.println("\nError: " + s + ".");
		System.exit(1);
	}

	/**
	 * Exit cleanly (with exit code 0).
	 */
	private void exit()
	{
		prism.closeDown(true);
		System.exit(0);
	}

	/**
	 * Exit cleanly (with exit code i).
	 */
	private void exit(int i)
	{
		prism.closeDown(true);
		System.exit(i);
	}

	// main method

	public static void main(String[] args)
	{
		new PrismCL().run(args);
	}
}

//------------------------------------------------------------------------------
