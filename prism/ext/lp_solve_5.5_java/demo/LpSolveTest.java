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

import java.io.File;
import java.util.Arrays;

import lpsolve.*;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit tests for methods of the LpSolve class.
 *
 * @author Juergen Ebert
 */
public class LpSolveTest extends TestCase {

	public LpSolveTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	public static TestSuite suite() {
		return new TestSuite(LpSolveTest.class);
	}

	/**
	 * This test is important to check if we test against the
	 * correct lp_solve version.
	 */
	public void testVersion() throws Exception {
		VersionInfo info = LpSolve.lpSolveVersion();
		assertEquals(5, info.getMajorversion());
		assertEquals(5, info.getMinorversion());
		assertEquals(0, info.getRelease());
		/* assertEquals(2, info.getBuild()); */
	}

	public void testMakeLp() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertTrue(problem.getLp() > 0);
		problem.deleteLp();
		assertEquals(0, problem.getLp());
	}

	public void testTwoProblems() throws Exception {
		LpSolve problem1 = LpSolve.makeLp(3, 4);
		LpSolve problem2 = LpSolve.makeLp(5, 6);
		assertTrue(problem1.getLp() > 0);
		assertTrue(problem2.getLp() > 0);
		assertTrue(problem1.getLp() != problem2.getLp());

		assertEquals(3, problem1.getNrows());
		assertEquals(5, problem2.getNrows());
		assertEquals(4, problem1.getNcolumns());
		assertEquals(6, problem2.getNcolumns());
		assertEquals(3, problem1.getNorigRows());
		assertEquals(5, problem2.getNorigRows());
		assertEquals(4, problem1.getNorigColumns());
		assertEquals(6, problem2.getNorigColumns());

		problem1.deleteLp();
		problem2.deleteLp();
		assertEquals(0, problem1.getLp());
		assertEquals(0, problem2.getLp());
	}

	/**
	 * Set up a test model that is used in a number of test cases
	 */
	private LpSolve setupProblem() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		return problem;
	}

	/**
	 * Verify a test model that is used in a number of test cases
	 */
	private void verifyProblem(LpSolve problem) throws Exception {
		assertEquals(2, problem.getNrows());
		assertEquals(4, problem.getNcolumns());
		double[] objFn = problem.getPtrRow(0);
		assertEquals(4+1, objFn.length);
		assertEquals(2, objFn[1], 1e-8);
		assertEquals(3, objFn[2], 1e-8);
		assertEquals(-2, objFn[3], 1e-8);
		assertEquals(3, objFn[4], 1e-8);
	}

	public void testReadWriteLp() throws Exception {
		String filename = "testmodel.lp";
		File file = new File(filename);
		if (file.exists()) file.delete();

		LpSolve problem = setupProblem();
		problem.writeLp(filename);
		assertTrue(file.exists());
		problem.deleteLp();

		problem = LpSolve.readLp(filename, LpSolve.NORMAL, "Testmodel LP");
		verifyProblem(problem);
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

	public void testReadWriteLpNative() throws Exception {
		String filename = "model_äüö.lp";	// use german native chars in filename
		File file = new File(filename);
		if (file.exists()) file.delete();

		LpSolve problem = setupProblem();
		problem.writeLp(filename);
		assertTrue(file.exists());
		problem.deleteLp();

		problem = LpSolve.readLp(filename, LpSolve.NORMAL, "Testmodel LP");
		verifyProblem(problem);
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

	public void testReadWriteMps() throws Exception {
		String filename = "testmodel.mps";
		File file = new File(filename);
		if (file.exists()) file.delete();

		LpSolve problem = setupProblem();
		problem.writeMps(filename);
		assertTrue(file.exists());
		problem.deleteLp();

		problem = LpSolve.readMps(filename, LpSolve.NORMAL);
		verifyProblem(problem);
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

	public void testReadWriteFreeMps() throws Exception {
		String filename = "testmodel.fmps";
		File file = new File(filename);
		if (file.exists()) file.delete();

		LpSolve problem = setupProblem();
		problem.writeFreeMps(filename);
		assertTrue(file.exists());
		problem.deleteLp();

		problem = LpSolve.readFreeMps(filename, LpSolve.NORMAL);
		verifyProblem(problem);
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

        // API call copy_lp doesn't work yet. Test is useless */
        /*
	public void testCopyLp() throws Exception {
		LpSolve problem = setupProblem();
		LpSolve copy = problem.copyLp();
		verifyProblem(problem);
		verifyProblem(copy);
		problem.deleteLp();
		copy.deleteLp();
	}
        */

	// TODO: find out under what conditions to call dualizeLp
	/*
	public void testDualizeLp() throws Exception {
		LpSolve problem = setupProblem();
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		// No idea what this does; just call it to see if it crashes
		problem.dualizeLp();
		problem.deleteLp();
	}
	*/


	/**
	 * Helper method for the various xli_* variants
	 */
	private void readWriteXLI(String name) throws Exception {
		String filename = "testmodel_" + name + ".txt";
		File file = new File(filename);
		if (file.exists()) file.delete();

		String xliname = "xli_" + name;
		LpSolve problem = setupProblem();
		problem.setXLI(xliname);
		problem.writeXLI(filename, null, false);
		assertTrue(file.exists());
		problem.deleteLp();

		problem = LpSolve.readXLI(xliname, filename, null, null, LpSolve.NORMAL);
		verifyProblem(problem);
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

	public void testReadWriteXLI_CPLEX() throws Exception {
		readWriteXLI("CPLEX");
	}

	public void testReadWriteXLI_LINDO() throws Exception {
		readWriteXLI("LINDO");
	}

	/* Not used, because xli_LPFML is currently not available on LInux
	 * and requires additional lis (Xerces , ..)
	public void testReadWriteXLI_LPFML() throws Exception {
		readWriteXLI("LPFML");
	}
	*/

	/* Not used, because xli_MathProg cannot write models
	public void testReadWriteXLI_MathProg() throws Exception {
		readWriteXLI("MathProg");
	}
	*/

	public void testSetXLI_Unknown() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		try {
			problem.setXLI("Unknown");
			fail("Setting a nonexistent XLI should throw an exception");
		}
		catch (LpSolveException e) {
			// OK
                        System.out.println("setXLI failed as expected");
		}
		problem.deleteLp();
	}

	public void testHasXLI() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertFalse(problem.isNativeXLI()); 	// is always FALSE
		assertFalse(problem.hasXLI());
		problem.setXLI("xli_CPLEX");
		assertTrue(problem.hasXLI());
		problem.deleteLp();
	}

	public void testresizeLp() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		problem.resizeLp(4, 5);
		problem.deleteLp();
	}

	public void testAddColumnex() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 0);
		assertEquals(0, problem.getNcolumns());
		double[] column = { 1.5, 3.5 };
		int[] rowno = { 1, 3 };
		problem.addColumnex(2, column, rowno);
		assertEquals(1, problem.getNcolumns());
		double[] col = problem.getPtrColumn(1);
		assertEquals(0, col[0], 1e-8);
		assertEquals(1.5, col[1], 1e-8);
		assertEquals(0, col[2], 1e-8);
		assertEquals(3.5, col[3], 1e-8);
		problem.deleteLp();
	}

	public void testSetColumn() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 0);
		problem.strAddColumn("1 2 3 4");
		assertEquals(1, problem.getNcolumns());
		double[] col = problem.getPtrColumn(1);
		assertEquals(1, col[0], 1e-8);
		assertEquals(2, col[1], 1e-8);
		assertEquals(3, col[2], 1e-8);
		assertEquals(4, col[3], 1e-8);
		double[] newcol = { 5, 6, 7, 8 };
		problem.setColumn(1, newcol);
		col = problem.getPtrColumn(1);
		assertEquals(5, col[0], 1e-8);
		assertEquals(6, col[1], 1e-8);
		assertEquals(7, col[2], 1e-8);
		assertEquals(8, col[3], 1e-8);
		problem.deleteLp();
	}

	public void testSetColumnex() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 0);
		problem.strAddColumn("1 2 3 4");
		assertEquals(1, problem.getNcolumns());
		double[] col = problem.getPtrColumn(1);
		assertEquals(1, col[0], 1e-8);
		assertEquals(2, col[1], 1e-8);
		assertEquals(3, col[2], 1e-8);
		assertEquals(4, col[3], 1e-8);
		double[] newcol = { 6, 8 };
		int[] rowno = { 1, 3 };
		problem.setColumnex(1, 2, newcol, rowno);
		col = problem.getPtrColumn(1);
		assertEquals(0, col[0], 1e-8);	// unspecified values are set to zero !?
		assertEquals(6, col[1], 1e-8);
		assertEquals(0, col[2], 1e-8);
		assertEquals(8, col[3], 1e-8);	// unspecified values are set to zero !?
		problem.deleteLp();
	}

	public void testSetRow() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("1 2 3 4", LpSolve.LE, 4);
		double[] ptrRow = problem.getPtrRow(1);
		assertEquals(1+4, ptrRow.length);
		assertEquals(1, ptrRow[1], 1e-8);
		assertEquals(2, ptrRow[2], 1e-8);
		double[] newRow = { 0, 5, 6, 7, 8 };	// element 0 is unused
		problem.setRow(1, newRow);
		ptrRow = problem.getPtrRow(1);
		assertEquals(5, ptrRow[1], 1e-8);
		assertEquals(6, ptrRow[2], 1e-8);
		problem.deleteLp();
	}

	public void testSetRowex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("1 2 3 4", LpSolve.LE, 4);
		double[] ptrRow = problem.getPtrRow(1);
		assertEquals(1+4, ptrRow.length);
		assertEquals(1, ptrRow[1], 1e-8);
		assertEquals(2, ptrRow[2], 1e-8);
		assertEquals(3, ptrRow[3], 1e-8);
		assertEquals(4, ptrRow[4], 1e-8);
		double[] newRow = { 5, 7 };
		int[] colno = { 1, 3 };
		problem.setRowex(1, 2, newRow, colno);
		ptrRow = problem.getPtrRow(1);
		assertEquals(5, ptrRow[1], 1e-8);
		assertEquals(0, ptrRow[2], 1e-8);	 // unspecified values are set to 0
		assertEquals(7, ptrRow[3], 1e-8);
		assertEquals(0, ptrRow[4], 1e-8);
		problem.deleteLp();
	}

	public void testGetSetBasiscrash() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertEquals(LpSolve.CRASH_NOTHING, problem.getBasiscrash());	// default
		problem.setBasiscrash(LpSolve.CRASH_MOSTFEASIBLE);
		assertEquals(LpSolve.CRASH_MOSTFEASIBLE, problem.getBasiscrash());
		problem.deleteLp();
	}

	public void testGetSetBbDepthlimit() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertEquals(-50, problem.getBbDepthlimit());	// default
		problem.setBbDepthlimit(10);
		assertEquals(10, problem.getBbDepthlimit());
		problem.deleteLp();
	}

	public void testGetSetSimplextype() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertEquals(LpSolve.SIMPLEX_DEFAULT, problem.getSimplextype());	// default
		assertEquals(LpSolve.SIMPLEX_DUAL_PRIMAL, problem.getSimplextype());	// default
		problem.setSimplextype(LpSolve.SIMPLEX_PRIMAL_DUAL);
		assertEquals(LpSolve.SIMPLEX_PRIMAL_DUAL, problem.getSimplextype());
		problem.deleteLp();
	}

	public void testSetPreferdual() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		problem.setPreferdual(true);
		assertEquals(LpSolve.SIMPLEX_DUAL_DUAL, problem.getSimplextype());
		problem.setPreferdual(false);
		assertEquals(LpSolve.SIMPLEX_PRIMAL_PRIMAL, problem.getSimplextype());
		problem.deleteLp();
	}

	public void testGetSetScalelimit() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertEquals(5, problem.getScalelimit(), 1e-8);	 // default
		problem.setScalelimit(10);
		assertEquals(10, problem.getScalelimit(), 1e-8);
		problem.deleteLp();
	}

	public void testGetStatustext() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		problem.setVerbose(LpSolve.IMPORTANT);
		int status = problem.solve();
		String text = problem.getStatustext(status);
		assertNotNull(text);
		assertTrue(text.length() > 0);
		problem.deleteLp();
	}

	public void testSetBFP() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertTrue(problem.hasBFP());
		assertTrue(problem.isNativeBFP());
		// This requires the bfp_LUSOL.dll in the path !!
		problem.setBFP("bfp_LUSOL");
		assertTrue(problem.hasBFP());
		assertFalse(problem.isNativeBFP());
		problem.deleteLp();
	}

	public void testSetBFPException() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		try {
			problem.setBFP("nonexistent");
			fail("Setting a nonexistent BFP should throw an exception");
		} catch (LpSolveException e) {
			// OK
                        System.out.println("setBFP failed as expected");
		}
		problem.deleteLp();
	}

	public void testSetUnbounded() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		assertFalse(problem.isUnbounded(1));
		problem.setUnbounded(1);
		assertTrue(problem.isUnbounded(1));
		problem.deleteLp();
	}

	public void testIsNegative() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		assertFalse(problem.isNegative(1));
		problem.setLowbo(1, -10);
		problem.setUpbo(1, -1);
		assertTrue(problem.isNegative(1));
		problem.deleteLp();
	}

	public void testGetSetUpbo() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		assertEquals(1E30, problem.getUpbo(1), 1e-8);	 // default
		problem.setUpbo(1, 1234);
		assertEquals(1234, problem.getUpbo(1), 1e-8);
		problem.deleteLp();
	}

	public void testGetSetLowbo() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		assertEquals(0.0, problem.getLowbo(1), 1e-8);	 // default
		problem.setLowbo(1, -1234);
		assertEquals(-1234, problem.getLowbo(1), 1e-8);
		problem.deleteLp();
	}

	public void testSetBounds() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		problem.setBounds(1, -1234, 5555);
		assertEquals(-1234, problem.getLowbo(1), 1e-8);
		assertEquals(5555, problem.getUpbo(1), 1e-8);
		problem.deleteLp();
	}

	public void testWriteDebugdump() throws Exception {
		String filename = "debugdump.txt";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.printDebugdump(file.getAbsolutePath());
		problem.deleteLp();
		assertTrue(file.exists());
		if (file.exists()) file.delete();
	}

	public void testSetOutputfile() throws Exception {
		String filename = "print_file.txt";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setOutputfile(file.getAbsolutePath());
		problem.setOutputfile(null);
		problem.deleteLp();
		assertTrue(file.exists());
		if (file.exists()) file.delete();
	}

	public void testPrintSolution() throws Exception {
		String filename = "print_solution.txt";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setOutputfile(file.getAbsolutePath());
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.printLp();
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.printSolution(1);
		problem.printConstraints(1);
		problem.printObjective();
		problem.printDuals();
		problem.printScales();
		problem.setOutputfile(null);
		problem.deleteLp();
		assertTrue(file.exists());
		if (file.exists()) file.delete();
	}

	public void testPrintTableau() throws Exception {
		String filename = "print_tableau.txt";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setOutputfile(file.getAbsolutePath());
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.printTableau();
		problem.setOutputfile(null);
		problem.deleteLp();
		assertTrue(file.exists());
		assertTrue(file.canRead());
		if (file.exists()) file.delete();
	}

	public void testPrintStr() throws Exception {
		String filename = "print_str.txt";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setOutputfile(file.getAbsolutePath());
		problem.printStr("Testüöäß");
		problem.printStr(null);
		problem.setOutputfile(null);
		problem.deleteLp();
		assertTrue(file.exists());
		assertTrue(file.length() >= 8);
		if (file.exists()) file.delete();
	}

	public void testSetObj() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		double[] objFn = problem.getPtrRow(0);
		assertEquals(4+1, objFn.length);
		assertEquals(2, objFn[1], 1e-8);
		problem.setObj(1, 6.6);
		objFn = problem.getPtrRow(0);
		assertEquals(4+1, objFn.length);
		assertEquals(6.6, objFn[1], 1e-8);
		problem.deleteLp();
	}

	public void testGetRow() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		int size = 1 + problem.getNcolumns();
		double[] row = new double[size];
		problem.getRow(1, row);
		double[] ptrRow = problem.getPtrRow(1);
		assertEquals(size, ptrRow.length);
		assertEquals(3, row[1], 1e-8);
		for (int i = 0; i < size; i++) {
			assertEquals(row[i], ptrRow[i], 1e-8);
		}
		problem.deleteLp();
	}

	public void testGetRowex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strSetObjFn("2 3 -2 3");
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 0 1", LpSolve.GE, 3);
		int size = problem.getNcolumns();
		double[] row = new double[size];
		int[] nzcols = new int[size];

		int nzcount = problem.getRowex(1, row, nzcols);
		assertEquals(4, nzcount);
		assertEquals(3, row[0], 1e-8);
		assertEquals(2, row[1], 1e-8);
		assertEquals(2, row[2], 1e-8);
		assertEquals(1, row[3], 1e-8);
		assertEquals(1, nzcols[0]);
		assertEquals(2, nzcols[1]);
		assertEquals(3, nzcols[2]);
		assertEquals(4, nzcols[3]);

		nzcount = problem.getRowex(2, row, nzcols);
		assertEquals(2, nzcount);
		assertEquals(4, row[0], 1e-8);
		assertEquals(1, row[1], 1e-8);
		assertEquals(2, nzcols[0]);
		assertEquals(4, nzcols[1]);

		problem.deleteLp();
	}

	public void testGetColumn() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strSetObjFn("2 3 -2 3");
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		int size = 1 + problem.getNrows();
		double[] column = new double[size];
		problem.getColumn(1, column);
		double[] ptrColumn = problem.getPtrColumn(1);
		assertEquals(size, ptrColumn.length);
		assertEquals(2, column[0], 1e-8);
		for (int i = 0; i < size; i++) {
			assertEquals(column[i], ptrColumn[i], 1e-8);
		}
		problem.deleteLp();
	}

	public void testGetColumnex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strSetObjFn("2 3 -2 3");
		problem.strAddConstraint("0 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("3 4 0 1", LpSolve.GE, 3);
		int size = 1 + problem.getNrows();
		double[] col = new double[size];
		int[] nzrows = new int[size];

		int nzcount = problem.getColumnex(1, col, nzrows);
		assertEquals(2, nzcount);
		assertEquals(2, col[0], 1e-8);
		assertEquals(3, col[1], 1e-8);
		assertEquals(0, nzrows[0]);
		assertEquals(2, nzrows[1]);

		nzcount = problem.getColumnex(2, col, nzrows);
		assertEquals(3, nzcount);
		assertEquals(3, col[0], 1e-8);
		assertEquals(2, col[1], 1e-8);
		assertEquals(4, col[2], 1e-8);
		assertEquals(0, nzrows[0]);
		assertEquals(1, nzrows[1]);
		assertEquals(2, nzrows[2]);

		problem.deleteLp();
	}

	public void testMinimMaxim() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertFalse(problem.isMaxim());	// default
		problem.setMaxim();
		assertTrue(problem.isMaxim());
		problem.setMinim();
		assertFalse(problem.isMaxim());
		problem.setMaxim();
		assertTrue(problem.isMaxim());
		problem.setSense(false);
		assertFalse(problem.isMaxim());
		problem.deleteLp();
	}

	public void testAddDelConstraint() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(0, problem.getNrows());
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		assertEquals(1, problem.getNrows());
		double[] row = { 0.0, 1.0, 2.0, 3.0, 4.0 };
		problem.addConstraint(row, LpSolve.GE, 5.0);
		assertEquals(2, problem.getNrows());
		problem.delConstraint(1);
		assertEquals(1, problem.getNrows());
		problem.delConstraint(1);
		assertEquals(0, problem.getNrows());
		problem.deleteLp();
	}

	public void testAddConstraintex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(0, problem.getNrows());
		double[] constr = { 1.5, 3.5 };
		int[] colno = { 1, 3 };
		problem.addConstraintex(2, constr, colno, LpSolve.GE, 10);
		assertEquals(1, problem.getNrows());
		double[] row = problem.getPtrRow(1);
		assertEquals(5, row.length);
		assertEquals(1.5, row[1], 1e-8);
		assertEquals(0.0, row[2], 1e-8);
		assertEquals(3.5, row[3], 1e-8);
		problem.deleteLp();
	}

	public void testGetNonzeros() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(0, problem.getNonzeros());
		double[] constr = { 1.5, 3.5 };
		int[] colno = { 1, 3 };
		problem.addConstraintex(2, constr, colno, LpSolve.GE, 10);
		assertEquals(2, problem.getNonzeros());
		problem.deleteLp();
	}

	public void testSetObjFn() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strSetObjFn("1 -2 3 -4");
		double[] objFn = problem.getPtrRow(0);
		assertEquals(5, objFn.length);
		assertEquals(1, objFn[1], 1e-8);
		assertEquals(-2, objFn[2], 1e-8);
		assertEquals(3, objFn[3], 1e-8);
		assertEquals(-4, objFn[4], 1e-8);

		problem.setObjFn(new double[] {0, 9, -8, 7, -6});
		objFn = problem.getPtrRow(0);
		assertEquals(9, objFn[1], 1e-8);
		assertEquals(-8, objFn[2], 1e-8);
		assertEquals(7, objFn[3], 1e-8);
		assertEquals(-6, objFn[4], 1e-8);
		problem.deleteLp();
	}

	public void testSetObjFnex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		double[] objfn = { 1.5, 3.5 };
		int[] colno = { 2, 4 };
		problem.setObjFnex(2, objfn, colno);
		double[] row = problem.getPtrRow(0);
		assertEquals(5, row.length);
		assertEquals(1.5, row[2], 1e-8);
		assertEquals(0.0, row[3], 1e-8);
		assertEquals(3.5, row[4], 1e-8);
		problem.deleteLp();
	}

	/* TODO: activate if *Lag* functiona are supported again
	public void testAddLagConstraint() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(0, problem.getLrows());
		problem.strAddLagCon("3 2 2 1", LpSolve.LE, 4);
		assertEquals(1, problem.getLrows());
		double[] row = { 0.0, 1.0, 2.0, 3.0, 4.0 };
		problem.addLagCon(row, LpSolve.GE, 5.0);
		assertEquals(2, problem.getLrows());
		problem.deleteLp();
	}
	*/

	public void testAddDelColumn() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 0);
		assertEquals(0, problem.getNcolumns());
		problem.strAddColumn("3 2 2 1");
		assertEquals(1, problem.getNcolumns());
		double[] column = { 0.0, 1.0, 2.0, 3.0, 4.0 };
		problem.addColumn(column);
		assertEquals(2, problem.getNcolumns());
		problem.delColumn(1);
		assertEquals(1, problem.getNcolumns());
		problem.delColumn(1);
		assertEquals(0, problem.getNcolumns());
		problem.deleteLp();
	}

	public void testDelColumnWithException() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		try {
			problem.delColumn(1);
			fail("delColumn with nonexistent col shouold throw an Exception");
		}
		catch (LpSolveException e) {
			// OK
                        System.out.println("delColumn failed as expected");
		}
		problem.deleteLp();
	}

	public void testColumnInLp() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 0);
		double[] column1 = { 0.0, 1.0, 2.0, 3.0, 4.0 };
		double[] column2 = { 0.0, 5.0, 6.0, 7.0, 8.0 };
		problem.addColumn(column1);
		problem.addColumn(column2);
		assertEquals(2, problem.getNcolumns());
		assertEquals(1, problem.columnInLp(column1));
		assertEquals(2, problem.columnInLp(column2));
		column1[1] = 100.6;
		assertEquals(0, problem.columnInLp(column1));
		assertEquals(2, problem.columnInLp(column2));
		problem.deleteLp();
	}

	public void testGetSetLpName() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals("", problem.getLpName());
		problem.setLpName("test");
		assertEquals("test", problem.getLpName());
		problem.setLpName(null);
		assertEquals("", problem.getLpName());
		problem.deleteLp();
	}

	public void testGetSetRowName() throws Exception {
		LpSolve problem = LpSolve.makeLp(1, 0);
		assertEquals("R1", problem.getRowName(1));	// default
		problem.setRowName(1, "test");
		assertEquals("test", problem.getRowName(1));
		assertEquals("test", problem.getOrigrowName(1));
		problem.deleteLp();
	}

	public void testGetSetColName() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 1);
		assertEquals("C1", problem.getColName(1));	// default
		problem.setColName(1, "test");
		assertEquals("test", problem.getColName(1));
		assertEquals("test", problem.getOrigcolName(1));
		problem.deleteLp();
	}

	public void testGetSetMat() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(0.0, problem.getMat(1, 1), 1e-8);	 // default
		problem.setMat(1, 1, 0.5);
		assertEquals(0.5, problem.getMat(1, 1), 1e-8);
		problem.deleteLp();
	}

	public void testGetSetVerbose() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(LpSolve.NORMAL, problem.getVerbose());	// default
		problem.setVerbose(LpSolve.DETAILED);
		assertEquals(LpSolve.DETAILED, problem.getVerbose());
		problem.setVerbose(LpSolve.SEVERE);
		assertEquals(LpSolve.SEVERE, problem.getVerbose());
		problem.deleteLp();
	}

	public void testGetSetTimeout() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(0, problem.getTimeout());	// default
		problem.setTimeout(1234);
		assertEquals(1234, problem.getTimeout());
		problem.deleteLp();
	}

	public void testGetSetPrintSol() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(LpSolve.FALSE, problem.getPrintSol());	// default
		problem.setPrintSol(LpSolve.TRUE);
		assertEquals(LpSolve.TRUE, problem.getPrintSol());
		problem.deleteLp();
	}

	public void testGetSetDebug() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(false, problem.isDebug());	// default
		problem.setDebug(true);
		assertEquals(true, problem.isDebug());
		problem.deleteLp();
	}

	public void testGetSetTrace() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(false, problem.isTrace());	// default
		problem.setTrace(true);
		assertEquals(true, problem.isTrace());
		problem.deleteLp();
	}

	public void testGetSetLagTrace() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(false, problem.isLagTrace());	// default
		problem.setLagTrace(true);
		assertEquals(true, problem.isLagTrace());
		problem.deleteLp();
	}

	public void testSetAddRowmode() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(false, problem.isAddRowmode());	// default
		assertEquals(false, problem.setAddRowmode(false));
		assertEquals(true, problem.setAddRowmode(true));
		assertEquals(true, problem.isAddRowmode());
		problem.deleteLp();
	}

	/* Reproduces a problem reported in the mailing list */
	public void testSetAddRowmode2() throws Exception {
		// Do not predefine rows in makeLp when using addConstraint !
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setAddRowmode(true);
		assertEquals(true, problem.isAddRowmode());
		double[] objFn = { 0, 2, 3, -2, 3 };
		problem.setObjFn(objFn);
		double[] row = { 0.0, 1.0, 2.0, 3.0, 4.0 };
		problem.addConstraint(row, LpSolve.GE, 5.0);
		problem.setAddRowmode(false);

		objFn = problem.getPtrRow(0);
		assertEquals(2, objFn[1], 1e-8);
		assertEquals(3, objFn[2], 1e-8);
		problem.deleteLp();
	}

	public void testGetSetAntiDegen() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		problem.setAntiDegen(LpSolve.ANTIDEGEN_DYNAMIC);
		assertEquals(true, problem.isAntiDegen(LpSolve.ANTIDEGEN_DYNAMIC));
		assertEquals(LpSolve.ANTIDEGEN_DYNAMIC, problem.getAntiDegen());
		problem.deleteLp();
	}

	public void testGetSetPresolve() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(true, problem.isPresolve(LpSolve.PRESOLVE_NONE));	// default
		problem.setPresolve(LpSolve.PRESOLVE_ROWS | LpSolve.PRESOLVE_COLS, 2);
		assertTrue(problem.isPresolve(LpSolve.PRESOLVE_ROWS | LpSolve.PRESOLVE_COLS));
		assertTrue(problem.isPresolve(LpSolve.PRESOLVE_ROWS));
		assertEquals(LpSolve.PRESOLVE_ROWS | LpSolve.PRESOLVE_COLS, problem.getPresolve());
		problem.deleteLp();
	}

	public void testGetPresolveloops() throws Exception {
		LpSolve problem = setupProblem();
		problem.setPresolve(LpSolve.PRESOLVE_ROWS | LpSolve.PRESOLVE_COLS, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		assertEquals(1, problem.getPresolveloops());
		problem.deleteLp();
	}

	public void testGetSetMaxpivot() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(250, problem.getMaxpivot());	// default for LUSOL
		problem.setMaxpivot(44);
		assertEquals(44, problem.getMaxpivot());
		problem.deleteLp();
	}

	public void testGetSetBbRule() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		problem.setBbRule(LpSolve.NODE_RANGESELECT);
		assertEquals(LpSolve.NODE_RANGESELECT, problem.getBbRule());
		problem.deleteLp();
	}

	public void testGetSetObjBound() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(problem.getInfinite(), problem.getObjBound(), 1e-8);	 // default
		problem.setObjBound(1234.56);
		assertEquals(1234.56, problem.getObjBound(), 1e-8);
		problem.deleteLp();
	}

	public void testGetSetFloorFirst() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(LpSolve.BRANCH_AUTOMATIC, problem.getBbFloorfirst());	// default
		problem.setBbFloorfirst(LpSolve.BRANCH_CEILING);
		assertEquals(LpSolve.BRANCH_CEILING, problem.getBbFloorfirst());
		problem.deleteLp();
	}

	public void testGetSetUseNames() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 4);
		assertEquals(true, problem.isUseNames(true));	// default
		assertEquals(true, problem.isUseNames(false));	// default
		problem.setUseNames(true, false);
		problem.setUseNames(false, false);
		assertEquals(false, problem.isUseNames(true));
		assertEquals(false, problem.isUseNames(false));
		problem.deleteLp();
	}

	public void testGetObjectiveGetSolution() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		int rc = problem.solve();
		assertEquals(LpSolve.OPTIMAL, rc);
		assertEquals(-4.0, problem.getObjective(), 1e-8);
		double[] solution = problem.getPtrPrimalSolution();
		int size = 1 + problem.getNrows() + problem.getNcolumns();
		assertEquals(size, solution.length);
		int firstVar = 1 + problem.getNrows();
		assertEquals(0.0, solution[firstVar + 0], 1e-8);
		assertEquals(0.0, solution[firstVar + 1], 1e-8);
		assertEquals(2.0, solution[firstVar + 2], 1e-8);
		assertEquals(0.0, solution[firstVar + 3], 1e-8);
		problem.deleteLp();
	}

	public void testPrimalSolution() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		double[] objFn = { 0, 2, 3, -2, 3 };
		problem.setObjFn(objFn);
		problem.setVerbose(LpSolve.IMPORTANT);
		int rc = problem.solve();
		double[] solution1 = problem.getPtrPrimalSolution();
		double[] solution2 = new double[1 + 2 + 4];
		problem.getPrimalSolution(solution2);
		Arrays.equals(solution1, solution2);
		problem.deleteLp();
	}

	public void testVarDualresult() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.setObjFn(new double[] { 0, 2, 3, -2, 3 });
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		assertEquals(4.0, problem.getVarPrimalresult(1), 1e-8);
		// TODO: the following used to be 0.0 until version 5.5 !!
		assertEquals(-1.0, problem.getVarDualresult(1), 1e-8);
		problem.deleteLp();
	}

	public void testGetPtrVariables() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		double[] var = problem.getPtrVariables();
		assertEquals(problem.getNcolumns(), var.length);
		assertEquals(0.0, var[0], 1e-8);
		assertEquals(0.0, var[1], 1e-8);
		assertEquals(2.0, var[2], 1e-8);
		assertEquals(0.0, var[3], 1e-8);
		problem.deleteLp();
	}

	public void testGetVariables() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		double[] var = new double[4];
		problem.getVariables(var);
		assertEquals(problem.getNcolumns(), var.length);
		assertEquals(0.0, var[0], 1e-8);
		assertEquals(0.0, var[1], 1e-8);
		assertEquals(2.0, var[2], 1e-8);
		assertEquals(0.0, var[3], 1e-8);
		problem.deleteLp();
	}

	public void testGetPtrConstraints() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		double[] var = problem.getPtrConstraints();
		assertEquals(problem.getNrows(), var.length);
		assertEquals(4.0, var[0], 1e-8);
		assertEquals(6.0, var[1], 1e-8);
		problem.deleteLp();
	}

	public void testGetConstraints() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		double[] var = new double[2];
		problem.getConstraints(var);
		assertEquals(problem.getNrows(), var.length);
		assertEquals(4.0, var[0], 1e-8);
		assertEquals(6.0, var[1], 1e-8);
		problem.deleteLp();
	}

	public void testRhVec() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		assertEquals(4.0, problem.getRh(1), 1e-8);
		assertEquals(3.0, problem.getRh(2), 1e-8);
		problem.strSetRhVec("1 2");
		assertEquals(1.0, problem.getRh(1), 1e-8);
		assertEquals(2.0, problem.getRh(2), 1e-8);
		problem.setRhVec(new double[] {0.0, 6.0, 7.5});
		assertEquals(6.0, problem.getRh(1), 1e-8);
		assertEquals(7.5, problem.getRh(2), 1e-8);
		problem.deleteLp();
	}

	public void testRh() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		assertEquals(4.0, problem.getRh(1), 1e-8);
		assertEquals(3.0, problem.getRh(2), 1e-8);
		problem.setRh(1, 1.0);
		problem.setRh(2, 2.0);
		assertEquals(1.0, problem.getRh(1), 1e-8);
		assertEquals(2.0, problem.getRh(2), 1e-8);
		problem.deleteLp();
	}

	public void testConstrType() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		assertEquals(LpSolve.LE, problem.getConstrType(1));
		assertEquals(LpSolve.GE, problem.getConstrType(2));
		problem.setConstrType(1, LpSolve.EQ);
		assertEquals(LpSolve.EQ, problem.getConstrType(1));
		problem.deleteLp();
	}

	// TODO: enable test after problems with add_SOS are solved
	/*
	public void testAddSOS() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		int[] vars = { 2, 3 };
		double[] weights = { 1.0, 2.0 };
		problem.addSOS("SOS", 3, 1, 2, vars, weights);
		assertFalse(problem.isSOSVar(1));
		assertTrue(problem.isSOSVar(2));
		assertTrue(problem.isSOSVar(3));
		assertFalse(problem.isSOSVar(4));
		problem.deleteLp();
	}
	*/

	public void testRhRange() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.setRhRange(1, 20.5);
		assertEquals(20.5, problem.getRhRange(1), 1e-8);
		problem.deleteLp();
	}

	public void testInt() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(false, problem.isInt(1));
		problem.setInt(1, true);
		assertEquals(true, problem.isInt(1));
		problem.setInt(1, false);
		assertEquals(false, problem.isInt(1));
		problem.deleteLp();
	}

	public void testBinary() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(false, problem.isBinary(1));
		problem.setBinary(1, true);
		assertEquals(true, problem.isBinary(1));
		problem.setBinary(1, false);
		assertEquals(false, problem.isBinary(1));
		problem.deleteLp();
	}

	public void testSemicont() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(false, problem.isSemicont(1));
		problem.setSemicont(1, true);
		assertEquals(true, problem.isSemicont(1));
		problem.setSemicont(1, false);
		assertEquals(false, problem.isSemicont(1));
		problem.deleteLp();
	}

	public void testInfinite() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(1E30, problem.getInfinite(), 1e-8); // default
		assertTrue(problem.isInfinite(problem.getInfinite()));
		assertFalse(problem.isInfinite(3E22));
		problem.setInfinite(2.1E22);
		assertEquals(2.1E22, problem.getInfinite(), 1e-8);
		assertTrue(problem.isInfinite(3E22));
		problem.deleteLp();
	}

	public void testEpsint() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(1E-7, problem.getEpsint(), 1e-8);	 // default
		problem.setEpsint(0.0001);
		assertEquals(0.0001, problem.getEpsint(), 1e-8);
		problem.deleteLp();
	}

	public void testEpsb() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(1e-10, problem.getEpsb(), 1e-8);	 // default
		problem.setEpsb(0.0001);
		assertEquals(0.0001, problem.getEpsb(), 1e-8);
		problem.deleteLp();
	}

	public void testEpsd() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(1e-9, problem.getEpsd(), 1e-8);	 // default
		problem.setEpsd(0.0001);
		assertEquals(0.0001, problem.getEpsd(), 1e-8);
		problem.deleteLp();
	}

	public void testEpsel() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(1e-12, problem.getEpsel(), 1e-8);	 // default
		problem.setEpsel(0.0001);
		assertEquals(0.0001, problem.getEpsel(), 1e-8);
		problem.deleteLp();
	}

	public void testEpspivot() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(2e-7, problem.getEpspivot(), 1e-8); // default
		problem.setEpspivot(0.0001);
		assertEquals(0.0001, problem.getEpspivot(), 1e-8);
		problem.deleteLp();
	}

	public void testEpsperturb() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(1e-5, problem.getEpsperturb(), 1e-8);	 // default
		problem.setEpsperturb(0.0001);
		assertEquals(0.0001, problem.getEpsperturb(), 1e-8);
		problem.deleteLp();
	}

	// TODO: find out under what conditions to call setEpslevel
	/*
	public void testEpslevel() throws Exception {
		LpSolve problem = setupProblem();
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		// No idea what this method does and how to verify it.
		// Just call it to see if it crashes.
		problem.setEpslevel(5);
		problem.deleteLp();
	}
	*/

	public void testGetStatus() throws Exception {
		LpSolve problem = LpSolve.makeLp(3, 0);
		// I have no idea what these status values mean
		assertEquals(-1, problem.getStatus());
		try {
			problem.strAddColumn("1 2 3"); // should be 4 elements
			fail("this should throw an exception");
		}
		catch (LpSolveException e) {
                        System.out.println("strAddColumn failed as expected");
                        assertEquals(-4, problem.getStatus());
		}
		problem.deleteLp();
	}

	public void testMipGap() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setMipGap(true, 0.0001);
		assertEquals(0.0001, problem.getMipGap(true), 1e-8);
		problem.deleteLp();
	}

	public void testVarBranch() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.setVarBranch(1, LpSolve.BRANCH_AUTOMATIC);
		assertEquals(LpSolve.BRANCH_AUTOMATIC, problem.getVarBranch(1));
		problem.deleteLp();
	}

	public void testVarWeights() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		assertEquals(1, problem.getVarPriority(1));
		assertEquals(2, problem.getVarPriority(2));
		double[] weights = { 2, 1 };
		problem.setVarWeights(weights);
		assertEquals(2, problem.getVarPriority(1));
		assertEquals(1, problem.getVarPriority(2));
		problem.deleteLp();
	}

	public void testBreakAtFirst() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertFalse(problem.isBreakAtFirst());
		problem.setBreakAtFirst(true);
		assertTrue(problem.isBreakAtFirst());
		problem.deleteLp();
	}

	public void testBreakAtValue() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(- problem.getInfinite(), problem.getBreakAtValue(), 1e-8);
		problem.setBreakAtValue(-1.2E22);
		assertEquals(-1.2E22, problem.getBreakAtValue(), 1e-8);
		problem.deleteLp();
	}

	public void testScaling() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		int mode = (LpSolve.SCALE_EXTREME | LpSolve.SCALE_POWER2);
		problem.setScaling(mode);
		assertEquals(mode, problem.getScaling());
		assertTrue(problem.isScalemode(mode));
		assertTrue(problem.isScaletype(LpSolve.SCALE_EXTREME));
		assertFalse(problem.isIntegerscaling());
		problem.setScaling(mode | LpSolve.SCALE_INTEGERS);
		assertTrue(problem.isIntegerscaling());
		problem.unscale();
		problem.deleteLp();
	}

	public void testImprove() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(LpSolve.IMPROVE_DUALFEAS | LpSolve.IMPROVE_THETAGAP, problem.getImprove());
		problem.setImprove(LpSolve.IMPROVE_BBSIMPLEX );
		assertEquals(LpSolve.IMPROVE_BBSIMPLEX , problem.getImprove());
		problem.deleteLp();
	}

	public void testPivoting() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals((LpSolve.PRICER_DEVEX | LpSolve.PRICE_ADAPTIVE), problem.getPivoting());  // default
		assertTrue(problem.isPivRule(LpSolve.PRICER_DEVEX));	// default
		assertTrue(problem.isPivMode(LpSolve.PRICE_ADAPTIVE));	// default
		int mask = (LpSolve.PRICER_STEEPESTEDGE | LpSolve.PRICE_PRIMALFALLBACK);
		problem.setPivoting(mask);
		assertEquals(mask, problem.getPivoting());
		assertTrue(problem.isPivRule(LpSolve.PRICER_STEEPESTEDGE));
		assertFalse(problem.isPivRule(LpSolve.PRICER_DANTZIG));
		assertTrue(problem.isPivMode(LpSolve.PRICE_PRIMALFALLBACK));
		problem.deleteLp();
	}

	public void testNegrange() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(-1000000, problem.getNegrange(), 1e-8); // default
		problem.setNegrange(-1.234);
		assertEquals(-1.234, problem.getNegrange(), 1e-8);
		problem.deleteLp();
	}

	public void testBasis() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		int[] basis = new int[1+2];
		problem.getBasis(basis, false);

		int[] newBasis = { 0, -2, -3 };
		problem.setBasis(newBasis, false);
		problem.getBasis(basis, false);
		assertEquals(-2, basis[1]);
		assertEquals(-3, basis[2]);

		problem.defaultBasis();
		problem.getBasis(basis, false);
		assertEquals(-1, basis[1]);
		assertEquals(-2, basis[2]);

		problem.deleteLp();
	}

	public void testGetSensitivityRhs() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		problem.strSetObjFn("1 1");
		problem.strAddConstraint("2 4", LpSolve.GE, 10);
		problem.setLowbo(1, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		int size = problem.getNcolumns() + problem.getNrows();
		double[] duals = new double[size];
		double[] dualsfrom = new double[size];
		double[] dualstill = new double[size];
		problem.getSensitivityRhs(duals, dualsfrom, dualstill);
		double[][] sens = problem.getPtrSensitivityRhs();
		assertEquals(3, sens.length);
		double[] ptrDuals = sens[0];
		double[] ptrDualsfrom = sens[1];
		double[] ptrDualstill = sens[2];
		assertEquals(size, ptrDuals.length);
		assertEquals(size, ptrDualsfrom.length);
		assertEquals(size, ptrDualstill.length);
		for (int i = 0; i < size; i++) {
			assertEquals(duals[i], ptrDuals[i], 1e-8);
			assertEquals(dualsfrom[i], ptrDualsfrom[i], 1e-8);
			assertEquals(dualstill[i], ptrDualstill[i], 1e-8);
		}
		problem.deleteLp();
	}

	public void testGetDualSolution() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		problem.strSetObjFn("1 1");
		problem.strAddConstraint("2 4", LpSolve.GE, 10);
		problem.setLowbo(1, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		int size = 1 + problem.getNcolumns() + problem.getNrows();
		double[] duals = new double[size];
		problem.getDualSolution(duals);
		double[] ptrDuals = problem.getPtrDualSolution();
		assertEquals(size, ptrDuals.length);
		for (int i = 0; i < size; i++) {
			assertEquals(duals[i], ptrDuals[i], 1e-8);
		}
		problem.deleteLp();
	}

	public void testGetSensitivityObj() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		problem.strSetObjFn("1 1");
		problem.strAddConstraint("2 4", LpSolve.GE, 10);
		problem.setLowbo(1, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		int size = problem.getNcolumns();
		double[] objfrom = new double[size];
		double[] objtill = new double[size];
		problem.getSensitivityObj(objfrom, objtill);
		double[][] sens = problem.getPtrSensitivityObj();
		assertEquals(2, sens.length);
		double[] ptrObjfrom = sens[0];
		double[] ptrObjtill = sens[1];
		assertEquals(size, ptrObjfrom.length);
		assertEquals(size, ptrObjtill.length);
		assertEquals(0.5, objfrom[0], 1e-8);
		assertEquals(problem.getInfinite(), objtill[0], 1e-8);
		assertEquals(0.0, objfrom[1], 1e-8);
		assertEquals(2.0, objtill[1], 1e-8);
		for (int i = 0; i < size; i++) {
			assertEquals(objfrom[i], ptrObjfrom[i], 1e-8);
			assertEquals(objtill[i], ptrObjtill[i], 1e-8);
		}
		problem.deleteLp();
	}

	public void testGetSensitivityObjex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 2);
		problem.strSetObjFn("1 1");
		problem.strAddConstraint("2 4", LpSolve.GE, 10);
		problem.setLowbo(1, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		int size = problem.getNcolumns();
		double[] objfrom = new double[size];
		double[] objtill = new double[size];
		double[] objfromvalue = new double[size];
		double[] objtillvalue = new double[size];
		problem.getSensitivityObjex(objfrom, objtill, objfromvalue, objtillvalue);
		double[][] sens = problem.getPtrSensitivityObjex();
		assertEquals(4, sens.length);
		double[] ptrObjfrom = sens[0];
		double[] ptrObjtill = sens[1];
		double[] ptrObjfromvalue = sens[2];
		double[] ptrObjtillvalue = sens[3];
		assertEquals(size, ptrObjfrom.length);
		assertEquals(size, ptrObjtill.length);
		assertEquals(size, ptrObjfromvalue.length);
		assertEquals(size, ptrObjtillvalue.length);
		assertEquals(0.5, objfrom[0], 1e-8);
		assertEquals(problem.getInfinite(), objtill[0], 1e-8);
		assertEquals(0.0, objfrom[1], 1e-8);
		assertEquals(2.0, objtill[1], 1e-8);
		for (int i = 0; i < size; i++) {
			assertEquals(objfrom[i], ptrObjfrom[i], 1e-8);
			assertEquals(objtill[i], ptrObjtill[i], 1e-8);
			assertEquals(objfromvalue[i], ptrObjfromvalue[i], 1e-8);
			/* assertEquals(objtillvalue[i], ptrObjtillvalue[i], 1e-8); */ /* Not used/filled at this time, so should not be tested */
		}
		problem.deleteLp();
	}

	public void testIsConstrType() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		assertTrue(problem.isConstrType(1, LpSolve.LE));
		assertFalse(problem.isConstrType(1, LpSolve.EQ));
		assertFalse(problem.isConstrType(1, LpSolve.GE));
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		assertTrue(problem.isConstrType(2, LpSolve.GE));
		assertFalse(problem.isConstrType(2, LpSolve.LE));
		problem.deleteLp();
	}

	public void testGetSolutioncount() throws Exception {
		// not a real test but at least we make sure the
		// function exists and does not crash ..
		LpSolve problem = LpSolve.makeLp(0, 0);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		assertEquals(0, problem.getSolutioncount());
		problem.deleteLp();
	}

	public void testGetSolutionlimit() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		problem.setSolutionlimit(10);
		assertEquals(10, problem.getSolutionlimit());
		problem.deleteLp();
	}

	public void testGetTotalIter() throws Exception {
		// not a real test but at least we make sure the
		// function exists and does not crash ..
		LpSolve problem = LpSolve.makeLp(0, 0);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		assertEquals(0, problem.getTotalIter());
		assertEquals(0, problem.getTotalNodes());
		assertEquals(0, problem.getMaxLevel());
		problem.deleteLp();
	}

	public void testGetSetBoundsTighter() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 0);
		assertFalse(problem.getBoundsTighter()); 	// default
		problem.setBoundsTighter(true);
		assertTrue(problem.getBoundsTighter());
		problem.deleteLp();
	}

	public void testGetLpIndex() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 1);
		assertEquals(1, problem.getLpIndex(1));
		problem.deleteLp();
	}

	public void testIsFeasible() throws Exception {
		LpSolve problem = LpSolve.makeLp(1, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		double[] values = { 0.0, 1.0, 2.0 };
		assertTrue(problem.isFeasible(values, 0));
		problem.deleteLp();
	}

	public void testTimeElapsed() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 1);
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.timeElapsed();
		problem.deleteLp();
	}

	public void testGetNameindexWithRows() throws Exception {
		LpSolve problem = LpSolve.makeLp(2, 4);
		assertEquals(-1, problem.getNameindex("myrow1", true));
		problem.setRowName(1, "myrow1");
		assertEquals(1, problem.getNameindex("myrow1", true));
		assertEquals(-1, problem.getNameindex("myrow2", true));
		problem.setRowName(2, "myrow2");
		assertEquals(2, problem.getNameindex("myrow2", true));
		problem.deleteLp();
	}

	public void testGetNameindexWithCols() throws Exception {
		LpSolve problem = LpSolve.makeLp(2, 4);
		assertEquals(-1, problem.getNameindex("mycol1", false));
		problem.setColName(1, "mycol1");
		assertEquals(1, problem.getNameindex("mycol1", false));
		assertEquals(-1, problem.getNameindex("mycol2", false));
		problem.setColName(3, "mycol2");
		assertEquals(3, problem.getNameindex("mycol2", false));
		problem.deleteLp();
	}

	public void testReadNonexistentBasis() throws Exception {
		String filename = "model.bas";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		try {
			problem.readBasis(file.getAbsolutePath());
			fail("readBasis should throw an Exception on nonexistent file");
		}
		catch (LpSolveException e) {
			// OK
                        System.out.println("readBasis failed as expected");
		}
		problem.deleteLp();
	}

	public void testReadWriteBasis() throws Exception {
		String filename = "model.bas";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.writeBasis(file.getAbsolutePath());
		assertTrue(file.exists());

		String info = problem.readBasis(file.getAbsolutePath());
		assertNotNull(info);
		assertTrue(info.length() > 0);
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

	public void testReadWriteParams() throws Exception {
		String filename = "model.params";
		File file = new File(filename);
		if (file.exists()) file.delete();
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(250, problem.getMaxpivot());
		problem.setMaxpivot(400);
		problem.writeParams(file.getAbsolutePath(), "");
		assertTrue(file.exists());
		problem.deleteLp();

		problem = LpSolve.makeLp(0, 4);
		assertEquals(250, problem.getMaxpivot());
		problem.readParams(file.getAbsolutePath(), "");
		assertEquals(400, problem.getMaxpivot());
		problem.deleteLp();
		if (file.exists()) file.delete();
	}

	public void testResetParams() throws Exception {
		LpSolve problem = LpSolve.makeLp(0, 4);
		assertEquals(250, problem.getMaxpivot());
		problem.setMaxpivot(400);
		assertEquals(400, problem.getMaxpivot());
		problem.resetParams();
		assertEquals(250, problem.getMaxpivot());
		problem.deleteLp();
	}

	// TODO: write test case for setBasisvar after docs have been added

	// TODO: write test case for getConstrValue

	// TODO: write test case for put_bb_branchfunc

	// TODO: write test case for put_bb_nodefunc

	public void testAbortListener() throws Exception {
		final LpSolve problem = LpSolve.makeLp(0, 4);
		class MyListener implements AbortListener {
			public int numCalls = 0;
			public boolean abortfunc(LpSolve prob, Object handle) {
				numCalls++;
				assertEquals(problem, prob);
				assertEquals(new Integer(123), handle);
				return false;
			}
		};
		MyListener listener = new MyListener();
		problem.putAbortfunc(listener, new Integer(123));
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.deleteLp();
		assertTrue(listener.numCalls > 0);
	}

	public void testMsgListener() throws Exception {
		final LpSolve problem = LpSolve.makeLp(0, 4);
		class MyListener implements MsgListener {
			public int  numCalls = 0;
			public void msgfunc(LpSolve prob, Object handle, int code) throws LpSolveException {
				numCalls++;
				assertEquals(problem, prob);
				assertEquals(new Integer(123), handle);
				problem.getWorkingObjective();
			}
		};
		MyListener listener = new MyListener();
		int mask = LpSolve.MSG_LPFEASIBLE | LpSolve.MSG_LPOPTIMAL;
		problem.putMsgfunc(listener, new Integer(123), mask);
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.strSetObjFn("2 3 -2 3");
		problem.setVerbose(LpSolve.IMPORTANT);
		problem.solve();
		problem.deleteLp();
		assertTrue(listener.numCalls > 0);
	}

	public void testLogListener() throws Exception {
		final LpSolve problem = LpSolve.makeLp(0, 0);
		class MyListener implements LogListener {
			public int  numCalls = 0;
			public void logfunc(LpSolve prob, Object handle, String buf) {
				numCalls++;
				assertEquals(problem, prob);
				assertEquals(new Integer(123), handle);
				assertNotNull(buf);
			}
		};
		MyListener listener = new MyListener();
		problem.putLogfunc(listener, new Integer(123));
		try {
			problem.delColumn(1);	// should generate an error (see docs)
		}
		catch (LpSolveException e) {
                        // OK
                        System.out.println("delColumn failed as expected");
                }
		problem.deleteLp();
		assertTrue(listener.numCalls > 0);
	}

}