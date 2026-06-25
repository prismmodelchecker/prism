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

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import prism.PrismException;

/**
 * LP solver backend backed by Gurobi.
 */
public class GurobiSolver implements LPSolver
{
	private final GRBEnv env;
	private final GRBModel model;
	private final GRBVar[] xVars;
	public static final String ID           = "gurobi";
	public static final String DISPLAY_NAME = "Gurobi";

	@Override public String getId()          { return ID; }
	@Override public String getDisplayName() { return DISPLAY_NAME; }

	private int varCount = 0;

	public GurobiSolver(int numVars) throws PrismException
	{
		xVars = new GRBVar[numVars];
		try {
			// Empty env suppresses console and file logging
			env = new GRBEnv(true);
			env.set(GRB.IntParam.OutputFlag, 0);
			env.start();
			model = new GRBModel(env);
		} catch (GRBException e) {
			throw new PrismException("Error initialising Gurobi: " + e.getMessage());
		}
	}

	@Override
	public void addVar(double lb, double ub, double objCoeff) throws PrismException
	{
		try {
			double gurobiUb = Double.isInfinite(ub) ? GRB.INFINITY : ub;
			xVars[varCount] = model.addVar(lb, gurobiUb, objCoeff, GRB.CONTINUOUS, null);
		} catch (GRBException e) {
			throw new PrismException("Error adding Gurobi variable: " + e.getMessage());
		}
		varCount++;
	}

	@Override
	public void addConstraint(int count, double[] coeffs, int[] vars, char sense, double rhs) throws PrismException
	{
		char grbiSense;
		switch (sense) {
		case '=': grbiSense = GRB.EQUAL; break;
		case '<': grbiSense = GRB.LESS_EQUAL; break;
		case '>': grbiSense = GRB.GREATER_EQUAL; break;
		default: throw new PrismException("Unknown LP constraint sense: " + sense);
		}
		try {
			GRBLinExpr expr = new GRBLinExpr();
			for (int j = 0; j < count; j++) {
				expr.addTerm(coeffs[j], xVars[vars[j]]);
			}
			model.addConstr(expr, grbiSense, rhs, null);
		} catch (GRBException e) {
			throw new PrismException("Error adding Gurobi constraint: " + e.getMessage());
		}
	}

	@Override
	public double[] solve(boolean maximize) throws PrismException
	{
		try {
			model.set(GRB.IntAttr.ModelSense, maximize ? GRB.MAXIMIZE : GRB.MINIMIZE);
			model.optimize();
			int status = model.get(GRB.IntAttr.Status);
			if (status == GRB.Status.OPTIMAL) {
				double[] soln = new double[varCount];
				for (int s = 0; s < varCount; s++) {
					soln[s] = xVars[s].get(GRB.DoubleAttr.X);
				}
				return soln;
			} else {
				String detail = status == GRB.Status.INFEASIBLE ? " (infeasible)" : status == GRB.Status.UNBOUNDED ? " (unbounded)" : "";
				throw new PrismException("Error solving LP" + detail);
			}
		} catch (GRBException e) {
			throw new PrismException("Error solving LP: " + e.getMessage());
		}
	}

	@Override
	public void dispose()
	{
		try {
			model.dispose();
			env.dispose();
		} catch (GRBException e) {
			// Suppress — nothing useful can be done during cleanup
		}
	}
}
