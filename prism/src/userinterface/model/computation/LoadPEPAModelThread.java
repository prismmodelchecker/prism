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

package userinterface.model.computation;

import java.io.*;
import javax.swing.*;
import userinterface.*;
import userinterface.model.*;
import userinterface.model.pepaModel.*;
import userinterface.util.*;

public class LoadPEPAModelThread extends Thread
{
	private GUIMultiModelHandler handler;
	private GUIModelEditor edit;
	private File f;
	private boolean reload;
	private GUIPlugin plug;
	private GUIPepaModelEditor pepaEdit;
	private boolean replace;
	private Exception ex;
	
	/** Creates a new instance of LoadPEPAModelThread */
	public LoadPEPAModelThread(GUIMultiModelHandler handler, GUIModelEditor edit, File f, boolean reload)
	{
		this.handler = handler;
		this.edit = edit;
		this.f = f;
		this.reload = reload;
		plug = handler.getGUIPlugin(); //to communicate with rest of gui
		replace = false;
		ex = null;
	}
	
	public void run()
	{
		if(edit instanceof GUIPepaModelEditor)
		{
			pepaEdit = (GUIPepaModelEditor)edit;
			replace = false;
		}
		else
		{
			pepaEdit = new GUIPepaModelEditor(handler);
			replace = true;
		}
		
		try
		{
			//notify interface of start of computation and start the read into pepaEdit
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				try
				{
					plug.startProgress();
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
					plug.setTaskBarText("Loading model...");
					pepaEdit.read(new FileReader(f), f);
				}
				catch(IOException e)
				{
					ex = e;
				}
			}});
			
			//If there was a problem with the loading, notify the interface.
			if(ex != null) {
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Loading model... error.");
					plug.error("Could not open file \"" + f + "\"");
				}});
				return;
			}
			
			//If we get here, the load has been successful, notify the interface and tell the handler.
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				plug.stopProgress(); 
				plug.setTaskBarText("Loading model... done.");
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
				if(!reload)
					handler.pepaModelLoaded(pepaEdit, f ,replace);
				else
					handler.pepaModelReLoaded(f);
			}});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {}
		catch (java.lang.reflect.InvocationTargetException e) {}
	}
}
