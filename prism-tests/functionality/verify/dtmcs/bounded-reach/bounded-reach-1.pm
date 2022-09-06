// Simple linear DTMC for checking bounds in bounded until, etc

dtmc

module m1
  s : [0..4] init 0;

  [] s<4 -> 1 : (s'=s+1);
  [] s=4 -> 1 : (s'=s);

endmodule
