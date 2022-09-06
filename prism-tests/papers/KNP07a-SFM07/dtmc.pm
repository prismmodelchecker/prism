dtmc

module M

	x : [0..3];
	
	[] x=0 -> (x'=1);
	[] x=1 -> 0.01 : (x'=1) + 0.01 : (x'=2) + 0.98 : (x'=3);
	[] x=2 -> (x'=0);
	[] x=3 -> (x'=3);

endmodule

label "try" = x=1;
label "fail" = x=2;
label "succ" = x=3;

rewards
	x=1 : 1;
endrewards
