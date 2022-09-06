/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

/** A state representing a union state from two DA. */
public class UnionState implements Comparable<UnionState>,NBA2DAState {
	
	public static class Result implements NBA2DAResult<UnionState> {
		private UnionState state;
		public Result(UnionState state_) {
			state = state_;
		}
		public UnionState getState() {
			return state;
		}
	}
	
	/** Index of the state from the first automaton */
	protected int da_state_1;
	/** Index of the state from the second automaton */
	protected int da_state_2;
	/** A shared_ptr with the acceptance signature of this state */
	RabinSignature signature;
	/** A shared_ptr to a string containing a detailed description of this state */
	String description;

	/** Constructor.
	 * @param da_state_1_ index of the state in the first automaton
	 * @param da_state_2_ index of the state in the second automaton
	 * @param acceptance_calculator UnionAcceptanceCalculator
	 */
	public UnionState(int da_state_1_, int da_state_2_,	UnionAcceptanceCalculator acceptance_calculator) {
		da_state_1 = da_state_1_;
		da_state_2 = da_state_2_;
		signature = acceptance_calculator.calculateAcceptance(da_state_1, da_state_2);
		description = "";
	}

	public int compareTo(UnionState other) {
		if (da_state_1 != other.da_state_1)
			return da_state_1 - other.da_state_1;
		else return da_state_2 - other.da_state_2;
	}
	
	public boolean equals(UnionState other) {
		return ((da_state_1 == other.da_state_1) && (da_state_2 == other.da_state_2));
	}
	
	public boolean equals(Object o) {
		return (o instanceof UnionState && this.equals((UnionState) o));
	}
	
	/** Copy acceptance signature for this state
	 * @param acceptance (<b>out</b>) AcceptanceForState for the state in the result automaton 
	 */
	public void generateAcceptance(AcceptanceForState acceptance) {
		acceptance.setSignature(signature);
	}

	/** Copy acceptance signature for this state
	 * @param acceptance (<b>out</b>) acceptance signature for the state in the result automaton 
	 */
	// void generateAcceptance(da_signature_t& acceptance) const {
//		acceptance=*signature;
//	}

	/** Return the acceptance acceptance signature for this state
	 * @return the acceptance signature for this state
	 */
	public RabinSignature generateAcceptance() {
		return signature;
	}

	/**
	 * Set the detailed description for this state
	 * @param description_ the description
	 */
	public void setDescription(String description_) {
		description = description_;
	}

	/** Generate a simple representation of this state 
	 * @return a string with the representation
	 */
	public String toString() {
		return "(" + da_state_1 + "," + da_state_1 + ")";
	}
	
    /** Return the detailed description 
     * @return the detailed description
     */
    public String toHTML() {
        return description;
    }

	/** Calculates the hash for the union state. 
	 * @param hash the HashFunction functor used for the calculation
	 */
	// template <class HashFunction>
	// void hashCode(HashFunction& hash) {
	// 		hash.hash(da_state_1);
	// 		hash.hash(da_state_2);
		// we don't have to consider the signature as there is a 
		// 1-on-1 mapping between <da_state_1, da_state_2> -> signature
	// }
    public int hashCode() {
    	return da_state_1 + da_state_2 * 31;
    }

}