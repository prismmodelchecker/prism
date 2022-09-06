dtmc

module M

	s : [0..3];
	
	[] s=0 -> (s'=1);
	[] s=1 -> (s'=2);
	[] s=2 -> (s'=3);
	[] s>2 -> true;

endmodule

rewards "a"
	s=0: 2;
	[] s=0: 3;
	s=1: 5;
	[] s=1: 7;
	s=2: 11;
endrewards

rewards "a_state"
	s=0: 5;
	s=1: 12;
	s=2: 11;
endrewards

rewards "b"
	s=0: 2;
	[] s=0: 3;
	s=1: 5;
	[] s=1: 7;
	s=2: 11;
	[] s=3: 13;
	s=3: 17;
endrewards

rewards "b_state"
	s=0: 5;
	s=1: 12;
	s=2: 11;
	s=3: 30;
endrewards

