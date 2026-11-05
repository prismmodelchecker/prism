# Experiments

PRISM supports *experiments*, which is a way of automating multiple instances of model checking.
This is done by leaving one or more [constants](../ThePRISMLanguage/Constants.md) undefined, e.g.:

```prism
const int N;
const double T;
```

This can be done for constants in the model file, the properties file, or both.
Before any verification can be performed, values must be provided for any such constants. In the GUI, a dialog appears in which the user is required to enter values. From the command line, the `-const` switch must be used, e.g.:

```shell
prism cluster.sm cluster.csl -const N=4,T=85.9
```

To run an experiment, provide a *range* of values for one or more of the constants. Model checking will be performed for all combinations of the constant values provided. For example:

```shell
prism cluster.sm cluster.csl -const N=4:6,T=60:10:100
```

where `N=4:6` means that values of 4,5 and 6 are used for `N`,
and `T=60:10:100` means that values of 60, 70, 80, 90 and 100 (i.e. steps of 10) are used for `T`.

For convenience, constant specifications can be split across separate instances of the `-const` switch, if desired.
You can also specify double-valued constants as fractions rather than decimals. For example:

```shell
prism cluster.sm cluster.csl -const N=4,T=85.9 -const p=1/3
```

From the GUI, the same thing can be achieved by selecting a single property,
right clicking on it and selecting "New experiment"
(or alternatively using the popup menu in the "Experiments" panel).
Values or ranges for each undefined constant can then be supplied in the resulting dialog.
Details of the new experiment and its progress are shown in the panel.
To stop the experiment before it has completed, click the red "Stop" button and it will
halt after finishing the current iteration of model checking.
Once the experiment has finished, right clicking on the experiment produces a pop-up menu,
from which you can view the results of the experiment or export them to a file.

For experiments based on properties which return numerical results, you can also use the GUI to plot graphs of the results.
This can be done either before the experiment starts, by selecting the "Create graph" tick-box in the dialog used to create the experiment
(in fact this box is ticked by default), or after the experiment's completion, by choosing "Plot results" from the pop-up menu on the experiment.
A dialog appears, where you can choose which constant (if there are more than one) to use for the x-axis of the graph,
and for which values of any other constants the results should be plotted.
The graph will appear in the panel below the list of experiments.
Right clicking on a graph and selecting "Graph options" brings up a dialog from which many properties of the graph can be configured.
From the pop-up menu of a graph, you can also choose to print the graph (to a printer or Postscript file)
or export it in a variety of formats:
as an image (PNG or JPEG),
as an encapsulated Postscript file (EPS),
in an XML-based format (for reloading back into PRISM),
or as code which can be used to generate the graph in Matlab.

Approximate computation of quantitive results obtained with the [simulator](StatisticalModelChecking.md) can also be used on experiments. In the GUI, select the "Use Simulation" option when defining the parameters for the experiment. From the command-line, just add the `-sim` switch as usual.

<a id="exportresults"></a>

## Exporting results

You can export all the results from an experiment to a file or to the screen. From the command-line, use the `-exportresults` switch, for example:

```shell
prism cluster.sm cluster.csl -prop 4 -const N=4:5,T=0:10:20 -exportresults res.txt
```

to send to output file `res.txt`, or:

```shell
prism cluster.sm cluster.csl -prop 4 -const N=4:5,T=0:10:20 -exportresults stdout
```

to send the results straight to the screen. From the GUI, right click on the experiment and select "Export results".

The default behaviour is to export a *list* of results in *text* form, using tabs to separate items. The above examples produce:

```text
N	T	Result
4	0	0.0
4	10	4.707364688019771E-6
4	20	1.3126420636755292E-5
5	0	0.0
5	10	3.267731327728599E-6
5	20	8.343575060356386E-6
```

You can change the format in which the results are exported by appending one or more comma-separated options to the end of the `-exportresults` switch, for example to export in CSV (comma-separated values) format:

```shell
prism cluster.sm cluster.csl -prop 4 -const N=4:5,T=0:10:20 -exportresults res.txt:csv
```

```text
N, T, Result
4, 0, 0.0
4, 10, 4.707364688019771E-6
4, 20, 1.3126420636755292E-5
5, 0, 0.0
5, 10, 3.267731327728599E-6
5, 20, 8.343575060356386E-6
```

or in [DataFrame](https://pandas.pydata.org/docs/reference/api/pandas.DataFrame.html) format:

```shell
prism cluster.sm cluster.csl -prop 4 -const N=4:5,T=0:10:20 -exportresults res.txt:dataframe
```

```text
N,T,Result
4,0,0
4,10,4.70736468802e-06
4,20,1.31264206368e-05
5,0,0
5,10,3.26773132773e-06
5,20,8.34357506036e-06
```

You can also add the `matrix` option, to export the results as one or more 2D matrices, rather than a list.
This is particularly useful if you want to create a surface plot from results that vary over two constants.

```shell
prism cluster.sm cluster.csl -prop 4 -const N=4:5,T=0:10:20 -exportresults res.txt:csv,matrix
```

```text
"N\T"
, 0.0, 10.0, 20.0
4, 0.0, 4.707364688019771E-6, 1.3126420636755292E-5
5, 0.0, 3.267731327728599E-6, 8.343575060356386E-6
```

The `matrix` option is also available in normal (non-CSV) mode.

You can also export results in the form of comments, used by PRISM's [regression testing](https://github.com/prismmodelchecker/prism/wiki/Regression-Testing) functionality:

```shell
prism cluster.sm cluster.csl -prop 4 -const N=4:5,T=0:10:20 -exportresults res.txt:comment
```

```prism
// RESULT (N=4,T=0): 0.0
// RESULT (N=4,T=10): 4.707364688019771E-6
// RESULT (N=4,T=20): 1.3126420636755292E-5
// RESULT (N=5,T=0): 0.0
// RESULT (N=5,T=10): 3.267731327728599E-6
// RESULT (N=5,T=20): 8.343575060356386E-6
```

From the GUI, it is also possible to *import* previously exported results (in [DataFrame](https://pandas.pydata.org/docs/reference/api/pandas.DataFrame.html) format).

A related option is the `-exportvector <file>` switch, useful in general contexts, not for experiments.
This exports the results for all states of the model
(typically, the log just displays the result for the initial state, unless a [filter](../PropertySpecification/Filters.md) has been used)
to the the file `file`.
