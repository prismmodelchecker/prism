# Statistical model checking

The discrete-event simulator built into PRISM (see the section "[Debugging models with the simulator](DebuggingModelsWithTheSimulator.md)") can also be used to generate *approximate* results for PRISM properties, a technique often called *statistical model checking*. Essentially, this is achieved by *sampling*: generating a large number of random paths through the model, evaluating the result of the given properties on each run, and using this information to generate an approximately correct result. This approach is particularly useful on very large models when normal model checking is infeasible. This is because discrete-event simulation is performed using the PRISM language model description, without explicitly constructing the corresponding probabilistic model.

Currently, statistical model checking can only be applied to `P` or `R` operators
and does not support LTL-style path properties or [filters](../PropertySpecification/Filters.md).
There are also a few restrictions on the modelling language features that can be used; see below for details.

To use this functionality, load a model and some properties into PRISM, as described in the previous sections. To generate an approximate value for one or more properties, select them in the list, right-click and select "Simulate" (as opposed to "Verify"). As usual, it is first necessary to provide values for any undefined constants. Subsequently, a dialog appears. Here, the state from which approximate values are to be computed (i.e. from which the paths will be generated) can be selected. By default, this is the initial state of the model. The other settings in the dialog concern the methods used for simulation.

PRISM supports four different methods for performing statistical model checking:

- CI (Confidence Interval)
- ACI (Asymptotic Confidence Interval)
- APMC (Approximate Probabilistic Model Checking)
- SPRT (Sequential Probability Ratio Test)

The first three of these are intended primarily for [numerical](../PropertySpecification/ThePOperator.md#numerical) properties (e.g. of the form `P=?[...]`), but can also be used for Boolean properties with a bound (e.g. of the form `P<p[...]`). The SPRT method is only applicable to Boolean properties.

Each method has several parameters that control its execution, i.e. the number of samples that are generated and the accuracy of the computed approximation. In most cases, these parameters are inter-related so one of them must be left unspecified and its value computed automatically based on the others. In some cases, this is done before simulation; in others, it must be done afterwards.

Below, we describe each method in more detail.
For simplicity, we describe the case of checking a `P` operator.
Details for the case of an `R` operator can be found in [[Nim10](../Reference/References.md#Nim10)].

## CI (Confidence Interval) Method

The CI method gives a *confidence interval* for the approximate value generated for a `P=?` property, based on a given *confidence level* and the number of samples generated.
The parameters of the method are:

- "Width" ($w$)
- "Confidence" ($\alpha$)
- "Number of samples" ($N$)

Let $X$ denote the true result of the query `P=?[...]` and $Y$ the approximation generated.
The confidence interval is $[Y-w, Y+w]$, i.e. $w$ gives the half-width of the interval.
The confidence level, which is usually stated as a percentage, is $100(1-\alpha)\%$.
This means that the actual value $X$ should fall into the confidence interval $[Y-w, Y+w]$ $100(1-\alpha)\%$ of the time.

To determine, for example, the width $w$ for given $\alpha$ and $N$, we use

$$w = q \sqrt{v / N}$$

where $q$ is a quantile, for probability $1-\alpha/2$, from the Student's t-distribution with $N-1$ degrees of freedom and $v$ is (an estimation of) the variance for $X$.
Similarly, we can determine the required number of iterations, from $w$ and $\alpha$, as

$$N = \frac{v q^2}{w^2}$$

where $q$ and $v$ are as before.

For a bounded property  `P~p[...]`, the (Boolean) result is determined according to the generated approximation for the probability. This is not the case, however, if the threshold $p$ falls within the confidence interval $[Y-w, Y+w]$, in which case no value is returned.

## ACI (Asymptotic Confidence Interval) Method

The ACI method works in exactly same fashion as the CI method, except that it uses the Normal distribution to approximate the Student's t-distribution when determining the confidence interval. This is appropriate when the number of samples is large (because we can get a reliable estimation of the variance from the samples) but may be less accurate for small numbers of samples.

## APMC (Approximate Probabilistic Model Checking) Method

The APMC method, based on [[HLMP04](../Reference/References.md#HLMP04)], offers a probabilistic guarantee on the accuracy of the  approximate value generated for a `P=?` property, based on the Chernoff-Hoeffding bound.
The parameters of the method are:

- "Approximation" ($\varepsilon$)
- "Confidence" ($\delta$)
- "Number of samples" ($N$)

Letting $X$ denote the true result of the query `P=?[...]` and $Y$ the approximation generated, we have:

$$\mathrm{Prob}(|Y-X| > \varepsilon) < \delta$$

where the parameters are related as follows:

$$N = \frac{\ln(2/\delta)}{2\varepsilon^2}$$

This imposes certain restrictions on the parameters,
namely that $N \varepsilon^2 \ge \ln(2/\delta)/2$.

In similar fashion to the CI/ACI methods, the APMC method can be also be used for bounded properties such as `P~p[...]`, as long as the threshold $p$ falls outside the interval $[Y-\varepsilon, Y+\varepsilon]$.

## SPRT (Sequential Probability Ratio Test) Method

The SPRT method is specifically for bounded properties, such as `P~p[...]` and is based on *acceptance sampling* techniques [[YS02](../Reference/References.md#YS02)]. It uses Wald's sequential probability ratio test (SPRT), which generates a succession of samples, deciding on-the-fly when an answer can be given with a sufficiently high confidence.

The parameters of the method are:

- "Indifference" ($\delta$)
- "Type I/II error" ($\alpha$/$\beta$)

Consider a property of the form `P>=p[...]`. The parameter $\delta$ is used as the half-width of an *indifference region* $[p-\delta, p+\delta]$. PRISM will attempt to determine whether either the hypothesis `P>=(p+delta)[...]` or `P<=(p-delta)[...]` is true, based on which it will return either `true` or `false`, respectively. The parameters $\alpha$ and $\beta$ represent the probability of the occurrence of a *type I error* (wrongly accepting the first hypothesis) and a *type II error* (wrongly accepting the second hypothesis), respectively. For simplicity, PRISM assigns the same value to both $\alpha$ and $\beta$.

## Maximum Path Length

Another setting that can be configured from the "Simulation Parameters" dialog is the maximum length of paths generated by PRISM during statistical model checking. In order to perform statistical model checking, PRISM needs to evaluate the property being checked along every generated path. For example, when checking `P=? [ F<=10 "end" ]`, PRISM must check whether `F<=10 "end"` is true for each path. On this example (assuming a discrete-time model), this can always be done within the first 10 steps. For a property such as `P=? [ F "end" ]`, however, there may be paths along which no finite fragment can show `F "end"` to be true or false. So, PRISM imposes a maximum path length to avoid the need to generate excessively long (or infinite) paths.
The default maximum length is 10,000 steps.
If, for a given property, statistical model checking results in one or more paths on which the property cannot be evaluated, an error is reported.

## Command-line Statistical Model Checking

Statistical model checking can also be enabled from the command-line version of PRISM, by including the `-sim` switch. The default methods used are CI (Confidence Interval) for numerical properties and SPRT (Sequential Probability Ratio Test) for Boolean properties. To select a particular method, use switch `-simmethod <method>` where `<method>` is one of `ci`, `aci`, `apmc` and `sprt`. For example:

```shell
prism model.pm model.pctl -prop 1 -sim -simmethod aci
```

PRISM has default values for the various simulation method parameters, but these can also be specified using the switches `-simsamples`, `-simconf`, `-simwidth` and `-simapprox`. The exact meaning of these switches for each simulation method is given in the table below.

| Switch | CI | ACI | APMC | SPRT |
| --- | --- | --- | --- | --- |
| `-simsamples` | "Num. samples" | "Num. samples" | "Num. samples" | n/a |
| `-simconf` | "Confidence" | "Confidence" | "Confidence" | "Type I/II error" |
| `-simwidth` | "Width" | "Width" | n/a | "Indifference" |
| `-simapprox` | n/a | n/a | "Approximation" | n/a |

The maximum length of simulation paths is set with switch `-simpathlen`.

## Limitations

Currently, the simulator does not support every part of the PRISM modelling languages. For example, it does not handle models with multiple initial states or with `system...endsystem` definitions.

It is also worth pointing out that statistical model checking techniques are not well suited to models that exhibit nondeterminism, such as MDPs. This because the techniques rely on generation of *random* paths, which are not well defined for a MDP. PRISM does allow statistical model checking to be performed on an MDP, but does so by simply resolving nondeterministic choices in a (uniformly) random fashion (and displaying a warning message). Currently PTAs are not supported by the simulator.
