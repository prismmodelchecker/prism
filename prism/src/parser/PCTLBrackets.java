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

import java.util.Vector;

import simulator.*;

// note: although this makes no difference to the meaning
// of the formula, it means we can keep the user's
// original bracketting for display purposes

public class PCTLBrackets extends PCTLFormulaUnary
{
	// constructors
	
	public PCTLBrackets()
	{
	}

	public PCTLBrackets(PCTLFormula f)
	{
		super(f);
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public long toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		return operand.toSimulator(sim);
	}

	// convert to string
		
	public String toString()
	{
		return "(" + operand + ")";
	}
}

//------------------------------------------------------------------------------
