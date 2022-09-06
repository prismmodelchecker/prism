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

import parser.ast.PropertiesFile;
import prism.*;
import userinterface.*;
import userinterface.model.*;
import userinterface.util.*;

/**
 * Thread that performs export of a built model.
 */
public class ExportBuiltModelThread extends GUIComputationThread
{
	private int exportEntity;
	private int exportType;
	private File exportFile;
	private PropertiesFile propertiesFile;

	/** Creates a new instance of ExportBuiltModelThread */
	public ExportBuiltModelThread(GUIMultiModelHandler handler, int entity, int type, File f)
	{
		this(handler.getGUIPlugin(), entity, type, f);
	}

	/** Creates a new instance of ExportBuiltModelThread */
	public ExportBuiltModelThread(GUIPlugin plug, int entity, int type, File f)
	{
		super(plug);
		this.exportEntity = entity;
		this.exportType = type;
		this.exportFile = f;
	}

	/** Set (optional) associated PropertiesFile (for label export) */
	public void setPropertiesFile(PropertiesFile propertiesFile)
	{
		this.propertiesFile = propertiesFile;
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
				switch (exportEntity) {
				case GUIMultiModelHandler.STATES_EXPORT:
					prism.exportStatesToFile(exportType, exportFile);
					break;
				case GUIMultiModelHandler.TRANS_EXPORT:
					prism.exportTransToFile(true, exportType, exportFile);
					break;
				case GUIMultiModelHandler.STATE_REWARDS_EXPORT:
					prism.exportStateRewardsToFile(exportType, exportFile);
					break;
				case GUIMultiModelHandler.TRANS_REWARDS_EXPORT:
					prism.exportTransRewardsToFile(true, exportType, exportFile);
					break;
				case GUIMultiModelHandler.LABELS_EXPORT:
					prism.exportLabelsToFile(propertiesFile, exportType, exportFile);
					break;
				}
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
