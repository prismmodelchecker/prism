#const N#
#const K=4+func(ceil,func(log,N,2))#
// N-processor mutual exclusion [Rab82]
// with global variables to remove sychronization 
// gxn/dxp 02/02/00

nondeterministic

// size of shared counter
const int K = #K#; // 4+ceil(log_2 N)

// global variables (all modules can read and write)
// c=0 critical section free
// c=1 critical section not free
// b current highest draw
// r current round 

global c : [0..1];
global b : [0..K];
global r : [1..2];

// local variables: process i 
// state: pi
//  0 remainder
//  1 trying
//  2 critical section
// current draw: bi
// current round: ri

// atomic formula for process 1 drawing
formula draw = p1=1 & (b<b1 | r!=r1);

module process1

	p1 : [0..2];
	b1 : [0..K];
	r1 : [0..2];

	// remain in remainder
	[] p1=0 -> (p1'=0);
	// enter trying
	[] p1=0 -> (p1'=1);
	// make a draw
	[] draw ->#+ i=1:K-1# #1/func(floor,func(pow,2,i))# : (b1'=#i#) & (r1'=r) & (b'=max(b,#i#))
	         #end#+ #1/func(floor,func(pow,2,K-1))# : (b1'=#K#) & (r1'=r) & (b'=max(b,#K#));
	// enter critical section and randomly set r to 1 or 2
	[] p1=1 & b=b1 & r=r1 & c=0 -> 0.5 : (r'=1) & (c'=1) & (b'=0) & (b1'=0) & (r1'=0) & (p1'=2)
	                             + 0.5 : (r'=2) & (c'=1) & (b'=0) & (b1'=0) & (r1'=0) & (p1'=2);
	// loop when trying and cannot make a draw or enter critical section
	[] p1=1 & r1=r & b1<=b & ((c=0 & b1!=b) | c=1) -> (p1'=p1);
	// leave crictical section
	[] p1=2 -> (p1'=0) & (c'=0);
	// stay in critical section
	[] p1=2 -> (p1'=2);
	
endmodule

// construct further modules through renaming
#for i=2:N#
module process#i# = process1 [p1=p#i#, b1=b#i#, r1=r#i#] endmodule#end#
