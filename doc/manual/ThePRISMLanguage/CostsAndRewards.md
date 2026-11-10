# Costs and rewards

PRISM supports the specification and analysis of
properties based on **costs** and **rewards**.
This means that it can be used to reason,
not just about the probability that a model behaves in a certain fashion,
but about a wider range of quantitative measures relating to model behaviour.
For example, PRISM can be used to compute properties such as
"expected time", "expected number of lost messages" or "expected energy consumption".

To achieve this, PRISM allows states and transitions to be annotated with numerical values,
representing for example the time taken for a transition or its energy consumption.
Although these examples are all "costs" (which we would intuitively like to minimise),
following classical definitions of MDPs in the literature,
PRISM consistently refers to these values as **rewards**.
But in practice they equally well represent costs.
This section describes how to specify rewards in a PRISM model.
We later describe [reward-based property specifications](../PropertySpecification/Reward-basedProperties.md).

Rewards are associated with models using `rewards ... endrewards` constructs,
which can appear anywhere in a model file except within a module definition.
These constructs contain one or more **reward items**.
Consider the following simple example:

```prism
rewards "r1"
    true : 1;
endrewards
```

This defines a reward structure with name `r1` (the name is optional)
which assigns a reward of 1 to every state of the model.
It comprises a single reward item, the left part of which (`true`) is a guard
and the right part of which (`1`) is a reward.
States of the model which satisfy the predicate in the guard are assigned the corresponding reward.
More generally, state rewards can be specified using multiple reward items,
each of the form `guard : reward;`,
where `guard` is a predicate (over all the variables of the model)
and `reward` is an expression (containing any variables, constants, etc. from the model).
For example:

```prism
rewards "r2"
    x=0 : 100;
    x>0 & x<10 : 2*x;
    x=10 : 100;
endrewards
```

assigns a reward of 100 to states satisfying `x=0` or `x=10`
and a reward of `2*x` to states satisfying `x>0 & x<10`.
Note that a single reward item can assign different rewards to different states,
depending on the values of model variables in each one.
Any states which do not satisfy the guard of any reward item will have no reward assigned to them.
For states which satisfy multiple guards, the reward assigned to the state
is the sum of the rewards for all the corresponding reward items.

Rewards can also be assigned to transitions of a model.
These are specified in a similar fashion to state rewards,
within the `rewards ... endrewards` construct.
Reward items describing transition rewards are of the form `[action] guard : reward;`,
the interpretation being that transitions from states which satisfy the guard `guard`
and are labelled with the action `action` acquire the reward `reward`.
For example:

```prism
rewards "r3"
    [] true : 1;
    [a] true : x;
    [b] true : 2*x;
endrewards
```

assigns a reward of 1 to all transitions in the model with no action label,
and rewards of `x` and `2*x` to all transitions labelled with actions `a` and `b`, respectively.

As is the case for states, multiple reward items can specify rewards for a single transition,
in which case the resulting reward is the sum of all the individual rewards.
A model description can specify rewards for both states and transitions.
These are all placed together in a single `rewards...endrewards` construct.

A PRISM model will often have multiple reward structures, for example:

```prism
rewards "total_time"
    true : 1;
endrewards

rewards "num_failures"
    [fail] true : 1;
endrewards
```

The first assigns a reward of 1 to every state, which, for a [CTMC](ContinuousTime.md), accumulates as time passes;
the second counts transitions labelled `fail`, as in the [repair example](ContinuousTime.md#example).
A property can then [refer to a reward structure](../PropertySpecification/Reward-basedProperties.md#reward-structure) by its name,
e.g., `R{"num_failures"}=? [ C<=10 ]` gives the expected number of failures up to time 10.
