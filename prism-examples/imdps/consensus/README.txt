This case study is based on the shared coin protocol from the
randomised consensus algorithm of Aspnes and Herlihy [AH90].

The models here are interval MDPs which consider the effect of unreliable or
adversarial behaviour in the form of a biased coin instead of a fair coin [PLSS13].

These extend the MDP version, the files for which can be found in ../../mdps/consensus.

=====================================================================================

PARAMETERS:

K: parameter of the random walk in the coin protocol
bias: half-width of the interval for the coin toss probability

=====================================================================================

[AH90]
J. Aspnes and M. Herlihy.
Fast randomized consensus using shared memory.
Journal of Algorithms, 15(1):441-460, 1990.

[PLSS13]
A. Puggelli, W. Li, A. L. Sangiovanni-Vincentelli and S. A. Seshia
Polynomial-Time Verification of PCTL Properties of MDPs with Convex Uncertainties
In Proc. 25th International Conference on Computer Aided Verification (CAV'13), 2013.
