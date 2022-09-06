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

package jdd;

/**
 * Helper class for doing sanity checks in the symbolic engines.
 * <br>
 * As the sanity checks perform various MTBDD operations, which
 * can be costly and can lead to a significant slow-down, the
 * sanity checks should only be enabled for debugging purposes.
 * <br>
 * The sanity checks are globally enabled by setting the
 * {@code static boolean enabled} to true.
 * <br>
 * In your code, check this variable before calling one
 * of the check methods. However, the methods themselves
 * do not check the {@code enabled} flag and can therefore
 * also be used when the global sanity check is disabled.
 */
public class SanityJDD
{
	/** Global flag: is sanity checking enabled? */
	public static boolean enabled = false;

	/**
	 * Perform sanity check: Is a contained in b?
	 * <br>
	 * a and b have to be 0/1 MTBDDs.
	 * <br>
	 * Throws a RuntimeException if the test fails.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void checkIsContainedIn(JDDNode a, JDDNode b)
	{
		checkIsZeroOneMTBDD(a);
		checkIsZeroOneMTBDD(b);

		if (!JDD.IsContainedIn(a, b)) {
			error("a is not contained in b");
		}
	}

	/**
	 * Perform sanity check:
	 * Is node a 0/1-MTBDD, i.e., has only 0 and 1 as constants.
	 * <br>
	 * Throws a RuntimeException if the test fails.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void checkIsZeroOneMTBDD(JDDNode node)
	{
		if (!JDD.IsZeroOneMTBDD(node)) {
			error("MTBDD is not a 0/1-MTBDD");
		}
	}

	/**
	 * Perform sanity check:
	 * Are the variable indizes for the vars vector
	 * in increasing order?
	 * <br>
	 * This is an assumption for various methods
	 * taking a list of variables as argument.
	 * <br>
	 * Throws a RuntimeException if the test fails.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void checkVarsAreSorted(JDDVars vars)
	{
		int lastIndex = -1;
		for (JDDNode var : vars) {
			int index = var.getIndex();
			if (index < lastIndex) {
				error("JDDVars are not sorted: " + vars);
			}
			lastIndex = index;
		}
	}

	/**
	 * Perform sanity check:
	 * Ensure that node has no relevant variables outside of the allowedVars.
	 * <br>
	 * Throws a RuntimeException if the test fails.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void checkIsDDOverVars(JDDNode node, JDDVars... allowedVars)
	{
		JDDNode cube = null;
		JDDNode support = null;
		JDDNode combined = null;

		try {
			// cube of the combined JDDVars
			cube = JDD.Constant(1);
			for (JDDVars vars : allowedVars) {
				cube = JDD.And(cube, vars.toCubeSet());
			}

			// the support of the node, a cube as well
			support = JDD.GetSupport(node);
			// If all variables in the support of node are also
			// variables in the cube, ANDing them will not change
			// the cube. If this is not the case, then there are
			// variables in the support that are not in the cube
			combined = JDD.And(support.copy(), cube.copy());
			if (!combined.equals(cube)) {
				error("MTBDD has unexpected essential variables");
			}
		} finally {
			if (support != null)
				JDD.Deref(support);
			if (cube != null)
				JDD.Deref(cube);
			if (combined != null)
				JDD.Deref(combined);
		}
	}
	
	/**
	 * Perform sanity check:
	 * Ensure that node is a state set over the given state variables.
	 * <br>
	 * Throws a RuntimeException if the test fails.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static void checkIsStateSet(JDDNode node, JDDVars vars)
	{
		checkIsZeroOneMTBDD(node);
		checkIsDDOverVars(node, vars);
	}

	/** Generic check method, raise error with the given message if value is false */
	public static void check(boolean value, String message)
	{
		if (!value) {
			error(message);
		}
	}

	/** Throw error */
	private static void error(String message)
	{
		throw new RuntimeException(message);
	}
}
