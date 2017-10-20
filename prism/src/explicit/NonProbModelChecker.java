//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
import java.util.Iterator;
import java.util.Vector;

import automata.LTL2NBA;
import jltl2dstar.NBA;
import common.IterableBitSet;
import common.IterableStateSet;
import parser.ast.Expression;
import parser.ast.ExpressionExists;
import parser.ast.ExpressionForAll;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;

/**
 * Explicit-state, non-probabilistic model checker.
 */
public class NonProbModelChecker extends StateModelChecker
{
	/**
	 * Create a new NonProbModelChecker, inherit basic state from parent (unless null).
	 */
	public NonProbModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}
	
	@Override
	public StateValues checkExpression(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		StateValues res;

		// E operator
		if (expr instanceof ExpressionExists) {
			return checkExpressionExists(model, ((ExpressionExists)expr).getExpression(), statesOfInterest);
		}
		// A operator
		else if (expr instanceof ExpressionForAll) {
			return checkExpressionForAll(model, ((ExpressionForAll)expr).getExpression(), statesOfInterest);
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(model, expr, statesOfInterest);
		}

		return res;
	}

	/**
	 * Compute the set of states satisfying E[ expr ].
	 * <br>
	 * 'expr' has to be a simple path formula.
	 *
	 * @param model the model
	 * @param expr the expression for 'expr'
	 * @param statesOfInterest the states of interest ({@code null} = all states)
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ expr ]
	 */
	protected StateValues checkExpressionExists(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		// Check whether we have to use LTL path formula handling
		if (getSettings().getBoolean(PrismSettings.PRISM_PATH_VIA_AUTOMATA)
		    || !expr.isSimplePathFormula() ) {
			return checkExistsLTL(model, expr, statesOfInterest);
		}

		// We have a simple path formula, convert to either
		// (1) a U b
		// (2) !(a U b)
		// (3) X a
		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);

		// next-step (3)
		if (expr instanceof ExpressionTemporal &&
		    ((ExpressionTemporal) expr).getOperator() == ExpressionTemporal.P_X) {
			if (((ExpressionTemporal)expr).hasBounds()) {
				throw new PrismNotSupportedException("Model checking of bounded CTL operators is not supported");
			}
			return checkExistsNext(model, ((ExpressionTemporal) expr).getOperand2(), statesOfInterest);
		}

		// until
		boolean negated = false;
		if (Expression.isNot(expr)) {
			// (2) !(a U b)
			negated = true;
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}

		ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
		assert (exprTemp.getOperator() == ExpressionTemporal.P_U);

		if (exprTemp.hasBounds()) {
			throw new PrismNotSupportedException("Model checking of bounded CTL operators is not supported");
		}

		StateValues result;

		if (negated) {
			// compute E[ !a R !b ] instead of E[ !(a U b) ]
			result = checkExistsRelease(model,
			                            Expression.Not(exprTemp.getOperand1()),
			                            Expression.Not(exprTemp.getOperand2()));
		} else {
			// compute E[ a U b ]
			result = checkExistsUntil(model, exprTemp.getOperand1(), exprTemp.getOperand2());
		}

		return result;
	}

	/**
	 * Compute the set of states satisfying A[ expr ].
	 * <br>
	 * This is computed by determining the set of states not satisfying E[ !expr ].
	 * 'expr' has to be a simple path formula.
	 *
	 * @param model the model
	 * @param expr the expression for 'expr'
	 * @param statesOfInterest the states of interest ({@code null} = all states)
	 * @return a boolean StateValues, with {@code true} for all states satisfying A[ expr ]
	 */
	protected StateValues checkExpressionForAll(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		StateValues result = checkExpressionExists(model, Expression.Not(expr), statesOfInterest);
		result.complement();

		return result;
	}

	/**
	 * Compute the set of states satisfying E[ X expr ].
	 * @param model the model
	 * @param expr the expression for 'expr'
	 * @param statesOfInterest the states of interest ({@code null} = all states)
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ X expr ]
	 */
	protected StateValues checkExistsNext(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		BitSet target = checkExpression(model, expr, null).getBitSet();
		BitSet result = computeExistsNext(model, target, statesOfInterest);

		return StateValues.createFromBitSet(result, model);
	}

	/**
	 * Compute the set of states satisfying E[ X "target" ].
	 * @param model the model
	 * @param target the BitSet of states for target
	 * @param statesOfInterest the states of interest ({@code null} = all states)
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ X "target" ]
	 */
	public BitSet computeExistsNext(Model model, BitSet target, BitSet statesOfInterest) throws PrismException
	{
		BitSet result = new BitSet();

		for (int s : new IterableStateSet(statesOfInterest, model.getNumStates())) {
			// for each state of interest, check whether there is a transition to the 'target'
			if (model.someSuccessorsInSet(s, target)) {
				result.set(s);
			}
		}

		return result;
	}

	/**
	 * Compute the set of states satisfying A[ X expr ].
	 * @param model the model
	 * @param expr the expression for 'expr'
	 * @param statesOfInterest the states of interest ({@code null} = all states)
	 * @return a boolean StateValues, with {@code true} for all states satisfying A[ X expr ]
	 */	
	protected StateValues checkForAllNext(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		BitSet target = checkExpression(model, expr, null).getBitSet();
		BitSet result = new BitSet();

		for (int s : new IterableStateSet(statesOfInterest, model.getNumStates())) {
			// for each state of interest, check whether all transitions go to 'target'
			if (model.allSuccessorsInSet(s, target)) {
				result.set(s);
			}
		}

		return StateValues.createFromBitSet(result, model);
	}

	/**
	 * Compute the set of states satisfying A[ X "target" ].
	 * @param model the model
	 * @param target the BitSet of states in target
	 * @param statesOfInterest the states of interest ({@code null} = all states)
	 * @return a boolean StateValues, with {@code true} for all states satisfying A[ X "target" ]
	 */
	public BitSet computeForAllNext(Model model, BitSet target, BitSet statesOfInterest) throws PrismException
	{
		BitSet result = new BitSet();

		for (int s : new IterableStateSet(statesOfInterest, model.getNumStates())) {
			// for each state of interest, check whether all transitions go to 'target'
			if (model.allSuccessorsInSet(s, target)) {
				result.set(s);
			}
		}

		return result;
	}

	
	/**
	 * Compute the set of states satisfying E[ a U b ].
	 * @param model the model
	 * @param exprA the expression for 'a'
	 * @param exprB the expression for 'b'
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ a U b ]
	 */
	protected StateValues checkExistsUntil(Model model, Expression exprA, Expression exprB) throws PrismException {
		// the set of states satisfying exprA
		BitSet A = checkExpression(model, exprA, null).getBitSet();
		// the set of states satisfying exprB
		BitSet B = checkExpression(model, exprB, null).getBitSet();

		BitSet result = computeExistsUntil(model, A, B);

		return StateValues.createFromBitSet(result, model);
	}

	/**
	 * Compute the set of states satisfying E[ "a" U "b" ].
	 * @param model the model
	 * @param A the BitSet of states for "a"
	 * @param B the BitSet of states for "b"
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ "a" U "b" ]
	 */
	public BitSet computeExistsUntil(Model model, BitSet A, BitSet B) throws PrismException
	{
 		PredecessorRelation pre = model.getPredecessorRelation(this, true);
 		return pre.calculatePreStar(A, B, B);
	}

	/**
	 * Compute the set of states satisfying E[ G a ].
	 * @param model the model
	 * @param exprA the expression for 'a'
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ G a ]
	 */
	protected StateValues checkExistsGlobally(Model model, Expression exprA) throws PrismException
	{
		return checkExistsRelease(model, Expression.False(), exprA);
	}

	/**
	 * Compute the set of states satisfying E[ G "a" ].
	 * @param model the model
	 * @param A the BitSet of states for "a"
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ G "a" ]
	 */
	public BitSet computeExistsGlobally(Model model, BitSet A) throws PrismException
	{
		return computeExistsRelease(model, new BitSet(), A);
	}

	/**
	 * Compute the set of states satisfying E[ a R b ], i.e., from which there
	 * exists a path satisfying G b or b U (a&b)
	 * @param model the model
	 * @param exprA the expression for 'a'
	 * @param exprB the expression for 'b'
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ a R b ]
	 */
	protected StateValues checkExistsRelease(Model model, Expression exprA, Expression exprB) throws PrismException
	{
		// the set of states satisfying exprA
		BitSet A = checkExpression(model, exprA, null).getBitSet();
		// the set of states satisfying exprB
		BitSet B = checkExpression(model, exprB, null).getBitSet();

		PredecessorRelation pre = model.getPredecessorRelation(this, true);

		// the intersection of A and B
		// these states surely satisfy E[ a R b ]
		BitSet AandB = (BitSet) A.clone();
		AandB.and(B);

		// The set T contains all states for which E[ a R b ] has not yet been disproven
		// Initially, this contains all 'B' states
		BitSet T = (BitSet) B.clone();

		// the set E contains all states that have not yet been visited
		// and for which we have proven that they do not satisfy E[ a R b ]
		// Initially, it contains the complement of 'T'
		BitSet E = (BitSet) T.clone();
		E.flip(0, model.getNumStates());

		// NOTE: The correctness relies on the fact that both
		// getSuccessorsIterator and pre.getPre are exactly the two sides
		// of the underlying edge relation, i.e., that the number of entries
		// is consistent. If the PredecessorRelation semantics are changed
		// later on, we have to adapt here as well...

		// for all states s in T \ AandB, we compute count[s], the number of successors
		// according to getSuccessorsIterator
		int count[] = new int[model.getNumStates()];
		for (int s : IterableBitSet.getSetBits(T)) {
			if (AandB.get(s)) continue;

			int i=0;
			for (Iterator<Integer> it = model.getSuccessorsIterator(s); it.hasNext(); it.next()) {
				i++;
			}
			count[s]=i;
		}
		
		while (!E.isEmpty()) {
			// get the first element of E
			int t = E.nextSetBit(0);
			// and mark it as processed
			E.clear(t);

			// We know that t does not satisfy E[ a R b ].
			// Any predecessor s of t can be shown to not satisfy E[ a R b ]
			// if there are no remaining successors into T, i.e, if count[s]==0

			// For all predecessors s of t....
			for (int s : pre.getPre(t)) {
				// ... ignore if we have already proven that it does not satisfy E[ a R b ]
				if (!T.get(s)) continue;

				// decrement count, because s can not use t to stay in T
				count[s]--;
				if (count[s] == 0 && !AandB.get(s)) {
					// the count is zero and s is not safe because it's in AandB
					//  -> remove from T and add to E
					T.clear(s);
					E.set(s);
				}
			}
		}

		return StateValues.createFromBitSet(T, model);
	}

	/**
	 * Compute the set of states satisfying E[ "a" R "b" ], i.e., from which there
	 * exists a path satisfying G "b" or "b" U ("a&b")
	 * @param model the model
	 * @param A the BitSet for the states in "a"
	 * @param A the BitSet for the states in "a"
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ "a" R "b" ]
	 */
	public BitSet computeExistsRelease(Model model, BitSet A, BitSet B) throws PrismException
	{
		PredecessorRelation pre = model.getPredecessorRelation(this, true);

		// the intersection of A and B
		// these states surely satisfy E[ a R b ]
		BitSet AandB = (BitSet) A.clone();
		AandB.and(B);

		// The set T contains all states for which E[ a R b ] has not yet been disproven
		// Initially, this contains all 'B' states
		BitSet T = (BitSet) B.clone();

		// the set E contains all states that have not yet been visited
		// and for which we have proven that they do not satisfy E[ a R b ]
		BitSet E = new BitSet();
		// Initially, it contains the complement of 'T'
		E.set(0, model.getNumStates(), true);
		E.andNot(T);

		// TODO: Important: The correctness relies on the fact that
		// the number of predecessors provided by pre.getPre()
		// and the number of successors provided by getSuccessorsIterator()
		// mirror each other. If PredecessorRelation is changed later on,
		// take this into account...

		// for all states s in T \ AandB, we compute count[s], the number of (unique) successors
		int count[] = new int[model.getNumStates()];
		for (int s : IterableBitSet.getSetBits(T)) {
			if (AandB.get(s)) continue;

			int i=0;
			for (Iterator<Integer> it = model.getSuccessorsIterator(s); it.hasNext(); it.next()) {
				i++;
			}
			count[s]=i;
		}

		while (!E.isEmpty()) {
			// get the first element of E
			int t = E.nextSetBit(0);
			// and mark it as processed
			E.clear(t);

			// We know that t does not satisfy E[ a R b ].
			// Any predecessor s of t can be shown to not satisfy E[ a R b ]
			// if there are no remaining successors into T, i.e, if count[s]==0

			// For all predecessors s of t....

			for (int s : pre.getPre(t)) {
				// ... ignore if we have already proven that it does not satisfy E[ a R b ]
				if (!T.get(s)) continue;

				// decrement count, because s can not use t to stay in T
				count[s]--;
				if (count[s] == 0 && !AandB.get(s)) {
					// the count is zero and s is not safe because it's in AandB
					//  -> remove from T and add to E
					T.clear(s);
					E.set(s);
				}
			}
		}

		return T;
	}

	/**
	 * Compute the set of states satisfying E[ phi ] for an LTL formula phi.
	 * The LTL formula can have nested P or R operators, as well as nested CTL formulas.
	 * @param model the model
	 * @param expr the LTL formula
	 * @param statesofInterest the states of interest
	 * @return a boolean StateValues, with {@code true} for all states satisfying E[ phi ]
	 */
	protected StateValues checkExistsLTL(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		if (Expression.containsTemporalTimeBounds(expr)) {
			throw new PrismNotSupportedException("Time-bounded operators not supported in LTL: " + expr);
		}

		LTLModelChecker ltlMC = new LTLModelChecker(this);
		Vector<BitSet> labelBS = new Vector<BitSet>();
		expr = ltlMC.checkMaximalStateFormulas(this, model, expr, labelBS);

		// We are doing existential LTL checking:
		//  - Construct an NBA for the LTL formula
		//  - Construct product M' = M x NBA
		//  - Search for an accepting lasso in M', i.e., a reachable cycle
		//    that visits F infinitely often

		mainLog.println("Non-probabilistic LTL model checking for E[ " +expr + " ]");
		mainLog.print("Constructing NBA...");
		mainLog.flush();
		LTL2NBA ltl2nba = new LTL2NBA(this);
		NBA nba = ltl2nba.convertLTLFormulaToNBA(expr, this.getConstantValues());
		mainLog.println(" NBA has " + nba.size() + " states");

		// If we only care about a few states in the model,
		// it would make sense to do a nested DFS and construct the product on the fly.
		// But for now it's easier to rely on the existing infrastructure,
		// construct the full product and just compute the SCCs.
		mainLog.print("Constructing " + model.getModelType()+ "-NBA product as LTS...");
		mainLog.flush();
		LTSNBAProduct product = LTSNBAProduct.doProduct(model, nba, statesOfInterest, labelBS);
		mainLog.println(" "+product.getProductModel().infoString()+", "+product.getAcceptingStates().cardinality()+" states accepting");

		// Note: As the NBA is not guaranteed to be complete, the product may contain
		// terminal states. The SCC computer can correctly deal with that.

		if (product.getAcceptingStates().isEmpty()) {
			mainLog.print("None of the states in the product are accepting, skipping further computations");
			// If there are no accepting states, there is no accepting run, return all-false result
			// Note: In the dual case, where all states are accepting, we nevertheless have to do the
			// SCC analysis below, as there is no guarantee that there is actually a cycle (i.e., when
			// eventually all runs reach terminal states in the product)
			return StateValues.createFromBitSet(new BitSet(), model);
		}

		mainLog.print("Searching for non-trivial, accepting SCCs in product LTS...");
		mainLog.flush();
		SCCConsumerStore sccConsumerStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, product.getProductModel(), sccConsumerStore);
		sccComputer.computeSCCs();

		// We determine the SCCs that intersect F, as those are guaranteed to
		// have at least one accepting cycle. This crucially relies on the fact
		// that the SCC computer returns only non-trivial SCCs.
		int accepting = 0;
		int sccs = 0;
		BitSet acceptingSCCs = new BitSet();
		for (BitSet scc : sccConsumerStore.getSCCs()) {
			sccs++;
			if (scc.intersects(product.getAcceptingStates())) {
				acceptingSCCs.or(scc);
				accepting++;
			}
		}
		mainLog.println(" "+accepting+" of "+sccs+" non-trivial SCCs are accepting");

		BitSet allStates = new BitSet();
		allStates.set(0, product.getProductModel().getNumStates());

		// compute the set of states that can reach an accepting cycle,
		// i.e., satisfy E[ true U acceptingSCCs ], using the CTL checker
		mainLog.println("Computing reachability of accepting SCCs...");
		BitSet resultProduct = computeExistsUntil(product.getProductModel(), allStates, acceptingSCCs);
		StateValues svProduct = StateValues.createFromBitSet(resultProduct, product.getProductModel());

		// we project to the original model
		StateValues result = product.projectToOriginalModel(svProduct);

		return result;
	}

}

