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
			return L.copy();
		}

		/** Get a referenced copy of the state set K.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getK()
		{
			return K.copy();
		}

		public RabinPairDD clone()
		{
			return new RabinPairDD(getL(), getK());
		}

		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
		 */
		public boolean isBSCCAccepting(JDDNode bscc_states)
		{
			if (JDD.AreIntersecting(L, bscc_states)) {
				// there is some state in bscc_states that is
				// forbidden by L
				return false;
			}

			if (JDD.AreIntersecting(K, bscc_states)) {
				// there is some state in bscc_states that is
				// contained in K -> infinitely often visits to K
				return true;
			}

			return false;
		}

		public AcceptanceGenericDD toAcceptanceGeneric()
 		{
			AcceptanceGenericDD genericL = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.FIN, getL());
			AcceptanceGenericDD genericK = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.INF, getK());

			//      F G ! "L" & G F "K"
			// <=>  Fin(L) & Inf(K)
			return new AcceptanceGenericDD(AcceptanceGeneric.ElementType.AND, genericL, genericK);
 		}

		/**
		 * Replaces the BDD functions for the acceptance sets
		 * of this Rabin pair with the intersection
		 * of the current acceptance sets and the function {@code restrict}.
		 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
		 */
		public void intersect(JDDNode restrict)
		{
			L = JDD.And(L, restrict.copy());
			K = JDD.And(K, restrict.copy());
		}

		/** Returns a textual representation of this Rabin pair. */
		@Override
		public String toString()
		{
			return "(" + L + "," + K + ")";
		}
	}

	/** Constructor, create empty condition */
	public AcceptanceRabinDD()
	{
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

	@Override
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
	public AcceptanceRabinDD clone()
	{
		AcceptanceRabinDD result = new AcceptanceRabinDD();
		for (RabinPairDD pair : this) {
			result.add(pair.clone());
		}
		return result;
	}

	@Override
	public void intersect(JDDNode restrict)
	{
		for (RabinPairDD pair : this) {
			pair.intersect(restrict);
		}
	}

	/**
	 * Get the Streett acceptance condition that is the dual of this Rabin acceptance condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Streett condition.
	 * <br>
	 * Deprecated, use complementToStreett() or complement(...)
	 * @return the complement Streett acceptance condition
	 */
	@Deprecated
	public AcceptanceStreettDD complement()
	{
		return complementToStreett();
	}

	/**
	 * Returns a new Rabin acceptance condition that corresponds to the disjunction
	 * of this and the other Rabin acceptance condition. The RabinPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other Rabin acceptance condition
	 * @return new AcceptanceRabin, disjunction of this and other
	 */
	public AcceptanceRabinDD or(AcceptanceRabinDD other)
	{
		AcceptanceRabinDD result = new AcceptanceRabinDD();
		for (RabinPairDD pair : this) {
			result.add(pair.clone());
		}
		for (RabinPairDD pair : other) {
			result.add(pair.clone());
		}
		return result;
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
	public AcceptanceOmegaDD complement(AcceptanceType... allowedAcceptance) throws PrismNotSupportedException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.STREETT)) {
			return complementToStreett();
		}
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
	}

	/**
	 * Get the Streett acceptance condition that is the dual of this Rabin acceptance condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Streett condition.
	 * @return the complement Streett acceptance condition
	 */
	public AcceptanceStreettDD complementToStreett()
	{
		AcceptanceStreettDD accStreett = new AcceptanceStreettDD();

		for (RabinPairDD accPairRabin : this) {
			JDDNode R = accPairRabin.getK();
			JDDNode G = accPairRabin.getL();
			AcceptanceStreettDD.StreettPairDD accPairStreett = new AcceptanceStreettDD.StreettPairDD(R, G);
			accStreett.add(accPairStreett);
		}

		return accStreett;
	}

	@Override
	public AcceptanceGenericDD toAcceptanceGeneric()
	{
		if (size() == 0) {
			return new AcceptanceGenericDD(false);
		}
		AcceptanceGenericDD genericPairs = null;
		for (RabinPairDD pair : this) {
			AcceptanceGenericDD genericPair = pair.toAcceptanceGeneric();
			if (genericPairs == null) {
				genericPairs = genericPair;
			} else {
				genericPairs = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.OR, genericPairs, genericPair);
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
