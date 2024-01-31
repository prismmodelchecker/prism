//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import common.IterableBitSet;
import common.IterableStateSet;
import explicit.CTMC;
import explicit.DTMC;
import explicit.DTMCFromMDPMemorylessAdversary;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.Model;
import explicit.rewards.MCRewards;
import explicit.rewards.MCRewardsFromMDPRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.MDPRewardsSimple;
import explicit.rewards.Rewards;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Computes values for properties of a parametric Markov model. 
 */
final class ValueComputer extends PrismComponent
{
	private enum PropType {
		REACH,
		STEADY
	};

	class SchedulerCacheKey
	{		
		final private PropType propType;
		final private BitSet b1;
		final private BitSet b2;
		final private boolean min;
		final private Rewards<?> rew;
		
		SchedulerCacheKey(PropType propType, StateValues b1, StateValues b2, boolean min, Rewards<?> rew, Region region)
		{
			this.propType = propType;
			this.b1 = b1.toBitSet();
			if (b2 == null) {
				this.b2 = null;
			} else {
				this.b2 = b2.toBitSet();
			}
			this.min = min;
			this.rew = rew;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SchedulerCacheKey)) {
				return false;
			}
			SchedulerCacheKey other = (SchedulerCacheKey) obj;
			if (!this.propType.equals(other.propType)) {
				return false;
			}
			if (!this.b1.equals(other.b1)) {
				return false;
			}
			if ((this.b2 == null) != (other.b2 == null)) {
				return false;
			}
			if (this.b2 != null && !this.b2.equals(other.b2)) {
				return false;
			}
			if ((this.rew == null) != (other.rew == null)) {
				return false;
			}
			if ((rew != null) && !this.rew.equals(other.rew)) {
				return false;
			}
			if (this.min != other.min){
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			int hash = 0;
			
			switch (propType) {
			case REACH:
				hash = 13;
				break;
			case STEADY:
				hash = 17;
				break;
			}
			hash = ((b1 == null) ? 0 : b1.hashCode()) + (hash << 6) + (hash << 16) - hash;
			if (this.b2 != null) {
				hash = ((b2 == null) ? 0 : b2.hashCode()) + (hash << 6) + (hash << 16) - hash;
			}
			hash = ((min) ? 13 : 17) + (hash << 6) + (hash << 16) - hash;
			hash = ((rew == null) ? 0 : rew.hashCode()) + (hash << 6) + (hash << 16) - hash;
			
			return hash;
		}
	}

	class ResultCacheKey
	{
		final private PropType propType;
		final private BitSet b1;
		final private BitSet b2;
		final private Rewards<?> rew;
		final private Scheduler sched;
		final private boolean min;
		
		ResultCacheKey(PropType propType, StateValues b1, StateValues b2, Rewards<?> rew, Scheduler sched, boolean min)
		{
			this.propType = propType;
			this.b1 = b1.toBitSet();
			if (b2 == null) {
				this.b2 = null;
			} else {
				this.b2 = b2.toBitSet();
			}
			this.rew = rew;
			this.sched = sched;
			this.min = min;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ResultCacheKey)) {
				return false;
			}
			ResultCacheKey other = (ResultCacheKey) obj;
			if (!this.propType.equals(other.propType)) {
				return false;
			}
			if (!this.b1.equals(other.b1)) {
				return false;
			}
			if ((this.b2 == null) != (other.b2 == null)) {
				return false;
			}
			if (this.b2 != null && !this.b2.equals(other.b2)) {
				return false;
			}
			if ((this.rew == null) != (other.rew == null)) {
				return false;
			}
			if ((rew != null) && !this.rew.equals(other.rew)) {
				return false;
			}
			if ((this.sched == null) != (other.sched == null)) {
				return false;
			}
			if (!this.sched.equals(other.sched)) {
				return false;
			}
			if (this.min != other.min){
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			int hash = 0;
			
			switch (propType) {
			case REACH:
				hash = 13;
				break;
			case STEADY:
				hash = 17;
				break;
			}
			hash = b1.hashCode() + (hash << 6) + (hash << 16) - hash;
			if (this.b2 != null) {
				hash = b2.hashCode() + (hash << 6) + (hash << 16) - hash;
			}
			hash = (rew == null ? 0 : rew.hashCode()) + (hash << 6) + (hash << 16) - hash;
			hash = (sched == null ? 0 : sched.hashCode()) + (hash << 6) + (hash << 16) - hash;
			hash = (min ? 13 : 17) + (hash << 6) + (hash << 16) - hash;
			
			return hash;
		}
	}
	
	class ResultCacheEntry
	{
		final private StateValues values;
		final private Function[] compare;
		
		ResultCacheEntry(StateValues values, Function[] compare)
		{
			this.values = values;
			this.compare = compare;
		}
		
		StateValues getValues()
		{
			return values;
		}
		
		Function[] getCompare()
		{
			return compare;
		}
	}

	private ParamMode mode;
	private RegionFactory regionFactory;
	private FunctionFactory functionFactory;
	private ConstraintChecker constraintChecker;
	private BigRational precision;
	private HashMap<SchedulerCacheKey,ArrayList<Scheduler>> schedCache;
	private HashMap<ResultCacheKey,ResultCacheEntry> resultCache;
	private StateEliminator.EliminationOrder eliminationOrder;
	private Lumper.BisimType bisimType;

	ValueComputer(PrismComponent parent, ParamMode mode, RegionFactory regionFactory, BigRational precision, StateEliminator.EliminationOrder eliminationOrder, Lumper.BisimType bisimType) {
		super(parent);
		this.mode = mode;
		this.regionFactory = regionFactory;
		this.functionFactory = regionFactory.getFunctionFactory();
		this.constraintChecker = regionFactory.getConstraintChecker();
		this.precision = precision;
		this.schedCache = new HashMap<SchedulerCacheKey,ArrayList<Scheduler>>();
		this.resultCache = new HashMap<ResultCacheKey,ResultCacheEntry>();
		this.eliminationOrder = eliminationOrder;
		this.bisimType = bisimType;
	}

	RegionValues computeUnbounded(Model<?> model, RegionValues b1, RegionValues b2, boolean min, Rewards<?> rew) throws PrismException {
		RegionValues result = new RegionValues(regionFactory);
		RegionValuesIntersections co = new RegionValuesIntersections(b1, b2);
		for (RegionIntersection inter : co) {
			Region region = inter.getRegion();
			StateValues value1 = inter.getStateValues1();
			StateValues value2 = inter.getStateValues2();
			RegionValues val = computeUnbounded(model, region, value1, value2, min, rew);
			result.addAll(val);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private RegionValues computeUnbounded(Model<?> model, Region region, StateValues b1, StateValues b2, boolean min, Rewards<?> rew) throws PrismException
	{
		switch (model.getModelType()) {
		case CTMC:
		case DTMC:
			return computeUnboundedMC((DTMC<Function>) model, region, b1, b2, (MCRewards<Function>) rew);
		case MDP:
			return computeUnboundedMDP((MDP<Function>) model, region, b1, b2, min, (MDPRewards<Function>) rew);
		default:
			throw new PrismNotSupportedException("Parametric unbounded reachability computation not supported for " + model.getModelType());
		}
	}

	private RegionValues computeUnboundedMC(DTMC<Function> dtmc, Region region, StateValues b1, StateValues b2, MCRewards<Function> mcRewards) throws PrismException
	{
		if (mcRewards != null) {
			// determine infinity states
			explicit.DTMCModelChecker mcExplicit = new explicit.DTMCModelChecker(this);
			mcExplicit.setSilentPrecomputations(true);
			BitSet inf = mcExplicit.prob1(dtmc, b1.toBitSet(), b2.toBitSet());
			inf.flip(0, dtmc.getNumStates());

			for (int i : new IterableStateSet(inf, dtmc.getNumStates())) {
				// clear states with infinite value from b1 so they will get Infinity value
				// in the DTMC
				b1.setStateValue(i, false);
			}
		}

		MutablePMC pmc = buildAlterablePMCForReach(dtmc, b1, b2, mcRewards);
		// TODO
//		if (rew != null && mode == ParamMode.EXACT) {
//			rew.checkForNonNormalRewards();
//		}

		StateValues values = computeValues(pmc, dtmc.getFirstInitialState());
		return regionFactory.completeCover(values);
	}

	private RegionValues computeUnboundedMDP(MDP<Function> mdp, Region region, StateValues b1, StateValues b2, boolean min, MDPRewards<Function> mdpRewards) throws PrismException
	{
		BigRational precisionForThisRegion = region.volume().multiply(precision);
		BigRational requiredVolume = region.volume().subtract(precisionForThisRegion);
		RegionValues result = new RegionValues(regionFactory);
		RegionsTODO todo = new RegionsTODO();
		todo.add(region);
		BigRational volume = BigRational.ZERO;

		if (mdpRewards != null) {
			// determine infinity states
			explicit.MDPModelChecker mcExplicit = new explicit.MDPModelChecker(this);
			mcExplicit.setSilentPrecomputations(true);
			BitSet inf = mcExplicit.prob1(mdp, b1.toBitSet(), b2.toBitSet(), !min, null);
			inf.flip(0, mdp.getNumStates());

			for (int i : new IterableStateSet(inf, mdp.getNumStates())) {
				// clear states with infinite value from b1 so they will get Infinity value
				// in the DTMC
				b1.setStateValue(i, false);
			}
		}

		Scheduler initialScheduler = new Scheduler(mdp);
		precomputeScheduler(mdp, initialScheduler, b1, b2, mdpRewards, min);

		while (volume.compareTo(requiredVolume) == -1) {
			Region currentRegion = todo.poll();
			Point midPoint = ((BoxRegion)currentRegion).getMidPoint();
			Scheduler scheduler = computeOptConcreteReachScheduler(midPoint, mdp, b1, b2, min, mdpRewards, initialScheduler);
			if (scheduler == null) {
				// midpoint leads to non-well-defined model
				if (currentRegion.volume().compareTo(precisionForThisRegion) <= 0) {
					// region is below precision threshold, treat as undefined
					// and adjust required volume
					requiredVolume = requiredVolume.subtract(currentRegion.volume());
				} else {
					// we split the current region
					// TODO: Would be nice to try and analyse the well-definedness constraints
					todo.addAll(currentRegion.split());
				}
				continue;
			}

			ResultCacheEntry resultCacheEntry = lookupValues(PropType.REACH, b1, b2, mdpRewards, scheduler, min);
			Function[] compare;
			StateValues values;
			if (resultCacheEntry == null) {
				DTMC<Function> dtmc = new DTMCFromMDPMemorylessAdversary<>(mdp, scheduler.choices);
				MCRewards<Function> mcRewards = mdpRewards == null ? null : new MCRewardsFromMDPRewards<>(mdpRewards, scheduler.choices);
				MutablePMC pmc = buildAlterablePMCForReach(dtmc, b1, b2, mcRewards);
				values = computeValues(pmc, mdp.getFirstInitialState());
				compare = computeCompare(mdp, b1, b2, mdpRewards, scheduler, min, values);
				storeValues(PropType.REACH, b1, b2, mdpRewards, scheduler, min, values, compare);
			} else {
				values = resultCacheEntry.getValues();
				compare = resultCacheEntry.getCompare();
			}
			boolean ok = true;
			Function choiceValue = null;
			for (Function entry : compare) {
				choiceValue = entry;
				if (!constraintChecker.check(currentRegion, entry, false)) {
					ok = false;
				}
			}
			if (ok) {
				volume = volume.add(currentRegion.volume());
				result.add(currentRegion, values);
			} else {
				todo.addAll(currentRegion.split(choiceValue));
			}
		}

		return result;
	}
	
	private Function[] computeCompare(MDP<Function> model, StateValues b1, StateValues b2,
			MDPRewards<Function> mdpRewards, Scheduler scheduler, boolean min,
			StateValues values) {
		HashSet<Function> allValues = new HashSet<Function>();
		
		for (int state = 0; state < model.getNumStates(); state++) {
			if (!b1.getStateValueAsBoolean(state) || b2.getStateValueAsBoolean(state)) {
				continue;
			}
			Function stateValue = values.getStateValueAsFunction(state);
			for (int altChoice = 0; altChoice < model.getNumChoices(state); altChoice++) {
				Function choiceValue = functionFactory.getZero();
				if (mdpRewards != null) {
					choiceValue = choiceValue.add(mdpRewards.getStateReward(state));
					choiceValue = choiceValue.add(mdpRewards.getTransitionReward(state, altChoice));
				}
				Iterator<Entry<Integer, Function>> iter = model.getTransitionsIterator(state, altChoice);
				while (iter.hasNext()) {
					Entry<Integer, Function> e = iter.next();
					int succState = e.getKey();
					Function weighted = e.getValue().multiply(values.getStateValueAsFunction(succState));
					choiceValue = choiceValue.add(weighted);
				}
				choiceValue = min ? choiceValue.subtract(stateValue) : stateValue.subtract(choiceValue);
				allValues.add(choiceValue);
			}
		}

		return allValues.toArray(new Function[0]);
	}

	private void storeValues(PropType propType, StateValues b1, StateValues b2,
			Rewards<?> rew, Scheduler scheduler, boolean min, StateValues values, Function[] compare) {
		ResultCacheKey cacheKey = new ResultCacheKey(propType, b1, b2, rew, scheduler, min);
		ResultCacheEntry resultCacheEntry = new ResultCacheEntry(values, compare);
		resultCache.put(cacheKey, resultCacheEntry);
	}

	private ResultCacheEntry lookupValues(PropType propType, StateValues b1, StateValues b2,
			Rewards<?> rew, Scheduler scheduler, boolean min) {
		ResultCacheKey cacheKey = new ResultCacheKey(propType, b1, b2, rew, scheduler, min);
		ResultCacheEntry resultCacheEntry = resultCache.get(cacheKey);
		return resultCacheEntry;
	}

	/**
	 * Compute an optimal scheduler for Pmin/Pmax[ b1 U b2 ] or Rmin/Rmax[ b1 U b2 ]
	 * at the given parameter instantiation (point).
	 * <br>
	 * In parametric mode, returns {@code null} if the given point leads to a model
	 * that is not well-formed, i.e., where transition probabilities are not actually
	 * probabilities or graph-preserving, or the rewards are negative (not supported for MDPs).
	 * <br>
	 * In exact mode, throws an exception if there are negative rewards (not supported for MDPs).
	 * <br>
	 * This method expects an initial scheduler that ensures that policy iteration
	 * will converge.
	 *
	 * @param point The point (parameter valuation) where the model should be instantiated
	 * @param mdp the model
	 * @param b1 the set of 'safe' states
	 * @param b2 the set of 'target' states
	 * @param min compute min or max? true = min
	 * @param mdpRewards if non-null, compute reachability reward
	 * @param initialScheduler an initial scheduler
	 * @return an optimal scheduler
	 */
	Scheduler computeOptConcreteReachScheduler(Point point, MDP<Function> mdp, StateValues b1, StateValues b2, boolean min, MDPRewards<Function> mdpRewards, Scheduler initialScheduler) throws PrismException
	{
		// Instantiate MDP/reward with parameter values
		MDP<Function> mdpConcrete = new MDPSimple<Function>(mdp, r -> functionFactory.fromBigRational(r.evaluate(point)));
		MDPRewards<Function> mdpRewardsConcrete = null;
		if (mdpRewards != null) {
			mdpRewardsConcrete = new MDPRewardsSimple<>(mdpRewards, mdp, r -> functionFactory.fromBigRational(r.evaluate(point)));
		}

		// Check that instantiated MDP is well defined (underlying graph is preserved)
		for (int state = 0; state < mdpConcrete.getNumStates(); state++) {
			for (int altChoice = 0; altChoice < mdpConcrete.getNumChoices(state); altChoice++) {
				Iterator<Entry<Integer, Function>> iter = mdpConcrete.getTransitionsIterator(state, altChoice);
				while (iter.hasNext()) {
					Entry<Integer, Function> e = iter.next();
					BigRational p = e.getValue().asBigRational();
					if (p.isSpecial() || p.compareTo(BigRational.ONE) == 1 || p.signum() <= 0) {
						throw new PrismException("Parametric MDP is not well defined: probability in state " + state + " is " + p);
					}
				}
			}
		}

		Scheduler scheduler = lookupScheduler(point, mdpConcrete, PropType.REACH, b1, b2, min, mdpRewardsConcrete);
		if (scheduler != null) {
			return scheduler;
		}
		scheduler = initialScheduler.clone();
		boolean changed = true;
		while (changed) {
			DTMC<Function> dtmcConcrete = new DTMCFromMDPMemorylessAdversary<>(mdpConcrete, scheduler.choices);
			MCRewards<Function> mcRewardsConcrete = mdpRewardsConcrete == null ? null : new MCRewardsFromMDPRewards<>(mdpRewardsConcrete, scheduler.choices);
			MutablePMC pmc = buildAlterablePMCForReach(dtmcConcrete, b1, b2, mcRewardsConcrete);
			StateValues fnValues = computeValues(pmc, mdpConcrete.getFirstInitialState());
			BigRational[] values = new BigRational[fnValues.getNumStates()];
			for (int state = 0; state < mdpConcrete.getNumStates(); state++) {
				values[state] = fnValues.getStateValueAsFunction(state).asBigRational();
			}
					
			changed = false;
			for (int state = 0; state < mdpConcrete.getNumStates(); state++) {
				if (!b1.getStateValueAsBoolean(state) || b2.getStateValueAsBoolean(state)) {
					continue;
				}
				BigRational bestVal = values[state];
				for (int altChoice = 0; altChoice < mdpConcrete.getNumChoices(state); altChoice++) {
					BigRational choiceValue = BigRational.ZERO;
					Iterator<Entry<Integer, Function>> iter = mdpConcrete.getTransitionsIterator(state, altChoice);
					while (iter.hasNext()) {
						Entry<Integer, Function> e = iter.next();
						int succState = e.getKey();
						BigRational succProb = e.getValue().asBigRational();
						BigRational succVal = values[succState];
						BigRational weighted = succProb.multiply(succVal);
						choiceValue = choiceValue.add(weighted);
					}
					if (mdpRewards != null) {
						choiceValue = choiceValue.add(mdpRewards.getStateReward(state).asBigRational());
						choiceValue = choiceValue.add(mdpRewards.getTransitionReward(state, altChoice).asBigRational());
					}
					if (bestVal.compareTo(choiceValue) == (min ? 1 : -1)) {
						scheduler.setChoice(state, altChoice);
						bestVal = choiceValue;
						changed = true;
					}
				}
			}
		}
		storeScheduler(PropType.REACH, b1, b2, min, mdpRewards, scheduler);

		return scheduler;
	}
	
	private void storeScheduler(PropType propType, StateValues b1, StateValues b2, boolean min,
			Rewards<?> rew, Scheduler scheduler) {
		SchedulerCacheKey cacheKey = new SchedulerCacheKey(propType, b1, b2, min, rew, null);
		ArrayList<Scheduler> schedulers = schedCache.get(cacheKey);
		if (schedulers == null) {
			schedulers = new ArrayList<Scheduler>();
			schedCache.put(cacheKey, schedulers);
		}
		schedulers.add(scheduler);		
	}

	private Scheduler lookupScheduler(Point point, MDP<Function> mdp, PropType propType, StateValues b1, StateValues b2,
			boolean min, Rewards<?> rew)
	{
		SchedulerCacheKey cacheKey = new SchedulerCacheKey(propType, b1, b2, min, rew, null);
		ArrayList<Scheduler> schedulers = schedCache.get(cacheKey);
		if (schedulers == null) {
			return null;
		}

		for (Scheduler scheduler : schedulers) {
			if (checkScheduler(point, propType, b1, b2, min, rew, scheduler)) {
				return scheduler;
			}
		}

		return null;
	}

	private boolean checkScheduler(final Point point, final PropType propType, final StateValues b1, final StateValues b2,
			final boolean min, final Rewards<?> rew, final Scheduler scheduler)
	{
		ResultCacheKey resultKey = new ResultCacheKey(propType, b1, b2, rew, scheduler, min);
		ResultCacheEntry resultCacheEntry = resultCache.get(resultKey);
		
		Function compare[] = resultCacheEntry.getCompare();		
		for (Function entry : compare) {
			if (entry.evaluate(point, false).signum() == -1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Perform precomputation for policy iteration.
	 * <br>
	 * For Pmin, if possible, for states with Pmin[ b1 U b2 ] = 0, generate zero scheduler.
	 * <br>
	 * For Rmin, generate a proper scheduler, i.e., with P^sched[ b1 U b2 ] = 1
	 * to get proper convergence in policy iteration.
	 * Assumes that states with Pmax[ b1 U b2 ] &lt; 1 have been filtered beforehand
	 * and are not contained in b1 or b2.
	 * <br>
	 * In case of maximal accumulated reward or probabilities, currently does
	 * nothing.
	 * <br>
	 * If {@code rew == null}, performs reachability, otherwise
	 * performs accumulated reward computation. {@code b2} is the target set
	 * for reachability probabilities or accumulated rewards. {@code b1} is
	 * either constantly {@code true} or describes the left side of an until
	 * property.
	 */
	private void precomputeScheduler(MDP<Function> mdp, Scheduler sched, StateValues b1, StateValues b2, MDPRewards<Function> rew, boolean min) throws PrismException
	{
		if (rew == null) {
			// probability case
			if (min) {
				precomputePmin(mdp, sched, b1, b2);
			}
		} else {
			if (min)
				precomputeRminProperScheduler(mdp, sched, b1, b2);
			// TODO: Would be nice to generate proper zero scheduler in the situations
			// where that is possible
		}
	}

	/**
	 * Precomputation for policy iteration, Rmin.
	 * For Rmin, generate a proper scheduler, i.e., with P^sched[ b1 U b2 ] = 1
	 * to get proper convergence in policy iteration.
	 * Assumes that states with Pmax[ b1 U b2 ] &lt; 1 have been filtered beforehand
	 * and are not contained in b1 or b2.
	 */
	private void precomputeRminProperScheduler(MDP<Function> mdp, Scheduler sched, StateValues b1, StateValues b2) throws PrismException
	{
		explicit.MDPModelChecker mcExplicit = new explicit.MDPModelChecker(this);
		mcExplicit.setSilentPrecomputations(true);
		int[] strat = new int[mdp.getNumStates()];
		BitSet b1bs = b1.toBitSet();
		// use prob1e strategy generation from explicit model checker
		mcExplicit.prob1(mdp, b1bs, b2.toBitSet(), false, strat);

		for (int s : IterableBitSet.getSetBits(b1bs)) {
			assert(strat[s] >= 0);
			sched.setChoice(s, strat[s]);
		}
	}

	/**
	 * Precomputation for policy iteration, Pmin.
	 * Sets decisions of {@code sched} such that states which have a minimal
	 * reachability probability of zero do already have a minimal value of zero
	 * if using this schedulers. For states with minimal value larger than zero,
	 * this scheduler needs not be minimising.
	 * {@code b2} is the target set for reachability probabilities. {@code b1} is
	 * either constantly {@code true} or describes the left side of an until
	 * property.
	 * 
	 * @param mdp Markov model to precompute for
	 * @param sched scheduler to precompute
	 * @param b1 left side of U property, or constant true
	 * @param b2 right side of U property, or of reachability reward
	 */
	private void precomputePmin(MDP<Function> mdp, Scheduler sched, StateValues b1, StateValues b2)
	{
		BitSet ones = new BitSet(mdp.getNumStates());
		for (int state = 0; state < mdp.getNumStates(); state++) {
			ones.set(state, b2.getStateValueAsBoolean(state));
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int state = 0; state < mdp.getNumStates(); state++) {
				if (!b1.getStateValueAsBoolean(state) || ones.get(state)) {
					continue;
				}
				boolean allChoicesSeenOnes = true;
				for (int choice = 0; choice < mdp.getNumChoices(state); choice++) {
					boolean seenOnes = false;
					Iterator<Entry<Integer, Function>> iter = mdp.getTransitionsIterator(state, choice);
					while (iter.hasNext()) {
						Entry<Integer, Function> e = iter.next();
						if (ones.get(e.getKey())) {
							seenOnes = true;
						}
					}
					if (!seenOnes) {
						sched.setChoice(state, choice);
						allChoicesSeenOnes = false;
					}
				}
				if (allChoicesSeenOnes) {
					ones.set(state, true);
					changed = true;
				}
			}
		}
	}

	/**
	 * Build a MutablePMC object for a probabilistic/reward reach problem.
	 */
	private MutablePMC buildAlterablePMCForReach(DTMC<Function> dtmc, StateValues b1, StateValues b2, MCRewards<Function> mcRewards) throws PrismException
	{
		MutablePMC pmc = buildAlterablePMC(dtmc, b2.toBitSet(), b1.toBitSet(), mcRewards != null, false);
		if (mcRewards != null) {
			setPMCReward(pmc, s -> b2.toBitSet().get(s) ? functionFactory.getZero() : b1.toBitSet().get(s) ? mcRewards.getStateReward(s) : functionFactory.getInf());
		}
		if (dtmc.getModelType() == ModelType.CTMC && mcRewards != null) {
			normalisePMCRewards(pmc, (CTMC<Function>) dtmc);
		}
		return pmc;
	}

	/**
	 * Build a MutablePMC object, copying transitions from a DTMC/CTMC
	 * @param dtmc D/CTMC to extract transitions an dinitial states from
	 * @param target States to note as target in MutablePMC
	 * @param nonSink Only copy transitions for (non-target) states from here (null = all)
	 * @param useRewards Flag to set in MutablePMC
	 * @param useTime Flag to set in MutablePMC
	 */
	private MutablePMC buildAlterablePMC(DTMC<Function> dtmc, BitSet target, BitSet nonSink, boolean useRewards, boolean useTime) throws PrismException
	{
		// Switch to embedded DTMC for a CTMC
		if (dtmc.getModelType() == ModelType.CTMC) {
			dtmc = ((CTMC<Function>) dtmc).getImplicitEmbeddedDTMC();
		}
		MutablePMC pmc = new MutablePMC(functionFactory, dtmc.getNumStates(), useRewards, useTime);
		for (int s = 0; s < dtmc.getNumStates(); s++) {
			// Set initial/target state info
			pmc.setTargetState(s, target.get(s));
			pmc.setInitState(s, dtmc.isInitialState(s));
			// Copy transitions or add loop for a sink state
			if ((nonSink == null || nonSink.get(s)) && !target.get(s)) {
				Iterator<Entry<Integer, Function>> iter = dtmc.getTransitionsIterator(s);
				while (iter.hasNext()) {
					Entry<Integer, Function> e = iter.next();
					pmc.addTransition(s, e.getKey(), e.getValue());
				}
			} else {
				pmc.addTransition(s, s, functionFactory.getOne());
			}
		}
		return pmc;
	}

	/**
	 * Set the reward for each state s in a MutablePMC to f(s).
	 */
	private void setPMCReward(MutablePMC pmc, java.util.function.Function<Integer,Function> f)
	{
		int numStates = pmc.getNumStates();
		for (int s = 0; s < numStates; s++) {
			pmc.setReward(s, f.apply(s));
		}
	}

	/**
	 * Divide the rewards in a MutablePMC by the exit rates of a CTMC.
	 */
	private void normalisePMCRewards(MutablePMC pmc, CTMC<Function> ctmc)
	{
		int numStates = pmc.getNumStates();
		for (int s = 0; s < numStates; s++) {
			pmc.setReward(s, pmc.getReward(s).divide(ctmc.getExitRate(s)));
		}
	}

	/**
	 * Divide the rewards in a MutablePMC by the exit rates of a CTMC.
	 */
	private void normalisePMCTimes(MutablePMC pmc, CTMC<Function> ctmc)
	{
		int numStates = pmc.getNumStates();
		for (int s = 0; s < numStates; s++) {
			pmc.setTime(s, pmc.getTime(s).divide(ctmc.getExitRate(s)));
		}
	}

	private StateValues computeValues(MutablePMC pmc, int initState)
	{
		Lumper lumper;
		switch (bisimType) {
		case NULL:
			lumper = new NullLumper(pmc);
			break;
		case STRONG:
			lumper = new StrongLumper(pmc);
			break;
		case WEAK:
			if (pmc.isUseRewards()) {
				lumper = new StrongLumper(pmc);
			} else{
				lumper = new WeakLumper(pmc);
			}
			break;
		default:
			throw new RuntimeException("invalid bisimulation method"); 
		}
		if (lumper instanceof WeakLumper && pmc.isUseTime()) {
			lumper = new StrongLumper(pmc);
		}
		
		MutablePMC quot = lumper.getQuotient();
		StateEliminator eliminator = new StateEliminator(quot, eliminationOrder);
		eliminator.eliminate();
		int[] origToCopy = lumper.getOriginalToOptimised();
		StateValues result = new StateValues(pmc.getNumStates(), initState);
		for (int state = 0; state < origToCopy.length; state++) {
			result.setStateValue(state, eliminator.getResult(origToCopy[state]));
		}
		return result;
	}

	private RegionValues computeSteadyState(Model<?> model, Region region, StateValues b1, boolean min, Rewards<?> rew) throws PrismException
	{
		if (!(model.getModelType() == ModelType.DTMC || model.getModelType() == ModelType.CTMC)) {
			throw new PrismNotSupportedException("Parametric steady state computation not supported for " + model.getModelType());
		}

		MCRewards<Function> mcRewards = (MCRewards<Function>) rew;
		RegionValues result = new RegionValues(regionFactory);
		ResultCacheEntry resultCacheEntry = lookupValues(PropType.STEADY, b1, null, rew, null, min);
		StateValues values;
		if (resultCacheEntry == null) {
			MutablePMC pmc = buildAlterablePMC((DTMC<Function>) model, new BitSet(), null, true, true);
			if (rew != null) {
				setPMCReward(pmc, s -> ((MCRewards<Function>) rew).getStateReward(s));
			} else {
				setPMCReward(pmc, s -> b1.getStateValueAsBoolean(s) ? functionFactory.getOne() : functionFactory.getZero());
			}
			if (model.getModelType() == ModelType.CTMC) {
				normalisePMCRewards(pmc, (CTMC<Function>) model);
				normalisePMCTimes(pmc, (CTMC<Function>) model);
			}
			values = computeValues(pmc, model.getFirstInitialState());
			storeValues(PropType.STEADY, b1, null, rew, null, min, values, null);
		} else {
			values = resultCacheEntry.getValues();
		}
		result.add(region, values);

		return result;
	}

	
	public RegionValues computeSteadyState(Model<?> model, RegionValues b, boolean min, Rewards<?> rew) throws PrismException
	{
		RegionValues result = new RegionValues(regionFactory);
		for (Entry<Region, StateValues> entry : b) {
			Region region = entry.getKey();
			StateValues value = entry.getValue();
			RegionValues val = computeSteadyState(model, region, value, min, rew);
			result.addAll(val);			
		}
		return result;
	}
}
