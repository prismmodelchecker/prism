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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;

import prism.PrismException;
import prism.PrismNotSupportedException;
import jdd.JDDVars;

/**
 * A Generalized Rabin acceptance condition (based on BitSet state sets)
 * This is a list of GenRabinPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Generalized Rabin pair has state sets L and K_1,...,K_n and is accepting iff
 * L is not visited infinitely often and all K_j are visited infinitely often:
 *   (F G !"L") & (G F "K_1") & ... & (G F "K_n").
 *
 * The Generalized Rabin condition is accepting if at least one of the pairs is accepting.
 */
@SuppressWarnings("serial")
public class AcceptanceGenRabin
       extends ArrayList<AcceptanceGenRabin.GenRabinPair>
       implements AcceptanceOmega
{

	/**
	 * A pair in a Generalized Rabin acceptance condition, i.e., with
	 *  (F G !"L") & (G F "K_1") & ... & (G F "K_n").
	 **/
	public static class GenRabinPair {
		/** State set L (should be visited only finitely often) */
		private BitSet L;

		/** State sets K_j (should all be visited infinitely often) */
		private ArrayList<BitSet> K_list;

		/** Constructor with L and K_j state sets */
		public GenRabinPair(BitSet L, ArrayList<BitSet> K_list) {
			this.L = L;
			this.K_list = K_list;
		}

		/** Get the state set L */
		public BitSet getL()
		{
			return L;
		}

		/** Get the number of K_j sets */
		public int getNumK()
		{
			return K_list.size();
		}
		
		/** Get the state set K_j */
		public BitSet getK(int j)
		{
			return K_list.get(j);
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

			for (BitSet K_j : K_list) {
				if (!K_j.intersects(bscc_states)) {
					// there is some state in bscc_states that is
					// contained in K -> infinitely often visits to K
					return false;
				}
			}

			return true;
		}

		/** Generate signature for this Rabin pair and the given state.
		 *  If the state is a member of L, returns "-pairIndex".
		 *  If the state is a member of K, returns "+pairIndex".
		 *  @param stateIndex the state index
		 *  @param pairIndex the index of this Rabin pair
		 **/
		public String getSignatureForState(int stateIndex, int pairIndex)
		{
			// TODO: (What is the correct syntax here?)
			/*if (L.get(stateIndex)) {
				return "-"+pairIndex;
			} else if (K.get(stateIndex)) {
				return "+"+pairIndex;
			} else {
				return "";
			}*/
			return "?";
		}

		@Override
		public GenRabinPair clone()
		{
			ArrayList<BitSet> newK_list = new ArrayList<BitSet>();
			for (BitSet K_j : K_list)
				newK_list.add((BitSet) K_j.clone());
			return new GenRabinPair((BitSet)L.clone(), newK_list);
		}

		public AcceptanceGeneric toAcceptanceGeneric()
		{
			AcceptanceGeneric genericL = new AcceptanceGeneric(AcceptanceGeneric.ElementType.FIN, (BitSet) L.clone());
			if (getNumK() == 0) {
				return genericL;
			}
			AcceptanceGeneric genericKs = null;
			for (BitSet K : K_list) {
				AcceptanceGeneric genericK = new AcceptanceGeneric(AcceptanceGeneric.ElementType.INF, (BitSet) K.clone());
				if (genericKs == null) {
					genericKs = genericK;
				} else {
					genericKs = new AcceptanceGeneric(AcceptanceGeneric.ElementType.AND, genericKs, genericK);
				}
			}
			return new AcceptanceGeneric(AcceptanceGeneric.ElementType.AND, genericL, genericKs);
		}
	
		/** Returns a textual representation of this Generalized Rabin pair. */
		@Override
		public String toString() {
			String s = "(" + L;
			for (BitSet K_j : K_list)
				s += "," + K_j;
			s += ")";
			return s;
		}
	}

	/** Make a copy of the acceptance condition. */
	public AcceptanceGenRabin clone()
	{
		AcceptanceGenRabin result = new AcceptanceGenRabin();
		for (GenRabinPair pair : this) {
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
		for (GenRabinPair pair : this) {
			if (pair.isBSCCAccepting(bscc_states)) {
				return true;
			}
		}

		return false;
	}

	/** Complement this acceptance condition, return as AcceptanceGeneric. */
	public AcceptanceGeneric complementToGeneric()
	{
		AcceptanceGeneric generic = toAcceptanceGeneric();
		return generic.complementToGeneric();
	}

	@Override
	public AcceptanceOmega complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
	}

	@Override
	public void lift(LiftBitSet lifter) {
		for (GenRabinPair pair : this) {
			pair.L = lifter.lift(pair.L);
			int n = pair.K_list.size();
			for (int j = 0; j < n; j++)
				pair.K_list.set(j, lifter.lift(pair.K_list.get(j)));
		}
	}

	/**
	 * Returns a new Generalized Rabin acceptance condition that corresponds to the disjunction
	 * of this and the other Generalized Rabin acceptance condition. The GenRabinPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other GeneralizedRabin acceptance condition
	 * @return new AcceptanceGenRabin, disjunction of this and other
	 */
	public AcceptanceGenRabin or(AcceptanceGenRabin other)
	{
		AcceptanceGenRabin result = new AcceptanceGenRabin();
		for (GenRabinPair pair : this) {
			result.add((GenRabinPair) pair.clone());
		}
		for (GenRabinPair pair : other) {
			result.add((GenRabinPair) pair.clone());
		}
		return result;
	}

	@Override
	public AcceptanceGenRabinDD toAcceptanceDD(JDDVars ddRowVars)
	{
		return new AcceptanceGenRabinDD(this, ddRowVars);
	}

	@Override
	public AcceptanceGeneric toAcceptanceGeneric()
	{
		if (size() == 0) {
			return new AcceptanceGeneric(false);
		}
		AcceptanceGeneric genericPairs = null;
		for (GenRabinPair pair : this) {
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
			GenRabinPair pair = get(pairIndex);
			result += pair.getSignatureForState(stateIndex,  pairIndex);
		}

		return result;
	}

	@Override
	public String getSignatureForStateHOA(int stateIndex)
	{
		String result = "";

		int set_index = 0;
		for (GenRabinPair pair : this) {
			if (pair.getL().get(stateIndex)) {
				result += (result.isEmpty() ? "" : " ") + set_index;
			}
			set_index++;
			for (int i=0; i < pair.getNumK(); i++) {
				if (pair.getK(i).get(stateIndex)) {
					result += (result.isEmpty() ? "" : " ") + set_index;
				}
				set_index++;
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
		for (GenRabinPair pair : this) {
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
		int sets = 0;
		out.print("acc-name: generalized-Rabin "+size());
		for (GenRabinPair pair : this) {
			sets++;  // the Fin
			out.print(" "+pair.getNumK());
			sets += pair.getNumK();
		}
		out.println();
		out.print("Acceptance: " + sets);
		if (sets == 0) {
			out.println("f");
			return;
		}

		int set_index = 0;
		for (GenRabinPair pair : this) {
			if (set_index > 0) out.print(" | ");
			out.print("( Fin(" + set_index + ")");
			set_index++;
			for (int i = 0; i < pair.getNumK(); i++) {
				out.print(" & Inf(" + set_index +")");
				set_index++;
			}
			out.print(")");
		}
		out.println();
	}
}
