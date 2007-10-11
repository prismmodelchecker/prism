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
 *
 * @author  ug60axh
 */
public class ModelCheckThread extends GUIComputationThread
{
	private GUIMultiProperties parent;
	private Model m;
	private PropertiesFile prFi;
	private ArrayList guiProps;
	private Values definedMFConstants;
	private Values definedPFConstants;
	
	/** Creates a new instance of ModelCheckThread */
	public ModelCheckThread(GUIMultiProperties parent, Model m, PropertiesFile prFi, ArrayList guiProps, Values definedMFConstants, Values definedPFConstants)
	{
		super(parent);
		this.parent = parent;
		this.m = m;
		this.prFi = prFi;
		this.guiProps = guiProps;
		this.definedMFConstants = definedMFConstants;
		this.definedPFConstants = definedPFConstants;
	}
	
	public void run()
	{
		if(m == null) return;
		
		//Notify user interface of the start of computation
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				parent.startProgress();
				parent.setTaskBarText("Verifying properties...");
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
			}
		});
		
		Object result = null;
		
		//Set icon for all properties to be verified to a clock
		for(int i = 0; i < guiProps.size(); i++)
		{
			GUIProperty gp = (GUIProperty)guiProps.get(i);
			gp.setStatus(GUIProperty.STATUS_DOING);
			parent.repaintList();
		}
		
		IconThread ic = new IconThread(null);
		
		for(int i = 0; i < prFi.getNumProperties(); i++)
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
				logln("\nModel checking: " + prFi.getProperty(i));
				if (definedMFConstants != null) if (definedMFConstants.getNumValues() > 0) logln("Model constants: " + definedMFConstants);
				if (definedPFConstants != null) if (definedPFConstants.getNumValues() > 0) logln("Property constants: " + definedPFConstants);
				result = prism.modelCheck(m, prFi, prFi.getProperty(i));
			}
			catch(PrismException e)
			{
				result = e;
				error(e.getMessage());
			}
			ic.interrupt();
			try
			{
				ic.join();
			}
			catch(InterruptedException e)
			{}
			//while(!ic.canContinue){}
			gp.setResult(result);
			gp.setMethodString("Verification");
			gp.setConstants(definedMFConstants, definedPFConstants);
			
			parent.repaintList();
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				parent.stopProgress();
				parent.setTaskBarText("Verifying properties... done.");
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
			try
			{
				int counter = 0;
				while(!interrupted() && gp != null)
				{
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
