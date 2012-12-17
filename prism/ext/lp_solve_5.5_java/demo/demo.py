# ###########################################################################
# 
# Demo program that shows how to call lp_solve methods from a Python program
# via the Java wrapper. See README.html on how to run this programm with Jython.
#
# ###########################################################################

from lpsolve import *

#
# Define some callback methods
#
class MyAbortListener(AbortListener):
    def abortfunc(self, problem, handle):
        # print "In abortfunc, handle = ", handle
        return LpSolve.FALSE

class MyLogListener(LogListener):
    def logfunc(self, problem, handle, buf):
        print "logfunc:", buf

#
# some utility methods
#
def print_line():
	print '-----------------------------------------------'
	
def msg(text):
    print_line()
    print "\n***", text, "***\n"
    raw_input("Press RETURN to continue")


#
# here we start with the demo code
#
v = LpSolve.lpSolveVersion()
print 'lp_solve %d.%d.%d.%d demo (Python version)' % (v.majorversion, v.minorversion, v.release, v.build)
	
msg("We start by creating a new problem with 4 variables and 0 constraints")
problem = LpSolve.makeLp(0, 4)
problem.printLp()

msg("let's first demonstrate the logfunc callback feature ")
problem.putLogfunc(MyLogListener(), 1)
problem.solve()  # just to see that a message is send via the logfunc routine
problem.putLogfunc(None, 0) # ok, that is enough, no more callback

msg("Now we add a constraint with the string version of add_constraint")
problem.strAddConstraint("3 2 2 1", LpSolve.LE, 4)
problem.printLp()

msg("Add another constraint with the normal version")
problem.addConstraint([0, 0, 4, 3, 1], LpSolve.GE, 3)
problem.printLp()

# NOTE: the Jython docs say that Java arrays must be passed to methods
# as PyArray objects. This would look like this:
#
# from jarray import array
# problem.addConstraint(array([0, 0, 4, 3, 1]), "d"), LpSolve.GE, 3)
#
# But it seems to work also with normal Python sequences !?

msg("Set the objective function")
problem.strSetObjFn("2 3 -2 3")
problem.printLp()

msg("Now solve the problem")
rc = problem.solve();

if rc != 0:
    print "ERROR"; exit()
   
msg("The value is 0, this means we found an optimal solution; display solution")
problem.printObjective()
problem.printSolution(1)
problem.printConstraints(1)

msg("Print the dual variables of the solution")
problem.printDuals()

msg("Change a single element in the matrix and set to maximize")
problem.setMat(2, 1, 0.5)
problem.setMaxim()
problem.printLp()

msg("Now solve the problem")
problem.solve();
problem.printObjective()
problem.printSolution(1)
problem.printConstraints(1)
problem.printDuals()

msg("Change the value of a rhs element")
problem.setRh(1, 7.45);
problem.printLp()

problem.solve();
problem.printObjective()
problem.printSolution(1)
problem.printConstraints(1)

msg("We change column 4 to the integer type")
problem.setInt(4, LpSolve.TRUE)
problem.printLp()

msg("We set branch & bound debugging on and solve")
problem.setDebug(LpSolve.TRUE)
problem.solve();
problem.printObjective()
problem.printSolution(1)
problem.printConstraints(1)

msg("We can set bounds on the variables")
problem.setLowbo(2, 2)
problem.setUpbo(4, 5.3)
problem.printLp()

problem.solve();
problem.printObjective()
problem.printSolution(1)
problem.printConstraints(1)

msg("Now remove a constraint ")
problem.delConstraint(1);
problem.printLp()

msg("Add an equality constraint")
problem.strAddConstraint("1 2 1 4", LpSolve.EQ, 8)
problem.printLp()

msg("Add a column")
problem.strAddColumn("3 2 2")
problem.printLp()

msg("Delete a column")
problem.delColumn(3)
problem.printLp()

msg("Remove interger attributefrom  column 3")
problem.setInt(3, LpSolve.FALSE) # done in scaling stuff
problem.printLp()

# Scaling stuff from C demo program left out because in versions 4
# and 5 of lp_solve scaling is transparent and would not produce any 
# visual change in the output

msg("Return a single matrix element with getMat()")
print "problem.getMat(2, 3) = ", problem.getMat(2, 3)
print "problem.getMat(1, 1) = ", problem.getMat(1, 1)

msg("Now turn B&B debugging off and simplex tracing on and solve again")
problem.setDebug(LpSolve.FALSE)
problem.setTrace(LpSolve.TRUE)
problem.solve();

msg("Where possible, lp_solve will start at the last found basis.\n"
  "We reset the problem to the initial basis and solve again");
problem.resetBasis()
problem.solve();

msg("It is possible to give variables and constraints names")
problem.setRowName(1, "speed")
problem.setColName(2, "money")
problem.printLp()

msg("If a column or constraint is deleted, the names shift place also")
problem.delColumn(1);
problem.printLp()

