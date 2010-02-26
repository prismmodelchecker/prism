#const N#
#const M#
#const K#
// full model of zeroconf protocol
// dxp/gxn 21/09/06

mdp

// constants
const int probe_time = 20; // time to send a probe
const int N = #N#;          // number of existing hosts
const int M = #M#;         // number of possible ip addresses
const int K = #K#;          // number of probes to send
// host ip addresses
#for i=1:N#
const int ip#i# = #i#;
#end#

// time for messages to be sent and probability of message loss
const int min_send1 = 2;
const int max_send1 = 8;
const double loss1 = 0.01*(min_send1+max_send1)/2;

const int min_send2 = 3;
const int max_send2 = 6;
const double loss2 = 0.01*(min_send2+max_send2)/2;

const int min_send3 = 1;
const int max_send3 = 2;
const double loss3 = 0.01*(min_send3+max_send3)/2;

const int min_send4 = 6;
const int max_send4 = 9;
const double loss4 = 0.01*(min_send4+max_send4)/2;

// formula: true when the channel is free
formula channel_free = chan=0;

module channel

	chan : [0..1];
	
	[broadcast] true -> (chan'=1);
	
#for i=1:N#
	[rec0#i#] #+ j=1:N#s0#j##end#=1 -> (chan'=0);
#end#

#for i=1:N#
	[rec0#i#] #+ j=1:N#s0#j##end#>1 -> true;
#end#

#for i=1:N#
	[send#i#0] true -> (chan'=1);
#end#
	
#for i=1:N#
	[rec#i#0] true -> (chan'=0);
#end#
	
endmodule

// host which is trying to configure its ip address
module host0
	
	s0 : [0..4];
	// 0 make random choice
	// 1 send first probe (to buffer)
	// 2 send remaining probes
	// 3 wait before using
	// 4 using
	
	ip0 : [0..M]; // current chosen ip address
	x0 : [0..probe_time]; // local clock
	probes : [0..K]; // number of probes sent
	
	// make choice (rest action used to clear buffer)
	[reset] s0=0 -> #+ i=1:M#1/M : (ip0'=#i#) & (s0'=1)#end#;

	// send first probe
	[broadcast] s0=1 & probes=0 -> (s0'=2) & (probes'=probes+1);
	// let time pass before sending next probe
	[time]    s0=2 & x0<probe_time -> (x0'=x0+1);
	// send a probe (not last probe)
	[broadcast] s0=2 & x0=probe_time & probes<K-1 -> (s0'=2) & (probes'=probes+1) & (x0'=0);
	// send last probe
	[broadcast] s0=2 & x0=probe_time & probes=K-1 -> (s0'=3) & (probes'=0) & (x0'=0);
	// wait
	[time] s0=3 & x0<probe_time -> (x0'=x0+1);
	// finished waiting
	[done] s0=3 & x0=probe_time -> (s0'=4) & (x0'=0);
	// use ip address (loop)
	[] s0=4 -> true;
	
	// receive an ARP and reconfigure (same IP address)
#for i=1:N#
	[rec#i#0] s0>0 & s0<4 & ip0=m#i#0 -> (s0'=0) & (probes'=0) & (ip0'=0) & (x0'=0);
#end#
	// receive an ARP and do nothing (different IP address)
#for i=1:N#
	[rec#i#0] s0=0 | (s0>0 & s0<4 & !ip0=m#i#0) -> true;
#end#
	
endmodule

// messages to host 1
module sender01

	s01 : [0..1]; // 0 - no message, 1 - message being sent
	x01 : [0..max_send1]; // local clock 
	m01 : [0..M]; // ip address in message
	
	// let time pass if nothing being sent
	[time] s01=0 -> true;
	// receive a message to be sent
	[broadcast] s01=0 -> 1-loss1 : (s01'=1) & (m01'=ip0) + loss1 : (s01'=1) & (m01'=0);
	// message not arrived yet
	[time]    s01=1 & x01<max_send1 -> (x01'=x01+1);
	// message arrives
	[rec01]   s01=1 & x0>=min_send1 -> (s01'=0) & (x01'=0) & (m01'=0);
	
endmodule

// messages to host 2
#for i=2:N#
module sender0#i# = sender01 [ s01=s0#i#, ip1=ip#i#, m01=m0#i#, x01=x0#i#, rec01=rec0#i#, min_send1=min_send#func(mod,i-1,4)+1#, max_send1=max_send#func(mod,i-1,4)+1#, loss1=loss#func(mod,i-1,4)+1# ] endmodule
#end#

// messages from sender 1 (need a buffer)
module sender10

	s10 : [0..2]; // 0 - no message, 1 - message to be sent 2 sending message
	x10 : [0..max_send1]; // local clock 
	m10 : [0..M]; // ip address in message
	

	// nothing to send so let time pass
	[time]  s10=0 -> true;
	// receive a message and no reply
	[rec01] !m01=ip1 -> true;
	// receive a message and reply (add to buffer)
	[rec01] m01=ip1 -> (s10'=1) & (m10'=m01);
	// cannot reply yet
	[time] s10=1 & !channel_free -> true;
	[send10] s10=1 & channel_free -> 1-loss1 : (s10'=2) & (x10'=0)
	                           + loss1 : (s10'=2) & (x10'=0) & (m10'=0);
	// message not arrived yet
	[time]  s10=2 & x10<max_send1 -> (x10'=x10+1);
	// deliver message
	[rec10] s10=2 & x10>=min_send1 -> (s10'=0) & (m10'=0) & (x10'=0);
	
endmodule

// messages from host 2
#for i=2:N#
module sender#i#0 = sender10 [ s10=s#i#0, m10=m#i#0, m01=m0#i#, x10=x#i#0, rec01=rec0#i#, send10=send#i#0, rec10=rec#i#0, ip1=ip#i#, min_send1=min_send#func(mod,i-1,4)+1#, max_send1=max_send#func(mod,i-1,4)+1#, loss1=loss#func(mod,i-1,4)+1# ] endmodule
#end#

label "done_correct" = s0=4 & ip0<=N;

rewards
	
	[time] true : 0.1;
	[done] ip0=1 | ip0=2 : 1000000;
	
endrewards
