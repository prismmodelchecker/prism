// ==============================================================================
//
// Copyright (c) 2026-
// Authors:
// * Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//
// ------------------------------------------------------------------------------
//
// This file is part of PRISM.
//
// PRISM is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// PRISM is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with PRISM; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// ==============================================================================

package solver;

import prism.PrismException;

/**
 * LP solver factory for the built-in lpsolve backend.
 */
public class LpSolveSolverFactory implements LPSolverFactory
{
	@Override public String getId()          { return LpSolveSolver.ID; }
	@Override public String getDisplayName() { return LpSolveSolver.DISPLAY_NAME; }

	@Override
	public LPSolver create(int numVars) throws PrismException
	{
		return new LpSolveSolver(numVars);
	}
}
