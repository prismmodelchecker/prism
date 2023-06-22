//==============================================================================
//
//	Copyright (c) 2023-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package explicit;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PrimitiveIterator;

import acceptance.AcceptanceReach;
import common.IntSet;
import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import parser.ast.Expression;
import prism.AccuracyFactory;
import prism.Evaluator;
import prism.PrismComponent;
import prism.PrismException;
import strat.FMDStrategyStep;
import prism.PrismFileLog;
import strat.FMDStrategyProduct;
import strat.MDStrategy;
import strat.MDStrategyArray;
import strat.Strategy;
import gurobi.*;

/**
 * Explicit-state model checker for interval Markov decision prcoesses (IMDPs).
 */
public class IPOMDPModelChecker extends ProbModelChecker
{
	/**
	 * Create a new IPOMDPModelChecker, inherit basic state from parent (unless null).
	 */
	public IPOMDPModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param ipomdp The IPOMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeUntilProbs(IPOMDP<Double> ipomdp, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		return computeReachProbs(ipomdp, remain, target, minMax);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param ipomdp The IPOMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachProbs(IPOMDP<Double> ipomdp, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		// Just iterate through model and print transitions
		int numStates = ipomdp.getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = ipomdp.getNumChoices(s);
			for (int i = 0; i < numChoices; i++) {
				Iterator<Map.Entry<Integer, Interval<Double>>> iter = ipomdp.getTransitionsIterator(s, i);
				while (iter.hasNext()) {
					Map.Entry<Integer, Interval<Double>> elem = iter.next();
					mainLog.println(s + "," + i + " -> " + elem.getValue() + ":" + elem.getKey());
				}
			}
		}

		try {
			GRBEnv env = new GRBEnv("IPOMDP.log");
			GRBModel model = new GRBModel(env);

			// Create variables
			GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
			GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
			GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");

			// Set objective: maximize x + y + 2 z
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerm(1.0, x); expr.addTerm(1.0, y); expr.addTerm(2.0, z);
			model.setObjective(expr, GRB.MAXIMIZE);

			// Add constraint: x + 2 y + 3 z <= 4
			expr = new GRBLinExpr();
			expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
			model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

			// Add constraint: x + y >= 1
			expr = new GRBLinExpr();
			expr.addTerm(1.0, x); expr.addTerm(1.0, y);
			model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

			// Optimize model
			model.optimize();

			System.out.println(x.get(GRB.StringAttr.VarName)
					+ " " +x.get(GRB.DoubleAttr.X));
			System.out.println(y.get(GRB.StringAttr.VarName)
					+ " " +y.get(GRB.DoubleAttr.X));
			System.out.println(z.get(GRB.StringAttr.VarName)
					+ " " +z.get(GRB.DoubleAttr.X));

			System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

			model.dispose();
			env.dispose();
		} catch (GRBException e) {
			throw new PrismException("Error solving LP: " + e.getMessage());
		}

		// Return dummy result vector
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = new double[numStates];
		return res;
	}
}
