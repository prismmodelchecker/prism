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


package automata;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import jltl2ba.APElement;
import jltl2ba.LTLFragments;
import jltl2ba.MyBitSet;
import jltl2ba.SimpleLTL;
import jltl2dstar.NBA;
import prism.PrismComponent;
import prism.PrismDevNullLog;
import prism.PrismException;
import acceptance.AcceptanceBuchi;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceReach;

import common.IterableBitSet;

import explicit.LTS;
import explicit.LTSSimple;
import explicit.NonProbModelChecker;
import explicit.SCCComputer;
import explicit.SCCConsumerStore;

/**
 * <p>
 * Construction of weak Deterministic Büchi Automata for
 * a fragment of LTL, based on the algorithm presented in
 * Christian Dax, Jochen Eisinger, Felix Klaedtke:
 * <a href="https://doi.org/10.1007/978-3-540-75596-8_17">
 * "Mechanizing the Powerset Construction for Restricted
 *  Classes of omega-Automata"</a>,
 * ATVA 2007, LNCS 4762.
 * </p>
 * <p>
 * The approach works as follows: First, we construct a
 * DBA via a standard power set construction of the NBA
 * for the LTL formula. Then, for the suitable fragment of LTL,
 * it is guaranteed that either all states in a SCC in the DA
 * can be made accepting or all can be made non-accepting (the
 * "weakness" property of the DBA).
 * </p>
 * <p>
 * Using the algorithm described in
 * Christof Löding:
 * <a href="https://doi.org/10.1016/S0020-0190(00)00183-6">
 * "Efficient minimization of deterministic weak omega-automata"</a>,
 * Inf. Process. Lett. 79(3), 2001,
 * it is then possible to obtain a minimal automaton for the language
 * via a DFA-based minimisation.
 * </p>
 * <p>
 * This minimisation step is not yet included, but will
 * be added in the future. Currently, we are using this
 * construction to obtain DAs with AcceptanceReach that
 * are suitable for the model checking of co-safety rewards.
 * For further details, see Sec. 4 of
 * Joachim Klein et al:
 * <a href="https://doi.org/10.1007/s10009-017-0456-3">
 * "Advances in probabilistic model checking with PRISM:
 *  variable reordering, quantiles and weak deterministic
 *  Büchi automata"</a>, International Journal on Software
 *  Tools for Technology Transfer, 2017.
 * </p>
 * <p>TODO: Optimise for performance</p>
 */
public class LTL2WDBA extends PrismComponent
{
	/** Constructor */
	public LTL2WDBA(PrismComponent parent)
	{
		super(parent);
	}

	/**
	 * For a co-safe LTL formula, construct a DFA (i.e., a DA with AcceptanceReach)
	 * with the additional property that every state that accepts the full language
	 * is accepting, i.e., that state s in F iff s |= Sigma^omega.
	 * <br>
	 * Note: Does not check if the formula is actually syntactically cosafe, it is the responsibility
	 * of the caller to ensure that the formula corresponds to a cosafe language.
	 * @param ltl the ltl formula, has to
	 * @return a DA with AcceptanceReach
	 */
	public DA<BitSet, AcceptanceReach> cosafeltl2dfa(SimpleLTL ltl) throws PrismException
	{
		// construct DBA using the powerset-based construction
		DA<BitSet, AcceptanceBuchi> wdba = ltl2wdba(ltl);

		BitSet F = wdba.getAcceptance().getAcceptingStates();
		BitSet notF = (BitSet) F.clone();
		notF.flip(0, wdba.size());

		// Now, to satisfy that s in F iff s |= Sigma^omega, we determine
		// the set of states that can not avoid reaching an F state, as
		// those are the states that will accept any word
		LTS lts = new LTSFromDA(wdba);
		NonProbModelChecker ctlMC = new NonProbModelChecker(this);
		ctlMC.setLog(new PrismDevNullLog());  // quietly
		BitSet canAvoidF = ctlMC.computeExistsGlobally(lts, notF);
		BitSet canNotAvoidF = (BitSet)canAvoidF.clone();
		canNotAvoidF.flip(0, wdba.size());

		// As the result, we construct the DFA on the transition structure
		// of the wdba, with goal states = canNotAvoidF
		DA<BitSet, AcceptanceReach> dfa = toDFA(wdba, canNotAvoidF);
		//dfa.printDot(System.out);
		return dfa;
	}

	/**
	 * For an LTL formula in the obligation fragment, return a WDBA.
	 * @param ltl the LTL formula
	 * @return a weak deterministic Büchi automaton
	 */
	public DA<BitSet, AcceptanceBuchi> obligation2wdba(SimpleLTL ltl) throws PrismException
	{
		return ltl2wdba(ltl);
	}

	/**
	 * Generate DFA (i.e., DA with AcceptanceReach) with the underlying transition
	 * structure of {@code da}. The set of goal states should be upward closed,
	 * i.e., once a goal state has been reached, all successor states should
	 * be goal states as well.
	 * <br>
	 * Note: {@code da} is destroyed during the transformation
	 * and should not be used afterwards.
	 * @param da the DA
	 * @param goalStates the set of goalStates for the DFA.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private DA<BitSet, AcceptanceReach> toDFA(DA<BitSet, ? extends AcceptanceOmega> da, BitSet goalStates)
	{
		AcceptanceReach reach = new AcceptanceReach(goalStates);
		DA.switchAcceptance(da, reach);
		return (DA<BitSet, AcceptanceReach>) da;
	}

	/**
	 * Perform the power set construction as described in
	 * "Mechanizing the Powerset Construction for Restricted Classes of omega-Automata",
	 * and determine whether the SCCs of the resulting automaton should be accepting
	 * or non-accepting.
	 * <br>
	 * The resulting WDBA is only equivalent to the language of the LTL formula
	 * if the language of the LTL formula is WDBA-realizable.
	 * @param ltl the LTL formula.
	 * @return a weak deterministich Büchi automaton.
	 */
	private DA<BitSet, AcceptanceBuchi> ltl2wdba(SimpleLTL ltl) throws PrismException
	{
		ltl = ltl.simplify();

		NBA nba = ltl.toNBA();
		PowersetDA P = powersetConstruction(nba);
		determineF(P);

		return P.da;
	}

	/**
	 * Storage for a DA resulting from the powerset
	 * construction on an NBA;
	 */
	private static class PowersetDA
	{
		/** The original NBA */
		public NBA nba;

		/** The DBA */
		public DA<BitSet, AcceptanceBuchi> da;

		/**
		 * The set of states in the DBA where at least
		 * one of the corresponding NBA states is accepting.
		 */
		public MyBitSet powersetOneF;

		/**
		 * The set of states in the DBA where all
		 * of the corresponding NBA states are accepting.
		 */
		public MyBitSet powersetAllF;

		/** Mapping DBA state index to the set of NBA states */
		public ArrayList<BitSet> idToState;

		/** The accepting states of the DBA */
		public BitSet F = new BitSet();
	}

	/** Perform powerset construction on the NBA */
	private PowersetDA powersetConstruction(NBA nba) throws PrismException
	{
		//System.out.println("--- NBA ---");
		//nba.print_hoa(System.out);
		DA<BitSet, AcceptanceBuchi> da = new DA<BitSet, AcceptanceBuchi>();
		da.setAcceptance(new AcceptanceBuchi());

		HashMap<BitSet,Integer> stateToId = new HashMap<BitSet,Integer>();
		ArrayList<BitSet> idToState = new ArrayList<BitSet>();

		MyBitSet nbaF = nba.getFinalStates();
		MyBitSet powersetOneF = new MyBitSet();
		MyBitSet powersetAllF = new MyBitSet();

		Queue<Integer> todo = new LinkedList<Integer>();
		BitSet initialState = new BitSet();
		initialState.set(nba.getStartState().getName());
		int initialId = da.addState();
		stateToId.put(initialState, initialId);
		idToState.add(initialState);
		todo.add(initialId);
		da.setStartState(initialId);
		da.setAPList(new ArrayList<String>(nba.getAPSet().asList()));

		if (initialState.intersects(nbaF)) {
			powersetOneF.set(initialId);
		}
		if (nbaF.containsAll(initialState)) {
			powersetAllF.set(initialId);
		}

		//System.out.println("new: "+initialId+" "+initialState);

		BitSet visited = new BitSet();
		while (!todo.isEmpty()) {
			int curId = todo.poll();
			if (visited.get(curId)) continue;

			BitSet cur = idToState.get(curId);
			//System.out.println("Expand "+curId+" "+cur);

			for (APElement e : nba.getAPSet().elements()) {
				BitSet to = new BitSet();
				for (int f : IterableBitSet.getSetBits(cur)) {
					to.or(nba.get(f).getEdge(e));
				}

				Integer toId = stateToId.get(to);
				if (toId == null) {
					toId = da.addState();
					stateToId.put(to, toId);
					idToState.add(to);
					todo.add(toId);

					//System.out.println("new: "+toId+" "+to);

					if (to.intersects(nbaF)) {
						powersetOneF.set(toId);
					}
					if (nbaF.containsAll(to)) {
						powersetAllF.set(toId);
					}
				}

				//System.out.println(" delta(" + curId + ", " + e + ") = " + toId);
				da.addEdge(curId, e, toId);
			}
		}

		//da.printHOA(System.out);

		PowersetDA result = new PowersetDA();
		result.nba = nba;
		result.da = da;
		result.idToState = idToState;
		result.powersetOneF = powersetOneF;
		result.powersetAllF = powersetAllF;
		//System.out.println("powersetOneF = "+powersetOneF);
		//System.out.println("powersetAllF = "+powersetAllF);

		return result;
	}

	/**
	 * Analyse the SCCs of the DA obtained by the power set construction,
	 * whether they should be accepting or rejecting.
	 * @param P the DA
	 */
	private void determineF(final PowersetDA P) throws PrismException
	{
		LTS daLTS = new LTSFromDA(P.da);

		SCCConsumerStore sccStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, daLTS, sccStore);
		sccComputer.computeSCCs();
		for (BitSet scc : sccStore.getSCCs()) {
			if (hasAcceptingCycle(P, scc)) {
				// mark all SCC states as final in powerset automaton
				P.F.or(scc);
			}
		}

		// construct acceptance
		P.da.getAcceptance().setAcceptingStates(P.F);
	}

	/**
	 * Check whether the given SCC of the DBA has an accepting cycle
	 * in the underlying NBA.
	 * @param P the powerset DBA
	 * @param scc an SCC of P
	 */
	private boolean hasAcceptingCycle(PowersetDA P, BitSet scc) throws PrismException
	{
		//System.out.println("hasAcceptingCycle "+scc+"?");
		 if (!scc.intersects(P.powersetOneF)) {
			// none of the NBA states in this powerset SCC are final
			//System.out.println(" -> no (none final)");
			return false;
		}
		if (P.powersetAllF.containsAll(scc)) {
			// all NBA states in this powerset SCC are final
			//System.out.println(" -> yes (all final)");
			return true;
		}

		// first, construct an arbitrary lasso in P, remaining
		// in the SCC
		Lasso lasso = findLasso(P, scc);
		//System.out.println(cycle);

		// second, construct the NBA fragment corresponding to the
		// lasso
		final BuchiLTS buchilts = buildLTSforLasso(P, lasso);
		// String name = "lts"+scc+".dot";
		// buchilts.lts.exportToDotFile(name, p.F);
		//System.out.println("DOT: "+name);

		// perform cycle check
		// TODO: use nested-DFS?
		boolean hasCycleViaF = false;
		SCCConsumerStore sccStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, buchilts.lts, sccStore);
		sccComputer.computeSCCs();
		for (BitSet subSCC : sccStore.getSCCs()) {
			if (subSCC.intersects(buchilts.F)) {
				hasCycleViaF = true;
				break;
			}
		}

		//System.out.println(hasCycleViaF ? " -> yes" : " -> no");
		return hasCycleViaF;
	}

	/** A lasso, i.e., an infinite word uw^omega */
	private static class Lasso {
		LinkedList<APElement> word;
		/** start of the w fragment */
		int cycleStart;

		public String toString()
		{
			String s = "Cycle starting at "+cycleStart+" with ";
			s+= word.toString();
			return s;
		}
	}

	/** Construct an arbitrary lasso inside the SCC of the DA */
	private Lasso findLasso(PowersetDA P, BitSet scc) throws PrismException
	{
		// pick a start state
		int R = scc.nextSetBit(0);

		Stack<Integer> states = new Stack<Integer>();
		Stack<BitSet> letters = new Stack<BitSet>();
		BitSet onStack = new BitSet();

		states.push(R);
		onStack.set(R);

		final DA<BitSet, AcceptanceBuchi> da = P.da;

		while (true) {
			int cur = states.peek();
			// find first letter that allows remaining in SCC
			int n = da.getNumEdges(cur);
			boolean found = false;
			int to = -1;
			for (int i=0; i<n; i++) {
				to = P.da.getEdgeDest(cur, i);
				if (scc.get(to)) {
					BitSet letter = da.getEdgeLabel(cur, i);
					letters.add(letter);
					states.add(to);
					found = true;
					break;
				}
			}
			if (!found) {
				throw new PrismException("Implementation error in findCycle");
			}

			if (!onStack.get(to)) {
				// not yet a cycle
				onStack.set(to);
				continue;
			}

			// found a cycle
			int cycleStart = to;
			Lasso cycle = new Lasso();
			cycle.word = new LinkedList<APElement>();
			cycle.cycleStart = cycleStart;

			do {
				APElement label = new APElement();
				label.or(letters.pop());
				cycle.word.addFirst(label);
				states.pop();
			} while (states.peek() != to);

			return cycle;
		}
	}

	private static class LassoLTSState
	{
		int nbaState;
		int cyclePos;

		LassoLTSState(int nbaState, int cyclePos)
		{
			this.nbaState = nbaState;
			this.cyclePos = cyclePos;
		}

		public String toString()
		{
			return "("+nbaState+","+cyclePos+")";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + cyclePos;
			result = prime * result + nbaState;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			LassoLTSState other = (LassoLTSState) obj;
			if (cyclePos != other.cyclePos)
				return false;
			if (nbaState != other.nbaState)
				return false;
			return true;
		}
	}

	private static class BuchiLTS
	{
		LTS lts;
		BitSet F;
	}

	/** Build an LTS for the NBA fragment corresponding to the lasso in the DBA. */
	private BuchiLTS buildLTSforLasso(PowersetDA p, Lasso lasso)
	{
		HashMap<LassoLTSState, Integer> stateToIndex = new HashMap<LassoLTSState, Integer>();
		ArrayList<LassoLTSState> indexToState = new ArrayList<LassoLTSState>();

		Stack<Integer> todo = new Stack<Integer>();
		BitSet expanded = new BitSet();
		BitSet F = new BitSet();

		LTSSimple lts = new LTSSimple();

		for (int startNBA : IterableBitSet.getSetBits(p.idToState.get(lasso.cycleStart))) {
			int i = lts.addState();
			todo.push(i);
			LassoLTSState s = new LassoLTSState(startNBA, 0);
			stateToIndex.put(s, i);
			indexToState.add(s);
			if (p.nba.get(startNBA).isFinal()) {
				F.set(i);
			}

			//System.out.println("new: " + i + " = "+ s);
		}

		while (!todo.isEmpty()) {
			int curProd = todo.pop();
			if (expanded.get(curProd)) continue;

			LassoLTSState cur = indexToState.get(curProd);
			// expand
			//System.out.println("Expand "+curProd+" = "+cur);
			APElement letter = lasso.word.get(cur.cyclePos);
			MyBitSet toSet = p.nba.get(cur.nbaState).getEdge(letter);
			int cyclePos = (cur.cyclePos+1) % lasso.word.size();

			for (int to : toSet) {
				LassoLTSState toProd = new LassoLTSState(to, cyclePos);
				Integer prodTo = stateToIndex.get(toProd);
				if (prodTo == null) {
					prodTo = lts.addState();
					todo.push(prodTo);
					stateToIndex.put(toProd, prodTo);
					indexToState.add(toProd);

					if (p.nba.get(to).isFinal()) {
						F.set(prodTo);
					}

					//System.out.println("new: " + prodTo + " = " +toProd);
				}

				lts.addTransition(curProd, prodTo);
				//System.out.println(" " + curProd +" -> " +prodTo);
			}
		}

		BuchiLTS result = new BuchiLTS();
		result.lts = lts;
		result.F = F;
		return result;
	}


	/**
	 * Simple test method: convert LTL formula (in LBT format) to HOA/Dot/txt
	 */
	public static void main(String args[])
	{
		try {
			// Usage:
			// * ... 'X p1'
			// * ... 'X p1' da.hoa
			// * ... 'X p1' da.hoa hoa
			// * ... 'X p1' da.dot dot
			// * ... 'X p1' - hoa
			// * ... 'X p1' - txt

			// Convert to Expression (from LBT format)
			SimpleLTL sltl = SimpleLTL.parseFormulaLBT(args[0]);

			// Build/export DA
			PrismComponent parent = new PrismComponent();
			parent.setLog(new PrismDevNullLog());
			LTL2WDBA ltl2wdba = new LTL2WDBA(parent);
			LTLFragments tl = LTLFragments.analyse(sltl);

			DA<BitSet, ? extends AcceptanceOmega> da;
			if (tl.isSyntacticGuarantee()) {
				da = ltl2wdba.cosafeltl2dfa(sltl);
			} else if (tl.isSyntacticObligation()) {
				da = ltl2wdba.obligation2wdba(sltl);
			} else {
				throw new Exception("Can not construct an automaton for " + sltl +", not syntactically co-safe or obligation");
			}
			PrintStream out = (args.length < 2 || "-".equals(args[1])) ? System.out : new PrintStream(args[1]);
			String format = (args.length < 3) ? "hoa" : args[2];
			da.print(out, format);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error: " + e + ".");
		}
	}
}
