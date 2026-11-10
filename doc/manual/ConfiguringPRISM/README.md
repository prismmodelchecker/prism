# Configuration and performance

The settings that control how PRISM works, such as its computation engines, numerical solution methods and automata generation, and how to change them;
and how to deal with large models and limited memory.

In the GUI, settings are made in the "Options" dialog and saved in a [settings file](SettingsFile.md);
on the command line, they are made with switches (`prism -help` lists them all).

## Contents

- [Settings](SettingsFile.md): the GUI's options dialog, the settings file, and command-line switches
- [Computation engines](ComputationEngines.md): PRISM's engines, symbolic and explicit, and how to choose one
- [Solution methods and options](SolutionMethodsAndOptions.md): numerical methods, convergence and their options
- [Automata generation](AutomataGeneration.md): how LTL formulas are converted to automata, including with external tools
- [Memory and model size](MemoryAndModelSize.md): memory settings for CUDD and Java, and what to do when PRISM runs out of memory
- [Other options](OtherOptions.md): output, probability checks, precomputation and time-outs
