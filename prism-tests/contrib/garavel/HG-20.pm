dtmc

module Generator
	// local state
	x : [0..1] init 0;	
	[] x=0 -> 0.5 : (x'=1) + 0.5 : (x'=0);
	[] x=1 -> 0.5 : (x'=1) + 0.5 : (x'=0);
endmodule


module Line1
	// local state
	y : [0..1];
	//[] x=0 -> 1 : (y'=0);
	//[] x=1 -> 1 : (y'=1);
	[] x=0 -> 0.01 : (y'=1) + 0.99 : (y'=0);
	[] x=1 -> 0.05 : (y'=0) + 0.95 : (y'=1);
endmodule



module Line2
	// local state
	z : [0..1];
	//[] y=0 -> 1 : (z'=0);
	//[] y=1 -> 1 : (z'=1);
	[] y=0 -> 0.01 : (z'=1) + 0.99 : (z'=0);
	[] y=1 -> 0.05 : (z'=0) + 0.95 : (z'=1);
endmodule
