//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package userinterface.simulator;

import parser.ast.ModulesFile;
import prism.PrismException;
import simulator.GenerateSimulationPath;
import simulator.SimulatorEngine;
import userinterface.GUIComputationThread;
import userinterface.graph.Graph;

public class SimPathPlotThread extends GUIComputationThread
{
	private SimulatorEngine engine;
	private ModulesFile modulesFile;
	private parser.State initialState;
	private String simPathDetails;
	private long maxPathLength;
	private Graph graphModel;

	public SimPathPlotThread(GUISimulator guiSim, SimulatorEngine engine, ModulesFile modulesFile, parser.State initialState, String simPathDetails,
			long maxPathLength, Graph graphModel)
	{
		super(guiSim);
		this.engine = engine;
		this.modulesFile = modulesFile;
		this.initialState = initialState;
		this.simPathDetails = simPathDetails;
		this.maxPathLength = maxPathLength;
		this.graphModel = graphModel;
	}

	public void run()
	{
		try {
			GenerateSimulationPath genPath = new GenerateSimulationPath(engine, prism.getMainLog());
			genPath.generateAndPlotSimulationPath(modulesFile, initialState, simPathDetails, maxPathLength, graphModel);
			if (genPath.getNumWarnings() > 0) {
				for (String msg : genPath.getWarnings()) {
					plug.warning(msg);
				}
			}
		} catch (PrismException e) {
			error(e.getMessage());
		}
	}
}
