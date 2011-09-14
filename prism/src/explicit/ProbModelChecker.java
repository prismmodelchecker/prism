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

import parser.ast.Expression;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.RewardStruct;
import prism.ModelType;
import prism.PrismException;
import prism.PrismSettings;
import explicit.rewards.ConstructRewards;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;

/**
 * Super class for explicit-state probabilistic model checkers
 */
public class ProbModelChecker extends StateModelChecker
{
	// Flags/settings

	// Iterative numerical method termination criteria
	protected TermCrit termCrit = TermCrit.RELATIVE;
	// Parameter for iterative numerical method termination criteria
	protected double termCritParam = 1e-8;
	// Max iterations for numerical solution
	protected int maxIters = 100000; // TODO: make same as PRISM?
	// Use precomputation algorithms in model checking?
	protected boolean precomp = true;
	protected boolean prob0 = true;
	protected boolean prob1 = true;
	// Direction of convergence for value iteration (lfp/gfp)
	protected ValIterDir valIterDir = ValIterDir.BELOW;
	// Method used for numerical solution
	protected SolnMethod solnMethod = SolnMethod.VALUE_ITERATION;
	// Adversary export
	protected boolean exportAdv = false;
	protected String exportAdvFilename;

	// Enums for flags/settings

	// Iterative numerical method termination criteria
	public enum TermCrit {
		ABSOLUTE, RELATIVE
	};

	// Direction of convergence for value iteration (lfp/gfp)
	public enum ValIterDir {
		BELOW, ABOVE
	};

	// Method used for numerical solution
	public enum SolnMethod {
		VALUE_ITERATION, GAUSS_SEIDEL, POLICY_ITERATION, MODIFIED_POLICY_ITERATION
	};

	// Settings methods
	
	/**
	 * Set settings from a PRISMSettings object.
	 */
	public void setSettings(PrismSettings settings)
	{
		String s;
		s = settings.getString(PrismSettings.PRISM_TERM_CRIT);
		if (s.equals("Absolute")) {
			setTermCrit(TermCrit.ABSOLUTE);
		} else if (s.equals("Relative")) {
			setTermCrit(TermCrit.RELATIVE);
		}
		setTermCritParam(settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM));
		setMaxIters(settings.getInteger(PrismSettings.PRISM_MAX_ITERS));
		setPrecomp(settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION));
		// prob0
		// prob1
		// valiterdir
		s = settings.getString(PrismSettings.PRISM_LIN_EQ_METHOD);
		if (s.equals("Gauss-Seidel")) {
			setSolnMethod(SolnMethod.GAUSS_SEIDEL);
		} else {
			setSolnMethod(SolnMethod.VALUE_ITERATION);
		}
		s = settings.getString(PrismSettings.PRISM_MDP_SOLN_METHOD);
		if (s.equals("Gauss-Seidel")) {
			setSolnMethod(SolnMethod.GAUSS_SEIDEL);
		} else if (s.equals("Policy iteration")) {
			setSolnMethod(SolnMethod.POLICY_ITERATION);
		} else if (s.equals("Modified policy iteration")) {
			setSolnMethod(SolnMethod.MODIFIED_POLICY_ITERATION);
		} else {
			setSolnMethod(SolnMethod.VALUE_ITERATION);
		}
		s = settings.getString(PrismSettings.PRISM_EXPORT_ADV);
		if (!(s.equals("None")))
			exportAdv = true;
		exportAdvFilename = settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME);
	}

	/**
	 * Inherit settings (and other info) from another model checker object.
	 */
	public void inheritSettings(ProbModelChecker other)
	{
		super.inheritSettings(other);
		setTermCrit(other.getTermCrit());
		setTermCritParam(other.getTermCritParam());
		setMaxIters(other.getMaxIters());
		setPrecomp(other.getPrecomp());
		setProb0(other.getProb0());
		setProb1(other.getProb1());
		setValIterDir(other.getValIterDir());
		setSolnMethod(other.getSolnMethod());
	}

	/**
	 * Print summary of current settings.
	 */
	public void printSettings()
	{
		super.printSettings();
		mainLog.print("termCrit = " + termCrit + " ");
		mainLog.print("termCritParam = " + termCritParam + " ");
		mainLog.print("maxIters = " + maxIters + " ");
		mainLog.print("precomp = " + precomp + " ");
		mainLog.print("prob0 = " + prob0 + " ");
		mainLog.print("prob1 = " + prob1 + " ");
		mainLog.print("valIterDir = " + valIterDir + " ");
		mainLog.print("solnMethod = " + solnMethod + " ");
	}

	// Set methods for flags/settings

	/**
	 * Set verbosity level, i.e. amount of output produced.
	 */
	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
	}

	/**
	 * Set termination criteria type for numerical iterative methods.
	 */
	public void setTermCrit(TermCrit termCrit)
	{
		this.termCrit = termCrit;
	}

	/**
	 * Set termination criteria parameter (epsilon) for numerical iterative methods.
	 */
	public void setTermCritParam(double termCritParam)
	{
		this.termCritParam = termCritParam;
	}

	/**
	 * Set maximum number of iterations for numerical iterative methods.
	 */
	public void setMaxIters(int maxIters)
	{
		this.maxIters = maxIters;
	}

	/**
	 * Set whether or not to use precomputation (Prob0, Prob1, etc.).
	 */
	public void setPrecomp(boolean precomp)
	{
		this.precomp = precomp;
	}

	/**
	 * Set whether or not to use Prob0 precomputation
	 */
	public void setProb0(boolean prob0)
	{
		this.prob0 = prob0;
	}

	/**
	 * Set whether or not to use Prob1 precomputation
	 */
	public void setProb1(boolean prob1)
	{
		this.prob1 = prob1;
	}

	/**
	 * Set direction of convergence for value iteration (lfp/gfp).
	 */
	public void setValIterDir(ValIterDir valIterDir)
	{
		this.valIterDir = valIterDir;
	}

	/**
	 * Set method used for numerical solution.
	 */
	public void setSolnMethod(SolnMethod solnMethod)
	{
		this.solnMethod = solnMethod;
	}

	// Get methods for flags/settings

	public int getVerbosity()
	{
		return verbosity;
	}

	public TermCrit getTermCrit()
	{
		return termCrit;
	}

	public double getTermCritParam()
	{
		return termCritParam;
	}

	public int getMaxIters()
	{
		return maxIters;
	}

	public boolean getPrecomp()
	{
		return precomp;
	}

	public boolean getProb0()
	{
		return prob0;
	}

	public boolean getProb1()
	{
		return prob1;
	}

	public ValIterDir getValIterDir()
	{
		return valIterDir;
	}

	public SolnMethod getSolnMethod()
	{
		return solnMethod;
	}

	// Model checking functions

	@Override
	public StateValues checkExpression(Model model, Expression expr) throws PrismException
	{
		StateValues res;

		// P operator
		if (expr instanceof ExpressionProb) {
			res = checkExpressionProb(model, (ExpressionProb) expr);
		}
		// R operator
		else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward(model, (ExpressionReward) expr);
		}
		// S operator
		else if (expr instanceof ExpressionSS) {
			throw new PrismException("Explicit engine does not yet handle the S operator");
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(model, expr);
		}

		return res;
	}

	/**
	 * Model check a P operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionProb(Model model, ExpressionProb expr) throws PrismException
	{
		Expression pb; // Probability bound (expression)
		double p = 0; // Probability bound (actual value)
		String relOp; // Relational operator
		boolean min = false; // For nondeterministic models, are we finding min (true) or max (false) probs
		ModelType modelType = model.getModelType();

		StateValues probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateDouble(constantValues);
			if (p < 0 || p > 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}

		// For nondeterministic models, determine whether min or max probabilities needed
		if (modelType.nondeterministic()) {
			if (relOp.equals(">") || relOp.equals(">=") || relOp.equals("min=")) {
				// min
				min = true;
			} else if (relOp.equals("<") || relOp.equals("<=") || relOp.equals("max=")) {
				// max
				min = false;
			} else {
				throw new PrismException("Can't use \"P=?\" for nondeterministic models; use \"Pmin=?\" or \"Pmax=?\"");
			}
		}

		// Compute probabilities
		switch (modelType) {
		case CTMC:
			probs = ((CTMCModelChecker) this).checkProbPathFormula(model, expr.getExpression());
			break;
		case CTMDP:
			probs = ((CTMDPModelChecker) this).checkProbPathFormula(model, expr.getExpression(), min);
			break;
		case DTMC:
			probs = ((DTMCModelChecker) this).checkProbPathFormula(model, expr.getExpression());
			break;
		case MDP:
			probs = ((MDPModelChecker) this).checkProbPathFormula(model, expr.getExpression(), min);
			break;
		/*case STPG:
			probs = ((STPGModelChecker) this).checkProbPathFormula(model, expr.getExpression(), min);
			break;*/
		default:
			throw new PrismException("Cannot model check " + expr + " for a " + modelType);
		}

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(probs);
		}

		// For =? properties, just return values
		if (pb == null) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			BitSet sol = probs.getBitSetFromInterval(relOp, p);
			probs.clear();
			return StateValues.createFromBitSet(sol, model);
		}
	}
	
	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionReward(Model model, ExpressionReward expr) throws PrismException
	{
		Object rs; // Reward struct index
		RewardStruct rewStruct = null; // Reward struct object
		Expression rb; // Reward bound (expression)
		double r = 0; // Reward bound (actual value)
		String relOp; // Relational operator
		boolean min = false; // For nondeterministic models, are we finding min (true) or max (false) rewards
		ModelType modelType = model.getModelType();
		StateValues rews = null;
		MCRewards mcRewards = null;
		MDPRewards mdpRewards = null;
		int i;

		// Get info from reward operator
		rs = expr.getRewardStructIndex();
		relOp = expr.getRelOp();
		rb = expr.getReward();
		if (rb != null) {
			r = rb.evaluateDouble(constantValues);
			if (r < 0)
				throw new PrismException("Invalid reward bound " + r + " in R[] formula");
		}

		// For nondeterministic models, determine whether min or max rewards needed
		if (modelType.nondeterministic()) {
			if (relOp.equals(">") || relOp.equals(">=") || relOp.equals("min=")) {
				// min
				min = true;
			} else if (relOp.equals("<") || relOp.equals("<=") || relOp.equals("max=")) {
				// max
				min = false;
			} else {
				throw new PrismException("Can't use \"R=?\" for nondeterministic models; use \"Rmin=?\" or \"Rmax=?\"");
			}
		}

		// Get reward info
		if (modulesFile == null)
			throw new PrismException("No model file to obtain reward structures");
		if (modulesFile.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		if (rs == null) {
			rewStruct = modulesFile.getRewardStruct(0);
		} else if (rs instanceof Expression) {
			i = ((Expression) rs).evaluateInt(constantValues);
			rs = new Integer(i); // for better error reporting below
			rewStruct = modulesFile.getRewardStruct(i - 1);
		} else if (rs instanceof String) {
			rewStruct = modulesFile.getRewardStructByName((String) rs);
		}
		if (rewStruct == null)
			throw new PrismException("Invalid reward structure index \"" + rs + "\"");

		// Build rewards
		ConstructRewards constructRewards = new ConstructRewards(mainLog);
		switch (modelType) {
		case CTMC:
		case DTMC:
			mcRewards = constructRewards.buildMCRewardStructure((DTMC) model, rewStruct, constantValues);
			break;
		case MDP:
			mdpRewards = constructRewards.buildMDPRewardStructure((MDP) model, rewStruct, constantValues);
			break;
		default:
			throw new PrismException("Cannot build rewards for " + modelType + "s");
		}
		
		// Compute rewards
		mainLog.println("Building reward structure...");
		switch (modelType) {
		case CTMC:
			rews = ((CTMCModelChecker) this).checkRewardFormula(model, mcRewards, expr.getExpression());
			break;
		case DTMC:
			rews = ((DTMCModelChecker) this).checkRewardFormula(model, mcRewards, expr.getExpression());
			break;
		case MDP:
			rews = ((MDPModelChecker) this).checkRewardFormula(model, mdpRewards, expr.getExpression(), min);
			break;
		default:
			throw new PrismException("Cannot model check " + expr + " for " + modelType + "s");
		}

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(rews);
		}

		// For =? properties, just return values
		if (rb == null) {
			return rews;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			BitSet sol = rews.getBitSetFromInterval(relOp, r);
			rews.clear();
			return StateValues.createFromBitSet(sol, model);
		}
	}
}
