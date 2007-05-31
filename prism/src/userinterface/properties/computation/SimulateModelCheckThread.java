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

package userinterface.properties.computation;

import java.lang.*;
import userinterface.properties.*;
import parser.*;
import prism.*;
import javax.swing.*;
import java.util.*;
import userinterface.*;
import userinterface.util.*;

/**
 *  This thread handles the calling of simulation-based
 *  model checking (sampling) with the given modules file (constants
 *  defined), properties file (constants defined), list of properties to
 *  be (approximately) verified and initial state.
 */
public class SimulateModelCheckThread extends GUIComputationThread
{
	private GUIMultiProperties parent;
	private ModulesFile mf;
	private PropertiesFile pf;
	private ArrayList guiProps;
	private Values definedMFConstants;
	private Values definedPFConstants;
	private Values initialState;
	private int noIterations;
	private int maxPathLength;
	private SimulationInformation info;
	
	/** Creates a new instance of SimulateModelCheckThread */
	public SimulateModelCheckThread
		(GUIMultiProperties parent,
		ModulesFile m,
		PropertiesFile prFi,
		ArrayList guiProps,
		Values definedMFConstants,
		Values definedPFConstants,
		Values initialState,
		int noIterations,
		int maxPathLength,
		SimulationInformation info)
	{
		super(parent);
		this.parent = parent;
		this.mf = m;
		this.pf = prFi;
		this.guiProps = guiProps;
		this.definedMFConstants = definedMFConstants;
		this.definedPFConstants = definedPFConstants;
		this.initialState = initialState;
		this.noIterations = noIterations;
		this.maxPathLength = maxPathLength;
		this.info = info;
	}
    
	public void run()
	{
		boolean allAtOnce = prism.getSettings().getBoolean(PrismSettings.SIMULATOR_SIMULTANEOUS);
		
		if(mf == null) return;
		
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
		for(int i = 0; i < guiProps.size(); i++)
		{
			GUIProperty gp = (GUIProperty)guiProps.get(i);
			gp.setStatus(GUIProperty.STATUS_DOING);
			parent.repaintList();
		}
		
		IconThread ic = new IconThread(null);
		
		// do all at once if requested
		if(allAtOnce)
		{
			Object results[] = null;
			Exception resultError = null;
			ArrayList properties = new ArrayList();
			ArrayList clkThreads = new ArrayList();
			for(int i = 0; i < guiProps.size(); i++)
			{
				GUIProperty gp = (GUIProperty)guiProps.get(i);
				properties.add(gp.getPCTLProperty());
				ic = new IconThread(gp);
				ic.start();
				clkThreads.add(ic);
			}
			
			try
			{
				// display info
				logln("\n-------------------------------------------");
				log("\nSimulating");
				if (pf.getNumProperties() == 1) {
					logln(": " + properties.get(0));
				} else {
					logln(" " + pf.getNumProperties() + " properties:");
					for (int i = 0; i < pf.getNumProperties(); i++) {
						logln(" " + properties.get(i));
					}
				}
				if (definedMFConstants != null) if (definedMFConstants.getNumValues() > 0) logln("Model constants: " + definedMFConstants);
				if (definedPFConstants != null) if (definedPFConstants.getNumValues() > 0) logln("Property constants: " + definedPFConstants);
				log("Simulation parameters: approx = "+info.getApprox()+", conf = "+info.getConfidence()+", num samples = "+noIterations+", max path len = "+maxPathLength+")\n");
				
				// do simulation
				results = prism.modelCheckSimulatorSimultaneously(mf, pf, properties, initialState, noIterations, maxPathLength);
			}
			catch(PrismException e)
			{
				// in the case of an error which affects all props, store/report it
				resultError = e;
				error(e.getMessage());
			}
			//after collecting the results stop all of the clock icons
			for(int i =0; i < clkThreads.size(); i++)
				{
				IconThread ict = (IconThread)clkThreads.get(i);
				ict.interrupt();
				while(!ict.canContinue)
				{}
			}
			// store results
			for(int i = 0; i < guiProps.size(); i++)
			{
				GUIProperty gp = (GUIProperty)guiProps.get(i);
				gp.setResult((results == null) ? resultError : results[i]);
				gp.setMethodString("Simulation");
				gp.setConstants(definedMFConstants, definedPFConstants);
			}
		}
		// do each property individually
		else
		{
			Object result = null;
			for(int i = 0; i < pf.getNumProperties(); i++)
			{
				// get property
				GUIProperty gp = (GUIProperty)guiProps.get(i);
				// animate it's clock icon
				ic = new IconThread(gp);
				ic.start();
				// do model checking
				try
				{
					logln("\n-------------------------------------------");
					logln("\nSimulating"+": " + pf.getProperty(i));
					if (definedMFConstants != null) if (definedMFConstants.getNumValues() > 0) logln("Model constants: " + definedMFConstants);
					if (definedPFConstants != null) if (definedPFConstants.getNumValues() > 0) logln("Property constants: " + definedPFConstants);
					log("Simulation parameters: approx = "+info.getApprox()+", conf = "+info.getConfidence()+", num samples = "+noIterations+", max path len = "+maxPathLength+")\n");
					result = prism.modelCheckSimulator(mf, pf,  pf.getProperty(i), initialState, noIterations, maxPathLength);
				}
				catch(PrismException e)
				{
					result = e;
					error(e.getMessage());
				}
				ic.interrupt();
				while(!ic.canContinue) {}
				gp.setResult(result);
				gp.setMethodString("Simulation");
				gp.setConstants(definedMFConstants, definedPFConstants);
				
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
			images[0] = GUIPrism.getIconFromImage("smallClockAnim1.gif");
			images[1] = GUIPrism.getIconFromImage("smallClockAnim2.gif");
			images[2] = GUIPrism.getIconFromImage("smallClockAnim3.gif");
			images[3] = GUIPrism.getIconFromImage("smallClockAnim4.gif");
			images[4] = GUIPrism.getIconFromImage("smallClockAnim5.gif");
			images[5] = GUIPrism.getIconFromImage("smallClockAnim6.gif");
			images[6] = GUIPrism.getIconFromImage("smallClockAnim7.gif");
			images[7] = GUIPrism.getIconFromImage("smallClockAnim8.gif");
		}
		public void run()
		{
			try
			{
				int counter = 0;
				while(!interrupted() && gp != null)
				{
					//System.out.println("counter = "+counter);
					counter++;
					counter = counter%8;
					gp.setDoingImage(images[counter]);
					parent.repaintList();
					sleep(150);
				}
			}
			catch(InterruptedException e)
			{
			}
			canContinue = true;
		}
	}
}
