#const N#
// randomized dining philosophers [LR81]
// dxp/gxn 23/01/02

// model which does not require fairness
// remove the possibility of loops:
// (1) cannot stay in thinking 
// (2) if first fork not free then cannot move (another philosopher must more)

mdp

// atomic formulae 
// left fork free and right fork free resp.
formula lfree = (p2>=0&p2<=4)|p2=6|p2=10;
formula rfree = (p#N#>=0&p#N#<=3)|p#N#=5|p#N#=7|p#N#=11;

module phil1

	p1: [0..11];

	[] p1=0 -> (p1'=1); // trying
	[] p1=1 -> 0.5 : (p1'=2) + 0.5 : (p1'=3); // draw randomly
	[] p1=2 &  lfree -> (p1'=4); // pick up left
	[] p1=3 &  rfree -> (p1'=5); // pick up right
	[] p1=4 &  rfree -> (p1'=8); // pick up right (got left)
	[] p1=4 & !rfree -> (p1'=6); // right not free (got left)
	[] p1=5 &  lfree -> (p1'=8); // pick up left (got right)
	[] p1=5 & !lfree -> (p1'=7); // left not free (got right)
	[] p1=6  -> (p1'=1); // put down left
	[] p1=7  -> (p1'=1); // put down right
	[] p1=8  -> (p1'=9); // move to eating (got forks)
	[] p1=9  -> (p1'=10); // finished eating and put down left 
	[] p1=9  -> (p1'=11); // finished eating and put down right
	[] p1=10 -> (p1'=0); // put down right and return to think
	[] p1=11 -> (p1'=0); // put down left and return to think

endmodule

// construct further modules through renaming
#for i=2:N#
module phil#i# = phil1 [ p1=p#i#, p2=p#mod(i,N)+1#, p#N#=p#mod(i-2,N)+1# ] endmodule
#end#

// rewards (number of steps)
rewards "num_steps"
	[] true : 1;
endrewards
// labels
label "hungry" = #| i=1:N#((p#i#>0)&(p#i#<8))#end#;
label "eat" = #| i=1:N#((p#i#>=8)&(p#i#<=9))#end#;


