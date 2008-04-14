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

import java.io.PrintStream;
import java.util.Iterator;

import jltl2ba.APSet;

import prism.PrismException;

/**
 * A class representing a deterministic Rabin automaton.
 * <p>
 * For details on the template parameters, see class DA.
 * </p>
 * The DRA can be considered as a Streett automaton, if
 * a flag is set.
 */
public class DRA extends DA {

	/** Marker, is this DRA considered as a Streett automaton? */
	private boolean _isStreett;

	/**
	 * Constructor.
	 * @param ap_set the underlying APSet
	 */
	public DRA(APSet ap_set) {
		super(ap_set);
		_isStreett = false;
	}
	
	public static DRA newInstance(APSet ap_set) {
		return new DRA(ap_set);
	}

	private String typeID() {
		if (isStreett()) {
			return "DSA";
		} else {
			return "DRA";
		}
	}

	/** Is this DRA considered as a Streett automaton? */
	public boolean isStreett()
	{
		return _isStreett;
	}

	/** Consider this DRA as a Streett automaton. */
	public void considerAsStreett(boolean flag)
	{
		_isStreett=flag;
	}


	/**
	 * Print the DRA/DSA in v2 format to the output stream.
	 * This function can compact the automaton, which may invalidate iterators!
	 */
	public void print(PrintStream out) throws PrismException {
		if (!this.isCompact()) {
			this.makeCompact();
		}

		this.print(typeID(), out);
	}

	/**
	 * Print the DRA/DSA in DOT format to the output stream.
	 * This function can compact the automaton, which may invalidate iterators!
	 */
	// void print_dot(std::ostream& out)
	
	/**
	 * Optimizes the acceptance condition.
	 * This function may delete acceptance pairs,
	 * which can invalidate iterators.
	 */
	public void optimizeAcceptanceCondition() throws PrismException {
		
		for (Iterator<Integer> it = this.acceptance().iterator(); it.hasNext(); ) {
			Integer id = it.next();

			if (this.acceptance().getAcceptance_L(id) == null)
				continue;

			// L = L \ U
			if (this.acceptance().getAcceptance_L(id).intersects(this.acceptance().getAcceptance_U(id))) {
				this.acceptance().getAcceptance_L(id).andNot(this.acceptance().getAcceptance_U(id));
			}

			// remove if L is empty
			if (this.acceptance().getAcceptance_L(id).isEmpty()) {
				// no state is in L(id) -> remove
				this.acceptance().removeAcceptancePair(id);
			}
		}
	}


	public DRA calculateUnion(DRA other, boolean trueloop_check, boolean detailed_states) throws PrismException {
		if (this.isStreett() || other.isStreett()) {
			throw new PrismException("Can not calculate union for Streett automata");
		}

		return DAUnionAlgorithm.calculateUnion(this, other, trueloop_check, detailed_states);
	}

	//	public DRA calculateUnionStuttered(DRA other,
	//			StutterSensitivenessInformation stutter_information,
	//			boolean trueloop_check,
	//			boolean detailed_states) {
	//		if (this.isStreett() ||	other.isStreett()) {
	//			throw new PrismException("Can not calculate union for Streett automata");
	//		}
	//
	//		return DAUnionAlgorithm<DRA>.calculateUnionStuttered(this, other, stutter_information, trueloop_check, detailed_states);
	//	}
}
