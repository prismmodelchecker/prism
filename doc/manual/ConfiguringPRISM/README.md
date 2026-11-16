# Configuration and performance

PRISM is highly configurable, in terms of the various model checking algorithms and techniques
that it uses, and the different ways that they are implemented and parameterised.

<a id="settings"></a>
## Changing settings

In the GUI, select "Options" from the main menu to bring up the "Options" dialog.
The settings are grouped under several tabs: those which affect the basic model checking functionality of the tool are under "PRISM",
and there are separate tabs for the simulator and for the GUI itself (the model editor, the property editor and the log).
Settings made in the GUI are saved in a [settings file](#settings-file) and reused the next time it is started.
To restore the default settings, click "Load Defaults" and then "Save Options".

From the command line, settings are made with switches,
all of which are listed, with full details, in "[Command-line switches](../Reference/CommandLineSwitches.md)".
A list can also be displayed by typing `prism -help`,
and further help for a switch whose format is not straightforward with `-help switch`, e.g.:

```shell
prism -help const
```

The command-line version ignores the settings file, unless you ask for it to be read with the [`-settings`](../Reference/CommandLineSwitches.md#settings) switch, e.g. on Linux:

```shell
prism -settings ~/.config/prism.settings
```

The other pages in this section describe the most important settings.

<a id="performance"></a>
## If PRISM is slow or runs out of memory

Things to try:

- The `-heuristic speed` switch (the "Heuristic" option in the GUI), which chooses an engine and other settings aimed at speed rather than saving memory.
- A different [computation engine](ComputationEngines.md): their performance can vary a great deal.
- [Topological value iteration](SolutionMethodsAndOptions.md#topo) or another [solution method](SolutionMethodsAndOptions.md).
- More memory for CUDD or Java: see "[Memory and model size](MemoryAndModelSize.md)".
- The FAQ's "[Memory problems](../FrequentlyAskedQuestions/MemoryProblems.md)" for advice on reducing the size of the model.

<a id="settings-file"></a>
## Settings file

The settings file is in a simple textual format and can also be edited by hand,
or deleted to restore the default settings the next time the GUI is started.
Where it is depends on the operating system:

- `$HOME/.prism` (if this file already exists, as created by PRISM versions before 4.5)
- `$XDG_CONFIG_HOME/prism.settings` (on Linux, if that environment variable is set)
- `$HOME/.config/prism.settings` (on Linux, if `$XDG_CONFIG_HOME` is not set)
- `$HOME/Library/Preferences/prism.settings` (on macOS)
- `.prism` in the user's home directory, e.g. `C:\Users\username` (on Windows)

## Contents

- [Computation engines](ComputationEngines.md): PRISM's engines, symbolic and explicit, how to choose one, and their options
- [Solution methods and options](SolutionMethodsAndOptions.md): numerical methods, convergence and their options
- [Automata generation](AutomataGeneration.md): how LTL formulas are converted to automata, including with external tools
- [Memory and model size](MemoryAndModelSize.md): memory settings for CUDD and Java, and environment variables
- [Other options](OtherOptions.md): output and logging, model construction and model checking options, and time-outs
