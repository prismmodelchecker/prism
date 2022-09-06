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
 * A Streett acceptance condition (based on BitSet state sets).
 * This is a list of StreettPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Streett pair has a state set R and G and is accepting iff
 * visiting R infinitely often implies visiting G infinitely often:
 *   (G F "R") -> (G F "G").
 *
 * The Streett condition is accepting if all pairs are accepting.
 */
@SuppressWarnings("serial")
public class AcceptanceStreett
       extends ArrayList<AcceptanceStreett.StreettPair>
       implements AcceptanceOmega
{

	/**
	 * A pair in a Streett acceptance condition, i.e., with
	 *  (G F "R") -> (G F "G")
	 **/
	public static class StreettPair {
		/** State set R */
		private BitSet R;

		/** State set G */
		private BitSet G;

		/**
		 * Constructor with R and G state sets.
		 * 	 (G F "R") -> (G F "G")
		 */
		public StreettPair(BitSet R, BitSet G)
		{
			this.R = R;
			this.G = G;
		}

		/** Get the state set R */
		public BitSet getR()
		{
			return R;
		}

		/** Get the state set G */
		public BitSet getG()
		{
			return G;
		}

		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 */
		public boolean isBSCCAccepting(BitSet bscc_states)
		{
			if (R.intersects(bscc_states)) {
				// there is some state in bscc_states
				// that is in R, requiring that G is visited
				// as well:
				if (!G.intersects(bscc_states)) {
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

		public AcceptanceGeneric toAcceptanceGeneric()
		{
			AcceptanceGeneric genericR = new AcceptanceGeneric(AcceptanceGeneric.ElementType.FIN, (BitSet)R.clone());
			AcceptanceGeneric genericG = new AcceptanceGeneric(AcceptanceGeneric.ElementType.INF, (BitSet)G.clone());
			//      G F "R" -> G F "G"
			// <=>  ! G F "R"  | G F "G"
			// <=>  F G ! "R"  | G F "G"
			// <=>  Fin(R) | Inf(G)
			return new AcceptanceGeneric(AcceptanceGeneric.ElementType.OR, genericR, genericG);
		}

		/** Generate signature for this Streett pair and the given state.
		 *  If the state is a member of R, returns "-pairIndex".
		 *  If the state is a member of G, returns "+pairIndex".
		 *  @param stateIndex the state index
		 *  @param pairIndex the index of this Streeet pair
		 **/
		public String getSignatureForState(int stateIndex, int pairIndex)
		{
			if (G.get(stateIndex)) {
				return "+"+pairIndex;
			} else if (R.get(stateIndex)) {
				return "-"+pairIndex;
			} else {
				return "";
			}
		}

		@Override
		public StreettPair clone()
		{
			return new StreettPair((BitSet)R.clone(), (BitSet)G.clone());
		}

		/** Returns a textual representation of this Streett pair. */
		@Override
		public String toString() {
			return "(" + R + "->" + G + ")";
		}
	}

	/** Make a copy of the acceptance condition. */
	public AcceptanceStreett clone()
	{
		AcceptanceStreett result = new AcceptanceStreett();
		for (StreettPair pair : this) {
			result.add(pair.clone());
		}

		return result;
	}

	/** Returns true if the bottom strongly connected component
	 * given by bscc_states is accepting for this Streett condition,
	 * i.e., all pairs accept for bscc_states.
	 */
	@Override
	public boolean isBSCCAccepting(BitSet bscc_states)
	{
		for (StreettPair pair : this) {
			if (!pair.isBSCCAccepting(bscc_states)) {
				return false;
			}
		}
		return true;
	}


	@Override
	public void lift(LiftBitSet lifter) {
		for (StreettPair pair : this) {
			pair.R = lifter.lift(pair.R);
			pair.G = lifter.lift(pair.G);
		}
	}

	/**
	 * Get the Rabin acceptance condition that is the dual of this Streett acceptance condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Rabin condition.
	 * @return the complement Rabin acceptance condition
	 */
	public AcceptanceRabin complementToRabin()
	{
		AcceptanceRabin accRabin = new AcceptanceRabin();

		for (StreettPair accPairStreett : this) {
			BitSet L = (BitSet) accPairStreett.getG().clone();
			BitSet K = (BitSet) accPairStreett.getR().clone();
			AcceptanceRabin.RabinPair accPairRabin = new AcceptanceRabin.RabinPair(L, K);
			accRabin.add(accPairRabin);
		}
		return accRabin;
	}

	/** Complement this acceptance condition, return as AcceptanceGeneric. */
	public AcceptanceGeneric complementToGeneric()
	{
		return toAcceptanceGeneric().complementToGeneric();
	}

	@Override
	public AcceptanceOmega complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.RABIN)) {
			return complementToRabin();
		}
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
	}


	/**
	 * Returns a new Streett acceptance condition that corresponds to the conjunction
	 * of this and the other Streett acceptance condition. The StreettPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other Streett acceptance condition
	 * @return new AcceptanceStreett, conjunction of this and other
	 */
	public AcceptanceStreett and(AcceptanceStreett other)
	{
		AcceptanceStreett result = new AcceptanceStreett();
		for (StreettPair pair : this) {
			result.add((StreettPair) pair.clone());
		}
		for (StreettPair pair : other) {
			result.add((StreettPair) pair.clone());
		}
		return result;
	}

	@Override
	public AcceptanceStreettDD toAcceptanceDD(JDDVars ddRowVars)
	{
		return new AcceptanceStreettDD(this, ddRowVars);
	}

	@Override
	public AcceptanceGeneric toAcceptanceGeneric()
	{
		if (size() == 0) {
			return new AcceptanceGeneric(true);
		}
		AcceptanceGeneric genericPairs = null;
		for (StreettPair pair : this) {
			AcceptanceGeneric genericPair = pair.toAcceptanceGeneric();
			if (genericPairs == null) {
				genericPairs = genericPair;
			} else {
				genericPairs = new AcceptanceGeneric(AcceptanceGeneric.ElementType.AND, genericPairs, genericPair);
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
			StreettPair pair = get(pairIndex);
			result += pair.getSignatureForState(stateIndex,  pairIndex);
		}

		return result;
	}

	@Override
	public String getSignatureForStateHOA(int stateIndex)
	{
		String result = "";

		for (int pairIndex=0; pairIndex<size(); pairIndex++) {
			StreettPair pair = get(pairIndex);
			if (pair.getR().get(stateIndex)) {
				result += (result.isEmpty() ? "" : " ") + pairIndex*2;
			}
			if (pair.getG().get(stateIndex)) {
				result += (result.isEmpty() ? "" : " ") + (pairIndex*2+1);
			}
		}

		if (!result.isEmpty())
			result = "{"+result+"}";

		return result;
	}

	@Override
	public String toString()
	{
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
		out.println("acc-name: Streett "+size());
		out.print("Acceptance: " + (size()*2)+" ");
		if (size() == 0) {
			out.println("t");
			return;
		}

		for (int pair = 0; pair < size(); pair++) {
			if (pair > 0) out.print(" & ");
			out.print("( Fin(" + (2*pair) + ") | Inf(" + (2*pair+1) +") )");
		}
		out.println();
	}
}
