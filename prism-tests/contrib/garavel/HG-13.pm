dtmc

module GENERATOR
	x : [0..1] init 0;
	[] x=0 -> 0.5 : (x'=0) + 0.5 : (x'=1);
	[] x=1 -> 0.5 : (x'=0) + 0.5 : (x'=1);
endmodule

module LINE1
	y : [0..1];
	[] x=0 -> 0.99 : (y'=0) + 0.01 : (y'=1);
	[] x=1 -> 0.05 : (y'=0) + 0.95 : (y'=1);
endmodule

module LINE2
	z : [0..1];
	[] y=0 -> (z'=0);
	[] y=1 -> (z'=1);
endmodule