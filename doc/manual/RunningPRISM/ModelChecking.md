# Model checking

Properties to be checked on a model are specified as described in the "[Property specification](../PropertySpecification/README.md)" section,
and are usually kept in [properties files](../PropertySpecification/PropertiesFiles.md) with extension `.props`.
There are properties files accompanying most of the sample PRISM models in the `prism-examples` directory.

<a id="gui"></a>
## GUI

A properties file can be loaded into the GUI along with a model when starting it, e.g.:

```shell
xprism example.prism example.props
```

Otherwise, after first loading a model, select menu option "Properties | Open properties list".
The file can only be loaded if there are no errors, otherwise an error is displayed.
Note that it may be necessary to have loaded the corresponding model first,
since the properties will probably make reference to labels, variables or constants declared in the model file.
Once loaded, the properties contained in the file are displayed in a list in the "Properties" tab of the GUI.
Additional constants and labels defined in the properties file are displayed in separate lists below.
You can modify or create new properties, constants and labels from the GUI,
by right-clicking on the appropriate list and selecting from the pop-up menu which appears. Properties with errors are shaded red and marked with a warning sign.
Positioning the mouse pointer over the property displays the corresponding error message.

The pop-up menu for the properties list also contains a "Verify" option,
which allows you to instruct PRISM to model check the currently selected properties
(hold down Ctrl/Cmd to select more than one property simultaneously).
All properties can be model checked at once by selecting "Verify all".
PRISM verifies each property individually.
Upon completion, the icon next to the property changes according to the result of model checking. For Boolean-valued properties, a result of true or false is indicated by a green tick or red cross, respectively. For properties which have a numerical result (e.g. `P=? [ ...]`), position the mouse pointer over the property to view the result.
In addition, this and further information about model checking is displayed in the log in the "Log" tab.

<a id="cl"></a>
## Command-line

From the command-line, model checking is achieved by passing both a model file and a properties file as arguments, e.g.:

```shell
prism poll2.prism poll.props -const T=10
```

Here, [`poll2.prism`](../files/poll2.prism) is a CTMC model of a simple polling system
and the properties file [`poll.props`](../files/poll.props) has a constant `T` with no value,
which is given here with the [`-const`](../Reference/CommandLineSwitches.md#const) switch (see "[Experiments](Experiments.md)").
The results of model checking are sent to the display and are as described above for the GUI version.
By default, all properties in the file are checked.
To model check only a single property, use the `-prop` switch.
For example, to check only the fourth property in the file:

```shell
prism poll2.prism poll.props -prop 4
```

Properties can also be referred to by [name](../PropertySpecification/PropertiesFiles.md#names).
For example, if a properties file `model.props` contains a property named "safe", it can be checked on its own with:

```shell
prism model.prism model.props -prop safe
```

You can also provide a comma-separated list of multiple properties to check,
using numerical indices, property names or a mix of the two:

```shell
prism model.prism model.props -prop 4,5,safe
```

Alternatively, the contents of a properties file can be specified directly from the command-line, using the `-pf` switch:

```shell
prism poll2.prism -pf 'P>=0.5 [ F<=5 (s=1 & a=0) ]'
```

The switches `-pctl` and `-csl` are aliases for `-pf`.

Note the use of single quotes (`'...'`) to avoid characters such as
`(` and `>` being interpreted by the command-line shell.
Single quotes may be preferable to double quotes since PRISM properties often include double quotes, e.g., for references to labels or properties.

<a id="accuracy"></a>
## Accuracy of results

The default methods used for model checking in most cases are iterative numerical methods
implemented using floating point arithmetic.
The **accuracy** of the resulting values is impacted by both the method chosen and parameters such as the threshold for termination;
these are discussed more in the "[Configuration and performance](../ConfiguringPRISM/README.md)" section.

When PRISM prints the result of a (numerical) model checking query,
it typically shows an indication of the accuracy too, for example:

```shell
prism model.prism model.props
# ...
# Result: 0.15625 (exact floating point)
# ...
# Result: 0.1666666865348816 (+/- 1.1920933218335556E-7; rel err 7.152559078349847E-7)
# ...
# Result: 0.16666650772094727 (+/- 1.1920928955078125E-6 estimated; rel err 7.1525641942636435E-6)
```

The first result is exact, up to the precision of floating point arithmetic.,
in this case because it was the result of a [step-bounded](../PropertySpecification/ThePOperator.md#bounded) (finite-horizon) query needing a fixed number of iterations to compute.
The second was computed using [interval iteration](../ConfiguringPRISM/SolutionMethodsAndOptions.md#interval-iteration),
which is an iterative method but that provides a guaranteed error bound on the result.
The third was computed by [value iteration](../ConfiguringPRISM/SolutionMethodsAndOptions.md#value-iteration), and its error bound is only an estimate,
based on the check for [convergence](../ConfiguringPRISM/SolutionMethodsAndOptions.md#convergence).

Here is an example of the output when using the [exact](../ConfiguringPRISM/ComputationEngines.md#exact) engine,
which uses arbitrary precision arithmetic to compute precise results:

```shell
prism model.prism model.props -ex
# ...
# Result: 1/6 (exact) (~0.16666666666666666)
```

An approximate decimal value is also shown, for convenience.
