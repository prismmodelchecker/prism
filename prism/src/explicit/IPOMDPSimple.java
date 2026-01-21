//==============================================================================
//
//	Copyright (c) 2025-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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
import io.ModelExportOptions;
import io.PrismExplicitExporter;
import parser.State;
import prism.Evaluator;
import prism.ModelInfo;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple explicit-state representation of an IPOMDP.
 */
public class IPOMDPSimple<Value> extends ModelExplicitWrapper<Value> implements NondetModelSimple<Value>, IntervalModelExplicit<Value>, IPOMDP<Value>
{
    /**
     * The IPOMDP, stored as a POMDPSimple over Intervals.
     * Also stored in {@link ModelExplicitWrapper#model} as a ModelExplicit.
     */
    protected POMDPSimple<Interval<Value>> pomdp;

    // Constructors

    /**
     * Constructor: empty IPOMDP.
     */
    @SuppressWarnings("unchecked")
    public IPOMDPSimple()
    {
        this.pomdp = new POMDPSimple<>();
        this.model = (ModelExplicit<Value>) pomdp;
        createDefaultEvaluatorForPOMDP();
    }

    /**
     * Constructor: new IPOMDP with fixed number of states.
     */
    @SuppressWarnings("unchecked")
    public IPOMDPSimple(int numStates)
    {
        this.pomdp = new POMDPSimple<>(numStates);
        this.model = (ModelExplicit<Value>) pomdp;
        createDefaultEvaluatorForPOMDP();
    }

    /**
     * Copy constructor.
     */
    @SuppressWarnings("unchecked")
    public IPOMDPSimple(IPOMDPSimple<Value> ipomdp)
    {
        setEvaluator(ipomdp.getEvaluator());
        this.pomdp = new POMDPSimple<>(ipomdp.pomdp);
        this.model = (ModelExplicit<Value>) pomdp;
    }

    /**
     * Construct an IPOMDP from an existing one and a state index permutation,
     * i.e. in which state index i becomes index permut[i].
     * Pointer to states list is NOT copied (since now wrong).
     * Note: have to build new Distributions from scratch anyway to do this,
     * so may as well provide this functionality as a constructor.
     */
    @SuppressWarnings("unchecked")
    public IPOMDPSimple(IPOMDPSimple<Value> ipomdp, int permut[])
    {
        setEvaluator(ipomdp.getEvaluator());
        this.pomdp = new POMDPSimple<>(ipomdp.pomdp, permut);
        this.model = (ModelExplicit<Value>) pomdp;
    }

    /**
     * Add a default (double interval) evaluator to the POMDP
     */
    @SuppressWarnings("unchecked")
    private void createDefaultEvaluatorForPOMDP()
    {
        ((IPOMDPSimple<Double>) this).setIntervalEvaluator(Evaluator.forDoubleInterval());
    }

    // Mutators (for ModelExplicit)

    @Override
    public void setActions(List<Object> actions)
    {
        pomdp.setActions(actions);
    }

    // Mutators (for ModelSimple)

    @Override
    public void clearState(int s)
    {
        pomdp.clearState(s);
    }

    @Override
    public int addState()
    {
        return pomdp.addState();
    }

    @Override
    public void addStates(int numToAdd)
    {
        pomdp.addStates(numToAdd);
    }

    // Mutators (for IntervalModelExplicit)

    @Override
    public void setIntervalEvaluator(Evaluator<Interval<Value>> eval)
    {
        pomdp.setEvaluator(eval);
    }

    // Mutators (for PartiallyObservableModel)

    @Override
    public void setObservationsList(List<State> observationsList)
    {
        pomdp.setObservationsList(observationsList);
    }

    @Override
    public void setUnobservationsList(List<State> unobservationsList)
    {
        pomdp.setUnobservationsList(unobservationsList);
    }

    @Override
    public void setObservation(int s, State observ, State unobserv, List<String> observableNames) throws PrismException
    {
        pomdp.setObservation(s, observ, unobserv, observableNames);
    }

    @Override
    public void setObservation(int s, int o) throws PrismException
    {
        pomdp.setObservation(s, o);
    }

    // Mutators (other)

    /**
     * Add a choice (uncertain distribution {@code udistr}) to state {@code s} (which must exist).
     * Returns the index of the (newly added) distribution.
     * Returns -1 in case of error.
     */
    public int addChoice(int s, Distribution<Interval<Value>> udistr)
    {
        return pomdp.addChoice(s, udistr);
    }

    /**
     * Add a choice (uncertain distribution {@code udistr}) labelled with {@code action} to state {@code s} (which must exist).
     * Returns the index of the (newly added) distribution.
     * Returns -1 in case of error.
     */
    public int addActionLabelledChoice(int s, Distribution<Interval<Value>> udistr, Object action)
    {
        return pomdp.addActionLabelledChoice(s, udistr, action);
    }

    /**
     * Set the action label for choice i in some state s.
     */
    public void setAction(int s, int i, Object action)
    {
        pomdp.setAction(s, i, action);
    }

    /**
     * Delimit the intervals for probabilities for the ith choice (distribution) for state s.
     * i.e., trim the bounds of the intervals such that at least one
     * possible distribution takes each of the extremal values.
     * @param s The index of the state to delimit
     * @param i The index of the choice to delimit
     */
    public void delimit(int s, int i)
    {
        IntervalUtils.delimit(pomdp.trans.get(s).get(i), getEvaluator());
    }

    // Accessors (for PartiallyObservableModel)

    @Override
    public List<State> getObservationsList()
    {
        return pomdp.getObservationsList();
    }

    @Override
    public List<State> getUnobservationsList()
    {
        return pomdp.getUnobservationsList();
    }

    @Override
    public int getNumObservations()
    {
        return pomdp.getNumObservations();
    }

    @Override
    public int getNumUnobservations()
    {
        return pomdp.getNumUnobservations();
    }

    @Override
    public int getObservation(int s)
    {
        return pomdp.getObservation(s);
    }

    @Override
    public State getObservationAsState(int s)
    {
        return pomdp.getObservationAsState(s);
    }

    @Override
    public int getUnobservation(int s)
    {
        return pomdp.getUnobservation(s);
    }

    @Override
    public State getUnobservationAsState(int s)
    {
        return pomdp.getUnobservationAsState(s);
    }

    @Override
    public double getObservationProb(int s, int o)
    {
        return pomdp.getObservationProb(s, o);
    }

    @Override
    public int getNumChoicesForObservation(int o)
    {
        return pomdp.getNumChoicesForObservation(o);
    }

    @Override
    public void exportObservations(ModelInfo modelInfo, PrismLog out, ModelExportOptions exportOptions) throws PrismException
    {
        pomdp.exportObservations(modelInfo, out, exportOptions);
    }

    // Accessors (for NondetModel)

    @Override
    public int getNumChoices(int s)
    {
        return pomdp.getNumChoices(s);
    }

    @Override
    public Object getAction(int s, int i)
    {
        return pomdp.getAction(s, i);
    }

    @Override
    public boolean allSuccessorsInSet(int s, int i, BitSet set)
    {
        return pomdp.allSuccessorsInSet(s, i, set);
    }

    @Override
    public boolean someSuccessorsInSet(int s, int i, BitSet set)
    {
        return pomdp.someSuccessorsInSet(s, i, set);
    }

    @Override
    public Iterator<Integer> getSuccessorsIterator(final int s, final int i)
    {
        return pomdp.getSuccessorsIterator(s, i);
    }

    @Override
    public SuccessorsIterator getSuccessors(final int s, final int i)
    {
        return pomdp.getSuccessors(s, i);
    }

    @Override
    public int getNumTransitions(int s, int i)
    {
        return pomdp.getNumTransitions(s, i);
    }

    @Override
    public Model<Value> constructInducedModel(MDStrategy<Value> strat)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Accessors (for UMDP)

    @Override
    public void checkLowerBoundsArePositive() throws PrismException
    {
        Evaluator<Interval<Value>> eval = pomdp.getEvaluator();
        int numStates = getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = getNumChoices(s);
            for (int j = 0; j < numChoices; j++) {
                Iterator<Map.Entry<Integer, Interval<Value>>> iter = getIntervalTransitionsIterator(s, j);
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
    }

    // Accessors (for IntervalModel)

    @Override
    public Evaluator<Interval<Value>> getIntervalEvaluator()
    {
        return pomdp.getEvaluator();
    }

    @Override
    public POMDP<Interval<Value>> getIntervalModel()
    {
        return pomdp;
    }

    // Accessors (for IMDP)

    @Override
    public Iterator<Map.Entry<Integer, Interval<Value>>> getIntervalTransitionsIterator(int s, int i)
    {
        return pomdp.getTransitionsIterator(s, i);
    }
}
