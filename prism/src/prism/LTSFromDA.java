package prism;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import strat.MDStrategy;
import explicit.LTS;
import explicit.Model;
import explicit.ModelExplicit;

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
	public Iterator<Integer> getSuccessorsIterator(int s)
	{
		List<Integer> succs = new ArrayList<Integer>();
		int n = da.getNumEdges(s);
		for (int i = 0; i < n; i++) {
			succs.add(da.getEdgeDest(s, i));
		}
		return succs.iterator();
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		int n = da.getNumEdges(s);
		for (int i = 0; i < n; i++) {
			if (!set.get(da.getEdgeDest(s, i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		int n = da.getNumEdges(s);
		for (int i = 0; i < n; i++) {
			if (set.get(da.getEdgeDest(s, i))) {
				return true;
			}
		}
		return false;
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
	protected void exportTransitionsToDotFile(int i, PrismLog out)
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
	public Iterator<Integer> getSuccessorsIterator(int s, int i)
	{
		List<Integer> succs = new ArrayList<Integer>();
		succs.add(da.getEdgeDest(s, i));
		return succs.iterator();
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
