dtmc

const double p = 1e-13;

module rare
  rare : bool init false;

  [tick] !rare -> p:   (rare' = true)
                + 1-p: true;
  [tick] rare  -> true;
endmodule

rewards
  rare:  1-p;
  !rare: p;
//  [tick] rare:  1-p;
//  [tick] !rare: p; // omitted because p^2 is too small and disappears
endrewards
