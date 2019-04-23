//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package odd;

import jdd.*;
import prism.PrismException;
import prism.PrismNotSupportedException;

public class ODDUtils
{
	//------------------------------------------------------------------------------
	// load jni stuff from shared library
	//------------------------------------------------------------------------------

	static
	{
		try {
			System.loadLibrary("odd");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	//------------------------------------------------------------------------------
	// cudd manager
	//------------------------------------------------------------------------------

	// cudd manager
	
	// jni method to set cudd manager for native code
	private static native void ODD_SetCUDDManager(long ddm);
	public static void setCUDDManager()
	{
		ODD_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	//------------------------------------------------------------------------------
	// JNI wrappers
	//------------------------------------------------------------------------------
	
	private static native long ODD_BuildODD(long dd, long vars, int num_vars);
	/**
	 *  Build an ODD.
	 *  If the ODD could not be constructed, returns {@code null}.
	 */
	public static ODDNode BuildODD(JDDNode dd, JDDVars vars) throws PrismException
	{
		if (jdd.SanityJDD.enabled) {
			// ODD construction requires the JDDVars to be in ascending order
			jdd.SanityJDD.checkVarsAreSorted(vars);
			jdd.SanityJDD.checkIsDDOverVars(dd, vars);
		}

		long res = ODD_BuildODD(dd.ptr(), vars.array(), vars.n());
		if (res == 0) {
			// we could not build the ODD (i.e., we had more than Long.MAX_LONG states
			// we return null and will have to live with the limited functionality
			return null;
		}
		return new ODDNode(res);
	}

	private static native void ODD_ClearODD(long ptr);
	/**
	 * Clear the ODD with root node {@code odd}.
	 *<br>
	 * Note: {@code odd} has to be an ODDNode previously returned by a
	 * call to the {@code BuildODD} method. Any other odd will
	 * lead to unexpected behaviour, possibly including crash, etc.
	 */
	public static void ClearODD(ODDNode odd)
	{
		ODD_ClearODD(odd.ptr());
	}

	private static native int ODD_GetNumODDNodes();
	/**
	 *  Get the number of nodes in the ODD just built.
	 */
	public static int GetNumODDNodes()
	{
		return ODD_GetNumODDNodes();
	}
	
	public static native int ODD_GetIndexOfFirstFromDD(long dd, long odd, long vars, int num_vars);
	/**
	 *  Get the index of the first non-zero element of a 0-1 MTBDD, according to an ODD.
	 */
	public static int GetIndexOfFirstFromDD(JDDNode dd, ODDNode odd, JDDVars vars)
	{
		return ODD_GetIndexOfFirstFromDD(dd.ptr(), odd.ptr(), vars.array(), vars.n());
	}
	
	public static native long ODD_SingleIndexToDD(int i, long odd, long vars, int num_vars);
	/**
	 *  Convert a state index to a 0-1 MTBDD representing it, according to an ODD.
	 */
	public static JDDNode SingleIndexToDD(int i, ODDNode odd, JDDVars vars)
	{
		return JDD.ptrToNode(ODD_SingleIndexToDD(i, odd.ptr(), vars.array(), vars.n()));
	}

	/**
	 * Checks that the given ODD has indices that can be represented by
	 * Java integers. If that is not the case, or the ODD is {@code null},
	 * a PrismNotSupportedException is thrown.
	 * @param odd the ODD (may be {@code null})
	 * @param msg Initial part of error message, will be extended with
	 *        " with more than X states, have Y states"
	 */
	public static void checkInt(ODDNode odd, String msg) throws PrismNotSupportedException
	{
		if (odd != null) {
			try {
				long numStates = odd.getNumStates();
				if (numStates > Integer.MAX_VALUE) {
					// number of states fit in long, but not in int
					throw new PrismNotSupportedException(msg + " with more than " + Integer.MAX_VALUE + " states, have " + numStates + " states");
				} else {
					// everything is fine
					return;
				}
			} catch (ArithmeticException e) {
				// number of states does not fit into long, ignore here, handled below
			}
		}

		// we either have no ODD or eoff + toff does not fit into long
		throw new PrismNotSupportedException(msg + " with more than " + Integer.MAX_VALUE + " states, have at least " + Long.MAX_VALUE + " states");
	}

	/**
	 * Returns true if the given ODD has indices that can be represented by
	 * Java integers. If the odd is {@code null}, returns false as well.
	 * @param odd the ODD (may be {@code null})
	 */
	public static boolean hasIntValue(ODDNode odd)
	{
		if (odd == null)
			return false;

		try {
			long numStates = odd.getNumStates();
			if (numStates <= Integer.MAX_VALUE) {
				return true;
			}
		} catch (ArithmeticException e) {
			// number of states does not fit into long, ignore here, handled below
		}
		return false;
	}

	//------------------------------------------------------------------------------
	// ODDNode methods
	//------------------------------------------------------------------------------

	public static native long ODD_GetTOff(long odd);
	public static native long ODD_GetEOff(long odd);
	public static native long ODD_GetThen(long odd);
	public static native long ODD_GetElse(long odd);

}

//------------------------------------------------------------------------------
