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
public class ExportBuiltModelThread extends GUIComputationThread
{
	private GUIMultiModelHandler handler;
	private Model m;
	private int exportEntity;
	private int exportType;
	private File exportFile;
	private Exception e;
	
	/** Creates a new instance of ExportBuiltModelThread */
	public ExportBuiltModelThread(GUIMultiModelHandler handler, Model m, int entity, int type, File f)
	{
		super(handler.getGUIPlugin());
		this.handler = handler;
		this.m = m; 
		this.exportEntity = entity;
		this.exportType = type;
		this.exportFile = f;
	}
	
	public void run()
	{
		try
		{
			//notify the interface of the start of computation
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				plug.startProgress();
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
				plug.setTaskBarText("Exporting...");
			}});
			
			//Do export
			try {
				switch (exportEntity) {
				case GUIMultiModelHandler.STATES_EXPORT:
					prism.exportStatesToFile(m, exportType, exportFile);
					break;
				case GUIMultiModelHandler.TRANS_EXPORT:
					prism.exportTransToFile(m, true, exportType, exportFile);
					break;
				case GUIMultiModelHandler.STATE_REWARDS_EXPORT:
					prism.exportStateRewardsToFile(m, exportType, exportFile);
					break;
				case GUIMultiModelHandler.TRANS_REWARDS_EXPORT:
					prism.exportTransRewardsToFile(m, true, exportType, exportFile);
					break;
				}
			}
			catch(FileNotFoundException e)
			{
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Exporting... error.");
					error("Could not export to file \"" + exportFile + "\"");
				}});
				return;
			}
			catch(PrismException e2)
			{
				this.e = e2;
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Exporting... error.");
					error(e.getMessage());
				}});
				return;
			}
			
			//If we get here export was successful, notify interface
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				plug.stopProgress(); 
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
				plug.setTaskBarText("Exporting... done.");
			}});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {}
		catch (java.lang.reflect.InvocationTargetException e) {}
	}
}
