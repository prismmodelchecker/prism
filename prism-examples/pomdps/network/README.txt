This case study concerns wireless downlink scheduling of traffic to a number
of different users with hard deadlines and where packets have priorities.

This is a POMDP model, described in [NPZ17], and based on [YMZ11].

Model variants:
* without priorities: network2.prism and network3.prism
* without priorities and no idling: network2_noidle.prism and network3_noidle.prism
* with priorities: network2_priorities.prism and network3_priorities.prism
* with priorities and no idling: network2_priorities_noidle.prism and network3_priorities_noidle.prism

Property files: network.props and network_priorities.props

=====================================================================================

PARAMETERS:

T: number of slots per time period
K: number of time periods

=====================================================================================

[NPZ17]
Gethin Norman, David Parker and Xueyi Zou
Verification and Control of Partially Observable Probabilistic Systems
Real-Time Systems, 53(3), pages 354-402, Springer, 2017

[YMZ11]
L. Yang, S. Murugesan and J. Zhang
Real-time scheduling over Markovian channels: When partial observability meets hard deadlines
In Proc. Global Telecommunications Conference (GLOBECOM’11), pp. 1–5, 2011
