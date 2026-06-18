//==============================================================================
//
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package prism;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract base class for multi-objective model checking.
 * Contains engine-agnostic iteration algorithms for Pareto curve computation
 * and achievability/numerical queries. Engine-specific subclasses implement
 * {@link WeightedObjectiveSolver} to solve a single weighted-sum MDP objective.
 *
 * <p>See {@code symbolic.comp.MultiObjModelChecker} for the symbolic (sparse) engine
 * implementation.
 */
public abstract class MultiObjModelChecker extends PrismComponent
{
	protected boolean verbose;

	/** Weight scale used in the error-recovery fallback when a solver call fails to converge on an axis direction. */
	protected static final double FALLBACK_WEIGHT_SCALE = 1e4;

	/**
	 * Functional interface for the engine-specific part of multi-objective solving:
	 * given a weight vector, solve the corresponding weighted single-objective MDP
	 * and return the objective values at the initial state.
	 */
	@FunctionalInterface
	public interface WeightedObjectiveSolver
	{
		/**
		 * @param weights Weight vector (one entry per objective, summing to 1 after normalisation)
		 * @return Array of objective values at the initial state (one per objective)
		 */
		double[] solve(double[] weights) throws PrismException;
	}

	public MultiObjModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
		this.verbose = settings.getBoolean(PrismSettings.PRISM_VERBOSE);
	}

	/**
	 * Run the Pareto curve iteration loop.
	 *
	 * <p>Iteratively optimises weighted sums to build an under-approximation of the Pareto
	 * front. The algorithm terminates when the TileList reports no direction that can still
	 * be improved by more than {@code tolerance}, or when {@code maxIters} is reached.
	 *
	 * <p><b>Convention:</b> {@code moQuery} must be fully canonicalised before this method
	 * is called — probabilistic operators P_MAX / P_GE (via {@link MultiObjQuery#makeAllProbUp()})
	 * and reward operators R_MAX / R_GE (via {@link MultiObjQuery#makeAllRewardUp()}).
	 * The solver must be configured so that it always maximises: minimising reward DDs must
	 * have been pre-negated, and minimising prob DDs must have been built for the negated formula.
	 * Points returned by the solver are in this solver-space; the resulting {@link TileList}
	 * uses {@link MultiObjQuery#isProbNegated} and {@link MultiObjQuery#isRewardNegated}
	 * (via {@link Point#toRealProperties}) to convert points back to user-space on output.
	 *
	 * @param solver              Engine-specific weighted solver (captures product model, targets, rewards)
	 * @param moQuery             Canonicalised objective operators/bounds (P_MAX/P_GE, R_MAX/R_GE)
	 * @param pointsForInitialTile Extreme points computed during axis initialisation (one per objective)
	 * @param tolerance           Pareto epsilon tolerance
	 * @param maxIters            Maximum number of weight-direction iterations
	 * @return Under-approximation of the Pareto front as a {@link TileList}
	 */
	protected TileList runParetoCurveIteration(WeightedObjectiveSolver solver, MultiObjQuery moQuery,
	                                            List<Point> pointsForInitialTile, double tolerance, int maxIters)
	        throws PrismException
	{
		Tile initialTile = new Tile(new ArrayList<>(pointsForInitialTile));
		TileList tileList = new TileList(initialTile, moQuery, tolerance);
		Point direction = tileList.getCandidateHyperplane();

		if (verbose) {
			mainLog.println("The initial direction is " + direction);
		}

		int numberOfPoints = pointsForInitialTile.size();
		double timer = System.currentTimeMillis();

		boolean decided = false;
		int iters = 0;
		while (iters < maxIters) {
			iters++;

			mainLog.println("Optimising weighted sum of objectives: weights " + direction);
			double[] result = solver.solve(direction.getCoords());
			numberOfPoints++;

			Point newPoint = new Point(result);
			mainLog.println("Computed point: " + newPoint);

			if (verbose) {
				mainLog.println("\n" + numberOfPoints + ": New point is " + newPoint + ".");
				mainLog.println("TileList:" + tileList);
			}

			tileList.addNewPoint(newPoint);

			direction = tileList.getCandidateHyperplane();
			if (verbose) {
				mainLog.println("New direction is " + direction);
			}

			if (direction == null) {
				decided = true;
				break;
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("The value iteration(s) took " + timer / 1000.0 + " seconds altogether.");
		mainLog.println("Number of weight vectors used: " + numberOfPoints);

		if (!decided) {
			throw new PrismException("The computation did not finish in " + maxIters
			                         + " target point iterations, try increasing this number using the -multimaxpoints switch.");
		}

		String paretoFile = settings.getString(PrismSettings.PRISM_EXPORT_PARETO_FILENAME);
		if (paretoFile != null && !paretoFile.equals("")) {
			MultiObjUtils.exportPareto(tileList, paretoFile);
			mainLog.println("Exported Pareto curve. To see it, run\n etc/scripts/prism-pareto.py " + paretoFile);
		}

		if (verbose) {
			mainLog.print("Computed " + tileList.getNumberOfDifferentPoints() + " points altogether: ");
			mainLog.println(tileList.getPoints().toString());
		}

		return tileList;
	}

	/**
	 * Run the achievability/numerical iteration loop.
	 *
	 * <p>Builds the initial target point, optionally tightens it with a pure reward-axis solve
	 * when a reward is being maximised, then iteratively finds separating hyperplanes to
	 * determine whether the target is achievable or (for numerical queries) to compute the
	 * optimal value.
	 *
	 * <p><b>Convention:</b> {@code moQuery} must be fully canonicalised before this method
	 * is called — probabilistic operators P_MAX / P_GE (via {@link MultiObjQuery#makeAllProbUp()})
	 * and reward operators R_MAX / R_GE (via {@link MultiObjQuery#makeAllRewardUp()}).
	 * The solver must always maximise: minimising reward DDs must have been pre-negated;
	 * minimising prob DDs must have been built for the negated formula.
	 *
	 * <p><b>Result conventions:</b> the returned value is in <em>solver-space</em>:
	 * <ul>
	 *   <li>For a negated reward (originally R_MIN/R_LE): the solver maximised −reward,
	 *       so the returned coordinate is the negated minimum. The caller must negate it
	 *       (check {@link MultiObjQuery#isRewardNegated}) to recover the user-space value.
	 *   <li>For a negated probability (originally P_MIN): the DRA was built for ¬φ and the
	 *       returned coordinate is max P(¬φ). The caller must apply 1−value to recover
	 *       the user-space probability.
	 * </ul>
	 *
	 * @param solver       Engine-specific weighted solver
	 * @param moQuery      Canonicalised objective operators/bounds (P_MAX/P_GE, R_MAX/R_GE)
	 * @param maxIters     Maximum number of iterations
	 * @return For achievability: 1.0 (achievable) or 0.0 (not achievable).
	 *         For numerical: the solver-space coordinate of the optimised objective
	 *         (sign correction for negated objectives is the caller's responsibility).
	 */
	protected double runAchievabilityIteration(WeightedObjectiveSolver solver, MultiObjQuery moQuery,
	                                            int maxIters) throws PrismException
	{
		int dimProb = moQuery.numProbObjectives();
		int dimReward = moQuery.numRewardObjectives();

		// After makeAllProbUp/makeAllRewardUp, prob operators are P_MAX or P_GE,
		// and reward operators are R_MAX or R_GE.
		boolean maximizingProb = dimProb > 0 && moQuery.getProbOperator(0) == Operator.P_MAX;
		boolean maximizingReward = dimReward > 0 && moQuery.getRewardOperator(0) == Operator.R_MAX;
		// Used only for the infeasibility-direction check inside the iteration loop:
		// when a reward was negated (R_MIN/R_LE → maximise −reward), the "decided infeasible"
		// condition is rest > 0 rather than rest < 0. Sign correction of the returned value
		// is the caller's responsibility.
		boolean maximizingNegated = maximizingReward && moQuery.isRewardNegated(0);

		// Build initial target point from operator bounds.
		// Bounds for negated objectives are already stored with the correct sign by makeAllRewardUp.
		Point targetPoint = new Point(dimProb + dimReward);
		for (int i = 0; i < dimProb; i++) {
			targetPoint.setCoord(i, moQuery.getProbBound(i));
		}
		if (maximizingProb) {
			targetPoint.setCoord(0, 1.0);
		}
		for (int i = 0; i < dimReward; i++) {
			targetPoint.setCoord(i + dimProb, moQuery.getRewardBound(i));
		}

		// For a maximising reward objective, tighten the initial target point with a pure reward-axis solve
		if (maximizingReward) {
			if (verbose) {
				mainLog.println("Getting an upper bound on maximizing objective");
			}
			double[] axisDir = new double[dimProb + dimReward];
			axisDir[dimProb] = 1.0;
			double[] result = solver.solve(axisDir);
			targetPoint.setCoord(dimProb, result[dimProb]);
			if (verbose) {
				mainLog.println("Upper bound is " + result[dimProb]);
			}
		}

		ArrayList<Point> computedPoints = new ArrayList<>();
		ArrayList<Point> computedDirections = new ArrayList<>();

		Point direction = MultiObjUtils.getWeights(targetPoint, computedPoints);

		if (verbose) {
			mainLog.println("The initial target point is " + targetPoint);
			mainLog.println("The initial direction is " + direction);
		}

		double timer = System.currentTimeMillis();
		int numberOfPoints = 0;
		boolean decided = false;
		boolean isAchievable = false;
		int iters = 0;
		while (iters < maxIters) {
			iters++;

			double[] weights = direction.getCoords();
			double[] result = solver.solve(weights);
			numberOfPoints++;

			Point newPoint = new Point(result);
			if (verbose) {
				mainLog.println("New point is " + newPoint + ".");
			}

			computedPoints.add(newPoint);
			computedDirections.add(direction);

			// Compute weighted distance of new point and target to the separating hyperplane
			double dNew = 0.0;
			for (int i = 0; i < dimProb + dimReward; i++) {
				dNew += newPoint.getCoord(i) * direction.getCoord(i);
			}
			double dTarget = 0.0;
			for (int i = 0; i < dimProb + dimReward; i++) {
				dTarget += targetPoint.getCoord(i) * direction.getCoord(i);
			}

			if (dTarget > dNew) {
				if (maximizingProb || maximizingReward) {
					int maximizingCoord = maximizingProb ? 0 : dimProb;
					double rest = dNew - (dTarget - direction.getCoord(maximizingCoord) * targetPoint.getCoord(maximizingCoord));
					if ((!maximizingNegated && rest < 0) || (maximizingNegated && rest > 0)) {
						decided = true;
						targetPoint.setCoord(maximizingCoord, Double.NaN);
						if (verbose) {
							mainLog.println("Decided, target is " + targetPoint);
						}
						break;
					} else {
						double lowered = rest / direction.getCoord(maximizingCoord);
						targetPoint.setCoord(maximizingCoord, lowered);
						// Negative infinity means constraints are not achievable
						if (lowered == Double.NEGATIVE_INFINITY) {
							targetPoint.setCoord(maximizingCoord, Double.NaN);
							mainLog.println("\nThe constraints are not achievable!\n");
							decided = true;
							isAchievable = false;
							break;
						}
						if (verbose) {
							mainLog.println("Target lowered to " + targetPoint);
						}
					}
				} else {
					decided = true;
					isAchievable = false;
					break;
				}
			}

			direction = MultiObjUtils.getWeights(targetPoint, computedPoints);
			if (verbose) {
				mainLog.println("New direction is " + direction);
			}

			if (direction == null || computedDirections.contains(direction)) {
				decided = true;
				isAchievable = true;
				break;
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("The value iteration(s) took " + timer / 1000.0 + " seconds altogether.");
		mainLog.println("Number of weight vectors used: " + numberOfPoints);

		if (!decided) {
			throw new PrismException("The computation did not finish in " + maxIters
			                         + " target point iterations, try increasing this number using the -multimaxpoints switch.");
		}
		if (maximizingProb || maximizingReward) {
			int maximizingCoord = maximizingProb ? 0 : dimProb;
			return targetPoint.getCoord(maximizingCoord);
		} else {
			return isAchievable ? 1.0 : 0.0;
		}
	}

	/**
	 * Compute one extreme point per objective by optimising along each axis direction.
	 * These points seed the initial tile of the Pareto curve computation.
	 *
	 * <p>For each objective {@code k} (probability objectives first, then reward objectives),
	 * a unit weight vector with weight 1 on axis {@code k} is solved. If the solver throws,
	 * the direction is replaced by a strongly-skewed normalised fallback that still strongly
	 * favours objective {@code k}, and the solve is retried.
	 *
	 * @param solver    Engine-specific weighted-sum solver
	 * @param dimProb   Number of probability objectives
	 * @param dimReward Number of reward objectives
	 * @return List of extreme points, one per objective (prob axes first, then reward axes)
	 * @throws PrismException if a fallback solve also fails
	 */
	protected List<Point> buildAxisInitialPoints(WeightedObjectiveSolver solver, int dimProb, int dimReward)
	        throws PrismException
	{
		int dim = dimProb + dimReward;
		List<Point> points = new ArrayList<>();
		for (int k = 0; k < dim; k++) {
			boolean isProb = k < dimProb;
			String objType = isProb ? "probability" : "reward";
			int objNum = isProb ? (k + 1) : (k - dimProb + 1);
			int objTotal = isProb ? dimProb : dimReward;

			double[] axisDir = new double[dim];
			axisDir[k] = 1.0;
			double[] result;
			try {
				mainLog.println("Optimising weighted sum for " + objType + " objective " + objNum + "/" + objTotal + ": weights " + Arrays.toString(axisDir));
				result = solver.solve(axisDir);
			} catch (PrismException e) {
				mainLog.println("Ignoring the last multi-objective computation since it did not complete successfully");
				for (int j = 0; j < dim; j++) {
					axisDir[j] = (j == k) ? FALLBACK_WEIGHT_SCALE : 1.0;
				}
				Point fallback = new Point(axisDir);
				fallback = fallback.normalize();
				axisDir = fallback.getCoords();
				mainLog.println("Optimising weighted sum for " + objType + " objective " + objNum + "/" + objTotal + ": weights " + Arrays.toString(axisDir));
				result = solver.solve(axisDir);
			}
			Point pt = new Point(result);
			mainLog.println("Computed point: " + pt);
			points.add(pt);
		}
		return points;
	}
}
