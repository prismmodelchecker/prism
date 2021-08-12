//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
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
import java.util.Random;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.Iterator;
import cern.colt.Arrays;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import explicit.rewards.StateRewardsSimple;
import explicit.rewards.WeightedSumMDPRewards;
import prism.Accuracy;
import prism.AccuracyFactory;
import prism.Pair;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismUtils;
//mport solver.BeliefPoint;
//import program.POMDP;
//import solver.BeliefPoint;
//import solver.AlphaVector;
//import solver.BeliefPoint;
//import solver.OutputFileWriter;
//import solver.BeliefPoint;
//import solver.ProbabilitySample;
import explicit.AlphaVector;
//import solver.BeliefPoint;

/**
 * Explicit-state model checker for partially observable Markov decision processes (POMDPs).
 */
public class POMDPModelChecker extends ProbModelChecker
{
	// Some local data structures for convenience
	
	/**
	 * Info for a single state of a belief MDP:
	 * (1) a list (over choices in the state) of distributions over beliefs, stored as hashmap;
	 * (2) optionally, a list (over choices in the state) of rewards
	 */
	class BeliefMDPState
	{
		public List<HashMap<Belief, Double>> trans;
		public List<Double> rewards;
		public BeliefMDPState()
		{
			trans = new ArrayList<>();
			rewards = new ArrayList<>();
		}
	}
	
	/**
	 * Value backup function for belief state value iteration:
	 * mapping from a state and its definition (reward + transitions)
	 * to a pair of the optimal value + choice index. 
	 */
	@FunctionalInterface
	interface BeliefMDPBackUp extends BiFunction<Belief, BeliefMDPState, Pair<Double, Integer>> {}
	
	/**
	 * A model constructed to represent a fragment of a belief MDP induced by a strategy:
	 * (1) the model (represented as an MDP for ease of storing actions labels)
	 * (2) the indices of the choices made by the strategy in states of the original POMDP
	 * (3) a list of the beliefs corresponding to each state of the model
	 */
	class POMDPStrategyModel
	{
		public MDP mdp;
		public List<Integer> strat;
		public List<Belief> beliefs;
	}
	
	/**
	 * Create a new POMDPModelChecker, inherit basic state from parent (unless null).
	 */
	public POMDPModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in @{code remain}.
	 * @param pomdp The POMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult computeReachProbs(POMDP pomdp, BitSet remain, BitSet target, boolean min, BitSet statesOfInterest) throws PrismException
	{
		ModelCheckerResult res = null;
		long timer;

		// Check we are only computing for a single state (and use initial state if unspecified)
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet();
			statesOfInterest.set(pomdp.getFirstInitialState());
		} else if (statesOfInterest.cardinality() > 1) {
			throw new PrismNotSupportedException("POMDPs can only be solved from a single start state");
		}
		
		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting probabilistic reachability (" + (min ? "min" : "max") + ")...");

		// Compute rewards
		res = computeReachProbsFixedGrid(pomdp, remain, target, min, statesOfInterest.nextSetBit(0));

		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute reachability/until probabilities,
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in @{code remain},
	 * using Lovejoy's fixed-resolution grid approach.
	 * This only computes the probabiity from a single start state
	 * @param pomdp The POMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 * @param sInit State to compute for
	 */
	protected ModelCheckerResult computeReachProbsFixedGrid(POMDP pomdp, BitSet remain, BitSet target, boolean min, int sInit) throws PrismException
	{
		// Start fixed-resolution grid approximation
		mainLog.println("calling computeReachProbsFixedGrid!!!");
		long timer = System.currentTimeMillis();
		mainLog.println("Starting fixed-resolution grid approximation (" + (min ? "min" : "max") + ")...");

		// Find out the observations for the target/remain states
		BitSet targetObs = getObservationsMatchingStates(pomdp, target);;
		if (targetObs == null) {
			throw new PrismException("Target for reachability is not observable");
		}
		BitSet remainObs = (remain == null) ? null : getObservationsMatchingStates(pomdp, remain);
		if (remain != null && remainObs == null) {
			throw new PrismException("Left-hand side of until is not observable");
		}
		mainLog.println("target obs=" + targetObs.cardinality() + ", remain obs=" + remainObs.cardinality());
		
		// Determine set of observations actually need to perform computation for
		BitSet unknownObs = new BitSet();
		unknownObs.set(0, pomdp.getNumObservations());
		unknownObs.andNot(targetObs);
		if (remainObs != null) {
			unknownObs.and(remainObs);
		}

		// Initialise the grid points (just for unknown beliefs)
		List<Belief> gridPoints = initialiseGridPoints(pomdp, unknownObs);
		mainLog.println("Grid statistics: resolution=" + gridResolution + ", points=" + gridPoints.size());
		// Construct grid belief "MDP"
		mainLog.println("Building belief space approximation...");
		List<BeliefMDPState> beliefMDP = buildBeliefMDP(pomdp, null, gridPoints);
		
		// Initialise hashmaps for storing values for the unknown belief states
		HashMap<Belief, Double> vhash = new HashMap<>();
		HashMap<Belief, Double> vhash_backUp = new HashMap<>();
		for (Belief belief : gridPoints) {
			vhash.put(belief, 0.0);
			vhash_backUp.put(belief, 0.0);
		}
		// Define value function for the full set of belief states
		Function<Belief, Double> values = belief -> approximateReachProb(belief, vhash_backUp, targetObs, unknownObs);
		// Define value backup function
		BeliefMDPBackUp backup = (belief, beliefState) -> approximateReachProbBackup(belief, beliefState, values, min);
		
		// Start iterations
		mainLog.println("Solving belief space approximation...");
		long timer2 = System.currentTimeMillis();
		int iters = 0;
		boolean done = false;
		while (!done && iters < maxIters) {
			// Iterate over all (unknown) grid points
			int unK = gridPoints.size();
			for (int b = 0; b < unK; b++) {
				Belief belief = gridPoints.get(b);
				Pair<Double, Integer> valChoice = backup.apply(belief, beliefMDP.get(b));
				vhash.put(belief, valChoice.first);
			}
			// Check termination
			done = PrismUtils.doublesAreClose(vhash, vhash_backUp, termCritParam, termCrit == TermCrit.RELATIVE);
			// back up	
			Set<Map.Entry<Belief, Double>> entries = vhash.entrySet();
			for (Map.Entry<Belief, Double> entry : entries) {
				vhash_backUp.put(entry.getKey(), entry.getValue());
			}
			iters++;
		}
		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}
		timer2 = System.currentTimeMillis() - timer2;
		mainLog.print("Belief space value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer2 / 1000.0 + " seconds.");
		
		// Extract (approximate) solution value for the initial belief
		// Also get (approximate) accuracy of result from value iteration
		Belief initialBelief = Belief.pointDistribution(sInit, pomdp);
		double outerBound = values.apply(initialBelief);
		double outerBoundMaxDiff = PrismUtils.measureSupNorm(vhash, vhash_backUp, termCrit == TermCrit.RELATIVE);
		Accuracy outerBoundAcc = AccuracyFactory.valueIteration(termCritParam, outerBoundMaxDiff, termCrit == TermCrit.RELATIVE);
		// Print result
		mainLog.println("Outer bound: " + outerBound + " (" + outerBoundAcc.toString(outerBound) + ")");
		
		// Build DTMC to get inner bound (and strategy)
		mainLog.println("\nBuilding strategy-induced model...");
		POMDPStrategyModel psm = buildStrategyModel(pomdp, sInit, null, targetObs, unknownObs, backup);
		MDP mdp = psm.mdp;
		mainLog.print("Strategy-induced model: " + mdp.infoString());
		
		// Export strategy if requested
		// NB: proper storage of strategy for genStrat not yet supported,
		// so just treat it as if -exportadv had been used, with default file (adv.tra)
		if (genStrat || exportAdv) {
			// Export in Dot format if filename extension is .dot
			if (exportAdvFilename.endsWith(".dot")) {
				mdp.exportToDotFile(exportAdvFilename, Collections.singleton(new Decorator()
				{
					@Override
					public Decoration decorateState(int state, Decoration d)
					{
						d.labelAddBelow(psm.beliefs.get(state).toString(pomdp));
						return d;
					}
				}));
			}
			// Otherwise use .tra format
			else {
				mdp.exportToPrismExplicitTra(exportAdvFilename);
			}
		}
		// Create MDP model checker (disable strat generation - if enabled, we want the POMDP one) 
		MDPModelChecker mcMDP = new MDPModelChecker(this);
		mcMDP.setExportAdv(false);
		mcMDP.setGenStrat(false);
		// Solve MDP to get inner bound
		// (just reachability: can ignore "remain" since violating states are absent)
		ModelCheckerResult mcRes = mcMDP.computeReachProbs(mdp, mdp.getLabelStates("target"), true);
		double innerBound = mcRes.soln[0];
		Accuracy innerBoundAcc = mcRes.accuracy;
		// Print result
		String innerBoundStr = "" + innerBound;
		if (innerBoundAcc != null) {
			innerBoundStr += " (" + innerBoundAcc.toString(innerBound) + ")";
		}
		mainLog.println("Inner bound: " + innerBoundStr);

		// Finished fixed-resolution grid approximation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("\nFixed-resolution grid approximation (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		// Extract and store result
		Pair<Double,Accuracy> resultValAndAcc;
		if (min) {
			resultValAndAcc = AccuracyFactory.valueAndAccuracyFromInterval(outerBound, outerBoundAcc, innerBound, innerBoundAcc);
		} else {
			resultValAndAcc = AccuracyFactory.valueAndAccuracyFromInterval(innerBound, innerBoundAcc, outerBound, outerBoundAcc);
		}
		double resultVal = resultValAndAcc.first;
		Accuracy resultAcc = resultValAndAcc.second;
		mainLog.println("Result bounds: [" + resultAcc.getResultLowerBound(resultVal) + "," + resultAcc.getResultUpperBound(resultVal) + "]");
		double soln[] = new double[pomdp.getNumStates()];
		soln[sInit] = resultVal;

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = soln;
		res.accuracy = resultAcc;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}
	
	/**Perseus
	 * Compute expected reachability rewards,
	 * i.e. compute the min/max reward accumulated to reach a state in {@code target}.
	 * @param pomdp The POMDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ArrayList<AlphaVector> backupStage(POMDP pomdp, ArrayList<AlphaVector> immediateRewards, ArrayList<AlphaVector> V, ArrayList<Belief> B) {
		int nStates = pomdp.getNumStates();
		int nObservations = pomdp.getNumObservations();
		ArrayList<Object> allActions = new ArrayList<Object>();
		
		for (int s=0; s<nStates;s++) {
			List<Object> availabeActions = pomdp.getAvailableActions(s);
			for (Object a: availabeActions) {
				if (!allActions.contains(a))
					allActions.add(a);
			}
		}
		int nActions = allActions.size();

		ArrayList<AlphaVector> Vnext = new ArrayList<AlphaVector>();
		List<Belief> Btilde = new ArrayList<Belief>();
		Btilde.addAll(B);
		
		double [][][] tm = new double [nActions][nStates] [nStates] ;
		
	
		AlphaVector[][][] gkao = new AlphaVector[V.size()][nActions][nObservations];
		for(int k=0; k<V.size(); k++) {
			for(int a=0; a<nActions; a++) {
				for(int o=0; o<nObservations; o++) {
					double[] entries = new double[nStates];
					
					for(int s=0; s<nStates; s++) {
						double val = 0.0;
						
						for(int sPrime=0; sPrime<nStates; sPrime++) {
							double value =V.get(k).getEntry(sPrime);
							double obsP = 0.0;// (a, sPrime, o);
							obsP = pomdp.getObservationProb(sPrime, o);
							
							double tranP=0.0;
							Object action = allActions.get(a);
							List<Object> availableActions= pomdp.getAvailableActions(s);
							if (availableActions.contains(action)) {
								int choice = pomdp.getChoiceByAction(s, action);
								Iterator<Entry<Integer, Double>> iter = pomdp.getTransitionsIterator(s,choice);
								while (iter.hasNext()) {
									Map.Entry<Integer, Double> trans = iter.next();
									if (trans.getKey()==sPrime) {
										tranP = trans.getValue();	 
									}
								}
							}

							val += obsP * tranP * value ;
							}
						
						entries[s] = val;
	
					}
					
					AlphaVector av = new AlphaVector(entries);
					av.setAction(a);
					gkao[k][a][o] = av;
				}
			}
		}
		

		Random rnd = new Random();

		int count =0;
		// run the backup stage
		while(Btilde.size() > 0) {
			// sample a belief point uniformly at random
			int beliefIndex = rnd.nextInt(Btilde.size());
			
			Belief b = Btilde.get(beliefIndex);
			//Btilde.remove(Btilde.indexOf(b));

			count++;

			// compute backup(b)
			AlphaVector alpha = backup(pomdp, immediateRewards, gkao, b);
			
			
			// check if we need to add alpha
			double oldValue = AlphaVector.getValue(b.toDistributionOverStates(pomdp), V);
			double newValue = alpha.getDotProduct(b.toDistributionOverStates(pomdp));
			

			if(newValue > oldValue) {
				assert alpha.getAction() >= 0 && alpha.getAction() < pomdp.getMaxNumChoices() : "invalid action: "+alpha.getAction();
				if (!Vnext.contains(alpha)) {
					Vnext.add(alpha);
				}
				
				ArrayList<Belief> newB = new ArrayList<Belief> ();
				for (int r =0; r<Btilde.size();r++) {
					Belief br = Btilde.get(r);
					if (alpha.getDotProduct(br.toDistributionOverStates(pomdp))<AlphaVector.getValue(br.toDistributionOverStates(pomdp),V) ) {
						newB.add(br);
					}

				}
				Btilde = newB;
	
				
			}
			else {
				int bestVectorIndex = AlphaVector.getBestVectorIndex(b.toDistributionOverStates(pomdp), V);
				assert V.get(bestVectorIndex).getAction() >= 0 && V.get(bestVectorIndex).getAction() < pomdp.getMaxNumChoices() : "invalid action: "+V.get(bestVectorIndex).getAction();
				
				if (!Vnext.contains(V.get(bestVectorIndex))) {
					Vnext.add(V.get(bestVectorIndex));
				}
				//Btilde.remove(Btilde.indexOf(b));
				Btilde.remove(beliefIndex);

			}
			// compute new Btilde containing non-improved belief points
			/*ArrayList<Belief> newBtilde = new ArrayList<Belief>();	
			for(Belief bp : B) {
				
				double oV = AlphaVector.getValue(b.toDistributionOverStates(pomdp), V);
				double nV = AlphaVector.getValue(b.toDistributionOverStates(pomdp), Vnext);
				if(nV < oV) {
					newBtilde.add(bp);
				}
			}	
			*/	
			//Btilde = newBtilde ;

		}
		/*
		for (int p=0;p<Vnext.size();p++) {
			int a1 = Vnext.get(p).getAction();
			double [] v1 = Vnext.get(p).getEntries();
			
			for (int j=0; j<Vnext.size();j++) {
				int a2 = Vnext.get(j).getAction();
				double [] v2 = Vnext.get(j).getEntries();
				if(p!=j & a1==a2) {
					int dom =1;
					for (int q=0; q<v1.length; q++) {
						if (v1[q]< v2[q]) {
							dom =0;
						}
					}

					if (dom==1) {
						mainLog.println("v1 is always greate than v2");
						mainLog.println(p+"  ->"+j);

						mainLog.println(Arrays.toString(v1));
						mainLog.println(Arrays.toString(v2));
						//mainLog.println(Arrays.toString(v1-v2));
					}
				}
			}
		}
*/
		return Vnext;
	}
	
	public AlphaVector backup(POMDP pomdp, List<AlphaVector> immediateRewards, AlphaVector[][][] gkao, Belief b) {
		int nStates = pomdp.getNumStates();
		int nActions = pomdp.getMaxNumChoices();
		int nObservations = pomdp.getNumObservations();
		List<AlphaVector> ga = new ArrayList<AlphaVector>();
		for(int a=0; a<nActions; a++) {
			List<AlphaVector> oVectors = new ArrayList<AlphaVector>();
			for(int o=0; o<nObservations; o++) {
				double maxVal = Double.NEGATIVE_INFINITY;
				AlphaVector maxVector = null;
				
				int K = gkao.length;
				for(int k=0; k<K; k++) {
					double product = gkao[k][a][o].getDotProduct(b.toDistributionOverStates(pomdp));
					if(product > maxVal) {
						maxVal = product;
						maxVector = gkao[k][a][o];
					}
				}
				assert maxVector != null;
				oVectors.add(maxVector);
			}
			assert oVectors.size() > 0;
			// take sum of the vectors
			AlphaVector sumVector = oVectors.get(0);
			for(int j=1; j<oVectors.size(); j++) {
				sumVector = AlphaVector.sumVectors(sumVector, oVectors.get(j));
			}
			// multiply by discount factor
			double[] sumVectorEntries = sumVector.getEntries();
			for(int s=0; s<nStates; s++) {
				sumVectorEntries[s] =  sumVectorEntries[s];
			}
			sumVector.setEntries(sumVectorEntries);
			AlphaVector av = AlphaVector.sumVectors(immediateRewards.get(a), sumVector);
			av.setAction(a);
			ga.add(av);
		}
		assert ga.size() == nActions;
		// find the maximizing vector
		double maxVal = Double.NEGATIVE_INFINITY;
		AlphaVector vFinal = null;
		for(AlphaVector av : ga) {
			double product = av.getDotProduct(b.toDistributionOverStates(pomdp));
			if(product > maxVal) {
				maxVal = product;
				vFinal = av;
			}
		}
		assert vFinal != null;
		return vFinal;
	}
	
	public ModelCheckerResult computeReachRewardsPerseus(POMDP pomdp, MDPRewards mdpRewards, BitSet target, boolean min, BitSet statesOfInterest) throws PrismException
	{ 
		ModelCheckerResult res = null;
		long timer;
	
		mainLog.println("get number of getNumObservations"+pomdp.getNumObservations());
		mainLog.println("get number of getNumUnobservations"+pomdp.getNumUnobservations());
		
		// Check we are only computing for a single state (and use initial state if unspecified)
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet();
			statesOfInterest.set(pomdp.getFirstInitialState());
		} else if (statesOfInterest.cardinality() > 1) {
			throw new PrismNotSupportedException("POMDPs can only be solved from a single start state");
		}		
		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability (" + (min ? "min" : "max") + ")...");
		mainLog.println("=== RUN POMDP SOLVER ===");
		mainLog.println("Algorithm: Perseus (point-based value iteration)");
		mainLog.println("Belief sampling started...");

		// Compute rewards
		
		int nStates = pomdp.getNumStates();
		ArrayList<Object> allActions = new ArrayList<Object>();
		
		for (int s=0; s<nStates;s++) {
			List<Object> availabeActions = pomdp.getAvailableActions(s);
			for (Object a: availabeActions) {
				if (!allActions.contains(a))
					allActions.add(a);
			}
		}
		int nActions = allActions.size();


		mainLog.println("NumeStates/nActions"+nStates+nActions);

//doing		ArrayList<BeliefPoint> B = getBeliefPoints(pomdp);
		ArrayList<Belief> B = new ArrayList<Belief>();
		ArrayList<Belief>Bset = new ArrayList<Belief>();
		
		
		B.add(pomdp.getInitialBelief());
		int BeliefSamplingSteps=100;
		int BeliefSamplingRuns=200;
		Belief b = pomdp.getInitialBelief();
		
		for(int run=0; run<BeliefSamplingRuns; run++) {
			for(int step=0; step<BeliefSamplingSteps; step++) {
				double [] b_dis = b.toDistributionOverStates(pomdp);
				if (!Bset.contains(b)) {
					for(int o=0; o<pomdp.getNumObservations(); o++) {
						HashSet<Integer> availableChoices = new HashSet<Integer> ();
						//find the available choices
						for (int i=0; i<b_dis.length; i++) {
							if (b_dis[i]>0) {
								List<Object> availbleActions = pomdp.getAvailableActions(i);
								for (Object availbleAction : availbleActions)
									availableChoices.add(pomdp.getChoiceByAction(i, availbleAction));
							}
						}
						
						//iterate all choices
						//add all possible updated choices 
						for(int a: availableChoices) {
							double probs= pomdp.getObservationProbAfterChoice(b, a, o);
							if (probs>0) {
								Belief bao = pomdp.getBeliefAfterChoiceAndObservation(b, a, o);
								double [] bao_dis = bao.toDistributionOverStates(pomdp);;
								if (!B.contains(bao)){
									B.add(bao);
								}
								
							}
						}
					}
				}
				Random rnd = new Random();
				Bset.add(b);
				//randomly choose a successor Belief to continue;
				b = B.get(rnd.nextInt(B.size()));
			}
		}
		// add corner beliefs
		for(int s=0; s<pomdp.getNumStates(); s++) {
			double[] beliefEntries = new double[pomdp.getNumStates()];
			beliefEntries[s] = 1.0;
			Belief corner = new Belief(beliefEntries, pomdp);
			if (!B.contains(corner)) {
				B.add(corner);
			}
		}
		mainLog.println("Perseus Number of beliefs: "+B.size());
		mainLog.println("Defining immediate rewards ");
						
		// create initial vector set and vectors defining immediate rewards
		ArrayList<AlphaVector> V = new ArrayList<AlphaVector>();
		ArrayList<AlphaVector> immediateRewards = new ArrayList<AlphaVector>();
		

		for(int a=0; a<nActions; a++) {
			double[] entries = new double[nStates];
			for(int s=0; s<nStates; s++) {
				//entries[s] = pomdp.getReward(s, a); original
				Object action = allActions.get(a);
			
				entries[s] = 0;
				
				if (pomdp.getAvailableActions(s).contains(action)) {							
					int choice =  pomdp.getChoiceByAction(s, action);
					entries[s] = mdpRewards.getTransitionReward(s, choice) + mdpRewards.getStateReward(s);
				}
				

			}
			AlphaVector av = new AlphaVector(entries);
			av.setAction(a);
			V.add(av);
			immediateRewards.add(av);
		}

		int stage = 1;

		System.out.println("Stage 1: "+V.size()+" vectors");


		//
		//OutputFileWriter.dumpValueFunction(pomdp, V, sp.getOutputDir()+"/"+pomdp.getInstanceName()+".alpha"+stage, sp.dumpActionLabels());
		
		// run the backup stages
		long startTime = System.currentTimeMillis();
		
		
		while(true) {
			stage++;
			//todo

			 ArrayList<AlphaVector> Vnext = backupStage(pomdp, immediateRewards, V, B);
			double valueDifference = Double.NEGATIVE_INFINITY;
			
			for(Belief bel : B) {
				double [] belief = bel.toDistributionOverStates(pomdp);
				//todo
				double diff = AlphaVector.getValue(belief, Vnext) - AlphaVector.getValue(belief, V);
				if(diff > valueDifference) valueDifference = diff;
			}
			
			double elapsed = (System.currentTimeMillis() - startTime) * 0.001;
			mainLog.println("Stage "+stage+": "+Vnext.size()+" vectors, diff "+valueDifference+", time elapsed "+elapsed+" sec");

			
			V = Vnext;

			//OutputFileWriter.dumpValueFunction(pomdp, V, sp.getOutputDir()+"/"+pomdp.getInstanceName()+".alpha"+stage, sp.dumpActionLabels());
			
			double elapsedTime = (System.currentTimeMillis() - startTime) * 0.001;
			
			double ValueFunctionTolerance = 1E-06;
			double TimeLimit =1000.0;
			if(valueDifference < ValueFunctionTolerance || elapsedTime > TimeLimit) {
				break;
			}
		}
		
		long totalSolveTime = (System.currentTimeMillis() - startTime);
		double expectedValue = AlphaVector.getValue(pomdp.getInitialBelief().toDistributionOverStates(pomdp), V);
		
		//String outputFileAlpha = sp.getOutputDir()+"/"+pomdp.getInstanceName()+".alpha";
		//OutputFileWriter.dumpValueFunction(pomdp, V, outputFileAlpha, sp.dumpActionLabels());
		
		
		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("*********Value" +expectedValue );
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		//res = computeReachRewardsFixedGrid(pomdp, mdpRewards, target, min, statesOfInterest.nextSetBit(0));

		// Update time taken
		//res.timeTaken = timer / 1000.0;
		/*
		
		
		*/

		return res;

	}
	
	/**
	 * Compute expected reachability rewards,
	 * i.e. compute the min/max reward accumulated to reach a state in {@code target}.
	 * @param pomdp The POMDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ModelCheckerResult computeReachRewards(POMDP pomdp, MDPRewards mdpRewards, BitSet target, boolean min, BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("TESSsSSSSSsssSSSSST");
		computeReachRewardsPerseus( pomdp,  mdpRewards,  target,  min,  statesOfInterest);
		mainLog.println("TESSsSSSSSsssSSSSST");
		
		
		ModelCheckerResult res = null;
		long timer;
		
		
		// Check we are only computing for a single state (and use initial state if unspecified)
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet();
			statesOfInterest.set(pomdp.getFirstInitialState());
		} else if (statesOfInterest.cardinality() > 1) {
			throw new PrismNotSupportedException("POMDPs can only be solved from a single start state");
		}
		
		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability (" + (min ? "min" : "max") + ")...");

		// Compute rewards
		res = computeReachRewardsFixedGrid(pomdp, mdpRewards, target, min, statesOfInterest.nextSetBit(0));

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute expected reachability rewards using Lovejoy's fixed-resolution grid approach.
	 * This only computes the expected reward from a single start state
	 * @param pomdp The POMMDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param sInit State to compute for
	 */
	protected ModelCheckerResult computeReachRewardsFixedGrid(POMDP pomdp, MDPRewards mdpRewards, BitSet target, boolean min, int sInit) throws PrismException
	{
		// Start fixed-resolution grid approximation
		mainLog.println("calling computeReachRewardsFixedGrid!!!!");
		long timer = System.currentTimeMillis();
		mainLog.println("Starting fixed-resolution grid approximation (" + (min ? "min" : "max") + ")...");

		// Find out the observations for the target states

		BitSet targetObs = getObservationsMatchingStates(pomdp, target);

		if (targetObs == null) {
			throw new PrismException("Target for expected reachability is not observable");
		}
		
		// Find _some_ of the states with infinite reward
		// (those from which *every* MDP strategy has prob<1 of reaching the target,
		// and therefore so does every POMDP strategy)
		MDPModelChecker mcProb1 = new MDPModelChecker(this);
		BitSet inf = mcProb1.prob1(pomdp, null, target, false, null);
		inf.flip(0, pomdp.getNumStates());
		// Find observations for which all states are known to have inf reward
		BitSet infObs = getObservationsCoveredByStates(pomdp, inf);
		mainLog.println("target obs=" + targetObs.cardinality() + ", inf obs=" + infObs.cardinality());
		
		// Determine set of observations actually need to perform computation for
		BitSet unknownObs = new BitSet();
		unknownObs.set(0, pomdp.getNumObservations());
		unknownObs.andNot(targetObs);
		unknownObs.andNot(infObs);

		// Initialise the grid points (just for unknown beliefs)
		List<Belief> gridPoints = initialiseGridPoints(pomdp, unknownObs);
		mainLog.println("Grid statistics: resolution=" + gridResolution + ", points=" + gridPoints.size());
		// Construct grid belief "MDP"
		mainLog.println("Building belief space approximation...");
		List<BeliefMDPState> beliefMDP = buildBeliefMDP(pomdp, mdpRewards, gridPoints);
		
		// Initialise hashmaps for storing values for the unknown belief states
		HashMap<Belief, Double> vhash = new HashMap<>();
		HashMap<Belief, Double> vhash_backUp = new HashMap<>();
		for (Belief belief : gridPoints) {
			vhash.put(belief, 0.0);
			vhash_backUp.put(belief, 0.0);
		}
		// Define value function for the full set of belief states
		Function<Belief, Double> values = belief -> approximateReachReward(belief, vhash_backUp, targetObs, infObs);
		// Define value backup function
		BeliefMDPBackUp backup = (belief, beliefState) -> approximateReachRewardBackup(belief, beliefState, values, min);
		
		// Start iterations
		mainLog.println("Solving belief space approximation...");
		long timer2 = System.currentTimeMillis();
		int iters = 0;
		boolean done = false;
		while (!done && iters < maxIters) {
			// Iterate over all (unknown) grid points
			int unK = gridPoints.size();
			for (int b = 0; b < unK; b++) {
				Belief belief = gridPoints.get(b);
				Pair<Double, Integer> valChoice = backup.apply(belief, beliefMDP.get(b));
				vhash.put(belief, valChoice.first);
			}
			// Check termination
			done = PrismUtils.doublesAreClose(vhash, vhash_backUp, termCritParam, termCrit == TermCrit.RELATIVE);
			// back up	
			Set<Map.Entry<Belief, Double>> entries = vhash.entrySet();
			for (Map.Entry<Belief, Double> entry : entries) {
				vhash_backUp.put(entry.getKey(), entry.getValue());
			}
			iters++;
		}

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}
		timer2 = System.currentTimeMillis() - timer2;
		mainLog.print("Belief space value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer2 / 1000.0 + " seconds.");

		// Extract (approximate) solution value for the initial belief
		// Also get (approximate) accuracy of result from value iteration
		Belief initialBelief = Belief.pointDistribution(sInit, pomdp);
		double outerBound = values.apply(initialBelief);
		double outerBoundMaxDiff = PrismUtils.measureSupNorm(vhash, vhash_backUp, termCrit == TermCrit.RELATIVE);
		Accuracy outerBoundAcc = AccuracyFactory.valueIteration(termCritParam, outerBoundMaxDiff, termCrit == TermCrit.RELATIVE);
		// Print result
		mainLog.println("Outer bound: " + outerBound + " (" + outerBoundAcc.toString(outerBound) + ")");
			
		// Build DTMC to get inner bound (and strategy)
		mainLog.println("\nBuilding strategy-induced model...");
		POMDPStrategyModel psm = buildStrategyModel(pomdp, sInit, mdpRewards, targetObs, unknownObs, backup);
		MDP mdp = psm.mdp;
		MDPRewards mdpRewardsNew = liftRewardsToStrategyModel(pomdp, mdpRewards, psm);
		mainLog.print("Strategy-induced model: " + mdp.infoString());
		
		// Export strategy if requested
		// NB: proper storage of strategy for genStrat not yet supported,
		// so just treat it as if -exportadv had been used, with default file (adv.tra)
		if (genStrat || exportAdv) {
			// Export in Dot format if filename extension is .dot
			if (exportAdvFilename.endsWith(".dot")) {
				mdp.exportToDotFile(exportAdvFilename, Collections.singleton(new Decorator()
				{
					@Override
					public Decoration decorateState(int state, Decoration d)
					{
						d.labelAddBelow(psm.beliefs.get(state).toString(pomdp));
						return d;
					}
				}));
			}
			// Otherwise use .tra format
			else {
				mdp.exportToPrismExplicitTra(exportAdvFilename);
			}
		}

		// Create MDP model checker (disable strat generation - if enabled, we want the POMDP one) 
		MDPModelChecker mcMDP = new MDPModelChecker(this);
		mcMDP.setExportAdv(false);
		mcMDP.setGenStrat(false);
		// Solve MDP to get inner bound
		ModelCheckerResult mcRes = mcMDP.computeReachRewards(mdp, mdpRewardsNew, mdp.getLabelStates("target"), true);
		double innerBound = mcRes.soln[0];
		Accuracy innerBoundAcc = mcRes.accuracy;
		// Print result
		String innerBoundStr = "" + innerBound;
		if (innerBoundAcc != null) {
			innerBoundStr += " (" + innerBoundAcc.toString(innerBound) + ")";
		}
		mainLog.println("Inner bound: " + innerBoundStr);

		// Finished fixed-resolution grid approximation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("\nFixed-resolution grid approximation (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		// Extract and store result
		Pair<Double,Accuracy> resultValAndAcc;
		if (min) {
			resultValAndAcc = AccuracyFactory.valueAndAccuracyFromInterval(outerBound, outerBoundAcc, innerBound, innerBoundAcc);
		} else {
			resultValAndAcc = AccuracyFactory.valueAndAccuracyFromInterval(innerBound, innerBoundAcc, outerBound, outerBoundAcc);
		}
		double resultVal = resultValAndAcc.first;
		Accuracy resultAcc = resultValAndAcc.second;
		mainLog.println("Result bounds: [" + resultAcc.getResultLowerBound(resultVal) + "," + resultAcc.getResultUpperBound(resultVal) + "]");
		double soln[] = new double[pomdp.getNumStates()];
		soln[sInit] = resultVal;

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = soln;
		res.accuracy = resultAcc;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute weighted multi-objective expected reachability rewards,
	 * i.e. compute the min/max weighted multi-objective reward accumulated to reach a state in {@code target}.
	 * @param pomdp The POMDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param min Min or max rewards (true=min, false=max)
	 */
	public ModelCheckerResult computeMultiReachRewards(POMDP pomdp, List<Double> weights, List<MDPRewards> mdpRewardsList, BitSet target, boolean min, BitSet statesOfInterest) throws PrismException
	{
		ModelCheckerResult res = null;
		long timer;

		// Check we are only computing for a single state (and use initial state if unspecified)
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet();
			statesOfInterest.set(pomdp.getFirstInitialState());
		} else if (statesOfInterest.cardinality() > 1) {
			throw new PrismNotSupportedException("POMDPs can only be solved from a single start state");
		}
		
		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability (" + (min ? "min" : "max") + ")...");

		// Compute rewards
		res = computeMultiReachRewardsFixedGrid(pomdp, weights, mdpRewardsList, target, min, statesOfInterest.nextSetBit(0));

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute weighted multi-objective expected reachability rewards using Lovejoy's fixed-resolution grid approach.
	 * This only computes the weighted multi-objective expected reward from a single start state
	 * @param pomdp The POMMDP
	 * @param mdpRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min Min or max rewards (true=min, false=max)
	 * @param sInit State to compute for
	 */
	protected ModelCheckerResult computeMultiReachRewardsFixedGrid(POMDP pomdp, List<Double> weights, List<MDPRewards> mdpRewardsList, BitSet target, boolean min, int sInit) throws PrismException
	{
		// Start fixed-resolution grid approximation
		long timer = System.currentTimeMillis();
		mainLog.println("calling computeMultiReachRewardsFixedGrid!!!");
		mainLog.println("Starting fixed-resolution grid approximation (" + (min ? "min" : "max") + ")...");



		// Find out the observations for the target states
		BitSet targetObs = getObservationsMatchingStates(pomdp, target);
		
		mainLog.println("target states obs"+targetObs.size());
		

		
		if (targetObs == null) {
			throw new PrismException("Target for expected reachability is not observable");
		}
		
		// Find _some_ of the states with infinite reward
		// (those from which *every* MDP strategy has prob<1 of reaching the target,
		// and therefore so does every POMDP strategy)
		MDPModelChecker mcProb1 = new MDPModelChecker(this);
		BitSet inf = mcProb1.prob1(pomdp, null, target, false, null);
		inf.flip(0, pomdp.getNumStates());
		// Find observations for which all states are known to have inf reward
		BitSet infObs = getObservationsCoveredByStates(pomdp, inf);
		mainLog.println("target obs=" + targetObs.cardinality() + ", inf obs=" + infObs.cardinality());
		
		// Determine set of observations actually need to perform computation for
		BitSet unknownObs = new BitSet();

		unknownObs.set(0, pomdp.getNumObservations());
		unknownObs.andNot(targetObs);
		unknownObs.andNot(infObs);

		// Build a combined reward structure
		int numRewards = weights.size();
		WeightedSumMDPRewards mdpRewardsWeighted = new WeightedSumMDPRewards();
		for (int i = 0; i < numRewards; i++) {
			mdpRewardsWeighted.addRewards(weights.get(i), mdpRewardsList.get(i));
		}
		
		// Initialise the grid points (just for unknown beliefs)
		List<Belief> gridPoints = initialiseGridPoints(pomdp, unknownObs);
		mainLog.println("Grid statistics: resolution=" + gridResolution + ", points=" + gridPoints.size());
		for(int q=0; q<gridPoints.size();q++) {
			mainLog.println(q);
			mainLog.println("index"+gridPoints.get(q).so);
			mainLog.println(gridPoints.get(q).bu);
		}
		// Construct grid belief "MDP"
		mainLog.println("Building belief space approximation...");
		List<BeliefMDPState> beliefMDP = buildBeliefMDP(pomdp, mdpRewardsWeighted, gridPoints);
		
		// Initialise hashmaps for storing values for the unknown belief states
		HashMap<Belief, Double> vhash = new HashMap<>();
		HashMap<Belief, Double> vhash_backUp = new HashMap<>();
		for (Belief belief : gridPoints) {
			vhash.put(belief, 0.0);
			vhash_backUp.put(belief, 0.0);
		}
		// Define value function for the full set of belief states
		Function<Belief, Double> values = belief -> approximateReachReward(belief, vhash_backUp, targetObs, infObs);
		// Define value backup function
		BeliefMDPBackUp backup = (belief, beliefState) -> approximateReachRewardBackup(belief, beliefState, values, min);
		
		
		
		// Start iterations
		mainLog.println("Solving belief space approximation...");
		long timer2 = System.currentTimeMillis();
		int iters = 0;
		boolean done = false;
		while (!done && iters < maxIters) {
			// Iterate over all (unknown) grid points
			int unK = gridPoints.size();

			for (int b = 0; b < unK; b++) {
				Belief belief = gridPoints.get(b);
				Pair<Double, Integer> valChoice = backup.apply(belief, beliefMDP.get(b));
				vhash.put(belief, valChoice.first);				//updating vhash, not vhash_backup
				}
			// Check termination
			done = PrismUtils.doublesAreClose(vhash, vhash_backUp, termCritParam, termCrit == TermCrit.RELATIVE);
			// back up	
			Set<Map.Entry<Belief, Double>> entries = vhash.entrySet();
			for (Map.Entry<Belief, Double> entry : entries) {
				vhash_backUp.put(entry.getKey(), entry.getValue());
			}
			iters++;
		}
		mainLog.println("vhash");
		for (Object key:  vhash.keySet()) {
			mainLog.println(key);
			mainLog.println(vhash.get(key));
		}
		mainLog.println("vhash_backup");
		for (Object key:  vhash_backUp.keySet()) {
			mainLog.println(key);
			mainLog.println(vhash_backUp.get(key));
		}
		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}
		timer2 = System.currentTimeMillis() - timer2;
		mainLog.print("Belief space value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iterations and " + timer2 / 1000.0 + " seconds.");

		// Extract (approximate) solution value for the initial belief
		// Also get (approximate) accuracy of result from value iteration
		Belief initialBelief = Belief.pointDistribution(sInit, pomdp);
		mainLog.println("initialBelief");
		mainLog.println(initialBelief.so);
		mainLog.println(initialBelief.bu);
		
		double outerBound = values.apply(initialBelief);
		double outerBoundMaxDiff = PrismUtils.measureSupNorm(vhash, vhash_backUp, termCrit == TermCrit.RELATIVE);
		Accuracy outerBoundAcc = AccuracyFactory.valueIteration(termCritParam, outerBoundMaxDiff, termCrit == TermCrit.RELATIVE);
		// Print result
		mainLog.println("Outer bound: " + outerBound + " (" + outerBoundAcc.toString(outerBound) + ")");
		
		// Build DTMC to get inner bound (and strategy)
		mainLog.println("\nBuilding strategy-induced model...");
		POMDPStrategyModel psm = buildStrategyModel(pomdp, sInit, mdpRewardsWeighted, targetObs, unknownObs, backup);
		MDP mdp = psm.mdp;
		MDPRewards mdpRewardsWeightedNew = liftRewardsToStrategyModel(pomdp, mdpRewardsWeighted, psm);
		List<MDPRewards> mdpRewardsListNew = new ArrayList<>();
		for (MDPRewards mdpRewards : mdpRewardsList) {
			mdpRewardsListNew.add(liftRewardsToStrategyModel(pomdp, mdpRewards, psm));
		}
		mainLog.print("Strategy-induced model: " + mdp.infoString());
		
		// Export strategy if requested
		// NB: proper storage of strategy for genStrat not yet supported,
		// so just treat it as if -exportadv had been used, with default file (adv.tra)
		if (genStrat || exportAdv) {
			// Export in Dot format if filename extension is .dot
			if (exportAdvFilename.endsWith(".dot")) {
				mdp.exportToDotFile(exportAdvFilename, Collections.singleton(new Decorator()
				{
					@Override
					public Decoration decorateState(int state, Decoration d)
					{
						d.labelAddBelow(psm.beliefs.get(state).toString(pomdp));
						return d;
					}
				}));
			}
			// Otherwise use .tra format
			else {
				mdp.exportToPrismExplicitTra(exportAdvFilename);
			}
		}

		
		// Create MDP model checker (disable strat generation - if enabled, we want the POMDP one) 
		MDPModelChecker mcMDP = new MDPModelChecker(this);
		mcMDP.setExportAdv(false);
		mcMDP.setGenStrat(false);
		// Solve MDP to get inner bound
		List<Double> point = new ArrayList<>();
		
		//get inner bound
		ModelCheckerResult mcRes = mcMDP.computeReachRewards(mdp, mdpRewardsWeightedNew, mdp.getLabelStates("target"), true);
		//get inner bound
		
		for (MDPRewards mdpRewards : mdpRewardsListNew) {
			//get value for each obs
			ModelCheckerResult mcResTmp = mcMDP.computeReachRewards(mdp, mdpRewards, mdp.getLabelStates("target"), true);
			//get value for each obs
			
			mainLog.println(mcResTmp.soln);
			
			point.add(mcResTmp.soln[0]);
		}
		double innerBound = mcRes.soln[0];
		Accuracy innerBoundAcc = mcRes.accuracy;
		// Print result
		String innerBoundStr = "" + innerBound;
		if (innerBoundAcc != null) {
			innerBoundStr += " (" + innerBoundAcc.toString(innerBound) + ")";
		}
		mainLog.println("Inner bound: " + innerBoundStr);

		// Finished fixed-resolution grid approximation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("\nFixed-resolution grid approximation (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		// Extract and store result
		Pair<Double,Accuracy> resultValAndAcc;
		if (min) {
			resultValAndAcc = AccuracyFactory.valueAndAccuracyFromInterval(outerBound, outerBoundAcc, innerBound, innerBoundAcc);
		} else {
			resultValAndAcc = AccuracyFactory.valueAndAccuracyFromInterval(innerBound, innerBoundAcc, outerBound, outerBoundAcc);
		}
		double resultVal = resultValAndAcc.first;
		Accuracy resultAcc = resultValAndAcc.second;
		mainLog.println("Result bounds: [" + resultAcc.getResultLowerBound(resultVal) + "," + resultAcc.getResultUpperBound(resultVal) + "]");
		Object solnObj[] = new Object[pomdp.getNumStates()];
		solnObj[sInit] = point; //resultVal;

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.solnObj = solnObj;
		res.accuracy = resultAcc;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Get a list of observations from a set of states
	 * (both are represented by BitSets over their indices).
	 * The states should correspond exactly to a set of observations,
	 * i.e., if a state corresponding to an observation is in the set,
	 * then all other states corresponding to it should also be.
	 * Returns null if not.
	 */
	protected BitSet getObservationsMatchingStates(POMDP pomdp, BitSet set)
	{
		// Find observations corresponding to each state in the set
		BitSet setObs = new BitSet();
		for (int s = set.nextSetBit(0); s >= 0; s = set.nextSetBit(s + 1)) {
			setObs.set(pomdp.getObservation(s));
		}
		// Recreate the set of states from the observations and make sure it matches
		BitSet set2 = new BitSet();
		int numStates = pomdp.getNumStates();
		for (int s = 0; s < numStates; s++) {
			if (setObs.get(pomdp.getObservation(s))) {
				set2.set(s);
			}
		}
		if (!set.equals(set2)) {
			return null;
		}
		return setObs;
	}
	
	/**
	 * Get a list of observations from a set of states
	 * (both are represented by BitSets over their indices).
	 * Observations are included only if all their corresponding states
	 * are included in the passed in set.
	 */
	protected BitSet getObservationsCoveredByStates(POMDP pomdp, BitSet set) throws PrismException
	{
		// Find observations corresponding to each state in the set
		BitSet setObs = new BitSet();
		for (int s = set.nextSetBit(0); s >= 0; s = set.nextSetBit(s + 1)) {
			setObs.set(pomdp.getObservation(s));
		}
		// Find observations for which not all states are in the set
		// and remove them from the observation set to be returned
		int numStates = pomdp.getNumStates();
		for (int o = setObs.nextSetBit(0); o >= 0; o = set.nextSetBit(o + 1)) {
			for (int s = 0; s < numStates; s++) {
				if (pomdp.getObservation(s) == o && !set.get(s)) {
					setObs.set(o, false);
					break;
				}
			}
		}
		return setObs;
	}
	
	/**
	 * Construct a list of beliefs for a grid-based approximation of the belief space.
	 * Only beliefs with observable values from {@code unknownObs) are added.
	 */
	protected List<Belief> initialiseGridPoints(POMDP pomdp, BitSet unknownObs)
	{
		List<Belief> gridPoints = new ArrayList<>();
		ArrayList<ArrayList<Double>> assignment;
		int numUnobservations = pomdp.getNumUnobservations();
		int numStates = pomdp.getNumStates();

		for (int so = unknownObs.nextSetBit(0); so >= 0; so = unknownObs.nextSetBit(so + 1)) {

			ArrayList<Integer> unobservsForObserv = new ArrayList<>();
			for (int s = 0; s < numStates; s++) {
				if (so == pomdp.getObservation(s)) {
					unobservsForObserv.add(pomdp.getUnobservation(s));
				}
			}

			assignment = fullAssignment(unobservsForObserv.size(), gridResolution);
			for (ArrayList<Double> inner : assignment) {
				double[] bu = new double[numUnobservations];
				int k = 0;
				for (int unobservForObserv : unobservsForObserv) {
					bu[unobservForObserv] = inner.get(k);
					k++;
				}
				gridPoints.add(new Belief(so, bu));
			}
		}
		return gridPoints;
	}
	
	/**
	 * Construct (part of) a belief MDP, just for the set of passed in belief states.
	 * If provided, also construct a list of rewards for each state.
	 * It is stored as a list (over source beliefs) of BeliefMDPState objects.
	 */
	protected List<BeliefMDPState> buildBeliefMDP(POMDP pomdp, MDPRewards mdpRewards, List<Belief> beliefs)
	{
		List<BeliefMDPState> beliefMDP = new ArrayList<>();
		for (Belief belief: beliefs) {
			beliefMDP.add(buildBeliefMDPState(pomdp, mdpRewards, belief));
		}
		return beliefMDP;
	}
	
	/**
	 * Construct a single single state (belief) of a belief MDP, stored as a
	 * list (over choices) of distributions over target beliefs.
	 * If provided, also construct a list of rewards for the state.
	 * It is stored as a BeliefMDPState object.
	 */
	protected BeliefMDPState buildBeliefMDPState(POMDP pomdp, MDPRewards mdpRewards, Belief belief)
	{

		double[] beliefInDist = belief.toDistributionOverStates(pomdp);


		BeliefMDPState beliefMDPState = new BeliefMDPState();
		// And for each choice
		
		int numChoices = pomdp.getNumChoicesForObservation(belief.so);

		for (int i = 0; i < numChoices; i++) {
			// Get successor observations and their probs
			HashMap<Integer, Double> obsProbs = pomdp.computeObservationProbsAfterAction(beliefInDist, i);
			HashMap<Belief, Double> beliefDist = new HashMap<>();
			// Find the belief for each observation
			for (Map.Entry<Integer, Double> entry : obsProbs.entrySet()) {
				int o = entry.getKey();
				Belief nextBelief = pomdp.getBeliefAfterChoiceAndObservation(belief, i, o);
				//mainLog.println("Next Belief"+nextBelief);
				beliefDist.put(nextBelief, entry.getValue());
			}
			beliefMDPState.trans.add(beliefDist);
			// Store reward too, if required
			if (mdpRewards != null) {
				beliefMDPState.rewards.add(pomdp.getRewardAfterChoice(belief, i, mdpRewards));
			}
		}
		return beliefMDPState;
	}
	
	/**
	 * Perform a single backup step of (approximate) value iteration for probabilistic reachability
	 */
	protected Pair<Double, Integer> approximateReachProbBackup(Belief belief, BeliefMDPState beliefMDPState, Function<Belief, Double> values, boolean min)
	{
		int numChoices = beliefMDPState.trans.size();
		double chosenValue = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		int chosenActionIndex = -1;
		for (int i = 0; i < numChoices; i++) {
			double value = 0;
			for (Map.Entry<Belief, Double> entry : beliefMDPState.trans.get(i).entrySet()) {
				double nextBeliefProb = entry.getValue();
				Belief nextBelief = entry.getKey();
				value += nextBeliefProb * values.apply(nextBelief);
			}
			if ((min && chosenValue - value > 1.0e-6) || (!min && value - chosenValue > 1.0e-6)) {
				chosenValue = value;
				chosenActionIndex = i;
			} else if (Math.abs(value - chosenValue) < 1.0e-6) {
				chosenActionIndex = i;
			}
		}
		return new Pair<Double, Integer>(chosenValue, chosenActionIndex);
	}
	
	/**
	 * Perform a single backup step of (approximate) value iteration for reward reachability
	 */
	protected Pair<Double, Integer> approximateReachRewardBackup(Belief belief, BeliefMDPState beliefMDPState, Function<Belief, Double> values, boolean min)
	{
		int numChoices = beliefMDPState.trans.size();
		double chosenValue = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		int chosenActionIndex = 0;
		for (int i = 0; i < numChoices; i++) {
			double value = beliefMDPState.rewards.get(i);
			for (Map.Entry<Belief, Double> entry : beliefMDPState.trans.get(i).entrySet()) {
				double nextBeliefProb = entry.getValue();
				Belief nextBelief = entry.getKey();
				value += nextBeliefProb * values.apply(nextBelief);
			}
			if ((min && chosenValue - value > 1.0e-6) || (!min && value - chosenValue > 1.0e-6)) {
				chosenValue = value;
				chosenActionIndex = i;
			} else if (Math.abs(value - chosenValue) < 1.0e-6) {
				chosenActionIndex = i;
			}
		}
		return new Pair<Double, Integer>(chosenValue, chosenActionIndex);
	}
	
	/**
	 * Compute the grid-based approximate value for a belief for probabilistic reachability
	 */
	protected double approximateReachProb(Belief belief, HashMap<Belief, Double> gridValues, BitSet targetObs, BitSet unknownObs)
	{
		// 1 for target states
		if (targetObs.get(belief.so)) {
			return 1.0;
		}
		// 0 for other non-unknown states
		else if (!unknownObs.get(belief.so)) {
			return 0.0;
		}
		// Otherwise approximate vie interpolation over grid points
		else {
			return interpolateOverGrid(belief, gridValues);
		}
	}
	
	/**
	 * Compute the grid-based approximate value for a belief for reward reachability
	 */
	protected double approximateReachReward(Belief belief, HashMap<Belief, Double> gridValues, BitSet targetObs, BitSet infObs)
	{
		// 0 for target states
		if (targetObs.get(belief.so)) {
			return 0.0;
		}
		// +Inf for states in "inf"
		else if (infObs.get(belief.so)) {
			return Double.POSITIVE_INFINITY;
		}
		// Otherwise approximate vie interpolation over grid points
		else {
			return interpolateOverGrid(belief, gridValues);
		}
	}
	
	/**
	 * Approximate the value for a belief {@code belief} by interpolating over values {@code gridValues}
	 * for a representative set of beliefs whose convex hull is the full belief space.
	 */
	protected double interpolateOverGrid(Belief belief, HashMap<Belief, Double> gridValues)
	{
		ArrayList<double[]> subSimplex = new ArrayList<>();
		double[] lambdas = new double[belief.bu.length];
		getSubSimplexAndLambdas(belief.bu, subSimplex, lambdas, gridResolution);
		double val = 0;
		for (int j = 0; j < lambdas.length; j++) {
			if (lambdas[j] >= 1e-6) {
				val += lambdas[j] * gridValues.get(new Belief(belief.so, subSimplex.get(j)));
			}
		}
		return val;
	}
	
	/**
	 * Build a (Markov chain) model representing the fragment of the belief MDP induced by an optimal strategy.
	 * The model is stored as an MDP to allow easier attachment of optional actions.
	 * @param pomdp
	 * @param sInit
	 * @param mdpRewards
	 * @param vhash
	 * @param vhash_backUp
	 * @param target
	 * @param min
	 * @param listBeliefs
	 */
	protected POMDPStrategyModel buildStrategyModel(POMDP pomdp, int sInit, MDPRewards mdpRewards, BitSet targetObs, BitSet unknownObs, BeliefMDPBackUp backup) throws PrismException
	{
		// Initialise model/strat/state storage
		MDPSimple mdp = new MDPSimple();
		List<Integer> strat = new ArrayList<>();
		IndexedSet<Belief> exploredBeliefs = new IndexedSet<>(true);
		LinkedList<Belief> toBeExploredBeliefs = new LinkedList<>();
		BitSet mdpTarget = new BitSet();
		// Add initial state
		Belief initialBelief = Belief.pointDistribution(sInit, pomdp);
		exploredBeliefs.add(initialBelief);
		toBeExploredBeliefs.offer(initialBelief);
		mdp.addState();
		mdp.addInitialState(0);
		
		// Explore model
		int src = -1;
		while (!toBeExploredBeliefs.isEmpty()) {
			Belief belief = toBeExploredBeliefs.pollFirst();
			src++;
			// Remember if this is a target state
			if (targetObs.get(belief.so)) {
				mdpTarget.set(src);
			}
			// Only explore "unknown" states
			if (unknownObs.get(belief.so)) {
				// Build the belief MDP for this belief state and solve
				BeliefMDPState beliefMDPState = buildBeliefMDPState(pomdp, mdpRewards, belief);
				Pair<Double, Integer> valChoice = backup.apply(belief, beliefMDPState);
				int chosenActionIndex = valChoice.second;
				// Build a distribution over successor belief states and add to MDP
				Distribution distr = new Distribution();
				for (Map.Entry<Belief, Double> entry : beliefMDPState.trans.get(chosenActionIndex).entrySet()) {
					double nextBeliefProb = entry.getValue();
					Belief nextBelief = entry.getKey();
					// Add each successor belief to the MDP and the "to explore" set if new
					if (exploredBeliefs.add(nextBelief)) {
						toBeExploredBeliefs.add(nextBelief);
						mdp.addState();
					}
					// Get index of state in state set
					int dest = exploredBeliefs.getIndexOfLastAdd();
					distr.add(dest, nextBeliefProb);
				}
				// Add transition distribution, with optimal choice action attached
				mdp.addActionLabelledChoice(src, distr, pomdp.getActionForObservation(belief.so, chosenActionIndex));
				// Also remember the optimal choice index for later use
				strat.add(chosenActionIndex);
			} else {
				// No transition so store dummy choice index
				strat.add(-1);
			}
		}
		// Add deadlocks to unexplored (known-value) states
		mdp.findDeadlocks(true);
		// Attach a label marking target states
		mdp.addLabel("target", mdpTarget);
		// Return
		POMDPStrategyModel psm = new POMDPStrategyModel();
		psm.mdp = mdp;
		psm.strat = strat;
		psm.beliefs = new ArrayList<>();
		psm.beliefs.addAll(exploredBeliefs.toArrayList());
		return psm;
	}
	
	/**
	 * Construct a reward structure for the model representing the fragment of the belief MDP
	 * that is induced by an optimal strategy, from a reward structure for the original POMDP.
	 */
	MDPRewards liftRewardsToStrategyModel(POMDP pomdp, MDPRewards mdpRewards, POMDPStrategyModel psm)
	{
		// Markov chain so just store as state rewards
		StateRewardsSimple stateRewards = new StateRewardsSimple();
		int numStates = psm.mdp.getNumStates();
		for (int s = 0; s < numStates; s++) {
			Belief belief = psm.beliefs.get(s);
			int ch = psm.strat.get(s);
			// Zero reward if no transitions; otherwise compute from belief
			double rew = ch == -1 ? 0.0 : pomdp.getRewardAfterChoice(belief, ch, mdpRewards);
			stateRewards.setStateReward(s, rew);
		}
		return stateRewards;
	}
	
	protected ArrayList<ArrayList<Integer>> assignGPrime(int startIndex, int min, int max, int length)
	{
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
		if (startIndex == length - 1) {
			for (int i = min; i <= max; i++) {
				ArrayList<Integer> innerList = new ArrayList<>();
				innerList.add(i);
				result.add(innerList);
			}
		} else {
			for (int i = min; i <= max; i++) {
				ArrayList<ArrayList<Integer>> nextResult = assignGPrime(startIndex + 1, 0, i, length);
				for (ArrayList<Integer> nextReulstInner : nextResult) {
					ArrayList<Integer> innerList = new ArrayList<>();
					innerList.add(i);
					for (Integer a : nextReulstInner) {
						innerList.add(a);
					}
					result.add(innerList);
				}
			}
		}

		return result;
	}

	private ArrayList<ArrayList<Double>> fullAssignment(int length, int resolution)
	{
		ArrayList<ArrayList<Integer>> GPrime = assignGPrime(0, resolution, resolution, length);
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (ArrayList<Integer> GPrimeInner : GPrime) {
			ArrayList<Double> innerList = new ArrayList<>();
			int i;
			for (i = 0; i < length - 1; i++) {
				int temp = GPrimeInner.get(i) - GPrimeInner.get(i + 1);
				innerList.add((double) temp / resolution);
			}
			innerList.add((double) GPrimeInner.get(i) / resolution);
			result.add(innerList);
		}
		return result;
	}

	private int[] getSortedPermutation(double[] inputArray)
	{
		int n = inputArray.length;
		double[] inputCopy = new double[n];
		int[] permutation = new int[n];
		int iState = 0, iIteration = 0;
		int iNonZeroEntry = 0, iZeroEntry = n - 1;
		boolean bDone = false;

		for (iState = n - 1; iState >= 0; iState--) {
			if (inputArray[iState] == 0.0) {
				inputCopy[iZeroEntry] = 0.0;
				permutation[iZeroEntry] = iState;
				iZeroEntry--;
			}

		}

		for (iState = 0; iState < n; iState++) {
			if (inputArray[iState] != 0.0) {
				inputCopy[iNonZeroEntry] = inputArray[iState];
				permutation[iNonZeroEntry] = iState;
				iNonZeroEntry++;
			}
		}

		while (!bDone) {
			bDone = true;
			for (iState = 0; iState < iNonZeroEntry - iIteration - 1; iState++) {
				if (inputCopy[iState] < inputCopy[iState + 1]) {
					swap(inputCopy, iState, iState + 1);
					swap(permutation, iState, iState + 1);
					bDone = false;
				}
			}
			iIteration++;
		}

		return permutation;
	}

	private void swap(int[] aiArray, int i, int j)
	{
		int temp = aiArray[i];
		aiArray[i] = aiArray[j];
		aiArray[j] = temp;
	}

	private void swap(double[] aiArray, int i, int j)
	{
		double temp = aiArray[i];
		aiArray[i] = aiArray[j];
		aiArray[j] = temp;
	}

	protected boolean getSubSimplexAndLambdas(double[] b, ArrayList<double[]> subSimplex, double[] lambdas, int resolution)
	{
		int n = b.length;
		int M = resolution;

		double[] X = new double[n];
		int[] V = new int[n];
		double[] D = new double[n];
		for (int i = 0; i < n; i++) {
			X[i] = 0;
			for (int j = i; j < n; j++) {
				X[i] += M * b[j];
			}
			X[i] = Math.round(X[i] * 1e6) / 1e6;
			V[i] = (int) Math.floor(X[i]);
			D[i] = X[i] - V[i];
		}

		int[] P = getSortedPermutation(D);
		//		mainLog.println("X: "+ Arrays.toString(X));
		//		mainLog.println("V: "+ Arrays.toString(V));
		//		mainLog.println("D: "+ Arrays.toString(D));
		//		mainLog.println("P: "+ Arrays.toString(P));

		ArrayList<int[]> Qs = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			int[] Q = new int[n];
			if (i == 0) {
				for (int j = 0; j < n; j++) {
					Q[j] = V[j];
				}
				Qs.add(Q);
			} else {
				for (int j = 0; j < n; j++) {
					if (j == P[i - 1]) {
						Q[j] = Qs.get(i - 1)[j] + 1;
					} else {
						Q[j] = Qs.get(i - 1)[j];
					}

				}
				Qs.add(Q);
			}
			//			mainLog.println(Arrays.toString(Q));
		}

		for (int[] Q : Qs) {
			double[] node = new double[n];
			int i;
			for (i = 0; i < n - 1; i++) {
				int temp = Q[i] - Q[i + 1];
				node[i] = (double) temp / M;
			}
			node[i] = (double) Q[i] / M;
			subSimplex.add(node);
		}

		double sum = 0;
		for (int i = 1; i < n; i++) {
			double lambda = D[P[i - 1]] - D[P[i]];
			lambdas[i] = lambda;
			sum = sum + lambda;
		}
		lambdas[0] = 1 - sum;

		for (int i = 0; i < n; i++) {
			double sum2 = 0;
			for (int j = 0; j < n; j++) {
				sum2 += lambdas[j] * subSimplex.get(j)[i];
			}
			//			mainLog.println("b["+i+"]: "+b[i]+"  b^[i]:"+sum2);
			if (Math.abs(b[i] - sum2) > 1e-4) {
				return false;
			}

		}
		return true;
	}

	public static boolean isTargetBelief(double[] belief, BitSet target)
	{
		 double prob=0;
		 for (int i = target.nextSetBit(0); i >= 0; i = target.nextSetBit(i+1)) 
		 {
			 prob+=belief[i];
		 }
		 if(Math.abs(prob-1.0)<1.0e-6)
		 {
			 return true;
		 }
		 return false;
	}	

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		POMDPModelChecker mc;
		POMDPSimple pomdp;
		ModelCheckerResult res;
		BitSet init, target;
		Map<String, BitSet> labels;
		boolean min = true;
		try {
			mc = new POMDPModelChecker(null);
			MDPSimple mdp = new MDPSimple();
			mdp.buildFromPrismExplicit(args[0]);
			//mainLog.println(mdp);
			labels = mc.loadLabelsFile(args[1]);
			//mainLog.println(labels);
			init = labels.get("init");
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			for (int i = 3; i < args.length; i++) {
				if (args[i].equals("-min"))
					min = true;
				else if (args[i].equals("-max"))
					min = false;
				else if (args[i].equals("-nopre"))
					mc.setPrecomp(false);
			}
			pomdp = new POMDPSimple(mdp);
			res = mc.computeReachRewards(pomdp, null, target, min, null);
			System.out.println(res.soln[init.nextSetBit(0)]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}


