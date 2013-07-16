mdp

module mec_tests

	s : [0..1] init 0;
	
	[] s=0 -> 1.0 : (s'=1) ;
	[] s=1 -> 1.0 : (s'=0) ;
	
	
endmodule
