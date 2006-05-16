//==============================================================================
//	
//	Copyright (c) 2002-2006, Dave Parker
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

import java.io.*;
import java.util.Vector;

import jdd.*;
import odd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.*;

// pctl model checker for stochastic systems (ctmcs)

public class StochModelChecker implements ModelChecker
{
	// logs
	private PrismLog mainLog;
	private PrismLog techLog;

	// options
	
	// which engine to use
	private int engine;
	// method for solving linear equation systems
	private int linEqMethod;
	// parameter for linear equation solver methods
	private double linEqMethodParam;
	// termination criterion (iterative methods)
	private int termCrit;
	// parameter for termination criterion
	private double termCritParam;
	// max num iterations (iterative methods)
	private int maxIters;
	// flags
	private boolean verbose;	// verbose output?
	private boolean precomp;	// use 0,1 precomputation algorithms?
	private boolean bsccComp; 	// do BSCC computation?
	// sparse bits info
	private int SBMaxMem;
	private int numSBLevels;
	// hybrid sor info
	private int SORMaxMem;
	private int numSORLevels;	

	// properties file
	private PropertiesFile propertiesFile;
	
	// constant values
	private Values constantValues;

	// Expression2MTBDD object for translating expressions
	private Expression2MTBDD expr2mtbdd;

	// scc computer
	private SCCComputer sccComputer;
	
	// class-wide storage for probability in the initial state
	private double numericalRes;

	// model info
	private StochModel model;
	private JDDNode trans;
	private JDDNode trans01;
	private JDDNode start;
	private JDDNode reach;
	private JDDNode stateRewards;
	private JDDNode transRewards;
	private ODDNode odd;
	private JDDVars allDDRowVars;
	private JDDVars allDDColVars;
	private JDDVars[] varDDRowVars;
	private long numStates;

	// constructor - set some defaults
	
	public StochModelChecker(PrismLog log1, PrismLog log2, Model m, PropertiesFile pf) throws PrismException
	{
		// initialise
		mainLog = log1;
		techLog = log2;
		if (!(m instanceof StochModel)) {
			throw new PrismException("Wrong model type passed to StochModelChecker.");
		}
		model = (StochModel)m;
		propertiesFile = pf;
		trans = model.getTrans();
		trans01 = model.getTrans01();
		start = model.getStart();
		reach = model.getReach();
		stateRewards = model.getStateRewards();
		transRewards = model.getTransRewards();
		odd = model.getODD();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		varDDRowVars = model.getVarDDRowVars();
		numStates = model.getNumStates();
		
		// create list of all constant values needed
		constantValues = new Values();
		constantValues.addValues(model.getConstantValues());
		if (pf != null) constantValues.addValues(pf.getConstantValues());

		// create Expression2MTBDD object
		expr2mtbdd = new Expression2MTBDD(mainLog, techLog, model.getVarList(), varDDRowVars, constantValues);
		expr2mtbdd.setFilter(reach);
		
		// create SCCComputer object
		sccComputer = new SCCComputer(log1, log2, model);

		// set up some default options
		// (although all should be overridden before model checking)
		engine = Prism.HYBRID;
		linEqMethod = Prism.JACOBI;
		linEqMethodParam = 0.9;
		termCrit = Prism.RELATIVE;
		termCritParam = 1e-6;
		maxIters = 10000;
		verbose = false;
		precomp = true;
		bsccComp = true;
		SBMaxMem = 1024;
		numSBLevels = -1;
		SORMaxMem = 1024;
		numSORLevels = -1;
	}

	// set engine

	public void setEngine(int e)
	{
		engine = e;
	}

	// set options (generic)
	
	public void setOption(String option, boolean b)
	{
		if (option.equals("verbose")) {
			verbose = b;
		}
		else if (option.equals("precomp")) {
			precomp = b;
		}
		else if (option.equals("bscccomp")) {
			bsccComp = b;
		}
		else if (option.equals("compact")) {
			PrismHybrid.setCompact(b);
			PrismSparse.setCompact(b);
		}
		else {
			mainLog.println("Warning: option \""+option+"\" not supported by StochModelChecker.");
		}
	}
	
	public void setOption(String option, int i)
	{
		if (option.equals("lineqmethod")) {
			linEqMethod = i;
			PrismMTBDD.setLinEqMethod(i);
			PrismSparse.setLinEqMethod(i);
			PrismHybrid.setLinEqMethod(i);
		}
		else if (option.equals("termcrit")) {
			termCrit = i;
			PrismMTBDD.setTermCrit(i);
			PrismSparse.setTermCrit(i);
			PrismHybrid.setTermCrit(i);
		}
		else if (option.equals("maxiters")) {
			maxIters = i;
			PrismMTBDD.setMaxIters(i);
			PrismSparse.setMaxIters(i);
			PrismHybrid.setMaxIters(i);
		}
		else if (option.equals("sbmaxmem")) {
			SBMaxMem = i;
			PrismHybrid.setSBMaxMem(i);
		}
		else if (option.equals("numsblevels")) {
			numSBLevels = i;
			PrismHybrid.setNumSBLevels(i);
		}
		else if (option.equals("sormaxmem")) {
			SORMaxMem = i;
			PrismHybrid.setSORMaxMem(i);
		}
		else if (option.equals("numsorlevels")) {
			numSORLevels = i;
			PrismHybrid.setNumSORLevels(i);
		}
		else {
			mainLog.println("Warning: option \""+option+"\" not supported by StochModelChecker.");
		}
	}

	public void setOption(String option, double d)
	{
		if (option.equals("lineqmethodparam")) {
			linEqMethodParam = d;
			PrismMTBDD.setLinEqMethodParam(d);
			PrismSparse.setLinEqMethodParam(d);
			PrismHybrid.setLinEqMethodParam(d);
		}
		else if (option.equals("termcritparam")) {
			termCritParam = d;
			PrismMTBDD.setTermCritParam(d);
			PrismSparse.setTermCritParam(d);
			PrismHybrid.setTermCritParam(d);
		}
		else {
			mainLog.println("Warning: option \""+option+"\" not supported by StochModelChecker.");
		}
	}

	public void setOption(String option, String s)
	{
		mainLog.println("Warning: option \""+option+"\" not supported by StochModelChecker.");
	}

	// compute number of places to round solution vector to
	// (given termination criteria, etc.)

	public int getPlacesToRoundBy()
	{
		return 1 - (int)(Math.log(termCritParam)/Math.log(10));
	}

	//-----------------------------------------------------------------------------------
	// check a pctl formula
	//-----------------------------------------------------------------------------------

	public Object check(PCTLFormula pctl) throws PrismException
	{
		long timer = 0;
		JDDNode dd;
		StateList states;
		boolean b;
		Object res;
		
		// start timer
		timer = System.currentTimeMillis();
		
		// do model checking and store result
		dd = checkPCTLFormula(pctl);
		states = new StateListMTBDD(dd, model);
		
		// stop timer
		timer = System.currentTimeMillis() - timer;
		
		// print out model checking time
		mainLog.println("\nTime for model checking: " + timer/1000.0 + " seconds.");
		
		// output and result of model checking depend on return type
		if (pctl.getType() == Expression.BOOLEAN) {
			
			// print out number of satisfying states
			mainLog.print("\nNumber of satisfying states: ");
			mainLog.print(states.size());
			if (states.size() == numStates) {
				mainLog.print(" (all)");
			}
			else if (states.includes(model.getStart())) {
				mainLog.print((model.getNumStartStates() == 1) ? " (including initial state)" : " (including all initial states)");
			}
			else {
				mainLog.print((model.getNumStartStates() == 1) ? " (initial state not satisfied)" : " (initial states not all satisfied)");
			}
			mainLog.print("\n");
			
			// if "verbose", print out satisfying states
			if (verbose) {
				mainLog.print("\nSatisfying states:");
				if (states.size() > 0) {
					mainLog.print("\n");
					states.print(mainLog);
				}
				else {
					mainLog.print(" (none)\n");
				}
			}
			
			// result is true if all states satisfy, false otherwise
			b = (states.size() == numStates);
			res = b ? new Boolean(true) : new Boolean (false);
			
			// print result
			mainLog.print("\nResult: " + b + " (property " + (b?"":"not ") + "satisfied in all states)\n");
		}
		else {
			// result is value stored earlier
			res = new Double(numericalRes);
			
			// print result
			mainLog.print("\nResult: " + res + "\n");
		}
		
		// finished with memory
		states.clear();
		
		// return result
		return res;
	}
	
	// check pctl formula (recursive)
	
	public JDDNode checkPCTLFormula(PCTLFormula pctl) throws PrismException
	{
		JDDNode states;
		
		// implies
		if (pctl instanceof PCTLImplies) {
			states = checkPCTLImplies((PCTLImplies)pctl);
		}
		// or
		else if (pctl instanceof PCTLOr) {
			states = checkPCTLOr((PCTLOr)pctl);
		}
		// and
		else if (pctl instanceof PCTLAnd) {
			states = checkPCTLAnd((PCTLAnd)pctl);
		}
		// not
		else if (pctl instanceof PCTLNot) {
			states = checkPCTLNot((PCTLNot)pctl);
		}
		// csl prob
		else if (pctl instanceof PCTLProb) {
			states = checkPCTLProb((PCTLProb)pctl);
		}
		// csl steady-state
		else if (pctl instanceof PCTLSS) {
			states = checkPCTLSteadyState((PCTLSS)pctl);
		}
		// pctl reward
		else if (pctl instanceof PCTLReward) {
			states = checkPCTLReward((PCTLReward)pctl);
		}
		// expression
		else if (pctl instanceof PCTLExpression) {
			states = checkPCTLExpression((PCTLExpression)pctl);
		}
		// init
		else if (pctl instanceof PCTLInit) {
			JDD.Ref(start);
			states = start;
		}
		// label
		else if (pctl instanceof PCTLLabel) {
			states = checkPCTLLabel((PCTLLabel)pctl);
		}
		// brackets
		else if (pctl instanceof PCTLBrackets) {
			states = checkPCTLFormula(((PCTLBrackets)pctl).getOperand());
		}
		else {
			throw new PrismException("Unknown CSL operator");
		}
		
		// remove nonreachable states from solution
		JDD.Ref(reach);
		states = JDD.Apply(JDD.TIMES, states, reach);		
		
		return states;
	}
	
	// model checking for each pctl operator...
	
	//-----------------------------------------------------------------------------------
	// check method for each pctl operator
	//-----------------------------------------------------------------------------------
	
	// implies
	
	private JDDNode checkPCTLImplies(PCTLImplies pctl) throws PrismException
	{
		JDDNode dd1, dd2;
		
		dd1 = checkPCTLFormula(pctl.getOperand1());
		try {
			dd2 = checkPCTLFormula(pctl.getOperand2());
		}
		catch (PrismException e) {
			JDD.Deref(dd1);
			throw e;
		}
		
		return JDD.Or(JDD.Not(dd1), dd2);
	}
	
	// or
	
	private JDDNode checkPCTLOr(PCTLOr pctl) throws PrismException
	{
		JDDNode res;
		int i;
		
		res = JDD.Constant(0);
		for (i = 0; i < pctl.getNumOperands(); i++) {
			try {
				res = JDD.Or(res, checkPCTLFormula(pctl.getOperand(i)));
			}
			catch (PrismException e) {
				JDD.Deref(res);
				throw e;
			}
		}
		
		return res;
	}
	
	// and
	
	private JDDNode checkPCTLAnd(PCTLAnd pctl) throws PrismException
	{
		JDDNode res;
		int i;
		
		res = JDD.Constant(1);
		for (i = 0; i < pctl.getNumOperands(); i++) {
			try {
				res = JDD.And(res, checkPCTLFormula(pctl.getOperand(i)));
			}
			catch (PrismException e) {
				JDD.Deref(res);
				throw e;
			}
		}
		
		return res;
	}

	// not
	
	private JDDNode checkPCTLNot(PCTLNot pctl) throws PrismException
	{
		return JDD.Not(checkPCTLFormula(pctl.getOperand()));
	}

	// prob
	
	private JDDNode checkPCTLProb(PCTLProb pctl) throws PrismException
	{
		Expression pe;	// probability bound (expression)
		double p = 0;	// probability value (actual value)
		String relOp;	// relational operator
		PCTLFormula f;	// path formula
		
		JDDNode filter, sol, tmp;
		StateProbs probs = null;
		StateList states = null;
		double minRes = 0, maxRes = 0;
		
		// get info from prob operator
		relOp = pctl.getRelOp();
		pe = pctl.getProb();
		if (pe != null) {
			p = pe.evaluateDouble(constantValues, null);
			if (p < 0 || p > 1) throw new PrismException("Invalid probability bound " + p + " in P[] formula");
		}
		
		// check for trivial (i.e. stupid) cases
		if (pe != null) {
			if ((p == 0 && relOp.equals(">=")) || (p == 1 && relOp.equals("<="))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p + " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return reach;
			}
			else if ((p == 0 && relOp.equals("<")) || (p == 1 && relOp.equals(">"))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p + " - formula trivially satisfies no states\n");
				return JDD.Constant(0);
			}
		}
		
		// print a warning if Pmin/Pmax used
		if (relOp.equals("min=") || relOp.equals("max=")) {
			mainLog.print("\nWarning: \"Pmin=?\" and \"Pmax=?\" operators are identical to \"P=?\" for DTMCs\n");
		}
		
		// translate filter (if present)
		filter = null;
		if (pctl.getFilter() != null) {
			filter = checkPCTLFormula(pctl.getFilter());
		}
		
		// check if filter satisfies no states
		if (filter != null) if (filter.equals(JDD.ZERO)) {
			// for P=? properties, this is an error
			if (pe == null) {
				throw new PrismException("Filter {"+ pctl.getFilter() + "} in P=?[] property satisfies no states");
			}
			// otherwise just print a warning
			else {
				mainLog.println("\nWarning: Filter {" + pctl.getFilter() + "} satisfies no states and is being ignored");
				JDD.Deref(filter);
				filter = null;
			}
		}
		
		// compute probabilities
		try {
			f = pctl.getOperand();
			if (f instanceof PCTLProbNext)
				probs = checkPCTLProbNext((PCTLProbNext)f);
			else if (f instanceof PCTLProbBoundedUntil)
				probs = checkPCTLProbBoundedUntil((PCTLProbBoundedUntil)f);
			else if (f instanceof PCTLProbUntil)
				probs = checkPCTLProbUntil((PCTLProbUntil)f, pe, p);
			else
				throw new PrismException("Unrecognised path operator in P[] formula");
		}
		catch (PrismException e) {
			if (filter != null) JDD.Deref(filter);
			throw e;
		}
		
		// round off probabilities
		//probs.roundOff(getPlacesToRoundBy());
		
		// print out probabilities
		if (verbose) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}
		
		// if a filter was provided, there is some additional output...
		if (filter != null) {
			
			// for non-"P=?"-type properties, print out some probs
			if (pe != null) {
				if (!pctl.noFilterRequests()) {
					mainLog.println("\nWarning: \"{min}\", \"{max}\" only apply to \"P=?\" properties; they are ignored here.");
				}
				mainLog.print("\nProbabilities (non-zero only) for states satisfying " + pctl.getFilter() + ":\n");
				probs.printFiltered(mainLog, filter);
			}
			
			// for "P=?"-type properties...
			if (pe == null) {
				// compute/print min info
				if (pctl.filterMinRequested()) {
					minRes = probs.minOverBDD(filter);
					mainLog.print("\nMinimum probability for states satisfying " + pctl.getFilter() + ": " + minRes + "\n");
					tmp = probs.getBDDFromInterval(minRes-termCritParam, minRes+termCritParam);
					JDD.Ref(filter);
					tmp = JDD.And(tmp, filter);
					states = new StateListMTBDD(tmp, model);
					mainLog.print("There are " + states.size() + " states with this minimum probability (+/- " + termCritParam + ")");
					if (!verbose && states.size() > 10) {
						mainLog.print(".\nThe first 10 states are displayed below. To view them all, use verbose mode.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					JDD.Deref(tmp);
				}
				// compute/print min info
				if (pctl.filterMaxRequested()) {
					maxRes = probs.maxOverBDD(filter);
					mainLog.print("\nMaximum probability for states satisfying " + pctl.getFilter() + ": " + maxRes + "\n");
					tmp = probs.getBDDFromInterval(maxRes-termCritParam, maxRes+termCritParam);
					JDD.Ref(filter);
					tmp = JDD.And(tmp, filter);
					states = new StateListMTBDD(tmp, model);
					mainLog.print("There are " + states.size() + " states with this maximum probability (+/- " + termCritParam + ")");
					if (!verbose && states.size() > 10) {
						mainLog.print(".\nThe first 10 states are displayed below. To view them all, use verbose mode.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					JDD.Deref(tmp);
				}
			}
		}
		
		// for P=? properties...
		// if there are multiple initial states or if there is a filter satisfying
		// multiple states with no {min}/{max}/etc., print a warning...
		if (pe == null) {
			if (filter == null) {
				if (model.getNumStartStates() > 1) {
					mainLog.print("\nWarning: There are multiple initial states; the result of model checking is for the first one: ");
					model.getStartStates().print(mainLog, 1);
				}
			} else if (pctl.noFilterRequests()) {
				StateListMTBDD filterStates = new StateListMTBDD(filter, model);
				if (filterStates.size() > 1) {
					mainLog.print("\nWarning: The filter {" + pctl.getFilter() + "} is satisfied by " + filterStates.size() + " states.\n");
					mainLog.print("The result of model checking is for the first of these: ");
					filterStates.print(mainLog, 1);
				}
			}
		}
		
		// compute result of property
		// if there's a bound, get set of satisfying states
		if (pe != null) {
			sol = probs.getBDDFromInterval(relOp, p);
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
		}
		// if there's no bound, result will be a probability
		else {
			// just store empty set for sol
			sol = JDD.Constant(0);
			// use filter if present
			if (filter != null) {
				if (pctl.filterMinRequested()) numericalRes = minRes;
				else if (pctl.filterMaxRequested()) numericalRes = maxRes;
				else numericalRes = probs.firstFromBDD(filter);
			}
			// otherwise use initial state
			else {
				numericalRes = probs.firstFromBDD(start);
			}
		}
		
		// free vector
		probs.clear();
		
		// derefs
		if (filter != null) JDD.Deref(filter);
		
		return sol;
	}

	// reward
	
	private JDDNode checkPCTLReward(PCTLReward pctl) throws PrismException
	{
		Expression re;	// reward bound (expression)
		double r = 0;	// reward value (actual value)
		String relOp;	// relational operator
		PCTLFormula f;	// path formula
		
		JDDNode filter, sol, tmp;
		StateProbs rewards = null;
		StateList states = null;
		double minRes = 0, maxRes = 0;
		
		// get info from reward operator
		relOp = pctl.getRelOp();
		re = pctl.getReward();
		if (re != null) {
			r = re.evaluateDouble(constantValues, null);
			if (r < 0) throw new PrismException("Invalid reward bound " + r + " in R[] formula");
		}
		
		// check for trivial (i.e. stupid) cases
		if (re != null) {
			if (r == 0 && relOp.equals(">=")) {
				mainLog.print("\nWarning: checking for reward " + relOp + " " + r + " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return reach;
			}
			else if (r == 0 && relOp.equals("<")) {
				mainLog.print("\nWarning: checking for reward " + relOp + " " + r + " - formula trivially satisfies no states\n");
				return JDD.Constant(0);
			}
		}
		
		// print a warning if Rmin/Rmax used
		if (relOp.equals("min=") || relOp.equals("max=")) {
			mainLog.print("\nWarning: \"Rmin=?\" and \"Rmax=?\" operators are identical to \"R=?\" for DTMCs\n");
		}
		
		// translate filter (if present)
		filter = null;
		if (pctl.getFilter() != null) {
			filter = checkPCTLFormula(pctl.getFilter());
		}
		
		// check if filter satisfies no states
		if (filter != null) if (filter.equals(JDD.ZERO)) {
			// for R=? properties, this is an error
			if (re == null) {
				throw new PrismException("Filter {"+ pctl.getFilter() + "} in R=?[] property satisfies no states");
			}
			// otherwise just print a warning
			else {
				mainLog.println("\nWarning: Filter {" + pctl.getFilter() + "} satisfies no states and is being ignored");
				JDD.Deref(filter);
				filter = null;
			}
		}
		
		// compute rewards
		f = pctl.getOperand();
		try {
			if (f instanceof PCTLRewardCumul)
				rewards = checkPCTLRewardCumul((PCTLRewardCumul)f);
			else if (f instanceof PCTLRewardInst)
				rewards = checkPCTLRewardInst((PCTLRewardInst)f);
			else if (f instanceof PCTLRewardReach)
				rewards = checkPCTLRewardReach((PCTLRewardReach)f);
			else if (f instanceof PCTLRewardSS)
				rewards = checkPCTLRewardSS((PCTLRewardSS)f, filter, pctl.getFilter());
			else
				throw new PrismException("Unrecognised operator in R[] formula");
		}
		catch (PrismException e) {
			if (filter != null) JDD.Deref(filter);
			throw e;
		}
		
		// round off rewards
		//rewards.roundOff(getPlacesToRoundBy());
		
		// print out rewards
		if (verbose) {
			mainLog.print("\nRewards (non-zero only) for all states:\n");
			rewards.print(mainLog);
		}
		
		// if a filter was provided, there is some additional output...
		if (filter != null) {
			
			// for non-"R=?"-type properties, print out some rewards
			if (re != null) {
				if (!pctl.noFilterRequests()) {
					mainLog.println("\nWarning: \"{min}\", \"{max}\" only apply to \"R=?\" properties; they are ignored here.");
				}
				mainLog.print("\nRewards (non-zero only) for states satisfying " + pctl.getFilter() + ":\n");
				rewards.printFiltered(mainLog, filter);
			}
			
			// for "R=?"-type properties...
			if (re == null) {
				// compute/print min info
				if (pctl.filterMinRequested()) {
					minRes = rewards.minOverBDD(filter);
					mainLog.print("\nMinimum reward for states satisfying " + pctl.getFilter() + ": " + minRes + "\n");
					tmp = rewards.getBDDFromInterval(minRes-termCritParam, minRes+termCritParam);
					JDD.Ref(filter);
					tmp = JDD.And(tmp, filter);
					states = new StateListMTBDD(tmp, model);
					mainLog.print("There are " + states.size() + " states with this minimum reward (+/- " + termCritParam + ")");
					if (!verbose && states.size() > 10) {
						mainLog.print(".\nThe first 10 states are displayed below. To view them all, use verbose mode.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					JDD.Deref(tmp);
				}
				// compute/print min info
				if (pctl.filterMaxRequested()) {
					maxRes = rewards.maxOverBDD(filter);
					mainLog.print("\nMaximum reward for states satisfying " + pctl.getFilter() + ": " + maxRes + "\n");
					tmp = rewards.getBDDFromInterval(maxRes-termCritParam, maxRes+termCritParam);
					JDD.Ref(filter);
					tmp = JDD.And(tmp, filter);
					states = new StateListMTBDD(tmp, model);
					mainLog.print("There are " + states.size() + " states with this maximum reward (+/- " + termCritParam + ")");
					if (!verbose && states.size() > 10) {
						mainLog.print(".\nThe first 10 states are displayed below. To view them all, use verbose mode.\n");
						states.print(mainLog, 10);
					} else {
						mainLog.print(":\n");
						states.print(mainLog);
					}
					JDD.Deref(tmp);
				}
			}
		}
		
		// for R=? properties...
		// if there are multiple initial states or if there is a filter satisfying
		// multiple states with no {min}/{max}/etc., print a warning...
		if (re == null) {
			if (filter == null) {
				if (model.getNumStartStates() > 1) {
					mainLog.print("\nWarning: There are multiple initial states; the result of model checking is for the first one: ");
					model.getStartStates().print(mainLog, 1);
				}
			} else if (pctl.noFilterRequests()) {
				StateListMTBDD filterStates = new StateListMTBDD(filter, model);
				if (filterStates.size() > 1) {
					mainLog.print("\nWarning: The filter {" + pctl.getFilter() + "} is satisfied by " + filterStates.size() + " states.\n");
					mainLog.print("The result of model checking is for the first of these: ");
					filterStates.print(mainLog, 1);
				}
			}
		}
		
		// compute result of property
		// if there's a bound, get set of satisfying states
		if (re != null) {
			sol = rewards.getBDDFromInterval(relOp, r);
			// remove unreachable states from solution
			JDD.Ref(reach);
			sol = JDD.And(sol, reach);
		}
		// if there's no bound, result will be a reward
		else {
			// just store empty set for sol
			sol = JDD.Constant(0);
			// use filter if present
			if (filter != null) {
				if (pctl.filterMinRequested()) numericalRes = minRes;
				else if (pctl.filterMaxRequested()) numericalRes = maxRes;
				else numericalRes = rewards.firstFromBDD(filter);
			}
			// otherwise use initial state
			else {
				numericalRes = rewards.firstFromBDD(start);
			}
		}
		
		// free vector
		rewards.clear();
		
		// derefs
		if (filter != null) JDD.Deref(filter);
		
		return sol;
	}

	// steady state operator
	
	private JDDNode checkPCTLSteadyState(PCTLSS pctl) throws PrismException
	{
		// formula stuff
		Expression pe;	// probability bound (expression)
		double p = 0;	// probability value (actual value)
		String relOp;	// relational operator
		// bscc stuff
		Vector vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode b, filter, bscc, diags, emb, sol, tmp;
		// other stuff
		StateProbs probs = null, totalProbs = null;
		int i, n;
		double d, probBSCCs[];
		boolean sat;
		
		// get info from steady-state operator
		relOp = pctl.getRelOp();
		pe = pctl.getProb();
		if (pe != null) {
			p = pe.evaluateDouble(constantValues, null);
			if (p < 0 || p > 1) throw new PrismException("Invalid probability bound " + p + " in until formula");
		}
		
		// check for trivial (i.e. stupid) cases
		if (pe != null) {
			if ((p == 0 && relOp.equals(">=")) || (p == 1 && relOp.equals("<="))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p + " - formula trivially satisfies all states\n");
				JDD.Ref(reach);
				return reach;
			}
			else if ((p == 0 && relOp.equals("<")) || (p == 1 && relOp.equals(">"))) {
				mainLog.print("\nWarning: checking for probability " + relOp + " " + p + " - formula trivially satisfies no states\n");
				return JDD.Constant(0);
			}
		}
		
		// translate b into a bdd
		// (i.e. model check argument first)
		b = checkPCTLFormula(pctl.getOperand());
		
		// translate filter (if present)
		filter = null;
		if (pctl.getFilter() != null) {
			filter = checkPCTLFormula(pctl.getFilter());
			// if filter is empty, warn and ignore
			if (filter.equals(JDD.ZERO)) {
				mainLog.println("\nWarning: Filter \"" + pctl.getFilter() + "\" satisfies no states and is being ignored");
				JDD.Deref(filter);
				filter = null;
			}
		}
		
		// print out some basic info
		mainLog.print("\nCSL Steady State:\n");
		mainLog.print("\nb = " + JDD.GetNumMintermsString(b, allDDRowVars.n()) + " states\n");
		
		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			mainLog.print("\nComputing (B)SCCs...");
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getVectBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			n = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			n = 1;
		}
		
		// compute steady state for each bscc...
		probBSCCs = new double[n];
		for (i = 0; i < n; i++) {
			
			mainLog.println("\nComputing steady state probabilities for BSCC " + (i+1));
			
			// get bscc
			bscc = (JDDNode)vectBSCCs.elementAt(i);
			
			// compute steady state probabilities
			try {
				probs = computeSteadyStateProbs(trans, bscc);
			}
			catch (PrismException e) {
				JDD.Deref(b);
				if (filter != null) JDD.Deref(filter);
				for (i = 0; i < n; i++) {
					JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
				}
				JDD.Deref(notInBSCCs);
				throw e;
			}
			
			// round off probabilities
			//probs.roundOff(getPlacesToRoundBy());
			
			// print out probabilities
			if (verbose) {
				mainLog.print("\nBSCC " + (i+1) + " Steady-State Probabilities: \n");
				probs.print(mainLog);
			}
			
			// print out filtered probabilities
			if (filter != null) {
				mainLog.print("\nBSCC " + (i+1) + " Steady-State Probabilities (" + pctl.getFilter() + "): \n");
				probs.printFiltered(mainLog, filter);
			}
			
			// sum probabilities over bdd b
			d = probs.sumOverBDD(b);
			probBSCCs[i] = d;
			mainLog.print("\nBSCC " + (i+1) + " Probability: " + d + "\n");
			
			// free vector
			probs.clear();
		}
		
		// if every state is in a bscc, it's much easier...
		if (notInBSCCs.equals(JDD.ZERO)) {
		
			mainLog.println("\nAll states are in a BSCC (so no reachability probabilities computed)");
			
			// there's more efficient ways to do this if we just create the solution bdd directly
			// but we actually build the prob vector so it can be printed out if necessary
			tmp = JDD.Constant(0);
			for (i = 0; i < n; i++) {
				bscc = (JDDNode)vectBSCCs.elementAt(i);
				JDD.Ref(bscc);
				tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(probBSCCs[i]), bscc));
			}
			probs = new StateProbsMTBDD(tmp, model);
			
			// print out probabilities
			if (verbose) {
				mainLog.print("\nProbabilities for CSL steady-state formula: \n");
				probs.print(mainLog);
			}
			
			// print out filtered probabilities
			if (filter != null) {
				mainLog.print("\nProbabilities for CSL steady-state formula (" + pctl.getFilter() + "): \n");
				probs.printFiltered(mainLog, filter);
			}
			
			// compute bdd of satisfying states
			if (pe != null) {
				sol = probs.getBDDFromInterval(relOp, p);
				// remove unreachable states from solution
				JDD.Ref(reach);
				sol = JDD.And(sol, reach);
			}
			// if there's no bound, result will be a probability (just store empty set for sol)
			else {
				sol = JDD.Constant(0);
				// use filter if present
				if (filter != null) {
					numericalRes = probs.firstFromBDD(filter);
				}
				// otherwise use initial state
				else {
					numericalRes = probs.firstFromBDD(start);
				}
			}
			
			// free vector
			probs.clear();
		}
		// otherwise we have to do more work...
		else {
			
			// initialise total probabilities vector
			switch (engine) {
				case Prism.MTBDD: totalProbs = new StateProbsMTBDD(JDD.Constant(0), model); break;
				case Prism.SPARSE: totalProbs = new StateProbsDV(new DoubleVector((int)numStates), model); break;
				case Prism.HYBRID: totalProbs = new StateProbsDV(new DoubleVector((int)numStates), model); break;
			}
			
			// compute diagonals
			JDD.Ref(trans);
			diags = JDD.SumAbstract(trans, allDDColVars);
			
			// compute embedded markov chain
			JDD.Ref(trans);
			JDD.Ref(diags);
			emb = JDD.Apply(JDD.DIVIDE, trans, diags);
			
			mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
			mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
			
			// compute probabilities of reaching each bscc...
			for (i = 0; i < n; i++) {
				
				// skip bsccs with zero probability
				if (probBSCCs[i] == 0.0) continue;
				
				mainLog.println("\nComputing probabilities of reaching BSCC " + (i+1));
				
				// get bscc
				bscc = (JDDNode)vectBSCCs.elementAt(i);
				
				// compute probabilities
				try {
					probs = computeUntilProbs(emb, trans01, notInBSCCs, bscc);
				}
				catch (PrismException e) {
					JDD.Deref(diags);
					JDD.Deref(emb);
					JDD.Deref(b);
					if (filter != null) JDD.Deref(filter);
					for (i = 0; i < n; i++) {
						JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
					}
					JDD.Deref(notInBSCCs);
					totalProbs.clear();
					throw e;
				}
				
				// round off probabilities
				//probs.roundOff(getPlacesToRoundBy());
				
				// print out probabilities
				if (verbose) {
					mainLog.print("\nBSCC " + (i+1) + " Reachability Probabilities: \n");
					probs.print(mainLog);
				}
				
				// print out filtered probabilities
				if (filter != null) {
					mainLog.print("\nBSCC " + (i+1) + " Reachability Probabilities (" + pctl.getFilter() + "): \n");
					probs.printFiltered(mainLog, filter);
				}
				
				// times by bscc prob, add to total
				probs.timesConstant(probBSCCs[i]);
				totalProbs.add(probs);
				
				// free vector
				probs.clear();
			}
			
			// print out probabilities
			if (verbose) {
				mainLog.print("\nProbabilities for CSL steady-state formula: \n");
				totalProbs.print(mainLog);
			}
			
			// print out filtered probabilities
			if (filter != null) {
				mainLog.print("\nProbabilities for CSL steady-state formula (" + pctl.getFilter() + "): \n");
				totalProbs.printFiltered(mainLog, filter);
			}
			
			// compute bdd of satisfying states
			if (pe != null) {
				sol = totalProbs.getBDDFromInterval(relOp, p);
				// remove unreachable states from solution
				JDD.Ref(reach);
				sol = JDD.And(sol, reach);
			}
			// if there's no bound, result will be a probability (just store empty set for sol)
			else {
				sol = JDD.Constant(0);
				// use filter if present
				if (filter != null) {
					numericalRes = totalProbs.firstFromBDD(filter);
				}
				// otherwise use initial state
				else {
					numericalRes = totalProbs.firstFromBDD(start);
				}
			}
			
			// free vector
			totalProbs.clear();
			
			// derefs
			JDD.Deref(diags);
			JDD.Deref(emb);
		}
		
		// derefs
		JDD.Deref(b);
		if (filter != null) JDD.Deref(filter);
		for (i = 0; i < n; i++) {
			JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
		}
		JDD.Deref(notInBSCCs);
		
		return sol;
	}

	// next
	
	private StateProbs checkPCTLProbNext(PCTLProbNext pctl) throws PrismException
	{
		JDDNode b, diags, emb;
		StateProbs probs = null;
		
		// model check operand first
		b = checkPCTLFormula(pctl.getOperand());
		
		// print out some info about num states
		//mainLog.print("\nb = " + JDD.GetNumMintermsString(b, allDDRowVars.n()) + " states\n");
		
		// compute diagonals
		JDD.Ref(trans);
		diags = JDD.SumAbstract(trans, allDDColVars);
		
		// compute embedded markov chain
		JDD.Ref(trans);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		
		mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
		mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
		
		// compute probabilities
		probs = computeNextProbs(emb, b);
		
		// derefs
		JDD.Deref(b);
		JDD.Deref(diags);
		JDD.Deref(emb);
		
		return probs;
	}

	// bounded until
	
	private StateProbs checkPCTLProbBoundedUntil(PCTLProbBoundedUntil pctl) throws PrismException
	{
		double lTime, uTime;	// time bounds
		Expression expr;
		JDDNode b1, b2, tmp, diags, emb;
		StateProbs tmpProbs = null, probs = null;
		
		// get info from bounded until
		
		// lower bound is 0 if not specified
		// (i.e. if until is of form U<=t)
		expr = pctl.getLowerBound();
		if (expr != null) {
			lTime = expr.evaluateDouble(constantValues, null);
			if (lTime < 0) {
				throw new PrismException("Invalid lower bound " + lTime + " in time-bounded until formula");
			}
		}
		else {
			lTime = 0;
		}
		// upper bound is -1 if not specified
		// (i.e. if until is of form U>=t)
		expr = pctl.getUpperBound();
		if (expr != null) {
			uTime = expr.evaluateDouble(constantValues, null);
			if (uTime < 0) {
				throw new PrismException("Invalid upper bound " + uTime + " in time-bounded until formula");
			}
			if (uTime < lTime) {
				throw new PrismException("Upper bound must exceed lower bound in time-bounded until formula");
			}
		}
		else {
			uTime = -1;
		}
		
		// model check operands first
		b1 = checkPCTLFormula(pctl.getOperand1());
		try {
			b2 = checkPCTLFormula(pctl.getOperand2());
		}
		catch (PrismException e) {
			JDD.Deref(b1);
			throw e;
		}
		
		// print out some info about num states
		//mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1, allDDRowVars.n()));
		//mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2, allDDRowVars.n()) + " states\n");
		
		// compute probabilities
		
		// a trivial case: "U<=0"
		if (lTime == 0 && uTime == 0) {
			// prob is 1 in b2 states, 0 otherwise
			JDD.Ref(b2);
			probs = new StateProbsMTBDD(b2, model);
		}
		else {
		
			// break down into different cases to compute probabilities
			
			// >= lTime
			if (uTime == -1) {
				// check for special case of lTime == 0, this is actually an unbounded until
				if (lTime == 0) {
					// compute diagonals
					JDD.Ref(trans);
					diags = JDD.SumAbstract(trans, allDDColVars);
					// compute embedded markov chain
					JDD.Ref(trans);
					JDD.Ref(diags);
					emb = JDD.Apply(JDD.DIVIDE, trans, diags);
					mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
					mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
					// compute probs
					try {
						probs = computeUntilProbs(emb, trans01, b1, b2);
					}
					catch (PrismException e) {
						JDD.Deref(diags);
						JDD.Deref(emb);
						JDD.Deref(b1);
						JDD.Deref(b2);
						throw e;
					}
					JDD.Deref(diags);
					JDD.Deref(emb);
				}
				else {
					// compute diagonals
					JDD.Ref(trans);
					diags = JDD.SumAbstract(trans, allDDColVars);
					// compute embedded markov chain
					JDD.Ref(trans);
					JDD.Ref(diags);
					emb = JDD.Apply(JDD.DIVIDE, trans, diags);
					mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
					mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
					// compute unbounded until probs
					try {
						tmpProbs = computeUntilProbs(emb, trans01, b1, b2);
					}
					catch (PrismException e) {
						JDD.Deref(diags);
						JDD.Deref(emb);
						JDD.Deref(b1);
						JDD.Deref(b2);
						throw e;
					}
					JDD.Deref(diags);
					JDD.Deref(emb);
					// compute bounded until probs
					try {
						probs = computeBoundedUntilProbs(trans, trans01, b1, b1, lTime, tmpProbs);
					}
					catch (PrismException e) {
						tmpProbs.clear();
						JDD.Deref(b1);
						JDD.Deref(b2);
						throw e;
					}
					tmpProbs.clear();
				}
			}
			// <= uTime
			else if (lTime == 0) {
				// nb: uTime != 0 since would be caught above (trivial case)
				JDD.Ref(b1);
				JDD.Ref(b2);
				tmp = JDD.And(b1, JDD.Not(b2));
				try {
					probs = computeBoundedUntilProbs(trans, trans01, b2, tmp, uTime, null);
				}
				catch (PrismException e) {
					JDD.Deref(tmp);
					JDD.Deref(b1);
					JDD.Deref(b2);
					throw e;
				}
				JDD.Deref(tmp);
			}
			// [lTime,uTime] (including where lTime == uTime)
			else {
				JDD.Ref(b1);
				JDD.Ref(b2);
				tmp = JDD.And(b1, JDD.Not(b2));
				try {
					tmpProbs = computeBoundedUntilProbs(trans, trans01, b2, tmp, uTime-lTime, null);
				}
				catch (PrismException e) {
					JDD.Deref(tmp);
					JDD.Deref(b1);
					JDD.Deref(b2);
					throw e;
				}
				JDD.Deref(tmp);
				try {
					probs = computeBoundedUntilProbs(trans, trans01, b1, b1, lTime, tmpProbs);
				}
				catch (PrismException e) {
					tmpProbs.clear();
					JDD.Deref(b1);
					JDD.Deref(b2);
					throw e;
				}
				tmpProbs.clear();
			}
		}
		
		// derefs
		JDD.Deref(b1);
		JDD.Deref(b2);
		
		return probs;
	}

	// until (unbounded)
	
	private StateProbs checkPCTLProbUntil(PCTLProbUntil pctl, Expression pe, double p) throws PrismException
	{
		JDDNode b1, b2, diags, emb;
		StateProbs probs = null;
		
		// model check operands first
		b1 = checkPCTLFormula(pctl.getOperand1());
		try {
			b2 = checkPCTLFormula(pctl.getOperand2());
		}
		catch (PrismException e) {
			JDD.Deref(b1);
			throw e;
		}
		
		// print out some info about num states
		mainLog.print("\nb1 = " + JDD.GetNumMintermsString(b1, allDDRowVars.n()));
		mainLog.print(" states, b2 = " + JDD.GetNumMintermsString(b2, allDDRowVars.n()) + " states\n");
		
		// if p is 0 or 1 and precomputation algorithms are enabled, compute probabilities qualitatively
		if (pe != null && ((p==0) || (p==1)) & precomp) {
			mainLog.print("\nWarning: probability bound in formula is " + p + " so exact probabilities may not be computed\n");
			probs = computeUntilProbsQual(trans01, b1, b2, p);
		}
		// otherwise actually compute probabilities
		else {
			// compute diagonals
			JDD.Ref(trans);
			diags = JDD.SumAbstract(trans, allDDColVars);
			
			// compute embedded markov chain
			JDD.Ref(trans);
			JDD.Ref(diags);
			emb = JDD.Apply(JDD.DIVIDE, trans, diags);
			
			mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
			mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
			
			// compute probabilities
			try {
				probs = computeUntilProbs(emb, trans01, b1, b2);
			}
			catch (PrismException e) {
				JDD.Deref(b1);
				JDD.Deref(b2);
				JDD.Deref(diags);
				JDD.Deref(emb);
				throw e;
			}
			
			// derefs
			JDD.Deref(diags);
			JDD.Deref(emb);
		}
		
		// derefs
		JDD.Deref(b1);
		JDD.Deref(b2);
		
		return probs;
	}
	
	// cumulative reward
	
	private StateProbs checkPCTLRewardCumul(PCTLRewardCumul pctl) throws PrismException
	{
		double time;	// time
		Expression expr;
		StateProbs rewards = null;
		
		// get info from inst reward
		expr = pctl.getBound();
		if (expr != null) {
			time = expr.evaluateDouble(constantValues, null);
			if (time < 0) {
				throw new PrismException("Invalid time bound " + time + " in cumulative reward formula");
			}
		}
		else {
			throw new PrismException("No time bound specified in cumulative reward formula");
		}
		
		// compute rewards
		
		// a trivial case: "<=0"
		if (time == 0) {
			rewards = new StateProbsMTBDD(JDD.Constant(0), model);
		}
		else {
			// compute rewards
			try {
				rewards = computeCumulRewards(trans, trans01, stateRewards, transRewards, time);
			}
			catch (PrismException e) {
				throw e;
			}
		}
		
		return rewards;
	}

	// inst reward
	
	private StateProbs checkPCTLRewardInst(PCTLRewardInst pctl) throws PrismException
	{
		double time;	// time
		Expression expr;
		StateProbs sr = null, rewards = null;
		
		// get info from inst reward
		expr = pctl.getTime();
		if (expr != null) {
			time = expr.evaluateDouble(constantValues, null);
			if (time < 0) {
				throw new PrismException("Invalid time instant " + time + " in instantaneous reward formula");
			}
		}
		else {
			throw new PrismException("No time instant specified in instantaneous reward formula");
		}
		
		// compute rewards
		
		// a trivial case: "=0"
		if (time == 0) {
			JDD.Ref(stateRewards);
			rewards = new StateProbsMTBDD(stateRewards, model);
		}
		else {
			// convert state rewards vector to appropriate type (depending on engine)
			switch (engine) {
				case Prism.MTBDD: JDD.Ref(stateRewards); sr = new StateProbsMTBDD(stateRewards, model); break;
				case Prism.SPARSE: sr = new StateProbsDV(stateRewards, model); break;
				case Prism.HYBRID: sr = new StateProbsDV(stateRewards, model); break;
			}
			// and for the computation, we can reuse the computation for time-bounded until formulae
			// which is nice
			try {
				rewards = computeBoundedUntilProbs(trans, trans01, reach, reach, time, sr);
			}
			catch (PrismException e){
				sr.clear();
				throw e;
			}
			sr.clear();
		}
		
		return rewards;
	}

	// reach reward
	
	private StateProbs checkPCTLRewardReach(PCTLRewardReach pctl) throws PrismException
	{
		JDDNode b, diags, emb, newStateRewards;
		StateProbs rewards = null;
		
		// model check operand first
		b = checkPCTLFormula(pctl.getOperand());
		
		// print out some info about num states
		//mainLog.print("\nb = " + JDD.GetNumMintermsString(b, allDDRowVars.n()) + " states\n");
		
		// compute diagonals
		JDD.Ref(trans);
		diags = JDD.SumAbstract(trans, allDDColVars);
		
		// compute embedded markov chain
		JDD.Ref(trans);
		JDD.Ref(diags);
		emb = JDD.Apply(JDD.DIVIDE, trans, diags);
		
		mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
		mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
		
		// convert rewards
		JDD.Ref(stateRewards);
		JDD.Ref(diags);
		newStateRewards = JDD.Apply(JDD.DIVIDE, stateRewards, diags);
		
		// compute rewards
		try {
			rewards = computeReachRewards(emb, trans01, newStateRewards, transRewards, b);
		}
		catch (PrismException e) {
			JDD.Deref(b);
			JDD.Deref(emb);
			JDD.Deref(diags);
			JDD.Deref(newStateRewards);
			throw e;
		}
		
		// derefs
		JDD.Deref(b);
		JDD.Deref(emb);
		JDD.Deref(diags);
		JDD.Deref(newStateRewards);
		
		return rewards;
	}

	// steady state reward
	
	private StateProbs checkPCTLRewardSS(PCTLRewardSS pctl, JDDNode filter, PCTLFormula filterName) throws PrismException
	{
		// bscc stuff
		Vector vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode newStateRewards, bscc, diags, emb, sol, tmp;
		// other stuff
		StateProbs probs = null, rewards = null;
		int i, n;
		double d, rewBSCCs[];
		boolean sat;
		
		// compute rewards corresponding to each state
		JDD.Ref(trans);
		JDD.Ref(transRewards);
		newStateRewards = JDD.SumAbstract(JDD.Apply(JDD.TIMES, trans, transRewards), allDDColVars);
		JDD.Ref(stateRewards);
		newStateRewards = JDD.Apply(JDD.PLUS, newStateRewards, stateRewards);
		
		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			mainLog.print("\nComputing (B)SCCs...");
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getVectBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			n = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			n = 1;
		}
		
		// compute steady state for each bscc...
		rewBSCCs = new double[n];
		for (i = 0; i < n; i++) {
			
			mainLog.println("\nComputing steady state probabilities for BSCC " + (i+1));
			
			// get bscc
			bscc = (JDDNode)vectBSCCs.elementAt(i);
			
			// compute steady state probabilities
			try {
				probs = computeSteadyStateProbs(trans, bscc);
			}
			catch (PrismException e) {
				JDD.Deref(newStateRewards);
				for (i = 0; i < n; i++) {
					JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
				}
				JDD.Deref(notInBSCCs);
				throw e;
			}
			
			// round off probabilities
			//probs.roundOff(getPlacesToRoundBy());
			
			// print out probabilities
			if (verbose) {
				mainLog.print("\nBSCC " + (i+1) + " Steady-State Probabilities: \n");
				probs.print(mainLog);
			}
			
			// print out filtered probabilities
			if (filter != null) {
				mainLog.print("\nBSCC " + (i+1) + " Steady-State Probabilities (" + filterName + "): \n");
				probs.printFiltered(mainLog, filter);
			}
			
			// do weighted sum of probabilities and rewards
			JDD.Ref(bscc);
			JDD.Ref(newStateRewards);
			tmp = JDD.Apply(JDD.TIMES, bscc, newStateRewards);
			d = probs.sumOverMTBDD(tmp);
			rewBSCCs[i] = d;
			mainLog.print("\nBSCC " + (i+1) + " Reward: " + d + "\n");
			JDD.Deref(tmp);
			
			// free vector
			probs.clear();
		}
		
		// if every state is in a bscc, it's much easier...
		if (notInBSCCs.equals(JDD.ZERO)) {
		
			mainLog.println("\nAll states are in a BSCC (so no reachability probabilities computed)");
			
			// build the reward vector
			tmp = JDD.Constant(0);
			for (i = 0; i < n; i++) {
				bscc = (JDDNode)vectBSCCs.elementAt(i);
				JDD.Ref(bscc);
				tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(rewBSCCs[i]), bscc));
			}
			rewards = new StateProbsMTBDD(tmp, model);
		}
		// otherwise we have to do more work...
		else {
			
			// initialise rewards vector
			switch (engine) {
				case Prism.MTBDD: rewards = new StateProbsMTBDD(JDD.Constant(0), model); break;
				case Prism.SPARSE: rewards = new StateProbsDV(new DoubleVector((int)numStates), model); break;
				case Prism.HYBRID: rewards = new StateProbsDV(new DoubleVector((int)numStates), model); break;
			}
			
			// compute diagonals
			JDD.Ref(trans);
			diags = JDD.SumAbstract(trans, allDDColVars);
			
			// compute embedded markov chain
			JDD.Ref(trans);
			JDD.Ref(diags);
			emb = JDD.Apply(JDD.DIVIDE, trans, diags);
			
			mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
			mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
			
			// compute probabilities of reaching each bscc...
			for (i = 0; i < n; i++) {
				
				// skip bsccs with zero reward
				if (rewBSCCs[i] == 0.0) continue;
				
				mainLog.println("\nComputing probabilities of reaching BSCC " + (i+1));
				
				// get bscc
				bscc = (JDDNode)vectBSCCs.elementAt(i);
				
				// compute probabilities
				try {
					probs = computeUntilProbs(emb, trans01, notInBSCCs, bscc);
				}
				catch (PrismException e) {
					JDD.Deref(diags);
					JDD.Deref(emb);
					throw e;
				}
				
				// round off probabilities
				//probs.roundOff(getPlacesToRoundBy());
				
				// print out probabilities
				if (verbose) {
					mainLog.print("\nBSCC " + (i+1) + " Reachability Probabilities: \n");
					probs.print(mainLog);
				}
				
				// print out filtered probabilities
				if (filter != null) {
					mainLog.print("\nBSCC " + (i+1) + " Reachability Probabilities (" + filterName + "): \n");
					probs.printFiltered(mainLog, filter);
				}
				
				// times by bscc reward, add to total
				probs.timesConstant(rewBSCCs[i]);
				rewards.add(probs);
				
				// free vector
				probs.clear();
			}
			
			// derefs
			JDD.Deref(diags);
			JDD.Deref(emb);
		}
		
		// derefs
		JDD.Deref(newStateRewards);
		if (filter != null) JDD.Deref(filter);
		for (i = 0; i < n; i++) {
			JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
		}
		JDD.Deref(notInBSCCs);
		
		return rewards;
	}

	// check pctl expression
	
	private JDDNode checkPCTLExpression(PCTLExpression pctl) throws PrismException
	{
		// pass this work onto the Expression2MTBDD object
		return expr2mtbdd.translateExpression(pctl.getExpression());
	}

	// check pctl label
	
	private JDDNode checkPCTLLabel(PCTLLabel pctl) throws PrismException
	{
		LabelList ll;
		Expression expr;
		JDDNode dd;
		int i;
		
		// treat special case
		if (pctl.getName().equals("deadlock")) {
			dd = model.getFixedDeadlocks();
			JDD.Ref(dd);
			return dd;
		}
		// get expression associated with label
		ll = propertiesFile.getLabelList();
		i = ll.getLabelIndex(pctl.getName());
		if (i == -1) throw new PrismException("Unknown label \"" + pctl.getName() + "\" in CSL formula");
		expr = ll.getLabel(i);
		// pass this work onto the Expression2MTBDD object
		return expr2mtbdd.translateExpression(expr);
	}

	//-----------------------------------------------------------------------------------
	// do steady state computation
	//-----------------------------------------------------------------------------------

	// steady state computation (from initial states)
	
	public StateProbs doSteadyState() throws PrismException
	{
		// bscc stuff
		Vector vectBSCCs;
		JDDNode notInBSCCs;
		// mtbdd stuff
		JDDNode start, bscc, diags, emb, tmp;
		// other stuff
		StateProbs probs = null, solnProbs = null;
		double d, probBSCCs[];
		int i, n, whichBSCC, bsccCount;
		
		// compute bottom strongly connected components (bsccs)
		if (bsccComp) {
			mainLog.print("\nComputing (B)SCCs...");
			sccComputer.computeBSCCs();
			vectBSCCs = sccComputer.getVectBSCCs();
			notInBSCCs = sccComputer.getNotInBSCCs();
			n = vectBSCCs.size();
		}
		// unless we've been told to skip it
		else {
			mainLog.println("\nSkipping BSCC computation...");
			vectBSCCs = new Vector();
			JDD.Ref(reach);
			vectBSCCs.add(reach);
			notInBSCCs = JDD.Constant(0);
			n = 1;
		}
		
		// get initial states of model
		start = model.getStart();
		
		// see how many bsccs contain initial states and, if just one, which one
		whichBSCC = -1;
		bsccCount = 0;
		for (i = 0; i < n; i++) {
			bscc = (JDDNode)vectBSCCs.elementAt(i);
			JDD.Ref(bscc);
			JDD.Ref(start);
			tmp = JDD.And(bscc, start);
			if (!tmp.equals(JDD.ZERO)) {
				bsccCount++;
				if (bsccCount == 1) whichBSCC = i;
			}
			JDD.Deref(tmp);
		}
		
		// if all initial states are in a single bscc, it's easy...
		JDD.Ref(notInBSCCs);
		JDD.Ref(start);
		tmp = JDD.And(notInBSCCs, start);
		if (tmp.equals(JDD.ZERO) && bsccCount == 1) {
			
			JDD.Deref(tmp);
			
			mainLog.println("\nInitial states all in one BSCC (so no reachability probabilities computed)");
			
			// get bscc
			bscc = (JDDNode)vectBSCCs.elementAt(whichBSCC);
			
			// compute steady-state probabilities for the bscc
			try { 
				solnProbs = computeSteadyStateProbs(trans, bscc);
			}
			catch (PrismException e) {
				for (i = 0; i < n; i++) {
					JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
				}
				JDD.Deref(notInBSCCs);
				throw e;
			}
			
			// round off probabilities
			//solnProbs.roundOff(getPlacesToRoundBy());
		}
		
		// otherwise have to consider all the bsccs
		else {
			
			JDD.Deref(tmp);
			
			// initialise total probabilities vector
			switch (engine) {
				case Prism.MTBDD: solnProbs = new StateProbsMTBDD(JDD.Constant(0), model); break;
				case Prism.SPARSE: solnProbs = new StateProbsDV(new DoubleVector((int)numStates), model); break;
				case Prism.HYBRID: solnProbs = new StateProbsDV(new DoubleVector((int)numStates), model); break;
			}
			
			// compute diagonals
			JDD.Ref(trans);
			diags = JDD.SumAbstract(trans, allDDColVars);
			
			// compute embedded markov chain
			JDD.Ref(trans);
			JDD.Ref(diags);
			emb = JDD.Apply(JDD.DIVIDE, trans, diags);
			
			mainLog.print("\nDiagonals vector: ");JDD.PrintInfo(diags, allDDRowVars.n());
			mainLog.print("Embedded Markov chain: ");JDD.PrintInfo(emb, allDDRowVars.n()*2);
			
			// compute prob of reaching each bscc from initial state
			probBSCCs = new double[n];
			for (i = 0; i < n; i++) {
				
				mainLog.println("\nComputing probability of reaching BSCC " + (i+1));
				
				// get bscc
				bscc = (JDDNode)vectBSCCs.elementAt(i);
				
				// compute probabilities
				try {
					probs = computeUntilProbs(emb, trans01, notInBSCCs, bscc);
				}
				catch (PrismException e) {
					JDD.Deref(diags);
					JDD.Deref(emb);
					for (i = 0; i < n; i++) {
						JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
					}
					JDD.Deref(notInBSCCs);
					solnProbs.clear();
					throw e;
				}
				
				// round off probabilities
				//probs.roundOff(getPlacesToRoundBy());
				
				// sum probabilities over bdd for initial state
				// and then divide by number of start states
				// (we assume an equiprobable initial probability distribution over all initial states)
				d = probs.sumOverBDD(start);
				d /= model.getNumStartStates();
				probBSCCs[i] = d;
				mainLog.print("\nBSCC " + (i+1) + " Probability: " + d + "\n");
				
				// free vector
				probs.clear();
			}
			
			// derefs
			JDD.Deref(diags);
			JDD.Deref(emb);
			
			// compute steady-state for each bscc
			for (i = 0; i < n; i++) {
				
				mainLog.println("\nComputing steady-state probabilities for BSCC " + (i+1));
				
				// get bscc
				bscc = (JDDNode)vectBSCCs.elementAt(i);
				
				// compute steady-state probabilities for the bscc
				try { 
					probs = computeSteadyStateProbs(trans, bscc);
				}
				catch (PrismException e) {
					for (i = 0; i < n; i++) {
						JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
					}
					JDD.Deref(notInBSCCs);
					solnProbs.clear();
					throw e;
				}
				
				// round off probabilities
				//probs.roundOff(getPlacesToRoundBy());
				
				// print out probabilities
				if (verbose) {
					mainLog.print("\nBSCC " + (i+1) + " Steady-State Probabilities: \n");
					probs.print(mainLog);
				}
				
				// times by bscc reach prob, add to total
				probs.timesConstant(probBSCCs[i]);
				solnProbs.add(probs);
				
				// free vector
				probs.clear();
			}
		}
		
		// derefs
		for (i = 0; i < n; i++) {
			JDD.Deref((JDDNode)vectBSCCs.elementAt(i));
		}
		JDD.Deref(notInBSCCs);
		
		return solnProbs;
	}

	//-----------------------------------------------------------------------------------
	// do transient computation
	//-----------------------------------------------------------------------------------

	// transient computation (from initial states)
	
	public StateProbs doTransient(double time) throws PrismException
	{
		// mtbdd stuff
		JDDNode start, init;
		// other stuff
		StateProbs probs = null;
		
		// get initial states of model
		start = model.getStart();
		
		// and hence compute initial probability distribution (equiprobable over all start states)
		JDD.Ref(start);
		init = JDD.Apply(JDD.DIVIDE, start, JDD.Constant(JDD.GetNumMinterms(start, allDDRowVars.n())));
		
		// compute transient probabilities
		try {
			probs = computeTransientProbs(trans, init, time);
		}
		catch (PrismException e) {
			JDD.Deref(init);
			throw e;
		}
		
		// round off probabilities
		//probs.roundOff(getPlacesToRoundBy());
			
		// derefs
		JDD.Deref(init);
		
		return probs;
	}

	//-----------------------------------------------------------------------------------
	// probability computation methods
	//-----------------------------------------------------------------------------------

	// compute probabilities for next
	
	private StateProbs computeNextProbs(JDDNode tr, JDDNode b)
	{
		JDDNode tmp;
		StateProbs probs = null;
		
		// matrix multiply: trans * b
		JDD.Ref(b);
		tmp = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		JDD.Ref(tr);
		tmp = JDD.MatrixMultiply(tr, tmp, allDDColVars, JDD.BOULDER);
		probs = new StateProbsMTBDD(tmp, model);
		
		return probs;
	}

	// compute probabilities for bounded until
	
	// nb: this is a generic function used by several different parts of the csl bounded until
	//     model checking algorithm. it actually computes, for each state, the sum over 'b2' states
	//     of the probability of being in that state at time 'time' multiplied by the corresponding
	//     probability in the vector 'multProbs', assuming that all states not in 'nonabs' are absorbing
	// nb: if 'multProbs' is null it is assumed to be all 1s
	// nb: if not null, the type (StateProbsDV/MTBDD) of 'multProbs' must match the current engine
	//     i.e. DV for sparse/hybrid, MTBDD for mtbdd
	
	private StateProbs computeBoundedUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b2, JDDNode nonabs, double time, StateProbs multProbs) throws PrismException
	{
		JDDNode multProbsMTBDD, probsMTBDD;
		DoubleVector multProbsDV, probsDV;
		StateProbs probs = null;
				
		// if nonabs is empty and multProbs was null, we don't need to do any further solution
		// likewise if time = 0 (and in this case we know multProbs will be null anyway
		// because U[0,0] is treated as a special case)
		if ((nonabs.equals(JDD.ZERO) && multProbs == null) || (time == 0)) {
			switch (engine) {
			case Prism.MTBDD: 
				JDD.Ref(b2);
				probs = new StateProbsMTBDD(b2, model);
				break;
			case Prism.SPARSE: 
			case Prism.HYBRID:
				probs = new StateProbsDV(b2, model);
				break;
			}
		}
		// otherwise explicitly compute the probabilities
		else {
			// compute probabilities
			try {
				switch (engine) {
				case Prism.MTBDD:
					multProbsMTBDD = (multProbs == null) ? null : ((StateProbsMTBDD)multProbs).getJDDNode();
					probsMTBDD = PrismMTBDD.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time, multProbsMTBDD);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					multProbsDV = (multProbs == null) ? null : ((StateProbsDV)multProbs).getDoubleVector();
					probsDV = PrismSparse.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time, multProbsDV);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					multProbsDV = (multProbs == null) ? null : ((StateProbsDV)multProbs).getDoubleVector();
					probsDV = PrismHybrid.StochBoundedUntil(tr, odd, allDDRowVars, allDDColVars, b2, nonabs, time, multProbsDV);
					probs = new StateProbsDV(probsDV, model);
					break;
				}
			}
			catch (PrismException e) {
				throw e;
			}
		}
		
		return probs;
	}

	// compute probabilities for until (for qualitative properties)
	
	private StateProbs computeUntilProbsQual(JDDNode tr01, JDDNode b1, JDDNode b2, double p)
	{
		JDDNode yes, no, maybe;
		StateProbs probs = null;
		
		// note: we know precomputation is enabled else this function wouldn't have been called
		
		// compute yes/no/maybe states
		if (b2.equals(JDD.ZERO)) {
			yes = JDD.Constant(0);
			JDD.Ref(reach);
			no = reach;
			maybe = JDD.Constant(0);
		}
		else if (b1.equals(JDD.ZERO)) {
			JDD.Ref(b2);
			yes = b2;
			JDD.Ref(reach);
			JDD.Ref(b2);
			no = JDD.And(reach, JDD.Not(b2));
			maybe = JDD.Constant(0);
		}
		else {
			// yes
			yes = PrismMTBDD.Prob1(tr01, allDDRowVars, allDDColVars, b1, b2);
			if (yes.equals(reach)) {
				no = JDD.Constant(0);
			} else {
				no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, b1, yes);
			}
			// maybe
			JDD.Ref(reach);
			JDD.Ref(yes);
			JDD.Ref(no);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(yes, no)));
		}
		
		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, allDDRowVars.n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");
		
		// if maybe is empty, we have the probabilities already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(yes);
			probs = new StateProbsMTBDD(yes, model);
		}
		// p = 0
		else if (p == 0) {
			// anything that's unknown but definitely > 0
			// may as well be 1
			JDD.Ref(yes);
			JDD.Ref(maybe);
			probs = new StateProbsMTBDD(JDD.Or(yes, maybe), model);
		}
		// p = 1
		else {
			// anything that's unknown but definitely < 1
			// may as well be 0
			JDD.Ref(yes);
			probs = new StateProbsMTBDD(yes, model);
		}
		
		// derefs
		JDD.Deref(yes);
		JDD.Deref(no);
		JDD.Deref(maybe);
		
		return probs;
	}

	// compute probabilities for until (general case)
	
	private StateProbs computeUntilProbs(JDDNode tr, JDDNode tr01, JDDNode b1, JDDNode b2) throws PrismException
	{
		JDDNode yes, no, maybe;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;
		
		// compute yes/no/maybe states
		if (b2.equals(JDD.ZERO)) {
			yes = JDD.Constant(0);
			JDD.Ref(reach);
			no = reach;
			maybe = JDD.Constant(0);
		}
		else if (b1.equals(JDD.ZERO)) {
			JDD.Ref(b2);
			yes = b2;
			JDD.Ref(reach);
			JDD.Ref(b2);
			no = JDD.And(reach, JDD.Not(b2));
			maybe = JDD.Constant(0);
		}
		else {
			// yes
			if (precomp) {
				yes = PrismMTBDD.Prob1(tr01, allDDRowVars, allDDColVars, b1, b2);
			}
			else {
				JDD.Ref(b2);
				yes = b2;
			}
			// no
			if (yes.equals(reach)) {
				no = JDD.Constant(0);
			} else if (precomp) {
				no = PrismMTBDD.Prob0(tr01, reach, allDDRowVars, allDDColVars, b1, yes);
			} else {
				JDD.Ref(reach);
				JDD.Ref(b1);
				JDD.Ref(b2);
				no = JDD.And(reach, JDD.Not(JDD.Or(b1, b2)));
			}
			// maybe
			JDD.Ref(reach);
			JDD.Ref(yes);
			JDD.Ref(no);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(yes, no)));
		}
		
		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, allDDRowVars.n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");
		
		// if maybe is empty, we have the probabilities already
		if (maybe.equals(JDD.ZERO)) {
			// nb: we make sure to return a vector of the appropriate type
			//     (doublevector for hybrid/sparse, mtbdd for mtbdd)
			//     this is because bounded until uses this method too
			switch (engine) {
			case Prism.MTBDD: 
				JDD.Ref(yes);
				probs = new StateProbsMTBDD(yes, model);
				break;
			case Prism.SPARSE: 
			case Prism.HYBRID:
				probs = new StateProbsDV(yes, model);
				break;
			}
		}
		// otherwise we compute the actual probabilities
		else {		
			// compute probabilities
			mainLog.println("\nComputing remaining probabilities...");
			
			try {
				switch (engine) {
				case Prism.MTBDD:
					probsMTBDD = PrismMTBDD.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateProbsMTBDD(probsMTBDD, model);
					break;
				case Prism.SPARSE:
					probsDV = PrismSparse.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateProbsDV(probsDV, model);
					break;
				case Prism.HYBRID:
					probsDV = PrismHybrid.ProbUntil(tr, odd, allDDRowVars, allDDColVars, yes, maybe);
					probs = new StateProbsDV(probsDV, model);
					break;
				default: throw new PrismException("Engine does not support this numerical method");
				}
			}
			catch (PrismException e) {
				JDD.Deref(yes);
				JDD.Deref(no);
				JDD.Deref(maybe);
				throw e;
			}
		}
		
		// derefs
		JDD.Deref(yes);
		JDD.Deref(no);
		JDD.Deref(maybe);
		
		return probs;
	}

	// compute cumulative rewards
	
	private StateProbs computeCumulRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, double time) throws PrismException
	{
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;
		
		// compute rewards
		try {
			switch (engine) {
			case Prism.MTBDD:
				rewardsMTBDD = PrismMTBDD.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsMTBDD(rewardsMTBDD, model);
				break;
			case Prism.SPARSE:
				rewardsDV = PrismSparse.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsDV(rewardsDV, model);
				break;
			case Prism.HYBRID:
				rewardsDV = PrismHybrid.StochCumulReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, time);
				rewards = new StateProbsDV(rewardsDV, model);
				break;
			default: throw new PrismException("Engine does not support this numerical method");
			}
		}
		catch (PrismException e) {
			throw e;
		}
		
		return rewards;
	}

	// compute rewards for reach reward
	
	private StateProbs computeReachRewards(JDDNode tr, JDDNode tr01, JDDNode sr, JDDNode trr, JDDNode b) throws PrismException
	{
		JDDNode inf, maybe;
		JDDNode rewardsMTBDD;
		DoubleVector rewardsDV;
		StateProbs rewards = null;
		
		// compute states which can't reach goal with probability 1
		if (b.equals(JDD.ZERO)) {
			JDD.Ref(reach);
			inf = reach;
			maybe = JDD.Constant(0);
		}
		else if (b.equals(reach)) {
			inf = JDD.Constant(0);
			maybe = JDD.Constant(0);
		}
		else {
			JDDNode prob1 = PrismMTBDD.Prob1(tr01, allDDRowVars, allDDColVars, reach, b);
			JDD.Ref(reach);
			inf = JDD.And(reach, JDD.Not(prob1));
			JDD.Ref(reach);
			JDD.Ref(inf);
			JDD.Ref(b);
			maybe = JDD.And(reach, JDD.Not(JDD.Or(inf, b)));
		}
		
		// print out yes/no/maybe
		mainLog.print("\ngoal = " + JDD.GetNumMintermsString(b, allDDRowVars.n()));
		mainLog.print(", inf = " + JDD.GetNumMintermsString(inf, allDDRowVars.n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, allDDRowVars.n()) + "\n");
		
		// if maybe is empty, we have the rewards already
		if (maybe.equals(JDD.ZERO)) {
			JDD.Ref(inf);
			rewards = new StateProbsMTBDD(JDD.ITE(inf, JDD.PlusInfinity(), JDD.Constant(0)), model);
		}
		// otherwise we compute the actual rewards
		else {
			// compute the rewards
			mainLog.println("\nComputing remaining rewards...");
			
			try {
				switch (engine) {
				case Prism.MTBDD:
					rewardsMTBDD = PrismMTBDD.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateProbsMTBDD(rewardsMTBDD, model);
					break;
				case Prism.SPARSE:
					rewardsDV = PrismSparse.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				case Prism.HYBRID:
					rewardsDV = PrismHybrid.ProbReachReward(tr, sr, trr, odd, allDDRowVars, allDDColVars, b, inf, maybe);
					rewards = new StateProbsDV(rewardsDV, model);
					break;
				default: throw new PrismException("Engine does not support this numerical method");
				}
			}
			catch (PrismException e) {
				JDD.Deref(inf);
				JDD.Deref(maybe);
				throw e;
			}
		}
		
		// derefs
		JDD.Deref(inf);
		JDD.Deref(maybe);
		
		return rewards;
	}

	// compute steady-state probabilities
	
	// tr = the rate matrix for the whole ctmc
	// states = the subset of reachable states (e.g. bscc) for which steady-state is to be done
	
	private StateProbs computeSteadyStateProbs(JDDNode tr, JDDNode subset) throws PrismException
	{
		JDDNode trf, init;
		long n;
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;
		
		// work out number of states in 'subset'
		if (tr.equals(reach)) {
			// avoid a call to GetNumMinterms in this simple (and common) case
			n = numStates;
		} else {
			n = (long)JDD.GetNumMinterms(subset, allDDRowVars.n());
		}
		
		// special case - there is only one state in 'subset'
		// (in fact, we need to check for this special case because the general solution work breaks)
		if (n == 1) {
			// answer is trivially one in the single state
			switch (engine) {
				case Prism.MTBDD: JDD.Ref(subset); return new StateProbsMTBDD(subset, model);
				case Prism.SPARSE: return new StateProbsDV(subset, model);
				case Prism.HYBRID: return new StateProbsDV(subset, model);
			}
		}
		
		// filter out unwanted states from transition matrix
		JDD.Ref(tr);
		JDD.Ref(subset);
		trf = JDD.Apply(JDD.TIMES, tr, subset);
		JDD.Ref(subset);
		trf = JDD.Apply(JDD.TIMES, trf, JDD.PermuteVariables(subset, allDDRowVars, allDDColVars));
		
		// compute initial solution (equiprobable)
		JDD.Ref(subset);
		init = JDD.Apply(JDD.DIVIDE, subset, JDD.Constant(n));
		
		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateProbsMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateProbsDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.StochSteadyState(trf, odd, init, allDDRowVars, allDDColVars);
				probs = new StateProbsDV(probsDV, model);
				break;
			default: throw new PrismException("Engine does not support this numerical method");
			}
		}
		catch (PrismException e) {
			JDD.Deref(trf);
			JDD.Deref(init);
			throw e;
		}
		
		// derefs
		JDD.Deref(trf);
		JDD.Deref(init);
		
		return probs;
	}

	// compute transient probabilities
	
	private StateProbs computeTransientProbs(JDDNode tr, JDDNode init, double time) throws PrismException
	{
		JDDNode probsMTBDD;
		DoubleVector probsDV;
		StateProbs probs = null;
		PrismException ex;
		
		try {
			switch (engine) {
			case Prism.MTBDD:
				probsMTBDD = PrismMTBDD.StochTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsMTBDD(probsMTBDD, model);
				break;
			case Prism.SPARSE:
				probsDV = PrismSparse.StochTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsDV(probsDV, model);
				break;
			case Prism.HYBRID:
				probsDV = PrismHybrid.StochTransient(tr, odd, init, allDDRowVars, allDDColVars, time);
				probs = new StateProbsDV(probsDV, model);
				break;
			default: throw new PrismException("Engine does not support this numerical method");
			}
		}
		catch (PrismException e) {
			throw e;
		}
		
		return probs;
	}
}
