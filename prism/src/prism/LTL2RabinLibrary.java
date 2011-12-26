//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import jltl2dstar.*;
import parser.ast.*;
import parser.visitor.ASTTraverse;
import parser.visitor.ASTTraverseModify;

/**
 * LTL-to-DRA conversion.
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
	 * Convert an LTL formula into a DRA. The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 */
	public static DRA<BitSet> convertLTLFormulaToDRA(Expression ltl) throws PrismException
	{
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
		
		// If none, convert using jltl2dstar library
		return LTL2Rabin.ltl2rabin(ltl.convertForJltl2ba());
	}

	/**
	 * Create a DRA from a string, e.g.:
	 * "2 states (start 0), 1 labels: 0-{0}->1 0-{}->0 1-{0}->1 1-{}->0; 1 acceptance pairs: ({},{1})"
	 */
	private static DRA<BitSet> createDRAFromString(String s, List<String> labels) throws PrismException
	{
		int ptr = 0, i, j, k, n, from, to;
		String bs;
		prism.DRA<BitSet> draNew;

		try {
			// Num states
			j = s.indexOf("states", ptr);
			n = Integer.parseInt(s.substring(0, j).trim());
			draNew = new prism.DRA<BitSet>(n);
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
				draNew.addAcceptancePair(createBitSetFromString(s.substring(i + 2, j)), createBitSetFromString(s.substring(j + 3, k)));
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
	public static DRA<BitSet> draForNotFaCb(String l0, String l1) throws PrismException
	{
		int numStates;
		List<String> apList;
		prism.DRA<BitSet> draNew;

		// 4 states (start 3), 2 labels: 0-{1}->0 0-{0, 1}->1 0-{}->0 0-{0}->1 1-{1}->2 1-{0, 1}->2 1-{}->0 1-{0}->1 2-{1}->2 2-{0, 1}->2 2-{}->2 2-{0}->2 3-{1}->0 3-{0, 1}->1 3-{}->0 3-{0}->1; 1 acceptance pairs: ({2},{0, 1})
		numStates = 4;
		draNew = new prism.DRA<BitSet>(numStates);
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
		draNew.addAcceptancePair(bitsetL, bitsetK);

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
			DRA<BitSet> dra1 = jltl2dstar.LTL2Rabin.ltl2rabin(expr.convertForJltl2ba());
			System.out.println(dra1);
			DRA<BitSet> dra2 = convertLTLFormulaToDRA(expr);
			System.out.println(dra2);
			System.out.println(dra1.toString().equals(dra2.toString()));
			//dra2.printDot(new PrintStream(new File("dra")));

		} catch (Exception e) {
			System.err.print("Error: " + e);
		}
	}
}
