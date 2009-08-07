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

public class IJManual extends PRISMAbstraction
{
	protected int N;

	/*
	 * Parse comma-separated command-line argument with model details Returns null if OK, correct format string if not
	 */

	public String modelArgs(String arg)
	{
		N = Integer.parseInt(arg);
		filename =  "data/israeli-jalfon/ij"+arg;
		return null;
	}

	/* Comma-separated list of names of variables in abstraction */

	protected String abstractVarsString()
	{
		String s = "";
		for (int i = 0; i < N; i++) {
			if (i > 0)
				s += ",";
			s += "d" + i;
		}
		return s;
	}

	/* Abstraction: conversion of concrete to abstract state */

	protected AbstractState concreteToAbstract(int vars[])
	{
		AbstractState state;
		int i, j, d, first, tmp[];

		state = new AbstractState(N);

		// Temp storage for abstract state
		tmp = new int[N];
		for (i = 0; i < N; i++)
			tmp[i] = 0;

		// Find first token
		first = -1;
		for (i = 0; i < N; i++)
			if (vars[i] == 1) {
				first = i;
				break;
			}

		// Count distances
		if (first > -1) {
			d = 0; // current distance
			for (i = 0; i < N; i++) {
				j = (i + first + 1) % N;
				if (vars[j] == 1) {
					tmp[d]++;
					d = 0;
				} else
					d++;
			}
		}

		// Build/return abstract state
		for (i = 0; i < N; i++)
			state.set(i, tmp[i]);
		return state;
	}

	protected boolean isTargetConcreteState(int vars[])
	{
		int i, count = 0;
		for (i = 0; i < N; i++)
			if (vars[i] == 1)
				count++;
		return (count == 1);
	}

	protected boolean isInitialConcreteState(int vars[])
	{
		int i;
		for (i = 0; i < N; i++)
			if (vars[i] == 0)
				return false;
		return true;
	}

	protected AbstractState getInitialAbstractState()
	{
		AbstractState init;
		int i;

		init = new AbstractState(N);
		init.set(0, N);
		for (i = 1; i < N; i++)
			init.set(i, 0);

		return init;
	}
}
