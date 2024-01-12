# PRISM

This is PRISM (Probabilistic Symbolic Model Checker).


## Installation

For detailed installation instructions, check the online manual at:

  https://www.prismmodelchecker.org/manual/InstallingPRISM/Instructions
  
or see the local copy included in this distribution:

 * `manual/InstallingPRISM/Instructions.html`

Very abbreviated instructions for installing/running PRISM are as follows:

For Windows binary distributions:

 * to install, run `prism-XXX-win64-installer.exe`
 * to run, use Desktop/Start menu shortcuts or double-click `bin\xprism.bat`

For other binary distributions:

 * to install, enter the PRISM directory and type `./install.sh`
 * to run, execute `bin/xprism` or `bin/prism`

For source code distributions:

 * enter the PRISM directory and type `cd prism` then `make`
 * to check the install, type `make test` or `etc/tests/run.sh`
 * to run, execute `bin/xprism` or `bin/prism`

If you have problems check the manual, especially the section "Common Problems And Questions".


## Documentation

The best source of information about using PRISM is the online manual:

  https://www.prismmodelchecker.org/manual/

You can also view the local copy included in this distribution:

  * `manual/index.html`

For other PRISM-related information, see the website:

  https://www.prismmodelchecker.org/doc

Information for developers is kept here:

  https://github.com/prismmodelchecker/prism/wiki

## Licensing

PRISM is distributed under the GNU General Public License (GPL), version 2.
A copy of this license can be found in the file `COPYING.txt`.
For more information, see:

  https://www.gnu.org/licenses/

PRISM also uses various other libraries (mainly to be found in the lib directory).
For details of those, including licenses and links to downloads and source code, see:

https://www.prismmodelchecker.org/other-downloads.php


## Acknowledgements

PRISM was created and is still actively maintained by:

 * Dave Parker (University of Oxford)
 * Gethin Norman (University of Glasgow)
 * Marta Kwiatkowska (University of Oxford) 

Development of the tool is currently led from Oxford by Dave Parker.

The following have made a wide range of contributions to
PRISM covering many different aspects of the tool
(in approximately reverse chronological order):

 * Steffen Märcker (Technische Universität Dresden)
 * Joachim Klein (formerly Technische Universität Dresden)
 * Vojtech Forejt (formerly University of Oxford)

We also gratefully acknowledge contributions to the PRISM code-base from
(in approximately reverse chronological order):

 * Max Kurze: Language parser code improvements
 * Ludwig Pauly: Reward import/export
 * Alberto Puggelli: First version of interval DTMC/MDP code
 * Xueyi Zou: Partially observable Markov decision processes (POMDPs)
 * Chris Novakovic: Build infrastructure and explicit engine improvements
 * Ernst Moritz Hahn: Parametric model checking, fast adaptive uniformisation + various other features
 * Frits Dannenberg: Fast adaptive uniformisation
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

  https://www.prismmodelchecker.org/people.php


## Contact

If you have problems or questions regarding PRISM, please use the help forum provided. See:

  https://www.prismmodelchecker.org/support.php

Other comments and feedback about any aspect of PRISM are also very welcome. Please contact:

  Dave Parker  
  (david.parker@cs.ox.ac.uk)  
  Department of Computer Science  
  University of Oxford  
  Oxford  
  OX1 3QG
  UK
