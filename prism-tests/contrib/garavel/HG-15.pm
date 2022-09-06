
dtmc

const double a = 0.01;
const double b = 0.05;

init
        (x=0)
endinit

module generator
x : [0..1];

[] x=0 | x=1 -> 1/2: (x'=0)
		+ 1/2: (x'=1);
endmodule

module line1
y : [0..1];

[] x=0 -> 1-a: (y'=0)
        + a: (y'=1);
[] x=1 -> 1-b: (y'=1)
        + b: (y'=0);
endmodule

module line2
z : [0..1];

[] y=0 -> 1-a: (z'=0)
        + a: (z'=1);
[] y=1 -> 1-b: (z'=1)
        + b: (z'=0);
endmodule
