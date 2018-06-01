// simple sanity test case for DTMC steady state computations (2 BSCCs)

dtmc

module m1
  s: [0..2] init 0;

  [] s=0 -> 1/4:(s'=0) + 1/2:(s'=1) + 1/4:(s'=2);
  [] s>0 -> true;
endmodule

module m2
  t: [0..2] init 0;

  [] true -> 1/2:(t'=0) + 1/4:(t'=1) + 1/4:(t'=2);
endmodule
