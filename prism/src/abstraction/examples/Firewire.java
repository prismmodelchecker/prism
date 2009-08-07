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

public class Firewire extends PRISMAbstraction
{
	/* Parse comma-separated command-line argument with model details
	   Returns null if OK, correct format string if not */
	
	public String modelArgs(String arg)
	{
		filename = "data/firewire/impl"+arg;
		return null;
	}
	
	/* Comma-separated list of names of variables in abstraction */
	
	protected String abstractVarsString()
	{
		return "x";
	}
	
	/* Abstraction: conversion of concrete to abstract state */
	
	protected AbstractState concreteToAbstract(int vars[])
	{
		AbstractState state;
		int i;
		
		state = new AbstractState(1);
		if (isInitialConcreteState(vars)) i = 0;
		else if (isTargetConcreteState(vars)) i = 1;
		else i = 2;
		state.set(0, i);
		
		return state;
	}
	
	protected boolean isTargetConcreteState(int vars[])
	{
		return ((vars[4] == 7 && vars[9] == 8) || (vars[4] == 8 && vars[9] == 7));
	}
	
	protected boolean isInitialConcreteState(int vars[])
	{
		int i;
		for (i = 0; i < 10; i++) if (vars[i] != 0) return false;
		return true;
	}
	
	protected AbstractState getInitialAbstractState()
	{
		// not currently used?
		return null;
	}
}
