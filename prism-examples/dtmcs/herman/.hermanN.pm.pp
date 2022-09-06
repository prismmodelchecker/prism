#const N#
// herman's self stabilising algorithm [Her90]
// gxn/dxp 13/07/02

// the procotol is synchronous with no nondeterminism (a DTMC)
dtmc

const double p = 0.5;

// module for process 1
module process1

	// Boolean variable for process 1
	x1 : [0..1];
	
	[step]  (x1=x#N#) -> p : (x1'=0) + 1-p : (x1'=1);
	[step] !(x1=x#N#) -> (x1'=x#N#);
	
endmodule

// add further processes through renaming
#for i=2:N#
module process#i# = process1 [ x1=x#i#, x#N#=x#i-1# ] endmodule
#end#

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
formula num_tokens = #+ i=1:N#(x#i#=x#func(mod, i, N)+1#?1:0)#end#;

// label - stable configurations (1 token)
label "stable" = num_tokens=1;

