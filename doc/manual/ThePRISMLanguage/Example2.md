# Example 2

We now introduce a second example: a CTMC that models an *N*-place queue of jobs and
a server which removes jobs from the queue and processes them.
The PRISM code is as follows:

```prism
// Example 2
// N-place queue + server

ctmc

const int N = 10;
const double mu = 1/10;
const double lambda = 1/2;
const double gamma = 1/3;

module queue
     q : [0..N];

     [] q<N -> mu:(q'=q+1);
     [] q=N -> mu:(q'=q);
     [serve] q>0 -> lambda:(q'=q-1);
endmodule

module server
     s : [0..1];

     [serve] s=0 -> 1:(s'=1);
     [] s=1 -> gamma:(s'=0);
endmodule
```

**The PRISM Language: Example 2**

This example also introduces a number of other PRISM language concepts,
including [constants](Constants.md), action labels and [synchronisation](Synchronisation.md).
These are described in the following sections.
