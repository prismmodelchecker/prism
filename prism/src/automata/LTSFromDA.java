//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
// * Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package automata;

import java.util.BitSet;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;
import explicit.LTS;
import explicit.Model;
import explicit.ModelExplicit;
import explicit.SuccessorsIterator;

/**
 * Class giving access to the labelled transition system (LTS) underlying a deterministic automaton (DA).
 * This is not particularly efficiently; we assume the DA will probably be relatively small.
 */
public class LTSFromDA extends ModelExplicit implements LTS
{
	/** Underlying DA */
	private DA<?, ?> da;

	public LTSFromDA(DA<?, ?> da)
	{
		this.numStates = da.size();
		this.da = da;
	}

	// Methods to implement Model

	@Override
	public SuccessorsIterator getSuccessors(int s)
	{
		return new SuccessorsIterator() {
			private int n = da.getNumEdges(s);
			private int i = 0;

			@Override
			public boolean successorsAreDistinct()
			{
				return false;
			}

			@Override
			public boolean hasNext()
			{
				return i < n;
			}

			@Override
			public int nextInt()
			{
				return da.getEdgeDest(s, i++);
			}
		};
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public ModelType getModelType()
	{
		return ModelType.LTS;
	}

	@Override
	public int getNumTransitions()
	{
		int size = da.size();
		int num = 0;
		for (int s = 0; s < size; s++) {
			num += da.getNumEdges(s);
		}
		return num;
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		int n = da.getNumEdges(s1);
		for (int i = 0; i < n; i++) {
			if (da.getEdgeDest(s1, i) == s2) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out)
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		throw new RuntimeException("Not implemented yet");
	}

	// Methods to implement NondetModel

	@Override
	public int getNumChoices(int s)
	{
		return da.getNumEdges(s);
	}

	@Override
	public int getMaxNumChoices()
	{
		int size = da.size();
		int max = 0;
		for (int s = 0; s < size; s++) {
			max = Math.max(max, da.getNumEdges(s));
		}
		return max;
	}

	@Override
	public int getNumChoices()
	{
		return getNumTransitions();
	}

	@Override
	public Object getAction(int s, int i)
	{
		return null;
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return false;
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		return 1;
	}

	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		return set.get(da.getEdgeDest(s, i));
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		return set.get(da.getEdgeDest(s, i));
	}

	@Override
	public SuccessorsIterator getSuccessors(int s, int i)
	{
		return SuccessorsIterator.fromSingleton(da.getEdgeDest(s, i));
	}

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat)
	{
		throw new RuntimeException("Not implemented yet");
	}
}
