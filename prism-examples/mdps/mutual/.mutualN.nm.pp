#const N#
// mutual exclusion [PZ82]
// dxp/gxn 19/12/99

mdp

// atomic formula
// none in low, high, tie
formula none_lht = #& i=2:N#!(p#i#>=4&p#i#<=13)#end#;
// some in admit
formula some_a   = #| i=2:N#(p#i#>=14&p#i#<=15)#end#;
// some in high, admit
formula some_ha  = #| i=2:N#(p#i#>=4&p#i#<=5|p#i#>=10&p#i#<=15)#end#;
// none in high, tie, admit
formula none_hta = #& i=2:N#(p#i#>=0&p#i#<=3|p#i#>=7&p#i#<=8)#end#;
// none in enter
formula none_e   = #& i=2:N#!(p#i#>=2&p#i#<=3)#end#;


module process1

	p1: [0..15];
	
	[] p1=0 -> (p1'=0);
	[] p1=0 -> (p1'=1);
	[] p1=1 -> (p1'=2);
	[] p1=2 &  (none_lht | some_a) -> (p1'=3);
	[] p1=2 & !(none_lht | some_a) -> (p1'=2);
	[] p1=3 -> (p1'=4);
	[] p1=3 -> (p1'=7);
	[] p1=4 &  some_ha -> (p1'=5);
	[] p1=4 & !some_ha -> (p1'=10);
	[] p1=5 -> (p1'=6);
	[] p1=6 &  some_ha -> (p1'=6);
	[] p1=6 & !some_ha -> (p1'=9);
	[] p1=7 &  none_hta -> (p1'=8);
	[] p1=7 & !none_hta -> (p1'=7);
	[] p1=8  -> (p1'=9);
	[] p1=9  -> 0.5 : (p1'=4) + 0.5 : (p1'=7);
	[] p1=10 -> (p1'=11);
	[] p1=11 &  none_lht -> (p1'=13);
	[] p1=11 & !none_lht -> (p1'=12);
	[] p1=12 -> (p1'=0);
	[] p1=13 -> (p1'=14);
	[] p1=14 &  none_e -> (p1'=15);
	[] p1=14 & !none_e -> (p1'=14);
	[] p1=15 -> (p1'=0);
	
endmodule

// construct further modules through renaming

#for i=2:N#
module process#i# = process1 [ p1=p#i#, p#i#=p1 ] endmodule
#end#

// formulas/labels for properties

// number of procs in critical section
formula num_crit = #+ i=1:N#p#1#>9?1:0#end#;
// some process is between 4 and 13
label "some_4_13"  = #| i=1:N#(p#i#>=4&p#i#<=13)#end#;
// some process is in 14
label "some_14"    = #| i=1:N#(p#i#=14)#end#;


