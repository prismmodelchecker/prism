# Exporting The Model

If required, once the model has been constructed, it can be exported, either for manual examination or for use in another tool. The following can all be exported:

- the **transition matrix** (or function);
- the **rewards** (state/transition) rewards;
- the **labels** (in the [model](../ThePRISMLanguage/FormulasAndLabels.md) or [properties](../PropertySpecification/PropertiesFiles.md#labels)) and the states that satisfy them
- the definition of (reachable) **states**, i.e., their variable values
- the definition of **observations**, i.e., their observable values, for partially observable models

From the command-line version of PRISM, the most convenient and flexible approach is to use the `-exportmodel` switch.
This will, by default, use the extension of the filename(s) to determine the format.

From the GUI, use the "Model | Export" menu to export the data to a file or, for small models, use the "Model | View" menu to print the details directly to the log. For the case of labels, if you want to export labels from the properties file too, use the "Properties | Export labels" option, rather than the "Model | Export" one.

The main formats for model export are:

- [plain text](../Appendices/ExplicitModelFiles.md) files (sometimes referred to as "explicit" or ".tra" or ".pexp" files)
- [UMB](https://pmc-tools.github.io/umb/) (Unified Markov binary) format, a binary format supported by other probabilistic model checkers
- [Dot](https://www.graphviz.org/) format, for a graphical view of the model

<a id="explicit"></a>

## Plain text (explicit files)

This format either exports all/multiple parts of the model into a single combined file,
or individual parts of the model in different files. The expected filename extensions are:

- `.pexp` - all parts of the model("PRISM explicit")

- `.tra` - transition matrix
- `.srew`, `.trew`, `.rew` - rewards: states, transitions or both
- `.lab` - labels
- `.sta` - states
- `.obs` - observations

and appending `.gz` or `.xz` to these extensions causes the file to be zipped.

To export everything, use:

```shell
prism model.prism -exportmodel model.pexp
```

To export everything zipped, use:

```shell
prism model.prism -exportmodel model.pexp.gz
```

To export everything except states, use:

```shell
prism model.prism -exportmodel model.pexp:states=false
```

To export just the transition matrix in this format, use:

```shell
prism model.prism -exportmodel model.tra
```

To export multiple parts, use, e.g.:

```shell
prism model.prism -exportmodel model.tra,sta
```

If you omit the file basename of the export files and the basename of the model will be used, so this is equivalent to the above:

```shell
prism model.prism -exportmodel .tra,sta
```

You can use the shorthand `.all` to export everything to separate files, and `.rew` to export both state and transition rewards. For example:

```shell
prism model.prism -exportmodel model.all
```

You can always use `stdout` instead of a filename. For example:

```shell
prism model.prism -exportmodel stdout
```

is a quick way to print all parts of a (small) model to the terminal, or

```shell
prism model.prism -exportmodel stdout.sta
```

prints its states.

The labels (`.lab`) export includes the built-in labels `"init"` and `"deadlock"`,
providing a way to export information about initial states and (fixed) deadlock states.

When exporting multiple reward structures to individual files, a (1-indexed) suffix is added to distinguish them.

By default each part of the model is prefixed by a header (starting `#`),
identifying what it is, plus some additional details,
e.g., the name of a reward structure or the type of a model
(see the "[Explicit Model Files](../Appendices/ExplicitModelFiles.md#srew)" appendix).
This can be omitted - see the options below - or via the option "Include headers in model exports" in the GUI.

<a id="umb"></a>

## UMB (Unified Markov binary) format

UMB is a binary format that can incorporate all parts of the model listed above.

```shell
prism model.prism -exportmodel model.umb
```

By default, everything is included. You can omit some parts with the `-exportmodel`
options `rewards`, `labels`, `states` and `obs`, e.g.:

```shell
prism model.prism -exportmodel model.umb:states=false,rewards=false
```

For a small model, you can also see a textual version of the UMB format
using file extension `.umbt` (or option `text`):

```shell
prism model.prism -exportmodel stdout.umbt
```

UMB files are compressed by default (but still have the extension `.umb`).
You can also configure the compression used: `zip=false` turns it off,
`zip=gzip` uses gzip and `zip=xz` uses xz (smaller but slower to read/write).

<a id="other"></a>

## Other formats and options

If the file extension is not recognised, PRISM defaults to plain text (explicit) format.
You can always override this using the `format` option, e.g.:

```shell
prism model.prism -exportmodel model.txt:format=explicit
prism model.prism -exportmodel model.txt:format=umb,text
```

You can perform multiple model exports using several instances of `-exportmodel`, e.g.:

```shell
prism model.prism -exportmodel model.txt:format=explicit -exportmodel model.dot
```

Other file formats are also available:

- [Matlab](http://www.mathworks.com/) code (extension `.m`) or `format=matlab`
- Storm's DRN model format (extension `.drn`) or `format=drn`

Other `-exportmodel` options are:

- `states` (=`true`/`false`) - whether to include states
- `rewards` (=`true`/`false`) - whether to include rewards
- `labels` (=`true`/`false`) - whether to include labels
- `actions` (=`true`/`false`) - whether to  actions on choices/transitions
- `obs` (=`true`/`false`) - whether to include observations
- `precision` (=`<n>`) - use `<n>` significant figures for floating point values (in text)
- `headers` (`=true/false`) - whether to include headers when exporting rewards
- `rows` - export matrices with one row/distribution on each line (plain text)
- `proplabels` - also export labels from a properties file into a `.lab` file
- `zip` (`=true/false/gzip/xz`) - whether to zip the output and in what format

For plain text export, although `-exportmodel` is now usually the best switch to use,
other older switches still exist. For example, you can export individual files using
`-exporttrans <file>`,
`-exportstates <file>`,
`-exportstaterewards <file>`,
`-exporttransrewards <file>`,
`-exportrewards <file> <file>`,
`-exportlabels <file>`, and
`-exportproplabels <file>`.
And you can use switches
`-exportmodelprecision <x>`,
`-exportmatlab` and
`-exportrows`
to specify format options affecting all of them.

<a id="scc"></a>

## Exporting (B)SCCs and end components

It is also possible to export the set of (bottom) strongly connected components (SCCs or BSCCs) for a model. This can only be done from the command-line currently. Use, for example:

```shell
prism model.prism -exportsccs stdout
prism model.prism -exportbsccs stdout
```

For an MDP, you can also export the set of maximal end components (MECs):

```shell
prism mdp.nm -exportmecs stdout
```
