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

import java.util.*;

import prism.*;

/**
 * Explicit-state model checker for continuous-time Markov chains (CTMCs).
 * 
 * This uses various bits of functionality of DTMCModelChecker, so we inherit from that.
 * (This way DTMCModelChecker picks up any options set on this one.) 
 */
public class CTMCModelChecker extends DTMCModelChecker
{
	// Model checking functions

	/**
	 * Compute probabilistic reachability.
	 * @param ctmc The CTMC
	 * @param target Target states
	 */
	public ModelCheckerResult probReach(CTMC ctmc, BitSet target) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmc = ctmc.buildImplicitEmbeddedDTMC();
		return probReach(dtmc, target);
	}

	/**
	 * Compute probabilistic reachability.
	 * @param ctmc The CTMC
	 * @param target Target states
	 * @param init Optionally, an initial solution vector for value iteration 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult probReach(CTMC ctmc, BitSet target, double init[], BitSet known) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmc = ctmc.buildImplicitEmbeddedDTMC();
		return probReach(dtmc, target, init, known);
	}

	/**
	 * Compute time-bounded probabilistic reachability.
	 * @param ctmc The CTMC
	 * @param target Target states
	 * @param t Time bound
	 */
	public ModelCheckerResult probReachTimeBounded(CTMC ctmc, BitSet target, double t) throws PrismException
	{
		return probReachTimeBounded(ctmc, target, t, null);
	}

	/**
	 * Compute time-bounded probabilistic reachability.
	 * @param ctmc The CTMC
	 * @param target Target states
	 * @param t Time bound
	 * @param init Initial solution vector - pass null for default
	 */
	public ModelCheckerResult probReachTimeBounded(CTMC ctmc, BitSet target, double t, double init[])
			throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], sum[];
		DTMC dtmc;
		long timer;
		// Fox-Glynn stuff
		FoxGlynn fg;
		int left, right;
		double q, qt, acc, weights[], totalWeight;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting time-bounded probabilistic reachability...");

		// Store num states
		n = ctmc.getNumStates();

		// Get uniformisation rate; do Fox-Glynn
		q = ctmc.getDefaultUniformisationRate();
		qt = q * t;
		mainLog.println("\nUniformisation: q.t = " + q + " x " + t + " = " + qt);
		acc = termCritParam / 8.0;
		fg = new FoxGlynn(qt, 1e-300, 1e+300, acc);
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
		mainLog.println("Fox-Glynn (" + acc + "): left = " + left + ", right = " + right);

		// Build (implicit) uniformised DTMC
		dtmc = ctmc.buildImplicitUniformisedDTMC(q);
		
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
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, target, true);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (i = 0; i < n; i++)
					sum[i] += weights[iters - left] * soln[i];
			}
			iters++;
		}

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Time-bounded probabilistic reachability");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = sum;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Compute expected reachability.
	 * @param ctmc The CTMC
	 * @param target Target states
	 */
	public ModelCheckerResult expReach(CTMC ctmc, BitSet target) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmc = ctmc.buildImplicitEmbeddedDTMC();
		return expReach(dtmc, target);
	}

	/**
	 * Compute expected reachability.
	 * @param ctmc The CTMC
	 * @param target Target states
	 * @param init Optionally, an initial solution vector for value iteration 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult expReach(CTMC ctmc, BitSet target, double init[], BitSet known) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmc = ctmc.buildImplicitEmbeddedDTMC();
		return expReach(dtmc, target, init, known);
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		CTMCModelChecker mc;
		CTMCSimple ctmc;
		ModelCheckerResult res;
		BitSet target;
		Map<String, BitSet> labels;
		try {
			mc = new CTMCModelChecker();
			ctmc = new CTMCSimple();
			ctmc.buildFromPrismExplicit(args[0]);
			//System.out.println(dtmc);
			labels = mc.loadLabelsFile(args[1]);
			//System.out.println(labels);
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			for (int i = 4; i < args.length; i++) {
				if (args[i].equals("-nopre"))
					mc.setPrecomp(false);
			}
			res = mc.probReachTimeBounded(ctmc, target, Double.parseDouble(args[3]));
			System.out.println(res.soln[0]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
