//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

import simulator.*;

public class PCTLImplies extends PCTLFormulaBinary
{
	// constructor
	
	public PCTLImplies(PCTLFormula f1, PCTLFormula f2)
	{
		super(f1, f2);
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public long toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		//Implies is just !operand1 || operand2
		long[]exprPointers = new long[2];
		exprPointers[0] = SimulatorEngine.createNot(operand1.toSimulator(sim));
		exprPointers[1] = operand2.toSimulator(sim);
		return SimulatorEngine.createOr(exprPointers);
	}

	// convert to string
	
	public String toString()
	{
		return operand1 + " => " + operand2;
	}
}

//------------------------------------------------------------------------------
