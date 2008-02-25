======
README
======

This is PRISM (Probabilistic Symbolic Model Checker), version 3.2.

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

The core team behind the development of PRISM, previously working in the
School of Computer Science at the University of Birmingham and now working
in the Computing Laboratory at the University of Oxford, comprises:

 * Dave Parker
 * Gethin Norman
 * Marta Kwiatkowska
 * Mark Kattenbelt
 
Contributions to the development of PRISM have also been gratefully received from:

 * Andrew Hinton: Original versions of the GUI, Windows-port and simulator
 * Joachim Meyer-Kayser: Implementation of the "Fox-Glynn" algorithm
 * Alistair John Strachan: Port to 64-bit architectures
 * Stephen Gilmore: Support for the stochastic process algebra PEPA
 * Paolo Ballarini & Kenneth Chan: Port of PRISM to Mac OS X
 * Rashid Mehmood: Improvements to low-level data structures and numerical solution algorithms
 * Alistair John Strachan, Mike Arthur and Zak Cohen: Integration of JFreeChart into PRISM
 
For more details see:

  http://www.prismmodelchecker.org/people.php


-------
CONTACT
-------

If you have problems or questions regarding PRISM, please use the help forum provided. See:

  http://www.prismmodelchecker.org/support.php

Other comments and feedback about any aspect of PRISM are also very welcome. Please contact:

  Dave Parker
  (david.parker@comlab.ox.ac.uk)
  Oxford University Computing Laboratory,
  Wolfson Building
  Parks Road
  Oxford
  OX1 3QD
  ENGLAND

