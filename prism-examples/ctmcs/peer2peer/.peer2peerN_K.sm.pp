// Simple peer-to-peer file distribution protocol based on BitTorrent
// gxn/dxp Jan 2006

ctmc

#const N#
#const K#
// N=#N# clients, K=#K# blocks to be downloaded
// Actually there are N+1=#N+1# clients, one of which has all blocks initially

// Rate of block download for a single source
const double mu=2;

// Rate of download of block i:
// A client can download from the single client which starts with all blocks
// or from anyone that has subsequently downloaded it.
// Total number of concurrent downloads for each block is 4.
#for j=1:K#
formula rate#j#=mu*(1+min(3,#+ i=1:N#b#i##j##end#));
#end#

// client 1
module client1
	
	// bij - has client i downloaded block j yet?
	#for j=1:K#
	b1#j# : [0..1];
	#end#
	
	// Downloading of each block (see rate computations above)
	#for j=1:K#
	[] b1#j#=0 -> rate#j# : (b1#j#'=1);
	#end#
	
endmodule

// construct remaining clients through renaming
#for i=2:N#
module client#i#=client1[b11=b#i#1#for j=2:K#,b1#j#=b#i##j##end#,b#i#1=b11#for j=2:K#,b#i##j#=b1#j##end#] endmodule
#end#

// labels
#for i=1:N#
label "done#i#" = #+ j=1:K#b#i##j##end# = #K#; // client #i# has received all blocks
#end#
label "done" = #+ i=1:N#(#+ j=1:K#b#i##j##end#)#end# = #N*K#; // all clients have received all blocks

// reward: fraction of blocks received
rewards "frac_rec"
#for i=1:N#
	true : ((#+ j=1:K#b#i##j##end#)/#K#)/#N#;
#end#
endrewards
