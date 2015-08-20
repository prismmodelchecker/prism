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

import java.util.Vector;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.PrintStream;

import jltl2ba.MyBitSet;
import prism.PrismException;

public class RabinAcceptance implements Iterable<Integer> {

/**
 * Class storing a Rabin acceptance condition.
 * Contains a number k of pairs of MyBitSets (L_i,U_i), where the states
 * in the acceptance sets L_i and U_i are set.
 */

	/** The 3 different colors for RabinAcceptance */
	public enum RabinColor {RABIN_WHITE, RABIN_GREEN, RABIN_RED};
	
	/** The number of acceptance pairs */
	private int _acceptance_count;
		    
	/** A vector of MyBitSet* representing the L part of the acceptance pairs. */
	private Vector<MyBitSet> _acceptance_L;
	/** A vector of MyBitSet* representing the U part of the acceptance pairs. */
	private Vector<MyBitSet> _acceptance_U;

	boolean _is_compact;
	
	/**
	 * Constructor
	 * @param number_of_initial_pairs The initial numbers of pairs to allocate
	 */
	public RabinAcceptance()
	{
		_is_compact = true;
		_acceptance_L = new Vector<MyBitSet>();
		_acceptance_U = new Vector<MyBitSet>();
	}
  
	/**
	 * Check if this RabinAcceptance is compact (part of interface AcceptanceCondition).
	 * @return true iff compact
	 */
	public boolean isCompact()
	{
		return _is_compact;
	}

	/**
	 * Make this RabinAcceptance compact (part of interface AcceptanceCondition).
	 */
	public void makeCompact()
	{
		if (isCompact()) {
			return;
		}

		// Compress Acceptance-Pairs 
		int pair_to = 0;
		for (int pair_from = 0; pair_from < _acceptance_L.size(); pair_from++) {
			if (_acceptance_L.get(pair_from) != null) {
				if (pair_from == pair_to) {
					// nothing to do
				} else {
					_acceptance_L.set(pair_to, _acceptance_L.get(pair_from));
					_acceptance_U.set(pair_to, _acceptance_U.get(pair_from));
				}
				pair_to++;
			}
		}
   
		int new_acceptance_count=pair_to;
    
		_acceptance_L.setSize(new_acceptance_count);
		_acceptance_U.setSize(new_acceptance_count);

		_is_compact = true;
	}

	/** Update the acceptance condition upon renaming of states acording
	 *  to the mapping (part of AcceptanceCondition interface).
	 *  Assumes that states can only get a lower name.
	 * @param mapping vector with mapping a[i] -> j
	 */
	public void moveStates(Vector<Integer> mapping) throws PrismException
	{
		if (!isCompact()) {
			makeCompact();
		}

		for (int i = 0; i < size(); i++) {
			move_acceptance_bits(_acceptance_L.get(i), mapping);
			move_acceptance_bits(_acceptance_U.get(i), mapping);
		}
	}

	/**
	 * Print the Acceptance-Pairs: header (part of interface AcceptanceCondition).
	 * @param out the output stream.
	 */
	public void outputAcceptanceHeader(PrintStream out) throws PrismException {
		out.println("Acceptance-Pairs: " + size());
	}

	/**
	 * Print the Acceptance: header in the HOA format
	 * @param out the output stream.
	 */
	public void outputAcceptanceHeaderHOA(PrintStream out) throws PrismException
	{
		out.println("acc-name: Rabin "+size());
		out.print("Acceptance: " + (size()*2)+" ");
		if (size() == 0) {
			out.println("f");
			return;
		}

		for (int pair = 0; pair < size(); pair++) {
			if (pair > 0) out.print(" | ");
			out.print("( Fin(" + (2*pair) + ") & Inf(" + (2*pair+1) +") )");
		}
		out.println();
	}

	/**
	 * Print the Acc-Sig: line for a state (part of interface AcceptanceCondition).
	 * @param out the output stream.
	 * @param state_index the state
	 */
	public void outputAcceptanceForState(PrintStream out, int state_index) throws PrismException {
		out.print("Acc-Sig:");
		for (int pair_index = 0; pair_index < size(); pair_index++) {
			if (isStateInAcceptance_L(pair_index, state_index)) {
				out.print(" +" + pair_index);
			}
			if (isStateInAcceptance_U(pair_index, state_index)) {
				out.print(" -" + pair_index);
			}
		}
		out.println();
	}

	/**
	 * Print the acceptance signature for a state (HOA format)
	 * @param out the output stream.
	 * @param state_index the state
	 */
	public void outputAcceptanceForStateHOA(PrintStream out, int state_index) throws PrismException
	{
		String signature = "";
		for (int pair_index = 0; pair_index < size(); pair_index++) {
			if (isStateInAcceptance_L(pair_index, state_index)) {
				signature += (!signature.isEmpty() ? " " :"")+(pair_index*2+1);
			}
			if (isStateInAcceptance_U(pair_index, state_index)) {
				signature += (!signature.isEmpty() ? " " :"")+(pair_index*2);
			}
		}

		if (!signature.isEmpty()) {
			out.println("{" + signature + "}");
		}
	}

	/**
	 * Add a state (part of interface AcceptanceCondition).
	 * @param state_index the index of the added state.
	 */
	public void addState(int state_index) {
		// TODO: Assert that state_index > highest set bit
		;
	}

	// ---- Rabin/Streett acceptance specific

	/**
	 * Creates a new acceptance pair.
	 * @return the index of the new acceptance pair.
	 */
	public int newAcceptancePair() {
		MyBitSet l = new MyBitSet();
		MyBitSet u = new MyBitSet();

		_acceptance_L.add(l);
		_acceptance_U.add(u);

		_acceptance_count++;  
		return _acceptance_L.size()-1;
	}

	/**
	 * Creates count new acceptance pairs.
	 * @return the index of the first new acceptance pair.
	 */
	public int newAcceptancePairs(int count) {
		int rv = _acceptance_L.size();

		for (int i = 0; i < count; i++) {
			newAcceptancePair();
		}
		return rv;
	}

	/**
	 * Delete an acceptance pair.
	 */
	public void removeAcceptancePair(int pair_index) {
		if (_acceptance_L.get(pair_index) != null) {
			_acceptance_count--;
		}

		_acceptance_L.set(pair_index, null);
		_acceptance_U.set(pair_index, null);

		_is_compact = false;
	}

	/**
	 * Get a reference to the MyBitSet representing L[pair_index], 
	 * allowing changes to this set.
	 */
	public MyBitSet getAcceptance_L(int pair_index) {
		return _acceptance_L.get(pair_index);
	}

	/**
	 * Get a reference to the MyBitSet representing U[pair_index], 
	 * allowing changes to this set.
	 */
	public MyBitSet getAcceptance_U(int pair_index) {
		return _acceptance_U.get(pair_index);
	}


	/**
	 * Get the L part of the acceptance signature for a state (changes to the
	 * MyBitSet do not affect the automaton).
	 */
	public MyBitSet getAcceptance_L_forState(int state_index) {
		return getMyBitSetForState(state_index, _acceptance_L);
	}

	/**
	 * Get the U part of the acceptance signature for a state (changes to the
	 * MyBitSet do not affect the automaton).
	 */
	public MyBitSet getAcceptance_U_forState(int state_index) {
		return getMyBitSetForState(state_index, _acceptance_U);
	}

	/** Is a certain state in L[pair_index]? */
	public boolean isStateInAcceptance_L(int pair_index, int state_index) {
		return _acceptance_L.get(pair_index).get(state_index);
	}

	/** Is a certain state in U[pair_index]? */
	public boolean isStateInAcceptance_U(int pair_index, int state_index) {
		return _acceptance_U.get(pair_index).get(state_index);
	}

	/** Set L[pair_index] for this state to value. */
	void stateIn_L(int pair_index, int state_index,	boolean value) {
		getAcceptance_L(pair_index).set(state_index,value);
	}

	/** Set U[pair_index] for this state to value. */
	void stateIn_U(int pair_index, int state_index, boolean value) {
		getAcceptance_U(pair_index).set(state_index,value);
	}

	/** Get the number of acceptance pairs. 
	 *  Requires the acceptance pairs to be compact. */
	public int size() throws PrismException {
		if (!isCompact()) {
			throw new PrismException("Can't give acceptance pair count for uncompacted condition");
		}
		return _acceptance_L.size();
	}

	/** Calculate the MyBitSet for a state from the acceptance pairs, store
	 *  result in result.
	 *  @param state_index the state
	 *  @param acc the MyBitSetVector (either _L or _U)
	 *  @param result the MyBitSet where the results are stored, has to be clear 
	 *                at the beginning!
	 */
	private MyBitSet getMyBitSetForState(int state_index, Vector<MyBitSet> acc) {
		
		MyBitSet result = new MyBitSet(acc.size());

		for (int i = 0; i < acc.size(); i++) {
			if (acc.get(i) != null) {
				if (acc.get(i).get(state_index)) {
					result.set(i);
				}
			}
		}
		return result;
	}

	/** 
	 * Move the bits set in acc to the places specified by mapping.
	 */
	private void move_acceptance_bits(MyBitSet acc, Vector<Integer> mapping) throws PrismException {
		
		int i = acc.nextSetBit(0);
		while (i != -1) {
			int j = mapping.get(i);
			// :: j is always <= i
			if (j > i) {
				throw new PrismException("Wrong mapping in move_acceptance_bits");
			}

			if (i == j) {
				// do nothing
			} else {
				// move bit from i->j
				acc.set(j);
				acc.clear(i);
			}
			i = acc.nextSetBit(i + 1);
		}
	}
	
	public Iterator<Integer> iterator() {
		return new AcceptancePairIterator(_acceptance_L);
	}
	
	public static class AcceptancePairIterator implements Iterator<Integer> {
		private Vector<MyBitSet> _acceptance_vector;
		private int index;
		
		public AcceptancePairIterator(Vector<MyBitSet> acceptance_vector) {
			_acceptance_vector = acceptance_vector;
			index = 0;
		}
		
		public boolean hasNext() {
			return (index < _acceptance_vector.size());
		}
		
		public Integer next() throws NoSuchElementException {
			if (hasNext()) {
				Integer rv = new Integer(index);
				increment();
				return rv;
			}
			else throw new NoSuchElementException();
		}
			
		private void increment() {
			index++;
			while ((index < _acceptance_vector.size()) && (_acceptance_vector.get(index) == null))
				index++;
		}
		
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
	}
}
