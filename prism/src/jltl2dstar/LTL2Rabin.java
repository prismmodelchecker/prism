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

public class LTL2Rabin {
	
	public static prism.DRA<BitSet> ltl2rabin(SimpleLTL ltlFormula) throws PrismException {
		SimpleLTL ltl = ltlFormula.simplify();
		return ltl2rabin(ltl, ltl.getAPs()).createPrismDRA();
	}
	
	private static DRA ltl2rabin(SimpleLTL ltl, APSet apset) throws PrismException {
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
		opt_ltl2rabin.automata = Options_LTL2DRA.AutomataType.RABIN;
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
