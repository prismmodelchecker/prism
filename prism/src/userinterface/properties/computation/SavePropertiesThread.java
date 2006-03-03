//==============================================================================
//	
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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
import userinterface.properties.*;
import userinterface.model.*;
import userinterface.*;
import userinterface.util.*;
import prism.*;
import parser.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
/**
 *
 * @author  ug60axh
 */
public class SavePropertiesThread extends Thread
{
	private GUIMultiProperties parent;
	private GUIPropertiesList propList;
	private GUIPropConstantList consList;
	private GUIPropLabelList labList;
	private File f;
	private Exception saveError;
	
	/** Creates a new instance of SavePropertiesThread */
	public SavePropertiesThread(GUIMultiProperties parent, GUIPropertiesList propList, GUIPropConstantList consList, GUIPropLabelList labList, File f)
	{
		this.parent = parent;
		this.propList = propList;
		this.consList = consList;
		this.labList = labList;
		this.f = f;
	}
	
	public void run()
	{
		try
		{
			//notify interface of start of computation
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				parent.startProgress();
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
				parent.setTaskBarText("Saving properties...");
			}});
			
			// do output to file
			try {
				PrintWriter out = new PrintWriter(new FileWriter(f));
				out.print(propList.toFileString(f, consList, labList));
				out.flush();
				out.close();
			}
			//If there was a problem with the save, notify the interface.
			catch (IOException e) {
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					parent.stopProgress(); 
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, parent));
					parent.setTaskBarText("Saving properties... error.");
					parent.error("Could not save to file \"" + f + "\"");
				}});
				return;
			}
			
			//Computation successful, notify the user interface
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				parent.stopProgress(); 
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
				parent.setTaskBarText("Saving properties... done.");
				parent.propertySaveSuccessful(f);
			}});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {}
		catch (java.lang.reflect.InvocationTargetException e) {}
	}
}
