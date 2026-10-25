# PRISM Regression Tests

This is the PRISM Regression Test Suite, a collection of probabilistic model checking
tasks that PRISM can perform, along with their expected result/output.

Most tests take the form of a PRISM model file (e.g. `test.prism`),
usually with an accompanying property file with a matching name (e.g. `test.prism.props`).
Command-line switches that need to be passed to PRISM to run the test
can be included in a separate `.args` file, again with matching name (e.g. `test.prism.props.args`).

The tests are executed by running PRISM in test mode: `prism -test`.
This looks for a comment of the form `// RESULT: xxx` preceding each property
and then checks agains the expected result (xxx) after model checking.
See, e.g., the files example.prism and example.prism.props in this directory.

Use the `prism-test` script (in `etc/scripts/`) to automate test execution.
(In fact, this works via the `prism-auto` script, also in `etc/scripts`,
which should be run with the `-t` and `-m` switches for testing).

You can give `prism-test` either a specific property file (`test.prism.props`)
or a model file (`test.prism`), which runs all tests for that model.
Alternatively, pass a directory, which is searched recursively for tests.

For anything beyond a handful of tests, pass the `--nailgun` option
(e.g. `prism-test <dir> --nailgun`) to run PRISM via a persistent JVM (`bin/ngprism`)
instead of starting a fresh one for every test - much faster for a
suite made up of many small invocations. This needs a Java version
below 25: the Nailgun compatibility workaround used for Java >=19 relies on
`-Djava.security.manager=allow`, and that property was removed in JDK 24,
so Nailgun mode cannot start under JDK 24+.

Current test sets are:

* functionality/ : currently partial coverage of PRISM's functionality
* bugfixes/ : examples based on previously fixed bugs
* papers/ : tutorial/toy examples from papers
* pmc/ : examples from Probabilistic Model Checking lecture course
* contrib/ : tests contributed by others

See here for more details:

* https://github.com/prismmodelchecker/prism/wiki/Regression-Testing
