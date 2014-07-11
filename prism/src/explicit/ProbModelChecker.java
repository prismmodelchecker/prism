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
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.RelOp;
import parser.ast.RewardStruct;
import parser.type.TypeDouble;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismSettings;
import explicit.rewards.ConstructRewards;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import explicit.rewards.STPGRewards;

/**
 * Super class for explicit-state probabilistic model checkers.
 */
public class ProbModelChecker extends NonProbModelChecker
{
	// Flags/settings
	// (NB: defaults do not necessarily coincide with PRISM)

	// Method used to solve linear equation systems
	protected LinEqMethod linEqMethod = LinEqMethod.GAUSS_SEIDEL;
	// Method used to solve MDPs
	protected MDPSolnMethod mdpSolnMethod = MDPSolnMethod.GAUSS_SEIDEL;
	// Iterative numerical method termination criteria
	protected TermCrit termCrit = TermCrit.RELATIVE;
	// Parameter for iterative numerical method termination criteria
	protected double termCritParam = 1e-8;
	// Max iterations for numerical solution
	protected int maxIters = 100000;
	// Use precomputation algorithms in model checking?
	protected boolean precomp = true;
	protected boolean prob0 = true;
	protected boolean prob1 = true;
	// Direction of convergence for value iteration (lfp/gfp)
	protected ValIterDir valIterDir = ValIterDir.BELOW;
	// Method used for numerical solution
	protected SolnMethod solnMethod = SolnMethod.VALUE_ITERATION;
	// Is non-convergence of an iterative method an error?
	protected boolean errorOnNonConverge = true;
	// Adversary export
	protected boolean exportAdv = false;
	protected String exportAdvFilename;

	// Enums for flags/settings

	// Method used for numerical solution
	public enum LinEqMethod {
		POWER, JACOBI, GAUSS_SEIDEL, BACKWARDS_GAUSS_SEIDEL, JOR, SOR, BACKWARDS_SOR;
		public String fullName()
		{
			switch (this) {
			case POWER:
				return "Power method";
			case JACOBI:
				return "Jacobi";
			case GAUSS_SEIDEL:
				return "Gauss-Seidel";
			case BACKWARDS_GAUSS_SEIDEL:
				return "Backwards Gauss-Seidel";
			case JOR:
				return "JOR";
			case SOR:
				return "SOR";
			case BACKWARDS_SOR:
				return "Backwards SOR";
			default:
				return this.toString();
			}
		}
	};

	// Method used for solving MDPs
	public enum MDPSolnMethod {
		VALUE_ITERATION, GAUSS_SEIDEL, POLICY_ITERATION, MODIFIED_POLICY_ITERATION, LINEAR_PROGRAMMING;
		public String fullName()
		{
			switch (this) {
			case VALUE_ITERATION:
				return "Value iteration";
			case GAUSS_SEIDEL:
				return "Gauss-Seidel";
			case POLICY_ITERATION:
				return "Policy iteration";
			case MODIFIED_POLICY_ITERATION:
				return "Modified policy iteration";
			case LINEAR_PROGRAMMING:
				return "Linear programming";
			default:
				return this.toString();
			}
		}
	};

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
		VALUE_ITERATION, GAUSS_SEIDEL, POLICY_ITERATION, MODIFIED_POLICY_ITERATION, LINEAR_PROGRAMMING
	};

	/**
	 * Create a new ProbModelChecker, inherit basic state from parent (unless null).
	 */
	public ProbModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);

		// If present, initialise settings from PrismSettings
		if (settings != null) {
			String s;
			// PRISM_LIN_EQ_METHOD
			s = settings.getString(PrismSettings.PRISM_LIN_EQ_METHOD);
			if (s.equals("Power")) {
				setLinEqMethod(LinEqMethod.POWER);
			} else if (s.equals("Jacobi")) {
				setLinEqMethod(LinEqMethod.JACOBI);
			} else if (s.equals("Gauss-Seidel")) {
				setLinEqMethod(LinEqMethod.GAUSS_SEIDEL);
			} else if (s.equals("Backwards Gauss-Seidel")) {
				setLinEqMethod(LinEqMethod.BACKWARDS_GAUSS_SEIDEL);
			} else if (s.equals("JOR")) {
				setLinEqMethod(LinEqMethod.JOR);
			} else if (s.equals("SOR")) {
				setLinEqMethod(LinEqMethod.SOR);
			} else if (s.equals("Backwards SOR")) {
				setLinEqMethod(LinEqMethod.BACKWARDS_SOR);
			} else {
				throw new PrismException("Explicit engine does not support linear equation solution method \"" + s + "\"");
			}
			// PRISM_MDP_SOLN_METHOD
			s = settings.getString(PrismSettings.PRISM_MDP_SOLN_METHOD);
			if (s.equals("Value iteration")) {
				setMDPSolnMethod(MDPSolnMethod.VALUE_ITERATION);
			} else if (s.equals("Gauss-Seidel")) {
				setMDPSolnMethod(MDPSolnMethod.GAUSS_SEIDEL);
			} else if (s.equals("Policy iteration")) {
				setMDPSolnMethod(MDPSolnMethod.POLICY_ITERATION);
			} else if (s.equals("Modified policy iteration")) {
				setMDPSolnMethod(MDPSolnMethod.MODIFIED_POLICY_ITERATION);
			} else if (s.equals("Linear programming")) {
				setMDPSolnMethod(MDPSolnMethod.LINEAR_PROGRAMMING);
			} else {
				throw new PrismException("Explicit engine does not support MDP solution method \"" + s + "\"");
			}
			// PRISM_TERM_CRIT
			s = settings.getString(PrismSettings.PRISM_TERM_CRIT);
			if (s.equals("Absolute")) {
				setTermCrit(TermCrit.ABSOLUTE);
			} else if (s.equals("Relative")) {
				setTermCrit(TermCrit.RELATIVE);
			} else {
				throw new PrismException("Unknown termination criterion \"" + s + "\"");
			}
			// PRISM_TERM_CRIT_PARAM
			setTermCritParam(settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM));
			// PRISM_MAX_ITERS
			setMaxIters(settings.getInteger(PrismSettings.PRISM_MAX_ITERS));
			// PRISM_PRECOMPUTATION
			setPrecomp(settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION));
			// PRISM_PROB0
			setProb0(settings.getBoolean(PrismSettings.PRISM_PROB0));
			// PRISM_PROB1
			setProb1(settings.getBoolean(PrismSettings.PRISM_PROB1));
			// PRISM_FAIRNESS
			if (settings.getBoolean(PrismSettings.PRISM_FAIRNESS)) {
				throw new PrismException("The explicit engine does not support model checking MDPs under fairness");
			}

			// PRISM_EXPORT_ADV
			s = settings.getString(PrismSettings.PRISM_EXPORT_ADV);
			if (!(s.equals("None")))
				setExportAdv(true);
			// PRISM_EXPORT_ADV_FILENAME
			setExportAdvFilename(settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME));
		}
	}

	// Settings methods

	/**
	 * Inherit settings (and the log) from another ProbModelChecker object.
	 * For model checker objects that inherit a PrismSettings object, this is superfluous
	 * since this has been done already.
	 */
	public void inheritSettings(ProbModelChecker other)
	{
		super.inheritSettings(other);
		setLinEqMethod(other.getLinEqMethod());
		setMDPSolnMethod(other.getMDPSolnMethod());
		setTermCrit(other.getTermCrit());
		setTermCritParam(other.getTermCritParam());
		setMaxIters(other.getMaxIters());
		setPrecomp(other.getPrecomp());
		setProb0(other.getProb0());
		setProb1(other.getProb1());
		setValIterDir(other.getValIterDir());
		setSolnMethod(other.getSolnMethod());
		setErrorOnNonConverge(other.geterrorOnNonConverge());
	}

	/**
	 * Print summary of current settings.
	 */
	public void printSettings()
	{
		super.printSettings();
		mainLog.print("linEqMethod = " + linEqMethod + " ");
		mainLog.print("mdpSolnMethod = " + mdpSolnMethod + " ");
		mainLog.print("termCrit = " + termCrit + " ");
		mainLog.print("termCritParam = " + termCritParam + " ");
		mainLog.print("maxIters = " + maxIters + " ");
		mainLog.print("precomp = " + precomp + " ");
		mainLog.print("prob0 = " + prob0 + " ");
		mainLog.print("prob1 = " + prob1 + " ");
		mainLog.print("valIterDir = " + valIterDir + " ");
		mainLog.print("solnMethod = " + solnMethod + " ");
		mainLog.print("errorOnNonConverge = " + errorOnNonConverge + " ");
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
	 * Set method used to solve linear equation systems.
	 */
	public void setLinEqMethod(LinEqMethod linEqMethod)
	{
		this.linEqMethod = linEqMethod;
	}

	/**
	 * Set method used to solve MDPs.
	 */
	public void setMDPSolnMethod(MDPSolnMethod mdpSolnMethod)
	{
		this.mdpSolnMethod = mdpSolnMethod;
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

	/**
	 * Set whether non-convergence of an iterative method an error
	 */
	public void setErrorOnNonConverge(boolean errorOnNonConverge)
	{
		this.errorOnNonConverge = errorOnNonConverge;
	}

	public void setExportAdv(boolean exportAdv)
	{
		this.exportAdv = exportAdv;
	}

	public void setExportAdvFilename(String exportAdvFilename)
	{
		this.exportAdvFilename = exportAdvFilename;
	}

	// Get methods for flags/settings

	public int getVerbosity()
	{
		return verbosity;
	}

	public LinEqMethod getLinEqMethod()
	{
		return linEqMethod;
	}

	public MDPSolnMethod getMDPSolnMethod()
	{
		return mdpSolnMethod;
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

	/**
	 * Is non-convergence of an iterative method an error?
	 */
	public boolean geterrorOnNonConverge()
	{
		return errorOnNonConverge;
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
			res = checkExpressionSteadyState(model, (ExpressionSS) expr);
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
		RelOp relOp; // Relational operator
		StateValues probs = null;

		// Get info from P operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateDouble(constantValues);
			if (p < 0 || p > 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}

		// Compute probabilities
		MinMax minMax = (relOp.isLowerBound() || relOp.isMin()) ? MinMax.min() : MinMax.max();
		probs = checkProbPathFormula(model, expr.getExpression(), minMax);

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
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
	 * Compute probabilities for the contents of a P operator.
	 */
	protected StateValues checkProbPathFormula(Model model, Expression expr, MinMax minMax) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and then pass control to appropriate method. 
		if (expr.isSimplePathFormula()) {
			return checkProbPathFormulaSimple(model, expr, minMax);
		} else {
			return checkProbPathFormulaLTL(model, expr, false, minMax);
		}
	}

	/**
	 * Compute probabilities for a simple, non-LTL path operator.
	 */
	protected StateValues checkProbPathFormulaSimple(Model model, Expression expr, MinMax minMax) throws PrismException
	{
		StateValues probs = null;

		// Negation/parentheses
		if (expr instanceof ExpressionUnaryOp) {
			ExpressionUnaryOp exprUnary = (ExpressionUnaryOp) expr;
			// Parentheses
			if (exprUnary.getOperator() == ExpressionUnaryOp.PARENTH) {
				// Recurse
				probs = checkProbPathFormulaSimple(model, exprUnary.getOperand(), minMax);
			}
			// Negation
			else if (exprUnary.getOperator() == ExpressionUnaryOp.NOT) {
				// Compute, then subtract from 1 
				probs = checkProbPathFormulaSimple(model, exprUnary.getOperand(), minMax.negate());
				probs.timesConstant(-1.0);
				probs.plusConstant(1.0);
			}
		}
		// Temporal operators
		else if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(model, exprTemp, minMax);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(model, exprTemp, minMax);
				} else {
					probs = checkProbUntil(model, exprTemp, minMax);
				}
			}
			// Anything else - convert to until and recurse
			else {
				probs = checkProbPathFormulaSimple(model, exprTemp.convertToUntilForm(), minMax);
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		return probs;
	}

	/**
	 * Compute probabilities for a next operator.
	 */
	protected StateValues checkProbNext(Model model, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		// Model check the operand
		BitSet target = checkExpression(model, expr.getOperand2()).getBitSet();

		// Compute/return the probabilities
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case CTMC:
			res = ((CTMCModelChecker) this).computeNextProbs((CTMC) model, target);
			break;
		case DTMC:
			res = ((DTMCModelChecker) this).computeNextProbs((DTMC) model, target);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeNextProbs((MDP) model, target, minMax.isMin());
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeNextProbs((STPG) model, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute probabilities for a bounded until operator.
	 */
	protected StateValues checkProbBoundedUntil(Model model, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		// This method just handles discrete time
		// Continuous-time model checkers will override this method

		// Get info from bounded until
		int time = expr.getUpperBound().evaluateInt(constantValues);
		if (expr.upperBoundIsStrict())
			time--;
		if (time < 0) {
			String bound = expr.upperBoundIsStrict() ? "<" + (time + 1) : "<=" + time;
			throw new PrismException("Invalid bound " + bound + " in bounded until formula");
		}

		// Model check operands
		BitSet remain = checkExpression(model, expr.getOperand1()).getBitSet();
		BitSet target = checkExpression(model, expr.getOperand2()).getBitSet();

		// Compute/return the probabilities
		// A trivial case: "U<=0" (prob is 1 in target states, 0 otherwise)
		if (time == 0) {
			return StateValues.createFromBitSetAsDoubles(target, model);
		}
		// Otherwise: numerical solution
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeBoundedUntilProbs((DTMC) model, remain, target, time);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeBoundedUntilProbs((MDP) model, remain, target, time, minMax.isMin());
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeBoundedUntilProbs((STPG) model, remain, target, time, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute probabilities for an (unbounded) until operator.
	 */
	protected StateValues checkProbUntil(Model model, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		// Model check operands
		BitSet remain = checkExpression(model, expr.getOperand1()).getBitSet();
		BitSet target = checkExpression(model, expr.getOperand2()).getBitSet();

		// Compute/return the probabilities
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case CTMC:
			res = ((CTMCModelChecker) this).computeUntilProbs((CTMC) model, remain, target);
			break;
		case DTMC:
			res = ((DTMCModelChecker) this).computeUntilProbs((DTMC) model, remain, target);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeUntilProbs((MDP) model, remain, target, minMax.isMin());
			result.setStrategy(res.strat);
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeUntilProbs((STPG) model, remain, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute probabilities for an LTL path formula
	 */
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax) throws PrismException
	{
		// To be overridden by subclasses
		throw new PrismException("Computation not implemented yet");
	}

	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionReward(Model model, ExpressionReward expr) throws PrismException
	{
		Expression rb; // Reward bound (expression)
		double r = 0; // Reward bound (actual value)
		RelOp relOp; // Relational operator
		StateValues rews = null;
		Rewards rewards = null;

		// Get info from R operator
		RewardStruct rewStruct = expr.getRewardStructByIndexObject(modulesFile, constantValues);
		relOp = expr.getRelOp();
		rb = expr.getReward();
		if (rb != null) {
			r = rb.evaluateDouble(constantValues);
			if (r < 0)
				throw new PrismException("Invalid reward bound " + r + " in R[] formula");
		}

		// Build rewards
		mainLog.println("Building reward structure...");
		rewards = constructRewards(model, rewStruct);

		// Compute rewards
		MinMax minMax = (relOp.isLowerBound() || relOp.isMin()) ? MinMax.min() : MinMax.max();
		rews = checkRewardFormula(model, rewards, expr.getExpression(), minMax);

		// Print out rewards
		if (getVerbosity() > 5) {
			mainLog.print("\nRewards (non-zero only) for all states:\n");
			rews.print(mainLog);
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

	/**
	 * Construct rewards from a reward structure and a model.
	 */
	protected Rewards constructRewards(Model model, RewardStruct rewStruct) throws PrismException
	{
		Rewards rewards;
		ConstructRewards constructRewards = new ConstructRewards(mainLog);
		switch (model.getModelType()) {
		case CTMC:
		case DTMC:
			rewards = constructRewards.buildMCRewardStructure((DTMC) model, rewStruct, constantValues);
			break;
		case MDP:
			rewards = constructRewards.buildMDPRewardStructure((MDP) model, rewStruct, constantValues);
			break;
		default:
			throw new PrismException("Cannot build rewards for " + model.getModelType() + "s");
		}
		return rewards;
	}
	
	/**
	 * Compute rewards for the contents of an R operator.
	 */
	protected StateValues checkRewardFormula(Model model, Rewards modelRewards, Expression expr, MinMax minMax) throws PrismException
	{
		StateValues rewards = null;

		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_F:
				rewards = checkRewardReach(model, modelRewards, exprTemp, minMax);
				break;
			case ExpressionTemporal.R_I:
				rewards = checkRewardInstantaneous(model, modelRewards, exprTemp, minMax);
				break;
			case ExpressionTemporal.R_C:
				rewards = checkRewardCumulative(model, modelRewards, exprTemp, minMax);
				break;
			default:
				throw new PrismException("Explicit engine does not yet handle the " + exprTemp.getOperatorSymbol() + " reward operator");
			}
		}

		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		return rewards;
	}

	/**
	 * Compute rewards for a reachability reward operator.
	 */
	protected StateValues checkRewardReach(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		// Model check the operand
		BitSet target = checkExpression(model, expr.getOperand2()).getBitSet();

		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeReachRewards((DTMC) model, (MCRewards) modelRewards, target);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeReachRewards((CTMC) model, (MCRewards) modelRewards, target);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeReachRewards((MDP) model, (MDPRewards) modelRewards, target, minMax.isMin());
			result.setStrategy(res.strat);
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeReachRewards((STPG) model, (STPGRewards) modelRewards, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute rewards for an instantaneous reward operator.
	 */
	protected StateValues checkRewardInstantaneous(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		// Get time bound
		double t = expr.getUpperBound().evaluateDouble(constantValues);

		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeInstantaneousRewards((DTMC) model, (MCRewards) modelRewards, t);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeInstantaneousRewards((CTMC) model, (MCRewards) modelRewards, t);
			break;
		default:
			throw new PrismException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute rewards for a cumulative reward operator.
	 */
	protected StateValues checkRewardCumulative(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		int timeInt = -1;
		double timeDouble = -1;

		// Check that there is an upper time bound
		if (expr.getUpperBound() == null) {
			throw new PrismException("Cumulative reward operator without time bound (C) is only allowed for multi-objective queries");
		}

		// Get time bound
		if (model.getModelType().continuousTime()) {
			timeDouble = expr.getUpperBound().evaluateDouble(constantValues);
			if (timeDouble < 0) {
				throw new PrismException("Invalid time bound " + timeDouble + " in cumulative reward formula");
			}
		} else {
			timeInt = expr.getUpperBound().evaluateInt(constantValues);
			if (timeInt < 0) {
				throw new PrismException("Invalid time bound " + timeInt + " in cumulative reward formula");
			}
		}

		// Compute/return the rewards
		// A trivial case: "C<=0" (prob is 1 in target states, 0 otherwise)
		if (timeInt == 0 || timeDouble == 0) {
			return new StateValues(TypeDouble.getInstance(), model.getNumStates(), new Double(0));
		}
		// Otherwise: numerical solution
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeCumulativeRewards((DTMC) model, (MCRewards) modelRewards, timeInt);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeCumulativeRewards((CTMC) model, (MCRewards) modelRewards, timeDouble);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeCumulativeRewards((MDP) model, (MDPRewards) modelRewards, timeInt, minMax.isMin());
			result.setStrategy(res.strat);
			break;
		default:
			throw new PrismException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Model check an S operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionSteadyState(Model model, ExpressionSS expr) throws PrismException
	{
		Expression pb; // Probability bound (expression)
		double p = 0; // Probability bound (actual value)
		RelOp relOp; // Relational operator
		StateValues probs = null;

		// Get info from S operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateDouble(constantValues);
			if (p < 0 || p > 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}

		// Compute probabilities
		MinMax minMax = (relOp.isLowerBound() || relOp.isMin()) ? MinMax.min() : MinMax.max();
		probs = checkSteadyStateFormula(model, expr.getExpression(), minMax);

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
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
	 * Compute steady-state probabilities for an S operator.
	 */
	protected StateValues checkSteadyStateFormula(Model model, Expression expr, MinMax minMax) throws PrismException
	{
		// Model check operand
		BitSet b = checkExpression(model, expr).getBitSet();

		// Compute/return the probabilities
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			double multProbs[] = Utils.bitsetToDoubleArray(b, model.getNumStates());
			res = ((DTMCModelChecker) this).computeSteadyStateBackwardsProbs((DTMC) model, multProbs);
			break;
		default:
			throw new PrismException("Explicit engine does not yet handle the S operator for " + model.getModelType() + "s");
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}
}
