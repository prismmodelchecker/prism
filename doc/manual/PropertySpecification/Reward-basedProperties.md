# Reward-based properties

PRISM models can be augmented with information about [rewards](../ThePRISMLanguage/CostsAndRewards.md) (or, equivalently, costs).
The tool can analyse properties which relate to the *expected values* of these rewards.
This is achieved using the `R` operator, which works in a similar fashion to the
`P` and `S` operators, and can be used either in a Boolean-valued query, e.g.:

```prism
R bound [ rewardprop ]
```

where `bound` takes the form `<r`, `<=r`, `>r` or `>=r` for an expression `r` evaluating to a double,
or a real-valued query, e.g.:

```prism
R query [ rewardprop ]
```

where `query` is `=?`, `min=?` or `max=?`.
In the latter case, [filters](Filters.md) can be used, as for the `P` and `S` operators.

Informally, "`R bound [ rewardprop ]`" is true in a state of a model if
"the expected reward associated with `rewardprop` of the model when starting from that state"
meets the bound `bound` and "`R query [ rewardprop ]`"  returns the actual expected reward value.

There are various different types of reward properties:

- "reachability reward": `F prop`
- "co-safe LTL reward": e.g. `F (prop1 & (F prop2))`
- "cumulative reward" : `C<=t`
- "total reward" : `C`
- "instantaneous reward" : `I=t`
- "steady-state reward" : `S`.

Below, we consider each of these cases in turn.
The descriptions here are kept relatively informal.
Precise definitions for most of these can be found in, for example,
[[KNP07a](../Main/References.md#KNP07a)] (for DTMCs and CTMCs) or [[FKNP11](../Main/References.md#FKNP11)] (for MDPs).

For all but instantaneous reward properties,
it is currently required that rewards are non-negative.

## "Reachability reward" properties

"Reachability reward" properties associate a reward with each path of a model.
More specifically, they refer to the reward accumulated along a path until a certain point is reached.
The manner in which rewards are accumulated depends on the model type.
For DTMCs and MDPs, the total reward for a path is the sum of the state rewards for each state along the path
plus the sum of the transition rewards for each transition between these states.
The situation for CTMCs is similar, except that the state reward assigned to each state
of the model is interpreted as the *rate* at which rewards are accumulated in that state,
i.e. if *t* time units are spent in a state with state reward *r*,
the reward accumulated in that state is *r* x *t*.
Hence, the total reward for a path in a CTMC is the sum of these products for each state along the path
plus the sum of the transition rewards for each transition between these states.

The reward property "`F prop`" corresponds to the reward cumulated along a path
until a state satisfying property `prop` is reached,
where rewards are cumulated as described above.
State rewards for the `prop`-satisfying state reached are not included in the cumulated value.
In the case where the probability of reaching a state satisfying `prop` is less than 1, the reward is equal to infinity.

A common application of this type of property is the case when the rewards associated with the model correspond to time.
One can then state, for example:

```prism
R<=9.5 [ F z=2 ]
```

which is true in a state *s* if "the expected time taken to reach, from *s*, a state where `z` equals 2 is less than or equal to 9.5".

## "Co-safe LTL reward" properties

These generalise the "reachability" properties above. Again, reward is accumulated along a path up until some point,
but this is specified in a more general way, by giving a formula in the co-safe fragment of linear temporal logic (LTL).
Rewards are accumulated up until the point where the formula is first satisfied. For example, this property, for a DTMC or CTMC,
queries the expected reward accumulated until first `goal` equals 1 and then subsequently `goal` equals 2:

```prism
R=? [ F (goal=1 & F goal=2) ]
```

and this property, for an MDP, minimises the expected reward until `loc` equals 1,
having passed only through states where `loc` never equals 4

```prism
Rmin=? [ loc!=4 U loc=1 ]
```

As for reachability rewards, if the probability of satisfying the formula is less than 1,
then the expected reward is defined to be infinite.

Intuitively, a *co-safe* formula is one that is satisfied within a finite period of time,
and remains true for ever once it becomes true for the first time.
For simplicity, PRISM actually supports the *syntactic* co-safe fragment of LTL,
which is defined as any LTL formula that only uses the temporal operators `F`, `U` and `X`
(but not `G`, for example).
PRISM's notation for LTL formulas is described [here](ThePOperator.md).

## "Cumulative reward" properties

"Cumulative reward" properties also associate a reward with each path of a model,
but only up to a given time bound.
The property `C<=t` corresponds to the reward cumulated along a path
until `t` time units have elapsed.
For DTMCs and MDPs, the bound `t` must evaluate to an integer;
for CTMCs, it can evaluate to double.
State and transition rewards along a path are cumulated exactly as described in the previous section.

A typical application of this type of property is the following.
Consider a model of a disk-drive controller which includes a queue of incoming disk requests.
If we assign a reward of 1 to each transition of the model
corresponding to the situation where an incoming request is lost because the queue is full,
then the property:

```prism
R=? [ C<=15.5 ]
```

would return, for a given state of the model,
"the expected number of lost requests within 15.5 time units of operation".

<a id="total"></a>

## "Total reward" properties

"Total reward" properties refer to the accumulation of state and transition rewards
in the same way as for "reachability reward" and "cumulative reward" properties,
but the rewards is accumulated indefinitely,
i.e. the total reward accumulated along the whole (infinite) path.
Note that this means that, unless a path ends up remaining forever in states with zero reward,
the total reward will be infinite.

Re-using the reward structure in the previous example,

```prism
R=? [ C ]
```

returns "the expected total number of lost requests".

## "Instantaneous reward" properties

"Instantaneous reward" properties refer to the reward of a model at a particular instant in time.
The reward property `I=t` associates with a path the reward in the state
of that path when exactly `t` time units have elapsed.
For DTMCs and MDPs, the bound `t` must evaluate to an integer;
for CTMCs, it can evaluate to double.

Returning to our example from the previous section of a model for a disk-request queue in a disk-drive controller,
consider the case where the rewards assigned to each state of the model give the current size of the queue in that state.
Then, the following property:

```prism
R<4.4 [ I=100 ]
```

would be true in a state *s* of the model if
"starting from *s*, the expected queue size exactly 100 time units later is less than 4.4".
Note that, for this type of reward property, state rewards for CTMCs do not have to refer to rates;
they can refer to any instantaneous measure of interest for a state.

## "Steady-state reward" properties

Unlike the previous three types of property,
"steady-state reward" properties relate not to paths, but rather to the reward in the long-run.
A typical application of this type of property would be, in the case where
the rewards associated with the model correspond to power consumption, the property:

```prism
R<=0.7 [ S ]
```

which is true in a state *s* if "starting from *s*, the long-run average power consumption is less than 0.7".

## Which reward structure?

In the case where a PRISM model has multiple [reward structures](../ThePRISMLanguage/CostsAndRewards.md) you may need to specify which reward structure your property refers to. This is done by placing the information in braces (`{}`) after the `R` operator. You can do so either using the name assigned to a reward structure (if any) or using the index (where `1` means the first rewards structure in the PRISM model file, `2` the second, etc.). Examples are:

```prism
R{"num_failures"}=? [ C<=10.0 ]
R{"time"}=? [ F step=final ]
R{2}=? [ F step=final ]
```

Note that when using an index to specify the reward structure, you can actually put any expression that evaluates to an integer. This allows you to, for example, write a property of the form `R{c}=?[...]` where `c` is an undefined integer constant. You can then vary the value of `c` in an experiment and compute values for several different reward structures at once.

If you don't specify a reward structure to the `R` operator, by default, the first one in the model file is used.

## Discounts

For the reward operators that refer to accumulated rewards,
i.e., `F prop`, `C<=t` and `C`,
rewards on DTMCs and MDPs can be *discounted* using a specified factor. Examples are:

```prism
Rmin=? [ F{discount=0.9} "goal" ]
R=? [ C{discount=0.5}<=100 ]
Rmax=? [ C{discount=0.9} ]
```
