#const N#
// Israeli-Jalfon self stabilising algorithm
// dxp/gxn 10/06/02

mdp

// variables to represent whether a process has a token or not
// note they are global because they can be updated by other processes
#for i=1:N#
global q#i#  : [0..1];
#end#

// module of process 1
module process1
	
	[] (q1=1) -> 0.5 : (q1'=0) & (q#N#'=1) + 0.5 : (q1'=0) & (q2'=1);
	
endmodule

// add further processes through renaming
#for i=2:N#
module process#i# = process1 [ q1=q#i#, q2=q#func(mod,i,N)+1#, q#N#=q#i-1# ] endmodule
#end#

// cost - 1 in each state (expected steps)
rewards "steps"
	true : 1;
endrewards

// formula, for use here and in properties: number of tokens
formula num_tokens = #+ i=1:N#q#i##end#;

// label - stable configurations (1 token)
label "stable" = num_tokens=1;

// initial states (at least one token)
init
	num_tokens >= 1
endinit

