mdp

module mec_tests

	s : [0..2] init 0;
	
	[] s=0 -> 1.0 : (s'=1);
	[] s=1 -> 1.0 : (s'=0);
	[] s=0 -> 1.0 : (s'=2);
	[] s=2 -> 1.0 : (s'=2);
	
endmodule
