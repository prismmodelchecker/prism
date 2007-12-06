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

public class PCTLAnd extends PCTLFormulaNary
{
	/**
	 *	Convert and build simulator data structures
	 */
	public long toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		int n = getNumOperands();
		
		if( n < 1 )
			throw new SimulatorException("Expression \"" + toString() + "\" has zero operands");
		
		long[] exprPointers = new long[n];
		for(int i = 0; i < n; i++)
			exprPointers[i] = getOperand(i).toSimulator(sim);
		
		return SimulatorEngine.createAnd(exprPointers);
	}

	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		n = getNumOperands();
		for (i = 0; i < n-1; i++) {
			s = s + getOperand(i) + " & ";
		}
		if (n > 0) {
			s = s + getOperand(n-1);
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
