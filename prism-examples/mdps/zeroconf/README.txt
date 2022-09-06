This case study concerns the IPv4 Zeroconf Protocol [CAG02]

We consider the probabilistic timed automata models presented in [KNPS06] using
the integer semantics also presented in the paper.

For more information, see: http://www.prismmodelchecker.org/casestudies/zeroconf.php

=====================================================================================

PARAMETERS
reset: reset is true/false dependent on whether the reset/norest model is to be analysed
loss: probability of message (0.1, 0.01, 0.001 and 0)
K: number of probes (4 in standard) 1:1:8
N: number of concrete hosts, e.g. 20 or 1000 for small/large network
err: error cost from 1e+6 to 1e+12
bound: time bound from 0:50 (then set T to be 1+maximum value of bound in experiment)

=====================================================================================

[CAG02]
S. Cheshire and B. Adoba and E. Gutterman
Dynamic configuration of {IPv}4 link local addresses
Available from http://www.ietf.org/rfc/rfc3927.txt

[KNPS06]
M. Kwiatkowska, G. Norman, D. Parker and J. Sproston
Performance Analysis of Probabilistic Timed Automata using Digital Clocks
Formal Methods in System Design, 29:33-78, 2006

