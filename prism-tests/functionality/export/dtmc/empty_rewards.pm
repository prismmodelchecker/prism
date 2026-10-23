// Simple DTMC with an empty reward structure
dtmc

module m
	s : [0..1] init 0;
	[] s=0 -> 1/3 : (s'=1) + 2/3 : (s'=0);
endmodule

rewards "none"
endrewards

rewards "r"
	s=0 : 1/2;
endrewards
