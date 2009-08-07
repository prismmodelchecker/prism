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

package explicit;

import java.util.BitSet;

import prism.PrismException;

/**
 * Explicit-state model checker for continuous-time Markov decision processes (CTMDPs).
 */
public class CTMDPModelChecker extends MDPModelChecker
{
	/**
	 * Compute bounded probabilistic reachability.
	 * @param target: Target states
	 * @param t: Time bound
	 * @param min: Min or max probabilities for (true=min, false=max)
	 * @param init: Initial solution vector - pass null for default
	 * @param results: Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult probReachBounded(CTMDP ctmdp, BitSet target, double t, boolean min, double init[],
			double results[]) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double  soln[], soln2[], tmpsoln[], sum[];
		long timer;
		// Fox-Glynn stuff
		FoxGlynn fg;
		int left, right;
		double unif, qt, weights[], totalWeight;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting bounded probabilistic reachability...");

		// Store num states
		n = ctmdp.numStates;

		// Get uniformisation rate; do Fox-Glynn
		unif = ctmdp.unif;
		qt = unif * t;
		mainLog.println("\nUniformisation: q.t = " + unif + " x " + t + " = " + unif * t);
		fg = new FoxGlynn(qt, 1e-300, 1e+300, termCritParam / 8.0);
		left = fg.getLeftTruncationPoint();
		right = fg.getRightTruncationPoint();
		if (right < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation (time bound too big?)");
		}
		weights = fg.getWeights();
		totalWeight = fg.getTotalWeight();
		for (i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}
		mainLog.println("Fox-Glynn: left = "+left+", right = "+right);

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;
		sum = new double[n];

		// Initialise solution vectors. Use passed in initial vector, if present
		if (init != null) {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : init[i];
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;
		}
		for (i = 0; i < n; i++)
			sum[i] = 0.0;
		
		// If necessary, do 0th element of summation (doesn't require any matrix powers)
		if (left == 0)
			for (i = 0; i < n; i++)
				sum[i] += weights[0] * soln[i];
		
		// Start iterations
		iters = 1;
		while (iters <= right) {
			// Matrix-vector multiply and min/max ops
			ctmdp.mvMultMinMax(soln, min, soln2, target, true);
			// Since is globally uniform, can do this? and more?
			for (i = 0; i < n; i++)
				soln2[i] /= unif;
			// Store intermediate results if required
			// TODO?
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (i = 0; i < n; i++)
					sum[i] += weights[iters-left] * soln[i];
			}
			iters++;
		}

		// Print vector (for debugging)
		mainLog.println(sum);

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Probabilistic bounded reachability (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = sum;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}
}
