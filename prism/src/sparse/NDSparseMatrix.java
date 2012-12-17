package sparse;

import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import prism.PrismException;

/**
 * A wrapper class around a native sparse matrix.
 * @author vojfor
 *
 */
public class NDSparseMatrix 
{
	private static native long PS_BuildNDSparseMatrix(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv);
	private static native long PS_BuildSubNDSparseMatrix(long trans, long odd, long rv, int nrv, long cv, int ncv, long ndv, int nndv, long rewards);
	private static native void PS_DeleteNDSparseMatrix(long ptr_matrix);
	
	static
	{
		try {
			System.loadLibrary("prismsparse");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	private long ptr;
	
	private NDSparseMatrix(long ptr) {
		this.ptr = ptr;
	}
	
	public static NDSparseMatrix BuildNDSparseMatrix(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet) throws PrismException
	{
		long ptr = PS_BuildNDSparseMatrix(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n());
		if (ptr == 0) throw new PrismException(PrismSparse.getErrorMessage());
		return new NDSparseMatrix(ptr);
	}
	
	public static NDSparseMatrix BuildSubNDSparseMatrix(JDDNode trans, ODDNode odd, JDDVars rows, JDDVars cols, JDDVars nondet, JDDNode rewards) throws PrismException
	{
		long ptr = PS_BuildSubNDSparseMatrix(trans.ptr(), odd.ptr(), rows.array(), rows.n(), cols.array(), cols.n(), nondet.array(), nondet.n(), rewards.ptr());
		if (ptr == 0) throw new PrismException(PrismSparse.getErrorMessage());
		return new NDSparseMatrix(ptr);
	}
	
	/**
	 * Returns the pointer to the wrapped native array.
	 * @return Pointer
	 */
	public long getPtr()
	{
		return ptr;
	}
	

	/**
	 * Deletes the matrix
	 */
	public void delete() {
		PS_DeleteNDSparseMatrix(this.ptr);
	}
}
