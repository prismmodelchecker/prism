dtmc

module chan1
	s : [0..1];
	[] s=0 -> 0.99 : (s'=0) + 0.01 : (s'=1);
	[] s=1 -> 0.05 : (s'=0) + 0.95 : (s'=1);
endmodule
