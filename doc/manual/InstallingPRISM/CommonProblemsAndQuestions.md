# Common problems and questions

This section describes some of the most common problems and questions related to the installation and running of PRISM. These are grouped into the following categories:

- Running PRISM on Windows
- Running PRISM on non-Windows platforms
- Compiling PRISM
- Other issues

---

## Running PRISM on Windows

**When I try to run PRISM on Windows, I double-click the PRISM shortcut but nothing happens.**

The most common cause of this is that you either do not have Java installed or the `java` executable is not in your path. In any case, to determine the exact problem, launch a command shell and navigate to the `bin` directory inside the directory where you installed PRISM (you can use the "PRISM (console)" shortcut installed in the start menu to do this). Then, type `prism.bat` and see what error message is displayed.

---

## Running PRISM on non-Windows platforms

**When I try to run PRISM, I get one of these errors:**<br>

- **`Exception in thread "main" java.lang.NoClassDefFoundError: ...`**
- **`java.lang.UnsatisfiedLinkError: no prism in java.library.path`**
- **`java.lang.UnsatisfiedLinkError: ... Library not loaded: ../../lib/libdd.dylib`**

Things to check:

(1) Did you run `install.sh` from the PRISM directory? (non-Windows platforms)

(2) If you compiled PRISM from source code, are you sure no errors occurred during the process? To check, go into the PRISM directory, type `make clean_all` and then `make`, checking the output (especially at the end) carefully for any error messages.

(3) Are you running on Mac OS? See the next question.

(4) Are you running on an old version of Mac OS (notably El Capitan)? This had some issues with PRISM calling `java`.
A workaround is to specify the exact path to the `java` when running PRISM, e.g.:

`PRISM_JAVA=/Library/Java/JavaVirtualMachines/jdk1.9.jdk/Contents/Home/bin/java prism`

or by replacing the value of PRISM_JAVA directly in the prism script
directly.

**When I try to run PRISM on macOS, I get an error of the form: "`libprism.dylib` can’t be opened because Apple cannot check it for malicious software." or "`libprism.dylib not valid for use in process: library load disallowed by system policy`".**

PRISM's macOS binaries are currently not signed/notarised,
so you need to approve the binary files manually to run them.
Run this command from the same place you ran `./install.sh`:

```shell
xattr -d com.apple.quarantine lib/*lib
```

or approve the libraries one by one through the
[security settings](https://support.apple.com/en-gb/102445).

You can also avoid this issue by compiling from source code.

**When I try to run PRISM, I get an error of the form:<br>
`Exception in thread "main" java.lang.UnsupportedClassVersionError: Bad version number in .class file`**

Your version of Java is too old. Update or install a newer version of Java, and then try again.

---

## Compiling PRISM

**When I try to compile PRISM, `make` seems to get stuck in an infinite loop**

This is probably due to the detection of Java failing. Specify the location of your Java directory by hand, e.g. `make JAVA_DIR=/usr/java/jdk1.9.0`. See the [Instructions](Instructions.md) page for more on this.

**When I try to compile PRISM, I get errors of the form:<br>
`/usr/bin/libtool: for architecture: cputype (16777234) cpusubtype (0) file: -lSystem is not an object file (not allowed in a library)`**

Are you compiling PRISM on Mac OS X? If so, the likely explanation is that you have upgraded to a new version of Mac OS X but have not upgraded the developer tools (eg. XCode). Upgrade and try again.

**When I try to compile PRISM, nothing seems to happen**

Perhaps you are not using the GNU version of `make`. Try typing `make -v` to find out. On some systems, GNU make is called `gmake`.

**When I try to compile PRISM, I get errors of the form:<br>
`Unexpected end of line seen...`<br>
or:<br>
`make: Fatal error in reader: Makefile, line 58: Unexpected end of line seen...`**

Perhaps you are not using the GNU version of `make`. Try typing `make -v` to find out. On some systems, GNU make is called `gmake`.

**When I try to compile PRISM, I get an error of the form:<br>
`./setup.sh: line 33: syntax error: unexpected end of file`**

Are you building on Cygwin? And did you unpack PRISM using WinZip? If so, unpack from Cygwin, using `tar xfz` (or similar) instead.

**When I try to compile PRISM, I get an error of the form:<br>
`Assembler messages:  Fatal error: can't create ../../obj/dd/dd_abstr.o: No such file or directory`**

Did you unpack PRISM using a graphical tool or file manager? If so, unpack using `tar xfz` (or similar) instead.

**Do I have to use GNU `make` to build PRISM?**

Strictly speaking, no, but you will have to modify the various PRISM Makefiles manually to overcome this.

**Can I build PRISM on operating systems other than those currently supported?**

PRISM should be suitable for any Unix/Linux variant.

The first thing you will need to do is compile CUDD (the BDD library used by and included in PRISM) on that platform.
Fortunately, CUDD has already been successfully built on a large number of
operating systems. Have a look at the sample Makefiles we provide (i.e. the
files `cudd/Makefile.*`) which are slight variants of the original Makefile
provided with CUDD (found here: `cudd/modified/orig/Makefile`). They contain
instructions on how to modify it for various platforms. You can then call
your new modified makefile something appropriate (`cudd/Makefile.$OSTYPE`) and
proceed to build PRISM as usual. To just build CUDD, not PRISM, type
`make cuddpackage` instead of `make`.

Next, look at the main PRISM Makefile, in particular, each place where the
variable `$OSTYPE` is referred to. Most lines include comments and further
instructions. Once you have done this, proceed as usual.

If you do successfully build PRISM on other platforms, please let us know
so we can include this information in future releases. Thanks.

---

## Other issues

**How do I uninstall PRISM?**

If you installed PRISM on Windows using the self-extracting installer, you can uninstall it using the option on the start menu. If you didn't add these shortcuts, just run `uninstall.exe` from the directory where you installed PRISM.

For older versions of PRISM on Windows or on any other platform, simply delete the directory containing it.

The only thing that is not removed via either of these methods is the `.prism` file containing your PRISM settings which is in your home directory (see the section "[Configuring PRISM](../ConfiguringPRISM/Introduction.md)"). You may wish to retain this when upgrading.

**I still have a problem installing/running PRISM. What can I do?**

Please post a message in the discussion group (see the [support](https://www.prismmodelchecker.org/support.php) section of the PRISM website).
