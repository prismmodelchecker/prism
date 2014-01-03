//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
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

package userinterface.properties.computation;

import java.util.*;
import javax.swing.*;

import parser.*;
import parser.ast.*;
import prism.*;
import simulator.method.SimulationMethod;
import userinterface.*;
import userinterface.util.*;
import userinterface.properties.*;

/**
 * Thread that executes approximate (simulation-based) model checking of a property via PRISM.
 * Model should have been loaded into PRISM already. Supplied are:
 * properties file (constants defined), list of properties to
 * be (approximately) verified and initial state (default/random if null).
 */
public class SimulateModelCheckThread extends GUIComputationThread
{
	private GUIMultiProperties parent;
	private PropertiesFile pf;
	private ArrayList<GUIProperty> guiProps;
	private Values definedPFConstants;
	private long maxPathLength;
	private SimulationInformation info;

	/** Creates a new instance of SimulateModelCheckThread */
	public SimulateModelCheckThread(GUIMultiProperties parent, PropertiesFile prFi, ArrayList<GUIProperty> guiProps,
			Values definedPFConstants, SimulationInformation info)
	{
		super(parent);
		this.parent = parent;
		this.pf = prFi;
		this.guiProps = guiProps;
		this.definedPFConstants = definedPFConstants;
		this.info = info;
		this.maxPathLength = info.getMaxPathLength();
	}

	public void run()
	{
		boolean allAtOnce = prism.getSettings().getBoolean(PrismSettings.SIMULATOR_SIMULTANEOUS);

		SimulationMethod method = info.createSimulationMethod();

		//Notify user interface of the start of computation
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				parent.startProgress();
				parent.setTaskBarText("Checking properties using simulation...");
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
			}
		});

		//Set icon for all properties to be verified to a clock
		for (int i = 0; i < guiProps.size(); i++) {
			GUIProperty gp = guiProps.get(i);
			gp.setStatus(GUIProperty.STATUS_DOING);
			parent.repaintList();
		}

		// do all at once if requested
		if (allAtOnce) {
			Result results[] = null;
			Exception resultError = null;
			ArrayList<Expression> properties = new ArrayList<Expression>();
			ArrayList<IconThread> clkThreads = new ArrayList<IconThread>();
			for (int i = 0; i < guiProps.size(); i++) {
				GUIProperty gp = guiProps.get(i);
				properties.add(gp.getProperty());
				IconThread ict = new IconThread(gp);
				ict.start();
				clkThreads.add(ict);
			}

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
				results = prism.modelCheckSimulatorSimultaneously(pf, properties, definedPFConstants, initialState, maxPathLength, method);
				method.reset();
			} catch (PrismException e) {
				// in the case of an error which affects all props, store/report it
				resultError = e;
				error(e.getMessage());
			}
			//after collecting the results stop all of the clock icons
			for (int i = 0; i < clkThreads.size(); i++) {
				IconThread ict = (IconThread) clkThreads.get(i);
				ict.interrupt();
				while (!ict.canContinue) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
			// store results
			for (int i = 0; i < guiProps.size(); i++) {
				GUIProperty gp = guiProps.get(i);
				gp.setResult((results == null) ? new Result(resultError) : results[i]);
				gp.setMethodString("Simulation");
				gp.setNumberOfWarnings(prism.getMainLog().getNumberOfWarnings());
			}
		}
		// do each property individually
		else {
			Result result = null;
			for (int i = 0; i < pf.getNumProperties(); i++) {
				// get property
				GUIProperty gp = guiProps.get(i);
				// animate it's clock icon
				IconThread ict = new IconThread(gp);
				ict.start();
				// do model checking
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
					result = prism.modelCheckSimulator(pf, pf.getProperty(i), definedPFConstants, initialState, maxPathLength, method);
					method.reset();
				} catch (PrismException e) {
					result = new Result(e);
					error(e.getMessage());
				}
				ict.interrupt();
				while (!ict.canContinue) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						return;
					}
				}
				gp.setResult(result);
				gp.setMethodString("Simulation");

				parent.repaintList();
			}
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				parent.stopProgress();
				parent.setTaskBarText("Checking properties using simulation... done.");
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
				parent.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.VERIFY_END));
			}
		});
	}

	class IconThread extends Thread
	{
		GUIProperty gp;
		ImageIcon[] images;
		boolean canContinue = false;

		public IconThread(GUIProperty gp)
		{
			this.gp = gp;
			images = new ImageIcon[8];
			images[0] = GUIPrism.getIconFromImage("smallClockAnim1.png");
			images[1] = GUIPrism.getIconFromImage("smallClockAnim2.png");
			images[2] = GUIPrism.getIconFromImage("smallClockAnim3.png");
			images[3] = GUIPrism.getIconFromImage("smallClockAnim4.png");
			images[4] = GUIPrism.getIconFromImage("smallClockAnim5.png");
			images[5] = GUIPrism.getIconFromImage("smallClockAnim6.png");
			images[6] = GUIPrism.getIconFromImage("smallClockAnim7.png");
			images[7] = GUIPrism.getIconFromImage("smallClockAnim8.png");
		}

		public void run()
		{
			try {
				int counter = 0;
				while (!interrupted() && gp != null) {
					//System.out.println("counter = " + counter);
					counter++;
					counter = counter % 8;
					gp.setDoingImage(images[counter]);
					parent.repaintList();
					sleep(150);
				}
			} catch (InterruptedException e) {
			} finally {
				canContinue = true;
			}
		}
	}
}
