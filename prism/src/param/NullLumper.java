//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Lumper for identity bisimulation.
 * That is, this class does basically nothing and is there for convenience
 * when choosing between different types of bisimulations. So, for
 * disabling bisimulation, one can use an object of this class.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class NullLumper extends Lumper {

	/**
	 * Creates a new identity bisimulation lumper for given Markov chain.
	 * @param origPmc parametric Markov chain to create lumper for
	 */
	NullLumper(MutablePMC origPmc) {
		this.origPmc = origPmc;
		this.optPmc = origPmc;
	}

	/**
	 * Does not have to do anything.
	 */
	@Override
	protected void refineBlock(HashSet<Integer> oldBlock,
			ArrayList<HashSet<Integer>> newBlocks) {
	}

	/**
	 * Does not have to do anything.
	 */
	@Override
	protected void buildQuotient() {
	}

	/**
	 * Build identity mapping.
	 * 
	 * @return identity mapping
	 */
	@Override
	int[] getOriginalToOptimised() {
		int[] result = new int[origPmc.getNumStates()];
		for (int state = 0; state < result.length; state++) {
			result[state] = state;
		}
		return result;
	}
}
