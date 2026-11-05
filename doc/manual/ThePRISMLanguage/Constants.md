# Constants

PRISM supports the use of *constants*, as seen in [Example 2](Example2.md).
Constants can be integers, doubles or Booleans
and can be defined using literal values or as constant expressions (including in terms of each other) using the `const`
keyword. For example:

```prism
const int radius = 12;
const double pi = 3.141592;
const double area = pi * radius * radius;
const bool yes = true;
```

The identifiers used for their names are subject to the same rules as [variables](ModulesAndVariables.md).

Constants can be used anywhere that a constant value would be expected,
such as the lower or upper range of a variable (e.g. `N` in [Example 2](Example2.md)),
the probability or rate associated with an update (`mu` in [Example 2](Example2.md)),
or anywhere in a guard or update.
As will be described later constants can also be left undefined
and specified later, either to a single value or a range of values, using [experiments](../RunningPRISM/Experiments.md).

**Note:** For the sake of backward-compatibility, the notation used in earlier versions of PRISM
(`const` for `const int` and `rate` or `prob` for `const double`) is still supported.
