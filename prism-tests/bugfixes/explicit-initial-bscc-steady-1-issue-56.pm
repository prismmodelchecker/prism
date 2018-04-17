// test case for github issue #56
// all states are initial and are in their own BSCC,
// so "all initial states in the same BSCC" does not apply

dtmc

init true endinit

module m
    s: [0..1];
  [] true -> true;
endmodule
