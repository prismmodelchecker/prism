# Strategies

Properties to be model checked on MDPs (and their variants, such as POMDPs or IMDPs) usually quantify over **strategies** (or **policies**) of the model,
i.e., over the different possible ways that nondeterminism can be resolved in the model.
For example, this property:

```prism
Pmax=? [ F "goal" ]
```

determines the maximum probability, over all strategies, of reaching a state satisfying the label `"goal"`.
When checking such properties, you can also ask PRISM to generate a corresponding (optimal) strategy,
which yields this maximum probability when followed. The strategy can then be viewed, exported or simulated.

**Note:** PRISM uses the term *strategy* for all model types (other common names are *policy*, *scheduler* and *adversary*).
Strategy generation is implemented in the ["explicit"](../ConfiguringPRISM/ComputationEngines.md) model checking engine,
which is used automatically if strategy generation is requested.
The "sparse" engine has a more limited *adversary generation* feature (see below).

## Generating strategies

Optimal strategies can be generated either from the command-line or the graphical user interface (GUI). For the former, use the `-exportstrat` switch. Simple examples are:

```shell
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat stdout
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.pexp
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.umb
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.dot
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.tra
```

The first of these exports actions, the others export induced models (see below).

From the GUI, you can trigger strategy generation by ticking the "Generate strategy" box either on the popup menu that appears when you right-click a property, or from the "Strategies" menu at the top. As long as it is supported, a strategy will then be generated once "Verify" is clicked.

From the same menu(s), you can then

- export the strategy to a file
- view the strategy by printing it in the log
- explore the strategy in the simulator

## Export formats

Strategies can be viewed or exported in several different formats:

### Action list

An action list gives the action taken in each state of the model, e.g.:

```text
(0,0):east
(0,1):north
(0,2):north
(1,0):south
...
```

where states, by default, are shown as a tuple of variable values.

### Induced model

An induced model is a representation of the model that is induced when the strategy is applied. There are two "modes" for this export. The first (and default) is
*reduce*, which removes the nondeterminism resolved by the strategy (e.g., an MDP becomes a DTMC). This can be useful to re-import the model back into PRISM and analyse the induced model. The second is *restrict*, which shows the original model but with a restricted set of choices (e.g., an MDP with just one choice in each state).

In each case, the induced model can be exported in various formats,
as for regular [model export](ImportingAndExportingModels.md#exporting),
including UMB or explicit format,
the latter either a combined (`.pexp`) file or just the transitions (`.tra`).

### Dot file

A Dot file is, like an induced model, a view of the model induced by the strategy, but in [Dot](https://www.graphviz.org/) format, which allows it to be visualised.

## Configuring strategy export

As hinted in the command-line examples above, the `-exportstrat` switch uses the file extension to determine the preferred format: if the strategy is exported to a file with extension `.umb`/`.pexp`/`.tra` or `.dot`, then it uses an induced model or Dot file, respectively. Otherwise, the default is an action list. You can specify the desired format:

```shell
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat file.txt:type=actions
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat file.txt:type=induced
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat file.txt:type=dot
```

Further options can be added, e.g., to specify whether an induced model is exported in "restrict" or "reduce" mode:

```shell
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat file.txt:type=induced,mode=restrict
```

The switch can be used more than once, to export the same strategy in several ways:

```shell
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.txt -exportstrat strat.dot
```

A list of available options is as follows:

- `type` (`actions`, `induced` or `dot`): the type of strategy export to use (action list, induced model or Dot file)
- `mode` (`restrict` or `reduce`): when exporting as an induced model or Dot file, whether to "restrict" or "reduce" the model (see above); the default is "reduce"
- `reach` (`true` or `false`): whether to restrict the strategy to states that are reachable when it is applied to the model (this is only used for exporting induced models and Dot files, and the default value is true, except for exporting just transitions in `.tra` format)
- `states` (`true` or `false`): whether to show states, rather than state indices, for actions lists or Dot files; this is true by default
- `mergeobs` (`true` or `false`): for partially observable models, whether to merge observationally equivalent states; this is true by default (and was previously called `obs`).

Mirroring the `-exportmodel` switch ([see here](ImportingAndExportingModels.md#exporting)),
several other options are available for induced model export:

- `format` (`explicit`, `dot` or `umb`): the format to export in
- `states` (`true` or `false`): whether to include states
- `rewards` (`true` or `false`): whether to include rewards
- `labels` (`true` or `false`): whether to include labels
- `actions` (`true` or `false`): whether to include actions on choices/transitions
- `obs` (`true` or `false`): whether to include observations
- `precision` (`<n>`): use `<n>` significant figures for floating point values (in text)
- `zip` (`true`, `false`, `gzip` or `xz`): whether to zip the output and in what format
- `headers` (`true` or `false`): whether to include headers when exporting rewards
- `text` (`true` or `false`): whether to use textual form for UMB

## Strategy types

PRISM generates several types of strategies. The simplest are *memoryless deterministic* strategies, which pick a single action in each state, as in the examples above. For some query types (e.g., step-bounded properties, or LTL-based properties), *finite-memory* strategies are generated, where an additional memory value is used. For these, induced models or Dot files are most useful since they will also show how the memory values are updated as the strategy is executed. Note that, in these cases, the state indices of the strategy will correspond to the product model constructed during model checking, not the original model. The product model can be exported using the `-exportprodtrans` and `-exportprodstates` switches.

## Checking strategies

To check that generated strategies are correct, use the switch [`-test:strat`](../Reference/CommandLineSwitches.md#test).
This is an option of PRISM's *test mode* (`-test`), which compares the result of each property
with the expected result given in a `// RESULT: ...` comment before the property in the properties file,
and is used for PRISM's regression tests.
With `-test:strat`, PRISM also generates a strategy for each property and checks the property again
on the model induced by the strategy (essentially the one that `-exportstrat ...:type=induced` would export), e.g.:

```shell
prism mdp.prism mdp.props -test:strat
```

The two results should match:
for a query such as `Pmax=? [ F "goal" ]`, the strategy should achieve the optimal value;
for a bound such as `P>=0.5 [ F "goal" ]`, it should be a witness if the property holds and a counterexample if not.
Subformulas nested inside the property's operators are checked on the original model, not under the strategy.
For POMDPs, whose results are approximate, the strategy's value should be no better than the computed bound.
The outcome is shown after the usual test result as `Testing result: strategy PASS`
(or `FAIL`, `UNSUPPORTED` or `SKIPPED`, with an explanation).
Strategies that cannot be generated or applied, e.g., where an action labels several choices in a state,
are reported as unsupported.
Properties are skipped if they are not a single `P`, `R`, ... operator (possibly compared with a constant),
or use a filter other than `state`, or are multi-objective queries.
Where a strategy leaves a reachable state's choice undefined for an unknown reason,
the first choice is used (strategy export would report an error), and a warning is shown.
As with `-exportstrat`, generating strategies may mean that PRISM switches to the explicit engine,
or changes some settings for other engines, so results are not necessarily computed with the engine requested.

## Adversary generation

The "sparse" engine still includes PRISM's older "adversary generation" functionality,
which exports the induced model of an optimal strategy for MDPs, as a DTMC or an MDP
(switches [`-exportadv`](../Reference/CommandLineSwitches.md#exportadv) and [`-exportadvmdp`](../Reference/CommandLineSwitches.md#exportadvmdp),
or the "Adversary export" option in the GUI).
This is superseded by `-exportstrat`, described above, whose `reduce` and `restrict` modes give the same two models.
