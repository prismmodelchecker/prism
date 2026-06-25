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
 * Interface for a linear programming (LP) solver.
 *
 * Usage pattern:
 *   1. Call addVar() once per variable, in order (indices 0, 1, ...).
 *   2. Call addConstraint() for each row.
 *   3. Call solve() to solve and retrieve the solution vector.
 *   4. Call dispose() in a finally block to free native resources.
 */
public interface LPSolver
{
	/** Short identifier for this backend, e.g. "lpsolve" or "gurobi". */
	String getId();

	/** Human-readable name for logging, e.g. "lpsolve" or "Gurobi". */
	String getDisplayName();

	/**
	 * Add a variable with bounds [lb, ub] and the given objective coefficient.
	 * Variables are indexed 0, 1, ... in the order they are added.
	 * Use Double.POSITIVE_INFINITY for an unbounded upper limit.
	 */
	void addVar(double lb, double ub, double objCoeff) throws PrismException;

	/**
	 * Add a sparse linear constraint:
	 *   sum_{j=0}^{count-1} coeffs[j] * x[vars[j]]  [sense]  rhs
	 * where sense is '=', '<' (less-or-equal), or '>' (greater-or-equal).
	 * Only the first {@code count} entries of {@code coeffs} and {@code vars} are read;
	 * both arrays may be larger and are owned by the caller (safe to reuse between calls).
	 * Variable indices in {@code vars} are 0-based.
	 */
	void addConstraint(int count, double[] coeffs, int[] vars, char sense, double rhs) throws PrismException;

	/**
	 * Solve the LP.
	 * @param maximize True to maximise the objective; false to minimise.
	 * @return Solution vector, indexed 0..numVars-1 (parallel to addVar calls).
	 */
	double[] solve(boolean maximize) throws PrismException;

	/**
	 * Free any native resources held by this solver.
	 * Always call this, even if an exception was thrown — use a finally block.
	 */
	void dispose();
}
