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
import explicit.ModelChecker.TermCrit;
import explicit.ModelChecker.ValIterDir;

public abstract class STPGAbstractRefine
{
	// Log for output (default: just send to stdout)
	protected PrismLog mainLog = new PrismPrintStreamLog(System.out);
	// Model checker
	protected ModelChecker mc;
	// Dummy model checker to store options
	protected ModelChecker mcOptions;

	// Flags/settings

	// Model type
	protected ModelType modelType = ModelType.MDP;
	// Property type
	protected PropertyType propertyType = PropertyType.PROB_REACH;
	// Bound for when property is bounded reachabilty
	protected int reachBound = 0;
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
	protected RefineStratWhere refineStratWhere = RefineStratWhere.ALL_MAX;
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

	// Objects needed for abstraction refinement
	// (abstract model, target states)
	protected Model abstraction;
	protected BitSet target;
	// Other parameters
	protected boolean min;
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
	public STPGAbstractRefine()
	{
		// By default, log output goes to System.out.
		setLog(new PrismPrintStreamLog(System.out));
		// Create dummy model checker to store options
		mcOptions = new ModelChecker();
	}

	/**
	 * Provides access to the underlying model checker for the purpose of setting options. 
	 */
	public ModelChecker getModelChecker()
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
		mcOptions.printSettings();
	}

	// Set methods for flags/settings, etc.

	public void setLog(PrismLog log)
	{
		this.mainLog = log;
		// Store this log in model checker options too
		if (mcOptions != null)
			mcOptions.setLog(log);
	}

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
			setVerbosity((optVal == null) ? 10 : Integer.parseInt(optVal));
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
		} else if (opt.equals("nopre")) {
			getModelChecker().setPrecomp(false);
		} else if (opt.equals("pre")) {
			getModelChecker().setPrecomp(true);
		} else if (opt.equals("noprob0")) {
			getModelChecker().setProb0(false);
		} else if (opt.equals("noprob1")) {
			getModelChecker().setProb1(false);
		} else if (opt.equals("maxrefs")) {
			if (optVal != null) {
				setMaxRefinements(Integer.parseInt(optVal));
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

	// Abstract methods that must be implemented for abstraction-refinement loop

	/**
	 * Initialise: Build initial abstraction and set of target abstract states.
	 * (storing in abstraction and target, respectively).
	 * Note: abstraction must respect initial states,
	 * i.e. any abstract state contains either all/no initial states. 
	 */
	protected abstract void initialise() throws PrismException;

	/**
	 * Split an abstract state for refinement, based on sets of nondeterministic choices.
	 * This function should update any information stored locally about abstract state space etc.
	 * and then rebuild the abstraction and set of target states appropriately.
	 * One of the new states should replace the state being split;
	 * the rest should be appended to the list of abstract states.
	 * 
	 * The total number of new states should be returned.
	 * Notes:
	 *  # The union of all these sets may not cover all choices in the state to be split.
	 *      This is because there may be more efficient ways to compute the remainder of the abstract state.
	 *      If not, use the utility function addRemainderIntoChoiceLists(...).
	 * 
	 * TODO: what about:
	 * (ii) work out which states of the abstraction will need rebuilding as a result
	 * 
	 * @param splitState: State to split.
	 * @param choiceLists: Lists of nondeterministic choices defining split.
	 * @param rebuildStates: States that need rebuilding as a result should be added here.
	 * @return: Number of states into which split (i.e. 1 denotes split failed).
	 */
	protected abstract int splitState(int splitState, List<List<Integer>> choiceLists, Set<Integer> rebuildStates)
			throws PrismException;

	/**
	 * Rebuild the abstraction after a refinement.
	 * @param rebuildStates: States that need rebuilding.
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

		// Store what abstract model type is
		// and create appropriate model checker
		// (which inherits log/options from mcOptions)
		switch (modelType) {
		case DTMC:
			abstractionType = ModelType.MDP;
			mc = new MDPModelChecker();
			break;
		case CTMC:
			abstractionType = ModelType.CTMDP;
			mc = new CTMDPModelChecker();
			break;
		case MDP:
			abstractionType = ModelType.STPG;
			mc = new STPGModelChecker();
			break;
		default:
			throw new PrismException("Cannot handle model type " + modelType);
		}
		mc.inheritSettings(mcOptions);

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
		mainLog.println(abstractionType + " constructed in " + (timer / 1000.0) + " secs.");
		timeBuild += timer / 1000.0;
		mainLog.println(abstractionType + ": " + abstraction.infoString());
		if (verbosity >= 5) {
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
			if (i < 0)
				break;

			//log.println("YY "+i+" "+((STPG)abstraction).steps.get(i));
			if (((STPG) abstraction).allSuccessorsInSet(i, known)) {
				// Compute... note lbsoln for both is ok sinec = ubsoln
				lb = ((STPG) abstraction).mvMultMinMaxSingle(i, lbSoln, true, min);
				ub = ((STPG) abstraction).mvMultMinMaxSingle(i, lbSoln, false, min);
				mainLog.println(((STPG) abstraction).steps.get(i));
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
					numNewStates = refineState(i, rebuildStates);
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
			throw new PrismException("Property type " + propertyType + " not supported");
		}

		// See if each state has "converged", i.e. bounds are close enough to assume exact
		n = abstraction.getNumStates();
		for (i = 0; i < n; i++) {
			known.set(i, PrismUtils.doublesAreClose(ubSoln[i], lbSoln[i], refineTermCritParam,
					refineTermCrit == RefineTermCrit.ABSOLUTE));
		}
		mainLog.println(known.cardinality() + "/" + n + " states converged.");

		// Compute bounds for initial states
		lbInit = ubInit = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		for (int j : abstraction.getInitialStates()) {
			if (verbosity >= 5)
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

		// Pass settings to model checker
		mc.setTermCrit(TermCrit.RELATIVE);
		mc.setTermCritParam(1e-8);

		// Compute lower bounds
		switch (abstractionType) {
		case MDP:
			if (optimise && refinementNum > 0) {
				mc.setValIterDir(MDPModelChecker.ValIterDir.BELOW);
				// TODO
			} else {
				res = ((MDPModelChecker) mc).probReach((MDP) abstraction, target, true);
			}
			break;
		case CTMDP:
			if (optimise && refinementNum > 0) {
				mc.setValIterDir(MDPModelChecker.ValIterDir.BELOW);
				// TODO
			} else {
				res = ((CTMDPModelChecker) mc).probReach((CTMDP) abstraction, target, true);
			}
			break;
		case STPG:
			if (optimise && refinementNum > 0) {
				mc.setValIterDir(MDPModelChecker.ValIterDir.BELOW);
				res = ((STPGModelChecker) mc).probReach((STPG) abstraction, target, true, min, lbSoln, known);
			} else {
				res = ((STPGModelChecker) mc).probReach((STPG) abstraction, target, true, min, null, null);
			}
			break;
		default:
			throw new PrismException("Cannot model check " + abstractionType);
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
				// TODO
			} else {
				res = ((MDPModelChecker) mc).probReach((MDP) abstraction, target, false);
			}
			break;
		case CTMDP:
			if (optimise) {
				// TODO
			} else {
				res = ((CTMDPModelChecker) mc).probReach((CTMDP) abstraction, target, false);
			}
			break;
		case STPG:
			if (optimise) {
				if (above) {
					mc.setValIterDir(ValIterDir.ABOVE);
					res = ((STPGModelChecker) mc).probReach((STPG) abstraction, target, false, min, ubSoln, known);
				} else {
					mc.setValIterDir(ValIterDir.BELOW);
					double lbCopy[] = Utils.cloneDoubleArray(lbSoln);
					res = ((STPGModelChecker) mc).probReach((STPG) abstraction, target, false, min, lbCopy, known);
				}
			} else {
				res = ((STPGModelChecker) mc).probReach((STPG) abstraction, target, false, min, null, null);
			}
			break;
		default:
			throw new PrismException("Cannot model check " + abstractionType);
		}
		ubSoln = res.soln;
		ubLastSoln = ubSoln; // TODO: fix (if nec.)
		timeCheckUb += res.timeTaken;
		timeCheckProb0 += res.timeProb0;
		timeCheckPre += res.timePre;
		itersTotal += res.numIters;
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
			res = ((MDPModelChecker) mc).probReachBounded((MDP) abstraction, target, reachBound, true, null, results);
			break;
		case CTMDP:
			res = ((CTMDPModelChecker) mc).probReachBounded((CTMDP) abstraction, target, (double) reachBound, true,
					null, results);
			break;
		case STPG:
			res = ((STPGModelChecker) mc).probReachBounded((STPG) abstraction, target, reachBound, true, min, null,
					results);
			break;
		default:
			throw new PrismException("Cannot model check " + abstractionType);
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
			res = ((MDPModelChecker) mc).probReachBounded((MDP) abstraction, target, reachBound, false, null, results);
			break;
		case CTMDP:
			res = ((CTMDPModelChecker) mc).probReachBounded((CTMDP) abstraction, target, (double) reachBound, false,
					null, results);
			break;
		case STPG:
			res = ((STPGModelChecker) mc).probReachBounded((STPG) abstraction, target, reachBound, false, min, null,
					results);
			break;
		default:
			throw new PrismException("Cannot model check " + abstractionType);
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
				res = ((MDPModelChecker) mc).expReach((MDP) abstraction, target, true, lbSoln, known);
			} else {
				res = ((MDPModelChecker) mc).expReach((MDP) abstraction, target, true, null, null);
			}
			break;
		default:
			throw new PrismException("Cannot model check " + abstractionType);
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
				res = ((MDPModelChecker) mc).expReach((MDP) abstraction, target, false, lbCopy, known);
			} else {
				res = ((MDPModelChecker) mc).expReach((MDP) abstraction, target, false, null, null);
			}
			break;
		default:
			throw new PrismException("Cannot model check " + abstractionType);
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
	 * @param refineState: States to refine.
	 */
	protected void refine(List<Integer> refineStates) throws PrismException
	{
		Set<Integer> rebuildStates;
		int i, n, refineState, numNewStates, numSuccRefines;
		long timer;

		mainLog.println("\nRefinement " + refinementNum + "...");
		timer = System.currentTimeMillis();

		// Store list of game states that will need rebuilding
		rebuildStates = new LinkedHashSet<Integer>();

		numSuccRefines = 0;
		n = refineStates.size();
		// Go through list in reverse order
		for (i = n - 1; i >= 0; i--) {
			if (verbosity >= 1)
				mainLog.println("Refinement " + refinementNum + "." + (n - i) + "...");
			refineState = refineStates.get(i);
			numNewStates = refineState(refineState, rebuildStates);
			if (numNewStates > 1)
				numSuccRefines++;
		}

		timer = System.currentTimeMillis() - timer;
		timeRefine += timer / 1000.0;
		mainLog.print(numSuccRefines + " states successfully refined");
		mainLog.println(" in " + (timer / 1000.0) + " secs.");

		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("Rebuilding states: " + rebuildStates);
		//rebuildAbstraction(rebuildStates);
		timer = System.currentTimeMillis() - timer;
		timeRebuild += timer / 1000.0;
		mainLog.println(rebuildStates.size() + " states of " + abstractionType + " rebuilt in " + (timer / 1000.0)
				+ " secs.");
		mainLog.println("New " + abstractionType + " has " + abstraction.getNumStates() + " states.");
	}

	/**
	 * Refine a single state, by splitting using the current refinement strategy.
	 * Adds new states to abstraction (and updates initial states), updates target states,
	 * and resizes/updates lb/ub solution vectors and 'known' set.
	 * Also keeps track of which states of the abstraction will need rebuilding as a result.
	 * @param refineState: State to refine.
	 * @param rebuildStates: States that need rebuilding as a result should be added here.
	 * @return: Number of states into which split (i.e. 1 denotes refinement failed).
	 */
	protected int refineState(int refineState, Set<Integer> rebuildStates) throws PrismException
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
		if (rebuildStates.contains(refineState)) {
			if (verbosity >= 1)
				mainLog.println("Warning: Skipping refinement of #" + refineState
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
				case EXP_REACH:
					lbStrat = ((MDPModelChecker) mc).expReachStrategy((MDP) abstraction, refineState, target, true,
							lbLastSoln);
					ubStrat = ((MDPModelChecker) mc).expReachStrategy((MDP) abstraction, refineState, target, false,
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
			if (lbStrat.equals(ubStrat)) {
				if (verbosity >= 1)
					mainLog.println("Warning: Skipping refinement of #" + refineState
							+ " for which lb/ub strategy sets are equal.");
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
				choiceLists.add(lbStrat);
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
		numNewStates = splitState(refineState, choiceLists, rebuildStates);

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
		mainLog.print(refinementNum + " x avg " + PrismUtils.formatDouble2dp(timeRebuild / (refinementNum)) + " secs)");
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
		mainLog.print(refinementNum + " x avg " + PrismUtils.formatDouble2dp(timeRefine / refinementNum) + " secs)");
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
		STPG stpg;
		int i, j, k;
		if (abstraction instanceof STPG) {
			stpg = (STPG) abstraction;
		} else if (abstraction instanceof MDP) {
			stpg = new STPG((MDP) abstraction);
		} else {
			throw new PrismException("Cannot export this model type to a dot file");
		}

		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + "STPG" + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < stpg.numStates; i++) {
				if (known.get(i))
					out.write(i + " [label=\"" + i + " {" + lbSoln[i] + "}" + "\" style=filled  fillcolor=\"#cccccc\"");
				else
					out.write(i + " [label=\"" + i + " [" + (ubSoln[i] - lbSoln[i]) + "]" + "\"");
				//out.write(i + " [label=\"" + i + " [" + (ubSoln[i]) + "-" + (lbSoln[i]) + "=" + (ubSoln[i] - lbSoln[i]) + "]" + "\"");
				out.write("]\n");
				j = 0;
				for (DistributionSet distrs : stpg.steps.get(i)) {
					k = 0;
					for (Distribution distr : distrs) {
						for (Map.Entry<Integer, Double> e : distr) {
							out.write(i + " -> " + e.getKey() + " [ label=\"");
							out.write(j + "," + k + ":" + e.getValue() + "\" ];\n");
						}
						k++;
					}
					j++;
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write abstraction to file \"" + filename + "\"" + e);
		}
	}
}
