======
README
======

This is PRISM (Probabilistic Symbolic Model Checker).

------------
INSTALLATION
------------

For detailed installation instructions, check the online manual at:

  http://www.prismmodelchecker.org/manual/InstallingPRISM/Instructions
  
or see the local copies included in this distribution:

  doc/manual/InstallingPRISM/Instructions.html
  doc/manual.pdf

Very abbreviated instructions for installing/running PRISM are as follows:

For Windows binary distributions:

 - to install, run prism-XXX-win-installer.exe
 - to run, use Desktop/Start menu shortcuts or double-click bin\xprism.bat

For other binary distributions:

 - to install, enter the PRISM directory, type "./install.sh"
 - to run, execute bin/xprism or bin/prism

For source code distributions:

 - enter the PRISM directory and type "make"
 - to run, execute bin/xprism or bin/prism

If you have problems check the manual, especially the section "Common Problems And Questions".

-------------
DOCUMENTATION
-------------

The best source of information about using PRISM is the online manual:

  http://www.prismmodelchecker.org/manual/

You can also view the local copies included in this distribution:

  doc/manual/index.html
  doc/manual.pdf

For other PRISM-related information, see the website:

  http://www.prismmodelchecker.org/

---------
LICENSING
---------

PRISM is distributed under the GNU General Public License (GPL).
A copy of this license can be found in the file COPYING.txt.
For more information, see:

  http://www.gnu.org/licenses/

PRISM uses the CUDD (Colorado University Decision Diagram) library of
Fabio Somenzi, which is freely available. For more information about this
library, see:

  http://vlsi.colorado.edu/~fabio/CUDD/

----------------
ACKNOWLEDGEMENTS
----------------

PRISM was created and is still actively maintained by:

 * Dave Parker (University of Birmingham)
 * Gethin Norman (University of Glasgow)
 * Marta Kwiatkowska (University of Oxford) 

We gratefully acknowledge contributions to the PRISM code-base from various sources,
including (in approximately reverse chronological order):

 * Joachim Klein: Various bug fixes, especially related to LTL
 * Ernst Moritz Hahn: Parametric model checking, fast adaptive uniformisation + various other features
 * Frits Dannenberg: Fast adaptive uniformisation
 * Vojtech Forejt: Various model checking code, including multi-objective + GUI enhancements
 * Hongyang Qu: Multi-objective model checking
 * Mateusz Ujma: Bug fixes and GUI improvements
 * Christian von Essen: Symbolic/explicit-state model checking
 * Vincent Nimal: Approximate (simulation-based) model checking techniques
 * Mark Kattenbelt: Wide range of enhancements/additions, especially in the GUI
 * Carlos Bederian (working with Pedro D'Argenio): LTL model checking for MDPs
 * Gethin Norman: Precomputation algorithms, abstraction
 * Alistair John Strachan: Port to 64-bit architectures
 * Alistair John Strachan, Mike Arthur and Zak Cohen: Integration of JFreeChart into PRISM
 * Charles Harley and Sebastian Vermehren: GUI enhancements
 * Rashid Mehmood: Improvements to low-level data structures and numerical solution algorithms
 * Stephen Gilmore: Support for the stochastic process algebra PEPA
 * Paolo Ballarini & Kenneth Chan: Port to Mac OS X
 * Andrew Hinton: Original versions of the GUI, Windows port and simulator
 * Joachim Meyer-Kayser: Original implementation of the "Fox-Glynn" algorithm 

For more details see:

  http://www.prismmodelchecker.org/people.php

-------
CONTACT
-------

If you have problems or questions regarding PRISM, please use the help forum provided. See:

  http://www.prismmodelchecker.org/support.php

Other comments and feedback about any aspect of PRISM are also very welcome. Please contact:

  Dave Parker
  (d.a.parker@cs.bham.ac.uk)
  School of Computer Science
  University of Birmingham
  Edgbaston
  Birmingham
  B15 2TT
  ENGLAND
