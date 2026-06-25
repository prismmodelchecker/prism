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

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import prism.PrismException;

/**
 * LP solver backend backed by lpsolve.
 */
public class LpSolveSolver implements LPSolver
{
	public static final String ID           = "lpsolve";
	public static final String DISPLAY_NAME = "lpsolve";

	@Override public String getId()          { return ID; }
	@Override public String getDisplayName() { return DISPLAY_NAME; }

	private final int numVars;
	private final LpSolve solver;
	// 1-based objective row (index 0 unused, indices 1..numVars are variable coefficients)
	private final double[] objRow;
	// Pre-allocated buffer for converting 0-based variable indices to 1-based lpsolve column numbers
	private final int[] colno1based;
	private int varCount = 0;

	public LpSolveSolver(int numVars) throws PrismException
	{
		this.numVars = numVars;
		try {
			solver = LpSolve.makeLp(0, numVars);
			solver.setVerbose(LpSolve.CRITICAL);
			solver.setAddRowmode(true);
		} catch (LpSolveException e) {
			throw new PrismException("Error initialising LP solver: " + e.getMessage());
		}
		objRow = new double[numVars + 1];
		colno1based = new int[numVars + 1];
	}

	@Override
	public void addVar(double lb, double ub, double objCoeff) throws PrismException
	{
		int col = varCount + 1;
		objRow[col] = objCoeff;
		try {
			if (Double.isInfinite(ub)) {
				solver.setLowbo(col, lb);
			} else {
				solver.setBounds(col, lb, ub);
			}
		} catch (LpSolveException e) {
			throw new PrismException("Error setting variable bounds: " + e.getMessage());
		}
		varCount++;
	}

	@Override
	public void addConstraint(int count, double[] coeffs, int[] vars, char sense, double rhs) throws PrismException
	{
		int lpSense;
		switch (sense) {
		case '=': lpSense = LpSolve.EQ; break;
		case '<': lpSense = LpSolve.LE; break;
		case '>': lpSense = LpSolve.GE; break;
		default: throw new PrismException("Unknown LP constraint sense: " + sense);
		}
		for (int j = 0; j < count; j++) {
			colno1based[j] = vars[j] + 1;
		}
		try {
			solver.addConstraintex(count, coeffs, colno1based, lpSense, rhs);
		} catch (LpSolveException e) {
			throw new PrismException("Error adding LP constraint: " + e.getMessage());
		}
	}

	@Override
	public double[] solve(boolean maximize) throws PrismException
	{
		try {
			solver.setAddRowmode(false);
			solver.setObjFn(objRow);
			if (maximize) solver.setMaxim(); else solver.setMinim();
			int lpRes = solver.solve();
			if (lpRes == LpSolve.OPTIMAL) {
				return solver.getPtrVariables();
			} else {
				String detail = lpRes == LpSolve.INFEASIBLE ? " (infeasible)" : lpRes == LpSolve.UNBOUNDED ? " (unbounded)" : "";
				throw new PrismException("Error solving LP" + detail);
			}
		} catch (LpSolveException e) {
			throw new PrismException("Error solving LP: " + e.getMessage());
		}
	}

	@Override
	public void dispose()
	{
		solver.deleteLp();
	}
}
