# Expressions

The definition of the `area` constant, in the example above, uses an *expression*.
We now define more precisely what types of expression are supported by PRISM.
Expressions can contain literal values (12, 3.141592, `true`, `false`, etc.),
identifiers (corresponding to variables, constants, etc.) and operators from the following list:

- `-` (unary minus)
- `^` (power)
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

All of these operators except `?` and `=>` are left associative
(i.e. they are evaluated from left to right).
The precedence of the operators is as found in the list above,
most strongly binding operators first.
Operators on the same line (e.g. `+` and `-`) are of equal precedence.

Much of the notation for expressions is hence essentially equivalent to that of C/C++ or Java.
One notable exception to this is that  the division operator `/` always performs floating point, not integer, division,
i.e. the result of `22/7` is 3.142857... not 3.
All expressions must evaluate correctly in terms of type (integer, double or Boolean).

**Built-in Functions**

Expressions can make use of several built-in functions:

- `min(...)` and `max(...)`, which select the minimum and maximum value, respectively, of two or more numbers
- `floor(x)` and `ceil(x)`, which round `x` down and up, respectively, to the nearest integer
- `round(x)`, which rounds `x` to the nearest integer (note, in a tie-break, we always round *up*, e.g. `round(-1.5)` gives `-1` not `-2`)
- `pow(x,y)` which computes `x` to the power of `y` (same as `x^y`)
- `mod(i,n)` for integer modulo operations
- `log(x,b)`, which computes the logarithm of `x` to base `b`

Examples of their usage are:

```prism
min(x+1, x_max)
max(a,b,c)
floor(13.5)
ceil(13.5)
round(13.5)
pow(2, 8)
pow(9.0, 0.5)
mod(1977, 100)
log(123, 2.71828183)
```

For compatibility with older versions of PRISM, all functions can also be expressed via the `func` keyword, e.g. `func(floor, 13.5)`.

**Use of Expressions**

Expressions can be used in a wide range of places in a PRISM language description, e.g.:

- constant definitions
- lower/upper bounds and initial values for variables
- guards
- probabilities/rates
- updates

This allows, for example, the probability in a command to be dependent on the current state:

```prism
[] (x>=1 & x<=10) -> x/10 : (x'=max(1,x-1)) + 1-x/10 : (x'=min(10,x+1))
```
