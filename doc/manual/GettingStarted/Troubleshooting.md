# Troubleshooting

This page describes some of the most common problems and questions related to the installation and running of PRISM. These are grouped into the following categories:

- [Running PRISM on Windows](#windows)
- [Running PRISM on non-Windows platforms](#running)
- [Compiling PRISM](#compiling)
- [Other issues](#other)

<a id="windows"></a>
## Running PRISM on Windows

<a id="windows-nothing"></a>
### I double-click the PRISM shortcut but nothing happens

The most common cause of this is that you either do not have Java installed or the `java` executable is not in your path. In any case, to determine the exact problem, launch a command shell and navigate to the `bin` directory inside the directory where you installed PRISM (you can use the "PRISM (console)" shortcut installed in the start menu to do this). Then, type `prism.bat` and see what error message is displayed.

<a id="running"></a>
## Running PRISM on non-Windows platforms

<a id="link-errors"></a>
### PRISM won't start: `NoClassDefFoundError` or `UnsatisfiedLinkError`

When I try to run PRISM, I get one of these errors:

```shell
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

<a id="macos-blocked"></a>
### macOS won't open `libprism.dylib`

When I try to run PRISM on macOS, I get an error of the form:

```shell
"libprism.dylib" can't be opened because Apple cannot check it for malicious software.
```

or:

```shell
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

<a id="java-version"></a>
### `UnsupportedClassVersionError`

When I try to run PRISM, I get an error of the form:

```shell
Exception in thread "main" java.lang.UnsupportedClassVersionError: ... has been compiled by a more recent version of the Java Runtime ...
```

Your version of Java is too old (PRISM needs Java 11 or above). Update or install a newer version of Java, and then try again.

<a id="compiling"></a>
## Compiling PRISM

<a id="make-loop"></a>
### `make` seems to get stuck in an infinite loop

This is probably due to the detection of Java failing. Specify the location of your Java directory by hand, e.g. `make JAVA_DIR=/usr/lib/jvm/java-21-openjdk`. See "[Building PRISM from source](Installing.md#source)" for more on this.

<a id="libtool"></a>
### `libtool` errors on macOS

When I try to compile PRISM, I get errors of the form:

```shell
/usr/bin/libtool: for architecture: cputype (16777234) cpusubtype (0) file: -lSystem is not an object file (not allowed in a library)
```

The likely explanation is that you have upgraded to a new version of macOS but have not upgraded the developer tools (Xcode or its command line tools). Upgrade (e.g. with `xcode-select --install`) and try again.

<a id="gnu-make"></a>
### Nothing happens, or `Unexpected end of line seen`

When I try to compile PRISM, nothing seems to happen, or I get errors of the form:

```shell
make: Fatal error in reader: Makefile, line 58: Unexpected end of line seen...
```

Perhaps you are not using the GNU version of `make`. Try typing `make -v` to find out. On some systems, GNU make is called `gmake`.

<a id="line-endings"></a>
### `$'\r': command not found` on Windows

When I try to compile PRISM on Windows, I get errors of the form:

```shell
./install.sh: line 2: $'\r': command not found
```

The files have Windows line endings. Did you unpack PRISM using WinZip or another Windows program, or check it out with a Windows version of git? If so, unpack from Cygwin, using `tar xfz` (or similar), or use Cygwin's git (see "[Building PRISM from source on Windows using Cygwin](Installing.md#cygwin)").

<a id="unpack"></a>
### `can't create ... No such file or directory`

When I try to compile PRISM, I get an error of the form:

```shell
Assembler messages:  Fatal error: can't create ../../obj/dd/dd_abstr.o: No such file or directory
```

Did you unpack PRISM using a graphical tool or file manager? If so, unpack using `tar xfz` (or similar) instead.

<a id="other-make"></a>
### Do I have to use GNU `make` to build PRISM?

Strictly speaking, no, but you will have to modify the various PRISM Makefiles manually to overcome this.

<a id="other-os"></a>
### Can I build PRISM on other operating systems?

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

<a id="other"></a>
## Other issues

<a id="uninstall"></a>
### How do I uninstall PRISM?

If you installed PRISM on Windows using the self-extracting installer, you can uninstall it using the option on the start menu. If you didn't add these shortcuts, just run `uninstall.exe` from the directory where you installed PRISM.

On other platforms, simply delete the directory containing it.

The only thing that is not removed via either of these methods is the file containing your PRISM settings (see "[Settings](../ConfiguringPRISM/SettingsFile.md)" for where it is). You may wish to retain this when upgrading.

<a id="help"></a>
### I still have a problem installing/running PRISM. What can I do?

Please post a message in the discussion forum (see "[Getting help](https://www.prismmodelchecker.org/support/)" on the PRISM website).
