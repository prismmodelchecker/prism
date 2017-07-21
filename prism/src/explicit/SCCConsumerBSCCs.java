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

import java.util.BitSet;

import prism.PrismException;

/**
 * Abstract base class for an SCC consumer that is only interested in
 * the bottom strongly-connected components (BSCCs). <br/>
 *
 * Intercepts the {@code notifyNextSCC()} call, checks whether the SCC
 * is a BSCC and calls {@code notifyNextBSCC()} if that is the case.
 */
public abstract class SCCConsumerBSCCs extends SCCConsumerBitSet {

	protected Model model;

	/** Constructor */
	public SCCConsumerBSCCs()
	{
		model = null;  // will be set by notifyStart call
	}

	public  void notifyStart(Model model)
	{
		this.model = model;
	}

	/**
	 * Call-back function. Called upon discovery of a BSCC.
	 */
	public abstract void notifyNextBSCC(BitSet bscc) throws PrismException;

	@Override
	public void notifyNextSCC(BitSet scc) throws PrismException
	{
		boolean bottom = true;
		// BSCC <=> for all states s, all successors are again in the SCC.
		for (int s = scc.nextSetBit(0); s >= 0; s = scc.nextSetBit(s + 1)) {
			if (!model.allSuccessorsInSet(s, scc)) {
				bottom = false;
				break;
			}
		}

		if (bottom) {
			notifyNextBSCC(scc);
		}
	}
}
