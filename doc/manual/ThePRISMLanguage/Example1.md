# Example 1

We will use the following simple example to illustrate the basic concepts of the PRISM language.
Consider a system comprising two identical processes which must operate under mutual exclusion.
Each process can be in one of 3 states: {0,1,2}.
From state 0, a process will move to state 1 with probability 0.2
and remain in the same state with probability 0.8.
From state 1, it tries to move to the critical section: state 2.
This can only occur if the other process is not in its critical section.
Finally, from state 2, a process will either remain there or move back to state 0
with equal probability.
The PRISM code to describe an MDP model of this system can be seen below.
In the next sections, we explain each aspect of the code in turn.

```prism
// Example 1
// Two process mutual exclusion

mdp

module M1

    x : [0..2] init 0;

    [] x=0 -> 0.8:(x'=0) + 0.2:(x'=1);
    [] x=1 & y!=2 -> (x'=2);
    [] x=2 -> 0.5:(x'=2) + 0.5:(x'=0);

endmodule

module M2

    y : [0..2] init 0;

    [] y=0 -> 0.8:(y'=0) + 0.2:(y'=1);
    [] y=1 & x!=2 -> (y'=2);
    [] y=2 -> 0.5:(y'=2) + 0.5:(y'=0);

endmodule
```

**The PRISM Language: Example 1**
