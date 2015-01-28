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

package acceptance;

import java.util.ArrayList;

import common.IterableBitSet;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * A Rabin acceptance condition (based on JDD state sets)
 * This is a list of RabinPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Rabin pair has a state set L and K and is accepting iff
 * L is not visited infinitely often and K is visited infinitely often:
 *   (F G !"L") & (G F "K").
 *
 * The Rabin condition is accepting if at least one of the pairs is accepting.
 */
@SuppressWarnings("serial")
public class AcceptanceRabinDD
       extends ArrayList<AcceptanceRabinDD.RabinPairDD>
       implements AcceptanceOmegaDD
{

	/**
	 * A pair in a Rabin acceptance condition, i.e., with
	 *  (F G !"L")  &  (G F "K")
	 **/
	public static class RabinPairDD {
		/** State set L (should be visited only finitely often) */
		private JDDNode L;

		/** State set K (should be visited infinitely often) */
		private JDDNode K;

		/**
		 * Constructor with L and K state sets.
		 * Becomes owner of the references of L and K.
		 */
		public RabinPairDD(JDDNode L, JDDNode K)
		{
			this.L = L;
			this.K = K;
		}

		/** Clear resources of the state sets */
		public void clear()
		{
			if (L!=null) JDD.Deref(L);
			if (K!=null) JDD.Deref(K);
		}

		/** Get a referenced copy of the state set L.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getL()
		{
			JDD.Ref(L);
			return L;
		}

		/** Get a referenced copy of the state set K.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getK()
		{
			JDD.Ref(K);
			return K;
		}

		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 */
		public boolean isBSCCAccepting(JDDNode bscc_states)
		{
			if (JDD.AreInterecting(L, bscc_states)) {
				// there is some state in bscc_states that is
				// forbidden by L
				return false;
			}

			if (JDD.AreInterecting(K, bscc_states)) {
				// there is some state in bscc_states that is
				// contained in K -> infinitely often visits to K
				return true;
			}

			return false;
		}

		/** Returns a textual representation of this Rabin pair. */
		@Override
		public String toString()
		{
			return "(" + L + "," + K + ")";
		}
	}

	/**
	 * Constructor, from a BitSet-based AcceptanceRabin.
	 *
	 * @param acceptance the BitSet-based acceptance condition
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bit set
	 */
	public AcceptanceRabinDD(AcceptanceRabin acceptance, JDDVars ddRowVars)
	{
		for (AcceptanceRabin.RabinPair pair : acceptance) {
			// get BDD based newL and newK from the bit sets
			JDDNode newL = JDD.Constant(0);
			for (int i : IterableBitSet.getSetBits(pair.getL())) {
				newL = JDD.SetVectorElement(newL, ddRowVars, i, 1.0);
			}

			JDDNode newK = JDD.Constant(0);
			for (int i : IterableBitSet.getSetBits(pair.getK())) {
				newK = JDD.SetVectorElement(newK, ddRowVars, i, 1.0);
			}

			RabinPairDD newPair = new RabinPairDD(newL, newK);
			this.add(newPair);
		}
	}

	/** Returns true if the bottom strongly connected component
	 * given by bscc_states is accepting for this Rabin condition,
	 * i.e., there is a pair that accepts for bscc_states.
	 */
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		for (RabinPairDD pair : this) {
			if (pair.isBSCCAccepting(bscc_states)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void clear()
	{
		for (RabinPairDD pair : this) {
			pair.clear();
		}
		super.clear();
	}

	/** Returns a textual representation of this acceptance condition. */
	@Override
	public String toString()
	{
		String result = "";
		for (RabinPairDD pair : this) {
			result += pair.toString();
		}
		return result;
	}

	@Override
	public String getSizeStatistics()
	{
		return size() + " Rabin pairs";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.RABIN;
	}

	@Override
	public String getTypeAbbreviated()
	{
		return "R";
	}

	@Override
	public String getTypeName()
	{
		return "Rabin";
	}
}
