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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;

import prism.PrismException;
import prism.PrismNotSupportedException;
import jdd.JDDVars;

/**
 * A Rabin acceptance condition (based on BitSet state sets).
 * This is a list of RabinPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Rabin pair has a state set L and K and is accepting iff
 * L is not visited infinitely often and K is visited infinitely often:
 *   (F G !"L") & (G F "K").
 *
 * The Rabin condition is accepting if at least one of the pairs is accepting.
 */
@SuppressWarnings("serial")
public class AcceptanceRabin
       extends ArrayList<AcceptanceRabin.RabinPair>
       implements AcceptanceOmega
{

	/**
	 * A pair in a Rabin acceptance condition, i.e., with
	 *  (F G !"L")  &  (G F "K")
	 **/
	public static class RabinPair {
		/** State set L (should be visited only finitely often) */
		private BitSet L;

		/** State set K (should be visited infinitely often) */
		private BitSet K;

		/**
		 * Constructor with L and K state sets.
		 *  (F G !"L")  &  (G F "K")
		 */
		public RabinPair(BitSet L, BitSet K) {
			this.L = L;
			this.K = K;
		}

		/** Get the state set L */
		public BitSet getL()
		{
			return L;
		}

		/** Get the state set K */
		public BitSet getK()
		{
			return K;
		}

		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 */
		public boolean isBSCCAccepting(BitSet bscc_states)
		{
			if (L.intersects(bscc_states)) {
				// there is some state in bscc_states that is
				// forbidden by L
				return false;
			}

			if (K.intersects(bscc_states)) {
				// there is some state in bscc_states that is
				// contained in K -> infinitely often visits to K
				return true;
			}

			return false;
		}

		public AcceptanceGeneric toAcceptanceGeneric()
		{
			AcceptanceGeneric genericL = new AcceptanceGeneric(AcceptanceGeneric.ElementType.FIN, (BitSet)L.clone());
			AcceptanceGeneric genericK = new AcceptanceGeneric(AcceptanceGeneric.ElementType.INF, (BitSet)K.clone());
			
			//      F G ! "L" & G F "K"
			// <=>  Fin(L) & Inf(K)
			return new AcceptanceGeneric(AcceptanceGeneric.ElementType.AND, genericL, genericK);
		}

		/** Generate signature for this Rabin pair and the given state.
		 *  If the state is a member of L, returns "-pairIndex".
		 *  If the state is a member of K, returns "+pairIndex".
		 *  @param stateIndex the state index
		 *  @param pairIndex the index of this Rabin pair
		 **/
		public String getSignatureForState(int stateIndex, int pairIndex)
		{
			if (L.get(stateIndex)) {
				return "-"+pairIndex;
			} else if (K.get(stateIndex)) {
				return "+"+pairIndex;
			} else {
				return "";
			}
		}

		@Override
		public RabinPair clone()
		{
			return new RabinPair((BitSet)L.clone(), (BitSet)K.clone());
		}

		/** Returns a textual representation of this Rabin pair. */
		@Override
		public String toString() {
			return "(" + L + "," + K + ")";
		}
	}

	/** Make a copy of the acceptance condition. */
	public AcceptanceRabin clone()
	{
		AcceptanceRabin result = new AcceptanceRabin();
		for (RabinPair pair : this) {
			result.add(pair.clone());
		}

		return result;
	}

	/** Returns true if the bottom strongly connected component
	 * given by bscc_states is accepting for this Rabin condition,
	 * i.e., there is a pair that accepts for bscc_states.
	 */
	public boolean isBSCCAccepting(BitSet bscc_states)
	{
		for (RabinPair pair : this) {
			if (pair.isBSCCAccepting(bscc_states)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void lift(LiftBitSet lifter) {
		for (RabinPair pair : this) {
			pair.L = lifter.lift(pair.L);
			pair.K = lifter.lift(pair.K);
		}
	}

	/**
	 * Get the Streett acceptance condition that is the dual of this Rabin acceptance condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Streett condition.
	 * @return the complement Streett acceptance condition
	 */
	public AcceptanceStreett complementToStreett()
	{
		AcceptanceStreett accStreett = new AcceptanceStreett();

		for (RabinPair accPairRabin : this) {
			BitSet R = (BitSet) accPairRabin.getK().clone();
			BitSet G = (BitSet) accPairRabin.getL().clone();
			AcceptanceStreett.StreettPair accPairStreett = new AcceptanceStreett.StreettPair(R, G);
			accStreett.add(accPairStreett);
		}

		return accStreett;
	}

	/** Complement this acceptance condition, return as AcceptanceGeneric. */
	public AcceptanceGeneric complementToGeneric()
	{
		return toAcceptanceGeneric().complementToGeneric();
	}

	@Override
	public AcceptanceOmega complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException
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
	 * Returns a new Rabin acceptance condition that corresponds to the disjunction
	 * of this and the other Rabin acceptance condition. The RabinPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other Rabin acceptance condition
	 * @return new AcceptanceRabin, disjunction of this and other
	 */
	public AcceptanceRabin or(AcceptanceRabin other)
	{
		AcceptanceRabin result = new AcceptanceRabin();
		for (RabinPair pair : this) {
			result.add((RabinPair) pair.clone());
		}
		for (RabinPair pair : other) {
			result.add((RabinPair) pair.clone());
		}
		return result;
	}

	@Override
	public AcceptanceRabinDD toAcceptanceDD(JDDVars ddRowVars)
	{
		return new AcceptanceRabinDD(this, ddRowVars);
	}

	@Override
	public AcceptanceGeneric toAcceptanceGeneric()
	{
		if (size() == 0) {
			return new AcceptanceGeneric(false);
		}
		AcceptanceGeneric genericPairs = null;
		for (RabinPair pair : this) {
			AcceptanceGeneric genericPair = pair.toAcceptanceGeneric();
			if (genericPairs == null) {
				genericPairs = genericPair;
			} else {
				genericPairs = new AcceptanceGeneric(AcceptanceGeneric.ElementType.OR, genericPairs, genericPair);
			}
		}
		return genericPairs;
	}

	/**
	 * Get the acceptance signature for state stateIndex.
	 **/
	public String getSignatureForState(int stateIndex)
	{
		String result = "";

		for (int pairIndex=0; pairIndex<size(); pairIndex++) {
			RabinPair pair = get(pairIndex);
			result += pair.getSignatureForState(stateIndex,  pairIndex);
		}

		return result;
	}

	@Override
	public String getSignatureForStateHOA(int stateIndex)
	{
		String result = "";

		for (int pairIndex=0; pairIndex<size(); pairIndex++) {
			RabinPair pair = get(pairIndex);
			if (pair.getL().get(stateIndex)) {
				result += (result.isEmpty() ? "" : " ") + pairIndex*2;
			}
			if (pair.getK().get(stateIndex)) {
				result += (result.isEmpty() ? "" : " ") + (pairIndex*2+1);
			}
		}

		if (!result.isEmpty())
			result = "{"+result+"}";

		return result;
	}

	
	/** Returns a textual representation of this acceptance condition. */
	@Override
	public String toString()
	{
		String result = "";
		for (RabinPair pair : this) {
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
	@Deprecated
	public String getTypeAbbreviated() {
		return getType().getNameAbbreviated();
	}

	@Override
	@Deprecated
	public String getTypeName() {
		return getType().getName();
	}

	@Override
	public void outputHOAHeader(PrintStream out)
	{
		out.println("acc-name: Rabin "+size());
		out.print("Acceptance: " + (size()*2)+" ");
		if (size() == 0) {
			out.println("f");
			return;
		}

		for (int pair = 0; pair < size(); pair++) {
			if (pair > 0) out.print(" | ");
			out.print("( Fin(" + 2*pair + ") & Inf(" + (2*pair+1) +") )");
		}
		out.println();
	}
}
