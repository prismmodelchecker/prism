//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.SwingUtilities;

import strat.StrategyExportOptions;
import userinterface.GUIComputationThread;
import userinterface.GUIPlugin;
import userinterface.util.GUIComputationEvent;

/**
 * Thread that performs export of a strategy.
 */
public class ExportStrategyThread extends GUIComputationThread
{
	private StrategyExportOptions exportOptions;
	private File exportFile;

	/** Creates a new instance of ExportStrategyThread */
	public ExportStrategyThread(GUIPlugin plug, StrategyExportOptions exportOptions, File exportFile)
	{
		super(plug);
		this.exportOptions = exportOptions;
		this.exportFile = exportFile;
	}

	public void run()
	{
		try {
			// Notify the interface of the start of computation
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					plug.startProgress();
					plug.setTaskBarText("Exporting...");
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
				}
			});

			// Do export
			try {
				prism.exportStrategy(exportOptions, exportFile);
			} catch (FileNotFoundException e) {
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						plug.stopProgress();
						plug.setTaskBarText("Exporting... error.");
						plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
						error("Could not export to file \"" + exportFile + "\"");
					}
				});
				return;
			} catch (Throwable e2) {
				error(e2);
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						plug.stopProgress();
						plug.setTaskBarText("Exporting... error.");
						plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					}
				});
				return;
			}

			//If we get here export was successful, notify interface
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					plug.stopProgress();
					plug.setTaskBarText("Exporting... done.");
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
				}
			});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e) {
		} catch (java.lang.reflect.InvocationTargetException e) {
		}
	}
}
