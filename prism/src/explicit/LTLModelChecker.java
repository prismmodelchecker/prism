//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Alessandro Bruni <albr@dtu.dk> (Technical University of Denmark)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.awt.Point;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import acceptance.AcceptanceRabin;
import common.IterableStateSet;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import parser.type.TypePathBool;
import prism.DA;
import prism.LTL2RabinLibrary;
import prism.PrismComponent;
import prism.PrismException;

/**
 * LTL model checking functionality
 */
public class LTLModelChecker extends PrismComponent
{
	/** Make LTL product accessible as a Product */
	public class LTLProduct<M extends Model> extends Product<M> {
		private int draSize;
		private int invMap[];
		private AcceptanceRabin acceptance;

		public LTLProduct(M productModel, M originalModel, AcceptanceRabin acceptance, int draSize, int[] invMap)
		{
			super(productModel, originalModel);
			this.draSize = draSize;
			this.invMap = invMap;
			this.acceptance = acceptance;
		}

		@Override
		public int getModelState(int productState)
		{
			return invMap[productState] / draSize;
		}

		@Override
		public int getAutomatonState(int productState)
		{
			return invMap[productState] % draSize;
		}

		public AcceptanceRabin getAcceptance() {
			return acceptance;
		}
	}


	/**
	 * Create a new LTLModelChecker, inherit basic state from parent (unless null).
	 */
	public LTLModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Convert an LTL formula into a DRA. The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 */
	public static DA<BitSet,AcceptanceRabin> convertLTLFormulaToDRA(Expression ltl) throws PrismException
	{
		return LTL2RabinLibrary.convertLTLFormulaToDRA(ltl);
	}

	/**
	 * Extract maximal state formula from an LTL path formula, model check them (with passed in model checker) and
	 * replace them with ExpressionLabel objects L0, L1, etc. Expression passed in is modified directly, but the result
	 * is also returned. As an optimisation, model checking that results in true/false for all states is converted to an
	 * actual true/false, and duplicate results are given the same label. BitSets giving the states which satisfy each label
	 * are put into the vector {@code labelBS}, which should be empty when this function is called.
	 */
	public Expression checkMaximalStateFormulas(ProbModelChecker mc, Model model, Expression expr, Vector<BitSet> labelBS) throws PrismException
	{
		// A state formula
		if (expr.getType() instanceof TypeBool) {
			// Model check state formula for all states
			StateValues sv = mc.checkExpression(model, expr, null);
			BitSet bs = sv.getBitSet();
			// Detect special cases (true, false) for optimisation
			if (bs.isEmpty()) {
				return Expression.False();
			}
			if (bs.cardinality() == model.getNumStates()) {
				return Expression.True();
			}
			// See if we already have an identical result
			// (in which case, reuse it)
			int i = labelBS.indexOf(bs);
			if (i != -1) {
				sv.clear();
				return new ExpressionLabel("L" + i);
			}
			// Otherwise, add result to list, return new label
			labelBS.add(bs);
			return new ExpressionLabel("L" + (labelBS.size() - 1));
		}
		// A path formula (recurse, modify, return)
		else if (expr.getType() instanceof TypePathBool) {
			if (expr instanceof ExpressionBinaryOp) {
				ExpressionBinaryOp exprBinOp = (ExpressionBinaryOp) expr;
				exprBinOp.setOperand1(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand1(), labelBS));
				exprBinOp.setOperand2(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand2(), labelBS));
			} else if (expr instanceof ExpressionUnaryOp) {
				ExpressionUnaryOp exprUnOp = (ExpressionUnaryOp) expr;
				exprUnOp.setOperand(checkMaximalStateFormulas(mc, model, exprUnOp.getOperand(), labelBS));
			} else if (expr instanceof ExpressionTemporal) {
				ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
				if (exprTemp.getOperand1() != null) {
					exprTemp.setOperand1(checkMaximalStateFormulas(mc, model, exprTemp.getOperand1(), labelBS));
				}
				if (exprTemp.getOperand2() != null) {
					exprTemp.setOperand2(checkMaximalStateFormulas(mc, model, exprTemp.getOperand2(), labelBS));
				}
			}
		}
		return expr;
	}

	/**
	 * Construct the product of a DRA and a DTMC.
	 * @param dra The DRA
	 * @param dtmc The DTMC
	 * @param labelBS BitSets giving the set of states for each AP in the DRA
	 * @param statesOfInterest the set of states for which values should be calculated (null = all states)
	 * @return The product DTMC
	 */
	public LTLProduct<DTMC> constructProductMC(DA<BitSet,AcceptanceRabin> dra, DTMC dtmc, Vector<BitSet> labelBS, BitSet statesOfInterest) throws PrismException
	{
		DTMCSimple prodModel = new DTMCSimple();

		int draSize = dra.size();
		int numAPs = dra.getAPList().size();
		int modelNumStates = dtmc.getNumStates();
		int prodNumStates = modelNumStates * draSize;
		int s_1, s_2, q_1, q_2;
		BitSet s_labels = new BitSet(numAPs);
		AcceptanceRabin prodAcceptance;

		// Encoding: 
		// each state s' = <s, q> = s * draSize + q
		// s(s') = s' / draSize
		// q(s') = s' % draSize

		LinkedList<Point> queue = new LinkedList<Point>();
		int map[] = new int[prodNumStates];
		Arrays.fill(map, -1);

		// We need results for all states of the original model in statesOfInterest
		// We thus explore states of the product starting from these states.
		// These are designated as initial states of the product model
		// (a) to ensure reachability is done for these states; and
		// (b) to later identify the corresponding product state for the original states
		//     of interest
		for (int s_0 : new IterableStateSet(statesOfInterest, dtmc.getNumStates())) {
			// Get BitSet representing APs (labels) satisfied by state s_0
			for (int k = 0; k < numAPs; k++) {
				s_labels.set(k, labelBS.get(Integer.parseInt(dra.getAPList().get(k).substring(1))).get(s_0));
			}
			// Find corresponding initial state in DRA
			int q_0 = dra.getEdgeDestByLabel(dra.getStartState(), s_labels);
			// Add (initial) state to product
			queue.add(new Point(s_0, q_0));
			prodModel.addState();
			prodModel.addInitialState(prodModel.getNumStates() - 1);
			map[s_0 * draSize + q_0] = prodModel.getNumStates() - 1;
		}

		// Product states
		BitSet visited = new BitSet(prodNumStates);
		while (!queue.isEmpty()) {
			Point p = queue.pop();
			s_1 = p.x;
			q_1 = p.y;
			visited.set(s_1 * draSize + q_1);

			// Go through transitions from state s_1 in original DTMC
			Iterator<Map.Entry<Integer, Double>> iter = dtmc.getTransitionsIterator(s_1);
			while (iter.hasNext()) {
				Map.Entry<Integer, Double> e = iter.next();
				s_2 = e.getKey();
				double prob = e.getValue();
				// Get BitSet representing APs (labels) satisfied by successor state s_2
				for (int k = 0; k < numAPs; k++) {
					s_labels.set(k, labelBS.get(Integer.parseInt(dra.getAPList().get(k).substring(1))).get(s_2));
				}
				// Find corresponding successor in DRA
				q_2 = dra.getEdgeDestByLabel(q_1, s_labels);
				// Add state/transition to model
				if (!visited.get(s_2 * draSize + q_2) && map[s_2 * draSize + q_2] == -1) {
					queue.add(new Point(s_2, q_2));
					prodModel.addState();
					map[s_2 * draSize + q_2] = prodModel.getNumStates() - 1;
				}
				prodModel.setProbability(map[s_1 * draSize + q_1], map[s_2 * draSize + q_2], prob);
			}
		}

		// Build a mapping from state indices to states (s,q), encoded as (s * draSize + q) 
		int invMap[] = new int[prodModel.getNumStates()];
		for (int i = 0; i < map.length; i++) {
			if (map[i] != -1) {
				invMap[map[i]] = i;
			}
		}

		prodModel.findDeadlocks(false);

		prodAcceptance = new AcceptanceRabin();
		LTLProduct<DTMC> product = new LTLProduct<DTMC>(prodModel, dtmc, prodAcceptance, draSize, invMap);

		// generate acceptance for the product model by lifting
		for (AcceptanceRabin.RabinPair pair : dra.getAcceptance()) {
			BitSet Lprod = product.liftFromAutomaton(pair.getL());
			BitSet Kprod = product.liftFromAutomaton(pair.getK());

			prodAcceptance.add(new AcceptanceRabin.RabinPair(Lprod, Kprod));
		}

		return product;
	}

	/**
	 * Construct the product of a DRA and an MDP.
	 * @param dra The DRA
	 * @param mdp The MDP
	 * @param labelBS BitSets giving the set of states for each AP in the DRA
	 * @param statesOfInterest the set of states for which values should be calculated (null = all states)
	 * @return The product MDP
	 */
	public LTLProduct<MDP> constructProductMDP(DA<BitSet,AcceptanceRabin> dra, MDP mdp, Vector<BitSet> labelBS, BitSet statesOfInterest) throws PrismException
	{
		MDPSimple prodModel = new MDPSimple();

		int draSize = dra.size();
		int numAPs = dra.getAPList().size();
		int modelNumStates = mdp.getNumStates();
		int prodNumStates = modelNumStates * draSize;
		int s_1, s_2, q_1, q_2;
		BitSet s_labels = new BitSet(numAPs);
		AcceptanceRabin prodAcceptance;

		// Encoding: 
		// each state s' = <s, q> = s * draSize + q
		// s(s') = s' / draSize
		// q(s') = s' % draSize

		LinkedList<Point> queue = new LinkedList<Point>();
		int map[] = new int[prodNumStates];
		Arrays.fill(map, -1);

		// We need results for all states of the original model in statesOfInterest
		// We thus explore states of the product starting from these states.
		// These are designated as initial states of the product model
		// (a) to ensure reachability is done for these states; and
		// (b) to later identify the corresponding product state for the original states
		//     of interest
		for (int s_0 : new IterableStateSet(statesOfInterest, mdp.getNumStates())) {
			// Get BitSet representing APs (labels) satisfied by state s_0
			for (int k = 0; k < numAPs; k++) {
				s_labels.set(k, labelBS.get(Integer.parseInt(dra.getAPList().get(k).substring(1))).get(s_0));
			}
			// Find corresponding initial state in DRA
			int q_0 = dra.getEdgeDestByLabel(dra.getStartState(), s_labels);
			// Add (initial) state to product
			queue.add(new Point(s_0, q_0));
			prodModel.addState();
			prodModel.addInitialState(prodModel.getNumStates() - 1);
			map[s_0 * draSize + q_0] = prodModel.getNumStates() - 1;
		}

		// Product states
		BitSet visited = new BitSet(prodNumStates);
		while (!queue.isEmpty()) {
			Point p = queue.pop();
			s_1 = p.x;
			q_1 = p.y;
			visited.set(s_1 * draSize + q_1);

			// Go through transitions from state s_1 in original DTMC
			int numChoices = mdp.getNumChoices(s_1);
			for (int j = 0; j < numChoices; j++) {
				Distribution prodDistr = new Distribution();
				Iterator<Map.Entry<Integer, Double>> iter = mdp.getTransitionsIterator(s_1, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> e = iter.next();
					s_2 = e.getKey();
					double prob = e.getValue();
					// Get BitSet representing APs (labels) satisfied by successor state s_2
					for (int k = 0; k < numAPs; k++) {
						s_labels.set(k, labelBS.get(Integer.parseInt(dra.getAPList().get(k).substring(1))).get(s_2));
					}
					// Find corresponding successor in DRA
					q_2 = dra.getEdgeDestByLabel(q_1, s_labels);
					// Add state/transition to model
					if (!visited.get(s_2 * draSize + q_2) && map[s_2 * draSize + q_2] == -1) {
						queue.add(new Point(s_2, q_2));
						prodModel.addState();
						map[s_2 * draSize + q_2] = prodModel.getNumStates() - 1;
					}
					prodDistr.set(map[s_2 * draSize + q_2], prob);
				}
				prodModel.addActionLabelledChoice(map[s_1 * draSize + q_1], prodDistr, mdp.getAction(s_1, j));
			}
		}

		// Build a mapping from state indices to states (s,q), encoded as (s * draSize + q) 
		int invMap[] = new int[prodModel.getNumStates()];
		for (int i = 0; i < map.length; i++) {
			if (map[i] != -1) {
				invMap[map[i]] = i;
			}
		}

		prodModel.findDeadlocks(false);

		prodAcceptance = new AcceptanceRabin();
		LTLProduct<MDP> product = new LTLProduct<MDP>(prodModel, mdp, prodAcceptance, draSize, invMap);

		// generate acceptance for the product model by lifting
		for (AcceptanceRabin.RabinPair pair : dra.getAcceptance()) {
			BitSet Lprod = product.liftFromAutomaton(pair.getL());
			BitSet Kprod = product.liftFromAutomaton(pair.getK());

			prodAcceptance.add(new AcceptanceRabin.RabinPair(Lprod, Kprod));
		}

		return product;
	}

	/**
	 * Find the set of states belong to accepting BSCCs in a model wrt a Rabin acceptance condition.
	 * @param model The model
	 * @param acceptance The Rabin acceptance condition
	 */
	public BitSet findAcceptingBSCCsForRabin(Model model, AcceptanceRabin acceptance) throws PrismException
	{
		// Compute bottom strongly connected components (BSCCs)
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, model);
		sccComputer.computeBSCCs();
		List<BitSet> bsccs = sccComputer.getBSCCs();

		BitSet result = new BitSet();

		for (BitSet bscc : bsccs) {
			if (acceptance.isBSCCAccepting(bscc)) {
				// this BSCC is accepting
				result.or(bscc);
			}
		}

		return result;
	}

	/**
	 * Find the set of states in accepting end components (ECs) in a nondeterministic model wrt a Rabin acceptance condition.
	 * @param model The model
	 * @param acceptance the acceptance condition
	 */
	public BitSet findAcceptingECStatesForRabin(NondetModel model, AcceptanceRabin acceptance) throws PrismException
	{
		BitSet allAcceptingStates = new BitSet();
		int numStates = model.getNumStates();
		
		// Go through the DRA acceptance pairs (L_i, K_i) 
		for (int i = 0; i < acceptance.size(); i++) {
			// Find model states *not* satisfying L_i
			BitSet bitsetLi = acceptance.get(i).getL();
			BitSet statesLi_not = new BitSet();
			for (int s = 0; s < numStates; s++) {
				if (!bitsetLi.get(s)) {
					statesLi_not.set(s);
				}
			}
			// Skip pairs with empty !L_i
			if (statesLi_not.cardinality() == 0)
				continue;
			// Compute maximum end components (MECs) in !L_i
			ECComputer ecComputer = ECComputer.createECComputer(this, model);
			ecComputer.computeMECStates(statesLi_not);
			List<BitSet> mecs = ecComputer.getMECStates();
			// Check with MECs contain a K_i state
			BitSet bitsetKi = acceptance.get(i).getK();
			for (BitSet mec : mecs) {
				if (mec.intersects(bitsetKi)) {
					allAcceptingStates.or(mec);
				}
			}
		}

		return allAcceptingStates;
	}
}
