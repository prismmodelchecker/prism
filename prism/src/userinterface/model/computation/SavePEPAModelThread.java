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

public class SavePEPAModelThread extends Thread
{
	private GUIModelEditor editor;
	private GUIMultiModelHandler handler;
	private File f;
	private GUIPlugin plug;
	private Exception ex;
	
	/** Creates a new instance of SavePEPAModelThread */
	public SavePEPAModelThread(File f, GUIMultiModelHandler handler, GUIModelEditor editor)
	{
		this.editor = editor;
		this.handler = handler;
		this.f = f;
		plug = handler.getGUIPlugin();	
		ex = null;
	}
	
	public void run()
	{
		try
		{
			//notify the interface of the start of computation and save the content of editor to f
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				try
				{
					plug.startProgress();
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
					plug.setTaskBarText("Saving model...");
					((GUIPepaModelEditor)editor).write(new FileWriter(f));
				}
				catch(IOException e)
				{
					ex = e;
				}
				catch(ClassCastException e)
				{
					ex = e;
				}
			}});
			
			//If there was a problem with the save, notify the interface.
			if(ex != null) {
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Saving model... error.");
					plug.error("Could not save to file \"" + f + "\"");
				}});
				return;
			}
			
			//If we get here, the save has been successful, notify the interface and tell the handler.
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				plug.stopProgress(); 
				plug.setTaskBarText("Saving model... done.");
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
				handler.pepaFileWasSaved(f);
			}});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {}
		catch (java.lang.reflect.InvocationTargetException e) {}
	}
}
