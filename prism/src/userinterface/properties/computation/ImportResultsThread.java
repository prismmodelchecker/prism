//==============================================================================
//	
//	Copyright (c) 2021-
//	Authors:
//	* Steffen Märcker <steffen.maercker@tu-dresden.de> (TU Dresden)
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
import java.util.Map.Entry;

import javax.swing.*;

import csv.CsvFormatException;
import parser.ast.Property;
import prism.PrismLangException;
import prism.ResultsImporter;
import prism.ResultsImporter.RawResultsCollection;
import userinterface.util.*;

/**
 * Thread that runs the import or experiment data from a file.
 */
public class ImportResultsThread extends Thread
{
	private final GUIMultiProperties parent;
	private final File file;
	private final GUIExperimentTable table;

	/** Creates a new instance of ImportResultsThread */
	public ImportResultsThread(GUIMultiProperties parent, GUIExperimentTable table, File file)
	{
		this.parent = parent;
		this.table = table;
		this.file = file;
	}

	public void run()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
					parent.startProgress();
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, parent));
					parent.setTaskBarText("Importing results...");
			}
		});

		String error = null;
		try {
			FileReader reader = new FileReader(file);
			ResultsImporter importer = new ResultsImporter(new BufferedReader(reader));
			for (Entry<Property, RawResultsCollection> result : importer) {
				table.importExperiment(result.getKey(), result.getValue().toResultsCollection());
			}
		} catch (FileNotFoundException e) {
			error = "Could not export results: " + e.getMessage();
		} catch (IOException e) {
			error = "Could not read results file: " + e.getMessage();
		} catch (CsvFormatException e) {
			error = "Malformatted CSV results file: " + e.getMessage();
		} catch (PrismLangException e) {
			error = "Syntax error in results file: " + e.getMessage();
		}
		if (error != null) {
			String msg = error; // Copy error message since an enclosed variable must be final
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					parent.stopProgress(); 
					parent.setTaskBarText("Importing results... error.");
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, parent));
					parent.error("Could not import results: " + msg);
				}
			});
		}

		//Computation successful, notify the user interface
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
					parent.stopProgress();
					parent.setTaskBarText("Importing results... done.");
					parent.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, parent));
			}
		});
	}
}
