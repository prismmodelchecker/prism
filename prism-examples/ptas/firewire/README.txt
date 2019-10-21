This case study concerns the Tree Identify Protocol of the IEEE 1394 High Performance Serial Bus (called ``FireWire'').

These are probabilistic timed automaton (PTA) models. You can also find, in the directory ../../firewire,
manually-created MDP models for this case study, built using the "digital clocks" semantics [KNPS06].

We consider the following probabilistic timed automata models of the root contention part of the 
tree identify protocol, which are based on probabilistic I/O automata models presented in [SV99].

impl: which consists of the parallel composition of two nodes (Node1 and Node2),
      and two communication channels (Wire12 for messages from Node1 to Node2,
      and Wire21 for messages from Node2 to Node1) and corresponds to the 
      system Impl of [SV99]. 

abst: which is represented by a single probabilistic timed automaton and is an abstraction of Impl 
      based on the the probabilistic I/O automaton I1 of [SV99].

For more information, see: http://www.prismmodelchecker.org/casestudies/firewire.php

=====================================================================================

[SV99]
M. Stoelinga and F. Vaandrager
Root Contention in IEEE 1394
In Proc.  5th AMAST Workshop on Real-Time and Probabilistic Systems (ARTS'99), pp. 53-74, 1999
(Available as Volume 1601 of LNCS, (c) Springer Verlag)

[KNPS06]
M. Kwiatkowska, G. Norman, D. Parker and J. Sproston
Performance Analysis of Probabilistic Timed Automata using Digital Clocks
Formal Methods in System Design, 29:33-78, 2006