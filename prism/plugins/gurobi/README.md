# Gurobi LP Solver Plugin

This plugin provides a Gurobi backend for PRISM's LP-based solutions.
It is compiled conditionally — only when `lib/gurobi.jar` is present.

## Installation

1. Obtain a Gurobi licence and download the Gurobi software package.
2. Copy `gurobi.jar` and the native libraries (e.g., `libgurobi130.dylib`
   and `libGurobiJni130.dylib` (macOS) or `libgurobi130.so` and
  `libGurobiJni130.so` (Linux) into main PRISM the `lib/` directory.
3. Run `make` from the `prism/` directory.
   This produces `lib/prism-gurobi.jar` automatically.

The plugin is then active at runtime. To select it, use e.g.:

```
prism model.prism reach.probs -ex -lp -lpsolver gurobi
```

## Pre-built artifact

If you have a pre-built `prism-gurobi.jar`, drop it into `lib/`.
No `make` invocation needed — `lib/*` is on the runtime classpath.

## Removing the plugin

Delete `lib/prism-gurobi.jar`.
