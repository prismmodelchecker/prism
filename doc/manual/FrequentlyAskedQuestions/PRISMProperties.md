# PRISM properties

Click on a question to see its answer.

<details id="inf_rewards">
<summary>Why is my expected reward infinite?</summary>

This most commonly occurs when you are computing the expected reward that is accumulated up until some target set of states is reached ("reachability reward" properties). For example:

```prism
R=? [ F "end" ]
```

As mentioned [earlier](../PropertySpecification/Reward-basedProperties.md), this kind of property returns infinity if `"end"` is not eventually reached with probability 1. This is a choice that we made when designing the property specification language. Often, it is reasonable to assume that, if a path continues indefinitely without reaching a goal state, then reward will continue to be accumulated infinitely often (this would usually be true when modelling time as a reward structure, for instance). If there is a non-zero probability of *not* reaching the target (i.e. the probability of reaching it is less than 1), we would then expect the overall expected reward to be infinite.

You can check whether the probability of reaching the target *is* 1 with a property like:

```prism
P=? [ F "end" ]
```

A similar situation arises with models that contain nondeterminism, such as MDPs. The *maximum* expected reward to reach a target is finite if and only if the *minimum* probability of reaching the target is 1. Conversely, the *minimum* expected reward is finite if and only if the *maximum* probability is 1.

</details>

<details id="mult_states">
<summary>How do I check if a property is true in multiple (or all) states?</summary>

Consider a typical boolean-valued PRISM property, such as:

```prism
P<0.01 [ F "error" ]
```

i.e. "the probability of reaching a state labelled with `"error"` is less than 0.01. By default, when model checking this query, PRISM will report the result of this property *for the initial state* of the model, i.e. whether, *starting from the initial state*, the probability of reaching `"error"` is below 0.01.

To check whether the above property is true for, say, all (reachable) states satisfying the label `"safe"`, you should use [filters](../PropertySpecification/Filters.md), as
illustrated below:

```prism
filter(forall, P<0.01 [ F "error" ], "safe")
```

If you want to check whether the property is true for *all* reachable states, you can use either of the following two (equivalent) properties:

```prism
filter(forall, P<0.01 [ F "error" ], true)
filter(forall, P<0.01 [ F "error" ])
```

Equivalently, you can use implication (`=>`) inside the filter, as in this example:

```prism
filter(forall, "safe" => P<0.01 [ F "error" ])
```

</details>

<details id="action">
<summary>How do I compute the probability of an action occurring?</summary>

PRISM's property specification language is primarily state-based, e.g. you can compute the probability of reaching a *state* that satisfies the label `"error"`:

```prism
P=? [ F "error" ]
```

So how do you compute the probability of a some action `b` occurring? You need to make a small change to your model. The cleanest way to do this is to add a small module that changes state when the action occurs, e.g.:

```prism
module checker

    q : [0..1] init 0;

    [b] q=0 -> (q'=1);
    [b] q=1 -> (q'=1);

endmodule
```

You can determine the probability of action `b` occurring in the model with the property:

```prism
P=? [ F q=1 ]
```

By design, the module above will not affect the behaviour (timing, probability, etc.) of your model at all, so all other properties will remain unchanged. This is true for any of the model types that PRISM supports. It may, though, lead to a (hopefully small) increase in total model size.

You can also modify the property above to compute, for example, the probability of `b` occurring within `T` time-units or the expected time until `b` occurs:

```prism
P=? [ F<=T q=1 ]
R{"time"}=? [ F q=1 ]
```

(where a constant `T` or reward structure `time` have been added to the model, as appropriate).

</details>
