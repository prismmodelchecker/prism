// simple test case for DTMC steady state computation (exhibits non-convergence using "naive" iteration)

dtmc

module m
  s: [0..2] init 0;

  [] s=0 -> 1/2:(s'=1) + 1/2:(s'=2);
  [] s>0 -> (s'=0);
endmodule
