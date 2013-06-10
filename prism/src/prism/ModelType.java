//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package prism;

public enum ModelType {

	// List of model types (ordered alphabetically)
	CTMC, CTMDP, DTMC, MDP, PTA, STPG, SMG;

	/**
	 * Get the full name, in words, of the this model type.
	 */
	public String fullName()
	{
		switch (this) {
		case CTMC:
			return "continuous-time Markov chain";
		case CTMDP:
			return "continuous-time Markov decision process";
		case DTMC:
			return "discrete-time Markov chain";
		case MDP:
			return "Markov decision process";
		case PTA:
			return "probabilistic timed automaton";
		case STPG:
			return "stochastic two-player game";
		case SMG:
			return "stochastic multi-player game";
		}
		// Should never happen
		return "";
	}

	/**
	 * Get the PRISM keyword for this model type.
	 */
	public String keyword()
	{
		switch (this) {
		case CTMC:
			return "ctmc";
		case CTMDP:
			return "ctmdp";
		case DTMC:
			return "dtmc";
		case MDP:
			return "mdp";
		case PTA:
			return "pta";
		case STPG:
			return "stpg";
		case SMG:
			return "smg";
		}
		// Should never happen
		return "";
	}

	/**
	 * Do the transitions in a choice sum to 1 for this model type?
	 * Can also use this to test whether models uses rates or probabilities.
	 */
	public boolean choicesSumToOne()
	{
		switch (this) {
		case DTMC:
		case MDP:
		case PTA:
		case STPG:
		case SMG:
			return true;
		case CTMC:
		case CTMDP:
			return false;
		}
		// Should never happen
		return true;
	}

	/**
	 * Are time delay continuous for this model type?
	 */
	public boolean continuousTime()
	{
		switch (this) {
		case DTMC:
		case MDP:
		case STPG:
		case SMG:
			return false;
		case PTA:
		case CTMC:
		case CTMDP:
			return true;
		}
		// Should never happen
		return true;
	}

	/**
	 * Does this model allow nondeterministic choices?
	 */
	public boolean nondeterministic()
	{
		switch (this) {
		case DTMC:
		case CTMC:
			return false;
		case MDP:
		case STPG:
		case SMG:
		case PTA:
		case CTMDP:
			return true;
		}
		// Should never happen
		return true;
	}

	/**
	 * Does this model have more than 1 player?
	 */
	public boolean multiplePlayers()
	{
		switch (this) {
		case DTMC:
		case CTMC:
		case MDP:
		case PTA:
		case CTMDP:
			return false;
		case STPG:
		case SMG:
			return true;
		}
		// Should never happen
		return true;
	}

	/**
	 * Does this model have probabilities or rates?
	 * @return "Probability" or "Rate"
	 */
	public String probabilityOrRate()
	{
		switch (this) {
		case CTMC:
		case CTMDP:
			return "Rate";
		default:
			return "Probability";
		}
	}

	public static ModelType parseName(String name)
	{
		if ("ctmc".equals(name))
			return CTMC;
		else if ("ctmdp".equals(name))
			return CTMDP;
		else if ("dtmc".equals(name))
			return DTMC;
		else if ("mdp".equals(name))
			return MDP;
		else if ("pta".equals(name))
			return PTA;
		else if ("stpg".equals(name))
			return STPG;
		else if ("smg".equals(name))
			return SMG;
		else
			return null;
	}
}
