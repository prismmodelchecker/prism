//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package userinterface.model.computation;
import java.lang.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import userinterface.*;
import userinterface.model.*;
import parser.*;
import prism.*;
import userinterface.util.*;

/**
 *
 * @author  ug60axh
 */
public class BuildModelThread extends GUIComputationThread
{
	private GUIMultiModelHandler handler;
	private String buildThis;
	private boolean isPepa;
	private ModulesFile mod;
	private Model model;
	private Values buildValues;
	private boolean forceShow;
	private String errMsg;
	private StateList deadlocks;

	/** Creates a new instance of BuildModelThread */
// 	public BuildModelThread(GUIMultiModelHandler handler, String buildThis, boolean isPepa, Values buildValues, boolean forceShow)
// 	{
// 		this.handler = handler;
// 		this.buildThis = buildThis;
// 		this.isPepa = isPepa;
// 		this.buildValues = buildValues;
// 		plug = handler.getGUIPlugin();
// 		this.forceShow = forceShow;
// 	}
	
	public BuildModelThread(GUIMultiModelHandler handler, ModulesFile buildParse, Values buildValues, boolean forceShow)
	{
		super(handler.getGUIPlugin());
		this.handler = handler;
		this.buildThis = null;
		this.mod = buildParse;
		this.buildValues = buildValues;
		this.forceShow = forceShow;
	}

	public void run()
	{
		//Notify user interface of the start of computation
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			plug.startProgress();
			plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
		}});
		
		//If need to do parse first...
		if(buildThis!=null)
		{
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				plug.setTaskBarText("Parsing model...");
			}});
			
			// do parsing
			try {
				// normal prism mode
				if(!isPepa) {
					mod = prism.parseModelString(buildThis);
				}
				// pepa mode
				else {
					mod = prism.importPepaString(buildThis);
				}
			}
			catch (ParseException e) {
				errMsg = "Could not parse model:\n" + e.getShortMessage();
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Parsing model... error.");
					error(errMsg);
					handler.modelParseFailed(errMsg);
					handler.modelBuildFailed();
				}});
				return;
			}
			catch (PrismException e) {
				errMsg = e.getMessage();
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Parsing model... error.");
					error(errMsg);
					handler.modelParseFailed(errMsg);
					handler.modelBuildFailed();
				}});
				return;
			}
			
			//If we are here, the parse was successful, notify the interface
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				plug.setTaskBarText("Parsing model... done.");
				handler.modelParsedSuccessful(mod);
			}});
			
			//give it a break so user can see the ...done before the building model...
			try { sleep(100); } catch(InterruptedException e) {}
		}
		
		// Check whether there are any undefined constants...
		// ...and if there are, get values for them
		UndefinedConstants unC = new UndefinedConstants(mod, null);
		if(unC.getMFNumUndefined() > 0)
		{
			if(forceShow || (buildValues == null))
			{
				// notify interface
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					plug.setTaskBarText("Defining constants...");
				}});
				
				// show dialog
				int result = GUIConstantsPicker.defineConstantsWithDialog(handler.getGUIPlugin().getGUI(), unC, buildValues, null);
				
				// if build cancelled...
				if(result != GUIConstantsPicker.VALUES_DONE) {
					SwingUtilities.invokeLater(new Runnable() { public void run() {
						plug.stopProgress(); 
						plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
						plug.setTaskBarText("Building model... cancelled.");
						handler.modelBuildFailed();
					}});
					return;
				}
				
				// get build values
				buildValues = unC.getMFConstantValues();
			}
			
			// notify interface
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				plug.setTaskBarText("Defining constants... done.");
			}});
		}
		else {
			// create an empty values object
			buildValues = new Values();
		}
		
		if (buildValues != null) if (buildValues.getNumValues() > 0) logln("\nModel constants: " + buildValues);
		
		// set undefined constants
		try {
			mod.setUndefinedConstants(buildValues);
		}
		catch (PrismException e) {
			errMsg = e.getMessage();
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				plug.stopProgress(); 
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
				plug.setTaskBarText("Building model... error.");
				error(errMsg);
				handler.modelBuildFailed();
			}});
		}
		
		// notify interface
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			plug.setTaskBarText("Building model...");
		}});
		
		// do build
		try {
			model = prism.buildModel(mod);
		}
		catch (PrismException e) {
			errMsg = e.getMessage();
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				plug.stopProgress(); 
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
				plug.setTaskBarText("Building model... error.");
				error(errMsg);
				handler.modelBuildFailed();
			}});
			return;
		}
		
		// Check for Deadlocks
		deadlocks = model.getDeadlockStates();
		if (deadlocks != null) if (deadlocks.size() > 0) {
			String[] options = {"Yes", "No"};
			int choice = JOptionPane.YES_OPTION;
			choice = plug.optionPane("Error: Model contains deadlock states.\nAdd self-loops to these states and continue?", "Error", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, options, options[0]);
			if (choice == JOptionPane.YES_OPTION) {
				log("\nWarning: " + deadlocks.size() + " deadlock states detected; adding self-loops in these states...\n");
				model.fixDeadlocks();
			}
			else {
				model.printTransInfo(plug.getGUI().getLog());
				log("\nError: Model contains " + deadlocks.size() + " deadlock states");
				if (deadlocks.size() > 10) {
					log(".\nThe first 10 deadlock states are displayed below.\n");
					deadlocks.print(plug.getGUI().getLog(), 10);
				} else {
					log(":\n");
					deadlocks.print(plug.getGUI().getLog());
				}
				model.clear();
				
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Building model... error.");
					handler.modelBuildFailed();
				}});
				return;
			}
		}

		logln();
		model.printTransInfo(plug.getGUI().getLog());

		//If we are here, the build was successful, notify the interface
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			plug.stopProgress(); 
			plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
			plug.setTaskBarText("Building model... done.");
			handler.modelBuildSuccessful(model, buildValues);
		}});
	}
}
