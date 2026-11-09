# Explicit model import

It is also possible to construct models in PRISM through direct specification of their transition matrix.
Two formats for explicit model import are currently supported:

- [plain text](../Appendices/ExplicitModelFiles.md) files (sometimes referred to as "explicit" or "`.tra`" or "`.pexp`" files)
- [UMB](https://pmc-tools.github.io/umb/) (Unified Markov binary) format, a binary format supported by other probabilistic model checkers

Presently, this functionality is only supported in the command-line version of the tool, usually using the `-importmodel` switch.
In a similar fashion to `-exportmodel` switch, the type of import is usually determined from the file extension.

For UMB, all model info is contained in a single file. An example of import is:

```shell
prism -importmodel poll2.umb
```

For the plain text files (`.tra` etc.), various options are possible.
To import everything (in the combined `.pexp` format) use:

```shell
prism -importmodel poll2.pexp
```

To just import the core part of the model (the transition function) use:

```shell
prism -importmodel poll2.tra
```

From explicit format, PRISM tries to determine the model type from the transitions data,
using the model type included in the header, if present,
and guessing from the format of the data if not.
If this does not work, the model type can be overwritten using the `-dtmc`, `-ctmc` and `-mdp` switches.
For example:

```shell
prism -importmodel poll2.tra -ctmc
```

Using the same formats as for [model export](ExportingTheModel.md), multiple other parts of the model can also be imported separately:

- `.lab` - labels
- `.srew`, `.trew` - rewards: states, transitions
- `.sta` - states
- `.obs` - observations

Here is an example of also importing labels and states:

```shell
prism -importmodel poll2.tra,sta,lab
```

If state information is not imported, a single zero-indexed variable `x` is assumed.

Note also that, since details about the initial state(s) of a model are not preserved in the `.tra` file,
but are included in the labels file, this should also be used to designate a particular initial state for a model.
Otherwise, state 0 is assumed to be the initial state or,
if state information is imported, the state in which all variables take their minimum value is used.

Use the extension `.all` to import from multiple files containing separate parts of the model, named with appropriate file extensions:

```shell
prism -importmodel poll2.all
```

When providing the whole model, you can omit the `-importmodel` switch, e.g.:

```shell
prism poll2.pexp
prism poll2.all
```

Any files can be zipped, and this will be detected by their (`.gz` or `.xz`) filename extension.
These are decompressed automatically on import.

## Efficiency

Please note, for PRISM's symbolic model construction/engines, which are often used by default,
this explicit method of constructing models in PRISM is typically less efficient than using the PRISM language.
This is because the underlying data structures used to represent the model
function better when there is high-level structure and regularity to exploit.
You may want to switch to the "explicit" engine:

```shell
prism -importmodel poll2.tra -ex
```

The situation can also be alleviated to a certain extent for the symbolic implementations by importing
the `.sta` file, since the composition of the states reintroduces some regularity
(although is still typically inefficient to construct).

<a id="other"></a>

## Other options

In a similar style to PRISM's [`-exportmodel`](ExportingTheModel.md) switch, while `-importmodel` is usually the most convenient, you can also specify each part separately, e.g.:

```shell
prism -importtrans poll2.tra -importstates poll2.sta -importlabels poll2.lab -ctmc
```

There are also `-importstaterewards` and `-importtransrewards` switches.
You can import multiple reward structures using multiple instances of the these switches.
If present in the rewards files (see the appendix "[Explicit model files](../Appendices/ExplicitModelFiles.md)"),
the names of the reward structures are read too.
