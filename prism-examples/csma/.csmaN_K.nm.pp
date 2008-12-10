// CSMA/CD protocol - probabilistic version of kronos model (3 stations)
// gxn/dxp 04/12/01

mdp

// note made changes since cannot have strict inequalities
// in digital clocks approach and suppose a station only sends one message

// actual parameters
#const N#
const int N = #N#; // number of processes
#const K#
const int K = #K#; // exponential backoff limit
const int slot = 2*sigma; // length of slot
const int M = floor(pow(2, K))-1 ; // max number of slots to wait
//const int lambda=782;
//const int sigma=26;

// simplified parameters scaled
const int sigma=1; // time for messages to propagate along the bus
const int lambda=30; // time to send a message

//----------------------------------------------------------------------------------------------------------------------------
// the bus
module bus
	
	b : [0..2];
	// b=0 - idle
	// b=1 - active
	// b=2 - collision
	
	// clocks of bus
	y1 : [0..sigma+1]; // time since first send (used find time until channel sensed busy)
	y2 : [0..sigma+1]; // time since second send (used to find time until collision detected)
	
	// a sender sends (ok - no other message being sent)
	#for i=1:N#
	[send#i#] (b=0) -> (b'=1);
	#end#
	
	// a sender sends (bus busy - collision)
	#for i=1:N#
	[send#i#] (b=1|b=2) & (y1<sigma) -> (b'=2);
	#end#
	
	// finish sending
	#for i=1:N#
	[end#i#] (b=1) -> (b'=0) & (y1'=0);
	#end#
	
	// bus busy
	#for i=1:N#
	[busy#i#] (b=1|b=2) & (y1>=sigma) -> (b'=b);  
	#end#
	
	// collision detected
	[cd] (b=2) & (y2<=sigma) -> (b'=0) & (y1'=0) & (y2'=0);
	
	// time passage
	[time] (b=0) -> (y1'=0); // value of y1/y2 does not matter in state 0
	[time] (b=1) -> (y1'=min(y1+1,sigma+1)); // no invariant in state 1
	[time] (b=2) & (y2<sigma) -> (y1'=min(y1+1,sigma+1)) & (y2'=min(y2+1,sigma+1)); // invariant in state 2 (time until collision detected)
	
endmodule

//----------------------------------------------------------------------------------------------------------------------------
// model of first sender
module station1
	
	// LOCAL STATE
	s1 : [0..5];
	// s1=0 - initial state
	// s1=1 - transmit
	// s1=2 - collision (set backoff)
	// s1=3 - wait (bus busy)
	// s1=4 - successfully sent
	
	// LOCAL CLOCK
	x1 : [0..max(lambda,slot)];
	
	// BACKOFF COUNTER (number of slots to wait)
	bc1 : [0..M];
	
	// COLLISION COUNTER
	cd1 : [0..K];
	
	// start sending
	[send1] (s1=0) -> (s1'=1) & (x1'=0); // start sending
	[busy1] (s1=0) -> (s1'=2) & (x1'=0) & (cd1'=min(K,cd1+1)); // detects channel is busy so go into backoff
	
	// transmitting
	[time] (s1=1) & (x1<lambda) -> (x1'=min(x1+1,lambda)); // let time pass
	[end1]  (s1=1) & (x1=lambda) -> (s1'=4) & (x1'=0); // finished
	[cd]   (s1=1) -> (s1'=2) & (x1'=0) & (cd1'=min(K,cd1+1)); // collision detected (increment backoff counter)
	[cd] !(s1=1) -> (s1'=s1); // add loop for collision detection when not important
	
	// set backoff (no time can pass in this state)
	// probability depends on which transmission this is (cd1)
	#for i=1:K#
	#const i2=floor(pow(2,i))#
	[] s1=2 & cd1=#i# -> #+ j=0:i2-1# 1/#i2# : (s1'=3) & (bc1'=#j#) #end#;
	#end#
	
	// wait until backoff counter reaches 0 then send again
	[time] (s1=3) & (x1<slot) -> (x1'=x1+1); // let time pass (in slot)
	[time] (s1=3) & (x1=slot) & (bc1>0) -> (x1'=1) & (bc1'=bc1-1); // let time pass (move slots)
	[send1] (s1=3) & (x1=slot) & (bc1=0) -> (s1'=1) & (x1'=0); // finished backoff (bus appears free)
	[busy1] (s1=3) & (x1=slot) & (bc1=0) -> (s1'=2) & (x1'=0) & (cd1'=min(K,cd1+1)); // finished backoff (bus busy)
	
	// once finished nothing matters
	[time] (s1>=4) -> (x1'=0);

endmodule

//----------------------------------------------------------------------------------------------------------------------------

// construct further stations through renaming
#for i=2:N#
module station#i#=station1[s1=s#i#,x1=x#i#,cd1=cd#i#,bc1=bc#i#,send1=send#i#,busy1=busy#i#,end1=end#i#] endmodule
#end#

//----------------------------------------------------------------------------------------------------------------------------

// reward structure for expected time
rewards "time"
	[time] true : 1;
endrewards

//----------------------------------------------------------------------------------------------------------------------------

// labels/formulae
label "all_delivered" = #& i=1:N#s#i#=4#end#;
label "one_delivered" = #| i=1:N#s#i#=4#end#;
label "collision_max_backoff" = #| i=1:N#(cd#i#=K & s#i#=1 & b=2)#end#;
formula min_backoff_after_success = min(#, i=1:N#s#i#=4?cd#i#:K+1#end#);
formula min_collisions = min(#, i=1:N#cd#i##end#);
formula max_collisions = max(#, i=1:N#cd#i##end#);

