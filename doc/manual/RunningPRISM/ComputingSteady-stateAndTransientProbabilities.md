# Computing steady-state and transient probabilities

If the model is a CTMC or DTMC, it is possible to compute corresponding vectors of
steady-state or transient probabilities directly
(rather than indirectly by analysing a property which requires their computation).
From the GUI, select an option from the "Model | Compute" menu.
For transient probabilities, you will be asked to supply the
time value for which you wish to compute probabilities.
From the command-line, add the `-steadystate` (or `-ss`) switch:

```shell
prism poll2.sm -ss
```

for steady-state probabilities or the `-transient` (or `-tr`) switch:

```shell
prism poll2.sm -tr 2.0
```

for transient probabilities, again specifying a time value in the latter case.
The probabilities are computed for all states of the model and displayed,
either on the screen (from the command-line) or in the log (from the GUI).

To instead export the vector of computed probabilities to a file, use the "Model | Compute/export" option from the GUI, or the `-exportsteadystate` (or `-exportss`) and `-exporttransient` (or `-exporttr`) switches from the command-line:

```shell
prism poll2.sm -ss -exportss poll2-ss.txt
prism poll2.sm -tr 2.0 -exporttr poll2-tr2.txt
```

From the command-line, you can request that the probability vectors exported are in Matlab format by adding the `-exportmatlab` switch.

## Initial probability distributions

By default, for both steady-state and transient probability computation,
PRISM assumes that the initial probability distribution of the model is
an equiprobable choice over the set of initial states.
You can override this and provide a specific initial distribution. This is done using the `-importinitdist` switch. The format for this imported distribution is identical to the ones exported by PRISM, i.e. simply a list of probabilities for all states separated by new lines. For example, this:

```shell
prism poll2.sm -tr 1.0 -exporttr poll2-tr1.txt
prism poll2.sm -tr 1.0 -importinitdist poll2-tr1.txt -exporttr poll2-tr2.txt
```

is (essentially) equivalent to this:

```shell
prism poll2.sm -tr 2.0 -exporttr poll2-tr2.txt
```

## Ranges of time values

Finally, you can compute transient probabilities for a range of time values, e.g.:

```shell
prism poll2.sm -tr 0.1:0.01:0.2
```

which computes transient probabilities for the time points 0.1, 0.11, 0.12, .., 0.2. In this case, the computation is done incrementally, with probabilities for each time point being computed from the previous point for efficiency.
