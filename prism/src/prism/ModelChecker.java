//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import jdd.JDDNode;
import parser.Values;
import parser.ast.*;

// interface for model checker classes

public interface ModelChecker
{
	/**
	 * Model check an expression, process, print and return the result.
	 */
	public Result check(Expression expr) throws PrismException;

	/**
	 * Model check an expression and return a vector of result values,
	 * with valid results for at least all states of interest.
	 *
	 * @param expr the expression
	 * @param statesOfInterest the states of interest.
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	public StateValues checkExpression(Expression expr, JDDNode statesOfInterest) throws PrismException;

	/**
	 * Model check expression, convert to symbolic form (if not already), return BDD.
	 * <br>
	 * Will have valid results at least for all states of interest.
	 * @param expr the expression
	 * @param statesOfInterest the states of interest
	 * <br>[ REFS: <i>result</i>, DEREFS: statesOfInterest ]
	 */
	public JDDNode checkExpressionDD(Expression expr, JDDNode statesOfInterest) throws PrismException;

	/** Get the constant values (both from the modules file and the properties file) */
	public Values getConstantValues();
}

//------------------------------------------------------------------------------
