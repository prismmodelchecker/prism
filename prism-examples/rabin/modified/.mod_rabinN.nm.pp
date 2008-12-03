#const N#
#const K=4+func(ceil,func(log,N,2))#
// N-processor mutual exclusion [Rab82]
// with global variables to remove sychronization 
// gxn/dxp 03/12/08

// modified version to verify a variant of the bounded waiting property
// namely the probability a process entries the critical sectiongiven it tries

// note we have removed the self loops to allow the fairness constraints 
// to be ignored during verification

// split the drawing phase into two steps but is still atomic as no
// other process can move one the first step has been made

mdp

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

// formula for process 1 drawing
formula draw = p1=1 & (b<b1 | r!=r1);

// formula to keep drawing phase atomic
// (can onlymove if no process is drawing
formula go = (#& i=2:N#draw#i#=0#end#);

module process1

	p1 : [0..2];
	b1 : [0..K];
	r1 : [0..2];
	draw1 : [0..1];

	// remain in remainder
	// [] go & p1=0 -> (p1'=0);
	// enter trying
	[] go & p1=0 -> (p1'=1);
	// make a draw
	[] go & draw & draw1=0 -> (draw1'=1);
	[] draw1=1 ->#+ i=1:K-1# #1/func(floor,func(pow,2,i))# : (b1'=#i#) & (r1'=r) & (b'=max(b,#i#)) & (draw1'=0)
	         #end#+ #1/func(floor,func(pow,2,K-1))# : (b1'=#K#) & (r1'=r) & (b'=max(b,#K#)) & (draw1'=0);
	// enter critical section and randomly set r to 1 or 2
	[] go & p1=1 & b=b1 & r=r1 & c=0 -> 0.5 : (r'=1) & (c'=1) & (b'=0) & (b1'=0) & (r1'=0) & (p1'=2)
	                             + 0.5 : (r'=2) & (c'=1) & (b'=0) & (b1'=0) & (r1'=0) & (p1'=2);
	// loop when trying and cannot make a draw or enter critical section
	// [] go & p1=1 & r1=r & b1<=b & ((c=0 & b1!=b) | c=1) -> (p1'=p1);
	// leave crictical section
	[] go & p1=2 -> (p1'=0) & (c'=0);
	// stay in critical section
	// [] go & p1=2 -> (p1'=2);
	
endmodule

// construct further modules through renaming
#for i=2:N#
module process#i# = process1 [p1=p#i#, b1=b#i#, r1=r#i#, draw1=draw#i#, draw#i#=draw1 ] endmodule#end#

// formulas/labels for use in properties:

// number of processes in critical section
formula num_procs_in_crit = #+ i=1:N#(p#i#=2?1:0)#end#;

// one of the processes is trying
label "one_trying" = #| i=1:N#p#i#=1#end#;

// one of the processes is in the critical section
label "one_critical" = #| i=1:N#p#i#=2#end#;

// maximum value of the bi's
formula maxb = max(b1#for i=2:N#,b#i##end#);
