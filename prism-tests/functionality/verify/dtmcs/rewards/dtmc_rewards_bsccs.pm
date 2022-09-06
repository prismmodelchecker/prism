dtmc

module M

	s : [0..4];
	
	[] s=0 -> 0.4:(s'=1) + 0.6:(s'=3);
	[] s=1 -> (s'=2);
	[] s=2 -> (s'=1);
	[] s=3 -> (s'=4);
	[] s=4 -> (s'=3);

endmodule

rewards "a"
	s=0: 2;
endrewards

rewards "b"
	s=0: 2;
	s=1: 5;
endrewards

rewards "c"
	s=0: 2;
	[] s=3: 5;
endrewards
