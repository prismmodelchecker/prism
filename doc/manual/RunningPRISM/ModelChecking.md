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

When PRISM prints the result of a (numerical) model checking query,
it typically shows an indication of the accuracy too:

```shell
prism model.prism model.props
# ...
# Result: 0.15625 (exact floating point)
# ...
# Result: 0.16666650772094727 (+/- 1.1920928955078125E-6 estimated; rel err 7.1525641942636435E-6)
```

The first result is exact, up to the precision of floating point arithmetic.
The second was computed by an iterative method, and its error bound is only an estimate,
based on the check for [convergence](../ConfiguringPRISM/SolutionMethodsAndOptions.md#convergence).

The default methods in most cases are iterative numerical methods
implemented using floating point arithmetic.
The accuracy is impacted by both the method chosen and parameters such as the threshold for termination;
these are discussed more in the "[Configuration and performance](../ConfiguringPRISM/README.md)" section.

We also note here that PRISM has some support for **exact** model checking,
i.e., using arbitrary precision arithmetic to provide exact numerical values.
This is implemented as a special case of [parametric model checking](ParametricModelChecking.md),
which limits its application to relatively small models.
It can be used for analysing DTMCs/CTMCs (unbounded until, steady-state probabilities, reachability reward and steady-state reward) or MDPs (unbounded until and reachability rewards). You can enable this functionality using the "Do exact model checking" option in the GUI or using switch [`-exact`](../Reference/CommandLineSwitches.md#exact) from the command line.
Results computed in this way are shown as fractions, e.g., `1/6 (exact)`.
