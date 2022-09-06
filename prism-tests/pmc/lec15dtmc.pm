// Simple DTMC from Lec 15 of Probabilistic Model Checking

dtmc

module M

s:[0..5];

[] s=0 -> 0.5:(s'=1) + 0.5:(s'=3);
[] s=1 -> 0.5:(s'=0) + 0.25:(s'=2) + 0.25:(s'=4);
[] s=2 -> 1:(s'=5);
[] s=3 -> 1:(s'=3);
[] s=4 -> 1:(s'=4);
[] s=5 -> 1:(s'=2);

endmodule
