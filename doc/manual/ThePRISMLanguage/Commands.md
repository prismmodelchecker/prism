# Commands

The behaviour of each module is described by *commands*,
comprising a *guard* and one or more *updates*.
The first command of module `M1` in our [example](Example1.md) is:

```prism
[] x=0 -> 0.8:(x'=0) + 0.2:(x'=1);
```

The guard `x=0` indicates that this describes the behaviour of the module when the variable `x` has value 0.
The updates `(x'=0)` and `(x'=1)` and their associated probabilities state that the value of `x` will
remain at 0 with probability 0.8 and change to 1 with probability 0.2.
Note that the inclusion of updates in parentheses, e.g. `(x'=1)`, is essential.
While older versions of PRISM did not report the absence of parentheses as an error, newer versions do.
Note also that PRISM will complain if the probabilities on the right hand side of a command do not sum to one.

The second command:

```prism
[] x=1 & y!=2 -> (x'=2);
```

illustrates that guards can contain constraints on any variable, not just the ones in that module,
i.e. the behaviour of one module can depend on the state of another.
Updates, however, can only specify values for variables belonging to the module.
In general a module can *read* the variables of any other module, but only *write* to its own.
When a command comprises a single update with probability 1, the `1.0:` can be omitted,
as is done in the example above.

If a module has more than one variable, updates describe the new value for each of them.
For example, if it had two variables `x1` and `x2`, a possible command would be:

```prism
[] x1=0 & x2>0 & x2<10 -> 0.5:(x1'=1)&(x2'=x2+1) + 0.5:(x1'=2)&(x2'=x2-1);
```

Notice that elements of the updates are concatenated with `&` and that each element must be bracketed individually.
If an update does not give a new value for a local variable, it is assumed not to change.
As a special case, the keyword `true` can be used to denote an update where no variable's value changes, i.e. the following are all equivalent:

```prism
[] x1>10 | x2>10 -> (x1'=x1)&(x2'=x2);
[] x1>10 | x2>10 -> (x1'=x1);
[] x1>10 | x2>10 -> true;
```

Finally, it is important to remember that the expressions on the right hand side of each update refer to the state of the model *before* the update occurs. So, for example, this command:

```prism
[] x1=0 & x2=1 -> (x1'=2)&(x2'=x1)
```

updates variable `x2` to 0, not 2.
