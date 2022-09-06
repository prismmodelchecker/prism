//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
 * A Generalized Rabin acceptance condition (based on JDD state sets)
 * This is a list of GenRabinPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Generalized Rabin pair has state sets L and K_1,...,K_n and is accepting iff
 * L is not visited infinitely often and all K_j are visited infinitely often:
 *   (F G !"L") & (G F "K_1") & ... & (G F "K_n").
 *
 * The Generalized Rabin condition is accepting if at least one of the pairs is accepting.
 */
@SuppressWarnings("serial")
public class AcceptanceGenRabinDD
       extends ArrayList<AcceptanceGenRabinDD.GenRabinPairDD>
       implements AcceptanceOmegaDD
{

	/**
	 * A pair in a Generalized Rabin acceptance condition, i.e., with
	 *  (F G !"L") & (G F "K_1") & ... & (G F "K_n").
	 **/
	public static class GenRabinPairDD implements Cloneable {
		/** State set L (should be visited only finitely often) */
		private JDDNode L;

		/** State sets K_j (should all be visited infinitely often) */
		private ArrayList<JDDNode> K_list;

		/**
		 * Constructor with L and K_j state sets.
		 * Becomes owner of the references of L and K_j's.
		 */
		public GenRabinPairDD(JDDNode L, ArrayList<JDDNode> K_list)
		{
			this.L = L;
			this.K_list = K_list;
		}

		/** Clear resources of the state sets */
		public void clear()
		{
			if (L!=null) JDD.Deref(L);
			for (JDDNode K_j : K_list)
				JDD.Deref(K_j);
		}

		/** Get a referenced copy of the state set L.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getL()
		{
			return L.copy();
		}

		/** Get the number of K_j sets */
		public int getNumK()
		{
			return K_list.size();
		}
		
		/** Get a referenced copy of the state set K_j.
		 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
		 */
		public JDDNode getK(int j)
		{
			return K_list.get(j).copy();
		}

		@Override
		public GenRabinPairDD clone()
		{
			ArrayList<JDDNode> newK_list = new ArrayList<JDDNode>();
			for (JDDNode K_j : K_list) {
				newK_list.add(K_j.copy());
			}
			return new GenRabinPairDD(getL(), newK_list);
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

			for (JDDNode K_j : K_list) {
				if (!JDD.AreIntersecting(K_j, bscc_states)) {
					// there is some state in bscc_states that is
					// contained in K_j -> infinitely often visits to K_j
					return false;
				}
			}

			return true;
		}

		public AcceptanceGenericDD toAcceptanceGeneric()
 		{
			AcceptanceGenericDD genericL = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.FIN, L.copy());
			if (getNumK() == 0) {
				return genericL;
			}
			AcceptanceGenericDD genericKs = null;
			for (JDDNode K : K_list) {
				AcceptanceGenericDD genericK = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.INF, K.copy());
				if (genericKs == null) {
					genericKs = genericK;
				} else {
					genericKs = new AcceptanceGenericDD(AcceptanceGeneric.ElementType.AND, genericKs, genericK);
				}
			}
			return new AcceptanceGenericDD(AcceptanceGeneric.ElementType.AND, genericL, genericKs);
 		}

		/**
		 * Replaces the BDD functions for the acceptance sets
		 * of this generalized Rabin pair with the intersection
		 * of the current acceptance sets and the function {@code restrict}.
		 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
		 */
		public void intersect(JDDNode restrict)
		{
			L = JDD.And(L, restrict.copy());
			for (int i = 0; i < K_list.size(); i++) {
				K_list.set(i, JDD.And(K_list.get(i), restrict.copy()));
			}
		}

		/** Returns a textual representation of this Rabin pair. */
		@Override
		public String toString()
		{
			String s = "(" + L;
			for (JDDNode K_j : K_list)
				s += "," + K_j;
			s += ")";
			return s;
		}
	}

	/** Constructor, create empty condition */
	public AcceptanceGenRabinDD()
	{
	}

	/**
	 * Constructor, from a BitSet-based AcceptanceGenRabin.
	 *
	 * @param acceptance the BitSet-based acceptance condition
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bit set
	 */
	public AcceptanceGenRabinDD(AcceptanceGenRabin acceptance, JDDVars ddRowVars)
	{
		for (AcceptanceGenRabin.GenRabinPair pair : acceptance) {
			// get BDD based newL and newK from the bit sets
			JDDNode newL = JDD.Constant(0);
			for (int i : IterableBitSet.getSetBits(pair.getL())) {
				newL = JDD.SetVectorElement(newL, ddRowVars, i, 1.0);
			}

			ArrayList<JDDNode> newK_list = new ArrayList<JDDNode>();
			int n = pair.getNumK();
			for (int j = 0; j < n; j++) {
				JDDNode newK_j = JDD.Constant(0);
				for (int i : IterableBitSet.getSetBits(pair.getK(j))) {
					newK_j = JDD.SetVectorElement(newK_j, ddRowVars, i, 1.0);
				}
				newK_list.add(newK_j);
			}

			GenRabinPairDD newPair = new GenRabinPairDD(newL, newK_list);
			this.add(newPair);
		}
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		for (GenRabinPairDD pair : this) {
			if (pair.isBSCCAccepting(bscc_states)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public AcceptanceGenRabinDD clone()
	{
		AcceptanceGenRabinDD result = new AcceptanceGenRabinDD();
		for (GenRabinPairDD pair : this) {
			result.add(pair.clone());
		}
		return result;
	}

	@Override
	public void intersect(JDDNode restrict)
	{
		for (GenRabinPairDD pair : this) {
			pair.intersect(restrict);
		}
	}

	@Override
	public void clear()
	{
		for (GenRabinPairDD pair : this) {
			pair.clear();
		}
		super.clear();
	}

	/** Returns a textual representation of this acceptance condition. */
	@Override
	public String toString()
	{
		String result = "";
		for (GenRabinPairDD pair : this) {
			result += pair.toString();
		}
		return result;
	}

	@Override
	public String getSizeStatistics()
	{
		return size() + " Generalized Rabin pairs";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.GENERALIZED_RABIN;
	}

	@Override
	public AcceptanceOmegaDD complement(AcceptanceType... allowedAcceptance) throws PrismNotSupportedException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
	}

	@Override
	public AcceptanceGenericDD toAcceptanceGeneric()
	{
		if (size() == 0) {
			return new AcceptanceGenericDD(false);
		}
		AcceptanceGenericDD genericPairs = null;
		for (GenRabinPairDD pair : this) {
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
