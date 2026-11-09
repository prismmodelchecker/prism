# Other options

## Output options

To increase the amount of information displayed by PRISM (in particular, to display lists of states and probability vectors), you can use the "Verbose output" option (activated with command-line switch `-verbose` or `-v`). To display additional statistics about MTBDDs after model construction, use the "Extra MTBDD information" option (switch `-extraddinfo`) and, to view MTBDD sizes during the process of reachability, use option "Extra reachability information" (switch `-extrareachinfo`).

## Fairness

Sometimes, model checking of properties for MDPs requires fairness constraints to be taken into account.
See e.g. [[BK98](../Main/References.md#BK98)],[[Bai98](../Main/References.md#Bai98)] for more information.
To enable the use of fairness constraints (for `P` operator properties), use the `-fair` switch.

## Probability/rate checks

By default, when constructing a model, PRISM checks that all probabilities and rates are within acceptable ranges (i.e. are between 0 and 1, or are non-negative, respectively). For DTMCs and MDPs, it also checks that the probabilities sum up to one for each command. These checks are often very useful for highlighting user modelling errors and it is strongly recommended that you keep them enabled, however if you need to disable them you can do so via option "do prob checks?" in the GUI or command-line switch `-noprobchecks`.
You can also change the level of precision used to check that probabilities sum to 1 using the option "Probability sum threshold" (or command-line switch `-sumroundoff`.

## CUDD memory

CUDD, the underlying BDD and MTBDD library used in PRISM has an upper memory limit. By default, this limit is 1 GB. If you are working on a machine with significantly more memory than this and PRISM runs out of memory when model checking, it may help to change this. To set the limit, from the command-line, use the `-cuddmaxmem` switch. For example:

```shell
prism -cuddmaxmem 2g big_model.pm
```

Above, `g` denotes GB. You can also use `m` for MB.
You can also change the CUDD maximum memory setting from the options panel in the GUI, but you will need to close and restart the GUI (saving the settings as you do) for this to take effect.

<a id="javamaxmem"></a>

## Java memory

The Java virtual machine (JVM) used to execute PRISM also has upper memory limits. Sometimes this limit will be exceeded and you will see an error of the form `java.lang.OutOfMemory`. To resolve this problem, you can increase this memory limit. On Unix, Linux or Mac OS X platforms, this can done by using the `-javamaxmem` switch, passed either to the command-line script `prism` or the GUI launcher `xprism`. For example:

```shell
prism -javamaxmem 4g big_model.pm
xprism -javamaxmem 4g big_model.pm
```

each set the limit to 4GB. Alternatively, you set the environment variable PRISM_JAVAMAXMEM before running PRISM. For example, under a `bash` shell:

```shell
PRISM_JAVAMAXMEM=4g
export PRISM_JAVAMAXMEM
prism big_model.pm
```

If you get an error of the form `java.lang.StackOverflowError`, then you can try increasing the stack size of the JVM.
On Unix, Linux or Mac OS X platforms, this can done by using the `-javastack` switch or the `PRISM_JAVASTACKSIZE` environment variable.
Examples are:

```shell
prism -javastack 1g big_model.pm
xprism -javastack 1g big_model.pm
```

or:

```shell
PRISM_JAVASTACKSIZE=1g
export PRISM_JAVASTACKSIZE
prism big_model.pm
```

If you are running PRISM on Windows you will have to do make adjustments to Java memory manually, by modifying the `prism.bat` or `xprism.bat` scripts.
To set the memory to 4GB, for example, add `-Xmx4g` to the list of arguments in the call to `java` or `javaw` at the end of the file.
To change the stack size to 1GB, add `-Xss1g`.

## Other Java options

If you want to pass additional switches to the JVM used to run PRISM, you can use the `-javaparams` switch.
For example:

```shell
prism -javaparams "-XX:AutoBoxCacheMax=100000000 -Xmn2g" -javamaxmem 12g
```

## Precomputation

By default, PRISM's probabilistic model checking algorithms use an initial *precomputation* step which uses graph-based techniques to efficient detect trivial cases where probabilities are 0 or 1. This can often result in improved performance and also reduce round-off errors. Occasionally, though, you may want to disable this step for efficiency (e.g. if you know that there are no/few such states and the precomputation process is slow). This can be done with the `-nopre` switch. You can also disable the individual algorithms for probability 0/1 using switches `-noprob0` and `-noprob1`.

## Time-outs

The command-line version of PRISM has a time-out option, specified using the switch `-timeout <n>`.
This causes the program to exit after `<n>` seconds if it has not already terminated by that point.
This is particularly useful for benchmarking scenarios where you wish to ignore runs of PRISM that exceed a certain length of time.
