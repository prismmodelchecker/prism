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

/**
 * Options for the LTL2DRA scheduler.
 */
public class Options_LTL2DRA implements Cloneable {

	/** Constructor */
	public Options_LTL2DRA() {
		// Defaults...
		allow_union=true;
		recursive_union=true;
		optimizeAcceptance=true;
		bisim=false;
		recursive_bisim=true;
		// safety=false;

		automata=AutomataType.RABIN;
		only_union=false;
		// only_safety=false;

		detailed_states=false;
		verbose_scheduler=false;
		opt_safra=new Options_Safra();
		// scheck_path="";
	}
	
	public Options_LTL2DRA clone()
	{
		Options_LTL2DRA rv = new Options_LTL2DRA();
		rv.allow_union = allow_union;
		rv.recursive_union = recursive_union;
		rv.optimizeAcceptance = optimizeAcceptance;
		rv.bisim = bisim;
		rv.recursive_bisim = recursive_bisim;
		// rv.safety = false;

		rv.automata=automata;
		rv.only_union = only_union;
		// rv.only_safety = false;

		rv.detailed_states = detailed_states;
		rv.verbose_scheduler = verbose_scheduler;
		rv.opt_safra = opt_safra.clone(); 
		// rv.scheck_path=scheck_path;
		
		return rv;
	}

	/** Disable all options */
	public void allFalse() {
		allow_union
		=recursive_union

		// =safety

		=optimizeAcceptance
		=bisim
		=recursive_bisim

		=only_union
		// =only_safety
		=detailed_states
		=verbose_scheduler
		=false;
	}

	/** Change options for next level of recursion */
	public void recursion() {
		allow_union = allow_union && recursive_union;
		only_union = false;

		bisim = bisim && recursive_bisim;
	}

	/** Safra Options */
	public Options_Safra opt_safra;

	/** Allow union construction */
	public boolean allow_union;

	/** Allow union construction on next levels */
	public boolean recursive_union;

	/** Allow using scheck for (co-)safety LTL formulas */
	// public boolean safety;

	/** Allow optimization of acceptance conditions */
	public boolean optimizeAcceptance;

	/** Allow bisimulation */
	public boolean bisim;

	/** Allow bisimulation on all levels. */
	public boolean recursive_bisim;

	/** Provide detailed internal description in the states */
	public boolean detailed_states;

	/** Type of the automata that should be generated */
	public static enum AutomataType {STREETT, RABIN, RABIN_AND_STREETT};
	public AutomataType automata;

	/** Use union construction exclusively */
	public boolean only_union;

	/** Debug information from the scheduler */
	public boolean verbose_scheduler;

	/** Use scheck exclusively */
	// public boolean only_safety;
	/** Path to scheck */
	// String scheck_path;
}

