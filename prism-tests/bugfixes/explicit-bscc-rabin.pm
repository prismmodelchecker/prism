// Bug in Rabin check for explicit engine
// Reported by Joachim Klein
// Fixed in rev 9501

dtmc

module M1
  s : [0..1] init 0;

  [] s=0 -> 1:(s'=1);
  [] s=1 -> 1:(s'=1);
endmodule

