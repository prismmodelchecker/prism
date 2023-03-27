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

package strat;

import explicit.NondetModel;
import prism.Evaluator;

/**
 * Base class for implementations of Strategy associated with an explicit engine model.
 */
public abstract class StrategyExplicit<Value> extends StrategyWithStates<Value>
{
	// Model associated with the strategy
	protected NondetModel<Value> model;

	public StrategyExplicit(NondetModel<Value> model)
	{
		this.model = model;
		if (model.getStatesList() != null) {
			setStateLookUp(state -> model.getStatesList().indexOf(state));
		} else {
			setStateLookUp(state -> -1);
		}
	}
	
	@Override
	public int getNumStates()
	{
		return model.getNumStates();
	}
	
	@Override
	public Evaluator<Value> getEvaluator()
	{
		return model.getEvaluator();
	}
}
