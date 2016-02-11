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

import jltl2ba.APSet;
import jltl2ba.Alternating;
import jltl2ba.Buchi;
import jltl2ba.Generalized;
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

		SimpleLTL ltlSimple = ltl.convertForJltl2ba();
		APSet apset = ltlSimple.getAPs();

		Alternating a = new Alternating(ltlSimple, apset);
		// a.print(System.out);
		Generalized g = new Generalized(a);
		// g.print(System.out, apset);
		Buchi b = new Buchi(g);
		// b.print_spin(System.out, apset);
		NBA nba = b.toNBA(apset);
		// nba.print(System.out);
		
		// jltl2ba should never produce disjoint NBA,
		// i.e., where some states are not reachable
		// from the intial state, so we want to fail
		// later if the NBA is discovered to be disjoint:
		nba.setFailIfDisjoint(true);

		return nba;
	}
}
