// Simple DTMC from Lec 17 of Probabilistic Model Checking

dtmc

module M

s:[0..5];

[] s=0 -> 0.6:(s'=0) + 0.1:(s'=1) + 0.3:(s'=3);
[] s=1 -> 0.2:(s'=3) + 0.3:(s'=4) + 0.5:(s'=2);
[] s=2 -> 1:(s'=5);
[] s=3 -> 1:(s'=4);
[] s=4 -> 1:(s'=3);
[] s=5 -> 0.1:(s'=2) + 0.9:(s'=5);

endmodule

label "a" = s=2 | s=3 | s=5;
label "b" = s=1;
