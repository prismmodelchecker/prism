//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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
	private static native void ODD_SetCUDDManager(int ddm);
	public static void setCUDDManager()
	{
		ODD_SetCUDDManager(JDD.GetCUDDManager());
	}
	
	//------------------------------------------------------------------------------
	// JNI wrappers
	//------------------------------------------------------------------------------
	
	// build odd
	private static native int ODD_BuildODD(int dd, int vars, int num_vars);
	public static ODDNode BuildODD(JDDNode dd, JDDVars vars)
	{
		return new ODDNode(
			ODD_BuildODD(dd.ptr(), vars.array(), vars.n())
		);
	}
	
	//------------------------------------------------------------------------------

	// get number of nodes in odd just built
	private static native int ODD_GetNumODDNodes();
	public static int GetNumODDNodes()
	{
		return ODD_GetNumODDNodes();
	}
	
	//------------------------------------------------------------------------------
	// ODDNode methods
	//------------------------------------------------------------------------------

	public static native long ODD_GetTOff(int odd);
	public static native long ODD_GetEOff(int odd);
	public static native int ODD_GetThen(int odd);
	public static native int ODD_GetElse(int odd);

}

//------------------------------------------------------------------------------
