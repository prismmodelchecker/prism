# Syntax and semantics

## Syntax

The syntax of the PRISM property specification language subsumes various probabilistic temporal logics, including PCTL, CSL, (probabilistic) LTL, PCTL\* and CTL. Informally, the syntax can be summarised as follows: a property can be any valid, well-typed PRISM [expression](../ThePRISMLanguage/Expressions.md), which (optionally) also includes the probabilistic operators discussed previously (`P`, `S` and `R`) and the non-probabilistic (CTL) ones `A` and `E`). This mean that any of the following operators can be used:

- `-` (unary minus)
- `*`, `/` (multiplication, division)
- `+`, `-` (addition, subtraction)
- `<`, `<=`, `>=`, `>` (relational operators)
- `=`, `!=` (equality operators)
- `!` (negation)
- `&` (conjunction)
- `|` (disjunction)
- `<=>` (if-and-only-if)
- `=>` (implication)
- `?` (condition evaluation: `condition ? a : b` means "if `condition` is true then `a` else `b`")
- `P` (probabilistic operator)
- `S` (steady-state operator)
- `R` (reward operator)
- `A` (for-all operator)
- `E` (there-exists operator)

This allows you to write any property expressible in logics such as PCTL and CSL. For example, CSL allows you to nest `P` and `S` operators:

```prism
P=? [ F>2 S>0.9[ num_servers >= 5 ] ]
```

"the probability of it taking more than 2 hours to get to a state from which the long-run probability of at least 5 servers being operational is &gt;0.9"

You can also express various arithmetic expressions such as:

```prism
1 - P=? [ F[3600,7200] oper ]
```

"the probability that the system is **not** operational at any point during the second hour of operation"

```prism
R{"oper"}=? [ C<=t ] / t
```

"the expected fraction of time that the system is available (i.e. the expected interval availability) in the time interval [0, t]"

```prism
P=? [ F fail_A ] / P=? [ F any_fail ]
```

"the (conditional) probability that component A eventually fails, given
that at least one component fails"

## Semantics

We omit a formal presentation of the semantics of the PRISM property language. The semantics of the probabilistic temporal logics that the language incorporates can be found from a variety of sources. See for example the pointers given in the [About](https://www.prismmodelchecker.org/about.php) and [Documentation](https://www.prismmodelchecker.org/doc/) sections of the PRISM website.

It is worth, however, clarifying a few points specific to PRISM. A property is evaluated with respect to a particular state of a model. Depending on the type of the property, this value may either be a Boolean, an integer or a double. When performing model checking, PRISM usually has to actually compute the value for *all* states of the model but, for clarity, will by default report just a single value. Typically, this is the value for the (single) initial state of the model. For example, this:

```prism
P=? [ F "error" ]
```

will report the probability, from the initial state of the model, of reaching an "error" state.
This:

```prism
P>0.5 [ F "error" ]
```

will return `true` if and only if the probability, from the initial state, is greater than 0.5.

**Note:** This is contrast to older versions of PRISM, which treated numerical and Boolean-valued properties differently in this respect.

For models with [multiple initial states](../ThePRISMLanguage/MultipleInitialStates.md), we need to adapt these definitions slightly. In this case, the two properties above will yield, respectively:

- the range of values (over all initial states) of the probability of reaching "error"

- `true` if and only if the probability is greater than 0.5 from *all* initial states.

You can also ask PRISM to return different values using [filters](Filters.md),
which are described in the next section.
