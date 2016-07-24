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

package pta;

import java.util.*;

import parser.*;
import parser.ast.*;
import parser.type.*;
import parser.visitor.ASTTraverseModify;
import prism.*;

/**
 * Model checker for probabilistic timed automata (PTAs).
 */
public class PTAModelChecker extends PrismComponent
{
	// Model file
	private ModulesFile modulesFile;
	// Properties file
	private PropertiesFile propertiesFile;
	// Constants from model
	private Values constantValues;
	// Label list (combined: model/properties file)
	private LabelList labelList;
	// PTA
	private PTA pta;
	// Mapping from all (original model) variables to non-clocks only
	private int[] nonClockVarMap;

	/**
	 * Constructor.
	 */
	public PTAModelChecker(PrismComponent parent, ModulesFile modulesFile, PropertiesFile propertiesFile) throws PrismException
	{
		super(parent);
		this.modulesFile = modulesFile;
		this.propertiesFile = propertiesFile;

		// Get combined constant values from model/properties
		constantValues = new Values();
		constantValues.addValues(modulesFile.getConstantValues());
		if (propertiesFile != null)
			constantValues.addValues(propertiesFile.getConstantValues());

		// Build a combined label list and expand any constants
		// (note labels in model are ignored (removed) during PTA translation so need to store here)
		labelList = new LabelList();
		for (int i = 0; i < modulesFile.getLabelList().size(); i++) {
			labelList.addLabel(modulesFile.getLabelList().getLabelNameIdent(i), modulesFile.getLabelList().getLabel(i).deepCopy());
		}
		for (int i = 0; i < propertiesFile.getLabelList().size(); i++) {
			labelList.addLabel(propertiesFile.getLabelList().getLabelNameIdent(i), propertiesFile.getLabelList().getLabel(i).deepCopy());
		}
		labelList = (LabelList) labelList.replaceConstants(constantValues);

		// Build mapping from all (original model) variables to non-clocks only
		int numVars = modulesFile.getNumVars();
		nonClockVarMap = new int[numVars];
		int count = 0;
		for (int i = 0; i < numVars; i++) {
			if (modulesFile.getVarType(i) instanceof TypeClock) {
				nonClockVarMap[i] = -1;
			} else {
				nonClockVarMap[i] = count++;
			}
		}
	}

	/**
	 * Model check a property.
	 */
	public Result check(Expression expr) throws PrismException
	{
		Modules2PTA m2pta;
		Result res;
		String resultString;
		long timer;

		// Starting model checking
		timer = System.currentTimeMillis();

		// Check for system...endsystem - not supported yet
		if (modulesFile.getSystemDefn() != null) {
			throw new PrismException("The system...endsystem construct is not supported yet (try the digital clocks engine instead)");
		}
		
		// Translate ModulesFile object into a PTA object
		mainLog.println("\nBuilding PTA...");
		m2pta = new Modules2PTA(this, modulesFile);
		pta = m2pta.translate();
		mainLog.println("\nPTA: " + pta.infoString());

		// Check for references to clocks - not allowed (yet)
		// (do this before modifications below for better error reporting)
		expr.accept(new ASTTraverseModify()
		{
			public Object visit(ExpressionVar e) throws PrismLangException
			{
				if (e.getType() instanceof TypeClock) {
					throw new PrismLangException("Properties cannot contain references to clocks (try the digital clocks engine instead)", e);
				} else {
					return e;
				}
			}
		});
		
		// Take a copy of property, since will modify
		expr = expr.deepCopy();
		// Remove property refs ands labels from property 
		expr = (Expression) expr.expandPropRefsAndLabels(propertiesFile, labelList);
		// Evaluate constants in property (easier to do now)
		expr = (Expression) expr.replaceConstants(constantValues);
		// Also simplify expression to optimise model checking
		expr = (Expression) expr.simplify();

		// Do model checking
		res = checkExpression(expr);

		// Model checking complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nModel checking completed in " + (timer / 1000.0) + " secs.");

		// Print result to log
		resultString = "Result";
		if (!("Result".equals(expr.getResultName())))
			resultString += " (" + expr.getResultName().toLowerCase() + ")";
		resultString += ": " + res;
		mainLog.print("\n" + resultString + "\n");

		// Return result
		return res;
	}

	/**
	 * Model check an expression (used recursively).
	 */
	private Result checkExpression(Expression expr) throws PrismException
	{
		Result res;

		// Current range of supported properties is quite limited...
		if (expr instanceof ExpressionProb)
			res = checkExpressionProb((ExpressionProb) expr);
		else if (expr instanceof ExpressionReward)
			res = checkExpressionReward((ExpressionReward) expr);
		else if (expr instanceof ExpressionFilter)
			throw new PrismException("PTA model checker does not handle filters since it only computes values for the initial state");
		else
			throw new PrismException("PTA model checking not yet supported for this operator (try the digital clocks engine)");

		return res;
	}

	/**
	 * Model check a P operator.
	 */
	private Result checkExpressionProb(ExpressionProb expr) throws PrismException
	{
		boolean min;
		ExpressionTemporal exprTemp;
		Expression exprTarget;
		BitSet targetLocs;
		int timeBound;
		boolean timeBoundStrict;
		double prob;

		// Check whether Pmin=? or Pmax=? (only two cases allowed)
		RelOp relOp = expr.getRelOp();
		if (expr.getProb() != null) {
			throw new PrismException("PTA model checking currently only supports Pmin=? and Pmax=? properties (try the digital clocks engine instead)");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		// Check this is a F path property (only case allowed at the moment)
		if (!(expr.getExpression() instanceof ExpressionTemporal))
			throw new PrismException("PTA model checking currently only supports the F path operator (try the digital clocks engine instead)");
		exprTemp = (ExpressionTemporal) expr.getExpression();
		if (exprTemp.getOperator() != ExpressionTemporal.P_F || !exprTemp.isSimplePathFormula())
			throw new PrismException("PTA model checking currently only supports the F path operator (try the digital clocks engine instead)");
		
		// Determine locations satisfying target
		exprTarget = exprTemp.getOperand2();
		targetLocs = checkLocationExpression(exprTarget);
		mainLog.println("Target (" + exprTarget + ") satisfied by " + targetLocs.cardinality() + " locations.");
		//mainLog.println(targetLocs);

		// If there is a time bound, add a clock and encode this into target
		if (exprTemp.hasBounds()) {
			mainLog.println("Modifying PTA to encode time bound from property...");
			// Get time bound info (is always of form <=T or <T)
			timeBound = exprTemp.getUpperBound().evaluateInt(constantValues);
			timeBoundStrict = exprTemp.upperBoundIsStrict();
			// Check for non-allowed time bounds (negative)
			if (timeBound < (timeBoundStrict ? 1 : 0)) {
				throw new PrismLangException("Negative bound in " + exprTemp);
			}
			// Modify PTA to include time bound; get new target
			targetLocs = buildTimeBoundIntoPta(pta, targetLocs, timeBound, timeBoundStrict);
			mainLog.println("New PTA: " + pta.infoString());
		}

		// Compute probability of reaching the set of target locations
		prob = computeProbabilisticReachability(targetLocs, min);

		// Return result
		return new Result(new Double(prob));
	}

	/**
	 * Build a time bounded reachability query into a PTA; return the new target location set.  
	 */
	private BitSet buildTimeBoundIntoPta(PTA pta, BitSet targetLocs, int timeBound, boolean timeBoundStrict)
	{
		String timerClock = null;
		int timerClockIndex, numLocs, newTargetLoc;
		String newTargetLocString;
		List<Transition> trNewList;
		Transition trNew;
		BitSet targetLocsNew;
		boolean toTarget;
		int i;

		// Add a timer clock
		timerClock = "time";
		while (pta.getClockIndex(timerClock) != -1)
			timerClock += "_";
		timerClockIndex = pta.addClock(timerClock);
		// Add a new target location
		numLocs = pta.getNumLocations();
		newTargetLocString = "target";
		while (pta.getLocationIndex(newTargetLocString) != -1)
			newTargetLocString += "_";
		newTargetLoc = pta.addLocation(newTargetLocString);
		// Go through old (on-target) locations
		for (i = 0; i < numLocs; i++) {
			trNewList = new ArrayList<Transition>();
			for (Transition tr : pta.getTransitions(i)) {
				// See if the transition can go to a target location 
				toTarget = false;
				for (Edge e : tr.getEdges()) {
					if (targetLocs.get(e.getDestination())) {
						toTarget = true;
						break;
					}
				}
				// Copy transition, modify edges going to target and add guard 
				if (toTarget) {
					trNew = new Transition(tr);
					for (Edge e : trNew.getEdges()) {
						if (targetLocs.get(e.getDestination())) {
							e.setDestination(newTargetLoc);
						}
					}
					if (timeBoundStrict)
						trNew.addGuardConstraint(Constraint.buildLt(timerClockIndex, timeBound));
					else
						trNew.addGuardConstraint(Constraint.buildLeq(timerClockIndex, timeBound));
					trNewList.add(trNew);
					// Modify guard of copied transition
					if (timeBoundStrict)
						tr.addGuardConstraint(Constraint.buildGeq(timerClockIndex, timeBound));
					else
						tr.addGuardConstraint(Constraint.buildGt(timerClockIndex, timeBound));
				}
			}
			// Add new transitions to PTA
			for (Transition tr : trNewList) {
				pta.addTransition(tr);
			}
		}
		// Re-generate set of target locations
		// (newly added target location, plus initial location (always 0) is this is a target)
		targetLocsNew = new BitSet(pta.getNumLocations());
		targetLocsNew.set(newTargetLoc);
		if (targetLocs.get(0)) {
			targetLocsNew.set(0);
		}

		return targetLocsNew;
	}

	/**
	 * Compute the min/max probability, from the initial state, of reaching a set of target locations.
	 */
	private double computeProbabilisticReachability(BitSet targetLocs, boolean min) throws PrismException
	{
		// Catch the special case where the initial state is already a target - just return 1.0 directly
		if (targetLocs.get(0)) {
			mainLog.println("Skipping numerical computation since initial state is a target...");
			return 1.0;
		}
		
		// Determine which method to use for computation
		String ptaMethod = settings.getString(PrismSettings.PRISM_PTA_METHOD);

		// Do probability computation through abstraction/refinement/stochastic games  
		if (ptaMethod.equals("Stochastic games")) {
			PTAAbstractRefine ptaAR;
			ptaAR = new PTAAbstractRefine(this);
			String arOptions = settings.getString(PrismSettings.PRISM_AR_OPTIONS);
			ptaAR.parseOptions(arOptions.split(","));
			return ptaAR.forwardsReachAbstractRefine(pta, targetLocs, null, min);
		}

		// Do probability computation through backwards reachability  
		else if (ptaMethod.equals("Backwards reachability")) {
			BackwardsReach ptaBw = new BackwardsReach(this);
			return ptaBw.computeProbabilisticReachability(pta, targetLocs, min);
		}

		else
			throw new PrismException("Unknown PTA solution method");
	}

	/**
	 * Model check an R operator.
	 */
	private Result checkExpressionReward(ExpressionReward expr) throws PrismException
	{
		throw new PrismException("Reward properties not yet supported for PTA model checking (try the digital clocks engine instead)");
	}

	/**
	 * Determine which locations in the PTA satisfy a (Boolean) expression.
	 * Note: This is rather inefficiently at the moment.
	 * TODO: potentially use explicit.StateMC on dummy model eventually
	 */
	private BitSet checkLocationExpression(Expression expr) throws PrismException
	{
		int i, n;
		BitSet res;

		// Labels - expand and recurse
		// (note: currently not used - these are expanded earlier)
		if (expr instanceof ExpressionLabel) {
			ExpressionLabel exprLabel = (ExpressionLabel) expr;
			if (exprLabel.isDeadlockLabel())
				throw new PrismException("The \"deadlock\" label is not yet supported for PTAs");
			if (exprLabel.isInitLabel())
				throw new PrismException("The \"init\" label is not yet supported for PTAs");
			i = labelList.getLabelIndex(exprLabel.getName());
			if (i == -1)
				throw new PrismException("Unknown label \"" + exprLabel.getName() + "\" in property");
			// Check recursively
			return checkLocationExpression(labelList.getLabel(i));

		}
		// Other expressions...
		else {
			List<Object> states;
			//Object[] state;
			states = pta.getLocationNameList();
			n = states.size();
			res = new BitSet(n);
			for (i = 0; i < n; i++) {
				//state = (Object[])states.get(i);
				State state = (State) states.get(i);
				if (expr.evaluateBoolean(state, nonClockVarMap)) {
					res.set(i);
				}
			}
		}

		return res;
	}
}
