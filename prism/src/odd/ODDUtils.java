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
	 */
	public static ODDNode BuildODD(JDDNode dd, JDDVars vars)
	{
		return new ODDNode(
			ODD_BuildODD(dd.ptr(), vars.array(), vars.n())
		);
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
		return new JDDNode(ODD_SingleIndexToDD(i, odd.ptr(), vars.array(), vars.n()));
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
