# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Layout

The working directory is `prism/` (one level below the repo root). Most build/test/run commands are executed from here.

```
repo-root/
  prism/         ← primary working directory (this file lives here)
  prism-tests/   ← regression test suite
  prism-examples/ ← example models and properties
  cudd/          ← CUDD BDD library (C source)
  manual/        ← HTML user manual
```

Inside `prism/`:
- `src/` — all Java and C++ source
- `unit-tests/` — JUnit 5 unit tests
- `bin/` — launch scripts (`prism`, `xprism`, `ngprism`)
- `etc/scripts/` — utilities including `prism-auto` (regression driver)
- `etc/tests/` — local smoke-test models/properties
- `ext/` — external library source (lpsolve)
- `lib/` — JAR dependencies and compiled native shared libraries
- `classes/` — compiled `.class` files (build output)
- `obj/` — compiled `.o`/`.so` files (build output)

## Build Commands

Run all of these from `prism/`.

```bash
make                  # full build (CUDD + native C++ + Java)
make prism_java       # Java-only recompile (fast after Java-only changes)
make parser           # regenerate parser from src/parser/PrismParser.jj (JavaCC)
```

Java must target language level 11 (`javac --release 11`). Do not use Java 12+ language features.

**Never run `make binary` (or otherwise create/update `lib/prism.jar`) during normal development.** That target packages a `lib/prism.jar` for binary releases only, and it is not part of the default `make`/`make prism_java` build. `bin/prism` puts `lib/prism.jar` *first* on the classpath, ahead of `classes/`, so if a stale jar is lying around it silently shadows freshly compiled classes and `./bin/prism` will run old code even after a successful `make prism_java`. If `lib/prism.jar` exists and isn't needed, remove it with `make clean_binary`.

## Running PRISM

Always use the local binary, never rely on a `prism` from `PATH` (a global install can cause misleading failures):

```bash
# From prism/
./bin/prism <model-file> <props-file> [options]

# Example with explicit engine (-ex):
./bin/prism ../prism-tests/functionality/verify/dtmcs/dtmc_pctl.pm \
            ../prism-tests/functionality/verify/dtmcs/dtmc_pctl.pm.props -ex
```

Engine flags: `-mtbdd` (MTBDD/symbolic), `-sparse`, `-hybrid`, `-explicit` / `-ex` (most actively developed).

## Testing

From `prism/`:

```bash
make test             # smoke test (one DTMC PCTL check)
make unittests        # compile and run JUnit unit tests
make tests            # full regression suite (../prism-tests/), via Nailgun
make testsfull        # all engines, all variants, with timeout
etc/tests/run.sh      # alternative smoke-test runner
```

Extra `prism-auto` options can be passed to `make tests` via `TESTS_ARGS=...`.

To run a single JUnit test class (after `make unittests` has compiled):

```bash
java -cp "classes:lib/*" org.junit.platform.console.ConsoleLauncher \
  --select-class <fully.qualified.TestClassName> \
  --details=summary
```

For regression tests, targeted reproduction is preferred over broad test suites — reproduce the failing case with a specific `./bin/prism` invocation first.

For a scoped/custom regression run over part of `prism-tests/` (e.g. one subdirectory, or with extra flags like `--test-all`), run `etc/scripts/prism-auto` with the same core options the Makefile uses:

```bash
# From prism/
etc/scripts/prism-auto -t -m ../prism-tests/<subdir> -p ./bin/prism \
    --nailgun --ngprism ./bin/ngprism
```

- **`-m`/`--matching` is required alongside `-t`/`--test`.** Without it, `prism-auto` cross-tests every model against every properties file in a directory, producing spurious failures (e.g. "Undeclared label") from nonsensical file pairings that look like real regressions but aren't.
- **`-p ./bin/prism` is required.** `prism-auto` defaults to whatever `prism` is on `PATH`, so without it you may silently test a different install rather than your local build. (`etc/scripts/prism-test` is a shorthand for `prism-auto -m -t`, but it also relies on `PATH` for both `prism-auto` and `prism`, so still pass `-p ./bin/prism` to it.)
- **Use `--nailgun`** for any run of more than a handful of tests. It reuses one persistent JVM (`bin/ngprism`) across the whole run instead of starting a fresh one per invocation, which is far faster. It requires **JDK 23 or earlier**: for Java ≥ 19, `prism-auto` starts Nailgun with `-Djava.security.manager=allow`, which JDK 24 removed (JEP 486), so Nailgun mode cannot start under JDK 24+. Check with `java -version` and switch to an older JDK (e.g. 21) if needed.

## Architecture

### Computation Engines

PRISM has four selectable engines defined as constants in `src/prism/Prism.java`:

| Constant | Flag | Description |
|---|---|---|
| `MTBDD = 1` | `-mtbdd` | Symbolic, BDD-based (uses CUDD via JNI) |
| `SPARSE = 2` | `-sparse` | Explicit matrix via C++ sparse routines (JNI) |
| `HYBRID = 3` | `-hybrid` | Hybrid BDD + explicit (JNI) |
| `EXPLICIT = 4` | `-ex` | Pure Java explicit-state — most actively developed |

### Key Source Packages (`src/`)

| Package | Role |
|---|---|
| `prism/` | Core API: `Prism.java` (main API class), `PrismCL.java` (CLI entry point), model type enums, interfaces |
| `explicit/` | Explicit-state engine: model classes (`DTMCSimple`, `MDPSimple`, `CTMCSimple`, `POMDP`, `STPG`, …) and model checkers (`DTMCModelChecker`, `MDPModelChecker`, etc.) |
| `symbolic/` | Symbolic (MTBDD) engine: BDD-based model checkers |
| `hybrid/` | Hybrid engine: `PrismHybrid.java` + JNI C++ files |
| `sparse/` | Sparse engine: `PrismSparse.java` + JNI C++ files |
| `dd/`, `jdd/`, `odd/`, `dv/`, `mtbdd/` | BDD and decision diagram layers (C++ + Java JNI wrappers) |
| `parser/` | PRISM language parser (JavaCC grammar `PrismParser.jj`) and AST classes |
| `simulator/` | Statistical model checking / simulation engine |
| `param/` | Parametric model checking (rational functions) |
| `pta/` | Probabilistic timed automata (digital clocks translation) |
| `automata/` | LTL-to-automaton translation (Rabin, Büchi, WDBA, HOA) |
| `acceptance/` | Acceptance condition classes (Büchi, Rabin, Streett, etc.) |
| `strat/` | Strategy/policy computation and export |
| `io/` | Model import/export (DRN, DOT, UMB/PRISM explicit format, Matlab) |
| `settings/` | PRISM settings and preferences |
| `userinterface/` | Swing GUI — `GUIPrism` is the JAR manifest `Main-Class` |

### Entry Points

- **CLI:** `bin/prism` → `prism.PrismCL.main()`
- **GUI:** `bin/xprism` → `userinterface.GUIPrism`
- **Nailgun daemon:** `bin/ngprism` — persistent JVM for fast repeated CLI invocations

### Parser

The PRISM modelling language parser is generated by JavaCC from `src/parser/PrismParser.jj`. After editing the grammar, regenerate with `make parser`. The resulting AST classes live alongside the grammar in `src/parser/ast/`.

### The umbj Library

The general-purpose UMB import/export functionality forms a standalone library `umbj`.
But the code is also kept within PRISM, since most development happens there,
and is ported across. So code changes that involve UMB should if necessary be
split into commits that only touch `src/io/github/pmctools/umbj` (which should not use
any other classes from the PRISM codebase) and PRISM-specific code. The first line
of commit messages for the former type should be prefixed "umbj: ".

## Coding Guidelines

- Keep fixes minimal and localized; do not refactor unrelated code in the same change.
- Preserve existing style and naming conventions throughout.
- Add or adjust tests when behaviour changes or bugs are fixed (see the `prism-add-test` skill in `.claude/skills/`).
- Don't add tests whose expected output pins one particular strategy/policy when several are equally optimal: PRISM does not specify which one is chosen. Only test a strategy when it is the unique valid one.
- Avoid introducing new dependencies unless explicitly requested.
- Java language level 11 is required — do not use features from Java 12+.

## Bug Fix Workflow

1. Reproduce the issue with an exact `./bin/prism` command.
2. Implement the smallest defensible fix.
3. Rebuild (`make` or `make prism_java` for Java-only changes).
4. Re-run the reproduction command (multiple times if nondeterministic).
5. Run nearby regression tests where feasible.

When summarising a bug fix, include:

- the exact command(s) used,
- whether the issue reproduced before the fix,
- evidence after the fix (exit codes / key output),
- any residual risk.

## Git Hygiene

- Never discard or overwrite user changes you did not make.
- Keep edits scoped to the task.
- Avoid destructive commands (`reset --hard`, `checkout --`) unless explicitly asked.
- If the tree is dirty, work around unrelated files and do not clean them up automatically.

## Pull Requests

- Feature branches are usually pushed to the user's own fork of the repository (a separate git remote), not to `origin`.
- PRs should target `origin` (`prismmodelchecker/prism`), with `--head <user>:<branch>` pointing at the branch on the fork.
- Merge locally and push, rather than merging via the GitHub UI or `gh pr merge`. A local fast-forward preserves the commit hash; GitHub's squash/rebase merge creates a new hash, which forces rebasing of any dependent branches.

For example, for GitHub user `davexparker`, whose fork is set up as a remote also named `davexparker`:

```bash
git push davexparker <branch>

gh pr create \
  --base master \
  --head davexparker:<branch> \
  --repo prismmodelchecker/prism \
  --title "..." --body "..."
```

After the PR has passed review/CI, merge and clean up locally:

```bash
git checkout master
git merge --ff-only <branch>
git push origin master        # closes the PR automatically
git branch -d <branch>
```
