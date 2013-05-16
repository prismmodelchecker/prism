//==============================================================================
//	
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator;

import parser.State;
import parser.ast.ModulesFile;
import prism.PrismException;
import explicit.ModelExplorer;

public class PrismModelExplorer implements ModelExplorer
{
	private SimulatorEngine simEngine;
	private ModulesFile modulesFile;

	public PrismModelExplorer(SimulatorEngine simEngine, ModulesFile modulesFile) throws PrismException
	{
		this.simEngine = simEngine;
		this.modulesFile = modulesFile;
		simEngine.createNewOnTheFlyPath(modulesFile);

	}

	@Override
	public State getDefaultInitialState() throws PrismException
	{
		return modulesFile.getDefaultInitialState();
	}
	
	@Override
	public void queryState(State state) throws PrismException
	{
		simEngine.initialisePath(state);
	}

	@Override
	public void queryState(State state, double time) throws PrismException
	{
		queryState(state);
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		return simEngine.getNumChoices();
	}
	
	@Override
	public int getNumTransitions() throws PrismException
	{
		return simEngine.getNumTransitions();
	}
	
	@Override
	public int getNumTransitions(int i) throws PrismException
	{
		return simEngine.getNumTransitions(i);
	}
	
	@Override
	public String getTransitionAction(int i, int offset) throws PrismException
	{
		return simEngine.getTransitionAction(i, offset);
	}
	
	@Override
	public String getTransitionAction(int i) throws PrismException
	{
		return simEngine.getTransitionAction(i);
	}
	
	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException
	{
		return simEngine.getTransitionProbability(i, offset);
	}
	
	@Override
	public double getTransitionProbability(int i) throws PrismException
	{
		return simEngine.getTransitionProbability(i);
	}
	
	/*
	@Override
	public double getTransitionProbabilitySum() throws PrismException
	{
		return simEngine.getTransitionProbabilitySum();
	}
	*/
	
	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException
	{
		return simEngine.computeTransitionTarget(i, offset);
	}

	@Override
	public State computeTransitionTarget(int i) throws PrismException
	{
		return simEngine.computeTransitionTarget(i);
	}
}
