This case study concerns the NRL (Naval Research Laboratory) pump,
which is designed to provide reliable and secure communication over networks of
nodes with 'high' and 'low' security levels, preventing the channel leaking
information through the timing of messages and acknowledgements. 

It is modelled as a partially observable probabilistic timed automaton (POPTA),
as described in [NPZ17]. This extends the PTA model of [LMS+14].
The design of the NRL pump itself is presented in [KMM98].

=====================================================================================

PARAMETERS:

h0: delay added when high sends a 0
h1: delay added when high sends a 1
N: number of messages low can send before guessing

=====================================================================================

[KMM98]
M. Kang, A. Moore and I. Moskowitz
Design and assurance strategy for the NRL pump
Computer 31(4), 56–64,1998

[LMS+14]
R. Lanotte, A. Maggiolo-Schettini, S. Tini, A. Troina and E. Tronci
Automatic analysis of the NRL pump
ENTCS, 99:245–266, 2014

[NPZ17]
Gethin Norman, David Parker and Xueyi Zou
Verification and Control of Partially Observable Probabilistic Systems
Real-Time Systems, 53(3), pages 354-402, Springer, 2017
