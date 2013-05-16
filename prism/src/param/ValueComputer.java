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

/**
 * Computes values for properties of a parametric Markov model. 
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class ValueComputer
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

	private ParamModel model;
	private RegionFactory regionFactory;
	private FunctionFactory functionFactory;
	private ConstraintChecker constraintChecker;
	private BigRational precision;
	private HashMap<SchedulerCacheKey,ArrayList<Scheduler>> schedCache;
	private HashMap<ResultCacheKey,ResultCacheEntry> resultCache;
	private StateEliminator.EliminationOrder eliminationOrder;
	private Lumper.BisimType bisimType;

	ValueComputer(ParamModel model, RegionFactory regionFactory, BigRational precision, StateEliminator.EliminationOrder eliminationOrder, Lumper.BisimType bisimType) {
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

	RegionValues computeUnbounded(RegionValues b1, RegionValues b2, boolean min, ParamRewardStruct rew) {
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

	private RegionValues computeUnbounded(Region region, StateValues b1, StateValues b2, boolean min, ParamRewardStruct rew)
	{
		BigRational requiredVolume = region.volume().multiply(BigRational.ONE.subtract(precision));		
		RegionValues result = new RegionValues(regionFactory);
		RegionsTODO todo = new RegionsTODO();
		todo.add(region);
		BigRational volume = BigRational.ZERO;
		while (volume.compareTo(requiredVolume) == -1) {
			Region currentRegion = todo.poll();
			Point midPoint = ((BoxRegion)currentRegion).getMidPoint();
			Scheduler scheduler = computeOptConcreteReachScheduler(midPoint, model, b1, b2, min, rew);
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

	Scheduler computeOptConcreteReachScheduler(Point point, ParamModel model, StateValues b1, StateValues b2, boolean min, ParamRewardStruct rew)
	{
		ParamModel concrete = model.instantiate(point);
		ParamRewardStruct rewConcrete = null;
		if (rew != null) {
			rewConcrete = rew.instantiate(point);
		}
		
		Scheduler scheduler = lookupScheduler(point, concrete, PropType.REACH, b1, b2, min, rewConcrete);
		if (scheduler != null) {
			return scheduler;
		}
		scheduler = new Scheduler(concrete);
		precomputeZero(concrete, scheduler, b1, b2, rew, min);
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
	 * Precomputation for policy iteration.
	 * Sets decisions of {@code sched} such that states which have a minimal
	 * reachability probability or accumulated reward of zero do already
	 * have a minimal value of zero if using this schedulers. For states with
	 * minimal value larger than zero, this scheduler needs not be minimising.
	 * In case of maximal accumulated reward or probabilities, currently does
	 * nothing. If {@code rew == null}, performs reachability, otherwise
	 * performs accumulated reward computation. {@code b2} is the target set
	 * for reachability probabilities or accumulated rewards. {@code b1} is
	 * either constantly {@code} or describes the left side of an until
	 * property.
	 * 
	 * @param mdp Markov model to precompute for
	 * @param sched scheduler to precompute
	 * @param b1 left side of U property, or constant true
	 * @param b2 right side of U property, or of reachability reward
	 * @param rew reward structure
	 * @param min true iff minimising
	 */
	private void precomputeZero(ParamModel mdp, Scheduler sched, StateValues b1, StateValues b2, ParamRewardStruct rew, boolean min)
	{
		if (!min) {
			return;
		}
		BitSet ones = new BitSet(mdp.getNumStates());
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (rew == null) {
				ones.set(state, b2.getStateValueAsBoolean(state));
			} else {
				if (!b2.getStateValueAsBoolean(state)) {
					boolean avoidReward = false;
					for (int choice = mdp.stateBegin(state); choice < mdp.stateEnd(state); choice++) {
						if (mdp.sumLeaving(choice).equals(functionFactory.getZero())) {
							sched.setChoice(state, choice);
							avoidReward = true;
							break;
						}
					}
					ones.set(state, !avoidReward);
				}
			}
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
					pmc.setReward(state, functionFactory.getZero());
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
