# Strategies

Properties to be model checked on MDPs (and their variants, such as POMDPs or IMDPs) usually quantify over *strategies* (or *policies*) of the model, i.e., over the different possible ways that nondeterminism can be resolved in the model.
For example, this property:

```prism
Pmax=? [ F "goal" ]
```

determines the maximum probability, over all strategies, of reaching a state satisfying the label `"goal"`. When checking such properties, you can also ask PRISM to generate a corresponding (optimal) strategy, which yields this maximum probability when followed. The strategy can then be viewed, exported or simulated.

**Note:** For consistency across models, PRISM now uses the terminology *strategy* (rather than alternatives such as *policy*). In older versions of the tool, these were referred to as *adversaries*. Currently, the newer (and more extensive) *strategy generation* functionality is implemented just for the ["explicit"](../ConfiguringPRISM/ComputationEngines.md) model checking engine,
which is used automatically if strategy generation is requested.
The old *adversary generation* functionality (see below) still exists for the "sparse" engine, but will be updated in the future.

**Generating strategies**. Optimal strategies can be generated either from the command-line or the graphical user interface (GUI). For the former, use the `-exportstrat` switch. Simple examples are:

```shell
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat stdout
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.pexp
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.umb
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.dot
prism mdp.prism -pf 'Pmax=? [ F "goal" ]' -exportstrat strat.tra
```

The first of these exports actions, the others export induced models (see below).

From the GUI, you can trigger strategy generation by ticking the "Generate strategy" box either on the popup menu that appears when you right-click a property, or from the "Strategies" menu at the top. As long as it is supported, a strategy will be then generated once "Verify" is clicked.

From the same menu(s), you can then

- export the strategy to a file
- view the strategy by printing it in the log
- explore the strategy in the simulator

**Strategy export types**. Strategies can be viewed or exported in several different formats:

(i) *Action list*. This is a list of the action taken in each state of the model, e.g.:

```text
(0,0):east
(0,1):north
(0,2):north
(1,0):south
...
```

where states, by default, are shown as a tuple of variable values.

(ii) *Induced model*. This is a representation of the model that is induced when the strategy is applied. There are two "modes" for this export. The first (and default) is
*reduce*, which removes the nondeterminism resolved by the strategy (e.g., an MDP becomes a DTMC). This can be useful to re-import the model back into PRISM and analyse the induced model. The second is *restrict*, which shows the original model but with a restricted set of choices (e.g., an MDP with just one choice in each state).

In each case, the induced model can be exported in various formats,
as for regular [model export](ImportingAndExportingModels.md#exporting),
including UMB or explicit format,
the latter either a combined (`.pexp`) file or just the transitions (`.tra`).

(iii) *Dot file*. This is, like the previous format, a view of the model induced by the strategy, but in [Dot](http://www.graphviz.org) format, which allows it to be visualised.

**Configuring strategy export**.
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

A list of available options is as follows:

- `type` (`actions`, `induced` or `dot`): the type of strategy export to use (action list, induced model or Dot file)
- `mode` (`restrict` or `reduce`): when exporting as an induced model or Dot file, whether to "restrict" or "reduce" the model (see above); the default is "reduce"
- `reach` (`true` or `false`): whether to restrict the strategy to states that are reachable when it is applied to the model (this is currently only used for exporting induced models and Dot files, and the default value is true, except for exporting just transitions in `.tra` format)
- `states` (`true` or `false`): whether to show states, rather than state indices, for actions lists or Dot files; this is true by default
- `mergeobs` (`true` or `false`): for partially observable models, whether to merge observationally equivalent states; this is true by default (and was previously called `obs`).

Mirroring the `-exportmodel` switch ([see here](ImportingAndExportingModels.md#exporting)),
several other options are available for induced model export:

- `format` (=`explicit`/`dot`/`umb`) - the format to export in
- `states` (=`true`/`false`) - whether to include states
- `rewards` (=`true`/`false`) - whether to include rewards
- `labels` (=`true`/`false`) - whether to include labels
- `actions` (=`true`/`false`) - whether to include actions on choices/transitions
- `obs` (=`true`/`false`) - whether to include observations
- `precision` (=`<n>`) - use `<n>` significant figures for floating point values (in text)
- `zip` (`=true/false/gzip/xz`) - whether to zip the output and in what format
- `headers` (`=true/false`) - whether to include headers when exporting rewards
- `text` (`=true/false`) - whether to use textual form for UMB

**Strategy types**. PRISM generates several types of strategies. The simplest are *memoryless deterministic* strategies, which pick a single action in each state, as in the examples above. For some query types (e.g., step-bounded properties, or LTL-based properties), *finite-memory* strategies are generated, where an additional memory value is used. For these, induced models or Dot files are most useful since they will also show how the memory values are updated as the strategy is executed. Note that, in these cases, the state indices of the strategy will correspond to the product model constructed during model checking, not the original model. The product model can be exported using the `-exportprodtrans` and `-exportprodstates` switches.

**Adversary generation**. As mentioned above, the "sparse" model checking engine still includes older so-called "adversary generation" functionality. This can be used to export the induced model to a file using the `-exportadv` switch, e.g.:

```shell
prism mdp.nm -pf 'Pmax=? [ F "goal" ]' -exportadv adv.tra -s
prism mdp.nm -pf 'Pmax=? [ F "goal" ]' -exportadvmdp adv.tra -s
```

where the `-exportadv` and `-exportadvmdp` export a DTMC and an MDP, respectively, i.e., corresponding to the "reduce" and "restrict" modes described above.
From the GUI, change the "Adversary export" option (under the "PRISM" settings) from "None" to "DTMC" or "MDP". You can also change the filename for the export adversary which, by default, is `adv.tra` as in the example above.
