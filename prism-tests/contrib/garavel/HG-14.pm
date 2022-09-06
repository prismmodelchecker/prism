dtmc

const double a = 0.01;
const double b = 0.05;

init
	s=0 | s=1
endinit

module chan
s : [0..1];

[] s=0 -> 1-a: (s'=0)
	+ a: (s'=1);
[] s=1 -> 1-b: (s'=1)
	+ b: (s'=0);
endmodule
