/*
	This software is a Java wrapper for the lp_solve optimization library.

	Copyright (C) 2004  Juergen Ebert (juergen.ebert@web.de)

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package lpsolve;

import java.util.HashMap;
import java.util.Map;


/**
 * Object wrapper for a problem structure of the lp_solve library.
 * Offers access to all lp_solve methods.
 *
 * @author Juergen Ebert
 */
public class LpSolve {

	public static final int FALSE           = 0;
	public static final int TRUE            = 1;
	public static final int AUTOMATIC       = 2;
	public static final int DYNAMIC         = 4;

	public static final int FR = 0;
	public static final int LE = 1;
	public static final int GE = 2;
	public static final int EQ = 3;
	public static final int OF = 4;

	public static final int SIMPLEX_PRIMAL_PRIMAL    = 5;
	public static final int SIMPLEX_DUAL_PRIMAL      = 6;
	public static final int SIMPLEX_PRIMAL_DUAL      = 9;
	public static final int SIMPLEX_DUAL_DUAL        = 10;
	public static final int SIMPLEX_DEFAULT          = SIMPLEX_DUAL_PRIMAL;

	/* Presolve defines */
	public static final int PRESOLVE_NONE            = 0;
	public static final int PRESOLVE_ROWS            = 1;
	public static final int PRESOLVE_COLS            = 2;
	public static final int PRESOLVE_LINDEP          = 4;
	public static final int PRESOLVE_SOS             = 32;
	public static final int PRESOLVE_REDUCEMIP       = 64;
	public static final int PRESOLVE_KNAPSACK        = 128;
	public static final int PRESOLVE_ELIMEQ2         = 256;
	public static final int PRESOLVE_IMPLIEDFREE     = 512;
	public static final int PRESOLVE_REDUCEGCD       = 1024;
	public static final int PRESOLVE_PROBEFIX        = 2048;
	public static final int PRESOLVE_PROBEREDUCE     = 4096;
	public static final int PRESOLVE_ROWDOMINATE     = 8192;
	public static final int PRESOLVE_COLDOMINATE     = 16384;
	public static final int PRESOLVE_MERGEROWS       = 32768;
	public static final int PRESOLVE_IMPLIEDSLK      = 65536;
	public static final int PRESOLVE_COLFIXDUAL      = 131072;
	public static final int PRESOLVE_BOUNDS          = 262144;
	public static final int PRESOLVE_DUALS           = 524288;
	public static final int PRESOLVE_SENSDUALS       = 1048576;

	/* Basis crash options */
	public static final int CRASH_NOTHING            = 0;
	public static final int CRASH_MOSTFEASIBLE       = 2;

	/* Strategy codes to avoid or recover from degenerate pivots */
	public static final int ANTIDEGEN_NONE           = 0;
	public static final int ANTIDEGEN_FIXEDVARS      = 1;
	public static final int ANTIDEGEN_COLUMNCHECK    = 2;
	public static final int ANTIDEGEN_STALLING       = 4;
	public static final int ANTIDEGEN_NUMFAILURE     = 8;
	public static final int ANTIDEGEN_LOSTFEAS      = 16;
	public static final int ANTIDEGEN_INFEASIBLE    = 32;
	public static final int ANTIDEGEN_DYNAMIC       = 64;
	public static final int ANTIDEGEN_DURINGBB     = 128;
	public static final int ANTIDEGEN_RHSPERTURB   = 256;
	public static final int ANTIDEGEN_BOUNDFLIP    = 512;

	/* REPORT defines */
	public static final int NEUTRAL      = 0;
	public static final int CRITICAL     = 1;
	public static final int SEVERE       = 2;
	public static final int IMPORTANT    = 3;
	public static final int NORMAL       = 4;
	public static final int DETAILED     = 5;
	public static final int FULL         = 6;

	/* MESSAGE defines */
	public static final int MSG_NONE                 = 0;
	public static final int MSG_PRESOLVE             = 1;
	public static final int MSG_ITERATION            = 2;
	public static final int MSG_INVERT               = 4;
	public static final int MSG_LPFEASIBLE           = 8;
	public static final int MSG_LPOPTIMAL           = 16;
	public static final int MSG_LPEQUAL             = 32;
	public static final int MSG_LPBETTER            = 64;
	public static final int MSG_MILPFEASIBLE       = 128;
	public static final int MSG_MILPEQUAL          = 256;
	public static final int MSG_MILPBETTER         = 512;
	public static final int MSG_MILPSTRATEGY      = 1024;
	public static final int MSG_MILPOPTIMAL       = 2048;
	public static final int MSG_PERFORMANCE       = 4096;
	public static final int MSG_INITPSEUDOCOST    = 8192;

	/* Improvement defines */
	public static final int IMPROVE_NONE        = 0;
	public static final int IMPROVE_SOLUTION    = 1;
	public static final int IMPROVE_DUALFEAS    = 2;
	public static final int IMPROVE_THETAGAP    = 4;
	public static final int IMPROVE_BBSIMPLEX   = 8;

	/* Scaling types */
	public static final int SCALE_NONE               = 0;
	public static final int SCALE_EXTREME            = 1;
	public static final int SCALE_RANGE              = 2;
	public static final int SCALE_MEAN               = 3;
	public static final int SCALE_GEOMETRIC          = 4;
	public static final int SCALE_CURTISREID         = 7;   /* Override to optimal Curtis-Reid scaling */

	/* Alternative scaling weights */
	public static final int SCALE_LINEAR             = 0;
	public static final int SCALE_QUADRATIC          = 8;
	public static final int SCALE_LOGARITHMIC       = 16;
	public static final int SCALE_USERWEIGHT        = 31;
	public static final int SCALE_POWER2            = 32;   /* As is or rounded to power of 2 */
	public static final int SCALE_EQUILIBRATE       = 64;   /* Make sure that no scaled number is above 1 */
	public static final int SCALE_INTEGERS         = 128;   /* Apply to integer column variables */
	public static final int SCALE_DYNUPDATE        = 256;
	public static final int SCALE_ROWSONLY         = 512;
	public static final int SCALE_COLSONLY        = 1024;

	/* Pricing methods */
	public static final int PRICER_FIRSTINDEX        = 0;
	public static final int PRICER_DANTZIG           = 1;
	public static final int PRICER_DEVEX             = 2;
	public static final int PRICER_STEEPESTEDGE      = 3;

	/* Pricing strategies */
	public static final int PRICE_METHODDEFAULT      = 0;
	public static final int PRICE_PRIMALFALLBACK     = 4;    /* In case of Steepest Edge, fall back to DEVEX in primal */
	public static final int PRICE_MULTIPLE           = 8;    /* Multiple pricing (as of v5 only binary) */
	public static final int PRICE_PARTIAL           = 16;    /* Enable partial pricing */
	public static final int PRICE_ADAPTIVE          = 32;    /* Temporarily use First Index if cycling is detected */
	public static final int PRICE_HYBRID            = 64;    /* NOT IMPLEMENTED */
	public static final int PRICE_RANDOMIZE        = 128;    /* Adds a small randomization effect to the selected pricer */
	public static final int PRICE_AUTOPARTIAL      = 512;
	public static final int PRICE_LOOPLEFT        = 1024;    /* Scan entering/leaving columns left rather than right */
	public static final int PRICE_LOOPALTERNATE   = 2048;    /* Scan entering/leaving columns alternatingly left/right */
	public static final int PRICE_HARRISTWOPASS   = 4096;
	public static final int PRICE_TRUENORMINIT   = 16384;

	/* B&B strategies */
	public static final int NODE_FIRSTSELECT         = 0;
	public static final int NODE_GAPSELECT           = 1;
	public static final int NODE_RANGESELECT         = 2;
	public static final int NODE_FRACTIONSELECT      = 3;
	public static final int NODE_PSEUDOCOSTSELECT    = 4;
	public static final int NODE_PSEUDONONINTSELECT  = 5;                /* Kjell Eikland special #1 */
	public static final int NODE_PSEUDORATIOSELECT   = 6;                /* Kjell Eikland special #2 */
	public static final int NODE_USERSELECT          = 7;

	public static final int NODE_WEIGHTREVERSEMODE   = 8;
	public static final int NODE_BRANCHREVERSEMODE   = 16;
	public static final int NODE_GREEDYMODE          = 32;
	public static final int NODE_PSEUDOCOSTMODE      = 64;
	public static final int NODE_DEPTHFIRSTMODE      = 128;
	public static final int NODE_RANDOMIZEMODE       = 256;
	public static final int NODE_DYNAMICMODE         = 1024;
	public static final int NODE_RESTARTMODE         = 2048;
	public static final int NODE_BREADTHFIRSTMODE    = 4096;
	public static final int NODE_AUTOORDER           = 8192;
	public static final int NODE_RCOSTFIXING         = 16384;
	public static final int NODE_STRONGINIT          = 32768;

	public static final int BRANCH_CEILING   = 0;
	public static final int BRANCH_FLOOR     = 1;
	public static final int BRANCH_AUTOMATIC = 2;
        public static final int BRANCH_DEFAULT   = 3;

	/* Solver status values */
	public static final int UNKNOWNERROR            = -5;
	public static final int DATAIGNORED             = -4;
	public static final int NOBFP                   = -3;
	public static final int NOMEMORY                = -2;
	public static final int NOTRUN                  = -1;
	public static final int OPTIMAL                  = 0;
	public static final int SUBOPTIMAL               = 1;
	public static final int INFEASIBLE               = 2;
	public static final int UNBOUNDED                = 3;
	public static final int DEGENERATE               = 4;
	public static final int NUMFAILURE               = 5;
	public static final int USERABORT                = 6;
	public static final int TIMEOUT                  = 7;
	public static final int RUNNING                  = 8;
	public static final int PRESOLVED                = 9;

	/* Branch & Bound and Lagrangean extra status values */
	public static final int PROCFAIL                = 10;
	public static final int PROCBREAK               = 11;
	public static final int FEASFOUND               = 12;
	public static final int NOFEASFOUND             = 13;


	/**
	 * Value of the pointer to the <code>lprec</code> structure in the C stub DLL.
	 */
	private long lp = 0;

	/**
	 * Callback listener set by <code>putAbortfunc</code>
	 */
	private AbortListener abortListener = null;

	/**
	 * User handle set by <code>putAbortfunc</code>
	 */
	private Object abortUserhandle = null;

	/**
	 * Callback listener set by <code>putLogfunc</code>
	 */
	private LogListener logListener = null;

	/**
	 * User handle set by <code>putLogfunc</code>
	 */
	private Object logUserhandle = null;

	/**
	 * Callback listener set by <code>putMsgfunc</code>
	 */
	private MsgListener msgListener = null;

	/**
	 * User handle set by <code>putMsgfunc</code>
	 */
	private Object msgUserhandle = null;

	/**
	 * Callback listener set by <code>putBbBranchfunc</code>
	 */
	private BbListener bbBranchListener = null;

	/**
	 * User handle set by <code>putBbBranchfunc</code>
	 */
	private Object bbBranchUserhandle = null;

	/**
	 * Callback listener set by <code>putBbNodefunc</code>
	 */
	private BbListener bbNodeListener = null;

	/**
	 * User handle set by <code>putBbNodefunc</code>
	 */
	private Object bbNodeUserhandle = null;

	/**
	 * Static initializer to load the stub library
	 */
	static {
		System.loadLibrary("lpsolve55j");
		init();
	}

	/**
	 * Native helper method to cache method and field IDs.
	 */
	private static native void init();


	// ========================================================================
	// Constructors
	// ========================================================================

	/**
	 * Constructs a new LpSolve instance
	 */
	private LpSolve(long lp) {
		this.lp = lp;
	}

	// ========================================================================
	// Public static factory methods
	// ========================================================================

	/**
	 * Creates a new problem. Upon successful completion, the lprec attribute
	 * in this class contains the value of the pointer to the <code>lprec</code>
	 * structure.
	 *
	 * @param rows Initial number of rows.
	 * @param columns Initial number of columns.
	 * @throws LpSolveException if lp_solve could not create the problem
	 */
	public static native LpSolve makeLp(int rows, int columns) throws LpSolveException;

	/**
	 * Read an lp model from file and create a new problem.
	 */
	public static native LpSolve readLp(String filename, int verbose, String lpName)
		throws LpSolveException;

	/**
	 * Read an mps model from file and create a new problem.
	 */
	public static native LpSolve readMps(String filename, int verbose)
		throws LpSolveException;

	/**
	 * Read a model in free MPS format from file and create a new problem.
	 */
	public static native LpSolve readFreeMps(String filename, int verbose)
		throws LpSolveException;

	/**
	 * Read a model via the External Language Interface and create a new problem.
	 */
	public static native LpSolve readXLI(String xliname, String modelname,
			String dataname, String options, int verbose) throws LpSolveException;


	// ========================================================================
	// Other static methods
	// ========================================================================

	/**
	 * Returns the full version number of the underlying lp_solve library.
	 *
	 * @return VersionInfo object with full version info
	 */
	public static native VersionInfo lpSolveVersion();


	// ========================================================================
	// Overridden methods of the base class
	// ========================================================================


	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		if (lp != 0) {
			removeLp(lp);
			deleteLp();
		}
		super.finalize();
	}

	/**
	 * Return the value of the lp attribute.
	 * @return the value of the lp attribute
	 */
	public long getLp() {
		return lp;
	}


	// ========================================================================
	// Native methods.
	// ========================================================================

	/**
	 * Copy an existing lprec structure to a new lprec structure.
	 * Creates an independent new problem.
	 */
	public native LpSolve copyLp() throws LpSolveException;

	/**
	 * Set the name of the problem.
	 */
	public native void setLpName(String name) throws LpSolveException;

	/**
	 * Allocate memory for the specified size.
	 */
	public native void resizeLp(int rows, int columns) throws LpSolveException;

	/**
	 * Get the name of the problem.
	 */
	public native String getLpName() throws LpSolveException;

	/**
	 * Add a constraint to the problem.
	 */
	public native void addConstraint(double[] row, int constrType, double rh) throws LpSolveException;

	/**
	 * Add a constraint to the problem.
	 */
	public native void strAddConstraint(String row, int constrType, double rh) throws LpSolveException;

	/**
	 * Add a constraint to the problem.
	 */
	public native void addConstraintex(int count, double[] row, int[] colno, int constrType, double rh) throws LpSolveException;

	/**
	 * Remove a constraint from the problem.
	 */
	public native void delConstraint(int rownr) throws LpSolveException;

	/**
	 * Returns if constraint type specified in mask is active.
	 */
	public native boolean isConstrType(int row, int mask);

	/**
	 * Add a Lagrangian constraint to the problem.
	 */
	public native void addLagCon(double[] row, int constrType, double rh) throws LpSolveException;

	/**
	 * Add a Lagrangian constraint to the problem.
	 */
	public native void strAddLagCon(String row, int constrType, double rh) throws LpSolveException;

	/**
	 * Add a column to the problem.
	 */
	public native void addColumn(double[] column) throws LpSolveException;

	/**
	 * Add a column to the problem.
	 */
	public native void addColumnex(int count, double[] column, int[] rowno) throws LpSolveException;

	/**
	 * Add a column to the problem.
	 */
	public native void strAddColumn(String column) throws LpSolveException;

	/**
	 * Remove a column from the problem.
	 */
	public native void delColumn(int columnnr) throws LpSolveException;

	/**
	 * Set a constraint in the lp.
	 */
	public native void setRow(int rowno, double[] row) throws LpSolveException;

	/**
	 * Set a constraint in the lp.
	 */
	public native void setRowex(int rowno, int count, double[] row, int[] colno) throws LpSolveException;

	/**
	 * Set a column in the lp.
	 */
	public native void setColumn(int colno, double[] column) throws LpSolveException;

	/**
	 * Set a column in the lp.
	 */
	public native void setColumnex(int colno, int count, double[] column, int[] rowno) throws LpSolveException;

	/**
	 * Check if a column is already present in the problem.
	 */
	public native int columnInLp(double[] column);

	/**
	 * Set the name of a constraint (row) in the problem.
	 */
	public native void setRowName(int rownr, String name) throws LpSolveException;

	/**
	 * Gets the name of a constraint (row) in the problem.
	 */
	public native String getRowName(int rownr) throws LpSolveException;

	/**
	 * Gets the name of a constraint (row) in the problem.
	 */
	public native String getOrigrowName(int rownr) throws LpSolveException;

	/**
	 * Set the name of a column in the problem.
	 */
	public native void setColName(int colnr, String name) throws LpSolveException;

	/**
	 * Gets the name of a column in the problem.
	 */
	public native String getColName(int colnr) throws LpSolveException;

	/**
	 * Gets the name of a column in the problem.
	 */
	public native String getOrigcolName(int colnr) throws LpSolveException;

	/**
	 * Set the right hand side (RHS) vector (column 0).
	 */
	public native void setRhVec(double[] rh) throws LpSolveException;

	/**
	 * Set the right hand side (RHS) vector (column 0).
	 */
	public native void strSetRhVec(String rh) throws LpSolveException;

	/**
	 * Set the value of the right hand side (RHS) vector (column 0) for one row.
	 */
	public native void setRh(int row, double value) throws LpSolveException;

	/**
	 * Get the value of the right hand side (RHS) vector (column 0) for one row.
	 * NOTE: Returns 0 even if the row index is out of bounds, in accordance
	 * to the behaviour of the lp_solve routine!
	 */
	public native double getRh(int row);

	/**
	 * Set the type of a constraint.
	 */
	public native void setConstrType(int rownr, int constrType) throws LpSolveException;

	/**
	 * Get the type of a constraint.
	 */
	public native short getConstrType(int rownr) throws LpSolveException;

	/**
	 * Add a SOS constraint.
	 */
	public native void addSOS(String name, int sostype, int priority, int count,
		int[] sosvars, double[] weights) throws LpSolveException;

	/**
	 * Returns if the variable is SOS or not.
	 */
	public native boolean isSOSVar(int colnr) throws LpSolveException;

	/**
	 * Set the objective function (row 0) of the matrix.
	 */
	public native void setObjFn(double[] row) throws LpSolveException;

	/**
	 * Set the objective function (row 0) of the matrix.
	 */
	public native void strSetObjFn(String row) throws LpSolveException;

	/**
	 * Set the objective function (row 0) of the matrix.
	 */
	public native void setObjFnex(int count, double[] row, int[] colno) throws LpSolveException;

	/**
	 * Set the objective function (row 0) of the matrix.
	 */
	public native void setObj(int column, double value) throws LpSolveException;

	/**
	 * Set a single element in the matrix.
	 */
	public native void setMat(int row, int column, double value) throws LpSolveException;

	/**
	 * Get a single element from the matrix.
	 */
	public native double getMat(int row, int column);

	/**
	 * Get all row elements from the matrix.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getRow(int rownr, double[] row) throws LpSolveException;

	/**
	 * Get the non-zero row elements from the matrix.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native int getRowex(int rownr, double[] row, int[] nzcols) throws LpSolveException;

	/**
	 * Get all row elements from the matrix.
	 * Returned array is allocated by the method.
	 * This is an additional method which is not implemented by lp_solve.
	 * Internally, get_row is used.
	 */
	public native double[] getPtrRow(int rownr) throws LpSolveException;

	/**
	 * Get all column elements from the matrix.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getColumn(int colnr, double[] column) throws LpSolveException;

	/**
	 * Get the non-zero column elements from the matrix.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native int getColumnex(int colnr, double[] column, int[] nzrows) throws LpSolveException;

	/**
	 * Get all column elements from the matrix.
	 * Returned array is allocated by the method.
	 * This is an additional method which is not implemented by lp_solve.
	 * Internally, get_column is used.
	 */
	public native double[] getPtrColumn(int columnrnr) throws LpSolveException;

	/**
	 * Set objective function to maximize.
	 */
	public native void setMaxim();

	/**
	 * Set objective function to minimize.
	 */
	public native void setMinim();

	/**
	 * Set objective function sense.
	 */
	public native void setSense(boolean maximize);

	/**
	 * Returns objective function direction.
	 */
	public native boolean isMaxim();

	/**
	 * Set the lower bound of a variable.
	 */
	public native void setLowbo(int colnr, double value) throws LpSolveException;

	/**
	 * Get the lower bound of a variable.
	 */
	public native double getLowbo(int colnr) throws LpSolveException;

	/**
	 * Set the upper bound of a variable.
	 */
	public native void setUpbo(int colnr, double value) throws LpSolveException;

	/**
	 * Get the upper bound of a variable.
	 */
	public native double getUpbo(int colnr) throws LpSolveException;

	/**
	 * Sets if the variable is free.
	 */
	public native void setUnbounded(int colnr) throws LpSolveException;

	/**
	 * Returns if the variable is free.
	 */
	public native boolean isUnbounded(int colnr);

	/**
	 * Returns if the variable is negative.
	 */
	public native boolean isNegative(int colnr);

	/**
	 * Set the upper and lower bound of a variable.
	 */
	public native void setBounds(int colnr, double lower, double upper) throws LpSolveException;

	/**
	 * Specifies if set bounds may only be tighter or also less restrictive.
	 */
	public native void setBoundsTighter(boolean tighten);

	/**
	 * Returns if set bounds may only be tighter or also less restrictive.
	 */
	public native boolean getBoundsTighter();

	/**
	 * Set the range on a constraint.
	 */
	public native void setRhRange(int rownr, double range) throws LpSolveException;

	/**
	 * Gets the range on a constraint.
	 */
	public native double getRhRange(int rownr) throws LpSolveException;

	/**
	 * Set the type of the variable. Integer or floating point.
	 */
	public native void setInt(int colnr, boolean mustBeInteger) throws LpSolveException;

	/**
	 * Get the type of the variable. Integer or floating point.
	 */
	public native boolean isInt(int colnr);

	/**
	 * Set the type of the variable. Binary or floating point.
	 */
	public native void setBinary(int colnr, boolean mustBeBin) throws LpSolveException;

	/**
	 * Gets the type of the variable. Binary integer or floating point.
	 */
	public native boolean isBinary(int colnr);

	/**
	 * Set the type of the variable. semi-continious or not.
	 */
	public native void setSemicont(int colnr, boolean mustBeSc) throws LpSolveException;

	/**
	 * Get the type of the variable. semi-continious or not.
	 */
	public native boolean isSemicont(int colnr);

	/**
	 * Specifies the practical value for "infinite".
	 */
	public native void setInfinite(double value);

	/**
	 * Returns the value of "infinite".
	 */
	public native double getInfinite();

	/**
	 * Checks if the provided absolute of the value is larger or equal to "infinite".
	 */
	public native boolean isInfinite(double value);

	/**
	 * Specifies the tolerance that is used to determine whether a floating-point
	 * number is in fact an integer.
	 */
	public native void setEpsint(double value);

	/**
	 * Returns the tolerance that is used to determine whether a floating-point
	 * number is in fact an integer
	 */
	public native double getEpsint();

	/**
	 * Specifies the value that is used as a tolerance for the Right Hand Side (RHS)
	 * to determine whether a value should be considered as 0
	 */
	public native void setEpsb(double value);

	/**
	 * Returns the value that is used as a tolerance for the Right Hand Side (RHS)
	 * to determine whether a value should be considered as 0.
	 */
	public native double getEpsb();

	/**
	 * Specifies the value that is used as a tolerance for reduced costs
	 * to determine whether a value should be considered as 0.
	 */
	public native void setEpsd(double value);

	/**
	 * Returns the value that is used as a tolerance for the reduced costs
	 * to determine whether a value should be considered as 0.
	 */
	public native double getEpsd();

	/**
	 * Specifies the value that is used as a tolerance for rounding values to zero.
	 */
	public native void setEpsel(double value);

	/**
	 * Returns the value that is used as a tolerance for rounding values to zero.
	 */
	public native double getEpsel();

	/**
	 * Specifies the value that is used as a tolerance pivot element to determine
	 * whether a value should be considered as 0.
	 */
	public native void setEpspivot(double value);

	/**
	 * Returns the value that is used as a tolerance pivot element to determine
	 * whether a value should be considered as 0.
	 */
	public native double getEpspivot();

	/**
	 * Specifies the value that is used as perturbation scalar for degenerative problems.
	 */
	public native void setEpsperturb(double value);

	/**
	 * Returns the value that is used as perturbation scalar for degenerative problems.
	 */
	public native double getEpsperturb();

	/**
	 * This is a simplified way of specifying multiple eps thresholds that are "logically" consistent.
	 */
	public native void setEpslevel(int epslevel) throws LpSolveException;

	/**
	 * Returns an extra status after a call to a function.
	 */
	public native int getStatus();

	/**
	 * Specifies the MIP gap value.
	 */
	public native void setMipGap(boolean absolute, double value);

	/**
	 * Returns the MIP gap value.
	 */
	public native double getMipGap(boolean absolute);

	/**
	 * Set the verbose level.
	 */
	public native void setVerbose(int verbose);

	/**
	 * Returns the verbose level.
	 */
	public native int getVerbose();

	/**
	 * Set a timeout.
	 */
	public native void setTimeout(long timeout);

	/**
	 * Gets the timout.
	 */
	public native long getTimeout();

	/**
	 * Gets the time elapsed since start of solve.
	 */
	public native double timeElapsed();

	/**
	 * Sets a flag if all intermediate valid solutions must be printed while solving.
	 */
	public native void setPrintSol(int printSol);

	/**
	 * Returns a flag if all intermediate valid solutions must be printed while solving.
	 */
	public native int getPrintSol();

	/**
	 * Sets a flag if all intermediate results and the branch-and-bound decisions
	 * must be printed while solving.
	 */
	public native void setDebug(boolean debug);

	/**
	 * Returns a flag if all intermediate results and the branch-and-bound decisions
	 * must be printed while solving.
	 */
	public native boolean isDebug();

	/**
	 * Sets a flag if pivot selection must be printed while solving.
	 */
	public native void setTrace(boolean trace);

	/**
	 * Returns a flag if pivot selection must be printed while solving.
	 */
	public native boolean isTrace();

	/**
	 * Sets a flag if Lagrangian progression must be printed while solving.
	 */
	public native void setLagTrace(boolean lagTrace);

	/**
	 * Returns a flag if Lagrangian progression must be printed while solving.
	 */
	public native boolean isLagTrace();

	/**
	 * Specifies which add routine performs best.
	 */
	public native boolean setAddRowmode(boolean turnon);

	/**
	 * Returns a flag which of the add routines perform best.
	 */
	public native boolean isAddRowmode();

	/**
	 * Specifies if special handling must be done to reduce degeneracy/cycling while solving.
	 */
	public native void setAntiDegen(int antiDegen);

	/**
	 * Returns if the degeneracy rule specified in testmask is active.
	 */
	public native boolean isAntiDegen(int testmask);

	/**
	 * Returns the used degeneracy rule.
	 */
	public native int getAntiDegen();

	/**
	 * Specifies if a presolve must be done before solving.
	 */
	public native void setPresolve(int doPresolve, int maxloops);

	/**
	 * Returns if presolve level specified in testmask is active.
	 */
	public native boolean isPresolve(int testmask);

	/**
	 * Returns the current presolve setting.
	 */
	public native int getPresolve();

	/**
	 * Returns the number of times presolve is done.
	 */
	public native int getPresolveloops();

	/**
	 * Sets the maximum number of pivots between a reinversion of the matrix.
	 */
	public native void setMaxpivot(int maxNumInv);

	/**
	 * Returns the maximum number of pivots between a reinversion of the matrix.
	 */
	public native int getMaxpivot();

	/**
	 * Specifies the branch-and-bound rule.
	 */
	public native void setBbRule(int bbRule);

	/**
	 * Returns the branch-and-bound rule.
	 */
	public native int getBbRule();

	/**
	 * Sets the maximum branch-and-bound depth.
	 */
	public native void setBbDepthlimit(int bbMaxlevel);

	/**
	 * Returns the maximum branch-and-bound depth.
	 */
	public native int getBbDepthlimit();

	/**
	 * Returns the number of equal solutions.
	 */
	public native int getSolutioncount();

	/**
	 * Sets the solution number that must be returned.
	 */
	public native void setSolutionlimit(int limit);

	/**
	 * Returns the solution number that must be returned.
	 */
	public native int getSolutionlimit();

	/**
	 * Set initial "at least better than" guess for objective function.
	 */
	public native void setObjBound(double objBound);

	/**
	 * Returns initial "at least better than" guess for objective function.
	 */
	public native double getObjBound();

	/**
	 * Specifies which branch to take first in branch-and-bound algorithm.
	 */
	public native void setBbFloorfirst(int floorFirst);

	/**
	 * Returns which branch to take first in branch-and-bound algorithm.
	 */
	public native int getBbFloorfirst();

	/**
	 * Specifies, for the specified variable, which branch to take first
	 * in branch-and-bound algorithm.
	 */
	public native void setVarBranch(int colnr, int branchMode) throws LpSolveException;

	/**
	 * Returns, for the specified variable, which branch to take first
	 * in branch-and-bound algorithm.
	 */
	public native int getVarBranch(int colnr) throws LpSolveException;

	/**
	 * Set the weights on variables.
	 */
	public native void setVarWeights(double[] weights) throws LpSolveException;

	/**
	 * Returns, for the specified variable, the priority the variable has
	 * in the branch-and-bound algorithm.
	 */
	public native int getVarPriority(int colnr) throws LpSolveException;

	/**
	 * Specifies if the branch-and-bound algorithm stops at first found solution.
	 */
	public native void setBreakAtFirst(boolean breakAtFirst);

	/**
	 * Returns if the branch-and-bound algorithm stops at first found solution.
	 */
	public native boolean isBreakAtFirst();

	/**
	 * Specifies if the branch-and-bound algorithm stops when the object value
	 * is better than a given value.
	 */
	public native void setBreakAtValue(double breakAtValue);

	/**
	 * Returns the value at which the branch-and-bound algorithm stops
	 * when the object value is better than this value.
	 */
	public native double getBreakAtValue();

	/**
	 * Specifies which scaling algorithm must be used.
	 */
	public native void setScaling(int scalemode);

	/**
	 * Specifies which scaling algorithm is used.
	 */
	public native int getScaling();

	/**
	 * Returns if scaling mode specified in testmask is active.
	 */
	public native boolean isScalemode(int testmask);

	/**
	 * Returns if scaling type specified in scaletype is active.
	 */
	public native boolean isScaletype(int scaletype);

	/**
	 * Specifies which scaling algorithm is used.
	 */
	public native boolean isIntegerscaling();

	/**
	 * Sets the relative scaling convergence criterion for the active scaling mode;
	 * the integer part specifies the maximum number of iterations.
	 */
	public native void setScalelimit(double scalelimit);

	/**
	 * Returns the relative scaling convergence criterion for the active scaling mode;
	 * the integer part specifies the maximum number of iterations.
	 */
	public native double getScalelimit();

	/**
	 * Specifies the iterative improvement level.
	 */
	public native void setImprove(int improve);

	/**
	 * Returns the iterative improvement level.
	 */
	public native int getImprove();

	/**
	 * Specifies the pivot rule.
	 */
	public native void setPivoting(int pivRule);

	/**
	 * Returns the pivot rule.
	 */
	public native int getPivoting();

	/**
	 * Returns if pivot strategy specified in testmask is active.
	 */
	public native boolean isPivMode(int testmask);

	/**
	 * Checks if the specified pivot rule is active.
	 */
	public native boolean isPivRule(int rule);

	/**
	 * Sets the desired combination of primal and dual simplex algorithms.
	 */
	public native void setPreferdual(boolean dodual);

	/**
	 * Sets the desired combination of primal and dual simplex algorithms.
	 */
	public native void setSimplextype(int simplextype);

	/**
	 * Returns the desired combination of primal and dual simplex algorithms.
	 */
	public native int getSimplextype();

	/**
	 * Set negative value below which variables are split into a negative
	 * and a positive part.
	 */
	public native void setNegrange(double negRange);

	/**
	 * Returns the negative value below which variables are split
	 * into a negative and a positive part.
	 */
	public native double getNegrange();

	/**
	 * Returns the total number of iterations with Branch-and-bound of the last solution.
	 */
	public native long getTotalIter();

	/**
	 * Returns the deepest Branch-and-bound level of the last solution.
	 */
	public native int getMaxLevel();

	/**
	 * Returns the total number of nodes processed in branch-and-bound.
	 */
	public native long getTotalNodes();

	/**
	 * Returns the number of rows (constraints) in the problem.
	 */
	public native int getNrows();

	/**
	 * Returns the number of original rows (constraints) in the problem.
	 */
	public native int getNorigRows();

	/**
	 * Returns the number of Lagrangian rows in the lp.
	 */
	public native int getLrows();

	/**
	 * Returns the number of columns (variables) in the problem.
	 */
	public native int getNcolumns();

	/**
	 * Returns the number of original columns (variables) in the problem.
	 */
	public native int getNorigColumns();

	/**
	 * Returns the number of non-zero elements in the matrix.
	 */
	public native int getNonzeros();

	/**
	 * Returns the original row/column where a constraint/variable was before presolve.
	 */
	public native int getOrigIndex(int index);

	/**
	 * Returns the index in the lp of the original row/column.
	 */
	public native int getLpIndex(int index);

	/**
	 * Sets an initial basis of the lp.
	 */
	public native void setBasis(int[] bascolumn, boolean nonbasic) throws LpSolveException;

	/**
	 * Guess a basis for the lp.
	 */
	public native void guessBasis(double[] guessvector, int[] basisvector) throws LpSolveException;

	/**
	 * Returns the basis of the lp.
	 */
	public native void getBasis(int[] bascolumn, boolean nonbasic) throws LpSolveException;

	/**
	 * Resets the basis to the initial basis.
	 */
	public native void resetBasis();

	/**
	 * Sets the starting base to an all slack basis (the default simplex starting basis).
	 */
	public native void defaultBasis();

	/**
	 * Specifies which basis crash mode must be used.
	 */
	public native void setBasiscrash(int mode);

	/**
	 * Returns which basis crash mode must be used.
	 */
	public native int getBasiscrash();

	/**
	 * Unscales the model.
	 */
	public native void unscale();

	/**
	 * Set basis factorization package.
	 */
	public native void setBFP(String filename) throws LpSolveException;

	/**
	 * Returns if the native (build-in) basis factorization package (BFP) is used,
	 * or an external package.
	 */
	public native boolean isNativeBFP();

	/**
	 * Returns if there is a basis factorization package (BFP) available.
	 */
	public native boolean hasBFP();

	/**
	 * Solve the model.
	 */
	public native int solve() throws LpSolveException;

	/**
	 * Solve the model via Lagrangian relaxation.
	 */
	/*
	 * According to Peter Notebaert, this method is temporarily unavailable
	 * but might be added again in later versions.

	public native int lagSolve(double startBound, int numIter) throws LpSolveException;
	*/

	/**
	 * Returns the description of a returncode of the solve function.
	 */
	public native String getStatustext(int statuscode);

	/**
	 * Checks if provided solution is a feasible solution.
	 */
	public native boolean isFeasible(double[] values, double threshold) throws LpSolveException;

	/**
	 * Returns the value of the objective function.
	 */
	public native double getObjective() throws LpSolveException;

	/**
	 * Returns the value of the objective function.
	 */
	public native double getWorkingObjective() throws LpSolveException;

	/**
	 * Returns the values of the variables.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getVariables(double[] var) throws LpSolveException;

	/**
	 * Returns the values of the variables.
	 * Returned array is allocated by the method.
	 */
	public native double[] getPtrVariables() throws LpSolveException;

	/**
	 * Returns the values of the constraints.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getConstraints(double[] var) throws LpSolveException;

	/**
	 * Returns the values of the constraints.
	 * Returned array is allocated by the method.
	 */
	public native double[] getPtrConstraints() throws LpSolveException;

	/**
	 * Returns the solution of the model.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getPrimalSolution(double[] pv) throws LpSolveException;

	/**
	 * Returns the solution of the model.
	 * Returned array is allocated by the method.
	 */
	public native double[] getPtrPrimalSolution() throws LpSolveException;

	/**
	 * Returns the solution of the model.
	 */
	public native double getVarPrimalresult(int index) throws LpSolveException;

	/**
	 * Returns the sensitivity of the constraints and the variables.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getSensitivityRhs(double[] duals, double[] dualsfrom, double[] dualstill) throws LpSolveException;

	/**
	 * Returns the sensitivity of the constraints and the variables.
	 * Returned arrays are allocated by the method.
	 * The returned array contains two elements of type double[].
	 * element [0] is the duals array, element [1] is the dualsfrom array,
	 * element [2] is the dualstill array.
	 */
	public native double[][] getPtrSensitivityRhs() throws LpSolveException;

	/**
	 * Returns the sensitivity of the constraints and the variables.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getDualSolution(double[] duals) throws LpSolveException;

	/**
	 * Returns the sensitivity of the constraints and the variables.
	 * Returned array is allocated by the method.
	 */
	public native double[] getPtrDualSolution() throws LpSolveException;

	/**
	 * Returns the sensitivity of the constraints and the variables.
	 */
	public native double getVarDualresult(int index) throws LpSolveException;

	/**
	 * Returns the sensitivity of the objective function.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getSensitivityObj(double[] objfrom, double[] objtill) throws LpSolveException;

	/**
	 * Returns the sensitivity of the objective function.
	 * Returned arrays are allocated by the method.
	 * The returned array contains two elements of type double[].
	 * element [0] is the objfrom array, element [1] is the objtill array.
	 */
	public native double[][] getPtrSensitivityObj() throws LpSolveException;

	/**
	 * Returns the sensitivity of the objective function.
	 * Passed in arrays must be allocated by the caller of the method.
	 */
	public native void getSensitivityObjex(double[] objfrom, double[] objtill,
		double[] objfromvalue, double[] objtillvalue) throws LpSolveException;

	/**
	 * Returns the sensitivity of the objective function.
	 * Returned arrays are allocated by the method.
	 * The returned array contains four elements of type double[].
	 * element [0] is the objfrom array, element [1] is the objtill array,
	 * element [2] is the objfromvalue array, element [3] is the objtillvalue array.
	 */
	public native double[][] getPtrSensitivityObjex() throws LpSolveException;

	/**
	 * Returns the Lamdba vectors (Lagrangian optimization).
	 * Passed in array must be allocated by the caller of the method.
	 */
	public native void getLambda(double[] lambda) throws LpSolveException;

	/**
	 * Returns the Lamdba vectors (Lagrangian optimization).
	 * Returned array is allocated by the method.
	 */
	public native double[] getPtrLambda() throws LpSolveException;

	/**
	 * Frees all resources allocated to this problem.
	 */
	public native void deleteLp();

	/**
	 * Write an lp model to a file.
	 */
	public native void writeLp(String filename) throws LpSolveException;

	/**
	 * Write an mps model to a file.
	 */
	public native void writeMps(String filename) throws LpSolveException;

	/**
	 * Write a model in free MPS format to a file.
	 */
	public native void writeFreeMps(String filename) throws LpSolveException;

	/**
	 * Read basis from a file and set as default basis. The info text
	 * is returned as method result.
	 */
	public native String readBasis(String filename) throws LpSolveException;

	/**
	 * Writes current basis to a file.
	 */
	public native void writeBasis(String filename) throws LpSolveException;

	/**
	 * Read settings from a parameter file.
	 */
	public native void readParams(String filename, String options) throws LpSolveException;

	/**
	 * Write settings to a parameter file.
	 */
	public native void writeParams(String filename, String options) throws LpSolveException;

	/**
	 * Resets parameters back to their default values.
	 */
	public native void resetParams();

	/**
	 * Prints the lp model. This function is meant for debugging purposes.
	 * By default, the output is stdout. However this can be changed via a call
	 * to setOutputfile.
	 */
	public native void printLp();

	/**
	 * Prints the values of the constraints of the lp.
	 * This can only be done after a successful solve.
	 * This function is meant for debugging purposes. By default, the output is stdout.
	 * However this can be changed via a call to setOutputfile.
	 */
	public native void printConstraints(int columns);

	/**
	 * Prints the values of the duals of the lp.
	 * This can only be done after a successful solve.
	 * This function is meant for debugging purposes. By default, the output is stdout.
	 * However this can be changed via a call to setOutputfile.
	 */
	public native void printDuals();

	/**
	 * Prints the scales of the lp.
	 * This can only be done after a successful solve.
	 * This function is meant for debugging purposes. By default, the output is stdout.
	 * However this can be changed via a call to setOutputfile.
	 */
	public native void printScales();

	/**
	 * Prints the tableau.
	 * This can only be done after a successful solve.
	 * This function is meant for debugging purposes. By default, the output is stdout.
	 * However this can be changed via a call to setOutputfile.
	 */
	public native void printTableau();

	/**
	 * Prints the objective value of the lp.
	 * This can only be done after a successful solve.
	 * This function is meant for debugging purposes. By default, the output is stdout.
	 * However this can be changed via a call to setOutputfile.
	 */
	public native void printObjective();

	/**
	 * Prints the solution (variables) of the lp.
	 * This can only be done after a successful solve.
	 * This function is meant for debugging purposes. By default, the output is stdout.
	 * However this can be changed via a call to setOutputfile.
	 */
	public native void printSolution(int columns);

	/**
	 * Prints a string.
	 */
	public native void printStr(String str);

	/**
	 * Defines the output for the print_* functions.
	 */
	public native void setOutputfile(String filename) throws LpSolveException;

	/**
	 * Do a generic readable data dump of key lp_solve model variables;
	 * principally for run difference and debugging purposes.
	 */
	public native void printDebugdump(String filename) throws LpSolveException;

	/**
	 * Set External Language Interfaces package.
	 */
	public native void setXLI(String filename) throws LpSolveException;

	/**
	 * Write a model to a file via the External Language Interface.
	 */
	public native void writeXLI(String filename, String options, boolean results) throws LpSolveException;

	/**
	 * Returns if there is an external language interface (XLI) set.
	 */
	public native boolean hasXLI();

	/**
	 * Returns if a build-in External Language Interfaces (XLI) is available or not.
	 */
	public native boolean isNativeXLI();

	/**
	 * Gets the index of a given column or row name in the lp.
	 * A return value of -1 indicates that the name does not exist.
	 */
	public native int getNameindex(String name, boolean isRow);

	/**
	 * Create the dual of the current model.
	 */
	public native void dualizeLp() throws LpSolveException;

	/**
	 * Returns if variable or constraint names are used.
	 */
	public native boolean isUseNames(boolean isRow);

	/**
	 * Sets if variable or constraint names are used.
	 */
	public native void setUseNames(boolean isRow, boolean useNames);

	/**
	 * Gets the value of a constraint according to provided variable values.
	 */
	public native double getConstrValue(int rownr, int count, double[] primsolution, int[] nzindex);

	/**
	 * This is an internal function that has been published for special purposes. It should generally not be used.
	 */
	public native int setBasisvar(int basisPos, int enteringCol);


	// ========================================================================
	// Callback methods.
	// ========================================================================

	/**
	 * Calls the native lp_solve method <code>put_abortfunc</code>
	 */
	private native void registerAbortfunc();

	/**
	 * Register an <code>AbortListener</code> for callback.
	 *
	 * @param listener the listener that should be called by lp_solve
	 * @param userhandle an arbitrary object that is passed to the listener on call
	 */
	public void putAbortfunc(AbortListener listener, Object userhandle) throws LpSolveException {
		abortListener = listener;
		abortUserhandle = (listener != null) ? userhandle : null;
		addLp(this);
		registerAbortfunc();
	}

	/**
	 * Calls the native lp_solve method <code>put_logfunc</code>
	 */
	private native void registerLogfunc();

	/**
	 * Register an <code>LogListener</code> for callback.
	 *
	 * @param listener the listener that should be called by lp_solve
	 * @param userhandle an arbitrary object that is passed to the listener on call
	 */
	public void putLogfunc(LogListener listener, Object userhandle) throws LpSolveException {
		logListener = listener;
		logUserhandle = (listener != null) ? userhandle : null;
		addLp(this);
		registerLogfunc();
	}

	/**
	 * Calls the native lp_solve method <code>put_msgfunc</code>
	 */
	private native void registerMsgfunc(int mask);

	/**
	 * Register an <code>MsgListener</code> for callback.
	 *
	 * @param listener the listener that should be called by lp_solve
	 * @param userhandle an arbitrary object that is passed to the listener on call
	 */
	public void putMsgfunc(MsgListener listener, Object userhandle, int mask) throws LpSolveException {
		msgListener = listener;
		msgUserhandle = (listener != null) ? userhandle : null;
		addLp(this);
		registerMsgfunc(mask);
	}

	/**
	 * Calls the native lp_solve method <code>put_bb_branchfunc</code>
	 */
	private native void registerBbBranchfunc();

	/**
	 * Register an <code>BbBranchListener</code> for callback.
	 *
	 * @param listener the listener that should be called by lp_solve
	 * @param userhandle an arbitrary object that is passed to the listener on call
	 */
	public void putBbBranchfunc(BbListener listener, Object userhandle) throws LpSolveException {
		bbBranchListener = listener;
		bbBranchUserhandle = (listener != null) ? userhandle : null;
		addLp(this);
		registerBbBranchfunc();
	}

	/**
	 * Calls the native lp_solve method <code>put_bb_nodefunc</code>
	 */
	private native void registerBbNodefunc();

	/**
	 * Register an <code>BbNodeListener</code> for callback.
	 *
	 * @param listener the listener that should be called by lp_solve
	 * @param userhandle an arbitrary object that is passed to the listener on call
	 */
	public void putBbNodefunc(BbListener listener, Object userhandle) throws LpSolveException {
		bbNodeListener = listener;
		bbNodeUserhandle = (listener != null) ? userhandle : null;
		addLp(this);
		registerBbNodefunc();
	}

	/**
	 * Stores references to LpSolve objects. The key to this map
	 * is the lp_solve lprec pointer value.
	 */
	private static Map lpMap = new HashMap();

	/**
	 * Adds a LpSolve object to the lpMap
	 * @param problem the problem to add
	 */
	private static synchronized void addLp(LpSolve problem) {
		lpMap.put(new Long(problem.lp), problem);
	}

	/**
	 * Retrieves a LpSolve object from the lpMap.
	 * @param lp lprec pointer value
	 * @return the LpSolve object or null, if not found
	 */
	private static synchronized LpSolve getLp(long lp) {
		return (LpSolve)lpMap.get(new Long(lp));
	}

	/**
	 * Removes a LpSolve object from the lpMap.
	 * @param lp lprec pointer value
	 */
	private static synchronized void removeLp(long lp) {
		lpMap.remove(new Long(lp));
	}


}
