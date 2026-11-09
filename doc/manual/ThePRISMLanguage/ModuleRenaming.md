# Module renaming

PRISM also supports *module renaming*, which allows duplication of modules.
In [Example 1](Example1.md), module `M2` is identical to module `M1` so we can in fact replace its entire definition with:

```prism
module M2 = M1 [ x=y, y=x ] endmodule
```

All of the variables in the module being renamed (in this case, just `x`) *must* be renamed to new, unused names. Optionally, it is also possible to rename other aspects of the module definition. In fact, the renaming is done at a textual level, so any identifiers (including [action labels](Synchronisation.md), [constants](Constants.md) and [functions](Expressions.md)) used in the module definition can be changed in this way.

**Note:** Care should be taken when renaming modules that make use of [formulas](FormulasAndLabels.md).
