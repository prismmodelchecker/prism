//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//  * Frits Dannenberg <frits.dannenberg@cs.ox.ac.uk> (University of Oxford)
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

package explicit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.ast.LabelList;
import parser.ast.RewardStruct;
import parser.type.TypeDouble;
import parser.Values;
import parser.State;
import prism.*;
import prism.Model;

/*
 * TODO
 * - add options for state removal, e.g.
 *   - by delta (as current)
 *   - by max probability loss per iteration (requires sort by prob)
 *   - by max number of states (requires sort by prob)
 * - compress states to Bitset (memory waste currently excessive e.g. for mapk)
 * - do not delete states immediately but only after they have been below
 *   delta for a specified number of iterations to avoid deleting and exploring
 *   the same states over and over again
 * - dynamic adaption of interval width (with of 1 seems to work best, however)
 * - improve birth process - only worth it if we find case study where it makes a
 *   difference, but could contribute to publicability then
 * - plot number of active states over time for mapk to get idea of model behaviour
 * - in gen-dat.pl, mark runs as dead if we can derive that they cannot succeed
 * - discuss public interface with Dave
 * - make stop of deletion after half of Birth threshold reached optional
 * - check whether it's worth storing incoming transitions rather than outgoing
 *   would be faster, but edges are more costly to remove
 * - if we reach a point where we only delete states and don't add new ones, it
 *   might make sense to switch to array representation and ignore the fact that
 *   we could delete further states
 */

/**
 * Implementation of fast adaptive uniformisation (FAU).
 */
public final class FastAdaptiveUniformisation extends PrismComponent
{
	/**
	 * Stores properties of states needed for fast adaptive method.
	 * This includes the current-step probability, next-state probability,
	 * and the transient probability (sum of step probabilities weighted
	 * with birth process distributions). It also contains the list of successor
	 * states and the rates to them, the number of incoming transitions
	 * (references) and a flag whether the state has a significant probability
	 * mass (alive).
	 */
	private final static class StateProp
	{
		/** current-step probability.
		 * should contain initial probability before actual analysis is started.
		 * will contain transient probability after analysis. */
		private double prob;
		/** next-state probability */
		private double nextProb;
		/** sum probability weighted with birth process distribution */
		private double sum;
		/** reward of this state */
		private double reward;
		/** rates to successor states */
		private double[] succRates;
		/** successor states */
		private StateProp[] succStates;
		/** number of incoming transitions of relevant states */
		private int references;
		/** true if and only if state probability above relevance threshold */
		private boolean alive;
		
		/**
		 * Constructs a new state property object.
		 */
		StateProp()
		{
			prob = 0.0;
			nextProb = 0.0;
			sum = 0.0;
			reward = 0.0;
			succRates = null;
			succStates = null;
			references = 0;
			alive = true;
		}
		
		/**
		 * Set current state probability.
		 * 
		 * @param prob current state probability to set
		 */
		void setProb(double prob)
		{
			this.prob = prob;
		}

		/**
		 * Gets current state probability.
		 * 
		 * @return current state probability
		 */
		double getProb()
		{
			return prob;
		}

		/**
		 * Sets next state probability.
		 * 
		 * @param nextProb next state probability to set
		 */
		void setNextProb(double nextProb)
		{
			this.nextProb = nextProb;
		}

		/**
		 * Adds value to next state probability.
		 * 
		 * @param add value to add to next state probability
		 */
		void addToNextProb(double add)
		{
			this.nextProb += add;
		}

		/**
		 * Sets weighted sum probability.
		 * 
		 * @param sum weighted sum probability to set.
		 */
		void setSum(double sum)
		{
			this.sum = sum;
		}

		/**
		 * Adds current probability times {@code poisson} to weighted sum probability.
		 * 
		 * @param poisson this value times current probability will be added to sum probability
		 */
		void addToSum(double poisson)
		{
			sum += poisson * prob;
		}

		/**
		 * Gets weighted sum probability.
		 * 
		 * @return weighted sum probability
		 */
		double getSum()
		{
			return sum;
		}

		/**
		 * Prepares next iteration step.
		 * Sets current probability to next probability, and sets next
		 * probability to zero.
		 */
		void prepareNextIteration()
		{
			prob = nextProb;
			nextProb = 0.0;
		}

		/**
		 * Set state reward.
		 * 
		 * @param reward state reward to set
		 */
		void setReward(double reward)
		{
			this.reward = reward;
		}

		/**
		 * Get state reward.
		 * 
		 * @return state reward
		 */
		double getReward()
		{
			return reward;
		}

		/**
		 * Sets rates to successor states.
		 * Expects an array of rates, so that the rate to the successor
		 * state set by {@setSuccStates} is the one given by the corresponding
		 * index. The value {@code null} is allowed here.
		 * 
		 * @param succRates rates to successor states.
		 */
		void setSuccRates(double[] succRates)
		{
			this.succRates = succRates;
		}

		/**
		 * Sets successor states.
		 * Expects an array of successor states, so that the rate set by
		 * {@setSuccRates} is the one given by the corresponding index.
		 * The value {@code null} is allowed here.
		 * 
		 * @param succStates successor states
		 */
		void setSuccStates(StateProp[] succStates)
		{
			this.succStates = succStates;
			if (succStates != null) {
				for (int succNr = 0; succNr < succStates.length; succNr++) {
					succStates[succNr].incReferences();
				}
			}
		}

		/**
		 * Returns number of successor states of this state.
		 * 
		 * @return number of successor states
		 */
		int getNumSuccs()
		{
			if (succRates == null) {
				return 0;
			} else {
				return succRates.length;
			}
		}
		
		/**
		 * Gets successor rates.
		 * 
		 * @return successor rates
		 */
		double[] getSuccRates()
		{
			return succRates;
		}

		/**
		 * Gets successor states.
		 * 
		 * @return successor states
		 */
		StateProp[] getSuccStates()
		{
			return succStates;
		}

		/**
		 * Sets whether state is alive.
		 * 
		 * @param alive whether state should be set to being alive
		 */
		void setAlive(boolean alive)
		{
			this.alive = alive;
		}

		/**
		 * Checks whether state is alive.
		 * 
		 * @return true iff state is alive
		 */
		boolean isAlive()
		{
			return alive;
		}

		/**
		 * Increments the number of references of this state.
		 * The number of references should correspond to the number of alive
		 * states which have this state as successor state.
		 */
		void incReferences()
		{
			references++;
		}

		/**
		 * Decrements the number of references of this state.
		 * The number of references should correspond to the number of alive
		 * states which have this state as successor state.
		 */
		void decReferences()
		{
			references--;
		}

		/**
		 * Deletes this state.
		 * This means basically removing all of its successors. Beforehand,
		 * their reference counter is decreased, because this state does no
		 * longer count as a model state. It is left in the model however,
		 * because it might still be the successor state of some alive state.
		 */
		void delete()
		{
			if (null != succStates) {
				for (int succNr = 0; succNr < succStates.length; succNr++) {
					succStates[succNr].decReferences();
				}
			}
			succStates = null;
			succRates = null;
			alive = false;
			prob = 0.0;
			nextProb = 0.0;
		}

		/**
		 * Checks whether this state can be removed.
		 * This is only the case if its probability is below the threshold
		 * specified, and then only if there are no transitions from alive
		 * states into this state.
		 * 
		 * @return true if and only if this state can be removed
		 */
		boolean canRemove()
		{
			return !alive && (0 == references);
		}

		/**
		 * Checks whether this state has successors or not.
		 * Will be true if and only if successor state array is nonnull.
		 * 
		 * @return whether this state has successors or not
		 */
		boolean hasSuccs()
		{
			return succStates != null;
		}

		/**
		 * Returns the sum of all rates leaving to successor states.
		 * 
		 * @return sum of all rates leaving to successor states
		 */
		double sumRates()
		{
			if (null == succRates) {
				return 0.0;
			}
			double sumRates = 0.0;
			for (int rateNr = 0; rateNr < succRates.length; rateNr++) {
				sumRates += succRates[rateNr];
			}
			return sumRates;
		}
	}
	
	/**
	 * Enum to store type of analysis to perform.
	 */
	public enum AnalysisType {
		/** transient probability distribution */
		TRANSIENT,
		/** reachability, for "F" or "U" PCTL operators */
		REACH,
		/** instantaneous rewards */
		REW_INST,
		/** cumulative rewards */
		REW_CUMUL
	}

	/** model exploration component to generate new states */
	private ModelGenerator modelGen;
	/** probability allowed to drop birth process */
	private double epsilon;
	/** probability threshold when to drop states in discrete-time process */
	private double delta;
	/** number of intervals to divide time into */
	private int numIntervals;
	/** iterations after which switch to sparse matrix if no new/dropped states */
	private int arrayThreshold;
	
	/** reward structure to use for analysis */
	private RewardStruct rewStruct = null;
	/** result value of analysis */
	private double value;
	/** model constants */
	private Values constantValues = null;
	/** maps from state (assignment of variable values) to property object */
	private LinkedHashMap<State,StateProp> states;
	/** states for which successor rates are to be computed */
	private ArrayList<State> addDistr;
	/** states which are to be deleted */
	private ArrayList<State> deleteStates;
	/** initial size of state hash map */
	private final int initSize = 3000;
	/** maximal total leaving rate of all states alive */
	private double maxRate = 0.0;
	/** target state set - used for reachability (until or finally properties) */
	private Expression target;
	/** number of consecutive iterations without new states are state drops */
	private int itersUnchanged;
	/** sum of probabilities in stages of birth process seen so far */
	private double birthProbSum;
	/** birth process used for time discretisation */
	private BirthProcess birthProc;
	/** states which fulfill this will be made absorbing - for until props */
	private Expression sink;
	/** if true, don't drop further states.
	 * Used to avoid excessive probability loss in some cases. */
	private boolean keepSumProb;
	/** maximal number of states ever stored during analysis */
	private int maxNumStates;
	/** list of special labels we need to maintain, like "init", "deadlock", etc. */
	private LabelList specialLabels;
	/** set of initial states of the model */
	private HashSet<State> initStates;
	/** type of analysis to perform */
	private AnalysisType analysisType;
	/** total loss of probability in discrete-time process */
	private double totalProbLoss;
	/** probability mass intentionally set to zero */
	private double totalProbSetZero;
	
	/**
	 * Constructor.
	 */
	public FastAdaptiveUniformisation(PrismComponent parent, ModelGenerator modelGen) throws PrismException
	{
		super(parent);
		
		this.modelGen = modelGen;
		maxNumStates = 0;

		epsilon = settings.getDouble(PrismSettings.PRISM_FAU_EPSILON);
		delta = settings.getDouble(PrismSettings.PRISM_FAU_DELTA);
		numIntervals = settings.getInteger(PrismSettings.PRISM_FAU_INTERVALS);
		arrayThreshold = settings.getInteger(PrismSettings.PRISM_FAU_ARRAYTHRESHOLD);
		analysisType = AnalysisType.TRANSIENT;
		rewStruct = null;
		target = Expression.False();
		sink = Expression.False();
		specialLabels = new LabelList();
		specialLabels.addLabel(new ExpressionIdent("deadlock"), new ExpressionIdent("deadlock"));
		specialLabels.addLabel(new ExpressionIdent("init"), new ExpressionIdent("init"));
	}

	/**
	 * Sets analysis type to perform.
	 * 
	 * @param analysisType analysis type to perform
	 */
	public void setAnalysisType(AnalysisType analysisType)
	{
		this.analysisType = analysisType;
	}

	/**
	 * Sets values for model constants.
	 * 
	 * @param constantValues values for model constants
	 */
	public void setConstantValues(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	/**
	 * Sets reward structure to use.
	 * 
	 * @param rewStruct reward structure to use
	 */
	public void setRewardStruct(RewardStruct rewStruct)
	{
		this.rewStruct = rewStruct;
	}

	/**
	 * 
	 * @param target
	 */
	public void setTarget(Expression target)
	{
		this.target = target;
	}
	
	/**
	 * Returns maximal number of states used during analysis.
	 * 
	 * @return maximal number of states used during analysis
	 */
	public int getMaxNumStates()
	{
		return maxNumStates;
	}

	/**
	 * Returns the value of the analysis.
	 * For reachability analyses, this is the probability to reach state in
	 * the reach set, for instantaneous reward properties this is the
	 * instantaneous reward and for cumulative reward analysis it is the
	 * cumulative reward. For the computation of transient probabilities
	 * without doing model checking, this value is not significant.
	 * 
	 * @return value of the analysis
	 */
	public double getValue()
	{
		return value;
	}

	/**
	 * Sets which states shall be treated as sink states.
	 * To be used for properties like "a U<=T b" where states "b || !a" have
	 * to be made absorbing.
	 * 
	 * @param sink expressing stating which states are sink states
	 * @throws PrismException thrown if problems in underlying function occurs
	 */
	public void setSink(Expression sink) throws PrismException
	{
		this.sink = sink;
		if (states != null) {
			for (Map.Entry<State,StateProp> statePair : states.entrySet()) {
				State state = statePair.getKey();
				StateProp prop = statePair.getValue();
				modelGen.exploreState(state);
				specialLabels.setLabel(0, modelGen.getNumTransitions() == 0 ? Expression.True() : Expression.False());
				specialLabels.setLabel(1, initStates.contains(state) ? Expression.True() : Expression.False());
				Expression evSink = sink.deepCopy();
				evSink = (Expression) evSink.expandLabels(specialLabels);
				if (evSink.evaluateBoolean(constantValues, state)) {
					double[] succRates = new double[1];
					StateProp[] succStates = new StateProp[1];
					succRates[0] = 1.0;
					succStates[0] = states.get(state);
					prop.setSuccRates(succRates);
					prop.setSuccStates(succStates);
				}
			}
		}
	}

	/**
	 * Get the number of states in the current window.
	 */
	public int getNumStates()
	{
		return states.size();
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(double time) throws PrismException
	{
		return doTransient(time, (StateValues) null);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * @param t Time point
	 * @param initDistFile File containing initial distribution
	 * @param currentModel 
	 */
	public StateValues doTransient(double t, File initDistFile, Model model)
			throws PrismException
	{
		StateValues initDist = null;
		if (initDistFile != null) {
			int numValues = countNumStates(initDistFile);
			initDist = new StateValues(TypeDouble.getInstance(), numValues);
			initDist.readFromFile(initDistFile);
		}
		return doTransient(t, initDist);
	}

	/**
	 * Counts number of states in a file.
	 * We need this function because the functions to read values into
	 * StateValues object expect that these objects have been initialised with
	 * the right number of states.
	 * 
	 * @param file file to count states of
	 * @return number of states in file
	 * @throws PrismException thrown in case of I/O errors
	 */
	private int countNumStates(File file) throws PrismException {
		BufferedReader in;
		String s;
		int lineNum = 0, count = 0;

		try {
			// open file for reading
			in = new BufferedReader(new FileReader(file));
			// read remaining lines
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (!("".equals(s))) {
					count++;
				}
				s = in.readLine();
				lineNum++;
			}
			// close file
			in.close();
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of file \"" + file + "\"");
		}

		return count;
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Use the passed in vector initDist as the initial probability distribution (time 0).
	 * In case initDist is null starts at the default initial state with prob 1.
	 * 
	 * @param time Time point
	 * @param initDist Initial distribution
	 */
	public StateValues doTransient(double time, StateValues initDist) throws PrismException
	{
		if (!modelGen.hasSingleInitialState())
			throw new PrismException("Fast adaptive uniformisation does not yet support models with multiple initial states");
		
		mainLog.println("\nComputing probabilities (fast adaptive uniformisation)...");
		
		if (initDist == null) {
			initDist = new StateValues();
			initDist.type = TypeDouble.getInstance();
			initDist.size = 1;
			initDist.valuesD = new double[1];
			initDist.statesList = new ArrayList<State>();
			initDist.valuesD[0] = 1.0;
			initDist.statesList.add(modelGen.getInitialState());
		}
		
		/* prepare fast adaptive uniformisation */
		addDistr = new ArrayList<State>();
		deleteStates = new ArrayList<State>();
		states = new LinkedHashMap<State,StateProp>(initSize);
		value = 0.0;
		initStates = new HashSet<State>();
		ListIterator<State> it = initDist.statesList.listIterator();
		double[] values = initDist.getDoubleArray();
		maxRate = 0.0;
		for (int stateNr = 0; stateNr < initDist.size; stateNr++) {
			State initState = it.next();
			addToModel(initState);
		}
		it = initDist.statesList.listIterator();
		for (int stateNr = 0; stateNr < initDist.size; stateNr++) {
			State initState = it.next();
			computeStateRatesAndRewards(initState);
			states.get(initState).setProb(values[stateNr]);
			maxRate = Math.max(maxRate, states.get(initState).sumRates() * 1.02);			
		}

		/* run fast adaptive uniformisation */
		computeTransientProbsAdaptive(time);

		/* prepare and return results */
		ArrayList<State> statesList = new ArrayList<State>(states.size());
		double[] probsArr = new double[states.size()];
		int probsArrEntry = 0;
		for (Map.Entry<State,StateProp> statePair : states.entrySet()) {
			statesList.add(statePair.getKey());
			probsArr[probsArrEntry] = statePair.getValue().getProb();
			probsArrEntry++;
		}
		StateValues probs = new StateValues();
		probs.type = TypeDouble.getInstance();
		probs.size = probsArr.length;
		probs.valuesD = probsArr;
		probs.statesList = statesList;		

		mainLog.println("\nTotal probability lost is : " + getTotalDiscreteLoss());
		mainLog.println("Maximal number of states stored during analysis : " + getMaxNumStates());
		
		return probs;
	}

	/**
	 * Compute transient probabilities using fast adaptive uniformisation
	 * Compute the probability of being in each state at time {@code t}.
	 * If corresponding options are set, also computes cumulative rewards.
	 * For space efficiency, the initial distribution vector will be modified and values over-written,  
	 * so if you wanted it, take a copy. 
	 * @param time time point
	 */
	public void computeTransientProbsAdaptive(double time) throws PrismException
	{
		if (addDistr == null) {
			addDistr = new ArrayList<State>();
			deleteStates = new ArrayList<State>();
			states = new LinkedHashMap<State,StateProp>(initSize);
			value = 0.0;
			prepareInitialDistribution();
		}

		double initIval = settings.getDouble(PrismSettings.PRISM_FAU_INITIVAL);
		if (time - initIval < 0.0) {
			initIval = 0.0;
		}
		if (initIval != 0.0) {
			iterateAdaptiveInterval(initIval);
			for (StateProp prop : states.values()) {
				prop.setProb(prop.getSum());
				prop.setSum(0.0);
				prop.setNextProb(0.0);
			}
			updateStates();
		}

		for (int ivalNr = 0; ivalNr < numIntervals; ivalNr++) {
			double interval = (time - initIval) / numIntervals;
			iterateAdaptiveInterval(interval);
			for (StateProp prop : states.values()) {
				prop.setProb(prop.getSum());
				prop.setSum(0.0);
				prop.setNextProb(0.0);
			}
			updateStates();
		}
		if (AnalysisType.REW_INST == analysisType) {
			for (StateProp prop : states.values()) {
				value += prop.getProb() * prop.getReward();
			}
		} else {
			for (Map.Entry<State,StateProp> statePair : states.entrySet()) {
				State state = statePair.getKey();
				StateProp prop = statePair.getValue();
				modelGen.exploreState(state);
				specialLabels.setLabel(0, modelGen.getNumTransitions() == 0 ? Expression.True() : Expression.False());
				specialLabels.setLabel(1, initStates.contains(state) ? Expression.True() : Expression.False());
				Expression evTarget = target.deepCopy();
				evTarget = (Expression) evTarget.expandLabels(specialLabels);
				if (AnalysisType.REACH == analysisType) {
					value += prop.getProb() * (evTarget.evaluateBoolean(constantValues, state) ? 1.0 : 0.0);
				}
			}
		}
	}

	/**
	 * Performs fast adaptive uniformisation for a single time interval.
	 * 
	 * @param interval duration of time interval
	 * @throws PrismException
	 */
	private void iterateAdaptiveInterval(double interval) throws PrismException
	{
		birthProc = new BirthProcess();
		birthProc.setTime(interval);
		birthProc.setEpsilon(epsilon);

		int iters = 0;
		birthProbSum = 0.0;
		itersUnchanged = 0;
		keepSumProb = false;
		while (birthProbSum < (1 - epsilon)) {
			if (birthProbSum >= epsilon/2) {
				keepSumProb = true;
			}
			if ((itersUnchanged == arrayThreshold)) {
				iters = arrayIterate(iters);
			} else {
			    long birthProcTimer = System.currentTimeMillis();
				double prob = birthProc.calculateNextProb(maxRate);
				birthProcTimer = System.currentTimeMillis() - birthProcTimer;
				birthProbSum += prob;
				collectValuePostIter(prob, birthProbSum);
				for (StateProp prop : states.values()) {
					prop.addToSum(prob);
				}
				
				mvMult(maxRate);
				updateStates();
				iters++;
			}
		}

		computeTotalDiscreteLoss();
	}

	/**
	 * Transforms the current submodel to array form.
	 * In case there are no further changes in the states discovered, or
	 * further states only become relevant after a large number of
	 * iterations, this allows the analysis to be performed much faster.
	 * After the analysis has finished or after it has to be terminated as
	 * formerly irrelevant states become relevant, results are mapped back
	 * to the original data structure. The method returns the current
	 * iteration.
	 * 
	 * In case border states become
	 * relevant, this data structure can 
	 * 
	 * @param iters current iteration number
	 * @return current iteration after termination of this method
	 * @throws PrismException thrown if problems in underlying methods occur
	 */
	private int arrayIterate(int iters) throws PrismException
	{
		/* build backwards matrix and map values */
		int numStates = states.size();
		int numTransitions = 0;
		for (StateProp prop : states.values()) {
			numTransitions += prop.getNumSuccs() + 1;
		}
		int stateNr = 0;
		HashMap<StateProp,Integer> stateToNumber = new HashMap<StateProp,Integer>(numStates);
		StateProp[] numberToState = new StateProp[numStates];
		for (StateProp prop : states.values()) {
			if (prop.isAlive()) {
				stateToNumber.put(prop, stateNr);
				numberToState[stateNr] = prop;
				stateNr++;
			}
		}
		int numAlive = stateNr;
		for (StateProp prop : states.values()) {
			if (!prop.isAlive()) {
				stateToNumber.put(prop, stateNr);
				numberToState[stateNr] = prop;
				stateNr++;
			}
		}

		double[] inProbs = new double[numTransitions];
		int[] rows = new int[numStates + 1];
		int[] cols = new int[numTransitions];
		double[] outRates = new double[numStates];
		for (StateProp prop : states.values()) {
			StateProp[] succStates = prop.getSuccStates();
			if (succStates != null) {
				for (StateProp succ : succStates) {
					rows[stateToNumber.get(succ) + 1]++;
				}
			}
			rows[stateToNumber.get(prop) + 1]++;
		}
		for (stateNr = 0; stateNr < numStates; stateNr++) {
			rows[stateNr + 1] += rows[stateNr];
		}

		for (StateProp prop : states.values()) {
			int stateNumber = stateToNumber.get(prop);
			StateProp[] succStates = prop.getSuccStates();
			double[] succRates = prop.getSuccRates();
			if (succStates != null) {
				for (int i = 0; i < succStates.length; i++) {
					StateProp succState = succStates[i];
					int succStateNumber = stateToNumber.get(succState);
					double succRate = succRates[i];
					cols[rows[succStateNumber]] = stateNumber;
					inProbs[rows[succStateNumber]] = succRate / maxRate;
					rows[succStateNumber]++;
					outRates[stateNumber] += succRate;
				}
			}
		}

		for (stateNr = 0; stateNr < numStates; stateNr++) {
			cols[rows[stateNr]] = stateNr;
			inProbs[rows[stateNr]] = (maxRate - outRates[stateNr]) / maxRate;
		}

		Arrays.fill(rows, 0);
		for (StateProp prop : states.values()) {
			StateProp[] succStates = prop.getSuccStates();
			if (succStates != null) {
				for (StateProp succ : succStates) {
					rows[stateToNumber.get(succ) + 1]++;
				}
			}
			rows[stateToNumber.get(prop) + 1]++;
		}
		for (stateNr = 0; stateNr < numStates; stateNr++) {
			rows[stateNr + 1] += rows[stateNr];
		}

		double[] rewards = new double[numStates];
		double[] probs = new double[numStates];
		double[] nextProbs = new double[numStates];
		double[] sum = new double[numStates];
		for (stateNr = 0; stateNr < numberToState.length; stateNr++) {
			StateProp prop = numberToState[stateNr];
			if (analysisType == AnalysisType.REW_CUMUL) {
				rewards[stateNr] = prop.getReward();
			}
			probs[stateNr] = prop.getProb();
			sum[stateNr] = prop.getSum();
		}

		/* iterate using matrix */
		boolean canArray = true;
		while (birthProbSum < (1 - epsilon) && canArray) {
			//			timer2 = System.currentTimeMillis();
			double prob = birthProc.calculateNextProb(maxRate);
			birthProbSum += prob;
			double mixed = (1.0 - birthProbSum) / maxRate;
			for (stateNr = 0; stateNr < numStates; stateNr++) {
				value += probs[stateNr] * mixed * rewards[stateNr];
				sum[stateNr] += prob * probs[stateNr];
				nextProbs[stateNr] = 0.0;
				for (int succNr = rows[stateNr]; succNr < rows[stateNr+1]; succNr++) {
					nextProbs[stateNr] += inProbs[succNr] * probs[cols[succNr]];
				}
				if ((stateNr < numAlive) != (nextProbs[stateNr] > delta)) {
					canArray = false;
				} else if (stateNr >= numAlive) {
				    nextProbs[stateNr] = 0.0;
				}
			}
			double[] swap = probs;
			probs = nextProbs;
			nextProbs = swap;

			iters++;
		}
		
		/* map back, update states and return current iteration */
		for (stateNr = 0; stateNr < numberToState.length; stateNr++) {
			StateProp prop = numberToState[stateNr];
			prop.setProb(probs[stateNr]);
			prop.setSum(sum[stateNr]);
		}
		updateStates();
		return iters;
	}

	/**
	 * Update analysis value after iteration.
	 * For certain analyses (currently cumulative rewards) we have to modify
	 * the analysis value after each iteration.
	 * 
	 * @param prob
	 * @param probSum
	 */
	private void collectValuePostIter(double prob, double probSum)
	{
		switch (analysisType) {
		case TRANSIENT:
			// nothing to do here, we're just computing distributions
			break;
		case REACH:
			// nothing to do here, we're collecting values later on
			break;
		case REW_INST:
			// nothing to do here, we're collecting rewards later on
			break;
		case REW_CUMUL:
			double mixed = (1.0 - probSum) / maxRate;
			for (StateProp prop : states.values()) {
				value += prop.getProb() * mixed * prop.getReward();
			}
			break;
		}

	}

	/**
	 * Updates state values once a transient analysis of time interval finished.
	 * Deletes states which can be deleted according to their current
	 * probability and the threshold. Computes new maximal rate for remaining
	 * states. Computes transitions to successors of states which have become
	 * alive to to probability threshold only after a transient analysis has
	 * finished.
	 * 
	 * @throws PrismException thrown if something goes wrong
	 */
	private void updateStates() throws PrismException
	{
		maxRate = 0.0;
		addDistr.clear();
		for (Map.Entry<State,StateProp> statePair : states.entrySet()) {
			State state = statePair.getKey();
			StateProp prop = statePair.getValue();
			if (prop.getProb() > delta) {
				prop.setAlive(true);
				if (!prop.hasSuccs()) {
					itersUnchanged = 0;
					addDistr.add(state);
				} else {
					maxRate = Math.max(maxRate, prop.sumRates());
				}
			} else {
				prop.delete();
			}
		}
		for (int stateNr = 0; stateNr < addDistr.size(); stateNr++) {
			computeStateRatesAndRewards(addDistr.get(stateNr));
			maxRate = Math.max(maxRate, states.get(addDistr.get(stateNr)).sumRates());
		}
		maxRate *= 1.02;

		removeDeletedStates();
	}

	/**
	 * Removes all states subject to removal.
	 * This affects states which both have a present-state probability below
	 * the given threshold, and do not have incoming transitions from states
	 * with a relevant probability mass.
	 */
	private void removeDeletedStates()
	{
		boolean unchanged = true;
		for (Map.Entry<State,StateProp> statePair : states.entrySet()) {
			State state = statePair.getKey();
			StateProp prop = statePair.getValue();
			if (prop.canRemove()) {
				deleteStates.add(state);
				unchanged = false;
			}
		}
		if (!keepSumProb) {
			for (int i = 0; i < deleteStates.size(); i++) {
				states.remove(deleteStates.get(i));
			}
		}
		if (unchanged) {
			itersUnchanged++;
		} else {
			itersUnchanged = 0;
		}
		deleteStates.clear();
	}
    
	/**
	 * Prepares initial distribution for the case of a single initial state.
	 * 
	 * @throws PrismException
	 */
    private void prepareInitialDistribution() throws PrismException
    {
    	initStates = new HashSet<State>();
		State initState = modelGen.getInitialState();
		initStates.add(initState);
		addToModel(initState);
		computeStateRatesAndRewards(initState);
		states.get(initState).setProb(1.0);
		maxRate = states.get(initState).sumRates() * 1.02;
	}

    /**
     * Computes total sum of lost probabilities.
     * 
     * @return total probability still in model
     */
	public void computeTotalDiscreteLoss()
	{
		double totalProb = 0;
		for (StateProp prop : states.values()) {
			totalProb += prop.getSum();
		}
		totalProb += totalProbSetZero;
		
		totalProbLoss = 1.0 - totalProb;
	}

	/**
	 * Returns the total probability loss.
	 * 
	 * @return
	 */
	public double getTotalDiscreteLoss()
	{
		return totalProbLoss;
	}

	/**
	 * Sets the probability of sink states to zero.
	 * @throws PrismException 
	 */
	public void clearSinkStates() throws PrismException {
		for (Map.Entry<State,StateProp> statePair : states.entrySet()) {
			State state = statePair.getKey();
			StateProp prop = statePair.getValue();
			modelGen.exploreState(state);
			specialLabels.setLabel(0, modelGen.getNumTransitions() == 0 ? Expression.True() : Expression.False());
			specialLabels.setLabel(1, initStates.contains(state) ? Expression.True() : Expression.False());
			Expression evSink = sink.deepCopy();
			evSink = (Expression) evSink.expandLabels(specialLabels);
			if (evSink.evaluateBoolean(constantValues, state)) {
				totalProbSetZero += prop.getProb();
				prop.setProb(0.0);
			}
		}
	}
	
	/**
	 * Adds @a state to model.
	 * Computes reward for this states, creates entry in map of states,
	 * and updates number of states
	 * 
	 * @param state state to add
	 * @throws PrismException thrown if something wrong happens in underlying methods
	 */
	private void addToModel(State state) throws PrismException
	{
		StateProp prop = new StateProp();
		prop.setReward(computeRewards(state));
		states.put(state, prop);
		maxNumStates = Math.max(maxNumStates, states.size());
	}

	/**
	 * Computes successor rates and rewards for a given state.
	 * Rewards computed depend on the reward structure set by
	 * {@code setRewardStruct}.
	 * 
	 * @param state state to compute successor rates and rewards for
	 * @throws PrismException thrown if something goes wrong
	 */
	private void computeStateRatesAndRewards(State state) throws PrismException
	{
		double[] succRates;
		StateProp[] succStates;
		modelGen.exploreState(state);
		specialLabels.setLabel(0, modelGen.getNumTransitions() == 0 ? Expression.True() : Expression.False());
		specialLabels.setLabel(1, initStates.contains(state) ? Expression.True() : Expression.False());
		Expression evSink = sink.deepCopy();
		evSink = (Expression) evSink.expandLabels(specialLabels);
		if (evSink.evaluateBoolean(constantValues, state)) {
			succRates = new double[1];
			succStates = new StateProp[1];
			succRates[0] = 1.0;
			succStates[0] = states.get(state);
		} else {
			int ntAll = modelGen.getNumTransitions();
			if (ntAll > 0) {
				succRates = new double[ntAll];
				succStates = new StateProp[ntAll];

				int t = 0;
				for (int i = 0, nc = modelGen.getNumChoices(); i < nc; i++) {
					for (int j = 0, ntChoice = modelGen.getNumTransitions(i); j < ntChoice; j++) {
						State succState = modelGen.computeTransitionTarget(i, j);
						StateProp succProp = states.get(succState);
						if (null == succProp) {
							addToModel(succState);
							succProp = states.get(succState);

							// re-explore state, as call to addToModel may have explored succState
							modelGen.exploreState(state);
						}
						succRates[t] = modelGen.getTransitionProbability(i, j);
						succStates[t] = succProp;
						t++;
					}
				}
			} else {
				succRates = new double[1];
				succStates = new StateProp[1];
				succRates[0] = 1.0;
				succStates[0] = states.get(state);
			}
		}
		states.get(state).setSuccRates(succRates);
		states.get(state).setSuccStates(succStates);
	}

	/**
	 * Perform a single matrix-vector multiplication.
	 * 
	 * @param maxRate maximal total leaving rate sum in living states
	 */
	private void mvMult(double maxRate)
	{
		for (StateProp prop : states.values()) {
			double[] succRates = prop.getSuccRates();
			StateProp[] succStates = prop.getSuccStates();
			double stateProb = prop.getProb();
			if (null != succStates) {
				double sumRates = 0.0;
				for (int succ = 0; succ < succStates.length; succ++) {
				    double rate = succRates[succ];
				    sumRates += rate;
				    succStates[succ].addToNextProb((rate / maxRate) * stateProb);
				}
				prop.addToNextProb(((maxRate - sumRates) / maxRate) * prop.getProb());
			}
		}
		for (StateProp prop : states.values()) {
			prop.prepareNextIteration();
		}
	}

	/**
	 * Checks if rewards are needed for analysis.
	 * 
	 * @return true if and only if rewards are needed
	 */
	private boolean isRewardAnalysis()
	{
		return (analysisType == AnalysisType.REW_INST)
			|| (analysisType == AnalysisType.REW_CUMUL);
	}
	
	/**
	 * Computes the reward for a given state.
	 * In case a cumulative reward analysis is to be performed, transition
	 * rewards are transformed into equivalent state rewards.
	 * 
	 * @param state the state to compute the reward of
	 * @return the reward for state @a state
	 * @throws PrismException thrown if problems occur in PRISM functions called
	 */
	private double computeRewards(State state) throws PrismException
	{
		if (!isRewardAnalysis()) {
			return 0.0;
		}
		int numChoices = 0;
		if (AnalysisType.REW_CUMUL == analysisType) {
			modelGen.exploreState(state);
			numChoices = modelGen.getNumChoices();
		}
		double sumReward = 0.0;
		int numStateItems = rewStruct.getNumItems();
		for (int i = 0; i < numStateItems; i++) {
			Expression guard = rewStruct.getStates(i);
			if (guard.evaluateBoolean(constantValues, state)) {
				double reward = rewStruct.getReward(i).evaluateDouble(constantValues, state);
				String action = rewStruct.getSynch(i);
				if (action != null) {
					if (AnalysisType.REW_CUMUL == analysisType) {
						for (int j = 0; j < numChoices; j++) {
							int numTransitions = modelGen.getNumTransitions(j);
							for (int k = 0; k < numTransitions; k++) {
								Object tAction = modelGen.getTransitionAction(j, k);
								if (tAction == null) {
									tAction = "";
								}
								if (tAction.toString().equals(action)) {
									sumReward += reward * modelGen.getTransitionProbability(j, k);
								}
							}
						}
					}
				} else {
					sumReward += reward;
				}
			}
		}

		return sumReward;
	}
}
