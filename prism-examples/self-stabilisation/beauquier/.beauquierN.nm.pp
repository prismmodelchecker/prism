#const N#
// self stabilisation algorithm Beauquier, Gradinariu and Johnen
// gxn/dxp 18/07/02

mdp

// module of process 1
module process1
	
	d1 : bool; // probabilistic variable
	p1 : bool; // deterministic variable
	
	[] d1=d#N# &  p1=p#N# -> 0.5 : (d1'=!d1) & (p1'=p1) + 0.5 : (d1'=!d1) & (p1'=!p1);
	[] d1=d#N# & !p1=p#N# -> (d1'=!d1);
	
endmodule

// add further processes through renaming
#for i=2:N#
module process#i# = process1 [ p1=p#i#, p#N#=p#i-1#, d1=d#i#, d#N#=d#i-1# ] endmodule
#end#

// cost - 1 in each state (expected steps)
rewards "steps"
	true : 1;
endrewards

// initial states - any state with more than 1 token, that is all states
init
	true
endinit

// formula, for use in properties: number of tokens
formula num_tokens = #+ i=1:N#(p#i#=p#func(mod, i, N)+1#?1:0)#end#;

