//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package acceptance;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import acceptance.AcceptanceRabin.RabinPair;
import acceptance.AcceptanceStreett.StreettPair;
import prism.PrismException;
import prism.PrismNotSupportedException;
import jdd.JDDVars;

/**
 * A generic acceptance condition (based on BitSet state sets).
 * This is an AST of a boolean formula (conjunction and disjunction) over
 * atoms of the form Inf(states), Inf(!states), Fin(states) and Fin(!states).
 * <br>
 * Semantics:
 *  Inf(states)  <=> G F states
 *  Inf(!states) <=> G F !states
 *  Fin(states)  <=> F G !states
 *  Fin(!states) <=> F G states
 */
public class AcceptanceGeneric implements AcceptanceOmega {
	
	/** The types of elements in the AST */
	public enum ElementType {
		FALSE,
		TRUE, 

		OR,
		AND,

		INF,
		FIN,
		INF_NOT,
		FIN_NOT;
	}
	
	/** The type of this node in the AST */
	private ElementType kind;

	/** The left child (if it exists) */ 
	private AcceptanceGeneric left = null;

	/** The right child (if it exists) */ 
	private AcceptanceGeneric right = null;

	/** The set of states (if this is one of INF, FIN, INF_NOT, FIN_NOT) */
	private BitSet states = null;

	/**
	 * Constructor for TRUE or FALSE
	 * @param value true or false?
	 */
	public AcceptanceGeneric(boolean value) {
		kind = value ? ElementType.TRUE : ElementType.FALSE;
	}
	
	/** 
	 * Constructor for an INF, FIN, INF_NOT or FIN_NOT element. 
	 */
	public AcceptanceGeneric(ElementType kind, BitSet states) {
		this.kind = kind;

		this.states = states;
	}

	/**
	 * Constructor for a binary operator (AND/OR).
	 * @param kind
	 * @param left
	 * @param right
	 */
	public AcceptanceGeneric(ElementType kind, AcceptanceGeneric left, AcceptanceGeneric right) {
		this.kind = kind;
		this.left = left;
		this.right = right;
	}

	/** Get the ElementType of this AST element */
	public ElementType getKind() {
		return kind;
	}

	/** Get the left child of this AST element */
	public AcceptanceGeneric getLeft() {
		return left;
	}

	/** Get the right child of this AST element */
	public AcceptanceGeneric getRight() {
		return right;
	}

	/** Get the state set of this element (if kind is one of INF, FIN, INF_NOT, FIN_NOT).
	 */
	public BitSet getStates() {
		return states;
	}

	@Override
	public boolean isBSCCAccepting(BitSet bscc) {
		switch(kind) {
			case TRUE: return true;
			case FALSE: return false;
			case AND: return left.isBSCCAccepting(bscc) && right.isBSCCAccepting(bscc);
			case OR: return left.isBSCCAccepting(bscc) || right.isBSCCAccepting(bscc);
			case INF:
				// bscc |= G F states?
				// there exists a state in bscc and states
				return bscc.intersects(states);
			case INF_NOT: {
				// bscc_state |= G F !states?
				// the BSCC does not consist only of states
				BitSet bs = (BitSet) bscc.clone();
				bs.andNot(states);
				return !bs.isEmpty();
			}
			case FIN: {
				// bscc |= F G !states?
				// <=> there exists no states state in BSCC
				return !bscc.intersects(states);
			}
			case FIN_NOT: {
				// bscc |= F G states?
				// the BSCC consists entirely of states
				BitSet bs = (BitSet) bscc.clone();
				bs.and(states);
				return bs.equals(bscc);
			}
		}
		return false;
	}

	/** Get a list of all the (non-true/false) leaf nodes in this acceptance condition */
	public List<AcceptanceGeneric> getLeafNodes()
	{
		switch (getKind()) {
		case AND:
		case OR: {
			List<AcceptanceGeneric> result = new ArrayList<AcceptanceGeneric>();
			result.addAll(left.getLeafNodes());
			result.addAll(right.getLeafNodes());
			return result;
		}
		case TRUE:
		case FALSE:
			return Collections.emptyList();
		case FIN:
		case FIN_NOT:
		case INF:
		case INF_NOT:
			return Collections.singletonList(this);
		}
		throw new UnsupportedOperationException("Unknown kind");
	}

	@Override
	public String getSignatureForState(int stateIndex) {
		List<AcceptanceGeneric> leafNodes = getLeafNodes();

		String result = "";
		for (int i=0; i < leafNodes.size(); i++) {
			if (leafNodes.get(i).getStates().get(stateIndex)) {
				result += (result.isEmpty() ? "" : ",")+i;
			}
		}

		result = "{" + result + "}";
		return result;
	}

	@Override
	public String getSignatureForStateHOA(int stateIndex) {
		List<AcceptanceGeneric> leafNodes = getLeafNodes();

		String result = "";
		for (int i=0; i < leafNodes.size(); i++) {
			if (leafNodes.get(i).getStates().get(stateIndex)) {
				result += (result.isEmpty() ? "" : " ")+i;
			}
		}

		if (!result.isEmpty())
			result = "{" + result + "}";
		return result;
	}

	@Override
	public String getSizeStatistics() {
		return "generic acceptance with " + countAcceptanceSets() + " acceptance sets";
	}

	@Override
	public AcceptanceType getType() {
		return AcceptanceType.GENERIC;
	}

	@Override
	@Deprecated
	public String getTypeAbbreviated() {
		return getType().getNameAbbreviated();
	}

	@Override
	@Deprecated
	public String getTypeName() {
		return getType().getName();
	}

	@Override
	public AcceptanceGeneric clone() {
		switch (kind) {
			case FIN:
			case FIN_NOT:
			case INF:
			case INF_NOT:
				return new AcceptanceGeneric(kind, states);
			case AND:
			case OR:
				return new AcceptanceGeneric(kind, left.clone(), right.clone());
			case FALSE:
				return new AcceptanceGeneric(false);
			case TRUE:
				return new AcceptanceGeneric(true);
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance condition");
	}

	/**
	 * Returns a new generic acceptance condition that corresponds to the conjunction
	 * of this and the other acceptance condition. Both conditions are <b>not</b>
	 * cloned; take care not to modify the conditions or clone beforehand.
	 * @param other the other generic acceptance condition
	 * @return new AcceptanceRabin, conjunction of this and other
	 */
	public AcceptanceGeneric and(AcceptanceGeneric other)
	{
		return new AcceptanceGeneric(ElementType.AND, this, other);
	}

	/**
	 * Returns a new generic acceptance condition that corresponds to the disjunction
	 * of this and the other acceptance condition. Both conditions are <b>not</b>
	 * cloned; take care not to modify the conditions or clone beforehand.
	 * @param other the other generic acceptance condition
	 * @return new AcceptanceGeneric, disjunction of this and other
	 */
	public AcceptanceGeneric or(AcceptanceGeneric other)
	{
		return new AcceptanceGeneric(ElementType.OR, this, other);
	}

	/** Complement this acceptance condition, return as AcceptanceGeneric. */
	public AcceptanceGeneric complementToGeneric()
	{
		switch (kind) {
		case TRUE: return new AcceptanceGeneric(false);
		case FALSE:  return new AcceptanceGeneric(true);

		case AND:
			return new AcceptanceGeneric(ElementType.OR,
			                             getLeft().complementToGeneric(),
			                             getRight().complementToGeneric());
		case OR:
			return new AcceptanceGeneric(ElementType.AND,
                        	             getLeft().complementToGeneric(),
                        	             getRight().complementToGeneric());
		case FIN:
			return new AcceptanceGeneric(ElementType.INF, (BitSet) states.clone());
		case FIN_NOT:
			return new AcceptanceGeneric(ElementType.INF_NOT, (BitSet) states.clone());
		case INF:
			return new AcceptanceGeneric(ElementType.FIN, (BitSet) states.clone());
		case INF_NOT:
			return new AcceptanceGeneric(ElementType.FIN_NOT, (BitSet) states.clone());
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public AcceptanceOmega complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return this.complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to required acceptance type");
	}

	@Override
	public void lift(LiftBitSet lifter) {
		switch(kind) {
			case TRUE:
			case FALSE:
				return;
			case INF:
			case INF_NOT:
			case FIN:
			case FIN_NOT:
				states = lifter.lift(states);
				return;
			case AND:
			case OR:
				left.lift(lifter);
				right.lift(lifter);
				return;
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance condition");
	}

	/** Count the number of state sets in this acceptance condition */
	public int countAcceptanceSets() {
		switch(kind) {
			case FALSE:
			case TRUE:
				return 0;
			case INF:
			case FIN: 
			case INF_NOT:
			case FIN_NOT:
				return 1;
			case OR:
			case AND: 
				return left.countAcceptanceSets() + right.countAcceptanceSets();
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance condition");
	}

	@Override
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars) {
		return new AcceptanceGenericDD(this, ddRowVars);
	}

	@Override
	public AcceptanceGeneric toAcceptanceGeneric()
	{
		return this.clone();
	}

	/**
	 * Attempt to convert this generic acceptance condition
	 * to a Rabin condition. If this condition does not have
	 * the required form (top-level disjunctions with conjunctive pairs),
	 * returns {@code null}.
	 * <br>
	 * If the condition does not syntactically have the required
	 * from but can be easily converted to one (e.g., by converting
	 * a single Inf(S) to a Fin(emptyset) & Inf(S) etc) or by swapping
	 * Inf and Fin, this will be done.
	 * <br>
	 * The BitSets for the state sets are cloned, i.e., modifications of
	 * the state sets of the returned acceptance condition will not affect
	 * the original generic acceptance condition.
	 *
	 * @param numStates the number of states in the underlying model/automaton (for generating state sets for true)
	 * @return the generated AcceptanceRabin condition, or false on failure
	 */
	public AcceptanceRabin toAcceptanceRabin(int numStates)
	{
		AcceptanceRabin result = new AcceptanceRabin();
		List<AcceptanceGeneric> disjuncts = extractDisjuncts();

		for (AcceptanceGeneric term : disjuncts) {
			RabinPair pair = term.toAcceptanceRabinPair(numStates);
			if (pair == null) return null;
			result.add(pair);
		}

		return result;
	}

	/**
	 * Attempt to convert this generic acceptance to a Rabin pair.
	 *
	 * @param numStates the number of states in the underlying model/automaton (for generating state sets for true)
	 * @return the generated RabinPair, or false on failure
	 */
	private RabinPair toAcceptanceRabinPair(int numStates)
	{
		BitSet L = null, K = null;
		switch (getKind()) {
		case TRUE:
			L = new BitSet();
			K = new BitSet();
			K.set(0, numStates);
			break;
		case FALSE:
			L = new BitSet();
			L.set(0, numStates);
			K = new BitSet();
			break;
		case AND: {
			AcceptanceGeneric left = getLeft();
			AcceptanceGeneric right = getRight();

			if (left.getKind() == ElementType.INF || left.getKind() == ElementType.INF_NOT) {
				// swap
				AcceptanceGeneric tmp = left;
				left = right;
				right = tmp;
			}

			switch (left.getKind()) {
			case AND:
			case OR:
			case FALSE:
			case TRUE:
			case INF:
			case INF_NOT:
				// not a Rabin condition
				return null;
			case FIN:
				// Fin(A) <=> <>[]!A
				// L = A
				L = (BitSet) left.getStates().clone();
				break;
			case FIN_NOT:
				// Fin(!A) <=> <>[]A
				// L = !A
				L = (BitSet) left.getStates().clone();
				L.flip(0, numStates);
				break;
			}

			switch (right.getKind()) {
			case AND:
			case OR:
			case FALSE:
			case TRUE:
			case FIN:
			case FIN_NOT:
				// not a Rabin condition
				return null;
			case INF:
				// Inf(A) <=> []<>A
				// K = A
				K = (BitSet) right.getStates().clone();
				break;
			case INF_NOT:
				// Inf(!A) <=> []<>!A
				// K = !A
				K = (BitSet) right.getStates().clone();
				K.flip(0, numStates);
				break;
			}

			break;
		}
		case FIN:
			// Fin(A) <=> <>[]!A & []<> true <=> RabinPair(A, true)
			// L = A
			L = (BitSet) getStates().clone();
			// K = true
			K = new BitSet();
			K.set(0, numStates);
			break;
		case FIN_NOT:
			// Fin(!A) <=> <>[]A & []<> true <=> RabinPair(!A, true)
			// L = !A
			L = (BitSet) getStates().clone();
			L.flip(0, numStates);
			// K = true
			K = new BitSet();
			K.set(0, numStates);
			break;
		case INF:
			// Inf(A) <=> <>[]!false & []<> A <=> RabinPair(false, A)
			// L = false
			L = new BitSet();
			// K = A
			K = (BitSet) getStates().clone();
			break;
		case INF_NOT:
			// Inf(!A) <=> <>[]!false & []<> !A <=> RabinPair(false, !A)
			// L = false
			L = new BitSet();
			// K = !A
			K = (BitSet) getStates().clone();
			K.flip(0, numStates);
			break;
		case OR:
			// not a Rabin pair
			return null;
		}

		return new AcceptanceRabin.RabinPair(L, K);
	}

	/**
	 * Attempt to convert this generic acceptance condition
	 * to a Streett condition. If this condition does not have
	 * the required form (top-level conjunction with disjunctive pairs),
	 * returns {@code null}.
	 * <br>
	 * If the condition does not syntactically have the required
	 * from but can be easily converted to one (e.g., by converting
	 * a single Inf(S) to a Fin(true) | Inf(S) etc) or by swapping
	 * Inf and Fin, this will be done.
	 * <br>
	 * The BitSets for the state sets are cloned, i.e., modifications of
	 * the state sets of the returned acceptance condition will not affect
	 * the original generic acceptance condition.
	 *
	 * @param numStates the number of states in the underlying model/automaton (for generating state sets for true)
	 * @return the generated AcceptanceRabin condition, or false on failure
	 */
	public AcceptanceStreett toAcceptanceStreett(int numStates)
	{
		AcceptanceStreett result = new AcceptanceStreett();
		List<AcceptanceGeneric> conjuncts = extractConjuncts();

		for (AcceptanceGeneric term : conjuncts) {
			StreettPair pair = term.toAcceptanceStreettPair(numStates);
			if (pair == null) return null;
			result.add(pair);
		}

		return result;
	}

	/**
	 * Attempt to convert this generic acceptance to a Streett pair.
	 *
	 * @param numStates the number of states in the underlying model/automaton (for generating state sets for true)
	 * @return the generated RabinPair, or false on failure
	 */
	private StreettPair toAcceptanceStreettPair(int numStates)
	{
		BitSet R = null, G = null;
		switch (getKind()) {
		case TRUE:
			// true = []<> false -> []<> false
			R = new BitSet();
			G = new BitSet();
			break;
		case FALSE:
			// false = []<> true -> []<> false
			R = new BitSet();
			R.set(0, numStates);
			G = new BitSet();
			break;
		case OR: {
			AcceptanceGeneric left = getLeft();
			AcceptanceGeneric right = getRight();

			if (left.getKind() == ElementType.INF || left.getKind() == ElementType.INF_NOT) {
				// swap
				AcceptanceGeneric tmp = left;
				left = right;
				right = tmp;
			}

			switch (left.getKind()) {
			case AND:
			case OR:
			case FALSE:
			case TRUE:
			case INF:
			case INF_NOT:
				// not a Streett condition
				return null;
			case FIN:
				// Fin(A) -> R = A
				R = (BitSet) left.getStates().clone();
				break;
			case FIN_NOT:
				// Fin(!A) -> R = !A
				R = (BitSet) left.getStates().clone();
				R.flip(0, numStates);
				break;
			}

			switch (right.getKind()) {
			case AND:
			case OR:
			case FALSE:
			case TRUE:
			case FIN:
			case FIN_NOT:
				// not a Streett condition
				return null;
			case INF:
				// Inf(A) -> G = A
				G = (BitSet) right.getStates().clone();
				break;
			case INF_NOT:
				// Inf(!A) -> G = !A
				G = (BitSet) right.getStates().clone();
				G.flip(0, numStates);
				break;
			}

			break;
		}
		case FIN:
			// Fin(A) <=> []<>A -> []<>false
			// R = A
			R = (BitSet) getStates().clone();
			// G = false
			G = new BitSet();
			break;
		case FIN_NOT:
			// Fin(!A) <=> []<>!A -> []<>false
			// R = !A
			R = (BitSet) getStates().clone();
			R.flip(0, numStates);
			// G = false
			G = new BitSet();
			break;
		case INF:
			// Inf(A) <=> []<>true -> []<>A
			// R = true
			R = new BitSet();
			R.set(0, numStates);
			// G = A
			G = (BitSet) getStates().clone();
			break;
		case INF_NOT:
			// Inf(!A) <=> []<>true -> []<>!A
			// R = true
			R = new BitSet();
			R.set(0, numStates);
			// G = !A
			G = (BitSet) getStates().clone();
			G.flip(0, numStates);
			break;
		case AND:
			// not a Streett pair
			return null;
		}

		return new AcceptanceStreett.StreettPair(R,G);
	}

	/**
	 * Extract the operands of the top-level disjunctions, e.g.,
	 * for (A & B) | (C | D)) would return the list [A&B,C,D].
	 */
	public List<AcceptanceGeneric> extractDisjuncts()
	{
		ArrayList<AcceptanceGeneric> result = new ArrayList<AcceptanceGeneric>();
		Stack<AcceptanceGeneric> todo = new Stack<AcceptanceGeneric>();

		todo.add(this);
		while (!todo.isEmpty()) {
			AcceptanceGeneric current = todo.pop();
			switch (current.getKind()) {
			case AND:
			case FALSE:
			case TRUE:
			case FIN:
			case FIN_NOT:
			case INF:
			case INF_NOT:
				// not a top level disjunction, add to list
				result.add(current);
				break;
			case OR:
				// still a top level disjunction, recurse
				todo.push(current.getRight());
				todo.push(current.getLeft());
				break;
			}
		}

		return result;
	}

	/**
	 * Extract the operands of the top-level conjunctions, e.g.,
	 * for (A | B) & (C & D)) would return the list [A|B,C,D].
	 */
	public List<AcceptanceGeneric> extractConjuncts()
	{
		ArrayList<AcceptanceGeneric> result = new ArrayList<AcceptanceGeneric>();
		Stack<AcceptanceGeneric> todo = new Stack<AcceptanceGeneric>();

		todo.add(this);
		while (!todo.isEmpty()) {
			AcceptanceGeneric current = todo.pop();
			switch (current.getKind()) {
			case OR:
			case FALSE:
			case TRUE:
			case FIN:
			case FIN_NOT:
			case INF:
			case INF_NOT:
				// not a top level conjunction, add to list
				result.add(current);
				break;
			case AND:
				// still a top level conjunction, recurse
				todo.push(current.getRight());
				todo.push(current.getLeft());
				break;
			}
		}

		return result;
	}

	@Override
	public String toString() {
		switch(kind) {
			case TRUE:
				return "true";
			case FALSE:
				return "false";
			case AND:
				return  "(" + left.toString() + " & " + right.toString() + ")";
			case OR:
				return  "(" + left.toString() + " | " + right.toString() + ")";
			case INF:
				return "Inf(" + states.toString() + ")";
			case FIN:
				return "Fin(" + states.toString() + ")";
			case INF_NOT:
				return "Inf(!" + states.toString() + ")";
			case FIN_NOT:
				return "Fin(!" + states.toString() + ")";
			default:
				return null;
		}
	}

	@Override
	public void outputHOAHeader(PrintStream out)
	{
		List<AcceptanceGeneric> leafNodes = getLeafNodes();
		out.print("Acceptance: "+leafNodes.size()+" ");
		outputHOAFormula(out, 0);
		out.println();
	}

	private int outputHOAFormula(PrintStream out, int nextSetIndex)
	{
		switch (kind) {
		case AND:
			out.print("(");
			nextSetIndex = left.outputHOAFormula(out, nextSetIndex);
			out.print(")&(");
			nextSetIndex = right.outputHOAFormula(out, nextSetIndex);
			out.print(")");
			return nextSetIndex;
		case OR:
			out.print("(");
			nextSetIndex = left.outputHOAFormula(out, nextSetIndex);
			out.print(")|(");
			nextSetIndex = right.outputHOAFormula(out, nextSetIndex);
			out.print(")");
			return nextSetIndex;
		case TRUE:
			out.print("t");
			return nextSetIndex;
		case FALSE:
			out.print("f");
			return nextSetIndex;
		case FIN:
			out.print("Fin("+nextSetIndex+")");
			return nextSetIndex+1;
		case FIN_NOT:
			out.print("Fin(!"+nextSetIndex+")");
			return nextSetIndex+1;
		case INF:
			out.print("Inf("+nextSetIndex+")");
			return nextSetIndex+1;
		case INF_NOT:
			out.print("Inf(!"+nextSetIndex+")");
			return nextSetIndex+1;
		}
		throw new UnsupportedOperationException("Unknown kind");
	}


}
