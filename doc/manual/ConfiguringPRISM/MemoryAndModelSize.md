# Memory and model size

Probabilistic model checking can need a lot of memory.
This page describes how PRISM uses memory, how to give it more,
and what to do when it runs out.

<a id="settings"></a>
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

<a id="memory-problems"></a>
## Memory problems

<a id="crash"></a>
### PRISM crashed or stopped responding. Why?

When PRISM crashes, the most likely cause is that it has run out of memory.
Similarly, if PRISM (or the machine you are running it on) becomes very slow or seems to have stopped responding, this is probably because it is using too much of your machine's memory. Probabilistic model checking, much like other formal verification techniques, can be a very resource-intensive process. It is very easy to create a seemingly simple PRISM model that requires a large amount of time and/or memory to construct and analyse. See the other questions on this page for tips on how to avoid this.

The other possibility is that you have found a bug.
If PRISM crashes or freezes whilst not using all/most of the available memory (you can check this with the `top` command in a Unix/Linux terminal or the Task Manager (via Ctrl-Alt-Delete) on Windows) then please file a [bug report](https://github.com/prismmodelchecker/prism/issues).

<a id="memout"></a>
### I ran out of memory. What can I do?

It depends. First, you need to establish at what point in PRISM's operation, you ran out of memory. If you are running the command-line version of PRISM then the output from the tool so far should give an indication of this. If using the GUI, check the log tab for this information. If PRISM crashed because of its memory usage, the error message can be helpful. If using the GUI, you may need to start the GUI from the command-line to see any error messages.

The two main steps that PRISM typically has to perform are:

1. **Model construction** (conversion of a PRISM language description to the corresponding probabilistic model)
1. **Model checking/analysis** (processing/analysis of a constructed probabilistic model in order to determine the result of a property or to compute steady-state/transient probabilities)

Memory usage issues for each of these steps are discussed in separate sections below. In some cases the process performed prior to step 1 (**model parsing** - reading in a model description in the PRISM language and checking it for correctness) can also be resource intensive. This is also discussed below.

If you are using the **simulator** to generate approximate model checking results then step 1 (model construction) is not performed and step 2 is carried out very differently. Memory consumption is not usually a problem in this case.

<a id="memout_constr"></a>
### I ran out of memory during model construction. What can I do?

If PRISM has already output this:

```shell
#Building model...
```

but there is no line of the form:

```shell
#Time for model construction: 34.3 seconds.
```

and then you get an error like this:

```shell
##
## An unexpected error has been detected by Java Runtime Environment:
##
##  SIGSEGV (0xb) at pc=0xb5249323, pid=19298, tid=3086363536
##
## Java VM: Java HotSpot(TM) Client VM (1.6.0-b105 mixed mode, sharing)
## Problematic frame:
## C  [libdd.so+0x39323]  Cudd_Ref+0xf
##
## An error report file with more information is saved as hs_err_pid19298.log
##
## If you would like to submit a bug report, please visit:
##   http://java.sun.com/webapps/bugreport/crash.jsp
##
#/home/dxp/bin/prism: line 50: 19298 Aborted                 "$PRISM_JAVA" #$PRISM_JAVAMAXMEM -Djava.awt.headless=$PRISM_HEADLESS -Djava.library.path=$PRISM_DIR/lib -classpath "$PRISM_CLASSPATH" $PRISM_MAINCLASS "$@"
```

or like this:

```shell
##
## An unexpected error has been detected by HotSpot Virtual Machine:
##
## EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x0b1c7da3, pid=2884, tid=2544
##
## Java VM: Java HotSpot(TM) Client VM (1.5.0_06-b05 mixed mode)
## Problematic frame:
## C [dd.dll+0x7da3]
##
#...
```

then PRISM ran out of memory whilst trying to construct the model.
Model construction in PRISM is performed using BDDs (binary decision diagrams) and MTBDDs (multi-terminal) BDDs which are implemented in the CUDD library.
The first thing to try in this case is to increase the amount of memory available to CUDD. See the entry "CUDD memory" in the section "[Configuring PRISM - Other Options](../ConfiguringPRISM/OtherOptions.md)" for details of this.

If increasing this memory limit does not resolve the problem, then you will need to consider ways to reduce the size of your model. You can find some tips on this in the [PRISM modelling](../FrequentlyAskedQuestions/PRISMModelling.md) section. Bear in mind also that if you are having to increase the CUDD memory limit too high (e.g. close to the physical memory available on your computer) just for model construction, then it is unlikely that you will have enough memory for subsequent model checking operations.

Finally, it is also worth considering the ordering of the modules and variables in your model since this can have a (in some cases dramatic) effect on the size of MTBDD representation of the model. This topic is covered in the "[PRISM modelling](../FrequentlyAskedQuestions/PRISMModelling.md)" section of the FAQ.

<a id="memout_check"></a>
### I ran out of memory during model checking. What can I do?

If model construction was successfully completed (see previous question) but model checking was not, there are several things you can try. First of all, if the error message you see looks like the one in the previous question or you see a message such as

```shell
#DD_MatrixMultiply: res is NULL
```

then it may be worth increasing the memory limit for CUDD (as described above). However, if you see an error more like this:

```shell
#/home/dxp/bin/prism: line 50:  3139 Aborted                 "$PRISM_JAVA" $PRISM_JAVAMAXMEM -Djava.awt.headless=$PRISM_HEADLESS -Djava.library.path=$PRISM_DIR/lib -classpath "$PRISM_CLASSPATH" $PRISM_MAINCLASS "$@"
```

then increasing the memory CUDD probably will not help - PRISM is just trying to allocate more memory than is physically available on your system.

Here are some general tips:

- Try experimenting with using the different *engines* in PRISM. Read the section "[Configuring PRISM - Computation Engines](../ConfiguringPRISM/ComputationEngines.md)" for details.

- Look at the detailed output of PRISM for information about memory usage. If you are using the hybrid (or sparse) engine and the limiting factor in terms of memory is creation of the vectors, then you have no choice but to try and reduce the size (number of states) of your model. If you are using the MTBDD engine, it is also well worth considering the variable ordering of your model. Both topics are discussed in the "[PRISM modelling](../FrequentlyAskedQuestions/PRISMModelling.md)" section of the FAQ.

- Finally, if you can find no way to reduce the size of your model and are happy to consider an approximate (rather than exact) analysis, you may wish to try using PRISM's [discrete-event simulation engine](../RunningPRISM/StatisticalModelChecking.md) for analysis.

<a id="memout_parse"></a>
### I ran out of memory during model parsing. What can I do?

This is a less common problem and will only occur if the actual PRISM language description of your model is very large. This may be the case, for example, if you are automatically generating PRISM models in some way. Errors due to lack of memory during parsing usually look like:

```shell
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

or:

```shell
Exception in thread "main" java.lang.StackOverflowError
```

You can resolve this problem by increasing the memory allocated to Java.
See the entry "Java memory" in the section "[Configuring PRISM - Other Options](../ConfiguringPRISM/OtherOptions.md)" for details of this.
