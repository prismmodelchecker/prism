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
 * A Streett acceptance condition (based on JDD state sets).
 * This is a list of StreettPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Streett pair has a state set R and G and is accepting iff
 * visiting R infinitely often implies visiting G infinitely often:
 *   (G F "R") -> (G F "G").
 *
 * The Streett condition is accepting if all pairs are accepting.
 */
@SuppressWarnings("serial")
public class AcceptanceStreettDD
       extends ArrayList<AcceptanceStreettDD.StreettPair>
       implements AcceptanceOmegaDD
{

	/**
	 * A pair in a Streett acceptance condition, i.e., with
	 *  (G F "R") -> (G F "G")
	 **/
	public static class StreettPair {
		/** State set R */
		private JDDNode R;
		
		/** State set G */
		private JDDNode G;

		/**
		 * Constructor with R and G state sets.
		 * Becomes owner of the references of R and G.
		 */
		public StreettPair(JDDNode R, JDDNode G)
		{
			this.R = R;
			this.G = G;
		}

		/** Clear resources of the state sets */
		public void clear()
		{
			if (R != null) JDD.Deref(R);
			if (G != null) JDD.Deref(G);
		}

		/** Get a referenced copy of the state set R.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getR()
		{
			JDD.Ref(R);
			return R;
		}

		/** Get a referenced copy of the state set G.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getG()
		{
			JDD.Ref(G);
			return G;
		}

		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 */
		public boolean isBSCCAccepting(JDDNode bscc_states)
		{
			if (JDD.AreInterecting(R, bscc_states)) {
				// there is some state in bscc_states
				// that is in R, requiring that G is visited
				// as well:
				if (!JDD.AreInterecting(G, bscc_states)) {
					return false;
				} else {
					// G is visited as well
					return true;
				}
			} else {
				// no R visited, no need to check for G
				return true;
			}
		}

		@Override
		public String toString()
		{
			return "(" + R + "->" + G + ")";
		}
	}

	/**
	 * Constructor, from a BitSet-based AcceptanceStreett.
	 *
	 * @param acceptance the BitSet-based acceptance condition
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bit set
	 */
	public AcceptanceStreettDD(AcceptanceStreett acceptance, JDDVars ddRowVars)
	{
		for (AcceptanceStreett.StreettPair pair : acceptance) {
			// get BDD based newR and newG from the bit sets
			JDDNode newR = JDD.Constant(0);
			for (int i : IterableBitSet.getSetBits(pair.getR())) {
				newR = JDD.SetVectorElement(newR, ddRowVars, i, 1.0);
			}
	
			JDDNode newG = JDD.Constant(0);
			for (int i : IterableBitSet.getSetBits(pair.getG())) {
				newG = JDD.SetVectorElement(newG, ddRowVars, i, 1.0);
			}
	
			StreettPair newPair = new StreettPair(newR, newG);
			this.add(newPair);
		}
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		for (StreettPair pair : this) {
			if (!pair.isBSCCAccepting(bscc_states)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void clear()
	{
		for (StreettPair pair : this) {
			pair.clear();
		}
		super.clear();
	}

	@Override
	public String toString() {
		String result = "";
		for (StreettPair pair : this) {
			result += pair.toString();
		}
		return result;
	}

	@Override
	public String getSizeStatistics()
	{
		return size() + " Streett pairs";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.STREETT;
	}

	@Override
	public String getTypeAbbreviated()
	{
		return "S";
	}

	@Override
	public String getTypeName()
	{
		return "Streett";
	}
}
