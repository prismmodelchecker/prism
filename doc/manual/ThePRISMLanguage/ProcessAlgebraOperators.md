# Process algebra operators

To make the concept of synchronisation described above more powerful,
PRISM allows you to define precisely the way in which the set of modules are composed in parallel.
This is specified using the `system ... endsystem` construct,
placed at the end of the model description, which should contain a process-algebraic expression.
This expression should feature each module exactly once, and can use the following (CSP-based) operators:

- `M1 || M2` : alphabetised parallel composition of modules `M1` and `M2` (synchronising on only actions appearing in both `M1` and `M2`)

- `M1 ||| M2` : asynchronous parallel composition of `M1` and `M2` (fully interleaved, no synchronisation)

- `M1 |[a,b,...]| M2` : restricted parallel composition of modules `M1` and `M2` (synchronising only on actions from the set {`a`, `b`,...})

- `M / {a,b,...`} : hiding of actions {`a`, `b`, ...} in module `M`

- `M {a<-b,c<-d,...`} : renaming of actions `a` to `b`, `c` to `d`, etc. in module `M`.

The first two types of parallel composition (`||` and `|||`) are associative and can be applied to more than two modules at once.
When evaluating the expression, the hiding and renaming operators bind more tightly than the three parallel composition operators.
No other rules of precedence are defined and parentheses should be used to specify the order in which modules are composed.

Some examples of expressions which could be included in the `system ... endsystem` construct are as follows:

- `(station1 ||| station2 ||| station3) |[serve]| server`
- `((P1 |[a]| P2) / {a}) || Q`
- `((P1 |[a]| P2) {a<-b}) |[b]| Q`

When no parallel composition is specified by the user,
PRISM implicitly assumes an expression of the form `M1 || M2 || ...` containing all of the modules in the model.
For a more formal definition of the process algebra operators described above, check the semantics of the PRISM language, available from the "[Documentation](https://www.prismmodelchecker.org/doc/)" section of the PRISM web site.

PRISM is also able to [import](../RunningPRISM/SupportForPEPAModels.md) model descriptions written in (a subset of) the stochastic process algebra [PEPA](http://www.dcs.ed.ac.uk/pepa/) [[Hil96](../Main/References.md#Hil96)].
