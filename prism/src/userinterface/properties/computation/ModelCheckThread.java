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

package userinterface.properties.computation;

import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import parser.ast.PropertiesFile;
import prism.PrismException;
import prism.Result;
import userinterface.GUIComputationThread;
import userinterface.GUIPrism;
import userinterface.properties.GUIMultiProperties;
import userinterface.properties.GUIPropertiesEvent;
import userinterface.properties.GUIProperty;
import userinterface.util.GUIComputationEvent;

/**
 * Thread that executes model checking of a property via PRISM.
 */
public class ModelCheckThread extends GUIComputationThread
{
	// Access to GUI (to send notifications etc.)
	private GUIMultiProperties parent;
	// Properties file and GUI properties (these are assumed to match)
	// (Also need properties file for access to constants/labels/etc.)
	private PropertiesFile propertiesFile;
	private ArrayList<GUIProperty> guiProps;

	/**
	 * Create a new instance of ModelCheckThread (where a Model has been built)
	 */
	public ModelCheckThread(GUIMultiProperties parent, PropertiesFile propertiesFile, ArrayList<GUIProperty> guiProps)
	{
		super(parent);
		this.parent = parent;
		this.propertiesFile = propertiesFile;
		this.guiProps = guiProps;
	}

	public void run()
	{
		// Notify user interface of the start of computation
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				parent.startProgress();
				parent.setTaskBarText("Verifying properties...");
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
			}
		});

		Result result = null;

		// Set icon for all properties to be verified to a clock
		for (int i = 0; i < guiProps.size(); i++) {
			GUIProperty gp = guiProps.get(i);
			gp.setStatus(GUIProperty.STATUS_DOING);
			parent.repaintList();
		}

		IconThread ic = new IconThread(null);

		//numAuxiliary is the number of properties we don't check but that are contained because
		//they are referenced. These are at the beginning of the file.
		int numAuxiliary = propertiesFile.getNumProperties() - guiProps.size();
		// Work through list of properties
		for (int i = numAuxiliary; i < propertiesFile.getNumProperties(); i++) {

			// Get ith property
			GUIProperty gp = guiProps.get(i - numAuxiliary);
			// Animate it's clock icon
			ic = new IconThread(gp);
			ic.start();

			// Do model checking
			try {
				result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(i));
			} catch (PrismException e) {
				result = new Result(e);
				error(e.getMessage());
			}
			ic.interrupt();
			try {
				ic.join();
			} catch (InterruptedException e) {
			}
			//while(!ic.canContinue){}
			gp.setResult(result);
			gp.setMethodString("Verification");
			gp.setNumberOfWarnings(prism.getMainLog().getNumberOfWarnings());

			parent.repaintList();
		}

		// Notify user interface of the end of computation
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				parent.stopProgress();
				parent.setTaskBarText("Verifying properties... done.");
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
				parent.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.VERIFY_END));
			}
		});
	}

	// Clock animation icon
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
					counter++;
					counter = counter % 8;
					gp.setDoingImage(images[counter]);
					parent.repaintList();
					sleep(150);
				}
			} catch (InterruptedException e) {
			}
			canContinue = true;
		}
	}
}
