// Fix for the handling of strict/non-strict in simulation based bounded until fixed in revision 9163

dtmc 

module m1
  s : [0..4] init 0;

  [] s<4 -> 1 : (s'=s+1);
  [] s=4 -> 1 : (s'=s);

endmodule
