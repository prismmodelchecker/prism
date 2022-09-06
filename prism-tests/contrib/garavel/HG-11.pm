dtmc

const double a = 0.01;
const double b = 0.05;

// init 
// true 
// endinit

module Sender
x : [0..1] init 0; 
[] true -> 0.5 : (x'=0) + 0.5 : (x'=1);
endmodule

module Channel1
y : [0..1] init 0;
[] (x=0) -> a:(y'=1) + 1-a : (y'=0);
[] (x=1) -> b:(y'=0) + 1-b : (y'=1); 
endmodule

module Channel2
z : [0..1] init 0;
[] (y=0) -> a:(z'=1) + 1-a : (z'=0);
[] (y=1) -> b:(z'=0) + 1-b : (z'=1); 
endmodule


