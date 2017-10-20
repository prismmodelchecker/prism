//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.function.IntPredicate;

import common.StopWatch;
import common.IterableBitSet;
import explicit.IncomingChoiceRelation.Choice;
import explicit.rewards.MDPRewards;
import prism.PrismComponent;

/**
 * An implementation of the upper bound computation for Rmin as detailed in the
 * paper McMahan, Likhachev, Gordon "Bounded Real-Time Dynamic Programming:
 * RTDP with monotone upper bounds and performance guarantees" (International
 * Conference on Machine Learning, 2005).
 * */
public class DijkstraSweepMPI {

	private static class QueueEntry implements Comparable<QueueEntry> {
		public int y;
		public double p;
		public double w;

		public QueueEntry(int y, double p, double w)
		{
			this.y = y;
			this.p = p;
			this.w = w;
		}

		@Override
		public int compareTo(QueueEntry o)
		{
			int r = Double.compare(p, o.p);
			if (r == 0) {
				return Double.compare(w, o.w);
			} else {
				return r;
			}
		}
	}

	private static class ChoiceValues {
		public double p;
		public double w;

		public ChoiceValues(double p, double w)
		{
			this.p = p;
			this.w = w;
		}
	}

	private static boolean debug = false;
	private MDP mdp;
	private MDPRewards rewards;
	private PriorityQueue<QueueEntry> queue;
	private double[] pState;
	private double[] wState;
	private HashMap<Choice, ChoiceValues> choiceValues = new HashMap<Choice, ChoiceValues>();
	private QueueEntry[] pri;
	private int[] pi;
	private BitSet unknown, target;
	private BitSet fin = new BitSet();
	private IncomingChoiceRelation incoming;
	private double lambda;

	private DijkstraSweepMPI(PrismComponent parent, MDP mdp, MDPRewards rewards, BitSet target, BitSet unknown)
	{
		this.mdp = mdp;
		this.unknown = unknown;
		this.target = target;
		this.rewards = rewards;

		incoming = IncomingChoiceRelation.forModel(parent, mdp);

		queue = new PriorityQueue<QueueEntry>();
		pState = new double[mdp.getNumStates()];
		wState = new double[mdp.getNumStates()];
		pri = new QueueEntry[mdp.getNumStates()];
		pi = new int[mdp.getNumStates()];

		for (int s : IterableBitSet.getSetBits(unknown)) {
			for (int choice = 0, numChoices = mdp.getNumChoices(s); choice < numChoices; choice++) {
				Choice c = new Choice(s, choice);
				double rew = rewards.getStateReward(s);
				rew += rewards.getTransitionReward(s, choice);
				choiceValues.put(c, new ChoiceValues(0.0, rew));
			}
		}

		for (int s : IterableBitSet.getSetBits(target)) {
			pState[s] = 1.0;
		}

		HashSet<Choice> preTarget = new HashSet<Choice>();
		for (int t : IterableBitSet.getSetBits(target)) {
			for (Choice c : incoming.getIncomingChoices(t)) {
				boolean newChoice = preTarget.add(c);
				if (newChoice) {
					if (!unknown.get(c.getState())) {
						continue;
					}
					if (!validChoice(c)) {
						continue;
					}

					update(c, target);
				}
			}
		}
		preTarget.clear();

		sweep();

		computeLambda();
	}

	private void sweep()
	{
		while (!queue.isEmpty()) {
			int x = queue.poll().y;
			if (fin.get(x)) {
				// already handled
				continue;
			}

			fin.set(x);
			ChoiceValues v = choiceValues.get(new Choice(x, pi[x]));
			wState[x] = v.w;
			pState[x] = v.p;

			for (Choice c : incoming.getIncomingChoices(x)) {
				if (fin.get(c.getState())) {
					// already handled, skip
					continue;
				}

				if (!unknown.get(c.getState())) {
					// uninteresting state
					continue;
				}

				if (!validChoice(c)) {
					// some successor go outside unknown U target (e.g., to some infinity or undefined state)
					// skip
					continue;
				}

				// a relevant choice, update
				update(c, x);
			}
		}
	}

	private boolean validChoice(Choice choice)
	{
		IntPredicate outsideRelevant = (int t) -> {
			if (unknown.get(t) || target.get(t)) return false;
			return true;
		};

		return !mdp.someSuccessorsMatch(choice.getState(), choice.getChoice(), outsideRelevant);
	}

	private void update(Choice choice, int x)
	{
		double w_x = wState[x];
		// compute P^a_yx * w(x)
		double Pw = mdp.sumOverTransitions(choice.getState(), choice.getChoice(), (int s, int t, double p) -> {
			if (t != x) return 0.0;
			return p * w_x;
		});

		double p_x = pState[x];
		// compute P^a_yx * p_g(x)
		double Pp = mdp.sumOverTransitions(choice.getState(), choice.getChoice(), (int s, int t, double p) -> {
			if (t != x) return 0.0;
			return p * p_x;
		});

		ChoiceValues c = choiceValues.get(choice);
		assert(c != null);
		c.p += Pp;
		c.w += Pw;

		QueueEntry newPri = new QueueEntry(choice.getState(), 1 - c.p, c.w);
		if (pri[choice.getState()] == null || newPri.compareTo(pri[choice.getState()]) < 0) {
			pri[choice.getState()] = newPri;
			pi[choice.getState()] = choice.getChoice();
			queue.add(newPri);
		}
	}

	private void update(Choice choice, BitSet target)
	{
		// compute P^a_y->target
		double Pp = mdp.sumOverTransitions(choice.getState(), choice.getChoice(), (int s, int t, double p) -> {
			if (target.get(t)) return p;
			return 0.0;
		});

		ChoiceValues c = choiceValues.get(choice);
		c.p += Pp;

		QueueEntry newPri = new QueueEntry(choice.getState(), 1 - c.p, c.w);
		if (pri[choice.getState()] == null || newPri.compareTo(pri[choice.getState()]) < 0) {
			pri[choice.getState()] = newPri;
			pi[choice.getState()] = choice.getChoice();
			queue.add(newPri);
		}
	}

	private double computeLambda()
	{
		lambda = 0.0;

		for (int x : IterableBitSet.getSetBits(unknown)) {
			int a = pi[x];
			double lambda_x_a = Double.POSITIVE_INFINITY;

			// check condition (I)
			double I_sum = mdp.sumOverTransitions(x, a, (int s, int t, double p) -> {
				return p * pState[t];
			});

			if (pState[x] < I_sum) {
				// condition (I) holds
				double den = rewards.getStateReward(x) + rewards.getTransitionReward(x, a); // c(x,a)
				den += mdp.sumOverTransitions(x, a, (int s, int t, double p) -> {
					return p * wState[t];
				});
				den -= wState[x];

				double num = mdp.sumOverTransitions(x, a, (int s, int t, double p) -> {
					return p * pState[t];
				});
				num -= pState[x];

				lambda_x_a = den / num;
			} else {
				// TODO: check condition (II)
				lambda_x_a = 0;
			}

			lambda = Double.max(lambda, lambda_x_a);
		}

		return lambda;
	}

	public static double[] computeUpperBounds(PrismComponent parent, MDP mdp, MDPRewards rewards, BitSet target, BitSet unknown)
	{
		StopWatch timer = new StopWatch(parent.getLog());
		timer.start("computing upper bound(s) for Rmin using the DSI-MP algorithm");

		parent.getLog().println("Computing upper bound(s) for Rmin using the Dijkstra Sweep for Monotone Pessimistic Initialization algorithm...");
		double[] upperBounds = new double[mdp.getNumStates()];
		DijkstraSweepMPI dsmpi = new DijkstraSweepMPI(parent, mdp, rewards, target, unknown);

		for (int x : IterableBitSet.getSetBits(unknown)) {
			upperBounds[x] = dsmpi.wState[x] + dsmpi.lambda*(1 - dsmpi.pState[x]);
		}

		if (debug) {
			parent.getLog().println(upperBounds);
		}

		timer.stop();
		return upperBounds;
	}

	public static double computeUpperBound(PrismComponent parent, MDP mdp, MDPRewards rewards, BitSet target, BitSet unknown)
	{
		double bound = 0.0;
		final double[] upperBounds = computeUpperBounds(parent, mdp, rewards, target, unknown);
		for (int s : IterableBitSet.getSetBits(unknown)) {
			bound = Double.max(bound, upperBounds[s]);
		}
		return bound;
	}
}
