# Global variables

In addition to the local variables belonging to each module, a PRISM model can also include *global variables*,
which can be written to, as well as read, by all modules.
Like local variables, these can be integers or Booleans.
Global variables are declared in identical fashion to a module's local variables,
except that the declaration must not be inside the definition of any module.
Some example declarations are as follows:

```prism
global g : [1..10];
global b : bool init true;
```

A global variable can be modified by any module and provides another way for modules to interact.
An important restriction on the use of global variables is the fact that commands which synchronise with other modules
(i.e. those with an action label attached; see the section "[Synchronisation](Synchronisation.md)") cannot modify global variables.
PRISM will detect this and report an error.
