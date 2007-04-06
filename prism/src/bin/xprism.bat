@echo off

rem Startup script for PRISM (Windows)
echo Loading PRISM...

rem PRISM home directory
rem Default is .. so it can be run directly from the bin directory.
rem Change to the actual prism directory to allow it to be run from anywhere.
set PRISM_DIR=..

rem Add PRISM to path
path=%PRISM_DIR%\lib;%path%

rem Set up CLASSPATH:
rem  - PRISM jar file (for binary versions) (gets priority)
rem  - classes directory (most PRISM classes)
rem  - top-level directory (for images, dtds)
rem  - lib/pepa.zip (PEPA stuff)
set CP=%PRISM_DIR%\lib\prism.jar;%PRISM_DIR%\classes;%PRISM_DIR%;%PRISM_DIR%\lib\pepa.zip

rem Run PRISM through Java
rem start javaw -Djava.library.path=%PRISM_DIR%\lib -classpath %CP% userinterface/GUIPrism %*
java -Djava.library.path=%PRISM_DIR%\lib -classpath %CP% userinterface/GUIPrism %*
