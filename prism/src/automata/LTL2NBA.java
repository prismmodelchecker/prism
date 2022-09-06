//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
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

package automata;

import jltl2ba.SimpleLTL;
import jltl2dstar.NBA;
import parser.Values;
import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Infrastructure for constructing non-deterministic Büchi automata for LTL formulas.
 */
public class LTL2NBA extends PrismComponent
{

	public LTL2NBA(PrismComponent parent) throws PrismException {
		super(parent);
	}

	/**
	 * Convert an LTL formula into a non-deterministic Büchi automaton.
	 * The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 * @param ltl the formula
	 * @param constantValues the values of constants, may be {@code null}
	 */
	public NBA convertLTLFormulaToNBA(Expression ltl, Values constantValues) throws PrismException
	{
		if (Expression.containsTemporalTimeBounds(ltl)) {
			throw new PrismNotSupportedException("LTL with time bounds currently not supported for LTL model checking.");
		}

		// convert to jltl2ba LTL, simplify formula
		SimpleLTL ltlSimple = ltl.convertForJltl2ba().simplify();
		return ltlSimple.toNBA();
	}
}
