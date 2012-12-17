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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import lpsolve.*;

/**
 * Java version of the C Demo program that comes with the lp_solve software.
 * 
 * @author Juergen Ebert
 */
public class Demo {

	public Demo() {
	}

	public void execute() throws LpSolveException {
		VersionInfo info = LpSolve.lpSolveVersion();
		System.out.println("This demo (Java version) will show most of the features of lp_solve " 
			+ info.getMajorversion()
			+ "." + info.getMinorversion()
			+ "." + info.getRelease()
			+ "." + info.getBuild());
		pressRet();
		
		System.out.println("We start by creating a new problem with 4 variables "
			+ "and 0 constraints");
		System.out.println("We use: LpSolve problem = LpSolve.makeLp(0, 4);");
		LpSolve problem = LpSolve.makeLp(0, 4);
		pressRet();
		
		/* Let's first demonstrate the logfunc callback feature */
	    LogListener logfunc = new LogListener() {
	        public void logfunc(LpSolve problem, Object handle, String buf) {
	        	System.out.println(buf);
	        }
	    };
	    problem.putLogfunc(logfunc, null);
	    problem.solve(); /* just to see that a message is send via the logfunc routine ... */
	    problem.putLogfunc(null, null); /* ok, that is enough, no more callback */

	    /* Set an abort function. This is optional */
	    AbortListener abortfunc = new AbortListener() {
	        public boolean abortfunc(LpSolve problem, Object handle) {
	        	/* If set to true, then solve is aborted and returncode will indicate this. */
	        	return false;
	        }
	    };
	    problem.putAbortfunc(abortfunc, null);

	    /* Set a message function. Again optional */
	    MsgListener msgfunc = new MsgListener() {
	        public void msgfunc(LpSolve problem, Object handle, int msg) {
	        	// System.out.println("Message = " + msg);
	        }
	    };
	    problem.putMsgfunc(msgfunc, null, 
	    		LpSolve.MSG_PRESOLVE | LpSolve.MSG_LPFEASIBLE | LpSolve.MSG_LPOPTIMAL 
				| LpSolve.MSG_MILPEQUAL | LpSolve.MSG_MILPFEASIBLE | LpSolve.MSG_MILPBETTER);

	    System.out.println("We can show the current problem with problem.printLp()");
		problem.printLp();
		pressRet();

		System.out.println("Now we add some constraints:");
		System.out.println("problem.strAddConstraint(\"3 2 2 1\", LpSolve.LE, 4);");
		problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4);
		problem.printLp();
		pressRet();
		
		System.out.println("problem.strAddConstraint(\"0 4 3 1\", LpSolve.GE, 3);");
		problem.strAddConstraint("0 4 3 1", LpSolve.GE, 3);
		problem.printLp();
		pressRet();
			
		System.out.println("Set the objective function:");
		System.out.println("problem.strSetObjFn(\"2 3 -2 3\");");
		problem.strSetObjFn("2 3 -2 3");
		problem.printLp();
		pressRet();
		
		System.out.println("Now solve the problem with System.out.println(problem.solve());");
		System.out.println(problem.solve());
		pressRet();
		
		System.out.println("The value is 0, this means we found an optimal solution.");
		System.out.println("We can display the solution with problem.printObjective(), "
			+ "problem.printSolution(1), and problem.printConstraints(1)");		
		problem.printObjective();
		problem.printSolution(1);
		problem.printConstraints(1);
		pressRet();
		
		System.out.println("The dual variables of the solution are printed with "
			+ "problem.printDuals()");
		problem.printDuals();
		pressRet();
				
		System.out.println("We can change a single element in the matix with "
			+ "problem.setMat(2, 1, 0.5)");
		problem.setMat(2, 1, 0.5);
		problem.printLp();
		pressRet();
		
		System.out.println("If we want to maximize the objective function use "
			+ "problem.setMaxim()");
		problem.setMaxim();		
		problem.printLp();
		pressRet();
				
		System.out.println("After solving this gives us:");
		problem.solve();
		problem.printObjective();
		problem.printSolution(1);
		problem.printConstraints(1);
		problem.printDuals();
		pressRet();
		
		System.out.println("Change the value of a rhs element with problem.setRh(1, 7.45)");
		problem.setRh(1, 7.45);
		problem.printLp();
		problem.solve();
		problem.printObjective();
		problem.printSolution(1);
		problem.printConstraints(1);
		pressRet();
	
		System.out.println("We change " + problem.getColName(4) 
			+ " to the integer type with problem.setInt(4, true)");
		problem.setInt(4, true);
		problem.printLp();
		System.out.println("We set branch & bound debugging on with problem.setDebug(true)");
		problem.setDebug(true);
		System.out.println("and solve...");
		pressRet();
		
		problem.solve();
		problem.printObjective();
		problem.printSolution(1);
		problem.printConstraints(1);
		pressRet();
	
		System.out.println("We can set bounds on the variables with "
			+ "problem.setLowbo(2, 2) & problem.setUpbo(4, 5.3)");
		problem.setLowbo(2, 2);
		problem.setUpbo(4, 5.3);
		problem.printLp();
		pressRet();
	
		problem.solve();
		problem.printObjective();
		problem.printSolution(1);
		problem.printConstraints(1);
		pressRet();
	
		System.out.println("Now remove a constraint with problem.delConstraint(1)");
		problem.delConstraint(1);
		problem.printLp();
		pressRet();

		System.out.println("Add an equality constraint:\n" 
			+ "problem.strAddConstraint(\"1 2 1 4\", LpSolve.EQ, 8)");
		problem.strAddConstraint("1 2 1 4", LpSolve.EQ, 8);
		problem.printLp();
		pressRet();
		
		System.out.println("A column can be added with problem.strAddColumn(\"3 2 2\")");
		problem.strAddColumn("3 2 2");
		problem.printLp();
		pressRet();
	
		System.out.println("A column can be removed with problem.delColumn(3)");
		problem.delColumn(3);
		problem.printLp();
		pressRet();
		
		System.out.println("We can use automatic scaling with problem.setScaling(LpSolve.SCALE_MEAN)");
		problem.setScaling(LpSolve.SCALE_MEAN);
		problem.printLp();
		pressRet();
	
		System.out.println("The function matElm returns a single matrix element.");
		System.out.println("problem.getMat(2, 3) returns " + problem.getMat(2, 3));
		System.out.println("problem.getMat(1, 1) returns " + problem.getMat(1, 1));
		System.out.println("Notice that getMat returns the value of the "
			+ "original unscaled problem");
		pressRet();

		System.out.println("If there are any integer type variables, then only "
			+ " the rows are scaled.");
		System.out.println("problem.setScaling(LpSolve.SCALE_MEAN);");		
		problem.setScaling(LpSolve.SCALE_MEAN);
		System.out.println("problem.setInt(3, false);");		
		problem.setInt(3, false);
		problem.printLp();
		pressRet();
		
		System.out.println("printObjective, printSolution gives the solution to the "
			+ "original problem");
		problem.solve();
		problem.printObjective();
		problem.printSolution(1);
		problem.printConstraints(1);
		pressRet();

		System.out.println("Scaling is turned off with problem.unscale()");
		problem.unscale();
		problem.printLp();
		pressRet();

		System.out.println("Now turn B&B debugging off and simplex tracing on with");
		System.out.println("problem.setDebug(false), problem.setTrace(true) and solve.");
		problem.setDebug(false);
		problem.setTrace(true);
		pressRet();

		problem.solve();
		System.out.println("Where possible, lp_solve will start at the last found basis.");
		System.out.println("We can reset the problem to the initial basis with");
		System.out.println("problem.resetBasis(). Now solve it again ...");
		pressRet();

		problem.resetBasis();
		problem.solve();
		System.out.println("It is possible to give variables and constraints names.");
		System.out.println("problem.setRowName(1, \"speed\") & problem.setColName(2, \"money\")");
		problem.setRowName(1, "speed");
		problem.setColName(2, "money");
		problem.printLp();
		System.out.println("As you can see, all column and rows are assigned default names");
		System.out.println("If a column or constraint is deleted, the names shift place also:");
		pressRet();
		
		System.out.println("problem.delColumn(1)");
		problem.delColumn(1);
		problem.printLp();
		pressRet();
		
		// delete the problem 
		problem.deleteLp();
	}

	private void pressRet() {
		System.out.print("\n[Press return to continue or type 'q' to quit] ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			String line = br.readLine();
			if (line == null || "q".equals(line.trim())) {
				System.out.println("Demo terminated.");
				System.exit(0);
			}
		} 
		catch (IOException e) {
			System.exit(0);
		}
		System.out.println("");
	}

	public static void main(String[] args) {
		try {
			new Demo().execute();
		} 
		catch (LpSolveException e) {
			e.printStackTrace();
		}
	}
}
