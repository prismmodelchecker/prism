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
 * Factory interface for LP solver backends.
 * Implementations are discovered at runtime via Java SPI:
 * each jar that provides a backend lists its factory class in
 * META-INF/services/solver.LPSolverFactory.
 */
public interface LPSolverFactory
{
	/** Short identifier used to select this backend, e.g. "lpsolve" or "gurobi". */
	String getId();

	/** Human-readable name, e.g. "lpsolve 5.5" or "Gurobi". */
	String getDisplayName();

	/** Create a fresh solver for an LP with the given number of variables. */
	LPSolver create(int numVars) throws PrismException;
}
