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
import explicit.AlphaMatrix;
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
	public ArrayList<AlphaVector> backupStage(POMDP pomdp, ArrayList<AlphaVector> immediateRewards, ArrayList<AlphaVector> V, ArrayList<Belief> B, BitSet unknowObs) {
		
		int nStates = pomdp.getNumStates();
		int nObservations = pomdp.getNumObservations();
		ArrayList<Object> allActions =getAllActions(pomdp);
		int nActions = allActions.size();
		
		ArrayList<AlphaVector> Vnext = new ArrayList<AlphaVector>();
		List<Belief> Btilde = new ArrayList<Belief>();
		Btilde.addAll(B);
		double [][][] tm = new double [nActions][nStates] [nStates] ;
		
		AlphaVector[][][] gkao = new AlphaVector[V.size()][nActions][nObservations];
		for(int k=0; k<V.size(); k++) {
			for(int a=0; a<nActions; a++) {
				//if(V.get(k).getAction()!=a)
					//continue;
				for(int o=0; o<nObservations; o++) {
					double[] entries = new double[nStates];
					for(int s=0; s<nStates; s++) {
						double val = 0.0;
						Object action = allActions.get(a);
						List<Object> availableActions= pomdp.getAvailableActions(s);
	
						if (availableActions.contains(action)) {
							for(int sPrime=0; sPrime<nStates; sPrime++) {
								double value = V.get(k).getEntry(sPrime);
								double obsP = 0.0;// (a, sPrime, o);
								obsP = pomdp.getObservationProb(sPrime, o);
								double tranP=0.0;
								int choice = pomdp.getChoiceByAction(s, action);
								Iterator<Entry<Integer, Double>> iter = pomdp.getTransitionsIterator(s,choice);
								while (iter.hasNext()) {
									Map.Entry<Integer, Double> trans = iter.next();
									if (trans.getKey()==sPrime) {
										tranP = trans.getValue();	 
									}
								}
								val += obsP * tranP * value ;
							}
						}
						else {
								//if (unknowObs.get(pomdp.getObservation(s))) {
								//	val =-9999;
									val*=1;
								//}
						}
	
						entries[s] = val;
					}
					AlphaVector av = new AlphaVector(entries);
					av.setAction(a);
					gkao[k][a][o] = av;
					//mainLog.print("");//mainLog.print(k+" "+a+" "+o+" "+" gkao Action = "+ av.getAction());
					mainLog.print("");//mainLog.println( "   value = "+ Arrays.toString(av.getEntries()));
				}
			}
		}
		
		Random rnd = new Random();
		int count =0;
		mainLog.print("");//mainLog.println("Bsize="+Btilde.size());
		// run the backup stage
		while(Btilde.size() > 0) {
			// sample a belief point uniformly at random
			int beliefIndex = rnd.nextInt(Btilde.size());
			Belief b = Btilde.get(beliefIndex);
			//Btilde.remove(Btilde.indexOf(b));
			count++;
	
			// compute backup(b)
			mainLog.print("");//mainLog.println("*************ready to back up for"+b );
			mainLog.print("");//mainLog.println("b dis ="+ Arrays.toString(b.toDistributionOverStates(pomdp)));
	
			
			AlphaVector alpha = backup(pomdp, immediateRewards, gkao, b, V);
			if (alpha==null) {
				Btilde.remove(beliefIndex);
				continue;
			}
			mainLog.print("");//mainLog.println("alpha a="+alpha.getAction());
			
			ArrayList<Integer> possibleObservationsForBeliefAction = getPossibleObservationsForBeliefAction(b, allActions.get(alpha.getAction()), pomdp);
			for (int p=0; p<possibleObservationsForBeliefAction.size() ;p++)
				mainLog.print("");//mainLog.print("o="+possibleObservationsForBeliefAction.get(p)+" ");
			
			mainLog.print("");//mainLog.println("alpha v="+Arrays.toString(alpha.getEntries()));
			
			// check if we need to add alpha
			double oldValue = (getPossibleValue(b,V,pomdp));
			mainLog.print("");//mainLog.println("oldValue="+oldValue);
			//double oldValue = AlphaVector.getValue(b.toDistributionOverStates(pomdp), V));
			
			double newValue =  (alpha.getDotProduct(b.toDistributionOverStates(pomdp)));
			mainLog.print("");//mainLog.println("newValueValue="+newValue);
			if(oldValue!=newValue)
				mainLog.print("");//mainLog.println("different="+(oldValue-newValue));
	
			double diff = Math.abs(oldValue-newValue);
			if(diff >0) {
				assert alpha.getAction() >= 0 && alpha.getAction() < pomdp.getMaxNumChoices() : "invalid action: "+alpha.getAction();
				if (!Vnext.contains(alpha)) {
					Vnext.add(alpha);
					mainLog.print("");//mainLog.println("1 Adding vector action ="+alpha.getAction());
					mainLog.print("");//mainLog.println("1 Adding vector action ="+Arrays.toString(alpha.getEntries()));
				}
				ArrayList<Belief> newB = new ArrayList<Belief> ();
				Btilde.remove(beliefIndex);
	
				for (int r =0; r<Btilde.size();r++) {
					Belief br = Btilde.get(r);
					double VValue = Math.abs(getPossibleValue(br,V,pomdp));
					if (!getPossibleActionsForBelief(br,pomdp).contains(allActions.get(alpha.getAction()))) {
						mainLog.print("");//mainLog.println("belief does not have this action"+br);
						newB.add(br);
					}
					else {
						double alphaValue = Math.abs(alpha.getDotProduct(br.toDistributionOverStates(pomdp)));
						//ol= AlphaVector.getValue(br.toDistributionOverStates(pomdp),V);
						if (alphaValue < VValue ) {
							newB.add(br);
							mainLog.print("");//mainLog.println("keep"+br);
							mainLog.print("");//mainLog.println("VValue = "+VValue+" alphaValue = "+alphaValue);
						}
						else {
							mainLog.print("");//mainLog.println("remove"+br);
							mainLog.print("");//mainLog.println("VValue = "+VValue+" alphaValue = "+alphaValue);
						}
					}
				}
				Btilde = newB;
				mainLog.print("");//mainLog.println("prune Bsize="+newB.size()+"Vsize="+Vnext.size());
			}
			else {
				//int bestVectorIndex = AlphaVector.getBestVectorIndex(b.toDistributionOverStates(pomdp), V);
				//AlphaVector alphaBest= V.get(bestVectorIndex);
				AlphaVector alphaBest = getBestPossibleAlpha(b, V,pomdp);
				//assert V.get(bestVectorIndex).getAction() >= 0 && V.get(bestVectorIndex).getAction() < pomdp.getMaxNumChoices() : "invalid action: "+V.get(bestVectorIndex).getAction();
				mainLog.print("");//mainLog.println("Best alpha action="+alphaBest.getAction()+Arrays.toString(alphaBest.getEntries()));
	
				if (!Vnext.contains(alphaBest)) {
					Vnext.add(alphaBest);
					mainLog.print("");//mainLog.println("2 Adding vector action ="+alphaBest.getAction());
					mainLog.print("");//mainLog.println("2 Adding vector action ="+Arrays.toString(alphaBest.getEntries()));
				}
				//Btilde.remove(Btilde.indexOf(b));
				Btilde.remove(beliefIndex);
			}
			
			
			// compute new Btilde containing non-improved belief points
			ArrayList<Belief> n = new ArrayList<Belief>();	
			for(Belief bp : B) {
				//double oV = AlphaVector.getValue(bp.toDistributionOverStates(pomdp), V);
				//double nV = AlphaVector.getValue(bp.toDistributionOverStates(pomdp), Vnext);
				//if(nV < oV) 					newBtilde.add(bp);				
			}	
		

			mainLog.print("");//mainLog.println("Btilde"+Btilde.size());
		}
		return Vnext;
	}

	public AlphaVector backup(POMDP pomdp, List<AlphaVector> immediateRewards, AlphaVector[][][] gkao, Belief b, ArrayList<AlphaVector> V) {
		int nStates = pomdp.getNumStates();
		int nActions = pomdp.getMaxNumChoices();
		int nObservations = pomdp.getNumObservations();
		List<AlphaVector> ga = new ArrayList<AlphaVector>();
		
		ArrayList<Object> allActions = getAllActions(pomdp);
		
		ArrayList<Object> possibelActionsForBelief = getPossibleActionsForBelief(b,pomdp);
		
		if (b.so==0) {
			mainLog.print("");//mainLog.print(b.so);
		}
		for(int a=0; a<nActions; a++) {
			if (!possibelActionsForBelief.contains(allActions.get(a))) {
				continue;
			}
			mainLog.print("");//mainLog.println("\ncomputing for action ="+allActions.get(a));
			ArrayList<Integer> possibleObservationsForBeliefAction = getPossibleObservationsForBeliefAction(b, allActions.get(a), pomdp);
						
			List<AlphaVector> oVectors = new ArrayList<AlphaVector>();
			
			for(int o=0; o<nObservations; o++) {
				if (!possibleObservationsForBeliefAction.contains(o)) {
					continue;
				}
				double maxVal = Double.NEGATIVE_INFINITY;
				AlphaVector maxVector = null;

				// Belief bao = ;

				int choice = possibelActionsForBelief.indexOf(allActions.get(a));
				Belief updatedBelief= pomdp.getBeliefAfterChoiceAndObservation(b,choice, o);
				mainLog.print("");//mainLog.println("updatedBelief="+updatedBelief);
				ArrayList<Object> futureActions = getPossibleActionsForBelief(updatedBelief,pomdp);
				
				int K = gkao.length;
				for(int k=0; k<K; k++) {
					if (!futureActions.contains(allActions.get(V.get(k).getAction()))) {
						mainLog.print("");//mainLog.println("Updated belief do not have future action"+V.get(k).getAction());
						continue;
					}
					if( gkao[k][a][o]==null|| gkao[k][a][o].getAction()!=a) { 
						continue;
					}
					double product = gkao[k][a][o].getDotProduct(b.toDistributionOverStates(pomdp));
					if(product > maxVal ) {
						maxVal = product;
						maxVector = gkao[k][a][o];
					}
					mainLog.print("");//mainLog.println("kao="+k+a+o+"->"+product);
					mainLog.print("");//mainLog.println("maxVal"+k+a+o+"->"+maxVal);
				}

				assert maxVector != null;
				if (maxVector==null) 
					continue;
				oVectors.add(maxVector);
				mainLog.print("");//mainLog.println("Action "+a+" Final maxVal->"+maxVal);
				mainLog.print("");//mainLog.println("Action "+a+" Final maxVector action ->"+maxVector.getAction());
				mainLog.print("");//mainLog.println("Action "+a+" Final maxVect value->"+Arrays.toString(maxVector.getEntries()));

			}
			
			if(oVectors.size()==0) {
				continue;
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
	public ArrayList<Integer> getPossibleObservationsForBeliefAction(Belief b, Object a, POMDP pomdp) {
		ArrayList<Integer> possibleObservationsForBeliefAction = new ArrayList<Integer> ();
		
		for (int s =0; s<pomdp.getNumStates();s++) {
			if (b.toDistributionOverStates(pomdp)[s]>0) {
				int choice = pomdp.getChoiceByAction(s, a);
				for (int sPrime =0; sPrime<pomdp.getNumStates();sPrime++) {
					double tranP =0.0;
					Iterator<Entry<Integer, Double>> iter = pomdp.getTransitionsIterator(s,choice);
					while (iter.hasNext()) {
						Map.Entry<Integer, Double> trans = iter.next();
						if (trans.getKey()==sPrime) {
							tranP = trans.getValue();	 
						}
					}
					if (tranP>0) {
						if(!possibleObservationsForBeliefAction.contains(pomdp.getObservation(sPrime)))
							possibleObservationsForBeliefAction.add(pomdp.getObservation(sPrime));
					}					
				}
			}
		}
		return possibleObservationsForBeliefAction;
	}
	public ArrayList<Object> getPossibleActionsForBelief(Belief b, POMDP pomdp) {
		ArrayList <Object> availableActionsForBelief = new ArrayList<Object> ();
		for (int s =0; s<pomdp.getNumStates();s++) {
			if ((b.toDistributionOverStates(pomdp)[s])>0){
				List <Object> availableActionsForState = pomdp.getAvailableActions(s);
				for (Object a: availableActionsForState) {
					if (!availableActionsForBelief.contains(a)) {
						availableActionsForBelief.add(a);
					}
				}
			}
		}
		return availableActionsForBelief;
		
	}
	public ArrayList<Object> getAllActions(POMDP pomdp){
		ArrayList <Object> allActions = new ArrayList<Object> ();
		for (int s =0; s<pomdp.getNumStates();s++) {
			List <Object> availableActionsForState = pomdp.getAvailableActions(s);

			for (Object a: availableActionsForState) {
				if (!allActions.contains(a) & a!= null) {
					allActions.add(a);
				}
			}
		}
		return allActions;
	}
	
	public double getPossibleValue(Belief b, List<AlphaVector> V, POMDP pomdp) {
			boolean min = false; 
			double val = min? Double.POSITIVE_INFINITY: Double.NEGATIVE_INFINITY;
			//double val = min? 777777: -77777777;
			ArrayList <Object> allActions = getAllActions(pomdp);
			ArrayList <Object> availableActionsForBelief = getPossibleActionsForBelief (b, pomdp);
			for (int v=0; v<V.size(); v++) {
				Object action = allActions.get(V.get(v).getAction());
				if (availableActionsForBelief.contains(action)) {
					AlphaVector alpha = V.get(v);
					double product =  alpha.getDotProduct(b.toDistributionOverStates(pomdp));
					//product = Math.abs(product);
					if (min && product <val) {
						val=product;
					}
					if (!min && product>val) {
						val = product;
						mainLog.print("");//mainLog.println("get Possible valueproduct ="+product+" action "+action+ " Values="+Arrays.toString(alpha.getEntries()));

					}
				}
			}
			return val;
	}
	public double getPossibleValue(Belief b, ArrayList<AlphaVector> V, POMDP pomdp) {
		boolean min = false; 
		//double val = min? Double.POSITIVE_INFINITY: Double.NEGATIVE_INFINITY;
		double val = min? 777777: -77777777;
		ArrayList <Object> allActions = getAllActions(pomdp);
		ArrayList <Object> availableActionsForBelief = getPossibleActionsForBelief (b, pomdp);
		for (int v=0; v<V.size(); v++) {
			Object action = allActions.get(V.get(v).getAction());
			if (availableActionsForBelief.contains(action)) {
				AlphaVector alpha = V.get(v);
				double product =  alpha.getDotProduct(b.toDistributionOverStates(pomdp));
				//product = Math.abs(product);
				if (min && product <val) {
					val=product;
				}
				if (!min && product>val) {
					val = product;
					mainLog.print("");//mainLog.println("get Possible value product ="+product+" action "+action+ " Values="+Arrays.toString(alpha.getEntries()));
				}
			}
		}
		return val;
}
	
	public  boolean lexGreater(AlphaVector v1, AlphaVector v2) {
		assert v1.size() == v2.size();
		for(int i=0; i<v1.size(); i++) {
			double v1Entry = v1.getEntry(i);
			double v2Entry = v2.getEntry(i);
			
			v1Entry = Math.abs(v1Entry);
			v2Entry = Math.abs(v2Entry);
			if(v1Entry != v2Entry) {
				if(v1Entry > v2Entry) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}
	
	
	public AlphaVector getBestPossibleAlpha(Belief b, List<AlphaVector> V, POMDP pomdp) {
		boolean min = false; 
		double val = min? Double.POSITIVE_INFINITY: Double.NEGATIVE_INFINITY;
		AlphaVector bestAlpha= null;
		ArrayList <Object> allActions = getAllActions(pomdp);
		ArrayList <Object> availableActionsForBelief = getPossibleActionsForBelief (b, pomdp);
		for (int v=0; v<V.size(); v++) {
			Object action = allActions.get(V.get(v).getAction());
			if (availableActionsForBelief.contains(action)) {
				AlphaVector alpha = V.get(v);
				double product =  alpha.getDotProduct(b.toDistributionOverStates(pomdp));
				mainLog.print("");//mainLog.println("best alpha cand product="+product);
				mainLog.print("");//mainLog.println("best alpha cand getAction="+alpha.getAction());
				mainLog.print("");//mainLog.println("best alpha cand getEntries="+Arrays.toString(alpha.getEntries()));
				if (min && product <val) {
					val=product;
					bestAlpha= alpha;
				}
				
				if (!min ) {
					if(product>val) {
						val = product;
						bestAlpha= alpha;
					}
					else if  (product == val && lexGreater(alpha, bestAlpha)) {
						val = product;
						bestAlpha= alpha;
					}
				}
			}
		}
		return bestAlpha;
	}
	
	public boolean checkConverven (ArrayList<AlphaVector>V, ArrayList<AlphaVector> Vnext, ArrayList<Belief>B, double ValueFunctionTolerance) {
		boolean done = true;
		for (int i=0; i<Vnext.size(); i++) {
			if (!V.contains(Vnext.get(i))) {
				done = false;
			}
		}
		return done;
	}
	
	public ArrayList<Belief> randomExploreBeliefs(POMDP pomdp, BitSet target,  BitSet statesOfInterest) throws PrismException
	{
		// ArrayList<Belief> B  = randomExploreBeliefs(pomdp)
		//doing		ArrayList<BeliefPoint> B = getBeliefPoints(pomdp);
		BitSet targetObs = getObservationsMatchingStates(pomdp, target);
		
		// Check we are only computing for a single state (and use initial state if unspecified)
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet();
			statesOfInterest.set(pomdp.getFirstInitialState());
		} else if (statesOfInterest.cardinality() > 1) {
			throw new PrismNotSupportedException("POMDPs can only be solved from a single start state");
		}	
		
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
		//mainLog.println("target obs=" + targetObs.cardinality() + ", inf obs=" + infObs.cardinality());
		
		// Determine set of observations actually need to perform computation for
		// eg. if obs=1 & unknownObs(obs)=true -> obs=1 needs computation
		// eg. if obs=2 & unknownObs(obs)=false -> obs=1 does not need computation
		BitSet unknownObs = new BitSet();
		unknownObs.set(0, pomdp.getNumObservations());
		unknownObs.andNot(targetObs);
		unknownObs.andNot(infObs);
		
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
								if (!B.contains(bao) & unknownObs.get(bao.so)){
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
			if (!B.contains(corner)& unknownObs.get(corner.so)) {
				B.add(corner);
			}
		}
		return B;
	}
	
	public ModelCheckerResult computeReachRewardsPerseus(POMDP pomdp, MDPRewards mdpRewards, BitSet target, boolean min, BitSet statesOfInterest) throws PrismException
	{ 
		BitSet targetObs = getObservationsMatchingStates(pomdp, target);
		
		// Check we are only computing for a single state (and use initial state if unspecified)
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet();
			statesOfInterest.set(pomdp.getFirstInitialState());
		} else if (statesOfInterest.cardinality() > 1) {
			throw new PrismNotSupportedException("POMDPs can only be solved from a single start state");
		}	
		
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
		//mainLog.println("target obs=" + targetObs.cardinality() + ", inf obs=" + infObs.cardinality());
		
		// Determine set of observations actually need to perform computation for
		// eg. if obs=1 & unknownObs(obs)=true -> obs=1 needs computation
		// eg. if obs=2 & unknownObs(obs)=false -> obs=1 does not need computation
		BitSet unknownObs = new BitSet();
		unknownObs.set(0, pomdp.getNumObservations());
		unknownObs.andNot(targetObs);
		unknownObs.andNot(infObs);
		
		
		int nStates = pomdp.getNumStates();
		mainLog.print("");//mainLog.println(pomdp.getMaxNumChoices());
		ArrayList<Object> allActions = getAllActions(pomdp);
		for (int p =0; p<allActions.size();p++)
			mainLog.print("");//mainLog.println("Action ="+allActions.get(p));
		int nActions = allActions.size();
		// Find out the observations for the target states
		/*for (int so=unknownObs.nextSetBit(0); so>=0; so = unknownObs.nextSetBit(so+1)) 
			mainLog.print("");//mainLog.println("so"+so);
		for (int s=0; s<pomdp.getNumObservations(); s++)
			mainLog.print("");//mainLog.println("obs"+s+unknownObs.get(s));
		*/
		ModelCheckerResult res = null;
		long timer;
		
		mainLog.print("");//mainLog.println("get number of getNumObservations"+pomdp.getNumObservations());
		mainLog.print("");//mainLog.println("get number of getNumUnobservations"+pomdp.getNumUnobservations());
		
	
		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.print("");//mainLog.println("\nStarting expected reachability (" + (min ? "min" : "max") + ")...");
		mainLog.print("");//mainLog.println("=== RUN POMDP SOLVER ===");
		mainLog.print("");//mainLog.println("Algorithm: Perseus (point-based value iteration)");
		mainLog.print("");//mainLog.println("Belief sampling started...");
		// Compute rewards
		mainLog.print("");//mainLog.println("NumeStates/nActions"+nStates+nActions);

		 ArrayList<Belief> B  = randomExploreBeliefs(pomdp, target, statesOfInterest);
		 
//doing		ArrayList<BeliefPoint> B = getBeliefPoints(pomdp);
		/*
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
								if (!B.contains(bao) & unknownObs.get(bao.so)){
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
			if (!B.contains(corner)& unknownObs.get(corner.so)) {
				B.add(corner);
			}
		}
		mainLog.print("");//mainLog.println("Perseus Number of beliefs: "+B.size());
		*/
		mainLog.print("");//mainLog.println("Defining immediate rewards ");
						
		// create initial vector set and vectors defining immediate rewards
		ArrayList<AlphaVector> V = new ArrayList<AlphaVector>();
		ArrayList<AlphaVector> immediateRewards = new ArrayList<AlphaVector>();
		

		double minMax = min? -1: 1; //negate reward for min problems
		
		
		for(int a=0; a<nActions; a++) {
			double[] entries = new double[nStates];
			for(int s=0; s<nStates; s++) {
				//entries[s] = pomdp.getReward(s, a); original
				Object action = allActions.get(a);
			
				entries[s] = 0;

				if (pomdp.getAvailableActions(s).contains(action)) {							
					int choice =  pomdp.getChoiceByAction(s, action);
					mainLog.print("");//mainLog.println("state="+s+"action="+action+"tranReward"+mdpRewards.getTransitionReward(s, choice)+"stagereward="+mdpRewards.getStateReward(s) );
					entries[s] +=  minMax*(mdpRewards.getTransitionReward(s, choice)+mdpRewards.getStateReward(s)) ;
				}
				//else {
				//	if (unknownObs.get(pomdp.getObservation(s)))
				//		entries[s]=-99999;
				//}
			}
			AlphaVector av = new AlphaVector(entries);
			av.setAction(a);
			immediateRewards.add(av);
		}
		for (int v=0; v<immediateRewards.size();v++) {
			mainLog.print("");//mainLog.print(v+" immediate Action = "+ immediateRewards.get(v).getAction());
			mainLog.print("");//mainLog.println( "  value = "+ Arrays.toString(immediateRewards.get(v).getEntries()));
		}
		for(int a=0; a<nActions; a++) {
			double[] entries = new double[nStates];
			for(int s=0; s<nStates; s++) {
				//entries[s] = pomdp.getReward(s, a); original
				Object action = allActions.get(a);
			
				entries[s] = 0;

				if (pomdp.getAvailableActions(s).contains(action)) {							
					int choice =  pomdp.getChoiceByAction(s, action);
					entries[s] += minMax*( mdpRewards.getTransitionReward(s, choice)+mdpRewards.getStateReward(s) );
				}
				//else {
				//	if (unknownObs.get(pomdp.getObservation(s)))
				//		entries[s]=-99999;
				//}
				
			}
			AlphaVector av = new AlphaVector(entries);
			av.setAction(a);
			V.add(av);
		}
		
		for (int v=0; v<V.size();v++) {
			mainLog.print("");//mainLog.print(v+" V Action = "+ V.get(v).getAction());
			mainLog.print("");//mainLog.println( "   value = "+ Arrays.toString(V.get(v).getEntries()));
		}
		
		int stage = 1;

		System.out.println("Stage 1: "+V.size()+" vectors");
		//
		//OutputFileWriter.dumpValueFunction(pomdp, V, sp.getOutputDir()+"/"+pomdp.getInstanceName()+".alpha"+stage, sp.dumpActionLabels());
		
		// run the backup stages
		long startTime = System.currentTimeMillis();
		double ValueFunctionTolerance = 1E-06;

		
		while(true) {
			mainLog.print("");//mainLog.println("$$$"+stage);
			for (int i =0; i<V.size();i++) {
				AlphaVector a = V.get(i);
				mainLog.print("");//mainLog.print(stage+" "+ a.getAction());
				for (Belief br : B) {
					if (getPossibleActionsForBelief(br, pomdp).contains(allActions.get(a.getAction()))) {
						
						double v = a.getDotProduct(br.toDistributionOverStates(pomdp));
						mainLog.print("");//mainLog.print(" "+v);
					}
					else {
						mainLog.print("");//mainLog.print(" NaN");
					}
				}
				mainLog.print("");//mainLog.println(" ");
			}
			mainLog.print("");//mainLog.println("$$$"+stage);

			stage++;
			//todo

			 ArrayList<AlphaVector> Vnext = backupStage(pomdp, immediateRewards, V, B, unknownObs);
			 
			 if (checkConverven (V, Vnext, B, ValueFunctionTolerance)) {
				 //mainLog.print("");//mainLog.println("Done");
				 break;
			 }
				 
			 
			double valueDifference = Double.NEGATIVE_INFINITY;
			
			ArrayList <Double> V_value = new ArrayList<Double> ();
			ArrayList <Double> VN_value = new ArrayList<Double> ();
			for(Belief bel : B) {
				//double [] belief = bel.toDistributionOverStates(pomdp);
				//todo
				//double diff = AlphaVector.getValue(belief, Vnext) - AlphaVector.getValue(belief, V);
				V_value.add(getPossibleValue(bel, V,pomdp));
				VN_value.add(getPossibleValue(bel, V,pomdp));
				mainLog.print("");//mainLog.println(bel.toDistributionOverStates(pomdp));
				//mainLog.print("");//mainLog.println("V"+getPossibleValue(bel, V,pomdp));
				//mainLog.print("");//mainLog.println("Vnext"+getPossibleValue(bel, Vnext,pomdp));
				double diff = Math.abs(getPossibleValue(bel, Vnext,pomdp) - getPossibleValue(bel, V,pomdp));
				if(diff > valueDifference) valueDifference = diff;
			}
			

			double elapsed = (System.currentTimeMillis() - startTime) * 0.001;
			V = Vnext;
			mainLog.print("");//mainLog.println("Stage "+stage+": "+Vnext.size()+" vectors, diff "+valueDifference+", time elapsed "+elapsed+" sec");
			for (int v=0; v<V.size();v++) {
				mainLog.print("");//mainLog.print(v+" V Action = "+ V.get(v).getAction());
				mainLog.print("");//mainLog.println( "   value = "+ Arrays.toString(V.get(v).getEntries()));
			}
			//OutputFileWriter.dumpValueFunction(pomdp, V, sp.getOutputDir()+"/"+pomdp.getInstanceName()+".alpha"+stage, sp.dumpActionLabels());
			
			double elapsedTime = (System.currentTimeMillis() - startTime) * 0.001;
			
			double TimeLimit =1000.0;
			//if(valueDifference < ValueFunctionTolerance || elapsedTime > TimeLimit) {
			//	break;
			//}
		}
		
		long totalSolveTime = (System.currentTimeMillis() - startTime);
		double expectedValue = Math.abs(getPossibleValue(pomdp.getInitialBelief(),V,pomdp));
		
		//String outputFileAlpha = sp.getOutputDir()+"/"+pomdp.getInstanceName()+".alpha";
		//OutputFileWriter.dumpValueFunction(pomdp, V, outputFileAlpha, sp.dumpActionLabels());
		
		
		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("");//mainLog.println("*********Value" +expectedValue );
		mainLog.print("");//mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

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
	
	
	/**
	 *  make a copy of AlphaMatrix set
	 *  @param A AlphaMatrix Set
	 *  @return a copy of AlphaMatrix Set
	*/
	public ArrayList<AlphaMatrix> copyAlphaMatrixSet(ArrayList<AlphaMatrix> A){
		ArrayList<AlphaMatrix> B = new ArrayList<AlphaMatrix>();
		
		for (int i =0; i<A.size(); i++) {
			AlphaMatrix alphaMatrix = A.get(i);
			AlphaMatrix alphaMatrix_copy = new AlphaMatrix(alphaMatrix.getMatrix());
			alphaMatrix_copy.setAction(alphaMatrix.getAction());
			B.add(alphaMatrix_copy);
		}
		return B;
	}
	/*
    public double  valueMOPOMDP (Belief b, POMDP pomdp , ArrayList<AlphaVector> A, ArrayList<Double> w)
    {
		ArrayList<Object>  allActions = getAllActions(pomdp);
		ArrayList<Object> availableActions = getPossibleActionsForBelief(b, pomdp);
		
		int actionIndex = A.get(0).getAction();
		Object action = allActions.get(actionIndex);
		if(!availableActions.contains(action)) {
			mainLog.println("action not available");
			return Double.NEGATIVE_INFINITY;
		}
		double result = 0.0;
		for (int i=0; i<w.size(); i++){
			result += ((double) w.get(i)) * ( A.get(i).getDotProduct(b.toDistributionOverStates(pomdp))) ;
		}
		return result;
    }
    
	public Pair<Integer, Double> max_MOPOMDP_value(Belief b, ArrayList<ArrayList<AlphaVector>> A, ArrayList<Double> w, POMDP pomdp) 
	{
		ArrayList<Object>  allActions = getAllActions(pomdp);
		ArrayList<Object> availableActions = getPossibleActionsForBelief(b, pomdp);
		int bestAlphaMatrixIndex = -1;
		double value = Double.NEGATIVE_INFINITY;
		for (int i=0; i<A.size(); i++) {
			ArrayList<AlphaVector> am = A.get(i);
			int actionIndex = am.get(0).getAction();
			Object action = allActions.get(actionIndex);
			if(!availableActions.contains(action)) {
				//mainLog.println("action not available for belief");
				continue;
			}
			double val = valueMOPOMDP(b, pomdp, am, w);
			if (val>value) {
				value =val; 
				bestAlphaMatrixIndex = i;
			}
		}
		return new Pair<Integer, Double> (bestAlphaMatrixIndex, value);
	}
	*/
	public ArrayList<Belief> copyBeliefSet(ArrayList<Belief> B){
		ArrayList<Belief> B_copy = new ArrayList<Belief> ();
		for (int i=0; i<B.size(); i++) {
			Belief b_copy = B.get(i);
			B_copy.add(b_copy);
		}
		return B_copy;
	}
	
	public AlphaMatrix backupStageMO(ArrayList<AlphaMatrix> A, Belief b, double [] weights, POMDP pomdp, ArrayList<AlphaMatrix> immediateRewards, AlphaMatrix [][][] gkao)
	{
		
		int nStates = pomdp.getNumStates();
		int nActions = pomdp.getMaxNumChoices();
		int nObservations = pomdp.getNumObservations();
		ArrayList<AlphaMatrix> ga = new ArrayList<AlphaMatrix>();
		ArrayList<Object> allActions = getAllActions(pomdp);
		ArrayList<Object> possibelActionsForBelief = getPossibleActionsForBelief(b,pomdp);
		
		for (int a=0; a<nActions; a++) {
			if (!possibelActionsForBelief.contains(allActions.get(a))) {
				continue;
			}
			ArrayList<Integer> possibleObservationsForBeliefAction = getPossibleObservationsForBeliefAction(b, allActions.get(a), pomdp);
			ArrayList<AlphaMatrix> oMatrices = new ArrayList<AlphaMatrix>();
			for (int o=0; o<nObservations; o++) {
				if (!possibleObservationsForBeliefAction.contains(o)) {
					continue;
				}
				double maxVal = Double.NEGATIVE_INFINITY;
				AlphaMatrix maxMatrix = null;
				int choice = possibelActionsForBelief.indexOf(allActions.get(a));
				Belief updatedBelief= pomdp.getBeliefAfterChoiceAndObservation(b,choice, o);
				ArrayList<Object> futureActions = getPossibleActionsForBelief(updatedBelief, pomdp);

				int K = gkao.length;
				for(int k=0; k<K; k++) {
					if (!futureActions.contains(allActions.get(A.get(k).getAction()))) {
						continue;
					}
					if( gkao[k][a][o]==null|| gkao[k][a][o].getAction()!=a) { 
						continue;
					}

					//double product = gkao[k][a][o].getMaxValue(b, oMatrices, weights, pomdp) // .getDotProduct(b.toDistributionOverStates(pomdp));
					double product= gkao[k][a][o].value(b, weights, pomdp);

					if(product > maxVal ) {
						maxVal = product;
						maxMatrix= gkao[k][a][o];
					}
					//mainLog.println("kao====================="+k+a+o);
					//mainLog.println("Belief="+Arrays.toString(b.toDistributionOverStates(pomdp)));
					//mainLog.println("gkao="+gkao[k][a][o]);
					//mainLog.println("product="+product);
					//mainLog.println("maxVal="+maxVal);
					
				}
				
				if (maxMatrix==null) {
					continue;
				}
				oMatrices.add(maxMatrix.clone());
			}
			if (oMatrices.size()==0) {
				continue;
			}

			AlphaMatrix sumMatrix = oMatrices.get(0).clone();
			//mainLog.println(0+ "OMatrices = "+oMatrices.get(0));
			for (int j =1; j<oMatrices.size();j++) {
				sumMatrix = AlphaMatrix.sumMatrices(sumMatrix, oMatrices.get(j));
				//mainLog.println(j+ "OMatrices = "+oMatrices.get(j));
			}
			//mainLog.println("Sum= "+sumMatrix);
			
			AlphaMatrix am = AlphaMatrix.sumMatrices(immediateRewards.get(a), sumMatrix);
			am.setAction(a);
			ga.add(am.clone());
		}
		
		int bestAlphaMatrixIndex = AlphaMatrix.getMaxValueIndex(b, ga, weights, pomdp);

		AlphaMatrix bestAlphaMatrix = ga.get(bestAlphaMatrixIndex);

		return bestAlphaMatrix;
	}
	
	public AlphaMatrix [][][] cacheGKao(ArrayList<AlphaMatrix> V, POMDP pomdp){
		ArrayList<Object> allActions =getAllActions(pomdp);
		int nActions = allActions.size();
		int nObservations = pomdp.getNumObservations();
		int nObjectives= V.get(0).getNumObjectives();
		int	nStates = pomdp.getNumStates();
		// Eq.9 Initial GAO
		AlphaMatrix[][][] gkao = new AlphaMatrix[V.size()][nActions][nObservations];
		for (int k=0; k<V.size(); k++) {
			for (int a=0; a<nActions; a++) {
				for (int o=0; o<nObservations; o++) {
					double[][] matrix = new double[nStates][nObjectives];
					for (int s=0; s<nStates; s++)	{
						double[] val = new double [nObjectives];
						Object action = allActions.get(a);
						List<Object> availableActions= pomdp.getAvailableActions(s);
						if (availableActions.contains(action)) {
							for(int sPrime=0; sPrime<nStates; sPrime++) {
								double[] value = V.get(k).getValues(sPrime);
								double obsP = 0.0;// (a, sPrime, o);
								obsP = pomdp.getObservationProb(sPrime, o);
								double tranP=0.0;
								int choice = pomdp.getChoiceByAction(s, action);
								Iterator<Entry<Integer, Double>> iter = pomdp.getTransitionsIterator(s,choice);
								while (iter.hasNext()) {
									Map.Entry<Integer, Double> trans = iter.next();
									if (trans.getKey()==sPrime) {
										tranP = trans.getValue();	 
									}
								}
								for (int v=0; v<val.length; v++) {
									val[v] += value[v]* obsP *tranP;
								}
							}
						}
						for (int v=0; v<val.length; v++) {
							matrix[s][v]=val[v];
						}
					}
					AlphaMatrix am = new AlphaMatrix(matrix);
					am.setAction(a);
					gkao[k][a][o] = am;
					//mainLog.print(k+" "+a+" "+o+" "+ am);

				}
			}
		}
		return gkao;
	}
	public ArrayList<AlphaMatrix> solveScalarizedPOMDP(ArrayList<AlphaMatrix> A, ArrayList<Belief> B, double [] weights, double eta, POMDP pomdp, ArrayList<AlphaMatrix> immediateRewards, ArrayList<AlphaMatrix> V)
	{
		mainLog.println("calling solve scalarized POMDP");
		//mainLog.println("Weights="+Arrays.toString(weights));
		ArrayList<AlphaMatrix> Aprime =  copyAlphaMatrixSet (A); // L1
		//L2
		for (int i=0; i<A.size(); i++) {
			AlphaMatrix am = A.get(i);
			double[][] matrix = new double [am.getNumStates()][am.getNumObjectives()];
			for (int s=0; s<am.getNumStates();s++) {
				for (int obj=0; obj<am.getNumObjectives();obj++) {
					matrix [s][obj] = -999;//Double.NEGATIVE_INFINITY;
				}
			}
			am.setMatrix(matrix);
		}
		
		Random rnd = new Random();
		
		int count=0;
		while(true) {
			//line 3
			count+=1;
			double diff = Double.NEGATIVE_INFINITY;
			for( Belief b : B){
				double value_Aprime = AlphaMatrix.getMaxValue(b, Aprime, weights, pomdp);
				double value_A= AlphaMatrix.getMaxValue(b, A, weights, pomdp);
				if (value_Aprime-value_A > diff) {
					diff = value_Aprime-value_A;
				}
			}
			mainLog.println(count+"dif="+diff);
			if (diff <= eta) {
				mainLog.println("break");
				break;
			}
			
			//Line 4
			A = copyAlphaMatrixSet(Aprime);
			Aprime = new ArrayList<AlphaMatrix> ();
			/*
			mainLog.println("AAAAAAAAAAAAAAstage+"+count);
			for (int i=0; i<A.size();i++) {
				mainLog.println(A.get(i));
			}
			mainLog.println("A'''''''''''''''''''stage+"+count);
			for (int i=0; i<Aprime.size();i++) {
				mainLog.println(Aprime.get(i));
			}
			 */
			
			ArrayList<Belief> Bprime = copyBeliefSet(B);
			
			AlphaMatrix [][][] gkao = cacheGKao(A, pomdp);
			
			//Line 5
			while (Bprime.size()>0) {
				//Line 6 get random belief
				int beliefIndex = rnd.nextInt(Bprime.size());
				Belief b = Bprime.get(beliefIndex);
				Bprime.remove(beliefIndex);
				//Line 7 Backup AlphaMatrixSet belief weights 

				//mainLog.println("ready to back up for "+b);
				AlphaMatrix Am = backupStageMO (A, b, weights, pomdp, immediateRewards, gkao);
				double newValue= Am.value(b, weights, pomdp);
				double oldValue = AlphaMatrix.getMaxValue(b, A, weights, pomdp);
				//mainLog.println("new value="+Am.value(b, weights, pomdp));
				//mainLog.println("old value="+AlphaMatrix.getMaxValue(b, A, weights, pomdp));

				//Line 8 update A'
				ArrayList<AlphaMatrix> A_tp = copyAlphaMatrixSet(A);
				A_tp.add(Am);
				int bestAlphaMatrixIndex = AlphaMatrix.getMaxValueIndex(b, A_tp, weights, pomdp);
				AlphaMatrix bestAlphaMatrix = A_tp.get(bestAlphaMatrixIndex);
				//mainLog.println("best="+bestAlphaMatrix);
				if (!AlphaMatrix.contains(Aprime, bestAlphaMatrix) ) {
					Aprime.add(bestAlphaMatrix);
				}
				
				if(newValue>oldValue) {
					//Line 9 update Belief set
					ArrayList<Belief> B_new = new ArrayList<Belief> ();
					for (Belief br : Bprime) {
						if ( AlphaMatrix.getMaxValue(br, Aprime, weights, pomdp) < AlphaMatrix.getMaxValue(br, A, weights, pomdp) ) {
							B_new.add(br);
						}
					}
					Bprime = B_new;
				}
				
				//mainLog.println("B size="+Bprime.size());
				//mainLog.println("A size="+Aprime.size());
			}
		}
		
		return Aprime;
	}
	
	public ModelCheckerResult computeReachRewards(POMDP pomdp, MDPRewards mdpRewards, BitSet target, boolean min, BitSet statesOfInterest) throws PrismException
	{
		mainLog.print("");//mainLog.println("TESSsSSSSSsssSSSSST");
		computeReachRewardsPerseus( pomdp,  mdpRewards,  target,  min,  statesOfInterest);
		mainLog.print("");//mainLog.println("TESSsSSSSSsssSSSSST");
		
		
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
		mainLog.print("");//mainLog.println("\nStarting expected reachability (" + (min ? "min" : "max") + ")...");

		// Compute rewards
		res = computeReachRewardsFixedGrid(pomdp, mdpRewards, target, min, statesOfInterest.nextSetBit(0));

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("");//mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

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
		/*
		for(int q=0; q<gridPoints.size();q++) {
			mainLog.println(q);
			mainLog.println("index"+gridPoints.get(q).so);
			mainLog.println(gridPoints.get(q).bu);
		}*/
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
		/*
		mainLog.println("vhash");
		for (Object key:  vhash.keySet()) {
			mainLog.println(key);
			mainLog.println(vhash.get(key));
		}
		mainLog.println("vhash_backup");
		for (Object key:  vhash_backUp.keySet()) {
			mainLog.println(key);
			mainLog.println(vhash_backUp.get(key));
		}*/
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


