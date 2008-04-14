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

/** Accessor for the acceptance signature for a state 
 *  (part of AcceptanceCondition interface)
 */
public class AcceptanceForState {

	/** Reference to the underlying RabinAcceptance */
	private RabinAcceptance _acceptance;
	/** The state index for this accessor */
	private int _state_index;

	/** Constructor */
	public AcceptanceForState(RabinAcceptance acceptance, int state_index) {
		_acceptance = acceptance;
		_state_index = state_index;
	}

	/** Add this state to L[pair_index] */
	public void addTo_L(int pair_index) {
		_acceptance.getAcceptance_L(pair_index).set(_state_index);
		_acceptance.getAcceptance_U(pair_index).set(_state_index, false);
	}

	/** Add this state to U[pair_index] */
	public void addTo_U(int pair_index) {
		_acceptance.getAcceptance_U(pair_index).set(_state_index);
		_acceptance.getAcceptance_L(pair_index).set(_state_index, false);
	}

	/** Is this state in L[pair_index] */
	public boolean isIn_L(int pair_index) {
		return _acceptance.isStateInAcceptance_L(pair_index, _state_index);
	}

	/** Is this state in U[pair_index] */
	public boolean isIn_U(int pair_index) {
		return _acceptance.isStateInAcceptance_U(pair_index, _state_index);
	}

	/** Set L and U for this state according to RabinSignature */
	public void setSignature(RabinSignature signature) {
		for (int i = 0; i < signature.size(); i++) {
			if (signature.getL().get(i)) {
				addTo_L(i);
			}
			if (signature.getU().get(i)) {
				addTo_U(i);
			}
		}
	}

	/** Get number of acceptance pairs */
	public int size() throws PrismException {
		return _acceptance.size();
	}

	/** Get the signature for this state */
	RabinSignature getSignature() throws PrismException {
		return new RabinSignature(_acceptance.getAcceptance_L_forState(_state_index),
				_acceptance.getAcceptance_U_forState(_state_index),
				_acceptance.size());
	}
}
