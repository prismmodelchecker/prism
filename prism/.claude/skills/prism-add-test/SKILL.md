---
name: prism-add-test
description: >
  Add a PRISM regression test. Use this skill whenever the user wants to add,
  create, or write a regression test (or "test case") for the PRISM model
  checker — including after fixing a bug, verifying new behaviour, or exploring
  a boundary condition. Triggers on phrases like "add this test", "write a
  regression test", "add a test case", "capture this as a test", "put this in
  prism-tests", or any request to create test files for a PRISM model/property
  combination.
---

# Adding a PRISM Regression Test

A test is three files: a model `<stem>.prism` (or historic aliases `.nm`/`.pm`/`.sm`), a properties file
`<stem>.prism.props`, and an optional engine-arguments file `<stem>.prism.props.args`. The test runner
(`prism-auto`) pairs them by name and runs each line in the `.args` file as a
separate PRISM invocation, checking that every `// RESULT:` comment in the
`.props` file matches actual output.

---

## Step 1 — Identify what's being tested

Clarify (or infer from context):
- **Model type**: DTMC, CTMC, MDP, PTA, POMDP, …
- **Property type**: reachability probability, expected reward, steady-state, LTL, …
- **Purpose**: new feature, bug fix, boundary/edge case, LP engine, …
- **Engines to cover**: explicit (`-ex`), sparse (`-s`), hybrid (`-h`), MTBDD (`-m`)

If you already have a working `./bin/prism` invocation from the current session,
use that — you know the model and the expected result.

---

## Step 2 — Pick the right directory

All tests live under `../prism-tests/` (sibling of the `prism/` working
directory). Pick the most specific subdirectory that fits:

| Situation | Directory |
|---|---|
| Bug fix (any model type) | `bugfixes/` |
| DTMC verification | `functionality/verify/dtmcs/` |
| CTMC verification | `functionality/verify/ctmcs/` |
| MDP reachability probability | `functionality/verify/mdps/reach/` |
| MDP expected rewards | `functionality/verify/mdps/rewards/` |
| MDP PCTL / LTL | `functionality/verify/mdps/pctl/` |
| PTA | `functionality/verify/ptas/` |
| Exact arithmetic | `functionality/verify/exact/` |
| Parametric | `functionality/verify/param/` |
| From a course or paper | `pmc/` or `papers/` |

When in doubt, `bugfixes/` is always safe for anything fixing a known problem.

Choose a short, descriptive file stem — e.g. `mdp_rmin_lp_inf` rather than
`test1`. Avoid generic names like `test` or `model`.

---

## Step 3 — Create the model file

Write the PRISM model as `<stem>.prism`. The extensions `.nm`, `.pm`, and `.sm` are historic aliases that still work but `.prism` is the modern default.
Do not create new tests with extension `.nm`, `.pm` or `.sm`.
Include a brief comment at the top explaining what the model demonstrates and
why it is interesting as a test case (future readers need to understand the
intent without running it).

Working directory is `prism/`; the file goes in `../prism-tests/<subdir>/`.

---

## Step 4 — Create the properties file (`<stem>.prism.props`)

Every `// RESULT:` comment must appear on the line immediately before the
property it annotates. Property formulas must end with a semicolon (`;`).

```
// RESULT: 0.5
Pmax=? [ F "done" ];

// RESULT: 10
Rmin=? [ F "done" ];
```

**Common result formats:**

```
// RESULT: 0.5                          plain numeric
// RESULT: 1/3                          exact fraction (preferred for exact values)
// RESULT: true                         boolean
// RESULT: Infinity                     infinite reward
// RESULT: [0.432,0.435]               interval (floating-point bounds)
// RESULT: Error                        any error
// RESULT: Error:keyword                error whose message contains "keyword"
// RESULT (N=2): 0.75                   only applies when constant N=2 is set
// RESULT (exact=true): 1/3            conditional on a -const value from .args
```

**Key rules:**
- End every property formula with `;` — the test runner requires it.
- Use exact fractions (e.g. `5/3`) rather than decimals when the result is
  mathematically exact — the test runner accepts both, but fractions are
  preferred style in prism-tests.
- For iterative numerical methods, a decimal like `10.0` or an interval
  `[9.999, 10.001]` is fine.
- `// RESULT: Infinity` (capital I) matches PRISM's literal output string.
- Multiple `// RESULT (...)` lines for the same property are OR'd — each is
  checked against the engine configuration's constants/flags.

---

## Step 5 — Get the actual result values

Run the model with each intended engine to get the results you'll put in the
`.props` file. The `-test` flag makes PRISM parse and check the `// RESULT:`
comments, but first run **without** `-test` to capture the actual values:

```bash
# From prism/
./bin/prism ../prism-tests/<subdir>/<stem>.prism \
            ../prism-tests/<subdir>/<stem>.prism.props \
            <engine-flags>
```

Note the printed results. If you already know the correct values (from a prior
run in the session, or from mathematical reasoning), skip this step.

---

## Step 6 — Create the arguments file (`<stem>.prism.props.args`)

List one set of engine flags per line. Each line becomes a separate test run.
Leave a blank line between logical groups if it helps readability (blank lines
are ignored by the runner).

```
-ex -lp
-ex -gs
-ex -politer
```

**Use abbreviated engine flags** — these are the conventional forms in prism-tests:

| Abbreviation | Meaning |
|---|---|
| `-ex` | explicit engine (most actively developed) |
| `-s` | sparse engine |
| `-h` | hybrid engine |
| `-m` | MTBDD engine |
| `-lp` | LP solution method (explicit engine; selects it automatically) |
| `-gs` | Gauss-Seidel solver |
| `-politer` | policy iteration |
| `-valiter` | value iteration |
| `-ii` | interval iteration (combine with VI or GS: `-ex -ii`) |
| `-exact` | exact arithmetic |

Do not use the long forms (`-explicit`, `-sparse`, `-hybrid`, `-mtbdd`) —
prism-tests convention is the short forms above.

Decide how many engines the test is *meant* to cover. An LP-specific test only
needs `-ex -lp`. A general correctness test should cover several engines.
A bug-fix test should at minimum cover the engine where the bug appeared.

---

## Step 7 — Verify with `-test`

Run the test with PRISM's `-test` flag, which reads the `// RESULT:` comments
and reports PASS/FAIL:

```bash
./bin/prism ../prism-tests/<subdir>/<stem>.prism \
            ../prism-tests/<subdir>/<stem>.prism.props \
            <engine-flags> -test
```

Do this for each line of the `.args` file. Every result should print
`Testing result: PASS`. If any FAIL, adjust the `// RESULT:` annotations or
the model/property until all pass.

Alternatively, use `prism-auto` to run all lines at once (from `prism/`):

```bash
etc/scripts/prism-auto -t -m ../prism-tests/<subdir>/<stem>.prism -p ./bin/prism
```

---

## Step 8 — Confirm and report

Tell the user:
- The three file paths created
- What each `.args` line tests and whether it passed
- Any non-obvious modelling choices (e.g. why a particular state has infinite
  expected reward, why a certain result is exact)

If you're amending an existing commit, stage and amend. If creating a new
commit, stage only the three test files (plus any source changes) and commit
with a descriptive message.
