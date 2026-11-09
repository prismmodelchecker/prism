# Parametric model checking

Often, PRISM models contain [constants](../ThePRISMLanguage/Constants.md), representing parameters of the system being modelled, which define for example the transition probabilities in the model. In order to perform model checking, these constants have to be assigned concrete values. PRISM also allows [experiments](Experiments.md), where model checking is performed for a range of different values for the constants.

PRISM's *parametric model checking* [[HHZ11b](../Main/References.md#HHZ11b)],[[HHZ11](../Main/References.md#HHZ11)] functionality, however, provides a more powerful method for analysing probabilistic models whose transition probabilities are specified as functions over a set of parameters. Depending on the property under consideration, the result is then given as either a rational function over the parameters or as a mapping from regions of these parameters to rational functions or truth values. This function (or functions) can then be used to, for example:

- plot a graph showing how the parameter affects the result of the property; or

- use optimisation methods to find parameter values that minimise or maximise the result.

PRISM's implementation of parametric model checking [[CHH+13](../Main/References.md#CHH+13)] re-implements the techniques previously included in the [PARAM](http://depend.cs.uni-sb.de/tools/param/) tool.

Parameters are specified as undefined constants in the model file, e.g.:

```prism
const double x;
```

These parameters can only be used to describe probabilities (or rates). For example:

```prism
[] s=0 -> x : (s'=1) + 1-x : (s'=2);
```

They may not be used in guards or updates. The parametric definitions of probabilities or rates (e.g. `x` and `1-x` in the above) must be rational functions (fractions of polynomials). PRISM currently supports parametric versions of discrete-time Markov chains (DTMCs), continuous-time Markov chains (CTMCs) and Markov decision processes (MDPs). The classes of properties that be checked on these models are as follows:

- parametric DTMCs/CTMCs: unbounded until, steady-state probabilities, reachability reward and steady-state reward;

- parametric MDPs: unbounded until and reachability rewards.

Currently, parametric model checking can only be performed from the command-line. This is done by using the switch `-param <vals>`, where `<vals>` lists the undefined constants that should be treated as parameters. A range of possible values should also be provided for each parameter, in the form `<parameter>=<lower-bound>:<upper-bound>`. For example:

```shell
prism model.pm model.props -param x=0.2:0.4,y=-2:2
```

would specify a parameter `x` with lower bound 0.2 and upper bound 0.4, and a parameter `y` with values between -2 and 2. You can also omit the bounds for a parameter, in which case it will be assumed to have range 0 to 1.

The result of parametric model checking will be a mapping from regions (subsets of parameter valuations) to functions over the parameters. The regions are given as hyper-rectangles, e.g. "[ [0.2,0.3],[-2,0] ]" would represent the region of parameter valuations in which the first parameter is between 0.2 and 0.3 and the second is between -2 and 0. The results obtained are exact, that is no rounding errors are made during computation. Here is an example of the output of model checking:

```shell
prism model.pm model.props -param x=0:1
#...
#Result: ([0.0,1.0]): { 2 x - 5  | 8 x - 12  }
```

which indicates that, for the full range ([0,1]) of the parameter `x`, the result of model checking is the expression `(2x-5)/(8x-12)`.

Parametric model checking can be configured with the following options:

- `-paramprecision <x>`: PRISM uses regions in the form of hyper-rectangles to subsume parameter valuations with the same rational function or truth value. Because it is not always possible to cover the whole parameter space with hyper-rectangles, this option can be used to specify a *precision*, that is, an amount of the parameter space which may remain undecided. The default is 5/100.

- `-paramsplit <name>`: During model checking, undecided regions may have to be split into several parts, because there might not exist a single rational function or truth value to which all parameter valuations of the original regions can be mapped. When a region is split, it can either be split at only its longest side (`<name>=longest`) or at all sides at once (`<name>=all`). The default is `longest`.

- `-parambisim <name>`: The parametric analysis is costly in terms of time and memory, so it can help to perform bisimulation minimisation to speed up the analysis and use less memory. The possible options here are to use weak bisimulation (`<name>=weak`), strong bisimulation (`<name>=strong`) or none at all (`<name>=none`). The default is `weak`. In case an analysis is to be performed for which the current bisimulation type does not maintain validity of the results (e.g. weak bisimulation and a steady state analysis), an appropriate bisimulation engine is chosen automatically.

- `-paramfunction <name>`: Sets the way rational functions are represented. Currently, only [Java Algebra System (JAS)](http://krum.rz.uni-mannheim.de/jas/) is supported. The options are to use JAS directly (`<name>=jas`) or to use a version in which results of some previous mathematical operations performed during the analysis are cached (`<name>=jas-cached`). This can speed up computation, but also needs more memory. The default is to use the cached version.

- `-paramelimorder <name>`: In the parametric engine, computations are performed by "eliminating" one state after the other, that is completely treating a state together with its incoming and outgoing transitions, rather than performing iterative methods. This option sets the order in which states are eliminated. The values currently available are: arbitrary order (`<name>=arbitrary`), forward starting from the initial states (`<name>=forward`), reversed forward order (`<name>=forward-reversed`), starting with target/unsafe states and then going backward (`<name>=backward`), the reverse of this order (`<name>=backward-reversed`) or random order (`<name>=random`). The default is the backward order.

- `-paramrandompoints <n>`: Under some conditions, it has to be decided whether certain properties hold for all parameter valuations of a given region. This is the case for instance when computing truth values of properties for parametric DTMCs, CTMCs and MDPs, and also when computing any value for MDPs. At the moment, the truth values are only computed approximately, by evaluating and checking values at the edges of regions (as they are hyper-rectangles) and some random points. The exact number of random points to use is given with this option. The default is 5.

- `-paramsubsumeregions <b>`: During the parametric analysis, quite a number of different regions might be created, which have to be stored in memory. If this option is used (`<b>=true`), PRISM tries to subsume neighbouring regions with the same value. The default is to use it.
