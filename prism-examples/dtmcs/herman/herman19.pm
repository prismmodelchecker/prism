// herman's self stabilising algorithm [Her90]
// gxn/dxp 13/07/02

// the procotol is synchronous with no nondeterminism (a DTMC)
dtmc

const double p = 0.5;

// module for process 1
module process1

	// Boolean variable for process 1
	x1 : [0..1];
	
	[step]  (x1=x19) -> p : (x1'=0) + 1-p : (x1'=1);
	[step] !(x1=x19) -> (x1'=x19);
	
endmodule

// add further processes through renaming
module process2 = process1 [ x1=x2, x19=x1 ] endmodule
module process3 = process1 [ x1=x3, x19=x2 ] endmodule
module process4 = process1 [ x1=x4, x19=x3 ] endmodule
module process5 = process1 [ x1=x5, x19=x4 ] endmodule
module process6 = process1 [ x1=x6, x19=x5 ] endmodule
module process7 = process1 [ x1=x7, x19=x6 ] endmodule
module process8 = process1 [ x1=x8, x19=x7 ] endmodule
module process9 = process1 [ x1=x9, x19=x8 ] endmodule
module process10 = process1 [ x1=x10, x19=x9 ] endmodule
module process11 = process1 [ x1=x11, x19=x10 ] endmodule
module process12 = process1 [ x1=x12, x19=x11 ] endmodule
module process13 = process1 [ x1=x13, x19=x12 ] endmodule
module process14 = process1 [ x1=x14, x19=x13 ] endmodule
module process15 = process1 [ x1=x15, x19=x14 ] endmodule
module process16 = process1 [ x1=x16, x19=x15 ] endmodule
module process17 = process1 [ x1=x17, x19=x16 ] endmodule
module process18 = process1 [ x1=x18, x19=x17 ] endmodule
module process19 = process1 [ x1=x19, x19=x18 ] endmodule

// cost - 1 in each state (expected number of steps)
rewards "steps"
	true : 1;
endrewards

// set of initial states: all (i.e. any possible initial configuration of tokens)
init
	true
endinit

// formula, for use in properties: number of tokens
// (i.e. number of processes that have the same value as the process to their left)
formula num_tokens = (x1=x2?1:0)+(x2=x3?1:0)+(x3=x4?1:0)+(x4=x5?1:0)+(x5=x6?1:0)+(x6=x7?1:0)+(x7=x8?1:0)+(x8=x9?1:0)+(x9=x10?1:0)+(x10=x11?1:0)+(x11=x12?1:0)+(x12=x13?1:0)+(x13=x14?1:0)+(x14=x15?1:0)+(x15=x16?1:0)+(x16=x17?1:0)+(x17=x18?1:0)+(x18=x19?1:0)+(x19=x1?1:0);

// label - stable configurations (1 token)
label "stable" = num_tokens=1;

