# Model Checking

Typically, once a model has been constructed, it is analysed through model checking.
Properties are specified as described in the "[Property Specification](../PropertySpecification/Introduction.md)" section,
and are usually kept in files with extensions `.props`, `.pctl` or `.csl`.
There are properties files accompanying most of the sample PRISM models in the `prism-examples` directory.

<a id="gui"></a>

## GUI

To load a file containing properties into the GUI, select menu option "Properties | Open properties list".
The file can only be loaded if there are no errors, otherwise an error is displayed.
Note that it may be necessary to have loaded the corresponding model first,
since the properties will probably make reference to variables (and perhaps constants) declared in the model file.
Once loaded, the properties contained in the file are displayed in a list in the "Properties" tab of the GUI.
Constants and labels are displayed in separate lists below.
You can modify or create new properties, constants and labels from the GUI,
by right-clicking on the appropriate list and selecting from the pop-up menu which appears. Properties with errors are shaded red and marked with a warning sign.
Positioning the mouse pointer over the property displays the corresponding error message.

The pop-up menu for the properties list also contains a "Verify" option,
which allows you instruct PRISM to model check the currently selected properties
(hold down Ctrl/Cmd to select more than one property simultaneously).
All properties can be model checked at once by selecting "Verify all".
PRISM verifies each property individually.
Upon completion, the icon next to the property changes according to the result of model checking. For Boolean-valued properties, a result of true or false is indicated by a green tick or red cross, respectively. For properties which have a numerical result (e.g. `P=? [ ...]`), position the mouse pointer over the property to view the result.
In addition, this and further information about model checking is displayed in the log in the "Log" tab.

<a id="cl"></a>

## Command-line

From the command-line, model checking is achieved by passing both a model file and a properties file as arguments, e.g.:

```shell
prism poll2.sm poll.csl
```

The results of model checking are sent to the display and are as described above for the GUI version.
By default, all properties in the file are checked.
To model check only a single property, use the `-prop` switch.
For example, to check only the fourth property in the file:

```shell
prism poll2.sm poll.csl -prop 4
```

or to check only the property with [name](../PropertySpecification/PropertiesFiles.md#names) "safe" in the file:

```shell
prism poll2.sm poll.csl -prop safe
```

You can also provide a comma-separated list of multiple properties to check,
using neither numerical indices or property names:

```shell
prism poll2.sm poll.csl -prop 4,5,safe
```

Alternatively, the contents of a properties file can be specified directly from the command-line, using the `-pf` switch:

```shell
prism poll2.sm -pf 'P>=0.5 [ true U<=5 (s=1 & a=0) ]'
```

The switches `-pctl` and `-csl` are aliases for `-pf`.

Note the use of single quotes (`'...'`) to avoid characters such as
`(` and `>` being interpreted by the command-line shell.
Single quotes are preferable to double quotes since PRISM properties often include double quotes, e.g. for references to labels or properties.
