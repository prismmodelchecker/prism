#const N#
// model of dining cryptographers
// gxn/dxp 15/11/06

mdp

// number of cryptographers
const int N = #N#;

// constants used in renaming (identities of cryptographers)
#for i=1:N#
const int p#i# = #i#;
#end#

// global variable which decides who pays
// (0 - master pays, i=1..N - cryptographer i pays)
global pay : [0..N];

// module for first cryptographer
module crypt1
	
	coin1 : [0..2]; // value of its coin
	s1 : [0..1]; // its status (0 = not done, 1 = done)
	agree1 : [0..1]; // what it states (0 = disagree, 1 = agree)
	
	// flip coin
	[] coin1=0 -> 0.5 : (coin1'=1) + 0.5 : (coin1'=2);
	
	// make statement (once relevant coins have been flipped)
	// agree (coins the same and does not pay)
	[] s1=0 & coin1>0 & coin2>0 & coin1=coin2    & (pay!=p1) -> (s1'=1) & (agree1'=1);
	// disagree (coins different and does not pay)
	[] s1=0 & coin1>0 & coin2>0 & !(coin1=coin2) & (pay!=p1) -> (s1'=1);
	// disagree (coins the same and pays)
	[] s1=0 & coin1>0 & coin2>0 & coin1=coin2    & (pay=p1)  -> (s1'=1);
	// agree (coins different and pays)
	[] s1=0 & coin1>0 & coin2>0 & !(coin1=coin2) & (pay=p1)  -> (s1'=1) & (agree1'=1);
	
	// synchronising loop when finished to avoid deadlock
	[done] s1=1 -> true;

endmodule

// construct further cryptographers with renaming
#for i=2:N#
module crypt#i# = crypt1 [ coin1=coin#i#, s1=s#i#, agree1=agree#i#, p1=p#i#, coin2=coin#func(mod,i,N)+1# ] endmodule
#end#

// set of initial states
// (cryptographers in their initial state, "pay" can be anything)
init #& i=1:N# coin#i#=0&s#i#=0&agree#i#=0 #end# endinit

// unique integer representing outcome
formula outcome = #+ j=1:N# #func(floor,func(pow,2,N-j))#*agree#j# #end#;

// parity of number of "agree"s (0 = even, 1 = odd)
formula parity = func(mod, #+ j=1:N#agree#j##end#, 2);

// label denoting states where protocol has finished
label "done" = #& i=1:N#s#i#=1#end#;
// label denoting states where number of "agree"s is even
label "even" = func(mod,(#+ i=1:N#agree#i##end#),2)=0;
// label denoting states where number of "agree"s is even
label "odd" = func(mod,(#+ i=1:N#agree#i##end#),2)=1;

