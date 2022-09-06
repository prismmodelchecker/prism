dtmc

const double a = 0.01;
const double b = 0.05;

module generator
    x : [0..1] init 0;
    i : bool init false;

    [] x=0 -> 0.5 : (x'=0) & (i'=true) + 0.5 : (x'=1) & (i'=true);
    [] x=1 -> 0.5 : (x'=0) & (i'=true) + 0.5 : (x'=1) & (i'=true);
endmodule

module line1
    
    y : [0..1];
    e1 : bool init false;
    ee1 : bool init false;

    [] x=0 -> 1-a : (y'=0) & (e1'=false) & (ee1'=e1) + a : (y'=1) & (e1'=true) & (ee1'=e1);
    [] x=1 -> 1-b : (y'=1) & (e1'=false) & (ee1'=e1) + b : (y'=0) & (e1'=true) & (ee1'=e1);

endmodule

module line2
    
    z : [0..1];
    e2 : bool init false;

    [] y=0 & i=true -> 1-a : (z'=0) & (e2'=false) + a : (z'=1) & (e2'=true);
    [] y=1 & i=true -> 1-b : (z'=1) & (e2'=false) + b : (z'=0) & (e2'=true);

endmodule
