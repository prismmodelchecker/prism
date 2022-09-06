dtmc

const double a = 0.23;

const double b = 0.41;

// init 
// true 
// endinit

global x : [0..1] init 0; // init 0;
global y : [0..1] init 0;
global z : [0..1] init 0;

module Sender
[] true -> 0.5 : (x'=0) + 0.5 : (x'=1);
endmodule

// module Sender
// [] true -> 0.5 : (y'=0) + 0.5 : (y'=1);
// endmodule

module Channel1
s : [0..1] init 0;
[] (s=0) & (x=0) -> a:(s'=1)&(y'=1) + 1-a : (s'=0)&(y'=0);
[] (s=1) & (x=1) -> b:(s'=0)&(y'=0) + 1-b : (s'=1)&(y'=1); 
endmodule

module Channel2
t : [0..1] init 0;
[] (t=0) & (y=0) -> a:(t'=1)&(z'=1) + 1-a : (t'=0)&(z'=0);
[] (t=1) & (y=1) -> b:(t'=0)&(z'=0) + 1-b : (t'=1)&(z'=1); 
endmodule


