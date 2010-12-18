Brief notes on the structure of this source directory.
Dave Parker.

-----------

* prism/ - main Prism API, the command-line tool, and Java classes for symbolic data structures and algorithms
* parser/ - JavaCC parser files and accompanying abstract syntax tree data structures and tools
* settings/ - generic 'settings' libraries
* simulator/ - the discrete event simulation engine and approximate model checking code
* dd/ - a C/C++ library of BDD/MTBDD functions, mostly just wrappers for the CUDD library
* jdd/ - a Java library providing a wrapper around the dd librar and hence Java access to CUDD via JNI
* odd/ - "offset-labelled BDDs", used to facilitate indexing of states between BDDs and explicit-state data structures
* dv/ - "double vector" - utility functions for operations on vectors of doubles
* mtbdd/ - the "MTBDD" engine: fully symbolic implementations of model checking algorithms
* sparse/ - the "Sparse" engine: sparse matrix data structures and corresponding implementations of model checking algorithms
* hybrid/ - the "Hybrid" engine: data structures and model checking algorithms that mix MTBDDs and explicit-state techniques
* jltl2ba/ and jltl2dstar/ - Java ports of the LTL-to-automata libraries
* explicit/ - explicit-state probabilistic model checking, implemented in Java
* pta/ - probabilistic timed automata (PTA) model checking, including DBM library
* userinterface/ - the GUI
* pepa/ - PEPA-to-PRISM model translation
* bin/ - OS-specific build scripts, customised and installed by make
* scripts/ - schell script(s) used from within make
* nsis_script.nsi - Script to build Windows binaries with NSIS
