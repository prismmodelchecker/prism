// bounded retransmission protocol [D'AJJL01]
// gxn/dxp 23/05/2001

pta

// number of chunks
const int N;
// maximum number of retransmissions
const int MAX;
// max time to send a message (transition delay)
const int TD;
// time out limit (greater than TD+TD - the max time to send a message and receive an ack)
const int TIME_OUT;

module sender

	// clock of the sender
	x : clock;

	s : [0..6];
	// 0 idle
	// 1 next_frame	
	// 2 wait_ack
	// 3 retransmit
	// 4 success
	// 5 error
	// 6 wait sync
	srep : [0..3];
	// 0 bottom
	// 1 not ok (nok)
	// 2 do not know (dk)
	// 3 ok (ok)
	nrtr : [0..MAX];
	i : [0..N];
	bs : bool;
	s_ab : bool;
	fs : bool;
	ls : bool;

	invariant
		(s=2 => x<=TIME_OUT) // only wait until timeout
	endinvariant
	
	// idle
	[NewFile] (s=0) -> (s'=1) & (i'=1) & (srep'=0);
	// next_frame
	[aF] (s=1) -> (s'=2) & (fs'=(i=1)) & (ls'=(i=N)) & (bs'=s_ab) & (nrtr'=0) & (x'=0);
	// wait_ack
	[aB] (s=2) -> (s'=4) & (s_ab'=!s_ab);
	[] (s=2) & x=TIME_OUT -> (s'=3); // time out
	// retransmit
	[aF] (s=3) & (nrtr<MAX) -> (s'=2) & (fs'=(i=1)) & (ls'=(i=N)) & (bs'=s_ab) & (nrtr'=nrtr+1) & (x'=0);
	[] (s=3) & (nrtr=MAX) & (i<N) -> (s'=5) & (srep'=1);
	[] (s=3) & (nrtr=MAX) & (i=N) -> (s'=5) & (srep'=2);
	// success
	[] (s=4) & (i<N) -> (s'=1) & (i'=i+1);
	[] (s=4) & (i=N) -> (s'=0) & (srep'=3);
	// error
	[SyncWait] (s=5) -> (s'=6); 
	// wait sync
	[SyncWait] (s=6) -> (s'=0) & (s_ab'=false); 
	
endmodule

module receiver

	r : [0..5];
	// 0 new_file
	// 1 fst_safe
	// 2 frame_received
	// 3 frame_reported
	// 4 idle
	// 5 resync
	rrep : [0..4];
	// 0 bottom
	// 1 fst
	// 2 inc
	// 3 ok
	// 4 nok
	fr : bool;
	lr : bool;
	br : bool;
	r_ab : bool;
	recv : bool;
	
	
	// new_file
	[SyncWait] (r=0) -> (r'=0);
	[aG] (r=0) -> (r'=1) & (fr'=fs) & (lr'=ls) & (br'=bs) & (recv'=T);
	// fst_safe_frame
	[] (r=1) -> (r'=2) & (r_ab'=br);
	// frame_received
	[] (r=2) & (r_ab=br) & (fr=true) & (lr=false)  -> (r'=3) & (rrep'=1);
	[] (r=2) & (r_ab=br) & (fr=false) & (lr=false) -> (r'=3) & (rrep'=2);
	[] (r=2) & (r_ab=br) & (fr=false) & (lr=true)  -> (r'=3) & (rrep'=3);
	[aA] (r=2) & !(r_ab=br) -> (r'=4);  
	// frame_reported
	[aA] (r=3) -> (r'=4) & (r_ab'=!r_ab);
	// idle
	[aG] (r=4) -> (r'=2) & (fr'=fs) & (lr'=ls) & (br'=bs) & (recv'=T);
	[SyncWait] (r=4) & (ls=true) -> (r'=5);
	[SyncWait] (r=4) & (ls=false) -> (r'=5) & (rrep'=4);
	// resync
	[SyncWait] (r=5) -> (r'=0) & (rrep'=0);
	
endmodule
	
module checker // prevents more than one frame being set

	T : bool;
	
	[NewFile] (T=false) -> (T'=true);
	
endmodule

module	channelK

	xK : clock;
	k : [0..1];

	invariant
		(k=1 => xK<=TD)
	endinvariant
	
	// idle
	[aF] (k=0) -> 0.98 : (k'=1) & (xK'=0) + 0.02 : (k'=0) & (xK'=0);
	// sending
	[aG] (k=1) & xK<=TD -> (k'=0) & (xK'=0);
	
endmodule

module	channelL

	xL : clock;
	l : [0..1];

	invariant
		(l=1 => xL<=TD)
	endinvariant
	
	// idle
	[aA] (l=0) -> 0.99 : (l'=1) & (xL'=0) + 0.02 : (l'=0) & (xL'=0);
	// sending
	[aB] (l=1) & xL<=TD -> (l'=0) & (xL'=0);

endmodule

rewards
	[aF] i=1 : 1;
endrewards
