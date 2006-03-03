======
README
======

This is PRISM (Probabilistic Symbolic Model Checker), version 2.1.

This document contains information about installing and running PRISM.

If you downloaded a source code version, please start at section "1. Compiling PRISM from source code".

If you downloaded a precompiled binary version, please start at section "2. Installing a binary version of PRISM".

If you experience problems, please see section "4. Common problems and questions".

For information on how to use the tool itself, please refer to the PRISM Users' Guide
in doc/manual.pdf. For more information about the tool and some of the case studies
which it has already been applied to, please visit the PRISM web site at:

  http://www.cs.bham.ac.uk/~dxp/prism/

--------
Contents
--------

1. Compiling PRISM from source code
2. Installing a binary version of PRISM
3. Running PRISM
4. Common problems and questions
5. Licensing details
6. Acknowledgements
7. Contact


-----------------------------------
1. Compiling PRISM from source code
-----------------------------------

------------------------------------
1.1 What operating system can I use?
------------------------------------

To date, PRISM has been successfully compiled on the following platforms:

 * Linux
 * Solaris
 * Windows (using Cygwin and MingW)
 * Mac OS X

There is more information about compiling on other platforms in section 4.

------------------------------------
1.2 What do I need to compile PRISM?
------------------------------------

The compilation of PRISM relies on a Unix-like environment. For Windows,
we use the the Cygwin development environment (www.cygwin.com). However,
we actually use the MingW libraries (www.mingw.org) so that the final result
is independent of Cygwin at run-time.

You will need:

 * GNU make (sometimes called gmake)

 * A C/C++ compiler (e.g. gcc/g++)

 * Java 2, Standard Edition (J2SE) version 1.4 or higher:
   the Java development kit (JDK), including javac, javah, java, etc.

If you don't know what version of Java you have, try typing the following at
the command prompt:

  java -version

The version information should be displayed. If you don't know whether you have
the JDK, checking that javac, the Java compiler is present by typing:

  javac

If you see information about javac displayed, this is fine. If you get an error
message that javac cannot be found, you probably do not have the JDK installed.
You can download the Sun Java JDK from java.sun.com. Alternatively, you may have
Java installed, but your path may not be set up correctly.

---------------------------
1.3 How do I compile PRISM?
---------------------------

1. Enter the PRISM directory, e.g.:
   
     cd prism-2.1-src
   
2. Hopefully, you can build PRISM simply by typing:
   
     make
   
   For this process to complete correctly, PRISM needs to be able to determine
   both the operating system you are using and the location of your Java distribution.
   If there is a problem with either of these, you will be given instructions
   telling you how to resolve this. You will be advised to specify one or both of
   these manually, such as in these examples:
   
     make OSTYPE=linux
     make JAVA_DIR=/usr/java/j2sdk1.4.2
     make OSTYPE=linux JAVA_DIR=/usr/java/j2sdk1.4.2
   
   Note that it is also possible to achieve this by setting the environment variables
   OSTYPE and JAVA_DIR yourself or by editing the values in the Makefile directly.
   
   If you have any other problems, see section "4. Common problems and questions".

3. Run the PRISM installation script:
   
     ./install.sh
   
   This script simply sets a few parameters in the scripts to run PRISM.
   Note that if you later move or rename the PRISM directory, you will
   need to repeat this step.

Installation is complete. Now see section "3. Running PRISM".


---------------------------------------
2. Installing a binary version of PRISM
---------------------------------------

----------------------------------
2.1 What will I need to run PRISM?
----------------------------------

To run a binary version of PRISM, you will need:

 * Java 2, Standard Edition (J2SE) version 1.4 or higher
   Note that the Java runtime environment (JRE) is sufficient:
   you do not need the Java development kit (JDK).

If you don't know what version of Java you have, try typing the following at
the command prompt:

  java -version

The version information should be displayed. If you get an error message that
java cannot be found, you probably do not have Java installed. You can download
the Sun Java JRE/JDK from java.sun.com. Alternatively, you may have Java
installed, but your path may not be set up correctly.

-----------------------------------------------
2.2 How do I install a binary version of PRISM?
-----------------------------------------------

For Linux/Solaris/OS X:

 1. Unpack the PRISM distribution into a suitable location, e.g.:
    
      tar xfz prism-2.1-linux.tar.gz
    
 2. Enter the prism directory and run the installation script
    
      cd prism-2.1-linux
      ./install.sh
    
    Please note that if your move or rename the PRISM directory after
    this point, you will need to repeat this step.

For Windows:

 1. Unpack the PRISM directory into a suitable location using WinZip
    or a similar application.

Now see section "3. Running PRISM".


----------------
3. Running PRISM
----------------

There are two versions of PRISM: the graphical user interface (GUI) version
and the command-line version. To run the GUI version, see section 3.1, To run
the command-line version, see section 3.2.

If you experience any problems, see section "4. Common problems and questions".

------------------------------------------
3.1 How do I run the GUI version of PRISM?
------------------------------------------

For Linux/Solaris/OS X:

 1. Run the executable file xprism in the bin directory of the PRISM distribution.
    Alternatively, add the bin directory to your path and just type xprism.

For Windows:

 1. Double click on the file xprism.bat in the bin directory.

If you want to create shortcuts to PRISM on your desktop, you can find a selection
of icons in the etc directory.

---------------------------------------------------
3.2 How do I run the command-line version of PRISM?
---------------------------------------------------

For Linux/Solaris/OS X:

 1. Run the executable file prism in the bin directory of the PRISM distribution.
    Alternatively, add the bin directory to your path and just type prism.

For Windows:

 1. Run the command prompt and go into the PRISM bin directory, e.g.:
    
      cd "c:\Program Files\prism-2.1-win\bin"
    
 2. Execute the prism.bat batch file, passing a PRISM example file as an argument, e.g.:
    
      prism ..\examples\dice\dice.pm
    
 3. Alternatively, edit the file prism.bat to allow it to be run from any location.
    The instructions inside the file explain this procedure.
 
To get a full list of options and switches for the command-line version,
please refer to the PRISM Users' Guide in doc/manual.pdf.


--------------------------------
4. Common problems and questions
--------------------------------

1. Compilation (i.e. running make) seems to do nothing

   Perhaps you are not using the GNU version of make. Try typing "make -v"
   to find out. On some systems, GNU make is called gmake. See also point 3.


2. Compilation (i.e. running make) gives errors of the form:
   "Unexpected end of line seen..."
   or
   "make: Fatal error in reader: Makefile, line 58: Unexpected end of line seen"
   
   See answer to previous question.


3. Do I have to use GNU make?
   
   No, but you will have to modify the PRISM Makefile manually to overcome this.
   All places where a dependency occurs are marked with a comment "requires GNU make".


4. When I run PRISM, I get an error of the form:
   "Exception in thread "main" java.lang.NoClassDefFoundError: ..."
   
   (a) If you are on Linux/Solaris/OS X, make sure that you ran the installation script
       install.sh from the PRISM directory.
   
   (b) If you are on Windows, make sure that you followed the instructions in
       section 3.2 correctly.
   
   (c) If you compiled the PRISM distribution from source code, make sure that no errors
       occurred during the process. To recompile, go into the PRISM directory, type
       "make clean_all" and then follow the instructions in section 1.3 again.
   
   
5. When I run PRISM, I get an error of the form:
   "java.lang.UnsatisfiedLinkError: ..."
   
   See answer to previous question.


6. Can I build PRISM on operating systems other than those currently supported?
   
   PRISM should be suitable for any Unix/Linux variant.
   
   The first thing you will need to do is compile CUDD on that platform.
   Fortunately, CUDD has already been successfully built on a large number of
   operating systems. Have a look at the sample makefiles we provide (i.e. the
   files cudd/Makefile.*) which are slight variants of the original makefile
   provided with cudd (found here: cudd/modified/orig/Makefile). They contain
   instructions on how to modify it for various platforms. You can then call
   your new modified makefile something appropriate (cudd/Makefile.$OSTYPE) and
   proceed to build PRISM as usual. To just build CUDD, not PRISM, type
   "make cuddpackage" instead of make".
   
   Next, look at the main PRISM Makefile, in particular, each place where the
   variable $OSTYPE is referred to. Most lines include comments and further
   instructions. Once you have done this, proceed as usual.
   
   If you do successfully build PRISM on other platforms, please let us know
   so we can include this information in future releases. Thanks.


7. I still have a problem. What can I do?

   Please feel free to email us for advice:
   
     Dave Parker: dxp@cs.bham.ac.uk 


--------------------
5. Licensing details
--------------------

PRISM is distributed under the GNU General Public License (GPL).
A copy of this license can be found in the file COPYING.txt.
For more information, see:

  http://www.gnu.org/licenses/

PRISM uses the CUDD (Colorado University Decision Diagram) library of
Fabio Somenzi, which is freely available. For more information about this
library, see:

  http://vlsi.colorado.edu/~fabio/CUDD/


-------------------
6. Acknowledgements
-------------------

PRISM has been developed at the University of Birmingham by:

- Dave Parker
- Gethin Norman
- Marta Kwiatkowska

The PRISM graphical user interface and the port to Windows were developed by: 

- Andrew Hinton

We also gratefully acknowledge contributions by:

- Joachim Meyer-Kayser ("Fox-Glynn" algorithm)
- Stephen Gilmore (PEPA support)
- Rashid Mehmood (low-level data structure/algorithm improvements)


----------
7. Contact
----------

All comments and feedback about any aspect of PRISM are very welcome.
Please contact:

  Dave Parker
  (dxp@cs.bham.ac.uk)
  School of Computer Science
  University of Birmingham
  Edgbaston
  Birmingham
  B15 2TT
  ENGLAND


---------------------------------------------------------------------

Dave Parker
8/9/2004
