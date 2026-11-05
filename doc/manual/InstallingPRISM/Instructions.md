# Instructions

## Prerequisites

PRISM is known to run on Linux, Windows and macOS, both 64-bit and 32-bit versions.

You will need **Java**, version 11 or above.
To run binary versions of PRISM, you only need the Java Runtime Environment (JRE), not the full Java Development Kit (JDK).

To compile PRISM from source, you need the Java Development Kit (JDK), GNU make and a C/C++ compiler (e.g. gcc/g++). For compilation under Windows, you will need Cygwin. See below for more information:

- [Installation on Windows](#windows)
- [Installation of Linux/Mac binary versions](#binaries)
- [Building PRISM from source (non-Windows)](#source)
- [Building PRISM from source on Windows using Cygwin](#cygwin)
- [Building PRISM from source on Windows using MSYS2](#msys)

If you are installing on a completely fresh operating system installation (e.g. in a virtual machine), you may find the following scripts useful,
which install the required dependencies and PRISM itself. They can be found in the `prism/etc/scripts` directory:

- Script to build on a clean install of Ubuntu: [prism-install-ubuntu](https://raw.githubusercontent.com/prismmodelchecker/prism/master/prism/etc/scripts/prism-install-ubuntu)
- Script to build on a clean install of Fedora-based Linux (e.g. Amazon Linux): [prism-install-fedora](https://raw.githubusercontent.com/prismmodelchecker/prism/master/prism/etc/scripts/prism-install-fedora)
- Scripts to build on a clean install of Windows: [prism-install-windows.bat](https://raw.githubusercontent.com/prismmodelchecker/prism/master/prism/etc/scripts/prism-install-windows.bat) and [prism-install-cygwin](https://raw.githubusercontent.com/prismmodelchecker/prism/master/prism/etc/scripts/prism-install-cygwin)

---

<a id="windows"></a>
<a id="windows64"></a>

## Installation on Windows

If you don't yet have **Java**, install it, for example with the [Temurin installer](https://adoptium.net/temurin/releases).

To install PRISM on Windows, just run the self-extracting installer which you [downloaded](https://www.prismmodelchecker.org/download.php). You do **not** need administrator privileges for this, just write-access to the directory chosen for installation.

If requested, the installer will place shortcuts to run PRISM on the desktop and/or start menu. If not, you can run by PRISM double-clicking the file `xprism.bat` (which may just appear as `xprism`) in the `bin` folder of your PRISM folder. If nothing happens, the most likely explanation is that Java is not installed or not in your path. To check, open a command prompt window, navigate to the PRISM directory, type `cd bin`, then `xprism.bat` and examine the resulting error. If you want to create shortcuts to `xprism.bat` manually, you will find some PRISM icons in the `etc` folder.

If you wish to use the command-line version of PRISM on Windows, open a command prompt window and type for example:

```shell
cd "c:\Program Files\prism-4.8-win\bin"
prism ..\prism-examples\simple\dice\dice.pm
```

You can also edit the file `bin\prism.bat` to allow it to be run from any location. See the instructions within the file for further details.

**Problems?** See the section "[Common Problems And Questions](CommonProblemsAndQuestions.md)".

---

<a id="binaries"></a>

## Installation of Linux/Mac binary versions

We provide pre-compiled [binary distributions](https://www.prismmodelchecker.org/download.php) for Linux and macOS
(if you have any problems running these, try compiling PRISM from source instead - see below for instructions).
To install a binary distribution, unpack the tarred/zipped PRISM distribution into a suitable location, enter the directory and run the `install.sh` script, e.g.:

```shell
tar xfz prism-4.8-linux64-x86.tar.gz
cd prism-4.8-linux64-x86
./install.sh
```

On macOS, you may want to run the following command,
which avoids [manual approving](https://support.apple.com/en-gb/102445) the integrity of the binary files:

```shell
xattr -d com.apple.quarantine lib/*lib
```

You do not need to be root to install PRISM. The install script simply makes some small customisations to the scripts used to launch PRISM. The PRISM distribution is self-contained and can be freely moved/renamed, however if you do so you will need to re-run `./install.sh` afterwards.

To run PRISM, execute either the `xprism` or `prism` script (for the graphical user interface or command-line version, respectively). These can be found in the `bin` directory. These scripts are designed to be run from anywhere and you can easily create symbolic links or aliases to them. If you want icons to create desktop shortcuts to PRISM, you can find some in the `etc` directory.

**Problems?** See the section "[Common Problems And Questions](CommonProblemsAndQuestions.md)".

---

<a id="source"></a>

## Building PRISM from source (non-Windows)

To compile PRISM form source code, you will need:

- GNU make (sometimes called gmake)
- a C/C++ compiler (e.g. gcc/g++)
- a Java Development Kit, version 11 or above

To check that you have the development kit, type `javac`. If you get an error message that `javac` cannot be found, you probably do not have the JDK installed (or your path is not set up correctly). To check what version you have, type `javac -version`.

Hopefully, you can build PRISM simply by entering the PRISM directory and running `make`, e.g.:

```shell
tar xfz prism-4.8-src.tar.gz
cd prism-4.8-src/prism
make
```

For this process to complete correctly, PRISM needs to be able to determine your operating system (`OSTYPE`), machine architecture (`ARCH`) and the location of your Java distribution (`JAVA_DIR`). If there is a problem with either of these, you will see an error message and will need to specify one or more of these manually, such as in these examples:

```shell
make OSTYPE=linux
make OSTYPE=linux ARCH=aarch64
make JAVA_DIR=/usr/java/jdk1.8.0
make OSTYPE=cygwin JAVA_DIR="/cygdrive/c/Program Files/Java/jdk1.8.0"
```

Note the use of double quotes for the case where the directory contains a space. If you don't know the location of your Java installation, try typing `which javac`. If the result is e.g. `/usr/java/jdk1.8.0/bin/javac` then your Java directory is `/usr/java/jdk1.8.0`. Sometimes `javac` will be a symbolic link, in which case use "`ls -l`" to determine the actual location.

It is also possible to to set the environment variables directly or edit their values in the Makefile directly. Note that even when you specify `JAVA_DIR` explicitly (in either way), PRISM still uses the version of `javac` that is in your path. To override this, you can also specify a specific version by setting `JAVAC`.

**Problems?** See the section "[Common Problems And Questions](CommonProblemsAndQuestions.md)".

---

<a id="cygwin"></a>

## Building PRISM from source on Windows using Cygwin

The compilation of PRISM currently relies on a Unix-like environment. On Windows, this can be achieved using the [Cygwin](http://www.cygwin.com/) development environment, with the packages below installed.
You can run the script [prism-install-windows.bat](https://raw.githubusercontent.com/prismmodelchecker/prism/master/prism/etc/scripts/prism-install-windows.bat) in a Command Prompt to automate this.
- `make`
- `mingw64-x86_64-gcc-g++` (for 64-bit Windows) or `mingw64-i686-gcc-g++` (for 32-bit Windows)
- `binutils`
- `dos2unix`
- `git` (if you will pull source code from GitHub)
- `wget` (if you will pull source code from the web)
Then proceed as described in the previous section. Note that the PRISM compilation process uses the [MinGW](http://www.mingw.org/) libraries so that the final result is independent of Cygwin at run-time.

One thing to note: make sure you unzip the PRISM distribution from within Cygwin (e.g. using `tar xfz prism-XXX-src.tar.gz`). Don't use a Windows program (Winzip, etc.) since this can cause problems.

If you use git to checkout the PRISM repository, we recommend that you use the version of git provided by Cygwin.
If you use a native Windows version of git, you may want to disable the Unix-to-Windows line-ending conversion, e.g., via

- `git config --global core.autocrlf false`

**Problems?** See the section "[Common Problems And Questions](CommonProblemsAndQuestions.md)".

---

<a id="msys"></a>

## Building PRISM from source on Windows using MSYS2

An alternative to Cygwin on Windows is [MSYS2](https://www.msys2.org/).
You can install the required MSYS2 packages like this:

```shell
pacman -S make mingw-w64-x86_64-gcc binutils dos2unix git wget unzip python nano
```

The environment is currently not directly supported in the Makefile,
which can be fixed as follows:

```shell
make OSTYPE=cygwin CC=/mingw64/bin/gcc CXX=/mingw64/bin/g++
```

Additionally, MSYS2 does not handle symlinks in the same way as Cygwin does.
At some point `make` will fail, saying that it cannot find the CUDD library.
You can solve this as follows:

```shell
cd ../cudd/
rmdir lib/
./setup.sh
cd ../prism
make OSTYPE=cygwin CC=/mingw64/bin/gcc CXX=/mingw64/bin/g++
./install.sh
```

**Problems?** See the section "[Common Problems And Questions](CommonProblemsAndQuestions.md)".
