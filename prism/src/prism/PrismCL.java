//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import common.StackTraceHelper;
import csv.CsvFormatException;
import io.ModelExportOptions;
import io.ModelExportFormat;
import io.ModelExportTask;
import io.PrismExplicitImporter;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import strat.StrategyExportOptions;
import prism.ResultsExporter.ResultsExportShape;
import prism.ResultsImporter.RawResultsCollection;
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

import static prism.PrismSettings.RANGE_EXPORT_DOUBLE_PRECISION;

// prism - command line version

public class PrismCL implements PrismModelListener
{
	// flags
	private boolean importpepa = false;
	private boolean importprismpp = false;
	private boolean importinitdist = false;
	private boolean importresults = false;
	private boolean steadystate = false;
	private boolean dotransient = false;
	private boolean exportprism = false;
	private boolean exportprismconst = false;
	private boolean exportmodeldotview = false;
	private boolean exportsccs = false;
	private boolean exportbsccs = false;
	private boolean exportmecs = false;
	private boolean exportresults = false;
	private ResultsExportShape exportShape = ResultsExportShape.LIST_PLAIN;
	private boolean exportvector = false;
	private int exportType = Prism.EXPORT_PLAIN;
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

	// export info
	private List<ModelExportTask> modelExportTasks = new ArrayList<>();
	private ModelExportOptions modelExportOptionsGlobal = new ModelExportOptions();

	// import info
	private List<ModelImportSource> modelImportSources = new ArrayList<>();

	// property info
	private List<Object> propertyIndices = null;
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
	private String settingsFilename = null;
	private String modelFilename = null;
	private String importInitDistFilename = null;
	private String importResultsFilename = null;
	private String importModelWarning = null;
	private String propertiesFilename = null;
	private String exportPrismFilename = null;
	private String exportPrismConstFilename = null;
	private String exportSCCsFilename = null;
	private String exportBSCCsFilename = null;
	private String exportMECsFilename = null;
	private String exportResultsFilename = null;
	private String exportVectorFilename = null;
	private String exportSteadyStateFilename = null;
	private String exportTransientFilename = null;
	private String exportStratFilename = null;
	private String simpathFilename = null;

	// CLI switch handler map (populated by initSwitchHandlers)
	private Map<String, SwitchHandler> switchHandlers;

	// logs
	private PrismLog mainLog = null;

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
	private StrategyExportOptions exportStratOptions = null;
	
	// parametric analysis info
	private String[] paramLowerBounds = null;
	private String[] paramUpperBounds = null;
	private String[] paramNames = null;

	private boolean exactConstants = false;

	/** Specification of a single file for model import. */
	private class ModelImportSource
	{
		private ModelExportTask.ModelExportEntity entity;
		private ModelExportFormat format;
		private File file;

		public ModelImportSource(ModelExportTask.ModelExportEntity entity, ModelExportFormat format, File file)
		{
			this.entity = entity;
			this.format = format;
			this.file = file;
		}
	}

	/**
	 * Entry point: call run method, catch CuddOutOfMemoryException
	 */
	public void go(String[] args) {
		try {
			run(args);
		} catch (jdd.JDD.CuddOutOfMemoryException e) {
			mainLog.println("\nCUDD internal error detected, from the following stack trace:");
			for (StackTraceElement st : e.getStackTrace()) {
				mainLog.print("  ");
				mainLog.println(st);
			}
			errorAndExit(e.getMessage() + ".\nTip: Try using the -cuddmaxmem switch to increase the memory available to CUDD");
		} catch (com.martiansoftware.nailgun.NGExitException e) {
			// we don't want to catch the nailgun exception below,
			// so we catch it and rethrow
			throw e;
		} catch (Exception|StackOverflowError e) {
			// We catch Exceptions/stack overflows here ourself to ensure that we actually exit
			// In the presence of thread pools (e.g., in the JAS library when using -exact),
			// the main thread dying does not necessarily quit the program...
			mainLog.println();
			if (e instanceof StackOverflowError) {
				// print exception + limited stack trace for stack overflows
				mainLog.println(e.toString());
				mainLog.println(StackTraceHelper.asString(e, StackTraceHelper.DEFAULT_STACK_TRACE_LIMIT));
				mainLog.println("Try increasing the value of the Java stack size (via the -javastack argument).");
			} else {
				// print exception + full stack trace for generic exceptions
				mainLog.print(e.toString() + "\n" + StackTraceHelper.asString(e, 0));
			}
			errorAndExit("Caught unhandled exception, aborting...");
		}
	}

	/**
	 * Run PRISM.
	 */
	public void run(String[] args)
	{
		int i, j, k;
		Result res;

		// Initialise
		initialise(args);

		// Import (and optionally re-export) results
		if (importresults) {
			importResults();
			if (exportresults) {
				exportResults();
			}
			closeDown();
			return;
		}

		// Parse/load model/properties
		doParsing();

		// Sort out properties to check
		sortProperties();

		// evaluate constants exactly if we are in param or exact computation mode
		exactConstants = param || prism.getSettings().getBoolean(PrismSettings.PRISM_EXACT_ENABLED);

		// process info about undefined constants
		try {
			// first, see which constants are undefined
			// (one set of info for model, and one set of info for each property)
			if (modelExportTasks.stream().anyMatch(ModelExportTask::extraLabelsUsed)) {
				undefinedMFConstants = new UndefinedConstants(modulesFile, propertiesFile, true);
			} else {
				undefinedMFConstants = new UndefinedConstants(modulesFile, null);
			}
			undefinedMFConstants.setExactMode(exactConstants);
			undefinedConstants = new UndefinedConstants[numPropertiesToCheck];
			for (i = 0; i < numPropertiesToCheck; i++) {
				undefinedConstants[i] = new UndefinedConstants(modulesFile, propertiesFile, propertiesToCheck.get(i));
				undefinedConstants[i].setExactMode(exactConstants);
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
		if (param) {
			prism.setParametric(paramNames, paramLowerBounds, paramUpperBounds);
		}

		// If -exportadv was used and the explicit engine has been requested for MDPs,
		// or the model type is only supported by the explicit engine, stop with an error message
		if (prism.getSettings().getChoice(PrismSettings.PRISM_EXPORT_ADV) != Prism.EXPORT_ADV_NONE) {
			if (prism.getModelType() == ModelType.MDP && prism.getCurrentEngine() == Prism.PrismEngine.EXPLICIT) {
				errorAndExit("The -exportadv functionality does not work for the explicit engine; use -exportstrat instead");
			}
			if (prism.getModelType().partiallyObservable() || prism.getModelType().uncertain()) {
				errorAndExit("The -exportadv functionality does not work for the explicit engine; use -exportstrat instead");
			}
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
				prism.setPRISMModelConstants(definedMFConstants, exactConstants);
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
					prism.generateSimulationPath(simpathDetails, simMaxPath, f);
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
			try {
				doExports();
			} catch (PrismException e) {
				error(e);
			}
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
								propertiesFile.setSomeUndefinedConstants(definedPFConstants, exactConstants);
							}
							// Normal model checking
							if (!simulate) {
								res = prism.modelCheck(propertiesFile, propertiesToCheck.get(j));
							}
							// Approximate (simulation-based) model checking
							else {
								simMethod = processSimulationOptions(propertiesToCheck.get(j).getExpression());
								res = prism.modelCheckSimulator(propertiesFile, propertiesToCheck.get(j).getExpression(), definedPFConstants, null, simMaxPath,
										simMethod);
								simMethod.reset();
							}
						} catch (PrismException e) {
							// in case of error, report it, store exception as the result and proceed
							error(e.getMessage(), true);
							res = new Result(e);
						}

						// in case of build failure during model checking, store as result for any const values and continue
						if (modelBuildFail) {
							results[j].setMultipleErrors(definedMFConstants, null, modelBuildException);
							if (test) {
								doResultTest(propertiesToCheck.get(j), new Result(modelBuildException), prism.getModelInfo().getConstantValues(), propertiesFile.getConstantValues());
							}
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
								prism.exportStrategy(res.getStrategy(), exportStratOptions, exportStratFilename.equals("stdout") ? null : new File(exportStratFilename));
							}
							// in case of error, report it and proceed
							catch (FileNotFoundException e) {
								error("Couldn't open file \"" + exportStratFilename + "\" for output");
							} catch (PrismException e) {
								error(e.getMessage());
							}
						}

						// if a results vector was stored, and we need to export it, do so
						if (exportvector && res.getVector() != null) {
							mainLog.print("\nExporting vector of results for all states ");
							mainLog.println(exportVectorFilename.equals("stdout") ? "below:" : "to file \"" + exportVectorFilename + "\"...");
							boolean toStdout = exportVectorFilename.equals("stdout");
							try {
								PrismLog tmpLog = toStdout ? prism.getMainLog() : new PrismFileLog(exportVectorFilename);
								res.getVector().print(tmpLog, false, false, toStdout, toStdout);
								res.getVector().clear();
								if (!toStdout) {
									tmpLog.close();
								}
							} catch (PrismException e) {
								error(e.getMessage());
							}
						}
						
						// if required, check result against expected value
						if (test) {
							doResultTest(propertiesToCheck.get(j), res, prism.getModelInfo().getConstantValues(), propertiesFile.getConstantValues());
						}

						// iterate to next property
						undefinedConstants[j].iterateProperty();
					}
				}

				// in case of build failure during model checking, store as result for any further properties and continue
				if (modelBuildFail) {
					for (j++; j < numPropertiesToCheck; j++) {
						results[j].setMultipleErrors(definedMFConstants, null, modelBuildException);
						if (test) {
							doResultTest(propertiesToCheck.get(j), new Result(modelBuildException), prism.getModelInfo().getConstantValues(), propertiesFile.getConstantValues());
						}
					}
					break;
				}
			}

			// Explicitly request a build if necessary
			if (propertiesToCheck.size() == 0 && !steadystate && !dotransient && !simpath && !nobuild && prism.modelCanBeBuilt() && !prism.someModelIsBuilt()) {
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
			exportResults();
		}
		// close down
		closeDown();

	}

	/**
	 * Import results from a data frame in a CSV file.
	 */
	protected void importResults()
	{
		mainLog.print("\nImporting results from dataframe in " + importResultsFilename + "\"...");
		try {
			propertiesToCheck = new ArrayList<Property>();
			List<ResultsCollection> importedResults = new ArrayList<ResultsCollection>();
			FileReader reader = new FileReader(new File(importResultsFilename));
			ResultsImporter importer = new ResultsImporter(new BufferedReader(reader));
			for (Entry<Property, RawResultsCollection> result : importer) {
				propertiesToCheck.add(result.getKey());
				importedResults.add(result.getValue().toResultsCollection());
			}
			results = importedResults.toArray(new ResultsCollection[0]);
		} catch (FileNotFoundException e) {
			errorAndExit("Could not import results: " + e.getMessage());
		} catch (IOException e) {
			errorAndExit("Could not read results file: " + e.getMessage());
		} catch (CsvFormatException e) {
			errorAndExit("Malformatted CSV results file: " + e.getMessage());
		} catch (PrismLangException e) {
			errorAndExit("Syntax error in results file: " + e.getMessage());
		}
	}

	/**
	 * Export results to a file according to the export options.
	 */
	protected void exportResults()
	{
		mainLog.print("\nExporting results as " + exportShape.fullName);
		mainLog.println(exportResultsFilename.equals("stdout") ? " below:\n" : " to file \"" + exportResultsFilename + "\"...");

		try {
			PrintWriter out;
			if (exportResultsFilename.equals("stdout")) {
				out = new PrintWriter(System.out);
				exportShape.getExporter().printResults(Arrays.asList(results), propertiesToCheck, out);
				// Do not close System.out !
			} else {
				out = new PrintWriter(exportResultsFilename);
				exportShape.getExporter().printResults(Arrays.asList(results), propertiesToCheck, out);
				out.close();
			}
			if (out.checkError()) {
				// PrintWriter hides exceptions in print methods and close()
				errorAndExit("Could not export results: unknown IO exception");
			}
		} catch (FileNotFoundException e) {
			errorAndExit("Could not export results: " + e.getMessage());
		}
	}

	/**
	 * Initialise.
	 */
	private void initialise(String[] args)
	{
		try {
			// default to log going to stdout
			// this means all errors etc. can be safely sent to the log
			// even if a new log is created shortly
			mainLog = new PrismFileLog("stdout");

			// create prism object(s)
			prism = new Prism(mainLog);
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
		List<File> srf = new ArrayList<>();
		List<File> trf = new ArrayList<>();

		// parse model

		try {
			if (importModelWarning != null) {
				mainLog.printWarning(importModelWarning);
			}
			if (importpepa) {
				modulesFile = prism.importPepaFile(new File(modelFilename));
				prism.loadPRISMModel(modulesFile);
			} else if (importprismpp) {
				String prismppParamsList[] = ("? " + prismppParams).split(" ");
				modulesFile = prism.importPrismPreprocFile(new File(modelFilename), prismppParamsList);
				prism.loadPRISMModel(modulesFile);
			} else if (!modelImportSources.isEmpty()) {
				sortModelImports();
			} else {
				modulesFile = prism.parseModelFile(new File(modelFilename), typeOverride);
				prism.loadPRISMModel(modulesFile);
			}
		} catch (FileNotFoundException e) {
			errorAndExit("File \"" + modelFilename + "\" not found");
		} catch (PrismException e) {
			errorAndExit(e.getMessage());
		}

		// export prism model, if requested
		if (exportprism) {
			try {
				File f = (exportPrismFilename.equals("stdout")) ? null : new File(exportPrismFilename);
				prism.exportPRISMModel(f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportPrismFilename + "\" for output");
			} catch (PrismException e) {
				error(e);
			}
		}

		// parse properties

		try {
			// if properties file specified...
			if (propertiesFilename != null) {
				mainLog.print("\nParsing properties file \"" + propertiesFilename + "\"...\n");
				propertiesFile = prism.parsePropertiesFile(new File(propertiesFilename));
			}
			// if properties were given on command line...
			else if (!propertyString.equals("")) {
				propertiesFile = prism.parsePropertiesString(propertyString);
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
	}

	/**
	 * If importing a model, process the specification and import.
	 */
	private void sortModelImports() throws PrismException
	{
		// Exactly one model/transitions source should be provided
		int numModelSources = (int) modelImportSources.stream().filter(s -> s.entity == ModelExportTask.ModelExportEntity.MODEL).count();
		if (numModelSources < 1) {
			throw new PrismException("No transitions specified for model import");
		}
		if (numModelSources > 1) {
			throw new PrismException("Multiple model imports provided");
		}
		// Create an importer and add the requested files
		ModelImportSource modelSource = modelImportSources.stream().filter(s -> s.entity == ModelExportTask.ModelExportEntity.MODEL).findFirst().get();
		switch (modelSource.format) {
			// Import from PRISM explicit files
			case EXPLICIT:
				if (!modelImportSources.stream().allMatch(s -> s.format == ModelExportFormat.EXPLICIT)) {
					throw new PrismException("Mixed formats for model import");
				}
				PrismExplicitImporter importer = new PrismExplicitImporter(modelSource.file, typeOverride);
				for (ModelImportSource modelImportSource : modelImportSources) {
					switch (modelImportSource.entity) {
						case MODEL:
							// Skip; already handled
							break;
						case STATES:
							if (importer.getStatesFile() != null) {
								throw new PrismException("Multiple state files provided for model import");
							}
							importer.setStatesFile(modelImportSource.file);
							break;
						case OBSERVATIONS:
							if (importer.getObservationsFile() != null) {
								throw new PrismException("Multiple observation files provided for model import");
							}
							importer.setObservationsFile(modelImportSource.file);
							break;
						case LABELS:
							if (importer.getLabelsFile() != null) {
								throw new PrismException("Multiple label files provided for model import");
							}
							importer.setLabelsFile(modelImportSource.file);
							break;
						case STATE_REWARDS:
							importer.addStateRewardsFile(modelImportSource.file);
							break;
						case TRANSITION_REWARDS:
							importer.addTransitionRewardsFile(modelImportSource.file);
							break;
						default:
							throw new PrismException("Unknown model import entity");
					}
				}
				prism.loadModelFromExplicitFiles(importer);
				break;

			// Import from a UMB file
			case UMB:
				if (modelImportSources.size() > 1) {
					throw new PrismException("Conflicting files provided for model import");
				}
				prism.loadModelFromUMBFile(modelSource.file);
				break;

			default:
				throw new PrismException("Unknown model import format " + modelSource.format);
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
			if (propertyIndices != null && !propertyIndices.isEmpty()) {
				errorAndExit("There is not a property \"" + propertyIndices.get(0) + "\" to check");
			}
			numPropertiesToCheck = 0;
		}
		// unless specified, verify all properties
		else if (propertyIndices == null) {
			numPropertiesToCheck = propertiesFile.getNumProperties();
			for (i = 0; i < numPropertiesToCheck; i++) {
				propertiesToCheck.add(propertiesFile.getPropertyObject(i));
			}
		}
		// otherwise just verify the specified properties
		else {
			for (Object o : propertyIndices) {
				if (o instanceof Integer) {
					int propIndex = (Integer) o;
					if (propIndex <= 0 || propIndex > propertiesFile.getNumProperties())
						errorAndExit("There is not a property " + propIndex + " to verify");
					numPropertiesToCheck += 1;
					propertiesToCheck.add(propertiesFile.getPropertyObject(propIndex - 1));
				} else if (o instanceof String) {
					Property p = propertiesFile.getPropertyObjectByName((String) o);
					if (p == null)
						errorAndExit("There is not a property \"" + o + "\" to check");
					numPropertiesToCheck += 1;
					propertiesToCheck.add(p);
				} else {
					errorAndExit("There is not a property " + o + " to check");
				}
			}
		}
	}

	// do any exporting requested

	private void doExports() throws PrismException
	{
		// export prism model (with constants), if requested
		if (exportprismconst) {
			try {
				File f = (exportPrismConstFilename.equals("stdout")) ? null : new File(exportPrismConstFilename);
				prism.exportPRISMModelWithExpandedConstants(f);
			}
			// in case of error, report it and proceed
			catch (FileNotFoundException e) {
				error("Couldn't open file \"" + exportPrismConstFilename + "\" for output");
			} catch (PrismException e) {
				error(e);
			}
		}

		// Exceptions from the remaining exports are thrown
		// since they usually indicate a model build problem, affecting all

		// Do export tasks
		for (ModelExportTask exportTask : modelExportTasks) {
			exportTask.getExportOptions().apply(modelExportOptionsGlobal);
			if (exportTask.extraLabelsUsed()) {
				definedPFConstants = undefinedMFConstants.getPFConstantValues();
				propertiesFile.setSomeUndefinedConstants(definedPFConstants, exactConstants);
				exportTask.setExtraLabelsSource(propertiesFile);
			}
		}
		if (!modelExportTasks.isEmpty()) {
			prism.exportBuiltModelTasks(modelExportTasks);
		}

		// export transition matrix graph to dot file and view it
		if (exportmodeldotview) {
			try {
				File dotFile = File.createTempFile("prism-dot-", ".dot", null);
				File dotPdfFile = File.createTempFile("prism-dot-", ".dot.pdf", null);
				prism.exportBuiltModelTransitions(dotFile, new ModelExportOptions().setFormat(ModelExportFormat.DOT).setShowStates(true).setShowObservations(true));
				(new ProcessBuilder(new String[]{ "dot", "-Tpdf", "-o", dotPdfFile.getPath(), dotFile.getPath()})).start().waitFor();
				(new ProcessBuilder(new String[]{ "open",dotPdfFile.getPath()})).start();
			}
			// in case of error, report it and proceed
			catch (IOException | InterruptedException e) {
				error("Problem generating dot file: " + e.getMessage());
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
				prism.exportSteadyStateProbabilities(exportSteadyStateFile, Prism.convertExportType(exportType), importinitdist ? new File(importInitDistFilename) : null);
			} catch (PrismException e) {
				// In case of error, report it and proceed
				error(e);
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
				// Compute transient probabilities
				prism.exportTransientProbabilities(transientTime, exportTransientFile, Prism.convertExportType(exportType), importinitdist ? new File(importInitDistFilename) : null);
			}
			// In case of error, report it and proceed
			catch (PrismException e) {
				error(e);
			}
		}
	}

	/**
	 * Test a model checking result against the RESULT specifications attached
	 * to the property (test mode). Test results are output to the log.
	 * If a test fails and {@code testExitsOnFail} is {@code true} then {@code errorAndExit} is called.
	 * Model/properties file constant values should be provided in case they are used
	 * in the RESULT specification (but either can be left null if not needed).
	 * @param prop the property
	 * @param res the result
	 * @param mfConstants values for model constants
	 * @param pfConstants values for properties file constants
	 */
	private void doResultTest(Property prop, Result res, Values mfConstants, Values pfConstants)
	{
		try {
			Values allConsts = new Values(mfConstants, pfConstants);
			List<String> allParams = param ? Arrays.asList(paramNames) : Collections.emptyList(); 
			if (prop.checkAgainstExpectedResult(res, allConsts, allParams)) {
				mainLog.println("Testing result: PASS");
			} else {
				mainLog.println("Testing result: NOT TESTED");
			}
		} catch (PrismNotSupportedException e) {
			mainLog.println("Testing result: UNSUPPORTED: " + e.getMessage());
		} catch (PrismException e) {
			mainLog.println("Testing result: FAIL: " + e.getMessage());
			if (testExitsOnFail)
				errorAndExit("Testing failed");
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
	}

	/** Set a timeout, exit program if timeout is reached */
	private void setTimeout(final int timeout)
	{
		common.Timeout.setTimeout(timeout, new Runnable() {
			@Override
			public void run()
			{
				mainLog.println("\nError: Timeout (after " + timeout + " seconds).");
				mainLog.flush();
				System.exit(1);
			}
		});
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
		constSwitch = "";
		paramSwitch = "";
		List<String> filenameArgs = new ArrayList<>();
		initSwitchHandlers();

		ArgConsumer consumer = new ArgConsumer(args);
		while (consumer.hasNext()) {
			String arg = consumer.advance();
			String sw = consumer.parseSwitch(arg);
			if (sw != null) {
				SwitchHandler handler = switchHandlers.get(sw);
				if (handler != null)
					handler.handle(sw, consumer);
				else
					prism.getSettings().setFromCommandLineSwitch(sw, consumer);
			} else {
				// argument is assumed to be a (model/properties) filename
				filenameArgs.add(arg);
			}
		}

		processFileNames(filenameArgs);
	}

	/**
	 * Populate {@link #switchHandlers} with a handler for every switch recognised by PrismCL.
	 * Switches not registered here fall through to {@link PrismSettings#setFromCommandLineSwitch}.
	 * The insertion order matches the -help output ordering.
	 */
	private void initSwitchHandlers()
	{
		switchHandlers = new LinkedHashMap<>();

		// Help / version / meta
		addSwitch("help", "?", (sw, a) -> {
			// -help [switch] is one of the few cases where the value arg is optional
			if (a.hasNext()) printHelpSwitch(a.next(sw)); else printHelp();
			exit();
		});
		addSwitch("javamaxmem",
				(sw, a) -> a.next(sw)); // consumed before JVM launch; value discarded
		addSwitch("javastack",
				(sw, a) -> a.next(sw)); // consumed before JVM launch; value discarded
		addSwitch("javaparams",
				(sw, a) -> a.next(sw)); // consumed before JVM launch; value discarded
		addSwitch("timeout", (sw, a) -> {
			int t = PrismUtils.convertTimeStringtoSeconds(a.next(sw));
			if (t < 0) errorAndExit("Negative timeout value \"" + t + "\" for -" + sw + " switch");
			if (t > 0) setTimeout(t);
			// t == 0 -> no timeout
		});
		addSwitch("version", new FlagSwitch(() -> { printVersion(); exit(); }));
		addSwitch("dir", new StringSwitch(Prism::setWorkingDirectory));
		addSwitch("settings", new StringSwitch(s -> settingsFilename = s.trim()));
		addSwitch("keywords", new FlagSwitch(() -> { printListOfKeywords(); exit(); })); // hidden

		// Property / constant / parameter info
		addSwitch("pf", "pctl", "csl", new StringSwitch(s -> propertyString = s));
		addSwitch("prop", "property", (sw, a) -> {
			String[] props = a.next(sw).trim().split(",");
			propertyIndices = new ArrayList<>();
			for (String p : props) {
				if (!p.isEmpty()) {
					try { propertyIndices.add(Integer.parseInt(p)); }
					catch (NumberFormatException e) { propertyIndices.add(p); }
				}
			}
		});
		addSwitch("const", (sw, a) -> {
			String v = a.next(sw).trim();
			if ("".equals(constSwitch)) constSwitch = v; else constSwitch += "," + v;
		});
		addSwitch("param", (sw, a) -> {
			param = true;
			String v = a.next(sw).trim();
			if ("".equals(paramSwitch)) paramSwitch = v; else paramSwitch += "," + v;
		});

		// Computation modes
		addSwitch("steadystate", "ss", new FlagSwitch(() -> steadystate = true));
		addSwitch("transient", "tr", (sw, a) -> { dotransient = true; transientTime = a.next(sw); });
		addSwitch("simpath", (sw, a) -> {
			simpath = true;
			simpathDetails = a.next(sw);
			simpathFilename = a.next(sw);
		});
		addSwitch("nobuild", new FlagSwitch(() -> nobuild = true));
		addSwitch("test", new FlagSwitch(() -> test = true));
		addSwitch("testall", new FlagSwitch(() -> { test = true; testExitsOnFail = false; }));
		addSwitch("test:umb", new FlagSwitch(() -> prism.setTestUMB(true)));

		// DD debugging (hidden)
		addSwitch("dddebug", new FlagSwitch(() -> jdd.DebugJDD.enable()));
		addSwitch("ddtraceall", new FlagSwitch(() -> jdd.DebugJDD.traceAll = true));
		addSwitch("ddtracefollowcopies", new FlagSwitch(() -> jdd.DebugJDD.traceFollowCopies = true));
		addSwitch("dddebugwarnfatal", new FlagSwitch(() -> jdd.DebugJDD.warningsAreFatal = true));
		addSwitch("dddebugwarnoff", new FlagSwitch(() -> jdd.DebugJDD.warningsOff = true));
		addSwitch("ddtrace", (sw, a) -> {
			String idStr = a.next(sw);
			try {
				jdd.DebugJDD.enableTracingForID(Integer.parseInt(idStr));
			} catch (NumberFormatException e) {
				errorAndExit("The -" + sw + " switch requires an integer argument (JDDNode ID)");
			}
		});

		// IMPORT OPTIONS:
		addSwitch("importpepa", new FlagSwitch(() -> importpepa = true));
		addSwitch("importprismpp", (sw, a) -> { importprismpp = true; prismppParams = a.next(sw); }); // hidden
		addSwitch("importmodel", new StringSwitch(this::processImportModelSwitch));
		addSwitch("importtrans", (sw, a) -> {
			// Recall model name in case needed as basename for model exports
			modelFilename = a.next(sw);
			modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.MODEL, ModelExportFormat.EXPLICIT, new File(modelFilename)));
		});
		addSwitch("importstates", new StringSwitch(s -> modelImportSources.add(
			new ModelImportSource(ModelExportTask.ModelExportEntity.STATES, ModelExportFormat.EXPLICIT, new File(s)))));
		addSwitch("importobs", new StringSwitch(s -> modelImportSources.add(
			new ModelImportSource(ModelExportTask.ModelExportEntity.OBSERVATIONS, ModelExportFormat.EXPLICIT, new File(s)))));
		addSwitch("importlabels", new StringSwitch(s -> modelImportSources.add(
			new ModelImportSource(ModelExportTask.ModelExportEntity.LABELS, ModelExportFormat.EXPLICIT, new File(s)))));
		addSwitch("importstaterewards", new StringSwitch(s -> modelImportSources.add(
			new ModelImportSource(ModelExportTask.ModelExportEntity.STATE_REWARDS, ModelExportFormat.EXPLICIT, new File(s)))));
		addSwitch("importtransrewards", new StringSwitch(s -> modelImportSources.add(
			new ModelImportSource(ModelExportTask.ModelExportEntity.TRANSITION_REWARDS, ModelExportFormat.EXPLICIT, new File(s)))));
		addSwitch("importinitdist", (sw, a) -> { importinitdist = true; importInitDistFilename = a.next(sw); });
		addSwitch("importresults", (sw, a) -> {
			importresults = true;
			modelFilename = "no-model-file.prism";
			importResultsFilename = a.next(sw);
		});
		addSwitch("dtmc", new FlagSwitch(() -> typeOverride = ModelType.DTMC));
		addSwitch("mdp",  new FlagSwitch(() -> typeOverride = ModelType.MDP));
		addSwitch("ctmc", new FlagSwitch(() -> typeOverride = ModelType.CTMC));

		// EXPORT OPTIONS:
		addSwitch("exportprism", (sw, a) -> { exportprism = true; exportPrismFilename = a.next(sw); });
		addSwitch("exportprismconst", (sw, a) -> { exportprismconst = true; exportPrismConstFilename = a.next(sw); });
		addSwitch("exportresults", new StringSwitch(this::processExportResultsSwitch));
		addSwitch("exportvector", (sw, a) -> {
			exportvector = true; exportVectorFilename = a.next(sw); prism.setStoreVector(true);
		});
		addSwitch("exportmodel", new StringSwitch(this::processExportModelSwitch));
		// process -exportmodelprecision in PrismSettings
		addSwitch("exporttrans", new StringSwitch(s -> modelExportTasks.add(
			new ModelExportTask(ModelExportTask.ModelExportEntity.MODEL, s))));
		addSwitch("exportstaterewards", new StringSwitch(s -> modelExportTasks.add(
			new ModelExportTask(ModelExportTask.ModelExportEntity.STATE_REWARDS, s))));
		addSwitch("exporttransrewards", new StringSwitch(s -> modelExportTasks.add(
			new ModelExportTask(ModelExportTask.ModelExportEntity.TRANSITION_REWARDS, s))));
		addSwitch("exportrewards", (sw, a) -> {
			modelExportTasks.add(new ModelExportTask(ModelExportTask.ModelExportEntity.STATE_REWARDS, a.next(sw)));
			modelExportTasks.add(new ModelExportTask(ModelExportTask.ModelExportEntity.TRANSITION_REWARDS, a.next(sw)));
		});
		addSwitch("exportstates", new StringSwitch(s -> modelExportTasks.add(
			new ModelExportTask(ModelExportTask.ModelExportEntity.STATES, s))));
		addSwitch("exportobs", new StringSwitch(s -> modelExportTasks.add(
			new ModelExportTask(ModelExportTask.ModelExportEntity.OBSERVATIONS, s))));
		addSwitch("exportlabels", new StringSwitch(this::processExportLabelsSwitch));
		addSwitch("exportproplabels", new StringSwitch(this::processExportPropLabelsSwitch));
		addSwitch("exportmatlab", new FlagSwitch(() -> {
			exportType = Prism.EXPORT_MATLAB;
			modelExportOptionsGlobal.setFormat(ModelExportFormat.MATLAB);
		}));
		addSwitch("exportmrmc", new FlagSwitch(() -> errorAndExit("Export to MRMC format no longer supported")));
		addSwitch("exportrows", new FlagSwitch(() -> {
			exportType = Prism.EXPORT_ROWS;
			modelExportOptionsGlobal.setExplicitRows(true);
		}));
		addSwitch("exportordered", "ordered", new FlagSwitch(() -> {})); // always done now, no-op
		addSwitch("exportunordered", "unordered", new FlagSwitch(() -> errorAndExit("Switch -exportunordered is no longer supported")));
		addSwitch("exporttransdot", new StringSwitch(s -> {
			ModelExportOptions exportOptions = new ModelExportOptions().setFormat(ModelExportFormat.DOT).setShowStates(false).setShowObservations(false);
			modelExportTasks.add(new ModelExportTask(ModelExportTask.ModelExportEntity.MODEL, s, exportOptions));
		}));
		addSwitch("exporttransdotstates", new StringSwitch(s -> {
			ModelExportOptions exportOptions = new ModelExportOptions().setFormat(ModelExportFormat.DOT).setShowStates(true).setShowObservations(true);
			modelExportTasks.add(new ModelExportTask(ModelExportTask.ModelExportEntity.MODEL, s, exportOptions));
		}));
		addSwitch("exportdot", new StringSwitch(s -> {
			ModelExportOptions exportOptions = new ModelExportOptions().setFormat(ModelExportFormat.DD_DOT);
			modelExportTasks.add(new ModelExportTask(ModelExportTask.ModelExportEntity.MODEL, s, exportOptions));
		}));
		addSwitch("exportsccs", (sw, a) -> { exportsccs = true; exportSCCsFilename = a.next(sw); });
		addSwitch("exportbsccs", (sw, a) -> { exportbsccs = true; exportBSCCsFilename = a.next(sw); });
		addSwitch("exportmecs", (sw, a) -> { exportmecs = true; exportMECsFilename = a.next(sw); });
		addSwitch("exportsteadystate", "exportss", (sw, a) -> {
			exportSteadyStateFilename = a.next(sw);
			steadystate = true; // compute if asked to export
		});
		addSwitch("exporttransient", "exporttr", new StringSwitch(s -> exportTransientFilename = s));
		addSwitch("exportstrat", new StringSwitch(this::processExportStratSwitch));
		addSwitch("exportdigital", new StringSwitch(s -> {
			File f = s.equals("stdout") ? null : new File(s);
			prism.setExportDigital(true);
			prism.setExportDigitalFile(f);
		}));
		addSwitch("exporttarget", new StringSwitch(s -> {      // hidden
			prism.setExportTarget(true); prism.setExportTargetFilename(s);
		}));
		addSwitch("exportprodtrans", new StringSwitch(s -> {   // hidden
			prism.setExportProductTrans(true); prism.setExportProductTransFilename(s);
		}));
		addSwitch("exportprodstates", new StringSwitch(s -> {  // hidden
			prism.setExportProductStates(true); prism.setExportProductStatesFilename(s);
		}));
		addSwitch("exportprodvector", new StringSwitch(s -> {  // hidden
			prism.setExportProductVector(true); prism.setExportProductVectorFilename(s);
		}));

		// NB: Following the ordering of the -help text, more options go here,
		// but these are processed in the PrismSettings class; see below.

		// SIMULATION OPTIONS:
		addSwitch("sim", new FlagSwitch(() -> simulate = true));
		addSwitch("simmethod", new EnumSwitch()
			.when("ci",   () -> simMethodName = "ci")
			.when("aci",  () -> simMethodName = "aci")
			.when("apmc", () -> simMethodName = "apmc")
			.when("sprt", () -> simMethodName = "sprt"));
		addSwitch("simsamples", (sw, a) -> {
			int n = a.nextInt(sw);
			if (n <= 0) errorAndExit("Invalid value for -" + sw + " switch");
			simNumSamples = n; simNumSamplesGiven = true;
		});
		addSwitch("simconf", (sw, a) -> {
			double d = a.nextDouble(sw);
			if (d <= 0 || d >= 1) errorAndExit("Invalid value for -" + sw + " switch");
			simConfidence = d; simConfidenceGiven = true;
		});
		addSwitch("simwidth", (sw, a) -> {
			double d = a.nextDouble(sw);
			if (d <= 0) errorAndExit("Invalid value for -" + sw + " switch");
			simWidth = d; simWidthGiven = true;
		});
		addSwitch("simapprox", (sw, a) -> {
			double d = a.nextDouble(sw);
			if (d <= 0) errorAndExit("Invalid value for -" + sw + " switch");
			simApprox = d; simApproxGiven = true;
		});
		addSwitch("simmanual", new FlagSwitch(() -> simManual = true));
		addSwitch("simvar", (sw, a) -> {
			int n = a.nextInt(sw);
			if (n <= 0) errorAndExit("Invalid value for -" + sw + " switch");
			reqIterToConclude = n; reqIterToConcludeGiven = true;
		});
		addSwitch("simmaxrwd", (sw, a) -> {
			double d = a.nextDouble(sw);
			if (d <= 0.0) errorAndExit("Invalid value for -" + sw + " switch");
			simMaxReward = d; simMaxRewardGiven = true;
		});
		addSwitch("simpathlen", (sw, a) -> {
			long n = a.nextLong(sw);
			if (n <= 0) errorAndExit("Invalid value for -" + sw + " switch");
			simMaxPath = n; simMaxPathGiven = true;
		});

		// FURTHER OPTIONS:
		addSwitch("zerorewardcheck", new FlagSwitch(() -> prism.setCheckZeroLoops(true)));
		addSwitch("explicitbuild", new FlagSwitch(() -> explicitbuild = true));
		addSwitch("explicitbuildtest", new FlagSwitch(() -> explicitbuildtest = true)); // hidden

		// MISCELLANEOUS HIDDEN OPTIONS:
		addSwitch("mainlog", new StringSwitch(this::processMainLogSwitch));
		addSwitch("exportmodeldotview", new FlagSwitch(() -> exportmodeldotview = true));
		addSwitch("c1", new FlagSwitch(() -> prism.setConstruction(1)));
		addSwitch("c2", new FlagSwitch(() -> prism.setConstruction(2)));
		addSwitch("c3", new FlagSwitch(() -> prism.setConstruction(3)));
		addSwitch("o1", new FlagSwitch(() -> { prism.setOrdering(1); orderingOverride = true; }));
		addSwitch("o2", new FlagSwitch(() -> { prism.setOrdering(2); orderingOverride = true; }));
		addSwitch("noreach", new FlagSwitch(() -> prism.setDoReach(false)));
		addSwitch("nobscc",  new FlagSwitch(() -> prism.setBSCCComp(false)));
		addSwitch("frontier", new FlagSwitch(() -> prism.setReachMethod(Prism.REACH_FRONTIER)));
		addSwitch("bfs",      new FlagSwitch(() -> prism.setReachMethod(Prism.REACH_BFS)));
		addSwitch("bisim",    new FlagSwitch(() -> prism.setDoBisim(true)));
	}

	/** Register a switch handler under a single name. */
	private void addSwitch(String name, SwitchHandler h)
	{
		switchHandlers.put(name, h);
	}

	/** Register a switch handler under two names (primary + alias). */
	private void addSwitch(String n1, String n2, SwitchHandler h)
	{
		switchHandlers.put(n1, h);
		switchHandlers.put(n2, h);
	}

	/** Register a switch handler under three names (primary + two aliases). */
	private void addSwitch(String n1, String n2, String n3, SwitchHandler h)
	{
		switchHandlers.put(n1, h);
		switchHandlers.put(n2, h);
		switchHandlers.put(n3, h);
	}

	/**
	 * Process the non-switch command-line arguments,
	 * which should be (model/properties) file names.
	 */
	private void processFileNames(List<String> filenameArgs) throws PrismException
	{
		if (filenameArgs.size() > 2) {
			errorAndExit("Invalid argument syntax");
		}
		if (!modelImportSources.isEmpty()) {
			if (filenameArgs.size() > 1) {
				errorAndExit("Two models provided (" + filenameArgs.get(0) + ", " + modelFilename + ")");
			} else if (filenameArgs.size() == 1) {
				propertiesFilename = filenameArgs.get(0);
			}
		} else {
			if (filenameArgs.size() > 0) {
				modelFilename = filenameArgs.get(0);
				if (modelFilename.endsWith(".all")) {
					processImportModelSwitch(modelFilename);
				}
			}
			if (filenameArgs.size() > 1) {
				propertiesFilename = filenameArgs.get(1);
			}
		}
	}

	/** Process the argument to the -exportresults switch. */
	private void processExportResultsSwitch(String filesOptionsString) throws PrismException
	{
		exportresults = true;
		// Split into filename/options; also accept , as a legacy separator if : is absent
		String[] halves = splitFilesAndOptions(filesOptionsString);
		if (halves[1].isEmpty() && halves[0].indexOf(',') > -1) {
			int comma = halves[0].indexOf(',');
			halves[1] = halves[0].substring(comma + 1);
			halves[0] = halves[0].substring(0, comma);
		}
		exportResultsFilename = halves[0];
		exportShape = ResultsExportShape.LIST_PLAIN;
		boolean[] isCsv = {false}, isMatrix = {false};
		new OptionParser()
			.flag("csv",       () -> isCsv[0] = true)
			.flag("matrix",    () -> isMatrix[0] = true)
			.flag("dataframe", () -> exportShape = ResultsExportShape.DATA_FRAME)
			.flag("comment",   () -> exportShape = ResultsExportShape.COMMENT)
			.parse(halves[1], "exportresults");
		if (exportShape == ResultsExportShape.LIST_PLAIN)
			exportShape = isCsv[0] ? (isMatrix[0] ? ResultsExportShape.MATRIX_CSV   : ResultsExportShape.LIST_CSV)
			                       : (isMatrix[0] ? ResultsExportShape.MATRIX_PLAIN  : ResultsExportShape.LIST_PLAIN);
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
		String basename = i == -1 ? filesString : filesString.substring(0, i);
		String extList = i == -1 ? "" : filesString.substring(i + 1);
		String exts[] = extList.split(",");
		// Process file extensions
		importModelWarning = null;
		for (String ext : exts) {
			// Items to import
			if (ext.equals("all")) {
				modelFilename = basename + ".tra";
				addModelImport(ModelExportTask.ModelExportEntity.MODEL,basename + ".tra", false);
				addModelImport(ModelExportTask.ModelExportEntity.STATES,basename + ".sta", false);
				addModelImport(ModelExportTask.ModelExportEntity.OBSERVATIONS,basename + ".obs", false);
				addModelImport(ModelExportTask.ModelExportEntity.LABELS,basename + ".lab", false);
				addStateRewardImports(basename, false);
				addTransitionRewardImports(basename, false);
			} else if (ext.equals("tra")) {
				modelFilename = basename + ".tra";
				addModelImport(ModelExportTask.ModelExportEntity.MODEL,basename + ".tra", true);
			} else if (ext.equals("sta")) {
				addModelImport(ModelExportTask.ModelExportEntity.STATES,basename + ".sta", true);
			} else if (ext.equals("obs")) {
				addModelImport(ModelExportTask.ModelExportEntity.OBSERVATIONS,basename + ".obs", true);
			} else if (ext.equals("lab")) {
				addModelImport(ModelExportTask.ModelExportEntity.LABELS,basename + ".lab", true);
			} else if (ext.equals("srew")) {
				addStateRewardImports(basename, true);
			} else if (ext.equals("trew")) {
				addTransitionRewardImports(basename, true);
			} else if (ext.equals("umb")) {
				modelFilename = basename + ".umb";;
				modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.MODEL, ModelExportFormat.UMB, new File(basename + ".umb")));
			}
			// For any other extension (including none/unknown), default to explicit (.tra)
			else {
				modelFilename = basename + (ext.isEmpty() ? "" : "." + ext);
				addModelImport(ModelExportTask.ModelExportEntity.MODEL,modelFilename, true);
			}
		}
		// Process options
		new OptionParser()
			.choice("format", new OptionParser.Choice()
				.when("explicit", () -> { for (ModelImportSource s : modelImportSources) s.format = ModelExportFormat.EXPLICIT; })
				.when("umb",      () -> { for (ModelImportSource s : modelImportSources) s.format = ModelExportFormat.UMB; }))
			.parse(optionsString, "importmodel");
	}

	/**
	 * Add a model import file to {@code modelImportSources}.
	 * @param entity Model entity
	 * @param filename Model import filename
	 * @param assumeExists If true, we add the file even if it does not exist
	 */
	private void addModelImport(ModelExportTask.ModelExportEntity entity, String filename, boolean assumeExists)
	{
		if (assumeExists || new File(filename).exists()) {
			modelImportSources.add(new ModelImportSource(entity, ModelExportFormat.EXPLICIT, new File(filename)));
		}
	}

	/**
	 * Given a file basename, find corresponding .srew files
	 * and add them to {@code modelImportSources}.
	 * "corresponding" means basename.srew, or a set basename1.srew, ...
	 * If any are present, {@code importstaterewards} is set to true.
	 * 
	 * If {@code assumeExists} is true, then we add basename.srew regardless,
	 * typically because the user has told us it should be there.
	 */
	private void addStateRewardImports(String basename, boolean assumeExists)
	{
		boolean found = false;
		if (new File(basename + ".srew").exists()) {
			modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.STATE_REWARDS, ModelExportFormat.EXPLICIT, new File(basename + ".srew")));
			found = true;
		} else {
			int index = 1;
			while (true) {
				if (new File(basename + String.valueOf(index) + ".srew").exists()) {
					modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.STATE_REWARDS, ModelExportFormat.EXPLICIT, new File(basename + String.valueOf(index) + ".srew")));
					found = true;
					index++;
				} else {
					break;
				}
			}
		}
		if (assumeExists && !found) {
			modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.STATE_REWARDS, ModelExportFormat.EXPLICIT, new File(basename + ".srew")));
		}
	}

	/**
	 * Given a file basename, find corresponding .trew files
	 * and add them to {@code modelImportSources}.
	 * "corresponding" means basename.srew, or a set basename1.srew, ...
	 * If any are present, {@code importtransrewards} is set to true.
	 *
	 * If {@code assumeExists} is true, then we add basename.srew regardless,
	 * typically because the user has told us it should be there.
	 */
	private void addTransitionRewardImports(String basename, boolean assumeExists)
	{
		boolean found = false;
		if (new File(basename + ".trew").exists()) {
			modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.TRANSITION_REWARDS, ModelExportFormat.EXPLICIT, new File(basename + ".trew")));
			found = true;
		} else {
			int index = 1;
			while (true) {
				if (new File(basename + String.valueOf(index) + ".trew").exists()) {
					modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.TRANSITION_REWARDS, ModelExportFormat.EXPLICIT, new File(basename + String.valueOf(index) + ".trew")));
					found = true;
					index++;
				} else {
					break;
				}
			}
		}
		if (assumeExists && !found) {
			modelImportSources.add(new ModelImportSource(ModelExportTask.ModelExportEntity.TRANSITION_REWARDS, ModelExportFormat.EXPLICIT, new File(basename + ".trew")));
		}
	}

	/**
	 * Process the arguments (file, options) to the -exportlabels switch.
	 */
	private void processExportLabelsSwitch(String filesOptionsString) throws PrismException
	{
		// Split into files/options (on :)
		String pair[] = splitFilesAndOptions(filesOptionsString);
		ModelExportTask newExportTask = new ModelExportTask(ModelExportTask.ModelExportEntity.LABELS, pair[0]);
		new OptionParser()
			.flag("matlab",     () -> newExportTask.getExportOptions().setFormat(ModelExportFormat.MATLAB))
			.flag("proplabels", () -> newExportTask.setLabelExportSet(ModelExportTask.LabelExportSet.ALL))
			.parse(pair[1], "exportlabels");
		modelExportTasks.add(newExportTask);
	}

	/**
	 * Process the arguments (file, options) to the -exportproplabels switch.
	 */
	private void processExportPropLabelsSwitch(String filesOptionsString) throws PrismException
	{
		// Split into files/options (on :)
		String pair[] = splitFilesAndOptions(filesOptionsString);
		ModelExportTask newExportTask = new ModelExportTask(ModelExportTask.ModelExportEntity.LABELS, pair[0]);
		newExportTask.setLabelExportSet(ModelExportTask.LabelExportSet.EXTRA);
		new OptionParser()
			.flag("matlab", () -> newExportTask.getExportOptions().setFormat(ModelExportFormat.MATLAB))
			.parse(pair[1], "exportproplabels");
		modelExportTasks.add(newExportTask);
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
		String basename = i == -1 ? filesString : filesString.substring(0, i);
		String extList = i == -1 ? "" : filesString.substring(i + 1);
		String exts[] = extList.split(",");
		// Process file extensions
		List<ModelExportTask> newModelExportTasks = new ArrayList<>();
		for (String ext : exts) {
			// Some extensions get expanded to multiple exports
			if (ext.equals("all")) {
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "tra"));
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "srew"));
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "trew"));
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "sta"));
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "obs"));
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "lab"));
			} else if (ext.equals("rew")) {
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "srew"));
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, "trew"));
			}
			// For any other extension (including none/unknown), deduce export
			else {
				newModelExportTasks.add(ModelExportTask.fromFilename(basename, ext));
			}
		}
		// Process options
		ModelExportOptions exportOptions = new ModelExportOptions();
		new OptionParser()
			.flag("matlab",     () -> { exportOptions.setFormat(ModelExportFormat.MATLAB); exportType = Prism.EXPORT_MATLAB; })
			.flag("rows",       () -> { exportOptions.setExplicitRows(true); exportType = Prism.EXPORT_ROWS; })
			.flag("text",       () -> exportOptions.setBinaryAsText(true))
			.flag("proplabels", () -> {
				for (ModelExportTask t : newModelExportTasks)
					if (t.getEntity() == ModelExportTask.ModelExportEntity.LABELS)
						t.setLabelExportSet(ModelExportTask.LabelExportSet.ALL);
			})
			.choice("format", new OptionParser.Choice()
				.when("explicit", () -> exportOptions.setFormat(ModelExportFormat.EXPLICIT))
				.when("matlab",   () -> exportOptions.setFormat(ModelExportFormat.MATLAB))
				.when("dot",      () -> exportOptions.setFormat(ModelExportFormat.DOT))
				.when("drn",      () -> exportOptions.setFormat(ModelExportFormat.DRN))
				.when("umb",      () -> exportOptions.setFormat(ModelExportFormat.UMB)))
			.bool("labels",   v -> exportOptions.setShowLabels(v))
			.bool("rewards",  v -> exportOptions.setShowRewards(v))
			.bool("states",   v -> exportOptions.setShowStates(v))
			.bool("obs",      v -> exportOptions.setShowObservations(v))
			.bool("actions",  v -> exportOptions.setShowActions(v))
			.bool("headers",  v -> exportOptions.setPrintHeaders(v))
			.string("precision", v -> {
				try {
					int n = Integer.parseInt(v);
					if (!RANGE_EXPORT_DOUBLE_PRECISION.contains(n)) throw new NumberFormatException();
					exportOptions.setModelPrecision(n);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value \"" + v + "\" for \"precision\" option of -exportmodel");
				}
			})
			.choice("zip", new OptionParser.Choice()
				.when("true",       () -> exportOptions.setZipped(true))
				.when("false",      () -> exportOptions.setZipped(false))
				.when("gzip", "gz", () -> exportOptions.setZipped(true).setCompressionFormat(ModelExportOptions.CompressionFormat.GZIP))
				.when("xz",         () -> exportOptions.setZipped(true).setCompressionFormat(ModelExportOptions.CompressionFormat.XZ)))
			.parse(optionsString, "exportmodel");
		// Apply options from this switch to each export task
		for (ModelExportTask exportTask : newModelExportTasks) {
			exportTask.getExportOptions().apply(exportOptions);
		}
		// Add export tasks to the main list
		modelExportTasks.addAll(newModelExportTasks);
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
		// Split file into basename/extension
		int i = fileString.lastIndexOf('.');
		String basename = i == -1 ? fileString : fileString.substring(0, i);
		String ext = i == -1 ? "" : fileString.substring(i + 1);
		// Store some settings (here and in PRISM)
		exportstrat = true;
		exportStratFilename = basename.equals("stdout") ? "stdout" : fileString;
		exportStratOptions = new StrategyExportOptions();
		prism.setGenStrat(true);
		// Default strategy export type is based on filename extension
		if (ext.equals("tra")) {
			exportStratOptions.setType(StrategyExportOptions.StrategyExportType.INDUCED_MODEL);
		} else if (ext.equals("dot")) {
			exportStratOptions.setType(StrategyExportOptions.StrategyExportType.DOT_FILE);
		} else {
			exportStratOptions.setType(StrategyExportOptions.StrategyExportType.ACTIONS);
		}
		// Process options
		new OptionParser()
			.choice("type", new OptionParser.Choice()
				.when("actions",          () -> exportStratOptions.setType(StrategyExportOptions.StrategyExportType.ACTIONS))
				.when("indices",          () -> exportStratOptions.setType(StrategyExportOptions.StrategyExportType.INDICES))
				.when("model", "induced", () -> exportStratOptions.setType(StrategyExportOptions.StrategyExportType.INDUCED_MODEL))
				.when("dot",              () -> exportStratOptions.setType(StrategyExportOptions.StrategyExportType.DOT_FILE)))
			.choice("mode", new OptionParser.Choice()
				.when("restrict", () -> exportStratOptions.setMode(StrategyExportOptions.InducedModelMode.RESTRICT))
				.when("reduce",   () -> exportStratOptions.setMode(StrategyExportOptions.InducedModelMode.REDUCE)))
			.bool("reach",  v -> exportStratOptions.setReachOnly(v))
			.bool("states", v -> exportStratOptions.setShowStates(v))
			.bool("obs",    v -> exportStratOptions.setMergeObservations(v))
			.parse(optionsString, "exportstrat");
	}

	/**
	 * Process the arguments (file, options) to the -mainlog switch
	 */
	private void processMainLogSwitch(String filesOptionsString) throws PrismException
	{
		// Split into file/options (on :)
		String halves[] = splitFilesAndOptions(filesOptionsString);
		String filename = halves[0];
		String optionsString = halves[1];
		// Process options
		boolean[] append = { false };
		new OptionParser()
			.flag("append", () -> append[0] = true)
			.parse(optionsString, "mainlog");
		// Open the log
		try {
			mainLog = new PrismFileLog(filename, append[0]);
			prism.setMainLog(mainLog);
		} catch (PrismException e) {
			errorAndExit("Couldn't open log file \"" + filename + "\"");
		}
	}

	/**
	 * Split a string of the form <files>:<options> into its two parts.
	 * The latter can be empty, in which case the : is optional.
	 * Instances of :\ are ignored (not treated as :) in case there is a Windows filename.
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
			s = shellQuoteSingleIfNecessary(s);
			mainLog.print(" " + s);
		}
		mainLog.println();
	}

	/**
	 * For a command-line argument, returns a quoted version
	 * with single quotes if it contains unsafe characters.
	 * Otherwise, just returns the unquoted argument.
	 */
	public static String shellQuoteSingleIfNecessary(String arg)
	{
		if (arg.isEmpty()) {
			// empty argument needs to be quoted
			return "''";
		}

		// If necessary add quotes so can be pasted back into a shell
		// (where "necessary" means contains any non-safe characters)
		if (arg.matches(".*[^_a-zA-Z0-9\\./\\-=].*")) {
			// argument needs quoting, so we surround with single quotes,
			// which neutralises all characters except '
			// for that we have to have special handling, replacing ' by '\''
			// (close quote, escaped-', open quote again)
			arg = arg.replace("'", "'\\''");
			arg = "'" + arg + "'";
		}
		return arg;
	}

	// do some processing of the options

	private void processOptions() throws PrismException
	{
		int j;

		// make sure a model file is specified
		if (modelFilename == null) {
			mainLog.println("Usage: " + Prism.getCommandLineName() + " [options] <model-file> [<properties-file>] [more-options]");
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
		// (if we are in test mode, we ignore here; will lead to appropriate 'not supported' error handling during testing)
		if (prism.getEngine() == Prism.MTBDD && !test) {
			j = prism.getLinEqMethod();
			if (j == Prism.GAUSSSEIDEL || j == Prism.BGAUSSSEIDEL || j == Prism.PGAUSSSEIDEL || j == Prism.BPGAUSSSEIDEL) {
				errorAndExit("Gauss-Seidel and its variants are currently not supported by the MTBDD engine");
			}
			if (j == Prism.SOR || j == Prism.BSOR || j == Prism.PSOR || j == Prism.BPSOR) {
				errorAndExit("SOR and its variants are currently not supported by the MTBDD engine");
			}
		}

		// or pseudo methods with sparse engine
		// (if we are in test mode, we ignore here; will lead to appropriate 'not supported' error handling during testing)
		else if (prism.getEngine() == Prism.SPARSE && !test) {
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

		// Plug in model basename for model exports where needed
		if (!modelExportTasks.isEmpty()) {
			String modelFileBasename = modelFilename;
			if (modelFileBasename.lastIndexOf('.') > -1) {
				modelFileBasename = modelFilename.substring(0, modelFileBasename.lastIndexOf('.'));
			}
			for (ModelExportTask exportTask : modelExportTasks) {
				exportTask.replaceEmptyFileBasename(modelFileBasename);
			}
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
		mainLog.println("Usage: " + Prism.getCommandLineName() + " [options] <model-file> [<properties-file>] [more-options]");
		mainLog.println();
		mainLog.println("Options:");
		mainLog.println("========");
		mainLog.println();
		mainLog.println("-help .......................... Display this help message");
		mainLog.println("-version ....................... Display PRISM version info");
		mainLog.println("-javaversion ................... Display Java version info");
		mainLog.println("-dir <dir> ..................... Set current working directory");
		mainLog.println("-settings <file>................ Load settings from <file>");
		mainLog.println();
		mainLog.println("-pf <props> (or -pctl or -csl) . Model check properties <props>");
		mainLog.println("-property <refs> (or -prop) .... Only model check properties included in list <refs> of indices/names");
		mainLog.println("-const <vals> .................. Define constant values as <vals> (e.g. for experiments)");
		mainLog.println("-steadystate (or -ss) .......... Compute steady-state probabilities (D/CTMCs only)");
		mainLog.println("-transient <x> (or -tr <x>) .... Compute transient probabilities for time (or time range) <x> (D/CTMCs only)");
		mainLog.println("-simpath <options> <file>....... Generate a random path with the simulator");
		mainLog.println("-nobuild ....................... Skip model construction (just do parse/export)");
		mainLog.println("-test .......................... Enable \"test\" mode");
		mainLog.println("-testall ....................... Enable \"test\" mode, but don't exit on error");
		mainLog.println("-javamaxmem <x>................. Set the maximum heap size for Java, e.g. 500m, 4g [default: 1g]");
		mainLog.println("-javastack <x> ................. Set the Java stack size [default: 4m]");
		mainLog.println("-javaparams <x>................. Pass additional command-line arguments to Java");
		mainLog.println("-timeout <n> ................... Exit after a time-out of <n> seconds if not already terminated");
		mainLog.println("-ng ............................ Run PRISM in Nailgun server mode; subsequent calls are then made via \"ngprism\"");
		mainLog.println();
		mainLog.println("IMPORTS:");
		mainLog.println("-importpepa .................... Model description is in PEPA, not the PRISM language");
		mainLog.println("-importmodel <files> ........... Import the model directly from text file(s)");
		mainLog.println("-importtrans <file> ............ Import the transition matrix directly from a text file");
		mainLog.println("-importstates <file>............ Import the list of states directly from a text file");
		mainLog.println("-importobs <file>............... Import the list of observations directly from a text file");
		mainLog.println("-importlabels <file>............ Import the list of labels directly from a text file");
		mainLog.println("-importstaterewards <file>...... Import the state rewards directly from a text file");
		mainLog.println("-importtransrewards <file>...... Import the transition rewards directly from a text file");
		mainLog.println("-importinitdist <file>.......... Specify initial probability distribution for transient/steady-state analysis");
		mainLog.println("-dtmc .......................... Force imported/built model to be a DTMC");
		mainLog.println("-ctmc .......................... Force imported/built model to be a CTMC");
		mainLog.println("-mdp ........................... Force imported/built model to be an MDP");
		mainLog.println("-importresults <file> .......... Import results from a data frame stored in CSV file");
		mainLog.println();
		mainLog.println("EXPORTS:");
		mainLog.println("-exportresults <file[:options]>  Export the results of model checking to a file");
		mainLog.println("-exportvector <file>  .......... Export results of model checking for all states to a file");
		mainLog.println("-exportmodel <files[:options]> . Export the built model to file(s)");
		mainLog.println("-exporttrans <file> ............ Export the transition matrix to a file");
		mainLog.println("-exportstaterewards <file> ..... Export the state rewards vector to a file");
		mainLog.println("-exporttransrewards <file> ..... Export the transition rewards matrix to a file");
		mainLog.println("-exportrewards <file1> <file2>.. Export state/transition rewards to files 1/2");
		mainLog.println("-exportstates <file> ........... Export the list of reachable states to a file");
		mainLog.println("-exportobs <file> .............. Export the list of observations to a file");
		mainLog.println("-exportlabels <file[:options]> . Export the list of labels and satisfying states to a file");
		mainLog.println("-exportproplabels <file[:opt]> . Export the list of labels and satisfying states from the properties file to a file");
		mainLog.println("-exportstrat <file[:options]> .. Generate and export a strategy to a file");
		mainLog.println("-exportmatlab .................. When exporting matrices/vectors/labels/etc., use Matlab format");
		mainLog.println("-exportrows .................... When exporting matrices, put a whole row on one line");
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
			mainLog.println("Switch: -importmodel <files>[:options]\n");
			mainLog.println("Import the model directly from one or more file(s).");
			mainLog.println("Use a list of file extensions to indicate which files should be read, e.g.:");
			mainLog.println("\n -importmodel in.tra,sta\n");
			mainLog.println("Possible extensions are: .tra, .sta, .obs, .lab, .srew, .trew, .umb");
			mainLog.println("Use extension .all to import all explicit files (.tra/sta/obs/lab/srew/trew), e.g.:");
			mainLog.println("\n -importmodel in.all\n");
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * format (=explicit/umb) - model import format");
		}
		// -importresults
		else if (sw.equals("importresults")) {
			mainLog.println("Switch: -importresults <file>\n");
			mainLog.println("Import results from a data frame stored as comma-separated values in <file>.");
		}			
		// -exportresults
		else if (sw.equals("exportresults")) {
			mainLog.println("Switch: -exportresults <file[:options]>\n");
			mainLog.println("Exports the results of model checking to <file> (or to the screen if <file>=\"stdout\").");
			mainLog.println("The default behaviour is to export a list of results in text form, using tabs to separate items.");
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * csv - Export results as comma-separated values");
			mainLog.println(" * matrix - Export results as one or more 2D matrices (e.g. for surface plots)");
			mainLog.println(" * dataframe - Export results as dataframe in comma-separated values)");
			mainLog.println(" * comment - Export results in comment format for regression testing)");
		}
		// -exportlabels
		else if (sw.equals("exportlabels")) {
			mainLog.println("Switch: -exportlabels <files[:options]>\n");
			mainLog.println("Export the list of labels and satisfying states to a file (or to the screen if <file>=\"stdout\").");
			mainLog.println();
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * matlab - export data in Matlab format");
			mainLog.println(" * proplabels - export labels from a properties file into the same file, too");
		}
		// -exportproplabels
		else if (sw.equals("exportproplabels")) {
			mainLog.println("Switch: -exportproplabels <files[:options]>\n");
			mainLog.println("Export the list of labels and satisfying states from the properties file to a file (or to the screen if <file>=\"stdout\").");
			mainLog.println();
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * matlab - export data in Matlab format");
		}
		// -exportmodel
		else if (sw.equals("exportmodel")) {
			mainLog.println("Switch: -exportmodel <files[:options]>\n");
			mainLog.println("Export the built model to file(s) (or to the screen if <file>=\"stdout\").");
			mainLog.println("Use a list of file extensions to indicate which files should be generated, e.g.:");
			mainLog.println("\n -exportmodel out.tra,sta\n");
			mainLog.println("\n -exportmodel out.umb\n");
			mainLog.println("Possible extensions are: .tra, .srew, .trew, .lab, .sta, .obs, .dot, .umb, .drn");
			mainLog.println("Use extension .all to export all explicit files (.tra/srew/trew/lab/sta/obs), e.g.:");
			mainLog.println("\n -exportmodel out.all\n");
			mainLog.println("Omit the file basename to use the basename of the model file, e.g.:");
			mainLog.println("\n -exportmodel .all\n");
			mainLog.println("Use extension .rew to export both .srew/.trew files");
			mainLog.println();
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * format (=explicit/matlab/dot/drn/umb) - model export format");
			mainLog.println(" * rewards (=true/false) - whether to include rewards");
			mainLog.println(" * labels (=true/false) - whether to include labels");
			mainLog.println(" * states (=true/false) - whether to include state definitions");
			mainLog.println(" * obs (=true/false) - whether to include observation definitions");
			mainLog.println(" * actions (=true/false) - whether to include actions on choices/transitions");
			mainLog.println(" * precision (=<n>) - use <n> significant figures for floating point values (in text)");
			mainLog.println(" * zip (=true/false) - whether to zip UMB files");
			mainLog.println(" * text - show binary formats in textual form ");
			mainLog.println(" * matlab - same as format=matlab");
			mainLog.println("For the explicit files format:");
			mainLog.println(" * rows - export matrices with one row/distribution on each line");
			mainLog.println(" * proplabels - also export labels from a properties file into the same file, too");
			mainLog.println(" * headers (=true/false) - include headers in explicit (reward) files");
		}
		// -exportstrat
		else if (sw.equals("exportstrat")) {
			mainLog.println("Switch: -exportstrat <file[:options]>\n");
			mainLog.println("Generate and export a strategy to a file (or to the screen if <file>=\"stdout\").");
			mainLog.println("Use file extension .tra or .dot to export as an induced model or Dot file, respectively.");
			mainLog.println("If provided, <options> is a comma-separated list of options taken from:");
			mainLog.println(" * type (=actions/induced/dot) - type of strategy export");
			mainLog.println(" * mode (=restrict/reduce) - mode to use for building induced model (or Dot file)");
			mainLog.println(" * reach (=true/false) - whether to restrict the strategy to its reachable states");
			mainLog.println(" * states (=true/false) - whether to show states, rather than state indices, for actions lists or Dot files");
			mainLog.println(" * obs (=true/false) - for partially observable models, whether to merge observationally equivalent states");
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
		mainLog.println(Prism.getToolName() + " version " + Prism.getVersion());
	}

	/**
	 * Print out a list of all PRISM language keywords.
	 */
	private void printListOfKeywords()
	{
		List<String> list = Prism.getListOfKeywords();
		mainLog.print("PRISM keywords:");
		for (String s : list) {
			mainLog.print(" " + s);
		}
		mainLog.println();
	}

	/**
	 * Report a (non-fatal) error to the log.
	 * In test mode, this _will_ result in an exit,
	 * unless we are in test-all mode or the passed in error
	 * is a PrismNotSupportedException, which is not
	 * treated as a normal error (e.g., by prism-auto/prism-test)
	 */
	private void error(PrismException e)
	{
		error(e.getMessage(), e instanceof PrismNotSupportedException);
	}

	/**
	 * Report a (non-fatal) error to the log.
	 * In test (but not test-all) mode, this _will_ result in an exit.
	 */
	private void error(String s)
	{
		error(s, false);
	}

	/**
	 * Report a (non-fatal) error to the log.
	 * In test (but not test-all) mode, this _will_ result in an exit.
	 * The latter can be overridden by setting dontExit to true.
	 */
	private void error(String s, boolean dontExit)
	{
		// If (and only if) we are in "test" (and not "testall") mode, treat any error as fatal
		if (test && testExitsOnFail && !dontExit) {
			errorAndExit(s);
		}
		// Normal case: just display error message, but don't exit
		mainLog.println("\nError: " + s + ".");
		mainLog.flush();
	}

	/**
	 * Report a (fatal) error and exit cleanly (with exit code 1).
	 */
	private void errorAndExit(String s)
	{
		prism.closeDown(false);
		mainLog.println("\nError: " + s + ".");
		mainLog.flush();
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
		// Normal operation: just run PrismCL
		if (!(args.length > 0 && "-ng".equals(args[0]))) {
			new PrismCL().go(args);
		}
		// Nailgun server mode (-ng switch)
		else {
			try {
				System.out.println("Starting PRISM-Nailgun server...");
				com.martiansoftware.nailgun.NGServer.main(new String[0]);
			} catch (NumberFormatException | UnknownHostException e) {
				System.out.println("Failed to launch Nailgun server: " + e);
			}
		}
	}
}

//------------------------------------------------------------------------------
