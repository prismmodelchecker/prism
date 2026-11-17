# Configuration and performance

PRISM is highly configurable, in terms of the various model checking algorithms and techniques
that it uses, and the different ways that they are implemented and parameterised.

<a id="settings"></a>
## Changing settings

**GUI:** In the GUI, select "Options" from the main menu to bring up the "Options" dialog.
The first two tabs contain core PRISM settings, discussed in this section,
and [simulator](../RunningPRISM/Simulation.md) options;
the others configure aspects of the GUI itself.
If you wish, you can choose "Save Options" to store your changes
in a [settings file](#settings-file), to be reloaded as defaults next time.
To restore the default settings, click "Load Defaults" and then "Save Options".

**Command line:**
From the command line, settings are changed via switches.
You can use either `-switch` or `--switch` and many options have abbreviations,
so for example these are all equivalent:

```shell
prism model.prism model.props -ex
prism model.prism model.props -explicit
prism model.prism model.props --explicit
```

See the "[Command-line switches](../Reference/CommandLineSwitches.md)" section for a full list, or type `prism -help`.
To display details of a switch, use:

```shell
prism -help exportmodel
```

The command-line version ignores the GUI [settings file](#settings-file),
unless you choose to load it with the [`-settings`](../Reference/CommandLineSwitches.md#settings) switch,
e.g., on Linux: `prism -settings ~/.config/prism.settings`.

## Contents

This section guides you through some of the key settings for PRISM.

- [Computation engines](ComputationEngines.md): PRISM's various computation engines and how to choose one
- [Solution methods and options](SolutionMethodsAndOptions.md): methods available for numerical computation and their configuration
- [Automata generation](AutomataGeneration.md): how LTL formulas are converted to automata, including with external tools
- [Memory and model size](MemoryAndModelSize.md): memory settings for PRISM, Java and the CUDD library
- [Other options](OtherOptions.md): output and logging, model construction and model checking options, and more

See also the guidance on [running out of memory](../FrequentlyAskedQuestions/MemoryProblems.md).

<a id="settings-file"></a>
## Settings file

The PRISM settings file, used primarily by the [GUI](#settings),
is in a simple textual format and can also be edited by hand,
or deleted to restore the default settings the next time the GUI is started.
Where it is stored depends on the operating system:

- `$HOME/.prism` (if this file already exists, as created by PRISM versions before 4.5)
- `$XDG_CONFIG_HOME/prism.settings` (on Linux, if that environment variable is set)
- `$HOME/.config/prism.settings` (on Linux, if `$XDG_CONFIG_HOME` is not set)
- `$HOME/Library/Preferences/prism.settings` (on macOS)
- `.prism` in the user's home directory, e.g. `C:\Users\username` (on Windows)
