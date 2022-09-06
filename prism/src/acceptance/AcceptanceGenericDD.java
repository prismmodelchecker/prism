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

import common.IterableBitSet;
import acceptance.AcceptanceGeneric.ElementType;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismNotSupportedException;

/**
 * A generic acceptance condition (based on JDD state sets).
 * This is an AST of a boolean formula (conjunction and disjunction) over
 * atoms of the form Inf(states), Inf(!states), Fin(states) and Fin(!states).
 * <br>
 * Semantics:
 *  Inf(states)  <=> G F states
 *  Inf(!states) <=> G F !states
 *  Fin(states)  <=> F G !states
 *  Fin(!states) <=> F G states
 */
public class AcceptanceGenericDD implements AcceptanceOmegaDD {

	private ElementType kind;
	private AcceptanceGenericDD left = null;
	private AcceptanceGenericDD right = null;
	private JDDNode states = null;

	public AcceptanceGenericDD(AcceptanceGeneric acceptance, JDDVars ddRowVars)
	{
		switch(acceptance.getKind()) {
			case AND:
			case OR:
				kind = acceptance.getKind();
				left = (AcceptanceGenericDD) acceptance.getLeft().toAcceptanceDD(ddRowVars);
				right = (AcceptanceGenericDD) acceptance.getRight().toAcceptanceDD(ddRowVars);
				return;
			case TRUE:
				kind = ElementType.TRUE;
				return;
			case FALSE:
				kind = ElementType.FALSE;
				return;
			case INF:
			case INF_NOT:
			case FIN:
			case FIN_NOT:
				kind = acceptance.getKind();
				states = JDD.Constant(0);
				for (int i : IterableBitSet.getSetBits(acceptance.getStates())) {
					states = JDD.SetVectorElement(states, ddRowVars, i, 1.0);
				}
				return;
		}
		throw new UnsupportedOperationException("Unsupported operatator in generic acceptance condition");
	}

	/**
	 * Constructor for TRUE or FALSE
	 * @param value true or false?
	 */
	public AcceptanceGenericDD(boolean value) {
		kind = value ? ElementType.TRUE : ElementType.FALSE;
	}

	/**
	 * Constructor for an INF, FIN, INF_NOT or FIN_NOT element.
	 * <br>[ STORES: <i>states</i> ]
	 */
	public AcceptanceGenericDD(ElementType kind, JDDNode states)
	{
		this.kind = kind;
		left = null;
		right = null;
		this.states = states;
	}

	/**
	 * Constructor for a binary operator (AND/OR).
	 * <br>[ STORES: <i>left, right operand</i> ]
	 */
	public AcceptanceGenericDD(ElementType kind, AcceptanceGenericDD left, AcceptanceGenericDD right)
	{
		this.kind = kind;
		this.left = left;
		this.right = right;
		states = null;
	}

	/** Get the ElementType of this AST element */
	public ElementType getKind()
	{
		return kind;
	}

	/** Get the left child of this AST element */
	public AcceptanceGenericDD getLeft()
	{
		return left;
	}

	/** Get the right child of this AST element */
	public AcceptanceGenericDD getRight()
	{
		return right;
	}

	/** Get a referenced copy of the state sets (if kind is one of INF, FIN, INF_NOT, FIN_NOT).
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getStates()
	{
		if (states != null) {
			JDD.Ref(states);
		}
		return states;
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc)
	{
		switch(kind) {
		case TRUE: 
			return true;
		case FALSE: 
			return false;
		case AND: 
			return left.isBSCCAccepting(bscc) && right.isBSCCAccepting(bscc);
		case OR: 
			return left.isBSCCAccepting(bscc) || right.isBSCCAccepting(bscc);
		case INF:
			// bscc |= G F states?
			// there exists a state in bscc and states
			return JDD.AreIntersecting(states, bscc);
		case INF_NOT:
			// bscc_state |= G F !states?
			// the BSCC intersects Not(states)
			JDD.Ref(states);
			return JDD.AreIntersecting(JDD.Not(states), bscc);
		case FIN:
			// bscc |= F G !states?
			// the BSCC consists only of !states
			return !JDD.AreIntersecting(states, bscc);
		case FIN_NOT:
			// bscc |= F G states?
			// the BSCC consists entirely of states
			JDD.Ref(states);
			return !JDD.AreIntersecting(JDD.Not(states), bscc);
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
	}

	@Override
	public void intersect(JDDNode restrict)
	{
		switch(kind) {
		case TRUE:
		case FALSE:
			return;
		case AND:
		case OR:
			left.intersect(restrict);
			right.intersect(restrict);
			return;
		case INF:
		case INF_NOT:
		case FIN:
		case FIN_NOT:
			states = JDD.And(states, restrict.copy());
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
	}


	@Override
	public AcceptanceGenericDD clone()
	{
		switch(kind) {
		case TRUE:
			return new AcceptanceGenericDD(true);
		case FALSE:
			return new AcceptanceGenericDD(false);
		case AND:
			return new AcceptanceGenericDD(ElementType.AND, left.clone(), right.clone());
		case OR:
			return new AcceptanceGenericDD(ElementType.OR, left.clone(), right.clone());
		case INF:
		case INF_NOT:
		case FIN:
		case FIN_NOT:
			return new AcceptanceGenericDD(kind, states.copy());
		}
		throw new UnsupportedOperationException("Unsupported operatator in generic acceptance condition");
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
	public AcceptanceOmegaDD complement(AcceptanceType... allowedAcceptance) throws PrismNotSupportedException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return this.complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to required acceptance type");
	}

	/** Complement this acceptance condition, return as AcceptanceGeneric. */
	@Override
	public AcceptanceGenericDD complementToGeneric()
	{
		// overrides the default implementation in AcceptanceOmegaDD, as it's the base case
		// that is used there

		switch (kind) {
		case TRUE:
			return new AcceptanceGenericDD(false);
		case FALSE:
			return new AcceptanceGenericDD(true);
		case AND:
			return new AcceptanceGenericDD(ElementType.OR,
			                               getLeft().complementToGeneric(),
			                               getRight().complementToGeneric());
		case OR:
			return new AcceptanceGenericDD(ElementType.AND,
			                               getLeft().complementToGeneric(),
			                               getRight().complementToGeneric());
		case FIN:
			return new AcceptanceGenericDD(ElementType.INF, states.copy());
		case FIN_NOT:
			return new AcceptanceGenericDD(ElementType.INF_NOT, states.copy());
		case INF:
			return new AcceptanceGenericDD(ElementType.FIN, states.copy());
		case INF_NOT:
			return new AcceptanceGenericDD(ElementType.FIN_NOT, states.copy());
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public AcceptanceGenericDD toAcceptanceGeneric()
	{
		return this.clone();
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
	public void clear() {
		switch (kind) {
			case TRUE:
			case FALSE:
				return;
			case AND:
			case OR:
				left.clear();
				right.clear();
				return;
			case INF_NOT:
			case FIN:
			case FIN_NOT:
			case INF:
				if (states != null) JDD.Deref(states);
				states = null;
				return;
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
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
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
	}

}
