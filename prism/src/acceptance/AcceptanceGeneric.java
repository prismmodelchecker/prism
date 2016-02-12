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
