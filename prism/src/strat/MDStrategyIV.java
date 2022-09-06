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

import java.io.FileNotFoundException;
import java.util.List;

import prism.Model;
import prism.Prism;
import prism.PrismException;
import prism.PrismLog;
import dv.IntegerVector;

/**
 * Class to store a memoryless deterministic (MD) strategy, as an IntegerVector (i.e. stored natively as an array).
 */
public class MDStrategyIV extends MDStrategy
{
	// Model associated with the strategy
	private Model model;
	// Other model info
	private int numStates;
	private List<String> actions;
	// Array storing MD strategy: *action* index (not choice index) for each state
	private IntegerVector iv;
	
	/**
	 * Create an MDStrategyIV from an IntegerVector.
	 */
	public MDStrategyIV(Model model, IntegerVector iv)
	{
		this.model = model;
		numStates = (int) model.getNumStates();
		actions = model.getSynchs();
		this.iv = iv;
	}
	
	// Methods for MDStrategy
	
	@Override
	public int getNumStates()
	{
		return numStates;
	}
	
	@Override
	public boolean isChoiceDefined(int s)
	{
		return iv.getElement(s) >= 0;
	}

	@Override
	public Strategy.Choice getChoice(int s)
	{
		int c = iv.getElement(s);
		switch (c) {
		case -1:
			return Choice.UNKNOWN;
		case -2:
			return Choice.ARBITRARY;
		case -3:
			return Choice.UNREACHABLE;
		default:
			return Choice.INDEX;
		}
	}
	
	@Override
	public int getChoiceIndex(int s)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Object getChoiceAction(int s)
	{
		int c = iv.getElement(s);
		return c >= 0 ? actions.get(c) : c == -1 ? "?" : c == -2 ? "*" : "-";
	}
	
	// Methods for Strategy
	
	@Override
	public void exportInducedModel(PrismLog out)
	{
		// TODO
	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		try {
			model.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("a.dot"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PrismException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void clear()
	{
		iv.clear();
		iv = null;
	}
}
