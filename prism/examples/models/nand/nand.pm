// nand multiplex system
// gxn/dxp 20/03/03

// U (correctly) performs a random permutation of the outputs of the previous stage

dtmc

const int N; // number of inputs in each bundle
const int K; // number of restorative stages

const int M = 2*K+1; // total number of multiplexing units

// parameters taken from the following paper
// A system architecture solution for unreliable nanoelectric devices
// J. Han & P. Jonker
// IEEEE trans. on nanotechnology vol 1(4) 2002

const double perr = 0.02; // probability nand works correctly
const double prob1 = 0.9; // probability initial inputs are stimulated

// model whole system as a single module by resuing variables 
// to decrease the state space
module multiplex

	u : [1..M]; // number of stages
	c : [0..N]; // counter (number of copies of the nand done)

	s : [0..4]; // local state
	// 0 - initial state
	// 1 - set x inputs
	// 2 - set y inputs
	// 3 - set outputs
	// 4 - done
	z : [0..N]; // number of new outputs equal to 1
	zx : [0..N]; // number of old outputs equal to 1
	zy : [0..N]; // need second copy for y
	// initially 9 since initially probability of stimulated state is 0.9

	x : [0..1]; // value of first input
	y : [0..1]; // value of second input
	
	[] s=0 & (c<N) -> (s'=1); // do next nand if have not done N yet
	[] s=0 & (c=N) & (u<M) -> (s'=1) & (zx'=z) & (zy'=z) & (z'=0) & (u'=u+1) & (c'=0); // move on to next u if not finished
	[] s=0 & (c=N) & (u=M) -> (s'=4) & (zx'=0) & (zy'=0) & (x'=0) & (y'=0); // finished (so reset variables not needed to reduce state space)

	// choose x permute selection (have zx stimulated inputs)
	// note only need y to be random	
	[] s=1 & u=1  -> prob1 : (x'=1) & (s'=2) + (1-prob1) : (x'=0) & (s'=2); // initially random
	[] s=1 & u>1 & zx>0 -> (x'=1) & (s'=2) & (zx'=zx-1);
	[] s=1 & u>1 & zx=0 -> (x'=0) & (s'=2);

	// choose x randomly from selection (have zy stimulated inputs)
	[] s=2 & u=1 -> prob1 : (y'=1) & (s'=3) + (1-prob1) : (y'=0) & (s'=3); // initially random
	[] s=2 & u>1 & zy<(N-c) & zy>0  -> zy/(N-c) : (y'=1) & (s'=3) & (zy'=zy-1) + 1-(zy/(N-c)) : (y'=0) & (s'=3);
	[] s=2 & u>1 & zy=(N-c) & c<N -> 1 : (y'=1) & (s'=3) & (zy'=zy-1);
	[] s=2 & u>1 & zy=0 -> 1 : (y'=0) & (s'=3);

	// use nand gate
	[] s=3 & z<N & c<N -> (1-perr) : (z'=z+(1-x*y)) & (s'=0) & (c'=c+1) & (x'=0) & (y'=0) // not faulty
	         + perr    : (z'=z+(x*y))    & (s'=0) & (c'=c+1) & (x'=0) & (y'=0); // von neumann fault
	// [] s=3 & z<N -> (1-perr) : (z'=z+(1-x*y)) & (s'=0) & (c'=c+1) & (x'=0) & (y'=0) // not faulty
	//         + perr    : (z'=z+(x*y))    & (s'=0) & (c'=c+1) & (x'=0) & (y'=0); // von neumann fault
	
	[] s=4 -> true;
	
endmodule
// rewards: final value of gate
rewards
	[] s=0 & (c=N) & (u=M) : z/N;
endrewards