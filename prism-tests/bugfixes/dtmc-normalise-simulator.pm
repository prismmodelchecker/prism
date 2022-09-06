// Bug fixed in rev 7797:
// simulator was normalising DTMC bu dividing by number of choices
// (which was inconsistent with symbolic construction when using -noprobchecks)

// Discrete-time Markov chain model
dtmc

// Number of fitness levels: n
const int n = 3;

// Total number of agents/particles: k
const int k = 3;

module tournament

	// Counters: ci = number of agents/particles with fitness i
	c1 : [0..k] init 1;
	c2 : [0..k] init 1;
	c3 : [0..k] init 1;
	
	// Possible reactions between agents/particles
	// Each possible pairwise collision
	[r12] c1>0 & c2>0 & c2<k -> 2*c1*c2: (c1'=c1-1) & (c2'=c2+1);
	[r13] c1>0 & c3>0 & c3<k -> 2*c1*c3: (c1'=c1-1) & (c3'=c3+1);
	[r23] c2>0 & c3>0 & c3<k -> 2*c2*c3: (c2'=c2-1) & (c3'=c3+1);
	// Collision between 2 identical agents/particles
	[r11] c1>1 -> c1*(c1-1): true;
	[r22] c2>1 -> c2*(c2-1): true;
	[r33] c3>1 -> c3*(c3-1): true;
	
endmodule

// Labels (atomic propositions) for properties:

// Finished: all agents/particles have maximum fitness
label "done" = c3>=k;

// Reward structure used to reason about passage of time (discrete steps)
rewards "time"
	true : 1;
endrewards
