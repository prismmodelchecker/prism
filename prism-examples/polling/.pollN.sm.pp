// polling example [IT90]
// gxn/dxp 26/01/00

ctmc

#const N#
const int N = #N#;

const double mu		= 1;
const double gamma	= 200;
const double lambda	= mu/N;

module server
	
	s : [1..#N#]; // station
	a : [0..1]; // action: 0=polling, 1=serving
	
	#for i=1:N-1#
	[loop#i#a] (s=#i#)&(a=0) -> gamma	: (s'=s+1);
	[loop#i#b] (s=#i#)&(a=0) -> gamma	: (a'=1);
	[serve#i#] (s=#i#)&(a=1) -> mu		: (s'=s+1)&(a'=0);
	
	#end#
	[loop#N#a] (s=#N#)&(a=0) -> gamma	: (s'=1);
	[loop#N#b] (s=#N#)&(a=0) -> gamma	: (a'=1);
	[serve#N#] (s=#N#)&(a=1) -> mu		: (s'=1)&(a'=0);
	
endmodule

module station1
	
	s1 : [0..1]; // state of station: 0=empty, 1=full
	
	[loop1a] (s1=0) -> 1 : (s1'=0);
	[]       (s1=0) -> lambda : (s1'=1);
	[loop1b] (s1=1) -> 1 : (s1'=1);
	[serve1] (s1=1) -> 1 : (s1'=0);
	
endmodule

// construct further stations through renaming

#for i=2:N#
module station#i# = station1 [ s1=s#i#, loop1a=loop#i#a, loop1b=loop#i#b, serve1=serve#i# ] endmodule
#end#
// (cumulative) rewards

// expected time station 1 is waiting to be served
rewards "waiting"
	s1=1 & !(s=1 & a=1) : 1;
endrewards

// expected number of times station 1 is served
rewards "served"
	[serve1] true : 1;
endrewards
