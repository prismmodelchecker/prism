# Running PRISM

There are two versions of PRISM, one based on a **graphical user interface** (GUI),
the other based on a **command line interface** (CLI).
Both use the same underlying model checker.
The latter is useful for running large batches of jobs, leaving long-running model checking tasks in the background, or simply for running the tool quickly and easily once you are familiar with its operation.

Details of how to run PRISM can be found in the [installation instructions](../GettingStarted/Installing.md).

In short, to run the PRISM GUI:

- (on Windows) click the short-cut (to `xprism.bat`) installed on the Desktop/Start Menu
- (on other OSs) run the `xprism` script in the `bin` directory

and for the command-line:

- run the `prism` script in the `bin` directory

This section describes the main functionality offered by PRISM,
from both the GUI and command line.

## Contents

- [Loading and building a model](LoadingAndBuildingAModel.md): loading a model, building it, and dealing with deadlocks
- [Model checking](ModelChecking.md): checking properties, in the GUI and on the command line
- [Experiments](Experiments.md): checking properties for ranges of constants, and plotting the results
- [Strategies](Strategies.md): generating and exporting strategies (policies) for MDPs and other models
- [Simulation](Simulation.md): exploring a model step by step, and generating random paths
- [Statistical model checking](StatisticalModelChecking.md): approximate results by simulation
- [Transient and steady-state probabilities](ComputingSteady-stateAndTransientProbabilities.md): probability distributions over a model's states
- [Parametric model checking](ParametricModelChecking.md): results as functions of model parameters
- [Importing and exporting models](ImportingAndExportingModels.md): writing a built model to files, and building one from files
- [Other modelling languages](OtherLanguages.md): models written in the PEPA process algebra, or in SBML
