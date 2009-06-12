// herman's self stabilising algorithm [Her90]
// gxn/dxp 13/07/02

// the procotol is synchronous with no nondeterminism (a DTMC)
dtmc

const double p = 0.5;

// module for process 1
module process1

	// Boolean variable for process 1
	x1 : [0..1];
	
	[step]  (x1=x3) -> p : (x1'=0) + 1-p : (x1'=1);
	[step] !(x1=x3) -> (x1'=x3);
	
endmodule

// add further processes through renaming
module process2 = process1 [ x1=x2, x3=x1 ] endmodule
module process3 = process1 [ x1=x3, x3=x2 ] endmodule

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
formula num_tokens = (x1=x2?1:0)+(x2=x3?1:0)+(x3=x1?1:0);

// label - stable configurations (1 token)
label "stable" = num_tokens=1;

