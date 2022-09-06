// synchronous leader election protocol  (itai & Rodeh)
// dxp/gxn 25/01/01

dtmc

// CONSTANTS
#const N#
const N = #N#; // number of processes
#const K#
const K = #K#; // range of probabilistic choice

// counter module used to count the number of processes that have been read
// and to know when a process has decided
module counter
	
	// counter (c=i  means process j reading process (i-1)+j next)
	c : [1..N-1];
	
	// reading
	[read] c<N-1 -> (c'=c+1);
	// finished reading
	[read] c=N-1 -> (c'=c);
	//decide
	[done] #| i=1:N#u#i##end# -> (c'=c);
	// pick again reset counter 
	[retry] !(#| i=1:N#u#i##end#) -> (c'=1);
	// loop (when finished to avoid deadlocks)
	[loop] s1=3 -> (c'=c);
	
endmodule

//  processes form a ring and suppose:
// process 1 reads process 2
// process 2 reads process 3
// process 3 reads process 1
module process1
	
	// local state
	s1 : [0..3];
	// s1=0 make random choice
	// s1=1 reading
	// s1=2 deciding
	// s1=3 finished
	
	// has a unique id so far (initially true)
	u1 : bool;
	
	// value to be sent to next process in the ring (initially sets this to its own value)
	v1 : [0..K-1];
	
	// random choice
	p1 : [0..K-1];
	
	// pick value
	[pick] s1=0 -> 1/K : (s1'=1) & (p1'=0) & (v1'=0) & (u1'=true)#for i=1:K-1#
	             + 1/K : (s1'=1) & (p1'=#i#) & (v1'=#i#) & (u1'=true)#end#;
	// read
	[read] s1=1 &  u1 & c<N-1 -> (u1'=(p1!=v2)) & (v1'=v2);
	[read] s1=1 & !u1 & c<N-1 -> (u1'=false) & (v1'=v2) & (p1'=0);
	// read and move to decide
	[read] s1=1 &  u1 & c=N-1 -> (s1'=2) & (u1'=(p1!=v2)) & (v1'=0) & (p1'=0);
	[read] s1=1 & !u1 & c=N-1 -> (s1'=2) & (u1'=false) & (v1'=0);
	// deciding
	// done
	[done] s1=2 -> (s1'=3) & (u1'=false) & (v1'=0) & (p1'=0);
	//retry
	[retry] s1=2 -> (s1'=0) & (u1'=false) & (v1'=0) & (p1'=0);
	// loop (when finished to avoid deadlocks)
	[loop] s1=3 -> (s1'=3);
	
endmodule

// construct remaining processes through renaming
#for i=2:N#
module process#i# = process1 [ s1=s#i#,p1=p#i#,v1=v#i#,u1=u#i#,v2=v#mod(i,N)+1# ] endmodule
#end#

// expected number of rounds
rewards "num_rounds"
	[pick] true : 1;
endrewards

// labels
label "elected" = #& i=1:N#s#i#=3#end#;

