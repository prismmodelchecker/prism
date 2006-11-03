// herman's self stabilising algorithm [Her90]
// gxn/dxp 13/07/02

// the procotol is synchronous with no non-determinism (a DTMC)
dtmc

// module for process 1
module process1

	// Boolean variable for process 1
	x1 : [0..1];
	
	[step]  (x1=x17) -> 0.5 : (x1'=0) + 0.5 : (x1'=1);
	[step] !(x1=x17) -> (x1'=x17);
	
endmodule

// add further processes through renaming
module process2 = process1[x1=x2, x17=x1 ] endmodule
module process3 = process1[x1=x3, x17=x2 ] endmodule
module process4 = process1[x1=x4, x17=x3 ] endmodule
module process5 = process1[x1=x5, x17=x4 ] endmodule
module process6 = process1[x1=x6, x17=x5 ] endmodule
module process7 = process1[x1=x7, x17=x6 ] endmodule
module process8 = process1[x1=x8, x17=x7 ] endmodule
module process9 = process1[x1=x9, x17=x8 ] endmodule
module process10 = process1[x1=x10, x17=x9 ] endmodule
module process11 = process1[x1=x11, x17=x10 ] endmodule
module process12 = process1[x1=x12, x17=x11 ] endmodule
module process13 = process1[x1=x13, x17=x12 ] endmodule
module process14 = process1[x1=x14, x17=x13 ] endmodule
module process15 = process1[x1=x15, x17=x14 ] endmodule
module process16 = process1[x1=x16, x17=x15 ] endmodule
module process17 = process1[x1=x17, x17=x16 ] endmodule

// cost - 1 in each state (expected steps)
rewards
	
	true : 1;
	
endrewards

// any initial state (consider any possible initial configuration of tokens)
init
	
	true
	
endinit
