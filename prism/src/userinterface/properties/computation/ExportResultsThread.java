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

import userinterface.properties.*;
import java.io.*;
import javax.swing.*;
import userinterface.util.*;

public class ExportResultsThread extends Thread
{
	private GUIMultiProperties parent;
	private GUIExperiment exps[];
	private File file;
	private boolean exportMatrix; // export in matrix form?
	private String sep; // string separating items
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exp, File file)
	{
		this(parent, new GUIExperiment[] {exp}, file );
	}
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exps[], File file)
	{
		this(parent, exps, file, false, " ");
	}
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exps[], File file, boolean exportMatrix, String sep)
	{
		this.parent = parent;
		this.exps = exps;
		this.file = file;
		this.exportMatrix = exportMatrix;
		this.sep = sep;
	}
	
	public void run()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
					parent.startProgress();
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
					parent.setTaskBarText("Exporting results...");
			}
		});
		
		try(PrintWriter out = new PrintWriter(new FileWriter(file))) {
			int n = exps.length;
			for (int i = 0; i < n; i++) {
				if (i > 0)
					out.println();
				if (n > 1) {
					if (sep.equals(", "))
						out.print("\"" + exps[i].getPropertyString() + ":\"\n");
					else
						out.print(exps[i].getPropertyString() + ":\n");
				}
				if (!exportMatrix) {
					out.println(exps[i].getResults().toString(false, sep, sep));
				} else {
					out.println(exps[i].getResults().toStringMatrix(sep));
				}
			}
			out.flush();
		}
		catch (Exception saveError) {
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					parent.stopProgress(); 
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, parent));
					parent.setTaskBarText("Exporting results... error.");
					parent.error("Could not export results: " + saveError.getMessage());
				}
			});
		}
		
		//Computation successful, notify the user interface
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
					parent.stopProgress();
					parent.setTaskBarText("Exporting results... done.");
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
			}
		});
	}
}
