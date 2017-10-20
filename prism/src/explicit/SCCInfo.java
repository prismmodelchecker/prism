//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit;

import java.util.Arrays;
import java.util.PrimitiveIterator.OfInt;

import common.IntSet;
import prism.PrismLog;

/**
 * Storage of the SCC information, with a topological order of the SCCs.
 * <br>
 * Provides IntSet-based access to the states in the SCCs.
 */
public class SCCInfo implements SCCConsumer
{
	/** The number of states in the model */
	private int numStates;
	/** List of state indizes, in order of SCC membership */
	private int[] stateList;
	/** Index into stateList: sccEnd[i] points to the last state in the SCC */
	private int[] sccEnd;
	/** Mapping from state index to the SCC index */
	private int[] stateToSCCIndex;
	/** Number of SCCs that are stored */
	private int storedSCCCount;
	/** Number of states that are stored */
	private int storedStateCount;

	/**
	 * Constructor.
	 *
	 * @param numStates number of states in the model (used for allocating data structures)
	 */
	public SCCInfo(int numStates) {
		this.numStates = numStates;
		stateList = new int[numStates];
		sccEnd = new int[numStates];
		stateToSCCIndex = new int[numStates];
		Arrays.fill(stateToSCCIndex, -1);
		storedSCCCount = 0;
		storedStateCount = 0;
	}

	/** SCCConsumer interface: Notification about start of SCC */
	@Override
	public void notifyStartSCC()
	{
	}

	/** SCCConsumer interface: Notification about end of SCC */
	public void notifyEndSCC()
	{
		// store index of last state of SCC
		int curSCC = storedSCCCount;
		sccEnd[curSCC] = storedStateCount - 1;
		storedSCCCount++;
	}

	/** SCCConsumer interface: Notification about state in SCC */
	public void notifyStateInSCC(int stateIndex)
	{
		int curSCC = storedSCCCount;
		stateList[storedStateCount] = stateIndex;
		stateToSCCIndex[stateIndex] = curSCC;
		storedStateCount++;
	}

	/** Returns the number of SCCs */
	public int getNumSCCs()
	{
		return storedSCCCount;
	}

	/** Compute and return the number of SCCs with at least 2 states. */
	public int countNonSingletonSCCs()
	{
		int nonSingletonSCCs = 0;
		for (int i = 0; i < getNumSCCs(); i++) {
			if (!isSingletonSCC(i)) {
				nonSingletonSCCs++;
			}
		}
		return nonSingletonSCCs;
	}

	/** Returns the number of states in the SCC with index {@code sccIndex} */
	public int getNumStatesInSCC(int sccIndex)
	{
		checkSCCIndex(sccIndex);
		return endOfSCC(sccIndex) - startOfSCC(sccIndex) + 1;
	}

	/** Returns true if the given SCC is a singleton SCC */
	public boolean isSingletonSCC(int sccIndex)
	{
		checkSCCIndex(sccIndex);
		return getNumStatesInSCC(sccIndex) == 1;
	}

	/**
	 * Returns the index of the SCC containing the given state,
	 * or -1 if the state does not belong to an SCC.
	 */
	public int getSCCIndex(int stateIndex)
	{
		checkStateIndex(stateIndex);
		return stateToSCCIndex[stateIndex];
	}

	/** Returns an IntSet for the states in the given SCC */
	public IntSet getStatesForSCC(final int sccIndex)
	{
		checkSCCIndex(sccIndex);

		final int start = startOfSCC(sccIndex);
		final int end = endOfSCC(sccIndex);

		return new IntSet() {

			@Override
			public OfInt iterator()
			{
				return new OfInt() {
					int cur = start;

					@Override
					public boolean hasNext()
					{
						return cur <= end;
					}

					@Override
					public int nextInt()
					{
						return stateList[cur++];
					}
				};
			}

			@Override
			public OfInt reversedIterator()
			{
				return new OfInt() {
					int cur = end;

					@Override
					public boolean hasNext()
					{
						return cur >= start;
					}

					@Override
					public int nextInt()
					{
						return stateList[cur--];
					}
				};
			}

			@Override
			public int cardinality()
			{
				return getNumStatesInSCC(sccIndex);
			}

			@Override
			public boolean contains(int stateIndex)
			{
				int sccForState = getSCCIndex(stateIndex);
				return sccForState == sccIndex;
			}
		};
	}

	/** Prints the SCC info to the log */
	public void print(PrismLog log)
	{
		for (int scc = 0; scc < getNumSCCs(); scc++) {
			log.println("SCC " + scc + " (" + getNumStatesInSCC(scc) + "):");
			for (OfInt states = getStatesForSCC(scc).iterator(); states.hasNext();) {
				int state = states.nextInt();
				log.print(" ");
				log.print(state);
			}
			log.println();
		}
	}

	private void checkStateIndex(int stateIndex)
	{
		if (stateIndex < 0 || stateIndex >= numStates)
			throw new IllegalArgumentException("SCCInfo: State index " + stateIndex + " is out of range");
	}

	private void checkSCCIndex(int sccIndex)
	{
		if (sccIndex < 0 || sccIndex >= storedSCCCount)
			throw new IllegalArgumentException("SCCInfo: SCC index " + sccIndex + " is out of range");
	}

	/** Compute the index in stateList of the first state in the given SCC */
	private int startOfSCC(int sccIndex)
	{
		if (sccIndex == 0) {
			return 0;
		} else {
			return sccEnd[sccIndex - 1] + 1;
		}
	}

	/** Compute the index in stateList of the last state in the given SCC */
	private int endOfSCC(int sccIndex)
	{
		return sccEnd[sccIndex];
	}
}
