// Question 3 of Applied Concurrency Theory Project 3

dtmc

const double a = 0.01;
const double b = 0.05;

module Generator

  x : [0..1];

[] true -> 0.5:(x'=0) + 0.5:(x'=1);

endmodule


module Line1

  y : [0..1];
  error_1 : bool;
  error_last_1  : bool;

  [] x=0 & error_1 -> (1-a):(y'=0) & (error_1'=false) & (error_last_1'=true)
          + a:(y'=1) & (error_1'=true) & (error_last_1'=true);
  [] x=0 & !error_1 -> (1-a):(y'=0) & (error_1'=false) & (error_last_1'=false)
          + a:(y'=1) & (error_1'=true) & (error_last_1'=false);
  [] x=1 & error_1 -> (1-b):(y'=1) & (error_1'=false) & (error_last_1'=true)
          + b:(y'=0) & (error_1'=true) & (error_last_1'=true);
  [] x=1 & !error_1 -> (1-b):(y'=1) & (error_1'=false) & (error_last_1'=false)
          + b:(y'=0) & (error_1'=true) & (error_last_1'=false);

endmodule


module Line2

  z : [0..1];
  error_2 : bool;

  [] y=0 -> (1-a):(z'=0) & (error_2'=false)
          + a:(z'=1) & (error_2'=true);
  [] y=1 -> (1-b):(z'=1) & (error_2'=false)
          + b:(z'=0) & (error_2'=true);

endmodule


init (x=0) & (error_1=false) & (error_last_1=false) & (error_2=false)  endinit
