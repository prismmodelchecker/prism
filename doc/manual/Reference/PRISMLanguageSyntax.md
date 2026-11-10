# PRISM language syntax

This reference documents various aspects of the PRISM language syntax,
covering both model and property specification.

<a id="model-property-files"></a>
## Model and property files

Files containing models specified in the PRISM modelling language
and properties expressed in the PRISM property specification language
are stored as plain text files, with the **file extensions** `.prism` and `.props`, respectively.

Files can contain any amount of **white space** (spaces, tabs, new lines, etc.),
all of which is ignored when the file is parsed.
**Comments** are written using `//`, which starts a comment that runs to the end of the line.

<a id="model-types"></a>
## Model types

A **model type** keyword, usually at the very start of a model file, indicates the type of model being specified,
and can currently be any of the following:

* `dtmc` (DTMC: discrete-time Markov chain)
* `ctmc` (CTMC: continuous-time Markov chain)
* `mdp` (MDP: Markov decision process)
* `pta` (PTA: probabilistic timed automaton)
* `pomdp` (POMDP: partially observable MDP)
* `popta` (POPTA: partially observable PTA)

Based on features that are included in a model,
PRISM will, to some extent, auto-detect the model type if no keyword is given
and adapt the model type beyond the keyword if features are added that require a different type.
Indeed, some model types do not have explicit keywords (e.g., interval MDPs),
and are only identified by the features that they use.

* The **default** model type is MDP, unless no probabilities are used,
in which case it is LTS (labelled transition system).
* An MDP model with [clock](../ModelTypes/Real-timeModels.md#modelling) variables
is treated as a PTA, even if the `pta` keyword is not used.
* An MDP or PTA with [observables](../ModelTypes/PartiallyObservableModels.md#modelling)
is treated as a POMDP or POPTA (partially observable PTA), respectively.
* If a model uses [intervals](../ModelTypes/UncertainModels.md#modelling) for probabilities,
it is treated as an interval variant: an IDTMC (interval DTMC) or IMDP (interval MDP).

<a id="identifiers"></a>
## Identifiers and keywords

**Identifiers** (names of modules, variables, constants, formulas and so on)
are made up of letters, digits and underscores, but cannot start with a digit:
they match `[A-Za-z_][A-Za-z0-9_]*`. They are case-sensitive.
They cannot be any of PRISM's **keywords**:

<a id="keywords"></a>

<!-- generated: keywords (by doc/update-generated.py; do not edit) -->
`A`, `bool`, `C`, `clock`, `const`, `ctmc`, `double`, `dtmc`, `E`, `endinit`, `endinvariant`, `endmodule`, `endobservables`, `endrewards`, `endsystem`, `F`, `false`, `filter`, `formula`, `func`, `G`, `global`, `I`, `init`, `int`, `invariant`, `label`, `max`, `maxmax`, `maxmin`, `mdp`, `min`, `minmax`, `minmin`, `module`, `nondeterministic`, `observable`, `observables`, `P`, `Pmax`, `Pmaxmax`, `Pmaxmin`, `Pmin`, `Pminmax`, `Pminmin`, `pomdp`, `popta`, `prob`, `probabilistic`, `pta`, `R`, `rate`, `rewards`, `Rmax`, `Rmaxmax`, `Rmaxmin`, `Rmin`, `Rminmax`, `Rminmin`, `S`, `stochastic`, `system`, `true`, `U`, `W`, `X`.
<!-- end generated -->

**Uniqueness**: The names of variables (global, or local to any module), constants and formulas share a single namespace,
so they must all be distinct.
Constants defined in a properties file must also be distinct from all of these names in the model.
Modules, actions and reward structures each have their own, separate, namespace,
so their names only need to be unique within their own category, not across all names in the model.
Names written in quotes, i.e., those of labels, observables and named properties, form another namespace:
these must be distinct from each other, across both the model and the properties file.
The labels `"init"` and `"deadlock"` are built in and cannot be redefined.

<a id="expressions"></a>
## Expressions

Expressions are used in both models and properties.

<a id="operators"></a>
### Operators

The table below lists operators that can be used in expressions,
from the most strongly binding (1) to least.

| Precedence | Operator | Meaning | Associativity |
| --- | --- | --- | --- |
| 1 | `-` | unary minus | |
| 2 | `^` | power | right |
| 3 | `*`, `/` | multiplication, division | left |
| 4 | `+`, `-` | addition, subtraction | left |
| 5 | `<`, `<=`, `>=`, `>` | relational operators | left |
| 6 | `=`, `!=` | equality operators | left |
| 7 | `!` | negation | |
| 8 | `&` | conjunction | left |
| 9 | `\|` | disjunction | left |
| 10 | `<=>` | if-and-only-if | left |
| 11 | `=>` | implication | right |
| 12 | `? :` | condition: `c ? a : b` is `a` if `c` is true and `b` otherwise | right |

So, for example, `-x^2` means `(-x)^2`, and `2^3^2` means `2^(3^2)`.
Division `/` always gives a double, not integer division: `22/7` is 3.142857..., not 3.

In properties, the [temporal operators](../PropertySpecification/ThePOperator.md#path) bind less tightly than all of these.

<a id="literals"></a>
### Literals

- Integers: `0`, `42`
- Doubles: `0.5`, `.5`, `1e-3`, `2.5E+6`
- Booleans: `true`, `false`

<a id="functions"></a>
### Built-in functions

| Function | Result |
| --- | --- |
| `min(a,b,...)`, `max(a,b,...)` | minimum or maximum of two or more numbers |
| `floor(x)`, `ceil(x)` | `x` rounded down or up to an integer |
| `round(x)` | `x` rounded to the nearest integer (ties round up: `round(-1.5)` is `-1`) |
| `pow(x,y)` | `x` to the power `y` (as `x^y`) |
| `mod(i,n)` | `i` modulo `n` (`mod(-7,3)` is 2) |
| `log(x,b)` | logarithm of `x` to base `b` |

<a id="legacy"></a>
## Legacy syntax

Older versions of PRISM used some different conventions and syntax, which are still accepted for compatibility.
New models and properties should use the current forms.

* File extensions: model files used to have extensions `.pm` (DTMCs), `.nm` (MDPs, PTAs) or `.sm` (CTMCs), not `.prism`.
* File extensions: property files used to have extensions `.pctl` or `.csl`, not `.props`.
* Model types: `probabilistic`, `nondeterministic` and `stochastic` were formerly synonyms for `dtmc`, `mdp`, `ctmc`.
* Integer-valued constants: were previously declared as `const N = 5;`, meaning `const int N = 5;`.
* Double-valued constants: could previous also be written as `rate r = 0.5;` or `prob p = 0.1;`, meaning `const double r = 0.5;`.
* Functions: could be written with a special `func` keyword, e.g., `func(floor, x)` or `func(min, a, b)`, instead of the current forms `floor(x)` and `min(a, b)`.
* Filters: the old-style syntax for [filters](../PropertySpecification/Filters.md) in properties was, e.g., `P=? [ path {states} ]` for `filter(state, P=? [ path ], states)`,
  with `{states}{min}`, `{states}{max}` and `{states}{min}{max}` for the `min`, `max` and `range` filters.
