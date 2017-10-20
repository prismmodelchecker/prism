//==============================================================================
//	
//	Copyright (c) 2014-
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * SCC consumer that stores lists of the discovered SCCs and BSCCs.
 * The list of BSCCs is computed on demand from the list of SCCs.
 */
public class SCCConsumerStore extends SCCConsumerBitSet {
	/* Computed list of SCCs */
	private List<BitSet> sccs = new ArrayList<BitSet>();
	/* Computed list of BSCCs */
	private List<BitSet> bsccs;
	/* States not in any BSCC */
	private BitSet notInBSCCs;
	/* States not in any SCC */
	private BitSet notInSCCs;

	/** Is the SCC computation finished? */
	private boolean finished = false;

	private Model model;

	/** Constructor */
	public SCCConsumerStore()
	{
		model = null;  // will be set by notifyStart call
	}

	@Override
	public void notifyStart(Model model)
	{
		this.model = model;
	}

	@Override
	public void notifyNextSCC(BitSet scc)
	{
		sccs.add(scc);
	}

	@Override
	public void notifyDone()
	{
		finished = true;
	}

	/**
	 * Get a list of the SCCs. Can only be called once the SCC computation is finished.
	 */
	public List<BitSet> getSCCs()
	{
		if (!finished)
			throw new UnsupportedOperationException("SCC computation is not yet finished.");

		return sccs;
	}

	/**
	 * Get a list of the BSCCs. Can only be called once the SCC computation is finished.
	 */
	public List<BitSet> getBSCCs()
	{
		if (!finished)
			throw new UnsupportedOperationException("SCC computation is not yet finished.");

		// If we don't have the list of BSCCs already, compute it.
		if (bsccs == null) {
			computeBSCCs();
		}
		return bsccs;
	}

	/**
	 * Get the set of states not in any BSCCs. Can only be called once the SCC computation is finished.
	 */
	public BitSet getNotInBSCCs()
	{
		if (!finished)
			throw new UnsupportedOperationException("SCC computation is not yet finished.");

		// If we don't have the set already, compute it.
		if (notInBSCCs == null) {
			computeBSCCs();
		}
		return notInBSCCs;
	}

	/**
	 * Compute the list of BSCCs from the list of SCCs.
	 */
	private void computeBSCCs()
	{
		if (!finished)
			throw new UnsupportedOperationException("SCC computation is not yet finished.");

		bsccs = new ArrayList<BitSet>();
		notInBSCCs = (BitSet) getNotInSCCs().clone();
		for (BitSet scc : sccs) {
			boolean bottom = true;
			// BSCC <=> for all states s, all successors are again in SCC
			for (int s = scc.nextSetBit(0); s >= 0; s = scc.nextSetBit(s + 1)) {
				if (!model.allSuccessorsInSet(s, scc)) {
					bottom = false;
					break;
				}
			}
			if (bottom)
				// store SCC as a BSCC
				bsccs.add(scc);
			else
				// add states in scc to notInBSCCs
				notInBSCCs.or(scc);
		}
	}

	public BitSet getNotInSCCs()
	{
		if (!finished)
			throw new UnsupportedOperationException("SCC computation is not yet finished.");

		if (notInSCCs != null) {
			return notInSCCs;
		}
		BitSet result = new BitSet();
		for (BitSet scc : getSCCs()) {
			result.or(scc);
		}
		result.flip(0, model.getNumStates());
		notInSCCs = result;
		return notInSCCs;
	}

}
