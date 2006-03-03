//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker, Andrew Hinton
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

import apmc.*;
import simulator.*;

public class PCTLAnd extends PCTLFormulaNary
{
	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		int r = (getOperand(0)).toApmc(apmc);
		for(int i = 1; i < getNumOperands(); i++)
			r = apmc.newBinaryOperand( apmc.AND, r, (getOperand(i)).toApmc(apmc));
		return r;
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		int n = getNumOperands();
		
		if( n < 1 )
			throw new SimulatorException("Expression \"" + toString() + "\" has zero operands");
		
		int[] exprPointers = new int[n];
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
