@echo off

rem Interface wrapper for calling Rabinizer (version 4)
rem Invoke from PRISM with
rem   -ltl2datool hoa-rabinizer-for-prism.bat -ltl2dasyntax rabinizer

rem Expects the ltl2dra executable to be on the PATH, otherwise specify its location below
 
ltl2dra.bat --filein "%1" --fileout="%2" --complete
