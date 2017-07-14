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
import prism.PrismNotSupportedException;

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
       extends ArrayList<AcceptanceStreettDD.StreettPairDD>
       implements AcceptanceOmegaDD
{

	/**
	 * A pair in a Streett acceptance condition, i.e., with
	 *  (G F "R") -> (G F "G")
	 **/
	public static class StreettPairDD {
		/** State set R */
		private JDDNode R;
		
		/** State set G */
		private JDDNode G;

		/**
		 * Constructor with R and G state sets.
		 * Becomes owner of the references of R and G.
		 */
		public StreettPairDD(JDDNode R, JDDNode G)
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
			return R.copy();
		}

		/** Get a referenced copy of the state set G.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getG()
		{
			return G.copy();
		}

		public StreettPairDD clone()
		{
			return new StreettPairDD(getR(), getG());
		}

		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
		 */
		public boolean isBSCCAccepting(JDDNode bscc_states)
		{
			if (JDD.AreIntersecting(R, bscc_states)) {
				// there is some state in bscc_states
				// that is in R, requiring that G is visited
				// as well:
				if (!JDD.AreIntersecting(G, bscc_states)) {
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

		public AcceptanceGenericDD toAcceptanceGeneric()
		{
		    AcceptanceGenericDD genericR = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.FIN, getR());
		    AcceptanceGenericDD genericG = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.INF, getG());
		    //      G F "R" -> G F "G"
		    // <=>  ! G F "R"  | G F "G"
		    // <=>  F G ! "R"  | G F "G"
		    // <=>  Fin(R) | Inf(G)
		    return new AcceptanceGenericDD(AcceptanceGeneric.ElementType.OR, genericR, genericG);
                }

		/**
		 * Replaces the BDD functions for the acceptance sets
		 * of this Streett pair with the intersection
		 * of the current acceptance sets and the function {@code restrict}.
		 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
		 */
		public void intersect(JDDNode restrict)
		{
			R = JDD.And(R, restrict.copy());
			G = JDD.And(G, restrict.copy());
		}

		@Override
		public String toString()
		{
			return "(" + R + "->" + G + ")";
		}
	}

	/** Constructor, create empty condition */
	public AcceptanceStreettDD()
	{
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
	
			StreettPairDD newPair = new StreettPairDD(newR, newG);
			this.add(newPair);
		}
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		for (StreettPairDD pair : this) {
			if (!pair.isBSCCAccepting(bscc_states)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public AcceptanceStreettDD clone()
	{
		AcceptanceStreettDD result = new AcceptanceStreettDD();
		for (StreettPairDD pair : this) {
			result.add(pair.clone());
		}
		return result;
	}


	@Override
	public void intersect(JDDNode restrict)
	{
		for (StreettPairDD pair : this) {
			pair.intersect(restrict);
		}
	}

	@Override
	public void clear()
	{
		for (StreettPairDD pair : this) {
			pair.clear();
		}
		super.clear();
	}

	/**
	 * Returns a new Streett acceptance condition that corresponds to the conjunction
	 * of this and the other Streett acceptance condition. The StreettPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other Streett acceptance condition
	 * @return new AcceptanceStreett, conjunction of this and other
	 */
	public AcceptanceStreettDD and(AcceptanceStreettDD other)
	{
		AcceptanceStreettDD result = new AcceptanceStreettDD();
		for (StreettPairDD pair : this) {
			result.add(pair.clone());
		}
		for (StreettPairDD pair : other) {
			result.add(pair.clone());
		}
		return result;
	}

	/**
	 * Get the Rabin acceptance condition that is the dual of this Streett acceptance condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Rabin condition.
	 * <br>
	 * Deprecated, use complementToRabin or complement(...).
	 * @return the complement Rabin acceptance condition
	 */
	@Deprecated
	public AcceptanceRabinDD complement()
	{
		return complementToRabin();
	}

	@Override
	public String toString() {
		String result = "";
		for (StreettPairDD pair : this) {
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
	public AcceptanceOmegaDD complement(AcceptanceType... allowedAcceptance) throws PrismNotSupportedException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.RABIN)) {
			return complementToRabin();
		}
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
	}

	public AcceptanceRabinDD complementToRabin()
	{
		AcceptanceRabinDD accRabin = new AcceptanceRabinDD();

		for (StreettPairDD accPairStreett : this) {
			JDDNode L = accPairStreett.getG();
			JDDNode K = accPairStreett.getR();
			AcceptanceRabinDD.RabinPairDD accPairRabin = new AcceptanceRabinDD.RabinPairDD(L, K);
			accRabin.add(accPairRabin);
		}
		return accRabin;
	}

	@Override
	public AcceptanceGenericDD toAcceptanceGeneric()
	{
		if (size() == 0) {
			return new AcceptanceGenericDD(true);
		}
		AcceptanceGenericDD genericPairs = null;
		for (StreettPairDD pair : this) {
			AcceptanceGenericDD genericPair = pair.toAcceptanceGeneric();
			if (genericPairs == null) {
				genericPairs = genericPair;
			} else {
				genericPairs = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.AND, genericPairs, genericPair);
			}
		}
		return genericPairs;
	}

	@Override
	@Deprecated
	public String getTypeAbbreviated() {
		return getType().getNameAbbreviated();
	}

	@Override
	@Deprecated
	public String getTypeName() {
		return getType().getName();
	}
}
