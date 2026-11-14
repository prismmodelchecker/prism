# Simulation

PRISM includes a *simulator*, a tool which can be used to generate sample paths (executions) through a PRISM model. From the GUI, the simulator allows you to explore a model by interactively generating such paths. This is particularly useful for debugging models during development and for running sanity checks on completed models. Paths can also be generated from the command-line.

<a id="gui"></a>
## Generating a path in the GUI

Once you have loaded a model into the PRISM GUI
(note that it is not necessary to build the model),
select the "Simulator" tab at the bottom of the main window.
You can now start a new path by double-clicking in the bottom half of the window
(or right-clicking and selecting "New path").
If there are undefined constants in the
model (or in any currently loaded properties files) you will be prompted to give values for these. You
can also specify the state from which you wish to generate a path. By default, this is the initial state of
the model.

The main portion of the user interface (the bottom part) displays a path through the currently loaded model. Initially, this will comprise just a single state. The table above shows the list of available transitions from this state. Double-click one of these to extend the path with this transition. The process can be repeated to extend the path in an interactive fashion. Clicking on any state in the current path shows the transition which was taken at this stage. Click on the final state in the path to continue
extending the path. Alternatively, clicking the "Simulate" button will select a transition randomly (according to the probabilities/rates of the available transitions). By changing the number in the box below this button, you can easily generate random paths of a given length with a single click.
There are also options (in the accompanying drop-down menu) to allow generation of paths up until a particular length or, for CTMCs, in terms of the time taken.

The figure shows the simulator in action.

![Screenshot of the Simulator tab: controls for automatic and manual exploration, backtracking and strategies at the top, a list of state labels on the right, and below a table of the path so far, one row per step showing the action taken, the time and the value of each variable](../files/gui-sim.png)
*The PRISM GUI: exploring a model using the simulator*

It is also possible to:

- backtrack to an earlier point in a path
- remove all of the states before some point in a path
- restart a path from its first state
- export a path to a text file

Notice that the table containing the path displays not just the value of each variable in each
state but also the time spent in that state and any rewards accumulated there. You can configure exactly which columns appear by right-clicking on the path and selecting "Configure view". For rewards (and for CTMC models, for the time-values), you can opt to display the reward/time for each individual state and/or the cumulative total up until each point in the path.

At the top-right of the interface, any labels contained in the currently loaded model/properties file are displayed, along with their value in the currently selected state of the path. In addition, the built-in [labels](../PropertySpecification/PropertiesFiles.md) `"init"` and `"deadlock"` are also included. Selecting a label from the list highlights all states in the current path which satisfy it.

The other tabs in this panel allow the value of path operators (taken from properties in the current file) to be viewed for the current path, as well as various other statistics.

Another very useful feature for some models is to use the "Plot new path" option from the simulator, which generates a plot of some/all of the variable/reward values for a particular randomly generated path through the model.

<a id="cl"></a>
## Path generation from the command-line

It is also possible to generate random paths through a model using the command-line version of PRISM. This is achieved using the `-simpath` switch, which requires two arguments, the first describing the path to be generated and the second specifying the file to which the path should be output (as usual, specifying `stdout` sends output to the terminal). The following examples illustrate the various ways of generating paths in this way:

```shell
prism model.prism -simpath 10 path.txt
prism model.prism -simpath time=7.5 path.txt
prism model.prism -simpath deadlock path.txt
```

These generate a path of 10 steps, a path of at least 7.5 time units and a path ending in deadlock, respectively.

Here's an example of the output:

```shell
prism poll2.prism -simpath 10 stdout
#...
#action step time s a s1 s2
#- 0 0.0 1 0 0 0
#[loop1a] 1 0.007479539729154247 2 0 0 0
#[loop2a] 2 0.00782819795294666 1 0 0 0
#[loop1a] 3 0.01570585559933703 2 0 0 0
#[loop2a] 4 0.017061111948220263 1 0 0 0
#[loop1a] 5 0.026816317516034468 2 0 0 0
#[loop2a] 6 0.039878416276337814 1 0 0 0
#[loop1a] 7 0.04456566315999103 2 0 0 0
#[loop2a] 8 0.047368359683643765 1 0 0 0
#[loop1a] 9 0.04934857366557349 2 0 0 0
#[loop2a] 10 0.055031679365844674 1 0 0 0
```

[`poll2.prism`](../files/poll2.prism) is a CTMC model of a simple polling system (also shown, with a diagram, in "[Explicit model files](../Reference/ExplicitModelFiles.md#poll2)").

This shows the sequence of states in the path, i.e. the values of the variables in each state. In the example above, there are 4 variables: `s`, `a`, `s1` and `s2`.
The first three columns show the type of transition taken to reach that state, its index within the path (starting from 0) and the time at which it was entered. The latter is only shown for continuous time models. The type of the transition is written as *[act]* if action label `act` was taken, and as *module1* if the module named `module1` takes an unlabelled transition).

Further options can also be appended to the first parameter. For example, option `probs=true` also displays the probability/rate associated with each transition. For example:

```shell
prism poll2.prism -simpath '5,probs=true' stdout
#...
#action probability step time s a s1 s2
#- - 0 0.0 1 0 0 0
#[loop1a] 200.0 1 0.0011880118081395378 2 0 0 0
#[loop2a] 200.0 2 0.0037798355025401888 1 0 0 0
#[loop1a] 200.0 3 0.01029212322894221 2 0 0 0
#[loop2a] 200.0 4 0.023258883912578403 1 0 0 0
#[loop1a] 200.0 5 0.027402404026254504 2 0 0 0
```

In this example, the rate is 200.0 for all transitions.
To show the state/transition rewards for each step, use option `rewards=true`.

If you are only interested in values of certain variables of your model, use the `vars=(...)` option. For example:

```shell
prism poll2.prism -simpath '500,probs=true,vars=(a,s1,s2)' stdout
#...
#action probability step time a s1 s2
#- - 0 0.0 0 0 0
#station2 0.5 110 0.5025332771499665 0 0 1
#[loop2b] 200.0 111 0.5109407735244359 1 0 1
#[serve2] 1.0 112 0.9960642154887506 0 0 0
#station1 0.5 130 1.0645858553472822 0 1 0
#[loop1b] 200.0 132 1.0732572896618477 1 1 0
#[serve1] 1.0 133 2.939742026148121 0 0 0
#station2 0.5 225 3.4311507854807677 0 0 1
#[loop2b] 200.0 227 3.434285492243098 1 0 1
#[serve2] 1.0 228 3.553118276800078 0 0 0
#station2 0.5 250 3.6354431222941406 0 0 1
#[loop2b] 200.0 251 3.637552738997181 1 0 1
#[serve2] 1.0 252 3.7343375346150576 0 0 0
```

Note the use of single quotes around the path description argument to prevent the shell from misinterpreting special characters such as "`(`".

Notice also that the above only displays states in which the values of some variable of interest changes. This is achieved with the option `changes=true`, which is automatically enabled when you use `vars=(...)`. If you want to see all steps of the path, add the option `changes=false`.

An alternative way of viewing paths is to only display paths at certain fixed points in time. This is achieved with the `snapshot=x` option, where `x` is the time step. For example:

```shell
prism poll2.prism -simpath 'time=5.0,snapshot=0.5' stdout
#...
#step time s a s1 s2
#0 0.0 1 0 0 0
#94 0.5 1 0 0 0
#198 1.0 1 0 0 0
#314 1.5 1 0 0 0
#375 2.0 1 1 1 1
#376 2.5 2 0 0 1
#376 3.0 2 0 0 1
#378 3.5 1 0 0 0
#378 4.0 1 0 0 0
#478 4.5 1 0 0 0
#511 5.0 2 0 0 0
```

You can also use the `sep=...` option to specify the column separator. Possible values are `space` (the default), `tab` and `comma`. For example:

```shell
prism poll2.prism -simpath '200,vars=(a,s1),sep=comma' stdout
#...
#action,step,time,a,s1
#-,0,0.0,0,0
#[loop2b],71,0.30227293267212296,1,0
#station1,72,0.7818965999769389,1,1
#[serve2],73,1.132620216421162,0,1
#[loop1b],74,1.1350805223051847,1,1
#[serve1],76,5.305834484838731,0,0
#...
```

When generating paths to a deadlock state, additional `repeat=...` option is available which will construct multiple paths until a deadlock is found. For example:

```shell
prism model.prism -simpath 'deadlock,repeat=100' stdout
```

By default, the simulator detects deterministic loops in paths (e.g. if a path reaches a state from which there is a just a single self-loop leaving that state) and stops generating the path any further. You can disable this behaviour with the `loopcheck=false` option. For example:

```shell
prism dice.prism -simpath 10 stdout
#...
#Warning: Deterministic loop detected after 6 steps (use loopcheck=false option to extend path).
#action step s d
#- 0 0 0
#die 1 1 0
#die 2 4 0
#die 3 7 3
#die 4 7 3
```

[`dice.prism`](../files/dice.prism) is a DTMC model of Knuth's algorithm for simulating a die with a coin.

```shell
prism dice.prism -simpath 10,loopcheck=false stdout
#...
#action step s d
#- 0 0 0
#die 1 1 0
#die 2 4 0
#die 3 7 2
#die 4 7 2
#die 5 7 2
#die 6 7 2
#die 7 7 2
#die 8 7 2
#die 9 7 2
#die 10 7 2
```

One final note: the `-simpath` switch only generates paths up to the maximum path length setting of the simulator (the default is 10,000). If you want to generate longer paths, either change the
[default setting](../ConfiguringPRISM/SettingsFile.md) or override it temporarily from the command-line using the `-simpathlen` switch.
You might also use the latter to decrease the setting,
e.g. to look for a path leading to a deadlock state,
but only within 100 steps:

```shell
prism model.prism -simpath deadlock stdout -simpathlen 100
```
