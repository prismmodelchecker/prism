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
import prism.PrismException;

/**
 * Convert an NBA to a DRA using Safra's algorithm
 */
public class NBA2DRA {

	/** The options */
	private Options_Safra _options;
	/** Save detailed information on the Safra trees in the states? */
	private boolean _detailed_states;

	// stuttering
	// private StutterSensitivenessInformation _stutter_information;


	/** Constructor */
	public NBA2DRA() {
		_detailed_states = false;
	}

	/** Constructor.
	 * @param options Options_Safra specifying whether to stutter, etc...
	 * @param detailedStates generate detailed descriptions for the states?
	 * @param stutter_information Information about the symbols that may be stuttered
	 */
	public NBA2DRA(Options_Safra options, boolean detailedStates) {
//	public NBA2DRA(Options_Safra options, boolean detailedStates, StutterSensitivenessInformation stutter_information) {
		
		_options = options;
		_detailed_states = detailedStates;
	// _stutter_information = stutter_information;
	}

	/**
	 * Convert an NBA to an DRA (having APElements as edge labels).
	 * Throws LimitReachedException if a limit is set (>0) and
	 * there are more states in the generated DRA than the limit. 
	 * @param nba the NBA
	 * @param dra_result the DRA where the result is stored 
	 *        (has to have same APSet as the nba)
	 * @param limit a limit for the number of states (0 disables the limit).
	 */
	public DRA convert(NBA nba, int limit) throws PrismException {

		if (nba.size() == 0 || nba.getStartState() == null ) {
			// the NBA is empty -> construct DRA that is empty

			APSet ap_set = nba.getAPSet();
			DRA dra_result = new DRA(ap_set);

			dra_result.constructEmpty();
			return dra_result;
		}
		// nba.print(System.out);

		if (_options.dba_check && nba.isDeterministic()) {
			return DBA2DRA.dba2dra(nba, false);
		}

		/*		if (_options.stutter_closure) {
			if (_stutter_information != null &&	!_stutter_information.isCompletelyInsensitive()) {
				System.err.println("WARNING: NBA might not be 100% stutter insensitive, applying stutter closure can create invalid results!");
			}

			boost::shared_ptr<NBA_t> nba_closed=
				NBAStutterClosure::stutter_closure(nba);

			if (can_stutter()) {      
				convert_safra_stuttered(*nba_closed, dra_result, limit);
				return;
			}

			convert_safra(*nba_closed,dra_result,limit);
			return;
		}


		if (can_stutter()) {      
			convert_safra_stuttered(nba, dra_result, limit);
			return;
		}
		 */
		return convert_safra(nba, limit);
	}

	/** 
	 * Is stuttering allowed?
	 */
/*	public boolean can_stutter() {
		if (_stutter_information == null) {
			return false;
		}

		if (_options.stutter && _stutter_information.isCompletelyInsensitive()) {
			return true;
		}

		if (_options.stutter && _stutter_information.isPartiallyInsensitive()) {
			return true;
		}

		return false;
	}
*/

	/**
	 * Provides CandidateMatcher for SafraTrees
	 */
	/*	class SafraTreeCandidateMatcher {
		public:
			static bool isMatch(const SafraTreeTemplate_ptr temp, const SafraTree_ptr tree) {
		return temp->matches(*tree);
	};

	static bool abstract_equal_to(const SafraTree_ptr t1, const SafraTree_ptr t2) {
		return t1->structural_equal_to(*t2);
	}

	static bool abstract_less_than(const SafraTree_ptr t1, const SafraTree_ptr t2) {
		return t1->structural_less_than(*t2);
	}

	template <typename HashFunction>
	static void abstract_hash_code(HashFunction& hash, SafraTree_ptr t) {
		t->hashCode(hash, true);
	}
	};
	 */


	/**
	 * Convert the NBA to a DRA using Safra's algorithm
	 * @param nba the NBA
	 * @param dra_result the result DRA
	 * @param limit limit for the size of the DRA
	 */
	public DRA convert_safra(NBA nba, int limit) throws PrismException {

		SafrasAlgorithm safras_algo = new SafrasAlgorithm(nba, _options);
		
		APSet ap_set = nba.getAPSet();
		DRA dra_result = new DRA(ap_set);

		if (!_options.opt_rename) {
			SafraNBA2DRA nba2da = new SafraNBA2DRA(_detailed_states);
			nba2da.convert(safras_algo, dra_result, limit, new StateMapper<SafraTreeTemplate,SafraTree,DA_State>());
			return dra_result;
		} else {
			SafraNBA2DRA nba2da_fuzzy = new SafraNBA2DRA(_detailed_states);
			nba2da_fuzzy.convert(safras_algo, dra_result, limit, new StateMapperFuzzy<SafraTreeCandidateMatcher>());
			return dra_result;
		}
	}


	/**
	 * Convert the NBA to a DRA using Safra's algorithm, using stuttering
	 * @param nba the NBA
	 * @param dra_result the result DRA
	 * @param limit limit for the size of the DRA
	 */  
/*	public DRA convert_safra_stuttered(NBA nba, unsigned int limit) {
		StutteredNBA2DA<safra_t, DRA_t> nba2dra_stuttered = new StutteredNBA2DA(_detailed_states, _stutter_information);
		return nba2dra_stuttered.convert(safras_algo, limit);
	}
*/
}
