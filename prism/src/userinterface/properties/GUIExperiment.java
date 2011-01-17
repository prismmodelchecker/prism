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

package userinterface.properties;

import parser.*;
import parser.ast.*;
import parser.type.*;
import prism.*;
import pta.DigitalClocks;

import javax.swing.*;

import simulator.method.APMCapproximation;
import userinterface.*;
import java.util.*;
import userinterface.util.*;
import userinterface.simulator.networking.*;

public class GUIExperiment
{
	private GUIMultiProperties guiProp;
	private GUIExperimentTable table;
	private prism.ResultsCollection results;
	private String modString;
	private boolean finished = false;

	private ModulesFile mod;
	private UndefinedConstants cons;
	private PropertiesFile prop; //contains 1 property only

	private boolean running = false;

	private Thread theThread;

	private boolean useSimulation;

	private Values definedMFConstants;
	private Values definedPFConstants;
	private Result res;

	/** Creates a new instance of GUIExperiment */
	public GUIExperiment(GUIExperimentTable table, GUIMultiProperties guiProp, PropertiesFile prop, UndefinedConstants cons, ModulesFile mod, String modString,
			boolean useSimulation)
	{
		this.table = table;
		this.guiProp = guiProp;
		this.prop = prop;
		this.cons = cons;
		this.mod = mod;
		this.modString = modString;
		this.useSimulation = useSimulation;

		results = new prism.ResultsCollection(cons, prop.getProperty(0).getResultName());
	}

	//ACCESS METHODS

	public int getTotalIterations()
	{
		return cons.getNumIterations();
	}

	public int getCurrentIterations()
	{
		return results.getCurrentIteration();
	}

	public Vector getRangingConstants()
	{
		return cons.getRangingConstants();
	}

	public String getDefinedConstantsString()
	{
		return cons.getDefinedConstantsString();
	}

	public String getPFDefinedConstantsString()
	{
		return cons.getPFDefinedConstantsString();
	}

	public String getPropertyString()
	{
		return prop.getProperty(0).toString();
	}

	public Type getPropertyType()
	{
		return prop.getProperty(0).getType();
	}

	public ResultsCollection getResults()
	{
		return results;
	}

	public boolean isFinished()
	{
		return finished;
	}

	public boolean isUseSimulation()
	{
		return useSimulation;
	}

	public GUIExperimentTable getTable()
	{
		return table;
	}

	//UPDATE METHODS

	public void startExperiment()
	{
		theThread = new ExperimentThread(guiProp, this, cons, mod, prop);
		running = true;
		theThread.start();
	}

	public synchronized void experimentDone()
	{
		running = false;
		theThread = null;
		finished = true;
		this.table.repaint();
	}

	// note: presently, this is never actually called
	// (in case of errors, these are stored as the results of the experiment and things end normally)
	public synchronized void experimentFailed()
	{
		running = false;
		theThread = null;
		finished = true;
		table.repaint();
	}

	public synchronized void experimentInterrupted()
	{
		running = false;
		theThread = null;
		finished = true;
		table.repaint();
	}

	public synchronized void setResult(Values mfValues, Values pfValues, Result res) throws PrismException
	{
		results.setResult(mfValues, pfValues, res.getResult());
	}

	public synchronized void setMultipleErrors(Values mfValues, Values pfValues, Exception e) throws PrismException
	{
		results.setMultipleErrors(mfValues, pfValues, e);
	}

	public void stop()
	{
		if (running && theThread != null) {
			if (useSimulation)
				guiProp.getPrism().getSimulator().stopSampling();
			theThread.interrupt();
		}
	}

	//tidy up ResultsCollection when it has been removed (to tidy up graphs)
	public void clear()
	{
	}

	class ExperimentThread extends GUIComputationThread
	{
		private UndefinedConstants undefinedConstants;
		private ModulesFile modulesFile;
		private PropertiesFile propertiesFile;
		private GUIExperiment exp;

		public ExperimentThread(GUIMultiProperties guiProp, GUIExperiment exp, UndefinedConstants undefinedConstants, ModulesFile modulesFile,
				PropertiesFile propertiesFile)
		{
			super(guiProp);
			this.exp = exp;
			this.undefinedConstants = undefinedConstants;
			this.modulesFile = modulesFile;
			this.propertiesFile = propertiesFile;
		}

		public void run()
		{
			int i, k;
			boolean clear = true;
			Model model = null;

			ModulesFile modulesFileToCheck;
			Expression propertyToCheck = propertiesFile.getProperty(0);
			SimulationInformation info = null;
			boolean reuseInfo = false, reuseInfoAsked = false;

			definedMFConstants = null;
			definedPFConstants = null;
			res = null;

			try {
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						guiProp.startProgress();
						guiProp.setTaskBarText("Running experiment...");
						guiProp.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, guiProp));
						guiProp.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.EXPERIMENT_START));
					}
				});

				for (i = 0; i < undefinedConstants.getNumModelIterations(); i++) {
					definedMFConstants = undefinedConstants.getMFConstantValues();
					if (definedMFConstants != null)
						if (definedMFConstants.getNumValues() > 0)
							logln("\nModel constants: " + definedMFConstants);

					// set values for ModulesFile constants
					try {
						modulesFile.setUndefinedConstants(definedMFConstants);
					} catch (PrismException e) {
						// in case of error, report it (in log only), store as result, and go on to the next model
						errorLog(e.getMessage());
						try {
							setMultipleErrors(definedMFConstants, null, e);
						} catch (PrismException e2) {
							error("Problem storing results");
						}
						undefinedConstants.iterateModel();
						continue;
					}

					// only do explicit model construction if necessary
					if (!useSimulation && modulesFile.getModelType() != ModelType.PTA) {
						
						// build model
						try {
							logln("\n-------------------------------------------");
							model = prism.buildModel(modulesFile);
							clear = false;
						} catch (PrismException e) {
							// in case of error, report it (in log only), store as result, and go on to the next model
							errorLog(e.getMessage());
							try {
								setMultipleErrors(definedMFConstants, null, e);
							} catch (PrismException e2) {
								error("Problem storing results");
							}
							undefinedConstants.iterateModel();
							continue;
						}

						// remove any deadlocks (don't prompt - probably should)
						StateList states = model.getDeadlockStates();
						if (states != null) {
							if (states.size() > 0) {
								guiProp.log("\nWarning: " + states.size() + " deadlock states detected; adding self-loops in these states...\n");
								model.fixDeadlocks();
							}
						}

						// print some model info
						guiProp.log("\n");
						model.printTransInfo(guiProp.getGUI().getLog());
					}

					// collect information for simulation if required
					if (useSimulation && !reuseInfo) {
						try {
							info = null;
							info = GUISimulationPicker.defineSimulationWithDialog(guiProp.getGUI(), propertyToCheck, modulesFile, "("+definedMFConstants+")");
						}
						catch (PrismException e) {
							// in case of error, report it (in log only), store as result, and go on to the next model
							errorLog(e.getMessage());
							try {
								setMultipleErrors(definedMFConstants, null, e);
							} catch (PrismException e2) {
								error("Problem storing results");
							}
							if (!clear)
								model.clear();
							undefinedConstants.iterateModel();
							continue;
						}

						// if info is null, the user clicked cancel
						if (info == null)
							break;

						// if there are multiple models, offer the chance to reuse simulation info
						if (undefinedConstants.getNumModelIterations() > 1 && !reuseInfoAsked) {
							reuseInfoAsked = true;
							int q = guiProp.questionYesNo("Do you want to reuse the same initial state and simulation\n"
									+ "parameters for the remaining models in this experiment?\n" + "If not you will be prompted for new values for each one.");
							if (q == 0)
								reuseInfo = true;
						}
					}

					// for distributed simulation, pass control to the GUISimulatorDistributionDialog
					if (useSimulation && info.isDistributed()) {
						try {
							GUISimulatorDistributionDialog dist = new GUISimulatorDistributionDialog(guiProp.getGUI(), prism.getSimulator(), true);
							dist.show(exp, this, modulesFile, propertiesFile, undefinedConstants, propertyToCheck, info);
							//new GUISimulatorDistributionDialog(guiProp.getGUI(), prism.getSimulator(),  true).show(modulesFile, undefinedConstants, propertyToCheck, info);
						} catch (PrismException e) {
							// in case of error, report it (in log only), store as result, and go on to the next model
							errorLog(e.getMessage());
							try {
								setMultipleErrors(definedMFConstants, null, e);
							} catch (PrismException e2) {
								error("Problem storing results");
							}
							if (!clear)
								model.clear();
							undefinedConstants.iterateModel();
							continue;
						}
					}

					// for simulation where "simultaneous property checking" is enabled...
					else if (useSimulation && prism.getSettings().getBoolean(PrismSettings.SIMULATOR_SIMULTANEOUS)
							&& undefinedConstants.getNumPropertyIterations() > 1) {
						try {
							logln("\n-------------------------------------------");
							logln("\nSimulating: " + propertyToCheck);
							if (definedMFConstants != null)
								if (definedMFConstants.getNumValues() > 0)
									logln("Model constants: " + definedMFConstants);
							logln("Property constants: " + undefinedConstants.getPFDefinedConstantsString());
							prism.modelCheckSimulatorExperiment(modulesFile, propertiesFile, undefinedConstants, results, propertyToCheck, info
									.getInitialState(), info.getMaxPathLength(), info.createSimulationMethod());
							// update progress meter
							// (all properties simulated simultaneously so can't get more accurate feedback at the moment anyway)
							table.progressChanged();
						} catch (PrismException e) {
							// in case of error, report it (in log only), store as result, and go on to the next model
							errorLog(e.getMessage());
							try {
								setMultipleErrors(definedMFConstants, null, e);
							} catch (PrismException e2) {
								error("Problem storing results");
							}
							undefinedConstants.iterateModel();
							continue;
						}
					} else {
						// iterate through as many properties as necessary
						for (k = 0; k < undefinedConstants.getNumPropertyIterations(); k++) {
							// interrupt if requested
							if (interrupted())
								throw new InterruptedException();

							try {
								// set values for PropertiesFile constants
								if (propertiesFile != null) {
									definedPFConstants = undefinedConstants.getPFConstantValues();
									propertiesFile.setUndefinedConstants(definedPFConstants);
								}

								// log output
								logln("\n-------------------------------------------");
								logln("\n" + (useSimulation ? "Simulating" : "Model checking") + ": " + propertyToCheck);
								if (definedMFConstants != null)
									if (definedMFConstants.getNumValues() > 0)
										logln("Model constants: " + definedMFConstants);
								if (definedPFConstants != null)
									if (definedPFConstants.getNumValues() > 0)
										logln("Property constants: " + definedPFConstants);

								// for PTAs via digital clocks, do model translation and build
								if (modulesFile.getModelType() == ModelType.PTA
										&& prism.getSettings().getString(PrismSettings.PRISM_PTA_METHOD).equals("Digital clocks")) {
									DigitalClocks dc = new DigitalClocks(prism);
									dc.translate(modulesFile, propertiesFile, propertyToCheck);
									modulesFileToCheck = dc.getNewModulesFile();
									modulesFileToCheck.setUndefinedConstants(modulesFile.getConstantValues());
									// build model
									logln("\n-------------------------------------------");
									model = prism.buildModel(modulesFileToCheck);
									clear = false;
									// by construction, deadlocks can only occur from timelocks
									StateList states = model.getDeadlockStates();
									if (states != null && states.size() > 0) {
										throw new PrismException("Timelock in PTA, e.g. in state (" + states.getFirstAsValues() + ")");
									}
									// print some model info
									guiProp.log("\n");
									model.printTransInfo(guiProp.getGUI().getLog());
								} else {
									modulesFileToCheck = modulesFile;
								}

								// exact (non-approximate) model checking
								if (!useSimulation) {
									// PTA model checking
									if (modulesFileToCheck.getModelType() == ModelType.PTA) {
										res = prism.modelCheckPTA(modulesFileToCheck, propertiesFile, propertyToCheck);
									}
									// Non-PTA model checking
									else {
										res = prism.modelCheck(model, propertiesFile, propertyToCheck);
									}
								}
								// approximate (simulation-based) model checking
								else {
									res = prism.modelCheckSimulator(modulesFileToCheck, propertiesFile, propertyToCheck, info.getInitialState(), info.getMaxPathLength(), info.createSimulationMethod());
								}
							} catch (PrismException e) {
								// in case of error, report it (in log only), store exception as the result and proceed
								errorLog(e.getMessage());
								res = new Result(e);
							}
							// store result of model checking
							SwingUtilities.invokeAndWait(new Runnable()
							{
								public void run()
								{
									try {
										GUIExperiment.this.setResult(definedMFConstants, definedPFConstants, res);
									} catch (PrismException e) {
										error("Problem storing results");
									}
								}
							});

							table.progressChanged();

							// iterate to next property
							undefinedConstants.iterateProperty();
							yield();
						}
					}
					if (!clear)
						model.clear();
					// iterate to next model
					undefinedConstants.iterateModel();
					yield();
				}

				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						guiProp.stopProgress();
						guiProp.setTaskBarText("Running experiment... done.");
						guiProp.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, guiProp));
						guiProp.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.EXPERIMENT_END));
					}
				});
				experimentDone();

				if (results.containsErrors())
					errorDialog("One or more errors occured during this experiment.\nSelect \"View results\" or check the log for more information");
			} catch (InterruptedException e) {
				try {
					SwingUtilities.invokeAndWait(new Runnable()
					{
						public void run()
						{

							guiProp.stopProgress();
							guiProp.setTaskBarText("Running experiment... interrupted.");
							guiProp.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, guiProp));
							guiProp.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.EXPERIMENT_END));

						}
					});
				}
				// catch and ignore possible exceptions from invokeAndWait call
				catch (InterruptedException e2) {
				} catch (java.lang.reflect.InvocationTargetException e2) {
				}
				if (!clear)
					model.clear();
				experimentInterrupted();
			}
			// catch and ignore possible exception from invokeAndWait calls
			catch (java.lang.reflect.InvocationTargetException e) {
			}
		}
	}
}
