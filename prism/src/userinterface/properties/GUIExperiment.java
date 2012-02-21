//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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
import userinterface.*;

import javax.swing.*;
import java.util.*;
import userinterface.util.*;

public class GUIExperiment
{
	private GUIMultiProperties guiProp;
	private GUIExperimentTable table;
	private prism.ResultsCollection results;
	private boolean finished = false;

	private UndefinedConstants cons;
	private PropertiesFile prop; //contains 1 property only

	private boolean running = false;

	private Thread theThread;

	private boolean useSimulation;

	private Values definedMFConstants;
	private Values definedPFConstants;
	private Result res;

	/** Creates a new instance of GUIExperiment */
	public GUIExperiment(GUIExperimentTable table, GUIMultiProperties guiProp, PropertiesFile prop, UndefinedConstants cons, boolean useSimulation)
	{
		this.table = table;
		this.guiProp = guiProp;
		this.prop = prop;
		this.cons = cons;
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

	public Vector<DefinedConstant> getRangingConstants()
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
		int i = prop.getNumProperties() - 1;
		return prop.getProperty(i).toString();
	}

	public Type getPropertyType()
	{
		int i = prop.getNumProperties() - 1;
		return prop.getProperty(i).getType();
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
		theThread = new ExperimentThread(guiProp, cons, prop);
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

	public synchronized void setResult(Values mfValues, Values pfValues, Result res)
	{
		results.setResult(mfValues, pfValues, res.getResult());
	}

	public synchronized void setMultipleErrors(Values mfValues, Values pfValues, Exception e)
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
		private PropertiesFile propertiesFile;

		public ExperimentThread(GUIMultiProperties guiProp, UndefinedConstants undefinedConstants, PropertiesFile propertiesFile)
		{
			super(guiProp);
			this.undefinedConstants = undefinedConstants;
			this.propertiesFile = propertiesFile;
		}

		public void run()
		{
			int i, k;
			int propertyIndex = propertiesFile.getNumProperties() - 1;
			Property propertyToCheck = propertiesFile.getPropertyObject(propertyIndex);
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

					// set values for ModulesFile constants
					try {
						definedMFConstants = undefinedConstants.getMFConstantValues();
						prism.setPRISMModelConstants(definedMFConstants);
					} catch (PrismException e) {
						// in case of error, report it (in log only), store as result, and go on to the next model
						errorLog(e.getMessage());
						setMultipleErrors(definedMFConstants, null, e);
						undefinedConstants.iterateModel();
						continue;
					}

					// collect information for simulation if required
					if (useSimulation && !reuseInfo) {
						try {
							info = null;
							info = GUISimulationPicker.defineSimulationWithDialog(guiProp.getGUI(), propertyToCheck.getExpression(), prism.getPRISMModel(), "("
									+ definedMFConstants + ")");
						} catch (PrismException e) {
							// in case of error, report it (in log only), store as result, and go on to the next model
							errorLog(e.getMessage());
							setMultipleErrors(definedMFConstants, null, e);
							undefinedConstants.iterateModel();
							continue;
						}

						// if info is null, the user clicked cancel
						if (info == null)
							break;

						// if there are multiple models, offer the chance to reuse simulation info
						if (undefinedConstants.getNumModelIterations() > 1 && !reuseInfoAsked) {
							reuseInfoAsked = true;
							int q = guiProp.questionYesNo("Do you want to reuse the same simulation\n"
									+ "parameters for the remaining models in this experiment?\n" + "If not you will be prompted for new values for each one.");
							if (q == 0)
								reuseInfo = true;
						}
					}

					// for simulation where "simultaneous property checking" is enabled...
					if (useSimulation && prism.getSettings().getBoolean(PrismSettings.SIMULATOR_SIMULTANEOUS)
							&& undefinedConstants.getNumPropertyIterations() > 1) {
						try {
							// convert initial Values -> State
							// (remember: null means use default or pick randomly)
							parser.State initialState;
							if (info.getInitialState() == null) {
								initialState = null;
							} else {
								initialState = new parser.State(info.getInitialState(), prism.getPRISMModel());
							}
							// do simulation
							prism.modelCheckSimulatorExperiment(propertiesFile, undefinedConstants, results, propertyToCheck.getExpression(), initialState,
									info.getMaxPathLength(), info.createSimulationMethod());
							// update progress meter
							// (all properties simulated simultaneously so can't get more accurate feedback at the moment anyway)
							table.progressChanged();
						} catch (PrismException e) {
							// in case of error, report it (in log only), store as result, and go on to the next model
							errorLog(e.getMessage());
							setMultipleErrors(definedMFConstants, null, e);
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
								// Set values for PropertiesFile constants
								if (propertiesFile != null) {
									definedPFConstants = undefinedConstants.getPFConstantValues();
									propertiesFile.setSomeUndefinedConstants(definedPFConstants);
								}
								// Normal model checking
								if (!useSimulation) {
									res = prism.modelCheck(propertiesFile, propertyToCheck);
								}
								// Approximate (simulation-based) model checking
								else {
									// convert initial Values -> State
									// (remember: null means use default or pick randomly)
									parser.State initialState;
									if (info.getInitialState() == null) {
										initialState = null;
									} else {
										initialState = new parser.State(info.getInitialState(), prism.getPRISMModel());
									}
									// do simulation
									res = prism.modelCheckSimulator(propertiesFile, propertyToCheck.getExpression(), definedPFConstants, initialState,
											info.getMaxPathLength(), info.createSimulationMethod());
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
									GUIExperiment.this.setResult(definedMFConstants, definedPFConstants, res);
								}
							});

							table.progressChanged();

							// iterate to next property
							undefinedConstants.iterateProperty();
							yield();
						}
					}
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
				experimentInterrupted();
			}
			// catch and ignore possible exception from invokeAndWait calls
			catch (java.lang.reflect.InvocationTargetException e) {
			}
		}
	}
}
