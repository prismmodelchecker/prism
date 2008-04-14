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

import jltl2ba.APElement;
import jltl2ba.APSet;
import prism.PrismException;

/** @file
 * Provides DAUnionAlgorithm for calculating the union of two DA.
 */

/**
 * An algorithm calculating the union of two DA. Requires the existance of a UnionAcceptanceCalculator for the
 * AcceptanceCondition.
 * @param DA_t the Deterministic Automaton class.
 */

public class DAUnionAlgorithm {

	/** The first DA */
	private DRA _da_1;
	/** The second DA */
	private DRA _da_2;
	/** The result DA */
	private DRA _result_da;

	/** The acceptance calculator */
	private UnionAcceptanceCalculator _acceptance_calculator;

	/** Perform trueloop check? */
	// private boolean _trueloop_check;	
	/** Generate detailed descriptions? */
	private boolean _detailed_states;


	/** Constructor. 
	 * @param da_1 The first DA
	 * @param da_2 the second DA
	 * @param trueloop_check Check for trueloops?
	 * @param detailed_states Generate detailed descriptions of the states? */
	public DAUnionAlgorithm(DRA da_1, DRA da_2, boolean trueloop_check, boolean detailed_states) throws PrismException {
		_da_1 = da_1;
		_da_2 = da_2;
		// _trueloop_check = trueloop_check;
		_detailed_states = detailed_states;
		_acceptance_calculator = new UnionAcceptanceCalculator(da_1.acceptance(), da_2.acceptance());

		if (! (_da_1.getAPSet() == _da_2.getAPSet()) ) {
			throw new PrismException("Can't create union of DAs: APSets don't match");
		}

		APSet combined_ap = da_1.getAPSet();

		if (! _da_1.isCompact() || ! _da_2.isCompact()) {
			throw new PrismException("Can't create union of DAs: Not compact");
		}
		
		_result_da = new DRA(combined_ap);
	}

	/** Get the resulting DA 
	 * @return a shared_ptr to the resulting union DA.
	 */
	DRA getResultDA() {
		return _result_da;
	}

	/** Calculate the successor state.
	 * @param from_state The from state
	 * @param elem The edge label 
	 * @return result_t the shared_ptr of the successor state
	 */
	public UnionState.Result delta(UnionState from_state, APElement elem) throws PrismException {
		DA_State state1_to = _da_1.get(from_state.da_state_1).edges().get(elem);
		DA_State state2_to = _da_2.get(from_state.da_state_2).edges().get(elem);

		UnionState to = createState(state1_to.getName(), state2_to.getName());
		return new UnionState.Result(to);
	}


	/** Get the start state.
	 * @return a shared_ptr to the start state 
	 */
	public UnionState getStartState() throws PrismException {
		if (_da_1.getStartState() == null || _da_2.getStartState() == null) {
			throw new PrismException("DA has no start state!");
		}

		return createState(_da_1.getStartState().getName(), _da_2.getStartState().getName());
	}

	/** Prepare the acceptance condition 
	 * @param acceptance the acceptance condition in the result DA
	 */
	public void prepareAcceptance(RabinAcceptance acceptance) {
		_acceptance_calculator.prepareAcceptance(acceptance);
	}

	/** Check if the automaton is a-priori empty */
	public boolean checkEmpty() {
		return false;
	}

	/** Calculate the union of two DA. If the DAs are not compact, they are made compact.
	 * @param da_1 The first DA
	 * @param da_2 the second DA
	 * @param trueloop_check Check for trueloops?
	 * @param detailed_states Generate detailed descriptions of the states?
	 * @return shared_ptr to result DA
	 */
	public static DRA calculateUnion(DRA da_1, DRA da_2, boolean trueloop_check, boolean detailed_states) throws PrismException {
		if (!da_1.isCompact()) {
			da_1.makeCompact();
		}

		if (!da_2.isCompact()) {
			da_2.makeCompact();
		}

		DAUnionAlgorithm dua = new DAUnionAlgorithm(da_1, da_2, trueloop_check, detailed_states);
		UnionNBA2DRA generator = new UnionNBA2DRA(detailed_states);
		generator.convert(dua, dua.getResultDA(), 0, new StateMapper<UnionState.Result,UnionState,DA_State>());

		return dua.getResultDA();
	}


	/** Calculate the union of two DA, using stuttering if possible. If the DAs are not compact, they are made compact.
	 * @param da_1 The first DA
	 * @param da_2 the second DA
	 * @param stutter_information information about the symbols where stuttering is allowed
	 * @param trueloop_check Check for trueloops?
	 * @param detailed_states Generate detailed descriptions of the states? */
/*	public DRA calculateUnionStuttered(DRA da_1, DRA da_2, 
			StutterSensitivenessInformation stutter_information,
			boolean trueloop_check, boolean detailed_states) {
		if (!da_1.isCompact()) {
			da_1.makeCompact();
		}

		if (!da_2.isCompact()) {
			da_2.makeCompact();
		}

		DAUnionAlgorithm dua(da_1, da_2, trueloop_check, detailed_states);

		StutteredNBA2DA<DAUnionAlgorithm, DA_t> generator(detailed_states, stutter_information);
		generator.convert(dua, dua.getResultDA(), 0);

		return dua.getResultDA();
	}
*/	
		/** Create a UnionState 
		 * @param da_state_1
		 * @param da_state_2
		 * @return the corresponding UnionState
		 */
	private UnionState createState(int da_state_1, int da_state_2) {
		
		UnionState state = new UnionState(da_state_1, da_state_2, _acceptance_calculator);

		// Generate detailed description
		if (_detailed_states) {
			String s;

			s = "<TABLE BORDER=\"1\" CELLBORDER=\"0\"><TR><TD>";

			if (_da_1.get(da_state_1).hasDescription()) {
				s += _da_1.get(da_state_1).getDescription();
			} else {
				s += da_state_1;
			}

			s += "</TD><TD>U</TD><TD>";

			if (_da_2.get(da_state_2).hasDescription()) {
				s += _da_2.get(da_state_2).getDescription();
			} else {
				s += da_state_2;
			}

			s += "</TD></TR></TABLE>";

			state.setDescription(s);
		}
		return state;
	}
}

//TODO: trueloop again

