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

package parser;

import java.util.Vector;

import prism.PrismException;
import simulator.*;

public class PCTLProbFuture extends PCTLFormulaUnary
{
	// constructor
	
	public PCTLProbFuture(PCTLFormula f)
	{
		super(f);
	}

	/**
	 *	Convert and build simulator data structures
	 *	Note: Although the simulator supports ProbFuture operators, they are 
	 *	only supported if they belong to a top-most Prob formulae, and so are not 
	 *	handled by a toSimulator method.  Therefore, this method will only be called 
	 *	in error and hence throws an exception.
	 */
	public long toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("Unexpected error when loading PCTL formula into simulator - Future toSimulator should never be called");
	}

	// convert to string
	
	public String toString()
	{
		return "F " + operand;
	}
}
