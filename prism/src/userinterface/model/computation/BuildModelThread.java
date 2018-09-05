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

import userinterface.*;
import userinterface.model.*;
import prism.*;
import userinterface.util.*;

/**
 * Thread that performs model construction, whether needed or not.
 */
public class BuildModelThread extends GUIComputationThread
{
	@SuppressWarnings("unused")
	private GUIMultiModelHandler handler;

	/** Creates a new instance of BuildModelThread */
	public BuildModelThread(GUIMultiModelHandler handler)
	{
		super(handler.getGUIPlugin());
		this.handler = handler;
	}

	public void run()
	{
		// Notify user interface of the start of computation
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				plug.startProgress();
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
				plug.setTaskBarText("Building model...");
			}
		});

		// Do build
		try {
			prism.buildModel();
		} catch (Throwable e) {
			error(e);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					plug.stopProgress();
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
					plug.setTaskBarText("Building model... error.");
				}
			});
			return;
		}

		// If we are here, the build was successful, notify the interface
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				plug.stopProgress();
				plug.setTaskBarText("Building model... done.");
				plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
			}
		});
	}
}
