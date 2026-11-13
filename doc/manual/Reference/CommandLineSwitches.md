# Command-line switches

This page lists all the switches accepted by the [command-line](../RunningPRISM/README.md) version of PRISM,
grouped as in the output of `prism -help`.
Many of them are described in more detail elsewhere in this manual,
and most of the settings they change can also be made in the GUI (from the Options menu)
or in the [settings file](../ConfiguringPRISM/SettingsFile.md).
For switches whose syntax is not obvious, `prism -help` followed by the switch, e.g., `prism -help -exportmodel`,
gives the details shown below.

The list is generated from PRISM itself, so it is complete for this version of PRISM.
Switches are typed with a single `-`, as shown, but `--` also works, e.g., `--version`.

<!-- generated: switches (by doc/update-generated.py; do not edit) -->
<a id="general"></a>
## General

- <a id="help"></a>`-help`: Display this help message
- <a id="version"></a>`-version`: Display PRISM version info
- <a id="javaversion"></a>`-javaversion`: Display Java version info
- <a id="dir"></a>`-dir <dir>`: Set current working directory
- <a id="settings"></a>`-settings <file>`: Load settings from \<file\>
- <a id="mainlog"></a>`-mainlog <file>[:<options>]`: Write the log to \<file\> instead of stdout

  If provided, \<options\> is a comma-separated list of options taken from:

  - `append`: Append to the log file, rather than overwriting it

- <a id="pf"></a><a id="pctl"></a><a id="csl"></a>`-pf <props>` (or `-pctl`, `-csl`): Model check properties \<props\>
- <a id="property"></a><a id="prop"></a>`-property <refs>` (or `-prop`): Only model check properties included in list \<refs\> of indices/names
- <a id="const"></a>`-const <vals>`: Define constant values as \<vals\> (e.g. for experiments)

  \<vals\> is a comma-separated list of values or value ranges for undefined constants
  in the model or properties (i.e. those declared without values, such as "const int a;").
  You can either specify a single value (a=1), a range (a=1:10) or a range with a step (a=1:2:50).
  For convenience, constant definitions can also be split across multiple -const switches.

  Examples:

  ```
  -const a=1,b=5.6,c=true
  -const a=1:10,b=5.6
  -const a=1:2:50,b=5.6
  -const a=1:2:50 -const b=5.6
  ```

- <a id="steadystate"></a><a id="ss"></a>`-steadystate` (or `-ss`): Compute steady-state probabilities (D/CTMCs only)
- <a id="transient"></a><a id="tr"></a>`-transient <x>` (or `-tr`): Compute transient probabilities for time (or time range) \<x\> (D/CTMCs only)
- <a id="simpath"></a>`-simpath <options> <file>`: Generate a random path with the simulator

  Generate a random path with the simulator and export it to \<file\> (or to the screen if \<file\>="stdout").
  \<options\> is a comma-separated list of options taken from:

  - `<n>`: generate a path of \<n\> steps
  - `time=<x>`: generate a path of at least \<x\> time units
  - `deadlock`: generate a path until a deadlock is reached
  - `repeat=<n>`: try \<n\> paths until a deadlock is found
  - `sep=<val>`: use \<val\> as column separator (space, tab, comma)
  - `vars=<x1,x2,...>`: show values for variables x1,x2,.. only
  - `loopcheck=<true|false>`: whether to detect deterministic loops
  - `snapshot=<x>`: view states at fixed timepoints of interval \<x\>
  - `probs=<true|false>`: display probability (or rate) of transitions taken
  - `rewards=<true|false>`: display state/transition rewards
  - `changes=<true|false>`: only display states where displayed variables change

- <a id="nobuild"></a>`-nobuild`: Skip model construction (just do parse/export)
- <a id="test"></a>`-test[:<options>]`: Enable "test" mode

  If provided, \<options\> is a comma-separated list of options taken from:

  - `umb`: Enable UMB round trip test
  - `pexp`: Enable explicit (.pexp) round trip test

- <a id="testall"></a>`-testall`: Enable "test" mode, but don't exit on error
- <a id="javamaxmem"></a>`-javamaxmem <x>`: Set the maximum heap size for Java, e.g. 500m, 4g \[default: 1g\]
- <a id="javastack"></a>`-javastack <x>`: Set the Java stack size \[default: 4m\]
- <a id="javaparams"></a>`-javaparams <x>`: Pass additional command-line arguments to Java
- <a id="timeout"></a>`-timeout <n>`: Exit after a time-out of \<n\> seconds if not already terminated
- <a id="ng"></a>`-ng`: Run PRISM in Nailgun server mode; subsequent calls are then made via "ngprism"

<a id="imports"></a>
## Imports

- <a id="importpepa"></a>`-importpepa`: Model description is in PEPA, not the PRISM language
- <a id="importmodel"></a>`-importmodel <files>[:<options>]`: Import the model directly from file(s)

  Import the model directly from one or more file(s).
  Use a list of file extensions to indicate which files should be read, e.g.:

  ```
  -importmodel in.tra,sta
  ```

  Possible extensions are: .tra, .sta, .obs, .lab, .srew, .trew, .pexp, .umb
  Use extension .all to import all explicit files (.tra/sta/obs/lab/srew/trew), e.g.:

  ```
  -importmodel in.all
  ```

  If provided, \<options\> is a comma-separated list of options taken from:

  - `format` (=explicit/umb): model import format

- <a id="importtrans"></a>`-importtrans <file>`: Import the transition matrix directly from a text file
- <a id="importstates"></a>`-importstates <file>`: Import the list of states directly from a text file
- <a id="importobs"></a>`-importobs <file>`: Import the list of observations directly from a text file
- <a id="importlabels"></a>`-importlabels <file>`: Import the list of labels directly from a text file
- <a id="importstaterewards"></a>`-importstaterewards <file>`: Import the state rewards directly from a text file
- <a id="importtransrewards"></a>`-importtransrewards <file>`: Import the transition rewards directly from a text file
- <a id="importinitdist"></a>`-importinitdist <file>`: Specify initial probability distribution for transient/steady-state analysis
- <a id="dtmc"></a>`-dtmc`: Force imported/built model to be a DTMC
- <a id="ctmc"></a>`-ctmc`: Force imported/built model to be a CTMC
- <a id="mdp"></a>`-mdp`: Force imported/built model to be an MDP
- <a id="importresults"></a>`-importresults <file>`: Import results from a data frame stored in CSV file

  Import results from a data frame stored as comma-separated values in \<file\>.

<a id="exports"></a>
## Exports

- <a id="exportresults"></a>`-exportresults <file[:<options>]>`: Export the results of model checking to a file

  Exports the results of model checking to \<file\> (or to the screen if \<file\>="stdout").
  The default behaviour is to export a list of results in text form, using tabs to separate items.
  If provided, \<options\> is a comma-separated list of options taken from:

  - `csv`: Export results as comma-separated values
  - `matrix`: Export results as one or more 2D matrices (e.g. for surface plots)
  - `dataframe`: Export results as dataframe in comma-separated values
  - `comment`: Export results in comment format for regression testing

- <a id="exportvector"></a>`-exportvector <file>`: Export results of model checking for all states to a file
- <a id="exportmodel"></a>`-exportmodel <files[:<options>]>`: Export the built model to file(s)

  Export the built model to file(s) (or to the screen if \<file\>="stdout").
  Use a list of file extensions to indicate which files should be generated, e.g.:

  ```
  -exportmodel out.tra,sta
  -exportmodel out.umb
  ```

  Possible extensions are: .tra, .srew, .trew, .lab, .sta, .obs, .dot, .umb, .drn
  Use extension .all to export all explicit files (.tra/srew/trew/lab/sta/obs), e.g.:

  ```
  -exportmodel out.all
  ```

  Omit the file basename to use the basename of the model file, e.g.:

  ```
  -exportmodel .all
  ```

  Use extension .rew to export both .srew/.trew files

  If provided, \<options\> is a comma-separated list of options taken from:

  - `format` (=explicit/matlab/dot/drn/umb): model export format
  - `rewards` (=true/false, or bare for true): whether to include rewards
  - `labels` (=true/false, or bare for true): whether to include labels
  - `states` (=true/false, or bare for true): whether to include state definitions
  - `obs` (=true/false, or bare for true): whether to include observation definitions
  - `actions` (=true/false, or bare for true): whether to include actions on choices/transitions
  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `zip` (=true/false/gzip/xz): whether to zip UMB files
  - `text`: show binary formats in textual form
  - `matlab`: same as format=matlab
  - `rows`: export matrices with one row/distribution on each line
  - `proplabels`: also export labels from a properties file into the same file, too
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exporttrans"></a>`-exporttrans <file>[:<options>]`: Export the transition matrix to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exportstaterewards"></a>`-exportstaterewards <file>[:<options>]`: Export the state rewards vector to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exporttransrewards"></a>`-exporttransrewards <file>[:<options>]`: Export the transition rewards matrix to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exportrewards"></a>`-exportrewards <file1[:<options>]> <file2[:<options>]>`: Export state/transition rewards to files 1/2

  Export state/transition rewards to files 1/2 (or to the screen if \<file\>="stdout").

  If provided, \<options\> is a comma-separated list of options (for either file) taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exportstates"></a>`-exportstates <file>[:<options>]`: Export the list of reachable states to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exportobs"></a>`-exportobs <file>[:<options>]`: Export the list of observations to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files

- <a id="exportlabels"></a>`-exportlabels <file[:<options>]>`: Export the list of labels and satisfying states to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files
  - `matlab`: export data in Matlab format
  - `proplabels`: export labels from a properties file into the same file, too

- <a id="exportproplabels"></a>`-exportproplabels <file[:<options>]>`: Export the list of labels and satisfying states from the properties file to a file

  If provided, \<options\> is a comma-separated list of options taken from:

  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text)
  - `headers` (=true/false, or bare for true): include headers in explicit model files
  - `matlab`: export data in Matlab format

- <a id="exportstrat"></a>`-exportstrat <file[:<options>]>`: Generate and export a strategy to a file

  Generate and export a strategy to a file (or to the screen if "stdout").
  Use file extension .tra, .pexp, .umb or .dot to export as an induced model.
  The default (e.g. for file extension .txt) is a list of state-action choices.

  If provided, \<options\> is a comma-separated list of options taken from:

  - `type` (=actions/indices/induced/dot): type of strategy export
  - `mode` (=restrict/reduce): mode to use for building induced model (or Dot file)
  - `reach` (=true/false, or bare for true): whether to restrict the strategy to its reachable states
  - `states` (=true/false, or bare for true): whether to include full state information
  - `mergeobs` (=true/false, or bare for true): for partially observable models, whether to merge observationally equivalent states
  - `undefined` (=stop/loop/first/all/error): how to resolve undefined choices, for all reasons except unknown
  - `unknown` (=stop/loop/first/all/error): how to resolve undefined choices when there is no information about them
  - `arbitrary` (=stop/loop/first/all/error): how to resolve undefined choices where any choice is equally good
  - `satisfied` (=stop/loop/first/all/error): how to resolve undefined choices after the objective is satisfied
  - `unsatisfiable` (=stop/loop/first/all/error): how to resolve undefined choices when the objective can no longer be satisfied
  - `horizon` (=stop/loop/first/all/error): how to resolve undefined choices after a step bound
  - `unreachable` (=stop/loop/first/all/error): how to resolve undefined choices in states not reachable under the strategy
  - `showundef` (=true/false, or bare for true): whether to also show undefined choices (and why) when exporting actions/indices
  - `format` (=explicit/dot/drn/umb): model export format for induced model export
  - `rewards` (=true/false, or bare for true): whether to include rewards for induced model export
  - `labels` (=true/false, or bare for true): whether to include labels for induced model export
  - `actions` (=true/false, or bare for true): whether to include actions on choices/transitions for induced model export
  - `obs` (=true/false, or bare for true): whether to include observation details for induced model export
  - `precision` (=\<n\>): use \<n\> significant figures for floating point values (in text) for induced model export
  - `zip` (=true/false/gzip/xz): whether to zip files for induced model export
  - `text`: show binary formats in textual form for induced model export
  - `headers` (=true/false, or bare for true): include headers in explicit model files for induced model export

- <a id="exportmatlab"></a>`-exportmatlab`: When exporting matrices/vectors/labels/etc., use Matlab format
- <a id="exportrows"></a>`-exportrows`: When exporting matrices, put a whole row on one line
- <a id="exporttransdot"></a>`-exporttransdot <file>`: Export the transition matrix graph to a dot file
- <a id="exporttransdotstates"></a>`-exporttransdotstates <file>`: Export the transition matrix graph to a dot file, with state info
- <a id="exportdot"></a>`-exportdot <file>`: Export the transition matrix MTBDD to a dot file
- <a id="exportsccs"></a>`-exportsccs <file>`: Compute and export all SCCs of the model
- <a id="exportbsccs"></a>`-exportbsccs <file>`: Compute and export all BSCCs of the model
- <a id="exportmecs"></a>`-exportmecs <file>`: Compute and export all maximal end components (MDPs only)
- <a id="exportsteadystate"></a>`-exportsteadystate <file>`: Export steady-state probabilities to a file
- <a id="exporttransient"></a>`-exporttransient <file>`: Export transient probabilities to a file
- <a id="exportprism"></a>`-exportprism <file>`: Export final PRISM model to a file
- <a id="exportprismconst"></a>`-exportprismconst <file>`: Export final PRISM model with expanded constants to a file

<a id="export-options"></a>
## Export options

- <a id="exportmodelprecision"></a>`-exportmodelprecision <n>`: Export probabilities/rewards with n significant decimal places
- <a id="noexportheaders"></a>`-noexportheaders`: Don't include headers when exporting rewards

<a id="engines"></a>
## Engines and methods

- <a id="mtbdd"></a><a id="m"></a>`-mtbdd` (or `-m`): Use the MTBDD engine
- <a id="sparse"></a><a id="s"></a>`-sparse` (or `-s`): Use the Sparse engine
- <a id="hybrid"></a><a id="h"></a>`-hybrid` (or `-h`): Use the Hybrid engine \[default\]
- <a id="explicit"></a><a id="ex"></a>`-explicit` (or `-ex`): Use the explicit engine
- <a id="exact"></a>`-exact`: Perform exact (arbitrary precision) model checking
- <a id="ptamethod"></a>`-ptamethod <name>`: Specify PTA engine (games, digital, backwards) \[default: games\]

  Valid options: digital, games, backwards, bw.

- <a id="transientmethod"></a>`-transientmethod <name>`: CTMC transient analysis method (unif, fau) \[default: unif\]

  Valid options: unif, fau.

- <a id="heuristic"></a>`-heuristic <mode>`: Automatic choice of engines/settings (none, speed, memory) \[default: none\]

  Valid options: none, speed, memory.

<a id="construction"></a>
## Model construction

- <a id="fixdl"></a>`-fixdl`: Automatically put self-loops in deadlock states \[default\]
- <a id="nofixdl"></a>`-nofixdl`: Do not automatically put self-loops in deadlock states
- <a id="noprobchecks"></a>`-noprobchecks`: Disable checks on model probabilities/rates
- <a id="buildall"></a>`-buildall`: Build/store all labels and rewards during model construction

<a id="linear-equations"></a>
## Solution methods (linear equations)

- <a id="power"></a><a id="pow"></a><a id="pwr"></a>`-power` (or `-pow`, `-pwr`): Use the Power method for numerical computation
- <a id="jacobi"></a><a id="jac"></a>`-jacobi` (or `-jac`): Use Jacobi for numerical computation \[default\]
- <a id="gaussseidel"></a><a id="gs"></a>`-gaussseidel` (or `-gs`): Use Gauss-Seidel for numerical computation
- <a id="bgaussseidel"></a><a id="bgs"></a>`-bgaussseidel` (or `-bgs`): Use Backwards Gauss-Seidel for numerical computation
- <a id="pgaussseidel"></a><a id="pgs"></a>`-pgaussseidel` (or `-pgs`): Use Pseudo Gauss-Seidel for numerical computation
- <a id="bpgaussseidel"></a><a id="bpgs"></a>`-bpgaussseidel` (or `-bpgs`): Use Backwards Pseudo Gauss-Seidel for numerical computation
- <a id="jor"></a>`-jor`: Use JOR for numerical computation
- <a id="sor"></a>`-sor`: Use SOR for numerical computation
- <a id="bsor"></a>`-bsor`: Use Backwards SOR for numerical computation
- <a id="psor"></a>`-psor`: Use Pseudo SOR for numerical computation
- <a id="bpsor"></a>`-bpsor`: Use Backwards Pseudo SOR for numerical computation
- <a id="omega"></a>`-omega <x>`: Set over-relaxation parameter (for JOR/SOR/...) \[default: 0.9\]

<a id="mdp-methods"></a>
## Solution methods (MDPs)

- <a id="valiter"></a>`-valiter`: Use value iteration for solving MDPs \[default\]
- `-gaussseidel` (or `-gs`): Use Gauss-Seidel value iteration for solving MDPs
- <a id="politer"></a>`-politer`: Use policy iteration for solving MDPs
- <a id="modpoliter"></a>`-modpoliter`: Use modified policy iteration for solving MDPs
- <a id="intervaliter"></a><a id="ii"></a>`-intervaliter[:<options>]` (or `-ii`): Use interval iteration to solve MDPs/MCs (see -help -ii)

  Use interval iteration to solve MDPs/MCs.

  If provided, \<options\> is a comma-separated list of options taken from:

  - `boundmethod` (=default/variant-1-coarse/variant-1-fine/variant-2/dsmpi): Select upper bound heuristic for reward computations
  - `lower` (=\<d\>): Manually specify lower bound for reward computations
  - `upper` (=\<d\>): Manually specify upper bound for reward computations
  - `[no]boundverbose`: Verbose output for upper bound computations (default = no)
  - `[no]selectmidpoint`: Select midpoint between upper and lower as the result (default = yes)
  - `[no]monotonicbelow`: Enforce monotonicity in iteration from below (default = yes)
  - `[no]monotonicabove`: Enforce monotonicity in iteration from above (default = yes)
  - `[no]checkmonotonic`: Check monotonicity, for testing (default = no)

  Example: boundmethod=default,upper=3.0,noselectmidpoint,checkmonotonic

- <a id="topological"></a>`-topological`: Use topological value iteration

<a id="solution-settings"></a>
## Solution method settings

- <a id="relative"></a><a id="rel"></a>`-relative` (or `-rel`): Use relative error for detecting convergence \[default\]
- <a id="absolute"></a><a id="abs"></a>`-absolute` (or `-abs`): Use absolute error for detecting convergence
- <a id="epsilon"></a><a id="e"></a>`-epsilon <x>` (or `-e`): Set value of epsilon (for convergence check) \[default: 1e-6\]
- <a id="maxiters"></a>`-maxiters <n>`: Set max number of iterations \[default: 10000\]
- <a id="gridresolution"></a>`-gridresolution <n>`: Set resolution for fixed grid approximation (POMDP) \[default: 10\]

<a id="model-checking"></a>
## Model checking

- <a id="nopre"></a>`-nopre`: Skip precomputation algorithms (where optional)
- <a id="noprob0"></a>`-noprob0`: Skip precomputation algorithm Prob0 (where optional)
- <a id="noprob1"></a>`-noprob1`: Skip precomputation algorithm Prob1 (where optional)
- <a id="noprerel"></a>`-noprerel`: Do not pre-compute/use predecessor relation, e.g. for precomputation
- <a id="fair"></a>`-fair`: Use fairness (for model checking of MDPs)
- <a id="nofair"></a>`-nofair`: Don't use fairness (for model checking of MDPs) \[default\]
- <a id="sumroundoff"></a>`-sumroundoff <x>`: Set probability sum threshold \[default: 1-e5\]
- <a id="nossdetect"></a>`-nossdetect`: Disable steady-state detection for CTMC transient computations
- <a id="sccmethod"></a>`-sccmethod <name>`: Specify (symbolic) SCC computation method (xiebeerel, lockstep, sccfind)

  Valid options: xiebeerel, lockstep, sccfind.

- <a id="symm"></a>`-symm <string>`: Symmetry reduction options string
- <a id="aroptions"></a>`-aroptions <string>`: Abstraction-refinement engine options string

  \<string\> is a comma-separated list of options regarding abstraction-refinement:

  - verbose=\<n\> (or v=\<n\>) - verbosity level
  - `refine=<where,how>`: which states to refine and how

  ```
      <where> = all, allmax, first, firstmax, last, lastmax
      <how> = all, val
  ```

  - epsilonref=\<x\> (or eref=\<x\>) - epsilon for refinement
  - nopre  - disable precomputation
  - `pre`: use precomputation
  - `noprob0`: disable prob0 precomputation
  - `noprob1`: disable prob1 precomputation
  - `epsilon=<x>`: epsilon for numerical convergence
  - `maxref=<n>`: maximum number of refinements
  - `opt`: use optimisations
  - `noopt`: disable optimisations
  - `exportdot`: export dot files for each refinement
  - `above`: start numerical soluton from above
  - `below`: start numerical soluton from below

- <a id="pathviaautomata"></a>`-pathviaautomata`: Handle all path formulas via automata constructions
- <a id="nodasimplify"></a>`-nodasimplify`: Do not attempt to simplify deterministic automata, acceptance conditions
- <a id="exportadv"></a>`-exportadv <file>`: Export an adversary from MDP model checking (as a DTMC)
- <a id="exportadvmdp"></a>`-exportadvmdp <file>`: Export an adversary from MDP model checking (as an MDP)
- <a id="ltl2datool"></a>`-ltl2datool <exec>`: Run executable \<exec\> to convert LTL formulas to deterministic automata
- <a id="ltl2dasyntax"></a>`-ltl2dasyntax <x>`: Specify output format for -ltl2datool switch (lbt, spin, spot, rabinizer)

  Valid options: lbt, spin, spot, rabinizer.

- <a id="exportiterations"></a>`-exportiterations`: Export vectors for iteration algorithms to file
- <a id="pmaxquotient"></a>`-pmaxquotient`: For Pmax computations in MDPs, compute in the MEC quotient

<a id="multi-objective"></a>
## Multi-objective model checking

- <a id="linprog"></a><a id="lp"></a>`-linprog` (or `-lp`): Use linear programming for MDP solution (explicit engine) and multi-objective model checking
- <a id="multimaxpoints"></a>`-multimaxpoints <n>`: Maximal number of corner points for (valiter-based) multi-objective
- <a id="paretoepsilon"></a>`-paretoepsilon <x>`: Threshold for Pareto curve approximation
- <a id="exportpareto"></a>`-exportpareto <file>`: When computing Pareto curves, export points to a file

<a id="output"></a>
## Output

- <a id="verbose"></a><a id="v"></a>`-verbose` (or `-v`): Verbose mode: print out state lists and probability vectors
- <a id="extraddinfo"></a>`-extraddinfo`: Display extra info about some (MT)BDDs
- <a id="extrareachinfo"></a>`-extrareachinfo`: Display extra info about progress of reachability

<a id="symbolic"></a>
## Sparse, hybrid and MTBDD engines

- <a id="nocompact"></a>`-nocompact`: Switch off "compact" sparse storage schemes
- <a id="sbl"></a>`-sbl <n>`: Set number of levels (for hybrid engine) \[default: -1\]
- <a id="sbmax"></a>`-sbmax <n>`: Set memory limit (KB) (for hybrid engine) \[default: 1024\]
- <a id="gsl"></a><a id="sorl"></a>`-gsl <n>` (or `-sorl`): Set number of levels for hybrid GS/SOR \[default: -1\]
- <a id="gsmax"></a><a id="sormax"></a>`-gsmax <n>` (or `-sormax`): Set memory limit (KB) for hybrid GS/SOR \[default: 1024\]
- <a id="cuddmaxmem"></a>`-cuddmaxmem <n>`: Set max memory for CUDD package, e.g. 125k, 50m, 4g \[default: 1g\]
- <a id="cuddepsilon"></a>`-cuddepsilon <x>`: Set epsilon value for CUDD package \[default: 1e-15\]
- <a id="ddsanity"></a>`-ddsanity`: Enable internal sanity checks (causes slow-down)
- <a id="ddextrastatevars"></a>`-ddextrastatevars <n>`: Set the number of preallocated state vars \[default: 20\]
- <a id="ddextraactionvars"></a>`-ddextraactionvars <n>`: Set the number of preallocated action vars \[default: 20\]

<a id="parametric"></a>
## Parametric model checking

- <a id="param"></a>`-param <vals>`: Do parametric model checking with parameters (and ranges) \<vals\>
- <a id="paramprecision"></a>`-paramprecision <x>`: Set max undecided region for parameter synthesis \[default: 5/100\]
- <a id="paramsplit"></a>`-paramsplit <name>`: Set method to split parameter regions (longest,all) \[default: longest\]

  Valid options: longest, all.

- <a id="parambisim"></a>`-parambisim <name>`: Set bisimulation minimisation for parameter synthesis (weak,strong,none) \[default: weak\]

  Valid options: strong, weak, none.

- <a id="paramfunction"></a>`-paramfunction <name>`: Set function representation for parameter synthesis (jascached,jas) \[default: jascached\]

  Valid options: jascached, jas, dag.

- <a id="paramelimorder"></a>`-paramelimorder <name>`: Set elimination order for parameter synthesis (arb,fw,fwrev,bw,bwrev,rand) \[default: bw\]

  Valid options: arb, fw, fwrev, bw, bwrev, rand.

- <a id="paramrandompoints"></a>`-paramrandompoints <n>`: Set number of random points to evaluate per region \[default: 5\]
- <a id="paramsubsumeregions"></a>`-paramsubsumeregions <b>`: Subsume adjacent regions during analysis \[default: true\]
- <a id="paramdagmaxerror"></a>`-paramdagmaxerror <b>`: Maximal error probability allowed for DAG function representation \[default: 1E-100\]

<a id="fau"></a>
## Fast adaptive uniformisation (FAU)

- <a id="fauepsilon"></a>`-fauepsilon <x>`: Set probability threshold of birth process in FAU \[default: 1e-6\]
- <a id="faudelta"></a>`-faudelta <x>`: Set probability threshold for irrelevant states in FAU \[default: 1e-12\]
- <a id="fauarraythreshold"></a>`-fauarraythreshold <x>`: Set threshold when to switch to sparse matrix in FAU \[default: 100\]
- <a id="fauintervals"></a>`-fauintervals <x>`: Set number of intervals to divide time intervals into for FAU \[default: 1\]
- <a id="fauinitival"></a>`-fauinitival <x>`: Set length of additional initial time interval for FAU \[default: 1.0\]

<a id="simulation"></a>
## Simulation

- <a id="sim"></a>`-sim`: Use the PRISM simulator to approximate results of model checking
- <a id="simmethod"></a>`-simmethod <name>`: Specify the method for approximate model checking (ci, aci, apmc, sprt)

  Valid options: ci, aci, apmc, sprt.

- <a id="simsamples"></a>`-simsamples <n>`: Set the number of samples for the simulator (CI/ACI/APMC methods)
- <a id="simconf"></a>`-simconf <x>`: Set the confidence parameter for the simulator (CI/ACI/APMC methods)
- <a id="simwidth"></a>`-simwidth <x>`: Set the interval width for the simulator (CI/ACI methods)
- <a id="simapprox"></a>`-simapprox <x>`: Set the approximation parameter for the simulator (APMC method)
- <a id="simmanual"></a>`-simmanual`: Do not use the automated way of deciding whether the variance is null or not
- <a id="simvar"></a>`-simvar <n>`: Set the minimum number of samples to know the variance is null or not
- <a id="simmaxrwd"></a>`-simmaxrwd <x>`: Set the maximum reward -- useful to display the CI/ACI methods progress
- <a id="simpathlen"></a>`-simpathlen <n>`: Set the maximum path length for the simulator
<!-- end generated -->
