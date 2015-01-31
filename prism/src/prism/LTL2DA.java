//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.BitSet;

import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceType;
import jltl2dstar.LTL2Rabin;
import parser.Values;
import parser.ast.Expression;

/**
 * Infrastructure for constructing deterministic automata for LTL formulas.
 */
public class LTL2DA extends PrismComponent
{

	public LTL2DA(PrismComponent parent) throws PrismException {
		super(parent);
	}

	/**
	 * Convert an LTL formula into a deterministic Rabin automaton.
	 * The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 * @param ltl the formula
	 * @param constantValues the values of constants, may be {@code null}
	 */
	@SuppressWarnings("unchecked")
	public DA<BitSet,AcceptanceRabin> convertLTLFormulaToDRA(Expression ltl, Values constantValues) throws PrismException
	{
		return (DA<BitSet, AcceptanceRabin>) convertLTLFormulaToDA(ltl, constantValues, AcceptanceType.RABIN);
	}

	/**
	 * Convert an LTL formula into a deterministic automaton.
	 * The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 * @param ltl the formula
	 * @param constantValues the values of constants, may be {@code null}
	 * @param allowedAcceptance the AcceptanceTypes that are allowed to be returned
	 */
	public DA<BitSet,? extends AcceptanceOmega> convertLTLFormulaToDA(Expression ltl, Values constants, AcceptanceType... allowedAcceptance) throws PrismException
	{
		DA<BitSet, ? extends AcceptanceOmega> result = null;
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.RABIN)) {
			// If we may construct a Rabin automaton, check the library first
			try {
				result = LTL2RabinLibrary.getDRAforLTL(ltl, constants);
				if (result != null) {
					getLog().println("Taking deterministic Rabin automaton from library...");
				}
			} catch (Exception e) {
				getLog().println("Warning: Exception during attempt to construct DRA using the LTL2RabinLibrary:");
				getLog().println(" "+e.getMessage());
			}
		}

		// TODO (JK): support generation of DSA for simple path formula with time bound
		if (result == null && !Expression.containsTemporalTimeBounds(ltl)) {
			// use jltl2dstar LTL2DA
			result = LTL2Rabin.ltl2da(ltl.convertForJltl2ba(), allowedAcceptance);
		}

		if (result == null) {
			throw new PrismException("Could not convert LTL formula to deterministic automaton");
		}

		result = DASimplifyAcceptance.simplifyAcceptance(result, allowedAcceptance);

		return result;
	}
}
