Local (Gradle-based) management of dependent jars, kept in `lib`

To find dependencies and copy into `lib`:

* `gradle -p etc/jars syncLibs`

To see what changed:

* `git diff lib`

To remove files in `lib` in order to re-run:

* `gradle -p etc/jars cleanLibs`

To print dependency tree:

* `gradle -p etc/jars dependencies --configuration runtimeClasspath`

