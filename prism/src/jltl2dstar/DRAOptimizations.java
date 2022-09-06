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
 * Provides optimizations on complete DRAs, notably quotienting using bisimulation.
 */
import java.util.*;
import java.io.PrintStream;

import jltl2ba.APElement;
import jltl2ba.APElementIterator;
import jltl2ba.MyBitSet;
import prism.PrismException;

/**
 * Provides optimizations on complete DRAs, notably quotienting using bisimulation.
 */
public class DRAOptimizations {

	/** type of a color */
	// typedef unsigned int color_t;
	/** type of vector state indizes */
	// typedef std::vector<unsigned int> state_vector;

	/** Helper class, storing a coloring of the states */
	public static class Coloring {

		/** The number of colors */
		private int _nr_of_colors;

		/** mapping state_id -> color */
		private Vector<Integer> _coloring;

		/** Keep detailed information of the equivalence classes? */
		private boolean _detailed;

		/** 
		 * mapping from color 
		 * -> the state ids which are colored alike
		 * only used when _detailed=true */
		private Vector<MyBitSet> _color2states;

		/** 
		 * mapping from color -> one representative state
		 */
		private Vector<Integer> _color2state;

		/** 
		 * Constructor, get initial size of the coloring from DRA.
		 * @param dra the DRA
		 * @param detailed Keep detailed information on the equivalence classes? (default: false)
		 */
		public Coloring(DRA dra, boolean detailed) {
			_nr_of_colors = 0;
			_detailed = detailed; 
			_coloring = new Vector<Integer>(dra.size());
			_coloring.setSize(dra.size());
			_color2state = new Vector<Integer>();
			if (_detailed) {
				_color2states = new Vector<MyBitSet>();
			} else {
				_color2states = null;
			}
		}

		/** 
		 * Constructor, explicitly set initial size of the coloring
		 * @param size the initial size
		 * @param detailed Keep detailed information on the equivalence classes? (default: false)
		 */
		public Coloring(int size, boolean detailed) {
			_nr_of_colors = 0;
			_detailed = detailed;
			_coloring = new Vector<Integer>(size);
			_coloring.setSize(size);
			_color2state = new Vector<Integer>();

			if (_detailed) {
				_color2states = new Vector<MyBitSet>();
			} else {
				_color2states = null;
			}
		}

		/** Reset (clear) coloring. */
		public void reset() {_nr_of_colors = 0;}

		/** Get the flag 'detailed' */
		public boolean getFlagDetailed() {return _detailed;}

		/** Returns the size (number of states) of this coloring. */
		public int size() {return _coloring.size();}

		/** 
		 * Create a new color
		 * @return the newly created color
		 */
		public int newColor() {
			_nr_of_colors++;

			_color2state.setSize(_nr_of_colors);

			if (_detailed) {
				_color2states.setSize(_nr_of_colors);
				_color2states.set(_nr_of_colors - 1, new MyBitSet());
			}

			return _nr_of_colors - 1;
		}

		/** Return the current (last created) color */
		public int currentColor() {
			assert(_nr_of_colors > 0);
			return _nr_of_colors - 1;
		}

		/** Return the number of colors */
		public int countColors() {
			return _nr_of_colors;
		}

		/** Set the color of a state */
		public void setColor(int state, int color) {
			assert(color < _nr_of_colors);

			_coloring.set(state, new Integer(color));
			_color2state.set(color, new Integer(state));

			if (_detailed) {
				_color2states.get(color).set(state);
			}
		}

		/** Get the color for a state */
		public int state2color(int state) {
			return _coloring.get(state);
		}

		/**
		 *Get one representative state for the equivalence class with the 
		 * specified color. 
		 */
		public int color2state(int color) {
			assert(color < _nr_of_colors);
			return _color2state.get(color);
		}

		/** 
		 * Get the state indizes (in a BitSet) that have the specified color. 
		 * Can only be called, when the 'detailed' flag is activated in the
		 * constructor.
		 */
		public MyBitSet color2states(int color) {
			assert(color < _nr_of_colors);
			assert(_detailed && _color2states != null);
			return _color2states.get(color);
		}

		/** Print the coloring */
		public void print(PrintStream out) {
			for (int i = 0; i < this.size(); i++) {
				out.println("color[" + i + "] = " + this.state2color(i));
			}
		}
	}

	/** 
	 * Functor, provides a 'less-than' Comparator 
	 * for the states of the DRA, using the color of 
	 * the states themself and the colors of the
	 * to-states of the edges.
	 */
	public static class ColoredStateComparator implements Comparator<Integer> {

		/** The coloring */
		private Coloring _coloring;
		/** The DRA */
		private DRA _dra;

		/** Constructor */
		public ColoredStateComparator(Coloring coloring, DRA dra) {
			_coloring = coloring;
			_dra = dra;
		}
		
		/**
		 * Compares two states 'less-than' using the
		 * coloring, uses the bisimulation
		 * equivalence relation to determine
		 * equality.
		 */
		public int compare(Integer state_x, Integer state_y) {
			int cx = _coloring.state2color(state_x);
			int cy = _coloring.state2color(state_y);

			if (cx < cy) {
				return -1;
			} else if (cx > cy) {
				return 1;
			}

			for (APElementIterator it = new APElementIterator(_dra.getAPSize()); it.hasNext(); ) {
				APElement label = it.next();
				DA_State to_x =	_dra.get(state_x).edges().get(label);
				DA_State to_y =	_dra.get(state_y).edges().get(label);

				int ctx = _coloring.state2color(to_x.getName());
				int cty = _coloring.state2color(to_y.getName());

				if (ctx < cty) {
					return -1;
				} else if (ctx > cty) {
					return 1;
				}
			}

			// we get here only if x and y are equal with this
			// coloring -> return false
			return 0;
		}
	}

	/** Type of an acceptance signature */
	// public typedef std::pair<BitSet*, BitSet*> acceptance_signature_t;
	public static class AcceptanceSignature {
		public MyBitSet l;
		public MyBitSet u;
		
		public AcceptanceSignature() {;}
	}
	
	/** 
	 * A container that stores (caches) the acceptance signatures of
	 * all the states in a DRA.
	 */
	public static class AcceptanceSignatureContainer {
		
		/** Storage for the acceptance signatures */
		private Vector<AcceptanceSignature> _acceptancesig_vector;

		/** 
		 * Constructor, fills the container with the acceptance signatures of the states.
		 * @param dra the DRA
		 */
		public AcceptanceSignatureContainer(DRA dra) {
			_acceptancesig_vector = new Vector<AcceptanceSignature>(dra.size());

			for (int i = 0; i < dra.size();	i++) {
				_acceptancesig_vector.add(new AcceptanceSignature());
				_acceptancesig_vector.get(i).l = (MyBitSet) dra.acceptance().getAcceptance_L_forState(i).clone();
				_acceptancesig_vector.get(i).u = (MyBitSet) dra.acceptance().getAcceptance_U_forState(i).clone();
			}
		}

		/** 
		 * Get the acceptance signature for state i.
		 * @param i the state index
		 */
		public AcceptanceSignature get(int i) {
			return _acceptancesig_vector.get(i);
		}
	}

	/** 
	 * Functor that compares two DRA states based on their
	 * acceptance signature.
	 */
	public static class AcceptanceSignatureComparator implements Comparator<Integer> {
		/** The acceptance signature container */
		private AcceptanceSignatureContainer _container;

		/** Constructor */
		public AcceptanceSignatureComparator(AcceptanceSignatureContainer container) {
			_container = container;
		}

		/** 
		 * Compares (less-than) two DRAState indizes based on their
		 * acceptance signature.
		 */
		public int compare(Integer x, Integer y) {
			AcceptanceSignature px = _container.get(x);
			AcceptanceSignature py = _container.get(y);

			return (px.l.compareTo(py.l) == 0 ? px.u.compareTo(py.u) : px.l.compareTo(py.l));
		}
		
		public boolean equals(Integer x, Integer y) {
			return compare(x,y) == 0;
		}
	}


	/** 
	 * Perform quotienting using bisimulation
	 * @param dra the DRA to be optimized
	 * @param printColoring print colorings on std::cerr? (default: false)
	 * @param detailedStates save detailed information on the interals in the state? (default: false)
	 * @param printStats print statistics on std::cerr? (default: false)
	 * @return shared_ptr to the quotiented DRA
	 */
	public DRA optimizeBisimulation(DRA dra, boolean printColoring, boolean detailedStates, boolean printStats) throws PrismException {
		if (!dra.isCompact()) 
			dra.makeCompact();

		Vector<Integer> states = new Vector<Integer>(dra.size());
		states.setSize(dra.size());

		for (int i = 0; i < dra.size(); i++) {
			states.set(i, new Integer(i));
		}

		AcceptanceSignatureContainer accsig_container = new AcceptanceSignatureContainer(dra);
		AcceptanceSignatureComparator accsig_comp = new AcceptanceSignatureComparator(accsig_container);


		Coloring tmpcoloring = new Coloring(dra, detailedStates);
		// generate initial coloring by running with the 
		// different acceptance signature
		Coloring coloring = generateColoring(states, tmpcoloring, accsig_comp);

		int old_size = dra.size();
		int initial_partition = coloring.countColors();

		int oldColors;
		do {
			oldColors = coloring.countColors();

			ColoredStateComparator cnc = new ColoredStateComparator(coloring, dra);      

			tmpcoloring = generateColoring(states, coloring, cnc);
			coloring = tmpcoloring;      
		} while (oldColors != coloring.countColors());

		if (printColoring) {
			coloring.print(System.err);
		}

		DRA dra_new = generateDRAfromColoring(dra, coloring, detailedStates);

		int new_size=dra_new.size();

		if (printStats) {
			System.err.println("Bisimulation: From (" + old_size + ") To (" + new_size + ") Initial: (" + initial_partition + ")");
		}
		return dra_new;
	}



	/**
	 * Generate a new coloring based on the Comparator comp 
	 * (one iteration of refinement)
	 * @param states A vector of the states
	 * @param coloring The current coloring
	 * @param comp the Comparator
	 * @return a pointer to a newly created Coloring, memory ownership
	 *         passes to the caller
	 */
	private <T extends Comparator<Integer>>Coloring generateColoring(Vector<Integer> states, Coloring coloring, T comp) {
		Integer[] statearray = (Integer[]) states.toArray(new Integer[0]);
		Arrays.sort(statearray, comp);
		Vector<Integer> sortedstates = new Vector<Integer>(Arrays.asList(statearray)); 

		Coloring result = new Coloring(coloring.size(), coloring.getFlagDetailed());

		if (sortedstates.size() == 0) {return result;}
		
		// reverse iterators
		ListIterator<Integer> current = sortedstates.listIterator(sortedstates.size());
		ListIterator<Integer> last = sortedstates.listIterator(sortedstates.size());
		
		result.setColor(current.previous(), result.newColor());

		// 0 .. n-3 n-2 n-1
		//                 ^ last
		//             ^     current
		while (current.hasPrevious()) {
			Integer curr = current.previous();
			// because states is sorted and we traverse 
			// from the end, either:
			//    *current  < *last with comp(current,last)==true
			// or *current == *last with !comp(current,last)
			
			if (comp.compare(curr, last.previous()) < 0) {
				// -> we have to start a new color
				result.setColor(curr, result.newColor());
			} else {
				// -> more of the same, we stay with this color
				result.setColor(curr, result.currentColor());
			}
		}
		return result;
	}


	/**
	 * Generate a new DRA from a coloring
	 */
	private DRA generateDRAfromColoring(DRA oldDRA, Coloring coloring, boolean detailedStates) throws PrismException {
		DRA newDRA = new DRA(oldDRA.getAPSet());

		newDRA.acceptance().newAcceptancePairs(oldDRA.acceptance().size());

		for (int color = 0;	color < coloring.countColors();	++color) {
			newDRA.newState();
		}

		int old_start_state = oldDRA.getStartState().getName();
		int start_state_color = coloring.state2color(old_start_state);

		newDRA.setStartState(newDRA.get(start_state_color));

		for (int color = 0; color < coloring.countColors();	++color) {
			DA_State new_state = newDRA.get(color);

			int old_state_representative = coloring.color2state(color);

			DA_State old_state = oldDRA.get(old_state_representative);

			if (detailedStates) {
				MyBitSet old_states = coloring.color2states(color);

				// create new description...
				if (old_states.cardinality() == 1) {
					if (old_state.hasDescription()) {
						new_state.setDescription(old_state.getDescription());
					}
				} else {
					String s = "<TABLE BORDER=\"1\" CELLBORDER=\"0\"><TR><TD>{</TD>";

					boolean first = true;
					for (Integer state : old_states) {
						if (first) {
							first = false; 
						} else {
							s += "<TD>,</TD>";
						}

						s += "<TD>";
						if (!oldDRA.get(state).hasDescription()) {
							s += state;
						} else {
							s += oldDRA.get(state).getDescription();
						}
						s += "</TD>";
					}
					s += "<TD>}</TD></TR></TABLE>";

					new_state.setDescription(s);
				}
			}

			// Create appropriate acceptance conditions
			int old_state_index = old_state.getName();
			for (int i = 0; i < oldDRA.acceptance().size();	++i) {
				if (oldDRA.acceptance().isStateInAcceptance_L(i, old_state_index)) {
					new_state.acceptance().addTo_L(i);
				}

				if (oldDRA.acceptance().isStateInAcceptance_U(i, old_state_index)) {
					new_state.acceptance().addTo_U(i);
				}
			}

			for (Map.Entry<APElement,DA_State> edge : old_state.edges().entrySet()) {

				int to_color = coloring.state2color(edge.getValue().getName());

				new_state.edges().put(edge.getKey(), newDRA.get(to_color));
			}
		}

		return newDRA;
	}
}