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

/** @file
 * Provides class NBA2DA, which converts an NBA to a DA
 */
import java.util.Stack;

import jltl2ba.APElement;
import jltl2ba.APElementIterator;
import jltl2ba.APSet;
import prism.PrismException;

/**
 * Convert an NBA to a DA using the specified Algorithm_t and StateMapper_t
 */
//template < typename Algorithm_t, 
//typename DA_t,
//typename StateMapper_t=StateMapper<typename Algorithm_t::result_t,
//typename Algorithm_t::state_t,
//typename DA_t::state_type> >
public class UnionNBA2DRA {
	
	/** Save detailed information on the Safra trees in the states? */
	private boolean _detailed_states;

	/** 
	 * Constructor
	 * detailedStates default = false 
	 */
	public UnionNBA2DRA(boolean detailedStates) {
		_detailed_states = detailedStates;
	}

	/**
	 * Generate a DA using the Algorithm
	 * Throws LimitReachedException if a limit is set (>0) and
	 * there are more states in the generated DA than the limit. 
	 * @param algo the algorithm 
	 * @param da_result the DA where the result is stored 
	 *        (has to have same APSet as the nba)
	 * @param limit a limit for the number of states (0 disables the limit - default).
	 */
	public void convert(DAUnionAlgorithm algo, DRA da_result, int limit, StateMapper<UnionState.Result,UnionState,DA_State> state_mapper) throws PrismException {

		APSet ap_set = da_result.getAPSet();

		if (algo.checkEmpty()) {
			da_result.constructEmpty();
			return;
		}

		algo.prepareAcceptance(da_result.acceptance());

		UnionState start = algo.getStartState();
		DA_State start_state = da_result.newState();
		start.generateAcceptance(start_state.acceptance());
		if (_detailed_states) {
			start_state.setDescription(start.toHTML());
		}

		state_mapper.add(start, start_state);
		da_result.setStartState(start_state);

		Stack<unprocessed_value> unprocessed = new Stack<unprocessed_value>();
		unprocessed.push(new unprocessed_value(start, start_state));

		while (!unprocessed.empty()) {
			unprocessed_value top = unprocessed.pop();

			UnionState cur = top.algo_state;
			DA_State from = top.da_state;
			
			for (APElementIterator it_elem = new APElementIterator(ap_set.size()); it_elem.hasNext(); ) {

				APElement elem = it_elem.next();

				UnionState.Result result = algo.delta(cur, elem);

				DA_State to = state_mapper.find(result);

				if (to == null) {
					to = da_result.newState();
					result.getState().generateAcceptance(to.acceptance());

					if (_detailed_states) {
						to.setDescription(result.getState().toHTML());
					}

					state_mapper.add(result.getState(), to);
					unprocessed.push(new unprocessed_value(result.getState(), to));
				}

				from.edges().put(elem, to);

				if (limit != 0 && da_result.size() > limit) {
					throw new PrismException("State limit reached");
				}
			}
		}

	}

	private class unprocessed_value {
		public UnionState algo_state;
		public DA_State da_state;
		
		public unprocessed_value(UnionState _algo_state, DA_State _da_state) {
			algo_state = _algo_state;
			da_state = _da_state;
		}
	}
}



