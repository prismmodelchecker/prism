// Bug (NPE) in explicit engine when creating rewards
// Fixed in trunk svn rev 6704

mdp
const STEPS_TO_GOAL = 3;

module Walker
	time: [0..STEPS_TO_GOAL] init 0;
	goal: bool init false;
	[tick] time<STEPS_TO_GOAL & !goal	->	(time'=time+1);
	[reach] time=STEPS_TO_GOAL & !goal	->	(goal'=true) & (time'=0);
	[doSelfLoop] goal	->	true;
endmodule

rewards "rew"
	!goal: 1;
endrewards
