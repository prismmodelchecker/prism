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
import java.util.Collections;
import java.util.List;
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
	
	@Override
	public int getMemorySize()
	{
		return unobsBeliefs.size();
	}
	
	@Override
	public int getInitialMemory(int sInit)
	{
		int oInit = pomdp.getObservation(sInit);
		int i = mdpStrat.getFirstInitialState();
		if (mdpStates.get(i)[0] != oInit) {
			return -1;
		} else {
			return mdpStates.get(i)[1];
		}
	}
	
	@Override
	public int getUpdatedMemory(int m, Object action, int sNext)
	{
		// If the current memory is unknown, we don't know how to update it
		if (m == -1) {
			return -1;
		}
		// Look for a matching transition in the MDP and find the memory update
		// (inefficiently, since they are not sorted)
		int oNext = pomdp.getObservation(sNext);
		int n = mdpStates.size();
		for (int i = 0; i < n; i++) {
			if (mdpStates.get(i)[1] == m) {
				int j = findMatchingMemoryUpdate(i, oNext);
				if (j != -1) {
					return j;
				}
			}
		}
		return -1;
	}
	
	@Override
    public String getMemoryString(int m)
    {
		return m == -1 ? "?" : Belief.toStringUnobs(unobsBeliefs.get(m), pomdp);
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
			}
		}
	}

	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		if (options.getMergeObservations()) {
			exportInducedModelObs(out, options);
		} else {
			exportInducedModelNonObs(out, options);
		}
	}

	public void exportInducedModelObs(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		mdpStrat.exportToPrismExplicitTra(out, options.getModelPrecision());
	}

	public void exportInducedModelNonObs(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		ConstructStrategyProduct csp = new ConstructStrategyProduct();
		csp.setMode(options.getMode());
		Model<Value> prodModel = csp.constructProductModel(model, this);
		prodModel.exportToPrismExplicitTra(out, options.getModelPrecision());
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
		ConstructStrategyProduct csp = new ConstructStrategyProduct();
		csp.setMode(options.getMode());
		Model<Value> prodModel = csp.constructProductModel(model, this);
		List<State> stateList = prodModel.getStatesList();
		// Export product model to dot file, with (if needed) a custom decorator
		// to extract the final value of each state and convert to a belief
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
					for (int i = 0; i < numVars - 1; i++) {
						if (i > 0)
							s += ",";
						s += State.valueToString(varValues[i]);
					}
					s += "),";
					int m = (int) varValues[numVars - 1];
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
	 * Find the index of a state (o,m) in the MDP, if there is one
	 */
	private int findMatchingMDPState(int o, int m)
	{
		// If memory value is unknown, there are no matching states
		if (m == -1) {
			return -1;
		}
		// Look for a matching state in the MDP
		// (inefficiently, since they are not sorted)
		int n = mdpStates.size();
		for (int i = 0; i < n; i++) {
			if (mdpStates.get(i)[0] == o && mdpStates.get(i)[1] == m) {
				return i;
			}
		}
		// No match
		return -1;
	}

	/**
	 * Find a transition in the i-th state of the MDP whose destination state
	 * has observable oNext, if there is one, and return the automaton state
	 */
	private int findMatchingMemoryUpdate(int i, int oNext)
	{
		for (SuccessorsIterator succ = mdpStrat.getSuccessors(i); succ.hasNext();) {
			int j = succ.nextInt();
			if (mdpStates.get(j)[0] == oNext) {
				return mdpStates.get(j)[1];
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
