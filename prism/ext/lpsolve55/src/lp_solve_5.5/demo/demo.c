/*
 This program is originally made by by Jeroen J. Dirks (jeroend@tor.numetrix.com)
 Adapted by Peter Notebaert (lpsolve@peno.be)
*/

#include <stdio.h>

#include "lp_lib.h"

#if defined FORTIFY
#include "lp_fortify.h"

int EndOfPgr(int i)
{
    exit(i);
}
#endif

void press_ret(void)
{
#if !defined NORETURN
  printf("[return]");
  getchar();
#endif
}

int main(void)
{
# if defined ERROR
#  undef ERROR
# endif
# define ERROR() { fprintf(stderr, "Error\n"); exit(1); }
  lprec *lp;
  int majorversion, minorversion, release, build;

#if defined FORTIFY
  Fortify_EnterScope();
#endif

  lp_solve_version(&majorversion, &minorversion, &release, &build);
  printf("lp_solve %d.%d.%d.%d demo\n\n", majorversion, minorversion, release, build);
  printf("This demo will show most of the features of lp_solve %d.%d.%d.%d\n", majorversion, minorversion, release, build);
  press_ret();
  printf("\nWe start by creating a new problem with 4 variables and 0 constraints\n");
  printf("We use: lp=make_lp(0,4);\n");
  if ((lp = make_lp(0,4)) == NULL)
    ERROR();
  press_ret();

  printf("We can show the current problem with print_lp(lp)\n");
  print_lp(lp);
  press_ret();
  printf("Now we add some constraints\n");
  printf("add_constraint(lp, {0, 3, 2, 2, 1}, LE, 4)\n");
  {
    double row[] = {0, 3, 2, 2, 1};
    if (!add_constraint(lp, row, LE, 4))
      ERROR();
  }
  print_lp(lp);
  press_ret();
  printf("add_constraintex is now used to add a row. Only the npn-zero values must be specfied with this call.\n");
  printf("add_constraintex(lp, 3, {4, 3, 1}, {2, 3, 4}, GE, 3)\n");
  {
    int colno[] = {2, 3, 4};
    double row[] = {4, 3, 1};
    if (!add_constraintex(lp, sizeof(colno) / sizeof(*colno), row, colno, GE, 3))
      ERROR();
  }
  print_lp(lp);
  press_ret();
  printf("Set the objective function\n");
  printf("set_obj_fn(lp, {0, 2, 3, -2, 3})\n");
  {
    double row[] = {0, 2, 3, -2, 3};
    if (!set_obj_fn(lp, row))
      ERROR();
  }
  print_lp(lp);
  press_ret();
  printf("Now solve the problem with printf(solve(lp));\n");
  printf("%d",solve(lp));
  press_ret();
  printf("The value is 0, this means we found an optimal solution\n");
  printf("We can display this solution with print_objective(lp) and print_solution(lp)\n");
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);

  press_ret();
  printf("The dual variables of the solution are printed with\n");
  printf("print_duals(lp);\n");
  print_duals(lp);
  press_ret();
  printf("We can change a single element in the matrix with\n");
  printf("set_mat(lp,2,1,0.5)\n");
  if (!set_mat(lp,2,1,0.5))
    ERROR();
  print_lp(lp);
  press_ret();
  printf("If we want to maximize the objective function use set_maxim(lp);\n");
  set_maxim(lp);
  print_lp(lp);
  press_ret();
  printf("after solving this gives us:\n");
  solve(lp);
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);
  print_duals(lp);
  press_ret();
  printf("Change the value of a rhs element with set_rh(lp,1,7.45)\n");
  set_rh(lp,1,7.45);
  print_lp(lp);
  solve(lp);
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);
  press_ret();
  printf("We change %s to the integer type with\n", get_col_name(lp, 4));
  printf("set_int(lp, 4, TRUE)\n");
  set_int(lp, 4, TRUE);
  print_lp(lp);
  printf("We set branch & bound debugging on with set_debug(lp, TRUE)\n");
  set_debug(lp, TRUE);
  printf("and solve...\n");
  press_ret();
  solve(lp);
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);
  press_ret();
  printf("We can set bounds on the variables with\n");
  printf("set_lowbo(lp,2,2); & set_upbo(lp,4,5.3)\n");
  set_lowbo(lp,2,2);
  set_upbo(lp,4,5.3);
  print_lp(lp);
  press_ret();
  solve(lp);
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);
  press_ret();
  printf("Now remove a constraint with del_constraint(lp, 1)\n");
  del_constraint(lp,1);
  print_lp(lp);
  printf("Add an equality constraint\n");
  {
    double row[] = {0, 1, 2, 1, 4};
    if (!add_constraint(lp, row, EQ, 8))
      ERROR();
  }
  print_lp(lp);
  press_ret();
  printf("A column can be added with:\n");
  printf("add_column(lp,{3, 2, 2});\n");
  {
    double col[] = {3, 2, 2};
    if (!add_column(lp, col))
      ERROR();
  }
  print_lp(lp);
  press_ret();
  printf("A column can be removed with:\n");
  printf("del_column(lp,3);\n");
  del_column(lp,3);
  print_lp(lp);
  press_ret();
  printf("We can use automatic scaling with:\n");
  printf("set_scaling(lp, SCALE_MEAN);\n");
  set_scaling(lp, SCALE_MEAN);
  print_lp(lp);
  press_ret();
  printf("The function get_mat(lprec *lp, int row, int column) returns a single\n");
  printf("matrix element\n");
  printf("%s get_mat(lp,2,3), get_mat(lp,1,1); gives\n","printf(\"%f %f\\n\",");
  printf("%f %f\n", (double)get_mat(lp,2,3), (double)get_mat(lp,1,1));
  printf("Notice that get_mat returns the value of the original unscaled problem\n");
  press_ret();
  printf("If there are any integer type variables, then only the rows are scaled\n");
  printf("set_scaling(lp, SCALE_MEAN);\n");
  set_scaling(lp, SCALE_MEAN);
  printf("set_int(lp,3,FALSE);\n");
  set_int(lp,3,FALSE);
  print_lp(lp);
  press_ret();
  solve(lp);
  printf("print_objective, print_solution gives the solution to the original problem\n");
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);
  press_ret();
  printf("Scaling is turned off with unscale(lp);\n");
  unscale(lp);
  print_lp(lp);
  press_ret();
  printf("Now turn B&B debugging off and simplex tracing on with\n");
  printf("set_debug(lp, FALSE), set_trace(lp, TRUE) and solve(lp)\n");
  set_debug(lp, FALSE);
  set_trace(lp, TRUE);
  press_ret();
  solve(lp);
  printf("Where possible, lp_solve will start at the last found basis\n");
  printf("We can reset the problem to the initial basis with\n");
  printf("default_basis(lp). Now solve it again...\n");
  press_ret();
  default_basis(lp);
  solve(lp);

  printf("It is possible to give variables and constraints names\n");
  printf("set_row_name(lp,1,\"speed\"); & set_col_name(lp,2,\"money\")\n");
  if (!set_row_name(lp,1,"speed"))
    ERROR();
  if (!set_col_name(lp,2,"money"))
    ERROR();
  print_lp(lp);
  printf("As you can see, all column and rows are assigned default names\n");
  printf("If a column or constraint is deleted, the names shift place also:\n");
  press_ret();
  printf("del_column(lp,1);\n");
  del_column(lp,1);
  print_lp(lp);
  press_ret();

  write_lp(lp, "lp.lp");

  delete_lp(lp);

  printf("An lp structure can be created and read from a .lp file\n");
  printf("lp = read_lp(\"lp.lp\", TRUE);\n");
  printf("The verbose option is used\n");
  if ((lp = read_LP("lp.lp", TRUE, "test")) == NULL)
    ERROR();
  press_ret();
  printf("lp is now:\n");
  print_lp(lp);

  press_ret();
  printf("solution:\n");
  set_debug(lp, TRUE);
  solve(lp);
  set_debug(lp, FALSE);
  print_objective(lp);
  print_solution(lp, 1);
  print_constraints(lp, 1);
  press_ret();

  delete_lp(lp);

#if defined FORTIFY
  Fortify_LeaveScope();
#endif

  return(0);
}
