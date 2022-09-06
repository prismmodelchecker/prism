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

import java.io.*;
import java.util.*;

import prism.*;
import explicit.ProbModelChecker.TermCrit;
import explicit.ProbModelChecker.ValIterDir;

/**
 * Base class for implementing quantitative abstraction-refinement loop.
 * Subclasses need to implement the following abstract methods:
 * <ul>
 * <li> {@link #initialise}
 * <li> {@link #splitState}
 * <li> {@link #rebuildAbstraction}
 * </ul>
 * Various settings are available. In particular, model/property type should be set with:
 * <ul>
 * <li> {@link #setModelType}
 * <li> {@link #setPropertyType}
 * </ul>
 * See other setXXX methods for further configuration options.
 * <br><br>
 * The abstraction-refinement loop can then be started with {@link #abstractRefine}.
 */
public abstract class QuantAbstractRefine extends PrismComponent
{
	// Model checker
	protected ProbModelChecker mc;
	// Dummy model checker to store options
	protected ProbModelChecker mcOptions;

	// Flags/settings

	// Verbosity level
	protected int verbosity = 0;
	// Maximum number of refinement iterations
	protected int maxRefinements = 1000;
	// Export abstractions to dot files at each refinement?
	protected boolean exportDot = false;
	// Optimise by reusing numerical solution etc.
	protected boolean optimise = true;
	// Refinement termination criteria
	protected RefineTermCrit refineTermCrit = RefineTermCrit.ABSOLUTE;
	// Parameter for refinement termination criteria
	protected double refineTermCritParam = 1e-6;
	// Use convergence from above for value iteration?
	protected boolean above = false;
	// Abstraction-refinement settings
	protected RefineStratWhere refineStratWhere = RefineStratWhere.ALL;
	protected RefineStratHow refineStratHow = RefineStratHow.VAL;

	// Private flags/settings
	protected boolean sanityChecks = false;

	// Enums for flags/settings

	// Property type
	public enum PropertyType {
		PROB_REACH, EXP_REACH, PROB_REACH_BOUNDED;
	};

	// Refinement termination criteria
	public enum RefineTermCrit {
		ABSOLUTE, RELATIVE
	};

	public enum RefineStratWhere {
		ALL, ALL_MAX, FIRST, FIRST_MAX, LAST, LAST_MAX
	};

	public enum RefineStratHow {
		ALL, VAL
	};

	// Concrete model info
	/** Type of (concrete) model; default is MDP. */
	protected ModelType modelType = ModelType.MDP;
	
	// Property info
	/** Property type; default is PROB_REACH */
	protected PropertyType propertyType = PropertyType.PROB_REACH;
	/** For nondeterministic (concrete) models, compute min? (or max?) */
	protected boolean min;
	/** Bound for when property is bounded reachability */
	protected int reachBound = 0;
	
	// Abstract model info (updated by subclasses)
	/** Abstract model */
	protected NondetModelSimple abstraction;
	/** BitSet of (abstract) target states for property to drive refinement */
	protected BitSet target;
	
	// Stuff for refinement loop
	protected ModelType abstractionType;
	protected double[] lbSoln;
	protected double[] ubSoln;
	protected double[] lbLastSoln;
	protected double[] ubLastSoln;
	protected double lbInit;
	protected double ubInit;
	protected BitSet known;
	protected List<Integer> refineStates;
	protected boolean buildEmbeddedDtmc = false; // Construct DTMC from CTMC?
	// Timing info, etc.
	protected double timeBuild;
	protected double timeRebuild;
	protected double timeCheck;
	protected double timeCheckLb;
	protected double timeCheckUb;
	protected double timeCheckPre;
	protected double timeCheckProb0;
	protected double timeRefine;
	protected double timeTotal;
	protected int itersTotal;
	protected int refinementNum;

	/**
	 * Default constructor.
	 */
	public QuantAbstractRefine(PrismComponent parent) throws PrismException
	{
		super(parent);
		// Create dummy model checker to store options
		try {
			mcOptions = new ProbModelChecker(null);
		} catch (PrismException e) {
			// Won't happen
		}
	}

	/**
	 * Provides access to the underlying model checker for the purpose of setting options. 
	 */
	public ProbModelChecker getModelChecker()
	{
		return mcOptions;
	}

	/**
	 * Print summary of current settings.
	 */
	public void printSettings()
	{
		mainLog.print("\nAR Settings:");
		mainLog.print(" modelType = " + modelType);
		mainLog.print(" propertyType = " + propertyType);
		mainLog.print(" reachBound = " + reachBound);
		mainLog.print(" verbosity = " + verbosity);
		mainLog.print(" maxRefinements = " + maxRefinements);
		mainLog.print(" exportDot = " + exportDot);
		mainLog.print(" optimise = " + optimise);
		mainLog.print(" refineTermCrit = " + refineTermCrit);
		mainLog.print(" refineTermCritParam = " + refineTermCritParam);
		mainLog.print(" above = " + above);
		mainLog.print(" refineStratWhere = " + refineStratWhere);
		mainLog.print(" refineStratHow = " + refineStratHow);
		mainLog.println();
		mainLog.print("\nMC Settings: ");
		mcOptions.printSettings();
		mainLog.println();
	}

	// Set methods for flags/settings, etc.

	public void setModelType(ModelType modelType)
	{
		this.modelType = modelType;
	}

	public void setPropertyType(PropertyType propertyType)
	{
		this.propertyType = propertyType;
	}

	public void setReachBound(int reachBound)
	{
		this.reachBound = reachBound;
	}

	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
		// Store this in model checker options too
		if (mcOptions != null)
			mcOptions.setVerbosity(verbosity);
	}

	public void setMaxRefinements(int maxRefinements)
	{
		this.maxRefinements = maxRefinements;
	}

	public void setExportDot(boolean exportDot)
	{
		this.exportDot = exportDot;
	}

	public void setOptimise(boolean optimise)
	{
		this.optimise = optimise;
	}

	public void setRefineTermCrit(RefineTermCrit refineTermCrit)
	{
		this.refineTermCrit = refineTermCrit;
	}

	public void setRefineTermCritParam(double refineTermCritParam)
	{
		this.refineTermCritParam = refineTermCritParam;
	}

	public void setAbove(boolean above)
	{
		this.above = above;
	}

	public void setRefineStratWhere(RefineStratWhere refineStratWhere)
	{
		this.refineStratWhere = refineStratWhere;
	}

	public void setRefineStratHow(RefineStratHow refineStratHow)
	{
		this.refineStratHow = refineStratHow;
	}

	/**
	 * Convenience method: parse an option specified as a (command-line style) string
	 */
	public void parseOption(String opt) throws PrismException
	{
		int j;
		String optVal;

		// Ignore empty option
		if ("".equals(opt))
			return;

		// Break switch up into parts if contains =
		if ((j = opt.indexOf('=')) != -1) {
			optVal = opt.substring(j + 1);
			opt = opt.substring(0, j);
		} else {
			optVal = null;
		}
		// Parse
		if (opt.equals("verbose") || opt.equals("v")) {
			try {
				setVerbosity((optVal == null) ? 10 : Integer.parseInt(optVal));
			} catch (NumberFormatException e) {
				throw new PrismNotSupportedException("Invalid value \"" + optVal + "\" for abstraction-refinement setting \"" + opt + "\"");
			}
		} else if (opt.matches("refine")) {
			if (optVal != null) {
				String ss[] = optVal.split(",");
				if (ss.length > 0) {
					if (ss[0].equals("all"))
						setRefineStratWhere(RefineStratWhere.ALL);
					else if (ss[0].equals("allmax"))
						setRefineStratWhere(RefineStratWhere.ALL_MAX);
					else if (ss[0].equals("first"))
						setRefineStratWhere(RefineStratWhere.FIRST);
					else if (ss[0].equals("firstmax"))
						setRefineStratWhere(RefineStratWhere.FIRST_MAX);
					else if (ss[0].equals("last"))
						setRefineStratWhere(RefineStratWhere.LAST);
					else if (ss[0].equals("lastmax"))
						setRefineStratWhere(RefineStratWhere.LAST_MAX);
					else
						throw new PrismException("Unknown refinement option \"" + ss[0] + "\"");
				}
				if (ss.length > 1) {
					if (ss[1].equals("all"))
						setRefineStratHow(RefineStratHow.ALL);
					else if (ss[1].equals("val"))
						setRefineStratHow(RefineStratHow.VAL);
					else
						throw new PrismException("Unknown refinement option \"" + ss[1] + "\"");
				}
			}
		} else if (opt.equals("epsilonref") || opt.equals("eref")) {
			if (optVal != null) {
				try {
					setRefineTermCritParam(Double.parseDouble(optVal));
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value \"" + optVal + "\" for abstraction-refinement setting \"" + opt + "\"");
				}
			}
		} else if (opt.equals("nopre")) {
			getModelChecker().setPrecomp(false);
		} else if (opt.equals("pre")) {
			getModelChecker().setPrecomp(true);
		} else if (opt.equals("noprob0")) {
			getModelChecker().setProb0(false);
		} else if (opt.equals("noprob1")) {
			getModelChecker().setProb1(false);
		} else if (opt.equals("epsilon")) {
			if (optVal != null) {
				try {
					getModelChecker().setTermCritParam(Double.parseDouble(optVal));
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value \"" + optVal + "\" for abstraction-refinement setting \"" + opt + "\"");
				}
			}
		} else if (opt.equals("maxrefs")) {
			if (optVal != null) {
				try {
					setMaxRefinements(Integer.parseInt(optVal));
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value \"" + optVal + "\" for abstraction-refinement setting \"" + opt + "\"");
				}
			}
		} else if (opt.equals("opt")) {
			setOptimise(true);
		} else if (opt.equals("noopt")) {
			setOptimise(false);
		} else if (opt.equals("exportdot")) {
			setExportDot(true);
		} else if (opt.equals("above")) {
			setAbove(true);
		} else if (opt.equals("below")) {
			setAbove(false);
		} else {
			throw new PrismException("Unknown switch " + opt);
		}
	}

	/**
	 * Convenience method: parse multiple options specified as a (command-line style) string
	 */
	public void parseOptions(String[] opts) throws PrismException
	{
		if (opts.length > 0)
			for (String opt : opts)
				parseOption(opt);
	}

	/**
	 * Print bulleted list of options to a log (used by -help switch). 
	 */
	public static void printOptions(PrismLog mainLog)
	{
		mainLog.println(" * verbose=<n> (or v=<n>) - verbosity level");
		mainLog.println(" * refine=<where,how> - which states to refine and how");
		mainLog.println("     <where> = all, allmax, first, firstmax, last, lastmax");
		mainLog.println("     <how> = all, val");
		mainLog.println(" * epsilonref=<x> (or eref=<x>) - epsilon for refinement");
		mainLog.println(" * nopre  - disable precomputation");
		mainLog.println(" * pre - use precomputation");
		mainLog.println(" * noprob0 - disable prob0 precomputation");
		mainLog.println(" * noprob1 - disable prob1 precomputation");
		mainLog.println(" * epsilon=<x> - epsilon for numerical convergence");
		mainLog.println(" * maxref=<n> - maximum number of refinements");
		mainLog.println(" * opt - use optimisations");
		mainLog.println(" * noopt - disable optimisations");
		mainLog.println(" * exportdot - export dot files for each refinement");
		mainLog.println(" * above - start numerical soluton from above");
		mainLog.println(" * below - start numerical soluton from below");
	}
	
	// Abstract methods that must be implemented for abstraction-refinement loop

	/**
	 * Initialise: Build initial abstraction and set of target abstract states
	 * (storing in {@link #abstraction} and {@link #target}, respectively).
	 * Note: {@link #abstraction} must respect initial states,
	 * i.e. any abstract state contains either all/no initial states. 
	 */
	protected abstract void initialise() throws PrismException;

	/**
	 * Split an abstract state for refinement, based on sets of nondeterministic choices.
	 * This function should add new states to the abstract model ({@link #abstraction}),
	 * update its initial states and update the {@link #target}. Any information stored locally
	 * about abstract state space etc. should also be updated at this point.
	 * <br><br>
	 * One of the new states should replace the state being split;
	 * the rest should be appended to the list of abstract states.
	 * Abstract states that need to be rebuilt (either because they are new, or because
	 * one of their successors has been split) can either be rebuilt at this point,
	 * or left until later. If the former, the state should be added to the list {@code rebuiltStates};
	 * if the latter, it should be added to {@code rebuildStates}.
	 * The total number of new states should be returned.
	 * <br><br>
	 * Notes:
	 * <ul>
	 * <li> The union of all the sets in {@link #choiceLists} may not cover all choices in the state to be split.
	 *      This is because there may be more efficient ways to compute the remainder of the abstract state.
	 *      If not, use the utility function {@link #addRemainderIntoChoiceLists}.
	 * </ul>
	 * @param splitState State to split.
	 * @param choiceLists Lists of nondeterministic choices defining split.
	 * @param rebuildStates States that need rebuilding as a result should be added here.
	 * @return Number of states into which split (i.e. 1 denotes split failed).
	 */
	protected abstract int splitState(int splitState, List<List<Integer>> choiceLists, Set<Integer> rebuiltStates,
			Set<Integer> rebuildStates) throws PrismException;

	/**
	 * Rebuild the abstraction after a refinement.
	 * @param rebuildStates States that need rebuilding.
	 */
	protected abstract void rebuildAbstraction(Set<Integer> rebuildStates) throws PrismException;

	// Remaining implementation of abstraction refinement loop.

	/**
	 * Execute abstraction-refinement loop.
	 * Return result for initial state(s).
	 */
	public double abstractRefine(boolean min) throws PrismException
	{
		int i, n;
		boolean canRefine = false;
		long timer, timerTotal;
		String initAbstractionInfo;

		// Store whether min/max
		this.min = min;

		// For some models, properties, we might need to change
		// what the model/abstraction type is
		if (modelType == ModelType.CTMC && propertyType == PropertyType.PROB_REACH) {
			buildEmbeddedDtmc = true;
			modelType = ModelType.DTMC;
		}

		// Store what abstract model type is
		// and create appropriate model checker
		// (which inherits log/options from mcOptions)
		switch (modelType) {
		case DTMC:
			abstractionType = ModelType.MDP;
			mc = new MDPModelChecker(null);
			break;
		case CTMC:
			abstractionType = ModelType.CTMDP;
			mc = new CTMDPModelChecker(null);
			break;
		case MDP:
			abstractionType = ModelType.STPG;
			mc = new STPGModelChecker(null);
			break;
		default:
			throw new PrismNotSupportedException("Cannot handle model type " + modelType);
		}
		mc.inheritSettings(mcOptions);
		// But limit verbosity (since model checking will be done many times)
		//mc.setVerbosity(verbosity - 1);

		// Init timers, etc.
		timerTotal = System.currentTimeMillis();
		timeBuild = timeRebuild = timeCheck = timeRefine = 0;
		timeCheckLb = timeCheckUb = timeCheckPre = timeCheckProb0 = 0;
		itersTotal = 0;

		// Build first abstraction (and state sets)
		mainLog.println("\nBuilding initial " + abstractionType + "...");
		timer = System.currentTimeMillis();
		initialise();
		initAbstractionInfo = abstraction.infoString();
		timer = System.currentTimeMillis() - timer;
		timeBuild += timer / 1000.0;
		if (verbosity >= 2) {
			mainLog.println(abstractionType + " constructed in " + (timer / 1000.0) + " secs.");
			mainLog.println(abstractionType + ": " + abstraction.infoString());
		}
		if (verbosity >= 10) {
			mainLog.println(abstractionType + ": " + abstraction);
		}

		// Initialise model checking info
		n = abstraction.getNumStates();
		lbSoln = Utils.bitsetToDoubleArray(target, n);
		ubSoln = new double[n];
		for (i = 0; i < n; i++)
			ubSoln[i] = 1.0;
		known = (BitSet) target.clone();

		// Refinement loop
		refinementNum = 0;
		while (true) {
			if (exportDot)
				exportToDotFile("abstr" + refinementNum + ".dot", abstraction, known, lbSoln, ubSoln);
			//while (cheapCheckRefine() > 0) ;
			modelCheckAbstraction(min);
			if (refinementNum >= maxRefinements)
				break;
			canRefine = chooseStatesToRefine();
			if (!canRefine)
				break;
			refinementNum++;
			refine(refineStates);
			if (verbosity >= 10) {
				mainLog.println(abstractionType + ": " + abstraction);
			}
		}

		// Finish up
		timerTotal = System.currentTimeMillis() - timerTotal;
		timeTotal = timerTotal / 1000.0;
		printFinalSummary(initAbstractionInfo, canRefine);

		// Return result (avg of lower/upper bounds)
		return (lbInit + ubInit) / 2;
	}

	protected int cheapCheckRefine() throws PrismException
	{
		if (propertyType != PropertyType.PROB_REACH)
			return 0;

		mainLog.println("cheap...");
		int i, n, count, numNewStates;
		double lb, ub;
		long timer;
		n = abstraction.getNumStates();
		count = 0;
		i = -1;
		while (i < n) {
			// Pick next state
			i = (known == null) ? i + 1 : known.nextClearBit(i + 1);
			// TODO: check use of nextClearBit here
			if (i < 0)
				break;

			//log.println("YY "+i+" "+((STPG)abstraction).steps.get(i));
			if (((STPG) abstraction).allSuccessorsInSet(i, known)) {
				// Compute... note lbsoln for both is ok sinec = ubsoln
				lb = ((STPG) abstraction).mvMultMinMaxSingle(i, lbSoln, true, min);
				ub = ((STPG) abstraction).mvMultMinMaxSingle(i, lbSoln, false, min);
				mainLog.println(((STPGAbstrSimple) abstraction).getChoices(i));
				mainLog.println("XX " + i + ": old=[" + lbSoln[i] + "," + ubSoln[i] + "], new=[" + lb + "," + ub + "]");
				if (PrismUtils.doublesAreClose(ub, lb, refineTermCritParam, refineTermCrit == RefineTermCrit.ABSOLUTE)) {
					lbSoln[i] = ubSoln[i] = lb;
					known.set(i);
					count++;
				}
				//
				else {
					HashSet<Integer> rebuildStates = new HashSet<Integer>();
					timer = System.currentTimeMillis();
					numNewStates = refineState(i, null, rebuildStates);
					timer = System.currentTimeMillis() - timer;
					timeRefine += timer / 1000.0;
					if (numNewStates > 1) {
						timer = System.currentTimeMillis();
						rebuildAbstraction(rebuildStates);
						timer = System.currentTimeMillis() - timer;
						timeRebuild += timer / 1000.0;
						mainLog.println("rebuildStates: " + rebuildStates);
						count++;
						for (int j = 0; j < numNewStates; j++) {
							int a = (j == 0) ? i : abstraction.getNumStates() - numNewStates + j;
							lb = ((STPG) abstraction).mvMultMinMaxSingle(a, lbSoln, true, min);
							ub = ((STPG) abstraction).mvMultMinMaxSingle(a, lbSoln, false, min);
							mainLog.println("XXX " + a + ": old=[" + lbSoln[a] + "," + ubSoln[a] + "], new=[" + lb
									+ "," + ub + "]");
							lbSoln[a] = lbLastSoln[a] = lb;
							ubSoln[a] = ubLastSoln[a] = ub;
							if (PrismUtils.doublesAreClose(ub, lb, refineTermCritParam,
									refineTermCrit == RefineTermCrit.ABSOLUTE)) {
								lbSoln[a] = ubSoln[a] = lb;
								known.set(a);
								count++;
							}
						}
						//	return count;
					}
				}
			}
		}
		return count;
	}

	/**
	 * Model check the abstraction; store results in lbSoln, ubSoln.
	 */
	protected void modelCheckAbstraction(boolean min) throws PrismException
	{
		int i, n, numInitialStates;
		long timer;

		// Start model checking
		mainLog.println("\nModel checking " + abstractionType + "...");
		timer = System.currentTimeMillis();

		// Do model checking to compute lower/upper bounds
		// (depends on type of property being checked)
		switch (propertyType) {
		case PROB_REACH:
			modelCheckAbstractionProbReach(min);
			break;
		case PROB_REACH_BOUNDED:
			modelCheckAbstractionReachBounded(min);
			break;
		case EXP_REACH:
			modelCheckAbstractionExpReach(min);
			break;
		default:
			throw new PrismNotSupportedException("Property type " + propertyType + " not supported");
		}

		// See if each state has "converged", i.e. bounds are close enough to assume exact
		n = abstraction.getNumStates();
		for (i = 0; i < n; i++) {
			known.set(i, PrismUtils.doublesAreClose(ubSoln[i], lbSoln[i], refineTermCritParam,
					refineTermCrit == RefineTermCrit.ABSOLUTE));
		}

		// Compute bounds for initial states
		lbInit = ubInit = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		for (int j : abstraction.getInitialStates()) {
			if (verbosity >= 10)
				mainLog.println("Init " + j + ": " + lbSoln[j] + "-" + ubSoln[j]);
			if (min) {
				lbInit = Math.min(lbInit, lbSoln[j]);
				ubInit = Math.min(ubInit, ubSoln[j]);
			} else {
				lbInit = Math.max(lbInit, lbSoln[j]);
				ubInit = Math.max(ubInit, ubSoln[j]);
			}
		}

		// Model checking done
		timer = System.currentTimeMillis() - timer;
		timeCheck += timer / 1000.0;
		mainLog.println(abstractionType + " model checked in " + (timer / 1000.0) + " secs.");

		// Display results
		mainLog.println(known.cardinality() + "/" + n + " states converged.");
		numInitialStates = abstraction.getNumInitialStates();
		mainLog.print("Diff across ");
		mainLog.print(numInitialStates + " initial state" + (numInitialStates == 1 ? "" : "s") + ": ");
		mainLog.println(ubInit - lbInit);
		mainLog.print("Lower/upper bounds for ");
		mainLog.print(numInitialStates + " initial state" + (numInitialStates == 1 ? "" : "s") + ": ");
		mainLog.println(lbInit + " - " + ubInit);
	}

	/**
	 * Do model checking for probabilistic reachability; store results in lbSoln, ubSoln.
	 */
	protected void modelCheckAbstractionProbReach(boolean min) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n;

		// Compute lower bounds
		switch (abstractionType) {
		case MDP:
			if (optimise && refinementNum > 0) {
				mc.setValIterDir(MDPModelChecker.ValIterDir.BELOW);
				// TODO
			} else {
				res = ((MDPModelChecker) mc).computeReachProbs((MDP) abstraction, target, true);
			}
			break;
		case CTMDP:
			if (optimise && refinementNum > 0) {
				mc.setValIterDir(MDPModelChecker.ValIterDir.BELOW);
				// TODO
			} else {
				res = ((CTMDPModelChecker) mc).computeReachProbs((CTMDP) abstraction, target, true);
			}
			break;
		case STPG:
			if (optimise && refinementNum > 0) {
				mc.setValIterDir(MDPModelChecker.ValIterDir.BELOW);
				res = ((STPGModelChecker) mc).computeReachProbs((STPG) abstraction, null, target, true, min, lbSoln, known);
			} else {
				res = ((STPGModelChecker) mc).computeReachProbs((STPG) abstraction, null, target, true, min, null, null);
			}
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + abstractionType);
		}
		lbSoln = res.soln;
		lbLastSoln = lbSoln; // TODO: fix (if nec.)
		timeCheckLb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;
		//mainLog.println(lbSoln);

		// Compute upper bounds
		switch (abstractionType) {
		case MDP:
			if (optimise) {
				// TODO
			} else {
				res = ((MDPModelChecker) mc).computeReachProbs((MDP) abstraction, target, false);
			}
			break;
		case CTMDP:
			if (optimise) {
				// TODO
			} else {
				res = ((CTMDPModelChecker) mc).computeReachProbs((CTMDP) abstraction, target, false);
			}
			break;
		case STPG:
			if (optimise) {
				if (above) {
					mc.setValIterDir(ValIterDir.ABOVE);
					res = ((STPGModelChecker) mc).computeReachProbs((STPG) abstraction, null, target, false, min, ubSoln, known);
				} else {
					mc.setValIterDir(ValIterDir.BELOW);
					double lbCopy[] = Utils.cloneDoubleArray(lbSoln);
					res = ((STPGModelChecker) mc).computeReachProbs((STPG) abstraction, null, target, false, min, lbCopy, known);
				}
			} else {
				res = ((STPGModelChecker) mc).computeReachProbs((STPG) abstraction, null, target, false, min, null, null);
			}
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + abstractionType);
		}
		ubSoln = res.soln;
		ubLastSoln = ubSoln; // TODO: fix (if nec.)
		timeCheckUb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;
		//mainLog.println(ubSoln);
	}

	/*
	 * Do model checking for bounded probabilistic reachability; store results in lbSoln, ubSoln.
	 */
	protected void modelCheckAbstractionReachBounded(boolean min) throws PrismException
	{
		ModelCheckerResult res;
		double results[] = new double[reachBound + 1];

		// Compute lower bounds
		switch (abstractionType) {
		case MDP:
			res = ((MDPModelChecker) mc).computeBoundedReachProbs((MDP) abstraction, null, target, reachBound, true, null, results);
			break;
		case CTMDP:
			res = ((CTMDPModelChecker) mc).computeBoundedReachProbs((CTMDP) abstraction, null, target, (double) reachBound, true,
					null, results);
			break;
		case STPG:
			res = ((STPGModelChecker) mc).computeBoundedReachProbs((STPG) abstraction, null, target, reachBound, true, min, null,
					results);
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + abstractionType);
		}
		lbSoln = res.soln;
		lbLastSoln = res.lastSoln;
		timeCheckLb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;

		mainLog.print("#");
		for (int i = 0; i < reachBound + 1; i++)
			mainLog.print(" " + results[i]);
		mainLog.println();

		// Compute upper bounds
		switch (abstractionType) {
		case MDP:
			res = ((MDPModelChecker) mc).computeBoundedReachProbs((MDP) abstraction, null, target, reachBound, false, null, results);
			break;
		case CTMDP:
			res = ((CTMDPModelChecker) mc).computeBoundedReachProbs((CTMDP) abstraction, null, target, (double) reachBound, false,
					null, results);
			break;
		case STPG:
			res = ((STPGModelChecker) mc).computeBoundedReachProbs((STPG) abstraction, null, target, reachBound, false, min, null,
					results);
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + abstractionType);
		}
		ubSoln = res.soln;
		ubLastSoln = res.lastSoln;
		timeCheckUb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;

		mainLog.print("#");
		for (int i = 0; i < reachBound + 1; i++)
			mainLog.print(" " + results[i]);
		mainLog.println();
	}

	/**
	 * Do model checking for expected reachability; store results in lbSoln, ubSoln.
	 */
	protected void modelCheckAbstractionExpReach(boolean min) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n;

		// Pass settings to model checker
		mc.termCrit = TermCrit.RELATIVE;
		mc.termCritParam = 1e-8;

		// Value iteration parameters depend on optimisations used 
		switch (abstractionType) {
		case MDP:
			if (optimise && refinementNum > 0) {
				res = ((MDPModelChecker) mc).computeReachRewards((MDP) abstraction, null, target, true, lbSoln, known);
			} else {
				res = ((MDPModelChecker) mc).computeReachRewards((MDP) abstraction, null, target, true, null, null);
			}
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + abstractionType);
		}
		lbSoln = res.soln;
		lbLastSoln = lbSoln; // TODO: fix (if nec.)
		timeCheckLb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;

		// Compute upper bounds
		switch (abstractionType) {
		case MDP:
			if (optimise) {
				double lbCopy[] = Utils.cloneDoubleArray(lbSoln);
				res = ((MDPModelChecker) mc).computeReachRewards((MDP) abstraction, null, target, false, lbCopy, known);
			} else {
				res = ((MDPModelChecker) mc).computeReachRewards((MDP) abstraction, null, target, false, null, null);
			}
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + abstractionType);
		}
		ubSoln = res.soln;
		ubLastSoln = ubSoln; // TODO: fix (if nec.)
		timeCheckUb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;
	}

	/*
	 * Pick states to refine.
	 */
	protected boolean chooseStatesToRefine() throws PrismException
	{
		ArrayList<Integer> refinableStates;
		double maxDiff, bound;
		int i, numStates;

		// Empty refinement states lists
		refinableStates = new ArrayList<Integer>();
		refineStates = new ArrayList<Integer>();

		// Check max diff in bounds over initial states
		// (computed after numerical solution)
		if (PrismUtils.doublesAreClose(ubInit, lbInit, refineTermCritParam, refineTermCrit == RefineTermCrit.ABSOLUTE))
			return false;

		// Compute max diff in bounds over all states
		numStates = abstraction.getNumStates();
		maxDiff = ubSoln[0] - lbSoln[0];
		for (i = 1; i < numStates; i++)
			if (ubSoln[i] - lbSoln[i] > maxDiff)
				maxDiff = ubSoln[i] - lbSoln[i];
		mainLog.println("Max diff over all states: " + maxDiff);

		// Find all states with suitably high diff *and* a game choice
		switch (refineStratWhere) {
		case ALL_MAX:
		case FIRST_MAX:
		case LAST_MAX:
			bound = Math.max(0, maxDiff - refineTermCritParam);
			// TODO: resolve issue of what to do here if relative error
			break;
		case ALL:
		case FIRST:
		case LAST:
		default:
			bound = refineTermCritParam;
			// TODO: resolve issue of what to do here if relative error
			break;
		}
		for (i = 0; i < numStates; i++) {
			// Note: Need to do comparison with >= below for case where maxdiff=infinity
			if (ubSoln[i] - lbSoln[i] >= bound && abstraction.getNumChoices(i) > 1)
				refinableStates.add(i);
		}
		mainLog.println(refinableStates.size() + " refineable states.");
		if (verbosity >= 1) {
			mainLog.println("Refinable states: " + refinableStates);
		}

		// If no states to refine, return false 
		if (refinableStates.size() == 0) {
			return false;
		}

		// Pick state(s) to refine according to strategy
		switch (refineStratWhere) {
		case ALL:
		case ALL_MAX:
			refineStates.addAll(refinableStates);
			break;
		case FIRST:
		case FIRST_MAX:
			refineStates.add(refinableStates.get(0));
			break;
		case LAST:
		case LAST_MAX:
			refineStates.add(refinableStates.get(refinableStates.size() - 1));
			break;
		default:
			throw new PrismException("Unknown (where) refinement strategy \"" + refineStratWhere.name() + "\"");
		}

		return true;
	}

	/**
	 * Refine a set of states.
	 * @param refineState States to refine.
	 */
	protected void refine(List<Integer> refineStates) throws PrismException
	{
		Set<Integer> rebuiltStates, rebuildStates;
		int i, n, refineState, numNewStates, numSuccRefines;
		long timer;

		mainLog.println("\nRefinement " + refinementNum + "...");
		timer = System.currentTimeMillis();

		// Store lists of game states that have been or will need to be rebuilt
		rebuiltStates = new LinkedHashSet<Integer>();
		rebuildStates = new LinkedHashSet<Integer>();

		numSuccRefines = 0;
		n = refineStates.size();
		// Go through list in reverse order
		for (i = n - 1; i >= 0; i--) {
			if (verbosity >= 1)
				mainLog.println("Refinement " + refinementNum + "." + (n - i) + "...");
			refineState = refineStates.get(i);
			numNewStates = refineState(refineState, rebuiltStates, rebuildStates);
			if (numNewStates > 1)
				numSuccRefines++;
		}

		timer = System.currentTimeMillis() - timer;
		timeRefine += timer / 1000.0;
		mainLog.print(numSuccRefines + " states successfully refined");
		mainLog.println(" in " + (timer / 1000.0) + " secs.");

		// Rebuild any states as necessary
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("Rebuilding states: " + rebuildStates);
		rebuildAbstraction(rebuildStates);
		timer = System.currentTimeMillis() - timer;
		timeRebuild += timer / 1000.0;
		mainLog.print(rebuiltStates.size() + "+" + rebuildStates.size() + "=");
		mainLog.print((rebuiltStates.size() + rebuildStates.size()));
		mainLog.println(" states of " + abstractionType + " rebuilt in " + (timer / 1000.0) + " secs.");
		mainLog.println("New " + abstractionType + " has " + abstraction.getNumStates() + " states.");
	}

	/**
	 * Refine a single state, by splitting using the current refinement strategy.
	 * Adds new states to abstraction (and updates initial states), updates target states,
	 * and resizes/updates lb/ub solution vectors and 'known' set.
	 * Also keeps track of which states have been or will need to be rebuilt as a result.
	 * @param refineState State to refine.
	 * @param rebuiltStates States that have been rebuilt as a result will be added here.
	 * @param rebuildStates States that need rebuilding as a result will be added here.
	 * @return Number of states into which split (i.e. 1 denotes refinement failed).
	 */
	protected int refineState(int refineState, Set<Integer> rebuiltStates, Set<Integer> rebuildStates)
			throws PrismException
	{
		List<List<Integer>> choiceLists;
		List<Integer> lbStrat = null, ubStrat = null;
		int i, n, numStates, numNewStates;

		// Sanity checks - pointless refinements...
		if (sanityChecks) {
			// Check that state to refine is not a target state (shouldn't happen
			// because diff in bounds should always be 0 in such states)
			if (target.get(refineState)) {
				throw new PrismException("Why would I want to refine a target state?");
			}
			// Likewise for states where lower/upper bounds have already converged
			if (known.get(refineState)) {
				throw new PrismException("Why would I want to refine a state that has already converged?");
			}
		}

		// Don't refine a state that we have already modified through refinement
		if (rebuiltStates.contains(refineState)) {
			if (verbosity >= 1)
				mainLog.printWarning("Skipping refinement of #" + refineState
						+ " which has already been modified by refinement.");
			return 1;
		}

		// Decide how this state will be split up (in terms of player 1 choices)
		choiceLists = new ArrayList<List<Integer>>();
		switch (refineStratHow) {
		case VAL:
			lbStrat = ubStrat = null;
			switch (abstractionType) {
			case MDP:
				switch (propertyType) {
				case PROB_REACH:
					lbStrat = ((MDPModelChecker) mc).probReachStrategy((MDP) abstraction, refineState, target, true,
							lbLastSoln);
					ubStrat = ((MDPModelChecker) mc).probReachStrategy((MDP) abstraction, refineState, target, false,
							ubLastSoln);
					break;
				case PROB_REACH_BOUNDED:
					lbStrat = ((MDPModelChecker) mc).probReachStrategy((MDP) abstraction, refineState, target, true,
							lbLastSoln);
					ubStrat = ((MDPModelChecker) mc).probReachStrategy((MDP) abstraction, refineState, target, false,
							ubLastSoln);
					break;
				case EXP_REACH:
					lbStrat = ((MDPModelChecker) mc).expReachStrategy((MDP) abstraction, null, refineState, target, true,
							lbLastSoln);
					ubStrat = ((MDPModelChecker) mc).expReachStrategy((MDP) abstraction, null, refineState, target, false,
							ubLastSoln);
					break;
				}
				break;
			case CTMDP:
				lbStrat = ((CTMDPModelChecker) mc).probReachStrategy((CTMDP) abstraction, refineState, target, true,
						lbLastSoln);
				ubStrat = ((CTMDPModelChecker) mc).probReachStrategy((CTMDP) abstraction, refineState, target, false,
						ubLastSoln);
				break;
			case STPG:
				lbStrat = ((STPGModelChecker) mc).probReachStrategy((STPG) abstraction, refineState, target, true, min,
						lbLastSoln);
				ubStrat = ((STPGModelChecker) mc).probReachStrategy((STPG) abstraction, refineState, target, false,
						min, ubLastSoln);
				break;
			}
			if (lbStrat == null || ubStrat == null) {
				String s = "Cannot generate strategy information for";
				s += " model type " + abstractionType + " and property type " + propertyType;
				throw new PrismException(s);
			}

			if (sanityChecks && (lbStrat.isEmpty() || ubStrat.isEmpty()))
				throw new PrismException("Empty strategy generated for state " + refineState);
			// Check if lb/ub are identical (just use equals() since lists are sorted)
			if (lbStrat.equals(ubStrat) && lbStrat.size() == abstraction.getNumChoices(refineState)) {
				if (verbosity >= 1)
					mainLog.printWarning("Skipping refinement of #" + refineState
							+ " for which lb/ub strategy sets are equal and covering.");
				return 1;
			}
			if (verbosity >= 1)
				mainLog.println("lbStrat: " + lbStrat + ", ubStrat: " + ubStrat);
			// Make disjoint
			int method = 1;
			switch (method) {
			// Remove intersection of lb/ub from larger
			case 1:
				if (lbStrat.containsAll(ubStrat)) {
					lbStrat.removeAll(ubStrat);
				} else {
					ubStrat.removeAll(lbStrat);
				}
				if (!lbStrat.isEmpty())
					choiceLists.add(lbStrat);
				if (!ubStrat.isEmpty())
					choiceLists.add(ubStrat);
				break;
			case 2:
				// Remove intersection of lb/ub from both
				lbStrat.removeAll(ubStrat);
				ubStrat.removeAll(lbStrat);
				choiceLists.add(lbStrat);
				choiceLists.add(ubStrat);
				break;
			case 3:
				// Pick a single (unique) choice from lb/ub
				if (lbStrat.containsAll(ubStrat)) {
					lbStrat.removeAll(ubStrat);
				} else {
					ubStrat.removeAll(lbStrat);
				}
				List<Integer> newChoiceList;
				newChoiceList = new ArrayList<Integer>();
				newChoiceList.add(lbStrat.get(0));
				choiceLists.add(newChoiceList);
				newChoiceList = new ArrayList<Integer>();
				newChoiceList.add(ubStrat.get(0));
				choiceLists.add(newChoiceList);
				break;
			}
			if (verbosity >= 1)
				mainLog.println("split: " + choiceLists);
			break;
		case ALL:
			n = abstraction.getNumChoices(refineState);
			for (i = 0; i < n; i++) {
				List<Integer> newChoiceList;
				newChoiceList = new ArrayList<Integer>();
				newChoiceList.add(i);
				choiceLists.add(newChoiceList);
			}
			break;
		default:
			throw new PrismException("Unknown (how) refinement strategy \"" + refineStratWhere.name() + "\"");
		}

		// Get (old) number of states
		numStates = abstraction.getNumStates();

		// Split the state, based on nondet choices selected above
		numNewStates = splitState(refineState, choiceLists, rebuiltStates, rebuildStates);

		// Update existing solution vectors (if any)
		lbSoln = Utils.extendDoubleArray(lbSoln, numStates, numStates + numNewStates - 1, lbSoln[refineState]);
		lbLastSoln = Utils.extendDoubleArray(lbLastSoln, numStates, numStates + numNewStates - 1,
				lbLastSoln[refineState]);
		ubSoln = Utils.extendDoubleArray(ubSoln, numStates, numStates + numNewStates - 1, ubSoln[refineState]);
		ubLastSoln = Utils.extendDoubleArray(ubLastSoln, numStates, numStates + numNewStates - 1,
				ubLastSoln[refineState]);

		// Note: we don't have to update 'known' since implicit new elements of a BitSet are assumed false

		return numNewStates;
	}

	/**
	 * Utility function to add, into the sets of choices passed to splitState(...),
	 * an additional set comprising the remainder of the nondeterministic choices.  
	 */
	public void addRemainderIntoChoiceLists(int splitState, List<List<Integer>> choiceLists)
	{
		int nChoices;
		BitSet included;
		ArrayList<Integer> otherChoices;
		int i;

		nChoices = abstraction.getNumChoices(splitState);
		included = new BitSet(nChoices);
		for (List<Integer> choiceList : choiceLists) {
			for (int j : choiceList) {
				included.set(j);
			}
		}
		otherChoices = new ArrayList<Integer>();
		i = included.nextClearBit(0);
		while (i < nChoices) {
			otherChoices.add(i);
			i = included.nextClearBit(i + 1);
		}
		if (otherChoices.size() > 0)
			choiceLists.add(otherChoices);
	}

	/**
	 * Display final summary about the abstraction-refinement loop.
	 */
	protected void printFinalSummary(String initAbstractionInfo, boolean canRefine)
	{
		// Print abstraction summary
		mainLog.println("\nInitial " + abstractionType + ": " + initAbstractionInfo);
		mainLog.println("Final " + abstractionType + ": " + abstraction.infoString());

		// Print termination info
		mainLog.print("\nTerminated " + (canRefine ? "(early) " : ""));
		mainLog.print("after " + refinementNum + " refinements");
		mainLog.print(" in " + PrismUtils.formatDouble2dp(timeTotal) + " secs.");
		mainLog.println();

		// Print breakdown of timings
		// (Note: format times to 2 d.p. for alignment)
		mainLog.println("\nAbstraction-refinement time breakdown:");
		mainLog.print("* " + PrismUtils.formatDouble2dp(timeBuild) + " secs");
		mainLog.print(" (" + PrismUtils.formatPercent1dp(timeBuild / timeTotal) + ")");
		mainLog.print(" = Building initial " + abstractionType);
		mainLog.println();
		mainLog.print("* " + PrismUtils.formatDouble2dp(timeRebuild) + " secs");
		mainLog.print(" (" + PrismUtils.formatPercent1dp(timeRebuild / timeTotal) + ")");
		mainLog.print(" = Rebuilding " + abstractionType + " (");
		mainLog.print(refinementNum + " x avg " + PrismUtils.formatDouble2dp(refinementNum > 0 ? (timeRebuild / (refinementNum)) : 0) + " secs)");
		mainLog.println();
		mainLog.print("* " + PrismUtils.formatDouble2dp(timeCheck) + " secs");
		mainLog.print(" (" + PrismUtils.formatPercent1dp(timeCheck / timeTotal) + ")");
		mainLog.print(" = model checking " + abstractionType + " (" + (refinementNum + 1) + " x avg ");
		mainLog.print(PrismUtils.formatDouble2dp(timeCheck / (refinementNum + 1)) + " secs)");
		mainLog.print(" (lb=" + PrismUtils.formatPercent1dp(timeCheckLb / (timeCheckLb + timeCheckUb)) + ")");
		mainLog.print(" (prob0=" + PrismUtils.formatPercent1dp(timeCheckProb0 / timeCheck) + ")");
		mainLog.print(" (pre=" + PrismUtils.formatPercent1dp(timeCheckPre / timeCheck) + ")");
		mainLog.print(" (iters=" + itersTotal + ")");
		mainLog.println();
		mainLog.print("* " + PrismUtils.formatDouble2dp(timeRefine) + " secs");
		mainLog.print(" (" + PrismUtils.formatPercent1dp(timeRefine / timeTotal) + ")");
		mainLog.print(" = refinement (");
		mainLog.print(refinementNum + " x avg " + PrismUtils.formatDouble2dp(refinementNum > 0 ? (timeRefine / refinementNum) : 0) + " secs)");
		mainLog.println();

		// Print result info for initial states
		int numInitialStates = abstraction.getNumInitialStates();
		mainLog.print("\nFinal diff across ");
		mainLog.print(numInitialStates + " initial state" + (numInitialStates == 1 ? "" : "s") + ": ");
		mainLog.println(ubInit - lbInit);
		mainLog.print("Final lower/upper bounds for ");
		mainLog.print(numInitialStates + " initial state" + (numInitialStates == 1 ? "" : "s") + ": ");
		mainLog.println(lbInit + " - " + ubInit);
	}

	// Private utility methods

	/**
	 * Export abstract model to a dot file with additional annotated info 
	 */
	private static void exportToDotFile(String filename, Model abstraction, BitSet known, double lbSoln[],
			double ubSoln[]) throws PrismException
	{
		STPGAbstrSimple stpg;
		int i, j, k;
		String nij, nijk;
		
		if (abstraction instanceof STPG) {
			stpg = (STPGAbstrSimple) abstraction;
		} else if (abstraction instanceof MDPSimple) {
			stpg = new STPGAbstrSimple((MDPSimple) abstraction);
		} else {
			throw new PrismNotSupportedException("Cannot export this model type to a dot file");
		}

		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + "STPG" + " {\nnode [shape=box];\n");
			for (i = 0; i < stpg.getNumStates(); i++) {
				if (known.get(i))
					out.write(i + " [label=\"" + i + " {" + lbSoln[i] + "}" + "\" style=filled  fillcolor=\"#cccccc\"");
				else
					out.write(i + " [label=\"" + i + " [" + (ubSoln[i] - lbSoln[i]) + "]" + "\"");
				out.write("]\n");
				j = -1;
				for (DistributionSet distrs : stpg.getChoices(i)) {
					j++;
					nij = "n" + i + "_" + j;
					out.write(i + " -> " + nij + " [ arrowhead=none,label=\"" + j + "\" ];\n");
					out.write(nij + " [ shape=circle,width=0.1,height=0.1,label=\"\" ];\n");
					k = -1;
					for (Distribution distr : distrs) {
						k++;
						nijk = "n" + i + "_" + j + "_" + k;
						out.write(nij + " -> " + nijk + " [ arrowhead=none,label=\"" + k + "\" ];\n");
						out.write(nijk + " [ shape=point,label=\"\" ];\n");
						for (Map.Entry<Integer, Double> e : distr) {
							out.write(nijk + " -> " + e.getKey() + " [ label=\"" + e.getValue() + "\" ];\n");
						}
					}
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write abstraction to file \"" + filename + "\"" + e);
		}
	}
}
