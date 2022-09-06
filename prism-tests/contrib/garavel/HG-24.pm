dtmc

module Sender
x : [0..1] init 0; 
[] x = 0 -> 1 : (x'=1);
[] x = 1 -> 1 : (x'=0);
endmodule

