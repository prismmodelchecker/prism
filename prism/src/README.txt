Brief notes on the structure of this source directory.
Further details on packages are in the Javadoc.
Dave Parker.

-----------

* bin/ - OS-specific build scripts, customised and installed by make.
* dd/ - A C/C++ library of BDD/MTBDD functions, mostly just wrappers for the CUDD library.
* dv/ - Utility functions for operations on vectors of doubles (stored in C++ through JNI).
* explicit/ - Explicit-state probabilistic model checking engine, implemented in Java.
* hybrid/ - The "Hybrid" engine: data structures and model checking algorithms that mix MTBDDs and explicit-state techniques.
* jdd/ - A Java library providing a wrapper around the dd library and hence Java access to CUDD via JNI.
* jltl2ba/ - Java port of the LTL to Buchi automata conversion library.
* jltl2dstar/ - Java port of the LTL to deterministic Rabin automata conversion library.
* mtbdd/ - The "MTBDD" engine: fully symbolic implementations of model checking algorithms.
* odd/ - ODDs (offset-labelled BDDs), used to facilitate indexing of states between BDDs and explicit-state data structures.
* parser/ - The PRISM model/properties parser, accompanying abstract syntax tree data structures and tools (and JavaCC parser files).
* pepa/ - PEPA-to-PRISM model translation.
* prism/ - The main Prism API, the command-line tool, and Java classes for symbolic data structures and algorithms.
* pta/ - Probabilistic timed automata (PTA) model checking, including DBM library.
* scripts/ - Shell script(s) used from within make.
* settings/ - Generic 'settings' functionality.
* simulator/ - The discrete event simulation engine and approximate (statistical) model checking code.
* sparse/ - The "Sparse" engine: sparse matrix data structures and corresponding implementations of model checking algorithms.
* userinterface/ - PRISM's graphical user interface (GUI).

* nsis_script.nsi - Script to build Windows binaries with NSIS.
