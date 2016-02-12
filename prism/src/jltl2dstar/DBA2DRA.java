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
import java.util.Iterator;
import java.util.BitSet;

import jltl2ba.APElement;
import jltl2ba.APSet;

/** @file 
 * Provides class DBA2DRA, which can convert a 
 * deterministic Büchi automaton to a deterministic Rabin
 * automaton.
 */

/**
 * Provides conversion from deterministic Büchi to 
 * deterministic Rabin.
 */
public class DBA2DRA {
	/**
	 * Convert a deterministic Büchi automaton
	 * (a nondeterministic Büchi automaton NBA, where every transition
	 * has at most one target state) to an equivalent deterministic 
	 * Rabin automaton.
	 * <p>
	 * This involves generation of the appropriate acceptance condition
	 * and making sure that the transition function is complete.
	 * </p>
	 * <p>
	 * The DBA can also be complemented on the fly 
	 * (by modifying the acceptance condition of the DRA). The resulting DRA can then be
	 * regarded as a Streett automaton of the original DBA.
	 * @param nba the NBA, the transitions have to be deterministic!
	 * @param complement complement the DBA?
	 * @return a shared_ptr to the created DRA
	 */
	public static DRA dba2dra(NBA nba, boolean complement) throws PrismException {
		APSet ap_set = nba.getAPSet();
		DRA dra = new DRA(ap_set);

		dra.acceptance().newAcceptancePair();

		for (int i = 0; i < nba.size(); i++) {
			dra.newState();

			if (complement) {
				// Final states -> U_0, all states -> L_0
				if (nba.get(i).isFinal()) {
					dra.acceptance().stateIn_U(0, i, true);
				}
				dra.acceptance().stateIn_L(0, i, true);
			} else {
				// Final states -> L_0, U_0 is empty
				if (nba.get(i).isFinal()) {
					dra.acceptance().stateIn_L(0, i, true);
				}
			}
		}

		if (nba.getStartState() != null) {
			dra.setStartState(dra.get(nba.getStartState().getName()));
		}

		DA_State sink_state = null;

		for (int i = 0; i < nba.size();	i++) {
			NBA_State nba_state = nba.get(i);
			DA_State dra_from = dra.get(i);

			for (Iterator<APElement> it = ap_set.elementIterator(); it.hasNext(); ) {
				APElement label = it.next(); 
				BitSet to = nba_state.getEdge(label);

				int to_cardinality = 0;
				if (to != null) {
					to_cardinality = to.cardinality();
				}

				DA_State dra_to = null;
				if (to == null || to_cardinality == 0) {
					// empty to -> go to sink state
					if (sink_state == null) {
						// we have to create the sink
						sink_state = dra.newState();

						// if we complement, we have to add the sink to
						// L_0 
						if (complement) {
							sink_state.acceptance().addTo_L(0);
						}	    
					}
					dra_to = sink_state;
				} else if (to_cardinality == 1) {
					int to_index = to.nextSetBit(0);

					//	  std::cerr << "to: " << to_index << std::endl;

					dra_to = dra.get(to_index);
				} else {
					// to_cardinality>1 !
					throw new PrismException("NBA is no DBA!");
				}

				dra_from.edges().put(label, dra_to);
			}
		}

		if (sink_state != null) {
			// there is a sink state
			// make true-loop from sink state to itself
			for (Iterator<APElement> it = ap_set.elementIterator(); it.hasNext(); ) {
				sink_state.edges().put(it.next(), sink_state);
			}
		}
		return dra;
	}
}
