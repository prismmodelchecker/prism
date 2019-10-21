#const N#
// COIN FLIPPING PROTOCOL FOR POLYNOMIAL RANDOMIZED CONSENSUS [AH90] 
// gxn/dxp 20/11/00

mdp

// constants
const int N=#N#;
const int K;
const int range = 2*(K+1)*N;
const int counter_init = (K+1)*N;
const int left = N;
const int right = 2*(K+1)*N - N;

// shared coin
global counter : [0..range] init counter_init;

module process1
	
	// program counter
	pc1 : [0..3];
	// 0 - flip
	// 1 - write 
	// 2 - check
	// 3 - finished
	
	// local coin
	coin1 : [0..1];	

	// flip coin
	[] (pc1=0)  -> 0.5 : (coin1'=0) & (pc1'=1) + 0.5 : (coin1'=1) & (pc1'=1);
	// write tails -1  (reset coin to add regularity)
	[] (pc1=1) & (coin1=0) & (counter>0) -> (counter'=counter-1) & (pc1'=2) & (coin1'=0);
	// write heads +1 (reset coin to add regularity)
	[] (pc1=1) & (coin1=1) & (counter<range) -> (counter'=counter+1) & (pc1'=2) & (coin1'=0);
	// check
	// decide tails
	[] (pc1=2) & (counter<=left) -> (pc1'=3) & (coin1'=0);
	// decide heads
	[] (pc1=2) & (counter>=right) -> (pc1'=3) & (coin1'=1);
	// flip again
	[] (pc1=2) & (counter>left) & (counter<right) -> (pc1'=0);
	// loop (all loop together when done)
	[done] (pc1=3) -> (pc1'=3);

endmodule

// construct remaining processes through renaming
#for i=2:N#
module process#i# = process1[pc1=pc#i#,coin1=coin#i#] endmodule
#end#

// labels
label "finished" =#& i=1:N# pc#i#=3 #end#;
label "all_coins_equal_0" =#& i=1:N# coin#i#=0 #end#;
label "all_coins_equal_1" =#& i=1:N# coin#i#=1 #end#;
label "agree" =#& i=1:N-1# coin#i#=coin#i+1# #end#;

// rewards
rewards "steps"
	true : 1;
endrewards

