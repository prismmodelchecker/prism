# Troubleshooting

This page describes some of the most common problems and questions related to the installation and running of PRISM.
Click on a question to see its answer.

## Running PRISM on Windows

<details id="windows-nothing">
<summary>I double-click the PRISM shortcut but nothing happens</summary>

The most common cause of this is that you either do not have Java installed or the `java` executable is not in your path. In any case, to determine the exact problem, launch a command shell and navigate to the `bin` directory inside the directory where you installed PRISM (you can use the "PRISM (console)" shortcut installed in the start menu to do this). Then, type `prism.bat` and see what error message is displayed.

</details>

## Running PRISM on other platforms

<details id="link-errors">
<summary>PRISM won't start: <code>NoClassDefFoundError</code> or <code>UnsatisfiedLinkError</code></summary>

When I try to run PRISM, I get one of these errors:

```text
Exception in thread "main" java.lang.NoClassDefFoundError: ...
java.lang.UnsatisfiedLinkError: no prism in java.library.path
java.lang.UnsatisfiedLinkError: ... Library not loaded: ../../lib/libprism.dylib
```

Things to check:

1. Did you run `install.sh` from the PRISM directory?
2. If you compiled PRISM from source code, are you sure no errors occurred during the process? To check, go into the PRISM directory, type `make clean_all` and then `make`, checking the output (especially at the end) carefully for any error messages.
3. Are you running on macOS? See the [next question](#macos-blocked).
4. Is PRISM picking up the wrong `java`? You can give the exact path to the `java` to use when running PRISM, e.g.:

   ```shell
   PRISM_JAVA=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java prism
   ```

   or set the value of `PRISM_JAVA` in the `prism` script directly.

</details>

<details id="macos-blocked">
<summary>macOS won't open <code>libprism.dylib</code></summary>

When I try to run PRISM on macOS, I get an error of the form:

```text
"libprism.dylib" can't be opened because Apple cannot check it for malicious software.
```

or:

```text
libprism.dylib not valid for use in process: library load disallowed by system policy
```

PRISM's macOS binaries are currently not signed/notarised,
so you need to approve the binary files manually to run them.
Run this command from the same place you ran `./install.sh`:

```shell
xattr -d com.apple.quarantine lib/*lib
```

or approve the libraries one by one through the
[security settings](https://support.apple.com/en-gb/102445).

You can also avoid this issue by compiling from source code.

</details>

<details id="java-version">
<summary>PRISM won't start: <code>UnsupportedClassVersionError</code></summary>

When I try to run PRISM, I get an error of the form:

```text
Exception in thread "main" java.lang.UnsupportedClassVersionError: ... has been compiled by a more recent version of the Java Runtime ...
```

Your version of Java is too old (PRISM needs Java 11 or above). Update or install a newer version of Java, and then try again.

</details>

## Compiling PRISM

<details id="make-loop">
<summary><code>make</code> seems to get stuck in an infinite loop</summary>

This is probably due to the detection of Java failing. Specify the location of your Java directory by hand, e.g. `make JAVA_DIR=/usr/lib/jvm/java-21-openjdk`. See "[Building PRISM from source](Installing.md#source)" for more on this.

</details>

<details id="libtool">
<summary><code>libtool</code> errors on macOS</summary>

When I try to compile PRISM, I get errors of the form:

```text
/usr/bin/libtool: for architecture: cputype (16777234) cpusubtype (0) file: -lSystem is not an object file (not allowed in a library)
```

The likely explanation is that you have upgraded to a new version of macOS but have not upgraded the developer tools (Xcode or its command line tools). Upgrade (e.g. with `xcode-select --install`) and try again.

</details>

<details id="gnu-make">
<summary>Nothing happens, or <code>Unexpected end of line seen</code></summary>

When I try to compile PRISM, nothing seems to happen, or I get errors of the form:

```text
make: Fatal error in reader: Makefile, line 58: Unexpected end of line seen...
```

Perhaps you are not using the GNU version of `make`. Try typing `make -v` to find out. On some systems, GNU make is called `gmake`.

</details>

<details id="line-endings">
<summary><code>$'\r': command not found</code> on Windows</summary>

When I try to compile PRISM on Windows, I get errors of the form:

```text
./install.sh: line 2: $'\r': command not found
```

The files have Windows line endings. Did you unpack PRISM using WinZip or another Windows program, or check it out with a Windows version of git? If so, unpack from Cygwin, using `tar xfz` (or similar), or use Cygwin's git (see "[Building PRISM from source on Windows using Cygwin](Installing.md#cygwin)").

</details>

<details id="unpack">
<summary><code>can't create ... No such file or directory</code></summary>

When I try to compile PRISM, I get an error of the form:

```text
Assembler messages:  Fatal error: can't create ../../obj/dd/dd_abstr.o: No such file or directory
```

Did you unpack PRISM using a graphical tool or file manager? If so, unpack using `tar xfz` (or similar) instead.

</details>

<details id="other-make">
<summary>Do I have to use GNU <code>make</code> to build PRISM?</summary>

Strictly speaking, no, but you will have to modify the various PRISM Makefiles manually to overcome this.

</details>

<details id="other-os">
<summary>Can I build PRISM on other operating systems?</summary>

PRISM should be suitable for any Unix/Linux variant.

The first thing you will need to do is compile CUDD (the BDD library used by and included in PRISM) on that platform.
CUDD is built with its own `configure` script (run by `cudd/install.sh`),
which works on most Unix-like systems.
To just build CUDD, not PRISM, type `make cuddpackage` instead of `make`.

Next, look at the main PRISM Makefile, in particular, each place where the
variable `$OSTYPE` is referred to. Most lines include comments and further
instructions. Once you have done this, proceed as usual.

If you do successfully build PRISM on other platforms, please let us know
so we can include this information in future releases. Thanks.

</details>

## Other issues

<details id="uninstall">
<summary>How do I uninstall PRISM?</summary>

If you installed PRISM on Windows using the self-extracting installer, you can uninstall it using the option on the start menu. If you didn't add these shortcuts, just run `uninstall.exe` from the directory where you installed PRISM.

On other platforms, simply delete the directory containing it.

The only thing that is not removed via either of these methods is the file containing your PRISM settings (see "[Settings](../ConfiguringPRISM/SettingsFile.md)" for where it is). You may wish to retain this when upgrading.

</details>

<details id="help">
<summary>I still have a problem installing/running PRISM. What can I do?</summary>

Please post a message in the discussion forum (see "[Getting help](https://www.prismmodelchecker.org/support/)" on the PRISM website).

</details>
