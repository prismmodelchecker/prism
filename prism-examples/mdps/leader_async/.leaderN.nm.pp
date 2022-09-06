// asynchronous leader election
// 4 processes
// gxn/dxp 29/01/01

mdp

#const N#
const N= #N#; // number of processes

//----------------------------------------------------------------------------------------------------------------------------
module process1
	
	// COUNTER
	c1 : [0..#N#-1];
	
	// STATES
	s1 : [0..4];
	// 0  make choice
	// 1 have not received neighbours choice
	// 2 active
	// 3 inactive
	// 4 leader
	
	// PREFERENCE
	p1 : [0..1];
	
	// VARIABLES FOR SENDING AND RECEIVING
	receive1 : [0..2];
	// not received anything
	// received choice
	// received counter
	sent1 : [0..2];
	// not send anything
	// sent choice
	// sent counter
	
	// pick value
	[] (s1=0) -> 0.5 : (s1'=1) & (p1'=0) + 0.5 : (s1'=1) & (p1'=1);
	
	// send preference
	[p12] (s1=1) & (sent1=0) -> (sent1'=1);
	// receive preference
	// stay active
	[p#N#1] (s1=1) & (receive1=0) & !( (p1=0) & (p#N#=1) ) -> (s1'=2) & (receive1'=1);
	// become inactive
	[p#N#1] (s1=1) & (receive1=0) & (p1=0) & (p#N#=1) -> (s1'=3) & (receive1'=1);
	
	// send preference (can now reset preference)
	[p12] (s1=2) & (sent1=0) -> (sent1'=1) & (p1'=0);
	// send counter (already sent preference)
	// not received counter yet
	[c12] (s1=2) & (sent1=1) & (receive1=1) -> (sent1'=2);
	// received counter (pick again)
	[c12] (s1=2) & (sent1=1) & (receive1=2) -> (s1'=0) & (p1'=0) & (c1'=0) & (sent1'=0) & (receive1'=0);
	
	// receive counter and not sent yet (note in this case do not pass it on as will send own counter)
	[c#N#1] (s1=2) & (receive1=1) & (sent1<2) -> (receive1'=2);
	// receive counter and sent counter
	// only active process (decide)
	[c#N#1] (s1=2) & (receive1=1) & (sent1=2) & (c#N#=N-1) -> (s1'=4) & (p1'=0) & (c1'=0) & (sent1'=0) & (receive1'=0);
	// other active process (pick again)
	[c#N#1] (s1=2) & (receive1=1) & (sent1=2) & (c#N#<N-1) -> (s1'=0) & (p1'=0) & (c1'=0) & (sent1'=0) & (receive1'=0);
	
	// send preference (must have received preference) and can now reset
	[p12] (s1=3) & (receive1>0) & (sent1=0) -> (sent1'=1) & (p1'=0);
	// send counter (must have received counter first) and can now reset
	[c12] (s1=3) & (receive1=2) & (sent1=1) ->  (s1'=3) & (p1'=0) & (c1'=0) & (sent1'=0) & (receive1'=0);
	
	// receive preference
	[p#N#1] (s1=3) & (receive1=0) -> (p1'=p#N#) & (receive1'=1);
	// receive counter
	[c#N#1] (s1=3) & (receive1=1) & (c#N#<N-1) -> (c1'=c#N#+1) & (receive1'=2);
		
	// done
	[done] (s1=4) -> (s1'=s1);
	// add loop for processes who are inactive
	[done] (s1=3) -> (s1'=s1);

endmodule

//----------------------------------------------------------------------------------------------------------------------------

// construct further stations through renaming
#for i=2:N#
module process#i#=process1[s1=s#i#,p1=p#i#,c1=c#i#,sent1=sent#i#,receive1=receive#i#,p12=p#i##mod(i,N)+1#,p#N#1=p#mod(i-2,N)+1##i#,c12=c#i##mod(i,N)+1#,c#N#1=c#mod(i-2,N)+1##i#,p#N#=p#mod(i-2,N)+1#,c#N#=c#mod(i-2,N)+1#] endmodule
#end#

//----------------------------------------------------------------------------------------------------------------------------

// reward - expected number of rounds (equals the number of times a process receives a counter)
rewards
	[c12] true : 1;
endrewards

//----------------------------------------------------------------------------------------------------------------------------
formula leaders = #+ i=1:N#(s#i#=4?1:0)#end#;
label "elected" = #| i=1:N#s#i#=4#end#;

