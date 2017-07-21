//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import common.IntSet;
import common.PeriodicTimer;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import prism.PrismException;
import prism.PrismUtils;

/**
 * Abstract class that encapsulates the functionality for the different iteration methods
 * (e.g., Power, Jacobi, Gauss-Seidel, ...).
 * <p>
 * Provides methods as well to do the actual work in a (topological) value iteration.
 */
public abstract class IterationMethod {

	/**
	 * Interface for an object that provides the basic steps for a value iteration.
	 */
	public interface IterationValIter {
		/** Initialise the value iteration with the given solution vector */
		public void init(double[] soln);
		/** Get the current solution vector */
		public double[] getSolnVector();

		/** Perform one iteration (over the set of states) and return true if convergence has been detected. */
		public boolean iterateAndCheckConvergence(IntSet states) throws PrismException;

		/**
		 * Solve for a given singleton SCC consisting of {@code state} using {@code solver},
		 * store the result in the solution vector(s).
		 */
		public void solveSingletonSCC(int state, SingletonSCCSolver solver);

		/** Return the underlying model */
		public Model getModel();
	}

	/** Storage for a single solution vector */
	public class IterationBasic {
		protected final Model model;
		protected double[] soln;

		public IterationBasic(Model model)
		{
			this.model = model;
		}

		public void init(double[] soln)
		{
			this.soln = soln;
		}

		public double[] getSolnVector()
		{
			return soln;
		}

		public void solveSingletonSCC(int state, SingletonSCCSolver solver)
		{
			solver.solveFor(state, soln);
		}

		public Model getModel()
		{
			return model;
		}
	}

	/** Abstract base class for an IterationValIter with a single solution vector */
	protected abstract class SingleVectorIterationValIter extends IterationBasic implements IterationValIter
	{
		public SingleVectorIterationValIter(Model model)
		{
			super(model);
		}
	}

	/**
	 * Functional interface for a post-processing step after an iteration that involves
	 * a pair of solution vectors.
	 * <br>
	 * This method may modify solnNew.
	 *
	 * @param solnOld the previous solution vector
	 * @param solnNew the new solution vector
	 * @param states the set of states that are the focus of the current iteration
	 */
	@FunctionalInterface
	interface IterationPostProcessor {
		void apply(double[] solnOld, double[] solnNew, IntSet states) throws PrismException;
	}

	/**
	 * Abstract base class for an IterationValIter that
	 * requires two solution vectors.
	 * Optionally, a post processing step is performed after each iteration.
	 */
	protected abstract class TwoVectorIteration extends IterationBasic implements IterationValIter {
		/** The solution vector that serves as the target vector in the iteration step */
		protected double[] soln2;
		/** Post processing, may be null */
		protected final IterationPostProcessor postProcessor;

		/** Constructor */
		protected TwoVectorIteration(Model model, IterationMethod.IterationPostProcessor postProcessor)
		{
			super(model);
			this.postProcessor = postProcessor;
		}

		@Override
		public void init(double[] soln)
		{
			super.init(soln);

			// create and initialise the second solution vector
			soln2 = new double[soln.length];
			System.arraycopy(soln, 0, soln2, 0, soln.length);
		}

		/** Perform one iteration */
		public abstract void doIterate(IntSet states) throws PrismException;

		@Override
		public boolean iterateAndCheckConvergence(IntSet states) throws PrismException
		{
			// do the iteration
			doIterate(states);
			// optionally, post processing
			if (postProcessor != null) {
				postProcessor.apply(soln, soln2, states);
			}
			// check convergence (on the set of states)
			boolean done = PrismUtils.doublesAreClose(soln, soln2, states.iterator(), termCritParam, absolute);

			// switch vectors
			double[] tmp = soln;
			soln = soln2;
			soln2 = tmp;

			return done;
		}

		@Override
		public void solveSingletonSCC(int state, SingletonSCCSolver solver)
		{
			// solve and store result in soln vector
			super.solveSingletonSCC(state, solver);
			// copy result to soln2 vector as well
			soln2[state] = soln[state];
		}

	}

	/**
	 * Functional interface for a method that allows to
	 * determine the value for a singleton SCC in the model,
	 * given that all successor value have already been computed.
	 */
	@FunctionalInterface
	public interface SingletonSCCSolver {
		/**
		 * Compute the value for state {@code state}, under the assumption
		 * that it constitutes a (trivial or non-trivial) singleton SCC
		 * and that all successor values have already been computed in {@code soln}.
		 * Stores the result in {@code soln[state]}.
		 */
		public void solveFor(int state, double[] soln);
	}

	/** Convergence check: absolute or relative? */
	protected final boolean absolute;
	/** Convergence check: epsilon value */
	protected final double termCritParam;

	/**
	 * Constructor.
	 * @param absolute For convergence check, perform absolute comparison?
	 * @param termCritParam For convergence check, the epsilon value to use
	 */
	protected IterationMethod(boolean absolute, double termCritParam)
	{
		this.absolute = absolute;
		this.termCritParam = termCritParam;
	}

	// ------------ Abstract DTMC methods ----------------------------

	/** Obtain an Iteration object using mvMult (matrix-vector multiplication) in a DTMC */
	public abstract IterationValIter forMvMult(DTMC dtmc) throws PrismException;

	/** Obtain an Iteration object using mvMultRew (matrix-vector multiplication with rewards) in a DTMC */
	public abstract IterationValIter forMvMultRew(DTMC dtmc, MCRewards rew) throws PrismException;



	// ------------ Abstract MDP methods ----------------------------

	/**
	 * Obtain an Iteration object using mvMultMinMax (matrix-vector multiplication, followed by min/max)
	 * in an MDP.
	 * @param mdp the MDP
	 * @param min do min?
	 * @param strat optional, storage for strategy, ignored if null
	 */
	public abstract IterationValIter forMvMultMinMax(MDP mdp, boolean min, int[] strat) throws PrismException;

	/**
	 * Obtain an Iteration object using mvMultRewMinMax (matrix-vector multiplication with rewards, followed by min/max)
	 * in an MDP.
	 * @param mdp the MDP
	 * @param rewards the reward structure
	 * @param min do min?
	 * @param strat optional, storage for strategy, ignored if null
	 */
	public abstract IterationValIter forMvMultRewMinMax(MDP mdp, MDPRewards rewards, boolean min, int[] strat) throws PrismException;



	// ------------ Abstract generic methods ----------------------------

	/**
	 * Return a description of this iteration method for display.
	 */
	public abstract String getDescriptionShort();


	// ------------ Value iteration implementations ----------------------------

	/**
	 * Perform the actual work of a value iteration, i.e., iterate until convergence or abort.
	 * @param mc ProbModelChecker (for log and settings)
	 * @param description (for logging)
	 * @param iteration The iteration object
	 * @param unknownStates The set of unknown states, i.e., whose value should be determined
	 * @param startTime The start time (for logging purposes, obtained from a call to System.currentTimeMillis())
	 * @param iterationsExport an ExportIterations object (optional, ignored if null)
	 * @return a ModelChecker result with the solution vector and statistics
	 * @throws PrismException on non-convergence (if mc.errorOnNonConverge is set)
	 */
	public ModelCheckerResult doValueIteration(ProbModelChecker mc, String description, IterationValIter iteration, IntSet unknownStates, long startTime, ExportIterations iterationsExport) throws PrismException
	{
		int iters = 0;
		final int maxIters = mc.maxIters;
		boolean done = false;

		PeriodicTimer updatesTimer = new PeriodicTimer(ProbModelChecker.UPDATE_DELAY);
		updatesTimer.start();

		while (!done && iters < maxIters) {
			iters++;
			// do iteration step
			done = iteration.iterateAndCheckConvergence(unknownStates);

			if (iterationsExport != null)
				iterationsExport.exportVector(iteration.getSolnVector(), 0);

			if (!done && updatesTimer.triggered()) {
				mc.getLog().print("Iteration " + iters + ": ");
				mc.getLog().println(PrismUtils.formatDouble2dp(updatesTimer.elapsedMillisTotal() / 1000.0) + " sec so far");
			}
		}

		// Finished value iteration
		long mvCount = iters * countTransitions(iteration.getModel(), unknownStates);
		long timer = System.currentTimeMillis() - startTime;
		mc.getLog().print("Value iteration (" + description + ")");
		mc.getLog().print(" took " + iters + " iterations, ");
		mc.getLog().print(mvCount + " MV-multiplications");
		mc.getLog().println(" and " + timer / 1000.0 + " seconds.");

		if (iterationsExport != null)
			iterationsExport.close();

		// Non-convergence is an error (usually)
		if (!done && mc.errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = iteration.getSolnVector();
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Perform the actual work of a topological value iteration, i.e., iterate until convergence or abort.
	 *
	 * @param mc ProbModelChecker (for log and settings)
	 * @param description (for logging)
	 * @param sccs The information about the SCCs and topological order
	 * @param iteration The iteration object
	 * @param singletonSCCSolver The solver for singleton SCCs
	 * @param startTime The start time (for logging purposes, obtained from a call to System.currentTimeMillis())
	 * @param iterationsExport an ExportIterations object (optional, ignored if null)
	 * @return a ModelChecker result with the solution vector and statistics
	 * @throws PrismException on non-convergence (if mc.errorOnNonConverge is set)
	 */
	public ModelCheckerResult doTopologicalValueIteration(ProbModelChecker mc, String description, SCCInfo sccs, IterationMethod.IterationValIter iterator, SingletonSCCSolver singletonSCCSolver, long startTime, ExportIterations iterationsExport) throws PrismException
	{
		// Start iterations
		int iters = 0;
		long mvCount = 0;
		final int maxIters = mc.maxIters;

		int numSCCs = sccs.getNumSCCs();
		int numNonSingletonSCCs = sccs.countNonSingletonSCCs();
		int finishedNonSingletonSCCs = 0;

		PeriodicTimer updatesTimer = new PeriodicTimer(ProbModelChecker.UPDATE_DELAY);
		updatesTimer.start();

		boolean done = true;
		for (int scc = 0; scc < numSCCs; scc++) {
			boolean doneSCC;

			if (sccs.isSingletonSCC(scc)) {
				// get the single state in this SCC
				int state = sccs.getStatesForSCC(scc).iterator().nextInt();
				iterator.solveSingletonSCC(state, singletonSCCSolver);

				mvCount += countTransitions(iterator.getModel(), IntSet.asIntSet(state));

				iters++;
				if (iterationsExport != null)
					iterationsExport.exportVector(iterator.getSolnVector(), 0);

				doneSCC = true;
			} else {
				// complex SCC: do VI
				doneSCC = false;
				IntSet statesForSCC = sccs.getStatesForSCC(scc);
				int itersInSCC = 0;
				// abort on convergence or if iterations *in this SCC* are above maxIters
				while (!doneSCC && itersInSCC < maxIters) {
					iters++;
					itersInSCC++;
					// do iteration step
					doneSCC = iterator.iterateAndCheckConvergence(statesForSCC);

					if (iterationsExport != null)
						iterationsExport.exportVector(iterator.getSolnVector(), 0);

					if (!doneSCC && updatesTimer.triggered()) {
						mc.getLog().print("Iteration " + iters + ": ");
						mc.getLog().print("Iteration " + itersInSCC + " in SCC " + (finishedNonSingletonSCCs+1) + " of " + numNonSingletonSCCs);
						mc.getLog().println(", " + PrismUtils.formatDouble2dp(updatesTimer.elapsedMillisTotal() / 1000.0) + " sec so far");
					}

				}

				mvCount += itersInSCC * countTransitions(iterator.getModel(), statesForSCC);
			}

			if (!doneSCC) {
				done = false;
				break;
			}
		}

		// Finished value iteration
		long timer = System.currentTimeMillis() - startTime;
		mc.getLog().print("Value iteration (" + description + ", with " + numNonSingletonSCCs + " non-singleton SCCs)");
		mc.getLog().print(" took " + iters + " iterations, ");
		mc.getLog().print(mvCount + " MV-multiplications");
		mc.getLog().println(" and " + timer / 1000.0 + " seconds.");

		if (iterationsExport != null)
			iterationsExport.close();

		// Non-convergence is an error (usually)
		if (!done && mc.errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = iterator.getSolnVector();
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	protected long countTransitions(Model model, IntSet unknownStates)
	{
		if (model instanceof DTMC) {
			return ((DTMC)model).getNumTransitions(unknownStates.iterator());
		} else if (model instanceof MDP) {
			return ((MDP)model).getNumTransitions(unknownStates.iterator());
		} else {
			throw new IllegalArgumentException("Can only count transitions for DTMCs and MDPs, not for " + model.getModelType());
		}
	}

}