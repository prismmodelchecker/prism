dtmc

module Sender
x : [0..1] init 0; 
[] true -> 0.5 : (x'=0) + 0.5 : (x'=1);
endmodule

