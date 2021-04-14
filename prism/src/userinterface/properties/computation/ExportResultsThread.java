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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import parser.ast.Property;
import prism.ResultsCollection;
import prism.ResultsExporter.ResultsExportShape;
import userinterface.util.*;

public class ExportResultsThread extends Thread
{
	private GUIMultiProperties parent;
	private GUIExperiment exps[];
	private File file;
	private ResultsExportShape exportShape;

	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exp, File file)
	{
		this(parent, new GUIExperiment[] {exp}, file );
	}
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exps[], File file)
	{
		this(parent, exps, file, ResultsExportShape.LIST_PLAIN);
	}
	
	/** Creates a new instance of ExportResultsThread */
	public ExportResultsThread(GUIMultiProperties parent, GUIExperiment exps[], File file, ResultsExportShape exportShape)
	{
		this.parent = parent;
		this.exps = exps;
		this.file = file;
		this.exportShape = exportShape;
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

		List<Property> properties = Arrays.stream(exps).map(GUIExperiment::getProperty).collect(Collectors.toList());
		List<ResultsCollection> results = Arrays.stream(exps).map(GUIExperiment::getResults).collect(Collectors.toList());

		String error = null;
		try {
			file.createNewFile(); // create file if not already present
			PrintWriter out = new PrintWriter(file);
			exportShape.getExporter().printResults(results, properties, out);
			out.close();
			if (out.checkError()) {
				// PrintWriter hides exceptions in print methods and close()
				error = "Could not export results: unknown IO exception";
			}
		} catch (IOException e) {
			error = "Could not export results: " + e.getMessage();
		}
		if (error != null) {
			final String msg = error; // Copy error message since an enclosed variable must be final
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					parent.stopProgress(); 
					parent.setTaskBarText("Exporting results... error.");
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, parent));
					parent.error("Could not export results: " + msg);
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
