// herman's self stabilising algorithm [Her90]
// gxn/dxp 13/07/02

// the procotol is synchronous with no non-determinism (a DTMC)
probabilistic

// module for process 1
module process1

	// bits in the ring (initially all the same i.e. a token in every place)
	x1 : [0..1];
	
	[step] (x1=x11) -> 0.5 : (x1'=0) + 0.5 : (x1'=1);
	[step] !x1=x11 -> (x1'=x11);
	
endmodule

// add further processes through renaming
module process2  = process1[x1=x2,  x11=x1 ] endmodule
module process3  = process1[x1=x3,  x11=x2 ] endmodule
module process4  = process1[x1=x4,  x11=x3 ] endmodule
module process5  = process1[x1=x5,  x11=x4 ] endmodule
module process6  = process1[x1=x6,  x11=x5 ] endmodule
module process7  = process1[x1=x7,  x11=x6 ] endmodule
module process8  = process1[x1=x8,  x11=x7 ] endmodule
module process9  = process1[x1=x9,  x11=x8 ] endmodule
module process10 = process1[x1=x10, x11=x9 ] endmodule
module process11 = process1[x1=x11, x11=x10 ] endmodule

// cost - 1 in each state (expected steps)
rewards
	
	true : 1;
	
endrewards

// initial states (at least one token i.e. all states)
init
	
	true
	
endinit
