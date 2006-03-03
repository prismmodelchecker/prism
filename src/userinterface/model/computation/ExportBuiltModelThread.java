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
	private File f;
	private int exportType;
	
	/** Creates a new instance of ExportBuiltModelThread */
	public ExportBuiltModelThread(GUIMultiModelHandler handler, Model m, File f, int exportType)
	{
		super(handler.getGUIPlugin());
		this.handler = handler;
		this.m = m; 
		this.f = f;
		this.exportType = exportType;
	}
	
	public void run()
	{
		try
		{
			//notify the interface of the start of computation
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				plug.startProgress();
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
				plug.setTaskBarText("Exporting model...");
			}});
			
			//Do export
			try {
				if(exportType == GUIMultiModelHandler.PLAIN_EXPORT)
				{
					prism.exportToFile(m, false, Prism.EXPORT_PLAIN, f);
				}
				else if(exportType == GUIMultiModelHandler.PLAIN_ORD_EXPORT)
				{
					prism.exportToFile(m, true, Prism.EXPORT_PLAIN, f);
				}
				else if(exportType == GUIMultiModelHandler.MATLAB_EXPORT)
				{
					prism.exportToFile(m, false, Prism.EXPORT_MATLAB, f);
				}
				else if(exportType == GUIMultiModelHandler.MATLAB_ORD_EXPORT)
				{
					prism.exportToFile(m, true, Prism.EXPORT_MATLAB, f);
				}
				else if(exportType == GUIMultiModelHandler.DOT_EXPORT)
				{
					prism.exportToDotFile(m, f);
				}
				else if (exportType == GUIMultiModelHandler.STATES_EXPORT) 
				{
					logln("\nExporting list of reachable states to file \"" + f.getPath() + "\"...");
					PrismFileLog tmpLog = new PrismFileLog(f.getPath());
					if (!tmpLog.ready()) throw new FileNotFoundException();
					tmpLog.print("(");
					for (int i = 0; i < m.getNumVars(); i++) {
						tmpLog.print(m.getVarName(i));
						if (i < m.getNumVars()-1) tmpLog.print(",");
					}
					tmpLog.println(")");
					m.getReachableStates().print(tmpLog);
					tmpLog.close();
				}
			}
			catch(FileNotFoundException e)
			{
				SwingUtilities.invokeAndWait(new Runnable() { public void run() {
					plug.stopProgress(); 
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Exporting model... error.");
					error("Could not export to file \"" + f + "\"");
				}});
				return;
			}
			
			//If we get here export was successful, notify interface
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				plug.stopProgress(); 
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
				plug.setTaskBarText("Exporting model... done.");
			}});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {}
		catch (java.lang.reflect.InvocationTargetException e) {}
	}
}
