//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
//	* Alberto Puggelli <alberto.puggelli@gmail.com>
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

import common.Interval;
import parser.State;
import prism.Evaluator;
import prism.Pair;
import prism.PrismException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple explicit-state representation of an IDTMC.
 */
public class IDTMCSimple<Value> extends ModelExplicitWrapper<Value> implements ModelSimple<Value>, IntervalModelExplicit<Value>, IDTMC<Value>
{
	/**
	 * The IDTMC, stored as a DTMCSimple over Intervals.
	 * Also stored in {@link ModelExplicitWrapper#model} as a ModelExplicit.
	 */
	protected DTMCSimple<Interval<Value>> dtmc;

	// Constructors

	/**
	 * Constructor: empty IDTMC.
	 */
	@SuppressWarnings("unchecked")
	public IDTMCSimple()
	{
		this.dtmc = new DTMCSimple<>();
		this.model = (ModelExplicit<Value>) dtmc;
		createDefaultEvaluatorForDTMC();
	}

	/**
	 * Constructor: new IDTMC with fixed number of states.
	 */
	@SuppressWarnings("unchecked")
	public IDTMCSimple(int numStates)
	{
		this.dtmc = new DTMCSimple<>(numStates);
		this.model = (ModelExplicit<Value>) dtmc;
		createDefaultEvaluatorForDTMC();
	}

	/**
	 * Copy constructor.
	 */
	@SuppressWarnings("unchecked")
	public IDTMCSimple(IDTMCSimple<Value> idtmc)
	{
		this.dtmc = new DTMCSimple<>(idtmc.dtmc);
		this.model = (ModelExplicit<Value>) dtmc;
		createDefaultEvaluatorForDTMC();
	}

	/**
	 * Construct an IDTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Pointer to states list is NOT copied (since now wrong).
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	@SuppressWarnings("unchecked")
	public IDTMCSimple(IDTMCSimple<Value> idtmc, int permut[])
	{
		this.dtmc = new DTMCSimple<>(idtmc.dtmc, permut);
		this.model = (ModelExplicit<Value>) dtmc;
		createDefaultEvaluatorForDTMC();
	}

	/**
	 * Add a default (double interval) evaluator to the DTMC
	 */
	@SuppressWarnings("unchecked")
	private void createDefaultEvaluatorForDTMC()
	{
		((IDTMCSimple<Double>) this).setIntervalEvaluator(Evaluator.forDoubleInterval());
	}

	// Mutators (for ModelSimple)

	@Override
	public void clearState(int s)
	{
		dtmc.clearState(s);
	}

	@Override
	public int addState()
	{
		return dtmc.addState();
	}

	@Override
	public void addStates(int numToAdd)
	{
		dtmc.addStates(numToAdd);
	}

	// Mutators (for IntervalModelExplicit)

	@Override
	public void setIntervalEvaluator(Evaluator<Interval<Value>> eval)
	{
		dtmc.setEvaluator(eval);
	}

	// Mutators (other)

	/**
	 * Set the probability for a transition.
	 */
	public void setProbability(int i, int j, Interval<Value> prob)
	{
		dtmc.setProbability(i, j, prob, null);
	}

	/**
	 * Set the probability for a transition.
	 */
	public void setProbability(int i, int j, Interval<Value> prob, Object action)
	{
		dtmc.setProbability(i, j, prob, action);
	}

	/**
	 * Add to the probability for a transition.
	 */
	public void addToProbability(int i, int j, Interval<Value> prob)
	{
		dtmc.addToProbability(i, j, prob, null);
	}

	/**
	 * Add to the probability for a transition.
	 */
	public void addToProbability(int i, int j, Interval<Value> prob, Object action)
	{
		dtmc.addToProbability(i, j, prob, action);
	}

	/**
	 * Delimit the intervals for probabilities from state s,
	 * i.e., trim the bounds of the intervals such that at least one
	 * possible distribution takes each of the extremal values.
	 * @param s The index of the state to delimit
	 */
	public void delimit(int s) throws PrismException
	{
		IntervalUtils.delimit(dtmc.trans.get(s), getEvaluator());
	}

	// Accessors (for UDTMC)

	@Override
	public void checkLowerBoundsArePositive() throws PrismException
	{
		Evaluator<Interval<Value>> eval = dtmc.getEvaluator();
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			Iterator<Map.Entry<Integer, Interval<Value>>> iter = getIntervalTransitionsIterator(s);
			while (iter.hasNext()) {
				Map.Entry<Integer, Interval<Value>> e = iter.next();
				// NB: we phrase the check as an operation on intervals, rather than
				// accessing the lower bound directly, to make use of the evaluator
				if (!eval.gt(e.getValue(), eval.zero())) {
					List<State> sl = getStatesList();
					String state = sl == null ? "" + s : sl.get(s).toString();
					throw new PrismException("Transition probability has lower bound of 0 in state " + state);
				}
			}
		}
	}

	@Override
	public double mvMultUncSingle(int s, double vect[], MinMax minMax)
	{
		@SuppressWarnings("unchecked")
		DoubleIntervalDistribution did = IntervalUtils.extractDoubleIntervalDistribution(((IDTMC<Double>) this).getIntervalTransitionsIterator(s), getNumTransitions(s));
		return IDTMC.mvMultUncSingle(did, vect, minMax);
	}

	// Accessors (for IntervalModel)

	@Override
	public Evaluator<Interval<Value>> getIntervalEvaluator()
	{
		return dtmc.getEvaluator();
	}

	@Override
	public DTMC<Interval<Value>> getIntervalModel()
	{
		return dtmc;
	}
	
	// Accessors (for IDTMC)

	@Override
	public Iterator<Map.Entry<Integer, Interval<Value>>> getIntervalTransitionsIterator(int s)
	{
		return dtmc.getTransitionsIterator(s);
	}

	@Override
	public Iterator<Map.Entry<Integer, Pair<Interval<Value>, Object>>> getIntervalTransitionsAndActionsIterator(int s)
	{
		return dtmc.getTransitionsAndActionsIterator(s);
	}
}
