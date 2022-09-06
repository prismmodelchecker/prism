/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

/**
 * Provides CandidateMatcher for SafraTrees
 */
public class SafraTreeCandidateMatcher {

	public static boolean isMatch(SafraTreeTemplate temp, SafraTree tree) {
		return temp.matches(tree);
	}

	public static boolean abstract_equal_to(SafraTree t1, SafraTree t2) {
		return t1.structural_equal_to(t2);
	}

	public static boolean abstract_less_than(SafraTree t1, SafraTree t2) {
		return t1.structural_less_than(t2);
	}
	
	public static int hash(SafraTree t1) {
		return t1.hashCode();
	}
}
