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

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import cern.colt.Arrays;
import explicit.rewards.ConstructRewards;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import explicit.rewards.STPGRewards;
import parser.ParserUtils;
import parser.ast.Coalition;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypePathBool;
import parser.type.TypePathDouble;
import prism.AccuracyFactory;
import prism.IntegerBound;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.PrismUtils;
import explicit.AlphaVector;

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
	// Resolution for POMDP fixed grid approximation algorithm
	protected int gridResolution = 10;
	// Use precomputation algorithms in model checking?
	protected boolean precomp = true;
	protected boolean prob0 = true;
	protected boolean prob1 = true;
	// should we suppress log output during precomputations?
	protected boolean silentPrecomputations = false;
	// Use predecessor relation? (e.g. for precomputation)
	protected boolean preRel = true;
	// Direction of convergence for value iteration (lfp/gfp)
	protected ValIterDir valIterDir = ValIterDir.BELOW;
	// Method used for numerical solution
	protected SolnMethod solnMethod = SolnMethod.VALUE_ITERATION;
	// Is non-convergence of an iterative method an error?
	protected boolean errorOnNonConverge = true;
	// Adversary export
	protected boolean exportAdv = false;
	protected String exportAdvFilename;

	// Delay between occasional updates for slow processes, e.g. numerical solution (milliseconds)
	public static final int UPDATE_DELAY = 5000;


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
				throw new PrismNotSupportedException("Explicit engine does not support linear equation solution method \"" + s + "\"");
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
				throw new PrismNotSupportedException("Explicit engine does not support MDP solution method \"" + s + "\"");
			}
			// PRISM_TERM_CRIT
			s = settings.getString(PrismSettings.PRISM_TERM_CRIT);
			if (s.equals("Absolute")) {
				setTermCrit(TermCrit.ABSOLUTE);
			} else if (s.equals("Relative")) {
				setTermCrit(TermCrit.RELATIVE);
			} else {
				throw new PrismNotSupportedException("Unknown termination criterion \"" + s + "\"");
			}
			// PRISM_TERM_CRIT_PARAM
			setTermCritParam(settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM));
			// PRISM_MAX_ITERS
			setMaxIters(settings.getInteger(PrismSettings.PRISM_MAX_ITERS));
			// PRISM_GRID_RESOLUTION
			setGridResolution(settings.getInteger(PrismSettings.PRISM_GRID_RESOLUTION));
			// PRISM_PRECOMPUTATION
			setPrecomp(settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION));
			// PRISM_PROB0
			setProb0(settings.getBoolean(PrismSettings.PRISM_PROB0));
			// PRISM_PROB1
			setProb1(settings.getBoolean(PrismSettings.PRISM_PROB1));
			// PRISM_USE_PRE
			setPreRel(settings.getBoolean(PrismSettings.PRISM_PRE_REL));
			// PRISM_FAIRNESS
			if (settings.getBoolean(PrismSettings.PRISM_FAIRNESS)) {
				throw new PrismNotSupportedException("The explicit engine does not support model checking MDPs under fairness");
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
		setGridResolution(other.getGridResolution());
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
		mainLog.print("gridResolution = " + gridResolution + " ");
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
	 * Set flag for suppressing log output during precomputations (prob0, prob1, ...)
	 * @param value silent?
	 * @return the previous value of this flag
	 */
	public boolean setSilentPrecomputations(boolean value)
	{
		boolean old = silentPrecomputations;
		silentPrecomputations = value;
		return old;
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
	 * Set resolution for POMDP fixed grid approximation algorithm.
	 */
	public void setGridResolution(int gridResolution)
	{
		this.gridResolution = gridResolution;
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
	 * Set whether or not to use pre-computed predecessor relation
	 */
	public void setPreRel(boolean preRel)
	{
		this.preRel = preRel;
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

	public int getGridResolution()
	{
		return gridResolution;
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

	public boolean getPreRel()
	{
		return preRel;
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
	public StateValues checkExpression(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		StateValues res;

		// <<>> or [[]] operator
		if (expr instanceof ExpressionStrategy) {
			res = checkExpressionStrategy(model, (ExpressionStrategy) expr, statesOfInterest);
		}
		// P operator
		else if (expr instanceof ExpressionProb) {
			res = checkExpressionProb(model, (ExpressionProb) expr, statesOfInterest);
		}
		// R operator
		else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward(model, (ExpressionReward) expr, statesOfInterest);
		}
		// S operator
		else if (expr instanceof ExpressionSS) {
			res = checkExpressionSteadyState(model, (ExpressionSS) expr);
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(model, expr, statesOfInterest);
		}

		return res;
	}

	/**
	 * Model check a <<>> or [[]] operator expression and return the values for the statesOfInterest.
	 * * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionStrategy(Model model, ExpressionStrategy expr, BitSet statesOfInterest) throws PrismException
	{
		// Only support <<>>/[[]] for MDPs right now
		if (!(this instanceof MDPModelChecker || this instanceof POMDPModelChecker))
			throw new PrismNotSupportedException("The " + expr.getOperatorString() + " operator is only supported for MDPs currently");

		// Will we be quantifying universally or existentially over strategies/adversaries?
		boolean forAll = !expr.isThereExists();
		
		// Extract coalition info
		Coalition coalition = expr.getCoalition();
		// For non-games (i.e., models with a single player), deal with the coalition operator here and then remove it
		if (coalition != null && !model.getModelType().multiplePlayers()) {
			if (coalition.isEmpty()) {
				// An empty coalition negates the quantification ("*" has no effect)
				forAll = !forAll;
			}
			coalition = null;
		}

		// Pass onto relevant method:
		List<Expression> exprs = expr.getOperands();
		// P operator
		if (exprs.size() == 1 && exprs.get(0) instanceof ExpressionProb) {
			return checkExpressionProb(model, (ExpressionProb) exprs.get(0), forAll, coalition, statesOfInterest);
		}
		// R operator
		else if (exprs.size() == 1 && exprs.get(0) instanceof ExpressionReward) {
			return checkExpressionReward(model, (ExpressionReward) exprs.get(0), forAll, coalition, statesOfInterest);
		}
		// Anything else is treated as multi-objective 
		else {
			return checkExpressionMultiObj(model, exprs, statesOfInterest);
		}
	}

	/**
	 * Model check a multi-objective property and return the values for the statesOfInterest.
	 * * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionMultiObj(Model model, List<Expression> exprs, BitSet statesOfInterest) throws PrismException
	{
		// Weighted sum
		if (exprs.size() == 1 && exprs.get(0) instanceof ExpressionBinaryOp && ((ExpressionBinaryOp) exprs.get(0)).getOperator() == ExpressionBinaryOp.PLUS) {
			Expression expr = exprs.get(0);
			List<Double> weights = new ArrayList<>();
			List<ExpressionReward> objs = new ArrayList<>();
			List<Expression> summands = ParserUtils.splitOnBinaryOp(expr, ExpressionBinaryOp.PLUS);
			for (Expression summand : summands) {
				if (summand instanceof ExpressionBinaryOp && ((ExpressionBinaryOp) summand).getOperator() == ExpressionBinaryOp.TIMES) {
					Expression weight = ((ExpressionBinaryOp) summand).getOperand1();
					if (!weight.isConstant()) {
						throw new PrismLangException("Non-constant weight in multi-objective property", weight);
					}
					if (TypeDouble.getInstance().canAssign(weight.getType())) {
						weights.add(weight.evaluateDouble(constantValues));
					} else {
						throw new PrismLangException("Weights in multi-objective properties should be doubles", weight);
					}
					Expression obj = ((ExpressionBinaryOp) summand).getOperand2();
					if (obj instanceof ExpressionReward) {
						if (((ExpressionReward) obj).getReward() != null) {
							throw new PrismLangException("Weighted multi-objective properties can only contain numerical reward properties", obj);
						}
						objs.add((ExpressionReward) obj);
					} else {
						throw new PrismLangException("Weighted multi-objective properties can only contain reward properties", obj);
					}
				} else {
					throw new PrismLangException("Multi-objective property is not a weighted sum ", expr);
				}
			}
			return checkExpressionWeightedMultiObj(model, weights, objs, statesOfInterest);
		}
		// Otherwise, assume Pareto
		else {
			List<ExpressionReward> objs = new ArrayList<>();
			for (Expression obj : exprs) {
				if (obj instanceof ExpressionReward) {
					if (((ExpressionReward) obj).getReward() != null) {
						throw new PrismLangException("Pareto multi-objective properties can only contain numerical reward properties", obj);
					}
					objs.add((ExpressionReward) obj);
				} else {
					throw new PrismLangException("Weighted multi-objective properties can only contain reward properties", obj);
				}

			}
			return checkExpressionParetoMultiObj(model, objs, statesOfInterest);
		}
	}
	
	/**
	 * Model check a weighted sum multi-objective property and return the values for the statesOfInterest.
	 * * @param statesOfInterest the states of interest, see checkExpression()
	 */

	protected StateValues checkExpressionWeightedMultiObj(Model model, List<Double> weights, List<ExpressionReward> objs, BitSet statesOfInterest) throws PrismException
	{
		// Build rewards
		// And recompute weights, negating if needed 
		//mainLog.println("Building reward structure...");
		int numRewards = weights.size();
		List<Double> weightsNew = new ArrayList<>();
		List<MDPRewards> mdpRewardsList = new ArrayList<>();
		for (int i = 0; i < numRewards; i++) {
			int r = objs.get(i).getRewardStructIndexByIndexObject(rewardGen, constantValues);
			mdpRewardsList.add((MDPRewards) constructRewards(model, r));
			if (objs.get(i).getRelopBoundInfo(constantValues).getMinMax(model.getModelType(), false).isMin()) {
				weightsNew.add(-1.0 * weights.get(i));
			} else {
				weightsNew.add(weights.get(i));
			}
		}
		
		// Model check the target
		// TODO: Check all targets are the same for all R props
		ExpressionTemporal exprTemp = (ExpressionTemporal) objs.get(0).getExpression();
		BitSet target = checkExpression(model, exprTemp.getOperand2(), null).getBitSet();
		
		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case MDP:
			res = ((MDPModelChecker) this).computeMultiReachRewards((MDP) model, weightsNew, mdpRewardsList, target, false, statesOfInterest);
			break;
		case POMDP:
			res = ((POMDPModelChecker) this).computeMultiReachRewards((POMDP) model, weightsNew, mdpRewardsList, target, false, statesOfInterest);
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + exprTemp.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromObjectArray(TypeDouble.getInstance(), res.solnObj, model);
	}
	
	public ArrayList<Double> linSolver(ArrayList<ArrayList<Double>> A, ArrayList<Double> b)
	{
		if (A.get(0).size()!=A.size()){
			mainLog.println("Matrix is not square!");
		}
		ArrayList<ArrayList<Double>> Ab = new ArrayList<ArrayList<Double>>();
		for (int i=0; i<A.size(); i++) {
			Ab.add( (ArrayList<Double>) A.get(i).clone() );
			Ab.get(i).add(b.get(i));
		}
		mainLog.println("\nA:"+Arrays.toString(A.toArray()));
		mainLog.println("\nb:"+Arrays.toString(b.toArray()));		
		for (int k=0; k<Ab.size()-1; k++) {
			mainLog.println("Ab0:"+Arrays.toString(Ab.toArray()));

			double pivot = Ab.get(k).get(k);
			mainLog.println("k:"+k);
			// find the max and swap
			double max=pivot;
			int max_location=k;
			for (int imax=k;imax<Ab.size();imax++) {
				if (Ab.get(imax).get(k)>max) {
					max = Ab.get(imax).get(k);
					max_location=imax;
			//		mainLog.println("max:"+max);
			//		mainLog.println("max_location:"+max_location);

				}
			}
			//mainLog.println("max_location:"+max_location);
			ArrayList<Double> tpmax= (ArrayList<Double>) Ab.get(k).clone();
			Ab.set(k, (ArrayList<Double>) Ab.get(max_location).clone());
			Ab.set(max_location, tpmax);
			pivot=Ab.get(k).get(k);
			if (pivot==0) {
				continue;
			}
			mainLog.println("Ab2:"+Arrays.toString(Ab.toArray()));

			/*
			if (pivot==0){
				//swap
				ArrayList<Double> tp= (ArrayList<Double>) Ab.get(k).clone();
				Ab.set(k, (ArrayList<Double>) Ab.get(k+1).clone());
				Ab.set(k+1, tp);
				pivot=Ab.get(k).get(k);
				mainLog.println("swap pivot:"+pivot);
			}
			*/
			//mainLog.println("start reverse pivot:"+pivot);
			for (int j=k; j<Ab.get(0).size(); j++) {
				Ab.get(k).set(j, Ab.get(k).get(j)/pivot);
			}
			mainLog.println("Ab3:"+Arrays.toString(Ab.toArray()));

			for (int i= k+1; i<Ab.size(); i++) {
				pivot = Ab.get(i).get(k);
				for (int j=0; j<Ab.get(0).size(); j++) {
					Ab.get(i).set(j, Ab.get(i).get(j)-Ab.get(k).get(j)*pivot);
				}
			}
			//mainLog.println("Ab4:"+Arrays.toString(Ab.toArray()));

		}
		for (int k=Ab.size()-1; k>0;k--) {
			double pivot = Ab.get(k).get(k);
			//mainLog.println("pivot:"+pivot);

			if (pivot==0){
				continue;
			}
			//mainLog.println("pivot:"+pivot);

			for (int j=0; j<Ab.get(0).size();j++) {
				Ab.get(k).set(j, Ab.get(k).get(j)/pivot);
			}
			for (int i=k-1; i>=0; i--) {
				pivot=Ab.get(i).get(k);
				for (int j=0; j<Ab.get(0).size();j++) {
					Ab.get(i).set(j, Ab.get(i).get(j)-Ab.get(k).get(j)*pivot);
				}
			}
		}

		ArrayList w_new = new ArrayList<Double>();
		for (int i=0; i<Ab.size(); i++) {
			w_new.add(Ab.get(i).get(Ab.get(i).size()-1));
		}
		mainLog.println("solved Ab:"+Arrays.toString(Ab.toArray()));
		return w_new;
		/* Jacobi
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> x_old = new ArrayList<Double>();
		for (int i=0; i<objs.size(); i++) {
			x.add(1.0);
			x_old.add(1.5);
		}
		x.add(1.0);				
		x_old.add(1.0);
		int max_iteration = 999;
		for (int iteration=0; iteration<max_iteration; iteration++ ) {
			for (int i=0;i<x.size();i++) {
				x_old.set(i, x.get(i));
			}
			for (int i_objective=0; i_objective<objs.size()+1; i_objective++) {
				double tp = 0;
				mainLog.println("----");
				for (int j_objective=0; j_objective<objs.size()+1; j_objective++) {
					if (i_objective!=j_objective) {
						//tp += a_ij * xk_j;
						tp += A.get(i_objective).get(j_objective)* x_old.get(j_objective);
						mainLog.println("Aij:"+A.get(i_objective).get(j_objective));
						mainLog.println("xj:"+x_old.get(j_objective));
					}
				}
				x.set(i_objective, (b.get(i_objective)-tp)/(A.get(i_objective).get(i_objective)));
			}
		}
		*/
	}
	public boolean containsWithError(ArrayList<ArrayList<Double>> S, ArrayList<Double> u, double error_threshold){
		for (int i=0; i<S.size(); i++){
			double error =0.0;
			for (int j=0; j<u.size(); j++){
				error += Math.abs(S.get(i).get(j)-u.get(j));
			}
			if (error<error_threshold){
				return true;
			}
		}
		return false;
	}

//	combinations2(old_value_vectors, objs.size()-1, 0, subset, subsets);


    public void combinations2(ArrayList<ArrayList<Double>> arr, int len, int startPosition, ArrayList<ArrayList<Double>> result,  ArrayList<ArrayList<ArrayList<Double>>> subsets ){
        if (len == 0){
        	mainLog.println("one instace of combinations");
		    mainLog.println(Arrays.toString(result.toArray()));
            subsets.add((ArrayList<ArrayList<Double>> )result.clone());
            return;
        } 

        for (int i = startPosition; i <= arr.size()-len; i++){
            //result[result.length - len] = arr[i];
            result.set(result.size()-len, (ArrayList<Double>) arr.get(i));
            //result.add((ArrayList<Double>) arr.get(i));
            combinations2(arr, len-1, i+1, result, subsets);
            
        }
    }       

    public double innerProduct(ArrayList<Double> A, ArrayList<Double> B){
    	//compute innerproduct of vector A, B
    	double result=0.0;
    	if(A.size()!=B.size()){
    		mainLog.println("vectors should have same size");
    	}
    	else{
    		for (int i=0; i<A.size();i++){
    			result += ((double) A.get(i) )* ((double) B.get(i));
    		}
    	}
    	return result;
    }
    public double innerProduct(double [] A, ArrayList<Double> B){
    	//compute innerproduct of vector A, B
    	double result=0.0;
    	if(A.length!=B.size()){
    		mainLog.println("vectors should have same size");
    	}
    	else{
    		for (int i=0; i<B.size();i++){
    			result += (A[i] )* ((double) B.get(i));
    		}
    	}
    	return result;
    }
    public double  valueMOPOMDP (Belief b, POMDP pomdp , ArrayList<AlphaVector> A, ArrayList<Double> w){
    			
    		double result = 0.0;
    		for (int i=0; i<w.size(); i++){
    			result += ((double) w.get(i)) * ( A.get(i).getDotProduct(b.toDistributionOverStates(pomdp))) ;
    		}
    	return result;
    }
    public ArrayList<Double> adjustWeight(ArrayList<Double> weights, List<ExpressionReward> objs, Model model)throws PrismException
    {
    	// this function is to convert weigths for 'min' to 'max'
    	ArrayList<Double> weights_adjust_min_max = new ArrayList<Double>();

	    for (int i_weight=0;i_weight<objs.size();i_weight++){
			if (objs.get(i_weight).getRelopBoundInfo(constantValues).getMinMax(model.getModelType(), false).isMin()){
				weights_adjust_min_max.add(-1.0*((double) weights.get(i_weight)));
			}
			else{
				weights_adjust_min_max.add(((double) weights.get(i_weight)));
			}
	    }
	    return weights_adjust_min_max;
    }
    public ArrayList<Double> deQueue(ArrayList<ArrayList<Double>> priority_queue){
		ArrayList w_pop = new ArrayList<Double>();
		if (priority_queue.size()>0){
			double top_priority = -1;
			int top_priority_index =0;
			for (int i=0; i<priority_queue.size(); i++) {
				if (priority_queue.get(i).get(priority_queue.get(i).size()-1) >= top_priority) {
					top_priority = priority_queue.get(i).get(priority_queue.get(i).size()-1);
					top_priority_index = i;
				}
			}			


			w_pop = priority_queue.get(top_priority_index);
			priority_queue.remove(top_priority_index);
			w_pop.remove(w_pop.size()-1);
			
			/*
			//Ensure weights sum to 1
			double tp_sum=0.0;
			for (int i=0; i<w_pop.size()-1;i++){
				tp_sum += (double) w_pop.get(i);
			}
			w_pop.set(w_pop.size()-1, 1- tp_sum);
			*/
		}
		return w_pop;
    }
    public ArrayList<ArrayList<Double>> initialQueue(List<ExpressionReward> objs,  HashMap corner_to_value, Model model)throws PrismException
    {
    	ArrayList<ArrayList<Double>> priority_queue = new ArrayList<ArrayList<Double>> ();
    	// create initial value vector for the exterme corner point
    	ArrayList<Double> initial_value_vector_weight = new ArrayList<Double>();
    	for (int i=0; i<objs.size();i++){
			initial_value_vector_weight.add(-1.0);
    	}
    	
    	// initial value vector is adjust for "min, max"
    	// if (max max max) add (-inf, -inf, -inf)
    	initial_value_vector_weight = adjustWeight(initial_value_vector_weight, objs, model);
    	ArrayList<Double> initial_value_vector = new ArrayList<Double>();
    	for (int i=0; i<objs.size();i++){
			initial_value_vector.add(((double) initial_value_vector_weight.get(i)) * (Double.POSITIVE_INFINITY) );
    	}
    	ArrayList<ArrayList<Double>> initial_value_vector_sets = new ArrayList<ArrayList<Double>> ();
    	initial_value_vector_sets.add(initial_value_vector);
    	//add extreme points in the queue
		for (int i =0; i<objs.size(); i++) {
			ArrayList<Double> w = new ArrayList<Double>();
			for (int j =0; j<objs.size(); j++) {
				w.add(0.0);
			}
			w.set(i, 1.0); //Extremum
			corner_to_value.put((ArrayList<Double>) w.clone(), initial_value_vector_sets); // create a map from extrema to value vector
			w.add(Double.POSITIVE_INFINITY); //Add extrema with infinite priority
			priority_queue.add((ArrayList<Double>) w.clone());
		}

		mainLog.println("Initialize Q (weight, priority)"+Arrays.toString(priority_queue.toArray()));
		mainLog.println("initial corner_to_value");
		for (Object  key:corner_to_value.keySet()){
			ArrayList<ArrayList<Double>> tp = (ArrayList<ArrayList<Double>>) corner_to_value.get(key);
			for (int i=0; i< tp.size(); i++){
				mainLog.println(key+" -> "+ Arrays.toString(((ArrayList<Double>) tp.get(i) ).toArray()));
			}
		}
		return priority_queue;
    }

	/**
	 * Model check a Pareto sum multi-objective property and return the values for the statesOfInterest.
	 * * @param statesOfInterest the states of interest, see checkExpression()
	 */
    protected StateValues checkExpressionParetoMultiObjPOMDP(Model model, List<ExpressionReward> objs, BitSet statesOfInterest) throws PrismException{
		mainLog.println(objs.size()+">>>");
    	HashSet<List<Double>> paretoCurve = new HashSet<>();
		ArrayList<ArrayList<Double>> partial_CCS = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> partial_CCS_weights = new ArrayList<ArrayList<Double>>();

		//Line 2
		ArrayList<ArrayList<Double>> w_v_checked = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> vector_checked = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> weights_checked = new ArrayList<ArrayList<Double>>();
		
		HashMap <ArrayList<Double>, ArrayList<ArrayList<Double> >> corner_to_value = new HashMap < ArrayList<Double>, ArrayList<ArrayList<Double>>> ();
		HashMap <ArrayList<Double>, ArrayList<ArrayList<Double> >> value_to_corner = new HashMap < ArrayList<Double>, ArrayList<ArrayList<Double>>> ();
		int numUnobs = ((POMDP) model).getNumUnobservations();
		int numStates = model.getNumStates();
		
		//Line 3 
		ArrayList<ArrayList<Double>> priority_queue = new ArrayList<ArrayList<Double>>();
		//Line 4
		priority_queue= initialQueue(objs, corner_to_value, model);
		
		//line 5
		ArrayList<ArrayList<AlphaVector>> A_all = new ArrayList<ArrayList<AlphaVector>> (); //A set of alpha matrix
		ArrayList<Object> allActions = ((POMDPModelChecker) this).getAllActions((POMDP) model);
		for (int a=0; a<allActions.size();a++) { 
			ArrayList<AlphaVector> am = new ArrayList<AlphaVector> (); //alphaMatrix
			for (int i=0; i<objs.size();i++) {
				double Rmin = 0.1;
				double[] entries = new double[numStates];
				for (int j=0; j<numStates;j++) {
					entries[j] = Rmin; 
				}
				AlphaVector av = new AlphaVector(entries);
				av.setAction(a);
				am.add(av);
			}
			A_all.add(am);
		}
		//check
		for (int a=0; a<A_all.size();a++){
			ArrayList<AlphaVector> amtp= A_all.get(a);
			mainLog.println("action"+a);
			for (int i=0; i<amtp.size(); i++) {
				mainLog.println("object"+i);
				AlphaVector avtp = amtp.get(i);
					mainLog.println(avtp);
			}
		}
		/*
		ArrayList<ArrayList<AlphaVector>> A_tp = ((POMDPModelChecker) this).copyAlphaMatrixSet(A_all);;
		ArrayList<AlphaVector> amtpd = A_tp.get(0);
		amtpd.remove(0);
		AlphaVector atp = amtpd.get(0);
		double [] t  = new double [2];
		t[0]=999;
		atp = new AlphaVector(t);
		atp.setAction(0);
		mainLog.println("DDsadasdasddasdDDDDD");

		mainLog.print(atp);
		mainLog.println("A_allDDDDDDDDDD");
		for (int a=0; a<A_all.size();a++){
			ArrayList<AlphaVector> amtp= A_all.get(a);
			mainLog.println("action"+a);
			for (int i=0; i<amtp.size(); i++) {
				mainLog.println("object"+i);
				AlphaVector avtp = amtp.get(i);
					mainLog.println(avtp);
			}
		}
		mainLog.println("A_tpDDDDDDDDDD");
		for (int a=0; a<A_all.size();a++){
			ArrayList<AlphaVector> amtp= A_tp.get(a);
			mainLog.println("action"+a);
			for (int i=0; i<amtp.size(); i++) {
				mainLog.println("object"+i);
				AlphaVector avtp = amtp.get(i);
					mainLog.println(avtp);
			}
		}		
		*/
		//line 6
		ExpressionTemporal exprTemp = (ExpressionTemporal) objs.get(0).getExpression();
		BitSet target = checkExpression(model, exprTemp.getOperand2(), null).getBitSet();
		ArrayList<Belief> belief_set = ((POMDPModelChecker) this).randomExploreBeliefs((POMDP) model, target, statesOfInterest);

		while(priority_queue.size()>0){

			mainLog.println("Current Q (weight, priority) Before Pop"+Arrays.toString(priority_queue.toArray()));
			ArrayList<Double> w_pop = deQueue(priority_queue);
			mainLog.println("deque...");
			mainLog.println("Current Q (weight, priority) After Pop"+Arrays.toString(priority_queue.toArray()));
			
			//Line 9  Select the best A from A_all for each b \belong belief, give w
			ArrayList<ArrayList<AlphaVector>> Ar =  new ArrayList<ArrayList<AlphaVector>> ();
			for (int i=0; i<belief_set.size();i++){
				Belief belief_candidate =  belief_set.get(i);
				double value_candidate = -99999;
				int value_candidate_index =0;
				for (int j=0; j<A_all.size();j++){
					ArrayList<AlphaVector> A_candidate =  A_all.get(j);
					mainLog.println("belief..."+belief_set.size()+"A_candidate..."+A_candidate.size()+"*"+A_candidate.get(0).size()+"w_pop..."+w_pop.size());
					double value = valueMOPOMDP(belief_candidate, (POMDP) model, A_candidate, adjustWeight(w_pop,objs,model) );
					if (value>value_candidate){
						value_candidate = value;
						value_candidate_index = j;
					}
					mainLog.println("Adjust min max value..."+value);
				}
				Ar.add(((POMDPModelChecker) this).copyAlphaMatrix(A_all.get(value_candidate_index)));
				mainLog.println("belief:..."+belief_candidate);
				mainLog.println("best matrix:..."+ Arrays.toString((A_all.get(value_candidate_index)).toArray()));
			}
			
			//line 10 
			double eta = 1E-5;
			ArrayList<ArrayList<AlphaVector>> Aw = ((POMDPModelChecker) this).solveScalarizedPOMDP(Ar, belief_set, w_pop, eta);
			// Aw <- solveScalarizedPOMDP(Ar, B, w, Eta)
		}
		
    	// Dummy return value
		return StateValues.createFromSingleValue(TypeDouble.getInstance(), 0.0, model);
    }
    
    protected StateValues checkExpressionParetoMultiObjMDP(Model model, List<ExpressionReward> objs, BitSet statesOfInterest) throws PrismException{
		// Dummy return value
    	double threshold = 0.00001;
		HashSet<List<Double>> paretoCurve = new HashSet<>();
		ArrayList<ArrayList<Double>> partial_CCS = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> partial_CCS_weights = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> w_v_checked = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> vector_checked = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> weights_checked = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> priority_queue = new ArrayList<ArrayList<Double>>();
		HashMap <ArrayList<Double>, ArrayList<ArrayList<Double> >> corner_to_value = new HashMap < ArrayList<Double>, ArrayList<ArrayList<Double>>> ();
		HashMap <ArrayList<Double>, ArrayList<ArrayList<Double> >> value_to_corner = new HashMap < ArrayList<Double>, ArrayList<ArrayList<Double>>> ();
		priority_queue =  initialQueue(objs,  corner_to_value, model);

		// Random sampling:
		ArrayList<ArrayList<Double>> w_v_checked_rs = new ArrayList<ArrayList<Double>>();
		double rs =1;
		if (rs>0){
			if (objs.size() == 3) {
				for (int i =0;i<11;i++){
					double w1 = ((double) i )*0.1;
					for (int j=0; j<11; j++){
						double w2= ((double) j )*0.1;
						if (w1+w2<=1){
							double w3= 1-w1-w2;
							ArrayList<Double> weights = new ArrayList<>();
							weights.add(w1);
							weights.add(w2);
							weights.add(w3);
							StateValues sv = checkExpressionWeightedMultiObj(model, weights, objs, statesOfInterest);
							ArrayList<Double> point = (ArrayList<Double>) sv.getValue(model.getFirstInitialState());
							mainLog.println("weights: "+Arrays.toString(weights.toArray()));
							mainLog.println("Points: "+Arrays.toString(point.toArray()));
							paretoCurve.add(point);
							if((!containsWithError(w_v_checked_rs,point,1E-06)) || true){
								w_v_checked_rs.add(weights);
								w_v_checked_rs.add(point);
							}
							mainLog.println("\nPareto curve: " + paretoCurve);
							mainLog.println("w_v_checked: "+Arrays.toString(w_v_checked_rs.toArray()));
						}
					}
				}		
				mainLog.println("\n finishing Pareto curve: " + paretoCurve);
				mainLog.println("finishing w_v_checked: "+Arrays.toString(w_v_checked_rs.toArray()));
				//return StateValues.createFromSingleValue(TypeDouble.getInstance(), 0.0, model);
			}
			if (objs.size() == 2) {		
				//HashSet<List<Double>> paretoCurve = new HashSet<>();
				int numPoints = 10;
				for (int i = 0; i <= numPoints; i++) {
					double w1 = ((double) i) / numPoints;
					double w2 = 1.0 - w1;
					ArrayList<Double> weights = new ArrayList<>();
					weights.add(w1);
					weights.add(w2);

					StateValues sv = checkExpressionWeightedMultiObj(model, weights, objs, statesOfInterest);
					ArrayList<Double> point = (ArrayList<Double>) sv.getValue(model.getFirstInitialState());

					w_v_checked_rs.add(weights);
					w_v_checked_rs.add(point);
					paretoCurve.add(point);
					mainLog.println(w1 + ":" + w2 + " = " + point);
				}
				mainLog.println("\nPareto curve: " + paretoCurve);
			}
		}
	 	
		mainLog.println("****************************************************");



		while(priority_queue.size()>0){
			mainLog.println("+++++++++++");
			mainLog.println("Current Q (weight, priority) Before Pop"+Arrays.toString(priority_queue.toArray()));
			mainLog.println("Current paritial CCS Before Pop"+Arrays.toString(partial_CCS.toArray()));
			mainLog.println("Current paritial CCS weights Before Pop"+Arrays.toString(partial_CCS_weights.toArray()));
			mainLog.println("corner_to_value");

			if(corner_to_value.keySet().size()>0){
				mainLog.println(corner_to_value.keySet().size());
				mainLog.println(corner_to_value.keySet());
				for (Object  key:corner_to_value.keySet()){
					ArrayList<ArrayList<Double>> tp = (ArrayList<ArrayList<Double>>) corner_to_value.get(key);
					mainLog.println(key);
					mainLog.println(tp.size());

					if (tp.size()>0){
						for (int i=0; i< tp.size(); i++){
							mainLog.println(key+" -> "+ Arrays.toString(((ArrayList<Double>) tp.get(i) ).toArray()));
						}
					}
				}
			}
			mainLog.println("value_to_corner");
			if(value_to_corner.keySet().size()>0){
				for (Object  key:value_to_corner.keySet()){
					ArrayList<ArrayList<Double>> tp = (ArrayList<ArrayList<Double>>) value_to_corner.get(key);
					for (int i=0; i< tp.size(); i++){
						mainLog.println(key+" -> "+ Arrays.toString(((ArrayList<Double>) tp.get(i) ).toArray()));
					}
				}
			}
			mainLog.println("+++++++++++");

			ArrayList w_pop = deQueue(priority_queue);
			/*
			double top_priority = -1;
			int top_priority_index =0;
			ArrayList w_pop = new ArrayList<Double>();
			for (int i=0; i<priority_queue.size(); i++) {
				if (priority_queue.get(i).get(objs.size()) >= top_priority) {
					top_priority = priority_queue.get(i).get(objs.size());
					top_priority_index = i;
				}
			}

			//Ensure weights sum to 1
			double tp_sum=0.0;
			w_pop = priority_queue.get(top_priority_index);
			for (int i=0; i<objs.size()-1;i++){
				tp_sum += (double) w_pop.get(i);
			}
			w_pop.set(objs.size()-1, 1- tp_sum);
			w_pop.remove(objs.size());

			priority_queue.remove(top_priority_index);
			*/

			mainLog.println("Pop weight with top priority: "+Arrays.toString(w_pop.toArray()));
			mainLog.println("Current Q (weight, priority)  After pop"+Arrays.toString(priority_queue.toArray()));
			
			StateValues sv = checkExpressionWeightedMultiObj(model, w_pop, objs, statesOfInterest);
			ArrayList u = (ArrayList<Double>) sv.getValue(model.getFirstInitialState());
			mainLog.println("Value vector: "+u);
			
			int countNewWeights = 0; //number of weights generated by u


			w_v_checked.add((ArrayList<Double>) w_pop.clone());
			weights_checked.add((ArrayList<Double>) w_pop.clone());
			w_v_checked.add((ArrayList<Double>) u.clone());
			vector_checked.add((ArrayList<Double>) u.clone());

			mainLog.println("w_pop: "+Arrays.toString(w_pop.toArray()));
			mainLog.println("u: "+Arrays.toString(u.toArray()));
			mainLog.println("w_pop*u: "+innerProduct(w_pop,u));
			mainLog.println("w_v_checked"+Arrays.toString(w_v_checked.toArray()));


			//if (!partial_CCS.contains(u)) {
			if (!containsWithError(partial_CCS,u, 1E-5)) {
				if (partial_CCS.size()==0) {// when to compute
					for (int i_queue=0; i_queue<priority_queue.size();i_queue++){
						ArrayList<Double> weight_tp = (ArrayList<Double>) priority_queue.get(i_queue).clone();
						weight_tp.remove(weight_tp.size()-1);
						ArrayList<ArrayList<Double>> current_value_set = (ArrayList<ArrayList<Double>>) corner_to_value.get(weight_tp).clone();
						current_value_set.add(u);
						corner_to_value.put(weight_tp, current_value_set );

						ArrayList<ArrayList<Double>> weight_tp_set =  new ArrayList<ArrayList<Double>> ();
						if(value_to_corner.containsKey(u))
							weight_tp_set = (ArrayList<ArrayList<Double>>) value_to_corner.get(u).clone();
						weight_tp_set.add(weight_tp);
						value_to_corner.put(u,weight_tp_set);
					}
					mainLog.println("add value vector from 1st exterme weights");
					partial_CCS.add((ArrayList<Double>) u.clone());
					partial_CCS_weights.add((ArrayList<Double>) w_pop.clone());
					mainLog.println(partial_CCS.size());
				}
				else {

					mainLog.println("elsess");
					double original_value = innerProduct(adjustWeight(w_pop,objs, model), u);
					double other_prod = innerProduct(adjustWeight(w_pop,objs,model), corner_to_value.get(w_pop).get(corner_to_value.get(w_pop).size()-1));
					if ((original_value - other_prod)>1E-08){

						//remove from value dict
						ArrayList<ArrayList<Double>> existing_value_vectors = corner_to_value.get(w_pop);
						ArrayList<ArrayList<Double>> existing_weights = new ArrayList<ArrayList<Double>> () ;
						for (int i_evv=0; i_evv<existing_value_vectors.size(); i_evv++){
							ArrayList<Double> existing_value_vector = existing_value_vectors.get(i_evv);
							if (value_to_corner.containsKey(existing_value_vector)){
								existing_weights = value_to_corner.get(existing_value_vector);
								for (int j_ew=0; j_ew<existing_weights.size();j_ew++){
									ArrayList<Double> existing_weigth = existing_weights.get(j_ew);
									if ( containsWithError(existing_weights, w_pop, threshold) && (!(w_pop.contains(1.0)))){
										int tp_index = existing_weights.indexOf(w_pop);
										value_to_corner.get(existing_value_vector).remove(tp_index);
									}
								}
							}
						}

						//check obsolete 
						ArrayList<ArrayList<Double>> weight_list = new ArrayList<ArrayList<Double>> ();
						weight_list.add(w_pop);
						ArrayList<ArrayList<Double>> obsolete_list = new ArrayList<ArrayList<Double>> ();
						while (weight_list.size()>0){
							ArrayList<Double> weight = weight_list.get(weight_list.size()-1);
							weight_list.remove(weight_list.size()-1);
							if (corner_to_value.containsKey(weight)){
								existing_value_vectors = (ArrayList<ArrayList<Double>>) corner_to_value.get(weight).clone();
								ArrayList<Double> existing_value_vector = existing_value_vectors.get(existing_value_vectors.size()-1);
								double scalarized_value = innerProduct(existing_value_vector, adjustWeight(weight,objs,model));
								if (original_value>scalarized_value){
									for (int i_evv=0; i_evv<existing_value_vectors.size(); i_evv++){
										existing_value_vector = existing_value_vectors.get(i_evv);
										if (value_to_corner.containsKey(existing_value_vector)){

											mainLog.println("+++++++++corner_to_value");
											for (Object  key:corner_to_value.keySet()){
												ArrayList<ArrayList<Double>> tp = (ArrayList<ArrayList<Double>>) corner_to_value.get(key);
												for (int i=0; i< tp.size(); i++){
													mainLog.println(key+" -> "+ Arrays.toString(((ArrayList<Double>) tp.get(i) ).toArray()));
												}
											}
											mainLog.println("+++++++++value_to_corner");
											for (Object  key:value_to_corner.keySet()){
												ArrayList<ArrayList<Double>> tp = (ArrayList<ArrayList<Double>>) value_to_corner.get(key);
												for (int i=0; i< tp.size(); i++){
													mainLog.println(key+" -> "+ Arrays.toString(((ArrayList<Double>) tp.get(i) ).toArray()));
												}
											}
											ArrayList<ArrayList<Double>> current_weights = value_to_corner.get(existing_value_vector);
											for (int j_cw=0; j_cw<current_weights.size(); j_cw++){
												ArrayList<Double> current_weight = current_weights.get(j_cw);
												if ((!current_weights.equals(weight)) && (obsolete_list.contains(current_weight))){
													weight_list.add((ArrayList<Double>) current_weight.clone());
												}
											}											
										}
									}
									obsolete_list.add((ArrayList<Double>) weight.clone());
								}
							}
						}

						for (int iprint=0; iprint<obsolete_list.size();iprint++){
							mainLog.println("obsolete_list: "+Arrays.toString(obsolete_list.get(iprint).toArray())+"\n");
						}

						///compute new corner
						ArrayList<ArrayList<Double>> boundaries=new ArrayList<ArrayList<Double>>();
						for (int i_obs=0; i_obs<obsolete_list.size();i_obs++){
							ArrayList<Double> obsolete_weight = obsolete_list.get(i_obs);
							for (int j_obs=0; j_obs<obsolete_weight.size();j_obs++){
								ArrayList<Double> boundary = new ArrayList<Double> ();
								boundary.add((double) j_obs);
								if (((double) obsolete_weight.get(j_obs)==0) && (!boundaries.contains(boundary)))
									boundaries.add(boundary);
							}
						}

						ArrayList<ArrayList<Double>> old_value_vectors = new ArrayList<ArrayList<Double>>  ();

						for (int i_obs=0; i_obs<obsolete_list.size();i_obs++){
							ArrayList<Double> found_weight = obsolete_list.get(i_obs);
							ArrayList<ArrayList<Double>> found_values = corner_to_value.get(found_weight);
							for (int j_fv=0; j_fv<found_values.size(); j_fv++){
								ArrayList<Double> found_value = found_values.get(j_fv);
								mainLog.println("found_value"+Arrays.toString(found_value.toArray()));
								mainLog.println(!old_value_vectors.contains(found_value));
								mainLog.println("u"+Arrays.toString(u.toArray()));
								mainLog.println(!found_value.equals(u));
								if((!old_value_vectors.contains(found_value))&&(!found_value.equals(u))) {
									old_value_vectors.add((ArrayList<Double>) found_value.clone());
								}
							}
						}

						// /*
						old_value_vectors = new ArrayList<ArrayList<Double>>  ();

						for (int i_obs=0; i_obs<obsolete_list.size();i_obs++){
							double bestValue = innerProduct(adjustWeight((ArrayList<Double>) obsolete_list.get(i_obs), objs, model), partial_CCS.get(0));
							for (int j_partialCSS=0; j_partialCSS < partial_CCS.size(); j_partialCSS++){
								if (innerProduct(adjustWeight((ArrayList<Double>) obsolete_list.get(i_obs), objs, model), partial_CCS.get(j_partialCSS))>bestValue){
									bestValue = innerProduct(adjustWeight((ArrayList<Double>) obsolete_list.get(i_obs), objs, model), partial_CCS.get(j_partialCSS));
								}
							}
							for (int j_partialCSS=0; j_partialCSS < partial_CCS.size(); j_partialCSS++){
								if (Math.abs(innerProduct(adjustWeight((ArrayList<Double>) obsolete_list.get(i_obs), objs, model), partial_CCS.get(j_partialCSS))-bestValue)<1E-06){
									if (old_value_vectors.size()<objs.size()){ //if Vs(w) contians fewer than dvalue vecotrs
										old_value_vectors.add((ArrayList<Double>) partial_CCS.get(j_partialCSS).clone());
									}
								}
							}
						}


						for (int i_b=0; i_b<boundaries.size();i_b++){
							old_value_vectors.add((ArrayList<Double>)boundaries.get(i_b).clone());
						}

						for (int iprint=0; iprint<old_value_vectors.size();iprint++){
							mainLog.println("old_value_vectors: "+Arrays.toString(old_value_vectors.get(iprint).toArray())+"\n");
						}

						//compute new points
						//Add to queue
						ArrayList<ArrayList<Double>> subset = new ArrayList<ArrayList<Double>>(); //one kind of combination

						ArrayList<Double> tpp= new ArrayList<Double>();
						tpp.add(1.0);
						tpp.add(1.0);
						for (int i_ojs=0; i_ojs<objs.size()-1;i_ojs++){
							subset.add(tpp);
						}

						ArrayList<ArrayList<ArrayList<Double>>> subsets = new ArrayList<ArrayList<ArrayList<Double>>>(); //all combination
						
						mainLog.println("old_value_vectors size"+old_value_vectors.size());
						combinations2(old_value_vectors, objs.size()-1, 0, subset, subsets);

						mainLog.println("allsubsets: total number of combinations"+Arrays.toString(subsets.toArray())+subsets.size());
						
						for(int i_subset=0; i_subset<subsets.size();i_subset++){
							ArrayList<ArrayList<Double>> hCCS = new ArrayList<ArrayList<Double>>(); //optimistic hypothetical CCS
							ArrayList<ArrayList<Double>> A = new ArrayList<ArrayList<Double>>();
							ArrayList<Double> augumented_vector = new ArrayList<Double>();
							ArrayList<Double> bound_from_w_obsolete = new ArrayList<Double>();
							ArrayList<ArrayList<Double>> oneCombination =(ArrayList<ArrayList<Double>> ) subsets.get(i_subset).clone();
							for (int j=0; j<oneCombination.size(); j++){
								augumented_vector = (ArrayList<Double>) oneCombination.get(j).clone();
								if (augumented_vector.size()==objs.size()){
									hCCS.add((ArrayList<Double>) augumented_vector.clone());
									augumented_vector.add(-1.0);
									A.add(augumented_vector);
								}
								else{
									bound_from_w_obsolete.add((double) augumented_vector.get(0));
								}
							}
							hCCS.add((ArrayList<Double>) u.clone());
							augumented_vector = (ArrayList<Double>) u.clone();
							augumented_vector.add(-1.0);
							A.add((ArrayList<Double>) augumented_vector.clone());

							//simplex constraint
							ArrayList<Double> bound = new ArrayList<Double>();
							for (int i=0; i<objs.size();i++) {
								bound.add(1.0);
							}
							bound.add(0.0);
							A.add(bound);


							// remove column that has bound. (E.g. for weight [0.5,0.5,0], the boundary index is 2, then remove column 2 from A)
							
							for (int i_bf=0; i_bf<bound_from_w_obsolete.size();i_bf++){
								double removeIndex = (double) bound_from_w_obsolete.get(i_bf);
								int remove = (int) removeIndex;
								for (int i_A=0; i_A<A.size(); i_A++){
									A.get(i_A).remove(remove);
								}
							}
							
							ArrayList<Double> b = new ArrayList<Double>();
							for (int i=0; i<A.size()-1; i++) {
								b.add(0.0);
							}
							b.add(1.0);

							ArrayList w_new = new ArrayList<Double>();
							w_new = linSolver(A,b);
							w_new.remove(w_new.size()-1);

							for (int i_bf=0; i_bf<bound_from_w_obsolete.size();i_bf++){
								double insertIndex = (double) bound_from_w_obsolete.get(i_bf);
								int insert = (int) insertIndex;
								w_new.add(insert,0.0);
							}
							

							Boolean allPositive = true;
							for (int iw=0; iw<w_new.size();iw++){
								if ((double) w_new.get(iw)<0){
									allPositive = false;
								}
							}
							if (!allPositive){
								mainLog.println("Negative... continue");
								continue;
							}

							if (w_new.contains(1.0)){
								mainLog.println("Extreme... continue");
								continue;
							}
							if (w_new.contains(Double.NaN)){
								mainLog.println("NaN... continue");
								continue;
							}
							mainLog.println("generateing new weights by"+Arrays.toString(u.toArray()));
							
							countNewWeights++;
							mainLog.println(countNewWeights+"Number of New weights generated by :"+Arrays.toString(u.toArray()));
							mainLog.println("w_new"+Arrays.toString(w_new.toArray()));
							if (countNewWeights>objs.size())
								mainLog.println("More new weights than expected");

							//////// this is for rounding
							
							double weigth_sum = 0.0;
							for (int iw=0;iw<w_new.size()-1;iw++){
								w_new.set(iw, (double) Math.round((double)w_new.get(iw) * 1000000)/1000000);
								weigth_sum += (double) w_new.get(iw);
							}
							w_new.set(w_new.size()-1, (double) 1-weigth_sum);
							mainLog.println("w_new (sum to 1)"+Arrays.toString(w_new.toArray()));
							

							//update value_to_corner & corner_to_value
							ArrayList<ArrayList<Double>> default_value_vecotrs = new  ArrayList<ArrayList<Double>>();
							default_value_vecotrs.add((ArrayList<Double>) u.clone());
							corner_to_value.put((ArrayList<Double>) w_new.clone(), default_value_vecotrs);

							ArrayList<ArrayList<Double>> new_weights = new  ArrayList<ArrayList<Double>>();
							new_weights.add((ArrayList<Double>) w_new.clone());
							value_to_corner.put(u,new_weights);


							// Add to priority queue
							double priority=1.0;

							mainLog.println("computing priority = "+hCCS.size());

							//priority = maxValueLP(w_new,partial_CCS,W)
							if (hCCS.size()==objs.size()){	
								ArrayList<ArrayList<Double>> A_tp = new ArrayList<ArrayList<Double>> ();
								ArrayList<Double> b_tp = new ArrayList<Double> ();

								for (int i_hCCS=0; i_hCCS<hCCS.size(); i_hCCS++){
									ArrayList<Double> value_tp = (ArrayList<Double>) hCCS.get(i_hCCS);
									//ArrayList<Double> weight_tp = (ArrayList<Double>) value_to_corner.get(value_tp).get(value_to_corner.get(value_tp).size()-1);
									ArrayList<Double> weight_tp =(ArrayList<Double>) w_v_checked.get(w_v_checked.indexOf(value_tp)-1);
									double v_sw = innerProduct(value_tp,weight_tp);
									A_tp.add((ArrayList<Double>) weight_tp.clone());
									b_tp.add(v_sw); 
								}
								ArrayList<Double> h_value = linSolver(A_tp,b_tp);
								double v_ccs = innerProduct(h_value,w_new);
								double v_sw = innerProduct(u, w_new);
								priority = Math.abs((v_ccs-v_sw)/v_ccs);
								mainLog.println("computing priority = "+priority);
							}
							

							if (priority > threshold){
								w_new.add(priority);
								priority_queue.add((ArrayList<Double>) w_new.clone());
							}

							//delete obsolete list from priority queue
							for (int i_obs=0; i_obs<obsolete_list.size();i_obs++){
								ArrayList<Double> obsolete_weight =  obsolete_list.get(i_obs);
								if (!obsolete_weight.contains(1.0)){
									for (int j_queue=0; j_queue<priority_queue.size(); j_queue++){
										ArrayList<Double> tp_weight = (ArrayList<Double>) priority_queue.get(j_queue).clone();
										tp_weight.remove(tp_weight.size()-1);
										if (tp_weight.equals(obsolete_weight)){
											mainLog.println("Removing obsolete_weight:"+ Arrays.toString(obsolete_weight.toArray()));
											priority_queue.remove(j_queue);
										}

									}
								}
							}

						}
					}
				//
				//add to solution
				partial_CCS.add((ArrayList<Double>) u.clone());
				partial_CCS_weights.add((ArrayList<Double>) w_pop.clone());
				}

				///////////////////////////////////////////////////////
				/*mainLog.println("TO.........herehere.delete obsolete");
				

				if(priority_queue.size()>0){
					for (int i_Q=0;i_Q<priority_queue.size();i_Q++){
						mainLog.println("iq "+i_Q);

						ArrayList<Double> tp_w =(ArrayList<Double> ) priority_queue.get(i_Q).clone();
						mainLog.println("weight in queue: "+Arrays.toString(tp_w.toArray()));

						tp_w.remove(objs.size());
						mainLog.println("weight in queue: "+Arrays.toString(tp_w.toArray())+"current value w*u: "+innerProduct(tp_w,u));
						if (innerProduct(adjustWeight(tp_w,objs,model),u)- innerProduct(adjustWeight(w_pop,objs,model),u)> 1E-06){
							mainLog.println("Weigth is obsolete.");
							mainLog.println("weight in queue: "+Arrays.toString(tp_w.toArray())+"current value w*u: "+innerProduct(tp_w,u));
							//priority_queue.remove(i_Q); //comment to delete obsolete weights in queue //uncomment to keep all weights in queue
						}
					}
				}
			

				//compute new corner weights
				if (partial_CCS.size()<objs.size()-1) {// when to compute
					mainLog.println("add value vector from exterme weights");
					partial_CCS.add((ArrayList<Double>) u.clone());
					partial_CCS_weights.add((ArrayList<Double>) w_pop.clone());
					mainLog.println(partial_CCS.size());
				}
				else {
					mainLog.println("Compute new corner weights");
					
					//need (objs.size()-1) vectors to compute a new corner weight
					//n_vectors_needed = objs.size()-1;

					ArrayList<ArrayList<Double>> subset = new ArrayList<ArrayList<Double>>(); //one kind of combination
					ArrayList<ArrayList<ArrayList<Double>>> subsets = new ArrayList<ArrayList<ArrayList<Double>>>(); //all combination
					for (int i=0; i<objs.size()-1;i++){
						subset.add(u);
					}
					combinations2(partial_CCS, objs.size()-1, 0, subset, subsets);

					mainLog.println("allsubsets: total number of combinations"+Arrays.toString(subsets.toArray())+subsets.size());

					//select (n_vectors_needed) from (particial_CCS)
					int countNewWeights=0;

					for (int i_subsets=0;i_subsets<subsets.size();i_subsets++){
						ArrayList<ArrayList<Double>> A = new ArrayList<ArrayList<Double>>();

						ArrayList<ArrayList<Double>> oneCombination =(ArrayList<ArrayList<Double>> ) subsets.get(i_subsets).clone();
						for (int j=0; j<oneCombination.size(); j++){
							ArrayList<Double> augumented_vector = new ArrayList<Double>();
							augumented_vector = (ArrayList<Double>) oneCombination.get(j).clone();
							augumented_vector.add(-1.0);
							A.add(augumented_vector);
						}
						ArrayList<Double> augumented_vector = new ArrayList<Double>();
						augumented_vector =(ArrayList<Double>) u.clone();
						augumented_vector.add(-1.0);
						A.add(augumented_vector);
						
						ArrayList<Double> bound = new ArrayList<Double>();
						for (int i=0; i<objs.size();i++) {
							bound.add(1.0);
						}
						bound.add(0.0);
						A.add(bound);

						ArrayList<Double> b = new ArrayList<Double>();
						for (int i=0; i<objs.size(); i++) {
							b.add(0.0);
						}
						b.add(1.0);
						
						ArrayList w_new = new ArrayList<Double>();
						w_new = linSolver(A,b);
						w_new.remove(w_new.size()-1);

						double tp_sum = 0.0;
						for (int iw=0;iw<w_new.size()-1;iw++){
							w_new.set(iw, (double) Math.round((double)w_new.get(iw) * 1000000)/1000000);
							tp_sum += (double) w_new.get(iw);
						}
						countNewWeights++;
						mainLog.println(countNewWeights+"Number of New weights generated by :"+Arrays.toString(u.toArray()));
						mainLog.println("w_new"+Arrays.toString(w_new.toArray()));

						w_new.set(w_new.size()-1, (double) 1-tp_sum);

						mainLog.println("w_new (sum to 1)"+Arrays.toString(w_new.toArray()));

						// w_new.set(w_new.size()-1,  1- ((double) w_new.get(0)));


						boolean is_w_new_positive = true;
						for(int iw=0; iw<w_new.size();iw++){
							if ((double) w_new.get(iw)<0) {
								mainLog.print("Negative weigths not supported");
								is_w_new_positive = false;
							}
						}

						if (is_w_new_positive){
							if (containsWithError(weights_checked,w_new,1E-6)){
								mainLog.println("weigths already checked");
							}
							else{
								boolean AlreadyBetter = false;
						
								// enable this block can reduce the number of weights to serach
								// currently only works for ('max' 'max' 'max') problems
								

								for (int i_par=0; i_par<partial_CCS.size();i_par++){
									if (!containsWithError(oneCombination, (ArrayList<Double>) partial_CCS.get(i_par),1E-06)){
									    mainLog.println(Arrays.toString(oneCombination.toArray()));
									    
									    ArrayList<Double> weights_adjust_min_max = new ArrayList<Double>();

									    for (int i_weight=0;i_weight<objs.size();i_weight++){
											if (objs.get(i_weight).getRelopBoundInfo(constantValues).getMinMax(model.getModelType(), false).isMin()){
												weights_adjust_min_max.add(-1.0*((double) w_new.get(i_weight)));
											}
											else{
												weights_adjust_min_max.add(((double) w_new.get(i_weight)));
											}
									    }

										if (innerProduct((ArrayList<Double>) partial_CCS.get(i_par),weights_adjust_min_max) - innerProduct(u,weights_adjust_min_max)> 1E-6){
											mainLog.println("Already better vector");
											mainLog.println("Better vecotr:"+Arrays.toString(partial_CCS.get(i_par).toArray())+"value: "+ innerProduct((ArrayList<Double>) partial_CCS.get(i_par),w_new) );
											mainLog.println("current vecotr:"+Arrays.toString(u.toArray())+"value: "+ innerProduct(u,w_new) );
											mainLog.println("difference:"+(innerProduct((ArrayList<Double>) partial_CCS.get(i_par),w_new) - innerProduct(u,w_new)));
											mainLog.println("weights_adjust_min_max:"+Arrays.toString(weights_adjust_min_max.toArray()));
											AlreadyBetter =true;
										}
									}
								}
								//boolean AlreadyBetter = false; //Uncomment to get all intersections; //comment to get intersections with surfaces.


								if (!AlreadyBetter){
									mainLog.println("valid new weights");

									//compute priority
									double priority = 1;
									
									//Ax=b
									//wu=b
									A = new ArrayList<ArrayList<Double>>();
									b = new ArrayList<Double>();

									A.add((ArrayList<Double>) w_pop.clone());
									double b2=0.0;
									for (int i=0; i<objs.size(); i++) {
										b2 += ((double) u.get(i))*((double) w_pop.get(i));
									}
									b.add(b2);

									for(int i_c=0; i_c<oneCombination.size();i_c++){
										ArrayList<Double> tp_vector = (ArrayList<Double>) oneCombination.get(i_c).clone();
										int id_tp_vector =partial_CCS.indexOf(tp_vector);
										ArrayList<Double> tp_weights = (ArrayList<Double>) partial_CCS_weights.get(id_tp_vector).clone();
										A.add((ArrayList<Double>) tp_weights);

										double b1=0.0;
										for (int i=0; i<objs.size(); i++) {
											b1 += tp_weights.get(i)* tp_vector.get(i);
											//(partial_CCS.get(i_partial_CCS).get(i))*(partial_CCS_weights.get(i_partial_CCS).get(i));
										}
										b.add(b1);
									}
									
									ArrayList<Double> super_vector = linSolver(A, b);
									mainLog.println("New super"+Arrays.toString(super_vector.toArray()));
									
									double super_vector_value = 0.0;
									for (int i=0; i<objs.size(); i++){
										super_vector_value += (super_vector.get(i)) *((double) w_new.get(i));
									}
									double u_value = 0.0;
									for (int i=0; i<objs.size(); i++){
										u_value += ((double ) (u.get(i)))*((double) (w_new.get(i)));
									}
									priority = (super_vector_value-u_value)/super_vector_value;
									priority = Math.abs(priority);
									
									mainLog.println("super_vector_value"+super_vector_value);
									mainLog.println("u_value"+u_value);
									
									if (priority > threshold){
										w_new.add(priority);
										priority_queue.add(w_new);
									}
								}
							}
						}
					}

					
					partial_CCS.add((ArrayList<Double>) u.clone());
					partial_CCS_weights.add((ArrayList<Double>) w_pop.clone());
				}
				*/
			}
			else {
				mainLog.println("Vector already in the partial CCS");

			}
		}
		//mainLog.println("Checked weights and values:"+Arrays.toString(w_v_checked.toArray()));
		//mainLog.println("Pareto Curve by OLS: "+Arrays.toString(partial_CCS.toArray()));
		mainLog.println("ALl weights checked:"+ weights_checked.size());
		HashSet values_OLS = new HashSet();

		for (int iprint=0; iprint<weights_checked.size();iprint++){
			mainLog.print("weight: "+Arrays.toString(weights_checked.get(iprint).toArray())+"; vector: "+Arrays.toString(vector_checked.get(iprint).toArray())+"\n");
		}
		mainLog.println("*********************ParetoCurve by optimal linear solution*******************************");

		for (int iprint=0; iprint<partial_CCS.size();iprint++){
			mainLog.print("weight: "+Arrays.toString(partial_CCS_weights.get(iprint).toArray())+"; vector: "+Arrays.toString(partial_CCS.get(iprint).toArray())+"\n");
			values_OLS.add(innerProduct(partial_CCS_weights.get(iprint),partial_CCS.get(iprint)));

		}
		mainLog.print("#weights: "+partial_CCS_weights.size()+"\n");
		for (int iprint=0; iprint<partial_CCS.size();iprint++){
			mainLog.print(Arrays.toString(partial_CCS_weights.get(iprint).toArray())+"\n");
		}
		mainLog.println("#Parecto Curve points: "+partial_CCS.size()+"\n");
		for (int iprint=0; iprint<partial_CCS.size();iprint++){
			mainLog.print(Arrays.toString(partial_CCS.get(iprint).toArray())+"\n");
		}
		//mainLog.println("scalarized values OLS:"+values_OLS.size());
		//mainLog.println(values_OLS);
		
		mainLog.println("*********************ParetoCurve by iterating [w1 w2] Random Sampling*******************************");
		
		mainLog.println("\nPareto curve: " +paretoCurve.size()+"\n"+ paretoCurve);
		ArrayList<ArrayList<Double>> paretoCurveCompact = new ArrayList<ArrayList<Double>>();
		for (int iprint=0; iprint<paretoCurve.size(); iprint++){
			if (!containsWithError(paretoCurveCompact, (ArrayList<Double>) paretoCurve.toArray()[iprint],1E-06)){
				paretoCurveCompact.add((ArrayList<Double>)paretoCurve.toArray()[iprint]);
			}
		}
		mainLog.println("\nPareto curve (compact)" + paretoCurveCompact.size());

		for (int iprint=0; iprint<paretoCurve.size(); iprint++){
				mainLog.println(paretoCurve.toArray()[iprint]);
		}


		HashSet values_rs = new HashSet();
		mainLog.println("Weights and vectors checked by random sampling start: ");
		for (int iprint=0; iprint<w_v_checked_rs.size()/2;iprint++){
			mainLog.print("weight: "+Arrays.toString(w_v_checked_rs.get(iprint*2).toArray())+"; vector: "+Arrays.toString(w_v_checked_rs.get(iprint*2+1).toArray())+"\n");
			values_rs.add(innerProduct(w_v_checked_rs.get(iprint*2),w_v_checked_rs.get(iprint*2+1)));
		}
		mainLog.println("Weights and vectors checked by random sampling end: ");
		//mainLog.println("scalarized values RS:"+values_rs.size());
		//mainLog.println(values_rs);
		// Dummy return value
		return StateValues.createFromSingleValue(TypeDouble.getInstance(), 0.0, model);
    }
	protected StateValues checkExpressionParetoMultiObj(Model model, List<ExpressionReward> objs, BitSet statesOfInterest) throws PrismException
	{	
		switch (model.getModelType()) {
		case POMDP:
			 return checkExpressionParetoMultiObjPOMDP(model, objs, statesOfInterest);
		case MDP:
			return checkExpressionParetoMultiObjMDP(model, objs, statesOfInterest);
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle ");
		}
		
		
	}
	
	/**
	 * Model check a P operator expression and return the values for the statesOfInterest.
 	 * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionProb(Model model, ExpressionProb expr, BitSet statesOfInterest) throws PrismException
	{
		// Use the default semantics for a standalone P operator
		// (i.e. quantification over all strategies, and no game-coalition info)
		return checkExpressionProb(model, expr, true, null, statesOfInterest);
	}
	
	/**
	 * Model check a P operator expression and return the values for the states of interest.
	 * @param model The model
	 * @param expr The P operator expression
	 * @param forAll Are we checking "for all strategies" (true) or "there exists a strategy" (false)? [irrelevant for numerical (=?) queries] 
	 * @param coalition If relevant, info about which set of players this P operator refers to (null if irrelevant)
	 * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionProb(Model model, ExpressionProb expr, boolean forAll, Coalition coalition, BitSet statesOfInterest) throws PrismException
	{
		// Get info from P operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType(), forAll);

		// Compute probabilities
		StateValues probs = checkProbPathFormula(model, expr.getExpression(), minMax, statesOfInterest);

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}

		// For =? properties, just return values; otherwise compare against bound
		if (!opInfo.isNumeric()) {
			probs.applyPredicate(v -> opInfo.apply((double) v, probs.getAccuracy()));
		}
		return probs;
	}

	/**
	 * Compute probabilities for the contents of a P operator.
	 * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkProbPathFormula(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and whether we want to use the corresponding algorithms
		boolean useSimplePathAlgo = expr.isSimplePathFormula();

		if (useSimplePathAlgo &&
		    settings.getBoolean(PrismSettings.PRISM_PATH_VIA_AUTOMATA) &&
		    LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expr)) {
			// If PRISM_PATH_VIA_AUTOMATA is true, we want to use the LTL engine
			// whenever possible
			useSimplePathAlgo = false;
		}

		if (useSimplePathAlgo) {
			return checkProbPathFormulaSimple(model, expr, minMax, statesOfInterest);
		} else {
			return checkProbPathFormulaLTL(model, expr, false, minMax, statesOfInterest);
		}
	}

	/**
	 * Compute probabilities for a simple, non-LTL path operator.
	 */
	protected StateValues checkProbPathFormulaSimple(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		boolean negated = false;
		StateValues probs = null;

		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);

		// Negation
		if (expr instanceof ExpressionUnaryOp &&
		    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
			negated = true;
			minMax = minMax.negate();
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}

		if (expr instanceof ExpressionTemporal) {
 			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;

			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(model, exprTemp, minMax, statesOfInterest);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(model, exprTemp, minMax, statesOfInterest);
				} else {
					probs = checkProbUntil(model, exprTemp, minMax, statesOfInterest);
				}
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		if (negated) {
			// Subtract from 1 for negation
			probs.applyFunction(TypeDouble.getInstance(), v -> 1.0 - (double) v);
		}

		return probs;
	}

	/**
	 * Compute probabilities for a next operator.
	 */
	protected StateValues checkProbNext(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Model check the operand for all states
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

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
			throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
	}

	/**
	 * Compute probabilities for a bounded until operator.
	 */
	protected StateValues checkProbBoundedUntil(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// This method just handles discrete time
		// Continuous-time model checkers will override this method

		// Get info from bounded until
		Integer lowerBound;
		IntegerBound bounds;
		int i;

		// get and check bounds information
		bounds = IntegerBound.fromExpressionTemporal(expr, constantValues, true);

		// Model check operands for all states
		BitSet remain = checkExpression(model, expr.getOperand1(), null).getBitSet();
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

		if (bounds.hasLowerBound()) {
			lowerBound = bounds.getLowestInteger();
		} else {
			lowerBound = 0;
		}

		Integer windowSize = null;  // unbounded

		if (bounds.hasUpperBound()) {
			windowSize = bounds.getHighestInteger() - lowerBound;
		}

		// compute probabilities for Until<=windowSize
		StateValues sv = null;

		if (windowSize == null) {
			// unbounded
			ModelCheckerResult res = null;
			switch (model.getModelType()) {
			case DTMC:
				res = ((DTMCModelChecker) this).computeUntilProbs((DTMC) model, remain, target);
				break;
			case MDP:
				res = ((MDPModelChecker) this).computeUntilProbs((MDP) model, remain, target, minMax.isMin());
				break;
			case STPG:
				res = ((STPGModelChecker) this).computeUntilProbs((STPG) model, remain, target, minMax.isMin1(), minMax.isMin2());
				break;
			default:
				throw new PrismException("Cannot model check " + expr + " for " + model.getModelType() + "s");
			}
			result.setStrategy(res.strat);
			sv = StateValues.createFromDoubleArrayResult(res, model);
		} else if (windowSize == 0) {
			// A trivial case: windowSize=0 (prob is 1 in target states, 0 otherwise)
			sv = StateValues.createFromBitSetAsDoubles(target, model);
		} else {
			// Otherwise: numerical solution
			ModelCheckerResult res = null;

			switch (model.getModelType()) {
			case DTMC:
				res = ((DTMCModelChecker) this).computeBoundedUntilProbs((DTMC) model, remain, target, windowSize);
				break;
			case MDP:
				res = ((MDPModelChecker) this).computeBoundedUntilProbs((MDP) model, remain, target, windowSize, minMax.isMin());
				break;
			case STPG:
				res = ((STPGModelChecker) this).computeBoundedUntilProbs((STPG) model, remain, target, windowSize, minMax.isMin1(), minMax.isMin2());
				break;
			default:
				throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
			}
			result.setStrategy(res.strat);
			sv = StateValues.createFromDoubleArrayResult(res, model);
		}

		// perform lowerBound restricted next-step computations to
		// deal with lower bound.
		if (lowerBound > 0) {
			double[] probs = sv.getDoubleArray();

			for (i = 0; i < lowerBound; i++) {
				switch (model.getModelType()) {
				case DTMC:
					probs = ((DTMCModelChecker) this).computeRestrictedNext((DTMC) model, remain, probs);
					break;
				case MDP:
					probs = ((MDPModelChecker) this).computeRestrictedNext((MDP) model, remain, probs, minMax.isMin());
					break;
				case STPG:
					// TODO (JK): Figure out if we can handle lower bounds for STPG in the same way
					throw new PrismNotSupportedException("Lower bounds not yet supported for STPGModelChecker");
				default:
					throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
				}
			}

			sv = StateValues.createFromDoubleArray(probs, model);
			sv.setAccuracy(AccuracyFactory.boundedNumericalIterations());
		}

		return sv;
	}

	/**
	 * Compute probabilities for an (unbounded) until operator.
	 */
	protected StateValues checkProbUntil(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Model check operands for all states
		BitSet remain = checkExpression(model, expr.getOperand1(), null).getBitSet();
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

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
			break;
		case POMDP:
			res = ((POMDPModelChecker) this).computeReachProbs((POMDP) model, remain, target, minMax.isMin(), statesOfInterest);
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeUntilProbs((STPG) model, remain, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
	}

	/**
	 * Compute probabilities for an LTL path formula
	 */
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// To be overridden by subclasses
		throw new PrismNotSupportedException("Computation not implemented yet");
	}

	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionReward(Model model, ExpressionReward expr, BitSet statesOfInterest) throws PrismException
	{
		// Use the default semantics for a standalone R operator
		// (i.e. quantification over all strategies, and no game-coalition info)
		return checkExpressionReward(model, expr, true, null, statesOfInterest);
	}
	
	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionReward(Model model, ExpressionReward expr, boolean forAll, Coalition coalition, BitSet statesOfInterest) throws PrismException
	{
		// Get info from R operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType(), forAll);

		// Build rewards
		int r = expr.getRewardStructIndexByIndexObject(rewardGen, constantValues);
		mainLog.println("Building reward structure...");
		Rewards rewards = constructRewards(model, r);

		// Compute rewards
		StateValues rews = checkRewardFormula(model, rewards, expr.getExpression(), minMax, statesOfInterest);

		// Print out rewards
		if (getVerbosity() > 5) {
			mainLog.print("\nRewards (non-zero only) for all states:\n");
			rews.print(mainLog);
		}

		// For =? properties, just return values; otherwise compare against bound
		if (!opInfo.isNumeric()) {
			rews.applyPredicate(v -> opInfo.apply((double) v, rews.getAccuracy()));
		}
		return rews;
	}

	/**
	 * Construct rewards for the reward structure with index r of the reward generator and a model.
	 * Ensures non-negative rewards.
	 * <br>
	 * Note: Relies on the stored RewardGenerator for constructing the reward structure.
	 */
	protected Rewards constructRewards(Model model, int r) throws PrismException
	{
		return constructRewards(model, r, false);
	}

	/**
	 * Construct rewards for the reward structure with index r of the reward generator and a model.
	 * <br>
	 * If {@code allowNegativeRewards} is true, the rewards may be positive and negative, i.e., weights.
	 * <br>
	 * Note: Relies on the stored RewardGenerator for constructing the reward structure.
	 */
	protected Rewards constructRewards(Model model, int r, boolean allowNegativeRewards) throws PrismException
	{
		ConstructRewards constructRewards = new ConstructRewards(this);
		if (allowNegativeRewards)
			constructRewards.allowNegativeRewards();
		return constructRewards.buildRewardStructure(model, rewardGen, r);
	}

	/**
	 * Compute rewards for the contents of an R operator.
	 */
	protected StateValues checkRewardFormula(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		StateValues rewards = null;

		if (expr.getType() instanceof TypePathDouble) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_I:
				rewards = checkRewardInstantaneous(model, modelRewards, exprTemp, minMax, statesOfInterest);
				break;
			case ExpressionTemporal.R_C:
				if (exprTemp.hasBounds()) {
					rewards = checkRewardCumulative(model, modelRewards, exprTemp, minMax);
				} else {
					rewards = checkRewardTotal(model, modelRewards, exprTemp, minMax);
				}
				break;
			case ExpressionTemporal.R_S:
				rewards = checkRewardSteady(model, modelRewards);
				break;
			default:
				throw new PrismNotSupportedException("Explicit engine does not yet handle the " + exprTemp.getOperatorSymbol() + " reward operator");
			}
		} else if (expr.getType() instanceof TypePathBool || expr.getType() instanceof TypeBool) {
			rewards = checkRewardPathFormula(model, modelRewards, expr, minMax, statesOfInterest);
		}

		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		return rewards;
	}

	/**
	 * Compute rewards for an instantaneous reward operator.
	 */
	protected StateValues checkRewardInstantaneous(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC: {
			int k = expr.getUpperBound().evaluateInt(constantValues);
			res = ((DTMCModelChecker) this).computeInstantaneousRewards((DTMC) model, (MCRewards) modelRewards, k, statesOfInterest);
			break;
		}
		case CTMC: {
			double t = expr.getUpperBound().evaluateDouble(constantValues);
			res = ((CTMCModelChecker) this).computeInstantaneousRewards((CTMC) model, (MCRewards) modelRewards, t);
			break;
		}
		case MDP: {
			int k = expr.getUpperBound().evaluateInt(constantValues);
			res = ((MDPModelChecker) this).computeInstantaneousRewards((MDP) model, (MDPRewards) modelRewards, k, minMax.isMin());
			break;
		}
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
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
			throw new PrismNotSupportedException("This is not a cumulative reward operator");
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
			StateValues res = StateValues.createFromSingleValue(TypeDouble.getInstance(), 0.0, model);
			res.setAccuracy(AccuracyFactory.doublesFromQualitative());
			return res;
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
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
	}

	/**
	 * Compute expected rewards for a total reward operator.
	 */
	protected StateValues checkRewardTotal(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		// Check that there is no upper time bound
		if (expr.getUpperBound() != null) {
			throw new PrismException("This is not a total reward operator");
		}

		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeTotalRewards((DTMC) model, (MCRewards) modelRewards);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeTotalRewards((CTMC) model, (MCRewards) modelRewards);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeTotalRewards((MDP) model, (MDPRewards) modelRewards, minMax.isMin());
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
	}

	/**
	 * Compute expected rewards for a steady-state reward operator.
	 */
	protected StateValues checkRewardSteady(Model model, Rewards modelRewards) throws PrismException
	{
		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeSteadyStateRewards((DTMC) model, (MCRewards) modelRewards);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeSteadyStateRewards((CTMC) model, (MCRewards) modelRewards);
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the steady-state reward operator for " + model.getModelType() + "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
	}

	/**
	 * Compute rewards for a path formula in a reward operator.
	 */
	protected StateValues checkRewardPathFormula(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		if (Expression.isReach(expr)) {
			return checkRewardReach(model, modelRewards, (ExpressionTemporal) expr, minMax, statesOfInterest);
		}
		else if (Expression.isCoSafeLTLSyntactic(expr, true)) {
			return checkRewardCoSafeLTL(model, modelRewards, expr, minMax, statesOfInterest);
		}
		throw new PrismException("R operator contains a path formula that is not syntactically co-safe: " + expr);
	}
	
	/**
	 * Compute rewards for a reachability reward operator.
	 */
	protected StateValues checkRewardReach(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// No time bounds allowed
		if (expr.hasBounds()) {
			throw new PrismNotSupportedException("R operator cannot contain a bounded F operator: " + expr);
		}
		
		// Model check the operand for all states
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

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
			break;
		case POMDP:
			res = ((POMDPModelChecker) this).computeReachRewards((POMDP) model, (MDPRewards) modelRewards, target, minMax.isMin(), statesOfInterest);
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeReachRewards((STPG) model, (STPGRewards) modelRewards, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArrayResult(res, model);
	}

	/**
	 * Compute rewards for a co-safe LTL reward operator.
	 */
	protected StateValues checkRewardCoSafeLTL(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// To be overridden by subclasses
		throw new PrismException("Computation not implemented yet");
	}

	/**
	 * Model check an S operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionSteadyState(Model model, ExpressionSS expr) throws PrismException
	{
		// Get info from S operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType());

		// Compute probabilities
		StateValues probs = checkSteadyStateFormula(model, expr.getExpression(), minMax);

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}

		// For =? properties, just return values; otherwise compare against bound
		if (!opInfo.isNumeric()) {
			probs.applyPredicate(v -> opInfo.apply((double) v, probs.getAccuracy()));
		}
		return probs;
	}

	/**
	 * Compute steady-state probabilities for an S operator.
	 */
	protected StateValues checkSteadyStateFormula(Model model, Expression expr, MinMax minMax) throws PrismException
	{
		// Model check operand for all states
		BitSet b = checkExpression(model, expr, null).getBitSet();

		// Compute/return the probabilities
		switch (model.getModelType()) {
		case DTMC:
			return ((DTMCModelChecker) this).computeSteadyStateFormula((DTMC) model, b);
		case CTMC:
			return ((CTMCModelChecker) this).computeSteadyStateFormula((CTMC) model, b);
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the S operator for " + model.getModelType() + "s");
		}
	}

	// Utility methods for probability distributions

	/**
	 * Generate a probability distribution, stored as a StateValues object, from a file.
	 * If {@code distFile} is null, so is the return value.
	 */
	public StateValues readDistributionFromFile(File distFile, Model model) throws PrismException
	{
		StateValues dist = null;

		if (distFile != null) {
			mainLog.println("\nImporting probability distribution from file \"" + distFile + "\"...");
			dist = StateValues.createFromFile(TypeDouble.getInstance(), distFile, model);
		}

		return dist;
	}

	/**
	 * Build a probability distribution, stored as a StateValues object,
	 * from the initial states info of the current model: either probability 1 for
	 * the (single) initial state or equiprobable over multiple initial states.
	 */
	public StateValues buildInitialDistribution(Model model) throws PrismException
	{
		int numStates = model.getNumStates();
		if (numStates == 1) {
			int sInit = model.getFirstInitialState();
			return StateValues.create(TypeDouble.getInstance(), s -> s == sInit ? 1.0 : 0.0, model);
		} else {
			double pInit = 1.0 / numStates;
			return StateValues.create(TypeDouble.getInstance(), s -> model.isInitialState(s) ? pInit : 0.0, model);
		}
	}
	
	/**
	 * Export (non-zero) state rewards for one reward structure of a model.
	 * @param model The model
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param out Where to export
	 */
	public void exportStateRewardsToFile(Model model, int r, int exportType, PrismLog out) throws PrismException
	{
		int numStates = model.getNumStates();
		int nonZeroRews = 0;

		if (exportType != Prism.EXPORT_PLAIN) {
			throw new PrismNotSupportedException("Exporting state rewards in the requested format is currently not supported by the explicit engine");
		}

		Rewards modelRewards = constructRewards(model, r);
		switch (model.getModelType()) {
		case DTMC:
		case CTMC:
			MCRewards mcRewards = (MCRewards) modelRewards;
			for (int s = 0; s < numStates; s++) {
				double d = mcRewards.getStateReward(s);
				if (d != 0) {
					nonZeroRews++;
				}
			}
			out.println(numStates + " " + nonZeroRews);
			for (int s = 0; s < numStates; s++) {
				double d = mcRewards.getStateReward(s);
				if (d != 0) {
					out.println(s + " " + PrismUtils.formatDouble(d));
				}
			}
			break;
		case MDP:
		case STPG:
			MDPRewards mdpRewards = (MDPRewards) modelRewards;
			for (int s = 0; s < numStates; s++) {
				double d = mdpRewards.getStateReward(s);
				if (d != 0) {
					nonZeroRews++;
				}
			}
			out.println(numStates + " " + nonZeroRews);
			for (int s = 0; s < numStates; s++) {
				double d = mdpRewards.getStateReward(s);
				if (d != 0) {
					out.println(s + " " + PrismUtils.formatDouble(d));
				}
			}
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet export state rewards for " + model.getModelType() + "s");
		}
	}
}
