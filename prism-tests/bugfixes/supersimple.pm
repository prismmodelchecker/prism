// A very very simple model, that can be used with multiple different tests
// that do not depend on too much model structure

dtmc

module M

	s : [0..3] init 0;
	
	[] s=0 -> 0.6 : (s'=1) + 0.4 : (s'=2);
	[] s>0 -> true;
	
endmodule

rewards
	true : 1;
endrewards

label "a" = s=1;
label "b" = s=2;
