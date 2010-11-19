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
	private File f;
	private Exception saveError;
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exp, File f)
	{
		this.parent = parent;
		this.exps = new GUIExperiment[1];
		this.exps[0] = exp;
		this.f = f;
	}
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exps[], File f)
	{
		this.parent = parent;
		this.exps = exps;
		this.f = f;
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
		
		try {
			int i, n;
			PrintWriter out = new PrintWriter(new FileWriter(f));
			n = exps.length;
			for (i = 0; i < n; i++) {
				if (i > 0) out.print("\n");
				out.print(exps[i].getPropertyString() + ":\n");
				out.print(exps[i].getResults().toString(false, " ", " "));
			}
			out.flush();
			out.close();
		}
		catch (IOException e) {
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
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
					parent.setTaskBarText("Exporting results... done.");
			}
		});
	}
}
