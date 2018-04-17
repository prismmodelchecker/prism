//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import common.IterableBitSet;
import common.IterableStateSet;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Computes values for properties of a parametric Markov model. 
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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
		final private ParamRewardStruct rew;
		
		SchedulerCacheKey(PropType propType, StateValues b1, StateValues b2, boolean min, ParamRewardStruct rew, Region region)
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
		final private ParamRewardStruct rew;
		final private Scheduler sched;
		final private boolean min;
		
		ResultCacheKey(PropType propType, StateValues b1, StateValues b2, ParamRewardStruct rew, Scheduler sched, boolean min)
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
			hash = sched.hashCode() + (hash << 6) + (hash << 16) - hash;
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
	private ParamModel model;
	private RegionFactory regionFactory;
	private FunctionFactory functionFactory;
	private ConstraintChecker constraintChecker;
	private BigRational precision;
	private HashMap<SchedulerCacheKey,ArrayList<Scheduler>> schedCache;
	private HashMap<ResultCacheKey,ResultCacheEntry> resultCache;
	private StateEliminator.EliminationOrder eliminationOrder;
	private Lumper.BisimType bisimType;

	ValueComputer(PrismComponent parent, ParamMode mode, ParamModel model, RegionFactory regionFactory, BigRational precision, StateEliminator.EliminationOrder eliminationOrder, Lumper.BisimType bisimType) {
		super(parent);
		this.mode = mode;
		this.model = model;
		this.regionFactory = regionFactory;
		this.functionFactory = regionFactory.getFunctionFactory();
		this.constraintChecker = regionFactory.getConstraintChecker();
		this.precision = precision;
		this.schedCache = new HashMap<SchedulerCacheKey,ArrayList<Scheduler>>();
		this.resultCache = new HashMap<ResultCacheKey,ResultCacheEntry>();
		this.eliminationOrder = eliminationOrder;
		this.bisimType = bisimType;
	}

	RegionValues computeUnbounded(RegionValues b1, RegionValues b2, boolean min, ParamRewardStruct rew) throws PrismException {
		RegionValues result = new RegionValues(regionFactory);
		RegionValuesIntersections co = new RegionValuesIntersections(b1, b2);
		for (RegionIntersection inter : co) {
			Region region = inter.getRegion();
			StateValues value1 = inter.getStateValues1();
			StateValues value2 = inter.getStateValues2();
			RegionValues val = computeUnbounded(region, value1, value2, min, rew);
			result.addAll(val);
		}
		return result;
	}

	private RegionValues computeUnbounded(Region region, StateValues b1, StateValues b2, boolean min, ParamRewardStruct rew) throws PrismException
	{
		if (rew != null) {
			// determine infinity states
			explicit.MDPModelChecker mcExplicit = new explicit.MDPModelChecker(this);
			mcExplicit.setSilentPrecomputations(true);
			BitSet inf = mcExplicit.prob1(model, b1.toBitSet(), b2.toBitSet(), !min, null);
			inf.flip(0, model.getNumStates());

			for (int i : new IterableStateSet(inf, model.getNumStates())) {
				// clear states with infinite value from b1 so they will get Infinity value
				// in the DTMC
				b1.setStateValue(i, false);
			}
		}

		switch (model.getModelType()) {
		case CTMC:
		case DTMC:
			return computeUnboundedMC(region, b1, b2, rew);
		case MDP:
			if (model.getMaxNumChoices() == 1) {
				return computeUnboundedMC(region, b1, b2, rew);
			}
			return computeUnboundedMDP(region, b1, b2, min, rew);
		default:
			throw new PrismNotSupportedException("Parametric unbounded reachability computation not supported for " + model.getModelType());
		}
	}

	private RegionValues computeUnboundedMC(Region region, StateValues b1, StateValues b2, ParamRewardStruct rew) throws PrismException
	{
		// Convert to MutablePMC, using trivial scheduler (take first choice everywhere)
		Scheduler trivialScheduler = new Scheduler(model);

		MutablePMC pmc = buildAlterablePMCForReach(model, b1, b2, trivialScheduler, rew);
		if (rew != null && mode == ParamMode.EXACT) {
			rew.checkForNonNormalRewards();
		}

		StateValues values = computeValues(pmc, model.getFirstInitialState());
		return regionFactory.completeCover(values);
	}

	private RegionValues computeUnboundedMDP(Region region, StateValues b1, StateValues b2, boolean min, ParamRewardStruct rew) throws PrismException
	{
		BigRational precisionForThisRegion = region.volume().multiply(precision);
		BigRational requiredVolume = region.volume().subtract(precisionForThisRegion);
		RegionValues result = new RegionValues(regionFactory);
		RegionsTODO todo = new RegionsTODO();
		todo.add(region);
		BigRational volume = BigRational.ZERO;

		Scheduler initialScheduler = new Scheduler(model);
		precomputeScheduler(model, initialScheduler, b1, b2, rew, min);

		while (volume.compareTo(requiredVolume) == -1) {
			Region currentRegion = todo.poll();
			Point midPoint = ((BoxRegion)currentRegion).getMidPoint();
			Scheduler scheduler = computeOptConcreteReachScheduler(midPoint, model, b1, b2, min, rew, initialScheduler);
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

			ResultCacheEntry resultCacheEntry = lookupValues(PropType.REACH, b1, b2, rew, scheduler, min);
			Function[] compare;
			StateValues values;
			if (resultCacheEntry == null) {
				MutablePMC pmc = buildAlterablePMCForReach(model, b1, b2, scheduler, rew);
				values = computeValues(pmc, model.getFirstInitialState());
				compare = computeCompare(b1, b2, rew, scheduler, min, values);
				storeValues(PropType.REACH, b1, b2, rew, scheduler, min, values, compare);
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
	
	private Function[] computeCompare(StateValues b1, StateValues b2,
			ParamRewardStruct rew, Scheduler scheduler, boolean min,
			StateValues values) {
		HashSet<Function> allValues = new HashSet<Function>();
		
		for (int state = 0; state < model.getNumStates(); state++) {
			if (!b1.getStateValueAsBoolean(state) || b2.getStateValueAsBoolean(state)) {
				continue;
			}
			Function stateValue = values.getStateValueAsFunction(state);
			for (int altChoice = model.stateBegin(state); altChoice < model.stateEnd(state); altChoice++) {
				Function choiceValue = (rew == null) ? functionFactory.getZero()  : rew.getReward(altChoice);
				for (int succ = model.choiceBegin(altChoice); succ < model.choiceEnd(altChoice); succ++) {
					int succState = model.succState(succ);
					Function weighted = model.succProb(succ).multiply(values.getStateValueAsFunction(succState));
					choiceValue = choiceValue.add(weighted);
				}
				choiceValue = min ? choiceValue.subtract(stateValue) : stateValue.subtract(choiceValue);
				allValues.add(choiceValue);
			}
		}

		return allValues.toArray(new Function[0]);
	}

	private void storeValues(PropType propType, StateValues b1, StateValues b2,
			ParamRewardStruct rew, Scheduler scheduler, boolean min, StateValues values, Function[] compare) {
		ResultCacheKey cacheKey = new ResultCacheKey(propType, b1, b2, rew, scheduler, min);
		ResultCacheEntry resultCacheEntry = new ResultCacheEntry(values, compare);
		resultCache.put(cacheKey, resultCacheEntry);
	}

	private ResultCacheEntry lookupValues(PropType propType, StateValues b1, StateValues b2,
			ParamRewardStruct rew, Scheduler scheduler, boolean min) {
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
	 * @param model the model
	 * @param b1 the set of 'safe' states
	 * @param b2 the set of 'target' states
	 * @param min compute min or max? true = min
	 * @param rew if non-null, compute reachability reward
	 * @param initialScheduler an initial scheduler
	 * @return an optimal scheduler
	 */
	Scheduler computeOptConcreteReachScheduler(Point point, ParamModel model, StateValues b1, StateValues b2, boolean min, ParamRewardStruct rew, Scheduler initialScheduler) throws PrismException
	{
		ParamModel concrete = model.instantiate(point, true);
		if (concrete == null) {
			// point leads to non-welldefined model
			return null;
		}
		ParamRewardStruct rewConcrete = null;
		if (rew != null) {
			rewConcrete = rew.instantiate(point);
			if (rewConcrete.hasNegativeRewards()) {
				if (mode == ParamMode.EXACT) {
					throw new PrismNotSupportedException(mode.Engine() + " currently does not support negative rewards in reachability reward computations");
				} else {
					// point leads to negative values in reward structure => unsupported
					return null;
				}
			}
		}
		
		Scheduler scheduler = lookupScheduler(point, concrete, PropType.REACH, b1, b2, min, rewConcrete);
		if (scheduler != null) {
			return scheduler;
		}
		scheduler = initialScheduler.clone();
		boolean changed = true;
		while (changed) {
			MutablePMC pmc = buildAlterablePMCForReach(concrete, b1, b2, scheduler, rew);
			StateValues fnValues = computeValues(pmc, concrete.getFirstInitialState());
			BigRational[] values = new BigRational[fnValues.getNumStates()];
			for (int state = 0; state < concrete.getNumStates(); state++) {
				values[state] = fnValues.getStateValueAsFunction(state).asBigRational();
			}
					
			changed = false;
			for (int state = 0; state < concrete.getNumStates(); state++) {
				if (!b1.getStateValueAsBoolean(state) || b2.getStateValueAsBoolean(state)) {
					continue;
				}
				BigRational bestVal = values[state];
				for (int altChoice = concrete.stateBegin(state); altChoice < concrete.stateEnd(state); altChoice++) {
					BigRational choiceValue = BigRational.ZERO;
					for (int succ = concrete.choiceBegin(altChoice); succ < concrete.choiceEnd(altChoice); succ++) {
						int succState = concrete.succState(succ);
						BigRational succProb = concrete.succProb(succ).asBigRational();
						BigRational succVal = values[succState];
						BigRational weighted = succProb.multiply(succVal);
						choiceValue = choiceValue.add(weighted);
					}
					if (rew != null) {
						choiceValue = choiceValue.add(rew.getReward(altChoice).asBigRational());
					}
					if (bestVal.compareTo(choiceValue) == (min ? 1 : -1)) {
						scheduler.setChoice(state, altChoice);
						bestVal = choiceValue;
						changed = true;
					}
				}
			}
		}
		storeScheduler(PropType.REACH, b1, b2, min, rew, scheduler);

		return scheduler;
	}
	
	private void storeScheduler(PropType propType, StateValues b1, StateValues b2, boolean min,
			ParamRewardStruct rew, Scheduler scheduler) {
		SchedulerCacheKey cacheKey = new SchedulerCacheKey(propType, b1, b2, min, rew, null);
		ArrayList<Scheduler> schedulers = schedCache.get(cacheKey);
		if (schedulers == null) {
			schedulers = new ArrayList<Scheduler>();
			schedCache.put(cacheKey, schedulers);
		}
		schedulers.add(scheduler);		
	}

	private Scheduler lookupScheduler(Point point, ParamModel concrete, PropType propType, StateValues b1, StateValues b2,
			boolean min, ParamRewardStruct rew)
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
			final boolean min, final ParamRewardStruct rew, final Scheduler scheduler)
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
	private void precomputeScheduler(ParamModel mdp, Scheduler sched, StateValues b1, StateValues b2, ParamRewardStruct rew, boolean min) throws PrismException
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
	private void precomputeRminProperScheduler(ParamModel mdp, Scheduler sched, StateValues b1, StateValues b2) throws PrismException
	{
		explicit.MDPModelChecker mcExplicit = new explicit.MDPModelChecker(this);
		mcExplicit.setSilentPrecomputations(true);
		int[] strat = new int[mdp.getNumStates()];
		BitSet b1bs = b1.toBitSet();
		// use prob1e strategy generation from explicit model checker
		mcExplicit.prob1(mdp, b1bs, b2.toBitSet(), false, strat);

		for (int s : IterableBitSet.getSetBits(b1bs)) {
			assert(strat[s] >= 0);
			sched.setChoice(s, mdp.stateBegin(s) + strat[s]);
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
	private void precomputePmin(ParamModel mdp, Scheduler sched, StateValues b1, StateValues b2)
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
				for (int choice = mdp.stateBegin(state); choice < mdp.stateEnd(state); choice++) {
					boolean seenOnes = false;
					for (int succ = mdp.choiceBegin(choice); succ < mdp.choiceEnd(choice); succ++) {
						if (ones.get(mdp.succState(succ))) {
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
	
	private MutablePMC buildAlterablePMCForReach(ParamModel model, StateValues b1, StateValues b2, Scheduler scheduler, ParamRewardStruct rew)
	{
		MutablePMC pmc = new MutablePMC(functionFactory, model.getNumStates(), rew != null, false);
		for (int state = 0; state < model.getNumStates(); state++) {
			boolean isTarget = b2.getStateValueAsBoolean(state);
			boolean isSink = isTarget || !b1.getStateValueAsBoolean(state);
			int choice = scheduler.getChoice(state);
			pmc.setTargetState(state, isTarget);
			pmc.setInitState(state, model.isInitialState(state));
			if (isSink) {
				pmc.addTransition(state, state, functionFactory.getOne());
				if (null != rew) {
					if (isTarget) {
						pmc.setReward(state, functionFactory.getZero());
					} else {
						// !b1 & !b2 state -> infinite (can not reach b2 via b1 states)
						pmc.setReward(state, functionFactory.getInf());
					}
				}
			} else {
				if (rew != null) {
					pmc.setReward(state, rew.getReward(choice));
				}
				Function sum = functionFactory.getZero();
				for (int succ = model.choiceBegin(choice); succ < model.choiceEnd(choice); succ++) {
					sum = sum.add(model.succProb(succ));
				}
				for (int succ = model.choiceBegin(choice); succ < model.choiceEnd(choice); succ++) {
					pmc.addTransition(state, model.succState(succ), model.succProb(succ).divide(sum));
				}
			}
		}
		
		return pmc;
	}

	private MutablePMC buildAlterablePMCForSteady(ParamModel model, StateValues b1, Scheduler scheduler, ParamRewardStruct rew)
	{
		MutablePMC pmc = new MutablePMC(functionFactory, model.getNumStates(), true, true);
		for (int state = 0; state < model.getNumStates(); state++) {
			int choice = scheduler.getChoice(state);
			pmc.setTargetState(state, false);
			pmc.setInitState(state, model.isInitialState(state));
			Function sumLeaving = model.sumLeaving(choice);
			if (rew != null) {
				pmc.setReward(state, rew.getReward(choice));
			} else {
				pmc.setReward(state, b1.getStateValueAsBoolean(state) ? functionFactory.getOne().divide(sumLeaving) : functionFactory.getZero());
			}
			for (int succ = model.choiceBegin(choice); succ < model.choiceEnd(choice); succ++) {
				pmc.addTransition(state, model.succState(succ), model.succProb(succ));
			}
			pmc.setTime(state, functionFactory.getOne().divide(sumLeaving));
		}
		
		return pmc;
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

	private RegionValues computeSteadyState(Region region, StateValues b1, boolean min, ParamRewardStruct rew)
	{
		RegionValues result = new RegionValues(regionFactory);
		Scheduler scheduler = new Scheduler(model);
		ResultCacheEntry resultCacheEntry = lookupValues(PropType.STEADY, b1, null, rew, scheduler, min);
		StateValues values;
		if (resultCacheEntry == null) {
			MutablePMC pmc = buildAlterablePMCForSteady(model, b1, scheduler, rew);
			values = computeValues(pmc, model.getFirstInitialState());
			storeValues(PropType.STEADY, b1, null, rew, scheduler, min, values, null);
		} else {
			values = resultCacheEntry.getValues();
		}
		result.add(region, values);

		return result;
	}

	
	public RegionValues computeSteadyState(RegionValues b, boolean min, ParamRewardStruct rew)
	{
		RegionValues result = new RegionValues(regionFactory);
		for (Entry<Region, StateValues> entry : b) {
			Region region = entry.getKey();
			StateValues value = entry.getValue();
			RegionValues val = computeSteadyState(region, value, min, rew);
			result.addAll(val);			
		}
		return result;
	}
}
