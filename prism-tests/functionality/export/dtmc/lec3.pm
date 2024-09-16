dtmc

module M

s:[0..5];

[] s=0 -> 0.5:(s'=1) + 0.5:(s'=3);
[] s=1 -> 0.5:(s'=0) + 0.25:(s'=2) + 0.25:(s'=4);
[] s=2 -> 1:(s'=5);
[] s=3 -> 1:(s'=3);
[] s=4 -> 1:(s'=4);
[] s=5 -> 1:(s'=2);

endmodule

rewards "r"
	s=0 : 2;
	[] s<3 : s;
endrewards

rewards "s"
	s>=4 : 1;
endrewards

rewards
	[] s<3 : s;
endrewards
