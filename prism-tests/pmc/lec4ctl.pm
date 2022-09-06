// 3-state DTMC from Lec 4 of Probabilistic Model Checking

dtmc

module M

s:[0..2];

[] s=0 -> 0.5:(s'=1) + 0.5:(s'=2);
[] s=1 -> (s'=0);
[] s=2 -> true;

endmodule

label "heads" = s=1;
label "tails" = s=2;
