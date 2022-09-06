//Project 3, Question 1

dtmc

	const double a = 0.01;
	const double b = 0.05;

module chan1

	x : [0..1];

	[] x=0 -> a:(x'=1) + 1-a:(x'=0);
	[] x=1 -> b:(x'=0) + 1-b:(x'=1);

endmodule

init x=0 | x=1 endinit
