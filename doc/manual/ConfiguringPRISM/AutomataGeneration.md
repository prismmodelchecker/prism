# Automata Generation

When PRISM performs verification of [LTL formulas](../PropertySpecification/ThePOperator.md#ltl), it does so by converting the formula into an automaton (such as a deterministic Rabin automaton) and then analysing a larger product model, constructed from the model being verified and the omega automaton. For this reason, the size of the automaton has an important effect on the efficiency of verification.

By default PRISM uses a port of the [ltl2dstar](http://www.ltl2dstar.de/) library to construct these automata. But it also allows the use of external LTL-to-automata converters producing deterministic automata through support for the [Hanoi Omega Automaton](http://adl.github.io/hoaf/) (HOA) format. From the command line, an example of this is:

```shell
prism model.pm -pf "P=? [ G F x=1 ]" -ltl2datool hoa-ltl2dstar-for-prism -ltl2dasyntax lbt
```

The `-ltl2datool` switch specifies the location of the program to be executed to perform the LTL-to-automaton conversion. This will be called by PRISM as "`exec` `in-file` `out-file`", where `exec` is the executable, `in-file` is the name of a file containing the LTL formula to be converted and `out-file` is the name of a file where the resulting automaton should be written, in HOA format. Typically, the executable will be a script. Here is a simple example (called as `hoa-ltl2dstar-for-prism` in the above example), which calls an external copy of `ltl2dstar` in the required fashion (assuming that the `ltl2dstar` and [`ltl2ba`](http://www.lsv.ens-cachan.fr/~gastin/ltl2ba/)  executables are located in the current directory or on the PATH).

```shell
#! /bin/bash
ltl2dstar --output=automaton --output-format=hoa "$1" "$2"
```

PRISM is known to work with these HOA-enabled tools:

- [ltl2dstar](http://ltl2dstar.de/)
- [Spot](https://spot.lre.epita.fr/)'s [`ltl2tgba`](https://spot.lre.epita.fr/ltl2tgba.html)
- [ltl3dra](https://github.com/xblahoud/ltl3dra)
- [Rabinizer](https://www7.in.tum.de/~kretinsk/rabinizer4.html)

and contains ready-made scripts for calling them in the `etc/scripts/hoa` directory of the distribution:

- `hoa-ltl2dstar-for-prism` <br>
     ([ltl2dstar](http://ltl2dstar.de/), using [Spot](https://spot.lre.epita.fr/)'s [`ltl2tgba`](https://spot.lre.epita.fr/ltl2tgba.html), [`ltl2ba`](http://www.lsv.ens-cachan.fr/~gastin/ltl2ba/) or [LTL3BA](https://github.com/xblahoud/ltl3dra) as the LTL-to-NBA tool as the LTL-to-NBA tool)
- `hoa-ltl2tgba-for-prism` <br>
     ([Spot](https://spot.lre.epita.fr/)'s [`ltl2tgba`](https://spot.lre.epita.fr/ltl2tgba.html))
- `hoa-ltl3dra-for-prism` <br>
     ([ltl3dra](https://github.com/xblahoud/ltl3dra))
- `hoa-rabinizer-for-prism` <br>
     ([Rabinizer](https://www7.in.tum.de/~kretinsk/rabinizer4.html)'s `ltl2dra`)

See the files themselves for details of any configuration required, possible options and for a reminder of the PRISM command-line arguments required.

There is also a `hoa-rabinizer-for-prism.bat` script for connecting to Rabinizer on Windows.

The `-ltl2dasyntax` switch is used to specify the textual format for passing the LTL formula to the external converter (i.e., in the file `in-file`). The options are:

- `lbt` - LBT format
- `spin` - SPIN format
- `spot` - Spot format
- `rabinizer` - Rabinizer format

Typical usage is:

```shell
prism model.pm -pf "P=? [ G F x=1 ]" -ltl2datool hoa-ltl2dstar-for-prism -ltl2dasyntax lbt
prism model.pm -pf "P=? [ G F x=1 ]" -ltl2datool hoa-ltl2tgba-for-prism -ltl2dasyntax lbt
prism model.pm -pf "P=? [ G F x=1 ]" -ltl2datool hoa-ltl3dra-for-prism -ltl2dasyntax spin
prism model.pm -pf "P=? [ G F x=1 ]" -ltl2datool hoa-rabinizer-for-prism -ltl2dasyntax rabinizer
```

From the GUI, configuring the external LTL converter is done with the two options
"Use external LTL-&gt;DA tool" and "LTL syntax for external LTL-&gt;DA tool".

Another related option is "All path formulas via automata" (command-line switch `-pathviaautomata`), which forces construction of an automata
when computing the probability of a path formula, even if it is not needed. This is primarily intended for debugging/testing, not regular use.

As mentioned above, PRISM's external LTL-to-automaton interfacing works using the
[HOA](http://adl.github.io/hoaf/) format
(and, in particular,  using the [`jhoafparser`](http://automata.tools/hoa/jhoafparser/) HOA parser.
Currently, PRISM can handle automata in HOA format that are
deterministic and complete, with state-based acceptance.
Automata with transition-based acceptance are converted to state-based acceptance by PRISM.
For DTMC and  CTMC model checking, generic acceptance conditions are supported, i.e.,
anything that can be specified as an `Acceptance:` header in HOA format.
For MDP model checking, currently Rabin and generalized Rabin acceptance
specified via the `acc-name:` header are supported. See the [HOA format specification](http://adl.github.io/hoaf/) for details.
