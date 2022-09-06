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

import java.util.BitSet;

import prism.PrismException;

/**
 * Abstract base class for a consumer of SCC information, for use with an {@code SCCComputer},
 * where each SCC is stored as a BitSet.
 * <br>
 * When a new SCC is discovered, {@code notifyNextSCC()} will be called with a {@code BitSet} of the
 * states in the SCC. When the SCC computation is finished, {@code notifyDone()} will be
 * called once.
 * <br>
 * By default, for each SCC a fresh BitSet is created. If the BitSets that are
 * passed to {@code notifyNextSCC} can be reused for the next call, the {@code reuseBitSet}
 * flag can be set in the constructor.
 */
public abstract class SCCConsumerBitSet implements SCCConsumer
{
	private BitSet curSCC = null;
	private boolean reuseBitSet = false;

	/** Default constructor. Don't reuse the BitSets */
	public SCCConsumerBitSet()
	{
		this(false);
	}

	/**
	 * Constructor. If {@code reuseBitSet} is set, reuse the same BitSet for all
	 * calls to {@code notifyNextSCC}.
	 * @param reuseBitSet allow reuse of BitSets
	 */
	public SCCConsumerBitSet(boolean reuseBitSet)
	{
		this.reuseBitSet = reuseBitSet;
		if (reuseBitSet)
			curSCC = new BitSet();
	}

	@Override
	public void notifyStartSCC() throws PrismException
	{
		if (reuseBitSet) {
			curSCC.clear();
		} else {
			curSCC = new BitSet();
		}
	}

	@Override
	public void notifyStateInSCC(int stateIndex) throws PrismException
	{
		curSCC.set(stateIndex);
	}

	@Override
	public void notifyEndSCC() throws PrismException
	{
		notifyNextSCC(curSCC);
		if (!reuseBitSet) {
			curSCC = null;
		}
	}

	public abstract void notifyNextSCC(BitSet scc) throws PrismException;
}
