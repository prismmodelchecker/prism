# Memory and model size

Probabilistic model checking can need a lot of memory.
This page describes how PRISM uses memory, and how to give it more.

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

If PRISM runs out of memory, see "[Memory problems](../FrequentlyAskedQuestions/MemoryProblems.md)" in the FAQ.
