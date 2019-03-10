// CROWDS [Reiter,Rubin]
// Vitaly Shmatikov, 2002

// Note:
// Change everything marked CWDSIZ when changing the size of the crowd
// Change everything marked CWDMAX when increasing max size of the crowd

dtmc

// Probability of forwarding
const double PF = 0.8;

// Probability that a crowd member is bad
const double  badC = 0.091;
// const double  badC = 0.167;

const int TotalRuns; // Total number of protocol runs to analyze
const int CrowdSize; // CWDSIZ: actual number of good crowd members
const int MaxGood=20; // CWDMAX: maximum number of good crowd members

// Process definitions
module crowds

	// Auxiliary variables
	launch:   bool init true;       // Start modeling?
	new:      bool init false;      // Initialize a new protocol instance?
	runCount: [0..TotalRuns] init TotalRuns;   // Counts protocol instances
	start:    bool init false;      // Start the protocol?
	run:      bool init false;      // Run the protocol?
	lastSeen: [0..MaxGood] init MaxGood;   // Last crowd member to touch msg
	good:     bool init false;      // Crowd member is good?
	bad:      bool init false;      //              ... bad?
	recordLast: bool init false;    // Record last seen crowd member?
	badObserve: bool init false;    // Bad members observes who sent msg?
	deliver:  bool init false;      // Deliver message to destination?
	done:     bool init false;      // Protocol instance finished?

	// Counters for attackers' observations
	// CWDMAX: 1 counter per each good crowd member
	observe0:  [0..TotalRuns] init 0;
	observe1:  [0..TotalRuns] init 0;
	observe2:  [0..TotalRuns] init 0;
	observe3:  [0..TotalRuns] init 0;
	observe4:  [0..TotalRuns] init 0;
	observe5:  [0..TotalRuns] init 0;
	observe6:  [0..TotalRuns] init 0;
	observe7:  [0..TotalRuns] init 0;
	observe8:  [0..TotalRuns] init 0;
	observe9:  [0..TotalRuns] init 0;
	observe10: [0..TotalRuns] init 0;
	observe11: [0..TotalRuns] init 0;
	observe12: [0..TotalRuns] init 0;
	observe13: [0..TotalRuns] init 0;
	observe14: [0..TotalRuns] init 0;
	observe15: [0..TotalRuns] init 0;
	observe16: [0..TotalRuns] init 0;
	observe17: [0..TotalRuns] init 0;
	observe18: [0..TotalRuns] init 0;
	observe19: [0..TotalRuns] init 0;
	
	[] launch -> (new'=true) & (runCount'=TotalRuns) & (launch'=false);
	// Set up a new protocol instance
	[] new & runCount>0 -> (runCount'=runCount-1) & (new'=false) & (start'=true);
	
	// SENDER
	// Start the protocol
	[] start -> (lastSeen'=0) & (run'=true) & (deliver'=false) & (start'=false);
	
	// CROWD MEMBERS
	// Good or bad crowd member?
	[] !good & !bad & !deliver & run ->
	             1-badC : (good'=true) & (recordLast'=true) & (run'=false) +
	               badC : (bad'=true)  & (badObserve'=true) & (run'=false);

	// GOOD MEMBERS
	// Forward with probability PF, else deliver
	[] good & !deliver & run -> PF : (good'=false) + 1-PF : (deliver'=true);
	// Record the last crowd member who touched the msg;
	// all good members may appear with equal probability
	//    Note: This is backward.  In the real protocol, each honest
	//          forwarder randomly chooses the next forwarder.
	//          Here, the identity of an honest forwarder is randomly
	//          chosen *after* it has forwarded the message.
	[] recordLast & CrowdSize=2 ->
	        1/2 : (lastSeen'=0) & (recordLast'=false) & (run'=true) +
	        1/2 : (lastSeen'=1) & (recordLast'=false) & (run'=true);
	[] recordLast & CrowdSize=4 ->
	        1/4 : (lastSeen'=0) & (recordLast'=false) & (run'=true) +
	        1/4 : (lastSeen'=1) & (recordLast'=false) & (run'=true) +
	        1/4 : (lastSeen'=2) & (recordLast'=false) & (run'=true) +
	        1/4 : (lastSeen'=3) & (recordLast'=false) & (run'=true);
	[] recordLast & CrowdSize=5 ->
	        1/5 : (lastSeen'=0) & (recordLast'=false) & (run'=true) +
	        1/5 : (lastSeen'=1) & (recordLast'=false) & (run'=true) +
	        1/5 : (lastSeen'=2) & (recordLast'=false) & (run'=true) +
	        1/5 : (lastSeen'=3) & (recordLast'=false) & (run'=true) +
	        1/5 : (lastSeen'=4) & (recordLast'=false) & (run'=true);
	[] recordLast & CrowdSize=10 ->
	        1/10 : (lastSeen'=0) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=1) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=2) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=3) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=4) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=5) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=6) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=7) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=8) & (recordLast'=false) & (run'=true) +
	        1/10 : (lastSeen'=9) & (recordLast'=false) & (run'=true);
	[] recordLast & CrowdSize=15 ->
	        1/15 : (lastSeen'=0)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=1)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=2)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=3)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=4)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=5)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=6)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=7)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=8)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=9)  & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=10) & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=11) & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=12) & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=13) & (recordLast'=false) & (run'=true) +
	        1/15 : (lastSeen'=14) & (recordLast'=false) & (run'=true);
	[] recordLast & CrowdSize=20 ->
	        1/20 : (lastSeen'=0)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=1)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=2)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=3)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=4)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=5)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=6)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=7)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=8)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=9)  & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=10) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=11) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=12) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=13) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=14) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=15) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=16) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=17) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=18) & (recordLast'=false) & (run'=true) +
	        1/20 : (lastSeen'=19) & (recordLast'=false) & (run'=true);
	
	// BAD MEMBERS
	// Remember from whom the message was received and deliver
	// CWDMAX: 1 rule per each good crowd member
	[] lastSeen=0  & badObserve & observe0 <TotalRuns -> (observe0' =observe0 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=1  & badObserve & observe1 <TotalRuns -> (observe1' =observe1 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=2  & badObserve & observe2 <TotalRuns -> (observe2' =observe2 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=3  & badObserve & observe3 <TotalRuns -> (observe3' =observe3 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=4  & badObserve & observe4 <TotalRuns -> (observe4' =observe4 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=5  & badObserve & observe5 <TotalRuns -> (observe5' =observe5 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=6  & badObserve & observe6 <TotalRuns -> (observe6' =observe6 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=7  & badObserve & observe7 <TotalRuns -> (observe7' =observe7 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=8  & badObserve & observe8 <TotalRuns -> (observe8' =observe8 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=9  & badObserve & observe9 <TotalRuns -> (observe9' =observe9 +1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=10 & badObserve & observe10<TotalRuns -> (observe10'=observe10+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=11 & badObserve & observe11<TotalRuns -> (observe11'=observe11+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=12 & badObserve & observe12<TotalRuns -> (observe12'=observe12+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=13 & badObserve & observe13<TotalRuns -> (observe13'=observe13+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=14 & badObserve & observe14<TotalRuns -> (observe14'=observe14+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=15 & badObserve & observe15<TotalRuns -> (observe15'=observe15+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=16 & badObserve & observe16<TotalRuns -> (observe16'=observe16+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=17 & badObserve & observe17<TotalRuns -> (observe17'=observe17+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=18 & badObserve & observe18<TotalRuns -> (observe18'=observe18+1) & (deliver'=true) & (run'=true) & (badObserve'=false);
	[] lastSeen=19 & badObserve & observe19<TotalRuns -> (observe19'=observe19+1) & (deliver'=true) & (run'=true) & (badObserve'=false);

	// RECIPIENT
	// Delivery to destination
	[] deliver & run -> (done'=true) & (deliver'=false) & (run'=false) & (good'=false) & (bad'=false);
	// Start a new instance
	[] done -> (new'=true) & (done'=false) & (run'=false) & (lastSeen'=MaxGood);
	
endmodule

