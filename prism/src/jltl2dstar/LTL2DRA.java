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

import jltl2ba.APSet;
import jltl2ba.SimpleLTL;
import jltl2ba.SimpleLTL.LTLType;
import prism.PrismException;

public class LTL2DRA {

	private Options_Safra _safra_opt;

	public LTL2DRA(Options_Safra safra_opt) {
		_safra_opt = safra_opt;
	}

	/**
	 * Convert an LTL formula to a DRA.
	 * @param ltl the LTL formula
	 * @param options which operators are allowed
	 * @return a shared_ptr to the DRA
	 */
	private DRA LTLtoDRA_rec(SimpleLTL ltl, APSet apset, Options_LTL2DRA options) throws PrismException
	{
		SimpleLTL ltl_pnf = ((SimpleLTL)ltl.clone()).simplify();
		
		if (options.allow_union && ltl_pnf.kind == LTLType.OR) {
			SimpleLTL ltl_left = ltl_pnf.left;
			SimpleLTL ltl_right = ltl_pnf.right;

			Options_LTL2DRA rec_opt = options.clone();
			rec_opt.recursion();

			DRA dra_left = LTLtoDRA_rec(ltl_left, apset, rec_opt);
			DRA dra_right = LTLtoDRA_rec(ltl_right, apset, rec_opt);

			return dra_left.calculateUnion(dra_right, options.opt_safra.union_trueloop, options.detailed_states);
		}

		/*  if (options.safety) {
	    LTLSafetyAutomata lsa;

	    DRA_ptr safety_dra(lsa.ltl2dra<DRA_t>(ltl,
						  options.scheck_path));

	    if (safety_dra.get()!=0) {
	      return safety_dra;
	    }
	  	}
		 */  
		NBA nba = ltl_pnf.toNBA(apset);

		if (nba == null) {
			throw new PrismException("Couldn't create NBA from LTL formula");
		}

		NBA2DRA nba2dra = new NBA2DRA(options.opt_safra, false);

		DRA dra = nba2dra.convert(nba, 0);

		if (options.optimizeAcceptance) {
			dra.optimizeAcceptanceCondition();
		}

		if (options.bisim) {
			DRAOptimizations dra_optimizer = new DRAOptimizations();
			dra = dra_optimizer.optimizeBisimulation(dra, false, false, false);
		}

		return dra;

	}

	/* public DRA LTLtoDRA(SimpleLTL ltl, Options_LTL2DRA options) throws PrismException {
		return LTLtoDRA_rec(ltl, ltl.getAPs(), options);
	} */
	
	public DRA LTLtoDRA(SimpleLTL ltl, APSet apset, Options_LTL2DRA options) throws PrismException {
		return LTLtoDRA_rec(ltl, apset, options);
	}



	/**
	 * Convert an NBA to a DRA using Safra's algorithm.
	 * If limit is specified (>0), the conversion is 
	 * aborted with LimitReachedException when the number of 
	 * states exceeds the limit.
	 * @param nba the formula
	 * @param limit a limit on the number of states (0 for no limit)
	 * @param detailedStates save detailed interal information (Safra trees) 
	 *                       in the generated states
	 * @param stutter_information Information about the symbols that can be stuttered
	 * @return a shared_ptr to the created DRA
	 */
	// DRA nba2dra(NBA nba, int limit, boolean detailedStates, StutterSensitivenessInformation::ptr stutter_information) {
	DRA nba2dra(NBA nba, int limit, boolean detailedStates) throws PrismException {

		DRA dra = null;
		NBA2DRA nba2dra = new NBA2DRA(_safra_opt, detailedStates);

		try {
			dra = nba2dra.convert(nba, limit);
		} catch (PrismException e) {
			dra = null;
			// rethrow to notify caller
			throw e;
		}
		return dra;  
	}


	/** Get the options for Safra's algorithm */
	public Options_Safra getOptions() {
		return _safra_opt;
	}

}
