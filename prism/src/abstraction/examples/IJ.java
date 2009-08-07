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

package abstraction.examples;

import abstraction.*;

public class IJ extends IJManual
{
	protected String abstractVarsString()
	{
		return "final";
	}
	
	protected AbstractState concreteToAbstract(int vars[])
	{
		AbstractState state;
		int i, j, d, count;
		
		state = new AbstractState(1);
		
		count = 0;
		for (i = 0; i < N; i++) {
			if (vars[i] == 1) count++;
		}
		
		if (count == 1) state.set(0, 0);
		//else if (count == N) state.set(0, 0);
		else state.set(0, 1);
		
		return state;
	}
	
	protected boolean isTargetConcreteState(int vars[])
	{
		int i, count = 0;
		for (i = 0; i < N; i++) if (vars[i] == 1) count ++;
		return (count == 1);
	}
	
	protected boolean isInitialConcreteState(int vars[])
	{
		int i;
		for (i = 0; i < N; i++) if (vars[i] == 0) return false;
		return true;
	}
	
	protected AbstractState getInitialAbstractState()
	{
		AbstractState init;
		
		init = new AbstractState(1);
		init.set(0, 0);
		
		return init;
	}
}
