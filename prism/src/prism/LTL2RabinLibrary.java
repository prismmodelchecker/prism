//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package prism;

import java.io.*;
import java.util.*;

import acceptance.AcceptanceRabin;
import acceptance.AcceptanceRabin.RabinPair;
import parser.Values;
import parser.ast.*;
import parser.visitor.ASTTraverse;
import parser.visitor.ASTTraverseModify;

/**
 * LTL-to-DRA conversion via
 * <ol>
 *  <li> hard-coded DRA for special formulas </li>
 *  <li> direct translation into DRA for simple path formulas with temporal bounds</li>
 * </ol>
 */
public class LTL2RabinLibrary
{
	private static ArrayList<String> labels;

	private static HashMap<String, String> dras;
	static {
		// Hard-coded DRA descriptions for various LTL formulas 
		dras = new HashMap<String, String>();
		dras.put("F \"L0\"", "2 states (start 0), 1 labels: 0-{}->0 0-{0}->1 1-{}->1 1-{0}->1; 1 acceptance pairs: ({},{1})");
		dras.put("G \"L0\"", "2 states (start 0), 1 labels: 0-{}->1 0-{0}->0 1-{}->1 1-{0}->1; 1 acceptance pairs: ({1},{0})");
		dras.put("G F \"L0\"", "2 states (start 0), 1 labels: 0-{0}->1 0-{}->0 1-{0}->1 1-{}->0; 1 acceptance pairs: ({},{1})");
		dras.put("!(G F \"L0\")", "2 states (start 0), 1 labels: 0-{}->1 0-{0}->0 1-{}->1 1-{0}->0; 1 acceptance pairs: ({0},{1})");
		dras.put("F G \"L0\"", "2 states (start 0), 1 labels: 0-{0}->1 0-{}->0 1-{0}->1 1-{}->0; 1 acceptance pairs: ({0},{1})");
		dras.put("!(F G \"L0\")", "2 states (start 0), 1 labels: 0-{}->1 0-{0}->0 1-{}->1 1-{0}->0; 1 acceptance pairs: ({},{1})");
		//dras.put("F (\"L0\"&(X \"L1\"))", "3 states (start 2), 2 labels: 0-{1}->0 0-{0, 1}->0 0-{}->0 0-{0}->0 1-{1}->0 1-{0, 1}->0 1-{}->2 1-{0}->1 2-{1}->2 2-{0, 1}->1 2-{}->2 2-{0}->1; 1 acceptance pairs: ({},{0})");
		//dras.put("!(F (\"L0\"&(X \"L1\")))", "4 states (start 3), 2 labels: 0-{1}->0 0-{0, 1}->1 0-{}->0 0-{0}->1 1-{1}->2 1-{0, 1}->2 1-{}->0 1-{0}->1 2-{1}->2 2-{0, 1}->2 2-{}->2 2-{0}->2 3-{1}->0 3-{0, 1}->1 3-{}->0 3-{0}->1; 1 acceptance pairs: ({2},{0, 1})");
		//dras.put("(X \"L0\")=>(G \"L1\")", "6 states (start 5), 2 labels: 0-{1}->0 0-{0, 1}->0 0-{}->0 0-{0}->0 1-{1}->0 1-{0, 1}->3 1-{}->0 1-{0}->4 2-{1}->0 2-{0, 1}->4 2-{}->0 2-{0}->4 3-{1}->3 3-{0, 1}->3 3-{}->4 3-{0}->4 4-{1}->4 4-{0, 1}->4 4-{}->4 4-{0}->4 5-{1}->1 5-{0, 1}->1 5-{}->2 5-{0}->2; 1 acceptance pairs: ({4},{0, 1, 2, 3})");
		//dras.put("!((X \"L0\")=>(G \"L1\"))", "6 states (start 5), 2 labels: 0-{1}->0 0-{0, 1}->0 0-{}->0 0-{0}->0 1-{1}->1 1-{0, 1}->1 1-{}->1 1-{0}->1 2-{1}->2 2-{0, 1}->2 2-{}->0 2-{0}->0 3-{1}->1 3-{0, 1}->0 3-{}->1 3-{0}->0 4-{1}->1 4-{0, 1}->2 4-{}->1 4-{0}->0 5-{1}->4 5-{0, 1}->4 5-{}->3 5-{0}->3; 1 acceptance pairs: ({1},{0})");
		//dras.put("(G !\"b\")&(G F \"a\")", "5 states (start 4), 2 labels: 0-{1}->0 0-{0, 1}->2 0-{}->1 0-{0}->2 1-{1}->3 1-{0, 1}->2 1-{}->4 1-{0}->2 2-{1}->2 2-{0, 1}->2 2-{}->2 2-{0}->2 3-{1}->0 3-{0, 1}->2 3-{}->1 3-{0}->2 4-{1}->3 4-{0, 1}->2 4-{}->4 4-{0}->2; 1 acceptance pairs: ({2},{0, 1})");
		//dras.put("(G \"L0\")&(G F \"L1\")", "5 states (start 4), 2 labels: 0-{1}->2 0-{0, 1}->0 0-{}->2 0-{0}->1 1-{1}->2 1-{0, 1}->3 1-{}->2 1-{0}->4 2-{1}->2 2-{0, 1}->2 2-{}->2 2-{0}->2 3-{1}->2 3-{0, 1}->0 3-{}->2 3-{0}->1 4-{1}->2 4-{0, 1}->3 4-{}->2 4-{0}->4; 1 acceptance pairs: ({2},{0, 1})");
		//dras.put("(G (\"L0\"=>(F \"L1\")))&(F G \"L2\")", "7 states (start 3), 3 labels: 0-{1, 2}->1 0-{0, 1, 2}->1 0-{2}->0 0-{0, 2}->0 0-{1}->3 0-{0, 1}->3 0-{}->4 0-{0}->4 1-{1, 2}->6 1-{0, 1, 2}->6 1-{2}->6 1-{0, 2}->5 1-{1}->3 1-{0, 1}->3 1-{}->3 1-{0}->4 2-{1, 2}->1 2-{0, 1, 2}->1 2-{2}->1 2-{0, 2}->0 2-{1}->3 2-{0, 1}->3 2-{}->3 2-{0}->4 3-{1, 2}->2 3-{0, 1, 2}->2 3-{2}->2 3-{0, 2}->4 3-{1}->3 3-{0, 1}->3 3-{}->3 3-{0}->4 4-{1, 2}->2 4-{0, 1, 2}->2 4-{2}->4 4-{0, 2}->4 4-{1}->3 4-{0, 1}->3 4-{}->4 4-{0}->4 5-{1, 2}->1 5-{0, 1, 2}->1 5-{2}->0 5-{0, 2}->0 5-{1}->3 5-{0, 1}->3 5-{}->4 5-{0}->4 6-{1, 2}->6 6-{0, 1, 2}->6 6-{2}->6 6-{0, 2}->5 6-{1}->3 6-{0, 1}->3 6-{}->3 6-{0}->4; 1 acceptance pairs: ({2, 3, 4},{5, 6})");
		//dras.put("!((G \"L0\")&(G F \"L1\"))", "4 states (start 3), 2 labels: 0-{1}->1 0-{0, 1}->3 0-{}->1 0-{0}->0 1-{1}->1 1-{0, 1}->1 1-{}->1 1-{0}->1 2-{1}->1 2-{0, 1}->3 2-{}->1 2-{0}->0 3-{1}->1 3-{0, 1}->3 3-{}->1 3-{0}->2; 2 acceptance pairs: ({},{1}) ({1, 2, 3},{0})"); 
	}

	/**
	 * Attempts to convert an LTL formula into a DRA by direct translation methods of the library:
	 * <ul>
	 * <li>First, look up hard-coded DRA from the DRA map.</li>
	 * <li>Second, if the formula is a simple path formula with temporal bounds, use the special
	 *     constructions {@code constructDRAFor....}
	 * </ul>
	 * Return {@code null} if the automaton can not be constructed using the library.
	 * <br/> The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 * @param ltl the LTL formula
	 * @param constants values for constants in the formula (may be {@code null})
	 */
	public static DA<BitSet, AcceptanceRabin> getDRAforLTL(Expression ltl, Values constants) throws PrismException {
		// Get list of labels appearing
		labels = new ArrayList<String>();
		ltl.accept(new ASTTraverse()
		{
			public Object visit(ExpressionLabel e) throws PrismLangException
			{
				labels.add(e.getName());
				return null;
			}
		});

		// On a copy of formula, rename labels to "L0", "L1", "L2", ...
		// (for looking up LTL formula in table of strings)
		Expression ltl2 = ltl.deepCopy();
		ltl2.accept(new ASTTraverseModify()
		{
			public Object visit(ExpressionLabel e) throws PrismLangException
			{
				int i = labels.indexOf(e.getName());
				//char letter = (char) ('a' + i);
				return new ExpressionLabel("L" + i);
			}
		});

		// See if we have a hard-coded DRA to return
		String draString = dras.get(ltl2.toString());
		if (draString != null)
			return createDRAFromString(draString, labels);
		
		
		// handle simple until formula with time bounds
		if (Expression.containsTemporalTimeBounds(ltl)) {
			boolean negated = false;
			
			if (ltl instanceof ExpressionUnaryOp &&
			    ((ExpressionUnaryOp)ltl).getOperator() == ExpressionUnaryOp.NOT) {
				// negated
				negated = true;
				ltl = ((ExpressionUnaryOp)ltl).getOperand();
			}
			
			if (ltl instanceof ExpressionTemporal &&
			    ((ExpressionTemporal)ltl).getOperator() == ExpressionTemporal.P_U) {
				return constructDRAForSimpleUntilFormula((ExpressionTemporal)ltl, constants, negated);				
			} else {
				throw new PrismException("Unsupported LTL formula with time bounds: "+ltl);
			}
		}
		
		// No time-bounded operators, do not convert using the library
		return null;
	}
	
	/**
	 * Construct a prism.DA<BitSet,AcceptanceRabin> for the given until formula.
	 * The expression is expected to have the form a U b, where
	 * a and b are either ExpressionLabels or true/false.
	 * The operator can have integer bounds.
	 * @param expr the until formula
	 * @param constants values for constants (in bounds)
	 * @param negated create DRA for the complement, i.e., !(a U b)
	 */
	public static DA<BitSet,AcceptanceRabin> constructDRAForSimpleUntilFormula(ExpressionTemporal expr, Values constants, boolean negated) throws PrismException
	{
		IntegerBound bounds;
		DA<BitSet,AcceptanceRabin> dra;

		if (expr.getOperator() != ExpressionTemporal.P_U) {
			throw new PrismException("Not an Until operator!");
		}

		// get and check bounds information (non-negative, non-empty)
		bounds = IntegerBound.fromExpressionTemporal(expr, constants, true);

		// extract information about the operands of the until operator, either a label (extracted to labelA)
		// or true/false (stored in aBoolean, labelA=null).
		String labelA, labelB;
		boolean aBoolean = false, bBoolean = false;

		if (expr.getOperand1() instanceof ExpressionLabel) {
			labelA = ((ExpressionLabel)expr.getOperand1()).getName();
		} else {
			if (expr.getOperand1() instanceof ExpressionLiteral &&
			    ((ExpressionLiteral) expr.getOperand1()).getValue() instanceof Boolean) {
				labelA = null;
				aBoolean = (Boolean) ((ExpressionLiteral)expr.getOperand1()).getValue();
			} else {
				throw new PrismException("Unsupported expression "+expr.getOperand1()+" in formula.");
			}
		}

		// do the same for the second operand.
		if (expr.getOperand2() instanceof ExpressionLabel) {
			labelB = ((ExpressionLabel)expr.getOperand2()).getName();
		} else {
			if (expr.getOperand2() instanceof ExpressionLiteral &&
			    ((ExpressionLiteral) expr.getOperand2()).getValue() instanceof Boolean) {
				labelB = null;
				bBoolean = (Boolean) ((ExpressionLiteral)expr.getOperand2()).getValue();
			} else {
				throw new PrismException("Unsupported expression "+expr.getOperand2()+" in formula.");
			}
		}
		
		
		// handle the special cases where one of the operands is true / false
		if (labelA == null && labelB == null) {
			// bool U bool
			boolean untilIsTrue;
			
			if (bBoolean == false) {
				// ? U false <=> false
				untilIsTrue = false;
			} else { // bBoolean == true
				if (aBoolean == true) {
					// true U true <=> true
					untilIsTrue = true;
				} else {
					// false U_bounds true
					// depends on bound, is true if 0 is in bound, false otherwise
					untilIsTrue = bounds.isInBounds(0);
				}
			}
			
			dra = constructDRAForTrue(null);
			if (!untilIsTrue) {
				negated = !negated;
			}
		} else if (labelA == null) {
			// first operand is boolean, second is normal label, bool U b
			if (aBoolean) {
				// true U_bounds b <=> F_bounds b
				dra = constructDRAForFinally(labelB, false, bounds);
			} else {
				// false U_bounds b <=> b in first step and first step in bounds
				if (bounds.isInBounds(0)) {
					dra = constructDRAForInitialStateLabel(labelB);
				} else {
					// false
					dra = constructDRAForTrue(labelB);
					negated = !negated;
				}
			}
		} else if (labelB == null) {
			// second operand is boolean, first is normal label, a U bool
			if (!bBoolean) {
				// a U false <=> false
				dra = constructDRAForTrue(labelA);
				negated = !negated;
			} else {
				if ((!bounds.hasLowerBound()) ||
					(bounds.isInBounds(0))) {
					// a U_bounds true <=> true if there is no lower bound
					// or if lower bound is >=0
					dra = constructDRAForTrue(labelA);
				} else {
					//     a U>=lower true
					// <=> G<lower a
					// <=> !(F<lower !a)

					// newBounds: >=lower becomes <lower
					IntegerBound newBounds = new IntegerBound(null, false,
					                                            bounds.getLowestInteger(), true);
					dra = constructDRAForFinally(labelA, true, newBounds);
					negated = !negated;
				}
			}
		} else {
			// the general case, a U_bounds b
			dra = constructDRAForUntil(labelA, labelB, bounds);
		}

		
		if (negated) {
			// the constructed DRAs can be complemented by switching L and K
			BitSet accL = (BitSet) dra.getAcceptance().get(0).getL().clone();
			BitSet accK = (BitSet) dra.getAcceptance().get(0).getK().clone();
			
			dra.getAcceptance().get(0).getL().clear();
			dra.getAcceptance().get(0).getL().or(accK);
			
			dra.getAcceptance().get(0).getK().clear();
			dra.getAcceptance().get(0).getK().or(accL);
		}
		
		return dra;
	}
	/** Construct a DRA for a U_bounds b. Can be complemented by switching the acceptance sets. */
	public static DA<BitSet, AcceptanceRabin> constructDRAForUntil(String labelA, String labelB, IntegerBound bounds)
	{
		DA<BitSet, AcceptanceRabin> dra;
		List<String> apList = new ArrayList<String>();
		BitSet edge_ab, edge_Ab, edge_aB, edge_AB;
		BitSet accL, accK;

		int saturation = bounds.getMaximalInterestingValue();

		// [0,saturation] + yes + no
		int states = saturation + 3;

		apList.add(labelA);
		apList.add(labelB);

		dra = new DA<BitSet,AcceptanceRabin>(states);
		dra.setAcceptance(new AcceptanceRabin());
		dra.setAPList(apList);
		dra.setStartState(0);

		edge_ab = new BitSet();  // !a & !b
		edge_Ab = new BitSet();  //  a & !b
		edge_aB = new BitSet();  // !a &  b
		edge_AB = new BitSet();  //  a &  b
		edge_Ab.set(0);
		edge_aB.set(1);
		edge_AB.set(0);
		edge_AB.set(1);

		int yes_state = states - 2;
		int no_state = states - 1;

		for (int counter = 0; counter <= saturation; counter++) {
			int next_counter = counter + 1;
			if (next_counter > saturation) next_counter = saturation;

			if (bounds.isInBounds(counter)) {
				// b => yes
				dra.addEdge(counter, edge_aB, yes_state);
				dra.addEdge(counter, edge_AB, yes_state);

				// !a & !b => no
				dra.addEdge(counter, edge_ab, no_state);

				// a & !b => next_counter
				dra.addEdge(counter, edge_Ab, next_counter);
			} else {
				// !a => no
				dra.addEdge(counter, edge_aB, no_state);
				dra.addEdge(counter, edge_ab, no_state);

				// a => next_counter
				dra.addEdge(counter, edge_Ab, next_counter);
				dra.addEdge(counter, edge_AB, next_counter);
			}
		}

		// yes state = true loop
		dra.addEdge(yes_state, edge_ab, yes_state);
		dra.addEdge(yes_state, edge_aB, yes_state);
		dra.addEdge(yes_state, edge_Ab, yes_state);
		dra.addEdge(yes_state, edge_AB, yes_state);

		// no state = true loop
		dra.addEdge(no_state, edge_ab, no_state);
		dra.addEdge(no_state, edge_aB, no_state);
		dra.addEdge(no_state, edge_Ab, no_state);
		dra.addEdge(no_state, edge_AB, no_state);

		// acceptance =
		//   infinitely often yes_state,
		//   not infinitely often no_state or saturation state
		// this allows complementation by switching L and K.
		accL = new BitSet();
		accL.set(no_state);
		accL.set(saturation);
		accK = new BitSet();
		accK.set(yes_state);
		
		dra.getAcceptance().add(new AcceptanceRabin.RabinPair(accL, accK));
		
		return dra;
	}
	/**
	 * Construct a DRA for LTL formula "F_bounds a".
	 * If {@code negateA == true}, constructs DRA for "F_bounds !a".
	 * Can be complemented by switching the acceptance sets. */
	public static DA<BitSet,AcceptanceRabin> constructDRAForFinally(String labelA, boolean negateA, IntegerBound bounds)
	{
		DA<BitSet,AcceptanceRabin> dra;
		List<String> apList = new ArrayList<String>();
		BitSet edge_no, edge_yes;
		BitSet accL, accK;

		int saturation = bounds.getMaximalInterestingValue();
		
		// [0,saturation] + yes
		int states = saturation + 2;
		
		apList.add(labelA);
		
		dra = new DA<BitSet,AcceptanceRabin>(states);
		dra.setAcceptance(new AcceptanceRabin());
		dra.setAPList(apList);
		dra.setStartState(0);
		
		// edge labeled with the target label
		edge_yes  = new BitSet();
		// edge not labeled with the target label
		edge_no = new BitSet();
		
		if (negateA) {
			edge_no.set(0);  // no = a, yes = !a
		} else {
			edge_yes.set(0); // yes = a, no = !a
		}

		int yes_state = states - 1;

		for (int counter = 0; counter <= saturation; counter++) {
			int next_counter = counter + 1;
			if (next_counter > saturation) next_counter = saturation;
		
			if (bounds.isInBounds(counter)) {
				// yes => yes_state
				dra.addEdge(counter, edge_yes, yes_state);
			
				// no => next_counter
				dra.addEdge(counter, edge_no, next_counter);
			} else {
				// true => next_counter
				dra.addEdge(counter, edge_no, next_counter);
				dra.addEdge(counter, edge_yes, next_counter);
			}
		}

		// yes state = true loop
		dra.addEdge(yes_state, edge_no, yes_state);
		dra.addEdge(yes_state, edge_yes, yes_state);

		// acceptance =
		//   infinitely often yes_state,
		//   not infinitely often saturation state
		// this allows complementation by switching L and K.
		accL = new BitSet();
		accL.set(saturation);
		accK = new BitSet();
		accK.set(yes_state);
		
		dra.getAcceptance().add(new RabinPair(accL, accK));
		
		return dra;
	}
	/**
	 * Construct a DRA that always accepts.
	 * If {@code label == null}, then there is no label, if {@code label != null} then
	 * there is a single label in the set of atomic propositions.
	 * Can be complemented by switching the acceptance sets.
	 */
	public static DA<BitSet,AcceptanceRabin> constructDRAForTrue(String label) throws PrismException {
		if (label != null) {
			List<String> labels = new ArrayList<String>();
			labels.add(label);
			return createDRAFromString("1 states (start 0), 1 labels: 0-{}->0 0-{0}->0; 1 acceptance pairs: ({},{0})", labels);
		} else {
			return createDRAFromString("1 states (start 0), 0 labels: 0-{}->0; 1 acceptance pairs: ({},{0})", new ArrayList<String>());
		}
	}

	/**
	 * Construct a DRA that accepts if the first label corresponds to the provided label.
	 * Can be complemented by switching the acceptance sets.
	 */
	public static DA<BitSet,AcceptanceRabin> constructDRAForInitialStateLabel(String label) throws PrismException {
		List<String> labels = new ArrayList<String>();
		labels.add(label);
		return createDRAFromString("3 states (start 0), 1 labels: 0-{0}->1 0-{}->2 1-{}->1 1-{0}->1 2-{}->2 2-{0}->2; 1 acceptance pairs: ({2},{1})", labels);
	}

	/**
	 * Create a DRA from a string, e.g.:
	 * "2 states (start 0), 1 labels: 0-{0}->1 0-{}->0 1-{0}->1 1-{}->0; 1 acceptance pairs: ({},{1})"
	 */
	private static DA<BitSet,AcceptanceRabin> createDRAFromString(String s, List<String> labels) throws PrismException
	{
		int ptr = 0, i, j, k, n, from, to;
		String bs;
		DA<BitSet,AcceptanceRabin> draNew;
		AcceptanceRabin acceptance = new AcceptanceRabin();

		try {
			// Num states
			j = s.indexOf("states", ptr);
			n = Integer.parseInt(s.substring(0, j).trim());
			draNew = new DA<BitSet,AcceptanceRabin>(n);
			draNew.setAcceptance(acceptance);
			draNew.setAPList(labels);
			// Start state
			i = s.indexOf("start", j) + 6;
			j = s.indexOf(")", i);
			n = Integer.parseInt(s.substring(i, j).trim());
			draNew.setStartState(n);
			// Edges
			i = s.indexOf("labels", j) + 8;
			j = s.indexOf("-{", i);
			while (j != -1) {
				from = Integer.parseInt(s.substring(i, j).trim());
				i = j + 2;
				j = s.indexOf("}", i);
				bs = s.substring(i, j);
				i = j + 3;
				j = Math.min(s.indexOf(";", i), s.indexOf(" ", i));
				to = Integer.parseInt(s.substring(i, j).trim());
				draNew.addEdge(from, createBitSetFromString(bs), to);
				i = j + 1;
				j = s.indexOf("-{", i);
			}
			// Acceptance pairs
			i = s.indexOf("({", i);
			while (i != -1) {
				j = s.indexOf("},{", i);
				k = s.indexOf("})", j);
				BitSet L = createBitSetFromString(s.substring(i + 2, j));
				BitSet K = createBitSetFromString(s.substring(j + 3, k));
				acceptance.add(new AcceptanceRabin.RabinPair(L,K));
				i = s.indexOf("({", k);
			}
		} catch (NumberFormatException e) {
			throw new PrismException("Error in DRA string format");
		}

		return draNew;
	}

	/**
	 * Create a BitSet from a string, e.g. "0,3,4".
	 */
	private static BitSet createBitSetFromString(String s) throws PrismException
	{
		int i, n;
		BitSet bs = new BitSet();
		String ss[] = s.split(",");
		n = ss.length;
		for (i = 0; i < n; i++) {
			s = ss[i].trim();
			if (s.length() == 0)
				continue;
			bs.set(Integer.parseInt(s));
		}
		return bs;
	}

	// Example: manual creation of DRA for: !(F ("L0"&(X "L1")))
	public static DA<BitSet,AcceptanceRabin> draForNotFaCb(String l0, String l1) throws PrismException
	{
		int numStates;
		List<String> apList;
		DA<BitSet,AcceptanceRabin> draNew;

		// 4 states (start 3), 2 labels: 0-{1}->0 0-{0, 1}->1 0-{}->0 0-{0}->1 1-{1}->2 1-{0, 1}->2 1-{}->0 1-{0}->1 2-{1}->2 2-{0, 1}->2 2-{}->2 2-{0}->2 3-{1}->0 3-{0, 1}->1 3-{}->0 3-{0}->1; 1 acceptance pairs: ({2},{0, 1})
		numStates = 4;
		draNew = new DA<BitSet,AcceptanceRabin>(numStates);
		draNew.setAcceptance(new AcceptanceRabin());
		// AP set
		apList = new ArrayList<String>(2);
		apList.add(l0);
		apList.add(l1);
		draNew.setAPList(apList);
		// Start state
		draNew.setStartState(3);
		// Bitsets for edges
		BitSet bitset = new BitSet(); // {}
		BitSet bitset0 = new BitSet(); // {0}
		bitset0.set(0);
		BitSet bitset1 = new BitSet(); // {1}
		bitset1.set(1);
		BitSet bitset01 = new BitSet(); // {0, 1}
		bitset01.set(0);
		bitset01.set(1);
		// Edges
		//   3-{}->0 3-{0}->1; 1 acceptance pairs: ({2},{0, 1})
		draNew.addEdge(0, bitset1, 0);
		draNew.addEdge(0, bitset01, 1);
		draNew.addEdge(0, bitset, 0);
		draNew.addEdge(0, bitset0, 1);
		draNew.addEdge(1, bitset1, 2);
		draNew.addEdge(1, bitset01, 2);
		draNew.addEdge(1, bitset, 0);
		draNew.addEdge(1, bitset0, 1);
		draNew.addEdge(2, bitset1, 2);
		draNew.addEdge(2, bitset01, 2);
		draNew.addEdge(2, bitset, 2);
		draNew.addEdge(2, bitset0, 2);
		draNew.addEdge(3, bitset1, 0);
		draNew.addEdge(3, bitset01, 1);
		draNew.addEdge(3, bitset, 0);
		draNew.addEdge(3, bitset0, 1);
		// Acceptance pairs
		BitSet bitsetL = new BitSet();
		bitsetL.set(2);
		BitSet bitsetK = new BitSet();
		bitsetK.set(01);
		bitsetK.set(1);
		draNew.getAcceptance().add(new AcceptanceRabin.RabinPair(bitsetL, bitsetK));

		return draNew;
	}

	// To run:
	// PRISM_MAINCLASS=prism.LTL2RabinLibrary bin/prism 'G F "L0"'
	public static void main(String args[])
	{
		try {
			String ltl = args.length > 0 ? args[0] : "G F \"L0\"";

			// Convert to Expression
			String pltl = "P=?[" + ltl + "]";
			PropertiesFile pf = Prism.getPrismParser().parsePropertiesFile(new ModulesFile(), new ByteArrayInputStream(pltl.getBytes()));
			Prism.releasePrismParser();
			Expression expr = pf.getProperty(0);
			expr = ((ExpressionProb) expr).getExpression();

			System.out.println(ltl);
			System.out.println(expr.toString());
			System.out.println(ltl.equals(expr.toString()));
			DA<BitSet,AcceptanceRabin> dra1 = jltl2dstar.LTL2Rabin.ltl2rabin(expr.convertForJltl2ba());
			System.out.println(dra1);
			DA<BitSet,AcceptanceRabin> dra2 = getDRAforLTL(expr, null);
			if (dra2 == null) {
			    dra2 = jltl2dstar.LTL2Rabin.ltl2rabin(expr.convertForJltl2ba());
			}
			System.out.println(dra2);
			System.out.println(dra1.toString().equals(dra2.toString()));
			//dra2.printDot(new PrintStream(new File("dra")));

		} catch (Exception e) {
			System.err.print("Error: " + e);
		}
	}
}
