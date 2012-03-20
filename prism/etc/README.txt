This directory contains the following files.
Where appropriate, comments/instructions are included in the files themselves.

 * icons/ - PRISM icons for shortcuts/etc.
   - p*.png - Variously sized PNGs
   - prism.ico - Windows icon file

 * scripts/ - Various useful scripts
   - prism2html - converts PRISM models/properties to HTML
   - prism2latex - converts PRISM models/properties to LaTex
   - prism3to4 - fixes some common problems in old (3.x and earlier) PRISM models
   - prism-statra - combines a PRISM .sta and .tra file into one file
   (these scripts run on Linux/Unix/OS X and require prism to be in your path)
   - prism-filler.py - automates filling a text file (e.g. latex) with results
     from log files. This script requires Python, run it without arguments for
     more help.
   - bash-prism-completion.sh - Bash programmable completion for PRISM,
     automatically completes options for PRISM when run in Bash.
   - prism-auto - automates PRISM execution for testing/benchmarking
   - prism-test - automates running PRISM in test mode

 * syntax-highlighters/ - Syntax highlighting configs for various tools
   - gnome/{Overrides.xml,prism.lang} - files for Gnome environment
   - nedit/prism.pats - files for NEdit editor
   
 * prism.css - CSS style file for PRISM-generated HTML code

 * prism.tex - LaTeX file for PRISM-generated LaTeX code
 
 * prism-eclipse-formatter.xml - Eclipse Java code formatting definitions
