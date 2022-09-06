dtmc

module m1
   s : [0..7] init 0;
   skip : [0..1] init 0;

   [] skip=1 & s<6 -> 1/2 : (skip'=1) + 1/2 : (skip'=0)&(s'=s+1);

   [] skip=0 & s<6 -> 1/2  : (s'=s+1) +   1/2  : (skip'=1);
   [] skip=0 & s=6 -> (s'=7);
   [] skip=0 & s=7 -> (s'=7);

endmodule
