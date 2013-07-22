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

package strat;

import java.util.List;

import dv.IntegerVector;

import prism.Model;

/**
 * Class to store a memoryless deterministic (MD) strategy, as a IntegerVector (i.e. stored natively as an array).
 */
public class MDStrategyIV extends MDStrategy
{
	// Model associated with the strategy
	private Model model;
	
	private IntegerVector iv;
	
	private List<String> actions;
	
	private int numStates;
	private long ptr;
	
	/**
	 * Creates...
	 */
	public MDStrategyIV(Model model)
	{
		this.model = model;
		numStates = (int) model.getNumStates();
		actions = model.getSynchs();
	}
	
	/**
	 * Creates...
	 */
	public MDStrategyIV(Model model, IntegerVector iv)
	{
		this.model = model;
		numStates = (int) model.getNumStates();
		actions = model.getSynchs();
		this.iv = iv;
	}
	
	public void setPointer(long ptr)
	{
		this.ptr = ptr;
	}
	
	@Override
	public int getNumStates()
	{
		return numStates;
	}
	
	@Override
	public int getChoice(int s)
	{
		return 99;
	}
	
	@Override
	public Object getChoiceAction(int s)
	{
		int c = iv.getElement(s);
		//return ""+c; //c >= 0 ? actions.get(c) : "?";
		return c >= 0 ? actions.get(c) : c == -1 ? "?" : c == -2 ? "*" : "-";
	}
	
	public void clear()
	{
		iv.clear();
		iv = null;
	}
}
