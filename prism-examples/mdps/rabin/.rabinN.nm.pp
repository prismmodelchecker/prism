#const N#
#const K=4+func(ceil,func(log,N,2))#
// N-processor mutual exclusion [Rab82]
// gxn/dxp 03/12/08

// to remove the need for fairness constraints for this model it is sufficent
// to remove the self loops from the model 

// the step corresponding to a process making a draw has been split into two steps
// to allow us to identify states where a process will draw without knowing the value
// randomly drawn
// to correctly model the protocol and prevent erroneous behaviour, the two steps are atomic
// (i.e. no other process can move one the first step has been made)
// as for example otherwise an adversary can prevent the process from actually drawing
// in the current round by not scheduling it after it has performed the first step

mdp

// size of shared counter
const int K = #K#; // 4+ceil(log_2 N)

// global variables (all modules can read and write)
global c : [0..1]; // 0/1 critical section free/taken
global b : [0..K]; // current highest draw
global r : [1..2]; // current round

// formula for process 1 drawing
formula draw = p1=1 & (b<b1 | r!=r1);

// formula to keep drawing phase atomic
// (a process can only move if no other process is in the middle of drawing)
formula go = (#& i=2:N#draw#i#=0#end#);

module process1

	p1 : [0..2]; // local state
	//  0 remainder
	//  1 trying
	//  2 critical section
	b1 : [0..K]; // current draw: bi
	r1 : [0..2]; // current round: ri
	draw1 : [0..1]; // performed first step of drawing phase

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

// maximum current draw of the processes
formula maxb = max(b1#for i=2:N#,b#i##end#);
