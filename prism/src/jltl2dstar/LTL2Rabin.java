/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 * Copyright (c) 2011- David Parker
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

import jltl2ba.APSet;
import jltl2ba.SimpleLTL;
import prism.PrismException;

import java.util.BitSet;

import prism.DA;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceType;

public class LTL2Rabin {

	@SuppressWarnings("unchecked")
	public static prism.DA<BitSet,AcceptanceRabin> ltl2rabin(SimpleLTL ltlFormula) throws PrismException
	{
		DA<BitSet, ? extends AcceptanceOmega> result;
		result = ltl2da(ltlFormula, AcceptanceType.RABIN);
		return (DA<BitSet, AcceptanceRabin>)result;
	}

	@SuppressWarnings("unchecked")
	public static prism.DA<BitSet, AcceptanceStreett> ltl2streett(SimpleLTL ltlFormula) throws PrismException
	{
		DA<BitSet, ? extends AcceptanceOmega> result;
		result = ltl2da(ltlFormula, AcceptanceType.STREETT);
		return (DA<BitSet, AcceptanceStreett>)result;
	}
	
	public static prism.DA<BitSet, ? extends AcceptanceOmega> ltl2da(SimpleLTL ltlFormula, AcceptanceType... allowedAcceptance) throws PrismException
	{
		SimpleLTL ltl = ltlFormula.simplify();

		boolean allowRabin=AcceptanceType.contains(allowedAcceptance, AcceptanceType.RABIN);
		boolean allowStreett=AcceptanceType.contains(allowedAcceptance, AcceptanceType.STREETT);

		if (allowRabin && allowStreett) {
			// currently, disable opportunistic generation of either Rabin or Streett automaton
			allowStreett = false;
		}

		return ltl2da(ltl, ltl.getAPs(), allowRabin, allowStreett).createPrismDA();
	}

	private static DRA ltl2da(SimpleLTL ltl, APSet apset, boolean allowRabin, boolean allowStreett) throws PrismException {
		DRA dra = null;
		Options_LTL2DRA opt_ltl2rabin = new Options_LTL2DRA();
		
		// boolean flag_dra2nba=false;
		boolean flag_sched_limits=false;
		// boolean flag_print_ltl_nba=false;
		boolean flag_stat_nba=false;
		double alpha=10.0;

		opt_ltl2rabin.allow_union = true;
		opt_ltl2rabin.recursive_union = true;
		opt_ltl2rabin.only_union = false;
		opt_ltl2rabin.optimizeAcceptance = true;
		opt_ltl2rabin.bisim = true;
		opt_ltl2rabin.recursive_bisim = true;
		// opt_ltl2dstar.safety=false;
		// opt_ltl2dstar.only_safety=false;
		// opt_ltl2dstar.scheck_path="";
		// stuttercheck_timekeep = true;
		// stuttercheck_print = false;
		if (allowRabin) {
			if (allowStreett)
				opt_ltl2rabin.automata = Options_LTL2DRA.AutomataType.RABIN_AND_STREETT;
			else
				opt_ltl2rabin.automata = Options_LTL2DRA.AutomataType.RABIN;
		} else if (allowStreett) {
			opt_ltl2rabin.automata = Options_LTL2DRA.AutomataType.STREETT;
		} else {
			throw new PrismException("Can not generate deterministic automata if neither Rabin nor Streett is allowed.");
		}
		opt_ltl2rabin.detailed_states = false;
		opt_ltl2rabin.verbose_scheduler = false;
		opt_ltl2rabin.opt_safra.opt_accloop = true;
		opt_ltl2rabin.opt_safra.opt_accsucc = true;
		opt_ltl2rabin.opt_safra.opt_rename = true;
		opt_ltl2rabin.opt_safra.opt_reorder = true;
		opt_ltl2rabin.opt_safra.dba_check = false;
		opt_ltl2rabin.opt_safra.stat = false;
		opt_ltl2rabin.opt_safra.union_trueloop = true;
		// opt_ltl2rabin.opt_safra.stutter = false;
		// opt_ltl2rabin.opt_safra.partial_stutter_check = false;
		// opt_ltl2rabin.opt_safra.stutter_closure = false;

		/* if (stuttercheck_timekeep) {
			StutterSensitivenessInformation::enableTimekeeping();
		}
		if (stuttercheck_print) {
			StutterSensitivenessInformation::enablePrintInfo();
		}
		*/
		
		LTL2DRA ltl2dra = new LTL2DRA(opt_ltl2rabin.opt_safra);
		Scheduler sched = new Scheduler(ltl2dra, flag_sched_limits, alpha);
		sched.flagStatNBA(flag_stat_nba);
		dra = sched.calculate(ltl, apset, opt_ltl2rabin);

		if (dra == null) {
			throw new PrismException("Couldn't generate DRA!");
		}

		if (!dra.isCompact()) {
			dra.makeCompact();
		}
		
		return dra;
	}
}
