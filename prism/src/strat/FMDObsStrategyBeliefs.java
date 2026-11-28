//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package strat;

import explicit.*;
import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import parser.State;
import prism.PrismException;
import prism.PrismLog;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class to store finite-memory deterministic (FMD) observation-based strategies
 * using a belief MDP fragment for a POMDP.
 */
public class FMDObsStrategyBeliefs<Value> extends StrategyExplicit<Value>
{
	/** Another copy of the model (to minimise casting) */
	protected POMDP<Value> pomdp;
	
	/** Finite-memory strategy represented as an MDP with 1 choice per state */
	protected MDP<Value> mdpStrat;

	/** States of the MDP: pairs of indices of POMDP observables and unobservable beliefs */
	protected List<int[]> mdpStates;

	/** List of beliefs over unobservables, as stored in MDP states */
	protected List<double[]> unobsBeliefs;

	// Memory values are indices of MDP states (i.e., an observation and a belief over unobservables),
	// plus two extra values for when the model has left the part of the belief space explored by the MDP,
	// via a state where the choice is undefined (and resolved in some other way,
	// e.g., when building the induced model). Later choices are then undefined too,
	// for the same reason: SATISFIED if the state left was a target, reasonNonTarget otherwise.

	/** Memory value: left the explored belief space via a target state */
	protected int memLeftTarget;

	/** Memory value: left the explored belief space via a non-target state */
	protected int memLeftNonTarget;

	/** Reason why the choice is undefined in (unexplored) states of the MDP that are not targets */
	protected UndefinedReason reasonNonTarget = UndefinedReason.UNKNOWN;

	/**
	 * Creates an FMDObsStrategyBeliefs.
	 */
	public FMDObsStrategyBeliefs(POMDP<Value> pomdp, MDP<Value> mdpStrat, List<int[]> mdpStates, List<double[]> unobsBeliefs)
	{
		super(pomdp);
		this.pomdp = pomdp;
		this.mdpStrat = mdpStrat;
		this.mdpStates = mdpStates;
		this.unobsBeliefs = unobsBeliefs;
		memLeftTarget = mdpStates.size();
		memLeftNonTarget = mdpStates.size() + 1;
	}

	@Override
	public Memory memory()
	{
		return Memory.FINITE;
	}
	
	@Override
	public Object getChoiceAction(int s, int m)
	{
		// Find a matching state in the MDP and look up the action for it
		int o = pomdp.getObservation(s);
		int i = findMatchingMDPState(o, m);
		return i == -1 ? Strategy.UNDEFINED : getActionPickedByMDP(i);
	}

	@Override
	public int getChoiceIndex(int s, int m)
	{
		// Find a matching state in the MDP and look up the action for it
		int o = pomdp.getObservation(s);
		int i = findMatchingMDPState(o, m);
		if (i == -1) {
			return -1;
		}
		Object act = getActionPickedByMDP(i);
		int actIndex = -1;
		if (act != UNDEFINED) {
			int[] mdpState = mdpStates.get(i);
			actIndex = pomdp.getChoiceByActionForObservation(mdpState[0], act);
		}
		return actIndex;
	}
	
	/**
	 * Set the reason why the choice is undefined in states of the belief MDP fragment
	 * that are not explored, other than target states (which are SATISFIED),
	 * i.e., those whose value is known (e.g., UNSATISFIABLE if the target can no longer be reached).
	 */
	public void setReasonNonTarget(UndefinedReason reasonNonTarget)
	{
		this.reasonNonTarget = reasonNonTarget;
	}

	@Override
	public UndefinedReason whyUndefined(int s, int m)
	{
		// Unknown memory value: no information
		if (m == -1) {
			return UndefinedReason.UNKNOWN;
		}
		// Left the explored belief space: same reason as for the state left
		if (isMemLeft(m)) {
			return whyUndefinedLeft(m);
		}
		// No matching state in the MDP: (s,m) cannot be reached
		int o = pomdp.getObservation(s);
		int i = findMatchingMDPState(o, m);
		if (i == -1) {
			return UndefinedReason.UNREACHABLE;
		}
		return getActionPickedByMDP(i) == UNDEFINED ? whyUndefinedInMDP(i) : null;
	}

	/**
	 * Get the reason why the choice is undefined in state i of the belief MDP fragment,
	 * assuming that it is: states not explored are either targets or known-value states.
	 */
	private UndefinedReason whyUndefinedInMDP(int i)
	{
		return isTargetInMDP(i) ? UndefinedReason.SATISFIED : reasonNonTarget;
	}

	/**
	 * Check whether memory value m denotes having left the explored belief space.
	 */
	private boolean isMemLeft(int m)
	{
		return m == memLeftTarget || m == memLeftNonTarget;
	}

	/**
	 * Get the reason why the choice is undefined for a memory value m
	 * denoting having left the explored belief space.
	 */
	private UndefinedReason whyUndefinedLeft(int m)
	{
		return m == memLeftTarget ? UndefinedReason.SATISFIED : reasonNonTarget;
	}

	/**
	 * Check whether state i of the belief MDP fragment is a target state.
	 */
	private boolean isTargetInMDP(int i)
	{
		BitSet target = mdpStrat.getLabelStates("target");
		return target != null && target.get(i);
	}

	@Override
	public int getMemorySize()
	{
		// MDP states, plus two values for having left the explored belief space
		return mdpStates.size() + 2;
	}
	
	@Override
	public int getInitialMemory(int sInit)
	{
		int oInit = pomdp.getObservation(sInit);
		int i = mdpStrat.getFirstInitialState();
		return mdpStates.get(i)[0] != oInit ? -1 : i;
	}
	
	@Override
	public int getUpdatedMemory(int m, Object action, int sNext)
	{
		// If the current memory is unknown, we don't know how to update it
		if (m == -1) {
			return -1;
		}
		// Once the explored belief space has been left, stay outside it
		if (isMemLeft(m)) {
			return m;
		}
		// If the choice is undefined in MDP state m (it is unexplored),
		// the action was picked in some other way, and the explored belief space is left
		Object act = getActionPickedByMDP(m);
		if (act == UNDEFINED) {
			return isTargetInMDP(m) ? memLeftTarget : memLeftNonTarget;
		}
		// Otherwise, if the action taken is the one picked in MDP state m,
		// find the successor MDP state matching the next observation
		if (!isActionChosen(act, action)) {
			return -1;
		}
		return findMatchingMemoryUpdate(m, pomdp.getObservation(sNext));
	}
	
	@Override
    public String getMemoryString(int m)
    {
		if (m == -1) {
			return "?";
		}
		if (isMemLeft(m)) {
			return "?" + whyUndefinedLeft(m).toString().toLowerCase();
		}
		return Belief.toStringUnobs(unobsBeliefs.get(mdpStates.get(m)[1]), pomdp);
    }

	@Override
	public explicit.Model<Value> constructInducedModel(StrategyExportOptions options, Consumer<InducedStateMap> stateMapConsumer) throws PrismException
	{
		return constructInducedModelWithMemory(options, stateMapConsumer);
	}

	@Override
	public void exportActions(PrismLog out, StrategyExportOptions options)
	{
		int n = mdpStrat.getNumStates();
		for (int i = 0; i < n; i++) {
			Object act = getActionPickedByMDP(i);
			if (act != UNDEFINED) {
				int[] mdpState = mdpStates.get(i);
				if (options.getShowStates()) {
					out.print(Belief.toString(mdpState[0], unobsBeliefs.get(mdpState[1]), pomdp));
				} else {
					out.print(mdpState[0] + "," + mdpState[1]);
				}
				out.println("=" + (act == null ? "" : act));
			} else if (options.getShowUndefined()) {
				int[] mdpState = mdpStates.get(i);
				if (options.getShowStates()) {
					out.print(Belief.toString(mdpState[0], unobsBeliefs.get(mdpState[1]), pomdp));
				} else {
					out.print(mdpState[0] + "," + mdpState[1]);
				}
				out.println("=?" + whyUndefinedInMDP(i).toString().toLowerCase());
			}
		}
	}

	@Override
	public void exportIndices(PrismLog out, StrategyExportOptions options)
	{
		int n = mdpStrat.getNumStates();
		for (int i = 0; i < n; i++) {
			Object act = getActionPickedByMDP(i);
			if (act != UNDEFINED) {
				int[] mdpState = mdpStates.get(i);
				int actIndex = pomdp.getChoiceByActionForObservation(mdpState[0], act);
				out.println(mdpState[0] + "," + mdpState[1] + "=" + actIndex);
			} else if (options.getShowUndefined()) {
				int[] mdpState = mdpStates.get(i);
				out.println(mdpState[0] + "," + mdpState[1] + "=?" + whyUndefinedInMDP(i).toString().toLowerCase());
			}
		}
	}

	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		if (options.getMergeObservations()) {
			mdpStrat.export(out, options.getInducedModelExportOptions());
		} else {
			super.exportInducedModel(out, options);
		}
	}

	@Override
	public void exportDotFile(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		if (options.getMergeObservations()) {
			exportDotFileObs(out, options);
		} else {
			exportDotFileNonObs(out, options);
		}
	}

	public void exportDotFileObs(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		// Use the already constructed strategy model for export
		List<Decorator> decorators = new ArrayList<>();
		if (options.getShowStates()) {
			decorators.add(new Decorator()
			{
				@Override
				public Decoration decorateState(int state, Decoration d)
				{
					d.labelAddBelow(Belief.toString(mdpStates.get(state)[0], unobsBeliefs.get(mdpStates.get(state)[1]), pomdp));
					return d;
				}
			});
		}
		mdpStrat.exportToDotFile(out, decorators, options.getModelPrecision());
	}

	public void exportDotFileNonObs(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		// Construct the strategy-induced product model
		Model<Value> prodModel = constructInducedModel(options);
		List<State> stateList = prodModel.getStatesList();
		// Export product model to dot file, with (if needed) a custom decorator
		// to extract the memory value of each state (the first variable) and convert to a belief
		List<Decorator> decorators = new ArrayList<>();
		if (options.getShowStates()) {
			decorators.add(new Decorator()
			{
				@Override
				public Decoration decorateState(int state, Decoration d)
				{
					Object[] varValues = stateList.get(state).varValues;
					int numVars = varValues.length;
					String s = "(";
					for (int i = 1; i < numVars; i++) {
						if (i > 1)
							s += ",";
						s += State.valueToString(varValues[i]);
					}
					s += "),";
					int m = (int) varValues[0];
					s += getMemoryString(m);
					d.labelAddBelow(s);
					return d;
				}
			});
		}
		prodModel.exportToDotFile(out, decorators, options.getModelPrecision());
	}

	@Override
	public void clear()
	{
		mdpStrat = null;
		mdpStates = null;
		unobsBeliefs = null;
	}

	// Utility methods

	/**
	 * Get the index of the state of the MDP for observation o and memory value m,
	 * i.e., m itself, if it is an MDP state with observation o; otherwise -1
	 */
	private int findMatchingMDPState(int o, int m)
	{
		if (m < 0 || m >= mdpStates.size() || mdpStates.get(m)[0] != o) {
			return -1;
		}
		return m;
	}

	/**
	 * Find a transition in the i-th state of the MDP whose destination state
	 * has observable oNext, if there is one, and return its index (the new memory value)
	 */
	private int findMatchingMemoryUpdate(int i, int oNext)
	{
		for (SuccessorsIterator succ = mdpStrat.getSuccessors(i); succ.hasNext();) {
			int j = succ.nextInt();
			if (mdpStates.get(j)[0] == oNext) {
				return j;
			}
		}
		// No match
		return -1;
	}

	/**
	 * Get the action picked by a state of the MDP. Returns {@link StrategyInfo#UNDEFINED} if undefined.
	 * @param i MDP state index
 	 */
	private Object getActionPickedByMDP(int i)
	{
		if (i < 0 || i >= mdpStrat.getNumStates() || mdpStrat.isDeadlockState(i)) {
			return UNDEFINED;
		}
		return mdpStrat.getAction(i, 0);
	}

	@Override
	public String toString()
	{
		return "[" + IntStream.range(0, mdpStrat.getNumStates())
				.mapToObj(s -> mdpStates.get(s)[0] + "," + mdpStates.get(s)[1]
					+ "=" + getActionPickedByMDP(s))
				.collect(Collectors.joining(",")) + "]";
	}
}
