//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package sparse;

import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import prism.PrismException;

/**
 * A wrapper class around a native sparse representation of an MDP.
 */
public class NDSparseMatrix
{
	// JNI methods
	
	private static native int PS_NDGetActionIndex(long ptr, int s, int i);

	private static native long PS_BuildNDSparseMatrix(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv);

	private static native long PS_BuildSubNDSparseMatrix(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long rewards);

	private static native void PS_AddActionsToNDSparseMatrix(long trans, long trans_actions, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long ndsm);
	
	private static native void PS_DeleteNDSparseMatrix(long ptr_matrix);

	static {
		try {
			System.loadLibrary("prismsparse");
		} catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	/** Pointer to C++ data structure. **/
	private long ptr;

	/**
	 * Constructor (from a C++ pointer).
	 */
	private NDSparseMatrix(long ptr)
	{
		this.ptr = ptr;
	}

	/**
	 * Returns the pointer to the native C++ data structure.
	 */
	public long getPtr()
	{
		return ptr;
	}

	/**
	 * Get the index of the action label (if any) for choice {@code i} of state {@code s}.
	 */
	public int getActionIndex(int s, int i)
	{
		return PS_NDGetActionIndex(ptr, s, i);
	}
	
	/**
	 * Deletes the matrix.
	 */
	public void delete()
	{
		PS_DeleteNDSparseMatrix(this.ptr);
	}

	// Static methods to construct NDSparseMatrix objects

	public static NDSparseMatrix BuildNDSparseMatrix(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet) throws PrismException
	{
		long ptr = PS_BuildNDSparseMatrix(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n());
		if (ptr == 0)
			throw new PrismException(PrismSparse.getErrorMessage());
		return new NDSparseMatrix(ptr);
	}

	public static NDSparseMatrix BuildSubNDSparseMatrix(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode rewards)
			throws PrismException
	{
		long ptr = PS_BuildSubNDSparseMatrix(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), rewards.ptr());
		if (ptr == 0)
			throw new PrismException(PrismSparse.getErrorMessage());
		return new NDSparseMatrix(ptr);
	}

	public static void AddActionsToNDSparseMatrix(JDDNode trans, JDDNode transActions, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, NDSparseMatrix ndsm) throws PrismException
	{
		PS_AddActionsToNDSparseMatrix(trans.ptr(), transActions.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), ndsm.getPtr());
	}
}
