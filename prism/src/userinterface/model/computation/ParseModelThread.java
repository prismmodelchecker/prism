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

import javax.swing.*;

import userinterface.model.*;
import parser.ast.*;
import prism.*;
import userinterface.util.*;
import userinterface.GUIComputationThread;

public class ParseModelThread extends GUIComputationThread
{
	private GUIMultiModelHandler handler;
	private String parseThis;
	private boolean isPepa;
	private boolean background;
	private ModulesFile mod;
	private String errMsg;
	private PrismException parseError;
	static int counter = 0;
	int id;
	long before;

	/** Creates a new instance of ParseModelThread */
	public ParseModelThread(GUIMultiModelHandler handler, String parseThis, boolean isPepa, boolean background)
	{
		super(handler.getGUIPlugin());
		this.handler = handler;
		this.parseThis = parseThis;
		this.isPepa = isPepa;
		this.background = background;
		id = counter;
		counter++;
	}

	public void run()
	{
		// Notify user interface of the start of computation
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (!background)
					plug.startProgress();
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
				if (!background)
					plug.setTaskBarText("Parsing model...");
			}
		});

		// do parsing
		try {
			// normal prism mode
			if (!isPepa) {
				if (!background)
					plug.log("\nParsing model...\n");
				mod = prism.parseModelString(parseThis);
			}
			// pepa mode
			else {
				if (!background)
					plug.log("\nParsing PEPA model...\n");
				mod = prism.importPepaString(parseThis);
			}
			// Load into PRISM once done
			prism.loadPRISMModel(mod);
		} catch (PrismException err) {
			parseError = err;
			errMsg = err.getMessage();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (!background)
						plug.stopProgress();
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					if (!background)
						plug.setTaskBarText("Parsing model... error.");
					if (!background)
						error(errMsg);
					handler.modelParseFailed(parseError, background);
				}
			});
			return;
		}

		// If we get here, the parse has been successful, notify the interface and tell the handler.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (!background)
					plug.stopProgress();
				if (!background)
					plug.setTaskBarText("Parsing model... done.");
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
				handler.modelParsedSuccessful(mod);
			}
		});
	}
}
