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

import prism.PrismException;

/**
 * Specialized UnionAcceptanceCalculator for RabinAcceptance, for calculating the acceptance in the union automaton.
 * This approach merges the acceptance signatures of the two states in the union tuple, the union is provided by
 * the semantics of the Rabin acceptance condition (There <i>exists</i> an acceptance pair ....)
 */
public class UnionAcceptanceCalculator {

	/** The acceptance condition of the first automaton. */
	private RabinAcceptance _acc_1;
	/** The acceptance condition of the second automaton. */
	private RabinAcceptance _acc_2;

	/** The size of the acceptance condition in the original automaton. */
	private int _acc_size_1, _acc_size_2;

	/**
	 * Constructor. 
	 * @param acc_1 The RabinAcceptance condition from automaton 1
	 * @param acc_2 The RabinAcceptance condition from automaton 2
	 */
	public UnionAcceptanceCalculator(RabinAcceptance acc_1,	RabinAcceptance acc_2) throws PrismException {
		_acc_1 = acc_1;
		_acc_2  = acc_2;
		_acc_size_1 = _acc_1.size();
		_acc_size_2 = _acc_2.size();
	}

	/**
	 * Prepares the acceptance condition in the result union automaton. If the two automata have k1 and k2 
	 * acceptance pairs, this function allocates k1+k2 acceptance pairs in the result automaton.
	 * @param acceptance_result The acceptance condition in the result automaton.
	 */
	public void prepareAcceptance(RabinAcceptance acceptance_result) {
		acceptance_result.newAcceptancePairs(_acc_size_1+_acc_size_2);
	}


	/**
	 * Calculate the acceptance signature for the union of two states.
	 * @param da_state_1 index of the state in the first automaton
	 * @param da_state_2 index of the state in the second automaton
	 * @return A Rabin acceptance signature
	 */
	RabinSignature calculateAcceptance(int da_state_1, int da_state_2) {
		RabinSignature signature = new RabinSignature(_acc_size_1 + _acc_size_2);

		for (int i = 0; i < _acc_size_1; i++) {
			if (_acc_1.isStateInAcceptance_L(i, da_state_1)) {
				signature.setL(i, true);
			}
			if (_acc_1.isStateInAcceptance_U(i, da_state_1)) {
				signature.setU(i, true);
			}
		}

		for (int j=0; j < _acc_size_2; j++) {
			if (_acc_2.isStateInAcceptance_L(j, da_state_2)) {
				signature.setL(j + _acc_size_1, true);
			}
			if (_acc_2.isStateInAcceptance_U(j, da_state_2)) {
				signature.setU(j + _acc_size_1, true);
			}
		}
		return signature;
	}
}
