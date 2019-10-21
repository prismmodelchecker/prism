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

import java.io.*;
import javax.swing.*;

import userinterface.properties.*;
import userinterface.util.*;
import prism.*;
import parser.ast.*;

public class LoadPropertiesThread extends Thread
{
	private GUIMultiProperties parent;
	private Prism pri;
	private File file;
	private PropertiesFile props = null;
	private boolean isInsert = false;
	private Exception ex;
	
	/** Creates a new instance of LoadPropertiesThread */
	public LoadPropertiesThread(GUIMultiProperties parent, File file)
	{
	   this(parent, file, false);
	}
	
	public LoadPropertiesThread(GUIMultiProperties parent, File file, boolean isInsert)
	{
		this.parent = parent;
		this.file = file;
		this.pri = parent.getPrism();
		this.isInsert = isInsert; 
	}
	
	public void run()
	{
		try
		{
			//notify interface of start of computation
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				parent.startProgress();
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
				parent.setTaskBarText("Loading properties...");
			}});
			
			// do parsing
			try {
				props = pri.parsePropertiesFile(file, false);
			}
			//If there was a problem with the loading, notify the interface.
			catch (FileNotFoundException e) {
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					parent.stopProgress(); 
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, parent));
					parent.setTaskBarText("Loading properties... error.");
					parent.error("Could not open file \"" + file + "\"");
				}});
				return;
			}
			catch (PrismException e) {
				ex = e;
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					parent.stopProgress(); 
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, parent));
					parent.setTaskBarText("Loading properties... error.");
					parent.error(ex.getMessage());
				}});
				return;
			}
			
			//If we get here, the load has been successful, notify the interface and tell the handler.
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				parent.stopProgress(); 
				parent.setTaskBarText("Loading properties... done.");
				parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
				if(isInsert)
					parent.propertyInsertSuccessful(props);
				else
					parent.propertyLoadSuccessful(props, file);
			}});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {}
		catch (java.lang.reflect.InvocationTargetException e) {}
	}
}
